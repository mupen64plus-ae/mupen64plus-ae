/*

  Copyright (C) 2003 Rice1964

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

#include "m64p_plugin.h"
#include "stdafx.h"

CGraphicsContext* CGraphicsContext::g_pGraphicsContext = NULL;
bool CGraphicsContext::m_deviceCapsIsInitialized = false;
bool CGraphicsContext::needCleanScene = false;
int CGraphicsContext::m_maxFSAA = 16;
int CGraphicsContext::m_maxAnisotropy = 16;
UINT CGraphicsContext::m_FullScreenRefreshRates[40] = { 0, 50, 55, 60, 65, 70, 72, 75, 80, 85, 90, 95, 100, 110, 120};
int CGraphicsContext::m_FullScreenResolutions[40][2] = {
    {320,200}, {400,300}, {480,360}, {512,384}, {640,480}, 
    {800,600}, {1024,768}, {1152,864}, {1280,960}, 
    {1400,1050}, {1600,1200}, {1920,1440}, {2048,1536}};
int CGraphicsContext::m_numOfResolutions = 0;
UINT CGraphicsContext::m_ColorBufferDepths[4] = {16, 32, 0, 0};

CGraphicsContext * CGraphicsContext::Get(void)
{   
    return CGraphicsContext::g_pGraphicsContext;
}
    
CGraphicsContext::CGraphicsContext() :
    m_supportTextureMirror(false),
    m_bReady(false), 
        m_bActive(false),
        m_bWindowed(true)
{
}
CGraphicsContext::~CGraphicsContext()
{
    g_pFrameBufferManager->CloseUp();
}

uint32      CGraphicsContext::m_dwWindowStyle=0;     // Saved window style for mode switches
uint32      CGraphicsContext::m_dwWindowExStyle=0;   // Saved window style for mode switches
uint32      CGraphicsContext::m_dwStatusWindowStyle=0;     // Saved window style for mode switches

void CGraphicsContext::InitWindowInfo()
{
}

bool CGraphicsContext::Initialize(HWND hWnd, HWND hWndStatus, uint32 dwWidth, uint32 dwHeight, BOOL bWindowed )
{
if(windowSetting.bDisplayFullscreen)
    {
    windowSetting.uDisplayWidth = windowSetting.uFullScreenDisplayWidth;
    windowSetting.uDisplayHeight = windowSetting.uFullScreenDisplayHeight;
    }
else
    {
     /*int maxidx = CGraphicsContext::m_numOfResolutions - 1;
    if(CGraphicsContext::m_FullScreenResolutions[maxidx][0] <=
      windowSetting.uWindowDisplayWidth ||
      CGraphicsContext::m_FullScreenResolutions[maxidx][1] <=
      windowSetting.uWindowDisplayHeight)
        {
        windowSetting.uWindowDisplayWidth = 640;
        windowSetting.uWindowDisplayHeight = 480;
        }*/

    windowSetting.uDisplayWidth = windowSetting.uWindowDisplayWidth;
    windowSetting.uDisplayHeight= windowSetting.uWindowDisplayHeight;
    }

g_pFrameBufferManager->Initialize();

return true;
}

void CGraphicsContext::CleanUp()
{
    m_bActive = false;
    m_bReady  = false;
}


int _cdecl SortFrequenciesCallback( const VOID* arg1, const VOID* arg2 )
{
    UINT* p1 = (UINT*)arg1;
    UINT* p2 = (UINT*)arg2;

    if( *p1 < *p2 )   
        return -1;
    else if( *p1 > *p2 )   
        return 1;
    else 
        return 0;
}
int _cdecl SortResolutionsCallback( const VOID* arg1, const VOID* arg2 )
{
    UINT* p1 = (UINT*)arg1;
    UINT* p2 = (UINT*)arg2;

    if( *p1 < *p2 )   
        return -1;
    else if( *p1 > *p2 )   
        return 1;
    else 
    {
        if( p1[1] < p2[1] )   
            return -1;
        else if( p1[1] > p2[1] )   
            return 1;
        else
            return 0;
    }
}

// This is a static function, will be called when the plugin DLL is initialized
void CGraphicsContext::InitDeviceParameters(void)
{
    // Initialize common device parameters

    int i=0, j;
    int numOfFrequency=0, numOfColorDepth = 0;
    CGraphicsContext::m_numOfResolutions=0;
    memset(&CGraphicsContext::m_FullScreenRefreshRates,0,40*sizeof(UINT));
    memset(&CGraphicsContext::m_FullScreenResolutions, 0, 40*2*sizeof(int));
    memset(&CGraphicsContext::m_ColorBufferDepths, 0, 4*sizeof(UINT));

   if(SDL_InitSubSystem(SDL_INIT_VIDEO) == -1)
     printf("(EE) Error initializing SDL video subsystem: %s\n", SDL_GetError());
   
   const SDL_VideoInfo *videoInfo;
   if(!(videoInfo = SDL_GetVideoInfo()))
     printf("(EE) Video query failed: %s\n", SDL_GetError());
   
   Uint32 videoFlags = SDL_OPENGL | SDL_GL_DOUBLEBUFFER | SDL_HWPALETTE | SDL_FULLSCREEN;
   
   if(videoInfo->hw_available)
     videoFlags |= SDL_HWSURFACE;
   else
     videoFlags |= SDL_SWSURFACE;
   
   if(videoInfo->blit_hw)
     videoFlags |= SDL_HWACCEL;
   
   SDL_Rect **modes;
   modes = SDL_ListModes(NULL, videoFlags);
   for(i=0; modes[i]; i++)
     {
    for (j = 0; j < CGraphicsContext::m_numOfResolutions; j++)
      {
         if ((modes[i]->w == CGraphicsContext::m_FullScreenResolutions[j][0]) &&
         (modes[i]->h == CGraphicsContext::m_FullScreenResolutions[j][1]))
           {
          break;
           }
      }
    
    if( j == CGraphicsContext::m_numOfResolutions )
      {
         CGraphicsContext::m_FullScreenResolutions[CGraphicsContext::m_numOfResolutions][0] = modes[i]->w;
         CGraphicsContext::m_FullScreenResolutions[CGraphicsContext::m_numOfResolutions][1] = modes[i]->h;
         CGraphicsContext::m_numOfResolutions++;
      }
     }
   
   CGraphicsContext::m_ColorBufferDepths[numOfColorDepth++] = 16;
   CGraphicsContext::m_ColorBufferDepths[numOfColorDepth++] = 32;
   CGraphicsContext::m_FullScreenRefreshRates[numOfFrequency++] = 60;

    qsort( &CGraphicsContext::m_FullScreenRefreshRates, numOfFrequency, sizeof(UINT), SortFrequenciesCallback );
    qsort( &CGraphicsContext::m_FullScreenResolutions, CGraphicsContext::m_numOfResolutions, sizeof(int)*2, SortResolutionsCallback );

    // To initialze device parameters for OpenGL
    COGLGraphicsContext::InitDeviceParameters();
}


