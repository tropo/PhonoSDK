package org.minijingle.jingle.description;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("encryption")
public class Encryption {

    @XStreamAsAttribute
    private final String required;

    private final Crypto crypto;

    public Encryption(final String required, final Crypto crypto) {
        this.required = required;
        this.crypto = crypto;
    }

    public Crypto getCrypto()
    {
		return crypto;
	}

}
