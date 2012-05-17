package com.phono.android.phonegap;

import android.app.Activity;
import android.os.Bundle;
import android.media.AudioManager;
import org.apache.cordova.*;

public class DefaultCordovaActivity extends DroidGap
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        AudioManager audiomanager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audiomanager.setSpeakerphoneOn(true);
        super.onCreate(savedInstanceState);
        super.loadUrl("file:///android_asset/www/mobile.html");

    }
}

