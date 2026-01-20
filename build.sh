#!/bin/bash

# This script builds the Wild KSU manager APK.

# Ensure you have the setup Android SDK & NDK installed and necessary environment variables set and sourced.

# For LKM make sure you have imported the androidX-X.X_kernelsu.ko drivers to userspace/ksud/bin/aarch64 directory.

cross build --target aarch64-linux-android --release --manifest-path ./userspace/ksud/Cargo.toml

cp userspace/ksud/target/aarch64-linux-android/release/ksud manager/app/src/main/jniLibs/arm64-v8a/libksud.so

cd manager

./setup.sh

cd ..

adb install manager/app/build/outputs/apk/release/Wild_KSU_v*.apk
