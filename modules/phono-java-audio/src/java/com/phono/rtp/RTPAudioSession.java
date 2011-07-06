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

import com.phono.audio.AudioException;
import com.phono.audio.Log;
import com.phono.audio.phone.PhonoAudioPropNames;
import com.phono.audio.StampedAudio;
import com.phono.audio.codec.gsm.GSM_Base;
import com.phono.api.CodecList;
import java.applet.AudioClip;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Properties;

public class RTPAudioSession implements RTPDataSink {

    RTPProtocolFace _sps;
    int _id = 1;
    PhonoAudioShim _audio;
    private int _audioheld = 0;

    public RTPAudioSession(DatagramSocket near, InetSocketAddress far, int type, PhonoAudioShim a, Properties lsrtpProps, Properties rsrtpProps) {
        if ((lsrtpProps != null) && (rsrtpProps != null)) {
            _sps = new SRTPProtocolImpl(_id++, near, far, type, lsrtpProps, rsrtpProps);

        } else {
            _sps = new RTPProtocolImpl(_id++, near, far, type);
        }
        _sps.setRTPDataSink(this);
        makePhonoAudioSrc(a);

    }

    public void halt() {
        _sps.terminate();
    }

    private void makePhonoAudioSrc(PhonoAudioShim a) {
        // send side.
        _audio = a;
        try {
            _audio.addAudioReceiver(_sps);
        } catch (AudioException ex) {
            com.phono.audio.Log.error(ex.toString());
        }
        // receive side

        //_audio.startPlay(); // actually don't - let some packets build up first
        _audio.startRec();
    }

    public void digit(String value, int duration, boolean audible) {
        /*
        Event  encoding (decimal)
        _________________________
        0--9                0--9
         *                     10
        #                     11
        A--D              12--15
        Flash                 16
         *

        The payload format is shown in Fig. 1.

        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |     event     |E|R| volume    |          duration             |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

         */
        int sp = 0;
        int end = 0;
        int db = 3;
        char c = value.toUpperCase().charAt(0);
        if (c >= '0' && c <= '9') {
            sp = (c - '0');
        } else {
            if (c == '#') {
                sp = 11;
            }
            if (c == '*') {
                sp = 10;
            }
        }
        if ((c >= 'A') && (c <= 'D')) {
            sp = (12 + (c - 'A'));
        }
        byte data[] = new byte[4];
        int fac = (int) (_audio._sampleRate / 1000.0); // assume duration is in ms.
        int dur = fac * duration;
        /*
        data[0] = (byte) ((0xff) & (sp | 0x80)); // set End flag
        data[1] = 0 ; // 0db - LOUD
        data[3] = (byte) ((0xff) & (dur));
        data[2] = (byte) ((0xff) & (dur >> 8)) ;
         *
         */
        GSM_Base.copyBits(sp, 8, data, 0);
        GSM_Base.copyBits(end, 0, data, 8);
        GSM_Base.copyBits(db, 6, data, 10);
        GSM_Base.copyBits(dur, 16, data, 16);


        int stamp = fac * _audio.getOutboundTimestamp();

        _sps.sendDTMFData(data, stamp, true);
        AudioClip a = null;
        if (audible) {
            a = _audio.playDigit(c);
        }
        // try to ensure that the time between messages is slightly less than the 
        // selected 'duration'
        long count = (duration / 20) - 1;
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(10);
                _sps.sendDTMFData(data, stamp, false);// send an update
                Thread.sleep(10);

            } catch (InterruptedException ex) {
                Log.verb(ex.getMessage());
            }
        }
        //stupid ugly mess - fixed stamp on multiple packets
        //stamp = fac * _audio.getOutboundTimestamp();
        end = 1;
        GSM_Base.copyBits(end, 1, data, 8);
        _sps.sendDTMFData(data, stamp, false);
        _sps.sendDTMFData(data, stamp, false);
        _sps.sendDTMFData(data, stamp, false);

        if (a != null) {
            a.stop();
        }


    }

    public void dataPacketReceived(byte[] data, long stamp) {
        // Log.debug("stamp: " + stamp);
        StampedAudio sa = _audio.getCleanStampedAudio();
        stamp = stamp / CodecList.getFac(_audio.getCodec());

        sa.setStampAndBytes(data, 0, data.length, (int) stamp);
        try {
            /*  let the audio layer decide this.
             * in a nat situation we want to wait a while.
             if (!_audio.isAudioUp()) {
                _audioheld++;
                if (_audioheld > 3) {
                    _audio.startPlay();
                }
            } */
            _audio.writeStampedAudio(sa);
        } catch (AudioException ex) {
            Log.error(ex.toString());
        }
    }
}
