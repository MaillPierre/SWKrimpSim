package com.irisa.krimp;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;

/**
 * KRIMP magic happens here !
 * @author pmaillot
 *
 */
public class KrimpAlgorithm {

	private static Logger logger = Logger.getLogger(KrimpAlgorithm.class);

	protected ItemsetSet _transactions = null;
	protected ItemsetSet _candidateCodes = null;


	public KrimpAlgorithm(ItemsetSet transactions, ItemsetSet candidates) {
		this._transactions = new ItemsetSet(transactions);
		this._candidateCodes = new ItemsetSet(candidates);
	}
	
	public int numberofUsedCandidates() {
		if(this._candidateCodes != null) {
			return this._candidateCodes.size();
		}
		return 0;
	}

	public CodeTable runAlgorithm(boolean pruning) throws LogicException {
		logger.debug("Starting KRIMP algorithm");
		logger.debug(this._transactions.size() + " transactions, " + this._candidateCodes.size() + " codes");

		CodeTable result = CodeTable.createStandardCodeTable( _transactions); // CT ←Standard Code Table(D)
		Collections.sort(_candidateCodes, CodeTable.standardCandidateOrderComparator); // Fo ←F in Standard Candidate Order
		double resultSize = result.totalCompressedSize();

//		logger.debug("CANDIDATE CODES");
//		logger.debug(_candidateCodes);
		
		Iterator<KItemset> itCandidates = this._candidateCodes.iterator();
		while(itCandidates.hasNext()) {
			KItemset candidate = itCandidates.next();
//			logger.debug("Trying to add: "+candidate);
			CodeTable tmpCT = new CodeTable(result);
			if(candidate.size() > 1 && ! tmpCT.contains(candidate)) { // F ∈ Fo \ I
				tmpCT.addCode(candidate); // CTc ←(CT ∪ F)in Standard Cover Order
				double candidateSize = tmpCT.totalCompressedSize();
//				logger.debug("candidateSize: "+candidateSize +" resultSize: "+resultSize); 
				if(candidateSize < resultSize) { // if L(D,CTc)< L(D,CT) then
//					logger.debug("--> Added:"+candidate);
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
//				logger.debug(candidate+ " skipped");
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
	protected ItemsetSet pruneSet(CodeTable inferior, CodeTable superior) {
		ItemsetSet pruneSet = new ItemsetSet();
		KItemset auxCode = null; 
		Iterator<KItemset> itInferiorCodes = inferior.codeIterator(); 
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
		KItemset pruneCandidate = null;
		
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
	
	protected KItemset findLowestUsageCode (ItemsetSet pSet, CodeTable CT) {
		Collections.sort(pSet, new Comparator<KItemset>() {
			@Override
			public int compare(KItemset arg0, KItemset arg1) {
				return Integer.compare(CT.getUsage(arg0), CT.getUsage(arg1));
			}
		});
		
		return pSet.getFirst(); 
	}

}
