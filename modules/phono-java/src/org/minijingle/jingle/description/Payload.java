package org.minijingle.jingle.description;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("payload-type")
public class Payload {

    @XStreamAsAttribute
    private final String id, name;

    @XStreamAsAttribute
    private int clockrate;

    public Payload(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Payload(String id, String name, int clockrate) {
        this.id = id;
        this.name = name;
        this.clockrate = clockrate;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getClockrate() {
        return clockrate;
    }
}
