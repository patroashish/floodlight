package net.floodlightcontroller.core.coap;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.annotations.LogMessageDoc;

import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFStringStatisticsReply;
import org.openflow.protocol.statistics.OFStringStatisticsRequest;
import org.openflow.protocol.statistics.OFUtilStatisticsReply;
import org.openflow.protocol.statistics.OFUtilStatisticsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Return switch statistics information for all switches
 * @author readams
 */
public class CoapEngine implements Runnable {
	
	// Constants.
	
	// Logger.
    protected static Logger log = 
        LoggerFactory.getLogger(CoapEngine.class);
    
    private static HashMap<Integer, Long> apIdToDpid = new HashMap<Integer, Long>();
    
    // Initialize parsers for processing different types of statistics collected 
    // from the routers.
    static Parser station_stats_parser = new StationStatsParser();
	static Parser metric_parser = new MetricParser();
	static Parser util_parser = new UtilParser();
	static Parser beacon_parser = new BeaconStatsParser();
	static Parser higher_layer_parser = new HigherLayerParser();
	static Parser passive_parser = new PassiveStatsParser();
	//static Parser pie_parser = new PieStatsParser();
	static Parser util_hop_parser = new UtilHopParser();
	static Parser passive_hop_parser = new PassiveHopParser();
	
	static Parser nonwifi_parser = new NonWifiStatsParser();
	
    // Copied from learning switch.
    // Module dependencies
    protected IFloodlightProviderService floodlightProvider;
    /*
    protected ICounterStoreService counterStore;
    protected IRestApiService restApi;
    
    // Stores the learned state for each switch
    protected Map<IOFSwitch, Map<MacVlanPair,Short>> macVlanToSwitchPortMap;
    */
    /*
    
    // Ensure we receive the full packet via PacketIn
    OFSetConfig config = (OFSetConfig) factory
            .getMessage(OFType.SET_CONFIG);
    config.setMissSendLength((short) 0xffff)
    .setLengthU(OFSwitchConfig.MINIMUM_LENGTH);
    sw.write(config, null);
    sw.write(factory.getMessage(OFType.GET_CONFIG_REQUEST),
            null);
            
    @Get("json")
    public Map<String, Object> retrieve() {    
        String statType = (String) getRequestAttributes().get("statType");
        return retrieveInternal(statType);
    }
    */
    
    public CoapEngine(IFloodlightProviderService floodlightProvider)
    {
    	this.floodlightProvider = floodlightProvider;
    }
    
    public static long GetDpIdFromAP(int apId)
    {
    	if (!apIdToDpid.containsKey(apId))
    	{
    		return -1;
    	}
    	
    	return apIdToDpid.get(apId);
    }
    
	@Override
	public void run() {
		while (true)
		{
			try
			{
				Thread.sleep(CoapConstants.DATA_POLL_FREQUENCY_MSEC);
				
				log.info("Sending data poll commands to APs");
				
			    Long[] switchDpids = floodlightProvider.getAllSwitchMap().keySet().toArray(new Long[0]);
			    List<StatsQueryThread> activeThreads = new ArrayList<StatsQueryThread>(switchDpids.length);
			    List<StatsQueryThread> pendingRemovalThreads = new ArrayList<StatsQueryThread>();
			    
			    StatsQueryThread t;
			    
			    apIdToDpid = new HashMap<Integer, Long>();
			    
			    for (Long l : switchDpids) {
			        	        
			        IOFSwitch sw = floodlightProvider.getAllSwitchMap().get(l);
			        int apId = CoapUtils.GetApIdFromRemoteIp(sw.getInetAddress().toString().toString());
			    
			        apIdToDpid.put(apId, l);
			        //log.info("Found " + sw.getChannel().getRemoteAddress() + " switchDpid: " + l);
		        	
		        	t = new StatsQueryThread(l, apId);
			        activeThreads.add(t);
			        t.start();
			    }
			    
			    // Join all the threads after the timeout. Set a hard timeout
			    // of 12 seconds for the threads to finish. If the thread has not
			    // finished the switch has not replied yet and therefore we won't 
			    // add the switch's stats to the reply.
			    for (int iSleepCycles = 0; iSleepCycles < 12; iSleepCycles++) {
			        for (StatsQueryThread curThread : activeThreads) {
			            if (curThread.getState() == State.TERMINATED) {
			            	List<OFStatistics> utilStats = curThread.getUtilStats();
			            	List<OFStatistics> stationStats = curThread.getStationStats();
		
			            	// TODO: Finalize the usage of AP_ID.
			            	int apId = (int) curThread.apId; //CoapUtils.AP_ID_MAP.get(curThread.switchId);
			            	
			            	for (int i = 0; i < utilStats.size(); i++)
			            	{
			            		//log.info("Got 1 " + ((OFUtilStatisticsReply) utilStats.get(i)).toString());
			            		
			            		// TODO: Update
			            		process_string(((OFUtilStatisticsReply) utilStats.get(i)).toString(), apId);
			            	}
			            	
			            	for (int i = 0; i < stationStats.size(); i++)
			            	{
			            		// TODO: All stats may not be polled all the time. Find the relevant stats to update. 
			            		
			            		// TODO: Update
			            		/*
			            		log.info("Got 2 " + ((OFStringStatisticsReply) stationStats.get(i)).getStationStatsString());
			            		log.info("Got 3 " + ((OFStringStatisticsReply) stationStats.get(i)).getPassiveStatsString());
			            		log.info("Got 4 " + ((OFStringStatisticsReply) stationStats.get(i)).getBeaconActivityString());
			            		
			            		// Debug related
			            		log.info("Got 5 " + ((OFStringStatisticsReply) stationStats.get(i)).getPassiveHopStatsString());
			            		log.info("Got 6 " + ((OFStringStatisticsReply) stationStats.get(i)).getHigherlayerStatsString());
			            		log.info("Got 7 " + ((OFStringStatisticsReply) stationStats.get(i)).getUtilhopStatsString());
			            		*/
			            		
			            		// TODO: Update
			            		process_string(((OFStringStatisticsReply) stationStats.get(i)).getStationStatsString().trim(), apId);
			            		process_string(((OFStringStatisticsReply) stationStats.get(i)).getPassiveStatsString().trim(), apId);
			            		process_string(((OFStringStatisticsReply) stationStats.get(i)).getBeaconActivityString().trim(), apId);
			            		process_string(((OFStringStatisticsReply) stationStats.get(i)).getNonwifiStatsString().trim(), apId);
			            		
			            		// Debug related
			            		process_string(((OFStringStatisticsReply) stationStats.get(i)).getPassiveHopStatsString().trim(), apId);
			            		process_string(((OFStringStatisticsReply) stationStats.get(i)).getHigherlayerStatsString().trim(), apId);
			            		process_string(((OFStringStatisticsReply) stationStats.get(i)).getUtilhopStatsString().trim(), apId);
			            	}
			            	
			            	pendingRemovalThreads.add(curThread);
			            }
			        }
			        
			        // remove the threads that have completed the queries to the switches
			        for (StatsQueryThread curThread : pendingRemovalThreads) {
			            activeThreads.remove(curThread);
			        }
			        // clear the list so we don't try to double remove them
			        pendingRemovalThreads.clear();
			        
			        // if we are done finish early so we don't always get the worst case
			        if (activeThreads.isEmpty()) {
			            break;
			        }
			        
			        // sleep for 1 s here
			        try {
			            Thread.sleep(1000);
			        } catch (InterruptedException e) {
			            log.error("Interrupted while waiting for statistics", e);
			        }
			    }
			} catch (Exception e)
			{
				log.error("Error in CoapEngine main thread: " + e);
				e.printStackTrace();
			}
		}
    }
	
	 @LogMessageDoc(level="ERROR",
             message="Failure retrieving statistics from switch {switch}",
             explanation="An error occurred while retrieving statistics" +
             		"from the switch",
             recommendation=LogMessageDoc.CHECK_SWITCH + " " +
             		LogMessageDoc.GENERIC_ACTION)
	protected List<OFStatistics> getSwitchStatistics(long switchId, 
	                                               OFStatisticsType statType) {
	  
	  IOFSwitch sw = floodlightProvider.getAllSwitchMap().get(switchId);
	  
	  Future<List<OFStatistics>> future;
	  List<OFStatistics> values = null;
	  if (sw != null) {
	      OFStatisticsRequest req = new OFStatisticsRequest();
	      req.setStatisticType(statType);
	      
	      int requestLength = req.getLengthU();
	      
	      if (statType == OFStatisticsType.UTIL) {
	          OFUtilStatisticsRequest specificReq = new OFUtilStatisticsRequest();
	          specificReq.setType((short) 0); // TODO: 0123 Dummy value - not using the field for now.
	          req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
	          requestLength += specificReq.getLength();
	          //log.info("Pulling util statistics...");
	      } else if (statType == OFStatisticsType.STATION) {
	          OFStringStatisticsRequest specificReq = new OFStringStatisticsRequest();
	          specificReq.setType((short) 0); // TODO: 0123 Dummy value - not using the field for now.
	          req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
	          requestLength += specificReq.getLength();
	          //log.info("Pulling station statistics...");
	      }
	      
	      req.setLengthU(requestLength);
	      try {
	          future = sw.queryStatistics(req);
	          values = future.get(10, TimeUnit.SECONDS);
	      } catch (Exception e) {
	          log.error("Failure retrieving statistics from switch " + sw, e);
	      }
	  }
	  return values;
	}
	
	public static int process_string(String curr, int ap_id) {
		//log.info("curr_process_string = " + curr);
		
		/*if (ap_id <= 0) {
			System.err.println("ap_id unknown");
			return;
		}*/
		
		// TODO: Find out the AP_ID;
		try {
			if (!curr.contains("beacon")) {
				//System.out.println("curr_process_string = " + curr);
			}
			
			String[] terms = curr.split(";");
			
			if (terms.length == 1)	{
				return -1;
			}
			
			String rest = "";
			for (int i = 1; i < terms.length - 1; ++i) {
				rest += terms[i];
				rest += ";";
			}
			
			rest += terms[terms.length - 1];
	
			// log.info("stats for ap " + ap_id + " type: " + terms[0] + " rest:" + rest);
			
      if (terms[0].equals(CoapConstants.STATION_MSG)) {
			  	log.info("stats for ap " + ap_id + " type: " + terms[0] + " rest:" + rest);
				
			  	station_stats_parser.process(rest, ap_id);
			} else if (terms[0].equals(CoapConstants.UTIL_MSG)) {
				log.info("stats for ap " + ap_id + " type: " + terms[0] + " rest:" + rest);
				
				util_parser.process(rest, ap_id);
			} else if (terms[0].equals(CoapConstants.METRIC_MSG)) {
				metric_parser.process(rest, ap_id);
			} else if (terms[0].equals(CoapConstants.BEACON_MSG)) {
				beacon_parser.process(rest, ap_id);
			} else if (terms[0].equals(CoapConstants.PASSIVE_MSG)) {
				passive_parser.process(rest, ap_id);
			} else if (terms[0].equals(CoapConstants.HIGHERLAYER_MSG)) {
				higher_layer_parser.process(rest, ap_id);
			}
			// 0123: Commented out.
			/*else if (terms[0].equals(Constants.PIE_MSG)) {
				pie_parser.process(rest, ap_id);
			} */
			else if (terms[0].equals(CoapConstants.UTILHOP_MSG)) {
				util_hop_parser.process(rest, ap_id);
			} else if (terms[0].equals(CoapConstants.AIRSHARK_MSG)) {
				nonwifi_parser.process(rest, ap_id);
			} else if (terms[0].equals(CoapConstants.MAC_MSG)) {
				
				// TODO: Not used.
				
			} else if (terms[0].equals("passivehop")) {
				passive_hop_parser.process(rest, ap_id);
			}
			
		} catch (Exception ex) {
			log.error("process_string: " + ex.getMessage());
			
			ex.printStackTrace();
		}
		return 0;
	}
	
	protected class StatsQueryThread extends Thread {
        private List<OFStatistics> utilStat;
		private List<OFStatistics> stationStat;
        
		private long switchId;
        private int apId;
        
        public StatsQueryThread(long switchId, int apId) {
            this.switchId = switchId;
            this.apId = apId;
        }
        
        public List<OFStatistics> getUtilStats() {
			return utilStat;
		}

		public List<OFStatistics> getStationStats() {
			return stationStat;
		}
        
        @Override
		public void run() {
        	utilStat = getSwitchStatistics(switchId, OFStatisticsType.UTIL);
        	stationStat = getSwitchStatistics(switchId, OFStatisticsType.STATION);
        }
    }
}
