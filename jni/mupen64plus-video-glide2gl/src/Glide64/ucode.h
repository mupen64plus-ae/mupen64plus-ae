/*
* Glide64 - Glide video plugin for Nintendo 64 emulators.
* Copyright (c) 2002  Dave2001
* Copyright (c) 2003-2009  Sergey 'Gonetz' Lipski
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

//****************************************************************
//
// Glide64 - Glide Plugin for Nintendo 64 emulators
// Project started on December 29th, 2001
//
// Authors:
// Dave2001, original author, founded the project in 2001, left it in 2002
// Gugaman, joined the project in 2002, left it in 2002
// Sergey 'Gonetz' Lipski, joined the project in 2002, main author since fall of 2002
// Hiroshi 'KoolSmoky' Morii, joined the project in 2007
//
//****************************************************************
//
// To modify Glide64:
// * Write your name and (optional)email, commented by your work, so I know who did it, and so that you can find which parts you modified when it comes time to send it to me.
// * Do NOT send me the whole project or file that you modified.  Take out your modified code sections, and tell me where to put them.  If people sent the whole thing, I would have many different versions, but no idea how to combine them all.
//
//****************************************************************

#ifndef _GLIDE64_UCODE_GENERIC_H
#define _GLIDE64_UCODE_GENERIC_H

#define ucode_Fast3D 0        // ** F3D     **
#define ucode_F3DEX 1         // ** F3DEX   **
#define ucode_F3DEX2 2        // ** F3DEX2  **
#define ucode_WaveRace 3      // ** WRUS    **
#define ucode_StarWars 4      // ** SWSE    **
#define ucode_DiddyKong 5     // ** F3DDKR  **
#define ucode_S2DEX 6         // ** S2DEX   **
#define ucode_PerfectDark 7   // ** F3DPD   **
#define ucode_CBFD 8          // ** F3DCBFD **
#define ucode_zSort 9         // ** ZSORT   **
#define ucode_Turbo3d 21      // ** TURBO3D **

// ** F3D **

#define F3D_MTX_STACKSIZE       10

#define F3D_MTX_MODELVIEW       0x00
#define F3D_MTX_PROJECTION      0x01
#define F3D_MTX_MUL             0x00
#define F3D_MTX_LOAD            0x02
#define F3D_MTX_NOPUSH          0x00
#define F3D_MTX_PUSH            0x04

#define F3D_TEXTURE_ENABLE      0x00000002
#define F3D_SHADING_SMOOTH      0x00000200
#define F3D_CULL_FRONT          0x00001000
#define F3D_CULL_BACK           0x00002000
#define F3D_CULL_BOTH           0x00003000
#define F3D_CLIPPING            0x00000000

#define F3D_MV_VIEWPORT         0x80

#define F3D_MWO_aLIGHT_1        0x00
#define F3D_MWO_bLIGHT_1        0x04
#define F3D_MWO_aLIGHT_2        0x20
#define F3D_MWO_bLIGHT_2        0x24
#define F3D_MWO_aLIGHT_3        0x40
#define F3D_MWO_bLIGHT_3        0x44
#define F3D_MWO_aLIGHT_4        0x60
#define F3D_MWO_bLIGHT_4        0x64
#define F3D_MWO_aLIGHT_5        0x80
#define F3D_MWO_bLIGHT_5        0x84
#define F3D_MWO_aLIGHT_6        0xa0
#define F3D_MWO_bLIGHT_6        0xa4
#define F3D_MWO_aLIGHT_7        0xc0
#define F3D_MWO_bLIGHT_7        0xc4
#define F3D_MWO_aLIGHT_8        0xe0
#define F3D_MWO_bLIGHT_8        0xe4

// FAST3D commands
#define F3D_SPNOOP              0x00
#define F3D_MTX                 0x01
#define F3D_RESERVED0           0x02
#define F3D_MOVEMEM             0x03
#define F3D_VTX                 0x04
#define F3D_RESERVED1           0x05
#define F3D_DL                  0x06
#define F3D_RESERVED2           0x07
#define F3D_RESERVED3           0x08
#define F3D_SPRITE2D_BASE       0x09

#define F3D_TRI1                0xBF
#define F3D_CULLDL              0xBE
#define F3D_POPMTX              0xBD
#define F3D_MOVEWORD            0xBC
#define F3D_TEXTURE             0xBB
#define F3D_SETOTHERMODE_H      0xBA
#define F3D_SETOTHERMODE_L      0xB9
#define F3D_ENDDL               0xB8
#define F3D_SETGEOMETRYMODE     0xB7
#define F3D_CLEARGEOMETRYMODE   0xB6
//#define F3D_LINE3D                0xB5 // Only used in Line3D
#define F3D_QUAD                0xB5
#define F3D_RDPHALF_1           0xB4
#define F3D_RDPHALF_2           0xB3
#define F3D_RDPHALF_CONT        0xB2
#define F3D_TRI4                0xB1

#define F3D_TRI_UNKNOWN         0xC0

// ** F3DEX2 **
//
#define F3DEX2_MTX_STACKSIZE        18

#define F3DEX2_MTX_MODELVIEW        0x00
#define F3DEX2_MTX_PROJECTION       0x04
#define F3DEX2_MTX_MUL              0x00
#define F3DEX2_MTX_LOAD             0x02
#define F3DEX2_MTX_NOPUSH           0x00
#define F3DEX2_MTX_PUSH             0x01

#define F3DEX2_TEXTURE_ENABLE       0x00000000
#define F3DEX2_SHADING_SMOOTH       0x00200000
#define F3DEX2_CULL_FRONT           0x00000200
#define F3DEX2_CULL_BACK            0x00000400
#define F3DEX2_CULL_BOTH            0x00000600
#define F3DEX2_CLIPPING             0x00800000

#define F3DEX2_MV_VIEWPORT          8

#define F3DEX2_MWO_aLIGHT_1     0x00
#define F3DEX2_MWO_bLIGHT_1     0x04
#define F3DEX2_MWO_aLIGHT_2     0x18
#define F3DEX2_MWO_bLIGHT_2     0x1c
#define F3DEX2_MWO_aLIGHT_3     0x30
#define F3DEX2_MWO_bLIGHT_3     0x34
#define F3DEX2_MWO_aLIGHT_4     0x48
#define F3DEX2_MWO_bLIGHT_4     0x4c
#define F3DEX2_MWO_aLIGHT_5     0x60
#define F3DEX2_MWO_bLIGHT_5     0x64
#define F3DEX2_MWO_aLIGHT_6     0x78
#define F3DEX2_MWO_bLIGHT_6     0x7c
#define F3DEX2_MWO_aLIGHT_7     0x90
#define F3DEX2_MWO_bLIGHT_7     0x94
#define F3DEX2_MWO_aLIGHT_8     0xa8
#define F3DEX2_MWO_bLIGHT_8     0xac


#define F3DEX2_RDPHALF_2        0xF1
#define F3DEX2_SETOTHERMODE_H   0xE3
#define F3DEX2_SETOTHERMODE_L   0xE2
#define F3DEX2_RDPHALF_1        0xE1
#define F3DEX2_SPNOOP           0xE0
#define F3DEX2_ENDDL            0xDF
#define F3DEX2_DL               0xDE
#define F3DEX2_LOAD_UCODE       0xDD
#define F3DEX2_MOVEMEM          0xDC
#define F3DEX2_MOVEWORD         0xDB
#define F3DEX2_MTX              0xDA
#define F3DEX2_GEOMETRYMODE     0xD9
#define F3DEX2_POPMTX           0xD8
#define F3DEX2_TEXTURE          0xD7
#define F3DEX2_DMA_IO           0xD6
#define F3DEX2_SPECIAL_1        0xD5
#define F3DEX2_SPECIAL_2        0xD4
#define F3DEX2_SPECIAL_3        0xD3

#define F3DEX2_VTX              0x01
#define F3DEX2_MODIFYVTX        0x02
#define F3DEX2_CULLDL           0x03
#define F3DEX2_BRANCH_Z         0x04
#define F3DEX2_TRI1             0x05
#define F3DEX2_TRI2             0x06
#define F3DEX2_QUAD             0x07
#define F3DEX2_LINE3D			  0x08

// ** RDP graphics functions **

static void rdp_texrect(uint32_t w0, uint32_t w1);
static void rdp_setscissor(uint32_t w0, uint32_t w1);
static void rdp_setothermode(uint32_t w0, uint32_t w1);
static void rdp_loadtlut(uint32_t w0, uint32_t w1);
static void rdp_settilesize(uint32_t w0, uint32_t w1);
static void rdp_loadblock(uint32_t w0, uint32_t w1);
static void rdp_loadtile(uint32_t w0, uint32_t w1);
static void rdp_settile(uint32_t w0, uint32_t w1);
static void rdp_fillrect(uint32_t w0, uint32_t w1);
static void rdp_setprimcolor(uint32_t w0, uint32_t w1);
static void rdp_setcombine(uint32_t w0, uint32_t w1);
static void rdp_settextureimage(uint32_t w0, uint32_t w1);
static void rdp_setdepthimage(uint32_t w0, uint32_t w1);
static void rdp_setcolorimage(uint32_t w0, uint32_t w1);
static void rdp_trifill(uint32_t w0, uint32_t w1);
static void rdp_trishade(uint32_t w0, uint32_t w1);
static void rdp_tritxtr(uint32_t w0, uint32_t w1);
static void rdp_trishadetxtr(uint32_t w0, uint32_t w1);
static void rdp_trifillz(uint32_t w0, uint32_t w1);
static void rdp_trishadez(uint32_t w0, uint32_t w1);
static void rdp_tritxtrz(uint32_t w0, uint32_t w1);
static void rdp_trishadetxtrz(uint32_t w0, uint32_t w1);
static void rdphalf_1(uint32_t w0, uint32_t w1);
static void rdphalf_2(uint32_t w0, uint32_t w1);
static void rdphalf_cont(uint32_t w0, uint32_t w1);

static void rsp_reserved0(uint32_t w0, uint32_t w1);
static void rsp_uc5_reserved0(uint32_t w0, uint32_t w1);
static void rsp_reserved1(uint32_t w0, uint32_t w1);
static void rsp_reserved2(uint32_t w0, uint32_t w1);
static void rsp_reserved3(uint32_t w0, uint32_t w1);

static void ys_memrect(uint32_t w0, uint32_t w1);

static void uc6_obj_sprite(uint32_t w0, uint32_t w1);

static void modelview_load (float m[4][4]);
static void modelview_mul (float m[4][4]);
static void modelview_push(void);
static void modelview_load_push (float m[4][4]);
static void modelview_mul_push (float m[4][4]);
static void projection_load (float m[4][4]);
static void projection_mul (float m[4][4]);
static void load_matrix (float m[4][4], uint32_t addr);

static float set_sprite_combine_mode(void);

//ucode 00
static void uc0_vertex(uint32_t w0, uint32_t w1);
static void uc0_matrix(uint32_t w0, uint32_t w1);
static void uc0_movemem(uint32_t w0, uint32_t w1);
static void uc0_displaylist(uint32_t w0, uint32_t w1);
static void uc0_tri1(uint32_t w0, uint32_t w1);
static void uc0_tri1_mischief(uint32_t w0, uint32_t w1);
static void uc0_enddl(uint32_t w0, uint32_t w1);
static void uc0_culldl(uint32_t w0, uint32_t w1);
static void uc0_popmatrix(uint32_t w0, uint32_t w1);
static void uc0_moveword(uint32_t w0, uint32_t w1);
static void uc0_texture(uint32_t w0, uint32_t w1);
static void uc0_setothermode_h(uint32_t w0, uint32_t w1);
static void uc0_setothermode_l(uint32_t w0, uint32_t w1);
static void uc0_setgeometrymode(uint32_t w0, uint32_t w1);
static void uc0_cleargeometrymode(uint32_t w0, uint32_t w1);
static void uc0_line3d(uint32_t w0, uint32_t w1);
static void uc0_tri4(uint32_t w0, uint32_t w1);

//ucode01
static void uc1_vertex(uint32_t w0, uint32_t w1);
static void uc1_tri1(uint32_t w0, uint32_t w1);
static void uc1_tri2(uint32_t w0, uint32_t w1);
static void uc1_line3d(uint32_t w0, uint32_t w1);
static void uc1_rdphalf_1(uint32_t w0, uint32_t w1);
static void uc1_branch_z(uint32_t w0, uint32_t w1);

static void uc6_select_dl(uint32_t w0, uint32_t w1);
static void uc6_obj_rendermode(uint32_t w0, uint32_t w1);
static void uc6_bg_1cyc(uint32_t w0, uint32_t w1);
static void uc6_bg_copy(uint32_t w0, uint32_t w1);
static void uc6_loaducode(uint32_t w0, uint32_t w1);
static void uc6_sprite2d(uint32_t w0, uint32_t w1);
static void uc6_obj_loadtxtr(uint32_t w0, uint32_t w1);
static void uc6_obj_rectangle(uint32_t w0, uint32_t w1);
static void uc6_obj_ldtx_sprite(uint32_t w0, uint32_t w1);
static void uc6_obj_ldtx_rect(uint32_t w0, uint32_t w1);
static void uc6_ldtx_rect_r(uint32_t w0, uint32_t w1);
static void uc6_obj_rectangle_r(uint32_t w0, uint32_t w1);
static void uc6_obj_movemem(uint32_t w0, uint32_t w1);

//ucode02
static void calc_point_light (VERTEX *v, float * vpos);
static void uc2_quad(uint32_t w0, uint32_t w1);
static void uc2_vertex_neon(uint32_t w0, uint32_t w1);
static void uc2_vertex(uint32_t w0, uint32_t w1);
static void uc2_modifyvtx(uint32_t w0, uint32_t w1);
static void uc2_culldl(uint32_t w0, uint32_t w1);
static void uc2_tri1(uint32_t w0, uint32_t w1);
static void uc2_line3d(uint32_t w0, uint32_t w1);
static void uc2_special3(uint32_t w0, uint32_t w1);
static void uc2_special2(uint32_t w0, uint32_t w1);
static void uc2_dma_io(uint32_t w0, uint32_t w1);
static void uc2_pop_matrix(uint32_t w0, uint32_t w1);
static void uc2_geom_mode(uint32_t w0, uint32_t w1);
static void uc2_matrix(uint32_t w0, uint32_t w1);
static void uc2_moveword(uint32_t w0, uint32_t w1);
static void uc2_movemem(uint32_t w0, uint32_t w1);
static void uc2_load_ucode(uint32_t w0, uint32_t w1);
static void uc2_rdphalf_2(uint32_t w0, uint32_t w1);
static void uc2_dlist_cnt(uint32_t w0, uint32_t w1);

//ucode03
static void uc3_vertex(uint32_t w0, uint32_t w1);
static void uc3_tri1(uint32_t w0, uint32_t w1);
static void uc3_tri2(uint32_t w0, uint32_t w1);
static void uc3_quad3d(uint32_t w0, uint32_t w1);

//ucode04
static void uc4_vertex(uint32_t w0, uint32_t w1);
static void uc4_tri1(uint32_t w0, uint32_t w1);
static void uc4_quad3d(uint32_t w0, uint32_t w1);

//ucode05
static void uc5_dma_offsets(uint32_t w0, uint32_t w1);
static void uc5_matrix(uint32_t w0, uint32_t w1);
static void uc5_vertex(uint32_t w0, uint32_t w1);
static void uc5_tridma(uint32_t w0, uint32_t w1);
static void uc5_dl_in_mem(uint32_t w0, uint32_t w1);
static void uc5_moveword(uint32_t w0, uint32_t w1);
static void uc5_setgeometrymode(uint32_t w0, uint32_t w1);
static void uc5_cleargeometrymode(uint32_t w0, uint32_t w1);

//ucode07
static void uc7_colorbase(uint32_t w0, uint32_t w1);
static void uc7_vertex(uint32_t w0, uint32_t w1);

//ucode08
static void uc8_vertex(uint32_t w0, uint32_t w1);
static void uc8_moveword(uint32_t w0, uint32_t w1);
static void uc8_movemem(uint32_t w0, uint32_t w1);
static void uc8_tri4(uint32_t w0, uint32_t w1);

//ucode09
static void uc9_rpdcmd(uint32_t w0, uint32_t w1);
static void uc9_draw_object (uint8_t * addr, uint32_t type);
static uint32_t uc9_load_object (uint32_t zHeader, uint32_t * rdpcmds);
static void uc9_object(uint32_t w0, uint32_t w1);
static void uc9_mix(uint32_t w0, uint32_t w1);
static void uc9_fmlight(uint32_t w0, uint32_t w1);
static void uc9_light(uint32_t w0, uint32_t w1);
static void uc9_mtxtrnsp(uint32_t w0, uint32_t w1);
static void uc9_mtxcat(uint32_t w0, uint32_t w1);
static void uc9_mult_mpmtx(uint32_t w0, uint32_t w1);
static void uc9_link_subdl(uint32_t w0, uint32_t w1);
static void uc9_set_subdl(uint32_t w0, uint32_t w1);
static void uc9_wait_signal(uint32_t w0, uint32_t w1);
static void uc9_send_signal(uint32_t w0, uint32_t w1);
static void uc9_movemem(uint32_t w0, uint32_t w1);
static void uc9_setscissor(uint32_t w0, uint32_t w1);

typedef void (*rdp_instr)(uint32_t w1, uint32_t w2);

// RDP graphic instructions pointer table

static rdp_instr gfx_instruction[10][256] =
{
   {
      // uCode 0 - RSP SW 2.0X
      // 00-3f
      // games: Super Mario 64, Tetrisphere, Demos
      gdp_no_op,                     uc0_matrix,             rsp_reserved0,              uc0_movemem,
      uc0_vertex,             rsp_reserved1,              uc0_displaylist,        rsp_reserved2,
      rsp_reserved3,              uc6_sprite2d,           gdp_invalid,                      gdp_invalid,
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 40-7f: Unused
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 80-bf: Immediate commands
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      uc0_tri4,           rdphalf_cont,           rdphalf_2,
      rdphalf_1,              uc0_line3d,             uc0_cleargeometrymode,  uc0_setgeometrymode,
      uc0_enddl,              uc0_setothermode_l,     uc0_setothermode_h,     uc0_texture,
      uc0_moveword,           uc0_popmatrix,          uc0_culldl,             uc0_tri1,
      // c0-ff: RDP commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_texrect,                    rdp_texrect,                gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,         gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,
      rdp_loadtlut,           gdp_invalid,                  rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },

   // uCode 1 - F3DEX 1.XX
   // 00-3f
   // games: Mario Kart, Star Fox
   {
      gdp_no_op,                     uc0_matrix,             rsp_reserved0,              uc0_movemem,
      uc1_vertex,             rsp_reserved1,              uc0_displaylist,        rsp_reserved2,
      rsp_reserved3,              uc6_sprite2d,           gdp_invalid,                      gdp_invalid,
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 40-7f: unused
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 80-bf: Immediate commands
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      uc6_loaducode,        
      uc1_branch_z,           uc1_tri2,               uc2_modifyvtx,             rdphalf_2,
      uc1_rdphalf_1,          uc1_line3d,             uc0_cleargeometrymode,  uc0_setgeometrymode,
      uc0_enddl,              uc0_setothermode_l,     uc0_setothermode_h,     uc0_texture,
      uc0_moveword,           uc0_popmatrix,          uc2_culldl,             uc1_tri1,
      // c0-ff: RDP commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_texrect,            rdp_texrect,            gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,         gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,
      rdp_loadtlut,           gdp_invalid,                  rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },

   // uCode 2 - F3DEX 2.XX
   // games: Zelda 64
   {
      // 00-3f
      gdp_no_op,                                 uc2_vertex,                             uc2_modifyvtx,                  uc2_culldl,
      uc1_branch_z,                   uc2_tri1,                               uc2_quad,                           uc2_quad,
      uc2_line3d,                             uc6_bg_1cyc,                    uc6_bg_copy,                    uc6_obj_rendermode/*gdp_invalid*/,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      uc0_tri4,                               uc0_tri4,                               uc0_tri4,                               uc0_tri4,
      uc0_tri4,                               uc0_tri4,                               uc0_tri4,                               uc0_tri4,
      uc0_tri4,                               uc0_tri4,                               uc0_tri4,                               uc0_tri4,
      uc0_tri4,                               uc0_tri4,                               uc0_tri4,                               uc0_tri4,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      // 40-7f: unused
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      // 80-bf: unused
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      // c0-ff: RDP commands mixed with uc2 commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  uc2_special3,
      uc2_special2,           uc2_dlist_cnt,          uc2_dma_io,             uc0_texture,
      uc2_pop_matrix,         uc2_geom_mode,          uc2_matrix,             uc2_moveword,
      uc2_movemem,            uc2_load_ucode,         uc0_displaylist,        uc0_enddl,
      gdp_no_op,                 uc1_rdphalf_1,          uc0_setothermode_l,     uc0_setothermode_h,
      rdp_texrect,            rdp_texrect,            gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,         gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,
      rdp_loadtlut,           uc2_rdphalf_2,          rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },

   // uCode 3 - "RSP SW 2.0D", but not really
   // 00-3f
   // games: Wave Race
   // ** Added by Gonetz **
   {
      gdp_no_op,                                 uc0_matrix,             rsp_reserved0,              uc0_movemem,
      uc3_vertex,                             rsp_reserved1,              uc0_displaylist,        rsp_reserved2,
      rsp_reserved3,              uc6_sprite2d,           gdp_invalid,                      gdp_invalid,
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 40-7f: unused
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 80-bf: Immediate commands
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                  uc3_tri2,               rdphalf_cont,       rdphalf_2,
      rdphalf_1,          uc3_quad3d,             uc0_cleargeometrymode,  uc0_setgeometrymode,
      uc0_enddl,              uc0_setothermode_l,     uc0_setothermode_h,     uc0_texture,
      uc0_moveword,           uc0_popmatrix,          uc0_culldl,             uc3_tri1,
      // c0-ff: RDP commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_texrect,            rdp_texrect,            gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,         gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,
      rdp_loadtlut,           gdp_invalid,                  rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },

   {
      // uCode 4 - RSP SW 2.0D EXT
      // 00-3f
      // games: Star Wars: Shadows of the Empire
      gdp_no_op,                     uc0_matrix,             rsp_reserved0,              uc0_movemem,
      uc4_vertex,             rsp_reserved1,              uc0_displaylist,        rsp_reserved2,
      rsp_reserved3,              uc6_sprite2d,           gdp_invalid,                      gdp_invalid,
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 40-7f: Unused
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 80-bf: Immediate commands
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      uc0_tri4,                       rdphalf_cont,       rdphalf_2,
      rdphalf_1,          uc4_quad3d,             uc0_cleargeometrymode,  uc0_setgeometrymode,
      uc0_enddl,              uc0_setothermode_l,     uc0_setothermode_h,     uc0_texture,
      uc0_moveword,           uc0_popmatrix,          uc0_culldl,             uc4_tri1,
      // c0-ff: RDP commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_texrect,                    rdp_texrect,            gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,         gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,
      rdp_loadtlut,           gdp_invalid,                  rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },

   {
      // uCode 5 - RSP SW 2.0 Diddy
      // 00-3f
      // games: Diddy Kong Racing
      gdp_no_op,                     uc5_matrix,             rsp_uc5_reserved0,              uc0_movemem,
      uc5_vertex,                                     uc5_tridma,                            uc0_displaylist,                  uc5_dl_in_mem,
      rsp_reserved3,              uc6_sprite2d,           gdp_invalid,                      gdp_invalid,
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 40-7f: Unused
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 80-bf: Immediate commands
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      uc0_tri4,                   rdphalf_cont,               rdphalf_2,
      rdphalf_1,              uc0_line3d,             uc5_cleargeometrymode,  uc5_setgeometrymode,
      uc0_enddl,              uc0_setothermode_l,     uc0_setothermode_h,     uc0_texture,
      uc5_moveword,           uc0_popmatrix,          uc0_culldl,             uc5_dma_offsets,
      // c0-ff: RDP commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_texrect,                    rdp_texrect,            gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,           gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,
      rdp_loadtlut,           gdp_invalid,                  rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },

   // uCode 6 - S2DEX 1.XX
   // games: Yoshi's Story
   {
      gdp_no_op,                     uc6_bg_1cyc,             uc6_bg_copy,              uc6_obj_rectangle,
      uc6_obj_sprite,             uc6_obj_movemem,         uc0_displaylist,        rsp_reserved2,
      rsp_reserved3,              gdp_invalid/*uc6_sprite2d*/,           gdp_invalid,                      gdp_invalid,
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 40-7f: unused
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 80-bf: Immediate commands
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      uc6_loaducode,        
      uc6_select_dl,              uc6_obj_rendermode,         uc6_obj_rectangle_r,            rdphalf_2,
      rdphalf_1,          uc1_line3d,             uc0_cleargeometrymode,  uc0_setgeometrymode,
      uc0_enddl,              uc0_setothermode_l,     uc0_setothermode_h,     uc0_texture,
      uc0_moveword,           uc0_popmatrix,          uc2_culldl,             uc1_tri1,
      // c0-ff: RDP commands
      gdp_no_op,               uc6_obj_loadtxtr,       uc6_obj_ldtx_sprite,    uc6_obj_ldtx_rect,
      uc6_ldtx_rect_r,        gdp_invalid,                  gdp_invalid,                  gdp_invalid,
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_texrect,            rdp_texrect,        gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,           gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,
      rdp_loadtlut,           gdp_invalid,                  rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },
   // uCode 7 - unknown
   // games: Perfect Dark
   {
      // 00-3f
      gdp_no_op,                                 uc0_matrix,                             rsp_reserved0,                  uc0_movemem,
      uc7_vertex,                             rsp_reserved1,                  uc0_displaylist,                uc7_colorbase,
      rsp_reserved3,                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      // 40-7f: unused
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      // 80-bf: unused
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      gdp_invalid,                                  uc0_tri4,                               rdphalf_cont,           rdphalf_2,
      rdphalf_1,                      uc1_tri2,                               uc0_cleargeometrymode,  uc0_setgeometrymode,
      uc0_enddl,                              uc0_setothermode_l,             uc0_setothermode_h,             uc0_texture,
      uc0_moveword,                   uc0_popmatrix,                  uc0_culldl,                             uc0_tri1,

      // c0-ff: RDP commands mixed with uc2 commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,

      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      rdp_texrect,            rdp_texrect,            gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,           gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,

      rdp_loadtlut,           rdphalf_2,          rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },

   // uCode 8 - unknown
   // games: Conker's Bad Fur Day
   {
      // 00-3f
      gdp_no_op,                                 uc8_vertex,                             uc2_modifyvtx,                  uc2_culldl,
      uc1_branch_z,                   uc2_tri1,                               uc2_quad,                               uc2_quad,
      uc2_line3d,                             uc6_bg_1cyc,                    uc6_bg_copy,                    uc6_obj_rendermode/*gdp_invalid*/,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      uc8_tri4,                               uc8_tri4,                               uc8_tri4,                               uc8_tri4,
      uc8_tri4,                               uc8_tri4,                               uc8_tri4,                               uc8_tri4,
      uc8_tri4,                               uc8_tri4,                               uc8_tri4,                               uc8_tri4,
      uc8_tri4,                               uc8_tri4,                               uc8_tri4,                               uc8_tri4,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      // 40-7f: unused
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      // 80-bf: unused
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,

      // c0-ff: RDP commands mixed with uc2 commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,
      gdp_invalid,                                  gdp_invalid,                                  gdp_invalid,                                  uc2_special3,
      uc2_special2,                   uc2_dlist_cnt,                  uc2_dma_io,                             uc0_texture,
      uc2_pop_matrix,                 uc2_geom_mode,                  uc2_matrix,                             uc8_moveword,
      uc8_movemem,                    uc2_load_ucode,                 uc0_displaylist,                uc0_enddl,
      gdp_no_op,                                 rdphalf_1,                      uc0_setothermode_l,             uc0_setothermode_h,
      rdp_texrect,            rdp_texrect,            gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,           gdp_set_key_r,
      gdp_set_convert,         rdp_setscissor,         gdp_set_prim_depth,       rdp_setothermode,
      rdp_loadtlut,           uc2_rdphalf_2,          rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },

   {
      // uCode 9 - gzsort
      // games: Telefoot Soccer
      // 00-3f
      gdp_no_op,                     gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 40-7f: Unused
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      // 80-bf: Immediate commands
      uc9_object,                 uc9_rpdcmd,                 gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      gdp_invalid,                      gdp_invalid,                      gdp_invalid,                      gdp_invalid,        
      rdphalf_1,                  gdp_invalid,                      uc0_cleargeometrymode,      uc0_setgeometrymode,
      uc0_enddl,                  uc0_setothermode_l,         uc0_setothermode_h,         uc0_texture,
      uc0_moveword,               gdp_invalid,                      uc0_culldl,                 gdp_invalid,
      // c0-ff: RDP commands
      gdp_no_op,               gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      gdp_invalid,                  gdp_invalid,                  gdp_invalid,                  gdp_invalid,    
      rdp_trifill,            rdp_trifillz,           rdp_tritxtr,            rdp_tritxtrz,
      rdp_trishade,           rdp_trishadez,          rdp_trishadetxtr,       rdp_trishadetxtrz,

      uc9_mix,                uc9_fmlight,            uc9_light,              gdp_invalid,    
      uc9_mtxtrnsp,           uc9_mtxcat,             uc9_mult_mpmtx,         uc9_link_subdl,    
      uc9_set_subdl,          uc9_wait_signal,        uc9_send_signal,        uc0_moveword,    
      uc9_movemem,            gdp_invalid,                  uc0_displaylist,        uc0_enddl,    

      gdp_invalid,                  gdp_invalid,                  uc0_setothermode_l,     uc0_setothermode_h,    
      rdp_texrect,            rdp_texrect,            gdp_load_sync,           gdp_pipe_sync,
      gdp_tile_sync,           gdp_full_sync,           gdp_set_key_gb,           gdp_set_key_r,
      gdp_set_convert,         uc9_setscissor,         gdp_set_prim_depth,       rdp_setothermode,

      rdp_loadtlut,           rdphalf_2,              rdp_settilesize,        rdp_loadblock,
      rdp_loadtile,           rdp_settile,            rdp_fillrect,           gdp_set_fill_color,
      gdp_set_fog_color,        gdp_set_blend_color,      rdp_setprimcolor,       gdp_set_env_color,
      rdp_setcombine,         rdp_settextureimage,    rdp_setdepthimage,      rdp_setcolorimage
   },
};


#endif
