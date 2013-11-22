LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := ../../src

LOCAL_MODULE := rsp-hle
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)

LOCAL_SRC_FILES :=          \
    $(SRCDIR)/alist.c       \
    $(SRCDIR)/cicx105.c     \
    $(SRCDIR)/jpeg.c        \
    $(SRCDIR)/main.c        \
    $(SRCDIR)/ucode1.cpp    \
    $(SRCDIR)/ucode2.cpp    \
    $(SRCDIR)/ucode3.cpp    \
    $(SRCDIR)/ucode3mp3.cpp \

LOCAL_CFLAGS := $(COMMON_CFLAGS)

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver

include $(BUILD_SHARED_LIBRARY)
