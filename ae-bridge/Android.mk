JNI_LOCAL_PATH := $(call my-dir)
include $(JNI_LOCAL_PATH)/../build_common/native_common.mk

RCHEEVOS_PATH := $(JNI_LOCAL_PATH)/../ndkLibs/rcheevos

include $(CLEAR_VARS)
LOCAL_MODULE := ae-bridge
LOCAL_STATIC_LIBRARIES := EGLLoader
LOCAL_C_INCLUDES := $(M64P_API_INCLUDES) $(GL_INCLUDES) $(RCHEEVOS_PATH)/include $(RCHEEVOS_PATH)/src
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/src/ae_bridge.cpp \
    $(JNI_LOCAL_PATH)/src/ae_ra_internal.c \
    $(RCHEEVOS_PATH)/src/rc_client.c \
    $(RCHEEVOS_PATH)/src/rc_compat.c \
    $(RCHEEVOS_PATH)/src/rc_util.c \
    $(RCHEEVOS_PATH)/src/rc_version.c \
    $(RCHEEVOS_PATH)/src/rapi/rc_api_common.c \
    $(RCHEEVOS_PATH)/src/rapi/rc_api_info.c \
    $(RCHEEVOS_PATH)/src/rapi/rc_api_runtime.c \
    $(RCHEEVOS_PATH)/src/rapi/rc_api_user.c \
    $(RCHEEVOS_PATH)/src/rcheevos/alloc.c \
    $(RCHEEVOS_PATH)/src/rcheevos/condition.c \
    $(RCHEEVOS_PATH)/src/rcheevos/condset.c \
    $(RCHEEVOS_PATH)/src/rcheevos/consoleinfo.c \
    $(RCHEEVOS_PATH)/src/rcheevos/format.c \
    $(RCHEEVOS_PATH)/src/rcheevos/lboard.c \
    $(RCHEEVOS_PATH)/src/rcheevos/memref.c \
    $(RCHEEVOS_PATH)/src/rcheevos/operand.c \
    $(RCHEEVOS_PATH)/src/rcheevos/richpresence.c \
    $(RCHEEVOS_PATH)/src/rcheevos/runtime.c \
    $(RCHEEVOS_PATH)/src/rcheevos/runtime_progress.c \
    $(RCHEEVOS_PATH)/src/rcheevos/trigger.c \
    $(RCHEEVOS_PATH)/src/rcheevos/value.c \
    $(RCHEEVOS_PATH)/src/rhash/hash.c \
    $(RCHEEVOS_PATH)/src/rhash/hash_disc.c \
    $(RCHEEVOS_PATH)/src/rhash/hash_encrypted.c \
    $(RCHEEVOS_PATH)/src/rhash/hash_rom.c \
    $(RCHEEVOS_PATH)/src/rhash/hash_zip.c \
    $(RCHEEVOS_PATH)/src/rhash/cdreader.c \
    $(RCHEEVOS_PATH)/src/rhash/md5.c \
    $(RCHEEVOS_PATH)/src/rhash/aes.c
LOCAL_CFLAGS := $(COMMON_CFLAGS) -DEGL -DRC_DISABLE_LUA -DRC_CLIENT_SUPPORTS_HASH
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS) -std=c++11 -g
LOCAL_LDFLAGS := $(COMMON_LDFLAGS)
LOCAL_LDLIBS := -llog -lEGL -landroid
include $(BUILD_SHARED_LIBRARY)
