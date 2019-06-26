package com.irisa.dbplharvest.data;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shared.Lock;
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
import com.irisa.utilities.Couple;

public class ChangesetTransactionConverterTDB {

	private static Logger logger = Logger.getLogger(ChangesetTransactionConverterTDB.class);

	private static boolean conversionFailed = false;

	private HashSet<Resource> _individuals = new HashSet<Resource>();
	private UtilOntology _onto = new UtilOntology();
	
	private Dataset dataset = null; 
	
	private boolean _noTypeBool = false;
	private boolean _noInBool = false;
	private boolean _noOutBool = false;	

	private Neighborhood _neighborLevel = Neighborhood.PropertyAndType;

	private ConcurrentHashMap<Resource, LabeledTransaction> _buildingTransactionsTypeItems = new ConcurrentHashMap<Resource, LabeledTransaction>(); // TYPE items per resource
	
	private ConcurrentHashMap<Resource, LabeledTransaction> _buildingTransactionsPropertyItems = new ConcurrentHashMap<Resource, LabeledTransaction>(); // PROPERTY items per resource
	
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
	
	

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	/**
	 * Extract the transactions of the triples of the changeset from the current context source. Clear the converter indexes after the extraction.
	 * 
	 * @param source
	 * @return
	 */
	public HashMap<Resource, KItemset> extractTransactionsFromAffectedResources(Changeset chg) {
		HashMap<Resource, KItemset> result = new HashMap<Resource, KItemset>();
		Model context = extractContextOfChangeset(chg);
		this.fillIndexesFromModel(context, chg);
		
		if(conversionFailed) {
			return result;
		}

		// Union of the transactions
		
		Iterator<HashSet<Resource>> itSets = chg.getAffectedResources().iterator();
		Iterator<Resource> itIndiv = null; 
		int nbtreatedIndiv = 1;
		
		itIndiv = chg.getFlattenedAffectedResources().iterator(); 
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
				result.put(indiv, indivTrans);
			}

			if(nbtreatedIndiv % 100000 == 0) {
				logger.debug("Individual n:" + nbtreatedIndiv);
			}
			nbtreatedIndiv++;
		}
		
		logger.debug("All transactions united, " + result.size() + " transactions for " + AttributeIndex.getInstance().size() + " attributes");
		clearIndexes();
		context.close(); 
		return result;
	}

	/**
	 * Will fill the indexes according to the triples in the model, filtering those concerning the affected resources of the changeset
	 * @param source
	 * @param chg
	 */
	protected void fillIndexesFromModel(Model source, Changeset chg) {

		logger.debug("Jena loading ...");
		// First, line by line, fill the indexes
		StmtIterator dataIt = source.listStatements();

		int nbtriples = 1;
		int nbMaxtriples = 0;
		int nbParsingErrors = 0;
		try {

			logger.debug("Jena loading ended");
			while (dataIt.hasNext() ) { 
				// this should be exactly equivalent as the previous version
				try {
					Statement stat = dataIt.next(); 
					Property prop = null;
					Resource subj = null;
					RDFNode obj = null;
					if(stat.getSubject() != null && stat.getSubject().getURI() != null) {
						subj = stat.getSubject();
					}
					if(stat.getPredicate() != null && stat.getPredicate().getURI() != null) {
						prop = stat.getPredicate();
					}
					if(stat.getObject() != null) {
						if(stat.getObject().isLiteral()) {
							obj = stat.getObject().asLiteral();
						} else if(stat.getObject().isURIResource()) {
							obj = stat.getObject().asResource();
						} else if(stat.getObject().isAnon()) {
							obj = stat.getObject().asResource(); 
						}
					}
					
					if(subj != null 
						&& prop != null 
						&& obj != null ) {
						if(prop.equals(RDF.type)) { // Instantiation triple
							if(! (obj.isLiteral())) { // checking basic RDF rule respect
								Resource objRes = obj.asResource();
								RDFPatternResource compoType = AttributeIndex.getInstance().getComponent(objRes, Type.TYPE);
								addComponentToIndexes(subj, compoType);
							}
						} else if(! _onto.isOntologyPropertyVocabulary(prop) 
								&& ! _onto.isOntologyClassVocabulary(subj)) { // property and subject not ontology stuff
	
							RDFPatternResource compoPropOut = AttributeIndex.getInstance().getComponent(prop, Type.OUT_PROPERTY);
							addComponentToIndexes(subj, compoPropOut);
	
							if(! obj.isLiteral() && ! obj.isAnon()){ // Object is not a literal 
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
					//Thread.sleep(0);
				} catch(Exception e) { // Catching the neurotic Jena parser exceptions
					logger.trace("Exception during this line treatment: ", e);
	//							nbParsingErrors++;
				}
			}		
			logger.debug("Property based items built");
			logger.debug(nbParsingErrors + " parsing errors");
		} finally {
			dataIt.close();
		}
		logger.debug("End of first reading");

		// we explicitly try to recover the used memory
		//System.gc();

		if(this.getNeighborLevel() == Neighborhood.PropertyAndType) {
			logger.debug("Second reading of the file fo the property-class conversion");
			// First, line by line, fill the indexes
			StmtIterator dataItSecond = source.listStatements();

			try {

				// Filling the indexes
				nbtriples = 1;
				// this should be exactly equivalent as the previous version
				while(dataItSecond.hasNext()) {
					try {
							Statement stat = dataItSecond.next();
							Property prop = null;
							Resource subj = null;
							RDFNode obj = null;
							if(stat.getSubject() != null && stat.getSubject().getURI() != null) {
								subj = stat.getSubject();
							}
							if(stat.getPredicate() != null && stat.getPredicate().getURI() != null) {
								prop = stat.getPredicate();
							}
							if(stat.getObject() != null) {
								if(stat.getObject().isLiteral()) {
									obj = stat.getObject().asLiteral();
								} else if(stat.getObject().isURIResource()) {
									obj = stat.getObject().asResource();
								} else if(stat.getObject().isAnon()) {
									obj = stat.getObject().asResource(); 
								}
							}

							if(subj != null 
									&& prop != null 
									&& obj != null 
									&& obj.isResource() 
									&& ! obj.isAnon()
									&& ! _onto.isOntologyPropertyVocabulary(prop) 
									&& ! _onto.isOntologyClassVocabulary(obj.asResource())
									&& (chg.isAffectedResource(subj) 
											|| (obj.isURIResource() 
													&& chg.isAffectedResource(obj.asResource())))) {
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
	}

	/**
	 * Extract from the contextSource attributes the triples containing resources appearing in the given Model and return an augmented Model 
	 * 
	 * @param source
	 * @return a Model containing both source and its context
	 */
	public Model extractContextOfChangeset	(Changeset chg) {
		Model result = ModelFactory.createDefaultModel();

//		CB: Iterative version 
//		Iterator<Resource> itRes = chg.getFlattenedAffectedResources().iterator();
//		
// 		
//		while (itRes.hasNext()) { 
//			Resource affectedRes = itRes.next();
//			if(affectedRes != null 
//					&& affectedRes.isResource() 
//					&& ! affectedRes.isAnon()
//					&& ! _onto.isOntologyPropertyVocabulary(affectedRes) 
//					&& ! _onto.isOntologyClassVocabulary(affectedRes)) {
//				this.dataset.begin(ReadWrite.READ);
//				result.add(this.dataset.getDefaultModel().listStatements(affectedRes, null, (RDFNode)null));
//				result.add(this.dataset.getDefaultModel().listStatements(null, null, affectedRes));
//				this.dataset.end(); 
//			}
//		}	
//		if(this.getNeighborLevel() == Neighborhood.PropertyAndType) {
//			ArrayList<Statement> extensions = new ArrayList<>(); 
//			this.dataset.begin(ReadWrite.READ);
//			result.listSubjects().forEachRemaining(sbj->
//					this.dataset.getDefaultModel().listStatements(sbj, RDF.type, (RDFNode)null).forEachRemaining(
//							stmt->extensions.add(stmt)));
//			result.listObjects().forEachRemaining(obj->
//					{
//						if (obj.isResource()) {
//							this.dataset.getDefaultModel().listStatements(obj.asResource(), RDF.type, (RDFNode)null).forEachRemaining(
//									stmt->extensions.add(stmt)); 
//						}
//					});
//			this.dataset.end(); 
//			result.add(extensions); 
//		}

		// Concurrent version
		chg.getFlattenedAffectedResources().parallelStream().forEach(
				affectedRes -> 
				{
					ArrayList<Statement> tmpList = new ArrayList<>(); 
					if(affectedRes != null 
							&& affectedRes.isResource() 
							&& ! affectedRes.isAnon()
							&& ! _onto.isOntologyPropertyVocabulary(affectedRes) 
							&& ! _onto.isOntologyClassVocabulary(affectedRes)) {

						this.dataset.begin(ReadWrite.READ);						
						this.dataset.getDefaultModel().listStatements(affectedRes, null, (RDFNode)null).forEachRemaining(tmpList::add);
						this.dataset.getDefaultModel().listStatements(null, null, affectedRes).forEachRemaining(tmpList::add);
						this.dataset.end(); 
						result.enterCriticalSection(Lock.WRITE) ;  // or Lock.WRITE
						try {
							result.add(tmpList); 
						} finally {
						    result.leaveCriticalSection() ;
						}
					}
				}
				
				); 		
		
		if(this.getNeighborLevel() == Neighborhood.PropertyAndType) {
			HashSet<Resource> individualsToExtend = new HashSet<>();
			
			result.listSubjects().forEachRemaining(individualsToExtend::add); 
			result.listObjects().forEachRemaining(obj->
										{
											if (obj.isResource()) {
												individualsToExtend.add(obj.asResource());  
											}
										});

			individualsToExtend.parallelStream().forEach(
					indToExtend ->
					{
						ArrayList<Statement> tmpList = new ArrayList<>(); 
						this.dataset.begin(ReadWrite.READ);	
						this.dataset.getDefaultModel().listStatements(indToExtend, RDF.type, (RDFNode)null).forEachRemaining(tmpList::add);
						this.dataset.end(); 						
						result.enterCriticalSection(Lock.WRITE) ;  // or Lock.WRITE
						try {
							result.add(tmpList); 
						} finally {
						    result.leaveCriticalSection() ;
						}
					}
			); 
		}
		return result;
	}


	private void addComponentToIndexes(Resource res, RDFPatternComponent compo) {
		// CB: directly adding should not change the result
		this._individuals.add(res); 
		
		// CB: we use putIfAbsent now, it is assured to be thread-safe
		switch(compo.getType()) {
		case OUT_PROPERTY:
		case IN_PROPERTY:
		case OUT_NEIGHBOUR_TYPE: 
		case IN_NEIGHBOUR_TYPE: 
			this._buildingTransactionsPropertyItems.putIfAbsent(res, new LabeledTransaction(res));
			this._buildingTransactionsPropertyItems.get(res).add(compo);
			break;
		case TYPE:
			this._buildingTransactionsTypeItems.putIfAbsent(res, new LabeledTransaction(res));
			this._buildingTransactionsTypeItems.get(res).add(compo);
			break;
		default:
			throw new LogicException("Unexpected element \""+ compo +"\" to add to the indexes for resource " + res );
		}
	}
	
	public void clearIndexes() {
		this._individuals.clear();
		this._buildingTransactionsPropertyItems.clear();
		this._buildingTransactionsTypeItems.clear();
	}

}
