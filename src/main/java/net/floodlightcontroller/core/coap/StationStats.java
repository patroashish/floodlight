package net.floodlightcontroller.core.coap;

import java.util.HashMap;

public class StationStats {
	public double UNKNOWN_RATE = -100.0;
	public static final HashMap<String, Double> MCS_RATE_MAP = 
			new HashMap<String, Double>() {/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

			{ put("MCS0", 6.5); put("MCS1", 13.00); 
			put("MCS2", 19.50); put("MCS3", 26.00); put("MCS4", 39.00);
			put("MCS5", 52.00); put("MCS6", 58.50); put("MCS7", 65.00); 
			put("MCS8", 13.00); put("MCS9", 26.00); put("MCS10", 39.00); 
			put("MCS11", 52.00); put("MCS12", 78.00); put("MCS13", 104.0); 
			put("MCS14", 117.0); put("MCS15", 130.0);}};
	
	public long ts;
	public int packet_cnt;
	public int packet_retries;
	public String rate_string;
	
	public Pair<Double, Double> GetAverageRateRetry()
	{
		String[] contents = rate_string.split("#");
		double totalPackets = 0, retriedPackets = 0, totalRate = 0;

		for (int i = 0; i < contents.length; i++)
		{
		      String[] temp_contents = contents[i].split("@");
		      totalPackets += Double.parseDouble(temp_contents[2]);
		      retriedPackets += (Double.parseDouble(temp_contents[2]) - Double.parseDouble(temp_contents[1]));

		      
		      // TODO: Not using goodput - Data rate for successful packets.
		      // totalRate += (Double.parseDouble(temp_contents[0]) * Double.parseDouble(temp_contents[1])); // Data rate for successful packets.
		      // Data rate for all packets.
		      totalRate += (Double.parseDouble(temp_contents[0]) * Double.parseDouble(temp_contents[2])); // Data rate for successful packets.
		}
		
		if (totalPackets > 0)
	    {
		    /*
		    goodput_rate = 0.0
		    if rate_packets > 0:
		      goodput_rate = total_rate * 1.0 / total_packets
		    }
		    */

			// TODO: Not using goodput.
			return new Pair<Double, Double>(retriedPackets / totalPackets,
					totalRate / totalPackets);
	    }
		
		return new Pair<Double, Double>(0.0, UNKNOWN_RATE);
	}
}