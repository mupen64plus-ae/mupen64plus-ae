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
// December 2008 Created by Gonetz (Gonetz@ngs.ru)
//
//****************************************************************


typedef float M44[4][4];

typedef struct
{
   float view_scale[2];
   float view_trans[2];
   float scale_x;
   float scale_y;
} ZSORTRDP;

ZSORTRDP zSortRdp = {{0, 0}, {0, 0}, 0, 0};

//RSP command VRCPL
static int Calc_invw (int w)
{
   int count, neg;
   union
   {
      int32_t		  W;
      uint32_t	  UW;
      int16_t			HW[2];
      uint16_t		UHW[2];
   } Result;
   Result.W = w;

   if (Result.UW == 0)
      Result.UW = 0x7FFFFFFF;
   else
   {
      if (Result.W < 0)
      {
         neg = true;
         if (Result.UHW[1] == 0xFFFF && Result.HW[0] < 0)
            Result.W = ~Result.W + 1;
         else
            Result.W = ~Result.W;
      }
      else
         neg = false;

      for (count = 31; count > 0; count--)
      {
         if ((Result.W & (1 << count)))
         {
            Result.W &= (0xFFC00000 >> (31 - count) );
            count = 0;
         }
      }

      Result.W = 0x7FFFFFFF / Result.W;
      for (count = 31; count > 0; count--)
      {
         if ((Result.W & (1 << count)))
         {
            Result.W &= (0xFFFF8000 >> (31 - count) );
            count = 0;
         }
      }
      if (neg == true)
         Result.W = ~Result.W;
   }
   return Result.W;
}

static void uc9_draw_object (uint8_t * addr, uint32_t type)
{
   uint32_t i, textured, vnum, vsize;
   VERTEX vtx[4], *pV[4];

   switch (type)
   {
      case 0: //null
         textured = vnum = vsize = 0;
         break;
      case 1: //sh tri
         textured = 0;
         vnum = 3;
         vsize = 8;
         break;
      case 2: //tx tri
         textured = 1;
         vnum = 3;
         vsize = 16;
         break;
      case 3: //sh quad
         textured = 0;
         vnum = 4;
         vsize = 8;
         break;
      case 4: //tx quad
         textured = 1;
         vnum = 4;
         vsize = 16;
         break;
   }

   for (i = 0; i < vnum; i++)
   {
      VERTEX *v = (VERTEX*)&vtx[i];
      v->sx = zSortRdp.scale_x * ((int16_t*)addr)[0^1];
      v->sy = zSortRdp.scale_y * ((int16_t*)addr)[1^1];
      v->sz = 1.0f;
      v->r = addr[4^3];
      v->g = addr[5^3];
      v->b = addr[6^3];
      v->a = addr[7^3];
      v->flags = 0;
      v->uv_scaled = 0;
      v->uv_calculated = 0xFFFFFFFF;
      v->shade_mod = 0;
      v->scr_off = 0;
      v->screen_translated = 2;
      if (textured)
      {
         v->ou = ((int16_t*)addr)[4^1];
         v->ov = ((int16_t*)addr)[5^1];
         v->w = Calc_invw(((int*)addr)[3]) / 31.0f;
         v->oow = 1.0f / v->w;
         //FRDP ("v%d - sx: %f, sy: %f ou: %f, ov: %f, w: %f, r=%d, g=%d, b=%d, a=%d\n", i, v->sx/rdp.scale_x, v->sy/rdp.scale_y, v->ou*rdp.tiles[rdp.cur_tile].s_scale, v->ov*rdp.tiles[rdp.cur_tile].t_scale, v->w, v->r, v->g, v->b, v->a);
      }
      else
      {
         v->oow = v->w = 1.0f;
         //FRDP ("v%d - sx: %f, sy: %f r=%d, g=%d, b=%d, a=%d\n", i, v->sx/rdp.scale_x, v->sy/rdp.scale_y, v->r, v->g, v->b, v->a);
      }
      addr += vsize;
   }
   //*
   pV[0] = &vtx[0];
   pV[1] = &vtx[1];
   pV[2] = &vtx[2];
   pV[3] = &vtx[3];

   cull_trianglefaces(pV, 1, false, false, 0); 

   if (vnum != 3)
      cull_trianglefaces(pV + 1, 1, false, false, 0); 
}

static uint32_t uc9_load_object (uint32_t zHeader, uint32_t * rdpcmds)
{
   uint32_t type, w0, w1;
   uint8_t *addr;
   
   w0 = rdp.cmd0;
   w1 = rdp.cmd1;
   type = zHeader & 7;
   addr = gfx_info.RDRAM + (zHeader&0xFFFFFFF8);

   switch (type)
   {
      case 1: //sh tri
      case 3: //sh quad
         {
            rdp.cmd1 = ((uint32_t*)addr)[1];
            if (rdp.cmd1 != rdpcmds[0])
            {
               rdpcmds[0] = rdp.cmd1;
               uc9_rpdcmd(w0, w1);
            }
            update();
            uc9_draw_object(addr + 8, type);
         }
         break;
      case 0: //null
      case 2: //tx tri
      case 4: //tx quad
         {
            rdp.cmd1 = ((uint32_t*)addr)[1];
            if (rdp.cmd1 != rdpcmds[0])
            {
               rdpcmds[0] = rdp.cmd1;
               uc9_rpdcmd(w0, w1);
            }
            rdp.cmd1 = ((uint32_t*)addr)[2];
            if (rdp.cmd1 != rdpcmds[1])
            {
               uc9_rpdcmd(w0, w1);
               rdpcmds[1] = rdp.cmd1;
            }
            rdp.cmd1 = ((uint32_t*)addr)[3];
            if (rdp.cmd1 != rdpcmds[2])
            {
               uc9_rpdcmd(w0, w1);
               rdpcmds[2] = rdp.cmd1;
            }
            if (type)
            {
               update();
               uc9_draw_object(addr + 16, type);
            }
         }
         break;
   }
   return RSP_SegmentToPhysical(((uint32_t*)addr)[0]);
}

static void uc9_object(uint32_t w0, uint32_t w1)
{
   uint32_t cmd1, zHeader;
   uint32_t rdpcmds[3];

   rdpcmds[0] = 0;
   rdpcmds[1] = 0;
   rdpcmds[2] = 0;

   cmd1 = w1;
   zHeader = RSP_SegmentToPhysical(w0);

   while (zHeader)
      zHeader = uc9_load_object(zHeader, rdpcmds);
   zHeader = RSP_SegmentToPhysical(cmd1);
   while (zHeader)
      zHeader = uc9_load_object(zHeader, rdpcmds);
}

static void uc9_mix(uint32_t w0, uint32_t w1)
{
}

static void uc9_fmlight(uint32_t w0, uint32_t w1)
{
   uint32_t i, a;
   int mid;
   M44 *m;

   mid = w0 & 0xFF;
   gSPNumLights_G64(1 + _SHIFTR(w1, 12, 8));
   a = -1024 + (w1 & 0xFFF);
   FRDP ("uc9:fmlight matrix: %d, num: %d, dmem: %04lx\n", mid, rdp.num_lights, a);

   switch (mid)
   {
      case 4:
         m = (M44*)rdp.model;
         break;
      case 6:
         m = (M44*)rdp.proj;
         break;
      case 8:
         m = (M44*)rdp.combined;
         break;
   }

   rdp.light[rdp.num_lights].col[0] = (float)(((uint8_t*)gfx_info.DMEM)[(a+0)^3]) / 255.0f;
   rdp.light[rdp.num_lights].col[1] = (float)(((uint8_t*)gfx_info.DMEM)[(a+1)^3]) / 255.0f;
   rdp.light[rdp.num_lights].col[2] = (float)(((uint8_t*)gfx_info.DMEM)[(a+2)^3]) / 255.0f;
   rdp.light[rdp.num_lights].col[3] = 1.0f;
   //FRDP ("ambient light: r: %.3f, g: %.3f, b: %.3f\n", rdp.light[rdp.num_lights].r, rdp.light[rdp.num_lights].g, rdp.light[rdp.num_lights].b);
   a += 8;

   for (i = 0; i < rdp.num_lights; i++)
   {
      rdp.light[i].col[0] = (float)(((uint8_t*)gfx_info.DMEM)[(a+0)^3]) / 255.0f;
      rdp.light[i].col[1] = (float)(((uint8_t*)gfx_info.DMEM)[(a+1)^3]) / 255.0f;
      rdp.light[i].col[2] = (float)(((uint8_t*)gfx_info.DMEM)[(a+2)^3]) / 255.0f;
      rdp.light[i].col[3] = 1.0f;
      rdp.light[i].dir[0] = (float)(((int8_t*)gfx_info.DMEM)[(a+8)^3]) / 127.0f;
      rdp.light[i].dir[1] = (float)(((int8_t*)gfx_info.DMEM)[(a+9)^3]) / 127.0f;
      rdp.light[i].dir[2] = (float)(((int8_t*)gfx_info.DMEM)[(a+10)^3]) / 127.0f;
      //FRDP ("light: n: %d, r: %.3f, g: %.3f, b: %.3f, x: %.3f, y: %.3f, z: %.3f\n", i, rdp.light[i].r, rdp.light[i].g, rdp.light[i].b, rdp.light[i].dir_x, rdp.light[i].dir_y, rdp.light[i].dir_z);
      InverseTransformVector(&rdp.light[i].dir[0], rdp.light_vector[i], *m);
      NormalizeVector (rdp.light_vector[i]);
      //FRDP ("light vector: n: %d, x: %.3f, y: %.3f, z: %.3f\n", i, rdp.light_vector[i][0], rdp.light_vector[i][1], rdp.light_vector[i][2]);
      a += 24;
   }

   for (i = 0; i < 2; i++)
   {
      float dir_x = (float)(((int8_t*)gfx_info.DMEM)[(a+8)^3]) / 127.0f;
      float dir_y = (float)(((int8_t*)gfx_info.DMEM)[(a+9)^3]) / 127.0f;
      float dir_z = (float)(((int8_t*)gfx_info.DMEM)[(a+10)^3]) / 127.0f;
      if (sqrt(dir_x*dir_x + dir_y*dir_y + dir_z*dir_z) < 0.98)
      {
         rdp.use_lookat = false;
         return;
      }
      rdp.lookat[i][0] = dir_x;
      rdp.lookat[i][1] = dir_y;
      rdp.lookat[i][2] = dir_z;
      a += 24;
   }
   rdp.use_lookat = true;
}

static void uc9_light(uint32_t w0, uint32_t w1)
{
   VERTEX v;
   uint32_t i;
   uint32_t csrs = -1024 + ((w0 >> 12) & 0xFFF);
   uint32_t nsrs = -1024 + (w0 & 0xFFF);
   uint32_t num = 1 + ((w1 >> 24) & 0xFF);
   uint32_t cdest = -1024 + ((w1 >> 12) & 0xFFF);
   uint32_t tdest = -1024 + (w1 & 0xFFF);
   int use_material = (csrs != 0x0ff0);
   tdest >>= 1;
   FRDP ("uc9:light n: %d, colsrs: %04lx, normales: %04lx, coldst: %04lx, texdst: %04lx\n", num, csrs, nsrs, cdest, tdest);

   for (i = 0; i < num; i++)
   {
      v.vec[0] = ((int8_t*)gfx_info.DMEM)[(nsrs++)^3];
      v.vec[1] = ((int8_t*)gfx_info.DMEM)[(nsrs++)^3];
      v.vec[2] = ((int8_t*)gfx_info.DMEM)[(nsrs++)^3];
      calc_sphere (&v);
      //    calc_linear (&v);
      NormalizeVector (v.vec);
      calc_light(&v);
      v.a = 0xFF;
      if (use_material)
      {
         v.r = (uint8_t)(((uint32_t)v.r * gfx_info.DMEM[(csrs++)^3]) >> 8);
         v.g = (uint8_t)(((uint32_t)v.g * gfx_info.DMEM[(csrs++)^3]) >> 8);
         v.b = (uint8_t)(((uint32_t)v.b * gfx_info.DMEM[(csrs++)^3]) >> 8);
         v.a = gfx_info.DMEM[(csrs++)^3];
      }
      gfx_info.DMEM[(cdest++)^3] = v.r;
      gfx_info.DMEM[(cdest++)^3] = v.g;
      gfx_info.DMEM[(cdest++)^3] = v.b;
      gfx_info.DMEM[(cdest++)^3] = v.a;
      ((int16_t*)gfx_info.DMEM)[(tdest++)^1] = (int16_t)v.ou;
      ((int16_t*)gfx_info.DMEM)[(tdest++)^1] = (int16_t)v.ov;
   }
}

static void uc9_mtxtrnsp(uint32_t w0, uint32_t w1)
{
   /*
   M44 *s;
   switch (w1 & 0xF)
   {
      case 4:
         s = (M44*)rdp.model;
         LRDP("Model\n");
         break;
      case 6:
         s = (M44*)rdp.proj;
         LRDP("Proj\n");
         break;
      case 8:
         s = (M44*)rdp.combined;
         LRDP("Comb\n");
         break;
    }
    float m = *s[1][0];
    *s[1][0] = *s[0][1];
    *s[0][1] = m;
    m = *s[2][0];
    *s[2][0] = *s[0][2];
    *s[0][2] = m;
    m = *s[2][1];
    *s[2][1] = *s[1][2];
    *s[1][2] = m;
    */
}

static void uc9_mtxcat(uint32_t w0, uint32_t w1)
{
   M44 *s;
   M44 *t;
   DECLAREALIGN16VAR(m[4][4]);
   uint32_t S = w0 & 0xF;
   uint32_t T = (w1 >> 16) & 0xF;
   uint32_t D = w1 & 0xF;

   switch (S)
   {
      case 4:
         s = (M44*)rdp.model;
         LRDP("Model * ");
         break;
      case 6:
         s = (M44*)rdp.proj;
         LRDP("Proj * ");
         break;
      case 8:
         s = (M44*)rdp.combined;
         LRDP("Comb * ");
         break;
   }

   switch (T)
   {
      case 4:
         t = (M44*)rdp.model;
         LRDP("Model -> ");
         break;
      case 6:
         t = (M44*)rdp.proj;
         LRDP("Proj -> ");
         break;
      case 8:
         LRDP("Comb -> ");
         t = (M44*)rdp.combined;
         break;
   }
   MulMatrices(*s, *t, m);

   switch (D)
   {
      case 4:
         memcpy (rdp.model, m, 64);;
         LRDP("Model\n");
         break;
      case 6:
         memcpy (rdp.proj, m, 64);;
         LRDP("Proj\n");
         break;
      case 8:
         memcpy (rdp.combined, m, 64);;
         LRDP("Comb\n");
         break;
   }

#ifdef EXTREME_LOGGING
   FRDP ("\nmodel\n{%f,%f,%f,%f}\n", rdp.model[0][0], rdp.model[0][1], rdp.model[0][2], rdp.model[0][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.model[1][0], rdp.model[1][1], rdp.model[1][2], rdp.model[1][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.model[2][0], rdp.model[2][1], rdp.model[2][2], rdp.model[2][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.model[3][0], rdp.model[3][1], rdp.model[3][2], rdp.model[3][3]);
   FRDP ("\nproj\n{%f,%f,%f,%f}\n", rdp.proj[0][0], rdp.proj[0][1], rdp.proj[0][2], rdp.proj[0][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.proj[1][0], rdp.proj[1][1], rdp.proj[1][2], rdp.proj[1][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.proj[2][0], rdp.proj[2][1], rdp.proj[2][2], rdp.proj[2][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.proj[3][0], rdp.proj[3][1], rdp.proj[3][2], rdp.proj[3][3]);
   FRDP ("\ncombined\n{%f,%f,%f,%f}\n", rdp.combined[0][0], rdp.combined[0][1], rdp.combined[0][2], rdp.combined[0][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.combined[1][0], rdp.combined[1][1], rdp.combined[1][2], rdp.combined[1][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.combined[2][0], rdp.combined[2][1], rdp.combined[2][2], rdp.combined[2][3]);
   FRDP ("{%f,%f,%f,%f}\n", rdp.combined[3][0], rdp.combined[3][1], rdp.combined[3][2], rdp.combined[3][3]);
#endif
}

typedef struct
{
   int16_t sy;
   int16_t sx;
   int   invw;
   int16_t yi;
   int16_t xi;
   int16_t wi;
   uint8_t fog;
   uint8_t cc;
} zSortVDest;

static void uc9_mult_mpmtx(uint32_t w0, uint32_t w1)
{
   int i, idx, num, src, dst;
   int16_t *saddr;
   zSortVDest v, *daddr;
   num = 1+ ((w1 >> 24) & 0xFF);
   src = -1024 + ((w1 >> 12) & 0xFFF);
   dst = -1024 + (w1 & 0xFFF);
   FRDP ("uc9:mult_mpmtx from: %04lx  to: %04lx n: %d\n", src, dst, num);
   saddr = (int16_t*)(gfx_info.DMEM+src);
   daddr = (zSortVDest*)(gfx_info.DMEM+dst);
   idx = 0;
   memset(&v, 0, sizeof(zSortVDest));
   //float scale_x = 4.0f/rdp.scale_x;
   //float scale_y = 4.0f/rdp.scale_y;
   for (i = 0; i < num; i++)
   {
      int16_t sx   = saddr[(idx++)^1];
      int16_t sy   = saddr[(idx++)^1];
      int16_t sz   = saddr[(idx++)^1];
      float x = sx*rdp.combined[0][0] + sy*rdp.combined[1][0] + sz*rdp.combined[2][0] + rdp.combined[3][0];
      float y = sx*rdp.combined[0][1] + sy*rdp.combined[1][1] + sz*rdp.combined[2][1] + rdp.combined[3][1];
      float z = sx*rdp.combined[0][2] + sy*rdp.combined[1][2] + sz*rdp.combined[2][2] + rdp.combined[3][2];
      float w = sx*rdp.combined[0][3] + sy*rdp.combined[1][3] + sz*rdp.combined[2][3] + rdp.combined[3][3];
      v.sx = (int16_t)(zSortRdp.view_trans[0] + x / w * zSortRdp.view_scale[0]);
      v.sy = (int16_t)(zSortRdp.view_trans[1] + y / w * zSortRdp.view_scale[1]);

      v.xi = (int16_t)x;
      v.yi = (int16_t)y;
      v.wi = (int16_t)w;
      v.invw = Calc_invw((int)(w * 31.0));

      if (w < 0.0f)
         v.fog = 0;
      else
      {
         int fog = (int)(z / w * rdp.fog_multiplier + rdp.fog_offset);
         if (fog > 255)
            fog = 255;
         v.fog = (fog >= 0) ? (uint8_t)fog : 0;
      }

      v.cc = 0;
      if (x < -w) v.cc |= 0x10;
      if (x > w) v.cc |= 0x01;
      if (y < -w) v.cc |= 0x20;
      if (y > w) v.cc |= 0x02;
      if (w < 0.1f) v.cc |= 0x04;

      daddr[i] = v;
      //memcpy(gfx_info.DMEM+dst+sizeof(zSortVDest)*i, &v, sizeof(zSortVDest));
      //    FRDP("v%d x: %d, y: %d, z: %d -> sx: %d, sy: %d, w: %d, xi: %d, yi: %d, wi: %d, fog: %d\n", i, sx, sy, sz, v.sx, v.sy, v.invw, v.xi, v.yi, v.wi, v.fog);
      FRDP("v%d x: %d, y: %d, z: %d -> sx: %04lx, sy: %04lx, invw: %08lx - %f, xi: %04lx, yi: %04lx, wi: %04lx, fog: %04lx\n", i, sx, sy, sz, v.sx, v.sy, v.invw, w, v.xi, v.yi, v.wi, v.fog);
   }
}

static void uc9_link_subdl(uint32_t w0, uint32_t w1)
{
}

static void uc9_set_subdl(uint32_t w0, uint32_t w1)
{
}

static void uc9_wait_signal(uint32_t w0, uint32_t w1)
{
}

static void uc9_send_signal(uint32_t w0, uint32_t w1)
{
}

static void uc9_movemem(uint32_t w0, uint32_t w1)
{
   int idx = w0 & 0x0E;
   int ofs = ((w0 >> 6) & 0x1ff)<<3;
   int len = (1 + ((w0 >> 15) & 0x1ff))<<3;
   int flag = w0 & 0x01;
   uint32_t addr = RSP_SegmentToPhysical(w1);

   switch (idx)
   {
      case 0: //save/load
         if (flag == 0)
         {
            int dmem_addr = (idx<<3) + ofs;
            FRDP ("Load to DMEM. %08lx -> %08lx\n", addr, dmem_addr);
            memcpy(gfx_info.DMEM + dmem_addr, gfx_info.RDRAM + addr, len);
         }
         else
         {
            int dmem_addr = (idx<<3) + ofs;
            FRDP ("Load from DMEM. %08lx -> %08lx\n", dmem_addr, addr);
            memcpy(gfx_info.RDRAM + addr, gfx_info.DMEM + dmem_addr, len);
         }
         break;

      case 4:  // model matrix
      case 6:  // projection matrix
      case 8:  // combined matrix
         {
            DECLAREALIGN16VAR(m[4][4]);
            load_matrix(m, addr);
            switch (idx)
            {
               case 4:  // model matrix
                  modelview_load (m);
                  break;
               case 6:  // projection matrix
                  projection_load (m);
                  break;
               case 8:  // projection matrix
                  LRDP("Combined load\n");
                  g_gdp.flags &= ~UPDATE_MULT_MAT;
                  memcpy (rdp.combined, m, 64);;
                  break;
            }
#ifdef EXTREME_LOGGING
            FRDP ("{%f,%f,%f,%f}\n", m[0][0], m[0][1], m[0][2], m[0][3]);
            FRDP ("{%f,%f,%f,%f}\n", m[1][0], m[1][1], m[1][2], m[1][3]);
            FRDP ("{%f,%f,%f,%f}\n", m[2][0], m[2][1], m[2][2], m[2][3]);
            FRDP ("{%f,%f,%f,%f}\n", m[3][0], m[3][1], m[3][2], m[3][3]);
            FRDP ("\nmodel\n{%f,%f,%f,%f}\n", rdp.model[0][0], rdp.model[0][1], rdp.model[0][2], rdp.model[0][3]);
            FRDP ("{%f,%f,%f,%f}\n", rdp.model[1][0], rdp.model[1][1], rdp.model[1][2], rdp.model[1][3]);
            FRDP ("{%f,%f,%f,%f}\n", rdp.model[2][0], rdp.model[2][1], rdp.model[2][2], rdp.model[2][3]);
            FRDP ("{%f,%f,%f,%f}\n", rdp.model[3][0], rdp.model[3][1], rdp.model[3][2], rdp.model[3][3]);
            FRDP ("\nproj\n{%f,%f,%f,%f}\n", rdp.proj[0][0], rdp.proj[0][1], rdp.proj[0][2], rdp.proj[0][3]);
            FRDP ("{%f,%f,%f,%f}\n", rdp.proj[1][0], rdp.proj[1][1], rdp.proj[1][2], rdp.proj[1][3]);
            FRDP ("{%f,%f,%f,%f}\n", rdp.proj[2][0], rdp.proj[2][1], rdp.proj[2][2], rdp.proj[2][3]);
            FRDP ("{%f,%f,%f,%f}\n", rdp.proj[3][0], rdp.proj[3][1], rdp.proj[3][2], rdp.proj[3][3]);
#endif
         }
         break;

      case 10:
         LRDP("Othermode - IGNORED\n");
         break;

      case 12:   // VIEWPORT
         {
            int16_t scale_x, scale_y, scale_z, trans_x, trans_y, trans_z;
			uint32_t a;
			TILE *tmp_tile;

            a = addr >> 1;
            scale_x = ((int16_t*)gfx_info.RDRAM)[(a+0)^1] >> 2;
            scale_y = ((int16_t*)gfx_info.RDRAM)[(a+1)^1] >> 2;
            scale_z = ((int16_t*)gfx_info.RDRAM)[(a+2)^1];
            rdp.fog_multiplier = ((int16_t*)gfx_info.RDRAM)[(a+3)^1];
            trans_x = ((int16_t*)gfx_info.RDRAM)[(a+4)^1] >> 2;
            trans_y = ((int16_t*)gfx_info.RDRAM)[(a+5)^1] >> 2;
            trans_z = ((int16_t*)gfx_info.RDRAM)[(a+6)^1];
            rdp.fog_offset = ((int16_t*)gfx_info.RDRAM)[(a+7)^1];
            rdp.view_scale[0] = scale_x * rdp.scale_x;
            rdp.view_scale[1] = scale_y * rdp.scale_y;
            rdp.view_scale[2] = 32.0f * scale_z;
            rdp.view_trans[0] = trans_x * rdp.scale_x;
            rdp.view_trans[1] = trans_y * rdp.scale_y;
            rdp.view_trans[2] = 32.0f * trans_z;
            zSortRdp.view_scale[0] = (float)(scale_x*4);
            zSortRdp.view_scale[1] = (float)(scale_y*4);
            zSortRdp.view_trans[0] = (float)(trans_x*4);
            zSortRdp.view_trans[1] = (float)(trans_y*4);
            zSortRdp.scale_x = rdp.scale_x / 4.0f;
            zSortRdp.scale_y = rdp.scale_y / 4.0f;

            g_gdp.flags |= UPDATE_VIEWPORT;

            rdp.mipmap_level = 0;
            rdp.cur_tile = 0;

            tmp_tile = (TILE*)&rdp.tiles[0];
            tmp_tile->on = 1;
            tmp_tile->org_s_scale = 0xFFFF;
            tmp_tile->org_t_scale = 0xFFFF;
            tmp_tile->s_scale = 0.031250f;
            tmp_tile->t_scale = 0.031250f;

            rdp.geom_mode |= 0x0200;

            FRDP ("viewport scale(%d, %d, %d), trans(%d, %d, %d), from:%08lx\n", scale_x, scale_y, scale_z,
                  trans_x, trans_y, trans_z, a);
            FRDP ("fog: multiplier: %f, offset: %f\n", rdp.fog_multiplier, rdp.fog_offset);
         }
         break;

      default:
         FRDP ("** UNKNOWN %d\n", idx);
   }
}

static void uc9_setscissor(uint32_t w0, uint32_t w1)
{
   rdp_setscissor(w0, w1);

   if ((g_gdp.__clip.xl - g_gdp.__clip.xh) > (zSortRdp.view_scale[0] - zSortRdp.view_trans[0]))
   {
      TILE *tmp_tile;
      float w = (g_gdp.__clip.xl - g_gdp.__clip.xh) / 2.0f;
      float h = (g_gdp.__clip.yl - g_gdp.__clip.yh) / 2.0f;
      rdp.view_scale[0] = w * rdp.scale_x;
      rdp.view_scale[1] = h * rdp.scale_y;
      rdp.view_trans[0] = w * rdp.scale_x;
      rdp.view_trans[1] = h * rdp.scale_y;
      zSortRdp.view_scale[0] = w * 4.0f;
      zSortRdp.view_scale[1] = h * 4.0f;
      zSortRdp.view_trans[0] = w * 4.0f;
      zSortRdp.view_trans[1] = h * 4.0f;
      zSortRdp.scale_x = rdp.scale_x / 4.0f;
      zSortRdp.scale_y = rdp.scale_y / 4.0f;
      g_gdp.flags |= UPDATE_VIEWPORT;

      rdp.mipmap_level = 0;
      rdp.cur_tile = 0;

      tmp_tile = (TILE*)&rdp.tiles[0];
      tmp_tile->on = 1;
      tmp_tile->org_s_scale = 0xFFFF;
      tmp_tile->org_t_scale = 0xFFFF;
      tmp_tile->s_scale = 0.031250f;
      tmp_tile->t_scale = 0.031250f;

      rdp.geom_mode |= 0x0200;
   }
}
