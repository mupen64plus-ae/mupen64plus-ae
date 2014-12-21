JNI_LOCAL_PATH := $(call my-dir)

AE_BRIDGE_INCLUDES := $(JNI_LOCAL_PATH)/ae-bridge/
M64P_API_INCLUDES := $(JNI_LOCAL_PATH)/mupen64plus-core/src/api/
SDL_INCLUDES := $(JNI_LOCAL_PATH)/SDL2/include/
PNG_INCLUDES := $(JNI_LOCAL_PATH)/png/include/
SAMPLERATE_INCLUDES := $(JNI_LOCAL_PATH)/libsamplerate/

COMMON_CFLAGS :=                    \
    -O3                             \
    -ffast-math                     \
    -fno-strict-aliasing            \
    -fomit-frame-pointer            \
    -frename-registers              \
    -fsingle-precision-constant     \
    -fvisibility=hidden             \

COMMON_CPPFLAGS :=                  \
    -fvisibility-inlines-hidden     \

include $(call all-subdir-makefiles)

LOCAL_PATH := $(JNI_LOCAL_PATH)


########################
# mupen64plus-audio-sdl
########################
include $(CLEAR_VARS)
SRCDIR := mupen64plus-audio-sdl/src

LOCAL_MODULE := mupen64plus-audio-sdl
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := samplerate
#LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(M64P_API_INCLUDES)    \
    $(SDL_INCLUDES)         \
    $(SAMPLERATE_INCLUDES)  \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/main.c                    \
    $(SRCDIR)/volume.c                  \
    $(SRCDIR)/osal_dynamiclib_unix.c    \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID_EDITION   \
    -DUSE_SRC           \

include $(BUILD_SHARED_LIBRARY)


###################
# mupen64plus-core
###################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-core/src

LOCAL_MODULE := mupen64plus-core
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(LOCAL_PATH)/$(SRCDIR) \
    $(PNG_INCLUDES)         \
    $(SDL_INCLUDES)         \

LOCAL_SRC_FILES :=                              \
    $(SRCDIR)/api/callbacks.c                   \
    $(SRCDIR)/api/common.c                      \
    $(SRCDIR)/api/config.c                      \
    $(SRCDIR)/api/debugger.c                    \
    $(SRCDIR)/api/frontend.c                    \
    $(SRCDIR)/api/vidext.c                      \
    $(SRCDIR)/main/cheat.c                      \
    $(SRCDIR)/main/eventloop.c                  \
    $(SRCDIR)/main/main.c                       \
    $(SRCDIR)/main/md5.c                        \
    $(SRCDIR)/main/profile.c                    \
    $(SRCDIR)/main/rom.c                        \
    $(SRCDIR)/main/savestates.c                 \
    $(SRCDIR)/main/sdl_key_converter.c          \
    $(SRCDIR)/main/util.c                       \
    $(SRCDIR)/main/zip/ioapi.c                  \
    $(SRCDIR)/main/zip/unzip.c                  \
    $(SRCDIR)/main/zip/zip.c                    \
    $(SRCDIR)/memory/dma.c                      \
    $(SRCDIR)/memory/flashram.c                 \
    $(SRCDIR)/memory/memory.c                   \
    $(SRCDIR)/memory/n64_cic_nus_6105.c         \
    $(SRCDIR)/memory/pif.c                      \
    $(SRCDIR)/osal/dynamiclib_unix.c            \
    $(SRCDIR)/osal/files_unix.c                 \
    $(SRCDIR)/osd/screenshot.cpp                \
    $(SRCDIR)/plugin/dummy_audio.c              \
    $(SRCDIR)/plugin/dummy_input.c              \
    $(SRCDIR)/plugin/dummy_rsp.c                \
    $(SRCDIR)/plugin/dummy_video.c              \
    $(SRCDIR)/plugin/plugin.c                   \
    $(SRCDIR)/r4300/cached_interp.c             \
    $(SRCDIR)/r4300/cp0.c                       \
    $(SRCDIR)/r4300/cp1.c                       \
    $(SRCDIR)/r4300/empty_dynarec.c             \
    $(SRCDIR)/r4300/exception.c                 \
    $(SRCDIR)/r4300/instr_counters.c            \
    $(SRCDIR)/r4300/interupt.c                  \
    $(SRCDIR)/r4300/pure_interp.c               \
    $(SRCDIR)/r4300/r4300.c                     \
    $(SRCDIR)/r4300/recomp.c                    \
    $(SRCDIR)/r4300/reset.c                     \
    $(SRCDIR)/r4300/tlb.c                       \
    $(SRCDIR)/r4300/new_dynarec/new_dynarec.c   \
    #$(SRCDIR)/debugger/dbg_breakpoints.c        \
    #$(SRCDIR)/debugger/dbg_decoder.c            \
    #$(SRCDIR)/debugger/dbg_memory.c             \
    #$(SRCDIR)/debugger/debugger.c               \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DIOAPI_NO_64       \
    -DNOCRYPT           \
    -DNOUNCRYPT         \
    -DUSE_GLES=1        \

LOCAL_LDFLAGS :=                                                    \
    -Wl,-Bsymbolic                                                  \
    -Wl,-export-dynamic                                             \
    -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/api/api_export.ver  \

LOCAL_LDLIBS := -lz

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/linkage_arm.S
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=3
    LOCAL_CFLAGS += -mfloat-abi=softfp
    LOCAL_CFLAGS += -mfpu=vfp

else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/linkage_arm.S
    LOCAL_CFLAGS += -DARMv5_ONLY
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=3

else ifeq ($(TARGET_ARCH_ABI), x86)
    # Use for x86:
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/linkage_x86.S
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=1

else ifeq ($(TARGET_ARCH_ABI), mips)
    # Use for MIPS:
    #TODO: Possible to port dynarec from Daedalus? 

else
    # Any other architectures that Android could be running on?

endif

include $(BUILD_SHARED_LIBRARY)


######################
# mupen64plus-rsp-hle
######################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-rsp-hle/src

LOCAL_MODULE := mupen64plus-rsp-hle
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES := $(M64P_API_INCLUDES)

LOCAL_SRC_FILES :=           \
    $(SRCDIR)/alist.c        \
    $(SRCDIR)/alist_audio.c  \
    $(SRCDIR)/alist_naudio.c \
    $(SRCDIR)/alist_nead.c   \
    $(SRCDIR)/audio.c        \
    $(SRCDIR)/cicx105.c      \
    $(SRCDIR)/hle.c          \
    $(SRCDIR)/jpeg.c         \
    $(SRCDIR)/memory.c       \
    $(SRCDIR)/mp3.c          \
    $(SRCDIR)/musyx.c        \
    $(SRCDIR)/plugin.c       \

LOCAL_CFLAGS := $(COMMON_CFLAGS)

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/rsp_api_export.ver

include $(BUILD_SHARED_LIBRARY)


#########################
# mupen64plus-ui-console
#########################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-ui-console/src

LOCAL_MODULE := mupen64plus-ui-console
LOCAL_SHARED_LIBRARIES := ae-imports
#LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(LOCAL_PATH)/$(SRCDIR) \
    $(M64P_API_INCLUDES)    \
    $(SDL_INCLUDES)         \
    $(AE_BRIDGE_INCLUDES)   \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/cheat.c                   \
    $(SRCDIR)/compare_core.c            \
    $(SRCDIR)/core_interface.c          \
    $(SRCDIR)/main.c                    \
    $(SRCDIR)/osal_dynamiclib_unix.c    \
    $(SRCDIR)/osal_files_unix.c         \
    $(SRCDIR)/plugin.c                  \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DANDROID_EDITION   \
    -DNO_ASM            \

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)


###############################
# mupen64plus-video-glide64mk2
###############################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-video-glide64mk2/src

LOCAL_MODULE := mupen64plus-video-glide64mk2
LOCAL_SHARED_LIBRARIES := ae-imports SDL2
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=                             \
    $(LOCAL_PATH)/$(SRCDIR)/Glitch64/inc        \
    $(M64P_API_INCLUDES)                        \
    $(PNG_INCLUDES)                             \
    $(SDL_INCLUDES)                             \
    $(AE_BRIDGE_INCLUDES)                       \

LOCAL_SRC_FILES :=                              \
    $(SRCDIR)/Glitch64/OGLEScombiner.cpp        \
    $(SRCDIR)/Glitch64/OGLESgeometry.cpp        \
    $(SRCDIR)/Glitch64/OGLESglitchmain.cpp      \
    $(SRCDIR)/Glitch64/OGLEStextures.cpp        \
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
    -DANDROID_EDITION   \
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


#########################
# mupen64plus-video-rice
#########################
include $(CLEAR_VARS)
SRCDIR := ./mupen64plus-video-rice/src

LOCAL_MODULE := mupen64plus-video-rice
LOCAL_SHARED_LIBRARIES := ae-imports SDL2
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
    -DUSE_GLES=1        \
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
