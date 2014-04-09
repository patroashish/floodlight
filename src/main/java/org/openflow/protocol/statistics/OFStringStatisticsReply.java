/**
*    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
*    University
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package org.openflow.protocol.statistics;


import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.StringByteSerializer;

/**
 * Represents an ofp_util_stats_reply structure
 * @author Ashish Patro (patro@cs.wisc.edu)
 */
public class OFStringStatisticsReply implements OFStatistics {
	public static int STATION_STATS_LENGTH = 4096; //2048;
	public static int HIGHERLAYER_STATS_LENGTH = 1024;
	public static int UTILHOP_STATS_LENGTH = 256;
	
    protected String stationStatsString;
    protected String passiveStatsString;
    protected String beaconStatsString;
    protected String nonwifiStatsString;
    
	// Debug/Experiment related variables.
    protected String passiveHopStatsString;
    protected String higherlayerStatsString;
    protected String utilhopStatsString;
    
    public String getStationStatsString() {
		return stationStatsString;
	}

	public void setStationStatsString(String stationStatsString) {
		this.stationStatsString = stationStatsString;
	}

	public String getPassiveStatsString() {
		return passiveStatsString;
	}

	public void setPassiveStatsString(String passiveStatsString) {
		this.passiveStatsString = passiveStatsString;
	}

	public String getBeaconActivityString() {
		return beaconStatsString;
	}

	public void setBeaconActivityString(String beaconActivityString) {
		this.beaconStatsString = beaconActivityString;
	}

	public String getPassiveHopStatsString() {
		return passiveHopStatsString;
	}

	public void setPassiveHopStatsString(String passiveHopStatsString) {
		this.passiveHopStatsString = passiveHopStatsString;
	}

	public String getHigherlayerStatsString() {
		return higherlayerStatsString;
	}

	public void setHigherlayerStatsString(String higherlayerStatsString) {
		this.higherlayerStatsString = higherlayerStatsString;
	}

	public String getUtilhopStatsString() {
		return utilhopStatsString;
	}

	public void setUtilhopStatsString(String utilhopStatsString) {
		this.utilhopStatsString = utilhopStatsString;
	}

	public String getStatsString() {
		return stationStatsString;
	}

	public void setStatsString(String statsString) {
		this.stationStatsString = statsString;
	}
	
	public String getNonwifiStatsString() {
		return nonwifiStatsString;
	}

	public void setNonwifiStatsString(String nonwifiStatsString) {
		this.nonwifiStatsString = nonwifiStatsString;
	}

	@Override
    public int getLength() {
        return 22784; //9472;
    }

    @Override
    public void readFrom(ChannelBuffer data) {
    	this.stationStatsString = StringByteSerializer.readFrom(data,
    			STATION_STATS_LENGTH);
    	this.passiveStatsString = StringByteSerializer.readFrom(data,
    			STATION_STATS_LENGTH);
    	this.beaconStatsString = StringByteSerializer.readFrom(data,
    			STATION_STATS_LENGTH);
    	this.nonwifiStatsString = StringByteSerializer.readFrom(data,
    			HIGHERLAYER_STATS_LENGTH);
    	
    	this.passiveHopStatsString = StringByteSerializer.readFrom(data,
    			STATION_STATS_LENGTH * 2);
    	this.higherlayerStatsString = StringByteSerializer.readFrom(data,
    			HIGHERLAYER_STATS_LENGTH);
    	this.utilhopStatsString = StringByteSerializer.readFrom(data,
    			UTILHOP_STATS_LENGTH);
    }

    @Override
    public void writeTo(ChannelBuffer data) {
    	StringByteSerializer.writeTo(data, STATION_STATS_LENGTH,
                this.stationStatsString);
    	StringByteSerializer.writeTo(data, STATION_STATS_LENGTH,
                this.passiveStatsString);
    	StringByteSerializer.writeTo(data, STATION_STATS_LENGTH,
                this.beaconStatsString);
    	StringByteSerializer.writeTo(data, HIGHERLAYER_STATS_LENGTH,
                this.nonwifiStatsString);
    	
    	StringByteSerializer.writeTo(data, STATION_STATS_LENGTH,
                this.passiveHopStatsString);
    	StringByteSerializer.writeTo(data, HIGHERLAYER_STATS_LENGTH,
                this.higherlayerStatsString);
    	StringByteSerializer.writeTo(data, UTILHOP_STATS_LENGTH,
                this.utilhopStatsString);
    }

    @Override
    public int hashCode() {
        final int prime = 521;
        int result = 1;
        result = prime
                * result
                + ((stationStatsString == null) ? 0 : stationStatsString
                        .hashCode());
        result = prime
                * result
                + ((stationStatsString == null) ? 0 : passiveStatsString
                        .hashCode());
        result = prime
                * result
                + ((stationStatsString == null) ? 0 : beaconStatsString
                        .hashCode());
        
        result = prime
                * result
                + ((stationStatsString == null) ? 0 : nonwifiStatsString
                        .hashCode());
        
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OFStringStatisticsReply)) {
            return false;
        }
        
        OFStringStatisticsReply other = (OFStringStatisticsReply) obj;
        
        if (stationStatsString == null) {
            if (other.stationStatsString != null) {
                return false;
            }
        } else if (!stationStatsString.equals(other.stationStatsString)) {
            return false;
        }
        
        if (passiveStatsString == null) {
            if (other.passiveStatsString != null) {
                return false;
            }
        } else if (!passiveStatsString.equals(other.passiveStatsString)) {
            return false;
        }
        
        if (beaconStatsString == null) {
            if (other.beaconStatsString != null) {
                return false;
            }
        } else if (!beaconStatsString.equals(other.beaconStatsString)) {
            return false;
        }
        
        if (nonwifiStatsString == null) {
            if (other.nonwifiStatsString != null) {
                return false;
            }
        } else if (!nonwifiStatsString.equals(other.nonwifiStatsString)) {
            return false;
        }
        
        return true;
    }
}
