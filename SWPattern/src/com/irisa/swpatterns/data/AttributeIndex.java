package com.irisa.swpatterns.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class AttributeIndex {

	private LabeledTransaction attributes = new LabeledTransaction();
	private HashMap<RDFPatternComponent, Integer> attributeItemIndex = new HashMap<RDFPatternComponent, Integer>();
	private HashMap<Integer, RDFPatternComponent> itemAttributeIndex = new HashMap<Integer, RDFPatternComponent>();
	private HashMap<RDFPatternComponent, Integer> attributeCount = new HashMap<RDFPatternComponent, Integer>();
	
	private static int counterAttribute = 0;
	
	public static int getAttributeNumber() {
		return counterAttribute++;
	}
	
	public Iterator<RDFPatternComponent> patternComponentIterator() {
		return attributeItemIndex.keySet().iterator();
	}
	
	public Iterator<Integer> itemIterator() {
		return itemAttributeIndex.keySet().iterator();
	}
	
	public boolean contains(RDFPatternComponent attr) {
		return attributes.contains(attr);
	}
	
	public int getItem(RDFPatternComponent compo) {
		return attributeItemIndex.get(compo);
	}
	
	public RDFPatternComponent getComponent(int item) {
		return itemAttributeIndex.get(item);
	}
	
	public void add(RDFPatternComponent attribute) {
		if(! contains(attribute)) {
			attributes.add(attribute);
			if(! attributeItemIndex.containsKey(attribute)) {
				attributeItemIndex.put(attribute, getAttributeNumber());
				itemAttributeIndex.put(attributeItemIndex.get(attribute), attribute );
				attributeCount.put(attribute, 0);
			}
		}
		attributeCount.replace(attribute, attributeCount.get(attribute) + 1);
	}
	
	public int getAttributeCount(RDFPatternComponent compo) {
		return this.attributeCount.get(compo);
	}

	public LabeledItemSet labelItemSet(Itemset iSet) {
		LabeledItemSet result = new LabeledItemSet();

		for(int i = 0; i < iSet.getItems().length; i++) {
			result.addItem(itemAttributeIndex.get(iSet.get(i)));
		}
		result.setCount(iSet.getAbsoluteSupport());

		return result;
	}
	
	public List<LabeledItemSet> labelItemSet(Itemsets iSets) {
		List<LabeledItemSet> result = new ArrayList<LabeledItemSet>();
		
		for(int level = 0; level < iSets.getLevels().size(); level++) {
			iSets.getLevels().forEach(new Consumer<List<Itemset>>() {
				@Override
				public void accept(List<Itemset> l) {
					l.forEach(new Consumer<Itemset>(){
						@Override
						public void accept(Itemset is) {
							result.add(labelItemSet(is));
						}
					});
				}
			});
		}
		
		return result;
	}
	
	public int size() {
		return attributes.size();
	}

	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces). Will update the attribute/item indexes
	 * @param attributes Set of all attributes appearing in the descriptions
	 * @param transactions
	 * @param output
	 * @return
	 * @throws Exception 
	 */
	public void printTransactionsItems(LinkedList<LabeledTransaction> transactions, String output) throws Exception {

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output)));
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.TDF.withDelimiter(' '));
			CSVPrinter attributePrinter = new CSVPrinter(new PrintWriter(new BufferedWriter(new FileWriter(output+".attr"))), CSVFormat.TDF);

			// Writing lines
			Iterator<LabeledTransaction> itResult = transactions.iterator();
			while(itResult.hasNext()) {
				LabeledTransaction resultLine = itResult.next();
				// Ecriture des attributs types
				Iterator<RDFPatternComponent> itTypes = resultLine.getSortedIterator();
				while(itTypes.hasNext()) {
					RDFPatternComponent res = itTypes.next();
					
					int itemIndex = getItem(res);
					printer.print(itemIndex);
				}
				
				printer.println();
			}

			printer.close();

			// Writing attributes
			Iterator<RDFPatternComponent> itAttr = patternComponentIterator();
			while(itAttr.hasNext()) {
				RDFPatternComponent attr = itAttr.next();
				attributePrinter.print(attr);
				attributePrinter.print(getItem(attr));
				attributePrinter.println();
			}
			attributePrinter.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
