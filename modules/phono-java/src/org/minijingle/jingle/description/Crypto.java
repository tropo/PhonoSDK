package org.minijingle.jingle.description;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("crypto")
public class Crypto {

    @XStreamAsAttribute
    @XStreamAlias("crypto-suite")
    private  String cryptoSuite;

    @XStreamAsAttribute
    @XStreamAlias("key-params")
    private  String keyParams;

	@XStreamAsAttribute
	@XStreamAlias("session-params")
    private  String sessionParams;

	@XStreamAsAttribute
    private  String tag = "1";

    public Crypto(String cryptoSuite, String keyParams, String sessionParams) {
        this.cryptoSuite = cryptoSuite;
        this.keyParams = keyParams;
        this.sessionParams = sessionParams;
    }

    public Crypto(){
    	super();
    }
    public String getCryptoSuite() {return  cryptoSuite; }
    public String getKeyParams() {return  keyParams; }
    public String getSessionParams() {return  sessionParams; }
    public void setKeyParams(String k){
    	keyParams =k;
    }
    public void setSessionParams(String s){
    	sessionParams =s;
    }
    public void setCryptoSuite(String c){
    	cryptoSuite =c;
    }
}
