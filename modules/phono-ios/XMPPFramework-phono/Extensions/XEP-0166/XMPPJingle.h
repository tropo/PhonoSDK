//
//  XMPPJingle.h
//  PhonoNative
//
//  Created by Tim Panton on 20/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import "XMPPModule.h"
#import "XMPPFramework.h"

@interface XMPPJingle : XMPPModule
{
    XMPPJID *me;
    NSMutableDictionary *namespaces;
    NSMutableDictionary *unAcked;
    NSString *payloadAttrFilter;
    BOOL phonoBugs;
}
@property (copy, readwrite) XMPPJID *me;
@property (copy, readwrite) NSString *payloadAttrFilter;
- (NSString *) mkIdElement;
- (id)initWithPhono:(BOOL)phonoBugs;
- (NSString *) ptypeWithPayload:(NSXMLElement *)payload;
- (NSString *) ipWithCandidate:(NSXMLElement *) candidate;
- (NSString *) portWithCandidate:(NSXMLElement *) candidate;
- (void) sendSessionAccept:(NSString *)sid to:(NSString *)tos host:(NSString *)host port:(NSString *)port payload:(NSXMLElement*)payload cryptos:(NSArray *) cryptos ;

-(void) sendHangup:(NSString *) sid 
                to:(NSString *) tos reason:(NSString *)reason;
-(NSXMLElement *)emptyAudioPayload;
-(void) addCodecToPayload:(NSXMLElement *)allAudioCodecs name:(NSString *)name rate:(NSString *)rate ptype:(NSString *)ptype;
-(NSString *) initSessionTo:(NSString *)tos lhost:(NSString *)host lport:(NSString *)port payloads:(NSXMLElement *)codecs  custom:(NSArray*)misc
    cryptoRequired:(NSInteger)cryptoRequired cryptoLines:(NSArray *)cryptoLines;

@end

@protocol XMPPJingleDelegate <NSObject>
- (void)xmppJingle:(XMPPJingle *)sender didReceiveIncommingAudioCall:(NSString *)sid from:(XMPPJID *)from to:(XMPPJID *)to transport:(NSXMLElement *)candidate sdp:(NSXMLElement *)payload cryptoRequired:(NSString *) creqI cryptoElements:(NSArray *)cels;
- (void)xmppJingle:(XMPPJingle *)sender
  didReceiveResult:(NSString *)sid ;
- (void)xmppJingle:(XMPPJingle *)sender
   didReceiveError:(NSString *)sid error:(NSXMLElement*)error;
- (void)xmppJingle:(XMPPJingle *)sender
   didReceiveTerminate:(NSString *)sid reason:(NSString*)reason;
- (void)xmppJingle:(XMPPJingle *)sender didReceiveAcceptForCall:(NSString *)sid from:(XMPPJID *)from to:(XMPPJID *)to transport:(NSXMLElement *)candidate sdp:(NSXMLElement *)payload cryptoRequired:(NSString *) creqI cryptoElements:(NSArray *)cels ;
- (void)xmppJingle:(XMPPJingle *)sender didReceiveInfoForCall:(NSString *)sid from:(XMPPJID *)from to:(XMPPJID *)to info:(NSXMLNode *)info;

@optional


@end