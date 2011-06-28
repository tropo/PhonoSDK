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
import com.phono.audio.codec.CodecFace;
import com.phono.codecs.speex.SpeexCodec;

//import com.phono.dsp.EchoCanceler;

import java.applet.Applet;
import java.applet.AudioClip;
import java.net.URL;
import java.util.Hashtable;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class PhonoAudioShim extends com.phono.audio.phone.EsupPhonoAudio {

    Hashtable _clipcache;
    //EchoCanceler _ec;
    SampleBuffer _speakBuf;
    public static final int SPEAKER_BUFFER_LEN = 1024;
    public static final int CUTTER_TIME = 8000*0; // Don't cut for this long
    int _outstamp=0;
    private boolean _doplay;
    private boolean _dorec;
    private boolean _recstarted;
    // Files for debug output
    DataOutputStream micos = null;
    DataOutputStream speakeros = null;
    DataOutputStream speakbufos = null;
    DataOutputStream cancelledos = null;
    boolean debugAudio = false; // Write samples to local disk
    int cutterTime = 0; // Don't cut for this long
    int dropMicSamples = 48;

    @Override
    public void init(long codec, int latency) throws AudioException {
        // Called at the start of every new share
        super.init(codec, latency);
        if (_clipcache == null) {
            _clipcache = new Hashtable();
            preCacheDigits();
        }
        cutterTime = CUTTER_TIME;
        //_ec = new EchoCanceler();
        _speakBuf = new SampleBuffer(SPEAKER_BUFFER_LEN);
        if (debugAudio) {
            Log.debug("Allocate debug file streams.");
            try {
                micos = new DataOutputStream(new FileOutputStream(new File("/tmp/mic-" + System.currentTimeMillis() + ".raw")));
                speakeros = new DataOutputStream(new FileOutputStream(new File("/tmp/speaker-" + System.currentTimeMillis() + ".raw")));
                speakbufos = new DataOutputStream(new FileOutputStream(new File("/tmp/speakbuf-" + System.currentTimeMillis() + ".raw")));
                cancelledos = new DataOutputStream(new FileOutputStream(new File("/tmp/cancelled-" + System.currentTimeMillis() + ".raw")));
            } catch (IOException e) {
                Log.debug("Error allocating file streams.");
            }
        }
    }

    public CodecFace getCodec(long l) {
        return this._codecMap.get(new Long(l));
    }

    @Override
    protected void fillCodecMap() {
        SpeexCodec sc = new SpeexCodec(false);
        SpeexCodec sc16 = new SpeexCodec(true);

        _defaultCodec = sc;

        super.fillCodecMap();
        _codecMap.put(new Long(sc16.getCodec()), sc16);
        _codecMap.put(new Long(sc.getCodec()), sc);
        printAvailableCodecs();
    }

    private static String getDTMFWavName(char c) {
        return "/sounds/" + c + ".wav";
    }

    private AudioClip getDTMFClip(char c) {
        AudioClip ret = null;
        String cname = getDTMFWavName(c);
        ret = (AudioClip) _clipcache.get(cname);
        if (ret == null) {
            URL u = this.getClass().getResource(cname);
            Log.debug("getting clip" + cname);
            ret = Applet.newAudioClip(u);
            if (ret != null) {
                _clipcache.put(cname, ret);
            }
        }
        return ret;
    }

    private void preCacheDigits() {
        Runnable r = new Runnable() {

            public void run() {
                String digits = "0123456789sp";
                for (int i = 0; i < digits.length(); i++) {
                    getDTMFClip(digits.charAt(i));
                }
            }
        };
        Thread pl = new Thread(r);
        pl.start();
    }

    AudioClip playDigit(char c) {
        if (c == '#') {
            c = 'p';
        }
        if (c == '*') {
            c = 's';
        }
        AudioClip a = getDTMFClip(c);
        if (a != null) {
            a.play();
        }
        return a;
    }
    /**
     * note that the superclass also implements the gain and mute functionality,
     * in these 2 methods, so if you override them without invoking the superclass methods,
     * then you will need to provide matching implementations for:
     *     public boolean callHasECon()
     *     public void muteMic(boolean mute)
     *     public void setGain(double gain)
     */


    private void write(DataOutputStream os, short [] in) {
        ByteBuffer writeBuffer = ByteBuffer.allocate(in.length * 2);
        writeBuffer.order(ByteOrder.BIG_ENDIAN);
        
        for (short s : in) {
            writeBuffer.putShort(s);
        }
        try {
            os.write(writeBuffer.array());
        } catch (IOException e) {
            Log.debug("IOException writing samples to debug file.");
        }
    }

    @Override
    /**
     * when a frame's worth of data is read from the microphone thread, it is
     * passed through this method before it is encoded and queued to be sent
     * re-implement this method to implement an echo canceler.
     *
     * The current (super) implementation checks the doEC property (see setAudioProperties)
     * and if it is set, it reduces the amplitude of the samples based on the historic amplitude of
     * the 'relevant' frame sent to the speakers.
     */
    public short [] effectIn(short [] in){
        // Pull from the speaker output buffer and feed both into the EC
        short [] cancelled = new short[in.length];
        short [] speaker;
        short [] incpy = new short[in.length];

        System.arraycopy(in, 0, incpy, 0, in.length);

        if (dropMicSamples > 0) {
            speaker = new short[in.length];
            dropMicSamples = dropMicSamples - 1;
        } else {
            speaker = _speakBuf.read(in.length);
        }

        // Dump some samples for debug
        if (debugAudio) {
            write(micos, incpy);
            write(speakbufos, speaker);
        }

        //_ec.process(speaker, in, cancelled);

        // Dump some samples for debug
        if (debugAudio) {
            write(cancelledos, cancelled);
        }
        
        return super.effectIn(in); //cancelled
    }

    protected void trim(int numberOfSamplesRemovedOrAdded){
        // do nothing here, but echo cans want to know
        //Log.debug("trim: "+ numberOfSamplesRemovedOrAdded);
        _speakBuf.adjust(numberOfSamplesRemovedOrAdded);
    }

    @Override
    /**
     * when a frame's worth of data about to be sent to the speakers, it is
     * passed through this method after it has been decoded and de-jittered
     * re-implement this method to implement an echo canceler.
     *
     * The current (super) implementation checks the doEC property (see setAudioProperties)
     * and if it is set, it calculates the amplitude of the current frame and stores it in
     * a ring buffer for use by effectIn.
     */
    public short [] effectOut(short [] in){
        if (cutterTime > 0) {
            _cutsz = 0;
            cutterTime = cutterTime - in.length;
            Log.debug("No cutting!");
        } else _cutsz = 48;

        //_cutsz = 0;

        // Push the data in to the speaker output buffer
        _speakBuf.write(in);
        if (debugAudio) {
            write(speakeros, in);
        }
        return super.effectOut(in);
    }

    // make the timestamps tidy
    @Override
    public int getOutboundTimestamp() {
        _outstamp+=20;
        return _outstamp ;
    }

    @Override
    public void startRec(){
        // weird faffing about here because in IAX rec is started on
        // first recieved frame
        // with RTP it is the other way around.
        if (!_recstarted){
            Log.debug("Start Rec called ");
            super.startRec();
            _recstarted = true;
        } else {
            Log.debug("not restarting rec");
        }
    }

    @Override
    public void stopRec(){
        _recstarted = false;
        super.stopRec();
    }

    /** 
     * Maintain a circular sample buffer.
     * Writing can overwrite old samples if full.
     * Reading can produce 0s when empty.
     */
    class SampleBuffer {
        private short [] buffer;
        private int tail;
        private int head;

        public SampleBuffer(int size) {
            buffer = new short[size];
            tail = 0;
            head = 0;
        }

        // Remove or duplicate samples based on the shaver
        public void adjust(int count) {
            if (count != 0) Log.debug("adjust: " + count);
            int i = count;

            while (i < 0) {
                // Nuke some samples
                if (tail != head) tail = tail - 1;
                if (tail == -1) tail = buffer.length - 1;
                i = i + 1;   
            }
            short sample;
            if (head == tail) sample = 0;
            else if (head - 1 >= 0) sample = buffer[head - 1];
            else sample = buffer[buffer.length-1];
            while (i > 0) {
                // Pad some samples
                buffer[head] = sample;
                head = (head + 1) % buffer.length;
                if (head == tail) tail = (tail + 1) % buffer.length;
                i = i - 1;
            }
        }

        // Push these samples in at head and advance head
        public void write(short [] samples) {
            // if we are full, overwrite the tail and advance it
            int i = 0;
            while (i < samples.length) {
                buffer[head] = samples[i];
                i = i + 1;
                head = (head + 1) % buffer.length;
                if (head == tail) tail = (tail + 1) % buffer.length;
            }
        }

        // Pull this many samples out of the buffer from tail and advance tail
        // if not enough data then return 0s.
        public short [] read(int size) {
            // If head == tail it's empty
            int contents = (head - tail) % buffer.length;
            short [] output = new short[size];
            int i = 0;
            while (i < size) {
                if (head == tail) {
                    output[i] = 0;
                    Log.debug("Speaker buffer empty, Outputting 0 to EC...");
                } else output[i] = buffer[tail];
                i = i + 1;
                tail = (tail + 1) % buffer.length;                
            }
            return output;
        }
    }
}
