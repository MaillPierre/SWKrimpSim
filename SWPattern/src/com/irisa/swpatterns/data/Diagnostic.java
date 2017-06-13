package com.irisa.swpatterns.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

public class Diagnostic {
	
	private static Logger logger = Logger.getLogger(Diagnostic.class);

	private List<LabeledItemSet> _itemSet1;
	private List<LabeledItemSet> _itemSet2;
	
	private List<LabeledItemSet> _commons;
	private List<LabeledItemSet> _differents1;
	private List<LabeledItemSet> _differents2;
	private HashMap<LabeledItemSet, List<LabeledItemSet> > _inclusions1 ;
	private HashMap<LabeledItemSet, List<LabeledItemSet> > _inclusions2 ;
	
	
	public Diagnostic(List<LabeledItemSet> itemSet1, List<LabeledItemSet> itemSet2) {
		this._itemSet1 = itemSet1;
		this._itemSet2 = itemSet2;
		
		
		Comparator<LabeledItemSet> comp = new Comparator<LabeledItemSet>(){
			@Override
			public int compare(LabeledItemSet is1, LabeledItemSet is2) {
				if(is1 != null && is2 != null) { 
					try {
					if(is1.getItems().size() == is1.getItems().size()) {
						return Integer.compare(is1.getCount(), is2.getCount());
					}
					return Integer.compare(is1.getItems().size(), is2.getItems().size());
					} catch(NullPointerException e) {
						logger.fatal(e);
					}
				}
				return 0;
			}
		};
		Collections.sort(_itemSet1, comp);
		Collections.sort(_itemSet2, comp);
	}
	
	public void compareItemsets() {
		this._commons = new ArrayList<LabeledItemSet>(_itemSet1);
		this._commons.retainAll(_itemSet2);
		this._differents1 = new ArrayList<LabeledItemSet>(_itemSet1);
		this._differents1.removeAll(_itemSet2);
		this._differents2 = new ArrayList<LabeledItemSet>(_itemSet2);
		this._differents2.removeAll(_itemSet1);
		
		this._inclusions1 = new HashMap<LabeledItemSet, List<LabeledItemSet> >();
		this._itemSet1.forEach(new Consumer<LabeledItemSet>() {
			@Override
			public void accept(LabeledItemSet is1) {
				_itemSet2.forEach(new Consumer<LabeledItemSet>() {
					@Override
					public void accept(LabeledItemSet is2) {
						if( ! _commons.contains(is1) && ! _commons.contains(is2) && is1.getItems().size() > is2.getItems().size() &&is1.getItems().containsAll(is2.getItems())) {
							if(! _inclusions1.containsKey(is1)) {
								_inclusions1.put(is1, new ArrayList<LabeledItemSet>());
							}
							_inclusions1.get(is1).add(is2);
							_differents1.remove(is1);
							_differents2.remove(is2);
						}
					}
				});
			}
		});
		
		this._inclusions2 = new HashMap<LabeledItemSet, List<LabeledItemSet> >();
		this._itemSet2.forEach(new Consumer<LabeledItemSet>() {
			@Override
			public void accept(LabeledItemSet is2) {
				_itemSet1.forEach(new Consumer<LabeledItemSet>() {
					@Override
					public void accept(LabeledItemSet is1) {
						if( ! _commons.contains(is1) && ! _commons.contains(is2) && is2.getItems().size() > is1.getItems().size() &&is2.getItems().containsAll(is1.getItems())) {
							if(! _inclusions2.containsKey(is2)) {
								_inclusions2.put(is2, new ArrayList<LabeledItemSet>());
							}
							_inclusions2.get(is2).add(is1);
							_differents1.remove(is1);
							_differents2.remove(is2);
						}
					}
				});
			}
		});
	}
	
	public List<LabeledItemSet> getCommons() {
		return this._commons;
	}
	
	public List<LabeledItemSet> getDifference1() {
		return this._differents1;
	}
	
	public List<LabeledItemSet> getDifference2() {
		return this._differents2;
	}
	
	public HashMap<LabeledItemSet, List<LabeledItemSet> > getInclusionsIn1() {
		return this._inclusions1;
	}
	
	public HashMap<LabeledItemSet, List<LabeledItemSet> > getInclusionsIn2() {
		return this._inclusions2;
	}
	
//	public static LabeledItemSet intersectionOfLabeledItemSet(List<LabeledItemSet> is1, List<LabeledItemSet> is2) {
//		
//	}
	
}
