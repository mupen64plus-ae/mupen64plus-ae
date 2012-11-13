#include "Common.h"
#include "gles2N64.h"
#include "Debug.h"
#include "F3D.h"
#include "F3DEX.h"
#include "F3DEX2.h"
#include "F3DCBFD.h"
#include "S2DEX.h"
#include "S2DEX2.h"
#include "N64.h"
#include "RSP.h"
#include "RDP.h"
#include "gSP.h"
#include "gDP.h"
#include "GBI.h"
#include "OpenGL.h"
#include "Config.h"

//BASED ON GLIDE64 Implementation

u32 normal_address = 0;

void F3DCBFD_Vtx(u32 w0, u32 w1)
{

	s32 v0, n;
    u32 address;
	n = (w0 >> 12)&0xFF;
	v0 = ((w0 >> 1)&0x7F) - n;
	address = RSP_SegmentToPhysical(w1);

	if (v0 < 0)
	{
		return;
	}

    gSPFlushTriangles();

    Vertex* vertex = (Vertex*)&RDRAM[address];

	for (s32 i=0; i < n; i++)
	{
        u32 v;
#ifdef __TRIBUFFER_OPT
        v = __indexmap_getnew(i, 1);
#else
        v = i;
#endif

		OGL.triangles.vertices[v].x = vertex->x;
		OGL.triangles.vertices[v].y = vertex->y;
		OGL.triangles.vertices[v].z = vertex->z;
		OGL.triangles.vertices[v].w = 1.0f;

		OGL.triangles.vertices[v].s = _FIXED2FLOAT(vertex->s, 5);
		OGL.triangles.vertices[v].t = _FIXED2FLOAT(vertex->t, 5);

        if (config.enableLighting && gSP.geometryMode & G_LIGHTING)
		{
			OGL.triangles.vertices[v].nx = ((s8*)RDRAM)[(normal_address + (i<<2) + (v0<<1) + 0)^3];
			OGL.triangles.vertices[v].ny = ((s8*)RDRAM)[(normal_address + (i<<2) + (v0<<1) + 1)^3];
			OGL.triangles.vertices[v].nz = (s8)(vertex->flag&0xff);
		}

        gSPProcessVertex(v);

        if (config.enableLighting && gSP.geometryMode & G_LIGHTING)
		{
            OGL.triangles.vertices[v].r = OGL.triangles.vertices[v].r * vertex->color.r * 0.0039215689f;
            OGL.triangles.vertices[v].g = OGL.triangles.vertices[v].g * vertex->color.g * 0.0039215689f;
            OGL.triangles.vertices[v].b = OGL.triangles.vertices[v].b * vertex->color.b * 0.0039215689f;
            OGL.triangles.vertices[v].a = OGL.triangles.vertices[v].a * vertex->color.a * 0.0039215689f;
		}
		else
		{
            OGL.triangles.vertices[v].r = vertex->color.r * 0.0039215689f;
            OGL.triangles.vertices[v].g = vertex->color.g * 0.0039215689f;
            OGL.triangles.vertices[v].b = vertex->color.b * 0.0039215689f;
            OGL.triangles.vertices[v].a = vertex->color.a * 0.0039215689f;
		}
		vertex++;
    }
}

void F3DCBFD_MoveWord(u32 w0, u32 w1)
{
	u8 index = (u8)((w0 >> 16) & 0xFF);
	u16 offset = (u16)(w0 & 0xFFFF);

	switch (index)
	{
        case G_MW_NUMLIGHT:
            gSPNumLights(w1 / 48);
            break;

        case G_MW_CLIP:
            if (offset == 0x04)
            {
                gSPClipRatio( w1 );
            }
            break;

        case G_MW_SEGMENT:
            gSPSegment(_SHIFTR(offset, 2, 4), w1);
            break;

        case G_MW_FOG:
            gSPFogFactor( (s16)_SHIFTR( w1, 16, 16 ), (s16)_SHIFTR( w1, 0, 16 ) );
            break;

        case G_MV_COORDMOD:  // moveword coord mod
            break;

        default:
            break;
    }
}

#define F3DCBFD_MV_VIEWPORT     8
#define F3DCBFD_MV_LIGHT        10
#define F3DCBFD_MV_NORMAL       14

void F3DCBFD_MoveMem(u32 w0, u32 w1)
{
#ifdef __TRIBUFFER_OPT
    gSPFlushTriangles();
#endif
    switch (_SHIFTR( w0, 0, 8 ))
    {
        case F3DCBFD_MV_VIEWPORT:
            gSPViewport(w1);
            break;

        case F3DCBFD_MV_LIGHT:
        {
            u32 offset = _SHIFTR( w0, 8, 8 ) << 3;
            if (offset >= 48)
            {
                gSPLight( w1, (offset - 24) / 24);
            }
            break;
        }

        case F3DCBFD_MV_NORMAL:
			normal_address = RSP_SegmentToPhysical(w1);
            break;

    }
}

void F3DCBFD_Tri4(u32 w0, u32 w1)
{
    gSP4Triangles( _SHIFTR(w0, 23, 5), _SHIFTR(w0, 18, 5), (_SHIFTR(w0, 15, 3 ) << 2) | _SHIFTR(w1, 30, 2),
                   _SHIFTR(w0, 10, 5), _SHIFTR(w0, 5, 5), _SHIFTR(w1, 0, 5),
                   _SHIFTR(w1, 25, 5), _SHIFTR(w1, 20, 5), _SHIFTR(w1, 15, 5),
                   _SHIFTR(w1, 10, 5), _SHIFTR(w1, 5, 5), _SHIFTR(w1, 0, 5));
}


void F3DCBFD_Init()
{
    LOG(LOG_VERBOSE, "USING CBFD ucode!\n");

    // Set GeometryMode flags
    GBI_InitFlags(F3DEX2);

    GBI.PCStackSize = 10;

    // GBI Command                      Command Value               Command Function
    GBI_SetGBI( G_RDPHALF_2,            F3DEX2_RDPHALF_2,           F3D_RDPHalf_2 );
    GBI_SetGBI( G_SETOTHERMODE_H,       F3DEX2_SETOTHERMODE_H,      F3DEX2_SetOtherMode_H );
    GBI_SetGBI( G_SETOTHERMODE_L,       F3DEX2_SETOTHERMODE_L,      F3DEX2_SetOtherMode_L );
    GBI_SetGBI( G_RDPHALF_1,            F3DEX2_RDPHALF_1,           F3D_RDPHalf_1 );
    GBI_SetGBI( G_SPNOOP,               F3DEX2_SPNOOP,              F3D_SPNoOp );
    GBI_SetGBI( G_ENDDL,                F3DEX2_ENDDL,               F3D_EndDL );
    GBI_SetGBI( G_DL,                   F3DEX2_DL,                  F3D_DList );
    GBI_SetGBI( G_LOAD_UCODE,           F3DEX2_LOAD_UCODE,          F3DEX_Load_uCode );
    GBI_SetGBI( G_MOVEMEM,              F3DEX2_MOVEMEM,             F3DCBFD_MoveMem);
    GBI_SetGBI( G_MOVEWORD,             F3DEX2_MOVEWORD,            F3DCBFD_MoveWord);
    GBI_SetGBI( G_MTX,                  F3DEX2_MTX,                 F3DEX2_Mtx );
    GBI_SetGBI( G_GEOMETRYMODE,         F3DEX2_GEOMETRYMODE,        F3DEX2_GeometryMode );
    GBI_SetGBI( G_POPMTX,               F3DEX2_POPMTX,              F3DEX2_PopMtx );
    GBI_SetGBI( G_TEXTURE,              F3DEX2_TEXTURE,             F3DEX2_Texture );
    GBI_SetGBI( G_DMA_IO,               F3DEX2_DMA_IO,              F3DEX2_DMAIO );
    GBI_SetGBI( G_SPECIAL_1,            F3DEX2_SPECIAL_1,           F3DEX2_Special_1 );
    GBI_SetGBI( G_SPECIAL_2,            F3DEX2_SPECIAL_2,           F3DEX2_Special_2 );
    GBI_SetGBI( G_SPECIAL_3,            F3DEX2_SPECIAL_3,           F3DEX2_Special_3 );



    GBI_SetGBI(G_VTX,                   F3DEX2_VTX,                 F3DCBFD_Vtx);
    GBI_SetGBI(G_MODIFYVTX,             F3DEX2_MODIFYVTX,           F3DEX_ModifyVtx);
    GBI_SetGBI(G_CULLDL,                F3DEX2_CULLDL,              F3DEX_CullDL);
    GBI_SetGBI(G_BRANCH_Z,              F3DEX2_BRANCH_Z,            F3DEX_Branch_Z);
    GBI_SetGBI(G_TRI1,                  F3DEX2_TRI1,                F3DEX2_Tri1);
    GBI_SetGBI(G_TRI2,                  F3DEX2_TRI2,                F3DEX_Tri2);
    GBI_SetGBI(G_QUAD,                  F3DEX2_QUAD,                F3DEX2_Quad);
//  GBI_SetGBI( G_LINE3D,               F3DEX2_LINE3D,              F3DEX2_Line3D );

    //for some reason glide64 maps TRI4 to these locations:

    for(int i = 0x10; i <= 0x1F; i++)
    {
        GBI_SetGBI(G_TRI4, i, F3DCBFD_Tri4);
    }

    GBI_SetGBI( G_BG_1CYC,              S2DEX2_BG_1CYC,             S2DEX_BG_1Cyc);
    GBI_SetGBI( G_BG_COPY,              S2DEX2_BG_COPY,             S2DEX_BG_Copy);
    GBI_SetGBI( G_OBJ_RENDERMODE,       S2DEX2_OBJ_RENDERMODE,      S2DEX_Obj_RenderMode);

}

