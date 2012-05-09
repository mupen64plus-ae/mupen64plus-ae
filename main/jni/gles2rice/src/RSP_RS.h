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
#ifndef RSP_RS_H__
#define RSP_RS_H__

uint32 Rogue_Squadron_Vtx_XYZ_Cmd;
uint32 Rogue_Squadron_Vtx_XYZ_Addr;
uint32 Rogue_Squadron_Vtx_Color_Cmd;
uint32 Rogue_Squadron_Vtx_Color_Addr;

void ProcessVertexData_Rogue_Squadron(uint32 dwXYZAddr, uint32 dwColorAddr, uint32 dwXYZCmd, uint32 dwColorCmd);
void DLParser_RS_Color_Buffer(Gfx *gfx)
{
	uint32 dwPC = gDlistStack[gDlistStackPointer].pc-8;
	uint32 dwAddr = RSPSegmentAddr((gfx->words.cmd1));

	if( dwAddr > g_dwRamSize )
	{
		TRACE0("DL, addr is wrong");
		dwAddr = (gfx->words.cmd1)&(g_dwRamSize-1);
	}

	Rogue_Squadron_Vtx_Color_Cmd = (gfx->words.cmd0);
	Rogue_Squadron_Vtx_Color_Addr = dwAddr;

	LOG_UCODE("Vtx_Color at PC=%08X: 0x%08x 0x%08x\n", dwPC-8, (gfx->words.cmd0), (gfx->words.cmd1));
#ifdef DEBUGGER
	if( pauseAtNext && (eventToPause == NEXT_VERTEX_CMD ) )
	{
		DebuggerAppendMsg("Vtx_Color at PC=%08X: 0x%08x 0x%08x\n", dwPC-8, (gfx->words.cmd0), (gfx->words.cmd1));
		if( dwAddr < g_dwRamSize )
		{
			DumpHex(dwAddr, min(64, g_dwRamSize-dwAddr));
		}
	}
#endif

	ProcessVertexData_Rogue_Squadron(Rogue_Squadron_Vtx_XYZ_Addr, Rogue_Squadron_Vtx_Color_Addr, Rogue_Squadron_Vtx_XYZ_Cmd, Rogue_Squadron_Vtx_Color_Cmd);
}


void DLParser_RS_Vtx_Buffer(Gfx *gfx)
{
	uint32 dwPC = gDlistStack[gDlistStackPointer].pc-8;
	uint32 dwAddr = RSPSegmentAddr((gfx->words.cmd1));
	if( dwAddr > g_dwRamSize )
	{
		TRACE0("DL, addr is wrong");
		dwAddr = (gfx->words.cmd1)&(g_dwRamSize-1);
	}

	LOG_UCODE("Vtx_XYZ at PC=%08X: 0x%08x 0x%08x\n", dwPC-8, (gfx->words.cmd0), (gfx->words.cmd1));
	Rogue_Squadron_Vtx_XYZ_Cmd = (gfx->words.cmd0);
	Rogue_Squadron_Vtx_XYZ_Addr = dwAddr;

#ifdef DEBUGGER
	if( pauseAtNext && (eventToPause == NEXT_VERTEX_CMD ) )
	{
		DebuggerAppendMsg("Vtx_XYZ at PC=%08X: 0x%08x 0x%08x\n", dwPC-8, (gfx->words.cmd0), (gfx->words.cmd1));
		if( dwAddr < g_dwRamSize )
		{
			DumpHex(dwAddr, min(64, g_dwRamSize-dwAddr));
		}
	}
#endif
}


void DLParser_RS_Block(Gfx *gfx)
{
	uint32 dwPC = gDlistStack[gDlistStackPointer].pc-8;
	LOG_UCODE("ucode 0x80 at PC=%08X: 0x%08x 0x%08x\n", dwPC, (gfx->words.cmd0), (gfx->words.cmd1));
}

void DLParser_RS_MoveMem(Gfx *gfx)
{
	uint32 dwPC = gDlistStack[gDlistStackPointer].pc;
	uint32 cmd1 = ((dwPC)&0x00FFFFFF)|0x80000000;
	RSP_GBI1_MoveMem(gfx);
	/*
	LOG_UCODE("RS_MoveMem", ((gfx->words.cmd0)>>24));
	LOG_UCODE("\tPC=%08X: 0x%08x 0x%08x", dwPC, (gfx->words.cmd0), (gfx->words.cmd1));
	dwPC+=8;
	uint32 dwCmd2 = *(uint32 *)(g_pRDRAMu8 + dwPC);
	uint32 dwCmd3 = *(uint32 *)(g_pRDRAMu8 + dwPC+4);
	LOG_UCODE("\tPC=%08X: 0x%08x 0x%08x", dwPC, dwCmd2, dwCmd3);
	dwPC+=8;
	uint32 dwCmd4 = *(uint32 *)(g_pRDRAMu8 + dwPC);
	uint32 dwCmd5 = *(uint32 *)(g_pRDRAMu8 + dwPC+4);
	LOG_UCODE("\tPC=%08X: 0x%08x 0x%08x\n", dwPC, dwCmd4, dwCmd5);
	*/
	gDlistStack[gDlistStackPointer].pc += 16;

	//DEBUGGER_PAUSE_AND_DUMP(NEXT_SET_MODE_CMD, {
	//	DebuggerAppendMsg("Pause after RS_MoveMem at: %08X\n", dwPC-8);
	//});

}

void DLParser_RS_0xbe(Gfx *gfx)
{
	uint32 dwPC = gDlistStack[gDlistStackPointer].pc-8;
	LOG_UCODE("ucode %02X, skip 1", ((gfx->words.cmd0)>>24));
	LOG_UCODE("\tPC=%08X: 0x%08x 0x%08x", dwPC, (gfx->words.cmd0), (gfx->words.cmd1));
	dwPC+=8;
	uint32 dwCmd2 = *(uint32 *)(g_pRDRAMu8 + dwPC);
	uint32 dwCmd3 = *(uint32 *)(g_pRDRAMu8 + dwPC+4);
	LOG_UCODE("\tPC=%08X: 0x%08x 0x%08x\n", dwPC, dwCmd2, dwCmd3);
	gDlistStack[gDlistStackPointer].pc += 8;

	DEBUGGER_PAUSE_AND_DUMP(NEXT_SET_MODE_CMD, {
		DebuggerAppendMsg("Pause after RS_0xbe at: %08X\n", dwPC-8);
		DebuggerAppendMsg("\t0x%08x 0x%08x", (gfx->words.cmd0), (gfx->words.cmd1));
		DebuggerAppendMsg("\t0x%08x 0x%08x", dwCmd2, dwCmd3);
	});
}

#endif //RSP_RS_H__