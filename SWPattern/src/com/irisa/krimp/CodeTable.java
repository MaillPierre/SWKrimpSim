package com.irisa.krimp;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.jenautils.Couple;
import com.irisa.krimp.data.DataIndexes;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.krimp.data.Utils;

/**
 * Was the central class for our own implementation of the algorithms.
 * Becomes an unchangeable contener for codes and their lengths.
 * The encoded length of a database is retrieved with codificationlength()
 * 
 * @author pmaillot
 *
 */
public class CodeTable {
	
	private static Logger logger = Logger.getLogger(CodeTable.class);

	@Deprecated
	private ItemsetSet _transactions = null;
	private ItemsetSet _codes = null;
	@Deprecated
	private HashMap<KItemset, Integer> _itemsetUsage = new HashMap<KItemset, Integer>();
	@Deprecated
	private DataIndexes _index = null;
	private long _usageTotal = 0;

	// 	private HashMap<Itemset, BitSet> _codeSupportVector = new HashMap<Itemset, BitSet>();
	
	private boolean _standardFlag = false; // Set true if it is the standard codetable
	private CodeTable _standardCT = null; // Codetable containing only singletons for the coding length of a CT
	
	@Deprecated
	/**
	 * Initialization of the usages and codes indices
	 * @Deprecated: CT not linked to a particular transaction set
	 * @param index
	 * @param transactions
	 * @param codes
	 */
	public CodeTable(ItemsetSet transactions, ItemsetSet codes, DataIndexes analysis) {
		this(transactions, codes, analysis, false);
	}

	@Deprecated
	/**
	 * @Deprecated: CT not linked to a particular transaction set
	 */
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
	
	/**
	 * Simple creation of a codetable, each pattern contains its own usage.
	 * @param codes
	 */
	public CodeTable(ItemsetSet codes) {
		this(codes, false);
	}
	
	public CodeTable(ItemsetSet codes, boolean standard) {
		this._standardFlag = standard;
		if(standard) {
			this._codes = generateStandardCodeTableCodes(codes);
			
		} else {
			this._codes = codes;
			this._standardCT = new CodeTable(codes, true);
		}
		recomputeUsageTotal();
	}
	
	/**
	 * Generate a standard code table from a normal code table by adding for each items the usage of each code containing it.
	 * @param codes
	 * @return
	 */
	private static ItemsetSet generateStandardCodeTableCodes(ItemsetSet codes) {
		ItemsetSet stdCodes = new ItemsetSet();
		HashMap<Integer, Integer> stdUsagesMap = new HashMap<Integer, Integer>();
		
		for(KItemset code : codes) {
			for(int item : code) {
				if(stdUsagesMap.get(item) == null) {
					stdUsagesMap.put(item, 0);
				}
				stdUsagesMap.put(item, stdUsagesMap.get(item) + code.getUsage());
			}
		}
		
		for(Integer item : stdUsagesMap.keySet()) {
			KItemset singleton = Utils.createCodeSingleton(item);
			singleton.setSupport(stdUsagesMap.get(item));
			singleton.setUsage(stdUsagesMap.get(item));
			
			stdCodes.add(singleton);
		}
		
		return stdCodes;
	}
	
	public void regenerateStandardCodeTable() {
		this._standardCT = new CodeTable(_codes, true);
	}

	@Deprecated
	/**
	 * 
	 * @Deprecated: Moved to CodificationMeasure or removed 
	 */
	private void init() {
		initSingletonSupports();
		initCodes();
		orderCodesStandardCoverageOrder();
		updateUsages();		
	}

	@Deprecated
	/**
	 * @Deprecated: CT is not linked to a particular set of transactions
	 * @param transactions
	 * @param analysis
	 * @return
	 */
	public static CodeTable createStandardCodeTable(ItemsetSet transactions, DataIndexes analysis) {
		return new CodeTable(transactions, null, analysis, true);
	}

	@Deprecated
	/**
	 * @Deprecated: CT is not linked to a particular set of transactions
	 * @param transactions
	 * @return
	 */
	public static CodeTable createStandardCodeTable(ItemsetSet transactions) {
		return new CodeTable(transactions, null, new DataIndexes(transactions), true);
	}
	
	public CodeTable(CodeTable ct) {
		_transactions = ct._transactions;
		_codes = new ItemsetSet(ct._codes);
		_itemsetUsage = new HashMap<KItemset, Integer>(ct._itemsetUsage);
		_usageTotal = ct._usageTotal;
		_standardFlag = ct._standardFlag;
		_index = ct._index; // only depend on transactions and unmutable, no copy needed
				
		_standardCT = ct._standardCT;
		
		// CB: We ensure that the copy is in standardCoverageOrder 
//		orderCodesStandardCoverageOrder();
	}
	
	/**
	 * 
	 * @Deprecated: CT is not linked to a particular set of transactions
	 * @return
	 */
	public ItemsetSet getTransactions() {
		return _transactions;
	}
	
	public boolean isStandard() {
		return this._standardFlag;
	}
	
	public CodeTable getStandardCodeTable() {
		return this._standardCT;
	}

	@Deprecated
	/**
	 * Trigger reinitialization of the indexes
	 * @Deprecated: CT is not linked to a particular set of transactions
	 * @param transactions
	 */
	public void setTransactionsReinitializing(ItemsetSet transactions) {
		this._transactions = transactions;
		this._index = new DataIndexes(transactions);
		init();
	}

	@Deprecated
	/**
	 * Substitute the transactions we want this code table act on without reinitializing the 
	 *	supports (needed to calculate the new compression rates) 
	 * @Deprecated: CT is not linked to a particular set of transactions
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
	
	public long getUsageTotal() {
		return this._usageTotal;
	}
	
	public void recomputeUsageTotal() {
		this._usageTotal = 0;
		for(KItemset code : _codes) {
			this._usageTotal += code.getUsage();
		}
	}

	@Deprecated
	/**
	 * Create new indices for new codes, put the usage of each code to 0
	 * @Deprecated: not used anymore
	 */
	private void initCodes() {
		this._codes.forEach(new Consumer<KItemset>() {
			@Override
			public void accept(KItemset code) {
				if(_itemsetUsage.get(code) == null) {
					_itemsetUsage.put(code, 0);
				}
			}
		});
	}

	@Deprecated
	/**
	 * 
	 * @Deprecated: Not used anymore
	 */
	private void initSingletonSupports() {
		logger.debug("initSupport");
		
		Iterator<Integer> itItem = _index.itemIterator();
		while(itItem.hasNext()) {
			int item = itItem.next();
			KItemset single = Utils.createCodeSingleton(item);
			single.setSupport(_index.getItemSupport(item));
			if(! this._codes.contains(single)) {
				_codes.add(single);
			}
			_itemsetUsage.put(single, _index.getItemSupport(item));
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

	@Deprecated
	/**
	 * 
	 * @Deprecated: Moved to CodificationMeasure
	 * @param code
	 * @return
	 */
	public int getUsage(KItemset code) {
		if(this._itemsetUsage.get(code) == null) {
			return 0;
		}
		return this._itemsetUsage.get(code);
	}

	@Deprecated
	/**
	 * 
	 * @Deprecated: Moved to CodificationMeasure
	 * @param code
	 * @return
	 */
	public double probabilisticDistrib(KItemset code) {
		return (double) this.getUsage(code) / (double) this._usageTotal;
	}

	@Deprecated
	/**
	 * @Deprecated: Moved to CodificationMeasure
	 * L(code_CT(X))
	 * @param code
	 * @return
	 */
	public double codeLengthOfcode(KItemset code) {
		return - Math.log(this.probabilisticDistrib(code));
	}
	
	/**
	 * @Deprecated: Moved to CodificationMeasure
	 * L(t | CT)  [Dirty version]
	 * PRE: codeTable in standardCoverageOrder 
	 * @param transaction
	 * @return
	 * @throws LogicException 
	 */
	@Deprecated 
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
	 * @Deprecated: Moved to CodificationMeasure
	 * L(D | CT)
	 * PRE: codetable in standardCoverageOrder
	 * @return
	 * @throws LogicException 
	 */
	@Deprecated
	public double encodedTransactionSetCodeLength() throws LogicException {
		double result = 0.0;
		Iterator<KItemset> itTrans = this._transactions.iterator();
		while(itTrans.hasNext()) {
			KItemset trans = itTrans.next();
			result += this.encodedTransactionCodeLength(trans);
		}
		
		return result;
	}

	@Deprecated
	/**
	 * @Deprecated: Moved to CodificationMeasure
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

	@Deprecated
	/** 
	 * @Deprecated: Moved to CodificationMeasure
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

	@Deprecated
	/**
	 * @Deprecated: Moved to CodificationMeasure
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
	
	@Deprecated
	/**
	 * @Deprecated: Moved to CodificationMeasure
	 * Initialize the usage of each code according to the cover
	 * PRE: the codeTable must be in standardCoverTable order
	 */
	public void updateUsages() {
		this._usageTotal = 0;
		
		Iterator<KItemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			KItemset code = itCodes.next();
			_itemsetUsage.replace(code, 0); 
			code.setUsage(0);
		}
		
		for (KItemset t: this._transactions) {
			ItemsetSet codes = this.codify(t); 
			for (KItemset aux: codes) {
				aux.setUsage(aux.getUsage()+1);
				_itemsetUsage.replace(aux, _itemsetUsage.get(aux)+1); 
			}
			this._usageTotal+=codes.size(); 
		}
		
		
		
		
	}
	
	/**
	 * Comparator to sort the code list by size -> support -> alphabetical order
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
	 * Comparator to sort the code list by support -> size -> alphabetical order
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

	@Deprecated
	/**
	 * @Deprecated: not used anymore
	 * fast check for basic conditions to be a cover of a transaction
	 * @param trans transaction
	 * @param code code
	 * @return true if code is smaller or equal and contained in the transaction
	 */
	private boolean isCoverCandidate(KItemset trans, KItemset code) {
		return ( code.size() <= trans.size()  && ( trans.containsAll(code)));
	}
	
	@Deprecated
	/**
	 * @Deprecated: not used anymore
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

	@Deprecated
	/**
	 * @Deprecated: not used anymore
	 * @param trans transaction
	 * @param code code from the codetable
	 * @param itLastTestedCode Iterator over codes, used for recursive calls to avoid re-iteration
	 *  over the whole code set when one is cover without intersection with code
	 * @return true if the code is part of the transaction cover
	 */
	private boolean isCover(KItemset trans, KItemset code, Iterator<KItemset> itLastTestedCode) {
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
	
	@Deprecated
	/** 
	 * 
	 * Codifying function according to the KRIMP paper
	 * Deprecated: Moved to CodificationMeasure
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
	 * Codifying function that is aware of the fact that there might be part of the transaction 
	 * that is not being covered by the codes contained in the code table. 
	 * 
	 *  It returns the codes used in the partial coverage, and the set of items (singletons) 
	 *  that have not been covered. 
	 * 
	 * @param trans
	 * @return
	 */
	
	public Couple<ItemsetSet, KItemset> codifyAware (KItemset trans)  {
		KItemset auxTrans = new KItemset(trans);
		ItemsetSet result = new ItemsetSet(); 
		Iterator<KItemset> itIs = codeIterator();
		KItemset auxCode = null; 
		while (itIs.hasNext() && (auxTrans.size() != 0) ) {
			auxCode = itIs.next(); 
			if (auxTrans.containsAll(auxCode)) {
				result.add(auxCode); 
				auxTrans = auxTrans.substraction(auxCode); 
			}
		}
		// currently, trans can be non-empty
		// we return both the codes used, and the remaining non-covered part of the transaction (new items) 
		return new Couple<ItemsetSet, KItemset>(result, auxTrans); 
	}

	@Deprecated
	/**
	 * Deprecated: The updating of the usages should be done only if needed
	 * @param pruneCandidate
	 */
	public void removeCode(KItemset pruneCandidate) {
		this._codes.remove(pruneCandidate);
		this._itemsetUsage.remove(pruneCandidate);
		// CB: removing from an ordered list must not alter the order
		updateUsages(); // Have to maintain the thing up to date ? 
	}
	
	public boolean contains(KItemset candidate) {
		return this._codes.contains(candidate);
	}
	
	@Deprecated
	/**
	 * Supposed to be a new code
	 * Deprecated: The updating of the usages should be done only if needed
	 * @param code
	 */
	public void addCode(KItemset code) {
		if(! this._codes.contains(code)) {
			this._codes.add(code);
			this._itemsetUsage.put(code, this.getUsage(code));
			// after adding it we have to reorder 
			orderCodesStandardCoverageOrder();
			this.updateUsages(); // maintain the usage index uptodate ?
			
		}
	}
	
	/**
	 * Add a code to the end of the code table before reordering in coverageOrder. This code MUST NOT overlap with any existing code
	 * @param code
	 */
	public void addSingleton(KItemset code) {
		if(code.size() == 1 && ! this._codes.contains(code)) {
			for(KItemset otherCode: this._codes) {
				assert (! otherCode.contains(code)); // tmp safety test to check if their is no need to update usages
			}
			this._codes.addLast(code);
		}
		orderCodesStandardCoverageOrder();
	}
	
	public String toString() {
		
		// StringBuilder copied from smpf code, just to see ...
		StringBuilder r = new StringBuilder ();
		r.append("Total Usages: ");
		r.append(this._usageTotal);
		r.append('\n');
		Iterator<KItemset> itIs = this.codeIterator();
		while(itIs.hasNext()) {
			KItemset is = itIs.next();
			r.append(is.toString());
			r.append(" s:"); 
			r.append(is.getSupport()); 
			r.append(" u:");
			r.append(is.getUsage());
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
	
	@Deprecated
	/** 
	 * Length of the codification of a set of transactions using this code table
	 * It uses exactly the same cover order (it does not update either the support or the usage of the elements in the table) 
	 * Deprecated: Moved to CodificationMeasure
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
	
	@Deprecated
	/**
	 * Deprecated: Moved to CodificationMeasure
	 */
	public void applyLaplaceSmoothingToUsages () {
		
		int totalAdded = 0;
		for (KItemset key: _itemsetUsage.keySet()) {
			totalAdded++; 
			_itemsetUsage.put(key,_itemsetUsage.get(key)+1); 
		}
		// we now add the singletons that might not have been seen in the new database
		Integer currentItem = null;
		KItemset currentKey = null; 
	    for (Iterator<Integer> itemIter = _index.itemIterator(); itemIter.hasNext(); ){
	    	currentItem = itemIter.next(); 
	    	currentKey = Utils.createCodeSingleton(currentItem); 
	    	
	    	if (!_codes.contains(currentKey)) {
	    		_codes.add(currentKey); 
	    		_itemsetUsage.put(currentKey, 1); 
	    		totalAdded++; 
	    	}
	    	else {
	    		if (_itemsetUsage.get(currentKey) == null) {
	    			_itemsetUsage.put(currentKey, 1);
	    			totalAdded++; 
	    		}
	    	}
	    }	
		_usageTotal += totalAdded; 
	}
	
}
	
