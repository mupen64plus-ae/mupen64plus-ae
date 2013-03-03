LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := front-end
#LOCAL_ARM_MODE := arm
SRCDIR := ../../src

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SRCDIR)
LOCAL_C_INCLUDES += $(SDL_INCLUDES)
LOCAL_C_INCLUDES += $(M64P_API_INCLUDES)

LOCAL_SRC_FILES := $(SDL_MAIN_ENTRY) \
	$(SRCDIR)/cheat.c \
	$(SRCDIR)/compare_core.c \
	$(SRCDIR)/core_interface.c \
	$(SRCDIR)/main.c \
	$(SRCDIR)/plugin.c \
	$(SRCDIR)/osal_dynamiclib_unix.c \
	$(SRCDIR)/osal_files_unix.c

LOCAL_CFLAGS := -DNO_ASM

LOCAL_CFLAGS += -O3 -ffast-math -frename-registers -fomit-frame-pointer -fsingle-precision-constant -fpredictive-commoning -fno-strict-aliasing -fvisibility=hidden
LOCAL_CPPFLAGS += -fvisibility-inlines-hidden

LOCAL_SHARED_LIBRARIES := SDL core

LOCAL_LDLIBS := -ldl -lGLESv1_CM -lGLESv2 -llog

include $(BUILD_SHARED_LIBRARY)
