package com.irisa.krimp;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.DataAnalysis;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

/**
 * @author pmaillot
 *
 */
public class CodeTable {
	
	private static Logger logger = Logger.getLogger(CodeTable.class);

//	private AttributeIndex _index = null;
//	private HashMap<Integer, Integer> _singletonSupports = new HashMap<Integer, Integer>(); // Numerical supports for singletons
	private ItemsetSet _transactions = null;
	private ItemsetSet _codes = null;
	private HashMap<Itemset, Integer> _itemsetUsage = new HashMap<Itemset, Integer>();
	private HashMap<Itemset, Integer> _itemsetCode = new HashMap<Itemset, Integer>();
//	private HashMap<Itemset, BitSet> _codeSupport = new HashMap<Itemset, BitSet>(); // bitset supports for all codes
	private DataAnalysis _analysis = null;
	private long _usageTotal = 0;

	// 	private HashMap<Itemset, BitSet> _codeSupportVector = new HashMap<Itemset, BitSet>();
	
	private boolean _standardFlag = false; // Set true if it is the standard codetable
	private CodeTable _standardCT = null; // Codetable containing only singletons for the coding length of a CT
	
	/**
	 * Initialization of the usages and codes indices
	 * @param index
	 * @param transactions
	 * @param codes
	 */
	public CodeTable(ItemsetSet transactions, ItemsetSet codes, DataAnalysis analysis) {
		this(transactions, codes, analysis, false);
	}
	
	protected CodeTable(ItemsetSet transactions, ItemsetSet codes, DataAnalysis analysis, boolean standardFlag) {
		_transactions = transactions;
		if(codes == null) {
			_codes = new ItemsetSet();
		} else {
			_codes = new ItemsetSet(codes);
		}
		_standardFlag = standardFlag;
		
		if(codes != null) {
			_standardCT = CodeTable.createStandardCodeTable(transactions, analysis);
		} else { // this is a standard codetable
			_standardCT = null;
			_codes = new ItemsetSet();
		}
		_analysis = analysis;
		init();	
	}
	
	private void init() {
		initSupports();
//		initializeSingletons();
		initCodes();
		orderCodesStandardCoverageOrder();
		countUsages();		
	}
	
	public static CodeTable createStandardCodeTable(ItemsetSet transactions, DataAnalysis analysis) {
		return new CodeTable(transactions, null, analysis, true);
	}
	
	public static CodeTable createStandardCodeTable(ItemsetSet transactions) {
		return new CodeTable(transactions, null, new DataAnalysis(transactions), true);
	}
	
	public CodeTable(CodeTable ct) {
//		_index = ct._index;
//		_singletonSupports = new HashMap<Integer, Integer>(ct._singletonSupports);
		_transactions = ct._transactions;
		_codes = new ItemsetSet(ct._codes);
		_itemsetUsage = new HashMap<Itemset, Integer>(ct._itemsetUsage);
		_itemsetCode = new HashMap<Itemset, Integer>(ct._itemsetCode);
//		_codeSupport = new HashMap<Itemset, BitSet>(ct._codeSupport);
		_usageTotal = ct._usageTotal;
		_standardFlag = ct._standardFlag;
		_analysis = ct._analysis; // only depend on transactions and unmutable, no copy needed
				
		_standardCT = ct._standardCT;
		
		// CB: We ensure that the copy is in standardCoverageOrder 
		orderCodesStandardCoverageOrder();
	}

	public ItemsetSet getTransactions() {
		return _transactions;
	}
	
	/**
	 * Trigger reinitialization of the indexes
	 * @param transactions
	 */
	public void setTransactions(ItemsetSet transactions) {
		this._transactions = transactions;
		this._analysis = new DataAnalysis(transactions);
		init();
	}

	/**
	 * @return Iterator over a sorted temporary copy of the itemset list of the code table
	 */
	public Iterator<Itemset> codeIterator() {
		return _codes.iterator();
	}
	
	public ItemsetSet getCodes() {
		return this._codes;
	}
	
	/**
	 * Create new indices for new codes, put the usage of each code to 0
	 */
	private void initCodes() {
		this._codes.forEach(new Consumer<Itemset>() {
			@Override
			public void accept(Itemset code) {
				if(_itemsetCode.get(code) == null) {
					_itemsetCode.put(code, Utils.getAttributeNumber());
				}
				if(_itemsetUsage.get(code) == null) {
					_itemsetUsage.put(code, 0);
				}
			}
		});
	}
	
	private void initSupports() {
		logger.debug("initSupport");
//		for(int iTrans = 0; iTrans < this._transactions.size(); iTrans++) {
//			Itemset trans = this._transactions.get(iTrans);
//			// Init singletons supports and support vectors
//			for(int i = 0; i < trans.size() ; i++) {
//				int item = trans.get(i);
//				Itemset single = createCodeSingleton(item);
////				if(_singletonSupports.get(item) == null) {
////					_singletonSupports.put(item, 0);
////				}
//				_codeSupport.putIfAbsent(single, new BitSet(this._transactions.size()));
////				_singletonSupports.replace(item, _singletonSupports.get(item) + 1);
//				_codeSupport.get(single).set(iTrans);
//			}
//		}
		Iterator<Integer> itItem = _analysis.itemIterator();
		while(itItem.hasNext()) {
			int item = itItem.next();
			Itemset single = Utils.createCodeSingleton(item);
			single.setAbsoluteSupport(_analysis.getItemSupport(item));
			_codes.add(single);
//			_codeSupport.put(single, _analysis.getItemTransactionVector(item));
			_itemsetUsage.put(single, _analysis.getItemSupport(item));
			_itemsetCode.put(single, item);
		}
		
//		// init other codes supports vectors
//		Iterator<Itemset> itCode = this._codes.iterator();
//		while(itCode.hasNext()) {
//			Itemset code = itCode.next();
//			if(code.size() > 1 ) { // singletons were already initialized above, so we rely on them
//				initCodeSupport(code);
//			}
//		}
	}
	
//	/**
//	 * init the support vector of a code by doing a AND operation of all of its constituting singletons support vectors
//	 * @param code
//	 */
//	private void initCodeSupport(Itemset code) {
//		BitSet candidateSupport = new BitSet(this._transactions.size());
//		candidateSupport.set(0, candidateSupport.size()-1);
//		for(int iItem = 0; iItem < code.size(); iItem++) {
//			int item = code.get(iItem);
//			candidateSupport.and(this._analysis.getItemTransactionVector(item));
//		}
//		this._codeSupport.put(code, candidateSupport);		
//	}
	
	public int getUsage(Itemset is) {
		if(this._itemsetUsage.get(is) == null) {
			return 0;
		}
		return this._itemsetUsage.get(is);
	}
	
	public Integer getCodeIndice(Itemset is) {
		return this._itemsetCode.get(is);
	}
	
	public double probabilisticDistrib(Itemset code) {
		return (double) this.getUsage(code) / (double) this._usageTotal;
	}
	
	/**
	 * L(code_CT(X))
	 * @param code
	 * @return
	 */
	public double codeLengthOfcode(Itemset code) {
		return - Math.log(this.probabilisticDistrib(code));
	}
	
	/**
	 * L(t | CT)  [Dirty version]
	 * PRE: codeTable in standardCoverageOrder 
	 * @param transaction
	 * @return
	 * @throws LogicException 
	 */
	public double encodedTransactionCodeLength(Itemset transaction) throws LogicException {
		double result = 0.0;
		Iterator<Itemset> itCodes = this.codeIterator();
//		logger.debug("encodingTransaction: "+transaction);
		while(itCodes.hasNext()) {
			Itemset code = itCodes.next();
			if(this.getUsage(code) > 0 && isCover(transaction, code) ) {
//				logger.debug("coveredBy: "+code);
				result += codeLengthOfcode(code);
				if(result == Double.NEGATIVE_INFINITY || result == Double.POSITIVE_INFINITY) {
					throw new LogicException( "INFINITY: " + transaction + " code: " + code + " usage: " + this.getUsage(code) + " cover ?: " + this.isCover(transaction, code)); // If the code is cover, it shouldn't have an usage = 0
				}
//				logger.debug("newLength: "+result);
			}
		}
		return result;
	}
	
	/**
	 * L(D | CT)
	 * PRE: codetable in standardCoverageOrder
	 * @return
	 * @throws LogicException 
	 */
	public double encodedTransactionSetCodeLength() throws LogicException {
		double result = 0.0;
		Iterator<Itemset> itTrans = this._transactions.iterator();
		while(itTrans.hasNext()) {
			Itemset trans = itTrans.next();
			result += this.encodedTransactionCodeLength(trans);
		}
		
		return result;
	}
	
	/**
	 * L(CT|D)
	 * @return
	 */
	public double codeTableCodeLength() {
		double result = 0.0;
		Iterator<Itemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			Itemset code = itCodes.next();
			if(this.getUsage(code) != 0.0) {
				// CB: this is the code length according to the CT
				double cL = codeLengthOfcode(code);
				
				// CB: we also need the code length according to the ST: we codify the codeusing it
				double stcL = 0 ;
				if (!_standardFlag) {
					stcL = this._standardCT.codeLengthOfCodeAccordingST(code);
				}
				// else => it is a 0.0
				
//				if(code.size() == 1 && ! this._standardFlag) {
//					stcL = this._standardCT.codeLengthOfcode(code);
//				} else if(this._standardFlag) {
//					stcL = cL;
//				}
				
				result += cL + stcL;
			}
		}
		return result;
	}
	
	/** 
	 * L(code_ST(X))
	 */
	
	public double codeLengthOfCodeAccordingST(Itemset code) {
		double result = 0.0;
		// this method should return 0 if the codetable is not the ST
		if (!_standardFlag) {
			for (int i=0; i<code.size(); i++) {
				result+= this._standardCT.codeLengthOfCodeAccordingST(new Itemset(code.get(i))); 
			}
		}
		return result; 
	}
	
	/**
	 * L(D, CT)
	 * @return
	 * @throws LogicException 
	 */
	public double totalCompressedSize() throws LogicException {
		double ctL = codeTableCodeLength();
		double teL = encodedTransactionSetCodeLength();
//		logger.debug("CodeTable Length: " + ctL + " transactionLength: " + teL);
		return ctL + teL;
	}

//	/**
//	 * Add the singletons of all items to the code table 
//	 */
//	private void initializeSingletons() {
//		Iterator<Integer> itItems = _singletonSupports.keySet().iterator();
//		while(itItems.hasNext()) {
//			Integer item = itItems.next();
//			
//			Itemset single = new Itemset(item);
//			single.setAbsoluteSupport(_singletonSupports.get(item));
//			if(this._codes.contains(single)) {
//				this._codes.removeFirstOccurrence(single);
//				_itemsetUsage.remove(single);
//				_itemsetCode.remove(single);
//			}
//			_itemsetUsage.put(single, single.getAbsoluteSupport());
//			_itemsetCode.put(single, item);
//			this._codes.addItemset(single);
//		}
//	}
	
	/**
	 * Initialize the usage of each code according to the cover
	 * PRE: the codeTable must be in standardCoverTable order
	 */
	protected void countUsages() {
//		logger.debug("countUsages");
		this._usageTotal = 0;
		Iterator<Itemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			Itemset code = itCodes.next();
			
			_itemsetUsage.replace(code, 0);
			
			int itrans = this._analysis.getCodeTransactionVector(code).nextSetBit(0);
			while(itrans >= 0) {
				Itemset trans = this._transactions.get(itrans);
				if(isCover(trans, code)) {
					_itemsetUsage.replace(code, _itemsetUsage.get(code) +1);
				}
				itrans = this._analysis.getCodeTransactionVector(code).nextSetBit(itrans+1);
			}
			
			this._usageTotal += _itemsetUsage.get(code);
		}
//		logger.debug("usages: " + this._itemsetUsage);
	}
	
	/**
	 * Comparator to sort the code list
	 */
	public static Comparator<Itemset> standardCoverOrderComparator = new Comparator<Itemset>() {
		@Override
		public int compare(Itemset o1, Itemset o2) {
			if(o1.size() != o2.size()) {
				return - Integer.compare(o1.size(), o2.size());
			} else if(o1.support != o2.support) {
				return - Integer.compare(o1.support, o2.support);
			} else if( ! o1.isEqualTo(o2)) {
				for(int i = 0 ; i < o1.size() ; i++) {
					if(o1.get(i) != o2.get(i)) {
						return Integer.compare(o1.get(i), o2.get(i));
					}
				}
			}
			return 0;
		}
	};
	
	/**
	 * Comparator to sort the code list
	 */
	public static Comparator<Itemset> standardCandidateOrderComparator = new Comparator<Itemset>() {
		@Override
		public int compare(Itemset o1, Itemset o2) {
			if(o1.support != o2.support) {
				return - Integer.compare(o1.support, o2.support);
			} else if(o1.size() != o2.size()) {
				return - Integer.compare(o1.size(), o2.size());
			} else if( ! o1.isEqualTo(o2)) {
				for(int i = 0 ; i < o1.size() ; i++) {
					if(o1.get(i) != o2.get(i)) {
						return Integer.compare(o1.get(i), o2.get(i));
					}
				}
			}
			return 0;
		}
	};
	
	/**
	 * fast check for basic conditions to be a cover of a transaction
	 * @param trans transaction
	 * @param code code
	 * @return true if code is smaller or equal and contained in the transaction
	 */
	private boolean isCoverCandidate(Itemset trans, Itemset code) {
		return ( code.size() <= trans.size()  && ( trans.containsAll(code)));
	}
	
	/**
	 * 
	 * @param trans transaction
	 * @param code code from the codetable
	 * @return true if the code is part of the transaction cover
	 */
	public boolean isCover(Itemset trans, Itemset code) {
		if(isCoverCandidate(trans, code)) {
			Iterator<Itemset> itIs = codeIterator();
			return isCover(trans, code, itIs);
			
		}
		return false;
	}
	
	/**
	 * 
	 * @param trans transaction
	 * @param code code from the codetable
	 * @param itLastTestedCode Iterator over codes, used for recursive calls to avoid re-iteration
	 *  over the whole code set when one is cover without intersection with code
	 * @return true if the code is part of the transaction cover
	 */
	private boolean isCover(Itemset trans, Itemset code, Iterator<Itemset> itLastTestedCode) {
		Itemset tmpCode = null;
		while(itLastTestedCode.hasNext()) {
			tmpCode = itLastTestedCode.next();
			
			if(isCoverCandidate(trans, tmpCode)) { // If the size of code is correct and it is contained in trans
				if(tmpCode.isEqualTo(code)) { // if code cover = OK
					return true;
				} else if (tmpCode.intersection(code).size() != 0) { // if another cover code overlap with code = !OK
					return false;
				} else { // transaction partially covered but there is still some chances
					Itemset covered = CodeTable.itemsetSubstraction(trans, tmpCode);
					return isCover(covered, code, itLastTestedCode); 
				}
			}
		}
		return false;
	}
	

	/** 
	 * 
	 * Codifying function according to the KRIMP paper
	 * 
	 * @param trans
	 * @return
	 */
	public ItemsetSet codify(Itemset trans) {
		Itemset auxTrans = new Itemset(trans.itemset);
		ItemsetSet result = new ItemsetSet(); 
		Iterator<Itemset> itIs = codeIterator();
		Itemset auxCode = null; 
		while (itIs.hasNext() && (auxTrans.size() != 0) ) {
			auxCode = itIs.next(); 
			if (auxTrans.containsAll(auxCode)) {
				result.add(auxCode); 
				auxTrans = auxTrans.cloneItemSetMinusAnItemset(auxCode); 
			}
		}
		assert trans.size() == 0; // this should always happen 
		return result; 
	}
	
	/** 
	 * 
	 * Codifying function using isCover function
	 * 
	 */
	
	public ItemsetSet codifyUsingIsCover (Itemset trans) {
		ItemsetSet result = new ItemsetSet(); 
		Iterator<Itemset> itIs = codeIterator();
		
		Itemset auxCode = null; 
		while (itIs.hasNext()) {
			auxCode = itIs.next();
			if (this.isCover(trans, auxCode)) {
				result.add(auxCode); 
			}
		}
		return result; 
	}
	
	
	public void removeCode(Itemset code) {
		this._codes.remove(code);
		this._itemsetCode.remove(code);
		this._itemsetUsage.remove(code);
		// CB: removing from an ordered list must not alter the order
		countUsages(); // Have to maintain the thing up to date ? 
		
	}
	
	public boolean contains(Itemset code) {
		return this._codes.contains(code);
	}
	
	/**
	 * Supposed to be a new code
	 * @param code
	 */
	public void addCode(Itemset code) {
		this.addCode(code, Utils.getAttributeNumber());
	}
	
	/**
	 * Add a code and its already existing indice
	 * @param code
	 * @param indice
	 */
	public void addCode(Itemset code, int indice) {
		if(! this._codes.contains(code)) {
			this._codes.add(code);
			this._itemsetCode.put(code, indice);
			this._itemsetUsage.put(code, this.getUsage(code));
			// after adding it we have to reorder 
			orderCodesStandardCoverageOrder();
			this.countUsages(); // maintain the usage index uptodate ?
			
		}
	}
	
	public static Itemset itemsetAddition(Itemset iSet, Itemset added) {
		TreeSet<Integer> tmpBaseSet = new TreeSet<Integer>();
		for(int i = 0; i < iSet.getItems().length; i++) {
			tmpBaseSet.add(iSet.get(i));
		}
		TreeSet<Integer> tmpAddedSet = new TreeSet<Integer>();
		for(int i = 0; i < added.getItems().length; i++) {
			tmpAddedSet.add(added.get(i));
		}
		tmpBaseSet.addAll(tmpAddedSet);
		
		return new Itemset(new ArrayList<Integer>(tmpBaseSet), iSet.getAbsoluteSupport());
	}
	
	public static Itemset itemsetSubstraction(Itemset iSet, Itemset substracted) {
		TreeSet<Integer> tmpBaseSet = new TreeSet<Integer>();
		for(int i = 0; i < iSet.getItems().length; i++) {
			tmpBaseSet.add(iSet.get(i));
		}
		TreeSet<Integer> tmpSubstractedSet = new TreeSet<Integer>();
		for(int i = 0; i < substracted.getItems().length; i++) {
			tmpSubstractedSet.add(substracted.get(i));
		}
		tmpBaseSet.removeAll(tmpSubstractedSet);
		
		return new Itemset(new ArrayList<Integer>(tmpBaseSet), iSet.getAbsoluteSupport());
	}
	
	public String toString() {
		
		// StringBuilder copied from smpf code, just to see ...
		StringBuilder r = new StringBuilder ();
		r.append("Total Usages: ");
		r.append(this._usageTotal);
		r.append('\n');
		Iterator<Itemset> itIs = this.codeIterator();
		while(itIs.hasNext()) {
			Itemset is = itIs.next();
			r.append(is.toString());
			r.append(" s:"); 
			r.append(is.getAbsoluteSupport()); 
			r.append(" u:");
			r.append(this.getUsage(is));
			r.append(" P:");
			r.append(this.probabilisticDistrib(is));
			r.append(" L:");
			r.append(this.codeLengthOfcode(is));
			r.append('\n');
		}
		return r.toString();
	}
	
	public void orderCodesStandardCoverageOrder() {
		Collections.sort(this._codes, CodeTable.standardCoverOrderComparator);
	}
	
	public void orderCodesStandardCandidateOrder() {
		Collections.sort(this._codes, CodeTable.standardCandidateOrderComparator);
	}
	
	public Itemset getCodeFromIndex (Integer idx) {
		
		// CB: this should be stored as an inverted index
		// done this way only for testing purposes
		Itemset result = null; 
		for (Itemset it: _codes) {
			Integer aux = _itemsetCode.get(it); 
			if (aux != null) {
				if (aux.equals(idx)) {
					result = it; 
					break; // early termination
				}
			}
		}
		return result; 
		
	}
	
	
}
