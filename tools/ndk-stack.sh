#! /bin/sh

adb logcat | ndk-stack -sym ../obj/local/armeabi-v7a > crashdump.log
