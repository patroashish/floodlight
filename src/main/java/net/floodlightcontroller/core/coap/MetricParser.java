package net.floodlightcontroller.core.coap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class MetricParser implements Parser {

	@Override
	public void process(String rest, int ap_id) {
		if (ap_id <= 0) {
			return;
		}

		int cnt = Integer.parseInt(rest.substring(0, 2));
		if (cnt == 0) {
			return;
		}

		rest = rest.substring(3);
		String[] terms = rest.split(";");
		
		assert(cnt == terms.length);
		for (int i = 0; i < cnt; ++i) {
			String curr = terms[i];
			String client;
			Double metric;
			String[] curr_terms = curr.split(" ");
			client = curr_terms[0];
			metric = Double.parseDouble(curr_terms[1]);
			long sec = Long.parseLong(curr_terms[2]);
			
			MetricEvent currEvent = new MetricEvent();
			currEvent.ts = sec;
			currEvent.ap_id = ap_id;
			currEvent.metric = metric;
			
			//DFW: need to make sure the parsed value makes sense, the range will be between -5.0 and 45.0
			if(metric < -5.0 || metric > 45.0) {
				return;
			}
			
			updateMap(ap_id, sec, client, currEvent);
		}
	}
	
	@Override
	public void updateMap(int ap_id, Long sec, String client_mac, Object o) {
		//lock.lock();
		//synchronized (metric_map) {
		if (!metric_map.containsKey(ap_id)) {
			metric_map.put(ap_id, new HashMap<Long, HashMap<String, Double>>());
		}
			
		if (!metric_map.get(ap_id).containsKey(sec)) {
			metric_map.get(ap_id).put(sec, new HashMap<String, Double>());
		}
		
		metric_map.get(ap_id).get(sec).put(client_mac, ((MetricEvent)o).metric);
		mx_ts = Math.max(mx_ts, sec);
		
		synchronized(policyMetricMap)
		{
			if (!policyMetricMap.containsKey(ap_id))
			{
				policyMetricMap.put(ap_id, new ArrayList<MetricStatsObject>());
			}
			boolean found = false;
			for (int i = 0; i < policyMetricMap.get(ap_id).size(); i++)
			{
				if (client_mac.equals(policyMetricMap.get(ap_id).get(i).client))
				{
					policyMetricMap.get(ap_id).get(i).metricList.add((MetricEvent) o);
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				MetricStatsObject obj = new MetricStatsObject();
				obj.metricList = new ArrayList<MetricEvent>();
				obj.client = client_mac;
				obj.metricList.add((MetricEvent)o);
				policyMetricMap.get(ap_id).add(obj);
			}
			
			/*
			MetricStatsObject obj = null;
			for (MetricStatsObject obj2: policyMetricMap.get(ap_id)) {
				if (obj2.client.equals(client_mac)) {
					obj = obj2;
					break;
				}
			}
			
			if (obj == null) {
				obj = new MetricStatsObject(client_mac);
				policyMetricMap.get(ap_id).add(obj);
			}
			
			obj.metricList.add((Double)o);
			obj.tsList.add(sec);
			*/
		}
		//lock.unlock();
	}

	@Override
	public Object getHashMap() {
		synchronized (metric_map) {
			return metric_map;
		}
	}
	
	HashMap<Integer, HashMap<Long, HashMap<String, Double> > > metric_map = new HashMap<Integer, HashMap<Long, HashMap<String,Double> > >();
	HashMap<Integer, ArrayList<MetricStatsObject>> policyMetricMap = new HashMap<Integer, ArrayList<MetricStatsObject>>();
	Lock lock = new ReentrantLock();
	
	@Override
	public void commit(long ts_limit) {
		ArrayList<ArrayList<Object>> objArrayVector = new ArrayList<ArrayList<Object>>();
		String queryFormat = "insert into " + CoapConstants.METRIC_TABLE + " VALUES (?, ?, ?, ?)";
		
		@SuppressWarnings("rawtypes")
		Iterator it = metric_map.entrySet().iterator();
		while (it.hasNext()) {
			//if (queries.size() > Constants.MAX_QUERIES) {
			//	break;
			//}
			@SuppressWarnings("unchecked")
			Map.Entry<Integer, HashMap<Long, HashMap<String, Double> >> pairs = (Map.Entry<Integer, HashMap<Long, HashMap<String, Double>>>)it.next();
			Integer ap_id = pairs.getKey();
			HashMap<Long, HashMap<String, Double> > tim_hashmap = pairs.getValue();
			Iterator<Map.Entry<Long, HashMap<String, Double>>> tim_it = tim_hashmap.entrySet().iterator();
			while (tim_it.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry tim_pairs = tim_it.next();
				Long ts = (Long)tim_pairs.getKey();
				/*if (ts > ts_limit || queries.size() > 100) {
					if (queries.size() > 100) {
						break;
					}
					
					continue;
				}*/
				
				@SuppressWarnings("unchecked")
				HashMap<String, Double> client_hashmap = (HashMap<String, Double>)tim_pairs.getValue();
				@SuppressWarnings("rawtypes")
				Iterator client_it = client_hashmap.entrySet().iterator();
				
				while (client_it.hasNext()) {
					@SuppressWarnings("unchecked")
					Map.Entry<String, Double> client_pairs = (Map.Entry<String, Double>)client_it.next();
					String client = client_pairs.getKey();
					Double metric = client_pairs.getValue();
					/*String query = String.format("insert into metric_val values(%d, '%s', %d, %f)", 
							ap_id, client, ts, metric);
					queries.add(query);*/
					ArrayList<Object> objArray = new ArrayList<Object>();
					objArray.add(ap_id);
					objArray.add(client);
					objArray.add(ts);
					objArray.add(metric);
					objArrayVector.add(objArray);
					client_it.remove();
				}
				
				tim_it.remove();
			}
			
			it.remove();
		}
		
		//DatabaseCommitter.ExecuteQuery(queries);
		DatabaseCommitter.ExecuteQuery(queryFormat, objArrayVector);
		//System.out.println("queries size = " + queries.size());
	}
	
	public synchronized void commit2() {
		System.out.println("commiting metric stats");
		ArrayList<String> queries = new ArrayList<String>();
		//lock.lock();
		// write metric per ap to the database
		//boolean dontRemove = false;
		//synchronized (metric_map) {
		for (int ap_id : metric_map.keySet()) {
			boolean dontRemove_apid = false;
			for (long timestamp: metric_map.get(ap_id).keySet()) {
				boolean dontRemove_ts = false;
				for (String client: metric_map.get(ap_id).get(timestamp).keySet()) {
					Double metric = metric_map.get(ap_id).get(timestamp).get(client);
					String query = String.format("insert into " + CoapConstants.METRIC_TABLE +
							" values (%d, '%s', %d, %f)", 
							ap_id, client, timestamp, metric);
					queries.add(query);
					/*if (DatabaseCommitter.ExecuteQuery(query) == 0) {
							//metric_map.get(ap_id).get(timestamp).remove(client);
						} else {
							dontRemove_apid = true;
							dontRemove_ts = true;
							dontRemove = true;
						}*/

				}

				if (!dontRemove_ts) {
					//metric_map.get(ap_id).remove(timestamp);
				}
			}

			if (!dontRemove_apid) {
				//metric_map.remove(ap_id);
			}
		}

		//if (!dontRemove) {
		lock.lock();
		metric_map.clear();
		lock.unlock();
		//}
		//}
		
		//lock.unlock();
		DatabaseCommitter.ExecuteQuery(queries);
		System.out.println("done commiting metric stats " + queries.size());
	}
	
	long mx_ts = 0;
	@Override
	public long get_maxts() {
		return mx_ts;
	}

	@Override
	public Object readHashMap() {
		return policyMetricMap;
	}

	@Override
	public int ClearPolicyMap(long tsLimit) {
		int cnt = 0;
		
		synchronized(policyMetricMap) {
			Iterator<Map.Entry<Integer, ArrayList<MetricStatsObject>>> metricIterator = policyMetricMap.entrySet().iterator();
			while (metricIterator.hasNext()) {
				Map.Entry<Integer, ArrayList<MetricStatsObject>> metricEntry = metricIterator.next();
				ArrayList<MetricStatsObject> metricArray = metricEntry.getValue();
				Iterator<MetricStatsObject> arrayIter = metricArray.iterator();
				
				while (arrayIter.hasNext()) {
					MetricStatsObject obj = arrayIter.next();
					Iterator<MetricEvent> stats_iter = obj.metricList.iterator();
					
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
			
			System.out.println("ClearMaps: " + tsLimit + " removed " + cnt + " metric entries...");
		}
		
		return cnt;
	}
}

class MetricStatsObject {
	public MetricStatsObject() {
		metricList = new ArrayList<MetricEvent>();
	}
	
	public MetricStatsObject(String client) {
		this.client = new String(client);
		metricList = new ArrayList<MetricEvent>();
	}
	
	String client;
	ArrayList<MetricEvent> metricList;
}
