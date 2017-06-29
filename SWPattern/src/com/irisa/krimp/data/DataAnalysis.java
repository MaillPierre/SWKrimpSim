package com.irisa.krimp.data;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

public class DataAnalysis {

	private ItemsetSet _transactions = null;
	private HashSet<Integer> _items = new HashSet<Integer>();
	private HashMap<Integer, BitSet> _itemTransactionVectors = new HashMap<Integer, BitSet>();
	private HashMap<Itemset, BitSet> _transactionItemVectors = new HashMap<Itemset, BitSet>();
	
	private int _highestItemIndice = 0;
	
	public DataAnalysis(ItemsetSet transactions) {
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
				if(this._items.contains(item)) {
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
			}
		}
	}
	
	public long getNumberOfTransactions() {
		return this._transactions.size();
	}
	
	public long getNumberOfItems() {
		return this._items.size();
	}
	
	public long getSupportOfItem(int item) {
		return this._itemTransactionVectors.get(item).cardinality();
	}
	
	public BitSet getItemVectorOfTransaction(Itemset trans) {
		return this._transactionItemVectors.get(trans);
	}
	
	public BitSet getTransactionVectorOfItem(int item) {
		return this._itemTransactionVectors.get(item);
	}

}
