#############################
# mupen64plus-video-gliden64
#############################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := ./mupen64plus-video-gliden64/src

MY_LOCAL_MODULE := mupen64plus-video-gliden64
MY_LOCAL_SHARED_LIBRARIES := freetype
MY_LOCAL_STATIC_LIBRARIES := glidenhq osal
MY_LOCAL_ARM_MODE := arm

MY_LOCAL_C_INCLUDES :=                          \
    $(LOCAL_PATH)/$(SRCDIR)                     \
    $(M64P_API_INCLUDES)                        \
    $(SDL_INCLUDES)                             \
    $(FREETYPE_INCLUDES)                        \
    $(LOCAL_PATH)/$(SRCDIR)/osal                \

MY_LOCAL_SRC_FILES :=                               \
    $(SRCDIR)/3DMath.cpp                            \
    $(SRCDIR)/Combiner.cpp                          \
    $(SRCDIR)/CommonPluginAPI.cpp                   \
    $(SRCDIR)/Config.cpp                            \
    $(SRCDIR)/CRC.cpp                               \
    $(SRCDIR)/DepthBuffer.cpp                       \
    $(SRCDIR)/F3D.cpp                               \
    $(SRCDIR)/F3DDKR.cpp                            \
    $(SRCDIR)/F3DEX2CBFD.cpp                        \
    $(SRCDIR)/F3DEX2.cpp                            \
    $(SRCDIR)/F3DEX.cpp                             \
    $(SRCDIR)/F3DPD.cpp                             \
    $(SRCDIR)/F3DSWSE.cpp                           \
    $(SRCDIR)/F3DWRUS.cpp                           \
    $(SRCDIR)/FrameBuffer.cpp                       \
    $(SRCDIR)/GBI.cpp                               \
    $(SRCDIR)/gDP.cpp                               \
    $(SRCDIR)/GLideN64.cpp                          \
    $(SRCDIR)/glState.cpp                           \
    $(SRCDIR)/gSP.cpp                               \
    $(SRCDIR)/Keys.cpp                              \
    $(SRCDIR)/L3D.cpp                               \
    $(SRCDIR)/L3DEX2.cpp                            \
    $(SRCDIR)/L3DEX.cpp                             \
    $(SRCDIR)/MupenPlusPluginAPI.cpp                \
    $(SRCDIR)/N64.cpp                               \
    $(SRCDIR)/OpenGL.cpp                            \
    $(SRCDIR)/PostProcessor.cpp                     \
    $(SRCDIR)/RDP.cpp                               \
    $(SRCDIR)/RSP.cpp                               \
    $(SRCDIR)/S2DEX2.cpp                            \
    $(SRCDIR)/S2DEX.cpp                             \
    $(SRCDIR)/Textures.cpp                          \
    $(SRCDIR)/Turbo3D.cpp                           \
    $(SRCDIR)/VI.cpp                                \
    $(SRCDIR)/ZSort.cpp                             \
    $(SRCDIR)/ShaderUtils.cpp                       \
    $(SRCDIR)/common/CommonAPIImpl_common.cpp       \
    $(SRCDIR)/mupenplus/CommonAPIImpl_mupenplus.cpp \
    $(SRCDIR)/mupenplus/Config_mupenplus.cpp        \
    $(SRCDIR)/mupenplus/MupenPlusAPIImpl.cpp        \
    $(SRCDIR)/mupenplus/OpenGL_mupenplus.cpp        \
    $(SRCDIR)/TextDrawer.cpp                        \

MY_LOCAL_CFLAGS :=      \
    $(COMMON_CFLAGS)    \
    -g                  \
    -DTXFILTER_LIB      \
    -DANDROID           \
    -DUSE_SDL           \
    -DMUPENPLUSAPI      \
    -fsigned-char       \
    #-DSDL_NO_COMPAT     \

MY_LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS) -std=c++11 -g

MY_LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/mupenplus/video_api_export.ver

MY_LOCAL_LDLIBS := -llog

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    #MY_LOCAL_SRC_FILES += gSPNeon.cpp.neon
    #MY_LOCAL_SRC_FILES += 3DMathNeon.cpp.neon 
    MY_LOCAL_CFLAGS += -DARM_ASM
    MY_LOCAL_CFLAGS += -D__NEON_OPT
    
else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    
else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here
    
else
    # Any other architectures that Android could be running on?
    
endif

###########
# gles 2.0
###########
include $(CLEAR_VARS)
LOCAL_MODULE            := $(MY_LOCAL_MODULE)-gles20
LOCAL_SHARED_LIBRARIES  := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_STATIC_LIBRARIES  := $(MY_LOCAL_STATIC_LIBRARIES)
LOCAL_ARM_MODE          := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES        := $(MY_LOCAL_C_INCLUDES)
LOCAL_SRC_FILES         := $(MY_LOCAL_SRC_FILES) $(SRCDIR)/GLES2/UniformSet.cpp $(SRCDIR)/GLES2/GLSLCombiner_gles2.cpp
LOCAL_CFLAGS            := $(MY_LOCAL_CFLAGS) -DGLES2
LOCAL_CPPFLAGS          := $(MY_LOCAL_CPPFLAGS)
LOCAL_LDFLAGS           := $(MY_LOCAL_LDFLAGS)
LOCAL_LDLIBS            := $(MY_LOCAL_LDLIBS) -lGLESv2
include $(BUILD_SHARED_LIBRARY)

ifneq ($(TARGET_ARCH_ABI), armeabi)

###########
# gles 3.0
###########
include $(CLEAR_VARS)
LOCAL_MODULE            := $(MY_LOCAL_MODULE)-gles30
LOCAL_SHARED_LIBRARIES  := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_STATIC_LIBRARIES  := $(MY_LOCAL_STATIC_LIBRARIES)
LOCAL_ARM_MODE          := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES        := $(MY_LOCAL_C_INCLUDES) $(LOCAL_PATH)/GLES3/include/
LOCAL_SRC_FILES         := $(MY_LOCAL_SRC_FILES) $(SRCDIR)/OGL3X/UniformBlock.cpp $(SRCDIR)/OGL3X/GLSLCombiner_ogl3x.cpp
LOCAL_CFLAGS            := $(MY_LOCAL_CFLAGS) -DGLES3
LOCAL_CPPFLAGS          := $(MY_LOCAL_CPPFLAGS)
LOCAL_LDFLAGS           := $(MY_LOCAL_LDFLAGS)

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    LOCAL_LDLIBS        := $(MY_LOCAL_LDLIBS) -L$(LOCAL_PATH)/GLES3/lib/arm/ -lGLESv3
else ifeq ($(TARGET_ARCH_ABI), x86)
    LOCAL_LDLIBS        := $(MY_LOCAL_LDLIBS) -L$(LOCAL_PATH)/GLES3/lib/x86/ -lGLESv3
else
    # Any other architectures that Android could be running on?
endif

include $(BUILD_SHARED_LIBRARY)

###########
# gles 3.1
###########
include $(CLEAR_VARS)
LOCAL_MODULE            := $(MY_LOCAL_MODULE)-gles31
LOCAL_SHARED_LIBRARIES  := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_STATIC_LIBRARIES  := $(MY_LOCAL_STATIC_LIBRARIES)
LOCAL_ARM_MODE          := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES        := $(MY_LOCAL_C_INCLUDES) $(LOCAL_PATH)/GLES3/include/
LOCAL_SRC_FILES         := $(MY_LOCAL_SRC_FILES) $(SRCDIR)/OGL3X/UniformBlock.cpp $(SRCDIR)/OGL3X/GLSLCombiner_ogl3x.cpp
LOCAL_CFLAGS            := $(MY_LOCAL_CFLAGS) -DGLES3_1
LOCAL_CPPFLAGS          := $(MY_LOCAL_CPPFLAGS)
LOCAL_LDFLAGS           := $(MY_LOCAL_LDFLAGS)

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    LOCAL_LDLIBS        := $(MY_LOCAL_LDLIBS) -L$(LOCAL_PATH)/GLES3/lib/arm/ -lGLESv3
else ifeq ($(TARGET_ARCH_ABI), x86)
    LOCAL_LDLIBS        := $(MY_LOCAL_LDLIBS) -L$(LOCAL_PATH)/GLES3/lib/x86/ -lGLESv3
else
    # Any other architectures that Android could be running on?
endif

include $(BUILD_SHARED_LIBRARY)

endif #not armebi