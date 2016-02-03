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

#include "Gfx_1.3.h"
#include "../../libretro/SDL.h"

#include <math.h>
#include "3dmath.h"

float DotProductC(float *v0, float *v1)
{
    return v0[0] * v1[0] + v0[1] * v1[1] + v0[2] * v1[2];
}

void NormalizeVectorC(float *v)
{
   float len = v[0]*v[0] + v[1]*v[1] + v[2]*v[2];
   if (len == 0.0f)
      return;
   len = sqrtf( len );
   v[0] /= len;
   v[1] /= len;
   v[2] /= len;
}


static void TransformVectorC(float *src, float *dst, float mat[4][4])
{
   dst[0] = mat[0][0]*src[0] + mat[1][0]*src[1] + mat[2][0]*src[2];
   dst[1] = mat[0][1]*src[0] + mat[1][1]*src[1] + mat[2][1]*src[2];
   dst[2] = mat[0][2]*src[0] + mat[1][2]*src[1] + mat[2][2]*src[2];
}

void InverseTransformVectorC (float *src, float *dst, float mat[4][4])
{
   dst[0] = mat[0][0]*src[0] + mat[0][1]*src[1] + mat[0][2]*src[2];
   dst[1] = mat[1][0]*src[0] + mat[1][1]*src[1] + mat[1][2]*src[2];
   dst[2] = mat[2][0]*src[0] + mat[2][1]*src[1] + mat[2][2]*src[2];
}

void MulMatricesC(float m1[4][4], float m2[4][4], float r[4][4])
{
    float row[4][4];
    register unsigned int i, j;

    for (i = 0; i < 4; i++)
        for (j = 0; j < 4; j++)
            row[i][j] = m2[i][j];
    for (i = 0; i < 4; i++)
    {
        float summand[4][4];

        for (j = 0; j < 4; j++)
        {
            summand[0][j] = m1[i][0] * row[0][j];
            summand[1][j] = m1[i][1] * row[1][j];
            summand[2][j] = m1[i][2] * row[2][j];
            summand[3][j] = m1[i][3] * row[3][j];
        }
        for (j = 0; j < 4; j++)
            r[i][j] =
                summand[0][j]
              + summand[1][j]
              + summand[2][j]
              + summand[3][j]
        ;
    }
}

// 2011-01-03 Balrog - removed because is in NASM format and not 64-bit compatible
// This will need fixing.
GLIDE64MULMATRIX glide64MulMatrices = MulMatricesC;
GLIDE64TRANSFORMVECTOR glide64InverseTransformVector = InverseTransformVectorC;
GLIDE64DOTPRODUCT glide64DotProduct = DotProductC;
GLIDE64NORMALIZEVECTOR glide64NormalizeVector = NormalizeVectorC;

// 2008.03.29 H.Morii - added SSE 3DNOW! 3x3 1x3 matrix multiplication
//                      and 3DNOW! 4x4 4x4 matrix multiplication

#if defined(__ARM_NEON__)
static void NormalizeVectorNeon(float *v)
{
   asm volatile (
         "vld1.32             {d4}, [%0]!    \n\t" //d4={x,y}
         "flds                s10, [%0]      \n\t" //d5[0] = z
         "sub                 %0, %0, #8     \n\t" //d5[0] = z
         "vmul.f32            d0, d4, d4     \n\t" //d0= d4*d4
         "vpadd.f32           d0, d0, d0     \n\t" //d0 = d[0] + d[1]
         "vmla.f32            d0, d5, d5     \n\t" //d0 = d0 + d5*d5

         "vmov.f32            d1, d0         \n\t" //d1 = d0
         "vrsqrte.f32         d0, d0         \n\t" //d0 = ~ 1.0 / sqrt(d0)
         "vmul.f32            d2, d0, d1     \n\t" //d2 = d0 * d1
         "vrsqrts.f32         d3, d2, d0     \n\t" //d3 = (3 - d0 * d2) / 2
         "vmul.f32            d0, d0, d3     \n\t" //d0 = d0 * d3
         "vmul.f32            d2, d0, d1     \n\t" //d2 = d0 * d1
         "vrsqrts.f32         d3, d2, d0     \n\t" //d3 = (3 - d0 * d3) / 2
         "vmul.f32            d0, d0, d3     \n\t" //d0 = d0 * d4

         "vmul.f32            q2, q2, d0[0]  \n\t" //d0= d2*d4
         "vst1.32             {d4}, [%0]!    \n\t" //d2={x0,y0}, d3={z0, w0}
         "fsts                s10, [%0]      \n\t" //d2={x0,y0}, d3={z0, w0}

:"+r"(v) :
   : "d0", "d1", "d2", "d3", "d4", "d5", "memory"
      );
}

static float DotProductNeon(float *v0, float *v1)
{
   float dot;
   __asm(
         "vld1.32       {d8}, [%1]!    \n\t" //d8={x0,y0}
         "vld1.32       {d10}, [%2]!   \n\t" //d10={x1,y1}
         "flds          s18, [%1, #0]  \n\t" //d9[0]={z0}
         "flds          s22, [%2, #0]  \n\t" //d11[0]={z1}
         "vmul.f32      d12, d8, d10   \n\t" //d0= d2*d4
         "vpadd.f32     d12, d12, d12  \n\t" //d0 = d[0] + d[1]
         "vmla.f32      d12, d9, d11   \n\t" //d0 = d0 + d3*d5
         "fmrs          %0, s24        \n\t" //r0 = s0
         : "=r"(dot), "+r"(v0), "+r"(v1):
         : "d8", "d9", "d10", "d11", "d12"

        );
   return dot;
}

void MulMatricesNeon(float m0[4][4],float m1[4][4],float dest[4][4])
{
     asm volatile (
        "vld1.32                 {d0, d1}, [%1]!          \n\t"        //q0 = m1
        "vld1.32                 {d2, d3}, [%1]!          \n\t"        //q1 = m1+4
        "vld1.32                 {d4, d5}, [%1]!          \n\t"        //q2 = m1+8
        "vld1.32                 {d6, d7}, [%1]           \n\t"        //q3 = m1+12
        "vld1.32                 {d16, d17}, [%0]!        \n\t"        //q8 = m0
        "vld1.32                 {d18, d19}, [%0]!        \n\t"        //q9 = m0+4
        "vld1.32                 {d20, d21}, [%0]!        \n\t"        //q10 = m0+8
        "vld1.32                 {d22, d23}, [%0]         \n\t"        //q11 = m0+12

        "vmul.f32                 q12, q8, d0[0]          \n\t"        //q12 = q8 * d0[0]
        "vmul.f32                 q13, q8, d2[0]          \n\t"        //q13 = q8 * d2[0]
        "vmul.f32                 q14, q8, d4[0]          \n\t"        //q14 = q8 * d4[0]
        "vmul.f32                 q15, q8, d6[0]          \n\t"        //q15 = q8 * d6[0]
        "vmla.f32                 q12, q9, d0[1]          \n\t"        //q12 = q9 * d0[1]
        "vmla.f32                 q13, q9, d2[1]          \n\t"        //q13 = q9 * d2[1]
        "vmla.f32                 q14, q9, d4[1]          \n\t"        //q14 = q9 * d4[1]
        "vmla.f32                 q15, q9, d6[1]          \n\t"        //q15 = q9 * d6[1]
        "vmla.f32                 q12, q10, d1[0]         \n\t"        //q12 = q10 * d0[0]
        "vmla.f32                 q13, q10, d3[0]         \n\t"        //q13 = q10 * d2[0]
        "vmla.f32                 q14, q10, d5[0]         \n\t"        //q14 = q10 * d4[0]
        "vmla.f32                 q15, q10, d7[0]         \n\t"        //q15 = q10 * d6[0]
        "vmla.f32                 q12, q11, d1[1]         \n\t"        //q12 = q11 * d0[1]
        "vmla.f32                 q13, q11, d3[1]         \n\t"        //q13 = q11 * d2[1]
        "vmla.f32                 q14, q11, d5[1]         \n\t"        //q14 = q11 * d4[1]
        "vmla.f32                 q15, q11, d7[1]         \n\t"        //q15 = q11 * d6[1]

        "vst1.32                 {d24, d25}, [%2]!        \n\t"        //d = q12
        "vst1.32                 {d26, d27}, [%2]!        \n\t"        //d+4 = q13
        "vst1.32                 {d28, d29}, [%2]!        \n\t"        //d+8 = q14
        "vst1.32                 {d30, d31}, [%2]         \n\t"        //d+12 = q15

        :"+r"(m1), "+r"(m0), "+r"(dest):
    : "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
    "d16", "d17", "d18", "d19", "d20", "d21", "d22", "d23",
    "d24", "d25", "d26", "d27", "d28", "d29", "d30", "d31",
    "memory"
        );
}
#endif

void math_init(void)
{
   unsigned cpu = 0;

   if (perf_get_cpu_features_cb)
      cpu = perf_get_cpu_features_cb();

#if defined(__ARM_NEON__)
   if (cpu & RETRO_SIMD_NEON)
   {
      glide64NormalizeVector = NormalizeVectorNeon;
      glide64MulMatrices = MulMatricesNeon;
      glide64DotProduct = DotProductNeon;
      if (log_cb)
         log_cb(RETRO_LOG_INFO, "NEON detected, using (some) optimized math functions.\n");
   }
#endif
}

void calc_light (VERTEX *v)
{
   uint32_t i;
   float color[3];
   float light_intensity = 0.0f;

   color[0] = rdp.light[rdp.num_lights].col[0];
   color[1] = rdp.light[rdp.num_lights].col[1];
   color[2] = rdp.light[rdp.num_lights].col[2];

   for (i = 0; i < rdp.num_lights; i++)
   {
      light_intensity = DotProduct (rdp.light_vector[i], v->vec);

      if (light_intensity > 0.0f) 
      {
         color[0] += rdp.light[i].col[0] * light_intensity;
         color[1] += rdp.light[i].col[1] * light_intensity;
         color[2] += rdp.light[i].col[2] * light_intensity;
      }
   }

   v->r = (uint8_t)(255.0f * get_float_color_clamped(color[0]));
   v->g = (uint8_t)(255.0f * get_float_color_clamped(color[1]));
   v->b = (uint8_t)(255.0f * get_float_color_clamped(color[2]));
}

void calc_linear (VERTEX *v)
{
   DECLAREALIGN16VAR(vec[3]);
   float x, y;

   if (settings.force_calc_sphere)
   {
      calc_sphere(v);
      return;
   }

   TransformVectorC(v->vec, vec, rdp.model);
   NormalizeVector (vec);
   x = vec[0];
   y = vec[1];
   if (rdp.use_lookat)
   {
      x = DotProduct (rdp.lookat[0], vec);
      y = DotProduct (rdp.lookat[1], vec);
   }

   if (x > 1.0f)
      x = 1.0f;
   else if (x < -1.0f)
      x = -1.0f;
   if (y > 1.0f)
      y = 1.0f;
   else if (y < -1.0f)
      y = -1.0f;

   if (rdp.cur_cache[0])
   {
      // scale >> 6 is size to map to
      v->ou = (acosf(x)/3.141592654f) * (rdp.tiles[rdp.cur_tile].org_s_scale >> 6);
      v->ov = (acosf(y)/3.141592654f) * (rdp.tiles[rdp.cur_tile].org_t_scale >> 6);
   }
   v->uv_scaled = 1;
#ifdef EXTREME_LOGGING
   FRDP ("calc linear u: %f, v: %f\n", v->ou, v->ov);
#endif
}

void calc_sphere (VERTEX *v)
{
   DECLAREALIGN16VAR(vec[3]);
   float x, y;
   int s_scale, t_scale;

   s_scale = rdp.tiles[rdp.cur_tile].org_s_scale >> 6;
   t_scale = rdp.tiles[rdp.cur_tile].org_t_scale >> 6;

   if (settings.hacks&hack_Chopper)
   {
      s_scale = min(rdp.tiles[rdp.cur_tile].org_s_scale >> 6, g_gdp.tile[rdp.cur_tile].sl);
      t_scale = min(rdp.tiles[rdp.cur_tile].org_t_scale >> 6, g_gdp.tile[rdp.cur_tile].tl);
   }

   TransformVectorC(v->vec, vec, rdp.model);
   NormalizeVector (vec);
   x = vec[0];
   y = vec[1];
   if (rdp.use_lookat)
   {
      x = DotProduct (rdp.lookat[0], vec);
      y = DotProduct (rdp.lookat[1], vec);
   }
   v->ou = (x * 0.5f + 0.5f) * s_scale;
   v->ov = (y * 0.5f + 0.5f) * t_scale;
   v->uv_scaled = 1;
#ifdef EXTREME_LOGGING
   FRDP ("calc sphere u: %f, v: %f\n", v->ou, v->ov);
#endif
}
