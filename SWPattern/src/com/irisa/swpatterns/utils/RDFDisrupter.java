///////////////////////////////////////////////////////////////////////////////
// File: RDFDisrupter.java 
// Author: Carlos Bobed
// Date: July 2017
// Comments: Utility to get an RDF model simulating different modeling 
// 		differences - used for the experiments in SIGAPP'18
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
import org.apache.jena.rdf.model.Property;
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

public class RDFDisrupter {

	public enum MODIFICATION_TYPE {
		INSTANCE_CENTERED, 
		PROPERTY_CENTERED
	}; 
	
	private static Logger logger = Logger.getLogger(RDFDisrupter.class);
	
	public static double modifyingProbability = 0.5; 
	public static double orientationModificationProbability = 0.5; 
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
		String modifyingProbabilityOption = "modifyingProbability"; 
		String orientationModificationProbabilityOption = "orientationProbability"; 
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
		options.addOption(deletingProbabilityOption, true, "Probability of deleting a triple");
		options.addOption(modifyingProbabilityOption, true, "Probability of modifying a triple - it is conditioned to not to be deleted previously");
		options.addOption(orientationModificationProbabilityOption, true, "Probability of the orientation of the modification being "+
																" changing the object (1-value will be the probability of changing the subject)"); 
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
				if (cmd.hasOption(orientationModificationProbabilityOption)) {
					orientationModificationProbability = Double.valueOf(cmd.getOptionValue(orientationModificationProbabilityOption)); 
				}
				if (cmd.hasOption(modifyingProbabilityOption)) {
					modifyingProbability = Double.valueOf(cmd.getOptionValue(modifyingProbabilityOption)); 
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
				else {
					// by default, we apply the instance centered approach
					activeModifications.add(MODIFICATION_TYPE.INSTANCE_CENTERED); 
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
				long numberOfSubjectModifications = 0; 
				long numberOfObjectModifications = 0;
				long numberOfModifiedInstances = 0; 
				long numberOfModifiedProperties = 0; 
				long numberOfInstanceCenteredModifications = 0; 
				long numberOfPropertyCenteredModifications = 0; 
				StmtIterator it = null; 
				Statement stmt = null; 
				Random rand = new Random(); 
				int auxIdx = -1; 
				
				List<Statement> triples = new ArrayList<>();
				List<Resource> instances = new ArrayList<>(); 
				List<Resource> properties = new ArrayList<>(); 
				RDFModel.listSubjects().forEachRemaining(instances::add);
				ontology.properties().iterator().forEachRemaining(properties::add);
				
				
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
							numberOfModifiedInstances++; 
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
							while ( (currentNumberModifiedTriples <totalNumberModifiedTriples) 
									&& !instanceTriples.isEmpty()) {
								auxIdx = rand.nextInt(instanceTriples.size()) ;
								if (rand.nextDouble()<deletingProbability) {					
									logger.debug(instanceTriples.get(auxIdx));
									logger.debug(!ontology.isOntologyPropertyVocabulary(instanceTriples.get(auxIdx).getPredicate()));									
									if (!ontology.isOntologyPropertyVocabulary(instanceTriples.get(auxIdx).getPredicate()) )  {
										stmtToBeRemoved.add(instanceTriples.get(auxIdx)); 										
										currentNumberModifiedTriples++; 
										numberOfDeletions++; 
										numberOfInstanceCenteredModifications++;
									}			
									instanceTriples.remove(auxIdx); 
								}
							}
							
							break; 
						case PROPERTY_CENTERED:
							numberOfModifiedProperties++; 
							if (properties.isEmpty()) {
								// we initialize again the candidates 
								ontology.properties().iterator().forEachRemaining(properties::add);
							}
							
							// here we are assured that they are part of the vocabulary
							auxIdx = rand.nextInt(properties.size()); 
							Resource property = properties.get(auxIdx); 
							List<Statement> propertyTriples = new ArrayList<>(); 
							RDFModel.listStatements(new SimpleSelector(null, property.as(Property.class), (RDFNode) null)).forEachRemaining(propertyTriples::add);
							
							while ( (currentNumberModifiedTriples < totalNumberModifiedTriples)
									&& !propertyTriples.isEmpty() ) {
								auxIdx = rand.nextInt(propertyTriples.size());
								stmt = propertyTriples.get(auxIdx); 
								if (rand.nextDouble()<deletingProbability) {
									stmtToBeRemoved.add(stmt); 
									propertyTriples.remove(auxIdx); 
									currentNumberModifiedTriples++;
									// stats stuff
									numberOfDeletions++; 
									numberOfPropertyCenteredModifications++; 
								}
								else {
									// we have to take into account that the probability of modifiying a 
									// triple is then conditioned to not to be deleted
									
									if (rand.nextDouble()<modifyingProbability) {
										// we now select the orientation of the modification 
										List<Statement> candidateTriples = new ArrayList<>();
										Statement newStmt = null; 										
										RDFModel.listStatements(new SimpleSelector(null, stmt.getPredicate(), (RDFNode)null)).forEachRemaining(candidateTriples::add);
										
										if (rand.nextDouble() < orientationModificationProbability) {
											// we modify the object of the property											
											newStmt = RDFModel.createStatement(stmt.getSubject(), stmt.getPredicate(), 
													candidateTriples.get(rand.nextInt(candidateTriples.size())).getObject());
											// stats stuff
											numberOfObjectModifications++; 
										}
										else{
											// we modify the subject of the property
											newStmt = RDFModel.createStatement(candidateTriples.get(rand.nextInt(candidateTriples.size())).getSubject(),
														stmt.getPredicate(),stmt.getObject());
											// stats stuff
											numberOfSubjectModifications++; 
										}
										propertyTriples.remove(auxIdx); 
										stmtToBeRemoved.add(stmt);
										stmtToBeAdded.add(newStmt);
										currentNumberModifiedTriples++;
										// stats stuff
										numberOfPropertyCenteredModifications++; 
									}
									else{
										// we remove it from the potential triples to be modified (it has just survived as it is)
										propertyTriples.remove(auxIdx); 
									}
								}
							}							
							break; 
						default: 
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
				
				logger.debug("STATS:"); 
				logger.debug("-- InstanceCentered modifications: "+numberOfInstanceCenteredModifications);
				logger.debug("---- Number of modified instances: "+numberOfModifiedInstances);
				logger.debug("-- PropertyCentered modifications: "+numberOfPropertyCenteredModifications);
				logger.debug("---- Number of modified properties: "+numberOfModifiedProperties);
				logger.debug("------ Object modifications: "+numberOfObjectModifications );
				logger.debug("------ Subject modifications: "+numberOfSubjectModifications);
				logger.debug("-- Number of deletions: "+numberOfDeletions);
				
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
