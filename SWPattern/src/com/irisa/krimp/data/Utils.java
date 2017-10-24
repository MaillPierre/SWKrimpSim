package com.irisa.krimp.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class Utils {

	private static Logger logger = Logger.getLogger(Utils.class);

	private static int counterAttribute = 0;
	private static HashSet<Integer> itemNumberSet = new HashSet<Integer>();

	public static ItemsetSet readItemsetSetFile(String filename) {
		return new ItemsetSet(Utils.readItemsetFile(filename));
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
	
	public static Itemsets readItemsetFile(String filename) {
		Itemsets result = new Itemsets(filename);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

		 CSVParser parser = new CSVParser(reader, CSVFormat.TDF.withDelimiter(' '));
		 for (CSVRecord line : parser) {
			 	boolean withSupport = false;
				LinkedList<Integer> itemsetLine = new LinkedList<Integer>();
				int support = 1;
				if (line.get(0).equals('#') 
						|| line.get(0).equals('%')
						|| line.get(0).equals('@')) {
					continue;
				}
				for(int i = 0; i < line.size(); i++) {
					if( i == line.size()-1 && withSupport) {
						support = Integer.valueOf(line.get(i));
					} else if(line.get(i).equals("#SUP:")) {
						withSupport = true;
						continue;
					} else {
						try {
						itemsetLine.add(Integer.valueOf(line.get(i)));
						} catch(NumberFormatException e) {
							logger.fatal(filename + " " + line + " (" + i + "): " + line.get(i), e);
						}
					}
				}
				result.addItemset(new Itemset(itemsetLine, support), line.size());
		 }
		 parser.close();
		} catch (IOException e) {
			logger.fatal(e);
		}
		
		return result;
	}

	public static Itemsets readTransactionFile(String filename) {
		Itemsets result = new Itemsets(filename);
		
		// scan the database
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filename));
			String line;
			// for each line (transaction) until the end of file
			while (((line = reader.readLine()) != null)){ 
				LinkedList<Integer> itemsetLine = new LinkedList<Integer>();
				int lineSupport = 1;
				// if the line is  a comment, is  empty or is a
				// kind of metadata
				if (line.isEmpty() == true ||
						line.charAt(0) == '#' || line.charAt(0) == '%'
								|| line.charAt(0) == '@') {
					continue;
				}
				
				// split the transaction into items
				String[] lineSplited = line.split(" ");
				// for each item in the
				// transaction
				for (String itemString : lineSplited) { 
					try {
						if(itemString.isEmpty()) {
							continue;
					} else {
						// convert item to integer
							Integer item = Integer.parseInt(itemString);
							itemsetLine.add(item);
					}
					} catch (NumberFormatException e) {
						logger.error(itemString, e);
					}
				}
				Collections.sort(itemsetLine);
				result.addItemset(new Itemset(itemsetLine, lineSupport), itemsetLine.size());
			}
			// close the input file
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	protected static void printItemsets(Itemsets is, String output) {
		printItemsets(is, output, false);
	}
	
	protected static void printItemsets(Itemsets is, String output, boolean noSupport) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output)));
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.TDF.withDelimiter(' ').withQuote(null));

			// Writing lines
			is.getLevels().forEach(new Consumer<List<Itemset>>() {
				@Override
				public void accept(List<Itemset> l) {
					l.forEach(new Consumer<Itemset>() {
						@Override
						public void accept(Itemset t) {
							try {
								for(int i = 0; i < t.size(); i++) {
										printer.print(t.get(i));
								}
								if(! noSupport) {
									printer.print((Object)"#SUP:");
									printer.print(t.getAbsoluteSupport());
								}
								printer.println();
							} catch (IOException e) {
								logger.error(e);
							}
						}
					});
				}
			});

			printer.close();
			} catch (IOException e) {
				logger.error(e);
			}
	}
	
	public static void printTransactions(Itemsets is, String output) {
		printItemsets(is, output, true);
	}

	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces).
	 * @param transactions
	 * @param output
	 */
	public static void printItemsetSet(ItemsetSet transactions, String output) {
		printItemsetSet(transactions, output, false);
	}

	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces).
	 * @param transactions
	 * @param output
	 */
	protected static void printItemsetSet(ItemsetSet transactions, String output, boolean noSupport) {

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

	public static KItemset createCodeSingleton(int codeNum) {
		return new KItemset(Collections.singleton(codeNum));
	}

	public static int getAttributeNumber() {
		int result = counterAttribute++;
		if(itemNumberSet.contains(result)) {
			result = getAttributeNumber();
		}
		itemNumberSet.add(result);
		return result;
	}
	
	public static void addUsedItemNumber(int item) {
		itemNumberSet.add(item);
	}
	
}
