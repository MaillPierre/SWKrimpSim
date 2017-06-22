package com.irisa.swpatterns;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
	
	private AttributeIndex index = new AttributeIndex();

	private boolean noTypeBool = false;
	private boolean noInBool = false;
	private boolean noOutBool = false;	
	
	private boolean rankOne = false;
	private boolean randZero = true;
	
	private int pathsLength = 0;
	
	private static Logger logger = Logger.getLogger(TransactionsExtractor.class);
	
	public static int getQueryLimit() {
		return queryLimit;
	}

	public static void setQueryLimit(int queryLimit) {
		TransactionsExtractor.queryLimit = queryLimit;
	}

	public AttributeIndex getIndex() {
		return index;
	}

	public void setIndex(AttributeIndex index) {
		this.index = index;
	}

	/**
	 * Rank zero translations: type and outgoing/ingoing properties.
	 * For each class, if limit is > 0, only take into account the limited amount of individual
	 * @param baseRDF
	 * @param onto
	 * @return
	 */
	public LabeledTransactions extractTransactions(BaseRDF baseRDF, UtilOntology onto) {

//		logger.debug(onto.classes().size() + " classes");

		// Accumulation de descriptions des propriétés d'individus
		LabeledTransactions results = new LabeledTransactions();
		Iterator<Resource> itClass = onto.usedClassIterator();
		while(itClass.hasNext()) 
		{
			Resource currentClass = itClass.next();
			results.addAll(extractTransactionsForClass(baseRDF, onto, currentClass));
		}

//		logger.debug("End of extraction");
		logger.debug(index.size() + " attributes");
//
		logger.debug(results.size() + " lines");
		return results;
	}
	
	private LabeledTransaction extractTypeAttributeForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {
		LabeledTransaction indivResult = new LabeledTransaction();
		String typeTripQueryString = "SELECT DISTINCT ?t WHERE { <" + currIndiv + "> a ?t }";
		QueryResultIterator itTypeResult = new QueryResultIterator(typeTripQueryString, baseRDF);
		try {
			while(itTypeResult.hasNext()) {
				CustomQuerySolution queryResultLine = itTypeResult.nextAnswerSet();
				Resource indiType = queryResultLine.getResource("t");
				RDFPatternResource attribute = new RDFPatternResource(indiType, RDFPatternResource.Type.Type );

				if(onto.isClass(indiType) && ! onto.isOntologyClassVocabulary(indiType)) {
					index.add(attribute);
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
		String outTripQueryString = "SELECT DISTINCT ?p WHERE { <" + currIndiv + "> ?p ?o }";
		QueryResultIterator itOutResult = new QueryResultIterator(outTripQueryString, baseRDF);
		try {
			while(itOutResult.hasNext()) {
				CustomQuerySolution queryResultLine = itOutResult.nextAnswerSet();
				Resource prop = queryResultLine.getResource("p");
				RDFPatternResource attribute = new RDFPatternResource(prop, RDFPatternResource.Type.Out );

				if(! onto.isOntologyPropertyVocabulary(prop)) {
					index.add(attribute);
					indivResult.add(attribute);
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
		if(! noTypeBool) {
			indivResult.addAll(this.extractTypeAttributeForIndividual(baseRDF, onto, currIndiv));
		}

		// QUERY out triples
		if(! noOutBool) {
			indivResult.addAll(this.extractOutPropertyAttributeForIndividual(baseRDF, onto, currIndiv));
		}

		// QUERY in triples
		if(! noInBool) {
			indivResult.addAll(this.extractInPropertyAttributeForIndividual(baseRDF, onto, currIndiv));
		}
		
		if(this.rankOne) {
			indivResult.addAll(this.extractPathFragmentAttributesForIndividual(baseRDF, onto, currIndiv));
		}
		
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
			
			results.add(extractTransactionsForIndividual(baseRDF, onto, currIndiv));
		}
		return results;
	}
	
	public LabeledTransactions extractPathAttributes(BaseRDF baseRDF, UtilOntology onto){
		return extractPathAttributes(baseRDF, onto, this.getPathsLength());
	}
	
	private LabeledTransactions extractPathAttributes(BaseRDF baseRDF, UtilOntology onto, int rank) {
		LabeledTransactions result = new LabeledTransactions();
		
		switch (rank) {
		case 1:
			result.addAll(extractPathAttributesRankOne(baseRDF, onto));
			break;
		case 2:
			result.addAll(extractPathAttributesRankTwo(baseRDF, onto));
			break;
		case 3:
			result.addAll(extractPathAttributesRankThree(baseRDF, onto));
			break;
		case 4:
			result.addAll(extractPathAttributesRankFour(baseRDF, onto));
			break;

		default:
			break;
		}
		
		return result;
	}

	private Collection<? extends LabeledTransaction> extractPathAttributesRankFour(BaseRDF baseRDF, UtilOntology onto) {
		LinkedList<LabeledTransaction> result = new LinkedList<LabeledTransaction>();
		
		String rankTwoQueryString = "SELECT ?i1 ?i1c ?p1 ?i2 ?i2c ?p2 ?i3 ?i3c ?p3 ?i4 ?i4c ?p4 ?i5 ?i5c WHERE { ";
		rankTwoQueryString += " ?i1 ?p1 ?i2 . ?i2 ?p2 ?i3 . ?i3 ?p3 ?i4 . ?i4 ?p4 ?i5 ";
		rankTwoQueryString += " OPTIONAL { ?i1 a ?i1c . } ";
		rankTwoQueryString += " OPTIONAL { ?i2 a ?i2c . } ";
		rankTwoQueryString += " OPTIONAL { ?i3 a ?i3c . } ";
		rankTwoQueryString += " OPTIONAL { ?i4 a ?i4c . } ";
		rankTwoQueryString += " OPTIONAL { ?i5 a ?i5c . } ";
		rankTwoQueryString += " }";
		
		QueryResultIterator itRank2 = new QueryResultIterator(rankTwoQueryString, baseRDF);
		while(itRank2.hasNext()) {
			CustomQuerySolution sol = itRank2.next();
			Resource i1 = sol.getResource("i1");
			Resource i2 = sol.getResource("i2");
			Resource i3 = sol.getResource("i3");
			Resource i4 = sol.getResource("i4");
			Resource i5 = sol.getResource("i5");
			Resource p1 = sol.getResource("p1");
			Resource p2 = sol.getResource("p2");
			Resource p3 = sol.getResource("p3");
			Resource p4 = sol.getResource("p4");
			Resource i1c = sol.getResource("i1c");
			Resource i2c = sol.getResource("i2c");
			Resource i3c = sol.getResource("i3c");
			Resource i4c = sol.getResource("i4c");
			Resource i5c = sol.getResource("i5c");
			
			// Regroupement des statement pour éliminer les cycles
			HashSet<Resource> setStat1 = new HashSet<Resource>();
			setStat1.add(i1);
			setStat1.add(p1);
			setStat1.add(i2);
			HashSet<Resource> setStat2 = new HashSet<Resource>();
			setStat2.add(i2);
			setStat2.add(p2);
			setStat2.add(i3);
			HashSet<Resource> setStat3 = new HashSet<Resource>();
			setStat3.add(i3);
			setStat3.add(p3);
			setStat3.add(i4);
			HashSet<Resource> setStat4 = new HashSet<Resource>();
			setStat4.add(i4);
			setStat4.add(p4);
			setStat4.add(i5);
			
			if(i1 != null && i2 != null && i3 != null && i4 != null && p1 != null && p2 != null && p3 != null
				&& ! onto.isClass(i1) && ! onto.isClass(i2) && ! onto.isClass(i3) && ! onto.isClass(i4)
				&& ! onto.isOntologyClassVocabulary(i1) && ! onto.isOntologyClassVocabulary(i2) && ! onto.isOntologyClassVocabulary(i3) && ! onto.isOntologyClassVocabulary(i4)
				&& ! onto.isOntologyPropertyVocabulary(p1) && ! onto.isOntologyPropertyVocabulary(p2) && ! onto.isOntologyPropertyVocabulary(p3)
				&& ! setStat1.equals(setStat2) && ! setStat1.equals(setStat3) && ! setStat1.equals(setStat4) && ! setStat2.equals(setStat3) && ! setStat2.equals(setStat4) && ! setStat3.equals(setStat4)
				) {
				LabeledTransaction line = new LabeledTransaction();
				
				RDFPatternResource i1Attr = new RDFPatternResource(i1, RDFPatternComponent.Type.Node1);
				RDFPatternResource i2Attr = new RDFPatternResource(i2, RDFPatternComponent.Type.Node2);
				RDFPatternResource i3Attr = new RDFPatternResource(i3, RDFPatternComponent.Type.Node3);
				RDFPatternResource i4Attr = new RDFPatternResource(i4, RDFPatternComponent.Type.Node4);
				RDFPatternResource i5Attr = new RDFPatternResource(i5, RDFPatternComponent.Type.Node5);
				RDFPatternResource p1Attr = new RDFPatternResource(p1, RDFPatternComponent.Type.Relation1);
				RDFPatternResource p2Attr = new RDFPatternResource(p2, RDFPatternComponent.Type.Relation2);
				RDFPatternResource p3Attr = new RDFPatternResource(p3, RDFPatternComponent.Type.Relation3);
				RDFPatternResource p4Attr = new RDFPatternResource(p4, RDFPatternComponent.Type.Relation4);

				index.add(i1Attr);
				line.add(i1Attr);

				index.add(i2Attr);
				line.add(i2Attr);
				

				index.add(i3Attr);
				line.add(i3Attr);
				

				index.add(i4Attr);
				line.add(i4Attr);
				

				index.add(i5Attr);
				line.add(i5Attr);
				

				index.add(p1Attr);
				line.add(p1Attr);
				

				index.add(p2Attr);
				line.add(p2Attr);
				

				index.add(p3Attr);
				line.add(p3Attr);
				

				index.add(p4Attr);
				line.add(p4Attr);
				
				if(i1c != null) { 
					RDFPatternResource i1cAttr = new RDFPatternResource(i1c, RDFPatternComponent.Type.Node1Type);

					index.add(i1cAttr);
					line.add(i1cAttr);
				}
				
				if(i2c != null) { 
					RDFPatternResource i2cAttr = new RDFPatternResource(i2c, RDFPatternComponent.Type.Node2Type);

					index.add(i2cAttr);
					line.add(i2cAttr);
				}
				
				if(i3c != null) {
					RDFPatternResource i3cAttr = new RDFPatternResource(i3c, RDFPatternComponent.Type.Node3Type);

					index.add(i3cAttr);
					line.add(i3cAttr);
				}
				
				if(i4c != null) {
					RDFPatternResource i4cAttr = new RDFPatternResource(i4c, RDFPatternComponent.Type.Node4Type);

					index.add(i4cAttr);
					line.add(i4cAttr);
				}
				
				if(i5c != null) {
					RDFPatternResource i5cAttr = new RDFPatternResource(i5c, RDFPatternComponent.Type.Node5Type);

					index.add(i5cAttr);
					line.add(i5cAttr);
				}
								
				result.add(line);
			}
		}
		
		return result;
	}

	private Collection<? extends LabeledTransaction> extractPathAttributesRankThree(BaseRDF baseRDF, UtilOntology onto) {
		LinkedList<LabeledTransaction> result = new LinkedList<LabeledTransaction>();
		
		String rankTwoQueryString = "SELECT ?i1 ?i1c ?p1 ?i2 ?i2c ?p2 ?i3 ?i3c ?p3 ?i4 ?i4c WHERE { ?i1 ?p1 ?i2 . ?i2 ?p2 ?i3 . ?i3 ?p3 ?i4 ";
		rankTwoQueryString += " OPTIONAL { ?i1 a ?i1c . } ";
		rankTwoQueryString += " OPTIONAL { ?i2 a ?i2c . } ";
		rankTwoQueryString += " OPTIONAL { ?i3 a ?i3c . } ";
		rankTwoQueryString += " OPTIONAL { ?i4 a ?i4c . } ";
		rankTwoQueryString += " }";
		
		QueryResultIterator itRank2 = new QueryResultIterator(rankTwoQueryString, baseRDF);
		while(itRank2.hasNext()) {
			CustomQuerySolution sol = itRank2.next();
			Resource i1 = sol.getResource("i1");
			Resource i2 = sol.getResource("i2");
			Resource i3 = sol.getResource("i3");
			Resource i4 = sol.getResource("i4");
			Resource p1 = sol.getResource("p1");
			Resource p2 = sol.getResource("p2");
			Resource p3 = sol.getResource("p3");
			Resource i1c = sol.getResource("i1c");
			Resource i2c = sol.getResource("i2c");
			Resource i3c = sol.getResource("i3c");
			Resource i4c = sol.getResource("i4c");
			
			// Regroupement des statement pour éliminer les cycles
			HashSet<Resource> setStat1 = new HashSet<Resource>();
			setStat1.add(i1);
			setStat1.add(p1);
			setStat1.add(i2);
			HashSet<Resource> setStat2 = new HashSet<Resource>();
			setStat2.add(i2);
			setStat2.add(p2);
			setStat2.add(i3);
			HashSet<Resource> setStat3 = new HashSet<Resource>();
			setStat3.add(i3);
			setStat3.add(p3);
			setStat3.add(i4);
			
			if(i1 != null && i2 != null && i3 != null && i4 != null && p1 != null && p2 != null && p3 != null // no null
				&& ! onto.isClass(i1) && ! onto.isClass(i2) && ! onto.isClass(i3) && ! onto.isClass(i4) // no class
				&& ! onto.isOntologyClassVocabulary(i1) && ! onto.isOntologyClassVocabulary(i2) && ! onto.isOntologyClassVocabulary(i3) && ! onto.isOntologyClassVocabulary(i4) // no RDF stuff
				&& ! onto.isOntologyPropertyVocabulary(p1) && ! onto.isOntologyPropertyVocabulary(p2) && ! onto.isOntologyPropertyVocabulary(p3) // no RDF stuff
				&& ! setStat1.equals(setStat2) && ! setStat1.equals(setStat3) && ! setStat2.equals(setStat3)
				) {
				LabeledTransaction line = new LabeledTransaction();
				
				RDFPatternResource i1Attr = new RDFPatternResource(i1, RDFPatternComponent.Type.Node1);
				RDFPatternResource i2Attr = new RDFPatternResource(i2, RDFPatternComponent.Type.Node2);
				RDFPatternResource i3Attr = new RDFPatternResource(i3, RDFPatternComponent.Type.Node3);
				RDFPatternResource i4Attr = new RDFPatternResource(i4, RDFPatternComponent.Type.Node4);
				RDFPatternResource p1Attr = new RDFPatternResource(p1, RDFPatternComponent.Type.Relation1);
				RDFPatternResource p2Attr = new RDFPatternResource(p2, RDFPatternComponent.Type.Relation2);
				RDFPatternResource p3Attr = new RDFPatternResource(p3, RDFPatternComponent.Type.Relation3);


				index.add(i1Attr);
				line.add(i1Attr);

				index.add(i2Attr);
				line.add(i2Attr);

				index.add(i3Attr);
				line.add(i3Attr);

				index.add(i4Attr);
				line.add(i4Attr);

				index.add(p1Attr);
				line.add(p1Attr);

				index.add(p2Attr);
				line.add(p2Attr);

				index.add(p3Attr);
				line.add(p3Attr);
				
				if(i1c != null) { 
					RDFPatternResource i1cAttr = new RDFPatternResource(i1c, RDFPatternComponent.Type.Node1Type);

					index.add(i1cAttr);
					line.add(i1cAttr);
				}
				
				if(i2c != null) { 
					RDFPatternResource i2cAttr = new RDFPatternResource(i2c, RDFPatternComponent.Type.Node2Type);

					index.add(i2cAttr);
					line.add(i2cAttr);
				}
				
				if(i3c != null) {
					RDFPatternResource i3cAttr = new RDFPatternResource(i3c, RDFPatternComponent.Type.Node3Type);

					index.add(i3cAttr);
					line.add(i3cAttr);
				}
				
				if(i4c != null) {
					RDFPatternResource i4cAttr = new RDFPatternResource(i4c, RDFPatternComponent.Type.Node3Type);

					index.add(i4cAttr);
					line.add(i4cAttr);
				}
								
				result.add(line);
			}
		}
		
		return result;
	}

	private Collection<? extends LabeledTransaction> extractPathAttributesRankTwo(BaseRDF baseRDF, UtilOntology onto) {
		LinkedList<LabeledTransaction> result = new LinkedList<LabeledTransaction>();
		
		String rankTwoQueryString = "SELECT ?i1 ?i1c ?p1 ?i2 ?i2c ?p2 ?i3 ?i3c WHERE { ?i1 ?p1 ?i2 . ?i2 ?p2 ?i3 .";
		rankTwoQueryString += " OPTIONAL { ?i1 a ?i1c . } ";
		rankTwoQueryString += " OPTIONAL { ?i2 a ?i2c . } ";
		rankTwoQueryString += " OPTIONAL { ?i3 a ?i3c . } ";
		rankTwoQueryString += " }";
		
		QueryResultIterator itRank2 = new QueryResultIterator(rankTwoQueryString, baseRDF);
		while(itRank2.hasNext()) {
			CustomQuerySolution sol = itRank2.next();
			Resource i1 = sol.getResource("i1");
			Resource i2 = sol.getResource("i2");
			Resource i3 = sol.getResource("i3");
			Resource p1 = sol.getResource("p1");
			Resource p2 = sol.getResource("p2");
			Resource i1c = sol.getResource("i1c");
			Resource i2c = sol.getResource("i2c");
			Resource i3c = sol.getResource("i3c");
			
			// Regroupement des statement pour éliminer les cycles
			HashSet<Resource> setStat1 = new HashSet<Resource>();
			setStat1.add(i1);
			setStat1.add(p1);
			setStat1.add(i2);
			HashSet<Resource> setStat2 = new HashSet<Resource>();
			setStat2.add(i2);
			setStat2.add(p2);
			setStat2.add(i3);
			
			if(i1 != null && i2 != null && i3 != null && p1 != null && p2 != null
				&& ! onto.isClass(i1) && ! onto.isClass(i2) && ! onto.isClass(i3)
				&& ! onto.isOntologyClassVocabulary(i1) && ! onto.isOntologyClassVocabulary(i2) && ! onto.isOntologyClassVocabulary(i3)
				&& ! onto.isOntologyPropertyVocabulary(p1) && ! onto.isOntologyPropertyVocabulary(p2)
				&& ! setStat1.equals(setStat2)
				) {
				LabeledTransaction line = new LabeledTransaction();
				
				RDFPatternResource i1Attr = new RDFPatternResource(i1, RDFPatternComponent.Type.Node1);
				RDFPatternResource i2Attr = new RDFPatternResource(i2, RDFPatternComponent.Type.Node2);
				RDFPatternResource i3Attr = new RDFPatternResource(i3, RDFPatternComponent.Type.Node3);
				RDFPatternResource p1Attr = new RDFPatternResource(p1, RDFPatternComponent.Type.Relation1);
				RDFPatternResource p2Attr = new RDFPatternResource(p2, RDFPatternComponent.Type.Relation2);

				index.add(i1Attr);
				line.add(i1Attr);

				index.add(i2Attr);
				line.add(i2Attr);

				index.add(i3Attr);
				line.add(i3Attr);

				index.add(p1Attr);
				line.add(p1Attr);

				index.add(p2Attr);
				line.add(p2Attr);
				
				if(i1c != null) { 
					RDFPatternResource i1cAttr = new RDFPatternResource(i1c, RDFPatternComponent.Type.Node1Type);

					index.add(i1cAttr);
					line.add(i1cAttr);
				}
				if(i2c != null) { 
					RDFPatternResource i2cAttr = new RDFPatternResource(i2c, RDFPatternComponent.Type.Node2Type);

					index.add(i2cAttr);
					line.add(i2cAttr);
				}
				if(i3c != null) {
					RDFPatternResource i3cAttr = new RDFPatternResource(i3c, RDFPatternComponent.Type.Node3Type);

					index.add(i3cAttr);
					line.add(i3cAttr);
				}
								
				result.add(line);
			}
		}
		
		return result;
	}

	private Collection<? extends LabeledTransaction> extractPathAttributesRankOne(BaseRDF baseRDF, UtilOntology onto) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public RankUpQuery sparqlizeItemSet(LabeledTransaction is) {
		HashMap<RDFPatternComponent, String> variables = new HashMap<RDFPatternComponent, String>();
		HashMap<String, RDFPatternComponent> patterns = new HashMap<String, RDFPatternComponent>();
		
		String patternCenterVar = "?pattern";
		
		String queryHead = "SELECT DISTINCT " + patternCenterVar;
		String queryBody = " WHERE { ";
		String queryEnd = " }";
		int varNum = 0;
		
		Iterator<RDFPatternComponent> itItems = is.iterator();
		while(itItems.hasNext()) {
			RDFPatternComponent item = itItems.next();
			if(item.getType() != Type.Type) {
				if(! variables.containsKey( item )) {
					String tmpVar = "?var" + varNum++;
					variables.put(item, tmpVar);
					patterns.put(tmpVar, item);
				}
			}
			String varName = variables.get(item);
			
			String patternString = "";
			if(item instanceof RDFPatternResource) {
				Resource patternRes = ((RDFPatternResource) item).getResource();
				if(item.getType() == Type.Type) {
					patternString = patternCenterVar + " <" + RDF.type + "> <" + patternRes.getURI() + "> . ";
				} else if(item.getType() == Type.Out) {
					patternString = patternCenterVar + " <" + patternRes.getURI() + "> " + varName +" . ";
				} else if(item.getType() == Type.In) {
					patternString = varName + " <" + patternRes.getURI() + "> " + patternCenterVar +" . ";
				}
			} else 
				if(item instanceof RDFPatternPathFragment) {
				Resource patternFirst = ((RDFPatternPathFragment) item).getPathFragment().getFirst();
				Resource patternSecond = ((RDFPatternPathFragment) item).getPathFragment().getSecond();
				if(item.getType() == Type.OutNeighbourType) {
					patternString = patternCenterVar + " <" + patternFirst.getURI() + "> " + varName +" . " + varName + " <" + RDF.type + "> <" + patternSecond.getURI() + "> . ";
				} else if(item.getType() == Type.InNeighbourType) {
					patternString = varName + " <" + patternSecond.getURI() + "> " + patternCenterVar + " . " + varName + " <" + RDF.type + "> <" + patternFirst.getURI() + "> . ";
				}
			}
			if(varName != null) {
				queryHead += " " + varName;
				queryBody += patternString;
			}
		}
		
		String queryString = queryHead + queryBody + queryEnd;
		return new RankUpQuery(queryString, variables, patterns);
	}

	
	private LabeledTransaction extractInPropertyAttributeForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {

		LabeledTransaction indivResult = new LabeledTransaction();
		
		String inTripQueryString = "SELECT DISTINCT ?p WHERE { ?s ?p <" + currIndiv + "> }";
		QueryResultIterator itInResult = new QueryResultIterator(inTripQueryString, baseRDF);
		try {
			while(itInResult.hasNext()) {
				CustomQuerySolution queryResultLine = itInResult.nextAnswerSet();
				Resource prop = queryResultLine.getResource("p");
				RDFPatternResource attribute = new RDFPatternResource(prop, RDFPatternResource.Type.In );

				if(! onto.isOntologyPropertyVocabulary(prop)) {
					index.add(attribute);
					indivResult.add(attribute);
				}
			}
		} catch(HttpException e) {

		} finally {
			itInResult.close();
		}
		
		return indivResult;
	}
	

	private LabeledTransaction extractPathFragmentAttributesForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {

		LabeledTransaction indivResult = new LabeledTransaction();
		
		if(! this.noOutTriples()) {
			String outTripQueryString = "SELECT DISTINCT ?p ?oc WHERE { <" + currIndiv + "> ?p ?o . ?o a ?oc }";
			QueryResultIterator itOutResult = new QueryResultIterator(outTripQueryString, baseRDF);
			try {
				while(itOutResult.hasNext()) {
					CustomQuerySolution queryResultLine = itOutResult.nextAnswerSet();
					Resource prop = queryResultLine.getResource("p");
					Resource objType = queryResultLine.getResource("oc");
					if(!onto.isOntologyPropertyVocabulary(prop)) {
						if(objType != null && prop != null) {
							RDFPatternPathFragment attribute = new RDFPatternPathFragment(prop, objType, RDFPatternResource.Type.OutNeighbourType );
							if(! onto.isOntologyClassVocabulary(objType) && onto.isClass(objType)) {
								index.add(attribute);
								indivResult.add(attribute);
							}
						}
					}
				}
			} catch(HttpException e) {
	
			} finally {
				itOutResult.close();
			}
		}
		
		if(! this.noInTriples()) {
			String inTripQueryString = "SELECT DISTINCT ?p ?oc WHERE { ?o ?p <" + currIndiv + "> . ?o a ?oc }";
			QueryResultIterator itInResult = new QueryResultIterator(inTripQueryString, baseRDF);
			try {
				while(itInResult.hasNext()) {
					CustomQuerySolution queryResultLine = itInResult.nextAnswerSet();
					Resource prop = queryResultLine.getResource("p");
					Resource objType = queryResultLine.getResource("oc");
//					logger.debug("Extracting InNeighbour attributes " + prop + " " +  );
					if(!onto.isOntologyPropertyVocabulary(prop)) {
						if(objType != null && prop != null) {
							RDFPatternPathFragment attribute = new RDFPatternPathFragment(objType, prop, RDFPatternResource.Type.InNeighbourType );
							if(! onto.isOntologyClassVocabulary(objType) && onto.isClass(objType)) {
								index.add(attribute);
								indivResult.add(attribute);
							}
						}
					}
				}
			} catch(HttpException e) {
	
			} finally {
				itInResult.close();
			}
		}
		
		return indivResult;
	}
	
	public boolean isRankOne() {
		return rankOne;
	}

	public void setRankOne(boolean rankOne) {
		this.rankOne = rankOne;
	}

	public boolean isRandZero() {
		return randZero;
	}

	public void setRandZero(boolean randZero) {
		this.randZero = randZero;
	}

	public boolean noTypeTriples() {
		return noTypeBool;
	}

	public void setNoTypeTriples(boolean noTypeBool) {
		this.noTypeBool = noTypeBool;
	}

	public boolean noInTriples() {
		return noInBool;
	}

	public void noInTriples(boolean noInBool) {
		this.noInBool = noInBool;
	}

	public boolean noOutTriples() {
		return noOutBool;
	}

	public void setNoOutTriples(boolean noOutBool) {
		this.noOutBool = noOutBool;
	}

	public int getPathsLength() {
		return pathsLength;
	}

	public void setPathsLength(int pathsLength) {
		this.pathsLength = pathsLength;
	}
	
}
