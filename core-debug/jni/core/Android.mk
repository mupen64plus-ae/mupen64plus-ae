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
	$(SRCDIR)/main/adler32.c \
	$(SRCDIR)/main/md5.c \
	$(SRCDIR)/main/rom.c \
	$(SRCDIR)/main/ini_reader.c \
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
	$(SRCDIR)/r4300/bc.c \
	$(SRCDIR)/r4300/cop0.c \
	$(SRCDIR)/r4300/cop1.c \
	$(SRCDIR)/r4300/cop1_d.c \
	$(SRCDIR)/r4300/cop1_l.c \
	$(SRCDIR)/r4300/cop1_s.c \
	$(SRCDIR)/r4300/cop1_w.c \
	$(SRCDIR)/r4300/exception.c \
	$(SRCDIR)/r4300/interupt.c \
	$(SRCDIR)/r4300/profile.c \
	$(SRCDIR)/r4300/pure_interp.c \
	$(SRCDIR)/r4300/recomp.c \
	$(SRCDIR)/r4300/special.c \
	$(SRCDIR)/r4300/regimm.c \
	$(SRCDIR)/r4300/tlb.c \
	$(SRCDIR)/r4300/empty_dynarec.c \
	$(SRCDIR)/r4300/new_dynarec/new_dynarec.c \
	$(SRCDIR)/r4300/new_dynarec/fpu.c \
	$(SRCDIR)/r4300/new_dynarec/linkage_arm.S \
	$(SRCDIR)/debugger/debugger.c \
	$(SRCDIR)/debugger/dbg_decoder.c \
	$(SRCDIR)/debugger/dbg_memory.c \
	$(SRCDIR)/debugger/dbg_breakpoints.c

LOCAL_CFLAGS := -I$(LOCAL_PATH)/$(SRCDIR)
LOCAL_CFLAGS += -DANDROID
#LOCAL_CFLAGS += -DSDL_NO_COMPAT

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/api/api_export.ver
LOCAL_LDLIBS := -llog -lz

SDL_PATH := ../../../main
LOCAL_CFLAGS += -I$(LOCAL_PATH)/$(SDL_PATH)/jni/SDL/include

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
LOCAL_LDLIBS += -lSDL

include $(BUILD_SHARED_LIBRARY)
