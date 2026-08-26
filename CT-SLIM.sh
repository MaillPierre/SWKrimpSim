#!/bin/bash
# args:
# args:
# $1 name of the database with the extension (should be .dat),
#    OR a .nt RDF file to convert first
# $2 "nP" to use -nProperties,
#    "nPT" to use -nPropertiesAndTypes,
# $3 (optional, only used if $1 is .nt) path to an .idx index file to reuse/extend

cd /work || { echo "Could not cd into /work"; exit 1; }

if [ $# -lt 1 ]; then
    echo "Usage:"
    echo "  $0 <database.dat>"
    echo "  $0 <dataset.nt> <nP|nPT> [index.idx]"
    exit 1
fi

# Helper to update configuration files
set_conf() {
    local file="$1"
    local key="$2"
    local value="$3"

    if grep -qE "^[[:space:]]*${key}[[:space:]]*=" "$file"; then
        sed -i "s|^[[:space:]]*${key}[[:space:]]*=.*|${key} = ${value}|" "$file"
    else
        sed -i "/^EndConfig/i ${key} = ${value}" "$file"
    fi
}

EXEC_FILENAME=fic

# Here is where the fic exec and its configuration files are
BIN_PATH=/opt/slim/bin

# Our datadir and experiments dir
# Don't forget in the script to put the last slash => they require it
DATA_DIR=/opt/slim/data/
EXP_DIR=/opt/slim/xps/

# We usually use -cls-1d (closed freq itemsets with minsupport = 1 and desdecing order)
OPTIONS_DIR=-cls-1d
PRUNE_STRATEGY=pop
ALGORITHM=slimCS-coverpartial-orderly-alt-length
ESTIMATION_STRATEGY=ngainct


###############################################################################
# RDF -> DAT conversion
###############################################################################
if [[ $1 == *.nt ]]; then
    echo "Converting the RDF file to .dat format"
 
    if [[ "$2" == "nP" ]]; then
        NEIGHBORHOOD_OPTION="-nProperties"
    elif [[ "$2" == "nPT" ]]; then
        NEIGHBORHOOD_OPTION="-nPropertiesAndTypes"
    else
        echo "Second parameter must be nP or nPT"
        exit 1
    fi
 
    JAVA_ARGS=(-input "$1" "$NEIGHBORHOOD_OPTION")
 
    # Reuse/extend a shared index
    if [[ -n "$3" && "$3" == *.idx ]]; then
        INDEX_FILE="$3"
        if [[ -f "$3" ]]; then
            echo "Reusing existing index $3"
            JAVA_ARGS+=(-inputIndex "$3")
        else
            echo "Index $3 does not exist yet, it will be created"
        fi
    else
        INDEX_FILE="/tmp/conversionIndex.idx"
    fi
    JAVA_ARGS+=(-outputIndex "$INDEX_FILE") 

    echo "Starting the RDF - .dat conversion process ..."
    echo "Running: java -jar SWPatternsCover.jar ${JAVA_ARGS[*]}"
    java -jar ./SWPatternsCover.jar "${JAVA_ARGS[@]}" >/dev/null 2>&1

    GENERATED_DAT=$(find . -maxdepth 1 -type f -name "$(basename "$1").*.dat" | head -n 1)
 
    if [[ ! -f "${GENERATED_DAT}" ]]; then
        echo "RDF to .dat conversion failed, generated .dat file not found"
        exit 1
    fi
    ORIGINAL_NAME="$(basename "${GENERATED_DAT%.dat}")"

else
    # $1 is already a .dat database (with or without the extension written)
    ORIGINAL_NAME="$(basename "${1%.dat}")"
fi

###############################################################################
# SLIM compression
###############################################################################
echo
echo "Starting the SLIM compression process for $ORIGINAL_NAME"
echo


# Dots are replaced by underscores
DATABASE_NAME="${ORIGINAL_NAME//./_}"
DATABASE_NAME="${DATABASE_NAME//-/_}"

INPUT_FILE="./${ORIGINAL_NAME}.dat"

OUTPUT_DIR="./results/${ORIGINAL_NAME}-output"
HOURS=1

set_conf "$BIN_PATH/datadir.conf" "dataDir" "${DATA_DIR}datasets/"
set_conf "$BIN_PATH/datadir.conf" "expDir" "${EXP_DIR}"

# we move the database to the dataDir 
if [ -f "$INPUT_FILE" ]; then
	echo ${ORIGINAL_NAME}.dat to "${DATA_DIR}"datasets
    if [[ $1 == *.nt ]]; then
        mv "${INPUT_FILE}" "${DATA_DIR}datasets/${DATABASE_NAME}.dat"
    else
        cp "${INPUT_FILE}" "${DATA_DIR}datasets/${DATABASE_NAME}.dat"
    fi
else
    echo Database ${ORIGINAL_NAME}.dat not found in the current directory
    exit 1
fi

# First we convert the .dat to their format
set_conf "$BIN_PATH/convertdb.conf" "dbName" "${DATABASE_NAME}"
set_conf "$BIN_PATH/convertdb.conf" "dataDir" "${DATA_DIR}"

echo Executing the conversion ...
$BIN_PATH/$EXEC_FILENAME $BIN_PATH/convertdb.conf

if [ -f ""$DATA_DIR"datasets/"$DATABASE_NAME".db" ]
then
    echo Database created successfully
else
    echo Wrong ...
    exit -1
fi

# we analyse the database 
echo Analysing the database ...
set_conf "$BIN_PATH/analysedb.conf" "dbName" "${DATABASE_NAME}"
set_conf "$BIN_PATH/analysedb.conf" "dataDir" "${DATA_DIR//\//\\/}"

$BIN_PATH/$EXEC_FILENAME $BIN_PATH/analysedb.conf
echo Done
echo

# we update the compress.conf file 
set_conf "$BIN_PATH/compress.conf" "dbName" "${DATABASE_NAME}"
set_conf "$BIN_PATH/compress.conf" "dataDir" "${DATA_DIR//\//\\/}"
set_conf "$BIN_PATH/compress.conf" "iscName" "${DATABASE_NAME}${OPTIONS_DIR}"
set_conf "$BIN_PATH/compress.conf" "pruneStrategy" "${PRUNE_STRATEGY}"
set_conf "$BIN_PATH/compress.conf" "estStrategy" "${ESTIMATION_STRATEGY}"
set_conf "$BIN_PATH/compress.conf" "algo" "${ALGORITHM}"
set_conf "$BIN_PATH/compress.conf" "writeReportFile" "yes"
set_conf "$BIN_PATH/compress.conf" "maxTime" "${HOURS}"

echo Executing the SLIM algorithm ...
$BIN_PATH/$EXEC_FILENAME $BIN_PATH/compress.conf

NEW_DIR=$(find "$EXP_DIR/compress_ng" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort | tail -n 1)

if [[ -z "$NEW_DIR" ]]; then
    echo "SLIM compression failed."
    exit 1
fi


###############################################################################
# Collect results
###############################################################################
echo Retrieving all the information ...
NEW_DIR=$( ls -td -- "$EXP_DIR"compress_ng/* | head -n 1)

mkdir -p "$OUTPUT_DIR"

cp "$NEW_DIR"/*.ct "$OUTPUT_DIR/"
cp "$NEW_DIR"/*.csv "$OUTPUT_DIR/"

cp "$DATA_DIR"datasets/"$DATABASE_NAME".db "$OUTPUT_DIR/${ORIGINAL_NAME}.db"
cp "$DATA_DIR"datasets/"$DATABASE_NAME".db.analysis.txt "$OUTPUT_DIR/${ORIGINAL_NAME}.db.analysis.txt"
cp "$DATA_DIR"datasets/"$DATABASE_NAME".dat "$OUTPUT_DIR/${ORIGINAL_NAME}.dat"

# If an index file was generated or updated
if [[ -f "$INDEX_FILE" ]]; then
    if [[ "$INDEX_FILE" == /tmp/* ]]; then
        mv "$INDEX_FILE" "$OUTPUT_DIR/"
    else
        cp "$INDEX_FILE" "$OUTPUT_DIR/"
    fi
fi

echo Done. All the results are in $OUTPUT_DIR

# we clean the experiments directory
rm -fr "$EXP_DIR"compress_ng/*

###############################################################################
# Decode Vreeken code table
###############################################################################

#echo
#echo "Decoding Vreeken code table..."

#java -jar ./DecodeVreeken.jar "$ORIGINAL_NAME"

#if [[ $? -ne 0 ]]; then
#    echo "Code table decoding failed."
#    exit 1
#fi

