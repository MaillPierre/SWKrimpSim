package com.irisa.swpatterns;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.BaseRDF.MODE;
import com.irisa.jenautils.QueryResultIterator;
import com.irisa.jenautils.UtilOntology;
import com.irisa.krimp.CodeTable;
import com.irisa.krimp.KrimpAlgorithm;
import com.irisa.krimp.data.DataAnalysis;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.LabeledTransactions;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class SWPatterns {
	
	private static Logger logger = Logger.getLogger(SWPatterns.class);

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("file", true, "RDF file");
		options.addOption("otherFile", true, "Other RDF file");
		options.addOption("endpoint", true, "Endpoint adress");
		options.addOption("output", true, "Output csv file");
		options.addOption("limit", true, "Limit to the number of individuals extracted from each class");
		options.addOption("resultWindow", true, "Size of the result window used to query RDF data.");
		options.addOption("classPattern", true, "Substring contained by the class uris.");
		options.addOption("noOut", false, "Not taking OUT properties into account.");
		options.addOption("noIn", false, "Not taking IN properties into account.");
		options.addOption("noTypes", false, "Not taking TYPES into account.");
		options.addOption("FPClose", false, "Use FPClose algorithm. (default)");
		options.addOption("FPMax", false, "Use FPMax algorithm.");
		options.addOption("FIN", false, "Use FIN algorithm.");
		options.addOption("PrePost", false, "Use PrePost algorithm.");
		options.addOption("FPGrowth", false, "Use FPGrowth algorithm.");
		options.addOption("Relim", false, "Use Relim algorithm.");
		options.addOption("class", true, "Class of the studied individuals.");
		options.addOption("nProperties", false, "Extract items representing only properties (central individual types, out-going and in-going properties).");
		options.addOption("nPropertiesAndTypes", false, "Extract items representing only properties and connected ressources types.");
		options.addOption("nPropertiesAndOthers", false, "Extract items representing properties and connected ressources.");
		options.addOption("transactionFile", false, "Only create a .dat transaction for each given file.");
		options.addOption("path", true, "Extract paths of length N.");
		options.addOption("help", false, "Display this help.");
		options.addOption("inputTransaction", true, "Transaction file (RDF data will be ignored).");
		options.addOption("otherInputTransaction", true, "Other transaction file (RDF data will be ignored).");
		// added for pruning 
		options.addOption("pruning", false, "Activate post-acceptance pruning"); 
	
		// Setting up options and constants etc.
		try {
			CommandLine cmd = parser.parse( options, args);
	
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "RDFtoTransactionConverter", options );
			} else {
				UtilOntology onto = new UtilOntology();
				TransactionsExtractor converter = new TransactionsExtractor();
				SWFrequentItemsetExtractor fsExtractor = new SWFrequentItemsetExtractor();
				ItemsetSet realtransactions ;
				Itemsets codes = null;
				
				boolean activatePruning = false; 
	
				String filename = cmd.getOptionValue("file");
				String otherFilename = cmd.getOptionValue("otherFile");
				String endpoint = cmd.getOptionValue("endpoint"); 
				String output = cmd.getOptionValue("output"); 
				String limitString = cmd.getOptionValue("limit");
				String resultWindow = cmd.getOptionValue("resultWindow");
				String classRegex = cmd.getOptionValue("classPattern");
				String className = cmd.getOptionValue("class");
				String pathOption = cmd.getOptionValue("path");
				converter.setNoTypeTriples( cmd.hasOption("noTypes") || converter.noTypeTriples());
				converter.noInTriples(cmd.hasOption("noIn") || converter.noInTriples());
				converter.setNoOutTriples(cmd.hasOption("noOut") || converter.noOutTriples());
				
				activatePruning = cmd.hasOption("pruning"); 

				if(cmd.hasOption("FPGrowth")) {
					fsExtractor.setAlgoFPGrowth();
				}
				if(cmd.hasOption("FPClose")) {
					fsExtractor.setAlgoFPClose();
				}
				if(cmd.hasOption("FPMax")) {
					fsExtractor.setAlgoFPMax();
				}
				if(cmd.hasOption("FIN")) {
					fsExtractor.setAlgoFIN();
				}
				if(cmd.hasOption("Relim")) {
					fsExtractor.setAlgoRelim();
				}
				if(cmd.hasOption("PrePost")) {
					fsExtractor.setAlgoPrePost();
				}
				if(cmd.hasOption("nProperties")) {
					converter.setNeighborLevel(Neighborhood.Property);
				}
				if(cmd.hasOption("nPropertiesAndTypes")) {
					converter.setNeighborLevel(Neighborhood.PropertyAndObjectType);
				}
				if(cmd.hasOption("nPropertiesAndOthers")) {
					converter.setNeighborLevel(Neighborhood.PropertyAndObject);
				}

				logger.debug("output: " + output + " limit:" + limitString + " resultWindow:" + resultWindow + " classpattern:" + classRegex + " noType:" + converter.noTypeTriples() + " noOut:" + converter.noOutTriples() + " noIn:"+ converter.noInTriples());
				logger.debug("Pruning activated: "+activatePruning);
			
				
				if(!cmd.hasOption("inputTransaction")) {
					if(limitString != null) {
						QueryResultIterator.setDefaultLimit(Integer.valueOf(limitString));
					}
					if(resultWindow != null) {
						QueryResultIterator.setDefaultLimit(Integer.valueOf(resultWindow));
					}
					if(cmd.hasOption("classPattern")) {
						UtilOntology.setClassRegex(classRegex);
					} else {
						UtilOntology.setClassRegex(null);
					}
	
					if(pathOption != null) {
						converter.setPathsLength(Integer.valueOf(pathOption));
					}
	
					BaseRDF baseRDF = null;
					if(filename != null) {
						baseRDF = new BaseRDF(filename, MODE.LOCAL);
					} else if (endpoint != null){
						baseRDF = new BaseRDF(endpoint, MODE.DISTANT);
					}
	
					logger.debug("initOnto");
					onto.init(baseRDF);
	
					logger.debug("extract");
					// Extracting transactions
	
					LabeledTransactions transactions;
	
					if(cmd.hasOption("class")) {
						Resource classRes = onto.getModel().createResource(className);
						transactions = converter.extractTransactionsForClass(baseRDF, onto, classRes);
					} else if(cmd.hasOption("path")) {
						transactions = converter.extractPathAttributes(baseRDF, onto);
					} else {
						transactions = converter.extractTransactions(baseRDF, onto);
					}
	
					AttributeIndex index = converter.getIndex();
	
					// Printing transactions
					if(cmd.hasOption("transactionFile")) {
						index.printTransactionsItems(transactions, filename + ".dat");
	
						if(cmd.hasOption("otherFile")) {
							index.printTransactionsItems(transactions, otherFilename + ".dat");
						}
					}
	
					realtransactions = index.convertToTransactions(transactions);
					codes = fsExtractor.computeItemsets(transactions, index);
					logger.debug("Nb Lines: " + realtransactions.size());
	
					if(cmd.hasOption("transactionFile")) {
						index.printTransactionsItems(transactions, filename + ".dat");
					}
					logger.debug("Nb items: " + converter.getIndex().size());
	
					baseRDF.close();
	
				} else {
					realtransactions = new ItemsetSet(Utils.readTransactionFile(cmd.getOptionValue("inputTransaction")));
					codes = fsExtractor.computeItemsets(realtransactions);
					logger.debug("Nb Lines: " + realtransactions.size());
				}
				ItemsetSet realcodes = new ItemsetSet(codes);
	
				try {
					DataAnalysis analysis = new DataAnalysis(realtransactions);
					CodeTable standardCT = CodeTable.createStandardCodeTable(realtransactions, analysis );
	
					KrimpAlgorithm kAlgo = new KrimpAlgorithm(realtransactions, realcodes);
					CodeTable krimpCT = kAlgo.runAlgorithm(activatePruning);
					double normalSize = standardCT.totalCompressedSize();
					double compressedSize = krimpCT.totalCompressedSize();
					logger.debug("-------- FIRST RESULT ---------");
					logger.debug(krimpCT);
					//					logger.debug("First Code table: " + krimpCT);
					logger.debug("First NormalLength: " + normalSize);
					logger.debug("First CompressedLength: " + compressedSize);
					logger.debug("First Compression: " + (compressedSize / normalSize));
	
	
					if(cmd.hasOption("otherFile")) {
	
						ItemsetSet otherRealTransactions;
						
						if(! cmd.hasOption("otherInputTransaction")) {
							LabeledTransactions otherTransactions;
							if(cmd.hasOption("class")) {
								Resource classRes = onto.getModel().createResource(className);
								otherTransactions = converter.extractTransactionsForClass(new BaseRDF(otherFilename, MODE.LOCAL),  onto, classRes);
							} else if(cmd.hasOption("path")) {
								otherTransactions = converter.extractPathAttributes(new BaseRDF(otherFilename, MODE.LOCAL),  onto);
							} else {
								otherTransactions = converter.extractTransactions(new BaseRDF(otherFilename, MODE.LOCAL),  onto);
							}
		
							otherRealTransactions = converter.getIndex().convertToTransactions(otherTransactions);
							if(cmd.hasOption("transactionFile")) {
								converter.getIndex().printTransactionsItems(otherTransactions, otherFilename + ".dat");
							}
						} else {
							otherRealTransactions = new ItemsetSet(Utils.readTransactionFile(cmd.getOptionValue("otherInputTransaction")));
						}
	
						logger.debug("Equals ? " + realtransactions.equals(otherRealTransactions));
						DataAnalysis otherAnalysis = new DataAnalysis(otherRealTransactions);
	
						standardCT = CodeTable.createStandardCodeTable(otherRealTransactions, otherAnalysis );
						CodeTable otherResult = new CodeTable( otherRealTransactions, krimpCT.getCodes(), otherAnalysis);
						double otherNormalSize = standardCT.totalCompressedSize();
						double otherCompressedSize = otherResult.totalCompressedSize();
						//					logger.debug("First Code table: " + krimpCT);
						logger.debug("Second NormalLength: " + otherNormalSize);
						logger.debug("Second CompressedLength: " + otherCompressedSize);
						logger.debug("Second Compression: " + (otherCompressedSize / otherNormalSize));
	
//						logger.debug("-------- OTHER RESULT ---------");
//						logger.debug(otherResult);

	
					}
	
				} catch (Exception e) {
					logger.fatal("RAAAH", e);
				}
				onto.close();
	
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
