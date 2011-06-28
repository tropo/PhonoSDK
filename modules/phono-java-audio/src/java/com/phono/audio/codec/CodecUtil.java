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
 * CodecUtil
 *
 */

public class CodecUtil {
    
    /** Creates a new instance of CodecUtil */
    public CodecUtil() {
    }
    
    
    public static short[] bytesToShorts(byte byteBuffer[]) {
        int len = byteBuffer.length/2;
        short[] output = new short[len];
        int j = 0;

        for (int i = 0; i < len; i++) {
            output[i] = (short) (byteBuffer[j++] << 8);
            output[i] |= (byteBuffer[j++] & 0xff);

//            output[i] = (short) (byteBuffer[j++] << 8);
//            output[i] += ((0x7f &byteBuffer[j]) + (byteBuffer[j] < 0 ? 128 : 0));
//            j++;
            
        }
        return output;
    }


    public static byte[] shortsToBytes(short shortBuffer[]) {
        int len = shortBuffer.length;
        byte[] output = new byte[len*2];
        int j = 0;

        for (int i = 0; i < len; i++) {
            output[j++] = (byte) (shortBuffer[i] >>> 8);
            output[j++] = (byte) (0xff & shortBuffer[i]);
        }
        return output;
    }
}
