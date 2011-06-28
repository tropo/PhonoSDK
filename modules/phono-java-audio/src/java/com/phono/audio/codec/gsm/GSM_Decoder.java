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

import com.phono.audio.codec.DecoderFace;
import java.io.*;
import java.util.*;

/**
 * Chapter 5.3 Fixed point implementation of the RPE-LTP decoder<br/>
 * <br/>
 * ETSI EN 300 961 V8.1.1 (2000-11)<br/>
 * European Standard (Telecommunications series)<br/>
 * Digital cellular telecommunications system (Phase 2+);<br/>
 * Full rate speech;<br/>
 * Transcoding<br/>
 * (GSM 06.10 version 8.1.1 Release 1999)<br/>
 *
 */
public class GSM_Decoder extends GSM_Base implements DecoderFace {
    
    /**
     * Used to offset index into _drp[-120..39]
     */
    public final int DRP_OFFSET = 120;
    
    private short _nrp;
    private short _drp[];
    private short _v[];
    private short _msr;
    
    public GSM_Decoder() {
        // Keep the _nrp value for the next sub-segment.
        // Initial value: _nrp=40;
        _nrp = 40;
        
        // An array _drp[-120..39] is used in this procedure
        // Keep the array _drp[-120..-1] for the next sub-segment.
        // Initial value: _drp[-120..-1]=0;
        _drp = new short[FRAME];
        
        // Keep the array _v[0..8] in memory for the next call.
        // Initial value: _v[0..8]=0;
        _v = new short[9];
        
        // Keep _msr in memory for the next frame.
        // Initial value: _msr=0;
        _msr = 0;
    }
    
    // byte input[] = new byte[33];
    public short[] decode_frame(byte input[]) {
        getCodedParams(input);
        
        short srop[] = decode_params();
        return srop;
    }
    
    // byte input[] = new byte[33];
    // creates _LAR & _params
    public void getCodedParams(byte input[]) {
        _LAR = new short[9];
        int start_pos = 0;
        start_pos = getLARcFromInput(input, start_pos, _LAR);
        
        _params = new SegmentParam[4];
        for (int sub=0; sub<4; sub++) {
            SegmentParam para = new SegmentParam();
            _params[sub] = para;
            start_pos = getSubSegment(input, start_pos, para);
        }
    }
    
    
    /*
     * Only the synthesis filter and the de-emphasis procedure are
     * different from the procedures found in the RPE-LTP coder.<br/>
     * Procedures 5.3.1 and 5.3.2 are executed for each sub-segment
     * (four times per frame). Procedures 5.3.3, 5.3.4 and 5.3.5 are
     * executed once per frame.
     */
    public short[] decode_params() {
        short wt[] = new short[FRAME];
        for (int sub=0; sub<4; sub++) {
            SegmentParam param = _params[sub];
            
            // Procedure 5.3.1
            short erp[] = rpe_decoding_clause(param);
            
            // Procedure 5.3.2
            _drp = longTermSynthesisFiltering(erp, _drp, param);
            
            int index = sub * SEGMENT;
            for (int k=0; k<SEGMENT; k++) {
                wt[index] = _drp[DRP_OFFSET+k];
                index++;
            }
        }
        
        short LARpp_j[] = new short[9];
        
        // Procedure 5.3.3
        // Procedure 5.3.4
        short sr [] = short_term_synthesis_filtering_clause(_LAR,
                _LARpp_j_1, LARpp_j, _v, wt);
        
        // Procedure 5.3.5
        short srop[] = post_processing_clause(sr);
        
        _LARpp_j_1 = LARpp_j;
        return srop;
    }
    
    /**
     * 5.3.1 RPE decoding clause<br/>
     * Procedure 5.2.15 (only the part to get mant and exp of _xmaxc),
     * procedure 5.2.16 and
     * procedure 5.2.17 are used to obtain the reconstructed long term
     * residual signal erp[0..39] signal from the received parameters
     * for each sub-segment (i.e. Mcr, xmaxcr, xMcr[0..12]).
     */
    // 4x per frame
    public short[] rpe_decoding_clause(SegmentParam param) {
        // procedure 5.2.15 (2)
        // Changes 'param.mant' & 'param.exp' inside
        getExponentMantissa(param);
        
        // procedure 5.2.16
        short xMp[] = apcmInverseQuantization(param);
        
        // procedure 5.2.17
        short erp[] = rpeGridPositioning(xMp, param);
        return erp;
    }
    
    /**
     * 5.3.2 Long term synthesis filtering<br/>
     * This procedure uses the bcr and Ncr parameter to realize the long
     * term synthesis filtering. The decoding of bcr needs the use of
     * table 5.3b.<br/>
     * <ul>
     *   <li>Nr is the received and decoded LTP lag.</li>
     *   <li>An array _drp[-120..39] is used in this procedure.</li>
     * </ul>
     * The elements for -120 to -1 of the array _drp are kept in memory
     * for the long term synthesis filter. For each sub-segment (40
     * samples), this procedure computes the _drp[0..39] to be fed to the
     * synthesis filter.
     */
    // 4x per frame
    private short[] longTermSynthesisFiltering(short erp[],
            short drp[], SegmentParam param) {
        // Check the limits of Nr.
        // Keep the _nrp value for the next sub-segment.
        // Initial value: _nrp=40;
        
        short Nr = param._Nc;
        if (param._Nc < 40) {
            Nr = _nrp;
        }
        if (param._Nc > 120) {
            Nr = _nrp;
        }
        _nrp= Nr;
        
        // Decoding of the LTP gain _bc.
        short brp = QLB[param._bc];
        
        // Keep the array _drp[-120..-1] for the next sub-segment.
        // Initial value: _drp[-120..-1]=0;
        
        // Computation of the reconstructed short term residual signal
        // _drp[0..39].
        short drpp;
        for (int k=0; k<SEGMENT; k++) {
            drpp = AR.s_mult_r(brp, drp[DRP_OFFSET+k-Nr]);
            drp[DRP_OFFSET+k] = AR.s_add(erp[k], drpp);
        }
        
        // Update of the reconstructed short term residual signal
        // _drp[-1..-120].
        for (int k=0; k<120; k++) {
            drp[DRP_OFFSET-120+k] = drp[DRP_OFFSET-80+k];
        }
        return drp;
    }
    
    
    /**
     * 5.3.3 Computation of the decoded reflection coefficients<br/>
     * This procedure (which is executed once per frame) is the same as
     * the one described in the CODER part.<br/>
     * For decoding of the received LARcr[1..8], see procedure 5.2.8.<br/>
     * For the interpolation of the decoded Log.-Area Ratios, see
     * procedure 5.2.9.1 <br/>
     * For the computation of the reflection coefficients rrp[1..8], see
     * procedure 5.2.9.2.
     */
    // once per frame
    public short[] short_term_synthesis_filtering_clause(short LARc[],
            short LARpp_j_1[], short LARpp_j[], short v[], short wt[]) {
        // procedure 5.2.8
        LARpp_j = decodeLAR(LARc, LARpp_j);
        
        short LARp[] = new short[9];
        short rrp[] = new short[9];
        short sr[] = new short[FRAME];
        
        int k_start, k_end;
        
        k_start = 0;
        k_end = 13;
        // procedure 5.2.9.1
        LARp = interpolationLARpp_1(LARpp_j_1, LARpp_j, LARp);
        // procedure 5.2.9.2
        rrp = computeRP(LARp, rrp);
        // Changes '_drp' inside
        sr = shortTermSynthesisFilteringClause(_drp, rrp, sr, k_start, k_end, v, wt);
        
        k_start = 13;
        k_end = 27;
        LARp = interpolationLARpp_2(LARpp_j_1, LARpp_j, LARp);
        rrp = computeRP(LARp, rrp);
        sr = shortTermSynthesisFilteringClause(_drp, rrp, sr, k_start, k_end, v, wt);
        
        k_start = 27;
        k_end = 40;
        LARp = interpolationLARpp_3(LARpp_j_1, LARpp_j, LARp);
        rrp = computeRP(LARp, rrp);
        sr = shortTermSynthesisFilteringClause(_drp, rrp, sr, k_start, k_end, v, wt);
        
        k_start = 40;
        k_end = 160;
        LARp = interpolationLARpp_4(LARpp_j_1, LARpp_j, LARp);
        rrp = computeRP(LARp, rrp);
        sr = shortTermSynthesisFilteringClause(_drp, rrp, sr, k_start, k_end, v, wt);
        return sr;
    }
    
    
    /**
     * 5.3.4 Short term synthesis filtering clause<br/>
     * This procedure uses the _drp[0..39] signal and produces the
     * sr[0..159] signal which is the output of the short term synthesis
     * filter. For ease of explanation, a temporary array wt[0..159] is
     * used.
     */
    // once per frame
    private short[] shortTermSynthesisFilteringClause(short drp[],
            short rrp[], short sr[], int k_start, int k_end, short v[],
            short wt[]) {
        /*
         * As the call of the short term synthesis filter procedure can
         * be done in many ways (see the interpolation of the LAR
         * coefficient), it is assumed that the computation begins with
         * index k_start (for arrays wt[..] and sr[..]) and stops with
         * index k_end (k_start and k_end are defined in 5.2.9.1). The
         * procedure also needs to keep the array _v[0..8] in memory
         * between calls.
         */
        
        // Keep the array _v[0..8] in memory for the next call.
        // Initial value: _v[0..8]=0;
        short sri;
        for (int k=k_start; k<k_end; k++) {
            sri = wt[k];
            for (int i=1; i<9; i++) {
                sri = AR.s_sub(sri, AR.s_mult_r(rrp[9-i], v[8-i]));
                v[9-i] = AR.s_add(v[8-i], AR.s_mult_r(rrp[9-i], sri));
            }
            sr[k] = sri;
            v[0] = sri;
        }
        return sr;
    }
    
    
    // Post-processing
    // once per frame
    public short[] post_processing_clause(short sr[]) {
        short sro[] = de_emphasisFiltering(sr);
        short srop[] = upscaleOutputSignal(sro);
        srop = truncateOutputVariable(srop);
        return srop;
    }
    
    /**
     * 5.3.5 De-emphasis filtering
     */
    // once per frame
    private short[] de_emphasisFiltering(short sr[]) {
        // Keep _msr in memory for the next frame.
        // Initial value: _msr=0;
        short temp;
        short sro[] = new short[FRAME];
        for (int k=0; k<FRAME; k++) {
            temp = AR.s_add(sr[k], AR.s_mult_r(_msr, (short)28180));
            _msr = temp;
            sro[k] = _msr;
        }
        return sro;
    }
    
    /**
     * 5.3.6 Upscaling of the output signal
     */
    private short[] upscaleOutputSignal(short sro[]) {
        short srop[] = new short[FRAME];
        for (int k=0; k<FRAME; k++) {
            srop[k] = AR.s_add(sro[k], sro[k]);
        }
        return srop;
    }
    
    /**
     * 5.3.7 Truncation of the output variable<br/>
     * 
     * The output format is the following:
     * <pre>S._v._v._v._v._v._v._v._v._v._v._v._v.0.0.0 (2's complement format),
     * where</pre>
     * <ul>
     *   <li>S - the sign bit</li>
     *   <li>_v - valid bit</li>
     * </ul>
     */
    private short[] truncateOutputVariable(short srop[]) {
        for (int k=0; k<FRAME; k++) {
            srop[k] = AR.toShort(srop[k] >> 3);
            srop[k] = AR.toShort(srop[k] << 3);
        }
        return srop;
    }
    
    /*
     * NOTE: When a linear to A-law compression is needed, then the
     * sub-block COMPRESS of CCITT G721 recommendation shall be used
     * with inputs:
     *    SR = srop[k] >> 3;
     *    LAW = 1;
     *
     * When a linear to μ-law compression is needed, then the sub-block
     * COMPRESS of CCITT G721 recommendation shall be used with inputs:
     *    SR = srop[k] >> 3;
     *    LAW = 0;
     */
    
    
    // short _LAR[] = new short[9];
    public int getLARcFromInput(byte input[], int start_pos, short LARc[]) {
        // skip GSM_HEADER, 4 bits
        start_pos += 4;
        
        int no_bits = 6;
        LARc[1] = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        LARc[2] = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        
        no_bits = 5;
        LARc[3] = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        LARc[4] = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        
        no_bits = 4;
        LARc[5] = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        LARc[6] = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        
        no_bits = 3;
        LARc[7] = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        LARc[8] = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        
        return start_pos;
    }
    
    public int getSubSegment(byte input[], int start_pos,
            SegmentParam param) {
        /*
         * 7 _Nc
         * 2 _bc
         * 2 _Mc
         * 6 _xmaxc
         * 3 _xMc[x]
         */
        int no_bits = 7;
        param._Nc = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        
        no_bits = 2;
        param._bc = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        
        no_bits = 2;
        param._Mc = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        
        no_bits = 6;
        param._xmaxc = copyBits(input, start_pos, no_bits);
        start_pos += no_bits;
        
        no_bits = 3;
        param._xMc = new short[13];
        for (int i=0; i<13; i++) {
            param._xMc[i] = copyBits(input, start_pos, no_bits);
            start_pos += no_bits;
        }
        return start_pos;
    }
    
    
    public static void main(String[] args) {
        if (args.length !=2 && args.length != 3) {
            System.err.println("Usage: GSM_Decoder"
                    + " [ <gsm input file> ]"
                    + " <param in/out file>"
                    + " <output file>");
            System.exit(1);
        } else {
            String input = null;
            String para_output = null;
            String output = null;
            boolean isInputGSM = false;
            
            if (args.length == 2) {
                // encoded para is the input file
                input = args[0];
                output = args[1];
                isInputGSM = false;
            } else {
                // para is an intermediate output file
                input = args[0];
                para_output = args[1];
                output = args[2];
                isInputGSM = true;
            }
            
            File inputFile = new File(input);
            if (inputFile.canRead() == false) {
                System.err.println("Cannot read input file "
                        + inputFile.getAbsolutePath());
                System.exit(1);
            } else {
                DataInputStream in = null;
                File outputFile = new File(output);
                DataOutputStream out = null;
                try {
                    in = new DataInputStream(
                            new FileInputStream(inputFile));
                } catch (FileNotFoundException exc) {
                    System.err.println("FileNotFoundException: " + exc.getMessage());
                }
                
                try {
                    out = new DataOutputStream(new FileOutputStream(outputFile, false));
                } catch (FileNotFoundException exc) {
                    System.err.println("FileNotFoundException: " + exc.getMessage());
                }
                
                File para_outputFile = null;
                DataOutputStream para_out = null;
                if (isInputGSM == true) {
                    para_outputFile = new File(para_output);
                    try {
                        para_out = new DataOutputStream(
                                new FileOutputStream(para_outputFile, false));
                    } catch (FileNotFoundException exc) {
                        System.err.println("FileNotFoundException: " + exc.getMessage());
                    }
                }
                
                if (in != null && out != null) {
                    System.out.println("In        - " + inputFile.getAbsolutePath());
                    if (para_out != null) {
                        System.out.println("Param Out - " + para_outputFile.getAbsolutePath());
                    }
                    System.out.println("Out       - " + outputFile.getAbsolutePath());
                    System.out.println("Input GSM - " + isInputGSM);
                    
                    GregorianCalendar now1 = new GregorianCalendar();
                    long millis1 = now1.getTimeInMillis();
                    
                    short srop[] = null;
                    int i=0;
                    int no_frames = 0;
                    boolean readOn = true;
                    
                    GSM_Decoder dec = new GSM_Decoder();
                    
                    try {
                        while (readOn) {
                            if (isInputGSM == true) {
                                // read the 33 input bytes
                                // byte gsm[] = new byte[33];
                                byte gsm[] = new byte[33];
                                int no_read = in.read(gsm);
                                if (no_read == gsm.length) {
                                    // get the encoded _params
                                    // written in gsm format
                                    dec.getCodedParams(gsm);
                                    
                                    // write encoded _params for test
                                    dec.writeCodedParams(para_out);
                                    
                                } else {
                                    readOn = false;
                                }
                            } else {
                                // read the encoded _params
                                // written as shorts
                                dec.readCodedParams(in);
                            }
                            
                            if (readOn) {
                                // decode the encoded _params
                                srop = dec.decode_params();
                                
                                // write it out
                                for (i=0; i<FRAME; i++) {
                                    dec.writeShort(out, srop[i]);
                                }
                                
                                no_frames++;
                            }
                        }
                    } catch (EOFException exc) {
                        // This is expected to happen!
                    } catch (IOException exc) {
                        System.err.println("IOException: " + exc.getMessage());
                    } catch (Exception exc) {
                        System.err.println("Exception: "
                                + exc.getClass().getName() + ": "
                                + exc.getMessage());
                        exc.printStackTrace();
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
                    } catch (IOException exc) { }
                }
                if (para_out != null) {
                    try {
                        para_out.close();
                    } catch (IOException exc) { }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException exc) { }
                }
                System.exit(0);
            }
        }
    }

    public byte[] lost_frame(byte current_frame[], byte next_frame[]) {
        return current_frame;
    }
}
