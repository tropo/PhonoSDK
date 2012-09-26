/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.jingle;

import com.phono.srtplight.Log;
import java.util.Hashtable;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.minijingle.jingle.Jingle;
import org.minijingle.jingle.content.Content;
import org.minijingle.jingle.description.Description;
import org.minijingle.jingle.reason.Reason;
import org.minijingle.jingle.reason.Success;
import org.minijingle.jingle.ringing.Ringing;
import org.minijingle.jingle.transport.RawUdpTransport;
import org.minijingle.xmpp.smack.JingleIQ;

/**
 *
 * @author tim
 */
public class PhonoPhone  {

    private PhonoNative _pni;
    private String _ringTone;
    private String _ringbackTone;
    private Hashtable<String, PhonoCall> _currentCalls = new Hashtable<String, PhonoCall>();

    void setPhonoNative(PhonoNative p) {
        _pni = (PhonoNative) p;
        PacketListener pli = new PacketListener() {

            public void processPacket(final Packet packet) {
                Log.debug("PhonePhone processPacket: \n" + packet.toXML());

                if (packet instanceof IQ) {
                    IQ iq = (IQ) packet;

                    if (iq.getType() != IQ.Type.RESULT && iq.getType() != IQ.Type.ERROR) {
                        if (packet instanceof JingleIQ) {
                            processJingle((JingleIQ) packet);
                        }
                    }
                }
            }
        };
        PacketFilter pf = new PacketFilter() {

            public boolean accept(Packet packet) {
                return (packet instanceof IQ);
            }
        };
        _pni.addPacketListener(pli, pf);
    }

    public String getRingTone() {
        return _ringTone;
    }

    public void setRingTone(String rt) {
        _ringTone = rt;
    }

    public String getRingbackTone() {
        return _ringbackTone;
    }

    public void setRingbackTone(String rt) {
        _ringbackTone = rt;
    }

    public void onError() {
        throw new UnsupportedOperationException("This method should generally be overridden - PhonoPhone.onError()");
    }

    public void onIncommingCall(PhonoCall c) {
        throw new UnsupportedOperationException("This method should generally be overridden - PhonoPhone.onIncommingCall()");
    }

    public PhonoCall newCall() {
        throw new UnsupportedOperationException("This method should always be overridden - PhonoPhone.newCall()");
    }

    public PhonoCall dial(String jid, Hashtable headers)  {


        if (headers != null ){
            Log.warn("headers not implemented yet.");
        }

        PhonoCall currentCall = (PhonoCall) newCall();

        Log.debug("Phone.dial(): " + jid);

        RawUdpTransport theTransport = currentCall.getTransport();
        Description localDescription;
        localDescription = currentCall.getLocalDescription();

        final String localJid = _pni.getSessionID();
        final String sid = "phonoNative" + String.valueOf(System.currentTimeMillis());
        currentCall.setSid(sid);
        currentCall.setRJid(jid);
        _currentCalls.put(sid, currentCall); // need to axe these at some point

        final Jingle initiate = new Jingle(sid, localJid, jid, Jingle.SESSION_INITIATE);
        final Content localContent = new Content(localJid, localJid.split("/")[0], "both", localDescription, theTransport);

        initiate.setContent(localContent);

        final JingleIQ initiateIQ = new JingleIQ(initiate);
        //initiateIQ.setFrom(localJid);
        initiateIQ.setTo(jid);
        _pni.sendPacket(initiateIQ);

        return currentCall;

    }

    private void processJingle(final JingleIQ jingleIQ) {
        _pni.sendPacket(JingleIQ.createResult(jingleIQ));

        final Jingle jingle = jingleIQ.getElement();

        jingle.setTo(jingleIQ.getTo());
        jingle.setFrom(jingleIQ.getFrom());

        // Incomming Call
        if (Jingle.SESSION_INITIATE.equals(jingle.getAction())) {
            PhonoCall call = (PhonoCall) this.newCall();
            String sid = jingleIQ.getElement().getSid();
            _currentCalls.put(sid, call);
            call.setSid(sid);
            String rji = jingleIQ.getElement().getInitiator();
            call.setRJid(rji);
            call.incomming(jingleIQ.getElement().getContent());
            call.play(_ringTone);
            Log.debug("Incomming call");

            this.onIncommingCall(call);

        } // Call Accepted
        else if (Jingle.SESSION_ACCEPT.equals(jingle.getAction())) {
            String sid = jingleIQ.getElement().getSid();
            PhonoCall call = _currentCalls.get(sid);
            Content c = jingleIQ.getElement().getContent();
            if ((call != null) && (c != null)) {
                Log.debug("Accepted");
                call.setup(c);
                call.onAnswer();
            }
        } // Call Terminated
        else if (Jingle.SESSION_TERMINATE.equals(jingle.getAction())) {
            String sid = jingleIQ.getElement().getSid();
            PhonoCall call = _currentCalls.get(sid);
            Reason r = jingleIQ.getElement().getReason();
            if ((call != null) && (r != null)) {
                Log.debug("Hungup");
                call.teardown(r);
                call.onHangup();
            }
        } // ringing
        else if (Jingle.SESSION_INFO.equals(jingle.getAction())) {
            String sid = jingleIQ.getElement().getSid();
            PhonoCall call = _currentCalls.get(sid);
            Ringing r = jingleIQ.getElement().getRinging();
            if ((call != null) && (r != null)) {
                Log.debug("Ring Ring.... Ring Ring.");
                call.play(_ringbackTone);
                call.onRing();
            }
        }

    }

    PhonoNative getNative() {
        return _pni;
    }

    void sendHangup(PhonoCall call) {
        String user = _pni.getSessionID();
        String sid = call.getSid();
        String rjid = call.getRJid();
        Jingle terminate = new Jingle(sid, user, rjid, Jingle.SESSION_TERMINATE);
        terminate.setReason(new Reason(new Success()));
        JingleIQ terminateIQ = new JingleIQ(terminate);
        terminateIQ.setTo(rjid);
        Log.debug("sendHangup: " + terminateIQ.toXML());

        _pni.sendPacket(terminateIQ);

    }
}
