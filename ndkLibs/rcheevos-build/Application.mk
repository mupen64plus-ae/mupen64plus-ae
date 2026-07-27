# Standalone ndk-build settings for regenerating librcheevos.a.
# Only used by build.sh -- the app build links the checked-in prebuilt instead.

APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
APP_PLATFORM := android-28
APP_STL := c++_shared
