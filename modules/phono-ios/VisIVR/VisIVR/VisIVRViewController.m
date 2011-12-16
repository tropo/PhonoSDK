//
//  VisIVRViewController.m
//  VisIVR
//
//  Created by Tim Panton on 15/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import "VisIVRViewController.h"
#import "PhonoNative.h"

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


- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    phono = [[PhonoNative alloc] init ];
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
    call = [[PhonoCall alloc] initOutbound:@"app:9996160714"];
    [self update:@"dialing"];
    call.onAnswer = ^{ [self update:@"answered"];};
    call.onError = ^{ [self update:@"error"];};
    call.onHangup = ^{ [self update:@"hangup"];};
    call.onRing = ^{ [self update:@"ring"];};
    [phono.phone dial:call];

}
- (IBAction)hangup{}
- (IBAction)digit{}

@end
