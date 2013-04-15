LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := ../../src

LOCAL_MODULE := gles2rice
LOCAL_SHARED_LIBRARIES := ae-imports $(SDL_MODULE) core
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=                     \
    $(SRCDIR)/liblinux                  \
    $(M64P_API_INCLUDES)                \
    $(PNG_INCLUDES)                     \
    $(SDL_INCLUDES)                     \
    $(AE_BRIDGE_INCLUDES)               \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/Blender.cpp               \
    $(SRCDIR)/Combiner.cpp              \
    $(SRCDIR)/CombinerTable.cpp         \
    $(SRCDIR)/Config.cpp                \
    $(SRCDIR)/ConvertImage.cpp          \
    $(SRCDIR)/ConvertImage16.cpp        \
    $(SRCDIR)/Debugger.cpp              \
    $(SRCDIR)/DecodedMux.cpp            \
    $(SRCDIR)/DeviceBuilder.cpp         \
    $(SRCDIR)/DirectXDecodedMux.cpp     \
    $(SRCDIR)/FrameBuffer.cpp           \
    $(SRCDIR)/GeneralCombiner.cpp       \
    $(SRCDIR)/GraphicsContext.cpp       \
    $(SRCDIR)/OGLCombiner.cpp           \
    $(SRCDIR)/OGLDecodedMux.cpp         \
    $(SRCDIR)/OGLExtCombiner.cpp        \
    $(SRCDIR)/OGLExtRender.cpp          \
    $(SRCDIR)/OGLES2FragmentShaders.cpp \
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
    -DPAULSCODE         \
    -fsigned-char       \
    #-DBGR_SHADER        \
    #-DSDL_NO_COMPAT     \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
    
LOCAL_CPP_FEATURES := exceptions

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -lGLESv2            \
    -llog               \

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_CFLAGS += -mfpu=vfp
    LOCAL_CFLAGS += -mfloat-abi=softfp
    
else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    
else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here
    
else
    # Any other architectures that Android could be running on?
    
endif

include $(BUILD_SHARED_LIBRARY)
