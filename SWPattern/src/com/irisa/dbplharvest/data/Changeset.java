///////////////////////////////////////////////////////////////////////////////
//File: ChangeSet.java 
//Author: Pierre Maillot
//Date: December 2017
//Comments: Class that handles the update files - added and removed at the 
//			same time. Data representation in memory of a changeset.
// 			 * Contains the modified triples, the list of modified resources apearing in them.
//Modifications:
// 			* Feb 2018 (CBobed): 
//				- added method to calculate and group the resources 
// 			affected.
// 				- the transaction conversion is now created/calculated against a
//			given model (the evolving one), so the context is not stored anymore
///////////////////////////////////////////////////////////////////////////////


package com.irisa.dbplharvest.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.irisa.dbplharvest.DataUtils;
import com.irisa.dbplharvest.data.DataConstants.ACCEPTED_URI_FILTER;
import com.irisa.krimp.data.ItemsetSet;
import com.irisa.swpatterns.TransactionsExtractor.Neighborhood;
import com.irisa.utilities.Couple;

/**
 * Data representation in memory of a changeset.
 * Contains the modified triples, the list of modified resources apearing in them and the their contexts.
 * The context of each modified resource contains the triples where they appear and the type of the resources they are connected to.
 * @author pmaillot
 *
 */
public class Changeset implements AbstractChangeset {

	protected String _year;
	protected String _month;
	protected String _day;
	protected String _hour;
	protected String _number;
	
	protected Model _addedTriples = ModelFactory.createDefaultModel();
	protected Model _deletedTriples = ModelFactory.createDefaultModel();
	
	// it is initialized on demand
	protected HashSet<HashSet<Resource>> _affectedResources = null; 
	
//	protected Model _contextTriples = ModelFactory.createDefaultModel();
	
//	protected HashSet<Resource> _modifiedResources = new HashSet<Resource>();
//	protected String _modifiedResFilename = "";
//	protected String _contextFilename = "";
	
//	protected ItemsetSet _addTransactions = new ItemsetSet();
//	protected ItemsetSet _delTransactions = new ItemsetSet();
	
	public enum CONTEXT_SOURCE {
		FROM_FILE,
		FROM_MEMORY
	}

	public Changeset() {
	}
	
	public Changeset(String year, String month, String day, String hour, String number) {
		this._year = year;
		this._month = month;
		this._day = day;
		this._hour = hour;
		this._number = number;
	}
	
	public Changeset(ChangesetFile chFile) {
		this.setYear(chFile.getYear());
		this.setMonth(chFile.getMonth());
		this.setDay(chFile.getDay());
		this.setHour(chFile.getHour());
		this.setNumber(chFile.getNumber());
		readFiles(chFile);
	}
	
	protected String modifFilename() {
		return DataUtils.dateBasedName(_year, _month, _day, _hour, _number) + ".modif.txt";
	}
	
	protected String contextFilename() {
		return DataUtils.dateBasedName(_year, _month, _day, _hour, _number) + ".context.nt";
	}
	
//	public ItemsetSet getAddTransaction(Neighborhood level) {
//		if(this._addTransactions.isEmpty() && ! this._addedTriples.isEmpty()) { // If we should have transactions
//			convertTransactions(level);
//		}
//		return this._addTransactions;
//	}
//	
//	public ItemsetSet getDelTransaction(Neighborhood level) {
//		if(this._delTransactions.isEmpty() 
//				&& ! this._deletedTriples.isEmpty()) { // If we should have transactions
//			convertTransactions(level);
//		}
//		return this._delTransactions;
//	}
	
//	public void convertTransactions(Neighborhood level) {
//		ChangesetTransactionConverter converter = new ChangesetTransactionConverter();
//		converter.setNeighborLevel(level);
//		Couple<ItemsetSet, ItemsetSet> trans = converter.extractChangesetTransactionsFromContext(this);
//		this._addTransactions = trans.getSecond();
//		this._delTransactions = trans.getFirst();
//	}
	
//	protected void updateTmpFilenames() {
//		this._contextFilename = contextFilename();
//		this._modifiedResFilename = modifFilename();
//	}
	
	public void readDeleteTriples(String filename) {
		_deletedTriples.read(filename);
//		this._modifiedResources.addAll(extractModifiedResources(_deletedTriples));
	}
	
//	public void readContextTriples(String filename) {
//		_contextTriples.read(filename);
//	}
	
//	public void extractContextTriples(String sourcename, CONTEXT_SOURCE source) {
//		if(! (new File(this._modifiedResFilename).exists())) {
//			printModifiedResources();
//		}
//		if(source == CONTEXT_SOURCE.FROM_FILE) {
//			if(! (new File(this._contextFilename).exists())) {
//				DataUtils.printFilter(sourcename, this._contextFilename, this._modifiedResFilename);
//			}
//			DataUtils.putTriplesInModel(this._contextTriples, _contextFilename);
//		} else {
//			extractContextTriples((ModelFactory.createDefaultModel().read(sourcename)));
//		}
//	}
	
//	public void extractContextTriples(Model model) {
//		DataUtils.extractDescriptionTriples(_contextTriples, model, _modifiedResources);
//	}

	public void readAddTriples(String filename) {
		_addedTriples.read(filename);
//		this._modifiedResources.addAll(extractModifiedResources(_addedTriples));
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
	
//	public boolean printModifiedResources() {
//		return DataUtils.writeResourcesToFile(this._modifiedResources, modifFilename());
//	}
	
	public Model getAddTriples() {
		return this._addedTriples;
	}
	
	public Model getDelTriples() {
		return this._deletedTriples;
	}
	
//	public HashSet<Resource> getModifiedResources() {
//		return _modifiedResources;
//	}
//
//	public String getModifiedResFilename() {
//		return _modifiedResFilename;
//	}
//
//	public String getContextFilename() {
//		return _contextFilename;
//	}
//
//	public Model getContextTriples() {
//		return this._contextTriples;
//	}

	@Override
	public String getYear() {
		return this._year;
	}

	@Override
	public void setYear(String year) {
		this._year = year;
//		this.updateTmpFilenames();
	}

	@Override
	public String getMonth() {
		return this._month;
	}

	@Override
	public void setMonth(String month) {
		this._month = month;
//		this.updateTmpFilenames();
	}

	@Override
	public String getDay() {
		return this._day;
	}

	@Override
	public void setDay(String day) {
		this._day = day;
//		this.updateTmpFilenames();
	}

	@Override
	public String getHour() {
		return this._hour;
	}

	@Override
	public void setHour(String hour) {
		this._hour = hour;
//		this.updateTmpFilenames();
	}

	@Override
	public String getNumber() {
		return this._number;
	}

	@Override
	public void setNumber(String number) {
		this._number = number;
//		this.updateTmpFilenames();
	}
	
	/**
	 * Makes sure that the removed triples are not added afterward and vice-versa
	 */
	protected void canonize() {
//		if(this._addedTriples != null && this._deletedTriples != null) {
//			this._deletedTriples.remove(_addedTriples);
//			this._addedTriples.remove(_deletedTriples);
//		}
		
		LinkedList<Statement> addStatToRemove = new LinkedList<Statement>();
		this._addedTriples.listStatements().filterDrop(new Predicate<Statement>() {
			@Override
			public boolean test(Statement stat) {
				boolean subjectOk = filterAcceptURI(stat.getSubject());
				boolean objectOk = stat.getObject().isLiteral() 
						|| (stat.getObject().isResource() 
								&& filterAcceptURI(stat.getObject().asResource()));
				boolean relationOK = filterAcceptURI(stat.getPredicate());
				return subjectOk && objectOk && relationOK;
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
				boolean relationOK = filterAcceptURI(stat.getPredicate());
				return subjectOk && objectOk && relationOK;
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
	
	/** 
	 * Calculates the connected resources that will define each of the low level updates
	 * @return
	 */
	public HashSet<HashSet<Resource>> getAffectedResources () {
		
		if (this._affectedResources == null) {
		
			this._affectedResources = new HashSet<HashSet<Resource>>(); 
			HashMap<Resource,HashSet<Resource>> invertedIndex = new HashMap<Resource, HashSet<Resource>>();
			Statement stmt = null; 
			Resource sbj = null; 
			Resource obj = null; 
			
			if (!_addedTriples.isEmpty()) {
				StmtIterator it = _addedTriples.listStatements(); 
				while (it.hasNext()) {
					stmt = it.next(); 
					sbj = stmt.getSubject();
					if (stmt.getObject().isResource()) {
						obj = stmt.getObject().asResource();
					}
					else { 
						obj = null; 
					}
					
					// we do it this way to save some checkings
					if (invertedIndex.containsKey(sbj)) {  
						
						if (obj != null) {
							if (invertedIndex.containsKey(obj)) { 
								// it contains both we merge the sets if they are not the same
								// otherwise we don't have to do anything
								if ( !invertedIndex.get(sbj).equals(invertedIndex.get(obj)) )  { 
									HashSet<Resource> oldSet = invertedIndex.get(obj);
									invertedIndex.get(sbj).addAll(oldSet); 
									HashSet<Resource> newSet = invertedIndex.get(sbj); 
									// we make all the previous elements point at the new merged set
									for (Resource res: oldSet) { 
										invertedIndex.put(res, newSet); 
									}
									this._affectedResources.remove(oldSet);
								}
								// else 
								// they are equal, we do not have to do anything
							}
							else { 
								// we add the newly seen resource to the existing set
								// and update the inverted index
								invertedIndex.get(sbj).add(obj); 
								invertedIndex.put(obj, invertedIndex.get(sbj)); 
							}
						}
						// else : nothing to be done
					}
					else { 
						if (obj != null) {
							if (invertedIndex.containsKey(obj)) { 
								invertedIndex.get(obj).add(sbj); 
								invertedIndex.put(sbj, invertedIndex.get(obj)); 
							}
							else { 
								// none of them are included
								HashSet<Resource> newSet = new HashSet<Resource>(); 
								newSet.add(sbj); 
								newSet.add(obj); 
								this._affectedResources.add(newSet); 
								invertedIndex.put(sbj, newSet);
								invertedIndex.put(obj, newSet); 
							}
						} 
						else { 
							// only the resouce a with itself in the set 
							HashSet<Resource> newSet = new HashSet<Resource>(); 
							newSet.add(sbj); 
							this._affectedResources.add(newSet); 
							invertedIndex.put(sbj, newSet); 
						}
					}
				}
			}
			
			// we could work on top of a join model, but given the volume of operations
			// we prefer not to do so
			
			if (!_deletedTriples.isEmpty()) {
				StmtIterator it = _deletedTriples.listStatements(); 
				while (it.hasNext()) {
					stmt = it.next(); 
					sbj = stmt.getSubject();
					if (stmt.getObject().isResource()) {
						obj = stmt.getObject().asResource();
					}
					else { 
						obj = null; 
					}
					
					// we do it this way to save some checkings
					if (invertedIndex.containsKey(sbj)) {  
						
						if (obj != null) {
							if (invertedIndex.containsKey(obj)) { 
								// it contains both we merge the sets if they are not the same
								// otherwise we don't have to do anything
								if ( !invertedIndex.get(sbj).equals(invertedIndex.get(obj)) )  { 
									HashSet<Resource> oldSet = invertedIndex.get(obj);
									invertedIndex.get(sbj).addAll(oldSet); 
									HashSet<Resource> newSet = invertedIndex.get(sbj); 
									// we make all the previous elements point at the new merged set
									for (Resource res: oldSet) { 
										invertedIndex.put(res, newSet); 
									}
									this._affectedResources.remove(oldSet);
								}
								// else 
								// they are equal, we do not have to do anything
							}
							else { 
								// we add the newly seen resource to the existing set
								// and update the inverted index
								invertedIndex.get(sbj).add(obj); 
								invertedIndex.put(obj, invertedIndex.get(sbj)); 
							}
						}
						// else : nothing to be done
					}
					else { 
						if (obj != null) {
							if (invertedIndex.containsKey(obj)) { 
								invertedIndex.get(obj).add(sbj); 
								invertedIndex.put(sbj, invertedIndex.get(obj)); 
							}
							else { 
								// none of them are included
								HashSet<Resource> newSet = new HashSet<Resource>(); 
								newSet.add(sbj); 
								newSet.add(obj); 
								this._affectedResources.add(newSet); 
								invertedIndex.put(sbj, newSet);
								invertedIndex.put(obj, newSet); 
							}
						} 
						else { 
							// only the resouce a with itself in the set 
							HashSet<Resource> newSet = new HashSet<Resource>(); 
							newSet.add(sbj); 
							this._affectedResources.add(newSet); 
							invertedIndex.put(sbj, newSet); 
						}
					}
				}
			}
			// end of
		}
		
		return this._affectedResources; 
	}
	
	/**
	 * Check if a resource is among the affected ressources of the updates of the changeset
	 * @param res
	 * @return
	 */
	public boolean isAffectedResource(Resource res) {
		Iterator<HashSet<Resource>> itAffectSets = this._affectedResources.iterator();
		while(itAffectSets.hasNext()) {
			HashSet<Resource> tmpSet = itAffectSets.next();
			
			Iterator<Resource> itRes = tmpSet.iterator();
			while(itRes.hasNext()) {
				Resource affRes = itRes.next();
				
				if(res.equals(affRes)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	
//	protected HashSet<Resource> extractModifiedResources(final Model data) {
//		HashSet<Resource> result = new HashSet<Resource>();
//		
//		// Extract all URI resources subjet of triples and neither templates or anything else
//		data.listSubjects().filterKeep(new Predicate<Resource>() {
//			@Override
//			public boolean test(Resource t) {
//				return filterAcceptURI(t);
//			}
//		}).filterKeep(new Predicate<Resource>() {
//			@Override
//			public boolean test(Resource t) {
//				return filterResourceURI(t);
//			}
//		}).forEachRemaining(new Consumer<Resource>() {
//			@Override
//			public void accept(Resource t) {
//				if(t.isURIResource()) {
//					result.add(t);
//				}
//			}
//		});
//		
//		// extract all object of triples that are URIs and neither templates or anything else
//		data.listObjects().filterKeep(new Predicate<RDFNode>() {
//			@Override
//			public boolean test(RDFNode t) {
//				return t.isURIResource() && filterResourceURI(t.asResource());
//			}
//		}).filterKeep(new Predicate<RDFNode>() {
//			@Override
//			public boolean test(RDFNode t) {
//				return t.isURIResource() && filterAcceptURI(t.asResource());
//			}
//		}).forEachRemaining(new Consumer<RDFNode>() {
//			@Override
//			public void accept(RDFNode t) {
//				result.add(t.asResource());
//			}
//		});
//		
//		return result;
//	}
	
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

	/** 
	 * Returns the size of the changeset in terms of triples 
	 * @return
	 */
	
	public long getUpdateSize () { 
		return _addedTriples.size() + _deletedTriples.size();
	}
	
	/// input/output operations to handle the affected sets 
	
	public void writeAffectedResources (PrintWriter out) {
		
		if (this._affectedResources == null) { 
			this.getAffectedResources();
		}
		
		for (HashSet<Resource> set: this.getAffectedResources()) { 
			for (Resource res: set) { 
				out.println(res.getURI());
			}
			// they are just separted by a line
			out.println(); 
		}
	}
	
	public void readAffectedResources (BufferedReader in) throws IOException { 
		this._affectedResources = new HashSet<HashSet<Resource>> (); 
		HashSet<Resource> auxiliar = new HashSet<Resource>(); 
		String line = null; 
		while ( (line = in.readLine()) != null ) {
			if ("".equals(line)) { 
				// an empty line == new set
				this._affectedResources.add(auxiliar);
				auxiliar = new HashSet<Resource>(); 
			}
			else { 
				auxiliar.add(ResourceFactory.createResource(line)); 
			}
		}
		if (!auxiliar.isEmpty()) { 
			this._affectedResources.add(auxiliar); 
		}
	}
	
	
	
}
