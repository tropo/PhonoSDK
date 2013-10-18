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

#import "SpeexCodec.h"
#include <speex/speex_callbacks.h>


@implementation SpeexCodec

- (id) init{
    return [self initWide:NO];
}

- (id) initWide:(BOOL)wide{
    [super init];
    wideBand = wide;
    if (wide) {
        decoder_st = speex_decoder_init(&speex_wb_mode);
        encoder_st = speex_encoder_init(&speex_wb_mode);
        int tmp=0;
        speex_encoder_ctl(encoder_st, SPEEX_SET_VBR, &tmp);
        tmp=5;
        speex_encoder_ctl(encoder_st, SPEEX_SET_QUALITY, &tmp);
        tmp=2;
        speex_encoder_ctl(encoder_st, SPEEX_SET_COMPLEXITY, &tmp);
        tmp = 16000;
        speex_encoder_ctl(encoder_st,SPEEX_SET_SAMPLING_RATE,&tmp);  
    } else {
        decoder_st = speex_decoder_init(&speex_nb_mode);
        encoder_st = speex_encoder_init(&speex_nb_mode);
        int tmp=0;
        speex_encoder_ctl(encoder_st, SPEEX_SET_VBR, &tmp);
        tmp=5;
        speex_encoder_ctl(encoder_st, SPEEX_SET_QUALITY, &tmp);
        tmp=1;
        speex_encoder_ctl(encoder_st, SPEEX_SET_COMPLEXITY, &tmp);
        tmp = 8000;
        speex_encoder_ctl(encoder_st,SPEEX_SET_SAMPLING_RATE,&tmp);
    }
	speex_bits_init(&eBits);
	speex_bits_init(&dBits);
    
    
    return self;
}

- (NSString *) getName{
    return @"SPEEX";
}
- (NSInteger) getRate{
    return wideBand ? 16000:8000;
}

- (int) ptype{
    return wideBand?103:102;
}
- (BOOL) decode:(NSData *)wireData audioData:(NSMutableData *)audioData{
    int len = wideBand?640:320;
    [audioData setLength:len];
    uint8_t * wire = (uint8_t *) [wireData bytes];
    int16_t *audio = (int16_t *) [audioData mutableBytes];
    speex_bits_reset(&dBits);
    speex_bits_read_from(&dBits, (char *) wire,[wireData length]);
    speex_decode_int(decoder_st, &dBits, audio);
    return YES;
}


- (BOOL) encode:(NSData *)audioData wireData:(NSMutableData *)wireData{
    [wireData setLength:160];
    uint8_t * wire = (uint8_t *) [wireData mutableBytes];
    int16_t *audio = (int16_t *) [audioData bytes];    
    speex_bits_reset(&eBits);
    speex_encode_int(encoder_st, audio, &eBits);
    int nbBytes = speex_bits_write(&eBits, (char*) wire, 160);
    [wireData setLength:nbBytes];

    return YES;
}
- (void)dealloc {
    speex_encoder_destroy(encoder_st);
    speex_decoder_destroy(decoder_st);
    encoder_st = nil;
    decoder_st = nil;
    [super dealloc];
}
@end
