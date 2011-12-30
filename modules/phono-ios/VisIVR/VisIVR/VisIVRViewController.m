//
//  VisIVRViewController.m
//  VisIVR
//
//  Created by Tim Panton on 15/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import "VisIVRViewController.h"
#import "PhonoNative.h"
#import "PhonoPhone.h"

@implementation VisIVRViewController

@synthesize  appNum , tjid, status, prompt, domain, outMess;

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Release any cached data, images, etc that aren't in use.
}

#pragma mark - View lifecycle

- (void) gotjId{
    [tjid setText:[phono sessionID]];
}

- (void) update:(NSString *) state {
    [status setText:state];
}

-(void) popIncommingCallAlert:(PhonoCall *) incall{
    call = incall;
    NSString *erom = [PhonoNative unescapeString:[incall from]];
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Incomming Call"
                          
                                                    message:erom
                          
                                                   delegate:self
                          
                                          cancelButtonTitle:@"Ignore"
                          
                                          otherButtonTitles:nil];
    [alert addButtonWithTitle:@"Accept"];

    [alert show];
    [alert release];
}

-(void) gotMessage:(PhonoMessage *)mess{
    // strictly test to and from here but for now.....
    // stringByEvaluatingJavaScriptFromString in future $("#"+newCallID);
    //NSString *html =[NSString stringWithFormat:@"<html><body>%@</body></html>",[mess body]];
    //[prompt loadHTMLString:html baseURL:nil];
    
    NSString *bo = [mess body];
    if ([bo characterAtIndex:0] == '$'){
        NSString *res = [prompt stringByEvaluatingJavaScriptFromString:bo];
        NSLog(@" js result %@",res);
    } else {
        NSString *html =[NSString stringWithFormat:@"<html><body>%@</body></html>",[mess body]];
        [prompt loadHTMLString:html baseURL:nil];
    }
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    phone = [[PhonoPhone alloc] init];
    phone.onIncommingCall = ^(PhonoCall * incall){
        [self popIncommingCallAlert:incall];
    };
    phone.ringTone = [[NSBundle mainBundle] pathForResource:@"Diggztone_Marimba" ofType:@"mp3"] ;
    phone.ringbackTone= [[NSBundle mainBundle] pathForResource:@"ringback-uk" ofType:@"mp3"];

    phono = [[PhonoNative alloc] initWithPhone:phone ];
    phono.messaging.onMessage = ^(PhonoMessage *message){
        [self gotMessage:message];
    };
    phono.onReady = ^{ [self gotjId];};
    NSURL *base = [NSURL URLWithString:@"http://s.phono.com/"];
    NSString *empty = @"<html>\
    <head>\
    <script src='http://code.jquery.com/jquery-1.4.2.min.js'></script>\
    </head>\
    <body id='body'>Empty\
    </body>\
    </html>";
    
    [prompt loadHTMLString:empty baseURL:base];
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
    // e.g. self.myOutlet = nil;
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
	[super viewWillDisappear:animated];
}

- (void)viewDidDisappear:(BOOL)animated
{
	[super viewDidDisappear:animated];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    // Return YES for supported orientations
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        return (interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown);
    } else {
        return YES;
    }
}

- (IBAction)connect{
    [phono setApiKey:@"C17D167F-09C6-4E4C-A3DD-2025D48BA243"];
    [phono connect];
}
- (IBAction)disconnect{
    [phono disconnect];
}
- (IBAction)call{
    NSString *user = [appNum text];
    NSInteger m = [domain selectedSegmentIndex];
    NSString *dom =  (m == 1)? @"sip":@"app";

    call = [[[PhonoCall alloc] initOutbound:user domain:dom] retain]; 
    [self update:@"dialing"];
    [call.headers setObject:[phono sessionID] forKey:@"x-jid"];
    call.onAnswer = ^{ [self update:@"answered"];};
    call.onError = ^{ [self update:@"error"];};
    call.onHangup = ^{ [self update:@"hangup"];};
    call.onRing = ^{ [self update:@"ring"];};
    call.from = [NSString stringWithFormat:@"%@@gw114.phono.com",[phono sessionID]];
    [phono.phone dial:call];

}
- (IBAction)hangup{
    [call hangup];
}
- (IBAction)digit:(id) sender {
    UIButton *b = (UIButton *) sender;
    NSString *d = [[b titleLabel] text];
    NSLog(@"Digit %@",d);
    [call digit:d];
}

- (IBAction)sendMess{
    if (call == nil){
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Can't send message"
                                                        message:@"You can only send messages during a call, sorry."
                                                       delegate:nil
                                              cancelButtonTitle:@"Cancel"
                                              otherButtonTitles:nil];
        
        [alert show];
        [alert release];
    } else {
        NSString *b = [outMess text];
        PhonoMessage *m = [[PhonoMessage alloc] init];
        [m setPhono:phono];
        [m setBody:b];
        [m setFrom:[call from]];
        NSString *t = [NSString stringWithFormat:@"%@@tropo.im",[appNum text]];
        [m setTo:t];
        [m sendMe];
    }
}

// delegate actions
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    // don't actually care.
    
}
- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex{
    if (buttonIndex == 0){
        // ignore
        [call hangup];
    } else if (buttonIndex == 1){
        // accept incomming call
        [call answer];
    }
}


@end
