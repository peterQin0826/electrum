//
//  OKSelectAssetTypeModel.h
//  OneKey
//
//  Created by xiaoliang on 2020/11/16.
//  Copyright © 2020 OneKey. All rights reserved..
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface OKSelectAssetTypeModel : NSObject
@property (nonatomic,copy) NSString* coin;
@property (nonatomic,assign)BOOL isSelected;
@end

NS_ASSUME_NONNULL_END
