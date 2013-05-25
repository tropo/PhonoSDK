package org.minijingle.jingle;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.minijingle.jingle.content.Content;
import org.minijingle.jingle.reason.Reason;
import org.minijingle.jingle.ringing.Ringing;
import org.minijingle.xmpp.smack.parser.XStreamIQ;

@XStreamAlias("jingle")
public class Jingle {

    public final static String SESSION_INITIATE = "session-initiate";
    public final static String SESSION_TERMINATE = "session-terminate";
    public final static String SESSION_ACCEPT = "session-accept";
    public final static String SESSION_INFO = "session-info";
    @XStreamAsAttribute
    @XStreamAlias("xmlns")
    public final String NAMESPACE = "urn:xmpp:jingle:1";
    public static final String XMLNS = "urn:xmpp:jingle:1";
    @XStreamAsAttribute
    private String action, sid, initiator, responder;
    private Content content;
    private Reason reason;
    private String from, to;
    private Ringing ringing;
    @XStreamImplicit(itemFieldName = "custom-header")
    private List<CustomHeader>  customHeaders;

    public Jingle(){
    	super();
    }
    public Jingle(String sid, String initiator, String responder, String action) {
        this( sid,  initiator,  responder,  action,null);
    }

    public Jingle(String sid, String initiator, String responder, String action,Hashtable headers) {
        this.sid = sid;
        this.initiator = initiator;
        this.responder = responder;
        this.action = action;
        if (headers != null){
            Enumeration ke = headers.keys();
            this.customHeaders = new ArrayList<CustomHeader>();
            while (ke.hasMoreElements()){
                String n = (String)ke.nextElement();
                String v = (String)headers.get(n);
                CustomHeader ch = new CustomHeader(n,v);
                customHeaders.add(ch);
            }
        }
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public Content getContent() {
        return content;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public void setRinging(Ringing r) {
        this.ringing = r;
    }

    public Ringing getRinging() {
        return ringing;
    }

    public String getSid() {
        return sid;
    }

    public String getInitiator() {
        return initiator;
    }

    public String getResponder() {
        return responder;
    }

    public String getAction() {
        return action;
    }

    public String toString() {
        return XStreamIQ.getStream().toXML(this);
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
