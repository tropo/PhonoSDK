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

package com.phono.api;

import com.phono.audio.Log;
import com.phono.audio.phone.PhonoAudioPropNames;
import com.phono.rtp.Endpoint;
import com.phono.rtp.PhonoAudioShim;
import com.phono.rtp.RTPAudioSession;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

public class Share {

    final public String uri;
    String nearH;
    int nearP;
    String farH;
    int farP;
    int pt;
    PhonoAudioShim audio;
    RTPAudioSession ras;
    private Properties lsrtpProps;
    private Properties rsrtpProps;
    private DatagramSocket ds;
    private InetSocketAddress far;

    public Share(String remuri, PhonoAudioShim a, int t, Properties lsp, Properties rsp) throws IllegalArgumentException {

        uri = remuri;
        parseURI();
        audio = a;
        pt = t;
        lsrtpProps = lsp;
        rsrtpProps = rsp;
        far = new InetSocketAddress(farH, farP);
    }

    public String getLocalURI() {
        return "rtp://" + nearH + ":" + nearP;
    }

    public void start() {
        Log.debug("in Share.start()");
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
                if (ds != null) {
                    ras = new RTPAudioSession(ds, far, pt, audio, lsrtpProps, rsrtpProps);
                    Log.debug("starting :"+uri+" with pt="+pt+" codec "+audio.getCodecName());
                } else {
                    Log.error("Cant start - no socket set for " + getLocalURI());
                }
                return null; // nothing to return
            }
        });
    }

    public void stop() {
        Log.debug("in Share.stop()");
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
                audio.stopRec();
                ras.halt();
                audio.stopPlay();
                return null; // nothing to return
            }
        });
    }

    public float gain(float value) {
        audio.setGain(value / 100.0);
        return value; // for now
    }

    public double [] energy(){
        return audio.getEnergy();
    }
    
    public boolean mute(boolean v) {
        audio.muteMic(v);
        return v;
    }

    public boolean doES(boolean v) {
        audio.setAudioProperty(PhonoAudioPropNames.DOEC, v ? "true" : "false");
        return audio.callHasECon();
    }

    public void digit(final String value, final int duration, final boolean audible) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
                Runnable sendDTMF = new Runnable() {

                    public void run() {
                        ras.digit(value, duration, audible);
                    }
                };
                Thread ds = new Thread(sendDTMF);
                ds.start();
                return null; // nothing to return
            }
        });

    }

    private void parseURI() throws IllegalArgumentException, NumberFormatException {
        if (!uri.startsWith("rtp://")) {
            throw new IllegalArgumentException(" Share URI must start with rtp:");
        }
        String bits[] = uri.split(":");
        if (bits.length != 5) {
            throw new IllegalArgumentException(" Share URI must contain 2 host:port pairs");
        }
        nearH = bits[1].substring(2);
        nearP = Integer.parseInt(bits[2]);
        farH = bits[3];
        farP = Integer.parseInt(bits[4]);
    }

    public void setSocket(DatagramSocket ds) {
        this.ds = ds;
    }

    public InetSocketAddress getNear() {
        InetSocketAddress ret = null;
        ret = new InetSocketAddress(nearH,nearP);
        return ret;
    }
    /*
    uri,
    Play:
    Examples
    start(), stop(), gain(value) -> value, mute(bool) -> bool, digit(value, duration, audible){DTMF over RTP ? thp} digits(values, duration, interval, audible)
     */
}
