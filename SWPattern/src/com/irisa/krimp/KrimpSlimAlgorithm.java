package com.irisa.krimp;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.jenautils.Couple;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

public class KrimpSlimAlgorithm extends KrimpAlgorithm {
	
	private static Logger logger = Logger.getLogger(KrimpSlimAlgorithm.class);
	
	private LinkedList<Couple<KItemset, KItemset>> _topKCandidates = new LinkedList<Couple<KItemset, KItemset>>();

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
		
		KItemset candidate = generateCandidate(result, standardSize, testedCandidates);
		while(candidate != null) {
			testedCandidates.add(candidate);
			logger.debug("Trying to add: "+candidate);
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

	/**
	 * Generate candidates codes by:
	 * 1) ordering the codes by descending order of usage
	 * 2) test the combination of two different codes according to their gain as a new code, using b&b algorithm while keeping in memory the best gain found and the usage of the best candidate found
	 * 	i) if usage(X) or usage(Y) are inferior to best usage, cut the branch
	 *  ii) if size of the candidate is superior to max size of transaction, cut the branch
	 *  iii) if the candidate was previously tested, cut the branch
	 *  iv) 
	 * @param codetable
	 * @param standardSize
	 * @param testedCandidates
	 * @return
	 */
	private KItemset generateCandidate(CodeTableSlim codetable, double standardSize , HashSet<KItemset> testedCandidates) {
		double currentMaxGain = 0;
		double bestUsage = 0;
		
		boolean moreCandidates = true;
		while(! this._topKCandidates.isEmpty() || moreCandidates) {
			if(! this._topKCandidates.isEmpty()) {
				KItemset tmpX = this._topKCandidates.peekFirst().getFirst();
				KItemset tmpY = this._topKCandidates.peekFirst().getSecond();
				if(codetable.estimateUsageCombination(tmpX, tmpY) > 0) {
					KItemset candidate = this._topKCandidates.peekFirst().getFirst();
					candidate.addAll(this._topKCandidates.peekFirst().getSecond());
					this._topKCandidates.removeFirst();
					return candidate;
				} else {
					this._topKCandidates.removeFirst();
				}
			} else if(moreCandidates){
				moreCandidates = false;
				ItemsetSet codes = new ItemsetSet(codetable._codes);
				codes.sort(new Comparator<KItemset>(){
					@Override
					public int compare(KItemset o1, KItemset o2) {
						return Integer.compare(codetable.getUsage(o1), codetable.getUsage(o2));
					}
				});
				
				for(int iX = 0; iX < codetable._codes.size(); iX++) {
					KItemset tmpX = codetable._codes.get(iX);
					int currentXUsage = codetable.getUsage(tmpX);
					if(currentXUsage > 0 && currentXUsage >= bestUsage) {
						for(int iY = iX+1; iY < codetable._codes.size(); iY++) {
							KItemset tmpY = codetable._codes.get(iY);
							int currentYUsage = codetable.getUsage(tmpY);
							if(currentYUsage > 0 && currentYUsage >= bestUsage) {
								double deltaSize = evaluateGainCandidate(codetable, standardSize, tmpX, tmpY, currentMaxGain, testedCandidates);
								if( deltaSize > currentMaxGain) {
									moreCandidates = true;
									this._topKCandidates.addFirst(new Couple<KItemset, KItemset>(tmpX, tmpY));
									bestUsage = currentYUsage;
									currentMaxGain = deltaSize;
								}
							} else {
								break;
							}
						}
					}
				}
			}
		}
		
		logger.debug("No candidate proposed");
		return null;
	}
	
	private double evaluateGainCandidate(CodeTableSlim codetable, double standardSize, KItemset tmpX, KItemset tmpY, double maxGain, HashSet<KItemset> testedCandidates) {
		KItemset candidatePotential = new KItemset(tmpX);
		candidatePotential.addAll(tmpY);
		if(candidatePotential.size() <= codetable._index.getMaxSize()/* && result._index.getCodeSupport(candidatePotential) > 0*/) {
			if( ! testedCandidates.contains(candidatePotential)
					&& candidatePotential.size() > 1) {
				int tmpCombiUsage = codetable.estimateUsageCombination(tmpX, tmpY); // xy'
				if(tmpCombiUsage > 0) {
					double deltaD = deltaDCTModified(codetable, standardSize, tmpX, tmpY, tmpCombiUsage);
					double deltaCT = deltaCTModifiedD(codetable, standardSize, tmpX, tmpY, tmpCombiUsage);
					double deltaSize = (deltaD + deltaCT);
					//logger.debug("Testing potential candidate "+ candidatePotential +" deltaD: " + deltaD + " deltaCT: " + deltaCT);
					if(deltaSize > maxGain ) {
						return deltaSize;
					}
				}
			}
		}
		return maxGain;
	}

	private double deltaDCTModified(CodeTableSlim codetable, double standardSize, KItemset x, KItemset y, int tmpCombiUsage) {
			int currentYUsage = codetable.getUsage(y); // y
			int currentXUsage = codetable.getUsage(x); // x
			KItemset candidatePotential = new KItemset(x);
			candidatePotential.addAll(y);
			long currentSumUsages = codetable._usageTotal; // s
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
	
			double result = 0;
			if(currentSumUsages != 0) {
				result += currentSumUsages*Math.log(currentSumUsages);
			}
			if(tmpSumUsages != 0) {
				result -= tmpSumUsages*Math.log(tmpSumUsages);
			}
			if(tmpCombiUsage != 0) {
				result += tmpCombiUsage*Math.log(tmpCombiUsage);
			}
			result -= sum;
//			logger.debug("currentSumUsages: " + currentSumUsages + " tmpSumUsages: " + tmpSumUsages + " tmpCombiUsage: " + tmpCombiUsage + " sum: " + sum);
	
			return result;
	}

	private double deltaCTModifiedD(CodeTableSlim codetable, double standardSize, KItemset x, KItemset y, int tmpCombiUsage) {
			int currentYUsage = codetable.getUsage(y); // y
			int currentXUsage = codetable.getUsage(x); // x
			double standardXlength = codetable.codeLengthOfCodeAccordingST(x);
			double standardYlength = codetable.codeLengthOfCodeAccordingST(y);
			KItemset candidatePotential = new KItemset(x);
			candidatePotential.addAll(y);
			long currentSumUsages = codetable._usageTotal; // s
			long tmpSumUsages = currentSumUsages - tmpCombiUsage; // s'
			int tmpXUsage = currentXUsage - tmpCombiUsage; // x'
			int tmpYUsage = currentYUsage - tmpCombiUsage; // y'
			double newCodeStandardLength = codetable.codeLengthOfCodeAccordingST(candidatePotential); // L(XUY | ST)
	
			double sum1Usage = 0;
			double sum2UsageLength = 0;
			double sum3LengthUsage = 0;
			double logX = 0;
			if(currentXUsage != 0) {
				logX = Math.log(currentXUsage);
			}
			double logY = 0;
			if(currentYUsage != 0) {
				logY = Math.log(currentYUsage);
			}
			double logXprime = 0;
			if(tmpXUsage != 0) {
				logXprime = Math.log(tmpXUsage);
			}
			double logYprime = 0;
			if(tmpYUsage != 0) {
				logYprime = Math.log(tmpYUsage);
			}
			if(currentXUsage != 0 && tmpXUsage != 0 && currentXUsage != tmpXUsage) {
				sum1Usage += logXprime - logX;
			}
			if(currentYUsage != 0 && tmpYUsage != 0 && currentYUsage != tmpYUsage) {
				sum1Usage += logYprime - logY;
			}
			
			if(currentXUsage == 0 && currentXUsage != tmpXUsage) {
				sum2UsageLength += logXprime - standardXlength;
			}
			if(currentYUsage == 0 && currentYUsage != tmpYUsage) {
				sum2UsageLength += logYprime - standardYlength;
			}
			
			if(tmpXUsage == 0 && currentXUsage != tmpXUsage) {
				sum3LengthUsage += standardXlength - logX;
			}
			if(tmpYUsage == 0 && currentYUsage != tmpYUsage) {
				sum3LengthUsage += standardYlength - logY;
			}
	
			double result = 0;
			if(tmpCombiUsage > 0) {
				result += Math.log(tmpCombiUsage);
			}
			result -= newCodeStandardLength;
			if(currentSumUsages != 0) {
				result += codetable.getCodes().size()*Math.log(currentSumUsages);
			}
			if(tmpSumUsages != 0) {
				result -= (codetable.getCodes().size()+1)*Math.log(tmpSumUsages);
			}
			result += sum1Usage + sum2UsageLength + sum3LengthUsage;
			return result;
	}

}
