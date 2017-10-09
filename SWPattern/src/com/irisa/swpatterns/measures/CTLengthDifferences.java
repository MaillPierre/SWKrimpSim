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

import com.irisa.krimp.data.KItemset;

public class CTLengthDifferences {

	HashMap<KItemset, Double> differences = null; 
	
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
	
}
