package com.irisa.krimp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.DataIndexes;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.krimp.data.Utils;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

/**
 * @author pmaillot
 *
 */
public class CodeTable {
	
	private static Logger logger = Logger.getLogger(CodeTable.class);

	protected ItemsetSet _transactions = null;
	protected ItemsetSet _codes = null;
	protected HashMap<KItemset, Integer> _itemsetUsage = new HashMap<KItemset, Integer>();
	protected HashMap<KItemset, Integer> _itemsetCode = new HashMap<KItemset, Integer>();
	protected DataIndexes _index = null;
	protected long _usageTotal = 0;

	// 	private HashMap<Itemset, BitSet> _codeSupportVector = new HashMap<Itemset, BitSet>();
	
	protected boolean _standardFlag = false; // Set true if it is the standard codetable
	protected CodeTable _standardCT = null; // Codetable containing only singletons for the coding length of a CT
	
	protected CodeTable() {
	}
	
	/**
	 * Initialization of the usages and codes indices
	 * @param index
	 * @param transactions
	 * @param codes
	 */
	public CodeTable(ItemsetSet transactions, ItemsetSet codes, DataIndexes analysis) {
		this(transactions, codes, analysis, false);
	}
	
	protected CodeTable(ItemsetSet transactions, ItemsetSet codes, DataIndexes analysis, boolean standardFlag) {
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
		_index = analysis;
		init();	
	}
	
	protected void init() {
		initSingletonSupports();
		initCodes();
		orderCodesStandardCoverageOrder();
		updateUsages();		
	}
	
	public static CodeTable createStandardCodeTable(ItemsetSet transactions, DataIndexes analysis) {
		return new CodeTable(transactions, null, analysis, true);
	}
	
	public static CodeTable createStandardCodeTable(ItemsetSet transactions) {
		return new CodeTable(transactions, null, new DataIndexes(transactions), true);
	}
	
	public CodeTable(CodeTable ct) {
		_transactions = ct._transactions;
		_codes = new ItemsetSet(ct._codes);
		_itemsetUsage = new HashMap<KItemset, Integer>(ct._itemsetUsage);
		_itemsetCode = new HashMap<KItemset, Integer>(ct._itemsetCode);
		_usageTotal = ct._usageTotal;
		_standardFlag = ct._standardFlag;
		_index = ct._index; // only depend on transactions and unmutable, no copy needed
				
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
	public void setTransactionsReinitializing(ItemsetSet transactions) {
		this._transactions = transactions;
		this._index = new DataIndexes(transactions);
		init();
	}
	
	/**
	 * Substitute the transactions we want this code table act on without reinitializing the 
	 *	supports (needed to calculate the new compression rates) 
	 * @param transactions
	 */
	public void setTransactions(ItemsetSet transactions) {
		this._transactions = transactions;
		this._index = new DataIndexes(transactions);
	}

	/**
	 * @return Iterator over a sorted temporary copy of the itemset list of the code table
	 */
	public Iterator<KItemset> codeIterator() {
		return _codes.iterator();
	}
	
	public ItemsetSet getCodes() {
		return this._codes;
	}
	
	/**
	 * Create new indices for new codes, put the usage of each code to 0
	 */
	protected void initCodes() {
		this._codes.forEach(new Consumer<KItemset>() {
			@Override
			public void accept(KItemset code) {
				if(_itemsetCode.get(code) == null) {
					_itemsetCode.put(code, Utils.getAttributeNumber());
				}
				if(_itemsetUsage.get(code) == null) {
					_itemsetUsage.put(code, 0);
				}
			}
		});
	}
	
	protected void initSingletonSupports() {
//		logger.debug("initSupport");
		
		Iterator<Integer> itItem = _index.itemIterator();
		while(itItem.hasNext()) {
			int item = itItem.next();
			KItemset single = Utils.createCodeSingleton(item);
			single.setSupport(_index.getItemSupport(item));
			if(! this._codes.contains(single)) {
				_codes.add(single);
			}
			_itemsetUsage.put(single, _index.getItemSupport(item));
			_itemsetCode.put(single, item);
		}
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
	
	public int getUsage(KItemset code) {
		if(this._itemsetUsage.get(code) == null) {
			return 0;
		}
		return this._itemsetUsage.get(code);
	}
	
	public Integer getCodeIndice(KItemset it) {
		return this._itemsetCode.get(it);
	}
	
	public double probabilisticDistrib(KItemset code) {
		return (double) this.getUsage(code) / (double) this._usageTotal;
	}
	
	/**
	 * L(code_CT(X))
	 * @param code
	 * @return
	 */
	public double codeLengthOfcode(KItemset code) {
		return - Math.log(this.probabilisticDistrib(code));
	}
	
	/**
	 * L(t | CT)  [Dirty version]
	 * PRE: codeTable in standardCoverageOrder 
	 * @param transaction
	 * @return
	 * @throws LogicException 
	 */
	public double encodedTransactionCodeLength(KItemset transaction) throws LogicException {
		double result = 0.0;
		Iterator<KItemset> itCodes = this.codeIterator();
//		logger.debug("encodingTransaction: "+transaction);
		while(itCodes.hasNext()) {
			KItemset code = itCodes.next();
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
		Iterator<KItemset> itTrans = this._transactions.iterator();
		while(itTrans.hasNext()) {
			KItemset trans = itTrans.next();
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
		Iterator<KItemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			KItemset code = itCodes.next();
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
	
	public double codeLengthOfCodeAccordingST(KItemset code) {
		double result = 0.0;
		// this method should return 0 if the codetable is not the ST
		if (!_standardFlag) {
			Iterator<Integer> itCode = code.iterator();
			while(itCode.hasNext()) {
				Integer item = itCode.next();
				result+= this._standardCT.codeLengthOfCodeAccordingST(Utils.createCodeSingleton(item)); 
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
		return ctL + teL;
	}
	
	/**
	 * Initialize the usage of each code according to the cover
	 * PRE: the codeTable must be in standardCoverTable order
	 */
	public void updateUsages() {
		this._usageTotal = 0;
		
		Iterator<KItemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			_itemsetUsage.replace(itCodes.next(), 0); 
		}
		
		for (KItemset t: this._transactions) {
			ItemsetSet codes = this.codify(t); 
			for (KItemset aux: codes) {
				_itemsetUsage.replace(aux, _itemsetUsage.get(aux)+1); 
			}
			this._usageTotal+=codes.size(); 
		}		
	}
	
	/**
	 * Comparator to sort the code list
	 */
	public static Comparator<KItemset> standardCoverOrderComparator = new Comparator<KItemset>() {
		@Override
		public int compare(KItemset o1, KItemset o2) {
			if(o1.size() != o2.size()) {
				return - Integer.compare(o1.size(), o2.size());
			} else if(o1.getSupport() != o2.getSupport()) {
				return - Integer.compare(o1.getSupport(), o2.getSupport());
			} else if( ! o1.equals(o2)) {
				return o1.alphabeticalCompare(o2);
			}
			return 0;
		}
	};
	
	/**
	 * Comparator to sort the code list
	 */
	public static Comparator<KItemset> standardCandidateOrderComparator = new Comparator<KItemset>() {
		@Override
		public int compare(KItemset o1, KItemset o2) {
			if(o1.getSupport() != o2.getSupport()) {
				return - Integer.compare(o1.getSupport(), o2.getSupport());
			} else if(o1.size() != o2.size()) {
				return - Integer.compare(o1.size(), o2.size());
			} else if( ! o1.equals(o2)) {
				return o1.alphabeticalCompare(o2);
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
	protected boolean isCoverCandidate(KItemset trans, KItemset code) {
		return ( code.size() <= trans.size()  && ( trans.containsAll(code)));
	}
	
	/**
	 * 
	 * @param transaction transaction
	 * @param code code from the codetable
	 * @return true if the code is part of the transaction cover
	 */
	public boolean isCover(KItemset transaction, KItemset code) {
		if(isCoverCandidate(transaction, code)) {
			Iterator<KItemset> itIs = codeIterator();
			return isCover(transaction, code, itIs);
			
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
	protected boolean isCover(KItemset trans, KItemset code, Iterator<KItemset> itLastTestedCode) {
		KItemset tmpCode = null;
		while(itLastTestedCode.hasNext()) {
			tmpCode = itLastTestedCode.next();
			
			if(isCoverCandidate(trans, tmpCode)) { // If the size of code is correct and it is contained in trans
				if(tmpCode.equals(code)) { // if code cover = OK
					return true;
				} else if (tmpCode.intersection(code).size() != 0) { // if another cover code overlap with code = !OK
					return false;
				} else { // transaction partially covered but there is still some chances
//					KItemset covered = CodeTable.itemsetSubstraction(trans, tmpCode);
					KItemset covered = trans.substraction(tmpCode);
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
	public ItemsetSet codify(KItemset trans) {
		KItemset auxTrans = new KItemset(trans);
		ItemsetSet result = new ItemsetSet(); 
		Iterator<KItemset> itIs = codeIterator();
		KItemset auxCode = null; 
		while (itIs.hasNext() && (auxTrans.size() != 0) ) {
			auxCode = itIs.next(); 
			if (auxTrans.containsAll(auxCode)) {
				result.add(auxCode); 
//				auxTrans = auxTrans.cloneItemSetMinusAnItemset(auxCode); 
				auxTrans = auxTrans.substraction(auxCode); 
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
	
	public ItemsetSet codifyUsingIsCover (KItemset trans) {
		ItemsetSet result = new ItemsetSet(); 
		Iterator<KItemset> itIs = codeIterator();
		
		KItemset auxCode = null; 
		while (itIs.hasNext()) {
			auxCode = itIs.next();
			if (this.isCover(trans, auxCode)) {
				result.add(auxCode); 
			}
		}
		return result; 
	}
	
	
	public void removeCode(KItemset pruneCandidate) {
		this._codes.remove(pruneCandidate);
		this._itemsetCode.remove(pruneCandidate);
		this._itemsetUsage.remove(pruneCandidate);
		// CB: removing from an ordered list must not alter the order
		updateUsages(); // Have to maintain the thing up to date ? 
		
	}
	
	public boolean contains(KItemset candidate) {
		return this._codes.contains(candidate);
	}
	
	/**
	 * Supposed to be a new code
	 * @param code
	 */
	public void addCode(KItemset code) {
		this.addCode(code, Utils.getAttributeNumber());
	}
	
	/**
	 * Add a code and its already existing indice
	 * @param code
	 * @param indice
	 */
	public void addCode(KItemset code, int indice) {
		if(! this._codes.contains(code)) {
			this._codes.add(code);
			this._itemsetCode.put(code, indice);
			this._itemsetUsage.put(code, this.getUsage(code));
			// after adding it we have to reorder 
			orderCodesStandardCoverageOrder();
			this.updateUsages(); // maintain the usage index uptodate ?
			
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
	
	// replaced by KItemset.substraction
//	public static Itemset itemsetSubstraction(KItemset trans, KItemset tmpCode) {
//		TreeSet<Integer> tmpBaseSet = new TreeSet<Integer>();
//		for(int i = 0; i < trans.getItems().length; i++) {
//			tmpBaseSet.add(trans.get(i));
//		}
//		TreeSet<Integer> tmpSubstractedSet = new TreeSet<Integer>();
//		for(int i = 0; i < tmpCode.getItems().length; i++) {
//			tmpSubstractedSet.add(tmpCode.get(i));
//		}
//		tmpBaseSet.removeAll(tmpSubstractedSet);
//		
//		return new Itemset(new ArrayList<Integer>(tmpBaseSet), trans.getAbsoluteSupport());
//	}
	
	public String toString() {
		
		// StringBuilder copied from smpf code, just to see ...
		StringBuilder r = new StringBuilder ();
		r.append("Total Usages: ");
		r.append(this._usageTotal);
		r.append(" number of codes: ");
		r.append(this._codes.size());
		r.append('\n');
		Iterator<KItemset> itIs = this.codeIterator();
		while(itIs.hasNext()) {
			KItemset is = itIs.next();
			r.append(is.toString());
			r.append(" s:"); 
			r.append(is.getSupport()); 
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
	
	public KItemset getCodeFromIndex (Integer idx) {
		
		// CB: this should be stored as an inverted index
		// done this way only for testing purposes
		KItemset result = null; 
		for (KItemset it: _codes) {
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
	
	/** 
	 * Length of the codification of a set of transactions using this code table
	 * It uses exactly the same cover order (it does not update either the support or the usage of the elements in the table) 
	 */
	public double codificationLength (ItemsetSet database) {
		double result = 0.0;
		ItemsetSet codes = null; 
		for (KItemset it: database) {
			codes = this.codify(it); 
			for (KItemset code: codes) {
				result += this.codeLengthOfcode(code); 
			}
		}
		return result; 
	}
}
	
