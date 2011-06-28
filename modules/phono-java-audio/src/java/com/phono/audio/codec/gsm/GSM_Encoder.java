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

import com.phono.audio.codec.EncoderFace;
import java.io.*;
import java.util.*;

/**
 * This class implements the basic arithmetic for gsm.<br/>
 * Chapter 5.2 Fixed point implementation of the RPE-LTP coder of <br/>
 * <br/>
 * ETSI EN 300 961 V8.1.1 (2000-11)<br/>
 * European Standard (Telecommunications series)<br/>
 * Digital cellular telecommunications system (Phase 2+);<br/>
 * Full rate speech;<br/>
 * Transcoding<br/>
 * (GSM 06.10 version 8.1.1 Release 1999)<br/>
 *
 */
public class GSM_Encoder
        extends GSM_Base implements EncoderFace {
    
    /*
     The RPE-LTP coder works on a frame by frame basis. The length of the
     frame is equal to 160 samples. Some computations are done once per frame
     (analysis) and some others for each of the four sub-segments (40
     samples).
     
     In the following detailed description, procedure 5.2.0 to 5.2.10 are
     done once per frame to produce at the output of the coder the _LAR[1..8]
     parameters which are the coded LAR coefficients and also to realize the
     inverse filtering operation for the entire frame (160 samples of signal
     d[0..159]). These parts produce at the output of the coder:
     
     | _LAR[1..8] : Coded LAR coefficients
     |--> These parameters are calculated and sent once per frame.
     
     Procedure 5.2.11 to 5.2.18 are to be executed four times per frame. That
     means once for each sub-segment RPE-LTP analysis of 40 samples. These
     parts produce at the output of the coder:
     
     | _Nc : LTP lag;
     | _bc : Coded LTP gain;
     | _Mc : RPE grid selection;
     | _xmaxc : Coded maximum amplitude of the RPE sequence;
     | _xMc[0..12] : Codes of the normalized RPE samples;
     |--> These parameters are calculated and sent four times per frame.
     */
    
    /**
     * Used to offset index into _dp[-120 .. -1]
     */
    public final int DP_OFFSET = 120;
    
    /** Keep in memory for the next frame or sub-segment */
    private short _z1;
    private int _i_z2;
    private short _mp;
    private short _u[];
    private short _dp[];
    
    public GSM_Encoder() {
        _LARpp_j_1 = new short[9];
        
        // Keep variables in memory for the next frame.
        _z1 = 0;
        _i_z2 = 0;
        _mp = 0;
        
        // Keep the array _u[0..7] in memory for each call.
        // Initial value: _u[0..7]=0;
        _u = new short[8];
        
        // Keep the array _dp[-120..-1] in memory for the next
        // sub-segment.
        // Initial value: _dp[-120..-1]=0;
        _dp = new short[120];
    }
    
    /**
     * Encodes one GSM frame.
     * Encode GSM<br/>
     * After A or μ--law (PCS 1900) to linear conversion (or directly
     * from the A to D converter) the following scaling is assumed for
     * input to the RPE-LTP algorithm:<br/>
     *
     * <pre>S.v.v.v.v.v.v.v.v.v.v.v.v.x.x.x (2's complement format),
     * where</pre>
     * <ul>
     *   <li>S - the sign bit</li>
     *   <li>v - valid bit</li>
     *   <li>x - "don't care" bit</li>
     * </ul>
     * The original signal is called sop[..];
     *
     * @param sop Array with the original signal
     * @return the gsm encoded byte array [33]
     */
    public byte[] encode_frame(short sop[]) {
        int start_pos = 0;
        byte[] output = new byte[33];
        
        
        // once per frame:
        short s[] = pre_processing_clause(sop);
        
        // Changes 's' inside
        _LAR = lpc_analysis_clause(s);
        start_pos = copyHeaderLARc(_LAR, output, start_pos);
        
        short LARpp_j[] = new short[9];
        // Changes 'LARpp_j' inside
        short d[] = short_term_analysis_filtering_clause(_LAR, s,
                _LARpp_j_1, LARpp_j, _u);
        
        // 4x per frame:
        // How does this code shift from the first to the fourth
        // sub-segment?
        // Option: pass offset parameter (based on sub) into d!
        _params = new SegmentParam[4];
        
        short dpp[] = new short[SEGMENT];
        short e[];
        int d_start = 0; // check value (d vs so)
        for (int sub = 0; sub < 4; sub++) {
            SegmentParam para = new SegmentParam();
            _params[sub] = para;
            e = long_term_predictor_clause(d, d_start, para, _dp, dpp);
            rpe_encoding_clause(e, _dp, dpp, para);
            
            start_pos = copySubSegment(para, output, start_pos);
            d_start += SEGMENT;
        }
        
        _LARpp_j_1 = LARpp_j;
        return output;
    }
    
    /************ once per frame ************/
    
    public short[] pre_processing_clause(short sop[]) {
        short so[] = downscaleInput(sop);
        short sof[] = offsetCompensation(so);
        short s[] = pre_emphasis(sof);
        return s;
    }
    
    public short[] lpc_analysis_clause(short s[]) {
        // Changes 's' internally
        int i_ACF[] = autocorrelation(s);
        short r[] = getReflectionCoefficients(i_ACF);
        short LAR[] = transformReflectionCoefficients(r);
        short LARc[] = quantizeCodeLAR(LAR);
        return LARc;
    }
    
    public short[] short_term_analysis_filtering_clause(short LARc[],
            short s[], short LARpp_j_1[], short LARpp_j[], short u[]) {
        LARpp_j = decodeLAR(LARc, LARpp_j);
        
        short LARp[] = new short[9];
        short rp[] = new short[9];
        short d[] = new short[FRAME];
        
        int k_start, k_end;
        
        k_start = 0;
        k_end = 13;
        LARp = interpolationLARpp_1(LARpp_j_1, LARpp_j, LARp);
        rp = computeRP(LARp, rp);
        // Changes 'd' inside
        shortTermAnalysisFiltering(d, rp, s, k_start, k_end, u);
        
        k_start = 13;
        k_end = 27;
        LARp = interpolationLARpp_2(LARpp_j_1, LARpp_j, LARp);
        rp = computeRP(LARp, rp);
        shortTermAnalysisFiltering(d, rp, s, k_start, k_end, u);
        
        k_start = 27;
        k_end = 40;
        LARp = interpolationLARpp_3(LARpp_j_1, LARpp_j, LARp);
        rp = computeRP(LARp, rp);
        shortTermAnalysisFiltering(d, rp, s, k_start, k_end, u);
        
        k_start = 40;
        k_end = 160;
        LARp = interpolationLARpp_4(LARpp_j_1, LARpp_j, LARp);
        rp = computeRP(LARp, rp);
        shortTermAnalysisFiltering(d, rp, s, k_start, k_end, u);
        
        return d;
    }
    
    /************ 4x per frame ************/
    
    public short[] long_term_predictor_clause(short d[], int d_start,
            SegmentParam param, short dp[],
            short dpp[]) {
        // Changes param._bc, param._Nc
        dp = getLTPParameters(d, d_start, dp, param);
        
        // Changes 'dpp' inside
        short e[] = longTermAnalysisFiltering(dp, d, d_start, dpp, param);
        return e;
    }
    
    public void rpe_encoding_clause(short e[], short dp[],
            short dpp[], SegmentParam param) {
        short x[] = weightingFilter(e);
        
        // Changes 'param._Mc' inside
        short xM[] = rpeGridSelection(x, param);
        
        // Changes 'param._mant' & 'param._exp' inside
        apcmQuantizationRPESequence(xM, param);
        
        // Changes 'param._mant' & 'param._exp' inside
        getExponentMantissa(param);
        
        // Changes 'param._xMc' inside
        computeXMC(xM, param);
        
        short xMp[] = apcmInverseQuantization(param);
        short ep[] = rpeGridPositioning(xMp, param);
        
        dp = updateDP(dp, ep, dpp);
    }
    
    /******************************************************************/
    /************ Methods according to document's sections ************/
    /******************************************************************/
    
    /**
     * Procedure 5.2.1 Downscaling of the input signal<br/>
     * After A or μ--law (PCS 1900) to linear conversion (or directly
     * from the A to D converter) the following scaling is assumed for
     * input to the RPE-LTP algorithm:<br/>
     *
     * <pre>S.v.v.v.v.v.v.v.v.v.v.v.v.x.x.x (2's complement format),
     * where</pre>
     * <ul>
     *   <li>S - the sign bit</li>
     *   <li>v - valid bit</li>
     *   <li>x - "don't care" bit</li>
     * </ul>
     * The original signal is called sop[..];
     */
    private short[] downscaleInput(short sop[]) {
        short so[] = new short[FRAME];
        for (int k = 0; k < FRAME; k++) {
            so[k] = AR.toShort(sop[k] >> 3);
            so[k] = AR.toShort(so[k] << 2);
        }
        return so;
    }
    
    /**
     * Procedure 5.2.2 Offset compensation<br/>
     * This part implements a high-pass filter and requires extended
     * arithmetic precision for the recursive part of this filter.</br>
     * The input of this procedure is the array so[0..159] and the output
     * the array sof[0..159].<br/>
     */
    private short[] offsetCompensation(short so[]) {
        // Keep _z1 and _i_z2 in memory for the next frame.
        // Initial value: _z1=0; _i_z2=0;
        short sof[] = new short[FRAME];
        short number1 = (short) 32735;
        
        // 16384 (dec) = 0x4000
        short number2 = 0x4000;
        
        for (int k = 0; k < FRAME; k++) {
            // Compute the non-recursive part.
            short s1 = AR.s_sub(so[k], _z1);
            _z1 = so[k];
            
            // Compute the recursive part.
            int i_s2 = AR.toInt(s1);
            i_s2 = i_s2 << 15;
            
            // Execution of a 31 by 16 bits multiplication.
            short msp = AR.toShort(_i_z2 >> 15);
            short lsp = AR.toShort(AR.i_sub(_i_z2, (msp << 15)));
            
            short temp = AR.s_mult_r(lsp, number1);
            i_s2 = AR.i_add(i_s2, temp);
            int i_temp = AR.i_mult(msp, number1) >> 1;
            _i_z2 = AR.i_add(i_temp, i_s2);
            
            // Compute sof[k] with rounding.
            sof[k] = AR.toShort(AR.i_add(_i_z2, number2) >> 15);
        }
        return sof;
    }
    
    /**
     * Procedure 5.2.3 Pre-emphasis<br/>
     */
    private short[] pre_emphasis(short sof[]) {
        // Keep _mp in memory for the next frame.
        // Initial value: _mp=0;
        short s[] = new short[FRAME];
        short number1 = (short) - 28180;
        short temp;
        
        for (int k = 0; k < FRAME; k++) {
            temp = AR.s_mult_r(_mp, number1);
            s[k] = AR.s_add(sof[k], temp);
            _mp = sof[k];
        }
        return s;
    }
    
    /**
     * Procedure 5.2.4 Autocorrelation<br/>
     * The goal is to compute the array i_ACF[k]. The signal s[i] shall
     * be scaled in order to avoid an overflow situation.<br/>
     * Dynamic scaling of the array s[0..159].
     */
    private int[] autocorrelation(short s[]) {
        // Search for the maximum.
        short smax = 0;
        short temp;
        for (int k = 0; k < FRAME; k++) {
            temp = AR.s_abs(s[k]);
            if (temp > smax) {
                smax = temp;
            }
        }
        
        // Computation of the scaling factor.
        short scalauto;
        if (smax == 0) {
            scalauto = 0;
        } else {
            scalauto = AR.s_sub( (short) 4, AR.norm(smax << 16));
        }
        
        // Scaling of the array s[0..159].
        // 16384 (dec) = 0x4000
        if (scalauto > 0) {
            temp = AR.toShort(0x4000 >> AR.s_sub(scalauto, (short) 1));
            for (int k = 0; k < FRAME; k++) {
                s[k] = AR.s_mult_r(s[k], temp);
            }
        }
        
        // Compute the i_ACF[..].
        int i_ACF[] = new int[FRAME];
        int i_temp;
        for (int k = 0; k < 9; k++) {
            i_ACF[k] = 0;
            for (int i = k; i < FRAME; i++) {
                i_temp = AR.i_mult(s[i], s[i - k]);
                i_ACF[k] = AR.i_add(i_ACF[k], i_temp);
            }
        }
        
        // Rescaling of the array s[0..159].
        if (scalauto > 0) {
            for (int k = 0; k < FRAME; k++) {
                s[k] = AR.toShort(s[k] << scalauto);
            }
        }
        return i_ACF;
    }
    
    /**
     * Procedure 5.2.5 Computation of the reflection coefficients<br/>
     */
    private short[] getReflectionCoefficients(int i_ACF[]) {
        short r[] = new short[9];
        // Schur recursion with 16 bits arithmetic.
        if (i_ACF[0] == 0) {
            for (int i = 1; i < 9; i++) {
                r[i] = 0;
            }
            // EXIT; /continue with clause 5.2.6/
        } else {
            short ACF[] = new short[9];
            short K[] = new short[9];
            short P[] = new short[9];
            
            short temp = AR.norm(i_ACF[0]);
            for (int k = 0; k < 9; k++) {
                ACF[k] = AR.toShort( (i_ACF[k] << temp) >> 16);
            }
            
            // Initialize array P[..] and K[..] for the recursion.
            for (int i = 1; i < 8; i++) {
                K[9 - i] = ACF[i];
            }
            for (int i = 0; i < 9; i++) {
                P[i] = ACF[i];
            }
            
            // Compute reflection coefficients.
            for (int n = 1; n < 9; n++) {
                short absP1 = AR.s_abs(P[1]);
                if (P[0] < absP1) {
                    for (int i = n; i < 9; i++) {
                        r[i] = 0;
                    }
                    // EXIT; /continue with clause 5.2.6/
                    break;
                }
                r[n] = AR.s_div(absP1, P[0]);
                if (P[1] > 0) {
                    r[n] = AR.s_sub( (short) 0, r[n]);
                }
                
                /*
                                 if (n == 8)
                                 {
                    // EXIT; /continue with clause 5.2-6/
                                 }
                 */
                if (n < 8) {
                    // Schur recursion.
                    short var2 = AR.s_mult_r(P[1], r[n]);
                    P[0] = AR.s_add(P[0], var2);
                    for (int m = 1; m < 9 - n; m++) {
                        var2 = AR.s_mult_r(K[9 - m], r[n]);
                        P[m] = AR.s_add(P[m + 1], var2);
                        
                        var2 = AR.s_mult_r(P[m + 1], r[n]);
                        K[9 - m] = AR.s_add(K[9 - m], var2);
                    }
                }
            }
        }
        
        return r;
    }
    
    /**
     * Procedure 5.2.6 Transformation of reflection coefficients to Log.-Area
     * Ratios<br/>
     */
    /*
     * The following scaling for r[..] and LAR[..] has been used:
     * r[..] = integer(real_r[..]*32768.); -1. <= real_r <1.
     * LAR[..] = integer(real_LAR[..]*16384.);
     * with -1.625 <= real_LAR <= 1.625
     */
    private short[] transformReflectionCoefficients(short r[]) {
        short LAR[] = new short[9];
        
        // Computation of the LAR[1..8] from the r[1..8].
        short temp, var;
        for (int i = 1; i < 9; i++) {
            temp = AR.s_abs(r[i]);
            if (temp < 22118) {
                temp = AR.toShort(temp >> 1);
            } else if (temp < 31130) {
                temp = AR.s_sub(temp, (short) 11059);
            } else {
                var = AR.s_sub(temp, (short) 26112);
                temp = AR.toShort(var << 2);
            }
            LAR[i] = temp;
            if (r[i] < 0) {
                LAR[i] = AR.s_sub( (short) 0, LAR[i]);
            }
        }
        return LAR;
    }
    
    /**
     * Procedure 5.2.7 Quantization and coding of the Log.-Area Ratios<br/>
     */
    /*
     * This procedure needs four tables; the following equations give the
     * optimum scaling for the constants:
     * A[1..8]= integer(real_A[1..8]*1024); 8 values (see table 5.1)
     * B[1..8]= integer(real_B[1..8]*512); 8 values (see table 5.1)
     * MAC[1..8]= maximum of the _LAR[1..8]; 8 values (see table 5.1)
     * MIC[1..8]= minimum of the _LAR[1..8]; 8 values (see table 5.1)
     */
    private short[] quantizeCodeLAR(short LAR[]) {
        short LARc[] = new short[9];
        // Computation for quantizing and coding the LAR[1..8].
        short temp;
        
        // 256 (dec) = 0x0100
        short number1 = 0x0100;
        for (int i = 1; i < 9; i++) {
            temp = AR.s_mult(A[i], LAR[i]);
            temp = AR.s_add(temp, B[i]);
            temp = AR.s_add(temp, number1); // for rounding
            LARc[i] = AR.toShort(temp >> 9);
            
            // Check if _LAR[i] lies between MIN and MAX
            if (LARc[i] > MAC[i]) {
                LARc[i] = MAC[i];
            } else if (LARc[i] < MIC[i]) {
                LARc[i] = MIC[i];
            }
            
            // This equation is used to make all the _LAR[i] positive.
            LARc[i] = AR.s_sub(LARc[i], MIC[i]);
        }
        return LARc;
    }
    
    /**
     * Procedure 5.2.10 Short term analysis filtering<br/>
     * This procedure computes the short term residual signal d[..] to
     * be fed to the RPE-LTP loop from the s[..] signal and from the
     * local rp[..] array (quantized reflection coefficients). As the
     * call of this procedure can be done in many ways (see the
     * interpolation of the LAR coefficient), it is assumed that the
     * computation begins with index k_start (for arrays d[..] and
     * s[..]) and stops with index k_end (k_start and k_end are defined
     * in 5.2.9.1). This procedure also needs to keep the array _u[0..7]
     * in memory for each call.
     */
    private void shortTermAnalysisFiltering(short d[], short rp[],
            short s[], int k_start, int k_end,
            short u[]) {
        // Keep the array _u[0..7] in memory for each call.
        // Initial value: _u[0..7]=0;
        
        short di, sav, temp, mult1, mult2, ui, rpi;
        for (int k = k_start; k < k_end; k++) {
            di = s[k];
            sav = di;
            for (int i = 1; i < 9; i++) {
                ui = u[i - 1];
                rpi = rp[i];
                
                mult1 = AR.s_mult_r(rpi, di);
                temp = AR.s_add(ui, mult1);
                
                mult2 = AR.s_mult_r(rpi, ui);
                di = AR.s_add(di, mult2);
                
                u[i - 1] = sav;
                sav = temp;
            }
            d[k] = di;
        }
    }
    
    /**
     * Procedure 5.2.11 Calculation of the LTP parameters<br/>
     * This procedure computes the LTP gain (_bc) and the LTP lag (_Nc)
     * for the long term analysis filter. This is done by calculating a
     * maximum of the cross-correlation function between the current
     * sub-segment short term residual signal d[0..39] (output of the
     * short term analysis filter; for simplification the index of this
     * array begins at 0 and ends at 39 for each sub-segment of the
     * RPE-LTP analysis) and the previous reconstructed short term
     * residual signal _dp[-120..-1]. A dynamic scaling shall be
     * performed to avoid overflow.
     */
    // 4x per frame
    private short[] getLTPParameters(short d[], int d_start, short dp[],
            SegmentParam param) {
        // Search of the optimum scaling of d[0..39].
        short dmax = 0;
        short temp, scal;
        short wt[] = new short[SEGMENT];
        
        int d_index = d_start;
        for (int k = 0; k < SEGMENT; k++) {
            temp = AR.s_abs(d[d_index]);
            if (temp > dmax) {
                dmax = temp;
            }
            d_index++;
        }
        
        temp = 0;
        if (dmax == 0) {
            scal = 0;
        } else {
            temp = AR.norm(dmax << 16);
        }
        if (temp > 6) {
            scal = 0;
        } else {
            scal = AR.s_sub( (short) 6, temp);
        }
        
        // Initialization of a working array wt[0..39].
        d_index = d_start;
        for (int k = 0; k < SEGMENT; k++) {
            wt[k] = AR.toShort(d[d_index] >> scal);
            d_index++;
        }
        
        // Search for the maximum cross-correlation and coding of the
        // LTP lag.
        int i_max, i_power, i_result, i_temp;
        i_max = 0;
        
        // Index for the maximum cross-correlation
        param._Nc = 40;
        short var1, var2;
        int ind;
        for (short lambda = 40; lambda < 121; lambda++) {
            i_result = 0;
            for (int k = 0; k < SEGMENT; k++) {
                var1 = wt[k];
                ind = DP_OFFSET + k - lambda;
                var2 = dp[ind];
                i_temp = AR.i_mult(var1, var2);
                i_result = AR.i_add(i_temp, i_result);
            }
            if (i_result > i_max) {
                param._Nc = lambda;
                i_max = i_result;
            }
        }
        
        // Rescaling of i_max.
        i_max = i_max >> (AR.s_sub( (short) 6, scal));
        
        // Initialization of a working array wt[0..39].
        for (int k = 0; k < SEGMENT; k++) {
            wt[k] = AR.toShort(dp[DP_OFFSET + k - param._Nc] >> 3);
        }
        
        // Compute the power of the reconstructed short term residual
        // signal _dp[..].
        i_power = 0;
        for (int k = 0; k < SEGMENT; k++) {
            i_temp = AR.i_mult(wt[k], wt[k]);
            i_power = AR.i_add(i_temp, i_power);
        }
        
        // Normalization of i_max and i_power.
        if (i_max <= 0) {
            param._bc = 0;
            // EXIT; // cont. with 5.2.12
        } else if (i_max >= i_power) {
            param._bc = 3;
            // EXIT; // cont. with 5.2.12
        } else {
            temp = AR.norm(i_power);
            short R = AR.toShort( (i_max << temp) >> 16);
            short S = AR.toShort( (i_power << temp) >> 16);
            
            /*
             * Coding of the LTP gain.
             * Table 5.3a shall be used to obtain the level DLB[i] for the
             * quantization of the LTP gain b to get the coded version _bc.
             */
            /*
                         for (int _bc=0; _bc<3; _bc++)
                         {
                if (R <= AR.s_mult(S, DLB[_bc]))
                {
                    break;
                    // EXIT; // cont. with 5.2.12
                }
                         }
                         _bc = 3;
             */
            // Rewritten previous block as:
            param._bc = 0;
            while ( (param._bc < 3) && (R > AR.s_mult(S, DLB[param._bc]))) {
                param._bc++;
            }
        }
        return dp;
    }
    
    /**
     * Procedure 5.2.12 Long term analysis filtering<br/>
     * In this part, we have to decode the _bc parameter to compute the
     * samples of the estimate dpp[0..39]. The decoding of _bc needs the
     * use of table 5.3b. The long term residual signal e[0..39] is then
     * calculated to be fed to the RPE encoding clause.
     */
    // 4x per frame
    private short[] longTermAnalysisFiltering(short dp[], short d[],
            int d_start, short dpp[],
            SegmentParam param) {
        short e[] = new short[SEGMENT];
        
        // Decoding of the coded LTP gain.
        short bp = QLB[param._bc];
        
        // Calculating the array e[0..39] and the array dpp[0..39].
        int d_index = d_start;
        for (int k = 0; k < SEGMENT; k++) {
            dpp[k] = AR.s_mult_r(bp, dp[DP_OFFSET + k - param._Nc]);
            e[k] = AR.s_sub(d[d_index], dpp[k]);
            d_index++;
        }
        return e;
    }
    
    /**
     * Procedure 5.2.13 Weighting filter<br/>
     * The coefficients of the weighting filter are stored in a table
     * (see table 5.4). The following scaling is used:<br/>
     * H[0..10] = integer(real_H[0..10]*8192);
     */
    // 4x per frame
    private short[] weightingFilter(short e[]) {
        // Initialization of a temporary working array wt[0..49].
        short wt[] = new short[50];
        short x[] = new short[SEGMENT];
        
        /*
                 // No need, new short[] inits all to zero
                 for (int k=0; k<5; k++)
                 {
            wt[k] = 0;
                 }
         */
        for (int k = 5; k < 45; k++) {
            wt[k] = e[k - 5];
        }
        /*
                 // No need, new short[] inits all to zero
                 for (int k=45; k<50; k++)
                 {
            wt[k] = 0;
                 }
         */
        
        // Compute the signal x[0..39].
        int i_result, i_temp;
        for (int k = 0; k < SEGMENT; k++) {
            //rounding of the output of the filter
            i_result = 8192; // = 0x2000
            for (int i = 0; i < 11; i++) {
                i_temp = AR.i_mult(wt[k + i], H[i]);
                i_result = AR.i_add(i_result, i_temp);
            }
            //scaling (x2)
            i_result = AR.i_add(i_result, i_result);
            //scaling (x4)
            i_result = AR.i_add(i_result, i_result);
            x[k] = AR.toShort(i_result >> 16);
        }
        return x;
    }
    
    /**
     * Procedure 5.2.14 RPE grid selection<br/>
     * The signal x[0..39] is used to select the RPE grid which is
     * represented by _Mc.
     */
    // 4x per frame
    private short[] rpeGridSelection(short x[], SegmentParam param) {
        short temp1;
        int i_temp;
        
        int i_EM = 0;
        param._Mc = 0;
        for (short m = 0; m < 4; m++) {
            int i_result = 0;
            for (int i = 0; i < 13; i++) {
                temp1 = AR.toShort(x[m + (3 * i)] >> 2);
                i_temp = AR.i_mult(temp1, temp1);
                i_result = AR.i_add(i_temp, i_result);
            }
            if (i_result > i_EM) {
                param._Mc = m;
                i_EM = i_result;
            }
        }
        
        // Down-sampling by a factor 3 to get the selected xM[0..12] RPE
        // sequence.
        short xM[] = new short[13];
        for (int i = 0; i < 13; i++) {
            xM[i] = x[param._Mc + (3 * i)];
        }
        return xM;
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
    private void apcmQuantizationRPESequence(short xM[], SegmentParam param) {
        short itest, temp;
        
        // Find the maximum absolute value xmax of xM[0..12].
        short xmax = 0;
        for (int i = 0; i < 13; i++) {
            temp = AR.s_abs(xM[i]);
            if (temp > xmax) {
                xmax = temp;
            }
        }
        
        // Quantizing and coding of xmax to get _xmaxc.
        param._exp = 0;
        temp = AR.toShort(xmax >> 9);
        itest = 0;
        for (int i = 0; i < 6; i++) {
            if (temp <= 0) {
                itest = 1;
            }
            temp = AR.toShort(temp >> 1);
            if (itest == 0) {
                param._exp = AR.s_add(param._exp, (short) 1);
            }
        }
        temp = AR.s_add(param._exp, (short) 5);
        short var1, var2;
        var1 = AR.toShort(xmax >> temp);
        var2 = AR.toShort(param._exp << 3);
        param._xmaxc = AR.s_add(var1, var2);
    }
    
    // 4x per frame
    private void computeXMC(short xM[], SegmentParam param) {
        // Direct computation of _xMc[0..12] using table 5.5.
        // Normalization by the exponent
        param._xMc = new short[13];
        short temp1 = AR.s_sub( (short) 6, param._exp);
        // See table 5.5 (inverse mantissa)
        short temp2 = NRFAC[param._mant];
        short temp, var1;
        for (int i = 0; i < 13; i++) {
            temp = AR.toShort(xM[i] << temp1);
            temp = AR.s_mult(temp, temp2);
            // This equation is used to make all the _xMc[i] positive.
            var1 = AR.toShort(temp >> 12);
            param._xMc[i] = AR.s_add(var1, (short) 4);
        }
    }
    
    /**
     * Procedure 5.2.18 Update of the reconstructed short term residual signal
     * _dp[-120..-1]<br/>
     * This procedure adds the reconstructed long term residual signal
     * ep[0..39] to the estimated signal dpp[0..39] from the long term
     * analysis filter to compute the reconstructed short term residual
     * signal _dp[-40..-1]; also the reconstructed short term residual
     * array _dp[-120..-41] is updated.<br/>
     * Keep the array _dp[-120..-1] in memory for the next sub-segment.<br/>
     * Initial value: _dp[-120..-1]=0;<br/>
     */
    // 4x per frame
    private short[] updateDP(short dp[], short ep[], short dpp[]) {
        for (int k = 0; k < 80; k++) {
            dp[DP_OFFSET - 120 + k] = dp[DP_OFFSET - 80 + k];
        }
        for (int k = 0; k < SEGMENT; k++) {
            dp[DP_OFFSET - 40 + k] = AR.s_add(ep[k], dpp[k]);
        }
        return dp;
    }
    
    /******************************************************************/
    /************ Format package **************************************/
    /******************************************************************/
    
    /*
     Table 1.1: Encoder output parameters in order of occurrence and
     bit allocation within the speech frame of 260 bits/20 ms
     
     GSM always starts with 0xD (4 bits)
     33 bytes = 33 * 8 bits = 264 bits
     
     ==================================================================
     Parameter Parameter Parameter         Var.  Number      Bit no.
              number    name              name  of bits     (LSB-MSB)
     ==================================================================
     ==================================================================
                   1                      LARc1     6       b1 - b6
                   2                      LARc2     6       b7 - b12
     FILTER         3    Log. Area         LARc3     5       b13 - b17
                   4    ratios            LARc4     5       b18 - b22
     PARAMETERS     5    1 - 8             LARc5     4       b23 - b26
                   6                      LARc6     4       b27 - b30
                   7                      LARc7     3       b31 - b33
                   8                      LARc8     3       b34 - b36
     ==================================================================
     
     Sub-frame no.1
     ==================================================================
     LTP            9    LTP lag           Nc1       7       b37 - b43
     PARAMETERS    10    LTP gain          bc1       2       b44 - b45
     ------------------------------------------------------------------
                  11    RPE grid position Mc1       2       b46 - b47
     RPE           12    Block amplitude   xmaxc1    6       b48 - b53
     PARAMETERS    13    RPE-pulse no.1    xMc1(0)   3       b54 - b56
                  14    RPE-pulse no.2    xMc1(1)   3       b57 - b59
                  ..    ...                                 ...
                  25    RPE-pulse no.13   xMc1(12)  3       b90 - b92
     ==================================================================
     
     Sub-frame no.2
     ==================================================================
     LTP           26    LTP lag           Nc2       7       b93 - b99
     PARAMETERS    27    LTP gain          bc2       2       b100- b101
     ------------------------------------------------------------------
                  28    RPE grid position Mc2       2       b102- b103
     RPE           29    Block amplitude   xmaxc2    6       b104- b109
     PARAMETERS    30    RPE-pulse no.1    xMc2(0)   3       b110- b112
                  31    RPE-pulse no.2    xMc2(1)   3       b113- b115
                  ..    ...                                 ...
                  42    RPE-pulse no.13   xMc2(12)  3       b146- b148
     ==================================================================
     
     Sub-frame no.3
     ==================================================================
     LTP           43    LTP lag           Nc3       7       b149- b155
     PARAMETERS    44    LTP gain          bc3       2       b156- b157
     ------------------------------------------------------------------
                  45    RPE grid position Mc3       2       b158- b159
     RPE           46    Block amplitude   xmaxc3    6       b160- b165
     PARAMETERS    47    RPE-pulse no.1    xMc3(0)   3       b166- b168
                  48    RPE-pulse no.2    xMc3(1)   3       b169- b171
                  ..    ...                                 ...
                  59    RPE-pulse no.13   xMc3(12)  3       b202- b204
     ==================================================================
     
     Sub-frame no.4
     ==================================================================
     LTP           60    LTP lag           Nc4       7       b205- b211
     PARAMETERS    61    LTP gain          bc4       2       b212- b213
     ------------------------------------------------------------------
                  62    RPE grid position Mc4       2       b214- b215
     RPE           63    Block amplitude   xmaxc4    6       b216- b221
     PARAMETERS    64    RPE-pulse no.1    xMc4(0)   3       b222- b224
                  65    RPE-pulse no.2    xMc4(1)   3       b225- b227
                  ..    ...                                 ...
                  76    RPE-pulse no.13   xMc4(12)  3       b258- b260
     ==================================================================
     
     Start Position    No bits     In Byte   Param
      0                4           0        GSM_HEADER    // 0
      4                6           0,1      _LAR[1]       // 8
     10                6           1        _LAR[2]
     
     16                5           2        _LAR[3]       // 16
     21                5           2,3      _LAR[4]       // 24
     26                4           2        _LAR[5]
     30                4           3,4      _LAR[6]       // 32
     34                3           3        _LAR[7]
     37                3           4        _LAR[8]
     
     Subframe 1 (55 bits):
     40                7           5        _Nc(1)         // 40
     47                2           5,6      _bc(1)         // 48
     49                2           6        _Mc(1)
     51                6           6,7      _xmaxc(1)      // 56
     57                3           7        _xMc(1)[0]
     60                3           7        _xMc(1)[1]
     63                3           7,8      _xMc(1)[2]     // 64
     66                3           8        _xMc(1)[3]
     69                3           8        _xMc(1)[4]
     71                3           8,9      _xMc(1)[5]     // 72
     74                3           9        _xMc(1)[6]
     77                3           9        _xMc(1)[7]
     
     80                3           10       _xMc(1)[8]     // 80
     83                3           10       _xMc(1)[9]
     86                3           10,11    _xMc(1)[10]    // 88
     89                3           11       _xMc(1)[11]
     92                3           11       _xMc(1)[12]
     */
    
    public int copyHeaderLARc(short LARc[], byte output[], int start_pos) {
        /*
         * 4 GSM_HEADER
         * 6 _LAR[1]
         * 6 _LAR[2]
         * 5 _LAR[3]
         * 5 _LAR[4]
         * 4 _LAR[5]
         * 4 _LAR[6]
         * 3 _LAR[7]
         * 3 _LAR[8]
         */
        start_pos = copyBits(GSM_HEADER, 4, output, start_pos);
        
        start_pos = copyBits(LARc[1], 6, output, start_pos);
        start_pos = copyBits(LARc[2], 6, output, start_pos);
        start_pos = copyBits(LARc[3], 5, output, start_pos);
        start_pos = copyBits(LARc[4], 5, output, start_pos);
        start_pos = copyBits(LARc[5], 4, output, start_pos);
        start_pos = copyBits(LARc[6], 4, output, start_pos);
        start_pos = copyBits(LARc[7], 3, output, start_pos);
        start_pos = copyBits(LARc[8], 3, output, start_pos);
        
        return start_pos;
    }
    
    public int copySubSegment(SegmentParam params,
            byte output[], int start_pos) {
        /*
         * 7 _Nc
         * 2 _bc
         * 2 _Mc
         * 6 _xmaxc
         * 3 _xMc[x]
         */
        start_pos = copyBits(params._Nc, 7, output, start_pos);
        start_pos = copyBits(params._bc, 2, output, start_pos);
        start_pos = copyBits(params._Mc, 2, output, start_pos);
        start_pos = copyBits(params._xmaxc, 6, output, start_pos);
        
        for (int i = 0; i < params._xMc.length; i++) {
            start_pos = copyBits(params._xMc[i], 3, output, start_pos);
        }
        return start_pos;
    }
    
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: GSM_Encoder <input file>"
                    + " <param output file> <gsm output file>");
            System.exit(1);
        } else {
            String input = args[0];
            String para_output = args[1];
            String gsm_output = args[2];
            File inputFile = new File(input);
            if (inputFile.canRead() == false) {
                System.err.println("Cannot read input file " +
                        inputFile.getAbsolutePath());
                System.exit(1);
            } else {
                DataInputStream in = null;
                File para_outputFile = new File(para_output);
                DataOutputStream para_out = null;
                
                File gsm_outputFile = new File(gsm_output);
                DataOutputStream gsm_out = null;
                try {
                    in = new DataInputStream(new FileInputStream(inputFile));
                } catch (FileNotFoundException exc) {
                    System.err.println("FileNotFoundException: " +
                            exc.getMessage());
                }
                
                try {
                    para_out = new DataOutputStream(
                            new FileOutputStream(para_outputFile, false));
                } catch (FileNotFoundException exc) {
                    System.err.println("FileNotFoundException: " +
                            exc.getMessage());
                }
                
                try {
                    gsm_out = new DataOutputStream(
                            new FileOutputStream(gsm_outputFile, false));
                } catch (FileNotFoundException exc) {
                    System.err.println("FileNotFoundException: " +
                            exc.getMessage());
                }
                
                if (in != null && para_out != null && gsm_out != null) {
                    System.out.println("In        - " +
                            inputFile.getAbsolutePath());
                    System.out.println("Param Out - " +
                            para_outputFile.getAbsolutePath());
                    System.out.println("GSM Out   - " +
                            gsm_outputFile.getAbsolutePath());
                    GregorianCalendar now1 = new GregorianCalendar();
                    long millis1 = now1.getTimeInMillis();
                    
                    byte encoded[];
                    short sop[];
                    int i = 0;
                    int no_frames = 0;
                    
                    GSM_Encoder enc = new GSM_Encoder();
                    
                    try {
                        while (true) {
                            // read a FRAME
                            sop = new short[FRAME];
                            for (i = 0; i < FRAME; i++) {
                                sop[i] = enc.readShort(in);
                            }
                            
                            // only process full frames
                            if (i == FRAME) {
                                // encode a FRAME
                                no_frames++;
                                encoded = enc.encode_frame(sop);
                                
                                // write encoded _params
                                enc.writeCodedParams(para_out);
                                
                                // write gsm output
                                gsm_out.write(encoded, 0, encoded.length);
                            }
                        }
                    } catch (EOFException exc) {
                        // This is expected to happen!
                    } catch (IOException exc) {
                        System.err.println("IOException: " + exc.getMessage());
                    }
                    
                    System.err.println("no_frames=" + no_frames
                            + ", i=" + i + " (should be 0)");
                    
                    GregorianCalendar now2 = new GregorianCalendar();
                    long millis2 = now2.getTimeInMillis();
                    long delta = millis2 - millis1;
                    System.out.println("Duration - " + delta + " milli sec");
                }
                
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException exc) {}
                }
                if (para_out != null) {
                    try {
                        para_out.close();
                    } catch (IOException exc) {}
                }
                if (gsm_out != null) {
                    try {
                        gsm_out.close();
                    } catch (IOException exc) {}
                }
                System.exit(0);
            }
        }
    }
    
}
