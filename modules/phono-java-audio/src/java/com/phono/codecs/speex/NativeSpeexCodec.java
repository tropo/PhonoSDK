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

import org.xiph.speex.SpeexDecoder;
import org.xiph.speex.SpeexEncoder;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;
import com.phono.srtplight.Log;

/**
 *
 * @author tim
 */
public class NativeSpeexCodec implements CodecFace, EncoderFace, DecoderFace {

    int _sampleRate;
    short[] _adataOut;
    byte[] _wireOut;
    byte[] _codec;
    long _iaxcn;
    String _name;
    int _aframesz;
    int _speexmode;

    private native byte[] initCodec(int mode, int q, int c, int sampleRate);

    private native byte[] speexEncode(byte[] codec, short[] a);

    private native void speexDecode(byte[] codec, byte[] w, short[] a);

    public native void freeCodec(byte[] codec);

    static {
        System.loadLibrary("phono-speex");
    }

    protected  int getCompexity(boolean wide){
        return wide? 1:0 ;
    }

    protected  int getQuality(boolean wide){
        return wide ? 5:3;
    }

    public NativeSpeexCodec(boolean wide) {
        _speexmode = wide ? 1:0;
        _sampleRate = wide ? 16000 : 8000;
        _iaxcn = wide ? CodecFace.AUDIO_SPEEX16 : CodecFace.AUDIO_SPEEX;
        _name = wide ? "SPEEX" : "SPEEX";
        int q = getQuality(wide) ;
        int c = getCompexity(wide);
        _aframesz = wide ? 320 : 160; // number of shorts in an audio frame;

        _adataOut = new short[_aframesz];
        _codec = initCodec(_speexmode,q,c,_sampleRate);
        Log.debug("Native speex codec "+(wide?"wide":"narrow")+"band rate="+_sampleRate+" q="+q+" c="+c);
    }

    public int getFrameSize() {
        return -1;
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

    public float getSampleRate() {
        return _sampleRate;
    }

    public String getName() {
        return _name;
    }


    public byte[] encode_frame(short[] audio) {
        return speexEncode(_codec, audio);
    }

    public short[] decode_frame(byte[] bytes) {
        speexDecode(_codec, bytes, _adataOut);
        return _adataOut;
    }

    public byte[] lost_frame(byte[] current_frame, byte[] next_frame) {
        return current_frame;
    }

}
