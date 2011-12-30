//
//  PhonoMessaging.m
//  PhonoNative
//
//  Created by Tim Panton on 14/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import "PhonoMessaging.h"
#import "PhonoMessage.h"
@implementation PhonoMessaging
@synthesize phono,onMessage;
- (void) send:(PhonoMessage *)mess {
    mess.phono = [self phono];
    [mess sendMe];
};

@end
