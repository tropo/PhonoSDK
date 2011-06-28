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

package com.phono.audio.codec;

import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;

/*
| 0x00000001 | G.723.1         | 4, 20, and 24 byte frames of 240   |
|            |                 | samples                            |
|            |                 |                                    |
| 0x00000002 | GSM Full Rate   | 33 byte chunks of 160 samples or   |
|            |                 | 65 byte chunks of 320 samples      |
|            |                 |                                    |
| 0x00000004 | G.711 mu-law    | 1 byte per sample                  |
|            |                 |                                    |
| 0x00000008 | G.711 a-law     | 1 byte per sample                  |

|            |                 |                                    |
| 0x00000010 | G.726           |                                    |
|            |                 |                                    |
| 0x00000020 | IMA ADPCM       | 1 byte per 2 samples               |
|            |                 |                                    |
| 0x00000040 | 16-bit linear   | 2 bytes per sample                 |
|            | little-endian   |                                    |
|            |                 |                                    |
| 0x00000080 | LPC10           | Variable size frame of 172 samples |
|            |                 |                                    |
| 0x00000100 | G.729           | 20 bytes chunks of 172 samples     |
|            |                 |                                    |
| 0x00000200 | Speex           | Variable                           |
|            |                 |                                    |
| 0x00000400 | ILBC            | 50 bytes per 240 samples           |
|            |                 |                                    |
| 0x00000800 | G.726 AAL2      |                                    |
|            |                 |                                    |
| 0x00001000 | G.722           | 16kHz ADPCM                        |
|            |                 |                                    |
| 0x00002000 | AMR             | Variable                           |
 */

/*
 * Discrepancies between asterisk and draft-04:
 * Asterisk:
 * - AST_FORMAT_G726_AAL2  (1 << 4)
 * - AST_FORMAT_G726   (1 << 11)
 *
 * Draft 04:
 * - AUDIO_AMR      = (1 << 13)
 */
/**
 * CodecFace
 *
 * This is the interface to be implemented by each codec to be supported by this 
 * IAX implementation.
 *
 * @see DecoderFace
 * @see EncoderFace
 *
 */
public interface CodecFace {

    /** G.723.1 compression */
    public final static long AUDIO_G723_1 = (1 << 0);
    /** GSM Full Rate compression */
    public final static long AUDIO_GSM = (1 << 1);
    /** Raw mu-law data (G.711) */
    public final static long AUDIO_ULAW = (1 << 2);
    /** Raw A-law data (G.711) */
    public final static long AUDIO_ALAW = (1 << 3);
    /** ADPCM (G.726, 32kbps, ?? codeword packing) */
    public final static long AUDIO_G726 = (1 << 4);
    /** ADPCM (IMA) */
    public final static long AUDIO_ADPCM = (1 << 5);
    /** 16-bit linear little-endian */
    public final static long AUDIO_SLINEAR = (1 << 6);
    /** LPC10, 172 (180?) samples/frame */
    public final static long AUDIO_LPC10 = (1 << 7);
    /** G.729A audio */
    public final static long AUDIO_G729A = (1 << 8);
    /** SpeeX Free Compression */
    public final static long AUDIO_SPEEX = (1 << 9) /** iLBC Free Compression */
            ;
    public final static long AUDIO_ILBC = (1 << 10);
    /** ADPCM (G.726, 32kbps, AAL2 codeword packing) */
    public final static long AUDIO_G726_AAL2 = (1 << 11);
    /** G.722 */
    public final static long AUDIO_G722 = (1 << 12);
    /** G.722.1 (also known as Siren7, 32kbps assumed) */
    public final static long AUDIO_SIREN7 = (1L << 13);
    /** G.722.1 Annex C (also known as Siren14, 48kbps assumed) */
    public final static long AUDIO_SIREN14 = (1L << 14);
    /** Raw 16-bit Signed Linear (16000 Hz) PCM */
    public final static long AUDIO_SLINEAR16 = (1L << 15);
    public final static long AUDIO_G719 = (1L << 32);
    /** SpeeX Wideband (16kHz) Free Compression */
    public final static long AUDIO_SPEEX16 = (1L << 33);
    /** silk Narrowband (16kHz) */
    public final static long AUDIO_SILK8 = (1L << 34);
    /** silk Wideband (16kHz) */
    public final static long AUDIO_SILK16 = (1L << 35);
    /*! Raw mu-law data (G.711) */
    public final static long AUDIO_TESTLAW = (1L << 47);
    /*! Reserved bit - do not use */
    public final static long AUDIO__RESERVED = (1L << 63);
    /** Maximum audio format */
    public final static long AUDIO_MAX = (1L << 63);
    /** Maximum audio mask */
    public final static long AUDIO_MASK = 0xFFFF0000FFFFL;
    public final static String[] CODECS = {
        "AUDIO_G723",
        "AUDIO_GSM",
        "AUDIO_ULAW",
        "AUDIO_ALAW",
        "AUDIO_G726",
        "AUDIO_ADPCM",
        "AUDIO_SLINEAR",
        "AUDIO_LPC10",
        "AUDIO_G729A",
        "AUDIO_SPEEX",
        "AUDIO_ILBC",
        "AUDIO_G726_AAL2",
        "AUDIO_G722",
        "AUDIO_SIREN7",
        "AUDIO_SIREN14",
        "AUDIO_SLINEAR16",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "UNKNOWN_FORMAT",
        "AUDIO_G719",
        "AUDIO_SPEEX16",
        "AUDIO_SILK8",
        "AUDIO_SILK16"
    };

    /**
     * Returns the optimum size (in bytes) of a frame. eg:
     * <ol>
     *  <li>33 bytes for GSM</li>
     *  <li>320 for SLIN</li>
     *  <li>160 for [ua]law</li>
     * </ol>
     *
     * @return The frame size in bytes
     */
    public int getFrameSize();

    /**
     * Returns the frame interval in milliseconds,
     * normally 20.
     *
     * @return The frame interval in milliseconds
     */
    public int getFrameInterval();

    /**
     * Returns "Media Format Subclass Values" of this Codec. This should be one 
     * of the constants above.
     *
     * @return The codec, as specified in the IAX RFC
     */
    public long getCodec();

    /**
     * Returns an instance of the Decoder class of this Codec.
     *
     * @return An instance of the appropriate Decoder class
     */
    public DecoderFace getDecoder();

    /**
     * Returns an instance of the Encoder class of this Codec.
     *
     * @return An instance of the appropriate Encoder class
     */
    public EncoderFace getEncoder();

    /**
     * Returns the name of the codec, for examlpe "SLIN".
     * 
     * @return The name of the codec.
     */
    public String getName();

    /**
     * Returns a string representation.
     *
     * @return The string, representing this object
     */
    public String toString();

    /**
     * Returns the sample rate, for example "8000.0F" for SLin.
     * @return The sample rate
     */
    public float getSampleRate();
}
