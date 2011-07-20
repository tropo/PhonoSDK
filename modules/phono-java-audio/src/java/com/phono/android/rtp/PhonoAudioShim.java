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

package com.phono.android.rtp;

import android.media.MediaPlayer;
import com.phono.android.audio.AndroidAudio;
import com.phono.audio.AudioException;
import com.phono.android.audio.Log;
import com.phono.audio.codec.CodecFace;
import com.phono.codecs.speex.SpeexCodec;

//import com.phono.dsp.EchoCanceler;




public class PhonoAudioShim extends AndroidAudio {

    int _outstamp=0;

    private boolean _recstarted;
    // Files for debug output

    boolean debugAudio = false; // Write samples to local disk
    int cutterTime = 0; // Don't cut for this long
    int dropMicSamples = 48;
    double _oMicGain = 1.0;



    @Override
    public void init(long codec, int latency) throws AudioException {
        // Called at the start of every new share
        super.init(codec, latency);


    }

    public CodecFace getCodec(long l) {
        return this._codecMap.get(new Long(l));
    }

    @Override
    protected void fillCodecMap() {
        /*SpeexCodec sc = new SpeexCodec(false);
        SpeexCodec sc16 = new SpeexCodec(true);
*/

        super.fillCodecMap();
        
        /*_codecMap.put(new Long(sc16.getCodec()), sc16);
        _codecMap.put(new Long(sc.getCodec()), sc);
        _defaultCodec = sc;
*/
    }

    // make the timestamps tidy
    @Override
    public int getOutboundTimestamp() {
        _outstamp+=20;
        return _outstamp ;
    }

    @Override
    public void startRec(){
        // weird faffing about here because in IAX rec is started on
        // first recieved frame
        // with RTP it is the other way around.
        if (!_recstarted){
            Log.debug("Start Rec called ");
            super.startRec();
            _recstarted = true;
        } 
        
    }

    @Override
    public void stopRec(){
        _recstarted = false;
        super.stopRec();
    }

    public void setVolume(double d) {
        float fvol = (float) (d/100.0);
        this.getAudioTrack().setStereoVolume(fvol, fvol);
    }

    public void muteMic(boolean v) {
        if (v){
            _oMicGain = super.getMicGain();
            super.setMicGain(0.0f);
        } else {
            super.setMicGain((float)_oMicGain);
        }
        
    }

    public boolean callHasECon() {
        return false;
    }

    public MediaPlayer playDigit(char c) {
        Log.debug("Beep dtmf "+c);

        Log.error("No dtmf playback yet.");
        return null; // todo
    }






}
