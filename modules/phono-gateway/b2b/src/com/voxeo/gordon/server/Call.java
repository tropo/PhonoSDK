package com.voxeo.gordon.server;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import org.jcouchdb.db.Database;
import org.jcouchdb.document.Document;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.voxeo.servlet.xmpp.XmppServletIQRequest;
import com.voxeo.servlet.xmpp.XmppSession;

public abstract class Call {

	private static final Logger LOG = Logger.getLogger(Call.class.getName());

	public String apiKey;
	
	public String initiator;

	public String jingleSessionId;

	public String xmppAddress;

	public String xmppIPAddress;

	public String sipAddress;

	public String sipIPAddress;
	
	public String unescapedSipAddress;

	public XmppSession xmppSession;

	public SipSession sipSession;

	public Relay relay;

	public Bridge bridge;

	protected State _xmppSideState;

	protected State _sipSideState;

	protected SipServletRequest _initReq;

	Database db;
	Map<String,String> cdr;

	enum State {
		Trying, Answered, Terminated
	}

	enum Side {
		Jingle, SIP, Gateway
	}

	public Call(String initiator, Database db) {
		this.initiator = initiator;

		// create a hash map document with two fields    
		cdr = new HashMap<String, String>();

		this.db = db;
	}

	public void openCDR(Side side) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("openCDR");
		}
		try {
			if (db != null) {
				long ts = (new Date()).getTime();
				cdr.put("type","call");
				cdr.put("sid", jingleSessionId);
				cdr.put("source", side==Side.Jingle?"xmpp":"sip");
				cdr.put("initiator", initiator);
				cdr.put("xmppId", xmppAddress);
				cdr.put("xmppHost", xmppIPAddress);
				cdr.put("sipId", unescapedSipAddress);
				cdr.put("sipHost", sipIPAddress);
				cdr.put("startTs", Long.toString(ts));	
				cdr.put("apiKey", apiKey);

				// create the document in couchdb
				db.createDocument(cdr);
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public void answerCDR() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("answerCDR");
		}
		try {
			if (db != null) {
				long ts = (new Date()).getTime();	
				cdr.put("answerTs", Long.toString(ts));
				db.updateDocument(cdr);
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public void closeCDR(Side side, String reason) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("closeCDR");
		}
		try {
			if (db != null) {
				long ts = (new Date()).getTime();	
				cdr.put("endTs", Long.toString(ts));
				cdr.put("terminateReason", reason);
				cdr.put("terminateSide", side==Side.Jingle?"xmpp":side==Side.SIP?"sip":"gateway");
				db.updateDocument(cdr);
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	// terminate is the real call termination - both normal and in error, in which case both sides must be closed.
	synchronized public void terminate(Side side, String reason) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Terminating call.");
		}

		LOG.debug("_xmppSideState=" + _xmppSideState + "_sipSideState=" + _sipSideState);

		int sipReason;
		if (reason.isEmpty()) {
			reason = Jingle.REASON_SUCCESS;
			if (this instanceof SipToXmppCall && _sipSideState==State.Trying) 
				reason = Jingle.REASON_DECLINE;
			if (this instanceof XmppToSipCall && _sipSideState==State.Trying)
				reason = Jingle.REASON_CANCEL;		
		}
		sipReason = Jingle.JingleToSIPReason(reason);

		if (_xmppSideState != State.Terminated || _sipSideState != State.Terminated) closeCDR(side, reason);

		if (relay != null) {
			relay.Delete();
		}
		relay = null;
		if (bridge != null) {
			bridge.destroy();
		}
		bridge = null;

		if (_xmppSideState == State.Answered || _xmppSideState == State.Trying) {
			Element resElement = DocumentHelper.createElement("iq");
			resElement.addAttribute("type", "set");
			resElement.addAttribute("from", sipAddress);
			resElement.addAttribute("to", xmppAddress);
			resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI))).addAttribute(
					"action", Jingle.ACTION_SESSION_TERMINATE).addAttribute("initiator", initiator).addAttribute("sid",
							jingleSessionId).addElement(new QName("reason")).addElement(new QName(reason));
			XmppServletIQRequest iq;
			try {
				iq = xmppSession.createStanzaIQRequest(resElement, null, null, null, null, null);
				iq.send();
			}
			catch (Exception e) {
				LOG.warn("Exception when sending bye to flash client.");
			}
			_xmppSideState = State.Terminated;
		}



		if (_sipSideState == State.Answered) {
			try {
				SipServletRequest sipreq = sipSession.createRequest("BYE");
				sipreq.addHeader("Reason", "SIP ;cause="+sipReason+" ;text='"+reason+"'");
				sipreq.send();
			}
			catch (Exception e) {
				LOG.warn("Exception when sending bye to sip." + e);
			}
			_sipSideState = State.Terminated;
		}

		if (_sipSideState == State.Trying) {
			if (this instanceof SipToXmppCall) {
				try {

					SipServletResponse sipresp = _initReq.createResponse(sipReason);
					sipresp.send();
				}
				catch (Exception e) {
					LOG.warn("Exception when sending busy to sip." + e);
				}
			} else {
				try {
					SipServletRequest sipreq = _initReq.createCancel();
					sipreq.addHeader("Reason", "SIP ;cause="+sipReason+" ;text='"+reason+"'");
					sipreq.send();
				}
				catch (Exception e) {
					LOG.warn("Exception when sending cancel to sip.");
				}
			}  	
			_sipSideState = State.Terminated;
		}

	}

	public void startCallPeer() throws Exception {

	}

	public void peerAccept() throws Exception {

	}

	public void peerConfirm(byte[] sdp) throws Exception {

	}

	// doBye is a graceful call termination - assuming one side is already down.
	synchronized public void doBye(Side side, String reason) {
		if (_xmppSideState == State.Terminated && _sipSideState == State.Terminated) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Abort bye of already terminated call.");
			}
			return;
		}

		if (side == Side.Jingle) {
			_xmppSideState = State.Terminated;
		}
		else {
			_sipSideState = State.Terminated;
		}

		terminate(side, reason);
	}
}
