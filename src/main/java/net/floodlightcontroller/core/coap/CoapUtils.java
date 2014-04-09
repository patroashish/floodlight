package net.floodlightcontroller.core.coap;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CoapUtils {

  /*
     public static HashMap<Long, Integer> AP_ID_MAP = new HashMap<S, Integer>();

     static
     {
     AP_ID_MAP = new HashMap<Long, Integer>();
     AP_ID_MAP.put(150867985271L, 101);
     AP_ID_MAP.put(150867721325L, 102);
     }
     */

  public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";	

  public static String CurrentTime() {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
    return sdf.format(cal.getTime());
  }

  public static int GetApIdFromRemoteIp(String ipString)
  {
    return Integer.parseInt(ipString.split(":")[0].split("\\.")[3]) + 1000;
  }

  public static int GetChannelFromFreq(int frequency) {
    return (frequency - 2407) / 5;
  }

  public static int GetFrequencyFromChannel(int channel) {
    return channel * 5 + 2407;
  }

  public static int GetApId(String addr, String filepath, boolean useDb) {
    try {
      if (useDb) {
        return GetApIdDb(addr);
      } else {
        return GetApIdFile(addr, filepath);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return -1;
  }

  public static int GetApIdFile(String addr, String filepath) {
    try {
      FileInputStream input = new FileInputStream(filepath);
      DataInputStream in = new DataInputStream(input);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String str;
      while ((str = br.readLine()) != null) {
        String[] terms = str.split(" ");

        //System.out.println(terms[0] + " " + addr);
        if (terms[0].equals(addr)) {
          return Integer.parseInt(terms[1]);
        }
      }
    } catch (FileNotFoundException e) {
      return -1;
    } catch (IOException e) {
      return -1;
    }

    return -1;
  }

  public static int GetApIdDb(String addrx) {
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e1) {
      e1.printStackTrace();
      System.exit(-1);
    }

    int ap_id = -1;
    Connection conn = null;

    try {
      conn = DriverManager.getConnection("jdbc:mysql://" + CoapConstants.SERVER_HOSTNAME + 
          ":" + CoapConstants.SERVER_PORT + "/" + CoapConstants.CONFIG_DB_NAME +
          "?user=" + CoapConstants.DB_USER_NAME + "&password=" + CoapConstants.DB_PASSWORD);

      if (conn == null) {
        return ap_id;
      }

      Statement stmt = conn.createStatement();

      ResultSet rs = stmt.executeQuery( "SELECT routerid FROM " + CoapConstants.NET_INFO_TABLE + 
          " WHERE mac = '" + addrx.toUpperCase().replace(':', '-') + "'" );
      try {
        while ( rs.next() ) {
          /*
             int numColumns = rs.getMetaData().getColumnCount();
             for ( int i = 1 ; i <= numColumns ; i++ ) {
          // Column numbers start at 1.
          // Also there are many methods on the result set to return
          //  the column as a particular type. Refer to the Sun documentation
          //  for the list of valid conversions.
          System.out.println( "COLUMN " + i + " = " + rs.getObject(i) );
          }*/

          // Return the first known valid AP ID
          ap_id = rs.getInt(1);
          break;
        }

        rs.close();

      } catch (Exception exquery) {
        exquery.printStackTrace();
      }

      try {
        stmt.close();
      } catch (SQLException exstmt) {
        exstmt.printStackTrace();
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }

    if (conn != null) {
      try {
        conn.close();
        conn = null;
      } catch (SQLException exn) { exn.printStackTrace();}
    }

    return ap_id;
  }

  public static String GetAddressFromApDb(int ap_id) {
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e1) {
      e1.printStackTrace();
      System.exit(-1);
    }

    String address = null;
    Connection conn = null;

    try {
      conn = DriverManager.getConnection("jdbc:mysql://" + CoapConstants.SERVER_HOSTNAME + 
          ":" + CoapConstants.SERVER_PORT + "/" + CoapConstants.CONFIG_DB_NAME +
          "?user=" + CoapConstants.DB_USER_NAME + "&password=" + CoapConstants.DB_PASSWORD);

      if (conn == null) {
        return address;
      }

      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery( "SELECT ipaddr FROM " + CoapConstants.ROUTER_INFO_TABLE + 
          " WHERE routerid = " + ap_id);
      try {
        while ( rs.next() ) {
          /*
             int numColumns = rs.getMetaData().getColumnCount();
             for ( int i = 1 ; i <= numColumns ; i++ ) {
          // Column numbers start at 1.
          // Also there are many methods on the result set to return
          //  the column as a particular type. Refer to the Sun documentation
          //  for the list of valid conversions.
          System.out.println( "COLUMN " + i + " = " + rs.getObject(i) );
          }*/

          // Return the first known valid AP ID
          address = rs.getString(1);
          break;
        }

        rs.close();

      } catch (Exception exquery) {
        exquery.printStackTrace();
      }

      try {
        stmt.close();
      } catch (SQLException exstmt) {
        exstmt.printStackTrace();
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    
    if (conn != null) {
      try {
        conn.close();
        conn = null;
      } catch (SQLException exn) { exn.printStackTrace();}
    }

    return address;
  }

  public static boolean IsAutomaticAllowed(int ap_id, int net_id) {
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e1) {
      e1.printStackTrace();
      System.exit(-1);
    }

    boolean isAutoAllowed = false;
    Connection conn = null;

    try {
      conn = DriverManager.getConnection("jdbc:mysql://" + CoapConstants.SERVER_HOSTNAME + 
          ":" + CoapConstants.SERVER_PORT + "/" + CoapConstants.CONFIG_DB_NAME +
          "?user=" + CoapConstants.DB_USER_NAME + "&password=" + CoapConstants.DB_PASSWORD);

      // Default to false.
      if (conn == null) {
        return isAutoAllowed;
      }

      Statement stmt = conn.createStatement();

      ResultSet rs = stmt.executeQuery( "SELECT manual FROM " + CoapConstants.WIRELESS_TABLE + 
          " WHERE routerid = " + ap_id + " and netid = " + net_id);
      try {
        while ( rs.next() ) {
          // return whether automatic is allowed.
          isAutoAllowed = (rs.getInt(0) == 0);
          break;
        }

        rs.close();

      } catch (Exception exquery) {
        exquery.printStackTrace();
      }

      try {
        stmt.close();
      } catch (SQLException exstmt) {
        exstmt.printStackTrace();
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    
    if (conn != null) {
      try {
        conn.close();
        conn = null;
      } catch (SQLException exn) { exn.printStackTrace();}
    }

    return isAutoAllowed;
  }
}
