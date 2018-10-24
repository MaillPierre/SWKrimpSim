///////////////////////////////////////////////////////////////////////////////
// File: CodeLengthCalculator.java 
// Author: Carlos Bobed
// Date: February 2018
// Comments: Program that calculates the code lengths of a code table 
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
import java.util.Collections;
import java.util.Comparator;
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
import com.irisa.krimp.data.KItemset;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.measures.CodificationMeasure;
import com.irisa.swpatterns.measures.Measures;
import com.irisa.utilities.Couple;

public class CodeLengthCalculator {
		
	private static Logger logger = Logger.getLogger(CodeLengthCalculator.class);
	
	public static String RESULTS_FILE_OPTION = "resultsFile"; 
	public static String CT_OPTION = "CT"; 
	public static String DB_ANALYSIS_OPTION = "DBAnalysis"; 
	public static String VREEKEN_OPTION = "vreekenFormat"; 
	public static String HELP_OPTION = "help"; 
			
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(RESULTS_FILE_OPTION, true, ".csv file to include the results - append if exists");
		options.addOption(CT_OPTION, true, "codeTable of the dataset AGAINST which we compare the udpate");
		options.addOption(DB_ANALYSIS_OPTION, true, "original db analysis file required to read Vreeken CTs"); 
		options.addOption(VREEKEN_OPTION, false, "whether we use or not the Vreeken Format"); 
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "CodeLengthCalculator", options );
				System.exit(0);
			} 
			
			String resultsFilename = cmd.getOptionValue(RESULTS_FILE_OPTION);
			
			// first, we read all the file names in memory to paralellize everything
			List<String> filenames= new ArrayList<String>(); 
			
				File resultsFile = new File(resultsFilename);
				final PrintWriter out; 
				if (resultsFile.exists()) {
					out = new PrintWriter(new FileOutputStream(resultsFile, resultsFile.exists())); 
				}
				else {
					out = new PrintWriter(resultsFile); 
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
				CodeTable CT = new CodeTable(itemCT);
				CodificationMeasure measure = new CodificationMeasure(CT);
				
				ArrayList<Couple<KItemset,Double>> codeInfo = new ArrayList<>(); 
				measure.getCodetable().getCodes().forEach( code -> {
					Double value = measure.codeLengthOfCode(code); 
					codeInfo.add(new Couple<>(code,value)); 
				});
				
				Collections.sort(codeInfo, new Comparator<Couple<KItemset,Double>>() {
					public int compare(Couple<KItemset, Double> o1, Couple<KItemset, Double> o2) {
						if (o1.getSecond() < o2.getSecond()) return 1; 
						else if (o1.getSecond() == o2.getSecond()) return 0; 
						else return -1; 
					}
				});
				
				for (Couple<KItemset,Double> c: codeInfo) {
					out.println(c.getFirst());
					out.println("codeLength: "+c.getSecond()); 
				}
				
				out.flush();
				out.close();
				
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
