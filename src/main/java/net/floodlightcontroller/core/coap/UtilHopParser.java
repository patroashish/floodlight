package net.floodlightcontroller.core.coap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class UtilHopParser implements Parser {

	@Override
	public void process(String rest, int ap_id) {
		int cnt = Integer.parseInt(rest.substring(0, 2).replace("^0", ""));
		if (cnt == 0) {
			return;
		}
		
		rest = rest.substring(3);
		String[] freq_terms = rest.split(";");
		for (int i = 0; i < cnt; ++i) {
			
			String[] terms = freq_terms[i].split(" ");
			
			//System.out.println("utilhop freq_terms " + i + " :" + freq_terms[i] + "@" +
			//		terms[6] + "XX");
					
			int frequency = Integer.parseInt(terms[0]);
			long active_dur = Long.parseLong(terms[1]);
			long busy_dur = Long.parseLong(terms[2]);
			long recv_dur = Long.parseLong(terms[3]);
			long transmit_dur = Long.parseLong(terms[4]);
			long sec = Long.parseLong(terms[5]);
			int noise_floor = Integer.parseInt(terms[6]);
			
			if (active_dur < 50) {
				continue;
			}

			UtilHopStats stats = new UtilHopStats();
			stats.active_dur = active_dur;
			stats.recv_dur = recv_dur;
			stats.busy_dur = busy_dur;
			stats.transmit_dur = transmit_dur;
			stats.frequency = frequency;
			stats.noise_floor = noise_floor;
			
			updateMap(ap_id, sec, null, stats);
		}
		
	}

	@Override
	public void updateMap(int ap_id, Long sec, String client_mac, Object o) {
		UtilHopStats obj = (UtilHopStats)o;
		//lock.lock();
		if (!util_hop_stats.containsKey(ap_id)) {
			util_hop_stats.put(ap_id, new HashMap<Long, HashMap<Integer, ArrayList<UtilHopStats>>>());
		}
		
		if (!util_hop_stats.get(ap_id).containsKey(sec)) {
			util_hop_stats.get(ap_id).put(sec, new HashMap<Integer, ArrayList<UtilHopStats>>());
		}
		
		if (!util_hop_stats.get(ap_id).get(sec).containsKey(obj.frequency)) {
			util_hop_stats.get(ap_id).get(sec).put(obj.frequency, new ArrayList<UtilHopStats>());
		}
		
		util_hop_stats.get(ap_id).get(sec).get(obj.frequency).add(obj);
		
		synchronized (policyUtilHopMap) {
			if (!policyUtilHopMap.containsKey(ap_id))
			{
				policyUtilHopMap.put(ap_id, new ArrayList<UtilHopObject>());
			}
			
			UtilHopObject utilHopObj = new UtilHopObject();
			utilHopObj.stats = obj;
			utilHopObj.ts = sec;
			policyUtilHopMap.get(ap_id).add(utilHopObj);
		}
		
		mx_ts = Math.max(mx_ts, sec);
		//lock.unlock();
	}

	@Override
	public Object getHashMap() {
		return util_hop_stats;
	}

	@Override
	public Object readHashMap() {
		return policyUtilHopMap;
	}
	
	@Override
	public void commit(long ts_limit) {
		String queryFormat = "INSERT INTO " + CoapConstants.UTIL_HOP_TABLE + " VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
		ArrayList<String> queries = new ArrayList<String>();
		ArrayList<ArrayList<Object>> objArrayVector = new ArrayList<ArrayList<Object>>();
		Iterator<Map.Entry<Integer, HashMap<Long, HashMap<Integer, ArrayList<UtilHopStats> > > > > it = util_hop_stats.entrySet().iterator();
		//lock.lock();
		while (it.hasNext()) {
			/*if (queries.size() > Constants.MAX_QUERIES) {
				break;
			}*/
			
			boolean dontRemove = false;
			Map.Entry<Integer, HashMap<Long, HashMap<Integer, ArrayList<UtilHopStats> > > > pair = it.next();
			int ap_id = pair.getKey();
			Iterator<Map.Entry<Long, HashMap<Integer, ArrayList<UtilHopStats> > > > tim_it = pair.getValue().entrySet().iterator();
			while (tim_it.hasNext()) {
				Map.Entry<Long, HashMap<Integer, ArrayList<UtilHopStats>>> pair_tim = tim_it.next();
				long ts = pair_tim.getKey();
				/*if (ts > ts_limit || queries.size() > Constants.MAX_QUERIES) {
					dontRemove = true;
					if (queries.size() > Constants.MAX_QUERIES) {
						break;
					}
					
					continue;
				}*/
				
				Iterator<Map.Entry<Integer, ArrayList<UtilHopStats>>> freq_it = pair_tim.getValue().entrySet().iterator();
				while (freq_it.hasNext()) {
					Map.Entry<Integer, ArrayList<UtilHopStats>> pair_freq = freq_it.next();
					ArrayList<UtilHopStats> util_stats_list = pair_freq.getValue();
					for (UtilHopStats stats: util_stats_list) {
						ArrayList<Object> objArray = new ArrayList<Object>();
						objArray.add(ap_id);
						objArray.add(stats.frequency);
						objArray.add(ts);
						objArray.add(stats.active_dur);
						objArray.add(stats.busy_dur);
						objArray.add(stats.recv_dur);
						objArray.add(stats.transmit_dur);
						objArray.add(stats.noise_floor);
						
						//String query = String.format("INSERT INTO utilhop VALUES(%d, %d, %d, %d, %d, %d, %d)", 
						//		ap_id, stats.frequency, ts, stats.active_dur, stats.busy_dur, stats.recv_dur, stats.transmit_dur);
						//queries.add(query);
						objArrayVector.add(objArray);
					}
					
					freq_it.remove();
				}
				
				tim_it.remove();
			}
			
			if (!dontRemove) {
				it.remove();
			}
		}
		
		//lock.unlock();
		DatabaseCommitter.ExecuteQuery(queryFormat, objArrayVector);
		//System.out.println("queries size = " + queries.size());
	}
	
	long mx_ts = 0;
	@Override
	public long get_maxts() {
		return mx_ts;
	}
	
	public void commit2() {
		//lock.lock();
		System.out.println("commiting util hop stats");
		ArrayList<String> queries = new ArrayList<String>();
		for (int ap_id: util_hop_stats.keySet()) {
			for (long ts: util_hop_stats.get(ap_id).keySet()) {
				for (int freq: util_hop_stats.get(ap_id).get(ts).keySet()) {
					for( UtilHopStats stats: util_hop_stats.get(ap_id).get(ts).get(freq)) {
						String query = String.format("INSERT INTO " + CoapConstants.UTIL_HOP_TABLE + " VALUES(%d, %d, %d, %d, %d, %d, %d, %d)", 
								ap_id, stats.frequency, ts, stats.active_dur, stats.busy_dur, stats.recv_dur, stats.transmit_dur, stats.noise_floor);
						queries.add(query);
					}
				}
			}
		}
		DatabaseCommitter.ExecuteQuery(queries);
		//if (DatabaseCommitter.ExecuteQuery(queries) == 0) {
		lock.lock();
		util_hop_stats.clear();
		lock.unlock();
		//}
		
		System.out.println("done commiting util hop stats" + queries.size());
	}
	
	HashMap<Integer, HashMap<Long, HashMap<Integer, ArrayList<UtilHopStats>>> > util_hop_stats = new HashMap<Integer, HashMap<Long, HashMap<Integer, ArrayList<UtilHopStats>>>>();
	public HashMap<Integer, ArrayList<UtilHopObject>> policyUtilHopMap = new HashMap<Integer, ArrayList<UtilHopObject>>();
	Lock lock = new ReentrantLock();
	@Override
	public int ClearPolicyMap(long tsLimit) {
		int cnt = 0;
		
		synchronized(policyUtilHopMap) {
			Iterator<Map.Entry<Integer, ArrayList<UtilHopObject>>> utilHopIterator = policyUtilHopMap.entrySet().iterator();
			while (utilHopIterator.hasNext()) {
				Map.Entry<Integer, ArrayList<UtilHopObject>> utilHopEntry = utilHopIterator.next();
				ArrayList<UtilHopObject> utilHopArray = utilHopEntry.getValue();
				Iterator<UtilHopObject> arrayIter = utilHopArray.iterator();
				
				while (arrayIter.hasNext()) {
					UtilHopObject obj = arrayIter.next();
					
					if (obj.ts > tsLimit - CoapConstants.INMEMORY_DATA_INTERVAL_SEC) {
						break;
					}
					
					cnt ++;
					arrayIter.remove();
				}
				
				//utilHopIterator.remove();
			}
			
			System.out.println("ClearMaps: " + tsLimit + " removed " + cnt + " utilhop entries...");
		}
		
		return cnt;
	}
}

class UtilHopObject {
	long ts;
	UtilHopStats stats;
}

class UtilHopStats {
	public UtilHopStats() {
		active_dur = busy_dur = recv_dur = transmit_dur = 0;
		frequency = -1;
		noise_floor = CoapConstants.UNK_NOISE_FLOOR;
	}
	
	public long active_dur, busy_dur, recv_dur, transmit_dur;
	public int frequency;
	public int noise_floor;
};
