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

#import "G722Codec.h"


@implementation G722Codec
- (id) init{
    [super init];
    g722_encode_init(&encoder_st, 64000, 0);
    g722_decode_init(&decoder_st, 64000, 0);
    return self;
}

- (NSString *) getName{
    return @"G722";
}
- (NSInteger) getRate{
    return 16000;
}

- (BOOL) decode:(NSData *)wireData audioData:(NSMutableData *)audioData{
    [audioData setLength:640];
    uint8_t * wire = (uint8_t *) [wireData bytes];
    int16_t *audio = (int16_t *) [audioData mutableBytes];
    g722_decode(&decoder_st, audio, wire, 160);
    return YES;
}

- (int) ptype{
    return 9;
}

- (BOOL) encode:(NSData *)audioData wireData:(NSMutableData *)wireData{
    [wireData setLength:160];
    uint8_t * wire = (uint8_t *) [wireData mutableBytes];
    int16_t *audio = (int16_t *) [audioData bytes];    
    g722_encode(&encoder_st, wire, audio, 320);
    return YES;
}
- (void)dealloc {
    // alloc'd in self
    [super dealloc];
}
@end
