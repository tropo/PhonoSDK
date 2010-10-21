package com.voxeo.gordon.server;

import java.util.Vector;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.servlet.sip.SipServletRequest;

import org.jcouchdb.db.Database;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServletIQRequest;

public class XmppToSipCall extends Call {

  private static final Logger LOG = Logger.getLogger(XmppToSipCall.class.getName());

  private XmppFactory _xmppFactory;

  private SdpFactory _sdpFactory;
  
  public XmppToSipCall(String initiator, Database db, XmppFactory xmppFactory, SdpFactory sdpFactory, SipServletRequest initReq) {
    super(initiator, db);

    _xmppFactory = xmppFactory;
    _sdpFactory = sdpFactory;
    _initReq = initReq;
    _xmppSideState = State.Trying;
    _sipSideState = State.Trying;
  }

  @Override
  public void peerAccept() throws Exception {
	  Element ringing = DocumentHelper.createElement(new QName("ringing", new Namespace("",
      	"urn:xmpp:jingle:apps:rtp:1:info")));
	  Element resElement = DocumentHelper.createElement("iq");
      resElement.addAttribute("type", "set");
      resElement.addAttribute("from", sipAddress);
      resElement.addAttribute("to", xmppAddress);
      resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI))).addAttribute(
          "action", Jingle.ACTION_SESSION_INFO).addAttribute("initiator", initiator).addAttribute("sid",
          jingleSessionId).add(ringing);
      XmppServletIQRequest iq = xmppSession.createStanzaIQRequest(resElement, null, null, null, null, null);
      iq.send();
  }
  
  @Override
  public void peerConfirm(byte[] sessionDescription) throws Exception {
    _sipSideState = State.Answered;

    answerCDR();
    
    try {
      bridge.processSdpAnswerRight(sessionDescription);
    }
    catch (Exception e1) {
      LOG.error("", e1);
      throw e1;
    }

    SessionDescription sdp = _sdpFactory.createSessionDescription(new String((byte[]) sipSession
        .getAttribute("RTMPDSideSDP")));

    int remotePort = 0;
    String remoteHost = "";
    int payloadType = 0;
    String codec = "";

    // Build the description Element
    Element description;
    if (relay != null) {
      description = DocumentHelper.createElement(new QName("description", new Namespace("",
          Jingle.DESCRIPTION_RTMP_NAMESPACE_URI)));
    }
    else {
      description = DocumentHelper.createElement(new QName("description", new Namespace("",
          Jingle.DESCRIPTION_RTP_NAMESPACE_URI)));
    }

    try {
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
              //String searchCodec = relay.codec.split("/")[0];
              if (relay != null && attribute.getValue().toLowerCase().contains(relay.codec.split("/")[0]))
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

    }
    catch (Exception e) {
      LOG.error("SDP parsing exception: " + e);
      throw e;
    }

    // Build the transport Element
    Element transport;
    if (relay != null) {
      relay.SetDestination(remoteHost, remotePort, payloadType);
      transport = DocumentHelper.createElement(new QName("transport", new Namespace("", Jingle.TRANSPORT_RTMP)));
      // Build out transport
      Element candidate = DocumentHelper.createElement("candidate");
      candidate.addAttribute("rtmpUri", relay.rtmpUri);
      candidate.addAttribute("playName", relay.playName);
      candidate.addAttribute("publishName", relay.publishName);
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
    resElement.addAttribute("from", sipAddress);
    resElement.addAttribute("to", xmppAddress);
    Element content = DocumentHelper.createElement("content");
    content.addAttribute("name", "Audio");
    content.add(description);
    content.add(transport);
    resElement.addElement(new QName(Jingle.ELEMENT_NAME, new Namespace("", Jingle.NAMESPACE_URI))).addAttribute(
        "action", Jingle.ACTION_SESSION_ACCEPT).addAttribute("initiator", initiator).addAttribute("sid",
        jingleSessionId).add(content);

    XmppServletIQRequest iq = xmppSession.createStanzaIQRequest(resElement, null, null, null, null, null);

    // Send the accept
    iq.send();

    _xmppSideState = State.Answered;
  }

  public void sessionTerminate() {

  }
}
