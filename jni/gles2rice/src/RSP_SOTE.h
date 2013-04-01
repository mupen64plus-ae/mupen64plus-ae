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
#ifndef RSP_SOTE_H__
#define RSP_SOTE_H__

void RSP_Vtx_ShadowOfEmpire(Gfx *gfx)
{
    uint32 dwAddr = RSPSegmentAddr((gfx->words.cmd1));
    uint32 dwLength = ((gfx->words.cmd0))&0xFFFF;

    uint32 dwN= (((gfx->words.cmd0) >> 4) & 0xFFF) / 33 + 1;
    uint32 dwV0 = 0;

    LOG_UCODE("    Address 0x%08x, v0: %d, Num: %d, Length: 0x%04x", dwAddr, dwV0, dwN, dwLength);

    if (dwV0 >= 32)
        dwV0 = 31;

    if ((dwV0 + dwN) > 32)
    {
        TRACE0("Warning, attempting to load into invalid vertex positions");
        dwN = 32 - dwV0;
    }

    ProcessVertexData(dwAddr, dwV0, dwN);

    status.dwNumVertices += dwN;

    DisplayVertexInfo(dwAddr, dwV0, dwN);
}


void RSP_Quad3d_ShadowOfEmpire(Gfx *gfx)
{
    status.primitiveType = PRIM_TRI2;
    bool bTrisAdded = false;
    bool bTexturesAreEnabled = CRender::g_pRender->IsTextureEnabled();

    // While the next command pair is Tri2, add vertices
    uint32 dwPC = gDlistStack[gDlistStackPointer].pc;

    do {
        uint32 dwV0 = ((gfx->words.cmd1 >> 24) & 0xFF) / 5;
        uint32 dwV1 = ((gfx->words.cmd1 >> 16) & 0xFF) / 5;
        uint32 dwV2 = ((gfx->words.cmd1 >>  8) & 0xFF) / 5;

        uint32 dwV3 = ((gfx->words.cmd1 >> 24) & 0xFF) / 5;
        uint32 dwV4 = ((gfx->words.cmd1 >>  8) & 0xFF) / 5;
        uint32 dwV5 = ((gfx->words.cmd1      ) & 0xFF) / 5;

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

void RSP_Tri1_ShadowOfEmpire(Gfx *gfx)
{
    status.primitiveType = PRIM_TRI1;
    bool bTrisAdded = false;
    bool bTexturesAreEnabled = CRender::g_pRender->IsTextureEnabled();

    // While the next command pair is Tri1, add vertices
    uint32 dwPC = gDlistStack[gDlistStackPointer].pc;
    uint32 * pCmdBase = (uint32 *)(g_pRDRAMu8 + dwPC);

    do
    {
        uint32 dwV0 = ((gfx->words.cmd1 >> 16) & 0xFF) / 5;
        uint32 dwV1 = ((gfx->words.cmd1 >> 8) & 0xFF) / 5;
        uint32 dwV2 = (gfx->words.cmd1 & 0xFF) / 5;

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
}

#endif //RSP_SOTE_H__