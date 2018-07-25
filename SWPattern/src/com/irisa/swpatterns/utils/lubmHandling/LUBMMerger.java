///////////////////////////////////////////////////////////////////////////////
// File: LUBMMerger.java 
// Author: Carlos Bobed 
// Date: July 2018
// Comments: LUBM Files merger (they are generated in a way such as every 
// 		university is scattered in different files - one per deparment). This 
// 		implementation is based on the merger we implemented for 
// 		resource aware project with Isa Guclu et al. 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////


package com.irisa.swpatterns.utils.lubmHandling;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

public class LUBMMerger {

	private static Logger logger = Logger.getLogger(LUBMMerger.class);
	
	public static void main(String[] args) {
		try {
			BasicConfigurator.configure();
			PropertyConfigurator.configure("log4j-config.txt");
			
			String inputDirOption= "inputDir";
			String outputDirOption= "outputDir";
			String numberUniversitiesOption = "numUniversities"; 
			String separatedUniversitiesOption = "separatedFiles"; 
			
			// Setting up options
			CommandLineParser parser = new DefaultParser();
			Options options = new Options();
			// In/Out
			options.addOption(inputDirOption, true, "Directory containing the files to be merged.");
			options.addOption(outputDirOption, true, "Directory containing the files that have been merged.");
			options.addOption(numberUniversitiesOption, true, "Number of universities to be merged.");
			options.addOption(separatedUniversitiesOption, false, "Separate each university in one file or merge ALL into one big one.");
			
			CommandLine cmd = parser.parse(options, args);
			
			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "LUBMMerger", options );
			} else {
				String inputDir = cmd.getOptionValue(inputDirOption);
				String outputDir = cmd.getOptionValue(outputDirOption); 
				String num = cmd.getOptionValue(numberUniversitiesOption); 
				boolean separated = cmd.hasOption(separatedUniversitiesOption); 
				
				logger.debug("Merging ontologies ..."); 
				merge(inputDir, outputDir, Integer.valueOf(num), separated);
			}
			
		}
		 catch (Exception e) {
			logger.fatal("Failed on " + Arrays.toString(args), e);
		}
		System.out.println("Done.");
	}
	
	
	public static void merge(String dir, String outDir, int pUniversityNumber, boolean separated) {
		OWLOntologyManager manager;
		String LUBM_URI_MERGED = "http://swat.cse.lehigh.edu/onto/univ-bench"; 
		try {
			File folder = new File(dir);
			File[] currentFileList = null; 
			
			if (separated) {
				logger.debug("one file per university ..."); 
				for (int i=0; i<pUniversityNumber; i++) {
					logger.debug("Merging University "+pUniversityNumber+"...");
					manager = OWLManager.createOWLOntologyManager();				
					currentFileList = folder.listFiles(new UniversityFileFilter(i));
					for (int j=0; j<currentFileList.length; j++) {
						logger.debug("--> Loading "+currentFileList[j]); 
						manager.loadOntologyFromOntologyDocument(currentFileList[j]); 
					}
					
					OWLOntologyMerger merger = new OWLOntologyMerger(manager); 
					OWLOntology mergedOntology = merger.createMergedOntology(manager, IRI.create(LUBM_URI_MERGED+"-"+i+".owl")); 
					
					FileOutputStream outOntology = new FileOutputStream(new File(outDir+File.separator+"Merged-"+i+".owl")); 
					manager.saveOntology(mergedOntology, new RDFXMLOntologyFormat(), outOntology);
					outOntology.flush(); 
					outOntology.close(); 
					
				}
			}
			else {
				logger.debug("one file to contain them all ..."); 
				manager = OWLManager.createOWLOntologyManager(); 
				for (int i=0; i<pUniversityNumber;i++) {
					currentFileList = folder.listFiles(new UniversityFileFilter(i)); 
					for (int j=0; j<currentFileList.length;j++) {
						manager.loadOntologyFromOntologyDocument(currentFileList[j]); 
					}
				}
				// once we have all ontologies loaded, we merge them
				// some code repetition, but it is for readability's sake 
				OWLOntologyMerger merger = new OWLOntologyMerger(manager); 
				OWLOntology mergedOntology = merger.createMergedOntology(manager, IRI.create(LUBM_URI_MERGED+"-Merged"+".owl")); 				
				FileOutputStream outOntology = new FileOutputStream(new File(outDir+File.separator+"AllUniversities-Merged.owl")); 
				manager.saveOntology(mergedOntology, new RDFXMLOntologyFormat(), outOntology);
				outOntology.flush(); 
				outOntology.close(); 
			}
		} catch (Exception e) {
			logger.fatal("There was a problem : " + e.getMessage());
		}

	}
	
	/* CBL */ 
	public static class UniversityFileFilter implements FileFilter {
		int universityNumber = -1; 
		public UniversityFileFilter(int number) {
			this.universityNumber = number; 
		}
		public boolean accept(File pathname) {
			return pathname.getName().startsWith("University"+universityNumber+"_");
		}
	}
}
