package net.floodlightcontroller.core.coap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class PassiveStatsParser implements Parser {
	String trim_zeros(String in, int pos) {
		
		if (in.length() < 2)
		{
			return in;
		}
		
		while(pos > 0) {
			in = in.replaceAll("^0", "");
			pos --;
		}
		
		return in;
	}
	
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
			String[] curr_terms = curr.split(" ");
			
			//System.out.println("passive term " + i + " :" + terms[i] + "--" + curr_terms[6] + "XX");

			String ap = curr_terms[0];
			client = curr_terms[1];
			int bytes_per_packet = Integer.parseInt(trim_zeros(curr_terms[2], 3));
			float rate = Float.parseFloat(curr_terms[3]);
			float rssi = Float.parseFloat(curr_terms[4]);
			
			int packet_cnt = Integer.parseInt(trim_zeros(curr_terms[5], 4));
			int packet_retries = Integer.parseInt(trim_zeros(curr_terms[6], 4));
			
			long sec = Long.parseLong(curr_terms[7]);
			String rate_string = curr_terms[8];
			int channel = Integer.parseInt(curr_terms[9]);
			
			PassiveStats stats = new PassiveStats(bytes_per_packet, rate, rssi, packet_cnt, packet_retries, rate_string, channel);
			updateMap(ap_id, sec, String.format("%s %s", ap, client), stats);
		}
	}
	
	@Override
	public void updateMap(int ap_id, Long sec, String link, Object o) {
		//lock.lock();
		//synchronized (passive_stats_map) {
		if (!passive_stats_map.containsKey(ap_id)) {
			passive_stats_map.put(ap_id, new HashMap<Long, HashMap<String, PassiveStats> >());
		}

		if (!passive_stats_map.get(ap_id).containsKey(sec)) {
			passive_stats_map.get(ap_id).put(sec, new HashMap<String, PassiveStats>());
		}

		passive_stats_map.get(ap_id).get(sec).put(link, (PassiveStats)o);
		//}
		//lock.unlock();
		mx_ts = Math.max(mx_ts, sec);
	}

	@Override
	public synchronized Object getHashMap() {
		return passive_stats_map;
	}

	@Override
	public void commit(long ts_limit) 
	{
		//ArrayList<String> queries = new ArrayList<String>();
		String queryFormat = "insert into " + CoapConstants.PASSIVE_TABLE + " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		ArrayList<ArrayList<Object>> params = new ArrayList<ArrayList<Object>>();
		Iterator<Map.Entry<Integer, HashMap<Long, HashMap<String, PassiveStats> > > > it = passive_stats_map.entrySet().iterator();
		while (it.hasNext()) {
			boolean dontRemove = false;
			Map.Entry<Integer, HashMap<Long, HashMap<String, PassiveStats> > > pair = it.next();
			int ap_id = pair.getKey();
			Iterator<Map.Entry<Long, HashMap<String, PassiveStats> > > tim_it = pair.getValue().entrySet().iterator();
			while (tim_it.hasNext()) {
				Map.Entry<Long, HashMap<String, PassiveStats>> pair_tim = tim_it.next();
				long ts = pair_tim.getKey();
				
				Iterator<Map.Entry<String, PassiveStats>> link_it = pair_tim.getValue().entrySet().iterator();
				while (link_it.hasNext()) {
					Map.Entry<String, PassiveStats> pair_link = link_it.next();
					String link = pair_link.getKey();
					PassiveStats stats = pair_link.getValue();
					String ap = link.split(" ")[0], client = link.split(" ")[1];
					ArrayList<Object> objArray = new ArrayList<Object>();
					objArray.add(ap_id);
					objArray.add(ts);
					objArray.add(ap);
					objArray.add(client);
					objArray.add(stats.bytes_per_packet);
					objArray.add(stats.avg_rate);
					objArray.add(stats.avg_rssi);
					objArray.add(stats.packet_cnt);
					objArray.add(stats.packet_retries);
					objArray.add(stats.rate_string);
					objArray.add(stats.channel);
					params.add(objArray);
					link_it.remove();
				}
				
				tim_it.remove();
			}
			
			if (!dontRemove) {
				it.remove();
			}
		}
		
		//System.out.println("queries size = " + queries.size());
		//DatabaseCommitter.ExecuteQuery(queries);
		DatabaseCommitter.ExecuteQuery(queryFormat, params);
	}
	
	public void commit2() 
	{
		System.out.println("commiting passive stats");
		//lock.lock();
		//boolean dontRemove = false;
		ArrayList<String> queries = new ArrayList<String>();
		//synchronized (passive_stats_map) {
		for (int ap_id: passive_stats_map.keySet()) {
			boolean dontRemove_ap = false;
			for (long ts: passive_stats_map.get(ap_id).keySet()) {
				boolean dontRemove_ts = false;
				for (String link: passive_stats_map.get(ap_id).get(ts).keySet()) {
					String ap = link.split(" ")[0], client = link.split(" ")[1];
					PassiveStats stats = passive_stats_map.get(ap_id).get(ts).get(link);
					String query = String.format("insert into " + CoapConstants.PASSIVE_TABLE + " values(%d, %d, '%s', '%s', %d, %f, %f, %d, %d, %s, %d)", 
							ap_id, ts, ap, client, stats.bytes_per_packet, stats.avg_rate, stats.avg_rssi, stats.packet_cnt, stats.packet_retries,
							stats.rate_string, stats.channel);
					/*if (DatabaseCommitter.ExecuteQuery(query) == 0) {
							//passive_stats_map.get(ap_id).get(ts).remove(link);
						} else {
							dontRemove_ts = true;
							dontRemove_ap = true;
							dontRemove = true;
						}*/
					queries.add(query);
				}

				if (!dontRemove_ts) {
					//passive_stats_map.get(ap_id).remove(ts);
				}
			}

			if (!dontRemove_ap) {
				//passive_stats_map.remove(ap_id);
			}
		}

		DatabaseCommitter.ExecuteQuery(queries);
		//if (!dontRemove) {
		lock.lock();
		passive_stats_map.clear();
		lock.unlock();
		//}
		
		DatabaseCommitter.ExecuteQuery(queries);
		System.out.println("done commiting passive stats" + queries.size());
	}
	
	@Override
	public long get_maxts() {
		return mx_ts;
	}
	
	HashMap<Integer, HashMap<Long, HashMap<String, PassiveStats> > > passive_stats_map = new HashMap<Integer, HashMap<Long,HashMap<String,PassiveStats>>>();
	Lock lock = new ReentrantLock();
	long mx_ts = 0;
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
