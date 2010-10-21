package com.voxeo.gordon.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;

import org.jcouchdb.db.Database;

import javax.annotation.Resource;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;
import javax.media.mscontrol.spi.DriverManager;
import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.voxeo.gordon.server.Call.Side;
import com.voxeo.servlet.xmpp.XmppServletIQRequest;
import com.voxeo.servlet.xmpp.XmppSession;

public class GordonSIPServlet extends SipServlet {

	private static final long serialVersionUID = -6533418135799291706L;

	private static final Logger LOG = Logger.getLogger(GordonSIPServlet.class.getName());

	// private PolicyServer _policyServer;

	@Resource
	SdpFactory _sdpFactory;

	protected MsControlFactory _msControlFactory;

	@Override
	public void init() throws ServletException {
		LOG.info("Gordon: SIP initing");

		// LOG.debug("Starting policy server");
		// _policyServer = new PolicyServer(843);
		// _policyServer.start();

		try {
			// create the Media Session Factory
			_msControlFactory = DriverManager.getDrivers().next().getFactory(null);
		}
		catch (final Exception e) {
			LOG.error("", e);
			throw new ServletException(e);
		}
		LOG.info("Gordon: SIP inited");
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	/**
	 * Handle an options request
	 */
	protected void doOptions(SipServletRequest request) throws ServletException, IOException {
		request.createResponse(SipServletResponse.SC_NOT_IMPLEMENTED).send();
	}
	
	/**
	 * Handle a request to start a call from the SIP side
	 * 
	 * @param request
	 *          the request to start the call
	 */
	protected void doInvite(SipServletRequest request) throws ServletException, IOException {
		if (request.isInitial()) {
			LOG.debug("SIP:Got initial invite");

			String fromURI = request.getFrom().getURI().toString().toLowerCase();
			String initiator = fromURI.substring(fromURI.indexOf(":") + 1).trim();

			String toURI = request.getTo().getURI().toString().toLowerCase();
			String to = toURI.substring(toURI.indexOf(":") + 1).trim();

			// get the xmpp client session accordingly.
			Map<String, XmppSession> clientSessions = (Map<String, XmppSession>) getServletContext().getAttribute("xmppSessions");
			XmppSession xmppSession = clientSessions.get(to.split("@")[0]);

			if (xmppSession != null) {
				LOG.debug("SIP: Got initial invite, find requested xmpp client.");
				Database db = (Database)getServletContext().getAttribute("phonoDb");
				Call theCall = new SipToXmppCall(initiator, db, request, _sdpFactory);
				theCall.sipAddress = escapeNode(initiator)+"@sip";
				theCall.unescapedSipAddress = initiator;
				theCall.xmppAddress = to;
				theCall.jingleSessionId = UUID.randomUUID().toString();
				theCall.sipSession = request.getSession();
				theCall.xmppSession = xmppSession;
				theCall.xmppIPAddress = (String)xmppSession.getAttribute("remoteAddr");
				theCall.sipIPAddress = request.getRemoteAddr();

				// Allocate a relay
				Relay relay = new Relay((String)getServletContext().getAttribute("rtmpdUri"));
				theCall.relay = relay;
				theCall.openCDR(Side.SIP);

				if (relay.initialReset) {
					// Terminate all calls other than this one
					try {
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
				
				request.getSession().setAttribute("Call", theCall);
				xmppSession.setAttribute("Call-" + theCall.jingleSessionId, theCall);

				try {
					// BRIDGE JOIN.
					// Bridge bridge = new NCJoinBridgeImpl(_msControlFactory,
					// xmppSession.getServletContext());
					Bridge bridge = new MixerBridgeImpl(_msControlFactory);

					theCall.bridge = bridge;

					bridge.addRightListener(new SipClientSideSDPListener(theCall));

					if (LOG.isDebugEnabled()) {
						LOG.debug("bridge.processSdpOfferRight , the SDP" + new String(request.getRawContent()));
					}

					bridge.processSdpOfferRight(request.getRawContent());
				}
				catch (Exception ex) {
					LOG.error(ex);
					theCall.terminate(Side.SIP, Jingle.REASON_MEDIA_ERROR);
				}
			}
			else {
				LOG.debug("SIP: Got initial invite, can't find the requested xmpp client.");
				request.createResponse(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE).send();
			}

		}
		else {
			LOG.debug("SIP: Got re-invite");
			Call theCall = (Call) request.getSession().getAttribute("Call");
			if (request.getRawContent() != null) {
				try {
					theCall.bridge.addRightListener(new SipClientSideReInviteSDPListener(request));
					theCall.bridge.processSdpOfferRight(request.getRawContent());
				}
				catch (SdpPortManagerException e) {
					LOG.error("", e);
					request.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR).send();
				}
				catch (MsControlException e) {
					LOG.error("", e);
					request.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR).send();
				}
			}

		}
	}

	private String escapeNode(String node) {
		node = node.replace("\\", "\\5c");
		node = node.replace(" ", "\\20");
		node = node.replace("\"", "\\22");
		node = node.replace("&", "\\26");
		node = node.replace("'", "\\27");
		node = node.replace("/", "\\2f");
		node = node.replace(":", "\\3a");
		node = node.replace("<", "\\3c");
		node = node.replace(">", "\\3e");
		node = node.replace("@", "\\40");
		return node;
	}

	/**
	 * Handle a request to end the call
	 * 
	 * @param request
	 *          the request to end the call
	 */
	protected void doBye(SipServletRequest request) throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("SIP:Got bye");
		}

		Call theCall = (Call) request.getSession().getAttribute("Call");
		if (theCall != null) {
			// Do we have a reason?
			String reason = Jingle.REASON_SUCCESS;
			try {
				if (!request.getHeader("Reason").isEmpty()) {
					// XXX pull the cause code out
				}
			} catch (Exception e) {
				LOG.info("Exception reading Reason header in BYE:" + e);
			}
			theCall.doBye(Side.SIP, reason);
		}
		else {
			LOG.warn("Receive ACTION_SESSION_TERMINATE req, but can't find call in session");
		}

		// send an OK for the BYE
		SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
		ok.send();
	}

	/**
	 * Handle a request to cancel a call in progress
	 * 
	 * @param request
	 *          the request to end the call
	 */
	protected void doCancel(SipServletRequest request) throws ServletException, IOException {
		LOG.info("SIP:Got cancel");

		Call theCall = (Call) request.getSession().getAttribute("Call");

		if (theCall != null) {
			theCall.doBye(Side.SIP, Jingle.REASON_CANCEL);
		}
		// send an OK for the CANCEL
		SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
		ok.send();
	}

	/**
	 * Handle an info request
	 * 
	 * @param request
	 *          the request to end the call
	 */
	protected void doInfo(SipServletRequest request) throws ServletException, IOException {
		LOG.info("SIP:Got info");
		if (request.getContentType().equals("application/dtmf-relay")) {
			String content = (String) request.getContent();
			StringTokenizer st = new StringTokenizer(content, "=\r\n");
			String signal = null;
			String duration = null;
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.equals("Signal") && st.hasMoreTokens())
					signal = st.nextToken();
				if (token.equals("Duration") && st.hasMoreTokens())
					duration = st.nextToken();
			}
			if (signal != null) {
				Element dtmf = DocumentHelper.createElement(new QName("dtmf", new Namespace("", "urn:xmpp:jingle:dtmf:0")));
				dtmf.addAttribute("code", signal);
				if (duration != null)
					dtmf.addAttribute("duration", duration);

				// Grab the jingle session
				Call theCall = (Call) request.getSession().getAttribute("Call");

				if (theCall != null) {
					Element resElement = DocumentHelper.createElement("iq");
					resElement.addAttribute("type", "set");
					resElement.addAttribute("from", theCall.sipAddress);
					resElement.addAttribute("to", theCall.xmppAddress);
					resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI))).addAttribute(
							"action", Jingle.ACTION_SESSION_INFO).addAttribute("initiator", theCall.initiator).addAttribute("sid",
									theCall.jingleSessionId).add(dtmf);
					XmppServletIQRequest iq = theCall.xmppSession.createStanzaIQRequest(resElement, null, null, null, null, null);

					iq.send();
				}
			}
		}
	}

	/**
	 * Handle a provisional response 1XX
	 * 
	 * @param request
	 *          the request to end the call
	 */
	protected void doProvisionalResponse(SipServletResponse resp) throws ServletException, IOException {
		LOG.info("SIP:Got 1XX");
		if (resp.getStatus() == SipServletResponse.SC_RINGING) {

			// Grab the jingle session
			Call theCall = (Call) resp.getSession().getAttribute("Call");
			try {
				theCall.peerAccept();
			} catch(Exception e) {
				LOG.error("", e);
			}
			/*
      if (theCall != null) {
      	Element ringing = DocumentHelper.createElement(new QName("ringing", new Namespace("",
          "urn:xmpp:jingle:apps:rtp:1:info")));
        Element resElement = DocumentHelper.createElement("iq");
        resElement.addAttribute("type", "set");
        resElement.addAttribute("from", theCall.sipAddress);
        resElement.addAttribute("to", theCall.xmppAddress);
        resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI))).addAttribute(
            "action", Jingle.ACTION_SESSION_INFO).addAttribute("initiator", theCall.initiator).addAttribute("sid",
            theCall.jingleSessionId).add(ringing);
        XmppServletIQRequest iq = theCall.xmppSession.createStanzaIQRequest(resElement, null, null, null, null, null);

        iq.send();
      }
			 */
		}
	}

	/**
	 * Handle a success response. When a call sends an OK message, we complete the
	 * call at the bridge.
	 * 
	 * @param resp
	 *          the success response
	 */
	protected void doSuccessResponse(SipServletResponse resp) throws ServletException, IOException {
		LOG.info("Got OK");

		if (resp.getStatus() == SipServletResponse.SC_OK) {
			if (!resp.getRequest().isInitial()) {
				return;
			}

			// ACK the OK
			resp.createAck().send();

			// Grab the sessions
			SipSession session = resp.getSession();
			Call theCall = (Call) session.getAttribute("Call");

			// send the accept iq.
			try {
				theCall.peerConfirm(resp.getRawContent());
			}
			catch (Exception e1) {
				LOG.error("", e1);
				theCall.terminate(Side.SIP, Jingle.REASON_GENERAL_ERROR);
			}

		}
	}

	/**
	 * Handle an error response. Close the relevant call at the bridge.
	 * 
	 * @param resp
	 *          the error response
	 */
	protected void doErrorResponse(SipServletResponse resp) throws ServletException, IOException {
		LOG.info("SIP:Got error");

		// Grab the jingle session
		Call theCall = (Call) resp.getSession().getAttribute("Call");

		if (theCall != null) {
			theCall.doBye(Side.SIP, Jingle.SIPToJingleReason(resp.getStatus()));
		}

		// cancel the call
		resp.getSession().invalidate();
	}

	class RTMPDSideSDPListener implements MediaEventListener<SdpPortManagerEvent> {
		Call theCall;

		public RTMPDSideSDPListener(Call call) {
			super();
			theCall = call;
		}

		public void onEvent(SdpPortManagerEvent event) {
			if (event.isSuccessful() && event.getEventType() == SdpPortManagerEvent.OFFER_GENERATED) {
				event.getSource().removeListener(this);

				LOG.debug("GordonSIPServlet RTMPDSideSDPListener OFFER_GENERATED, The SDP: "
						+ new String(event.getMediaServerSdp()));

				// Parse the SDP and set the relay destination
				int remotePort = 0;
				String remoteHost = "";
				int payloadType = 0;
				String codec = "";

				// Build the description Element
				Element description;
				if (theCall.relay != null) {
					description = DocumentHelper.createElement(new QName("description", new Namespace("",
							Jingle.DESCRIPTION_RTMP_NAMESPACE_URI)));
				}
				else {
					description = DocumentHelper.createElement(new QName("description", new Namespace("",
							Jingle.DESCRIPTION_RTP_NAMESPACE_URI)));
				}

				// Parse the SDP to get the information we need

				try {
					SessionDescription sdp = _sdpFactory.createSessionDescription(new String(event.getMediaServerSdp()));
					Connection c = sdp.getConnection();
					remoteHost = c.getAddress();
					// Build <description>
					description.addAttribute("media", "audio");
					for (Object o : sdp.getMediaDescriptions(false)) {
						MediaDescription md = (MediaDescription) o;
						if (md.getMedia().getMediaType().equals("audio")) {
							// Build <payload-type>'s and add to <description>
							remotePort = md.getMedia().getMediaPort();
							Vector<Attribute> attributes = md.getAttributes(false);
							for (Attribute attribute : attributes) {
								if (attribute.getName().equals("rtpmap")) {
									Element payload = DocumentHelper.createElement("payload-type");
									String[] tokens = attribute.getValue().split(" ");
									int tPayloadType = Integer.parseInt(tokens[0]);
									payload.addAttribute("id", tokens[0]);
									tokens = tokens[1].split("/");
									codec = tokens[0];
									payload.addAttribute("name", codec);

									payload.addAttribute("clockrate", tokens[1]);

									if (codec.trim().equalsIgnoreCase("SPEEX") && tokens[1].contains("8000")) {
										payloadType = tPayloadType;
									}

									description.add(payload);
								}
							}
						}
					}

				}
				catch (Exception e) {
					LOG.error("SDP parsing exception: " + e);
				}

				// Build the transport Element
				Element transport;
				if (theCall.relay != null) {
					theCall.relay.SetDestination(remoteHost, remotePort, payloadType);
					transport = DocumentHelper.createElement(new QName("transport", new Namespace("", Jingle.TRANSPORT_RTMP)));
					// Build out transport
					Element candidate = DocumentHelper.createElement("candidate");
					candidate.addAttribute("rtmpUri", theCall.relay.rtmpUri);
					// candidate.addAttribute("rtmpUri", "rtmp://127.0.0.1/broadcast");
					candidate.addAttribute("playName", theCall.relay.playName);
					candidate.addAttribute("publishName", theCall.relay.publishName);
					candidate.addAttribute("id", "1");
					transport.add(candidate);
				}
				else {
					// Build our transport
					transport = DocumentHelper.createElement(new QName("transport", new Namespace("", Jingle.TRANSPORT_UDPRAW)));
					Element candidate = DocumentHelper.createElement("candidate");
					candidate.addAttribute("ip", remoteHost);
					candidate.addAttribute("port", Integer.toString(remotePort));
					candidate.addAttribute("id", "1");
					candidate.addAttribute("generation", "0");
					candidate.addAttribute("component", "1");
					transport.add(candidate);
				}

				Element resElement = DocumentHelper.createElement("iq");
				resElement.addAttribute("type", "set");
				resElement.addAttribute("from", theCall.sipAddress);
				resElement.addAttribute("to", theCall.xmppAddress);
				Element content = DocumentHelper.createElement("content");
				content.add(description);
				content.add(transport);
				Element jingle = DocumentHelper.createElement(new QName(Jingle.ELEMENT_NAME, new Namespace("",
						Jingle.NAMESPACE_URI)));
				jingle.addAttribute("initiator", theCall.initiator).addAttribute("sid", theCall.jingleSessionId).addAttribute(
						"action", Jingle.ACTION_SESSION_INITIATE).add(content);
				SipToXmppCall sipCall = (SipToXmppCall) theCall;

				// Add the headers
				for (Element header : sipCall.getHeaderElements()) {
					jingle.add(header);
				}

				resElement.add(jingle);

				// Send the initiate
				try {
					XmppServletIQRequest iq = null;
					if (theCall.xmppSession != null) {
						iq = theCall.xmppSession.createStanzaIQRequest(resElement, null, null, null, null, null);
					}

					iq.send();
				}
				catch (IOException e) {
					LOG.error("", e);
					if (theCall != null) {
						theCall.terminate(Side.SIP, Jingle.REASON_GENERAL_ERROR);
					}
				}
			}
		}

	}

	class SipClientSideSDPListener implements MediaEventListener<SdpPortManagerEvent> {

		Call _call;

		public SipClientSideSDPListener(Call call) {
			super();
			_call = call;
		}

		public void onEvent(SdpPortManagerEvent event) {
			if (event.isSuccessful() && event.getEventType() == SdpPortManagerEvent.ANSWER_GENERATED) {
				event.getSource().removeListener(this);

				if (LOG.isDebugEnabled()) {
					LOG.debug("GordonSIPServlet SipClientSideSDPListener ANSWER_GENERATED, The SDP saved to session: "
							+ new String(event.getMediaServerSdp()));
				}

				_call.sipSession.setAttribute("SipClientSideSDP", event.getMediaServerSdp());

				try {
					_call.bridge.addLeftListener(new RTMPDSideSDPListener(_call));
					_call.bridge.generateSdpOfferLeft();
				}
				catch (MsControlException e) {
					LOG.error("", e);
					_call.terminate(Side.SIP, Jingle.REASON_MEDIA_ERROR);
				}
			}
		}

	}

	class SipClientSideReInviteSDPListener implements MediaEventListener<SdpPortManagerEvent> {

		SipServletRequest _req;

		public SipClientSideReInviteSDPListener(SipServletRequest req) {
			super();
			_req = req;
		}

		public void onEvent(SdpPortManagerEvent event) {
			if (event.isSuccessful() && event.getEventType() == SdpPortManagerEvent.ANSWER_GENERATED) {
				event.getSource().removeListener(this);
				SipServletResponse resp = _req.createResponse(200);
				try {
					resp.setContent(event.getMediaServerSdp(), "application/sdp");
					resp.send();
				}
				catch (UnsupportedEncodingException e) {
					LOG.error("", e);
				}
				catch (IOException e) {
					LOG.error("", e);
				}
			}
		}

	}
}
