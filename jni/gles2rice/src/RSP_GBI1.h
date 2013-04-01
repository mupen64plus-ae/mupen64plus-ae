/*
Copyright (C) 2002-2009 Rice1964

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

#include "Render.h"
#include "Timing.h"

void RSP_GBI1_Vtx(Gfx *gfx)
{
    uint32 addr = RSPSegmentAddr((gfx->vtx1.addr));
    uint32 v0  = gfx->vtx1.v0;
    uint32 n   = gfx->vtx1.n;

    LOG_UCODE("    Address 0x%08x, v0: %d, Num: %d, Length: 0x%04x", addr, v0, n, gfx->vtx1.len);

    if (addr > g_dwRamSize)
    {
        TRACE0("     Address out of range - ignoring load");
        return;
    }

    if ((v0 + n) > 80)
    {
        TRACE5("Warning, invalid vertex positions, N=%d, v0=%d, Addr=0x%08X, Cmd=%08X-%08X",
            n, v0, addr, gfx->words.cmd0, gfx->words.cmd1);
        return;
    }

    ProcessVertexData(addr, v0, n);
    status.dwNumVertices += n;
    DisplayVertexInfo(addr, v0, n);
}

void RSP_GBI1_ModifyVtx(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_ModifyVtx);

    if( gRSP.ucode == 5 && ((gfx->words.cmd0)&0x00FFFFFF) == 0 && ((gfx->words.cmd1)&0xFF000000) == 0x80000000 )
    {
        DLParser_Bomberman2TextRect(gfx);
    }
    else
    {
        uint32 dwWhere = ((gfx->words.cmd0) >> 16) & 0xFF;
        uint32 dwVert   = (((gfx->words.cmd0)      ) & 0xFFFF) / 2;
        uint32 dwValue  = (gfx->words.cmd1);

        if( dwVert > 80 )
        {
            RSP_RDP_NOIMPL("RSP_GBI1_ModifyVtx: Invalid vertex number: %d", dwVert, 0);
            return;
        }

        // Data for other commands?
        switch (dwWhere)
        {
        case RSP_MV_WORD_OFFSET_POINT_RGBA:         // Modify RGBA
        case RSP_MV_WORD_OFFSET_POINT_XYSCREEN:     // Modify X,Y
        case RSP_MV_WORD_OFFSET_POINT_ZSCREEN:      // Modify C
        case RSP_MV_WORD_OFFSET_POINT_ST:           // Texture
            ModifyVertexInfo(dwWhere, dwVert, dwValue);
            break;
        default:
            RSP_RDP_NOIMPL("RSP_GBI1_ModifyVtx: Setting unk value: 0x%02x, 0x%08x", dwWhere, dwValue);
            break;
        }
    }
}

void RSP_GBI1_Tri2(Gfx *gfx)
{
    status.primitiveType = PRIM_TRI2;
    bool bTrisAdded = false;
    bool bTexturesAreEnabled = CRender::g_pRender->IsTextureEnabled();

    // While the next command pair is Tri2, add vertices
    uint32 dwPC = gDlistStack[gDlistStackPointer].pc;

    do {
        // Vertex indices are multiplied by 10 for Mario64, by 2 for MarioKart
        uint32 dwV0 = gfx->gbi1tri2.v0/gRSP.vertexMult;
        uint32 dwV1 = gfx->gbi1tri2.v1/gRSP.vertexMult;
        uint32 dwV2 = gfx->gbi1tri2.v2/gRSP.vertexMult;

        uint32 dwV3 = gfx->gbi1tri2.v3/gRSP.vertexMult;
        uint32 dwV4 = gfx->gbi1tri2.v4/gRSP.vertexMult;
        uint32 dwV5 = gfx->gbi1tri2.v5/gRSP.vertexMult;

        // Do first tri
        if (IsTriangleVisible(dwV0, dwV1, dwV2))
        {
            DEBUG_DUMP_VERTEXES("Tri2 1/2", dwV0, dwV1, dwV2);
            if (!bTrisAdded)
            {
                if( bTexturesAreEnabled )
            {
                PrepareTextures();
                InitVertexTextureConstants();
            }
                CRender::g_pRender->SetCombinerAndBlender();
                bTrisAdded = true;
            }
            PrepareTriangle(dwV0, dwV1, dwV2);
        }

        // Do second tri
        if (IsTriangleVisible(dwV3, dwV4, dwV5))
        {
            DEBUG_DUMP_VERTEXES("Tri2 2/2", dwV3, dwV4, dwV5);
            if (!bTrisAdded)
            {
                if( bTexturesAreEnabled )
            {
                PrepareTextures();
                InitVertexTextureConstants();
            }
                CRender::g_pRender->SetCombinerAndBlender();
                bTrisAdded = true;
            }
            PrepareTriangle(dwV3, dwV4, dwV5);
        }
        
        gfx++;
        dwPC += 8;
#ifdef DEBUGGER
    } while (!(pauseAtNext && eventToPause==NEXT_TRIANGLE) && gfx->words.cmd == (uint8)RSP_TRI2);
#else
    } while( gfx->words.cmd == (uint8)RSP_TRI2);
#endif


    gDlistStack[gDlistStackPointer].pc = dwPC-8;


    if (bTrisAdded) 
    {
        CRender::g_pRender->DrawTriangles();
    }

    DEBUG_TRIANGLE(TRACE0("Pause at GBI1 TRI1"));
}

extern XVECTOR4 g_vtxNonTransformed[MAX_VERTS];

void RSP_GBI1_BranchZ(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_BranchZ);

    uint32 vtx = ((gfx->words.cmd0)&0xFFF)>1;
    float vtxdepth = g_vecProjected[vtx].z/g_vecProjected[vtx].w;

#ifdef DEBUGGER
    if( debuggerEnableZBuffer==FALSE || vtxdepth <= (s32)gfx->words.cmd1 || g_curRomInfo.bForceDepthBuffer )
#else
    if( vtxdepth <= (s32)(gfx->words.cmd1) || g_curRomInfo.bForceDepthBuffer )
#endif
    {
        uint32 dwPC = gDlistStack[gDlistStackPointer].pc;       // This points to the next instruction
        uint32 dwDL = *(uint32 *)(g_pRDRAMu8 + dwPC-12);
        uint32 dwAddr = RSPSegmentAddr(dwDL);

        dwAddr = RSPSegmentAddr(dwDL);;

        LOG_UCODE("BranchZ to DisplayList 0x%08x", dwAddr);
        gDlistStack[gDlistStackPointer].pc = dwAddr;
        gDlistStack[gDlistStackPointer].countdown = MAX_DL_COUNT;
    }
}

#ifdef DEBUGGER
void DumpUcodeInfo(UcodeInfo &info)
{
    DebuggerAppendMsg("Loading Unknown Ucode:\n%08X-%08X-%08X-%08X, Size=0x%X, CRC=0x%08X\nCode:\n",
        info.ucDWORD1, info.ucDWORD2, info.ucDWORD3, info.ucDWORD4, 
        info.ucSize, info.ucCRC);
    DumpHex(info.ucStart,20);
    TRACE0("Data:\n");
    DumpHex(info.ucDStart,20);
}
#endif

void RSP_GBI1_LoadUCode(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_LoadUCode);

    //TRACE0("Load ucode");
    uint32 dwPC = gDlistStack[gDlistStackPointer].pc;
    uint32 dwUcStart = RSPSegmentAddr((gfx->words.cmd1));
    uint32 dwSize = ((gfx->words.cmd0)&0xFFFF)+1;
    uint32 dwUcDStart = RSPSegmentAddr(*(uint32 *)(g_pRDRAMu8 + dwPC-12));

    uint32 ucode = DLParser_CheckUcode(dwUcStart, dwUcDStart, dwSize, 8);
    RSP_SetUcode(ucode, dwUcStart, dwUcDStart, dwSize);

    DEBUGGER_PAUSE_AND_DUMP(NEXT_SWITCH_UCODE,{DebuggerAppendMsg("Pause at loading ucode");});
}

void RSP_GFX_Force_Matrix(uint32 dwAddr)
{
    if (dwAddr + 64 > g_dwRamSize)
    {
        DebuggerAppendMsg("ForceMtx: Address invalid (0x%08x)", dwAddr);
        return;
    }

    // Load matrix from dwAddr
    LoadMatrix(dwAddr);

    CRender::g_pRender->SetWorldProjectMatrix(matToLoad);

    DEBUGGER_PAUSE_AND_DUMP(NEXT_MATRIX_CMD,{TRACE0("Paused at ModMatrix Cmd");});
}


void DisplayVertexInfo(uint32 dwAddr, uint32 dwV0, uint32 dwN)
{
#ifdef DEBUGGER
        s8 *pcSrc = (s8 *)(g_pRDRAMu8 + dwAddr);
        short *psSrc = (short *)(g_pRDRAMu8 + dwAddr);

        for (uint32 dwV = dwV0; dwV < dwV0 + dwN; dwV++)
        {
            float x = (float)psSrc[0^0x1];
            float y = (float)psSrc[1^0x1];
            float z = (float)psSrc[2^0x1];

            //uint32 wFlags = g_dwVtxFlags[dwV]; //(uint16)psSrc[3^0x1];
            uint32 wFlags = 0;

            uint8 a = pcSrc[12^0x3];
            uint8 b = pcSrc[13^0x3];
            uint8 c = pcSrc[14^0x3];
            uint8 d = pcSrc[15^0x3];
            
            //int nTU = (int)(short)(psSrc[4^0x1]<<4);
            //int nTV = (int)(short)(psSrc[5^0x1]<<4);

            //float tu = (float)(nTU>>4);
            //float tv = (float)(nTV>>4);
            float tu = (float)(short)(psSrc[4^0x1]);
            float tv = (float)(short)(psSrc[5^0x1]);

            XVECTOR4 & t = g_vecProjected[dwV];

            psSrc += 8;         // Increase by 16 bytes
            pcSrc += 16;

            LOG_UCODE(" #%02d Flags: 0x%04x Pos: {% 6f,% 6f,% 6f} Tex: {%+7.2f,%+7.2f}, Extra: %02x %02x %02x %02x (transf: {% 6f,% 6f,% 6f})",
                dwV, wFlags, x, y, z, tu, tv, a, b, c, d, t.x, t.y, t.z );
        }
#endif
}

void RSP_MoveMemLight(uint32 dwLight, uint32 dwAddr)
{
    if( dwLight >= 16 )
    {
        DebuggerAppendMsg("Warning: invalid light # = %d", dwLight);
        return;
    }

    s8 * pcBase = g_pRDRAMs8 + dwAddr;
    uint32 * pdwBase = (uint32 *)pcBase;


    float range = 0, x, y, z;
    if( options.enableHackForGames == HACK_FOR_ZELDA_MM && (pdwBase[0]&0xFF) == 0x08 && (pdwBase[1]&0xFF) == 0xFF )
    {
        gRSPn64lights[dwLight].dwRGBA       = pdwBase[0];
        gRSPn64lights[dwLight].dwRGBACopy   = pdwBase[1];
        short* pdwBase16 = (short*)pcBase;
        x       = pdwBase16[5];
        y       = pdwBase16[4];
        z       = pdwBase16[7];
        range   = pdwBase16[6];
    }
    else
    {
        gRSPn64lights[dwLight].dwRGBA       = pdwBase[0];
        gRSPn64lights[dwLight].dwRGBACopy   = pdwBase[1];
        x       = pcBase[8 ^ 0x3];
        y       = pcBase[9 ^ 0x3];
        z       = pcBase[10 ^ 0x3];
    }

                    
    LOG_UCODE("       RGBA: 0x%08x, RGBACopy: 0x%08x, x: %d, y: %d, z: %d", 
        gRSPn64lights[dwLight].dwRGBA,
        gRSPn64lights[dwLight].dwRGBACopy,
        x, y, z);

    LIGHT_DUMP(TRACE3("Move Light: %08X, %08X, %08X", pdwBase[0], pdwBase[1], pdwBase[2]));


    if (dwLight == gRSP.ambientLightIndex)
    {
        LOG_UCODE("      (Ambient Light)");

        uint32 dwCol = COLOR_RGBA( (gRSPn64lights[dwLight].dwRGBA >> 24)&0xFF,
                      (gRSPn64lights[dwLight].dwRGBA >> 16)&0xFF,
                      (gRSPn64lights[dwLight].dwRGBA >>  8)&0xFF, 0xff);

        SetAmbientLight( dwCol );
    }
    else
    {
        
        LOG_UCODE("      (Normal Light)");

        SetLightCol(dwLight, gRSPn64lights[dwLight].dwRGBA);
        if (pdwBase[2] == 0)    // Direction is 0!
        {
            LOG_UCODE("      Light is invalid");
        }
        SetLightDirection(dwLight, x, y, z, range);
    }
}

void RSP_MoveMemViewport(uint32 dwAddr)
{
    if( dwAddr+16 >= g_dwRamSize )
    {
        TRACE0("MoveMem Viewport, invalid memory");
        return;
    }

    short scale[4];
    short trans[4];

    // dwAddr is offset into RD_RAM of 8 x 16bits of data...
    scale[0] = *(short *)(g_pRDRAMu8 + ((dwAddr+(0*2))^0x2));
    scale[1] = *(short *)(g_pRDRAMu8 + ((dwAddr+(1*2))^0x2));
    scale[2] = *(short *)(g_pRDRAMu8 + ((dwAddr+(2*2))^0x2));
    scale[3] = *(short *)(g_pRDRAMu8 + ((dwAddr+(3*2))^0x2));

    trans[0] = *(short *)(g_pRDRAMu8 + ((dwAddr+(4*2))^0x2));
    trans[1] = *(short *)(g_pRDRAMu8 + ((dwAddr+(5*2))^0x2));
    trans[2] = *(short *)(g_pRDRAMu8 + ((dwAddr+(6*2))^0x2));
    trans[3] = *(short *)(g_pRDRAMu8 + ((dwAddr+(7*2))^0x2));


    int nCenterX = trans[0]/4;
    int nCenterY = trans[1]/4;
    int nWidth   = scale[0]/4;
    int nHeight  = scale[1]/4;

    // Check for some strange games
    if( nWidth < 0 )    nWidth = -nWidth;
    if( nHeight < 0 )   nHeight = -nHeight;

    int nLeft = nCenterX - nWidth;
    int nTop  = nCenterY - nHeight;
    int nRight= nCenterX + nWidth;
    int nBottom= nCenterY + nHeight;

    //int maxZ = scale[2];
    int maxZ = 0x3FF;

    CRender::g_pRender->SetViewport(nLeft, nTop, nRight, nBottom, maxZ);


    LOG_UCODE("        Scale: %d %d %d %d = %d,%d", scale[0], scale[1], scale[2], scale[3], nWidth, nHeight);
    LOG_UCODE("        Trans: %d %d %d %d = %d,%d", trans[0], trans[1], trans[2], trans[3], nCenterX, nCenterY);
}


// S2DEX uses this - 0xc1
void RSP_S2DEX_SPObjLoadTxtr_Ucode1(Gfx *gfx)
{
    SP_Timing(RSP_S2DEX_SPObjLoadTxtr_Ucode1);

    // Add S2DEX ucode supporting to F3DEX, see game DT and others
    status.bUseModifiedUcodeMap = true;
    RSP_SetUcode(1);
    memcpy( &LoadedUcodeMap, &ucodeMap1, sizeof(UcodeMap));
    
    LoadedUcodeMap[S2DEX_OBJ_MOVEMEM] = &RSP_S2DEX_OBJ_MOVEMEM;
    LoadedUcodeMap[S2DEX_OBJ_LOADTXTR] = &RSP_S2DEX_SPObjLoadTxtr;
    LoadedUcodeMap[S2DEX_OBJ_LDTX_SPRITE] = &RSP_S2DEX_SPObjLoadTxSprite;
    LoadedUcodeMap[S2DEX_OBJ_LDTX_RECT] = &RSP_S2DEX_SPObjLoadTxRect;
    LoadedUcodeMap[S2DEX_OBJ_LDTX_RECT_R] = &RSP_S2DEX_SPObjLoadTxRectR;

    RSP_S2DEX_SPObjLoadTxtr(gfx);
}

void RSP_GBI1_SpNoop(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_SpNoop);

    if( (gfx+1)->words.cmd == 0x00 && gRSP.ucode >= 17 )
    {
        RSP_RDP_NOIMPL("Double SPNOOP, Skip remain ucodes, PC=%08X, Cmd1=%08X", gDlistStack[gDlistStackPointer].pc, gfx->words.cmd1);
        RDP_GFX_PopDL();
        //if( gRSP.ucode < 17 ) TriggerDPInterrupt();
    }
}

void RSP_GBI1_Reserved(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_Reserved);
    RSP_RDP_NOIMPL("RDP: Reserved (0x%08x 0x%08x)", (gfx->words.cmd0), (gfx->words.cmd1));
}

void RSP_GBI1_MoveMem(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_MoveMem);

    uint32 type    = ((gfx->words.cmd0)>>16)&0xFF;
    uint32 dwLength  = ((gfx->words.cmd0))&0xFFFF;
    uint32 addr = RSPSegmentAddr((gfx->words.cmd1));

    switch (type)
    {
        case RSP_GBI1_MV_MEM_VIEWPORT:
            {
                LOG_UCODE("    RSP_GBI1_MV_MEM_VIEWPORT. Address: 0x%08x, Length: 0x%04x", addr, dwLength);
                RSP_MoveMemViewport(addr);
            }
            break;
        case RSP_GBI1_MV_MEM_LOOKATY:
            LOG_UCODE("    RSP_GBI1_MV_MEM_LOOKATY");
            break;
        case RSP_GBI1_MV_MEM_LOOKATX:
            LOG_UCODE("    RSP_GBI1_MV_MEM_LOOKATX");
            break;
        case RSP_GBI1_MV_MEM_L0:
        case RSP_GBI1_MV_MEM_L1:
        case RSP_GBI1_MV_MEM_L2:
        case RSP_GBI1_MV_MEM_L3:
        case RSP_GBI1_MV_MEM_L4:
        case RSP_GBI1_MV_MEM_L5:
        case RSP_GBI1_MV_MEM_L6:
        case RSP_GBI1_MV_MEM_L7:
            {
                uint32 dwLight = (type-RSP_GBI1_MV_MEM_L0)/2;
                LOG_UCODE("    RSP_GBI1_MV_MEM_L%d", dwLight);
                LOG_UCODE("    Light%d: Length:0x%04x, Address: 0x%08x", dwLight, dwLength, addr);

                RSP_MoveMemLight(dwLight, addr);
            }
            break;
        case RSP_GBI1_MV_MEM_TXTATT:
            LOG_UCODE("    RSP_GBI1_MV_MEM_TXTATT");
            break;
        case RSP_GBI1_MV_MEM_MATRIX_1:
            RSP_GFX_Force_Matrix(addr);
            break;
        case RSP_GBI1_MV_MEM_MATRIX_2:
            break;
        case RSP_GBI1_MV_MEM_MATRIX_3:
            break;
        case RSP_GBI1_MV_MEM_MATRIX_4:
            break;
        default:
            RSP_RDP_NOIMPL("MoveMem: Unknown Move Type, cmd=%08X, %08X", gfx->words.cmd0, gfx->words.cmd1);
            break;
    }
}

void RSP_GBI1_RDPHalf_Cont(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_RDPHalf_Cont);

    LOG_UCODE("RDPHalf_Cont: (Ignored)");
}
void RSP_GBI1_RDPHalf_2(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_RDPHalf_2);

    LOG_UCODE("RDPHalf_2: (Ignored)");
}

void RSP_GBI1_RDPHalf_1(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_RDPHalf_1);

    LOG_UCODE("RDPHalf_1: (Ignored)");
}

void RSP_GBI1_Line3D(Gfx *gfx)
{
    status.primitiveType = PRIM_LINE3D;

    uint32 dwPC = gDlistStack[gDlistStackPointer].pc;

    BOOL bTrisAdded = FALSE;

    if( gfx->gbi1line3d.v3 == 0 )
    {
        // Flying Dragon
        uint32 dwV0     = gfx->gbi1line3d.v0/gRSP.vertexMult;
        uint32 dwV1     = gfx->gbi1line3d.v1/gRSP.vertexMult;
        uint32 dwWidth  = gfx->gbi1line3d.v2;
        uint32 dwFlag   = gfx->gbi1line3d.v3/gRSP.vertexMult;

        CRender::g_pRender->SetCombinerAndBlender();

        status.dwNumTrisRendered++;

        CRender::g_pRender->Line3D(dwV0, dwV1, dwWidth);
        SP_Timing(RSP_GBI1_Line3D);
        DP_Timing(RSP_GBI1_Line3D);
    }
    else
    {
        do {
            uint32 dwV3  = gfx->gbi1line3d.v3/gRSP.vertexMult;
            uint32 dwV0  = gfx->gbi1line3d.v0/gRSP.vertexMult;
            uint32 dwV1  = gfx->gbi1line3d.v1/gRSP.vertexMult;
            uint32 dwV2  = gfx->gbi1line3d.v2/gRSP.vertexMult;

            LOG_UCODE("    Line3D: V0: %d, V1: %d, V2: %d, V3: %d", dwV0, dwV1, dwV2, dwV3);

            // Do first tri
            if (IsTriangleVisible(dwV0, dwV1, dwV2))
            {
                DEBUG_DUMP_VERTEXES("Line3D 1/2", dwV0, dwV1, dwV2);
                if (!bTrisAdded && CRender::g_pRender->IsTextureEnabled())
                {
                    PrepareTextures();
                    InitVertexTextureConstants();
                }

                if( !bTrisAdded )
                {
                    CRender::g_pRender->SetCombinerAndBlender();
                }

                bTrisAdded = true;
                PrepareTriangle(dwV0, dwV1, dwV2);
            }

            // Do second tri
            if (IsTriangleVisible(dwV2, dwV3, dwV0))
            {
                DEBUG_DUMP_VERTEXES("Line3D 2/2", dwV0, dwV1, dwV2);
                if (!bTrisAdded && CRender::g_pRender->IsTextureEnabled())
                {
                    PrepareTextures();
                    InitVertexTextureConstants();
                }

                if( !bTrisAdded )
                {
                    CRender::g_pRender->SetCombinerAndBlender();
                }

                bTrisAdded = true;
                PrepareTriangle(dwV2, dwV3, dwV0);
            }

            gfx++;
            dwPC += 8;
#ifdef DEBUGGER
        } while (gfx->words.cmd == (uint8)RSP_LINE3D && !(pauseAtNext && eventToPause==NEXT_FLUSH_TRI));
#else
        } while (gfx->words.cmd == (uint8)RSP_LINE3D);
#endif

        gDlistStack[gDlistStackPointer].pc = dwPC-8;

        if (bTrisAdded)
        {
            CRender::g_pRender->DrawTriangles();
        }
    }
}


void RSP_GBI1_ClearGeometryMode(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_ClearGeometryMode);
    uint32 dwMask = ((gfx->words.cmd1));

#ifdef DEBUGGER
    LOG_UCODE("    Mask=0x%08x", dwMask);
    if (dwMask & G_ZBUFFER)                     LOG_UCODE("  Disabling ZBuffer");
    if (dwMask & G_TEXTURE_ENABLE)              LOG_UCODE("  Disabling Texture");
    if (dwMask & G_SHADE)                       LOG_UCODE("  Disabling Shade");
    if (dwMask & G_SHADING_SMOOTH)              LOG_UCODE("  Disabling Smooth Shading");
    if (dwMask & G_CULL_FRONT)                  LOG_UCODE("  Disabling Front Culling");
    if (dwMask & G_CULL_BACK)                   LOG_UCODE("  Disabling Back Culling");
    if (dwMask & G_FOG)                         LOG_UCODE("  Disabling Fog");
    if (dwMask & G_LIGHTING)                    LOG_UCODE("  Disabling Lighting");
    if (dwMask & G_TEXTURE_GEN)                 LOG_UCODE("  Disabling Texture Gen");
    if (dwMask & G_TEXTURE_GEN_LINEAR)          LOG_UCODE("  Disabling Texture Gen Linear");
    if (dwMask & G_LOD)                         LOG_UCODE("  Disabling LOD (no impl)");
#endif

    gRDP.geometryMode &= ~dwMask;
    RSP_GFX_InitGeometryMode();
}

void RSP_GBI1_SetGeometryMode(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_SetGeometryMode);
    uint32 dwMask = ((gfx->words.cmd1));

#ifdef DEBUGGER
    LOG_UCODE("    Mask=0x%08x", dwMask);
    if (dwMask & G_ZBUFFER)                     LOG_UCODE("  Enabling ZBuffer");
    if (dwMask & G_TEXTURE_ENABLE)              LOG_UCODE("  Enabling Texture");
    if (dwMask & G_SHADE)                       LOG_UCODE("  Enabling Shade");
    if (dwMask & G_SHADING_SMOOTH)              LOG_UCODE("  Enabling Smooth Shading");
    if (dwMask & G_CULL_FRONT)                  LOG_UCODE("  Enabling Front Culling");
    if (dwMask & G_CULL_BACK)                   LOG_UCODE("  Enabling Back Culling");
    if (dwMask & G_FOG)                         LOG_UCODE("  Enabling Fog");
    if (dwMask & G_LIGHTING)                    LOG_UCODE("  Enabling Lighting");
    if (dwMask & G_TEXTURE_GEN)                 LOG_UCODE("  Enabling Texture Gen");
    if (dwMask & G_TEXTURE_GEN_LINEAR)          LOG_UCODE("  Enabling Texture Gen Linear");
    if (dwMask & G_LOD)                         LOG_UCODE("  Enabling LOD (no impl)");
#endif // DEBUGGER
    gRDP.geometryMode |= dwMask;
    RSP_GFX_InitGeometryMode();
}

void RSP_GBI1_EndDL(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_EndDL);
    RDP_GFX_PopDL();
}

static const char * sc_szBlClr[4] = { "In", "Mem", "Bl", "Fog" };
static const char * sc_szBlA1[4] = { "AIn", "AFog", "AShade", "0" };
static const char * sc_szBlA2[4] = { "1-A", "AMem", "1", "?" };

void RSP_GBI1_SetOtherModeL(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_SetOtherModeL);

    uint32 dwShift = ((gfx->words.cmd0)>>8)&0xFF;
    uint32 dwLength= ((gfx->words.cmd0)   )&0xFF;
    uint32 dwData  = (gfx->words.cmd1);

    uint32 dwMask = ((1<<dwLength)-1)<<dwShift;

    uint32 modeL = gRDP.otherModeL;
    modeL = (modeL&(~dwMask)) | dwData;

    Gfx tempgfx;
    tempgfx.words.cmd0 = gRDP.otherModeH;
    tempgfx.words.cmd1 = modeL;
    DLParser_RDPSetOtherMode(&tempgfx);
}


void RSP_GBI1_SetOtherModeH(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_SetOtherModeH);

    uint32 dwShift = ((gfx->words.cmd0)>>8)&0xFF;
    uint32 dwLength= ((gfx->words.cmd0)   )&0xFF;
    uint32 dwData  = (gfx->words.cmd1);

    uint32 dwMask = ((1<<dwLength)-1)<<dwShift;
    uint32 dwModeH = gRDP.otherModeH;

    dwModeH = (dwModeH&(~dwMask)) | dwData;
    Gfx tempgfx;
    tempgfx.words.cmd0 = dwModeH;
    tempgfx.words.cmd1 = gRDP.otherModeL;
    DLParser_RDPSetOtherMode(&tempgfx );
}


void RSP_GBI1_Texture(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_Texture);

    bool bEnable = gfx->texture.enable_gbi0;

    CRender::g_pRender->SetTextureEnable( bEnable );

    //Since the texture isnt enabled, lets stop it from calculating texture scales
    if(!bEnable) return;

    float fTextureScaleS = (float)(gfx->texture.scaleS) / (65536.0f * 32.0f);
    float fTextureScaleT = (float)(gfx->texture.scaleT) / (65536.0f * 32.0f);

    if( (((gfx->words.cmd1)>>16)&0xFFFF) == 0xFFFF )
    {
        fTextureScaleS = 1/32.0f;
    }
    else if( (((gfx->words.cmd1)>>16)&0xFFFF) == 0x8000 )
    {
        fTextureScaleS = 1/64.0f;
    }
#ifdef DEBUGGER
    else if( ((gfx->words.cmd1>>16)&0xFFFF) != 0 )
    {
        //DebuggerAppendMsg("Warning, texture scale = %08X is not integer", (word1>>16)&0xFFFF);
    }
#endif

    if( (((gfx->words.cmd1)    )&0xFFFF) == 0xFFFF )
    {
        fTextureScaleT = 1/32.0f;
    }
    else if( (((gfx->words.cmd1)    )&0xFFFF) == 0x8000 )
    {
        fTextureScaleT = 1/64.0f;
    }
#ifdef DEBUGGER
    else if( (gfx->words.cmd1&0xFFFF) != 0 )
    {
        //DebuggerAppendMsg("Warning, texture scale = %08X is not integer", (word1)&0xFFFF);
    }
#endif

    if( gRSP.ucode == 6 )
    {
        if( fTextureScaleS == 0 )   fTextureScaleS = 1.0f/32.0f;
        if( fTextureScaleT == 0 )   fTextureScaleT = 1.0f/32.0f;
    }

    CRender::g_pRender->SetTextureScale(gfx->texture.tile, fTextureScaleS, fTextureScaleT);

    // What happens if these are 0? Interpret as 1.0f?

    LOG_TEXTURE(
    {
        DebuggerAppendMsg("SetTexture: Level: %d Tile: %d %s\n", gfx->texture.level, gfx->texture.tile, gfx->texture.enable_gbi0 ? "enabled":"disabled");
        DebuggerAppendMsg("            ScaleS: %f, ScaleT: %f\n", fTextureScaleS*32.0f, fTextureScaleT*32.0f);
    });

    DEBUGGER_PAUSE_COUNT_N(NEXT_SET_TEXTURE);
    LOG_UCODE("    Level: %d Tile: %d %s", gfx->texture.level, gfx->texture.tile, gfx->texture.enable_gbi0 ? "enabled":"disabled");
    LOG_UCODE("    ScaleS: %f, ScaleT: %f", fTextureScaleS*32.0f, fTextureScaleT*32.0f);
}

extern void RSP_RDP_InsertMatrix(uint32 word0, uint32 word1);
void RSP_GBI1_MoveWord(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_MoveWord);

    switch (gfx->mw1.type)
    {
    case RSP_MOVE_WORD_MATRIX:
        RSP_RDP_InsertMatrix(gfx);
        break;
    case RSP_MOVE_WORD_NUMLIGHT:
        {
            uint32 dwNumLights = (((gfx->mw1.value)-0x80000000)/32)-1;
            LOG_UCODE("    RSP_MOVE_WORD_NUMLIGHT: Val:%d", dwNumLights);

            gRSP.ambientLightIndex = dwNumLights;
            SetNumLights(dwNumLights);
        }
        break;
    case RSP_MOVE_WORD_CLIP:
        {
            switch (gfx->mw1.offset)
            {
            case RSP_MV_WORD_OFFSET_CLIP_RNX:
            case RSP_MV_WORD_OFFSET_CLIP_RNY:
            case RSP_MV_WORD_OFFSET_CLIP_RPX:
            case RSP_MV_WORD_OFFSET_CLIP_RPY:
                CRender::g_pRender->SetClipRatio(gfx->mw1.offset, gfx->mw1.value);
                break;
            default:
                LOG_UCODE("    RSP_MOVE_WORD_CLIP  ?   : 0x%08x", gfx->words.cmd1);
                break;
            }
        }
        break;
    case RSP_MOVE_WORD_SEGMENT:
        {
            uint32 dwSegment = (gfx->mw1.offset >> 2) & 0xF;
            uint32 dwBase = (gfx->mw1.value)&0x00FFFFFF;
            LOG_UCODE("    RSP_MOVE_WORD_SEGMENT Seg[%d] = 0x%08x", dwSegment, dwBase);
            if( dwBase > g_dwRamSize )
            {
                gRSP.segments[dwSegment] = dwBase;
#ifdef DEBUGGER
                if( pauseAtNext )
                    DebuggerAppendMsg("warning: Segment %d addr is %8X", dwSegment, dwBase);
#endif
            }
            else
            {
                gRSP.segments[dwSegment] = dwBase;
            }
        }
        break;
    case RSP_MOVE_WORD_FOG:
        {
            uint16 wMult = (uint16)(((gfx->mw1.value) >> 16) & 0xFFFF);
            uint16 wOff  = (uint16)(((gfx->mw1.value)      ) & 0xFFFF);

            float fMult = (float)(short)wMult;
            float fOff = (float)(short)wOff;

            float rng = 128000.0f / fMult;
            float fMin = 500.0f - (fOff*rng/256.0f);
            float fMax = rng + fMin;

            FOG_DUMP(TRACE4("Set Fog: Min=%f, Max=%f, Mul=%f, Off=%f", fMin, fMax, fMult, fOff));
            //if( fMult <= 0 || fMin > fMax || fMax < 0 || fMin > 1000 )
            if( fMult <= 0 || fMax < 0 )
            {
                // Hack
                fMin = 996;
                fMax = 1000;
                fMult = 0;
                fOff = 1;
            }

            LOG_UCODE("    RSP_MOVE_WORD_FOG/Mul=%d: Off=%d", wMult, wOff);
            FOG_DUMP(TRACE3("Set Fog: Min=%f, Max=%f, Data=%08X", fMin, fMax, gfx->mw1.value));
            SetFogMinMax(fMin, fMax, fMult, fOff);
        }
        break;
    case RSP_MOVE_WORD_LIGHTCOL:
        {
            uint32 dwLight = gfx->mw1.offset / 0x20;
            uint32 dwField = (gfx->mw1.offset & 0x7);

            LOG_UCODE("    RSP_MOVE_WORD_LIGHTCOL/0x%08x: 0x%08x", gfx->mw1.offset, gfx->words.cmd1);

            switch (dwField)
            {
            case 0:
                if (dwLight == gRSP.ambientLightIndex)
                {
                    SetAmbientLight( ((gfx->mw1.value)>>8) );
                }
                else
                {
                    SetLightCol(dwLight, gfx->mw1.value);
                }
                break;

            case 4:
                break;

            default:
                TRACE1("RSP_MOVE_WORD_LIGHTCOL with unknown offset 0x%08x", dwField);
                break;
            }
        }
        break;
    case RSP_MOVE_WORD_POINTS:
        {
            uint32 vtx = gfx->mw1.offset/40;
            uint32 where = gfx->mw1.offset - vtx*40;
            ModifyVertexInfo(where, vtx, gfx->mw1.value);
        }
        break;
    case RSP_MOVE_WORD_PERSPNORM:
        LOG_UCODE("    RSP_MOVE_WORD_PERSPNORM");
        //if( word1 != 0x1A ) DebuggerAppendMsg("PerspNorm: 0x%04x", (short)word1);
        break;
    default:
        RSP_RDP_NOIMPL("Unknown MoveWord, %08X, %08X", gfx->words.cmd0, gfx->words.cmd1);
        break;
    }

}

void RSP_GBI1_PopMtx(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_PopMtx);

    LOG_UCODE("    Command: (%s)",  gfx->popmtx.projection ? "Projection" : "ModelView");

    // Do any of the other bits do anything?
    // So far only Extreme-G seems to Push/Pop projection matrices

    if (gfx->popmtx.projection)
    {
        CRender::g_pRender->PopProjection();
    }
    else
    {
        CRender::g_pRender->PopWorldView();
    }
#ifdef DEBUGGER
    if( pauseAtNext && eventToPause == NEXT_MATRIX_CMD )
    {
        pauseAtNext = false;
        debuggerPause = true;
        DebuggerAppendMsg("Pause after Pop Matrix: %s\n", gfx->popmtx.projection ? "Proj":"World");
    }
    else
    {
        if( pauseAtNext && logMatrix )
        {
            DebuggerAppendMsg("Pause after Pop Matrix: %s\n", gfx->popmtx.projection ? "Proj":"World");
        }
    }
#endif
}



void RSP_GBI1_CullDL(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_CullDL);

#ifdef DEBUGGER
    if( !debuggerEnableCullFace )
    {
        return; //Disable Culling
    }
#endif
    if( g_curRomInfo.bDisableCulling )
    {
        return; //Disable Culling
    }

    uint32 i;
    uint32 dwVFirst = ((gfx->words.cmd0) & 0xFFF) / gRSP.vertexMult;
    uint32 dwVLast  = (((gfx->words.cmd1)) & 0xFFF) / gRSP.vertexMult;

    LOG_UCODE("    Culling using verts %d to %d", dwVFirst, dwVLast);

    // Mask into range
    dwVFirst &= 0x1f;
    dwVLast &= 0x1f;

    if( dwVLast < dwVFirst )    return;
    if( !gRSP.bRejectVtx )  return;

    for (i = dwVFirst; i <= dwVLast; i++)
    {
        if (g_clipFlag[i] == 0)
        {
            LOG_UCODE("    Vertex %d is visible, continuing with display list processing", i);
            return;
        }
    }

    status.dwNumDListsCulled++;

    LOG_UCODE("    No vertices were visible, culling rest of display list");

    RDP_GFX_PopDL();
}



void RSP_GBI1_Tri1(Gfx *gfx)
{
    status.primitiveType = PRIM_TRI1;
    bool bTrisAdded = false;
    bool bTexturesAreEnabled = CRender::g_pRender->IsTextureEnabled();

    // While the next command pair is Tri1, add vertices
    uint32 dwPC = gDlistStack[gDlistStackPointer].pc;
    uint32 * pCmdBase = (uint32 *)(g_pRDRAMu8 + dwPC);

    do
    {
        uint32 dwV0 = gfx->gbi1tri1.v0/gRSP.vertexMult;
        uint32 dwV1 = gfx->gbi1tri1.v1/gRSP.vertexMult;
        uint32 dwV2 = gfx->gbi1tri1.v2/gRSP.vertexMult;

        if (IsTriangleVisible(dwV0, dwV1, dwV2))
        {
            DEBUG_DUMP_VERTEXES("Tri1", dwV0, dwV1, dwV2);
            LOG_UCODE("    Tri1: 0x%08x 0x%08x %d,%d,%d", gfx->words.cmd0, gfx->words.cmd1, dwV0, dwV1, dwV2);

            if (!bTrisAdded)
            {
                if( bTexturesAreEnabled )
                {
                    PrepareTextures();
                    InitVertexTextureConstants();
                }
                CRender::g_pRender->SetCombinerAndBlender();
                bTrisAdded = true;
            }
            PrepareTriangle(dwV0, dwV1, dwV2);
        }

        gfx++;
        dwPC += 8;

#ifdef DEBUGGER
    } while (!(pauseAtNext && eventToPause==NEXT_TRIANGLE) && gfx->words.cmd == (uint8)RSP_TRI1);
#else
    } while (gfx->words.cmd == (uint8)RSP_TRI1);
#endif

    gDlistStack[gDlistStackPointer].pc = dwPC-8;

    if (bTrisAdded)
    {
        CRender::g_pRender->DrawTriangles();
    }

    DEBUG_TRIANGLE(TRACE0("Pause at GBI0 TRI1"));
}

