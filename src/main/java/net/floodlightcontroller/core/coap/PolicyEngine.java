package net.floodlightcontroller.core.coap;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.coap.TrafficProfile.TrafficType;

public class PolicyEngine implements Runnable {
	// Constants.
	//enum Event {HIGHUTIL, BETTER_CHANNEL_AVAILABLE, LOWMETRIC};

	// Logger.
	protected static Logger log = 
			LoggerFactory.getLogger(CoapEngine.class);

	// List of channels per AP.
	private HashMap<Integer, String> channelList;

	private long currTs, clearTs;
	private HashMap<Integer, ArrayList<UtilObject>> utilPolicyMap;
	private HashMap<Integer, ArrayList<UtilHopObject>> utilHopPolicyMap;
	private HashMap<Integer, ArrayList<StationStatsObject>> stationStatsPolicyMap;
	private HashMap<Integer, ArrayList<HigherLayerObject>> higherLayerPolicyMap;
	private HashMap<Integer, ArrayList<MetricStatsObject>> metricPolicyMap;

	private HashMap<Integer, DoubleEntry<Integer, Long>> chanMap = new HashMap<Integer, DoubleEntry<Integer, Long>>();

	// Used for context aware analysis
	//HashMap<String, Pair<Long, TrafficPatterns>> currMap = new HashMap<String, Pair<Long, TrafficPatterns>>();
	private HashMap<Integer, HashMap<Integer, TrafficPattern>> aptrafficContextMap = new HashMap<Integer, HashMap<Integer, TrafficPattern>>();	

	// Maintains a map of the link quality across all APs.
	private HashMap<Integer, HashMap<String, StationLoad>> linkQualityMap = new HashMap<Integer, HashMap<String, StationLoad>>();

	/*
	HashMap<Integer, HashMap<Long, HashMap<String, StationStats> > > station_stats_map = new HashMap<Integer, HashMap<Long,HashMap<String,StationStats>>>();
	HashMap<Integer, HashMap<Long, HashMap<String, Double> > > metric_map = new HashMap<Integer, HashMap<Long, HashMap<String,Double> > >();
	HashMap<Integer, HashMap<Long, Double> > util_map = new HashMap<Integer, HashMap<Long,Double>>();
	HashMap<Integer, HashMap<Long, HashMap<String, PassiveStats>>> passive_stats_map = new HashMap<Integer, HashMap<Long,HashMap<String,PassiveStats>>>();

	HashMap<Integer, HashMap<String, DoubleEntry<Double, Integer>>> conditional_probability_map = new HashMap<Integer, HashMap<String,DoubleEntry<Double,Integer>>>();
	HashMap<String, ArrayList<String>> neighboringAPs = new HashMap<String, ArrayList<String>>();

	 */

	//public static ReentrantLock lock = new ReentrantLock();
	//public static boolean toRead = false;

	// Send configuration commands to APs.

	private APConfigurer apConfigurer;

	@SuppressWarnings("unchecked")
	public PolicyEngine(IFloodlightProviderService floodlightProvider) {
		// Initialize the AP configurer.
		apConfigurer = new APConfigurer(floodlightProvider);

		clearTs = System.currentTimeMillis() / 1000;

		// Initialize channel list.
		channelList = new HashMap<Integer, String>();

		utilPolicyMap = (HashMap<Integer, ArrayList<UtilObject>>) CoapEngine.util_parser.readHashMap();
		utilHopPolicyMap = (HashMap<Integer, ArrayList<UtilHopObject>>) CoapEngine.util_hop_parser.readHashMap();
		stationStatsPolicyMap = (HashMap<Integer, ArrayList<StationStatsObject>>) CoapEngine.station_stats_parser.readHashMap();
		higherLayerPolicyMap = (HashMap<Integer, ArrayList<HigherLayerObject>>) CoapEngine.higher_layer_parser.readHashMap();
		metricPolicyMap = (HashMap<Integer, ArrayList<MetricStatsObject>>) CoapEngine.metric_parser.readHashMap();
	}

	@Override
	public void run() {

		while (true) {
			try {
				System.out.println("MIT: Going to sleep now.. " + System.currentTimeMillis() / 1000);
				Thread.sleep(CoapConstants.MITIGATION_SLEEP_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("MIT: executing PolicyEngine in-memory");
			long ts_sec = System.currentTimeMillis() / 1000;
			currTs = ts_sec;

			long diff = ts_sec - DatabaseCommitter.mx_ts;
			diff = Math.max(diff, 0);
			System.out.println("MIT: lag in data between AP and controller = " + diff);

			// TODO 0211: Removed the repeated reference update. Only printing the stats.

			System.out.println("MIT: utilPolicyMap size: " + utilPolicyMap.keySet().size());
			System.out.println("MIT: utilHopPolicyMap size: " + utilHopPolicyMap.keySet().size());
			System.out.println("MIT: stationStatsPolicyMap size: " + stationStatsPolicyMap.keySet().size());
			System.out.println("MIT: higherLayerPolicyMap size: " + higherLayerPolicyMap.keySet().size());
			System.out.println("MIT: metricPolicyMap size: " + metricPolicyMap.keySet().size());

			try {

				// Using Channel load proxy for channel configuration.
				// TODO: Using higher context to predict future load.
				//ChannelLoadAwareConfiguration();

				// Using non-WiFi aware channel configuration.
				NonwifiAwareChannelConfiguration();

				// TDMA configuration.
				TDMAPolicyConfiguration();

				// Using util hop information for channel configuration.
				// UtilHopAwareChannelConfiguration();

			} catch (Exception e) {
				e.printStackTrace();
			}

			int cleared = ClearMaps();
			System.out.println("MIT: cleared " + CoapUtils.CurrentTime() + " " + cleared +  " duration from map time");
		}
	}

	/** 
	 * Based on the context, estimates the channel load for 'N' seconds in the future.
	 * Performs the channel assignments for the future. 
	 * @return
	 */
	// TODO: 0211 Testing for the channel load code.
	// Simplifying and creating a single channel load map, since APs are in the vicinity of each other.
	/////////////////////////////////////////////////////
	private void ChannelLoadAwareConfiguration()
	{
		// TODO 0216 - Using a more simplified version.
		/*HashMap<Integer, HashMap<Integer, Double>> channelLoadMap = GetPerAPChannelLoadMap();

		for (Integer currApid: channelLoadMap.keySet()) {
			System.out.println("MIT channelLoadAnalysis: " + CoapUtils.CurrentTime() + " ap_id " + currApid + " channel load 2412: " +
					channelLoadMap.get(currApid).get(2412) + " channel load 2437: " +
					channelLoadMap.get(currApid).get(2437) + " channel load 2462: " +
					channelLoadMap.get(currApid).get(2462));
		}
		 */

		// Configure channel and print statistics before and after configuration.
		ChannelLoadManager channelLoadManager = GetPerAPChannelLoadMap();
		channelLoadManager.PrintOverallLoadStatistics();

		// TODO: Goals: Maintain the airtime fairness.
		channelLoadManager.ReconfigureAPChannels();
		System.out.println("MIT ChannelLoadAwareConfiguration: " + CoapUtils.CurrentTime() +  " reconfiguration complete");
		channelLoadManager.PrintOverallLoadStatistics();

	}

	private void UpdateLinkQualityMap()
	{
		linkQualityMap = new HashMap<Integer, HashMap<String, StationLoad>>();

		long fromTsSec = System.currentTimeMillis() / 1000 - CoapConstants.UTIL_AVERAGE_INTERVAL_SEC;

		// Get information about the client's link quality.
		for (Integer outerApid: utilPolicyMap.keySet()) {
			HashMap<String, StationLoad> currStationAtivity =
					GetStationRetryPHYRates(outerApid, fromTsSec);

			if (currStationAtivity != null)
			{
				linkQualityMap.put(outerApid, currStationAtivity);
			}
		}
	}

	// TODO: 0216 - Using a more simplified map.
	//private HashMap<Integer, HashMap<Integer, Double>> GetPerAPChannelLoadMap()
	private ChannelLoadManager GetPerAPChannelLoadMap()
	{
		// TODO: update
		//double TCP_LOAD_MBPS = 10.0;
		double FUTURE_PERIOD_ESTIMATE_SEC = 30.0;

		// TODO: Use this to update the per AP traffic load.
		UpdatePerAPTrafficLoadEstimate();

		// Update information about link quality.
		UpdateLinkQualityMap();

		// TODO: 0216 - Using a more simplified map instead of this one to have a single map for all channels.
		// HashMap<Integer, HashMap<Integer, Double>> channelLoadMap = 
		//		new HashMap<Integer, HashMap<Integer, Double>>();
		ChannelLoadManager channelLoadMap = new ChannelLoadManager(this);

		// Use per AP-activity information to channel load on each channel.
		for (Integer outerApid: utilPolicyMap.keySet()) {

			// Get the current Channel of the AP.
			int currentFreq = GetCurrentAPFreq(outerApid);

			if (currentFreq < 0)
			{
				System.out.println("MIT: GetPerAPChannelLoadMap" + CoapUtils.CurrentTime() + " outerApid " + outerApid + " has " +
						"unknown channel. Skipping");
				continue;
			}

			// Initializing the current channel map and debug channel list.
			if (!chanMap.containsKey(outerApid)) {
				chanMap.put(outerApid, new DoubleEntry<Integer, Long>(CoapUtils.GetChannelFromFreq(currentFreq), this.currTs));
				channelList.put(outerApid, " ");
			}		

			// TODO: Using traffic type to determine priority.
			TrafficType currTrafficType = TrafficType.UNKNOWN;

			// The AP should have valid client and traffic context information.
			if (aptrafficContextMap.containsKey(outerApid) && 
					linkQualityMap.containsKey(outerApid))
			{
				// Retry and avg. PHY Rate
				HashMap<String, StationLoad> currLinks = linkQualityMap.get(outerApid);
				HashMap<Integer, TrafficPattern> currContext = aptrafficContextMap.get(outerApid);

				double estimatedLoad = 0.0;

				for (Integer dstPort: currContext.keySet())
				{
					if (!currLinks.containsKey(currContext.get(dstPort).client))
					{
						System.out.println("MIT GetPerAPChannelLoadMap: Warning!!! outerApid" + outerApid + 
								" missing context client " + currContext.get(dstPort).client + " in stationActivityMap. Skippping client");
						continue;
					}

					// TODO: Only matters when clients have different link qualities
					StationLoad currLinkQuality = currLinks.get(currContext.get(dstPort).client);

					// Estimate the channel load based on the future predicted activity.
					//channelLoadMap.put(currentFreq, channelLoadMap.get(currentFreq) + 
					//		TCP_LOAD_MBPS * (1 + currLinkQuality.fst) / currLinkQuality.snd);


					// TODO: Finalize (Max. vs. Sum). Using single client per AP.
					// Load estimation method 1: Context aware traffic load estimation.
					double currentLoad = currContext.get(dstPort).getSessionSpeedMbps() * 
							Math.min(1.0, currContext.get(dstPort).getRemainingSessionDurationSec() /
									FUTURE_PERIOD_ESTIMATE_SEC);

					// Load estimation method 2: History based load estimation.
					//double currentLoad = currLinkQuality.totalPackets;

					estimatedLoad = Math.max(estimatedLoad, currentLoad);

					System.out.println("MIT: Predicted Load " +  CoapUtils.CurrentTime() +
							" outerApid " + outerApid + " dstPort: " + dstPort +
							" Link " + currContext.get(dstPort).client + " retry " +  currLinkQuality.retryRate + " rate " + currLinkQuality.avgPhyRate +
							" rem_duration " + currContext.get(dstPort).getRemainingSessionDurationSec() + 
							" speed " + currContext.get(dstPort).getSessionSpeedMbps() + 
							" currentload  " + currentLoad );

					if (currTrafficType != TrafficType.VIDEO)	{
						currTrafficType = TrafficProfile.GetTrafficTypeFromPort(dstPort);
					}
				}

				channelLoadMap.AddChannelLoadAP(currentFreq, outerApid, estimatedLoad, currTrafficType);
			} else {
				channelLoadMap.AddChannelLoadAP(currentFreq, outerApid, 0.0, currTrafficType);
			}

			// TODO: 0216 Removed: Using a simplified version using information about per-AP load. 
			// TODO: Only 1 client per AP in our experiments.
			/*
			channelLoadMap.put(outerApid, new HashMap<Integer, Double>());

			for (Integer freqVal: FREQ_LIST)
			{
				channelLoadMap.get(outerApid).put(freqVal, 0.0);
			}

			for (Integer innerApid: utilPolicyMap.keySet()) {

				// Get the current Channel of the AP.
				int currentFreq = GetCurrentAPFreq(innerApid);

				//HashMap<Integer, Double> currApLoad = channelLoadMap.get(outerApid);

				if (currentFreq < 0)
				{
					System.out.println("MIT: GetPerAPChannelLoadMap" + CoapUtils.CurrentTime() + " innerApid " + innerApid + " has " +
							"unknown channel. Skipping");
					continue;
				}

				if (stationActivityMap.containsKey(innerApid))
				{
					// Retry and avg. PHY Rate
					HashMap<String, Pair<Double, Double>> currNeighbor = stationActivityMap.get(innerApid);

					for (String neighborClient: currNeighbor.keySet())
					{
						 Pair<Double, Double> currLinkQuality = currNeighbor.get(neighborClient);

						 currApLoad.put(currentFreq, currApLoad.get(currentFreq) + 
								 TCP_LOAD_MBPS * (1 + currLinkQuality.fst) / currLinkQuality.snd);

						 System.out.println("MIT: GetPerAPChannelLoadMap" + CoapUtils.CurrentTime() + " innerApid " + innerApid + " has " +
									" neighborClient " + neighborClient + " retry " +  currLinkQuality.fst + " rate " + currLinkQuality.snd);
					}
				}
			}
			 */

		}

		return channelLoadMap;
	}

	// IMP: Using an estimate of the future traffic demand using the COAP stats.
	// In a real scenario, it should be much faster to do by looking at the domains based on DNS queries.
	private void UpdatePerAPTrafficLoadEstimate()
	{
		long fromTsSec = System.currentTimeMillis() / 1000 - CoapConstants.UTIL_AVERAGE_INTERVAL_SEC;

		for (Integer currApid: higherLayerPolicyMap.keySet()) {
			ArrayList<HigherLayerObject> higherLayerActivity = higherLayerPolicyMap.get(currApid);

			//TrafficPattern predPattern = null; // new TrafficPattern();

			// If not done, initialize the map for the current AP_ID.
			if (!aptrafficContextMap.containsKey(currApid))
			{
				aptrafficContextMap.put(currApid, new HashMap<Integer, TrafficPattern>());
			}

			for (int i = 0; i < higherLayerActivity.size(); i++)
			{
				HigherLayerObject currClient = higherLayerActivity.get(i);

				if (currClient.statsList.size() == 0) 
				{
					continue;
				}

				// Going backwards...
				for (int j = currClient.statsList.size() - 1; j >= 0; j--)
				{
					HigherLayerStats currStats = currClient.statsList.get(j);

					if (currStats.ts < fromTsSec) {
						break;
					}

					// TODO: Using this as a proxy for traffic context.
					//long srcIP = currStats.srcIP;
					int dstPort = currStats.dstPort;

					// TODO: Take something like max. of all values.
					//if (srcIP == 00L || dstPort > 0)
					if (TrafficProfile.trafficContextInformation.containsKey(dstPort))
					{
						if (!aptrafficContextMap.get(currApid).containsKey(dstPort))
						{
							aptrafficContextMap.get(currApid).put(dstPort, 
									new TrafficPattern(TrafficProfile.trafficContextInformation.get(dstPort)));

							aptrafficContextMap.get(currApid).get(dstPort).ts = currStats.ts;
							aptrafficContextMap.get(currApid).get(dstPort).trafficDstPort = dstPort;
							aptrafficContextMap.get(currApid).get(dstPort).client = currStats.client;
						} else {
							if (aptrafficContextMap.get(currApid).get(dstPort).ts + aptrafficContextMap.get(currApid).get(dstPort).getSessionDurationSec()
									< currStats.ts)
							{
								aptrafficContextMap.get(currApid).get(dstPort).ts = currStats.ts;
								aptrafficContextMap.get(currApid).get(dstPort).client = currStats.client;
							}
						}
					}
				}
			}

			// Clean up the map.
			synchronized(aptrafficContextMap) {
				Iterator<Entry<Integer, TrafficPattern>> contextIterator = 
						aptrafficContextMap.get(currApid).entrySet().iterator();

				long currTempTime = System.currentTimeMillis() / 1000;

				while (contextIterator.hasNext()) {
					Entry<Integer, TrafficPattern> contextEntry = contextIterator.next();

					TrafficPattern traffic = contextEntry.getValue();

					// Stale traffic entry that should be removed
					if (traffic.ts + traffic.getSessionDurationSec() < currTempTime) {

						System.out.println("MIT: Removing higherlayer for " + currApid + " port " + contextEntry.getKey() +
								" last seen sec ago: " + (currTempTime - traffic.ts));

						contextIterator.remove();
					}
				}

				//System.out.println("ClearMaps: " + tsLimit + " removed " + cnt + " util entries...");
			}
		}
	}

	// TODO: 0205 Added to enable non-WiFi based policy. Currently switching to fixed channel for demo.
	/////////////////////////////////////////////////////
	private void NonwifiAwareChannelConfiguration()
	{
		for (Integer currApid: utilPolicyMap.keySet()) {

			long fromTsSec = System.currentTimeMillis() / 1000 - CoapConstants.UTIL_AVERAGE_INTERVAL_SEC;
			int currFreq = GetCurrentAPFreq(currApid);

			// Initializing the current channel map and debug channel list.
			if (!chanMap.containsKey(currApid)) {
				chanMap.put(currApid, new DoubleEntry<Integer, Long>(CoapUtils.GetChannelFromFreq(currFreq), this.currTs));
				channelList.put(currApid, " ");
			}		

			boolean configureAp = IsAnalogPhoneInterference(currApid, currFreq, fromTsSec);

			if (configureAp)
			{
				int setChannel = 6;
				//boolean isSuccess =
				PolicySwitchAPChannel(currApid, setChannel, "g"); //, "g", "y");
			}
		}
	}

	////////////////////////////////////////////////////

	///////////
	/// 0223 - Without HT settings /////
	/*
	MIT: PolicySetTDMASlotsDemo, for VIDEO_AP_ID 1058 found station: 
	28:cf:e9:18:14:c1 5726.0 298.0 0.052043311212015365 54.0
	MIT: PolicySetTDMASlotsDemo, for INTF_AP_ID 1057 found station: 
	a8:54:b2:90:c6:38 13866.0 315.0 0.022717438338381652 54.0
	 */

	/// 0223 - With HT settings /////
	/*
	MIT: PolicySetTDMASlotsDemo, for VIDEO_AP_ID 1058 found station: 
	28:cf:e9:18:14:c1 17420.0 10372.0 0.5954075774971297 54.0
	MIT: PolicySetTDMASlotsDemo, for INTF_AP_ID 1057 found station: 
	a8:54:b2:90:c6:38 26881.0 11316.0 0.420966481901715 54.0
	 */

	/// 0223 - With HT but slotted transmission settings /////
	/*
	MIT: PolicySetTDMASlotsDemo, for VIDEO_AP_ID 1058 found station: 
	28:cf:e9:18:14:c1 9738.0 1451.0 0.1490039022386527 54.0
	MIT: PolicySetTDMASlotsDemo, for INTF_AP_ID 1057 found station: 
	a8:54:b2:90:c6:38 8468.0 1197.0 0.1413556920170052 54.0
	 */

	/// 0223 - UDP blast at 24 Mbps and JellyFish Video at 24 Mbps, no slotting /////
	/*
	MIT: PolicySetTDMASlotsDemo, for VIDEO_AP_ID 1058 found station:
	28:cf:e9:18:14:c1 14748.0 2064.0 0.13995117982099267 54.0
	MIT: PolicySetTDMASlotsDemo, for INTF_AP_ID 1057 found station: 
	a8:54:b2:90:c6:38 13006.0 1178.0 0.09057358142395817 24.0
	 */

	/// 0223 - UDP blast at 24 Mbps (throttled 6 20 0x07) and JellyFish Video at 24 Mbps, no slotting /////
	/*
	MIT: PolicySetTDMASlotsDemo, for VIDEO_AP_ID 1058 found station:
	28:cf:e9:18:14:c1 12256.0 389.0 0.03173955613577024 54.0
	MIT: PolicySetTDMASlotsDemo, for INTF_AP_ID 1057 found station: 
	a8:54:b2:90:c6:38 7493.0 516.0 0.06886427332176698 24.0
	 */
	
	// 0222: Added for demo purposes.
	// TODO: Fix the final APIDs
	int VIDEO_AP_ID = 1058;
	int INTF_AP_ID = 1057;
	
	public static long currActivityStartTs = 0;
	public static boolean isIntfThrottled = false;

	public static int ACTIVE_WAIT_FOR_THROTTLE_POLICY_SEC = 15;
	//public static int INACTIVE_WAIT_FOR_THROTTLE_CLEAR_SEC = 5;
	public static int THROTTLE_POLICY_SLOT_MS = 20;
	public static String THROTTLE_POLICY_TX_BITMAP = "0x07", THROTTLE_VALUE = "62.5";

	public static int INTF_ACTIVE_PKT_COUNT_THRESH = 300;
	
	public static String THROTTLE_STATUS_FILE = "/var/www/ons_demo/graphs/throttle_status.txt";

	public boolean TDMAPolicyConfiguration()
	{
		if (!utilPolicyMap.containsKey(VIDEO_AP_ID) || !utilHopPolicyMap.containsKey(INTF_AP_ID)) {
			System.out.println("MIT: PolicySetTDMASlotsDemo - One of the AP_IDs is not active. Doing nothing\n");
			
			PolicyClearThrottle();
			return false;
		}
		
		int currVideoFreq = GetCurrentAPFreq(VIDEO_AP_ID);
		int currIntfFreq = GetCurrentAPFreq(INTF_AP_ID);
		
		if (currVideoFreq < 0 || (currIntfFreq != currVideoFreq)) {
			System.out.println("MIT: PolicySetTDMASlotsDemo - APs not on same channel - " +
			" currVideoFreq " + currVideoFreq + " currIntfFreq " + currIntfFreq + " Doing nothing...\n");
			
			PolicyClearThrottle();
			return false;
		}

		long fromTsSec = System.currentTimeMillis() / 1000 - CoapConstants.UTIL_AVERAGE_INTERVAL_SEC;

		HashMap<String, StationLoad> currVideoQuality = GetStationRetryPHYRates(VIDEO_AP_ID, fromTsSec);
		HashMap<String, StationLoad> currIntfQuality = GetStationRetryPHYRates(INTF_AP_ID, fromTsSec);

		StationLoad linkLoad = null;
		StationLoad intfLoad = null;
		
		System.out.println("MIT: PolicySetTDMASlotsDemo " +
				" isLinkNull " + (currVideoQuality == null) + " isIntfNull " + (currIntfQuality == null));
		
		if (currVideoQuality != null && currIntfQuality != null)
		{
			for (String station: currVideoQuality.keySet())
			{
				// TODO: Put threshold on the minimum number of packets.
				linkLoad = currVideoQuality.get(station);
				System.out.println("MIT: PolicySetTDMASlotsDemo, for VIDEO_AP_ID " + VIDEO_AP_ID +
						" found station: " + station + " " + linkLoad.totalPackets + " " +
						linkLoad.totalRetries + " " + linkLoad.retryRate + " " + linkLoad.avgPhyRate);
			}

			for (String station: currIntfQuality.keySet())
			{
				// TODO: Put threshold on the minimum number of packets.
				// TODO: Check for success after pushing commands.
				intfLoad = currIntfQuality.get(station);
				System.out.println("MIT: PolicySetTDMASlotsDemo, for INTF_AP_ID " + INTF_AP_ID +
						" found station: " + station + " " + intfLoad.totalPackets + " " +
						intfLoad.totalRetries + " " + intfLoad.retryRate + " " + intfLoad.avgPhyRate);
			}
		}
		
		if (linkLoad != null && intfLoad != null) {
			if (intfLoad.totalPackets > INTF_ACTIVE_PKT_COUNT_THRESH) {
	
				if (currActivityStartTs <= 0) {
					currActivityStartTs = System.currentTimeMillis() / 1000;
				} else {
					long activeDurationSec = (System.currentTimeMillis() / 1000) - currActivityStartTs;
	
					if ((activeDurationSec > ACTIVE_WAIT_FOR_THROTTLE_POLICY_SEC) &&
							!isIntfThrottled) {
					
						System.out.println("MIT: PolicySetTDMASlotsDemo, throttling interferer, " +
								" activeDurationSec: " + activeDurationSec);
						
						apConfigurer.SetTDMAThrottle(INTF_AP_ID, 
								THROTTLE_POLICY_SLOT_MS, 
								THROTTLE_POLICY_TX_BITMAP);
						
						isIntfThrottled = true;
						
						// No throttle in this scenario.
						try {
							PrintWriter writer = new PrintWriter(THROTTLE_STATUS_FILE, "UTF-8");
							writer.print(THROTTLE_VALUE);
							writer.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if (!isIntfThrottled){
						System.out.println("MIT: PolicySetTDMASlotsDemo, waiting to throttle interferer, " +
								" activeDurationSec: " + activeDurationSec);
					}
				}		
			}
		} else {
			PolicyClearThrottle();
			
			System.out.println("MIT: PolicySetTDMASlotsDemo, not doing anything " +
					" linkLoad " + (linkLoad == null) + " intfLoad " + (intfLoad == null)); 
		}


		return true;
	}
	
	private void PolicyClearThrottle()
	{
		if (isIntfThrottled) {
			apConfigurer.ClearTDMA(INTF_AP_ID);
			
			System.out.println("MIT: PolicySetTDMASlotsDemo, cleared interferer throttle...");
			
			isIntfThrottled = false;
		}
		
		currActivityStartTs = 0;
		
		// No throttle in this scenario.
		try {
			PrintWriter writer = new PrintWriter(THROTTLE_STATUS_FILE, "UTF-8");
			writer.print("0.0");
			writer.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean PolicySwitchAPChannel(int apId, int setChannel, String mode)
	{
		boolean isSuccess = apConfigurer.SwitchChannel(apId, setChannel, mode, 
				CoapConstants.IS_CHANNEL_MODE_11N);
		DoubleEntry<Integer, Long> chEntry = chanMap.get(apId);

		//boolean isSuccess = true;
		if (isSuccess) {
			System.out.println("MIT: nonwifi mitigation ap_id " + apId + " cmd send successful");

			chEntry.setV(setChannel);
			chEntry.setN(currTs);
		} else {
			System.out.println("MIT: nonwifi mitigation ap_id " + apId + " cmd send unsuccessful");
		}

		return isSuccess;
	}

	private void UtilHopAwareChannelConfiguration()
	{
		for (Integer currApid: utilPolicyMap.keySet()) {

			long fromTsSec = System.currentTimeMillis() / 1000 - CoapConstants.UTIL_AVERAGE_INTERVAL_SEC;

			// Don't do anything if we don't have any utilization information.
			Pair<Double, Double> apUtilPair = GetCurrentAPUtil(currApid, fromTsSec);
			int currFreq = GetCurrentAPFreq(currApid);

			if (apUtilPair == null || currFreq < 0)
			{
				System.out.println("MIT: " + CoapUtils.CurrentTime() + " ap_id " + currApid + " has no util entries, doing nothing...");
				continue;
			}

			// Initializing the current channel map and debug channel list.
			if (!chanMap.containsKey(currApid)) {
				chanMap.put(currApid, new DoubleEntry<Integer, Long>(CoapUtils.GetChannelFromFreq(currFreq), this.currTs));

				channelList.put(currApid, " ");
			}

			// Only apply policy if the connection to the AP is still available.
			// 0211 : Not used
			/*
			if (CoapEngine.GetDpIdFromAP(currApid) < 0)
			{
				System.out.println("MIT: ap_id " + currApid + " connection to AP not avaiable... Doing nothing...");
				continue;
			}
			 */

			//DoubleEntry<Integer, Long> chEntry = chanMap.get(currApid);

			// TODO: add or condition for noise floor.
			//if ((CoapUtils.GetFrequencyFromChannel(chEntry.getV()) != minFreq) && this.CanChangeChannel(currApid)) {
			// Above one doesn't handle channel override.
			double averageUtilNoXmit = apUtilPair.fst, averageXmit = apUtilPair.snd;
			Pair<Integer, Double> minUtilHopInfo = 
					GetMinFreqUtilHop(currApid, currFreq, fromTsSec, averageUtilNoXmit, averageXmit);
			double currChannelHopUtil = GetCurrChannelUtilFromHop(currApid, currFreq, fromTsSec, averageUtilNoXmit, averageXmit);

			if (minUtilHopInfo == null)
			{
				System.out.println("MIT: minUtilHopInfo is null for ap_id: " + currApid + "Doing nothing...");
				continue;
			}

			int minFreq = minUtilHopInfo.fst;
			double minUtil = minUtilHopInfo.snd;

			if (minFreq != currFreq) {
				System.out.println("MIT: Better channel " + minFreq + " available for ap_id " + currApid +
						" Possible gain: " + ((currChannelHopUtil - minUtil) * 100.0 / minUtil));
			}

			channelList.put(currApid, channelList.get(currApid) + " " + CoapUtils.GetChannelFromFreq(minFreq));

			System.out.println("MIT: ap_id " + currApid + " Channel List: " +
					channelList.get(currApid).substring(Math.max(0, channelList.get(currApid).length() - 100)));

			if ((currFreq != minFreq) && this.CanChangeChannel(currApid)) {

				//if (currFinalHopAvgUtil.get(CoapUtils.GetFrequencyFromChannel(chEntry.getV()))
				// Same above.
				if (currChannelHopUtil 	< minUtil + CoapConstants.UTIL_CHANGE_THRESHOLD) {
					System.out.println("MIT: ap_id " + currApid + " util not changing above threshold " + CoapConstants.UTIL_CHANGE_THRESHOLD);
					continue;
				}

				System.out.println("MIT: ap_id " + currApid + " " + CoapUtils.CurrentTime() + " Frequency Val: " + 
						//CoapUtils.GetFrequencyFromChannel(chEntry.getV()) + " currFinalHopAvgUtil curr Freq: " +
						currFreq + " currFinalHopAvgUtil curr Freq: " +
						//currFinalHopAvgUtil.get(currFreq) + " min Freq: " + currFinalHopAvgUtil.get(minFreq));
						currChannelHopUtil + " min Freq: " + minUtil);

				// TODO: 0204 Using 2.4 static type for now. Not using the 5 GHz channel. 
				boolean isSuccess = PolicySwitchAPChannel(currApid, CoapUtils.GetChannelFromFreq(minFreq), "g"); //, "g", "y");
			}
		}
	}

	private double GetCurrChannelUtilFromHop(int currApid, int currFreq, long fromTsSec, double averageUtilNoXmit, double averageXmit)
	{
		HashMap<Integer, MutablePair<Integer, Double>> currSumHopAvgUtil = new HashMap<Integer, MutablePair<Integer,Double>>();

		if (!utilHopPolicyMap.containsKey(currApid)) {
			System.out.println("MIT: Warning! " + CoapUtils.CurrentTime() + " ap_id " + currApid + " not present in utilHop..");
			return averageUtilNoXmit;
		}

		ArrayList<UtilHopObject> utilHopInfo = utilHopPolicyMap.get(currApid);

		// TODO: Assumption is that everything is sorted by time. 
		for (int i = utilHopInfo.size() - 1; i >= 0; i--) {
			if (utilHopInfo.get(i).ts < fromTsSec) {
				break;
			}

			UtilHopStats currHopStats = utilHopInfo.get(i).stats;

			if (currHopStats.active_dur < CoapConstants.MIN_HOP_ACTIVE_DURATION_MS) {
				continue;
			}

			if (!currSumHopAvgUtil.containsKey(currHopStats.frequency)) {
				//System.out.println("MIT:Current ap_id a1 ab1 " + currApid + " i: " + i +  "...");

				currSumHopAvgUtil.put(currHopStats.frequency, new MutablePair<Integer, Double>
				(1, currHopStats.busy_dur * 1.0 / currHopStats.active_dur));

			} else {

				currSumHopAvgUtil.get(currHopStats.frequency).fst += 1;
				currSumHopAvgUtil.get(currHopStats.frequency).snd += (currHopStats.busy_dur * 1.0 / currHopStats.active_dur);
			}
		}

		// Not enough information available to proceed.
		if (!currSumHopAvgUtil.containsKey(currFreq)) {
			System.out.println("MIT: Hop utilization information missing for curr_freq: " + currFreq +
					" for ap_id: " + currApid + "Returning null...");
			return averageUtilNoXmit;
		}

		return (currSumHopAvgUtil.get(currFreq).snd / currSumHopAvgUtil.get(currFreq).fst) - averageXmit;
	}



	private HashMap<String, StationLoad> GetStationRetryPHYRates(int currApid, long fromTsSec)
	{
		HashMap<String, StationLoad> currMap = new HashMap<String, StationLoad>();

		if (!stationStatsPolicyMap.containsKey(currApid))
		{
			return null;
		}

		ArrayList<StationStatsObject> currStationStats = stationStatsPolicyMap.get(currApid);
		System.out.println("MIT: GetStationRetryPHYRates" + CoapUtils.CurrentTime() + " ap_id " + currApid + " has " +
				currStationStats.size() + " clients...");

		if (currStationStats.size() == 0)
		{
			return null;
		}

		for (int i = 0; i < currStationStats.size(); i++) {

			StationStatsObject currClient = currStationStats.get(i);

			if (currClient.statsList.size() == 0) 
			{
				continue;
			}

			double totalPackets = 0.0, totalRetries = 0.0;
			double avgPhyRate = 0.0;
			int countEntries = 0;

			// Going backwards...
			for (int j = currClient.statsList.size() - 1; j >= 0; j--)
			{
				StationStats currStats = currClient.statsList.get(j);

				if (currStats.ts < fromTsSec) {
					break;
				}

				double currAvgPhyRate = currStats.GetAverageRateRetry().snd;

				if (currAvgPhyRate > 0.0)
				{
					totalPackets += currStats.packet_cnt;
					totalRetries += currStats.packet_retries;

					avgPhyRate += currAvgPhyRate;
					countEntries ++;

					System.out.println("MIT: GetStationRetryPHYRates" + CoapUtils.CurrentTime() + " ap_id " + currApid + " client " +
							currClient.client + " " + totalPackets + " " + totalRetries + " " + avgPhyRate + " " + countEntries);
				}
			}

			if (countEntries > 0.0)
			{
				currMap.put(currClient.client, new StationLoad(totalPackets, totalRetries, totalRetries / totalPackets,
						avgPhyRate / countEntries));
			}
		}

		return currMap;		
	}

	private Pair<Integer, Double> GetMinFreqUtilHop(int currApid, int currFreq, long fromTsSec, double averageUtilNoXmit, double averageXmit)
	{
		HashMap<Integer, MutablePair<Integer, Double>> currSumHopAvgUtil = new HashMap<Integer, MutablePair<Integer,Double>>();
		HashMap<Integer, Double> currFinalHopAvgUtil = new HashMap<Integer, Double>();

		if (!utilHopPolicyMap.containsKey(currApid)) {
			System.out.println("MIT: Warning! " + CoapUtils.CurrentTime() + " ap_id " + currApid + " not present in utilHop..");
			return null;
		}

		ArrayList<UtilHopObject> utilHopInfo = utilHopPolicyMap.get(currApid);

		// TODO: Assumption is that everything is sorted by time. 
		for (int i = utilHopInfo.size() - 1; i >= 0; i--) {
			if (utilHopInfo.get(i).ts < fromTsSec) {
				break;
			}

			UtilHopStats currHopStats = utilHopInfo.get(i).stats;

			if (currHopStats.active_dur < CoapConstants.MIN_HOP_ACTIVE_DURATION_MS) {
				continue;
			}

			if (!currSumHopAvgUtil.containsKey(currHopStats.frequency)) {
				//System.out.println("MIT:Current ap_id a1 ab1 " + currApid + " i: " + i +  "...");

				currSumHopAvgUtil.put(currHopStats.frequency, new MutablePair<Integer, Double>
				(1, currHopStats.busy_dur * 1.0 / currHopStats.active_dur));

			} else {

				currSumHopAvgUtil.get(currHopStats.frequency).fst += 1;
				currSumHopAvgUtil.get(currHopStats.frequency).snd += (currHopStats.busy_dur * 1.0 / currHopStats.active_dur);
			}
		}

		// Not enough information available to proceed.
		if (!currSumHopAvgUtil.containsKey(currFreq)) {
			System.out.println("MIT: Hop utilization information missing for curr_freq: " + currFreq +
					" for ap_id: " + currApid + "Returning null...");
			return null;
		}

		for (Integer hopFreq: CoapConstants.FREQ_LIST) {
			if (currSumHopAvgUtil.containsKey(hopFreq)) {
				currFinalHopAvgUtil.put(hopFreq, 
						(currSumHopAvgUtil.get(hopFreq).snd / currSumHopAvgUtil.get(hopFreq).fst) - 
						(hopFreq == currFreq ? averageXmit : 0.0));

				if (hopFreq == currFreq) {
					// 0131: Some the hopper can report lower util. Eg. when there is nonwifi continously on.
					if (averageUtilNoXmit > currFinalHopAvgUtil.get(hopFreq))
					{
						System.out.println("currApid " + currApid + " averageUtil " + averageUtilNoXmit +
								" more than currSumHopAvgUtil.get(hopFreq).snd " + 
								currFinalHopAvgUtil.get(hopFreq) + " !!!\n");

						currFinalHopAvgUtil.put(hopFreq, averageUtilNoXmit);
					}
				}
			} else {
				currFinalHopAvgUtil.put(hopFreq, CoapConstants.DEFAULT_UTIL);
			}
		}

		int minFreq = CoapConstants.DEFAULT_FREQ;
		double minUtil = CoapConstants.DEFAULT_UTIL;

		for (Integer hopFreq: currFinalHopAvgUtil.keySet()) {
			if (currFinalHopAvgUtil.get(hopFreq) < minUtil) {
				minFreq = hopFreq;
				minUtil = currFinalHopAvgUtil.get(hopFreq);
			}
		}

		System.out.println("MIT: For ap_id " + currApid + " curr_util: " + averageUtilNoXmit +
				" curr_xmit: " + averageXmit +
				" Hop Utils: " + currFinalHopAvgUtil.get(2412) + " " +
				currFinalHopAvgUtil.get(2437) + " " +
				//currFinalHopAvgUtil.get(2462) + " currfreq: " + CoapUtils.GetFrequencyFromChannel(chEntry.getV()));
				currFinalHopAvgUtil.get(2462) + " currfreq: " + currFreq);

		return new Pair<Integer, Double>(minFreq, minUtil);
	}

	private Pair<Double, Double> GetCurrentAPUtil(int currApid, long fromTsSec)
	{
		UtilStats currUtilStats;
		int countUtils = 0;
		double averageUtil = 0.0, averageXmit = 0.0;

		ArrayList<UtilObject> utilInfo = utilPolicyMap.get(currApid);
		System.out.println("MIT: " + CoapUtils.CurrentTime() + " ap_id " + currApid + " has " + utilInfo.size() + " util entries...");

		if (utilInfo.size() == 0)
		{
			return null;
		}

		// Going backwards...
		for (int i = utilInfo.size() - 1; i >= 0; i--) {
			if (utilInfo.get(i).ts < fromTsSec) {

				break;
			}

			currUtilStats = utilInfo.get(i).stats;
			averageUtil += ((currUtilStats.busy_tim - currUtilStats.transmit_tim) * 1.0 / utilInfo.get(i).stats.active_tim);
			averageXmit += ((currUtilStats.transmit_tim) * 1.0 / utilInfo.get(i).stats.active_tim);

			countUtils ++;
		}

		if (countUtils < 1) {

			return null;
		}

		averageUtil /= countUtils;
		averageXmit /= countUtils;

		return new Pair<Double, Double> (averageUtil, averageXmit);
	}

	private int GetCurrentAPFreq(int currApid)
	{
		ArrayList<UtilObject> utilInfo = utilPolicyMap.get(currApid);

		if (utilInfo.size() > 0)
		{
			return utilInfo.get(utilInfo.size() - 1).stats.frequency;
		}

		return -1;
	}

	@SuppressWarnings("unchecked")
	private boolean IsAnalogPhoneInterference(int currApid, int currFreq, long fromTsSec)
	{
		HashMap<Integer, HashMap<String, NonWiFiDevice > > airshark_stats_map = 
				(HashMap<Integer, HashMap<String, NonWiFiDevice > >) CoapEngine.nonwifi_parser.getHashMap();

		System.out.println("MIT: checking non wifi for ap_id " + currApid);

		long currentTime = System.currentTimeMillis() / 1000;

		if (airshark_stats_map.containsKey(currApid))
		{
			Iterator<Map.Entry<String, NonWiFiDevice>> dev_it = 
					airshark_stats_map.get(currApid).entrySet().iterator();

			System.out.println("MIT: airshark_stats_map contains ap_id " + currApid);

			while (dev_it.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry tim_pairs = (Map.Entry) dev_it.next();

				String devId = (String)tim_pairs.getKey();
				NonWiFiDevice stat = (NonWiFiDevice) tim_pairs.getValue();

				// Seen in the last 2 minutes.
				if (currentTime - stat.endTs < 120)
				{
					System.out.println("MIT: for nonwifi for ap_id " + currApid + " curr_freq " + currFreq +
						" devId: " + devId + " duration: " + stat.duration +
						" centerFreq: " + stat.centerFreq +
						" consider_active? " + (stat.endTs - fromTsSec) + " seconds " +
						" lastSeen: " + (currentTime - stat.endTs) + " seconds ago...");
				}
				
				if (stat.type == DeviceType.PULSE_ANALOGPHONE 
						&& stat.subbandFreq == 2412
						&& ((stat.centerFreq > 5 && stat.centerFreq < 18) || (stat.centerFreq > 22 && stat.centerFreq < 25))
						&& stat.endTs > fromTsSec // TODO: finalizeAdded 0211
						&& stat.subbandFreq == currFreq)
				{
					System.out.println("MIT: detected analophone for ap_id " + currApid + " devId: " + devId);

					return true;
				}
			}
		}

		return false;
	}

	/**
	 * TODO: Assuming a single flow for client.
	 */
	private boolean CheckHigherLayerInfo(int ap_id, int inactive_time_sec, String client) {
		/**
		 * TODO: Currently assuming a fixed threshold. The commented code contains information for doing more sophisticated.
		 */

		return inactive_time_sec >= CoapConstants.INACTIVE_SEARCH_INTERVAL_SEC;

		/*
		ArrayList<HigherLayerObject> higherLayerObjectList = higherLayerPolicyMap.get(ap_id);
		int n = higherLayerObjectList.size();

	    if (n == 0) {
	      return true;
	    }

		while (n > 0) {
			n--;
			if (higherLayerObjectList.get(n).client.equals(client)) {
				break;
			}
		}

		HigherLayerObject obj = higherLayerObjectList.get(n);
		int m = obj.stats_list.size();
		if (m == 0) {
			return true;
		}

		String type = get_higher_layer_info(obj.stats_list.get(m - 1).type);
		if (type == null) {
			return false;
		}

		int inactive_threshold = get_inactive_higherlayer(ap_id, type);
		return inactive_time > inactive_threshold;
		 */
	}

	private boolean CanChangeChannel(int ap_id) {
		// TODO: Finalize switching the AP's channel, even if no station is connected. This is to make sure that 
		// the AP is on the least utilized channel.
		if (!stationStatsPolicyMap.containsKey(ap_id)) {
			System.out.println("MIT: CanChangeChannel ap_id " + ap_id + " - stationStats doesn't contain ap_id");
			return true;
			//return false;
		}

		ArrayList<StationStatsObject> stationStatsObjectList = stationStatsPolicyMap.get(ap_id);
		int numClients = stationStatsObjectList.size();
		DoubleEntry<Integer,Long> chEntry = chanMap.get(ap_id);

		if (currTs - chEntry.getN() < CoapConstants.MIN_CHANNEL_SWITCH_GAP_SEC) {
			System.out.println("MIT: CanChangeChannel ap_id " + ap_id + " - channel switch happened < " + 
					CoapConstants.MIN_CHANNEL_SWITCH_GAP_SEC + " seconds ago " + currTs + " " + chEntry.getN());
			return false;
		}

		//HashMap<Long, HashMap<String, StationStats> > it = stationStatsPolicyMap.entrySet().iterator();
		System.out.println("MIT: CanChangeChannel " + CoapUtils.CurrentTime() + " ap_id " + ap_id + " stationStatsObjectList client size: " + numClients);

		for (int i = 0; i < numClients; ++i) {
			int inactive_time = CoapConstants.DEFAULT_INACTIVE_DURATION_SEC; // Setting it to a large value to make a station inactive by default.

			StationStatsObject object = stationStatsObjectList.get(i);
			int numEntries = object.statsList.size();

			System.out.println("MIT: CanChangeChannel " + CoapUtils.CurrentTime() + " ap_id " + ap_id + " client " + object.client + " num entries " + numEntries);

			// Search within the most recent instances for activity.
			for (int j = numEntries - 1; j > Math.max(0, numEntries - 10) ; --j) {
				StationStats stats = object.statsList.get(j);
				long ts = object.statsList.get(j).ts;

				inactive_time = (int) (currTs - ts);

				// Currently, only use packet count as an indicator of inactivity.
				/*
				if (ts < currTs - CoapConstants.INACTIVE_SEARCH_INTERVAL_SEC) {
					System.out.println("MIT: CanChangeChannel ap_id " + ap_id + " Inactive time break: " + inactive_time);
					break;
				}
				 */

				if (stats.packet_cnt > CoapConstants.INACTIVE_PACKET_COUNT_THESHOLD) {
					System.out.println("MIT: CanChangeChannel " + CoapUtils.CurrentTime() + " " + 
							" ap_id " + ap_id + " Inactive time break 2nd cond: " + inactive_time + " " + currTs + " " + ts);
					break;
				}
			}

			if (inactive_time < CoapConstants.INACTIVE_SEARCH_INTERVAL_SEC) {
				System.out.println("MIT: CanChangeChannel " + CoapUtils.CurrentTime() + " ap_id " + ap_id + " At least one station active (inactive time: " +
						inactive_time + "). Returning false..: ");
				return false;
			}

			if (!CheckHigherLayerInfo(ap_id, inactive_time, object.client)) {
				System.out.println("MIT: CanChangeChannel " + CoapUtils.CurrentTime() + " ap_id " + ap_id + " CheckHigherLayerInfo returned false..: ");
				return false;
			}
		}

		return true;
	}

	public boolean CanChangeChannelWithGap(int ap_id) {
		// TODO: Finalize switching the AP's channel, even if no station is connected. This is to make sure that 
		// the AP is on the least utilized channel.

		DoubleEntry<Integer,Long> chEntry = chanMap.get(ap_id);

		if (currTs - chEntry.getN() < CoapConstants.MIN_CHANNEL_SWITCH_GAP_SEC) {
			System.out.println("MIT: CanChangeChannelWithGap ap_id " + ap_id + " - channel switch happened < " + 
					CoapConstants.MIN_CHANNEL_SWITCH_GAP_SEC + " seconds ago " + currTs + " " + chEntry.getN());
			return false;
		}

		// TODO: 0216: Moved below to prevent repeated application of channel updates. 
		if (!stationStatsPolicyMap.containsKey(ap_id)) {
			System.out.println("MIT: CanChangeChannelWithGap ap_id " + ap_id + " - stationStats doesn't contain ap_id");
			return true;
			//return false;
		}

		return true;
	}

	/*
	Double ComputeUtil(int ap_id) {
		Double util = 0.0;
		int n = 0;

		for (long ts = Math.max(currTs - CoapConstants.UTIL_AVERAGE_INTERVAL_SEC, chanMap.get(ap_id).getN()); ts <= currTs; ts++) {
			if (util_map.get(ap_id).containsKey(ts)) {
				util += util_map.get(ap_id).get(ts);
				n += 1;
			}
		}

		if (n < CoapConstants.MIN_CHECKUTILS_ENTRIES) {
			return 0.0;
		}

		return util / n;
	}
	 */

	/*
	HashMap<Integer, Double> inactiveProbMap = new HashMap<Integer, Double>();
	private int GetInactiveHigherlayer(int ap_id, String type) {
		if (type == null) {
			return -1;
		}

		try {
			int curr_time = 0;
			FileInputStream input = new FileInputStream("/home/ashish/wah_code/process_data/traffic_prediction_analysis/final_thresholds");
			DataInputStream in = new DataInputStream(input);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String str;
			Double prob = 0.0;
			while ((str = br.readLine()) != null) {
				String[] terms = str.split(" ");
				if (Integer.parseInt(terms[0]) != ap_id) {
					continue;
				}

				if (!terms[1].equals(type)) {
					continue;
				}

				Double curr_prob = Double.parseDouble(terms[6]);
				if (curr_prob == 0.0) {
					continue;
				}

				if (prob <= curr_prob) {
					prob = curr_prob;
					curr_time = Integer.parseInt(terms[2]) * 5;
					continue;
				}

				return curr_time;
			}
		} catch (FileNotFoundException e) {
			return 10000;
		} catch (IOException e) {
			return 10000;
		}

		return 10000;
	}

	private String GetHigherLayerInfo(String type) {
		String[] terms = type.split("_");

		if (terms.length < 2 || !terms[0].equals("TCP")) {
			return null;
		}

		try {
			FileInputStream input = new FileInputStream("/home/ashish/wah_code/sql_scripts/ip_resolved_may");
			DataInputStream in = new DataInputStream(input);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String str;
			while ((str = br.readLine()) != null) {
				String[] terms_higherlayer = str.split(" ");
				if (terms_higherlayer[0].equals(type)) {
					return terms_higherlayer[2];
				}
			}
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}

		return null;
	}
	 */


	// TODO: Clear the conditional probability map.	
	/*
	void CheckUtilCause() {
		for (int ap_id: util_map.keySet()) {
			for (long ts: util_map.get(ap_id).keySet()) {

				Double util = util_map.get(ap_id).get(ts);
				//Double util = calc_util(ap_id, ts);
				if (!passive_stats_map.containsKey(ap_id) || !passive_stats_map.get(ap_id).containsKey(ts)) {
					continue;
				}

				if (!conditional_probability_map.containsKey(ap_id)) {
					conditional_probability_map.put(ap_id, new HashMap<String, DoubleEntry<Double, Integer>>());
				}

				for(String link: passive_stats_map.get(ap_id).get(ts).keySet()) {
					DoubleEntry<Double, Integer> e;
					if (!conditional_probability_map.get(ap_id).containsKey(link)) {
						e = new DoubleEntry<Double, Integer>(0.0, 0);
					} else {
						e = conditional_probability_map.get(ap_id).get(link);
					}

					Double v = e.getV();
					int n = e.getN();
					v += util;
					n += 1;
					e.setN(n);
					e.setV(v);

					conditional_probability_map.get(ap_id).put(link, e);
				}
			}
		}

		for (int ap_id: util_map.keySet()) {
			Double util = ComputeUtil(ap_id);
			System.out.println("MIT: CheckUtilCause ap_id " + ap_id + " util = " + util);

			if (util > 0.6) {  // should be util > 0.6
				// "raise event"
				UtilEvent event = new UtilEvent();
				event.ap_id = ap_id;
				event.ts = currTs;
				event.util = util;
				System.out.println("MIT: CheckUtilCause ap_id " + ap_id + " 'util > 0.6' " + util);
				//raise_event(Event.UTIL, event);
			}
		}
	}

	void CheckMetricCause() {

	}


	int ChooseBestChannel(int ap_id, DoubleEntry<Integer, Long> curr_ch, long ts) {
		if (ts - curr_ch.getN() < CoapConstants.MIN_CHANNEL_SWITCH_GAP_SEC) {  // change back to 30
			return curr_ch.getV();  // placeholder, choose best channel based on hop statistics
		}

		// ideally use the last hop statistics
		return ((curr_ch.getV() / 5 + 1) % 3) * 5 + 1;
		//return 1;
	}

	void HandleUtil(Object o) {
		// handle high util event
		UtilEvent event = (UtilEvent)o;
		DoubleEntry<Integer, Long> curr_ch = chanMap.get(event.ap_id);
		int ch = ChooseBestChannel(event.ap_id, curr_ch, event.ts);
		if (ch == curr_ch.getV()) {
			return;
		}

		String cmd = ClientNotifier.makeChannelCmd(ch);
		ClientNotifier.sendCommand(event.ap_id, cmd);
		curr_ch.setN(currTs);
		curr_ch.setV(ch);
	}

	void HandleMetric(Object o) {
		// handle low metric event
		//MetricEvent event = (MetricEvent)o;

	}

	void RaiseEvent(Event e, Object o) {
		if (e == Event.BETTER_CHANNEL_AVAILABLE)
		{
			HandleUtil(o);
		} else if (e == Event.HIGHUTIL) {
			// send high util event to controller
			HandleUtil(o);
		} else if (e == Event.LOWMETRIC) {
			// send low metric event to controller
			HandleMetric(o);
		}
	}

	void CheckPolicy() {
		// find out pathologies, call ClientNotifier to change txpower or channel
		CheckUtilCause();
		CheckMetricCause();

		//ClearMaps();
	}
	 */

	private int ClearMaps() {  // sliding window implementation
		System.out.println("MIT: ClearMaps: " + CoapUtils.CurrentTime() + " clear_ts: " + clearTs + " currTs: " + currTs);

		if (currTs - clearTs < CoapConstants.INMEMORY_DATA_INTERVAL_SEC) {
			return 0;
		}

		System.out.println("MIT: ClearMaps: " + CoapUtils.CurrentTime() + "Clearing the in-memory data: clear_ts: " + clearTs + " currTs: " + currTs);

		clearTs = currTs;
		int cnt = 0;

		// Clear the util hop stats.
		cnt += CoapEngine.util_hop_parser.ClearPolicyMap(currTs);

		// Clear the util stats.
		cnt += CoapEngine.util_parser.ClearPolicyMap(currTs);

		// Clear the station stats.
		cnt += CoapEngine.station_stats_parser.ClearPolicyMap(currTs);

		// Clear the higher layer stats.
		cnt += CoapEngine.higher_layer_parser.ClearPolicyMap(currTs);

		// Clear the metric stats.
		cnt += CoapEngine.metric_parser.ClearPolicyMap(currTs);

		// TODO: Test.
		cnt += CoapEngine.passive_parser.ClearPolicyMap(currTs);

		// TODO: Test.
		cnt += CoapEngine.passive_hop_parser.ClearPolicyMap(currTs);

		// TODO: Test.
		cnt += CoapEngine.nonwifi_parser.ClearPolicyMap(currTs);

		//synchronized
		return cnt;
	}

	class StationLoad
	{
		double totalPackets = 0.0, totalRetries = 0.0, retryRate = 0.0, avgPhyRate = 0.0;

		public StationLoad(double totalPackets, double totalRetries, double retryRate, double avgPhyRate)
		{
			this.totalPackets = totalPackets;
			this.totalRetries = totalRetries;
			this.retryRate = retryRate;
			this.avgPhyRate = avgPhyRate;
		}
	}
}

