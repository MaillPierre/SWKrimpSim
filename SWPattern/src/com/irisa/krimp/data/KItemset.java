package com.irisa.krimp.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;

public class KItemset extends HashSet<Integer> {
	
	private int _support = 0;
	private String _label = "";
	private int _usage = 0;

	public KItemset() {
	}

	public KItemset(Collection<? extends Integer> arg0) {
		super(arg0);
		if(arg0 instanceof KItemset) {
			this._support = ((KItemset) arg0).getSupport();
			this._usage = ((KItemset) arg0).getUsage();
		}
	}
	
	public KItemset(Collection<? extends Integer> arg0, int supp) {
		this(arg0, supp, 0);
	}
	
	public KItemset(Collection<? extends Integer> arg0, int supp, int usg) {
		super(arg0);
		this._support = supp;
		this._usage = usg;
	}

	protected KItemset(int initialCapacity) {
		super(initialCapacity);
	}

	public KItemset(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public int getSupport() {
		return this._support;
	}
	
	public void setSupport(int supp) {
		this._support = supp;
	}

	public KItemset intersection(KItemset code) {
		KItemset thisCopy = new KItemset(this);
		thisCopy.retainAll(code);
		return thisCopy;
	}

	public KItemset substraction(KItemset code) {
		KItemset thisCopy = new KItemset(this);
		thisCopy.removeAll(code);
		return thisCopy;
	}
	
	public boolean alphabeticalEqual(KItemset kis) {
		boolean result = false;

		if(this.size() == kis.size()) {
			LinkedList<Integer> thisList = Lists.newLinkedList(kis);
			LinkedList<Integer> kisList = Lists.newLinkedList(kis);
			Collections.sort(thisList);
			Collections.sort(kisList);
			
			for(int i = 0; i < thisList.size(); i++) {
				if(thisList.get(i) != kisList.get(i)) {
					return false;
				}
			}
			return true;
		}
		
		return result;
	}
	
	public int alphabeticalCompare(KItemset kis) {
		LinkedList<Integer> thisList = Lists.newLinkedList(kis);
		LinkedList<Integer> kisList = Lists.newLinkedList(kis);
		Collections.sort(thisList);
		Collections.sort(kisList);
		
		for(int i = 0; i < thisList.size(); i++) {
			if(thisList.get(i) != kisList.get(i)) {
				return Integer.compare(thisList.get(i), kisList.get(i));
			}
		}
		return 0;
	}
	
	public Integer[] getItems() {
		return this.toArray(new Integer[0]);
	}
	
	public List<Integer> getItemList() {
		return Lists.newArrayList(this);
	}
	
	public int getUsage() {
		return _usage;
	}

	public void setUsage(int usage) {
		this._usage = usage;
	}
	
	/** to be able to use streams and parallelization when updating the usages **/ 
	public synchronized void incrementUsageAtomically() {
		this._usage++; 
	}

	public String getLabel() {
		return _label;
	}

	public void setLabel(String _label) {
		this._label = _label;
	}

	
	
}
