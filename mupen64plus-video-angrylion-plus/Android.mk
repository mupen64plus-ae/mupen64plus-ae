JNI_LOCAL_PATH := $(call my-dir)
include $(JNI_LOCAL_PATH)/../build_common/native_common.mk

BASE_DIR := upstream
include $(JNI_LOCAL_PATH)/upstream/mupen64plus-video-angrylion-plus.mk

