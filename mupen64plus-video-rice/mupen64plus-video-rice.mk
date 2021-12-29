#########################
# mupen64plus-video-rice
#########################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := ./upstream/src

LOCAL_MODULE := mupen64plus-video-rice
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_SHARED_LIBRARIES := asan

LOCAL_C_INCLUDES :=                     \
    $(LOCAL_PATH)/$(SRCDIR)             \
    $(M64P_API_INCLUDES)                \
    $(LOCAL_PATH)/../ndkLibs/SDL2_stub  \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/Blender.cpp               \
    $(SRCDIR)/Combiner.cpp              \
    $(SRCDIR)/Config.cpp                \
    $(SRCDIR)/ConvertImage.cpp          \
    $(SRCDIR)/ConvertImage16.cpp        \
    $(SRCDIR)/Debugger.cpp              \
    $(SRCDIR)/DeviceBuilder.cpp         \
    $(SRCDIR)/FrameBuffer.cpp           \
    $(SRCDIR)/GraphicsContext.cpp       \
    $(SRCDIR)/OGLCombiner.cpp           \
    $(SRCDIR)/OGLGraphicsContext.cpp    \
    $(SRCDIR)/OGLRender.cpp             \
    $(SRCDIR)/OGLRenderExt.cpp          \
    $(SRCDIR)/OGLTexture.cpp            \
    $(SRCDIR)/Render.cpp                \
    $(SRCDIR)/RenderBase.cpp            \
    $(SRCDIR)/RenderExt.cpp             \
    $(SRCDIR)/RenderTexture.cpp         \
    $(SRCDIR)/RSP_Parser.cpp            \
    $(SRCDIR)/RSP_S2DEX.cpp             \
    $(SRCDIR)/Texture.cpp               \
    $(SRCDIR)/TextureFilters.cpp        \
    $(SRCDIR)/TextureFilters_2xsai.cpp  \
    $(SRCDIR)/TextureFilters_hq2x.cpp   \
    $(SRCDIR)/TextureFilters_hq4x.cpp   \
    $(SRCDIR)/TextureManager.cpp        \
    $(SRCDIR)/VectorMath.cpp            \
    $(SRCDIR)/Video.cpp                 \
    $(SRCDIR)/osal_dynamiclib_unix.c    \
    $(SRCDIR)/osal_files_unix.c         \
    $(SRCDIR)/liblinux/BMGImage.c       \
    $(SRCDIR)/liblinux/BMGUtils.c       \
    $(SRCDIR)/liblinux/bmp.c            \
    $(SRCDIR)/liblinux/pngrw.c          \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DNO_ASM            \
    -DUSE_GLES=1        \
    -fsigned-char       \
    #-DBGR_SHADER        \
    #-DSDL_NO_COMPAT     \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
    
LOCAL_CPP_FEATURES := exceptions

LOCAL_LDFLAGS := $(COMMON_LDFLAGS) -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -lGLESv2            \
    -llog

include $(BUILD_SHARED_LIBRARY)
