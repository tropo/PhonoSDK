//
//  VisIVRAppDelegate.h
//  VisIVR
//
//  Created by Tim Panton on 15/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import <UIKit/UIKit.h>


@class VisIVRViewController;

@interface VisIVRAppDelegate : UIResponder <UIApplicationDelegate>
{
	UIWindow *window;
}



@property (strong, nonatomic) UIWindow *window;

@property (strong, nonatomic) VisIVRViewController *viewController;




@end
