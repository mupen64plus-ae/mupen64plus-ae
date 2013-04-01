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

#include <cmath>
#include "Render.h"
#include "Timing.h"


void RSP_GBI0_Mtx(Gfx *gfx)
{   
    SP_Timing(RSP_GBI0_Mtx);

    uint32 addr = RSPSegmentAddr((gfx->mtx1.addr));

    LOG_UCODE("    Command: %s %s %s Length %d Address 0x%08x",
        gfx->mtx1.projection == 1 ? "Projection" : "ModelView",
        gfx->mtx1.load == 1 ? "Load" : "Mul",
        gfx->mtx1.push == 1 ? "Push" : "NoPush",
        gfx->mtx1.len, addr);

    if (addr + 64 > g_dwRamSize)
    {
        TRACE1("Mtx: Address invalid (0x%08x)", addr);
        return;
    }

    LoadMatrix(addr);
    
    if (gfx->mtx1.projection)
    {
        CRender::g_pRender->SetProjection(matToLoad, gfx->mtx1.push, gfx->mtx1.load);
    }
    else
    {
        CRender::g_pRender->SetWorldView(matToLoad, gfx->mtx1.push, gfx->mtx1.load);
    }

#ifdef DEBUGGER
    const char *loadstr = gfx->mtx1.load ? "Load" : "Mul";
    const char *pushstr = gfx->mtx1.push ? "Push" : "Nopush";
    int projlevel = CRender::g_pRender->GetProjectMatrixLevel();
    int worldlevel = CRender::g_pRender->GetWorldViewMatrixLevel();
    if( pauseAtNext && eventToPause == NEXT_MATRIX_CMD )
    {
        pauseAtNext = false;
        debuggerPause = true;
        if (gfx->mtx1.projection)
        {
            TRACE3("Pause after %s and %s Matrix: Projection, level=%d\n", loadstr, pushstr, projlevel );
        }
        else
        {
            TRACE3("Pause after %s and %s Matrix: WorldView level=%d\n", loadstr, pushstr, worldlevel);
        }
    }
    else
    {
        if( pauseAtNext && logMatrix ) 
        {
            if (gfx->mtx1.projection)
            {
                TRACE3("Matrix: %s and %s Projection level=%d\n", loadstr, pushstr, projlevel);
            }
            else
            {
                TRACE3("Matrix: %s and %s WorldView\n level=%d", loadstr, pushstr, worldlevel);
            }
        }
    }
#endif
}

void RSP_GBI0_Vtx(Gfx *gfx)
{
    SP_Timing(RSP_GBI0_Vtx);

    uint32 addr = RSPSegmentAddr((gfx->vtx0.addr));
    int v0 = gfx->vtx0.v0;
    int n = gfx->vtx0.n + 1;

    LOG_UCODE("    Address 0x%08x, v0: %d, Num: %d, Length: 0x%04x", addr, v0, n, gfx->vtx0.len);

    if ((v0 + n) > 80)
    {
        TRACE3("Warning, invalid vertex positions, N=%d, v0=%d, Addr=0x%08X", n, v0, addr);
        n = 32 - v0;
    }

    // Check that address is valid...
    if ((addr + n*16) > g_dwRamSize)
    {
        TRACE1("Vertex Data: Address out of range (0x%08x)", addr);
    }
    else
    {
        ProcessVertexData(addr, v0, n);
        status.dwNumVertices += n;
        DisplayVertexInfo(addr, v0, n);
    }
}

void RSP_GBI0_DL(Gfx *gfx)
{   
    SP_Timing(RSP_GBI0_DL);

    uint32 addr = RSPSegmentAddr((gfx->dlist.addr)) & (g_dwRamSize-1);

    LOG_UCODE("    Address=0x%08x Push: 0x%02x", addr, gfx->dlist.param);
    if( addr > g_dwRamSize )
    {
        RSP_RDP_NOIMPL("Error: DL addr = %08X out of range, PC=%08X", addr, gDlistStack[gDlistStackPointer].pc );
        addr &= (g_dwRamSize-1);
        DebuggerPauseCountN( NEXT_DLIST );
    }

    if( gfx->dlist.param == RSP_DLIST_PUSH )
        gDlistStackPointer++;

    gDlistStack[gDlistStackPointer].pc = addr;
    gDlistStack[gDlistStackPointer].countdown = MAX_DL_COUNT;

        LOG_UCODE("Level=%d", gDlistStackPointer+1);
        LOG_UCODE("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
}

void RSP_GBI0_Tri4(Gfx *gfx)
{
    uint32 w0 = gfx->words.cmd0;
    uint32 w1 = gfx->words.cmd1;

    status.primitiveType = PRIM_TRI2;

    // While the next command pair is Tri2, add vertices
    uint32 dwPC = gDlistStack[gDlistStackPointer].pc;

    BOOL bTrisAdded = FALSE;

    do {
        uint32 dwFlag = (w0>>16)&0xFF;
        LOG_UCODE("    PD Tri4: 0x%08x 0x%08x Flag: 0x%02x", gfx->words.cmd0, gfx->words.cmd1, dwFlag);

        BOOL bVisible;
        for( int i=0; i<4; i++)
        {
            uint32 v0 = (w1>>(4+(i<<3))) & 0xF;
            uint32 v1 = (w1>>(  (i<<3))) & 0xF;
            uint32 v2 = (w0>>(  (i<<2))) & 0xF;
            bVisible = IsTriangleVisible(v0, v2, v1);
            LOG_UCODE("       (%d, %d, %d) %s", v0, v1, v2, bVisible ? "": "(clipped)");
            if (bVisible)
            {
                DEBUG_DUMP_VERTEXES("Tri4_PerfectDark 1/2", v0, v1, v2);
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
                PrepareTriangle(v0, v2, v1);
            }
        }
        
        w0          = *(uint32 *)(g_pRDRAMu8 + dwPC+0);
        w1          = *(uint32 *)(g_pRDRAMu8 + dwPC+4);
        dwPC += 8;

#ifdef DEBUGGER
    } while (!(pauseAtNext && eventToPause==NEXT_TRIANGLE) && (w0>>24) == (uint8)RSP_TRI2);
#else
    } while (((w0)>>24) == (uint8)RSP_TRI2);
#endif

    gDlistStack[gDlistStackPointer].pc = dwPC-8;

    if (bTrisAdded) 
    {
        CRender::g_pRender->DrawTriangles();
    }
    
    DEBUG_TRIANGLE(TRACE0("Pause at GBI0 TRI4"));

//  gDKRVtxCount=0;
}

