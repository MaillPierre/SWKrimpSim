package com.irisa.swpatterns;

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
import com.irisa.krimp.CodeTable;
import com.irisa.krimp.KrimpAlgorithm;
import com.irisa.krimp.data.DataIndexes;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.LabeledTransactions;

public class TransanctionsExtractorMain {
	
	private static Logger logger = Logger.getLogger(TransanctionsExtractorMain.class);

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		
		logger.debug(Arrays.toString(args));
		
		// In/out options name to facilitate further references
		String inputRDFOption = "inputRDF";
		String outputTransactionOption = "outputTransaction";
		String inputConversionIndexOption = "inputConversionIndex";
		String outputConversionIndexOption = "outputConversionIndex";
		
		String PropertiesConversionOption = "nProperties";
		String PropertiesAndTypesConversionOption = "nPropertiesAndTypes";
		String PropertiesAndOthersConversionOption = "nPropertiesAndOthers";
	
		
		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		// In/Out
		options.addOption(inputRDFOption, true, "RDF file.");
		options.addOption(outputTransactionOption, true, "Create a .dat transaction for each given RDF file named <filename>.<encoding>.dat .");
		options.addOption(inputConversionIndexOption, true, "Set the index used for RDF to transaction conversion, new items will be added.");
		options.addOption(outputConversionIndexOption, true, "Create a file containing the index used for RDF to transaction conversion, new items will be added.");
		options.addOption(PropertiesConversionOption, false, ""); 
		options.addOption(PropertiesAndTypesConversionOption, false, ""); 
		options.addOption(PropertiesAndOthersConversionOption, false, ""); 
		
		
		// Setting up options and constants etc.
		try {
			CommandLine cmd = parser.parse( options, args);
	
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "RDFtoTransactionConverter", options );
			} else {
				boolean inputRDF = cmd.hasOption(inputRDFOption);
				boolean outputTransaction = cmd.hasOption(outputTransactionOption);
				boolean inputConversionIndex = cmd.hasOption(inputConversionIndexOption);
				boolean outputConversionIndex = cmd.hasOption(outputConversionIndexOption);
				
				TransactionsExtractor converter = new TransactionsExtractor();
				

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
				
				// NO MODE cmd.hasOption() past this point
	
				String firstRDFFile = cmd.getOptionValue(inputRDFOption);
				
				String inputConversionIndexFile = cmd.getOptionValue(inputConversionIndexOption);
				
				AttributeIndex index = converter.getIndex();
				
				// Extracting transactions
				if(inputConversionIndex) {
					index.readAttributeIndex(inputConversionIndexFile);
				}
				ItemsetSet transactions = converter.extractTransactions(firstRDFFile); 
				System.out.println("Transactions extracted "); 
				System.out.println("Writing to "+cmd.getOptionValue(outputTransactionOption));
				Utils.printTransactions(transactions, cmd.getOptionValue(outputTransactionOption));
				index.printAttributeIndex(cmd.getOptionValue(outputConversionIndexOption));
					
				} 
			}
		 catch (Exception e) {
			logger.fatal("Failed on " + Arrays.toString(args), e);
		}
	}
	

}
