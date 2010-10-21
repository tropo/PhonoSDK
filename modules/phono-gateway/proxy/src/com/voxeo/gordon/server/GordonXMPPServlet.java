package com.voxeo.gordon.server;

import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SessionName;
import javax.sdp.Connection;

import java.io.IOException;
import java.util.Vector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;

import javax.annotation.*;

import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppSession;
import com.voxeo.servlet.xmpp.XmppServlet;
import com.voxeo.servlet.xmpp.XmppServletIQRequest;
import com.voxeo.servlet.xmpp.XmppServletIQResponse;
import com.voxeo.servlet.xmpp.XmppServletStanzaRequest;
import com.voxeo.servlet.xmpp.XmppServletStreamRequest;
import com.voxeo.servlet.xmpp.XmppStanzaError;
import com.voxeo.servlet.xmpp.sasl.SASLAuthRequest;
import com.voxeo.servlet.xmpp.sasl.SASLRequest;
import com.voxeo.servlet.xmpp.XmppServletStreamErrorRequest;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sdp.SessionName;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.util.*;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;


public class GordonXMPPServlet extends XmppServlet {

	private static final Logger LOG = Logger.getLogger(GordonXMPPServlet.class);

	private XmppFactory _xmppFactory;
	private SipFactory _sipFactory;
	private String _rtmpdUri;
	
	private Map<String, XmppSession> _clientSessions;	
	private Map<String, XmppSession> _serverSessions;
	
	@Resource 
	SdpFactory _sdpFactory;

	@Override
	public void init() throws ServletException {
		// Get a reference to the XMPPFactory and SIPFactory
		_xmppFactory = (XmppFactory) getServletContext().getAttribute(XmppServlet.XMPP_FACTORY);
		_sipFactory = (SipFactory) getServletContext().getAttribute(SipServlet.SIP_FACTORY);
		_clientSessions = new ConcurrentHashMap<String, XmppSession>();
		_serverSessions = new ConcurrentHashMap<String, XmppSession>();
		
		// XXX Store a link to the client sessions in the servlet context 
		getServletContext().setAttribute("xmppSessions", _clientSessions);
		
		LOG.debug("Gordon: XMPP init");
		
		// Read the config to determine the rtmp relay server address
	    _rtmpdUri = getServletConfig().getInitParameter("rtmpdUri");
	    LOG.debug("Configuration says RTMP relay is at " + _rtmpdUri);
	}

	@Override
	public void destroy() {
		super.destroy();

	}

	@Override
	protected void doMessage(XmppServletStanzaRequest req) throws ServletException, IOException {
		LOG.debug("XMPP:Received message:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
		
		Element messageElement = DocumentHelper.createElement("message");
                
        if (req.getElement().attribute("type") != null) {
          messageElement.addAttribute("type", req.getElement().attribute("type").getValue());
        }
        if (req.getElement().element("body") != null) {
          messageElement.addElement("body").setText(req.getElement().element("body").getText());
        }
        if (req.getElement().element("subject") != null) {
          messageElement.addElement("subject").setText(req.getElement().element("subject").getText());
        }
        if (req.getElement().element("thread") != null) {
          messageElement.addElement("thread").setText(req.getElement().element("thread").getText());
        }
		
        // Lets look up the destination and forward it
		XmppSession session = req.getSession();
		XmppSession destSession;
		if (session.getSessionType() == XmppSession.SessionType.CLIENT) {
			// It's coming from a client
	    	JID fromJid = session.getRemoteJID();
	    	JID toJid = _xmppFactory.createJID(req.getElement().attribute("to").getValue());
	    	messageElement.addAttribute("from", fromJid.toString());
	        messageElement.addAttribute("to", toJid.toString());
	        
	        // Get a handle to the server session and forward it
	        _xmppFactory.createStanzaRequest(toJid, messageElement, fromJid);
	    } else if (session.getSessionType() == XmppSession.SessionType.SERVER) {
	    	// It's coming from a server
	    	// Lookup the destination session
	    	 destSession = _clientSessions.get(req.getElement().attribute("to").getValue());
	    	 messageElement.addAttribute("from", req.getElement().attribute("from").getValue());
	    	 destSession.createStanzaRequest(messageElement, null, null, null, null, null).send();
	    }
	}

	@Override
	protected void doPresence(XmppServletStanzaRequest req) throws ServletException, IOException {
		LOG.debug("XMPP:Received presence:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
	}

	@Override
	protected void doIQRequest(XmppServletIQRequest req) throws ServletException, IOException {
		LOG.debug("XMPP:Received iq request:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
		if (!req.getElement().elements(new QName(Jingle.ELEMENT_NAME, new Namespace("",Jingle.NAMESPACE_URI))).isEmpty()) doJingleRequest(req);
	}

	@Override
	protected void doIQResponse(XmppServletIQResponse req) throws ServletException, IOException {
		LOG.debug("XMPP:Received iq response:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
		if (!req.getElement().elements("jingle").isEmpty()) doJingleResponse(req);
	}

	@Override
	protected void doSASLAbort(SASLRequest req) throws ServletException, IOException {
		LOG.debug("XMPP:Received doSASLAbort:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
	}

	@Override
	protected void doSASLAuth(SASLAuthRequest req) throws ServletException, IOException {
		LOG.debug("Received doSASLAuth:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
	}

	@Override
	protected void doSASLChallenge(SASLRequest req) throws ServletException, IOException {
		LOG.debug("Received doSASLChallenge:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
	}

	@Override
	protected void doSASLFailure(SASLRequest req) throws ServletException, IOException {
		LOG.debug("Received doSASLFailure:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
	}

	@Override
	protected void doSASLResponse(SASLRequest req) throws ServletException, IOException {
		LOG.debug("Received doSASLResponse:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
	}

	@Override
	protected void doSASLSuccess(SASLRequest req) throws ServletException, IOException {
		LOG.debug("Received doSASLSuccess:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
	}

	@Override
	protected void doStreamEnd(XmppServletStreamRequest req) throws ServletException, IOException {
		LOG.debug("received doStreamEnd:" + req.getElement().asXML() + "session id:" + req.getSession().getId());
		
		XmppSession session = req.getSession();
    	JID jid = session.getRemoteJID();
	    if (session.getSessionType() == XmppSession.SessionType.CLIENT) {
	    	_clientSessions.remove(jid.toString());	      
	    	SipApplicationSession appSession = session.getApplicationSession();
	    	session.unbind();
	    } else if (session.getSessionType() == XmppSession.SessionType.SERVER) {
	    	_clientSessions.remove(jid.getDomain().toString());	      
	    }
	}

	@Override
	protected void doStreamError(XmppServletStreamErrorRequest req) throws ServletException, IOException {
		LOG.debug("Received doStreamError:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
	}

	@Override
	protected void doStreamStart(XmppServletStreamRequest req) throws ServletException, IOException {
		LOG.debug("Received doStreamStart:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
		XmppSession session = req.getSession();
		JID jid = session.getRemoteJID();
	    if (session.getSessionType() == XmppSession.SessionType.CLIENT) {
	    	_clientSessions.put(jid.toString(), session);
	    	SipApplicationSession appSession = session.getApplicationSession();
	    	session.bind(appSession);
	    } else if (session.getSessionType() == XmppSession.SessionType.SERVER) {
	    	_serverSessions.put(jid.getDomain(), session);
	    }
	}

	// Do echo, parse the received message and send back the same message.
	private void doEcho(XmppServletStanzaRequest req) throws IOException {
		if (req.getTo().getDomain().equalsIgnoreCase(req.getSession().getLocalJID().getDomain())) {
			Element echoElement = DocumentHelper.createElement("message");

			XmppServletStanzaRequest echoReq = _xmppFactory.createStanzaRequest(req.getSession(), echoElement);
			echoReq.setAttribute("from", "test@example.com");
			echoReq.send();
		}
	}

	/**
	 * <iq to="neils@192.168.0.10" id="733C8206-AA72-5E7F-F5CB-FA017B47CC4A" type="set">
	 *
	  <jingle xmlns="urn:xmpp:jingle:1" action="session-initiate" initiator="user1@vipadia.com/voxeo" sid="1234">
	    <content creator="initiator" name="voice" senders="both">
	      <description xmlns="urn:xmpp:jingle:apps:rtp:1" media="audio">
	        <payload-type id="110" name="speex" clockrate="8000"/>
	      </description>
	      <transport xmlns="urn:xmpp:jingle:transports:raw-udp:1">
	        <candidate id="1234" component="1" generation="0" ip="192.168.0.10" port="3002"/>
	      </transport>
	    </content>
	  </jingle>
	</iq>
	 */

	private void doJingleRequest(XmppServletIQRequest req) throws ServletException, IOException
	{
		LOG.debug("Received jingle request:" + req.getElement().asXML());
		Element jgl = req.getElement().element("jingle");
		String action = jgl.attribute("action").getData().toString();
		String mediaAddress = "";
		String mediaPort = "";
		String codec = "";
		String pt = "";
		String clockrate = "";
		Relay relay = null;
		
		if (action.equals(Jingle.ACTION_SESSION_TERMINATE)) 
		{
			// This is a request to hang up an existing call
			
			// Acknowledge the request
			XmppServletStanzaRequest iq = buildReply(req);
			iq.send();

			// Find the call
			Call theCall = (Call)req.getSession().getAttribute("Call-"+req.getElement().element("jingle").attributeValue("sid"));
			if (theCall != null) {
			
				// Generate a BYE
				SipServletRequest sipreq = theCall.sipSession.createRequest("BYE");
				sipreq.send();
			
				// Free any relay that has been allocated
				if (theCall.relay != null) {
					theCall.relay.Delete();
					theCall.relay = null;
				}			
			}
		}		
		else if (action.equals(Jingle.ACTION_SESSION_INFO)) 
		{
			// This may be ringing or dtmf

			// Acknowledge the request
			XmppServletStanzaRequest iq = buildReply(req);
			iq.send();

			// Find the call
			Call theCall = (Call)req.getSession().getAttribute("Call-"+req.getElement().element("jingle").attributeValue("sid"));
			
			if (theCall != null && req.getElement().element("jingle").element("dtmf") != null) {
				// Generate an INFO	
				SipServletRequest sipreq = theCall.sipSession.createRequest("INFO");
				String content="Signal="+req.getElement().element("jingle").element("dtmf").attributeValue("code");
				if (req.getElement().element("jingle").element("dtmf").attributeValue("duration") != null)
					content += "\r\nDuration="+req.getElement().element("jingle").element("dtmf").attributeValue("duration");
				sipreq.setContent(content,"application/dtmf-relay");
				sipreq.send();
			}
		}
		else if (action.equals(Jingle.ACTION_CONTENT_ACCEPT))
		{
			// Acknowledge the request
			XmppServletStanzaRequest iq = buildReply(req);
			iq.send();
			
			// Find the call
			Call theCall = (Call)req.getSession().getAttribute("Call-"+req.getElement().element("jingle").attributeValue("sid"));
			if (theCall != null) {
			
				// Generate an OK with some SDP
				SipServletRequest sipreq = theCall.sipSession.createRequest("OK");
				
				relay = theCall.relay;
				if (relay != null) {
					mediaAddress = relay.localHost;
					mediaPort = relay.localPort;
					codec = relay.codec;
					pt = relay.payloadType;
				} else {
					LOG.error("Error allocating relay - sending an error back to the client.");
				}
				
				// Create some SDP
				try {
					SessionDescription sdp = _sdpFactory.createSessionDescription();

					SessionName s = _sdpFactory.createSessionName("-");
					Connection c = _sdpFactory.createConnection("IN","IP4", mediaAddress);

					int formats[] = {Integer.parseInt(pt),};
					MediaDescription am = _sdpFactory.createMediaDescription(
							"audio", Integer.parseInt(mediaPort), 1, "RTP/AVP",
							formats);
					am.setAttribute("rtpmap", pt + " " + codec + "/" + clockrate);
					am.setAttribute("ptime","40");

					Vector mediaDescs = new Vector();
					mediaDescs.add(am);
					sdp.setConnection(c);
					sdp.setSessionName(s);

					sdp.setMediaDescriptions(mediaDescs);

					sipreq.setContent(sdp, "application/SDP");

					// send the ok
					sipreq.send();
				} catch (Exception e) {
					LOG.error(e);
				}	 
			}
		}
		else if (action.equals(Jingle.ACTION_SESSION_INITIATE)) 
		{
			// Acknowledge the request
			XmppServletStanzaRequest iq = buildReply(req);
			iq.send();
			
			

			// This is a new call request from the client

			// We support 2 types of transport - standard rtp and our own rtmp
			Element description = req.getElement().element("jingle").element("content").element("description");
			if (description.getNamespaceURI() == Jingle.DESCRIPTION_RTMP_NAMESPACE_URI) {			
				// We allocate the relay using REST
				relay = new Relay(_rtmpdUri);
				if (relay != null) {
					mediaAddress = relay.localHost;
					mediaPort = relay.localPort;
					codec = relay.codec;
					pt = relay.payloadType;
				} else {
					LOG.error("Error allocating relay - sending an error back to the client.");
				}

			} else {
				// We assume it's rtp
				mediaAddress = req.getElement().element("jingle").element("content").element("transport").element("candidate").attributeValue("ip");
				mediaPort = req.getElement().element("jingle").element("content").element("transport").element("candidate").attributeValue("port");
				codec = req.getElement().element("jingle").element("content").element("description").element("payload-type").attributeValue("name");
				pt = req.getElement().element("jingle").element("content").element("description").element("payload-type").attributeValue("id");
				clockrate = req.getElement().element("jingle").element("content").element("description").element("payload-type").attributeValue("clockrate");				
			}

			
			// Check that this is a SIP call, if not we should route elsewhere
			if (req.getTo().getDomain().contentEquals("sip") || req.getTo().getDomain().contentEquals("app")) {
				// Build us a SIP invite
				SipApplicationSession appSession = _sipFactory.createApplicationSession();
				
				Address to, from;
				
				if (req.getTo().getDomain().contentEquals("app"))
					to = _sipFactory.createAddress("sip:" + unescapeNode(req.getTo().getNode()) + "@sip.voxeo.com");
				else
					to = _sipFactory.createAddress("sip:" + unescapeNode(req.getTo().getNode()));
				from = _sipFactory.createAddress("sip:" + req.getElement().element("jingle").attributeValue("initiator"));

				LOG.info("Sending invite from " + from + " to " + to);

				// create an INVITE request to the first party from the second
				SipServletRequest sipreq = _sipFactory.createRequest(appSession, "INVITE", from, to);

				// Create a call object to track the state of this call
				Call newCall = new Call(req.getElement().element("jingle").attributeValue("initiator"));
				if (relay != null) newCall.relay = relay;
				newCall.sipSession = sipreq.getSession();
				newCall.xmppSession = req.getSession();
				newCall.xmppAddress = req.getFrom().toString();
				newCall.sipAddress = req.getTo().toString();
				newCall.jingleSessionId = req.getElement().element("jingle").attributeValue("sid");

				// Store the call object in each session so we can find it again
				req.getSession().setAttribute("Call-" + req.getElement().element("jingle").attributeValue("sid"), newCall);
				sipreq.getSession().setAttribute("Call", newCall);

				// Add custom headers if we have some			
				for (Object customObj : req.getElement().element("jingle").elements("custom-header")) {
					Element custom = (Element)customObj;
					sipreq.addHeader(custom.attributeValue("name"), custom.attributeValue("data"));					
				} 

				// Create some SDP
				try {
					SessionDescription sdp = _sdpFactory.createSessionDescription();

					SessionName s = _sdpFactory.createSessionName("-");
					Connection c = _sdpFactory.createConnection("IN","IP4", mediaAddress);

					int formats[] = {Integer.parseInt(pt),};
					MediaDescription am = _sdpFactory.createMediaDescription(
							"audio", Integer.parseInt(mediaPort), 1, "RTP/AVP",
							formats);
					am.setAttribute("rtpmap", pt + " " + codec + "/" + clockrate);
					am.setAttribute("ptime","40");

					Vector mediaDescs = new Vector();
					mediaDescs.add(am);
					sdp.setConnection(c);
					sdp.setSessionName(s);

					sdp.setMediaDescriptions(mediaDescs);

					sipreq.setContent(sdp, "application/SDP");

					// send the invite
					sipreq.send();
				} catch (Exception e) {
					LOG.error(e);
				}	 
			} else {
				LOG.debug("Destination is not SIP");
				
				Element resElement = DocumentHelper.createElement("iq");
	    	    resElement.addAttribute("type", "set");
	    	    resElement.addAttribute("from", req.getTo().toString());
	    	    resElement.addAttribute("to", req.getFrom().toString());
	    	    resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI)))
	    	    	.addAttribute("action", Jingle.ACTION_SESSION_TERMINATE)
	    	    	.addAttribute("initiator", req.getElement().element("jingle").attributeValue("initiator"))
	    	    	.addAttribute("sid", req.getElement().element("jingle").attributeValue("sid"))
	    	    	.addElement(new QName("reason")).addElement(new QName("incompatible-parameters"));
	    	    iq = _xmppFactory.createStanzaRequest(req.getSession(), resElement);
	    	    
	    	    iq.send();
	    	    
	    	    // Free any relay associated with this call
	    	    relay.Delete();
	    	    relay = null;
			}
		}		
	}

	private void doJingleResponse(XmppServletIQResponse req) throws ServletException, IOException {
		LOG.debug("Received jingle response:" + req.getElement().asXML());
	}

	// Utility functions

	private XmppServletStanzaRequest buildReply(XmppServletIQRequest req)
	{
		Element resElement = DocumentHelper.createElement("iq");
		resElement.addAttribute("type", "result");
		resElement.addAttribute("id", req.getId());
		resElement.addAttribute("from", req.getTo().toString());
		resElement.addAttribute("to", req.getFrom().toString());
		XmppServletStanzaRequest iq = _xmppFactory.createStanzaRequest(req.getSession(), resElement);
		return iq;
	}

	private String unescapeNode(String node)
	{
		node = node.replace("\\20", " ");
		node = node.replace("\\22", "\"");
		node = node.replace("\\26","&");
		node = node.replace("\\27", "'");
		node = node.replace("\\2f", "/");
		node = node.replace("\\3a", ":");
		node = node.replace("\\3c", "<");
		node = node.replace("\\3e", ">");
		node = node.replace("\\40", "@");
		node = node.replace("\\5c", "\\");
		return node;
	}
}
