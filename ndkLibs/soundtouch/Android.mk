# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# $Id: Android.mk 216 2015-05-18 15:28:41Z oparviai $

LOCAL_PATH := $(call my-dir)

MY_LOCAL_SRC_FILES := ../../SoundTouch/AAFilter.cpp  ../../SoundTouch/FIFOSampleBuffer.cpp \
                                      ../../SoundTouch/FIRFilter.cpp ../../SoundTouch/cpu_detect_x86.cpp \
                                      ../../SoundTouch/sse_optimized.cpp ../../SoundStretch/WavFile.cpp \
                                      ../../SoundTouch/RateTransposer.cpp ../../SoundTouch/SoundTouch.cpp \
                                      ../../SoundTouch/InterpolateCubic.cpp ../../SoundTouch/InterpolateLinear.cpp \
                                      ../../SoundTouch/InterpolateShannon.cpp ../../SoundTouch/TDStretch.cpp \
                                      ../../SoundTouch/BPMDetect.cpp ../../SoundTouch/PeakFinder.cpp

MY_LOCAL_LDLIBS    := -llog
MY_LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../../include
MY_LOCAL_CFLAGS := -fdata-sections -ffunction-sections -fexceptions
MY_LOCAL_ARM_MODE := arm

ifeq ($(TARGET_ARCH_ABI), x86)
MY_LOCAL_SRC_FILES += ../../SoundTouch/mmx_optimized.cpp
endif

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
MY_LOCAL_CFLAGS += -mfpu=neon -DSOUNDTOUCH_USE_NEON
endif

ifeq ($(TARGET_ARCH_ABI), arm64-v8a)
MY_LOCAL_CFLAGS += -mfpu=neon -DSOUNDTOUCH_USE_NEON
endif


include $(CLEAR_VARS)
LOCAL_MODULE    := soundtouch
LOCAL_SRC_FILES := $(MY_LOCAL_SRC_FILES)
LOCAL_LDLIBS    := $(MY_LOCAL_LDLIBS)
LOCAL_C_INCLUDES := $(MY_LOCAL_C_INCLUDES)
LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS) -D__SOFTFP__ -DANDROID
LOCAL_ARM_MODE := $(MY_LOCAL_ARM_MODE)
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := soundtouch_fp
LOCAL_SRC_FILES := $(MY_LOCAL_SRC_FILES)
LOCAL_LDLIBS    := $(MY_LOCAL_LDLIBS)
LOCAL_C_INCLUDES := $(MY_LOCAL_C_INCLUDES)
LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS)
LOCAL_ARM_MODE := $(MY_LOCAL_ARM_MODE)
include $(BUILD_SHARED_LIBRARY)


