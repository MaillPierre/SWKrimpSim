package com.irisa.swpatterns.data.big;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
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
import com.irisa.swpatterns.data.RDFPatternComponent.Type;
import com.irisa.swpatterns.data.RDFPatternResource;

public class BigDataTransactionExtractor {

	private static Logger logger = Logger.getLogger(BigDataTransactionExtractor.class);
	
	private static boolean conversionFailed = false;

	private HashSet<Resource> _individuals = new HashSet<Resource>();
	private UtilOntology _onto = new UtilOntology();

	private boolean _noTypeBool = false;
	private boolean _noInBool = false;
	private boolean _noOutBool = false;	

	private Neighborhood _neighborLevel = Neighborhood.PropertyAndType;

	private HashMap<Resource, LabeledTransaction> _buildingTransactionsTypeItems = new HashMap<Resource, LabeledTransaction>(); // TYPE items per resource
	private HashMap<Resource, LabeledTransaction> _buildingTransactionsPropertyItems = new HashMap<Resource, LabeledTransaction>(); // PROPERTY items per resource

	// private HashMap<Resource, LabeledTransaction> _buildingtransactionsPropertyClassItems = new HashMap<Resource, LabeledTransaction>();

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

	private static Thread bigdataParserThread(String filename, PipedTriplesStream dataStream) {
		conversionFailed = false;
		Thread parser=new Thread(){
			@Override public void run(){
				try {
					RDFParser.source(filename)
					.checking(false) // SHOULD NOT stop at malformed IRIs, but does anyway when they contain spaces...
					.errorHandler(ErrorHandlerFactory.errorHandlerNoWarnings)
					.errorHandler(new ErrorHandler() {
						private Logger rdfParserLogger = Logger.getLogger(RDFParser.class);
						
						@Override
						public void warning(String arg0, long arg1, long arg2) {
							rdfParserLogger.warn("line: " + arg1 + " col: " + arg2 + ", " + arg0);
						}
						@Override
						public void fatal(String arg0, long arg1, long arg2) {
							warning(arg0, arg1, arg2);
						}
						@Override
						public void error(String arg0, long arg1, long arg2) {
							warning(arg0, arg1, arg2);
						}
					})
					.parse(dataStream);
				} catch(Exception e) {
					logger.error("Unhandled exception during parsing, Dataset probably badly formated: ", e);
					conversionFailed = true;
					throw e;
				}
			}};

			return parser;
	}

	public ItemsetSet extractTransactionsFromFile(String filename) {
		logger.debug("big data loading START");

		ItemsetSet result = new ItemsetSet();

		logger.debug("Jena loading ...");
		// First, line by line, fill the indexes
		Model model = ModelFactory.createDefaultModel(); 
		PipedRDFIterator<Triple> dataIt = new PipedRDFIterator<Triple>();
		PipedTriplesStream dataSteam = new PipedTriplesStream(dataIt);
		ExecutorService executor=Executors.newSingleThreadExecutor();

		int nbtriples = 1;
		int nbMaxtriples = 0;
		int nbParsingErrors = 0;
		try {
			Thread parser = bigdataParserThread(filename, dataSteam);
			executor.submit(parser);

			logger.debug("Jena loading ended");

			// Filling the indexes
			while(dataIt.hasNext()) {
				try {
					Triple stat = dataIt.next();
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
							obj = model.createTypedLiteral(stat.getObject().getLiteralValue(), stat.getObject().getLiteralDatatype());
						} else if(stat.getObject().isURI()) {
							obj = model.createResource(stat.getObject().getURI());
						} else if(stat.getObject().isBlank()) {
							obj = model.createResource(new AnonId(stat.getObject().getBlankNodeId())); 
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
			executor.shutdown();
			dataIt.close();
		}
		
		if(conversionFailed) {
			return result;
		}
		logger.debug("End of first reading");

		// we explicitly try to recover the used memory
		executor = null; 
		System.gc();
		
		if(this.getNeighborLevel() == Neighborhood.PropertyAndType) {
			logger.debug("Second reading of the file fo the property-class conversion");
			// First, line by line, fill the indexes
			PipedRDFIterator<Triple> dataItSecond = new PipedRDFIterator<Triple>();
			PipedTriplesStream dataSteamSecond = new PipedTriplesStream(dataItSecond);
			ExecutorService executorSecond=Executors.newSingleThreadExecutor();

			try {
				Thread parserSecond = bigdataParserThread(filename, dataSteamSecond);
				executorSecond.submit(parserSecond);

				// Filling the indexes
				nbtriples = 1;
				while(dataItSecond.hasNext()) {
					try {
						Triple stat = dataItSecond.next();
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
								obj = model.createTypedLiteral(stat.getObject().getLiteralValue(), stat.getObject().getLiteralDatatype());
							} else if(stat.getObject().isURI()) {
								obj = model.createResource(stat.getObject().getURI());
							} else if(stat.getObject().isBlank()) {
								obj = model.createResource(new AnonId(stat.getObject().getBlankNodeId())); 
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
				executorSecond.shutdown();
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
//			if(this._buildingtransactionsPropertyClassItems.containsKey(indiv)) {
//				indivTrans.addAll(AttributeIndex.getInstance().convertToTransaction(this._buildingtransactionsPropertyClassItems.get(indiv)));
//				this._buildingtransactionsPropertyClassItems.remove(indiv);
//			}

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
//		case OUT_NEIGHBOUR_TYPE:
//		case IN_NEIGHBOUR_TYPE:
//			if(! this._buildingtransactionsPropertyClassItems.containsKey(res)) {
//				this._buildingtransactionsPropertyClassItems.put(res, new LabeledTransaction(res));
//			}
//			this._buildingtransactionsPropertyClassItems.get(res).add(compo);
//			break;
		default:
			throw new LogicException("Unexpected element \""+ compo +"\" to add to the indexes for resource " + res );
		}
	}

}
