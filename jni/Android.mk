JNI_LOCAL_PATH := $(call my-dir)

M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/core/src/api/
SDL_INCLUDES := $(JNI_LOCAL_PATH)/SDL/include/
PNG_INCLUDES := $(JNI_LOCAL_PATH)/png/include/
SAMPLERATE_INCLUDES := $(JNI_LOCAL_PATH)/libsamplerate/

SDL_MAIN_ENTRY := ../../../SDL/src/main/android/SDL_android_main.cpp

include $(call all-subdir-makefiles)
include $(wildcard $(JNI_LOCAL_PATH)/*/projects/android/Android.mk)