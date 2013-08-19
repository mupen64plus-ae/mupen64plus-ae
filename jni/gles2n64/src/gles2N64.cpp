
#include <dlfcn.h>
#include <string.h>
#include <cpu-features.h>

#include "m64p_types.h"
#include "m64p_plugin.h"

#include "gles2N64.h"
#include "Debug.h"
#include "OpenGL.h"
#include "N64.h"
#include "RSP.h"
#include "RDP.h"
#include "VI.h"
#include "Config.h"
#include "Textures.h"
#include "ShaderCombiner.h"
#include "3DMath.h"
#include "FrameSkipper.h"
#include "ticks.h"

#include "ae_bridge.h"

ptr_ConfigGetSharedDataFilepath ConfigGetSharedDataFilepath = NULL;

static FrameSkipper frameSkipper;

u32         last_good_ucode = (u32) -1;
void        (*CheckInterrupts)( void );
void        (*renderCallback)() = NULL;

extern "C" {

EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle CoreLibHandle,
        void *Context, void (*DebugCallback)(void *, int, const char *))
{
    ConfigGetSharedDataFilepath = (ptr_ConfigGetSharedDataFilepath)
            dlsym(CoreLibHandle, "ConfigGetSharedDataFilepath");

#ifdef __NEON_OPT
    if (android_getCpuFamily() == ANDROID_CPU_FAMILY_ARM &&
            (android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON) != 0)
    {
        MathInitNeon();
        gSPInitNeon();
    }
#endif
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginShutdown(void)
{
    OGL_Stop();  // paulscode, OGL_Stop missing from Yongzh's code
}

EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type *PluginType,
        int *PluginVersion, int *APIVersion, const char **PluginNamePtr,
        int *Capabilities)
{
    /* set version info */
    if (PluginType != NULL)
        *PluginType = M64PLUGIN_GFX;

    if (PluginVersion != NULL)
        *PluginVersion = PLUGIN_VERSION;

    if (APIVersion != NULL)
        *APIVersion = PLUGIN_API_VERSION;
    
    if (PluginNamePtr != NULL)
        *PluginNamePtr = PLUGIN_NAME;

    if (Capabilities != NULL)
    {
        *Capabilities = 0;
    }
                    
    return M64ERR_SUCCESS;
}

EXPORT void CALL ChangeWindow (void)
{
}

EXPORT void CALL MoveScreen (int xpos, int ypos)
{
}

EXPORT int CALL InitiateGFX (GFX_INFO Gfx_Info)
{
    DMEM = Gfx_Info.DMEM;
    IMEM = Gfx_Info.IMEM;
    RDRAM = Gfx_Info.RDRAM;

    REG.MI_INTR = (u32*) Gfx_Info.MI_INTR_REG;
    REG.DPC_START = (u32*) Gfx_Info.DPC_START_REG;
    REG.DPC_END = (u32*) Gfx_Info.DPC_END_REG;
    REG.DPC_CURRENT = (u32*) Gfx_Info.DPC_CURRENT_REG;
    REG.DPC_STATUS = (u32*) Gfx_Info.DPC_STATUS_REG;
    REG.DPC_CLOCK = (u32*) Gfx_Info.DPC_CLOCK_REG;
    REG.DPC_BUFBUSY = (u32*) Gfx_Info.DPC_BUFBUSY_REG;
    REG.DPC_PIPEBUSY = (u32*) Gfx_Info.DPC_PIPEBUSY_REG;
    REG.DPC_TMEM = (u32*) Gfx_Info.DPC_TMEM_REG;

    REG.VI_STATUS = (u32*) Gfx_Info.VI_STATUS_REG;
    REG.VI_ORIGIN = (u32*) Gfx_Info.VI_ORIGIN_REG;
    REG.VI_WIDTH = (u32*) Gfx_Info.VI_WIDTH_REG;
    REG.VI_INTR = (u32*) Gfx_Info.VI_INTR_REG;
    REG.VI_V_CURRENT_LINE = (u32*) Gfx_Info.VI_V_CURRENT_LINE_REG;
    REG.VI_TIMING = (u32*) Gfx_Info.VI_TIMING_REG;
    REG.VI_V_SYNC = (u32*) Gfx_Info.VI_V_SYNC_REG;
    REG.VI_H_SYNC = (u32*) Gfx_Info.VI_H_SYNC_REG;
    REG.VI_LEAP = (u32*) Gfx_Info.VI_LEAP_REG;
    REG.VI_H_START = (u32*) Gfx_Info.VI_H_START_REG;
    REG.VI_V_START = (u32*) Gfx_Info.VI_V_START_REG;
    REG.VI_V_BURST = (u32*) Gfx_Info.VI_V_BURST_REG;
    REG.VI_X_SCALE = (u32*) Gfx_Info.VI_X_SCALE_REG;
    REG.VI_Y_SCALE = (u32*) Gfx_Info.VI_Y_SCALE_REG;

    CheckInterrupts = Gfx_Info.CheckInterrupts;

    Config_LoadConfig();
    Config_LoadRomConfig(Gfx_Info.HEADER);

    ticksInitialize();
    if( config.autoFrameSkip )
        frameSkipper.setSkips( FrameSkipper::AUTO, config.maxFrameSkip );
    else
        frameSkipper.setSkips( FrameSkipper::MANUAL, config.maxFrameSkip );

    OGL_Start();

    return 1;
}

EXPORT void CALL ProcessDList(void)
{
    OGL.frame_dl++;

    if (frameSkipper.willSkipNext())
    {
        OGL.frameSkipped++;
        RSP.busy = FALSE;
        RSP.DList++;

        /* avoid hang on frameskip */
        *REG.MI_INTR |= MI_INTR_DP;
        CheckInterrupts();
        *REG.MI_INTR |= MI_INTR_SP;
        CheckInterrupts();
        return;
    }

    OGL.consecutiveSkips = 0;
    RSP_ProcessDList();
    OGL.mustRenderDlist = true;
}

EXPORT void CALL ProcessRDPList(void)
{
}

EXPORT void CALL ResizeVideoOutput(int Width, int Height)
{
}

EXPORT void CALL RomClosed (void)
{
}

EXPORT int CALL RomOpen (void)
{
    RSP_Init();
    OGL.frame_vsync = 0;
    OGL.frame_dl = 0;
    OGL.frame_prevdl = -1;
    OGL.mustRenderDlist = false;

    frameSkipper.setTargetFPS(config.romPAL ? 50 : 60);
    return 1;
}

EXPORT void CALL RomResumed(void)
{
    frameSkipper.start();
}

EXPORT void CALL ShowCFB (void)
{
}

EXPORT void CALL UpdateScreen (void)
{
    frameSkipper.update();

    //has there been any display lists since last update
    if (OGL.frame_prevdl == OGL.frame_dl) return;

    OGL.frame_prevdl = OGL.frame_dl;

    if (OGL.frame_dl > 0) OGL.frame_vsync++;

    if (OGL.mustRenderDlist)
    {
        OGL.screenUpdate=true;
        VI_UpdateScreen();
        OGL.mustRenderDlist = false;
    }
}

EXPORT void CALL ViStatusChanged (void)
{
}

EXPORT void CALL ViWidthChanged (void)
{
}

/******************************************************************
  Function: FrameBufferRead
  Purpose:  This function is called to notify the dll that the
            frame buffer memory is beening read at the given address.
            DLL should copy content from its render buffer to the frame buffer
            in N64 RDRAM
            DLL is responsible to maintain its own frame buffer memory addr list
            DLL should copy 4KB block content back to RDRAM frame buffer.
            Emulator should not call this function again if other memory
            is read within the same 4KB range

            Since depth buffer is also being watched, the reported addr
            may belong to depth buffer
  input:    addr        rdram address
            val         val
            size        1 = uint8, 2 = uint16, 4 = uint32
  output:   none
*******************************************************************/ 

EXPORT void CALL FBRead(u32 addr)
{
}

/******************************************************************
  Function: FrameBufferWrite
  Purpose:  This function is called to notify the dll that the
            frame buffer has been modified by CPU at the given address.

            Since depth buffer is also being watched, the reported addr
            may belong to depth buffer

  input:    addr        rdram address
            val         val
            size        1 = uint8, 2 = uint16, 4 = uint32
  output:   none
*******************************************************************/ 

EXPORT void CALL FBWrite(u32 addr, u32 size)
{
}

/************************************************************************
Function: FBGetFrameBufferInfo
Purpose:  This function is called by the emulator core to retrieve frame
          buffer information from the video plugin in order to be able
          to notify the video plugin about CPU frame buffer read/write
          operations

          size:
            = 1     byte
            = 2     word (16 bit) <-- this is N64 default depth buffer format
            = 4     dword (32 bit)

          when frame buffer information is not available yet, set all values
          in the FrameBufferInfo structure to 0

input:    FrameBufferInfo pinfo[6]
          pinfo is pointed to a FrameBufferInfo structure which to be
          filled in by this function
output:   Values are return in the FrameBufferInfo structure
          Plugin can return up to 6 frame buffer info
 ************************************************************************/

EXPORT void CALL FBGetFrameBufferInfo(void *p)
{
}

// paulscode, API changed this to "ReadScreen2" in Mupen64Plus 1.99.4
EXPORT void CALL ReadScreen2(void *dest, int *width, int *height, int front)
{
/* TODO: 'int front' was added in 1.99.4.  What to do with this here? */
    OGL_ReadScreen(dest, width, height);
}

EXPORT void CALL SetRenderingCallback(void (*callback)())
{
    renderCallback = callback;
}

EXPORT void CALL SetFrameSkipping(bool autoSkip, int maxSkips)
{
    frameSkipper.setSkips(
            autoSkip ? FrameSkipper::AUTO : FrameSkipper::MANUAL,
            maxSkips);
}

EXPORT void CALL SetStretchVideo(bool stretch)
{
    config.stretchVideo = stretch;
}

EXPORT void CALL StartGL()
{
    OGL_Start();
}

EXPORT void CALL StopGL()
{
    OGL_Stop();
}

EXPORT void CALL ResizeGL(int width, int height)
{
    const float ratio = (config.romPAL ? 9.0f/11.0f : 0.75f);
    int videoWidth = width;
    int videoHeight = height;

    if (!config.stretchVideo) {
        videoWidth = (int) (height / ratio);
        if (videoWidth > width) {
            videoWidth = width;
            videoHeight = (int) (width * ratio);
        }
    }
    int x = (width - videoWidth) / 2;
    int y = (height - videoHeight) / 2;

    OGL_ResizeWindow(x, y, videoWidth, videoHeight);
}

} // extern "C"

