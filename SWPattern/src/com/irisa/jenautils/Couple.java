package com.irisa.jenautils;

public class Couple<A, B> {
	private A _first;
	private B _second;
	
	public Couple()
	{
		this._first = null;
		this._second = null;
	}
	
	public Couple(A first, B second)
	{
		this._first = first;
		this._second = second;
	}
	
	public A getFirst()
	{
		return _first;
	}
	
	public B getSecond()
	{
		return _second;
	}
	
	public void setFirst(A obj)
	{
		this._first = obj;
	}
	
	public void setSecond(B obj)
	{
		this._second = obj;
	}
	
	public int hashCode()
	{
		return this._first.toString().hashCode() + this._second.toString().hashCode();
	}
	
	/**
	 * Check by hashcode
	 */
	public boolean equals(Object o)
	{
		if(o.getClass() == this.getClass())
		{
			@SuppressWarnings("unchecked")
			Couple<A, B> c = (Couple<A, B>) o;
			return (this.hashCode() == c.hashCode());	
		}
		
		return false;
	}
	
	public String toString() 
	{
		return "( " + this._first + " ) ( " + this._second + ") ";
	}
}
