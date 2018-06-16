###################
# mupen64plus-core
###################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := ./upstream/src
ASM_DEFINE_PATH := $(LOCAL_PATH)/upstream/src/asm_defines

LOCAL_MODULE := mupen64plus-core
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_STATIC_LIBRARIES := png
LOCAL_ARM_MODE := arm
LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true

LOCAL_C_INCLUDES :=                       \
    $(LOCAL_PATH)/$(SRCDIR)               \
    $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI) \

LOCAL_SRC_FILES :=                                              \
    $(SRCDIR)/device/rcp/ai/ai_controller.c                     \
    $(SRCDIR)/api/callbacks.c                                   \
    $(SRCDIR)/api/common.c                                      \
    $(SRCDIR)/api/config.c                                      \
    $(SRCDIR)/api/debugger.c                                    \
    $(SRCDIR)/api/frontend.c                                    \
    $(SRCDIR)/api/vidext.c                                      \
    $(SRCDIR)/backends/plugins_compat/audio_plugin_compat.c     \
    $(SRCDIR)/backends/plugins_compat/input_plugin_compat.c     \
    $(SRCDIR)/backends/clock_ctime_plus_delta.c                 \
    $(SRCDIR)/backends/file_storage.c                           \
    $(SRCDIR)/backends/dummy_video_backend.c                    \
    $(SRCDIR)/main/cheat.c                                      \
    $(SRCDIR)/device/device.c                                   \
    $(SRCDIR)/main/eventloop.c                                  \
    $(SRCDIR)/main/main.c                                       \
    $(SRCDIR)/main/md5.c                                        \
    $(SRCDIR)/main/profile.c                                    \
    $(SRCDIR)/main/rom.c                                        \
    $(SRCDIR)/main/savestates.c                                 \
    $(SRCDIR)/main/sdl_key_converter.c                          \
    $(SRCDIR)/main/util.c                                       \
    $(SRCDIR)/main/zip/ioapi.c                                  \
    $(SRCDIR)/main/zip/unzip.c                                  \
    $(SRCDIR)/main/zip/zip.c                                    \
    $(SRCDIR)/main/xxHash/xxhash.c                              \
    $(SRCDIR)/device/memory/memory.c                            \
    $(SRCDIR)/osal/dynamiclib_unix.c                            \
    $(SRCDIR)/osal/files_unix.c                                 \
    $(SRCDIR)/device/cart/af_rtc.c                              \
    $(SRCDIR)/device/cart/cart.c                                \
    $(SRCDIR)/device/cart/eeprom.c                              \
    $(SRCDIR)/device/controllers/paks/mempak.c                  \
    $(SRCDIR)/device/controllers/paks/rumblepak.c               \
    $(SRCDIR)/device/controllers/paks/transferpak.c             \
    $(SRCDIR)/device/controllers/paks/biopak.c                  \
    $(SRCDIR)/device/controllers/game_controller.c              \
    $(SRCDIR)/device/gb/gb_cart.c                               \
    $(SRCDIR)/device/gb/m64282fp.c                              \
    $(SRCDIR)/device/gb/mbc3_rtc.c                              \
    $(SRCDIR)/device/cart/cart_rom.c                            \
    $(SRCDIR)/device/cart/flashram.c                            \
    $(SRCDIR)/device/cart/sram.c                                \
    $(SRCDIR)/device/rcp/pi/pi_controller.c                     \
    $(SRCDIR)/device/pif/bootrom_hle.c                          \
    $(SRCDIR)/plugin/dummy_audio.c                              \
    $(SRCDIR)/plugin/dummy_input.c                              \
    $(SRCDIR)/plugin/dummy_rsp.c                                \
    $(SRCDIR)/plugin/dummy_video.c                              \
    $(SRCDIR)/plugin/plugin.c                                   \
    $(SRCDIR)/device/r4300/cached_interp.c                      \
    $(SRCDIR)/device/r4300/cp0.c                                \
    $(SRCDIR)/device/r4300/cp1.c                                \
    $(SRCDIR)/device/r4300/instr_counters.c                     \
    $(SRCDIR)/device/r4300/interrupt.c                          \
    $(SRCDIR)/device/rcp/mi/mi_controller.c                     \
    $(SRCDIR)/device/r4300/pure_interp.c                        \
    $(SRCDIR)/device/r4300/r4300_core.c                         \
    $(SRCDIR)/device/r4300/tlb.c                                \
    $(SRCDIR)/device/r4300/new_dynarec/new_dynarec.c            \
    $(SRCDIR)/device/r4300/idec.c                               \
    $(SRCDIR)/device/rcp/rdp/rdp_core.c                         \
    $(SRCDIR)/device/rdram/rdram.c                              \
    $(SRCDIR)/device/rcp/ri/ri_controller.c                     \
    $(SRCDIR)/device/rcp/rsp/rsp_core.c                         \
    $(SRCDIR)/device/pif/cic.c                                  \
    $(SRCDIR)/device/pif/n64_cic_nus_6105.c                     \
    $(SRCDIR)/device/pif/pif.c                                  \
    $(SRCDIR)/device/rcp/si/si_controller.c                     \
    $(SRCDIR)/device/rcp/vi/vi_controller.c                     \
    $(SRCDIR)/device/rcp/rdp/fb.c                               \
    $(SRCDIR)/device/dd/dd_controller.c                         \
    $(SRCDIR)/osd/screenshot.c                                  \
    #$(SRCDIR)/debugger/dbg_breakpoints.c                       \
    #$(SRCDIR)/debugger/dbg_debugger.c                          \
    #$(SRCDIR)/debugger/dbg_decoder.c                           \
    #$(SRCDIR)/debugger/dbg_memory.c                            \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -DANDROID           \
    -DIOAPI_NO_64       \
    -DNOCRYPT           \
    -DNOUNCRYPT         \
    -DUSE_GLES=1        \
    -DUSE_SDL

LOCAL_LDFLAGS :=                                                    \
    $(COMMON_LDFLAGS)                                               \
    -Wl,-Bsymbolic                                                  \
    -Wl,-export-dynamic                                             \
    -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/api/api_export.ver  \


ASM_DEFINES_INCLUDE += -I$(SYSROOT_INC)/usr/include
TARGET := ""

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/device/r4300/new_dynarec/arm/linkage_arm.S
    LOCAL_SRC_FILES += $(SRCDIR)/device/r4300/new_dynarec/arm/arm_cpu_features.c
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=3
    ASM_DEFINES_INCLUDE += -isystem $(SYSROOT_INC)/usr/include/arm-linux-androideabi
    TARGET := -target armv7-none-linux-androideabi19

else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    LOCAL_SRC_FILES += $(SRCDIR)/device/r4300/new_dynarec/arm/linkage_arm.S
    LOCAL_SRC_FILES += $(SRCDIR)/device/r4300/new_dynarec/arm/arm_cpu_features.c
    LOCAL_CFLAGS += -DARMv5_ONLY
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=3

else ifeq ($(TARGET_ARCH_ABI), x86)
    # Use for x86:
    LOCAL_SRC_FILES += $(SRCDIR)/device/r4300/new_dynarec/x86/linkage_x86.asm
    LOCAL_CFLAGS += -DDYNAREC
    LOCAL_CFLAGS += -DNEW_DYNAREC=1
    LOCAL_ASMFLAGS = -d PIC
    ASM_DEFINES_INCLUDE += -isystem $(SYSROOT_INC)/usr/include/i686-linux-android
    TARGET := -target i686-none-linux-android

else ifeq ($(TARGET_ARCH_ABI), mips)
    # Use for MIPS:
    #TODO: Possible to port dynarec from Daedalus?

else
    # Any other architectures that Android could be running on?

endif

# Use gawk in linux
AWK_CMD := gawk

# Use awk in windows
ifeq ($(HOST_OS),windows)
    AWK_CMD := awk
endif

# Create folders if they don't exist
ifeq ("$(wildcard $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI))","")
    ifeq ($(HOST_OS),windows)
        # mkdir on windows fail with paths using a forward-slash path separator
        $(shell mkdir upstream\src\asm_defines\$(TARGET_ARCH_ABI))
    else
        $(shell mkdir $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI))
    endif
endif

# Compile asm_defines if it doesn't exist
ifeq ("$(wildcard $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI)/asm_defines.o)","")
    $(shell $(LLVM_TOOLCHAIN_PREFIX)/clang $(TARGET) -c $(ASM_DEFINE_PATH)/asm_defines.c $(LOCAL_CFLAGS) -fno-lto -I$(LOCAL_PATH)/upstream/src $(ASM_DEFINES_INCLUDE) -D__ANDROID_API__=19 -Wno-attributes -o $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI)/asm_defines.o)
endif

# Create asm_defines_gas.h if it doesn't exist
ifeq ("$(wildcard $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI)/asm_defines_gas.h)","")
    $(shell $(AWK_CMD) -v dest_dir="$(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI)" -f $(LOCAL_PATH)/upstream/tools/gen_asm_defines.awk $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI)/asm_defines.o)
endif

# Create asm_defines_nasm.h if it doesn't exist
ifeq ("$(wildcard $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI)/asm_defines_nasm.h)","")
    $(shell $(AWK_CMD) -v dest_dir="$(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI)" -f $(LOCAL_PATH)/upstream/tools/gen_asm_defines.awk $(ASM_DEFINE_PATH)/$(TARGET_ARCH_ABI)/asm_defines.o)
endif

include $(BUILD_SHARED_LIBRARY)
