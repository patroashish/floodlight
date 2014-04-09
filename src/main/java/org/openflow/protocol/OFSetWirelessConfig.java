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

package org.openflow.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.util.StringByteSerializer;

/**
 * Represents an OFPT_SET_WIRELESS_CONFIG type message
 * @author Ashish Patro (patro@cs.wisc.edu)
 */
public class OFSetWirelessConfig extends OFMessage {
	public static int CONFIG_STRING_LENGTH = 256;
    public static int MINIMUM_LENGTH = 264;

    public OFSetWirelessConfig() {
    	super();
    	
    	this.type = OFType.WIRELESS_CONFIG;
        super.setLengthU(MINIMUM_LENGTH);
        /*
        super();
        this.type = OFType.SET_CONFIG;
        */
    }

    protected String configString;
    
    public String getConfigString() {
		return configString;
	}

	public void setConfigString(String configString) {
		this.configString = configString;
	}

    @Override
    public void readFrom(ChannelBuffer data) {
        super.readFrom(data);
        
        this.configString = StringByteSerializer.readFrom(data,
    			CONFIG_STRING_LENGTH);
    }

    @Override
    public void writeTo(ChannelBuffer data) {
        super.writeTo(data);
        
        StringByteSerializer.writeTo(data, CONFIG_STRING_LENGTH,
                this.configString);
    }

    @Override
    public int hashCode() {
        final int prime = 331;
        int result = super.hashCode();
        
        result = prime
                * result
                + ((configString == null) ? 0 : configString
                        .hashCode());
        
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OFSetWirelessConfig)) {
            return false;
        }
        
        OFSetWirelessConfig other = (OFSetWirelessConfig) obj;
        if (configString == null) {
            if (other.configString != null) {
                return false;
            }
        } else if (!configString.equals(other.configString)) {
            return false;
        }
        return true;
    }
}
