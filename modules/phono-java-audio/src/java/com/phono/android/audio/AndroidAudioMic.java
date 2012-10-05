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
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

import com.phono.audio.codec.*;
import com.phono.audio.StampedAudio;
import com.phono.srtplight.Log;

public class AndroidAudioMic implements Runnable {

    @SuppressWarnings("unused")
    private AndroidAudio _audio;
    // Codec related
    private EncoderFace _encoder;
    // in milliseconds
    private long _timestampRecStart;
    // mic related
    private AudioRecord _mic = null;
    private short _micFramebuff[];
    private Thread _me;
    private int _countFrames;
    private double _inEnergy;
    private float _gain;

    public AndroidAudioMic(CodecFace codec, AndroidAudio androidAudio) {
        _encoder = codec.getEncoder();
        _audio = androidAudio;
    }

    synchronized protected boolean initMic(int sampleRate, int bytesPerFrame) {
        boolean isOK = true;
        if (_mic == null) {
            // Will this work? In the emulator only 8k works!
            int micBufferSizeBytes = AudioRecord.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AndroidAudio.ENCODING_FORMAT);

            Log.debug(
                    this.getClass().getSimpleName()
                    + ".init(): minMicBufferSizeBytes (1)="
                    + micBufferSizeBytes);

            if (micBufferSizeBytes < bytesPerFrame) {
                micBufferSizeBytes = bytesPerFrame;
            }

            int mdeep = 2 + (micBufferSizeBytes / bytesPerFrame);

            mdeep = Math.max(mdeep, 4);

            int recBuffSizeBytes = bytesPerFrame * mdeep;
            _micFramebuff = new short[bytesPerFrame / 2];

            Log.info(
                    this.getClass().getSimpleName() + ".initMic(): sampleRate="
                    + sampleRate + ", bytesPerFrame=" + bytesPerFrame
                    + ", recBuffsz=" + recBuffSizeBytes + ", ");
            try {
                /*
                 * make sure you add to AndroidManifest.xml:
                 * 
                 * <uses-permission
                 * android:name="android.permission.RECORD_AUDIO"/>
                 */
                /*
                 * TODO, only read now that
                 * 44100Hz is currently the only rate that is guaranteed to work on all devices!
                 * I set it to 8000K!
                 * Need to check this!
                 */
                // try and take advantage of an echo can if there is one.
                int micdevice = (android.os.Build.VERSION.SDK_INT >= 11) ? 7 : AudioSource.MIC;
                _mic = new AudioRecord(micdevice, sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AndroidAudio.ENCODING_FORMAT, recBuffSizeBytes);
                if (_mic.getState() != AudioRecord.STATE_INITIALIZED) {
                    isOK = false;
                }
            } catch (Throwable exc) {
                Log.error(exc.toString());
                isOK = false;
            }
        }
        return isOK;
    }

    protected int getOutboundTimestamp() {
        return (int) _countFrames * _audio.getFrameInterval();
    }

    protected void startRec() {
        if (_mic != null && _mic.getState() == AudioRecord.STATE_INITIALIZED) {
            Log.debug(
                    this.getClass().getSimpleName()
                    + ".startRec(): starting mic");
            _mic.startRecording();
            resetTimestampRecStart();
            _countFrames = 0;

            Log.debug(
                    this.getClass().getSimpleName() + ".startRec(): state="
                    + _mic.getState() + ", recordingState="
                    + _mic.getRecordingState());

            if (_me == null) {
                _me = new Thread(this, "mic");
                //_me.setPriority(Thread.NORM_PRIORITY);
                _me.start();
                Log.debug(
                        this.getClass().getSimpleName()
                        + ".startRec(): micThread started ");
            }
        } else {
            Log.error(
                    this.getClass().getSimpleName()
                    + ".startRec(): Failed to initialise microphone.");
        }
    }

    protected void stopRec() {
        if ((_mic != null) && (_mic.getState() == AudioRecord.STATE_INITIALIZED)) {
            if (_mic.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                _mic.stop();
            }
            Log.debug(this.getClass().getSimpleName()
                    + ".stopRec(): mic stopped");
        }

        if (_me != null) {
            Thread tmic = _me;
            _me = null;
            try {
                tmic.join(1000);
                Log.debug(
                        this.getClass().getSimpleName()
                        + ".stopRec(): micThread stoppped");
            } catch (InterruptedException ex) {
                Log.debug(
                        this.getClass().getSimpleName()
                        + ".stopRec(): InterruptedException: "
                        + ex.getMessage());
            }
        }
    }

    protected void destroy() {
        Log.debug(
                this.getClass().getSimpleName() + ".destroy(): ");
        if (_mic != null) {
            _mic.release();
            Log.debug("\tmic released");
        }
    }

    protected double getInEnergy() {
        return _inEnergy;
    }

    public void run() {
        //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        //_mic.startRecording();
        long ftime = _audio.getFrameInterval();
        while (_me != null) {
            readMic();

            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Log.verb(this.getClass().getSimpleName()
                        + ".run(): InterruptedException: " + ex.getMessage());
            }

        }
    }

    protected StampedAudio readMic() {
        StampedAudio stampedAudio = null;

        if (_mic != null) {
            int timestamp = getOutboundTimestamp();
            // read is blocking.
            int bufferRead = _mic.read(_micFramebuff, 0, _micFramebuff.length);
            Log.verb(
                    this.getClass().getSimpleName()
                    + ".readMic(): length=" + _micFramebuff.length
                    + ", bufferRead=" + bufferRead);

            short[] sframe = _micFramebuff;
            short[] seframe = effectIn(sframe);
            //long then = 0;
            //if ((_countFrames % 500) == 0) { then = getTime();}
            byte[] tbuff = _encoder.encode_frame(seframe);

            //if ((_countFrames % 50) == 0) {long diff = getTime() - then;
            //    Log.debug("encode took "+ diff);
            //}
            if (tbuff != null) {
                stampedAudio = _audio.getCleanStampedAudio();
                stampedAudio.setStampAndBytes(tbuff, 0, tbuff.length,
                        timestamp);
                _audio.saveReadStampedAudio(stampedAudio);

                _countFrames++;
            }

        }
        return stampedAudio;
    }

    // data from mic
    protected short[] effectIn(short[] in) {
        double energy = 0;
        for (int i = 0; i < in.length; i++) {
            in[i] = (short) (in[i] * _gain);
            energy = energy + (Math.abs(in[i]));
        }
        _inEnergy = energy / in.length;
        return in;
    }

    void setGain(float g) {
        _gain = g;
    }
    /*
     * TODO: bit of a problem: first call to this method comes too late for the
     * first new/accept, etc frames
     */

    private void resetTimestampRecStart() {
        // convert from micro to milli seconds
        _timestampRecStart = getTime();
    }

    private long getTime() {
        long time = 0;
        time = System.currentTimeMillis();
        return time;
    }

    public static short[] bytesToShorts(byte byteBuffer[]) {
        int len = byteBuffer.length / 2;
        short[] output = new short[len];
        int j = 0;

        for (int i = 0; i < len; i++) {
            output[i] = (short) (byteBuffer[j++] << 8);
            output[i] |= (byteBuffer[j++] & 0xff);
        }
        return output;
    }

    float getGain() {
        return _gain;
    }
}
