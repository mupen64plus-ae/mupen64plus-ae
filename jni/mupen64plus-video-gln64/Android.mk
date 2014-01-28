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
SRCDIR := src

LOCAL_MODULE := mupen64plus-video-gln64
LOCAL_SHARED_LIBRARIES := ae-imports SDL2
LOCAL_STATIC_LIBRARIES := cpufeatures
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(M64P_API_INCLUDES)    \
    $(SDL_INCLUDES)         \
    $(AE_BRIDGE_INCLUDES)   \

LOCAL_SRC_FILES :=                  \
    $(SRCDIR)/2xSAI.cpp             \
    $(SRCDIR)/3DMath.cpp            \
    $(SRCDIR)/Config.cpp            \
    $(SRCDIR)/CRC.cpp               \
    $(SRCDIR)/DepthBuffer.cpp       \
    $(SRCDIR)/F3D.cpp               \
    $(SRCDIR)/F3DCBFD.cpp           \
    $(SRCDIR)/F3DDKR.cpp            \
    $(SRCDIR)/F3DEX.cpp             \
    $(SRCDIR)/F3DEX2.cpp            \
    $(SRCDIR)/F3DPD.cpp             \
    $(SRCDIR)/F3DWRUS.cpp           \
    $(SRCDIR)/FrameSkipper.cpp      \
    $(SRCDIR)/GBI.cpp               \
    $(SRCDIR)/gDP.cpp               \
    $(SRCDIR)/gles2N64.cpp          \
    $(SRCDIR)/gSP.cpp               \
    $(SRCDIR)/L3D.cpp               \
    $(SRCDIR)/L3DEX.cpp             \
    $(SRCDIR)/L3DEX2.cpp            \
    $(SRCDIR)/N64.cpp               \
    $(SRCDIR)/OpenGL.cpp            \
    $(SRCDIR)/RDP.cpp               \
    $(SRCDIR)/RSP.cpp               \
    $(SRCDIR)/S2DEX.cpp             \
    $(SRCDIR)/S2DEX2.cpp            \
    $(SRCDIR)/ShaderCombiner.cpp    \
    $(SRCDIR)/Textures.cpp          \
    $(SRCDIR)/ticks.c               \
    $(SRCDIR)/VI.cpp                \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -D__CRC_OPT         \
    -D__HASHMAP_OPT     \
    -D__TRIBUFFER_OPT   \
    -D__VEC4_OPT        \
    -DANDROID           \
    -DUSE_SDL           \
    -fsigned-char       \
    #-DSDL_NO_COMPAT     \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
    
LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -lGLESv2            \
    -llog               \

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/gSPNeon.cpp.neon
    LOCAL_SRC_FILES += $(SRCDIR)/3DMathNeon.cpp.neon 
    LOCAL_CFLAGS += -DARM_ASM
    LOCAL_CFLAGS += -D__NEON_OPT

else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:

else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here

else
    # Any other architectures that Android could be running on?

endif

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)
