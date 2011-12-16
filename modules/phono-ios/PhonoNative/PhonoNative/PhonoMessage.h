//
//  PhonoMessage.h
//  PhonoNative
//
//  Created by Tim Panton on 14/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import <Foundation/Foundation.h>
@class PhonoNative;

@interface PhonoMessage : NSObject{
    PhonoNative *phono;
    NSString *from;
    NSString *to;
    NSString *body;
}

@property(readwrite, assign) PhonoNative *phono;
@property(readwrite, copy) NSString *body;
@property(readwrite, copy) NSString *from;
@property(readwrite, copy) NSString *to;
- (void) sendMe;
- (PhonoMessage *) reply:(NSString *)body;
@end
