//
//  VisIVRViewController.h
//  VisIVR
//
//  Created by Tim Panton on 15/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import <UIKit/UIKit.h>
@class PhonoNative;
@class PhonoCall;

@interface VisIVRViewController : UIViewController
{
    UITextField *appNum;
    UILabel *tjid;
    UILabel *status;
    UIWebView *prompt;
    PhonoNative *phono;
    PhonoCall *call;
}

@property (nonatomic,retain) IBOutlet UITextField *appNum;
@property (nonatomic,retain) IBOutlet UILabel *tjid;
@property (nonatomic,retain) IBOutlet UILabel *status;
@property (nonatomic,retain) IBOutlet UIWebView *prompt;


- (IBAction)connect;
- (IBAction)disconnect;
- (IBAction)call;
- (IBAction)hangup;
- (IBAction)digit;
@end
