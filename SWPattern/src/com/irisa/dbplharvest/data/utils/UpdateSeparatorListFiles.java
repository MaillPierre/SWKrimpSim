///////////////////////////////////////////////////////////////////////////////
//File: UpdateSeparatorListFiles.java 
//Author: Carlos Bobed
//Date: February 2018
//Comments: Program to separate the updates in the dbpedia live files
//Modifications:
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.Changeset;
import com.irisa.dbplharvest.data.ChangesetFile;

public class UpdateSeparatorListFiles {
	
	private static Logger logger = Logger.getLogger(UpdateSeparatorListFiles.class);
	
	// file ID is the the complete path, but for the .added.nt.gz or the .removed.nt.gz extension
	public static String FILE_ID_OPTION = "fileList"; 
	public static String HELP_OPTION = "help"; 
	
	public static String OUTPUT_EXTENSION = ".separated.nt"; 

	public static void main(String[] args) {
		
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(FILE_ID_OPTION, true, "fileList is the file with the file IDs, i.d., their complete path, "
				+ "but for the .added.nt.gz or the .removed.nt.gz extension");
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "OrientedMeasuresCalculator", options );
				System.exit(0);
			} 
			
			String fileListFilename = cmd.getOptionValue(FILE_ID_OPTION); 
			
			// first, we read all the file names in memory to paralellize everything
			List<String> filenames= new ArrayList<String>(); 
			
			try (BufferedReader br = Files.newBufferedReader(Paths.get(fileListFilename))) {
				filenames =br.lines().collect(Collectors.toList()); 
			}
			catch (IOException e) { 
				e.printStackTrace();
			}
			
			if (!filenames.isEmpty()) {
				
				filenames.parallelStream().forEach( fileID -> { 
			
					try { 
						File resultsFile = new File(fileID+OUTPUT_EXTENSION);
						PrintWriter out = null; 
						if (resultsFile.exists()) {
							resultsFile.delete(); 
						}	
						out = new PrintWriter(resultsFile); 
						
						StringTokenizer filenameParser = new StringTokenizer(fileID, File.separator);
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
													fileID+ChangesetFile.ADDED_EXTENSION, fileID+ChangesetFile.DELETED_EXTENSION); 
						
						Changeset changeset = new Changeset(changeFile);  
						
						HashSet<HashSet<Resource>> first = changeset.getAffectedResources(); 
						changeset.writeAffectedResources(out);
						out.flush();
						out.close();
					} 
					catch (IOException e) { 
						logger.error(fileID+": couldn't be processed"); 
					}
				}
				
				); 
			}
		
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}

}
