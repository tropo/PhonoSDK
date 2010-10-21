package com.voxeo.gordon.server;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;

import com.voxeo.servlet.*;
import com.voxeo.servlet.xmpp.*;

import javax.sdp.SdpFactory;
import javax.sdp.Attribute;
import javax.sdp.SessionDescription;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SessionName;
import javax.sdp.Connection;
import javax.sdp.SdpParseException;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class GordonSIPServlet extends SipServlet implements SipSessionListener {

	private static final Logger LOG = Logger.getLogger(GordonSIPServlet.class.getName());
	
	private XmppFactory _xmppFactory;
	private SipFactory _sipFactory;
	private PolicyServer _policyServer;
	private String _rtmpdUri;
	
	@Resource 
	SdpFactory _sdpFactory;
	
	@Override
	public void init() throws ServletException {
		LOG.debug("Gordon: SIP init");
	    // Get a reference to the XMPPFactory.
	    _xmppFactory = (XmppFactory) getServletContext().getAttribute(XmppServlet.XMPP_FACTORY);
	    _sipFactory = (SipFactory) getServletContext().getAttribute(SipServlet.SIP_FACTORY);
 	   	    
		// Read the config to determine the rtmp relay server address
	    _rtmpdUri = getServletConfig().getInitParameter("rtmpdUri");
	    
	    //LOG.debug("Starting policy server");
	    //_policyServer = new PolicyServer(843);
        //_policyServer.start();
	}

	@Override
	public void destroy() {
	    super.destroy();
	}
	
	/**
     * Handle a request to start a call from the SIP side
     * @param request the request to start the call
     */
    protected void doInvite(SipServletRequest request)
        throws ServletException, IOException 
    {
        LOG.info("SIP:Got invite");        
        SipSession session = request.getSession();
        String initiator = request.getFrom().toString();
        Call theCall = new Call(initiator);
        theCall.sipAddress = initiator;
        theCall.jingleSessionId = "1234";
        theCall.sipSession = request.getSession();
        // Allocate a relay
        Relay relay = new Relay(_rtmpdUri);
        theCall.relay = relay;
        
        // Parse the SDP and set the relay destination        
        int remotePort = 0;
		String remoteHost = "";
		int payloadType = 0;
		String codec = "";
		        		
		// Build the description Element
		Element description;       		
		if (theCall.relay != null) {
			description = DocumentHelper.createElement(new QName("description", new Namespace("",Jingle.DESCRIPTION_RTMP_NAMESPACE_URI)));
		} else {
			description = DocumentHelper.createElement(new QName("description", new Namespace("",Jingle.DESCRIPTION_RTP_NAMESPACE_URI)));
		}
		
		// Parse the SDP to get the information we need
    	LOG.info("ContentType: " + request.getContentType());
    	if (request.getContentType().toLowerCase().equals("application/sdp")) {
    		try {
    			LOG.info(request.getContent().toString());
    			SessionDescription sdp = _sdpFactory.createSessionDescription(request.getContent().toString());
    			Connection c = sdp.getConnection();
    			remoteHost = c.getAddress();
    			// Build <description>
    			description.addAttribute("media", "audio");
    			for (Object o : sdp.getMediaDescriptions(false)) {
    				MediaDescription md = (MediaDescription)o;
       				if (md.getMedia().getMediaType().equals("audio")) {
    					// Build <payload-type>'s and add to <description>
    					remotePort = md.getMedia().getMediaPort();
    					Vector<Attribute> attributes = md.getAttributes(false);
    					for (Attribute attribute : attributes) {
    						if (attribute.getName().equals("rtpmap")) {
    							Element payload = DocumentHelper.createElement("payload-type");
    							String[] tokens = attribute.getValue().split(" ");
    							payloadType = Integer.parseInt(tokens[0]);
            					payload.addAttribute("id", tokens[0]);
            					tokens = tokens[1].split("/");
            					codec = tokens[0];
            					payload.addAttribute("name", codec);
            					payload.addAttribute("clockrate", tokens[1]);
            					description.add(payload);
    						}
    					}
    				}        				
    			}
    		
    		} catch (Exception e) {
    			LOG.error("SDP parsing exception: " + e);
    		}

    		// Build the transport Element
    		Element transport;            	
    		if (theCall.relay != null) {
    			theCall.relay.SetDestination(remoteHost, remotePort, payloadType);
    			transport = DocumentHelper.createElement(new QName("transport", new Namespace("",Jingle.TRANSPORT_RTMP)));    	               
    			// Build out transport
    			Element candidate = DocumentHelper.createElement("candidate");
    			candidate.addAttribute("rtmpUri",theCall.relay.rtmpUri);
    			candidate.addAttribute("playName",theCall.relay.playName);
    			candidate.addAttribute("publishName", theCall.relay.publishName);
    			candidate.addAttribute("id","1");
    			transport.add(candidate);
    		} else {        
    			// Build our transport
    			transport = DocumentHelper.createElement(new QName("transport", new Namespace("",Jingle.TRANSPORT_UDPRAW)));
    			Element candidate = DocumentHelper.createElement("candidate");
    			candidate.addAttribute("ip",remoteHost);
    			candidate.addAttribute("port",Integer.toString(remotePort));
    			candidate.addAttribute("id", "1");
    			candidate.addAttribute("generation", "0");
    			candidate.addAttribute("component","1");
    			transport.add(candidate);
    		}
                     
    		Element resElement = DocumentHelper.createElement("iq");
    		resElement.addAttribute("type", "set");
    		resElement.addAttribute("from", theCall.sipAddress);
    		resElement.addAttribute("to", theCall.xmppAddress);
    		Element content = DocumentHelper.createElement("content");
    		content.add(description);
    		content.add(transport);
    		resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI)))
    		.addAttribute("action", Jingle.ACTION_SESSION_INITIATE)
    		.addAttribute("initiator", theCall.initiator)
    		.addAttribute("sid", theCall.jingleSessionId)
    		.add(content);
    		XmppServletStanzaRequest iq = _xmppFactory.createStanzaRequest(theCall.xmppSession, resElement);

    		// Send the initiate
    		iq.send();
    	}
    }   
	
	/**
     * Handle a request to end the call
     * @param request the request to end the call
     */
    protected void doBye(SipServletRequest request)
        throws ServletException, IOException 
    {
        LOG.info("SIP:Got bye");        
        SipSession session = request.getSession();        
        // Kill the jingle call
        // Grab the jingle session
        Call theCall = (Call)request.getSession().getAttribute("Call");
        
        if (theCall != null) {
        	Element resElement = DocumentHelper.createElement("iq");
    	    resElement.addAttribute("type", "set");
    	    resElement.addAttribute("from", theCall.sipAddress);
    	    resElement.addAttribute("to", theCall.xmppAddress);
    	    resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI)))
    	    	.addAttribute("action", Jingle.ACTION_SESSION_TERMINATE)
    	    	.addAttribute("initiator", theCall.initiator)
    	    	.addAttribute("sid", theCall.jingleSessionId)
    	    	.addElement(new QName("reason")).addElement(new QName("success"));
    	    XmppServletStanzaRequest iq = _xmppFactory.createStanzaRequest(theCall.xmppSession, resElement);
    	    
    	    iq.send();
    	    
    	    // Free any relay associated with this call
    	    theCall.relay.Delete();
    	    theCall.relay = null;
        }
        // send an OK for the BYE
        SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
        ok.send();
    }
    
    /**
     * Handle a request to cancel a call in progress
     * @param request the request to end the call
     */
    protected void doCancel(SipServletRequest request) 
        throws ServletException, IOException 
    {
        LOG.info("SIP:Got cancel");
        // Kill the jingle call
        // Grab the jingle session
        Call theCall = (Call)request.getSession().getAttribute("Call");
        
        if (theCall != null) {
        	Element resElement = DocumentHelper.createElement("iq");
    	    resElement.addAttribute("type", "set");
    	    resElement.addAttribute("from", theCall.sipAddress);
    	    resElement.addAttribute("to", theCall.xmppAddress);
    	    resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI)))
    	    	.addAttribute("action", Jingle.ACTION_SESSION_TERMINATE)
    	    	.addAttribute("initiator", theCall.initiator)
    	    	.addAttribute("sid", theCall.jingleSessionId)
    	    	.addElement(new QName("reason")).addElement(new QName("cancel"));
    	    XmppServletStanzaRequest iq = _xmppFactory.createStanzaRequest(theCall.xmppSession, resElement);
    	    
    	    iq.send();
    	    
    	    // Free any relay associated with this call
    	    theCall.relay.Delete();
    	    theCall.relay = null;
        }
        // send an OK for the CANCEL
        SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
        ok.send();
    }
  
    /**
     * Handle an info request 
     * @param request the request to end the call
     */
    protected void doInfo(SipServletRequest request) 
        throws ServletException, IOException 
    {
        LOG.info("SIP:Got info");
        if (request.getContentType().equals("application/dtmf-relay"))
        {
        	String content = (String)request.getContent();
        	StringTokenizer st = new StringTokenizer(content, "=\r\n");
        	String signal = null;
        	String duration = null;
        	while (st.hasMoreTokens()) {
        		String token = st.nextToken();
        		if (token.equals("Signal") && st.hasMoreTokens()) signal = st.nextToken();
        		if (token.equals("Duration") && st.hasMoreTokens()) duration = st.nextToken();
        	}
        	if (signal != null) {
        		Element dtmf = DocumentHelper.createElement(new QName("dtmf", new Namespace("","urn:xmpp:jingle:dtmf:0")));
        		dtmf.addAttribute("code", signal);
        		if (duration != null) dtmf.addAttribute("duration", duration);

        		// Grab the jingle session
        		Call theCall = (Call)request.getSession().getAttribute("Call");

        		if (theCall != null) {
        			Element resElement = DocumentHelper.createElement("iq");
        			resElement.addAttribute("type", "set");
        			resElement.addAttribute("from", theCall.sipAddress);
        			resElement.addAttribute("to", theCall.xmppAddress);
        			resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI)))
        			.addAttribute("action", Jingle.ACTION_SESSION_INFO)
        			.addAttribute("initiator", theCall.initiator)
        			.addAttribute("sid", theCall.jingleSessionId)
        			.add(dtmf);
        			XmppServletStanzaRequest iq = _xmppFactory.createStanzaRequest(theCall.xmppSession, resElement);

        			iq.send();
        		}
        	}
        }
    }
    
    /**
     * Handle a provisional response 1XX
     * @param request the request to end the call
     */
    protected void doProvisionalResponse(SipServletResponse resp) 
        throws ServletException, IOException 
    {
        LOG.info("SIP:Got 1XX");
        if (resp.getStatus() == SipServletResponse.SC_RINGING) {
        	Element ringing = DocumentHelper.createElement(new QName("ringing", new Namespace("","urn:xmpp:jingle:apps:rtp:1:info")));

        	// Grab the jingle session
    		Call theCall = (Call)resp.getSession().getAttribute("Call");
            
            if (theCall != null) {
    			Element resElement = DocumentHelper.createElement("iq");
    			resElement.addAttribute("type", "set");
    			resElement.addAttribute("from", theCall.sipAddress);
    			resElement.addAttribute("to", theCall.xmppAddress);
    			resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI)))
    			.addAttribute("action", Jingle.ACTION_SESSION_INFO)
    			.addAttribute("initiator", theCall.initiator)
    			.addAttribute("sid", theCall.jingleSessionId)
    			.add(ringing);
    			XmppServletStanzaRequest iq = _xmppFactory.createStanzaRequest(theCall.xmppSession, resElement);

    			iq.send();
    		}
        }
    }
    
    /**
     * Handle a success response.  When a call sends an OK message, we 
     * complete the call at the bridge.
     * @param resp the success response
     */  
    protected void doSuccessResponse(SipServletResponse resp)
        throws ServletException, IOException 
    {
        LOG.info("Got OK");

        if (resp.getStatus() == SipServletResponse.SC_OK) {
        	if (!resp.getRequest().isInitial())
        	{
        		return;
        	}
        	
        	// ACK the OK
            resp.createAck().send();   
        	
            // Grab the sessions
        	SipSession session = resp.getSession();
        	Call theCall = (Call)session.getAttribute("Call");
		
        	if (theCall != null) {
        		int remotePort = 0;
        		String remoteHost = "";
        		int payloadType = 0;
        		String codec = "";
        		        		
        		// Build the description Element
        		Element description;       		
        		if (theCall.relay != null) {
        			description = DocumentHelper.createElement(new QName("description", new Namespace("",Jingle.DESCRIPTION_RTMP_NAMESPACE_URI)));
        		} else {
        			description = DocumentHelper.createElement(new QName("description", new Namespace("",Jingle.DESCRIPTION_RTP_NAMESPACE_URI)));
        		}
        		
        		// Parse the SDP to get the information we need
            	LOG.info("ContentType: " + resp.getContentType());
            	if (resp.getContentType().toLowerCase().equals("application/sdp")) {
            		try {
            			LOG.info(resp.getContent().toString());
            			SessionDescription sdp = _sdpFactory.createSessionDescription(resp.getContent().toString());
            			Connection c = sdp.getConnection();
            			remoteHost = c.getAddress();
            			// Build <description>
            			description.addAttribute("media", "audio");
            			for (Object o : sdp.getMediaDescriptions(false)) {
            				MediaDescription md = (MediaDescription)o;
               				if (md.getMedia().getMediaType().equals("audio")) {
            					// Build <payload-type>'s and add to <description>
            					remotePort = md.getMedia().getMediaPort();
            					Vector<Attribute> attributes = md.getAttributes(false);
            					for (Attribute attribute : attributes) {
            						if (attribute.getName().equals("rtpmap")) {
            							Element payload = DocumentHelper.createElement("payload-type");
            							String[] tokens = attribute.getValue().split(" ");
            							payloadType = Integer.parseInt(tokens[0]);
                    					payload.addAttribute("id", tokens[0]);
                    					tokens = tokens[1].split("/");
                    					codec = tokens[0];
                    					payload.addAttribute("name", codec);
                    					payload.addAttribute("clockrate", tokens[1]);
                    					description.add(payload);
            						}
            					}
            				}        				
            			}
            		
            		} catch (Exception e) {
            			LOG.error("SDP parsing exception: " + e);
            		}

            		// Build the transport Element
            		Element transport;            	
            		if (theCall.relay != null) {
            			theCall.relay.SetDestination(remoteHost, remotePort, payloadType);
            			transport = DocumentHelper.createElement(new QName("transport", new Namespace("",Jingle.TRANSPORT_RTMP)));    	               
            			// Build out transport
            			Element candidate = DocumentHelper.createElement("candidate");
            			candidate.addAttribute("rtmpUri",theCall.relay.rtmpUri);
            			candidate.addAttribute("playName",theCall.relay.playName);
            			candidate.addAttribute("publishName", theCall.relay.publishName);
            			candidate.addAttribute("id","1");
            			transport.add(candidate);
            		} else {        
            			// Build our transport
            			transport = DocumentHelper.createElement(new QName("transport", new Namespace("",Jingle.TRANSPORT_UDPRAW)));
            			Element candidate = DocumentHelper.createElement("candidate");
            			candidate.addAttribute("ip",remoteHost);
            			candidate.addAttribute("port",Integer.toString(remotePort));
            			candidate.addAttribute("id", "1");
            			candidate.addAttribute("generation", "0");
            			candidate.addAttribute("component","1");
            			transport.add(candidate);
            		}
                             
            		Element resElement = DocumentHelper.createElement("iq");
            		resElement.addAttribute("type", "set");
            		resElement.addAttribute("from", theCall.sipAddress);
            		resElement.addAttribute("to", theCall.xmppAddress);
            		Element content = DocumentHelper.createElement("content");
            		content.add(description);
            		content.add(transport);
            		resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI)))
            		.addAttribute("action", Jingle.ACTION_SESSION_ACCEPT)
            		.addAttribute("initiator", theCall.initiator)
            		.addAttribute("sid", theCall.jingleSessionId)
            		.add(content);
            		XmppServletStanzaRequest iq = _xmppFactory.createStanzaRequest(theCall.xmppSession, resElement);

            		// Send the accept
            		iq.send();
            	}
        	}
                 
        }
    }
    
    /**
     * Handle an error response.  Close the relevant call at the bridge.
     * @param resp the error response
     */
    protected void doErrorResponse(SipServletResponse resp) 
        throws ServletException, IOException 
    {
        LOG.info("SIP:Got error");
        
        // Grab the jingle session
		Call theCall = (Call)resp.getSession().getAttribute("Call");
        
        if (theCall != null) {
        	String reason="";
        	switch (resp.getStatus()) {
        	case SipServletResponse.SC_BUSY_HERE: reason = Jingle.REASON_BUSY;break;
        	case SipServletResponse.SC_BUSY_EVERYWHERE: reason = Jingle.REASON_BUSY;break;
        	case SipServletResponse.SC_BAD_IDENTITY_INFO:reason = Jingle.REASON_SECURITY_ERROR;break;
        	case SipServletResponse.SC_DECLINE:reason = Jingle.REASON_DECLINE;break;
        	case SipServletResponse.SC_FORBIDDEN:reason = Jingle.REASON_SECURITY_ERROR;break;
        	case SipServletResponse.SC_UNAUTHORIZED:reason = Jingle.REASON_SECURITY_ERROR;break;
        	case SipServletResponse.SC_SERVER_TIMEOUT:reason = Jingle.REASON_EXPIRED;break;        	
        	default:reason = Jingle.REASON_GENERAL_ERROR;break;
        	}
        	        	
			Element resElement = DocumentHelper.createElement("iq");
			resElement.addAttribute("type", "set");
			resElement.addAttribute("from", theCall.sipAddress);
			resElement.addAttribute("to", theCall.xmppAddress);
			resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI)))
			.addAttribute("action", Jingle.ACTION_SESSION_TERMINATE)
			.addAttribute("initiator", theCall.initiator)
			.addAttribute("sid", theCall.jingleSessionId)
			.addElement(new QName("reason")).addElement(new QName(reason)).addElement(new QName("text")).setData(resp.getReasonPhrase());
			XmppServletStanzaRequest iq = _xmppFactory.createStanzaRequest(theCall.xmppSession, resElement);

			iq.send();
			
			// Free any relay
			if (theCall.relay != null) {
				theCall.relay.Delete();
				theCall.relay = null;
			}
		}	
        
        // cancel the call
        resp.getSession().invalidate();
    }
	
	////////
    // SipSessionListener interface
    
    public void sessionCreated(SipSessionEvent sse) {
        LOG.info("SIP:Session created");
    }

    public void sessionDestroyed(SipSessionEvent sse) {
        LOG.info("SIP:Session destroyed");
    }
    
    public void sessionReadyToInvalidate(SipSessionEvent sse) {
    	LOG.info("SIP:Ready to invalidate");
    }
}
