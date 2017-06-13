package com.irisa.swpatterns;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.BaseRDF.MODE;
import com.irisa.swpatterns.data.Diagnostic;
import com.irisa.swpatterns.data.LabeledItemSet;
import com.irisa.swpatterns.data.RDFPatternComponent;
import com.irisa.swpatterns.data.RDFPatternPathFragment;
import com.irisa.swpatterns.data.RDFPatternResource;
import com.irisa.swpatterns.data.RankNAttributeSet;
import com.irisa.swpatterns.data.RankUpQuery;
import com.irisa.swpatterns.data.RDFPatternComponent.Type;
import com.irisa.jenautils.CustomQuerySolution;
import com.irisa.jenautils.QueryResultIterator;
import com.irisa.jenautils.UtilOntology;

import ca.pfv.spmf.algorithms.frequentpatterns.fin_prepost.PrePost;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPClose;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPMax;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * Connection to the SMPF "library" or any other way to extract frequent itemsets from a transaction database
 * @author pmaillot
 *
 */
public class FrequentItemSetExtractor {

	private static Logger logger = Logger.getLogger(FrequentItemSetExtractor.class);

	private boolean algoFPMax = false;
	private boolean algoFPClose = true;
	
	private static int countPattern = 0;

	public FrequentItemSetExtractor() {
	}

	public static int getPatternNumber() {
		return countPattern++;
	}

	public List<LabeledItemSet> computeItemsets(String outputTransactions) {
		if(this.algoFPMax()) {
			return TransactionsExtractor.labelItemSet(this.computeItemSet_FPMax(outputTransactions));
		} else if(this.algoFPClose()) {
			return TransactionsExtractor.labelItemSet(computeItemSet_FPClose(outputTransactions));
		}
		return null;
	}

	public boolean algoFPClose() {
		return algoFPClose;
	}

	public void setAlgoFPClose(boolean algo) {
		this.algoFPClose = algo;
		this.algoFPMax = ! algo;
	}

	public boolean algoFPMax() {
		return algoFPMax;
	}

	public void setAlgoFPMax(boolean algo) {
		this.algoFPMax = algo;
		this.algoFPClose = ! algo;
	}
	
	public Itemsets computeItemSet_FPClose(String outputTransactions) {
		try {
			AlgoFPClose algoFpc = new AlgoFPClose();
			logger.debug("FBGrowth Algorithm");
			Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(outputTransactions, null, 0.01);
			fpcResult.printItemsets(fpcResult.getItemsetsCount());
			
			return fpcResult;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Itemsets computeItemSet_FPMax(String outputTransactions) {
		try {
			AlgoFPMax algoFpc = new AlgoFPMax();
			logger.debug("FBGrowth Algorithm");
			Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(outputTransactions, null, 0.1);
			fpcResult.printItemsets(fpcResult.getItemsetsCount());
		
			return fpcResult;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Model rdfizePattern(LabeledItemSet liSet) {
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

		Iterator<RDFPatternComponent> itItems = liSet.getItems().iterator();
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

		result.add(result.createLiteralStatement(mainRes, result.createProperty(Global.baseDomain+"property/support"), liSet.getCount()));

		return result;
	}
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("file", true, "RDF file");
		options.addOption("compareTo", true, "RDF file to be compared to.");
		options.addOption("endpoint", true, "Endpoint adress");
		options.addOption("output", true, "Output csv file");
		options.addOption("limit", true, "Limit to the number of individuals extracted");
		options.addOption("resultWindow", true, "Size of the result window used to query servers.");
		options.addOption("classPattern", true, "Substring contained by the class uris.");
		options.addOption("noOut", false, "Not taking OUT properties into account.");
		options.addOption("noIn", false, "Not taking IN properties into account.");
		options.addOption("noTypes", false, "Not taking TYPES into account.");
		options.addOption("onlyTrans", false, "Juste extract the transactions to a '.dat' file with the index in a '.attr' file.");
		options.addOption("FPClose", false, "Use FPClose algorithm. (default)");
		options.addOption("FPMax", false, "Use FPMax algorithm.");
		options.addOption("class", true, "Class of the studied individuals.");
		options.addOption("rank1", false, "Extract informations up to rank 1 (types, out-going and in-going properties and object types), default is only types, out-going and in-going properties.");
//		options.addOption("rank0", false, "Extract informations up to rank 0 (out-going and in-going properties.");
		options.addOption("path", true, "Use FPClose algorithm. (default)");
		options.addOption("help", false, "Display this help.");

		// Setting up options and constants etc.
		UtilOntology onto = new UtilOntology();
		try {
			CommandLine cmd = parser.parse( options, args);

			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "RDFtoTransactionConverter", options );
			} else {
				TransactionsExtractor converter = new TransactionsExtractor();
				FrequentItemSetExtractor fsExtractor = new FrequentItemSetExtractor();

				String filename = cmd.getOptionValue("file");
				String fileCompare = cmd.getOptionValue("compareTo");
				String endpoint = cmd.getOptionValue("endpoint"); 
				String output = cmd.getOptionValue("output"); 
				String limitString = cmd.getOptionValue("limit");
				String resultWindow = cmd.getOptionValue("resultWindow");
				String classRegex = cmd.getOptionValue("classPattern");
				String className = cmd.getOptionValue("class");
				String pathOption = cmd.getOptionValue("path");
				boolean onlytrans = cmd.hasOption("onlyTrans");
				converter.setNoTypeTriples( cmd.hasOption("noTypes") || converter.noTypeTriples());
				converter.noInTriples(cmd.hasOption("noIn") || converter.noInTriples());
				converter.setNoOutTriples(cmd.hasOption("noOut") || converter.noOutTriples());
				fsExtractor.setAlgoFPClose(cmd.hasOption("FPClose") || fsExtractor.algoFPClose() );
				fsExtractor.setAlgoFPMax(cmd.hasOption("FPMax") || fsExtractor.algoFPMax() );
				converter.setRankOne(cmd.hasOption("rank1") || converter.isRankOne());

				String outputTransactions = "transactions."+filename + ".dat"; 
				String outputCompareTransactions = "transactions." + fileCompare + ".dat"; 
				String outputRDFPatterns = "rdfpatternes."+filename+".ttl"; 
				logger.debug("output: " + output + " limit:" + limitString + " resultWindow:" + resultWindow + " classpattern:" + classRegex + " noType:" + converter.noTypeTriples() + " noOut:" + converter.noOutTriples() + " noIn:"+ converter.noInTriples());

				if(limitString != null) {
					QueryResultIterator.setDefaultLimit(Integer.valueOf(limitString));
				}
				if(resultWindow != null) {
					QueryResultIterator.setDefaultLimit(Integer.valueOf(resultWindow));
				}
				if(cmd.hasOption("classPattern")) {
					UtilOntology.setClassRegex(classRegex);
				} else {
					UtilOntology.setClassRegex(null);
				}
				
				if(pathOption != null) {
					converter.setPathsLength(Integer.valueOf(pathOption));
				}

				BaseRDF baseRDF = null;
				if(filename != null) {
					baseRDF = new BaseRDF(filename, MODE.LOCAL);
				} else if (endpoint != null){
					baseRDF = new BaseRDF(endpoint, MODE.DISTANT);
				}

				logger.debug("initOnto");
				onto.init(baseRDF);

				logger.debug("extract");
				
				// Extracting transactions
				
				LinkedList<RankNAttributeSet> transactions;
				if(cmd.hasOption("class")) {
					Resource classRes = onto.getModel().createResource(className);
					transactions = converter.extractTransactionsForClass(baseRDF, onto, classRes);
				} else if(cmd.hasOption("path")) {
					transactions = converter.extractPathAttributes(baseRDF, onto);
				} else {
					transactions = converter.extractTransactions(baseRDF, onto);
				}
				
				try {
					converter.printTransactionsItems(transactions, outputTransactions);
				} catch (Exception e) {
					logger.fatal("RAAAH", e);
				}
				
				// If we asked more than juste extracting transactions
				if(! onlytrans) {
					List<LabeledItemSet> itemSets = fsExtractor.computeItemsets(outputTransactions);
					
					if(cmd.hasOption("compareTo")) { // Comparison of two datasets through Set operations on their frequent itemsets (ToBeDeleted)
						UtilOntology compareOnto = new UtilOntology();
						BaseRDF compBase = new BaseRDF(fileCompare, BaseRDF.MODE.LOCAL);
						compareOnto.init(compBase);
						converter.printTransactionsItems(converter.extractTransactions(compBase, compareOnto), outputCompareTransactions);
						List<LabeledItemSet> compareIs = fsExtractor.computeItemsets(outputCompareTransactions);
						if(compareIs != null) {
							Diagnostic diag = new Diagnostic(itemSets, compareIs);
							diag.compareItemsets();
							logger.debug("Communs:");
							logger.debug(diag.getCommons());
							diag.getCommons().forEach(new Consumer<LabeledItemSet>() {
								@Override
								public void accept(LabeledItemSet t) {
									fsExtractor.rdfizePattern(t).write(System.out, "TTL");
									logger.debug(converter.sparqlizeItemSet(t).getQuery());
								}
							});
							BiConsumer<LabeledItemSet, List<LabeledItemSet>> biCon = new BiConsumer<LabeledItemSet, List<LabeledItemSet>>( ){
								@Override
								public void accept(LabeledItemSet t, List<LabeledItemSet> u) {
									logger.debug(t + " INCLUS  " + u);
								}
							};
							logger.debug("Inclusion dans 1:");
							diag.getInclusionsIn1().forEach(biCon);
	
							logger.debug("Inclusion dans 2:");
							diag.getInclusionsIn2().forEach(biCon);
							logger.debug("Difference 1:");
							logger.debug(diag.getDifference1());
							logger.debug("Difference 2:");
							logger.debug(diag.getDifference2());
						}
					} else { // Printing the extracted itemsets and their RDF versions
						if(itemSets != null) {
							Model rdfPatterns = ModelFactory.createDefaultModel();
							Iterator<LabeledItemSet> itlas = itemSets.iterator();
							while(itlas.hasNext()) {
								LabeledItemSet labItemS = itlas.next();
								System.out.println(labItemS.getCount() + " " + labItemS.getItems());
								rdfPatterns.add(fsExtractor.rdfizePattern(labItemS));
								rdfPatterns.write(System.err, "TTL");
							}
							rdfPatterns.write(new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputRDFPatterns))), "TTL");
						}
					}
	
					baseRDF.close();
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		onto.close();
	}
	

}
