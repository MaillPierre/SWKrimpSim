///////////////////////////////////////////////////////////////////////////////
// File: UpdateGenerator.java 
// Author: Carlos Bobed
// Date: August 2018
// Comments: Utility to build synthetic pairs of updates: 
// 		- a first "bad" one, which deviates an instance and its context from 
// 		the observed structure 
// 		- its contrary to check whether it's captured as a good evolution one
// 		(going back to a previous state which was better)
// 		We can work with all the context of each instance (CONTEXT_SEPARATION) 
// 		or just the one-step neighborhood (PLAIN_SEPARATION).
// 		We do not consider to modify in a property-centered way anymore as 
// 		currently it is enough to modify the instance neighbourhood/context, as 
// 		we want to keep the update scope more controlled
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.utils;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class UpdaterGenerator {

	public enum SeparationType {
		PLAIN_SEPARATION,
		CONTEXT_SEPARATION
	}; 
	
	private static Logger logger = Logger.getLogger(UpdaterGenerator.class);
		
	public static String INPUT_MODEL_OPTION = "inputModel";
	public static String SEPARATION_TYPE_OPTION = "separationType";  
	public static String OUTPUT_DIRECTORY_OPTION = "outputDirectory";
	public static String MODIFYING_PERCENTAGE_OPTION = "percentage"; 
	public static String DELETE_PROBABILITY_OPTION ="deletingProbability"; 
	public static String MODIFY_OBJECT_OPTION = "modObjectProbability";
//	DELETE_PROBABILITY + MODIFY_OBJECT + MODIFY_PROPERTY == 1
//	public static String MODIFY_PROPERTY_OPTION = "modPropertyProbability"; 
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
		options.addOption(MODIFYING_PERCENTAGE_OPTION, true, "percentage of the context to be modified"); 
		options.addOption(DELETE_PROBABILITY_OPTION, true, "probability of deleting a triple");
		options.addOption(MODIFY_OBJECT_OPTION, true, "probabilty of modifying the object of a triple"); 
		
		options.addOption(HELP_OPTION, false, "display this help"); 
		
		try {
			CommandLine cmd = parser.parse(options, args);
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "UpdaterGenerator", options );
			} else {
				String RDFFilename = cmd.getOptionValue(INPUT_MODEL_OPTION);
				SeparationType sepType = SeparationType.valueOf(cmd.getOptionValue(SEPARATION_TYPE_OPTION));
				String directoryName = cmd.getOptionValue(OUTPUT_DIRECTORY_OPTION);
				Double modPercentage = Double.valueOf(cmd.getOptionValue(MODIFYING_PERCENTAGE_OPTION)); 
				Double deleteProbability = Double.valueOf(cmd.getOptionValue(DELETE_PROBABILITY_OPTION)); 
				Double modObjectProbability = Double.valueOf(cmd.getOptionValue(MODIFY_OBJECT_OPTION)); 
				Double modPropertyProbability = 1 - (deleteProbability + modObjectProbability); 
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
				List<Resource> properties = new ArrayList<> (); 
				ontology.properties().forEach(properties::add);

				int updateId=1;
				Random rand = new Random(); 
				double nextMod = 0.0; 
				
				for (Resource instance: instances) {
					// it is exactly the same code used for the addition separator
					Model updateModel = getUpdateModelForInstance(RDFModel, instance, sepType); 
					// we can now focus on messing up this instance (carefully ...) 
					ArrayList<Statement> candidateTriples = new ArrayList<>(); 
					updateModel.listStatements().forEachRemaining(candidateTriples::add);
					
					// triples to modify 
					long triplesToModify = (long) Math.floor(modPercentage * candidateTriples.size());
					long modifiedTriples = 0; 
					
					// we should check that the initial and final transactions are the same 
					// in pairs 
					ArrayList<Statement> addFirstUpdate = new ArrayList<>();
					ArrayList<Statement> delFirstUpdate = new ArrayList<>();
					ArrayList<Statement> addSecondUpdate = new ArrayList<>();
					ArrayList<Statement> delSecondUpdate = new ArrayList<>();
					int auxIdx = 0; 
					int auxInstanceIdx = 0;
					int auxPropertyIdx = 0; 
					Statement selectedTriple = null; 
					Statement addedStatement = null; 
					
					while (modifiedTriples < triplesToModify) {
						nextMod = rand.nextDouble();
						auxIdx = rand.nextInt(candidateTriples.size());
						assert (0.0 <= nextMod && nextMod <= 1.0); 
						selectedTriple = candidateTriples.get(auxIdx); 
						if (nextMod <= deleteProbability) {
							// we delete the triple in the first place
							delFirstUpdate.add(selectedTriple); 
							// we restore the triple in the second update
							addSecondUpdate.add(selectedTriple); 
						}
						else if (nextMod <= modObjectProbability) {
							// we make sure that we modify the object of the triple
							auxInstanceIdx = rand.nextInt(instances.size()); 
							while (candidateTriples.get(auxIdx).getObject().equals(instances.get(auxInstanceIdx))) {
								auxInstanceIdx = rand.nextInt(instances.size()); 
							}
							addedStatement = RDFModel.createStatement(selectedTriple.getSubject(), selectedTriple.getPredicate(), 
														instances.get(auxInstanceIdx));  
							
							addFirstUpdate.add(addedStatement); 
							delFirstUpdate.add(selectedTriple); 
							
							addSecondUpdate.add(selectedTriple);
							delSecondUpdate.add(addedStatement); 							
						}
						else {
							// we then modify the property
							// we make sure that we modify the property of the triple
							auxPropertyIdx = rand.nextInt(properties.size()); 
							while (candidateTriples.get(auxIdx).getPredicate().equals(properties.get(auxPropertyIdx))) {
								auxPropertyIdx = rand.nextInt(properties.size());						
							}
							
							addedStatement = RDFModel.createStatement(selectedTriple.getSubject(), 
												RDFModel.getProperty(properties.get(auxPropertyIdx).getURI()), 
												selectedTriple.getObject());
							addFirstUpdate.add(addedStatement); 
							delFirstUpdate.add(selectedTriple); 
							
							addSecondUpdate.add(selectedTriple);
							delSecondUpdate.add(addedStatement);
						}
						candidateTriples.remove(auxIdx); 
						modifiedTriples++; 
					}
					
					// we have to write two pairs of files 
					// we codify the order with the last integer in the name 
					// 0 is a bad update
					// 1 is a good one
					
					writeUpdate(addFirstUpdate,
							directoryName+File.separator+"upd-"+String.format("%016d", updateId)+"0"+ChangesetFile.ADDED_EXTENSION); 
					
					writeUpdate(delFirstUpdate,
							directoryName+File.separator+"upd-"+String.format("%016d", updateId)+"0"+ChangesetFile.DELETED_EXTENSION); 
					
					writeUpdate(addSecondUpdate,
							directoryName+File.separator+"upd-"+String.format("%016d", updateId)+"1"+ChangesetFile.ADDED_EXTENSION);
					
					writeUpdate(delSecondUpdate,
							directoryName+File.separator+"upd-"+String.format("%016d", updateId)+"1"+ChangesetFile.DELETED_EXTENSION);

					updateId++; 
				}
				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
	
	public static void writeUpdate(ArrayList<Statement> triples, String filename) throws FileNotFoundException, IOException {
		Model tmpModel = ModelFactory.createDefaultModel();
		tmpModel.add(triples); 
		OutputStream out = new GZIPOutputStream(new FileOutputStream(new File(filename))); 
		RDFDataMgr.write(out, tmpModel, RDFFormat.NTRIPLES_UTF8);
		tmpModel.close(); 
		out.flush(); 
		out.close();
		
	}
	
	public static Model getUpdateModelForInstance (Model baseModel, Resource inst, SeparationType sepType) {

		// in fact this is exactly equal to AdditionSeparator
		ArrayList<Statement> incomingTriples = new ArrayList<>();
		ArrayList<Statement> outgoingTriples = new ArrayList<>();
		// we list as an update all the triples affecting the instance, both in and out ones
		baseModel.listStatements(new SimpleSelector(inst, null, (Object) null)).forEachRemaining(outgoingTriples::add);
		baseModel.listStatements(new SimpleSelector(null, null, inst)).forEachRemaining(incomingTriples::add);
		logger.debug(inst.getURI()+" outgoingTriples: "+outgoingTriples.size());
		logger.debug(inst.getURI()+" incomingTriples: "+incomingTriples.size());
		
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
				instancesToExpand.forEach(lambdaInst -> 
							updateModel.add(baseModel.listStatements(new SimpleSelector(lambdaInst, RDF.type, (Resource) null))) 
				); 
			break; 
		}
		return updateModel; 
	}

}
