package org.minijingle.jingle.content;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.minijingle.jingle.description.Description;
import org.minijingle.jingle.transport.RawUdpTransport;

@XStreamAlias("content")
public class Content {

    @XStreamAsAttribute
    private String creator, name, senders;
    @XStreamImplicit(itemFieldName = "description")
    private List<Description> description;
    @XStreamImplicit(itemFieldName = "transport")
    private List<RawUdpTransport> transport;

    //private RawUdpTransport transport;
    //private Description description;
    public Content(String creator, String name, String senders, Object... things) {
        this.creator = creator;
        this.name = name;
        this.senders = senders;

        this.description = new ArrayList<Description>();
        this.transport = new ArrayList<RawUdpTransport>();
        for (Object o : things) {
            if (o instanceof RawUdpTransport) {
                transport.add((RawUdpTransport) o);
            } else if (o instanceof Description) {
                description.add((Description) o);
            }
        }

    }

    public Content(){
    	super();
    }
    
    public String getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    public String getSenders() {
        return senders;
    }

    public Description getDescription() {
        Description ret = null;
        for (Description d : description) {
            if (d.getPayloads() != null) {
                ret = d;
            }
        }
        return ret;
    }

    /*
    public RawUdpTransport getTransport() {
    return transport;
    }

    public Description getDescription() {
    return description;
    }
     *
     */
    public RawUdpTransport getTransport() {
        RawUdpTransport ret = null;
        for (RawUdpTransport t : transport) {
            if (t.getCandidates() != null) {
                ret = t;
            }
        }
        return ret;
    }
}
