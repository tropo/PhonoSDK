//
//  phonoSRTP.h
//  libphono
//
//  Created by Tim Panton on 02/07/2012.
//  Copyright (c) 2012 Voxeo. All rights reserved.
//

#import "phonoRTP.h"
#import "../libsrtp/include/srtp.h"

@interface phonoSRTP : phonoRTP {
    srtp_t session;
    srtp_policy_t spol;
    crypto_policy_t cpol;
    ssrc_t ssrcS;
}
- (id) initWithTypeAndKey:(NSString *)ktype key:(NSString *) master;
@end
