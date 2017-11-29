package com.irisa.krimp.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class Utils {

	private static Logger logger = Logger.getLogger(Utils.class);

	public static ItemsetSet readItemsetSetFile(String filename) {
		ItemsetSet result = new ItemsetSet();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			CSVParser parser = new CSVParser(reader, CSVFormat.TDF.withDelimiter(' '));
			for (CSVRecord line : parser) {
				boolean withSupport = false;
				boolean withUsage = false;
				KItemset itemsetLine = new KItemset();
				int support = 1;
				int usage = 0;
				if (line.get(0).equals('#') 
						|| line.get(0).equals('%')
						|| line.get(0).equals('@')) {
					continue;
				}
				for(int i = 0; i < line.size(); i++) {
					if( withSupport || withUsage) {
						if(withSupport) {
							support = Integer.valueOf(line.get(i));
						}
						withSupport = false;
						if(withUsage) {
							usage = Integer.valueOf(line.get(i));
						}
						withUsage = false;
					} else if(line.get(i).equals("#SUP:")) {
						withSupport = true;
						continue;
					} else if(line.get(i).equals("#USG:")) {
						withUsage = true;
						continue;
					} else {
						try {
							itemsetLine.add(Integer.valueOf(line.get(i)));
						} catch(NumberFormatException e) {
							logger.fatal(filename + " " + line + " (" + i + "): " + line.get(i), e);
						}
					}
				}
				itemsetLine.setSupport(support);
				itemsetLine.setUsage(usage);
				result.add(itemsetLine);
			}
			parser.close();
		} catch (IOException e) {
			logger.fatal(e);
		}

		return result;
	}
	
	public ItemsetSet readCodeTableCodes(String filename) {
		return readItemsetSetFile(filename);
	}

	//	public static Itemsets readItemsetFile(String filename) {
	//		Itemsets result = new Itemsets(filename);
	//		
	//		// scan the database
	//		BufferedReader reader;
	//		try {
	//			reader = new BufferedReader(new FileReader(filename));
	//			String line;
	//			// for each line (transaction) until the end of file
	//			while (((line = reader.readLine()) != null)){ 
	//				LinkedList<Integer> itemsetLine = new LinkedList<Integer>();
	//				int lineSupport = 0;
	//				// if the line is  a comment, is  empty or is a
	//				// kind of metadata
	//				if (line.isEmpty() == true ||
	//						line.charAt(0) == '#' || line.charAt(0) == '%'
	//								|| line.charAt(0) == '@') {
	//					continue;
	//				}
	//				
	//				// split the transaction into items
	//				String[] lineSplited = line.split(" ");
	//				// for each item in the
	//				// transaction
	//				boolean nextIsSupport = false;
	//				for (String itemString : lineSplited) { 
	//					try {
	//						if(itemString.isEmpty()) {
	//							continue;
	//						} else if(itemString.equals("#SUP:")) {
	//						nextIsSupport = true;
	//					} else {
	//						// convert item to integer
	//							Integer item = Integer.parseInt(itemString);
	//						if(nextIsSupport) {
	//							lineSupport = item;
	//						} else {
	//							itemsetLine.add(item);
	//						}
	//					}
	//					} catch (NumberFormatException e) {
	//						logger.error(itemString, e);
	//					}
	//				}
	//				Collections.sort(itemsetLine);
	//				result.addItemset(new Itemset(itemsetLine, lineSupport), itemsetLine.size());
	//			}
	//			// close the input file
	//			reader.close();
	//		} catch (FileNotFoundException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		} catch (IOException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		
	//		return result;
	//	}




	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces).
	 * @param transactions
	 * @param output
	 */
	public static void printItemsetSet(ItemsetSet transactions, String output) {
		printItemsetSet(transactions, output, false, true);
	}

	/**
	 * Print the codetable in the format expected by SPMF (int separated by spaces) plus an item giving the usage of patterns.
	 * @param transactions
	 * @param output
	 */
	public static void printCodeTableCodes(ItemsetSet ct, String output) {
		printItemsetSet(ct, output, false, false);
	}

	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces).
	 * @param transactions
	 * @param output
	 */
	protected static void printItemsetSet(ItemsetSet transactions, String output, boolean noSupport) {
		printItemsetSet(transactions, output, noSupport, true);
	}
	
	protected static void printItemsetSet(ItemsetSet transactions, String output, boolean noSupport, boolean noUsage) {

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output)));
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.TDF.withDelimiter(' ').withQuote(null).withIgnoreEmptyLines());

			// Writing lines
			Iterator<KItemset> itResult = transactions.iterator();
			while(itResult.hasNext()) {
				KItemset resultLine = itResult.next();
				// Ecriture des attributs types
				resultLine.forEach(new Consumer<Integer>() {

					@Override
					public void accept(Integer item) {
						try {
							printer.print(item);
						} catch (IOException e) {
							logger.error(e);
						}
					}

				});

				if(! noSupport) {
					printer.print((Object)"#SUP:");
					printer.print(resultLine.getSupport());
				}
				if(! noUsage) {
					printer.print((Object)"#USG:");
					printer.print(resultLine.getUsage());
				}
				printer.println();
			}

			printer.close();

		} catch (IOException e1) {
			logger.error(e1);
		}
	}

	public static void printTransactions(ItemsetSet transactions, String output) {
		printItemsetSet(transactions, output, true);
	}

	public static ItemsetSet readVreekenEtAlCodeTable(String ctFilename, String analysisFilename) {
		// reading the codetable in Vreeken format
		ItemsetSet tmpResult = new ItemsetSet();
		HashMap<Integer, Integer> convertionIndex = new HashMap<Integer, Integer>();
		try {
			BufferedReader readerCT = new BufferedReader(new FileReader(ctFilename));

			CSVParser parserCT = new CSVParser(readerCT, CSVFormat.TDF.withDelimiter(' ').withIgnoreEmptyLines().withTrim());
			logger.trace("reading the codetable in Vreeken format");
			int nbLineCT = 0;
			for (CSVRecord lineCT : parserCT) {
				nbLineCT++;
				if(nbLineCT < 3) { // First two lines 
					continue;
				}
				
				KItemset pattern = new KItemset();
				for(int i = 0; i < lineCT.size() ; i++) {
					if(lineCT.get(i).contains(",")) { // We take the support out of there
						String usageSupport = lineCT.get(i).replaceAll("\\(", "").replaceAll("\\)", "");
						String[] usTab = usageSupport.trim().split(",");
						String supportString = usTab[1];
						String usageString = usTab[0];
						int support = Integer.parseInt(supportString);
						int usage = Integer.parseInt(usageString);
						pattern.setSupport(support);
						pattern.setUsage(usage);
					} else if(! lineCT.get(i).isEmpty()){ // As long if its not a wandering space or the usage/support brackets, we take
						pattern.add(Integer.parseInt(lineCT.get(i)));
					} 
				}
				tmpResult.add(pattern);
			}
			parserCT.close();
			logger.trace("Codetable in Vreeken format read");

			logger.trace("Reading the DB analysis");
			// Reading the database analysis 
			BufferedReader readerAnalysis = new BufferedReader(new FileReader(analysisFilename));
			CSVParser parserAnalysis = new CSVParser(readerAnalysis, CSVFormat.TDF.withIgnoreEmptyLines().withTrim());
			boolean interestingPart = false;
			for(CSVRecord lineAnalysis : parserAnalysis) {
				if(lineAnalysis.get(0).equals("* Alphabet")) { // the interesting part of the analysis is situated between these two lines
					interestingPart = true;
				}
				if(lineAnalysis.get(0).equals("* Row lengths:")) {
					interestingPart = false;
				}
				if(interestingPart 
						&& lineAnalysis.get(0).contains("=>")) { // the only interesting part for us is the conversion index giving "newOne=>oldOne"
					StringTokenizer convertionToken = new StringTokenizer(lineAnalysis.get(0), "=>");
					int vreekenItem = Integer.parseInt((String) convertionToken.nextElement());
					int ourItem = Integer.parseInt((String) convertionToken.nextElement());
					convertionIndex.put(vreekenItem, ourItem);
				}
			}
			logger.trace("Analysis read");
			parserAnalysis.close();
		} catch (IOException e) {
			logger.fatal(e);
		}
		
		ItemsetSet result = new ItemsetSet();
		logger.trace("Converting the codetable");
		Iterator<KItemset> itVreekenCT = tmpResult.iterator();
		while(itVreekenCT.hasNext()) {
			KItemset vreekenPattern = itVreekenCT.next();
			
			KItemset ourPattern = new KItemset();
			Iterator<Integer> itVItems = vreekenPattern.iterator();
			while(itVItems.hasNext()) {
				Integer vItem = itVItems.next();
				Integer ourItem = convertionIndex.get(vItem);
				
				ourPattern.add(ourItem);
			}
			
			ourPattern.setSupport(vreekenPattern.getSupport());
			ourPattern.setUsage(vreekenPattern.getUsage());
			result.add(ourPattern);
		}
		logger.trace("Codetable converted");

		return result;
	}

	public static KItemset createCodeSingleton(int codeNum) {
		return new KItemset(Collections.singleton(codeNum));
	}

	public static KItemset createCodeSingleton(int codeNum, int support) {
		return new KItemset(Collections.singleton(codeNum), support);
	}

	public static KItemset createCodeSingleton(int codeNum, int support, int usage) {
		return new KItemset(Collections.singleton(codeNum), support, usage);
	}

	public static void main(String[] argv) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		if(argv.length == 3) {
			String ctName = argv[0];
			String analysisName = argv[1];
			String outputName = argv[2];
			ItemsetSet ct = readVreekenEtAlCodeTable(ctName, analysisName);
			printCodeTableCodes(ct, outputName);
		} else {
			logger.fatal("This program needs 3 arguments: <Vreeken et al. CT filename> <Vreeken et al. database analysis> <Output file>");
		}
	}

}
