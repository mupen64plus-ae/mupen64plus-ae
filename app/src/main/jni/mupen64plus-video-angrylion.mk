######################
# mupen64plus-rsp-angrylion
######################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := mupen64plus-video-angrylion

MY_LOCAL_CFLAGS := $(COMMON_CFLAGS) -DM64P_PLUGIN_API -DM64P_CORE_PROTOTYPES

LOCAL_MODULE := mupen64plus-video-angrylion
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := $(M64P_API_INCLUDES) $(LIBRETRO_INCLUDES)/libretro-common/include $(LIBRETRO_INCLUDES) \
    $(LOCAL_PATH)/GLES3/include/

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    MY_LOCAL_CFLAGS += -mfpu=neon -mfloat-abi=softfp -ftree-vectorize -funsafe-math-optimizations

else ifeq ($(TARGET_ARCH_ABI), x86)
    MY_LOCAL_CFLAGS += -DUSE_SSE_SUPPORT
endif

LOCAL_SRC_FILES := \
    $(SRCDIR)/src/n64video.c \
    $(SRCDIR)/src/n64video_main.c \
    $(SRCDIR)/src/n64video_vi.c \
    $(SRCDIR)/osal_dynamiclib_unix.c \
    $(SRCDIR)/main.c \
    $(SRCDIR)/config_functions.c \
    $(SRCDIR)/../libretro/libretro_custom.c \
    $(SRCDIR)/../libretro/RefreshScreen.c \
    $(SRCDIR)/../libretro/Graphics/plugins.c

LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS)
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
LOCAL_LDLIBS := -llog -L$(LOCAL_PATH)/GLES3/lib/$(TARGET_ARCH_ABI)/ -lGLESv3

include $(BUILD_SHARED_LIBRARY)
