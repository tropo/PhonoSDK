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
        super.onCreate(savedInstanceState);
        super.loadUrl("file:///android_asset/www/mobile.html");
	AudioManager aM = (AudioManager) getSystemService(AUDIO_SERVICE);
	int mode = (android.os.Build.VERSION.SDK_INT >= 11)?3:aM.MODE_IN_CALL ;
        aM.setMode(mode);
	aM.setBluetoothScoOn(false);
	aM.setBluetoothA2dpOn(false);
	aM.setWiredHeadsetOn(false);
	aM.setSpeakerphoneOn(true);

    }
}

