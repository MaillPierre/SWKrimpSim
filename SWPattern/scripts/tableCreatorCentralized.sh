#!/bin/bash
#FILES="ekaw-2016-complete-0.1-instanceBased.nt ekaw-2016-complete-0.2-instanceBased.nt"
FILES="ekaw-2016-complete.nt iswc-2013-complete.nt www-2012-complete.nt"
DIR="dataDebugging"
CODIFICATION_METHOD="R0 R1 R15"
#CODIFICATION_METHOD="R0"

PROP_COUNTER=1
PROP_TYPE_COUNTER=1
PROP_OTHER_COUNTER=1
#for file in `ls -1 output/ekaw*.nt`; do 
for file in $FILES; do 
	for method in $CODIFICATION_METHOD; do 
		echo Applying $method
		case $method in
			R0)				
			  let "AUX = $PROP_COUNTER - 1"
			  java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $DIR/$file -inputConversionIndex $DIR/Properties-$AUX.idx -outputCodeTable -nProperties -outputConversionIndex $DIR/Properties-$PROP_COUNTER.idx -pruning
			  let "PROP_COUNTER += 1"
			  ;;
			R1)
			  let "AUX = $PROP_TYPE_COUNTER - 1"			  
			  java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $DIR/$file -inputConversionIndex $DIR/PropertiesAndTypes-$AUX.idx -outputCodeTable -nPropertiesAndTypes -outputConversionIndex $DIR/PropertiesAndTypes-$PROP_TYPE_COUNTER.idx -pruning
			  let "PROP_TYPE_COUNTER += 1"
			  ;;
			R15)
			  let "AUX = $PROP_OTHER_COUNTER - 1"
			  java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $DIR/$file -inputConversionIndex $DIR/PropertyAndOthers-$AUX.idx -outputCodeTable -nPropertiesAndOthers -outputConversionIndex $DIR/PropertiesAndOthers-$PROP_OTHER_COUNTER.idx -pruning
			  let "PROP_OTHER_COUNTER += 1"
			  ;;
		esac
	done
done
