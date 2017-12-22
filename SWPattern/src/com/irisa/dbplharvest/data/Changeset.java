package com.irisa.dbplharvest.data;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import com.irisa.dbplharvest.DataUtils;
import com.irisa.dbplharvest.data.DataConstants.ACCEPTED_URI_FILTER;

public class Changeset implements AbstractChangeset {

	protected String _year;
	protected String _month;
	protected String _day;
	protected String _hour;
	protected String _number;
	
	protected Model _addedTriples = ModelFactory.createDefaultModel();
	protected Model _deletedTriples = ModelFactory.createDefaultModel();
	protected Model _contextTriples = ModelFactory.createDefaultModel();
	
	protected HashSet<Resource> _modifiedResources = new HashSet<Resource>();
	protected String _modifiedResFilename = "";
	protected String _contextFilename = "";
	
	public enum CONTEXT_SOURCE {
		FROM_FILE,
		FROM_SPARQL
	}

	public Changeset() {
	}
	
	public Changeset(String year, String month, String day, String hour, String number) {
		this._year = year;
		this._month = month;
		this._day = day;
		this._hour = hour;
		this._number = number;
		this.updateTmpFilenames();
	}
	
	public Changeset(ChangesetFile chFile) {
		this.setYear(chFile.getYear());
		this.setMonth(chFile.getMonth());
		this.setDay(chFile.getDay());
		this.setHour(chFile.getHour());
		this.setNumber(chFile.getNumber());
		readFiles(chFile);
		this.updateTmpFilenames();
	}
	
	protected String modifFilename() {
		return DataUtils.dateBasedName(_year, _month, _day, _hour, _number) + ".modif.txt";
	}
	
	protected String contextFilename() {
		return DataUtils.dateBasedName(_year, _month, _day, _hour, _number) + ".context.nt";
	}
	
	protected void updateTmpFilenames() {
		this._contextFilename = contextFilename();
		this._modifiedResFilename = modifFilename();
	}
	
	public void readDeleteTriples(String filename) {
		_deletedTriples.read(filename);
		this._modifiedResources.addAll(extractModifiedResources(_deletedTriples));
	}
	
	public void readContextTriples(String filename) {
		_contextTriples.read(filename);
	}
	
	public void extractContextTriples(String sourcename, CONTEXT_SOURCE source) {
		if(! (new File(this._modifiedResFilename).exists())) {
			printModifiedResources();
		}
		if(source == CONTEXT_SOURCE.FROM_FILE) {
			if(! (new File(this._contextFilename).exists())) {
				DataUtils.printFilter(sourcename, this._contextFilename, this._modifiedResFilename);
			}
			DataUtils.putTriplesInModel(this._contextTriples, _contextFilename);
		} else {
			extractContextTriples((ModelFactory.createDefaultModel().read(sourcename)));
		}
	}
	
	public void extractContextTriples(Model model) {
		DataUtils.extractDescriptionTriples(_contextTriples, model, _modifiedResources);
	}

	public void readAddTriples(String filename) {
		_addedTriples.read(filename);
		this._modifiedResources.addAll(extractModifiedResources(_addedTriples));
	}
	
	public void readFiles(ChangesetFile chFile) {
		if(chFile.getAddFile() != null) {
			readAddTriples(chFile.getAddFile());
		}
		if(chFile.getDelFile() != null) {
			readDeleteTriples(chFile.getDelFile());
		}
		this.canonize();
	}
	
	public boolean printModifiedResources() {
		return DataUtils.writeResourcesToFile(this._modifiedResources, modifFilename());
	}
	
	public Model getAddTriples() {
		return this._addedTriples;
	}
	
	public Model getDelTriples() {
		return this._deletedTriples;
	}
	
	public HashSet<Resource> getModifiedResources() {
		return _modifiedResources;
	}

	public String getModifiedResFilename() {
		return _modifiedResFilename;
	}

	public String getContextFilename() {
		return _contextFilename;
	}

	public Model getContextTriples() {
		return this._contextTriples;
	}

	@Override
	public String getYear() {
		return this._year;
	}

	@Override
	public void setYear(String year) {
		this._year = year;
		this.updateTmpFilenames();
	}

	@Override
	public String getMonth() {
		return this._month;
	}

	@Override
	public void setMonth(String month) {
		this._month = month;
		this.updateTmpFilenames();
	}

	@Override
	public String getDay() {
		return this._day;
	}

	@Override
	public void setDay(String day) {
		this._day = day;
		this.updateTmpFilenames();
	}

	@Override
	public String getHour() {
		return this._hour;
	}

	@Override
	public void setHour(String hour) {
		this._hour = hour;
		this.updateTmpFilenames();
	}

	@Override
	public String getNumber() {
		return this._number;
	}

	@Override
	public void setNumber(String number) {
		this._number = number;
		this.updateTmpFilenames();
	}
	
	/**
	 * Makes sure that the removed triples are not added afterward and vice-versa
	 */
	protected void canonize() {
		if(this._addedTriples != null && this._deletedTriples != null) {
			this._deletedTriples.remove(_addedTriples);
			this._addedTriples.remove(_deletedTriples);
		}
		
		LinkedList<Statement> addStatToRemove = new LinkedList<Statement>();
		this._addedTriples.listStatements().filterDrop(new Predicate<Statement>() {
			@Override
			public boolean test(Statement stat) {
				boolean subjectOk = filterAcceptURI(stat.getSubject());
				boolean objectOk = stat.getObject().isLiteral() 
						|| (stat.getObject().isResource() 
								&& filterAcceptURI(stat.getObject().asResource()));
				return subjectOk && objectOk;
			}
		}).forEachRemaining(new Consumer<Statement>() {
			@Override
			public void accept(Statement stat) {
				addStatToRemove.add(stat);
			}
		});
		

		LinkedList<Statement> removedStatToRemove = new LinkedList<Statement>();
		this._deletedTriples.listStatements().filterDrop(new Predicate<Statement>() {
			@Override
			public boolean test(Statement stat) {
				boolean subjectOk = filterAcceptURI(stat.getSubject());
				boolean objectOk = stat.getObject().isLiteral() || (stat.getObject().isResource() 
						&& filterAcceptURI(stat.getObject().asResource()));
				return subjectOk && objectOk;
			}
		}).forEachRemaining(new Consumer<Statement>() {
			@Override
			public void accept(Statement stat) {
				removedStatToRemove.add(stat);
			}
		});
		
		this._addedTriples.remove(addStatToRemove);
		this._deletedTriples.remove(removedStatToRemove);
	}
	
	protected HashSet<Resource> extractModifiedResources(final Model data) {
		HashSet<Resource> result = new HashSet<Resource>();
		
		// Extract all URI resources subjet of triples and neither templates or anything else
		data.listSubjects().filterKeep(new Predicate<Resource>() {
			@Override
			public boolean test(Resource t) {
				return filterAcceptURI(t);
			}
		}).filterKeep(new Predicate<Resource>() {
			@Override
			public boolean test(Resource t) {
				return filterResourceURI(t);
			}
		}).forEachRemaining(new Consumer<Resource>() {
			@Override
			public void accept(Resource t) {
				if(t.isURIResource()) {
					result.add(t);
				}
			}
		});
		
		// extract all object of triples that are URIs and neither templates or anything else
		data.listObjects().filterKeep(new Predicate<RDFNode>() {
			@Override
			public boolean test(RDFNode t) {
				return t.isURIResource() && filterResourceURI(t.asResource());
			}
		}).filterKeep(new Predicate<RDFNode>() {
			@Override
			public boolean test(RDFNode t) {
				return t.isURIResource() && filterAcceptURI(t.asResource());
			}
		}).forEachRemaining(new Consumer<RDFNode>() {
			@Override
			public void accept(RDFNode t) {
				result.add(t.asResource());
			}
		});
		
		return result;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(this.getYear());
		sb.append("/");
		sb.append(this.getMonth());
		sb.append("/");
		sb.append(this.getDay());
		sb.append(":");
		sb.append(this.getHour());
		sb.append(" n°");
		sb.append(this.getNumber());
		sb.append(" Add=");
		sb.append(this._addedTriples.size());
		sb.append(" Delete=");
		sb.append(this._deletedTriples.size());
		sb.append(" Modified resources=");
		sb.append(this._modifiedResources.size());
		sb.append("Context size=");
		sb.append(this._contextTriples.size());
		
		return sb.toString();
	}
	
	/**
	 * Check if the resource URI accept at least one filter
	 * @param res
	 * @param filters
	 * @return
	 */
	public static boolean filterAcceptURI(Resource res, List<String> filters) {
		for(String filter : filters) {
			if(res.getURI().contains(filter)) {
				return true;
			}
		}
		return filters.isEmpty();
	}
	
	/**
	 * Check if the resource is accepted by the filters from DataConstants
	 * @param res
	 * @return
	 */
	public static boolean filterAcceptURI(Resource res) {
		return filterAcceptURI(res, DataConstants.ACCEPTED_URI_FILTER.getStringList()) 
				&& ! filterAcceptURI(res, DataConstants.REFUSED_URI_FILTER.getStringList());
	}
	
	/**
	 * Check if the resource is from dbpedia/ontology
	 * @param res
	 * @return
	 */
	public static boolean filterOntologyURI(Resource res) {
		List<String> ontologyString = Collections.singletonList(ACCEPTED_URI_FILTER.dbpediaOntology.getString());
		return filterAcceptURI(res, ontologyString);
	}
	
	/**
	 * Check if the resource is from dbpedia/property
	 * @param res
	 * @return
	 */
	public static boolean filterPropertyURI(Resource res) {
		List<String> ontologyString = Collections.singletonList(ACCEPTED_URI_FILTER.dbpediaProperty.getString());
		return filterAcceptURI(res, ontologyString);
	}
	
	/**
	 * Check if the resource is from dbpedia/property
	 * @param res
	 * @return
	 */
	public static boolean filterResourceURI(Resource res) {
		List<String> ontologyString = Collections.singletonList(ACCEPTED_URI_FILTER.dbpediaResource.getString());
		return filterAcceptURI(res, ontologyString);
	}

}
