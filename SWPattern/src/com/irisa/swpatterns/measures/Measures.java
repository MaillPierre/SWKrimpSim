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

		// first we get the size of the database D1 codified with its own CT
		double evalKrimpSize = CT1.codificationLength(D1);
		
		// we clone the CT2
		// the usages are updated in the init() method
		// we reuse as much as possible the information already calculated in the previous CTs
		CodeTable tempCT = new CodeTable (CT2); 
		tempCT.setTransactions(D1);
		tempCT.updateUsages();
		
		double refKrimpSize = tempCT.codificationLength(D1); 
		
		assert evalKrimpSize > 0.0; 
		return refKrimpSize / evalKrimpSize; 		
	}
	
	
	
	public static double structuralSimilarityWithoutKeepingDistributionSharingItemset (CodeTable CT1, CodeTable CT2 ) {

		// first we get the size of the database D1 codified with its own CT
		double evalKrimpSize = CT1.encodedTransactionSetCodeLength();
		
		// we don't have to clone it, as it share the itemset
		double refKrimpSize = CT2.encodedTransactionSetCodeLength(); 
		
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
		double evalKrimpSize = CT1.codificationLength(D1);
		
		// we clone the CT2
		// the usages are updated in the init() method
		// we reuse as much as possible the information already calculated in the previous CTs
		CodeTable tempCT = new CodeTable (CT2); 
		tempCT.setTransactions(D1);
		tempCT.updateUsages();
		
		double refKrimpSize = tempCT.codificationLength(D1); 
		
		assert evalKrimpSize > 0.0; 
		
		
		return (refKrimpSize + tempCT.codeTableCodeLength()) / (evalKrimpSize + CT1.codeTableCodeLength()); 		
	}
	
	public static double structuralSimilarityWithoutKeepingDistributionUsingLengthsSharingItemset (CodeTable CT1, CodeTable CT2 ) {

		// first we get the size of the database D1 codified with its own CT
		double evalKrimpSize = CT1.encodedTransactionSetCodeLength();
		double refKrimpSize = CT2.encodedTransactionSetCodeLength(); 
		
		assert evalKrimpSize > 0.0; 
		
		return (refKrimpSize + CT2.codeTableCodeLength()) / (evalKrimpSize + CT1.codeTableCodeLength()); 		
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
		
		CodeTable originalCT = new CodeTable (CT); 
		originalCT.applyLaplaceSmoothingToUsages(); 
		
		CodeTable tempCT = new CodeTable (CT); 
		tempCT.setTransactions(D);
		tempCT.updateUsages();
		tempCT.applyLaplaceSmoothingToUsages();
		
		for (KItemset code: CT.getCodes()) {
			result.putDifference(code, tempCT.codeLengthOfcode(code) - originalCT.codeLengthOfcode(code));
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
		CodeTable originalCT = new CodeTable(CT1); 
		originalCT.applyLaplaceSmoothingToUsages();
		
		CodeTable tempCT = new CodeTable (CT2); 
		tempCT.setTransactions(CT1.getCodes());
		tempCT.applyLaplaceSmoothingToUsages();
		
		double evalKrimpSize = originalCT.codificationLength(CT1.getCodes()); 
		double refKrimpSize = tempCT.codificationLength(CT1.getCodes()); 
		
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
		CodeTable tempCT = new CodeTable(CT1);
		// we change the database without updating anything but the dataIndex
		tempCT.setTransactions(D1);
		// we applyLaplaceSmoothing for perplexity purposes
		tempCT.applyLaplaceSmoothingToUsages();
		// second we get the size of the database D1 codified with the
		return tempCT.codificationLength(D1);  		
	}
	
	
	
	
}
