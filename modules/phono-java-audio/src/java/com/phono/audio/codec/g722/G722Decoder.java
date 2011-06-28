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

import com.phono.audio.Log;
import com.phono.audio.codec.*;

public class G722Decoder implements DecoderFace {

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
    int _rate;
    static int wl[] = {-60, -30, 58, 172, 334, 538, 1198, 3042};
    static int rl42[] = {0, 7, 6, 5, 4, 3, 2, 1, 7, 6, 5, 4, 3, 2, 1, 0};
    static int ilb[] = {
        2048, 2093, 2139, 2186, 2233, 2282, 2332,
        2383, 2435, 2489, 2543, 2599, 2656, 2714,
        2774, 2834, 2896, 2960, 3025, 3091, 3158,
        3228, 3298, 3371, 3444, 3520, 3597, 3676,
        3756, 3838, 3922, 4008
    };
    static int wh[] = {0, -214, 798};
    static int rh2[] = {2, 1, 2, 1};
    static int qm2[] = {-7408, -1616, 7408, 1616};
    static int qm4[] = {
        0, -20456, -12896, -8968,
        -6288, -4240, -2584, -1200,
        20456, 12896, 8968, 6288,
        4240, 2584, 1200, 0
    };
    static int qm5[] = {
        -280, -280, -23352, -17560,
        -14120, -11664, -9752, -8184,
        -6864, -5712, -4696, -3784,
        -2960, -2208, -1520, -880,
        23352, 17560, 14120, 11664,
        9752, 8184, 6864, 5712,
        4696, 3784, 2960, 2208,
        1520, 880, 280, -280
    };
    static int qm6[] = {
        -136, -136, -136, -136,
        -24808, -21904, -19008, -16704,
        -14984, -13512, -12280, -11192,
        -10232, -9360, -8576, -7856,
        -7192, -6576, -6000, -5456,
        -4944, -4464, -4008, -3576,
        -3168, -2776, -2400, -2032,
        -1688, -1360, -1040, -728,
        24808, 21904, 19008, 16704,
        14984, 13512, 12280, 11192,
        10232, 9360, 8576, 7856,
        7192, 6576, 6000, 5456,
        4944, 4464, 4008, 3576,
        3168, 2776, 2400, 2032,
        1688, 1360, 1040, 728,
        432, 136, -432, -136
    };
    static int qmf_coeffs[] = {
        3, -11, 12, 32, -210, 951, 3876, -805, 362, -156, 53, -11,};
    private int _audioSz;

    public G722Decoder(int audiosz) {
        _band = new Band[2];
        _band[0] = new Band();
        _band[1] = new Band();

        _rate = 64000;
        _bits_per_sample = 8;

        _eight_k = false;

        _packed = false;
        _band[0]._det = 32;
        _band[1]._det = 8;
        _audioSz = audiosz;

    }


    /*- End of function --------------------------------------------------------*/
    int g722_decode(short amp[], byte[] g722_data) {


        int dlowt;
        int rlow;
        int ihigh;
        int dhigh;
        int rhigh;
        int xout1;
        int xout2;
        int wd1;
        int wd2;
        int wd3;
        int code;
        int outlen;
        int i;
        int j;
        int len;

        len = g722_data.length;
        outlen = 0;
        rhigh = 0;
        for (j = 0; j < len;) {
            if (_packed) {
                /* Unpack the code bits */
                if (_in_bits < _bits_per_sample) {
                    _in_buffer |= (g722_data[j++] << _in_bits);
                    _in_bits += 8;
                }
                code = _in_buffer & ((1 << _bits_per_sample) - 1);
                _in_buffer >>= _bits_per_sample;
                _in_bits -= _bits_per_sample;
            } else {
                code = g722_data[j++];
            }

            switch (_bits_per_sample) {
                default:
                case 8:
                    wd1 = code & 0x3F;
                    ihigh = (code >> 6) & 0x03;
                    wd2 = qm6[wd1];
                    wd1 >>= 2;
                    break;
                case 7:
                    wd1 = code & 0x1F;
                    ihigh = (code >> 5) & 0x03;
                    wd2 = qm5[wd1];
                    wd1 >>= 1;
                    break;
                case 6:
                    wd1 = code & 0x0F;
                    ihigh = (code >> 4) & 0x03;
                    wd2 = qm4[wd1];
                    break;
            }
            /* Block 5L, LOW BAND INVQBL */
            wd2 = (_band[0]._det * wd2) >> 15;
            /* Block 5L, RECONS */
            rlow = _band[0]._s + wd2;
            /* Block 6L, LIMIT */
            if (rlow > 16383) {
                rlow = 16383;
            } else if (rlow < -16384) {
                rlow = -16384;
            }

            /* Block 2L, INVQAL */
            wd2 = qm4[wd1];
            dlowt = (_band[0]._det * wd2) >> 15;

            /* Block 3L, LOGSCL */
            wd2 = rl42[wd1];
            wd1 = (_band[0]._nb * 127) >> 7;
            wd1 += wl[wd2];
            if (wd1 < 0) {
                wd1 = 0;
            } else if (wd1 > 18432) {
                wd1 = 18432;
            }
            _band[0]._nb = wd1;

            /* Block 3L, SCALEL */
            wd1 = (_band[0]._nb >> 6) & 31;
            wd2 = 8 - (_band[0]._nb >> 11);
            wd3 = (wd2 < 0) ? (ilb[wd1] << -wd2) : (ilb[wd1] >> wd2);
            _band[0]._det = wd3 << 2;

            _band[0].block4(dlowt);

            if (!_eight_k) {
                /* Block 2H, INVQAH */
                wd2 = qm2[ihigh];
                dhigh = (_band[1]._det * wd2) >> 15;
                /* Block 5H, RECONS */
                rhigh = dhigh + _band[1]._s;
                /* Block 6H, LIMIT */
                if (rhigh > 16383) {
                    rhigh = 16383;
                } else if (rhigh < -16384) {
                    rhigh = -16384;
                }

                /* Block 2H, INVQAH */
                wd2 = rh2[ihigh];
                wd1 = (_band[1]._nb * 127) >> 7;
                wd1 += wh[wd2];
                if (wd1 < 0) {
                    wd1 = 0;
                } else if (wd1 > 22528) {
                    wd1 = 22528;
                }
                _band[1]._nb = wd1;

                /* Block 3H, SCALEH */
                wd1 = (_band[1]._nb >> 6) & 31;
                wd2 = 10 - (_band[1]._nb >> 11);
                wd3 = (wd2 < 0) ? (ilb[wd1] << -wd2) : (ilb[wd1] >> wd2);
                _band[1]._det = wd3 << 2;

                _band[1].block4(dhigh);
            }

            if (_itu_test_mode) {
                amp[outlen++] = (short) (rlow << 1);
                amp[outlen++] = (short) (rhigh << 1);
            } else {
                if (_eight_k) {
                    amp[outlen++] = (short) rlow;
                } else {
                    /* Apply the receive QMF */
                    for (i = 0; i < 22; i++) {
                        _x[i] = _x[i + 2];
                    }
                    _x[22] = rlow + rhigh;
                    _x[23] = rlow - rhigh;

                    xout1 = 0;
                    xout2 = 0;
                    for (i = 0; i < 12; i++) {
                        xout2 += _x[2 * i] * qmf_coeffs[i];
                        xout1 += _x[2 * i + 1] * qmf_coeffs[11 - i];
                    }
                    amp[outlen++] = (short) (xout1 >>> 12);
                    amp[outlen++] = (short) (xout2 >>> 12);
                }
            }
        }
        return outlen;
    }

    public short[] decode_frame(byte[] encoded_signal) {
        short[] ret = new short[_audioSz];
        int l = g722_decode(ret, encoded_signal);
        Log.verb("722 decode of "+encoded_signal.length
                +" bytes extecting to create "
                +ret.length+" audio samples, got "+l);
        return ret;

    }

    public byte[] lost_frame(byte current_frame[], byte next_frame[]) {
        return current_frame;
    }
}
