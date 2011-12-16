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
@class PhonoNative;
@interface PhonoCall : NSObject
{
    NSString *destination;
    NSMutableDictionary *headers;
    NSString *callId;
    NSString *state;
    BOOL hold;
    BOOL mute;
    int volume;
    int gain;
    PhonoNative *phono;
    void (^onRing)();
    void (^onAnswer)();
    void (^onHangup)();
    void (^onError)();
}
@property(readwrite, retain) PhonoNative *phono;
@property(readwrite, copy) NSString *callId;
@property(nonatomic, copy) void (^onRing)();
@property(nonatomic, copy) void (^onAnswer)();
@property(nonatomic, copy) void (^onHangup)();
@property(nonatomic, copy) void (^onError)();
@property(readonly, copy) NSString *state;
@property(readonly, copy) NSMutableDictionary *headers;
@property(readwrite) BOOL hold;
@property(readwrite) BOOL mute;
@property(readwrite) int volume;
@property(readwrite) int gain;


- (id)initOutbound:(NSString *) dest;
- (id)initOutboundWithHeaders:(NSString *) dest headers:(NSDictionary *)outhead;

-(void) answer; //	 When a call arrives via an incomingCall event, it can be answered by calling this function.
-(void) hangup; //	 Hangs up an active call.
-(void) digit:(NSString*)dtmf;
- (void) outbound;

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
