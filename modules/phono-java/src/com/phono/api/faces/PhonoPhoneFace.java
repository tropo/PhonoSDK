/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.phono.api.faces;

import com.phono.api.exceptions.PhonoNativeException;
import java.util.Hashtable;

/**
 *
 * @author tim
 */
public interface PhonoPhoneFace {
/*
 *     PhonoNative *phono;
    BOOL tones;
    BOOL headset;
    NSString *ringTone;
    NSString *ringbackTone;
    void (^onError)(PhonoEventFace *);
    void (^onIncommingCall)(PhonoCallFace *);
    PhonoCallFace *currentCall;
 *
 *
 *
- (PhonoCallFace *)dial:(PhonoCallFace *)dest;
- (void) didReceiveIncommingCall:(PhonoCallFace *)call;
- (void) acceptCall:(PhonoCallFace *)call;
 //      When a call arrives via an incomingCall event, it can be answered by calling this function.
-(void) hangupCall:(PhonoCallFace *)call;
 */
    public String getRingTone();
    public void setRingTone(String rt);
    public String getRingbackTone();
    public void setRingbackTone(String rt);
    public void onError(PhonoEventFace e);
    public void onIncommingCall(PhonoCallFace c);

    /*
     * override this to return a call object of your choosing.
     */
    public PhonoCallFace newCall();

    public PhonoCallFace dial(String string, Hashtable headers);

}
