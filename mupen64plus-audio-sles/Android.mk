JNI_LOCAL_PATH := $(call my-dir)

BUILD_VARIANT := debug

ifeq ($(NDK_DEBUG), 1)
    BUILD_VARIANT := release
endif

#Soundtouch
include $(CLEAR_VARS)
LOCAL_MODULE := soundtouch
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libsoundtouch.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/soundtouch/include/
include $(PREBUILT_SHARED_LIBRARY)

#Soundtouch floating point
include $(CLEAR_VARS)
LOCAL_MODULE := soundtouch_fp
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libsoundtouch_fp.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/soundtouch/include/
include $(PREBUILT_SHARED_LIBRARY)

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
