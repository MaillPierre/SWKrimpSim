package com.irisa.swpatterns.measures;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.irisa.exception.LogicException;
import com.irisa.jenautils.Couple;
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
		this._codetable = new CodeTable(_codetable);
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
		if(ct.contains(code)) {
			try {
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
				
				logger.debug("codeTableCodeLength: CTlength=" + cL + " stcL=" + stcL + " code=" + code );
				result += cL + stcL;
			}
		}
		return result;
	}

	
	/** 
	 * L(code_ST(X))
	 */
	private static double codeLengthOfCodeAccordingST(CodeTable ct, KItemset code) {
		double result = 0.0;
		// this method should return 0 if the codetable is the ST
		if (! ct.isStandard()) {
			Iterator<Integer> itCode = code.iterator();
			while(itCode.hasNext()) {
				Integer item = itCode.next();
				double itemCodeLength = codeLengthOfcode(ct.getStandardCodeTable(), Utils.createCodeSingleton(item));
				logger.debug("codeLengthOfCodeAccordingST: codelength=" + itemCodeLength + " item=" + item);
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
				
		Iterator<KItemset> itCodes = this._codetable.codeIterator();
		while(itCodes.hasNext()) {
			KItemset code = itCodes.next();
			code.setUsage(0); 
		}
		
		for (KItemset t: this._transactions) {
			ItemsetSet codes = this.codify(t); 
			for (KItemset code: codes) {
				code.setUsage(code.getUsage()+1);
			}
		}
		
		this._codetable.recomputeUsageTotal();
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
		while (itIs.hasNext() && (auxTrans.size() != 0) ) {
			auxCode = itIs.next(); 
			if (auxTrans.containsAll(auxCode)) {
				result.add(auxCode); 
				auxTrans = auxTrans.substraction(auxCode); 
			}
		}
		assert trans.size() == 0; // this should always happen 
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
		ItemsetSet codes = null; 
		for (KItemset it: this._transactions) {
			codes = this.codify(it); 
			for (KItemset code: codes) {
				result += codeLengthOfcode(this._codetable, code); 
			}
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
	
	
	
	
	
}
