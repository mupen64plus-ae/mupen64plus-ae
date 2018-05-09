#ifndef __VIDEXT_H__
#define __VIDEXT_H__
#include "m64p_types.h"

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

#ifdef __cplusplus
extern "C" {
#endif
extern m64p_error VidExtFuncInit(void);
extern m64p_error VidExtFuncQuit(void);
extern m64p_error VidExtFuncListModes(m64p_2d_size *SizeArray, int *NumSizes);
extern m64p_error VidExtFuncSetMode(int Width, int Height, int BitsPerPixel, int ScreenMode, int Flags);
extern m64p_error VidExtFuncSetCaption(const char *Title);
extern m64p_error VidExtFuncToggleFS(void);
extern m64p_error VidExtFuncResizeWindow(int Width, int Height);
extern void *VidExtFuncGLGetProc(const char *Proc);
extern m64p_error VidExtFuncGLSetAttr(m64p_GLattr Attr, int Value);
extern m64p_error VidExtFuncGLGetAttr(m64p_GLattr Attr, int *pValue);
extern m64p_error VidExtFuncGLSwapBuf(void);
extern uint32_t VidExtFuncGLGetDefaultFramebuffer(void);
extern void vsyncEnabled(int enabled);
extern void pauseEmulator();
extern void resumeEmulator();

m64p_video_extension_functions vidExtFunctions = {12,
                                                  VidExtFuncInit,
                                                  VidExtFuncQuit,
                                                  VidExtFuncListModes,
                                                  VidExtFuncSetMode,
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
