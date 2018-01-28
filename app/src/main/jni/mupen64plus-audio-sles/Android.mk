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

MY_LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)

MY_LOCAL_SRC_FILES :=            \
    main.cpp                    \
    osal_dynamiclib_unix.cpp    \
    threadqueue.cpp             \

MY_LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -fpermissive        \

include $(CLEAR_VARS)

LOCAL_MODULE := mupen64plus-audio-sles
LOCAL_SHARED_LIBRARIES := soundtouch
LOCAL_C_INCLUDES := $(MY_LOCAL_C_INCLUDES)
LOCAL_SRC_FILES := $(MY_LOCAL_SRC_FILES)
LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS) -D__SOFTFP__ -DANDROID
LOCAL_LDFLAGS := $(COMMON_LDFLAGS)
LOCAL_LDLIBS := -lOpenSLES -llog

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := mupen64plus-audio-sles-fp
LOCAL_SHARED_LIBRARIES := soundtouch_fp
LOCAL_C_INCLUDES := $(MY_LOCAL_C_INCLUDES) $(LOCAL_PATH)/../SLES/include/
LOCAL_SRC_FILES := $(MY_LOCAL_SRC_FILES)
LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS) -DFP_ENABLED
LOCAL_LDFLAGS := $(COMMON_LDFLAGS)

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    LOCAL_LDLIBS        += -lOpenSLES -llog
else ifeq ($(TARGET_ARCH_ABI), x86)
    LOCAL_LDLIBS        += -lOpenSLES -llog
endif


include $(BUILD_SHARED_LIBRARY)
