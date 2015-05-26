JNI_LOCAL_PATH := $(call my-dir)

AE_BRIDGE_INCLUDES := $(JNI_LOCAL_PATH)/ae-bridge/
M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/mupen64plus-core/src/api/
SDL_INCLUDES := $(JNI_LOCAL_PATH)/SDL2/include/
PNG_INCLUDES := $(JNI_LOCAL_PATH)/png/include/
SAMPLERATE_INCLUDES := $(JNI_LOCAL_PATH)/libsamplerate/

COMMON_CFLAGS :=                    \
    -O3                             \
    -ffast-math                     \
    -fno-strict-aliasing            \
    -fomit-frame-pointer            \
    -frename-registers              \
    -fsingle-precision-constant     \
    -fvisibility=hidden             \

COMMON_CPPFLAGS :=                  \
    -fvisibility-inlines-hidden     \

include $(JNI_LOCAL_PATH)/SDL2/android.mk
include $(JNI_LOCAL_PATH)/png/android.mk
include $(JNI_LOCAL_PATH)/libsamplerate/android.mk
include $(JNI_LOCAL_PATH)/ae-bridge/android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-audio-sdl.mk
include $(JNI_LOCAL_PATH)/mupen64plus-audio-sles/android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-core.mk
include $(JNI_LOCAL_PATH)/mupen64plus-input-android/android.mk
include $(JNI_LOCAL_PATH)/xperia-touchpad/android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-rsp-hle.mk
include $(JNI_LOCAL_PATH)/mupen64plus-ui-console.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-glide64mk2.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-gln64/android.mk
include $(JNI_LOCAL_PATH)/mupen64plus-video-rice.mk
