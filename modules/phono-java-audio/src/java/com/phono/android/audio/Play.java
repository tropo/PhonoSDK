/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.android.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import com.phonegap.api.PhonegapActivity;
import com.phono.rtp.Endpoint;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tim
 */
public class Play extends Endpoint implements OnPreparedListener {

    MediaPlayer _mp;
    String _uri;
    boolean _prepared = false;
    final static String FILE = "file://";

    public Play(String uri, PhonegapActivity ctx) throws Exception {
        super(uri);
        _uri = uri;
        _mp = new MediaPlayer();
        if (uri.startsWith(FILE)) {
            _mp.setDataSource(uri);
            _mp.prepare();
            _prepared = true;
        } else {
            _mp.setDataSource(uri);
            _mp.setOnPreparedListener(this);
            _mp.prepareAsync();
        }


    }

    public void stop() {
        if (_mp != null) {
            _mp.stop();
        }
    }

    public void start() {
        if (_mp != null) {
            if (_prepared) {
                _mp.start();
            } else {
                Log.debug("not ready to play " + _uri);
            }
        }
    }

    public void volume(float val) {
        float fval = val / 100.0f;
        if (_mp != null) {
            _mp.setVolume(fval, fval);
        }
    }

    public void onPrepared(MediaPlayer mp) {
        Log.debug("prepared " + _uri);
        _prepared = true;
    }
}
