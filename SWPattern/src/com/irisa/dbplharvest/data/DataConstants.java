package com.irisa.dbplharvest.data;

import java.util.LinkedList;

public class DataConstants {

	public enum REFUSED_URI_FILTER {
		wikipediaOrg("wikipedia.org"),
		dbpediaContributor("dbpedia.org/contributor/"),
		dbpediaResourceTemplate("dbpedia.org/resource/Template"),
		dbpediaResourceCategory("dbpedia.org/resource/Category"),
		dbpediaResourceFile("dbpedia.org/resource/File:"),
		dbpediaOntoWikiPageRevision("http://dbpedia.org/ontology/wikiPageRevisionID"),
		dbpediaOntoWikiPageOutDegree("http://dbpedia.org/ontology/wikiPageOutDegree"),
		dbpediaOntoWikiPageModified("http://dbpedia.org/ontology/wikiPageModified"),
		dbpediaOntoWikiPageLength("http://dbpedia.org/ontology/wikiPageLength"),
		dbpediaOntoWikiPageExtracted("http://dbpedia.org/ontology/wikiPageExtracted"),
		;
		
		private String _url;
		
		REFUSED_URI_FILTER(String url) {
			_url = url;
		}
		
		public String getString() {
			return _url;
		}
		
		public static LinkedList<String> getStringList() {
			LinkedList<String> result = new LinkedList<String>();
			for(REFUSED_URI_FILTER filter: values()) {
				result.add(filter._url);
			}
				
			return result;
		}
	}

	public enum ACCEPTED_URI_FILTER {
		dbpediaOntology("dbpedia.org/ontology/"),
		dbpediaResource("dbpedia.org/resource/"),
		dbpediaProperty("dbpedia.org/property/")
		;
		
		private String _url;
		
		ACCEPTED_URI_FILTER(String url) {
			_url = url;
		}
		
		public String getString() {
			return _url;
		}
		
		public static LinkedList<String> getStringList() {
			LinkedList<String> result = new LinkedList<String>();
			for(ACCEPTED_URI_FILTER filter: values()) {
				result.add(filter._url);
			}
				
			return result;
		}
	}

}
