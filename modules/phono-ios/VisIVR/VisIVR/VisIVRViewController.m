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

@synthesize  appNum , tjid, status, prompt;

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
    phono.onReady = ^{ [self gotjId];};
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
- (IBAction)disconnect{}
- (IBAction)call{
    call = [[PhonoCall alloc] initOutbound:@"9996160714" domain:@"app"]; 
    //call = [[PhonoCall alloc] initOutbound:@"timpanton@sip2sip.info" domain:@"sip"];
    [self update:@"dialing"];
    call.onAnswer = ^{ [self update:@"answered"];};
    call.onError = ^{ [self update:@"error"];};
    call.onHangup = ^{ [self update:@"hangup"];};
    call.onRing = ^{ [self update:@"ring"];};
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
