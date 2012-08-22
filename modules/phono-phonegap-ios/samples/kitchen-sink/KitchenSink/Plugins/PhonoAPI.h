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
#import "AVFoundation/AVAudioPlayer.h"
@class PhonoAudio;
/*
 v1
allocateEndpoint() -> uri
Allocate a local RTP endpoint uri, rtp://ipaddress:port
freeEndpoint(uri)
Free a given local RTP endpoint
share(uri, autoPlay, codec) -> Share
Start to send audio from the localUri to the remoteUri {bi-directionally? - thp}
play(uri, autoPlay) -> Play
Start to play audio received on the local Uri,
or from a remote http Uri (used for mp3 ringtones etc).
codecs() -> Codec[]
Return an array of codecs supported by the plugin {JSON array of property objects thp}
*/
@interface PhonoAPI : NSObject <AVAudioPlayerDelegate> {
    NSMutableDictionary *endpoints;
    NSMutableDictionary *players;
    PhonoAudio *audio;
    BOOL useSpeakerForCall;

}

+ (NSString *) mkKey;

- (void) setUseSpeakerForCall:(BOOL)val;

- (NSString *) allocateEndpoint;
- (BOOL) freeEndpoint:(NSString *)uri;
- (NSString *) share:(NSString *)uri autoplay:(BOOL)autoplay codec:(NSString *)codec srtpType:(NSString *)srtpType srtpKeyL:(NSString *)srtpKeyL srtpKeyR:(NSString *)srtpKeyR;
- (NSString *) play:(NSString *)uri autoplay:(BOOL)autoplay;
- (NSString *) codecs;
- (NSArray *) codecArray;
- (NSString *) energy:(NSString *)uri;

- (BOOL) start:(NSString *)uri;
- (BOOL) stop:(NSString *)uri;
- (BOOL) gain:(NSString *)uri value:(float) value;
- (BOOL) mute:(NSString *)uri value:(BOOL)value;
- (BOOL) digit:(NSString *)uri digit:(NSString *)digit duration:(int)duration audible:(BOOL)audible;

// test use.
- (NSString *) setEndpoint:(NSString *)luri;

@end
