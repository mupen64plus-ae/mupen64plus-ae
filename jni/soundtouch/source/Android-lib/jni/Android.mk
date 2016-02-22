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

include $(CLEAR_VARS)

# *** Remember: Change -O0 into -O2 in add-applications.mk ***

LOCAL_MODULE    := soundtouch
LOCAL_SRC_FILES := ../../SoundTouch/AAFilter.cpp ../../SoundTouch/FIFOSampleBuffer.cpp \
                ../../SoundTouch/FIRFilter.cpp ../../SoundTouch/cpu_detect_x86.cpp \
                ../../SoundTouch/sse_optimized.cpp ../../SoundStretch/WavFile.cpp \
                ../../SoundTouch/RateTransposer.cpp ../../SoundTouch/SoundTouch.cpp \
                ../../SoundTouch/InterpolateCubic.cpp ../../SoundTouch/InterpolateLinear.cpp \
                ../../SoundTouch/InterpolateShannon.cpp ../../SoundTouch/TDStretch.cpp \
                ../../SoundTouch/BPMDetect.cpp ../../SoundTouch/PeakFinder.cpp \

ifeq ($(TARGET_ARCH_ABI), x86)
LOCAL_SRC_FILES += ../../SoundTouch/mmx_optimized.cpp \
                   ../../SoundTouch/sse_optimized.cpp \

endif

# for native audio
LOCAL_SHARED_LIBRARIES += -lgcc 
# --whole-archive -lgcc 
# for logging
LOCAL_LDLIBS    += -llog
# for native asset manager
#LOCAL_LDLIBS    += -landroid

# Custom Flags: 
# -fvisibility=hidden : don't export all symbols
LOCAL_CFLAGS += -I $(LOCAL_PATH)/../../../include -fdata-sections -ffunction-sections -fexceptions -D__SOFTFP__ -DANDROID

# OpenMP mode : enable these flags to enable using OpenMP for parallel computation 
#LOCAL_CFLAGS += -fopenmp
#LOCAL_LDFLAGS += -fopenmp


# Use ARM instruction set instead of Thumb for improved calculation performance in ARM CPUs	
LOCAL_ARM_MODE := arm

include $(BUILD_SHARED_LIBRARY)
