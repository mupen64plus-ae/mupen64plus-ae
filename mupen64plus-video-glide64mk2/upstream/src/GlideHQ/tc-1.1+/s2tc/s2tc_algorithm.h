/*
 * Copyright (C) 2011  Rudolf Polzer   All Rights Reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * RUDOLF POLZER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

#ifndef S2TC_COMPRESSOR_H
#define S2TC_COMPRESSOR_H

// note: this is a C header file!

#ifdef __cplusplus
extern "C" {
#endif

enum DitherMode
{
	DITHER_NONE,
	DITHER_SIMPLE,
	DITHER_FLOYDSTEINBERG
};

void rgb565_image(unsigned char *out, const unsigned char *rgba, int w, int h, int srccomps, int alphabits, DitherMode dither);

enum DxtMode
{
	DXT1,
	DXT3,
	DXT5
};
enum RefinementMode
{
	REFINE_NEVER,
	REFINE_ALWAYS,
	REFINE_LOOP
};

typedef enum
{
	RGB,
	YUV,
	SRGB,
	SRGB_MIXED,
	AVG,
	WAVG,
	NORMALMAP
} ColorDistMode;

typedef void (*s2tc_encode_block_func_t) (unsigned char *out, const unsigned char *rgba, int iw, int w, int h, int nrandom);
s2tc_encode_block_func_t s2tc_encode_block_func(DxtMode dxt, ColorDistMode cd, int nrandom, RefinementMode refine);

#ifdef __cplusplus
}
#endif

#endif
