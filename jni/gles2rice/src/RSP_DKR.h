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
#ifndef RSP_DKR_H__
#define RSP_DKR_H__

uint32 gDKRCMatrixIndex = 0;
uint32 gDKRMatrixAddr = 0;
uint32 gDKRVtxAddr = 0;
uint32 gDKRVtxCount = 0;
bool gDKRBillBoard = false;
extern Matrix dkrMatrixTransposed;

// DKR verts are extra 4 bytes
//*****************************************************************************
//
//*****************************************************************************
void RDP_GFX_DumpVtxInfoDKR(uint32 dwAddr, uint32 dwV0, uint32 dwN)
{
#ifdef DEBUGGER
        uint32 dwV;
        LONG i;

        short * psSrc = (short *)(g_pRDRAMu8 + dwAddr);

        i = 0;
        for (dwV = dwV0; dwV < dwV0 + dwN; dwV++)
        {
            float x = (float)psSrc[(i + 0) ^ 1];
            float y = (float)psSrc[(i + 1) ^ 1];
            float z = (float)psSrc[(i + 2) ^ 1];

            //uint16 wFlags = CRender::g_pRender->m_dwVecFlags[dwV]; //(uint16)psSrc[3^0x1];

            uint16 wA = psSrc[(i + 3) ^ 1];
            uint16 wB = psSrc[(i + 4) ^ 1];

            uint8 a = wA>>8;
            uint8 b = (uint8)wA;
            uint8 c = wB>>8;
            uint8 d = (uint8)wB;

            XVECTOR4 & t = g_vecProjected[dwV];


            LOG_UCODE(" #%02d Pos: {% 6f,% 6f,% 6f} Extra: %02x %02x %02x %02x (transf: {% 6f,% 6f,% 6f})",
                dwV, x, y, z, a, b, c, d, t.x, t.y, t.z );

            i+=5;
        }


        uint16 * pwSrc = (uint16 *)(g_pRDRAMu8 + dwAddr);
        i = 0;
        for (dwV = dwV0; dwV < dwV0 + dwN; dwV++)
        {
            LOG_UCODE(" #%02d %04x %04x %04x %04x %04x",
                dwV, pwSrc[(i + 0) ^ 1],
                pwSrc[(i + 1) ^ 1],
                pwSrc[(i + 2) ^ 1],
                pwSrc[(i + 3) ^ 1],
                pwSrc[(i + 4) ^ 1]);

            i += 5;
        }

#endif // DEBUGGER
}

//*****************************************************************************
//
//*****************************************************************************
void RSP_Vtx_DKR(Gfx *gfx)
{
    uint32 dwAddr = (gfx->words.cmd1) + gDKRVtxAddr;
    uint32 dwV0 = (((gfx->words.cmd0) >> 9 )&0x1F);
    uint32 dwN  = (((gfx->words.cmd0) >>19 )&0x1F)+1;

    if( gfx->words.cmd0 & 0x00010000 )
    {
        if( gDKRBillBoard )
            gDKRVtxCount = 1;
    }
    else
    {
        gDKRVtxCount = 0;
    }

    dwV0 += gDKRVtxCount;

    LOG_UCODE("    Address 0x%08x, v0: %d, Num: %d", dwAddr, dwV0, dwN);
    DEBUGGER_ONLY_IF( (pauseAtNext && (eventToPause==NEXT_VERTEX_CMD||eventToPause==NEXT_MATRIX_CMD)), {DebuggerAppendMsg("DKR Vtx: Cmd0=%08X, Cmd1=%08X", (gfx->words.cmd0), (gfx->words.cmd1));});

    VTX_DUMP(TRACE2("Vtx_DKR, cmd0=%08X cmd1=%08X", (gfx->words.cmd0), (gfx->words.cmd1)));
    VTX_DUMP(TRACE2("Vtx_DKR, v0=%d n=%d", dwV0, dwN));

    if (dwV0 >= 32)        dwV0 = 31;

    if ((dwV0 + dwN) > 32)
    {
        WARNING(TRACE0("Warning, attempting to load into invalid vertex positions"));
        dwN = 32 - dwV0;
    }

    // Check that address is valid...
    if ((dwAddr + (dwN*16)) > g_dwRamSize)
    {
        WARNING(TRACE1("ProcessVertexData: Address out of range (0x%08x)", dwAddr));
    }
    else
    {
        ProcessVertexDataDKR(dwAddr, dwV0, dwN);

        status.dwNumVertices += dwN;

        RDP_GFX_DumpVtxInfoDKR(dwAddr, dwV0, dwN);
    }
}

//*****************************************************************************
//
//*****************************************************************************
void RSP_DL_In_MEM_DKR(Gfx *gfx)
{
    // This cmd is likely to execute number of ucode at the given address
    gDlistStackPointer++;
    gDlistStack[gDlistStackPointer].pc = gfx->words.cmd1;
    gDlistStack[gDlistStackPointer].countdown = (((gfx->words.cmd0)>>16)&0xFF);
}

//*****************************************************************************
//
//*****************************************************************************
void RSP_Mtx_DKR(Gfx *gfx)
{
    uint32 dwAddr = RSPSegmentAddr((gfx->words.cmd1));
    uint32 dwCommand = ((gfx->words.cmd0)>>16)&0xFF;
    uint32 dwLength  = ((gfx->words.cmd0))    &0xFFFF;

    dwAddr = (gfx->words.cmd1)+RSPSegmentAddr(gDKRMatrixAddr);

    bool mul=false;
    int index;
    switch( dwCommand )
    {
    case 0xC0:    // DKR
        gDKRCMatrixIndex = index = 3;
        break;
    case 0x80:    // DKR
        gDKRCMatrixIndex = index = 2;
        break;
    case 0x40:    // DKR
        gDKRCMatrixIndex = index = 1;
        break;
    case 0x20:    // DKR
        gDKRCMatrixIndex = index = 0;
        break;
    case 0x00:
        gDKRCMatrixIndex = index = 0;
        break;
    case 0x01:
        //mul = true;
        gDKRCMatrixIndex = index = 1;
        break;
    case 0x02:
        //mul = true;
        gDKRCMatrixIndex = index = 2;
        break;
    case 0x03:
        //mul = true;
        gDKRCMatrixIndex = index = 3;
        break;
    case 0x81:
        index = 1;
        mul = true;
        break;
    case 0x82:
        index = 2;
        mul = true;
        break;
    case 0x83:
        index = 3;
        mul = true;
        break;
    default:
        DebuggerAppendMsg("Fix me, mtx DKR, cmd=%08X", dwCommand);
        break;
    }

    // Load matrix from dwAddr
    Matrix &mat = gRSP.DKRMatrixes[index];
    LoadMatrix(dwAddr);

    if( mul )
    {
        mat = matToLoad*gRSP.DKRMatrixes[0];
    }
    else
    {
        mat = matToLoad;
    }

    if( status.isSSEEnabled )
        MatrixTranspose(&dkrMatrixTransposed, &mat);

    DEBUGGER_IF_DUMP(logMatrix,TRACE3("DKR Matrix: cmd=0x%X, idx = %d, mul=%d", dwCommand, index, mul));
    LOG_UCODE("    DKR Loading Mtx: %d, command=%d", index, dwCommand);
    DEBUGGER_PAUSE_AND_DUMP(NEXT_MATRIX_CMD,{TRACE0("Paused at DKR Matrix Cmd");});
}



//*****************************************************************************
//
//*****************************************************************************
void RSP_MoveWord_DKR(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_MoveWord);

    switch ((gfx->words.cmd0) & 0xFF)
    {
    case RSP_MOVE_WORD_NUMLIGHT:
        gDKRBillBoard = (gfx->words.cmd1)&0x1 ? true : false;
        LOG_UCODE("    gDKRBillBoard = %d", gDKRBillBoard);
        DEBUGGER_PAUSE_AND_DUMP_COUNT_N(NEXT_MATRIX_CMD, {DebuggerAppendMsg("DKR Moveword, select gRSP.DKRBillBoard %s, cmd0=%08X, cmd1=%08X", gDKRBillBoard?"true":"false", (gfx->words.cmd0), (gfx->words.cmd1));});
        break;
    case RSP_MOVE_WORD_LIGHTCOL:
        gDKRCMatrixIndex = ((gfx->words.cmd1)>>6)&0x7;
        LOG_UCODE("    gDKRCMatrixIndex = %d", gDKRCMatrixIndex);
        DEBUGGER_PAUSE_AND_DUMP_COUNT_N(NEXT_MATRIX_CMD, {DebuggerAppendMsg("DKR Moveword, select matrix %d, cmd0=%08X, cmd1=%08X", gDKRCMatrixIndex, (gfx->words.cmd0), (gfx->words.cmd1));});
        break;
    default:
        RSP_GBI1_MoveWord(gfx);
        break;
    }

}

//*****************************************************************************
//
//*****************************************************************************
void DLParser_Set_Addr_DKR(Gfx *gfx)
{
    gDKRMatrixAddr = gfx->words.cmd0 & 0x00FFFFFF;
    gDKRVtxAddr = RSPSegmentAddr(gfx->words.cmd1 & 0x00FFFFFF);
    gDKRVtxCount=0;
}

//*****************************************************************************
//
//*****************************************************************************
//DKR: 00229BA8: 05710080 001E4AF0 CMD G_DMATRI  Triangles 9 at 801E4AF0
void RSP_DMA_Tri_DKR(Gfx *gfx)
{
    uint32 dwAddr = RSPSegmentAddr(gfx->words.cmd1);
    uint32 dwNum = (((gfx->words.cmd0) &  0xFFF0) >>4 );
    TriDKR *tri = (TriDKR*)&g_pRDRAMu32[ dwAddr >> 2];

    if( dwAddr+16*dwNum >= g_dwRamSize )
    {
        TRACE0("DMATRI invalid memory pointer");
        return;
    }

    TRI_DUMP(TRACE2("DMATRI, addr=%08X, Cmd0=%08X\n", dwAddr, (gfx->words.cmd0)));

    bool bTrisAdded = false;

    status.primitiveType = PRIM_DMA_TRI;

    for (uint32 i = 0; i < dwNum; i++)
    {

        uint32 dwV0 = tri->v0;
        uint32 dwV1 = tri->v1;
        uint32 dwV2 = tri->v2;

        CRender::g_pRender->SetCullMode(!(tri->flag & 0x40), false);

        TRI_DUMP(TRACE5("DMATRI: %d, %d, %d (%08X-%08X)", dwV0,dwV1,dwV2,(gfx->words.cmd0),(gfx->words.cmd1)));

        DEBUG_DUMP_VERTEXES("DmaTri", dwV0, dwV1, dwV2);
        LOG_UCODE("   Tri: %d,%d,%d", dwV0, dwV1, dwV2);
        if (!bTrisAdded )//&& CRender::g_pRender->IsTextureEnabled())
        {
            PrepareTextures();
            InitVertexTextureConstants();
        }

        // Generate texture coordinates
        CRender::g_pRender->SetVtxTextureCoord(dwV0, tri->s0, tri->t0);
        CRender::g_pRender->SetVtxTextureCoord(dwV1, tri->s1, tri->t1);
        CRender::g_pRender->SetVtxTextureCoord(dwV2, tri->s2, tri->t2);

        if( !bTrisAdded )
        {
            CRender::g_pRender->SetCombinerAndBlender();
        }

        bTrisAdded = true;
        PrepareTriangle(dwV0, dwV1, dwV2);
        tri++;

    }

    if (bTrisAdded)
    {
        CRender::g_pRender->DrawTriangles();
    }
    gDKRVtxCount=0;
}

//*****************************************************************************
//
//*****************************************************************************
void RSP_Vtx_Gemini(Gfx *gfx)
{
    uint32 dwAddr = RSPSegmentAddr((gfx->words.cmd1));
    uint32 dwV0 =  (((gfx->words.cmd0)>>9)&0x1F);
    uint32 dwN  = (((gfx->words.cmd0) >>19 )&0x1F);

    LOG_UCODE("    Address 0x%08x, v0: %d, Num: %d", dwAddr, dwV0, dwN);
    DEBUGGER_ONLY_IF( (pauseAtNext && (eventToPause==NEXT_VERTEX_CMD||eventToPause==NEXT_MATRIX_CMD)), {DebuggerAppendMsg("DKR Vtx: Cmd0=%08X, Cmd1=%08X", (gfx->words.cmd0), (gfx->words.cmd1));});

    VTX_DUMP(TRACE2("Vtx_DKR, cmd0=%08X cmd1=%08X", (gfx->words.cmd0), (gfx->words.cmd1)));

    if (dwV0 >= 32)
        dwV0 = 31;

    if ((dwV0 + dwN) > 32)
    {
        TRACE0("Warning, attempting to load into invalid vertex positions");
        dwN = 32 - dwV0;
    }


    //if( dwAddr == 0 || dwAddr < 0x2000)
    {
        dwAddr = (gfx->words.cmd1)+RSPSegmentAddr(gDKRVtxAddr);
    }

    // Check that address is valid...
    if ((dwAddr + (dwN*16)) > g_dwRamSize)
    {
        TRACE1("ProcessVertexData: Address out of range (0x%08x)", dwAddr);
    }
    else
    {
        ProcessVertexDataDKR(dwAddr, dwV0, dwN);

        status.dwNumVertices += dwN;

        RDP_GFX_DumpVtxInfoDKR(dwAddr, dwV0, dwN);
    }
}

#endif //RSP_DKR_H__
