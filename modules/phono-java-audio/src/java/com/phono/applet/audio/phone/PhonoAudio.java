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
import com.phono.audio.AudioFace;
import com.phono.audio.AudioReceiver;
import com.phono.audio.StampedAudio;
import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.CodecUtil;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;
import com.phono.audio.codec.TuneableCodec;
import com.phono.audio.codec.VoiceDetectionCodec;
import com.phono.audio.codec.alaw.ALaw_Codec;
import com.phono.audio.codec.g722.G722Codec;
import com.phono.audio.codec.gsm.GSM_Codec;
import com.phono.audio.codec.slin.SLin_Codec;
import com.phono.audio.codec.ulaw.ULaw_Codec;
import com.phono.audio.NetStatsFace;
import com.phono.audio.phone.PhonoAudioPropNames;
import com.phono.audio.phone.StampedAudioImpl;
import com.phono.srtplight.Log;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * PhonoAudio
 */
public class PhonoAudio implements AudioFace {

    public final static boolean DEFAULT_MUTE_STATE = false;
    // 8kHZ sampling rate:
    public final static float SAMPLING_RATE_8K = 8000.0F;
    public final static double QUIET = 100.0;
    protected LinkedHashMap<Long, CodecFace> _codecMap;
    protected CodecFace _codec;
    private AudioReceiver _audioReceiver;
    private Properties _audioProperties;
    protected String _osname;
    protected String _javaVersion;
    private int _bytesPerFrame;
    private int _codecFrameSize;
    protected int _deep;

    /* Variables relating to inbound audio: incoming frames going to speakers */
    private SourceDataLine _play;
    private DecoderFace _decode;
    private byte _encodedbuffPlay[];
    private boolean _hasPlayStarted;
    protected int _pframes;
    /**
     * the timestamp of the last written (played) packet
     */
    private int _timestampPlay = 0;
    private StampedAudio _holdPlay;
    /* Variables relating to outbound audio: microphone recording going to network */
    private TargetDataLine _rec;
    private byte[] _macbuff;
    private EncoderFace _encode;
    private byte _framebuffR[];
    private Thread _recThread;
    public int _cutsz;
    public float _sampleRate;
    // protected AudioFormat _cdmono;
    protected AudioFormat _bestMacFormat;

    private short[] _macbuffp;
    // a circular buffer for StampedAudio, read from the mic
    private StampedAudio[] _stampedBuffer;
    private int _stampedBufferStart;
    private int _stampedBufferEnd;
    // in milliseconds
    private long _timestampRecStart;
    protected double _inEnergy;
    protected double _outEnergy;
    int _countFrames = 1; // you know what -  don't - lets do one at a time.
    protected CodecFace _defaultCodec;
    private int _pvadhvc;
    private int _pvadvc;
    private static float __mac_rate =  44100.0F;
    protected FloatControl pan;
    private int _dtmfDigit = -1;
    private int _samplesPerFrame;
    /**
     * Creates a new instance of PhonoAudio
     */
    public PhonoAudio() {
        //_cdmono = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 1, 2, 44100.0F, true);
        _bestMacFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, __mac_rate, 16, 1, 2, __mac_rate , true);
        _codecMap = new LinkedHashMap<Long, CodecFace>();
        fillCodecMap();

        // init other variables
        _osname = System.getProperty("os.name");
        if (_osname != null) {
            _osname = _osname.toLowerCase();
        } else {
            _osname = "";
        }
        _javaVersion = System.getProperty("java.specification.version");
        _audioProperties = new Properties();
    }

    protected void fillCodecMap() {
        // add all the supported Codecs, in the order of preference
        G722Codec g722Codec = new G722Codec();
        _codecMap.put(new Long(g722Codec.getCodec()), g722Codec);

        ULaw_Codec ulawCodec = new ULaw_Codec();
        _codecMap.put(new Long(ulawCodec.getCodec()), ulawCodec);

        GSM_Codec gsmCodec = new GSM_Codec();
        _codecMap.put(new Long(gsmCodec.getCodec()), gsmCodec);

        ALaw_Codec alawCodec = new ALaw_Codec();
        _codecMap.put(new Long(alawCodec.getCodec()), alawCodec);

        SLin_Codec slinCodec = new SLin_Codec();
        _codecMap.put(new Long(slinCodec.getCodec()), slinCodec);

        _defaultCodec = gsmCodec;

        _defaultCodec = g722Codec;
    }

    public void printAvailableCodecs() {
        Set<Long> keySet = _codecMap.keySet();
        Iterator<Long> iter = keySet.iterator();
        int i = 0;
        while (iter.hasNext()) {
            Long codecl = iter.next();
            CodecFace codecFace = _codecMap.get(codecl);
            Log.debug("Codec " + i + ": " + codecFace.getName());
            i++;
        }
    }

    public long getTime() {
        long time = 0;
        time = System.currentTimeMillis();
        return time;
    }

    public AudioFormat getAudioFormat() {
        _cutsz = 2 * ((_sampleRate > 8000.0) ? 4 : 2); // for mono - more for  stereo
        Log.debug("PhonoAudio.getAudioFormat(): Cutsz =" + _cutsz);
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, _sampleRate, 16, 1, 2, _sampleRate, true);

    }

    /**
     * Returns an array of available codecs (int values - as specified in IAX RFC).
     * Must return all the codecs that can be supported by implementation.
     * May be called on an uninitialized instance.
     *
     * @return The array of available codecs
     */
    public long[] getCodecs() {
        int len = _codecMap.size();
        long codecs[] = new long[len];

        Set<Long> keySet = _codecMap.keySet();
        Iterator<Long> iter = keySet.iterator();
        int i = 0;
        while (iter.hasNext()) {
            long codec = iter.next().longValue();
            codecs[i] = codec;
            i++;
        }
        return codecs;
    }

    public boolean isCodecAvailable(long codec) {
        Long codecL = new Long(codec);
        return _codecMap.containsKey(codecL);
    }

    /**
     * Adds a class that will be notified of incomming audio
     * from the microphone.
     * Implementing class - eg PhonoAudio -
     * will call readStampedAudio() and then send data out over wire.
     * AudioReceiver can be null - in which case readStampedAudio()
     * should be called directly.
     * May be called on an uninitialized instance.
     *
     * @param r The AudioReceiver to be notified
     * @see #readStampedAudio()
     */
    public void addAudioReceiver(AudioReceiver r) throws AudioException {
        _audioReceiver = r;
    }

    synchronized public void unInit() {
        _codec = null;
        stopPlay();
        stopRec();
        _rec = null;
        _play =null;
        _sampleRate = 0;
        _deep = 0;
        _samplesPerFrame =0;
        _bytesPerFrame =0;
        _codecFrameSize = 0;
        _stampedBuffer = null;
        _encode = null;
        _decode = null;
        _encodedbuffPlay= null;
        _framebuffR = null;
        _macbuff = null;
        Log.debug("uninit()ed audio device");
    }
    /**
     * Actually allocate resources.
     * The constructor should not allocate any resources.
     *
     * @param codec integer representing the codec to use.
     * Must be one of the values in the array returned by getCodecs().
     * May be called on an uninitialized instance.
     *
     * @see #getCodecs()
     */
    synchronized public void init(long codec, int latency) throws AudioException {
        _codec = _codecMap.get(new Long(codec));
        if (_codec == null) {
            _codec = getDefaultCodec();
            Log.warn("Using default codec:" + _codec.getName());
        }
        _sampleRate = _codec.getSampleRate();
        String text = "PhonoAudio.init(): codec=" + this.getCodecString(codec) + " (" + codec + ")";

        if (_codec == null) {
            Log.debug(text + " is not supported.");
            throw new AudioException(text + " is not supported");
        } else {
            Log.debug("PhonoAudio.init(): " + _codec.toString());

            // apply audio properties again, in case it is codec related!
            Enumeration e = _audioProperties.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                Object value = _audioProperties.getProperty(key);
                setAudioProperty(key, value);
            }
        }

        if (latency == 0) {
            latency = 120;
        }

        _deep = 2 * (latency / 20);


        // frame interval in milliseconds is:
        int frameIntMS = _codec.getFrameInterval();
        // frame rate per sec is:
        int frameRateSec = 1000 / frameIntMS;
        // samples per frame, at 8kHz, is:
        _samplesPerFrame = (int) (_sampleRate / frameRateSec);
        // sample is in short, so frame size in bytes is:
        _bytesPerFrame = _samplesPerFrame * 2;

        _codecFrameSize = _codec.getFrameSize();
        _stampedBuffer = new StampedAudioImpl[frameRateSec];

        _deep = Math.max(6, _deep);
        if (_javaVersion.startsWith("1.4")) {
            _deep = Math.max(10, _deep);
        } else if ("linux".equals(_osname)) {
            _deep = Math.max(10, _deep);
        }
        _deep = Math.min(20, _deep);

        Log.debug("PhonoAudio.init(): _deep=" + _deep);

        // open Audio: mic & speakers/headset
        this.initPlay();
        this.initMic(); // so if this fails we should stream? ....
    }

    /**
     * Returns the optimum size of a frame.
     * May not be called on an uninitialized instance.
     * @throws IllegalStateException
     */
    public int getFrameSize() throws IllegalStateException {
        int ret;
        if (_codec != null) {
            ret = _codec.getFrameSize();
        } else {
            throw new IllegalStateException("PhonoAudio.getFrameSize(): codec hasn't been set yet, init first");
        }
        return ret;
    }

    /**
     * Returns the frame interval in ms,
     * normally 20.
     * May not be called on an uninitialized instance.
     * @throws IllegalStateException
     */
    public int getFrameInterval() throws IllegalStateException {
        int ret;
        if (_codec != null) {
            ret = _codec.getFrameInterval();
        } else {
            Log.warn("PhonoAudio.getFrameInterval(): codec hasn't been set yet, init first");
            ret = 20;
        }
        return ret;
    }

    /**
     * Returns the current codec in use (int values - as specified in IAX RFC).
     * May not be called on an uninitialized instance.
     * @throws IllegalStateException
     */
    public long getCodec() throws IllegalStateException {
        long ret;
        if (_codec != null) {
            ret = _codec.getCodec();
        } else {
            throw new IllegalStateException("PhonoAudio.getCodec(): codec hasn't been set yet, init first");
        }
        return ret;
    }

    /**
     * Obtains a 'cleaned' or unused StampedAudio,
     * used as a factory method in place of 'new'.
     * Implementations may choose to recycle StampedAudios after they
     * have been read.
     * Implementations may choose to assume that this method is _only_
     * ever called by the protocol stack in order to store data prior to
     * calling writeStampedAudio().
     *
     * @return A clean or unused instance of StampedAudio.
     * @see StampedAudio
     * @see #writeStampedAudio(StampedAudio)
     */
    public StampedAudio getCleanStampedAudio() {
        return new StampedAudioImpl();
    }

    /**
     * Returns if the audio is up (true) or not (false).
     *
     * @return True if the audio is up, else false
     */
    public boolean isAudioUp() {
        return (_rec != null && _play != null);
    }

    /**
     * Returns a copy  of all the available properties of this audio channnel.
     * eg:
     * <ol>
     *    <li>playLevel</li>
     *    <li>recLevel</li>
     *    <li>ec</li>
     *    <li>pitchShift</li>
     *    <li>field</li>
     * </ol>
     */
    public Properties getAudioProperties() {
        return new Properties(_audioProperties);
    }

    /**
     * Sets the named property.
     *
     * @param name The name of the property
     * @param value The value of the property
     *
     * @return false if property unsupported or not settable,
     * true if property set.
     * @throws IllegalArgumentException if value invalid
     */
    public boolean setAudioProperty(String name, Object value)
            throws IllegalArgumentException {
        Log.debug("PhonoAudio.setAudioProperty(): name=" + name + ", value=" + value);

        _audioProperties.put(name, value);
        if (name.equalsIgnoreCase(PhonoAudioPropNames.DOVAD) && (_codec instanceof VoiceDetectionCodec)) {
            VoiceDetectionCodec vcodec = (VoiceDetectionCodec) _codec;
            String v = (String) value;
            boolean doVAD = Boolean.parseBoolean(v);
            if (doVAD) {
                vcodec.startDetector();
            } else {
                vcodec.stopDetector();
            }
        }
        return false;
    }

    // data from mic
    public short[] effectIn(short[] in) {
        double energy = 0;
        for (int i = 0; i < in.length; i++) {
            energy = energy + (Math.abs(in[i]));
        }
        _inEnergy = energy / in.length;
        if (_inEnergy > 100) {
            _pvadhvc++;
        }
        _pvadvc++;
        return in;
    }

    // data to speakers
    public short[] effectOut(short[] out) {
        double energy = 0;
        for (int i = 0; i < out.length; i++) {
            energy = energy + (Math.abs(out[i]));
        }
         int ld = _dtmfDigit;
                if ( ld >= 0) {
                    long k = _pframes * _samplesPerFrame;
                    if ((_pframes % 5) == 0) Log.debug("bleep "+_dtmfDigit);
                    for (int j = 0; j < out.length; j++) {
                        out[j] = (short) (getDigitSample(ld, j+k, _sampleRate) + out[j] / 2);
                    }
                }
        _outEnergy = energy / out.length;
        return out;
    }

    private CodecFace getDefaultCodec() {
        return _defaultCodec;
    }

    /**
     */
    private void initPlay() throws AudioException {
        if (_play == null) {
            // TODO: other codecs might not work very well with 8k!
            AudioFormat pfmt = getAudioFormat();
            /*if ("mac os x".equals(_osname)) {
            pfmt = _cdmono; // attempt to solve mac buzz problem. - failed.
            _macbuffp = new short[ (int) (44100.0/1000.0 * _codec.getFrameInterval())];
            }*/
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, pfmt, AudioSystem.NOT_SPECIFIED);
            _decode = _codec.getDecoder();
            _encodedbuffPlay = _codecFrameSize > 0 ? new byte[_codecFrameSize] : null;

            int playBuffsz = _bytesPerFrame * _deep;

            try {
                _play = (SourceDataLine) (DataLine) AudioSystem.getLine(info);
                _play.open(pfmt, playBuffsz);

                Log.verb("PhonoAudio.initPlay(): audioFormat: " + pfmt);
                Log.verb("PhonoAudio.initPlay(): format: " + _play.getFormat());
                Log.verb("PhonoAudio.initPlay(): buffer size: " + _play.getBufferSize());

                Control[] c = _play.getControls();
                if (c.length > 0) {
                    Log.debug("PhonoAudio.initPlay(): _play has " + c.length + " controls.");
                    for (int i = 0; i < c.length; i++) {
                        Control control = c[i];
                        Type type = control.getType();
                        if ("Pan".equals(type.toString())){
                            pan = (FloatControl) control;
                        }
                        Log.debug("  _play control: " + control + ", type=" + type.toString() + ", " + type.getClass().getName());
                    }
                }

            } catch (LineUnavailableException lue) {
                lue.printStackTrace();
//                Log.database(lue, this, "initPlay");
                throw new AudioException(lue.getMessage(), lue);
            } catch (Exception e) {
                e.printStackTrace();
//                Log.database(e, this, "initPlay");
                throw new AudioException(e.getMessage(), e);
            }
        }
    }

    private void orderedWrite(StampedAudio stampedAudio, StampedAudio next) throws AudioException {
        int offs = stampedAudio.getOffset();
        int flen = stampedAudio.getLength();
        byte[] bs = stampedAudio.getData();
        if (next != null) {
            // fill in the gap before stampedAudio
            // this DOESN'T write 'next'
            byte[] bs1 = new byte[flen];
            System.arraycopy(bs, offs, bs1, 0, flen);
            byte[] bs2 = new byte[next.getLength()];
            System.arraycopy(next.getData(), next.getOffset(), bs2, 0, next.getLength());
            byte[] bs0 = _decode.lost_frame(bs1, bs2);
            if (bs0 != null) {
                Log.debug("PhonoAudio.orderedWrite(): recovering lost packet");
                writeBuff(bs0.length, 0, bs0);
            }
        }
        int dlen = writeBuff(flen, offs, bs);
        _timestampPlay = stampedAudio.getStamp();
        Log.verb("PhonoAudio.orderedWrite(): Wrote packet to audio timestamp=" + _timestampPlay + " len =" + dlen);
    }

    private int writeBuff(int flen, int offs, byte[] bs) throws AudioException {
        int av = _play.available();
        int olen = 0;
        // variable length - have to assume that it is a whole packet
        int cfs = _codecFrameSize > 0 ? _codecFrameSize : flen;
        while ((flen >= cfs) && (flen > 0)) {
            byte[] ebuff = (_encodedbuffPlay != null) ? _encodedbuffPlay : new byte[flen];
            System.arraycopy(bs, offs, ebuff, 0, cfs);
            short[] sframe = _decode.decode_frame(ebuff);
            int dlen = 0;
            if (sframe != null) {
                int trimmed =0;
                short[] eframe = effectOut(sframe);
                if (_macbuffp != null) {
                    eframe = upsample(eframe, _macbuffp);
                }
                byte[] framebuffP = CodecUtil.shortsToBytes(eframe);

                dlen = framebuffP.length;
                if (dlen > av) {
                    if (this._outEnergy < QUIET) {
                        trimmed = _cutsz - dlen;
                        dlen = _cutsz; // its so quiet we can lop off _lots_
                    } else {
                        trimmed = -_cutsz;
                        dlen -= _cutsz; // lop a sample off - hope they will catch up....
                    }
                    Log.debug("PhonoAudio.writeBuff(): note (" + _pframes + ") trimming play to " + dlen + ", av=" + av);
                }
                int plink = _play.write(framebuffP, 0, dlen);
                /* don't do this EXPERIMENT
                if ((_play.getBufferSize() - av) < dlen) {
                    // if the buffer is empty, pad it out a bit with the last bit
                    _play.write(framebuffP, dlen - _cutsz, _cutsz);
                    trimmed = _cutsz;
                }
                */
                trim(trimmed/2);

            // record this play (speaker) sound
            // _playF.write(framebuffP, 0, dlen);
            }

            _pframes++;
            flen -= cfs;
            offs += cfs;
            olen += dlen;
        }
        if (flen != 0) {
            Log.debug("PhonoAudio.writeBuff(): short frame = " + flen);
        }
        Log.verb("PhonoAudio.writeBuff(): av=" + av + ", pframes=" + _pframes);
        return olen;
    }

    /**
     * Sends audio data to the speakers.
     *
     * @param stampedAudio The stamped audio to play
     */
    // is/was PhonoAudio.audioOut(byte[] bs, int offs, int flen)
    public void writeStampedAudio(StampedAudio stampedAudio) throws AudioException {
        if (_play != null) {
            int stamp = stampedAudio.getStamp();
            if (_pframes == 0) {
                // init _stamp, make sure it less than the current audio.
                _timestampPlay = stamp - _codec.getFrameInterval();
            }
            if (_hasPlayStarted == false) {
                if (_pframes >= (_deep / 2)) {
                    Log.debug("PhonoAudio.writeStampedAudio(): framesz=" + _bytesPerFrame + ", pframes=" + _pframes + ", deep=" + _deep);
                    startPlay();
                }
            }
            int nextStamp = _timestampPlay + _codec.getFrameInterval();
            if ((_holdPlay != null) && (_holdPlay.getStamp() < stamp)) {
                // assume missing one was dropped so play this packet anyhow,
                StampedAudio a = _holdPlay;
                Log.verb("PhonoAudio.writeStampedAudio(): about to play a delayed packet " + _holdPlay.getStamp() + " expecting " + nextStamp);
                orderedWrite(_holdPlay, stampedAudio);
                nextStamp = _holdPlay.getStamp() + _codec.getFrameInterval();
                _holdPlay = null;
            }
            if ((stamp > nextStamp + 5) && (_holdPlay == null)) {
                // lost one for now - hope it is mis-ordered
                // hold this packet for later use, do not play it yet
                _holdPlay = stampedAudio;
                Log.verb("PhonoAudio.writeStampedAudio(): Held a misordered packet " + stamp + " expecting " + nextStamp);
            } else {
                if (stamp > _timestampPlay) {
                    orderedWrite(stampedAudio, null);
                } else {
                    // did we wrap?? or perhaps transfer ?
                    if ((_timestampPlay - stamp) > 1000) {
                        // this will set _timestampPlay again
                        orderedWrite(stampedAudio, null);
                    } else {
                        Log.debug("PhonoAudio.writeStampedAudio(): dumped a duplicate packet " + stamp + " expecting " + nextStamp);
                    }
                }
                if (_holdPlay != null) {
                    Log.verb("PhonoAudio.writeStampedAudio(): about to play a misordered packet " + _holdPlay.getStamp() + " expecting " + nextStamp);
                    orderedWrite(_holdPlay, null);
                    _holdPlay = null;
                }
            }
        } else {
            throw new AudioException("PhonoAudio.writeStampedAudio(): Audio not initialised, call init() first!");
        }
    }

    public void startPlay() {
        if (_play != null) {
            //_play.flush();
            _play.start();
            Log.debug("PhonoAudio.startPlay(): play started");
            _hasPlayStarted = true;
        }
    }

    public void stopPlay() {
        if (_play != null) {
            _play.flush();
            _play.stop();
            Log.debug("PhonoAudio.stopPlay(): play stopped");
            _pframes = 0;
            _hasPlayStarted = false;
        }
    }

    /**
     */
    protected void initMic(Mixer m) throws AudioException {
        if (_rec == null) {
            // TODO: other codecs might not work very well with 8k!
            AudioFormat slin = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, _sampleRate, 16, 1, 2, _sampleRate, true);

            AudioFormat recfmt = slin;

            int recBuffsz = _bytesPerFrame * _deep;

            // macs won't give you 8k slin
            if ("mac os x".equals(_osname)) {
                recfmt = _bestMacFormat;
                int mbpf = _bytesPerFrame * (int)recfmt.getFrameRate() / (int)_sampleRate;
                recBuffsz = mbpf * _deep;
                _macbuff = new byte[mbpf];
            }
            Log.verb("PhonoAudio.initMic(): audioFormat = " + recfmt);
            DataLine.Info info = null;

            if (m == null){
                info = new DataLine.Info(TargetDataLine.class, recfmt, AudioSystem.NOT_SPECIFIED);
            } else {
                info = new DataLine.Info(TargetDataLine.class, recfmt);
            }

            _encode = _codec.getEncoder();
            // _encodedbuffRec = new byte[_codecFrameSize];
            _framebuffR = new byte[_bytesPerFrame];

            try {
                _rec = (TargetDataLine) ( (m== null) ? AudioSystem.getLine(info): m.getLine(info)) ;
                _rec.open(recfmt, recBuffsz);
                Control[] c = _rec.getControls();
                if (c.length > 0) {
                    Log.debug("PhonoAudio.initMic(): _rec has " + c.length + " controls.");
                    for (int i = 0; i < c.length; i++) {
                        Log.debug("_rec control: " + c[i]);
                    }
                }

            } catch (LineUnavailableException lue) {
                // setMute(true);
                lue.printStackTrace();
//                Log.database(lue, this, "initMic");
                throw new AudioException(lue.getMessage(), lue);
            } catch (Exception e) {
//                Log.database(e, this, "initMic");
                e.printStackTrace();
                throw new AudioException(e.getMessage(), e);
            }
        }

        return;
    }

    protected void initMic() throws AudioException {
        initMic(null);
    }
    
    public int getOutboundTimestamp() {
        return (int) (this.getTime() - _timestampRecStart);
    }
    // is/was PhonoAudio.audioIn()

    /**
     * Reads from the microphone into a (circular) buffer of StampedAudio
     */
    private boolean readMic() {
        boolean more = false;
        if (_rec != null) {
            int av = _rec.available();
            int bsz = 0;
            if (_macbuff != null) {
                bsz = _macbuff.length;
            } else {
                bsz = _framebuffR.length;
            }
            if (av >= bsz) {

                int c = _countFrames;
                int timestamp = getOutboundTimestamp();
                /* if (timestamp > 65000) {
                resetTimestampRecStart();
                timestamp = 0;
                }  dont bother... */

                if (_macbuff != null) {
                    _rec.read(_macbuff, 0, _macbuff.length);
                    downsample(_macbuff, _framebuffR);
                } else {
                    _rec.read(_framebuffR, 0, _bytesPerFrame);
                }

                // record this recording (mic) sound
                // openRecordingFiles();
                // _recF.write(_framebuffR, 0, _bytesPerFrame);

                short[] sframe = CodecUtil.bytesToShorts(_framebuffR);
                short[] seframe = effectIn(sframe);
                byte[] tbuff = _encode.encode_frame(seframe);

                Log.verb("PhonoAudio.readMic(): c=" + c + ", av=" + av + ", bsz=" + bsz);

                if (tbuff != null) {
                    StampedAudioImpl stampedAudio = new StampedAudioImpl();
                    stampedAudio.setStampAndBytes(tbuff, 0, tbuff.length, timestamp);
                    saveReadStampedAudio(stampedAudio);
                }
                /*                } */

                if (_audioReceiver != null) {
                    _audioReceiver.newAudioDataReady(this, _codecFrameSize);
                }
                _countFrames++;
                av = _rec.available();
                more = av >= bsz;
            }
        }
        return more;
    }

    // very dumb downsample for the Mac
    void downsample(byte[] in, byte[] out) {
        int curr = 0;
        int cnt = 0;
        int val = 0;
        int x = 0;
        int olen = out.length;
        int ilen = in.length;
        int framesz = olen;
        for (int i = 0; i < in.length; i += 2) {
            // walk through the input data
            x = (0xffffe) & ((i * olen) / ilen);
            // see which slot it maps to in the output
            if (x != curr) {
                // if this is a new x, deal with the old one
                val /= cnt; // scale the value by the count that mapped
                // now stick it in the outputbuffer
                if ((curr + 1) < framesz) {
                    out[curr] = (byte) (0xff & (val >> 8));
                    out[curr + 1] = (byte) (0xff & val);
                }
                // and reset state, ready to move on
                curr = x;
                cnt = 0;
                val = 0;
            }
            // add this sample to the current total
            val += in[i] << 8;
            val += (0xff & in[i + 1]);
            cnt++; // keep a count of samples added
        }
        // and clean up the end case
        if ((cnt != 0) && ((curr + 1) < framesz)) {
            val /= cnt;
            out[curr] = (byte) (0xff & (val >> 8));
            out[curr + 1] = (byte) (0xff & val);
        }

    }

    /**
     * Saves the stamped audio, read from the mic, into a buffer
     */
    private void saveReadStampedAudio(StampedAudio stampedAudio) {
        _stampedBufferEnd++;
        int actualPos = _stampedBufferEnd % _stampedBuffer.length;
        _stampedBuffer[actualPos] = stampedAudio;

        Log.verb("PhonoAudio.saveReadStampedAudio(): pos=" + _stampedBufferEnd + " (" + actualPos + "), " + stampedAudio.toString());

        int distance = _stampedBufferEnd - _stampedBufferStart;
        if (distance > _stampedBuffer.length) {
            Log.debug("PhonoAudio.saveReadStampedAudio(): overflow, start=" + _stampedBufferStart + ", end=" + _stampedBufferEnd);
        }
    }

    /**
     * Reads data from the audio queue.
     * May block for upto 2x frame interval,
     * otherwise returns null.
     * May throw an exception if Audio Channel is not set up correctly.
     *
     * @return a StampedAudio containing a framesize worth of data.
     * @see #getFrameSize()
     */
    public StampedAudio readStampedAudio() throws AudioException {
        StampedAudio stampedAudio = null;
        if (_rec != null) {
            if (_stampedBufferStart < _stampedBufferEnd) {
                int actualPos = _stampedBufferStart % _stampedBuffer.length;
                stampedAudio = _stampedBuffer[actualPos];

                if (stampedAudio != null) {
                    Log.verb("PhonoAudio.readStampedAudio(): pos=" + _stampedBufferStart + " (" + actualPos + "), " + stampedAudio.toString());
                }

                _stampedBufferStart++;
            }
        } else {
            throw new AudioException("PhonoAudio.readStampedAudio(): Audio not initialised, call init() first!");
        }
        return stampedAudio;
    }

    /*
     * TODO: bit of a problem: first call to this method comes too late for the
     * first new/accept, etc frames
     */
    private void resetTimestampRecStart() {
        // convert from micro to milli seconds
        _timestampRecStart = getTime();
    }

    public void startRec() {
        if (_rec != null) {
            _rec.flush();
            _rec.start();
            Log.debug("PhonoAudio.startRec(): rec started");

            resetTimestampRecStart();

            if (_recThread == null) {
                Runnable runnable = new Runnable() {

                    public void run() {
                        while (_recThread != null) {
                            boolean more = readMic();
                            try {
                                long nap = getFrameInterval();
                                if (more) nap -=1;
                                Thread.sleep(nap);
                            } catch (InterruptedException ex) {
                                Log.debug("PhonoAudio.startRec(): InterruptedException: " + ex.getMessage());
                            }
                        }
                    }
                };
                _countFrames = 0;
                _recThread = new Thread(runnable, "mic");
                _recThread.setPriority(Thread.NORM_PRIORITY);
                _recThread.start();
                Log.debug("PhonoAudio.startRec(): recThread started");
            }
        }
    }

    public void stopRec() {
        if (_rec != null) {
            _rec.stop();
            Log.debug("PhonoAudio.stopRec(): rec stopped");
            _pframes = 0;
        }

        if (_recThread != null) {
            Thread trec = _recThread;
            _recThread = null;
            try {
                trec.join(1000);
                Log.debug("PhonoAudio.stopRec(): recThread stoppped");
            } catch (InterruptedException ex) {
                Log.debug("PhonoAudio.stopRec(): InterruptedException: " + ex.getMessage());

            }
        }
    }

    public void destroy() {
        boolean closeLine = true;
        /*if (_osname.startsWith("mac")) {
            closeLine = false;
        }*/
        stopRec();
        stopPlay();
        if (closeLine == true) {
            Log.debug("PhonoAudio.destroy(): closeLine=" + closeLine);
            if (_play != null) {
                _play.close();
                Log.debug("play closed");
            }
            if (_rec != null) {
                _rec.close();
                Log.debug("rec closed");
            }
        }
    }

    public String getCodecString(long codec) {
        int count = 0;
        while (codec > 1) {
            codec = codec >> 1;
            count++;
        }

        String str = "Unknown Codec";
        if (count < CodecFace.CODECS.length) {
            str = CodecFace.CODECS[count];
        }
        return str;
    }

    short[] upsample(short[] in, short out[]) {
        double rat = 1.0 * in.length / out.length;
        short pval = out[out.length - 1]; // old value from previous shift.
        for (int o = 0; o < out.length; o++) {
            int ioff = (int) Math.floor(o * rat);
            double deltat = (o * rat) - ioff;
            double oval = in[ioff];
            double deltav = (pval - in[ioff]) * deltat;
            out[o] = (short) (oval + deltav);
            Log.verb("PhonoAudio.upsample(): in[" + ioff + "]=" + in[ioff] + " pval=" + pval + " deltat=" + deltat + " deltav=" + deltav + " out[" + o + "]=" + out[o] + " rat=" + rat);
            if (ioff > 0) {
                pval = in[ioff];
            }
        }
        return out;
    }

    public double[] getEnergy() {
        double[] ret = new double[2];
        ret[0] = this._inEnergy;
        ret[1] = this._outEnergy;
        return ret;
    }

    public String getCodecName() {
        String ret = "";
        if (_codec != null) {
            ret = _codec.getName();
        }
        return ret;
    }

    public void updateRemoteStats(NetStatsFace r) {
        if (_codec instanceof TuneableCodec) {
            ((TuneableCodec) _codec).setStats(r);
        }
    }

    public TargetDataLine getRec() {
        return _rec;
    }

    public int getVADpc() {
        int ret = -1;
        if (_codec instanceof VoiceDetectionCodec) {
            ret = ((VoiceDetectionCodec) _codec).getVoicePercent();
        } else {
            if (_pvadvc > 0) {
                ret = (100 * _pvadhvc) / _pvadvc;
                _pvadvc = 0;
                _pvadhvc = 0;
            }
        }
        return ret;
    }

    public boolean doVAD() {
        boolean doVAD = false;
        if (_codec instanceof VoiceDetectionCodec) {
            doVAD = ((VoiceDetectionCodec) _codec).isDetecting();
        }
        return doVAD;
    }
    protected void trim(int numberOfSamplesRemovedOrAdded){
        // do nothing here, but echo cans want to know
    }

    public double getSampleRate() {
       return this._sampleRate;
    }
    long toneMap[][] = {{1336, 941}, {1209, 697}, {1336, 697}, {1477, 696}, {1209, 770}, {1336, 770}, {1477, 770}, {1209, 852}, {1336, 852}, {1447, 852}, {1209, 941}, {1477, 941}};

    short getDigitSample(int digit, long position, float rate) {
        double n1 = (2 * Math.PI) * toneMap[digit][0] / rate;
        double n2 = (2 * Math.PI) * toneMap[digit][1] / rate;
        return (short) (((Math.sin(position * n1) + Math.sin(position * n2)) / 4) * Short.MAX_VALUE);
    }
    public void playDigit(char c) {
        String valid = "0123456789#*";
        _dtmfDigit = valid.indexOf(c);
        Log.debug("DtmfDigit is "+_dtmfDigit);
    }

    public void setMicGain(float f) {
    }

    public void setVolume(double d) {
    }

    public void muteMic(boolean v) {
    }

    public boolean callHasECon() {
        return false;
    }

    public CodecFace getCodec(long codec) {
        Long codecL = new Long(codec);
        return (CodecFace) _codecMap.get(codecL);

    }
}
