package com.irisa.swpatterns.data;

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
public class RankNAttributeSet extends HashSet<RDFPatternComponent> {
	
	public Iterator<RDFPatternComponent> getSortedIterator(Comparator<RDFPatternComponent> comp) {
		LinkedList<RDFPatternComponent> tmp = new LinkedList<RDFPatternComponent>(this);
		try {
			Collections.sort(tmp, comp);
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println(this.toString());
			throw e;
		}
		return tmp.iterator();
	}

	public Iterator<RDFPatternComponent> getSortedIterator() {
		return getSortedIterator(RDFPatternComponent.getComparator());
	}

}
