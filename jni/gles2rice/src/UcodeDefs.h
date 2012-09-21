/*
Copyright (C) 2003 Rice1964

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/

#include "typedefs.h"

#ifndef _UCODE_DEFS_H_
#define _UCODE_DEFS_H_

struct Instruction
{
    union 
    {
        uint32 cmd0;
        struct 
        {
            uint32 arg0:24;
            uint32 cmd:8;
        };
    };
    union 
    {
        uint32 cmd1;
        struct 
        {
            uint32 arg1:24;
            uint32 pad:8;
        };
    };
};

struct GGBI1_Matrix
{
    uint32 len:16;
    uint32 projection:1;
    uint32 load:1;
    uint32 push:1;
    uint32 :5;
    uint32 cmd:8;
    uint32    addr;
};
        
struct GGBI1_PopMatrix
{
    uint32 :24;
    uint32 cmd:8;
    uint32 projection:1;
    uint32 :31;
};

struct GGBI2_Matrix
{
    union 
    {
        struct 
        {
            uint32 param:8;
            uint32 len:16;
            uint32 cmd:8;
        };
        
        struct 
        {
            uint32 nopush:1;
            uint32 load:1;
            uint32 projection:1;
            uint32 :5;
            uint32 len2:16;
            uint32 cmd2:8;
        };
    };
    uint32 addr;
};


struct GGBI0_Vtx
{
    uint32 len:16;
    uint32 v0:4;
    uint32 n:4;
    uint32 cmd:8;
    uint32 addr;
};

struct GGBI1_Vtx
{
    uint32 len:10;
    uint32 n:6;
    uint32 :1;
    uint32 v0:7;
    uint32 cmd:8;
    uint32 addr;
};

struct GGBI2_Vtx
{
    uint32 vend:8;
    uint32 :4;
    uint32 n:8;
    uint32 :4;
    uint32 cmd:8;
    uint32 addr;
};

struct GGBI1_BranchZ
{
    uint32 pad0:1;      
    uint32 vtx:11;     
    uint32 pad1:12;       
    uint32 cmd:8;         
    uint32 value:32;     
}; 

struct GGBI1_ModifyVtx
{
    uint32 pad0:1;          
    uint32 vtx:15;  
    uint32 offset:8;    
    uint32 cmd:8;           
    uint32 value;
};

struct GBI_Texture
{
    uint32 enable_gbi0:1;
    uint32 enable_gbi2:1;
    uint32 :6;
    uint32 tile:3;
    uint32 level:3;
    uint32 :10;
    uint32 cmd:8;
    uint32 scaleT:16;
    uint32 scaleS:16;
};

struct SetCullDL
{
    uint32 pad0:1;             
    uint32 first:15;   
    uint32 pad2:8;            
    uint32 cmd:8;             
    uint32 pad3:1;            
    uint32 end:15;    
    uint32 pad4:8;             
};

struct SetTImg
{
    uint32 width:12;
    uint32 :7;
    uint32 siz:2;
    uint32 fmt:3;
    uint32 cmd:8;
    uint32 addr;
};

struct LoadTile
{
    uint32 tl:12; //Top
    uint32 sl:12; //Left
    uint32 cmd:8;

    uint32 th:12; //Bottom
    uint32 sh:12; //Right
    uint32 tile:3;
    uint32 pad:5;
};


struct GGBI1_MoveWord
{
    uint32 type:8;
    uint32 offset:16;
    uint32 cmd:8;
    uint32 value;
};

struct GGBI2_MoveWord
{
    uint32 offset:16;
    uint32 type:8;
    uint32 cmd:8;
    uint32 value;
};

struct GGBI2_Tri1
{
    uint32 v0:8;
    uint32 v1:8;
    uint32 v2:8;
    uint32 cmd:8;
    uint32 pad:24;
    uint32 flag:8;
};

struct GGBI2_Tri2
{
    uint32 :1;
    uint32 v3:7;
    uint32 :1;
    uint32 v4:7;
    uint32 :1;
    uint32 v5:7;
    uint32 cmd:8;
    uint32 :1;
    uint32 v0:7;
    uint32 :1;
    uint32 v1:7;
    uint32 :1;
    uint32 v2:7;
    uint32 flag:8;
};

struct GGBI2_Line3D
{
    uint32 v3:8;
    uint32 v4:8;
    uint32 v5:8;
    uint32 cmd:8;

    uint32 v0:8;
    uint32 v1:8;
    uint32 v2:8;
    uint32 flag:8;
};

struct GGBI1_Line3D
{
    uint32 w0;
    uint32 v2:8;
    uint32 v1:8;
    uint32 v0:8;
    uint32 v3:8;
};

struct GGBI1_Tri1
{
    uint32 w0;
    uint32 v2:8;
    uint32 v1:8;
    uint32 v0:8;
    uint32 flag:8;
};

struct GGBI1_Tri2
{
    uint32 v5:8;
    uint32 v4:8;
    uint32 v3:8;
    uint32 cmd:8;

    uint32 v2:8;
    uint32 v1:8;
    uint32 v0:8;
    uint32 flag:8;
};

struct GGBI0_Tri4
{
    uint32 v0:4;
    uint32 v3:4;
    uint32 v6:4;
    uint32 v9:4;
    uint32 pad:8;
    uint32 cmd:8;
    uint32 v1:4;
    uint32 v2:4;
    uint32 v4:4;
    uint32 v5:4;
    uint32 v7:4;
    uint32 v8:4;
    uint32 v10:4;
    uint32 v11:4;
};

struct GSetColor
{
    uint32 prim_level:8;
    uint32 prim_min_level:8;
    uint32 pad:8;
    uint32 cmd:8;

    union 
    {
        uint32 color;
        struct 
        {
            uint32 fillcolor:16;
            uint32 fillcolor2:16;
        };
        
        struct 
        {
            uint32 a:8;
            uint32 b:8;
            uint32 g:8;
            uint32 r:8;
        };
    };
};

struct GGBI1_Dlist {
    uint32 :16;
    uint32 param:8;
    uint32 cmd:8;
    uint32 addr;
};

struct Gsettile
{
    uint32 tmem:9;
    uint32 line:9;
    uint32 pad0:1;
    uint32 siz:2;
    uint32 fmt:3;
    uint32 cmd:8;

    uint32 shifts:4;
    uint32 masks:4;
    uint32 ms:1;
    uint32 cs:1;
    uint32 shiftt:4;
    uint32 maskt:4;
    uint32 mt:1;
    uint32 ct:1;
    uint32 palette:4;
    uint32 tile:3;
    uint32 pad1:5;
};

struct SetFillRect
{
    uint32 pad1    : 2;
    uint32 y1      : 10;
    uint32 pad0    : 2;
    uint32 x1      : 10;
    uint32 cmd     : 8;

    uint32 pad3    : 2;
    uint32 y0      : 10;
    uint32 pad4    : 2;
    uint32 x0      : 10;
    uint32 pad2    : 8;
};

struct SetPrimDepth
{
    uint32 pad0:24;
    uint32 cmd:8; 
    uint32 dz:16;   
    uint32 z:15;   
    uint32 pad:1;
};

struct SetOthermode
{
    uint32 len:8;
    uint32 sft:8;
    uint32 cmd:8;
    uint32 data;
};

struct TriDKR
{
    unsigned char  v2, v1, v0, flag;
    signed short    t0, s0;
    signed short    t1, s1;
    signed short    t2, s2;
};

union Gfx
{
    Instruction     words;
    GGBI0_Vtx       vtx0;
    GGBI1_Vtx       vtx1;
    GGBI2_Vtx       vtx2;
    
    GGBI1_ModifyVtx modifyvtx;
    GGBI1_BranchZ   branchz;
    GGBI1_Matrix    mtx1;
    GGBI2_Matrix    mtx2;
    GGBI1_PopMatrix popmtx;

    GGBI1_Line3D    gbi1line3d;
    GGBI1_Tri1      gbi1tri1;
    GGBI1_Tri2      gbi1tri2;
    GGBI2_Line3D    gbi2line3d;
    GGBI2_Tri1      gbi2tri1;
    GGBI2_Tri2      gbi2tri2;
    GGBI0_Tri4      tri4;

    GGBI1_MoveWord  mw1;
    GGBI2_MoveWord  mw2;
    GBI_Texture     texture;
    GGBI1_Dlist     dlist;

    SetCullDL       culldl; 
    SetTImg         img;
    GSetColor       setcolor;
    LoadTile        loadtile;
    SetFillRect     fillrect;
    SetPrimDepth    primdepth;
    SetOthermode    othermode;
    Gsettile        settile;
    /*
    Gdma        dma;
    Gsegment    segment;
    GsetothermodeH  setothermodeH;
    GsetothermodeL  setothermodeL;
    Gtexture    texture;
    Gperspnorm  perspnorm;
    Gsetcombine setcombine;
    Gfillrect   fillrect;
    Gsettile    settile;
    Gloadtile   loadtile;
    Gsettilesize    settilesize;
    Gloadtlut   loadtlut;
    */
    long long int   force_structure_alignment;
};

typedef union
{
    struct
    {
        unsigned int    w0;
        unsigned int    w1;
        unsigned int    w2;
        unsigned int    w3;
    };
    
    struct
    {
        unsigned int    yl:12;  /* Y coordinate of upper left   */
        unsigned int    xl:12;  /* X coordinate of upper left   */
        unsigned int    cmd:8;  /* command          */

        unsigned int    yh:12;  /* Y coordinate of lower right  */
        unsigned int    xh:12;  /* X coordinate of lower right  */
        unsigned int    tile:3; /* Tile descriptor index    */
        unsigned int    pad1:5; /* Padding          */

        unsigned int    t:16;   /* T texture coord at top left  */
        unsigned int    s:16;   /* S texture coord at top left  */

        unsigned int    dtdy:16;/* Change in T per change in Y  */
        unsigned int    dsdx:16;/* Change in S per change in X  */
    };
} Gtexrect;

#endif

