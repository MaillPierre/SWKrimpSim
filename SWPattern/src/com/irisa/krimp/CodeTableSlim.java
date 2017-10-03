package com.irisa.krimp;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.irisa.exception.LogicException;
import com.irisa.krimp.data.DataIndexes;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.krimp.data.Utils;

public class CodeTableSlim extends CodeTable {
	
	private static Logger logger = Logger.getLogger(CodeTableSlim.class);

	protected HashMap<KItemset, BitSet> _itemsetUsageVector = new HashMap<KItemset, BitSet>();

	public CodeTableSlim(ItemsetSet transactions, DataIndexes analysis) {
		this(transactions, analysis, false);
		logger.debug("CodeTableSlim(ItemsetSet transactions, DataIndexes analysis)");
	}
	
	public CodeTableSlim(ItemsetSet transactions, DataIndexes analysis, boolean standardFlag) {
		super(transactions, new ItemsetSet(), analysis, standardFlag);
		logger.debug("CodeTableSlim(ItemsetSet transactions, DataIndexes analysis, boolean standardFlag)");
	}
	
	public CodeTableSlim(ItemsetSet transactions, boolean standardFlag) {
		this(transactions, new DataIndexes(transactions), standardFlag);
		logger.debug("CodeTableSlim(ItemsetSet transactions, boolean standardFlag)");
	}
	
	public CodeTableSlim(ItemsetSet transactions) {
		this(transactions, new DataIndexes(transactions), false);
		logger.debug("CodeTableSlim(ItemsetSet transactions)");
	}
	
	public CodeTableSlim(CodeTable ct) {
		super(ct);
		if(ct instanceof CodeTableSlim) {
			this._itemsetUsageVector = new HashMap<KItemset, BitSet>(((CodeTableSlim) ct)._itemsetUsageVector);
		} else {
			this._itemsetUsageVector = new HashMap<KItemset, BitSet>();
		}
		
		
	}
	
	public static CodeTableSlim createStandardCodeTable(ItemsetSet transactions, DataIndexes analysis) {
		return new CodeTableSlim(transactions, analysis, true);
	}
	
	public static CodeTableSlim createStandardCodeTable(ItemsetSet transactions) {
		return new CodeTableSlim(transactions, new DataIndexes(transactions), true);
	}
	
	@Override
	protected void init() {
		_itemsetUsageVector = new HashMap<KItemset, BitSet>(); // Have to move that here to remove null pointer exception, dirty but ....
		initSingletonSupports();
		initCodes();
		orderCodesStandardCoverageOrder();
		updateUsages();		
	}
	
	@Override
	/**
	 * Create new indices for new codes, put the usage of each code to 0
	 */ 
	protected void initCodes() {
		this._codes.forEach(new Consumer<KItemset>() {
			@Override
			public void accept(KItemset code) {
				if(_itemsetCode.get(code) == null) {
					_itemsetCode.put(code, Utils.getAttributeNumber());
				}
				if(_itemsetUsage.get(code) == null) {
					_itemsetUsage.put(code, 0);
				}
				if(_itemsetUsageVector.get(code) == null) {
					_itemsetUsageVector.put(code, generateUsageVector(code));
				}
			}
		});
	}
	
	/**
	 * Return a boolean vector representing the transaction the code is support of
	 * @return
	 */
	public BitSet getUsageVector(KItemset code) {
		if(this._itemsetUsageVector.get(code) == null) {
			this._itemsetUsageVector.put(code, this.generateUsageVector(code));
			this._itemsetUsage.put(code, this._itemsetUsageVector.get(code).cardinality());
		}
		return this._itemsetUsageVector.get(code);
	}
	
	/**
	 * Gives an estimation of the usage of the combination of 2 codes according to the SLIM paper
	 * @param x 
	 * @param y
	 * @return
	 */
	public int estimateUsageCombination(KItemset x, KItemset y) {
		BitSet estimate = new BitSet();
		estimate.or(this.getUsageVector(x));
		estimate.and(this.getUsageVector(y));
//		logger.debug("estimateUsageCombination( " + x + " , " + y + " ): " + estimate.cardinality() );
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
//		logger.debug("estimateCodeLengthOfcode( " + x + ", " + y + " ) "+ this.estimateProbabilisticDistrib(x,y));
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
			_itemsetUsageVector.put(code, null);
		}
		
		for (int i = 0; i < this._transactions.size(); i++) {
			KItemset t = this._transactions.get(i);
			ItemsetSet codes = this.codify(t); 
			for (KItemset aux: codes) {
				_itemsetUsage.replace(aux, _itemsetUsage.get(aux)+1); 
//				_itemsetUsageVector.get(aux).set(i);
			}
			this._usageTotal+=codes.size(); 
		}		
	}
	
	private BitSet generateUsageVector(KItemset code) {
//		logger.debug("Usage vector for " + code + " has been generated");
		BitSet newUsageVector = new BitSet(this._index.getNumberOfTransactions());

		BitSet supportVector = this._index.getCodeTransactionVector(code);
		for(int i = supportVector.nextSetBit(0); i != -1; i = supportVector.nextSetBit(i+1)) {
			if(this.codify(this._transactions.get(i)).contains(code)) {
				newUsageVector.set(i);
			}
		}
		
		return newUsageVector;
	}
	
	public int getUsage(KItemset code) {
		if(this._itemsetUsage.get(code) == null) {
			BitSet usageVector = this.generateUsageVector(code);
			this._itemsetUsageVector.put(code, usageVector);
			this._itemsetUsage.put(code, usageVector.cardinality());
		}
		return this._itemsetUsage.get(code);
	}

}
