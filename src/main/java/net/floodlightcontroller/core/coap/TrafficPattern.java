package net.floodlightcontroller.core.coap;

// Represents a traffic pattern class
class TrafficPattern
{
	private double sessionMaxSpeedMbps, sessionDurationSec; //sessionBytes;

	public String client;
	public int trafficDstPort;
	public long ts;
	
	//private double onPeriodDuration, onPeriodSpeedMbps;

	public double getSessionSpeedMbps() {
		return sessionMaxSpeedMbps;
	}

	public void setSessionSpeedMbps(double sessionMaxSpeedMbps) {
		this.sessionMaxSpeedMbps = sessionMaxSpeedMbps;
	}

	public double getSessionDurationSec() {
		return sessionDurationSec;
	}
	
	public double getRemainingSessionDurationSec() {
		return (ts + sessionDurationSec) - (System.currentTimeMillis() / 1000);
	}

	public void setSessionDurationSec(double sessionDurationSec) {
		this.sessionDurationSec = sessionDurationSec;
	}

	/*
	public double getSessionBytes() {
		return sessionBytes;
	}

	public void setSessionBytes(double sessionBytes) {
		this.sessionBytes = sessionBytes;
	}
	 */

	//private double onPeriodSpeedMbps, onPeriodBytes;

	public TrafficPattern(TrafficPattern another) {
		this.sessionMaxSpeedMbps = another.sessionMaxSpeedMbps;
		this.sessionDurationSec = another.sessionDurationSec;
	}
	
	public void SetMaxValues(TrafficPattern another) {
		this.sessionMaxSpeedMbps = 
				Math.max(this.sessionMaxSpeedMbps, another.sessionMaxSpeedMbps);
		this.sessionDurationSec = 
				Math.max(this.sessionDurationSec, another.sessionDurationSec);
	}

	public TrafficPattern()
	{
		sessionMaxSpeedMbps = 0.0;
		sessionDurationSec = 0.0;
		//sessionBytes = 0.0;

		/*
		onPeriodSpeedMbps = 0.0;
		onPeriodBytes = 0.0;
		 */
	}

	public TrafficPattern(double sessionSpeedMbps, double sessionDurationSec) //, double sessionBytes)
	//double onPeriodSpeedMbps, double onPeriodBytes)
	{
		this.sessionMaxSpeedMbps = sessionSpeedMbps;
		this.sessionDurationSec = sessionDurationSec;
		//this.sessionBytes = sessionBytes;

		/*
		this.onPeriodSpeedMbps = onPeriodSpeedMbps;
		this.onPeriodBytes = onPeriodBytes;
		 */
	}
}
