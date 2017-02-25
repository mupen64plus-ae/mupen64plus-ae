#include <string.h>
#include <stdlib.h>
#include <stdbool.h>
#include <stdarg.h>
#include "m64p_types.h"
#include "m64p_config.h"
#include "m64p_vidext.h"
#include <stdlib.h>
#include "osal_dynamiclib.h"
#include "api/libretro.h"
#include "src/Gfx #1.3.h"
#include <android/log.h>
#include <freetype/include/config/ftstdlib.h>

extern retro_log_printf_t log_cb;
extern unsigned int screen_width, screen_height;
extern uint32_t screen_pitch;
extern void retro_init(void);
extern void retro_shutdown(void);

extern void angrylionChangeWindow(void);
extern void angrylionReadScreen2(void *dest, int *width, int *height, int front);
extern void angrylionDrawScreen (void);
extern void angrylionSetRenderingCallback(void (*callback)(int));
extern int angrylionInitiateGFX (GFX_INFO Gfx_Info);
extern void angrylionMoveScreen (int xpos, int ypos);
extern void angrylionProcessDList(void);
extern void angrylionProcessRDPList(void);
extern void angrylionRomClosed (void);
extern int angrylionRomOpen (void);
extern void angrylionUpdateScreen(void);
extern void angrylionShowCFB (void);
extern void angrylionViStatusChanged (void);
extern void angrylionViWidthChanged (void);
extern void angrylionFBWrite(unsigned int addr, unsigned int size);
extern void angrylionFBRead(unsigned int addr);
extern void angrylionFBGetFrameBufferInfo(void *pinfo);
extern m64p_error angrylionPluginGetVersion(m64p_plugin_type *PluginType, int *PluginVersion, int *APIVersion, const char **PluginNamePtr, int *Capabilities);

GFX_INFO gfx_info;

static void (*l_DebugCallback)(void *, int, const char *) = NULL;
static void *l_DebugCallContext = NULL;

/* definitions of pointers to Core video extension functions */
ptr_VidExt_Init                  CoreVideo_Init = NULL;
ptr_VidExt_Quit                  CoreVideo_Quit = NULL;
ptr_VidExt_ListFullscreenModes   CoreVideo_ListFullscreenModes = NULL;
ptr_VidExt_SetVideoMode          CoreVideo_SetVideoMode = NULL;
ptr_VidExt_SetCaption            CoreVideo_SetCaption = NULL;
ptr_VidExt_ToggleFullScreen      CoreVideo_ToggleFullScreen = NULL;
ptr_VidExt_ResizeWindow          CoreVideo_ResizeWindow = NULL;
ptr_VidExt_GL_GetProcAddress     CoreVideo_GL_GetProcAddress = NULL;
ptr_VidExt_GL_SetAttribute       CoreVideo_GL_SetAttribute = NULL;
ptr_VidExt_GL_GetAttribute       CoreVideo_GL_GetAttribute = NULL;
ptr_VidExt_GL_SwapBuffers        CoreVideo_GL_SwapBuffers = NULL;

extern void InitializeConfigFunctions(m64p_dynlib_handle CoreLibHandle);

/* Global functions */
void DebugMessage(int level, const char *message, ...)
{
   char msgbuf[1024];
   va_list args;

   if (l_DebugCallback == NULL)
      return;

   va_start(args, message);
   vsprintf(msgbuf, message, args);

   (*l_DebugCallback)(l_DebugCallContext, level, msgbuf);

   va_end(args);
}


EXPORT void CALL ChangeWindow (void)
{
   angrylionChangeWindow();
}

EXPORT void CALL ReadScreen2(void *dest, int *width, int *height, int front)
{
   angrylionReadScreen2(dest, width, height, front);
}

EXPORT void CALL SetRenderingCallback(void (*callback)(int))
{
   angrylionSetRenderingCallback(callback);
}

EXPORT m64p_error CALL InitiateGFX (GFX_INFO Gfx_Info)
{
   gfx_info = Gfx_Info;
   angrylionInitiateGFX(Gfx_Info);

   return M64ERR_SUCCESS;
}

 
EXPORT void CALL MoveScreen (int xpos, int ypos)
{
   angrylionMoveScreen(xpos, ypos);
}

 
EXPORT void CALL ProcessDList(void)
{
   angrylionProcessDList();
}

EXPORT void CALL ProcessRDPList(void)
{
   angrylionProcessRDPList();
}

EXPORT void CALL RomClosed (void)
{
   angrylionRomClosed();
}

 
EXPORT m64p_error CALL RomOpen (void)
{
   angrylionRomOpen();

   return M64ERR_SUCCESS;
}

EXPORT void CALL UpdateScreen(void)
{
   angrylionUpdateScreen();
}

EXPORT void CALL ShowCFB (void)
{
   angrylionShowCFB();
}


EXPORT void CALL ViStatusChanged (void)
{
   angrylionViStatusChanged();
}

EXPORT void CALL ViWidthChanged (void)
{
   angrylionViWidthChanged();
}

EXPORT void FBWrite(unsigned int addr, unsigned int size)
{
   angrylionFBWrite(addr, size);
}

EXPORT void CALL FBRead(unsigned int addr)
{
   angrylionFBRead(addr);
}

EXPORT void CALL FBGetFrameBufferInfo(void *pinfo)
{
   angrylionFBGetFrameBufferInfo(pinfo);
}

EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type *PluginType, int *PluginVersion, int *APIVersion, const char **PluginNamePtr, int *Capabilities)
{
   return angrylionPluginGetVersion(PluginType, PluginVersion, APIVersion, PluginNamePtr, Capabilities);
}


EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle CoreLibHandle, void *Context,
                                   void (*DebugCallback)(void *, int, const char *))
{
   InitializeConfigFunctions(CoreLibHandle);

   /* Get the core Video Extension function pointers from the library handle */
   CoreVideo_Init = (ptr_VidExt_Init) osal_dynlib_getproc(CoreLibHandle, "VidExt_Init");
   CoreVideo_Quit = (ptr_VidExt_Quit) osal_dynlib_getproc(CoreLibHandle, "VidExt_Quit");
   CoreVideo_ListFullscreenModes = (ptr_VidExt_ListFullscreenModes) osal_dynlib_getproc(CoreLibHandle, "VidExt_ListFullscreenModes");
   CoreVideo_SetVideoMode = (ptr_VidExt_SetVideoMode) osal_dynlib_getproc(CoreLibHandle, "VidExt_SetVideoMode");
   CoreVideo_SetCaption = (ptr_VidExt_SetCaption) osal_dynlib_getproc(CoreLibHandle, "VidExt_SetCaption");
   CoreVideo_ToggleFullScreen = (ptr_VidExt_ToggleFullScreen) osal_dynlib_getproc(CoreLibHandle, "VidExt_ToggleFullScreen");
   CoreVideo_ResizeWindow = (ptr_VidExt_ResizeWindow) osal_dynlib_getproc(CoreLibHandle, "VidExt_ResizeWindow");
   CoreVideo_GL_GetProcAddress = (ptr_VidExt_GL_GetProcAddress) osal_dynlib_getproc(CoreLibHandle, "VidExt_GL_GetProcAddress");
   CoreVideo_GL_SetAttribute = (ptr_VidExt_GL_SetAttribute) osal_dynlib_getproc(CoreLibHandle, "VidExt_GL_SetAttribute");
   CoreVideo_GL_GetAttribute = (ptr_VidExt_GL_GetAttribute) osal_dynlib_getproc(CoreLibHandle, "VidExt_GL_GetAttribute");
   CoreVideo_GL_SwapBuffers = (ptr_VidExt_GL_SwapBuffers) osal_dynlib_getproc(CoreLibHandle, "VidExt_GL_SwapBuffers");

   CoreVideo_Init();
   CoreVideo_SetVideoMode(screen_width, screen_height, 0, M64VIDEO_FULLSCREEN, (m64p_video_flags) 0);

   retro_init();

   if(&ConfigGetParamBool == NULL)
   {
   __android_log_print(ANDROID_LOG_ERROR, "Angrylion", "ConfigGetParamBool IS NULL");
   }


   //ConfigOpenSection("Video-Angrylion", &l_ConfigAngrylion);

   l_DebugCallback = DebugCallback;
   l_DebugCallContext = Context;

   if(CoreVideo_GL_SwapBuffers == NULL)
   {
      log_cb(RETRO_LOG_DEBUG, "Invalid SwapByffers function");
   }

   return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginShutdown(void)
{
   retro_shutdown();
   CoreVideo_Quit();

   return M64ERR_SUCCESS;
}

int irand(void)
{
   return rand();
}
