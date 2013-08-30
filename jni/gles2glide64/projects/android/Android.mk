LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := ../../src

LOCAL_MODULE := gles2glide64
LOCAL_SHARED_LIBRARIES := ae-imports SDL2 core
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=                             \
    $(LOCAL_PATH)/$(SRCDIR)/Glitch64/inc        \
    $(M64P_API_INCLUDES)                        \
    $(PNG_INCLUDES)                             \
    $(SDL_INCLUDES)                             \
    $(AE_BRIDGE_INCLUDES)                       \

LOCAL_SRC_FILES :=                              \
    $(SRCDIR)/Glitch64/combiner.cpp             \
    $(SRCDIR)/Glitch64/geometry.cpp             \
    $(SRCDIR)/Glitch64/glitchmain.cpp           \
    $(SRCDIR)/Glitch64/textures.cpp             \
    $(SRCDIR)/Glide64/osal_dynamiclib_unix.c    \
    $(SRCDIR)/Glide64/3dmath.cpp                \
    $(SRCDIR)/Glide64/Combine.cpp               \
    $(SRCDIR)/Glide64/Config.cpp                \
    $(SRCDIR)/Glide64/CRC.cpp                   \
    $(SRCDIR)/Glide64/Debugger.cpp              \
    $(SRCDIR)/Glide64/DepthBufferRender.cpp     \
    $(SRCDIR)/Glide64/FBtoScreen.cpp            \
    $(SRCDIR)/Glide64/FrameSkipper.cpp          \
    $(SRCDIR)/Glide64/Ini.cpp                   \
    $(SRCDIR)/Glide64/Keys.cpp                  \
    $(SRCDIR)/Glide64/Main.cpp                  \
    $(SRCDIR)/Glide64/rdp.cpp                   \
    $(SRCDIR)/Glide64/TexBuffer.cpp             \
    $(SRCDIR)/Glide64/TexCache.cpp              \
    $(SRCDIR)/Glide64/ticks.c                   \
    $(SRCDIR)/Glide64/Util.cpp                  \
#    $(SRCDIR)/GlideHQ/Ext_TxFilter.cpp          \
#    $(SRCDIR)/GlideHQ/TxFilterExport.cpp        \
#    $(SRCDIR)/GlideHQ/TxFilter.cpp              \
#    $(SRCDIR)/GlideHQ/TxCache.cpp               \
#    $(SRCDIR)/GlideHQ/TxTexCache.cpp            \
#    $(SRCDIR)/GlideHQ/TxHiResCache.cpp          \
#    $(SRCDIR)/GlideHQ/TxQuantize.cpp            \
#    $(SRCDIR)/GlideHQ/TxUtil.cpp                \
#    $(SRCDIR)/GlideHQ/TextureFilters.cpp        \
#    $(SRCDIR)/GlideHQ/TextureFilters_2xsai.cpp  \
#    $(SRCDIR)/GlideHQ/TextureFilters_hq2x.cpp   \
#    $(SRCDIR)/GlideHQ/TextureFilters_hq4x.cpp   \
#    $(SRCDIR)/GlideHQ/TxImage.cpp               \
#    $(SRCDIR)/GlideHQ/TxReSample.cpp            \
#    $(SRCDIR)/GlideHQ/TxDbg.cpp                 \
#    $(SRCDIR)/GlideHQ/tc-1.1+/fxt1.c            \
#    $(SRCDIR)/GlideHQ/tc-1.1+/dxtn.c            \
#    $(SRCDIR)/GlideHQ/tc-1.1+/wrapper.c         \
#    $(SRCDIR)/GlideHQ/tc-1.1+/texstore.c        \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DNOSSE             \
    -DNO_ASM            \
    -DPAULSCODE         \
    -fsigned-char       \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
    
LOCAL_CPP_FEATURES := exceptions

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -ldl                \
    -lGLESv2            \
    -llog               \
    -lz                 \

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
