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

public class KrimpSlimAlgorithmExperimental extends AbstractKrimpSlimAlgorithm {
	
	
	private static boolean moreCandidates = true; 
	
	private static Logger logger = Logger.getLogger(KrimpSlimAlgorithmExperimental.class);
	
	private LinkedList<Couple<KItemset, KItemset>> _topKCandidates = new LinkedList<Couple<KItemset, KItemset>>();

	public KrimpSlimAlgorithmExperimental(ItemsetSet transactions) {
		super(transactions);
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
		_numberOfUsedCandidates = 0;
		
		KItemset candidate = generateCandidate(result, standardSize, testedCandidates);
		while(candidate != null) {
			_numberOfUsedCandidates++;
			testedCandidates.add(candidate);
//			logger.debug("Trying to add: "+candidate);
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
					moreCandidates = true; 
				}
			}
			candidate = generateCandidate(result, standardSize, testedCandidates);
		}
		logger.debug("KRIMP algorithm ended");
		return result;
	}

	/**
	 * 
	 * @param codetable
	 * @param standardSize
	 * @param testedCandidates
	 * @return
	 */
	protected KItemset generateCandidate(final CodeTable refCode, double standardSize , HashSet<KItemset> testedCandidates) {
		
		int maxNumberofCandidates = 100;
		final CodeTableSlim codetable = new CodeTableSlim(refCode);
		
		Comparator<KItemset> usageComparator = new Comparator<KItemset>(){
			@Override
			public int compare(KItemset o1, KItemset o2) {
				return - Integer.compare(codetable.getUsage(o1), codetable.getUsage(o2));
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
	
		while(! this._topKCandidates.isEmpty() || moreCandidates) {
			if(! this._topKCandidates.isEmpty()) {
				
//				logger.debug("Trying with top "+ this._topKCandidates.size() +" candidates");
				KItemset tmpX = this._topKCandidates.peekFirst().getFirst();
				KItemset tmpY = this._topKCandidates.peekFirst().getSecond();
//				logger.debug("First:" +codetable.estimateUsageCombination(_topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond()));
//				logger.debug("Last:" +codetable.estimateUsageCombination(_topKCandidates.peekLast().getFirst(), _topKCandidates.peekLast().getSecond()));
				if(codetable.estimateUsageCombination(tmpX, tmpY) > 0) {
					KItemset candidate = new KItemset(tmpX);
					candidate.addAll(this._topKCandidates.peekFirst().getSecond());
					this._topKCandidates.removeFirst();
					return candidate;
				} else {
//					logger.debug("Removed top K " + this._topKCandidates.getFirst());
					this._topKCandidates.removeFirst();
				}
			}
			
			if(moreCandidates){
//				logger.debug("Generating more candidates");
//				TreeSet<Couple<KItemset, KItemset>> newCandidates = new TreeSet<Couple<KItemset, KItemset>>(gainComparator);
				
				moreCandidates = false;
				double maxSeenCriteria = 0.0;
				int maxSeenUsage = 0;
//				logger.debug("start candidates "+_topKCandidates.size()); 
				for(int iX = 0; iX < codes.size(); iX++) {
					
					if (_topKCandidates.size() == maxNumberofCandidates) {
						int combiUsage = codetable.estimateUsageCombination(_topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
						if(getCandidateStrategy() == CANDIDATE_STRATEGY.USAGE) {
							maxSeenCriteria = combiUsage;
						} else if(getCandidateStrategy() == CANDIDATE_STRATEGY.GAIN) {
							maxSeenCriteria = deltaSize(codetable, standardSize, _topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
						}
						maxSeenUsage = combiUsage;
					}
					else {
						maxSeenCriteria = 0.0; 
					}
					
					KItemset tmpX = codes.get(iX);
					int currentXUsage = codetable.getUsage(tmpX);
					
					if (currentXUsage <= maxSeenCriteria) {
						// it will not provide any good candidate by union usage(XUY)<=usage(X)
						moreCandidates = (currentXUsage >0); 
						break;
					}
			
					// we skip the step of the minimum support SHould we add it? 
					
					for(int iY = iX+1; iY < codes.size(); iY++) {
						
						if (_topKCandidates.size() == maxNumberofCandidates) {
							int combiUsage = codetable.estimateUsageCombination(_topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
							if(getCandidateStrategy() == CANDIDATE_STRATEGY.USAGE) {
								maxSeenCriteria = combiUsage;
							} else if(getCandidateStrategy() == CANDIDATE_STRATEGY.GAIN) {
								maxSeenCriteria = deltaSize(codetable, standardSize, _topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
							}
							maxSeenUsage = combiUsage;
						}
						else {
							maxSeenCriteria = 0; 
						}
						
						KItemset tmpY = codes.get(iY);
						int currentYUsage = codetable.getUsage(tmpY); 
						
						if (currentYUsage <= maxSeenUsage) {
							moreCandidates = (currentYUsage>0); 
							break; 
						}
						
						// we estimate this candidate 
						
						int candidateUsage = codetable.estimateUsageCombination(tmpX, tmpY);
						
						if (( this.getCandidateStrategy() == CANDIDATE_STRATEGY.USAGE && candidateUsage >= maxSeenCriteria) 
								|| ( this.getCandidateStrategy() == CANDIDATE_STRATEGY.GAIN && deltaSize(codetable, standardSize, tmpX, tmpY) >= maxSeenCriteria)) {
							if (_topKCandidates.size() == maxNumberofCandidates) {
								moreCandidates = true; 								
								Couple<KItemset, KItemset> last = _topKCandidates.pollLast(); 
//								logger.debug("getting rid of element with "+codetable.estimateUsageCombination(last.getFirst(),last.getSecond()) +" when min: "+minSeenUsage);
								
							}
							
							if (_topKCandidates.size() < maxNumberofCandidates) 
							{
//								logger.debug("adding an element with: "+codetable.estimateUsageCombination(tmpX, tmpY));
								
								if (candidateUsage > 0) 
									_topKCandidates.add(new Couple<KItemset, KItemset>(tmpX, tmpY) ); 
							}
						}
					}
					
				}
//				logger.debug("end candidates "+_topKCandidates.size()); 
			}
		}
		
		return null;
	}

}
