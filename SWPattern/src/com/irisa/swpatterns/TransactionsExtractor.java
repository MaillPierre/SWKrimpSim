package com.irisa.swpatterns;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.CustomQuerySolution;
import com.irisa.jenautils.QueryResultIterator;
import com.irisa.jenautils.UtilOntology;
import com.irisa.swpatterns.data.LabeledItemSet;
import com.irisa.swpatterns.data.RDFPatternComponent;
import com.irisa.swpatterns.data.RDFPatternPathFragment;
import com.irisa.swpatterns.data.RDFPatternResource;
import com.irisa.swpatterns.data.RankNAttributeSet;
import com.irisa.swpatterns.data.RankUpQuery;
import com.irisa.swpatterns.data.RDFPatternComponent.Type;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class TransactionsExtractor {

	private static int queryLimit = 0;

	private static RankNAttributeSet attributes = new RankNAttributeSet();
	private static HashMap<RDFPatternComponent, Integer> attributeItemIndex = new HashMap<RDFPatternComponent, Integer>();
	private static HashMap<Integer, RDFPatternComponent> itemAttributeIndex = new HashMap<Integer, RDFPatternComponent>();

	private boolean noTypeBool = false;
	private boolean noInBool = false;
	private boolean noOutBool = false;	
	
	private boolean rankOne = false;
	private boolean randZero = true;
	
	private int pathsLength = 0;
	
	private static Logger logger = Logger.getLogger(TransactionsExtractor.class);
	
	private static int counterAttribute = 0;
	
	public static int getAttributeNumber() {
		return counterAttribute++;
	}
	
	public static int getQueryLimit() {
		return queryLimit;
	}

	public static void setQueryLimit(int queryLimit) {
		TransactionsExtractor.queryLimit = queryLimit;
	}

	/**
	 * Print the transaction in the format expected by SPMF (int separated by spaces). Will update the attribute/item indexes
	 * @param attributes Set of all attributes appearing in the descriptions
	 * @param transactions
	 * @param output
	 * @return
	 * @throws Exception 
	 */
	public void printTransactionsItems(LinkedList<RankNAttributeSet> transactions, String output) throws Exception {

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output)));
			CSVPrinter printer = new CSVPrinter(out, CSVFormat.TDF.withDelimiter(' '));
			CSVPrinter attributePrinter = new CSVPrinter(new PrintWriter(new BufferedWriter(new FileWriter(output+".attr"))), CSVFormat.TDF);

			// Writing lines
			Iterator<RankNAttributeSet> itResult = transactions.iterator();
			while(itResult.hasNext()) {
				RankNAttributeSet resultLine = itResult.next();
				// Ecriture des attributs types
				Iterator<RDFPatternComponent> itTypes = resultLine.getSortedIterator();
				while(itTypes.hasNext()) {
					RDFPatternComponent res = itTypes.next();
					if( null == attributeItemIndex.get(res)) {
						logger.fatal("RAAAAAAAAAAAAAAAAAAAH " + res);
					}
					int itemIndex = attributeItemIndex.get(res);
					printer.print(itemIndex);
				}
				
				printer.println();
			}

			printer.close();

			// Writing attributes
			Iterator<RDFPatternComponent> itAttr = attributeItemIndex.keySet().iterator();
			while(itAttr.hasNext()) {
				RDFPatternComponent attr = itAttr.next();
				attributePrinter.print(attr);
				attributePrinter.print(attributeItemIndex.get(attr));
				attributePrinter.println();
			}
			attributePrinter.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * Rank zero translations: type and outgoing/ingoing properties.
	 * For each class, if limit is > 0, only take into account the limited amount of individual
	 * @param baseRDF
	 * @param onto
	 * @return
	 */
	public LinkedList<RankNAttributeSet> extractTransactions(BaseRDF baseRDF, UtilOntology onto) {

		logger.debug(onto.classes().size() + " classes");

		// Accumulation de descriptions des propriétés d'individus
		LinkedList<RankNAttributeSet> results = new LinkedList<RankNAttributeSet>();
		Iterator<Resource> itClass = onto.usedClassIterator();
		while(itClass.hasNext()) 
		{
			Resource currentClass = itClass.next();
			results.addAll(extractTransactionsForClass(baseRDF, onto, currentClass));
		}

		logger.debug("End of extraction");
		logger.debug(attributes.size() + " attributes");

		logger.debug(results.size() + " lines");
		return results;
	}
	
	private RankNAttributeSet extractTypeAttributeForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {
		RankNAttributeSet indivResult = new RankNAttributeSet();
		String typeTripQueryString = "SELECT DISTINCT ?t WHERE { <" + currIndiv + "> a ?t }";
		QueryResultIterator itTypeResult = new QueryResultIterator(typeTripQueryString, baseRDF);
		try {
			while(itTypeResult.hasNext()) {
				CustomQuerySolution queryResultLine = itTypeResult.nextAnswerSet();
				Resource indiType = queryResultLine.getResource("t");
				RDFPatternResource attribute = new RDFPatternResource(indiType, RDFPatternResource.Type.Type );

				if(onto.isClass(indiType) && ! onto.isOntologyClassVocabulary(indiType)) {
					if(! attributes.contains(attribute)) {
						attributes.add(attribute);
						if(! attributeItemIndex.containsKey(attribute)) {
							attributeItemIndex.put(attribute, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(attribute), attribute );
						}
					}
					indivResult.add(attribute);
				}
			}
		} catch(HttpException e) {

		} finally {
			itTypeResult.close();
		}
		return indivResult;
	}
	
	private RankNAttributeSet extractOutPropertyAttributeForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {
		RankNAttributeSet indivResult = new RankNAttributeSet();
		String outTripQueryString = "SELECT DISTINCT ?p WHERE { <" + currIndiv + "> ?p ?o }";
		QueryResultIterator itOutResult = new QueryResultIterator(outTripQueryString, baseRDF);
		try {
			while(itOutResult.hasNext()) {
				CustomQuerySolution queryResultLine = itOutResult.nextAnswerSet();
				Resource prop = queryResultLine.getResource("p");
				RDFPatternResource attribute = new RDFPatternResource(prop, RDFPatternResource.Type.Out );

				if(! onto.isOntologyPropertyVocabulary(prop)) {
					if(! attributes.contains(prop)) {
						attributes.add(attribute);
						if(! attributeItemIndex.containsKey(attribute)) {
							attributeItemIndex.put(attribute, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(attribute), attribute );
						}
					}
					indivResult.add(attribute);
				}
			}
		} catch(HttpException e) {

		} finally {
			itOutResult.close();
		}
		return indivResult;
	}
	public RankNAttributeSet extractTransactionsForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {
		RankNAttributeSet indivResult = new RankNAttributeSet();

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
	
	public LinkedList<RankNAttributeSet> extractTransactionsForClass(BaseRDF baseRDF, UtilOntology onto, Resource currentClass) {

		logger.debug("Current class: " + currentClass);

		LinkedList<RankNAttributeSet> results = new LinkedList<RankNAttributeSet>();

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
	
	public LinkedList<RankNAttributeSet> extractPathAttributes(BaseRDF baseRDF, UtilOntology onto){
		return extractPathAttributes(baseRDF, onto, this.getPathsLength());
	}
	
	private LinkedList<RankNAttributeSet> extractPathAttributes(BaseRDF baseRDF, UtilOntology onto, int rank) {
		LinkedList<RankNAttributeSet> result = new LinkedList<RankNAttributeSet>();
		
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

	private Collection<? extends RankNAttributeSet> extractPathAttributesRankFour(BaseRDF baseRDF, UtilOntology onto) {
		LinkedList<RankNAttributeSet> result = new LinkedList<RankNAttributeSet>();
		
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
				RankNAttributeSet line = new RankNAttributeSet();
				
				RDFPatternResource i1Attr = new RDFPatternResource(i1, RDFPatternComponent.Type.Node1);
				RDFPatternResource i2Attr = new RDFPatternResource(i2, RDFPatternComponent.Type.Node2);
				RDFPatternResource i3Attr = new RDFPatternResource(i3, RDFPatternComponent.Type.Node3);
				RDFPatternResource i4Attr = new RDFPatternResource(i4, RDFPatternComponent.Type.Node4);
				RDFPatternResource i5Attr = new RDFPatternResource(i5, RDFPatternComponent.Type.Node5);
				RDFPatternResource p1Attr = new RDFPatternResource(p1, RDFPatternComponent.Type.Relation1);
				RDFPatternResource p2Attr = new RDFPatternResource(p2, RDFPatternComponent.Type.Relation2);
				RDFPatternResource p3Attr = new RDFPatternResource(p3, RDFPatternComponent.Type.Relation3);
				RDFPatternResource p4Attr = new RDFPatternResource(p4, RDFPatternComponent.Type.Relation4);

				if(! attributes.contains(i1Attr)) {
					attributes.add(i1Attr);
					if(! attributeItemIndex.containsKey(i1Attr)) {
						attributeItemIndex.put(i1Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i1Attr), i1Attr );
					}
				}
				line.add(i1Attr);
				
				if(! attributes.contains(i2Attr)) {
					attributes.add(i2Attr);
					if(! attributeItemIndex.containsKey(i2Attr)) {
						attributeItemIndex.put(i2Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i2Attr), i2Attr );
					}
				}
				line.add(i2Attr);
				
				if(! attributes.contains(i3Attr)) {
					attributes.add(i3Attr);
					if(! attributeItemIndex.containsKey(i3Attr)) {
						attributeItemIndex.put(i3Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i3Attr), i3Attr );
					}
				}
				line.add(i3Attr);
				
				if(! attributes.contains(i4Attr)) {
					attributes.add(i4Attr);
					if(! attributeItemIndex.containsKey(i4Attr)) {
						attributeItemIndex.put(i4Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i4Attr), i4Attr );
					}
				}
				line.add(i4Attr);
				
				if(! attributes.contains(i5Attr)) {
					attributes.add(i5Attr);
					if(! attributeItemIndex.containsKey(i5Attr)) {
						attributeItemIndex.put(i5Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i5Attr), i5Attr );
					}
				}
				line.add(i5Attr);
				
				if(! attributes.contains(p1Attr)) {
					attributes.add(p1Attr);
					if(! attributeItemIndex.containsKey(p1Attr)) {
						attributeItemIndex.put(p1Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p1Attr), p1Attr );
					}
				}
				line.add(p1Attr);
				
				if(! attributes.contains(p2Attr)) {
					attributes.add(p2Attr);
					if(! attributeItemIndex.containsKey(p2Attr)) {
						attributeItemIndex.put(p2Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p2Attr), p2Attr );
					}
				}
				line.add(p2Attr);
				
				if(! attributes.contains(p3Attr)) {
					attributes.add(p3Attr);
					if(! attributeItemIndex.containsKey(p3Attr)) {
						attributeItemIndex.put(p3Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p3Attr), p3Attr );
					}
				}
				line.add(p3Attr);
				
				if(! attributes.contains(p4Attr)) {
					attributes.add(p4Attr);
					if(! attributeItemIndex.containsKey(p4Attr)) {
						attributeItemIndex.put(p4Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p4Attr), p4Attr );
					}
				}
				line.add(p4Attr);
				
				if(i1c != null) { 
					RDFPatternResource i1cAttr = new RDFPatternResource(i1c, RDFPatternComponent.Type.Node1Type);
					if(! attributes.contains(i1cAttr)) {
						attributes.add(i1cAttr);
						if(! attributeItemIndex.containsKey(i1cAttr)) {
							attributeItemIndex.put(i1cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i1cAttr), i1cAttr );
						}
					}
					line.add(i1cAttr);
				}
				
				if(i2c != null) { 
					RDFPatternResource i2cAttr = new RDFPatternResource(i2c, RDFPatternComponent.Type.Node2Type);
					if(! attributes.contains(i2cAttr)) {
						attributes.add(i2cAttr);
						if(! attributeItemIndex.containsKey(i2cAttr)) {
							attributeItemIndex.put(i2cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i2cAttr), i2cAttr );
						}
					}
					line.add(i2cAttr);
				}
				
				if(i3c != null) {
					RDFPatternResource i3cAttr = new RDFPatternResource(i3c, RDFPatternComponent.Type.Node3Type);
					if(! attributes.contains(i3cAttr)) {
						attributes.add(i3cAttr);
						if(! attributeItemIndex.containsKey(i3cAttr)) {
							attributeItemIndex.put(i3cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i3cAttr), i3cAttr );
						}
					}
					line.add(i3cAttr);
				}
				
				if(i4c != null) {
					RDFPatternResource i4cAttr = new RDFPatternResource(i4c, RDFPatternComponent.Type.Node4Type);
					if(! attributes.contains(i4cAttr)) {
						attributes.add(i4cAttr);
						if(! attributeItemIndex.containsKey(i4cAttr)) {
							attributeItemIndex.put(i4cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i4cAttr), i4cAttr );
						}
					}
					line.add(i4cAttr);
				}
				
				if(i5c != null) {
					RDFPatternResource i5cAttr = new RDFPatternResource(i5c, RDFPatternComponent.Type.Node5Type);
					if(! attributes.contains(i5cAttr)) {
						attributes.add(i5cAttr);
						if(! attributeItemIndex.containsKey(i5cAttr)) {
							attributeItemIndex.put(i5cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i5cAttr), i5cAttr );
						}
					}
					line.add(i5cAttr);
				}
								
				result.add(line);
			}
		}
		
		return result;
	}

	private Collection<? extends RankNAttributeSet> extractPathAttributesRankThree(BaseRDF baseRDF, UtilOntology onto) {
		LinkedList<RankNAttributeSet> result = new LinkedList<RankNAttributeSet>();
		
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
				RankNAttributeSet line = new RankNAttributeSet();
				
				RDFPatternResource i1Attr = new RDFPatternResource(i1, RDFPatternComponent.Type.Node1);
				RDFPatternResource i2Attr = new RDFPatternResource(i2, RDFPatternComponent.Type.Node2);
				RDFPatternResource i3Attr = new RDFPatternResource(i3, RDFPatternComponent.Type.Node3);
				RDFPatternResource i4Attr = new RDFPatternResource(i4, RDFPatternComponent.Type.Node4);
				RDFPatternResource p1Attr = new RDFPatternResource(p1, RDFPatternComponent.Type.Relation1);
				RDFPatternResource p2Attr = new RDFPatternResource(p2, RDFPatternComponent.Type.Relation2);
				RDFPatternResource p3Attr = new RDFPatternResource(p3, RDFPatternComponent.Type.Relation3);

				if(! attributes.contains(i1Attr)) {
					attributes.add(i1Attr);
					if(! attributeItemIndex.containsKey(i1Attr)) {
						attributeItemIndex.put(i1Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i1Attr), i1Attr );
					}
				}
				line.add(i1Attr);
				
				if(! attributes.contains(i2Attr)) {
					attributes.add(i2Attr);
					if(! attributeItemIndex.containsKey(i2Attr)) {
						attributeItemIndex.put(i2Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i2Attr), i2Attr );
					}
				}
				line.add(i2Attr);
				
				if(! attributes.contains(i3Attr)) {
					attributes.add(i3Attr);
					if(! attributeItemIndex.containsKey(i3Attr)) {
						attributeItemIndex.put(i3Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i3Attr), i3Attr );
					}
				}
				line.add(i3Attr);
				
				if(! attributes.contains(i4Attr)) {
					attributes.add(i4Attr);
					if(! attributeItemIndex.containsKey(i4Attr)) {
						attributeItemIndex.put(i4Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i4Attr), i4Attr );
					}
				}
				line.add(i4Attr);
				
				if(! attributes.contains(p1Attr)) {
					attributes.add(p1Attr);
					if(! attributeItemIndex.containsKey(p1Attr)) {
						attributeItemIndex.put(p1Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p1Attr), p1Attr );
					}
				}
				line.add(p1Attr);
				
				if(! attributes.contains(p2Attr)) {
					attributes.add(p2Attr);
					if(! attributeItemIndex.containsKey(p2Attr)) {
						attributeItemIndex.put(p2Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p2Attr), p2Attr );
					}
				}
				line.add(p2Attr);
				
				if(! attributes.contains(p3Attr)) {
					attributes.add(p3Attr);
					if(! attributeItemIndex.containsKey(p3Attr)) {
						attributeItemIndex.put(p3Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p3Attr), p3Attr );
					}
				}
				line.add(p3Attr);
				
				if(i1c != null) { 
					RDFPatternResource i1cAttr = new RDFPatternResource(i1c, RDFPatternComponent.Type.Node1Type);
					if(! attributes.contains(i1cAttr)) {
						attributes.add(i1cAttr);
						if(! attributeItemIndex.containsKey(i1cAttr)) {
							attributeItemIndex.put(i1cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i1cAttr), i1cAttr );
						}
					}
					line.add(i1cAttr);
				}
				
				if(i2c != null) { 
					RDFPatternResource i2cAttr = new RDFPatternResource(i2c, RDFPatternComponent.Type.Node2Type);
					if(! attributes.contains(i2cAttr)) {
						attributes.add(i2cAttr);
						if(! attributeItemIndex.containsKey(i2cAttr)) {
							attributeItemIndex.put(i2cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i2cAttr), i2cAttr );
						}
					}
					line.add(i2cAttr);
				}
				
				if(i3c != null) {
					RDFPatternResource i3cAttr = new RDFPatternResource(i3c, RDFPatternComponent.Type.Node3Type);
					if(! attributes.contains(i3cAttr)) {
						attributes.add(i3cAttr);
						if(! attributeItemIndex.containsKey(i3cAttr)) {
							attributeItemIndex.put(i3cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i3cAttr), i3cAttr );
						}
					}
					line.add(i3cAttr);
				}
				
				if(i4c != null) {
					RDFPatternResource i4cAttr = new RDFPatternResource(i4c, RDFPatternComponent.Type.Node3Type);
					if(! attributes.contains(i4cAttr)) {
						attributes.add(i4cAttr);
						if(! attributeItemIndex.containsKey(i4cAttr)) {
							attributeItemIndex.put(i4cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i4cAttr), i4cAttr );
						}
					}
					line.add(i4cAttr);
				}
								
				result.add(line);
			}
		}
		
		return result;
	}

	private Collection<? extends RankNAttributeSet> extractPathAttributesRankTwo(BaseRDF baseRDF, UtilOntology onto) {
		LinkedList<RankNAttributeSet> result = new LinkedList<RankNAttributeSet>();
		
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
				RankNAttributeSet line = new RankNAttributeSet();
				
				RDFPatternResource i1Attr = new RDFPatternResource(i1, RDFPatternComponent.Type.Node1);
				RDFPatternResource i2Attr = new RDFPatternResource(i2, RDFPatternComponent.Type.Node2);
				RDFPatternResource i3Attr = new RDFPatternResource(i3, RDFPatternComponent.Type.Node3);
				RDFPatternResource p1Attr = new RDFPatternResource(p1, RDFPatternComponent.Type.Relation1);
				RDFPatternResource p2Attr = new RDFPatternResource(p2, RDFPatternComponent.Type.Relation2);

				if(! attributes.contains(i1Attr)) {
					attributes.add(i1Attr);
					if(! attributeItemIndex.containsKey(i1Attr)) {
						attributeItemIndex.put(i1Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i1Attr), i1Attr );
					}
				}
				line.add(i1Attr);
				if(! attributes.contains(i2Attr)) {
					attributes.add(i2Attr);
					if(! attributeItemIndex.containsKey(i2Attr)) {
						attributeItemIndex.put(i2Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i2Attr), i2Attr );
					}
				}
				line.add(i2Attr);
				if(! attributes.contains(i3Attr)) {
					attributes.add(i3Attr);
					if(! attributeItemIndex.containsKey(i3Attr)) {
						attributeItemIndex.put(i3Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(i3Attr), i3Attr );
					}
				}
				line.add(i3Attr);
				if(! attributes.contains(p1Attr)) {
					attributes.add(p1Attr);
					if(! attributeItemIndex.containsKey(p1Attr)) {
						attributeItemIndex.put(p1Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p1Attr), p1Attr );
					}
				}
				line.add(p1Attr);
				if(! attributes.contains(p2Attr)) {
					attributes.add(p2Attr);
					if(! attributeItemIndex.containsKey(p2Attr)) {
						attributeItemIndex.put(p2Attr, getAttributeNumber());
						itemAttributeIndex.put(attributeItemIndex.get(p2Attr), p2Attr );
					}
				}
				line.add(p2Attr);
				
				if(i1c != null) { 
					RDFPatternResource i1cAttr = new RDFPatternResource(i1c, RDFPatternComponent.Type.Node1Type);
					if(! attributes.contains(i1cAttr)) {
						attributes.add(i1cAttr);
						if(! attributeItemIndex.containsKey(i1cAttr)) {
							attributeItemIndex.put(i1cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i1cAttr), i1cAttr );
						}
					}
					line.add(i1cAttr);
				}
				if(i2c != null) { 
					RDFPatternResource i2cAttr = new RDFPatternResource(i2c, RDFPatternComponent.Type.Node2Type);
					if(! attributes.contains(i2cAttr)) {
						attributes.add(i2cAttr);
						if(! attributeItemIndex.containsKey(i2cAttr)) {
							attributeItemIndex.put(i2cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i2cAttr), i2cAttr );
						}
					}
					line.add(i2cAttr);
				}
				if(i3c != null) {
					RDFPatternResource i3cAttr = new RDFPatternResource(i3c, RDFPatternComponent.Type.Node3Type);
					if(! attributes.contains(i3cAttr)) {
						attributes.add(i3cAttr);
						if(! attributeItemIndex.containsKey(i3cAttr)) {
							attributeItemIndex.put(i3cAttr, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(i3cAttr), i3cAttr );
						}
					}
					line.add(i3cAttr);
				}
								
				result.add(line);
			}
		}
		
		return result;
	}

	private Collection<? extends RankNAttributeSet> extractPathAttributesRankOne(BaseRDF baseRDF, UtilOntology onto) {
		// TODO Auto-generated method stub
		return null;
	}

	public static LabeledItemSet labelItemSet(Itemset iSet) {
		LabeledItemSet result = new LabeledItemSet();

		for(int i = 0; i < iSet.getItems().length; i++) {
			result.addItem(itemAttributeIndex.get(iSet.get(i)));
		}
		result.setCount(iSet.getAbsoluteSupport());

		return result;
	}
	
	public static List<LabeledItemSet> labelItemSet(Itemsets iSets) {
		List<LabeledItemSet> result = new ArrayList<LabeledItemSet>();
		
		for(int level = 0; level < iSets.getLevels().size(); level++) {
			iSets.getLevels().forEach(new Consumer<List<Itemset>>() {
				@Override
				public void accept(List<Itemset> l) {
					l.forEach(new Consumer<Itemset>(){
						@Override
						public void accept(Itemset is) {
							result.add(labelItemSet(is));
						}
					});
				}
			});
		}
		
		return result;
	}
	
	public RankUpQuery sparqlizeItemSet(LabeledItemSet is) {
		HashMap<RDFPatternComponent, String> variables = new HashMap<RDFPatternComponent, String>();
		HashMap<String, RDFPatternComponent> patterns = new HashMap<String, RDFPatternComponent>();
		
		String patternCenterVar = "?pattern";
		
		String queryHead = "SELECT DISTINCT " + patternCenterVar;
		String queryBody = " WHERE { ";
		String queryEnd = " }";
		int varNum = 0;
		
		Iterator<RDFPatternComponent> itItems = is.getItems().iterator();
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

	
	private RankNAttributeSet extractInPropertyAttributeForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {

		RankNAttributeSet indivResult = new RankNAttributeSet();
		
		String inTripQueryString = "SELECT DISTINCT ?p WHERE { ?s ?p <" + currIndiv + "> }";
		QueryResultIterator itInResult = new QueryResultIterator(inTripQueryString, baseRDF);
		try {
			while(itInResult.hasNext()) {
				CustomQuerySolution queryResultLine = itInResult.nextAnswerSet();
				Resource prop = queryResultLine.getResource("p");
				RDFPatternResource attribute = new RDFPatternResource(prop, RDFPatternResource.Type.In );

				if(! onto.isOntologyPropertyVocabulary(prop)) {
					if(! attributes.contains(attribute)) {
						attributes.add(attribute);
						if(! attributeItemIndex.containsKey(attribute)) {
							attributeItemIndex.put(attribute, getAttributeNumber());
							itemAttributeIndex.put(attributeItemIndex.get(attribute), attribute );
						}
					}
					indivResult.add(attribute);
				}
			}
		} catch(HttpException e) {

		} finally {
			itInResult.close();
		}
		
		return indivResult;
	}
	

	private RankNAttributeSet extractPathFragmentAttributesForIndividual(BaseRDF baseRDF, UtilOntology onto, Resource currIndiv) {

		RankNAttributeSet indivResult = new RankNAttributeSet();
		
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
								if(! attributes.contains(attribute)) {
									attributes.add(attribute);
									if(! attributeItemIndex.containsKey(attribute)) {
										attributeItemIndex.put(attribute, getAttributeNumber());
										itemAttributeIndex.put(attributeItemIndex.get(attribute), attribute );
									}
								}
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
								if(! attributes.contains(attribute)) {
									attributes.add(attribute);
									if(! attributeItemIndex.containsKey(attribute)) {
										attributeItemIndex.put(attribute, getAttributeNumber());
										itemAttributeIndex.put(attributeItemIndex.get(attribute), attribute );
									}
								}
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

	public RankNAttributeSet getAttributes() {
		return attributes;
	}

	public void setAttributes(RankNAttributeSet attributes) {
		this.attributes = attributes;
	}
	
}
