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
#include "../../Glide64/m64p.h"

typedef void (*dxtCompressTexFuncExt)(int srccomps, int width,
                                      int height, const byte *srcPixData,
                                      int destformat, byte *dest,
                                      int dstRowStride);
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

#include "dxtn.h"

#define GL_COMPRESSED_RGB_S3TC_DXT1_EXT   0x83F0
#define GL_COMPRESSED_RGBA_S3TC_DXT1_EXT  0x83F1
#define GL_COMPRESSED_RGBA_S3TC_DXT3_EXT  0x83F2
#define GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  0x83F3

TAPI void TAPIENTRY
fetch_2d_texel_rgb_dxt1 (int texImage_RowStride,
			 const byte *texImage_Data,
			 int i, int j,
			 byte *texel)
{
    dxt1_rgb_decode_1(texImage_Data, texImage_RowStride, i, j, texel);
}


TAPI void TAPIENTRY
fetch_2d_texel_rgba_dxt1 (int texImage_RowStride,
			  const byte *texImage_Data,
			  int i, int j,
			  byte *texel)
{
    dxt1_rgba_decode_1(texImage_Data, texImage_RowStride, i, j, texel);
}


TAPI void TAPIENTRY
fetch_2d_texel_rgba_dxt3 (int texImage_RowStride,
			  const byte *texImage_Data,
			  int i, int j,
			  byte *texel)
{
    dxt3_rgba_decode_1(texImage_Data, texImage_RowStride, i, j, texel);
}


TAPI void TAPIENTRY
fetch_2d_texel_rgba_dxt5 (int texImage_RowStride,
			  const byte *texImage_Data,
			  int i, int j,
			  byte *texel)
{
    dxt5_rgba_decode_1(texImage_Data, texImage_RowStride, i, j, texel);
}


static
void tx_compress_dxtn (int srccomps, int width, int height,
		  const byte *source, int destformat, byte *dest,
		  int destRowStride)
{
    int srcRowStride = width * srccomps;

    switch (destformat) {
	case GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
	    dxt1_rgb_encode(width, height, srccomps,
			    source, srcRowStride,
			    dest, destRowStride);
	    break;
	case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
	    dxt1_rgba_encode(width, height, srccomps,
			     source, srcRowStride,
			     dest, destRowStride);
	    break;
	case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
	    dxt3_rgba_encode(width, height, srccomps,
			     source, srcRowStride,
			     dest, destRowStride);
	    break;
	case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
	    dxt5_rgba_encode(width, height, srccomps,
			     source, srcRowStride,
			     dest, destRowStride);
	    break;
	default:
	    assert(0);
    }
}

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
