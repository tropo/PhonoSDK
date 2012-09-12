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

#import "PhonoCall.h"
#import "PhonoNative.h"
#import "PhonoPhone.h"
#import "PhonoAPI.h"
#import <Security/Security.h>


@implementation PhonoCall
@synthesize callId,onRing,onAnswer,onHangup,onError,state,hold,volume,gain,headers,phono,payload, candidate, to ,from, share, ringing,codecInd,srtpType,srtpKeyL,srtpKeyR,secure;

@dynamic mute;



- (void) setMute:(BOOL)mutev {
    mute = mutev;
    if (state == ACTIVE){
        [phono.papi mute:share value:mute];
    }
}
- (BOOL) getMute{
    return mute;
}

- (id)initInbound
{
    self = [super init];
    if (self) {
        headers = [[NSMutableDictionary alloc] init];
        state = PENDING;
    }
    
    return self;
}
- (void) outbound{
    [headers setObject:[phono sessionID] forKey:@"PhonoSessionID"];
}

- (id)initOutbound:(NSString *) user domain:(NSString *) domain
{
    self = [super init];
    if (self) {
        to = [[[NSString alloc ] initWithFormat:@"%@@%@" , [PhonoNative escapeString:user] , domain] retain];
        headers = [[NSMutableDictionary alloc] init];
        srtpType = @"AES_CM_128_HMAC_SHA1_80"; // always
        srtpKeyL = [[NSString alloc] initWithString:[PhonoAPI mkKey]];
    }
    
    return self;
}
- (id)initOutboundWithHeaders:(NSString *) user domain:(NSString *) domain headers:(NSMutableDictionary *)outhead
{
    self = [super init];
    if (self) {
        to = [[[NSString alloc ] initWithFormat:@"%@@%@" , [PhonoNative escapeString:user] , domain] retain];
        headers = outhead;
    }
    
    return self;
}

-(void) answer{
    [phono.phone acceptCall:self];
} //	 When a call arrives via an incomingCall event, it can be answered by calling this function.
-(void) hangup{
    [phono.phone hangupCall:self];
    state = ENDED;
} //	 Hangs up an active call.

-(void) digit:(NSString*)dtmf{
    if (state == ACTIVE){
        [phono.papi digit:share digit:dtmf duration:250 audible:YES];
    }
}
@end
