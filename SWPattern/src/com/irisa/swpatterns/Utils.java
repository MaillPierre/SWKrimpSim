package com.irisa.swpatterns;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class Utils {

	private static Logger logger = Logger.getLogger(Utils.class);

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
	
}
