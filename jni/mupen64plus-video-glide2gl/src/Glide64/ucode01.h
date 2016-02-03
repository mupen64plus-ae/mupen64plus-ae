/*
* Glide64 - Glide video plugin for Nintendo 64 emulators.
* Copyright (c) 2002  Dave2001
* Copyright (c) 2003-2009  Sergey 'Gonetz' Lipski
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

//****************************************************************
//
// Glide64 - Glide Plugin for Nintendo 64 emulators
// Project started on December 29th, 2001
//
// Authors:
// Dave2001, original author, founded the project in 2001, left it in 2002
// Gugaman, joined the project in 2002, left it in 2002
// Sergey 'Gonetz' Lipski, joined the project in 2002, main author since fall of 2002
// Hiroshi 'KoolSmoky' Morii, joined the project in 2007
//
//****************************************************************
//
// To modify Glide64:
// * Write your name and (optional)email, commented by your work, so I know who did it, and so that you can find which parts you modified when it comes time to send it to me.
// * Do NOT send me the whole project or file that you modified.  Take out your modified code sections, and tell me where to put them.  If people sent the whole thing, I would have many different versions, but no idea how to combine them all.
//
//****************************************************************

//
// vertex - loads vertices
//

static void uc1_vertex(uint32_t w0, uint32_t w1)
{
   int32_t v0 = (w0 >> 17) & 0x7F; // Current vertex
   int32_t n = (w0 >> 10) & 0x3F; // Number to copy
   rsp_vertex(v0, n);
}

static void uc1_tri1(uint32_t w0, uint32_t w1)
{
   VERTEX *v[3];
   if (rdp.skip_drawing)
      return;

   v[0] = &rdp.vtx[(w1 >> 17) & 0x7F];
   v[1] = &rdp.vtx[(w1 >> 9) & 0x7F];
   v[2] = &rdp.vtx[(w1 >> 1) & 0x7F];

   cull_trianglefaces(v, 1, true, true, 0);
}

static void uc1_tri2(uint32_t w0, uint32_t w1)
{
   VERTEX *v[6];
   if (rdp.skip_drawing)
      return;

   v[0] = &rdp.vtx[(w0 >> 17) & 0x7F];
   v[1] = &rdp.vtx[(w0 >> 9) & 0x7F];
   v[2] = &rdp.vtx[(w0 >> 1) & 0x7F];
   v[3] = &rdp.vtx[(w1 >> 17) & 0x7F];
   v[4] = &rdp.vtx[(w1 >> 9) & 0x7F];
   v[5] = &rdp.vtx[(w1 >> 1) & 0x7F];

   cull_trianglefaces(v, 2, true, true, 0);
}

static void uc1_line3d(uint32_t w0, uint32_t w1)
{
   if (!settings.force_quad3d && ((w1 & 0xFF000000) == 0) && ((w0 & 0x00FFFFFF) == 0))
   {
      uint32_t cull_mode;
      VERTEX *v[3];
      uint16_t width = (uint16_t)(w1 & 0xFF) + 3;

      v[0] = &rdp.vtx[(w1 >> 17) & 0x7F];
      v[1] = &rdp.vtx[(w1 >> 9) & 0x7F];
      v[2] = &rdp.vtx[(w1 >> 9) & 0x7F];
      cull_mode = (rdp.flags & CULLMASK) >> CULLSHIFT;
      rdp.flags |= CULLMASK;
      g_gdp.flags |= UPDATE_CULL_MODE;
      cull_trianglefaces(v, 1, true, true, width);
      rdp.flags ^= CULLMASK;
      rdp.flags |= cull_mode << CULLSHIFT;
      g_gdp.flags |= UPDATE_CULL_MODE;
   }
   else
   {
      VERTEX *v[6];

      v[0] = &rdp.vtx[(w1 >> 25) & 0x7F];
      v[1] = &rdp.vtx[(w1 >> 17) & 0x7F];
      v[2] = &rdp.vtx[(w1 >> 9) & 0x7F];
      v[3] = &rdp.vtx[(w1 >> 1) & 0x7F];
      v[4] = &rdp.vtx[(w1 >> 25) & 0x7F];
      v[5] = &rdp.vtx[(w1 >> 9) & 0x7F];

      cull_trianglefaces(v, 2, true, true, 0);
   }
}

uint32_t branch_dl = 0;

static void uc1_rdphalf_1(uint32_t w0, uint32_t w1)
{
   branch_dl = w1;
   rdphalf_1(w0, w1);
}

static void uc1_branch_z(uint32_t w0, uint32_t w1)
{
   uint32_t addr = RSP_SegmentToPhysical(branch_dl);
   uint32_t vtx  = (w0 & 0xFFF) >> 1;

   if( fabs(rdp.vtx[vtx].z) <= (w1/*&0xFFFF*/) )
      rdp.pc[rdp.pc_i] = addr;
}
