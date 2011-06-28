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

package com.phono.audio.codec.g722;

import com.phono.audio.codec.*;

public class G722Codec implements CodecFace{

    public int getFrameSize() {
        return 160;
    }

    public int getFrameInterval() {
        return 20;
    }

    public long getCodec() {
        return G722Codec.AUDIO_G722;
    }

    public DecoderFace getDecoder() {
        return new G722Decoder(16*getFrameInterval());
    }

    public EncoderFace getEncoder() {
        return new G722Encoder(this.getFrameSize()); // thats in shorts..
    }

    static short saturate(int amp) {
        short amp16;

        /* Hopefully this is optimised for the common case - not clipping */
        amp16 = (short) amp;
        if (amp == amp16) {
            return amp16;
        }
        if (amp > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        return Short.MIN_VALUE;
    }

    public float getSampleRate() {
        return 16000.0F;
    }

    public String getName() {
        return "G.722";
    }
}
