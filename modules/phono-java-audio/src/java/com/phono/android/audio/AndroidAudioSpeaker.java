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


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import com.phono.audio.codec.CodecFace;
import com.phono.audio.AudioException;
import com.phono.audio.StampedAudio;
import com.phono.srtplight.Log;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


public class AndroidAudioSpeaker implements Runnable {

    @SuppressWarnings("unused")
    private final static int SILENT = 250;
    private AndroidAudio _audio;
    // Codec related
    private CodecFace _codec;
    // Speaker related
    private AudioTrack _speaker = null;
    private byte _encodedbuffSpeaker[];
    private boolean _hasSpeakerStarted;
    protected int _speakerFrames;
    private int _timestampSpeaker = 0;
    private StampedAudio _holdSpeaker;
    private double _outEnergy;
    private Thread _me;
    private int _sampleRate;
    private int _origSampleRate;
    //private ArrayList <StampedAudio> _listStampedAudio;
    private ArrayBlockingQueue<StampedAudio> _qStampedAudio;
    private final static int MAXQ = 20;

    public AndroidAudioSpeaker(CodecFace codec, AndroidAudio androidAudio) {
        _codec = codec;
        _audio = androidAudio;
    }

    protected boolean initSpeaker(int sampleRate, int bytesPerFrame) {
        boolean isOK = true;
        _sampleRate = sampleRate;
        _origSampleRate = sampleRate;
        //_listStampedAudio = new ArrayList<StampedAudio>();
        _qStampedAudio = new ArrayBlockingQueue<StampedAudio>(MAXQ);
        if (_speaker == null) {
            int codecFrameSize = _codec.getFrameSize();
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AndroidAudio.ENCODING_FORMAT);
            int deep = 1 + (minBufferSize / bytesPerFrame);
            deep = Math.max(deep, 5);
            int speakerBuffSizeBytes = deep * bytesPerFrame;
            if (speakerBuffSizeBytes < minBufferSize) {
                speakerBuffSizeBytes = minBufferSize;
            }
            _encodedbuffSpeaker = null;
            if (codecFrameSize > 0) {
                _encodedbuffSpeaker = new byte[codecFrameSize];
            }

            Log.info(
                    this.getClass().getSimpleName()
                    + ".initSpeaker(): sampleRate=" + sampleRate);
            Log.info(
                    this.getClass().getSimpleName()
                    + ".initSpeaker(): speakerBuffsz=" + speakerBuffSizeBytes);
            Log.info(
                    this.getClass().getSimpleName()
                    + ".initSpeaker(): minBufferSize=" + minBufferSize);

            try {
                _speaker = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AndroidAudio.ENCODING_FORMAT, speakerBuffSizeBytes,
                        AudioTrack.MODE_STREAM);
                _speaker.setPlaybackRate(sampleRate);
                if (_speaker.getState() != AudioTrack.STATE_INITIALIZED) {
                    isOK = false;
                }
            } catch (Throwable exc) {
                Log.error(exc.toString());
                isOK = false;
            }
        }
        return isOK;
    }

    /**
     * Sends audio data to the speakers.
     * 
     * @param stampedAudio
     *            The stamped audio to play
     */
    protected void writeStampedAudio(StampedAudio stampedAudio)
            throws AudioException {
        if (_speaker != null) {


            if (_hasSpeakerStarted == false) {
                startPlay();

                /*
                 * if (_speakerFrames >= (_deep / 2)) {
                 * Log.debug( this.getClass().getSimpleName() +
                 * ".writeStampedAudio(): _bytesPerFrame=" + _bytesPerFrame +
                 * ", _speakerFrames=" + _speakerFrames + ", deep=" + _deep); }
                 */
            }
            this.setLastStampedAudio(stampedAudio);
            //writeStampedAudio2(stampedAudio);
        } else {
            throw new AudioException(
                    this.getClass().getSimpleName()
                    + ".writeStampedAudio(): Audio not initialised, call initSpeaker() first!");
        }
    }

    protected int getSpeakerFrames() {
        return _speakerFrames;
    }

    protected void startPlay() {
        if (_speaker != null
                && _speaker.getState() == AudioTrack.STATE_INITIALIZED) {
            Log.debug(
                    this.getClass().getSimpleName()
                    + ".startPlay(): starting speaker");
            _speaker.play();
            _hasSpeakerStarted = true;

            Log.debug(
                    this.getClass().getSimpleName() + ".startPlay(): state="
                    + _speaker.getState()
                    + ", playState=" + _speaker.getPlayState());

            if (_me == null) {
                _me = new Thread(this, "speaker");
                _me.setPriority(Thread.NORM_PRIORITY);
                _me.start();
                Log.debug(
                        this.getClass().getSimpleName()
                        + ".startPlay(): speakerThread started");
            }
        } else {
            Log.error(
                    this.getClass().getSimpleName()
                    + ".startPlay(): Failed to initialise speakers.");
        }
    }

    protected void stopPlay() {
        if ((_speaker != null) && (_speaker.getState() == AudioTrack.STATE_INITIALIZED)) {
            if (_speaker.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                _speaker.flush();
                _speaker.stop();
            }
            Log.debug(
                    this.getClass().getSimpleName()
                    + ".stopPlay(): speaker stopped");
            _speakerFrames = 0;
            _hasSpeakerStarted = false;

            if (_me != null) {
                Thread tspeaker = _me;
                _me = null;
                try {
                    tspeaker.join(1000);
                    Log.debug(
                            this.getClass().getSimpleName()
                            + ".stopPlay(): speakerThread stoppped");
                } catch (InterruptedException ex) {
                    Log.debug(
                            this.getClass().getSimpleName()
                            + ".stopPlay(): InterruptedException: "
                            + ex.getMessage());
                }
            }
        }
    }

    protected double getOutEnergy() {
        return _outEnergy;
    }

    @Override
    public void run() {
        Log.debug(this.getClass().getSimpleName() + ".run()");
        while (_me != null) {
            StampedAudio stampedAudio = this.getLastStampedAudio();
            if (stampedAudio != null) {
                //writeStampedAudio2(stampedAudio); // dump the complexity
                orderedWrite(stampedAudio);
            }
        }
    }

    protected void destroy() {
        Log.debug(
                this.getClass().getSimpleName() + ".destroy(): ");
        if (_speaker != null) {
            _speaker.release();
            Log.debug("\tspeaker released");
        }
    }

    private void orderedWrite(StampedAudio stampedAudio) {
        int offs = stampedAudio.getOffset();
        int flen = stampedAudio.getLength();
        byte[] bs = stampedAudio.getData();

        _timestampSpeaker = stampedAudio.getStamp();
        int bytesWritten = writeBuff(flen, offs, bs);
        _audio.releaseStampedAudio(stampedAudio);
        Log.verb(
                this.getClass().getSimpleName()
                + ".orderedWrite(): Wrote packet to audio timestamp="
                + _timestampSpeaker + ", bytesWritten=" + bytesWritten);
    }

    private int writeBuff(int flen, int offs, byte[] bs) {
        int olen = 0;
        // variable length - have to assume that it is a whole packet
        int codecFrameSize = _codec.getFrameSize();
        int cfs = codecFrameSize > 0 ? codecFrameSize : flen;
        while ((flen >= cfs) && (flen > 0)) {
            byte[] ebuff = null;
            if (_encodedbuffSpeaker != null) {
                ebuff = _encodedbuffSpeaker;
            } else {
                ebuff = new byte[flen];
            }

            System.arraycopy(bs, offs, ebuff, 0, cfs);
            short[] sframe = _codec.getDecoder().decode_frame(ebuff);
            int written = 0;
            if (sframe != null) {
                short[] eframe = effectOut(sframe);
                // This is blocking, therefore it runs on the separate speaker
                // thread,
                // so it does not block the recv thread
                if ((_qStampedAudio.remainingCapacity() < 2) && (_outEnergy < SILENT)) {
                    Log.verb(
                            this.getClass().getSimpleName()
                            + ".writeBuff(): Skip this quiet frame - buffer is nearly full");
                } else {
                    written = _speaker.write(eframe, 0, eframe.length);
                    _speakerFrames++;
                    Log.verb(
                            this.getClass().getSimpleName()
                            + ".writeBuff(): _speakerFrames="
                            + _speakerFrames + ", written="
                            + written);
                }
            }
            flen -= cfs;
            offs += cfs;
            olen += written;
        }
        if (flen != 0) {
            Log.verb(
                    this.getClass().getSimpleName()
                    + ".writeBuff(): short frame = " + flen);
        }
        return olen;
    }

    // data to speakers
    short[] effectOut(short[] out) {
        double energy = 0;
        for (int i = 0; i < out.length; i++) {
            energy = energy + (Math.abs(out[i]));
        }
        _outEnergy = energy / out.length;
        return out;
    }

    private void setLastStampedAudio(StampedAudio stampedAudio) {
        /*
        synchronized (_listStampedAudio) {
        if (_listStampedAudio.size() < MAXQ) {
        _listStampedAudio.add(stampedAudio);
        _listStampedAudio.notifyAll();
        } else {
        Log.debug(
        this.getClass().getSimpleName()
        + ".setLastStampedAudio(): dumping excess frame ");
        _audio.releaseStampedAudio(stampedAudio);
        }
        }*/

        boolean accepted = _qStampedAudio.offer(stampedAudio);
        if (!accepted) {
            Log.verb(
                    this.getClass().getSimpleName()
                    + ".setLastStampedAudio(): dumping excess frame ");
            _audio.releaseStampedAudio(stampedAudio);
            tweakSpeed(+8);
        }

    }

    private StampedAudio getLastStampedAudio() {
        StampedAudio stampedAudio = null;
        /*
        if (_listStampedAudio != null) {
        synchronized (_listStampedAudio) {
        if (_listStampedAudio.size() > 0) {
        stampedAudio = _listStampedAudio.remove(0);
        } else {
        try {
        _listStampedAudio.wait(100);
        } catch (InterruptedException e) {
        ;
        }
        }
        }
        }*/
        try {
            stampedAudio = _qStampedAudio.poll(20, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            tweakSpeed(-8);
        }

        return stampedAudio;
    }

    public static byte[] shortsToBytes(short shortBuffer[]) {
        int len = shortBuffer.length;
        byte[] output = new byte[len * 2];
        int j = 0;

        for (int i = 0; i < len; i++) {
            output[j++] = (byte) (shortBuffer[i] >>> 8);
            output[j++] = (byte) (0xff & shortBuffer[i]);
        }
        return output;
    }

    private void tweakSpeed(int i) {
        //int sampleRate = _speaker.getPlaybackRate();
        _sampleRate += i;
        if ((((_origSampleRate -_sampleRate)*100.0)/_origSampleRate) < 2.0){
            _speaker.setPlaybackRate(_sampleRate);
        } else {
            _sampleRate -= i;
        }
        Log.verb(
                this.getClass().getSimpleName()
                + ".tweakSpeed(): would set speed to " + _sampleRate);
    }

    AudioTrack getAudioTrack() {
        return _speaker;
    }
}
