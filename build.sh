#!/bin/bash
set -xe

APP=androtest
SDK=~/android_sdk
PLATFORM=$SDK/platforms/android-29
BUILD_TOOLS=$SDK/build-tools/29.0.3
KOTLIN_STDLIB=/usr/share/kotlin/lib/kotlin-stdlib.jar


mkdir -p classes
kotlinc src/*.kt -d classes/classes.jar \
	-cp $PLATFORM/android.jar \


mkdir -p compiled_res
for f in res/mipmap-*/*.png; do
    $BUILD_TOOLS/aapt2 compile "$f" -o compiled_res/
done
mkdir -p dex
$BUILD_TOOLS/d8 \
	--lib $PLATFORM/android.jar \
	classes/classes.jar\
	$KOTLIN_STDLIB \
	--output dex/

# Package
$BUILD_TOOLS/aapt2 link  -o base.apk \
	--manifest AndroidManifest.xml \
	-I $PLATFORM/android.jar \
	--min-sdk-version 29\
	-R compiled_res/*.flat

cp base.apk unaligned-app.apk
zip -uj unaligned-app.apk dex/classes.dex

$BUILD_TOOLS/zipalign -f 4 unaligned-app.apk app.apk
$BUILD_TOOLS/apksigner sign --ks debug.keystore --ks-pass pass:android app.apk

