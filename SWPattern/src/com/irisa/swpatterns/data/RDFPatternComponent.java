package com.irisa.swpatterns.data;

import java.util.Comparator;

/**
 * Elements of a pattern, based on design pattern Composite
 * @author pmaillot
 *
 */
public abstract class RDFPatternComponent {

	public enum Type {
		Type, // Type resource alone
		In, // Property resource of an in-going triple alone
		Out, // Property resource of an out-going triple alone
		OutNeighbourType, // Property and type of the object of an out-going triple
		InNeighbourType, // Property and type of the subject of an in-going triple
		// For Paths
		Node1, // First node of a path
		Node1Type, // Type of the first node of a path
		Relation1, // First relation of a path
		Node2, // Second node of a path
		Node2Type, // Type of the second node of a path
		Relation2, // Second relation of a path
		Node3, // Third node of a path
		Node3Type, // Type of the third node of a path
		Relation3, // Third relation of a path
		Node4, // Fourth node of a path
		Node4Type, // Type of the fourth node of a path
		Relation4, // Fourth relation of a path
		Node5, // Fifth node of a path
		Node5Type, // Type of the fifth node of a path
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
		return this.getElement().toString() + " (" + this.getType() + ")";
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
				} else if(o1.getType() == Type.Type ) {
					return 1;
				}else if(o2.getType() == Type.Type ) {
					return -1;
				} else if(o1.getType() == Type.Out) {
					return 1;
				}else if(o2.getType() == Type.Out ) {
					return -1;
				} else if(o1.getType() == Type.In) {
					return 1;
				} else if(o2.getType() == Type.In ) {
					return -1;
				} else if(o1.getType() == Type.OutNeighbourType) {
					return 1;
				} else if(o2.getType() == Type.OutNeighbourType) {
					return -1;
				} else if(o1.getType() == Type.InNeighbourType) {
					return 1;
				} else if(o2.getType() == Type.InNeighbourType) {
					return -1;
				} else if(o1.getType() == Type.Node1) {
					return 1;
				} else if(o2.getType() == Type.Node1) {
					return -1;
				} else if(o1.getType() == Type.Node1Type) {
					return 1;
				} else if(o2.getType() == Type.Node1Type) {
					return -1;
				} else if(o1.getType() == Type.Relation1) {
					return 1;
				} else if(o2.getType() == Type.Relation1) {
					return -1;
				} else if(o1.getType() == Type.Node2) {
					return 1;
				} else if(o2.getType() == Type.Node2) {
					return -1;
				} else if(o1.getType() == Type.Node2Type) {
					return 1;
				} else if(o2.getType() == Type.Node2Type) {
					return -1;
				} else if(o1.getType() == Type.Relation2) {
					return 1;
				} else if(o2.getType() == Type.Relation2) {
					return -1;
				} else if(o1.getType() == Type.Node3) {
					return 1;
				} else if(o2.getType() == Type.Node3) {
					return -1;
				} else if(o1.getType() == Type.Node3Type) {
					return 1;
				} else if(o2.getType() == Type.Node3Type) {
					return -1;
				} else if(o1.getType() == Type.Relation3) {
					return 1;
				} else if(o2.getType() == Type.Relation3) {
					return -1;
				} else if(o1.getType() == Type.Node4) {
					return 1;
				} else if(o2.getType() == Type.Node4) {
					return -1;
				} else if(o1.getType() == Type.Node4Type) {
					return 1;
				} else if(o2.getType() == Type.Node4Type) {
					return -1;
				} else if(o1.getType() == Type.Relation4) {
					return 1;
				} else if(o2.getType() == Type.Relation4) {
					return -1;
				} else if(o1.getType() == Type.Node5) {
					return 1;
				} else if(o2.getType() == Type.Node5) {
					return -1;
				} else if(o1.getType() == Type.Node5Type) {
					return 1;
				} else if(o2.getType() == Type.Node5Type) {
					return -1;
				}
				return -1;
			}
		};
	}
	
}
