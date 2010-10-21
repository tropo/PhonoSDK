package com.voxeo.gordon.server;

import javax.servlet.sip.SipServletResponse;

public class Jingle {
	public static String ELEMENT_NAME = "jingle";
	public static String NAMESPACE_URI = "urn:xmpp:jingle:1";
	public static String NAMESPACE_TMPURI = "urn:xmpp:tmp:jingle";
	public static String DESCRIPTION_RTP_NAMESPACE_URI = "urn:xmpp:jingle:apps:rtp:1";
	public static String DESCRIPTION_RTMP_NAMESPACE_URI = "http://voxeo.com/gordon/apps/rtmp";
	public static String CUSTOMHEADER_NAMESPACE_URI = "http://voxeo.com/gordon/ext/header";
	public static String TRANSPORT_UDPRAW = "urn:xmpp:jingle:transports:raw-udp:1";
	public static String TRANSPORT_RTMP = "http://voxeo.com/gordon/transports/rtmp";
	
	public static String ACTION_CONTENT_ACCEPT = "content-accept";
	public static String ACTION_CONTENT_ADD = "content-add";
	public static String ACTION_CONTENT_MODIFY = "content-modify";
	public static String ACTION_CONTENT_REJECT = "content-reject";
	public static String ACTION_DESCRIPTION_INFO = "description-info";
	public static String ACTION_SECURITY_INFO = "security-info";
	public static String ACTION_SESSION_ACCEPT = "session-accept";
	public static String ACTION_SESSION_INFO = "session-info";
	public static String ACTION_SESSION_INITIATE = "session-initiate";
	public static String ACTION_SESSION_TERMINATE = "session-terminate";
	public static String ACTION_TRANSPORT_ACCEPT = "transport-accept";
	public static String ACTION_TRANSPORT_INFO = "transport-info";
	public static String ACTION_TRANSPORT_REJECT = "transport-reject";
	public static String ACTION_TRANSPORT_REPLACE = "transport-replace";

	public static String REASON_ALTERNATIVE_SESSION = "alternative-session";
	public static String REASON_BUSY = "busy";
	public static String REASON_CANCEL = "cancel";
	public static String REASON_CONNECTIVITY_ERROR = "connectivity-error";
	public static String REASON_DECLINE = "decline";
	public static String REASON_EXPIRED = "expired";
	public static String REASON_FAILED_APPLICATION = "failed-application";
	public static String REASON_FAILED_TRANSPORT = "failed-transport";
	public static String REASON_GENERAL_ERROR = "general-error";
	public static String REASON_GONE = "gone";
	public static String REASON_INCOMPATIBLE_PARAMETERS = "incompatible-parameters";
	public static String REASON_MEDIA_ERROR = "media-error";
	public static String REASON_SECURITY_ERROR = "security-error";
	public static String REASON_SUCCESS = "success";
	public static String REASON_TIMEOUT = "timeout";
	public static String REASON_UNSUPPORTED_APPLICATIONS = "unsupported-applications";
	public static String REASON_UNSUPPORTED_TRANSPORTS = "unsupported-transports";
	
	static public String SIPToJingleReason(int sipReason)
	{
		String reason;
		switch (sipReason) {
		case SipServletResponse.SC_BUSY_HERE:
			reason = Jingle.REASON_BUSY;
			break;
		case SipServletResponse.SC_BUSY_EVERYWHERE:
			reason = Jingle.REASON_BUSY;
			break;
		case SipServletResponse.SC_BAD_IDENTITY_INFO:
			reason = Jingle.REASON_SECURITY_ERROR;
			break;
		case SipServletResponse.SC_DECLINE:
			reason = Jingle.REASON_DECLINE;
			break;
		case SipServletResponse.SC_FORBIDDEN:
			reason = Jingle.REASON_SECURITY_ERROR;
			break;
		case SipServletResponse.SC_UNAUTHORIZED:
			reason = Jingle.REASON_SECURITY_ERROR;
			break;
		case SipServletResponse.SC_SERVER_TIMEOUT:
			reason = Jingle.REASON_EXPIRED;
			break;
		default:
			reason = Jingle.REASON_GENERAL_ERROR;
			break;
		}
		return reason;
	}

	static public int JingleToSIPReason(String reason)
	{
		int sipReason;
		
		if (reason.contains(Jingle.REASON_BUSY)) sipReason = SipServletResponse.SC_BUSY_HERE;
		else if (reason.contains(Jingle.REASON_GONE)) sipReason = SipServletResponse.SC_GONE;
		else if (reason.contains(Jingle.REASON_DECLINE)) sipReason = SipServletResponse.SC_DECLINE;
		else if (reason.contains(Jingle.REASON_CONNECTIVITY_ERROR)) sipReason = SipServletResponse.SC_SERVER_INTERNAL_ERROR;
		else if (reason.contains(Jingle.REASON_GENERAL_ERROR)) sipReason = SipServletResponse.SC_SERVER_INTERNAL_ERROR;
		else if (reason.contains(Jingle.REASON_TIMEOUT)) sipReason = SipServletResponse.SC_SERVER_TIMEOUT;
		else if (reason.contains(Jingle.REASON_EXPIRED)) sipReason = SipServletResponse.SC_SERVER_TIMEOUT;
		else if (reason.contains(Jingle.REASON_INCOMPATIBLE_PARAMETERS)) sipReason = SipServletResponse.SC_NOT_ACCEPTABLE_HERE;
		else if (reason.contains(Jingle.REASON_SECURITY_ERROR)) sipReason = SipServletResponse.SC_UNAUTHORIZED;
		else if (reason.contains(Jingle.REASON_UNSUPPORTED_APPLICATIONS)) sipReason = SipServletResponse.SC_NOT_ACCEPTABLE_HERE;
		else if (reason.contains(Jingle.REASON_UNSUPPORTED_TRANSPORTS)) sipReason = SipServletResponse.SC_NOT_ACCEPTABLE_HERE;
		else sipReason = SipServletResponse.SC_ACCEPTED; // Default if no good reason given
		
		return sipReason;
	}
}
