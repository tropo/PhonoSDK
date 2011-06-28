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

#import "UlawCodec.h"
#import "ulaw.h"


@implementation UlawCodec
- (NSString *) getName{
    return @"ULAW";
}
- (NSInteger) getRate{
    return 8000;
}

- (BOOL) decode:(NSData *)wireData audioData:(NSMutableData *)audioData{
    int len = [wireData length];

    int count=0;
    [audioData setLength:(len*2)];
    uint8_t * wire = (uint8_t *) [wireData bytes];
    int16_t *audio = (int16_t *) [audioData mutableBytes];
    for (count =0; count <len ; count++){
        audio[count] =  ulaw_decode [(int)wire[count]];

    }
    return YES;
}

- (int) ptype{
    return 0;
}

- (BOOL) encode:(NSData *)audioData wireData:(NSMutableData *)wireData{
    int len = [audioData length]/2;
    int count = len;
    [wireData setLength:len];
    uint8_t * wire = (uint8_t *) [wireData mutableBytes];
    int16_t *audio = (int16_t *) [audioData bytes];    
    while (--count >= 0)
    {	
        if (audio[count] >= 0)
            wire[count] = ulaw_encode [audio[count] / 4] ;
        else
            wire[count] = 0x7F & ulaw_encode [audio[count] / -4] ;
    } 
    
    return YES;
}
@end
