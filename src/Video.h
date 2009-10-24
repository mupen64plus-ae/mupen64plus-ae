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

#ifndef _DLLINTERFACE_H_
#define _DLLINTERFACE_H_

#include "typedefs.h"
#include <limits.h> // PATH_MAX

typedef struct {
    float   fViWidth, fViHeight;
    unsigned __int16        uViWidth, uViHeight;
    unsigned __int16        uDisplayWidth, uDisplayHeight;
    unsigned __int16        uFullScreenDisplayWidth, uFullScreenDisplayHeight;
    unsigned __int16        uWindowDisplayWidth, uWindowDisplayHeight;
    
    BOOL    bDisplayFullscreen;
    int     uFullScreenRefreshRate;

    float   fMultX, fMultY;
    int     vpLeftW, vpTopW, vpRightW, vpBottomW, vpWidthW, vpHeightW;

    int     statusBarHeight, statusBarHeightToUse, toolbarHeight, toolbarHeightToUse;
    BOOL    screenSaverStatus;

    struct {
        uint32      left;
        uint32      top;
        uint32      right;
        uint32      bottom;
        uint32      width;
        uint32      height;
        bool        needToClip;
    } clipping;

    int     timer;
    float   fps;    // frame per second
    float   dps;    // dlist per second
    uint32  lastSecFrameCount;
    uint32  lastSecDlistCount;
}WindowSettingStruct;

extern WindowSettingStruct windowSetting;

typedef enum 
{
    PRIM_TRI1,
    PRIM_TRI2,
    PRIM_TRI3,
    PRIM_DMA_TRI,
    PRIM_LINE3D,
    PRIM_TEXTRECT,
    PRIM_TEXTRECTFLIP,
    PRIM_FILLRECT,
} PrimitiveType;

typedef enum 
{
    RSP_SCISSOR,
    RDP_SCISSOR,
    UNKNOWN_SCISSOR,
} CurScissorType;

typedef struct {
    bool    bGameIsRunning;
    uint32  dwTvSystem;
    float   fRatio;

    BOOL    frameReadByCPU;
    BOOL    frameWriteByCPU;

    uint32  SPCycleCount;       // Count how many CPU cycles SP used in this DLIST
    uint32  DPCycleCount;       // Count how many CPU cycles DP used in this DLIST

    uint32  dwNumTrisRendered;
    uint32  dwNumDListsCulled;
    uint32  dwNumTrisClipped;
    uint32  dwNumVertices;
    uint32  dwBiggestVertexIndex;

    uint32  gDlistCount;
    uint32  gFrameCount;
    uint32  gUcodeCount;
    uint32  gRDPTime;
    BOOL    ToToggleFullScreen;
    bool    bDisableFPS;

    bool    bUseModifiedUcodeMap;
    bool    ucodeHasBeenSet;
    bool    bUcodeIsKnown;

    uint32  curRenderBuffer;
    uint32  curDisplayBuffer;
    uint32  curVIOriginReg;
    CurScissorType  curScissor;

    PrimitiveType primitiveType;

    uint32  lastPurgeTimeTime;      // Time textures were last purged

    bool    UseLargerTile[2];       // This is a speed up for large tile loading,
    uint32  LargerTileRealLeft[2];  // works only for TexRect, LoadTile, large width, large pitch

    bool    bVIOriginIsUpdated;
    bool    bCIBufferIsRendered;
    int     leftRendered,topRendered,rightRendered,bottomRendered;

    bool    isMMXSupported;
    bool    isSSESupported;
    bool    isVertexShaderSupported;

    bool    isMMXEnabled;
    bool    isSSEEnabled;
    bool    isVertexShaderEnabled;
    bool    bUseHW_T_L;                 // Use hardware T&L, for debug purpose only

    bool    toShowCFB;
    bool    toCaptureScreen;
    char    screenCaptureFilename[MAX_PATH];

    char    CPUCoreMsgToDisplay[256];
    bool    CPUCoreMsgIsSet;

    bool    bAllowLoadFromTMEM;

    // Frame buffer simulation related status variables
    bool    bN64FrameBufferIsUsed;      // Frame buffer is used in the frame
    bool    bN64IsDrawingTextureBuffer; // The current N64 game is rendering into render_texture, to create self-rendering texture
    bool    bHandleN64RenderTexture;    // Do we need to handle of the N64 render_texture stuff?
    bool    bDirectWriteIntoRDRAM;      // When drawing into render_texture, this value =
                                        // = true   don't render, but write real N64 graphic value into RDRAM
                                        // = false  rendering into render_texture of DX or OGL, the render_texture
                                        //          will be copied into RDRAM at the end
    bool    bFrameBufferIsDrawn;        // flag to mark if the frame buffer is ever drawn
    bool    bFrameBufferDrawnByTriangles;   // flag to tell if the buffer is even drawn by Triangle cmds

    bool    bScreenIsDrawn;

} PluginStatus;

extern PluginStatus status;
extern char generalText[];
extern void (*renderCallback)();

void SetVIScales();
extern void _VIDEO_DisplayTemporaryMessage2(const char *msg, ...);
extern void _VIDEO_DisplayTemporaryMessage(const char *msg);
extern void XBOX_Debugger_Log(const char *Message, ...);

#endif

