package com.irisa.jenautils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.util.FileUtils;

public class Utils {

	public static Model purgeIncorrectURI(Model m) {
		StmtIterator itStat = m.listStatements();
		ArrayList<Statement> toRemove = new ArrayList<Statement>();
		while(itStat.hasNext()) {
			Statement s = itStat.nextStatement();
			if(! FileUtils.isURI(s.getSubject().toString())) {
				toRemove.add(s);
			}
			if(s.getObject().isResource()) {
				if(! FileUtils.isURI(s.getObject().toString())) {
					toRemove.add(s);
				}
			}
		}
		m.remove(toRemove);
		return m;
	}
	
	public static String guessLangFromFilename(String filename) {
		String result = null;
		
		if(filename.endsWith(".ttl"))
		{
			result = FileUtils.langTurtle;
		}
		else if(filename.endsWith(".nt"))
		{
			result = FileUtils.langNTriple;
		}
		else if(filename.endsWith(".n3"))
		{
			result = FileUtils.langN3;
		}
		
		return result;
	}
	
	public static <T> HashSet<T> intersection(HashSet<T> s1, HashSet<T> s2)
	{
		HashSet<T> result = new HashSet<T>();
		for(T n : s1)
		{
			if(s2.contains(n))
			{
				result.add(n);
			}
		}
		return result;
	}
	
	public static <T> HashSet<T> union(HashSet<T> s1, HashSet<T> s2)
	{
		HashSet<T> result = new HashSet<T>(s1);
		result.addAll(s2);
		return result;
	}
	
	public static <T> HashSet<T> difference(HashSet<T> s1, HashSet<T> s2)
	{
		HashSet<T> result = union(s1, s2);
		result.removeAll(intersection(s1, s2));
		return result;
	}
	
	/**
	 * 
	 * @param s1
	 * @param s2
	 * @return Retourne ce qui est dans s1 mais pas dans s2
	 */
	public static <T> HashSet<T> notIn(HashSet<T> s1, HashSet<T> s2)
	{
		HashSet<T> result = new HashSet<T>(s1);
		result.removeAll(intersection(s1, s2));
		return result;
	}
	
	public static HashSet<Property> getProperties(HashSet<Statement> s)
	{
		HashSet<Property> result = new HashSet<Property>();
		
		for(Statement p : s)
		{
			result.add(p.getPredicate());
		}
		
		return result;
	}
	
	public static HashSet<RDFNode> getUris(HashSet<Statement> s)
	{
		HashSet<RDFNode> result = new HashSet<RDFNode>();
		
		for(Statement p : s)
		{
			result.add(p.getPredicate());
			if(p.getSubject().isURIResource())
			{
				result.add(p.getSubject());
			}
			if(p.getObject().isURIResource())
			{
				result.add(p.getObject());
			}
		}
		
		return result;
	}
	
	public static String tripleToQueryString(Triple t)
	{
		String result = "";

		if(t.getSubject().isVariable())
		{
			result += t.getSubject() + " ";
		}
		else
		{
			result += "<" + t.getSubject() + "> ";
		}

		if(t.getPredicate().isURI())
		{
			result += "<" + t.getPredicate() + "> ";
		}
		else
		{
			result += t.getPredicate() + " ";
		}

		if(!t.getObject().isVariable())
		{
			if(t.getObject().isURI())
			{
				result += "<" + t.getObject() + "> . ";	
			}
			else
			{
				result += t.getObject() + ". ";
			}
		}
		else
		{
			result += t.getObject() + ". ";
		}

		return result;
	}

	public static String tripleToQueryString(ElementPathBlock block)
	{
		String result = "";
		Iterator<TriplePath> it = block.patternElts();
		while(it.hasNext())
		{
			TriplePath trip = it.next();
			result += tripleToQueryString(trip).replace("\n", "") + " \n";
		}

		return result;
	}

	public static String tripleToQueryString(TriplePath t)
	{
		return tripleToQueryString(t.asTriple());
	}
	
	public static String statementToString(Statement t)
	{
		return nodeToN3String(t.getSubject()) + " " + nodeToN3String(t.getPredicate()) + " " + nodeToN3String(t.getObject()) + " .";
	}
	
	public static String statementSetToString(Set<Statement> s)
	{
		String result = "";
		
		Iterator<Statement> it = s.iterator();
		while(it.hasNext())
		{
			Statement stat = it.next();
			result += statementToString(stat) + "\n";
		}
		
		return result;
	}
	
	public static String nodeToN3String(RDFNode r)
	{
		String result = "";
		
		if(r.isURIResource() && ! r.isAnon())
		{
			result = "<" + r.asResource().getURI() + ">";
		}
		else if(r.isLiteral())
		{
			result = "\"" + r.asLiteral().getValue() + "\"";
			if(r.asLiteral().getDatatype() != null)
			{
				result += "^^<" + r.asLiteral().getDatatype().getURI() + ">";
			}
		}
		else // C'est un blank node
		{
			result = "_:" + r.toString();
		}
		
		return result;
	}
}
