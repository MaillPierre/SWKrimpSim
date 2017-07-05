///////////////////////////////////////////////////////////////////////////////
// File: RDFDisrupter.java 
// Author: Carlos Bobed
// Date: July 2017
// Comments: Utility to get an RDF model and randomly messy 
// 		IMPORTANT: The results might be inconsistent as the modifications 
// 		are done randomly, without taking into account semantic aspects
// 		It is just a tool to prove that the more different the different 
// 		RDF graphs are, the more different the compression rates are 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.UtilOntology;
import com.irisa.swpatterns.SWPatterns;

import scala.Array;

public class RDFDisrupter {

	private static Logger logger = Logger.getLogger(RDFDisrupter.class);
	
	public static double deletingProbability = 0.2; 
	public static double flippingProbability = 0.2; 
	public static double modifiedPercentage = 0.1; 
	public static boolean touchIsA = false; 
	
	public static void main(String[] args) {
		// code adapted from Pierre's one ... thanks ;) 
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		
		// In/out options name to facilitate further references
		String inputRDFOption = "inputRDF";
		String deletingProbabilityOption= "deletingProbability"; 
		String modifiedPercentageOption = "modifiedPercentage";
		String flippingProbabilityOption = "flippingProbability"; 
		String touchIsAOption = "touchIsA"; 
		String outputRDFOption = "outputRDF"; 
		
		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		// In/Out
		options.addOption(inputRDFOption, true, "Input RDF file");
		options.addOption(outputRDFOption, true, "Output RDF file"); 
		options.addOption(deletingProbabilityOption, true, "Probability of deleting instead of modifying a triple"); 
		options.addOption(modifiedPercentageOption, true, "Percentage of the KB that has to be modified");
		options.addOption(flippingProbabilityOption, true, "Probability of flipping the orientation of a triple");
		options.addOption(touchIsAOption, false, "Mess with the isA triples"); 
		
		try {
			CommandLine cmd = parser.parse( options, args);
		
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
					outputFile = RDFFile+"-modified.rdf"; 
				}
				if (cmd.hasOption(deletingProbabilityOption)) {
					deletingProbability = Double.valueOf(cmd.getOptionValue(deletingProbabilityOption)); 
				}
				if (cmd.hasOption(modifiedPercentageOption)) {
					modifiedPercentage = Double.valueOf(cmd.getOptionValue(modifiedPercentageOption)); 
				}
				
				logger.debug("Reading the RDF file ...");
				BaseRDF RDFWrapper = new BaseRDF(RDFFile);
				logger.debug("Done");
				Model RDFModel = RDFWrapper.getModel();
				logger.debug("Initializing the ontological elements ...");
				UtilOntology ontology = new UtilOntology();
				ontology.init(RDFWrapper);
				logger.debug("Done");
				long numberOfTriples = -1; 
				long numberOfModifications = 0; 
				long numberOfDeletions = 0; 
				long numberOfFlippings = 0; 
				StmtIterator it = null; 
				Statement stmt = null; 
				Random rand = new Random(); 
				int auxIdx = -1; 
				
				List<Statement> triples = new ArrayList<>();
				List<Statement> candidateTriples = new ArrayList<>(); 
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
					else {
						// this is not done in a safe way 
						// it is only for data equivalence purposes 
						if (touchIsA && stmt.getPredicate().equals(RDF.type)) {
							triples.add(stmt); 
						}								
					}
				}
				numberOfTriples = (long) Math.floor(modifiedPercentage * triples.size());
				logger.debug("Messing with "+numberOfTriples+" triples ... ");
				for (int j=0; j<numberOfTriples; j++) {
					auxIdx = rand.nextInt(triples.size()) ;
					
					if (rand.nextFloat()<=deletingProbability) {
						numberOfDeletions++;
						RDFModel.remove(triples.get(auxIdx));					
					}
					else if (rand.nextFloat() <= flippingProbability) {
						if (triples.get(auxIdx).getObject().isLiteral()) {
							numberOfDeletions++; 
							RDFModel.remove(triples.get(auxIdx)); 
						}
						else{
							numberOfFlippings++; 
							stmt = triples.get(auxIdx); 
							RDFModel.add(RDFModel.createStatement(stmt.getObject().asResource(), stmt.getPredicate(), stmt.getSubject())); 
							RDFModel.remove(stmt); 
						}
					}
					else {
						// we mess with the object of the statement 
						numberOfModifications++; 
						stmt = triples.get(auxIdx); 
						RDFModel.add(RDFModel.createStatement(stmt.getSubject(), 
																stmt.getPredicate(), 
																instances.get(rand.nextInt(instances.size()))));
						RDFModel.remove(stmt); 
						
					}
				}
				
				logger.debug("-- Number of deletions: "+numberOfDeletions);
				logger.debug("-- Number of flippings: "+numberOfFlippings); 
				logger.debug("-- Number of modifications: "+numberOfModifications); 
				
				FileOutputStream out = new FileOutputStream(new File(outputFile));
				RDFModel.write(out); 
				out.flush(); 
				out.close(); 
				
			}
		}
		 catch (Exception e) {
			logger.fatal("Failed", e);
		}
	}
	
	
	
}
