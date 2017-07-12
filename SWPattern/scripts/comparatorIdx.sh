#!/bin/bash

MODIFICATION_PERCENTAGES="0.1 0.2 0.3 0.4 0.5"
CODIFICATION_METHOD="R0 R1 R15"
MODIFICATION_METHOD="instanceBased propertyBased both"
BATCHES="output2ndBatch output3rdBatch"
OUTPUT_FILE="outputIdx-iswc.csv"

for file in `ls -1 data/iswc*.nt`; do
	for batch in $BATCHES; do 
		for modPer in $MODIFICATION_PERCENTAGES; do
			for modMethod in $MODIFICATION_METHOD; do 			
				for codMethod in $CODIFICATION_METHOD; do 			
					OTHER_FILE=${file/data/$batch}
					OTHER_FILE=${OTHER_FILE/%\.nt/-$modPer-$modMethod\.nt}
					case $codMethod in
						R0)		
						   java -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $file -inputOtherRDF $OTHER_FILE -inputCodeTable $file.Property.krimp.dat -inputConversionIndex $file-Properties.idx -pruning -nProperties -outputComparisonResultsFile $OUTPUT_FILE
						  ;;
						R1)
						   java -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $file -inputOtherRDF $OTHER_FILE -inputCodeTable $file.PropertyAndType.krimp.dat -inputConversionIndex $file-PropertiesAndTypes.idx -pruning -nPropertiesAndTypes -outputComparisonResultsFile $OUTPUT_FILE
						  ;;
						R15)
						   java -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $file -inputOtherRDF $OTHER_FILE -inputCodeTable $file.PropertyAndOther.krimp.dat -inputConversionIndex $file-PropertiesAndOthers.idx -pruning -nPropertiesAndOthers -outputComparisonResultsFile $OUTPUT_FILE
						  ;;
					esac
				done
			done
		done
	done
done
