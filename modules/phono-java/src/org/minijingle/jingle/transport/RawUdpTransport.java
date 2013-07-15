package org.minijingle.jingle.transport;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

@XStreamAlias("transport")
public class RawUdpTransport extends Transport {

    public RawUdpTransport(Candidate candidate) {
        super(candidate);
        NAMESPACE = "urn:xmpp:jingle:transports:raw-udp:1";
    }

    public RawUdpTransport(Candidate candidate, String pwd, String ufrag) {
        super(candidate, pwd, ufrag);
        NAMESPACE = "urn:xmpp:jingle:transports:raw-udp:1";
    }

    public RawUdpTransport(){
    }
}
