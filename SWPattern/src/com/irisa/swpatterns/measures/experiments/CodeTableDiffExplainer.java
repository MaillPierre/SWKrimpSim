///////////////////////////////////////////////////////////////////////////////
// File: CodeTableDiffExplainer.java 
// Author: Carlos Bobed
// Date: March 2018
// Comments:
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures.experiments;

import java.io.File;
import java.io.FileOutputStream;
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

public class CodeTableDiffExplainer {
	
	private static Logger logger = Logger.getLogger(CodeTableDiffExplainer.class);
	
	public static String RESULTS_FILE_OPTION = "resultsFile"; 
	public static String EXPLANATIONS_FILE_OPTION = "explanationsFile"; 
	public static String CT_BASE_OPTION = "CTBase";
	public static String CT_COMPARED_OPTION = "CTCompared"; 
	public static String DB_ANALYSIS_BASE_OPTION = "DBAnalysisBase";
	public static String DB_ANALYSIS_COMPARED_OPTION = "DBAnalysisCompared";
	public static String INDEX_OPTION = "index";  
	public static String VREEKEN_BASE_OPTION = "vreekenFormatBase";
	public static String VREEKEN_COMPARED_OPTION = "vreekenFormatCompared";
	public static String HELP_OPTION = "help"; 
	
	public static String CALCULATE_EXPLANATION_OPTION = "withExplanation";
	
	
	public static String RESULTS_HEADERS = "CTBase;CTCompared;dist";  
			
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(RESULTS_FILE_OPTION, true, ".csv file to include the results - append if exists");
		options.addOption(EXPLANATIONS_FILE_OPTION, true, ""); 
		options.addOption(CT_BASE_OPTION, true, "codeTable of the dataset AGAINST which we compare the udpate");
		options.addOption(CT_COMPARED_OPTION, true, "codeTableto be compared");
		options.addOption(DB_ANALYSIS_BASE_OPTION, true, "original db analysis file required to read Vreeken CTs");
		options.addOption(DB_ANALYSIS_COMPARED_OPTION, true, "original db analysis file required to read Vreeken CTs");
		options.addOption(INDEX_OPTION, true, "file containing the index"); 
		options.addOption(VREEKEN_BASE_OPTION, false, "whether we use or not the Vreeken Format"); 
		options.addOption(VREEKEN_COMPARED_OPTION, false, "whether we use or not the Vreeken Format"); 
		options.addOption(HELP_OPTION, false, "display this help"); 
		try  {
			CommandLine cmd = parser.parse( options, args);
			
			boolean helpAsked = cmd.hasOption(HELP_OPTION);
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "UpdateExplainer", options );
				System.exit(0);
			} 
			
			String resultsFilename = cmd.getOptionValue(RESULTS_FILE_OPTION); 

			AttributeIndex index = AttributeIndex.getInstance(); 
			index.readAttributeIndex(cmd.getOptionValue(INDEX_OPTION));
			
			File resultsFile = new File(resultsFilename);
			PrintWriter out = null; 
			if (!resultsFile.exists()) { 
				out = new PrintWriter(new FileOutputStream(resultsFile));
				out.println(RESULTS_HEADERS);
			}
			else { 
				out = new PrintWriter(new FileOutputStream(resultsFile, true)); 
			}
			
			String CTBaseFilename = cmd.getOptionValue(CT_BASE_OPTION);
			ItemsetSet itemCTBase = null; 
			
			if (cmd.hasOption(VREEKEN_BASE_OPTION)) {
				String DBAnalysisBaseFilename = cmd.getOptionValue(DB_ANALYSIS_BASE_OPTION);
				itemCTBase = Utils.readVreekenEtAlCodeTable(CTBaseFilename, DBAnalysisBaseFilename);  
			}
			else {
				itemCTBase = Utils.readItemsetSetFile(CTBaseFilename);  
			}
			
			String CTComparedFilename = cmd.getOptionValue(CT_COMPARED_OPTION);
			ItemsetSet itemCTCompared = null; 
			
			if (cmd.hasOption(VREEKEN_COMPARED_OPTION)) {
				String DBAnalysisComparedFilename = cmd.getOptionValue(DB_ANALYSIS_COMPARED_OPTION);
				itemCTCompared = Utils.readVreekenEtAlCodeTable(CTComparedFilename, DBAnalysisComparedFilename);  
			}
			else {
				itemCTCompared = Utils.readItemsetSetFile(CTComparedFilename);  
			}
			
			double structDiff = Measures.CTStructuralComparison(new CodeTable(itemCTCompared), new CodeTable(itemCTBase)); 
			
			out.println(cmd.getOptionValue(CT_BASE_OPTION) + ";"+ cmd.getOptionValue(CT_COMPARED_OPTION)+";"+structDiff ); 
			
			out.flush();
			out.close();
			
			
			if (cmd.hasOption(CALCULATE_EXPLANATION_OPTION)) { 
				if (!itemCTCompared.isEmpty()) { 
				
					out = new PrintWriter(new FileOutputStream(new File(cmd.getOptionValue(EXPLANATIONS_FILE_OPTION)))); 
					
					ArrayList<Couple<KItemset, ItemsetSet>>codification = null; 
					codification = Measures.codificationExternalApplyingLaplaceSmoothingIncludingSCT(itemCTCompared, new CodeTable(itemCTBase)); 
					
					
					out.println("-------------------------------------"); 
					out.println("Codification transactions: ");
					out.println("-------------------------------------"); 
					int count = 1; 
					for (Couple<KItemset, ItemsetSet> transaction: codification){
						out.print("("+count+") :: "); 
						out.println(transaction.getFirst()); 
						out.println("Codes: "); 
						for (KItemset code: transaction.getSecond()) {
							out.println("\t"+code); 
						}
						count++; 
					}
					
					out.println("--------------------------------------"); 
					out.println("TTL translation "); 
					out.println("--------------------------------------"); 
					printTTL(out, codification, "Q");
					out.flush();
					out.close();
					
				}
			} 
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1); 
		}
	}
	
	public static void printTTL(PrintWriter out, ArrayList<Couple<KItemset, ItemsetSet>> values, String miniNamespace) {
		int id = 1; 
		int idExistential = 1; 
		int idCode = 1; 
		AttributeIndex index = AttributeIndex.getInstance(); 
		String baseInstances = "http://experiments/"+miniNamespace+"/";
		String baseExistentials = baseInstances + "exist/"; 
		HashSet<KItemset> codesUsed = new HashSet<>(); 
		String resourceID = null;
		RDFPatternComponent currentPattern = null; 
		for (Couple<KItemset, ItemsetSet> transaction: values){
			resourceID = baseInstances + id; 
			out.println("# transaction: "+transaction.getFirst()); 
			out.println("# resourceID: "+resourceID); 
			for (Integer singleton: transaction.getFirst()) {
				currentPattern = index.getComponent(singleton); 
				switch(currentPattern.getType()) { 
					case TYPE: 
						out.println("<"+resourceID + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+ currentPattern.getElement().getResource().toString()+"> . "); 
						break;
					case IN_PROPERTY: 
						out.println("<"+baseExistentials+idExistential+ "> <"+ currentPattern.getElement().getResource().toString()+"> "+
								"<"+resourceID + "> ."); 
						idExistential++; 
						break; 
					case OUT_PROPERTY: 
						out.println("<"+resourceID + "> <"+ currentPattern.getElement().getResource().toString()+"> "+
								 "<"+baseExistentials+idExistential+ "> . ");
						idExistential++; 
						break; 
					case IN_NEIGHBOUR_TYPE: 
						out.println("<"+baseExistentials+idExistential + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+ currentPattern.getElement().getCouple().getSecond().toString()+"> . "); 
						out.println("<"+baseExistentials+idExistential+ "> <"+ currentPattern.getElement().getCouple().getFirst().toString()+"> "+
								"<"+resourceID + "> ."); 
						idExistential++; 
						break; 
					case OUT_NEIGHBOUR_TYPE: 
						out.println("<"+baseExistentials+idExistential + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+ currentPattern.getElement().getCouple().getSecond().toString()+"> . "); 
						out.println("<"+resourceID + "> <"+ currentPattern.getElement().getCouple().getFirst().toString()+"> "+
								 "<"+baseExistentials+idExistential+ "> . ");
						idExistential++; 
						break; 
					default: 
						out.println ("# this shouldn't happen ... id: "+singleton); 
						break; 
				}
			}
			id++; 
			codesUsed.addAll(transaction.getSecond()); 	
		}
		out.println("-----------------------------"); 
		for (KItemset code: codesUsed){
			resourceID = baseInstances+"code/" + idCode; 
			out.println("# code: "+code); 
			out.println("# codeID: "+resourceID); 
			for (Integer singleton: code) {
				currentPattern = index.getComponent(singleton); 
				switch(currentPattern.getType()) { 
					case TYPE: 
						out.println("<"+resourceID + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+ currentPattern.getElement().getResource().toString()+"> . "); 
						break;
					case IN_PROPERTY: 
						out.println("<"+baseExistentials+idExistential+ "> <"+ currentPattern.getElement().getResource().toString()+"> "+
								"<"+resourceID + "> ."); 
						idExistential++; 
						break; 
					case OUT_PROPERTY: 
						out.println("<"+resourceID + "> <"+ currentPattern.getElement().getResource().toString()+"> "+
								 "<"+baseExistentials+idExistential+ "> . ");
						idExistential++; 
						break; 
					case IN_NEIGHBOUR_TYPE: 
						out.println("<"+baseExistentials+idExistential + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+ currentPattern.getElement().getCouple().getSecond().toString()+"> . "); 
						out.println("<"+baseExistentials+idExistential+ "> <"+ currentPattern.getElement().getCouple().getFirst().toString()+"> "+
								"<"+resourceID + "> ."); 
						idExistential++; 
						break; 
					case OUT_NEIGHBOUR_TYPE: 
						out.println("<"+baseExistentials+idExistential + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+ currentPattern.getElement().getCouple().getSecond().toString()+"> . "); 
						out.println("<"+resourceID + "> <"+ currentPattern.getElement().getCouple().getFirst().toString()+"> "+
								 "<"+baseExistentials+idExistential+ "> . ");
						idExistential++; 
						break; 
					default: 
						out.println ("# this shouldn't happen ... id: "+singleton); 
						break; 
				}
			}
			idCode++; 
		}
	}
}
