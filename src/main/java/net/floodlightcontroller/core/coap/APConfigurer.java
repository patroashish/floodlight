package net.floodlightcontroller.core.coap;

import java.io.IOException;

import org.openflow.protocol.OFSetWirelessConfig;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.annotations.LogMessageDoc;

public class APConfigurer {

	// Logger.
	protected static Logger log = 
			LoggerFactory.getLogger(APConfigurer.class);

	protected IFloodlightProviderService floodlightProvider;

	public APConfigurer(IFloodlightProviderService floodlightProvider)
	{
		this.floodlightProvider = floodlightProvider;		
	}

	public boolean SwitchChannel(int apId, int channel, String type, String canUse11n) {
		log.info("Switching channel of ap " + apId + " to channel" + channel + " type " + type);
		String command = "bash /etc/wah/switch_channel.sh " + channel + " " + type + " " + canUse11n;
		return SendCommand(apId, command);
	}
	
	public boolean ClearTDMA(int apId) {
		log.info("Resetting the channel for ap " + apId);
		String command = "bash /root/tdma_analysis/settdma_type.sh 1 x x; bash /root/tdma_analysis/settdma_type.sh 2 x x; bash /root/tdma_analysis/settdma_type.sh 3 10 0";
		return SendCommand(apId, command);
	}

	public boolean UpdateTxPower(int apId, int power) {
		log.info("Updating tx pow of ap " + apId + " to " + power);
		String command = "bash /etc/wah/update_txpow.sh " + power;
		return SendCommand(apId, command);
	}

	public boolean SetTDMAThrottle(int apId, int slotDuration, String transmitBitmap) {
		log.info("Setting throttle TDMA ap " + apId + " with slotDuration " + slotDuration +
				" transmitBitmap" + transmitBitmap);
		String command = "bash /root/tdma_analysis/settdma_type.sh 6 " + slotDuration + " " + transmitBitmap;
		return SendCommand(apId, command);
	}

	public boolean SetTDMASlotted(int apId, int slotDuration, String transmitBitmap) {
		log.info("Setting slotted TDMA ap " + apId + " with slotDuration " + slotDuration +
				" transmitBitmap" + transmitBitmap);
		String command = "bash /root/tdma_analysis/settdma_type.sh 6 " + slotDuration + " " + transmitBitmap;
		return SendCommand(apId, command);
	}

	@LogMessageDoc(level="ERROR",
			message="Failure retrieving reference from switch {switch}",
			explanation="An error occurred while retrieving reference" +
					"from the switch",
					recommendation=LogMessageDoc.CHECK_SWITCH + " " +
							LogMessageDoc.GENERIC_ACTION)
	public boolean SendCommand(int apId, String command)  {
		try
		{
			long switchId = CoapEngine.GetDpIdFromAP(apId);

			if (switchId >= 0)
			{
				log.info("SendCommand '" + command + "' for ap " + apId + " and switchId " + switchId);
				sendWirelessConfigRequest(switchId, command);
				return true;
			} else
			{
				log.warn("Warning!! Could not apply configuration '" + command + "' to ap_id " + apId +
						" because AP is disconnected");
				return false;
			}
		} catch (Exception ex) {
			log.error("Error while sending command '" + command + "' to ap " + apId);
			ex.printStackTrace();			
		}

		return false;
	}

	@LogMessageDoc(level="ERROR",
			message="Failure retrieving reference from switch {switch}",
			explanation="An error occurred while retrieving reference" +
					"from the switch",
					recommendation=LogMessageDoc.CHECK_SWITCH + " " +
							LogMessageDoc.GENERIC_ACTION)
	protected void sendWirelessConfigRequest(long switchId, String configMessage) {

		// TODO: Finalize stuff.
		//IOFSwitch sw = floodlightProvider.getSwitches().get(switchId);
		IOFSwitch sw = floodlightProvider.getAllSwitchMap().get(switchId);

		// TODO: Finalize the AP ID.

		//sw = getSwitchReference(switchId);
		log.info("Trying wireless config for AP with switchId " + switchId);

		if (sw != null)
		{
			OFSetWirelessConfig config = (OFSetWirelessConfig) OFType.WIRELESS_CONFIG.newInstance();
			log.info("Start sending config '" + configMessage + "' to switchid " + switchId);

			try {

				if (config != null)
				{
					config.setConfigString(configMessage);

					log.info("Sending config to AP " + switchId);
					sw.write(config, null);
					log.info("Sent config to AP " + switchId);
				} else
				{
					log.info("config is null. Not doing anything...");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		else 
		{
			log.info("Not doing anything due to null reference"); 
		}
	}
}
