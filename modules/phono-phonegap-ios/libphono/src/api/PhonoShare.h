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

#import <Foundation/Foundation.h>
#import "PhonoEndpoint.h"
#import "phonoRTP.h"
#import "PhonoAudio.h"
#import "PlayProtocol.h"


@interface PhonoShare : NSObject <PlayProtocol> {
    NSString *nearUri;
    PhonoEndpoint *endpoint;
    NSString *codec;
    NSString *rhost;
    NSInteger rport;
    phonoRTP *rtp;
    PhonoAudio *audio;
    
}

@property (assign,readwrite) PhonoEndpoint * endpoint;
@property (assign,readonly) NSString *nearUri;
@property (assign,readwrite) NSString *codec;

- (id) initWithUri:(NSString *) uri;
- (float) gain:(float) value;
- (BOOL) mute:(BOOL)v;
- (BOOL) doES:(BOOL)v;
- (void) digit:(NSString*) digit duration:(int) duration audible:(BOOL)audible;
- (double) inEnergy;
- (double) outEnergy;
- (double) jitter;
- (double) latency;
@end
