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

/**
 * DecoderFace
 *
 * This interface is to be implemented by the decoder instance of each codec.
 *
 * @see CodecFace
 * @see EncoderFace
 *
 */
public interface DecoderFace {

    /**
     * Decodes an (encoded) frame.
     *
     * @param encoded_signal The encoded frame(s)
     * @return The decoded frame
     */
    public short[] decode_frame(byte encoded_signal[]);


    /**
     * Try to magic up a lost frame from what we have
     * @param current_frame
     * @param next_frame
     * @return A made up frame, that is still encoded.
     */
    public byte[] lost_frame(byte current_frame[], byte next_frame[]);
}
