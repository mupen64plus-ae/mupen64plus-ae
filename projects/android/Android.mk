# Mupen64PlusAE, an N64 emulator for the Android platform
#
# Copyright (C) 2013 Paul Lamb
#
# This file is part of Mupen64PlusAE.
#
# Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
# GNU General Public License as published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
# not, see <http://www.gnu.org/licenses/>.

# See mupen64plus-ae/jni/Android.mk for build variable definitions
# https://github.com/mupen64plus-ae/mupen64plus-ae/blob/master/jni/Android.mk

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := ../../src

LOCAL_MODULE := mupen64plus-audio-sdl
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := samplerate
#LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(M64P_API_INCLUDES)    \
    $(SDL_INCLUDES)         \
    $(SAMPLERATE_INCLUDES)  \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/main.c                    \
    $(SRCDIR)/volume.c                  \
    $(SRCDIR)/osal_dynamiclib_unix.c    \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID_EDITION   \
    -DUSE_SRC           \

include $(BUILD_SHARED_LIBRARY)
