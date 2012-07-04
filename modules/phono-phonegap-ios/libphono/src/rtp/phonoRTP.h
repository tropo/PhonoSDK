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
#import "AudioDataConsumer.h"
#import "PhonoAudio.h"

@interface phonoRTP : NSObject <AudioDataConsumer>  {


    PhonoAudio  *rtpds;
    /*  inbound state vars */
    uint64_t _sync;
    uint64_t _index;
    BOOL _first;
    int64_t _transit;
    double jitter;
    /* networky stuff bidriectional*/
	
	NSString *farHost;
    uint16_t farPort;
    CFSocketNativeHandle ipv4Soc;
    NSThread *rcvThread;
    NSThread *listen;
	BOOL _srtp;
    int _id;
    /* we don't support assymetric codec types (we could I suppose) so this is bi */
    int ptype;
    int codecfac;

    /* outbound state */
    uint64_t _seqno;
     uint64_t _csrcid;
     uint32_t _tailIn;
     uint32_t _tailOut;
    uint64_t ostamp;

    NSDate *firstSent;
    NSTimeInterval latency;
    

}
@property (nonatomic, retain) NSString *farHost;
@property (nonatomic, assign) uint16_t farPort;
@property (nonatomic, assign) int ptype;
@property (nonatomic, assign) int codecfac;
@property (readonly) double jitter;
@property (readonly) NSTimeInterval latency;
@property (nonatomic, assign) PhonoAudio  *rtpds;

- (BOOL)start: (CFSocketRef) s ;
- (void) stop;

- (void) protect:(uint8_t *)payload length:(int)paylen;
- (void) unprotect:(uint8_t *)payload length:(int)paylen;
- (void) sendPacket:(NSData *)data stamp:(uint64_t)stamp ptype:(int) ptype;
- (BOOL) sendPacket:(NSData *)data stamp:(uint64_t)stamp ptype:(int) ptype marker:(BOOL)marker;
- (void) deliverPayload:(uint8_t *)payload length:(int)paylen  stamp:(uint64_t) stamp ssrc:(uint32_t) ssrc;
- (void) updateCounters:(uint16_t) seqno ;
-(void) syncChanged:(uint64_t) sync ;
- (void) consumeAudioData:(NSData*)data time:(NSInteger)stamp;
- (void) digit:(NSString*) digit duration:(int) duration audible:(BOOL)audible;
@end
