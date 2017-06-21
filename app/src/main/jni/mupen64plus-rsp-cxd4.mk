######################
# mupen64plus-rsp-hle
######################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := mupen64plus-rsp-cxd4

MY_LOCAL_CFLAGS := $(COMMON_CFLAGS) -DM64P_PLUGIN_API

LOCAL_MODULE := mupen64plus-rsp-cxd4
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    MY_LOCAL_CFLAGS += -DUSE_SSE2NEON -D__ARM_NEON__ -mfpu=neon
else ifeq ($(TARGET_ARCH_ABI), x86)
    MY_LOCAL_CFLAGS += -ARCH_MIN_SSE2
endif

LOCAL_SRC_FILES := \
    $(SRCDIR)/module.c \
    $(SRCDIR)/su.c \
    $(SRCDIR)/osal_dynamiclib_unix.c \
    $(SRCDIR)/vu/add.c \
    $(SRCDIR)/vu/divide.c \
    $(SRCDIR)/vu/logical.c \
    $(SRCDIR)/vu/multiply.c \
    $(SRCDIR)/vu/select.c \
    $(SRCDIR)/vu/vu.c

LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS)
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
LOCAL_LDFLAGS := $(COMMON_LDFLAGS) -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
