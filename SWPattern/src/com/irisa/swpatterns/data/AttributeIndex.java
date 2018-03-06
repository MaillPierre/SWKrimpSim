package com.irisa.swpatterns.data;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.swpatterns.Global;
import com.irisa.swpatterns.data.RDFPatternComponent.Type;

/**
 * Singleton class containing the index for RDFComponent/Item number 
 * @author pmaillot
 *
 */
public class AttributeIndex {
	
	private Logger logger = Logger.getLogger(AttributeIndex.class);

	private LabeledTransaction _attributes = new LabeledTransaction();
	private HashMap<RDFPatternComponent, RDFPatternComponent> _instantiationIndex = new HashMap<>(); 
	private HashMap<RDFPatternComponent, Integer> _attributeItemIndex = new HashMap<RDFPatternComponent, Integer>();
	private HashMap<Integer, RDFPatternComponent> _itemAttributeIndex = new HashMap<Integer, RDFPatternComponent>();

	private static int countPattern = 0;

	private int _counterAttribute = 0;
	private BitSet _itemNumberSet = new BitSet();
	
	private static AttributeIndex _instance = null;

	private static int getPatternNumber() {
		return AttributeIndex.countPattern++;
	}
	
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
		
		this._instantiationIndex = new HashMap<>(index._instantiationIndex); 
		
		this._attributeItemIndex = new HashMap<RDFPatternComponent, Integer>(index._attributeItemIndex);
		this._itemAttributeIndex = new HashMap<Integer, RDFPatternComponent>(this._itemAttributeIndex);
		this._counterAttribute = index._counterAttribute;
		this._itemNumberSet = new BitSet();
		this._itemNumberSet.or(index._itemNumberSet);
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
		add(compo);
		return _attributeItemIndex.get(compo);
	}
	
	public RDFPatternComponent getComponent(int item) {
		return _itemAttributeIndex.get(item);
	}
	
	public RDFPatternResource getComponent(Resource res, Type type) {
		RDFPatternResource compo = new RDFPatternResource(res, type);
		add(compo);
		return (RDFPatternResource) _instantiationIndex.get(compo);
	}
	
	public RDFPatternPathFragment getComponent(Resource res1, Resource res2, Type type) {
		RDFPatternPathFragment compo = new RDFPatternPathFragment(res1, res2, type);	
		add(compo);		
		return (RDFPatternPathFragment) _instantiationIndex.get(compo);
	}
	
	public void add(RDFPatternComponent attribute) {
		_attributes.add(attribute); 
		_instantiationIndex.putIfAbsent(attribute, attribute); 
		// I have to keep the conditional due to the way attributeNumbers are handled
		if(! _attributeItemIndex.containsKey(attribute)) {
			_attributeItemIndex.put(attribute, getAttributeNumber());
			_itemAttributeIndex.put(_attributeItemIndex.get(attribute), attribute );
		}
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
	
	public KItemset convertToTransaction(LabeledTransaction t ) {
		LinkedList<Integer> resultTmp = new LinkedList<Integer>();
		Iterator<RDFPatternComponent> itCompo = t.getSortedIterator();
		while(itCompo.hasNext()) {
			RDFPatternComponent compo = itCompo.next();
			
			resultTmp.add(this.getItem(compo));
		}
		KItemset result = new KItemset(resultTmp, t.getSupport());
		result.setLabel(t.getSource().toString());
		return result;
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
		printTransactionsItems(transactions, true, output);
	}

	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces). Will update the attribute/item indexes
	 * @param transactions
	 * @param output
	 * @param debug Should the resource à the origin of the transactino appear ?
	 * @return
	 * @throws Exception 
	 */
	protected void printTransactionsItems(LinkedList<LabeledTransaction> transactions, boolean debug, String output) throws Exception {

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output)));
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.TDF.withDelimiter(' '));

			// Writing lines
			Iterator<LabeledTransaction> itResult = transactions.iterator();
			while(itResult.hasNext()) {
				LabeledTransaction resultLine = itResult.next();
				if(debug && resultLine.hasSource()) {
					printer.print(resultLine.getSource());
				}
				
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
				
				addUsedItemNumber(item);
				this._attributeItemIndex.put(compo, item);
				this._itemAttributeIndex.put(item, compo);
				this._attributes.add(compo);
				this._instantiationIndex.put(compo, compo); 
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
	
	public LinkedList<Model> rdfizePatterns(ItemsetSet liSet) {
		LinkedList<Model> result = new LinkedList<Model>();
		Iterator<KItemset> itIs = liSet.iterator();
		while(itIs.hasNext()) {
			KItemset is = itIs.next();
			result.add(rdfizePattern(is));
		}
		return result;
	}

	public Model rdfizePattern(KItemset liSet) {
		Model result = ModelFactory.createDefaultModel();
	
		Resource mainRes = result.createResource(Global.basePatternUri + getPatternNumber());
		Resource outlierRes = result.createResource(Global.basePatternUri + "Outlier");
	
		Iterator<Integer> itItems = liSet.iterator();
		while(itItems.hasNext()) {
			RDFPatternComponent item = this.getComponent(itItems.next());
			switch(item.getType()) {
			case TYPE:
				result.add(mainRes, RDF.type, ((RDFPatternResource) item).getResource());
				break;
			case IN_NEIGHBOUR:
				result.add(outlierRes, ((RDFPatternResource) item).getResource().as(Property.class), mainRes);
				break;
			case IN_NEIGHBOUR_TYPE:
				Resource subject = result.createResource();
				result.add(subject, ((RDFPatternPathFragment) item).getPathFragment().getSecond().as(Property.class), mainRes);
				result.add(subject, RDF.type, ((RDFPatternPathFragment) item).getPathFragment().getFirst());
				break;
			case IN_PROPERTY:
				result.add(result.createResource(), ((RDFPatternResource) item).getResource().as(Property.class), mainRes);
				break;
			case OUT_NEIGHBOUR:
				result.add(mainRes, ((RDFPatternResource) item).getResource().as(Property.class), outlierRes);
				break;
			case OUT_NEIGHBOUR_TYPE:
				Resource object = result.createResource();
				result.add(mainRes, ((RDFPatternPathFragment) item).getPathFragment().getFirst().as(Property.class), object);
				result.add(object, RDF.type, ((RDFPatternPathFragment) item).getPathFragment().getSecond());
				break;
			case OUT_PROPERTY:
				result.add(mainRes, ((RDFPatternResource) item).getResource().as(Property.class), result.createResource());
				break;
			default:
				break;
			}
		}
	
		result.add(result.createLiteralStatement(mainRes, result.createProperty(Global.baseDomain+"property/support"), liSet.getSupport()));
	
		return result;
	}
	
	public String graphPatternItemset(KItemset liSet) {
		StringWriter out = new StringWriter();
		this.rdfizePattern(liSet).write(out, "TURTLE");
		String result = out.toString();
		return result;
	}

	public void addUsedItemNumber(int item) {
		_itemNumberSet.set(item);
	}
	
	public boolean isItemUsed(int item ) {
		return _itemNumberSet.get(item);
	}

	/**
	 * @return an unique unused attribute number
	 */
	public int getAttributeNumber() {
		int result = _counterAttribute++;
		if(isItemUsed(result)) {
			result = this._itemNumberSet.nextClearBit(result);
		}
		addUsedItemNumber(result);
		return result;
	}
	
	
	public boolean checkInstantiationIndex () {
		boolean A = this._attributes.stream().allMatch(this._instantiationIndex::containsKey);
		boolean B = this._instantiationIndex.keySet().stream().allMatch(this._attributes::contains); 
		return A && B; 
	}
}
