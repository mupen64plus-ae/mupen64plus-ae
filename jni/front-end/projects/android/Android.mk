LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := ../../src

LOCAL_MODULE := front-end
LOCAL_SHARED_LIBRARIES := ae-imports SDL2 core
#LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(LOCAL_PATH)/$(SRCDIR) \
    $(M64P_API_INCLUDES)    \
    $(SDL_INCLUDES)         \
    $(AE_BRIDGE_INCLUDES)   \

LOCAL_SRC_FILES :=                      \
    $(SDL_MAIN_ENTRY)                   \
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
    -DNO_ASM            \
    -DPAULSCODE         \

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
