JNI_LOCAL_PATH := $(call my-dir)

BUILD_VARIANT := debug

ifeq ($(NDK_DEBUG), 1)
    BUILD_VARIANT := release
endif

M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/../mupen64plus-core/upstream/src/api/

COMMON_CFLAGS :=                    \
    -O3                             \
    -ffast-math                     \
    -fno-strict-aliasing            \
    -fomit-frame-pointer            \
    -fvisibility=hidden

COMMON_CPPFLAGS :=                  \
    -fvisibility-inlines-hidden     \
    -O3                             \
    -ffast-math                     \

COMMON_LDFLAGS :=

ifneq ($(HOST_OS),windows)
    COMMON_CFLAGS += -flto
    COMMON_CPPFLAGS += -flto
    COMMON_LDFLAGS +=                   \
        $(COMMON_CFLAGS)                \
        $(COMMON_CPPFLAGS)
endif

include $(JNI_LOCAL_PATH)/src/Android.mk

