JNI_LOCAL_PATH := $(call my-dir)

BUILD_VARIANT := debug

ifeq ($(NDK_DEBUG), 1)
    BUILD_VARIANT := release
endif

#SDL2
include $(CLEAR_VARS)
LOCAL_MODULE := SDL2
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../../../libs/ndkLibs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libSDL2.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/SDL2/include/
include $(PREBUILT_SHARED_LIBRARY)

#PNG
include $(CLEAR_VARS)
LOCAL_MODULE := png
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../../../libs/ndkLibs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libpng.a
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/png/
include $(PREBUILT_STATIC_LIBRARY)

#Freetype
include $(CLEAR_VARS)
LOCAL_MODULE := freetype
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../../../libs/ndkLibs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libfreetype.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/freetype/include/
include $(PREBUILT_SHARED_LIBRARY)

#Soundtouch
include $(CLEAR_VARS)
LOCAL_MODULE := soundtouch
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../../../libs/ndkLibs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libsoundtouch.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/soundtouch/include/
include $(PREBUILT_SHARED_LIBRARY)

#Soundtouch floating point
include $(CLEAR_VARS)
LOCAL_MODULE := soundtouch_fp
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../../../libs/ndkLibs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libsoundtouch_fp.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/soundtouch/include/
include $(PREBUILT_SHARED_LIBRARY)

AE_BRIDGE_INCLUDES := $(JNI_LOCAL_PATH)/ae-bridge/
M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/mupen64plus-core/src/api/
LIBRETRO_INCLUDES := $(JNI_LOCAL_PATH)/libretro/
GL_INCLUDES := $(JNI_LOCAL_PATH)/GL/
ANDROID_FRAMEWORK_INCLUDES := $(JNI_LOCAL_PATH)/android_framework/include/

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

#ifneq ($(HOST_OS),windows)
#    COMMON_CFLAGS += -flto
#    COMMON_LDFLAGS +=                   \
#        $(COMMON_CFLAGS)                \
#        $(COMMON_CPPFLAGS)
#endif

include $(JNI_LOCAL_PATH)/GL/GL/Android.mk
include $(JNI_LOCAL_PATH)/ae-bridge/Android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-audio-sles/Android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-core.mk
include $(JNI_LOCAL_PATH)/mupen64plus-input-android/Android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-rsp-hle.mk
include $(JNI_LOCAL_PATH)/mupen64plus-rsp-cxd4.mk
include $(JNI_LOCAL_PATH)/mupen64plus-ui-console.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-gliden64/src/osal/mupen64plus-video-osal.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-gliden64/src/GLideNHQ/mupen64plus-video-glidenhq.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-gliden64/src/mupen64plus-video-gliden64.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-glide64mk2.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-gln64/Android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-rice.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-angrylion.mk

