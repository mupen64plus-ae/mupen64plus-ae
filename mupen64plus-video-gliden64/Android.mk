JNI_LOCAL_PATH := $(call my-dir)
include $(JNI_LOCAL_PATH)/../build_common/native_common.mk

BASE_DIR := upstream

include $(JNI_LOCAL_PATH)/upstream/src/osal/mupen64plus-video-osal.mk
include $(JNI_LOCAL_PATH)/upstream/src/GLideNHQ/mupen64plus-video-glidenhq.mk
include $(JNI_LOCAL_PATH)/upstream/src/mupen64plus-video-gliden64.mk

