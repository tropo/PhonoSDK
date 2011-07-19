/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.android.audio;

import android.media.MediaPlayer;
import com.phono.rtp.Endpoint;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tim
 */
public class Play extends Endpoint {

    MediaPlayer _mp;

    public Play(String uri) throws Exception {
        super(uri);
        _mp = new MediaPlayer();

        _mp.setDataSource(uri);
        _mp.prepare();


    }

    public void stop() {
        if (_mp != null){
            _mp.stop();
        }
    }

    public void start() {
        if (_mp != null){
            _mp.start();
        }
    }

    public void gain(float val) {
        float fval = val / 100.0f;
        if (_mp != null){
            _mp.setVolume(fval, fval);
        }
    }
}
