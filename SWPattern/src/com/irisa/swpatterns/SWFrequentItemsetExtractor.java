package com.irisa.swpatterns;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.irisa.krimp.FrequentItemSetExtractor;
import com.irisa.swpatterns.data.AttributeIndex;
import com.irisa.swpatterns.data.LabeledTransactions;

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPMax;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class SWFrequentItemsetExtractor extends FrequentItemSetExtractor {
	
	private static Logger logger = Logger.getLogger(SWFrequentItemsetExtractor.class);

	public SWFrequentItemsetExtractor() {
		super();
	}

	public Itemsets computeItemsets(LabeledTransactions transactions, AttributeIndex index) {
		return computeItemsets(index.convertToTransactions(transactions));
	}


	public Itemsets computeItemSet_FPGrowth(LabeledTransactions transactions, AttributeIndex index) {
		try {
			index.printTransactionsItems(transactions, tmpTransactionFilename);

			return computeItemSet_FPGrowth(tmpTransactionFilename);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPClose(LabeledTransactions transactions, AttributeIndex index) {
		try {
			index.printTransactionsItems(transactions, tmpTransactionFilename);

			return computeItemSet_FPClose(tmpTransactionFilename);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPMax(LabeledTransactions transactions, AttributeIndex index) {
		try {
			AlgoFPMax algoFpc = new AlgoFPMax();
			logger.debug("FPMax Algorithm");
			index.printTransactionsItems(transactions, tmpTransactionFilename);
			Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(tmpTransactionFilename, null, 0.0);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_PrePost(LabeledTransactions transactions, AttributeIndex index) {
		try {
			index.printTransactionsItems(transactions, tmpTransactionFilename);
			return computeItemSet_PrePost(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_Relim(LabeledTransactions input, AttributeIndex index) {
		try {
			index.printTransactionsItems(input, tmpTransactionFilename);
			return this.computeItemSet_Relim(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FIN(LabeledTransactions input, AttributeIndex index) {
		try {
			index.printTransactionsItems(input, tmpTransactionFilename);
			return this.computeItemSet_FIN(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
