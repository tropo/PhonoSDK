//
//  PhonoXMPP.h
//  PhonoNative
//
//  Created by Tim Panton on 15/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import <Foundation/Foundation.h>
@class UIImage;
#import "XMPPFramework.h"
#import "XMPPReconnect.h"
#import "XMPPJingle.h"
#import "PhonoNative.h"

@interface PhonoXMPP : NSObject <XMPPStreamDelegate , XMPPJingleDelegate>
{
    XMPPStream *xmppStream;
	XMPPReconnect *xmppReconnect;
    XMPPJingle *xmppJingle;
    
	NSString *apiKey;
	BOOL allowSelfSignedCertificates;
	BOOL allowSSLHostNameMismatch;
	PhonoNative *phono;
	BOOL isXmppConnected;
}

@property (nonatomic, readonly) XMPPStream *xmppStream;
@property (nonatomic, readonly) XMPPReconnect *xmppReconnect;
@property (nonatomic, readonly) XMPPJingle *xmppJingle;


- (BOOL)connect:(NSString *)apiKey;
- (void)disconnect;
- (void)setupStream;
- (void)jingleSessionInit;
- (id) initWithPhono:(PhonoNative *)p;
- (void)acceptInboundCall:(PhonoCall *)incall;

@end
