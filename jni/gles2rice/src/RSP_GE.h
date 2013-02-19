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
#ifndef RSP_GE_H__
#define RSP_GE_H__

void DLParser_RDPHalf1_GoldenEye(Gfx *gfx)
{
    SP_Timing(RSP_GBI1_RDPHalf_1);
    if( ((gfx->words.cmd1)>>24) == 0xce )
    {
        PrepareTextures();
        CRender::g_pRender->SetCombinerAndBlender();

        uint32 dwPC = gDlistStack[gDlistStackPointer].pc;        // This points to the next instruction

        //PD_LoadMatrix_0xb4(dwPC + 8*16 - 8);

        uint32 dw1 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*0+4);
        uint32 dw2 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*1+4);
        uint32 dw3 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*2+4);
        uint32 dw4 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*3+4);
        uint32 dw5 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*4+4);
        uint32 dw6 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*5+4);
        uint32 dw7 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*6+4);
        uint32 dw8 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*7+4);
        uint32 dw9 = *(uint32 *)(g_pRDRAMu8 + dwPC+8*8+4);

        uint32 r = (dw8>>16)&0xFF;
        uint32 g = (dw8    )&0xFF;
        uint32 b = (dw9>>16)&0xFF;
        uint32 a = (dw9    )&0xFF;
        uint32 color = COLOR_RGBA(r, g, b, a);

        //int x0 = 0;
        //int x1 = gRDP.scissor.right;
        int x0 = gRSP.nVPLeftN;
        int x1 = gRSP.nVPRightN;
        int y0 = int(dw1&0xFFFF)/4;
        int y1 = int(dw1>>16)/4;

        float xscale = g_textures[0].m_pCTexture->m_dwWidth / (float)(x1-x0);
        float yscale = g_textures[0].m_pCTexture->m_dwHeight / (float)(y1-y0);
        float fs0 = (short)(dw3&0xFFFF)/32768.0f*g_textures[0].m_pCTexture->m_dwWidth;
        float ft0 = (short)(dw3>>16)/32768.0f*256;

        CRender::g_pRender->TexRect(x0,y0,x1,y1,0,0,xscale,yscale,true,color);

        gDlistStack[gDlistStackPointer].pc += 312;

#ifdef DEBUGGER
        if( logUcodes)
        {
            dwPC -= 8;
            LOG_UCODE("GoldenEye Sky at PC=%08X: 0x%08x 0x%08x", dwPC, (gfx->words.cmd0), (gfx->words.cmd1));
            uint32 *ptr = (uint32 *)(g_pRDRAMu8 + dwPC);
            for( int i=0; i<21; i++, dwPC+=16,ptr+=4 )
            {
                LOG_UCODE("%08X: %08X %08X %08X %08X", dwPC, ptr[0], ptr[1], ptr[2], ptr[3]);
            }
        }
#endif

        DEBUGGER_PAUSE_AND_DUMP_COUNT_N(NEXT_FLUSH_TRI, {
            TRACE0("Pause after Golden Sky Drawing\n");
        });
    }
}
#endif //RSP_GE_H__