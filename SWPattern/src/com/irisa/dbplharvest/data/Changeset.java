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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.irisa.dbplharvest.DataUtils;
import com.irisa.dbplharvest.data.DataConstants.ACCEPTED_URI_FILTER;

/**
 * Data representation in memory of a changeset. Contains the modified triples,
 * the list of modified resources apearing in them and the their contexts. The
 * context of each modified resource contains the triples where they appear and
 * the type of the resources they are connected to.
 * 
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

	protected long _numberFilteredTriples = 0;

	// it is initialized on demand
	protected HashSet<HashSet<Resource>> _affectedResources = null;

	protected HashSet<Resource> _flattenedAffectedResources = null;

	public enum CONTEXT_SOURCE {
		FROM_FILE, FROM_MEMORY
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

	public Changeset(ChangesetFile chFile, boolean canonizing) {
		this.setYear(chFile.getYear());
		this.setMonth(chFile.getMonth());
		this.setDay(chFile.getDay());
		this.setHour(chFile.getHour());
		this.setNumber(chFile.getNumber());
		// if canonizing is false, it just read the file
		readFiles(chFile, canonizing);
	}

	protected String modifFilename() {
		return DataUtils.dateBasedName(_year, _month, _day, _hour, _number) + ".modif.txt";
	}

	protected String contextFilename() {
		return DataUtils.dateBasedName(_year, _month, _day, _hour, _number) + ".context.nt";
	}

	public void readDeleteTriples(String filename) {

		// we have to check whether it exists
		File aux = new File(filename);
		if (aux.exists()) {
			_deletedTriples.read(filename);
		}
		// otherwise the deleted model is empty
	}

	public void readAddTriples(String filename) {
		File aux = new File(filename);
		if (aux.exists()) {
			_addedTriples.read(filename);
		}
		// otherwise the add model is empty

	}

	public void readFiles(ChangesetFile chFile, boolean canonize) {
		if (chFile.getAddFile() != null) {
			readAddTriples(chFile.getAddFile());
		}
		if (chFile.getDelFile() != null) {
			readDeleteTriples(chFile.getDelFile());
		}
		if (canonize) {
			this.canonize();
		}
	}

	public Model getAddTriples() {
		return this._addedTriples;
	}

	public Model getDelTriples() {
		return this._deletedTriples;
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

	/**
	 * Makes sure that the removed triples are not added afterward and vice-versa
	 */
	protected void canonize() {

		LinkedList<Statement> addStatToRemove = new LinkedList<Statement>();
		this._addedTriples.listStatements().filterDrop(new Predicate<Statement>() {
			@Override
			public boolean test(Statement stat) {
				boolean subjectOk = filterAcceptURI(stat.getSubject());
				boolean objectOk = stat.getObject().isLiteral()
						|| (stat.getObject().isResource() && filterAcceptURI(stat.getObject().asResource()));
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
				boolean objectOk = stat.getObject().isLiteral()
						|| (stat.getObject().isResource() && filterAcceptURI(stat.getObject().asResource()));
				boolean relationOK = filterAcceptURI(stat.getPredicate());
				return subjectOk && objectOk && relationOK;
			}
		}).forEachRemaining(new Consumer<Statement>() {
			@Override
			public void accept(Statement stat) {
				removedStatToRemove.add(stat);
			}
		});
		this._numberFilteredTriples = addStatToRemove.size() + removedStatToRemove.size();
		this._addedTriples.remove(addStatToRemove);
		this._deletedTriples.remove(removedStatToRemove);

	}

	/**
	 * Calculates the connected resources that will define each of the low level
	 * updates
	 * 
	 * @return
	 */
	public HashSet<HashSet<Resource>> getAffectedResources() {

		if (this._affectedResources == null) {
			this._affectedResources = new HashSet<HashSet<Resource>>();
			HashMap<Resource, HashSet<Resource>> invertedIndex = new HashMap<Resource, HashSet<Resource>>();
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
					} else {
						obj = null;
					}

					// we do it this way to save some checkings
					if (invertedIndex.containsKey(sbj)) {

						if (obj != null) {
							if (invertedIndex.containsKey(obj)) {
								// it contains both we merge the sets if they are not the same
								// otherwise we don't have to do anything

								if (!(invertedIndex.get(sbj).containsAll(invertedIndex.get(obj))
										&& invertedIndex.get(obj).containsAll(invertedIndex.get(sbj)))) {
									HashSet<Resource> oldSet = invertedIndex.get(obj);
									invertedIndex.get(sbj).addAll(oldSet);
									HashSet<Resource> newSet = invertedIndex.get(sbj);
									// we make all the previous elements point at the new merged set
									for (Resource res : oldSet) {
										invertedIndex.put(res, newSet);
									}
									System.err.println(
											"Affected resources set before: " + this._affectedResources.size());
									this._affectedResources.remove(invertedIndex.get(obj));
									System.err
											.println("Affected resources set after: " + this._affectedResources.size());
								}
								// else
								// they are equal, we do not have to do anything
							} else {
								// we add the newly seen resource to the existing set
								// and update the inverted index
								invertedIndex.get(sbj).add(obj);
								invertedIndex.put(obj, invertedIndex.get(sbj));
							}
						}
						// else : nothing to be done
					} else {
						if (obj != null) {
							if (invertedIndex.containsKey(obj)) {
								invertedIndex.get(obj).add(sbj);
								invertedIndex.put(sbj, invertedIndex.get(obj));
							} else {
								// none of them are included
								HashSet<Resource> newSet = new HashSet<Resource>();
								newSet.add(sbj);
								newSet.add(obj);
								this._affectedResources.add(newSet);
								invertedIndex.put(sbj, newSet);
								invertedIndex.put(obj, newSet);
							}
						} else {
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
					} else {
						obj = null;
					}

					// we do it this way to save some checkings
					if (invertedIndex.containsKey(sbj)) {

						if (obj != null) {
							if (invertedIndex.containsKey(obj)) {
								// it contains both we merge the sets if they are not the same
								// otherwise we don't have to do anything

								if (!(invertedIndex.get(sbj).containsAll(invertedIndex.get(obj))
										&& invertedIndex.get(obj).containsAll(invertedIndex.get(sbj)))) {
									// if ( !invertedIndex.get(sbj).equals(invertedIndex.get(obj)) ) {
									HashSet<Resource> oldSet = invertedIndex.get(obj);
									invertedIndex.get(sbj).addAll(oldSet);
									HashSet<Resource> newSet = invertedIndex.get(sbj);
									// we make all the previous elements point at the new merged set
									for (Resource res : oldSet) {
										invertedIndex.put(res, newSet);
									}
									System.err.println(
											"Affected resources set before: " + this._affectedResources.size());
									this._affectedResources.remove(invertedIndex.get(obj));
									System.err
											.println("Affected resources set after: " + this._affectedResources.size());
								}
								// else
								// they are equal, we do not have to do anything
							} else {
								// we add the newly seen resource to the existing set
								// and update the inverted index
								invertedIndex.get(sbj).add(obj);
								invertedIndex.put(obj, invertedIndex.get(sbj));
							}
						}
						// else : nothing to be done
					} else {
						if (obj != null) {
							if (invertedIndex.containsKey(obj)) {
								invertedIndex.get(obj).add(sbj);
								invertedIndex.put(sbj, invertedIndex.get(obj));
							} else {
								// none of them are included
								HashSet<Resource> newSet = new HashSet<Resource>();
								newSet.add(sbj);
								newSet.add(obj);
								this._affectedResources.add(newSet);
								invertedIndex.put(sbj, newSet);
								invertedIndex.put(obj, newSet);
							}
						} else {
							// only the resouce a with itself in the set
							HashSet<Resource> newSet = new HashSet<Resource>();
							newSet.add(sbj);
							this._affectedResources.add(newSet);
							invertedIndex.put(sbj, newSet);
						}
					}
				}
			}

			this._flattenedAffectedResources = new HashSet<Resource>();
			this._affectedResources.forEach(e -> this._flattenedAffectedResources.addAll(e));
			// end of
		}

		return this._affectedResources;
	}

	/**
	 * Check if a resource is among the affected resources of the updates of the
	 * changeset
	 * 
	 * @param res
	 * @return
	 */
	public boolean isAffectedResource(Resource res) {

		if (this._flattenedAffectedResources != null) {
			return this._flattenedAffectedResources.contains(res);
		}
		return false;
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

		return sb.toString();
	}

	/**
	 * Check if the resource URI accept at least one filter
	 * 
	 * @param res
	 * @param filters
	 * @return
	 */
	public static boolean filterAcceptURI(Resource res, List<String> filters) {
		for (String filter : filters) {
			if (res.getURI().contains(filter)) {
				return true;
			}
		}
		return filters.isEmpty();
	}

	/**
	 * Check if the resource is accepted by the filters from DataConstants
	 * 
	 * @param res
	 * @return
	 */
	public static boolean filterAcceptURI(Resource res) {
		return filterAcceptURI(res, DataConstants.ACCEPTED_URI_FILTER.getStringList())
				&& !filterAcceptURI(res, DataConstants.REFUSED_URI_FILTER.getStringList());
	}

	/**
	 * Check if the resource is from dbpedia/ontology
	 * 
	 * @param res
	 * @return
	 */
	public static boolean filterOntologyURI(Resource res) {
		List<String> ontologyString = Collections.singletonList(ACCEPTED_URI_FILTER.dbpediaOntology.getString());
		return filterAcceptURI(res, ontologyString);
	}

	/**
	 * Check if the resource is from dbpedia/property
	 * 
	 * @param res
	 * @return
	 */
	public static boolean filterResourceURI(Resource res) {
		List<String> ontologyString = Collections.singletonList(ACCEPTED_URI_FILTER.dbpediaResource.getString());
		return filterAcceptURI(res, ontologyString);
	}

	/**
	 * Returns the size of the changeset in terms of triples
	 * 
	 * @return
	 */

	public long getUpdateSize() {
		return _addedTriples.size() + _deletedTriples.size();
	}

	/// input/output operations to handle the affected sets

	public void writeAffectedResources(PrintWriter out) {

		if (this._affectedResources == null) {
			this.getAffectedResources();
		}

		for (HashSet<Resource> set : this.getAffectedResources()) {
			for (Resource res : set) {
				out.println(res.getURI());
			}
			// they are just separted by a line
			out.println();
		}
	}

	public void readAffectedResources(BufferedReader in) throws IOException {
		this._affectedResources = new HashSet<HashSet<Resource>>();
		this._flattenedAffectedResources = new HashSet<Resource>();
		HashSet<Resource> auxiliar = new HashSet<Resource>();
		String line = null;
		while ((line = in.readLine()) != null) {
			if ("".equals(line)) {
				// an empty line == new set
				this._affectedResources.add(auxiliar);
				this._flattenedAffectedResources.addAll(auxiliar);
				auxiliar = new HashSet<Resource>();
			} else {
				auxiliar.add(ResourceFactory.createResource(line));
			}
		}
		// we add the last one
		if (!auxiliar.isEmpty()) {
			this._affectedResources.add(auxiliar);
			this._flattenedAffectedResources.addAll(auxiliar);
		}
	}

	private void extractFlatAffectedResources() {

		if (this._flattenedAffectedResources == null) {
			this._flattenedAffectedResources = new HashSet<Resource>();
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
					} else {
						obj = null;
					}

					if(! this._flattenedAffectedResources.contains(sbj)) {
						this._flattenedAffectedResources.add(sbj);
					}

					if(obj != null && ! this._flattenedAffectedResources.contains(obj)) {
						this._flattenedAffectedResources.add(obj);
					}
				}
			}

			if (!_deletedTriples.isEmpty()) {
				StmtIterator it = _deletedTriples.listStatements();
				while (it.hasNext()) {
					stmt = it.next();
					sbj = stmt.getSubject();
					if (stmt.getObject().isResource()) {
						obj = stmt.getObject().asResource();
					} else {
						obj = null;
					}

					if(! this._flattenedAffectedResources.contains(sbj)) {
						this._flattenedAffectedResources.add(sbj);
					}

					if(obj != null && ! this._flattenedAffectedResources.contains(obj)) {
						this._flattenedAffectedResources.add(obj);
					}
				}
			}
		}
	}

	public HashSet<Resource> getFlattenedAffectedResources() {
		if (this._flattenedAffectedResources == null) {
			this.extractFlatAffectedResources();
		}
		return this._flattenedAffectedResources;
	}

	public long getNumberFilteredTriples() {
		return _numberFilteredTriples;
	}
}
