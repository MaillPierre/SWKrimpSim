package com.irisa.krimp;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.jenautils.Couple;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

public class KrimpSlimAlgorithm extends AbstractKrimpSlimAlgorithm {
	
	private static Logger logger = Logger.getLogger(KrimpSlimAlgorithm.class);
	
	private int _maxNumberofCandidates = 1000;
	private boolean _moreCandidates = true;

	public KrimpSlimAlgorithm(ItemsetSet transactions) {
		super(transactions);
	}

	/**
	 * 
	 * @param codetable
	 * @param standardSize
	 * @param testedCandidates
	 * @return
	 */
	protected KItemset generateCandidate(final CodeTable refCode, double standardSize , final HashSet<KItemset> testedCandidates) {
		
		if(! this._topKCandidates.isEmpty() || _moreCandidates) {
			final CodeTableSlim codetable = new CodeTableSlim(refCode);
			
			Comparator<Couple<KItemset, KItemset>> gainComparator = new Comparator<Couple<KItemset, KItemset>>(){
				@Override
				public int compare(Couple<KItemset, KItemset> o1, Couple<KItemset, KItemset> o2) {
					KItemset tmp1X = o1.getFirst();
					KItemset tmp1Y = o1.getSecond();
					KItemset tmp2X = o2.getFirst();
					KItemset tmp2Y = o2.getSecond();
					double delta1 = deltaSize(codetable, standardSize, tmp1X, tmp1Y);
					double delta2 = deltaSize(codetable, standardSize, tmp2X, tmp2Y);
					if(delta1 != delta2) {
						return - Double.compare(delta1, delta2);
					} else {
						return - Integer.compare((tmp1X.size() + tmp1Y.size()), (tmp2X.size() + tmp2Y.size()));
					}
				}
			};
			
			Comparator<Couple<KItemset, KItemset>> usageCoupleComparator = new Comparator<Couple<KItemset, KItemset>>(){
				@Override
				public int compare(Couple<KItemset, KItemset> o1, Couple<KItemset, KItemset> o2) {
					KItemset tmp1X = o1.getFirst();
					KItemset tmp1Y = o1.getSecond();
					int tmp1CombiUsage = codetable.estimateUsageCombination(tmp1X, tmp1Y);
					KItemset tmp2X = o2.getFirst();
					KItemset tmp2Y = o2.getSecond();
					int tmp2CombiUsage = codetable.estimateUsageCombination(tmp2X, tmp2Y);
					if(tmp1CombiUsage != tmp2CombiUsage) {
						return - Integer.compare(tmp1CombiUsage, tmp2CombiUsage);
					} else {
						return - Integer.compare((tmp1X.size() + tmp1Y.size()), (tmp2X.size() + tmp2Y.size()));
					}
				}
			};
			
			Comparator<KItemset> usageComparator = new Comparator<KItemset>(){
				@Override
				public int compare(KItemset o1, KItemset o2) {
					if(codetable.getUsage(o1) != codetable.getUsage(o2)) {
						return - Integer.compare(codetable.getUsage(o1), codetable.getUsage(o2));
					} else {
						return - Integer.compare(o1.size(), o2.size());
					}
				}
			};
			
			Comparator<KItemset> itemsetComparator = null;
			Comparator<Couple<KItemset, KItemset>> coupleComparator = null;
			if(this.getCandidateStrategy() == CANDIDATE_STRATEGY.USAGE) {
				itemsetComparator = usageComparator;
				coupleComparator = usageCoupleComparator;
			} else if(this.getCandidateStrategy() == CANDIDATE_STRATEGY.GAIN) {
				itemsetComparator = usageComparator;
				coupleComparator = gainComparator;
			}
			
			Predicate<KItemset> zeroUsagePredicate = new Predicate<KItemset>() {
				@Override
				public boolean test(KItemset fi) {
					return codetable.getUsage(fi) == 0;
				}
			};
			
			ItemsetSet codes = new ItemsetSet(codetable._codes);
			codes.removeIf(zeroUsagePredicate);
			codes.sort(itemsetComparator);
			
			while(! this._topKCandidates.isEmpty() || _moreCandidates) {
				if(! this._topKCandidates.isEmpty()) {
//					logger.debug("topKcandidates: first " + codetable.estimateUsageCombination(this._topKCandidates.peekFirst().getFirst(), this._topKCandidates.peekFirst().getSecond()));
//					logger.debug("topKcandidates: Last " + codetable.estimateUsageCombination(this._topKCandidates.peekLast().getFirst(), this._topKCandidates.peekLast().getSecond()));
//					
//					logger.debug("codes: first " + codetable.getUsage(codes.peekFirst()));
//					logger.debug("codes: last " + codetable.getUsage(codes.peekLast()));
					
//					logger.debug("Trying with top "+ this._topKCandidates.size() +" candidates");
					Iterator<Couple<KItemset, KItemset>> itTopKCandidates = this._topKCandidates.iterator();
					while(itTopKCandidates.hasNext()) {
						Couple<KItemset, KItemset> coupleCandidate = itTopKCandidates.next();
						KItemset tmpX = coupleCandidate.getFirst();
						KItemset tmpY = coupleCandidate.getSecond();
						KItemset candidate = new KItemset(tmpX);
						candidate.addAll(tmpY);
						if(! testedCandidates.contains(candidate)) {
							if(codetable.estimateUsageCombination(tmpX, tmpY) > 0) {
								return candidate;
							}
						}
					}
					
					// No candidate were found
				}
				
				if(_moreCandidates){
//					logger.debug("Generating more candidates");
					
					_moreCandidates = false;
					int bestUsage = 0;
					double topCandidateCriteria = 0.0;
					if(! _topKCandidates.isEmpty()) { // usage of the worst candidate in the top k list
						bestUsage = codetable.estimateUsageCombination(_topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
						if(this.getCandidateStrategy() == CANDIDATE_STRATEGY.USAGE) {
							topCandidateCriteria = codetable.estimateUsageCombination(_topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
						} else if(this.getCandidateStrategy() == CANDIDATE_STRATEGY.GAIN) {
							topCandidateCriteria = deltaSize(codetable, standardSize, _topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
						}
					}
									
					for(int iX = 0; iX < codes.size(); iX++) {
						KItemset tmpX = codes.get(iX);
						int currentXUsage = codetable.getUsage(tmpX);
						if(currentXUsage > 0 && currentXUsage >= bestUsage) {
							for(int iY = iX+1; iY < codes.size(); iY++) {
								KItemset tmpY = codes.get(iY);
								if(codetable.haveCommonSupport(tmpX, tmpY)) {
									int currentYUsage = codetable.getUsage(tmpY);
									if(currentYUsage > 0 && currentYUsage >= bestUsage) {
										KItemset candidatePotential = new KItemset(tmpX);
										candidatePotential.addAll(tmpY);
										if(candidatePotential.size() <= codetable._index.getMaxSize()
												&& ! testedCandidates.contains(candidatePotential)
												&& ! this._topKCandidates.contains(new Couple<KItemset, KItemset>(tmpX, tmpY))
												&& candidatePotential.size() > 1) {
											int candidateUsage = codetable.estimateUsageCombination(tmpX, tmpY);
											double candidateGain = deltaSize(codetable, standardSize, tmpX, tmpY);
											if( candidateUsage > 0) { 
												boolean newCandidateadded = false;
												if(this.getCandidateStrategy() == CANDIDATE_STRATEGY.USAGE && candidateUsage >= topCandidateCriteria) {
													topCandidateCriteria = codetable.estimateUsageCombination(tmpX, tmpY);
													newCandidateadded = true;
												}
												if(this.getCandidateStrategy() == CANDIDATE_STRATEGY.GAIN && candidateGain >= topCandidateCriteria) {
													newCandidateadded = true;
													topCandidateCriteria = deltaSize(codetable, standardSize, tmpX, tmpY);
												}
												if(newCandidateadded) {
													_topKCandidates.addFirst(new Couple<KItemset, KItemset>(tmpX, tmpY));
													bestUsage = codetable.estimateUsageCombination(_topKCandidates.peekFirst().getFirst(), _topKCandidates.peekFirst().getSecond());
													_moreCandidates = true; // We could contribute
												}
											}
										}
									} else {
										_moreCandidates = (currentYUsage > 0);
										break;
									}
								}
							}
						} else {
							_moreCandidates = (currentXUsage > 0);
							break;
						}
					}
	
					while(this._topKCandidates.size() > _maxNumberofCandidates) {
						this._topKCandidates.removeLast();
					}
				}
			}
		}
//		logger.debug("No candidate proposed");
		return null;
	}

}
