///////////////////////////////////////////////////////////////////////////////
//File: UpdateTransactionsFile.java 
//Author: Carlos Bobed
//Date: February 2018
//Comments: Class that stores the information about an updated transactions file
//Modifications:
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data;

import java.nio.file.Files;
import java.nio.file.Paths;

public class UpdateTransactionsFile implements AbstractChangeset {
	
	public static final String TRANSACTIONS_EXTENSION = ".trans"; 
	public static final String EMPTY_TRANSACTIONS_EXTENSION = ".noTrans"; 

	protected String _year = "";
	protected String _month = "";
	protected String _day = "";
	protected String _hour = "";
	protected String _number = "";
	
	protected String _baseFilename = "";
	
	public UpdateTransactionsFile(String year, String month, String day, String hour, String number) {
		this._year = year;
		this._month = month;
		this._day = day;
		this._hour = hour;
		this._number = number;
	}
	
	public UpdateTransactionsFile(String year, String month, String day, String hour, String number, String baseFilename) {
		
		this._year = year;
		this._month = month;
		this._day = day;
		this._hour = hour;
		this._number = number;
		
		this._baseFilename = baseFilename; 
	}

	public UpdateTransactionsFile(String baseFilename) {
		this._baseFilename = baseFilename; 
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

	public boolean isEmptyTrans() { 
		return Files.exists(Paths.get(this._baseFilename+EMPTY_TRANSACTIONS_EXTENSION)); 
	}
	public boolean isNonEmptyTrans() { 
		return Files.exists(Paths.get(this._baseFilename+TRANSACTIONS_EXTENSION)); 
	}

	public String getBaseFilename() {
		return _baseFilename;
	}
	
	
	
}
