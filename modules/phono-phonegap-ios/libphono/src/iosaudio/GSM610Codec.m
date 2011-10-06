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

#import "GSM610Codec.h"


@implementation GSM610Codec
- (id) init{
    [super init];
    encoder_st = gsm_create();
    decoder_st = gsm_create();
    return self;
}

- (NSString *) getName{
    return @"GSM";
}
- (NSInteger) getRate{
    return 8000;
}

- (BOOL) decode:(NSData *)wireData audioData:(NSMutableData *)audioData{
    [audioData setLength:320];
    uint8_t * wire = (uint8_t *) [wireData bytes];
    int16_t *audio = (int16_t *) [audioData mutableBytes];
    gsm_decode(decoder_st, wire, audio);
    return YES;
}

- (int) ptype{
    return 3;
}

- (BOOL) encode:(NSData *)audioData wireData:(NSMutableData *)wireData{
    [wireData setLength:33];
    uint8_t * wire = (uint8_t *) [wireData mutableBytes];
    int16_t *audio = (int16_t *) [audioData bytes];    
    gsm_encode(encoder_st, audio, wire);
    
    return YES;
}
- (void)dealloc {
    gsm_destroy(encoder_st);
    gsm_destroy(decoder_st);
    encoder_st = nil;
    decoder_st = nil;
    [super dealloc];
}
@end
