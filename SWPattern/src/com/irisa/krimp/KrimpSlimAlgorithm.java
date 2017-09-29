package com.irisa.krimp;

import java.util.Collections;
import java.util.HashSet;
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


	public CodeTableSlim runAlgorithm() throws LogicException {
		logger.debug("Starting KRIMP SLIM algorithm");
		logger.debug(this._transactions.size() + " transactions");

		CodeTableSlim result = CodeTableSlim.createStandardCodeTable( _transactions); // CT ←Standard Code Table(D)
		double resultSize = result.totalCompressedSize();
		double standardSize = resultSize;
		HashSet<KItemset> testedCandidates = new HashSet<KItemset>();

//		logger.debug("CANDIDATE CODES");
//		logger.debug(_candidateCodes);

		KItemset candidate = generateCandidate(result, standardSize, testedCandidates);
		while(candidate != null) {
			testedCandidates.add(candidate);
//			logger.debug("Trying to add: "+candidate);
			CodeTableSlim tmpCT = new CodeTableSlim(result);
			if(candidate.size() > 1 && ! tmpCT.contains(candidate)) { // F ∈ Fo \ I
				tmpCT.addCode(candidate); // CTc ←(CT ∪ F)in Standard Cover Order
				double candidateSize = tmpCT.totalCompressedSize();
//				logger.debug("candidateSize: "+candidateSize +" resultSize: "+resultSize); 
				if(candidateSize < resultSize) { // if L(D,CTc)< L(D,CT) then
//					logger.debug("--> Added:"+candidate);
					result = new CodeTableSlim(postAcceptancePruning(tmpCT, result));
					// we have to update the size 
					resultSize = result.totalCompressedSize(); 				
				}
			}
			candidate = generateCandidate(result, standardSize, testedCandidates);
		}
		logger.debug("KRIMP algorithm ended");
		return result;
	}


	private KItemset generateCandidate(CodeTableSlim result, double standardSize , HashSet<KItemset> testedCandidates) {
		KItemset x = null;
		KItemset y = null;
		double currentSize = result.totalCompressedSize();
		double currentMaxGain = currentSize;
		
		Iterator<KItemset> itX = result.codeIterator();
		while(itX.hasNext()) {
			KItemset tmpX = itX.next();
			int currentXUsage = result.getUsageWithRefresh(tmpX);
				Iterator<KItemset> itY = result.codeIterator();
				while(itY.hasNext()) {
					KItemset tmpY = itY.next();
					KItemset candidatePotential = new KItemset(tmpX);
					candidatePotential.addAll(tmpY);
					int tmpCombiUsage = result.estimateUsageCombination(x, y); // xy'
					if( tmpCombiUsage > 0
						&& ! testedCandidates.contains(candidatePotential)
						&& candidatePotential.size() > 1) {
						double deltaD = deltaDCTModified(result, standardSize, tmpX, tmpY);
						double deltaCT = deltaCTModifiedD(result, standardSize, tmpX, tmpY);
						double deltaSize = (deltaD + deltaCT);
						//logger.debug("Testing potential candidates x: " + tmpX + " y: " + tmpY);
						if(deltaSize > currentMaxGain ) {
							x = tmpX;
							y = tmpY;
							currentMaxGain = deltaSize;
						}
					}
				}
		}
		
		if(x != null && y != null) {
			KItemset candidate = new KItemset(x);
			candidate.addAll(y);
			logger.debug("Proposed " + candidate + " as a candidate, gain: " + currentMaxGain /*+ " already tested: " + testedCandidates*/);
			return candidate;
		}
		logger.debug("No candidate proposed");
		return null;
	}
	
	private double deltaDCTModified(CodeTableSlim result, double standardSize, KItemset x, KItemset y) {
		int currentYUsage = result.getUsageWithRefresh(y); // y
		int currentXUsage = result.getUsageWithRefresh(x); // x
		KItemset candidatePotential = new KItemset(x);
		candidatePotential.addAll(y);
		long currentSumUsages = result._usageTotal; // s
		int tmpCombiUsage = result.estimateUsageCombination(x, y); // xy'
		long tmpSumUsages = currentSumUsages - tmpCombiUsage; // s'
		int tmpXUsage = currentXUsage - tmpCombiUsage; // x'
		int tmpYUsage = currentYUsage - tmpCombiUsage; // y'
		
		logger.debug("s: " + currentSumUsages + " x: " + currentXUsage + " y: " + currentYUsage + " xy': " + tmpCombiUsage + " s': " + tmpSumUsages + " x': " + tmpXUsage + " y': " + tmpYUsage);
		
		double sum = 0;
//		Iterator<KItemset> itCodes1 = result.codeIterator();
//		while(itCodes1.hasNext()) {
//			KItemset code1 = itCodes1.next();
//			int code1Usage = result.getUsageWithRefresh(code1);
//			Iterator<KItemset> itCodes2 = result.codeIterator();
//			while(itCodes2.hasNext()) {
//				KItemset code2 = itCodes2.next();
//				
//				if(code1 != code2) {
//					int code2Usage = result.getUsageWithRefresh(code2);
//					sum += code1Usage*Math.log(code1Usage) -code2Usage*Math.log(code2Usage);
//				}
//			}
//		}
		// Taking only into account the code that are going to be modified, according to the practical observation of the paper, it is summed up to this:
		sum += currentXUsage*Math.log(currentXUsage) - tmpXUsage*Math.log(tmpYUsage);
		sum += currentYUsage*Math.log(currentYUsage) - tmpYUsage*Math.log(tmpXUsage);
		
		return currentSumUsages*Math.log(currentSumUsages) - tmpSumUsages*Math.log(tmpSumUsages) + tmpCombiUsage*Math.log(tmpCombiUsage) - sum;
	}
	
	private double deltaCTModifiedD(CodeTableSlim result, double standardSize, KItemset x, KItemset y) {
		int currentYUsage = result.getUsageWithRefresh(y);
		int currentXUsage = result.getUsageWithRefresh(x);
		KItemset candidatePotential = new KItemset(x);
		candidatePotential.addAll(y);
		long currentSumUsages = result._usageTotal; // s
		int tmpCombiUsage = result.estimateUsageCombination(x, y); // xy'
		long tmpSumUsages = currentSumUsages - tmpCombiUsage; // s'
		int tmpXUsage = currentXUsage - tmpCombiUsage; // x'
		int tmpYUsage = currentYUsage - tmpCombiUsage; // y'
		double newCodeStandardLength = result.codeLengthOfCodeAccordingST(candidatePotential); // L(XUY | ST)

		double sum1Usage = 0;
		double sum1UsageLength = 0;
		double sum2LengthUsage = 0;
		if(currentXUsage != 0 && currentYUsage != 0 && tmpXUsage != 0 && tmpYUsage != 0) {
			sum1Usage += tmpXUsage*Math.log(tmpXUsage) - currentXUsage*Math.log(currentYUsage);
			sum1Usage += tmpXUsage*Math.log(tmpYUsage) - currentXUsage*Math.log(currentXUsage);
		
			sum1UsageLength += Math.log(tmpXUsage) - result.codeLengthOfcode(y);
			sum1UsageLength += Math.log(tmpYUsage) - result.codeLengthOfcode(x);

			sum2LengthUsage += result.codeLengthOfcode(x) - Math.log(tmpYUsage);
			sum2LengthUsage += result.codeLengthOfcode(y) - Math.log(tmpXUsage);
		}
		
		
		return Math.log(tmpCombiUsage) - newCodeStandardLength + result.getCodes().size()*Math.log(currentSumUsages) - (result.getCodes().size()+1)*Math.log(tmpSumUsages) + sum1Usage + sum1UsageLength + sum2LengthUsage;
	}

}
