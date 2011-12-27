//
//  PhonoXMPP.m
//  PhonoNative
//
//  Created by Tim Panton on 15/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import "PhonoXMPP.h"
#import "PhonoAPI.h"
#import "PhonoNative.h"
#import "PhonoPhone.h"
#import "PhonoCall.h"

@implementation PhonoXMPP

@synthesize  xmppStream, xmppJingle,xmppReconnect;

- (id) initWithPhono:(PhonoNative *)p{
    
    
    self = [super init];
    if (self) {
        phono = p;     
    }
    return self;
}

- (void)setupStream
{
	NSAssert(xmppStream == nil, @"Method setupStream invoked multiple times");
	
	// Setup xmpp stream
	// 
	// The XMPPStream is the base class for all activity.
	// Everything else plugs into the xmppStream, such as modules/extensions and delegates.
    
	xmppStream = [[XMPPStream alloc] init];
	
#if !TARGET_IPHONE_SIMULATOR
	{
		// Want xmpp to run in the background?
		// 
		// P.S. - The simulator doesn't support backgrounding yet.
		//        When you try to set the associated property on the simulator, it simply fails.
		//        And when you background an app on the simulator,
		//        it just queues network traffic til the app is foregrounded again.
		//        We are patiently waiting for a fix from Apple.
		//        If you do enableBackgroundingOnSocket on the simulator,
		//        you will simply see an error message from the xmpp stack when it fails to set the property.
		
		xmppStream.enableBackgroundingOnSocket = YES;
	}
#endif
	
	// Setup reconnect
	// 
	// The XMPPReconnect module monitors for "accidental disconnections" and
	// automatically reconnects the stream for you.
	// There's a bunch more information in the XMPPReconnect header file.
	
	xmppReconnect = [[XMPPReconnect alloc] init];
	
    [xmppReconnect         activate:xmppStream];
	
    xmppJingle = [[XMPPJingle alloc] initWithPhono:NO];
    [xmppJingle setPayloadAttrFilter:@"[@name=\"SPEEX\" and @clockrate=\"8000\"]"];
    
    [xmppJingle activate:xmppStream];
    [xmppJingle addDelegate:self delegateQueue:dispatch_get_main_queue()];
    
    [xmppStream addDelegate:self delegateQueue:dispatch_get_main_queue()];
    
    [xmppStream setHostName:@"ec2-50-19-77-101.compute-1.amazonaws.com"];
    //[xmppStream setHostName:@"staging.phono.com"];

    [xmppStream setHostPort:5222];	
	
    
	// You may need to alter these settings depending on the server you're connecting to
	allowSelfSignedCertificates = NO;
	allowSSLHostNameMismatch = NO;
}

- (void)teardownStream
{
	[xmppStream removeDelegate:self];
	
	[xmppReconnect         deactivate];
    
	
	[xmppStream disconnect];
	
	[xmppStream release];
	[xmppReconnect release];
    [xmppJingle release];
	xmppJingle = nil;
	xmppStream = nil;
	xmppReconnect = nil;
}


- (BOOL)connect:(NSString *)anapiKey
{
    apiKey = [[NSString stringWithString:anapiKey] retain];
	if (![xmppStream isDisconnected]) {
		return YES;
	}
    
	//
	// If you don't want to use the Settings view to set the JID, 
	// uncomment the section below to hard code a JID and password.
	//
	// Replace me with the proper JID and password:
	NSString *myJID = @"anon@phono.com/voxeo";
	NSString *myPassword = @"";
    
	if (myJID == nil || myPassword == nil) {
		NSLog(@"JID and password must be set before connecting!");
        
		return NO;
	}
    
	[xmppStream setMyJID:[XMPPJID jidWithString:myJID]];
	NSError *error = nil;
	if (![xmppStream connect:&error]){        
		NSLog(@"Error connecting: %@", error);
        
		return NO;
	}
    
	return YES;
}

- (void)disconnect {    
    [xmppStream disconnect];
}

- (void)acceptInboundCall:(PhonoCall *)incall{
    
    NSString *lshare = incall.share =  [phono.papi allocateEndpoint];
    NSArray *bits = [lshare componentsSeparatedByString:@":"];
    // 0= 'rtp' 1 = //192.67.4.1 2 =3020
    if (( [bits count ] == 3) 
        && ( [(NSString *)[bits objectAtIndex:0] compare:@"rtp"] == NSOrderedSame ) ){
        NSString *host = [(NSString *)[bits objectAtIndex:1] substringFromIndex:2];
        NSString *port = [bits objectAtIndex:2];
        [xmppJingle sendSessionAccept:[incall callId] to:[incall from] host:host port:port payload:[incall payload]];
        NSString *fhost = [xmppJingle ipWithCandidate:(NSXMLElement *)[incall candidate] ];
        NSString *fport = [xmppJingle portWithCandidate:(NSXMLElement *)[incall candidate] ];

        NSString *fullshare = [NSString stringWithFormat:@"%@:%@:%@", lshare, fhost , fport];
        
        NSString *mycodec = [xmppJingle ptypeWithPayload:(NSXMLElement *)[incall payload] ];
        NSLog(@" codec req is %@",mycodec );
        [[phono papi] share:fullshare autoplay:NO codec:mycodec ]; // hack!b
    }
    
}

- (void)hangupCall:(PhonoCall *)acall{
    NSString *lshare = acall.share ;
    [[phono papi] stop:lshare];
    NSLog(@"stopping %@",[acall share] );

    [[phono papi] freeEndpoint:lshare];
    
    [xmppJingle sendHangup:acall.callId to:[acall from] reason:@"success"];
}

- (void) jingleSessionInit {
    NSError *error;
    NSString *sample = @"<iq type=\"set\" to=\"timpanton\\40sip2sip.info@sip\" xmlns=\"jabber:client\" id=\"8019:sendIQ\"><jingle xmlns=\"urn:xmpp:jingle:1\" action=\"session-initiate\" initiator=\"29b0c2c9-308e-4b7d-b969-16303d449298@gw114.phono.com/voxeo\" sid=\"5fa70004e001e018f8ae197e03ddb0a3\"><content creator=\"initiator\"><description xmlns=\"urn:xmpp:jingle:apps:rtp:1\"><payload-type id=\"103\" name=\"SPEEX\" clockrate=\"16000\"/><payload-type id=\"102\" name=\"SPEEX\" clockrate=\"8000\"/></description><transport xmlns=\"urn:xmpp:jingle:transports:raw-udp:1\"><candidate ip=\"192.168.0.14\" port=\"59891\" generation=\"1\"/></transport></content></jingle></iq>";
    NSXMLElement *iiq =[[NSXMLElement alloc] initWithXMLString:sample error:&error];
    
    XMPPIQ *iq = [XMPPIQ iqFromElement:iiq ];
    [xmppStream sendElement:iq];
    
}


- (void) sendApiKey {
    // <iq type="set" xmlns="jabber:client"><apikey xmlns="http://phono.com/apikey">C17D167F-09C6-4E4C-A3DD-2025D48BA243</apikey></iq>
    XMPPIQ *iq = [XMPPIQ iqWithType:@"set" ];
    NSXMLElement *xapikey = [NSXMLElement elementWithName:@"apikey" xmlns:@"http://phono.com/apikey"];
    [xapikey addChild:[NSXMLNode textWithStringValue:apiKey]];
    [iq addChild:xapikey];
    [xmppStream sendElement:iq];
}



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark XMPPJingle Delegate
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)xmppJingle:(XMPPJingle *)sender didReceiveIncommingAudioCall:(NSString *)sid from:(XMPPJID *)from to:(XMPPJID *)to transport:(NSXMLElement *)candidate sdp:(NSXMLElement *)payload {
    PhonoCall * incall = [[PhonoCall alloc] initInbound];
    [incall setPhono:phono];
    [incall setCallId:sid];
    [incall setPayload:payload];
    [incall setCandidate:candidate];
    [incall setFrom:[from full]];
    [incall setTo:[to full]];
    [phono.phone didReceiveIncommingCall:incall];
}

- (void)xmppJingle:(XMPPJingle *)sender
  didReceiveResult:(NSString *)sid {
    // should have a list of calls - but for now a choice of 1
    PhonoCall *ccall = [phono.phone currentCall];
    if ([[ccall callId] compare:sid] == NSOrderedSame ){
        [[phono papi] start:[ccall share]];
        NSLog(@"starting %@",[ccall share] );
    }
    
    
}
- (void)xmppJingle:(XMPPJingle *)sender
   didReceiveError:(NSString *)sid error:(NSXMLElement*)error{
    
}
- (void)xmppJingle:(XMPPJingle *)sender
didReceiveTerminate:(NSString *)sid reason:(NSString*)reason{
    // should have a list of calls - but for now a choice of 1
    PhonoCall *ccall = [phono.phone currentCall];
    if ([[ccall callId] compare:sid] == NSOrderedSame ){
        [[phono papi] stop:[ccall share]];
        NSLog(@"stopping %@",[ccall share] );
        if (ccall.onHangup != nil){
            ccall.onHangup();
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark XMPPStream Delegate
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)xmppStream:(XMPPStream *)sender socketDidConnect:(GCDAsyncSocket *)socket 
{
    
	NSLog(@"Socket Connected");
}

- (void)xmppStream:(XMPPStream *)sender willSecureWithSettings:(NSMutableDictionary *)settings
{
	NSLog(@"Secure with settings");
}

- (void)xmppStreamDidSecure:(XMPPStream *)sender
{
	NSLog(@"Secured with settings");
}

- (void)xmppStreamDidConnect:(XMPPStream *)sender
{
	NSLog(@"Connected");
	
	isXmppConnected = YES;
	
	NSError *error = nil;
    
    [sender authenticateAnonymously:&error];
}

- (void)xmppStreamDidAuthenticate:(XMPPStream *)sender
{
	NSLog(@"Authenticated as %@", [sender myJID]) ;
    phono.sessionID = [[sender myJID] user];
    [xmppJingle setMe:[sender myJID]];
     
    if (phono.onReady != nil){
        dispatch_queue_t q_main = dispatch_get_main_queue();
        dispatch_async(q_main, phono.onReady);
    }
    [self sendApiKey];
}

- (void)xmppStream:(XMPPStream *)sender didNotAuthenticate:(NSXMLElement *)error
{
	NSLog(@"Not Authenticated because %@", [error XMLString]);
}

- (BOOL)xmppStream:(XMPPStream *)sender didReceiveIQ:(XMPPIQ *)iq
{
	NSLog(@"Got iq %@", [iq XMLString]);
	
	return NO;
}

- (void)xmppStream:(XMPPStream *)sender didReceiveMessage:(XMPPMessage *)message
{
	NSLog(@"Got message from %@ : %@", [message from] , [[message elementForName:@"body"] stringValue]);
    
}

- (void)xmppStream:(XMPPStream *)sender didReceivePresence:(XMPPPresence *)presence
{
	NSLog(@"Presnce");
}

- (void)xmppStream:(XMPPStream *)sender didReceiveError:(id)error
{
	NSLog(@"Error");
}

- (void)xmppStreamDidDisconnect:(XMPPStream *)sender withError:(NSError *)error
{	
    NSLog(@"disconnected ");
	if (!isXmppConnected)
	{
		NSLog(@"Unable to disconnect from server. Check xmppStream.hostName");
	}
    if (phono.onUnready != nil){
        dispatch_queue_t q_main = dispatch_get_main_queue();
        dispatch_async(q_main, phono.onUnready);
    }
}
@end
