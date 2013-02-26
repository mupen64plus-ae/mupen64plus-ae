LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := gles2n64
LOCAL_ARM_MODE := arm
SRCDIR := $(shell readlink $(LOCAL_PATH)/src)src

SDL_PATH := ../SDL
CORE_PATH := ../core

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SDL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(CORE_PATH)/src/api
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SRCDIR)

LOCAL_SRC_FILES := \
	$(SRCDIR)/Config.cpp \
	$(SRCDIR)/gles2N64.cpp \
	$(SRCDIR)/OpenGL.cpp \
	$(SRCDIR)/N64.cpp \
	$(SRCDIR)/RSP.cpp \
	$(SRCDIR)/VI.cpp \
	$(SRCDIR)/Textures.cpp \
	$(SRCDIR)/ShaderCombiner.cpp \
	$(SRCDIR)/gDP.cpp \
	$(SRCDIR)/gSP.cpp \
	$(SRCDIR)/GBI.cpp \
	$(SRCDIR)/DepthBuffer.cpp \
	$(SRCDIR)/CRC.cpp \
	$(SRCDIR)/2xSAI.cpp \
	$(SRCDIR)/RDP.cpp \
	$(SRCDIR)/F3D.cpp \
	$(SRCDIR)/F3DEX.cpp \
	$(SRCDIR)/F3DEX2.cpp \
	$(SRCDIR)/L3D.cpp \
	$(SRCDIR)/L3DEX.cpp \
	$(SRCDIR)/L3DEX2.cpp \
	$(SRCDIR)/S2DEX.cpp \
	$(SRCDIR)/S2DEX2.cpp \
	$(SRCDIR)/F3DPD.cpp \
	$(SRCDIR)/F3DDKR.cpp \
	$(SRCDIR)/F3DWRUS.cpp \
	$(SRCDIR)/F3DCBFD.cpp \
	$(SRCDIR)/3DMath.cpp \
	$(SRCDIR)/ticks.c \
	$(SRCDIR)/FrameSkipper.cpp

LOCAL_CFLAGS := -DANDROID
LOCAL_CFLAGS += -D__VEC4_OPT
LOCAL_CFLAGS += -D__CRC_OPT
LOCAL_CFLAGS += -D__HASHMAP_OPT
LOCAL_CFLAGS += -D__TRIBUFFER_OPT

LOCAL_CFLAGS += -DUSE_SDL
#LOCAL_CFLAGS += -DSDL_NO_COMPAT

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
# Use for ARM7a:
	LOCAL_CFLAGS += -DARM_ASM
	LOCAL_CFLAGS += -D__NEON_OPT

	LOCAL_SRC_FILES += \
		$(SRCDIR)/gSPNeon.cpp.neon \
		$(SRCDIR)/3DMathNeon.cpp.neon
else ifeq ($(TARGET_ARCH_ABI), armeabi)
# Use for pre-ARM7a:
else ifeq ($(TARGET_ARCH_ABI), x86)
	# TODO: set the proper flags here
else
	# Any other architectures that Android could be running on?
endif

LOCAL_CFLAGS += -O3 -ffast-math -frename-registers -fomit-frame-pointer -fsingle-precision-constant -fpredictive-commoning -fno-strict-aliasing -fvisibility=hidden
LOCAL_CFLAGS += -fsigned-char
LOCAL_CFLAGS += -Wno-psabi
LOCAL_CXXFLAGS += -fvisibility-inlines-hidden

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS := -ldl -lGLESv1_CM -lGLESv2 -llog
LOCAL_SHARED_LIBRARIES := SDL core
LOCAL_STATIC_LIBRARIES := cpufeatures

include $(BUILD_SHARED_LIBRARY)

$(call import-module, android/cpufeatures)
