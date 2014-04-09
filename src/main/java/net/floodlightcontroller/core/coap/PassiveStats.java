package net.floodlightcontroller.core.coap;

public class PassiveStats {
	public PassiveStats(int bytes_per_packet, float avg_rate, float avg_rssi, int packet_cnt, int packet_retries,
			String rate_string, int channel) {
		this.bytes_per_packet = bytes_per_packet;
		this.avg_rate = avg_rate;
		this.avg_rssi = avg_rssi;
		this.packet_cnt = packet_cnt;
		this.packet_retries = packet_retries;
		this.rate_string = rate_string;
		this.channel = channel;
	}
	
	public int bytes_per_packet;
	public float avg_rate;
	public float avg_rssi;
	public int packet_cnt;
	public int packet_retries;
	public String rate_string;
	public int channel;
}
