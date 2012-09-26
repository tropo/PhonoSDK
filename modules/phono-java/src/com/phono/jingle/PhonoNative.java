/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.jingle;

import com.phono.api.Codec;
import com.phono.api.CodecList;
import com.phono.api.Share;
import com.phono.api.faces.PhonoMessagingFace;
import com.phono.api.faces.PhonoNativeFace;
import com.phono.api.faces.PhonoPhoneFace;
import com.phono.applet.audio.phone.PhonoAudioShim;
import com.phono.rtp.Endpoint;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;
import org.minijingle.jingle.Jingle;
import org.minijingle.xmpp.smack.JingleProvider;
import com.phono.srtplight.Log;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import org.jivesoftware.smack.XMPPException;
import org.minijingle.jingle.description.Payload;

/**
 *
 * @author tim
 */
public class PhonoNative implements PhonoNativeFace {

    static String NS_JINGLE = "urn:xmpp:jingle:1";
    static String NS_JINGLE_RTP = "urn:xmpp:jingle:apps:rtp:1";
    static String NS_JINGLE_UDP = "urn:xmpp:jingle:transports:raw-udp:1";
    static String NS_PHONOEMPTY = "";
    static String NS_JABBER = "jabber:client";
    static String NS_RTMP = "http://voxeo.com/gordon/apps/rtmp";
    static String NS_RTMPT = "http://voxeo.com/gordon/transports/rtmp";
    static String SERVICEUNAVAIL = "<error type='cancel'><service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>";
    protected String _apiKey;
    protected String _sessionID;
    protected String myJID; // not exposed for now
    PhonoPhoneFace _phone;
    PhonoMessagingFace _messaging;
    private boolean _connected;
    private XMPPConnection _xmppConnection;
    private final PhonoAudioShim _audio;
    private final CodecList _codecList;
    final private Hashtable _endpoints = new Hashtable();

    public PhonoNative(PhonoPhoneFace p, PhonoMessagingFace m) {
        this();
        setPhone(p);
        setMessaging(m);
    }

    public PhonoNative(String domain, PhonoPhoneFace p, PhonoMessagingFace m) {
        this(domain);
        setPhone(p);
        setMessaging(m);
    }

    public PhonoNative() {
        this("app.phono.com");
    }

    public PhonoNative(String domain) {
        _audio = new PhonoAudioShim();
        _codecList = new CodecList(_audio);
        ProviderManager.getInstance().addIQProvider("jingle", Jingle.XMLNS, new JingleProvider());
        ConnectionConfiguration cc = new ConnectionConfiguration(domain);
        if(Log.getLevel() >= Log.DEBUG) {
            cc.setDebuggerEnabled(true);
        }
        _xmppConnection = new XMPPConnection(cc);

    }

    public void setApiKey(String k) {
        _apiKey = k;
    }

    public String getApiKey() {
        return _apiKey;
    }

    public String getSessionID() {
        return _sessionID;
    }

    public void setPhone(PhonoPhoneFace p) {
        _phone = p;
        ((PhonoPhone) p).setPhonoNative(this);
    }

    public PhonoPhoneFace getPhone() {
        return _phone;
    }

    public void setMessaging(PhonoMessagingFace m) {
        _messaging = m;
        ((PhonoMessaging) m).setPhonoNative(this);
    }

    public PhonoMessagingFace getMessaging() {
        return _messaging;
    }

    public void connect() {
        final PhonoNativeFace nat = this;
        try {
            ConnectionListener natlist = new ConnectionListener() {

                public void connectionClosed() {
                    Log.debug("connection Closed");
                    nat.onUnready();
                }

                public void connectionClosedOnError(Exception e) {
                    Log.debug("connection ClosedonError");
                    nat.onError();
                    nat.onUnready();
                }

                public void reconnectingIn(int seconds) {
                    Log.debug("connection Reconnecting in" + seconds);
                    nat.onUnready();
                }

                public void reconnectionSuccessful() {
                    Log.debug("connection Reconnected");
                    nat.onReady();
                }

                public void reconnectionFailed(Exception e) {
                    Log.debug("Reconnection failed");
                    nat.onError();
                }
            };

            Log.debug("connecting");
            _xmppConnection.connect();
            _xmppConnection.addConnectionListener(natlist);

            Log.debug("connected");
            _xmppConnection.loginAnonymously();


            Log.debug("Connection ID " + _xmppConnection.getConnectionID());
            Log.debug("Temp User ID " + _xmppConnection.getUser());
            this._sessionID = _xmppConnection.getUser().split("/")[0];
            nat.onReady();
        } catch (XMPPException ex) {
            nat.onError();
        }
    }

    public void disconnect() {
        if (_xmppConnection.isConnected()) {
            _xmppConnection.disconnect();
            this.onUnready();
        }
    }

    public boolean isConnected() {
        return _connected;
    }

    /**
     * Override this method to get onReady notifications
     */
    public void onReady() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Override this method to get onUnready notifications
     */
    public void onUnready() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Override this method to get onError notifications
     */
    public void onError() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void sendPacket(IQ pack) {
        _xmppConnection.sendPacket(pack);
    }

    void addPacketListener(PacketListener pli, PacketFilter pf) {
        _xmppConnection.addPacketListener(pli, pf);
    }

    public List<Payload> getPayloads() {
        Codec[] codecs = getCodecs();
        List<Payload> payloads = new ArrayList<Payload>();

        for (Codec codec : codecs) {
            Payload payload = new Payload(String.valueOf(codec.pt), codec.name, codec.rate);
            payloads.add(payload);
        }
        return payloads;
    }

    public Share mkShare(String uri, Payload pay) {
        Codec codec = null;
        Codec[] codecs = getCodecs();
        int payid = Integer.parseInt(pay.getId());
        for (Codec c : codecs) {
            if ((c.name.equals(pay.getName())) && (c.rate == pay.getClockrate())) {
                if (payid != c.pt) {
                    codec = new Codec(payid, c.name, c.rate, c.ptime, c.iaxcn);
                } else {
                    codec = c;
                }
                break;
            }
        }
        return share(uri, codec);
    }

    public void terminate() {
        _audio.destroy();
    }

    public void allocateEndpoint(String uri) {
        Endpoint endpoint = new Endpoint(uri);
        _endpoints.put(endpoint.getLocalURI(), endpoint);
    }

    public void freeEndpoint(String uri) {
        synchronized (_endpoints) {
            Endpoint e = (Endpoint) _endpoints.get(uri);

            if (e != null) {
                e.release();
            }
            _endpoints.remove(e);
        }
    }

    public Codec[] getCodecs() {
        return _codecList.getCodecs();
    }

    public Share share(String uri, Codec codec) {
        Properties spl = null;
        Properties spr = null;

        return share(uri, codec, spl, spr);
    }

    public Share share(String uri, Codec codec, Properties spl, Properties spr) {
        // need to fix this for inbound at some point...


        Share ret = null;
        Share share = null;
        boolean autoStart = false;


        if (codec != null) {
            Log.debug("in share() codec = " + codec.name + " rate =" + codec.rate + " pt = " + codec.pt);
            Log.debug("in share() uri = " + uri);

            try {

                PhonoAudioShim af = getAudio(codec);
                share = new Share(uri, af, codec.pt, spl, spr);
                _audio.init(codec.iaxcn, 100); // todo fix rtt
                String luri = share.getLocalURI();

                synchronized (_endpoints) {
                    Endpoint endpoint = (Endpoint) _endpoints.get(luri);

                    if (endpoint != null) {
                        endpoint.setShare(share);
                    } else {
                        endpoint = new Endpoint(luri);
                        endpoint.setShare(share);
                        Log.debug("Unexpected local endpoint used :" + luri);
                        _endpoints.put(luri, endpoint);

                    }
                }
                // should check auto start here..

                if (autoStart) {
                    share.start();
                }
                ret = share; // only return the share if no exceptions ....

            } catch (Exception ex) {
                if (share != null) {
                    share.stop(); // minimal cleanup on errors.
                }
                System.out.println(ex);                // do something useful here....
            }
        } else {
            Log.warn("No codec matching " + codec.toString());
        }
        return ret;
    }

    private PhonoAudioShim getAudio(Codec codec) {
        if (_audio != null) {
            try {
                float ofreq = (_audio.getCodec(_audio.getCodec())).getSampleRate();
                float nfreq = (_audio.getCodec(codec.iaxcn)).getSampleRate();
                System.out.println("getting audio is " + ofreq + " = " + nfreq + " ? " + ((nfreq != ofreq) ? "No" : "Yes"));

                if (nfreq != ofreq) {
                    _audio.unInit();
                }
            } catch (IllegalStateException ok) {
                // thats actually legit - it is an uninitialized audio
                // so we haven't _set_ a rate yet
            }
        }
        return _audio;
    }
}
