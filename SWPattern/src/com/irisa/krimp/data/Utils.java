package com.irisa.krimp.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class Utils {

	private static Logger logger = Logger.getLogger(Utils.class);

	private static int counterAttribute = 0;

	public static ItemsetSet readItemsetSetFile(String filename) {
		return new ItemsetSet(Utils.readItemsetFile(filename));
	}
	
	public static Itemsets readItemsetFile(String filename) {
		Itemsets result = new Itemsets(filename);
		
		// scan the database
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filename));
			String line;
			// for each line (transaction) until the end of file
			while (((line = reader.readLine()) != null)){ 
				LinkedList<Integer> itemsetLine = new LinkedList<Integer>();
				int lineSupport = 0;
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
				boolean nextIsSupport = false;
				for (String itemString : lineSplited) { 
					try {
						if(itemString.isEmpty()) {
							continue;
						} else if(itemString.equals("#SUP:")) {
						nextIsSupport = true;
					} else {
						// convert item to integer
							Integer item = Integer.parseInt(itemString);
						if(nextIsSupport) {
							lineSupport = item;
						} else {
							itemsetLine.add(item);
						}
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
	
	public static void printItemsets(Itemsets is, String output) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output)));
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.TDF.withDelimiter(' '));

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

	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces).
	 * @param transactions
	 * @param output
	 */
	public static void printItemsetSet(ItemsetSet transactions, String output) {

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output)));
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.TDF.withDelimiter(' '));

			// Writing lines
			Iterator<Itemset> itResult = transactions.iterator();
			while(itResult.hasNext()) {
				Itemset resultLine = itResult.next();
				// Ecriture des attributs types
				for(int i = 0; i < resultLine.size(); i++) {
					printer.print(resultLine.get(i));
				}
				
				printer.println();
			}

			printer.close();

		} catch (IOException e1) {
			logger.error(e1);
		}
	}

	public static Itemset createCodeSingleton(int codeNum) {
		return new Itemset(codeNum);
	}

	public static int getAttributeNumber() {
		return counterAttribute++;
	}
	
}
