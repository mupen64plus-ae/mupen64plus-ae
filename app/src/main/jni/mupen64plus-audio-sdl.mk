########################
# mupen64plus-audio-sdl
########################
include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := mupen64plus-audio-sdl/src

LOCAL_MODULE := mupen64plus-audio-sdl
LOCAL_SHARED_LIBRARIES := SDL2
#LOCAL_STATIC_LIBRARIES := samplerate
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
#    -DUSE_SRC           \

include $(BUILD_SHARED_LIBRARY)
