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

int cur_mtx = 0;
int billboarding = 0;
int vtx_last = 0;
uint32_t dma_offset_mtx = 0;
uint32_t dma_offset_vtx = 0;

static void uc5_dma_offsets(uint32_t w0, uint32_t w1)
{
  dma_offset_mtx = _SHIFTR( w0, 0, 24);
  dma_offset_vtx = _SHIFTR( w1, 0, 24);
  vtx_last = 0;
}

static void uc5_matrix(uint32_t w0, uint32_t w1)
{
   // Use segment offset to get the address
   uint32_t addr = dma_offset_mtx + RSP_SegmentToPhysical(w1);

   uint8_t index = _SHIFTR(w0, 16, 4);
   uint8_t multiply;

   if (index == 0) //DKR
   {
      index = _SHIFTR(w0, 22, 2);
      multiply = 0;
   }
   else //JF
      multiply = _SHIFTR(w0, 23, 1);

   cur_mtx = index;

   if (multiply)
   {
      DECLAREALIGN16VAR(m[4][4]);
      DECLAREALIGN16VAR(m_src[4][4]);

      load_matrix(m, addr);
      memcpy (m_src, rdp.dkrproj[0], 64);
      MulMatrices(m, m_src, rdp.dkrproj[index]);
   }
   else
      load_matrix(rdp.dkrproj[index], addr);

   g_gdp.flags |= UPDATE_MULT_MAT;
}

static void uc5_vertex(uint32_t w0, uint32_t w1)
{
   int i, first, prj, n;
   uint32_t addr = dma_offset_vtx + RSP_SegmentToPhysical(w1);

   // | cccc cccc 1111 1??? 0000 0002 2222 2222 | cmd1 = address |
   // c = vtx command
   // 1 = method #1 of getting count
   // 2 = method #2 of getting count
   // ? = unknown, but used
   // 0 = unused

   n = _SHIFTR( w0, 19, 5);
   if (settings.hacks&hack_Diddy)
      n++;

   if (w0 & G_FOG)
   {
      if (billboarding)
         vtx_last = 1;
   }
   else
      vtx_last = 0;

   first = vtx_last + _SHIFTR(w0, 9, 5);;

   prj = cur_mtx;

   for (i = first; i < first + n; i++)
   {
      VERTEX *v = (VERTEX*)&rdp.vtx[i];
      int start = (i-first) * 10;
      float x   = (float)((int16_t*)gfx_info.RDRAM)[(((addr+start) >> 1) + 0)^1];
      float y   = (float)((int16_t*)gfx_info.RDRAM)[(((addr+start) >> 1) + 1)^1];
      float z   = (float)((int16_t*)gfx_info.RDRAM)[(((addr+start) >> 1) + 2)^1];

      v->x = x*rdp.dkrproj[prj][0][0] + y*rdp.dkrproj[prj][1][0] + z*rdp.dkrproj[prj][2][0] + rdp.dkrproj[prj][3][0];
      v->y = x*rdp.dkrproj[prj][0][1] + y*rdp.dkrproj[prj][1][1] + z*rdp.dkrproj[prj][2][1] + rdp.dkrproj[prj][3][1];
      v->z = x*rdp.dkrproj[prj][0][2] + y*rdp.dkrproj[prj][1][2] + z*rdp.dkrproj[prj][2][2] + rdp.dkrproj[prj][3][2];
      v->w = x*rdp.dkrproj[prj][0][3] + y*rdp.dkrproj[prj][1][3] + z*rdp.dkrproj[prj][2][3] + rdp.dkrproj[prj][3][3];

      if (billboarding)
      {
         v->x += rdp.vtx[0].x;
         v->y += rdp.vtx[0].y;
         v->z += rdp.vtx[0].z;
         v->w += rdp.vtx[0].w;
      }

      if (fabs(v->w) < 0.001)
         v->w = 0.001f;

      v->oow = 1.0f / v->w;
      v->x_w = v->x * v->oow;
      v->y_w = v->y * v->oow;
      v->z_w = v->z * v->oow;

      v->uv_calculated = 0xFFFFFFFF;
      v->screen_translated = 0;
      v->shade_mod = 0;

      v->scr_off = 0;
      if (v->x < -v->w)
         v->scr_off |= 1;
      if (v->x > v->w)
         v->scr_off |= 2;
      if (v->y < -v->w)
         v->scr_off |= 4;
      if (v->y > v->w)
         v->scr_off |= 8;
      if (v->w < 0.1f)
         v->scr_off |= 16;
      if (fabs(v->z_w) > 1.0)
         v->scr_off |= 32;

      v->r = ((uint8_t*)gfx_info.RDRAM)[(addr+start + 6)^3];
      v->g = ((uint8_t*)gfx_info.RDRAM)[(addr+start + 7)^3];
      v->b = ((uint8_t*)gfx_info.RDRAM)[(addr+start + 8)^3];
      v->a = ((uint8_t*)gfx_info.RDRAM)[(addr+start + 9)^3];
      CalculateFog (v);
   }

   vtx_last += n;
}

static void uc5_tridma(uint32_t w0, uint32_t w1)
{
   int i;
   uint32_t addr = RSP_SegmentToPhysical(w1);
   int num = _SHIFTR( w0, 4, 12);

   vtx_last = 0;    // we've drawn something, so the vertex index needs resetting

   // | cccc cccc 2222 0000 1111 1111 1111 0000 | cmd1 = address |
   // c = tridma command
   // 1 = method #1 of getting count
   // 2 = method #2 of getting count
   // 0 = unused


   for (i = 0; i < num; i++)
   {
      int flags;
      VERTEX *v[3];
      unsigned cull_mode = GR_CULL_NEGATIVE;
      int start = i << 4;
      int v0 = gfx_info.RDRAM[addr+start];
      int v1 = gfx_info.RDRAM[addr+start+1];
      int v2 = gfx_info.RDRAM[addr+start+2];

      v[0] = &rdp.vtx[v0];
      v[1] = &rdp.vtx[v1];
      v[2] = &rdp.vtx[v2];

      flags = gfx_info.RDRAM[addr+start+3];

      if (flags & 0x40)
      { // no cull
         rdp.flags &= ~CULLMASK;
         cull_mode = GR_CULL_DISABLE;
      }
      else
      {        // front cull
         rdp.flags &= ~CULLMASK;
         if (rdp.view_scale[0] < 0)
         {
            rdp.flags |= CULL_BACK;   // agh, backwards culling
            cull_mode = GR_CULL_POSITIVE;
         }
         else
            rdp.flags |= CULL_FRONT;
      }
      grCullMode(cull_mode);
      start += 4;

      v[0]->ou = (float)((int16_t*)gfx_info.RDRAM)[((addr+start) >> 1) + 5] / 32.0f;
      v[0]->ov = (float)((int16_t*)gfx_info.RDRAM)[((addr+start) >> 1) + 4] / 32.0f;
      v[1]->ou = (float)((int16_t*)gfx_info.RDRAM)[((addr+start) >> 1) + 3] / 32.0f;
      v[1]->ov = (float)((int16_t*)gfx_info.RDRAM)[((addr+start) >> 1) + 2] / 32.0f;
      v[2]->ou = (float)((int16_t*)gfx_info.RDRAM)[((addr+start) >> 1) + 1] / 32.0f;
      v[2]->ov = (float)((int16_t*)gfx_info.RDRAM)[((addr+start) >> 1) + 0] / 32.0f;

      v[0]->uv_calculated = 0xFFFFFFFF;
      v[1]->uv_calculated = 0xFFFFFFFF;
      v[2]->uv_calculated = 0xFFFFFFFF;

      cull_trianglefaces(v, 1, true, true, 0);
   }
}

static void uc5_dl_in_mem(uint32_t w0, uint32_t w1)
{
	gSPDlistCount_G64(_SHIFTR(w0, 16, 8), w1);
}

static void uc5_moveword(uint32_t w0, uint32_t w1)
{
   switch (_SHIFTR( w0, 0, 8))
   {
      case 0x02:
         billboarding = w1 & 1;
         break;
      case G_MW_CLIP:
         if (((rdp.cmd0>>8)&0xFFFF) == 0x04)
         {
            rdp.clip_ratio = (float)vi_integer_sqrt(w1);
            g_gdp.flags |= UPDATE_VIEWPORT;
         }
         break;

      case G_MW_SEGMENT:
         rdp.segment[(w0 >> 10) & 0x0F] = w1;
         break;
      case G_MW_FOG:
         rdp.fog_multiplier = (int16_t)(w1 >> 16);
         rdp.fog_offset = (int16_t)(w1 & 0x0000FFFF);
         break;

      case 0x0a:  // moveword matrix select
         cur_mtx = _SHIFTR( w1, 6, 2);
         FRDP ("matrix select - mtx: %d\n", cur_mtx);
         break;
   }
}

static void uc5_setgeometrymode(uint32_t w0, uint32_t w1)
{
  rdp.geom_mode |= w1;

  if (w1 & 0x00000001)  // Z-Buffer enable
  {
    if (!(rdp.flags & ZBUF_ENABLED))
    {
      rdp.flags |= ZBUF_ENABLED;
      g_gdp.flags |= UPDATE_ZBUF_ENABLED;
    }
  }

  if (w1 & 0x00010000)      // Fog enable
  {
    if (!(rdp.flags & FOG_ENABLED))
    {
      rdp.flags |= FOG_ENABLED;
      g_gdp.flags |= UPDATE_FOG_ENABLED;
    }
  }
}

static void uc5_cleargeometrymode(uint32_t w0, uint32_t w1)
{
   rdp.geom_mode &= (~w1);

   if (w1 & 0x00000001)  // Z-Buffer enable
   {
      if (rdp.flags & ZBUF_ENABLED)
      {
         rdp.flags ^= ZBUF_ENABLED;
         g_gdp.flags |= UPDATE_ZBUF_ENABLED;
      }
   }

   if (w1 & 0x00010000)      // Fog enable
   {
      if (rdp.flags & FOG_ENABLED)
      {
         rdp.flags ^= FOG_ENABLED;
         g_gdp.flags |= UPDATE_FOG_ENABLED;
      }
   }
}
