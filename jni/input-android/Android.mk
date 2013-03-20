LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := input-android
LOCAL_SHARED_LIBRARIES := core
LOCAL_STATIC_LIBRARIES := cpufeatures

LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)

LOCAL_SRC_FILES := plugin.c

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DNO_ASM            \

LOCAL_LDLIBS := $(COMMON_LDLIBS)

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)
