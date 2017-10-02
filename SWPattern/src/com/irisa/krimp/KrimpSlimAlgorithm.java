package com.irisa.krimp;

import java.util.Collections;
import java.util.Comparator;
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
		double currentMaxGain = 0;
		double bestUsage = 0;

		ItemsetSet codes = new ItemsetSet(result._codes);
		codes.sort(new Comparator<KItemset>(){
			@Override
			public int compare(KItemset o1, KItemset o2) {
				return Integer.compare(result.getUsageWithRefresh(o1), result.getUsageWithRefresh(o2));
			}
		});
		
		//Iterator<KItemset> itX = result.codeIterator();
		//while(itX.hasNext()) {
		for(int iX = 0; iX < result._codes.size(); iX++) {
			//KItemset tmpX = itX.next();
			KItemset tmpX = result._codes.get(iX);
			int currentXUsage = result.getUsage(tmpX);
			if(currentXUsage >= bestUsage) {
				//Iterator<KItemset> itY = result.codeIterator();
				//while(itY.hasNext()) {
				for(int iY = iX+1; iY < result._codes.size(); iY++) {
					//KItemset tmpY = itY.next();
					KItemset tmpY = result._codes.get(iY);
					int currentYUsage = result.getUsage(tmpY);
					if(tmpX != tmpY && currentYUsage >= bestUsage) {
						KItemset candidatePotential = new KItemset(tmpX);
						candidatePotential.addAll(tmpY);
						if(candidatePotential.size() <= result._index.getMaxSize()/* && result._index.getCodeSupport(candidatePotential) > 0*/) {
							if( ! testedCandidates.contains(candidatePotential)
									&& candidatePotential.size() > 1) {
								double deltaD = deltaDCTModified(result, standardSize, tmpX, tmpY);
								double deltaCT = deltaCTModifiedD(result, standardSize, tmpX, tmpY);
								double deltaSize = (deltaD + deltaCT);
								//logger.debug("Testing potential candidate "+ candidatePotential +" deltaD: " + deltaD + " deltaCT: " + deltaCT);
								if(deltaSize > currentMaxGain ) {
									x = tmpX;
									y = tmpY;
									bestUsage = currentYUsage;
									currentMaxGain = deltaSize;
								}
							}
						}
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

	private double deltaDCTModified(CodeTableSlim codetable, double standardSize, KItemset x, KItemset y) {
		int currentYUsage = codetable.getUsage(y); // y
		int currentXUsage = codetable.getUsage(x); // x
		KItemset candidatePotential = new KItemset(x);
		candidatePotential.addAll(y);
		long currentSumUsages = codetable._usageTotal; // s
		int tmpCombiUsage = codetable.estimateUsageCombination(x, y); // xy'
		long tmpSumUsages = currentSumUsages - tmpCombiUsage; // s'
		int tmpXUsage = currentXUsage - tmpCombiUsage; // x'
		int tmpYUsage = currentYUsage - tmpCombiUsage; // y'

		//		logger.debug("s: " + currentSumUsages + " x: " + currentXUsage + " y: " + currentYUsage + " xy': " + tmpCombiUsage + " s': " + tmpSumUsages + " x': " + tmpXUsage + " y': " + tmpYUsage);

		double sum = 0;
		// Taking only into account the code that are going to be modified, according to the practical observation of the paper, it is summed up to this:
		if(currentXUsage != tmpXUsage) {
			if(currentXUsage != 0) {
				sum += currentXUsage*Math.log(currentXUsage);
			}
			if(tmpXUsage != 0) {
				sum -= tmpXUsage*Math.log(tmpXUsage);
			}
		}
		if(currentYUsage != tmpYUsage) {
			if(currentYUsage != 0) {
				sum += currentYUsage*Math.log(currentYUsage);
			}
			if(tmpYUsage != 0) {
				sum -= tmpYUsage*Math.log(tmpYUsage);
			}
		}

		double result = currentSumUsages*Math.log(currentSumUsages) - tmpSumUsages*Math.log(tmpSumUsages) + tmpCombiUsage*Math.log(tmpCombiUsage) - sum;
		//		logger.debug("currentSumUsages: " + currentSumUsages + " tmpSumUsages: " + tmpSumUsages + " tmpCombiUsage: " + tmpCombiUsage + " sum: " + sum);

		return result;
	}

	private double deltaCTModifiedD(CodeTableSlim result, double standardSize, KItemset x, KItemset y) {
		int currentYUsage = result.getUsage(y); // y
		int currentXUsage = result.getUsage(x); // x
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
			if(tmpXUsage != currentXUsage) {
				if(currentXUsage != 0) {
					sum1Usage -= currentXUsage*Math.log(currentXUsage);
				}
				if(tmpXUsage != 0) {
					sum1Usage += tmpXUsage*Math.log(tmpXUsage);
					sum1UsageLength += Math.log(tmpXUsage);
					sum2LengthUsage -= Math.log(tmpXUsage);
				}
				sum1UsageLength -= result.codeLengthOfCodeAccordingST(x);
				sum2LengthUsage += result.codeLengthOfCodeAccordingST(x);
			}
			if(tmpYUsage != currentYUsage) {
				if(currentYUsage != 0) {
					sum1Usage -= currentXUsage*Math.log(currentYUsage);
				}
				if(tmpYUsage != 0) {
					sum1Usage += tmpXUsage*Math.log(tmpYUsage);
					sum1UsageLength += Math.log(tmpYUsage);
					sum2LengthUsage -= Math.log(tmpYUsage);
				}
				sum1UsageLength -= result.codeLengthOfCodeAccordingST(y);
				sum2LengthUsage += result.codeLengthOfCodeAccordingST(y);
			}
		}


		return Math.log(tmpCombiUsage) - newCodeStandardLength + result.getCodes().size()*Math.log(currentSumUsages) - (result.getCodes().size()+1)*Math.log(tmpSumUsages) + sum1Usage + sum1UsageLength + sum2LengthUsage;
	}

}
