package com.voxeo.gordon.server;

import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import com.voxeo.gordon.server.Call.Side;

import org.apache.log4j.Logger;

public class ReleaseResourceSipSessionListener implements SipSessionListener {
  private static final Logger LOG = Logger.getLogger(ReleaseResourceSipSessionListener.class.getName());

  public void sessionCreated(SipSessionEvent sse) {

  }

  public void sessionDestroyed(SipSessionEvent sse) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Invoked SIP sessionDestroyed for session:" + sse.getSession());
    }

    Call theCall = (Call) sse.getSession().getAttribute("Call");

    if (theCall != null) {
      //theCall.terminate(Jingle.REASON_GONE);
    	// The session has gone, so the sip side has already hungup - gracefully shut the xmpp side.
    	theCall.doBye(Side.SIP, Jingle.REASON_GONE);
    }
  }

  public void sessionReadyToInvalidate(SipSessionEvent sse) {

  }

}
