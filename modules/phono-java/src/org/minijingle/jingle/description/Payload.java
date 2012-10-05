package org.minijingle.jingle.description;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("payload-type")
public class Payload {

    @XStreamAsAttribute
    private  String id, name;

    @XStreamAsAttribute
    private int clockrate;

    public Payload(){
    	super();
    }
    
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

    public void setId(String i) {
        id = i;
    }

    public void  setName(String n) {
         name = n;
    }

    public void setClockrate(int c) {
        clockrate = c;
    }
    
}
