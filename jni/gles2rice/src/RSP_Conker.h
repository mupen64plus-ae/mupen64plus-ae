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
#ifndef RSP_CONKER_H__
#define RSP_CONKER_H__

uint32 dwConkerVtxZAddr=0;

extern void ProcessVertexDataConker(uint32 dwAddr, uint32 dwV0, uint32 dwNum);
void RSP_Vtx_Conker(Gfx *gfx)
{
	uint32 dwAddr = RSPSegmentAddr((gfx->words.cmd1));
	uint32 dwVEnd   = ((gfx->words.cmd0		 )&0xFFF)/2;
	uint32 dwN      = ((gfx->words.cmd0 >> 12)&0xFFF);
	uint32 dwV0		= dwVEnd - dwN;

	LOG_UCODE("    Vtx: Address 0x%08x, vEnd: %d, v0: %d, Num: %d", dwAddr, dwVEnd, dwV0, dwN);

	ProcessVertexDataConker(dwAddr, dwV0, dwN);
	status.dwNumVertices += dwN;
	DisplayVertexInfo(dwAddr, dwV0, dwN);
}

void RSP_Tri4_Conker(Gfx *gfx)
{
	uint32 w0 = gfx->words.cmd0;
	uint32 w1 = gfx->words.cmd1;

	status.primitiveType = PRIM_TRI2;

	// While the next command pair is Tri2, add vertices
	uint32 dwPC = gDlistStack[gDlistStackPointer].pc;

	bool bTrisAdded = FALSE;

	do {
		LOG_UCODE("    Conker Tri4: 0x%08x 0x%08x", w0, w1);
		uint32 idx[12];
		idx[0] = (w1      )&0x1F;
		idx[1] = (w1 >>  5)&0x1F;
		idx[2] = (w1 >> 10)&0x1F;

		bTrisAdded |= AddTri(idx[0], idx[1], idx[2]);

		idx[3] = (w1 >> 15)&0x1F;
		idx[4] = (w1 >> 20)&0x1F;
		idx[5] = (w1 >> 25)&0x1F;

		bTrisAdded |= AddTri(idx[3], idx[4], idx[5]);

		idx[6] = (w0    )&0x1F;
		idx[7] = (w0 >> 5)&0x1F;
		idx[8] = (w0 >> 10)&0x1F;

		bTrisAdded |= AddTri(idx[6], idx[7], idx[8]);

		idx[ 9] = ((((w0 >> 15)&0x7)<<2)|(w1>>30));
		idx[10] = (w0>>18)&0x1F;
		idx[11] = (w0>>23)&0x1F;

		bTrisAdded |= AddTri(idx[9], idx[10], idx[11]);

		w0 = *(uint32 *)(g_pRDRAMu8 + dwPC+0);
		w1 = *(uint32 *)(g_pRDRAMu8 + dwPC+4);
		dwPC += 8;

#ifdef DEBUGGER
	} while (!(pauseAtNext && eventToPause==NEXT_TRIANGLE) && (w0>>28) == 1 );
#else
	} while ((w0>>28) == 1);
#endif

	gDlistStack[gDlistStackPointer].pc = dwPC-8;

	if (bTrisAdded)	
	{
		CRender::g_pRender->DrawTriangles();
	}

	DEBUG_TRIANGLE(TRACE0("Pause at Conker Tri4"));
}

void RDP_GFX_Force_Vertex_Z_Conker(uint32 dwAddr)
{
	VTX_DUMP( 
	{
		s8 * pcBase = g_pRDRAMs8 + (dwAddr&(g_dwRamSize-1));
		uint32 * pdwBase = (uint32 *)pcBase;
		LONG i;

		for (i = 0; i < 4; i++)
		{
			DebuggerAppendMsg("    %08x %08x %08x %08x", pdwBase[0], pdwBase[1], pdwBase[2], pdwBase[3]);
			pdwBase+=4;
		}
	});

	dwConkerVtxZAddr = dwAddr;
	DEBUGGER_PAUSE_AND_DUMP(NEXT_VERTEX_CMD,{TRACE0("Paused at RDP_GFX_Force_Matrix_Conker Cmd");});
}

void RSP_MoveMem_Conker(Gfx *gfx)
{
	uint32 dwType = gfx->words.cmd0 & 0xFE;
	uint32 dwAddr = RSPSegmentAddr(gfx->words.cmd1);

	switch (dwType)
	{
	case RSP_GBI2_MV_MEM__MATRIX:
		{
			LOG_UCODE("    DLParser_MoveMem_Conker");
			RDP_GFX_Force_Vertex_Z_Conker(dwAddr);
		}
		break;
	case RSP_GBI2_MV_MEM__LIGHT:
		{
			LOG_UCODE("    MoveMem Light Conker");
			uint32 dwOffset2 = ((gfx->words.cmd0) >> 5) & 0x3FFF;
			if( dwOffset2 >= 0x30 )
			{
				uint32 dwLight = (dwOffset2 - 0x30)/0x30;
				LOG_UCODE("    Light %d:", dwLight);
				RSP_MoveMemLight(dwLight, dwAddr);
			}
			else
			{
				// fix me
				//TRACE0("Check me in DLParser_MoveMem_Conker - MoveMem Light");
			}
			DEBUGGER_PAUSE_AND_DUMP_COUNT_N( NEXT_SET_LIGHT, 
			{
				DebuggerAppendMsg("RSP_MoveMemLight: %d, Addr=%08X, cmd0=%08X", dwLight, dwAddr, (gfx->words.cmd0));
				TRACE0("Pause after MoveMemLight");
			});
		}
		break;
	default:
		RSP_GBI2_MoveMem(gfx);
		break;
	}
}

void RSP_Quad_Conker (Gfx *gfx)
{
	if ((gfx->words.cmd0 & 0x00FFFFFF) == 0x2F)
	{
		uint32 command = gfx->words.cmd0>>24;
		if (command == 0x6)
		{
			RSP_S2DEX_SPObjLoadTxSprite(gfx);
			return;
		}
		if (command == 0x7)
		{
			RSP_S2DEX_SPObjLoadTxSprite(gfx);
			return;
		}
	}
	uint32 v0 = ((gfx->words.cmd0 >> 17) & 0x7F);
	uint32 v1 = ((gfx->words.cmd0 >> 9) & 0x7F);
	uint32 v2 = ((gfx->words.cmd0 >> 1) & 0x7F);
	PrepareTriangle(v0,v1,v2);
}

void RSP_MoveWord_Conker(Gfx *gfx)
{
	uint32 dwType   = ((gfx->words.cmd0) >> 16) & 0xFF;
	if( dwType != RSP_MOVE_WORD_NUMLIGHT )
	{
		RSP_GBI2_MoveWord(gfx);
	}
	else
	{
		uint32 dwNumLights = ((gfx->words.cmd1)/48);
		LOG_UCODE("Conker RSP_MOVE_WORD_NUMLIGHT: %d", dwNumLights);
		gRSP.ambientLightIndex = dwNumLights+1;
		SetNumLights(dwNumLights);
		DEBUGGER_PAUSE_AND_DUMP_COUNT_N( NEXT_SET_LIGHT, 
		{
			DebuggerAppendMsg("SetNumLights: %d", dwNumLights);
			TRACE0("Pause after SetNumLights");
		});
	}
}
#endif //RSP_CONKER_H__