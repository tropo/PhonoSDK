package com.voxeo.gordon.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.Resource;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.spi.DriverManager;
import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sdp.SessionName;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;

import org.jcouchdb.db.Database;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.jcouchdb.db.Database;

import com.voxeo.gordon.server.Call.Side;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;
import com.voxeo.servlet.xmpp.XmppServletFeaturesRequest;
import com.voxeo.servlet.xmpp.XmppServletIQRequest;
import com.voxeo.servlet.xmpp.XmppServletIQResponse;
import com.voxeo.servlet.xmpp.XmppServletStanzaRequest;
import com.voxeo.servlet.xmpp.XmppServletStreamRequest;
import com.voxeo.servlet.xmpp.XmppSession;
import com.voxeo.servlet.xmpp.XmppStanzaError;
import com.voxeo.servlet.xmpp.XmppSession.SessionType;

public class GordonXMPPServlet extends XmppServlet {

	private static final long serialVersionUID = -4294730746874986102L;

	private static final Logger LOG = Logger.getLogger(GordonXMPPServlet.class);

	private XmppFactory _xmppFactory;

	private SipFactory _sipFactory;

	private String _rtmpdUri;

	private String _hostname;

	private String _sbcHost;

	private Database _db;

	private Map<String, XmppSession> _clientSessions;
	
	private Map<String, String> _apiKeys;

	@Resource
	SdpFactory _sdpFactory;

	protected MsControlFactory _msControlFactory;

	@Override
	public void init() throws ServletException {
		LOG.info("Gordon: XMPP initing");
		// Get a reference to the XMPPFactory and SIPFactory
		_xmppFactory = (XmppFactory) getServletContext().getAttribute(XmppServlet.XMPP_FACTORY);
		_sipFactory = (SipFactory) getServletContext().getAttribute(SipServlet.SIP_FACTORY);
		_clientSessions = new ConcurrentHashMap<String, XmppSession>();
		_apiKeys = new ConcurrentHashMap<String, String>();
		
		String dbHost = getServletConfig().getInitParameter("phonoDbHost");
		String dbName = getServletConfig().getInitParameter("phonoDbName");
		String dbPort = getServletConfig().getInitParameter("phonoDbPort");
		_sbcHost = getServletConfig().getInitParameter("sbcHost");

		// create a database object
		if (dbHost != null && dbName != null && !dbHost.isEmpty() && !dbName.isEmpty()) {
			int port = 27017;
			if (dbPort != null && !dbPort.isEmpty()) port = Integer.parseInt(dbPort);
			_db = new Database(dbHost, port, dbName);
		} else {
			_db = null;
			LOG.error("Missing CDR database - will not log CDRs");
		}
		// Store a link to the database in the servlet context
		getServletContext().setAttribute("phonoDb", _db);

		// Store a link to the client sessions in the servlet context
		getServletContext().setAttribute("xmppSessions", _clientSessions);

		// Store a link to the apiKey table in the servlet context
		getServletContext().setAttribute("apiKeys", _apiKeys);
		
		// Read the config to determine the rtmp relay server address
		_rtmpdUri = getServletConfig().getInitParameter("rtmpdUri");
		LOG.info("Configuration says RTMP relay is at " + _rtmpdUri);

		// Store a link to the rtmpd uri the servlet context
		getServletContext().setAttribute("rtmpdUri", _rtmpdUri);

		try {
			InetAddress addr = InetAddress.getLocalHost();
			// Get hostname
			_hostname = addr.getHostName();
		}
		catch (final Exception e) {
			LOG.error("", e);
			throw new ServletException(e);
		}

		try {
			// create the Media Session Factory
			_msControlFactory = DriverManager.getDrivers().next().getFactory(null);
		}
		catch (final Exception e) {
			LOG.error("", e);
			throw new ServletException(e);
		}
		LOG.info("Gordon: XMPP inited");
	}

	@Override
	public void destroy() {
		super.destroy();

	}

	@Override
	protected void doMessage(XmppServletStanzaRequest req) throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("XMPP:Received message:" + req.getElement().asXML());
		}

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
			// It's coming from a client (we only forward from client to server, and not client to client)
			JID fromJid = session.getRemoteJIDs().iterator().next();

			JID toJid = _xmppFactory.createJID(req.getElement().attribute("to").getValue());

			// Check if it's an error report
			if (req.getTo().getNode() == "issue" && req.getTo().getDomain().contentEquals("log."+_hostname)) {
				reportIssue(req.getElement().element("body").getText(), fromJid.getNode() + "@" + _hostname, req.getRemoteAddr());
			} else {
				// Forward it on
				messageElement.addAttribute("from", fromJid.getNode() + "@" + _hostname + "/voxeo");
				messageElement.addAttribute("to", toJid.toString());

				if (LOG.isDebugEnabled()) {
					LOG.debug("Sending message to domain :" + toJid.getDomain() + ". Message: " + messageElement.asXML());
				}
				// Get a handle to the server session and forward it
				_xmppFactory.createStanzaRequest(_xmppFactory.createJID(toJid.getDomain()), messageElement,
						_xmppFactory.createJID(_hostname)).send();
			}
		}
		else if (session.getSessionType() == XmppSession.SessionType.SERVER) {
			// It's coming from a server
			// Lookup the destination session (we only forward server to client and not server to server)
			destSession = _clientSessions.get(req.getTo().getNode());

			if (destSession != null) {
				messageElement.addAttribute("from", req.getElement().attribute("from").getValue());

				if (LOG.isDebugEnabled()) {
					LOG.debug("Sending message to client :" + req.getTo().getBareJID().toString() + ". Message: "
							+ messageElement.asXML());
				}
				destSession.createStanzaRequest(messageElement, null, null, null, null, null).send();
			}
			else {
				LOG.warn("User: " + req.getTo().getBareJID() + " is offline. Drop this message and send back error message.");
				req.createErrorStanzaRequest(XmppStanzaError.Type_CANCEL, XmppStanzaError.RECIPIENT_UNAVAILABLE_CONDITION,
						"user is offline", "en", null).send();
			}
		}
	}

	@Override
	protected void doPresence(XmppServletStanzaRequest req) throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("XMPP:Received presence:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
		}
		Element presenceElement = DocumentHelper.createElement("presence");
		
		if ((req.getTo().getDomain().startsWith("sip.") || req.getTo().getDomain().startsWith("app."))
				 && req.getTo().getNode() != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("XMPP:Presence sent to sip. or app.");
			}
			// Does the 'to' match our domain? (we accept from both client and s2s sessions)
			// This is presence sent to our server, but not to a client that is attached
			// If it is unavailable, then we hang up any calls that this source has active
			if (req.getElement().attribute("type").getValue().contentEquals("unavailable")) {
				if  (LOG.isDebugEnabled()) {
					LOG.debug("XMPP:Unavailable presence - hang up any active calls");
				}
				Enumeration<String> attributes = req.getSession().getAttributeNames();
				while (attributes.hasMoreElements()) {
					Object o = req.getSession().getAttribute(attributes.nextElement());
					if (o instanceof Call) {
						Call theCall = (Call) o;
						if (theCall.xmppAddress.equals(req.getFrom().toString())) {
							// The xmpp side has gone, along with any calls, so gracefully close the sip side.
							theCall.doBye(Side.Jingle, Jingle.REASON_GONE);
						}
					}
				}
			}
		} else if (req.getTo().getDomain().contentEquals(_hostname) && req.getTo().getNode() != null) {
			// Find the session associated with the 'to'
			XmppSession destSession = _clientSessions.get(req.getTo().getNode());
			if (destSession == null) {
				
				return;
			}
			Set presenceSubscriptions = (Set)destSession.getAttribute("presenceSubscriptions");		
			if (req.getSession().getSessionType() == XmppSession.SessionType.SERVER && 
					(req.getElement().attribute("type") == null || req.getElement().attribute("type").getValue().contentEquals("unavailable"))) {
				// We have received a presence notification from an s2s, so we send it to the client that it's for
				presenceElement.addAttribute("from", req.getElement().attribute("from").getValue());
				if (req.getElement().attribute("type") != null) presenceElement.addAttribute("type", req.getElement().attribute("type").getValue());
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Sending presence to client :" + req.getTo().getBareJID().toString() + ". Message: "
							+ presenceElement.asXML());
				}
				destSession.createStanzaRequest(presenceElement, null, null, null, null, null).send();
			}
			if (req.getElement().attribute("type") != null && req.getElement().attribute("type").getValue().contentEquals("subscribe")) {
				// Add the sender to the list to be notified on an unavailable
				presenceSubscriptions.add(req.getFrom().toString());
			}
			if (req.getElement().attribute("type") != null && req.getElement().attribute("type").getValue().contentEquals("unsubscribe")) {
				// Remove the sender from the list to be notified on an unavailable
				presenceSubscriptions.remove(req.getFrom().toString());
			}
		} else if (req.getSession().getSessionType() == XmppSession.SessionType.CLIENT && req.getTo() != null) {
			// if it is from one of our clients and it has a 'to' forward it out to s2s

			JID fromJid = req.getSession().getRemoteJIDs().iterator().next();
			JID toJid = _xmppFactory.createJID(req.getElement().attribute("to").getValue());

			if (req.getElement().attribute("type") != null) presenceElement.addAttribute("type", req.getElement().attribute("type").getValue());
			presenceElement.addAttribute("from", fromJid.toString());
			presenceElement.addAttribute("to", toJid.toString());

			if (LOG.isDebugEnabled()) {
				LOG.debug("Sending presence to domain :" + toJid.getDomain() + ". Message: " + presenceElement.asXML());
			}
			// Get a handle to the server session and forward it
			_xmppFactory.createStanzaRequest(_xmppFactory.createJID(toJid.getDomain()), presenceElement,
					_xmppFactory.createJID(_hostname)).send();

		} else if (req.getSession().getSessionType() == XmppSession.SessionType.CLIENT && req.getTo() == null) {
			// if it is from one of our clients and it does not have a 'to' we relay the presence to all subscribers
			JID fromJid = req.getSession().getRemoteJIDs().iterator().next();
			if (req.getElement().attribute("type") != null) presenceElement.addAttribute("type", req.getElement().attribute("type").getValue());
			presenceElement.addAttribute("from", fromJid.toString());
			sendPresence(presenceElement, (Set)(req.getSession().getAttribute("presenceSubscriptions")));
		} else {
			// We don't relay for third parties - but we do listen for presence 
		}
	}

	private void sendPresence(Element element, Set presenceSubscriptions) {
		// Loop over presenceSubscriptions and notify all parties of the presence change
		for (Object destination : presenceSubscriptions) {			
			JID toJid = _xmppFactory.createJID((String)destination);
			// Is this to a local JID?
			if (toJid.getDomain().contentEquals(_hostname)) {
				// Find the local session and send it
				XmppSession destSession = _clientSessions.get(toJid.getNode());
				if (destSession == null) {
					return;
				} else {
					try {
						destSession.createStanzaRequest(element, null, null, null, null, null).send();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				// It's remote, so set the 'to' and send it
				element.addAttribute("to", toJid.toString());
				// Get a handle to the server session and forward it
				try {
					_xmppFactory.createStanzaRequest(_xmppFactory.createJID(toJid.getDomain()), element,
						_xmppFactory.createJID(_hostname)).send();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	protected void doIQRequest(XmppServletIQRequest req) throws ServletException, IOException {
		if (req.getSession().getSessionType() == XmppSession.SessionType.CLIENT) {

			// 0 process resource binding
			/**
			 * <iq type='result' id='bind_2'> <bind
			 * xmlns='urn:ietf:params:xml:ns:xmpp-bind'>
			 * <jid>somenode@example.com/someresource</jid> </bind> </iq>
			 */
			Element element = req.getElement().element("bind");
			if (element != null) {
				Element resElement = DocumentHelper.createElement("iq");
				resElement.addAttribute("type", "result");
				// resElement.addElement(new QName("bind", new Namespace("",
				// "urn:ietf:params:xml:ns:xmpp-bind"))).addElement("jid")
				// .setText(req.getFrom().getBareJID().toString() + "/someresource");

				resElement.addElement(new QName("bind", new Namespace("", "urn:ietf:params:xml:ns:xmpp-bind"))).addElement("jid")
				.setText(req.getFrom().getNode() + "@" + _hostname + "/voxeo");

				req.createIQResultResponse(resElement).send();
				return;
			}

			// 1 process session feature iq
			/**
			 * <iq from='example.com' type='result' id='sess_1'/>
			 */
			element = req.getElement().element("session");
			if (element != null) {
				Element resElement = DocumentHelper.createElement("iq");
				resElement.addAttribute("type", "result");
				resElement.addElement(new QName("session", new Namespace("", "urn:ietf:params:xml:ns:xmpp-session")));

				req.createIQResultResponse(resElement).send();
				return;
			}
			
			// Process jingle from clients
			if (!req.getElement().elements(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI))).isEmpty()) {
				doJingleRequest(req);
				return;
			}
		}

		// Process apikey authentications
		if (!req.getElement().elements(new QName("apikey", new Namespace("","http://phono.com/apikey"))).isEmpty()) {
			if (req.getType().contentEquals("set")) {
				Element key = req.getElement().element("apikey");
				// Grab the key value				
				if (key != null) {
					String keyValue = key.getText();
					// Store it against the source JID so that we can put it with CDRs
					_apiKeys.put(req.getFrom().toString(), keyValue);
					updateSDR(req.getSession(), keyValue);
					Element resElement = DocumentHelper.createElement("iq");
					resElement.addAttribute("type", "result");
					resElement.addElement(new QName("apikey", new Namespace("", "http://phono.com/apikey")));
					req.createIQResultResponse(resElement).send();
					return;
				}
			}
		}
		
		// Process jingle requests for s2s federated connections
		//if (!req.getElement().elements(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI))).isEmpty()) {
		//	doJingleRequest(req);
		//	return;
		//}

		// process other iq request. return stanza error.
		XmppServletIQResponse resElement = req.createIQErrorResponse(XmppStanzaError.Type_CANCEL,
				XmppStanzaError.FEATURE_NOT_IMPLEMENTED_CONDITION, null, null, null);
		resElement.send();
	}

	@Override
	protected void doIQResponse(XmppServletIQResponse req) throws ServletException, IOException {
		/*
		if (req.getSession().getSessionType() != XmppSession.SessionType.CLIENT) {
			// Not from a client, drop it.
			return;
		}
		*/
		if (LOG.isDebugEnabled()) {
			LOG.debug("XMPP:Received iq response:" + req.getElement().asXML() + ". The session id:"
					+ req.getSession().getId());
		}

		if (!req.getElement().elements("jingle").isEmpty())
			doJingleResponse(req);
	}

	@Override
	protected void doStreamEnd(XmppServletStreamRequest req) throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("received doStreamEnd:" + req.getElement().asXML() + "session id:" + req.getSession().getId());
		}

		if (req.getSession().getSessionType() == XmppSession.SessionType.CLIENT) {
			JID jid = req.getSession().getRemoteJIDs().iterator().next();
			
			// Send all unavailable presences
			Element presenceElement = DocumentHelper.createElement("presence");
			presenceElement.addAttribute("from", jid.toString());
			presenceElement.addAttribute("type", "unavailable");
			sendPresence(presenceElement, (Set)(req.getSession().getAttribute("presenceSubscriptions")));
			
			// Remove from clientSessions
			_clientSessions.remove(jid.getNode());
		}
	}

	@Override
	protected void doStreamStart(XmppServletStreamRequest req) throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received doStreamStart:" + req.getElement().asXML() + ". The session id:" + req.getSession().getId());
		}

		// received the stream after SASL negotiation.
		// if it is a incoming client session. send bind and session features.
		if (req.getSession().getSessionType() == SessionType.CLIENT && req.isInitial()) {
			req.createRespXMPPServletStreamRequest().send();
			XmppServletFeaturesRequest featuresReq = req.createFeaturesRequest();
			featuresReq.addFeature("urn:ietf:params:xml:ns:xmpp-session", "session");
			featuresReq.send();

			// save to map.
			JID jid = req.getSession().getRemoteJIDs().iterator().next();
			_clientSessions.put(jid.getNode(), req.getSession());

			// Store the remote address
			req.getSession().setAttribute("remoteAddr", req.getRemoteAddr());
			
			// Create the presence subscription map
			Set presenceSubscriptions = new ConcurrentSkipListSet<String>();
			req.getSession().setAttribute("presenceSubscriptions", presenceSubscriptions);			
						
			// Write the SDR
			openSDR(req.getSession(), req.getRemoteAddr());
		} 
	}

	private void doJingleRequest(XmppServletIQRequest req) throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received jingle request:" + req.getElement().asXML());
		}

		Element jgl = req.getElement().element("jingle");
		String action = jgl.attribute("action").getData().toString();
		
		LOG.debug("action = " + action);
		
		Call theCall = null;

		if (action.equals(Jingle.ACTION_SESSION_TERMINATE)) {
			// Find the call
			theCall = (Call) req.getSession()
			.getAttribute("Call-" + req.getElement().element("jingle").attributeValue("sid"));
			if (theCall != null) {
				String reason = "";
				if (req.getElement().element("jingle").element("reason") != null) {
					for (Object o : req.getElement().element("jingle").element("reason").elements())
					{
						Element e = (Element)o;
						reason = e.getName();
					}
				}
				theCall.doBye(Side.Jingle,reason);
			}
			else {
				LOG.warn("Receive ACTION_SESSION_TERMINATE req, but can't find call: " + "Call-"
						+ req.getElement().element("jingle").attributeValue("sid"));
			}

			// This is a request to hang up an existing call
			// Acknowledge the request
			XmppServletIQResponse iq = buildReply(req);
			iq.send();
		}
		else if (action.equals(Jingle.ACTION_SESSION_INFO)) {
			// This may be ringing or dtmf

			// Acknowledge the request
			XmppServletIQResponse iq = buildReply(req);
			iq.send();

			// Find the call
			theCall = (Call) req.getSession()
			.getAttribute("Call-" + req.getElement().element("jingle").attributeValue("sid"));

			if (theCall != null && req.getElement().element("jingle").element("dtmf") != null) {
				// Generate an INFO
				SipServletRequest sipreq = theCall.sipSession.createRequest("INFO");
				String content = "Signal=" + req.getElement().element("jingle").element("dtmf").attributeValue("code");
				if (req.getElement().element("jingle").element("dtmf").attributeValue("duration") != null)
					content += "\r\nDuration=" + req.getElement().element("jingle").element("dtmf").attributeValue("duration");
				sipreq.setContent(content, "application/dtmf-relay");
				sipreq.send();
			}
			if (theCall != null && req.getElement().element("jingle").element("ringing") != null) {
				try {
					theCall.peerAccept();
				} catch(Exception e) {
					LOG.error("", e);
				}
			}

		}
		else if (action.equals(Jingle.ACTION_SESSION_ACCEPT)) {
			// Acknowledge the request
			XmppServletIQResponse iq = buildReply(req);
			iq.send();

			// Find the call
			theCall = (Call) req.getSession()
			.getAttribute("Call-" + req.getElement().element("jingle").attributeValue("sid"));
			if (theCall != null) {
				try {
					theCall.peerConfirm(null);
				}
				catch (Exception ex) {
					LOG.error("", ex);
					theCall.terminate(Side.Jingle, Jingle.REASON_GENERAL_ERROR);
				}
			}
			else {
				LOG.warn("Receive ACTION_CONTENT_ACCEPT req, but can't find call: " + "Call-"
						+ req.getElement().element("jingle").attributeValue("sid"));
			}
		} 
		else if (action.equals(Jingle.ACTION_TRANSPORT_INFO)) {
			// Acknowledge the request
			XmppServletIQResponse iq = buildReply(req);
			iq.send();
	
			// Parse and act on the transport
			doJingleTransport(req);		
		}
		else if (action.equals(Jingle.ACTION_SESSION_INITIATE)) {
			// This is a new call request from the user
			Address to = null;
			Address from = null;
			// Acknowledge the request
			XmppServletIQResponse iq = buildReply(req);
			iq.send();
			
			// Check that this is a SIP call, if not we should route elsewhere
			if (req.getSession().getSessionType() == SessionType.CLIENT) {
				LOG.debug("c2s jingle initiate");
				if (req.getTo().getDomain().contentEquals("sip") || req.getTo().getDomain().contentEquals("app") || 
					req.getTo().getDomain().contentEquals("sip."+_hostname) || req.getTo().getDomain().contentEquals("app."+_hostname)) {
				
					if (req.getTo().getDomain().contentEquals("app") || req.getTo().getDomain().contentEquals("app."+_hostname))
						to = _sipFactory.createAddress("sip:" + unescapeNode(req.getTo().getNode()) + "@" + _sbcHost);
					else	
						to = _sipFactory.createAddress("sip:" + unescapeNode(req.getTo().getNode()));
				} else {
					LOG.debug("Destination is not SIP - abort");
					return;
				}
			} else if (req.getSession().getSessionType() == SessionType.SERVER) {
				LOG.debug("s2s jingle initiate");
				to = _sipFactory.createAddress("sip:" + unescapeNode(req.getTo().getNode()));
			}
				
			from = _sipFactory.createAddress("sip:" + req.getElement().element("jingle").attributeValue("initiator"));

			LOG.info("Sending invite from " + from + " to " + to);

			SipApplicationSession appSession = _sipFactory.createApplicationSession();
				
			// create an INVITE request to the first party from the second
			SipServletRequest sipreq = _sipFactory.createRequest(appSession, "INVITE", from, to);

			// Create a call object to track the state of this call
			XmppToSipCall newCall = new XmppToSipCall(req.getElement().element("jingle").attributeValue("initiator"), _db, _xmppFactory,
					_sdpFactory, sipreq);
			newCall.sipSession = sipreq.getSession();
			newCall.xmppSession = req.getSession();
			newCall.xmppAddress = req.getFrom().toString();
			newCall.sipAddress = req.getTo().toString();
			newCall.unescapedSipAddress = to.toString().substring(to.toString().indexOf(":") + 1).trim();
			newCall.xmppIPAddress = req.getRemoteAddr();
			newCall.jingleSessionId = req.getElement().element("jingle").attributeValue("sid");
			newCall.sipIPAddress = "";
			newCall.apiKey = _apiKeys.get(req.getFrom().toString());

			newCall.openCDR(Side.Jingle);

			// Store the call object in each session so we can find it again
			req.getSession().setAttribute("Call-" + req.getElement().element("jingle").attributeValue("sid"), newCall);
			sipreq.getSession().setAttribute("Call", newCall);

			// Add custom headers if we have some
			for (Object customObj : req.getElement().element("jingle").elements("custom-header")) {
				Element custom = (Element) customObj;
				sipreq.addHeader(custom.attributeValue("name"), custom.attributeValue("data"));
			}

			doJingleTransport(req);						
		}
	}
	
	private void doJingleTransport(XmppServletIQRequest req) {
		String mediaAddress = null;
		String mediaPort = null;
		String codec = "";
		String pt = "";
		String clockrate = "";
		Relay relay = null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received jingle transport:" + req.getElement().asXML());
		}
		
		Call theCall = (Call) req.getSession().getAttribute("Call-" + req.getElement().element("jingle").attributeValue("sid"));
		if (theCall == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Found a call");
		}
		
		// We support 2 types of transport - standard rtp and our own rtmp
		Element description = req.getElement().element("jingle").element("content").element("description");
		if (description.getNamespaceURI() == Jingle.DESCRIPTION_RTMP_NAMESPACE_URI) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("RTMP transport found");
			}
			// We allocate the relay using REST
			relay = new Relay(_rtmpdUri);
			if (relay != null) {
				mediaAddress = relay.localHost;
				mediaPort = relay.localPort;
				codec = relay.codec;
				pt = relay.payloadType;
				theCall.relay = relay;
				
				if (relay.initialReset) {
					// Terminate all calls other than this one
					try {
						Map<String, XmppSession> clientSessions = (Map<String, XmppSession>) getServletContext().getAttribute("xmppSessions");
						Iterator it = clientSessions.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry entry = (Map.Entry)it.next();
							System.out.println(entry.getKey() + " = " + entry.getValue());
							XmppSession termSession = (XmppSession)entry.getValue();
							Enumeration<String> attributes = termSession.getAttributeNames();
							while (attributes.hasMoreElements()) {
								Object o = termSession.getAttribute(attributes.nextElement());
								LOG.debug("Got object:" + o);
								if (o instanceof Call) {
									LOG.debug("Is a call.");
									Call termCall = (Call) o;
									if (termCall.jingleSessionId != theCall.jingleSessionId) termCall.terminate(Side.Gateway, Jingle.REASON_MEDIA_ERROR);
								}
							}
						}
					} catch (Exception e) {
						LOG.error(e);
					}
				}
			}
			else {
				LOG.error("Error allocating relay - sending an error back to the client.");
				try {
					XmppServletIQResponse error = buildReply(req);
					error.setType("error");
					error.send();
					return;
				} catch (IOException e) {
					LOG.error(e);
				}
			}

		}
		else if (description.getNamespaceURI() == Jingle.DESCRIPTION_RTP_NAMESPACE_URI) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("RTP transport found");
			}
			codec = req.getElement().element("jingle").element("content").element("description").element("payload-type")
				.attributeValue("name");
			pt = req.getElement().element("jingle").element("content").element("description").element("payload-type")
				.attributeValue("id");
			clockrate = req.getElement().element("jingle").element("content").element("description")
				.element("payload-type").attributeValue("clockrate");
			// Do we have a transport yet?
			for (Object obj : req.getElement().element("jingle").element("content").elements("transport")) {	
				Element transport = (Element)obj;
				if (transport.element("candidate") != null) {
					mediaAddress = transport.element("candidate").attributeValue("ip");
					mediaPort = transport.element("candidate").attributeValue("port");
				}	
			}
			if (mediaAddress == null) {
				// No transport yet, so we should wait until we have one
				if (LOG.isDebugEnabled()) {
					LOG.debug("No valid RTP transport found");
				}
				return;
			}
		}
		else {
			//error
			LOG.error("Unable to parse transport.");
			try {
				XmppServletIQResponse error = buildReply(req);
				error.setType("error");
				error.send();
				return;
			} catch (IOException e) {
				LOG.error(e);
			}
		}
		
		// Create some SDP
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Setting up the bridge.");
			}
			SessionDescription sdp = _sdpFactory.createSessionDescription();
			
			SipServletRequest sipreq = theCall._initReq;

			SessionName s = _sdpFactory.createSessionName("-");
			Connection c = _sdpFactory.createConnection("IN", "IP4", mediaAddress);

			// int formats[] = {Integer.parseInt(pt),};
			int formats[] = {Integer.parseInt(pt), 101};
			MediaDescription am = _sdpFactory.createMediaDescription("audio", Integer.parseInt(mediaPort), 1, "RTP/AVP",
					formats);
			String rtpmap = pt + " " + codec;
			if (clockrate != null && clockrate.trim().length() > 0) {
				rtpmap = rtpmap + "/" + clockrate;
			}

			String dtmfmap = "101 telephone-event/8000";

			Attribute speexAttribute = _sdpFactory.createAttribute("rtpmap", rtpmap);
			Attribute dtmfAttribute = _sdpFactory.createAttribute("rtpmap", dtmfmap);

			// am.setAttribute("rtpmap", rtpmap);
			// am.setAttribute("ptime", "40");

			// a=rtpmap:101 telephone-event
			// String dtmfmap = "101 telephone-event/8000";
			// am.setAttribute("rtpmap", dtmfmap);
			Attribute fmtpAttribute = _sdpFactory.createAttribute("fmtp", "101 0-15");
			// am.setAttribute("fmtp", "101 0-15");

			Vector<Attribute> atts = new Vector<Attribute>();
			atts.add(speexAttribute);
			atts.add(dtmfAttribute);
			atts.add(fmtpAttribute);

			am.setAttributes(atts);

			Vector<MediaDescription> mediaDescs = new Vector<MediaDescription>();
			mediaDescs.add(am);

			sdp.setConnection(c);
			sdp.setSessionName(s);

			sdp.setMediaDescriptions(mediaDescs);

			// BRIDGE JOIN.
			// Bridge bridge = new NCJoinBridgeImpl(_msControlFactory,
			// sipreq.getSession().getServletContext());
			Bridge bridge = new MixerBridgeImpl(_msControlFactory);

			theCall.bridge = bridge;

			bridge.addLeftListener(new RTMPDSideSDPListener(sipreq, theCall));
			if (LOG.isDebugEnabled()) {
				LOG.debug("The constructed sdp: " + sdp.toString());
			}

			bridge.processSdpOfferLeft(sdp.toString().getBytes());
		}
		catch (Exception e) {
			LOG.error("", e);
			theCall.terminate(Side.Jingle, Jingle.REASON_GENERAL_ERROR);
		}
	}

	private void doJingleResponse(XmppServletIQResponse req) throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received jingle response:" + req.getElement().asXML());
		}
	}

	// CDR functions
	private void openSDR(XmppSession session, String xmppIPAddress) {
		Map<String,String> sdr;
		try {
			sdr = new HashMap<String, String>();
			if (LOG.isDebugEnabled()) {
				LOG.debug("openSession");
			}
			if (_db != null) {
				long ts = (new Date()).getTime();
				String xmppAddress = session.getRemoteJIDs().iterator().next().toString();
				sdr.put("type", "session");
				sdr.put("xmppId", xmppAddress);
				sdr.put("xmppHost", xmppIPAddress);
				sdr.put("startTs", Long.toString(ts));	  

				// create the document in couchdb
				_db.createDocument(sdr);
				session.setAttribute("sessionSDR", sdr);
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	private void updateSDR(XmppSession session, String apiKey) {
		Map<String,String> sdr;
		Database db;
		try {
			if (_db != null) {
				sdr = (Map<String,String>)session.getAttribute("sessionSDR");
				if (LOG.isDebugEnabled()) {
					LOG.debug("Got sdr = " + sdr + ", adding apiKey = " + apiKey);
				}
				if (sdr != null) {
					sdr.put("apiKey", apiKey);
					_db.updateDocument(sdr);
				}
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	private void reportIssue(String issue, String xmppAddress, String xmppIPAddress) {
		Map<String,String> cdr;
		cdr = new HashMap<String, String>();
		if (LOG.isDebugEnabled()) {
			LOG.debug("reportIssue");
		}
		if (_db != null) {
			long ts = (new Date()).getTime();
			cdr.put("type", "issue");
			cdr.put("xmppId", xmppAddress);
			cdr.put("xmppHost", xmppIPAddress);
			cdr.put("issue", issue);
			cdr.put("reportTs", Long.toString(ts));	  

			// create the document in couchdb
			_db.createDocument(cdr);
		}
	}

	// Utility functions
	private XmppServletIQResponse buildReply(XmppServletIQRequest req) throws IOException {
		Element resElement = DocumentHelper.createElement("iq");
		resElement.addAttribute("type", "result");
		resElement.addAttribute("id", req.getId());
		resElement.addAttribute("from", req.getTo().toString());
		resElement.addAttribute("to", req.getFrom().toString());
		LOG.debug("buildReply: " + req.toString());
		XmppServletIQResponse iq = req.createIQResultResponse(resElement);
		iq.setType("result");
		iq.setFrom(req.getTo()); //WTF? this is needed or it comes from the servlet
		iq.setTo(req.getFrom());
		LOG.debug("buildReply2");
		return iq;
	}

	private String unescapeNode(String node) {
		node = node.replace("\\20", " ");
		node = node.replace("\\22", "\""); 
		node = node.replace("\\26", "&");
		node = node.replace("\\27", "'");
		node = node.replace("\\2f", "/");
		node = node.replace("\\3a", ":");
		node = node.replace("\\3c", "<");
		node = node.replace("\\3e", ">");
		node = node.replace("\\40", "@");
		node = node.replace("\\5c", "\\");
		return node;
	}

	class RTMPDSideSDPListener implements MediaEventListener<SdpPortManagerEvent> {

		private SipServletRequest _req;

		private Call _call;

		public RTMPDSideSDPListener(SipServletRequest req, Call call) {
			super();
			_req = req;
			_call = call;
		}

		public void onEvent(SdpPortManagerEvent event) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("GordonXMPPServlet RTMPDSideSDPListener received event: " + event);
			}

			if (event.isSuccessful() && event.getEventType() == SdpPortManagerEvent.ANSWER_GENERATED) {
				try {
					event.getSource().removeListener(this);
					_call.sipSession.setAttribute("RTMPDSideSDP", event.getMediaServerSdp());

					_call.bridge.addRightListener(new SipClientSideSDPListener(_req, _call));
					_call.bridge.generateSdpOfferRight();
				}
				catch (Exception ex) {
					LOG.error("", ex);
					if (_call != null) {
						_call.terminate(Side.Jingle, Jingle.REASON_GENERAL_ERROR);
					}
				}

			}
		}

	}

	class SipClientSideSDPListener implements MediaEventListener<SdpPortManagerEvent> {

		private SipServletRequest _req;

		private Call _call;

		public SipClientSideSDPListener(SipServletRequest req, Call call) {
			super();
			_req = req;
			_call = call;
		}

		public void onEvent(SdpPortManagerEvent event) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("GordonXMPPServlet SipClientSideSDPListener received event: " + event);
			}

			if (event.isSuccessful() && event.getEventType() == SdpPortManagerEvent.OFFER_GENERATED) {
				event.getSource().removeListener(this);
				try {
					_req.setContent(event.getMediaServerSdp(), "application/SDP");
					// send the invite
					_req.send();
				}
				catch (Exception e) {
					LOG.error("", e);
					if (_call != null) {
						_call.terminate(Side.Jingle, Jingle.REASON_GENERAL_ERROR);
					}
				}
			}
		}
	}
}
