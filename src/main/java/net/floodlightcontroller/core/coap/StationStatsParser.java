package net.floodlightcontroller.core.coap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StationStatsParser implements Parser {

	@Override
	public void process(String rest, int ap_id) {
		if (ap_id <= 0) {
			return;
		}
		
		int cnt = Integer.parseInt(rest.substring(0, 2).replace("^0", ""));
		if (cnt == 0) {
			return;
		}

		rest = rest.substring(3);
		String[] terms = rest.split(";");
		assert(cnt == terms.length);
		for (int i = 0; i < cnt; ++i) {
			String curr = terms[i];
			String client;
			StationStats stats = new StationStats();
			String[] curr_terms = curr.split(" ");
			client = curr_terms[0].toLowerCase();
			stats.packet_cnt = Integer.parseInt(curr_terms[1]);
			stats.packet_retries = Integer.parseInt(curr_terms[2]);
			stats.ts = Long.parseLong(curr_terms[3]);
			stats.rate_string = curr_terms[4];
			updateMap(ap_id, stats.ts, client, stats);
		}
	}

	@Override
	public void updateMap(int ap_id, Long sec, String client_mac, Object o) {
		//lock.lock();

		//synchronized (station_stats_map) {
		if (!station_stats_map.containsKey(ap_id)) {
			station_stats_map.put(ap_id, new HashMap<Long, HashMap<String, StationStats>>());
		}
		
		if (!station_stats_map.get(ap_id).containsKey(sec)) {
			station_stats_map.get(ap_id).put(sec, new HashMap<String, StationStats>());
		}

		station_stats_map.get(ap_id).get(sec).put(client_mac, (StationStats)o);
		mx_ts = Math.max(mx_ts, sec);
		
		/*
		 * synchronized (policyUtilHopMap) {
			UtilHopObject utilHopObj = new UtilHopObject();
			utilHopObj.stats = obj;
			utilHopObj.ts = sec;
			policyUtilHopMap.get(ap_id).add(utilHopObj);
		 */
		synchronized (policyStationStatsMap) {
			//ArrayList<StationStatsObject> stationStatsArray = policyStationStatsMap.get(ap_id);
			
			boolean found = false; 
			
			if (!policyStationStatsMap.containsKey(ap_id)) {
				policyStationStatsMap.put(ap_id, new ArrayList<StationStatsObject>());
			}

			for (int i = 0; i < policyStationStatsMap.get(ap_id).size(); i++)
			{
				if (client_mac.equals(policyStationStatsMap.get(ap_id).get(i).client))
				{
					policyStationStatsMap.get(ap_id).get(i).statsList.add((StationStats) o);
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				StationStatsObject obj = new StationStatsObject();
				obj.statsList = new ArrayList<StationStats>();
				obj.client = client_mac;
				obj.statsList.add((StationStats)o);
				policyStationStatsMap.get(ap_id).add(obj);
			}
			
				/*
			StationStatsObject obj = null;
			for (StationStatsObject obj2: stationStatsArray) {
				if (obj2.client.equals(client_mac)) {
					obj = obj2;
					break;
				}
			}

			if (obj == null) {
				obj = new StationStatsObject();
				obj.stats_list = new ArrayList<StationStats>();
				obj.ts_list = new ArrayList<Long>();
				obj.client = client_mac;
			}

			obj.stats_list.add((StationStats)o);
			obj.ts_list.add(sec);
			*/
		}
		//}
		
		//lock.unlock();
	}

	@Override
	public synchronized Object getHashMap() {
		synchronized (station_stats_map) {
			return station_stats_map;
		}
	}
	
	@Override
	public Object readHashMap() {
		return policyStationStatsMap;
	}
	
	HashMap<Integer, HashMap<Long, HashMap<String, StationStats> > > station_stats_map = new HashMap<Integer, HashMap<Long,HashMap<String,StationStats>>>();
	HashMap<Integer, ArrayList<StationStatsObject>> policyStationStatsMap = new HashMap<Integer, ArrayList<StationStatsObject>>();
	Lock lock = new ReentrantLock();
	
	@Override
	public void commit(long ts_limit) {
		//ArrayList<String> queries = new ArrayList<String>();
		String queryFormat = "insert into " + CoapConstants.STATION_TABLE + " VALUES(?, ?, ?, ?, ?, ?)";
		ArrayList<ArrayList<Object>> params = new ArrayList<ArrayList<Object>>();
		Iterator<Map.Entry<Integer, HashMap<Long, HashMap<String, StationStats> > > > it = station_stats_map.entrySet().iterator();
		while (it.hasNext()) {
			boolean dontRemove = false;
			/*if (queries.size() > Constants.MAX_QUERIES) {
				break;
			}*/
			
			Map.Entry<Integer, HashMap<Long, HashMap<String, StationStats> > > pair = it.next();
			int ap_id = pair.getKey();
			Iterator<Map.Entry<Long, HashMap<String, StationStats> > > tim_it = pair.getValue().entrySet().iterator();
			while (tim_it.hasNext()) {
				Map.Entry<Long, HashMap<String, StationStats>> pair_tim = tim_it.next();
				long ts = pair_tim.getKey();
				/*if (ts > ts_limit || queries.size() > 100) {
					dontRemove = true;
					if (queries.size() > 100) {
						break;
					}
					continue;
				}*/
				
				Iterator<Map.Entry<String, StationStats>> link_it = pair_tim.getValue().entrySet().iterator();
				while (link_it.hasNext()) {
					Map.Entry<String, StationStats> pair_link = link_it.next();
					String client = pair_link.getKey();
					StationStats stats = pair_link.getValue();					
					/*String query = String.format("insert into station_stats values (%d, %d, '%s', %d, %d, '%s')", 
							ap_id, ts, client, stats.packet_cnt, stats.packet_retries, stats.rate_string);
					queries.add(query);*/
					ArrayList<Object> objArray = new ArrayList<Object>();
					objArray.add(ap_id);
					objArray.add(ts);
					objArray.add(client);
					objArray.add(stats.packet_cnt);
					objArray.add(stats.packet_retries);
					objArray.add(stats.rate_string);
					params.add(objArray);
					link_it.remove();
				}
				
				tim_it.remove();
			}
			
			if (!dontRemove) {
				it.remove();
			}
		}
		
		//DatabaseCommitter.ExecuteQuery(queries);
		DatabaseCommitter.ExecuteQuery(queryFormat, params);
		//System.out.println("queries size = " + queries.size());
	}
	
	long mx_ts = 0;
	@Override
	public long get_maxts() {
		return mx_ts;
	}
	
	public void commit2() {
		System.out.println("commiting station stats");
		//lock.lock();
		//boolean dontRemove = false;
		ArrayList<String> queries = new ArrayList<String>();
		//synchronized (station_stats_map) {
		for (int ap_id: station_stats_map.keySet()) {
			boolean dontRemove_ap = false;
			for (long ts: station_stats_map.get(ap_id).keySet()) {
				boolean dontRemove_ts = false;
				for (String client: station_stats_map.get(ap_id).get(ts).keySet()) {
					StationStats stats = station_stats_map.get(ap_id).get(ts).get(client);
					String query = String.format("insert into " + CoapConstants.STATION_TABLE + " values (%d, %d, '%s', %d, %d, '%s')", 
							ap_id, ts, client, stats.packet_cnt, stats.packet_retries, stats.rate_string);
					/*if (DatabaseCommitter.ExecuteQuery(query) == 0) {
						//station_stats_map.get(ap_id).get(ts).remove(client);
					} else {
						dontRemove_ts = true;
						dontRemove_ap = true;
						dontRemove = true;
					}*/
					queries.add(query);
				}

				if (!dontRemove_ts) {
					//station_stats_map.get(ap_id).remove(ts);
				}
			}

			if (!dontRemove_ap) {
				//station_stats_map.remove(ap_id);
			}
		}

		//if (!dontRemove) {
		lock.lock();
		station_stats_map.clear();
		lock.unlock();
		//}
		//}
		if (queries.size() > 0) {
			lock.lock();
			DatabaseCommitter.ExecuteQuery(queries);
			lock.unlock();
		}
		
		System.out.println("done commiting station stats " + queries.size());
	}

	@Override
	public int ClearPolicyMap(long tsLimit) {
		int cnt = 0;
		
		synchronized(policyStationStatsMap) {
			Iterator<Map.Entry<Integer, ArrayList<StationStatsObject>>> stationIterator = policyStationStatsMap.entrySet().iterator();
			while (stationIterator.hasNext()) {
				Map.Entry<Integer, ArrayList<StationStatsObject>> stationEntry = stationIterator.next();
				ArrayList<StationStatsObject> stationArray = stationEntry.getValue();
				Iterator<StationStatsObject> arrayIter = stationArray.iterator();
				
				while (arrayIter.hasNext()) {
					StationStatsObject obj = arrayIter.next();
					Iterator<StationStats> stats_iter = obj.statsList.iterator();
					
					while (stats_iter.hasNext()) {
						Long ts = stats_iter.next().ts;
						if (ts > tsLimit - CoapConstants.INMEMORY_DATA_INTERVAL_SEC) {
							break;
						}
						
						stats_iter.remove();
						cnt ++;
					}
				}
				
				//utilIterator.remove();
			}
			
			System.out.println("ClearMaps: " + tsLimit + " removed " + cnt + " station entries...");
		}
		
		return cnt;
	}
}

class StationStatsObject {
	String client;
	ArrayList<StationStats> statsList;
}
