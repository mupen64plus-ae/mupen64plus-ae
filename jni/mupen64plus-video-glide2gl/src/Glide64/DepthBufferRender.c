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
// Software rendering into N64 depth buffer
// Idea and N64 depth value format by Orkin
// Polygon rasterization algorithm is taken from FATMAP2 engine by Mats Byggmastar, mri@penti.sit.fi
//
// Created by Gonetz, Dec 2004
//
//****************************************************************

#include "Gfx_1.3.h"
#include "rdp.h"
#include "DepthBufferRender.h"

uint16_t *zLUT;

#define ZLUT_SIZE 0x40000

void ZLUT_init(void)
{
   int i;
   if (zLUT)
      return;

   zLUT = (uint16_t*)malloc(ZLUT_SIZE * sizeof(uint16_t));

   for(i = 0; i< ZLUT_SIZE; i++)
   {
      uint32_t exponent, testbit, mantissa;
      exponent = 0;
      testbit = 1 << 17;
      while((i & testbit) && (exponent < 7))
      {
         exponent++;
         testbit = 1 << (17 - exponent);
      }

      mantissa = (i >> (6 - (6 < exponent ? 6 : exponent))) & 0x7ff;
      zLUT[i] = (uint16_t)(((exponent << 11) | mantissa) << 2);
   }
}

void ZLUT_release(void)
{
   if (zLUT)
      free(zLUT);
   zLUT = 0;
}

static struct vertexi *max_vtx;                   // Max y vertex (ending vertex)
static struct vertexi *start_vtx, *end_vtx;      // First and last vertex in array
static struct vertexi *right_vtx, *left_vtx;     // Current right and left vertex

static int right_height, left_height;
static int right_x, right_dxdy, left_x, left_dxdy;
static int left_z, left_dzdy;

// (x * y) >> 16
#define imul16(x, y) ((((long long)x) * ((long long)y)) >> 16)

// (x * y) >> 14
#define imul14(x, y) ((((long long)x) * ((long long)y)) >> 14)

static INLINE int idiv16(int x, int y)
{
   const int64_t m = (int64_t)(x);
   const int64_t n = (int64_t)(y);
   int64_t result = (m << 16) / n;

   return (int)(result);
}

static INLINE int iceil(int x)
{
   return ((x + 0xffff)  >> 16);
}

static void RightSection(void)
{
   int prestep;
   // Walk backwards trough the vertex array
   struct vertexi *v1 = (struct vertexi*)right_vtx;
   struct vertexi *v2 = end_vtx;         // Wrap to end of array

   if(right_vtx > start_vtx)
      v2 = right_vtx-1;     

   right_vtx = v2;

   // v1 = top vertex
   // v2 = bottom vertex 

   // Calculate number of scanlines in this section

   right_height = iceil(v2->y) - iceil(v1->y);
   if(right_height <= 0)
      return;

   // Guard against possible div overflows

   if(right_height > 1)
   {
      // OK, no worries, we have a section that is at least
      // one pixel high. Calculate slope as usual.

      int height = v2->y - v1->y;
      right_dxdy  = idiv16(v2->x - v1->x, height);
   }
   else
   {
      // Height is less or equal to one pixel.
      // Calculate slope = width * 1/height
      // using 18:14 bit precision to avoid overflows.

      int inv_height = (0x10000 << 14) / (v2->y - v1->y);  
      right_dxdy = imul14(v2->x - v1->x, inv_height);
   }

   // Prestep initial values

   prestep = (iceil(v1->y) << 16) - v1->y;
   right_x = v1->x + imul16(prestep, right_dxdy);
}

static void LeftSection(void)
{
   int prestep;
   // Walk forward through the vertex array
   struct vertexi *v1 = (struct vertexi*)left_vtx;
   struct vertexi *v2 = start_vtx;      // Wrap to start of array

   if(left_vtx < end_vtx)
      v2 = left_vtx+1;
   left_vtx = v2;

   // v1 = top vertex
   // v2 = bottom vertex 

   // Calculate number of scanlines in this section

   left_height = iceil(v2->y) - iceil(v1->y);

   if(left_height <= 0)
      return;

   // Guard against possible div overflows

   if(left_height > 1)
   {
      // OK, no worries, we have a section that is at least
      // one pixel high. Calculate slope as usual.

      int height = v2->y - v1->y;
      left_dxdy = idiv16(v2->x - v1->x, height);
      left_dzdy = idiv16(v2->z - v1->z, height);
   }
   else
   {
      // Height is less or equal to one pixel.
      // Calculate slope = width * 1/height
      // using 18:14 bit precision to avoid overflows.

      int inv_height = (0x10000 << 14) / (v2->y - v1->y);
      left_dxdy = imul14(v2->x - v1->x, inv_height);
      left_dzdy = imul14(v2->z - v1->z, inv_height);
   }

   // Prestep initial values

   prestep = (iceil(v1->y) << 16) - v1->y;
   left_x = v1->x + imul16(prestep, left_dxdy);
   left_z = v1->z + imul16(prestep, left_dzdy);
}


void Rasterize(struct vertexi * vtx, int vertices, int dzdx)
{
   int n, min_y, max_y, y1, shift;
   uint16_t *destptr;
   struct vertexi *min_vtx;
   start_vtx = vtx;        // First vertex in array

   // Search trough the vtx array to find min y, max y
   // and the location of these structures.

   min_vtx = (struct vertexi*)vtx;
   max_vtx = vtx;

   min_y = vtx->y;
   max_y = vtx->y;

   vtx++;

   for (n = 1; n < vertices; n++)
   {
      if(vtx->y < min_y)
      {
         min_y = vtx->y;
         min_vtx = vtx;
      }
      else if(vtx->y > max_y)
      {
         max_y = vtx->y;
         max_vtx = vtx;
      }
      vtx++;
   }

   // OK, now we know where in the array we should start and
   // where to end while scanning the edges of the polygon

   left_vtx  = min_vtx;    // Left side starting vertex
   right_vtx = min_vtx;    // Right side starting vertex
   end_vtx   = vtx-1;      // Last vertex in array

   // Search for the first usable right section

   do {
      if(right_vtx == max_vtx)
         return;
      RightSection();
   } while(right_height <= 0);

   // Search for the first usable left section

   do {
      if(left_vtx == max_vtx)
         return;
      LeftSection();
   } while(left_height <= 0);

   destptr = (uint16_t*)(gfx_info.RDRAM + g_gdp.zb_address);
   y1      = iceil(min_y);

   if (y1 >= g_gdp.__clip.yl)
      return;

   for(;;)
   {
      int width;
      int x1 = iceil(left_x);
      if (x1 < g_gdp.__clip.xh)
         x1 = g_gdp.__clip.xh;
      width = iceil(right_x) - x1;
      if (x1+width >= g_gdp.__clip.xl)
         width = g_gdp.__clip.xl - x1 - 1;

      if(width > 0 && y1 >= g_gdp.__clip.yh)
      {
         unsigned x;
         // Prestep initial z
         int prestep = (x1 << 16) - left_x;
         int      z = left_z + imul16(prestep, dzdx);

         shift = x1 + y1 * rdp.zi_width;

         //draw to depth buffer
         for (x = 0; x < width; x++)
         {
            int idx;
            uint16_t encodedZ;
            int trueZ = z / 8192;
            if (trueZ < 0)
               trueZ = 0;
            else if (trueZ > 0x3FFFF)
               trueZ = 0x3FFFF;
            encodedZ = zLUT[trueZ];
            idx = (shift+x)^1;
            if(encodedZ < destptr[idx]) 
               destptr[idx] = encodedZ;
            z += dzdx;
         }
      }

      y1++;
      if (y1 >= g_gdp.__clip.yl)
         return;

      // Scan the right side

      if(--right_height <= 0) // End of this section?
      {
         do
         {
            if(right_vtx == max_vtx)
               return;
            RightSection();
         } while(right_height <= 0);
      }
      else 
         right_x += right_dxdy;

      // Scan the left side

      if(--left_height <= 0) // End of this section ?
      {
         do
         {
            if(left_vtx == max_vtx)
               return;
            LeftSection();
         } while(left_height <= 0);
      }
      else
      {
         left_x += left_dxdy;
         left_z += left_dzdy;
      }
   }
}
