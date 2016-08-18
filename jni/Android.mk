JNI_LOCAL_PATH := $(call my-dir)

AE_BRIDGE_INCLUDES := $(JNI_LOCAL_PATH)/ae-bridge/
M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/mupen64plus-core/src/api/
LIBRETRO_INCLUDES := $(JNI_LOCAL_PATH)/libretro/
SDL_INCLUDES := $(JNI_LOCAL_PATH)/SDL2/include/
PNG_INCLUDES := $(JNI_LOCAL_PATH)/png/include/
SAMPLERATE_INCLUDES := $(JNI_LOCAL_PATH)/libsamplerate/
FREETYPE_INCLUDES := $(JNI_LOCAL_PATH)/freetype/include/
SOUNDTOUCH_INCLUDES := $(JNI_LOCAL_PATH)/soundtouch/include/
ANDROID_FRAMEWORK_INCLUDES := $(JNI_LOCAL_PATH)/android_framework/include/

COMMON_CFLAGS :=                    \
    -O3                             \
    -ffast-math                     \
    -fno-strict-aliasing            \
    -fomit-frame-pointer            \
    -frename-registers              \
    -fvisibility=hidden             \

COMMON_CPPFLAGS :=                  \
    -fvisibility-inlines-hidden     \

include $(JNI_LOCAL_PATH)/SDL2/Android.mk
include $(JNI_LOCAL_PATH)/soundtouch/source/Android-lib/jni/Android.mk
include $(JNI_LOCAL_PATH)/png/Android.mk
include $(JNI_LOCAL_PATH)/freetype.mk
include $(JNI_LOCAL_PATH)/libsamplerate/Android.mk
include $(JNI_LOCAL_PATH)/ae-bridge/Android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-audio-sdl.mk
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

