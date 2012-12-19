package org.minijingle.jingle.description;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

@XStreamAlias("description")
public class Description {

    @XStreamAsAttribute
    @XStreamAlias("xmlns")
    public final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:1";

    @XStreamAsAttribute
    private  String media;

    @XStreamImplicit
    @XStreamAlias("payload-type")
    private  List<Payload> payloads = new ArrayList<Payload>();

    private Encryption encryption = null;

    public Description(){
    	super();
    }
    public Description(String media) {
        this.media = media;
    }

    public Description(String media, final Encryption encryption) {
        this.media = media;
        this.encryption = encryption;
    }

    public void setEncryption(final Encryption encryption){
        this.encryption = encryption;
    }

    public Encryption getEncryption(){
        return encryption;
    }

    public void addPayload(final Payload payload){
        payloads.add(payload);
    }

    public List<Payload> getPayloads() {
        return payloads;
    }

    public String getMedia() {
        return media;
    }
}

