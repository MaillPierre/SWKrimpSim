///////////////////////////////////////////////////////////////////////////////
// File: UpdateMeasuresCalculator.java 
// Author: Carlos Bobed
// Date: February 2018
// Comments: Program that calculates the codification lengths of different 
// 		transactions
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.ModelEvolver;
import com.irisa.dbplharvest.data.UpdateTransactions;
import com.irisa.dbplharvest.data.UpdateTransactionsFile;
import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.measures.Measures;
import com.irisa.utilities.Couple;

public class UpdateMeasuresCalculator {
		
	private static Logger logger = Logger.getLogger(UpdateMeasuresCalculator.class);
	
	public static String RESULTS_FILE_OPTION = "resultsFile"; 
	public static String CT_OPTION = "CT"; 
	public static String DB_ANALYSIS_OPTION = "DBAnalysis"; 
	public static String FILE_ID_OPTION = "fileList"; 
	public static String VREEKEN_OPTION = "vreekenFormat"; 
	public static String HELP_OPTION = "help"; 
	
	public static String RESULTS_HEADERS = "CT;updateID;prevCodSize;postCodSize;#prevTransactions;#postTransactions;prevCodTime;postCodTime";  
			
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(RESULTS_FILE_OPTION, true, ".csv file to include the results - append if exists");
		options.addOption(CT_OPTION, true, "codeTable of the dataset AGAINST which we compare the udpate");
		options.addOption(DB_ANALYSIS_OPTION, true, "original db analysis file required to read Vreeken CTs"); 
		options.addOption(FILE_ID_OPTION, true, "file with the list of files to codify"); 
		options.addOption(VREEKEN_OPTION, false, "whether we use or not the Vreeken Format"); 
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "OrientedMeasuresCalculator", options );
				System.exit(0);
			} 
			
			String resultsFilename = cmd.getOptionValue(RESULTS_FILE_OPTION);
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
				File resultsFile = new File(resultsFilename);
				final PrintWriter out; 
				if (resultsFile.exists()) {
					out = new PrintWriter(new FileOutputStream(resultsFile, resultsFile.exists())); 
				}
				else {
					out = new PrintWriter(resultsFile); 
					out.println(RESULTS_HEADERS); 
				}
				
				String CTFilename = cmd.getOptionValue(CT_OPTION);
				final ItemsetSet itemCT; 
				
				if (cmd.hasOption(VREEKEN_OPTION)) {
					String DBAnalysisFilename = cmd.getOptionValue(DB_ANALYSIS_OPTION);
					itemCT = Utils.readVreekenEtAlCodeTable(CTFilename, DBAnalysisFilename);  
				}
				else {
					itemCT = Utils.readItemsetSetFile(CTFilename);  
				}
				
				filenames.parallelStream().forEach( fileID -> {
					// we load the updateFile 
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
					logger.debug("reading "+fileID);
					UpdateTransactionsFile updFile = new UpdateTransactionsFile(year, month, day, hour, number, fileID); 
					UpdateTransactions updates = new UpdateTransactions(updFile); 
				
					if (!updates.isEmpty()) { 
						CodeTable CT = new CodeTable(itemCT);
						double firstCodLength = 0.0; 
						double secondCodLength = 0.0; 
						long firstTransNumber = 0; 
						long secondTransNumber = 0; 
						long innerID = 0; 
						long firstCodTime = 0; 
						long secondCodTime = 0; 
						long start = 0; 
						for (Couple<ItemsetSet, ItemsetSet> upd: updates.getUpdateTransactions() ) {
							firstCodLength = 0.0; 
							secondCodLength = 0.0; 
							firstTransNumber = 0; 
							secondTransNumber = 0;
							innerID++; 
							start = System.nanoTime();
							
							if (!upd.getFirst().isEmpty()) { 
								firstCodLength = Measures.codificationLengthApplyingLaplaceSmoothing(upd.getFirst(), CT); 
								firstTransNumber = upd.getFirst().size(); 
							} 
							firstCodTime = System.nanoTime()-start; 
							start = System.nanoTime();
							
							if (!upd.getSecond().isEmpty()) { 
								secondCodLength = Measures.codificationLengthApplyingLaplaceSmoothing(upd.getSecond(), CT); 
								secondTransNumber = upd.getSecond().size(); 
							}
							secondCodTime = System.nanoTime()-start; 
							
							StringBuilder strBldr = new StringBuilder(); 
							strBldr.append(CTFilename); 
							strBldr.append(";");
							strBldr.append(updates.getID());
							strBldr.append("-"); 
							strBldr.append(innerID); 
							strBldr.append(";");
							strBldr.append(firstCodLength); 
							strBldr.append(";");
							strBldr.append(secondCodLength);
							strBldr.append(";");
							strBldr.append(firstTransNumber);
							strBldr.append(";");
							strBldr.append(secondTransNumber);
							strBldr.append(";"); 
							strBldr.append(((double)firstCodTime)/1000000.0);
							strBldr.append(";"); 
							strBldr.append(((double)secondCodTime)/1000000.0); 
							writeLine (out, strBldr.toString()); 
							
						}
					}
					
				});  // stream processing
				
				out.flush();
				out.close();
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
