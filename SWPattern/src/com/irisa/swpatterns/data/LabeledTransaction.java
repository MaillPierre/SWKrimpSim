package com.irisa.swpatterns.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.jena.rdf.model.Resource;

/**
 * Set of RDFPattern component, represents one transaction line
 * @author pmaillot
 *
 */
@SuppressWarnings("serial")
public class LabeledTransaction extends HashSet<RDFPatternComponent> {
	
	private int _support = 0;
	private Resource _source = null;
	
	public LabeledTransaction() {
		super();
	}
	
	public LabeledTransaction(Resource source) {
		super();
		this._source = source;
	}
	
	public LabeledTransaction(Collection<RDFPatternComponent> compos, int support) {
		this(compos, null, support);
	}
	
	public LabeledTransaction(Collection<RDFPatternComponent> compos, Resource source, int support) {
		super(compos);
		setSupport(support);
		this._source = source;
	}
	
	public LabeledTransaction(LabeledTransaction _attributes) {
		super(_attributes);
		setSupport(_attributes.getSupport());
		if(_attributes.hasSource() && this.hasSource() && this.getSource() != _attributes.getSource()) {
			this._source = null;
		}
		this.setSource(_attributes.getSource());
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
	
	public boolean hasSource() {
		return this._source != null;
	}

	public Resource getSource() {
		return _source;
	}

	public void setSource(Resource _source) {
		this._source = _source;
	}

}
