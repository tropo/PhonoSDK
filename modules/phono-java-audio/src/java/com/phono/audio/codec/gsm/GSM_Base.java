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
import java.io.*;

/**
 * This class implements some of the basic arithmetic for encoding and
 * decoding gsm.<br/>
 * Chapter 5.2 and 5.3 of <br/>
 * <br/>
 * ETSI EN 300 961 V8.1.1 (2000-11)<br/>
 * European Standard (Telecommunications series)<br/>
 * Digital cellular telecommunications system (Phase 2+);<br/>
 * Full rate speech;<br/>
 * Transcoding<br/>
 * (GSM 06.10 version 8.1.1 Release 1999)<br/>
 *
 */
public class GSM_Base implements GSM_Constant {

    public final Arithmetic AR = new Arithmetic();
    
    /** LARpp_j from previous frame */
    protected short _LARpp_j_1[];
    
    /**
     * Keep _LAR, so main can use them
     */
    protected short _LAR[];
    /**
     * Keep the segment _params, so main can use them
     */
    protected SegmentParam _params[];
    
    public GSM_Base() {
        _LARpp_j_1 = new short[9];
    }
    
    
    /**
     * Procedure 5.2.15 APCM quantization of the selected RPE sequence<br/>
     *
     * This is cut in three, to facilitate decoding:
     * - apcmQuantizationRPESequence (short xM[], SegmentParam param)
     * - getExponentMantissa(SegmentParam param)
     * - computeXMC(short xM[], SegmentParam param)
     */
    // 4x per frame
    protected void getExponentMantissa(SegmentParam param) {
        short var1, var2, itest;
        /*
         * Quantizing and coding of the xM[0..12] RPE sequence to get
         * the _xMc[0..12].
         * This computation uses the fact that the decoded version of
         * _xmaxc can be calculated by using the exponent and the
         * mantissa part of _xmaxc (logarithmic table).
         *
         * So, this method avoids any division and uses only a scaling
         * of the RPE samples by a function of the exponent. A direct
         * multiplication by the inverse of the mantissa (NRFAC[0..7]
         * found in table 5.5) gives the 3 bit coded version _xMc[0..12]
         * of the RPE samples.
         */
        
        // Compute exponent and mantissa of the decoded version of _xmaxc.
        param._exp = 0;
        if (param._xmaxc > 15) {
            var1 = AR.toShort(param._xmaxc >> 3);
            param._exp = AR.s_sub(var1, (short)1);
        }
        var2 = AR.toShort(param._exp << 3);
        param._mant = AR.s_sub(param._xmaxc, var2);
        
        // Normalize mantissa 0 <= param._mant <= 7.
        if (param._mant == 0) {
            param._exp = -4;
            param._mant = 15;
        } else {
            // TODO: itest seems like boolean flag?
            // change to while loop?
            itest = 0;
            for (int i=0; i<3; i++) {
                if (param._mant > 7) {
                    itest = 1;
                }
                if (itest == 0) {
                    var1 = AR.toShort(param._mant << 1);
                    param._mant = AR.s_add(var1, (short)1);
                /*
                }
                if (itest == 0)
                {
                 */
                    param._exp = AR.s_sub(param._exp, (short)1);
                }
            }
        }
        param._mant = AR.s_sub(param._mant, (short)8);
    }
    
    /**
     * Procedure 5.2.16 APCM inverse quantization
     * This part is for decoding the RPE sequence of coded _xMc[0..12]
     * samples to obtain the xMp[0..12] array. Table 5.6 is used to get
     * the mantissa of _xmaxc (FAC[0..7]).
     */
    // 4x per frame
    protected short[] apcmInverseQuantization(SegmentParam param) {
        short xMp[] = new short[13];
        short temp, temp1, temp2, temp3, var1;
        
        // See 5.2.15 for _mant
        temp1 = FAC[param._mant];
        // See 5.2.15 for _exp
        temp2 = AR.s_sub((short)6, param._exp);
        temp3 = AR.toShort(1 << AR.s_sub(temp2, (short)1));
        for (int i=0; i<13; i++) {
            // This subtraction is used to restore the sign of _xMc[i].
            var1 = AR.toShort(param._xMc[i] << 1);
            temp = AR.s_sub(var1, (short)7);
            temp = AR.toShort(temp << 12);
            temp = AR.s_mult_r(temp1, temp);
            temp = AR.s_add(temp, temp3);
            xMp[i] = AR.toShort(temp >> temp2);
        }
        return xMp;
    }
    
    /**
     * Procedure 5.2.17 RPE grid positioning<br/>
     * This procedure computes the reconstructed long term residual
     * signal ep[0..39] for the LTP analysis filter. The inputs are the
     * _Mc which is the grid position selection and the xMp[0..12]
     * decoded RPE samples which are upsampled by a factor of 3 by
     * inserting zero values.
     */
    // 4x per frame
    protected short[] rpeGridPositioning(short xMp[], SegmentParam param) {
        short ep[] = new short[SEGMENT];
        /*
        // No need, new short[] inits all to zero
        for (int k=0; k<SEGMENT; k++)
        {
            ep[k] = 0;
        }
         */
        for (int i=0; i<13; i++) {
            ep[param._Mc + (3*i)] = xMp[i];
        }
        return ep;
    }
    
    /**
     * Procedure 5.2.8 Decoding of the coded Log.-Area Ratios<br/>
     */
    /*
     * This procedure requires for efficient implementation two tables.
     * INVA[1..8]=integer((32768*8)/(real_A[1..8]); 8 values (table 5.2)
     * MIC[1..8]=minimum value of the _LAR[1..8]; 8 values (table 5.1)
     */
    protected short[] decodeLAR(short LARc[], short LARpp[]) {
        // Compute the LARpp[1..8].
        short temp1, temp2;
        for (int i=1; i<9; i++) {
            // The addition of MIC[i] is used to restore the sign of
            // _LAR[i].
            temp1 = AR.toShort(AR.s_add(LARc[i], MIC[i]) << 10);
            temp2 = AR.toShort(B[i] << 1);
            temp1 = AR.s_sub(temp1, temp2);
            temp1 = AR.s_mult_r(INVA[i], temp1);
            LARpp[i] = AR.s_add(temp1, temp1);
        }
        
        return LARpp;
    }
    
    /**
     * Procedure 5.2.9 Computation of the quantized reflection coefficients<br/>
     *
     * Within each frame of 160 analysed speech samples the short term
     * analysis and synthesis filters operate with four different sets
     * of coefficients, derived from the previous set of decoded
     * LARs(LARpp(j-1)) and the actual set of decoded LARs (LARpp(j)).
     * <br/>
     * <br/>
     * Procedure 5.2.9.1 Interpolation of the LARpp[1..8] to get the
     * LARp[1..8]<br/>
     * <br/>
     */
    
    /*
     * Procedure 5.2.9 Computation of the quantized reflection coefficients<br/>
     *
     * Within each frame of 160 analysed speech samples the short term
     * analysis and synthesis filters operate with four different sets
     * of coefficients, derived from the previous set of decoded
     * LARs(LARpp(j-1)) and the actual set of decoded LARs (LARpp(j)).
     */
    
    /**
     * Procedure 5.2.9.1 Interpolation of the LARpp[1..8] to get the
     * LARp[1..8]<br/>
     * For k_start = 0 to k_end &lt; 13 <br/>
     * Initial value: LARpp(j-1)[1..8]=0;<br/>
     */
    protected short[] interpolationLARpp_1(short LARpp_j_1[],
            short LARpp_j[], short LARp[]) {
        short var1, var2;
        for (int i=1; i<9; i++) {
            var1 = AR.toShort(LARpp_j_1[i] >> 2);
            var2 = AR.toShort(LARpp_j[i] >> 2);
            LARp[i] = AR.s_add(var1, var2);
            
            var2 = AR.toShort(LARpp_j_1[i] >> 1);
            LARp[i] = AR.s_add(LARp[i], var2);
        }
        return LARp;
    }
    
    
    /**
     * Procedure 5.2.9.1 Interpolation of the LARpp[1..8] to get the
     * LARp[1..8]<br/>
     * For k_start = 13 to k_end &lt; 27<br/>
     */
    protected short[] interpolationLARpp_2(short LARpp_j_1[],
            short LARpp_j[], short LARp[]) {
        short var1, var2;
        for (int i=1; i<9; i++) {
            var1 = AR.toShort(LARpp_j_1[i] >> 1);
            var2 = AR.toShort(LARpp_j[i] >> 1);
            LARp[i] = AR.s_add(var1, var2);
        }
        return LARp;
    }
    
    /**
     * Procedure 5.2.9.1 Interpolation of the LARpp[1..8] to get the
     * LARp[1..8]<br/>
     * For k_start = 27 to k_end &lt; 40 <br/>
     */
    protected short[] interpolationLARpp_3(short LARpp_j_1[],
            short LARpp_j[], short LARp[]) {
        short var1, var2;
        for (int i=1; i<9; i++) {
            var1 = AR.toShort(LARpp_j_1[i] >> 2);
            var2 = AR.toShort(LARpp_j[i] >> 2);
            LARp[i] = AR.s_add(var1, var2);
            
            var1 = LARp[i];
            var2 = AR.toShort(LARpp_j[i] >> 1);
            LARp[i] = AR.s_add(var1, var2);
        }
        return LARp;
    }
    
    /**
     * Procedure 5.2.9.1 Interpolation of the LARpp[1..8] to get the
     * LARp[1..8]<br/>
     * For k_start = 40 to k_end &lt; 160 <br/>
     */
    protected short[] interpolationLARpp_4(short LARpp_j_1[],
            short LARpp_j[], short LARp[]) {
        // TODO:
        // use memcopy?
        for (int i=1; i<9; i++) {
            LARp[i] = LARpp_j[i];
        }
        return LARp;
    }
    
    
    /**
     * Procedure 5.2.9.2 Computation of the rp[1..8] from the interpolated
     * LARp[1..8]<br/>
     * The input of this procedure is the interpolated LARp[1..8] array.
     * The reflection coefficients, rp[i], are used in the analysis
     * filter and in the synthesis filter.
     */
    protected short[] computeRP(short LARp[], short rp[]) {
        short temp, var1;
        for (int i=1; i<9; i++) {
            temp = AR.s_abs(LARp[i]);
            if (temp < 11059) {
                temp = AR.toShort(temp << 1);
            } else if (temp < 20070) {
                temp = AR.s_add(temp, (short)11059);
            } else {
                var1 = AR.toShort(temp >> 2);
                temp = AR.s_add(var1, (short)26112);
            }
            rp[i] = temp;
            if (LARp[i] < 0) {
                rp[i] = AR.s_sub((short)0, rp[i]);
            }
        }
        return rp;
    }
    
    
    // The test files that come with the pdf are in LITTLE_ENDIAN
    public final static boolean LITTLE_ENDIAN = true;
    
    public short readShort(DataInputStream in)
    throws java.io.IOException {
        short s;
        if (LITTLE_ENDIAN) {
            s = readShortLittleEndian(in);
        } else {
            s = in.readShort();
        }
        return s;
    }
    
    public short readShortLittleEndian(DataInputStream in)
    throws java.io.IOException {
        // 2 bytes
        int low  = in.readByte() & 0xFF;
        int high = in.readByte() & 0xFF;
        short s = (short)(high << 8 | low);
        return s;
    }
    
    public void writeShort(DataOutputStream out, short value)
    throws java.io.IOException {
        if (LITTLE_ENDIAN) {
            writeShortLittleEndian(out, value);
        } else {
            out.writeShort(value);
        }
    }
    
    public void writeShortLittleEndian(DataOutputStream out, short value)
    throws java.io.IOException {
        // 2 bytes
        byte low = (byte) (value & 0xFF);
        byte high = (byte) ((value >> 8) & 0xFF);
        byte bytes[] = {low, high};
        
        out.write(bytes, 0, bytes.length);
    }
    
    /**
     * Set bit number (bitno) to one. The bit numbering pretends the
     * byte array is one very long word.
     *
     * @param output The output array to set the bit
     * @param bitno The position of the bit (in output)
     */
    public static void setBit(byte output[], int bitno)
    throws java.lang.ArrayIndexOutOfBoundsException {
        // bit 0 is on the left hand side, if bit 0 should be set to '1'
        // this would show as: 1000 0000 = 0x80
        
        // each byte is 8 bits
        int index = bitno / 8;
        int index_bitno = bitno % 8;
        
        // shift the '1' into the right place
        // shift with zero extension
        byte mask = (byte) (0x80 >>> index_bitno);
        
        // OR the bit into the byte, so the other bits remain
        // undisturbed.
        output[index] |= mask;
    }
    
    /**
     * Copies a number of bits from input to output.
     * Copy bits from left to right (MSB - LSB).
     *
     * @param input The input value to read from
     * @param in_noLSB The number of LSB in input to copy
     * @param output The output array to copy the bits to
     * @param out_pos The start position in output
     * @return the updated out_pos
     */
    public static int copyBits(int input, int in_noLSB, byte output[],
            int out_pos) {
        int res;
        int value = input;
        
        // start with the left most bit I've got to copy over:
        int mask = 0x1 << (in_noLSB -1);
        
        for (int i=0; i<in_noLSB; i++) {
            // see if the that bit is one or zero
            res = (value & mask);
            if (res > 0) {
                setBit(output, out_pos);
            }
            
            // shift the mask to the next position
            // shift with zero extension
            mask = mask >>> 1;
            out_pos++;
        }
        return out_pos;
    }
    
    
    
    /**
     * Returns zero or one.
     *
     * @param input The input array to read from
     * @param bitno The position of the bit (in input)
     * @return one or zero
     */
    public static int getBit(byte input[], int bitno)
    throws java.lang.ArrayIndexOutOfBoundsException {
        // bit 0 is on the left hand side, if bit 0 should be set to '1'
        // this would show as: 1000 0000 = 0x80
        
        // each byte is 8 bits
        int index = bitno / 8;
        int index_bitno = bitno % 8;
        
        byte onebyte = input[index];
        
        // shift the '1' into the right place
        // shift with zero extension
        byte mask = (byte) (0x80 >>> index_bitno);
        
        // mask (AND) it so see if the bit is one or zero
        int res = (onebyte & mask);
        if (res < 0) {
            // it can be negative when testing the signed bit (bit zero
            // in this case)
            res = 1;
        }
        return res;
    }
    
    
    /**
     * Copy a number of bits from input into a short.
     * Copy bits from left to right (MSB - LSB).
     *
     * @param input The input array to read from
     * @param in_pos The position of the bit (in input) to start from
     * @param no_bits The number of bits to copy
     * @return The new value as a short
     */
    public static short copyBits(byte input[], int in_pos,
            int no_bits) {
        // LSB is on the right hand side
        short out_value = 0;
        
        // start with the left most bit I've got to copy into:
        int out_value_mask = 0x1 << (no_bits -1);
        
        int myBit;
        for (int b=0; b<no_bits; b++) {
            myBit = getBit(input, in_pos);
            if (myBit > 0) {
                // OR the bit into place, so the other bits remain
                // undisturbed.
                out_value |= out_value_mask;
            }
            
            // move to the next bit of input
            in_pos++;
            
            // get ready for the next bit of output
            // shift with zero extension
            out_value_mask = (short) (out_value_mask >>> 1);
        }
        return out_value;
    }
    
    public void writeCodedParams(DataOutputStream out)
    throws java.io.IOException {
        /*
         * LSB  Variable      Frame
         * 6    _LAR[1]        1
         * 6    _LAR[2]        2
         * 5    _LAR[3]        3
         * 5    _LAR[4]        4
         * 4    _LAR[5]        5
         * 4    _LAR[6]        6
         * 3    _LAR[7]        7
         * 3    _LAR[8]        8
         */
        
        /*
         * 4x:
         * LSB  Variable      Seg1    Seg2
         * 7    _Nc             9      26
         * 2    _bc            10      27
         * 2    _Mc            11      28
         * 6    _xmaxc         12      29
         * 3    _xMc[0..12]    13-25   30-42
         */
        // NOT GSM_HEADER!!!
        
        for (int i=1; i<9; i++) {
            writeShort(out, _LAR[i]);
        }
        
        for (int s=0; s<4; s++) {
            SegmentParam para = _params[s];
            writeShort(out, para._Nc);
            writeShort(out, para._bc);
            writeShort(out, para._Mc);
            writeShort(out, para._xmaxc);
            for (int i=0; i<para._xMc.length; i++) {
                writeShort(out, para._xMc[i]);
            }
        }
    }
    
    public void readCodedParams(DataInputStream in)
    throws java.io.IOException {
        /*
         * LSB  Variable      Frame
         * 6    _LAR[1]        1
         * 6    _LAR[2]        2
         * 5    _LAR[3]        3
         * 5    _LAR[4]        4
         * 4    _LAR[5]        5
         * 4    _LAR[6]        6
         * 3    _LAR[7]        7
         * 3    _LAR[8]        8
         */
        
        /*
         * 4x:
         * LSB  Variable      Seg1    Seg2
         * 7    _Nc             9      26
         * 2    _bc            10      27
         * 2    _Mc            11      28
         * 6    _xmaxc         12      29
         * 3    _xMc[0..12]    13-25   30-42
         */
        // NOT GSM_HEADER!!!
        
        _LAR = new short[9];
        for (int i=1; i<9; i++) {
            _LAR[i] = readShort(in);
        }
        
        _params = new SegmentParam[4];
        for (int s=0; s<4; s++) {
            SegmentParam para = new SegmentParam();
            para._Nc = readShort(in);
            para._bc = readShort(in);
            para._Mc = readShort(in);
            para._xmaxc = readShort(in);
            para._xMc = new short[13];
            for (int i=0; i<para._xMc.length; i++) {
                para._xMc[i] = readShort(in);
            }
            
            _params[s] = para;
        }
    }
    
    
}
