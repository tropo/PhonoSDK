#!/bin/sh

export DEVROOT="`xcode-select -print-path`/Platforms/iPhoneOS.platform/Developer"
export SDKVER=`xcodebuild -showsdks | grep iphoneos | sort | tail -n 1 | awk '{ print $2}' `
export SDKROOT="$DEVROOT/SDKs/iPhoneOS$SDKVER.sdk"

export PREFIX="/opt/ios-$SDKVER"
export ARCH="armv6"

export CC="$DEVROOT/usr/bin/gcc"
export CFLAGS="-arch $ARCH -isysroot $SDKROOT"
./configure \
    --prefix="$PREFIX" \
    --host="arm-apple-darwin" \
    --enable-static \
    --disable-shared $@
