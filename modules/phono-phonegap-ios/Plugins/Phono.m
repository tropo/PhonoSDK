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

#import "Phono.h"
#import "PhonoAPI.h"


@implementation Phono

-(PGPlugin*) initWithWebView:(UIWebView*)theWebView{
    self = (id) [super initWithWebView:theWebView];
    phonoAPI = [[PhonoAPI alloc] init];
    return self;
}
-(PGPlugin*) initWithWebView:(UIWebView*)theWebView settings:(NSDictionary*)classSettings{
    self = (id) [super initWithWebView:theWebView settings:classSettings];
    phonoAPI = [[PhonoAPI alloc] init];
    return self; 
}


- (void) allocateEndpoint:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSString *uri = nil;
    NSUInteger argc = [arguments count];
    NSString *successCallback = [arguments objectAtIndex:0];
    NSString *cb = nil;
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    uri = [phonoAPI allocateEndpoint];
    if (uri == nil){
        uri = @"problem with phono API";
    } 
    cb = [[uri substringToIndex:6] isEqualToString:@"rtp://"] ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,uri];
        [self writeJavascript:jsCB];
    }
}

- (void) freeEndpoint:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    NSString *cb = nil;

    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString *uri = [options objectForKey:@"uri"];
    BOOL res = [phonoAPI freeEndpoint:uri];
    if (res == NO){
        uri = @"Can't find that endpoint";
    } 
    cb = res ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,uri];
        [self writeJavascript:jsCB];
    }
}

- (void) share:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    
    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString *uri = [options objectForKey:@"uri"];
    NSString *autoplay = [options objectForKey:@"autoplay"];
    NSString *codec = [options objectForKey:@"codec"];
    NSString *luri = [phonoAPI share:uri autoplay:[autoplay isEqualToString:@"YES"] codec:codec];

    NSString *cb = [[luri substringToIndex:6] isEqualToString:@"rtp://"] ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,luri];
        [self writeJavascript:jsCB];
    }
}
- (void) play:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    
    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString *uri = [options objectForKey:@"uri"];
    NSString *autoplay = [options objectForKey:@"autoplay"];
    NSString *luri = [phonoAPI play:uri autoplay:[autoplay isEqualToString:@"YES"]];
    
    NSString *cb = [[luri substringToIndex:6] isEqualToString:@"file://"] ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,luri];
        [self writeJavascript:jsCB];
    }
}
- (void) start:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    NSString *cb = nil;
    
    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString *uri = [options objectForKey:@"uri"];
    BOOL res = [phonoAPI start:uri];
    if (res == NO){
        uri = @"Can't start that endpoint";
    } 
    cb = res ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,uri];
        [self writeJavascript:jsCB];
    }
}
- (void) stop:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    NSString *cb = nil;
    
    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString *uri = [options objectForKey:@"uri"];
    BOOL res = [phonoAPI stop:uri];
    if (res == NO){
        uri = @"Can't stop that endpoint";
    } 
    cb = res ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,uri];
        [self writeJavascript:jsCB];
    }
}

- (void) gain:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    NSString *cb = nil;
    
    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString *uri = [options objectForKey:@"uri"];
    NSString *value = [options objectForKey:@"value"];
    
    BOOL res = [phonoAPI gain:uri value:([value floatValue]/100.0)];
    if (res == NO){
        uri = @"Can't set gain on that endpoint";
    } 
    cb = res ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,uri];
        [self writeJavascript:jsCB];
    }
}
- (void) mute:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    NSString *cb = nil;
    
    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString *uri = [options objectForKey:@"uri"];
    NSString *value = [options objectForKey:@"value"];

    BOOL res = [phonoAPI mute:uri value:[value isEqualToString:@"YES"]];
    if (res == NO){
        uri = @"Can't (un)mute that endpoint";
    } 
    cb = res ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,uri];
        [self writeJavascript:jsCB];
    }
}
- (void) digit:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    NSString *cb = nil;
    
    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString *uri = [options objectForKey:@"uri"];
    NSString *digit = [options objectForKey:@"digit"];
    int duration = [[options objectForKey:@"duration"] intValue];
    NSString *au = [options objectForKey:@"audible"];
    NSLog(@"sending %@ to %@ for %d ms autible = %@",digit,uri, duration, au);
    BOOL res = [phonoAPI digit:uri digit:digit duration:duration audible:[au isEqualToString:@"YES"]];
    if (res == NO){
        uri = @"Can't send digits that endpoint";
    } 
    cb = res ?successCallback:failCallback;
    if (cb != nil) {
        NSString *jsCB = [NSString stringWithFormat:@"%@(\"%@\")",cb,uri];
        [self writeJavascript:jsCB];
    }
}
- (void) codecs:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options{
    NSString *failCallback = nil;
    NSUInteger argc = [arguments count];
    
    NSString *successCallback = [arguments objectAtIndex:0];
    if (argc > 1) {
        failCallback = [arguments objectAtIndex:1];
    } 
    NSString * myCodecs = [phonoAPI codecs];
    NSString *jsCB = [NSString stringWithFormat:@"%@(\'%@\')",successCallback,myCodecs];
    [self writeJavascript:jsCB];
}
@end
