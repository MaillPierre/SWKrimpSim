#!/bin/bash
## $1 : output csv file name
## headers - according to generateStructuralComparisonSpreadsheet
#    filenameHeader = 'filename'
#    originalDBSizeHeader = 'originalDBSize'
#    compressedSizeHeader = 'compressedSize'
#    ratioHeader = 'ratio'

echo 'filename;originalDBSize;compressedSize;ratio' > $1

for i in `ls -d *-output`; do 
	echo $i
	ORIGINAL_DBSIZE=`head -n 2 $i/report*.csv | tail -1l | awk -F ";" '{print $8}'`
	echo original_dbSize: $ORIGINAL_DBSIZE
	COMPRESSED_DBSIZE=`tail -1l $i/report*.csv | awk -F ";" '{print $8}'`
	echo compressed_dbSize: $COMPRESSED_DBSIZE
	RATIO_STRING=$COMPRESSED_DBSIZE/$ORIGINAL_DBSIZE
	echo ratioString: $RATIO_STRING
	echo ratio: `echo $COMPRESSED_DBSIZE/$ORIGINAL_DBSIZE | bc -l`
	
	echo "${i/-output/};$ORIGINAL_DBSIZE;$COMPRESSED_DBSIZE;`echo $COMPRESSED_DBSIZE/$ORIGINAL_DBSIZE | bc -l`" >> $1
	
done
	
