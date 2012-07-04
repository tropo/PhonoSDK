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

#import "PhonoAudio.h"
#import "UlawCodec.h"
#import "GSM610Codec.h"
#import "G722Codec.h"
#import "SpeexCodec.h"

static int frameIntervalMS = 20; 

@implementation PhonoAudio
@synthesize inEnergy, outEnergy;

- (id) init {
    [super init];
    
    codecs = [[NSMutableDictionary alloc] init];
/*    id <CodecProtocol> ulaw = [[UlawCodec alloc] init];
    [codecs setObject:ulaw forKey:[NSString stringWithFormat:@"%d",[ulaw ptype]]];
    id <CodecProtocol> gsm = [[GSM610Codec alloc] init];
    [codecs setObject:gsm forKey:[NSString stringWithFormat:@"%d",[gsm ptype]]];
    id <CodecProtocol> g722 = [[G722Codec alloc] init];
    [codecs setObject:g722 forKey:[NSString stringWithFormat:@"%d",[g722 ptype]]]; */
    id <CodecProtocol> speex8 = [[SpeexCodec alloc] initWide:NO];
    [codecs setObject:speex8 forKey:[NSString stringWithFormat:@"%d",[speex8 ptype]]];
    id <CodecProtocol> speex16 = [[SpeexCodec alloc] initWide:YES];
    [codecs setObject:speex16 forKey:[NSString stringWithFormat:@"%d",[speex16 ptype]]];
    playing = NO;
    muted = NO;
    currentDigitDuration = 0;
    [self performSelectorInBackground:@selector(spawnAudio) withObject:nil];
    return self;
}




void audioRootChanged (
                        void                      *inUserData,
                        AudioSessionPropertyID    inPropertyID,
                        UInt32                    inPropertyValueSize,
                        const void                *inPropertyValue
                        ) {
    
    // Ensure that this callback was invoked because of an audio route change
    if (inPropertyID != kAudioSessionProperty_AudioRouteChange) return;
    
    // This callback, being outside the implementation block, needs a reference to the MixerHostAudio
    //   object, which it receives in the inUserData parameter. You provide this reference when
    //   registering this callback (see the call to AudioSessionAddPropertyListener).
    PhonoAudio * myself = (PhonoAudio *)inUserData;
    
    // if application sound is not playing, there's nothing to do, so return.
    if (NO == myself->playing) {
        
        NSLog (@"Audio route change while application audio is stopped.");
        return;
        
    } else {
        
        // Determine the specific type of audio route change that occurred.
        CFDictionaryRef routeChangeDictionary = inPropertyValue;
        
        CFNumberRef routeChangeReasonRef =
        CFDictionaryGetValue (
                              routeChangeDictionary,
                              CFSTR (kAudioSession_AudioRouteChangeKey_Reason)
                              );
        
        SInt32 rch;
        
        CFNumberGetValue (
                          routeChangeReasonRef,
                          kCFNumberSInt32Type,
                          &rch
                          );
        
        // "Old device unavailable" indicates that a headset or headphones were unplugged, or that 
        //    the device was removed from a dock connector that supports audio output. In such a case,
        //    pause or stop audio (as advised by the iOS Human Interface Guidelines).
        if (rch == kAudioSessionRouteChangeReason_OldDeviceUnavailable) {
            
            NSLog (@"Audio output device was removed; stopping audio playback.");
        } else {
            char * rchp =(char*) &rch;
            NSLog (@"A route change occurred that does not require stopping application audio. %c%c%c%c"
                   ,*rchp, *(rchp+1),*(rchp+2),*(rchp +3));
        }
    }
}
void interruptionListenerCallback (void *inUserData, UInt32  interruptionState) {
	NSLog(@"interruptionListenerCallback");
}


- (void) reportRate{
    Float64 mHWSampleRate = 0.0;
    UInt32 size = sizeof(mHWSampleRate);
    AudioSessionGetProperty(kAudioSessionProperty_CurrentHardwareSampleRate, &size, &mHWSampleRate);
    
    Float32 mHWBufferDuration = 0.0;
    
    size = sizeof(mHWBufferDuration);
    AudioSessionGetProperty(kAudioSessionProperty_CurrentHardwareIOBufferDuration, &size, &mHWBufferDuration);
    
    NSLog(@" actual HW sample rate is %f and buffer duration is %f",(float)mHWSampleRate,mHWBufferDuration);
    
}



- (void) audioSessionSetSampleRate:(Float64)rate{
    OSStatus err = AudioSessionSetProperty(kAudioSessionProperty_PreferredHardwareSampleRate, sizeof(rate), &rate);
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"kAudioSessionProperty_PreferredHardwareSampleRate ",err);}
    Float32 preferredBufferSize = .02; // I'd like a 20ms buffer duration but I can't have one....
    err =  AudioSessionSetProperty(kAudioSessionProperty_PreferredHardwareIOBufferDuration, sizeof(preferredBufferSize), &preferredBufferSize);
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"kAudioSessionProperty_PreferredHardwareIOBufferDuration ",err); }
    [self reportRate];
}

- (void) setSampleRate:(int) rate{
    AudioStreamBasicDescription asbd;
    memset(&asbd,0,sizeof(asbd));
    asbd.mSampleRate = rate;
    
    asbd.mFormatID = kAudioFormatLinearPCM;
    asbd.mFormatFlags = kLinearPCMFormatFlagIsSignedInteger | kAudioFormatFlagIsPacked ;
    asbd.mFramesPerPacket =1;
    asbd.mChannelsPerFrame = 1;
    asbd.mBitsPerChannel = 16;
    asbd.mBytesPerPacket = asbd.mBytesPerFrame = asbd.mChannelsPerFrame * sizeof (SInt16);
    OSStatus err = AudioUnitSetProperty(vioUnitSpeak, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, 0, &asbd, sizeof(asbd));
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"kAudioUnitProperty_StreamFormat Speak",err); return;}
    
    err = AudioUnitSetProperty(vioUnitMic, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, 1, &asbd, sizeof(asbd));
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"kAudioUnitProperty_StreamFormat Mic",err); return;}
}


- (void) audioSessionStuff{
	OSStatus result = AudioSessionInitialize (CFRunLoopGetCurrent(), kCFRunLoopCommonModes,interruptionListenerCallback,self);
	if (result) printf("ERROR AudioSessionInitialize!\n");
    
    AudioSessionAddPropertyListener(kAudioSessionProperty_AudioRouteChange, audioRootChanged, self);
    
	UInt32 sessionCategory = kAudioSessionCategory_PlayAndRecord; 
	
    result = AudioSessionSetProperty (kAudioSessionProperty_AudioCategory, sizeof (sessionCategory),&sessionCategory);
	if (result) printf("ERROR AudioSessionSetProperty session Category!\n");
    
    
    [self audioSessionSetSampleRate:16000.0];

    result =	AudioSessionSetActive (true); 
	if (result) printf("ERROR AudioSessionSetActive!\n");

    
    UInt32 allowBluetoothInput = 1;
    
    AudioSessionSetProperty (
                             kAudioSessionProperty_OverrideCategoryEnableBluetoothInput,
                             sizeof (allowBluetoothInput),
                             &allowBluetoothInput
                             );
    
    
}

- (void) setSpeaker:(BOOL)use{
    
    UInt32 audioRouteOverride =  use?kAudioSessionOverrideAudioRoute_Speaker:kAudioSessionOverrideAudioRoute_None ;
     
     OSStatus result =	 AudioSessionSetProperty (kAudioSessionProperty_OverrideAudioRoute,                         
     sizeof (audioRouteOverride),
     &audioRouteOverride);
    if (result){ 
        NSLog(@"ERROR AudioSessionSetProperty Speaker route !");
    } else{
        NSLog(@"set speaker to %@", use?@"yes":@"no");
    }
}




- (void) consumeWireData:(NSData*)data time:(NSInteger) stamp{
    if (stopped) {
        NSLog(@"Post call audio data ignored");
        return;
    }
    NSMutableData *din = [[NSMutableData alloc] initWithLength:aframeLen*2];
    if (data != nil){
        if (codec != nil) {
            [codec decode:data audioData:din];
        }
    } else {
        NSLog(@"Padding AudioQueue play buffer with empty buffer");
        memset([din mutableBytes],0,aframeLen*2);
    }
    int16_t *rp =  ringOut;
    int len = ringOutsz;
    int64_t put = putOut;
    if (firstOut) {
        firstOut = NO;
        put += (MAXJITTER *16) ; // in effect insert some blank frames ahead of real data
    }
    int off = 0;
    NSInteger samples = aframeLen;
    int16_t *bp =  (int16_t *) [din mutableBytes];
    double energy = 0.0;
    for (UInt32 j=0;j< samples;j++){
        off = (put % len);
        rp[off] = bp[j];
        put++;
        energy = energy + ABS(bp[j]);
    }
    outEnergy = energy / samples;

    [din release];
    long diff = (stamp -ostamp);

    if ((diff < 0) && (diff > -2000)){
        NSLog(@"out of order %d > %d",ostamp , stamp);
    }
    if ((diff > 20) && (diff < 2000)) {
        NSLog(@"Missing packet ? %d -> %d",ostamp , stamp);
    }
    ostamp = stamp;
    putOut = put;    
    //NSLog(@"put = %d get=%qd put=%qd last took = %qd avail = %qd wanted %ld",aframeLen,getOut,putOut, getOut-getOutold , putOut - getOut, wanted );
    getOutold = getOut;
}
- (NSArray *) listCodecs{
    return [codecs allKeys];
}

- (NSArray *) allCodecs{
    return [codecs allValues];
}

static OSStatus inRender(	
                         void *							inRefCon,
                         AudioUnitRenderActionFlags *	ioActionFlags,
                         const AudioTimeStamp *			inTimeStamp,
                         UInt32							inBusNumber,
                         UInt32							inNumberFrames,
                         AudioBufferList *				ioData){
    
    PhonoAudio *myself = (PhonoAudio *) inRefCon;
    OSStatus err =0;
    
    
    AudioBufferList abl;
     abl.mNumberBuffers = 1;
     abl.mBuffers[0].mNumberChannels = 1;
     abl.mBuffers[0].mData = myself->inslop; 
    abl.mBuffers[0].mDataByteSize = ((inNumberFrames > ENOUGH) ? ENOUGH: inNumberFrames)*2; 
    ioData = &abl; 
     err = AudioUnitRender(myself->vioUnitMic, ioActionFlags, inTimeStamp,1 , inNumberFrames, ioData);
    if (err) { printf("inRender: error %d\n", (int)err); return err; } 
    
    int16_t *rp = myself->ringIn;
    int len = myself->ringInsz;
    int64_t put = myself->putIn;
    int off = 0;
	for(UInt32 i = 0; i < ioData->mNumberBuffers; ++i){
        NSInteger samples = ioData->mBuffers[i].mDataByteSize / 2;
        int16_t *bp = (int16_t *) ioData->mBuffers[i].mData;
        for (UInt32 j=0;j< samples;j++){
            off = (put % len);
            rp[off] = bp[j];
            put++;
        }        
    }
    myself->putIn = put;
    return err;
}

uint32_t toneMap[120][2] = {{1336,941},{1209,697},{1336,697},{1477,696},{1209,770},{1336,770},{1477,770},{1209,852},{1336,852},{1447,852},{1209,941},{1477,941}};

uint16_t getDigitSample(char digit, uint64_t position, uint64_t rate) {
    float n1 = (2*M_PI) * toneMap[digit][0] / rate;
    float n2 = (2*M_PI) * toneMap[digit][1] / rate;
    return ((sin(position*n1) + sin(position*n2))/4)*32768;
}

static OSStatus outRender(	
                          void *							inRefCon,
                          AudioUnitRenderActionFlags *	ioActionFlags,
                          const AudioTimeStamp *			inTimeStamp,
                          UInt32							inBusNumber,
                          UInt32							inNumberFrames,
                          AudioBufferList *				ioData){
    PhonoAudio *myself = (PhonoAudio *) inRefCon;
    int64_t get = myself->getOut;
    int64_t put = myself->putOut;
    if ((put - get) < inNumberFrames) {
        memset((void *) ioData->mBuffers[0].mData,0,ioData->mBuffers[0].mDataByteSize);
        //NSLog(@" No data to be sent to speaker - filling with silence %qd %qd",get,put);

    } else {
        int len = myself ->ringOutsz;
        int off = 0;
        int16_t *rp = myself->ringOut;
        
        int i=0;
        NSInteger samples = ioData->mBuffers[i].mDataByteSize / 2;
        uint16_t *bp = (uint16_t *) ioData->mBuffers[i].mData;
        for (UInt32 j=0;j< samples;j++){
            off = (get % len);
            if (myself->currentDigitDuration > 0) {
                bp[j] = getDigitSample(myself->currentDigit, get, [myself->codec getRate]) + rp[off]/2; 
                myself->currentDigitDuration-=1;
            }
            else bp[j] = rp[off];
            get++;
            //if (get > target) break;
        }        
        myself ->wanted = inNumberFrames;
        if (inNumberFrames != (get -  myself->getOut)){
            NSLog(@" out problem with counting %ld != %qd", inNumberFrames , (get -  myself->getOut));
        }
        if (ioData->mNumberBuffers != 1){
            NSLog(@" out problem with number of buffers %ld", ioData->mNumberBuffers);

        }
        myself->getOut = get;
        
    }    
   
    return 0;
}




- (void) setUpAU{
    OSStatus err =0; 
    // configure audio unit here 
    AudioComponentDescription ioUnitDescription;
    inRenderProc.inputProc = inRender;
    inRenderProc.inputProcRefCon = self;
    outRenderProc.inputProc = outRender;
    outRenderProc.inputProcRefCon = self;    
    memset(&ioUnitDescription,0,sizeof(ioUnitDescription));

    
    ioUnitDescription.componentType          = kAudioUnitType_Output;
    ioUnitDescription.componentSubType       = kAudioUnitSubType_VoiceProcessingIO ;
    ioUnitDescription.componentManufacturer  = kAudioUnitManufacturer_Apple;
    ioUnitDescription.componentFlags         = 0;
    ioUnitDescription.componentFlagsMask     = 0;
    
    AudioComponent foundIoUnitReference = AudioComponentFindNext (
                                                                  NULL,
                                                                  &ioUnitDescription
                                                                  );

    err = AudioComponentInstanceNew (foundIoUnitReference,&vioUnitSpeak);
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"AudioComponentInstanceNew",err); return;}
    memcpy (&vioUnitMic,&vioUnitSpeak,sizeof(vioUnitSpeak));
    
    /*err = AudioComponentInstanceNew (foundIoUnitReference,&vioUnitMic);
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"AudioComponentInstanceNew",err); return;}
     */
    
    [self setSampleRate:32000];
    
    UInt32 one = 1;
    err = AudioUnitSetProperty(vioUnitMic, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, 1, &one, sizeof(one));
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"kAudioOutputUnitProperty_EnableIO mic",err); return;}

    err = AudioUnitSetProperty(vioUnitSpeak, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, 0, &one, sizeof(one));
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"kAudioOutputUnitProperty_EnableIO speak",err); return;}


    err = AudioUnitSetProperty(vioUnitSpeak, kAudioUnitProperty_SetRenderCallback, kAudioUnitScope_Output, 0, &outRenderProc, sizeof(outRenderProc));
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"kAudioUnitProperty_SetRenderCallback Speak",err); return;}

    err = AudioUnitSetProperty(vioUnitMic, kAudioOutputUnitProperty_SetInputCallback, kAudioUnitScope_Global, 1, &inRenderProc, sizeof(inRenderProc)); 
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"kAudioUnitProperty_SetRenderCallback Mic",err); return;}




    

    err = AudioUnitInitialize(vioUnitSpeak);
    if (err != 0) {
        NSLog(@"Error with %@ - %ld",@"AudioUnitInitialize Speak",err);
        //return;
    }
    
} 


- (void) setUpSendTimer{
	send  = [NSTimer timerWithTimeInterval:0.02 target:self selector:@selector(encodeAndSend) userInfo:nil repeats:YES];
	[audioRunLoop addTimer:send forMode:NSDefaultRunLoopMode];
}
- (void) setUpRingBuffers{
    ringInsz = ENOUGH;
//    ringInD = [[[NSMutableData alloc] initWithLength:(ringInsz*2) ] retain];
//    ringIn = (int16_t *) [ringInD mutableBytes];
//    memset(ringIn,0,ringInsz*2);

    putIn =0;
    getIn =0;
    
    ringOutsz =ENOUGH;
//    ringOutD = [[[NSMutableData alloc] initWithLength:(ringOutsz*2) ]retain];
//    ringOut = (int16_t *) [ringOutD mutableBytes];
    putOut =0; 
    firstOut = YES;// start with some headroom (silent)
    getOut =0;
    getOutold =0;
}

- (void) setupAudio {
    [self audioSessionStuff ];
    [self setUpRingBuffers];
    [self setUpAU];
    [self setUpSendTimer];
}
- (void)spawnAudio {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    // Do thread work here.
	if ([NSThread isMultiThreaded]){
		[NSThread setThreadPriority:1.0];
        audioThread = [NSThread currentThread];
        NSLog(@"Audio thread Started");
	}
	audioRunLoop = [NSRunLoop currentRunLoop];
    wout = [[NSMutableData  alloc] initWithCapacity:160]; // we put the wire data here before sending it. 
    [self setupAudio];
	[audioRunLoop run];
    [pool release];
	[NSThread exit];
}
-(void) encodeAndSend{
    
    if (stopped) return;
    
    int64_t get = getIn;
    int64_t avail  = putIn - get;
   // NSLog(@"avail = %qd",avail);
    //int64_t count = 0;
    if (avail >= aframeLen) {
        // got enough to send a packet
        //if (count >0) NSLog(@"taken = %d get=%qd put=%qd count=%qd",aframeLen,getIn,putIn,count );
        //count++;

        NSData * dts = nil ; 
        if (muted) {
            uint8_t *zeros = alloca(aframeLen*2);
            memset(zeros,0,aframeLen*2);
            dts = [NSData dataWithBytes:zeros length:aframeLen*2];
            get += aframeLen;
        } else {
            int16_t *audio = alloca(aframeLen*2);
            int len = ringInsz;
            double energy = 0.0;
            for (int i=0;i<aframeLen;i++){
                int offs = get % len;
                audio[i] = ringIn[offs];
                energy += ABS(ringIn[offs]);
                get++;
            }
            inEnergy = energy / aframeLen;
            dts = [NSData dataWithBytes:audio length:aframeLen*2];
        }
        [codec encode:dts  wireData:wout];
        [wire consumeAudioData:wout time:(sent*frameIntervalMS)];
        sent++;
        getIn =  get; 
        avail  = putIn - get;
    }
}
- (BOOL) setCodec:(NSString *) codecname{
    // we support 2 formats here - just an integer ptype
    // or name:rate:ptype
    // in the second case we will make sure we use the requested
    // ptype on the wire even if it wasn't  the value in our
    // codec array - speex for example.
    // see which format we have
    NSInteger ptype = 0;
    NSArray *bits = [codecname componentsSeparatedByString:@":"];
    if ([bits count] == 3){
        NSString *nam = [bits objectAtIndex:0];
        NSInteger rate = [[bits objectAtIndex:1] integerValue];
        //  stupid 722 crap.
        if ([nam compare:@"G722"] == NSOrderedSame) {
            rate = 16000;
        }
        ptype = [[bits objectAtIndex:2] integerValue];

        NSEnumerator *coan = [codecs objectEnumerator ];
        id <CodecProtocol> can = nil;
        while ((nil != (can = (id <CodecProtocol>)[coan nextObject]))){
            if (([[can getName] compare:nam] == NSOrderedSame) && (rate == [can getRate])){
                codec = can;
            }
        }
    } else {
        codec = [codecs objectForKey:codecname];
        ptype = [codec ptype];
    }
    if (codec != nil){
        int fac = [codec getRate] /1000;
        aframeLen = (frameIntervalMS * [codec getRate] )/1000; // unit is _shorts_ 
        playing = YES;
        [wire setCodecfac:fac];
        [wire setPtype:ptype];
        [self audioSessionSetSampleRate:[codec  getRate]];
        [self setSampleRate:[codec getRate]];
    }
    
    NSLog(@"Using %@ to set codec to %@ at %d ptype %d - res = %@",codecname, [codec getName],[codec getRate],ptype, ((codec != nil)?@"Yes":@"NO"));
    
    return (codec != nil);
}

- (void) start{
    OSStatus err =0; 
    memset(inslop,0,sizeof(inslop));
    memset(ringIn,0,sizeof(ringIn));
    memset(ringOut,0,sizeof(ringOut));

    err = AudioOutputUnitStart(vioUnitSpeak);
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"AudioOutputUnitStart Speak",err);}
    stopped = NO;
}


- (void) stop{
    OSStatus err =0; 
    
    //AudioOutputUnitStop(vioUnitMic);
    err =  AudioOutputUnitStop(vioUnitSpeak);
    if (err != 0) { NSLog(@"Error with %@ - %ld",@"AudioOutputUnitStop Speak",err);}
    
    stopped = YES;
}


- (void) mute:(BOOL)v{
    muted = v;
}

- (void) digit:(NSString*) digit duration:(int) duration{
    currentDigit = [[digit capitalizedString] characterAtIndex:0];
    if (currentDigit == '*') currentDigit = 10;
    else if (currentDigit == '#') currentDigit = 11;
    else if (currentDigit >= '0' && currentDigit <= '9') currentDigit = currentDigit - '0'; // offset to 0
    else return;
    currentDigitDuration = duration*[codec getRate]/1000;
    NSLog(@"digit=%d",currentDigit);
}

- (void) setWireConsumer:(id<AudioDataConsumer>)w{
    wire = w;
}

- (void)dealloc {
    [codecs removeAllObjects];
    [codecs release];
    [audioThread release];
    [self stop];
    [super dealloc];
}

@end