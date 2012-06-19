APP_STL := gnustl_static
APP_ABI := armeabi armeabi-v7a
APP_PLATFORM := android-9
#APP_OPTIM := release
# Fix for the "RAM full of zeros" bug:
APP_CFLAGS := -UNDEBUG
