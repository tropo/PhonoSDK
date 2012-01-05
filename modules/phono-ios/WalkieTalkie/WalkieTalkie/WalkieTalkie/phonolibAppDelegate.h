//
//  phonolibAppDelegate.h
//  WalkieTalkie
//
//  Created by Tim Panton on 15/12/2011.
//  Copyright (c) 2011 Westhhawk Ltd. All rights reserved.
//

#import <UIKit/UIKit.h>

@class phonolibViewController;

@interface phonolibAppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;

@property (strong, nonatomic) phonolibViewController *viewController;

@end
