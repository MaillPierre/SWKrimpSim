package com.irisa.krimp;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.jenautils.Couple;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

public class KrimpSlimAlgorithmExperimental extends KrimpAlgorithm {
	
	private static Logger logger = Logger.getLogger(KrimpSlimAlgorithmExperimental.class);
	
	private LinkedList<Couple<KItemset, KItemset>> _topKCandidates = new LinkedList<Couple<KItemset, KItemset>>();

	public KrimpSlimAlgorithmExperimental(ItemsetSet transactions) {
		super(transactions, new ItemsetSet());
	}

	/**
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * All the time and lack of precision make SLIM a dull algorithm
	 * @return
	 * @throws LogicException
	 */
	public CodeTable runAlgorithm() throws LogicException {
		logger.debug("Starting KRIMP SLIM algorithm");
		logger.debug(this._transactions.size() + " transactions");

		CodeTable result = CodeTable.createStandardCodeTable( _transactions); // CT ←Standard Code Table(D)
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
					result = postAcceptancePruning(tmpCT, result);
					// we have to update the size 
					resultSize = result.totalCompressedSize(); 		
					
					_topKCandidates.clear(); 
				}
			}
			candidate = generateCandidate(result, standardSize, testedCandidates);
		}
		logger.debug("KRIMP algorithm ended");
		return result;
	}
	
	public CodeTable postAcceptancePruning(CodeTable candidateTable, CodeTableSlim previousTable) throws LogicException {
		
		// CB: after the acceptance of the new code
		// first we have to get the PruneSet => those codes whose usage has become lower 
		// after adding the candidates
		ItemsetSet pruneSet = pruneSet(candidateTable, previousTable);
		

		// names are taken from the paper 
		CodeTable CTc = candidateTable;
		double CTcSize = -1; 
		CodeTableSlim CTp = null;
		double CTpSize = -1; 
		KItemset pruneCandidate = null;
		
		CTcSize = CTc.totalCompressedSize(); 
		while (!pruneSet.isEmpty()) {
			pruneCandidate = findLowestUsageCode (pruneSet, CTc);		
			pruneSet.remove(pruneCandidate); 
			CTp = new CodeTableSlim(CTc); 
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

	/**
	 * 
	 * @param codetable
	 * @param standardSize
	 * @param testedCandidates
	 * @return
	 */
	private KItemset generateCandidate(final CodeTable refCode, double standardSize , HashSet<KItemset> testedCandidates) {
		
		int maxNumberofCandidates = 10;
		final CodeTableSlim codetable = new CodeTableSlim(refCode);
		
		Comparator<Couple<KItemset, KItemset>> gainComparator = new Comparator<Couple<KItemset, KItemset>>(){
			@Override
			public int compare(Couple<KItemset, KItemset> o1, Couple<KItemset, KItemset> o2) {
				KItemset tmp1X = o1.getFirst();
				KItemset tmp1Y = o1.getSecond();
				int tmp1CombiUsage = codetable.estimateUsageCombination(tmp1X, tmp1Y);
				KItemset tmp2X = o2.getFirst();
				KItemset tmp2Y = o2.getSecond();
				int tmp2CombiUsage = codetable.estimateUsageCombination(tmp2X, tmp2Y);
				return Double.compare(deltaSize(codetable, standardSize, tmp1X, tmp1Y, tmp1CombiUsage), deltaSize(codetable, standardSize, tmp2X, tmp2Y, tmp2CombiUsage));
			}
		};
		Comparator<KItemset> usageComparator = new Comparator<KItemset>(){
			@Override
			public int compare(KItemset o1, KItemset o2) {
				return Integer.compare(codetable.getUsage(o1), codetable.getUsage(o2));
			}
		};
		
		Predicate<KItemset> zeroUsagePredicate = new Predicate<KItemset>() {
			@Override
			public boolean test(KItemset fi) {
				return codetable.getUsage(fi) == 0;
			}
		};
		
		ItemsetSet codes = new ItemsetSet(codetable._codes);
		codes.removeIf(zeroUsagePredicate);
		codes.sort(usageComparator);
		
		boolean moreCandidates = true;
		while(! this._topKCandidates.isEmpty() || moreCandidates) {
			if(! this._topKCandidates.isEmpty()) {
				
				logger.debug("Trying with top "+ this._topKCandidates.size() +" candidates");
				KItemset tmpX = this._topKCandidates.peekFirst().getFirst();
				KItemset tmpY = this._topKCandidates.peekFirst().getSecond();
				if(codetable.estimateUsageCombination(tmpX, tmpY) > 0) {
					KItemset candidate = new KItemset(tmpX);
					candidate.addAll(this._topKCandidates.peekFirst().getSecond());
					this._topKCandidates.removeFirst();
					return candidate;
				} else {
					logger.debug("Removed top K " + this._topKCandidates.getFirst());
					this._topKCandidates.removeFirst();
				}
			}
			
			if(moreCandidates){
				logger.debug("Generating more candidates");
				TreeSet<Couple<KItemset, KItemset>> newCandidates = new TreeSet<Couple<KItemset, KItemset>>(gainComparator);
				
				moreCandidates = false;
				int minSeenUsage = 0;
				if(! _topKCandidates.isEmpty()) {
					minSeenUsage = codetable.estimateUsageCombination(_topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
				}
								
				for(int iX = 0; iX < codes.size(); iX++) {
					KItemset tmpX = codes.get(iX);
					int currentXUsage = codetable.getUsage(tmpX);
					if(currentXUsage > 0) {
						for(int iY = iX+1; iY < codes.size(); iY++) {
							KItemset tmpY = codes.get(iY);
							if(codetable.haveCommonSupport(tmpX, tmpY)) {
								int currentYUsage = codetable.getUsage(tmpY);
								if(currentYUsage > 0 /*&& currentYUsage >= bestUsage*/) {
									KItemset candidatePotential = new KItemset(tmpX);
									candidatePotential.addAll(tmpY);
									if(candidatePotential.size() <= codetable._index.getMaxSize()
											&& ! testedCandidates.contains(candidatePotential)
											&& ! this._topKCandidates.contains(new Couple<KItemset, KItemset>(tmpX, tmpY))
											&& candidatePotential.size() > 1) {
										int candidateUsage = codetable.estimateUsageCombination(tmpX, tmpY);
										if( candidateUsage > 0 /*&& deltaSize > currentMaxGain*/) {
											newCandidates.add(new Couple<KItemset, KItemset>(tmpX, tmpY));
											if(candidateUsage <= minSeenUsage) {
												moreCandidates = true;
											}
										}
									}
								}
							}
						}
					}
				}

				newCandidates.removeAll(_topKCandidates);
				this._topKCandidates.removeIf(new Predicate<Couple<KItemset, KItemset>>(){
					@Override
					public boolean test(Couple<KItemset, KItemset> t) {
						return codetable.estimateUsageCombination(t.getFirst(), t.getSecond()) > 0;
					}
				});
				this._topKCandidates.addAll(newCandidates);
				while(this._topKCandidates.size() > maxNumberofCandidates) {
					this._topKCandidates.sort(gainComparator);
					this._topKCandidates.removeLast();
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
					double deltaSize = deltaSize(codetable, standardSize, tmpX, tmpY, tmpCombiUsage);
					//logger.debug("Testing potential candidate "+ candidatePotential +" deltaD: " + deltaD + " deltaCT: " + deltaCT);
					if(deltaSize > maxGain ) {
						return deltaSize;
					}
				}
			}
		}
		return maxGain;
	}
	
	private double deltaSize(CodeTableSlim codetable, double standardSize, KItemset tmpX, KItemset tmpY, int tmpCombiUsage) {
		double deltaD = deltaDCTModified(codetable, standardSize, tmpX, tmpY, tmpCombiUsage);
		double deltaCT = deltaCTModifiedD(codetable, standardSize, tmpX, tmpY, tmpCombiUsage);
		return (deltaD + deltaCT);
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
