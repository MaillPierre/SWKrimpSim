package com.irisa.swpatterns.measures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.irisa.exception.LogicException;
import com.irisa.utilities.Couple;
import com.irisa.krimp.CodeTable;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.krimp.data.Utils;
import org.apache.log4j.Logger;

/**
 * Represents one codification measure of a transaction set using one codetable or a codetable by itself.
 * The codetable is copied, the transactions are not
 * @author pmaillot
 *
 */
public class CodificationMeasure {
	
	private static Logger logger = Logger.getLogger(CodificationMeasure.class);

	private ItemsetSet _transactions = null;
	private CodeTable _codetable = null;
	
	public CodificationMeasure(final ItemsetSet transactions, final CodeTable codetable) {
		this.setTransactions(transactions);
		this.setCodetable(codetable);
	}
	
	public CodificationMeasure(final CodeTable codetable) {
		this.setCodetable(codetable);
	}

	public CodeTable getCodetable() {
		return _codetable;
	}

	public void setCodetable(CodeTable _codetable) {
		this._codetable = new CodeTable(new ItemsetSet(_codetable.getCodes()));
	}

	public ItemsetSet getTransactions() {
		return _transactions;
	}

	public void setTransactions(ItemsetSet _transactions) {
		this._transactions = _transactions;
	}

	/**
	 * For code X, this is P(X |D)
	 * @param code
	 * @return
	 */
	private static double probabilisticDistrib(CodeTable ct, KItemset code) {
		int codeUsage = code.getUsage();
//		if(codeUsage == 0 && ct.contains(code)) {	
		if(codeUsage == 0) {
			// we look for it in the table
			if (code.size() != 1) {
				Optional<KItemset> value = ct.getCodes()
		            .parallelStream()
					.filter(a -> a.size() != 1)
		            .filter(a -> a.equals(code))
		            .findFirst();
					
				if (value.isPresent()) {
					codeUsage = value.get().getUsage();
				}
				else {
					logger.error("WO Exception: Unable to retrieve probabilistic distribution of " + code +  " from the codetable.");
				}
				
//				Iterator<KItemset> codes = ct.getCodes().iterator();
//				KItemset auxValue = null; 
//				boolean nonOne = true; 
//				boolean found = false;
//				while (codes.hasNext() && nonOne && !found) { 
//					auxValue = codes.next(); 
//					if (auxValue.size() != 1) {
//						found = auxValue.equals(code);
//					}
//					else {
//						// we have reached the part of the CT that is only singletons
//						nonOne=false; 
//					}
//				}
//				if (found) {
//					codeUsage = auxValue.getUsage();	
//				}
//				else { 
//					logger.error("WO Exception - size !=1 - Unable to retrieve probabilistic distribution of " + code +  " from the codetable.");
//				}
				
			}
			else {
				// we use the onelengthIndex to access directly to the code
				HashMap<Integer,KItemset> oneLength = ct.getOneLengthCodes(); 
				if (oneLength.containsKey(code.getItems()[0])) {
					codeUsage = oneLength.get(code.getItems()[0]).getUsage();
				}
				else {
					logger.error("WO Exception: Unable to retrieve probabilistic distribution of " + code +  " from the codetable.");
				}
			}
		}
		return (double) codeUsage / (double) ct.getUsageTotal();
	}
	
	/**
	 * L(code_CT(X))
	 * @param code
	 * @return
	 */
	private static double codeLengthOfcode(CodeTable ct, KItemset code) {
		double value = probabilisticDistrib(ct, code);
		if (value!= 0) { 
			return - Math.log(value);
		} 
		else { 
			return 0; 
		}
	}
	
	/**
	 * L(CT|D)
	 * @return
	 */
	private static double codeTableCodeLength(CodeTable ct) {
		double result = 0.0;
		ct.regenerateStandardCodeTable(); // To be sure all codes appearing are taken into account in the STCT
		Iterator<KItemset> itCodes = ct.codeIterator();
		while(itCodes.hasNext()) {
			KItemset code = itCodes.next();
			if(code.getUsage() != 0) {
				// CB: this is the code length according to the CT
				double cL = codeLengthOfcode(ct, code);
				
				// CB: we also need the code length according to the ST: we codify the codeusing it
				double stcL = 0 ;
				if (!ct.isStandard()) {
					stcL = codeLengthOfCodeAccordingST(ct, code);
				}
				// else => it is a 0.0
				
				logger.trace("codeTableCodeLength: CTlength=" + cL + " stcL=" + stcL + " code=" + code );
				result += cL + stcL;
			}
		}
		return result;
	}
	
	
	public double getCodetableCodesLength() { 
		double result = 0.0;
		for (KItemset code: this._codetable.getCodes()) {
			result += codeLengthOfCode(code); 
		}
		return result; 
	}
	
	/** 
	 * L(code_ST(X)).
	 * The argument codetable is NOT the standard codetable
	 * @return 0 if the given code table is the standard codetable
	 */
	private static double codeLengthOfCodeAccordingST(CodeTable ct, KItemset code) {
		double result = 0.0;
		// this method should return 0 if the codetable is the ST
		if (! ct.isStandard()) {
			Iterator<Integer> itCode = code.iterator();
			while(itCode.hasNext()) {
				Integer item = itCode.next();
				double itemCodeLength = codeLengthOfcode(ct.getStandardCodeTable(), Utils.createCodeSingleton(item));
				logger.trace("codeLengthOfCodeAccordingST: codelength=" + itemCodeLength + " item=" + item);
				result += itemCodeLength; 
			}
		}
		return result; 
	}
	
//	TBD
	/**
	 * L(D, CT)
	 * @return
	 * @throws LogicException 
	 */
	public double totalCompressedSize() throws LogicException {
		double ctL = codeTableCodeLength(this._codetable);
		double teL = codificationLength();
		return ctL + teL;
	}

	
	/**
	 * return a codetable with the usages initialized according to the cover
	 * PRE: the codeTable must be in standardCoverTable order
	 */
	public void updateUsages() {
		
		// we first extend the singletons with the non-seen ones to 
		// avoid concurrent modifications
		ArrayList<KItemset> newOnes = new ArrayList<>();
		logger.debug("known Items: "+this._transactions.knownItems().size()); 
		HashMap<Integer,KItemset> oneLengthCodes = this._codetable.getOneLengthCodes();
		for (Integer codeInt: this._transactions.knownItems()) {
			
//			Optional<KItemset> value = this._codetable.getCodes()
//	            .stream()
//	            .filter(a -> a.equals(Utils.createCodeSingleton(codeInt)))
//	            .findFirst();
//			if (!value.isPresent()) {
//				// we create them with support 0 on purpose to them being 
//				// added to the end of the code table in standard cover order
//				newOnes.add(Utils.createCodeSingleton(codeInt, 0,1)); 
//			}		
			
			if (!oneLengthCodes.containsKey(codeInt)) {
				newOnes.add(Utils.createCodeSingleton(codeInt, 0,1));
			}
		}
//		newOnes.stream().forEach(e -> this._codetable.addSingleton(e));
		if (!newOnes.isEmpty()) {
			this._codetable.addSingletons(newOnes);
			logger.debug("Singletons added: "+newOnes.size());
			logger.debug("---");
//			logger.debug(this._codetable.toString());
//			logger.debug(this._codetable.getOneLengthCodes().keySet());
			
		}
		long start = System.nanoTime();
		// it should already be in standard coverage order
		this._codetable.orderCodesStandardCoverageOrder();
		logger.debug("Ordering CT: "+(((double)System.nanoTime()-start)/(double)1000000)+" ms.");
		// we should be now safe 
		
		start = System.nanoTime(); 
		this._codetable.getCodes().parallelStream().forEach(e -> e.setUsage(0));
		logger.debug("Setting 0: "+(((double)System.nanoTime()-start)/(double)1000000)+" ms.");
		start = System.nanoTime(); 
		this._transactions.parallelStream().forEach(e -> this.updateUsagesTransaction(e));
		logger.debug("UpdateTransactions: "+(((double)System.nanoTime()-start)/(double)1000000)+" ms.");
		start = System.nanoTime(); 
		this._codetable.recomputeUsageTotal();
		logger.debug("RecomputingUsageTotal: "+(((double)System.nanoTime()-start)/(double)1000000)+" ms.");
		
	}
	
	public void updateUsagesTransaction (KItemset transaction) {
		ItemsetSet codes = this.codify(transaction);
		for (KItemset code: codes) { 
			code.incrementUsageAtomically(); 
		}
	}
	
	
	/** 
	 * Codifying function according to the KRIMP paper
	 * It updates the code table with the non-seen singletons 
	 * as it is oriented to scenarios where we have to update the 
	 * 
	 * @param trans
	 * @return
	 */
	private ItemsetSet codify(KItemset trans) {
		KItemset auxTrans = new KItemset(trans);
		ItemsetSet result = new ItemsetSet(); 
		Iterator<KItemset> itIs = this._codetable.codeIterator();
		KItemset auxCode = null; 
		boolean lengthOneReached = false;
//		int execs = 0; 
//		long start = System.nanoTime();
		while (itIs.hasNext() && (auxTrans.size() != 0) && (!lengthOneReached))  { // Searching for the cover
			auxCode = itIs.next(); 
			if (auxCode.size()!=1) {
				// we check all the codes that are not singletons
				if (auxTrans.containsAll(auxCode)) {
					result.add(auxCode); 
					auxTrans = auxTrans.substraction(auxCode); 
				}
			}
			else {
				lengthOneReached = true;
			}
		}
		// now we treat all the remaining elements in auxTrans as 
		// singletons

		if (!auxTrans.isEmpty()) {
			KItemset remainingSingletons = new KItemset(auxTrans);
			HashMap<Integer, KItemset> oneLength = this._codetable.getOneLengthCodes();
 			for(int item : remainingSingletons) {
				if (oneLength.containsKey(item)) {
					auxTrans=auxTrans.substraction(oneLength.get(item));
					result.add(oneLength.get(item));
				}
				else { 
					KItemset singleton = Utils.createCodeSingleton(item, 0, 1); 
					result.add(singleton); 
					this._codetable.addSingleton(singleton);
				}
			}
		}
		
		// adding the codes that appear in the transaction but not in the code table
//		if(! auxTrans.isEmpty()) {
//			for(int item : auxTrans) {
//				
//				
//				
//				KItemset sinlgton = Utils.createCodeSingleton(item, 0, 1);
//				result.add(sinlgton);
//				this._codetable.addSingleton(sinlgton);
//			}
//		}
//		logger.debug("new approach: "+(((double)System.nanoTime()-start)/(double)1000000)+ " ms.");
//		logger.debug(result);

		
		
//		// old code
//		
//		KItemset auxTrans2 = new KItemset(trans);
//		ItemsetSet result2 = new ItemsetSet(); 
//		Iterator<KItemset> itIs2 = this._codetable.codeIterator();
//		KItemset auxCode2 = null; 
//		start = System.nanoTime();
//		
//		while (itIs2.hasNext() && (auxTrans2.size() != 0))  { // Searching for the cover
//			auxCode2 = itIs2.next(); 
//				// we check all the codes that are not singletons
//			if (auxTrans2.containsAll(auxCode2)) {
//				result2.add(auxCode2); 
//				auxTrans2 = auxTrans2.substraction(auxCode2); 
//			}
//		}
//		
//		// adding the codes that appear in the transaction but not in the code table
//		if(! auxTrans2.isEmpty()) {
//			for(int item : auxTrans2) {
//				KItemset sinlgton = Utils.createCodeSingleton(item, 0, 1);
//				result2.add(sinlgton);
//				this._codetable.addSingleton(sinlgton);
//			}
//		}
//		logger.debug("old approach: "+(((double)System.nanoTime()-start)/(double)1000000)+ " ms.");
//		logger.debug(result2);
		
		
		return result; 
	}
	
	/** 
	 * Codifying function that is aware of the fact that there might be part of the transaction 
	 * that is not being covered by the codes contained in the code table. 
	 * 
	 *  It returns the codes used in the partial coverage, and the set of items (singletons) 
	 *  that have not been covered. 
	 * 
	 * @param trans
	 * @return
	 */
	
	private Couple<ItemsetSet, KItemset> codifyAware (KItemset trans)  {
		
		KItemset auxTrans = new KItemset(trans);
		ItemsetSet result = new ItemsetSet(); 
		Iterator<KItemset> itIs = this._codetable.codeIterator();
		KItemset auxCode = null; 
		boolean lengthOneReached = false;

		while (itIs.hasNext() && (auxTrans.size() != 0) && (!lengthOneReached))  { // Searching for the cover
			auxCode = itIs.next(); 
			if (auxCode.size()!=1) {
				// we check all the codes that are not singletons
				if (auxTrans.containsAll(auxCode)) {
					result.add(auxCode); 
					auxTrans = auxTrans.substraction(auxCode); 
				}
			}
			else {
				lengthOneReached = true;
			}
		}
		// now we treat all the remaining elements in auxTrans as 
		// singletons

		if (!auxTrans.isEmpty()) {
			KItemset remainingSingletons = new KItemset(auxTrans);
			HashMap<Integer, KItemset> oneLength = this._codetable.getOneLengthCodes();
 			for(int item : remainingSingletons) {
				if (oneLength.containsKey(item)) {
					auxTrans=auxTrans.substraction(oneLength.get(item));
					result.add(oneLength.get(item));
				}
				// auxTrans keep the singletons that haven't been found 
			}
		}
		
		// currently, trans can be non-empty
		// we return both the codes used, and the remaining non-covered part of the transaction (new items) 
		return new Couple<ItemsetSet, KItemset>(result, auxTrans); 
	}

	/** 
	 * Length of the codification of a set of transactions using this code table
	 * It uses exactly the same cover order (it does not update either the support or the usage of the elements in the table) 
	 */
	public double codificationLength () {
		double result = 0.0;
		result = this._transactions.parallelStream().mapToDouble(e ->transactionCodificationLength(e)).sum();
		
		// nonparallel for debugging purposes 
//		result = this._transactions.stream().mapToDouble(e ->transactionCodificationLength(e)).sum();

		return result; 
	}
	
	public double codificationLengthExternal(ItemsetSet transactions) { 
		double result = 0.0; 
		result = transactions.parallelStream().mapToDouble(e -> transactionCodificationLength(e)).sum(); 
		logger.debug("Total: "+result);
		return result; 
	}
	
	
	public double codificationLengthAccordingSCT () {
		double result = 0.0;
		result = this._transactions.parallelStream().mapToDouble(e ->transactionCodificationLengthAccordingSCT(e)).sum();
		
		// nonparallel for debugging purposes 
//		result = this._transactions.stream().mapToDouble(e ->transactionCodificationLength(e)).sum();

		return result; 
	}
	
	public double codificationLengthAccordingSCTExternal (ItemsetSet transactions) {
		double result = 0.0;
		result = transactions.parallelStream().mapToDouble(e ->transactionCodificationLengthAccordingSCT(e)).sum();
		logger.debug("Total: "+result);
		// nonparallel for debugging purposes 
//		result = this._transactions.stream().mapToDouble(e ->transactionCodificationLength(e)).sum();

		return result; 
	}
	
	
	
	public double transactionCodificationLength (KItemset it) {
		ItemsetSet codes = null; 
		double result = 0.0; 
		codes = this.codify(it); 
		
		for (KItemset code: codes) {
			double codelength = codeLengthOfcode(this._codetable, code);
			try {
				assert ! Double.isInfinite(codelength);
			}
			catch (AssertionError e) {
				logger.debug(code + " codelength: "+codelength);
				System.exit(-1);
			}
			result += codelength; 
		}
		return result; 
	}
	
	public double transactionCodificationLengthAccordingSCT  (KItemset it) { 
		double result = 0.0; 
		if (!this._codetable.isStandard()) {
			for (Integer id: it.getItems()) { 
				double codelength = codeLengthOfcode(this._codetable.getStandardCodeTable(), 
											this._codetable.getStandardCodeTable().getOneLengthCodes().get(id));
				try {
					assert ! Double.isInfinite(codelength);
				}
				catch (AssertionError e) {
					logger.debug(id + " codelength: "+codelength);
					System.exit(-1);
				}
				result += codelength; 				
			}		
		}
		return result; 
	}
	
	/* only to be applied to non-stc code tables */ 
	
	public void applyLaplaceSmoothingToUsages () {
			
		HashSet<Integer> addedItems = new HashSet<Integer>(); 
		ArrayList<KItemset> addedSingletons = new ArrayList<KItemset>(); 
		
		// we now add the singletons that might not have been seen in the new database
		Integer currentItem = null;
		HashMap<Integer, KItemset> oneLength = this._codetable.getOneLengthCodes();
//		logger.debug("List of items: "+ this._transactions.knownItems()); 
	    for (Iterator<Integer> itemIter = this._transactions.knownItems().iterator(); itemIter.hasNext(); ){
	    	currentItem = itemIter.next(); 
	    	if (!addedItems.contains(currentItem)) { 
		    	if (!oneLength.containsKey(currentItem) ) {
		    		KItemset singleton = Utils.createCodeSingleton(currentItem, 0, 1); 
		    		addedSingletons.add(singleton); 
		    		addedItems.add(currentItem); 
		    	}
		    	else { 
		    		// the items used in the transactions might be exposed, and not being used by any upper code
		    		KItemset aux = oneLength.get(currentItem); 
		    		aux.setUsage(aux.getUsage()+1);
		    	}
	    	} 
	    }	
	    
	   
//	    logger.debug("List of new items: "+addedItems);
	    
	    if (!addedItems.isEmpty()) {
	    	
	    	// first of all, we add them 
	    	// before it was done one by one ... with an ordering op for each of them
//	    	StringBuilder strBld = new StringBuilder(); 
//			long timestamp = System.nanoTime();
//			strBld.append(timestamp); 
//			strBld.append("before adding singletons");
//			strBld.append(this._codetable.toString()); 
	    	this._codetable.addSingletons(addedSingletons);
//	    	strBld.append(timestamp); 
//			strBld.append("after adding singletons");
//			strBld.append(this._codetable.toString()); 
//	    	logger.debug(strBld.toString());
	    	// we have to apply +1 to all the ones of not length 1 that were already in the code table
		    for (KItemset key: this._codetable.getCodes()) {
		    	if (key.size() > 1) { 
		    		key.setUsage(key.getUsage()+1);
		    	} 
			}
		    
		    // we have also to update the codes in the STC 
		    // its a little bit tricky, as we don't have to 
		    // update the ones exposed (as they are already well updated in the STC)
		    CodeTable stc = this._codetable.getStandardCodeTable();
		    stc.getOneLengthCodes().keySet().forEach(key -> { 
		    	int currentValue = stc.getOneLengthCodes().get(key).getUsage(); 
		    	assert stc.getOneLengthCodes().get(key).getUsage() == 
		    			stc.getOneLengthCodes().get(key).getSupport(); 
		    	
		    	currentValue++; 
		    	stc.getOneLengthCodes().get(key).setUsage(currentValue);
		    	stc.getOneLengthCodes().get(key).setSupport(currentValue);
		    });
		    stc.addSingletons(addedSingletons);
		    stc.recomputeUsageTotal();
	    }
	    
		this._codetable.recomputeUsageTotal(); 
	}
	
	public double codetableCodeLength() {
		return codeTableCodeLength(_codetable);
	}
	
	
	public double codeLengthOfCode(KItemset code) {
		return codeLengthOfcode(_codetable, code);
	}
	
	public String toString() {
		
		// StringBuilder copied from smpf code, just to see ...
		StringBuilder r = new StringBuilder ();
		r.append("Total Usages: ");
		r.append(this._codetable.getUsageTotal());
		r.append('\n');
		Iterator<KItemset> itIs = this._codetable.codeIterator();
		while(itIs.hasNext()) {
			KItemset is = itIs.next();
			r.append(is.toString());
			r.append(" s:"); 
			r.append(is.getSupport()); 
			r.append(" u:");
			r.append(is.getUsage());
			r.append(" P:");
			r.append(probabilisticDistrib(this._codetable, is));
			r.append(" L:");
			r.append(codeLengthOfcode(this._codetable, is));
			r.append('\n');
		}
		return r.toString();
	}
	
	public ArrayList<Couple<KItemset, ItemsetSet>> codificationsExternal (ItemsetSet toCodify) {
		ArrayList<Couple<KItemset, ItemsetSet>> result = new ArrayList<>(); 
		for (KItemset trans: toCodify) {
			result.add(new Couple<>(trans, this.codify(trans))); 
		}
		return result;
	}
	
}
