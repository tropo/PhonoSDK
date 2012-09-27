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

import com.phono.api.Share;
import com.phono.applet.audio.phone.Play;
import com.phono.rtp.Endpoint;
import java.net.SocketException;
import java.util.List;
import org.minijingle.jingle.Jingle;
import org.minijingle.jingle.content.Content;
import org.minijingle.jingle.description.Description;
import org.minijingle.jingle.description.Payload;
import org.minijingle.jingle.reason.Reason;
import org.minijingle.jingle.transport.Candidate;
import org.minijingle.jingle.transport.RawUdpTransport;
import org.minijingle.xmpp.smack.JingleIQ;

/**
 * Abstract Class representing a Phono call
 * You must implement the abstract methods for your UI logic etc.
 * @author tim
 */
abstract public class PhonoCall {

    private PhonoPhone _phone;
    private Description _localDescription;
    private RawUdpTransport _transport;
    private Endpoint _end;
    private String _luri;
    private Candidate _candidate;
    private Play _play;
    private Share _share;
    private String _sid;
    private String _rjid;
    private Content _remoteContent;

    public PhonoCall(PhonoPhone pp) {
        _phone = pp;
        try {
            _end = Endpoint.allocate();
            _luri = _end.getLocalURI();
            String bod = _luri.substring("rtp://".length());
            String bits[] = bod.split(":");
            _candidate = new Candidate(bits[0], bits[1], "1");
            _transport = new RawUdpTransport(_candidate);
            _localDescription = new Description("audio", null);// explicitly _don't_ support SRTP in this release.
            PhonoNative pni = pp.getNative();
            List<Payload> payloads = pni.getPayloads();
            for (final Payload payload : payloads) {
                _localDescription.addPayload(payload);
            }
        } catch (SocketException ex) {
            _phone.onError();
        }

    }
    /* expect to have these overridden */

    /**
     * You must implement this abstract method.
     * It will be called when this outbound call is ringing
     */
    abstract public void onRing();

    /**
     * You must implement this abstract method.
     * It will be called when this outbound call is answered
     */
    abstract public void onAnswer();

    /**
     * You must implement this abstract method.
     * It will be called when this  call is answered
     */
    abstract public void onHangup();

    /**
     * You must implement this abstract method.
     * It will be called when a call related error occurs
     */
    abstract public void onError();

    /**
     * Set a custom header on this call
     * @param name
     * @param value
     */
    public void setHeader(String name, String value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * set 'hold' on or off on this call
     * @param h
     */
    public void setHold(boolean h) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * set to true if you require this call to be encrypted (not implemented)
     * @param s
     */
    public void setSecure(boolean s) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Mute this call
     * @param m
     */
    public void setMute(boolean m) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * check to see if this call is in the ringing state
     * @return
     */
    public boolean isRinging() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * set the playback volume
     * @param v
     */
    public void setVolume(int v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * set the microphone gain
     * @param v
     */
    public void setGain(int v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * send a dtmf digit
     * @param character
     */
    public void digit(Character character) {
        if (_share != null) {
            String ds = "" + character;
            _share.digit(ds, 250, true);
        }
    }

    public void hangup() {
        _phone.sendHangup(this);
        teardown(null);
        onHangup();
    }

    RawUdpTransport getTransport() {
        return _transport;
    }

    Description getLocalDescription() {
        return _localDescription;
    }

    void play(String _ringTone) {
        if (_share != null) {
            _share.stop();
            _share = null;
        }
        if (_play != null) {
            _play.stop();
            _play = null;
        }
        _play = new Play(_ringTone);
        _play.start();
    }

    void incomming(Content c) {
        _remoteContent = c;
    }

    void setup(Content c) {
        // stop any ringing.
        if (_play != null) {
            _play.stop();
            _play = null;
        }

        List<Payload> payloads = c.getDescription().getPayloads();
        Payload pay = payloads.get(0);
        List<Candidate> candidates = c.getTransport().getCandidates();
        Candidate candi = candidates.get(0);
        String uri = _luri + ":" + candi.getIp() + ":" + candi.getPort();
        _end.release(); // ugly... and a race condition .

        _share = _phone.getNative().mkShare(uri, pay);
        _share.start();


    }

    void teardown(Reason r) {
        if (_share != null) {
            _share.stop();
            _share = null;
        }
        if (_play != null) {
            _play.stop();
            _play = null;
        }
    }

    void setSid(String sid) {
        _sid = sid;
    }

    public String getSid() {
        return _sid;
    }

    public void setRJid(String jid) {
        _rjid = jid;
    }

    public String getRJid() {
        return _rjid;
    }

    /**
     * answer this (inbound) call.
     */
    public void answer() {
        if (_play != null) {
            _play.stop();
            _play = null;
        }
        if (this._remoteContent != null) {
            PhonoNative pni = _phone.getNative();
            // craft up an accept.....
            RawUdpTransport theTransport = getTransport();
            Description ld = new Description("audio", null);// explicitly _don't_ support SRTP in this release.
            List<Payload> nearpays = pni.getPayloads();
            List<Payload> farpays = this._remoteContent.getDescription().getPayloads();
            Payload apay = null;

            for (final Payload near : nearpays) {
                if ((apay = hasPayload(farpays, near)) != null) {
                    ld.addPayload(apay);
                    break;// pick the first one we like
                }
            }


            String localJid = pni.getSessionID();

            Jingle accept = new Jingle(_sid, localJid, _rjid, Jingle.SESSION_ACCEPT);
            Content combContent = new Content(localJid, localJid.split("/")[0], "both", ld, theTransport);

            accept.setContent(combContent);

            final JingleIQ initiateIQ = new JingleIQ(accept);
            //initiateIQ.setFrom(localJid);
            initiateIQ.setTo(_rjid);
            pni.sendPacket(initiateIQ);


            List<Candidate> candidates = _remoteContent.getTransport().getCandidates();
            Candidate candi = candidates.get(0);
            String uri = _luri + ":" + candi.getIp() + ":" + candi.getPort();
            _end.release(); // ugly... and a race condition .

            _share = pni.mkShare(uri, apay);
            _share.start();


        }
    }

    private Payload hasPayload(List<Payload> farpays, Payload near) {
        Payload ret = null;
        for (Payload p : farpays) {
            if ((p.getName().equals(near.getName())) && (p.getClockrate() == near.getClockrate())) {
                ret = p;
                break;
            }
        }
        return ret;
    }
}
