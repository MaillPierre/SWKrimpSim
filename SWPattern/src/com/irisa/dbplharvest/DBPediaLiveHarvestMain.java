package com.irisa.dbplharvest;

import java.io.File;
import java.util.LinkedList;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.irisa.dbplharvest.data.Changeset;
import com.irisa.dbplharvest.data.Changeset.CONTEXT_SOURCE;
import com.irisa.dbplharvest.data.ChangesetFile;

public class DBPediaLiveHarvestMain {

	private static final String EXTENSION_ADDED = ".added.nt.gz";
	private static final String EXTENSION_REMOVED = ".removed.nt.gz";
	private static final String EXTENSION_CLEAR = ".clear.nt.gz";
	private static final String EXTENSION_REINSERT = ".reinserted.nt.gz ";

	private static final int maximumMissingChangeset = 5;

	private static Logger logger = Logger.getLogger(DBPediaLiveHarvestMain.class);

	/**
	 * Extract updates and treat them.
	 * @param online true if the missing updates must be attempted to be fetched online
	 * @return
	 */
	public static LinkedList<ChangesetFile> harvest(boolean online) {
		return harvest(Integer.MAX_VALUE, online);
	}
	/**
	 * 
	 * @param maxChangesets max number of update to be extracted
	 * @param online true if the missing updates must be attempted to be fetched online
	 * @return
	 */
	public static LinkedList<ChangesetFile> harvest(int maxChangesets, boolean online) {
		return harvest(2016, 1, 1, 0, maxChangesets, online);
	}

	public static LinkedList<ChangesetFile> harvest(int year, int month, int day, int hour, int maxChangesets, boolean online) {
		LinkedList<ChangesetFile> result = new LinkedList<ChangesetFile>();

		String baseUrl = "http://live.dbpedia.org/changesets/";
		String localBasePath = "./datasets/";
		int tmpYear = year;
		String yearString = "" + tmpYear;
		for(int tmpMonth = month; (tmpMonth <= 12) && (result.size() < maxChangesets) ; tmpMonth++) {
			String monthString = "" + tmpMonth;
			if(tmpMonth < 10) {
				monthString = "0" + monthString;
			}

			for(int tmpDay = day; (tmpDay <= 31) && (result.size() < maxChangesets) ; tmpDay++) {
				String dayString = "" + tmpDay;
				if(tmpDay < 10) {
					dayString = "0" + dayString;
				}

				for(int tmpHour = hour; (tmpHour <= 23) && (result.size() < maxChangesets) ; tmpHour++) {
					String hourString = "" + tmpHour;
					if(tmpHour < 10) {
						hourString = "0" + hourString;
					}

					boolean noMoreChangeSet = false;
					int nbMissingChangeSet = 0;
					for(int changesetNum = 0; (changesetNum < 10000) && (! noMoreChangeSet) && (result.size() < maxChangesets) ; changesetNum++) {
						String changesetNumString = "" + changesetNum;
						while(changesetNumString.length() < 6) {
							changesetNumString = "0" + changesetNumString;
						}

						String dateFolderString = yearString + "/" + monthString + "/" + dayString + "/" + hourString + "/";
						String changesetBaseUrl = baseUrl + dateFolderString + changesetNumString;
						logger.debug("Base URL: " + changesetBaseUrl);
						String addChangeUrl = changesetBaseUrl + EXTENSION_ADDED;
						String removeChangeUrl = changesetBaseUrl + EXTENSION_REMOVED;
						String clearChangeUrl = changesetBaseUrl + EXTENSION_CLEAR;
						String reinsertChangeUrl = changesetBaseUrl + EXTENSION_REINSERT;

						String addChangeLocalName = localBasePath + dateFolderString + changesetNumString + EXTENSION_ADDED;
						String removeChangeLocalName = localBasePath + dateFolderString + changesetNumString + EXTENSION_REMOVED;
						File localAddFile = new File(addChangeLocalName);
						File localRemoveFile = new File(removeChangeLocalName);

						String dlAddChanges = null;
						String dlRemoveChanges = null;
						if(! localAddFile.exists() && ! localRemoveFile.exists()) {
							if(online) {
								dlAddChanges = DataUtils.downloadFile(addChangeUrl, localBasePath + dateFolderString);
								dlRemoveChanges = DataUtils.downloadFile(removeChangeUrl, localBasePath + dateFolderString);
							}
						} else {
							if(localAddFile.exists()) {
								dlAddChanges = addChangeLocalName;
							}
							if(localRemoveFile.exists()) {
								dlRemoveChanges = removeChangeLocalName;
							}
						}

						// When too much consecutive changesets do not exists, we pass to another hour
						if(dlAddChanges == null 
								&& dlRemoveChanges == null) {
							if( (online 
									&& ! DataUtils.checkRemoteFile(clearChangeUrl) 
									&& ! DataUtils.checkRemoteFile(reinsertChangeUrl) )
									|| ! online) { 
								nbMissingChangeSet++;
								logger.debug("No changeset retrieved");
							}
						} else {
							result.add(new ChangesetFile(yearString, monthString, dayString, hourString, changesetNumString, dlAddChanges, dlRemoveChanges));
							nbMissingChangeSet = 0;
						}
						noMoreChangeSet = nbMissingChangeSet >= maximumMissingChangeset;
					}
				}
			}
		}

		return result;
	}

	public static void main(String[] args) {
		BasicConfigurator.configure();
		PropertyConfigurator.configure("log4j-config.txt");

		LinkedList<ChangesetFile> changesets = harvest(3, false);
		LinkedList<Changeset> chTriples = new LinkedList<Changeset>();
		for(ChangesetFile chgFile : changesets) {
			chTriples.add(new Changeset(chgFile));
		}

		// "/home/pmaillot/Documents/KRIMP/DBpdedia/3.7/dbpedia.3.7.nt"
		
		for(Changeset chg : chTriples) {
			chg.printModifiedResources();
			chg.extractContextTriples("/home/pmaillot/Documents/KRIMP/DBpdedia/3.7/dbpedia.3.7.nt", CONTEXT_SOURCE.FROM_FILE);
			logger.debug(chg);
		}
	}

}
