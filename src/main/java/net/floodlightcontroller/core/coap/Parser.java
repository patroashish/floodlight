package net.floodlightcontroller.core.coap;

public interface Parser {
	void process(String rest, int ap_id);
	void updateMap(int ap_id, Long sec, String client_mac, Object o);
	public Object getHashMap();
	public Object readHashMap();
	public void commit(long tsLimit);
	public long get_maxts();
	public int ClearPolicyMap(long tsLimit);
}
