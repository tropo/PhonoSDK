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
#import <Foundation/Foundation.h>
@class PhonoEvent;
@class PhonoCall;
@class PhonoNative;
@interface PhonoPhone : NSObject {
    NSString *xmppsessionID;
    PhonoNative *phono;
    BOOL tones;
    BOOL headset;
    NSString *ringTone;
    NSString *ringbackTone;
    void (^onError)(PhonoEvent *);
    void (^onIncommingCall)(PhonoCall *);
    PhonoCall *currentCall;
}

@property(readwrite) BOOL tones;
@property(nonatomic, copy) void (^onReady)(PhonoEvent*);
@property(nonatomic, copy) void (^onIncommingCall)(PhonoCall *);
@property(readwrite) BOOL headset;
@property(readwrite, copy) NSString *xmppsessionID;
@property(readwrite, copy) NSString *ringTone;
@property(readwrite, assign) PhonoNative *phono;
@property(readwrite, assign) PhonoCall *currentCall;

@property(readwrite, copy) NSString *ringbackTone;
- (PhonoCall *)dial:(PhonoCall *)dest;
- (void) didReceiveIncommingCall:(PhonoCall *)call;
- (void) acceptCall:(PhonoCall *)call;
 //	 When a call arrives via an incomingCall event, it can be answered by calling this function.
-(void) hangupCall:(PhonoCall *)call;

/*
dial(destination, config)	 Used to make an outgoing call. This function takes two arguments: the destination, and a config object with additional parameters.

var call = phone.dial("774-271-7100", {
volume: 80,
pushToTalk:true,
headers: [
          {
          name:"x-foo",
          value:"bar"
          },
          {
          name:"x-bling",
          value:"baz"
          }
          ]
});

tones(boolean)	 Gets/Sets whether digit tones should be played on the client.
headset(boolean)	 When headset is false, Phono will optimize the user experience for your computers' speakers and supress echo.
ringTone(string)	 Gets/Sets an external MP3 file to play when receiving an incoming call. This audio file will start playing as soon as the call arrives and will stop when it's either answered or rejected by calling the Call.hangup() function.
ringbackTone(string)	 Gets/Sets an external MP3 file to play when placing an outgoing call. This audio file will start playing as soon as the call is dialed and will stop when the call is canceled by invoking Call.hangup(), is answered by the far end, or otherwise fails to complete.
 
 
 incomingCall	 Dispatched when a new inbound call has arrived.
 error	 Dispatched when an error is encountered. 
 
 */
@end
