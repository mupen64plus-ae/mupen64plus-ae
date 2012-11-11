LOCAL_PATH := $(call my-dir)

###########################
#
# SDL shared library
#
###########################

include $(CLEAR_VARS)

LOCAL_MODULE := SDL

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include

LOCAL_SRC_FILES := \
	$(subst $(LOCAL_PATH)/,, \
	$(wildcard $(LOCAL_PATH)/src/*.c) \
	$(wildcard $(LOCAL_PATH)/src/audio/*.c) \
	$(wildcard $(LOCAL_PATH)/src/audio/android/*.c) \
	$(wildcard $(LOCAL_PATH)/src/audio/dummy/*.c) \
	$(wildcard $(LOCAL_PATH)/src/core/android/*.cpp) \
	$(wildcard $(LOCAL_PATH)/src/cpuinfo/*.c) \
	$(wildcard $(LOCAL_PATH)/src/events/*.c) \
	$(wildcard $(LOCAL_PATH)/src/file/*.c) \
	$(wildcard $(LOCAL_PATH)/src/haptic/*.c) \
	$(wildcard $(LOCAL_PATH)/src/haptic/dummy/*.c) \
	$(wildcard $(LOCAL_PATH)/src/joystick/*.c) \
	$(wildcard $(LOCAL_PATH)/src/joystick/android/*.c) \
	$(wildcard $(LOCAL_PATH)/src/loadso/dlopen/*.c) \
	$(wildcard $(LOCAL_PATH)/src/power/*.c) \
	$(wildcard $(LOCAL_PATH)/src/render/*.c) \
	$(wildcard $(LOCAL_PATH)/src/render/*/*.c) \
	$(wildcard $(LOCAL_PATH)/src/stdlib/*.c) \
	$(wildcard $(LOCAL_PATH)/src/thread/*.c) \
	$(wildcard $(LOCAL_PATH)/src/thread/pthread/*.c) \
	$(wildcard $(LOCAL_PATH)/src/timer/*.c) \
	$(wildcard $(LOCAL_PATH)/src/timer/unix/*.c) \
	$(wildcard $(LOCAL_PATH)/src/video/*.c) \
	$(wildcard $(LOCAL_PATH)/src/video/android/*.c))

SRCDIR := $(shell readlink $(LOCAL_PATH)/src)src
LOCAL_SRC_FILES += $(SRCDIR)/atomic/SDL_atomic.c

LOCAL_LDLIBS := -ldl -lGLESv1_CM -lGLESv2 -llog

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
# Use for ARM7a:
	LOCAL_SRC_FILES += $(SRCDIR)/atomic/SDL_spinlock.c.arm
else ifeq ($(TARGET_ARCH_ABI), armeabi)
# Use for pre-ARM7a:
	LOCAL_SRC_FILES += $(SRCDIR)/atomic/SDL_spinlock.c.arm
else ifeq ($(TARGET_ARCH_ABI), x86)
# Use for x86:
	LOCAL_SRC_FILES += $(SRCDIR)/atomic/SDL_spinlock.c
else ifeq ($(TARGET_ARCH_ABI), mips)
# Use for MIPS:
	# TODO: Re-enable spinlock when implemented for MIPS
	LOCAL_CFLAGS += -DSDL_ATOMIC_DISABLED=1
	LOCAL_SRC_FILES += $(SRCDIR)/atomic/SDL_spinlock.c
else
	# Any other architectures that Android could be running on?
endif


LOCAL_CFLAGS += -O3

include $(BUILD_SHARED_LIBRARY)
