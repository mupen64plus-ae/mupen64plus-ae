######################
# mupen64plus-video-angrylion-rdp-plus
######################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := ./$(BASE_DIR)/src

MY_LOCAL_CFLAGS := $(COMMON_CFLAGS) -Wno-bitwise-op-parentheses -DM64P_PLUGIN_API -DGLES

LOCAL_MODULE := mupen64plus-video-angrylion-rdp-plus
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := $(M64P_API_INCLUDES) $(LOCAL_PATH)/$(SRCDIR)

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    MY_LOCAL_CFLAGS += -mfpu=neon -mfloat-abi=softfp -ftree-vectorize -funsafe-math-optimizations

else ifeq ($(TARGET_ARCH_ABI), x86)
    MY_LOCAL_CFLAGS += -DUSE_SSE_SUPPORT
endif

MY_LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS) -std=c++14 -g

LOCAL_SRC_FILES := \
    $(SRCDIR)/core/parallel.cpp \
    $(SRCDIR)/core/plugin.c \
    $(SRCDIR)/core/rdp.c \
    $(SRCDIR)/core/screen.c \
    $(SRCDIR)/plugin/common/gl_screen.c \
    $(SRCDIR)/plugin/mupen64plus/gfx_m64p.c \
    $(SRCDIR)/plugin/mupen64plus/msg.c \
    $(SRCDIR)/plugin/mupen64plus/plugin.c \
    $(SRCDIR)/plugin/mupen64plus/screen.c

LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS)
LOCAL_CPPFLAGS := $(MY_LOCAL_CPPFLAGS)
LOCAL_LDFLAGS := $(COMMON_LDFLAGS)
LOCAL_LDLIBS := -llog -lGLESv3

include $(BUILD_SHARED_LIBRARY)