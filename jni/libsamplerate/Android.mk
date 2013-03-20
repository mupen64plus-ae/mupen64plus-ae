LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := samplerate

LOCAL_SRC_FILES :=  \
    samplerate.c    \
    src_linear.c    \
    src_sinc.c      \
    src_zoh.c       \

LOCAL_CFLAGS := $(COMMON_CFLAGS)

include $(BUILD_STATIC_LIBRARY)
