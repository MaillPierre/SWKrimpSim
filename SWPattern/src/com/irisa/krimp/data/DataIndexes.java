package com.irisa.krimp.data;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

public class DataIndexes {
	
	private static Logger logger = Logger.getLogger(DataIndexes.class);

	private ItemsetSet _transactions = null;
	private HashSet<Integer> _items = new HashSet<Integer>();
	private HashMap<Integer, BitSet> _itemTransactionVectors = new HashMap<Integer, BitSet>();
	private HashMap<Itemset, BitSet> _transactionItemVectors = new HashMap<Itemset, BitSet>();
	private HashMap<Itemset, BitSet> _codeItemVectors = new HashMap<Itemset, BitSet>();
	private HashMap<Itemset, BitSet> _codeTransactionVectors = new HashMap<Itemset, BitSet>();
	
	private int _highestItemIndice = 0;
	
	public DataIndexes(ItemsetSet transactions) {
		this._transactions = transactions;
		
		analyze();
	}
	
	private void analyze() {
		for(int iTrans = 0; iTrans < this._transactions.size(); iTrans++ ) {
			Itemset trans = this._transactions.get(iTrans);
			if(this._transactionItemVectors.get(trans) == null) {
				this._transactionItemVectors.put(trans, new BitSet());
			}
			for(int iItem = 0; iItem < trans.size(); iItem++) {
				int item = trans.get(iItem);
				Itemset single = Utils.createCodeSingleton(item);
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
	
	private void computeCodeTransactionVector(Itemset code) {
//		logger.debug("computeCodeTransactionVector " + code);
		BitSet transVector = new BitSet();
		transVector.or(this.getItemTransactionVector(code.get(0)));
		
		for(int iItem = 1; iItem < code.size(); iItem++) {
			int item = code.get(iItem);
//			logger.debug("computeCodeTransactionVector then " + item + ": " + this._itemTransactionVectors.get(item));
			
			transVector.and(this.getItemTransactionVector(item));
		}
		this._codeTransactionVectors.put(code, transVector);
//		logger.debug("code: " + code + " (" + code.getAbsoluteSupport() + ") " + this._codeTransactionVectors.get(code));
		
	}
	
	private void computeCodeItemVector(Itemset code) {
		BitSet itemVector = new BitSet();
		
		for(int iItem = 0; iItem < code.size(); iItem++) {
			int item = code.get(iItem);
			
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
	
	public int getCodeSupport(Itemset code) {
		return getCodeTransactionVector(code).cardinality();
	}
	
	public BitSet getTransactionItemVector(Itemset trans) {
		return this._transactionItemVectors.get(trans);
	}
	
	public BitSet getItemTransactionVector(int item) {
		if(! this._itemTransactionVectors.containsKey(item)) {
			this._itemTransactionVectors.put(item, new BitSet());
			for(int iTrans = 0; iTrans < this._transactions.size(); iTrans++ ) {
				Itemset trans = this._transactions.get(iTrans);
				if(trans.contains(item)) {
					this._itemTransactionVectors.get(item).set(iTrans);
					this._transactionItemVectors.get(trans).set(item);
				}
			}
		}
		return this._itemTransactionVectors.get(item);
	}
	
	public BitSet getCodeTransactionVector(Itemset code) {
		if(this._codeItemVectors.get(code) == null) {
			computeCodeItemVector(code);
		}
		if(this._codeTransactionVectors.get(code) == null) {
			computeCodeTransactionVector(code);
		}
		return this._codeTransactionVectors.get(code);
	}
	
	public Iterator<Integer> itemIterator() {
		return this._items.iterator();
	}

}
