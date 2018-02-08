///////////////////////////////////////////////////////////////////////////////
//File: ChangeSetFile.java 
//Author: Pierre Maillot
//Date: December 2017
//Comments: Class that stores the information about an update file
//Modifications:
///////////////////////////////////////////////////////////////////////////////

package com.irisa.dbplharvest.data;

import com.irisa.utilities.Couple;

public class ChangesetFile extends Couple<String, String> implements AbstractChangeset {
	
	public static final String ADDED_EXTENSION =  ".added.nt.gz";
	public static final String DELETED_EXTENSION =  ".removed.nt.gz";

	protected String _year = "";
	protected String _month = "";
	protected String _day = "";
	protected String _hour = "";
	protected String _number = "";
	
	public ChangesetFile(String year, String month, String day, String hour, String number) {
		this._year = year;
		this._month = month;
		this._day = day;
		this._hour = hour;
		this._number = number;
	}
	
	public ChangesetFile(String year, String month, String day, String hour, String number, String fileAdd, String fileDelete) {
		super(fileAdd, fileDelete);
		this._year = year;
		this._month = month;
		this._day = day;
		this._hour = hour;
		this._number = number;
	}

	public ChangesetFile(String fileAdd, String fileDelete) {
		super(fileAdd, fileDelete);
	}
	
	public String getAddFile() {
		return this.getFirst();
	}
	
	public String getDelFile() {
		return this.getSecond();
	}
	
	public void setAddFile(String addFile) {
		this.setFirst(addFile);
	}
	
	public void setDelFile(String delFile) {
		this.setSecond(delFile);
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

}
