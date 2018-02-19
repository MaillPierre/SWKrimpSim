package com.irisa.dbplharvest.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
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

public class ChangesetTransactionConverter {

	private static Logger logger = Logger.getLogger(ChangesetTransactionConverter.class);

	private static boolean conversionFailed = false;

	private HashSet<Resource> _individuals = new HashSet<Resource>();
	private UtilOntology _onto = new UtilOntology();
	private Model _contextSource = ModelFactory.createDefaultModel();

	private boolean _noTypeBool = false;
	private boolean _noInBool = false;
	private boolean _noOutBool = false;	

	private Neighborhood _neighborLevel = Neighborhood.PropertyAndType;

	private HashMap<Resource, LabeledTransaction> _buildingTransactionsTypeItems = new HashMap<Resource, LabeledTransaction>(); // TYPE items per resource
	//	private HashMap<Resource, LabeledTransaction> _buildingSecondaryResTypeItems = new HashMap<Resource, LabeledTransaction>(); // TYPE items per resource
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

	public void setContextSource(Model source) {
		this._contextSource = source;
	}

	public Model getContextSource() {
		return this._contextSource;
	}


	/**
	 * Extract the transactions of the triples of the changeset from the current context source. Clear the converter indexes after the extraction.
	 * 
	 * @param source
	 * @param exceptThose Model containing triples to be removed from the context
	 * @return
	 */
	protected ItemsetSet extractTransactionsFromAffectedResources(Changeset chg) {
		ItemsetSet result = new ItemsetSet();

		Model context = extractContextOfChangeset(chg);

		this.fillIndexesFromModel(context, chg);

		if(conversionFailed) {
			return result;
		}

		logger.debug("Union of all tmp transactions for " + this._individuals.size() + " individuals");
		logger.debug(this._individuals);
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
		
		clearIndexes();

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

			// Filling the indexes
			while(dataIt.hasNext()) {
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
						&& obj != null
						&& (chg.isAffectedResource(subj) 
								|| (obj.isURIResource() 
										&& chg.isAffectedResource(obj.asResource()))) ) {
						logger.trace("triple n° " + nbtriples + " read: " + subj + " " + prop + " " + obj);
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

		System.gc(); 
	}

	/**
	 * Extract from the contextSource attributes the triples containing resources appearing in the given Model and return an augmented Model 
	 * 
	 * @param source
	 * @return a Model containing both source and its context
	 */
	protected Model extractContextOfChangeset(Changeset chg) {
		Model result = ModelFactory.createDefaultModel();
		Iterator<HashSet<Resource>> itRes1 = chg.getAffectedResources().iterator();
		while(itRes1.hasNext()) {
			HashSet<Resource> hashres = itRes1.next();
			Iterator<Resource> itRes2 = hashres.iterator();
			while(itRes2.hasNext()) {
				Resource affectedRes = itRes2.next();
				if(affectedRes != null 
						&& affectedRes.isResource() 
						&& ! affectedRes.isAnon()
						&& ! _onto.isOntologyPropertyVocabulary(affectedRes) 
						&& ! _onto.isOntologyClassVocabulary(affectedRes)) {
	
					result.add(this._contextSource.listStatements(affectedRes, null, (RDFNode)null));
					result.add(this._contextSource.listStatements(null, null, affectedRes));

				}
			}
		}
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
	
	public void clearIndexes() {
		this._individuals.clear();
		this._buildingTransactionsPropertyItems.clear();
		this._buildingTransactionsTypeItems.clear();
	}

	//	/**
	//	 * Should only be used to add typing component for secondary resources
	//	 * @param res
	//	 * @param compo
	//	 */
	//	private void addComponentToSecondaryIndexes(Resource res, RDFPatternComponent compo) {
	//		logger.trace("Adding component " + compo + " for secondary resource " + res);
	//		switch(compo.getType()) {
	//		case TYPE:
	//			if(! this._buildingSecondaryResTypeItems.containsKey(res)) { 
	//				this._buildingSecondaryResTypeItems.put(res, new LabeledTransaction(res));
	//			}
	//			this._buildingSecondaryResTypeItems.get(res).add(compo);
	//			break;
	//		case OUT_PROPERTY:
	//		case IN_PROPERTY:
	//		case OUT_NEIGHBOUR_TYPE: 
	//		case IN_NEIGHBOUR_TYPE: 
	//		default:
	//			throw new LogicException("Unexpected element \""+ compo +"\" to add to the indexes for resource " + res );
	//		}
	//	}

}
