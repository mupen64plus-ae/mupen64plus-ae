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
// Created by Gonetz, 2008
//
//****************************************************************
#include "GBI.h"

/******************Turbo3D microcode*************************/

struct t3dGlobState
{
   uint16_t		pad0;
   uint16_t		perspNorm;
   uint32_t		flag;
   uint32_t		othermode0;
   uint32_t		othermode1;
   uint32_t		segBases[16];
   /* the viewport to use */
   int16_t     vsacle1;
   int16_t     vsacle0;
   int16_t     vsacle3;
   int16_t     vsacle2;
   int16_t     vtrans1;
   int16_t     vtrans0;
   int16_t     vtrans3;
   int16_t     vtrans2;
   uint32_t  rdpCmds;
};

struct t3dState
{
   uint32_t	renderState;	/* render state */
   uint32_t	textureState;	/* texture state */
   uint8_t	flag;
   uint8_t	triCount;	/* how many tris? */
   uint8_t	vtxV0;		/* where to load verts? */
   uint8_t	vtxCount;	/* how many verts? */
   uint32_t	rdpCmds;	/* ptr (segment address) to RDP DL */
   uint32_t	othermode0;
   uint32_t	othermode1;
};


struct t3dTriN
{
   uint8_t	flag, v2, v1, v0;	/* flag is which one for flat shade */
};


static void t3dProcessRDP(uint32_t a)
{
   if (a)
   {
      rdp.LLE = 1;
      rdp.cmd0 = ((uint32_t*)gfx_info.RDRAM)[a++];
      rdp.cmd1 = ((uint32_t*)gfx_info.RDRAM)[a++];
      while (rdp.cmd0 + rdp.cmd1)
      {
         uint32_t cmd;
         gfx_instruction[0][rdp.cmd0>>24](rdp.cmd0, rdp.cmd1);
         rdp.cmd0 = ((uint32_t*)gfx_info.RDRAM)[a++];
         rdp.cmd1 = ((uint32_t*)gfx_info.RDRAM)[a++];
         cmd = rdp.cmd0>>24;
         if (cmd == G_TEXRECT || cmd == G_TEXRECTFLIP)
         {
            rdp.cmd2 = ((uint32_t*)gfx_info.RDRAM)[a++];
            rdp.cmd3 = ((uint32_t*)gfx_info.RDRAM)[a++];
         }
      }
      rdp.LLE = 0;
   }
}

static void t3dLoadGlobState(uint32_t pgstate)
{
   int s;
   int16_t scale_x, scale_y, scale_z, trans_x, trans_y, trans_z;
   struct t3dGlobState *gstate = (struct t3dGlobState*)&gfx_info.RDRAM[RSP_SegmentToPhysical(pgstate)];

   FRDP ("Global state. pad0: %04lx, perspNorm: %04lx, flag: %08lx\n", gstate->pad0, gstate->perspNorm, gstate->flag);
   rdp.cmd0 = gstate->othermode0;
   rdp.cmd1 = gstate->othermode1;
   rdp_setothermode(rdp.cmd0, rdp.cmd1);

   for (s = 0; s < 16; s++)
      rdp.segment[s] = gstate->segBases[s];

   scale_x = gstate->vsacle0 / 4;
   scale_y = gstate->vsacle1 / 4;;
   scale_z = gstate->vsacle2;
   trans_x = gstate->vtrans0 / 4;
   trans_y = gstate->vtrans1 / 4;
   trans_z = gstate->vtrans2;
   rdp.view_scale[0] = scale_x * rdp.scale_x;
   rdp.view_scale[1] = -scale_y * rdp.scale_y;
   rdp.view_scale[2] = 32.0f * scale_z;
   rdp.view_trans[0] = trans_x * rdp.scale_x;
   rdp.view_trans[1] = trans_y * rdp.scale_y;
   rdp.view_trans[2] = 32.0f * trans_z;
   g_gdp.flags |= UPDATE_VIEWPORT;
   FRDP ("viewport scale(%d, %d, %d), trans(%d, %d, %d)\n", scale_x, scale_y, scale_z,
         trans_x, trans_y, trans_z);

   t3dProcessRDP(RSP_SegmentToPhysical(gstate->rdpCmds) >> 2);
}

static void t3d_vertex(uint32_t addr, uint32_t v0, uint32_t n)
{
   uint32_t i;
   float x, y, z;

   n <<= 4;

   for (i = 0; i < n; i+=16)
   {
      VERTEX *v = &rdp.vtx[v0 + (i>>4)];
      x   = (float)((int16_t*)gfx_info.RDRAM)[(((addr+i) >> 1) + 0)^1];
      y   = (float)((int16_t*)gfx_info.RDRAM)[(((addr+i) >> 1) + 1)^1];
      z   = (float)((int16_t*)gfx_info.RDRAM)[(((addr+i) >> 1) + 2)^1];
      v->flags  = ((uint16_t*)gfx_info.RDRAM)[(((addr+i) >> 1) + 3)^1];
      v->ou   = 2.0f * (float)((int16_t*)gfx_info.RDRAM)[(((addr+i) >> 1) + 4)^1];
      v->ov   = 2.0f * (float)((int16_t*)gfx_info.RDRAM)[(((addr+i) >> 1) + 5)^1];
      v->uv_scaled = 0;
      v->r = ((uint8_t*)gfx_info.RDRAM)[(addr+i + 12)^3];
      v->g = ((uint8_t*)gfx_info.RDRAM)[(addr+i + 13)^3];
      v->b = ((uint8_t*)gfx_info.RDRAM)[(addr+i + 14)^3];
      v->a    = ((uint8_t*)gfx_info.RDRAM)[(addr+i + 15)^3];

      v->x = x*rdp.combined[0][0] + y*rdp.combined[1][0] + z*rdp.combined[2][0] + rdp.combined[3][0];
      v->y = x*rdp.combined[0][1] + y*rdp.combined[1][1] + z*rdp.combined[2][1] + rdp.combined[3][1];
      v->z = x*rdp.combined[0][2] + y*rdp.combined[1][2] + z*rdp.combined[2][2] + rdp.combined[3][2];
      v->w = x*rdp.combined[0][3] + y*rdp.combined[1][3] + z*rdp.combined[2][3] + rdp.combined[3][3];

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
#ifdef EXTREME_LOGGING
      FRDP ("v%d - x: %f, y: %f, z: %f, w: %f, u: %f, v: %f, f: %f, z_w: %f, r=%d, g=%d, b=%d, a=%d\n", i>>4, v->x, v->y, v->z, v->w, v->ou*rdp.tiles[rdp.cur_tile].s_scale, v->ov*rdp.tiles[rdp.cur_tile].t_scale, v->f, v->z_w, v->r, v->g, v->b, v->a);
#endif
   }
}

static void t3dLoadObject(uint32_t pstate, uint32_t pvtx, uint32_t ptri)
{
   int t;
   struct t3dState *ostate = (struct t3dState*)&gfx_info.RDRAM[RSP_SegmentToPhysical(pstate)];
   rdp.cur_tile = (ostate->textureState)&7;

   LRDP("Loading Turbo3D object\n");
   FRDP("tile: %d\n", rdp.cur_tile);

   if (rdp.tiles[rdp.cur_tile].s_scale < 0.001f)
      rdp.tiles[rdp.cur_tile].s_scale = 0.015625;
   if (rdp.tiles[rdp.cur_tile].t_scale < 0.001f)
      rdp.tiles[rdp.cur_tile].t_scale = 0.015625;

#ifdef EXTREME_LOGGING
   FRDP("renderState: %08lx, textureState: %08lx, othermode0: %08lx, othermode1: %08lx, rdpCmds: %08lx, triCount : %d, v0: %d, vn: %d\n", ostate->renderState, ostate->textureState,
         ostate->othermode0, ostate->othermode1, ostate->rdpCmds, ostate->triCount, ostate->vtxV0, ostate->vtxCount);
#endif

   rdp.cmd0 = ostate->othermode0;
   rdp.cmd1 = ostate->othermode1;
   rdp_setothermode(rdp.cmd0, rdp.cmd1);

   rdp.cmd1 = ostate->renderState;
   uc0_setgeometrymode(rdp.cmd0, rdp.cmd1);

   if (!(ostate->flag&1)) //load matrix
   {
      uint32_t addr = RSP_SegmentToPhysical(pstate+sizeof(struct t3dState)) & BMASK;
      load_matrix(rdp.combined, addr);
#ifdef EXTREME_LOGGING
      FRDP ("{%f,%f,%f,%f}\n", rdp.combined[0][0], rdp.combined[0][1], rdp.combined[0][2], rdp.combined[0][3]);
      FRDP ("{%f,%f,%f,%f}\n", rdp.combined[1][0], rdp.combined[1][1], rdp.combined[1][2], rdp.combined[1][3]);
      FRDP ("{%f,%f,%f,%f}\n", rdp.combined[2][0], rdp.combined[2][1], rdp.combined[2][2], rdp.combined[2][3]);
      FRDP ("{%f,%f,%f,%f}\n", rdp.combined[3][0], rdp.combined[3][1], rdp.combined[3][2], rdp.combined[3][3]);
#endif
   }

   rdp.geom_mode &= ~G_LIGHTING;
   rdp.geom_mode |= UPDATE_SCISSOR;
   if (pvtx) //load vtx
      t3d_vertex(RSP_SegmentToPhysical(pvtx), ostate->vtxV0, ostate->vtxCount);

   t3dProcessRDP(RSP_SegmentToPhysical(ostate->rdpCmds) >> 2);

   if (ptri)
   {
      uint32_t a;
      update();
      a = RSP_SegmentToPhysical(ptri);
      for (t = 0; t < ostate->triCount; t++)
      {
         VERTEX *v[3];
         struct t3dTriN *tri = (struct t3dTriN*)&gfx_info.RDRAM[a];

         v[0] = &rdp.vtx[tri->v0]; 
         v[1] = &rdp.vtx[tri->v1];
         v[2] = &rdp.vtx[tri->v2];

         cull_trianglefaces(v, 1, false, true, 0);
         a += 4;
      }
   }
}

static void Turbo3D(void)
{
   uint32_t a, pgstate, pstate, pvtx, ptri;
   LRDP("Start Turbo3D microcode\n");
   settings.ucode = ucode_Fast3D;
   a = 0;
   pgstate = 0;
   pstate = 0;
   pvtx = 0;
   ptri = 0;

   do
   {
      a = rdp.pc[rdp.pc_i] & BMASK;
      pgstate = ((uint32_t*)gfx_info.RDRAM)[a>>2];
      pstate = ((uint32_t*)gfx_info.RDRAM)[(a>>2)+1];
      pvtx = ((uint32_t*)gfx_info.RDRAM)[(a>>2)+2];
      ptri = ((uint32_t*)gfx_info.RDRAM)[(a>>2)+3];
      FRDP("GlobalState: %08lx, Object: %08lx, Vertices: %08lx, Triangles: %08lx\n", pgstate, pstate, pvtx, ptri);
      if (!pstate)
      {
         rdp.halt = 1;
         break;
      }

      if (pgstate)
         t3dLoadGlobState(pgstate);
      t3dLoadObject(pstate, pvtx, ptri);
      // Go to the next instruction
      rdp.pc[rdp.pc_i] += 16;
   } while (pstate);

   // rdp_fullsync();
   
   settings.ucode = ucode_Turbo3d;
}
