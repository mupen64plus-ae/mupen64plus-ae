/*
 * Texture compression
 * Version:  1.0
 *
 * Copyright (C) 2004  Daniel Borca   All Rights Reserved.
 *
 * this is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * this is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GNU Make; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.	
 */


#ifndef INTERNAL_H_included
#define INTERNAL_H_included

#include <stdint.h>

/*****************************************************************************\
 * DLL stuff
\*****************************************************************************/

#ifdef __WIN32__
#define TAPI __declspec(dllexport)
#define TAPIENTRY /*__stdcall*/
#else
#define TAPI
#define TAPIENTRY
#endif


/*****************************************************************************\
 * 64bit types on 32bit machine
\*****************************************************************************/

/*
 * Define a 64-bit unsigned integer type and macros
 */
#if 1

#define Q_NATIVE 1

typedef uint64_t qword;

#define Q_MOV32(a, b) a = b
#define Q_OR32(a, b)  a |= b
#define Q_SHL(a, c)   a <<= c

#else

#define Q_NATIVE 0

typedef struct {
   dword lo, hi;
} qword;

#define Q_MOV32(a, b) a.lo = b
#define Q_OR32(a, b)  a.lo |= b

#define Q_SHL(a, c)                                 \
   do {                                                \
       if ((c) >= 32) {                                \
          a.hi = a.lo << ((c) - 32);                   \
          a.lo = 0;                                    \
       } else {                                        \
          a.hi = (a.hi << (c)) | (a.lo >> (32 - (c))); \
          a.lo <<= (c);                                \
       }                                               \
   } while (0)

#endif


/*****************************************************************************\
 * Config
\*****************************************************************************/

#define RCOMP 0
#define GCOMP 1
#define BCOMP 2
#define ACOMP 3

/*****************************************************************************\
 * Metric
\*****************************************************************************/

#define F(i) (float)1 /* can be used to obtain an oblong metric: 0.30 / 0.59 / 0.11 */
#define SAFECDOT 1 /* for paranoids */

#define MAKEIVEC(NV, NC, IV, B, V0, V1)  \
   do {                                  \
      /* compute interpolation vector */ \
      float d2 = 0.0F;                   \
      float rd2;                         \
                                         \
      for (i = 0; i < NC; i++) {         \
         IV[i] = (V1[i] - V0[i]) * F(i); \
         d2 += IV[i] * IV[i];            \
      }                                  \
      rd2 = (float)NV / d2;              \
      B = 0;                             \
      for (i = 0; i < NC; i++) {         \
         IV[i] *= F(i);                  \
         B -= IV[i] * V0[i];             \
         IV[i] *= rd2;                   \
      }                                  \
      B = B * rd2 + 0.5f;                \
   } while (0)

#define CALCCDOT(TEXEL, NV, NC, IV, B, V)\
   do {                                  \
      float dot = 0.0F;                  \
      for (i = 0; i < NC; i++) {         \
         dot += V[i] * IV[i];            \
      }                                  \
      TEXEL = (int)(dot + B);            \
      if (SAFECDOT) {                    \
         if (TEXEL < 0) {                \
            TEXEL = 0;                   \
         } else if (TEXEL > NV) {        \
            TEXEL = NV;                  \
         }                               \
      }                                  \
   } while (0)


/*****************************************************************************\
 * Utility functions
\*****************************************************************************/

/** Copy a 4-element vector */
#define COPY_4V( DST, SRC )         \
do {                                \
   (DST)[0] = (SRC)[0];             \
   (DST)[1] = (SRC)[1];             \
   (DST)[2] = (SRC)[2];             \
   (DST)[3] = (SRC)[3];             \
} while (0)

/** Copy a 4-element unsigned byte vector */
static void
COPY_4UBV(uint8_t dst[4], const uint8_t src[4])
{
#if defined(__i386__)
   *((uint32_t *) dst) = *((uint32_t *) src);
#else
   /* The uint32_t cast might fail if DST or SRC are not dword-aligned (RISC) */
   COPY_4V(dst, src);
#endif
}

void reorder_source_3(byte *tex, dword width, dword height, int srcRowStride);
void *reorder_source_3_alloc(const byte *source, dword width, dword height, int srcRowStride);
void reorder_source_4(byte *tex, dword width, dword height, int srcRowStride);
void *reorder_source_4_alloc(const byte *source, dword width, dword height, int srcRowStride);

#endif
