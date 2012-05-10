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
        super.loadUrl("file:///android_asset/www/mobile.html?dial=app:9996160714&connectionUrl=http://panda-dev1-ext.qa.voxeolabs.net:8080/prism_bosh&gateway=gw-v4.d.phono.com");

    }
}

