/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - stdafx.h                                                *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2002 Rice1964                                           *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

#if !defined(_STDAFX_H_)
#define _STDAFX_H_

#include <SDL/SDL.h>

#define EXPORT              __declspec(dllexport)

#include <stdio.h>
#include "winlnxdefs.h"
#include "math.h"
#include "COLOR.h"

#include <math.h>           // For sqrt()
#include <iostream>
#include <fstream>
#include <istream>

#include <vector>

#ifndef SAFE_DELETE
#define SAFE_DELETE(p)  { if(p) { delete (p);     (p)=NULL; } }
#endif

#ifndef SAFE_CHECK
# define SAFE_CHECK(a)  if( (a) == NULL ) {ErrorMsg("Creater out of memory"); throw new std::exception();}
#endif

#include "typedefs.h"
#include "Graphics_1.3.h"
#include "Video.h"
#include "Config.h"
#include "Debugger.h"
#include "RSP_S2DEX.h"
#include "RSP_Parser.h"

#include "TextureManager.h"
#include "ConvertImage.h"
#include "Texture.h"

#include "CombinerDefs.h"
#include "DecodedMux.h"
#include "DirectXDecodedMux.h"

#include "blender.h"


#include "Combiner.h"
#include "GeneralCombiner.h"

#include "RenderTexture.h"
#include "FrameBuffer.h"

#include "GraphicsContext.h"
#include "DeviceBuilder.h"

#include "RenderBase.h"
#include "ExtendedRender.h"
#include "Render.h"

#include "OGLTexture.h"
#include "OGLDecodedMux.h"
#include "CNvTNTCombiner.h"

#include "OGLCombiner.h"
#include "OGLExtCombiner.h"
#include "OGLCombinerNV.h"
#include "OGLCombinerTNT2.h"
#include "OGLFragmentShaders.h"

#include "OGLRender.h"
#include "OGLExtRender.h"
#include "OGLGraphicsContext.h"

#include "IColor.h"

#include "CSortedList.h"
#include "CritSect.h"
#include "Timing.h"


extern WindowSettingStruct windowSetting;

void __cdecl MsgInfo (char* Message, ...);
void __cdecl ErrorMsg (const char* Message, ...);

#define MI_INTR_DP          0x00000020  
#define MI_INTR_SP          0x00000001  

extern uint32 g_dwRamSize;

extern uint32 * g_pRDRAMu32;
extern signed char* g_pRDRAMs8;
extern unsigned char *g_pRDRAMu8;

extern GFX_INFO g_GraphicsInfo;

extern const char *project_name;
#endif

