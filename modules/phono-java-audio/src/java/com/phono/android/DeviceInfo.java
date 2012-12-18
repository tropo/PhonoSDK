
package com.phono.android;

import com.phono.api.DeviceInfoFace;


public class DeviceInfo implements DeviceInfoFace{

    public String getName() {
        return android.os.Build.USER;
    }

    public String getSystemName() {
        return android.os.Build.MODEL;
    }

    public String getSystemVersion() {
        return android.os.Build.DISPLAY;
    }

    public String getPhonoPlatform() {
        return "android-native";
    }
}
