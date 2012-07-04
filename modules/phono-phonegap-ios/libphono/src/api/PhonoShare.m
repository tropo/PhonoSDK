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

#import "PhonoShare.h"
#import "phonoSRTP.h"



@implementation PhonoShare
@synthesize nearUri ,endpoint, codec,audio, srtpType,masterKeyL, masterKeyR ;


- (id) initWithUri:(NSString *) uri{
    BOOL ok;
    self = [super init];
    
    // all we do here is parse the URI - leave the rest to start - 
    NSArray *parts;
    NSString *host;
    NSInteger port;
    ok = [@"rtp://" isEqualToString:[uri substringToIndex:6]];
    if (ok) {
        parts = [[uri substringFromIndex:6] componentsSeparatedByString:@":"];
        ok = ([parts count] == 4);
    }
    if (ok) {
        host = [parts objectAtIndex:0]; // had better be an ipaddress 
        port = [[parts objectAtIndex:1] intValue];
        nearUri = [[NSString alloc] initWithFormat:@"rtp://%@:%d",host,port];
        rhost = [[NSString alloc] initWithString:[parts objectAtIndex:2]]; // had better be an ipaddress 
        rport = [[parts objectAtIndex:3] intValue];       
    }
    
    return self;
}
- (void) start{
    
    if (endpoint != nil){
        if (rtp == nil) {
            if(srtpType != nil){
                NSLog(@"Allocing an SRTP engine for %@ with %@ cypher suite",nearUri,srtpType);  
                rtp = [[phonoSRTP alloc] initWithTypeAndKey:srtpType keyL:masterKeyL keyR:masterKeyR ];
            } else {
                NSLog(@"Allocing an RTP engine for %@ ",nearUri);  
                rtp = [[phonoRTP alloc] init];
            }
            [rtp setFarHost:rhost];
            [rtp setFarPort:rport];
            [rtp start:[endpoint sock]];
        } else {
            NSLog(@"Already have an RTP engine for %@",nearUri);  
        }
        [audio setWireConsumer:rtp];
        [rtp setRtpds:audio];            
        [audio setCodec:codec]; // magically starts us up.
        [audio start];
    } else {
        NSLog(@"no endpoint for %@",nearUri);
    }
}
- (double) inEnergy{
    return (audio == nil) ? 0.0 : [audio inEnergy];
}
- (double) outEnergy{
    return (audio == nil) ? 0.0 : [audio outEnergy];
}
- (double) jitter{
    return (rtp == nil) ? 0.0 :[rtp jitter];
}
- (double) latency{
    return (rtp == nil) ? 0.0 :[rtp latency];
}

- (void) stop{
    [audio stop];
    [rtp stop];
    [endpoint close];
}


- (float) gain:(float) value{
    return value;
}



- (BOOL) mute:(BOOL)v{
    [audio mute:v];
    return v;
}

- (BOOL) doES:(BOOL)v{
    return v;
}

- (void) digit:(NSString*) digit duration:(int) duration audible:(BOOL)audible{
    [rtp digit:digit duration:duration audible:audible];
    if (audible) [audio digit:digit duration:duration];
}

- (void) destroy{
    [endpoint close];
}
@end
