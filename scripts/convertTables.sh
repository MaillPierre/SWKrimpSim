#!/bin/bash

# $1 is the directory where all the -outputs are stored
# $2 is the index file

for i in `ls -d $1/*-output`; do 
	echo Processing $i 
	CT_FILE=$i/ct-latest.ct
	DATASET_FILE=`basename $i`
	DATASET_FILE=${DATASET_FILE/-output/}
	DB_FILE=$i/$DATASET_FILE.db.analysis.txt
	OUT_FILE=$i/"$DATASET_FILE"-CT.dat
	java -cp krimpRDF-bigdata.jar com.irisa.swpatterns.data.utils.TableFormatConverter -CT $CT_FILE  -DBAnalysis $DB_FILE -index $2 -outputCT $OUT_FILE
done