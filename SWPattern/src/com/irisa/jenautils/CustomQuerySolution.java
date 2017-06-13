package com.irisa.jenautils;

import java.util.Iterator;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * Simple surcharge of a HashMap for query result handling
 * @author pmaillot
 *
 */
public class CustomQuerySolution extends java.util.HashMap<String, RDFNode> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5442810975647460560L;

	public Iterator<String> varNames() {
		return this.keySet().iterator();
	}
	
	public Resource getResource(String resString) {
		if(this.get(resString) != null && this.get(resString).isResource()) {
			return this.get(resString).asResource();
		}
		return null;
	}
}
