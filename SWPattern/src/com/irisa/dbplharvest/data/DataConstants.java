package com.irisa.dbplharvest.data;

import java.util.LinkedList;

public class DataConstants {

	public enum REFUSED_URI_FILTER {
		wikipediaOrg("wikipedia.org"),
		dbpediaContributor("dbpedia.org/contributor/"),
		dbpediaResourceTemplate("dbpedia.org/resource/Template"),
		dbpediaResourceCategory("dbpedia.org/resource/Category"),
		dbpediaResourceFile("dbpedia.org/resource/File:"),
//		dbpediaOntoWikiPageRevision("http://dbpedia.org/ontology/wikiPageRevisionID"),
//		dbpediaOntoWikiPageOutDegree("http://dbpedia.org/ontology/wikiPageOutDegree"),
//		dbpediaOntoWikiPageModified("http://dbpedia.org/ontology/wikiPageModified"),
//		dbpediaOntoWikiPageLength("http://dbpedia.org/ontology/wikiPageLength"),
//		dbpediaOntoWikiPageExtracted("http://dbpedia.org/ontology/wikiPageExtracted"),
		dbpediaProperty("http://dbpedia.org/property/"),
		dbpadiaOntoWikiPageAll("http://dbpedia.org/ontology/wikiPage")
//		dbpediaOntoWikiPageDisambiguates("http://dbpedia.org/ontology/wikiPageDisambiguates"), 
//		dbpediaOntoWikiPageID("http://dbpedia.org/ontology/wikiPageID"), 
//		dbpediaOntoWikiRedirects("http://dbpedia.org/ontology/wikiPageRedirects")
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
		dublinCore("http://purl.org/dc/"), 
		georss("http://www.georss.org/georss/point"), 
		rdfschema("http://www.w3.org/2000/01/rdf-schema"), 
		wgs84_pos("http://www.w3.org/2003/01/geo/wgs84_pos"), 
		skos("http://www.w3.org/2004/02/skos"), 
		provenance("http://www.w3.org/ns/prov"), 
		foaf("http://xmlns.com/foaf/0.1")
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
