// This is a Java port of the Span DSP code with the following 'license'

/*
 * SpanDSP - a series of DSP components for telephony
 *
 * g722_decode.c - The ITU G.722 codec, decode part.
 *
 * Written by Steve Underwood <steveu@coppice.org>
 *
 * Copyright (C) 2005 Steve Underwood
 *
 *  Despite my general liking of the GPL, I place my own contributions
 *  to this code in the public domain for the benefit of all mankind -
 *  even the slimy ones who might try to proprietize my work and use it
 *  to my detriment.
 *
 * Based in part on a single channel G.722 codec which is:
 *
 * Copyright (c) CMU 1993
 * Computer Science, Speech Group
 * Chengxiang Lu and Alex Hauptmann
 *
 */
package com.phono.audio.codec.g722;

import com.phono.audio.codec.EncoderFace;

public class G722Encoder implements EncoderFace {
    
    /*! TRUE if the operating in the special ITU test mode, with the band split filters
    disabled. */

    boolean _itu_test_mode;
    /*! TRUE if the G.722 data is packed */
    boolean _packed;
    /*! TRUE if decode to 8k samples/second */
    boolean _eight_k;
    /*! 6 for 48000kbps, 7 for 56000kbps, or 8 for 64000kbps. */
    int _bits_per_sample;

    /*! Signal history for the QMF */
    int _x[] = new int[24];
    Band _band[];

    /*unsigned*/
    int _in_buffer;
    int _in_bits;
    /*unsigned*/
    int _out_buffer;
    int _out_bits;
    int _frameSize;
    static int _q6[] = {
        0, 35, 72, 110, 150, 190, 233, 276,
        323, 370, 422, 473, 530, 587, 650, 714,
        786, 858, 940, 1023, 1121, 1219, 1339, 1458,
        1612, 1765, 1980, 2195, 2557, 2919, 0, 0
    };
    static int _iln[] = {
        0, 63, 62, 31, 30, 29, 28, 27,
        26, 25, 24, 23, 22, 21, 20, 19,
        18, 17, 16, 15, 14, 13, 12, 11,
        10, 9, 8, 7, 6, 5, 4, 0
    };
    static int _ilp[] = {
        0, 61, 60, 59, 58, 57, 56, 55,
        54, 53, 52, 51, 50, 49, 48, 47,
        46, 45, 44, 43, 42, 41, 40, 39,
        38, 37, 36, 35, 34, 33, 32, 0
    };
    static int _wl[] = {
        -60, -30, 58, 172, 334, 538, 1198, 3042
    };
    static int _rl42[] = {
        0, 7, 6, 5, 4, 3, 2, 1, 7, 6, 5, 4, 3, 2, 1, 0
    };
    static int _ilb[] = {
        2048, 2093, 2139, 2186, 2233, 2282, 2332,
        2383, 2435, 2489, 2543, 2599, 2656, 2714,
        2774, 2834, 2896, 2960, 3025, 3091, 3158,
        3228, 3298, 3371, 3444, 3520, 3597, 3676,
        3756, 3838, 3922, 4008
    };
    static int _qm4[] = {
        0, -20456, -12896, -8968,
        -6288, -4240, -2584, -1200,
        20456, 12896, 8968, 6288,
        4240, 2584, 1200, 0
    };
    static int _qm2[] = {
        -7408, -1616, 7408, 1616
    };
    static int _qmf_coeffs[] = {
        3, -11, 12, 32, -210, 951, 3876, -805, 362, -156, 53, -11,};
    static int _ihn[] = {0, 1, 0};
    static int _ihp[] = {0, 3, 2};
    static int _wh[] = {0, -214, 798};
    static int _rh2[] = {2, 1, 2, 1};

    public G722Encoder(int frsz) {
        _frameSize = frsz;
        _band = new Band[2];
        _band[0] = new Band();
        _band[1] = new Band();

        _bits_per_sample = 8;

        _eight_k = false;

        _packed = false;
        _band[0]._det = 32;
        _band[1]._det = 8;

    }

    int g722_encode(byte g722_data[], short amp[]) {
        int dlow;
        int dhigh;
        int el;
        int wd;
        int wd1;
        int ril;
        int wd2;
        int il4;
        int ih2;
        int wd3;
        int eh;
        int mih;
        int i;
        int j;
        /* Low and high band PCM from the QMF */
        int xlow;
        int xhigh;
        int g722_bytes;
        /* Even and odd tap accumulators */
        int sumeven;
        int sumodd;
        int ihigh;
        int ilow;
        int code;
        int len = amp.length;

        g722_bytes = 0;
        xhigh = 0;
        for (j = 0; j < len;) {
            if (_itu_test_mode) {
                xlow =
                        xhigh = amp[j++] >> 1;
            } else {
                if (_eight_k) {
                    xlow = amp[j++];
                } else {
                    /* Apply the transmit QMF */
                    /* Shuffle the buffer down */
                    for (i = 0; i < 22; i++) {
                        _x[i] = _x[i + 2];
                    }
                    _x[22] = amp[j++];
                    _x[23] = amp[j++];

                    /* Discard every other QMF output */
                    sumeven = 0;
                    sumodd = 0;
                    for (i = 0; i < 12; i++) {
                        sumodd += _x[2 * i] * _qmf_coeffs[i];
                        sumeven += _x[2 * i + 1] * _qmf_coeffs[11 - i];
                    }
                    xlow = (sumeven + sumodd) >> 13;
                    xhigh = (sumeven - sumodd) >> 13;
                }
            }
            /* Block 1L, SUBTRA */
            el = G722Codec.saturate(xlow - _band[0]._s);

            /* Block 1L, QUANTL */
            wd = (el >= 0) ? el : -(el + 1);

            for (i = 1; i < 30; i++) {
                wd1 = (_q6[i] * _band[0]._det) >> 12;
                if (wd < wd1) {
                    break;
                }
            }
            ilow = (el < 0) ? _iln[i] : _ilp[i];

            /* Block 2L, INVQAL */
            ril = ilow >> 2;
            wd2 = _qm4[ril];
            dlow = (_band[0]._det * wd2) >> 15;

            /* Block 3L, LOGSCL */
            il4 = _rl42[ril];
            wd = (_band[0]._nb * 127) >> 7;
            _band[0]._nb = wd + _wl[il4];
            if (_band[0]._nb < 0) {
                _band[0]._nb = 0;
            } else if (_band[0]._nb > 18432) {
                _band[0]._nb = 18432;
            }

            /* Block 3L, SCALEL */
            wd1 = (_band[0]._nb >> 6) & 31;
            wd2 = 8 - (_band[0]._nb >> 11);
            wd3 = (wd2 < 0) ? (_ilb[wd1] << -wd2) : (_ilb[wd1] >> wd2);
            _band[0]._det = wd3 << 2;

            _band[0].block4(dlow);

            if (_eight_k) {
                /* Just leave the high bits as zero */
                code = (0xC0 | ilow) >> (8 - _bits_per_sample);
            } else {
                /* Block 1H, SUBTRA */
                eh = G722Codec.saturate(xhigh - _band[1]._s);

                /* Block 1H, QUANTH */
                wd = (eh >= 0) ? eh : -(eh + 1);
                wd1 = (564 * _band[1]._det) >> 12;
                mih = (wd >= wd1) ? 2 : 1;
                ihigh = (eh < 0) ? _ihn[mih] : _ihp[mih];

                /* Block 2H, INVQAH */
                wd2 = _qm2[ihigh];
                dhigh = (_band[1]._det * wd2) >> 15;

                /* Block 3H, LOGSCH */
                ih2 = _rh2[ihigh];
                wd = (_band[1]._nb * 127) >> 7;
                _band[1]._nb = wd + _wh[ih2];
                if (_band[1]._nb < 0) {
                    _band[1]._nb = 0;
                } else if (_band[1]._nb > 22528) {
                    _band[1]._nb = 22528;
                }

                /* Block 3H, SCALEH */
                wd1 = (_band[1]._nb >> 6) & 31;
                wd2 = 10 - (_band[1]._nb >> 11);
                wd3 = (wd2 < 0) ? (_ilb[wd1] << -wd2) : (_ilb[wd1] >> wd2);
                _band[1]._det = wd3 << 2;

                _band[1].block4(dhigh);
                code = ((ihigh << 6) | ilow) >> (8 - _bits_per_sample);
            }

            if (_packed) {
                /* Pack the code bits */
                _out_buffer |= (code << _out_bits);
                _out_bits += _bits_per_sample;
                if (_out_bits >= 8) {
                    g722_data[g722_bytes++] = (byte) (_out_buffer & 0xFF);
                    _out_bits -= 8;
                    _out_buffer >>= 8;
                }
            } else {
                g722_data[g722_bytes++] = (byte) code;
            }
        }
        return g722_bytes;
    }

    public byte[] encode_frame(short[] original_signal) {
        byte[] ret = new byte[_frameSize];
        g722_encode(ret, original_signal);
        return ret;
    }
}
