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

ifeq ($(TARGET_ARCH_ABI), x86)
    MY_LOCAL_CFLAGS += -DARCH_MIN_SSE3 -DUSE_SSE_SUPPORT
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
LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
