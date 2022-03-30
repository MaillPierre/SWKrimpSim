///////////////////////////////////////////////////////////////////////////////
// File: CompressionRatioCalculator.java 
// Author: Carlos Bobed 
// Date: March 2022
// Comments: Program to calculate the compression ratio that a codetable achieves 
// 		over a .dat database. Used in the PartMining project. 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures.experiments;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.measures.CodificationMeasure;

public class CompressionRatioCalculator {
	
	private static Logger logger = Logger.getLogger(CompressionRatioCalculator.class);
	
	public static String CT_OPTION ="CT"; 
	public static String DATASET_OPTION = "dataset"; 
	public static String HELP_OPTION = "help"; 
			
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(CT_OPTION, true, "codeTable of the dataset to obtain the compression ratio");
		options.addOption(DATASET_OPTION, true, "compared dataset"); 
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "CompressionRatioCalculator", options );
				System.exit(0);
			} 
			
			String datasetFilename = cmd.getOptionValue(DATASET_OPTION); 
			
			String CTFilename = cmd.getOptionValue(CT_OPTION);
			ItemsetSet itemCT = null; 
			
			itemCT = Utils.readItemsetSetFile(CTFilename); 
			
			ItemsetSet transactions = Utils.readItemsetSetFile(datasetFilename); 
			CodeTable originalCT = new CodeTable(itemCT); 
			
			long start = System.nanoTime(); 
			double sctCompressedSize = -1.0; 
			double compressedSize = -1.0;
			
			CodificationMeasure measure = new CodificationMeasure(transactions, originalCT);
			logger.warn("Updating supports ... "); 
			measure.updateSupports();
			logger.warn("Done ... "+((System.nanoTime()-start)/1000000)+"ms"); 
			measure.getCodetable().orderCodesStandardCoverageOrder();
			logger.warn("Updating the usages ... "); 
			measure.updateUsages();
			logger.warn("Done ... "+((System.nanoTime()-start)/1000000)+"ms"); 
			compressedSize = measure.totalCompressedSize(); 
			sctCompressedSize = measure.codificationLengthAccordingSCT(); 
			
			System.out.println("CompressionRatio: "+(compressedSize/sctCompressedSize)); 
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
}

