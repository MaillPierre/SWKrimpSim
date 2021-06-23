///////////////////////////////////////////////////////////////////////////////
// File: TableFormatConverter.java 
// Author: Carlos Bobed
// Date: March 2018
// Comments: Class to convert a table in Vreeken format to a table in our format
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.data.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
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

import com.irisa.dbplharvest.data.UpdateTransactions;
import com.irisa.dbplharvest.data.UpdateTransactionsFile;
import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.krimp.data.Utils;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.RDFPatternComponent;
import com.irisa.swpatterns.measures.Measures;
import com.irisa.utilities.Couple;

public class TableFormatConverter {
	
	private static Logger logger = Logger.getLogger(TableFormatConverter.class);
	
	public static String CT_OPTION = "CT"; 
	public static String DB_ANALYSIS_OPTION = "DBAnalysis"; 
	public static String INDEX_OPTION = "index"; 
	public static String OUTPUT_CT = "outputCT";
	public static String HELP_OPTION = "help"; 
			
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(CT_OPTION, true, "original codeTable");
		options.addOption(DB_ANALYSIS_OPTION, true, "original db analysis file required to read Vreeken CTs");
		options.addOption(INDEX_OPTION, true, "file containing the index");
		options.addOption(OUTPUT_CT, true, "file to store the converted table");
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "UpdateExplainer", options );
				System.exit(0);
			} 
			
			AttributeIndex index = AttributeIndex.getInstance(); 
			index.readAttributeIndex(cmd.getOptionValue(INDEX_OPTION));
			
		
			String CTFilename = cmd.getOptionValue(CT_OPTION);
			ItemsetSet itemCT = null; 
			
			String DBAnalysisFilename = cmd.getOptionValue(DB_ANALYSIS_OPTION);
			itemCT = Utils.readVreekenEtAlCodeTable(CTFilename, DBAnalysisFilename);  
		
			Utils.printCodeTableCodes(itemCT, cmd.getOptionValue(OUTPUT_CT));
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
	
}
