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
#ifndef RSP_WRUS_H__
#define RSP_WRUS_H__

void RSP_Vtx_WRUS(Gfx *gfx)
{
	u32 dwAddr	 = RSPSegmentAddr(gfx->words.cmd1);
	u32 dwV0	 = ((gfx->words.cmd0 >>16 ) & 0xff) / 5;
	u32 dwN		 =  (gfx->words.cmd0 >>9  ) & 0x7f;
	u32 dwLength =  (gfx->words.cmd0      ) & 0x1ff;

	LOG_UCODE("    Address [0x%08x], v0: [%d], Num: [%d], Length: [0x%04x]", dwAddr, dwV0, dwN, dwLength);

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

#endif //RSP_WRUS_H__