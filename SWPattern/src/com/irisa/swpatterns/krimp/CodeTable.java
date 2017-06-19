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
	private HashMap<Itemset, Integer> _itemsetUsage = new HashMap<Itemset, Integer>();
	private HashMap<Itemset, Itemset> _itemsetCode = new HashMap<Itemset, Itemset>();
	private long _usageTotal = 0;
	
	public CodeTable(AttributeIndex index) {
		_index = index;
		
		initializeSingletons();
	}
	
	public Iterator<Itemset> codeIterator() {
		return _itemsetCode.keySet().iterator();
	}
	
	/**
	 * @return Iterator over a sorted temporary copy of the itemset list of the code table
	 */
	public Iterator<Itemset> sortedCodeIterator() {
		List<Itemset> tmpItemsets = new ArrayList<Itemset>(_itemsetCode.keySet());
		
		Collections.sort(tmpItemsets, standardCoverOrder);
		
		return tmpItemsets.iterator();
	}
	
	public int getUsage(Itemset is) {
		return this._itemsetUsage.get(is);
	}
	
	public Itemset getCode(Itemset is) {
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
			_itemsetCode.put(single, single);
		}
	}
	
	public void countUsages(ItemsetSet transactions) {
		Iterator<Itemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			Itemset code = itCodes.next();
			
			transactions.forEach(new Consumer<Itemset>(){
				@Override
				public void accept(Itemset trans) {
					if(isCover(trans, code)) {
						_itemsetUsage.replace(code, _itemsetUsage.get(code) +1);
					}
				}
			});
			
			this._usageTotal += _itemsetUsage.get(code);
		}
	}
	
	private Comparator<Itemset> standardCoverOrder = new Comparator<Itemset>() {
		@Override
		public int compare(Itemset o1, Itemset o2) {
			if(o1.size() != o2.size()) {
				return Integer.compare(o1.size(), o2.size());
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
	
	public static boolean isCover(Itemset trans, Itemset code) {
		return ( code.size() > 1 && trans.containsAll(code) ) || (code.size() == 1 && trans.isEqualTo(code));
	}
	
	public static Itemset addItemsets(Itemset iSet, Itemset added) {
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
	
	public static Itemset substractItemsets(Itemset iSet, Itemset substracted) {
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

		// Copied from smpf code, just to see ...
		StringBuilder r = new StringBuilder ();
		Iterator<Itemset> itIs = this.sortedCodeIterator();
		while(itIs.hasNext()) {
			Itemset is = itIs.next();
			r.append(is.toString());
			r.append(' ');
			r.append(this.getUsage(is));
			r.append('\n');
		}
		
		return r.toString();
	}
	
}
