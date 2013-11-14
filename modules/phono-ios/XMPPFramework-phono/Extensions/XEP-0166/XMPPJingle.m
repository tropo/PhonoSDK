//
//  XMPPJingle.m
//  PhonoNative
//
//  Created by Tim Panton on 20/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import "XMPPJingle.h"
#define NS_JINGLE          @"urn:xmpp:jingle:1"
#define NS_JINGLE_RTP      @"urn:xmpp:jingle:apps:rtp:1"
#define NS_JINGLE_UDP      @"urn:xmpp:jingle:transports:raw-udp:1"
#define NS_PHONOEMPTY      @""
#define NS_JABBER          @"jabber:client"
#define NS_RTMP            @"http://voxeo.com/gordon/apps/rtmp"
#define NS_RTMPT           @"http://voxeo.com/gordon/transports/rtmp"
#define SERVICEUNAVAIL     @"<error type='cancel'><service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>"

@implementation XMPPJingle
@synthesize me, payloadAttrFilter;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (id)initWithPhono:(BOOL)tphonoBugs
{
	self = [super initWithDispatchQueue:nil];
    namespaces = [[NSMutableDictionary alloc] init];
    [namespaces setObject:NS_JINGLE forKey:@"jingle"];
    [namespaces setObject:NS_JINGLE_RTP forKey:@"rtp"];
    [namespaces setObject:NS_JINGLE_UDP forKey:@"udp"];
    [namespaces setObject:NS_JABBER forKey:@"jabber"];
    unAcked = [[NSMutableDictionary alloc] init];
    phonoBugs = tphonoBugs;

	return self;
}


- (BOOL)activate:(XMPPStream *)aXmppStream
{
	if ([super activate:aXmppStream])
	{
		// Custom code goes here (if needed)
		return YES;
	}
	
	return NO;
}

- (void)deactivate
{
	[super deactivate];
}

- (void)dealloc
{
    // release stuff here
	[super dealloc];
}

- (NSString *) ptypeWithPayload:(NSXMLElement *)payload{
    return [[NSString alloc] initWithFormat:@"%@:%@:%@",
            [payload attributeStringValueForName:@"name"],
            [payload attributeStringValueForName:@"clockrate"],
            [payload attributeStringValueForName:@"id"]];
}
- (NSString *) ipWithCandidate:(NSXMLElement *)candidate{
    return [candidate attributeStringValueForName:@"ip"];
}

- (NSString *) portWithCandidate:(NSXMLElement *)candidate{
    return [candidate attributeStringValueForName:@"port"];
}



- (NSString *) mkIdElement{
    NSDate *now = [NSDate date];
    NSTimeInterval fpnow = [now timeIntervalSince1970];
    NSString *ret = [[[NSString alloc] initWithFormat:@"%10.4f", fpnow] autorelease];
    return ret;
}

- (NSString *) mkSidElement{
    NSDate *now = [NSDate date];
    NSTimeInterval fpnow = [now timeIntervalSince1970];
    NSString *ret = [[[NSString alloc] initWithFormat:@"%a", fpnow] autorelease];
    return ret;
}


- (XMPPIQ *) sendResultAck:(XMPPIQ *) iq{
    /*<iq from='juliet@capulet.lit/balcony'
     id='xs51r0k4'
     to='romeo@montague.lit/orchard'
     type='result'/> */
    
    return [XMPPIQ iqWithType:@"result" to:[iq from] elementID:[iq elementID]];
}

- (XMPPIQ *) sendResultError:(XMPPIQ *) iq because:(NSString *)bs{
    /* <iq from='juliet@capulet.lit/balcony'
     id='xs51r0k4'
     to='romeo@montague.lit/orchard'
     type='error'>
     <error type='cancel'>
     <service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
     </error>
     </iq> */
    NSError *error;
    NSXMLElement * body = [[NSXMLElement alloc] initWithXMLString:bs error:&error ];
    return (error != nil)?nil:[XMPPIQ iqWithType:@"result" to:[iq from] elementID:[iq elementID] child:body];
}

- (NSXMLElement *) jingleBodyWithAction:(NSString *) act sid:(NSString *) sid {
    NSXMLElement * body = [[NSXMLElement alloc] initWithName:@"jingle" xmlns:NS_JINGLE  ];
    [body addAttributeWithName:@"sid" stringValue:sid];
    [body addAttributeWithName:@"action" stringValue:act];

    return body;
}


- (NSArray *) xpns:(NSXMLElement *)d q:(NSString *)q {
    NSError *error = nil;
    if (phonoBugs) {
        q = [q stringByReplacingOccurrencesOfString:@"pjingle:" withString:@""]; 
        q = [q stringByReplacingOccurrencesOfString:@"prtp:" withString:@""]; 
        q = [q stringByReplacingOccurrencesOfString:@"pudp:" withString:@""]; 
    } else {
        q = [q stringByReplacingOccurrencesOfString:@"pjingle:" withString:@"jingle:"]; 
        q = [q stringByReplacingOccurrencesOfString:@"prtp:" withString:@"rtp:"]; 
        q = [q stringByReplacingOccurrencesOfString:@"pudp:" withString:@"udp:"];         
    }
    NSArray * ret = [d nodesForXPathWithNamespaces:q namespaces:namespaces error: &error];
    if (error != nil){
        NSLog(@"Problem in xpath : %@", error);
    }
    NSLog(@"xpath was : %@", q);
    return ret;
}

// find the first thing that the xpath matches
- (NSXMLElement *) xp0:(NSXMLElement *)d q:(NSString *)path{
    NSXMLElement * ret = nil;
    NSArray *a = [self xpns:d q:path];
    if ((a != nil) && ([a count] == 1)){
        ret = [a objectAtIndex:0];
    }
    return ret;
}
// set the value of the attribute the xpath matches
- (void) xp0sa:(NSXMLElement *)d q:(NSString *)path value:(NSString*)val{
    NSXMLElement *a = [self xp0:d q:path];
    [a setStringValue:val];
}

-(NSXMLElement *)emptyAudioPayload{
    NSXMLElement * descript =[[NSXMLElement alloc]  initWithName:@"description" xmlns:NS_JINGLE_RTP];
    [descript addAttributeWithName:@"media" stringValue:@"audio"];
    return descript;
}

-(void) addCodecToPayload:(NSXMLElement *)all name:(NSString *)name rate:(NSString *)rate ptype:(NSString *)ptype{
    NSXMLElement *pa = [NSXMLElement elementWithName:@"payload-type"];
    [pa setXmlns:NS_JINGLE_RTP];
    [pa addAttributeWithName:@"id" stringValue:ptype ];
    [pa addAttributeWithName:@"name" stringValue:name ];
    [pa addAttributeWithName:@"clockrate" stringValue:rate];
    [all addChild:pa];
}

- (NSXMLElement *) mkCandidate:(NSString *)host port:(NSString*)port gen:(NSString *)gen comp:(NSString *)comp {
    NSXMLElement *ca = [NSXMLElement elementWithName:@"candidate"];
    [ca setXmlns:NS_JINGLE_UDP];
    [ca addAttributeWithName:@"ip" stringValue:host ];
    [ca addAttributeWithName:@"port" stringValue:port ];
    [ca addAttributeWithName:@"generation" stringValue:@"0"];
    [ca addAttributeWithName:@"component" stringValue:@"1"];
    return ca;
}

- (NSArray *) mkCryptoElems:(NSArray *)cryptoLines {
    //      <crypto tag='1' crypto-suite='AES_CM_128_HMAC_SHA1_80' key-params=''/> 
    int len = 0;
    if ((cryptoLines != nil) && ([cryptoLines count] > 0)){
        len = [cryptoLines count];
    }
    NSMutableArray *ret = [NSMutableArray arrayWithCapacity:len];
    for (int i=0;i<len;i++){
        NSXMLElement *cr = [NSXMLElement elementWithName:@"crypto"];
        NSDictionary *cline = [cryptoLines objectAtIndex:i];
        NSLog(@"making crypto element from tag=%d suite=%@ key-params=%@", (i+1) , [cline objectForKey:@"crypto-suite"],[cline objectForKey:@"key-params"] );

        [cr setXmlns:NS_JINGLE_RTP];
        [cr addAttributeWithName:@"tag" stringValue:[NSString stringWithFormat:@"%d",i+1] ];
        [cr addAttributeWithName:@"crypto-suite" stringValue:[cline objectForKey:@"crypto-suite"] ];
        [cr addAttributeWithName:@"key-params" stringValue:[cline objectForKey:@"key-params"]];
        [ret addObject:cr];
    }
    
    return ret;
}

-(NSString *) initSessionTo:(NSString *)tos lhost:(NSString *)host lport:(NSString *)port payloads:(NSXMLElement *)codecs custom:(NSArray*)misc cryptoRequired:(NSInteger)cryptoRequired cryptoLines:(NSArray *)cryptoLines {
    NSString *sid = [self mkSidElement];
    
    NSString *template = @"<jingle xmlns='urn:xmpp:jingle:1' \
    action='session-initiate' \
    initiator='' \
    sid=''> \
    <content creator='initiator' name='voice'>\
    <description xmlns='urn:xmpp:jingle:apps:rtp:1' media='audio'>\
    <payload-type id='101' name='telephone-event' clockrate='8000'/>\
    <encryption required='0'>\
    </encryption> \
    </description>\
    <transport xmlns='urn:xmpp:jingle:transports:raw-udp:1'> \
    </transport> \
    </content> \
    </jingle>";
    NSError *error;
    NSString *initiator = [me full]; 
    
    NSXMLElement * body =[[NSXMLElement alloc] initWithXMLString:template error:&error ];
    if(misc != nil){
        for(int i=0; i<[misc count];i++){
            [body addChild:[misc objectAtIndex:i]];
        }
    }
    
    XMPPJID *to = [XMPPJID jidWithString:tos];
    NSString *elementID =[self mkIdElement];
    XMPPIQ *iq = [XMPPIQ iqWithType:@"set" to:to  elementID:elementID child:body];
    [iq setXmlns:NS_JABBER];
    [self xp0sa:iq q:@"/jabber:iq/jingle:jingle/@initiator" value:initiator];
    [self xp0sa:iq q:@"/jabber:iq/jingle:jingle/@sid" value:sid];

    
    NSXMLElement *desc = [self xp0:iq q:@"/jabber:iq/jingle:jingle/jingle:content/rtp:description"] ;
    NSArray * payloads = [self xpns:codecs q:[NSString stringWithFormat:@"rtp:payload-type%@", payloadAttrFilter]];
    if ((payloads != nil) && ([payloads count] > 0)) {
        for (int i=0; i<[payloads count]; i++){
            NSXMLElement * pay = [[payloads objectAtIndex:i] copy];
            [pay detach];
            NSLog(@"adding payload -> %@",[pay XMLString]);
            [desc addChild:pay];
        }
    } else {
        NSLog(@"error - no matching payloads for %@ in %@" ,payloadAttrFilter,[codecs XMLString]);
    }
    [[self xp0:iq q:@"/jabber:iq/jingle:jingle/jingle:content/udp:transport"] 
     addChild:[self mkCandidate:host port:port gen:@"0" comp:@"1"]];
    [self xp0sa:iq q:@"/jabber:iq/jingle:jingle/jingle:content/rtp:description/rtp:encryption/@required" value:[NSString stringWithFormat:@"%ld",(long)cryptoRequired]];
    
    NSXMLElement *encryption = [self xp0:iq q:@"/jabber:iq/jingle:jingle/jingle:content/rtp:description/rtp:encryption"];
    NSArray * cels = [self mkCryptoElems:cryptoLines];
    if ((cels != nil) && ([cels count] > 0)) {
        for (int i=0; i<[cels count]; i++){
            NSXMLElement * ce = [cels objectAtIndex:i];
            NSLog(@"adding crypto -> %@",[ce XMLString]);
            [encryption addChild:ce];
        }
    } else {
        [desc removeChildAtIndex:[encryption index] ];
    }
    NSLog(@" Send -> %@",[iq XMLString]);
    [unAcked setObject:sid forKey:elementID];
    [xmppStream sendElement:iq]; 
    return sid;
}


- (void) sendSessionAccept:(NSString *)sid to:(NSString *)tos host:(NSString *)host port:(NSString *)port payload:(NSXMLElement*)payload cryptos:(NSArray *) cryptos {
    NSString *template = @"<jingle xmlns=\"urn:xmpp:jingle:1\" action=\"session-accept\" initiator=\"\" sid=\"\">\
    <content creator=\"initiator\">\
    <description xmlns=\"urn:xmpp:jingle:apps:rtp:1\" media=\"audio\">\
    <encryption/>\
    <payload-type id=\"101\" name=\"telephone-event\" clockrate=\"8000\"/>\
    </description>\
    <transport xmlns=\"urn:xmpp:jingle:transports:raw-udp:1\">\
    </transport>\
    </content>\
    </jingle>";
    NSString *initiator = @"timpanton@sip2sip.info"; // FIX FIX FIX
    
    NSError *error;
    NSXMLElement * body =[[NSXMLElement alloc] initWithXMLString:template error:&error ];
    
    XMPPJID *to = [XMPPJID jidWithString:tos];
    NSString *elementID =[self mkIdElement];
    XMPPIQ *iq = [XMPPIQ iqWithType:@"set" to:to  elementID:elementID child:body];
    
    [self xp0sa:iq q:@"/iq/jingle:jingle/@initiator" value:initiator];
    [self xp0sa:iq q:@"/iq/jingle:jingle/@sid" value:sid];
    NSXMLElement * desc = [self xp0:iq q:@"/iq/jingle:jingle/jingle:content/rtp:description"];
    [desc addChild:payload];
    [[self xp0:iq q:@"/iq/jingle:jingle/jingle:content/udp:transport"] 
     addChild:[self mkCandidate:host port:port gen:@"0" comp:@"1"]];
    NSXMLElement * enc = [self xp0:iq q:@"/iq/jingle:jingle/jingle:content/rtp:description/rtp:encryption"];
    if ((cryptos != nil) && ([cryptos count] > 0)){
        NSArray *cels = [self mkCryptoElems:cryptos];
        for(NSXMLElement *cel in cels){
            NSLog(@"adding %@ to encryption element",[cel stringValue]);
            [enc addChild:cel];
        }
    } else {
        [desc removeChildAtIndex:[enc index]];  
    }
    NSLog(@" Send -> %@",[iq XMLString]);
    [unAcked setObject:sid forKey:elementID];
    [xmppStream sendElement:iq]; 
}

-(void) sendHangup:(NSString *) sid to:(NSString *)tos reason:(NSString *)reason{
    /* 
<iq xmlns="jabber:client" type="set" to="timpanton\40sip2sip.info@sip" id="3166:sendIQ">
     <jingle xmlns="urn:xmpp:jingle:1" action="session-terminate" initiator="timpanton@sip2sip.info" sid="75eb2be3-5f82-4790-a6df-31ac4f1f9a10"/></iq>
<iq xmlns="jabber:client" type="set" to="timpanton\40sip2sip.info@sip" id="1324999727.9577">
     <jingle xmlns="urn:xmpp:jingle:1" sid="33fbd5cc-e4a0-4c9d-b6ba-5080086e9b51" initiator="timpanton@sip2sip.info"></jingle></iq>
     */
    NSXMLElement * body = [self jingleBodyWithAction:@"session-terminate" sid:sid];
    [body addAttributeWithName:@"initiator" stringValue:@"timpanton@sip2sip.info"]; // FIX FIX FIX

    NSXMLElement *xr = [NSXMLElement elementWithName:@"reason" xmlns:NS_JINGLE];
    NSXMLElement *xrb = [NSXMLElement elementWithName:reason xmlns:NS_JINGLE];
    [xr addChild:xrb];
    [body addChild:xr];
    XMPPJID *to = [XMPPJID jidWithString:tos];
    NSString *elementID =[self mkIdElement];
    XMPPIQ *iq = [XMPPIQ iqWithType:@"set" to:to  elementID:elementID child:body];
    [iq setXmlns:NS_JABBER];
    NSLog(@" Send -> %@",[iq XMLString]);
    [unAcked setObject:sid forKey:elementID];
    [xmppStream sendElement:iq]; 
}


- (XMPPIQ *) didRecvSessionTerminate:(XMPPStream *)sender iq:(XMPPIQ *)iq {
    XMPPIQ * ret = nil;
    NSLog(@" got -> %@",[iq XMLString]);
    /* <iq from='juliet@capulet.lit/balcony'
     id='bv81gs75'
     to='romeo@montague.lit/orchard'
     type='set'>
     <jingle xmlns='urn:xmpp:jingle:1'
     action='session-terminate'
     sid='a73sjjvkla37jfea'>
     <reason>
     <success/>
     </reason>
     </jingle>
     </iq> 
     */
    NSXMLElement *sid = [self xp0:iq q:@"/jabber:iq/jingle:jingle[@action=\"session-terminate\"]/@sid"];
    NSXMLElement *reasonz = [self xp0:iq q:@"/jabber:iq/jingle:jingle/reason/*"]; // PHONO NAMESPACE bug here.
    if (sid != nil){
        NSString *ssid =[sid stringValue];
        NSString *sreason = @"";
        if  (reasonz != nil){
            sreason =[reasonz name];
        }
        [multicastDelegate xmppJingle:self didReceiveTerminate:ssid reason:sreason ];
        ret = [self sendResultAck:iq];
    }
    return ret;
}
- (XMPPIQ *) didRecvSessionAccept:(XMPPStream *)sender iq:(XMPPIQ *)iq {
    XMPPIQ * ret = nil;
    NSLog(@" got -> %@",[iq XMLString]);
    
    
    NSXMLElement *sid = [self xp0:iq q:@"jingle:jingle[@action=\"session-accept\"]/@sid"];
    NSXMLElement *candidate = [self xp0:iq q:@"jingle:jingle[@action=\"session-accept\"]/pjingle:content/udp:transport/pudp:candidate[@component=\"1\"]"];
    NSString *xpath = [NSString stringWithFormat:@"jingle:jingle[@action=\"session-accept\"]/pjingle:content/rtp:description[@media=\"audio\"]/prtp:payload-type%@", payloadAttrFilter];
    
    NSArray *cels = [self xpns:iq q:@"jingle:jingle[@action=\"session-accept\"]/pjingle:content/rtp:description[@media=\"audio\"]/prtp:encryption/prtp:crypto"];
    NSXMLElement *creq = [self xp0:iq q:@"jingle:jingle[@action=\"session-accept\"]/pjingle:content/rtp:description[@media=\"audio\"]/prtp:encryption/@required]"];
    
    NSXMLElement * payload = [self xp0:iq q:xpath];
      
    
    if ((sid != nil) && (payload != nil) && (candidate != nil)){
        // say we will think about it.
        ret = [self sendResultAck:iq];
        // and tell the user:
        NSString *ssid =[sid stringValue];
        NSString *creqI = [creq stringValue];
        XMPPJID *sfrom = [iq from];
        XMPPJID *sto = [iq to];
        NSMutableArray * cla = [NSMutableArray arrayWithCapacity:[cels count]];
        for (NSXMLElement *ce in cels){
            [cla addObject:[ce attributesAsDictionary]];
            NSLog(@"adding a crypto line");
        }
        NSLog(@"now have %lu crypto lines",(unsigned long)[cla count]);
        NSLog(@"far end required encryption attribute is %@ ",creqI);

        [multicastDelegate xmppJingle:self didReceiveAcceptForCall:ssid from:sfrom to:sto transport:candidate sdp:payload cryptoRequired:creqI cryptoElements:cla];
        
    } else {
        // nothing we can understand...
        NSLog(@"confused, one of these was nil");
        NSLog(@"payload nil %d",payload == nil );
        NSLog(@"sid nil %d",sid == nil );
        NSLog(@"candidate nil %d",candidate == nil );

        ret = [self sendResultError:iq because:SERVICEUNAVAIL];
    }
    
    
    return ret;

    
}
- (XMPPIQ *) didRecvSessionInfo:(XMPPStream *)sender iq:(XMPPIQ *)iq {

/*
 <iq xmlns="jabber:client" type="set" from="timpanton\40sip2sip.info@sip" to="6890ca2e-ae2a-46c7-9aea-93e3e1cc926b@phono.com/voxeo" id="adcae3c6-2f53-4eea-b610-4b0addcb0c94"><jingle xmlns="urn:xmpp:jingle:1" action="session-info" initiator="6890ca2e-ae2a-46c7-9aea-93e3e1cc926b@ec2-50-19-77-101.compute-1.amazonaws.com/voxeo" sid="0x1.3bf18cad13cp+30"><ringing xmlns="urn:xmpp:jingle:apps:rtp:1:info"></ringing></jingle></iq>
 */
    XMPPIQ * ret = nil;
    NSLog(@" got -> %@",[iq XMLString]);
    
    
    NSXMLElement *sid = [self xp0:iq q:@"jingle:jingle[@action=\"session-info\"]/@sid"];
    NSXMLNode *info = [iq childAtIndex:0];
    
    
    if ((sid != nil) && (info != nil) ){
        // say we will we got that.
        ret = [self sendResultAck:iq];
        // and tell the user:
        NSString *ssid =[sid stringValue];
        XMPPJID *sfrom = [iq from];
        XMPPJID *sto = [iq to];
        
        [multicastDelegate xmppJingle:self didReceiveInfoForCall:ssid from:sfrom to:sto info:info ];
        
    }     
    return ret;
}

- (XMPPIQ *) didRecvSessionInitiate:(XMPPStream *)sender iq:(XMPPIQ *)iq {
    XMPPIQ * ret = nil;
    /* THis is what old phono sends - note the xmlns="" errors  -we need to cope with them. Sigh.
     hence the faffing with prtp namespace - we rewrite that to being rtp or "" depending on 
     the state of the phonoBugs flag.
     
     <iq xmlns="jabber:client" type="set" from="timpanton\40sip2sip.info@sip" to="e873e415-272b-4e36-8fdc-3ea3d26097e1@phono.com/voxeo" id="08d36f0f-1be4-40f9-9c3f-11e3ca97c6e3">
      <jingle xmlns="urn:xmpp:jingle:1" initiator="timpanton@sip2sip.info" sid="c027b7a5-b464-4cc9-9b9c-351904640dd0" action="session-initiate">
       <content xmlns="">
        <description xmlns="http://voxeo.com/gordon/apps/rtmp" media="audio"><payload-type xmlns="" id="116" name="SPEEX" clockrate="16000"/></description>
        <transport xmlns="http://voxeo.com/gordon/transports/rtmp">
          <candidate xmlns="" rtmpUri="rtmfp://ec2-50-19-77-101.compute-1.amazonaws.com/live" playName="f24eb8d0-e540-4fd2-8f52-fe399dbd50f8" publishName="f5570db8-dbee-4930-b2e4-4c2046efe36e" id="1"/>
        </transport>
        <transport xmlns="urn:xmpp:jingle:transports:raw-udp:1">
          <candidate xmlns="" ip="50.19.77.101" port="20074" id="1" generation="0" component="1"/>
        </transport>
        <description xmlns="urn:xmpp:jingle:apps:rtp:1" media="audio">
         <payload-type xmlns="" id="9" name="G722" clockrate="8000"/>
         <payload-type xmlns="" id="0" name="PCMU" clockrate="8000"/>
         <payload-type xmlns="" id="116" name="SPEEX" clockrate="16000"/>
         <payload-type xmlns="" id="101" name="telephone-event" clockrate="8000"/>
         <payload-type xmlns="" id="115" name="SPEEX" clockrate="8000"/>
         <encryption>
           <crypto crypto-suite="AES_CM_128_HMAC_SHA1_80" key-params="inline:znX9Mr21swC7JMn54uQqWBtlm42DMz0LCu1Iabub" tag="1">
           </crypto>
           <crypto crypto-suite="AES_CM_128_HMAC_SHA1_32" key-params="inline:ifHPKYX9hV2rfZbS6C+dxNWr9aVwPhCxF817oCPz" tag="2">
           </crypto>
         </encryption>
        </description>
       </content>
      </jingle>
     </iq>
     */
    
    NSLog(@" got -> %@",[iq XMLString]);


    NSXMLElement *sid = [self xp0:iq q:@"jingle:jingle[@action=\"session-initiate\"]/@sid"];
    NSArray * candidates = [self xpns:iq q:@"jingle:jingle[@action=\"session-initiate\"]/pjingle:content/udp:transport/pudp:candidate"];
    NSXMLElement *candidate = nil;
    if (candidates != nil) {
        for (int i=0; i<[candidates count]; i++){
            NSLog(@"candidate -> %@",[((NSXMLElement *)[candidates objectAtIndex:i]) XMLString]);
            if (candidate == nil){
                candidate = (NSXMLElement *)[candidates objectAtIndex:i];
            }
        }
    } 
    NSXMLElement *payload = nil;
    NSString *xpath = [NSString stringWithFormat:@"jingle:jingle[@action=\"session-initiate\"]/pjingle:content/rtp:description[@media=\"audio\"]/prtp:payload-type%@", payloadAttrFilter];
    
    NSArray * payloads = [self xpns:iq q:xpath];
    if (payloads != nil) {
        for (int i=0; i<[payloads count]; i++){
            NSLog(@"payload -> %@",[((NSXMLElement *)[payloads objectAtIndex:i]) XMLString]);
            if (payload == nil){
                payload = [payloads objectAtIndex:i];
            }
        }
    }       
    
    NSArray *cels = [self xpns:iq q:@"jingle:jingle[@action=\"session-initiate\"]/pjingle:content/rtp:description[@media=\"audio\"]/prtp:encryption/prtp:crypto"];
    NSXMLElement *creq = [self xp0:iq q:@"jingle:jingle[@action=\"session-accept\"]/pjingle:content/rtp:description[@media=\"audio\"]/prtp:encryption/@required]"];
    
    if ((sid != nil) && (payload != nil) && (candidate != nil)){
        // say we will think about it.
        ret = [self sendResultAck:iq];
        // and tell the user:
        NSString *ssid =[sid stringValue];
        XMPPJID *sfrom = [iq from];
        XMPPJID *sto = [iq to];
        NSString *creqI = [creq stringValue];
        NSMutableArray * cla = [NSMutableArray arrayWithCapacity:[cels count]];
        for (NSXMLElement *ce in cels){
            [cla addObject:[ce attributesAsDictionary]];
        }
        
        [multicastDelegate xmppJingle:self didReceiveIncommingAudioCall:ssid from:sfrom to:sto transport:candidate sdp:payload cryptoRequired:creqI cryptoElements:cla ];

    } else {
        // nothing we can understand...
        ret = [self sendResultError:iq because:SERVICEUNAVAIL];
    }


    return ret;
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark XMPPStream Delegate
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Delegate method to receive incoming IQ stanzas.
 **/
- (BOOL)xmppStream:(XMPPStream *)sender didReceiveIQ:(XMPPIQ *)iq
{
    XMPPIQ *rep = nil;
    if ([iq isSetIQ]){
        NSString *to = [iq toStr];
        NSString *from = [iq fromStr];
        NSXMLElement * jingle  = [iq elementForName:@"jingle" xmlns:NS_JINGLE];
        if (jingle != nil) {
            NSString *action = [jingle attributeStringValueForName:@"action"];
            NSLog(@"jingle action %@ from %@ to %@",action,from,to );
            if ([action compare:@"session-initiate"] == NSOrderedSame ){
                rep = [self didRecvSessionInitiate:sender iq:iq];
            }
            if ([action compare:@"session-terminate"] == NSOrderedSame ){
                rep = [self didRecvSessionTerminate:sender iq:iq];
            }
            if ([action compare:@"session-accept"] == NSOrderedSame ){
                rep = [self didRecvSessionAccept:sender iq:iq];
            }
            if ([action compare:@"session-info"] == NSOrderedSame ){
                rep = [self didRecvSessionInfo:sender iq:iq];
            }
            // say what we have to say....
            if (rep != nil) {
                [sender sendElement:rep];
            }
        }
	} else if ([iq isResultIQ]){
        NSString * sid = [unAcked objectForKey:[iq elementID]];
        if (sid != nil){
            [multicastDelegate xmppJingle:self didReceiveResult:sid];
            [unAcked removeObjectForKey:[iq elementID]];
        }
    } else if ([iq isErrorIQ]){
        NSString * sid = [unAcked objectForKey:[iq elementID]];
        if (sid != nil){
            NSXMLElement *err = [iq elementForName:@"error"];
            [multicastDelegate xmppJingle:self didReceiveError:sid error:err];
            [unAcked removeObjectForKey:[iq elementID]];
        }
    }
	return (rep != nil);
}
@end
