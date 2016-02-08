###############################
# mupen64plus-video-glide64mk2
###############################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := ./mupen64plus-video-glide2gl/src

LOCAL_MODULE := mupen64plus-video-glide2gl
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=                             \
    $(LOCAL_PATH)/$(SRCDIR)/Glitch64            \
    $(LOCAL_PATH)/$(SRCDIR)/Glide64             \
    $(LOCAL_PATH)/$(SRCDIR)/libretro            \
    $(LOCAL_PATH)/$(SRCDIR)                     \
    $(M64P_API_INCLUDES)                        \
    $(PNG_INCLUDES)                             \
    $(SDL_INCLUDES)                             \

LOCAL_SRC_FILES :=                              \
    $(SRCDIR)/Glitch64/geometry.c               \
    $(SRCDIR)/Glitch64/glitch64_combiner.c      \
    $(SRCDIR)/Glitch64/glitch64_textures.c      \
    $(SRCDIR)/Glitch64/glitchmain.c             \
    $(SRCDIR)/Glide64/Combine.c                 \
    $(SRCDIR)/Glide64/DepthBufferRender.c       \
    $(SRCDIR)/Glide64/FBtoScreen.c              \
    $(SRCDIR)/Glide64/glide64_3dmath.c          \
    $(SRCDIR)/Glide64/Glide64_Ini.c             \
    $(SRCDIR)/Glide64/glide64_rdp.c             \
    $(SRCDIR)/Glide64/Glide64_UCode.c           \
    $(SRCDIR)/Glide64/glide64_util.c            \
    $(SRCDIR)/Glide64/glidemain.c               \
    $(SRCDIR)/Glide64/MiClWr.c                  \
    $(SRCDIR)/Glide64/TexCache.c                \
    $(SRCDIR)/Glide64/TexLoad.c                 \
    $(SRCDIR)/libretro/gdp.c                    \
    $(SRCDIR)/libretro/libretro.c               \
    $(SRCDIR)/libretro/libretro_crc.c           \
    $(SRCDIR)/osal/dynamiclib_unix.c    \


LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DUSE_FRAMESKIPPER  \
    -DNOSSE             \
    -DNO_ASM            \
    -DUSE_GLES          \
    -fsigned-char       \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
    
LOCAL_CPP_FEATURES := exceptions

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -ldl                \
    -lGLESv2            \
    -llog               \

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_CFLAGS += -D__ARM_NEON__
    LOCAL_CFLAGS += -mfpu=neon
    LOCAL_CFLAGS += -mfloat-abi=softfp
    
else ifeq ($(TARGET_ARCH_ABI), x86)
    LOCAL_CFLAGS += -DANDROID_X86 -DDYNAREC -D__SSE2__ -D__SSE__ -D__SOFTFP__
    
else
    # Any other architectures that Android could be running on?
    
endif

LOCAL_CFLAGS += -DM64P_CORE_PROTOTYPES -D_ENDUSER_RELEASE -DM64P_PLUGIN_API -D__LIBRETRO__ -DINLINE="inline" -DSDL_VIDEO_OPENGL_ES2=1 -DANDROID -DSINC_LOWER_QUALITY -DHAVE_LOGGER -DHAVE_COMBINE_EXT -fexceptions -DGLES -DHAVE_OPENGLES2 -DDISABLE_3POINT -DUSE_GLES

include $(BUILD_SHARED_LIBRARY)
