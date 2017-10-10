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

import org.apache.log4j.Logger;

import com.irisa.krimp.data.KItemset;


public class CTLengthDifferences {

	private static Logger logger = Logger.getLogger(CTLengthDifferences.class);
	
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
		
		logger.debug("updateStats >> keySize: "+differences.keySet().size());
		for (KItemset key: differences.keySet()) {
			meanDifferences+=differences.get(key); 
		}
		logger.debug("updateStats >> meanAgg: "+meanDifferences);
		if (!differences.isEmpty()) {
			meanDifferences= meanDifferences/(double)differences.size(); 
		}
		logger.debug("updateStats >> mean: "+meanDifferences);
		
		for (KItemset key: differences.keySet()) {
			varDifferences += Math.pow(differences.get(key)-meanDifferences, 2.0); 
		}
		logger.debug("updateStats >> varAgg: "+varDifferences);
		if (!differences.isEmpty()) {
			varDifferences = varDifferences/(double)differences.size(); 					
		}
		logger.debug("updateStats >> var: "+varDifferences);
	}

	public double getMeanDifferences() {
		logger.debug("getMeanDifferences >>"+meanDifferences);
		if (Double.isNaN(meanDifferences)) {
			logger.debug("getMeanDifferences >> updating");
			updateStats();
		}
		logger.debug("getMeanDifferences >>" +meanDifferences);
		return meanDifferences;
	}

	public double getVarDifferences() {
		logger.debug("getVarDifferences >>"+varDifferences);		
		if (Double.isNaN(varDifferences)) {
			logger.debug("getVarDifferences >>"+varDifferences);
			updateStats();
		}
		logger.debug("getVarDifferences >>"+varDifferences);
		return varDifferences;
	}
	
	
	
}
