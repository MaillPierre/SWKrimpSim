package com.irisa.swpatterns.krimp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.irisa.swpatterns.data.AttributeIndex;
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
	
	public CodeTable(AttributeIndex index) {
		_index = index;
		
		initializeSingletons();
	}
	
	public Iterator<Itemset> itemsetIterator() {
		return _itemsetCode.keySet().iterator();
	}
	
	/**
	 * @return Iterator over a sorted temporary copy of the itemset list of the code table
	 */
	public Iterator<Itemset> sortedItemsetIterator() {
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

	/**
	 * Add to the code table the singletons of all items 
	 */
	private void initializeSingletons() {
		Iterator<RDFPatternComponent> itComp = _index.patternComponentIterator();
		while(itComp.hasNext()) {
			RDFPatternComponent compo = itComp.next();
			
			int compoItem = _index.getItem(compo);
			Itemset single = new Itemset(compoItem);
			_itemsetUsage.put(single, _index.getAttributeCount(compo));
			_itemsetCode.put(single, single);
		}
	}
	
	private Comparator<Itemset> standardCoverOrder = new Comparator<Itemset>() {
		@Override
		public int compare(Itemset o1, Itemset o2) {
			if(o1.size() != o2.size()) {
				return Integer.compare(o1.size(), o2.size());
			} else if(o1.support != o2.support) {
				return Integer.compare(o1.support, o2.support);
			} else if( ! o1.isEqualTo(o2)) {
				for(int i = 0, j = 0; i < o1.size() && j < o2.size() ; i = j = i +1) {
					if(i != j) {
						return Integer.compare(o1.get(i), o2.get(j));
					}
				}
			}
			return 0;
		}
	};
	
}
