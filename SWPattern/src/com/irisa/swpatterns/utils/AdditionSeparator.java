///////////////////////////////////////////////////////////////////////////////
// File: AdditionSeparator.java 
// Author: Carlos Bobed
// Date: August 2018
// Comments: Utility to separate the triples in a model in different 
// 		updates following the format used for the DBpedia live experiments
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.utils;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.ChangesetFile;
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
	public static String OUTPUT_DIRECTORY_OPTION = "outputDirectory"; 
	public static String HELP_OPTION = "help";
	
	public static void main(String[] args) {
		// code adapted from Pierre's one ... thanks ;) 
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(INPUT_MODEL_OPTION, true, "filename with the model to be evolved");
		options.addOption(SEPARATION_TYPE_OPTION, true, "filename with the model to be evolved");
		options.addOption(OUTPUT_DIRECTORY_OPTION, true, "directory to store the update files"); 
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
				String directoryName = cmd.getOptionValue(OUTPUT_DIRECTORY_OPTION);
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

				List<Statement> incomingTriples = new ArrayList<Statement> ();
				List<Statement> outgoingTriples = new ArrayList<Statement> (); 
				int updateId=1; 
				for (Resource instance: instances) {
					incomingTriples.clear();
					outgoingTriples.clear();
					// we list as an update all the triples affecting the instance, both in and out ones
					RDFModel.listStatements(new SimpleSelector(instance, null, (Object) null)).forEachRemaining(outgoingTriples::add);
					RDFModel.listStatements(new SimpleSelector(null, null, instance)).forEachRemaining(incomingTriples::add);
					logger.debug(instance.getURI()+" outgoingTriples: "+outgoingTriples.size());
					logger.debug(instance.getURI()+" incomingTriples: "+incomingTriples.size());
					
					// we create the model
					Model updateModel =  ModelFactory.createDefaultModel();
					updateModel.add(incomingTriples); 
					updateModel.add(outgoingTriples); 
					
					switch (sepType) {
						case PLAIN_SEPARATION:
							// we do nothing 
							break;
						case CONTEXT_SEPARATION:
							// we have to extend the instances in the incoming and outcoming triples with their type
							Set<Resource> instancesToExpand= new HashSet<>();
							// for the incoming triples, all the subjects are instances
							incomingTriples.forEach(stmt -> instancesToExpand.add(stmt.getSubject()));
							// we just keep the instances
							outgoingTriples.forEach(stmt -> { 
										if (stmt.getObject().isResource()) {
											instancesToExpand.add(stmt.getObject().asResource()); 
										}								
							});
							
							instancesToExpand.forEach(inst -> 
										updateModel.add(RDFModel.listStatements(new SimpleSelector(inst, RDF.type, (Resource) null))) 
							); 
						break; 
					}
					
					
					OutputStream out = new GZIPOutputStream(new FileOutputStream(new File(directoryName+File.separator
																+"upd-"+String.format("%016d", updateId))+ChangesetFile.ADDED_EXTENSION)); 
					RDFDataMgr.write(out, updateModel, RDFFormat.NTRIPLES_UTF8);
					updateModel.close(); 
					out.flush(); 
					out.close();
					
					updateId++; 
				}
				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
}
