package com.irisa.swpatterns;

import java.io.IOException;
import java.util.Iterator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.ItemsetSet;
import com.irisa.swpatterns.data.LabeledTransaction;
import com.irisa.swpatterns.data.RDFPatternComponent;
import com.irisa.swpatterns.data.RDFPatternPathFragment;
import com.irisa.swpatterns.data.RDFPatternResource;
import com.irisa.swpatterns.data.LabeledTransactions;
import com.irisa.swpatterns.data.RDFPatternComponent.Type;

import ca.pfv.spmf.algorithms.frequentpatterns.fin_prepost.FIN;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPClose;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPMax;
import ca.pfv.spmf.algorithms.frequentpatterns.relim.AlgoRelim;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * Connection to the SMPF "library" or any other way to extract frequent itemsets from a transaction database
 * 
 * @author pmaillot
 *
 */
public class FrequentItemSetExtractor {

	private static Logger logger = Logger.getLogger(FrequentItemSetExtractor.class);

	private ALGORITHM _algo = ALGORITHM.FPClose;

	private static int countPattern = 0;

	private static String tmpTransactionFilename = "transactions.tmp";
	private static String tmpItemsetFilename = "itemsets.tmp";
	
	public enum ALGORITHM {
		FPMax,
		FPClose,
		Relim,
		FIN
	}

	public FrequentItemSetExtractor() {
	}

	public static int getPatternNumber() {
		return countPattern++;
	}

	public Itemsets computeItemsets(LabeledTransactions transactions, AttributeIndex index) {
		return computeItemsets(index.convertToTransactions(transactions));
	}


	public Itemsets computeItemsets(ItemsetSet is) {
		return computeItemsets(is.toItemsets());
	}

	public Itemsets computeItemsets(Itemsets transactions) {
		switch(this._algo) {
		case FPClose:
			logger.debug("Compute Frequent Itemsets with FPClose");
			return computeItemSet_FPClose(transactions);
		case FPMax:
			logger.debug("Compute Frequent Itemsets with FPMax");
			return this.computeItemSet_FPMax(transactions);
		case FIN:
			logger.debug("Compute Frequent Itemsets with FIN");
			return this.computeItemSet_FIN(transactions);
		case Relim:
			logger.debug("Compute Frequent Itemsets with Relim");
			return this.computeItemSet_Relim(transactions);
		default:
			return null;
		}
	}

	public Itemsets computeItemSet_FPMax(Itemsets transactions) {
		try {
			Utils.printItemsets(transactions, tmpTransactionFilename);
			return this.computeItemSet_FPMax(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPMax(String input) {
		try {
			logger.debug("FPMax Algorithm");
			AlgoFPMax algoFpc = new AlgoFPMax();
			Itemsets fpcResult = algoFpc.runAlgorithm(input, null, 0.0);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean algoFPClose() {
		return this._algo == ALGORITHM.FPClose;
	}

	public void setAlgoFPClose() {
		this._algo = ALGORITHM.FPClose;
	}

	public boolean algoFPMax() {
		return this._algo == ALGORITHM.FPMax;
	}

	public void setAlgoFPMax() {
		this._algo = ALGORITHM.FPMax;
	}

	public boolean algoRelim() {
		return this._algo == ALGORITHM.Relim;
	}

	public void setAlgoRelim() {
		this._algo = ALGORITHM.Relim;
	}

	public boolean algoFIN() {
		return this._algo == ALGORITHM.FIN;
	}

	public void setAlgoFIN() {
		this._algo = ALGORITHM.FIN;
	}

	public Itemsets computeItemSet_FPClose(LabeledTransactions transactions, AttributeIndex index) {
		try {
			index.printTransactionsItems(transactions, tmpTransactionFilename);

			return computeItemSet_FPClose(tmpTransactionFilename);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPClose(Itemsets is) {
		try {
			Utils.printItemsets(is, tmpTransactionFilename);

			return computeItemSet_FPClose(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPClose(String input) {
		try {
			AlgoFPClose algoFpc = new AlgoFPClose();
			logger.debug("FPClose Algorithm");
			Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(input, null, 0.0);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPMax(LabeledTransactions transactions, AttributeIndex index) {
		try {
			AlgoFPMax algoFpc = new AlgoFPMax();
			logger.debug("FBGrowth Algorithm");
			index.printTransactionsItems(transactions, tmpTransactionFilename);
			Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(tmpTransactionFilename, null, 0.0);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_Relim(String input) {
		try {
			AlgoRelim algoFpc = new AlgoRelim();
			logger.debug("Relim Algorithm");
			Itemsets fpcResult;
			algoFpc.runAlgorithm(0.0, input, tmpItemsetFilename);
			fpcResult = Utils.readItemsetFile(tmpItemsetFilename);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_Relim(Itemsets input) {
		try {
			Utils.printItemsets(input, tmpTransactionFilename);
			return this.computeItemSet_Relim(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_Relim(LabeledTransactions input, AttributeIndex index) {
		try {
			index.printTransactionsItems(input, tmpTransactionFilename);
			return this.computeItemSet_Relim(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FIN(Itemsets input) {
		try {
			Utils.printItemsets(input, tmpTransactionFilename);
			return this.computeItemSet_FIN(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FIN(LabeledTransactions input, AttributeIndex index) {
		try {
			index.printTransactionsItems(input, tmpTransactionFilename);
			return this.computeItemSet_FIN(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FIN(String input) {
		try {
			FIN algoFpc = new FIN();
			logger.debug("FIN Algorithm");
			Itemsets fpcResult;
			algoFpc.runAlgorithm(input, 0.0, tmpItemsetFilename);
			fpcResult = Utils.readItemsetFile(tmpItemsetFilename);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Model rdfizePattern(LabeledTransaction liSet) {
		Model result = ModelFactory.createDefaultModel();

		Resource mainRes = result.createResource(Global.basePatternUri + getPatternNumber());
		Resource node1 = null;
		Resource node2 = null;
		Resource node3 = null;
		Resource node4 = null;
		Resource node5 = null;
		Resource node1Type = null;
		Resource node2Type = null;
		Resource node3Type = null;
		Resource node4Type = null;
		Resource node5Type = null;
		Property relation1 = null;
		Property relation2 = null;
		Property relation3 = null;
		Property relation4 = null;

		Iterator<RDFPatternComponent> itItems = liSet.iterator();
		while(itItems.hasNext()) {
			RDFPatternComponent item = itItems.next();
			if(item instanceof RDFPatternResource) {
				if(item.getType()== Type.Type) {
					result.add(mainRes, RDF.type, ((RDFPatternResource) item).getResource());
				}
				else if(item.getType()== Type.Out) {
					result.add(mainRes, ((RDFPatternResource) item).getResource().as(Property.class), result.createResource());
				}
				else if(item.getType()== Type.In) {
					result.add(result.createResource(), ((RDFPatternResource) item).getResource().as(Property.class), mainRes);
				} else {
					switch (item.getType()) {
					case Node1:
						Property node1Rel = result.createProperty(Global.baseDomain+"property/node1");
						result.add(mainRes, node1Rel, ((RDFPatternResource) item).getResource());
						node1 = ((RDFPatternResource) item).getResource();
						break;
					case Node2:
						Property node2Rel = result.createProperty(Global.baseDomain+"property/node2");
						result.add(mainRes, node2Rel, ((RDFPatternResource) item).getResource());
						node2 = ((RDFPatternResource) item).getResource();
						break;
					case Node3:
						Property node3Rel = result.createProperty(Global.baseDomain+"property/node3");
						result.add(mainRes, node3Rel, ((RDFPatternResource) item).getResource());
						node3 = ((RDFPatternResource) item).getResource();
						break;
					case Node4:
						Property node4Rel = result.createProperty(Global.baseDomain+"property/node4");
						result.add(mainRes, node4Rel, ((RDFPatternResource) item).getResource());
						node4 = ((RDFPatternResource) item).getResource();
						break;
					case Node5:
						Property node5Rel = result.createProperty(Global.baseDomain+"property/node5");
						result.add(mainRes, node5Rel, ((RDFPatternResource) item).getResource());
						node5 = ((RDFPatternResource) item).getResource();
						break;
					case Node1Type:
						Property node1TypeRel = result.createProperty(Global.baseDomain+"property/node1type");
						result.add(mainRes, node1TypeRel, ((RDFPatternResource) item).getResource());
						node1Type = ((RDFPatternResource) item).getResource();
						break;
					case Node2Type:
						Property node2TypeRel = result.createProperty(Global.baseDomain+"property/node2type");
						result.add(mainRes, node2TypeRel, ((RDFPatternResource) item).getResource());
						node2Type = ((RDFPatternResource) item).getResource();
						break;
					case Node3Type:
						Property node3TypeRel = result.createProperty(Global.baseDomain+"property/node3type");
						result.add(mainRes, node3TypeRel, ((RDFPatternResource) item).getResource());
						node3Type = ((RDFPatternResource) item).getResource();
						break;
					case Node4Type:
						Property node4TypeRel = result.createProperty(Global.baseDomain+"property/node4type");
						result.add(mainRes, node4TypeRel, ((RDFPatternResource) item).getResource());
						node4Type = ((RDFPatternResource) item).getResource();
						break;
					case Node5Type:
						Property node5TypeRel = result.createProperty(Global.baseDomain+"property/node5type");
						result.add(mainRes, node5TypeRel, ((RDFPatternResource) item).getResource());
						node5Type = ((RDFPatternResource) item).getResource();
						break;
					case Relation1:
						Property relation1Rel = result.createProperty(Global.baseDomain+"property/relation1");
						result.add(mainRes, relation1Rel, ((RDFPatternResource) item).getResource());
						relation1 = ((RDFPatternResource) item).getResource().as(Property.class);
						break;
					case Relation2:
						Property relation2Rel = result.createProperty(Global.baseDomain+"property/relation2");
						result.add(mainRes, relation2Rel, ((RDFPatternResource) item).getResource());
						relation2 = ((RDFPatternResource) item).getResource().as(Property.class);
						break;
					case Relation3:
						Property relation3Rel = result.createProperty(Global.baseDomain+"property/relation3");
						result.add(mainRes, relation3Rel, ((RDFPatternResource) item).getResource());
						relation3 = ((RDFPatternResource) item).getResource().as(Property.class);
						break;
					case Relation4:
						Property relation4Rel = result.createProperty(Global.baseDomain+"property/relation4");
						result.add(mainRes, relation4Rel, ((RDFPatternResource) item).getResource());
						relation4 = ((RDFPatternResource) item).getResource().as(Property.class);
						break;

					default:
						break;
					}
				}
			} else if(item instanceof RDFPatternPathFragment) {
				if(item.getType() == Type.OutNeighbourType) {
					Resource object = result.createResource();
					result.add(mainRes, ((RDFPatternPathFragment) item).getPathFragment().getFirst().as(Property.class), object);
					result.add(object, RDF.type, ((RDFPatternPathFragment) item).getPathFragment().getSecond());
				} else if(item.getType() == Type.InNeighbourType) {
					Resource subject = result.createResource();
					result.add(subject, ((RDFPatternPathFragment) item).getPathFragment().getSecond().as(Property.class), mainRes);
					result.add(subject, RDF.type, ((RDFPatternPathFragment) item).getPathFragment().getFirst());
				}
			}
		}

		if(relation1 != null) {
			Resource subject = null;
			if(node1 != null) {
				subject = node1;
			} else if(node1Type != null) {
				subject = result.createResource(Global.baseDomain + "#nodeType" + node1Type.getLocalName());
				result.add(subject, RDF.type, node1Type);
			} else {
				subject = result.createResource();
			}
			Resource object = null;
			if(node2 != null) {
				object = node2;
			} else if(node2Type != null) {
				object = result.createResource(Global.baseDomain + "nodeType/#" + node2Type.getLocalName());
				result.add(object, RDF.type, node2Type);
			} else {
				object = result.createResource(Global.baseDomain + "nodeRelation/#" + relation1.getLocalName());
			}
			result.add(subject, relation1, object);
		}
		if(relation2 != null) {
			Resource subject = null;
			if(node2 != null) {
				subject = node2;
			} else if(node2Type != null) {
				subject = result.createResource(Global.baseDomain + "#nodeType" + node2Type.getLocalName());
				result.add(subject, RDF.type, node2Type);
			} else if(relation1 != null) {
				subject = result.createResource(Global.baseDomain + "nodeRelation/#" + relation1.getLocalName());
			} else {
				subject = result.createResource();
			}

			Resource object = null;
			if(node3 != null) {
				object = node3;
			} else if(node3Type != null) {
				object = result.createResource(Global.baseDomain + "nodeType/#" + node3Type.getLocalName());
				result.add(object, RDF.type, node3Type);
			} else {
				object = result.createResource(Global.baseDomain + "nodeRelation/#" + relation2.getLocalName());
			}

			result.add(subject, relation2, object);
		}
		if(relation3 != null) {
			Resource subject = null;
			if(node3 != null) {
				subject = node3;
			} else if(node3Type != null) {
				subject = result.createResource(Global.baseDomain + "nodeType/#" + node3Type.getLocalName());
				result.add(subject, RDF.type, node3Type);
			} else if(relation2 != null) {
				subject = result.createResource(Global.baseDomain + "nodeRelation/#" + relation2.getLocalName());
			} else {
				subject = result.createResource();
			}

			Resource object = null;
			if(node4 != null) {
				object = node4;
			} else if(node4Type != null) {
				object = result.createResource(Global.baseDomain + "nodeType/#" + node4Type.getLocalName());
				result.add(object, RDF.type, node4Type);
			} else {
				object = result.createResource(Global.baseDomain + "nodeRelation/#" + relation3.getLocalName());
			}

			if(node4Type != null) {
				result.add(object, RDF.type, node4Type);
			}
			result.add(subject, relation3, object);
		}
		if(relation4 != null) {
			Resource subject = null;
			if(node4 != null) {
				subject = node4;
			} else if(node4Type != null) {
				subject = result.createResource(Global.baseDomain + "nodeType/#" + node4Type.getLocalName());
				result.add(subject, RDF.type, node4Type);
			} else if(relation3 != null) {
				subject = result.createResource(Global.baseDomain + "nodeRelation/#" + relation3.getLocalName());
			} else {
				subject = result.createResource();
			}

			Resource object = null;
			if(node5 != null) {
				object = node5;
			} else if(node5Type != null) {
				object = result.createResource(Global.baseDomain + "nodeType/#" + node5Type.getLocalName());
				result.add(object, RDF.type, node5Type);
			} else {
				object = result.createResource(Global.baseDomain + "nodeRelation/#" + relation4.getLocalName());
			}
			result.add(subject, relation4, object);
		}

		result.add(result.createLiteralStatement(mainRes, result.createProperty(Global.baseDomain+"property/support"), liSet.getSupport()));

		return result;
	}


}
