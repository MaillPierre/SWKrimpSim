package com.irisa.jenautils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

//import org.apache.jena.atlas.web.auth.ServiceAuthenticator;
import org.apache.log4j.Logger;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.util.FileManager;

/**
 * Classe utilistaire pour la connection à un serveur.
 * La classe en configurable pour un acces distant via http ou local par le chargement d'un fichier dans Jena
 * elle permet également de disposer de fonctions pour le voisinage, le nombre d'occurence et autres fonctions utilitaires pour les requêtes
 * @author maillot
 *
 */
public class BaseRDF {
	
    private static Logger logger = Logger.getLogger(BaseRDF.class);

	private String _server;
	private MODE _mode;
	private Model _model;
//	private ServiceAuthenticator _auth;
	
	public enum MODE 
	{
		LOCAL, DISTANT;
	}
	
	public BaseRDF(String adresse, MODE mode) {
		this._mode = mode;
		if(mode == MODE.DISTANT)
		{
			this._server = adresse;
			this._model = null;
		}
		else
		{
			this._server = null;
			this._model = ModelFactory.createDefaultModel();
			if(adresse != null)
			{
				try {
					FileInputStream fileStr = new FileInputStream(adresse);
					this._model.read(fileStr, null, Utils.guessLangFromFilename(adresse) );
				} catch (IOException e) {
					e.printStackTrace();
				} catch (RiotParseException e) {
				logger.error("", e);
				}
			}
		}
	}
	
	public BaseRDF(String filename)
	{
		this(filename, MODE.LOCAL);
	}
	
	/**
	 * Par défaut, mode local, model vide
	 */
	public BaseRDF() {
		this(null, MODE.LOCAL);
	}
	
	public long size()
	{
		if(this._mode == MODE.DISTANT)
		{
			return -1;
		}
		else
		{
			return this._model.size();
		}
	}
	
	public void setDistantServer(String server)
	{
		this._server = server;
	}
	
	public void setLocalMode()
	{
		if(this._model == null)
		{
			this._model = ModelFactory.createDefaultModel();
		}
		this._mode = MODE.LOCAL;
	}
	
	public void setLocalMode(Model model)
	{
		if(this._model != null)
		{
			this._model.close();
		}
		this._mode = MODE.LOCAL;
		this._model = model;
	}
	
	public void setDistantMode(String server)
	{
		this._server = server;
		this._mode = MODE.DISTANT;
	}
	
	public QueryExecution executionQuery(String query)
	{
		Query nQuery = QueryFactory.create(query, Syntax.syntaxSPARQL_11);
		return executionQuery(nQuery);
	}

	public QueryExecution executionQuery(Query query) 
	{
		logger.trace("executeQuery(Query query)");
		QueryExecution result = null;
		
		try
		{
			if(_mode == MODE.DISTANT) 
			{
				result = QueryExecutionFactory.sparqlService(_server, query/*, this._auth*/);
			}
			else 
			{
				result = QueryExecutionFactory.create(query, _model);
			}
		}
		catch(QueryParseException e)
		{
			logger.error("Requete mal formée - " + query);
			e.printStackTrace();
			System.out.println(query);
		}
		result.setTimeout(30000, TimeUnit.MILLISECONDS);
		logger.trace("FIN executeQuery(Query query)");
		return result;
	}
	
	public void close() {
		if(this._model != null) {
			this._model.close();
		}
	}
	
	public String toString() {
		return this._mode + " : " + this._server + " : " + this.size();
	}

	
}
