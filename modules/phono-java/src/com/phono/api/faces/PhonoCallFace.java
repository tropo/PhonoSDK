/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.phono.api.faces;

/**
 *
 * @author tim
 */
public interface PhonoCallFace {




public enum CallState {
NEW,
PENDING,
ACTIVE,
ENDED
};

public void onRing();
public void onAnswer();
public void onHangup();
public void onError();

public CallState getState();

public void setHeader(String name, String value);
public void setHold(boolean h);
public void setSecure(boolean s);
public void setMute(boolean m);
public boolean isRinging();
public void setVolume(int v);
public void setGain(int v);
public void digit(Character character);
public void hangup();
public void answer();


}
