package com.irisa.krimp;

import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

public class KrimpSlimAlgorithm extends KrimpAlgorithm {
	private static Logger logger = Logger.getLogger(KrimpSlimAlgorithm.class);

	public KrimpSlimAlgorithm(ItemsetSet transactions) {
		super(transactions, new ItemsetSet());
	}


	public CodeTable runAlgorithm(boolean pruning) throws LogicException {
		logger.debug("Starting KRIMP SLIM algorithm");
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
	
	public CodeTable runAlgorithm() {
		return null;
		
	}

}
