package com.voxeo.gordon.server;

import java.util.Date;

import java.util.Enumeration;
import java.util.Map;

import org.apache.log4j.Logger;

import com.voxeo.gordon.server.Call.Side;
import com.voxeo.servlet.xmpp.XmppSession;
import com.voxeo.servlet.xmpp.XmppSessionEvent;
import com.voxeo.servlet.xmpp.XmppSessionListener;
import org.jcouchdb.db.Database;

public class ReleaseResourceXmppSessionListener implements XmppSessionListener {
	private static final Logger LOG = Logger.getLogger(ReleaseResourceXmppSessionListener.class.getName());

	public void sessionCreated(XmppSessionEvent event) {

	}

	private void closeSessionCDR(XmppSession session) {
		Map<String,String> sdr;
		Database db;
		db = (Database)session.getServletContext().getAttribute("phonoDb");
		LOG.debug("Got db = " + db);
		if (db != null) {
			sdr = (Map<String,String>)session.getAttribute("sessionSDR");
			LOG.debug("Got sdr = " + sdr);
			if (sdr != null) {
				long ts = (new Date()).getTime();	
				sdr.put("endTs", Long.toString(ts));
				db.updateDocument(sdr);
			}
		}
	}

	public void sessionDestroyed(XmppSessionEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Invoked XMPP sessionDestroyed for session:" + event.getSession());
		}

		closeSessionCDR(event.getSession());

		Enumeration<String> attributes = event.getSession().getAttributeNames();
		while (attributes.hasMoreElements()) {
			Object o = event.getSession().getAttribute(attributes.nextElement());
			LOG.debug("Got object:" + o);
			if (o instanceof Call) {
				LOG.debug("Is a call.");
				Call theCall = (Call) o;
				//theCall.terminate(Jingle.REASON_GONE);
				// The xmpp side has gone, along with any calls, so gracefully close the sip side.
				theCall.doBye(Side.Jingle, Jingle.REASON_GONE);
			}
		}
	}
}
