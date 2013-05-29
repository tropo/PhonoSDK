/*
 * Copyright 2012 Voxeo Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.phono.jingle;

import com.phono.api.Codec;
import com.phono.api.CodecList;
import com.phono.api.DeviceInfoFace;
import com.phono.api.PlayFace;
import com.phono.api.Share;
import com.phono.audio.AudioException;
import com.phono.audio.AudioFace;
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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.XMPPException;
import org.minijingle.jingle.description.Payload;
import org.minijingle.xmpp.smack.JingleIQ;
import org.minijingle.xmpp.smack.parser.XStreamIQ;

/**
 * Abstract class implementing the Main Phono Logic. You have to implement the
 * abstract methods of this class, create single instance of that and then
 * provide it with instances of PhonoMessaging and PhonoPhone.
 * 
 * @author tim
 */
abstract public class PhonoNative {

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
	PhonoPhone _phone;
	PhonoMessaging _messaging;
	private boolean _connected;
	private XMPPConnection _xmppConnection;
	private final AudioFace _audio;
	private final CodecList _codecList;
	final private Hashtable _endpoints = new Hashtable();
	private String _domain;
	private ScheduledThreadPoolExecutor _stpe;

	/**
	 * Constructor with an concrete PhonoPhone and PhonoMessaging instances
	 * 
	 * @param p
	 * @param m
	 */
	public PhonoNative(PhonoPhone p, PhonoMessaging m) {
		this();
		setPhone(p);
		setMessaging(m);
	}

	/**
	 * Constructor with an concrete PhonoPhone and PhonoMessaging instances
	 * 
	 * @param domain
	 *            - alternate domain to connect to - instead of gw.v1.phono.com
	 * @param p
	 * @param m
	 */
	public PhonoNative(String domain, PhonoPhone p, PhonoMessaging m) {
		this(domain);
		setPhone(p);
		setMessaging(m);
	}

	/**
	 * 
	 * bare constructor - you must set concrete PhonoPhone and PhonoMessaging
	 * instances once it is constructed.
	 */
	public PhonoNative() {
		this("gw.v1.phono.com");
	}

	/**
	 * bare constructor - you must set concrete PhonoPhone and PhonoMessaging
	 * instances once it is constructed.
	 * 
	 * @param domain
	 *            alternate domain to connect to - instead of app.phono.com
	 */
	public PhonoNative(String domain) {
		_stpe = new ScheduledThreadPoolExecutor(1);
		Log.setLevel(Log.DEBUG);
		_audio = newAudio();
		_codecList = new CodecList(_audio);
		_domain = domain;
		Runnable xmppRun = new Runnable() {
			public void run() {

				if (_xmppConnection == null) {
					JingleProvider jp = new JingleProvider();
					ProviderManager.getInstance().addIQProvider("jingle",
							Jingle.XMLNS, jp);
					ConnectionConfiguration cc = new ConnectionConfiguration(
							_domain);
					if (Log.getLevel() >= Log.DEBUG) {
						cc.setDebuggerEnabled(true);
					}
					_xmppConnection = new XMPPConnection(cc);
					jp.enableJingle(_xmppConnection);
				}
			}
		};
		_stpe.submit(xmppRun);
	}

	/**
	 * implement this method to provide a platform specific implementation of
	 * AudioFace
	 * 
	 * @return
	 */
	abstract public AudioFace newAudio();

	public void setApiKey(String k) {
		_apiKey = k;
	}

	public String getApiKey() {
		return _apiKey;
	}

	public String getSessionID() {
		return _sessionID;
	}

	public void setPhone(PhonoPhone p) {
		_phone = p;
		((PhonoPhone) p).setPhonoNative(this);
	}

	public PhonoPhone getPhone() {
		return _phone;
	}

	public void setMessaging(PhonoMessaging m) {
		_messaging = m;
		((PhonoMessaging) m).setPhonoNative(this);
	}

	public PhonoMessaging getMessaging() {
		return _messaging;
	}

	public void connect() {
		final PhonoNative nat = this;
		Runnable conRun = new Runnable() {

			public void run() {
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

					Log.debug("Connection ID "
							+ _xmppConnection.getConnectionID());
					Log.debug("Temp User ID " + _xmppConnection.getUser());
					nat._sessionID = _xmppConnection.getUser().split("/")[0];
                                        nat.sendApiKeyAndCaps();
					nat.onReady();
				} catch (XMPPException ex) {
					Log.error(ex.getMessage());
					nat.onError();
				}
			}
		};
		_stpe.submit(conRun);
	}

	public void disconnect() {
		if (_xmppConnection.isConnected()) {
			_xmppConnection.disconnect();
			this.onUnready();
		}
	}

	public void submit(Runnable r){
		_stpe.submit(r);
	}
	public boolean isConnected() {
		return _connected;
	}

	/**
	 * Override this method to get onReady notifications
	 */
	abstract public void onReady();

	/**
	 * Override this method to get onUnready notifications
	 */
	abstract public void onUnready();

	/**
	 * Override this method to get onError notifications
	 */
	abstract public void onError();

	void sendPacket(final IQ pack) {
		Runnable psend = new Runnable() {
			public void run() {
				_xmppConnection.sendPacket(pack);
			}
		};
		_stpe.submit(psend);
	}

	void addPacketListener(PacketListener pli, PacketFilter pf) {
		_xmppConnection.addPacketListener(pli, pf);
	}

	List<Payload> getPayloads() {
		Codec[] codecs = getCodecs();
		List<Payload> payloads = new ArrayList<Payload>();

		for (Codec codec : codecs) {
			Payload payload = new Payload(String.valueOf(codec.pt), codec.name,
					codec.rate);
			payloads.add(payload);
		}
		return payloads;
	}

	Share mkShare(String uri, Payload pay) {
		Codec codec = null;
		Codec[] codecs = getCodecs();
		int payid = Integer.parseInt(pay.getId());
		for (Codec c : codecs) {
			if ((c.name.equals(pay.getName()))
					&& (c.rate == pay.getClockrate())) {
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
		try {
			_audio.destroy();
		} catch (AudioException ex) {
			Log.error("error in audio destroy" + ex.getMessage());
		}
	}

	void allocateEndpoint(String uri) {
		Endpoint endpoint = new Endpoint(uri);
		_endpoints.put(endpoint.getLocalURI(), endpoint);
	}

	void freeEndpoint(String uri) {
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

	Share share(String uri, Codec codec) {
		Properties spl = null;
		Properties spr = null;

		return share(uri, codec, spl, spr);
	}

	Share share(String uri, Codec codec, Properties spl, Properties spr) {
		// need to fix this for inbound at some point...

		Share ret = null;
		Share share = null;
		boolean autoStart = false;

		if (codec != null) {
			Log.debug("in share() codec = " + codec.name + " rate ="
					+ codec.rate + " pt = " + codec.pt);
			Log.debug("in share() uri = " + uri);

			try {

				AudioFace af = getAudio(codec);
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
				Log.error(ex.getMessage()); // do something useful here....
			}
		} else {
			Log.warn("No codec matching null.");
		}
		return ret;
	}

	private AudioFace getAudio(Codec codec) {
		if (_audio != null) {
			try {
				float ofreq = (_audio.getCodec(_audio.getCodec()))
						.getSampleRate();
				float nfreq = (_audio.getCodec(codec.iaxcn)).getSampleRate();
				Log.debug("getting audio is " + ofreq + " = " + nfreq + " ? "
						+ ((nfreq != ofreq) ? "No" : "Yes"));

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

	/**
	 * return a platform appropriate implementation of a PlayFace with the
	 * specified ringtone loaded.
	 * 
	 * @param tone
	 * @return
	 */
	public abstract PlayFace newPlayer(String tone);

        public abstract DeviceInfoFace newDeviceInfo();

        
	public Payload findAudioPayload(List<Payload> payloads) {
		Payload ret = null;
		Codec[] codecs = getCodecs();
		for (Payload pay : payloads) {
			if (Integer.parseInt(pay.getId()) != CodecList.DTMFPAYLOADTTYPE) {
				for (Codec c : codecs) {
					if ((c.name.equals(pay.getName()))
							&& (c.rate == pay.getClockrate())) {
						ret = pay;
						Log.debug(c.name+" matches "+pay.getName()+" icode="+c.iaxcn);
						break;
					} else {
						Log.debug(c.name+" matches "+pay.getName()+" icode="+c.iaxcn);
					}
				}
			} else {
				Log.debug(pay.getName()+" not audio code");
			}
		}
		return ret;
	}

    private void sendApiKeyAndCaps() {
        final PhonoNative nat = this;
        final DeviceInfoFace dif = this.newDeviceInfo();
        IQ apiAndCaps = new IQ() {

            @Override
            public String getChildElementXML() {
                // look - I am _really_, _really_ sorry to do this, but some 8 classes later
                // I could _not_ get the xstream stuff to do this. - so....
            return  "<apikey xmlns=\"http://phono.com/apikey\">"+nat._apiKey+"</apikey>\n" +
                    " <caps xmlns=\"http://phono.com/caps\">\n" +
                    " <audio><"+dif.getPhonoPlatform()+" bridged=\"true\" protocol=\"rtp\" hasIce=\"false\"/></audio>\n" +
                    " <device name=\""+dif.getName()+"\" systemName=\""+dif.getSystemName()+"\" systemVersion=\""+dif.getSystemVersion()+"\"/>\n" +
                    " </caps>";
            }
        };
        apiAndCaps.setType(IQ.Type.SET);
        apiAndCaps.setTo(_domain);
        apiAndCaps.setFrom(null);
        this.sendPacket(apiAndCaps);
    }

}
