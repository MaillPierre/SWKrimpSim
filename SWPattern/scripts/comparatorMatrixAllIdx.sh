#!/bin/bash

CODIFICATION_METHOD="R0 R1 R15"

OUTPUT_FILE="outputMatrixAllIdx.csv"
DIR="dataCentralizedTables"

for fileRef in `ls -1 dataCentralizedTables/*.nt`; do 
	for fileEval in `ls -1 dataCentralizedTables/*.nt`; do 
		for codMethod in $CODIFICATION_METHOD; do 			
			case $codMethod in
				R0)		
				   java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $fileRef -inputOtherRDF $fileEval -inputCodeTable $fileRef.Property.krimp.dat -inputOtherCodeTable $fileEval.Property.krimp.dat -inputConversionIndex $DIR/Properties.idx -pruning -nProperties -outputComparisonResultsFile $OUTPUT_FILE
				  ;;
				R1)
				   java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $fileRef -inputOtherRDF $fileEval -inputCodeTable $fileRef.PropertyAndType.krimp.dat -inputOtherCodeTable $fileEval.PropertyAndType.krimp.dat -inputConversionIndex $DIR/PropertiesAndTypes.idx -pruning -nPropertiesAndTypes -outputComparisonResultsFile $OUTPUT_FILE
				  ;;
				R15)
				   java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $fileRef -inputOtherRDF $fileEval -inputCodeTable $fileRef.PropertyAndOther.krimp.dat -inputOtherCodeTable $fileEval.PropertyAndOther.krimp.dat -inputConversionIndex $DIR/PropertiesAndOthers.idx -pruning -nPropertiesAndOthers -outputComparisonResultsFile $OUTPUT_FILE 		
				  ;;
			esac
		done
	done
done
