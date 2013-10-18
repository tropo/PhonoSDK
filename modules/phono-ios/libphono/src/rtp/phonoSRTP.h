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
    srtp_policy_t spolL;
    srtp_policy_t spolR;
}
- (id) initWithTypeAndKey:(NSString *)ktype keyL:(NSString *) masterL keyR:(NSString *) masterR;
@end
