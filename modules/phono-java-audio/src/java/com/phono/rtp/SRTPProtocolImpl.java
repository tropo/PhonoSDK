/*
 * Copyright 2011 Voxeo Corp.
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

package com.phono.rtp;

import com.phono.android.audio.Log;
import java.io.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Properties;
import javax.crypto.Mac;

/**
 * see  http://www.faqs.org/rfcs/rfc3711.html
 */
public class SRTPProtocolImpl extends RTPProtocolImpl {

    final static int SRTPWINDOWSIZE = 64;

    ;
    long[] _replay = new long[SRTPWINDOWSIZE];
    long _windowLeadingEdge = 0;

    ;
    private SRTPSecContext _scIn;
    private SRTPSecContext _scOut;
    private boolean _doCrypt = true;
    private boolean _doAuth = true;

    private void init(Properties lcryptoProps, Properties rcryptoProps) {
        _srtp = true;
        _scIn = _scOut = null;

        try {
            if (_doAuth || _doCrypt) {
                _scIn = new SRTPSecContext(true);
                _scIn.parseCryptoProps(rcryptoProps);
                _tailIn = _scIn.getAuthTail();
                _scOut = new SRTPSecContext(false);
                _scOut.parseCryptoProps(lcryptoProps);
                _tailOut = _scOut.getAuthTail();
            }
        } catch (GeneralSecurityException ex) {
            Log.error(" error in constructor " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    // we are _trusting_  the kernel to only send us the 'correct' packets, no others.
    // we check of course, but we depend on it to route the packets, so there is no
    // need to have a separate list of crypto contexts as envisaged in the RFC

    public SRTPProtocolImpl(int id, DatagramSocket ds, InetSocketAddress far, int type, Properties lcryptoProps, Properties rcryptoProps) {
        super(id, ds, far, type);
        init(lcryptoProps, rcryptoProps);
    }

    public SRTPProtocolImpl(int id, String local_media_address, int local_audio_port,
            String remote_media_address, int remote_audio_port,
            int type, Properties lcryptoProps, Properties rcryptoProps) throws SocketException {
        super(id, local_media_address, local_audio_port, remote_media_address, remote_audio_port, type);
        init(lcryptoProps, rcryptoProps);
    }

    /*
    The format of an SRTP packet is illustrated in Figure 1.

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<+
    |V=2|P|X|  CC   |M|     PT      |       sequence number         | |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
    |                           timestamp                           | |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
    |           synchronization source (SSRC) identifier            | |
    +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+ |
    |            contributing source (CSRC) identifiers             | |
    |                               ....                            | |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
    |                   RTP extension (OPTIONAL)                    | |
    +>+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
    | |                          payload  ...                         | |
    | |                               +-------------------------------+ |
    | |                               | RTP padding   | RTP pad count | |
    +>+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<+
    | ~                     SRTP MKI (OPTIONAL)                       ~ |
    | +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
    | :                 authentication tag (RECOMMENDED)              : |
    | +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
    |                                                                   |
    +- Encrypted Portion*                      Authenticated Portion ---+

     *
     */
    /*

    To authenticate and decrypt an SRTP packet, the receiver SHALL do the
    following:

    1. Determine which cryptographic context to use as described in
    Section 3.2.3.

    2. Run the algorithm in Section 3.3.1 to get the index of the SRTP
    packet.  The algorithm uses the rollover counter and highest
    sequence number in the cryptographic context with the sequence
    number in the SRTP packet, as described in Section 3.3.1.

    3. Determine the master key and master salt.  If the MKI indicator in
    the context is set to one, use the MKI in the SRTP packet,
    otherwise use the index from the previous step, according to
    Section 8.1.

    4. Determine the session keys, and session salt (if used by the
    transform) as described in Section 4.3, using master key, master
    salt, key_derivation_rate and session key-lengths in the
    cryptographic context with the index, determined in Steps 2 and 3.

    5. For message authentication and replay protection, first check if
    the packet has been replayed (Section 3.3.2), using the Replay
    List and the index as determined in Step 2.  If the packet is
    judged to be replayed, then the packet MUST be discarded, and the
    event SHOULD be logged.

    Next, perform verification of the authentication tag, using the
    rollover counter from Step 2, the authentication algorithm
    indicated in the cryptographic context, and the session
    authentication key from Step 4.  If the result is "AUTHENTICATION
    FAILURE" (see Section 4.2), the packet MUST be discarded from
    further processing and the event SHOULD be logged.

    6. Decrypt the Encrypted Portion of the packet (see Section 4.1, for
    the defined ciphers), using the decryption algorithm indicated in
    the cryptographic context, the session encryption key and salt (if
    used) found in Step 4 with the index from Step 2.

    7. Update the rollover counter and highest sequence number, s_l, in
    the cryptographic context as in Section 3.3.1, using the packet
    index estimated in Step 2.  If replay protection is provided, also
    update the Replay List as described in Section 3.3.2.

    8. When present, remove the MKI and authentication tag fields from
    the packet.

     */
    void checkForReplay() throws RTPPacketException {
        // index is set by now...
        if (_index < _windowLeadingEdge) {
            // old packet....
            if ((_windowLeadingEdge - _index) > SRTPWINDOWSIZE) {
                throw new RTPPacketException(" out of window, packet too old");
            }
            // in window but .... is it a replay ?
            int tidx = (int) (_index % SRTPWINDOWSIZE);
            if (_replay[tidx] == _index) {
                throw new RTPPacketException(" Seen that packet before - replay attack ? " + _index);
            }
        }
    }

    @Override
    void checkAuth(byte[] packet, int plen) throws RTPPacketException {
        if (Log.getLevel() > Log.DEBUG) {

            Log.verb("auth on packet " + getHex(packet, plen));
            Log.verb("Packet index " + Long.toHexString(_index));

        }
        try {
            _scIn.deriveKeys(0/*_index*/);

            if (_doAuth) {
                Mac hmac = _scIn.getAuthMac();

                int alen = _tailIn;
                int offs = plen - alen;
                ByteBuffer m = ByteBuffer.allocate(offs + 4);
                m.put(packet, 0, offs);
                m.putInt((int) _roc);

                byte[] auth = new byte[alen];
                System.arraycopy(packet, offs, auth, 0, alen);
                int mlen = (plen - RTPHEAD) - alen;
                Log.verb("mess length =" + mlen);
                if (Log.getLevel() > Log.DEBUG) {
                    Log.verb("auth body " + getHex(m.array()));
                }
                m.position(0);
                hmac.update(m);
                byte[] mac = hmac.doFinal();

                if (Log.getLevel() > Log.DEBUG) {
                    Log.verb("auth in   " + getHex(auth));
                }
                if (Log.getLevel() > Log.DEBUG) {
                    Log.verb("auth out  " + getHex(mac, 10));
                }

                for (int i = 0; i < alen; i++) {
                    if (auth[i] != mac[i]) {
                        throw new RTPPacketException("not authorized byte " + i + " does not match ");
                    }
                }
            }
        } catch (GeneralSecurityException ex) {

            throw new RTPPacketException("Problem checking  packet " + ex.getMessage());

        }
    }

    @Override
    void deliverPayload(
            byte[] payload, long stamp, int ssrc , char seqno) {
        try {
            if (_doCrypt) {
                decrypt(payload, ssrc);
            }
            super.deliverPayload(payload, stamp, ssrc,seqno);
        } catch (GeneralSecurityException ex) {
            Log.error("problem with decryption " + ex.getMessage());
        }
    }



    @Override
    void updateCounters(
            char seqno) {
        // note that we have seen it.
        int tidx = (int) (_index % SRTPWINDOWSIZE);
        _replay[tidx] = _index;
        // and update the leading edge if needed.
        if (_index > _windowLeadingEdge) {
            _windowLeadingEdge = _index;
        }
        super.updateCounters(seqno);
    }

    @Override
    /**
     * calculate the outbound auth and put it at the end of the packet
     * starting at length - _tail space is already allocated.
     */
    void appendAuth(byte[] packet ) throws RTPPacketException {
        if (_doAuth) {
            try {
                // strictly we might need to derive the keys here too -
                // since we might be doing auth but no crypt.
                // we don't support that so nach.
                Mac mac = _scOut.getAuthMac();
                int offs = packet.length - _tailOut;
                ByteBuffer m = ByteBuffer.allocate(offs + 4);
                m.put(packet, 0, offs);
                int oroc = (int) (_seqno >>> 16);
                if ((_seqno & 0xffff)== 0){
                    Log.debug("seqno = 0 outgoing roc ="+oroc);
                }
                m.putInt(oroc);
                if (Log.getLevel() > Log.DEBUG) {
                    Log.verb("auth body " + getHex(m.array()));
                }
                m.position(0);

                mac.update(m);
                byte[] auth = mac.doFinal();
                int len = _tailOut;

                for (int i = 0; i < len; i++) {
                    packet[offs + i] = auth[i];
                }
            } catch (GeneralSecurityException ex) {
                throw new RTPPacketException("Problem sending  packet " + ex.getMessage());
            }
        }
        if (Log.getLevel() > Log.DEBUG) {
            Log.verb("Sending packet " + getHex(packet));
        }
    }

    @Override
    protected void sendPacket(byte[] data, long stamp, int ptype, boolean marker) throws SocketException,
            IOException {
        try {
            if (_doCrypt) {
                _scOut.deriveKeys(stamp);
                encrypt(data, (int) _csrcid, _seqno);
            }
            super.sendPacket(data, stamp, ptype, marker);
        } catch (Exception ex) {
            Log.error("problem encrypting packet" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private ByteBuffer getPepper(int ssrc, long idx) {
        //(SSRC * 2^64) XOR (i * 2^16)
        ByteBuffer pepper = ByteBuffer.allocate(16);
        pepper.putInt(4, ssrc);
        long sindex = idx << 16;
        pepper.putLong(8, sindex);

        return pepper;
    }

    private void decrypt(byte[] payload, int ssrc) throws GeneralSecurityException {
        ByteBuffer in = ByteBuffer.wrap(payload);
        // aes likes the buffer a multiple of 32 and longer than the input.
        int pl = (((payload.length / 32) + 2) * 32);
        ByteBuffer out = ByteBuffer.allocate(pl);
        ByteBuffer pepper = getPepper(ssrc, _index);
        _scIn.decipher(in, out, pepper);
        System.arraycopy(out.array(), 0, payload, 0, payload.length);
    }

    private void encrypt(byte[] payload, int ssrc, long idx) throws GeneralSecurityException {
        ByteBuffer in = ByteBuffer.wrap(payload);
        int pl = (((payload.length / 32) + 2) * 32);
        ByteBuffer out = ByteBuffer.allocate(pl);
        ByteBuffer pepper = getPepper(ssrc, idx);
        _scOut.decipher(in, out, pepper);
        System.arraycopy(out.array(), 0, payload, 0, payload.length);
    }

    public static void main(String[] args) {
        System.out.println("testing STRP ");
        int id = 99;
        String local_media_address = "127.0.0.1";
        int local_audio_port = 19000;
        String remote_media_address = "127.0.0.1";
        int remote_audio_port = 19001;
        int type = 1;
        Log.setLevel(Log.VERB);
        String sdp = "required='1' \n"
                + "crypto-suite='AES_CM_128_HMAC_SHA1_80' \n"
                + "key-params='inline:d0RmdmcmVCspeEc3QGZiNWpVLFJhQX1cfHAwJSoj' \n"
                + "session-params='KDR=0' \n"
                + "tag='1' \n";

        InputStream sr = new ByteArrayInputStream(sdp.getBytes());
        Properties cryptoProps = new Properties();
        try {
            cryptoProps.load(sr);
        } catch (IOException ex) {
            Log.error("invalid sdp props.");
        }
        SRTPProtocolImpl s = null;
        try {
            s = new SRTPProtocolImpl(id, local_media_address, local_audio_port,
                    remote_media_address, remote_audio_port,
                    type, cryptoProps, cryptoProps);

            //s.testSeqs();
            //s.testRcvSRTP();
            s.testSendSRTP();
        } catch (IOException ex) {
            Log.error(ex.getMessage());
        }
        try {
            Thread.sleep(60000);
        } catch (InterruptedException ex) {
            ;
        }
        if (s != null) {
            s.terminate();
        }


    }

    private void testRcvSRTP() {

        // need to make a more promiscuous socket on a known port
        _ds.close();
        try {
            _ds = new DatagramSocket(19000);
            _ds.setSoTimeout(1000);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        Log.debug("to test rcv of SRTP packets run");
        Log.debug("./rtpw -a -e -k " + getHex(_scIn._masterKey) + getHex(_scIn._masterSalt.array()) + " -s "
                + "127.0.0.1 " + _ds.getLocalPort());

        Log.debug("test srtp recv starting in 10 secs");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ;
        }

        RTPDataSink sink = new RTPDataSink() {

            public void dataPacketReceived(byte[] data, long stamp,long idx) {
                Log.debug("got " + data.length + " bytes");
                Log.debug("data =" + getHex(data));
                Log.debug("Message is " + new String(data));
            }
        };
        setRTPDataSink(sink);
        startrecv();
    }

    private void testSeqs() {
        try {
            char seq = 0;
            System.out.println("Seq test ");
            long top = Integer.MAX_VALUE;
            for (long j = 0; j < top; j++) {

                long i = getIndex(seq);
                if (i != j) {
                    throw new RTPPacketException("sequence test failed " + i + " != " + j + " seq " + (short) seq);
                }
                _index = i;
                checkForReplay();
                updateCounters(seq);
                seq++;

            }
        } catch (RTPPacketException ex) {
            Log.debug(ex.getMessage());
        }
    }

    static String getHex(byte[] in) {
        return getHex(in, in.length);
    }

    static String getHex(byte[] in, int len) {
        char cmap[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuffer ret = new StringBuffer();
        int top = Math.min(in.length, len);
        for (int i = 0; i < top; i++) {
            ret.append(cmap[0x0f & (in[i] >>> 4)]);
            ret.append(cmap[in[i] & 0x0f]);
        }
        return ret.toString();
    }

    private void testSendSRTP() throws IOException {

        // need to make a more promiscuous socket on a known port
        _ds.close();
        try {
            _ds = new DatagramSocket(19000);
            _ds.setSoTimeout(60000);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        Log.debug("to test rcv of SRTP packets run");
        Log.debug("./rtpw -d  -a -e -k " + getHex(_scIn._masterKey) + getHex(_scIn._masterSalt.array(), 14) + " -r "
                + "127.0.0.1 19002");

        Log.debug("test srtp send starting in 10 secs");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ;
        }
        // kinda don'r expect this.....
        RTPDataSink sink = new RTPDataSink() {

            public void dataPacketReceived(byte[] data, long stamp,long idx) {
                Log.debug("got " + data.length + " bytes");
                Log.debug("data =" + getHex(data));
                Log.debug("Message is " + new String(data));
            }
        };
        setRTPDataSink(sink);

        this._csrcid = 0xdeadbeef;
        startrecv();
        byte[] data = new byte[33];
        long stamp = 0;
        String messages[] = {"A",
            "a",
            "aa",
            "aal",
            "aalii",
            "aam",
            "Aani",
            "aardvark",
            "aardwolf",
            "Aaron"
        };
        for (int i = 0; i < messages.length; i++) {
            stamp += 8000;
            byte mess[] = new byte[messages[i].length() + 2];
            System.arraycopy(messages[i].getBytes(), 0, mess, 0, messages[i].length());
            mess[messages[i].length()] = (byte) 10;
            Log.debug("Sending " + messages[i]);
            sendPacket(mess, stamp, 1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ;
            }
        }

    }
}
