LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := xperia-touchpad

LOCAL_SRC_FILES := main.c

LOCAL_CFLAGS := $(COMMON_CFLAGS)

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDLIBS := \
    -landroid   \
    -llog       \

LOCAL_STATIC_LIBRARIES := android_native_app_glue

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)
