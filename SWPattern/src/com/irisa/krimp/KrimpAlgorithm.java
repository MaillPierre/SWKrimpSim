package com.irisa.krimp;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.ItemsetSet;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

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

}
