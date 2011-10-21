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

package com.phono.api;

import com.phono.audio.AudioFace;
import com.phono.audio.codec.CodecFace;
import com.phono.srtplight.Log;
import java.util.Vector;

public class CodecList {

    private AudioFace _audio;
    private Codec[] _codecs;
    final public static int DTMFPAYLOADTTYPE = 101;
    private static long phonoPrefOrder[] = {
        /*CodecFace.AUDIO_SPEEX16,
        CodecFace.AUDIO_SPEEX,*/
        CodecFace.AUDIO_ULAW,
        CodecFace.AUDIO_ALAW,
        CodecFace.AUDIO_GSM,
        CodecFace.AUDIO_G722};

    public CodecList(AudioFace a) {
        _audio = a;
        populate();
    }

    void populate() {
        //long[] iaxcodecs = _audio.getCodecs();
        long [] iaxcodecs = phonoPrefOrder;
        Vector cv = new Vector();
        for (int i = 0; i < iaxcodecs.length; i++) {
            CodecFace c = _audio.getCodec(iaxcodecs[i]);
            if (c == null){
                Log.error("Codec "+iaxcodecs[i]+" missing");
                continue;
            }
            Integer pt = ptLookup(iaxcodecs[i]);
            if (pt != null) {
                Codec tc = null;
                // work within the RFC typo.....
                if (c.getCodec() == c.AUDIO_G722) {
                    tc = new Codec(pt.intValue(), "g722", 8000, c.getFrameInterval(), c.getCodec());
                } else {
                    String name = c.getName();
                    if (name.contentEquals("ULaw")) name = "PCMU";
                    if (name.contentEquals("Alaw")) name = "PCMA";
                    tc = new Codec(pt.intValue(), name, (int) c.getSampleRate(), c.getFrameInterval(), c.getCodec());
                }
                cv.addElement(tc);
            }
        }
        // 110 telephone-event/8000
        Codec te = new Codec(DTMFPAYLOADTTYPE, "telephone-event", 8000, 0, 0);
        cv.addElement(te);
        Object[] o = cv.toArray();
        _codecs = new Codec[o.length];
        for (int i = 0; i < _codecs.length; i++) {
            _codecs[i] = (Codec) o[i];
            Log.debug("will offer codec: "+_codecs[i].name+" pt="+_codecs[i].pt);
        }
    }

    private Integer ptLookup(long iaxcn) {
        Integer ret = null;
        if (iaxcn == CodecFace.AUDIO_SPEEX16) {
            ret = new Integer(103);
        }
        if (iaxcn == CodecFace.AUDIO_SPEEX) {
            ret = new Integer(102);
        }
        if (iaxcn == CodecFace.AUDIO_ULAW) {
            ret = new Integer(0);
        }
        if (iaxcn == CodecFace.AUDIO_ALAW) {
            ret = new Integer(8);
        }
        if (iaxcn == CodecFace.AUDIO_G722) {
            ret = new Integer(9);
        }
        if (iaxcn == CodecFace.AUDIO_GSM) {
            ret = new Integer(3);
        }
        return ret;
    }

    /**
     *
     * @param iaxcn
     * @return return the 'factor' used to convert between the iax timestamp (ms) and the rtp one
     *
     */
    public static int getFac(long iaxcn) {
        int ret = 8; // sensible default.
        if (iaxcn == CodecFace.AUDIO_SPEEX16) {
            ret = 16;
        }
        if (iaxcn == CodecFace.AUDIO_SPEEX) {
            ret = 8;
        }
        if (iaxcn == CodecFace.AUDIO_ULAW) {
            ret = 8;
        }
        if (iaxcn == CodecFace.AUDIO_ALAW) {
            ret = 8;
        }
        if (iaxcn == CodecFace.AUDIO_G722) {
            ret = 8; // I think - weird spec error here -
        }
        if (iaxcn == CodecFace.AUDIO_GSM) {
            ret = 8;
        }
        return ret;
    }

    public Codec[] getCodecs() {
        return _codecs;
    }
}
