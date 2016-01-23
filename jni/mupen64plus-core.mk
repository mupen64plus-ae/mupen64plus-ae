###################
# mupen64plus-core
###################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
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
    $(SRCDIR)/ai/ai_controller.c                \
    $(SRCDIR)/api/callbacks.c                   \
    $(SRCDIR)/api/common.c                      \
    $(SRCDIR)/api/config.c                      \
    $(SRCDIR)/api/debugger.c                    \
    $(SRCDIR)/api/frontend.c                    \
    $(SRCDIR)/api/vidext.c                      \
    $(SRCDIR)/main/cheat.c                      \
    $(SRCDIR)/main/eep_file.c                   \
    $(SRCDIR)/main/eventloop.c                  \
    $(SRCDIR)/main/fla_file.c                   \
    $(SRCDIR)/main/main.c                       \
    $(SRCDIR)/main/md5.c                        \
    $(SRCDIR)/main/mpk_file.c                   \
    $(SRCDIR)/main/profile.c                    \
    $(SRCDIR)/main/rom.c                        \
    $(SRCDIR)/main/savestates.c                 \
    $(SRCDIR)/main/sdl_key_converter.c          \
    $(SRCDIR)/main/sra_file.c                   \
    $(SRCDIR)/main/util.c                       \
    $(SRCDIR)/main/zip/ioapi.c                  \
    $(SRCDIR)/main/zip/unzip.c                  \
    $(SRCDIR)/main/zip/zip.c                    \
    $(SRCDIR)/memory/memory.c                   \
    $(SRCDIR)/osal/dynamiclib_unix.c            \
    $(SRCDIR)/osal/files_unix.c                 \
    $(SRCDIR)/osd/screenshot.cpp                \
    $(SRCDIR)/pi/cart_rom.c                     \
    $(SRCDIR)/pi/flashram.c                     \
    $(SRCDIR)/pi/pi_controller.c                \
    $(SRCDIR)/pi/sram.c                         \
    $(SRCDIR)/plugin/dummy_audio.c              \
    $(SRCDIR)/plugin/dummy_input.c              \
    $(SRCDIR)/plugin/dummy_rsp.c                \
    $(SRCDIR)/plugin/dummy_video.c              \
    $(SRCDIR)/plugin/emulate_game_controller_via_input_plugin.c \
    $(SRCDIR)/plugin/emulate_speaker_via_audio_plugin.c \
    $(SRCDIR)/plugin/get_time_using_C_localtime.c \
    $(SRCDIR)/plugin/plugin.c                   \
    $(SRCDIR)/plugin/rumble_via_input_plugin.c  \
    $(SRCDIR)/r4300/cached_interp.c             \
    $(SRCDIR)/r4300/cp0.c                       \
    $(SRCDIR)/r4300/cp1.c                       \
    $(SRCDIR)/r4300/empty_dynarec.c             \
    $(SRCDIR)/r4300/exception.c                 \
    $(SRCDIR)/r4300/instr_counters.c            \
    $(SRCDIR)/r4300/interupt.c                  \
    $(SRCDIR)/r4300/mi_controller.c             \
    $(SRCDIR)/r4300/pure_interp.c               \
    $(SRCDIR)/r4300/r4300.c                     \
    $(SRCDIR)/r4300/r4300_core.c                \
    $(SRCDIR)/r4300/recomp.c                    \
    $(SRCDIR)/r4300/reset.c                     \
    $(SRCDIR)/r4300/tlb.c                       \
    $(SRCDIR)/r4300/new_dynarec/new_dynarec.c   \
    $(SRCDIR)/rdp/fb.c                          \
    $(SRCDIR)/rdp/rdp_core.c                    \
    $(SRCDIR)/ri/rdram.c                        \
    $(SRCDIR)/ri/rdram_detection_hack.c         \
    $(SRCDIR)/ri/ri_controller.c                \
    $(SRCDIR)/rsp/rsp_core.c                    \
    $(SRCDIR)/si/af_rtc.c                       \
    $(SRCDIR)/si/cic.c                          \
    $(SRCDIR)/si/eeprom.c                       \
    $(SRCDIR)/si/game_controller.c              \
    $(SRCDIR)/si/mempak.c                       \
    $(SRCDIR)/si/n64_cic_nus_6105.c             \
    $(SRCDIR)/si/pif.c                          \
    $(SRCDIR)/si/rumblepak.c                    \
    $(SRCDIR)/si/si_controller.c                \
    $(SRCDIR)/vi/vi_controller.c                \
    #$(SRCDIR)/debugger/dbg_breakpoints.c        \
    #$(SRCDIR)/debugger/dbg_debugger.c           \
    #$(SRCDIR)/debugger/dbg_decoder.c            \
    #$(SRCDIR)/debugger/dbg_memory.c             \

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

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/arm/linkage_arm.S
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/arm/arm_cpu_features.c
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=3
    LOCAL_CFLAGS += -mfloat-abi=softfp
    LOCAL_CFLAGS += -mfpu=vfp

else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/arm/linkage_arm.S
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/arm/arm_cpu_features.c
    LOCAL_CFLAGS += -DARMv5_ONLY
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=3

else ifeq ($(TARGET_ARCH_ABI), x86)
    # Use for x86:
    LOCAL_ASMFLAGS = -d ELF_TYPE
    LOCAL_SRC_FILES += $(SRCDIR)/r4300/new_dynarec/x86/linkage_x86.asm
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=1

    include $(BUILD_SHARED_LIBRARY)

    # Build PIC-compliant library
    SAVED_PATH := $(LOCAL_PATH)
    SAVED_SHARED_LIBRARIES := $(LOCAL_SHARED_LIBRARIES)
    SAVED_STATIC_LIBRARIES := $(LOCAL_STATIC_LIBRARIES)
    SAVED_C_INCLUDES := $(LOCAL_C_INCLUDES)
    SAVED_SRC_FILES := $(LOCAL_SRC_FILES)
    SAVED_CFLAGS := $(LOCAL_CFLAGS)
    SAVED_LDFLAGS := $(LOCAL_LDFLAGS)

    include $(CLEAR_VARS)
    LOCAL_PATH := $(SAVED_PATH)
    LOCAL_SHARED_LIBRARIES := $(SAVED_SHARED_LIBRARIES)
    LOCAL_STATIC_LIBRARIES := $(SAVED_STATIC_LIBRARIES)
    LOCAL_C_INCLUDES := $(SAVED_C_INCLUDES)
    LOCAL_MODULE := mupen64plus-core-pic

    LOCAL_SRC_FILES := $(filter-out $(SRCDIR)/r4300/empty_dynarec.c \
            $(SRCDIR)/r4300/new_dynarec/new_dynarec.c \
            $(SRCDIR)/r4300/new_dynarec/x86/linkage_x86.asm \
            , $(SAVED_SRC_FILES)) \
        $(SRCDIR)/r4300/x86/assemble.c \
        $(SRCDIR)/r4300/x86/gbc.c \
        $(SRCDIR)/r4300/x86/gcop0.c \
        $(SRCDIR)/r4300/x86/gcop1.c \
        $(SRCDIR)/r4300/x86/gcop1_d.c \
        $(SRCDIR)/r4300/x86/gcop1_l.c \
        $(SRCDIR)/r4300/x86/gcop1_s.c \
        $(SRCDIR)/r4300/x86/gcop1_w.c \
        $(SRCDIR)/r4300/x86/gr4300.c \
        $(SRCDIR)/r4300/x86/gregimm.c \
        $(SRCDIR)/r4300/x86/gspecial.c \
        $(SRCDIR)/r4300/x86/gtlb.c \
        $(SRCDIR)/r4300/x86/regcache.c \
        $(SRCDIR)/r4300/x86/rjump.c
    LOCAL_CFLAGS := $(filter-out -DNEW_DYNAREC=1, $(SAVED_CFLAGS))  \
        -ffast-math -mtune=atom -mssse3 -mfpmath=sse -fPIC
    LOCAL_LDFLAGS := $(SAVED_LDFLAGS)

else ifeq ($(TARGET_ARCH_ABI), mips)
    # Use for MIPS:
    #TODO: Possible to port dynarec from Daedalus? 

else
    # Any other architectures that Android could be running on?

endif

include $(BUILD_SHARED_LIBRARY)
