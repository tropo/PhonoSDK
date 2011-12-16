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

#import "PhonoNative.h"
#import "PhonoPhone.h"
#import "PhonoMessaging.h"
#import "PhonoXMPP.h"
#import "DDLog.h"
#import "DDTTYLogger.h"
@implementation PhonoNative

@synthesize apiKey,onReady,onUnready,onError,sessionID,phone,audio, pxmpp;

- (id)init
{
    [DDLog addLogger:[DDTTYLogger sharedInstance]];

    PhonoPhone * ph = [[PhonoPhone alloc] init];
    PhonoMessaging * pm = [[PhonoMessaging alloc] init];
    self = [super init];
    if (self) {
        phone = ph;
        [phone setPhono:self];
        messaging = pm;
        [messaging setPhono:self];
    }    return self;
}

- (id)initWithPhone:(PhonoPhone *) ph
{
    PhonoMessaging * pm = [[PhonoMessaging alloc] init];
    self = [super init];
    if (self) {
        phone = ph;
        [phone setPhono:self];
        messaging = pm;
        [messaging setPhono:self];
    }    return self;
    return self;
}
- (id) initWithPhoneAndMessaging:(PhonoPhone *) phoneo messaging:(PhonoMessaging *) messagingo
{
    self = [super init];
    if (self) {
        phone = phoneo;
        [phone setPhono:self];
        messaging = messagingo;
        [messaging setPhono:self];
    }
    
    return self;
}

//Connects the Phone to the Voxeo Cloud.

-(void) connect{
    if (pxmpp == nil){
        pxmpp = [[PhonoXMPP alloc] initWithPhono:self];
    }
    if (pxmpp != nil){
        [pxmpp setupStream];
        [pxmpp connect:apiKey];
    }
}	  
-(void) disconnect{} //	 Disconnects the Phone from the Voxeo Cloud. 
- (BOOL) connected{
    return NO;
}

@end
