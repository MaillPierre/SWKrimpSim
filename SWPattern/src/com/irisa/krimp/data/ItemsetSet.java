package com.irisa.krimp.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.irisa.krimp.KrimpAlgorithm;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * Intended to replace SPMF Itemsets
 * @author pmaillot
 *
 */
public class ItemsetSet extends LinkedList<Itemset> {

	protected HashMap<Integer, HashSet<Itemset>> itemItemsetIndex = new HashMap<Integer, HashSet<Itemset>>();
	
	public ItemsetSet() {
		super();
	}
	
	public ItemsetSet(ItemsetSet is) {
		super(is);
		itemItemsetIndex = new HashMap<Integer, HashSet<Itemset>>(is.itemItemsetIndex);
	}
	
	public ItemsetSet(Itemsets iset) {
		super();
		
		// Add the itemsets
		Iterator<List<Itemset>> itSets = iset.getLevels().iterator();
		while(itSets.hasNext()) {
			List<Itemset> list = itSets.next();
			
			addAll(list);
		}
	}
	
	public void addItemset(Itemset is) {
		super.add(is);
		for(int i = 0; i < is.getItems().length; i++) {
			if(itemItemsetIndex.get(is.get(i)) == null) {
				itemItemsetIndex.put(is.get(i), new HashSet<Itemset>());
			}
			itemItemsetIndex.get(is.get(i)).add(is);
		}
	}
	
	public String toString() {
		// Copied from smpf code, just to see ...
		StringBuilder r = new StringBuilder ();
		Iterator<Itemset> itIs = this.iterator();
		while(itIs.hasNext()) {
			Itemset is = itIs.next();
			r.append(is.toString());
			r.append(" (");
			r.append(is.getAbsoluteSupport());
			r.append(")\n");
		}
		
		return r.toString();
	}
	
	public Itemsets toItemsets() {
		Itemsets result = new Itemsets("");
		
		this.forEach(new Consumer<Itemset>() {
			@Override
			public void accept(Itemset t) {
				result.addItemset(t, t.size());
			}
		});
		
		return result;
	}
}
