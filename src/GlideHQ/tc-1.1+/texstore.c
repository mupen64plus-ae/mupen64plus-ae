/*
 * Mesa 3-D graphics library
 * Version:  6.3
 *
 * Copyright (C) 1999-2004  Brian Paul   All Rights Reserved.
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
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

#include <assert.h>
#include <string.h>
#include <stdlib.h>

#include "types.h"
#include "internal.h"

void reorder_source_3(byte *tex, dword width, dword height, int srcRowStride)
{
    byte *line;
    byte t;
    dword i, j;

    for (i = 0; i < height; i++) {
        line = &tex[srcRowStride * i];
        for (j = 0; j < width; j++) {
            t = line[2];
            line[2] = line[0];
            line[0] = t;
            line += 3;
        }
    }
}

void *reorder_source_3_alloc(const byte *source, dword width, dword height, int srcRowStride)
{
    byte *tex;

    tex = malloc(height * srcRowStride);
    if (!tex)
        goto out;

    memcpy(tex, source, height * srcRowStride);
    reorder_source_3(tex, width, height, srcRowStride);

out:
    return tex;
}

void reorder_source_4(byte *tex, dword width, dword height, int srcRowStride)
{
    byte *line;
    byte t;
    dword i, j;

    for (i = 0; i < height; i++) {
        line = &tex[srcRowStride * i];
        for (j = 0; j < width; j++) {
            t = line[2];
            line[2] = line[0];
            line[0] = t;
            line += 4;
        }
    }
}

void *reorder_source_4_alloc(const byte *source, dword width, dword height, int srcRowStride)
{
    byte *tex;

    tex = malloc(height * srcRowStride);
    if (!tex)
        goto out;

    memcpy(tex, source, height * srcRowStride);
    reorder_source_4(tex, width, height, srcRowStride);

out:
    return tex;
}
