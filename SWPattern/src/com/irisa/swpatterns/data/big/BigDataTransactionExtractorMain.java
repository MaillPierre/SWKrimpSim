package com.irisa.swpatterns.data.big;

import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.QueryResultIterator;
import com.irisa.jenautils.UtilOntology;
import com.irisa.jenautils.BaseRDF.MODE;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.SWFrequentItemsetExtractor;
import com.irisa.swpatterns.TransactionsExtractor;
import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.LabeledTransactions;

public class BigDataTransactionExtractorMain {
	
	private static Logger logger = Logger.getLogger(BigDataTransactionExtractorMain.class);

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		
		logger.debug(Arrays.toString(args));
		
		// In/out options name to facilitate further references
		String inputRDFOption = "inputRDF";
		String inputTransactionOption = "inputTransaction";
		String inputOtherRDFOption = "inputOtherRDF";
		String inputOtherTransactionOption = "inputOtherTransaction";
		String outputTransactionOption = "outputTransaction";
		String inputCandidatesOption = "inputCandidates";
		String inputOtherCandidatesOption = "inputOtherCandidates";
		String outputCandidatesOption = "outputCandidates";
		String inputCodeTableOption = "inputCodeTable";
		String inputOtherCodeTableOption = "inputOtherCodeTable";
		String outputCodeTableOption = "outputCodeTable";
		String inputConversionIndexOption = "inputConversionIndex";
		String outputConversionIndexOption = "outputConversionIndex";
		
		// CB: added to gather the results in CSV file
		String outputComparisonResultsFileOption = "outputComparisonResultsFile"; 
		
		String PropertiesConversionOption = "nProperties";
		String PropertiesAndTypesConversionOption = "nPropertiesAndTypes";
		String PropertiesAndOthersConversionOption = "nPropertiesAndOthers";
	
		OptionGroup algorithm = new OptionGroup();
		algorithm.addOption(new Option("FPClose", false, "Use FPClose algorithm. (default)"));
		algorithm.addOption(new Option("FPMax", false, "Use FPMax algorithm."));
		algorithm.addOption(new Option("FIN", false, "Use FIN algorithm."));
		algorithm.addOption(new Option("PrePost", false, "Use PrePost algorithm."));
		algorithm.addOption(new Option("FPGrowth", false, "Use FPGrowth algorithm."));
		algorithm.addOption(new Option("Relim", false, "Use Relim algorithm."));
		OptionGroup conversion = new OptionGroup();
		conversion.addOption(new Option(PropertiesConversionOption, false, "Extract items representing only properties (central individual types, out-going and in-going properties), encoding="+Neighborhood.Property+"."));
		conversion.addOption(new Option(PropertiesAndTypesConversionOption, false, "Extract items representing only properties and connected ressources types, encoding="+Neighborhood.PropertyAndType+"."));
		conversion.addOption(new Option(PropertiesAndOthersConversionOption, false, "Extract items representing properties and connected ressources, encoding="+Neighborhood.PropertyAndOther+"."));
		
		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		// In/Out
		options.addOption(inputRDFOption, true, "RDF file.");
		options.addOption(inputTransactionOption, true, "Transaction file (RDF data will be ignored).");
		options.addOption(inputOtherRDFOption, true, "Other RDF file to be compared to the first one.");
		options.addOption(inputOtherTransactionOption, true, "Other transaction file (RDF data will be ignored) to be compared to the first one.");
		options.addOption(outputTransactionOption, false, "Create a .dat transaction for each given RDF file named <filename>.<encoding>.dat .");
		options.addOption(inputCandidatesOption, true, "File containing the candidate codes extracted for the first file.");
		options.addOption(inputOtherCandidatesOption, true, "File containing the candidate codes extracted for the first file.");
		options.addOption(outputCandidatesOption, false, "Create file containing the candidate codes extracted for the first file <filename>.<encoding>.candidates.dat.");
		options.addOption(inputCodeTableOption, true, "Itemset file containing the KRIMP codetable for the first file.");
		options.addOption(inputOtherCodeTableOption, true, "Itemset file containing the KRIMP codetable for the other file.");
		options.addOption(outputCodeTableOption, false, "Create an Itemset file containing the  KRIMP codetable for each file <filename>.<encoding>.krimp.dat.");
		options.addOption(inputConversionIndexOption, true, "Set the index used for RDF to transaction conversion, new items will be added.");
		options.addOption(outputConversionIndexOption, true, "Create a file containing the index used for RDF to transaction conversion, new items will be added.");
		
		// CB: 
		options.addOption(outputComparisonResultsFileOption, true, "File to add the output of the comparison to in CSV format."); 
		
		options.addOption("limit", true, "Limit to the number of individuals extracted from each class.");
		options.addOption("resultWindow", true, "Size of the result window used to query RDF data.");
		options.addOption("classPattern", true, "Substring contained by the class uris.");
		options.addOption("class", true, "Class of the selected individuals.");
		// Boolean behavioral options
		options.addOptionGroup(algorithm);
		options.addOptionGroup(conversion);
		options.addOption("pruning", false, "Activate post-acceptance pruning for better quality code table but slower performances."); 
		options.addOption("noOut", false, "Not taking OUT properties into account for RDF conversion.");
		options.addOption("noIn", false, "Not taking IN properties into account for RDF conversion.");
		options.addOption("noTypes", false, "Not taking TYPES into account for RDF conversion.");
		options.addOption("path", true, "Extract paths of length N. (Here be dragons)");
		options.addOption("help", false, "Display this help.");
	
		// Setting up options and constants etc.
		try {
			CommandLine cmd = parser.parse( options, args);
	
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "RDFtoTransactionConverter", options );
			} else {
				boolean inputRDF = cmd.hasOption(inputRDFOption);
				boolean inputTransaction = cmd.hasOption(inputTransactionOption);
				cmd.hasOption(inputOtherRDFOption);
				cmd.hasOption(inputOtherTransactionOption);
				cmd.hasOption(inputOtherCodeTableOption);
				boolean outputTransaction = cmd.hasOption(outputTransactionOption);
				cmd.hasOption(inputOtherCandidatesOption);
				cmd.hasOption(outputCandidatesOption);
				cmd.hasOption(inputCodeTableOption);
				cmd.hasOption(outputCodeTableOption);
				boolean inputConversionIndex = cmd.hasOption(inputConversionIndexOption);
				cmd.hasOption(outputConversionIndexOption);
				boolean classPattern = cmd.hasOption("classPattern");
				
//				UtilOntology onto = new UtilOntology();
				BigDataTransactionExtractor converter = new BigDataTransactionExtractor();
//				TransactionsExtractor normalConverter = new TransactionsExtractor();
//				SWFrequentItemsetExtractor fsExtractor = new SWFrequentItemsetExtractor();
				ItemsetSet realtransactions ;
				// Boolean options
				boolean activatePruning = cmd.hasOption("pruning"); 
				converter.setNoTypeTriples( cmd.hasOption("noTypes") || converter.noTypeTriples());
				converter.noInTriples(cmd.hasOption("noIn") || converter.noInTriples());
				converter.setNoOutTriples(cmd.hasOption("noOut") || converter.noOutTriples());
					// Algorithm are excluding each other
//				if(cmd.hasOption("FPGrowth")) {
//					fsExtractor.setAlgoFPGrowth();
//				}
//				else if(cmd.hasOption("FPClose")) {
//					fsExtractor.setAlgoFPClose();
//				}
//				else if(cmd.hasOption("FPMax")) {
//					fsExtractor.setAlgoFPMax();
//				}
//				else if(cmd.hasOption("FIN")) {
//					fsExtractor.setAlgoFIN();
//				}
//				else if(cmd.hasOption("Relim")) {
//					fsExtractor.setAlgoRelim();
//				}
//				else if(cmd.hasOption("PrePost")) {
//					fsExtractor.setAlgoPrePost();
//				}
				// Encoding options
				if(cmd.hasOption(PropertiesConversionOption)) {
					converter.setNeighborLevel(Neighborhood.Property);
				}
				if(cmd.hasOption(PropertiesAndTypesConversionOption)) {
					converter.setNeighborLevel(Neighborhood.PropertyAndType);
				}
				if(cmd.hasOption(PropertiesAndOthersConversionOption)) {
					converter.setNeighborLevel(Neighborhood.PropertyAndOther);
				}
//				normalConverter.setNeighborLevel(converter.getNeighborLevel());
				logger.debug("Pruning activated: "+activatePruning);
				
				// NO MODE cmd.hasOption() past this point
	
				String firstRDFFile = cmd.getOptionValue(inputRDFOption);
				cmd.getOptionValue(inputOtherRDFOption);
				String firstTransactionFile = cmd.getOptionValue(inputTransactionOption);
				cmd.getOptionValue(inputOtherTransactionOption);
				cmd.getOptionValue(inputCandidatesOption);
				cmd.getOptionValue(inputOtherCandidatesOption);
				cmd.getOptionValue(inputCodeTableOption);
				cmd.getOptionValue(inputOtherCodeTableOption);
				String firstOutputFile = "";
				cmd.getOptionValue(outputComparisonResultsFileOption); 
				
				if(inputRDF) {
					firstOutputFile = firstRDFFile;
				} else if(inputTransaction) {
					firstOutputFile = firstTransactionFile;
				}
				firstOutputFile += "." + converter.getNeighborLevel();
				String firstOutputTransactionFile = firstOutputFile + ".dat";
				String inputConversionIndexFile = cmd.getOptionValue(inputConversionIndexOption);
				String outputConversionIndexFile = cmd.getOptionValue(outputConversionIndexOption);
				if(! cmd.hasOption(outputConversionIndexOption)) {
					outputConversionIndexFile = "conversionIndex.attr";
				}
				
				// RDF handling options
				String limitString = cmd.getOptionValue("limit");
				String resultWindow = cmd.getOptionValue("resultWindow");
				String classRegex = cmd.getOptionValue("classPattern");
				cmd.getOptionValue("class");
				cmd.getOptionValue("path");
			
				
				if(! inputTransaction) {
					if(limitString != null) {
						QueryResultIterator.setDefaultLimit(Integer.valueOf(limitString));
					}
					if(resultWindow != null) {
						QueryResultIterator.setDefaultLimit(Integer.valueOf(resultWindow));
					}
					if(classPattern) {
						UtilOntology.setClassRegex(classRegex);
					} else {
						UtilOntology.setClassRegex(null);
					}
		
//					BaseRDF baseRDF = new BaseRDF(firstRDFFile, MODE.LOCAL);
	
//					onto.init(baseRDF);
	
					logger.debug("Extracting transactions from RDF file with conversion " + converter.getNeighborLevel());

					
					AttributeIndex index = AttributeIndex.getInstance();
					
					// Extracting transactions
					ItemsetSet transactionsBigData;
//					LabeledTransactions transactionsNormal;
					if(inputConversionIndex) {
						index.readAttributeIndex(inputConversionIndexFile);
					}
	
						transactionsBigData = converter.extractTransactionsFromFile(firstRDFFile);
//						transactionsNormal = normalConverter.extractTransactions(baseRDF, onto);
						// Printing transactions for both files
						if(outputTransaction) {
//							Utils.printDebugTransactions(index.convertToTransactions(transactionsNormal), firstOutputTransactionFile + ".normal");
							Utils.printTransactions(transactionsBigData, firstOutputTransactionFile);
							logger.debug("Transactions printed to " + firstOutputTransactionFile);
							AttributeIndex.getInstance().printAttributeIndex(outputConversionIndexFile);
							logger.debug("Index printed to " + outputConversionIndexFile);
						}
					
					logger.debug("Nb transactions: " + transactionsBigData.size());
					logger.debug("Nb items: " + index.size());
					
//					baseRDF.close();
	
				} else {
					realtransactions = new ItemsetSet(Utils.readTransactionFile(cmd.getOptionValue(inputTransactionOption)));
					logger.debug("Nb Lines: " + realtransactions.size());
				}
//				onto.close();
			}
		} catch (Exception e) {
			logger.fatal("Failed on " + Arrays.toString(args), e);
		}
	}

}
