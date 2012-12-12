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
#import "PhonoCall.h"
#import "PhonoPhone.h"
#import "PhonoMessage.h"
#import "PhonoMessaging.h"


@class PhonoXMPP;
@class PhonoAPI;

@interface PhonoNative : NSObject {
    NSString *apiKey;
    void (^onReady)();
    void (^onUnready)();
    void (^onError)();
    NSString *sessionID;
    NSString *myJID;
    NSString *gateway;
    NSString *provisioningURL;

    PhonoPhone *phone;
    PhonoMessaging *messaging;
    NSDictionary *audio;
    PhonoXMPP *pxmpp;
    PhonoAPI *papi;
}
@property(readonly) PhonoXMPP *pxmpp;
@property(readonly) PhonoAPI *papi;

@property(readwrite, copy) NSString *apiKey;
@property(nonatomic, copy) void (^onReady)();
@property(nonatomic, copy) void (^onUnready)();
@property(nonatomic, copy) void (^onError)();
@property(readwrite, copy) NSString *sessionID;
@property(readwrite, copy) NSString *provisioningURL;
@property(readwrite, copy) NSString *gateway;

@property(readwrite, copy) NSString *myJID;
@property(readonly) PhonoPhone *phone;
@property(readonly) PhonoMessaging *messaging;

@property(assign,getter=getAudio) NSDictionary *audio;

- (void)connect;	 //Connects the Phone to the Voxeo Cloud. 
- (void)disconnect; //	 Disconnects the Phone from the Voxeo Cloud. 
- (BOOL) connected;
- (id) initWithPhone:(PhonoPhone *) phone;
- (id) initWithPhoneAndMessaging:(PhonoPhone *) phone messaging:(PhonoMessaging *) messaging;
- (void) setUseSpeaker:(BOOL)use;
- (NSDictionary *) guessCodecPrefs;
- (NSDictionary *) lowBWPrefs;

+ (NSString *) unescapeString:(NSString *) esc;
+ (NSString *) escapeString:(NSString *)une;

@end
