#include <stdlib.h>

#include "gles2N64.h"
#include "N64.h"
#include "GBI.h"
#include "RSP.h"
#include "gDP.h"
#include "gSP.h"
#include "Types.h"
#include "Debug.h"
#include "convert.h"
#include "OpenGL.h"
#include "CRC.h"
#include "DepthBuffer.h"
#include "VI.h"
#include "Config.h"


//thank rice_video for this:
bool _IsRenderTexture()
{
    bool foundSetScissor=false;
    bool foundFillRect=false;
    bool foundSetFillColor=false;
    bool foundSetCImg=false;
    bool foundTxtRect=false;
    int height;
    unsigned int newFillColor = 0;
    unsigned int dwPC = RSP.PC[RSP.PCi];       // This points to the next instruction

    for(int i=0; i<10; i++ )
    {
        unsigned int w0 = *(unsigned int *)(RDRAM + dwPC + i*8);
        unsigned int w1 = *(unsigned int *)(RDRAM + dwPC + 4 + i*8);

        if ((w0>>24) == G_SETSCISSOR)
        {
            height = ((w1>>0 )&0xFFF)/4;
            foundSetScissor = true;
            continue;
        }

        if ((w0>>24) == G_SETFILLCOLOR)
        {
            height = ((w1>>0 )&0xFFF)/4;
            foundSetFillColor = true;
            newFillColor = w1;
            continue;
        }

        if ((w0>>24) == G_FILLRECT)
        {
            unsigned int x0 = ((w1>>12)&0xFFF)/4;
            unsigned int y0 = ((w1>>0 )&0xFFF)/4;
            unsigned int x1 = ((w0>>12)&0xFFF)/4;
            unsigned int y1 = ((w0>>0 )&0xFFF)/4;

            if (x0 == 0 && y0 == 0)
            {
                if( x1 == gDP.colorImage.width)
                {
                    height = y1;
                    foundFillRect = true;
                    continue;
                }

                if(x1 == (unsigned int)(gDP.colorImage.width-1))
                {
                    height = y1+1;
                    foundFillRect = true;
                    continue;
                }
            }
        }

        if ((w0>>24) == G_TEXRECT)
        {
            foundTxtRect = true;
            break;
        }

        if ((w0>>24) == G_SETCIMG)
        {
            foundSetCImg = true;
            break;
        }
    }

    if (foundFillRect )
    {
        if (foundSetFillColor)
        {
            if (newFillColor != 0xFFFCFFFC)
                return true;    // this is a render_texture
            else
                return false;
        }

        if (gDP.fillColor.i == 0x00FFFFF7)
            return true;    // this is a render_texture
        else
            return false;   // this is a normal ZImg
    }
    else if (foundSetFillColor && newFillColor == 0xFFFCFFFC && foundSetCImg )
    {
        return false;
    }
    else
        return true;


    if (!foundSetCImg) return true;

    if (foundSetScissor ) return true;

    return false;
}

gDPInfo gDP;

void gDPSetOtherMode( u32 mode0, u32 mode1 )
{
    gDP.otherMode.h = mode0;
    gDP.otherMode.l = mode1;
    gDP.changed |= CHANGED_RENDERMODE | CHANGED_CYCLETYPE | CHANGED_ALPHACOMPARE;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetOtherMode( %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s, %s | %s | %s%s%s%s%s | %s | %s%s%s );\n",
        AlphaDitherText[gDP.otherMode.alphaDither],
        ColorDitherText[gDP.otherMode.colorDither],
        CombineKeyText[gDP.otherMode.combineKey],
        TextureConvertText[gDP.otherMode.textureConvert],
        TextureFilterText[gDP.otherMode.textureFilter],
        TextureLUTText[gDP.otherMode.textureLUT],
        TextureLODText[gDP.otherMode.textureLOD],
        TextureDetailText[gDP.otherMode.textureDetail],
        TexturePerspText[gDP.otherMode.texturePersp],
        CycleTypeText[gDP.otherMode.cycleType],
        PipelineModeText[gDP.otherMode.pipelineMode],
        AlphaCompareText[gDP.otherMode.alphaCompare],
        DepthSourceText[gDP.otherMode.depthSource],
        gDP.otherMode.AAEnable ? "AA_EN | " : "",
        gDP.otherMode.depthCompare ? "Z_CMP | " : "",
        gDP.otherMode.depthUpdate ? "Z_UPD | " : "",
        gDP.otherMode.imageRead ? "IM_RD | " : "",
        CvgDestText[gDP.otherMode.cvgDest],
        DepthModeText[gDP.otherMode.depthMode],
        gDP.otherMode.cvgXAlpha ? "CVG_X_ALPHA | " : "",
        gDP.otherMode.alphaCvgSel ? "ALPHA_CVG_SEL | " : "",
        gDP.otherMode.forceBlender ? "FORCE_BL" : "" );
#endif
}

void gDPSetPrimDepth( u16 z, u16 dz )
{
    z = z&0x7FFF;

    //gDP.primDepth.z = (_FIXED2FLOAT( z, 15 ) - gSP.viewport.vtrans[2]) / gSP.viewport.vscale[2] ;
    gDP.primDepth.z = (z - gSP.viewport.vtrans[2]) / gSP.viewport.vscale[2] ;
    gDP.primDepth.deltaZ = dz;
    gDP.changed |= CHANGED_PRIMITIVEZ;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetPrimDepth( %f, %f );\n",
        gDP.primDepth.z,
        gDP.primDepth.deltaZ);
#endif
}

void gDPPipelineMode( u32 mode )
{
    gDP.otherMode.pipelineMode = mode;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPPipelineMode( %s );\n",
        PipelineModeText[gDP.otherMode.pipelineMode] );
#endif
}

void gDPSetCycleType( u32 type )
{
    gDP.otherMode.cycleType = type;
    gDP.changed |= CHANGED_CYCLETYPE;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetCycleType( %s );\n",
        CycleTypeText[gDP.otherMode.cycleType] );
#endif
}

void gDPSetTexturePersp( u32 enable )
{
    gDP.otherMode.texturePersp = enable;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPSetTexturePersp( %s );\n",
        TexturePerspText[gDP.otherMode.texturePersp] );
#endif
}

void gDPSetTextureDetail( u32 type )
{
    gDP.otherMode.textureDetail = type;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPSetTextureDetail( %s );\n",
        TextureDetailText[gDP.otherMode.textureDetail] );
#endif
}

void gDPSetTextureLOD( u32 mode )
{
    gDP.otherMode.textureLOD = mode;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPSetTextureLOD( %s );\n",
        TextureLODText[gDP.otherMode.textureLOD] );
#endif
}

void gDPSetTextureLUT( u32 mode )
{
    gDP.otherMode.textureLUT = mode;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPSetTextureLUT( %s );\n",
        TextureLUTText[gDP.otherMode.textureLUT] );
#endif
}

void gDPSetTextureFilter( u32 type )
{
    gDP.otherMode.textureFilter = type;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPSetTextureFilter( %s );\n",
        TextureFilterText[gDP.otherMode.textureFilter] );
#endif
}

void gDPSetTextureConvert( u32 type )
{
    gDP.otherMode.textureConvert = type;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPSetTextureConvert( %s );\n",
        TextureConvertText[gDP.otherMode.textureConvert] );
#endif
}

void gDPSetCombineKey( u32 type )
{
    gDP.otherMode.combineKey = type;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_COMBINE, "gDPSetCombineKey( %s );\n",
        CombineKeyText[gDP.otherMode.combineKey] );
#endif
}

void gDPSetColorDither( u32 type )
{
    gDP.otherMode.colorDither = type;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetColorDither( %s );\n",
        ColorDitherText[gDP.otherMode.colorDither] );
#endif
}

void gDPSetAlphaDither( u32 type )
{
    gDP.otherMode.alphaDither = type;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetAlphaDither( %s );\n",
        AlphaDitherText[gDP.otherMode.alphaDither] );
#endif
}

void gDPSetAlphaCompare( u32 mode )
{
    gDP.otherMode.alphaCompare = mode;
    gDP.changed |= CHANGED_ALPHACOMPARE;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetAlphaCompare( %s );\n",
        AlphaCompareText[gDP.otherMode.alphaCompare] );
#endif
}

void gDPSetDepthSource( u32 source )
{
    gDP.otherMode.depthSource = source;
    gDP.changed |= CHANGED_DEPTHSOURCE;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetDepthSource( %s );\n",
        DepthSourceText[gDP.otherMode.depthSource] );
#endif
}

void gDPSetRenderMode( u32 mode1, u32 mode2 )
{
    gDP.otherMode.l &= 0x00000007;
    gDP.otherMode.l |= mode1 | mode2;
    gDP.changed |= CHANGED_RENDERMODE;

#ifdef DEBUG
    // THIS IS INCOMPLETE!!!
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetRenderMode( %s%s%s%s%s | %s | %s%s%s );\n",
        gDP.otherMode.AAEnable ? "AA_EN | " : "",
        gDP.otherMode.depthCompare ? "Z_CMP | " : "",
        gDP.otherMode.depthUpdate ? "Z_UPD | " : "",
        gDP.otherMode.imageRead ? "IM_RD | " : "",
        CvgDestText[gDP.otherMode.cvgDest],
        DepthModeText[gDP.otherMode.depthMode],
        gDP.otherMode.cvgXAlpha ? "CVG_X_ALPHA | " : "",
        gDP.otherMode.alphaCvgSel ? "ALPHA_CVG_SEL | " : "",
        gDP.otherMode.forceBlender ? "FORCE_BL" : "" );
#endif
}

void gDPSetCombine( s32 muxs0, s32 muxs1 )
{
    gDP.combine.muxs0 = muxs0;
    gDP.combine.muxs1 = muxs1;
    gDP.changed |= CHANGED_COMBINE;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_COMBINE, "gDPSetCombine( %s, %s, %s, %s, %s, %s, %s, %s,\n",
        saRGBText[gDP.combine.saRGB0],
        sbRGBText[gDP.combine.sbRGB0],
        mRGBText[gDP.combine.mRGB0],
        aRGBText[gDP.combine.aRGB0],
        saAText[gDP.combine.saA0],
        sbAText[gDP.combine.sbA0],
        mAText[gDP.combine.mA0],
        aAText[gDP.combine.aA0] );

    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_COMBINE, "               %s, %s, %s, %s, %s, %s, %s, %s );\n",
        saRGBText[gDP.combine.saRGB1],
        sbRGBText[gDP.combine.sbRGB1],
        mRGBText[gDP.combine.mRGB1],
        aRGBText[gDP.combine.aRGB1],
        saAText[gDP.combine.saA1],
        sbAText[gDP.combine.sbA1],
        mAText[gDP.combine.mA1],
        aAText[gDP.combine.aA1] );

#endif
}

void gDPSetColorImage( u32 format, u32 size, u32 width, u32 address )
{
    if (config.updateMode == SCREEN_UPDATE_AT_CI_CHANGE)
        OGL_SwapBuffers();

    if (config.updateMode == SCREEN_UPDATE_AT_1ST_CI_CHANGE && OGL.screenUpdate)
        OGL_SwapBuffers();

    u32 addr = RSP_SegmentToPhysical( address );

    if (gDP.colorImage.address != addr)
    {
        gDP.colorImage.changed = FALSE;
        if (width == VI.width)
            gDP.colorImage.height = VI.height;
        else
            gDP.colorImage.height = 1;
    }

    gDP.colorImage.format = format;
    gDP.colorImage.size = size;
    gDP.colorImage.width = width;
    gDP.colorImage.address = addr;

    if (config.ignoreOffscreenRendering)
    {
        int i;

        //colorimage byte size:
        //color image height is not the best thing to base this on, its normally set
        //later on in the code

        if (gDP.colorImage.address == gDP.depthImageAddress)
        {
            OGL.renderingToTexture = false;
        }
        else if (size == G_IM_SIZ_16b && format == G_IM_FMT_RGBA)
        {
            int s = 0;
            switch(size)
            {
                case G_IM_SIZ_4b:   s = (gDP.colorImage.width * gDP.colorImage.height) / 2; break;
                case G_IM_SIZ_8b:   s = (gDP.colorImage.width * gDP.colorImage.height); break;
                case G_IM_SIZ_16b:  s = (gDP.colorImage.width * gDP.colorImage.height) * 2; break;
                case G_IM_SIZ_32b:  s = (gDP.colorImage.width * gDP.colorImage.height) * 4; break;
            }
            u32 start = addr & 0x00FFFFFF;
            u32 end = min(start + s, RDRAMSize);
            for(i = 0; i < VI.displayNum; i++)
            {
                if (VI.display[i].start <= end && VI.display[i].start >= start) break;
                if (start <= VI.display[i].end && start >= VI.display[i].start) break;
            }

            OGL.renderingToTexture = (i == VI.displayNum);
        }
        else
        {
            OGL.renderingToTexture = true;
        }

#if 0
        if (OGL.renderingToTexture)
        {
            printf("start=%i end=%i\n", start, end);
            printf("display=");
            for(int i=0; i< VI.displayNum; i++) printf("%i,%i:", VI.display[i].start, VI.display[i].end);
            printf("\n");
        }
#endif
    }
    else
    {
        OGL.renderingToTexture = false;
    }


#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetColorImage( %s, %s, %i, 0x%08X );\n",
        ImageFormatText[gDP.colorImage.format],
        ImageSizeText[gDP.colorImage.size],
        gDP.colorImage.width,
        gDP.colorImage.address );
#endif
}

void gDPSetTextureImage( u32 format, u32 size, u32 width, u32 address )
{
    gDP.textureImage.format = format;
    gDP.textureImage.size = size;
    gDP.textureImage.width = width;
    gDP.textureImage.address = RSP_SegmentToPhysical( address );
    gDP.textureImage.bpl = gDP.textureImage.width << gDP.textureImage.size >> 1;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPSetTextureImage( %s, %s, %i, 0x%08X );\n",
        ImageFormatText[gDP.textureImage.format],
        ImageSizeText[gDP.textureImage.size],
        gDP.textureImage.width,
        gDP.textureImage.address );
#endif
}

void gDPSetDepthImage( u32 address )
{
//  if (address != gDP.depthImageAddress)
//      OGL_ClearDepthBuffer();

    u32 addr = RSP_SegmentToPhysical(address);
    DepthBuffer_SetBuffer(addr);

    if (depthBuffer.current->cleared)
        OGL_ClearDepthBuffer();

    gDP.depthImageAddress = addr;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetDepthImage( 0x%08X );\n", gDP.depthImageAddress );
#endif
}

void gDPSetEnvColor( u32 r, u32 g, u32 b, u32 a )
{
    gDP.envColor.r = r * 0.0039215689f;
    gDP.envColor.g = g * 0.0039215689f;
    gDP.envColor.b = b * 0.0039215689f;
    gDP.envColor.a = a * 0.0039215689f;

    gDP.changed |= CHANGED_ENV_COLOR;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_COMBINE, "gDPSetEnvColor( %i, %i, %i, %i );\n",
        r, g, b, a );
#endif
}

void gDPSetBlendColor( u32 r, u32 g, u32 b, u32 a )
{
    gDP.blendColor.r = r * 0.0039215689f;
    gDP.blendColor.g = g * 0.0039215689f;
    gDP.blendColor.b = b * 0.0039215689f;
    gDP.blendColor.a = a * 0.0039215689f;
    gDP.changed |= CHANGED_BLENDCOLOR;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetBlendColor( %i, %i, %i, %i );\n",
        r, g, b, a );
#endif
}

void gDPSetFogColor( u32 r, u32 g, u32 b, u32 a )
{
    gDP.fogColor.r = r * 0.0039215689f;
    gDP.fogColor.g = g * 0.0039215689f;
    gDP.fogColor.b = b * 0.0039215689f;
    gDP.fogColor.a = a * 0.0039215689f;

    gDP.changed |= CHANGED_FOGCOLOR;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetFogColor( %i, %i, %i, %i );\n",
        r, g, b, a );
#endif
}

void gDPSetFillColor( u32 c )
{

    gDP.fillColor.i = c;
    gDP.fillColor.r = _SHIFTR( c, 11, 5 ) * 0.032258064f;
    gDP.fillColor.g = _SHIFTR( c,  6, 5 ) * 0.032258064f;
    gDP.fillColor.b = _SHIFTR( c,  1, 5 ) * 0.032258064f;
    gDP.fillColor.a = _SHIFTR( c,  0, 1 );

    gDP.fillColor.z = _SHIFTR( c,  2, 14 );
    gDP.fillColor.dz = _SHIFTR( c, 0, 2 );

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPSetFillColor( 0x%08X );\n", c );
#endif
}

void gDPSetPrimColor( u32 m, u32 l, u32 r, u32 g, u32 b, u32 a )
{
    gDP.primColor.m = m;
    gDP.primColor.l = l * 0.0039215689f;
    gDP.primColor.r = r * 0.0039215689f;
    gDP.primColor.g = g * 0.0039215689f;
    gDP.primColor.b = b * 0.0039215689f;
    gDP.primColor.a = a * 0.0039215689f;

    gDP.changed |= CHANGED_PRIM_COLOR;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_COMBINE, "gDPSetPrimColor( %i, %i, %i, %i, %i, %i );\n",
        m, l, r, g, b, a );
#endif
}

void gDPSetTile( u32 format, u32 size, u32 line, u32 tmem, u32 tile, u32 palette, u32 cmt, u32 cms, u32 maskt, u32 masks, u32 shiftt, u32 shifts )
{
    if (((size == G_IM_SIZ_4b) || (size == G_IM_SIZ_8b)) && (format == G_IM_FMT_RGBA))
        format = G_IM_FMT_CI;

    gDP.tiles[tile].format = format;
    gDP.tiles[tile].size = size;
    gDP.tiles[tile].line = line;
    gDP.tiles[tile].tmem = tmem;
    gDP.tiles[tile].palette = palette;
    gDP.tiles[tile].cmt = cmt;
    gDP.tiles[tile].cms = cms;
    gDP.tiles[tile].maskt = maskt;
    gDP.tiles[tile].masks = masks;
    gDP.tiles[tile].shiftt = shiftt;
    gDP.tiles[tile].shifts = shifts;

    if (!gDP.tiles[tile].masks) gDP.tiles[tile].clamps = 1;
    if (!gDP.tiles[tile].maskt) gDP.tiles[tile].clampt = 1;
}

void gDPSetTileSize( u32 tile, u32 uls, u32 ult, u32 lrs, u32 lrt )
{
    gDP.tiles[tile].uls = _SHIFTR( uls, 2, 10 );
    gDP.tiles[tile].ult = _SHIFTR( ult, 2, 10 );
    gDP.tiles[tile].lrs = _SHIFTR( lrs, 2, 10 );
    gDP.tiles[tile].lrt = _SHIFTR( lrt, 2, 10 );

    gDP.tiles[tile].fuls = _FIXED2FLOAT( uls, 2 );
    gDP.tiles[tile].fult = _FIXED2FLOAT( ult, 2 );
    gDP.tiles[tile].flrs = _FIXED2FLOAT( lrs, 2 );
    gDP.tiles[tile].flrt = _FIXED2FLOAT( lrt, 2 );

    gDP.changed |= CHANGED_TILE;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPSetTileSize( %i, %.2f, %.2f, %.2f, %.2f );\n",
        tile,
        gDP.tiles[tile].fuls,
        gDP.tiles[tile].fult,
        gDP.tiles[tile].flrs,
        gDP.tiles[tile].flrt );
#endif
}

void gDPLoadTile( u32 tile, u32 uls, u32 ult, u32 lrs, u32 lrt )
{
    void (*Interleave)( void *mem, u32 numDWords );

    u32 address, height, bpl, line, y;
    u64 *dest;
    u8 *src;

    gDPSetTileSize( tile, uls, ult, lrs, lrt );
    gDP.loadTile = &gDP.tiles[tile];

    if (gDP.loadTile->line == 0)
        return;

    address = gDP.textureImage.address + gDP.loadTile->ult * gDP.textureImage.bpl + (gDP.loadTile->uls << gDP.textureImage.size >> 1);
    dest = &TMEM[gDP.loadTile->tmem];
    bpl = (gDP.loadTile->lrs - gDP.loadTile->uls + 1) << gDP.loadTile->size >> 1;
    height = gDP.loadTile->lrt - gDP.loadTile->ult + 1;
    src = &RDRAM[address];

    if (((address + height * bpl) > RDRAMSize) ||
        (((gDP.loadTile->tmem << 3) + bpl * height) > 4096)) // Stay within TMEM
    {
#ifdef DEBUG
        DebugMsg( DEBUG_HIGH | DEBUG_ERROR | DEBUG_TEXTURE, "// Attempting to load texture tile out of range\n" );
        DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPLoadTile( %i, %i, %i, %i, %i );\n",
            tile, gDP.loadTile->uls, gDP.loadTile->ult, gDP.loadTile->lrs, gDP.loadTile->lrt );
#endif
        return;
    }

    // Line given for 32-bit is half what it seems it should since they split the
    // high and low words. I'm cheating by putting them together.
    if (gDP.loadTile->size == G_IM_SIZ_32b)
    {
        line = gDP.loadTile->line << 1;
        Interleave = QWordInterleave;
    }
    else
    {
        line = gDP.loadTile->line;
        Interleave = DWordInterleave;
    }

    for (y = 0; y < height; y++)
    {
        UnswapCopy( src, dest, bpl );
        if (y & 1) Interleave( dest, line );

        src += gDP.textureImage.bpl;
        dest += line;
    }

    gDP.textureMode = TEXTUREMODE_NORMAL;
    gDP.loadType = LOADTYPE_TILE;
    gDP.changed |= CHANGED_TMEM;

#ifdef DEBUG
        DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPLoadTile( %i, %i, %i, %i, %i );\n",
            tile, gDP.loadTile->uls, gDP.loadTile->ult, gDP.loadTile->lrs, gDP.loadTile->lrt );
#endif
}

void gDPLoadBlock( u32 tile, u32 uls, u32 ult, u32 lrs, u32 dxt )
{
    gDPSetTileSize( tile, uls, ult, lrs, dxt );
    gDP.loadTile = &gDP.tiles[tile];

    u32 bytes = (lrs + 1) << gDP.loadTile->size >> 1;
    u32 address = gDP.textureImage.address + ult * gDP.textureImage.bpl + (uls << gDP.textureImage.size >> 1);

    if ((bytes == 0) ||
        ((address + bytes) > RDRAMSize) ||
        (((gDP.loadTile->tmem << 3) + bytes) > 4096))
    {
#ifdef DEBUG
        DebugMsg( DEBUG_HIGH | DEBUG_ERROR | DEBUG_TEXTURE, "// Attempting to load texture block out of range\n" );
        DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPLoadBlock( %i, %i, %i, %i, %i );\n",
            tile, uls, ult, lrs, dxt );
#endif
//      bytes = min( bytes, min( RDRAMSize - gDP.textureImage.address, 4096 - (gDP.loadTile->tmem << 3) ) );
        return;
    }

    u64* src = (u64*)&RDRAM[address];
    u64* dest = &TMEM[gDP.loadTile->tmem];

    if (dxt > 0)
    {
        u32 line = (2047 + dxt) / dxt;
        u32 bpl = line << 3;
        u32 height = bytes / bpl;

        if (gDP.loadTile->size == G_IM_SIZ_32b)
        {
            for (u32 y = 0; y < height; y++)
            {
                UnswapCopy( src, dest, bpl );
                if (y & 1) QWordInterleave( dest, line );
                src += line;
                dest += line;
            }
        }
        else
        {
            for (u32 y = 0; y < height; y++)
            {
                UnswapCopy( src, dest, bpl );
                if (y & 1) DWordInterleave( dest, line );
                src += line;
                dest += line;
            }

        }

    }
    else
        UnswapCopy( src, dest, bytes );

    gDP.textureMode = TEXTUREMODE_NORMAL;
    gDP.loadType = LOADTYPE_BLOCK;
    gDP.changed |= CHANGED_TMEM;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPLoadBlock( %i, %i, %i, %i, %i );\n",
        tile, uls, ult, lrs, dxt );
#endif
}

void gDPLoadTLUT( u32 tile, u32 uls, u32 ult, u32 lrs, u32 lrt )
{
    gDPSetTileSize( tile, uls, ult, lrs, lrt );

    u16 count = (gDP.tiles[tile].lrs - gDP.tiles[tile].uls + 1) * (gDP.tiles[tile].lrt - gDP.tiles[tile].ult + 1);
    u32 address = gDP.textureImage.address + gDP.tiles[tile].ult * gDP.textureImage.bpl + (gDP.tiles[tile].uls << gDP.textureImage.size >> 1);

    u16 *dest = (u16*)&TMEM[gDP.tiles[tile].tmem];
    u16 *src = (u16*)&RDRAM[address];

    u16 pal = (gDP.tiles[tile].tmem - 256) >> 4;

    int i = 0;
    while (i < count)
    {
        for (u16 j = 0; (j < 16) && (i < count); j++, i++)
        {
            u16 color = swapword( src[i^1] );

            *dest = color;
            //dest[1] = color;
            //dest[2] = color;
            //dest[3] = color;

            dest += 4;
        }

        gDP.paletteCRC16[pal] = CRC_CalculatePalette( 0xFFFFFFFF, &TMEM[256 + (pal << 4)], 16 );
        pal++;
    }

    gDP.paletteCRC256 = CRC_Calculate( 0xFFFFFFFF, gDP.paletteCRC16, 64 );

    gDP.changed |= CHANGED_TMEM;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED | DEBUG_TEXTURE, "gDPLoadTLUT( %i, %i, %i, %i, %i );\n",
        tile, gDP.tiles[tile].uls, gDP.tiles[tile].ult, gDP.tiles[tile].lrs, gDP.tiles[tile].lrt );
#endif
}

void gDPSetScissor( u32 mode, f32 ulx, f32 uly, f32 lrx, f32 lry )
{
    gDP.scissor.mode = mode;
    gDP.scissor.ulx = ulx;
    gDP.scissor.uly = uly;
    gDP.scissor.lrx = lrx;
    gDP.scissor.lry = lry;
    gDP.changed |= CHANGED_SCISSOR;

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_IGNORED, "gDPSetScissor( %s, %.2f, %.2f, %.2f, %.2f );\n",
        ScissorModeText[gDP.scissor.mode],
        gDP.scissor.ulx,
        gDP.scissor.uly,
        gDP.scissor.lrx,
        gDP.scissor.lry );
#endif
}

void gDPFillRectangle( s32 ulx, s32 uly, s32 lrx, s32 lry )
{
    DepthBuffer *buffer = DepthBuffer_FindBuffer( gDP.colorImage.address );

    if (buffer)
        buffer->cleared = TRUE;

    if (gDP.depthImageAddress == gDP.colorImage.address)
    {
        OGL_ClearDepthBuffer();
        return;
    }

    if (gDP.otherMode.cycleType == G_CYC_FILL)
    {
        lrx++;
        lry++;

        if ((ulx == 0) && (uly == 0) && ((unsigned int)lrx == VI.width) && ((unsigned int)lry == VI.height))
        {
            OGL_ClearColorBuffer( &gDP.fillColor.r );
            return;
        }
    }

    //shouldn't this be primitive color?
    //OGL_DrawRect( ulx, uly, lrx, lry, (gDP.otherMode.cycleType == G_CYC_FILL) ? &gDP.fillColor.r : &gDP.blendColor.r );
    //OGL_DrawRect( ulx, uly, lrx, lry, (gDP.otherMode.cycleType == G_CYC_FILL) ? &gDP.fillColor.r : &gDP.primColor.r);

    float black[] = {0,0,0,0};
    OGL_DrawRect( ulx, uly, lrx, lry, (gDP.otherMode.cycleType == G_CYC_FILL) ? &gDP.fillColor.r : black);

    if (depthBuffer.current) depthBuffer.current->cleared = FALSE;
    gDP.colorImage.changed = TRUE;
    gDP.colorImage.height = max( gDP.colorImage.height, (unsigned int)lry );

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPFillRectangle( %i, %i, %i, %i );\n",
        ulx, uly, lrx, lry );
#endif
}

void gDPSetConvert( s32 k0, s32 k1, s32 k2, s32 k3, s32 k4, s32 k5 )
{
    gDP.convert.k0 = k0 * 0.0039215689f;
    gDP.convert.k1 = k1 * 0.0039215689f;
    gDP.convert.k2 = k2 * 0.0039215689f;
    gDP.convert.k3 = k3 * 0.0039215689f;
    gDP.convert.k4 = k4 * 0.0039215689f;
    gDP.convert.k5 = k5 * 0.0039215689f;
    gDP.changed |= CHANGED_CONVERT;
}

void gDPSetKeyR( u32 cR, u32 sR, u32 wR )
{
    gDP.key.center.r = cR * 0.0039215689f;;
    gDP.key.scale.r = sR * 0.0039215689f;;
    gDP.key.width.r = wR * 0.0039215689f;;
}

void gDPSetKeyGB(u32 cG, u32 sG, u32 wG, u32 cB, u32 sB, u32 wB )
{
    gDP.key.center.g = cG * 0.0039215689f;;
    gDP.key.scale.g = sG * 0.0039215689f;;
    gDP.key.width.g = wG * 0.0039215689f;;
    gDP.key.center.b = cB * 0.0039215689f;;
    gDP.key.scale.b = sB * 0.0039215689f;;
    gDP.key.width.b = wB * 0.0039215689f;;
}

void gDPTextureRectangle( f32 ulx, f32 uly, f32 lrx, f32 lry, s32 tile, f32 s, f32 t, f32 dsdx, f32 dtdy )
{
    if (gDP.colorImage.address == gDP.depthImageAddress)
    {
        return;
    }

    if (gDP.otherMode.cycleType == G_CYC_COPY)
    {
        dsdx = 1.0f;
        lrx += 1.0f;
        lry += 1.0f;
    }

    gSP.textureTile[0] = &gDP.tiles[tile];
    gSP.textureTile[1] = &gDP.tiles[(tile < 7) ? (tile + 1) : tile];


    f32 lrs;
    f32 lrt;
    if (RSP.cmd == G_TEXRECTFLIP)
    {
        lrs = s + (lry - uly - 1) * dtdy;
        lrt = t + (lrx - ulx - 1) * dsdx;
    }
    else
    {
        lrs = s + (lrx - ulx - 1) * dsdx;
        lrt = t + (lry - uly - 1) * dtdy;
    }

    if (gDP.textureMode == TEXTUREMODE_NORMAL)
        gDP.textureMode = TEXTUREMODE_TEXRECT;

    gDP.texRect.width = (unsigned int)(max( lrs, s ) + dsdx);
    gDP.texRect.height = (unsigned int)(max( lrt, t ) + dtdy);

    float tmp;
    if (lrs < s)
    {
        tmp = ulx; ulx = lrx; lrx = tmp;
        tmp = s; s = lrs; lrs = tmp;
    }
    if (lrt < t)
    {
        tmp = uly; uly = lry; lry = tmp;
        tmp = t; t = lrt; lrt = tmp;
    }

    OGL_DrawTexturedRect( ulx, uly, lrx, lry, s, t, lrs, lrt, (RSP.cmd == G_TEXRECTFLIP));

    gSP.textureTile[0] = &gDP.tiles[gSP.texture.tile];
    gSP.textureTile[1] = &gDP.tiles[(gSP.texture.tile < 7) ? (gSP.texture.tile + 1) : gSP.texture.tile];

    if (depthBuffer.current) depthBuffer.current->cleared = FALSE;
    gDP.colorImage.changed = TRUE;
    gDP.colorImage.height = (unsigned int)(max( gDP.colorImage.height, gDP.scissor.lry ));

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPTextureRectangle( %f, %f, %f, %f, %i, %i, %f, %f, %f, %f );\n",
        ulx, uly, lrx, lry, tile, s, t, dsdx, dtdy );
#endif
}

void gDPTextureRectangleFlip( f32 ulx, f32 uly, f32 lrx, f32 lry, s32 tile, f32 s, f32 t, f32 dsdx, f32 dtdy )
{
    //gDPTextureRectangle( ulx, uly, lrx, lry, tile, s + (lrx - ulx) * dsdx, t + (lry - uly) * dtdy, -dsdx, -dtdy );

    gDPTextureRectangle( ulx, uly, lrx, lry, tile, s, t, dsdx, dtdy );
#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPTextureRectangleFlip( %f, %f, %f, %f, %i, %i, %f, %f, %f, %f );\n",
        ulx, uly, lrx, lry, tile, s, t, dsdx, dtdy );
#endif
}

void gDPFullSync()
{
    *REG.MI_INTR |= MI_INTR_DP;

    CheckInterrupts();

#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gDPFullSync();\n" );
#endif
}

void gDPTileSync()
{
#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_IGNORED | DEBUG_TEXTURE, "gDPTileSync();\n" );
#endif
}

void gDPPipeSync()
{
#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_IGNORED, "gDPPipeSync();\n" );
#endif
}

void gDPLoadSync()
{
#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_IGNORED, "gDPLoadSync();\n" );
#endif
}

void gDPNoOp()
{
#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_IGNORED, "gDPNoOp();\n" );
#endif
}

