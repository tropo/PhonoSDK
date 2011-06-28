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

package com.phono.audio.codec.gsm;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;

/**
 * GSM_Codec
 *
 */
public class GSM_Codec implements CodecFace {
    
    private GSM_Decoder _decoder;
    private GSM_Encoder _encoder;
    
    /** 
     * Creates a new instance of GSM_Codec. Make sure you do all necessary 
     * initialisations,
     */
    public GSM_Codec() {
        _decoder = new GSM_Decoder();
        _encoder = new GSM_Encoder();
    }

    public float getSampleRate() {
        return 8000.0F;
    }
    /**
     * Returns the optimum size (bytes) of a frame. 
     * This is 33 for GSM.
     */
    public int getFrameSize() {
        return 33;
    }
    
    /**
     * Returns the frame interval in ms.
     * GSM needs 160 samples per frame.
     * At 8kHz, that's 50 frames per seconds (8000 / 160 = 50).
     * 50 frames per second, makes 20 ms (1/50 = 0.02 sec = 20 ms).
     */
    public int getFrameInterval() {
        return 20;
    }
    
    /**
     * Returns "Media Format Subclass Values" of this Codec. This should be one 
     * of the constants above.
     */
    public long getCodec() {
        return CodecFace.AUDIO_GSM;
    }
    
    /**
     * Returns an instance of the Decoder class of this Codec.
     * #see GSM_Decoder
     */
    public DecoderFace getDecoder(){
        return _decoder;
    }
    
    /**
     * Returns an instance of the Encoder class of this Codec.
     * #see GSM_Encoder
     */
    public EncoderFace getEncoder() {
        return _encoder;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.getClass().getName()).append(": ");
        buf.append(" codec=").append(getCodec());
        buf.append(", framesize=").append(getFrameSize());
        buf.append(", frameinterval=").append(getFrameInterval());
        return buf.toString();
    }

    public String getName() {
        return "GSM";
    }

}
