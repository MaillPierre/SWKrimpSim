package com.irisa.swpatterns.util;

import java.util.HashSet;

public class TrioSet<T> {

	private HashSet<T> first = new HashSet<T>();
	private HashSet<T> second = new HashSet<T>();
	private HashSet<T> third = new HashSet<T>();
	
	public TrioSet() {
	}
	
	public void setFirstSet(HashSet<T> s) {
		first = s;
	}
	
	public void setSecondSet(HashSet<T> s) {
		second = s;
	}
	
	public void setThirdSet(HashSet<T> s) {
		third = s;
	}
	
	public HashSet<T> firstSet() {
		return first;
	}
	
	public HashSet<T> secondSet() {
		return second;
	}
	
	public HashSet<T> thirdSet() {
		return third;
	}
	
	public long size() {
		return first.size() + second.size() + third.size();
	}
}
