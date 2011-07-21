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

import com.phono.audio.AudioFace;
import com.phono.android.audio.Log;
import com.phono.audio.StampedAudio;
import com.phono.audio.codec.gsm.GSM_Base;
import com.phono.api.CodecList;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RTPProtocolImpl implements RTPProtocolFace {

    final static int RTPHEAD = 12;
    final static private int RTPVER = 2;
    final static Random _rand = new SecureRandom();
    private RTPDataSink _rtpds;
    /*  inbound state vars */
    private long _sync = -1;
    protected long _index;
    private boolean _first;
    protected long _roc = 0; // only used for inbound we _know_ the answer for outbound.
    protected char _s_l;// only used for inbound we _know_ the answer for outbound.

    /* networky stuff bidriectional*/
    DatagramSocket _ds;
    SocketAddress _far;
    Thread _listen;
    String _session;
    protected boolean _srtp = false;
    int _id;
    /* we don't support assymetric codec types (we could I suppose) so this is bi */
    int _ptype;

    /* outbound state */
    long _seqno;
    protected long _csrcid;
    protected int _tailIn;
    protected int _tailOut;

    public RTPProtocolImpl(int id, DatagramSocket ds, InetSocketAddress far, int type) {
        _ds = ds;
        _far = far;
        _id = id;
        _ptype = type;
        _session = "RTPSession" + id;
        _seqno = 0;
        _csrcid = _rand.nextInt();
        try {
            _ds.connect(_far);

            _ds.setSoTimeout(100);
        } catch (SocketException ex) {
            Log.warn("Problem with datagram socket:" + ex.getMessage());
        }

        // I like to hide the run method, otherwise it is public
        Runnable ir = new Runnable() {

            public void run() {
                irun();
            }
        };
        _listen = new Thread(ir);
        _listen.setName(_session);
        _first = true;
    }

    public RTPProtocolImpl(int id, String local_media_address, int local_audio_port, String remote_media_address, int remote_audio_port, int type) throws SocketException {
        this(id, new DatagramSocket(local_audio_port), new InetSocketAddress(remote_media_address, remote_audio_port), type);
    }

    protected void irun() {
        byte[] data = new byte[200];
        DatagramPacket dp = new DatagramPacket(data, data.length);
        while (_listen != null) {
            try {
                _ds.receive(dp);
                parsePacket(dp);
            } catch (IOException x) {
                Log.error(x.toString());
            }
        }
        // some tidyup here....
    }

    public void setRTPDataSink(RTPDataSink ds) {
        _rtpds = ds;
    }

    public void terminate() {
        _listen = null;
    }

    public boolean sendDTMFData(byte[] data, long stamp, boolean mark) {
        boolean ret = false;
        try {
            sendPacket(data, stamp, CodecList.DTMFPAYLOADTTYPE, mark);
            ret = true;
        } catch (Exception ex) {
            Log.error(ex.toString());
        }
        return ret;
    }
    /*
    void audioSend() {
    StampedAudio sa;
    try {
    while (null != (sa = _audio.readStampedAudio())) {
    int fac = CodecList.getFac(_audio.getCodec());
    sendPacket(sa.getData(), sa.getStamp() * fac, _ptype);
    }
    } catch (Exception ex) {
    Log.error(ex.toString());
    }

    }
     */

    public void newAudioDataReady(AudioFace af, int i) {

        if (_first) {
            startrecv();
            _first = false;
        }
        try {
            StampedAudio sa = af.readStampedAudio();
            if (sa != null) {
                int fac = CodecList.getFac(af.getCodec());
                sendPacket(sa.getData(), sa.getStamp() * fac, _ptype);
                //Log.debug("send "+ sa.getStamp() * fac);
            }
        } catch (Exception ex) {
            Log.error(ex.toString());
        }
        if (_listen == null) {
            af.stopRec();
            af.stopPlay();
        }

    }

    static long get4ByteInt(byte[] b, int offs) {
        return ((b[offs++] << 24) | (b[offs++] << 16) | (b[offs++] << 8) | (0xff & b[offs++]));
    }

    protected void sendPacket(byte[] data, long stamp, int ptype) throws SocketException, IOException {
        sendPacket(data, stamp, ptype, false);
    }

    protected void sendPacket(byte[] data, long stamp, int ptype, boolean marker) throws SocketException, IOException {

        byte payload[] = new byte[RTPHEAD + data.length + _tailOut]; // assume no pad and no cssrcs
        GSM_Base.copyBits(RTPVER, 2, payload, 0); // version
        // skip pad
        // skip X
        // skip cc
        // skip M
        // all the above are zero.
        if (marker) {
            GSM_Base.copyBits(1, 1, payload, 8);
        }
        GSM_Base.copyBits(ptype, 7, payload, 9);
        payload[2] = (byte) (_seqno >> 8);
        payload[3] = (byte) _seqno;
        payload[4] = (byte) (stamp >> 24);
        payload[5] = (byte) (stamp >> 16);
        payload[6] = (byte) (stamp >> 8);
        payload[7] = (byte) stamp;
        payload[8] = (byte) (_csrcid >> 24);
        payload[9] = (byte) (_csrcid >> 16);
        payload[10] = (byte) (_csrcid >> 8);
        payload[11] = (byte) _csrcid;
        for (int i = 0; i < data.length; i++) {
            payload[i + RTPHEAD] = data[i];
        }
        appendAuth(payload);
        DatagramPacket p = new DatagramPacket(payload, payload.length, _far);
        _ds.send(p);
        _seqno++;
        Log.verb("sending RTP " + _ptype + " packet length " + payload.length + " to " + _far.toString());

    }

    protected void parsePacket(DatagramPacket dp) throws IOException {

        // parse RTP header (if we care .....)
 /*
         *  0                   1                   2                   3
         *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * |                           timestamp                           |
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * |           synchronization source (SSRC) identifier            |
         * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         * |            contributing source (CSRC) identifiers             |
         * |                             ....                              |
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *
         */
        byte[] packet = dp.getData();
        byte[] payload;
        int plen = dp.getLength();

        int ver = 0;
        int pad = 0;
        int csrcn = 0;
        int mark = 0;
        int ptype = 0;
        char seqno = 0;
        long stamp = 0;
        int sync = 0;

        if (plen < 12) {
            throw new RTPPacketException("Packet too short. RTP must be >12 bytes");
        }
        ver = GSM_Base.copyBits(packet, 0, 2);
        pad = GSM_Base.copyBits(packet, 2, 1);
        csrcn = GSM_Base.copyBits(packet, 4, 4);
        ptype = GSM_Base.copyBits(packet, 9, 7);
        ByteBuffer pb = ByteBuffer.wrap(packet);
        /*
        seqno = (char) ((packet[2] << 8) | (packet[3] & 0xff));
        stamp = get4ByteInt(packet, 4);
        sync = get4ByteInt(packet, 8);

         */
        seqno = pb.getChar(2);
        stamp = pb.getInt(4);
        sync = pb.getInt(8);
        if (plen < (RTPHEAD + 4 * csrcn)) {
            throw new RTPPacketException("Packet too short. CSRN =" + csrcn + " but packet only " + plen);
        }

        long[] csrc = new long[csrcn];
        int offs = RTPHEAD;
        for (int i = 0; i < csrcn; i++) {
            /*
            csrc[i] = get4ByteInt(packet, offs);
             *
             */
            csrc[i] = pb.getInt(offs);
            offs += 4;
        }
        int endhead = offs;
        // if padding set then last byte tells you how much to skip
        int paylen = (pad == 0) ? (plen - offs) : ((plen - offs) - (0xff) & packet[plen - 1]);
        // SRTP packets have a tail auth section and potentially an MKI
        paylen -= _tailIn;
        payload = new byte[paylen];
        int o = 0;
        while (offs - endhead < paylen) {
            payload[o++] = packet[offs++];
        }
        // quick plausibility checks
        // should check the ip address etc - but actually we better trust the OS
        // since we have 'connected' this socket meaning _only_ correctly sourced packets seen here.
        if (ver != RTPVER) {
            throw new RTPPacketException("Only RTP version 2 supported");
        }
        if (ptype != _ptype) {
            throw new RTPPacketException("Unexpected payload type " + ptype);
        }
        if (sync != _sync) {
            syncChanged(sync);
        }
        _index = getIndex(seqno);
        try {
            updateCounters(seqno);
            checkAuth(packet, plen);
        } catch (RTPPacketException rpx) {
            Log.debug("Failed packet sync = " + (0 + seqno));
            Log.debug("index is = " + _index);
            if (this instanceof SRTPProtocolImpl) {
                Log.debug("roc is = " + ((SRTPProtocolImpl) this)._roc);
            }


            throw rpx;
        }
        deliverPayload(payload, stamp, sync, seqno);


        Log.verb("got RTP " + ptype + " packet " + payload.length + " from " + _far.toString());

    }

    void checkAuth(byte[] packet, int plen) throws RTPPacketException {
    }

    long getIndex(
            char seqno) {
        long v = _roc; // default assumption

        // detect wrap(s)
        int diff = seqno - _s_l; // normally we expect this to be 1
        if (diff < Short.MIN_VALUE) {
            // large negative offset so
            v = _roc + 1; // if the old value is more than 2^15 smaller
            // then we have wrapped
        }
        if (diff > Short.MAX_VALUE) {
            // big positive offset
            v = _roc - 1; // we  wrapped recently and this is an older packet.
        }
        if (v < 0) {
            v = 0; // trap odd initial cases
        }
        /*
        if (_s_l < 32768) {
        v = ((seqno - _s_l) > 32768) ? (_roc - 1) % (1 << 32) : _roc;
        } else {
        v = ((_s_l - 32768) > seqno) ? (_roc + 1) % (1 << 32) : _roc;
        }*/
        long low = (long) seqno;
        long high = ((long) v << 16);
        long ret = low | high;
        return ret;

    }

    void deliverPayload(byte[] payload, long stamp, int ssrc,char seqno) {
        if (_rtpds != null) {
            _rtpds.dataPacketReceived(payload, stamp,getIndex(seqno));
        }
    }

    void appendAuth(byte[] payload) throws RTPPacketException {
        // nothing to do in rtp
    }

    void updateCounters(
            char seqno) {
        // note that we have seen it.
        int diff = seqno - _s_l; // normally we expect this to be 1
        if (seqno == 0) {
            Log.debug("seqno = 0 _index =" + _index + " _roc =" + _roc + " _s_l= " + (0 + _s_l) + " diff = " + diff + " mins=" + Short.MIN_VALUE);
        }
        if (diff < Short.MIN_VALUE) {
            // large negative offset so
            _roc++; // if the old value is more than 2^15 smaller
            // then we have wrapped
        }
        _s_l = seqno;
    }

    protected void syncChanged(long sync) throws RTPPacketException {
        if (_sync == -1) {
            _sync = sync;
        } else {
            throw new RTPPacketException("Sync changed: was " + _sync + " now " + sync);
        }
    }

    protected void startrecv() {
        _listen.start();
    }

    protected static class RTPPacketException extends IOException {

        RTPPacketException(String mess) {
            super(mess);
        }
    }
}
