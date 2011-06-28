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

package com.phono.audio.codec.alaw;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;

/**
 * ALaw_Codec
 * 
 * G.711 a-law
 * 1 byte per sample
 * 
 * Thanks for Marc Sweetgall. We adapted this code from:
 * http://www.codeproject.com/KB/security/g711audio.aspx
 * 
 */
public class ALaw_Codec implements CodecFace, DecoderFace, EncoderFace {

    /**
     * 
     * Creates a new instance of ALaw_Codec. Make sure you do all necessary
     * initialisations,
     */
    public ALaw_Codec() {
    }

    public float getSampleRate() {
        return 8000.0F;
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

    public long getCodec() {
        return CodecFace.AUDIO_ALAW;
    }

    /**
     * Returns an instance of the Decoder class of this Codec.
     * #see ULaw_Decoder
     */
    public DecoderFace getDecoder() {
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

        //Get the sign bit. Shift it for later use 
        //without further modification
        boolean sign = (pcm < 0);
        
        //If the number is negative, 
        //make it positive (now it's a magnitude)
        if (sign) {
            pcm = (short) -pcm;
        }
        
        //The magnitude must fit in 15 bits to avoid overflow

        /* Finding the "exponent"
         * Bits:
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * S 7 6 5 4 3 2 1 0 0 0 0 0 0 0 0
         * We want to find where the first 1 after the sign bit is.
         * We take the corresponding value 
         * from the second row as the exponent value.
         * (i.e. if first 1 at position 7 -> exponent = 2)
         * The exponent is 0 if the 1 is not found in bits 2 through 8.
         * This means the exponent is 0 even if the "first 1" doesn't exist.
         */
        int exponent = 7;
        
        //Move to the right and decrement exponent 
        //until we hit the 1 or the exponent hits 0
        for (int expMask = 0x4000; (pcm & expMask) == 0 && exponent > 0; exponent--, expMask >>= 1) {
        }

        /* The last part - the "mantissa"
         * We need to take the four bits after the 1 we just found.
         * To get it, we shift 0x0f :
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * S 0 0 0 0 0 1 . . . . . . . . . (say that exponent is 2)
         * . . . . . . . . . . . . 1 1 1 1
         * We shift it 5 times for an exponent of two, meaning
         * we will shift our four bits (exponent + 3) bits.
         * For convenience, we will actually just
         * shift the number, then AND with 0x0f. 
         * 
         * NOTE: If the exponent is 0:
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * S 0 0 0 0 0 0 0 Z Y X W V U T S (we know nothing about bit 9)
         * . . . . . . . . . . . . 1 1 1 1
         * We want to get ZYXW, which means a shift of 4 instead of 3
         */
        int mantissa = (pcm >> ((exponent == 0) ? 4 : (exponent + 3))) & 0x0f;

        //The a-law byte bit arrangement is SEEEMMMM 
        //(Sign, Exponent, and Mantissa.)
        byte alaw = (byte) (exponent << 4 | mantissa | (sign ? 0x80 : 0));

        //Last is to flip every other bit, and the sign bit (0xD5 = 1101 0101)
        return (byte) (alaw ^ 0xD5);
    }

    private short decode(byte alaw) {
        //Invert every other bit, 
        //and the sign bit (0xD5 = 1101 0101)
        alaw ^= 0xD5;

        //Pull out the value of the sign bit
        int sign = alaw & 0x80;
        
        //Pull out and shift over the value of the exponent
        int exponent = (alaw & 0x70) >> 4;
        
        //Pull out the four bits of data
        int data = alaw & 0x0f;

        //Shift the data four bits to the left
        data <<= 4;
        
        //Add 8 to put the result in the middle 
        //of the range (like adding a half)
        data += 8;

        //If the exponent is not 0, then we know the four bits followed a 1,
        //and can thus add this implicit 1 with 0x100.
        if (exponent != 0) {
            data += 0x100;
        }
        
        /* Shift the bits to where they need to be: left (exponent - 1) places
         * Why (exponent - 1) ?
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * . 7 6 5 4 3 2 1 . . . . . . . . <-- starting bit (based on exponent)
         * . . . . . . . Z x x x x 1 0 0 0 <-- our data (Z is 0 only when 
         * exponent is 0)
         * We need to move the one under the value of the exponent,
         * which means it must move (exponent - 1) times
         * It also means shifting is unnecessary if exponent is 0 or 1.
         */
        if (exponent > 1) {
            data <<= (exponent - 1);
        }

        return (short) (sign == 0 ? data : -data);
    }

    public String getName() {
        return "Alaw";
    }

    public byte[] lost_frame(byte current_frame[], byte next_frame[]) {
        return current_frame;
    }
}
