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

#import "PhonoPhone.h"
#import "PhonoCall.h"
#import "PhonoNative.h"
#import "PhonoXMPP.h"

@implementation PhonoPhone
@synthesize tones,onReady,onIncommingCall,headset,ringTone,ringbackTone,phono, xmppsessionID,currentCall;

- (PhonoCall *)dial:(PhonoCall *)dest{
    dest.phono = phono;
    [dest outbound];
    if (currentCall != nil){
        [self hangupCall:currentCall];
    }    
    currentCall = dest;
    [[phono pxmpp] dialCall:dest];
    return dest;
}
- (void) didReceiveIncommingCall:(PhonoCall *)call{
    if (onIncommingCall != nil) {
        // this should be on the main queue already.....
        onIncommingCall(call);
    } else {
        [self hangupCall:call];
    }
}

//	 When a call arrives via an incomingCall event, it can be answered by calling this function.

- (void) acceptCall:(PhonoCall *)incall{
    if (currentCall != nil){
        [self hangupCall:currentCall];
    }
    currentCall = incall;
    [[phono pxmpp] acceptInboundCall:incall];
}

// hangup a call
-(void) hangupCall:(PhonoCall *)acall{
    [[phono pxmpp] hangupCall:acall];
    currentCall = nil;
}

@end
