BUILD_VARIANT := release

ifeq ($(NDK_DEBUG), 1)
    BUILD_VARIANT := debug
endif


#hidapi
include $(CLEAR_VARS)
LOCAL_MODULE := libhidapi
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libhidapi.so
include $(PREBUILT_SHARED_LIBRARY)

#SDL2
#include $(CLEAR_VARS)
#LOCAL_MODULE := SDL2
#LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libSDL2.so
#LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/SDL2/include/
#include $(PREBUILT_SHARED_LIBRARY)

#PNG
include $(CLEAR_VARS)
LOCAL_MODULE := png
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libpng.a
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/png/
LOCAL_EXPORT_LDLIBS := -lz
include $(PREBUILT_STATIC_LIBRARY)

#Soundtouch
include $(CLEAR_VARS)
LOCAL_MODULE := soundtouch
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libsoundtouch.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/soundtouch/include/
include $(PREBUILT_SHARED_LIBRARY)

#Soundtouch floating point
include $(CLEAR_VARS)
LOCAL_MODULE := soundtouch_fp
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libsoundtouch_fp.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/soundtouch/include/
include $(PREBUILT_SHARED_LIBRARY)

#GL
include $(CLEAR_VARS)
LOCAL_MODULE := EGLLoader
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libEGLLoader.a
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/GL
include $(PREBUILT_STATIC_LIBRARY)

#Freetype
include $(CLEAR_VARS)
LOCAL_MODULE := freetype
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libfreetype.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/freetype/include/
include $(PREBUILT_SHARED_LIBRARY)

AE_BRIDGE_INCLUDES := $(JNI_LOCAL_PATH)/ae-bridge/
M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/../mupen64plus-core/upstream/src/api/
GL_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/GL

COMMON_FLAGS := -O3

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
COMMON_FLAGS +=                     \
    -march=armv7-a                  \
    -mfloat-abi=softfp              \
    -mfpu=neon
endif

COMMON_LDFLAGS := -fuse-ld=lld

ifneq ($(BUILD_VARIANT), debug)
ifneq ($(HOST_OS),windows)
    COMMON_FLAGS += -flto
    COMMON_LDFLAGS += $(COMMON_FLAGS)
endif
endif

COMMON_CFLAGS := $(COMMON_FLAGS)    \
    -fomit-frame-pointer            \
    -fvisibility=hidden

COMMON_CPPFLAGS := $(COMMON_FLAGS)    \
    -fvisibility-inlines-hidden

