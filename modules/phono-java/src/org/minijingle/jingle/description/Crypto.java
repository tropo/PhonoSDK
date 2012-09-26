package org.minijingle.jingle.description;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("crypto")
public class Crypto {

    @XStreamAsAttribute
    @XStreamAlias("crypto-suite")
    private final String cryptoSuite;

    @XStreamAsAttribute
    @XStreamAlias("key-params")
    private final String keyParams;

	@XStreamAsAttribute
	@XStreamAlias("session-params")
    private final String sessionParams;

	@XStreamAsAttribute
    private final String tag = "1";

    public Crypto(String cryptoSuite, String keyParams, String sessionParams) {
        this.cryptoSuite = cryptoSuite;
        this.keyParams = keyParams;
        this.sessionParams = sessionParams;
    }

    public String getCryptoSuite() {return  cryptoSuite; }
    public String getKeyParams() {return  keyParams; }
    public String getSessionParams() {return  sessionParams; }

}
