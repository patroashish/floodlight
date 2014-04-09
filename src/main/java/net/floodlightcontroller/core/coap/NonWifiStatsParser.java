package net.floodlightcontroller.core.coap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
//import java.util.logging.Level;

/**
 * Example: 
 * Fixed: 1379038109 2013-09-12 21:08:29 110 2462 0 26 55 1379038096 1379038108 12 -73.5;
 * FreqHop: 1379038677 2013-09-12 21:17:57 106 2400 107 28 55 1379038671 1379038677 6 -66.4927
 * 
 * @author a
 *
 *
class NonWiFiDeviceId
{
	public Long startTs;
	public DeviceType type;
	public Integer subbandFreq;
	
	public NonWiFiDeviceId(long startTs, DeviceType type,
			int subbandFreq)
	{
		this.startTs = startTs;
		this.type = type;
		this.subbandFreq = subbandFreq;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return startTs.hashCode() * 10000 + type.hashCode() + subbandFreq.hashCode();
	}
	
	
}
*/

class NonWiFiDevice
{
	public long timeStamp;
	public long startTs;
	public long endTs;
	
	public long duration;
	public double rssi;
	public int startFreq, centerFreq, endFreq;
	//public byte startFreq, centerFreq, endFreq;
	public DeviceType type;
	
	public int subbandFreq;
	public boolean isInactive;
	
	public String Getkey()
	{
		return startTs + " " + type + " " + subbandFreq;
	}
	
	public NonWiFiDevice(long timeStamp, long startTs, long endTs, long duration, double rssi,
			int startFreq, int centerFreq, int endFreq,
			//byte startFreq, byte centerFreq, byte endFreq,
			DeviceType type, int subbandFreq)
	{
		this.timeStamp = timeStamp;
		
		this.startTs = startTs;
		this.endTs = endTs;
		this.duration = duration;
		
		this.rssi = rssi;
		
		this.startFreq = startFreq;
		this.centerFreq = centerFreq;
		this.endFreq = endFreq;
		
		this.type = type;
		this.subbandFreq = subbandFreq;
		
		this.isInactive = false;
	}
}

public class NonWifiStatsParser implements Parser {
	// Constants.
	public static int MAXIMUM_INACTIVE_DURATION_SEC = 30;
	
	@Override
	public void process(String rest, int ap_id) {
		if (ap_id <= 0) {
			return;
		}
		String[] terms = rest.split(";");
		//System.out.println(rest);
		
		// Fixed: 1379038109 2013-09-12 21:08:29 110 2462 0 26 55 1379038096 1379038108 12 -73.5;
		for (int i = 1; i < terms.length; i++) {
			//System.out.println(i + " " + terms.length);
			String[] contents = terms[i].split(" ");
			
			System.out.println("NonWifiStatsParser " + ap_id + " " + CoapUtils.CurrentTime() + " " + i + " " + terms[i]);
			
			// Don't process spurious lines			
			if (contents.length < 12) 
			{
				continue;
			}
			
			long timeStamp = Long.parseLong(contents[1]);
			NonWiFiDevice curDevice = new NonWiFiDevice(timeStamp,
					Long.parseLong(contents[9]),
					Long.parseLong(contents[10]),
					Long.parseLong(contents[11]),
					Double.parseDouble(contents[12]),
					Byte.parseByte(contents[6]), Byte.parseByte(contents[7]), Byte.parseByte(contents[8]),
					DeviceType.get(Integer.parseInt(contents[4])),
					Integer.parseInt(contents[5]));
			
			updateMap(ap_id, timeStamp, null, curDevice);
		}
	}

	@Override
	public void updateMap(int ap_id, Long sec, String client_mac, Object o) {
		//lock.lock();
		//synchronized (beacon_map) {
		
		String currId = ((NonWiFiDevice) o).Getkey();
		
		if (!nonwifi_device_map.containsKey(ap_id)) {
			nonwifi_device_map.put(ap_id, new HashMap<String, NonWiFiDevice>());
		}

		if (nonwifi_device_map.get(ap_id).containsKey(currId)) {
			//System.out.println("Removing : " + currId.toString());
			nonwifi_device_map.get(ap_id).remove(currId);
		}
		
		nonwifi_device_map.get(ap_id).put(currId, (NonWiFiDevice) o);
		
		//System.out.println("Num nonwifi " + currId.hashCode() + " " + nonwifi_device_map.get(ap_id).size());
		mx_ts = Math.max(mx_ts, sec);
		//lock.unlock();
	}
	
	@Override
	public synchronized Object getHashMap() {
		synchronized (nonwifi_device_map) {
			return nonwifi_device_map;
		}
	}
	
	public HashMap<Integer, HashMap<String, NonWiFiDevice > > nonwifi_device_map = 
			new HashMap<Integer, HashMap<String, NonWiFiDevice> >();
	public Lock lock = new ReentrantLock();
	/*
	CREATE TABLE `wahdata`.`airshark_stats_table` (
			  `ap_id` TINYINT  NOT NULL,
			  `timestamp` int(11)  NOT NULL,
			  `nonwifi_dev_id` TINYINT NOT NULL,
			  `subband_freq` int(11)  NOT NULL,
			  `start_bin` int(11) NOT NULL,
			  `peak_bin` TINYINT  NOT NULL,
			  `end_bin` TINYINT  NOT NULL,
			  `start_timestamp` int(11) NOT NULL,
			  `end_timestamp` int(11)  NOT NULL,
			  `duration` int(11)  NOT NULL,
			  `rssi` float  NOT NULL,
			  PRIMARY KEY (`ap_id`, `timestamp`, `nonwifi_dev_id`, `subband_freq`, `start_bin`)
			) ENGINE=InnoDB DEFAULT CHARSET=utf8;
	*/
	@Override
	public void commit(long ts_limit) { // Time limit doesn't matter in this case.
		String queryFormat = "Insert into " + CoapConstants.AIRSHARK_STATS_TABLE +
				" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		//ArrayList<String> queries = new ArrayList<String>();
		//System.out.println("commiting airshark stats");
		
    //lock.lock();
		//synchronized (beacon_map) {
		@SuppressWarnings("unchecked")
		HashMap<Integer, HashMap<String, NonWiFiDevice > > airshark_stats_map = 
			(HashMap<Integer, HashMap<String, NonWiFiDevice > >)getHashMap();
		
		@SuppressWarnings("rawtypes")
		Iterator it = airshark_stats_map.entrySet().iterator();
		ArrayList<ArrayList<Object>> params = new ArrayList<ArrayList<Object>>();
		while (it.hasNext()) {
			//int ret = 0;
			boolean dontRemove = false;
			/*if (queries.size() > Constants.MAX_QUERIES) {
				break;
			}*/
			
			@SuppressWarnings("rawtypes")
			Map.Entry pairs = (Map.Entry)it.next();
			Integer ap_id = (Integer)pairs.getKey();
			@SuppressWarnings("unchecked")
			HashMap<String, NonWiFiDevice > dev_hashmap = 
				(HashMap<String, NonWiFiDevice >)pairs.getValue();
			
			@SuppressWarnings("rawtypes")
			Iterator dev_it = dev_hashmap.entrySet().iterator();
			while (dev_it.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry tim_pairs = (Map.Entry)dev_it.next();
				String devId = (String)tim_pairs.getKey();
				NonWiFiDevice stat = (NonWiFiDevice) tim_pairs.getValue();
				
				// 0223: Added to insert all entries.
				if ((CoapConstants.USE_DEBUG && (((System.currentTimeMillis() / 1000) - stat.endTs) < 10)) ||
					(!CoapConstants.USE_DEBUG && ((System.currentTimeMillis() / 1000) - stat.endTs) 
							> MAXIMUM_INACTIVE_DURATION_SEC)) {
					
					ArrayList<Object> objArray = new ArrayList<Object>();
					objArray.add(ap_id);
					objArray.add(stat.timeStamp);
					objArray.add(stat.type.getValue());
					objArray.add(stat.subbandFreq);
					objArray.add(stat.startFreq);
					objArray.add(stat.centerFreq);
					objArray.add(stat.endFreq);
					objArray.add(stat.startTs);
					objArray.add(stat.endTs);
					objArray.add(stat.duration);
					objArray.add(stat.rssi);
					
					//System.out.println("Adding: " + devId.startTs + " " + devId.type + " " + devId.subbandFreq);
					//System.out.println("Adding: " + devId + " - " + stat.centerFreq);
					
					params.add(objArray);
					dev_it.remove();
				}
			}
			System.out.println("Num nonwifi after clear: " + nonwifi_device_map.get(ap_id).size());

			/*
			if (!dontRemove) {
				it.remove(); // avoids a ConcurrentModificationException
			}
			*/
		}
		
		//DatabaseCommitter.ExecuteQuery(queries);
		DatabaseCommitter.ExecuteQuery(queryFormat, params);
		//System.out.println("sz = " + queries.size());
		//beacon_map.clear();
		
		//lock.unlock();
		//System.out.println("done commiting airshark stats");
	}

	long mx_ts = 0;
	@Override
	public long get_maxts() {
		return mx_ts;
	}

	@Override
	public Object readHashMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int ClearPolicyMap(long tsLimit) {
		// TODO Auto-generated method stub
		return 0;
	}
}
