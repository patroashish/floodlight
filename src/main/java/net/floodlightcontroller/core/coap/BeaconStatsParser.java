package net.floodlightcontroller.core.coap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
//import java.util.logging.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

class BeaconStat
{
	public double avg_rssi;
	public int channel;
	
	public BeaconStat(double avg_rssi, int channel)
	{
		this.avg_rssi = avg_rssi;
		this.channel = channel;
	}
}

public class BeaconStatsParser implements Parser {
	String trim_string(String str, int len) {
		String tmp = new String(str);
		for (int i = 0; i < len - 1; ++i) {
			tmp = tmp.replaceAll("^0", "");
		}
		
		return tmp;
	}
	
	@Override
	public void process(String rest, int ap_id) {
		if (ap_id <= 0) {
			return;
		}
		
		int cnt = Integer.parseInt(trim_string(rest.substring(0, 3), 3));
		if (cnt == 0) {
			return;
		}

		rest = rest.substring(4);
		String[] terms = rest.split(";");
		assert(cnt == terms.length);
		for (int i = 0; i < cnt; ++i) {
			String curr = terms[i];
			String[] curr_terms = curr.split(" ");
			
			if (curr_terms.length < 4) 
			{
				System.err.println("Beacon stats warning!!! Got improper input: " + curr);
        
        try
        {
          PrintWriter out = new PrintWriter(new FileOutputStream(new File("bad_beacon.txt"), true));
          out.println("Bad " + ap_id + " : " + curr);
          out.close();
        } catch (FileNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        continue;
			}
			
			String ap = curr_terms[0];
			Double rssi = Double.parseDouble(curr_terms[1]);
			long sec = Long.parseLong(curr_terms[2]);
			int channel = Integer.parseInt(curr_terms[3]);
			
			updateMap(ap_id, sec, ap, new BeaconStat(rssi, channel));
		}
	}

	@Override
	public void updateMap(int ap_id, Long sec, String client_mac, Object o) {
		//lock.lock();
		//synchronized (beacon_map) {
		if (!beacon_map.containsKey(ap_id)) {
			beacon_map.put(ap_id, new HashMap<Long, HashMap<String, BeaconStat>>());
		}

		if (!beacon_map.get(ap_id).containsKey(sec)) {
			beacon_map.get(ap_id).put(sec, new HashMap<String, BeaconStat>());
		}

		beacon_map.get(ap_id).get(sec).put(client_mac, (BeaconStat)o);
		mx_ts = Math.max(mx_ts, sec);
		//}
		//lock.unlock();
	}
	
	@Override
	public synchronized Object getHashMap() {
		synchronized (beacon_map) {
			return beacon_map;
		}
	}
	
	public HashMap<Integer, HashMap<Long, HashMap<String, BeaconStat> > > beacon_map = 
			new HashMap<Integer, HashMap<Long, HashMap<String, BeaconStat> > >();
	public Lock lock = new ReentrantLock();
	
	@Override
	public void commit(long ts_limit) {
		String queryFormat = "Insert into " + CoapConstants.BEACON_STATS_TABLE + " VALUES(?, ?, ?, ?, ?)";
		
		//ArrayList<String> queries = new ArrayList<String>();
		//System.out.println("commiting beacon stats");
		
    //lock.lock();
		//synchronized (beacon_map) {
		@SuppressWarnings("unchecked")
		HashMap<Integer, HashMap<Long, HashMap<String, BeaconStat> > > beacon_stats_map = 
			(HashMap<Integer, HashMap<Long, HashMap<String, BeaconStat> > >)getHashMap();
		@SuppressWarnings("rawtypes")
		Iterator it = beacon_stats_map.entrySet().iterator();
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
			HashMap<Long, HashMap<String, BeaconStat> > tim_hashmap = 
				(HashMap<Long, HashMap<String, BeaconStat> >)pairs.getValue();
			
			@SuppressWarnings("rawtypes")
			Iterator tim_it = tim_hashmap.entrySet().iterator();
			while (tim_it.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry tim_pairs = (Map.Entry)tim_it.next();
				Long sec = (Long)tim_pairs.getKey();
				/*if (sec > ts_limit || queries.size() > 100) {
					dontRemove = true;
					if (queries.size() > 100) {
						break;
					}
					
					continue;
				}*/

				@SuppressWarnings("unchecked")
				HashMap<String, BeaconStat> client_map = 
					(HashMap<String, BeaconStat>)tim_pairs.getValue();
				@SuppressWarnings("rawtypes")
				Iterator client_it = client_map.entrySet().iterator();
				
				while (client_it.hasNext()) {
					@SuppressWarnings("rawtypes")
					Map.Entry client_pairs = (Map.Entry)client_it.next();
					String client_mac = (String)client_pairs.getKey();
					BeaconStat stat = (BeaconStat)client_pairs.getValue();

					// construct query and dump it
					/*String query = String.format("Insert into beacon_stats_table VALUES(%d, '%s', %d, %f)", 
							ap_id, client_mac, sec, rssi);
					queries.add(query);*/
					// ret = DatabaseCommitter.ExecuteQuery(query);
					/*if (ret < 0) {
						DatabaseCommitter.logger.log(Level.WARNING, String.format("query execution %s to mysql for beacon stats failed", query));
						System.err.println(String.format("query execution %s to mysql for beacon stats failed", query));
						dontRemove_ts = true;
						dontRemove_ap = true;*/
						//dontRemove = true;
					/*} else {
						//client_it.remove();
					}*/
					ArrayList<Object> objArray = new ArrayList<Object>();
					objArray.add(ap_id);
					objArray.add(client_mac);
					objArray.add(sec);
					objArray.add(stat.avg_rssi);
					objArray.add(stat.channel); // TODO
					params.add(objArray);
					client_it.remove();
				}
				
				tim_it.remove();
			}

			if (!dontRemove) {
				it.remove(); // avoids a ConcurrentModificationException
			}
		}
		
		//DatabaseCommitter.ExecuteQuery(queries);
		DatabaseCommitter.ExecuteQuery(queryFormat, params);
		//System.out.println("sz = " + queries.size());
		//beacon_map.clear();
		
		//lock.unlock();
		//System.out.println("done commiting beacon stats");
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
