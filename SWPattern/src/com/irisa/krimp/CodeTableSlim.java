package com.irisa.krimp;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.DataIndexes;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;

public class CodeTableSlim extends CodeTable {
	
	private HashMap<KItemset, BitSet> _itemsetUsageVector = new HashMap<KItemset, BitSet>();

	public CodeTableSlim(ItemsetSet transactions, ItemsetSet codes, DataIndexes analysis) {
		super(transactions, codes, analysis);
	}
	
	public CodeTableSlim(ItemsetSet transactions, ItemsetSet codes, DataIndexes analysis, boolean standardFlag) {
		super(transactions, codes, analysis, standardFlag);
	}
	
	/**
	 * Return a boolean vector representing the transaction the code is support of
	 * @return
	 */
	public BitSet getUsageVector(KItemset code) {
		return this._itemsetUsageVector.get(code);
	}
	
	/**
	 * Gives an estimation of the usage of the combinaison of 2 codes according to the SLIM paper
	 * @param x 
	 * @param y
	 * @return
	 */
	public int estimateUsageCombination(KItemset x, KItemset y) {
		BitSet estimate = new BitSet();
		estimate.or(this.getUsageVector(x));
		estimate.and(this.getUsageVector(y));
		return estimate.cardinality();
	}
	
	public double estimateProbabilisticDistrib(KItemset x, KItemset y) {
		return (double) this.estimateUsageCombination(x,y) / (double) this._usageTotal;
	}
	
	/**
	 * L(code_CT(X))
	 * @param code
	 * @return
	 */
	public double estimateCodeLengthOfcode(KItemset x, KItemset y) {
		return - Math.log(this.estimateProbabilisticDistrib(x,y));
	}
	
	public double estimateEncodedTransactionSetCodeLength(KItemset x, KItemset y) {
		return encodedTransactionSetCodeLength() - estimateCodeLengthOfcode(x, y);
	}
	
	/**
	 * L(CT|D)
	 * @return
	 */
	public double estimateCodeTableCodeLength(KItemset x, KItemset y) {
		double result = 0.0;
		Iterator<KItemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			KItemset code = itCodes.next();
			if(this.getUsage(code) != 0.0) {
				// CB: this is the code length according to the CT
				double cL = codeLengthOfcode(code);
				
				// CB: we also need the code length according to the ST: we codify the codeusing it
				double stcL = 0 ;
				if (!_standardFlag) {
					stcL = this._standardCT.codeLengthOfCodeAccordingST(code);
				}
				// else => it is a 0.0
				
				
				result += cL + stcL;
			}
		}
		result+=this.estimateCodeLengthOfcode(x, y);
		return result;
	}
	
	/**
	 * L(D, CT)
	 * @return
	 * @throws LogicException 
	 */
	public double estimateTotalCompressedSize(KItemset x, KItemset y) throws LogicException {
		double ctL = estimateCodeTableCodeLength(x, y);
		double teL = estimateEncodedTransactionSetCodeLength(x, y);
		return ctL + teL;
	}
	
	@Override
	/**
	 * Initialize the usage of each code according to the cover
	 * PRE: the codeTable must be in standardCoverTable order
	 */
	public void updateUsages() {
		this._usageTotal = 0;
		
		Iterator<KItemset> itCodes = this.codeIterator();
		while(itCodes.hasNext()) {
			KItemset code = itCodes.next();
			_itemsetUsage.replace(code, 0); 
			_itemsetUsageVector.replace(code, new BitSet(this._index.getNumberOfTransactions()));
		}
		
		for (int i = 0; i < this._transactions.size(); i++) {
			KItemset t = this._transactions.get(i);
			ItemsetSet codes = this.codify(t); 
			for (KItemset aux: codes) {
				_itemsetUsage.replace(aux, _itemsetUsage.get(aux)+1); 
				_itemsetUsageVector.get(aux).set(i);
			}
			this._usageTotal+=codes.size(); 
		}		
	}

}
