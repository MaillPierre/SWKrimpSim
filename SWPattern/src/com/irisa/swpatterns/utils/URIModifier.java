///////////////////////////////////////////////////////////////////////////////
// File: URIModifier.java 
// Author: Carlos Bobed
// Date: August 2018
// Comments: Utility to change all the property URIs used in a model uniformly
// 		for the third synthetic experiment (incrementally add new data different
// 		structurally and see how that develops from the beginning)
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.utils;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.UtilOntology;

public class URIModifier {

	private static Logger logger = Logger.getLogger(URIModifier.class);
		
	public static String INPUT_MODEL_OPTION = "inputModel";
	public static String OUTPUT_MODEL_OPTION = "outputModel"; 
	public static String SUFFIX_OPTION = "suffix"; 
	public static String HELP_OPTION = "help";
	
	public static void main(String[] args) {
		// code adapted from Pierre's one ... thanks ;) 
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(INPUT_MODEL_OPTION, true, "filename with the model to be modified");
		options.addOption(OUTPUT_MODEL_OPTION, true, "filename of the output model"); 
		options.addOption(SUFFIX_OPTION, true, "suffix to be added to the URIs"); 
		options.addOption(HELP_OPTION, false, "display this help"); 
		
		try {
			CommandLine cmd = parser.parse(options, args);
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "URIModifier", options );
			} else {
				String RDFFilename = cmd.getOptionValue(INPUT_MODEL_OPTION);
				String outputFilename = cmd.getOptionValue(OUTPUT_MODEL_OPTION);
				String suffix = cmd.getOptionValue(SUFFIX_OPTION); 
				logger.debug("Reading the RDF file ...");
				BaseRDF RDFWrapper = new BaseRDF(RDFFilename);
				logger.debug("Done");
				Model RDFModel = RDFWrapper.getModel();
				logger.debug("Initializing the ontological elements ...");
				UtilOntology ontology = new UtilOntology();
				ontology.init(RDFWrapper);
				logger.debug("Done");
				
				// we have to do it in a pertriple-basis
				// we only modify the URIs of the schema (classes and properties) 
				
				Model outModel = ModelFactory.createDefaultModel();

				RDFModel.listStatements().forEachRemaining(
					stmt -> {
					
					String subjectURI = stmt.getSubject().getURI(); 
					String predicateURI = stmt.getPredicate().getURI(); 
					String objectURI = stmt.getObject().isResource()?
											stmt.getObject().asResource().getURI():
											stmt.getObject().asLiteral().toString(); 
					if (ontology.isClass(stmt.getSubject()) || ontology.isProperty(stmt.getSubject())) {
						subjectURI += suffix; 
					}
					if (ontology.isProperty(stmt.getPredicate())) {
						predicateURI += suffix; 
					}
					if (stmt.getObject().isResource()) {
						if (ontology.isClass(stmt.getObject().asResource()) || ontology.isProperty(stmt.getObject().asResource())) {
							objectURI += suffix; 
						}
					}
					outModel.add(outModel.createStatement(outModel.createResource(subjectURI), 
							outModel.createProperty(predicateURI), 
							stmt.getObject().isResource()?
									outModel.createResource(objectURI):
									outModel.createLiteral(objectURI)));
				});
					
				OutputStream out = new GZIPOutputStream(new FileOutputStream(new File(outputFilename))); 
				RDFDataMgr.write(out, outModel, RDFFormat.NTRIPLES_UTF8);
				outModel.close(); 
				out.flush(); 
				out.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
}
