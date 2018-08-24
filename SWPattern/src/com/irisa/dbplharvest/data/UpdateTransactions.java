///////////////////////////////////////////////////////////////////////////////
//File: UpdateTransactions.java 
//Author: Carlos Bobed
//Date: February 2018
//Comments: Class that loads the transactions grouped by update result of the 
// 		modelEvolver
//Modifications:///////////////////////////////////////////////////////////////////////////////


package com.irisa.dbplharvest.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.irisa.krimp.data.ItemsetSet;
import com.irisa.krimp.data.KItemset;
import com.irisa.utilities.Couple;

import org.apache.log4j.Logger;


public class UpdateTransactions implements AbstractChangeset {

	private static Logger logger = Logger.getLogger(UpdateTransactions.class);
	public enum ParserStatus { BEFORE_UPDATES, AFTER_UPDATES }; 
	
	protected String _year;
	protected String _month;
	protected String _day;
	protected String _hour;
	protected String _number;
	
	// the set of updates of transactions is a list of 
	// couples <q, q'> with q and q' being the set
	// of transactions corresponding to the connected set 
	// of resources affected by an update before and after 
	// of the update
	ArrayList<Couple<ItemsetSet, ItemsetSet>> _updateTransactions = null;  
	

	public UpdateTransactions() {
	}
	
	public UpdateTransactions(String year, String month, String day, String hour, String number) {
		this._year = year;
		this._month = month;
		this._day = day;
		this._hour = hour;
		this._number = number;
	}
	
	public UpdateTransactions(UpdateTransactionsFile upFile) {
		this.setYear(upFile.getYear());
		this.setMonth(upFile.getMonth());
		this.setDay(upFile.getDay());
		this.setHour(upFile.getHour());
		this.setNumber(upFile.getNumber());
		logger.debug("reading "+upFile.getBaseFilename());
		if (upFile.isNonEmptyTrans()) { 
			this.readFile(upFile.getBaseFilename()+UpdateTransactionsFile.TRANSACTIONS_EXTENSION); 
		}
		else if (!upFile.isEmptyTrans()) { 
			System.err.println("something is wrong ... no files for this update"); 
		}
	}
	
	public void readFile(String filePath) {
		try { 
			BufferedReader in = Files.newBufferedReader(Paths.get(filePath)); 
			ItemsetSet beforeTransactions = new ItemsetSet (); 
			ItemsetSet afterTransactions = new ItemsetSet ();
			this._updateTransactions = new ArrayList<Couple<ItemsetSet,ItemsetSet>>(); 
			String line = null;
			ParserStatus status = ParserStatus.BEFORE_UPDATES; 
			// mini stupid parser
			while ( (line = in.readLine()) != null ) {
				switch (status) { 
					case BEFORE_UPDATES:
						if (!line.startsWith("----")) { 
							beforeTransactions.add(new KItemset(line)); 
						}
						else { 
							status = ParserStatus.AFTER_UPDATES; 
						}
						break; 
					case AFTER_UPDATES: 
						if (!"".equals(line)) { 
							afterTransactions.add(new KItemset(line)); 
						}
						else { 
							this._updateTransactions.add(new Couple(beforeTransactions, afterTransactions)); 
							beforeTransactions = new ItemsetSet(); 
							afterTransactions = new ItemsetSet(); 
							status = ParserStatus.BEFORE_UPDATES; 
						}
						break; 
				}
			}
			in.close();
			// we should always reach this point with both arrayLists empty
			assert beforeTransactions.isEmpty() && afterTransactions.isEmpty(); 
			
		} 
		catch (IOException e) { 
			e.printStackTrace();
			logger.debug("something wrong reading "+filePath); 
			this._updateTransactions = null; 
		}
		
	}


	public void writeFile (String filePath) { 
		try { 
			PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(filePath)));
			
			for (Couple<ItemsetSet, ItemsetSet> update: this._updateTransactions ) { 
				for (KItemset first: update.getFirst()) { 
					out.println(first.toString()); 
				}
				out.println(ModelEvolver.STATES_SEPARATOR); 
				for (KItemset second: update.getSecond()) { 
					out.println(second.toString()); 
				}
				out.println(); 
			}
			
			out.flush();
			out.close();
		} 
		catch (IOException e) { 
			e.printStackTrace();
			logger.debug("something wrong writing "+filePath); 
		}
	}
	

	@Override
	public String getYear() {
		return this._year;
	}

	@Override
	public void setYear(String year) {
		this._year = year;
	}

	@Override
	public String getMonth() {
		return this._month;
	}

	@Override
	public void setMonth(String month) {
		this._month = month;
	}

	@Override
	public String getDay() {
		return this._day;
	}

	@Override
	public void setDay(String day) {
		this._day = day;
	}

	@Override
	public String getHour() {
		return this._hour;
	}

	@Override
	public void setHour(String hour) {
		this._hour = hour;
	}

	@Override
	public String getNumber() {
		return this._number;
	}

	@Override
	public void setNumber(String number) {
		this._number = number;
	}
	
	public boolean isEmpty() { 
		if (this._updateTransactions != null) { 
			return this._updateTransactions.isEmpty(); 
		}
		else { 
			return true; 
		}
	}
	
	public ArrayList<Couple<ItemsetSet, ItemsetSet>> getUpdateTransactions() {
		return _updateTransactions;
	}
	
	public String getDBpediaLiveID() { 
		StringBuilder strBldr = new StringBuilder(); 
		strBldr.append(this._year); 
		strBldr.append("-");
		strBldr.append(this._month);
		strBldr.append("-");
		strBldr.append(this._day);
		strBldr.append("-");
		strBldr.append(this._hour);
		strBldr.append("-");
		strBldr.append(this._number);
		return strBldr.toString(); 
	}
}
