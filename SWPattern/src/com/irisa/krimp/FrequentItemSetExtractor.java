package com.irisa.krimp;

import java.io.IOException;
import org.apache.log4j.Logger;

import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.Utils;
import ca.pfv.spmf.algorithms.frequentpatterns.fin_prepost.FIN;
import ca.pfv.spmf.algorithms.frequentpatterns.fin_prepost.PrePost;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPClose;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPMax;
import ca.pfv.spmf.algorithms.frequentpatterns.relim.AlgoRelim;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * Connection to the SMPF "library" or any other way to extract frequent itemsets from a transaction database
 * 
 * @author pmaillot
 *
 */
public class FrequentItemSetExtractor {

	private static Logger logger = Logger.getLogger(FrequentItemSetExtractor.class);

	protected ALGORITHM _algo = ALGORITHM.FPClose;

	protected static String tmpTransactionFilename = "transactions.tmp";
	protected static String tmpItemsetFilename = "itemsets.tmp";
	
	public enum ALGORITHM {
		FPMax,
		FPClose,
		FPGrowth,
		Relim,
		FIN,
		PrePost
	}

	public FrequentItemSetExtractor() {
	}

	public Itemsets computeItemsets(ItemsetSet is) {
		return computeItemsets(is.toItemsets());
	}

	public Itemsets computeItemsets(Itemsets transactions) {
		switch(this._algo) {
		case FPClose:
			logger.debug("Compute Frequent Closed Itemsets with FPClose");
			return computeItemSet_FPClose(transactions);
		case FPMax:
			logger.debug("Compute Frequent Maximal Itemsets with FPMax");
			return this.computeItemSet_FPMax(transactions);
		case FIN:
			logger.debug("Compute Frequent Itemsets with FIN");
			return this.computeItemSet_FIN(transactions);
		case Relim:
			logger.debug("Compute Frequent Itemsets with Relim");
			return this.computeItemSet_Relim(transactions);
		case FPGrowth:
			logger.debug("Compute Frequent Itemsets with FPGrowth");
			return this.computeItemSet_FPGrowth(transactions);
		case PrePost:
			logger.debug("Compute Frequent Itemsets with PrePost");
			return this.computeItemSet_PrePost(transactions);
		default:
			return null;
		}
	}

	public boolean algoFPClose() {
		return this._algo == ALGORITHM.FPClose;
	}

	public void setAlgoFPClose() {
		this._algo = ALGORITHM.FPClose;
	}

	public boolean algoFPMax() {
		return this._algo == ALGORITHM.FPMax;
	}

	public void setAlgoFPMax() {
		this._algo = ALGORITHM.FPMax;
	}

	public boolean algoRelim() {
		return this._algo == ALGORITHM.Relim;
	}

	public void setAlgoRelim() {
		this._algo = ALGORITHM.Relim;
	}

	public boolean algoFIN() {
		return this._algo == ALGORITHM.FIN;
	}

	public void setAlgoFIN() {
		this._algo = ALGORITHM.FIN;
	}

	public boolean algoFPGrowth() {
		return this._algo == ALGORITHM.FPGrowth;
	}

	public void setAlgoFPGrowth() {
		this._algo = ALGORITHM.FPGrowth;
	}

	public boolean algoPrePost() {
		return this._algo == ALGORITHM.PrePost;
	}

	public void setAlgoPrePost() {
		this._algo = ALGORITHM.PrePost;
	}

	public Itemsets computeItemSet_FPGrowth(Itemsets is) {
		try {
			Utils.printTransactions(is, tmpTransactionFilename);

			return computeItemSet_FPGrowth(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPGrowth(String input) {
		try {
			AlgoFPGrowth algoFpc = new AlgoFPGrowth();
			logger.debug("FPGrowth Algorithm");
			Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(input, null, 0.0);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPClose(Itemsets is) {
		try {
			Utils.printTransactions(is, tmpTransactionFilename);

			return computeItemSet_FPClose(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPClose(String input) {
		try {
			AlgoFPClose algoFpc = new AlgoFPClose();
			logger.debug("FPClose Algorithm");
			Itemsets fpcResult;
			fpcResult = algoFpc.runAlgorithm(input, null, 0.0);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPMax(Itemsets transactions) {
		try {
			Utils.printTransactions(transactions, tmpTransactionFilename);
			return this.computeItemSet_FPMax(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FPMax(String input) {
		try {
			logger.debug("FPMax Algorithm");
			AlgoFPMax algoFpc = new AlgoFPMax();
			Itemsets fpcResult = algoFpc.runAlgorithm(input, null, 0.0);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_PrePost(Itemsets transactions) {
		try {
			Utils.printTransactions(transactions, tmpTransactionFilename);
			return this.computeItemSet_PrePost(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_PrePost(String input) {
		try {
			logger.debug("PrePost+ Algorithm");
			Itemsets fpcResult;
			PrePost algoFpc = new PrePost();
			algoFpc.setUsePrePostPlus(true);
			algoFpc.runAlgorithm(input, 0.0, tmpItemsetFilename);
			fpcResult = Utils.readItemsetFile(tmpItemsetFilename);

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_Relim(String input) {
		try {
			AlgoRelim algoFpc = new AlgoRelim();
			logger.debug("Relim Algorithm");
			Itemsets fpcResult;
			algoFpc.runAlgorithm(0.0, input, tmpItemsetFilename);
			fpcResult = Utils.readItemsetFile(tmpItemsetFilename);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_Relim(Itemsets input) {
		try {
			Utils.printTransactions(input, tmpTransactionFilename);
			return this.computeItemSet_Relim(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FIN(Itemsets input) {
		try {
			Utils.printTransactions(input, tmpTransactionFilename);
			return this.computeItemSet_FIN(tmpTransactionFilename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Itemsets computeItemSet_FIN(String input) {
		try {
			FIN algoFpc = new FIN();
			logger.debug("FIN Algorithm");
			Itemsets fpcResult;
			algoFpc.runAlgorithm(input, 0.0, tmpItemsetFilename);
			fpcResult = Utils.readItemsetFile(tmpItemsetFilename);
//			fpcResult.printItemsets(fpcResult.getItemsetsCount());

			return fpcResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


}
