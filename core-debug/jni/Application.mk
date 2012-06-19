APP_STL := gnustl_static
APP_ABI := armeabi armeabi-v7a
#APP_OPTIM := debug
# Fix for the "RAM full of zeros" bug:
APP_CFLAGS := -UNDEBUG

