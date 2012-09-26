/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.phono.jingle;


import com.phono.srtplight.Log;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 *
 * @author tim
 */
public class PhonoMessaging {
    private PhonoNative _pni;

        void setPhonoNative(PhonoNative p) {
        _pni = (PhonoNative) p;
        final PhonoMessaging pmi = this;
        PacketListener pli = new PacketListener() {

            public void processPacket(final Packet packet) {
                Log.debug("PhonoMessage processPacket: \n" + packet.toXML());

                if (packet instanceof Message) {
                   Message m = (Message) packet;
                   PhonoMessage pm = new PhonoMessage(pmi,m.getFrom(),m.getBody());
                   pmi.onMessage(pm);
                }
            }
        };
        PacketFilter pf = new PacketFilter() {

            public boolean accept(Packet packet) {
                return (packet instanceof Message); // probably need to narrow this down so messaging works.
                // but for now we'll take everything we are given
            }
        };
        _pni.addPacketListener(pli, pf);
    }


    public void onMessage(PhonoMessage message) {
        Log.warn("Message arrived - you should probably have overridden this method -> PhonoMessaging.onMessage()");
    }

    public void send(String to, String body) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
