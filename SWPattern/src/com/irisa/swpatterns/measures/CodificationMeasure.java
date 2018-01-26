package com.irisa.swpatterns.measures;

import java.util.ArrayList;
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
		if(codeUsage == 0 && ct.contains(code)) {
			try { // Using a bit of code using java8 and lambda stuff found on the web
			Optional<KItemset> value = ct.getCodes()
	            .stream()
	            .filter(a -> a.equals(code))
	            .findFirst();
			codeUsage = value.get().getUsage();
			} catch(NoSuchElementException e) {
				logger.error("Unable to retrieve probabilistic distribution of " + code +  " from the codetable.");
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
		return - Math.log(probabilisticDistrib(ct, code));
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
		for (Integer codeInt: this._transactions.knownItems()) {
			
			Optional<KItemset> value = this._codetable.getCodes()
	            .stream()
	            .filter(a -> a.equals(Utils.createCodeSingleton(codeInt)))
	            .findFirst();
			if (!value.isPresent()) {
				// we create them with support 0 on purpose to them being 
				// added to the end of the code table in standard cover order
				newOnes.add(Utils.createCodeSingleton(codeInt, 0,1)); 
			}						
		}
		newOnes.stream().forEach(e -> this._codetable.addSingleton(e));
		logger.debug("Singletons added: "+newOnes.size());
		logger.debug("---");
		logger.debug(this._codetable.toString());
		this._codetable.orderCodesStandardCoverageOrder();
		// we should be now safe 
		
		this._codetable.getCodes().stream().forEach(e -> e.setUsage(0));
		this._transactions.parallelStream().forEach(e -> this.updateUsagesTransaction(e)); 		
		this._codetable.recomputeUsageTotal();
	}
	
	public void updateUsagesTransaction (KItemset transaction) {
		ItemsetSet codes = this.codify(transaction);
//		logger.debug(transaction + " covered by "+codes.size()); 
		for (KItemset code: codes) { 
			int codeUsage = code.getUsage(); 
			code.incrementUsageAtomically(); 
			int codeUsageAfter = code.getUsage(); 
//			logger.debug("b4: "+codeUsage+" after: "+codeUsageAfter);
		}
	}
	
	
	/** 
	 * Codifying function according to the KRIMP paper
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
			for(int item : remainingSingletons) {
				KItemset singleton = Utils.createCodeSingleton(item, 0, 1);
				if (this._codetable.contains(singleton)) {
					auxTrans = auxTrans.substraction(singleton);
					result.add(singleton);
				}
			}
		}
		
		// adding the codes that appear in the transaction but not in the code table
		if(! auxTrans.isEmpty()) {
			for(int item : auxTrans) {
				KItemset sinlgton = Utils.createCodeSingleton(item, 0, 1);
				result.add(sinlgton);
				this._codetable.addSingleton(sinlgton);
			}
		}
		auxTrans.clear();
		
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
		while (itIs.hasNext() && (auxTrans.size() != 0) ) {
			auxCode = itIs.next(); 
			if (auxTrans.containsAll(auxCode)) {
				result.add(auxCode); 
				auxTrans = auxTrans.substraction(auxCode); 
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
		return result; 
	}
	
	public double transactionCodificationLength (KItemset it) {
		ItemsetSet codes = null; 
		double result = 0.0; 
		codes = this.codify(it); 
	
		for (KItemset code: codes) {
			double codelength = codeLengthOfcode(this._codetable, code);
//			logger.debug(code + " codelength: "+codelength);
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
	
	
	public void applyLaplaceSmoothingToUsages () {
		
		for (KItemset key: this._codetable.getCodes()) {
			key.setUsage(key.getUsage()+1);
		}
		
		// we now add the singletons that might not have been seen in the new database
		Integer currentItem = null;
		KItemset currentKey = null; 
	    for (Iterator<Integer> itemIter = this._transactions.knownItems().iterator(); itemIter.hasNext(); ){
	    	currentItem = itemIter.next(); 
	    	currentKey = Utils.createCodeSingleton(currentItem); 
	    	
	    	if (!this._codetable.contains(currentKey)) {
	    		currentKey.setUsage(1);
	    		this._codetable.addSingleton(currentKey); 
	    	}
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
	
	
	
	
}
