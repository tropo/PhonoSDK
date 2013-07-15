package org.minijingle.jingle.transport;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("candidate")
public class Candidate {

    @XStreamAsAttribute
    private String ip, port, generation, id, component;

    @XStreamAsAttribute
    private String protocol, type,foundation,priority,network;

    public Candidate(String ip, String port, String generation) {
        this.ip = ip;
        this.port = port;
        this.generation = generation;
        this.id = "1";
        this.component = "1";
        this.protocol = "udp";
        this.type = "host";
        this.priority = "2113932031";
        this.network = "1";
        this.foundation ="1";
    }

    public Candidate(){
    	super();
    	component ="1";
    }
    
    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getGeneration() {
        return generation;
    }

    public String getComponent() {
        return component;
    }

    public void setIp(String i) {
         ip = i;
    }

    public void setPort(String p) {
         port = p;
    }

    public void setGeneration(String g) {
         generation = g;
    }
}
