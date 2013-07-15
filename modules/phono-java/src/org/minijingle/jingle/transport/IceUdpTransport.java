package org.minijingle.jingle.transport;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("transport")
public class IceUdpTransport extends Transport {


    
    public IceUdpTransport(Candidate candidate) {
        super(candidate);
        
        NAMESPACE = "urn:xmpp:jingle:transports:ice-udp:1";
    }

    public IceUdpTransport(Candidate candidate, String pwd, String ufrag) {
        super(candidate, pwd, ufrag);

        NAMESPACE = "urn:xmpp:jingle:transports:ice-udp:1";
    }

    public IceUdpTransport(){
    	
    }
}
