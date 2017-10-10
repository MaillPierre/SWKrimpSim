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
import com.irisa.krimp.data.DataIndexes;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

public class Measures {
	
	private static Logger logger = Logger.getLogger(Measures.class);

	/** 
	 * 
	 * Calculate the Structural similarity between D1 and D2 through their codetables. 
	 * Keeping the distribution means that the usages of the codeTable CT2 is not updated 
	 * in order to keep the original data distribution as well. This is important for integration/updating/cleaning scenarios 
	 * where we want to get also information about the original data (it is implicitly stored in the 
	 * original length of the codes). 
	 * 
	 *  None of the CTs is altered. Both CTs have to have the same "vocabulary"/items
	 * 
	 * @param D1 The database to be compared
	 * @param CT1 The codetable obtained from D1
	 * @param CT2 The codetable of the original KB (against which we compare D1)
	 * @return
	 */
	
	public static double structuralSimilarityKeepingDistribution (ItemsetSet D1, CodeTable CT1, CodeTable CT2 ) {

		// first we get the size of the database D1 codified with its own CT
		double evalKrimpSize = CT1.codificationLength(D1);
		// second we get the size of the database D1 codified with the
		double refKrimpSize = CT2.codificationLength(D1); 
		assert evalKrimpSize > 0.0; 
		return refKrimpSize / evalKrimpSize; 		
	}
	
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
		CodeTable tempCT = new CodeTable(D1, CT2.getCodes(), CT1.get_index());
		
		double refKrimpSize = tempCT.codificationLength(D1); 
		
		assert evalKrimpSize > 0.0; 
		return refKrimpSize / evalKrimpSize; 		
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
		// TODO: we could speed up this cloning step if we shared also the indexes 
		DataIndexes indexes = new DataIndexes(D); 
		CodeTable tempCT = new CodeTable(D, CT.getCodes(), indexes); 
		
		for (KItemset code: CT.getCodes()) {
			result.putDifference(code, tempCT.codeLengthOfcode(code) - CT.codeLengthOfcode(code));
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
		
		double evalKrimpSize = CT1.codificationLength(CT1.getCodes()); 
		double refKrimpSize = CT2.codificationLength(CT1.getCodes()); 
		
		logger.debug("CTStructuralComparison>> CT1 coded with CT1: "+evalKrimpSize);
		logger.debug("CTStructuralComparison>> CT1 coded with CT2: "+refKrimpSize);
		
		return refKrimpSize/evalKrimpSize; 
		
	}
	
}
