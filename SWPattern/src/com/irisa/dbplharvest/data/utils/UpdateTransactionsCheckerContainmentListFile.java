///////////////////////////////////////////////////////////////////////////////
// File: UpdateTransactionsCheckerContainmentListFile.java 
// Author: Carlos Bobed
// Date: Sept 2018
// Comments: 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

import com.irisa.dbplharvest.data.ModelEvolver;
import com.irisa.dbplharvest.data.UpdateTransactions;
import com.irisa.dbplharvest.data.UpdateTransactionsFile;
import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.measures.Measures;
import com.irisa.utilities.Couple;

public class UpdateTransactionsCheckerContainmentListFile {
		
	private static Logger logger = Logger.getLogger(UpdateTransactionsCheckerContainmentListFile.class);

	public static String FILE_ID_OPTION = "fileList"; 
	public static String HELP_OPTION = "help"; 
	public static String INPUT_EXTENSION = ".trans"; 
	public static String OUTPUT_EXTENSION = ".trans.cleaned"; 
	public static String OUTPUT_STATS_EXTENSION = ".trans.cleaned.stats"; 
			
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(FILE_ID_OPTION, true, "file with the list of files to codify");
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "UpdateTransactionsCheckerContainmentListFile", options );
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
					
						// we load the updateFile 
						StringTokenizer filenameParser = new StringTokenizer(fileID, File.separator);
						Stack<String> auxStack = new Stack<String>(); 
						while (filenameParser.hasMoreElements()) { 
							auxStack.push(filenameParser.nextToken());
						}
						
						String number = auxStack.pop(); 
						String hour = auxStack.pop(); 
						String day = auxStack.pop(); 
						String month = auxStack.pop(); 
						String year = auxStack.pop();
						logger.debug("reading "+fileID);
						UpdateTransactionsFile updFile = new UpdateTransactionsFile(year, month, day, hour, number, fileID); 
						UpdateTransactions updates = new UpdateTransactions(updFile); 
					
						if (!updates.isEmpty()) { 
							
							File resultsFile = new File(fileID+OUTPUT_EXTENSION);
							PrintWriter out; 
							out = new PrintWriter(resultsFile); 
							File statsFile = new File(fileID+OUTPUT_STATS_EXTENSION); 
							PrintWriter statsOut; 
							statsOut = new PrintWriter(statsFile); 
							
							long firstTransNumber = 0; 
							long secondTransNumber = 0; 
							long innerID = 0; 
							long firstCodTime = 0; 
							long secondCodTime = -1; 
							long start = 0; 
							
							ArrayList<Couple<ItemsetSet, ItemsetSet>> ordered = new ArrayList<>(updates.getUpdateTransactions()); 
							Collections.sort(ordered, (s1, s2) -> {
								int maxS1 = Math.max(s1.getFirst().size(), s1.getSecond().size()); 
								int maxS2 = Math.max(s2.getFirst().size(), s2.getSecond().size());
								
								if (maxS1 < maxS2) {
									return 1; 
								}
								else if (maxS1 == maxS2) {
									return 0; 
								}
								else {
									return -1; 
								}
							}); 
							
							int initial=ordered.size(); 
							Couple<ItemsetSet, ItemsetSet> baseCouple = null; 
							Couple<ItemsetSet, ItemsetSet> comparedCouple = null; 
							for (int i=0; i<ordered.size(); i++) {
								baseCouple = ordered.get(i); 
								for (int j=ordered.size()-1; i<j; j--) {
									// both sets of transactions must be included
									comparedCouple = ordered.get(j); 
									if (baseCouple.getFirst().checkInclusion(comparedCouple.getFirst()) && 
											baseCouple.getSecond().checkInclusion(comparedCouple.getSecond())) {	
										ordered.remove(j); 
									}
								}
							}
							
							for (Couple<ItemsetSet, ItemsetSet> survivor: ordered) {
								for (KItemset trans: survivor.getFirst()) {
									out.println(trans); 
								}
								out.println(ModelEvolver.STATES_SEPARATOR); 
								for (KItemset trans: survivor.getSecond()) {
									out.println(trans); 
								}
								out.println(); 
							}
							
							
							statsOut.println(updates.getUpdateTransactions().size()); 
							statsOut.println(ordered.size()); 
							/* 
							 * File resultsFile = new File(currentUpdateFilename+TRANSACTIONS_EXTENSION);
							PrintWriter out = null; 
							if (resultsFile.exists()) {
								resultsFile.delete(); 
							}	
							out = new PrintWriter(resultsFile); 
							
							Iterator<HashSet<Resource>> itSets = changeset.getAffectedResources().iterator();
							HashSet<Resource> auxSet = null; 
							Iterator<Resource> itRes = null; 
							Resource auxRes = null; 
							
							while (itSets.hasNext()) { 
								auxSet = itSets.next();
								// we write the original state
								itRes = auxSet.iterator(); 
								while (itRes.hasNext()) { 
									auxRes= itRes.next(); 
									if (initialTransactions.containsKey(auxRes)) { 
										out.println(initialTransactions.get(auxRes));
									}
								}
								out.println(STATES_SEPARATOR); 
								// now we write the final state
								itRes = auxSet.iterator(); 
								while (itRes.hasNext()) { 
									auxRes = itRes.next(); 
									if (finalTransactions.containsKey(auxRes)) { 
										out.println(finalTransactions.get(auxRes)); 
									}
								}
								// we separate the groups by a blank line
								out.println(); 
							}
							 */
							
							
							out.flush();
							out.close();
							statsOut.flush();
							statsOut.close(); 
						}
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
					
				});  // stream processing
				
				
			} // filenames
				
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
			
	}
	
	public static synchronized void writeLine (PrintWriter out, String line) { 
		out.println(line); 
		out.flush();
	}
}
