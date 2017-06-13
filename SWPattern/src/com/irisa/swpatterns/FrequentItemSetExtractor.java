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

public class FrequentItemSetExtractor {

	private static Logger logger = Logger.getLogger(FrequentItemSetExtractor.class);

	private static long queryLimit = 0;
	private boolean noTypeBool = false;
	private boolean noInBool = false;
	private boolean noOutBool = false;

	private boolean algoPrepost = false;
	private boolean algoFPClose = true;
	
	private boolean rankOne = false;
	private boolean randZero = true;
	private int paths = 0;

	private RankNAttributeSet attributes = new RankNAttributeSet();
	private HashMap<RDFPatternComponent, Integer> attributeItemIndex = new HashMap<RDFPatternComponent, Integer>();
	private HashMap<Integer, RDFPatternComponent> itemAttributeIndex = new HashMap<Integer, RDFPatternComponent>();
	private long totalTransactions = 0;

	private static String baseDomain = "http://www.irisa.fr/semLIS/";
	private static String basePatternUri = "http://www.irisa.fr/semLIS/#pattern";
	private static int countPattern = 0;
	private static int counterAttribute = 0;

	public FrequentItemSetExtractor() {
	}

	public static int getPatternNumber() {
		return countPattern++;
	}
	
	public static int getAttributeNumber() {
		return counterAttribute++;
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

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
		options.addOption("prepost", false, "Use Prepost algorithm.");
		options.addOption("FPClose", false, "Use FPClose algorithm. (default)");
		options.addOption("class", true, "Class of the studied individuals.");
		options.addOption("rank1", false, "Extract informations up to rank 1 (properties and object types), default is only types, out-going and in-going properties.");
//		options.addOption("rank0", false, "Extract informations up to rank 0 (out-going and in-going properties.");
		options.addOption("path", true, "Use FPClose algorithm. (default)");
		options.addOption("help", false, "Display this help.");


		UtilOntology onto = new UtilOntology();
		try {
			CommandLine cmd = parser.parse( options, args);

			boolean helpAsked = cmd.hasOption("help");
			if(helpAsked) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "RDFtoTransactionConverter", options );
			} else {
				FrequentItemSetExtractor converter = new FrequentItemSetExtractor();

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
				converter.setAlgoPrepost(cmd.hasOption("prepost") || converter.algoPrepost());
				converter.setAlgoFPClose(cmd.hasOption("FPClose") || converter.algoFPClose() );
				converter.setRankOne(cmd.hasOption("rank1") || converter.isRankOne());

				String outputTransactions = "transactions."+filename + ".dat"; 
				String outputCompareTransactions = "transactions." + fileCompare + ".dat"; 
				String outputRDFPatterns = "rdfpatternes."+filename+".ttl"; 
				logger.debug("output: " + output + " limit:" + limitString + " resultWindow:" + resultWindow + " classpattern:" + classRegex + " noType:" + converter.noTypeTriples() + " noOut:" + converter.noOutTriples() + " noIn:"+ converter.noInTriples());

				if(limitString != null) {
					setQueryLimit(Integer.valueOf(limitString));
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
					converter.paths = Integer.valueOf(pathOption);
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
				
				LinkedList<RankNAttributeSet> transactions;
				if(cmd.hasOption("class")) {
					Resource classRes = onto.getModel().createResource(className);
					transactions = converter.extractTransactionsForClass(baseRDF, onto, classRes);
				} else if(cmd.hasOption("path")) {
					transactions = converter.extractPathAttributes(baseRDF, onto);
				} else {
					transactions = converter.extractTransactions(baseRDF, onto);
				}
				converter.setTotalTransactions(transactions.size());
				try {
					converter.printTransactionsItems(transactions, outputTransactions);
				} catch (Exception e) {
					logger.fatal("RAAAH", e);
				}
				
				if(! onlytrans) {
					List<LabeledItemSet> itemSets = converter.computeItemsets(outputTransactions);
					
					if(cmd.hasOption("compareTo")) {
						UtilOntology compareOnto = new UtilOntology();
						BaseRDF compBase = new BaseRDF(fileCompare, BaseRDF.MODE.LOCAL);
						compareOnto.init(compBase);
						converter.printTransactionsItems(converter.extractTransactions(compBase, compareOnto), outputCompareTransactions);
						List<LabeledItemSet> compareIs = converter.computeItemsets(outputCompareTransactions);
						if(compareIs != null) {
							Diagnostic diag = new Diagnostic(itemSets, compareIs);
							diag.compareItemsets();
							logger.debug("Communs:");
							logger.debug(diag.getCommons());
							diag.getCommons().forEach(new Consumer<LabeledItemSet>() {
								@Override
								public void accept(LabeledItemSet t) {
									converter.rdfizeItemset(t).write(System.out, "TTL");
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
					} else {				
						if(itemSets != null) {
							Model rdfPatterns = ModelFactory.createDefaultModel();
							Iterator<LabeledItemSet> itlas = itemSets.iterator();
							while(itlas.hasNext()) {
								LabeledItemSet labItemS = itlas.next();
								System.out.println(labItemS.getCount() + " " + labItemS.getItems());
								rdfPatterns.add(converter.rdfizeItemset(labItemS));
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
	
	public List<LabeledItemSet> computeItemsets(String outputTransactions) {
//		if(this.algoPrepost()) {
//			this.computeItemSet_PrePost(outputTransactions);
//		} else if(this.algoFPClose()) {
			return this.computeItemSet_FPMax(outputTransactions);
//		}
//		return null;
	}

	public long getQueryLimit() {
		return queryLimit;
	}

	public static void setQueryLimit(long limit) {
		queryLimit = limit;
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

	public boolean algoPrepost() {
		return algoPrepost;
	}

	public void setAlgoPrepost(boolean algoPrepost) {
		this.algoPrepost = algoPrepost;
	}

	public boolean algoFPClose() {
		return algoFPClose;
	}

	public void setAlgoFPClose(boolean algoFPClose) {
		this.algoFPClose = algoFPClose;
	}

	public RankNAttributeSet getAttributes() {
		return attributes;
	}

	public void setAttributes(RankNAttributeSet attributes) {
		this.attributes = attributes;
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
	
	public List<LabeledItemSet> computeItemSet_FPClose(String outputTransactions) {
		LinkedList<LabeledItemSet> result = new LinkedList<LabeledItemSet>();
		try {
		AlgoFPClose algoFpc = new AlgoFPClose();
		logger.debug("FBGrowth Algorithm");
		Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(outputTransactions, null, 0.01);
		fpcResult.printItemsets(fpcResult.getItemsetsCount());

		Iterator<List<Itemset>> itlist = fpcResult.getLevels().iterator();
		while(itlist.hasNext()) {
			List<Itemset> listIs = itlist.next();
			Iterator<Itemset> itIs = listIs.iterator();
			while(itIs.hasNext()) {
				Itemset itemSe = itIs.next();
				LabeledItemSet labItemS = this.labelItemSet(itemSe);
				result.add(labItemS);
			}
		}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public List<LabeledItemSet> computeItemSet_FPMax(String outputTransactions) {
		LinkedList<LabeledItemSet> result = new LinkedList<LabeledItemSet>();
		try {
		AlgoFPMax algoFpc = new AlgoFPMax();
		logger.debug("FBGrowth Algorithm");
		Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(outputTransactions, null, 0.1);
		fpcResult.printItemsets(fpcResult.getItemsetsCount());

		Iterator<List<Itemset>> itlist = fpcResult.getLevels().iterator();
		while(itlist.hasNext()) {
			List<Itemset> listIs = itlist.next();
			Iterator<Itemset> itIs = listIs.iterator();
			while(itIs.hasNext()) {
				Itemset itemSe = itIs.next();
				LabeledItemSet labItemS = this.labelItemSet(itemSe);
				result.add(labItemS);
			}
		}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public void computeItemSet_PrePost(String outputTransactions) {
		try {
		PrePost algoPrepost = new PrePost();
		algoPrepost.setUsePrePostPlus(true);
		logger.debug("prepost Algorithm");
		algoPrepost.runAlgorithm(outputTransactions, 0.0, "prepost."+outputTransactions+".result");
		logger.debug("Results of prepost Algorithm in prepost."+outputTransactions+".result");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		return extractPathAttributes(baseRDF, onto, this.paths);
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

	private LabeledItemSet labelItemSet(Itemset iSet) {
		LabeledItemSet result = new LabeledItemSet();

		for(int i = 0; i < iSet.getItems().length; i++) {
			result.addItem(this.itemAttributeIndex.get(iSet.get(i)));
		}
		result.setCount(iSet.getAbsoluteSupport());

		return result;
	}

	public Model rdfizeItemset(LabeledItemSet liSet) {
		Model result = ModelFactory.createDefaultModel();

		Resource mainRes = result.createResource(basePatternUri + getPatternNumber());
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
						Property node1Rel = result.createProperty(baseDomain+"property/node1");
						result.add(mainRes, node1Rel, ((RDFPatternResource) item).getResource());
						node1 = ((RDFPatternResource) item).getResource();
						break;
					case Node2:
						Property node2Rel = result.createProperty(baseDomain+"property/node2");
						result.add(mainRes, node2Rel, ((RDFPatternResource) item).getResource());
						node2 = ((RDFPatternResource) item).getResource();
						break;
					case Node3:
						Property node3Rel = result.createProperty(baseDomain+"property/node3");
						result.add(mainRes, node3Rel, ((RDFPatternResource) item).getResource());
						node3 = ((RDFPatternResource) item).getResource();
						break;
					case Node4:
						Property node4Rel = result.createProperty(baseDomain+"property/node4");
						result.add(mainRes, node4Rel, ((RDFPatternResource) item).getResource());
						node4 = ((RDFPatternResource) item).getResource();
						break;
					case Node5:
						Property node5Rel = result.createProperty(baseDomain+"property/node5");
						result.add(mainRes, node5Rel, ((RDFPatternResource) item).getResource());
						node5 = ((RDFPatternResource) item).getResource();
						break;
					case Node1Type:
						Property node1TypeRel = result.createProperty(baseDomain+"property/node1type");
						result.add(mainRes, node1TypeRel, ((RDFPatternResource) item).getResource());
						node1Type = ((RDFPatternResource) item).getResource();
						break;
					case Node2Type:
						Property node2TypeRel = result.createProperty(baseDomain+"property/node2type");
						result.add(mainRes, node2TypeRel, ((RDFPatternResource) item).getResource());
						node2Type = ((RDFPatternResource) item).getResource();
						break;
					case Node3Type:
						Property node3TypeRel = result.createProperty(baseDomain+"property/node3type");
						result.add(mainRes, node3TypeRel, ((RDFPatternResource) item).getResource());
						node3Type = ((RDFPatternResource) item).getResource();
						break;
					case Node4Type:
						Property node4TypeRel = result.createProperty(baseDomain+"property/node4type");
						result.add(mainRes, node4TypeRel, ((RDFPatternResource) item).getResource());
						node4Type = ((RDFPatternResource) item).getResource();
						break;
					case Node5Type:
						Property node5TypeRel = result.createProperty(baseDomain+"property/node5type");
						result.add(mainRes, node5TypeRel, ((RDFPatternResource) item).getResource());
						node5Type = ((RDFPatternResource) item).getResource();
						break;
					case Relation1:
						Property relation1Rel = result.createProperty(baseDomain+"property/relation1");
						result.add(mainRes, relation1Rel, ((RDFPatternResource) item).getResource());
						relation1 = ((RDFPatternResource) item).getResource().as(Property.class);
						break;
					case Relation2:
						Property relation2Rel = result.createProperty(baseDomain+"property/relation2");
						result.add(mainRes, relation2Rel, ((RDFPatternResource) item).getResource());
						relation2 = ((RDFPatternResource) item).getResource().as(Property.class);
						break;
					case Relation3:
						Property relation3Rel = result.createProperty(baseDomain+"property/relation3");
						result.add(mainRes, relation3Rel, ((RDFPatternResource) item).getResource());
						relation3 = ((RDFPatternResource) item).getResource().as(Property.class);
						break;
					case Relation4:
						Property relation4Rel = result.createProperty(baseDomain+"property/relation4");
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
				subject = result.createResource(baseDomain + "#nodeType" + node1Type.getLocalName());
				result.add(subject, RDF.type, node1Type);
			} else {
				subject = result.createResource();
			}
			Resource object = null;
			if(node2 != null) {
				object = node2;
			} else if(node2Type != null) {
				object = result.createResource(baseDomain + "nodeType/#" + node2Type.getLocalName());
				result.add(object, RDF.type, node2Type);
			} else {
				object = result.createResource(baseDomain + "nodeRelation/#" + relation1.getLocalName());
			}
			result.add(subject, relation1, object);
		}
		if(relation2 != null) {
			Resource subject = null;
			if(node2 != null) {
				subject = node2;
			} else if(node2Type != null) {
				subject = result.createResource(baseDomain + "#nodeType" + node2Type.getLocalName());
				result.add(subject, RDF.type, node2Type);
			} else if(relation1 != null) {
				subject = result.createResource(baseDomain + "nodeRelation/#" + relation1.getLocalName());
			} else {
				subject = result.createResource();
			}
			
			Resource object = null;
			if(node3 != null) {
				object = node3;
			} else if(node3Type != null) {
				object = result.createResource(baseDomain + "nodeType/#" + node3Type.getLocalName());
				result.add(object, RDF.type, node3Type);
			} else {
				object = result.createResource(baseDomain + "nodeRelation/#" + relation2.getLocalName());
			}
			
			result.add(subject, relation2, object);
		}
		if(relation3 != null) {
			Resource subject = null;
			if(node3 != null) {
				subject = node3;
			} else if(node3Type != null) {
				subject = result.createResource(baseDomain + "nodeType/#" + node3Type.getLocalName());
				result.add(subject, RDF.type, node3Type);
			} else if(relation2 != null) {
				subject = result.createResource(baseDomain + "nodeRelation/#" + relation2.getLocalName());
			} else {
				subject = result.createResource();
			}
			
			Resource object = null;
			if(node4 != null) {
				object = node4;
			} else if(node4Type != null) {
				object = result.createResource(baseDomain + "nodeType/#" + node4Type.getLocalName());
				result.add(object, RDF.type, node4Type);
			} else {
				object = result.createResource(baseDomain + "nodeRelation/#" + relation3.getLocalName());
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
				subject = result.createResource(baseDomain + "nodeType/#" + node4Type.getLocalName());
				result.add(subject, RDF.type, node4Type);
			} else if(relation3 != null) {
				subject = result.createResource(baseDomain + "nodeRelation/#" + relation3.getLocalName());
			} else {
				subject = result.createResource();
			}
			
			Resource object = null;
			if(node5 != null) {
				object = node5;
			} else if(node5Type != null) {
					object = result.createResource(baseDomain + "nodeType/#" + node5Type.getLocalName());
					result.add(object, RDF.type, node5Type);
			} else {
				object = result.createResource(baseDomain + "nodeRelation/#" + relation4.getLocalName());
			}
			result.add(subject, relation4, object);
		}

		result.add(result.createLiteralStatement(mainRes, result.createProperty(baseDomain+"property/support"), liSet.getCount()));
		if(this.getTotalTransactions() != 0) {
			result.add(result.createLiteralStatement(mainRes, result.createProperty(baseDomain+"property/support"), (double)liSet.getCount() / (double)this.getTotalTransactions() ));
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

	public long getTotalTransactions() {
		return totalTransactions;
	}

	public void setTotalTransactions(long totalTransactions) {
		this.totalTransactions = totalTransactions;
	}

}
