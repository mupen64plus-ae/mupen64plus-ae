LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := samplerate

LOCAL_SRC_FILES := \
    samplerate.c \
    src_linear.c \
    src_sinc.c \
    src_zoh.c

LOCAL_CFLAGS += -O3 -ffast-math -frename-registers -fomit-frame-pointer -fsingle-precision-constant -fpredictive-commoning -fno-strict-aliasing -fvisibility=hidden

include $(BUILD_STATIC_LIBRARY)
