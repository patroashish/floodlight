package net.floodlightcontroller.core.coap;

import java.util.ArrayList;
import java.util.Arrays;

public class CoapConstants {
	
	// Global config related constants.
	public static final boolean USE_POLICY_ENGINE = true; // Set true to enable dynamic configuration of APs.

	public static final boolean USE_DEBUG = true; // Setting for creating debug instances of COAP
	public static final boolean USE_TRAFFIC_GEN = false; // Setting for creating debug instances of COAP
	
	// Channel related information.
	public static final ArrayList<Integer> FREQ_LIST = new ArrayList<Integer>(Arrays.asList(2412, 2437, 2462));
	
	// Constants used for COAP.
	// SQL Query related constants.
	public static final String SERVER_HOSTNAME = "localhost";
	public static final int SERVER_PORT = 3306;

  // TODO: 0214 - Using a different database for experiments.
	public static final String DB_NAME = "wahdata_expts"; // TODO: testing the data commit
	public static final String CONFIG_DB_NAME = "wah"; // TODO: testing the data commit 
	public static final String DB_USER_NAME = "ashish"; // "root"; // TODO: Finalize
	public static final String DB_PASSWORD = "wingswifi";  // "699.tmp"; // TODO: Finalize
	
	public static String ROUTER_INFO_TABLE = "router_info";
	public static String NET_INFO_TABLE = "net_info";
	public static String WIRELESS_TABLE = "wireless";

	// 0223 - For demo
	public static String AIRSHARK_STATS_TABLE = USE_DEBUG ? "airshark_temp_table" : "airshark_stats_table";
	public static String BEACON_STATS_TABLE = "beacon_stats_table";
	public static String HIGHER_LAYER_TABLE = "higherlayer_info";
	public static String METRIC_TABLE = "metric_val";
	public static String PASSIVE_HOP_TABLE = "passive_hop_stats";
	public static String PASSIVE_TABLE = "passive_stats";
	public static String STATION_TABLE = "station_stats";
	public static String UTIL_TABLE = "util";
	public static String UTIL_HOP_TABLE = "utilhop";
	
	// 0222 - Reduced from 3 to 2.
	public static final int DB_COMMITTER_SLEEP_INTERVAL_MS = USE_DEBUG ? 3000 : 60000;
	
	public static final int DB_COMMITTER_DATA_TIMEWINDOW = DB_COMMITTER_SLEEP_INTERVAL_MS / 1000; // The number of preceding 10 second window which
																	// is used to commit data to a database. 
	
	// Util related constants.
	public static int UNK_NOISE_FLOOR = -120;
	public static int HIGH_NOISE_FLOOR_THRESHOLD = -85;
	
	//public static final int MAX_QUERIES = 1000;
	
	// COAP engine related constants
	// 0222 - Reduced from 3 to 2.
	public static final int DATA_POLL_FREQUENCY_MSEC = USE_DEBUG ? 3 * 1000 : 10 * 1000;
	
	// Policy engine related constants
	
	public static final String IS_CHANNEL_MODE_11N = USE_DEBUG ? "n" : "y";
	
	public static final double UTIL_CHANGE_THRESHOLD = USE_DEBUG ? 0.2 : 0.1;
	public static double DEFAULT_UTIL = 100.0;
	public static int UTIL_AVERAGE_INTERVAL_SEC = USE_DEBUG ? 15 : 60;
	public static int MIN_CHECKUTILS_ENTRIES = USE_DEBUG ? 1 : 3;
	
	public static int INMEMORY_DATA_INTERVAL_SEC = 1200;
	public static int DEFAULT_INACTIVE_DURATION_SEC = 1000;
	public static int INACTIVE_PACKET_COUNT_THESHOLD = USE_DEBUG ? 500 : 200;
	public static int INACTIVE_SEARCH_INTERVAL_SEC = USE_DEBUG ? 15 : 60;
	public static int MIN_CHANNEL_SWITCH_GAP_SEC = USE_DEBUG ? 20 : 300;

	public static int MIN_HOP_ACTIVE_DURATION_MS = 400;
	
	public static int DEFAULT_FREQ = 2400;
	
	// 0222 - Reduced from 5 to 3.
	public static int MITIGATION_SLEEP_MS = USE_DEBUG ? 5000 : 30000;
	
	// Message related constants
	public static String AIRSHARK_MSG = "airsharkstat";
	public static String BEACONSTATS_MSG = "beaconstats";
	public static String BEACON_MSG = "beacon";
	public static String HIGHERLAYER_MSG = "higherlayer";
	public static String MAC_MSG = "mac";
	public static String METRIC_MSG = "metric";
	public static String NONWIFI_MSG = "nonwifi";
	public static String PASSIVE_MSG = "passive";
	public static String PIE_MSG = "pie";
	public static String STATION_MSG = "stationstats";
	public static String UTIL_MSG = "util";
	public static String UTILHOP_MSG = "utilhop";
	
	public static int MAC_LENGTH = 17;
	
	public static int MESSAGE_QUEUE_LENGTH = 100;
	
	// Logging related constants.
	public static String OUTPUT_LOG_NAME = "output.log";
	public static String POLICYENGINE_LOG_NAME = "policy_output.log";
	public static int LOG_SIZE = 1000000;
	public static int LOG_ROTATION_COUNT = 1000000;
}
