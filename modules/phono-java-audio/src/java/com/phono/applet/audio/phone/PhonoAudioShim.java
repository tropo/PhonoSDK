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

package com.phono.applet.audio.phone;

import com.phono.audio.AudioException;
import com.phono.audio.codec.CodecFace;
import com.phono.codecs.speex.SpeexCodec;

//import com.phono.dsp.EchoCanceler;
import com.phono.srtplight.Log;

import java.applet.Applet;
import java.applet.AudioClip;
import java.net.URL;
import java.util.Hashtable;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

public class PhonoAudioShim extends EsupPhonoAudio {

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
    private String _mixinName = null;

    public void setAudioInName(String ain){
        _mixinName = ain;
    }

    @Override
    public void init(long codec, int latency) throws AudioException {
        // Called at the start of every new share
        super.init(codec, latency);
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


        super.fillCodecMap();
        
        _codecMap.put(new Long(sc16.getCodec()), sc16);
        _codecMap.put(new Long(sc.getCodec()), sc);
        _defaultCodec = sc;

        printAvailableCodecs();
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
            Exception whereami = new Exception("not restarting rec");
            StringWriter sw = new StringWriter();
            whereami.printStackTrace(new PrintWriter(sw));
            Log.debug(sw.toString());
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

    public static void getMixersJSON(StringBuffer sb) {
    Mixer.Info[] mixI = AudioSystem.getMixerInfo();
    sb.append("mixers").append(" : ").append(" [ \n");
    for (int i = 0; i < mixI.length; i++) {
      Mixer.Info mi = mixI[i];
      Log.debug("Mixer "+mi.getName()+" vendor "+mi.getVendor());

      if(i>0){sb.append(",");}
      sb.append("{\n");
      sb.append("class : ").append('"').append(mi.getClass().getName()).append('"').append("\n,");
      sb.append("name : ").append('"').append(mi.getName().trim()).append('"').append("\n,");
      sb.append("vendor : ").append('"').append(mi.getVendor()).append('"').append("\n,");
      Mixer m = AudioSystem.getMixer(mi);
      getMixerLinesJSON(sb, m);
      sb.append("}\n");
    }
    sb.append(" ] \n");
  }

  /**
   * listLines
   *
   * @param ps PrintStream
   * @param m Mixer
   */
    static void getMixerLinesJSON(StringBuffer sb, Mixer m) {
        Line.Info[] infos = m.getSourceLineInfo();
        sb.append("sources : [ \n");
        boolean first = true;
        if (infos.length > 0){
            Log.debug("sources = "+infos.length);
        }
        for (int i = 0; i < infos.length; i++) {
            if (infos[i] instanceof DataLine.Info) {

                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }
                sb.append("{\n");

                DataLine.Info dataLineInfo = (DataLine.Info) infos[i];
                AudioFormat[] supportedFormats = dataLineInfo.getFormats();
                sb.append("minBuf : ").append("" + dataLineInfo.getMinBufferSize()).append("\n,");
                sb.append("maxBuf : ").append("" + dataLineInfo.getMaxBufferSize()).append("\n");

                //getMixerLineFormatsJSON(sb, supportedFormats);
                sb.append("}\n");

            }
        }
        sb.append(" ], \n");
        first = true;
        infos = m.getTargetLineInfo();
        if (infos.length > 0){
            Log.debug("targets = "+infos.length);
        }
        sb.append("targets : [ \n");
        for (int i = 0; i < infos.length; i++) {
            if (infos[i] instanceof DataLine.Info) {

                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }
                sb.append("{\n");

                DataLine.Info dataLineInfo = (DataLine.Info) infos[i];
                AudioFormat[] supportedFormats = dataLineInfo.getFormats();
                sb.append("minBuf : ").append("" + dataLineInfo.getMinBufferSize()).append("\n,");
                sb.append("maxBuf : ").append("" + dataLineInfo.getMaxBufferSize()).append("\n,");

                //getMixerLineFormatsJSON(sb, supportedFormats);
                sb.append("}\n");

            }
        }
        sb.append(" ] \n");

    }

  /**
   * showFormats
   *
   * @param ps PrintStream
   * @param fmts AudioFormat[] supportedFormats
   */
    public static void getMixerLineFormatsJSON(StringBuffer sb, AudioFormat[] fmts) {

        sb.append("formats : [ \n");

        for (int i = 0; i < fmts.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\n");
            AudioFormat af = fmts[i];
            sb.append("encoding : ").append('"').append(af.getEncoding()).append('"').append(",\n");
            sb.append("samplerate : ").append("" + af.getSampleRate()).append(",\n");
            sb.append("bitsPerSample : ").append("" + af.getSampleSizeInBits()).append("\n");
            sb.append("}\n");
        }
        sb.append(" ] \n");
    }

    @Override
    //try and use the prefered mixer.
    protected void initMic() throws AudioException {
        Mixer pref = null;
        if (_mixinName != null) {
            Mixer.Info[] mixes = AudioSystem.getMixerInfo();
            for (int i = 0; i < mixes.length; i++) {
                Mixer.Info mixi = mixes[i];
                String mixup = mixi.getName().trim();
                Log.debug("Looking at Mixer " + i + " " + mixup);
                if (mixup.equals(_mixinName)) {
                    pref = AudioSystem.getMixer(mixi);
                    Log.debug("Selected Mixer " + i + " " + mixup);
                }
            }
        }
        super.initMic(pref);
    }
}
