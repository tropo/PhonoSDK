//
//  PhonoMessage.m
//  PhonoNative
//
//  Created by Tim Panton on 14/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import "PhonoMessage.h"
#import "PhonoXMPP.h"

@implementation PhonoMessage
@synthesize to,from,phono,body;
-(void) sendMe{
    PhonoXMPP *px = [phono pxmpp];
    [px sendMessage:self];
}
- (PhonoMessage *) reply:(NSString *)rbody {
    PhonoMessage * rep = [[PhonoMessage alloc] init];
    [rep setPhono:[self phono]];
    [rep setFrom:[self to]];
    [rep setTo:[self from]];
    [rep setBody:rbody];
    [rep sendMe];
    return rep;
}

@end
