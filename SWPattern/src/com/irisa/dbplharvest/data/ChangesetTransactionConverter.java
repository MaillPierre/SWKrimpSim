package com.irisa.dbplharvest.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.jenautils.UtilOntology;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.LabeledTransaction;
import com.irisa.swpatterns.data.RDFPatternComponent;
import com.irisa.swpatterns.data.RDFPatternResource;
import com.irisa.swpatterns.data.RDFPatternComponent.Type;
import com.irisa.swpatterns.data.RDFPatternPathFragment;
import com.irisa.swpatterns.data.big.BigDataTransactionExtractor;
import com.irisa.utilities.Couple;

public class ChangesetTransactionConverter {

private static Logger logger = Logger.getLogger(BigDataTransactionExtractor.class);
	
	private static boolean conversionFailed = false;

	private HashSet<Resource> _individuals = new HashSet<Resource>();
	private UtilOntology _onto = new UtilOntology();

	private boolean _noTypeBool = false;
	private boolean _noInBool = false;
	private boolean _noOutBool = false;	

	private Neighborhood _neighborLevel = Neighborhood.PropertyAndType;

	private HashMap<Resource, LabeledTransaction> _buildingTransactionsTypeItems = new HashMap<Resource, LabeledTransaction>(); // TYPE items per resource
	private HashMap<Resource, LabeledTransaction> _buildingSecondaryResTypeItems = new HashMap<Resource, LabeledTransaction>(); // TYPE items per resource
	private HashMap<Resource, LabeledTransaction> _buildingTransactionsPropertyItems = new HashMap<Resource, LabeledTransaction>(); // PROPERTY items per resource

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
	

	/**
	 * Return a couple of transactions set <delete, add> corresponding to the triples of the update
	 * @deprecated For testing an example purposes, do not use as it is
	 * @param chg
	 * @return
	 */
	public Couple<ItemsetSet, ItemsetSet> extractChangesetTransactionsFromChangeset(Changeset chg) {
		ItemsetSet beforeSet = extractTransactionFromIterator(chg.getDelTriples());
		ItemsetSet afterSet = extractTransactionFromIterator(chg.getAddTriples());
		return new Couple<ItemsetSet, ItemsetSet>(beforeSet,afterSet);
//		Couple<ItemsetSet, ItemsetSet> result = new Couple<ItemsetSet, ItemsetSet>(, new ItemsetSet());
	}

//	/**
//	 * Return a couple of transactions set <before, after> corresponding to the state of the context before and after modification
//	 * @param chg
//	 * @return
//	 */
//	public Couple<ItemsetSet, ItemsetSet> extractChangesetTransactionsFromContext(Changeset chg) {
//
//		Couple<ItemsetSet, ItemsetSet> result = new Couple<ItemsetSet, ItemsetSet>(new ItemsetSet(), new ItemsetSet());
//
//		// First, line by line, fill the indexes
//		Model tmpModel = ModelFactory.createDefaultModel();
//		tmpModel.add(chg.getUnionTriples());
////		tmpModel.add(chg.getContextTriples());
////		tmpModel.remove(chg.getDelTriples()); // Removing the deleted triples
//		// tmpModel now only contains the triples that are purely contextual, they will not change after the update
//		StmtIterator dataIt = tmpModel.listStatements();
//
//		int nbtriples = 1;
//		int nbMaxtriples = 0;
//		int nbParsingErrors = 0;
//		try {
//
//			// Filling the indexes
//			while(dataIt.hasNext()) {
//				try {
//					Statement stat = dataIt.next();
//					Property prop = stat.getPredicate();
//					Resource subj = stat.getSubject();
//					RDFNode obj = stat.getObject();
//
//					if(subj != null && prop != null && obj != null) {
//						logger.trace("triple n° " + nbtriples + " read: " + subj + " " + prop + " " + obj);
//						if(prop.equals(RDF.type)) {
//							if( chg.isAffectedResource(subj)) { // Instantiation triple of modified resource
//								Resource objRes = obj.asResource();
//								RDFPatternResource compoType = AttributeIndex.getInstance().getComponent(objRes, Type.TYPE);
//								addComponentToIndexes(subj, compoType);
//							} else { // Instantiation triple of secondary resource
//								Resource objRes = obj.asResource();
//								RDFPatternResource compoType = AttributeIndex.getInstance().getComponent(objRes, Type.TYPE);
//								this.addComponentToSecondaryIndexes(subj, compoType);
//							}
//						} else if(! _onto.isOntologyPropertyVocabulary(prop) 
//								&& ! _onto.isOntologyClassVocabulary(subj)) { // property and subject not ontology stuff
//							RDFPatternResource compoPropOut = AttributeIndex.getInstance().getComponent(prop, Type.OUT_PROPERTY);
//							if( chg.isAffectedResource(subj)) {
//								addComponentToIndexes(subj, compoPropOut);
//							} 
//
//							if(! obj.isLiteral()){ // Object is not a literal 
//								Resource objRes = obj.asResource();
//								if(! _onto.isOntologyClassVocabulary(objRes)) { // Object is not Ontology stuff
//									RDFPatternResource compoPropIn = AttributeIndex.getInstance().getComponent(prop, Type.IN_PROPERTY);
//									if( chg.isAffectedResource(subj)) {
//										addComponentToIndexes(objRes, compoPropIn);
//									}
//								}
//							}
//						}
//					}
//					if(nbtriples % 1000000 == 0) {
//						logger.trace("Reaching " + nbtriples + " triples, loading...");
//					}
//					nbtriples++;
//					nbMaxtriples++;
//				} catch(Exception e) { // Catching the neurotic Jena parser exceptions
//					logger.trace("Exception during this line treatment: ", e);
//					nbParsingErrors++;
//				}
//			}
//		} finally {
//			dataIt.close();
//		}
//		
//		if(conversionFailed) {
//			return result;
//		}
//
//		// we explicitly try to recover the used memory
//		System.gc();
//		
//		if(this.getNeighborLevel() == Neighborhood.PropertyAndType) {
//			// First, line by line, fill the indexes
//			StmtIterator dataItSecond = chg.getUnionTriples().listStatements();
//
//			try {
//				// Filling the indexes
//				nbtriples = 1;
//				while(dataItSecond.hasNext()) {
//					try {
//						Statement stat = dataIt.next();
//						Property prop = stat.getPredicate();
//						Resource subj = stat.getSubject();
//						RDFNode obj = stat.getObject();
//
//						if(subj != null 
//								&& prop != null 
//								&& obj != null 
//								&& obj.isResource() 
//								&& ! _onto.isOntologyPropertyVocabulary(prop) 
//								&& ! _onto.isOntologyClassVocabulary(obj.asResource())) {
//							if(chg.isAffectedResource(subj)) { // Subject is modified resource
//								if(chg.isAffectedResource(subj) 
//										&& this._buildingTransactionsTypeItems.get(obj) != null) { // Object is also modified resource
//									Iterator<RDFPatternComponent> itObjType = this._buildingTransactionsTypeItems.get(obj).iterator();
//									while(itObjType.hasNext()) {
//										RDFPatternComponent compoObjType = itObjType.next();
//										Resource objType = ((RDFPatternResource) compoObjType).getResource();
//	
//										RDFPatternComponent compoOut = AttributeIndex.getInstance().getComponent(prop, objType, Type.OUT_NEIGHBOUR_TYPE);
//										addComponentToIndexes(subj, compoOut);
//									}
//								} else if(this._buildingSecondaryResTypeItems.get(obj) != null) {
//									Iterator<RDFPatternComponent> itObjType = this._buildingSecondaryResTypeItems.get(obj).iterator();
//									while(itObjType.hasNext()) {
//										RDFPatternComponent compoObjType = itObjType.next();
//										Resource objType = ((RDFPatternResource) compoObjType).getResource();
//	
//										RDFPatternComponent compoOut = AttributeIndex.getInstance().getComponent(prop, objType, Type.OUT_NEIGHBOUR_TYPE);
//										addComponentToIndexes(subj, compoOut);
//									}
//								}
//							}
//
//							if(chg.isAffectedResource(obj.asResource())) { // Object is modified resource
//								if(chg.isAffectedResource(subj) 
//										&& this._buildingTransactionsTypeItems.get(subj) != null) { // Subject is also modified resource
//									Iterator<RDFPatternComponent> itSubjType = this._buildingTransactionsTypeItems.get(subj).iterator();
//									while(itSubjType.hasNext()) {
//										RDFPatternComponent compoSubjType = itSubjType.next();
//										Resource subjType = ((RDFPatternResource) compoSubjType).getResource();
//	
//										RDFPatternComponent compoIn = AttributeIndex.getInstance().getComponent(prop, subjType, Type.IN_NEIGHBOUR_TYPE);
//										addComponentToIndexes(obj.asResource(), compoIn);
//									}	
//								} else if(this._buildingSecondaryResTypeItems.get(subj) != null){ // Subject is secondary resource
//									Iterator<RDFPatternComponent> itSubjType = this._buildingSecondaryResTypeItems.get(subj).iterator();
//									while(itSubjType.hasNext()) {
//										RDFPatternComponent compoSubjType = itSubjType.next();
//										Resource subjType = ((RDFPatternResource) compoSubjType).getResource();
//	
//										RDFPatternComponent compoIn = AttributeIndex.getInstance().getComponent(prop, subjType, Type.IN_NEIGHBOUR_TYPE);
//										addComponentToIndexes(obj.asResource(), compoIn);
//									}	
//								}
//							}
//						}
//						if(nbtriples % 1000000 == 0) {
//							logger.trace("Reaching " + nbtriples + " triples over " + nbMaxtriples + ", loading...");
//						}
//						nbtriples++;
//					} catch(Exception e) { // Catching the neurotic Jena parser exceptions
//						logger.trace("Exception during this line treatment: ", e);
//					}
//				}
//				logger.debug("End of second reading");
//			} finally {
//				dataItSecond.close();
//			}
//		}
//		
//		tmpModel.close();
//		// we explicitly try to recover the used memory
//		System.gc(); 
//		
//		// Extraction of the items of the modification of the changeset
//		HashMap<Resource, LabeledTransaction> deleteResTransactMap = new HashMap<Resource, LabeledTransaction>();
//		HashMap<Resource, LabeledTransaction> addResTransactMap = new HashMap<Resource, LabeledTransaction>();
//			// Init the partial transaction for add and delete
//		
//		Iterator<HashSet<Resource>> itAffect = chg.getAffectedResources().iterator();
//		while(itAffect.hasNext()) {
//			HashSet<Resource> affect = itAffect.next();
//			Iterator<Resource> itModified = affect.iterator();
//			while(itModified.hasNext()) {
//				Resource modifiedRes = itModified.next();
//				deleteResTransactMap.put(modifiedRes, new LabeledTransaction(modifiedRes));
//				addResTransactMap.put(modifiedRes, new LabeledTransaction(modifiedRes));
//			}
//		}
//		
//		// Delete items
//		StmtIterator itDeleteTriples = chg._deletedTriples.listStatements();
//		while(itDeleteTriples.hasNext()) {
//			Statement deleteTriple = itDeleteTriples.next();
//			Resource subj = deleteTriple.getSubject();
//			Property prop = deleteTriple.getPredicate();
//			RDFNode obj = deleteTriple.getObject();
//			
//			if(chg.isAffectedResource(subj)) { // Should be true
//				if(! this._onto.isOntologyPropertyVocabulary(prop)) {
//					if(prop != RDF.type) {
//						RDFPatternComponent compoSubjPropOut = new RDFPatternResource(prop, Type.OUT_PROPERTY);
//						deleteResTransactMap.get(subj).add(compoSubjPropOut);
//						
//						if(obj.isResource() 
//								&& chg.isAffectedResource(obj.asResource())) {
//								if( this._buildingTransactionsTypeItems.containsKey(obj)) {
//									LinkedList<RDFPatternComponent> listObjTypeItems = new LinkedList<RDFPatternComponent>();
//									Iterator<RDFPatternComponent> itObjType = this._buildingTransactionsTypeItems.get(obj).iterator();
//									while(itObjType.hasNext()) {
//										RDFPatternResource typeCompo = (RDFPatternResource) itObjType.next();
//										RDFPatternComponent compoSubjPropTypeOut = new RDFPatternPathFragment(prop, typeCompo.getResource(), Type.OUT_NEIGHBOUR_TYPE);
//										listObjTypeItems.add(compoSubjPropTypeOut);
//									}
//									deleteResTransactMap.get(subj).addAll(listObjTypeItems);
//								}
//							
//							RDFPatternComponent compoObjPropIn = new RDFPatternResource(prop, Type.IN_PROPERTY);
//							deleteResTransactMap.get(obj).add(compoObjPropIn);
//							if(this._buildingTransactionsTypeItems.containsKey(subj)) {
//								LinkedList<RDFPatternComponent> listSubjTypeItems = new LinkedList<RDFPatternComponent>();
//								Iterator<RDFPatternComponent> itSubjType = this._buildingTransactionsTypeItems.get(subj).iterator();
//								while(itSubjType.hasNext()) {
//									RDFPatternResource typeCompo = (RDFPatternResource) itSubjType.next();
//									RDFPatternComponent compoObjPropTypeIn = new RDFPatternPathFragment(prop, typeCompo.getResource(), Type.IN_NEIGHBOUR_TYPE);
//									listSubjTypeItems.add(compoObjPropTypeIn);
//								}
//								deleteResTransactMap.get(obj).addAll(listSubjTypeItems);
//							}
//						}
//					} else { // Its a typing triple
//						RDFPatternComponent compoSubjType = new RDFPatternResource(obj.asResource(), Type.TYPE);
//						deleteResTransactMap.get(subj).add(compoSubjType);
//					}
//				}
//			}
//		}
//		
//		// Add items
//		StmtIterator itAddTriples = chg._addedTriples.listStatements();
//		while(itAddTriples.hasNext()) {
//			Statement deleteTriple = itAddTriples.next();
//			Resource subj = deleteTriple.getSubject();
//			Property prop = deleteTriple.getPredicate();
//			RDFNode obj = deleteTriple.getObject();
//			
//			if(chg.isAffectedResource(subj)) { // Should be true
//				if(! this._onto.isOntologyPropertyVocabulary(prop)) {
//					if(prop != RDF.type) {
//						RDFPatternComponent compoSubjPropOut = new RDFPatternResource(prop, Type.OUT_PROPERTY);
//						addResTransactMap.get(subj).add(compoSubjPropOut);
//						
//						if(obj.isResource() 
//								&& chg.isAffectedResource(obj.asResource())) {
//							if(this._buildingTransactionsTypeItems.containsKey(obj)) {
//								LinkedList<RDFPatternComponent> listObjTypeItems = new LinkedList<RDFPatternComponent>();
//								Iterator<RDFPatternComponent> itObjType = this._buildingTransactionsTypeItems.get(obj).iterator();
//								while(itObjType.hasNext()) {
//									RDFPatternResource typeCompo = (RDFPatternResource) itObjType.next();
//									RDFPatternComponent compoSubjPropTypeOut = new RDFPatternPathFragment(prop, typeCompo.getResource(), Type.OUT_NEIGHBOUR_TYPE);
//									listObjTypeItems.add(compoSubjPropTypeOut);
//								}
//								addResTransactMap.get(subj).addAll(listObjTypeItems);
//							}
//							
//							RDFPatternComponent compoObjPropIn = new RDFPatternResource(prop, Type.IN_PROPERTY);
//							addResTransactMap.get(obj).add(compoObjPropIn);
//							if(this._buildingTransactionsTypeItems.containsKey(subj)) {
//								LinkedList<RDFPatternComponent> listSubjTypeItems = new LinkedList<RDFPatternComponent>();
//								Iterator<RDFPatternComponent> itSubjType = this._buildingTransactionsTypeItems.get(subj).iterator();
//								while(itSubjType.hasNext()) {
//									RDFPatternResource typeCompo = (RDFPatternResource) itSubjType.next();
//									RDFPatternComponent compoObjPropTypeIn = new RDFPatternPathFragment(prop, typeCompo.getResource(), Type.IN_NEIGHBOUR_TYPE);
//									listSubjTypeItems.add(compoObjPropTypeIn);
//								}
//								addResTransactMap.get(obj).addAll(listSubjTypeItems);
//							}
//						}
//					} else { // Its a typing triple
//						RDFPatternComponent compoSubjType = new RDFPatternResource(obj.asResource(), Type.TYPE);
//						addResTransactMap.get(subj).add(compoSubjType);
//					}
//				}
//			}
//		}
//		
//		// Final union of the building transactions
//		logger.trace("Union of all tmp transactions for the modified individuals");
//		
//		Iterator<HashSet<Resource>> itAffectFinal = chg.getAffectedResources().iterator();
//		while(itAffectFinal.hasNext()) {
//			HashSet<Resource> affect = itAffectFinal.next();
//			Iterator<Resource> itIndiv = affect.iterator();
//			int nbtreatedIndiv = 1;
//			while(itIndiv.hasNext()) {
//				Resource indiv = itIndiv.next();
//	
//				KItemset indivAddTrans = new KItemset();
//				KItemset indivDelTrans = new KItemset();
//				indivAddTrans.setLabel(indiv.toString());
//				indivDelTrans.setLabel(indiv.toString());
//				if(this._buildingTransactionsTypeItems.containsKey(indiv)) {
//					indivAddTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingTransactionsTypeItems.get(indiv)));
//					indivDelTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingTransactionsTypeItems.get(indiv)));
//					
//					this._buildingTransactionsTypeItems.remove(indiv);
//				}
//				if(this._buildingTransactionsPropertyItems.containsKey(indiv)) {
//					indivAddTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingTransactionsPropertyItems.get(indiv)));
//					indivDelTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingTransactionsPropertyItems.get(indiv)));
//					
//					this._buildingTransactionsPropertyItems.remove(indiv);
//				}
//				if(addResTransactMap.containsKey(indiv)) {
//					indivAddTrans.addAll(AttributeIndex.getInstance().convertToTransaction(addResTransactMap.get(indiv)));
//				}
//				if(deleteResTransactMap.containsKey(indiv)) {
//					indivDelTrans.addAll(AttributeIndex.getInstance().convertToTransaction(deleteResTransactMap.get(indiv)));
//				}
//	
//				if(! indivAddTrans.isEmpty()) {
//					result.getSecond().addItemset(indivAddTrans);
//				}
//				if(! indivDelTrans.isEmpty()) {
//					result.getFirst().addItemset(indivDelTrans);
//				}
//	
//				if(nbtreatedIndiv % 100000 == 0) {
//					logger.debug("Individual n°" + nbtreatedIndiv);
//				}
//				nbtreatedIndiv++;
//			}
//		}
//		logger.trace("All transactions united, (" + result.getFirst().size() + ", " + result.getSecond().size() + ") transactions for " + AttributeIndex.getInstance().size() + " attributes");
//
//		return result;
//	}
	
	public ItemsetSet extractTransactionFromIterator(Model source) {
		ItemsetSet result = new ItemsetSet();

		logger.debug("Jena loading ...");
		// First, line by line, fill the indexes
		Model model = ModelFactory.createDefaultModel(); 
		StmtIterator dataIt = source.listStatements();

		int nbtriples = 1;
		int nbMaxtriples = 0;
		int nbParsingErrors = 0;
		try {

			logger.debug("Jena loading ended");

			// Filling the indexes
			while(dataIt.hasNext()) {
				try {
					Statement stat = dataIt.next();
					Property prop = null;
					Resource subj = null;
					RDFNode obj = null;
					if(stat.getSubject() != null && stat.getSubject().getURI() != null) {
						subj = model.createResource(stat.getSubject().getURI());
					}
					if(stat.getPredicate() != null && stat.getPredicate().getURI() != null) {
						prop = model.createProperty(stat.getPredicate().getURI());
					}
					if(stat.getObject() != null) {
						if(stat.getObject().isLiteral()) {
							obj = model.createTypedLiteral(stat.getObject().asLiteral().getValue(), stat.getObject().asLiteral().getDatatype());
						} else if(stat.getObject().isURIResource()) {
							obj = model.createResource(stat.getObject().asResource());
						} else if(stat.getObject().isAnon()) {
							obj = model.createResource(stat.getObject().asResource().getId()); 
						}
					}

					if(subj != null && prop != null && obj != null) {
						logger.trace("triple n° " + nbtriples + " read: " + subj + " " + prop + " " + obj);
						if(prop.equals(RDF.type)) { // Instantiation triple
							if(! (obj.isLiteral())) { // checking basic RDF rule respect
								Resource objRes = obj.asResource();
								RDFPatternResource compoType = AttributeIndex.getInstance().getComponent(objRes, Type.TYPE);
								addComponentToIndexes(subj, compoType);
							}
						} else if(! _onto.isOntologyPropertyVocabulary(prop) && ! _onto.isOntologyClassVocabulary(subj)) { // property and subject not ontology stuff

							RDFPatternResource compoPropOut = AttributeIndex.getInstance().getComponent(prop, Type.OUT_PROPERTY);
							addComponentToIndexes(subj, compoPropOut);

							if(! obj.isLiteral()){ // Object is not a literal 
								Resource objRes = obj.asResource();
								if(! _onto.isOntologyClassVocabulary(objRes)) { // Object is not Ontology stuff
									RDFPatternResource compoPropIn = AttributeIndex.getInstance().getComponent(prop, Type.IN_PROPERTY);
									addComponentToIndexes(objRes, compoPropIn);
								}
							}
						}
					}
					if(nbtriples % 1000000 == 0) {
						logger.debug("Reaching " + nbtriples + " triples, loading...");
					}
					nbtriples++;
					nbMaxtriples++;
					Thread.sleep(0);
				} catch(Exception e) { // Catching the neurotic Jena parser exceptions
					logger.trace("Exception during this line treatment: ", e);
					nbParsingErrors++;
				}
			}
			logger.debug("Property based items built");
			logger.debug(nbParsingErrors + " parsing errors");
		} finally {
			dataIt.close();
		}
		
		if(conversionFailed) {
			return result;
		}
		logger.debug("End of first reading");

		// we explicitly try to recover the used memory
		System.gc();
		
		if(this.getNeighborLevel() == Neighborhood.PropertyAndType) {
			logger.debug("Second reading of the file fo the property-class conversion");
			// First, line by line, fill the indexes
			StmtIterator dataItSecond = source.listStatements();

			try {

				// Filling the indexes
				nbtriples = 1;
				while(dataItSecond.hasNext()) {
					try {
						Statement stat = dataItSecond.next();
						Property prop = null;
						Resource subj = null;
						RDFNode obj = null;
						if(stat.getSubject() != null && stat.getSubject().getURI() != null) {
							subj = model.createResource(stat.getSubject().getURI());
						}
						if(stat.getPredicate() != null && stat.getPredicate().getURI() != null) {
							prop = model.createProperty(stat.getPredicate().getURI());
						}
						if(stat.getObject() != null) {
							if(stat.getObject().isLiteral()) {
								obj = model.createTypedLiteral(stat.getObject().asLiteral().getValue(), stat.getObject().asLiteral().getDatatype());
							} else if(stat.getObject().isURIResource()) {
								obj = model.createResource(stat.getObject().asResource());
							} else if(stat.getObject().isAnon()) {
								obj = model.createResource(stat.getObject().asResource().getId()); 
							}
						}

						if(subj != null 
								&& prop != null 
								&& obj != null 
								&& obj.isResource() 
								&& ! _onto.isOntologyPropertyVocabulary(prop) 
								&& ! _onto.isOntologyClassVocabulary(obj.asResource())) {
							if(this._buildingTransactionsTypeItems.get(obj) != null) {
								Iterator<RDFPatternComponent> itObjType = this._buildingTransactionsTypeItems.get(obj).iterator();
								while(itObjType.hasNext()) {
									RDFPatternComponent compoObjType = itObjType.next();
									Resource objType = ((RDFPatternResource) compoObjType).getResource();

									RDFPatternComponent compoOut = AttributeIndex.getInstance().getComponent(prop, objType, Type.OUT_NEIGHBOUR_TYPE);
									addComponentToIndexes(subj, compoOut);
								}
							}

							if(this._buildingTransactionsTypeItems.get(subj) != null) {
								Iterator<RDFPatternComponent> itSubjType = this._buildingTransactionsTypeItems.get(subj).iterator();
								while(itSubjType.hasNext()) {
									RDFPatternComponent compoSubjType = itSubjType.next();
									Resource subjType = ((RDFPatternResource) compoSubjType).getResource();

									RDFPatternComponent compoIn = AttributeIndex.getInstance().getComponent(prop, subjType, Type.IN_NEIGHBOUR_TYPE);
									addComponentToIndexes(obj.asResource(), compoIn);
								}	
							}
						}
						if(nbtriples % 1000000 == 0) {
							logger.debug("Reaching " + nbtriples + " triples over " + nbMaxtriples + ", loading...");
						}
						nbtriples++;
						Thread.sleep(0);
					} catch(Exception e) { // Catching the neurotic Jena parser exceptions
						logger.trace("Exception during this line treatment: ", e);
					}
				}
				logger.debug("End of second reading");
			} finally {
				dataItSecond.close();
			}
			logger.debug("Property-class based items built");
		}
		model.close();

		System.gc(); 
		
		logger.debug("Union of all tmp transactions for " + this._individuals.size() + " individuals");
		// Union of the transactions
		Iterator<Resource> itIndiv = this._individuals.iterator();
		int nbtreatedIndiv = 1;
		while(itIndiv.hasNext()) {
			Resource indiv = itIndiv.next();

			KItemset indivTrans = new KItemset();
			indivTrans.setLabel(indiv.toString());
			if(this._buildingTransactionsTypeItems.containsKey(indiv)) {
				indivTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingTransactionsTypeItems.get(indiv)));
				this._buildingTransactionsTypeItems.remove(indiv);
			}
			if(this._buildingTransactionsPropertyItems.containsKey(indiv)) {
				indivTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingTransactionsPropertyItems.get(indiv)));
				this._buildingTransactionsPropertyItems.remove(indiv);
			}

			if(! indivTrans.isEmpty()) {
				result.addItemset(indivTrans);
			}

			if(nbtreatedIndiv % 100000 == 0) {
				logger.debug("Individual n°" + nbtreatedIndiv);
			}
			nbtreatedIndiv++;
		}
		logger.debug("All transactions united, " + result.size() + " transactions for " + AttributeIndex.getInstance().size() + " attributes");

		return result;
	}
	

	private void addComponentToIndexes(Resource res, RDFPatternComponent compo) {
		logger.trace("Adding component " + compo + " for resource " + res);
		if(!this._individuals.contains(res)) {
			this._individuals.add(res);
		}
		switch(compo.getType()) {
		case OUT_PROPERTY:
		case IN_PROPERTY:
		case OUT_NEIGHBOUR_TYPE: 
		case IN_NEIGHBOUR_TYPE: 
			if(! this._buildingTransactionsPropertyItems.containsKey(res)) { 
				this._buildingTransactionsPropertyItems.put(res, new LabeledTransaction(res));
			}
			this._buildingTransactionsPropertyItems.get(res).add(compo);
			break;
		case TYPE:
			if(! this._buildingTransactionsTypeItems.containsKey(res)) { 
				this._buildingTransactionsTypeItems.put(res, new LabeledTransaction(res));
			}
			this._buildingTransactionsTypeItems.get(res).add(compo);
			break;
		default:
			throw new LogicException("Unexpected element \""+ compo +"\" to add to the indexes for resource " + res );
		}
	}

	/**
	 * Should only be used to add typing component for secondary resources
	 * @param res
	 * @param compo
	 */
	private void addComponentToSecondaryIndexes(Resource res, RDFPatternComponent compo) {
		logger.trace("Adding component " + compo + " for secondary resource " + res);
		switch(compo.getType()) {
		case TYPE:
			if(! this._buildingSecondaryResTypeItems.containsKey(res)) { 
				this._buildingSecondaryResTypeItems.put(res, new LabeledTransaction(res));
			}
			this._buildingSecondaryResTypeItems.get(res).add(compo);
			break;
		case OUT_PROPERTY:
		case IN_PROPERTY:
		case OUT_NEIGHBOUR_TYPE: 
		case IN_NEIGHBOUR_TYPE: 
		default:
			throw new LogicException("Unexpected element \""+ compo +"\" to add to the indexes for resource " + res );
		}
	}

}
