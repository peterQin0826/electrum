//
//  NSString+BXAdd.h
//  Electron-Cash
//
//  Created by xiaoliang on 2020/9/28.
//  Copyright © 2020 OneKey. All rights reserved..
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface NSString (OKAdd)
+(const char *)ok_stringToChar:(NSString *)string;
+(NSString *)ok_charToString:(const char *)cString;
+ (NSMutableAttributedString *)lineSpacing:(CGFloat)lineSpacing content:(NSString *)content;
- (NSString *)SHA256;
//根据宽度求高度  content 计算的内容  width 计算的宽度 font字体大小
- (CGFloat)getLabelHeightWithWidth:(CGFloat)width font: (CGFloat)font;
//根据高度度求宽度  text 计算的内容  Height 计算的高度 font字体大小
- (CGFloat)getWidthWithHeight:(CGFloat)height font:(CGFloat)font;
- (BOOL)isValid;
- (BOOL)isNumbersOrLetters;
- (BOOL)isNumbers;
- (BOOL)isEnglish;
- (BOOL)isChinese;
- (BOOL)containsChinese;
+ (BOOL)isHexString:(NSString *)str;
- (NSURL * _Nullable)toURL;
- (NSDictionary * _Nullable)toDict; // json string to dict
- (NSArray<NSString *>*)split:(NSString *)sparator;
+ (NSString *)stringFromHexString:(NSString *)hexString;
- (NSString *)localized;
@end

NS_ASSUME_NONNULL_END
