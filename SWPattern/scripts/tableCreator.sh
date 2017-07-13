#!/bin/bash
#FILES="ekaw-2016-complete-0.1-instanceBased.nt ekaw-2016-complete-0.2-instanceBased.nt"
FILES="ekaw-2016-complete.nt iswc-2013-complete.nt www-2012-complete.nt"
DIR="dataCentralizedTables"
CODIFICATION_METHOD="R0 R1 R15"
#CODIFICATION_METHOD="R0"

#for file in `ls -1 output/ekaw*.nt`; do 
for file in $FILES; do 
	for method in $CODIFICATION_METHOD; do 
		echo Applying $method
		case $method in
			R0)				
			  java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $DIR/$file -inputConversionIndex $DIR/$file-Properties.idx -outputCodeTable -nProperties -outputConversionIndex $DIR/$file-Properties.idx -pruning
			  ;;
			R1)
			  java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $DIR/$file -inputConversionIndex $DIR/$file-PropertiesAndTypes.idx -outputCodeTable -nPropertiesAndTypes -outputConversionIndex $DIR/$file-PropertiesAndTypes.idx -pruning
			  ;;
			R15)
			  java.exe -Xmx8G -classpath 'lib/*' com.irisa.swpatterns.SWPatterns -inputRDF $DIR/$file -inputConversionIndex $DIR/$file-PropertyAndOthers.idx -outputCodeTable -nPropertiesAndOthers -outputConversionIndex $DIR/$file-PropertiesAndOthers.idx -pruning
			  ;;
		esac
	done
done
