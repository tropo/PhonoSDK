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
#import "PhonoXMPP.h"

@implementation PhonoCall
@synthesize callId,onRing,onAnswer,onHangup,onError,state,hold,mute,volume,gain,headers,phono;


- (id)initInbound
{
    self = [super init];
    if (self) {
        headers = [[NSMutableDictionary alloc] init];
    }
    
    return self;
}
- (void) outbound{
    [headers setObject:[phono sessionID] forKey:@"PhonoSessionID"];
    [[phono pxmpp] jingleSessionInit];
}
- (id)initOutbound:(NSString *) dest
{
    self = [super init];
    if (self) {
        destination = dest;
        headers = [[NSMutableDictionary alloc] init];
    }
    
    return self;
}
- (id)initOutboundWithHeaders:(NSString *) dest headers:(NSMutableDictionary *)outhead
{
    self = [super init];
    if (self) {
        destination = dest;
        headers = outhead;
    }
    
    return self;
}

-(void) answer{} //	 When a call arrives via an incomingCall event, it can be answered by calling this function.
-(void) hangup{} //	 Hangs up an active call.
-(void) digit:(NSString*)dtmf{}
@end
