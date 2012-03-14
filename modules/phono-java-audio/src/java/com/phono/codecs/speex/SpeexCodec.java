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

package com.phono.codecs.speex;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;
import com.phono.srtplight.Log;
import java.io.StreamCorruptedException;
import org.xiph.speex.SpeexDecoder;
import org.xiph.speex.SpeexEncoder;

public class SpeexCodec implements CodecFace, EncoderFace, DecoderFace {

    SpeexEncoder _spxe;
    SpeexDecoder _spxd;
    int _sampleRate;
    long _iaxcn;
    String _name;
    int _aframesz;
    int _speexmode;

    public SpeexCodec(boolean wide) {
        _speexmode = wide ? 1:0;
        _sampleRate = wide ? 16000 : 8000;
        _iaxcn = wide ? CodecFace.AUDIO_SPEEX16 : CodecFace.AUDIO_SPEEX;
        _name = wide ? "SPEEX" : "SPEEX";
        _spxe = new SpeexEncoder();
        int q = 9;
        _spxe.init(_speexmode, q, _sampleRate, 1);// _mode, _quality, _sampleRate, _channels);
        _spxe.getEncoder().setComplexity(5);
        Log.debug("Speex mode "+ _spxe.getEncoder().getMode()
                +" encoder configured with rate ="+_spxe.getEncoder().getSamplingRate()
                +" Quality ="+q
                +" compexity = "+_spxe.getEncoder().getComplexity()
                +" packet size = "+_spxe.getEncoder().getEncodedFrameSize()/8
                +" bitrate = "+_spxe.getEncoder().getBitRate()/1024.0);
        _spxd = new SpeexDecoder();
        _spxd.init(_speexmode, _sampleRate, 1, false);// _mode, _sampleRate, _channels, false);
        _aframesz = wide ? 320 : 160; // number of shorts in an audio frame;
    }


    public int getFrameSize() {
        return -1; // we don't know - it is officailly a vbr codec.
    }

    public int getFrameInterval() {
        return 20;
    }

    public long getCodec() {
        return _iaxcn;
    }

    public DecoderFace getDecoder() {
        return this;
    }

    public EncoderFace getEncoder() {
        return this;
    }

    public String getName() {
        return _name;
    }

    public float getSampleRate() {
        return _sampleRate;
    }

    public byte[] encode_frame(short[] audio) {
        _spxe.processData(audio, 0, audio.length);
        int sz = _spxe.getProcessedDataByteSize();
        byte[] wireOut = new byte[sz];
        int got = _spxe.getProcessedData(wireOut, 0);
        return wireOut;
    }

    public short[] decode_frame(byte[] bytes) {
        try {
            _spxd.processData(bytes, 0, bytes.length);
        } catch (StreamCorruptedException ex) {
            Log.error("Speex Decoder error " + ex.getMessage());
        }
        short audioOut[] = new short[_aframesz];
        int decsize = _spxd.getProcessedData(audioOut, 0);
        return audioOut;
    }

    public byte[] lost_frame(byte[] bytes, byte[] bytes1) {
        // only gives us a _decoded_ frame - we don;t know what to do with that....
        // todo....
        return null;
    }
}
