#########################
# mupen64plus-ui-console
#########################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := ./upstream/src

LOCAL_MODULE := mupen64plus-ui-console
LOCAL_SHARED_LIBRARIES := ae-imports ae-vidext SDL2 libhidapi
#LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=         \
    $(LOCAL_PATH)/$(SRCDIR) \
    $(M64P_API_INCLUDES)    \
    $(AE_BRIDGE_INCLUDES)   \

LOCAL_SRC_FILES :=                      \
    $(SRCDIR)/cheat.c                   \
    $(SRCDIR)/compare_core.c            \
    $(SRCDIR)/core_interface.c          \
    $(SRCDIR)/main.c                    \
    $(SRCDIR)/osal_dynamiclib_unix.c    \
    $(SRCDIR)/osal_files_unix.c         \
    $(SRCDIR)/plugin.c                  \
    $(SRCDIR)/debugger.c                 \

LOCAL_CFLAGS :=                                 \
    $(COMMON_CFLAGS)                            \
    -DANDROID                                   \
    -DNO_ASM                                    \
    -DCALLBACK_HEADER=ae_imports.h              \
    -DCALLBACK_FUNC=Android_JNI_StateCallback   \
    -DVIDEXT_HEADER=ae_vidext.h                 \

LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS)

LOCAL_LDFLAGS := $(COMMON_LDFLAGS)

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
