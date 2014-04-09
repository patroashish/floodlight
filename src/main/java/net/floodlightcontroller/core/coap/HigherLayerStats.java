package net.floodlightcontroller.core.coap;

public class HigherLayerStats {
	
	public long ts;
	public int packet_cnt, num_bytes, packet_retries;
	public String type; // netflix, youtube
	
	String client;
	
	long srcIP, dstIP;
	int srcPort, dstPort;
}
