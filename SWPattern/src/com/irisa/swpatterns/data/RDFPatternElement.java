package com.irisa.swpatterns.data;

import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.Logger;

import com.irisa.jenautils.Couple;

/**
 * Encapsulate elements from Jena, either a resource or a couple of resources
 * @author pmaillot
 *
 */
public class RDFPatternElement {

	private static Logger logger = Logger.getLogger(RDFPatternElement.class);

	private Resource _res = null;
	private Couple<Resource, Resource> _couple = null;
	
	public RDFPatternElement(Resource r) {
		this.setresource(r);
	}
	
	public RDFPatternElement(Resource r1, Resource r2) {
		this(new Couple<Resource, Resource>(r1, r2));
	}
	
	public RDFPatternElement(Couple<Resource, Resource> couple) {
		this.setCouple(couple);
	}

	public Couple<Resource, Resource> getCouple() {
		return _couple;
	}

	public void setCouple(Couple<Resource, Resource> _couple) {
		this._couple = _couple;
	}

	public Resource getResource() {
		return _res;
	}

	public void setresource(Resource _res) {
		this._res = _res;
	}
	
	@Override
	public String toString() {
		if(this.getResource() != null) {
			return this.getResource().toString();
		} else if(this.getCouple() != null) {
			return this.getCouple().getFirst().toString() + " " + this.getCouple().getSecond().toString();
		}
		return "";
	}

	@Override
	public int hashCode() {
		if(this.getResource() != null) {
			return this.getResource().toString().hashCode();
		} else if(this.getCouple() != null) {
			return this.getCouple().toString().hashCode();
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
//		logger.debug(this.toString() + " equals " + o.toString() + " " + (((RDFPatternElement) o).getResource() == this.getResource() && ((RDFPatternElement) o).getCouple() == this.getCouple()));
		if(o instanceof RDFPatternElement) {
			RDFPatternElement oelem = (RDFPatternElement) o;
			if(oelem.getResource() != null && this.getResource() != null) {
				return oelem.getResource().equals(this.getResource());
			} else if(oelem.getCouple() != null && this.getCouple() != null) {
				return oelem.getCouple().equals(this.getCouple());
			}
		}
		return false;
	}
	
}
