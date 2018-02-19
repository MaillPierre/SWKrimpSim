///////////////////////////////////////////////////////////////////////////////
//File: ModelEvolver.java 
//Author: Carlos Bobed
//Date: February 2018
//Comments: Class that loads a model, evolves it according to the change files
// 			provided, calculates the transaction codification, 
// 			and, optionaly, stores the result of the evolution
//Modifications:
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data;

import java.io.BufferedReader;
import java.io.File;
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
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.utils.UpdateSeparatorListFiles;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.swpatterns.data.AttributeIndex;

public class ModelEvolver {

private static Logger logger = Logger.getLogger(ModelEvolver.class);
	
	public static String INPUT_MODEL_OPTION = "inputModel"; 
	public static String OUTPUT_MODEL_OPTION = "writeOutputModel"; 
	public static String INPUT_INDEX = "index"; 
	public static String UPDATE_LIST_OPTION = "updateList"; 
	public static String HELP_OPTION = "help";
	
	public static String TRANSACTIONS_EXTENSION = ".trans"; 
	public static String SEPARATED_RES_EXTENSION = ".separated.nt";
	
	public static void main(String[] args) {
		
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(INPUT_MODEL_OPTION, true, "filename with the model to be evolved");
		options.addOption(OUTPUT_MODEL_OPTION, false, "write the output of the evolution"); 
		options.addOption(INPUT_INDEX, true, "filename of the index used to translate the transactions");
		options.addOption(UPDATE_LIST_OPTION, true, "filename with the list of update files, ordered by date and hour");
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "OrientedMeasuresCalculator", options );
				System.exit(0);
			} 
			
			// we read the index
			// recall: its singleton
			AttributeIndex index = AttributeIndex.getInstance();
			if(cmd.hasOption(INPUT_INDEX)) {
				index.readAttributeIndex(cmd.getOptionValue(INPUT_INDEX));
			}
			
			Model originalModel =  ModelFactory.createDefaultModel();
			originalModel.read(cmd.getOptionValue(INPUT_MODEL_OPTION)); 
			
			String fileListFilename = cmd.getOptionValue(UPDATE_LIST_OPTION); 
			long start = System.nanoTime(); 
			long progress = 0; 
			BufferedReader br = Files.newBufferedReader(Paths.get(fileListFilename)); 
			String currentUpdateFilename = null; 
			
			ChangesetTransactionConverter converter = new ChangesetTransactionConverter(); 
			converter.setContextSource(originalModel);
			
			while ( (currentUpdateFilename = br.readLine()) != null) { 
				progress++; 
				if (progress%10000 == 0) { 
					System.out.println(progress + " already processed "); 
				}
				
				// we prepare the changesetFile 
				try { 
					File resultsFile = new File(currentUpdateFilename+TRANSACTIONS_EXTENSION);
					PrintWriter out = null; 
					if (resultsFile.exists()) {
						resultsFile.delete(); 
					}	
					out = new PrintWriter(resultsFile); 
					
					StringTokenizer filenameParser = new StringTokenizer(currentUpdateFilename, File.separator);
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
												currentUpdateFilename+ChangesetFile.ADDED_EXTENSION, currentUpdateFilename+ChangesetFile.DELETED_EXTENSION); 
					
					Changeset changeset = new Changeset(changeFile);  
					
					if (Files.exists(Paths.get(currentUpdateFilename+SEPARATED_RES_EXTENSION))) { 
						changeset.readAffectedResources(Files.newBufferedReader(Paths.get(currentUpdateFilename+SEPARATED_RES_EXTENSION)));
					}
					else { 
						// we force to calculate them if they are not precalculated
						changeset.getAffectedResources(); 
					}
					
					// we get the first set of transactions
					// before applying the evolution
					
					
					HashMap<Resource, KItemset> initialTransactions = converter.extractTransactionsFromAffectedResources(changeset); 
					
					// we now apply the modification to the model
					if (changeset.getDelTriples() != null) { 
						originalModel.remove(changeset.getDelTriples().listStatements()); 
					}
					if (changeset.getAddTriples() != null) { 
						originalModel.add(changeset.getAddTriples().listStatements()); 
					}
					
					HashMap<Resource, KItemset> finalTransactions = converter.extractTransactionsFromAffectedResources(changeset); 
					
					Iterator<HashSet<Resource>> itSets = changeset.getAffectedResources().iterator();
					HashSet<Resource> auxSet = null; 
					Iterator<Resource> itRes = null; 
					while (itSets.hasNext()) { 
						auxSet = itSets.next();
						// we write the original state
						itRes = auxSet.iterator(); 
						while (itRes.hasNext()) { 
							out.println()
						}
					}
					
					
					out.flush();
					out.close();
				} 
				catch (IOException e) { 
					logger.error(currentUpdateFilename+": couldn't be processed"); 
				}
				
				
				
				
			}
			
		
			System.out.println("Processed : "+progress);
			System.out.println("Finished after: "+ ( ((double)(System.nanoTime()-start))/1000000000)+" s."); 
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
	
}
