package com.irisa.swpatterns.krimp;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.QueryResultIterator;
import com.irisa.jenautils.UtilOntology;
import com.irisa.jenautils.BaseRDF.MODE;
import com.irisa.swpatterns.FrequentItemSetExtractor;
import com.irisa.swpatterns.TransactionsExtractor;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.ItemsetSet;
import com.irisa.swpatterns.data.LabeledTransactions;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * KRIMP magic happens here !
 * @author pmaillot
 *
 */
public class KrimpImpl {
	
	private static Logger logger = Logger.getLogger(KrimpImpl.class);

	private CodeTable _candidates = null;
	private ItemsetSet _transactions = null;

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("file", true, "RDF file");
		options.addOption("endpoint", true, "Endpoint adress");
		options.addOption("output", true, "Output csv file");
		options.addOption("limit", true, "Limit to the number of individuals extracted");
		options.addOption("resultWindow", true, "Size of the result window used to query servers.");
		options.addOption("classPattern", true, "Substring contained by the class uris.");
		options.addOption("noOut", false, "Not taking OUT properties into account.");
		options.addOption("noIn", false, "Not taking IN properties into account.");
		options.addOption("noTypes", false, "Not taking TYPES into account.");
		options.addOption("FPClose", false, "Use FPClose algorithm. (default)");
		options.addOption("FPMax", false, "Use FPMax algorithm.");
		options.addOption("class", true, "Class of the studied individuals.");
		options.addOption("rank1", false, "Extract informations up to rank 1 (types, out-going and in-going properties and object types), default is only types, out-going and in-going properties.");
		//		options.addOption("rank0", false, "Extract informations up to rank 0 (out-going and in-going properties.");
		options.addOption("path", true, "Use FPClose algorithm. (default)");
		options.addOption("help", false, "Display this help.");

		// Setting up options and constants etc.
		UtilOntology onto = new UtilOntology();
		try {
			CommandLine cmd = parser.parse( options, args);

			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "RDFtoTransactionConverter", options );
			} else {
				TransactionsExtractor converter = new TransactionsExtractor();
				FrequentItemSetExtractor fsExtractor = new FrequentItemSetExtractor();

				String filename = cmd.getOptionValue("file");
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
				fsExtractor.setAlgoFPClose(cmd.hasOption("FPClose") || fsExtractor.algoFPClose() );
				fsExtractor.setAlgoFPMax(cmd.hasOption("FPMax") || fsExtractor.algoFPMax() );
				converter.setRankOne(cmd.hasOption("rank1") || converter.isRankOne());

				String outputTransactions = "transactions."+filename + ".dat"; 
				String outputRDFPatterns = "rdfpatternes."+filename+".ttl"; 
				logger.debug("output: " + output + " limit:" + limitString + " resultWindow:" + resultWindow + " classpattern:" + classRegex + " noType:" + converter.noTypeTriples() + " noOut:" + converter.noOutTriples() + " noIn:"+ converter.noInTriples());

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

				try {
					converter.getIndex().printTransactionsItems(transactions, outputTransactions);
					ItemsetSet realtransactions = converter.getIndex().convertToTransactions(transactions);
					Itemsets codes = fsExtractor.computeItemsets(transactions, converter.getIndex());
					ItemsetSet realcodes = new ItemsetSet(codes, converter.getIndex());
					CodeTable codeTab = new CodeTable(converter.getIndex(), realtransactions, realcodes );
					logger.debug("Nb items: " + converter.getIndex().size());
					logger.debug(" Code table: " + codeTab);
				} catch (Exception e) {
					logger.fatal("RAAAH", e);
				}

				baseRDF.close();

			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		onto.close();
	}
	
}
