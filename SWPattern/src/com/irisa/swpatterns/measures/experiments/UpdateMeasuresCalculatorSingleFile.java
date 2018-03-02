///////////////////////////////////////////////////////////////////////////////
// File: UpdateMeasuresCalculatorSingleFile.java 
// Author: Carlos Bobed
// Date: February 2018
// Comments: Program that calculates which version of a code table classifies 
// 			better a set 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures.experiments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Stack;
import java.util.StringTokenizer;

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

public class UpdateMeasuresCalculatorSingleFile {
	
	private static Logger logger = Logger.getLogger(UpdateMeasuresCalculatorSingleFile.class);
	
	public static String RESULTS_FILE_OPTION = "resultsFile"; 
	public static String CT_OPTION = "CT"; 
	public static String DB_ANALYSIS_OPTION = "DBAnalysis"; 
	public static String UPDATE_FILEID_OPTION = "updateFileID"; 
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
		options.addOption(UPDATE_FILEID_OPTION, true, "compared update fileID"); 
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
			String updateFileID = cmd.getOptionValue(UPDATE_FILEID_OPTION); 
			
			File resultsFile = new File(resultsFilename);
			PrintWriter out = null; 
			if (resultsFile.exists()) {
				out = new PrintWriter(new FileOutputStream(resultsFile, resultsFile.exists())); 
			}
			else {
				out = new PrintWriter(resultsFile); 
				out.println(RESULTS_HEADERS); 
			}
			
			String CTFilename = cmd.getOptionValue(CT_OPTION);
			ItemsetSet itemCT = null; 
			
			if (cmd.hasOption(VREEKEN_OPTION)) {
				String DBAnalysisFilename = cmd.getOptionValue(DB_ANALYSIS_OPTION);
				itemCT = Utils.readVreekenEtAlCodeTable(CTFilename, DBAnalysisFilename);  
			}
			else {
				itemCT = Utils.readItemsetSetFile(CTFilename);  
			}
			logger.debug("itemCT"); 
			logger.debug(itemCT); 
			
			// we load the updateFile 
			StringTokenizer filenameParser = new StringTokenizer(updateFileID, File.separator);
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
			
			UpdateTransactionsFile updFile = new UpdateTransactionsFile(year, month, day, hour, number, updateFileID); 
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
				logger.debug(updates.getUpdateTransactions()+ " updates");
				for (Couple<ItemsetSet, ItemsetSet> upd: updates.getUpdateTransactions() ) {
					firstCodLength = 0.0; 
					secondCodLength = 0.0; 
					firstTransNumber = 0; 
					secondTransNumber = 0;
					innerID++; 
					start = System.nanoTime();
					logger.debug("--- first");
					logger.debug(upd.getFirst()); 
					if (!upd.getFirst().isEmpty()) { 
						firstCodLength = Measures.codificationLengthApplyingLaplaceSmoothing(upd.getFirst(), CT); 
						firstTransNumber = upd.getFirst().size(); 
					} 
					firstCodTime = System.nanoTime()-start; 
					start = System.nanoTime();
					logger.debug("second");
					logger.debug(upd.getSecond());
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
					out.println(strBldr.toString()); 
					out.flush();
				}
			}
			out.flush();
			out.close();
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
}
