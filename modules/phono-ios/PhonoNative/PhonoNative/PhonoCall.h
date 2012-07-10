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
#define NEW (0)
#define PENDING (1)
#define ACTIVE (2)
#define ENDED (3)

@class PhonoEvent;
@class PhonoNative;
@interface PhonoCall : NSObject
{
    NSString *share;
    NSString *from;
    NSString *to;
    NSMutableDictionary *headers;
    NSString *callId;
    NSString *codecInd;
    NSString *srtpKeyL;
    NSString *srtpKeyR;
    NSString *srtpType;


    NSInteger state;
    BOOL hold;
    BOOL mute;
    BOOL ringing;
    BOOL secure;
    int volume;
    int gain;
    id candidate; // 
    id payload; // opaque types for the xmpp engine to use
    PhonoNative *phono;
    void (^onRing)();
    void (^onAnswer)();
    void (^onHangup)();
    void (^onError)();
}
@property(readwrite, retain) PhonoNative *phono;
@property(readwrite, copy) id  candidate;
@property(readwrite, copy) id  payload;
@property(readwrite, copy) NSString *callId;
@property(readwrite, copy) NSString *from;
@property(readwrite, copy) NSString *to;
@property(readwrite, copy) NSString *share;
@property(readwrite, copy) NSString *codecInd;
@property(readwrite, copy) NSString *srtpKeyL;
@property(readwrite, copy) NSString *srtpKeyR;
@property(readwrite, copy) NSString *srtpType;


@property(nonatomic, copy) void (^onRing)();
@property(nonatomic, copy) void (^onAnswer)();
@property(nonatomic, copy) void (^onHangup)();
@property(nonatomic, copy) void (^onError)();
@property(readwrite, atomic) NSInteger state;

@property(readonly, copy) NSMutableDictionary *headers;
@property(readwrite) BOOL hold;
@property(readwrite) BOOL secure;
@property(readwrite) BOOL mute;
@property(readwrite) BOOL ringing;
@property(readwrite) int volume;
@property(readwrite) int gain;

- (id)initInbound;
- (id)initOutbound:(NSString *)user domain:(NSString *) domain ;
- (id)initOutboundWithHeaders:(NSString *)user domain:(NSString *) domain headers:(NSDictionary *)outhead;

-(void) answer; //	 When a call arrives via an incomingCall event, it can be answered by calling this function.
-(void) hangup; //	 Hangs up an active call.
-(void) digit:(NSString*)dtmf;
-(void) outbound;

/*
 answer()	 When a call arrives via an incomingCall event, it can be answered by calling this function.
 hangup()	 Hangs up an active call.
 digit(string)	 Sends a touch-tone signal to the remote party. This is equivalent to pressing a key on a touch tone phone.
 mute(boolean)	 Stops sending audio to the remote party
 hold(boolean)	 Mutes the microphone and stops sending audio to the remote party. Useful if implementing "call waiting" in your Phone.
 volume(int)	 Sets the volume of the call
 gain(int)	 Sets the microphone gain for the call
 Properties
 id	 Returns the unique identifier for this Call.
 state	 Returns the current state of the Call. Possible values are: connected, ringing, disconnected, progress, initial.
 Events
 ring	 Dispatched when an outbound call is ringing.
 answer	 Dispatched when an outbound call is answered.
 hangup	 Dispatched when a call is terminated by the remote party.
 error	 Dispatched when an error is reported relating to this call.
 */
@end
