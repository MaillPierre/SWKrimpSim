package com.irisa.jenautils;

import org.apache.jena.query.Query;

/**
 * Allows creation of a QueryResultIterator in different contexts.
 * Was created to be able to reuse Overviewer stuff into Hub project
 * @author pmaillot
 *
 */
public interface QueryIteratorFurnisher {

	public QueryResultIterator retrieve(Query query);
	
}
