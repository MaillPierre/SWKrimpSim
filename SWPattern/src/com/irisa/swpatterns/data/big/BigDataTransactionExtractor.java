package com.irisa.swpatterns.data.big;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.Logger;

import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.swpatterns.data.LabeledTransaction;
import com.irisa.swpatterns.data.LabeledTransactions;

public class BigDataTransactionExtractor {
	
	private static Logger logger = Logger.getLogger(BigDataTransactionExtractor.class);
	
	private HashSet<Resource> _individuals = new HashSet<Resource>();

	private boolean _noTypeBool = false;
	private boolean _noInBool = false;
	private boolean _noOutBool = false;	
	
	private Neighborhood _neighborLevel = Neighborhood.PropertyAndType;
	
	private HashMap<Resource, LabeledTransaction> _buildingTransactionsTypeItems = new HashMap<Resource, LabeledTransaction>(); // TYPE items per resource
	private HashMap<Resource, LabeledTransaction> _buildingTransactionsPropertyItems = new HashMap<Resource, LabeledTransaction>(); // PROPERTY items per resource
	private HashSet<ArrayList<RDFNode>> _connectedResources = new HashSet<ArrayList<RDFNode>>(); // Co-occuring property resources to generate PROPERTY_TYPE later
	
	public boolean isKnownIndividual(Resource indiv) {
		return _individuals.contains(indiv);
	}
	
	public void addKnownIndividual(Resource indiv) {
		_individuals.add(indiv);
	}
	
	public void setNeighborLevel(Neighborhood level) {
		this._neighborLevel = level;
	}
	
	public Neighborhood getNeighborLevel() {
		return this._neighborLevel;
	}

	public boolean noTypeTriples() {
		return _noTypeBool;
	}

	public void setNoTypeTriples(boolean noTypeBool) {
		this._noTypeBool = noTypeBool;
	}

	public boolean noInTriples() {
		return _noInBool;
	}

	public void noInTriples(boolean noInBool) {
		this._noInBool = noInBool;
	}

	public boolean noOutTriples() {
		return _noOutBool;
	}

	public void setNoOutTriples(boolean noOutBool) {
		this._noOutBool = noOutBool;
	}
	
	public LabeledTransactions extractTransactionsFromFile(String filename) {
		// First, Line by line, fill the indexes
		
		// If neighborhood in property-class, generate the property-class items from the co-occuring index and the type index
		
		return null;
	}
	
}
