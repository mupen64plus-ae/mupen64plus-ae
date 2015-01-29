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


#include <assert.h>
#include <stdlib.h>

#include "types.h"
#include "internal.h"
#ifdef USE_GLES
#include <SDL_opengles2.h>
#else
#include <SDL_opengl.h>
#endif
#include "../../Glide64/m64p.h"

typedef void (*dxtCompressTexFuncExt)(GLint srccomps, GLint width, GLint height,
		                      const GLubyte *srcPixData, GLenum destformat,
                                      GLubyte *dest, GLint dstRowStride);
static dxtCompressTexFuncExt _tx_compress_dxtn = NULL;

#ifdef TXCDXTN_EXTERNAL

#include "../../Glide64/osal_dynamiclib.h"

#if defined(_WIN32) || defined(WIN32)
#define DXTN_LIBNAME "dxtn.dll"
#elif defined(__DJGPP__)
#define DXTN_LIBNAME "dxtn.dxe"
#else
#define DXTN_LIBNAME "libtxc_dxtn.so"
#endif

static m64p_dynlib_handle dxtn_lib_handle;

static void tx_compress_dxtn_init()
{
    m64p_error rval;

    if (_tx_compress_dxtn)
        return;

    rval = osal_dynlib_open(&dxtn_lib_handle, DXTN_LIBNAME);
    if (rval != M64ERR_SUCCESS) {
        WriteLog(M64MSG_WARNING, "Failed to open %s", DXTN_LIBNAME);
        return;
    }

    _tx_compress_dxtn = osal_dynlib_getproc(dxtn_lib_handle, "tx_compress_dxtn");
    if (!_tx_compress_dxtn) {
        WriteLog(M64MSG_WARNING, "Shared library '%s' invalid; no PluginGetVersion() function found.", DXTN_LIBNAME, "tx_compress_dxtn");
	osal_dynlib_close(dxtn_lib_handle);
        return;
    }
}

#else

#include "s2tc/txc_dxtn.h"

static void tx_compress_dxtn_init()
{
	_tx_compress_dxtn = tx_compress_dxtn;
}

#endif


TAPI void TAPIENTRY
tx_compress_dxtn_rgba(int srccomps, int width, int height,
                      const byte *source, int destformat, byte *dest,
                      int destRowStride)
{
    int srcRowStride = width * srccomps;
    void *newSource = NULL;

    tx_compress_dxtn_init();
    if (!_tx_compress_dxtn) {
        WriteLog(M64MSG_ERROR, "Failed to initialize S3TC compressor");
        return;
    }

    assert(srccomps == 3 || srccomps == 4);

    if (srccomps == 3)
        newSource = reorder_source_3_alloc(source, width, height, srcRowStride);
    if (srccomps == 4)
        newSource = reorder_source_4_alloc(source, width, height, srcRowStride);

    _tx_compress_dxtn(srccomps, width, height, newSource, destformat, dest,
                      destRowStride);

    free(newSource);
}
