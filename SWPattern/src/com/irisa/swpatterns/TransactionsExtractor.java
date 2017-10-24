package com.irisa.swpatterns;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.CustomQuerySolution;
import com.irisa.jenautils.QueryResultIterator;
import com.irisa.jenautils.UtilOntology;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.RDFPatternComponent;
import com.irisa.swpatterns.data.RDFPatternPathFragment;
import com.irisa.swpatterns.data.RDFPatternResource;
import com.irisa.swpatterns.data.LabeledTransaction;
import com.irisa.swpatterns.data.RankUpQuery;
import com.irisa.swpatterns.data.LabeledTransactions;
import com.irisa.swpatterns.data.RDFPatternComponent.Type;

/**
 * Deal with the transaction extraction. Give access to the patternComponent index for later compressions (Good idea ?).
 * 
 * @author pmaillot
 *
 */
public class TransactionsExtractor {

	private static int queryLimit = 0;
	
	private AttributeIndex _index = AttributeIndex.getInstance();
	private HashMap<Resource, Integer> _inDegreeCount = new HashMap<Resource, Integer>();
	private HashMap<Resource, Integer> _outDegreeCount = new HashMap<Resource, Integer>();
	private HashSet<Resource> _outliers = new HashSet<Resource>();
	
	private HashSet<Resource> _individuals = new HashSet<Resource>();

	private boolean _noTypeBool = false;
	private boolean _noInBool = false;
	private boolean _noOutBool = false;	

	public enum Neighborhood {
		Property,
		PropertyAndType,
		PropertyAndOther
	}
	
	private Neighborhood _neighborLevel = Neighborhood.PropertyAndType;
	
	private int _pathsLength = 0;
	
	private static Logger logger = Logger.getLogger(TransactionsExtractor.class);
	
	public static int getQueryLimit() {
		return queryLimit;
	}

	public static void setQueryLimit(int queryLimit) {
		TransactionsExtractor.queryLimit = queryLimit;
	}
	
	public boolean isKnownIndividual(Resource indiv) {
		return _individuals.contains(indiv);
	}
	
	public void addKnownIndividual(Resource indiv) {
		_individuals.add(indiv);
	}

	public AttributeIndex getIndex() {
		return _index;
	}

	public void setIndex(AttributeIndex index) {
		this._index = index;
	}

	/**
	 * Neighbor properties transactions: type and outgoing/ingoing properties.
	 * For each class, if limit is > 0, only take into account the limited amount of individual
	 * @param baseRDF
	 * @param onto
	 * @return
	 */
	public LabeledTransactions extractTransactions(BaseRDF baseRDF, UtilOntology onto) {
		
		if(this.getNeighborLevel() == Neighborhood.PropertyAndOther) {
			initDegreeCount( baseRDF, onto);
		}
		
		logger.debug("Transaction extraction");

		// Aggregation of the individual descriptions
		LabeledTransactions results = new LabeledTransactions();
		Iterator<Resource> itClass = onto.usedClassIterator();
		while(itClass.hasNext()) 
		{
			Resource currentClass = itClass.next();
			results.addAll(extractTransactionsForClass(baseRDF, onto, currentClass));
		}

		logger.debug("End of transaction extraction");
		logger.debug(_index.size() + " attributes");
//
		logger.debug(results.size() + " lines");
		return results;
	}
	
	private void initDegreeCount(BaseRDF baseRDF, UtilOntology onto) {
		HashSet<Resource> instances = new HashSet<Resource>();
		Iterator<Resource> itClass = onto.usedClassIterator();
		while(itClass.hasNext()) {
			Resource currentClass = itClass.next();
			QueryResultIterator itInstances = new QueryResultIterator("SELECT DISTINCT ?i WHERE { ?i a <"+ currentClass.getURI() +"> }", baseRDF);
			itInstances.forEachRemaining(new Consumer<CustomQuerySolution>() {

				@Override
				public void accept(CustomQuerySolution sol) {
					instances.add(sol.getResource("i"));
				}
			});
		}

		DescriptiveStatistics summaryInDegree = new DescriptiveStatistics();
		instances.forEach(new Consumer<Resource>() {
			@Override
			public void accept(Resource res) {
				String inDegreeQuery = "SELECT DISTINCT (count(*) AS ?count) WHERE { ?subj ?prop <"+ res.getURI() +"> }";
				QueryResultIterator inDegreeiterator = new QueryResultIterator(inDegreeQuery, baseRDF);
				int degree = inDegreeiterator.next().get("count").asLiteral().getInt();
				_inDegreeCount.put(res, degree);
				summaryInDegree.addValue(degree);
			}
		});
		
		double q1In = summaryInDegree.getPercentile(25);
		double q3In = summaryInDegree.getPercentile(75);
		double outlierInDegreeThreshold = q3In + (1.5 * (q3In -q1In));
		logger.debug(summaryInDegree.getN() +" values q1: " + q1In + " q3: " + q3In + " In Outlier Threshold is: " + outlierInDegreeThreshold);
		
		instances.forEach(new Consumer<Resource>() {
			@Override
			public void accept(Resource res) {
				if((double)_inDegreeCount.get(res) > outlierInDegreeThreshold) {
					_outliers.add(res);
				}
			}
		});

		DescriptiveStatistics summaryOutDegree = new DescriptiveStatistics();
		instances.forEach(new Consumer<Resource>() {
			@Override
			public void accept(Resource res) {
				String outDegreeQuery = "SELECT DISTINCT (count(*) AS ?count) WHERE { <"+ res.getURI() +"> ?prop ?obj }";
				QueryResultIterator outDegreeiterator = new QueryResultIterator(outDegreeQuery, baseRDF);
				int degree = outDegreeiterator.next().get("count").asLiteral().getInt();
				_outDegreeCount.put(res, degree);
				summaryOutDegree.addValue(degree);
			}
		});
		
		double q1Out = summaryOutDegree.getPercentile(25);
		double q3Out = summaryOutDegree.getPercentile(75);
		double outlierOutDegreeThreshold = q3Out + (1.5 * (q3Out -q1Out));
		logger.debug(summaryOutDegree.getN() +" values q1: " + q1Out + " q3: " + q3Out + " Out Outlier Threshold is: " + outlierOutDegreeThreshold);
		
		instances.forEach(new Consumer<Resource>() {
			@Override
			public void accept(Resource res) {
				if((double)_outDegreeCount.get(res) > outlierOutDegreeThreshold) {
					_outliers.add(res);
				}
			}
		});
		logger.debug(instances.size() + " instances et " + _outliers.size() + " outliers.");
	}

	private LabeledTransaction extractTypeAttributeForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {
		LabeledTransaction indivResult = new LabeledTransaction();
		String typeTripQueryString = "SELECT DISTINCT ?t WHERE { <" + currIndiv + "> a ?t }";
		QueryResultIterator itTypeResult = new QueryResultIterator(typeTripQueryString, baseRDF);
		try {
			while(itTypeResult.hasNext()) {
				CustomQuerySolution queryResultLine = itTypeResult.nextAnswerSet();
				Resource indiType = queryResultLine.getResource("t");
				RDFPatternResource attribute = new RDFPatternResource(indiType, RDFPatternResource.Type.TYPE );

				if(onto.isClass(indiType) && ! onto.isOntologyClassVocabulary(indiType)) {
					_index.add(attribute);
					indivResult.add(attribute);
				}
			}
		} catch(HttpException e) {

		} finally {
			itTypeResult.close();
		}
		return indivResult;
	}
	
	private LabeledTransaction extractOutPropertyAttributeForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {

		LabeledTransaction indivResult = new LabeledTransaction();
		
		String outTripQueryString = "SELECT DISTINCT ?p "; 
		if(this.getNeighborLevel() == Neighborhood.PropertyAndType || this.getNeighborLevel() == Neighborhood.PropertyAndOther) {
			outTripQueryString += " ?ot " ;
		}
		if(this.getNeighborLevel() == Neighborhood.PropertyAndOther) {
			outTripQueryString += " ?o " ;
		}
		outTripQueryString += " WHERE { <" + currIndiv + "> ?p ?o . ";
		if(this.getNeighborLevel() == Neighborhood.PropertyAndType || this.getNeighborLevel() == Neighborhood.PropertyAndOther) {
			outTripQueryString += " OPTIONAL { ?o a ?ot . } ";
		}
		outTripQueryString += " }";
		QueryResultIterator itOutResult = new QueryResultIterator(outTripQueryString, baseRDF);
		try {
			while(itOutResult.hasNext()) {
				CustomQuerySolution queryResultLine = itOutResult.nextAnswerSet();
				Resource prop = queryResultLine.getResource("p");
				if(prop != null && ! onto.isOntologyPropertyVocabulary(prop)) {
					
					RDFPatternComponent propAttribute = new RDFPatternResource(prop, RDFPatternResource.Type.OUT_PROPERTY );
					if(! _index.contains(propAttribute)) {
						_index.add(propAttribute);
					}
					indivResult.add(propAttribute);
				
					if(this.getNeighborLevel()== Neighborhood.PropertyAndOther 
							&& queryResultLine.getResource("o") != null) {
						Resource obj = queryResultLine.getResource("o");
						if(! onto.isOntologyClassVocabulary(obj) && this._outliers.contains(obj)) {
							RDFPatternComponent attributeOther = new RDFPatternResource(prop, RDFPatternResource.Type.OUT_NEIGHBOUR );
							if(! _index.contains(attributeOther)) {
								_index.add(attributeOther);
							}
							indivResult.add(attributeOther);
						}
					} 
					if((this.getNeighborLevel() == Neighborhood.PropertyAndType 
							|| this.getNeighborLevel() == Neighborhood.PropertyAndOther)
							&& queryResultLine.getResource("ot") != null ) {
						Resource oType = queryResultLine.getResource("ot");
						if(! onto.isOntologyClassVocabulary(oType) 
								&& onto.isClass(oType)) {
							RDFPatternComponent attributeType = new RDFPatternPathFragment(prop, oType, RDFPatternResource.Type.OUT_NEIGHBOUR_TYPE );
							if(! _index.contains(attributeType)) {
								_index.add(attributeType);
							}
							indivResult.add(attributeType);
						}
					}
				}
			}
		} catch(HttpException e) {

		} finally {
			itOutResult.close();
		}
		
		return indivResult;
	}
	
	
	public LabeledTransaction extractTransactionsForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {
		LabeledTransaction indivResult = new LabeledTransaction();

		// QUERY types triples
		if(! _noTypeBool) {
			indivResult.addAll(this.extractTypeAttributeForIndividual(baseRDF, onto, currIndiv));
		}

		// QUERY out triples
		if(! _noOutBool) {
			indivResult.addAll(this.extractOutPropertyAttributeForIndividual(baseRDF, onto, currIndiv));
		}

		// QUERY in triples
		if(! _noInBool) {
			indivResult.addAll(this.extractInPropertyAttributesForIndividual(baseRDF, onto, currIndiv));
		}
		
//		if(this.getNeighborLevel() == Neighborhood.PropertyAndObjectType) {
//			indivResult.addAll(this.extractPathFragmentAttributesForIndividual(baseRDF, onto, currIndiv));
//		}
		
		logger.debug(currIndiv + " = " + indivResult);
		return indivResult;
	}
	
	public LabeledTransactions extractTransactionsForClass(BaseRDF baseRDF, UtilOntology onto, Resource currentClass) {

//		logger.debug("Current class: " + currentClass);

		LabeledTransactions results = new LabeledTransactions();

		HashSet<Resource> indivSet = new HashSet<Resource>();
		String indivQueryString = "SELECT DISTINCT ?i WHERE { ?i a <" + currentClass + "> . }";
		if(queryLimit > 0) {
			indivQueryString += " LIMIT " + queryLimit;
		}
		QueryResultIterator itIndivQuery = new QueryResultIterator(indivQueryString, baseRDF);
		try {
			while(itIndivQuery.hasNext()) {
				CustomQuerySolution indivSol = itIndivQuery.next();
				indivSet.add(indivSol.getResource("i"));
			}
		} catch(HttpException e) {

		} finally{
			itIndivQuery.close();
		}

		Iterator<Resource> itIndiv = indivSet.iterator();
		while(itIndiv.hasNext()) {
			Resource currIndiv = itIndiv.next();
			if(! currIndiv.isAnon() && ! isKnownIndividual(currIndiv)) {
				addKnownIndividual(currIndiv);
				results.add(extractTransactionsForIndividual(baseRDF, onto, currIndiv));
			}
		}
		return results;
	}

	private LabeledTransaction extractInPropertyAttributesForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {

		LabeledTransaction indivResult = new LabeledTransaction();
		
		String inTripQueryString = "SELECT DISTINCT ?p "; 
		if(this.getNeighborLevel() == Neighborhood.PropertyAndType 
				|| this.getNeighborLevel() == Neighborhood.PropertyAndOther) {
			inTripQueryString += " ?st " ;
		}
		if(this.getNeighborLevel() == Neighborhood.PropertyAndOther) {
			inTripQueryString += " ?s " ;
		}
		inTripQueryString += " WHERE { ?s ?p <" + currIndiv + "> . ";
		if(this.getNeighborLevel() == Neighborhood.PropertyAndType 
				|| this.getNeighborLevel() == Neighborhood.PropertyAndOther) {
			inTripQueryString += " OPTIONAL { ?s a ?st . } ";
		}
		inTripQueryString += " }";
		QueryResultIterator itInResult = new QueryResultIterator(inTripQueryString, baseRDF);
		try {
			while(itInResult.hasNext()) {
				CustomQuerySolution queryResultLine = itInResult.nextAnswerSet();
				Resource prop = queryResultLine.getResource("p");
				if(! onto.isOntologyPropertyVocabulary(prop)) {
					
					RDFPatternComponent propAttribute = new RDFPatternResource(prop, RDFPatternResource.Type.IN_PROPERTY );
					if(! _index.contains(propAttribute)) {
						_index.add(propAttribute);
					}
					indivResult.add(propAttribute);
					
					if(this.getNeighborLevel()== Neighborhood.PropertyAndOther) {
						RDFPatternComponent attributeOther = null;
						Resource subj = queryResultLine.getResource("s");
						if(! onto.isOntologyClassVocabulary(subj) && _outliers.contains(subj)) {
							attributeOther = new RDFPatternResource(prop, RDFPatternResource.Type.IN_NEIGHBOUR );
							if(! _index.contains(attributeOther)) {
								_index.add(attributeOther);
							}
							indivResult.add(attributeOther);
						}
					} 
					if (this.getNeighborLevel() == Neighborhood.PropertyAndType || this.getNeighborLevel() == Neighborhood.PropertyAndOther) {
						RDFPatternComponent attributeType = null;
						Resource sType = queryResultLine.getResource("st");
						if(sType != null && ! onto.isOntologyClassVocabulary(sType) && onto.isClass(sType)) {
							attributeType = new RDFPatternPathFragment(prop, sType, RDFPatternResource.Type.IN_NEIGHBOUR_TYPE );
							if(! _index.contains(attributeType)) {
								_index.add(attributeType);
							}
							indivResult.add(attributeType);
						}
					}
				}
			}
		} catch(HttpException e) {

		} finally {
			itInResult.close();
		}
		
		return indivResult;
	}
	

//	private LabeledTransaction extractPathFragmentAttributesForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {
//
//		LabeledTransaction indivResult = new LabeledTransaction();
//		
//		if(! this.noOutTriples()) {
//			String outTripQueryString = "SELECT DISTINCT ?p ?oc WHERE { <" + currIndiv + "> ?p ?o . ?o a ?oc }";
//			QueryResultIterator itOutResult = new QueryResultIterator(outTripQueryString, baseRDF);
//			try {
//				while(itOutResult.hasNext()) {
//					CustomQuerySolution queryResultLine = itOutResult.nextAnswerSet();
//					Resource prop = queryResultLine.getResource("p");
//					Resource objType = queryResultLine.getResource("oc");
//					if(!onto.isOntologyPropertyVocabulary(prop)) {
//						if(objType != null && prop != null) {
//							RDFPatternPathFragment attribute = new RDFPatternPathFragment(prop, objType, RDFPatternResource.Type.OUT_NEIGHBOUR_TYPE );
//							if(! onto.isOntologyClassVocabulary(objType) && onto.isClass(objType)) {
//								_index.add(attribute);
//								indivResult.add(attribute);
//							}
//						}
//					}
//				}
//			} catch(HttpException e) {
//	
//			} finally {
//				itOutResult.close();
//			}
//		}
//		
//		if(! this.noInTriples()) {
//			String inTripQueryString = "SELECT DISTINCT ?p ?oc WHERE { ?o ?p <" + currIndiv + "> . ?o a ?oc }";
//			QueryResultIterator itInResult = new QueryResultIterator(inTripQueryString, baseRDF);
//			try {
//				while(itInResult.hasNext()) {
//					CustomQuerySolution queryResultLine = itInResult.nextAnswerSet();
//					Resource prop = queryResultLine.getResource("p");
//					Resource objType = queryResultLine.getResource("oc");
////					logger.debug("Extracting InNeighbour attributes " + prop + " " +  );
//					if(!onto.isOntologyPropertyVocabulary(prop)) {
//						if(objType != null && prop != null) {
//							RDFPatternPathFragment attribute = new RDFPatternPathFragment(objType, prop, RDFPatternResource.Type.IN_NEIGHBOUR_TYPE );
//							if(! onto.isOntologyClassVocabulary(objType) && onto.isClass(objType)) {
//								_index.add(attribute);
//								indivResult.add(attribute);
//							}
//						}
//					}
//				}
//			} catch(HttpException e) {
//	
//			} finally {
//				itInResult.close();
//			}
//		}
//		
//		return indivResult;
//	}
//	
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

	public int getPathsLength() {
		return _pathsLength;
	}

	public void setPathsLength(int pathsLength) {
		this._pathsLength = pathsLength;
	}
	
}
