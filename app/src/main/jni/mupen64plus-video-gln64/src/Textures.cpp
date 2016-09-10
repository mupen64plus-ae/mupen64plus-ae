#include <time.h>
#include <stdlib.h>
#include <memory.h>

#ifndef min
#define min(a,b) ((a) < (b) ? (a) : (b))
#endif

#include "Common.h"
#include "Config.h"
#include "OpenGL.h"
#include "Textures.h"
#include "GBI.h"
#include "RSP.h"
#include "gDP.h"
#include "gSP.h"
#include "N64.h"
#include "CRC.h"
#include "convert.h"
#include "2xSAI.h"
//#include "FrameBuffer.h"

#define FORMAT_NONE     0
#define FORMAT_I8       1
#define FORMAT_IA88     2
#define FORMAT_RGBA4444 3
#define FORMAT_RGBA5551 4
#define FORMAT_RGBA8888 5

//#define PRINT_TEXTUREFORMAT

TextureCache    cache;

typedef u32 (*GetTexelFunc)( void *src, u16 x, u16 i, u8 palette );

u32 GetNone( void *src, u16 x, u16 i, u8 palette )
{
    return 0x00000000;
}

u32 GetCI4IA_RGBA4444( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    if (x & 1)
        return IA88_RGBA4444( *(u16*)&TMEM[256 + (palette << 4) + (color4B & 0x0F)] );
    else
        return IA88_RGBA4444( *(u16*)&TMEM[256 + (palette << 4) + (color4B >> 4)] );
}

u32 GetCI4IA_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    if (x & 1)
        return IA88_RGBA8888( *(u16*)&TMEM[256 + (palette << 4) + (color4B & 0x0F)] );
    else
        return IA88_RGBA8888( *(u16*)&TMEM[256 + (palette << 4) + (color4B >> 4)] );
}

u32 GetCI4RGBA_RGBA5551( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    if (x & 1)
        return RGBA5551_RGBA5551( *(u16*)&TMEM[256 + (palette << 4) + (color4B & 0x0F)] );
    else
        return RGBA5551_RGBA5551( *(u16*)&TMEM[256 + (palette << 4) + (color4B >> 4)] );
}

u32 GetCI4RGBA_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    if (x & 1)
        return RGBA5551_RGBA8888( *(u16*)&TMEM[256 + (palette << 4) + (color4B & 0x0F)] );
    else
        return RGBA5551_RGBA8888( *(u16*)&TMEM[256 + (palette << 4) + (color4B >> 4)] );
}

u32 GetIA31_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    return IA31_RGBA8888( (x & 1) ? (color4B & 0x0F) : (color4B >> 4) );
}

u32 GetIA31_RGBA4444( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    return IA31_RGBA4444( (x & 1) ? (color4B & 0x0F) : (color4B >> 4) );
}

u32 GetIA31_IA88( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    return IA31_IA88( (x & 1) ? (color4B & 0x0F) : (color4B >> 4) );
}

u32 GetI4_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    return I4_RGBA8888( (x & 1) ? (color4B & 0x0F) : (color4B >> 4) );
}

u32 GetI4_RGBA4444( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    return I4_RGBA4444( (x & 1) ? (color4B & 0x0F) : (color4B >> 4) );
}

u32 GetI4_I8( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    return I4_I8( (x & 1) ? (color4B & 0x0F) : (color4B >> 4) );
}


u32 GetI4_IA88( void *src, u16 x, u16 i, u8 palette )
{
    u8 color4B = ((u8*)src)[(x>>1)^(i<<1)];
    return I4_IA88( (x & 1) ? (color4B & 0x0F) : (color4B >> 4) );
}

u32 GetCI8IA_RGBA4444( void *src, u16 x, u16 i, u8 palette )
{
    return IA88_RGBA4444( *(u16*)&TMEM[256 + ((u8*)src)[x^(i<<1)]] );
}

u32 GetCI8IA_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    return IA88_RGBA8888( *(u16*)&TMEM[256 + ((u8*)src)[x^(i<<1)]] );
}

u32 GetCI8RGBA_RGBA5551( void *src, u16 x, u16 i, u8 palette )
{
    return RGBA5551_RGBA5551( *(u16*)&TMEM[256 + ((u8*)src)[x^(i<<1)]] );
}

u32 GetCI8RGBA_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    return RGBA5551_RGBA8888( *(u16*)&TMEM[256 + ((u8*)src)[x^(i<<1)]] );
}

u32 GetIA44_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    return IA44_RGBA8888(((u8*)src)[x^(i<<1)]);
}

u32 GetIA44_RGBA4444( void *src, u16 x, u16 i, u8 palette )
{
    return IA44_RGBA4444(((u8*)src)[x^(i<<1)]);
}

u32 GetIA44_IA88( void *src, u16 x, u16 i, u8 palette )
{
    return IA44_IA88(((u8*)src)[x^(i<<1)]);
}

u32 GetI8_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    return I8_RGBA8888(((u8*)src)[x^(i<<1)]);
}

u32 GetI8_I8( void *src, u16 x, u16 i, u8 palette )
{
    return ((u8*)src)[x^(i<<1)];
}

u32 GetI8_IA88( void *src, u16 x, u16 i, u8 palette )
{
    return I8_IA88(((u8*)src)[x^(i<<1)]);
}

u32 GetI8_RGBA4444( void *src, u16 x, u16 i, u8 palette )
{
    return I8_RGBA4444(((u8*)src)[x^(i<<1)]);
}

u32 GetRGBA5551_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    return RGBA5551_RGBA8888( ((u16*)src)[x^i] );
}

u32 GetRGBA5551_RGBA5551( void *src, u16 x, u16 i, u8 palette )
{
    return RGBA5551_RGBA5551( ((u16*)src)[x^i] );
}

u32 GetIA88_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    return IA88_RGBA8888(((u16*)src)[x^i]);
}

u32 GetIA88_RGBA4444( void *src, u16 x, u16 i, u8 palette )
{
    return IA88_RGBA4444(((u16*)src)[x^i]);
}

u32 GetIA88_IA88( void *src, u16 x, u16 i, u8 palette )
{
    return IA88_IA88(((u16*)src)[x^i]);
}

u32 GetRGBA8888_RGBA8888( void *src, u16 x, u16 i, u8 palette )
{
    return ((u32*)src)[x^i];
}

u32 GetRGBA8888_RGBA4444( void *src, u16 x, u16 i, u8 palette )
{
    return RGBA8888_RGBA4444(((u32*)src)[x^i]);
}


struct TextureFormat
{
    int format;
    GetTexelFunc getTexel;
    int lineShift, maxTexels;
};


TextureFormat textureFormatIA[4*6] =
{
    // 4-bit
    {   FORMAT_RGBA5551,    GetCI4RGBA_RGBA5551,    4,  4096 }, // RGBA (SELECT)
    {   FORMAT_NONE,        GetNone,                4,  8192 }, // YUV
    {   FORMAT_RGBA5551,    GetCI4RGBA_RGBA5551,    4,  4096 }, // CI
    {   FORMAT_IA88,        GetIA31_IA88,           4,  8192 }, // IA
    {   FORMAT_IA88,        GetI4_IA88,             4,  8192 }, // I
    {   FORMAT_RGBA8888,    GetCI4IA_RGBA8888,      4,  4096 }, // IA Palette
    // 8-bit
    {   FORMAT_RGBA5551,    GetCI8RGBA_RGBA5551,    3,  2048 }, // RGBA (SELECT)
    {   FORMAT_NONE,        GetNone,                3,  4096 }, // YUV
    {   FORMAT_RGBA5551,    GetCI8RGBA_RGBA5551,    3,  2048 }, // CI
    {   FORMAT_IA88,        GetIA44_IA88,           3,  4096 }, // IA
    {   FORMAT_IA88,        GetI8_IA88,             3,  4096 }, // I
    {   FORMAT_RGBA8888,    GetCI8IA_RGBA8888,      3,  2048 }, // IA Palette
    // 16-bit
    {   FORMAT_RGBA5551,    GetRGBA5551_RGBA5551,   2,  2048 }, // RGBA
    {   FORMAT_NONE,        GetNone,                2,  2048 }, // YUV
    {   FORMAT_NONE,        GetNone,                2,  2048 }, // CI
    {   FORMAT_IA88,        GetIA88_IA88,           2,  2048 }, // IA
    {   FORMAT_NONE,        GetNone,                2,  2048 }, // I
    {   FORMAT_NONE,        GetNone,                2,  2048 }, // IA Palette
    // 32-bit
    {   FORMAT_RGBA8888,    GetRGBA8888_RGBA8888,   2,  1024 }, // RGBA
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // YUV
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // CI
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // IA
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // I
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // IA Palette
};

TextureFormat textureFormatRGBA[4*6] =
{
    // 4-bit
    {   FORMAT_RGBA5551,    GetCI4RGBA_RGBA5551,    4,  4096 }, // RGBA (SELECT)
    {   FORMAT_NONE,        GetNone,                4,  8192 }, // YUV
    {   FORMAT_RGBA5551,    GetCI4RGBA_RGBA5551,    4,  4096 }, // CI
    {   FORMAT_RGBA4444,    GetIA31_RGBA4444,       4,  8192 }, // IA
    {   FORMAT_RGBA4444,    GetI4_RGBA4444,         4,  8192 }, // I
    {   FORMAT_RGBA8888,    GetCI4IA_RGBA8888,      4,  4096 }, // IA Palette
    // 8-bit
    {   FORMAT_RGBA5551,    GetCI8RGBA_RGBA5551,    3,  2048 }, // RGBA (SELECT)
    {   FORMAT_NONE,        GetNone,                3,  4096 }, // YUV
    {   FORMAT_RGBA5551,    GetCI8RGBA_RGBA5551,    3,  2048 }, // CI
    {   FORMAT_RGBA4444,    GetIA44_RGBA4444,       3,  4096 }, // IA
    {   FORMAT_RGBA8888,    GetI8_RGBA8888,         3,  4096 }, // I
    {   FORMAT_RGBA8888,    GetCI8IA_RGBA8888,      3,  2048 }, // IA Palette
    // 16-bit
    {   FORMAT_RGBA5551,    GetRGBA5551_RGBA5551,   2,  2048 }, // RGBA
    {   FORMAT_NONE,        GetNone,                2,  2048 }, // YUV
    {   FORMAT_NONE,        GetNone,                2,  2048 }, // CI
    {   FORMAT_RGBA8888,    GetIA88_RGBA8888,       2,  2048 }, // IA
    {   FORMAT_NONE,        GetNone,                2,  2048 }, // I
    {   FORMAT_NONE,        GetNone,                2,  2048 }, // IA Palette
    // 32-bit
    {   FORMAT_RGBA8888,    GetRGBA8888_RGBA8888,   2,  1024 }, // RGBA
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // YUV
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // CI
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // IA
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // I
    {   FORMAT_NONE,        GetNone,                2,  1024 }, // IA Palette
};


TextureFormat *textureFormat = textureFormatIA;

void __texture_format_rgba(int size, int format, TextureFormat *texFormat)
{
    if (size < G_IM_SIZ_16b)
    {
        if (gDP.otherMode.textureLUT == G_TT_NONE)
            *texFormat = textureFormat[size*6 + G_IM_FMT_I];
        else if (gDP.otherMode.textureLUT == G_TT_RGBA16)
            *texFormat = textureFormat[size*6 + G_IM_FMT_CI];
        else
            *texFormat = textureFormat[size*6 + G_IM_FMT_IA];
    }
    else
    {
        *texFormat = textureFormat[size*6 + G_IM_FMT_RGBA];
    }
}

void __texture_format_ci(int size, int format, TextureFormat *texFormat)
{
    switch(size)
    {
        case G_IM_SIZ_4b:
            if (gDP.otherMode.textureLUT == G_TT_IA16)
                *texFormat = textureFormat[G_IM_SIZ_4b*6 + G_IM_FMT_CI_IA];
            else
                *texFormat = textureFormat[G_IM_SIZ_4b*6 + G_IM_FMT_CI];
            break;

        case G_IM_SIZ_8b:
            if (gDP.otherMode.textureLUT == G_TT_NONE)
                *texFormat = textureFormat[G_IM_SIZ_8b*6 + G_IM_FMT_I];
            else if (gDP.otherMode.textureLUT == G_TT_IA16)
                *texFormat = textureFormat[G_IM_SIZ_8b*6 + G_IM_FMT_CI_IA];
            else
                *texFormat = textureFormat[G_IM_SIZ_8b*6 + G_IM_FMT_CI];
            break;

        default:
            *texFormat = textureFormat[size*6 + format];
    }
}

void __texture_format(int size, int format, TextureFormat *texFormat)
{
    if (format == G_IM_FMT_RGBA)
    {
        __texture_format_rgba(size, format, texFormat);
    }
    else if (format == G_IM_FMT_YUV)
    {
        *texFormat = textureFormat[size*6 + G_IM_FMT_YUV];
    }
    else if (format == G_IM_FMT_CI)
    {
        __texture_format_ci(size, format, texFormat);
    }
    else if (format == G_IM_FMT_IA)
    {
        if (gDP.otherMode.textureLUT != G_TT_NONE)
            __texture_format_ci(size, format, texFormat);
        else
            *texFormat = textureFormat[size*6 + G_IM_FMT_IA];
    }
    else if (format == G_IM_FMT_I)
    {
        if (gDP.otherMode.textureLUT == G_TT_NONE)
            *texFormat = textureFormat[size*6 + G_IM_FMT_I];
        else
            __texture_format_ci(size, format, texFormat);
    }
}


int isTexCacheInit = 0;

void TextureCache_Init()
{
    u32 dummyTexture[16] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    isTexCacheInit = 1;
    cache.current[0] = NULL;
    cache.current[1] = NULL;
    cache.top = NULL;
    cache.bottom = NULL;
    cache.numCached = 0;
    cache.cachedBytes = 0;

#ifdef __HASHMAP_OPT
    cache.hash.init(11);
#endif

    if (config.texture.useIA) textureFormat = textureFormatIA;
    else textureFormat = textureFormatRGBA;

    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glGenTextures( 32, cache.glNoiseNames );

    srand(time(NULL));
    u8 noise[64*64*2];
    for (u32 i = 0; i < 32; i++)
    {
        glBindTexture( GL_TEXTURE_2D, cache.glNoiseNames[i] );
        for (u32 y = 0; y < 64; y++)
        {
            for (u32 x = 0; x < 64; x++)
            {
                u32 r = (rand()&0xFF);
                noise[y*64*2+x*2] = r;
                noise[y*64*2+x*2+1] = r;
            }
        }
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE_ALPHA, 64, 64, 0, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, noise);
    }

    cache.dummy = TextureCache_AddTop();
    cache.dummy->address = 0;
    cache.dummy->clampS = 1;
    cache.dummy->clampT = 1;
    cache.dummy->clampWidth = 4;
    cache.dummy->clampHeight = 4;
    cache.dummy->crc = 0;
    cache.dummy->format = 0;
    cache.dummy->size = 0;
    cache.dummy->width = 4;
    cache.dummy->height = 4;
    cache.dummy->realWidth = 0;
    cache.dummy->realHeight = 0;
    cache.dummy->maskS = 0;
    cache.dummy->maskT = 0;
    cache.dummy->scaleS = 0.5f;
    cache.dummy->scaleT = 0.5f;
    cache.dummy->shiftScaleS = 1.0f;
    cache.dummy->shiftScaleT = 1.0f;
    cache.dummy->textureBytes = 64;
    cache.dummy->tMem = 0;

    glBindTexture( GL_TEXTURE_2D, cache.dummy->glName );
    glTexImage2D( GL_TEXTURE_2D, 0, GL_RGBA, 4, 4, 0, GL_RGBA, GL_UNSIGNED_BYTE, dummyTexture);

    cache.cachedBytes = cache.dummy->textureBytes;
    TextureCache_ActivateDummy(0);
    TextureCache_ActivateDummy(1);
    CRC_BuildTable();
}

bool TextureCache_Verify()
{
    u16 i = 0;
    CachedTexture *current;

    current = cache.top;

    while (current)
    {
        i++;
        current = current->lower;
    }
    if (i != cache.numCached) return false;

    i = 0;
    current = cache.bottom;
    while (current)
    {
        i++;
        current = current->higher;
    }
    if (i != cache.numCached) return false;

    return true;
}

void TextureCache_RemoveBottom()
{
    CachedTexture *newBottom = cache.bottom->higher;

#ifdef __HASHMAP_OPT
    CachedTexture* tex= cache.hash.find(cache.bottom->crc);
    if (tex == cache.bottom)
        cache.hash.insert(cache.bottom->crc, NULL);
#endif

    glDeleteTextures( 1, &cache.bottom->glName );
    cache.cachedBytes -= cache.bottom->textureBytes;

    if (cache.bottom == cache.top)
        cache.top = NULL;

    free( cache.bottom );

    cache.bottom = newBottom;

    if (cache.bottom)
        cache.bottom->lower = NULL;

    cache.numCached--;
}

void TextureCache_Remove( CachedTexture *texture )
{
    if ((texture == cache.bottom) && (texture == cache.top))
    {
        cache.top = NULL;
        cache.bottom = NULL;
    }
    else if (texture == cache.bottom)
    {
        cache.bottom = texture->higher;

        if (cache.bottom)
            cache.bottom->lower = NULL;
    }
    else if (texture == cache.top)
    {
        cache.top = texture->lower;

        if (cache.top)
            cache.top->higher = NULL;
    }
    else
    {
        texture->higher->lower = texture->lower;
        texture->lower->higher = texture->higher;
    }

#ifdef __HASHMAP_OPT
    CachedTexture* tex= cache.hash.find(texture->crc);
    if (tex == texture);
        cache.hash.insert(texture->crc, NULL);
#endif

    glDeleteTextures( 1, &texture->glName );
    cache.cachedBytes -= texture->textureBytes;
    free( texture );

    cache.numCached--;
}

CachedTexture *TextureCache_AddTop()
{
    while (cache.cachedBytes > TEXTURECACHE_MAX)
    {
        if (cache.bottom != cache.dummy)
            TextureCache_RemoveBottom();
        else if (cache.dummy->higher)
            TextureCache_Remove( cache.dummy->higher );
    }

    CachedTexture *newtop = (CachedTexture*)malloc( sizeof( CachedTexture ) );

    glGenTextures( 1, &newtop->glName );

    newtop->lower = cache.top;
    newtop->higher = NULL;

    if (cache.top)
        cache.top->higher = newtop;

    if (!cache.bottom)
        cache.bottom = newtop;

    cache.top = newtop;

    cache.numCached++;

    return newtop;
}

void TextureCache_MoveToTop( CachedTexture *newtop )
{
    if (newtop == cache.top) return;

    if (newtop == cache.bottom)
    {
        cache.bottom = newtop->higher;
        cache.bottom->lower = NULL;
    }
    else
    {
        newtop->higher->lower = newtop->lower;
        newtop->lower->higher = newtop->higher;
    }

    newtop->higher = NULL;
    newtop->lower = cache.top;
    cache.top->higher = newtop;
    cache.top = newtop;
}

void TextureCache_Destroy()
{
    while (cache.bottom)
        TextureCache_RemoveBottom();

    glDeleteTextures( 32, cache.glNoiseNames );
    glDeleteTextures( 1, &cache.dummy->glName  );

#ifdef __HASHMAP_OPT
    cache.hash.destroy();
#endif

    cache.top = NULL;
    cache.bottom = NULL;
}



void TextureCache_LoadBackground( CachedTexture *texInfo )
{
    u32 *dest, *scaledDest;
    u8 *swapped, *src;
    u32 numBytes, bpl;
    u32 x, y, j, tx, ty;
    u16 clampSClamp,  clampTClamp;

    int bytePerPixel=0;
    TextureFormat   texFormat;
    GetTexelFunc    getTexel;
    GLint glWidth=0, glHeight=0;
    GLenum glType=0;
    GLenum glFormat=0;

    __texture_format(texInfo->size, texInfo->format, &texFormat);

#ifdef PRINT_TEXTUREFORMAT
    printf("BG LUT=%i, TEXTURE SIZE=%i, FORMAT=%i -> GL FORMAT=%i\n", gDP.otherMode.textureLUT, texInfo->size, texInfo->format, texFormat.format); fflush(stdout);
#endif

    if (texFormat.format == FORMAT_NONE)
    {
        LOG(LOG_WARNING, "No Texture Conversion function available, size=%i format=%i\n", texInfo->size, texInfo->format);
    }

    switch(texFormat.format)
    {
        case FORMAT_I8:
            glFormat = GL_LUMINANCE;
            glType = GL_UNSIGNED_BYTE;
            bytePerPixel = 1;
            break;
        case FORMAT_IA88:
            glFormat = GL_LUMINANCE_ALPHA;
            glType = GL_UNSIGNED_BYTE;
            bytePerPixel = 2;
            break;
        case FORMAT_RGBA4444:
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_SHORT_4_4_4_4;
            bytePerPixel = 2;
            break;
        case FORMAT_RGBA5551:
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_SHORT_5_5_5_1;
            bytePerPixel = 2;
            break;
        case FORMAT_RGBA8888:
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_BYTE;
            bytePerPixel = 4;
            break;
    }

    glWidth = texInfo->realWidth;
    glHeight = texInfo->realHeight;
    texInfo->textureBytes = (glWidth * glHeight) * bytePerPixel;
    getTexel = texFormat.getTexel;

    bpl = gSP.bgImage.width << gSP.bgImage.size >> 1;
    numBytes = bpl * gSP.bgImage.height;
    swapped = (u8*) malloc(numBytes);
    dest = (u32*) malloc(texInfo->textureBytes);

    if (!dest || !swapped)
    {
        LOG(LOG_ERROR, "Malloc failed!\n");
        return;
    }

    UnswapCopy(&RDRAM[gSP.bgImage.address], swapped, numBytes);

    clampSClamp = texInfo->width - 1;
    clampTClamp = texInfo->height - 1;

    j = 0;
    for (y = 0; y < texInfo->realHeight; y++)
    {
        ty = min(y, clampTClamp);
        src = &swapped[bpl * ty];
        for (x = 0; x < texInfo->realWidth; x++)
        {
            tx = min(x, clampSClamp);
            if (bytePerPixel == 4)
                ((u32*)dest)[j++] = getTexel(src, tx, 0, texInfo->palette);
            else if (bytePerPixel == 2)
                ((u16*)dest)[j++] = getTexel(src, tx, 0, texInfo->palette);
            else if (bytePerPixel == 1)
                ((u8*)dest)[j++] = getTexel(src, tx, 0, texInfo->palette);
        }
    }

    if (!config.texture.sai2x || (texFormat.format == FORMAT_I8 || texFormat.format == FORMAT_IA88))
    {
        glTexImage2D( GL_TEXTURE_2D, 0, glFormat, glWidth, glHeight, 0, glFormat, glType, dest);
    }
    else
    {
        LOG(LOG_VERBOSE, "Using 2xSAI Filter on Texture\n");
        texInfo->textureBytes <<= 2;

        scaledDest = (u32*) malloc( texInfo->textureBytes );

        if (glType == GL_UNSIGNED_BYTE)
            _2xSaI8888( (u32*)dest, (u32*)scaledDest, texInfo->realWidth, texInfo->realHeight, texInfo->clampS, texInfo->clampT );
        if (glType == GL_UNSIGNED_SHORT_4_4_4_4)
            _2xSaI4444( (u16*)dest, (u16*)scaledDest, texInfo->realWidth, texInfo->realHeight, texInfo->clampS, texInfo->clampT );
        else
            _2xSaI5551( (u16*)dest, (u16*)scaledDest, texInfo->realWidth, texInfo->realHeight, texInfo->clampS, texInfo->clampT );

        glTexImage2D( GL_TEXTURE_2D, 0, GL_RGBA, texInfo->realWidth << 1, texInfo->realHeight << 1, 0, GL_RGBA, glType, scaledDest );

        free( scaledDest );
    }

    free(dest);
    free(swapped);


    if (config.texture.enableMipmap)
        glGenerateMipmap(GL_TEXTURE_2D);
}

void TextureCache_Load( CachedTexture *texInfo )
{
    u32 *dest, *scaledDest;

    void *src;
    u16 x, y, i, j, tx, ty, line;
    u16 mirrorSBit, maskSMask, clampSClamp;
    u16 mirrorTBit, maskTMask, clampTClamp;

    int bytePerPixel=0;
    TextureFormat   texFormat;
    GetTexelFunc    getTexel;
    GLint glWidth=0, glHeight=0;
    GLenum glType=0;
    GLenum glFormat=0;

    __texture_format(texInfo->size, texInfo->format, &texFormat);

#ifdef PRINT_TEXTUREFORMAT
    printf("TEX LUT=%i, TEXTURE SIZE=%i, FORMAT=%i -> GL FORMAT=%i\n", gDP.otherMode.textureLUT, texInfo->size, texInfo->format, texFormat.format); fflush(stdout);
#endif

    if (texFormat.format == FORMAT_NONE)
    {
        LOG(LOG_WARNING, "No Texture Conversion function available, size=%i format=%i\n", texInfo->size, texInfo->format);
    }

    switch(texFormat.format)
    {
        case FORMAT_I8:
            glFormat = GL_LUMINANCE;
            glType = GL_UNSIGNED_BYTE;
            bytePerPixel = 1;
            break;
        case FORMAT_IA88:
            glFormat = GL_LUMINANCE_ALPHA;
            glType = GL_UNSIGNED_BYTE;
            bytePerPixel = 2;
            break;
        case FORMAT_RGBA4444:
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_SHORT_4_4_4_4;
            bytePerPixel = 2;
            break;
        case FORMAT_RGBA5551:
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_SHORT_5_5_5_1;
            bytePerPixel = 2;
            break;
        case FORMAT_RGBA8888:
            glFormat = GL_RGBA;
            glType = GL_UNSIGNED_BYTE;
            bytePerPixel = 4;
            break;
    }

    glWidth = texInfo->realWidth;
    glHeight = texInfo->realHeight;
    texInfo->textureBytes = (glWidth * glHeight) * bytePerPixel;
    getTexel = texFormat.getTexel;

    dest = (u32*)malloc(texInfo->textureBytes);

    if (!dest)
    {
        LOG(LOG_ERROR, "Malloc failed!\n");
        return;
    }


    line = texInfo->line;

    if (texInfo->size == G_IM_SIZ_32b)
        line <<= 1;

    if (texInfo->maskS)
    {
        clampSClamp = texInfo->clampS ? texInfo->clampWidth - 1 : (texInfo->mirrorS ? (texInfo->width << 1) - 1 : texInfo->width - 1);
        maskSMask = (1 << texInfo->maskS) - 1;
        mirrorSBit = texInfo->mirrorS ? (1 << texInfo->maskS) : 0;
    }
    else
    {
        clampSClamp = min( texInfo->clampWidth, texInfo->width ) - 1;
        maskSMask = 0xFFFF;
        mirrorSBit = 0x0000;
    }

    if (texInfo->maskT)
    {
        clampTClamp = texInfo->clampT ? texInfo->clampHeight - 1 : (texInfo->mirrorT ? (texInfo->height << 1) - 1: texInfo->height - 1);
        maskTMask = (1 << texInfo->maskT) - 1;
        mirrorTBit = texInfo->mirrorT ? (1 << texInfo->maskT) : 0;
    }
    else
    {
        clampTClamp = min( texInfo->clampHeight, texInfo->height ) - 1;
        maskTMask = 0xFFFF;
        mirrorTBit = 0x0000;
    }

    // Hack for Zelda warp texture
    if (((texInfo->tMem << 3) + (texInfo->width * texInfo->height << texInfo->size >> 1)) > 4096)
    {
        texInfo->tMem = 0;
    }

    // limit clamp values to min-0 (Perfect Dark has height=0 textures, making negative clamps)
    if (clampTClamp & 0x8000) clampTClamp = 0;
    if (clampSClamp & 0x8000) clampSClamp = 0;

    j = 0;
    for (y = 0; y < texInfo->realHeight; y++)
    {
        ty = min(y, clampTClamp) & maskTMask;
        if (y & mirrorTBit) ty ^= maskTMask;
        src = &TMEM[(texInfo->tMem + line * ty) & 511];
        i = (ty & 1) << 1;
        for (x = 0; x < texInfo->realWidth; x++)
        {
            tx = min(x, clampSClamp) & maskSMask;

            if (x & mirrorSBit) tx ^= maskSMask;

            if (bytePerPixel == 4)
            {
                ((u32*)dest)[j] = getTexel(src, tx, i, texInfo->palette);
            }
            else if (bytePerPixel == 2)
            {
                ((u16*)dest)[j] = getTexel(src, tx, i, texInfo->palette);
            }
            else if (bytePerPixel == 1)
            {
                ((u8*)dest)[j] = getTexel(src, tx, i, texInfo->palette);
            }
            j++;
        }
    }

    if (!config.texture.sai2x || (texFormat.format == FORMAT_I8) || (texFormat.format == FORMAT_IA88))
    {
#ifdef PRINT_TEXTUREFORMAT
        printf("j=%u DEST=0x%x SIZE=%i F=0x%x, W=%i, H=%i, T=0x%x\n", j, dest, texInfo->textureBytes,glFormat, glWidth, glHeight, glType); fflush(stdout);
#endif
        glTexImage2D( GL_TEXTURE_2D, 0, glFormat, glWidth, glHeight, 0, glFormat, glType, dest);
    }
    else
    {
        LOG(LOG_VERBOSE, "Using 2xSAI Filter on Texture\n");

        texInfo->textureBytes <<= 2;

        scaledDest = (u32*)malloc( texInfo->textureBytes );

        if (glType == GL_UNSIGNED_BYTE)
            _2xSaI8888( (u32*)dest, (u32*)scaledDest, texInfo->realWidth, texInfo->realHeight, 1, 1 );
        else if (glType == GL_UNSIGNED_SHORT_4_4_4_4)
            _2xSaI4444( (u16*)dest, (u16*)scaledDest, texInfo->realWidth, texInfo->realHeight, 1, 1 );
        else
            _2xSaI5551( (u16*)dest, (u16*)scaledDest, texInfo->realWidth, texInfo->realHeight, 1, 1 );

        glTexImage2D( GL_TEXTURE_2D, 0, GL_RGBA, texInfo->realWidth << 1, texInfo->realHeight << 1, 0, GL_RGBA, glType, scaledDest );

        free( scaledDest );
    }

    free(dest);

    if (config.texture.enableMipmap)
        glGenerateMipmap(GL_TEXTURE_2D);

}

#define max(a,b) ((a) > (b) ? (a) : (b))

u32 TextureCache_CalculateCRC( u32 t, u32 width, u32 height )
{
    u32 crc;
    u32 y, /*i,*/ bpl, lineBytes, line;
    void *src;

    bpl = width << gSP.textureTile[t]->size >> 1;
    lineBytes = gSP.textureTile[t]->line << 3;

    line = gSP.textureTile[t]->line;
    if (gSP.textureTile[t]->size == G_IM_SIZ_32b)
        line <<= 1;

    crc = 0xFFFFFFFF;

#ifdef __CRC_OPT
    unsigned n = (config.texture.fastCRC) ? max(1, height / 8) : 1;
#else
    unsigned n = 1;
#endif

    for (y = 0; y < height; y += n)
    {
        src = (void*) &TMEM[(gSP.textureTile[t]->tmem + (y * line)) & 511];
        crc = CRC_Calculate( crc, src, bpl );
    }

    if (gSP.textureTile[t]->format == G_IM_FMT_CI)
    {
        if (gSP.textureTile[t]->size == G_IM_SIZ_4b)
            crc = CRC_Calculate( crc, &gDP.paletteCRC16[gSP.textureTile[t]->palette], 4 );
        else if (gSP.textureTile[t]->size == G_IM_SIZ_8b)
            crc = CRC_Calculate( crc, &gDP.paletteCRC256, 4 );
    }
    return crc;
}

void TextureCache_ActivateTexture( u32 t, CachedTexture *texture )
{

#ifdef __HASHMAP_OPT
    cache.hash.insert(texture->crc, texture);
#endif

    glActiveTexture( GL_TEXTURE0 + t );
    glBindTexture( GL_TEXTURE_2D, texture->glName );

    // Set filter mode. Almost always bilinear, but check anyways
    if ((gDP.otherMode.textureFilter == G_TF_BILERP) || (gDP.otherMode.textureFilter == G_TF_AVERAGE) || (config.texture.forceBilinear))
    {
        glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR );
        glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR );
    }
    else
    {
        glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST );
        glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST );
    }

    // Set clamping modes
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, (texture->clampS) ? GL_CLAMP_TO_EDGE : GL_REPEAT );
    glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, (texture->clampT) ? GL_CLAMP_TO_EDGE : GL_REPEAT );

    if (config.texture.maxAnisotropy > 0)
    {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, config.texture.maxAnisotropy);
    }

    texture->lastDList = RSP.DList;
    TextureCache_MoveToTop( texture );
    cache.current[t] = texture;
}

void TextureCache_ActivateDummy( u32 t)
{
    glActiveTexture(GL_TEXTURE0 + t);
    glBindTexture(GL_TEXTURE_2D, cache.dummy->glName );
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
}

int _background_compare(CachedTexture *current, u32 crc)
{
    if ((current != NULL) &&
        (current->crc == crc) &&
        (current->width == gSP.bgImage.width) &&
        (current->height == gSP.bgImage.height) &&
        (current->format == gSP.bgImage.format) &&
        (current->size == gSP.bgImage.size))
        return 1;
    else
        return 0;
}

void TextureCache_UpdateBackground()
{
    u32 numBytes = gSP.bgImage.width * gSP.bgImage.height << gSP.bgImage.size >> 1;
    u32 crc;

    crc = CRC_Calculate( 0xFFFFFFFF, &RDRAM[gSP.bgImage.address], numBytes );

    if (gSP.bgImage.format == G_IM_FMT_CI)
    {
        if (gSP.bgImage.size == G_IM_SIZ_4b)
            crc = CRC_Calculate( crc, &gDP.paletteCRC16[gSP.bgImage.palette], 4 );
        else if (gSP.bgImage.size == G_IM_SIZ_8b)
            crc = CRC_Calculate( crc, &gDP.paletteCRC256, 4 );
    }

    //before we traverse cache, check to see if texture is already bound:
    if (_background_compare(cache.current[0], crc))
    {
        return;
    }

#ifdef __HASHMAP_OPT
    CachedTexture *tex = cache.hash.find(crc);
    if (tex)
    {
        if (_background_compare(tex, crc))
        {
            TextureCache_ActivateTexture(0, tex);
            cache.hits++;
            return;
        }
    }
#endif

    CachedTexture *current = cache.top;
    while (current)
    {
        if (_background_compare(current, crc))
        {
            TextureCache_ActivateTexture( 0, current );
            cache.hits++;
            return;
        }
        current = current->lower;
    }
    cache.misses++;

    glActiveTexture(GL_TEXTURE0);
    cache.current[0] = TextureCache_AddTop();

    glBindTexture( GL_TEXTURE_2D, cache.current[0]->glName );
    cache.current[0]->address = gSP.bgImage.address;
    cache.current[0]->crc = crc;
    cache.current[0]->format = gSP.bgImage.format;
    cache.current[0]->size = gSP.bgImage.size;
    cache.current[0]->width = gSP.bgImage.width;
    cache.current[0]->height = gSP.bgImage.height;
    cache.current[0]->clampWidth = gSP.bgImage.width;
    cache.current[0]->clampHeight = gSP.bgImage.height;
    cache.current[0]->palette = gSP.bgImage.palette;
    cache.current[0]->maskS = 0;
    cache.current[0]->maskT = 0;
    cache.current[0]->mirrorS = 0;
    cache.current[0]->mirrorT = 0;
    cache.current[0]->clampS = 1;
    cache.current[0]->clampT = 1;
    cache.current[0]->line = 0;
    cache.current[0]->tMem = 0;
    cache.current[0]->lastDList = RSP.DList;

    cache.current[0]->realWidth = (config.texture.pow2) ? pow2(gSP.bgImage.width ) : gSP.bgImage.width;
    cache.current[0]->realHeight = (config.texture.pow2) ? pow2(gSP.bgImage.height) : gSP.bgImage.height;

    cache.current[0]->scaleS = 1.0f / (f32)(cache.current[0]->realWidth);
    cache.current[0]->scaleT = 1.0f / (f32)(cache.current[0]->realHeight);
    cache.current[0]->shiftScaleS = 1.0f;
    cache.current[0]->shiftScaleT = 1.0f;

    TextureCache_LoadBackground( cache.current[0] );
    TextureCache_ActivateTexture( 0, cache.current[0] );

    cache.cachedBytes += cache.current[0]->textureBytes;
}

int _texture_compare(u32 t, CachedTexture *current, u32 crc,  u32 width, u32 height, u32 clampWidth, u32 clampHeight)
{
    if  ((current != NULL) &&
        (current->crc == crc) &&
        (current->width == width) &&
        (current->height == height) &&
        (current->clampWidth == clampWidth) &&
        (current->clampHeight == clampHeight) &&
        (current->maskS == gSP.textureTile[t]->masks) &&
        (current->maskT == gSP.textureTile[t]->maskt) &&
        (current->mirrorS == gSP.textureTile[t]->mirrors) &&
        (current->mirrorT == gSP.textureTile[t]->mirrort) &&
        (current->clampS == gSP.textureTile[t]->clamps) &&
        (current->clampT == gSP.textureTile[t]->clampt) &&
        (current->format == gSP.textureTile[t]->format) &&
        (current->size == gSP.textureTile[t]->size))
        return 1;
    else
        return 0;
}


void TextureCache_Update( u32 t )
{
    CachedTexture *current;

    u32 crc, maxTexels;
    u32 tileWidth, maskWidth, loadWidth, lineWidth, clampWidth, height;
    u32 tileHeight, maskHeight, loadHeight, lineHeight, clampHeight, width;

    if (gDP.textureMode == TEXTUREMODE_BGIMAGE)
    {
        TextureCache_UpdateBackground();
        return;
    }

    TextureFormat texFormat;
    __texture_format(gSP.textureTile[t]->size, gSP.textureTile[t]->format, &texFormat);

    maxTexels = texFormat.maxTexels;

    // Here comes a bunch of code that just calculates the texture size...I wish there was an easier way...
    tileWidth = gSP.textureTile[t]->lrs - gSP.textureTile[t]->uls + 1;
    tileHeight = gSP.textureTile[t]->lrt - gSP.textureTile[t]->ult + 1;

    maskWidth = 1 << gSP.textureTile[t]->masks;
    maskHeight = 1 << gSP.textureTile[t]->maskt;

    loadWidth = gDP.loadTile->lrs - gDP.loadTile->uls + 1;
    loadHeight = gDP.loadTile->lrt - gDP.loadTile->ult + 1;

    lineWidth = gSP.textureTile[t]->line << texFormat.lineShift;

    if (lineWidth) // Don't allow division by zero
        lineHeight = min( maxTexels / lineWidth, tileHeight );
    else
        lineHeight = 0;

    if (gDP.textureMode == TEXTUREMODE_TEXRECT)
    {
        u32 texRectWidth = gDP.texRect.width - gSP.textureTile[t]->uls;
        u32 texRectHeight = gDP.texRect.height - gSP.textureTile[t]->ult;

        if (gSP.textureTile[t]->masks && ((maskWidth * maskHeight) <= maxTexels))
            width = maskWidth;
        else if ((tileWidth * tileHeight) <= maxTexels)
            width = tileWidth;
        else if ((tileWidth * texRectHeight) <= maxTexels)
            width = tileWidth;
        else if ((texRectWidth * tileHeight) <= maxTexels)
            width = gDP.texRect.width;
        else if ((texRectWidth * texRectHeight) <= maxTexels)
            width = gDP.texRect.width;
        else if (gDP.loadType == LOADTYPE_TILE)
            width = loadWidth;
        else
            width = lineWidth;

        if (gSP.textureTile[t]->maskt && ((maskWidth * maskHeight) <= maxTexels))
            height = maskHeight;
        else if ((tileWidth * tileHeight) <= maxTexels)
            height = tileHeight;
        else if ((tileWidth * texRectHeight) <= maxTexels)
            height = gDP.texRect.height;
        else if ((texRectWidth * tileHeight) <= maxTexels)
            height = tileHeight;
        else if ((texRectWidth * texRectHeight) <= maxTexels)
            height = gDP.texRect.height;
        else if (gDP.loadType == LOADTYPE_TILE)
            height = loadHeight;
        else
            height = lineHeight;
    }
    else
    {
        if (gSP.textureTile[t]->masks && ((maskWidth * maskHeight) <= maxTexels))
            width = maskWidth;
        else if ((tileWidth * tileHeight) <= maxTexels)
            width = tileWidth;
        else if (gDP.loadType == LOADTYPE_TILE)
            width = loadWidth;
        else
            width = lineWidth;

        if (gSP.textureTile[t]->maskt && ((maskWidth * maskHeight) <= maxTexels))
            height = maskHeight;
        else if ((tileWidth * tileHeight) <= maxTexels)
            height = tileHeight;
        else if (gDP.loadType == LOADTYPE_TILE)
            height = loadHeight;
        else
            height = lineHeight;
    }

    clampWidth = gSP.textureTile[t]->clamps ? tileWidth : width;
    clampHeight = gSP.textureTile[t]->clampt ? tileHeight : height;

    if (clampWidth > 256)
        gSP.textureTile[t]->clamps = 0;
    if (clampHeight > 256)
        gSP.textureTile[t]->clampt = 0;

    // Make sure masking is valid
    if (maskWidth > width)
    {
        gSP.textureTile[t]->masks = powof( width );
        maskWidth = 1 << gSP.textureTile[t]->masks;
    }

    if (maskHeight > height)
    {
        gSP.textureTile[t]->maskt = powof( height );
        maskHeight = 1 << gSP.textureTile[t]->maskt;
    }

    crc = TextureCache_CalculateCRC( t, width, height );

    //before we traverse cache, check to see if texture is already bound:
    if (_texture_compare(t, cache.current[t], crc, width, height, clampWidth, clampHeight))
    {
        cache.hits++;
        return;
    }

#ifdef __HASHMAP_OPT
    CachedTexture *tex = cache.hash.find(crc);
    if (tex)
    {
        if (_texture_compare(t, tex, crc, width, height, clampWidth, clampHeight))
        {
            TextureCache_ActivateTexture( t, tex);
            cache.hits++;
            return;
        }
    }
#endif

    current = cache.top;
    while (current)
    {
        if  (_texture_compare(t, current, crc, width, height, clampWidth, clampHeight))
        {
            TextureCache_ActivateTexture( t, current );
            cache.hits++;
            return;
        }

        current = current->lower;
    }

    cache.misses++;

    glActiveTexture( GL_TEXTURE0 + t);

    cache.current[t] = TextureCache_AddTop();

    if (cache.current[t] == NULL)
    {
        LOG(LOG_ERROR, "Texture Cache Failure\n");
    }

    glBindTexture( GL_TEXTURE_2D, cache.current[t]->glName );

    cache.current[t]->address = gDP.textureImage.address;
    cache.current[t]->crc = crc;

    cache.current[t]->format = gSP.textureTile[t]->format;
    cache.current[t]->size = gSP.textureTile[t]->size;

    cache.current[t]->width = width;
    cache.current[t]->height = height;
    cache.current[t]->clampWidth = clampWidth;
    cache.current[t]->clampHeight = clampHeight;
    cache.current[t]->palette = gSP.textureTile[t]->palette;
    cache.current[t]->maskS = gSP.textureTile[t]->masks;
    cache.current[t]->maskT = gSP.textureTile[t]->maskt;
    cache.current[t]->mirrorS = gSP.textureTile[t]->mirrors;
    cache.current[t]->mirrorT = gSP.textureTile[t]->mirrort;
    cache.current[t]->clampS = gSP.textureTile[t]->clamps;
    cache.current[t]->clampT = gSP.textureTile[t]->clampt;
    cache.current[t]->line = gSP.textureTile[t]->line;
    cache.current[t]->tMem = gSP.textureTile[t]->tmem;
    cache.current[t]->lastDList = RSP.DList;


    if (cache.current[t]->clampS)
        cache.current[t]->realWidth = (config.texture.pow2) ? pow2(clampWidth) : clampWidth;
    else if (cache.current[t]->mirrorS)
        cache.current[t]->realWidth = maskWidth << 1;
    else
        cache.current[t]->realWidth = (config.texture.pow2) ? pow2(width) : width;

    if (cache.current[t]->clampT)
        cache.current[t]->realHeight = (config.texture.pow2) ? pow2(clampHeight) : clampHeight;
    else if (cache.current[t]->mirrorT)
        cache.current[t]->realHeight = maskHeight << 1;
    else
        cache.current[t]->realHeight = (config.texture.pow2) ? pow2(height) : height;


    cache.current[t]->scaleS = 1.0f / (f32)(cache.current[t]->realWidth);
    cache.current[t]->scaleT = 1.0f / (f32)(cache.current[t]->realHeight);

    // Hack for Zelda Sun
    if ((config.hackZelda) && (gDP.combine.mux == 0x00262a60150c937fLL))
    {
        if ((cache.current[t]->format = G_IM_FMT_I) && (cache.current[t]->size == G_IM_SIZ_8b) &&
            (cache.current[t]->width == 64))
        {
            cache.current[t]->scaleS *= 0.5f;
            cache.current[t]->scaleT *= 0.5f;
        }
    }

    cache.current[t]->shiftScaleS = 1.0f;
    cache.current[t]->shiftScaleT = 1.0f;

    cache.current[t]->offsetS = config.texture.sai2x ? 0.25f : 0.5f;
    cache.current[t]->offsetT = config.texture.sai2x ? 0.25f : 0.5f;

    if (gSP.textureTile[t]->shifts > 10)
        cache.current[t]->shiftScaleS = (f32)(1 << (16 - gSP.textureTile[t]->shifts));
    else if (gSP.textureTile[t]->shifts > 0)
        cache.current[t]->shiftScaleS /= (f32)(1 << gSP.textureTile[t]->shifts);

    if (gSP.textureTile[t]->shiftt > 10)
        cache.current[t]->shiftScaleT = (f32)(1 << (16 - gSP.textureTile[t]->shiftt));
    else if (gSP.textureTile[t]->shiftt > 0)
        cache.current[t]->shiftScaleT /= (f32)(1 << gSP.textureTile[t]->shiftt);

    TextureCache_Load( cache.current[t] );
    TextureCache_ActivateTexture( t, cache.current[t] );

    cache.cachedBytes += cache.current[t]->textureBytes;
}

void TextureCache_ActivateNoise(u32 t)
{
    glActiveTexture(GL_TEXTURE0 + t);
    glBindTexture(GL_TEXTURE_2D, cache.glNoiseNames[RSP.DList & 0x1F]);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT );
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT );
}

