package com.irisa.swpatterns.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class ItemsetSet {

	
	private LinkedList<Itemset> _items = new LinkedList<Itemset>();
	private HashMap<Integer, HashSet<Itemset>> itemItemsetIndex = new HashMap<Integer, HashSet<Itemset>>();
	
	public ItemsetSet() {
		
	}
	
	public ItemsetSet(Itemsets iset) {
		
		// Add the itemsets
		Iterator<List<Itemset>> itSets = iset.getLevels().iterator();
		while(itSets.hasNext()) {
			List<Itemset> list = itSets.next();
			
			this._items.addAll(list);
			
			list.forEach(new Consumer<Itemset>() {
				@Override
				public void accept(Itemset is) {
					for(int i = 0; i < is.getItems().length; i++) {
						if(itemItemsetIndex.get(is.get(i)) == null) {
							itemItemsetIndex.put(is.get(i), new HashSet<Itemset>());
						}
						itemItemsetIndex.get(is.get(i)).add(is);
					}
				}
			});
		}
	}
}
