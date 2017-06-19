package com.irisa.swpatterns.krimp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.ItemsetSet;
import com.irisa.swpatterns.data.RDFPatternComponent;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

/**
 * @author pmaillot
 *
 */
public class CodeTable {

	private AttributeIndex _index = null;
	private ItemsetSet _transactions = null;
	private ItemsetSet _codes = null;
	private HashMap<Itemset, Integer> _itemsetUsage = new HashMap<Itemset, Integer>();
	private HashMap<Itemset, Integer> _itemsetCode = new HashMap<Itemset, Integer>();
	private long _usageTotal = 0;
	
	private static int _codeNumber = 0;
	
	/**
	 * Initialization of the usages and codes indices
	 * @param index
	 * @param transactions
	 * @param codes
	 */
	public CodeTable(AttributeIndex index, ItemsetSet transactions, ItemsetSet codes) {
		_index = index;
		_transactions = transactions;
		_codes = codes;
		
		initializeSingletons();
		initCodes();
		countUsages();
	}
	
	public static int getNewCodeNumber() {
		return _codeNumber++;
	}
	
	public ItemsetSet getTransactions() {
		return _transactions;
	}

	/**
	 * @return Iterator over a sorted temporary copy of the itemset list of the code table
	 */
	public Iterator<Itemset> codeIterator() {
		return _codes.iterator();
	}
	
	/**
	 * Create new indices for new codes, put the usage of each code to 0
	 */
	private void initCodes() {
		this._codes.forEach(new Consumer<Itemset>() {
			@Override
			public void accept(Itemset code) {
				if(_itemsetCode.get(code) == null) {
					_itemsetCode.put(code, getNewCodeNumber());
				}
				if(_itemsetUsage.get(code) == null) {
					_itemsetUsage.put(code, 0);
				}
			}
		});
		Collections.sort(_codes, standardCoverOrder);
	}
	
	public int getUsage(Itemset is) {
		return this._itemsetUsage.get(is);
	}
	
	public Integer getCodeIndice(Itemset is) {
		return this._itemsetCode.get(is);
	}
	
	public double probabilisticDistrib(Itemset code) {
		return (double) this.getUsage(code) / (double) this._usageTotal;
	}
	
	public double codeLength(Itemset code) {
		return - Math.log(this.probabilisticDistrib(code));
	}

	/**
	 * Add to the code table the singletons of all items 
	 */
	private void initializeSingletons() {
		Iterator<RDFPatternComponent> itComp = _index.patternComponentIterator();
		while(itComp.hasNext()) {
			RDFPatternComponent compo = itComp.next();
			
			int compoItem = _index.getItem(compo);
			Itemset single = new Itemset(compoItem);
			single.setAbsoluteSupport(_index.getAttributeCount(compo));
			_itemsetUsage.put(single, _index.getAttributeCount(compo));
			_itemsetCode.put(single, compoItem);
			if(! this._codes.contains(single)) {
				this._codes.addItemset(single);
			}
		}
	}
	
	/**
	 * Initialize the usage of each code according to the cover
	 */
	public void countUsages() {
		Iterator<Itemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			Itemset code = itCodes.next();
			
			_transactions.forEach(new Consumer<Itemset>(){
				@Override
				public void accept(Itemset trans) {
					if(trans.size() > 1) {
						if(isCover(trans, code)) {
							_itemsetUsage.replace(code, _itemsetUsage.get(code) +1);
						}
					}
				}
			});
			
			this._usageTotal += _itemsetUsage.get(code);
		}
	}
	
	/**
	 * Comparator to sort the code list
	 */
	private Comparator<Itemset> standardCoverOrder = new Comparator<Itemset>() {
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
			Itemset tmpCode = null;
			while(itIs.hasNext()) {
				tmpCode = itIs.next();
				
				if(isCoverCandidate(trans, tmpCode)) { // If the size of code is correct and it is contained in trans
					if(tmpCode.isEqualTo(code)) { // if code cover = OK
						return true;
					} else if(trans.isEqualTo(tmpCode)) { // if another cover code cover everything = !OK
						return false;
					}else if (tmpCode.intersection(code).size() != 0) { // if another cover code overlap with code = !OK
						return false;
					} else { // transaction partially covered but there is still some chances
						Itemset covered = CodeTable.itemsetAddition(trans, createCodeSingleton(getCodeIndice(tmpCode)));
						covered = CodeTable.itemsetSubstraction(covered, tmpCode);
						return isCover(covered, code); 
					}
				}
			}
			
		}
		return false;
	}
	
	public void removeCode(Itemset code) {
		this._codes.remove(code);
		this._itemsetCode.remove(code);
		this._itemsetUsage.remove(code);
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
	
	public static Itemset createCodeSingleton(int codeNum) {
		return new Itemset(codeNum);
	}
	
	public String toString() {

		// StringBuilder copied from smpf code, just to see ...
		StringBuilder r = new StringBuilder ();
		Iterator<Itemset> itIs = this.codeIterator();
		while(itIs.hasNext()) {
			Itemset is = itIs.next();
			r.append(is.toString());
			r.append(' ');
			r.append(is.getAbsoluteSupport());
			r.append(' ');
			r.append(this.getUsage(is));
			r.append(' ');
			r.append(this.probabilisticDistrib(is));
			r.append(' ');
			r.append(this.codeLength(is));
			r.append('\n');
		}
		
		return r.toString();
	}
	
}
