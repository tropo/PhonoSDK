package org.minijingle.jingle.transport;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

@XStreamAlias("transport")
public class Transport {

    @XStreamAsAttribute
    @XStreamAlias("xmlns")
    public  String NAMESPACE = "urn:xmpp:jingle:transports:undefined";

    @XStreamImplicit
    @XStreamAlias("candidate")
    private  ArrayList<Candidate> candidates = new ArrayList<Candidate>();

    //@XStreamImplicit
    @XStreamAlias("fingerprint")
    private  Fingerprint fingerprint;

    @XStreamAsAttribute
    private String pwd;

    @XStreamAsAttribute
    private String ufrag;

    public Transport(Candidate candidate) {
        this.candidates.add(candidate);
    }

    public Transport(Candidate candidate, String pwd, String ufrag) {
        this.candidates.add(candidate);
        this.ufrag = ufrag;
        this.pwd = pwd;
    }

    public Transport(){
    	
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
    
    public void setCandidate(Candidate c) {
         candidates.add(c);
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(Fingerprint f) {
        fingerprint = f;
    }

    public void setPwd(String p) {
        pwd = p;
    }

    public void setUfrag(String u) {
         ufrag = u;
    }    
}
