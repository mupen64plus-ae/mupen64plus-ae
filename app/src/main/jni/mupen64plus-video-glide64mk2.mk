###############################
# mupen64plus-video-glide64mk2
###############################

LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := ./mupen64plus-video-glide64mk2/src

MY_LOCAL_SHARED_LIBRARIES := SDL2
MY_LOCAL_STATIC_LIBRARIES := png
MY_LOCAL_ARM_MODE := arm

MY_LOCAL_C_INCLUDES :=                          \
    $(LOCAL_PATH)/$(SRCDIR)/Glitch64/inc        \
    $(M64P_API_INCLUDES)                        \

MY_LOCAL_SRC_FILES :=                           \
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

MY_LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DUSE_FRAMESKIPPER  \
    -DNOSSE             \
    -DNO_ASM            \
    -fsigned-char       \
    
MY_LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS) -Wno-unused-value -std=c++11
    
MY_LOCAL_CPP_FEATURES := exceptions

MY_LOCAL_LDFLAGS := $(COMMON_LDFLAGS) -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

MY_LOCAL_LDLIBS :=         \
    -ldl                \
    -llog               \

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    MY_LOCAL_CFLAGS += -mfpu=vfp
    MY_LOCAL_CFLAGS += -mfloat-abi=softfp
    
else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    
else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here
    
else
    # Any other architectures that Android could be running on?
    
endif

include $(CLEAR_VARS)
LOCAL_MODULE := mupen64plus-video-glide64mk2
LOCAL_SHARED_LIBRARIES := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_STATIC_LIBRARIES := $(MY_LOCAL_STATIC_LIBRARIES)
LOCAL_ARM_MODE := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES := $(MY_LOCAL_C_INCLUDES)
LOCAL_SRC_FILES := $(MY_LOCAL_SRC_FILES) $(SRCDIR)/Glitch64/OGLEScombiner.cpp \
    $(SRCDIR)/Glitch64/OGLESgeometry.cpp $(SRCDIR)/Glitch64/OGLESglitchmain.cpp $(SRCDIR)/Glitch64/OGLEStextures.cpp
LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS) -DUSE_GLES
LOCAL_CPPFLAGS := $(MY_LOCAL_CPPFLAGS)
LOCAL_CPP_FEATURES := $(MY_LOCAL_CPP_FEATURES)
LOCAL_LDFLAGS := $(MY_LOCAL_LDFLAGS)
LOCAL_LDLIBS :=  $(MY_LOCAL_LDLIBS) -lGLESv2
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := mupen64plus-video-glide64mk2-egl
LOCAL_SHARED_LIBRARIES := $(MY_LOCAL_SHARED_LIBRARIES)
LOCAL_STATIC_LIBRARIES := $(MY_LOCAL_STATIC_LIBRARIES) EGLLoader
LOCAL_ARM_MODE := $(MY_LOCAL_ARM_MODE)
LOCAL_C_INCLUDES := $(MY_LOCAL_C_INCLUDES) $(GL_INCLUDES) $(LOCAL_PATH)/$(SRCDIR)/Glitch64
LOCAL_SRC_FILES := $(MY_LOCAL_SRC_FILES) $(SRCDIR)/Glitch64/OGLcombiner.cpp \
    $(SRCDIR)/Glitch64/OGLgeometry.cpp $(SRCDIR)/../../mupen64plus-video-glide64mk2-egl/src/Glitch64/OGLglitchmain.cpp \
    $(SRCDIR)/Glitch64/OGLtextures.cpp
LOCAL_CFLAGS := $(MY_LOCAL_CFLAGS) -DEGL -DSDL_VIDEO_OPENGL -D_SDL_opengl_h
LOCAL_CPPFLAGS := $(MY_LOCAL_CPPFLAGS) -include $(SRCDIR)/../../GL/GL/EGLLoader.h
LOCAL_CPP_FEATURES := $(MY_LOCAL_CPP_FEATURES)
LOCAL_LDFLAGS := $(MY_LOCAL_LDFLAGS)
LOCAL_LDLIBS :=  $(MY_LOCAL_LDLIBS) -lEGL
include $(BUILD_SHARED_LIBRARY)
