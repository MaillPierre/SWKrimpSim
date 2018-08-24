///////////////////////////////////////////////////////////////////////////////
// File: UpdateSeparator.java 
// Author: Carlos Bobed
// Date: August 2018
// Comments: Utility to separate the triples in a model in different 
// 		updates following the format used for the DBpedia live experiments
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.utils;


import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.UtilOntology;

public class AdditionSeparator {

	public enum SeparationType {
		PLAIN_SEPARATION,
		CONTEXT_SEPARATION
	}; 
	
	private static Logger logger = Logger.getLogger(AdditionSeparator.class);
		
	public static String INPUT_MODEL_OPTION = "inputModel";
	public static String SEPARATION_TYPE_OPTION = "separationType";  
	public static String HELP_OPTION = "help";
	
	public static void main(String[] args) {
		// code adapted from Pierre's one ... thanks ;) 
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(INPUT_MODEL_OPTION, true, "filename with the model to be evolved");
		options.addOption(SEPARATION_TYPE_OPTION, true, "filename with the model to be evolved");
		options.addOption(HELP_OPTION, false, "display this help"); 
		
		try {
			CommandLine cmd = parser.parse(options, args);
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "AdditionSeparator", options );
			} else {
				String RDFFilename = cmd.getOptionValue(INPUT_MODEL_OPTION);
				SeparationType sepType = SeparationType.valueOf(cmd.getOptionValue(SEPARATION_TYPE_OPTION)); 
				logger.debug("Reading the RDF file ...");
				BaseRDF RDFWrapper = new BaseRDF(RDFFilename);
				logger.debug("Done");
				Model RDFModel = RDFWrapper.getModel();
				logger.debug("Initializing the ontological elements ...");
				UtilOntology ontology = new UtilOntology();
				ontology.init(RDFWrapper);
				logger.debug("Done");
				
				// we get all the subject instances 
				List<Resource> instances = new ArrayList<>(); 
				RDFModel.listSubjects().forEachRemaining(instances::add);
				
				for (Resource instance: instances) {
					
				}
				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
}
