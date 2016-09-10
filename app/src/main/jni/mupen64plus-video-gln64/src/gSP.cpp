#include <math.h>
#include <stdlib.h>

#include "Common.h"
#include "gles2N64.h"
#include "Debug.h"
#include "Types.h"
#include "RSP.h"
#include "GBI.h"
#include "gSP.h"
#include "gDP.h"
#include "3DMath.h"
#include "OpenGL.h"
#include "CRC.h"
#include <string.h>
#include "convert.h"
#include "S2DEX.h"
#include "VI.h"
#include "DepthBuffer.h"
#include "Config.h"

//Note: 0xC0 is used by 1080 alot, its an unknown command.

#ifdef DEBUG
extern u32 uc_crc, uc_dcrc;
extern char uc_str[256];
#endif

void gSPCombineMatrices();

//#ifdef __TRIBUFFER_OPT
void __indexmap_init()
{
    memset(OGL.triangles.indexmapinv, 0xFF, VERTBUFF_SIZE*sizeof(u32));
    for(int i=0;i<INDEXMAP_SIZE;i++)
    {
        OGL.triangles.indexmap[i] = i;
        //OGL.triangles.indexmapinv[i] = i;
    }

    OGL.triangles.indexmap_prev = -1;
    OGL.triangles.indexmap_nomap = 0;
}

void __indexmap_clear()
{
    memset(OGL.triangles.indexmapinv, 0xFF, VERTBUFF_SIZE * sizeof(u32));
    for(int i=0;i<INDEXMAP_SIZE;i++)
        OGL.triangles.indexmapinv[OGL.triangles.indexmap[i]] = i;
}

u32 __indexmap_findunused(u32 num)
{
    u32 c = 0;
    u32 i = min(OGL.triangles.indexmap_prev+1, VERTBUFF_SIZE-1);
    u32 n = 0;
    while(n < VERTBUFF_SIZE)
    {
        c = (OGL.triangles.indexmapinv[i] == 0xFFFFFFFF) ? (c+1) : 0;
        if ((c == num) && (i < (VERTBUFF_SIZE - num)))
        {
            break;
        }
        i=i+1;
        if (i >= VERTBUFF_SIZE) {i=0; c=0;}
        n++;
    }
    return (c == num) ? (i-num+1) : (0xFFFFFFFF);
}

void __indexmap_undomap()
{
    SPVertex tmp[INDEXMAP_SIZE];
    memset(OGL.triangles.indexmapinv, 0xFF, VERTBUFF_SIZE * sizeof(u32));

    for(int i=0;i<INDEXMAP_SIZE;i++)
    {
        u32 ind = OGL.triangles.indexmap[i];
        tmp[i] = OGL.triangles.vertices[ind];
        OGL.triangles.indexmap[i] = i;
        OGL.triangles.indexmapinv[i] = i;
    }

    memcpy(OGL.triangles.vertices, tmp, INDEXMAP_SIZE * sizeof(SPVertex));
    OGL.triangles.indexmap_nomap = 1;
}

u32 __indexmap_getnew(u32 index, u32 num)
{
    u32 ind;

    //test to see if unmapped
    u32 unmapped = 1;
    for(int i=0;i<num;i++)
    {
        if (OGL.triangles.indexmap[i]!=0xFFFFFFFF)
        {
            unmapped = 0;
            break;
        }

    }

    if (unmapped)
    {
        ind = index;
    }
    else
    {
        ind = __indexmap_findunused(num);

        //no more room in buffer....
        if (ind > VERTBUFF_SIZE)
        {
            OGL_DrawTriangles();
            ind = __indexmap_findunused(num);

            //OK the indices are spread so sparsely, we cannot find a num element block.
            if (ind > VERTBUFF_SIZE)
            {
                __indexmap_undomap();
                ind = __indexmap_findunused(num);
                if (ind > VERTBUFF_SIZE)
                {
                    LOG(LOG_ERROR, "Could not allocate %i indices\n", num);

                    LOG(LOG_VERBOSE, "indexmap=[");
                    for(int i=0;i<INDEXMAP_SIZE;i++)
                        LOG(LOG_VERBOSE, "%i,", OGL.triangles.indexmap[i]);
                    LOG(LOG_VERBOSE, "]\n");

                    LOG(LOG_VERBOSE, "indexmapinv=[");
                    for(int i=0;i<VERTBUFF_SIZE;i++)
                        LOG(LOG_VERBOSE, "%i,", OGL.triangles.indexmapinv[i]);
                    LOG(LOG_VERBOSE, "]\n");
                }
                return ind;
            }
        }
    }

    for(int i=0;i<num;i++)
    {
        OGL.triangles.indexmap[index+i] = ind+i;
        OGL.triangles.indexmapinv[ind+i] = index+i;
    }

    OGL.triangles.indexmap_prev = ind+num-1;
    OGL.triangles.indexmap_nomap = 0;

    return ind;
}
//#endif

void gSPTriangle(s32 v0, s32 v1, s32 v2)
{
    if ((v0 < INDEXMAP_SIZE) && (v1 < INDEXMAP_SIZE) && (v2 < INDEXMAP_SIZE))
    {

#ifdef __TRIBUFFER_OPT
        v0 = OGL.triangles.indexmap[v0];
        v1 = OGL.triangles.indexmap[v1];
        v2 = OGL.triangles.indexmap[v2];
#endif

#if 0
        // Don't bother with triangles completely outside clipping frustrum
        if (config.enableClipping)
        {
            if (OGL.triangles.vertices[v0].clip & OGL.triangles.vertices[v1].clip & OGL.triangles.vertices[v2].clip)
            {
                return;
            }
        }
#endif

        OGL_AddTriangle(v0, v1, v2);

    }

    if (depthBuffer.current) depthBuffer.current->cleared = FALSE;
    gDP.colorImage.changed = TRUE;
    gDP.colorImage.height = (unsigned int)(max( gDP.colorImage.height, gDP.scissor.lry ));
}

void gSP1Triangle( const s32 v0, const s32 v1, const s32 v2)
{
    gSPTriangle( v0, v1, v2);
    gSPFlushTriangles();
}

void gSP2Triangles(const s32 v00, const s32 v01, const s32 v02, const s32 flag0,
                    const s32 v10, const s32 v11, const s32 v12, const s32 flag1 )
{
    gSPTriangle( v00, v01, v02);
    gSPTriangle( v10, v11, v12);
    gSPFlushTriangles();
}

void gSP4Triangles(const s32 v00, const s32 v01, const s32 v02,
                    const s32 v10, const s32 v11, const s32 v12,
                    const s32 v20, const s32 v21, const s32 v22,
                    const s32 v30, const s32 v31, const s32 v32 )
{
    gSPTriangle(v00, v01, v02);
    gSPTriangle(v10, v11, v12);
    gSPTriangle(v20, v21, v22);
    gSPTriangle(v30, v31, v32);
    gSPFlushTriangles();
}


gSPInfo gSP;

f32 identityMatrix[4][4] =
{
    { 1.0f, 0.0f, 0.0f, 0.0f },
    { 0.0f, 1.0f, 0.0f, 0.0f },
    { 0.0f, 0.0f, 1.0f, 0.0f },
    { 0.0f, 0.0f, 0.0f, 1.0f }
};

#ifdef __VEC4_OPT
static void gSPTransformVertex4_default(u32 v, float mtx[4][4])
{
    float x, y, z, w;
    int i;
    for(i = 0; i < 4; i++)
    {
        x = OGL.triangles.vertices[v+i].x;
        y = OGL.triangles.vertices[v+i].y;
        z = OGL.triangles.vertices[v+i].z;
        w = OGL.triangles.vertices[v+i].w;
        OGL.triangles.vertices[v+i].x = x * mtx[0][0] + y * mtx[1][0] + z * mtx[2][0] + mtx[3][0];
        OGL.triangles.vertices[v+i].y = x * mtx[0][1] + y * mtx[1][1] + z * mtx[2][1] + mtx[3][1];
        OGL.triangles.vertices[v+i].z = x * mtx[0][2] + y * mtx[1][2] + z * mtx[2][2] + mtx[3][2];
        OGL.triangles.vertices[v+i].w = x * mtx[0][3] + y * mtx[1][3] + z * mtx[2][3] + mtx[3][3];
    }
}

void gSPClipVertex4(u32 v)
{
    int i;
    for(i = 0; i < 4; i++){
        SPVertex *vtx = &OGL.triangles.vertices[v+i];
        vtx->clip = 0;
        if (vtx->x > +vtx->w)   vtx->clip |= CLIP_POSX;
        if (vtx->x < -vtx->w)   vtx->clip |= CLIP_NEGX;
        if (vtx->y > +vtx->w)   vtx->clip |= CLIP_POSY;
        if (vtx->y < -vtx->w)   vtx->clip |= CLIP_NEGY;
    }
}

static void gSPTransformNormal4_default(u32 v, float mtx[4][4])
{
    float len, x, y, z;
    int i;
    for(i = 0; i < 4; i++){
        x = OGL.triangles.vertices[v+i].nx;
        y = OGL.triangles.vertices[v+i].ny;
        z = OGL.triangles.vertices[v+i].nz;

        OGL.triangles.vertices[v+i].nx = mtx[0][0]*x + mtx[1][0]*y + mtx[2][0]*z;
        OGL.triangles.vertices[v+i].ny = mtx[0][1]*x + mtx[1][1]*y + mtx[2][1]*z;
        OGL.triangles.vertices[v+i].nz = mtx[0][2]*x + mtx[1][2]*y + mtx[2][2]*z;
        len =   OGL.triangles.vertices[v+i].nx*OGL.triangles.vertices[v+i].nx +
                OGL.triangles.vertices[v+i].ny*OGL.triangles.vertices[v+i].ny +
                OGL.triangles.vertices[v+i].nz*OGL.triangles.vertices[v+i].nz;
        if (len != 0.0)
        {
            len = sqrtf(len);
            OGL.triangles.vertices[v+i].nx /= len;
            OGL.triangles.vertices[v+i].ny /= len;
            OGL.triangles.vertices[v+i].nz /= len;
        }
    }
}

static void gSPLightVertex4_default(u32 v)
{
    gSPTransformNormal4(v, gSP.matrix.modelView[gSP.matrix.modelViewi]);
    for(int j = 0; j < 4; j++)
    {
        f32 r,g,b;
        r = gSP.lights[gSP.numLights].r;
        g = gSP.lights[gSP.numLights].g;
        b = gSP.lights[gSP.numLights].b;

        for (int i = 0; i < gSP.numLights; i++)
        {
            f32 intensity = DotProduct( &OGL.triangles.vertices[v+j].nx, &gSP.lights[i].x );
            if (intensity < 0.0f) intensity = 0.0f;
/*
// paulscode, cause of the shader bug (not applying intensity to correct varriables)
            OGL.triangles.vertices[v+j].r += gSP.lights[i].r * intensity;
            OGL.triangles.vertices[v+j].g += gSP.lights[i].g * intensity;
            OGL.triangles.vertices[v+j].b += gSP.lights[i].b * intensity;
*/
//// paulscode, shader bug-fix:
            r += gSP.lights[i].r * intensity;
            g += gSP.lights[i].g * intensity;
            b += gSP.lights[i].b * intensity;
////
        }
        OGL.triangles.vertices[v+j].r = min(1.0f, r);
        OGL.triangles.vertices[v+j].g = min(1.0f, g);
        OGL.triangles.vertices[v+j].b = min(1.0f, b);
    }
}

static void gSPBillboardVertex4_default(u32 v)
{

    int i = 0;
#ifdef __TRIBUFFER_OPT
    i = OGL.triangles.indexmap[0];
#endif

    OGL.triangles.vertices[v].x += OGL.triangles.vertices[i].x;
    OGL.triangles.vertices[v].y += OGL.triangles.vertices[i].y;
    OGL.triangles.vertices[v].z += OGL.triangles.vertices[i].z;
    OGL.triangles.vertices[v].w += OGL.triangles.vertices[i].w;
    OGL.triangles.vertices[v+1].x += OGL.triangles.vertices[i].x;
    OGL.triangles.vertices[v+1].y += OGL.triangles.vertices[i].y;
    OGL.triangles.vertices[v+1].z += OGL.triangles.vertices[i].z;
    OGL.triangles.vertices[v+1].w += OGL.triangles.vertices[i].w;
    OGL.triangles.vertices[v+2].x += OGL.triangles.vertices[i].x;
    OGL.triangles.vertices[v+2].y += OGL.triangles.vertices[i].y;
    OGL.triangles.vertices[v+2].z += OGL.triangles.vertices[i].z;
    OGL.triangles.vertices[v+2].w += OGL.triangles.vertices[i].w;
    OGL.triangles.vertices[v+3].x += OGL.triangles.vertices[i].x;
    OGL.triangles.vertices[v+3].y += OGL.triangles.vertices[i].y;
    OGL.triangles.vertices[v+3].z += OGL.triangles.vertices[i].z;
    OGL.triangles.vertices[v+3].w += OGL.triangles.vertices[i].w;
}

void gSPProcessVertex4(u32 v)
{
    if (gSP.changed & CHANGED_MATRIX)
        gSPCombineMatrices();

    gSPTransformVertex4(v, gSP.matrix.combined );

    if (config.screen.flipVertical)
    {
        OGL.triangles.vertices[v+0].y = -OGL.triangles.vertices[v+0].y;
        OGL.triangles.vertices[v+1].y = -OGL.triangles.vertices[v+1].y;
        OGL.triangles.vertices[v+2].y = -OGL.triangles.vertices[v+2].y;
        OGL.triangles.vertices[v+3].y = -OGL.triangles.vertices[v+3].y;
    }

    if (gDP.otherMode.depthSource)
    {
        OGL.triangles.vertices[v+0].z = gDP.primDepth.z * OGL.triangles.vertices[v+0].w;
        OGL.triangles.vertices[v+1].z = gDP.primDepth.z * OGL.triangles.vertices[v+1].w;
        OGL.triangles.vertices[v+2].z = gDP.primDepth.z * OGL.triangles.vertices[v+2].w;
        OGL.triangles.vertices[v+3].z = gDP.primDepth.z * OGL.triangles.vertices[v+3].w;
    }

    if (gSP.matrix.billboard)
        gSPBillboardVertex4(v);

    if (!(gSP.geometryMode & G_ZBUFFER))
    {
        OGL.triangles.vertices[v].z = -OGL.triangles.vertices[v].w;
        OGL.triangles.vertices[v+1].z = -OGL.triangles.vertices[v+1].w;
        OGL.triangles.vertices[v+2].z = -OGL.triangles.vertices[v+2].w;
        OGL.triangles.vertices[v+3].z = -OGL.triangles.vertices[v+3].w;
    }

    if (gSP.geometryMode & G_LIGHTING)
    {
        if (config.enableLighting)
        {
            gSPLightVertex4(v);
        }
        else
        {
            OGL.triangles.vertices[v].r = 1.0f;
            OGL.triangles.vertices[v].g = 1.0f;
            OGL.triangles.vertices[v].b = 1.0f;
            OGL.triangles.vertices[v+1].r = 1.0f;
            OGL.triangles.vertices[v+1].g = 1.0f;
            OGL.triangles.vertices[v+1].b = 1.0f;
            OGL.triangles.vertices[v+2].r = 1.0f;
            OGL.triangles.vertices[v+2].g = 1.0f;
            OGL.triangles.vertices[v+2].b = 1.0f;
            OGL.triangles.vertices[v+3].r = 1.0f;
            OGL.triangles.vertices[v+3].g = 1.0f;
            OGL.triangles.vertices[v+3].b = 1.0f;
        }

        if (gSP.geometryMode & G_TEXTURE_GEN)
        {
            gSPTransformNormal4(v, gSP.matrix.projection);

            if (gSP.geometryMode & G_TEXTURE_GEN_LINEAR)
            {
                OGL.triangles.vertices[v].s = acosf(OGL.triangles.vertices[v].nx) * 325.94931f;
                OGL.triangles.vertices[v].t = acosf(OGL.triangles.vertices[v].ny) * 325.94931f;
                OGL.triangles.vertices[v+1].s = acosf(OGL.triangles.vertices[v+1].nx) * 325.94931f;
                OGL.triangles.vertices[v+1].t = acosf(OGL.triangles.vertices[v+1].ny) * 325.94931f;
                OGL.triangles.vertices[v+2].s = acosf(OGL.triangles.vertices[v+2].nx) * 325.94931f;
                OGL.triangles.vertices[v+2].t = acosf(OGL.triangles.vertices[v+2].ny) * 325.94931f;
                OGL.triangles.vertices[v+3].s = acosf(OGL.triangles.vertices[v+3].nx) * 325.94931f;
                OGL.triangles.vertices[v+3].t = acosf(OGL.triangles.vertices[v+3].ny) * 325.94931f;
            }
            else // G_TEXTURE_GEN
            {
                OGL.triangles.vertices[v].s = (OGL.triangles.vertices[v].nx + 1.0f) * 512.0f;
                OGL.triangles.vertices[v].t = (OGL.triangles.vertices[v].ny + 1.0f) * 512.0f;
                OGL.triangles.vertices[v+1].s = (OGL.triangles.vertices[v+1].nx + 1.0f) * 512.0f;
                OGL.triangles.vertices[v+1].t = (OGL.triangles.vertices[v+1].ny + 1.0f) * 512.0f;
                OGL.triangles.vertices[v+2].s = (OGL.triangles.vertices[v+2].nx + 1.0f) * 512.0f;
                OGL.triangles.vertices[v+2].t = (OGL.triangles.vertices[v+2].ny + 1.0f) * 512.0f;
                OGL.triangles.vertices[v+3].s = (OGL.triangles.vertices[v+3].nx + 1.0f) * 512.0f;
                OGL.triangles.vertices[v+3].t = (OGL.triangles.vertices[v+3].ny + 1.0f) * 512.0f;
            }
        }
    }

    if (config.enableClipping) gSPClipVertex4(v);
}
#endif

void gSPClipVertex(u32 v)
{
    SPVertex *vtx = &OGL.triangles.vertices[v];
    vtx->clip = 0;
    if (vtx->x > +vtx->w)   vtx->clip |= CLIP_POSX;
    if (vtx->x < -vtx->w)   vtx->clip |= CLIP_NEGX;
    if (vtx->y > +vtx->w)   vtx->clip |= CLIP_POSY;
    if (vtx->y < -vtx->w)   vtx->clip |= CLIP_NEGY;
    //if (vtx->w < 0.1f)      vtx->clip |= CLIP_NEGW;
}

static void gSPTransformVertex_default(float vtx[4], float mtx[4][4])
{
    float x, y, z, w;
    x = vtx[0];
    y = vtx[1];
    z = vtx[2];
    w = vtx[3];

    vtx[0] = x * mtx[0][0] + y * mtx[1][0] + z * mtx[2][0] + mtx[3][0];
    vtx[1] = x * mtx[0][1] + y * mtx[1][1] + z * mtx[2][1] + mtx[3][1];
    vtx[2] = x * mtx[0][2] + y * mtx[1][2] + z * mtx[2][2] + mtx[3][2];
    vtx[3] = x * mtx[0][3] + y * mtx[1][3] + z * mtx[2][3] + mtx[3][3];
}

static void gSPLightVertex_default(u32 v)
{
    TransformVectorNormalize( &OGL.triangles.vertices[v].nx, gSP.matrix.modelView[gSP.matrix.modelViewi] );

    f32 r, g, b;
    r = gSP.lights[gSP.numLights].r;
    g = gSP.lights[gSP.numLights].g;
    b = gSP.lights[gSP.numLights].b;
    for (int i = 0; i < gSP.numLights; i++)
    {
        f32 intensity = DotProduct( &OGL.triangles.vertices[v].nx, &gSP.lights[i].x );
        if (intensity < 0.0f) intensity = 0.0f;
        r += gSP.lights[i].r * intensity;
        g += gSP.lights[i].g * intensity;
        b += gSP.lights[i].b * intensity;
    }
    OGL.triangles.vertices[v].r = min(1.0, r);
    OGL.triangles.vertices[v].g = min(1.0, g);
    OGL.triangles.vertices[v].b = min(1.0, b);
}

static void gSPBillboardVertex_default(u32 v, u32 i)
{
    OGL.triangles.vertices[v].x += OGL.triangles.vertices[i].x;
    OGL.triangles.vertices[v].y += OGL.triangles.vertices[i].y;
    OGL.triangles.vertices[v].z += OGL.triangles.vertices[i].z;
    OGL.triangles.vertices[v].w += OGL.triangles.vertices[i].w;
}

void gSPCombineMatrices()
{
    MultMatrix(gSP.matrix.projection, gSP.matrix.modelView[gSP.matrix.modelViewi], gSP.matrix.combined);
    gSP.changed &= ~CHANGED_MATRIX;
}

void gSPProcessVertex( u32 v )
{
    f32 intensity;
    f32 r, g, b;

    if (gSP.changed & CHANGED_MATRIX)
        gSPCombineMatrices();

    gSPTransformVertex( &OGL.triangles.vertices[v].x, gSP.matrix.combined );

    if (config.screen.flipVertical)
    {
        OGL.triangles.vertices[v].y = -OGL.triangles.vertices[v].y;
    }

    if (gDP.otherMode.depthSource)
    {
        OGL.triangles.vertices[v].z = gDP.primDepth.z * OGL.triangles.vertices[v].w;
    }

    if (gSP.matrix.billboard)
    {
        int i = 0;
#ifdef __TRIBUFFER_OPT
        i = OGL.triangles.indexmap[0];
#endif

        gSPBillboardVertex(v, i);
    }

    if (!(gSP.geometryMode & G_ZBUFFER))
    {
        OGL.triangles.vertices[v].z = -OGL.triangles.vertices[v].w;
    }

    if (config.enableClipping)
        gSPClipVertex(v);

    if (gSP.geometryMode & G_LIGHTING)
    {
        if (config.enableLighting)
        {
            gSPLightVertex(v);
        }
        else
        {
            OGL.triangles.vertices[v].r = 1.0f;
            OGL.triangles.vertices[v].g = 1.0f;
            OGL.triangles.vertices[v].b = 1.0f;
        }

        if (gSP.geometryMode & G_TEXTURE_GEN)
        {
            TransformVectorNormalize(&OGL.triangles.vertices[v].nx, gSP.matrix.projection);

            if (gSP.geometryMode & G_TEXTURE_GEN_LINEAR)
            {
                OGL.triangles.vertices[v].s = acosf(OGL.triangles.vertices[v].nx) * 325.94931f;
                OGL.triangles.vertices[v].t = acosf(OGL.triangles.vertices[v].ny) * 325.94931f;
            }
            else // G_TEXTURE_GEN
            {
                OGL.triangles.vertices[v].s = (OGL.triangles.vertices[v].nx + 1.0f) * 512.0f;
                OGL.triangles.vertices[v].t = (OGL.triangles.vertices[v].ny + 1.0f) * 512.0f;
            }
        }
    }
}


void gSPLoadUcodeEx( u32 uc_start, u32 uc_dstart, u16 uc_dsize )
{
    RSP.PCi = 0;
    gSP.matrix.modelViewi = 0;
    gSP.changed |= CHANGED_MATRIX;
    gSP.status[0] = gSP.status[1] = gSP.status[2] = gSP.status[3] = 0;

    if ((((uc_start & 0x1FFFFFFF) + 4096) > RDRAMSize) || (((uc_dstart & 0x1FFFFFFF) + uc_dsize) > RDRAMSize))
    {
        return;
    }

    MicrocodeInfo *ucode = GBI_DetectMicrocode( uc_start, uc_dstart, uc_dsize );

    if (ucode->type != 0xFFFFFFFF)
        last_good_ucode = ucode->type;

    if (ucode->type != NONE)
    {
        GBI_MakeCurrent( ucode );
    }
    else
    {
        LOG(LOG_WARNING, "Unknown Ucode\n");
    }
}

void gSPNoOp()
{
    gSPFlushTriangles();
}

void gSPTriangleUnknown()
{
#ifdef __TRIBUFFER_OPT
    gSPFlushTriangles();
#endif
}

void gSPMatrix( u32 matrix, u8 param )
{
#ifdef __TRIBUFFER_OPT
    gSPFlushTriangles();
#endif

    f32 mtx[4][4];
    u32 address = RSP_SegmentToPhysical( matrix );

    if (address + 64 > RDRAMSize)
    {
        return;
    }

    RSP_LoadMatrix( mtx, address );

    if (param & G_MTX_PROJECTION)
    {
        if (param & G_MTX_LOAD)
            CopyMatrix( gSP.matrix.projection, mtx );
        else
            MultMatrix2( gSP.matrix.projection, mtx );
    }
    else
    {
        if ((param & G_MTX_PUSH) && (gSP.matrix.modelViewi < (gSP.matrix.stackSize - 1)))
        {
            CopyMatrix( gSP.matrix.modelView[gSP.matrix.modelViewi + 1], gSP.matrix.modelView[gSP.matrix.modelViewi] );
            gSP.matrix.modelViewi++;
        }
        if (param & G_MTX_LOAD)
            CopyMatrix( gSP.matrix.modelView[gSP.matrix.modelViewi], mtx );
        else
            MultMatrix2( gSP.matrix.modelView[gSP.matrix.modelViewi], mtx );
    }

    gSP.changed |= CHANGED_MATRIX;
}

void gSPDMAMatrix( u32 matrix, u8 index, u8 multiply )
{
    f32 mtx[4][4];
    u32 address = gSP.DMAOffsets.mtx + RSP_SegmentToPhysical( matrix );

    if (address + 64 > RDRAMSize)
    {
        return;
    }

    RSP_LoadMatrix( mtx, address );

    gSP.matrix.modelViewi = index;

    if (multiply)
    {
        //CopyMatrix( gSP.matrix.modelView[gSP.matrix.modelViewi], gSP.matrix.modelView[0] );
        //MultMatrix( gSP.matrix.modelView[gSP.matrix.modelViewi], mtx );
        MultMatrix(gSP.matrix.modelView[0], mtx, gSP.matrix.modelView[gSP.matrix.modelViewi]);
    }
    else
        CopyMatrix( gSP.matrix.modelView[gSP.matrix.modelViewi], mtx );

    CopyMatrix( gSP.matrix.projection, identityMatrix );
    gSP.changed |= CHANGED_MATRIX;
}

void gSPViewport( u32 v )
{
    u32 address = RSP_SegmentToPhysical( v );

    if ((address + 16) > RDRAMSize)
    {
#ifdef DEBUG
        DebugMsg( DEBUG_HIGH | DEBUG_ERROR, "// Attempting to load viewport from invalid address\n" );
        DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gSPViewport( 0x%08X );\n", v );
#endif
        return;
    }

    gSP.viewport.vscale[0] = _FIXED2FLOAT( *(s16*)&RDRAM[address +  2], 2 );
    gSP.viewport.vscale[1] = _FIXED2FLOAT( *(s16*)&RDRAM[address     ], 2 );
    gSP.viewport.vscale[2] = _FIXED2FLOAT( *(s16*)&RDRAM[address +  6], 10 );// * 0.00097847357f;
    gSP.viewport.vscale[3] = *(s16*)&RDRAM[address +  4];
    gSP.viewport.vtrans[0] = _FIXED2FLOAT( *(s16*)&RDRAM[address + 10], 2 );
    gSP.viewport.vtrans[1] = _FIXED2FLOAT( *(s16*)&RDRAM[address +  8], 2 );
    gSP.viewport.vtrans[2] = _FIXED2FLOAT( *(s16*)&RDRAM[address + 14], 10 );// * 0.00097847357f;
    gSP.viewport.vtrans[3] = *(s16*)&RDRAM[address + 12];

    gSP.viewport.x      = gSP.viewport.vtrans[0] - gSP.viewport.vscale[0];
    gSP.viewport.y      = gSP.viewport.vtrans[1] - gSP.viewport.vscale[1];
    gSP.viewport.width  = gSP.viewport.vscale[0] * 2;
    gSP.viewport.height = gSP.viewport.vscale[1] * 2;
    gSP.viewport.nearz  = gSP.viewport.vtrans[2] - gSP.viewport.vscale[2];
    gSP.viewport.farz   = (gSP.viewport.vtrans[2] + gSP.viewport.vscale[2]) ;

    gSP.changed |= CHANGED_VIEWPORT;
}

void gSPForceMatrix( u32 mptr )
{
    u32 address = RSP_SegmentToPhysical( mptr );

    if (address + 64 > RDRAMSize)
    {
        return;
    }

    RSP_LoadMatrix( gSP.matrix.combined, RSP_SegmentToPhysical( mptr ) );

    gSP.changed &= ~CHANGED_MATRIX;
}

void gSPLight( u32 l, s32 n )
{
    n--;
    if (n >= 8)
        return;

    u32 address = RSP_SegmentToPhysical( l );

    if ((address + sizeof( Light )) > RDRAMSize)
    {
        return;
    }

    u8 *addr = &RDRAM[address];

    if (config.hackZelda && (addr[0] == 0x08) && (addr[4] == 0xFF))
    {
        LightMM *light = (LightMM*)addr;
        gSP.lights[n].r = light->r * 0.0039215689f;
        gSP.lights[n].g = light->g * 0.0039215689f;
        gSP.lights[n].b = light->b * 0.0039215689f;
        gSP.lights[n].x = light->x;
        gSP.lights[n].y = light->y;
        gSP.lights[n].z = light->z;
    }
    else
    {
        Light *light = (Light*)addr;
        gSP.lights[n].r = light->r * 0.0039215689f;
        gSP.lights[n].g = light->g * 0.0039215689f;
        gSP.lights[n].b = light->b * 0.0039215689f;
        gSP.lights[n].x = light->x;
        gSP.lights[n].y = light->y;
        gSP.lights[n].z = light->z;
    }
    Normalize(&gSP.lights[n].x);
}

void gSPLookAt( u32 l )
{
}

void gSPVertex( u32 v, u32 n, u32 v0 )
{
    //flush batched triangles:
#ifdef __TRIBUFFER_OPT
    gSPFlushTriangles();
#endif

    u32 address = RSP_SegmentToPhysical( v );

    if ((address + sizeof( Vertex ) * n) > RDRAMSize)
    {
        return;
    }

    Vertex *vertex = (Vertex*)&RDRAM[address];

    if ((n + v0) <= INDEXMAP_SIZE)
    {
        unsigned int i = v0;
#ifdef __VEC4_OPT
        for (; i < n - (n%4) + v0; i += 4)
        {
            u32 v = i;
#ifdef __TRIBUFFER_OPT
            v = __indexmap_getnew(v, 4);
#endif
            for(int j = 0; j < 4; j++)
            {
                OGL.triangles.vertices[v+j].x = vertex->x;
                OGL.triangles.vertices[v+j].y = vertex->y;
                OGL.triangles.vertices[v+j].z = vertex->z;
                //OGL.triangles.vertices[i+j].flag = vertex->flag;
                OGL.triangles.vertices[v+j].s = _FIXED2FLOAT( vertex->s, 5 );
                OGL.triangles.vertices[v+j].t = _FIXED2FLOAT( vertex->t, 5 );
                if (gSP.geometryMode & G_LIGHTING)
                {
                    OGL.triangles.vertices[v+j].nx = vertex->normal.x;
                    OGL.triangles.vertices[v+j].ny = vertex->normal.y;
                    OGL.triangles.vertices[v+j].nz = vertex->normal.z;
                    OGL.triangles.vertices[v+j].a = vertex->color.a * 0.0039215689f;
                }
                else
                {
                    OGL.triangles.vertices[v+j].r = vertex->color.r * 0.0039215689f;
                    OGL.triangles.vertices[v+j].g = vertex->color.g * 0.0039215689f;
                    OGL.triangles.vertices[v+j].b = vertex->color.b * 0.0039215689f;
                    OGL.triangles.vertices[v+j].a = vertex->color.a * 0.0039215689f;
                }
                vertex++;
            }
            gSPProcessVertex4(v);
        }
#endif
        for (; i < n + v0; i++)
        {
            u32 v = i;
#ifdef __TRIBUFFER_OPT
            v = __indexmap_getnew(v, 1);
#endif
            OGL.triangles.vertices[v].x = vertex->x;
            OGL.triangles.vertices[v].y = vertex->y;
            OGL.triangles.vertices[v].z = vertex->z;
            OGL.triangles.vertices[v].s = _FIXED2FLOAT( vertex->s, 5 );
            OGL.triangles.vertices[v].t = _FIXED2FLOAT( vertex->t, 5 );
            if (gSP.geometryMode & G_LIGHTING)
            {
                OGL.triangles.vertices[v].nx = vertex->normal.x;
                OGL.triangles.vertices[v].ny = vertex->normal.y;
                OGL.triangles.vertices[v].nz = vertex->normal.z;
                OGL.triangles.vertices[v].a = vertex->color.a * 0.0039215689f;
            }
            else
            {
                OGL.triangles.vertices[v].r = vertex->color.r * 0.0039215689f;
                OGL.triangles.vertices[v].g = vertex->color.g * 0.0039215689f;
                OGL.triangles.vertices[v].b = vertex->color.b * 0.0039215689f;
                OGL.triangles.vertices[v].a = vertex->color.a * 0.0039215689f;
            }
            gSPProcessVertex(v);
            vertex++;
        }
    }
    else
    {
        LOG(LOG_ERROR, "Using Vertex outside buffer v0=%i, n=%i\n", v0, n);
    }

}

void gSPCIVertex( u32 v, u32 n, u32 v0 )
{

#ifdef __TRIBUFFER_OPT
    gSPFlushTriangles();
#endif

    u32 address = RSP_SegmentToPhysical( v );

    if ((address + sizeof( PDVertex ) * n) > RDRAMSize)
    {
        return;
    }

    PDVertex *vertex = (PDVertex*)&RDRAM[address];

    if ((n + v0) <= INDEXMAP_SIZE)
    {
        unsigned int i = v0;
#ifdef __VEC4_OPT
        for (; i < n - (n%4) + v0; i += 4)
        {
            u32 v = i;
#ifdef __TRIBUFFER_OPT
            v = __indexmap_getnew(v, 4);
#endif
            for(unsigned int j = 0; j < 4; j++)
            {
                OGL.triangles.vertices[v+j].x = vertex->x;
                OGL.triangles.vertices[v+j].y = vertex->y;
                OGL.triangles.vertices[v+j].z = vertex->z;
                OGL.triangles.vertices[v+j].s = _FIXED2FLOAT( vertex->s, 5 );
                OGL.triangles.vertices[v+j].t = _FIXED2FLOAT( vertex->t, 5 );
                u8 *color = &RDRAM[gSP.vertexColorBase + (vertex->ci & 0xff)];

                if (gSP.geometryMode & G_LIGHTING)
                {
                    OGL.triangles.vertices[v+j].nx = (s8)color[3];
                    OGL.triangles.vertices[v+j].ny = (s8)color[2];
                    OGL.triangles.vertices[v+j].nz = (s8)color[1];
                    OGL.triangles.vertices[v+j].a = color[0] * 0.0039215689f;
                }
                else
                {
                    OGL.triangles.vertices[v+j].r = color[3] * 0.0039215689f;
                    OGL.triangles.vertices[v+j].g = color[2] * 0.0039215689f;
                    OGL.triangles.vertices[v+j].b = color[1] * 0.0039215689f;
                    OGL.triangles.vertices[v+j].a = color[0] * 0.0039215689f;
                }
                vertex++;
            }
            gSPProcessVertex4(v);
        }
#endif
        for(; i < n + v0; i++)
        {
            u32 v = i;
#ifdef __TRIBUFFER_OPT
            v = __indexmap_getnew(v, 1);
#endif
            OGL.triangles.vertices[v].x = vertex->x;
            OGL.triangles.vertices[v].y = vertex->y;
            OGL.triangles.vertices[v].z = vertex->z;
            OGL.triangles.vertices[v].s = _FIXED2FLOAT( vertex->s, 5 );
            OGL.triangles.vertices[v].t = _FIXED2FLOAT( vertex->t, 5 );
            u8 *color = &RDRAM[gSP.vertexColorBase + (vertex->ci & 0xff)];

            if (gSP.geometryMode & G_LIGHTING)
            {
                OGL.triangles.vertices[v].nx = (s8)color[3];
                OGL.triangles.vertices[v].ny = (s8)color[2];
                OGL.triangles.vertices[v].nz = (s8)color[1];
                OGL.triangles.vertices[v].a = color[0] * 0.0039215689f;
            }
            else
            {
                OGL.triangles.vertices[v].r = color[3] * 0.0039215689f;
                OGL.triangles.vertices[v].g = color[2] * 0.0039215689f;
                OGL.triangles.vertices[v].b = color[1] * 0.0039215689f;
                OGL.triangles.vertices[v].a = color[0] * 0.0039215689f;
            }

            gSPProcessVertex(v);
            vertex++;
        }
    }
    else
    {
        LOG(LOG_ERROR, "Using Vertex outside buffer v0=%i, n=%i\n", v0, n);
    }

}

void gSPDMAVertex( u32 v, u32 n, u32 v0 )
{

    u32 address = gSP.DMAOffsets.vtx + RSP_SegmentToPhysical( v );

    if ((address + 10 * n) > RDRAMSize)
    {
        return;
    }

    if ((n + v0) <= INDEXMAP_SIZE)
    {
        u32 i = v0;
#ifdef __VEC4_OPT
        for (; i < n - (n%4) + v0; i += 4)
        {
            u32 v = i;
#ifdef __TRIBUFFER_OPT
            v = __indexmap_getnew(v, 4);
#endif
            for(int j = 0; j < 4; j++)
            {
                OGL.triangles.vertices[v+j].x = *(s16*)&RDRAM[address ^ 2];
                OGL.triangles.vertices[v+j].y = *(s16*)&RDRAM[(address + 2) ^ 2];
                OGL.triangles.vertices[v+j].z = *(s16*)&RDRAM[(address + 4) ^ 2];

                if (gSP.geometryMode & G_LIGHTING)
                {
                    OGL.triangles.vertices[v+j].nx = *(s8*)&RDRAM[(address + 6) ^ 3];
                    OGL.triangles.vertices[v+j].ny = *(s8*)&RDRAM[(address + 7) ^ 3];
                    OGL.triangles.vertices[v+j].nz = *(s8*)&RDRAM[(address + 8) ^ 3];
                    OGL.triangles.vertices[v+j].a = *(u8*)&RDRAM[(address + 9) ^ 3] * 0.0039215689f;
                }
                else
                {
                    OGL.triangles.vertices[v+j].r = *(u8*)&RDRAM[(address + 6) ^ 3] * 0.0039215689f;
                    OGL.triangles.vertices[v+j].g = *(u8*)&RDRAM[(address + 7) ^ 3] * 0.0039215689f;
                    OGL.triangles.vertices[v+j].b = *(u8*)&RDRAM[(address + 8) ^ 3] * 0.0039215689f;
                    OGL.triangles.vertices[v+j].a = *(u8*)&RDRAM[(address + 9) ^ 3] * 0.0039215689f;
                }
                address += 10;
            }
            gSPProcessVertex4(v);
        }
#endif
        for (; i < n + v0; i++)
        {
            u32 v = i;
#ifdef __TRIBUFFER_OPT
            //int ind = OGL.triangles.indexmap[i];
            v = __indexmap_getnew(v, 1);

            //if previously mapped copy across s/t.
            //if (ind != -1)
            //{
            //    SPVertex *vtx = &OGL.triangles.vertices[ind];
            //    OGL.triangles.vertices[v].s = vtx->s;
            //    OGL.triangles.vertices[v].t = vtx->s;
            //}
#else
            v = i;
#endif
            OGL.triangles.vertices[v].x = *(s16*)&RDRAM[address ^ 2];
            OGL.triangles.vertices[v].y = *(s16*)&RDRAM[(address + 2) ^ 2];
            OGL.triangles.vertices[v].z = *(s16*)&RDRAM[(address + 4) ^ 2];

            if (gSP.geometryMode & G_LIGHTING)
            {
                OGL.triangles.vertices[v].nx = *(s8*)&RDRAM[(address + 6) ^ 3];
                OGL.triangles.vertices[v].ny = *(s8*)&RDRAM[(address + 7) ^ 3];
                OGL.triangles.vertices[v].nz = *(s8*)&RDRAM[(address + 8) ^ 3];
                OGL.triangles.vertices[v].a = *(u8*)&RDRAM[(address + 9) ^ 3] * 0.0039215689f;
            }
            else
            {
                OGL.triangles.vertices[v].r = *(u8*)&RDRAM[(address + 6) ^ 3] * 0.0039215689f;
                OGL.triangles.vertices[v].g = *(u8*)&RDRAM[(address + 7) ^ 3] * 0.0039215689f;
                OGL.triangles.vertices[v].b = *(u8*)&RDRAM[(address + 8) ^ 3] * 0.0039215689f;
                OGL.triangles.vertices[v].a = *(u8*)&RDRAM[(address + 9) ^ 3] * 0.0039215689f;
            }

            gSPProcessVertex(v);
            address += 10;
        }
    }
    else
    {
        LOG(LOG_ERROR, "Using Vertex outside buffer v0=%i, n=%i\n", v0, n);
    }

}

void gSPDisplayList( u32 dl )
{
    u32 address = RSP_SegmentToPhysical( dl );

    if ((address + 8) > RDRAMSize)
    {
        return;
    }

    if (RSP.PCi < (GBI.PCStackSize - 1))
    {
#ifdef DEBUG
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "\n" );
    DebugMsg( DEBUG_HIGH | DEBUG_HANDLED, "gSPDisplayList( 0x%08X );\n",
        dl );
#endif
        RSP.PCi++;
        RSP.PC[RSP.PCi] = address;
        RSP.nextCmd = _SHIFTR( *(u32*)&RDRAM[address], 24, 8 );
    }


}

void gSPDMADisplayList( u32 dl, u32 n )
{
    if ((dl + (n << 3)) > RDRAMSize)
    {
        return;
    }

    u32 curDL = RSP.PC[RSP.PCi];

    RSP.PC[RSP.PCi] = RSP_SegmentToPhysical( dl );

    while ((RSP.PC[RSP.PCi] - dl) < (n << 3))
    {
        if ((RSP.PC[RSP.PCi] + 8) > RDRAMSize)
        {
            break;
        }

        u32 w0 = *(u32*)&RDRAM[RSP.PC[RSP.PCi]];
        u32 w1 = *(u32*)&RDRAM[RSP.PC[RSP.PCi] + 4];

        RSP.PC[RSP.PCi] += 8;
        RSP.nextCmd = _SHIFTR( *(u32*)&RDRAM[RSP.PC[RSP.PCi]], 24, 8 );

        GBI.cmd[_SHIFTR( w0, 24, 8 )]( w0, w1 );
    }

    RSP.PC[RSP.PCi] = curDL;
}

void gSPBranchList( u32 dl )
{
    u32 address = RSP_SegmentToPhysical( dl );

    if ((address + 8) > RDRAMSize)
    {
        return;
    }

    RSP.PC[RSP.PCi] = address;
    RSP.nextCmd = _SHIFTR( *(u32*)&RDRAM[address], 24, 8 );
}

void gSPBranchLessZ( u32 branchdl, u32 vtx, f32 zval )
{
    u32 address = RSP_SegmentToPhysical( branchdl );

    if ((address + 8) > RDRAMSize)
    {
        return;
    }

    if (OGL.triangles.vertices[vtx].z <= zval)
        RSP.PC[RSP.PCi] = address;
}

void gSPSetDMAOffsets( u32 mtxoffset, u32 vtxoffset )
{
    gSP.DMAOffsets.mtx = mtxoffset;
    gSP.DMAOffsets.vtx = vtxoffset;
}

void gSPSetVertexColorBase( u32 base )
{
    gSP.vertexColorBase = RSP_SegmentToPhysical( base );

#ifdef __TRIBUFFER_OPT
    gSPFlushTriangles();
#endif
}

void gSPSprite2DBase( u32 base )
{
}

void gSPCopyVertex( SPVertex *dest, SPVertex *src )
{
    dest->x = src->x;
    dest->y = src->y;
    dest->z = src->z;
    dest->w = src->w;
    dest->r = src->r;
    dest->g = src->g;
    dest->b = src->b;
    dest->a = src->a;
    dest->s = src->s;
    dest->t = src->t;
}

void gSPInterpolateVertex( SPVertex *dest, f32 percent, SPVertex *first, SPVertex *second )
{
    dest->x = first->x + percent * (second->x - first->x);
    dest->y = first->y + percent * (second->y - first->y);
    dest->z = first->z + percent * (second->z - first->z);
    dest->w = first->w + percent * (second->w - first->w);
    dest->r = first->r + percent * (second->r - first->r);
    dest->g = first->g + percent * (second->g - first->g);
    dest->b = first->b + percent * (second->b - first->b);
    dest->a = first->a + percent * (second->a - first->a);
    dest->s = first->s + percent * (second->s - first->s);
    dest->t = first->t + percent * (second->t - first->t);
}

void gSPDMATriangles( u32 tris, u32 n )
{
    u32 address = RSP_SegmentToPhysical( tris );

    if (address + sizeof( DKRTriangle ) * n > RDRAMSize)
    {
        return;
    }

#ifdef __TRIBUFFER_OPT
    __indexmap_undomap();
#endif

    DKRTriangle *triangles = (DKRTriangle*)&RDRAM[address];

    for (u32 i = 0; i < n; i++)
    {
        int mode = 0;
        if (!(triangles->flag & 0x40))
        {
            if (gSP.viewport.vscale[0] > 0)
                mode |= G_CULL_BACK;
            else
                mode |= G_CULL_FRONT;
        }

        if ((gSP.geometryMode&G_CULL_BOTH) != mode)
        {
            OGL_DrawTriangles();
            gSP.geometryMode &= ~G_CULL_BOTH;
            gSP.geometryMode |= mode;
            gSP.changed |= CHANGED_GEOMETRYMODE;
        }


        s32 v0 = triangles->v0;
        s32 v1 = triangles->v1;
        s32 v2 = triangles->v2;
        OGL.triangles.vertices[v0].s = _FIXED2FLOAT( triangles->s0, 5 );
        OGL.triangles.vertices[v0].t = _FIXED2FLOAT( triangles->t0, 5 );
        OGL.triangles.vertices[v1].s = _FIXED2FLOAT( triangles->s1, 5 );
        OGL.triangles.vertices[v1].t = _FIXED2FLOAT( triangles->t1, 5 );
        OGL.triangles.vertices[v2].s = _FIXED2FLOAT( triangles->s2, 5 );
        OGL.triangles.vertices[v2].t = _FIXED2FLOAT( triangles->t2, 5 );
        gSPTriangle(triangles->v0, triangles->v1, triangles->v2);
        triangles++;
    }

#ifdef __TRIBUFFER_OPT
    OGL_DrawTriangles();
#endif
}

void gSP1Quadrangle( s32 v0, s32 v1, s32 v2, s32 v3)
{
    gSPTriangle( v0, v1, v2);
    gSPTriangle( v0, v2, v3);
    gSPFlushTriangles();
}

bool gSPCullVertices( u32 v0, u32 vn )
{
    if (!config.enableClipping)
        return FALSE;

    s32 v = v0;
#ifdef __TRIBUFFER_OPT
    v = OGL.triangles.indexmap[v0];
#endif

    u32 clip = OGL.triangles.vertices[v].clip;
    if (clip == 0)
        return FALSE;

    for (unsigned int i = (v0+1); i <= vn; i++)
    {
        v = i;
#ifdef __TRIBUFFER_OPT
        v = OGL.triangles.indexmap[i];
#endif
        if (OGL.triangles.vertices[v].clip != clip) return FALSE;
    }
    return TRUE;
}

void gSPCullDisplayList( u32 v0, u32 vn )
{
    if (gSPCullVertices( v0, vn ))
    {
        if (RSP.PCi > 0)
            RSP.PCi--;
        else
        {
            RSP.halt = TRUE;
        }
    }
}

void gSPPopMatrixN( u32 param, u32 num )
{
    if (gSP.matrix.modelViewi > num - 1)
    {
        gSP.matrix.modelViewi -= num;

        gSP.changed |= CHANGED_MATRIX;
    }
}

void gSPPopMatrix( u32 param )
{
    if (gSP.matrix.modelViewi > 0)
    {
        gSP.matrix.modelViewi--;

        gSP.changed |= CHANGED_MATRIX;
    }
}

void gSPSegment( s32 seg, s32 base )
{
    if (seg > 0xF)
    {
        return;
    }

    if ((unsigned int)base > RDRAMSize - 1)
    {
        return;
    }

    gSP.segment[seg] = base;
}

void gSPClipRatio( u32 r )
{
}

void gSPInsertMatrix( u32 where, u32 num )
{
    f32 fraction, integer;

    if (gSP.changed & CHANGED_MATRIX)
        gSPCombineMatrices();

    if ((where & 0x3) || (where > 0x3C))
    {
        return;
    }

    if (where < 0x20)
    {
        fraction = modff( gSP.matrix.combined[0][where >> 1], &integer );
        gSP.matrix.combined[0][where >> 1] = (s16)_SHIFTR( num, 16, 16 ) + abs( (int)fraction );

        fraction = modff( gSP.matrix.combined[0][(where >> 1) + 1], &integer );
        gSP.matrix.combined[0][(where >> 1) + 1] = (s16)_SHIFTR( num, 0, 16 ) + abs( (int)fraction );
    }
    else
    {
        f32 newValue;

        fraction = modff( gSP.matrix.combined[0][(where - 0x20) >> 1], &integer );
        newValue = integer + _FIXED2FLOAT( _SHIFTR( num, 16, 16 ), 16);

        // Make sure the sign isn't lost
        if ((integer == 0.0f) && (fraction != 0.0f))
            newValue = newValue * (fraction / abs( (int)fraction ));

        gSP.matrix.combined[0][(where - 0x20) >> 1] = newValue;

        fraction = modff( gSP.matrix.combined[0][((where - 0x20) >> 1) + 1], &integer );
        newValue = integer + _FIXED2FLOAT( _SHIFTR( num, 0, 16 ), 16 );

        // Make sure the sign isn't lost
        if ((integer == 0.0f) && (fraction != 0.0f))
            newValue = newValue * (fraction / abs( (int)fraction ));

        gSP.matrix.combined[0][((where - 0x20) >> 1) + 1] = newValue;
    }
}

void gSPModifyVertex( u32 vtx, u32 where, u32 val )
{
    s32 v = vtx;

#ifdef __TRIBUFFER_OPT
    v = OGL.triangles.indexmap[v];
#endif

    switch (where)
    {
        case G_MWO_POINT_RGBA:
            OGL.triangles.vertices[v].r = _SHIFTR( val, 24, 8 ) * 0.0039215689f;
            OGL.triangles.vertices[v].g = _SHIFTR( val, 16, 8 ) * 0.0039215689f;
            OGL.triangles.vertices[v].b = _SHIFTR( val, 8, 8 ) * 0.0039215689f;
            OGL.triangles.vertices[v].a = _SHIFTR( val, 0, 8 ) * 0.0039215689f;
            break;
        case G_MWO_POINT_ST:
            OGL.triangles.vertices[v].s = _FIXED2FLOAT( (s16)_SHIFTR( val, 16, 16 ), 5 );
            OGL.triangles.vertices[v].t = _FIXED2FLOAT( (s16)_SHIFTR( val, 0, 16 ), 5 );
            break;
        case G_MWO_POINT_XYSCREEN:
            break;
        case G_MWO_POINT_ZSCREEN:
            break;
    }
}

void gSPNumLights( s32 n )
{
    gSP.numLights = (n <= 8) ? n : 0;
}


void gSPLightColor( u32 lightNum, u32 packedColor )
{
    lightNum--;

    if (lightNum < 8)
    {
        gSP.lights[lightNum].r = _SHIFTR( packedColor, 24, 8 ) * 0.0039215689f;
        gSP.lights[lightNum].g = _SHIFTR( packedColor, 16, 8 ) * 0.0039215689f;
        gSP.lights[lightNum].b = _SHIFTR( packedColor, 8, 8 ) * 0.0039215689f;
    }
}

void gSPFogFactor( s16 fm, s16 fo )
{
    gSP.fog.multiplier = fm;
    gSP.fog.offset = fo;

    gSP.changed |= CHANGED_FOGPOSITION;
}

void gSPPerspNormalize( u16 scale )
{
}

void gSPTexture( f32 sc, f32 tc, s32 level, s32 tile, s32 on )
{
    gSP.texture.scales = sc;
    gSP.texture.scalet = tc;

    if (gSP.texture.scales == 0.0f) gSP.texture.scales = 1.0f;
    if (gSP.texture.scalet == 0.0f) gSP.texture.scalet = 1.0f;

    gSP.texture.level = level;
    gSP.texture.on = on;

    if (gSP.texture.tile != tile)
    {
        gSP.texture.tile = tile;
        gSP.textureTile[0] = &gDP.tiles[tile];
        gSP.textureTile[1] = &gDP.tiles[(tile < 7) ? (tile + 1) : tile];
        gSP.changed |= CHANGED_TEXTURE;
    }

    gSP.changed |= CHANGED_TEXTURESCALE;
}

void gSPEndDisplayList()
{
    if (RSP.PCi > 0)
        RSP.PCi--;
    else
    {
        RSP.halt = TRUE;
    }

#ifdef __TRIBUFFER_OPT
    RSP.nextCmd = _SHIFTR( *(u32*)&RDRAM[RSP.PC[RSP.PCi]], 24, 8 );
    gSPFlushTriangles();
#endif
}

void gSPGeometryMode( u32 clear, u32 set )
{
    gSP.geometryMode = (gSP.geometryMode & ~clear) | set;
    gSP.changed |= CHANGED_GEOMETRYMODE;
}

void gSPSetGeometryMode( u32 mode )
{
    gSP.geometryMode |= mode;
    gSP.changed |= CHANGED_GEOMETRYMODE;
}

void gSPClearGeometryMode( u32 mode )
{
    gSP.geometryMode &= ~mode;
    gSP.changed |= CHANGED_GEOMETRYMODE;
}

void gSPLine3D( s32 v0, s32 v1, s32 flag )
{
    OGL_DrawLine(v0, v1, 1.5f );
}

void gSPLineW3D( s32 v0, s32 v1, s32 wd, s32 flag )
{
    OGL_DrawLine(v0, v1, 1.5f + wd * 0.5f );
}

void gSPBgRect1Cyc( u32 bg )
{

#if 1

    u32 addr = RSP_SegmentToPhysical(bg) >> 1;

    f32 imageX	= (((u16*)RDRAM)[(addr+0)^1] >> 5);	// 0
    f32 imageY	= (((u16*)RDRAM)[(addr+4)^1] >> 5);	// 4
    f32 imageW	= (((u16*)RDRAM)[(addr+1)^1] >> 2);	// 1
    f32 imageH	= (((u16*)RDRAM)[(addr+5)^1] >> 2);	// 5

    f32 frameX	= ((s16*)RDRAM)[(addr+2)^1] / 4.0f;	// 2
    f32 frameY	= ((s16*)RDRAM)[(addr+6)^1] / 4.0f;	// 6
    f32 frameW	= ((u16*)RDRAM)[(addr+3)^1] >> 2;		// 3
    f32 frameH	= ((u16*)RDRAM)[(addr+7)^1] >> 2;		// 7


    //wxUint16 imageFlip = ((u16*)gfx.RDRAM)[(addr+13)^1];	// 13;
    //d.flipX 	= (u8)imageFlip&0x01;

    gSP.bgImage.address	= RSP_SegmentToPhysical(((u32*)RDRAM)[(addr+8)>>1]);	// 8,9
    gSP.bgImage.width = imageW;
    gSP.bgImage.height = imageH;
    gSP.bgImage.format = ((u8*)RDRAM)[(((addr+11)<<1)+0)^3];
    gSP.bgImage.size = ((u8*)RDRAM)[(((addr+11)<<1)+1)^3];
    gSP.bgImage.palette = ((u16*)RDRAM)[(addr+12)^1];

    f32 scaleW	= ((s16*)RDRAM)[(addr+14)^1] / 1024.0f;	// 14
    f32 scaleH	= ((s16*)RDRAM)[(addr+15)^1] / 1024.0f;	// 15
    gDP.textureMode = TEXTUREMODE_BGIMAGE;

#else
    u32 address = RSP_SegmentToPhysical( bg );
    uObjScaleBg *objScaleBg = (uObjScaleBg*)&RDRAM[address];

    gSP.bgImage.address = RSP_SegmentToPhysical( objScaleBg->imagePtr );
    gSP.bgImage.width = objScaleBg->imageW >> 2;
    gSP.bgImage.height = objScaleBg->imageH >> 2;
    gSP.bgImage.format = objScaleBg->imageFmt;
    gSP.bgImage.size = objScaleBg->imageSiz;
    gSP.bgImage.palette = objScaleBg->imagePal;
    gDP.textureMode = TEXTUREMODE_BGIMAGE;

    f32 imageX = _FIXED2FLOAT( objScaleBg->imageX, 5 );
    f32 imageY = _FIXED2FLOAT( objScaleBg->imageY, 5 );
    f32 imageW = objScaleBg->imageW >> 2;
    f32 imageH = objScaleBg->imageH >> 2;

    f32 frameX = _FIXED2FLOAT( objScaleBg->frameX, 2 );
    f32 frameY = _FIXED2FLOAT( objScaleBg->frameY, 2 );
    f32 frameW = _FIXED2FLOAT( objScaleBg->frameW, 2 );
    f32 frameH = _FIXED2FLOAT( objScaleBg->frameH, 2 );
    f32 scaleW = _FIXED2FLOAT( objScaleBg->scaleW, 10 );
    f32 scaleH = _FIXED2FLOAT( objScaleBg->scaleH, 10 );
#endif

    f32 frameX0 = frameX;
    f32 frameY0 = frameY;
    f32 frameS0 = imageX;
    f32 frameT0 = imageY;

    f32 frameX1 = frameX + min( (imageW - imageX) / scaleW, frameW );
    f32 frameY1 = frameY + min( (imageH - imageY) / scaleH, frameH );
    //f32 frameS1 = imageX + min( (imageW - imageX) * scaleW, frameW * scaleW );
    //f32 frameT1 = imageY + min( (imageH - imageY) * scaleH, frameH * scaleH );

    gDP.otherMode.cycleType = G_CYC_1CYCLE;
    gDP.changed |= CHANGED_CYCLETYPE;
    gSPTexture( 1.0f, 1.0f, 0, 0, TRUE );
    gDPTextureRectangle( frameX0, frameY0, frameX1 - 1, frameY1 - 1, 0, frameS0 - 1, frameT0 - 1, scaleW, scaleH );

    if ((frameX1 - frameX0) < frameW)
    {
        f32 frameX2 = frameW - (frameX1 - frameX0) + frameX1;
        gDPTextureRectangle( frameX1, frameY0, frameX2 - 1, frameY1 - 1, 0, 0, frameT0, scaleW, scaleH );
    }

    if ((frameY1 - frameY0) < frameH)
    {
        f32 frameY2 = frameH - (frameY1 - frameY0) + frameY1;
        gDPTextureRectangle( frameX0, frameY1, frameX1 - 1, frameY2 - 1, 0, frameS0, 0, scaleW, scaleH );
    }

    gDPTextureRectangle( 0, 0, 319, 239, 0, 0, 0, scaleW, scaleH );
}

void gSPBgRectCopy( u32 bg )
{
    u32 address = RSP_SegmentToPhysical( bg );
    uObjBg *objBg = (uObjBg*)&RDRAM[address];

    gSP.bgImage.address = RSP_SegmentToPhysical( objBg->imagePtr );
    gSP.bgImage.width = objBg->imageW >> 2;
    gSP.bgImage.height = objBg->imageH >> 2;
    gSP.bgImage.format = objBg->imageFmt;
    gSP.bgImage.size = objBg->imageSiz;
    gSP.bgImage.palette = objBg->imagePal;
    gDP.textureMode = TEXTUREMODE_BGIMAGE;

    u16 imageX = objBg->imageX >> 5;
    u16 imageY = objBg->imageY >> 5;

    s16 frameX = objBg->frameX / 4;
    s16 frameY = objBg->frameY / 4;
    u16 frameW = objBg->frameW >> 2;
    u16 frameH = objBg->frameH >> 2;

    gSPTexture( 1.0f, 1.0f, 0, 0, TRUE );

    gDPTextureRectangle( frameX, frameY, frameX + frameW - 1, frameY + frameH - 1, 0, imageX, imageY, 4, 1 );
}

void gSPObjRectangle( u32 sp )
{
    u32 address = RSP_SegmentToPhysical( sp );
    uObjSprite *objSprite = (uObjSprite*)&RDRAM[address];

    f32 scaleW = _FIXED2FLOAT( objSprite->scaleW, 10 );
    f32 scaleH = _FIXED2FLOAT( objSprite->scaleH, 10 );
    f32 objX = _FIXED2FLOAT( objSprite->objX, 2 );
    f32 objY = _FIXED2FLOAT( objSprite->objY, 2 );
    u32 imageW = objSprite->imageW >> 2;
    u32 imageH = objSprite->imageH >> 2;

    gDPTextureRectangle( objX, objY, objX + imageW / scaleW - 1, objY + imageH / scaleH - 1, 0, 0.0f, 0.0f, scaleW * (gDP.otherMode.cycleType == G_CYC_COPY ? 4.0f : 1.0f), scaleH );
}

void gSPObjLoadTxtr( u32 tx )
{
    u32 address = RSP_SegmentToPhysical( tx );
    uObjTxtr *objTxtr = (uObjTxtr*)&RDRAM[address];

    if ((gSP.status[objTxtr->block.sid >> 2] & objTxtr->block.mask) != objTxtr->block.flag)
    {
        switch (objTxtr->block.type)
        {
            case G_OBJLT_TXTRBLOCK:
                gDPSetTextureImage( 0, 1, 0, objTxtr->block.image );
                gDPSetTile( 0, 1, 0, objTxtr->block.tmem, 7, 0, 0, 0, 0, 0, 0, 0 );
                gDPLoadBlock( 7, 0, 0, ((objTxtr->block.tsize + 1) << 3) - 1, objTxtr->block.tline );
                break;
            case G_OBJLT_TXTRTILE:
                gDPSetTextureImage( 0, 1, (objTxtr->tile.twidth + 1) << 1, objTxtr->tile.image );
                gDPSetTile( 0, 1, (objTxtr->tile.twidth + 1) >> 2, objTxtr->tile.tmem, 7, 0, 0, 0, 0, 0, 0, 0 );
                gDPLoadTile( 7, 0, 0, (((objTxtr->tile.twidth + 1) << 1) - 1) << 2, (((objTxtr->tile.theight + 1) >> 2) - 1) << 2 );
                break;
            case G_OBJLT_TLUT:
                gDPSetTextureImage( 0, 2, 1, objTxtr->tlut.image );
                gDPSetTile( 0, 2, 0, objTxtr->tlut.phead, 7, 0, 0, 0, 0, 0, 0, 0 );
                gDPLoadTLUT( 7, 0, 0, objTxtr->tlut.pnum << 2, 0 );
                break;
        }
        gSP.status[objTxtr->block.sid >> 2] = (gSP.status[objTxtr->block.sid >> 2] & ~objTxtr->block.mask) | (objTxtr->block.flag & objTxtr->block.mask);
    }
}

void gSPObjSprite( u32 sp )
{
    u32 address = RSP_SegmentToPhysical( sp );
    uObjSprite *objSprite = (uObjSprite*)&RDRAM[address];

    f32 scaleW = _FIXED2FLOAT( objSprite->scaleW, 10 );
    f32 scaleH = _FIXED2FLOAT( objSprite->scaleH, 10 );
    f32 objX = _FIXED2FLOAT( objSprite->objX, 2 );
    f32 objY = _FIXED2FLOAT( objSprite->objY, 2 );
    u32 imageW = objSprite->imageW >> 5;
    u32 imageH = objSprite->imageH >> 5;

    f32 x0 = objX;
    f32 y0 = objY;
    f32 x1 = objX + imageW / scaleW - 1;
    f32 y1 = objY + imageH / scaleH - 1;

    s32 v0=0,v1=1,v2=2,v3=3;

#ifdef __TRIBUFFER_OPT
    v0 = OGL.triangles.indexmap[v0];
    v1 = OGL.triangles.indexmap[v1];
    v2 = OGL.triangles.indexmap[v2];
    v3 = OGL.triangles.indexmap[v3];
#endif

    OGL.triangles.vertices[v0].x = gSP.objMatrix.A * x0 + gSP.objMatrix.B * y0 + gSP.objMatrix.X;
    OGL.triangles.vertices[v0].y = gSP.objMatrix.C * x0 + gSP.objMatrix.D * y0 + gSP.objMatrix.Y;
    OGL.triangles.vertices[v0].z = 0.0f;
    OGL.triangles.vertices[v0].w = 1.0f;
    OGL.triangles.vertices[v0].s = 0.0f;
    OGL.triangles.vertices[v0].t = 0.0f;
    OGL.triangles.vertices[v1].x = gSP.objMatrix.A * x1 + gSP.objMatrix.B * y0 + gSP.objMatrix.X;
    OGL.triangles.vertices[v1].y = gSP.objMatrix.C * x1 + gSP.objMatrix.D * y0 + gSP.objMatrix.Y;
    OGL.triangles.vertices[v1].z = 0.0f;
    OGL.triangles.vertices[v1].w = 1.0f;
    OGL.triangles.vertices[v1].s = imageW - 1;
    OGL.triangles.vertices[v1].t = 0.0f;
    OGL.triangles.vertices[v2].x = gSP.objMatrix.A * x1 + gSP.objMatrix.B * y1 + gSP.objMatrix.X;
    OGL.triangles.vertices[v2].y = gSP.objMatrix.C * x1 + gSP.objMatrix.D * y1 + gSP.objMatrix.Y;
    OGL.triangles.vertices[v2].z = 0.0f;
    OGL.triangles.vertices[v2].w = 1.0f;
    OGL.triangles.vertices[v2].s = imageW - 1;
    OGL.triangles.vertices[v2].t = imageH - 1;
    OGL.triangles.vertices[v3].x = gSP.objMatrix.A * x0 + gSP.objMatrix.B * y1 + gSP.objMatrix.X;
    OGL.triangles.vertices[v3].y = gSP.objMatrix.C * x0 + gSP.objMatrix.D * y1 + gSP.objMatrix.Y;
    OGL.triangles.vertices[v3].z = 0.0f;
    OGL.triangles.vertices[v3].w = 1.0f;
    OGL.triangles.vertices[v3].s = 0;
    OGL.triangles.vertices[v3].t = imageH - 1;

    gDPSetTile( objSprite->imageFmt, objSprite->imageSiz, objSprite->imageStride, objSprite->imageAdrs, 0, objSprite->imagePal, G_TX_CLAMP, G_TX_CLAMP, 0, 0, 0, 0 );
    gDPSetTileSize( 0, 0, 0, (imageW - 1) << 2, (imageH - 1) << 2 );
    gSPTexture( 1.0f, 1.0f, 0, 0, TRUE );

    //glOrtho( 0, VI.width, VI.height, 0, 0.0f, 32767.0f );
    OGL.triangles.vertices[v0].x = 2.0f * VI.rwidth * OGL.triangles.vertices[v0].x - 1.0f;
    OGL.triangles.vertices[v0].y = -2.0f * VI.rheight * OGL.triangles.vertices[v0].y + 1.0f;
    OGL.triangles.vertices[v0].z = -1.0f;
    OGL.triangles.vertices[v0].w = 1.0f;
    OGL.triangles.vertices[v1].x = 2.0f * VI.rwidth * OGL.triangles.vertices[v0].x - 1.0f;
    OGL.triangles.vertices[v1].y = -2.0f * VI.rheight * OGL.triangles.vertices[v0].y + 1.0f;
    OGL.triangles.vertices[v1].z = -1.0f;
    OGL.triangles.vertices[v1].w = 1.0f;
    OGL.triangles.vertices[v2].x = 2.0f * VI.rwidth * OGL.triangles.vertices[v0].x - 1.0f;
    OGL.triangles.vertices[v2].y = -2.0f * VI.rheight * OGL.triangles.vertices[v0].y + 1.0f;
    OGL.triangles.vertices[v2].z = -1.0f;
    OGL.triangles.vertices[v2].w = 1.0f;
    OGL.triangles.vertices[v3].x = 2.0f * VI.rwidth * OGL.triangles.vertices[v0].x - 1.0f;
    OGL.triangles.vertices[v3].y = -2.0f * VI.rheight * OGL.triangles.vertices[v0].y + 1.0f;
    OGL.triangles.vertices[v3].z = -1.0f;
    OGL.triangles.vertices[v3].w = 1.0f;

    OGL_AddTriangle(v0, v1, v2);
    OGL_AddTriangle(v0, v2, v3);
    OGL_DrawTriangles();

    if (depthBuffer.current) depthBuffer.current->cleared = FALSE;
    gDP.colorImage.changed = TRUE;
    gDP.colorImage.height = (unsigned int)(max( gDP.colorImage.height, gDP.scissor.lry ));
}

void gSPObjLoadTxSprite( u32 txsp )
{
    gSPObjLoadTxtr( txsp );
    gSPObjSprite( txsp + sizeof( uObjTxtr ) );
}

void gSPObjLoadTxRectR( u32 txsp )
{
    gSPObjLoadTxtr( txsp );
//  gSPObjRectangleR( txsp + sizeof( uObjTxtr ) );
}

void gSPObjMatrix( u32 mtx )
{
    u32 address = RSP_SegmentToPhysical( mtx );
    uObjMtx *objMtx = (uObjMtx*)&RDRAM[address];

    gSP.objMatrix.A = _FIXED2FLOAT( objMtx->A, 16 );
    gSP.objMatrix.B = _FIXED2FLOAT( objMtx->B, 16 );
    gSP.objMatrix.C = _FIXED2FLOAT( objMtx->C, 16 );
    gSP.objMatrix.D = _FIXED2FLOAT( objMtx->D, 16 );
    gSP.objMatrix.X = _FIXED2FLOAT( objMtx->X, 2 );
    gSP.objMatrix.Y = _FIXED2FLOAT( objMtx->Y, 2 );
    gSP.objMatrix.baseScaleX = _FIXED2FLOAT( objMtx->BaseScaleX, 10 );
    gSP.objMatrix.baseScaleY = _FIXED2FLOAT( objMtx->BaseScaleY, 10 );
}

void gSPObjSubMatrix( u32 mtx )
{
}


#ifdef __VEC4_OPT
void (*gSPTransformVertex4)(u32 v, float mtx[4][4]) =
        gSPTransformVertex4_default;
void (*gSPTransformNormal4)(u32 v, float mtx[4][4]) =
        gSPTransformNormal4_default;
void (*gSPLightVertex4)(u32 v) = gSPLightVertex4_default;
void (*gSPBillboardVertex4)(u32 v) = gSPBillboardVertex4_default;
#endif
void (*gSPTransformVertex)(float vtx[4], float mtx[4][4]) =
        gSPTransformVertex_default;
void (*gSPLightVertex)(u32 v) = gSPLightVertex_default;
void (*gSPBillboardVertex)(u32 v, u32 i) = gSPBillboardVertex_default;

