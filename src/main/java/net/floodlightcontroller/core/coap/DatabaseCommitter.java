package net.floodlightcontroller.core.coap;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.PreparedStatement;

public class DatabaseCommitter implements Runnable {
	public static Connection conn;
	static Logger logger;

	public DatabaseCommitter() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
	
    // TODO 0214: Using this different table for experiments.
    String connection = "jdbc:mysql://localhost:3306/wahdata_expts?user=ashish&password=wingswifi";
    //String connection = "jdbc:mysql://localhost:3306/wahdata_expts?user=root&password=699.tmp";

    //String connection = "jdbc:mysql://128.105.22.232:3306/wahdata?user=ashish&password=wingswifi";
		try {
      //String connection = "jdbc:mysql://localhost:3306/wahdata?user=root&password=699.tmp";
      //String connection = "jdbc:mysql://localhost:3306/wahdata?user=root&password=lily1986";

      /*
      String connection = "jdbc:mysql://" + CoapConstants.SERVER_HOSTNAME + 
          ":" + CoapConstants.SERVER_PORT + "/" + CoapConstants.DB_NAME +
          "?user=" + CoapConstants.DB_USER_NAME + "&password=" + CoapConstants.DB_PASSWORD;
      */

			conn = DriverManager.getConnection(connection);
		} catch (SQLException e) {
			System.err.println("Exception for connection: " + connection + " Error: " + e.toString());
			System.exit(1);
		}
		
		logger = Logger.getLogger("query logger");
		logger.setLevel(Level.ALL);
	}
	
	public static int ExecuteQuery(String query) {
		try {
			Statement stat = conn.createStatement();
			int ret = stat.executeUpdate(query);
			if (ret < 0) {
				logger.log(Level.WARNING, String.format("query %s failed", query));
				return -1;
			}
		} catch (SQLException e) {
			Logger.getLogger("").log(Level.SEVERE, "DatabaseCommitter: query execute failed: " + e.getMessage());
			
			System.err.println("query execute failed " + e.toString());
			return -1;
		}
		
		return 0;
	}
	
	public static int ExecuteQuery(String queryFormat, ArrayList<ArrayList<Object>> params) {
		long ts = System.currentTimeMillis();
		PreparedStatement statement;
		try {
			conn.setAutoCommit(false);
			statement = conn.prepareStatement(queryFormat);
		} catch (SQLException e) {
			Logger.getLogger("").log(Level.SEVERE, "DatabaseCommitter: ExecuteQuery1 " + e.getMessage());
			
			e.printStackTrace();
			return -1;
		}
		
		for (ArrayList<Object> objectArray: params) {
			try {
				int i = 0;
				for (Object obj: objectArray) {
					i += 1;
					if (obj instanceof Integer) {
						statement.setInt(i, (Integer)obj);
					} else if (obj instanceof Float) {
						statement.setFloat(i, (Float)obj);
					} else if (obj instanceof Double) {
						statement.setDouble(i, (Double)obj);
					} else if (obj instanceof String) {
						statement.setString(i, (String)obj);
					} else if (obj instanceof Long) {
						statement.setLong(i, (Long)obj);
					}
				}
				
				statement.addBatch();
			} catch (SQLException ex) {
				Logger.getLogger("").log(Level.SEVERE, "DatabaseCommitter: ExecuteQuery2 " + ex.getMessage());
				
				ex.printStackTrace();
				return -1;
			}
		}
		
		if (params.size() > 0) {
			try {
				statement.executeBatch();
				conn.commit();
			} catch (SQLException e) {
				e.printStackTrace();
				try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				
				return -1;
			}
		}
		
		try {
			conn.setAutoCommit(true);
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		
		long ts2 = System.currentTimeMillis() - ts;
		System.out.println("Entries_prepared: " + params.size() + " Time: " + ts2 / 1000);
		return 0;
	}
	
	public static int ExecuteQuery(ArrayList<String> queries) {
		try {
			long ts = System.currentTimeMillis();
			
			Statement stat = conn.createStatement();
			for (String query: queries) {
				stat.addBatch(query);
			}
			
			int[] rets = stat.executeBatch();
			for (int i = 0; i < rets.length; ++i) {
				if (rets[i] < 0) {
					String query = queries.get(i);
					logger.log(Level.WARNING, String.format("query %s failed", query));
				}
			}
			
			long ts2 = System.currentTimeMillis()  - ts;
			
			System.out.println("Entries: " + queries.size() + " Time: " + ts2 / 1000);
		} catch (SQLException e) {
			Logger.getLogger("").log(Level.SEVERE, "DatabaseCommitter: ExecuteQuery3 " + e.getMessage());
			
			System.err.println("query execute failed " + e.toString());
			return -1;
		}
		
		return 0;
	}
	
	public static ResultSet ExecuteSelect(String query) {
		Statement stat;
		try {
			stat = conn.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		
		ResultSet resultSet = null;
		try {
			resultSet = stat.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return resultSet;
	}
	
	public static long interval_sec;
	@Override
	public void run() {
		while (true) {
			System.out.println("commiting to db ");
			try {
			  System.out.println("Sleeping for: " + CoapConstants.DB_COMMITTER_SLEEP_INTERVAL_MS + "ms");
				Thread.sleep(CoapConstants.DB_COMMITTER_SLEEP_INTERVAL_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// ClientWorker.beacon_parser.commit();
			long ts = System.currentTimeMillis();
			interval_sec = ts / 1000 - CoapConstants.DB_COMMITTER_DATA_TIMEWINDOW;
			/*synchronized(((BeaconStatsParser) ClientWorker.beacon_parser).beacon_map) {
				((BeaconStatsParser) ClientWorker.beacon_parser).beacon_map.clear();
			}*/
		
      boolean showLogs = false;

			try {
			CoapEngine.beacon_parser.commit(interval_sec);

      if (showLogs) {
  			System.out.println("Beacon stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }
			
			ts = System.currentTimeMillis();
			CoapEngine.higher_layer_parser.commit(interval_sec);
      
      if (showLogs) {
  			System.out.println("Higher stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }
			
			ts = System.currentTimeMillis();
			CoapEngine.metric_parser.commit(interval_sec);
      
      if (showLogs) {
			  System.out.println("Metric stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }
			
			ts = System.currentTimeMillis();
			CoapEngine.passive_parser.commit(interval_sec);
      
      if (showLogs) {
			  System.out.println("Passive stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }

			//ClientWorker.pie_parser.commit();
			
			ts = System.currentTimeMillis();
			CoapEngine.station_stats_parser.commit(interval_sec);
      
      if (showLogs) {
			  System.out.println("Station stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }
			
			ts = System.currentTimeMillis();
			CoapEngine.util_parser.commit(interval_sec);
      
      if (showLogs) {
			  System.out.println("Util stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }
			
			ts = System.currentTimeMillis();
			CoapEngine.util_hop_parser.commit(interval_sec);
      
      if (showLogs) {
			  System.out.println("Util hop stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }
			
			ts = System.currentTimeMillis();
			CoapEngine.passive_hop_parser.commit(interval_sec);
      
      if (showLogs) {
			  System.out.println("Passive hop stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }
			
			ts = System.currentTimeMillis();
			CoapEngine.nonwifi_parser.commit(interval_sec);
      
      if (showLogs) {
			  System.out.println("Nonwifi stat time: " + (System.currentTimeMillis() - ts) + "ms");
      }
			
			mx_ts = Math.max(CoapEngine.higher_layer_parser.get_maxts(), mx_ts);
			mx_ts = Math.max(CoapEngine.metric_parser.get_maxts(), mx_ts);
			mx_ts = Math.max(CoapEngine.util_hop_parser.get_maxts(), mx_ts);
			mx_ts = Math.max(CoapEngine.util_parser.get_maxts(), mx_ts);
			mx_ts = Math.max(CoapEngine.station_stats_parser.get_maxts(), mx_ts);
			mx_ts = Math.max(CoapEngine.passive_parser.get_maxts(), mx_ts);
			mx_ts = Math.max(CoapEngine.passive_hop_parser.get_maxts(), mx_ts);
			mx_ts = Math.max(CoapEngine.nonwifi_parser.get_maxts(), mx_ts);
			
			} catch (ConcurrentModificationException e) {
				Logger.getLogger("").log(Level.SEVERE, "DatabaseCommitter: run " + e.getMessage());
				
				e.printStackTrace();
				continue;
			}
			/*PolicyEngine.lock.lock();
			PolicyEngine.toRead = true;
			PolicyEngine.lock.unlock();*/
		}
	}
	
	public static long mx_ts = 0;
}
