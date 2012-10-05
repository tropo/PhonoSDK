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

/**
 *
 * @author tim
 */
public class NativeSpeexCodec implements CodecFace, EncoderFace, DecoderFace {

    int _sampleRate;
    short[] _adataOut;
    byte[] _wireOut;
    byte[] _codec;

    private native byte[] initCodec();

    private native byte[] speexEncode(byte[] codec, short[] a);

    private native void speexDecode(byte[] codec, byte[] w, short[] a);

    public native void freeCodec(byte[] codec);

    static {
        System.loadLibrary("phono-speexw");
    }

    public NativeSpeexCodec() {

        _adataOut = new short[320];
        _codec = initCodec();
    }

    public int getFrameSize() {
        return -1;
    }

    public int getFrameInterval() {
        return 20;
    }

    public long getCodec() {
        return SpeexCodec.AUDIO_SPEEX16;
    }

    public DecoderFace getDecoder() {
        return this;
    }

    public EncoderFace getEncoder() {
        return this;
    }

    public float getSampleRate() {
        return 16000.0F;
    }

    public String getName() {
        return "SPEEX";
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
