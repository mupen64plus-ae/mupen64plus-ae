LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := src

LOCAL_MODULE := gles2n64
LOCAL_SHARED_LIBRARIES := ae-imports $(SDL_MODULE) core
LOCAL_STATIC_LIBRARIES := cpufeatures
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(M64P_API_INCLUDES)    \
    $(SDL_INCLUDES)         \
    $(AE_BRIDGE_INCLUDES)   \

LOCAL_SRC_FILES :=                  \
    $(SRCDIR)/2xSAI.cpp             \
    $(SRCDIR)/3DMath.cpp            \
    $(SRCDIR)/Config.cpp            \
    $(SRCDIR)/CRC.cpp               \
    $(SRCDIR)/DepthBuffer.cpp       \
    $(SRCDIR)/F3D.cpp               \
    $(SRCDIR)/F3DCBFD.cpp           \
    $(SRCDIR)/F3DDKR.cpp            \
    $(SRCDIR)/F3DEX.cpp             \
    $(SRCDIR)/F3DEX2.cpp            \
    $(SRCDIR)/F3DPD.cpp             \
    $(SRCDIR)/F3DWRUS.cpp           \
    $(SRCDIR)/FrameSkipper.cpp      \
    $(SRCDIR)/GBI.cpp               \
    $(SRCDIR)/gDP.cpp               \
    $(SRCDIR)/gles2N64.cpp          \
    $(SRCDIR)/gSP.cpp               \
    $(SRCDIR)/L3D.cpp               \
    $(SRCDIR)/L3DEX.cpp             \
    $(SRCDIR)/L3DEX2.cpp            \
    $(SRCDIR)/N64.cpp               \
    $(SRCDIR)/OpenGL.cpp            \
    $(SRCDIR)/RDP.cpp               \
    $(SRCDIR)/RSP.cpp               \
    $(SRCDIR)/S2DEX.cpp             \
    $(SRCDIR)/S2DEX2.cpp            \
    $(SRCDIR)/ShaderCombiner.cpp    \
    $(SRCDIR)/Textures.cpp          \
    $(SRCDIR)/ticks.c               \
    $(SRCDIR)/VI.cpp                \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -D__CRC_OPT         \
    -D__HASHMAP_OPT     \
    -D__TRIBUFFER_OPT   \
    -D__VEC4_OPT        \
    -DANDROID           \
    -DUSE_SDL           \
    -fsigned-char       \
    #-DSDL_NO_COMPAT     \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)
    
LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -lGLESv2            \
    -llog               \

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/gSPNeon.cpp.neon
    LOCAL_SRC_FILES += $(SRCDIR)/3DMathNeon.cpp.neon 
    LOCAL_CFLAGS += -DARM_ASM
    LOCAL_CFLAGS += -D__NEON_OPT

else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:

else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here

else
    # Any other architectures that Android could be running on?

endif

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)
