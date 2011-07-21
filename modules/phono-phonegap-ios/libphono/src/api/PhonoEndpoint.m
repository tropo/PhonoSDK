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

#import "PhonoEndpoint.h"
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <arpa/inet.h>

@implementation PhonoEndpoint
@synthesize uri, sock , player;

- (void) setPlayer:(id)play {
    player = play;
}

- (NSString *) findLocalIP
{
    static struct ifreq ifreqs[4000];
    struct ifconf ifconf;
    struct ifreq * ifr;
    int  nifaces, i;
    int lsock, rval;
    NSString *ret = nil;
    
    lsock = CFSocketGetNative(sock);
    
    memset(&ifconf,0,sizeof(ifconf));
    ifconf.ifc_buf = (char*) (ifreqs);
    ifconf.ifc_len = sizeof(ifreqs);
    
    if((rval = ioctl(lsock, SIOCGIFCONF , (char*) &ifconf  )) < 0 ){
        perror("ioctl(SIOGIFCONF)");
    } else {
        nifaces  =0;
        for (i = 0; i < ifconf.ifc_len; ) {
            /* Get a pointer to the current interface. */
            ifr = (struct ifreq *) &(ifconf.ifc_buf[i]);
            i += sizeof(struct ifreq);
            if (ifr->ifr_addr.sa_len > sizeof(ifr->ifr_addr))
                i += ifr->ifr_addr.sa_len - sizeof(struct sockaddr);
            nifaces ++;

            NSLog(@"Interface %d %s",nifaces, ifr->ifr_name);
           
            if (ioctl(lsock, SIOCGIFFLAGS, ifr) == 0) {
                short flags = ifr->ifr_ifru.ifru_flags;
                if (!( flags & IFF_UP)) {
                    NSLog(@"\t %d is Down",nifaces);
                    continue;
                }
                if ( flags & IFF_LOOPBACK) {
                    NSLog(@"\t %d is Loopback",nifaces);
                    continue;
                }
            } else {
                NSLog(@"no flags associated with %d",nifaces);
            }
            if (ioctl(lsock, SIOCGIFADDR, ifr) == 0) {
                if (ifr->ifr_ifru.ifru_addr.sa_family != AF_INET) {
                    NSLog(@"%d not a INET interface",nifaces);
                } else {
                    struct sockaddr_in *lad = (struct sockaddr_in *)&(ifr->ifr_ifru.ifru_addr);
                    NSLog(@"got an INET interface %s",inet_ntoa(lad->sin_addr));
                    if (ret == nil) {
                        ret = [[NSString alloc] initWithCString:inet_ntoa(lad->sin_addr)];
                        NSLog(@"using %s",inet_ntoa(lad->sin_addr));

                    }
                }
            } else {
                NSLog(@"no address associated with %d",nifaces);
            }
        }
    }
    if (ret == nil){
        NSLog(@"Making up an ipaddress...");
        ret = [[NSString alloc] initWithString:@"192.168.0.11"]; // well it is 'something'
    }
    return ret;
}

- (id) init{
    self = [super init];
    sock = CFSocketCreate (kCFAllocatorDefault, //CFAllocatorRef allocator,
                           PF_INET, //            SInt32 protocolFamily,
                           SOCK_DGRAM,//            SInt32 socketType,
                           IPPROTO_UDP,//            SInt32 protocol,
                              kCFSocketNoCallBack,//            CFOptionFlags callBackTypes,
                              NULL,//         CFSocketCallBack callout,
                               NULL//        const CFSocketContext *context
                                       );// note - the share will update the context and callback.

    struct sockaddr_in lad = {0};
    
    CFDataRef anywhere = CFDataCreate(kCFAllocatorDefault, (uint8_t *)&lad, sizeof(lad));
    
    CFSocketError err = CFSocketSetAddress (sock ,anywhere);
    
    CFDataRef loc =  CFSocketCopyAddress (sock);
    struct sockaddr_in *local = (struct sockaddr_in *) CFDataGetBytePtr(loc);

    NSString * addr = [self findLocalIP];
    uri = [[NSString alloc] initWithFormat:@"rtp://%@:%d", addr, ntohs(local->sin_port)];
    return self;
    
}

- (id) initWithUri:(NSString *)luri{
    BOOL ok;
    self = [super init];
    NSArray *parts;
    NSString *host;
    NSInteger port;
    ok = [@"rtp://" isEqualToString:[luri substringToIndex:6]];
    if (ok) {
        parts = [[luri substringFromIndex:6] componentsSeparatedByString:@":"];
        ok = ([parts count] == 2);
    }
    if (ok) {
        host = [parts objectAtIndex:0]; // had better be an ipaddress 
        port = [[parts objectAtIndex:1] intValue];
    }
    sock = CFSocketCreate (kCFAllocatorDefault, //CFAllocatorRef allocator,
                           PF_INET, //            SInt32 protocolFamily,
                           SOCK_DGRAM,//            SInt32 socketType,
                           IPPROTO_UDP,//            SInt32 protocol,
                           kCFSocketNoCallBack,//            CFOptionFlags callBackTypes,
                           NULL,//         CFSocketCallBack callout,
                           NULL//        const CFSocketContext *context
                           );// note - the share will update the context and callback.
    struct sockaddr_in lad = {0};
    if(ok){
        lad.sin_family = AF_INET;
        lad.sin_port = htons(port);
        inet_aton([host cStringUsingEncoding:NSASCIIStringEncoding],&(lad.sin_addr));
    }
    CFDataRef here = CFDataCreate(kCFAllocatorDefault, (uint8_t *)&lad, sizeof(lad));
    
    CFSocketError err = CFSocketSetAddress (sock ,here);
    if (err ==    kCFSocketSuccess) {
        CFDataRef loc =  CFSocketCopyAddress (sock);
        struct sockaddr_in *local = (struct sockaddr_in *) CFDataGetBytePtr(loc);
        
        char *addr = inet_ntoa(local->sin_addr);
        uri = [[NSString alloc] initWithFormat:@"rtp://%s:%d", addr, (int) ntohs(local->sin_port)];
    }
    return self;
    
}
- (void)close {
    CFSocketInvalidate(sock);
}



@end
