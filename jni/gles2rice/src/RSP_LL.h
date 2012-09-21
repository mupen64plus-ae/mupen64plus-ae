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

#ifndef RSP_LL_H__
#define RSP_LL_H__
//*****************************************************************************

//IS called Last Legion, but is used for several other games like: Dark Rift, Toukon Road, Toukon Road 2.
// We need Turbo3D ucode support

//*****************************************************************************
//
//*****************************************************************************
void DLParser_RSP_Last_Legion_0x80(Gfx *gfx)
{
	gDlistStack[gDlistStackPointer].pc += 16;
	LOG_UCODE("DLParser_RSP_Last_Legion_0x80");
}

//*****************************************************************************
//
//*****************************************************************************
void DLParser_RSP_Last_Legion_0x00(Gfx *gfx)
{
	LOG_UCODE("DLParser_RSP_Last_Legion_0x00");
	gDlistStack[gDlistStackPointer].pc += 16;

	if( (gfx->words.cmd0) == 0 && (gfx->words.cmd1) )
	{
		uint32 newaddr = RSPSegmentAddr((gfx->words.cmd1));
		if( newaddr >= g_dwRamSize )
		{
			RDP_GFX_PopDL();
			return;
		}

		uint32 pc1 = *(uint32 *)(g_pRDRAMu8 + newaddr+8*1+4);
		uint32 pc2 = *(uint32 *)(g_pRDRAMu8 + newaddr+8*4+4);
		pc1 = RSPSegmentAddr(pc1);
		pc2 = RSPSegmentAddr(pc2);

		if( pc1 && pc1 != 0xffffff && pc1 < g_dwRamSize)
		{
			// Need to call both DL
			gDlistStackPointer++;
			gDlistStack[gDlistStackPointer].pc = pc1;
			gDlistStack[gDlistStackPointer].countdown = MAX_DL_COUNT;
		}

		if( pc2 && pc2 != 0xffffff && pc2 < g_dwRamSize )
		{
			gDlistStackPointer++;
			gDlistStack[gDlistStackPointer].pc = pc2;
			gDlistStack[gDlistStackPointer].countdown = MAX_DL_COUNT;
		}
	}
	else if( (gfx->words.cmd1) == 0 )
	{
		RDP_GFX_PopDL();
	}
	else
	{
		RSP_RDP_Nothing(gfx);
		RDP_GFX_PopDL();
	}
}

//*****************************************************************************
//
//*****************************************************************************
void DLParser_TexRect_Last_Legion(Gfx *gfx)
{
	if( !status.bCIBufferIsRendered ) g_pFrameBufferManager->ActiveTextureBuffer();

	status.primitiveType = PRIM_TEXTRECT;

	// This command used 128bits, and not 64 bits. This means that we have to look one 
	// Command ahead in the buffer, and update the PC.
	uint32 dwPC = gDlistStack[gDlistStackPointer].pc;		// This points to the next instruction
	uint32 dwCmd2 = *(uint32 *)(g_pRDRAMu8 + dwPC);
	uint32 dwCmd3 = *(uint32 *)(g_pRDRAMu8 + dwPC+4);

	gDlistStack[gDlistStackPointer].pc += 8;


	LOG_UCODE("0x%08x: %08x %08x", dwPC, *(uint32 *)(g_pRDRAMu8 + dwPC+0), *(uint32 *)(g_pRDRAMu8 + dwPC+4));

	uint32 dwXH		= (((gfx->words.cmd0)>>12)&0x0FFF)/4;
	uint32 dwYH		= (((gfx->words.cmd0)    )&0x0FFF)/4;
	uint32 tileno	= ((gfx->words.cmd1)>>24)&0x07;
	uint32 dwXL		= (((gfx->words.cmd1)>>12)&0x0FFF)/4;
	uint32 dwYL		= (((gfx->words.cmd1)    )&0x0FFF)/4;


	if( (int)dwXL >= gRDP.scissor.right || (int)dwYL >= gRDP.scissor.bottom || (int)dwXH < gRDP.scissor.left || (int)dwYH < gRDP.scissor.top )
	{
		// Clipping
		return;
	}

	uint16 uS		= (uint16)(  dwCmd2>>16)&0xFFFF;
	uint16 uT		= (uint16)(  dwCmd2    )&0xFFFF;
	short s16S = *(short*)(&uS);
	short s16T = *(short*)(&uT);

	uint16  uDSDX 	= (uint16)((  dwCmd3>>16)&0xFFFF);
	uint16  uDTDY	    = (uint16)((  dwCmd3    )&0xFFFF);
	short	 s16DSDX  = *(short*)(&uDSDX);
	short  s16DTDY	= *(short*)(&uDTDY);

	uint32 curTile = gRSP.curTile;
	ForceMainTextureIndex(tileno);

	float fS0 = s16S / 32.0f;
	float fT0 = s16T / 32.0f;

	float fDSDX = s16DSDX / 1024.0f;
	float fDTDY = s16DTDY / 1024.0f;

	uint32 cycletype = gRDP.otherMode.cycle_type;

	if (cycletype == CYCLE_TYPE_COPY)
	{
		fDSDX /= 4.0f;	// In copy mode 4 pixels are copied at once.
		dwXH++;
		dwYH++;
	}
	else if (cycletype == CYCLE_TYPE_FILL)
	{
		dwXH++;
		dwYH++;
	}

	if( fDSDX == 0 )	fDSDX = 1;
	if( fDTDY == 0 )	fDTDY = 1;

	float fS1 = fS0 + (fDSDX * (dwXH - dwXL));
	float fT1 = fT0 + (fDTDY * (dwYH - dwYL));

	LOG_UCODE("    Tile:%d Screen(%d,%d) -> (%d,%d)", tileno, dwXL, dwYL, dwXH, dwYH);
	LOG_UCODE("           Tex:(%#5f,%#5f) -> (%#5f,%#5f) (DSDX:%#5f DTDY:%#5f)",
		fS0, fT0, fS1, fT1, fDSDX, fDTDY);
	LOG_UCODE("");

	float t0u0 = (fS0-gRDP.tiles[tileno].hilite_sl) * gRDP.tiles[tileno].fShiftScaleS;
	float t0v0 = (fT0-gRDP.tiles[tileno].hilite_tl) * gRDP.tiles[tileno].fShiftScaleT;
	float t0u1 = t0u0 + (fDSDX * (dwXH - dwXL))*gRDP.tiles[tileno].fShiftScaleS;
	float t0v1 = t0v0 + (fDTDY * (dwYH - dwYL))*gRDP.tiles[tileno].fShiftScaleT;

	if( dwXL==0 && dwYL==0 && dwXH==windowSetting.fViWidth-1 && dwYH==windowSetting.fViHeight-1 &&
		t0u0 == 0 && t0v0==0 && t0u1==0 && t0v1==0 )
	{
		//Using TextRect to clear the screen
	}
	else
	{
		if( status.bHandleN64RenderTexture && //status.bDirectWriteIntoRDRAM && 
			g_pRenderTextureInfo->CI_Info.dwFormat == gRDP.tiles[tileno].dwFormat && 
			g_pRenderTextureInfo->CI_Info.dwSize == gRDP.tiles[tileno].dwSize && 
			gRDP.tiles[tileno].dwFormat == TXT_FMT_CI && gRDP.tiles[tileno].dwSize == TXT_SIZE_8b )
		{
			if( options.enableHackForGames == HACK_FOR_YOSHI )
			{
				// Hack for Yoshi background image
				PrepareTextures();
				TexRectToFrameBuffer_8b(dwXL, dwYL, dwXH, dwYH, t0u0, t0v0, t0u1, t0v1, tileno);
				DEBUGGER_PAUSE_AT_COND_AND_DUMP_COUNT_N((eventToPause == NEXT_FLUSH_TRI || eventToPause == NEXT_TEXTRECT), {
					DebuggerAppendMsg("TexRect: tile=%d, X0=%d, Y0=%d, X1=%d, Y1=%d,\nfS0=%f, fT0=%f, ScaleS=%f, ScaleT=%f\n",
						gRSP.curTile, dwXL, dwYL, dwXH, dwYH, fS0, fT0, fDSDX, fDTDY);
					DebuggerAppendMsg("Pause after TexRect for Yoshi\n");
				});

			}
			else
			{
				if( frameBufferOptions.bUpdateCIInfo )
				{
					PrepareTextures();
					TexRectToFrameBuffer_8b(dwXL, dwYL, dwXH, dwYH, t0u0, t0v0, t0u1, t0v1, tileno);
				}

				if( !status.bDirectWriteIntoRDRAM )
				{
					CRender::g_pRender->TexRect(dwXL, dwYL, dwXH, dwYH, fS0, fT0, fDSDX, fDTDY);

					status.dwNumTrisRendered += 2;
				}
			}
		}
		else
		{
			CRender::g_pRender->TexRect(dwXL, dwYL, dwXH, dwYH, fS0, fT0, fDSDX, fDTDY);
			status.bFrameBufferDrawnByTriangles = true;

			status.dwNumTrisRendered += 2;
		}
	}

	if( status.bHandleN64RenderTexture )	g_pRenderTextureInfo->maxUsedHeight = max(g_pRenderTextureInfo->maxUsedHeight,(int)dwYH);

	ForceMainTextureIndex(curTile);
}

#endif //RSP_LL_H__