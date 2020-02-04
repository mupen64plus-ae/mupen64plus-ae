######################
# mupen64plus-rsp-hle
######################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := ./upstream/src

LOCAL_MODULE := mupen64plus-rsp-hle
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)

LOCAL_SRC_FILES :=           \
    $(SRCDIR)/alist.c        \
    $(SRCDIR)/alist_audio.c  \
    $(SRCDIR)/alist_naudio.c \
    $(SRCDIR)/alist_nead.c   \
    $(SRCDIR)/audio.c        \
    $(SRCDIR)/cicx105.c      \
    $(SRCDIR)/hle.c          \
    $(SRCDIR)/jpeg.c         \
    $(SRCDIR)/memory.c       \
    $(SRCDIR)/mp3.c          \
    $(SRCDIR)/musyx.c        \
    $(SRCDIR)/plugin.c       \
    $(SRCDIR)/re2.c          \
    $(SRCDIR)/hvqm.c         \
    $(SRCDIR)/osal_dynamiclib_unix.c          \

LOCAL_CFLAGS := $(COMMON_CFLAGS)

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDFLAGS := $(COMMON_LDFLAGS) -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver

include $(BUILD_SHARED_LIBRARY)
