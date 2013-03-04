LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := audio-sdl
#LOCAL_ARM_MODE := arm
SRCDIR := ../../src

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SRCDIR)
LOCAL_C_INCLUDES += $(SDL_INCLUDES)
LOCAL_C_INCLUDES += $(M64P_API_INCLUDES)
LOCAL_C_INCLUDES += $(SAMPLERATE_INCLUDES)

LOCAL_SRC_FILES := \
	$(SRCDIR)/main.c \
	$(SRCDIR)/volume.c \
	$(SRCDIR)/osal_dynamiclib_unix.c
    
LOCAL_CFLAGS := -DNO_ASM -DUSE_SRC

LOCAL_CFLAGS += -O3 -ffast-math -frename-registers -fomit-frame-pointer -fsingle-precision-constant -fpredictive-commoning -fno-strict-aliasing -fvisibility=hidden

LOCAL_LDLIBS := -ldl -llog
LOCAL_SHARED_LIBRARIES := SDL core
LOCAL_STATIC_LIBRARIES := cpufeatures samplerate

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)
