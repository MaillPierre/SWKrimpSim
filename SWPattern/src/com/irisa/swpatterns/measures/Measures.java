///////////////////////////////////////////////////////////////////////////////
// File: Measures.java 
// Author: Carlos Bobed 
// Date: October 2017
// Comments: Implementation of the different measures proposed, as well 
// 		as Jiles' ones (see Characterising the differences, SIGKDD 2007). 
// Modifications: 
///////////////////////////////////////////////////////////////////////////////

package com.irisa.swpatterns.measures;

import org.apache.log4j.Logger;

import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

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
	
	/** 
	 * 
	 * Comparison of the length of CT1 with the length of CT1 when codified using CT2
	 * Here, in principle, it does not make sense to change the distribution of the data
	 * 
	 * @param CT1
	 * @param CT2
	 * @return
	 */
	
	public static double CTStructuralComparison (CodeTable CT1, CodeTable CT2) {
		
		// we need to apply laplace smoothing to include the codes that are not used
		// in the game (another option in this case would be to make the assumption of 0*log(0) == 0) 
		CodificationMeasure measure1 = new CodificationMeasure(CT1);
		measure1.applyLaplaceSmoothingToUsages();
		
		CodificationMeasure measure2 = new CodificationMeasure(CT1.getCodes(), CT2);
		measure2.applyLaplaceSmoothingToUsages();
		
		double evalKrimpSize = measure1.codificationLength(); 
		double refKrimpSize = measure2.codificationLength(); 
		
		return refKrimpSize/evalKrimpSize; 
		
	}
	
	
	/** 
	 * Codify a set of transactions using a CT that might not include all the items
	 * To avoid potential problems with previously non-used singletons and new singletons 
	 * introduced by new items, this method assumes to the new items
	 * by giving them the longest codes (result of the laplace Smoothing). 
	 *
	 * @param D1
	 * @param CT1 
	 * @return 
	 */
	
	public static double codificationLengthApplyingLaplaceSmoothing (ItemsetSet D1, CodeTable CT1) {
		// we have to clone and smooth the codeTable 
		CodificationMeasure measure = new CodificationMeasure(D1, CT1);
		// we change the database without updating anything but the dataIndex
		// we applyLaplaceSmoothing for perplexity purposes
		measure.applyLaplaceSmoothingToUsages();
		// second we get the size of the database D1 codified with the
		return measure.codificationLength();  		
	}
	
	
	
	
}
