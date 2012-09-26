/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.api.faces;

/**
 *
 * @author tim
 */
public interface PhonoNativeFace {

    public void onReady();

    public void onUnready();

    public void onError();

    String getApiKey();

    PhonoMessagingFace getMessaging();

    PhonoPhoneFace getPhone();

    String getSessionID();

    void setApiKey(String k);

    void setMessaging(PhonoMessagingFace m);

    void setPhone(PhonoPhoneFace p);
    void connect();         //Connects the Phone to the Voxeo Cloud.
    void disconnect(); //   Disconnects the Phone from the Voxeo Cloud.
    boolean isConnected();
}
