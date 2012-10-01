LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := core
LOCAL_ARM_MODE := arm

SRCDIR := $(shell readlink $(LOCAL_PATH)/src)src

LOCAL_SRC_FILES := \
	$(SRCDIR)/api/callbacks.c \
	$(SRCDIR)/api/common.c \
	$(SRCDIR)/api/config.c \
	$(SRCDIR)/api/debugger.c \
	$(SRCDIR)/api/frontend.c \
	$(SRCDIR)/api/vidext.c \
	$(SRCDIR)/main/main.c \
	$(SRCDIR)/main/util.c \
	$(SRCDIR)/main/cheat.c \
	$(SRCDIR)/main/eventloop.c \
	$(SRCDIR)/main/md5.c \
	$(SRCDIR)/main/rom.c \
	$(SRCDIR)/main/savestates.c \
	$(SRCDIR)/main/zip-1.99.4/ioapi.c \
	$(SRCDIR)/main/zip-1.99.4/zip.c \
	$(SRCDIR)/main/zip-1.99.4/unzip.c \
	$(SRCDIR)/memory/dma.c \
	$(SRCDIR)/memory/flashram.c \
	$(SRCDIR)/memory/memory.c \
	$(SRCDIR)/memory/n64_cic_nus_6105.c \
	$(SRCDIR)/memory/pif.c \
	$(SRCDIR)/memory/tlb.c \
	$(SRCDIR)/osal/dynamiclib_unix.c \
	$(SRCDIR)/osal/files_unix.c \
	$(SRCDIR)/plugin/plugin.c \
	$(SRCDIR)/plugin/dummy_video.c \
	$(SRCDIR)/plugin/dummy_audio.c \
	$(SRCDIR)/plugin/dummy_input.c \
	$(SRCDIR)/plugin/dummy_rsp.c \
	$(SRCDIR)/r4300/r4300.c \
	$(SRCDIR)/r4300/exception.c \
	$(SRCDIR)/r4300/interupt.c \
	$(SRCDIR)/r4300/profile.c \
	$(SRCDIR)/r4300/pure_interp.c \
	$(SRCDIR)/r4300/recomp.c \
    $(SRCDIR)/r4300/reset.c \
	$(SRCDIR)/r4300/empty_dynarec.c \
	$(SRCDIR)/r4300/new_dynarec/new_dynarec.c \
	$(SRCDIR)/r4300/new_dynarec/fpu.c \
	$(SRCDIR)/r4300/new_dynarec/linkage_arm.S

# Removing these doesn't fix the "RAM full of zeros" bug, but they aren't needed anyway:

#LOCAL_SRC_FILES += \
#	$(SRCDIR)/debugger/debugger.c \
#	$(SRCDIR)/debugger/dbg_decoder.c \
#	$(SRCDIR)/debugger/dbg_memory.c \
#	$(SRCDIR)/debugger/dbg_breakpoints.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SRCDIR)
LOCAL_CFLAGS := -DANDROID
#LOCAL_CFLAGS += -DSDL_NO_COMPAT

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/api/api_export.ver
LOCAL_LDLIBS := -llog -lz

SDL_PATH := ../SDL
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SDL_PATH)/include
#Workaround for some reason 4.6 gcc doesnt include the usr/include directory
LOCAL_C_INCLUDES += $(SYSROOT)/usr/include/

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
# Use for ARM7a:
	LOCAL_CFLAGS += -D__arm__
	LOCAL_CFLAGS += -DNEW_DYNAREC
	LOCAL_CFLAGS += -DDYNAREC
	LOCAL_CFLAGS += -mfpu=vfp -mfloat-abi=softfp
	LOCAL_LDFLAGS += -L$(LOCAL_PATH)/$(SDL_PATH)/obj/local/armeabi-v7a
else ifeq ($(TARGET_ARCH_ABI), armeabi)
# Use for pre-ARM7a:
	LOCAL_CFLAGS += -D__arm__
	LOCAL_CFLAGS += -DNEW_DYNAREC
	LOCAL_CFLAGS += -DDYNAREC
	LOCAL_CFLAGS += -DARMv5_ONLY
	LOCAL_LDFLAGS += -L$(LOCAL_PATH)/$(SDL_PATH)/obj/local/armeabi
else ifeq ($(TARGET_ARCH_ABI), x86)
	# TODO: set the proper flags here
else
	# Any other architectures that Android could be running on?
endif

LOCAL_CFLAGS += -O3

LOCAL_CFLAGS += -ffast-math -fno-strict-aliasing

LOCAL_SHARED_LIBRARIES := SDL

include $(BUILD_SHARED_LIBRARY)
