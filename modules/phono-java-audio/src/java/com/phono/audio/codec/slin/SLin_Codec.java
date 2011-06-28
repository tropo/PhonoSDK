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

package com.phono.audio.codec.slin;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.CodecUtil;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;

/**
 * SLin_Codec
 *
 * 16-bit linear little-endian
 * 2 bytes per sample
 *
 */
public class SLin_Codec implements CodecFace, DecoderFace, EncoderFace {
    
    /**
     * 
     * Creates a new instance of SLin_Codec. Make sure you do all necessary 
     * initialisations,
     */
    public SLin_Codec() {
    }

    /**
     * Returns the optimum size (in bytes) of a frame. 
     * This is 320 for SLinear.
     */
    public int getFrameSize() {
        return 320;
    }
    
    public int getFrameInterval() {
        return 20;
    }
    

    public long getCodec() {
        return CodecFace.AUDIO_SLINEAR;
    }
    
    /**
     * Returns an instance of the Decoder class of this Codec.
     * #see SLin_Decoder
     */
    public DecoderFace getDecoder(){
        return this;
    }
    
    /**
     * Returns an instance of the Encoder class of this Codec.
     * #see SLin_Encoder
     */
    public EncoderFace getEncoder() {
        return this;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.getClass().getName()).append(": ");
        buf.append(" codec=").append(getCodec());
        buf.append(", framesize=").append(getFrameSize());
        buf.append(", frameinterval=").append(getFrameInterval());
        return buf.toString();
    }

    public short[] decode_frame(byte encoded_signal[]) {
        short[] output = CodecUtil.bytesToShorts(encoded_signal); 
        return output;
    }

    public byte[] encode_frame(short original_signal[]) {
        byte[] output = CodecUtil.shortsToBytes(original_signal);
        return output;
    }
    public float getSampleRate() {
        return 8000.0F;
    }

    public String getName() {
        return "SLIN";
    }

    public byte[] lost_frame(byte current_frame[], byte next_frame[]) {
        return current_frame;
    }
}
