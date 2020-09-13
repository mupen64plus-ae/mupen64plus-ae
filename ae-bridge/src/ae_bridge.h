#ifndef __VIDEXT_H__
#define __VIDEXT_H__
#include "m64p_types.h"
#include <android/log.h>

#ifndef DECLSPEC
# if defined(__BEOS__) || defined(__HAIKU__)
#  if defined(__GNUC__)
#   define DECLSPEC __declspec(dllexport)
#  else
#   define DECLSPEC __declspec(export)
#  endif
# elif defined(__WIN32__)
#  ifdef __BORLANDC__
#   ifdef BUILD_SDL
#    define DECLSPEC
#   else
#    define DECLSPEC    __declspec(dllimport)
#   endif
#  else
#   define DECLSPEC __declspec(dllexport)
#  endif
# else
#  if defined(__GNUC__) && __GNUC__ >= 4
#   define DECLSPEC __attribute__ ((visibility("default")))
#  else
#   define DECLSPEC
#  endif
# endif
#endif

// Generally we should use the core's Debug API, but these can be used in a pinch
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "ae-bridge", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   "ae-bridge", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    "ae-bridge", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,    "ae-bridge", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "ae-bridge", __VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif
extern m64p_error VidExtFuncInit(void);
extern m64p_error VidExtFuncQuit(void);
extern m64p_error VidExtFuncListModes(m64p_2d_size *SizeArray, int *NumSizes);
extern m64p_error VidExtFuncListRates(m64p_2d_size, int *, int *);
extern m64p_error VidExtFuncSetMode(int Width, int Height, int BitsPerPixel, int ScreenMode, int Flags);
extern m64p_error VidExtFuncSetModeWithRate(int, int, int, int, int, int);
extern m64p_error VidExtFuncSetCaption(const char *Title);
extern m64p_error VidExtFuncToggleFS(void);
extern m64p_error VidExtFuncResizeWindow(int Width, int Height);
extern m64p_function VidExtFuncGLGetProc(const char *Proc);
extern m64p_error VidExtFuncGLSetAttr(m64p_GLattr Attr, int Value);
extern m64p_error VidExtFuncGLGetAttr(m64p_GLattr Attr, int *pValue);
extern m64p_error VidExtFuncGLSwapBuf(void);
extern uint32_t VidExtFuncGLGetDefaultFramebuffer(void);
extern void vsyncEnabled(int enabled);
extern void pauseEmulator();
extern void resumeEmulator();

m64p_video_extension_functions vidExtFunctions = {14,
                                                  VidExtFuncInit,
                                                  VidExtFuncQuit,
                                                  VidExtFuncListModes,
                                                  VidExtFuncListRates,
                                                  VidExtFuncSetMode,
                                                  VidExtFuncSetModeWithRate,
                                                  VidExtFuncGLGetProc,
                                                  VidExtFuncGLSetAttr,
                                                  VidExtFuncGLGetAttr,
                                                  VidExtFuncGLSwapBuf,
                                                  VidExtFuncSetCaption,
                                                  VidExtFuncToggleFS,
                                                  VidExtFuncResizeWindow,
                                                  VidExtFuncGLGetDefaultFramebuffer};
#ifdef __cplusplus
}
#endif
#endif
