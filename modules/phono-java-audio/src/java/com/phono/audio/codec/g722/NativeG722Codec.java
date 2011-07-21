/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.phono.audio.codec.g722;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;

/**
 *
 * @author tim
 */
public class NativeG722Codec implements CodecFace, EncoderFace, DecoderFace {

    int _sampleRate;
    short[] _adataOut;
    byte[] _wireOut;
    byte[] _codec;
    byte[] _outW;

    private native byte[] initCodec();

    private native void g722Encode(byte[] codec, short[] a, byte[] w);

    private native void g722Decode(byte[] codec, byte[] w, short[] a);

    public native void freeCodec(byte[] codec);

    static {
        System.loadLibrary("g722");
    }

    public NativeG722Codec() {

        _adataOut = new short[320];
        _wireOut = new byte[160]; // that _has to be big enough
        _codec = initCodec();
    }

    public int getFrameSize() {
        return 160;
    }

    public int getFrameInterval() {
        return 20;
    }

    public long getCodec() {
        return G722Codec.AUDIO_G722;
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
        return "G.722";
    }


    @Override
    public byte[] encode_frame(short[] audio) {
        g722Encode(_codec, audio, _wireOut);
        return _wireOut;
    }

    @Override
    public short[] decode_frame(byte[] bytes) {
        g722Decode(_codec, bytes, _adataOut);
        return _adataOut;
    }

    public byte[] lost_frame(byte[] current_frame, byte[] next_frame) {
        return current_frame;
    }

}
