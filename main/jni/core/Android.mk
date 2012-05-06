LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := core
LOCAL_ARM_MODE := arm

CORE_PATH := ../../../core-debug

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
# Use for ARM7a:
	LOCAL_SRC_FILES := $(CORE_PATH)/libs/armeabi-v7a/libcore.so
else ifeq ($(TARGET_ARCH_ABI), armeabi)
# Use for pre-ARM7a:
	LOCAL_SRC_FILES := $(CORE_PATH)/libs/armeabi/libcore.so
else ifeq ($(TARGET_ARCH_ABI), x86)
	# TODO: set the proper flags here
else
	# Any other architectures that Android could be running on?
endif

include $(PREBUILT_SHARED_LIBRARY)
