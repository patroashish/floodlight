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

/**
 * Represents an ofp_util_stats_request structure
 * @author Ashish Patro (patro@cs.wisc.edu)
 */
public class OFStringStatisticsRequest implements OFStatistics {
    protected short type;

    /**
     * @return the type
     */
    public short getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(short type) {
        this.type = type;
    }

    @Override
    public int getLength() {
        return 4;
    }

    @Override
    public void readFrom(ChannelBuffer data) {
        this.type = data.readShort();
        data.readShort(); // pad
    }

    @Override
    public void writeTo(ChannelBuffer data) {
        data.writeShort(this.type);
        data.writeShort((short) 0); // pad
    }

    @Override
    public int hashCode() {
        final int prime = 523;
        int result = 1;
        result = prime * result + type;
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
        if (!(obj instanceof OFStringStatisticsRequest)) {
            return false;
        }
        OFStringStatisticsRequest other = (OFStringStatisticsRequest) obj;
        if (this.type != other.type) {
            return false;
        }
        return true;
    }
}
