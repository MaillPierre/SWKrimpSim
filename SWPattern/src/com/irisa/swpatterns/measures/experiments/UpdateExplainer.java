///////////////////////////////////////////////////////////////////////////////
// File: UpdateExplainer.java 
// Author: Carlos Bobed
// Date: February 2018
// Comments:
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures.experiments;

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

public class UpdateExplainer {
	
	private static Logger logger = Logger.getLogger(UpdateExplainer.class);
	
	public static String RESULTS_FILE_OPTION = "resultsFile"; 
	public static String CT_OPTION = "CT"; 
	public static String DB_ANALYSIS_OPTION = "DBAnalysis"; 
	public static String UPDATEID_OPTION = "updateID"; 
	public static String INDEX_OPTION = "index"; 
	public static String UPDATE_FILE_OPTION = "updateFile"; 
	public static String VREEKEN_OPTION = "vreekenFormat"; 
	public static String HELP_OPTION = "help"; 
	
	public static String RESULTS_HEADERS = "CT;updateID;prevCodSize;prevCodSizeSCT;postCodSize;postCodSizeSCT;#prevTransactions;#postTransactions;prevCodTime;postCodTime";  
			
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
	
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption(RESULTS_FILE_OPTION, true, ".csv file to include the results - append if exists");
		options.addOption(CT_OPTION, true, "codeTable of the dataset AGAINST which we compare the udpate");
		options.addOption(DB_ANALYSIS_OPTION, true, "original db analysis file required to read Vreeken CTs");
		options.addOption(UPDATE_FILE_OPTION, true, "file that contains the update to be compared"); 
		options.addOption(UPDATEID_OPTION, true, "compared update ID");
		options.addOption(INDEX_OPTION, true, "file containing the index"); 
		options.addOption(VREEKEN_OPTION, false, "whether we use or not the Vreeken Format"); 
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
			String updateFileID = cmd.getOptionValue(UPDATE_FILE_OPTION);
			Integer updateID = Integer.valueOf(cmd.getOptionValue(UPDATEID_OPTION)); 
			
			AttributeIndex index = AttributeIndex.getInstance(); 
			index.readAttributeIndex(cmd.getOptionValue(INDEX_OPTION));
			
			File resultsFile = new File(resultsFilename);
			PrintWriter out = new PrintWriter(resultsFile); 
			out.println(RESULTS_HEADERS); 
			
			String CTFilename = cmd.getOptionValue(CT_OPTION);
			ItemsetSet itemCT = null; 
			
			if (cmd.hasOption(VREEKEN_OPTION)) {
				String DBAnalysisFilename = cmd.getOptionValue(DB_ANALYSIS_OPTION);
				itemCT = Utils.readVreekenEtAlCodeTable(CTFilename, DBAnalysisFilename);  
			}
			else {
				itemCT = Utils.readItemsetSetFile(CTFilename);  
			}
			
			// we load the updateFile 
			StringTokenizer filenameParser = new StringTokenizer(updateFileID, File.separator);
			Stack<String> auxStack = new Stack<String>(); 
			while (filenameParser.hasMoreElements()) { 
				auxStack.push(filenameParser.nextToken());
			}
			
			String number = auxStack.pop(); 
			String hour = auxStack.pop(); 
			String day = auxStack.pop(); 
			String month = auxStack.pop(); 
			String year = auxStack.pop();
			
			UpdateTransactionsFile updFile = new UpdateTransactionsFile(year, month, day, hour, number, updateFileID); 
			UpdateTransactions updates = new UpdateTransactions(updFile); 
		
			if (!updates.isEmpty()) { 
				CodeTable CT = new CodeTable(itemCT);
				Couple<Double, Double> firstCodLength; 
				Couple<Double, Double> secondCodLength; 
				long firstTransNumber = 0; 
				long secondTransNumber = 0; 
				long innerID = 0; 
				long firstCodTime = 0; 
				long secondCodTime = 0; 
				long start = 0; 
				Couple<ItemsetSet,ItemsetSet> upd = updates.getUpdateTransactions().get(updateID-1);
				
				firstTransNumber = 0; 
				secondTransNumber = 0;
				innerID++; 
				start = System.nanoTime();
				logger.debug("--- first");
				logger.debug(upd.getFirst()); 
				if (!upd.getFirst().isEmpty()) { 
					firstCodLength = Measures.codificationLengthApplyingLaplaceSmoothingIncludingSCT(upd.getFirst(), CT); 
					firstTransNumber = upd.getFirst().size(); 
				} 
				else { 
					firstCodLength = new Couple<Double, Double>(0.0,0.0); 
				}
				firstCodTime = System.nanoTime()-start; 
				start = System.nanoTime();
				logger.debug("second");
				logger.debug(upd.getSecond());
				if (!upd.getSecond().isEmpty()) { 
					secondCodLength = Measures.codificationLengthApplyingLaplaceSmoothingIncludingSCT(upd.getSecond(), CT); 
					secondTransNumber = upd.getSecond().size(); 
				}
				else { 
					secondCodLength = new Couple<Double, Double>(0.0, 0.0); 
				}
				secondCodTime = System.nanoTime()-start; 
				
				StringBuilder strBldr = new StringBuilder(); 
				strBldr.append(CTFilename); 
				strBldr.append(";");
				logger.debug(updates.getID()); 
				logger.debug(updFile.getBaseFilename());
				logger.debug(updFile.getYear());
				logger.debug(updFile.getMonth());
				logger.debug(updFile.getDay());
				logger.debug(updFile.getHour());
				logger.debug(updFile.getNumber());
				logger.debug("--------");
				logger.debug(updates.getYear());
				logger.debug(updates.getMonth());
				logger.debug(updates.getDay());
				logger.debug(updates.getHour());
				logger.debug(updates.getNumber());
				strBldr.append(updates.getID());
				strBldr.append("-"); 
				strBldr.append(String.format("%010d", innerID));
				strBldr.append(";");
				strBldr.append(firstCodLength.getFirst());
				strBldr.append(";"); 
				strBldr.append(firstCodLength.getSecond());
				strBldr.append(";");
				strBldr.append(secondCodLength.getFirst());
				strBldr.append(";");
				strBldr.append(secondCodLength.getSecond());
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
				
				Couple<ArrayList<Couple<KItemset, ItemsetSet>>, ArrayList<Couple<KItemset, ItemsetSet>>> codification = null; 
				codification = Measures.codificationUpdateStatesApplyingLaplaceSmoothingIncludingSCT(upd.getFirst(), upd.getSecond(), CT); 
				
				
				out.println("-------------------------------------"); 
				out.println("Q transactions: ");
				out.println("-------------------------------------"); 
				int count = 1; 
				for (Couple<KItemset, ItemsetSet> transaction: codification.getFirst()){
					out.print("("+count+") :: "); 
					out.println(transaction.getFirst()); 
					out.println("Codes: "); 
					for (KItemset code: transaction.getSecond()) {
						out.println("\t"+code); 
					}
					count++; 
				}
				out.println("-------------------------------------"); 
				out.println("QPrima transactions: ");
				out.println("-------------------------------------"); 
				count = 1; 
				for (Couple<KItemset, ItemsetSet> transaction: codification.getSecond()){
					out.print("("+count+") :: "); 
					out.println(transaction.getFirst()); 
					out.println("Codes: "); 
					for (KItemset code: transaction.getSecond()) {
						out.println("\t"+code); 
					}
					count++; 
				}
				
				out.println("--------------------------------------"); 
				out.println("TTL translation Q"); 
				out.println("--------------------------------------"); 
				printTTL(out, codification.getFirst(), "Q");
				out.println("--------------------------------------"); 
				out.println("TTL translation Q'"); 
				out.println("--------------------------------------"); 
				printTTL(out, codification.getSecond(), "QPrima"); 
				
			}
			out.flush();
			out.close();
			
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
		String baseInstances = "http://experiments/"+miniNamespace+"-";
		String baseExistentials = baseInstances + "exist-"; 
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
			resourceID = baseInstances+"code-" + idCode; 
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
