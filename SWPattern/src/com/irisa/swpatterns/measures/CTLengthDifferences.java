///////////////////////////////////////////////////////////////////////////////
// File: CTLengthDifferences.java 
// Author: Carlos Bobed 
// Date: October 2017
// Comments: Class that stores the differences in length of a CT over two 
// 		different databases
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures;

import java.util.HashMap;

import org.apache.jena.sparql.sse.Item;

import com.irisa.krimp.data.KItemset;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

public class CTLengthDifferences {

	HashMap<KItemset, Double> differences = null; 
	
	private double meanDifferences = Double.NaN; 
	private double varDifferences = Double.NaN; 
	
	public CTLengthDifferences() {
		differences = new HashMap<>(); 
	}
	
	public void putDifference (KItemset key, double value) {
		differences.put(key, value); 
	}
	
	public double getDifference(KItemset key) {		
		assert differences.containsKey(key); 
		return differences.get(key); 
	}
	
	public void updateStats() {
		meanDifferences = 0; 
		varDifferences = 0; 
		for (KItemset key: differences.keySet()) {
			meanDifferences+=differences.get(key); 
		}
		if (!differences.isEmpty()) {
			meanDifferences= meanDifferences/(double)differences.size(); 
		}
		
		for (KItemset key: differences.keySet()) {
			varDifferences += Math.pow(differences.get(key)-meanDifferences, 2.0); 
		}
		if (!differences.isEmpty()) {
			varDifferences = varDifferences/(double)differences.size(); 					
		}
		
	}

	public double getMeanDifferences() {
		if (meanDifferences == Double.NaN) {
			updateStats();
		}
		return meanDifferences;
	}

	public double getVarDifferences() {
		if (varDifferences == Double.NaN) {
			updateStats();
		}
		return varDifferences;
	}
	
	
	
}
