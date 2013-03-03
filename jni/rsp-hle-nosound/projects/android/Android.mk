LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := rsp-hle-nosound
LOCAL_ARM_MODE := arm
SRCDIR := ../../src

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SRCDIR)
LOCAL_C_INCLUDES += $(M64P_API_INCLUDES)

LOCAL_SRC_FILES := \
	$(SRCDIR)/main.c \
	$(SRCDIR)/jpeg.c \
	$(SRCDIR)/ucode3.cpp \
	$(SRCDIR)/ucode2.cpp \
	$(SRCDIR)/ucode1.cpp \
	$(SRCDIR)/ucode3mp3.cpp

LOCAL_CFLAGS := -DNO_ASM

LOCAL_CFLAGS += -O3 -ffast-math -frename-registers -fomit-frame-pointer -fsingle-precision-constant -fpredictive-commoning -fno-strict-aliasing -fvisibility=hidden
LOCAL_CPPFLAGS += -fvisibility-inlines-hidden

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver

LOCAL_LDLIBS := -ldl -llog
LOCAL_SHARED_LIBRARIES := core
LOCAL_STATIC_LIBRARIES := cpufeatures

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)
