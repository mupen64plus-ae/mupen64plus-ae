LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := glide64mk2
LOCAL_ARM_MODE := arm
SRCDIR := $(shell readlink $(LOCAL_PATH)/src)src

SDL_PATH := ../SDL
PNG_PATH := ../png
CORE_PATH := ../core

#BOOST_DIR = /boost
#ANDROID_NDK_DIR = /android-ndk-r8d

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SDL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(PNG_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(CORE_PATH)/src/api
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SRCDIR)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SRCDIR)/liblinux
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SRCDIR)/Glitch64/inc
#LOCAL_C_INCLUDES += $(BOOST_DIR)

LOCAL_SRC_FILES := \
	$(SRCDIR)/Glitch64/combiner.cpp \
	$(SRCDIR)/Glitch64/geometry.cpp \
	$(SRCDIR)/Glitch64/main.cpp \
	$(SRCDIR)/Glitch64/textures.cpp \
	$(SRCDIR)/Glide64/osal_dynamiclib_unix.c \
	$(SRCDIR)/Glide64/3dmath.cpp \
	$(SRCDIR)/Glide64/Combine.cpp \
	$(SRCDIR)/Glide64/Config.cpp \
	$(SRCDIR)/Glide64/CRC.cpp \
	$(SRCDIR)/Glide64/Debugger.cpp \
	$(SRCDIR)/Glide64/DepthBufferRender.cpp \
	$(SRCDIR)/Glide64/FBtoScreen.cpp \
	$(SRCDIR)/Glide64/Ini.cpp \
	$(SRCDIR)/Glide64/Keys.cpp \
	$(SRCDIR)/Glide64/Main.cpp \
	$(SRCDIR)/Glide64/rdp.cpp \
	$(SRCDIR)/Glide64/TexBuffer.cpp \
	$(SRCDIR)/Glide64/TexCache.cpp \
	$(SRCDIR)/Glide64/Util.cpp \
#	$(SRCDIR)/GlideHQ/Ext_TxFilter.cpp \
#	$(SRCDIR)/GlideHQ/TxFilterExport.cpp \
#	$(SRCDIR)/GlideHQ/TxFilter.cpp \
#	$(SRCDIR)/GlideHQ/TxCache.cpp \
#	$(SRCDIR)/GlideHQ/TxTexCache.cpp \
#	$(SRCDIR)/GlideHQ/TxHiResCache.cpp \
#	$(SRCDIR)/GlideHQ/TxQuantize.cpp \
#	$(SRCDIR)/GlideHQ/TxUtil.cpp \
#	$(SRCDIR)/GlideHQ/TextureFilters.cpp \
#	$(SRCDIR)/GlideHQ/TextureFilters_2xsai.cpp \
#	$(SRCDIR)/GlideHQ/TextureFilters_hq2x.cpp \
#	$(SRCDIR)/GlideHQ/TextureFilters_hq4x.cpp \
#	$(SRCDIR)/GlideHQ/TxImage.cpp \
#	$(SRCDIR)/GlideHQ/TxReSample.cpp \
#	$(SRCDIR)/GlideHQ/TxDbg.cpp \
#	$(SRCDIR)/GlideHQ/tc-1.1+/fxt1.c \
#	$(SRCDIR)/GlideHQ/tc-1.1+/dxtn.c \
#	$(SRCDIR)/GlideHQ/tc-1.1+/wrapper.c \
#	$(SRCDIR)/GlideHQ/tc-1.1+/texstore.c

LOCAL_CFLAGS := -DANDROID
LOCAL_CFLAGS += -DNOSSE
LOCAL_CFLAGS += -DNO_ASM
LOCAL_CFLAGS += -DUSE_SDL
#LOCAL_CFLAGS += -DGLES_2

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

#LOCAL_CFLAGS += -O3 -ffast-math -frename-registers -fomit-frame-pointer -fsingle-precision-constant -fpredictive-commoning -fno-strict-aliasing -fvisibility=hidden
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CXXFLAGS += -fvisibility-inlines-hidden
#LOCAL_LDLIBS += -lcore

LOCAL_CFLAGS += -fsigned-char
LOCAL_CFLAGS += -fexceptions

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

#LOCAL_LDFLAGS += -L$(BOOST_DIR)/android/lib
#LOCAL_LDFLAGS += -L$(ANDROID_NDK_DIR)/sources/cxx-stl/gnu-libstdc++/4.6/libs/armeabi-v7a

LOCAL_LDLIBS += -ldl -llog -lz -lGLESv2
LOCAL_SHARED_LIBRARIES := SDL core
LOCAL_STATIC_LIBRARIES := cpufeatures png 


#LOCAL_LDLIBS += -lboost_chrono -lboost_system -lboost_filesystem -lboost_thread -lgnustl_static -lsupc++
#LOCAL_LDLIBS += -lgnustl_static -lsupc++ 


include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)

