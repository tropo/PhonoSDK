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

public interface GSM_Constant {

    public final int FRAME = 160;
    public final int SEGMENT = 40;
    
    // 4 bits GSM Header
    public final char GSM_HEADER = 0xD;
    
    // Chapter 5.4
    // Tables used in the fixed point implementation of the RPE-LTP
    // coder and decoder
    
    // Table 5.1: Quantization of the Log.-Area Ratios, index=1..8
    // index 1..8
    public final static short A[] =
    {
        0, 20480, 20480, 20480, 20480, 13964, 15360, 8534, 9036
    };
    // index 1..8
    public final static short B[] =
    {
        0, 0, 0, 2048, -2560, 94, -1792, -341, -1144
    };
    // index 1..8
    public final static short MIC[] =
    {
        0, -32, -32, -16, -16, -8, -8, -4, -4
    };
    // index 1..8
    public final static short MAC[] =
    {
        0, 31, 31, 15, 15, 7, 7, 3, 3
    };
    
    // Table 5.2: Tabulation of 1/A[1..8]
    public final static short INVA[] =
    {
        0, 13107, 13107, 13107, 13107, 19223, 17476, 31454, 29708
    };
    
    // Table 5.3a: Decision level of the LTP gain quantizer
    // index 0..3
    public final static short DLB[] =
    {
        6554, 16384, 26214, 32767
    };
    
    // Table 5.3b: Quantization levels of the LTP gain quantizer
    // index 0..3
    public final static short QLB[] =
    {
        3277, 11469, 21299, 32767
    };
    
    // Table 5.4: Coefficients of the weighting filter
    // index 0..10
    public final static short H[] =
    {
        -134, -374, 0, 2054, 5741, 8192, 5741, 2054, 0, -374, -134
    };
    
    // Table 5.5: Normalized inverse mantissa used to compute xM/xmax
    // index 0..7
    public final static short NRFAC[] =
    {
        29128, 26215, 23832, 21846, 20165, 18725, 17476, 16384
    };
    
    // Table 5.6: Normalized direct mantissa used to compute xM/xmax
    // index 0..7
    public final static short FAC[] =
    {
        18431, 20479, 22527, 24575, 26623, 28671, 30719, 32767
    };
    
}
