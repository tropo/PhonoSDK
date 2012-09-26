package org.minijingle.jingle.transport;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

@XStreamAlias("transport")
public class RawUdpTransport {

    @XStreamAsAttribute
    @XStreamAlias("xmlns")
    public final String NAMESPACE = "urn:xmpp:jingle:transports:raw-udp:1";

    @XStreamImplicit
    @XStreamAlias("candidate")
    private final ArrayList<Candidate> candidates = new ArrayList<Candidate>();

    @XStreamAsAttribute
    private String pwd , ufrag;

    public RawUdpTransport(Candidate candidate) {
        this.candidates.add(candidate);
    }

    public RawUdpTransport(Candidate candidate, String pwd, String ufrag) {
        this.candidates.add(candidate);
        this.ufrag = ufrag;
        this.pwd = pwd;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public String getPwd() {
        return pwd;
    }

    public String getUfrag() {
        return ufrag;
    }
}
