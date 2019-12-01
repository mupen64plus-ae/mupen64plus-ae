LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)
LOCAL_SHARED_LIBRARIES := libusb1.0

LOCAL_SRC_FILES := plugin.c \
	plugin_front.c \
	plugin_back.c \
	gcn64lib.c \
	gcn64_android.c \
	hexdump.c \
	osal_dynamiclib_unix.c

LOCAL_MODULE := mupen64plus-input-raphnet
LOCAL_CFLAGS := $(COMMON_CFLAGS)
LOCAL_LDFLAGS := $(COMMON_LDFLAGS)
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
