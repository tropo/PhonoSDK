/*
 * NAME
 *     $RCSfile: AndroidAudio.java,v $
 * DESCRIPTION
 *      [given below in javadoc format]
 * DELTA
 *      $Revision: 1.11 $
 * CREATED
 *      $Date: 2011/03/23 09:22:42 $
 * COPYRIGHT
 *      Phonefromhere.com Ltd
 * TO DO
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
import com.phono.audio.phone.StampedAudioImpl;


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
    protected  AndroidAudioSpeaker _speaker;
    protected AndroidAudioMic _mic;
    private ArrayList<StampedAudio> _cleanAudioList;
    public int _sampleRate;

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
        int samplesPerFrame = _sampleRate / frameRateSec;
        // sample is in short, so frame size in bytes is:
        int bytesPerFrame = samplesPerFrame * 2;

        _stampedBuffer = new StampedAudioImpl[frameRateSec];

        // open Audio: mic & speakers/headset
        boolean isOK = true;
        if (_speaker != null) {
            _speaker.destroy();
        }
        _speaker = new AndroidAudioSpeaker(_codec, this);
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
                Log.debug(
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
            _speaker.startPlay();
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
        G722Codec g722Codec = new G722Codec();
        _codecMap.put(new Long(g722Codec.getCodec()), g722Codec);

        ULaw_Codec ulawCodec = new ULaw_Codec();
        _codecMap.put(new Long(ulawCodec.getCodec()), ulawCodec);
        ALaw_Codec alawCodec = new ALaw_Codec();
        _codecMap.put(new Long(alawCodec.getCodec()), alawCodec);
        GSM_Codec gsmCodec = new GSM_Codec();
        _codecMap.put(new Long(gsmCodec.getCodec()), gsmCodec);


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
        if (_mic != null){
            _mic.setGain(f);
        }
    }
}
