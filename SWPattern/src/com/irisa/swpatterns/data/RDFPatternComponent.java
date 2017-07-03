package com.irisa.swpatterns.data;

import java.util.Comparator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import com.irisa.exception.LogicException;

/**
 * Elements of a pattern, based on design pattern Composite
 * @author pmaillot
 *
 */
public abstract class RDFPatternComponent {

	public enum Type {
		TYPE, // Type resource alone
		IN_PROPERTY, // Property resource of an in-going triple alone
		OUT_PROPERTY, // Property resource of an out-going triple alone
		OUT_NEIGHBOUR_TYPE, // Property and type of the object of an out-going triple
		OUT_NEIGHBOUR, // Property and object of an out-going triple
		IN_NEIGHBOUR_TYPE, // Property and type of the subject of an in-going triple
		IN_NEIGHBOUR, // Property and subject of an in-going triple
		// For Paths
		NODE1, // First node of a path
		NODE1TYPE, // Type of the first node of a path
		RELATION1, // First relation of a path
		NODE2, // Second node of a path
		NODE2TYPE, // Type of the second node of a path
		RELATION2, // Second relation of a path
		NODE3, // Third node of a path
		NODE3TYPE, // Type of the third node of a path
		RELATION3, // Third relation of a path
		NODE4, // Fourth node of a path
		NODE4TYPE, // Type of the fourth node of a path
		RELATION4, // Fourth relation of a path
		NODE5, // Fifth node of a path
		NODE5TYPE, // Type of the fifth node of a path
	};
	
	protected RDFPatternElement _element = null; 
	protected Type _type;
	
	protected RDFPatternComponent(RDFPatternElement element, Type type) {
		this._element = element;
		this._type = type;
	}
	
	protected RDFPatternElement getElement() {
		return this._element;
	}
	
	public Type getType() {
		return this._type;
	}

	@Override
	public String toString() {
		return this.getElement().toString() + " " + this.getType() ;
	}
	
	public static RDFPatternComponent parse(String element) {
		String[] splitElem = element.split("\t");
		
		switch(splitElem.length) {
		case 2:
			return parse(splitElem[0], splitElem[1]);
		case 3:
			return parse(splitElem[0], splitElem[1], splitElem[2]);
		default:
			throw new LogicException("Coudn't parse " + element + " to RDFPatternComponent");
		}
	}
	
	public static RDFPatternComponent parse(String element1, String element2) {
		Model parserModel = ModelFactory.createDefaultModel();
		RDFPatternComponent result;
		
		Resource res = parserModel.getResource(element1);
		Type type = Type.valueOf(element2);
		result = new RDFPatternResource(res, type);
		parserModel.close();
		return result;
	}
	
	public static RDFPatternComponent parse(String element1, String element2, String element3) {
		Model parserModel = ModelFactory.createDefaultModel();
		RDFPatternComponent result;
		Resource res1 = parserModel.getResource(element1);
		Resource res2 = parserModel.getResource(element2);
		Type typePath = Type.valueOf(element3);
		result = new RDFPatternPathFragment(res1, res2, typePath);
		parserModel.close();
		
		return result;
	}

	@Override
	public int hashCode() {
		return this.getElement().hashCode() + this.getType().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof RDFPatternComponent) {
			return ((RDFPatternComponent) o).getElement().equals(this.getElement()) && ((RDFPatternComponent) o).getType() == this.getType();
		}
		return false;
	}
	
	public static Comparator<RDFPatternComponent> getComparator() {
		return new Comparator<RDFPatternComponent>() {
			@Override
			public int compare(RDFPatternComponent o1, RDFPatternComponent o2) {
				if(o1.getType() == o2.getType()) {
					return o1.toString().compareTo(o2.toString());
				} else if(o1.getType() == Type.TYPE ) {
					return 1;
				}else if(o2.getType() == Type.TYPE ) {
					return -1;
				} else if(o1.getType() == Type.OUT_PROPERTY) {
					return 1;
				}else if(o2.getType() == Type.OUT_PROPERTY ) {
					return -1;
				} else if(o1.getType() == Type.IN_PROPERTY) {
					return 1;
				} else if(o2.getType() == Type.IN_PROPERTY ) {
					return -1;
				} else if(o1.getType() == Type.OUT_NEIGHBOUR_TYPE) {
					return 1;
				} else if(o2.getType() == Type.OUT_NEIGHBOUR_TYPE) {
					return -1;
				} else if(o1.getType() == Type.IN_NEIGHBOUR_TYPE) {
					return 1;
				} else if(o2.getType() == Type.IN_NEIGHBOUR_TYPE) {
					return -1;
				} else if(o1.getType() == Type.OUT_NEIGHBOUR) {
					return 1;
				} else if(o2.getType() == Type.OUT_NEIGHBOUR) {
					return -1;
				} else if(o1.getType() == Type.IN_NEIGHBOUR) {
					return 1;
				} else if(o2.getType() == Type.IN_NEIGHBOUR) {
					return -1;
				} else if(o1.getType() == Type.NODE1) {
					return 1;
				} else if(o2.getType() == Type.NODE1) {
					return -1;
				} else if(o1.getType() == Type.NODE1TYPE) {
					return 1;
				} else if(o2.getType() == Type.NODE1TYPE) {
					return -1;
				} else if(o1.getType() == Type.RELATION1) {
					return 1;
				} else if(o2.getType() == Type.RELATION1) {
					return -1;
				} else if(o1.getType() == Type.NODE2) {
					return 1;
				} else if(o2.getType() == Type.NODE2) {
					return -1;
				} else if(o1.getType() == Type.NODE2TYPE) {
					return 1;
				} else if(o2.getType() == Type.NODE2TYPE) {
					return -1;
				} else if(o1.getType() == Type.RELATION2) {
					return 1;
				} else if(o2.getType() == Type.RELATION2) {
					return -1;
				} else if(o1.getType() == Type.NODE3) {
					return 1;
				} else if(o2.getType() == Type.NODE3) {
					return -1;
				} else if(o1.getType() == Type.NODE3TYPE) {
					return 1;
				} else if(o2.getType() == Type.NODE3TYPE) {
					return -1;
				} else if(o1.getType() == Type.RELATION3) {
					return 1;
				} else if(o2.getType() == Type.RELATION3) {
					return -1;
				} else if(o1.getType() == Type.NODE4) {
					return 1;
				} else if(o2.getType() == Type.NODE4) {
					return -1;
				} else if(o1.getType() == Type.NODE4TYPE) {
					return 1;
				} else if(o2.getType() == Type.NODE4TYPE) {
					return -1;
				} else if(o1.getType() == Type.RELATION4) {
					return 1;
				} else if(o2.getType() == Type.RELATION4) {
					return -1;
				} else if(o1.getType() == Type.NODE5) {
					return 1;
				} else if(o2.getType() == Type.NODE5) {
					return -1;
				} else if(o1.getType() == Type.NODE5TYPE) {
					return 1;
				} else if(o2.getType() == Type.NODE5TYPE) {
					return -1;
				}
				return -1;
			}
		};
	}
	
}
