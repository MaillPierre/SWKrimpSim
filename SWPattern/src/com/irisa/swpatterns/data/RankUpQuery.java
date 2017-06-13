package com.irisa.swpatterns.data;

import java.util.HashMap;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

public class RankUpQuery {
	
	private HashMap<RDFPatternComponent, String> _patternVariablesIndex;
	private HashMap<String, RDFPatternComponent> _variablesPatternIndex;
	private Query _query;
	
	public RankUpQuery(Query query, HashMap<RDFPatternComponent, String> variables, HashMap<String, RDFPatternComponent> graphPatterns ) {
		this._patternVariablesIndex = variables;
		this._variablesPatternIndex = graphPatterns;
		this._query = query;
	}
	
	public RankUpQuery(String queryString, HashMap<RDFPatternComponent, String> variables, HashMap<String, RDFPatternComponent> graphPatterns ) {
		this(QueryFactory.create(queryString), variables, graphPatterns);
	}
	
	public RDFPatternComponent getComponentFromVar(String var) {
		return this._variablesPatternIndex.get(var);
	}
	
	public String getVarFromComponent(RDFPatternComponent compo) {
		return this._patternVariablesIndex.get(compo);
	}
	
	public Query getQuery() {
		return this._query;
	}

}
