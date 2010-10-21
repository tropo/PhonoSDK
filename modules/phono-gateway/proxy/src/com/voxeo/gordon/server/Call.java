package com.voxeo.gordon.server;

import javax.servlet.sip.SipSession;
import com.voxeo.servlet.xmpp.XmppSession;

public class Call {
	public String initiator;
	public String jingleSessionId;
	public String xmppAddress;
	public String sipAddress;
	public XmppSession xmppSession;
	public SipSession sipSession;
	public Relay relay;
	
	public Call(String initiator)
	{
		this.initiator = initiator;
	}
}
