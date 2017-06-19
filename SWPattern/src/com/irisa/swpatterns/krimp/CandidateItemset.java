package com.irisa.swpatterns.krimp;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.ItemsetSet;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * EXPERIMENTAL /!\ Structure to store the candidate itemset and their information value
 * @author pmaillot
 *
 */
public class CandidateItemset extends ItemsetSet{
	
	public CandidateItemset(Itemsets sets, AttributeIndex index) {
		super(sets, index);
		init();
	}
	
	private void init() {
		Collections.sort(_items, standardCandidateOrder);
	}
	
	private static int usage(ItemsetSet collection, int item) {
		return collection.coveredItemsets(item).size();
	}
	
	private Comparator<Itemset> standardCandidateOrder = new Comparator<Itemset>(){
		@Override
		public int compare(Itemset o1, Itemset o2) {
			if(o1.support != o2.support) {
				return - Integer.compare(o1.support, o2.support);
			} else if(o1.size() != o2.size()) {
				return Integer.compare(o1.size(), o2.size());
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
}
