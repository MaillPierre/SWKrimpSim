#!/bin/bash

# args: 
# 	$1 name of the database without the extension (should be .dat)

EXEC_FILENAME=krimp
# Here is where the fic/krimp exec and its configuration files are
BIN_PATH=/home/cbobed/proyectos/slim/KrimpBinSource/bin

# Our datadir and experiments dir
# Don't forget in the script to put the last slash => they require it 
DATA_DIR=/home/cbobed/proyectos/slim/KrimpBinSource/data/
EXP_DIR=/home/cbobed/proyectos/slim/KrimpBinSource/xps/

# Options for the krimp algorithm (used to populate iscName - it varies from one version to another)
## Input frequent itemset collection to be used as candidates (may or may not yet ex ist on disk)
# Candidate set order determined by [ a (supp desc, length asc, lex) | d (like a, but length desc) | z | aq | as ... see the code ]
# Candidate type determined by [ all | cls | closed ]

# We usually use -cls-1d (closed freq itemsets with minsupport = 1 and desdecing order)
OPTIONS_DIR=-cls-1d

PRUNE_STRATEGY=pop
ALGORITHM=coverpartial

DATABASE_NAME=$1

MOVED=false 

# we move the database to the dataDir 
if [ -f ""$DATABASE_NAME".dat" ]
then 
	echo Moving $DATABASE_NAME.dat to "$DATA_DIR"datasets
	MOVED=true
	mv $DATABASE_NAME.dat "$DATA_DIR"datasets
fi

# First we convert the .dat to their format

sed -i "s/^ *dbName *=.*/dbName = $1/g" $BIN_PATH/convertdb.conf
sed -i "s/^ *dataDir *=.*/dataDir = ${DATA_DIR//\//\\/}/g" $BIN_PATH/convertdb.conf

echo Executing the conversion ... 
$BIN_PATH/$EXEC_FILENAME $BIN_PATH/convertdb.conf
if [ -f ""$DATA_DIR"datasets/"$DATABASE_NAME".db" ]
then 
	echo Database created successfully 
else 
	echo Wrong ... 
	exit -1
fi

# we update the compress.conf file 
sed -i "s/^ *dbName *=.*/dbName = $1/g" $BIN_PATH/compress.conf
sed -i "s/^ *dataDir *=.*/dataDir = ${DATA_DIR//\//\\/}/g" $BIN_PATH/compress.conf
sed -i "s/^ *iscName *=.*/iscName = "$DATABASE_NAME""$OPTIONS_DIR"/g" $BIN_PATH/compress.conf
sed -i "s/^ *prunestrategy *=.*/prunestrategy = $PRUNE_STRATEGY/g" $BIN_PATH/compress.conf
sed -i "s/^ *algo *=.*/algo = $ALGORITHM/g" $BIN_PATH/compress.conf

echo Executing the krimp algorithm ... 
$BIN_PATH/$EXEC_FILENAME $BIN_PATH/compress.conf

NEW_DIR=$( ls -td -- "$EXP_DIR"compress/* | head -n 1)

mkdir "$DATABASE_NAME"-output
cp $NEW_DIR/*.ct ./"$DATABASE_NAME"-output 
cp $NEW_DIR/*.csv ./"$DATABASE_NAME"-output 

# reminders
# ls -td -- */ | head -n 1
# $(echo .. | tr "-" "\n")
# ${array[*]}

# we move back the database file if required 
if [ "$MOVED" = true ]
then 
	echo Moving back the file 
	echo Moving "$DATA_DIR"datasets/"$DATABASE_NAME".dat . 
	mv "$DATA_DIR"datasets/"$DATABASE_NAME".dat .
fi


