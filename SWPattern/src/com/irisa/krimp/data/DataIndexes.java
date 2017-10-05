package com.irisa.krimp.data;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class DataIndexes {
	
	private static Logger logger = Logger.getLogger(DataIndexes.class);

	private ItemsetSet _transactions = null;
	private HashSet<Integer> _items = new HashSet<Integer>();
	private HashMap<Integer, BitSet> _itemTransactionVectors = new HashMap<Integer, BitSet>();
	private HashMap<KItemset, BitSet> _transactionItemVectors = new HashMap<KItemset, BitSet>();
	private HashMap<KItemset, BitSet> _codeItemVectors = new HashMap<KItemset, BitSet>();
	private HashMap<KItemset, BitSet> _codeTransactionVectors = new HashMap<KItemset, BitSet>();
	
	private int _highestItemIndice = 0;
	private int _maxSize = 0;
	
	public DataIndexes(ItemsetSet transactions) {
		this._transactions = transactions;
		
		analyze();
	}
	
	public int getMaxSize() {
		return this._maxSize;
	}
	
	private void analyze() {
		for(int iTrans = 0; iTrans < this._transactions.size(); iTrans++ ) {
			KItemset trans = this._transactions.get(iTrans);
			if(this._transactionItemVectors.get(trans) == null) {
				this._transactionItemVectors.put(trans, new BitSet());
			}
			if(trans.size() > this._maxSize) {
				this._maxSize = trans.size();
			}
			Iterator<Integer> itTrans = trans.iterator();
			while(itTrans.hasNext()) {
				int item = itTrans.next();
				KItemset single = Utils.createCodeSingleton(item);
				if(this._codeItemVectors.get(single) == null) {
					this._codeItemVectors.put(single, new BitSet());
					this._codeItemVectors.get(single).set(item);
				}
				if(this._codeTransactionVectors.get(single) == null) {
					this._codeTransactionVectors.put(single, new BitSet());
				}
				if(! this._items.contains(item)) {
					this._items.add(item);
				}
				if(_highestItemIndice < item) {
					this._highestItemIndice = item;
				}
				if(_itemTransactionVectors.get(item) == null) {
					this._itemTransactionVectors.put(item, new BitSet());
				}
				this._itemTransactionVectors.get(item).set(iTrans);
				this._transactionItemVectors.get(trans).set(item);
				this._codeTransactionVectors.get(single).set(iTrans);
			}
		}
	}
	
	private void computeCodeTransactionVector(KItemset code) {
//		logger.debug("computeCodeTransactionVector " + code);
		BitSet transVector = new BitSet();

		Iterator<Integer> itCode = code.iterator();
		if(itCode.hasNext()) {
			transVector.or(this.getItemTransactionVector(itCode.next()));
			while(itCode.hasNext()) {
				int item = itCode.next();
	//			logger.debug("computeCodeTransactionVector then " + item + ": " + this._itemTransactionVectors.get(item));
				
				transVector.and(this.getItemTransactionVector(item));
			}
		}
		this._codeTransactionVectors.put(code, transVector);
//		logger.debug("code: " + code + " (" + code.getAbsoluteSupport() + ") " + this._codeTransactionVectors.get(code));
		
	}
	
	private void computeCodeItemVector(KItemset code) {
		BitSet itemVector = new BitSet();

		Iterator<Integer> itCode = code.iterator();
		while(itCode.hasNext()) {
			int item = itCode.next();
			
			itemVector.set(item);
		}
		this._codeItemVectors.put(code, itemVector);
	}
	
	public int getNumberOfTransactions() {
		return this._transactions.size();
	}
	
	public int getNumberOfItems() {
		return this._items.size();
	}
	
	public int getItemSupport(int item) {
		return this.getItemTransactionVector(item).cardinality();
	}
	
	public int getCodeSupport(KItemset code) {
		return getCodeTransactionVector(code).cardinality();
	}
	
	public BitSet getTransactionItemVector(KItemset transaction) {
		return this._transactionItemVectors.get(transaction);
	}
	
	public BitSet getCodeItemVector(KItemset code) {
		if(this._codeItemVectors.get(code) == null) {
			computeCodeItemVector(code);
		}
		return this._codeItemVectors.get(code);
	}
	
	public BitSet getItemTransactionVector(int item) {
		if(! this._itemTransactionVectors.containsKey(item)) {
			this._itemTransactionVectors.put(item, new BitSet());
			for(int iTrans = 0; iTrans < this._transactions.size(); iTrans++ ) {
				KItemset trans = this._transactions.get(iTrans);
				if(trans.contains(item)) {
					this._itemTransactionVectors.get(item).set(iTrans);
					this._transactionItemVectors.get(trans).set(item);
				}
			}
		}
		return this._itemTransactionVectors.get(item);
	}
	
	public BitSet getCodeTransactionVector(KItemset code) {
		if(this._codeTransactionVectors.get(code) == null) {
			computeCodeTransactionVector(code);
		}
		return this._codeTransactionVectors.get(code);
	}
	
	public Iterator<Integer> itemIterator() {
		return this._items.iterator();
	}

}
