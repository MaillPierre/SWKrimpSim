package com.irisa.swpatterns.data;

import org.apache.jena.rdf.model.Resource;

public class RDFPatternResource extends RDFPatternComponent {
	
	public RDFPatternResource(Resource res, RDFPatternResource.Type type) {
		super(new RDFPatternElement(res), type);
	}
	
	public Resource getResource() {
		return this.getElement().getResource();
	}

}
