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

package com.phono.applet.audio.phone;

import com.phono.rtp.Endpoint;
import com.phono.srtplight.Log;
import java.io.InputStream;
import java.net.URL;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import javazoom.jl.player.Player;

public class Play  implements Runnable {
    /*
    uri, {we trust the mime type if it is http ? thp} start(),
    stop(), volume(value) -> value
     *
     */

    final public String uri; // public property
    Player _play;
    Thread _playThread;
    private float _gain;

    public Play(String suburi) {
        InputStream in;
        AudioDevice dev;
        uri = suburi;
        _gain = (float) 0.5;

        try {
            dev = makeAudioDevice();
            //dev = FactoryRegistry.systemRegistry().createAudioDevice();
            in = new URL(uri).openStream();
            _play = new Player(in, dev);
            _playThread = new Thread(this);

        } catch (Exception ex) {
            Log.error(ex.toString());
        }
    }

    AudioDevice makeAudioDevice() throws JavaLayerException {
        final AudioDevice rdev = FactoryRegistry.systemRegistry().createAudioDevice();

        AudioDevice vad = new AudioDevice() {

            public void open(Decoder decoder) throws JavaLayerException {
                rdev.open(decoder);
            }

            public boolean isOpen() {
                return rdev.isOpen();
            }

            public void write(short[] samples, int offs, int len) throws JavaLayerException {
                scale(samples, offs, len);
                rdev.write(samples, offs, len);
            }

            public void close() {
                rdev.close();
            }

            public void flush() {
                rdev.flush();
            }

            public int getPosition() {
                return rdev.getPosition();
            }
        };
        return vad;
    }

   public int volume(int value) {
        _gain = (float) (value/100.0);
        return value; // for now
    }

    public int volume() {
        return (int)_gain*100;
    }

    private void scale(short[] samples, int offs, int len) {
        for (int i=offs;i < offs+len; i++){
            samples[i] = (short)(samples[i]*_gain);
        }
    }

    public void run() {
        try {
            _play.play();
        } catch (JavaLayerException ex) {
            Log.error(ex.toString());
        }
    }

    public void start() {
        _playThread.start();

    }

    public void stop() {
        _play.close();
    }


}
