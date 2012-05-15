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
#ifdef CORDOVA_FRAMEWORK
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVURLProtocol.h>
#else
#import "CDVPlugin.h"
#import "CDVURLProtocol.h"
#endif

@interface Phono : CDVPlugin {
    id phonoAPI;
}


// allocateEndPoint
// arguments[0]-> success function which will get invoked with a the allocated local uri "rtp://ipaddress:port" 
// arguments[1]-> failure function which will get invoked with an error message. 
// options will be ignored
- (void) allocateEndpoint:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

// freeEndPoint
// arguments[0]-> success function which will get invoked with a the deallocated local uri "rtp://ipaddress:port" 
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain a single entry 'uri' -> "rtp://ipaddress:port" as returned from allocate (or share)
- (void) freeEndpoint:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

// share
// arguments[0]-> success function which will get invoked with a the allocated local uri "rtp://ipaddress:port" 
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain the following key-value pairs (all strings):
//                  'uri' -> "rtp://localipaddress:localport:remoteipaddress:remoteport" 
//                  'autoplay' -> "YES" , if the share should start immediately
//                  'codec' -> "ULAW" , or other supported codec from the list returned by codecs
- (void) share:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

// play
// arguments[0]-> success function which will get invoked with a url "http://somewhere.com/ringing.mp3" 
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain the following key-value pairs (all strings):
//                  'uri' -> "http://somewhere.com/ringing.mp3" 
//                  'autoplay' -> "YES" , if the play should start immediately
- (void) play:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

// start
// arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or "http://somewhere.com/ringing.mp3"
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain the following key-value pairs (all strings):
//                  'uri' -> "rtp://localipaddress:localport" or "http://somewhere.com/ringing.mp3" 
- (void) start:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

// stop
// arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or "http://somewhere.com/ringing.mp3"
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain the following key-value pairs (all strings):
//                  'uri' -> "rtp://localipaddress:localport" or "http://somewhere.com/ringing.mp3" 
- (void) stop:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

// gain
// arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or "http://somewhere.com/ringing.mp3"
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain the following key-value pairs (all strings):
//                  'uri' -> "rtp://localipaddress:localport" or "http://somewhere.com/ringing.mp3" 
//                  'value' -> "70.5" % gain
- (void) gain:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;
// mute
// arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or "http://somewhere.com/ringing.mp3"
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain the following key-value pairs (all strings):
//                  'uri' -> "rtp://localipaddress:localport" or "http://somewhere.com/ringing.mp3" 
//                  'value' -> "YES" or "NO"
- (void) mute:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;


// digit
// arguments[0]-> success function which will get invoked with a uri "rtp://ipaddress:port" or 
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain the following key-value pairs (all strings):
//                  'uri' -> "rtp://localipaddress:localport" 
//                  'digit' -> "0" 0-9 etc
//                  'duration' -> "250" duration in milliseconds
//                  'audible' -> "YES" or "NO"

- (void) digit:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

// codecs
// arguments[0]-> success function which will get invoked with a json string containing a sequence of supported codec objects. 
// arguments[1]-> failure function which will get invoked with an error message. 
// options is ignored
//
- (void) codecs:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

// energy
// arguments[0]-> success function which will get invoked with a json string containing an array of energy values. 
// arguments[1]-> failure function which will get invoked with an error message. 
// options must contain the following key-value pairs (all strings):
//                  'uri' -> "rtp://localipaddress:localport" 
//
- (void) energy:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options;

@end
