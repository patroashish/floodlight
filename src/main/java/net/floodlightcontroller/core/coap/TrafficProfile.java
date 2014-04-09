package net.floodlightcontroller.core.coap;

import java.util.HashMap;

import org.python.antlr.PythonParser.return_stmt_return;

public class TrafficProfile {
	
	/*
	########### Average speed: 14.98 Mbps
	bash $script_name Jellyfish-15-Mbps-amachine-osmf.pcap $output_file $server_ip $client_ip 61411:10001
	########### Average speed: 10.96 Mbps
	bash $script_name Jellyfish-10-Mbps-amachine-osmf.pcap $output_file $server_ip $client_ip 61371:10002
	########### Average speed: 5.42 Mbps
	bash $script_name Jellyfish-5-Mbps-amachine-osmf.pcap $output_file $server_ip $client_ip 61405:10003
	########### Average speed: 3.29 Mbps
	bash $script_name Jellyfish-3-Mbps-amachine-osmf.pcap $output_file $server_ip $client_ip 57641:10004
	########### Average speed: 7.18 Mbps
	bash $script_name Netflix_HD-internet_120sec_11n.pcap $output_file $server_ip $client_ip 57561:10005,57562:10006
	
	########### Average speed: 0.64 Mbps
	bash $script_name Pandora-internet_40sec_11n.pcap $output_file $server_ip $client_ip 53900:10021
	########## Average speed: < 0.64 Mbps
	bash $script_name Pandora-internet_60sec_11n.pcap $output_file $server_ip $client_ip 53900:10022
	########## Average speed: 13.88 Mbps
	bash $script_name Ubuntu-iso-amachine_2sec_11g.pcap $output_file $server_ip $client_ip 53590:10023
	########## Average speed: 13.23 Mbps
	bash $script_name Ubuntu-iso-amachine_5sec_11g.pcap $output_file $server_ip $client_ip 53590:10024
	########## Average speed: 13.65 Mbps
	bash $script_name Ubuntu-iso-amachine_10sec_11g.pcap $output_file $server_ip $client_ip 53590:10025
	
	########## Average speed: 11.94 Mbps
	bash $script_name Ubuntu-iso-amachine_60sec_11g.pcap $output_file $server_ip $client_ip 53590:10041
	########## Average speed: 17.81 Mbps
	bash $script_name Ubuntu-iso-amachine_60sec_11n.pcap $output_file $server_ip $client_ip 53582:10042
	########## Average speed: 9.11 Mbps
	bash $script_name Ubuntu-iso-internet_60sec_11g.pcap $output_file $server_ip $client_ip 53647:10043
	########## Average speed: 2.94 Mbps
	bash $script_name Ubuntu-iso-internet_60sec_slow_PHY24Mbps.pcap $output_file $server_ip $client_ip 53708:10044
	*/
	
	public enum TrafficType {BULK, VIDEO, BURSTY, VOIP, UNKNOWN};

	public static final HashMap<Integer, TrafficPattern> trafficContextInformation =
			new HashMap<Integer, TrafficPattern>()
			{{ 	put(10001, new TrafficPattern(14.98, 60.0));
				put(10002, new TrafficPattern(10.96, 60.0));
				put(10003, new TrafficPattern(5.42, 60.0));
				put(10004, new TrafficPattern(3.29, 60.0));
				put(10005, new TrafficPattern(7.18, 60.0));
				put(10006, new TrafficPattern(7.18, 60.0)); // TODO: Warning - do not sum with the previous one.
				
				put(10021, new TrafficPattern(0.64, 40.0));
				put(10022, new TrafficPattern(0.64, 60.0));
				put(10023, new TrafficPattern(13.88, 2.0));
				put(10024, new TrafficPattern(13.23, 5.0));
				put(10025, new TrafficPattern(13.65, 10.0));
				
				put(10041, new TrafficPattern(11.94, 60.0));
				put(10042, new TrafficPattern(17.81, 60.0));
				put(10043, new TrafficPattern(9.11, 60.0));
				put(10044, new TrafficPattern(2.94, 60.0));
				put(10045, new TrafficPattern(11.94 * 0.5 , 60.0 * 2.0));
				put(10046, new TrafficPattern(11.94 * 2, 60.0 * 0.5));}};
				
	public static TrafficType GetTrafficTypeFromPort(int dstPort)
	{
		if (dstPort >= 10000 && dstPort < 10020) {
			return TrafficType.VIDEO;
		} else if (dstPort >= 10020 && dstPort < 10040) {
			return TrafficType.BURSTY;
		} else if (dstPort >= 10040 && dstPort < 10060) {
			return TrafficType.BULK;
		} else if (dstPort >= 10060 && dstPort < 10080) {
			return TrafficType.VOIP;
		}
			
		return TrafficType.UNKNOWN;
	}
}
