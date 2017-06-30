package com.irisa.swpatterns.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class AttributeIndex {

	private LabeledTransaction attributes = new LabeledTransaction();
	private HashMap<RDFPatternComponent, Integer> attributeItemIndex = new HashMap<RDFPatternComponent, Integer>();
	private HashMap<Integer, RDFPatternComponent> itemAttributeIndex = new HashMap<Integer, RDFPatternComponent>();
	private HashMap<RDFPatternComponent, Integer> attributeCount = new HashMap<RDFPatternComponent, Integer>();
	
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
				attributeItemIndex.put(attribute, Utils.getAttributeNumber());
				itemAttributeIndex.put(attributeItemIndex.get(attribute), attribute );
				attributeCount.put(attribute, 0);
			}
		}
		attributeCount.replace(attribute, attributeCount.get(attribute) + 1);
	}
	
	public int getAttributeCount(RDFPatternComponent compo) {
		return this.attributeCount.get(compo);
	}
	
	public int getAttributeCount(int item) {
		return this.attributeCount.get(this.getComponent(item));
	}
	
	public void recount(ItemsetSet transactions) {
		this.attributes.forEach(new Consumer<RDFPatternComponent>(){
			@Override
			public void accept(RDFPatternComponent t) {
				attributeCount.replace(t, 0);
			}
		});
		
		transactions.forEach(new Consumer<Itemset>(){
			@Override
			public void accept(Itemset line) {
				for(int i = 0; i < line.size(); i ++) {
					int itemKey = line.get(i);
					RDFPatternComponent key = getComponent(itemKey);

					attributeCount.replace(key, attributeCount.get(key) + 1);
				}
			}
		});
	}

	public LabeledTransaction labelItemSet(Itemset iSet) {
		LabeledTransaction result = new LabeledTransaction();

		for(int i = 0; i < iSet.getItems().length; i++) {
			result.add(itemAttributeIndex.get(iSet.get(i)));
		}
		result.setSupport(iSet.getAbsoluteSupport());

		return result;
	}
	
	public List<LabeledTransaction> labelItemSet(Itemsets iSets) {
		List<LabeledTransaction> result = new ArrayList<LabeledTransaction>();
		
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
	
	public ItemsetSet convertToTransactions(LabeledTransactions labelSet) {
		ItemsetSet result = new ItemsetSet();
		labelSet.forEach(new Consumer<LabeledTransaction>() {
			@Override
			public void accept(LabeledTransaction t) {
				result.addItemset(convertToTransaction(t));
			}
		});
		return result;
	}
	
	public Itemset convertToTransaction(LabeledTransaction t ) {
		LinkedList<Integer> result = new LinkedList<Integer>();
		Iterator<RDFPatternComponent> itCompo = t.getSortedIterator();
		while(itCompo.hasNext()) {
			RDFPatternComponent compo = itCompo.next();
			
			result.add(this.getItem(compo));
		}
		Collections.sort(result);
		return new Itemset(result, t.getSupport());
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
			LinkedList<RDFPatternComponent> compos = new LinkedList<RDFPatternComponent>(attributeItemIndex.keySet());
			Collections.sort(compos, new Comparator<RDFPatternComponent>() {
				@Override
				public int compare(RDFPatternComponent c1, RDFPatternComponent c2) {
					return Integer.compare(getItem(c1), getItem(c2));
				}
			});
			Iterator<RDFPatternComponent> itAttr = compos.iterator();
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
