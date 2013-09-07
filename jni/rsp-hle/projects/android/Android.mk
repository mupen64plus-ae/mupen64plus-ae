LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := ../../src

MY_LOCAL_MODULE := rsp-hle
MY_LOCAL_SHARED_LIBRARIES := core
MY_LOCAL_ARM_MODE := arm

MY_LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)

MY_LOCAL_SRC_FILES :=       \
    $(SRCDIR)/alist.c       \
    $(SRCDIR)/cicx105.c     \
    $(SRCDIR)/jpeg.c        \
    $(SRCDIR)/main.c        \
    $(SRCDIR)/ucode1.cpp    \
    $(SRCDIR)/ucode2.cpp    \
    $(SRCDIR)/ucode3.cpp    \
    $(SRCDIR)/ucode3mp3.cpp \

MY_LOCAL_CFLAGS := $(COMMON_CFLAGS) -DPAULSCODE

MY_LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

MY_LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver

######### Standard RSP Module #################################################

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES  := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_ARM_MODE          := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES        := $(MY_LOCAL_C_INCLUDES)
LOCAL_SRC_FILES         := $(MY_LOCAL_SRC_FILES)
LOCAL_CFLAGS            := $(MY_LOCAL_CFLAGS)
LOCAL_CPPFLAGS          := $(MY_LOCAL_CPPFLAGS)
LOCAL_LDFLAGS           := $(MY_LOCAL_LDFLAGS)

LOCAL_MODULE := $(MY_LOCAL_MODULE)

include $(BUILD_SHARED_LIBRARY)

######### Same Module, No Sound ###############################################

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES  := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_ARM_MODE          := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES        := $(MY_LOCAL_C_INCLUDES)
LOCAL_SRC_FILES         := $(MY_LOCAL_SRC_FILES)
LOCAL_CFLAGS            := $(MY_LOCAL_CFLAGS)
LOCAL_CPPFLAGS          := $(MY_LOCAL_CPPFLAGS)
LOCAL_LDFLAGS           := $(MY_LOCAL_LDFLAGS)

# Only difference is the module name and a flag
LOCAL_MODULE := $(MY_LOCAL_MODULE)-nosound
LOCAL_CFLAGS += -DM64P_NO_AUDIO

include $(BUILD_SHARED_LIBRARY)
