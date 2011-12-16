//
//  PhonoMessaging.h
//  PhonoNative
//
//  Created by Tim Panton on 14/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import <Foundation/Foundation.h>
@class PhonoEvent;
@class PhonoNative;
@class PhonoMessage;
@interface PhonoMessaging : NSObject {
    PhonoNative *phono;
    void (^onMessage)(PhonoMessage *);
}
- (void) send:(PhonoMessage *)mess;
@property(nonatomic, copy) void (^onMessage)(PhonoMessage *);
@property(readwrite, assign) PhonoNative *phono;

@end
