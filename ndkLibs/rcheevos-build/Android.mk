# Build definition for rcheevos (https://github.com/RetroAchievements/rcheevos)
#
# Kept separate from ndkLibs/rcheevos/ so the vendored upstream source stays
# pristine and can be updated by dropping in a new release unchanged.
#
# This produces librcheevos.a, which is checked in under
# ndkLibs/libs/<variant>/<abi>/ like the project's other prebuilt libraries.
# It is NOT part of the normal app build -- regenerate with build.sh after
# updating the vendored source.

RCHEEVOS_BUILD_PATH := $(call my-dir)

include $(CLEAR_VARS)

# Point LOCAL_PATH at the vendored source so LOCAL_SRC_FILES stays readable
LOCAL_PATH := $(RCHEEVOS_BUILD_PATH)/../rcheevos

LOCAL_MODULE := rcheevos

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include $(LOCAL_PATH)/src

# Consumers get the public headers without repeating these paths
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include $(LOCAL_PATH)/src

LOCAL_SRC_FILES := \
    src/rc_client.c \
    src/rc_compat.c \
    src/rc_util.c \
    src/rc_version.c \
    src/rapi/rc_api_common.c \
    src/rapi/rc_api_info.c \
    src/rapi/rc_api_runtime.c \
    src/rapi/rc_api_user.c \
    src/rcheevos/alloc.c \
    src/rcheevos/condition.c \
    src/rcheevos/condset.c \
    src/rcheevos/consoleinfo.c \
    src/rcheevos/format.c \
    src/rcheevos/lboard.c \
    src/rcheevos/memref.c \
    src/rcheevos/operand.c \
    src/rcheevos/richpresence.c \
    src/rcheevos/runtime.c \
    src/rcheevos/runtime_progress.c \
    src/rcheevos/trigger.c \
    src/rcheevos/value.c \
    src/rhash/hash.c \
    src/rhash/hash_disc.c \
    src/rhash/hash_encrypted.c \
    src/rhash/hash_rom.c \
    src/rhash/hash_zip.c \
    src/rhash/cdreader.c \
    src/rhash/md5.c \
    src/rhash/aes.c

# Mirrors COMMON_CFLAGS from build_common/native_common.mk so the prebuilt
# matches how these sources were compiled when they were part of ae-bridge.
#
# -flto is deliberately omitted: it would embed clang-version-specific bitcode
# in the archive, which stops linking whenever the NDK is upgraded. Plain object
# code links fine into an LTO-enabled consumer, it just doesn't participate in
# cross-module optimisation.
LOCAL_CFLAGS := \
    -Oz -fcommon -ffast-math -ftree-vectorize -fno-omit-frame-pointer \
    -fvisibility=hidden -Wno-error=implicit-function-declaration \
    -DRC_DISABLE_LUA -DRC_CLIENT_SUPPORTS_HASH

# Same ABI-specific flags native_common.mk applies. -mfloat-abi in particular
# must match the consumer or floats passed across the boundary (e.g.
# rc_typed_value_compare_floats) would be read from the wrong registers.
ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
LOCAL_CFLAGS +=                     \
    -march=armv7-a                  \
    -mfloat-abi=softfp              \
    -mfpu=neon
endif

include $(BUILD_STATIC_LIBRARY)
