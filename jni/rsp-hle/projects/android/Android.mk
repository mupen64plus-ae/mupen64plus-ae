LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := ../../src

LOCAL_MODULE := rsp-hle
LOCAL_SHARED_LIBRARIES := core
LOCAL_STATIC_LIBRARIES := cpufeatures
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

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DNO_ASM            \

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver

LOCAL_LDLIBS := $(COMMON_LDLIBS)

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)

###############################################################################

include $(CLEAR_VARS)
SRCDIR := ../../src

LOCAL_MODULE := rsp-hle-nosound
LOCAL_SHARED_LIBRARIES := core
LOCAL_STATIC_LIBRARIES := cpufeatures
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

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DNO_ASM            \

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver

LOCAL_LDLIBS := $(COMMON_LDLIBS)

LOCAL_CFLAGS += -DM64P_NO_AUDIO

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)