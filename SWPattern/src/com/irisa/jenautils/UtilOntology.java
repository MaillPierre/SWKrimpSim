package com.irisa.jenautils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Ontology simple description class
 * Class hierarchy is described by pairs of <superclass, subclass>
 * @author maillot
 *
 */
public class UtilOntology {

	private HashMap<Resource, Couple<HashSet<Resource>, HashSet<Resource>>> _classes;
	private HashMap<Resource, Couple<HashSet<Resource>, HashSet<Resource>>> _properties;
	private HashSet<Resource> _ontologyClassVocabulary;
	private HashSet<Property> _ontologyPropertyVocabulary;
	private HashSet<Resource> _usedClasses; // Listes des classes avec une instanciation
	
	private Model _ontoModel;
	
	private static String classRegex = null;
	
	private static Logger logger = Logger.getLogger(UtilOntology.class);
	
	public UtilOntology() 
	{
		this._classes = new HashMap<Resource, Couple<HashSet<Resource>, HashSet<Resource>>>();
		this._properties = new HashMap<Resource, Couple<HashSet<Resource>, HashSet<Resource>>>();
		this._ontoModel = ModelFactory.createDefaultModel();
		this._ontologyClassVocabulary = new HashSet<Resource>();
		this._ontologyPropertyVocabulary = new HashSet<Property>();
		this._usedClasses = new HashSet<Resource>();

		this._ontologyClassVocabulary.add(RDF.Alt);
		this._ontologyClassVocabulary.add(RDF.Bag);
		this._ontologyClassVocabulary.add(RDF.HTML);
		this._ontologyClassVocabulary.add(RDF.Property);
		this._ontologyClassVocabulary.add(RDF.Seq);
		this._ontologyClassVocabulary.add(RDF.Statement);
		this._ontologyClassVocabulary.add(RDFS.Class);
		this._ontologyClassVocabulary.add(RDFS.Container);
		this._ontologyClassVocabulary.add(RDFS.ContainerMembershipProperty);
		this._ontologyClassVocabulary.add(RDFS.Datatype);
		this._ontologyClassVocabulary.add(RDFS.Literal);
		this._ontologyClassVocabulary.add(RDFS.Resource);
		this._ontologyClassVocabulary.add(OWL.AllDifferent);
		this._ontologyClassVocabulary.add(OWL.AnnotationProperty);
		this._ontologyClassVocabulary.add(OWL.Class);
		this._ontologyClassVocabulary.add(OWL.DataRange);
		this._ontologyClassVocabulary.add(OWL.DatatypeProperty);
		this._ontologyClassVocabulary.add(OWL.DeprecatedClass);
		this._ontologyClassVocabulary.add(OWL.DeprecatedProperty);
		this._ontologyClassVocabulary.add(OWL.FunctionalProperty);
		this._ontologyClassVocabulary.add(OWL.InverseFunctionalProperty);
		this._ontologyClassVocabulary.add(OWL.Nothing);
		this._ontologyClassVocabulary.add(OWL.ObjectProperty);
		this._ontologyClassVocabulary.add(OWL.Ontology);
		this._ontologyClassVocabulary.add(OWL.OntologyProperty);
		this._ontologyClassVocabulary.add(OWL.Restriction);
		this._ontologyClassVocabulary.add(OWL.SymmetricProperty);
		this._ontologyClassVocabulary.add(OWL.Thing);
		this._ontologyClassVocabulary.add(OWL.TransitiveProperty);
		this._ontologyClassVocabulary.add(OWL2.AllDifferent);
		this._ontologyClassVocabulary.add(OWL2.AllDisjointClasses);
		this._ontologyClassVocabulary.add(OWL2.AllDisjointProperties);
		this._ontologyClassVocabulary.add(OWL2.Annotation);
		this._ontologyClassVocabulary.add(OWL2.AnnotationProperty);
		this._ontologyClassVocabulary.add(OWL2.AsymmetricProperty);
		this._ontologyClassVocabulary.add(OWL2.Axiom);
		this._ontologyClassVocabulary.add(OWL2.Class);
		this._ontologyClassVocabulary.add(OWL2.DataRange);
		this._ontologyClassVocabulary.add(OWL2.DatatypeProperty);
		this._ontologyClassVocabulary.add(OWL2.DeprecatedClass);
		this._ontologyClassVocabulary.add(OWL2.DeprecatedProperty);
		this._ontologyClassVocabulary.add(OWL2.FunctionalProperty);
		this._ontologyClassVocabulary.add(OWL2.InverseFunctionalProperty);
		this._ontologyClassVocabulary.add(OWL2.IrreflexiveProperty);
		this._ontologyClassVocabulary.add(OWL2.NamedIndividual);
		this._ontologyClassVocabulary.add(OWL2.NegativePropertyAssertion);
		this._ontologyClassVocabulary.add(OWL2.Nothing);
		this._ontologyClassVocabulary.add(OWL2.ObjectProperty);
		this._ontologyClassVocabulary.add(OWL2.Ontology);
		this._ontologyClassVocabulary.add(OWL2.OntologyProperty);
		this._ontologyClassVocabulary.add(OWL2.ReflexiveProperty);
		this._ontologyClassVocabulary.add(OWL2.Restriction);
		this._ontologyClassVocabulary.add(OWL2.SymmetricProperty);
		this._ontologyClassVocabulary.add(OWL2.Thing);
		this._ontologyClassVocabulary.add(OWL2.TransitiveProperty);
		
//		this._ontologyPropertyVocabulary.add(RDF.first);
		this._ontologyPropertyVocabulary.add(RDF.object);
		this._ontologyPropertyVocabulary.add(RDF.predicate);
//		this._ontologyPropertyVocabulary.add(RDF.rest);
		this._ontologyPropertyVocabulary.add(RDF.subject);
		this._ontologyPropertyVocabulary.add(RDF.type);
//		this._ontologyPropertyVocabulary.add(RDF.value);
//		this._ontologyPropertyVocabulary.add(RDFS.comment);
		this._ontologyPropertyVocabulary.add(RDFS.domain);
		this._ontologyPropertyVocabulary.add(RDFS.isDefinedBy);
//		this._ontologyPropertyVocabulary.add(RDFS.label);
//		this._ontologyPropertyVocabulary.add(RDFS.member);
		this._ontologyPropertyVocabulary.add(RDFS.range);
//		this._ontologyPropertyVocabulary.add(RDFS.seeAlso);
		this._ontologyPropertyVocabulary.add(RDFS.subClassOf);
		this._ontologyPropertyVocabulary.add(RDFS.subPropertyOf);
		this._ontologyPropertyVocabulary.add(OWL.allValuesFrom);
		this._ontologyPropertyVocabulary.add(OWL.backwardCompatibleWith);
		this._ontologyPropertyVocabulary.add(OWL.cardinality);
		this._ontologyPropertyVocabulary.add(OWL.complementOf);
		this._ontologyPropertyVocabulary.add(OWL.differentFrom);
		this._ontologyPropertyVocabulary.add(OWL.disjointWith);
		this._ontologyPropertyVocabulary.add(OWL.distinctMembers);
		this._ontologyPropertyVocabulary.add(OWL.equivalentClass);
		this._ontologyPropertyVocabulary.add(OWL.equivalentProperty);
		this._ontologyPropertyVocabulary.add(OWL.hasValue);
		this._ontologyPropertyVocabulary.add(OWL.imports);
		this._ontologyPropertyVocabulary.add(OWL.incompatibleWith);
		this._ontologyPropertyVocabulary.add(OWL.intersectionOf);
		this._ontologyPropertyVocabulary.add(OWL.inverseOf);
		this._ontologyPropertyVocabulary.add(OWL.maxCardinality);
		this._ontologyPropertyVocabulary.add(OWL.minCardinality);
		this._ontologyPropertyVocabulary.add(OWL.oneOf);
		this._ontologyPropertyVocabulary.add(OWL.onProperty);
		this._ontologyPropertyVocabulary.add(OWL.priorVersion);
		this._ontologyPropertyVocabulary.add(OWL.sameAs);
		this._ontologyPropertyVocabulary.add(OWL.someValuesFrom);
		this._ontologyPropertyVocabulary.add(OWL.unionOf);
		this._ontologyPropertyVocabulary.add(OWL.versionInfo);
		this._ontologyPropertyVocabulary.add(OWL2.allValuesFrom);
		this._ontologyPropertyVocabulary.add(OWL2.annotatedProperty);
		this._ontologyPropertyVocabulary.add(OWL2.annotatedSource);
		this._ontologyPropertyVocabulary.add(OWL2.annotatedTarget);
		this._ontologyPropertyVocabulary.add(OWL2.assertionProperty);
		this._ontologyPropertyVocabulary.add(OWL2.backwardCompatibleWith);
		this._ontologyPropertyVocabulary.add(OWL2.bottomDataProperty);
		this._ontologyPropertyVocabulary.add(OWL2.bottomObjectProperty);
		this._ontologyPropertyVocabulary.add(OWL2.cardinality);
		this._ontologyPropertyVocabulary.add(OWL2.complementOf);
		this._ontologyPropertyVocabulary.add(OWL2.datatypeComplementOf);
		this._ontologyPropertyVocabulary.add(OWL2.deprecated);
		this._ontologyPropertyVocabulary.add(OWL2.differentFrom);
		this._ontologyPropertyVocabulary.add(OWL2.disjointUnionOf);
		this._ontologyPropertyVocabulary.add(OWL2.disjointWith);
		this._ontologyPropertyVocabulary.add(OWL2.distinctMembers);
		this._ontologyPropertyVocabulary.add(OWL2.equivalentClass);
		this._ontologyPropertyVocabulary.add(OWL2.equivalentProperty);
		this._ontologyPropertyVocabulary.add(OWL2.hasKey);
		this._ontologyPropertyVocabulary.add(OWL2.hasSelf);
//		this._ontologyPropertyVocabulary.add(OWL2.hasValue);
		this._ontologyPropertyVocabulary.add(OWL2.imports);
		this._ontologyPropertyVocabulary.add(OWL2.incompatibleWith);
		this._ontologyPropertyVocabulary.add(OWL2.intersectionOf);
		this._ontologyPropertyVocabulary.add(OWL2.inverseOf);
		this._ontologyPropertyVocabulary.add(OWL2.maxCardinality);
		this._ontologyPropertyVocabulary.add(OWL2.maxQualifiedCardinality);
		this._ontologyPropertyVocabulary.add(OWL2.members);
		this._ontologyPropertyVocabulary.add(OWL2.minCardinality);
		this._ontologyPropertyVocabulary.add(OWL2.minQualifiedCardinality);
		this._ontologyPropertyVocabulary.add(OWL2.onClass);
		this._ontologyPropertyVocabulary.add(OWL2.onDataRange);
		this._ontologyPropertyVocabulary.add(OWL2.onDatatype);
		this._ontologyPropertyVocabulary.add(OWL2.oneOf);
		this._ontologyPropertyVocabulary.add(OWL2.onProperties);
		this._ontologyPropertyVocabulary.add(OWL2.onProperty);
		this._ontologyPropertyVocabulary.add(OWL2.priorVersion);
		this._ontologyPropertyVocabulary.add(OWL2.propertyChainAxiom);
		this._ontologyPropertyVocabulary.add(OWL2.propertyDisjointWith);
		this._ontologyPropertyVocabulary.add(OWL2.qualifiedCardinality);
//		this._ontologyPropertyVocabulary.add(OWL2.sameAs);
		this._ontologyPropertyVocabulary.add(OWL2.someValuesFrom);
		this._ontologyPropertyVocabulary.add(OWL2.sourceIndividual);
		this._ontologyPropertyVocabulary.add(OWL2.targetIndividual);
		this._ontologyPropertyVocabulary.add(OWL2.targetValue);
		this._ontologyPropertyVocabulary.add(OWL2.topDataProperty);
		this._ontologyPropertyVocabulary.add(OWL2.topObjectProperty);
		this._ontologyPropertyVocabulary.add(OWL2.unionOf);
//		this._ontologyPropertyVocabulary.add(OWL2.versionInfo);
//		this._ontologyPropertyVocabulary.add(OWL2.versionIRI);
		this._ontologyPropertyVocabulary.add(OWL2.withRestrictions);
		
	}
	
	public static void setClassRegex(String regex) {
		classRegex = regex;
	}
	
	public UtilOntology(UtilOntology onto) 
	{
		this._classes = new HashMap<Resource, Couple<HashSet<Resource>, HashSet<Resource>>>(onto._classes);
		this._properties = new HashMap<Resource, Couple<HashSet<Resource>, HashSet<Resource>>>(onto._properties);
		this._ontoModel = ModelFactory.createDefaultModel().add(onto.getModel());
		this._ontologyClassVocabulary = new HashSet<Resource>(onto._ontologyClassVocabulary);
		this._ontologyPropertyVocabulary = new HashSet<Property>(onto._ontologyPropertyVocabulary);
	}
	
	public HashSet<Resource> getOntologyClassVocabulary() 
	{
		return this._ontologyClassVocabulary;
	}
	
	public void addClass(Resource r)
	{
		if(!this._classes.containsKey(r) && r != null && this.respectPattern(r))
		{
			this._classes.put(r, new Couple<HashSet<Resource>, HashSet<Resource>>(new HashSet<Resource>(), new HashSet<Resource>()));
		}
	}
	
	/**
	 * r1 subclassOf r2
	 * @param r1
	 * @param r2
	 */
	public void addIsSubclassOf(Resource r1, Resource r2)
	{
		if(! this.isSubclassOf(r2, r1) && ! this.isSuperclassOf(r1, r2) && r1 != r2 && this.respectPattern(r1) && this.respectPattern(r2)) { // Pour éviter les boucles
			if(!this._classes.containsKey(r1))
			{
				addClass(r1);
			}
				this._classes.get(r1).getFirst().add(r2); // r2 superclass de r1
			if(!this._classes.containsKey(r2))
			{
				addClass(r2);
			}
			this._classes.get(r2).getSecond().add(r1); // r1 sousclasse de r2
		}
	}
	
	public HashSet<Resource> getSuperclass(Resource r) {
		HashSet<Resource> parents = new HashSet<Resource>();
		if(this._classes.containsKey(r))
		{
			Iterator<Resource> it = this._classes.get(r).getFirst().iterator();
			parents.addAll(this._classes.get(r).getFirst());
			while(it.hasNext())
			{
				Resource par = it.next();
				parents.addAll(getSuperclass(par));
			}
		}
		return parents;
	}
	
	public HashSet<Resource> getSubclass(Resource r)
	{
		HashSet<Resource> children = new HashSet<Resource>();
		if(this._classes.containsKey(r))
		{
			Iterator<Resource> it = this._classes.get(r).getSecond().iterator();
			children.addAll(this._classes.get(r).getSecond());
			while(it.hasNext())
			{
				Resource par = it.next();
				if(! children.contains(par)) {
					children.addAll(getSubclass(par));
				}
			}
		}
		return children;
	}
	
	public HashSet<Resource> transitiveSubclasses(Resource r) {
		HashSet<Resource> result = new HashSet<Resource>();
		
		Iterator<Resource> itSubs = this.getSubclass(r).iterator();
		while(itSubs.hasNext()) {
			Resource subClass = itSubs.next();
			result.add(subClass);
			result.addAll(this.transitiveSubclasses(subClass));
		}
		
		return result;
	}
	
	public boolean hasSuperclass(Resource r)
	{
		return !this._classes.get(r).getFirst().isEmpty();
	}
	
	public boolean hasSubclass(Resource r)
	{
		return !this._classes.get(r).getSecond().isEmpty();
	}
	
	public Iterator<Resource> classIterator()
	{
		return this._classes.keySet().iterator();
	}
	
	public Iterator<Resource> usedClassIterator()
	{
		return this._usedClasses.iterator();
	}
	
	public HashSet<Resource> classes()
	{
		return new HashSet<Resource>(this._classes.keySet());
	}
	
	public HashSet<Resource> usedClasses()
	{
		return this._usedClasses;
	}
	
	public boolean isClass(Resource r)
	{
		return this._classes.containsKey(r);
	}
	
	public boolean isClass(String uri) {
		return isClass(this._ontoModel.createResource(uri));
	}
	
	public boolean isSubclassOf(Resource r1, Resource r2)
	{
		return getSuperclass(r1).contains(r2);
	}
	
	public boolean isSuperclassOf(Resource r1, Resource r2)
	{
		return getSubclass(r1).contains(r2);
	}

	public boolean isProperty(Resource r)
	{
		return this._properties.containsKey(r);
	}
	
	public boolean isProperty(String uri) {
		return isProperty(this._ontoModel.createResource(uri));
	}
	
	public void addProperty(Resource r)
	{
		if(!this._properties.containsKey(r))
		{
			this._properties.put(r, new Couple<HashSet<Resource>, HashSet<Resource>>(new HashSet<Resource>(), new HashSet<Resource>()));
		}
	}
	
	public void addIsSubproperty(Resource r1, Resource r2)
	{
		addProperty(r1);
		this._properties.get(r1).getFirst().add(r2);
		addProperty(r2);
		this._properties.get(r2).getSecond().add(r1);
	}
	
	public HashSet<Resource> getSuperproperty(Resource r)
	{
		HashSet<Resource> parents = new HashSet<Resource>();
		if(this._properties.containsKey(r))
		{
			Iterator<Resource> it = this._properties.get(r).getFirst().iterator();
			parents.addAll(this._properties.get(r).getFirst());
			while(it.hasNext())
			{
				Resource par = it.next();
				parents.addAll(getSuperproperty(par));
			}
		}
		return parents;
	}
	
	public HashSet<Resource> getSubproperty(Resource r)
	{
		HashSet<Resource> children = new HashSet<Resource>();
		if(this._properties.containsKey(r))
		{
			Iterator<Resource> it = this._properties.get(r).getSecond().iterator();
			children.addAll(this._properties.get(r).getSecond());
			while(it.hasNext())
			{
				Resource par = it.next();
				children.addAll(getSubproperty(par));
			}
		}
		return children;
	}
	
	public boolean hasSuperproperty(Resource r)
	{
		return ! this._properties.get(r).getFirst().isEmpty();
	}
	
	public boolean hasSubproperty(Resource r)
	{
		return ! this._properties.get(r).getSecond().isEmpty();
	}
	
	public Set<Resource> properties() {
		return this._properties.keySet();
	}
	
	public Iterator<Resource> propertyIterator()
	{
		return this._properties.keySet().iterator();
	}
	
	public boolean isSubpropertyOf(Resource r1, Resource r2)
	{
		return getSuperproperty(r1).contains(r2);
	}
	
	public boolean isSuperpropertyOf(Resource r1, Resource r2)
	{
		return getSubproperty(r1).contains(r2);
	}
	
	public boolean isAboveLine(Resource r, LinkedList<Resource> compLine) throws Exception
	{
		if(!this._classes.containsKey(r))
		{
			throw new Exception();
		}
		else
		{
			Iterator<Resource> it = compLine.iterator();
			HashSet<Resource> above = new HashSet<Resource>();
			while(it.hasNext())
			{
				Resource c = it.next();
				above.addAll(getSuperclass(c));
			}
			return above.contains(r);
		}
	}
	
	public boolean isBelowLine(Resource r, LinkedList<Resource> compLine) throws Exception
	{
		if(!this._classes.containsKey(r))
		{
			throw new Exception();
		}
		else
		{
			Iterator<Resource> it = compLine.iterator();
			HashSet<Resource> below = new HashSet<Resource>();
			while(it.hasNext())
			{
				Resource c = it.next();
				below.addAll(getSubclass(c));
			}
			return below.contains(r);
		}
	}
	
	public boolean isOnLine(Resource r, LinkedList<Resource> compLine)
	{
		return compLine.contains(r);
	}
	
	public boolean isOntologyClassVocabulary(Resource r) {
		return this._ontologyClassVocabulary.contains(r);
	}
	
	public boolean isOntologyPropertyVocabulary(Resource r) {
		return this._ontologyPropertyVocabulary.contains(r);
	}
	
	public boolean respectPattern(Resource r) {
		return ((classRegex != null && r.toString().contains(classRegex)) || (classRegex == null));
	}
	
	public String toString()
	{
		String result = "";
		Iterator<Resource> it = this._classes.keySet().iterator();
		while(it.hasNext())
		{
			Resource key = it.next();
			result += key + " : " + this._classes.get(key) + "\n"; 
		}
		
		return result;
	}
	
	public Model getModel() {
		return this._ontoModel;
	}
	
	public void addTripleToModel(Resource s, Property p, Resource o) {
		this._ontoModel.add(s, p, o);
	}
	
	private void initClasses(QueryIteratorFurnisher furnisher) {
		logger.trace("Init classes");
		// Classes utilisées comme types
		String typeClassSelectString = "SELECT DISTINCT ?c WHERE { ?i a ?c . }";
		Query typeClassSelect = QueryFactory.create(typeClassSelectString);
//		QueryExecution typeClassSelectExec = tbase.executionQuery(typeClassSelect);
		QueryResultIterator classTypeResult = furnisher.retrieve(typeClassSelect); //new QueryResultIterator(typeClassSelect, tbase);
		try 
		{
			while(classTypeResult.hasNext())
			{
				CustomQuerySolution sol = classTypeResult.next();
				Resource nClass = sol.getResource("c");
//				logger.debug("Class used " + nClass + " " + classRegex);
				if(nClass != null && ! this.isOntologyClassVocabulary(nClass))
				{
					if(respectPattern(nClass)) {
						this.addClass(nClass);
						this._usedClasses.add(nClass);
					}
				} 
			}
		} 
		finally
		{
			classTypeResult.close();
		}
		
		logger.trace("init classes: rdfs classes");
		// Classe déclarée en RDFS
		String classRDFSSelectString = "SELECT DISTINCT ?c WHERE { ?c a <" + RDFS.Class + "> . }";
		Query classRDFSSelect = QueryFactory.create(classRDFSSelectString);
//		QueryExecution classRDFSSelectExec = tbase.executionQuery(classRDFSSelect);
		QueryResultIterator classRDFSResult = furnisher.retrieve(classRDFSSelect); //new QueryResultIterator(classRDFSSelect, tbase);
		try 
		{
			while(classRDFSResult.hasNext())
			{
				CustomQuerySolution sol = classRDFSResult.next();
				Resource nClass = sol.getResource("c");
				if(nClass != null && ! this.isOntologyClassVocabulary(nClass))
				{
					if(respectPattern(nClass)) {
						logger.trace("Class RDFS " + nClass);
						this.addClass(nClass);
						this.addTripleToModel(nClass, RDF.type, RDFS.Class);
					}
				}
			}
		} 
		finally
		{
			classRDFSResult.close();
		}

		logger.trace("init Onto: owl classes");
		// Classe déclarée en OWL
		String classOWLSelectString = "SELECT DISTINCT ?c WHERE { ?c a <" + OWL.Class + "> . }";
		Query classOWLSelect = QueryFactory.create(classOWLSelectString);
//		QueryExecution classOWLSelectExec = tbase.executionQuery(classOWLSelect);
		QueryResultIterator classOWLResult = furnisher.retrieve( classOWLSelect);//new QueryResultIterator( classOWLSelect, tbase);
		try 
		{
			while(classOWLResult.hasNext())
			{
				CustomQuerySolution sol = classOWLResult.next();
				Resource nClass = sol.getResource("c");
				if(nClass != null && ! this.isOntologyClassVocabulary(nClass))
				{
					if(respectPattern(nClass)) {
						logger.trace("Class OWL " + nClass);
						this.addClass(nClass);
						this.addTripleToModel(nClass, RDF.type, OWL.Class);
					}
				}
			}
		} 
		finally
		{
			classOWLResult.close();
		}

		logger.trace("init Onto: classes and subclasses");
		// Classes et sous-classes
		String subClassOfSelectString = "SELECT DISTINCT ?c1 ?c2 WHERE { ?c1 <" + RDFS.subClassOf + "> ?c2 . }";
		Query subclassOfSelect = QueryFactory.create(subClassOfSelectString);
//		QueryExecution subclassOfExec = tbase.executionQuery(subclassOfSelect);
		QueryResultIterator subclassOfResult = furnisher.retrieve(subclassOfSelect);//new QueryResultIterator(subclassOfSelect, tbase);
		try 
		{
			while(subclassOfResult.hasNext())
			{
				CustomQuerySolution sol = subclassOfResult.next();
				Resource classC1 = sol.getResource("c1");
				Resource classC2 = sol.getResource("c2");
				if(classC1 != null 
						&& classC2 != null 
						&& ! this.isOntologyClassVocabulary(classC1) 
						&& ! this.isOntologyClassVocabulary(classC2))
				{
					if(respectPattern(classC1) && respectPattern(classC2)) {
						logger.trace("Class " + classC1 + " subClassOf " + classC2);
						this.addIsSubclassOf(classC1, classC2);
						this.addTripleToModel(classC1, RDFS.subClassOf, classC2);
					}
				}
			}
		} 
		finally
		{
			subclassOfResult.close();
		}

	}
	
	private void initProperties(QueryIteratorFurnisher furnisher) {
		
		logger.trace("init Onto: rdfs subproperties");
		// Propriétés utilisées avec subPropertyOf
		String subpropertyOfSelectString = "SELECT DISTINCT ?p1 ?p2 WHERE { ?p1 <" + RDFS.subPropertyOf + "> ?p2 . }";
		Query subpropertyOfSelect = QueryFactory.create(subpropertyOfSelectString);
//		QueryExecution subpropertyOfExec = tbase.executionQuery(subpropertyOfSelect);
		QueryResultIterator subpropertyOfResult = furnisher.retrieve(subpropertyOfSelect);//new QueryResultIterator(subpropertyOfSelect, tbase);
		try 
		{
			while(subpropertyOfResult.hasNext())
			{
				CustomQuerySolution sol = subpropertyOfResult.next();
				Resource prop1 = sol.getResource("p1");
				Resource prop2 = sol.getResource("p2");
				if(prop1 != null && prop2 != null) 
				{
					this.addIsSubproperty(prop1, prop2);
					this.addTripleToModel(prop1, RDFS.subPropertyOf, prop2);
				}
			}
		} 
		finally
		{
			subpropertyOfResult.close();
		}
		
		logger.trace("init Onto: rdfs properties");
		// Propriété déclarée en RDFS
		String propertyRDFSSelectString = "SELECT DISTINCT ?p WHERE { ?p a <" + RDF.Property + "> . }";
		Query propertyRDFSSelect = QueryFactory.create(propertyRDFSSelectString);
//		QueryExecution propertyRDFSSelectExec = tbase.executionQuery(propertyRDFSSelect);
		QueryResultIterator propertyRDFSResult = furnisher.retrieve(propertyRDFSSelect); //new QueryResultIterator(propertyRDFSSelect, tbase);
		try 
		{
			while(propertyRDFSResult.hasNext())
			{
				CustomQuerySolution sol = propertyRDFSResult.next();
				Resource nProp = sol.getResource("p");
				if(nProp != null && ! this.isOntologyPropertyVocabulary(nProp))
				{
					logger.trace("Property RDFS " + nProp);
					this.addProperty(nProp);
					this.addTripleToModel(nProp, RDF.type, RDF.Property);
				}
			}
		} 
		finally
		{
			propertyRDFSResult.close();
		}

		logger.trace("init Onto: owl datatype properties");
		// Propriété datatype déclarée en OWL
		String datatypePropertyOWLSelectString = "SELECT DISTINCT ?p WHERE { ?p a <" + OWL.DatatypeProperty + "> . }";
		Query datatypePropertyOWLSelect = QueryFactory.create(datatypePropertyOWLSelectString);
//		QueryExecution datatypePropertyOWLSelectExec = tbase.executionQuery(datatypePropertyOWLSelect);
		QueryResultIterator datatypePropertyOWLResult = furnisher.retrieve(datatypePropertyOWLSelect);//new QueryResultIterator(datatypePropertyOWLSelect, tbase);
		try 
		{
			while(datatypePropertyOWLResult.hasNext())
			{
				CustomQuerySolution sol = datatypePropertyOWLResult.next();
				Resource nProp = sol.getResource("p");
				if(nProp != null && ! this.isOntologyPropertyVocabulary(nProp))
				{
					logger.trace("Property OWL " + nProp);
					this.addProperty(nProp);
					this.addTripleToModel(nProp, RDF.type, OWL.DatatypeProperty);
				}
			}
		} 
		finally
		{
			datatypePropertyOWLResult.close();
		}

		logger.trace("init Onto: owl object properties");
		// Propriété object déclarée en OWL
		String objectPropertyOWLSelectString = "SELECT DISTINCT ?p WHERE { ?p a <" + OWL.ObjectProperty + "> . }";
		Query objectPropertyOWLSelect = QueryFactory.create(objectPropertyOWLSelectString);
//		QueryExecution objectPropertyOWLSelectExec = tbase.executionQuery(objectPropertyOWLSelect);
		QueryResultIterator objectPropertyOWLResult = furnisher.retrieve(objectPropertyOWLSelect);// new QueryResultIterator(objectPropertyOWLSelect, tbase);
		try 
		{
			while(objectPropertyOWLResult.hasNext())
			{
				CustomQuerySolution sol = objectPropertyOWLResult.next();
				Resource nProp = sol.getResource("p");
				if(nProp != null && ! this.isOntologyPropertyVocabulary(nProp))
				{
					logger.trace("Property OWL " + nProp);
					this.addProperty(nProp);
					this.addTripleToModel(nProp, RDF.type, OWL.ObjectProperty);
				}
			}
		} 
		finally
		{
			objectPropertyOWLResult.close();
		}

		logger.trace("init Onto: owl transitive properties");
		// Propriété transitive déclarée en OWL
		String transitivePropertyOWLSelectString = "SELECT DISTINCT ?p WHERE { ?p a <" + OWL.TransitiveProperty + "> . }";
		Query transitivePropertyOWLSelect = QueryFactory.create(transitivePropertyOWLSelectString);
//		QueryExecution transitivePropertyOWLSelectExec = tbase.executionQuery(transitivePropertyOWLSelect);
		QueryResultIterator transitivePropertyOWLResult = furnisher.retrieve(transitivePropertyOWLSelect); // new QueryResultIterator(transitivePropertyOWLSelect, tbase);
		try 
		{
			while(transitivePropertyOWLResult.hasNext())
			{
				CustomQuerySolution sol = transitivePropertyOWLResult.next();
				Resource nProp = sol.getResource("p");
				if(nProp != null && ! this.isOntologyPropertyVocabulary(nProp))
				{
					logger.trace("Property OWL " + nProp);
					this.addProperty(nProp);
					this.addTripleToModel(nProp, RDF.type, OWL.TransitiveProperty);
				}
			}
		} 
		finally
		{
			transitivePropertyOWLResult.close();
		}

		logger.trace("init Onto: owl symmetric properties");
		// Propriété symétrique déclarée en OWL
		String symmetricPropertyOWLSelectString = "SELECT DISTINCT ?p WHERE { ?p a <" + OWL.SymmetricProperty + "> . }";
		Query symmetricPropertyOWLSelect = QueryFactory.create(symmetricPropertyOWLSelectString);
//		QueryExecution symmetricPropertyOWLSelectExec = tbase.executionQuery(symmetricPropertyOWLSelect);
		QueryResultIterator symmetricPropertyOWLResult = furnisher.retrieve(symmetricPropertyOWLSelect); //new QueryResultIterator(symmetricPropertyOWLSelect, tbase);
		try 
		{
			while(symmetricPropertyOWLResult.hasNext())
			{
				CustomQuerySolution sol = symmetricPropertyOWLResult.next();
				Resource nProp = sol.getResource("p");
				if(nProp != null && ! this.isOntologyPropertyVocabulary(nProp))
				{
					logger.trace("Property OWL " + nProp);
					this.addProperty(nProp);
					this.addTripleToModel(nProp, RDF.type, OWL.SymmetricProperty);
				}
			}
		} 
		finally
		{
			symmetricPropertyOWLResult.close();
		}

		logger.trace("init Onto: owl functional properties");
		// Propriété symétrique déclarée en OWL
		String functionalPropertyOWLSelectString = "SELECT DISTINCT ?p WHERE { ?p a <" + OWL.FunctionalProperty + "> . }";
		Query functionalPropertyOWLSelect = QueryFactory.create(functionalPropertyOWLSelectString);
//		QueryExecution functionalPropertyOWLSelectExec = tbase.executionQuery(functionalPropertyOWLSelect);
		QueryResultIterator functionalPropertyOWLResult = furnisher.retrieve(functionalPropertyOWLSelect) ; // new QueryResultIterator(functionalPropertyOWLSelect, tbase);
		try 
		{
			while(functionalPropertyOWLResult.hasNext())
			{
				CustomQuerySolution sol = functionalPropertyOWLResult.next();
				Resource nProp = sol.getResource("p");
				if(nProp != null && ! this.isOntologyPropertyVocabulary(nProp))
				{
					logger.trace("Property OWL " + nProp);
					this.addProperty(nProp);
					this.addTripleToModel(nProp, RDF.type, OWL.FunctionalProperty);
				}
			}
		} 
		finally
		{
			functionalPropertyOWLResult.close();
		}

//		logger.trace("init Onto: actual used properties");
//		// Propriété symétrique déclarée en OWL
//		String usedPropertiesSelectString = "SELECT DISTINCT ?p WHERE { ?s ?p ?o . }";
//		Query usedPropertiesSelect = QueryFactory.create(usedPropertiesSelectString);
//		QueryExecution usedPropertiesSelectExec = tbase.executionQuery(usedPropertiesSelect);
//		try 
//		{
//			QueryResultIterator usedPropertiesResult = new QueryResultIterator(usedPropertiesSelectExec);
//			while(usedPropertiesResult.hasNext())
//			{
//				CustomQuerySolution sol = usedPropertiesResult.next();
//				Resource nProp = sol.getResource("p");
//				if(nProp != null && ! this.isOntologyPropertyVocabulary(nProp))
//				{
//					logger.trace("Property used " + nProp);
//					this.addProperty(nProp);
//				}
//			}
//		} 
//		finally
//		{
//			functionalPropertyOWLSelectExec.close();
//		}
		
	}
	
	public int numberOfClasses() {
		return this.classes().size();
	}
	
	public int numberOfProperties() {
		return this.properties().size();
	}

	public void init(BaseRDF tbase)
	{
		logger.trace("INIT ONTO");
		
		QueryIteratorFurnisher newFurnisher = new QueryIteratorFurnisher() {
			@Override
			public QueryResultIterator retrieve(Query query) {
				return new QueryResultIterator(query, tbase);
			}
		};
		
		init(newFurnisher);

		logger.trace("FIN INITONTO");
	}
	
	public void init(QueryIteratorFurnisher furnisher) {
		logger.trace("INIT ONTO");
		
		initClasses(furnisher);

		initProperties(furnisher);
		
		logger.trace("FIN INITONTO");
	}

	public void close() {
		this._ontoModel.close();
	}

}
