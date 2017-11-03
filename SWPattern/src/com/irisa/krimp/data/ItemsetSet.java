package com.irisa.krimp.data;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.irisa.swpatterns.data.AttributeIndex;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * Intended to replace SPMF Itemsets
 * @author pmaillot
 *
 */
public class ItemsetSet extends LinkedList<KItemset> {

//	protected HashMap<Integer, HashSet<KItemset>> itemItemsetIndex = new HashMap<Integer, HashSet<KItemset>>();
	
	public ItemsetSet() {
		super();
	}
	
	public ItemsetSet(ItemsetSet is) {
		super(is);
//		itemItemsetIndex = new HashMap<Integer, HashSet<KItemset>>(is.itemItemsetIndex);
	}
	
	public ItemsetSet(Itemsets iset) {
		super();
		
		// Add the itemsets
		Iterator<List<Itemset>> itSets = iset.getLevels().iterator();
		while(itSets.hasNext()) {
			List<Itemset> list = itSets.next();
			
			Iterator<Itemset> itList = list.iterator();
			while(itList.hasNext()) {
				KItemset smpf = new KItemset(itList.next());
				
				add(smpf);
			}
		}
	}
	
	public void addItemset(KItemset auxCode) {
		KItemset newIs = new KItemset(auxCode);
		super.add(newIs);
//		Iterator<Integer> itAux = auxCode.iterator();
//		while(itAux.hasNext()) {
//			int item = itAux.next();
//			if(itemItemsetIndex.get(item) == null) {
//				itemItemsetIndex.put(item, new HashSet<KItemset>());
//			}
//			itemItemsetIndex.get(item).add(newIs);
//		}
	}
	
	public double averageSize() {
		int sumSize = 0;
		
		for( KItemset t : this) {
			sumSize += t.size();
		}
		
		return (double)sumSize/(double)this.size();
	}
	
	/**
	 * Refers to the attributeIndex singleton
	 * @return density value
	 */
	public double density() {
		int sumSize = 0;
		BitSet itemset = new BitSet();
		
		for( KItemset t : this) {
			sumSize += t.size();
			for( int item : t ) {
				itemset.set(item);
			}
		}
		
		return (double)sumSize/((double)this.size()*(double)itemset.cardinality());
	}
	
	public String toString() {
		// Copied from smpf code, just to see ...
		StringBuilder r = new StringBuilder ();
		Iterator<KItemset> itIs = this.iterator();
		while(itIs.hasNext()) {
			KItemset is = itIs.next();
			r.append(is.toString());
			r.append(" (");
			r.append(is.getUsage());
			r.append(",");
			r.append(is.getSupport());
			r.append(")\n");
		}
		
		return r.toString();
	}
	
	public Itemsets toItemsets() {
		Itemsets result = new Itemsets("");
		
		this.forEach(new Consumer<KItemset>() {
			@Override
			public void accept(KItemset t) {
				result.addItemset(t.toSMPFItemset(), t.size());
			}
		});
		
		return result;
	}
}
