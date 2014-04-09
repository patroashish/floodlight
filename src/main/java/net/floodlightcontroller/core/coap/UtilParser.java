package net.floodlightcontroller.core.coap;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class UtilParser implements Parser {

	@Override
	public void process(String rest, int ap_id) {
		if (ap_id <= 0) {
			return;
		}

		String[] terms = rest.split(" ");
		double util_val = Double.parseDouble(terms[0]);
		long active_tim = Long.parseLong(terms[1]);
		long busy_tim = Long.parseLong(terms[2]);
		long recv_tim = Long.parseLong(terms[3]);
		long transmit_tim = Long.parseLong(terms[4]);
		int frequency = Integer.parseInt(terms[5]);
		long sec = Long.parseLong(terms[6]);
		int noise_floor = Integer.parseInt(terms[7]);

	    // 1131: Robustness related code - filter out spurious ranges.
	    if (util_val < 0 || util_val > 100 || sec > 1491214493 || sec < 191214493)
	    {
	      System.out.println("Skipping util string: " + rest + " due to spurious values...");
	      return;
	    }

		UtilStats stats = new UtilStats();
		stats.frequency = frequency;
		stats.util = util_val;
		stats.active_tim = active_tim;
		stats.busy_tim = busy_tim;
		stats.recv_tim = recv_tim;
		stats.transmit_tim = transmit_tim;
		stats.noise_floor = noise_floor;
		
		updateMap(ap_id, sec, null, stats);
	}

	@Override
	public void updateMap(int ap_id, Long sec, String client_mac, Object o) {
		synchronized (util_map) {
			if (!util_map.containsKey(ap_id)) {
				util_map.put(ap_id, new HashMap<Long, UtilStats>());
			}
			
			mx_ts = Math.max(mx_ts, sec);
		}
		
		synchronized (policyUtilMap) {
			if (!policyUtilMap.containsKey(ap_id))
			{
				policyUtilMap.put(ap_id, new ArrayList<UtilObject>());
			}
			
			util_map.get(ap_id).put(sec, (UtilStats)o);
			UtilObject obj = new UtilObject();
			obj.stats = (UtilStats)o;
			obj.ts = sec;
			policyUtilMap.get(ap_id).add(obj);
		}
	}

	@Override
	public Object getHashMap() {
		//synchronized (util_map) {
		return util_map;
		//}
	}

	@Override
	public Object readHashMap() {
		return policyUtilMap;
	}
	
	Map<Integer, HashMap<Long, UtilStats> > util_map = new HashMap<Integer, HashMap<Long,UtilStats>>();
	Map<Integer, ArrayList<UtilObject>> policyUtilMap = new HashMap<Integer, ArrayList<UtilObject>>();
	ReentrantLock lock = new ReentrantLock();
	
	public void commit2(long ts_limit) {
		
		try {

		long ts_bm1 = System.currentTimeMillis();
		
		int count = 0; 
		
		Iterator<Map.Entry<Integer, HashMap<Long, UtilStats>>> it = util_map.entrySet().iterator();
		PreparedStatement ps = 
				DatabaseCommitter.conn.prepareStatement("insert into " + CoapConstants.UTIL_TABLE + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ");
		
		//lock.lock();
		while (it.hasNext()) {
			boolean dontRemove = false;
			Map.Entry<Integer, HashMap<Long, UtilStats>> pair = it.next();
			
			int ap_id = pair.getKey();
			Iterator<Map.Entry<Long, UtilStats>> tim_it = pair.getValue().entrySet().iterator();
			
			while (tim_it.hasNext()) {
				Map.Entry<Long, UtilStats> pair_tim = tim_it.next();
				long ts = pair_tim.getKey();
				
				if (ts > ts_limit) {
					dontRemove = true;
					continue;
				}
				
				UtilStats util_stats = pair_tim.getValue();
				
				ps.setInt(1, ap_id);
				ps.setInt(2, (int) ts);
				ps.setDouble(3, util_stats.util);
				ps.setInt(4, (int) util_stats.active_tim);
				ps.setInt(5, (int) util_stats.busy_tim);
				ps.setInt(6, (int) util_stats.recv_tim);
				ps.setInt(7, (int) util_stats.transmit_tim);
				ps.setInt(8, util_stats.frequency);
				ps.setInt(9, util_stats.noise_floor);
				
				count += 1;
				
				ps.addBatch(); 
				//ps.clearParameters();
			}
			
			if (!dontRemove) {
				it.remove();
			}
		}
		
		long ts_bm2 = System.currentTimeMillis();
		int[] results = ps.executeBatch();
		long ts_bm3 = System.currentTimeMillis();
		
		System.out.println("queries size = " + count +
				" Time: " + (ts_bm2 - ts_bm1) / 1000 + " " + (ts_bm3 - ts_bm2) / 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void commit(long ts_limit) {
		Iterator<Map.Entry<Integer, HashMap<Long, UtilStats>>> it = util_map.entrySet().iterator();
		//lock.lock();
		String queryFormat = "insert into " + CoapConstants.UTIL_TABLE + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
		ArrayList<ArrayList<Object>> objArrayVector = new ArrayList<ArrayList<Object>>();
		
		while (it.hasNext()) {
			/*if (queries.size() > MAX_ENTRIES) {
				System.out.println("Exceeded entries...");
				break;
			}*/
			
			boolean dontRemove = false;
			Map.Entry<Integer, HashMap<Long, UtilStats>> pair = it.next();
			int ap_id = pair.getKey();
			Iterator<Map.Entry<Long, UtilStats>> tim_it = pair.getValue().entrySet().iterator();
			while (tim_it.hasNext()) {
				Map.Entry<Long, UtilStats> pair_tim = tim_it.next();
				long ts = pair_tim.getKey();
				
				UtilStats util_stats = pair_tim.getValue();
				ArrayList<Object> objArray = new ArrayList<Object>();
				objArray.add(ap_id);
				objArray.add(ts);
				objArray.add(util_stats.util);
				objArray.add(util_stats.active_tim);
				objArray.add(util_stats.busy_tim);
				objArray.add(util_stats.recv_tim);
				objArray.add(util_stats.transmit_tim);
				objArray.add(util_stats.frequency);
				objArray.add(util_stats.noise_floor);
				
				objArrayVector.add(objArray);
								
				tim_it.remove();
			}
			
			if (!dontRemove) {
				it.remove();
			}
		}
		
		//lock.unlock();
		//DatabaseCommitter.ExecuteQuery(queries);
		DatabaseCommitter.ExecuteQuery(queryFormat, objArrayVector);
		//System.out.println("queries size = " + queries.size());
	}
	
	long mx_ts = 0;
	@Override
	public long get_maxts() {
		return mx_ts;
	}
	public void commit2() {
		System.out.println("commiting util stats");
		//boolean dontRemove = false;
		//synchronized (util_map) {
		ArrayList<String> queries = new ArrayList<String>();
		for (int ap_id: util_map.keySet()) {
			boolean dontRemove_ap = false;
			for (long ts: util_map.get(ap_id).keySet()) {
				UtilStats util_stats = util_map.get(ap_id).get(ts);
				String query = String.format("insert into util VALUES(%d, %d, %f, %d, %d, %d, %d, %d, %d)", 
						ap_id, 
						ts,
						util_stats.util, 
						util_stats.frequency, 
						util_stats.active_tim, 
						util_stats.busy_tim,
						util_stats.recv_tim,
						util_stats.transmit_tim,
						util_stats.noise_floor);
				/*if (DatabaseCommitter.ExecuteQuery(query) == 0) {
					//util_map.get(ap_id).remove(ts);
				} else {
					dontRemove_ap = true;
					//dontRemove = true;
				}*/
				queries.add(query);
			}

			if (!dontRemove_ap) {
				//	util_map.remove(ap_id);
			}
		}

		//if (!dontRemove) {
		DatabaseCommitter.ExecuteQuery(queries);
		synchronized (util_map) {
			util_map.clear();
		}
		
		//}
		//}
		System.out.println("done commiting util stats queries = " + queries.size());
	}

	@Override
	public int ClearPolicyMap(long tsLimit) {
		int cnt = 0;
		
		synchronized(policyUtilMap) {
			Iterator<Map.Entry<Integer, ArrayList<UtilObject>>> utilIterator = policyUtilMap.entrySet().iterator();
			while (utilIterator.hasNext()) {
				Map.Entry<Integer, ArrayList<UtilObject>> utilEntry = utilIterator.next();
				ArrayList<UtilObject> utilArray = utilEntry.getValue();
				Iterator<UtilObject> arrayIter = utilArray.iterator();
				
				while (arrayIter.hasNext()) {
					UtilObject obj = arrayIter.next();
					if (obj.ts > tsLimit - CoapConstants.INMEMORY_DATA_INTERVAL_SEC) {
						break;
					}
					
					cnt ++;
					arrayIter.remove();
				}
			}
			
			System.out.println("ClearMaps: " + tsLimit + " removed " + cnt + " util entries...");
		}
		
		return cnt;
	}
}

class UtilObject {
	public UtilStats stats;
	public long ts;
}

class UtilStats {
	public UtilStats() {
		frequency = -1;
		active_tim = -1;
		busy_tim = -1;
		recv_tim = -1; 
		transmit_tim = -1;
		util = -1;
		noise_floor = CoapConstants.UNK_NOISE_FLOOR;
	}
	
	public int frequency;
	public double util;
	public long active_tim, busy_tim, recv_tim, transmit_tim;
	public int noise_floor;
}
