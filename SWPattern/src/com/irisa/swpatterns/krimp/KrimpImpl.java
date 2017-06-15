package com.irisa.swpatterns.krimp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

/**
 * KRIMP magic happens here !
 * @author pmaillot
 *
 */
public class KrimpImpl {

	public Itemset standardCover(Itemset transact, CodeTable table ) {
		Itemset result = new Itemset();
		Itemset currentTrans = transact;
		
		Iterator<Itemset> itIs = table.sortedItemsetIterator();
		while(itIs.hasNext()) {
			Itemset istest = itIs.next();
			
			if( istest.size() > 1 && currentTrans.containsAll(istest)) {
				if(currentTrans.isEqualTo(istest)) {
					result = istest;
				} else {
					result = addItemsets(result, table.getCode(istest));
					result = standardCover(substractItemsets(currentTrans, istest), table);
				}
			}
		}

		return result;
	}
	
	private static Itemset addItemsets(Itemset iSet, Itemset added) {
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
	
	private static Itemset substractItemsets(Itemset iSet, Itemset substracted) {
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
	
}
