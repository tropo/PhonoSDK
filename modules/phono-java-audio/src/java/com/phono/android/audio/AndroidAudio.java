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

package com.phono.android.audio;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;

import android.media.AudioFormat;
import android.media.AudioTrack;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.gsm.GSM_Codec;
import com.phono.audio.codec.ulaw.ULaw_Codec;
import com.phono.audio.*;
import com.phono.audio.codec.alaw.ALaw_Codec;
import com.phono.audio.codec.g722.G722Codec;
import com.phono.audio.codec.g722.NativeG722Codec;
import com.phono.audio.phone.StampedAudioImpl;
import com.phono.srtplight.Log;

public class AndroidAudio implements AudioFace {

    @SuppressWarnings("unused")
    public final static int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    //public final static int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_8BIT;
    private AudioReceiver _audioReceiver;
    private Properties _audioProperties;
    // Codec related
    protected LinkedHashMap<Long, CodecFace> _codecMap;
    protected CodecFace _defaultCodec;
    private CodecFace _codec;
    // a circular buffer for StampedAudio, read from the mic
    private StampedAudio[] _stampedBuffer;
    private int _stampedBufferStart;
    private int _stampedBufferEnd;
    protected AndroidAudioSpeaker _speaker;
    protected AndroidAudioMic _mic;
    private ArrayList<StampedAudio> _cleanAudioList;
    public int _sampleRate;
    private int _dtmfDigit = -1;

    public AndroidAudio() {
        _codecMap = new LinkedHashMap<Long, CodecFace>();
        fillCodecMap();
        _audioProperties = new Properties();
        _cleanAudioList = new ArrayList<StampedAudio>();
    }

    @Override
    public void init(long codec, int latency) throws AudioException {
        _codec = _codecMap.get(new Long(codec));
        if (_codec == null) {
            _codec = getDefaultCodec();
            Log.warn(
                    this.getClass().getSimpleName()
                    + ".init(): Using default codec:"
                    + _codec.getName());
        }

        if (_codec != null) {
            Log.debug(
                    this.getClass().getSimpleName() + ".init(): "
                    + _codec.toString());

            // apply audio properties again, in case it is codec related!
            Enumeration<Object> e = _audioProperties.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                Object value = _audioProperties.getProperty(key);
                setAudioProperty(key, value);
            }
        } else {
            String text = this.getClass().getSimpleName() + ".init(): codec="
                    + _codec.getName() + " (" + codec
                    + ") is not supported.";
            Log.error(text);
            throw new AudioException(text);
        }

        if (latency == 0) {
            latency = 120;
        }

        _sampleRate = (int) _codec.getSampleRate();

        // frame interval in milliseconds is:
        int frameIntMS = _codec.getFrameInterval();
        // frame rate per sec is:
        int frameRateSec = 1000 / frameIntMS;
        // samples per frame, at 8kHz, is:
        final int samplesPerFrame = _sampleRate / frameRateSec;
        // sample is in short, so frame size in bytes is:
        int bytesPerFrame = samplesPerFrame * 2;

        _stampedBuffer = new StampedAudioImpl[frameRateSec];

        // open Audio: mic & speakers/headset
        boolean isOK = true;
        if (_speaker != null) {
            _speaker.destroy();
        }
        _speaker = new AndroidAudioSpeaker(_codec, this) {

            @Override
            short[] effectOut(short[] out) {
                short[] ret = super.effectOut(out);
                int ld = _dtmfDigit;
                if ( ld >= 0) {
                    long k = _speakerFrames * samplesPerFrame;
                    if ((_speakerFrames % 5) == 0) Log.debug("bleep "+_dtmfDigit);
                    for (int j = 0; j < ret.length; j++) {
                        ret[j] = (short) (getDigitSample(ld, j+k, _sampleRate) + ret[j] / 2);
                    }
                }
                return ret;
            }
        };
        isOK = _speaker.initSpeaker(_sampleRate, bytesPerFrame);

        if (isOK == true) {
            if (_mic != null) {
                _mic.destroy();
            }
            _mic = new AndroidAudioMic(_codec, this);
            isOK = _mic.initMic(_sampleRate, bytesPerFrame);
        }
        if (isOK == false) {
            String text = this.getClass().getSimpleName() + ".init(): "
                    + "failed to initialise either speaker or microphone";
            Log.error(text);
            throw new AudioException(text);
        }

    }
    long toneMap[][] = {{1336, 941}, {1209, 697}, {1336, 697}, {1477, 696}, {1209, 770}, {1336, 770}, {1477, 770}, {1209, 852}, {1336, 852}, {1447, 852}, {1209, 941}, {1477, 941}};

    short getDigitSample(int digit, long position, long rate) {
        double n1 = (2 * Math.PI) * toneMap[digit][0] / rate;
        double n2 = (2 * Math.PI) * toneMap[digit][1] / rate;
        return (short) (((Math.sin(position * n1) + Math.sin(position * n2)) / 4) * Short.MAX_VALUE);
    }

    @Override
    public long getCodec() {
        long ret;
        if (_codec != null) {
            ret = _codec.getCodec();
        } else {
            throw new IllegalStateException(this.getClass().getSimpleName()
                    + ".getCodec(): codec is null, init first");
        }
        return ret;
    }

    @Override
    public String getCodecName() {
        String ret = "";
        if (_codec != null) {
            ret = _codec.getName();
        }
        return ret;
    }

    @Override
    public int getFrameSize() {
        int ret;
        if (_codec != null) {
            ret = _codec.getFrameSize();
        } else {
            throw new IllegalStateException(this.getClass().getSimpleName()
                    + ".getFrameSize(): codec is null, init first");
        }
        return ret;
    }

    @Override
    public int getFrameInterval() {
        int ret;
        if (_codec != null) {
            ret = _codec.getFrameInterval();
        } else {
            Log.warn(
                    this.getClass().getSimpleName()
                    + ".getFrameInterval(): codec is null, init first");
            ret = 20;
        }
        return ret;
    }

    @Override
    public long[] getCodecs() {
        int len = _codecMap.size();
        long codecs[] = new long[len];

        Set<Long> keySet = _codecMap.keySet();
        Iterator<Long> iter = keySet.iterator();
        int i = 0;
        while (iter.hasNext()) {
            Long format = iter.next();
            long codec = format.longValue();
            codecs[i] = codec;
            i++;
        }
        return codecs;
    }

    @Override
    public int getVADpc() {
        return -1;
    }

    @Override
    public boolean isCodecAvailable(long codec) {
        Long format = new Long(codec);
        return _codecMap.containsKey(format);
    }

    @Override
    public int getOutboundTimestamp() {
        int ret = 0;
        if (_mic != null) {
            ret = _mic.getOutboundTimestamp();
        }
        return ret;
    }

    @Override
    public void addAudioReceiver(AudioReceiver r) throws AudioException {
        _audioReceiver = r;
    }

    /**
     * Sends audio data to the speakers.
     * 
     * @param stampedAudio
     *            The stamped audio to speaker
     */
    @Override
    public void writeStampedAudio(StampedAudio stampedAudio)
            throws AudioException {
        if (_speaker != null) {
            if (_speaker.getSpeakerFrames() == 0) {
                // let the mic buffer prefill before we start the speaker
                // startRec();
            }
            _speaker.writeStampedAudio(stampedAudio);

        } else {
            throw new AudioException(
                    this.getClass().getSimpleName()
                    + ".writeStampedAudio(): Audio not initialised, call init() first!");
        }
    }
    int _aibc = 0;

    @Override
    public StampedAudio getCleanStampedAudio() {
        StampedAudio ret = null;
        synchronized (_cleanAudioList) {
            if (_cleanAudioList.size() > 0) {
                ret = _cleanAudioList.remove(0);
            } else {
                ret = new StampedAudioImpl();
                Log.verb(
                        this.getClass().getSimpleName()
                        + ".getCleanStampedAudio(): created a fresh stampedAudio instance " + _aibc);
                _aibc++;

            }
        }
        return ret;
    }

    public void releaseStampedAudio(StampedAudio stampedAudio) {
        synchronized (_cleanAudioList) {
            _cleanAudioList.add(stampedAudio);
        }
    }

    /**
     * Saves the stamped audio, read from the mic, into a buffer
     */
    public void saveReadStampedAudio(StampedAudio stampedAudio) {
        _stampedBufferEnd++;
        int actualPos = _stampedBufferEnd % _stampedBuffer.length;
        _stampedBuffer[actualPos] = stampedAudio;

        Log.verb(
                this.getClass().getSimpleName()
                + ".saveReadStampedAudio(): pos=" + _stampedBufferEnd
                + " (" + actualPos + "), " + stampedAudio.toString());

        int distance = _stampedBufferEnd - _stampedBufferStart;
        if (distance > _stampedBuffer.length) {
            Log.debug(
                    this.getClass().getSimpleName()
                    + ".saveReadStampedAudio(): overflow, start="
                    + _stampedBufferStart + ", end="
                    + _stampedBufferEnd);
        }

        if (_audioReceiver != null) {
            _audioReceiver.newAudioDataReady(this, _codec.getFrameSize());
        } else {
            Log.error(
                    this.getClass().getSimpleName()
                    + ".saveReadStampedAudio(): "
                    + " _audioReceiver is null");
        }
    }

    /**
     * Reads data from the audio queue. May block for upto 2x frame interval,
     * otherwise returns null. May throw an exception if Audio Channel is not
     * set up correctly.
     * 
     * @return a StampedAudio containing a framesize worth of data.
     * @see #getFrameSize()
     */
    @Override
    public StampedAudio readStampedAudio() throws AudioException {
        StampedAudio stampedAudio = null;
        if (_mic != null) {
            if (_stampedBufferStart < _stampedBufferEnd) {
                int actualPos = _stampedBufferStart % _stampedBuffer.length;
                stampedAudio = _stampedBuffer[actualPos];

                if (stampedAudio != null) {
                    Log.verb(
                            this.getClass().getSimpleName()
                            + ".readStampedAudio(): pos="
                            + _stampedBufferStart + " (" + actualPos
                            + "), " + stampedAudio.toString());
                }

                _stampedBufferStart++;
            }
        } else {
            throw new AudioException(
                    this.getClass().getSimpleName()
                    + ".readStampedAudio(): Audio not initialised, call init() first!");
        }
        return stampedAudio;
    }

    @Override
    public boolean isAudioUp() {
        return (_mic != null && _speaker != null);
    }

    @Override
    public void startPlay() {
        if (_speaker != null) {
            //_speaker.startPlay(); // let the arrival of audio decide
        } else {
            Log.error(
                    this.getClass().getSimpleName()
                    + ".startPlay(): initialise speaker first");
        }
    }

    @Override
    public void stopPlay() {
        if (_speaker != null) {
            _speaker.stopPlay();
        }
    }

    @Override
    public void startRec() {
        if (_mic != null) {
            _mic.startRec();
            /*
             * Log.error( this.getClass().getSimpleName() +
             * ".startRec(): not starting mic to see what happens....");
             */
        } else {
            Log.error(
                    this.getClass().getSimpleName()
                    + ".startRec(): initialise microphone first.");
        }
    }

    @Override
    public void stopRec() {
        if (_mic != null) {
            _mic.stopRec();
        }
    }

    @Override
    public void destroy() throws AudioException {
        Log.debug(
                this.getClass().getSimpleName() + ".destroy(): ");
        if (_speaker != null) {
            _speaker.destroy();
            _speaker = null;
        }
        if (_mic != null) {
            _mic.destroy();
            _mic = null;
        }
    }

    @Override
    public Properties getAudioProperties() {
        return new Properties(_audioProperties);
    }

    @Override
    public boolean setAudioProperty(String name, Object value)
            throws IllegalArgumentException {
        Log.debug(
                this.getClass().getSimpleName() + ".setAudioProperty(): name="
                + name + ", value=" + value);
        _audioProperties.put(name, value);
        return false;
    }

    @Override
    public double[] getEnergy() {
        double[] ret = new double[2];
        ret[0] = 0;
        ret[1] = 0;
        if (_mic != null) {
            ret[0] = _mic.getInEnergy();
        }
        if (_speaker != null) {
            ret[1] = _speaker.getOutEnergy();

        }
        return ret;
    }

    @Override
    public void updateRemoteStats(NetStatsFace r) {
        // not implemented yet
    }

    @Override
    public boolean doVAD() {
        return false;
    }

    protected void fillCodecMap() {
        // add all the supported Codecs, in the order of preference
        try {
            NativeG722Codec g722Codec = new NativeG722Codec();
            _codecMap.put(new Long(g722Codec.getCodec()), g722Codec);

            Log.debug("fillCodecMap: " + "got native g722 codec");

        } catch (Throwable thrown) {
            Log.debug("fillCodecMap: " + "didn't get g722 native " + thrown.getMessage());
        }


        ULaw_Codec ulawCodec = new ULaw_Codec();
        _codecMap.put(new Long(ulawCodec.getCodec()), ulawCodec);
        /*
        ALaw_Codec alawCodec = new ALaw_Codec();
        _codecMap.put(new Long(alawCodec.getCodec()), alawCodec);
        GSM_Codec gsmCodec = new GSM_Codec();
        _codecMap.put(new Long(gsmCodec.getCodec()), gsmCodec);
         */

        _defaultCodec = ulawCodec;

    }

    private CodecFace getDefaultCodec() {
        return _defaultCodec;
    }

    public AudioTrack getAudioTrack() {
        AudioTrack ret = null;
        if (_speaker != null) {
            ret = _speaker.getAudioTrack();
        }
        return ret;

    }

    public void setMicGain(float f) {
        if (_mic != null) {
            _mic.setGain(f);
        }
    }

    public double getMicGain() {
        return (double) _mic.getGain();
    }

    public double getSampleRate() {
        return this._sampleRate;
    }

    public void playDigit(char c) {
        String valid = "0123456789#*";
        _dtmfDigit = valid.indexOf(c);
        Log.debug("DtmfDigit is "+_dtmfDigit);
    }

    public void setVolume(double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void muteMic(boolean v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean callHasECon() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CodecFace getCodec(long codec) {
        Long codecL = new Long(codec);
        return (CodecFace) _codecMap.get(codecL);

    }
}
