///////////////////////////////////////////////////////////////////////////////
// File: RDFDisrupter.java 
// Author: Carlos Bobed
// Date: July 2017
// Comments: Utility to get an RDF model simulating different modeling 
// 		differences 
// Modifications: 
// 		July 2017: 
// 			* the completely random version previously used lead to 
// 			huge closed frequent itemset sets which were non-informative (the 
// 			randomness did not respect ranges and domains of the properties, 
//	 		which are more than likely to be respected)
// 			* Added two different styles of modifying the RDFGraph: 
// 				- instance-centered modification
// 				- property-centered modification
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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.UtilOntology;

import scala.Array;

public class RDFDisrupter {

	public enum MODIFICATION_TYPE {
		INSTANCE_CENTERED, 
		PROPERTY_CENTERED
	}; 
	
	private static Logger logger = Logger.getLogger(RDFDisrupter.class);
	
	public static double deletingProbability = 0.2; 
	public static double modifiedPercentage = 0.1; 
	public static ArrayList<MODIFICATION_TYPE> activeModifications = new ArrayList<>(); 
	
	public static void main(String[] args) {
		// code adapted from Pierre's one ... thanks ;) 
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		
		// In/out options name to facilitate further references
		String inputRDFOption = "inputRDF";
		String deletingProbabilityOption= "deletingProbability"; 
		String modifiedPercentageOption = "modifiedPercentage";
		String outputRDFOption = "outputRDF"; 
		String instanceCenteredOption = "instanceCentered"; 
		String propertyCenteredOption = "propertyCentered"; 
		
		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		// In/Out
		options.addOption(inputRDFOption, true, "Input RDF file");
		options.addOption(outputRDFOption, true, "Output RDF file"); 
		options.addOption(deletingProbabilityOption, true, "Probability of deleting instead of modifying a triple"); 
		options.addOption(modifiedPercentageOption, true, "Percentage of the KB that has to be modified");
		options.addOption(instanceCenteredOption, false, "Activate the instanceCentered (default true if no option is given)"); 
		options.addOption(propertyCenteredOption, false, "Activate the propertyCentered (default false)"); 
		
		try {
			CommandLine cmd = parser.parse(options, args);
		
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "RDFDisrupter", options );
			} else {
				String RDFFile = cmd.getOptionValue(inputRDFOption);
				String outputFile = null;  
				if (cmd.hasOption(outputRDFOption)){
					outputFile = cmd.getOptionValue(outputRDFOption); 
				}
				else{
					outputFile = RDFFile+"-modified.nt"; 
				}
				if (cmd.hasOption(deletingProbabilityOption)) {
					deletingProbability = Double.valueOf(cmd.getOptionValue(deletingProbabilityOption)); 
				}
				if (cmd.hasOption(modifiedPercentageOption)) {
					modifiedPercentage = Double.valueOf(cmd.getOptionValue(modifiedPercentageOption)); 
				}
				
				if (cmd.hasOption(instanceCenteredOption) || 
						cmd.hasOption(propertyCenteredOption) ) {
					if (cmd.hasOption(instanceCenteredOption)) {
						activeModifications.add(MODIFICATION_TYPE.INSTANCE_CENTERED); 
					}
					
					if (cmd.hasOption(propertyCenteredOption)) {
						activeModifications.add(MODIFICATION_TYPE.PROPERTY_CENTERED); 
					}
				}
				
				logger.debug("Reading the RDF file ...");
				BaseRDF RDFWrapper = new BaseRDF(RDFFile);
				logger.debug("Done");
				Model RDFModel = RDFWrapper.getModel();
				logger.debug("Initializing the ontological elements ...");
				UtilOntology ontology = new UtilOntology();
				ontology.init(RDFWrapper);
				logger.debug("Done");
				long totalNumberModifiedTriples = -1; 
				long currentNumberModifiedTriples = 0; 
				long numberOfDeletions = 0; 
				long numberOfFlippings = 0; 
				StmtIterator it = null; 
				Statement stmt = null; 
				Random rand = new Random(); 
				int auxIdx = -1; 
				
				List<Statement> triples = new ArrayList<>();
				List<Resource> instances = new ArrayList<>(); 
				RDFModel.listSubjects().forEachRemaining(instances::add);
				
				// this could be done far more elegantly using lambda expressions 
				// e.j.: RDFModel.listStatements().forEachRemaining(triples::add); 
				
				it = RDFModel.listStatements();
				stmt = null; 
				while (it.hasNext()) {
					stmt = it.next(); 
					if (!ontology.isOntologyPropertyVocabulary(stmt.getPredicate())) {
						// we add it to the candidate list 
						triples.add(stmt); 
					}
				}
				totalNumberModifiedTriples = (long) Math.floor(modifiedPercentage * triples.size());
				logger.debug("Messing with "+totalNumberModifiedTriples+" triples ... ");
				ArrayList<Statement> stmtToBeAdded = new ArrayList<>(); 
				ArrayList<Statement> stmtToBeRemoved = new ArrayList<>(); 
				
				while (currentNumberModifiedTriples < totalNumberModifiedTriples) {
					switch (nextModification(rand, activeModifications)) {
						case INSTANCE_CENTERED: 
							
							if (instances.isEmpty()){
								// we initialize the candidates 
								RDFModel.listSubjects().forEachRemaining(instances::add);
							}
							
							// we select a resource that is not part of the TBox
							auxIdx = rand.nextInt(instances.size());
							Resource instance = instances.get(auxIdx); 
							while (!instances.isEmpty() && 
									(ontology.isClass(instance) || ontology.isProperty(instance)) )  {
								// we choose another one
								instances.remove(auxIdx);
								if (instances.size () != 0) {
									auxIdx = rand.nextInt(instances.size());
									instance = instances.get(auxIdx);
								}
							}
							if (instances.isEmpty()) 
							{
								// we break the loop 
								continue; 
							}
							
							List<Statement> instanceTriples = new ArrayList<Statement> (); 
							RDFModel.listStatements(new SimpleSelector(instance, null, (RDFNode)null)).forEachRemaining(instanceTriples::add); 
							while (currentNumberModifiedTriples <totalNumberModifiedTriples 
									&& !instanceTriples.isEmpty()) {
								auxIdx = rand.nextInt(instanceTriples.size()) ;
								if (rand.nextDouble()<deletingProbability) {
									stmtToBeRemoved.add(instanceTriples.get(auxIdx)); 
									instanceTriples.remove(auxIdx); 
									currentNumberModifiedTriples++; 
								}
							}
							
							break; 
							
							
						case PROPERTY_CENTERED: 
							break; 
					}
				}
			
				
// 				COMPLETE RANDOM MODIFICATION CODE
// 				
//				for (int j=0; j<totalNumberModifiedTriples; j++) {
//					auxIdx = rand.nextInt(triples.size()) ;
//					
//					if (rand.nextFloat()<=deletingProbability) {
//						numberOfDeletions++;
//						stmtToBeRemoved.add(triples.get(auxIdx));				
//					}
//					else if (rand.nextFloat() <= flippingProbability) {
//						if (triples.get(auxIdx).getObject().isLiteral()) {
//							numberOfDeletions++; 
//							stmtToBeRemoved.add(triples.get(auxIdx)); 
//						}
//						else{
//							numberOfFlippings++; 
//							stmt = triples.get(auxIdx); 
//							stmtToBeAdded.add(RDFModel.createStatement(stmt.getObject().asResource(), stmt.getPredicate(), stmt.getSubject()));						
//							stmtToBeRemoved.add(stmt); 
//						}
//					}
//					else {
//						// we mess with the object of the statement 
//						numberOfModifications++; 
//						stmt = triples.get(auxIdx); 
//						stmtToBeAdded.add(RDFModel.createStatement(stmt.getSubject(), 
//											stmt.getPredicate(), 
//											instances.get(rand.nextInt(instances.size())))); 						
//						stmtToBeRemoved.add(stmt); 
//						
//					}
//					triples.remove(auxIdx); 
//				}
				
				
				
				logger.debug("RDFModel size: "+RDFModel.size());
				logger.debug("Stmt to be removed: "+stmtToBeRemoved.size());
				RDFModel.remove(stmtToBeRemoved); 				
				logger.debug("RDFModel after removal size: "+RDFModel.size());
				logger.debug("Stmt to be added: "+stmtToBeAdded.size());
				RDFModel.add(stmtToBeAdded);
				logger.debug("RDFModel after addition size: "+RDFModel.size());
				
				logger.debug("-- Number of deletions: "+numberOfDeletions);
				logger.debug("-- Number of flippings: "+numberOfFlippings); 
				logger.debug("-- Number of modifications: "+numberOfModifications); 
				
				FileOutputStream out = new FileOutputStream(new File(outputFile));
				RDFModel.write(out, "N-TRIPLE");
				RDFModel.close(); 
				out.flush(); 
				out.close(); 
				
			}
		}
		 catch (Exception e) {
			logger.fatal("Failed", e);
		}
	}
	
	private static MODIFICATION_TYPE nextModification (Random r, ArrayList<MODIFICATION_TYPE> activeMods) {
		return activeMods.get(r.nextInt(activeMods.size())); 
	}
	
}
