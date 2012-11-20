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
#ifndef RSP_PD_H__
#define RSP_PD_H__

uint32 dwPDCIAddr = 0;

void ProcessVertexDataPD(uint32 dwAddr, uint32 dwV0, uint32 dwNum);
void RSP_Vtx_PD(Gfx *gfx)
{
	SP_Timing(RSP_GBI0_Vtx);

	uint32 dwAddr = RSPSegmentAddr((gfx->words.cmd1));
	uint32 dwV0 =  ((gfx->words.cmd0)>>16)&0x0F;
	uint32 dwN  = (((gfx->words.cmd0)>>20)&0x0F)+1;
	uint32 dwLength = ((gfx->words.cmd0))&0xFFFF;

	LOG_UCODE("    Address [0x%08x], Len[%d], v0: [%d], Num: [%d]", dwAddr, dwLength, dwV0, dwN);

	ProcessVertexDataPD(dwAddr, dwV0, dwN);
	status.dwNumVertices += dwN;
}

void RSP_Set_Vtx_CI_PD(Gfx *gfx)
{
	// Color index buf address
	dwPDCIAddr = RSPSegmentAddr((gfx->words.cmd1));
}

void RSP_Tri4_PD(Gfx *gfx)
{
	uint32 w0 = gfx->words.cmd0;
	uint32 w1 = gfx->words.cmd1;

	status.primitiveType = PRIM_TRI2;

	// While the next command pair is Tri2, add vertices
	uint32 dwPC = gDlistStack[gDlistStackPointer].pc;

	BOOL bTrisAdded = FALSE;

	do {
		uint32 dwFlag = (w0>>16)&0xFF;
		LOG_UCODE("    PD Tri4: 0x%08x 0x%08x Flag: 0x%02x", w0, w1, dwFlag);

		BOOL bVisible;
		for( uint32 i=0; i<4; i++)
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

		w0			= *(uint32 *)(g_pRDRAMu8 + dwPC+0);
		w1			= *(uint32 *)(g_pRDRAMu8 + dwPC+4);
		dwPC += 8;

#ifdef DEBUGGER
	} while (!(pauseAtNext && eventToPause==NEXT_TRIANGLE) && (w0>>24) == (uint8)RSP_TRI2);
#else
	} while ((w0>>24) == (uint8)RSP_TRI2);
#endif

	gDlistStack[gDlistStackPointer].pc = dwPC-8;

	if (bTrisAdded)	
	{
		CRender::g_pRender->DrawTriangles();
	}

	DEBUG_TRIANGLE(TRACE0("Pause at PD Tri4"));
}

#endif //RSP_PD_H__