/*
Copyright (C) 2002 Rice1964

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/

#include <dirent.h>
#include <stdarg.h>
#include <limits.h> // PATH_MAX

#include <SDL_opengl.h>

#include "stdafx.h"
#include "messagebox.h"

#include "../main/version.h"

PluginStatus status;
char generalText[256];
void (*renderCallback)() = NULL;

GFX_INFO g_GraphicsInfo;

uint32 g_dwRamSize = 0x400000;
uint32* g_pRDRAMu32 = NULL;
signed char *g_pRDRAMs8 = NULL;
unsigned char *g_pRDRAMu8 = NULL;

static char g_ConfigDir[PATH_MAX] = {0};

CCritSect g_CritialSection;

///#define USING_THREAD

#ifdef USING_THREAD
HANDLE          videoThread;
HANDLE          threadMsg[5];
HANDLE          threadFinished;

#define RSPMSG_CLOSE            0
#define RSPMSG_SWAPBUFFERS      1
#define RSPMSG_PROCESSDLIST     2
#define RSPMSG_CHANGEWINDOW     3
#define RSPMSG_PROCESSRDPLIST   4
#endif


//=======================================================
// User Options
RECT frameWriteByCPURect;
std::vector<RECT> frameWriteByCPURects;
RECT frameWriteByCPURectArray[20][20];
bool frameWriteByCPURectFlag[20][20];
std::vector<uint32> frameWriteRecord;

//---------------------------------------------------------------------------------------

void GetPluginDir( char * Directory ) 
{
   if(strlen(g_ConfigDir) > 0)
   {
      strncpy(Directory, g_ConfigDir, PATH_MAX);
      // make sure there's a trailing '/'
      if(Directory[strlen(Directory)-1] != '/')
          strncat(Directory, "/", PATH_MAX - strlen(Directory));
   }
   else
   {
      char path[PATH_MAX];
      int n = readlink("/proc/self/exe", path, PATH_MAX);
      if(n == -1) strcpy(path, "./");
      else
        {
           char path2[PATH_MAX];
           int i;
           
           path[n] = '\0';
           strcpy(path2, path);
           for (i=strlen(path2)-1; i>0; i--)
             {
                if(path2[i] == '/') break;
             }
           if(i == 0) strcpy(path, "./");
           else
             {
                DIR *dir;
                struct dirent *entry;
                int gooddir = 0;
                
                path2[i+1] = '\0';
                dir = opendir(path2);
                while((entry = readdir(dir)) != NULL)
                  {
              if(!strcmp(entry->d_name, "plugins"))
                gooddir = 1;
                  }
                closedir(dir);
                if(!gooddir) strcpy(path, "./");
             }
        }
      int i;
      for(i=strlen(path)-1; i>0; i--)
        {
           if(path[i] == '/') break;
        }
      path[i+1] = '\0';
      strcat(path, "plugins/");
      strcpy(Directory, path);
   }
}

//-------------------------------------------------------------------------------------
EXPORT void CALL GetDllInfo ( PLUGIN_INFO * PluginInfo )
{
#ifdef _DEBUG
    sprintf(PluginInfo->Name, "%s %s Debug",project_name, PLUGIN_VERSION);
#else
    sprintf(PluginInfo->Name, "%s %s",project_name, PLUGIN_VERSION);
#endif
    PluginInfo->Version        = 0x0103;
    PluginInfo->Type           = PLUGIN_TYPE_GFX;
    PluginInfo->NormalMemory   = FALSE;
    PluginInfo->MemoryBswaped  = TRUE;
}

//---------------------------------------------------------------------------------------

EXPORT void CALL DllAbout ( HWND hParent )
{
    char temp[300];
    sprintf(temp,"%s %s \nOpenGL 1.1-1.4/ATI/Nvidia TNT/Geforce Extension\n", project_name, PLUGIN_VERSION);
    MsgInfo(temp);
}


//---------------------------------------------------------------------------------------

EXPORT void CALL DllTest ( HWND hParent )
{
    MsgInfo((char*)"TODO: Test");
}

EXPORT void CALL DllConfig ( HWND hParent )
{
   ShowConfigBox();
}

void ChangeWindowStep2()
{
    status.bDisableFPS = true;
    windowSetting.bDisplayFullscreen = 1-windowSetting.bDisplayFullscreen;
    g_CritialSection.Lock();
    windowSetting.bDisplayFullscreen = CGraphicsContext::Get()->ToggleFullscreen();

    CGraphicsContext::Get()->Clear(CLEAR_COLOR_AND_DEPTH_BUFFER);
    CGraphicsContext::Get()->UpdateFrame();
    CGraphicsContext::Get()->Clear(CLEAR_COLOR_AND_DEPTH_BUFFER);
    CGraphicsContext::Get()->UpdateFrame();
    CGraphicsContext::Get()->Clear(CLEAR_COLOR_AND_DEPTH_BUFFER);
    CGraphicsContext::Get()->UpdateFrame();
    g_CritialSection.Unlock();
    status.bDisableFPS = false;
    status.ToToggleFullScreen = FALSE;
}

EXPORT void CALL ChangeWindow (void)
{
    if( status.ToToggleFullScreen )
        status.ToToggleFullScreen = FALSE;
    else
        status.ToToggleFullScreen = TRUE;
}

//---------------------------------------------------------------------------------------

EXPORT void CALL DrawScreen (void)
{
}

//---------------------------------------------------------------------------------------

EXPORT void CALL MoveScreen (int xpos, int ypos)
{ 
}

void Ini_GetRomOptions(LPGAMESETTING pGameSetting);
void Ini_StoreRomOptions(LPGAMESETTING pGameSetting);
void GenerateCurrentRomOptions();

extern void InitExternalTextures(void);
void StartVideo(void)
{
    windowSetting.dps = windowSetting.fps = -1;
    windowSetting.lastSecDlistCount = windowSetting.lastSecFrameCount = 0xFFFFFFFF;

    g_CritialSection.Lock();

    memcpy(&g_curRomInfo.romheader, g_GraphicsInfo.HEADER, sizeof(ROMHeader));
    unsigned char *puc = (unsigned char *) &g_curRomInfo.romheader;
    unsigned int i;
    unsigned char temp;
    for (i = 0; i < sizeof(ROMHeader); i += 4)
    {
        temp     = puc[i];
        puc[i]   = puc[i+3];
        puc[i+3] = temp;
        temp     = puc[i+1];
        puc[i+1] = puc[i+2];
        puc[i+2] = temp;
    }
    ROM_GetRomNameFromHeader(g_curRomInfo.szGameName, &g_curRomInfo.romheader);
    Ini_GetRomOptions(&g_curRomInfo);
    char *p = (char *) g_curRomInfo.szGameName + (strlen((char *) g_curRomInfo.szGameName) -1);     // -1 to skip null
    while (p >= (char *) g_curRomInfo.szGameName)
    {
        if( *p == ':' || *p == '\\' || *p == '/' )
            *p = '-';
        p--;
    }

    GenerateCurrentRomOptions();
    status.dwTvSystem = CountryCodeToTVSystem(g_curRomInfo.romheader.nCountryID);
    if( status.dwTvSystem == TV_SYSTEM_NTSC )
        status.fRatio = 0.75f;
    else
        status.fRatio = 9/11.0f;;
    
    InitExternalTextures();

    try {
        CDeviceBuilder::GetBuilder()->CreateGraphicsContext();
        CGraphicsContext::InitWindowInfo();
        
        windowSetting.bDisplayFullscreen = FALSE;
        bool res = CGraphicsContext::Get()->Initialize(g_GraphicsInfo.hWnd, g_GraphicsInfo.hStatusBar, 640, 480, TRUE);
        CDeviceBuilder::GetBuilder()->CreateRender();
        CRender::GetRender()->Initialize();
        
        if( res )
        {
            DLParser_Init();
        }
        
        status.bGameIsRunning = true;
    }
    catch(...)
    {
        ErrorMsg("Error to start video");
        throw 0;
    }
   
    g_CritialSection.Unlock();
}

extern void CloseExternalTextures(void);
void StopVideo()
{
    if( CGraphicsContext::Get()->IsWindowed() == false )
    {
        status.ToToggleFullScreen = TRUE;
        CGraphicsContext::Get()->ToggleFullscreen();
        status.ToToggleFullScreen = FALSE;
    }

    g_CritialSection.Lock();
    status.bGameIsRunning = false;


    try {
        CloseExternalTextures();

        // Kill all textures?
        gTextureManager.RecycleAllTextures();
        gTextureManager.CleanUp();
        RDP_Cleanup();

        CDeviceBuilder::GetBuilder()->DeleteRender();
        CGraphicsContext::Get()->CleanUp();
        CDeviceBuilder::GetBuilder()->DeleteGraphicsContext();
        }
    catch(...)
    {
        TRACE0("Some exceptions during RomClosed");
    }

    g_CritialSection.Unlock();
    windowSetting.dps = windowSetting.fps = -1;
    windowSetting.lastSecDlistCount = windowSetting.lastSecFrameCount = 0xFFFFFFFF;
    status.gDlistCount = status.gFrameCount = 0;

}

#ifdef USING_THREAD
void ChangeWindowStep2();
void UpdateScreenStep2 (void);
void ProcessDListStep2(void);

//BOOL SwitchToThread(VOID);
uint32 VideoThreadProc( LPVOID lpParameter )
{
    BOOL res;

    StartVideo();
    SetEvent( threadFinished );

    while(true)
    {
        switch (WaitForMultipleObjects( 5, threadMsg, FALSE, INFINITE ))
        {
        case (WAIT_OBJECT_0 + RSPMSG_PROCESSDLIST):
            ProcessDListStep2();
            SetEvent( threadFinished );
            break;
        case (WAIT_OBJECT_0 + RSPMSG_SWAPBUFFERS):
            //res = SwitchToThread();
            //Sleep(1);
            UpdateScreenStep2();
            SetEvent( threadFinished );
            break;
        case (WAIT_OBJECT_0 + RSPMSG_CLOSE):
            StopVideo();
            SetEvent( threadFinished );
            return 1;
        case (WAIT_OBJECT_0 + RSPMSG_CHANGEWINDOW):
            ChangeWindowStep2();
            SetEvent( threadFinished );
            break;
        case (WAIT_OBJECT_0 + RSPMSG_PROCESSRDPLIST):
            try
            {
                RDP_DLParser_Process();
            }
            catch (...)
            {
                ErrorMsg("Unknown Error in ProcessRDPList");
                //TriggerDPInterrupt();
                //TriggerSPInterrupt();
            }
            SetEvent( threadFinished );
            break;
        }
    }
    return 0;
}
#endif

//---------------------------------------------------------------------------------------
EXPORT void CALL RomClosed(void)
{
    TRACE0("To stop video");
    Ini_StoreRomOptions(&g_curRomInfo);
#ifdef USING_THREAD
    if(videoThread)
    {
        SetEvent( threadMsg[RSPMSG_CLOSE] );
        WaitForSingleObject( threadFinished, INFINITE );
        for (int i = 0; i < 5; i++)
        {
            if (threadMsg[i])   CloseHandle( threadMsg[i] );
        }
        CloseHandle( threadFinished );
        CloseHandle( videoThread );
    }
    videoThread = NULL;
#else
    StopVideo();
#endif
    TRACE0("Video is stopped");
}

EXPORT void CALL RomOpen(void)
{
   InitConfiguration();

    if( g_CritialSection.IsLocked() )
    {
        g_CritialSection.Unlock();
        TRACE0("g_CritialSection is locked when game is starting, unlock it now.");
    }
    status.bDisableFPS=false;

   g_dwRamSize = 0x800000;
    
#ifdef _DEBUG
    if( debuggerPause )
    {
        debuggerPause = FALSE;
        usleep(100 * 1000);
    }
#endif

#ifdef USING_THREAD
    uint32 threadID;
    for(int i = 0; i < 5; i++) 
    { 
        threadMsg[i] = CreateEvent( NULL, FALSE, FALSE, NULL );
        if (threadMsg[i] == NULL)
        { 
            ErrorMsg( "Error creating thread message events");
            return;
        } 
    } 
    threadFinished = CreateEvent( NULL, FALSE, FALSE, NULL );
    if (threadFinished == NULL)
    { 
        ErrorMsg( "Error creating video thread finished event");
        return;
    } 
    videoThread = CreateThread( NULL, 4096, VideoThreadProc, NULL, NULL, &threadID );

#else
    StartVideo();
#endif
}


void SetVIScales()
{
    if( g_curRomInfo.VIHeight>0 && g_curRomInfo.VIWidth>0 )
    {
        windowSetting.fViWidth = windowSetting.uViWidth = g_curRomInfo.VIWidth;
        windowSetting.fViHeight = windowSetting.uViHeight = g_curRomInfo.VIHeight;
    }
    else if( g_curRomInfo.UseCIWidthAndRatio && g_CI.dwWidth )
    {
        windowSetting.fViWidth = windowSetting.uViWidth = g_CI.dwWidth;
        windowSetting.fViHeight = windowSetting.uViHeight = 
            g_curRomInfo.UseCIWidthAndRatio == USE_CI_WIDTH_AND_RATIO_FOR_NTSC ? g_CI.dwWidth/4*3 : g_CI.dwWidth/11*9;
    }
    else
    {
        float xscale, yscale;
        uint32 val = *g_GraphicsInfo.VI_X_SCALE_REG & 0xFFF;
        xscale = (float)val / (1<<10);
        uint32 start = *g_GraphicsInfo.VI_H_START_REG >> 16;
        uint32 end = *g_GraphicsInfo.VI_H_START_REG&0xFFFF;
        uint32 width = *g_GraphicsInfo.VI_WIDTH_REG;
        windowSetting.fViWidth = (end-start)*xscale;
        if( abs((int)(windowSetting.fViWidth - width) ) < 8 ) 
        {
            windowSetting.fViWidth = (float)width;
        }
        else
        {
            DebuggerAppendMsg("fViWidth = %f, Width Reg=%d", windowSetting.fViWidth, width);
        }

        val = (*g_GraphicsInfo.VI_Y_SCALE_REG & 0xFFF);// - ((*g_GraphicsInfo.VI_Y_SCALE_REG>>16) & 0xFFF);
        if( val == 0x3FF )  val = 0x400;
        yscale = (float)val / (1<<10);
        start = *g_GraphicsInfo.VI_V_START_REG >> 16;
        end = *g_GraphicsInfo.VI_V_START_REG&0xFFFF;
        windowSetting.fViHeight = (end-start)/2*yscale;

        if( yscale == 0 )
        {
            windowSetting.fViHeight = windowSetting.fViWidth*status.fRatio;
        }
        else
        {
            if( *g_GraphicsInfo.VI_WIDTH_REG > 0x300 ) 
                windowSetting.fViHeight *= 2;

            if( windowSetting.fViWidth*status.fRatio > windowSetting.fViHeight && (*g_GraphicsInfo.VI_X_SCALE_REG & 0xFF) != 0 )
            {
                if( abs(int(windowSetting.fViWidth*status.fRatio - windowSetting.fViHeight)) < 8 )
                {
                    windowSetting.fViHeight = windowSetting.fViWidth*status.fRatio;
                }
                /*
                else
                {
                    if( abs(windowSetting.fViWidth*status.fRatio-windowSetting.fViHeight) > windowSetting.fViWidth*0.1f )
                    {
                        if( status.fRatio > 0.8 )
                            windowSetting.fViHeight = windowSetting.fViWidth*3/4;
                        //windowSetting.fViHeight = (*g_GraphicsInfo.VI_V_SYNC_REG - 0x2C)/2;
                    }
                }
                */
            }
            
            if( windowSetting.fViHeight<100 || windowSetting.fViWidth<100 )
            {
                //At sometime, value in VI_H_START_REG or VI_V_START_REG are 0
                windowSetting.fViWidth = (float)*g_GraphicsInfo.VI_WIDTH_REG;
                windowSetting.fViHeight = windowSetting.fViWidth*status.fRatio;
            }
        }

        windowSetting.uViWidth = (unsigned short)(windowSetting.fViWidth/4);
        windowSetting.fViWidth = windowSetting.uViWidth *= 4;

        windowSetting.uViHeight = (unsigned short)(windowSetting.fViHeight/4);
        windowSetting.fViHeight = windowSetting.uViHeight *= 4;
        uint16 optimizeHeight = (uint16)(windowSetting.uViWidth*status.fRatio);
        optimizeHeight &= ~3;

        uint16 optimizeHeight2 = (uint16)(windowSetting.uViWidth*3/4);
        optimizeHeight2 &= ~3;

        if( windowSetting.uViHeight != optimizeHeight && windowSetting.uViHeight != optimizeHeight2 )
        {
            if( abs(windowSetting.uViHeight-optimizeHeight) <= 8 )
                windowSetting.fViHeight = windowSetting.uViHeight = optimizeHeight;
            else if( abs(windowSetting.uViHeight-optimizeHeight2) <= 8 )
                windowSetting.fViHeight = windowSetting.uViHeight = optimizeHeight2;
        }


        if( gRDP.scissor.left == 0 && gRDP.scissor.top == 0 && gRDP.scissor.right != 0 )
        {
            if( (*g_GraphicsInfo.VI_X_SCALE_REG & 0xFF) != 0x0 && gRDP.scissor.right == windowSetting.uViWidth )
            {
                // Mario Tennis
                if( abs(int( windowSetting.fViHeight - gRDP.scissor.bottom )) < 8 )
                {
                    windowSetting.fViHeight = windowSetting.uViHeight = gRDP.scissor.bottom;
                }
                else if( windowSetting.fViHeight < gRDP.scissor.bottom )
                {
                    windowSetting.fViHeight = windowSetting.uViHeight = gRDP.scissor.bottom;
                }
                windowSetting.fViHeight = windowSetting.uViHeight = gRDP.scissor.bottom;
            }
            else if( gRDP.scissor.right == windowSetting.uViWidth - 1 && gRDP.scissor.bottom != 0 )
            {
                if( windowSetting.uViHeight != optimizeHeight && windowSetting.uViHeight != optimizeHeight2 )
                {
                    if( status.fRatio != 0.75 && windowSetting.fViHeight > optimizeHeight/2 )
                    {
                        windowSetting.fViHeight = windowSetting.uViHeight = gRDP.scissor.bottom + gRDP.scissor.top + 1;
                    }
                }
            }
            else if( gRDP.scissor.right == windowSetting.uViWidth && gRDP.scissor.bottom != 0  && status.fRatio != 0.75 )
            {
                if( windowSetting.uViHeight != optimizeHeight && windowSetting.uViHeight != optimizeHeight2 )
                {
                    if( status.fRatio != 0.75 && windowSetting.fViHeight > optimizeHeight/2 )
                    {
                        windowSetting.fViHeight = windowSetting.uViHeight = gRDP.scissor.bottom + gRDP.scissor.top + 1;
                    }
                }
            }
        }
    }
    SetScreenMult(windowSetting.uDisplayWidth/windowSetting.fViWidth, windowSetting.uDisplayHeight/windowSetting.fViHeight);
}

//---------------------------------------------------------------------------------------
void UpdateScreenStep2 (void)
{
    status.bVIOriginIsUpdated = false;

    if( status.ToToggleFullScreen && status.gDlistCount > 0 )
    {
        ChangeWindowStep2();
        return;
    }

    g_CritialSection.Lock();
    if( status.bHandleN64RenderTexture )
        g_pFrameBufferManager->CloseRenderTexture(true);
    
    g_pFrameBufferManager->SetAddrBeDisplayed(*g_GraphicsInfo.VI_ORIGIN_REG);

    if( status.gDlistCount == 0 )
    {
        // CPU frame buffer update
        uint32 width = *g_GraphicsInfo.VI_WIDTH_REG;
        if( (*g_GraphicsInfo.VI_ORIGIN_REG & (g_dwRamSize-1) ) > width*2 && *g_GraphicsInfo.VI_H_START_REG != 0 && width != 0 )
        {
            SetVIScales();
            CRender::GetRender()->DrawFrameBuffer(true);
            CGraphicsContext::Get()->UpdateFrame();
        }
        g_CritialSection.Unlock();
        return;
    }


    if( status.toCaptureScreen )
    {
        status.toCaptureScreen = false;
        // Capture screen here
        CRender::g_pRender->CaptureScreen(status.screenCaptureFilename);
    }

    TXTRBUF_DETAIL_DUMP(TRACE1("VI ORIG is updated to %08X", *g_GraphicsInfo.VI_ORIGIN_REG));

    if( currentRomOptions.screenUpdateSetting == SCREEN_UPDATE_AT_VI_UPDATE )
    {
        CGraphicsContext::Get()->UpdateFrame();

        DEBUGGER_IF_DUMP( pauseAtNext, TRACE1("Update Screen: VIORIG=%08X", *g_GraphicsInfo.VI_ORIGIN_REG));
        DEBUGGER_PAUSE_COUNT_N_WITHOUT_UPDATE(NEXT_FRAME);
        DEBUGGER_PAUSE_COUNT_N_WITHOUT_UPDATE(NEXT_SET_CIMG);
        g_CritialSection.Unlock();
        return;
    }

    TXTRBUF_DETAIL_DUMP(TRACE1("VI ORIG is updated to %08X", *g_GraphicsInfo.VI_ORIGIN_REG));

    if( currentRomOptions.screenUpdateSetting == SCREEN_UPDATE_AT_VI_UPDATE_AND_DRAWN )
    {
        if( status.bScreenIsDrawn )
        {
            CGraphicsContext::Get()->UpdateFrame();
            DEBUGGER_IF_DUMP( pauseAtNext, TRACE1("Update Screen: VIORIG=%08X", *g_GraphicsInfo.VI_ORIGIN_REG));
        }
        else
        {
            DEBUGGER_IF_DUMP( pauseAtNext, TRACE1("Skip Screen Update: VIORIG=%08X", *g_GraphicsInfo.VI_ORIGIN_REG));
        }

        DEBUGGER_PAUSE_COUNT_N_WITHOUT_UPDATE(NEXT_FRAME);
        DEBUGGER_PAUSE_COUNT_N_WITHOUT_UPDATE(NEXT_SET_CIMG);
        g_CritialSection.Unlock();
        return;
    }

    if( currentRomOptions.screenUpdateSetting==SCREEN_UPDATE_AT_VI_CHANGE )
    {

        if( *g_GraphicsInfo.VI_ORIGIN_REG != status.curVIOriginReg )
        {
            if( *g_GraphicsInfo.VI_ORIGIN_REG < status.curDisplayBuffer || *g_GraphicsInfo.VI_ORIGIN_REG > status.curDisplayBuffer+0x2000  )
            {
                status.curDisplayBuffer = *g_GraphicsInfo.VI_ORIGIN_REG;
                status.curVIOriginReg = status.curDisplayBuffer;
                //status.curRenderBuffer = NULL;

                CGraphicsContext::Get()->UpdateFrame();
                DEBUGGER_IF_DUMP( pauseAtNext, TRACE1("Update Screen: VIORIG=%08X", *g_GraphicsInfo.VI_ORIGIN_REG));
                DEBUGGER_PAUSE_COUNT_N_WITHOUT_UPDATE(NEXT_FRAME);
                DEBUGGER_PAUSE_COUNT_N_WITHOUT_UPDATE(NEXT_SET_CIMG);
            }
            else
            {
                status.curDisplayBuffer = *g_GraphicsInfo.VI_ORIGIN_REG;
                status.curVIOriginReg = status.curDisplayBuffer;
                DEBUGGER_PAUSE_AND_DUMP_NO_UPDATE(NEXT_FRAME, {DebuggerAppendMsg("Skip Screen Update, closed to the display buffer, VIORIG=%08X", *g_GraphicsInfo.VI_ORIGIN_REG);});
            }
        }
        else
        {
            DEBUGGER_PAUSE_AND_DUMP_NO_UPDATE(NEXT_FRAME, {DebuggerAppendMsg("Skip Screen Update, the same VIORIG=%08X", *g_GraphicsInfo.VI_ORIGIN_REG);});
        }

        g_CritialSection.Unlock();
        return;
    }

    if( currentRomOptions.screenUpdateSetting >= SCREEN_UPDATE_AT_1ST_CI_CHANGE )
    {
        status.bVIOriginIsUpdated=true;
        DEBUGGER_PAUSE_AND_DUMP_NO_UPDATE(NEXT_FRAME, {DebuggerAppendMsg("VI ORIG is updated to %08X", *g_GraphicsInfo.VI_ORIGIN_REG);});
        g_CritialSection.Unlock();
        return;
    }

    DEBUGGER_IF_DUMP( pauseAtNext, TRACE1("VI is updated, No screen update: VIORIG=%08X", *g_GraphicsInfo.VI_ORIGIN_REG));
    DEBUGGER_PAUSE_COUNT_N_WITHOUT_UPDATE(NEXT_FRAME);
    DEBUGGER_PAUSE_COUNT_N_WITHOUT_UPDATE(NEXT_SET_CIMG);

    g_CritialSection.Unlock();
}

EXPORT void CALL UpdateScreen(void)
{
    if(options.bShowFPS)
    {
        static unsigned int lastTick=0;
        static int frames=0;
        unsigned int nowTick = SDL_GetTicks();
        frames++;
        if(lastTick + 5000 <= nowTick)
        {
            char caption[200];
            sprintf(caption, "RiceVideoLinux N64 Plugin %s - %.3f VI/S", PLUGIN_VERSION, frames/5.0);
            SDL_WM_SetCaption(caption, caption);
            frames = 0;
            lastTick = nowTick;
        }
    }
#ifdef USING_THREAD
    if (videoThread)
    {
        SetEvent( threadMsg[RSPMSG_SWAPBUFFERS] );
        WaitForSingleObject( threadFinished, INFINITE );
    }
#else
    UpdateScreenStep2();
#endif  
}

//---------------------------------------------------------------------------------------

EXPORT void CALL ViStatusChanged(void)
{
    g_CritialSection.Lock();
    SetVIScales();
    CRender::g_pRender->UpdateClipRectangle();
    g_CritialSection.Unlock();
}

//---------------------------------------------------------------------------------------
EXPORT void CALL ViWidthChanged(void)
{
    g_CritialSection.Lock();
    SetVIScales();
    CRender::g_pRender->UpdateClipRectangle();
    g_CritialSection.Unlock();
}

EXPORT BOOL CALL GetFullScreenStatus(void);
EXPORT void CALL SetOnScreenText(char *msg)
{
    status.CPUCoreMsgIsSet = true;
    memset(&status.CPUCoreMsgToDisplay, 0, 256);
    strncpy(status.CPUCoreMsgToDisplay, msg, 255);
}

EXPORT BOOL CALL GetFullScreenStatus(void)
{
    if( CGraphicsContext::g_pGraphicsContext )
    {
        return CGraphicsContext::g_pGraphicsContext->IsWindowed() ? FALSE : TRUE;
    }
    else
    {
        return FALSE;
    }
}

EXPORT BOOL CALL InitiateGFX(GFX_INFO Gfx_Info)
{
    memset(&status, 0, sizeof(status));
    windowSetting.bDisplayFullscreen = FALSE;
    memcpy(&g_GraphicsInfo, &Gfx_Info, sizeof(GFX_INFO));

    g_pRDRAMu8          = Gfx_Info.RDRAM;
    g_pRDRAMu32         = (uint32*)Gfx_Info.RDRAM;
    g_pRDRAMs8          = (signed char *)Gfx_Info.RDRAM;

    windowSetting.fViWidth = 320;
    windowSetting.fViHeight = 240;
    status.ToToggleFullScreen = FALSE;
    status.bDisableFPS=false;

    InitConfiguration();
    CGraphicsContext::InitWindowInfo();
    CGraphicsContext::InitDeviceParameters();

    gui_init();
    return(TRUE);
}


void __cdecl MsgInfo (char * Message, ...)
{
    char Msg[400];
    va_list ap;

    va_start( ap, Message );
    vsprintf( Msg, Message, ap );
    va_end( ap );

    sprintf(generalText, "%s %s",project_name, PLUGIN_VERSION);
   messagebox(generalText, MB_OK|MB_ICONINFORMATION, Msg);
}

void __cdecl ErrorMsg (const char* Message, ...)
{
    char Msg[400];
    va_list ap;
    
    va_start( ap, Message );
    vsprintf( Msg, Message, ap );
    va_end( ap );
    
    sprintf(generalText, "%s %s",project_name, PLUGIN_VERSION);
   messagebox(generalText, MB_OK|MB_ICONERROR, Msg);
}

//---------------------------------------------------------------------------------------

EXPORT void CALL CloseDLL(void)
{ 
    if( status.bGameIsRunning )
    {
        RomClosed();
    }

    if (bIniIsChanged)
    {
        WriteIniFile();
        TRACE0("Write back INI file");
    }
}

void ProcessDListStep2(void)
{
    g_CritialSection.Lock();
    if( status.toShowCFB )
    {
        CRender::GetRender()->DrawFrameBuffer(true);
        status.toShowCFB = false;
    }

    try
    {
        DLParser_Process((OSTask *)(g_GraphicsInfo.DMEM + 0x0FC0));
    }
    catch (...)
    {
        TRACE0("Unknown Error in ProcessDList");
        TriggerDPInterrupt();
        TriggerSPInterrupt();
    }

    g_CritialSection.Unlock();
}   

EXPORT uint32 CALL ProcessDListCountCycles(void)
{
#ifdef USING_THREAD
    if (videoThread)
    {
        SetEvent( threadMsg[RSPMSG_PROCESSDLIST] );
        WaitForSingleObject( threadFinished, INFINITE );
    }
    return 0;
#else
    g_CritialSection.Lock();
    status.SPCycleCount = 100;
    status.DPCycleCount = 0;
    try
    {
        DLParser_Process((OSTask *)(g_GraphicsInfo.DMEM + 0x0FC0));
    }
    catch (...)
    {
        TRACE0("Unknown Error in ProcessDListCountCycles");
        TriggerDPInterrupt();
        TriggerSPInterrupt();
    }
    status.SPCycleCount *= 6;
    //status.DPCycleCount += status.SPCycleCount;
    //status.DPCycleCount *=4;
    //status.DPCycleCount = min(200,status.DPCycleCount);
    //status.DPCycleCount *= 15;
    status.DPCycleCount *= 5;
    status.DPCycleCount += status.SPCycleCount;

    g_CritialSection.Unlock();
    return (status.DPCycleCount<<16)+status.SPCycleCount;
#endif
}   

EXPORT void CALL ProcessRDPList(void)
{
#ifdef USING_THREAD
    if (videoThread)
    {
        SetEvent( threadMsg[RSPMSG_PROCESSRDPLIST] );
        WaitForSingleObject( threadFinished, INFINITE );
    }
#else
    try
    {
        RDP_DLParser_Process();
    }
    catch (...)
    {
        TRACE0("Unknown Error in ProcessRDPList");
        TriggerDPInterrupt();
        TriggerSPInterrupt();
    }
#endif
}   

EXPORT void CALL ProcessDList(void)
{
#ifdef USING_THREAD
    if (videoThread)
    {
        SetEvent( threadMsg[RSPMSG_PROCESSDLIST] );
        WaitForSingleObject( threadFinished, INFINITE );
    }
#else
    ProcessDListStep2();
#endif
}   

//---------------------------------------------------------------------------------------

void TriggerDPInterrupt(void)
{
    *(g_GraphicsInfo.MI_INTR_REG) |= MI_INTR_DP;
    g_GraphicsInfo.CheckInterrupts();
}

void TriggerSPInterrupt(void)
{
    *(g_GraphicsInfo.MI_INTR_REG) |= MI_INTR_SP;
    g_GraphicsInfo.CheckInterrupts();
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

EXPORT void CALL FBRead(uint32 addr)
{
    g_pFrameBufferManager->FrameBufferReadByCPU(addr);
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

EXPORT void CALL FBWrite(uint32 addr, uint32 size)
{
    g_pFrameBufferManager->FrameBufferWriteByCPU(addr, size);
}

void _VIDEO_DisplayTemporaryMessage(const char *Message)
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
typedef struct
{
    uint32  addr;
    uint32  size;
    uint32  width;
    uint32  height;
} FrameBufferInfo;

EXPORT void CALL FBGetFrameBufferInfo(void *p)
{
    FrameBufferInfo * pinfo = (FrameBufferInfo *)p;
    memset(pinfo,0,sizeof(FrameBufferInfo)*6);

    //if( g_ZI.dwAddr == 0 )
    //{
    //  memset(pinfo,0,sizeof(FrameBufferInfo)*6);
    //}
    //else
    {
        for (int i=0; i<5; i++ )
        {
            if( status.gDlistCount-g_RecentCIInfo[i].lastUsedFrame > 30 || g_RecentCIInfo[i].lastUsedFrame == 0 )
            {
                //memset(&pinfo[i],0,sizeof(FrameBufferInfo));
            }
            else
            {
                pinfo[i].addr = g_RecentCIInfo[i].dwAddr;
                pinfo[i].size = 2;
                pinfo[i].width = g_RecentCIInfo[i].dwWidth;
                pinfo[i].height = g_RecentCIInfo[i].dwHeight;
                TXTRBUF_DETAIL_DUMP(TRACE3("Protect 0x%08X (%d,%d)", g_RecentCIInfo[i].dwAddr, g_RecentCIInfo[i].dwWidth, g_RecentCIInfo[i].dwHeight));
                pinfo[5].width = g_RecentCIInfo[i].dwWidth;
                pinfo[5].height = g_RecentCIInfo[i].dwHeight;
            }
        }

        pinfo[5].addr = g_ZI.dwAddr;
        //pinfo->size = g_RecentCIInfo[5].dwSize;
        pinfo[5].size = 2;
        TXTRBUF_DETAIL_DUMP(TRACE3("Protect 0x%08X (%d,%d)", pinfo[5].addr, pinfo[5].width, pinfo[5].height));
    }
}

// Plugin spec 1.3 functions
EXPORT void CALL ShowCFB(void)
{
    status.toShowCFB = true;
}

EXPORT void CALL CaptureScreen( char * Directory )
{
    // start by getting the base file path
    char filepath[2048], filename[2048];
    filepath[0] = 0;
    filename[0] = 0;
    strcpy(filepath, Directory);
    if (strlen(filepath) > 0 && filepath[strlen(filepath)-1] != '/')
        strcat(filepath, "/");
    strcat(filepath, "mupen64");
    // look for a file
    int i;
    for (i = 0; i < 100; i++)
    {
        sprintf(filename, "%s_%03i.png", filepath, i);
        FILE *pFile = fopen(filename, "r");
        if (pFile == NULL)
            break;
        fclose(pFile);
    }
    if (i == 100) return;
    // save filename for screen saving at next refresh
    strcpy(status.screenCaptureFilename, filename);
    status.toCaptureScreen = true;
}

//void ReadScreen( void **dest, int *width, int *height )
EXPORT void CALL ReadScreen(void **dest, int *width, int *height)
{
   *width = windowSetting.uDisplayWidth;
   *height = windowSetting.uDisplayHeight;
   
   *dest = malloc( windowSetting.uDisplayHeight * windowSetting.uDisplayWidth * 3 );
   if (*dest == 0)
     return;
   
   GLint oldMode;
   glGetIntegerv( GL_READ_BUFFER, &oldMode );
   glReadBuffer( GL_FRONT );
   //      glReadBuffer( GL_BACK );
   glReadPixels( 0, 0, windowSetting.uDisplayWidth, windowSetting.uDisplayHeight,
         GL_RGB, GL_UNSIGNED_BYTE, *dest );
   glReadBuffer( oldMode );
}
    
/******************************************************************
   NOTE: THIS HAS BEEN ADDED FOR MUPEN64PLUS AND IS NOT PART OF THE
         ORIGINAL SPEC
  Function: SetConfigDir
  Purpose:  To pass the location where config files should be read/
            written to.
  input:    path to config directory
  output:   none
*******************************************************************/
EXPORT void CALL SetConfigDir(char *configDir)
{
    strncpy(g_ConfigDir, configDir, PATH_MAX);
}

/******************************************************************
   NOTE: THIS HAS BEEN ADDED FOR MUPEN64PLUS AND IS NOT PART OF THE
         ORIGINAL SPEC
  Function: SetRenderingCallback
  Purpose:  Allows emulator to register a callback function that will
            be called by the graphics plugin just before the the
            frame buffers are swapped.
            This was added as a way for the emulator to draw emulator-
            specific things to the screen, e.g. On-screen display.
  input:    pointer to a callback function.
  output:   none
*******************************************************************/
EXPORT void CALL SetRenderingCallback(void (*callback)())
{
    renderCallback = callback;
}

