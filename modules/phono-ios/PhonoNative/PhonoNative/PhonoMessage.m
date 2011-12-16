//
//  PhonoMessage.m
//  PhonoNative
//
//  Created by Tim Panton on 14/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import "PhonoMessage.h"

@implementation PhonoMessage
@synthesize to,from,phono,body;
-(void) sendMe{}
- (PhonoMessage *) reply:(NSString *)body {return nil;}

@end
