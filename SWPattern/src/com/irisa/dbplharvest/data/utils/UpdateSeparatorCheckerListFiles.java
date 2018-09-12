///////////////////////////////////////////////////////////////////////////////
//File: UpdateSeparatorCheckerListFiles.java 
//Author: Carlos Bobed
//Date: September 2018
//Comments: Program to check how many separated updates are duplicated
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
import java.util.Collections;
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
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.Changeset;
import com.irisa.dbplharvest.data.ChangesetFile;

public class UpdateSeparatorCheckerListFiles {
	
	private static Logger logger = Logger.getLogger(UpdateSeparatorCheckerListFiles.class);
	
	// file ID is the the complete path, but for the .added.nt.gz or the .removed.nt.gz extension
	public static String FILE_ID_OPTION = "fileList"; 
	public static String HELP_OPTION = "help"; 
	
	public static String INPUT_EXTENSION = ".separated.nt"; 
	public static String OUTPUT_EXTENSION = ".separated.checked.nt"; 
	
	
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
				formatter.printHelp( "UpdateSeparatorListFiles", options );
				System.exit(0);
			} 
			
			String fileListFilename = cmd.getOptionValue(FILE_ID_OPTION);
			long start = System.nanoTime(); 
			
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
						if (Files.exists(Paths.get(fileID+INPUT_EXTENSION))) { 
							logger.debug("reading the affected resources ...");
							BufferedReader in = Files.newBufferedReader(Paths.get(fileID+INPUT_EXTENSION)); 
							ArrayList<HashSet<Resource>> resources = readAffectedResources(in);
							in.close();
							
							Collections.sort(resources, (set1, set2) -> {
								if (set1.size()<set2.size()) 
									return 1; 
								else if (set1.size()==set2.size())
									return 0; 
								else 
									return -1; } );
							StringBuilder str = new StringBuilder(); 
							resources.forEach(id->{str.append(id.size()); str.append(" "); });
							
							int initialSize = resources.size(); 
							
							for (int i=0; i<resources.size(); i++) {
								for (int j=resources.size()-1; i < j; j--){
									if (resources.get(i).containsAll(resources.get(j))) {
										resources.remove(j); 
									}
								}
							}
							
							int finalSize = resources.size(); 
							
							File resultsFile = new File(fileID+OUTPUT_EXTENSION);
							PrintWriter out = null; 
							if (resultsFile.exists()) {
								resultsFile.delete(); 
							}	
							out = new PrintWriter(resultsFile); 
							out.println(initialSize - finalSize); 				
							
							out.flush();
							out.close();
							
						}
						
					} 
					catch (IOException e) { 
						logger.error(fileID+": couldn't be processed"); 
					}
				}
				
				); 
			}
			System.out.println("Processed : "+filenames.size());
			System.out.println("Finished after: "+ ( ((double)(System.nanoTime()-start))/1000000000)+" s."); 
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}

	public static ArrayList<HashSet<Resource>>readAffectedResources (BufferedReader in) throws IOException { 
		ArrayList<HashSet<Resource>> results = new ArrayList<HashSet<Resource>> ();  
		HashSet<Resource> auxiliar = new HashSet<Resource>(); 
		String line = null; 
		while ( (line = in.readLine()) != null ) {
			if ("".equals(line)) { 
				// an empty line == new set
				results.add(auxiliar);				 
				auxiliar = new HashSet<Resource>(); 
			}
			else { 
				auxiliar.add(ResourceFactory.createResource(line)); 
			}
		}
		// we add the last one
		if (!auxiliar.isEmpty()) { 
			results.add(auxiliar);  
		}
		return results; 
	}
}