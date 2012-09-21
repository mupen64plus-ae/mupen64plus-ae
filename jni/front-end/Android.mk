LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := front-end
#LOCAL_ARM_MODE := arm
SRCDIR := $(shell readlink $(LOCAL_PATH)/src)src

SDL_PATH := ../SDL
CORE_PATH := ../core

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SDL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(CORE_PATH)/src
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(CORE_PATH)/src/api
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(CORE_PATH)/src/main
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SRCDIR)

LOCAL_SRC_FILES := $(SDL_PATH)/src/main/android/SDL_android_main.cpp \
	$(SRCDIR)/cheat.c \
	$(SRCDIR)/compare_core.c \
	$(SRCDIR)/core_interface.c \
	$(SRCDIR)/main.c \
	$(SRCDIR)/plugin.c \
	$(SRCDIR)/osal_dynamiclib_unix.c \
	$(SRCDIR)/osal_files_unix.c

LOCAL_CFLAGS := -DNO_ASM
LOCAL_CFLAGS += -I$(LOCAL_PATH)/$(SRCDIR)

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
# Use for ARM7a:
#	LOCAL_LDFLAGS += -L$(LOCAL_PATH)/$(CORE_PATH)/obj/local/armeabi-v7a
else ifeq ($(TARGET_ARCH_ABI), armeabi)
# Use for pre-ARM7a:
#	LOCAL_LDFLAGS += -L$(LOCAL_PATH)/$(CORE_PATH)/obj/local/armeabi
else ifeq ($(TARGET_ARCH_ABI), x86)
	# TODO: set the proper flags here
else
	# Any other architectures that Android could be running on?
endif


LOCAL_CFLAGS += -O3
#LOCAL_LDLIBS += -lcore

LOCAL_SHARED_LIBRARIES := SDL core

LOCAL_LDLIBS := -ldl -lGLESv1_CM -lGLESv2 -llog

include $(BUILD_SHARED_LIBRARY)
