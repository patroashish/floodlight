package net.floodlightcontroller.core.coap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HigherLayerParser implements Parser {
	@Override
	public void process(String rest, int ap_id) {

		int cnt = Integer.parseInt(rest.substring(0, 2));
		if (cnt == 0) {
			return;
		}

		rest = rest.substring(3);
		String[] terms = rest.split(";");
		assert(cnt == terms.length);

		for (int i = 0; i < cnt; ++i) {
			// 28:CF:E9:18:14:C1 2915184226 3232236431 80 53080 TCP 36 47610 1392313564
			String curr = terms[i];
			String client, typ;
			String[] curr_terms = curr.split(" ");

			//System.out.println("higherlayer term " + i + " :" + terms[i] + "--" + curr_terms[6] + "XX");

			client = curr_terms[0];

			// TODO: Not used
			long srcIP = Long.parseLong(curr_terms[1]);
			long dstIP = Long.parseLong(curr_terms[2]);

			typ = curr_terms[5];

			int srcPort = 0, dstPort = 0, packet_cnt = 0, num_bytes = 0, num_retries = 0;
			long sec = 0;

			// TODO: Not sure about ICMP.
			if (!typ.equals("ICMP"))
			{
				srcPort = Integer.parseInt(curr_terms[3]);
				dstPort = Integer.parseInt(curr_terms[4]);

				packet_cnt = Integer.parseInt(curr_terms[6]);
				num_bytes = Integer.parseInt(curr_terms[7]);
				sec = Long.parseLong(curr_terms[8]);
			} else {
				packet_cnt = Integer.parseInt(curr_terms[6]);
				num_bytes = Integer.parseInt(curr_terms[7]);
				sec = Long.parseLong(curr_terms[8]);
			}

			// TODO: Not using retries for now.
			// int num_retries = Integer.parseInt(curr_terms[5]);

			HigherLayerStats stats = new HigherLayerStats();

			stats.ts = sec;
			stats.num_bytes = num_bytes;
			stats.packet_cnt = packet_cnt;

      // TODO: 0218: Added to get more information about the traffic flow.
			stats.type = typ + " " + srcPort + " " + dstPort;
			stats.packet_retries = num_retries;

			// 0216: Added client info to get a direct reference, pretty inefficient otherwise.
			stats.client = client.toLowerCase();
			
			stats.srcIP = srcIP;
			stats.dstIP = dstIP;
			stats.srcPort = srcPort;
			stats.dstPort = dstPort;

			updateMap(ap_id, sec, client, stats);
		}
	}

	@Override
	public void updateMap(int ap_id, Long sec, String client_mac, Object o) {

		//synchronized (higherlayer_stats_map) {
		//lock.lock();
		if (!higherlayer_stats_map.containsKey(ap_id)) {
			higherlayer_stats_map.put(ap_id, new HashMap<Long, HashMap<String, HigherLayerStats>>());
		}

		if (!higherlayer_stats_map.get(ap_id).containsKey(sec)) {
			higherlayer_stats_map.get(ap_id).put(sec, new HashMap<String, HigherLayerStats>());
		}

		higherlayer_stats_map.get(ap_id).get(sec).put(client_mac, (HigherLayerStats)o);
		mx_ts = Math.max(mx_ts, sec);

		synchronized (policyHigherLayerMap) {
			if (!policyHigherLayerMap.containsKey(ap_id)) {
				policyHigherLayerMap.put(ap_id, new ArrayList<HigherLayerObject>());
			}

			boolean found = false;
			for (int i = 0; i < policyHigherLayerMap.get(ap_id).size(); i++)
			{
				if (client_mac.equals(policyHigherLayerMap.get(ap_id).get(i).client))
				{
					policyHigherLayerMap.get(ap_id).get(i).statsList.add((HigherLayerStats) o);
					found = true;
					break;
				}
			}

			if (!found)
			{
				HigherLayerObject obj = new HigherLayerObject();
				obj.statsList = new ArrayList<HigherLayerStats>();
				obj.client = client_mac;
				obj.statsList.add((HigherLayerStats)o);

				policyHigherLayerMap.get(ap_id).add(obj);
			}

			/*
			HigherLayerObject obj = null;
			for (HigherLayerObject obj2: policyHigherLayerMap.get(ap_id)) {
				if (obj2.client.equals(client_mac)) {
					obj = obj2;
				}
			}

			if (obj == null) {
				obj = new HigherLayerObject();
				policyHigherLayerMap.get(ap_id).add(obj);
				obj.client = client_mac;
			}

			obj.ts_list.add(sec);
			obj.stats_list.add((HigherLayerStats)o);
			 */
		}
		//lock.unlock();
		//}
	}

	@Override
	public synchronized Object getHashMap() {
		synchronized (higherlayer_stats_map) {
			return higherlayer_stats_map;
		}
	}

	@Override
	public void commit(long ts_limit) {
		Iterator<Map.Entry<Integer, HashMap<Long, HashMap<String, HigherLayerStats> > > > it = higherlayer_stats_map.entrySet().iterator();
		String queryFormat = "insert into " + CoapConstants.HIGHER_LAYER_TABLE + " VALUES(?, ?, ?, ?, ?, ?, ?)";
		ArrayList<ArrayList<Object>> params = new ArrayList<ArrayList<Object>>();

		while (it.hasNext()) {
			boolean dontRemove = false;

			Map.Entry<Integer, HashMap<Long, HashMap<String, HigherLayerStats> > > pair = it.next();
			int ap_id = pair.getKey();
			Iterator<Map.Entry<Long, HashMap<String, HigherLayerStats> > > tim_it = pair.getValue().entrySet().iterator();
			while (tim_it.hasNext()) {
				Map.Entry<Long, HashMap<String, HigherLayerStats>> pair_tim = tim_it.next();
				long ts = pair_tim.getKey();

				Iterator<Map.Entry<String, HigherLayerStats>> link_it = pair_tim.getValue().entrySet().iterator();
				while (link_it.hasNext()) {
					Map.Entry<String, HigherLayerStats> pair_link = link_it.next();
					String client = pair_link.getKey();
					HigherLayerStats stats = pair_link.getValue();					

					ArrayList<Object> objArray = new ArrayList<Object>();
					objArray.add(ap_id);
					objArray.add(ts);
					objArray.add(client);
					objArray.add(stats.type);
					objArray.add(stats.packet_cnt);
					objArray.add(stats.num_bytes);
					objArray.add(stats.packet_retries);
					params.add(objArray);
					link_it.remove();
				}

				tim_it.remove();
			}

			if (!dontRemove) {
				it.remove();
			}
		}

		/*if (queries.size() > 0) {
			DatabaseCommitter.ExecuteQuery(queries);
		}*/
		DatabaseCommitter.ExecuteQuery(queryFormat, params);
		//System.out.println("queries size = " + queries.size());
	}

	public synchronized void commit2() {
		//System.out.println("thread = " + Thread.currentThread().getName());
		System.out.println("commiting higher layer stats");
		boolean dontRemove = false;
		ArrayList<String> queries = new ArrayList<String>();
		//lock.lock();
		//synchronized (higherlayer_stats_map) {
		// HashMap<Integer, HashMap<Long, HashMap<String, HigherLayerStats> > > higherlayer_stats_map2 = new HashMap<Integer, HashMap<Long,HashMap<String,HigherLayerStats>>>(higherlayer_stats_map);//(HashMap<Integer, HashMap<Long, HashMap<String, HigherLayerStats> > >)(higherlayer_stats_map.);
		for (int ap_id: higherlayer_stats_map.keySet()) {
			boolean dontRemove_ap = false;
			for (long ts: higherlayer_stats_map.get(ap_id).keySet()) {
				boolean dontRemove_ts = false;
				for (String client: higherlayer_stats_map.get(ap_id).get(ts).keySet()) {
					HigherLayerStats stats = higherlayer_stats_map.get(ap_id).get(ts).get(client);
					String query = String.format(
							"insert into " + CoapConstants.HIGHER_LAYER_TABLE + " VALUES(%d, %d, '%s', '%s', %d, %d, %d)", 
							ap_id,
							ts,
							client,
							stats.type,
							stats.packet_cnt,
							stats.num_bytes,
							stats.packet_retries);
					queries.add(query);
					/*if (DatabaseCommitter.ExecuteQuery(query) == 0) {
						//higherlayer_stats_map.get(ap_id).get(ts).remove(client);
					} else {
						dontRemove_ts = true;
						dontRemove_ap = true;
						dontRemove = true;
					}*/

				}

				if (!dontRemove_ts) {
					//higherlayer_stats_map.get(ap_id).remove(ts);
				}
			}

			if (!dontRemove_ap) {
				//higherlayer_stats_map.remove(ap_id);
			}
		}

		if (!dontRemove) {
			//higherlayer_stats_map.clear();
		}

		DatabaseCommitter.ExecuteQuery(queries);
		//}
		lock.lock();
		higherlayer_stats_map.clear();
		lock.unlock();
		System.out.println("done commiting higher layer stats");
	}

	static HashMap<Integer, HashMap<Long, HashMap<String, HigherLayerStats> > > higherlayer_stats_map = new HashMap<Integer, HashMap<Long,HashMap<String,HigherLayerStats>>>();
	static HashMap<Integer,  ArrayList<HigherLayerObject>> policyHigherLayerMap = new HashMap<Integer, ArrayList<HigherLayerObject>>();

	static Lock lock = new ReentrantLock();
	long mx_ts = 0;

	@Override
	public long get_maxts() {
		return mx_ts;
	}

	@Override
	public Object readHashMap() {
		return policyHigherLayerMap;
	}

	@Override
	public int ClearPolicyMap(long tsLimit) {
		int cnt = 0;

		synchronized(policyHigherLayerMap) {
			Iterator<Map.Entry<Integer, ArrayList<HigherLayerObject>>> higherLayerIterator = policyHigherLayerMap.entrySet().iterator();
			while (higherLayerIterator.hasNext()) {
				Map.Entry<Integer, ArrayList<HigherLayerObject>> higherLayerEntry = higherLayerIterator.next();
				ArrayList<HigherLayerObject> higherLayerArray = higherLayerEntry.getValue();
				Iterator<HigherLayerObject> arrayIter = higherLayerArray.iterator();

				while (arrayIter.hasNext()) {
					HigherLayerObject obj = arrayIter.next();
					Iterator<HigherLayerStats> stats_iter = obj.statsList.iterator();

					while (stats_iter.hasNext()) {
						Long ts = stats_iter.next().ts;
						if (ts > tsLimit - CoapConstants.INMEMORY_DATA_INTERVAL_SEC) {
							break;
						}

						stats_iter.remove();
						cnt ++;
					}
				}
			}

			System.out.println("ClearMaps: " + tsLimit + " removed " + cnt + " higher layer entries...");
		}

		return cnt;
	}
}

class HigherLayerObject {
	public HigherLayerObject() {
		statsList = new ArrayList<HigherLayerStats>();
	}

	public String client;
	ArrayList<HigherLayerStats> statsList;
}
