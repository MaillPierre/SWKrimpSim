package com.irisa.swpatterns.krimp;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.jenautils.BaseRDF;
import com.irisa.jenautils.QueryResultIterator;
import com.irisa.jenautils.UtilOntology;
import com.irisa.jenautils.BaseRDF.MODE;
import com.irisa.swpatterns.FrequentItemSetExtractor;
import com.irisa.swpatterns.TransactionsExtractor;
import com.irisa.swpatterns.Utils;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.ItemsetSet;
import com.irisa.swpatterns.data.LabeledTransactions;
import com.irisa.swpatterns.exception.LogicException;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * KRIMP magic happens here !
 * @author pmaillot
 *
 */
public class KrimpAlgorithm {

	private static Logger logger = Logger.getLogger(KrimpAlgorithm.class);

	private ItemsetSet _transactions = null;
	private ItemsetSet _candidateCodes = null;


	public KrimpAlgorithm(ItemsetSet transactions, ItemsetSet candidates) {
		this._transactions = new ItemsetSet(transactions);
		this._candidateCodes = new ItemsetSet(candidates);
	}

	public CodeTable runAlgorithm(boolean pruning) throws LogicException {
		logger.debug("Starting KRIMP algorithm");
		logger.debug(this._transactions.size() + " transactions, " + this._candidateCodes.size() + " codes");

		CodeTable result = CodeTable.createStandardCodeTable( _transactions); // CT ←Standard Code Table(D)
		Collections.sort(_candidateCodes, CodeTable.standardCandidateOrderComparator); // Fo ←F in Standard Candidate Order
		double resultSize = result.totalCompressedSize();

//		logger.debug("CANDIDATE CODES");
//		logger.debug(_candidateCodes);
		
		Iterator<Itemset> itCandidates = this._candidateCodes.iterator();
		while(itCandidates.hasNext()) {
			Itemset candidate = itCandidates.next();
			logger.debug("Trying to add: "+candidate);
			CodeTable tmpCT = new CodeTable(result);
			if(candidate.size() > 1 && ! tmpCT.contains(candidate)) { // F ∈ Fo \ I
				tmpCT.addCode(candidate); // CTc ←(CT ∪ F)in Standard Cover Order
				double candidateSize = tmpCT.totalCompressedSize();
//				logger.debug("candidateSize: "+candidateSize +" resultSize: "+resultSize); 
				if(candidateSize < resultSize) { // if L(D,CTc)< L(D,CT) then
					logger.debug("--> Added:"+candidate);
					if (!pruning) {
						result = tmpCT;
						resultSize = candidateSize;
					} else {
						result = postAcceptancePruning(tmpCT, result);
						// we have to update the size 
						resultSize = result.totalCompressedSize(); 
					}										
				}
			}
			else {
				logger.debug(candidate+ " skipped");
			}
		}
		logger.debug("KRIMP algorithm ended");
		return result;
	}
	
	/**
	 * PruneSet ←{X ∈ CTinferior | usageCTinferior (X)< usageCTsuperior(X)}
	 * @param inferior
	 * @param superior
	 * @return
	 */
	private ItemsetSet pruneSet(CodeTable inferior, CodeTable superior) {
		ItemsetSet pruneSet = new ItemsetSet();
		Itemset auxCode = null; 
		Iterator<Itemset> itInferiorCodes = inferior.codeIterator(); 
		// Iterator<Itemset> itCodes = previousTable.codeIterator(); 
		while (itInferiorCodes.hasNext()) {
			auxCode = itInferiorCodes.next(); 
			if (auxCode.size() >1 ) {
				if (inferior.getUsage(auxCode) < superior.getUsage(auxCode) ) {
					pruneSet.addItemset(auxCode);
				}
			}
		}
		return pruneSet;
	}
	
	public CodeTable postAcceptancePruning(CodeTable candidateTable, CodeTable previousTable) throws LogicException {
		
		// CB: after the acceptance of the new code
		// first we have to get the PruneSet => those codes whose usage has become lower 
		// after adding the candidates
		ItemsetSet pruneSet = pruneSet(candidateTable, previousTable);
		

		// names are taken from the paper 
		CodeTable CTc = new CodeTable(candidateTable);
		double CTcSize = -1; 
		CodeTable CTp = null;
		double CTpSize = -1; 
		Itemset pruneCandidate = null;
		
		CTcSize = CTc.totalCompressedSize(); 
		while (!pruneSet.isEmpty()) {
			pruneCandidate = findLowestUsageCode (pruneSet, CTc);		
			pruneSet.remove(pruneCandidate); 
			CTp = new CodeTable(CTc); 
			CTp.removeCode(pruneCandidate);
			CTpSize = CTp.totalCompressedSize(); 
			if (CTpSize < CTcSize) {
				pruneSet = pruneSet(CTp, CTc);
				CTc = CTp; 
				CTcSize = CTpSize; 
			}			
		}
		return CTc; 
	}
	
	private Itemset findLowestUsageCode (ItemsetSet pSet, CodeTable CT) {
//		Itemset result = null;
//		Itemset auxCode = null; 
//		Iterator<Itemset> codes = pSet.iterator();
//		if (codes.hasNext()) {
//			result = codes.next(); 
//		}
//		while (codes.hasNext()) {
//			auxCode = codes.next(); 
//			if (CT.getUsage(auxCode) < CT.getUsage(result)) {
//				result = auxCode; 
//			}
//		}
		Collections.sort(pSet, new Comparator<Itemset>() {
			@Override
			public int compare(Itemset arg0, Itemset arg1) {
				return Integer.compare(CT.getUsage(arg0), CT.getUsage(arg1));
			}
		});
		
		return pSet.getFirst(); 
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

		// Setting up options
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		options.addOption("file", true, "RDF file");
		options.addOption("otherFile", true, "Other RDF file");
		options.addOption("endpoint", true, "Endpoint adress");
		options.addOption("output", true, "Output csv file");
		options.addOption("limit", true, "Limit to the number of individuals extracted");
		options.addOption("resultWindow", true, "Size of the result window used to query servers.");
		options.addOption("classPattern", true, "Substring contained by the class uris.");
		options.addOption("noOut", false, "Not taking OUT properties into account.");
		options.addOption("noIn", false, "Not taking IN properties into account.");
		options.addOption("noTypes", false, "Not taking TYPES into account.");
		options.addOption("FPClose", false, "Use FPClose algorithm. (default)");
		options.addOption("FPMax", false, "Use FPMax algorithm.");
		options.addOption("FIN", false, "Use FIN algorithm.");
		options.addOption("Relim", false, "Use Relim algorithm.");
		options.addOption("class", true, "Class of the studied individuals.");
		options.addOption("rank1", false, "Extract informations up to rank 1 (types, out-going and in-going properties and object types), default is only types, out-going and in-going properties.");
		options.addOption("transactionFile", false, "Only create a .dat transaction for each given file.");
		options.addOption("path", true, "Extract paths of length N.");
		options.addOption("help", false, "Display this help.");
		options.addOption("inputTransaction", true, "Transaction file (RDF data will be ignored).");
		// added for pruning 
		options.addOption("pruning", false, "Activate post-acceptance pruning"); 

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
				ItemsetSet realtransactions ;
				Itemsets codes = null;
				
				boolean activatePruning = false; 

				String filename = cmd.getOptionValue("file");
				String otherFilename = cmd.getOptionValue("otherFile");
				String endpoint = cmd.getOptionValue("endpoint"); 
				String output = cmd.getOptionValue("output"); 
				String limitString = cmd.getOptionValue("limit");
				String resultWindow = cmd.getOptionValue("resultWindow");
				String classRegex = cmd.getOptionValue("classPattern");
				String className = cmd.getOptionValue("class");
				String pathOption = cmd.getOptionValue("path");
				converter.setNoTypeTriples( cmd.hasOption("noTypes") || converter.noTypeTriples());
				converter.noInTriples(cmd.hasOption("noIn") || converter.noInTriples());
				converter.setNoOutTriples(cmd.hasOption("noOut") || converter.noOutTriples());
				
				activatePruning = cmd.hasOption("pruning"); 
				
				if(cmd.hasOption("FPClose")) {
					fsExtractor.setAlgoFPClose();
				}
				if(cmd.hasOption("FPMax")) {
					fsExtractor.setAlgoFPMax();
				}
				if(cmd.hasOption("FIN")) {
					fsExtractor.setAlgoFIN();
				}
				if(cmd.hasOption("Relim")) {
					fsExtractor.setAlgoRelim();
				}
				converter.setRankOne(cmd.hasOption("rank1") || converter.isRankOne());
				logger.debug("output: " + output + " limit:" + limitString + " resultWindow:" + resultWindow + " classpattern:" + classRegex + " noType:" + converter.noTypeTriples() + " noOut:" + converter.noOutTriples() + " noIn:"+ converter.noInTriples());
				logger.debug("Pruning activated: "+activatePruning);
			
				
				if(!cmd.hasOption("inputTransaction")) {
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

					LabeledTransactions transactions;

					if(cmd.hasOption("class")) {
						Resource classRes = onto.getModel().createResource(className);
						transactions = converter.extractTransactionsForClass(baseRDF, onto, classRes);
					} else if(cmd.hasOption("path")) {
						transactions = converter.extractPathAttributes(baseRDF, onto);
					} else {
						transactions = converter.extractTransactions(baseRDF, onto);
					}

					AttributeIndex index = converter.getIndex();

					// Printing transactions
					if(cmd.hasOption("transactionFile")) {
						index.printTransactionsItems(transactions, filename + ".dat");

						if(cmd.hasOption("otherFile")) {
							index.printTransactionsItems(transactions, otherFilename + ".dat");
						}
					}

					realtransactions = index.convertToTransactions(transactions);
					codes = fsExtractor.computeItemsets(transactions, index);
					logger.debug("Nb Lines: " + realtransactions.size());

					if(cmd.hasOption("transactionFile")) {
						index.printTransactionsItems(transactions, filename + ".dat");
					}
					logger.debug("Nb items: " + converter.getIndex().size());

					baseRDF.close();

				} else {
					realtransactions = new ItemsetSet(Utils.readTransactionFile(cmd.getOptionValue("inputTransaction")));
					codes = fsExtractor.computeItemsets(realtransactions);
					logger.debug("Nb Lines: " + realtransactions.size());
				}
				ItemsetSet realcodes = new ItemsetSet(codes);

				try {
					CodeTable standardCT = CodeTable.createStandardCodeTable(realtransactions );

					KrimpAlgorithm kAlgo = new KrimpAlgorithm(realtransactions, realcodes);
					CodeTable krimpCT = kAlgo.runAlgorithm(activatePruning);
					double normalSize = standardCT.totalCompressedSize();
					double compressedSize = krimpCT.totalCompressedSize();
					logger.debug("-------- FIRST RESULT ---------");
					logger.debug(krimpCT);
					//					logger.debug("First Code table: " + krimpCT);
					logger.debug("First NormalLength: " + normalSize);
					logger.debug("First CompressedLength: " + compressedSize);
					logger.debug("First Compression: " + (compressedSize / normalSize));


					if(cmd.hasOption("otherFile")) {

						LabeledTransactions otherTransactions;
						if(cmd.hasOption("class")) {
							Resource classRes = onto.getModel().createResource(className);
							otherTransactions = converter.extractTransactionsForClass(new BaseRDF(otherFilename, MODE.LOCAL),  onto, classRes);
						} else if(cmd.hasOption("path")) {
							otherTransactions = converter.extractPathAttributes(new BaseRDF(otherFilename, MODE.LOCAL),  onto);
						} else {
							otherTransactions = converter.extractTransactions(new BaseRDF(otherFilename, MODE.LOCAL),  onto);
						}

						ItemsetSet otherRealTransactions = converter.getIndex().convertToTransactions(otherTransactions);

						logger.debug("Equals ? " + realtransactions.equals(otherRealTransactions));

						standardCT = CodeTable.createStandardCodeTable(/*index,*/ otherRealTransactions );
						CodeTable otherResult = new CodeTable( otherRealTransactions, krimpCT.getCodes());
						double otherNormalSize = standardCT.totalCompressedSize();
						double otherCompressedSize = otherResult.totalCompressedSize();
						//					logger.debug("First Code table: " + krimpCT);
						logger.debug("Second NormalLength: " + otherNormalSize);
						logger.debug("Second CompressedLength: " + otherCompressedSize);
						logger.debug("Second Compression: " + (otherCompressedSize / otherNormalSize));

						logger.debug("-------- OTHER RESULT ---------");
						logger.debug(otherResult);


						if(cmd.hasOption("transactionFile")) {
							converter.getIndex().printTransactionsItems(otherTransactions, otherFilename + ".dat");
						}
					}

				} catch (Exception e) {
					logger.fatal("RAAAH", e);
				}

			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		onto.close();
	}

}
