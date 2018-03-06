package com.irisa.swpatterns.data;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.jena.rdf.model.Resource;

import com.irisa.utilities.Couple;

public class RDFPatternPathFragment extends RDFPatternComponent {

	public RDFPatternPathFragment(Resource r1, Resource r2, Type type) {
		super(new RDFPatternElement(r1, r2), type);
	}
	
	public Couple<Resource, Resource> getPathFragment() {
		return this.getElement().getCouple();
	}

	@Override
	public List<Object> toList() {
		LinkedList<Object> result = new LinkedList<Object>();
		result.add(getPathFragment().getFirst());
		result.add(getPathFragment().getSecond());
		result.add(getType());
		
		return result;
	}
	
	
	
}
