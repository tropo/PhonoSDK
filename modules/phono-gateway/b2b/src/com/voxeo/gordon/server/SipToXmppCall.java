package com.voxeo.gordon.server;

import java.util.Iterator;
import java.util.Vector;

import org.jcouchdb.db.Database;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sdp.SessionName;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.voxeo.gordon.server.Call.State;

public class SipToXmppCall extends Call {

  private static final Logger LOG = Logger.getLogger(SipToXmppCall.class.getName());

  private SdpFactory _sdpFactory;

  public SipToXmppCall(String initiator, Database db, SipServletRequest initReq, SdpFactory sdpFactory) {
    super(initiator, db);
    _initReq = initReq;
    _sdpFactory = sdpFactory;
    _xmppSideState = State.Trying;
    _sipSideState = State.Trying;
  }

  public Vector<Element> getHeaderElements() {
    Vector<Element> headers = new Vector<Element>();
    Iterator<String> itr = _initReq.getHeaderNames();
    while (itr.hasNext()) {
      String headerName = itr.next();
      LOG.debug("HeaderName:" + headerName);
      if (headerName.toLowerCase().startsWith("x-")) {
        Element header = DocumentHelper.createElement("custom-header");
        header.addAttribute("name", headerName);
        header.addAttribute("data", _initReq.getHeader(headerName));
        headers.add(header);
      }
    }
    return headers;
  }

  @Override
  public void peerAccept() throws Exception {
	  SipServletResponse sipresp = _initReq.createResponse(180);
	  sipresp.send();
  }
  
  @Override
  public void peerConfirm(byte[] sessionDescription) throws Exception {
    _xmppSideState = State.Answered;

    answerCDR();
    
    // Generate an OK with some SDP
    String mediaAddress = "";
    String mediaPort = "";
    String codec = "";
    String pt = "";
    String clockrate = "";

    if (relay != null) {
      mediaAddress = relay.localHost;
      mediaPort = relay.localPort;
      codec = relay.codec;
      // pt = relay.payloadType;
      pt = "113";
      clockrate = "/16000";
    }
    else {
      LOG.error("Error allocating relay - sending an error back to the client.");
      throw new Exception("Error allocating relay");
    }

    // Create some SDP
    try {
      SessionDescription sdp = _sdpFactory.createSessionDescription();

      SessionName s = _sdpFactory.createSessionName("-");
      Connection c = _sdpFactory.createConnection("IN", "IP4", mediaAddress);

      int formats[] = {Integer.parseInt(pt),};
      MediaDescription am = _sdpFactory.createMediaDescription("audio", Integer.parseInt(mediaPort), 1, "RTP/AVP",
          formats);
      am.setAttribute("rtpmap", pt + " " + codec + clockrate);
      am.setAttribute("ptime", "20");

      Vector<MediaDescription> mediaDescs = new Vector<MediaDescription>();
      mediaDescs.add(am);
      sdp.setConnection(c);
      sdp.setSessionName(s);

      sdp.setMediaDescriptions(mediaDescs);

      if (LOG.isDebugEnabled()) {
        LOG.debug("SipToXmppCall Constructed SDP to bridge left; " + sdp.toString());
      }

      bridge.processSdpAnswerLeft(sdp.toString().getBytes());

      // send response to sip client.
      SipServletResponse sipresp = _initReq.createResponse(200);
      if (LOG.isDebugEnabled()) {
        LOG.debug("GordonSIPServlet SipClientSideSDPListener ANSWER_GENERATED to sip respones: "
            + new String((byte[]) sipSession.getAttribute("SipClientSideSDP")));
      }

      sipresp.setContent((byte[]) sipSession.getAttribute("SipClientSideSDP"), "application/SDP");
      // send the ok
      sipresp.send();

      _sipSideState = State.Answered;
    }
    catch (Exception e) {
      LOG.error(e);
      throw e;
    }
  }

}
