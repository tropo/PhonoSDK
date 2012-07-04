/*
 * Copyright 2011 Voxeo Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#import "PhonoAPI.h"
#import "PhonoShare.h"
#import "PhonoEndpoint.h"
#import "PhonoAudio.h"
#import "CJSONSerializer.h"
#import "CodecProtocol.h"


@implementation PhonoAPI

- (void) setUseSpeakerForCall:(BOOL)val{
    if(val != useSpeakerForCall){
        [audio setSpeaker:val];
        useSpeakerForCall = val;
    }
}


- (id) init{
    self = [super init];
    endpoints = [[NSMutableDictionary alloc] init];
    players = [[NSMutableDictionary alloc] init];
    audio = [[PhonoAudio alloc] init];
    return self;
    
}

- (NSString *) allocateEndpoint{
    PhonoEndpoint *nep = [[PhonoEndpoint alloc] init];
    NSString * nepuri = [nep uri];
    [endpoints setObject:nep forKey:nepuri];
    return nepuri;
}

- (NSString *) setEndpoint:(NSString *)luri{
    PhonoEndpoint *nep = [[PhonoEndpoint alloc] initWithUri:luri];
    NSString * nepuri = [nep uri];
    [endpoints setObject:nep forKey:nepuri];
    return nepuri;
}

- (BOOL) freeEndpoint:(NSString *)uri{
    PhonoEndpoint *nep = [endpoints objectForKey:uri];
    if (nep != nil){
        [nep close];
        [endpoints removeObjectForKey:uri];
    }
    return (nep == nil)?NO:YES;
}
/**
 *
 * @param uri rtp://localhost:port<:remotehost:remoteport>
 * @param autoPlay start immediatly
 * @param codec Selected codec.
 * @return
 */
- (NSString *) share:(NSString *)uri autoplay:(BOOL)autoplay codec:(NSString *)codec srtpType:(NSString *)srtpType srtpKey:(NSString *)srtpKey{
    PhonoShare *ps = [[PhonoShare alloc] initWithUri:uri];
    NSString *luri = [ps nearUri]; 
    [ps setSrtpType:srtpType];
    [ps setMasterKey:srtpKey];
    PhonoEndpoint *ep = [endpoints objectForKey:luri];
    if (ep == nil){
        NSLog(@"Can't find local endpoint %@ creating it",luri );
        ep = [[PhonoEndpoint alloc] initWithUri:luri];
        luri = [ep uri];
        [endpoints setObject:ep forKey:luri];
    }
    [ps setEndpoint:ep];
    [ps setAudio:audio];
    [ps setCodec:[[NSString alloc] initWithString:codec ]];
    [ep setPlayer:ps];
    
    if (autoplay == YES) {
        [ps start];
    }
    return luri;
}

- (NSString *) play:(NSString *)uri autoplay:(BOOL)autoplay{
    NSString * ret ;
    NSError *error = NULL;
    NSLog(@"playing %@",uri);

    
    NSURL *url = [[NSURL alloc ] initWithString:uri];
    
    AVAudioPlayer *av = [[AVAudioPlayer alloc ] initWithContentsOfURL:url error:&error];
    [av setNumberOfLoops:-1];
    [av setDelegate:self];
    if (error){
        ret = [error localizedDescription];
        NSLog(@"Play error on %@ of %@",uri,ret);
    } else {
        ret = [[av url] absoluteString] ;
        [players setObject: av forKey:ret];
        NSLog(@"adding %@",uri);

        if (autoplay) {
            [av play];
            NSLog(@"playing %@",uri);
        }
    }
    
    return ret;
}



- (BOOL) start:(NSString *)uri{
    NSLog(@"starting %@",uri);

    PhonoEndpoint *ep = [endpoints objectForKey:uri];
    AVAudioPlayer *av = nil;
    if (ep != nil) {
        PhonoShare *ps = (PhonoShare *) [ep player];
        [audio setSpeaker:useSpeakerForCall];
        [ps start];
    } else {
        av = [players objectForKey:uri];
        [av setNumberOfLoops:-1]; //repeat indefinitely
        [audio setSpeaker:YES];
        [av play];
    }
    return (av != nil) || (ep != nil);
}

- (BOOL) digit:(NSString *)uri digit:(NSString *)digit duration:(int)duration audible:(BOOL)audible{
    NSLog(@"digit %@ on  %@",digit,uri);
    PhonoEndpoint *ep = [endpoints objectForKey:uri];
    if (ep != nil) {
        PhonoShare *ps = (PhonoShare *) [ep player];
        [ps digit:digit duration:duration audible:audible];
    } 
    return (ep != nil);

}

- (BOOL) stop:(NSString *)uri{
    NSLog(@"stoping %@",uri);
    PhonoEndpoint *ep = [endpoints objectForKey:uri];
    AVAudioPlayer *av = nil;
    if (ep != nil) {
        PhonoShare *ps = (PhonoShare *) [ep player];
        [ps stop];
    } else {
        av = [players objectForKey:uri];
        [av stop];
    }
    return (av != nil) || (ep != nil);
}
- (BOOL) gain:(NSString *)uri value:(float) value{
    PhonoEndpoint *ep = [endpoints objectForKey:uri];
    AVAudioPlayer *av = nil;
    if (ep != nil) {
        PhonoShare *ps = (PhonoShare *) [ep player];
        [ps gain:value];
    } else {
        av = [players objectForKey:uri];
        [av setVolume:value];
    }
    return (av != nil) || (ep != nil);
}
- (BOOL) mute:(NSString *)uri value:(BOOL)value{
    PhonoEndpoint *ep = [endpoints objectForKey:uri];
    if (ep != nil) {
        PhonoShare *ps = (PhonoShare *) [ep player];
        [ps mute:value];
    }
    return (ep != nil);
}

- (NSString *) energy:(NSString *)uri {
    NSString *ret = nil;

    PhonoEndpoint *ep = [endpoints objectForKey:uri];
    if (ep != nil) {
        PhonoShare *ps = (PhonoShare *) [ep player];
        if (ps != nil){
            NSMutableArray * en = [[NSMutableArray alloc] init];
            [en addObject:[NSNumber numberWithDouble:[ps inEnergy]]];
            [en addObject: [NSNumber numberWithDouble:[ps outEnergy]]];

            NSError *error = NULL;
            NSData *jsonData = [[CJSONSerializer serializer] serializeObject:en error:&error];
            ret = [[[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding] autorelease];
            NSLog(@"energy json->\n%@", ret);
        }
    }
    return ret;
}

- (NSArray *) codecArray{
    NSArray *cs = [audio allCodecs];
    
    NSMutableArray *ca = [[NSMutableArray alloc] initWithCapacity:[cs count]];
    
    for (id <CodecProtocol> c in cs){
        NSMutableDictionary * co = [[NSMutableDictionary alloc] init];
        NSString *name = [c getName];
        NSString *rate = [name isEqualToString:@"G722"]?@"8000":[NSString stringWithFormat:@"%d",[c getRate] ];
        NSString *ptype = [NSString stringWithFormat:@"%d",[c ptype] ];
        
        [co setObject: name forKey:@"name"];
        [co setObject: rate forKey:@"rate"];
        [co setObject: ptype forKey:@"ptype"];
        [ca addObject:co];
    } 
    NSMutableDictionary * co = [[NSMutableDictionary alloc] init];
    [co setObject: @"telephone-event" forKey:@"name"];
    [co setObject: @"8000" forKey:@"rate"];
    [co setObject: @"101" forKey:@"ptype"];
    [ca addObject:co];  
    return ca;
}

- (NSString *) codecs{
    NSString *ret = nil;
    NSError *error = NULL;
    NSArray *ca = [self codecArray];
    NSData *jsonData = [[CJSONSerializer serializer] serializeObject:ca error:&error];
    ret = [[[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding] autorelease];
    NSLog(@"codec json->\n%@", ret);
    return ret;
}

- (void)dealloc {
    [endpoints removeAllObjects];
    [endpoints release];
    [players removeAllObjects];
    [players release];
    [super dealloc];
}

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)av successfully:(BOOL)flag{
// wrong assumption here....
    NSString *uri = [[av url] absoluteString];
    [players removeObjectForKey:uri];
    NSLog(@"removing %@",uri);
 
}
@end

