package net.floodlightcontroller.core.coap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.floodlightcontroller.core.coap.TrafficProfile.TrafficType;

public class ChannelLoadManager {

	class APLoadInfo implements Comparable<APLoadInfo>
	{
		int apId;
		int frequency;
		double channelLoad;
		TrafficType trafficType;

		public APLoadInfo(int apId, int frequency, double channelLoad, TrafficType trafficType)
		{
			this.apId = apId;
			this.frequency = frequency;
			this.channelLoad = channelLoad;
			this.trafficType = trafficType;
		}

		// Sorting in descending order by load.
		@Override
		public int compareTo(APLoadInfo o) {
			if (this.channelLoad > o.channelLoad) {
				return -1;
			} else if (this.channelLoad < o.channelLoad) {
				return 1;
			}

			return 0;
		}
	}

	ArrayList<APLoadInfo> channelLoadInfoList;
	PolicyEngine policyEngine; // Reference to the current policy engine.

	public ChannelLoadManager(PolicyEngine policyEngine)
	{
		this.policyEngine = policyEngine;

		this.channelLoadInfoList = new ArrayList<APLoadInfo>();

		/*
		for (Integer freqVal: CoapConstants.FREQ_LIST)
		{
			channelLoadInfoList.put(freqVal, new  ArrayList<Pair<Integer, Double>>());
		}

		apTrafficTypeInfo = new HashMap<Integer, TrafficType>();
		 */
	}

	public void AddChannelLoadAP(int apFreq, int apId, double estimatedLoad, TrafficType currTrafficType)
	{
    int videoLoadFactor = 1;

    // Play with the priority of video flows.
    if (currTrafficType == TrafficType.VIDEO) {
      estimatedLoad *= videoLoadFactor;
    }

		channelLoadInfoList.add(new APLoadInfo(apId, apFreq, estimatedLoad, currTrafficType)); 
		//apTrafficTypeInfo.put(apId, currTrafficType);
	}

	public void PrintOverallLoadStatistics()
	{
		HashMap<Integer, Double> perChannelLoad = new HashMap<Integer, Double>();
		for (Integer freqVal: CoapConstants.FREQ_LIST) {
			perChannelLoad.put(freqVal, 0.0);
		}

		for (APLoadInfo apLoad: channelLoadInfoList) {
			perChannelLoad.put(apLoad.frequency, perChannelLoad.get(apLoad.frequency) + apLoad.channelLoad);
		}

		double totalLoadAcrossAPsBeforeConfig = 0.0;

		for (APLoadInfo apLoad: channelLoadInfoList) {
			totalLoadAcrossAPsBeforeConfig += perChannelLoad.get(apLoad.frequency);
		}

		System.out.println("MIT PrintOverallLoadStatistics: " + CoapUtils.CurrentTime() +  " channel load 2412: " +
				perChannelLoad.get(2412) + " channel load 2437: " +
				perChannelLoad.get(2437) + " channel load 2462: " +
				perChannelLoad.get(2462) + " totalChannelLoadAcrossAPs: " + totalLoadAcrossAPsBeforeConfig);
	}

	/**
	 * Reconfigure the APs to minimize the channel load.
	 */
	public void ReconfigureAPChannels()
	{
		HashMap<Integer, Double> newPerChannelLoad = new HashMap<Integer, Double>();
		HashMap<Integer, Integer> newPerChannelApCount = new HashMap<Integer, Integer>();
		
		HashMap<Integer, Double> newPerChannelVideoLoad = new HashMap<Integer, Double>();
    HashMap<Integer, Integer> newPerChannelVideoCount = new HashMap<Integer, Integer>();

		ArrayList<APLoadInfo> updatebleAPs = new ArrayList<ChannelLoadManager.APLoadInfo>();

		for (Integer freqVal: CoapConstants.FREQ_LIST) {
			newPerChannelLoad.put(freqVal, 0.0);
			newPerChannelApCount.put(freqVal, 0);

      newPerChannelVideoLoad.put(freqVal, 0.0);
      newPerChannelVideoCount.put(freqVal, 0);
		}

		// Sorts the APs in decreasing order of channel load. 
		Collections.sort(channelLoadInfoList);

		for (int i = 0; i < channelLoadInfoList.size(); i++) {
			APLoadInfo curLoadInfo = channelLoadInfoList.get(i);
			boolean canChangeChannel = policyEngine.CanChangeChannelWithGap(curLoadInfo.apId);

			if (!canChangeChannel) {
				System.out.println("MIT ReconfigureAPChannels: " + CoapUtils.CurrentTime() + " can't switch apid " + curLoadInfo.apId + 
						" channel " + curLoadInfo.frequency + " ... done recently. traffic type: " + curLoadInfo.trafficType);
			}

			// Policy: Can't switch channel for video traffic.
			if (channelLoadInfoList.get(i).trafficType == TrafficType.VIDEO ||
					channelLoadInfoList.get(i).trafficType == TrafficType.VOIP ||
					channelLoadInfoList.get(i).trafficType == TrafficType.BULK ||
					!canChangeChannel) {

				// Update the channel load and AP count based on this information.
				newPerChannelLoad.put(curLoadInfo.frequency, 
						newPerChannelLoad.get(curLoadInfo.frequency) + curLoadInfo.channelLoad);
				newPerChannelApCount.put(curLoadInfo.frequency,
						newPerChannelApCount.get(curLoadInfo.frequency) + 1);
				
        if (channelLoadInfoList.get(i).trafficType == TrafficType.VIDEO) {
          newPerChannelVideoLoad.put(curLoadInfo.frequency, 
	  					newPerChannelVideoLoad.get(curLoadInfo.frequency) + curLoadInfo.channelLoad);
		  		newPerChannelVideoCount.put(curLoadInfo.frequency,
			  			newPerChannelVideoCount.get(curLoadInfo.frequency) + 1);
        }
			} else {
				updatebleAPs.add(curLoadInfo);
			}
		}

		System.out.println("MIT ReconfigureAPChannels: " + CoapUtils.CurrentTime()  + " assignable APs: " +
				updatebleAPs.size() + " static APs: " + (channelLoadInfoList.size() - updatebleAPs.size()));

		// Assign the remaining APs to the best available channel based on decreasing channel load.
		for (int i = 0; i < updatebleAPs.size(); i ++) {
			//policyEngine.PolicySwitchAPChannel(int apId, int setChannel);
			APLoadInfo curLoadInfo = updatebleAPs.get(i);

			// Preventing un-neccessary channel switches. Setting the load to the current channel's value.
			int nextFreq = curLoadInfo.frequency;
			double minPerchannelLoad = (newPerChannelLoad.get(nextFreq) + curLoadInfo.channelLoad) * (newPerChannelApCount.get(nextFreq) + 1); 

			for (Integer freqVal: CoapConstants.FREQ_LIST) {
				double tmpLoad = (newPerChannelLoad.get(freqVal) + curLoadInfo.channelLoad) * (newPerChannelApCount.get(freqVal) + 1);

				if (minPerchannelLoad > tmpLoad)
				{
					nextFreq = freqVal;
					minPerchannelLoad = tmpLoad;
				}
			}

			if (nextFreq != curLoadInfo.frequency) {
				System.out.println("MIT ReconfigureAPChannels: " + CoapUtils.CurrentTime()  + " updatebleAP " + i + 
						" moving apId: " + curLoadInfo.apId + " from currFreq " + curLoadInfo.frequency + " to nextFreq " + nextFreq );

				boolean isSuccess = policyEngine.PolicySwitchAPChannel(curLoadInfo.apId, 
						CoapUtils.GetChannelFromFreq(nextFreq), "g"); //, "g", "y");

				if (!isSuccess)	{
					System.out.println("MIT ReconfigureAPChannels: " + CoapUtils.CurrentTime()  + " Warning!!!! " + 
							" Could not switch channel for AP " + curLoadInfo.apId);

					// Resetting to old frequency.
					nextFreq = curLoadInfo.frequency;
				}
			}

			// Update the channel load and AP count based on this information.
			newPerChannelLoad.put(nextFreq, newPerChannelLoad.get(nextFreq) + curLoadInfo.channelLoad);
			newPerChannelApCount.put(nextFreq, newPerChannelApCount.get(nextFreq) + 1);
		}
	}
}
