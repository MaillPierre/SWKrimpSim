package com.irisa.swpatterns.data;

import org.apache.jena.rdf.model.Resource;

import com.irisa.jenautils.Couple;

public class RDFPatternPathFragment extends RDFPatternComponent {

	public RDFPatternPathFragment(Resource r1, Resource r2, Type type) {
		super(new RDFPatternElement(r1, r2), type);
	}
	
	public Couple<Resource, Resource> getPathFragment() {
		return this.getElement().getCouple();
	}
	
}
