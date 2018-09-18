///////////////////////////////////////////////////////////////////////////////
// File: Measures.java 
// Author: Carlos Bobed 
// Date: October 2017
// Comments: Implementation of the different measures proposed, as well 
// 		as Jiles' ones (see Characterising the differences, SIGKDD 2007). 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.utilities.Couple;

public class Measures {
	
	private static Logger logger = Logger.getLogger(Measures.class);


	
	/** 
	 * 
	 * Calculate the Structural similarity between D1 and D2 through their codetables. 
	 * Without Keeping the distribution means that the usages of the codeTable CT2 are updated, 
	 * not keeping the original distribution of the data implicitly. This is important for pure structural inclusion scenarios.  
	 * 
	 *  None of the CTs is altered (CT2 is cloned). Both CTs have to have the same "vocabulary"/items. 
	 *  
	 *  This is the measure proposed in our original submission, and corresponds also to the way Jilles et al. measure 
	 *  is calculated
	 * 
	 * @param D1 The database to be compared
	 * @param CT1 The codetable obtained from D1
	 * @param CT2 The codetable of the original KB (against which we compare D1)
	 * @return
	 */
	
	public static double structuralSimilarityWithoutKeepingDistribution (ItemsetSet D1, CodeTable CT1, CodeTable CT2 ) {
//		logger.debug("CT1: " + CT1);
//		logger.debug("CT2: " + CT2);

		
		// first we get the size of the database D1 codified with its own CT
		CodificationMeasure measure1 = new CodificationMeasure(D1, CT1);
		double evalKrimpSize = 0.0;
		long start = System.nanoTime(); 
		try {
			evalKrimpSize = measure1.codificationLength();
			logger.debug("Original dataset codification:  "+(((double)(System.nanoTime()-start))/(double)1000000)+" ms"); 
			
		} catch(AssertionError e) {
			logger.debug(CT1);
			throw e;
		}
		logger.debug("evalSize: "+evalKrimpSize);
		
		// we clone the CT2
		// the usages are updated in the init() method
		// we reuse as much as possible the information already calculated in the previous CTs
		CodificationMeasure measure2 = new CodificationMeasure(D1, CT2);
		start = System.nanoTime(); 
		measure2.updateUsages();
		logger.debug("Update usages: "+(((double)System.nanoTime()-start)/(double)1000000)+" ms"); 
		
		double refKrimpSize = 0.0; 
		try {
			logger.debug("OUT: "+D1.size());
			start = System.nanoTime(); 
			refKrimpSize = measure2.codificationLength();
			logger.debug("Compared dataset codification:  "+(((double)(System.nanoTime()-start))/(double)1000000)+" ms"); 
		} catch(AssertionError e) {
			logger.debug(CT2);
			throw e;
		}
//		logger.debug("structuralSimilarityWithoutKeepingDistribution " + refKrimpSize +  " / " + evalKrimpSize);
		assert evalKrimpSize > 0.0; 
		return refKrimpSize / evalKrimpSize; 		
	}
	
	/** 
	 * 
	 * Calculate the Structural similarity between D1 and D2 through their codetables. 
	 *   
	 *  This is the new measure adapted to calculate quickly deltas of versions of the same dataset
	 * 
	 * @param D1 The database to be compared
	 * @param CT1 The codetable obtained from D1
	 * @param CT2 The codetable of the original KB (against which we compare D1)
	 * @return
	 */
	
	public static double structuralSimilarityKeepingDistribution (ItemsetSet D1, CodeTable CT1, CodeTable CT2 ) {

		// first we get the size of the database D1 codified with its own CT
		CodificationMeasure measure1 = new CodificationMeasure(D1, CT1);
		double evalKrimpSize = measure1.codificationLength();
		
		// we clone the CT2
		// we reuse as much as possible the information already calculated in the previous CTs
		CodificationMeasure measure2 = new CodificationMeasure(D1, CT2);
		measure2.applyLaplaceSmoothingToUsages(); // Necessary for eventual items unknow to the CT2 appearing in the dataset D1
		
		double refKrimpSize = measure2.codificationLength(); 
		
		assert evalKrimpSize > 0.0; 
		return refKrimpSize / evalKrimpSize; 		
	}
	
	
	
	/** 
	 * 
	 * Calculate the Structural similarity between D1 and D2 through their codetables, including the information 
	 * derived from the new data distribution (through their code lengths). 
	 * Without Keeping the distribution means that the usages of the codeTable CT2 are updated, 
	 * not keeping the original distribution of the data implicitly. This is important for pure structural inclusion scenarios.
	 * Besides, this version takes into account also the implications of the new data distribution in terms of 
	 * code lengths.   
	 * 
	 *  None of the CTs is altered (CT2 is cloned). Both CTs have to have the same "vocabulary"/items. 
	 *  
	 *  This is the measure proposed in our original submission, and corresponds also to the way Jilles et al. measure 
	 *  is calculated
	 * 
	 * @param D1 The database to be compared
	 * @param CT1 The codetable obtained from D1
	 * @param CT2 The codetable of the original KB (against which we compare D1)
	 * @return
	 */
	
	public static double structuralSimilarityWithoutKeepingDistributionUsingLengths (ItemsetSet D1, CodeTable CT1, CodeTable CT2 ) {

		// first we get the size of the database D1 codified with its own CT
		CodificationMeasure measure1 = new CodificationMeasure(D1, CT1);
	
		long start = System.nanoTime(); 
		double evalKrimpSize = measure1.codificationLength();
		logger.debug("Original dataset codification:  "+(((double)(System.nanoTime()-start))/(double)1000000)+" ms"); 

		// we clone the CT2
		// the usages are updated in the init() method
		// we reuse as much as possible the information already calculated in the previous CTs
		CodificationMeasure measure2 = new CodificationMeasure(D1, CT2);
		start = System.nanoTime();
		measure2.updateUsages();
		logger.debug("Update usages: "+(((double)System.nanoTime()-start)/(double)1000000)+" ms"); 
		
		start = System.nanoTime(); 
		double refKrimpSize = measure2.codificationLength(); 
		logger.debug("Compared dataset codification:  "+(((double)(System.nanoTime()-start))/(double)1000000)+" ms"); 

		assert evalKrimpSize > 0.0; 
		
		logger.trace("structuralSimilarityWithoutKeepingDistributionUsingLengths = ( " + refKrimpSize + " + " + measure2.codetableCodeLength() + " / (" + evalKrimpSize + " + " + measure1.codetableCodeLength() + " )");
		return (refKrimpSize + measure2.codetableCodeLength()) / (evalKrimpSize + measure1.codetableCodeLength()); 		
	}
	
	
	/** 
	 * Calculate the differences of the lengths of the codes of a CT when presented a different database
	 * 
	 * @param CT The code table whose change of coverage we want to evaluate 
	 * @param D The new database 
	 * @return 
	 */
	public static CTLengthDifferences lengthCodeDifferences (CodeTable CT, ItemsetSet D) {
	
		CTLengthDifferences result = new CTLengthDifferences(); 
		
//		CodeTable originalCT = new CodeTable (CT); 
		CodificationMeasure measure1 = new CodificationMeasure(D, CT);
		measure1.applyLaplaceSmoothingToUsages();
		
		CodificationMeasure measure2 = new CodificationMeasure(D, CT);
		measure2.updateUsages();
		measure2.applyLaplaceSmoothingToUsages();
		
		for (KItemset code: CT.getCodes()) {
			result.putDifference(code, measure2.codeLengthOfCode(code) - measure1.codeLengthOfCode(code));
		}
		
		return result; 
		
	}
	

	
	
//	/** 
//	 * Codify a set of transactions using a CT that might not include all the items
//	 * To avoid potential problems with previously non-used singletons and new singletons 
//	 * introduced by new items, this method assumes to the new items
//	 * by giving them the longest codes (result of the laplace Smoothing). 
//	 *
//	 * @param D1
//	 * @param CT1 
//	 * @return 
//	 */
//	
//	public static double codificationLengthApplyingLaplaceSmoothing (ItemsetSet D1, CodeTable CT1) {
//		// we have to clone and smooth the codeTable 
//		CodificationMeasure measure = new CodificationMeasure(D1, CT1);
//		// we change the database without updating anything but the dataIndex
//		// we applyLaplaceSmoothing for perplexity purposes
//		measure.applyLaplaceSmoothingToUsages();
//		// second we get the size of the database D1 codified with the
//		return measure.codificationLength();  		
//	}
	
	/** 
	 * Codify a set of transactions using a CT that might not include all the items
	 * To avoid potential problems with previously non-used singletons and new singletons 
	 * introduced by new items, this method assumes to the new items
	 * by giving them the longest codes (result of the laplace Smoothing). 
	 * 
	 * Returns also the codification length using the SCT associated to CT
	 *
	 * @param D1
	 * @param CT1 
	 * @return 
	 */
	
	public static Couple<Double, Double> codificationLengthApplyingLaplaceSmoothingIncludingSCT (ItemsetSet D1, CodeTable CT1) {
		
		Couple<Double, Double> result = null; 
		// we have to clone and smooth the codeTable 
		CodificationMeasure measure = new CodificationMeasure(D1, CT1);
		// we change the database without updating anything but the dataIndex
		// we applyLaplaceSmoothing for perplexity purposes
		measure.applyLaplaceSmoothingToUsages();
		
		double resultCT = measure.codificationLength(); 
		double resultSCT = measure.codificationLengthAccordingSCT(); 
		
		// second we get the size of the database D1 codified with the
		return new Couple<Double, Double>(resultCT, resultSCT) ;  		
	}
	
	
	/** 
	 * Codify both states q and q' of an update using the same CT 
	 * in the same status
	 *
	 * @param q 
	 * @param qPrima 
	 * @param CT1 
	 * @return 
	 */
	
	public static Couple<Couple<Double, Double>, Couple<Double, Double>> codifyUpdateStatesApplyingLaplaceSmoothingIncludingSCT (ItemsetSet q, ItemsetSet qPrima, CodeTable CT1) {
		
		ItemsetSet union = new ItemsetSet(); 
		union.addAll(q); 
		union.addAll(qPrima); 
		// we have to clone and smooth the codeTable 
		CodificationMeasure measure = new CodificationMeasure(union, CT1);
		// we change the database without updating anything but the dataIndex
		// we applyLaplaceSmoothing for perplexity purposes
		measure.applyLaplaceSmoothingToUsages();
		// now, with the same codeTable, already containing the union
		// we codify the transactions separatedly 	
		
		double resultQCT = measure.codificationLengthExternal(q); 
		double resultQSCT = measure.codificationLengthAccordingSCTExternal(q);
		double resultQPrimaCT = measure.codificationLengthExternal(qPrima); 
		double resultQPrimaSCT = measure.codificationLengthAccordingSCTExternal(qPrima); 
		
		// second we get the size of the database D1 codified with the
		return new Couple<>	(new Couple<>(resultQCT, resultQSCT), new Couple<>(resultQPrimaCT, resultQPrimaSCT)) ;  		
	}
	
	public static Couple<ArrayList<Couple<KItemset, ItemsetSet>>, 
						ArrayList<Couple<KItemset, ItemsetSet>>> codificationUpdateStatesApplyingLaplaceSmoothingIncludingSCT 
						(ItemsetSet q, ItemsetSet qPrima, CodeTable CT1) {
		
		ItemsetSet union = new ItemsetSet(); 
		union.addAll(q); 
		union.addAll(qPrima); 
		// we have to clone and smooth the codeTable 
		CodificationMeasure measure = new CodificationMeasure(union, CT1);
		// we change the database without updating anything but the dataIndex
		// we applyLaplaceSmoothing for perplexity purposes
		measure.applyLaplaceSmoothingToUsages();
		
		// now, with the same codeTable, already containing the union
		// we codify the transactions separatedly
		
		ArrayList<Couple<KItemset, ItemsetSet>> qCod = measure.codificationsExternal(q); 
		ArrayList<Couple<KItemset, ItemsetSet>> qPrimaCod = measure.codificationsExternal(qPrima); 
		
		// second we get the size of the database D1 codified with the
		return new Couple<>	(qCod, qPrimaCod) ;  		
	}
	
	
	public static ArrayList<Couple<KItemset, ItemsetSet>>codificationExternalApplyingLaplaceSmoothingIncludingSCT 
				(ItemsetSet trans, CodeTable CT1) {
			
			CodificationMeasure measure = new CodificationMeasure(trans, CT1);
			// we change the database without updating anything but the dataIndex
			// we applyLaplaceSmoothing for perplexity purposes
			measure.applyLaplaceSmoothingToUsages();
			
			// now, with the same codeTable, already containing the union
			// we codify the transactions separatedly
			
			ArrayList<Couple<KItemset, ItemsetSet>> transCod = measure.codificationsExternal(trans); 
			
			// second we get the size of the database D1 codified with the
			return transCod ;  		
			}
	
	/** 
	 * 
	 * Comparison of the length of CT1 with the length of CT1 when codified using CT2
	 * Here, in principle, it does not make sense to change the distribution of the data
	 *	CT1 gets coded by CT2
	 *
	 * @param CT1
	 * @param CT2
	 * @return
	 */
	
	public static double CTStructuralComparison (CodeTable CT1, CodeTable CT2) {
		
		// we need to apply laplace smoothing to include the codes that are not used
		// in the game (another option in this case would be to make the assumption of 0*log(0) == 0) 
		CodificationMeasure measure1 = new CodificationMeasure( CT1);
		
		CodificationMeasure measure2 = new CodificationMeasure(CT1.getCodes(), CT2);
		measure2.applyLaplaceSmoothingToUsages();
		
		double evalKrimpSize = measure1.getCodetableCodesLength(); 
		System.out.println("evalKrimpSize: "+ evalKrimpSize);
		double refKrimpSize = measure2.codificationLength(); 
		System.out.println("refKrimpSize: "+refKrimpSize); 
		return refKrimpSize/evalKrimpSize; 
		
	}
	
	
}
