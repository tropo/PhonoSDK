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

package com.phono.audio.phone;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import javax.sound.sampled.*;

import com.phono.audio.Log;

public class WavPlay {
    
    private float[] MIDDLE = {50.0F, 50.0F};
    private Hashtable<String, short[]> _cache = new Hashtable<String, short[]>();
    private SourceDataLine _line = null;
    private boolean _closeLine;

    public WavPlay() throws LineUnavailableException {
        this(44100F, 10, 2);
        this.start();
    }

    public WavPlay(float freq, int mul, int chan) throws LineUnavailableException {
        _closeLine = true;
        String osname = System.getProperty("os.name");
        if (osname != null) {
            osname = osname.toLowerCase();
            if (osname.startsWith("mac")) {
                _closeLine = false;
            }
        }

        AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
            freq, 16, chan, 2 * chan, freq, true);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
        _line = (SourceDataLine) (DataLine) AudioSystem.getLine(info);
        _line.open(af, 2 * 320 * 6 * mul * chan);
    }

    public short[] cache(String wavName) {
        URL u = this.getClass().getResource(wavName);
        short[] audio = readWav(u);
        _cache.put(wavName, audio);
        return audio;
    }

    public void play(String wavName, float[] pct) {
        if ((pct == null) || (pct.length != 2)) {
            pct = MIDDLE;
        }
        else {
            // de-emph sounds wrt user
            float[] npct = new float[2];
            double tot = pct[0] + pct[1];
            double rat = 100.0 / tot;
            npct[0] = (float) (pct[0] * rat);
            npct[1] = (float) (pct[1] * rat);
            pct = npct;
        }
        short[] aud = (short[]) _cache.get(wavName);
        if (aud == null) {
            aud = cache(wavName);
        }
        if (aud != null) {
            byte[] data = new byte[aud.length * 4];
            int j = 0;
            for (int i = 0; i < aud.length; i++) {
                short left = (short) (aud[i] * pct[0] / 100.0);
                short right = (short) (aud[i] * pct[1] / 100.0);
                data[j++] = (byte) ((left >>> 8) & 0xff);
                data[j++] = (byte) (0xff & left);

                data[j++] = (byte) ((right >>> 8) & 0xff);
                data[j++] = (byte) (0xff & right);

            }
            this.start();
            this.write(data);
        }
    }

    
    public void start() {
        _line.start();
    }

    public void stop() {
        _line.stop();
    }

    /**
     * audioCleanup
     */
    public void audioCleanup() {
        if (_closeLine == true && _line != null && _line.isOpen()) {
            _line.close();
        }
    }

    public void write(byte[] sbuff) {
        _line.write(sbuff, 0, sbuff.length);
    }
    
    short swap(short i) {
        return (short) (((i >> 8) & 0xff) + ((i << 8) & 0xff00));
    }

    int swap(int i) {
        return ((i & 0xff) << 24) + ((i & 0xff00) << 8) + ((i & 0xff0000) >> 8) + ((i >> 24) & 0xff);
    }

    private short[] readWav(URL u) {
        short[] ret = null;
        byte[] chunkT = new byte[4];
        try {
            DataInputStream din = new DataInputStream(u.openStream());
            din.read(chunkT); // RIFF
            int flen = swap(din.readInt());
            din.read(chunkT); //WAVE
            din.read(chunkT); //fmt
            int clen = swap(din.readInt()); // 16
            short fmt = swap(din.readShort()); //1 (pcm)
            short channels = swap(din.readShort()); //1 mono
            int rate = swap(din.readInt());  //44100*2
            int bps = swap(din.readInt()); //44100*2
            int bs = swap(din.readShort());
            int bits = swap(din.readShort()); //16
            din.read(chunkT); //data
            int rofl = swap(din.readInt());
            int sofl = rofl / 2;
            ret = new short[sofl];
            for (int i = 0; i < sofl; i++) {
                char c = din.readChar();
                ret[i] = (short) (((c & 0xff) << 8) | ((c & 0xff00) >> 8));
            }
        }
        catch (IOException ex) {
            Log.warn(ex.getMessage());
        }

        return ret;
    }

    
    public static void main(String[] args) {
        try {
            float[] l = {75.0F, 25.0F};
            float[] r = {25.0F, 75.0F};
            WavPlay t = new WavPlay();
            t.start();
            t.cache("/enter.wav");
            t.cache("/exit.wav");
            t.cache("/you_have_ptt.wav");
            t.play("/enter.wav", l);
            t.play("/you_have_ptt.wav", null);
            t.play("/enter.wav", r);
            t.play("/exit.wav", l);
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            System.exit(0);

        }
        catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }
}
