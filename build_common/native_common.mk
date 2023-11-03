BUILD_VARIANT := release

ifeq ($(NDK_DEBUG), 1)
    BUILD_VARIANT := debug
endif


#hidapi
include $(CLEAR_VARS)
LOCAL_MODULE := libhidapi
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libhidapi.so
include $(PREBUILT_SHARED_LIBRARY)

#PNG
include $(CLEAR_VARS)
LOCAL_MODULE := png
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libpng.a
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/png/
LOCAL_EXPORT_LDLIBS := -lz
include $(PREBUILT_STATIC_LIBRARY)

#GL
include $(CLEAR_VARS)
LOCAL_MODULE := EGLLoader
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libEGLLoader.a
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/GL
include $(PREBUILT_STATIC_LIBRARY)

#SDL_net
include $(CLEAR_VARS)
LOCAL_MODULE := SDL2_net
LOCAL_SRC_FILES := $(JNI_LOCAL_PATH)/../ndkLibs/libs/$(BUILD_VARIANT)/$(TARGET_ARCH_ABI)/libSDL2_net.so
LOCAL_EXPORT_C_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/SDL_net/include/
include $(PREBUILT_SHARED_LIBRARY)

AE_BRIDGE_INCLUDES := $(JNI_LOCAL_PATH)/ae-bridge/
M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/../mupen64plus-core/upstream/src/api/
GL_INCLUDES := $(JNI_LOCAL_PATH)/../ndkLibs/GL

COMMON_FLAGS := -Oz -fcommon -ffast-math -ftree-vectorize -fno-omit-frame-pointer

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
COMMON_FLAGS +=                     \
    -march=armv7-a                  \
    -mfloat-abi=softfp              \
    -mfpu=neon
endif

ifneq ($(BUILD_VARIANT), debug)
ifneq ($(HOST_OS),windows)
    COMMON_FLAGS += -flto
    COMMON_LDFLAGS += $(COMMON_FLAGS)
endif
endif

COMMON_CFLAGS := $(COMMON_FLAGS)    \
    -fvisibility=hidden -Wno-error=implicit-function-declaration

COMMON_CPPFLAGS := $(COMMON_FLAGS)    \
    -fvisibility-inlines-hidden

