package com.irisa.swpatterns.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * Intended to replace SPMF Itemsets
 * @author pmaillot
 *
 */
public class ItemsetSet {

	
	protected LinkedList<Itemset> _items = new LinkedList<Itemset>();
	protected HashMap<Integer, HashSet<Itemset>> itemItemsetIndex = new HashMap<Integer, HashSet<Itemset>>();
	protected AttributeIndex _index = null;
	
	public ItemsetSet() {
	}
	
	public ItemsetSet(Itemsets iset, AttributeIndex index) {
		
		// Add the itemsets
		Iterator<List<Itemset>> itSets = iset.getLevels().iterator();
		while(itSets.hasNext()) {
			List<Itemset> list = itSets.next();
			
			this._items.addAll(list);
			
			list.forEach(new Consumer<Itemset>() {
				@Override
				public void accept(Itemset is) {
					addItemset(is);
				}
			});
		}
	}
	
	public Iterator<Itemset> iterator() {
		return this._items.iterator();
	}
	
	public Collection<Itemset> coveredItemsets(int item) {
		return itemItemsetIndex.get(item);
	}
	
	public static boolean isCover(Itemset trans, Itemset code) {
		return ( code.size() > 1 && trans.containsAll(code) ) || (code.size() == 1 && trans.isEqualTo(code));
	}
	
	public void addItemset(Itemset is) {
		for(int i = 0; i < is.getItems().length; i++) {
			if(itemItemsetIndex.get(is.get(i)) == null) {
				itemItemsetIndex.put(is.get(i), new HashSet<Itemset>());
			}
			itemItemsetIndex.get(is.get(i)).add(is);
		}
	}
	
	public void forEach(Consumer<Itemset> cons) {
		this._items.forEach(cons);
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
}
