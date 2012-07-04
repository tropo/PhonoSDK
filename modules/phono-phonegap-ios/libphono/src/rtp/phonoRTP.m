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

#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#import "phonoRTP.h"
#import "AVFoundation/AVAudioPlayer.h"
#import <Security/Security.h>

#define DTMFPAYLOADTTYPE (101)

#define RTPHEAD (12)
#define RTPVER  (2)
@implementation phonoRTP
@synthesize farHost , farPort, rtpds, ptype ,codecfac, jitter, latency;

- (id) init{
	self =  [super init];
	_sync = 0;
	_srtp = NO;
    SecRandomCopyBytes (kSecRandomDefault,sizeof(_csrcid),(uint8_t *)&_csrcid);
	return self;
}

static uint64_t get4ByteInt(uint8_t  *b, int offs) {
	return ((b[offs] << 24) | (b[offs+1] << 16) | (b[offs+2] << 8) | (0xff & b[offs+3]));
}

/*
 * @param output The output array to set the bit
 * @param bitno The position of the bit (in output)
 */
static void setBit(uint8_t *output, int bitno) {
	// bit 0 is on the left hand side, if bit 0 should be set to '1'
	// this would show as: 1000 0000 = 0x80
	
	// each byte is 8 bits
	int index = bitno / 8;
	int index_bitno = bitno % 8;
	
	// shift the '1' into the right place
	// shift with zero extension
	uint8_t mask = (uint8_t) (0x80 >> index_bitno);
	
	// OR the bit into the byte, so the other bits remain
	// undisturbed.
	output[index] |= mask;
}

/**
 * Copies a number of bits from input to output.
 * Copy bits from left to right (MSB - LSB).
 *
 * @param input The input value to read from
 * @param in_noLSB The number of LSB in input to copy
 * @param output The output array to copy the bits to
 * @param out_pos The start position in output
 * @return the updated out_pos
 */
static int copyBitsO(int input, int in_noLSB, uint8_t *output,
                     int out_pos) {
	int res;
	int value = input;
	int i;
	
	// start with the left most bit I've got to copy over:
	uint32_t mask = 0x1 << (in_noLSB -1);
	
	for (i=0; i<in_noLSB; i++) {
		// see if the that bit is one or zero
		res = (value & mask);
		if (res > 0) {
			setBit(output, out_pos);
		}
		
		// shift the mask to the next position
		// shift with zero extension
		mask = mask >> 1;
		out_pos++;
	}
	return out_pos;
}



/**
 * Returns zero or one.
 *
 * @param input The input array to read from
 * @param bitno The position of the bit (in input)
 * @return one or zero
 */
static int getBit(uint8_t *input, int bitno){
	// bit 0 is on the left hand side, if bit 0 should be set to '1'
	// this would show as: 1000 0000 = 0x80
	
	// each byte is 8 bits
	int index = bitno / 8;
	int index_bitno = bitno % 8;
	
	uint8_t onebyte = input[index];
	
	// shift the '1' into the right place
	// shift with zero extension
	uint8_t mask = (uint8_t) (0x80 >> index_bitno);
	
	// mask (AND) it so see if the bit is one or zero
	int res = (onebyte & mask);
	if (res < 0) {
		// it can be negative when testing the signed bit (bit zero
		// in this case)
		res = 1;
	}
	return res;
}


/**
 * Copy a number of bits from input into a short.
 * Copy bits from left to right (MSB - LSB).
 *
 * @param input The input array to read from
 * @param in_pos The position of the bit (in input) to start from
 * @param no_bits The number of bits to copy
 * @return The new value as a short
 */
static uint16_t copyBitsI(uint8_t *input, int in_pos,
                          int no_bits) {
	// LSB is on the right hand side
	uint16_t out_value = 0;
	int b;
	
	// start with the left most bit I've got to copy into:
	uint32_t out_value_mask = 0x1 << (no_bits -1);
	
	uint32_t myBit;
	for (b=0; b<no_bits; b++) {
		myBit = getBit(input, in_pos);
		if (myBit > 0) {
			// OR the bit into place, so the other bits remain
			// undisturbed.
			out_value |= out_value_mask;
		}
		
		// move to the next bit of input
		in_pos++;
		
		// get ready for the next bit of output
		// shift with zero extension
		out_value_mask = (uint16_t) (out_value_mask >> 1);
	}
	return out_value;
}

- (void) protect:(uint8_t *)payload length:(int)paylen{
    
}
- (void) unprotect:(uint8_t *)payload length:(int)paylen{
    
}
- (void) sendPacket:(NSData *)data stamp:(uint64_t)stamp ptype:(int) lptype{
    [self sendPacket:data stamp:stamp ptype:lptype marker:NO];
}
- (BOOL) sendPacket:(NSData *)data stamp:(uint64_t)stamp ptype:(int) lptype marker:(BOOL)marker{
    int i;
    int dlen = [data length];
    uint8_t *datp = (uint8_t *)[data bytes];
    int payload_length = RTPHEAD + dlen + _tailOut;
    uint8_t *payload = alloca(payload_length); // assume no pad and no cssrcs
    memset(payload,0,payload_length);
    copyBitsO(RTPVER, 2, payload, 0); // version
    // skip pad
    // skip X
    // skip cc
    // skip M
    // all the above are zero.
    if (marker) {
        copyBitsO(1, 1, payload, 8);
    }
    copyBitsO(lptype, 7, payload, 9);
    payload[2] = (uint8_t) (_seqno >> 8);
    payload[3] = (uint8_t) _seqno;
    payload[4] = (uint8_t) (stamp >> 24);
    payload[5] = (uint8_t) (stamp >> 16);
    payload[6] = (uint8_t) (stamp >> 8);
    payload[7] = (uint8_t) stamp;
    payload[8] = (uint8_t) (_csrcid >> 24);
    payload[9] = (uint8_t) (_csrcid >> 16);
    payload[10] = (uint8_t) (_csrcid >> 8);
    payload[11] = (uint8_t) _csrcid;
    for (i = 0; i < dlen ; i++) {
        payload[i + RTPHEAD] = datp[i];
    }
    [self protect:payload length:payload_length];
    int e = send(ipv4Soc,payload,payload_length,0);
    if (firstSent == nil) {
        firstSent = [[NSDate alloc] init];
        latency = 0.0;
    }
    //NSLog(@"sending frame of %d ",[nsd length]) ;
    
    _seqno++;
    ostamp = stamp; // hold onto a current ish stamp
    //NSLog(@"sending RTP %d packet length %d to %@:%d" , lptype , payload_length, farHost, farPort);
    return (e ==payload_length)?YES:NO  ;
}

- (void) consumeAudioData:(NSData*)data time:(NSInteger)stamp{
    uint64_t samplecnt = stamp * codecfac;
    [self sendPacket:data stamp:samplecnt ptype:ptype];
}



- (uint32_t) getIndex: (uint16_t) seqno {
    return seqno; // wrong wrong wrong  - todo
}
- (void) parsePacket:(NSMutableData *)dp {
    
    // parse RTP header (if we care .....)
    /*
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                           timestamp                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |           synchronization source (SSRC) identifier            |
     * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
     * |            contributing source (CSRC) identifiers             |
     * |                             ....                              |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     */
    uint8_t *packet = ( uint8_t *)[dp bytes];
    uint8_t * payload;
    int plen = [dp length];
    
    int ver = 0;
    int pad = 0;
    int csrcn = 0;
    //int mark = 0;
    int rptype = 0;
    char seqno = 0;
    long stamp = 0;
    long tstamp = 0;
    uint32_t sync = 0;
    
    if (plen < 12) {
        NSLog(@"Packet too short. RTP must be >12 bytes");
        return;
    }
    ver = copyBitsI(packet, 0, 2);
    pad = copyBitsI(packet, 2, 1);
    csrcn = copyBitsI(packet, 4, 4);
    rptype = copyBitsI(packet, 9, 7);
    
    seqno = ((packet[2] << 8) + (packet[3]));
    stamp =get4ByteInt(packet, 4) ;
    sync = get4ByteInt(packet,8);
    if (plen < (RTPHEAD + 4 * csrcn)) {
        NSLog(@"Packet too short. CSRN = %d  but packet only %d" , csrcn ,  plen);
        return;
    }
    
    long csrc =0; // if we were ever to care about this we would need to be cleverer
    int offs = RTPHEAD;
    int i =0;
    for (i = 0; i < csrcn; i++) {
        /*
         csrc[i] = get4ByteInt(packet, offs);
         *
         */
        csrc = get4ByteInt(packet,offs);
        offs += 4;
    }
    int endhead = offs;
    // if padding set then last byte tells you how much to skip
    int paylen = (pad == 0) ? (plen - offs) : ((plen - offs) - (0xff) & packet[plen - 1]);
    // SRTP packets have a tail auth section and potentially an MKI

    // quick plausibility checks
    // should check the ip address etc - but actually we better trust the OS
    // since we have 'connected' this socket meaning _only_ correctly sourced packets seen here.
    if (ver != RTPVER) {
        NSLog(@"Only RTP version 2 supported");
    }
    if (rptype != ptype) {
        NSLog(@"Unexpected payload type %d" , rptype);
    }
    if (sync != _sync) {
        [self syncChanged:sync];
    }
    _index = [self getIndex:seqno];
    NSLog(@"ctxt is %8x",sync);
    [self unprotect:packet length:plen];
    paylen -= _tailIn;
    payload = alloca(paylen);
    int o = 0;
    while (offs - endhead < paylen) {
        payload[o++] = packet[offs++];
    }
    tstamp = 20*_index;
    [self deliverPayload:payload length:paylen stamp:tstamp ssrc:sync];
    
    
   // NSLog(@"got RTP %d length %d packet from %@:%d" , rptype , paylen , farHost, farPort);
    
}

- (void) updateStats:(uint64_t)stamp{
    NSTimeInterval clockms = [firstSent timeIntervalSinceNow] * -1000;
    uint64_t arrival = clockms;
    int64_t transit = arrival - stamp;
    int64_t d = transit - _transit;
    _transit = transit;
    if (d < 0) d = -d;
    
    jitter += (1./16.) * ((double)d - jitter);
    
    if (latency == 0.0){
        latency = clockms;
        NSLog(@"latency estimate = %6.2lf ms",latency);
    }    
    /* 
     The code fragments below implement the algorithm given in Section
     6.3.1 for calculating an estimate of the statistical variance of the
     RTP data interarrival time to be inserted in the interarrival jitter
     field of reception reports. The inputs are r->ts , the timestamp from
     the incoming packet, and arrival , the current time in the same
     units. Here s points to state for the source; s->transit holds the
     relative transit time for the previous packet, and s->jitter holds
     the estimated jitter. The jitter field of the reception report is
     measured in timestamp units and expressed as an unsigned integer, but
     the jitter estimate is kept in a floating point. As each data packet
     arrives, the jitter estimate is updated:

     int transit = arrival - r->ts;
     int d = transit - s->transit;
     s->transit = transit;
     if (d < 0) d = -d;
     s->jitter += (1./16.) * ((double)d - s->jitter);
     
     When a reception report block (to which rr points) is generated for
     this member, the current jitter estimate is returned:
     
     rr->jitter = (u_int32) s->jitter;
     */
}

-(void) deliverPayload:(uint8_t *)payload length:(int)paylen  stamp:(uint64_t) stamp ssrc:(uint32_t) ssrc {
    if (rtpds != nil) {

        NSData * data = [[NSData alloc]initWithBytes:payload length:paylen];
        NSInteger clockStamp = stamp / codecfac;
        [rtpds consumeWireData:data time:clockStamp];
        [data release];
    }
}


-(void) syncChanged:(uint64_t) sync {
    if (_sync == 0) {
        _sync = sync;
    } else {
        // NSString
        NSLog(@"Sync changed: was %qi now %qi", _sync , sync);
        _sync = sync;
    }
}



- (void)rcvLoop{
    //NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	NSLog(@"in rcv thread");
    uint8_t *rcvbuff;
    NSMutableData *rb = [[NSMutableData alloc] initWithLength:1024]; 
    NSTimeInterval ntv = 0.01;
    while (ipv4Soc > 0){
        [rb setLength:1024];
        rcvbuff = [rb mutableBytes];
        int got = recv(ipv4Soc,rcvbuff,1024,0);
        /*if (got < 0) {
            perror("socket error: ");
            ipv4Soc = 0;
        }*/
        if (got > 4) {
            [rb setLength:got];
            [self parsePacket:rb];
        } 
        //[NSThread sleepForTimeInterval:ntv];
    }
    NSLog(@"leaving rcv thread");
    //[pool release];
}

- (BOOL)start: (CFSocketRef) s {
    
    struct hostent *h;
    struct sockaddr_in sock_addr ;
    CFSocketNativeHandle sock ;
    
    
    memset(&sock_addr, 0, sizeof(sock_addr));
    
    h = gethostbyname([farHost UTF8String]);
    memcpy(&sock_addr.sin_addr.s_addr, h->h_addr, sizeof(struct in_addr));
    
    sock_addr.sin_family = AF_INET;
    /* port number we want to connect to */
    sock_addr.sin_port = htons(farPort);
    sock_addr.sin_len = sizeof(sock_addr);
    /* Create our socket */
    if ( 0 >= ( sock = CFSocketGetNative(s))){
        NSLog(@"no socket") ;
    } else {
        NSLog(@"have socket") ;
        if (0 == (connect(sock, (struct sockaddr *)&sock_addr,sizeof(sock_addr)))){
            NSLog(@"socket connected to host %@ at %d ",farHost,farPort) ;
            ipv4Soc = sock;
            struct timeval tv;
            tv.tv_sec = 0;
            tv.tv_usec = 20000; // 20ms 
            setsockopt(ipv4Soc,SOL_SOCKET,SO_RCVTIMEO,&tv, sizeof(tv));
        } else {
            NSLog(@"connect failed to host %@ at %d ",farHost,farPort) ;
            perror("connect");
        }
        
        
        // spawn rcv thread here .....
        rcvThread = [[NSThread alloc] initWithTarget:self selector:@selector(rcvLoop) object:nil];
        [rcvThread setName:@"rtp-rcv"];
        [rcvThread setThreadPriority:0.9];
        [rcvThread start];
    }	
    /* No need for retrys I think.
     NSRunLoop * crl ;
     NSTimer * tick;
     
     
     tick = [NSTimer timerWithTimeInterval:0.1 target:self selector:@selector(retryTime:) userInfo:nil repeats:YES];
     
     crl = [NSRunLoop currentRunLoop];
     [crl addTimer:tick forMode:NSDefaultRunLoopMode];
     */
	_first = true;
    
    return YES;
}

/* this might be better done in it's own thread. */
- (void) digit:(NSString*) digit duration:(int) duration audible:(BOOL)audible{
    /*
     Event  encoding (decimal)
     _________________________
     0--9                0--9
     *                     10
     #                     11
     A--D              12--15
     Flash                 16
     *
     
     The payload format is shown in Fig. 1.
     
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     event     |E|R| volume    |          duration             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     
     */
    int sp = 0;
    int end = 0;
    int db = 3;
    NSString *pf = nil;
    char c = [[digit capitalizedString] characterAtIndex:0];
    if (c >= '0' && c <= '9') {
        sp = (c - '0');
        pf = [NSString stringWithFormat:@"%c",c];
    } else {
        if (c == '#') {
            sp = 11;
            pf = @"p";
        }
        if (c == '*') {
            sp = 10;
            pf = @"s";
        }
    }
    if ((c >= 'A') && (c <= 'F')) {
        sp = (12 + (c - 'A'));
        pf = [NSString stringWithFormat:@"%c",c];

    }
    uint8_t *data = alloca(4);
    memset(data,0,4);
    int dur = codecfac * duration;
    /*
     data[0] = (byte) ((0xff) & (sp | 0x80)); // set End flag
     data[1] = 0 ; // 0db - LOUD
     data[3] = (byte) ((0xff) & (dur));
     data[2] = (byte) ((0xff) & (dur >> 8)) ;
     *
     */
    copyBitsO(sp, 8, data, 0);
    copyBitsO(end, 0, data, 8);
    copyBitsO(db, 6, data, 10);
    copyBitsO(dur, 16, data, 16);
    
    
    uint64_t stamp = ostamp +codecfac; // make it close to the most recent audio packet
    NSData *nsd = [[NSData alloc] initWithBytes:data length:4];
    [self sendPacket:nsd stamp:stamp ptype:DTMFPAYLOADTTYPE marker:YES];
    AVAudioPlayer *av = nil;
    //NSError * error;
    /*if (audible) {
        NSString *path = [[NSBundle mainBundle] pathForResource:@"ringing" ofType:@"wav"];
        NSURL *url = [NSURL fileURLWithPath:path];
        NSLog(@"Loading tone %@  from %@",pf,[url absoluteString]);
        av = [[AVAudioPlayer alloc ] initWithContentsOfURL:url error:&error];
    }*/
    // try to ensure that the time between messages is slightly less than the 
    // selected 'duration'
    long count = (duration / 20) - 1;
    for (int i = 0; i < count; i++) {
        [NSThread sleepForTimeInterval:0.01];
        [self sendPacket:nsd stamp:stamp ptype:DTMFPAYLOADTTYPE marker:NO];
        [NSThread sleepForTimeInterval:0.01];

    }
    //stupid ugly mess - fixed stamp on multiple packets
    //stamp = fac * _audio.getOutboundTimestamp();
    end = 1;
    copyBitsO(end, 1, data, 8);
    [nsd release];
    nsd = [[NSData alloc] initWithBytes:data length:4];
    [self sendPacket:nsd stamp:stamp ptype:DTMFPAYLOADTTYPE marker:NO];
    [self sendPacket:nsd stamp:stamp ptype:DTMFPAYLOADTTYPE marker:NO];
    [self sendPacket:nsd stamp:stamp ptype:DTMFPAYLOADTTYPE marker:NO];
    
    if (av != nil) {
        [av stop];
        [av release];
    }
    [nsd release];
}

- (void) stop{
    close(ipv4Soc);
    ipv4Soc = 0;
}

@end
