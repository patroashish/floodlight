package net.floodlightcontroller.core.coap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class PassiveHopParser implements Parser {
	String trim_zeros(String in, int pos) {
		
		if (in.length() < 2)
			return in;
		
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

			if (curr_terms.length < 10) 
			{
				System.err.println("Passive stats warning!!! Got improper input: " + curr);

				try
				{
					PrintWriter out = new PrintWriter(new FileOutputStream(new File("bad_passive_hop.txt"), true));
					out.println("Bad " + ap_id + " : " + curr);
					out.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				continue;
			}
			
			//System.out.println("passivehop term " + i + " :" + terms[i] + "--" + curr_terms[6] + "XX");

			String ap = curr_terms[0];
			client = curr_terms[1];
			int bytes_per_packet = Integer.parseInt(trim_zeros(curr_terms[2], 3));
			float rate = Float.parseFloat(curr_terms[3]);
			float rssi = Float.parseFloat(curr_terms[4]);

			int packet_cnt = Integer.parseInt(trim_zeros(curr_terms[5], 4));
			int packet_retries = Integer.parseInt(trim_zeros(curr_terms[6], 4));
			int channel = Integer.parseInt(curr_terms[7]);
			long sec = Long.parseLong(curr_terms[8]);
			String rate_string = curr_terms[9];

			PassiveHopStats stats = new PassiveHopStats(bytes_per_packet, rate, rssi, packet_cnt, packet_retries, channel, rate_string);

			updateMap(ap_id, sec, String.format("%s %s", ap, client), stats);
		}
	}

	@Override
	public void updateMap(int ap_id, Long sec, String client_mac, Object o) {
		PassiveHopStats stats = (PassiveHopStats)o;
		int ch = stats.channel;
		//lock.lock();
		if (!stats_map.containsKey(ap_id)) {
			stats_map.put(ap_id, new HashMap<Long, HashMap<Integer, HashMap<String, PassiveHopStats>>>());
		}

		if (!stats_map.get(ap_id).containsKey(sec)) {
			stats_map.get(ap_id).put(sec, new HashMap<Integer, HashMap<String,PassiveHopStats>>());
		}

		if (!stats_map.get(ap_id).get(sec).containsKey(ch)) {
			stats_map.get(ap_id).get(sec).put(ch, new HashMap<String,PassiveHopStats>());
		}

		stats_map.get(ap_id).get(sec).get(ch).put(client_mac, stats);
		mx_ts = Math.max(mx_ts, sec);
		//lock.unlock();
	}

	@Override
	public Object getHashMap() {
		return stats_map;
	}

	@Override
	public void commit(long ts_limit) {
		//ArrayList<String> queries = new ArrayList<String>();
		String queryFormat = "INSERT INTO " + CoapConstants.PASSIVE_HOP_TABLE + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		ArrayList<ArrayList<Object>> params = new ArrayList<ArrayList<Object>>();
		//lock.lock();
		Iterator<Map.Entry<Integer, HashMap<Long, HashMap<Integer, HashMap<String, PassiveHopStats> > > > > it = stats_map.entrySet().iterator();
		while (it.hasNext()) {
			boolean dontRemove = false;
			Map.Entry<Integer, HashMap<Long, HashMap<Integer, HashMap<String, PassiveHopStats> > > > pair = it.next();
			int ap_id = pair.getKey();
			Iterator<Map.Entry<Long, HashMap<Integer, HashMap<String, PassiveHopStats> > > > tim_it = pair.getValue().entrySet().iterator();
			while (tim_it.hasNext()) {
				Map.Entry<Long, HashMap<Integer, HashMap<String, PassiveHopStats>>> pair_tim = tim_it.next();
				long ts = pair_tim.getKey();

				Iterator<Map.Entry<Integer, HashMap<String, PassiveHopStats>>> freq_it = pair_tim.getValue().entrySet().iterator();
				while (freq_it.hasNext()) {
					Map.Entry<Integer, HashMap<String, PassiveHopStats>> pair_freq = freq_it.next();
					Integer frequency = pair_freq.getKey();
					Iterator<Map.Entry<String, PassiveHopStats>> link_it = pair_freq.getValue().entrySet().iterator();
					while (link_it.hasNext()) {
						Map.Entry<String, PassiveHopStats> pair_link = link_it.next();
						String link = pair_link.getKey();
						String ap = link.split(" ")[0], client = link.split(" ")[1];
						PassiveHopStats stats = pair_link.getValue();
						ArrayList<Object> objArray = new ArrayList<Object>();
						objArray.add(ap_id);
						objArray.add(ts);
						objArray.add(frequency);
						objArray.add(ap);
						objArray.add(client);
						objArray.add(stats.bytes_per_packet);
						objArray.add(stats.avg_rate);
						objArray.add(stats.avg_rssi);
						objArray.add(stats.packet_cnt);
						objArray.add(stats.packet_retries);
						objArray.add(stats.rate_string);
						params.add(objArray);
						//queries.add(query);
						link_it.remove();
					}

					/*if (queries.size() > 10) {
						DatabaseCommitter.ExecuteQuery(queries);
						queries.clear();
					}*/

					freq_it.remove();
				}

				tim_it.remove();
			}

			if (!dontRemove) {
				it.remove();
			}
		}

		//lock.unlock();
		/*if (queries.size() > 0) {
			DatabaseCommitter.ExecuteQuery(queries);
		}*/

		DatabaseCommitter.ExecuteQuery(queryFormat, params);
		//System.out.println("queries size = " + queries.size());
	}

	long mx_ts = 0;
	@Override
	public long get_maxts() {
		return mx_ts;
	}

	public void commit2() {
		System.out.println("committing passive hop parser");
		ArrayList<String> queries = new ArrayList<String>();
		//lock.lock();
		for(int ap_id: stats_map.keySet()) {
			for (long ts: stats_map.get(ap_id).keySet()) {
				for (int ch: stats_map.get(ap_id).get(ts).keySet()) {
					for (String link: stats_map.get(ap_id).get(ts).get(ch).keySet()) {
						String ap = link.split(" ")[0], client = link.split(" ")[1];
						PassiveHopStats stats = stats_map.get(ap_id).get(ts).get(ch).get(link);
						String query = String.format(
								"INSERT INTO " + CoapConstants.PASSIVE_HOP_TABLE + " VALUES(%d, %d, %d, '%s', '%s', %d, %f, %f, %d, %d)", 
								ap_id, ts, ch, ap, client, stats.bytes_per_packet, stats.avg_rate, stats.avg_rssi, stats.packet_cnt, stats.packet_retries);
						queries.add(query);
					}
				}
			}
		}

		//if (DatabaseCommitter.ExecuteQuery(queries) == 0) {
		lock.lock();
		stats_map.clear();
		lock.unlock();
		//}

		System.out.println("done committing passive hop parser" + queries.size());
	}

	HashMap<Integer, HashMap<Long, HashMap<Integer, HashMap<String, PassiveHopStats> > > > stats_map = new HashMap<Integer, HashMap<Long,HashMap<Integer,HashMap<String,PassiveHopStats>>>>();
	static ReentrantLock lock = new ReentrantLock();

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

class PassiveHopStats {
	public PassiveHopStats(int bytes_per_packet, float avg_rate, float avg_rssi, int packet_cnt, int packet_retries,
			int channel, String rate_string) {
		this.bytes_per_packet = bytes_per_packet;
		this.avg_rate = avg_rate;
		this.avg_rssi = avg_rssi;
		this.packet_cnt = packet_cnt;
		this.packet_retries = packet_retries;
		this.channel = channel;
		this.rate_string = rate_string;
	}

	public int bytes_per_packet;
	public float avg_rate;
	public float avg_rssi;
	public int packet_cnt;
	public int packet_retries;
	public int channel;
	public String rate_string;
}
