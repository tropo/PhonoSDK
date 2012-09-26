package org.minijingle.jingle.reason;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("reason")
public class Reason {

    private Success success;
    private Busy busy;

    public Reason(Success success) {
        this.success = success;
    }

    public Reason(Busy busy) {
        this.busy = busy;
    }

}
