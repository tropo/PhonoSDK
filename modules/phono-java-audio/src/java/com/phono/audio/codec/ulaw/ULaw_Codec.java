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

package com.phono.audio.codec.ulaw;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;

/**
 * ULaw_Codec
 *
 * G.711 mu-law
 * 1 byte per sample
 *
 * Thanks for Marc Sweetgall. We copied this code from:
 * http://www.codeproject.com/KB/security/g711audio.aspx
 *
 */
public class ULaw_Codec implements CodecFace, DecoderFace, EncoderFace {
    
    public final static int BIAS = 0x84; //132, or 1000 0100
    
    public final static int MAX = 32635; //32767 (max 15-bit integer) minus BIAS
    
    /**
     *
     * Creates a new instance of ULaw_Codec. Make sure you do all necessary
     * initialisations,
     */
    public ULaw_Codec() {
    }
    
    /**
     * Returns the optimum size (in bytes) of a frame.
     * This is 320 for SLinear.
     */
    public int getFrameSize() {
        return 160;
    }
    
    public int getFrameInterval() {
        return 20;
    }
    
    public float getSampleRate() {
        return 8000.0F;
    }
    
    public long getCodec() {
        return CodecFace.AUDIO_ULAW;
    }
    
    /**
     * Returns an instance of the Decoder class of this Codec.
     * #see ULaw_Decoder
     */
    public DecoderFace getDecoder(){
        return this;
    }
    
    /**
     * Returns an instance of the Encoder class of this Codec.
     * #see ULaw_Encoder
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
        int len = encoded_signal.length;
        short[] output = new short[len];
        for (int i = 0; i < len; i++) {
            output[i] = decode(encoded_signal[i]);
        }        
        return output;
    }
    
    public byte[] encode_frame(short original_signal[]) {
        int len = original_signal.length;
        byte[] output = new byte[len];
        for (int i = 0; i < len; i++) {
            output[i] = encode(original_signal[i]);
        }        
        return output;
    }
    
    
    private byte encode(short pcm) {
        //Get the sign bit. Shift it for later
        //use without further modification
        boolean sign = (pcm < 0);
        
        //If the number is negative, make it
        //positive (now it's a magnitude)
        if (sign) {
            pcm = (short) -pcm;
        }
        
        //The magnitude must be less than 32635 to avoid overflow
        if (pcm > MAX) {
            pcm = MAX;
        }
        
        //Add 132 to guarantee a 1 in
        //the eight bits after the sign bit
        pcm += BIAS;
        
        /* Finding the "exponent"
         * Bits:
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * S 7 6 5 4 3 2 1 0 . . . . . . .
         * We want to find where the first 1 after the sign bit is.
         * We take the corresponding value from
         * the second row as the exponent value.
         * (i.e. if first 1 at position 7 -> exponent = 2) 
         */
        int exponent = 7;
        
        //Move to the right and decrement exponent until we hit the 1
        for (int expMask = 0x4000; (pcm & expMask) == 0; exponent--, expMask >>= 1) { }
        
        /* The last part - the "mantissa"
         * We need to take the four bits after the 1 we just found.
         * To get it, we shift 0x0f :
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * S 0 0 0 0 0 1 . . . . . . . . . (meaning exponent is 2)
         * . . . . . . . . . . . . 1 1 1 1
         * We shift it 5 times for an exponent of two, meaning
         * we will shift our four bits (exponent + 3) bits.
         * For convenience, we will actually just shift
         * the number, then and with 0x0f.
         */
        int mantissa = (pcm >> (exponent + 3)) & 0x0f;
        
        //The mu-law byte bit arrangement
        //is SEEEMMMM (Sign, Exponent, and Mantissa.)
        byte mulaw = (byte)(exponent << 4 | mantissa);
        if (sign) {
            mulaw = (byte) (mulaw | 0x80);
        }
        
        //Last is to flip the bits
        return (byte)~mulaw;
    }
    
    
    private short decode(byte mulaw) {
        //Flip all the bits
        mulaw = (byte)~mulaw;
        
        //Pull out the value of the sign bi
        int sign = mulaw & 0x80;
        
        //Pull out and shift over the value of the exponent
        int exponent = (mulaw & 0x70) >> 4;
        
        //Pull out the four bits of data
        int data = mulaw & 0x0f;
        
        //Add on the implicit fifth bit (we know
        //the four data bits followed a one bit)
        data |= 0x10;
        
        /* Add a 1 to the end of the data by
         * shifting over and adding one. Why?
         * Mu-law is not a one-to-one function.
         * There is a range of values that all
         * map to the same mu-law byte.
         * Adding a one to the end essentially adds a
         * "half byte", which means that
         * the decoding will return the value in the
         * middle of that range. Otherwise, the mu-law
         * decoding would always be
         * less than the original data. */
        data <<= 1;
        data += 1;
        
        /* Shift the five bits to where they need
         * to be: left (exponent + 2) places
         * Why (exponent + 2) ?
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * . 7 6 5 4 3 2 1 0 . . . . . . . <-- starting bit (based on exponent)
         * . . . . . . . . . . 1 x x x x 1 <-- our data
         * We need to move the one under the value of the exponent,
         * which means it must move (exponent + 2) times
         */
        data <<= exponent + 2;
        
        //Remember, we added to the original,
        //so we need to subtract from the final
        data -= BIAS;
        
        //If the sign bit is 0, the number
        //is positive. Otherwise, negative.
        return (short)(sign == 0 ? data : -data);
    }

    public String getName() {
        return "ULaw";
    }

    public byte[] lost_frame(byte current_frame[], byte next_frame[]) {
        return current_frame;
    }
    
}
