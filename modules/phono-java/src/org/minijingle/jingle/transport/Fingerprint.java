package org.minijingle.jingle.transport;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

/*
<fingerprint xmlns='urn:xmpp:tmp:jingle:apps:dtls:0' hash='sha-256' required='true'>
  02:1A:CC:54:27:AB:EB:9C:53:3F:3E:4B:65:2E:7D:46:3F:54:42:CD:54:F1:7A:03:A2:7D:F9:B0:7F:46:19:B2
</fingerprint>
*/

@XStreamAlias("finerprint")
@XStreamConverter(value=ToAttributedValueConverter.class, strings={"print"})
public class Fingerprint {
    @XStreamAsAttribute
    @XStreamAlias("xmlns")
    private String xmlns = "urn:xmpp:tmp:jingle:apps:dtls:0";

    @XStreamAsAttribute
    private String hash = "sha-256";
    @XStreamAsAttribute
    private String required = "true";

    private String print;

    public Fingerprint(String hash, String print, String requried) {
        this.hash = hash;
        this.print = print;
        this.required = required;
    }

    public Fingerprint(String print){
    	this.print = print;
    }
    
    public String getHash() {
        return hash;
    }

    public String getPrint() {
        return print;
    }

    public String getRequired() {
        return required;
    }
}
