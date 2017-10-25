package com.irisa.swpatterns.data.big;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.jenautils.Couple;
import com.irisa.jenautils.UtilOntology;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.LabeledTransaction;
import com.irisa.swpatterns.data.RDFPatternComponent;
import com.irisa.swpatterns.data.RDFPatternComponent.Type;
import com.irisa.swpatterns.data.RDFPatternResource;

public class BigDataTransactionExtractor {
	
	private static Logger logger = Logger.getLogger(BigDataTransactionExtractor.class);
	
	private HashSet<Resource> _individuals = new HashSet<Resource>();
	private UtilOntology _onto = new UtilOntology();

	private boolean _noTypeBool = false;
	private boolean _noInBool = false;
	private boolean _noOutBool = false;	
	
	private Neighborhood _neighborLevel = Neighborhood.PropertyAndType;
	
	private HashMap<Resource, LabeledTransaction> _buildingTransactionsTypeItems = new HashMap<Resource, LabeledTransaction>(); // TYPE items per resource
	private HashMap<Resource, LabeledTransaction> _buildingTransactionsPropertyItems = new HashMap<Resource, LabeledTransaction>(); // PROPERTY items per resource
	
	private HashMap<Couple<Resource, Resource>, HashSet<Property>> _connectedResources = new HashMap<Couple<Resource, Resource>, HashSet<Property>>(); // Co-occuring property resources to generate PROPERTY_TYPE later
	private HashMap<Resource, LabeledTransaction> _buildingtransactionsPropertyClassItems = new HashMap<Resource, LabeledTransaction>();

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
	
	public ItemsetSet extractTransactionsFromFile(String filename) {
		logger.debug("big data loading START");
		
		logger.debug("Jena loading ...");
		// First, line by line, fill the indexes
		Model model = ModelFactory.createDefaultModel(); // TMP
//		model.read(filename); // TMP
		PipedRDFIterator<Triple> dataIt = new PipedRDFIterator<Triple>();
		PipedTriplesStream dataSteam = new PipedTriplesStream(dataIt);
		ExecutorService executor=Executors.newSingleThreadExecutor();
		Runnable parser=new Runnable(){
			@Override public void run(){
				RDFParser.source(filename).parse(dataSteam);
		}};
		executor.submit(parser);
		
//		dataSteam.start();
		logger.debug("Jena loading ended");
		
//		logger.debug("Iterating over " + model.size() + " statements");
			// Filling the indexes
//		StmtIterator itStat = model.listStatements();
//		while(itStat.hasNext()) {
		while(dataIt.hasNext()) {
//			Statement stat = itStat.next();
			Triple stat = dataIt.next();
			Property prop = null;//stat.getPredicate();
			Resource subj = null;stat.getSubject();
			RDFNode obj = null;//stat.getObject();
			if(stat.getSubject() != null && stat.getSubject().getURI() != null) {
				subj = model.createResource(stat.getSubject().getURI());
			}
			if(stat.getPredicate() != null && stat.getPredicate().getURI() != null) {
				prop = model.createProperty(stat.getPredicate().getURI());
			}
			if(stat.getObject() != null) {
				if(stat.getObject().isLiteral()) {
					obj = model.createTypedLiteral(stat.getObject().getLiteralValue(), stat.getObject().getLiteralDatatype());
				} else if(stat.getObject().isURI()) {
					obj = model.createResource(stat.getObject().getURI());
				} else if(stat.getObject().isBlank()) {
					obj = model.createResource(new AnonId(stat.getObject().getBlankNodeId())); 
				}
			}
			
			if(subj != null && prop != null && obj != null) {
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
	
							if(this.getNeighborLevel() == Neighborhood.PropertyAndType) { // Filling the co-occuring indexes
								addConnection(subj, objRes, prop);
							}
						}
					}
				}
			}
		}
		logger.debug("End of iterations");
//		dataSteam.finish();
		executor.shutdown();
		model.close();
		
		logger.debug("Building property-class items over " + this._connectedResources.size() + " connexions");
		// If conversion in property-class, generate the property-class items from the co-occuring indexes and the type index
		int nbConnexion = 1;
		if(this.getNeighborLevel() == Neighborhood.PropertyAndType) {
			Iterator<Entry<Couple<Resource, Resource>, HashSet<Property>>> itConnected = this._connectedResources.entrySet().iterator();
			while(itConnected.hasNext()) {
				Entry<Couple<Resource, Resource>, HashSet<Property>> entry = itConnected.next();
				Couple<Resource, Resource> resCouple = entry.getKey();
				Resource subj = resCouple.getFirst();
				Resource obj = resCouple.getSecond();
				logger.trace("Examining connexion between " + subj + " and " + obj + " with " + entry.getValue());
				
				Iterator<Property> itProp = entry.getValue().iterator();
				while(itProp.hasNext()) {
					Property prop = itProp.next();
					
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
							addComponentToIndexes(obj, compoIn);
						}	
					}
				}
				
				if(nbConnexion % 10000 == 0) {
					logger.debug("Property-class connexion n°" + nbConnexion);
				}
				nbConnexion++;
			}
		}
		logger.debug("Property-class items built");
		
		logger.debug("Union of all tmp transactions for " + this._individuals.size() + " individuals");
		// Union of the transactions
		ItemsetSet result = new ItemsetSet();
		Iterator<Resource> itIndiv = this._individuals.iterator();
		int nbtreatedIndiv = 1;
		while(itIndiv.hasNext()) {
			Resource indiv = itIndiv.next();
			
			KItemset indivTrans = new KItemset();
			indivTrans.setLabel(indiv.toString());
			if(this._buildingTransactionsTypeItems.containsKey(indiv)) {
				indivTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingTransactionsTypeItems.get(indiv)));
				this._buildingTransactionsTypeItems.remove(indiv);
				if(this._buildingTransactionsPropertyItems.containsKey(indiv)) {
					indivTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingTransactionsPropertyItems.get(indiv)));
					this._buildingTransactionsPropertyItems.remove(indiv);
				}
				if(this._buildingtransactionsPropertyClassItems.containsKey(indiv)) {
					indivTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingtransactionsPropertyClassItems.get(indiv)));
					this._buildingtransactionsPropertyClassItems.remove(indiv);
				}
			}
			
			if(! indivTrans.isEmpty()) {
				result.add(indivTrans);
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
		case OUT_NEIGHBOUR_TYPE:
		case IN_NEIGHBOUR_TYPE:
			if(! this._buildingtransactionsPropertyClassItems.containsKey(res)) {
				this._buildingtransactionsPropertyClassItems.put(res, new LabeledTransaction(res));
			}
			this._buildingtransactionsPropertyClassItems.get(res).add(compo);
		break;
		default:
			throw new LogicException("Unexpected element \""+ compo +"\" to add to the indexes for resource " + res );
		}
	}
	
	private void addConnection(Resource subj, Resource obj, Property prop) {
		logger.trace("Connexion between " + subj + " and " + obj + " with " + prop);
		Couple<Resource, Resource> couple = new Couple<Resource, Resource>(subj, obj);
		if(! this._connectedResources.containsKey(couple)) {
			this._connectedResources.put(couple, new HashSet<Property>());
		}
		this._connectedResources.get(couple).add(prop);
	}
	
}
