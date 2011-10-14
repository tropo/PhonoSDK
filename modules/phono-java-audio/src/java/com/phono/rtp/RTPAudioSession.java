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

import com.phono.api.CodecList;
import com.phono.audio.AudioException;
import com.phono.audio.AudioFace;
import com.phono.audio.AudioReceiver;
import com.phono.audio.StampedAudio;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Properties;
import com.phono.srtplight.*;

public class RTPAudioSession implements RTPDataSink, AudioReceiver {

    RTPProtocolFace _sps;
    int _id = 1;
    AudioFace _audio;
    private boolean _first = true;
    int _ptype;

    public RTPAudioSession(DatagramSocket near, InetSocketAddress far, int type, AudioFace a, Properties lsrtpProps, Properties rsrtpProps) {
        if ((lsrtpProps != null) && (rsrtpProps != null)) {
            _sps = new SRTPProtocolImpl(_id++, near, far, type, lsrtpProps, rsrtpProps);

        } else {
            _sps = new RTPProtocolImpl(_id++, near, far, type);
        }
        _sps.setRTPDataSink(this);
        _ptype = type;
        makePhonoAudioSrc(a);

    }

    public void halt() {
        _sps.terminate();
    }

    private void makePhonoAudioSrc(AudioFace a) {
        // send side.
        _audio = a;
        try {
            _audio.addAudioReceiver(this);
        } catch (AudioException ex) {
            Log.error(ex.toString());
        }
        // receive side

        //_audio.startPlay(); // actually don't - let some packets build up first
        _audio.startRec();
    }


  public void digit(String value, int duration, boolean audible) throws SocketException, IOException {

        int fac = (int) (_audio.getSampleRate() / 1000.0); // assume duration is in ms.
        int dur = fac * duration;
        Log.debug("RAS sending digit "+value+" dur ="+duration+" "+(audible?"Audible":"InAudible"));
        int stamp = fac * _audio.getOutboundTimestamp();
        char c = value.toUpperCase().charAt(0);

        if (audible) {
             _audio.playDigit(c);
        }

        _sps.sendDigit(value, stamp, dur, duration);

        if (audible) {
            c = 0;
             _audio.playDigit(c);
        }


    }

    public void dataPacketReceived(byte[] data, long stamp , long index) {
        // Log.debug("stamp: " + stamp);
        StampedAudio sa = _audio.getCleanStampedAudio();
        /* broken timestamps so make up stamp from index */

        //stamp = stamp / CodecList.getFac(_audio.getCodec());
        stamp = index * _audio.getFrameInterval();
        //Log.debug("rcv fake stamp =" + stamp);
        sa.setStampAndBytes(data, 0, data.length, (int) stamp);
        try {
            _audio.writeStampedAudio(sa);
        } catch (AudioException ex) {
            Log.error(ex.toString());
        }
    }


    public void newAudioDataReady(AudioFace af, int i) {

        if (_first) {
            _sps.startrecv();
            _first = false;
        }
        try {
            StampedAudio sa = af.readStampedAudio();
            if (sa != null) {
                int fac = CodecList.getFac(af.getCodec());
                _sps.sendPacket(sa.getData(), sa.getStamp() * fac, _ptype);
                //Log.debug("send "+ sa.getStamp() * fac);
            }
        } catch (Exception ex) {
            Log.error(ex.toString());
        }
        if (_sps.finished()) {
            af.stopRec();
            af.stopPlay();
        }

    }

    public String getSent() {
        String ret =  "0";
        if ((_sps != null) && (_sps instanceof RTPProtocolImpl) ){
            ((RTPProtocolImpl)_sps).getIndex();
        }
        return ret;
    }

    public String getRcvd() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
