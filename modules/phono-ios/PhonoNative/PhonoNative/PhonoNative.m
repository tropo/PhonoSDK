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
#import "PhonoAPI.h"

@implementation PhonoNative

@synthesize apiKey,onReady,onUnready,onError,sessionID,phone, pxmpp, papi, messaging, myJID, gateway, provisioningURL;

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
    }    
    return self;
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

- (void) setAudio:(NSDictionary *)naudio{
    audio = naudio;
    if ((phone.currentCall == nil) || ( phone.currentCall.state != PENDING)){
        NSMutableString *filt = [[NSMutableString alloc] initWithString:@"["];
        NSEnumerator * e = [audio keyEnumerator];
        NSString *k = nil;
        BOOL first = YES;
        while (nil != (k = [e nextObject])){
            if ([k characterAtIndex:0] == '@'){
                if (first){
                    first = NO;
                } else {
                    [filt appendString:@" and "];
                }
                NSString * v = [ audio objectForKey:k];
                [filt appendFormat:@"%@=\"%@\"",k,v];
            }
        }
        [filt appendString:@"]"];
        NSLog(@"setting Attribute filter to %@",filt);
        [pxmpp.xmppJingle setPayloadAttrFilter:filt];
    } else {
        NSLog(@"ignore bandwidth changes while PENDING");
    }
}

- (NSDictionary *) getAudio{
    return audio;
}

//Connects the Phone to the Voxeo Cloud.

-(void) connect{
    if (papi == nil){
        papi = [[PhonoAPI alloc] init];
    }
    if (pxmpp == nil){
        pxmpp = [[PhonoXMPP alloc] initWithPhono:self];
    }
    if (pxmpp != nil){
        if (gateway != nil){
            [pxmpp setupStreamWithGateway:gateway];
        } else {
            [pxmpp setupStream];
        }
        [pxmpp connect:apiKey];
    }

}	  
-(void) disconnect{
    if (pxmpp != nil){
        [pxmpp disconnect];
    }
} //	 Disconnects the Phone from the Voxeo Cloud. 
- (BOOL) connected{
    return (pxmpp == nil) ? NO:[[pxmpp xmppStream] isConnected];
}

/* need to implement cmap ---
 
 ' '	\20 *
 "	\22
 &	\26
 '	\27
 /	\2f
 :	\3a
 <	\3c
 >	\3e
 @	\40
 \	\5c */

+ (NSString *) unescapeString:(NSString *) esc {
    NSString *js = [esc stringByReplacingOccurrencesOfString:@"\\20" withString:@" "];
    js = [js stringByReplacingOccurrencesOfString:@"\\22" withString:@"\""];
    js = [js stringByReplacingOccurrencesOfString:@"\\26" withString:@"&"];
    js = [js stringByReplacingOccurrencesOfString:@"\\27" withString:@"'"];
    js = [js stringByReplacingOccurrencesOfString:@"\\2f" withString:@"/"];
    js = [js stringByReplacingOccurrencesOfString:@"\\3a" withString:@":"];
    js = [js stringByReplacingOccurrencesOfString:@"\\3c" withString:@"<"];
    js = [js stringByReplacingOccurrencesOfString:@"\\3e" withString:@">"];
    js = [js stringByReplacingOccurrencesOfString:@"\\40" withString:@"@"];
    js = [js stringByReplacingOccurrencesOfString:@"\\5c" withString:@"\\"];
    
    return js;
}
+ (NSString *) escapeString:(NSString *)une{ 
    NSString *js = [une stringByReplacingOccurrencesOfString:@" " withString:@"\\20"];
    js = [js stringByReplacingOccurrencesOfString:@"\"" withString:@"\\22"];
    js = [js stringByReplacingOccurrencesOfString:@"&" withString:@"\\26"];
    js = [js stringByReplacingOccurrencesOfString:@"'" withString:@"\\27"];
    js = [js stringByReplacingOccurrencesOfString:@"/" withString:@"\\2f"];
    js = [js stringByReplacingOccurrencesOfString:@":" withString:@"\\3a"];
    js = [js stringByReplacingOccurrencesOfString:@"<" withString:@"\\3c"];
    js = [js stringByReplacingOccurrencesOfString:@">" withString:@"\\3e"];
    js = [js stringByReplacingOccurrencesOfString:@"@" withString:@"\\40"];
    js = [js stringByReplacingOccurrencesOfString:@">" withString:@"\\5c"];
    return js;
}
- (void) setUseSpeaker:(BOOL)use{
    if (papi != nil){
        [papi setUseSpeakerForCall:use];
    }
}

// some possible codec configs....

- (NSDictionary *) lowBWPrefs{
    NSDictionary * ret = [[NSDictionary alloc] initWithObjectsAndKeys:@"SPEEX",@"@name",@"8000",@"@clockrate", nil];
    return ret;
}

- (NSDictionary *) hiBWPrefs{
    NSDictionary * ret = [[NSDictionary alloc] initWithObjectsAndKeys:@"SPEEX",@"@name",@"16000",@"@clockrate", nil];
    return ret;
}

- (NSDictionary *) telBWPrefs{
    NSDictionary * ret = [[NSDictionary alloc] initWithObjectsAndKeys:@"ULAW",@"@name",@"8000",@"@clockrate", nil];
    return ret;
}

- (NSDictionary *) ulBWPrefs{
    NSDictionary * ret = [[NSDictionary alloc] initWithObjectsAndKeys:@"G722",@"@name",@"8000",@"@clockrate", nil];
    return ret;
}

- (NSDictionary *) guessCodecPrefs{
    NSDictionary *ret = nil;
    NSTimeInterval rtt = 0.5;
    // edge gave me 570 ms RTT
    // wifi 150 ms
    // 3g is 300ms
    
    if (pxmpp != nil) {
        rtt = [pxmpp rtt];
    }
    if (rtt < 0.1) {
        ret = [self ulBWPrefs];
    } else if ( rtt < 0.3) {
        ret = [self hiBWPrefs];
    } else if (rtt < 0.6) {
        ret = [self lowBWPrefs];
    }
    return ret;
}

@end
