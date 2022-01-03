///////////////////////////////////////////////////////////////////////////////
// File: CodeTableCleanerKRIMP.java 
// Author: Carlos Bobed
// Date: Dec 2021
// Comments: Utility to get a codetable resulting from the merge of different 
// 		codetables partitioned and select the best subset using KRIMP
// 		This is a tool directly related to "PartMining" project, but included here 
// 		as it is part of the long-term project on graph mining
// 		https://github.com/cbobed/PartMining
// Modifications: 
// 		
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.parallel;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.BaseRDF.MODE;
import com.irisa.jenautils.QueryResultIterator;
import com.irisa.jenautils.UtilOntology;
import com.irisa.krimp.AbstractKrimpSlimAlgorithm.CANDIDATE_STRATEGY;
import com.irisa.krimp.CodeTable;
import com.irisa.krimp.CodeTableSlim;
import com.irisa.krimp.KrimpAlgorithm;
import com.irisa.krimp.KrimpSlimAlgorithm;
import com.irisa.krimp.KrimpSlimAlgorithmExperimental;
import com.irisa.krimp.data.DataIndexes;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.LabeledTransactions;

public class CodeTableCleanerKRIMP {
	
	private static Logger logger = Logger.getLogger(CodeTableCleanerKRIMP.class);

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		
		logger.debug(Arrays.toString(args));
		
		// In/out options name to facilitate further references
		String inputTransactionOption = "inputTransaction";
		String inputCandidatesOption = "inputCandidates";
		String outputCodeTableOption = "outputCodeTable";
		
		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		// In/Out
		options.addOption(inputTransactionOption, true, "Transaction file.");
		options.addOption(inputCandidatesOption, true, "File containing the candidate codes extracted for the first file.");
		options.addOption(outputCodeTableOption, false, "Create an Itemset file containing the  KRIMP codetable for each file <filename>.<encoding>.krimp.dat.");
		
		options.addOption("pruning", false, "Activate post-acceptance pruning for better quality code table but slower performances."); 
		
		// Setting up options and constants etc.
		try {
			CommandLine cmd = parser.parse( options, args);
	
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "CodeTableMergerKRIMP", options );
			} else {
				boolean inputTransaction = cmd.hasOption(inputTransactionOption);
				boolean inputCodeTableCodes = cmd.hasOption(inputCandidatesOption);
				boolean outputCodeTableCodes = cmd.hasOption(outputCodeTableOption);
				
				// Boolean options
				boolean activatePruning = cmd.hasOption("pruning"); 
				String firstOutputFile = "";
				
				String firstOutputKRIMPFile = firstOutputFile + ".merged.dat";				
				try {
					
					// we read the transactions 
					ItemsetSet realtransactions = new ItemsetSet(Utils.readTransactionFile(cmd.getOptionValue(inputTransactionOption)));
					// we read the merged code table, used as directly candidate codes
					ItemsetSet codes = new ItemsetSet(Utils.readItemsetSetFile(cmd.getOptionValue(inputCandidatesOption))); 
					
					DataIndexes analysis = new DataIndexes(realtransactions);
					CodeTable standardCT = CodeTable.createStandardCodeTable(realtransactions, analysis);
	
					KrimpAlgorithm kAlgo = new KrimpAlgorithm(realtransactions, codes);
								
					CodeTable krimpCT = null;
					long startKrimp= -1 ; 
					long endKrimp= -1 ; 
					
					logger.debug("KRIMP algorithm START");
					startKrimp = System.nanoTime(); 
					krimpCT = kAlgo.runAlgorithm(activatePruning);
					endKrimp = System.nanoTime(); 
					logger.debug("KRIMP algorithm STOP");
					
					if(outputCodeTableCodes) {
						Utils.printItemsetSet(krimpCT.getCodes(), firstOutputKRIMPFile);
					}
					double normalSize = standardCT.totalCompressedSize();
					double compressedSize = krimpCT.totalCompressedSize();
					logger.debug("-------- FIRST RESULT ---------");
					logger.debug("First NormalLength: " + normalSize);
					logger.debug("First CompressedLength: " + compressedSize);
					logger.debug("First Compression: " + (compressedSize / normalSize));
					logger.debug("KRIMP: " + kAlgo.numberofUsedCandidates());
					logger.debug("------------- TIMES ---------------");					
					logger.debug("Time KRIMP: "+((double)(endKrimp-startKrimp)/1000000)+" ms.");
					
				} catch (Exception e) {
					logger.fatal("RAAAH", e);
				}
			}
		} catch (Exception e) {
			logger.fatal("Failed on " + Arrays.toString(args), e);
		}
	}
	

}
