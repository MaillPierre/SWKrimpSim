package com.irisa.swpatterns.data;

import java.util.HashSet;
import com.irisa.jenautils.Couple;

/**
 * Version of SMPF Itemset where numbers are replaced by the RDFPatternComponent
 * @author pmaillot
 *
 */
public class LabeledItemSet extends Couple<HashSet<RDFPatternComponent>, Integer> {

	public LabeledItemSet() {
		super( new HashSet<RDFPatternComponent>(), 0);
	}
	
	public HashSet<RDFPatternComponent> getItems() {
		return this.getFirst();
	}
	
	public void addItem(RDFPatternComponent rdfPatternComponent) {
		this.getFirst().add(rdfPatternComponent);
	}
	
	public int getCount() {
		return this.getSecond();
	}
	
	public void setCount(int count) {
		this.setSecond(count);
	}
	
	@Override
	public boolean equals(Object isO) {
		if(isO instanceof LabeledItemSet) {
			LabeledItemSet isOth = (LabeledItemSet)isO;
			return this.getItems().equals(isOth.getItems());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.getItems().hashCode()+this.getCount();
	}
}
