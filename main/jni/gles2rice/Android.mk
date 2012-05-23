LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := gles2rice
LOCAL_ARM_MODE := arm
SRCDIR := $(shell readlink $(LOCAL_PATH)/src)src

SDL_PATH := ../SDL
PNG_PATH := ../png
CORE_PATH := ../../../core-debug

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SDL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(PNG_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(CORE_PATH)/jni/core/src/api
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SRCDIR)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SRCDIR)/liblinux

LOCAL_SRC_FILES := \
	$(SRCDIR)/osal_dynamiclib_unix.c \
	$(SRCDIR)/osal_files_unix.c \
	$(SRCDIR)/Blender.cpp \
	$(SRCDIR)/Combiner.cpp \
	$(SRCDIR)/CombinerTable.cpp \
	$(SRCDIR)/Config.cpp \
	$(SRCDIR)/ConvertImage.cpp \
	$(SRCDIR)/ConvertImage16.cpp \
	$(SRCDIR)/Debugger.cpp \
	$(SRCDIR)/DecodedMux.cpp \
	$(SRCDIR)/DirectXDecodedMux.cpp \
	$(SRCDIR)/DeviceBuilder.cpp \
	$(SRCDIR)/FrameBuffer.cpp \
	$(SRCDIR)/GeneralCombiner.cpp \
	$(SRCDIR)/GraphicsContext.cpp \
	$(SRCDIR)/OGLCombiner.cpp \
	$(SRCDIR)/OGLDecodedMux.cpp \
	$(SRCDIR)/OGLExtCombiner.cpp \
	$(SRCDIR)/OGLExtRender.cpp \
	$(SRCDIR)/OGLFragmentShaders.cpp \
	$(SRCDIR)/OGLGraphicsContext.cpp \
	$(SRCDIR)/OGLRender.cpp \
	$(SRCDIR)/OGLRenderExt.cpp \
	$(SRCDIR)/OGLTexture.cpp \
	$(SRCDIR)/Render.cpp \
	$(SRCDIR)/RenderBase.cpp \
	$(SRCDIR)/RenderExt.cpp \
	$(SRCDIR)/RenderTexture.cpp \
	$(SRCDIR)/RSP_Parser.cpp \
	$(SRCDIR)/RSP_S2DEX.cpp \
	$(SRCDIR)/Texture.cpp \
	$(SRCDIR)/TextureFilters.cpp \
	$(SRCDIR)/TextureFilters_2xsai.cpp \
	$(SRCDIR)/TextureFilters_hq2x.cpp \
	$(SRCDIR)/TextureFilters_hq4x.cpp \
	$(SRCDIR)/TextureManager.cpp \
	$(SRCDIR)/VectorMath.cpp \
	$(SRCDIR)/Video.cpp \
	$(SRCDIR)/liblinux/BMGImage.c \
	$(SRCDIR)/liblinux/BMGUtils.c \
	$(SRCDIR)/liblinux/bmp.c \
	$(SRCDIR)/liblinux/pngrw.c \

LOCAL_CFLAGS := -DANDROID
LOCAL_CFLAGS += -DNO_ASM
LOCAL_CFLAGS += -DUSE_SDL
LOCAL_CFLAGS += -DGLES_2
#LOCAL_CFLAGS += -DBGR_SHADER
#LOCAL_CFLAGS += -DSDL_NO_COMPAT

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
# Use for ARM7a:
	LOCAL_CFLAGS += -mfpu=vfp -mfloat-abi=softfp
#	LOCAL_LDFLAGS += -L$(LOCAL_PATH)/$(CORE_PATH)/obj/local/armeabi-v7a
else ifeq ($(TARGET_ARCH_ABI), armeabi)
# Use for pre-ARM7a:
else ifeq ($(TARGET_ARCH_ABI), x86)
	# TODO: set the proper flags here
#	LOCAL_LDFLAGS += -L$(LOCAL_PATH)/$(CORE_PATH)/obj/local/armeabi
else
	# Any other architectures that Android could be running on?
endif

LOCAL_CFLAGS += -O3
#LOCAL_LDLIBS += -lcore

LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += -fsigned-char
LOCAL_CFLAGS += -fexceptions

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS := -ldl -lGLESv1_CM -lGLESv2 -llog -lz
LOCAL_SHARED_LIBRARIES := SDL core
LOCAL_STATIC_LIBRARIES := cpufeatures png

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)
