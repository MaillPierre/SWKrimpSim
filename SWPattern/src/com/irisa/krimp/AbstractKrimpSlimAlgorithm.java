package com.irisa.krimp;

import java.util.HashSet;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.jenautils.Couple;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

public abstract class AbstractKrimpSlimAlgorithm extends KrimpAlgorithm {
	private static Logger logger = Logger.getLogger(AbstractKrimpSlimAlgorithm.class);
	protected int _numberOfUsedCandidates = 0;
	protected CANDIDATE_STRATEGY _strat = CANDIDATE_STRATEGY.USAGE;
	protected LinkedList<Couple<KItemset, KItemset>> _topKCandidates = new LinkedList<Couple<KItemset, KItemset>>();
	
	public enum CANDIDATE_STRATEGY {
		USAGE,
		GAIN
	}
	
	public CANDIDATE_STRATEGY getCandidateStrategy() {
		return this._strat;
	}
	
	public void setCandidateStrategy(CANDIDATE_STRATEGY strat) {
		this._strat = strat;
	}
	
	@Override
	public int numberofUsedCandidates() {
		return this._numberOfUsedCandidates;
	}

	public AbstractKrimpSlimAlgorithm(ItemsetSet transactions) {
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
		_numberOfUsedCandidates = 0;

		CodeTable result = CodeTable.createStandardCodeTable( _transactions); // CT ←Standard Code Table(D)
		double resultSize = result.totalCompressedSize();
		double standardSize = resultSize;
		HashSet<KItemset> testedCandidates = new HashSet<KItemset>();
		
		KItemset candidate = generateCandidate(result, standardSize, testedCandidates);
		while(candidate != null) {
			_numberOfUsedCandidates++;
			testedCandidates.add(candidate);
//			logger.debug("Trying to add: "+candidate);
			CodeTable tmpCT = new CodeTable(result);
			if(candidate.size() > 1 && ! tmpCT.contains(candidate)) { // F ∈ Fo \ I
				tmpCT.addCode(candidate); // CTc ←(CT ∪ F)in Standard Cover Order
				double candidateSize = tmpCT.totalCompressedSize();
				if(candidateSize < resultSize) { // if L(D,CTc)< L(D,CT) then
					result = postAcceptancePruning(tmpCT, result);
					// we have to update the size 
					resultSize = result.totalCompressedSize(); 		
					
					if(result.contains(candidate)) {
						_topKCandidates.clear(); 		
					}
				}
			}
			candidate = generateCandidate(result, standardSize, testedCandidates);
		}
		logger.debug("KRIMP algorithm ended");
		return result;
	}
	
	protected abstract KItemset generateCandidate(final CodeTable refCode, double standardSize , final HashSet<KItemset> testedCandidates);

	double deltaSize(CodeTableSlim codetable, double standardSize, KItemset tmpX, KItemset tmpY) {
		int tmpCombiUsage = codetable.estimateUsageCombination(tmpX, tmpY);
		return deltaSize(codetable, standardSize, tmpX, tmpY, tmpCombiUsage);
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
