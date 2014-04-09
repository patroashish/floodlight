package net.floodlightcontroller.core.coap;

public class DoubleEntry<T,t> {
	public DoubleEntry(T v, t n) {
		this.val = v;
		this.num = n;
	}
	
	T val;
	t num;
	
	public void setV(T v) {
		this.val = v;
	}
	
	public void setN(t n) {
		this.num = n;
	}
	
	public T getV() {
		return this.val;
	}
	
	public t getN() {
		return this.num;
	}
}
