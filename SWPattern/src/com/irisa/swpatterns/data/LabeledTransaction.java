package com.irisa.swpatterns.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import com.irisa.swpatterns.data.RDFPatternComponent.Type;

/**
 * Set of RDFPattern component, represents one transaction line
 * @author pmaillot
 *
 */
@SuppressWarnings("serial")
public class LabeledTransaction extends HashSet<RDFPatternComponent> {
	
	private int _support = 0;
	public LabeledTransaction() {
		super();
	}
	
	public LabeledTransaction(Collection<RDFPatternComponent> compos, int support) {
		super(compos);
		setSupport(support);
	}
	
	public int getSupport() {
		return _support;
	}
	
	public void setSupport(int support) {
		this._support = support;
	}
	
	/**
	 * Create a sorted copy of the transaction
	 * @param comp comparator used for the sort
	 * @return iterator over a temp set
	 */
	public Iterator<RDFPatternComponent> getSortedIterator(Comparator<RDFPatternComponent> comp) {
		LinkedList<RDFPatternComponent> tmp = new LinkedList<RDFPatternComponent>(this);
		Collections.sort(tmp, comp);
		return tmp.iterator();
	}

	/**
	 * @return iterator on a temp set using the RDFPatternComponent::getComparator()
	 */
	public Iterator<RDFPatternComponent> getSortedIterator() {
		return getSortedIterator(RDFPatternComponent.getComparator());
	}

}
