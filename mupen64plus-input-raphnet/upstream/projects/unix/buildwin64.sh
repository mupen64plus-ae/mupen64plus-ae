#!/bin/bash

HIDHOME=../../../hidapi64
UNAME=MINGW64
DESTDIR=./dist_win64

HID_CFLAGS="-I $HIDHOME/hidapi"
HID_LDLIBS="-L $HIDHOME/windows/.libs/ -lhidapi sdl.lib"
TARGETS=all
MAKEFILE=Makefile

make -f $MAKEFILE clean
if [ $? -ne 0 ]; then
	exit 1
fi

# Hack to let this project link without requiring libsdl. Without
# this linking will fail due to a missing symbol (SDL_PumpEvents)
x86_64-w64-mingw32-dlltool -d sdl.def -l sdl.lib

make -f $MAKEFILE CROSS_COMPILE=x86_64-w64-mingw32- UNAME=$UNAME HID_CFLAGS="$HID_CFLAGS" HID_LDLIBS="$HID_LDLIBS" $TARGETS V=1
if [ $? -ne 0 ]; then
	exit 1
fi

mkdir -p $DESTDIR
cp mupen64plus-input-raphnetraw.dll $DESTDIR -v
cp $HIDHOME/windows/.libs/*.dll $DESTDIR -v
