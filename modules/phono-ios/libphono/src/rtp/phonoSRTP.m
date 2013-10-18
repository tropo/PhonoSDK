//
//  phonoSRTP.m
//  libphono
//
//  Created by Tim Panton on 02/07/2012.
//  Copyright (c) 2012 Voxeo. All rights reserved.
//

#import "phonoSRTP.h"
#import "Base64.h"
@implementation phonoSRTP

static NSString *  srtp_errs[] = {
    @" nothing to report                       ",
    @" unspecified failure                     ",
    @" unsupported parameter                   ",
    @" couldn't allocate memory                ",
    @" couldn't deallocate properly            ",
    @" couldn't initialize                     ",
    @" can't process as much data as requested ",
    @" authentication failure                  ",
    @" cipher failure                          ",
    @" replay check failed (bad index)         ",
    @" replay check failed (index too old)     ",
    @" algorithm failed test routine           ",
    @" unsupported operation                   ",
    @" no appropriate context found            ",
    @" unable to perform desired validation    ",
    @" can't use key any more                  ",
    @" error in use of socket                  ",
    @" error in use POSIX signals              ",
    @" nonce check failed                      ",
    @" couldn't read data                      ",
    @" couldn't write data                     ",
    @" error pasring data                      ",
    @" error encoding data                     ",
    @" error while using semaphores            ",
    @" error while using pfkey                 "
};


+ (void) initialize {
    if ([self class] == [phonoSRTP class]) {
        srtp_init();
        NSLog(@"initing SRTP lib");
    }
    // Initialization for this class and any subclasses
}

void srtpEventFunc(srtp_event_data_t *data){
    NSString *mess = @"Unknown";
    switch (data->event)
    { 
        case  event_ssrc_collision : mess = @"An SSRC collision occured.";             
            break;

        case event_key_soft_limit: mess = @"An SRTP stream reached the soft key usage limit and will expire soon.";	   
            break;

        case event_key_hard_limit: mess = @"An SRTP stream reached the hard key usage limit and has expired.";
            break;

        case event_packet_index_limit: mess = @"An SRTP stream reached the hard packet limit (2^48 packets).";            
            break;
    }
    NSLog(@"Got SRTP event %@",mess);
}

/*
 // initialize libSRTP 
     srtp_init();
 // set policy to describe a policy for an SRTP stream 
     crypto_policy_set_rtp_default(&policy.rtp);
     crypto_policy_set_rtcp_default(&policy.rtcp);
     policy.ssrc = ssrc;
     policy.key = key; policy.next = NULL;
 // set key to random value 
    crypto_get_random(key, 30);
 // allocate and initialize the SRTP session 
    srtp_create(&session, policy);
 */

- (void) fillPol:(srtp_policy_t *)pol key:(NSString *) master{
    NSData *d = [Base64 decode:master]; // may need to allocate....
    
    if ((d == NULL) ||[d length] != 30) {
        NSLog(@"problem decoding base 64 string -> %@",master);
        NSLog(@"results in %d byte key/salt",[d length]);
    }
    pol->ssrc.type = ssrc_specific;
    pol->ssrc.value = (uint32_t) _csrcid;
    NSLog(@"set context to %8x", (uint32_t)_csrcid);
    
    crypto_policy_set_aes_cm_128_hmac_sha1_80(&(pol->rtp));
    crypto_policy_set_aes_cm_128_hmac_sha1_80(&(pol->rtcp));
    int klen = [d length];
    
    pol->key = crypto_alloc(klen);
    [d getBytes:pol->key length:klen];
    if (pol->key == NULL){
        NSLog(@"Null key ?!?");
    }
    pol->next = 0;
}

- (id) initWithTypeAndKey:(NSString *)ktype keyL:(NSString *) masterL keyR:(NSString *) masterR {
	self =  [super init];
    NSRange range = [ktype rangeOfString:@"AES_CM_128_HMAC_SHA1_80"];
    if (range.length > 0 && range.location == 0){
        // only one we support for now....
        NSLog(@"Doing an SRTP init for %@:%d",farHost,farPort );
        
        [self fillPol:&spolR key:masterR];

        
        [self fillPol:&spolL key:masterL];
        err_status_t  err = srtp_create(&session,  &spolL);
        if (err != err_status_ok) {
            NSLog(@"srtp_create error was %@",srtp_errs[err]);
        } else {
            srtp_install_event_handler(srtpEventFunc);
            _srtp = YES;
        }
        _tailIn = _tailOut = 10;
    } else {
        NSLog(@"Not doing an SRTP init for %@:%d we only support AES_CM_128_HMAC_SHA1_80 not %@",farHost,farPort,ktype );
    }
    

	return self;
}

- (void) protect:(uint8_t *)payload length:(int)paylen{
    int  len = paylen-_tailOut;
    if (_srtp == YES){
        err_status_t  err = srtp_protect(session, payload, &len);
        if (err != err_status_ok) {
            NSLog(@"srtp_protect error was %@",srtp_errs[err]);
            bzero(payload,paylen);
        }
    }
    
}

-(void) syncChanged:(uint64_t) sync {
    if (_sync ==0) {
        spolR.ssrc.value = (uint32_t) sync;
        spolR.next = NULL;
        err_status_t  err = srtp_add_stream(session, &spolR);
        if (err != err_status_ok) {
            NSLog(@"srtp_add_stream inbound error was %@",srtp_errs[err]);
        } else {
            NSLog(@"added srtp stream for inbound %8x",(uint32_t)sync);
        }
    }
    [super syncChanged:sync];
}

- (void) unprotect:(uint8_t *)payload length:(int)paylen{
    int len = paylen ;
    if (_srtp == YES){
        err_status_t  err = srtp_unprotect(session, payload, &len);
        if (err != err_status_ok) {
            NSLog(@"srtp_unprotect error was %@",srtp_errs[err]);
            bzero(payload,len);
            if (err == err_status_no_ctx) {
                NSLog(@"want context of %8x", (uint32_t)_csrcid);
            }
        }
    }
}
- (void) stop{
    [super stop];
    srtp_dealloc(session);
}
@end
