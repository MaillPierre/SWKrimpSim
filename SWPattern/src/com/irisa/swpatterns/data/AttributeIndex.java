package com.irisa.swpatterns.data;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
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
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * Singleton class containing the index for RDFComponent/Item number 
 * @author pmaillot
 *
 */
public class AttributeIndex {
	
	private Logger logger = Logger.getLogger(AttributeIndex.class);

	private LabeledTransaction _attributes = new LabeledTransaction();
	private HashMap<RDFPatternComponent, Integer> _attributeItemIndex = new HashMap<RDFPatternComponent, Integer>();
	private HashMap<Integer, RDFPatternComponent> _itemAttributeIndex = new HashMap<Integer, RDFPatternComponent>();
	
	private static AttributeIndex _instance = null;
	
	public static AttributeIndex getInstance() {
		if(_instance == null) {
			_instance = new AttributeIndex();
		}
		return _instance;
	}
	
	protected AttributeIndex() {
		
	}
	
	protected AttributeIndex(AttributeIndex index) {
		this._attributes = new LabeledTransaction(index._attributes);
		this._attributeItemIndex = new HashMap<RDFPatternComponent, Integer>(index._attributeItemIndex);
		this._itemAttributeIndex = new HashMap<Integer, RDFPatternComponent>(this._itemAttributeIndex);
	}

	public Iterator<RDFPatternComponent> patternComponentIterator() {
		return _attributeItemIndex.keySet().iterator();
	}
	
	public Iterator<Integer> itemIterator() {
		return _itemAttributeIndex.keySet().iterator();
	}
	
	public boolean contains(RDFPatternComponent attr) {
		return _attributes.contains(attr);
	}
	
	public int getItem(RDFPatternComponent compo) {
		if(! _attributeItemIndex.containsKey(compo)) {
			add(compo);
		}
		return _attributeItemIndex.get(compo);
	}
	
	public RDFPatternComponent getComponent(int item) {
		return _itemAttributeIndex.get(item);
	}
	
	public void add(RDFPatternComponent attribute) {
		if(! contains(attribute)) {
			_attributes.add(attribute);
			if(! _attributeItemIndex.containsKey(attribute)) {
				_attributeItemIndex.put(attribute, Utils.getAttributeNumber());
				_itemAttributeIndex.put(_attributeItemIndex.get(attribute), attribute );
			}
		}
	}

	public LabeledTransaction labelItemSet(Itemset iSet) {
		LabeledTransaction result = new LabeledTransaction();

		for(int i = 0; i < iSet.getItems().length; i++) {
			result.add(_itemAttributeIndex.get(iSet.get(i)));
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
		return _attributes.size();
	}

	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces). Will update the attribute/item indexes
	 * @param _attributes Set of all attributes appearing in the descriptions
	 * @param transactions
	 * @param output
	 * @return
	 * @throws Exception 
	 */
	public void printTransactionsItems(LinkedList<LabeledTransaction> transactions, String output) throws Exception {

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output)));
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.TDF.withDelimiter(' '));

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
			printAttributeIndex(output + ".attr");

			printer.close();

		} catch (IOException e1) {
			logger.fatal(e1);
		}
	}
	
	public void printAttributeIndex(String filename) {
		try {
		CSVPrinter attributePrinter = new CSVPrinter(new PrintWriter(new BufferedWriter(new FileWriter(filename))), CSVFormat.TDF);
		
		// Writing attributes
//		LinkedList<RDFPatternComponent> compos = new LinkedList<RDFPatternComponent>(_attributeItemIndex.keySet());
//		Collections.sort(compos, new Comparator<RDFPatternComponent>() {
//			@Override
//			public int compare(RDFPatternComponent c1, RDFPatternComponent c2) {
//				return Integer.compare(getItem(c1), getItem(c2));
//			}
//		});
		
		Iterator<RDFPatternComponent> itAttr = _attributeItemIndex.keySet().iterator();
		while(itAttr.hasNext()) {
			RDFPatternComponent attr = itAttr.next();
			List<Object> recordList = attr.toList();
			recordList.add(getItem(attr));
			attributePrinter.printRecord(recordList);
//			attributePrinter.println();
		}

		attributePrinter.close();
		} catch (IOException e) {
			logger.error(e);
		}
	}
	
	public void readAttributeIndex(String filename) {
		try {
			Reader in = new FileReader(filename);
			Iterable<CSVRecord> records = CSVFormat.TDF.parse(in);
			for (CSVRecord record : records) {
				String itemS = record.get(record.size()-1);
				int item = Integer.parseInt(itemS);
				RDFPatternComponent compo;
				switch(record.size()) {
				case 3:
					compo = RDFPatternComponent.parse(record.get(0), record.get(1));
					break;
				case 4:
					compo = RDFPatternComponent.parse(record.get(0), record.get(1), record.get(2));
					break;
				default:
					throw new LogicException("Couldn't parse line " + record.getRecordNumber() + " : " + record);
				}
				
				Utils.addUsedItemNumber(item);
				this._attributeItemIndex.put(compo, item);
				this._itemAttributeIndex.put(item, compo);
				this._attributes.add(compo);
			}
		} catch (IOException e) {
			logger.error(e);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		Iterator<Integer> itItem = this.itemIterator();
		while(itItem.hasNext()) {
			int item = itItem.next();
			
			builder.append(item);
			builder.append(' ');
			builder.append("=>");
			builder.append(' ');
			builder.append(this._itemAttributeIndex.get(item).toString());
			builder.append('\n');
		}
		
		return builder.toString();
	}
}
