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

import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.swpatterns.data.AttributeIndex;

public class BigDataTransactionExtractorMain {

	private static Logger logger = Logger.getLogger(BigDataTransactionExtractorMain.class);

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

		logger.debug(Arrays.toString(args));

		// In/out options name to facilitate further references
		String inputRDFOption = "input";
		String outputTransactionOption = "output";
		String inputConversionIndexOption = "inputIndex";
		String outputConversionIndexOption = "outputIndex";
		

		String PropertiesConversionOption = "nProperties";
		String PropertiesAndTypesConversionOption = "nPropertiesAndTypes";

		OptionGroup conversion = new OptionGroup();
		conversion.addOption(new Option(PropertiesConversionOption, false, "Extract items representing only properties (central individual types, out-going and in-going properties), encoding="+Neighborhood.Property+"."));
		conversion.addOption(new Option(PropertiesAndTypesConversionOption, false, "Extract items representing only properties and connected ressources types, encoding="+Neighborhood.PropertyAndType+"."));

		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		// In/Out
		options.addOption(inputRDFOption, true, "RDF file.");
		options.addOption(outputTransactionOption, true, "By default, will create a .dat transaction file named <filename>.<encoding>.dat .");
		options.addOption(inputConversionIndexOption, true, "Set the index used for RDF to transaction conversion, new items will be added.");
		options.addOption(outputConversionIndexOption, true, "Create a file containing the index used for RDF to transaction conversion, new items will be added.");

		//		options.addOption("classPattern", true, "Substring contained by the class uris."); // To be reimplemented ?
		//		options.addOption("class", true, "Class of the selected individuals."); // To be reimplemented ?
		// Boolean behavioral options
		options.addOptionGroup(conversion);
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
				boolean outputTransaction = cmd.hasOption(outputTransactionOption);
				boolean inputConversionIndex = cmd.hasOption(inputConversionIndexOption);
				cmd.hasOption(outputConversionIndexOption);

				//				UtilOntology onto = new UtilOntology();
				BigDataTransactionExtractor converter = new BigDataTransactionExtractor();

				// Encoding options
				if(cmd.hasOption(PropertiesConversionOption)) {
					converter.setNeighborLevel(Neighborhood.Property);
				}
				if(cmd.hasOption(PropertiesAndTypesConversionOption)) {
					converter.setNeighborLevel(Neighborhood.PropertyAndType);
				}

				String rdfFile = cmd.getOptionValue(inputRDFOption);
				String transactOutputFile = "";

				if(cmd.hasOption(outputTransactionOption)) {
					transactOutputFile = cmd.getOptionValue(outputTransactionOption);
				} else {
					transactOutputFile = rdfFile + "." + converter.getNeighborLevel() + ".dat";
				}
				String inputConversionIndexFile = cmd.getOptionValue(inputConversionIndexOption);
				String outputConversionIndexFile = cmd.getOptionValue(outputConversionIndexOption);
				if(! cmd.hasOption(outputConversionIndexOption)) {
					outputConversionIndexFile = "conversionIndex.idx";
				}

				// RDF handling options
				logger.debug("Extracting transactions from RDF file with conversion " + converter.getNeighborLevel());

				AttributeIndex index = AttributeIndex.getInstance();

				// Extracting transactions
				ItemsetSet transactionsBigData;
				if(inputConversionIndex) {
					index.readAttributeIndex(inputConversionIndexFile);
				}

				transactionsBigData = converter.extractTransactionsFromFile(rdfFile);
				if(! transactionsBigData.isEmpty()) {
					// Printing transactions for both files
//					if(outputTransaction) {
						Utils.printTransactions(transactionsBigData, transactOutputFile);
						logger.debug(transactionsBigData.size() + " transactions printed to " + transactOutputFile);
						AttributeIndex.getInstance().printAttributeIndex(outputConversionIndexFile);
						logger.debug("Index printed to " + outputConversionIndexFile);
//					}

					logger.debug("Nb transactions: " + transactionsBigData.size());
					logger.debug("Nb items: " + index.size());
				} else {
					logger.fatal("No transaction returned");
				}


			}
		} catch (Exception e) {
			logger.fatal("Failed on " + Arrays.toString(args), e);
		}
	}

}
