///////////////////////////////////////////////////////////////////////////////
// File: OrientedMeasuresCalculator.java 
// Author: Carlos Bobed
// Date: November 2017
// Comments: Program to calculate both distances  
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures.experiments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.measures.Measures;

public class OrientedMeasuresCalculator {
	
	public static String RESULTS_FILE_OPTION = "resultsFile"; 
	public static String MEASURE_OPTION = "measure"; 
	public static String ORIGINAL_CT_OPTION ="originalCT"; 
	public static String ORIGINAL_DB_ANALYSIS_OPTION = "originalDBAnalysis"; 
	public static String COMPARED_CT_OPTION = "comparedCT"; 
	public static String COMPARED_DB_ANALYSIS_OPTION = "comparedDBAnalysis"; 
	public static String DATASET_OPTION = "dataset"; 
	public static String VREEKEN_OPTION = "vreekenFormat"; 
	public static String HELP_OPTION = "help"; 
	
	public static String RESULTS_HEADERS = "originalCT;comparedCT;comparedDB;ourFormat;measure;measureValue;execTime";  
			
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(RESULTS_FILE_OPTION, true, ".csv file to include the results - append if exists");
		options.addOption(MEASURE_OPTION, true, "measure type to be used - regular|usingLengths"); 
		options.addOption(ORIGINAL_CT_OPTION, true, "codeTable of the dataset AGAINST which we compare the dataset");
		options.addOption(ORIGINAL_DB_ANALYSIS_OPTION, true, "original db analysis file required to read Vreeken CTs "); 
		options.addOption(COMPARED_CT_OPTION, true, "codeTable of the compared dataset"); 
		options.addOption(COMPARED_DB_ANALYSIS_OPTION, true, "compared db analysis file required to read Vreeken CTs"); 
		options.addOption(DATASET_OPTION, true, "compared dataset"); 
		options.addOption(VREEKEN_OPTION, false, "whether we use or not the Vreeken Format"); 
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "OrientedMeasuresCalculator", options );
				System.exit(0);
			} 
			
			String resultsFilename = cmd.getOptionValue(RESULTS_FILE_OPTION); 
			String measure = cmd.getOptionValue(MEASURE_OPTION); 
			String datasetFilename = cmd.getOptionValue(DATASET_OPTION); 
			
			File resultsFile = new File(resultsFilename);
			PrintWriter out = null; 
			if (resultsFile.exists()) {
				out = new PrintWriter(new FileOutputStream(resultsFile, resultsFile.exists())); 
			}
			else {
				out = new PrintWriter(resultsFile); 
				out.println(RESULTS_HEADERS); 
			}
			
			String originalCTFilename = cmd.getOptionValue(ORIGINAL_CT_OPTION);
			String comparedCTFilename = cmd.getOptionValue(COMPARED_CT_OPTION);
			ItemsetSet originalItemCT = null; 
			ItemsetSet comparedItemCT = null; 
			
			if (cmd.hasOption(VREEKEN_OPTION)) {
				String originalDBAnalysisFilename = cmd.getOptionValue(ORIGINAL_DB_ANALYSIS_OPTION);
				String comparedDBAnalysisFilename = cmd.getOptionValue(COMPARED_DB_ANALYSIS_OPTION);
				originalItemCT = Utils.readVreekenEtAlCodeTable(originalCTFilename, originalDBAnalysisFilename); 
				comparedItemCT = Utils.readVreekenEtAlCodeTable(comparedCTFilename, comparedDBAnalysisFilename); 
			}
			else {
				originalItemCT = Utils.readItemsetSetFile(originalCTFilename); 
				comparedItemCT = Utils.readItemsetSetFile(comparedCTFilename); 
			}
			
			ItemsetSet transactions = Utils.readItemsetSetFile(datasetFilename); 
//			DataIndexes index = new DataIndexes(transactions); 
			CodeTable comparedCT = new CodeTable(comparedItemCT); 
			CodeTable originalCT = new CodeTable(originalItemCT); 
			
			long start = System.nanoTime(); 
			double value = -1.0; 
			switch (measure) {
				case "regular":
					value = Measures.structuralSimilarityWithoutKeepingDistribution(transactions, comparedCT, originalCT);
					break; 
				case "usingLengths": 
					value = Measures.structuralSimilarityWithoutKeepingDistributionUsingLengths(transactions, comparedCT, originalCT); 
					break; 
				default: 
					break; 
			}
			long end = System.nanoTime(); 
			
			out.println(originalCTFilename+";"+comparedCTFilename+";"+datasetFilename+";"+(!cmd.hasOption(VREEKEN_OPTION))+";"
					+measure+";"+value+";"+(end-start)/1000000.0); 
			out.flush();
			out.close();
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
}
