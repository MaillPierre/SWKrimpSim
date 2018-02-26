///////////////////////////////////////////////////////////////////////////////
//File: ModelExtractor.java 
//Author: Carlos Bobed
//Date: February 2018
//Comments: Class that loads a model and a changeset, and extracts the context
// 		model of the affected resources. Developed for testing purposes.
//Modifications:
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.utils.UpdateSeparatorListFiles;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.swpatterns.data.AttributeIndex;

public class ModelExtractor {

private static Logger logger = Logger.getLogger(ModelExtractor.class);
	
	public static String INPUT_MODEL_OPTION = "inputModel"; 
	public static String OUTPUT_MODEL_OPTION = "writeOutputModel"; 
	public static String PREVIOUSLY_CANONIZED_OPTION = "alreadyCanonized"; 
	public static String FILE_ID_OPTION = "fileID"; 
	public static String HELP_OPTION = "help";
	
	public static String SEPARATED_RES_EXTENSION = ".separated.nt";
	
	public static String STATES_SEPARATOR="----";
	
	public static void main(String[] args) {
		
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(INPUT_MODEL_OPTION, true, "filename with the model to be evolved");
		options.addOption(OUTPUT_MODEL_OPTION, true, "write the output of the evolution"); 
		options.addOption(PREVIOUSLY_CANONIZED_OPTION, false, "forces to work with the already canonized version of the updates");
		options.addOption(FILE_ID_OPTION, true, "filename with the list of update files, ordered by date and hour");
		
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "OrientedMeasuresCalculator", options );
				System.exit(0);
			} 
			
			long start = System.nanoTime(); 
			Model originalModel =  ModelFactory.createDefaultModel();
			RDFDataMgr.read(originalModel, cmd.getOptionValue(INPUT_MODEL_OPTION)); 
//			originalModel.read(cmd.getOptionValue(INPUT_MODEL_OPTION)); 

			System.out.println("Model read after: "+ ( ((double)(System.nanoTime()-start))/1000000000)+" s.");
			
			String updateFilename = cmd.getOptionValue(FILE_ID_OPTION); 
			
			ChangesetTransactionConverter converter = new ChangesetTransactionConverter(); 
			converter.setContextSource(originalModel);
			
			// we prepare the changesetFile 
			try { 
				
				StringTokenizer filenameParser = new StringTokenizer(updateFilename, File.separator);
				Stack<String> auxStack = new Stack<String>(); 
				while (filenameParser.hasMoreElements()) { 
					auxStack.push(filenameParser.nextToken());
				}
				
				String filenameBase = auxStack.pop(); 
				String number = auxStack.pop(); 
				String hour = auxStack.pop(); 
				String day = auxStack.pop(); 
				String month = auxStack.pop(); 
				String year = auxStack.pop();
				
				ChangesetFile changeFile = new ChangesetFile(year, month, day, hour, number, 
											updateFilename+ChangesetFile.ADDED_EXTENSION, updateFilename+ChangesetFile.DELETED_EXTENSION); 
				
				boolean canonize = !cmd.hasOption(PREVIOUSLY_CANONIZED_OPTION); 
				Changeset changeset = new Changeset(changeFile, canonize);  
				
				if (Files.exists(Paths.get(updateFilename+SEPARATED_RES_EXTENSION))) { 
					changeset.readAffectedResources(Files.newBufferedReader(Paths.get(updateFilename+SEPARATED_RES_EXTENSION)));
				}
				else { 
					// we force to calculate them if they are not precalculated
					System.out.println("It does not exist ... I calculate them on the fly"); 
					changeset.getAffectedResources(); 
				}

				Model context = converter.extractContextOfChangeset(changeset); 
				File resultsFile = new File(cmd.getOptionValue(OUTPUT_MODEL_OPTION));
				
				if (resultsFile.exists()) {
					resultsFile.delete(); 
				}	
				
				
				FileOutputStream out = new FileOutputStream(resultsFile); 
				RDFDataMgr.write(out, context, RDFFormat.NTRIPLES_UTF8); 
				
				out.flush(); 
				out.close();
			} 
			catch (IOException e) { 
				logger.error(updateFilename+": couldn't be processed"); 
			}
			
			System.out.println("Finished after: "+ ( ((double)(System.nanoTime()-start))/1000000000)+" s."); 
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
	
}
