#!/bin/bash

set -euo pipefail

# This script builds the Wild KSU manager APK.

# Ensure you have the setup Android SDK & NDK installed and necessary environment variables set and sourced.

# For LKM make sure you have imported the androidX-X.X_kernelsu.ko drivers to userspace/ksud/bin/aarch64 directory.

cross build --target aarch64-linux-android --release --manifest-path ./userspace/ksud/Cargo.toml

cp userspace/ksud/target/aarch64-linux-android/release/ksud manager/app/src/main/jniLibs/arm64-v8a/libksud.so

cd userspace/susfsd/jni

ndk-build

cp ../libs/arm64-v8a/susfsd ../../../manager/app/src/main/jniLibs/arm64-v8a/libsusfsd.so

cd ../../..

cd manager

./setup.sh

cd ..

# get exact APK filename (the one just built)
APK=$(ls -t manager/app/build/outputs/apk/release/Wild_KSU_*.apk | head -n 1)
APK_NAME=$(basename "$APK")

# Copy to Windows desktop
cp "$APK" /mnt/c/Users/james/Desktop/apk/

# Better way: use full path and proper quoting for PowerShell
WIN_APK_PATH="C:\\Users\\james\\Desktop\\apk\\$APK_NAME"

powershell.exe -Command "& 'C:\Users\james\Desktop\platform-tools\adb.exe' install \"$WIN_APK_PATH\""
