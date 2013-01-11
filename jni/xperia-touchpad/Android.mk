
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := xperia-touchpad
LOCAL_SRC_FILES := main.c
LOCAL_LDLIBS    := -llog -landroid -lEGL -lGLESv1_CM

LOCAL_CFLAGS += -O3 -ffast-math -fno-strict-aliasing

include $(BUILD_SHARED_LIBRARY)
