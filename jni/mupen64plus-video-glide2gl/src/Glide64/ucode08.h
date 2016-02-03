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
// January 2004 Created by Gonetz (Gonetz@ngs.ru)
//
//****************************************************************

uint32_t uc8_normale_addr = 0;
float uc8_coord_mod[16];

static void uc8_vertex(uint32_t w0, uint32_t w1)
{
   uint32_t i;
   uint32_t addr = RSP_SegmentToPhysical(w1);
   int32_t n = (w0 >> 12) & 0xFF;
   int32_t v0 = ((w0 >> 1) & 0x7F) - n;
   void   *membase_ptr  = (void*)(gfx_info.RDRAM + addr);
   uint32_t iter = 16;

   if (v0 < 0)
      return;

   pre_update();

   for (i=0; i < (n * iter); i+= iter)
   {
      VERTEX *vert = (VERTEX*)&rdp.vtx[v0 + (i / iter)];
      int16_t *rdram    = (int16_t*)membase_ptr;
      uint8_t *rdram_u8 = (uint8_t*)membase_ptr;
      uint8_t *color = (uint8_t*)(rdram_u8 + 12);

      float x                 = (float)rdram[1];
      float y                 = (float)rdram[0];
      float z                 = (float)rdram[3];

      vert->flags       = (uint16_t)rdram[2];
      vert->ov          = (float)rdram[4];
      vert->ou          = (float)rdram[5];
      vert->uv_scaled   = 0;
      vert->a           = color[0];

      vert->x = x*rdp.combined[0][0] + y*rdp.combined[1][0] + z*rdp.combined[2][0] + rdp.combined[3][0];
      vert->y = x*rdp.combined[0][1] + y*rdp.combined[1][1] + z*rdp.combined[2][1] + rdp.combined[3][1];
      vert->z = x*rdp.combined[0][2] + y*rdp.combined[1][2] + z*rdp.combined[2][2] + rdp.combined[3][2];
      vert->w = x*rdp.combined[0][3] + y*rdp.combined[1][3] + z*rdp.combined[2][3] + rdp.combined[3][3];

      vert->uv_calculated = 0xFFFFFFFF;
      vert->screen_translated = 0;
      vert->shade_mod = 0;

      if (fabs(vert->w) < 0.001)
         vert->w = 0.001f;
      vert->oow = 1.0f / vert->w;
      vert->x_w = vert->x * vert->oow;
      vert->y_w = vert->y * vert->oow;
      vert->z_w = vert->z * vert->oow;

      vert->scr_off = 0;
      if (vert->x < -vert->w)
         vert->scr_off |= 1;
      if (vert->x > vert->w)
         vert->scr_off |= 2;
      if (vert->y < -vert->w)
         vert->scr_off |= 4;
      if (vert->y > vert->w)
         vert->scr_off |= 8;
      if (vert->w < 0.1f)
         vert->scr_off |= 16;

      vert->r = color[3];
      vert->g = color[2];
      vert->b = color[1];

      if ((rdp.geom_mode & G_LIGHTING))
      {
         uint32_t shift, l;
         float light_intensity, color[3];

         shift = v0 << 1;
         vert->vec[0] = ((int8_t*)gfx_info.RDRAM)[(uc8_normale_addr + (i>>3) + shift + 0)^3];
         vert->vec[1] = ((int8_t*)gfx_info.RDRAM)[(uc8_normale_addr + (i>>3) + shift + 1)^3];
         vert->vec[2] = (int8_t)(vert->flags & 0xff);

         if (rdp.geom_mode & G_TEXTURE_GEN_LINEAR)
            calc_linear (vert);
         else if (rdp.geom_mode & G_TEXTURE_GEN)
            calc_sphere (vert);

         color[0] = rdp.light[rdp.num_lights].col[0];
         color[1] = rdp.light[rdp.num_lights].col[1];
         color[2] = rdp.light[rdp.num_lights].col[2];

         light_intensity = 0.0f;
         if (rdp.geom_mode & 0x00400000)
         {
            NormalizeVector (vert->vec);
            for (l = 0; l < rdp.num_lights-1; l++)
            {
               if (!rdp.light[l].nonblack)
                  continue;
               light_intensity = DotProduct (rdp.light_vector[l], vert->vec);
               FRDP("light %d, intensity : %f\n", l, light_intensity);
               if (light_intensity < 0.0f)
                  continue;
               //*
               if (rdp.light[l].ca > 0.0f)
               {
                  float vx = (vert->x + uc8_coord_mod[8])*uc8_coord_mod[12] - rdp.light[l].x;
                  float vy = (vert->y + uc8_coord_mod[9])*uc8_coord_mod[13] - rdp.light[l].y;
                  float vz = (vert->z + uc8_coord_mod[10])*uc8_coord_mod[14] - rdp.light[l].z;
                  float vw = (vert->w + uc8_coord_mod[11])*uc8_coord_mod[15] - rdp.light[l].w;
                  float len = (vx*vx+vy*vy+vz*vz+vw*vw)/65536.0f;
                  float p_i = rdp.light[l].ca / len;
                  if (p_i > 1.0f) p_i = 1.0f;
                  light_intensity *= p_i;
               }
               //*/
               color[0] += rdp.light[l].col[0] * light_intensity;
               color[1] += rdp.light[l].col[1] * light_intensity;
               color[2] += rdp.light[l].col[2] * light_intensity;
            }
            light_intensity = DotProduct (rdp.light_vector[l], vert->vec);
            if (light_intensity > 0.0f)
            {
               color[0] += rdp.light[l].col[0] * light_intensity;
               color[1] += rdp.light[l].col[1] * light_intensity;
               color[2] += rdp.light[l].col[2] * light_intensity;
            }
         }
         else
         {
            for (l = 0; l < rdp.num_lights; l++)
            {
               if (rdp.light[l].nonblack && rdp.light[l].nonzero)
               {
                  float vx = (vert->x + uc8_coord_mod[8])*uc8_coord_mod[12] - rdp.light[l].x;
                  float vy = (vert->y + uc8_coord_mod[9])*uc8_coord_mod[13] - rdp.light[l].y;
                  float vz = (vert->z + uc8_coord_mod[10])*uc8_coord_mod[14] - rdp.light[l].z;
                  float vw = (vert->w + uc8_coord_mod[11])*uc8_coord_mod[15] - rdp.light[l].w;
                  float len = (vx*vx+vy*vy+vz*vz+vw*vw)/65536.0f;
                  light_intensity = rdp.light[l].ca / len;
                  if (light_intensity > 1.0f) light_intensity = 1.0f;
                  color[0] += rdp.light[l].col[0] * light_intensity;
                  color[1] += rdp.light[l].col[1] * light_intensity;
                  color[2] += rdp.light[l].col[2] * light_intensity;
               }
            }
         }
         if (color[0] > 1.0f) color[0] = 1.0f;
         if (color[1] > 1.0f) color[1] = 1.0f;
         if (color[2] > 1.0f) color[2] = 1.0f;
         vert->r = (uint8_t)(((float)vert->r)*color[0]);
         vert->g = (uint8_t)(((float)vert->g)*color[1]);
         vert->b = (uint8_t)(((float)vert->b)*color[2]);
      }
      membase_ptr = (char*)membase_ptr + iter;
   }
}

static void uc8_moveword(uint32_t w0, uint32_t w1)
{
   int k;
   uint8_t index = (uint8_t)((w0 >> 16) & 0xFF);
   uint16_t offset = (uint16_t)(w0 & 0xFFFF);

   switch (index)
   {
      // NOTE: right now it's assuming that it sets the integer part first.  This could
      //  be easily fixed, but only if i had something to test with.

      case G_MW_NUMLIGHT:
         gSPNumLights_G64(w1 / 48);
         break;

      case G_MW_CLIP:
         if (offset == 0x04)
         {
            rdp.clip_ratio = (float)vi_integer_sqrt(w1);
            g_gdp.flags |= UPDATE_VIEWPORT;
         }
         break;

      case G_MW_SEGMENT:
         rdp.segment[(offset >> 2) & 0xF] = w1;
         break;

      case G_MW_FOG:
         gSPFogFactor_G64((int16_t)_SHIFTR(w1, 16, 16), (int16_t)_SHIFTR(w1, 0, 16));
         break;

      case G_MW_PERSPNORM:
         break;

      case G_MV_COORDMOD:  // moveword coord mod
         {
            uint32_t idx, pos;
            uint8_t n;
			n = offset >> 2;

            FRDP ("coord mod:%d, %08lx\n", n, w1);
            if (w0 & 8)
               return;
            idx = (w0 >> 1)&3;
            pos = w0 & 0x30;
            if (pos == 0)
            {
               uc8_coord_mod[0+idx] = (int16_t)(w1 >> 16);
               uc8_coord_mod[1+idx] = (int16_t)(w1 & 0xffff);
            }
            else if (pos == 0x10)
            {
               uc8_coord_mod[4+idx] = (w1 >> 16) / 65536.0f;
               uc8_coord_mod[5+idx] = (w1 & 0xffff) / 65536.0f;
               uc8_coord_mod[12+idx] = uc8_coord_mod[0+idx] + uc8_coord_mod[4+idx];
               uc8_coord_mod[13+idx] = uc8_coord_mod[1+idx] + uc8_coord_mod[5+idx];

            }
            else if (pos == 0x20)
            {
               uc8_coord_mod[8+idx] = (int16_t)(w1 >> 16);
               uc8_coord_mod[9+idx] = (int16_t)(w1 & 0xffff);
            }

         }
         break;
   }
}

static void uc8_movemem(uint32_t w0, uint32_t w1)
{
   int i, t;
   uint32_t addr = RSP_SegmentToPhysical(w1);
   int ofs = _SHIFTR(w0, 5, 14);

   switch (_SHIFTR(w0, 0, 8))
   {
      case F3DCBFD_MV_VIEWPORT:
         gSPViewport_G64(w1);
         break;

      case F3DCBFD_MV_LIGHT:  // LIGHT
         {
            uint32_t a;
            int n = (ofs / 48);

            if (n < 2)
               gSPLookAt_G64(w1, n);
            else
            {
               n -= 2;
               rdp.light[n].nonblack = gfx_info.RDRAM[(addr+0)^3];
               rdp.light[n].nonblack += gfx_info.RDRAM[(addr+1)^3];
               rdp.light[n].nonblack += gfx_info.RDRAM[(addr+2)^3];

               rdp.light[n].col[0] = (((uint8_t*)gfx_info.RDRAM)[(addr+0)^3]) / 255.0f;
               rdp.light[n].col[1] = (((uint8_t*)gfx_info.RDRAM)[(addr+1)^3]) / 255.0f;
               rdp.light[n].col[2] = (((uint8_t*)gfx_info.RDRAM)[(addr+2)^3]) / 255.0f;
               rdp.light[n].col[3] = 1.0f;

               // ** Thanks to Icepir8 for pointing this out **
               // Lighting must be signed byte instead of byte
               rdp.light[n].dir[0] = (float)(((int8_t*)gfx_info.RDRAM)[(addr+8)^3]) / 127.0f;
               rdp.light[n].dir[1] = (float)(((int8_t*)gfx_info.RDRAM)[(addr+9)^3]) / 127.0f;
               rdp.light[n].dir[2] = (float)(((int8_t*)gfx_info.RDRAM)[(addr+10)^3]) / 127.0f;
               // **
               a = addr >> 1;
               rdp.light[n].x = (float)(((int16_t*)gfx_info.RDRAM)[(a+16)^1]);
               rdp.light[n].y = (float)(((int16_t*)gfx_info.RDRAM)[(a+17)^1]);
               rdp.light[n].z = (float)(((int16_t*)gfx_info.RDRAM)[(a+18)^1]);
               rdp.light[n].w = (float)(((int16_t*)gfx_info.RDRAM)[(a+19)^1]);
               rdp.light[n].nonzero = gfx_info.RDRAM[(addr+12)^3];
               rdp.light[n].ca = (float)rdp.light[n].nonzero / 16.0f;
               //rdp.light[n].la = rdp.light[n].ca * 1.0f;
            }
         }
         break;

      case F3DCBFD_MV_NORMAL: //Normals
         uc8_normale_addr = RSP_SegmentToPhysical(w1);
         break;
   }
}

static void uc8_tri4(uint32_t w0, uint32_t w1) //by Gugaman Apr 19 2002
{
   VERTEX *v[12];

   if (rdp.skip_drawing)
      return;

   v[0]  = &rdp.vtx[_SHIFTR( w0, 23, 5)]; /* v00 */
   v[1]  = &rdp.vtx[_SHIFTR( w0, 18, 5)]; /* v01 */
   v[2]  = &rdp.vtx[(_SHIFTR(w0, 15, 3) << 2) | _SHIFTR(w1, 30, 2)]; /* v02 */
   v[3]  = &rdp.vtx[_SHIFTR( w0, 10, 5)]; /* v10 */
   v[4]  = &rdp.vtx[_SHIFTR( w0,  5, 5)];  /* v11 */
   v[5]  = &rdp.vtx[_SHIFTR( w0,  0, 5)];  /* v12 */
   v[6]  = &rdp.vtx[_SHIFTR( w1, 25, 5)]; /* v20 */
   v[7]  = &rdp.vtx[_SHIFTR( w1, 20, 5)]; /* v21 */
   v[8]  = &rdp.vtx[_SHIFTR( w1, 15, 5)]; /* v22 */
   v[9]  = &rdp.vtx[_SHIFTR( w1, 10, 5)]; /* v30 */
   v[10] = &rdp.vtx[_SHIFTR( w1,  5, 5)];  /* v31 */
   v[11] = &rdp.vtx[_SHIFTR( w1,  0, 5)];  /* v32 */

   cull_trianglefaces(v, 4, true, true, 0);
}
