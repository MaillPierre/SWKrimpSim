package com.irisa.jenautils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF.MODE;

/**
 * An iterator built over a query execution that will move over the results using LIMIT / OFFSET to avoid asking for to much results at the same time.
 * In case of an exception, it will try again a limited number of times, after having waited for a set amount of time
 * @author pmaillot
 *
 */
public class QueryResultIterator implements Iterator<CustomQuerySolution> {

	private QueryExecution _exec;
	private Query _query;
	private BaseRDF _base = null;
	private Model _data = null;
	private ResultSet _result = null;
	private int _limit;
	private int _offset;
	private int _resultCount;
	private int _errorWaitingTime;
	private static int _defaultNbTriesMax = 5;
	private static int _defaultLimit = 1000;
	private static int _defaultErrorWaitingTime = 15000;
	private static String _defaultDefaultGraph = null;
	private int _nbTriesMax;
	private int _nbTries = 0;
	private boolean _hasLimit = false;

	private static Logger logger = Logger.getLogger(QueryResultIterator.class);

	public QueryResultIterator(String queryString, Model data) {
		this(QueryFactory.create(queryString), data);
	}
	public QueryResultIterator(Query query, Model data) {
		this._data = data;
		init(query);
	}
	
	public QueryResultIterator(String queryString, BaseRDF base) {
		this(QueryFactory.create(queryString), base);
	}
	
	public QueryResultIterator(Query query, BaseRDF base) {
		this._base = base;
		init(query);
	}

	private void init(Query query) {
		_query = query;
		_errorWaitingTime = _defaultErrorWaitingTime;
		_nbTriesMax = _defaultNbTriesMax;
		_limit = _defaultLimit;
		_offset = 0;
		_resultCount = 0;
		_hasLimit = _query.hasLimit();
		if(!this._hasLimit) {
			_query.setLimit(_limit);
			_query.setOffset(_offset);
//			_query.addOrderBy(_query.getResultVars().get(0), Query.ORDER_DESCENDING);
		}
		_exec = getNewQueryExec(_query); // base.executionQuery(this._query);
		try 
		{
			_result = _exec.execSelect();
		} 
		catch(Exception e) 
		{
			logger.fatal("EXCEPTION at queryResultIterator contruction " + _query, e);
			if(e instanceof QueryExceptionHTTP) 
			{
				logger.fatal(((QueryExceptionHTTP) e).getResponseCode());
				logger.fatal(((QueryExceptionHTTP) e).getMessage());
//				throw e;
			} 
			while(this._result == null && this._nbTries < _nbTriesMax) {
				try 
				{
					this._nbTries++;
					Thread.sleep(_errorWaitingTime);
					logger.fatal("Query " + _query + " essai n°" + this._nbTries);
					_result = _exec.execSelect();
				} 
				catch (InterruptedException | QueryExceptionHTTP e1) 
				{
					logger.fatal("Query " + this._exec.getQuery() + " FAILED " + e);
				} 
			}
			this._nbTries = 0;
		}
//		logger.debug("CREATION FIN");
	}

	private QueryExecution getNewQueryExec(Query query) {
		if(this._base != null) {
			return this._base.executionQuery(query);
		} else if(this._data != null) {
			return QueryExecutionFactory.create(query, _data);
		}
		return null;
	}

	public static void setDefaultLimit(int defaultLimit) {
		_defaultLimit = defaultLimit;
	}

	public static int getDefaultLimit() {
		return _defaultLimit;
	}
	public static void setDefaultGraph(String defaultGraph) {
		_defaultDefaultGraph = defaultGraph;
	}

	public static String getDefaultGraph() {
		return _defaultDefaultGraph;
	}

	public void setLimit(int limit) {
		_limit = limit;
	}
	
	public int getOffset() {
		return this._offset;
	}

	public int getLimit() {
		return _limit;
	}

	public static void setDefaultErrorWaitingTime(int waitingTime) {
		_defaultErrorWaitingTime = waitingTime;
	}

	public static int getDefaultErrorWaitingTime() {
		return _defaultErrorWaitingTime;
	}

	public void setErrorWaitingTime(int waitingTime) {
		_errorWaitingTime = waitingTime;
	}

	public int getErrorWaitingTime() {
		return _errorWaitingTime;
	}

	public static void setDefaultNbTriesMax(int defaultNbTries) {
		_defaultNbTriesMax = defaultNbTries;
	}

	public static int getDefaultNbTriesMax() {
		return _defaultNbTriesMax;
	}

	public void setNbTriesMax(int nbTries) {
		_nbTriesMax = nbTries;
	}

	public int getNbTriesMax() {
		return _limit;
	}

	public int getCurrentNbTries() {
		return _nbTries;
	}

	public Query getQuery() {
		return this._exec.getQuery();
	}

	@Override
	public boolean hasNext() {
		try {
			return _result.hasNext();
		} catch (Exception e) {
			if(this._nbTries < this._nbTriesMax) {
				this._nbTries++;
				try {
					Thread.sleep(_errorWaitingTime);
				} catch (InterruptedException e1) {
					logger.error("Can't sleep", e1);
				}
				return hasNext();
				
			} else {
				throw e; 
			}
		}
	}

	public QueryExecution getExecution() {
		return this._exec;
	}
	
	@Override
	public CustomQuerySolution next() {
		return this.nextAnswerSet();
	}

	public RDFNode next(String var) {
		return getNextSolution().get(var);
	}

	public Resource nextResource(String var) {
		RDFNode nextNode = next(var);
		if(nextNode.isResource()) {
			return nextNode.asResource();
		} 
		return null;
	}

	public CustomQuerySolution next(Collection<String> varList) {
		QuerySolution currentSol = getNextSolution();
		CustomQuerySolution result = new CustomQuerySolution();
		Iterator<String> itVar = varList.iterator();
		while(itVar.hasNext()) {
			String var = itVar.next();
			if( currentSol != null && currentSol.contains(var)) {
				result.put(var, currentSol.get(var));
			} else {
				result.put(var, null);
			}
		}
		return result;
	}

	public CustomQuerySolution nextAnswerSet() {
		return next(this._result.getResultVars());
	}

	private QuerySolution getNextSolution() {
		QuerySolution result = null;
		try
		{
			if(this._result.hasNext()) {
				result = _result.next();
				_resultCount++;
				
				if(this._resultCount >= (this._limit + this._offset) && ! this._hasLimit) {
					this._offset += this._limit;
					_query.setOffset(_offset);
					this._exec.close();
					this._exec = getNewQueryExec(_query);
					this._result = _exec.execSelect();
				}
			} 
		}
		catch (Exception e) // De type NoRouteToHostException quand on arrive à cours de thread
		{
			logger.fatal("Triplets avec sujet sans classe Requete n° " + (this._offset/this._limit) + ", Trop de sockets ?" );
			logger.fatal(e);
			if(e instanceof QueryExceptionHTTP) 
			{
				logger.fatal(((QueryExceptionHTTP) e).getResponseCode());
				logger.fatal(((QueryExceptionHTTP) e).getMessage());
//				throw e;
			} 
			try 
			{
				if(this._nbTries < this._nbTriesMax) {
					Thread.sleep(_errorWaitingTime);
					this._offset -= this._limit;
					this._nbTries++;
					_resultCount--;
				}
			} 
			catch (InterruptedException e1) 
			{
				logger.error(e1);
			}
		}
		//		logger.debug("Row n°" + this.getRowNumber() + " QUERY:" + this.getQuery().toString().replace("\n", ""));
		return result;
	}

	public void close() {
		this._exec.close();
	}

	public int getRowNumber() {
		return this._resultCount;
	}


	public  List<String> getResultVars() {
		return this._exec.getQuery().getResultVars();
	}
	
	public static void main(String [] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");
		String query = "SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }";
		BaseRDF base = new BaseRDF("http://dbpedia.org/sparql", MODE.DISTANT);
		QueryResultIterator it = new QueryResultIterator(query, base);
		try {
		LinkedList<HashSet<RDFNode>> results = new LinkedList<HashSet<RDFNode>>();
		while(it.hasNext()) {
			CustomQuerySolution sol = it.next();
			
			HashSet<RDFNode> resultLine = new HashSet<RDFNode>();
			resultLine.add(sol.get("s"));
			resultLine.add(sol.get("p"));
			resultLine.add(sol.get("o"));
			if(results.contains(resultLine)) {
				logger.debug("HA !");
			}
			logger.debug("Solution: " + sol.get("s") + " " + sol.get("p") + " " + sol.get("o"));
			logger.debug("Offset: " + it.getOffset() + " limit: " + it.getLimit() + " resultCount: " + it.getRowNumber());
			logger.debug("Query: " + it.getQuery().toString().replaceAll("\n", " "));
			if(results.contains(resultLine)) {
				logger.debug("HA ! result number " + results.indexOf(resultLine) +" seen twice ");
				break;
			}
			results.add(resultLine);
		}
		} finally {
			it.close();
			base.close();
		}
	}

}
