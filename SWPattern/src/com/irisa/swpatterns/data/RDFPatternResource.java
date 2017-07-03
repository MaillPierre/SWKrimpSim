package com.irisa.swpatterns.data;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.jena.rdf.model.Resource;

/**
 * patternComponent with just a resource
 * @author pmaillot
 *
 */
public class RDFPatternResource extends RDFPatternComponent {
	
	public RDFPatternResource(Resource res, RDFPatternResource.Type type) {
		super(new RDFPatternElement(res), type);
	}
	
	public Resource getResource() {
		return this.getElement().getResource();
	}

	@Override
	public List<Object> toList() {
		LinkedList<Object> result = new LinkedList<Object>();
		result.add(getResource());
		result.add(getType());
		
		return result;
	}

}
