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

#ifndef RDP_H
#define RDP_H

#include "rdp_common/gdp.h"
#include "Gfx_1.3.h"

extern uint32_t frame_count; // frame counter
extern uint32_t gfx_plugin_accuracy;

#define MAX_CACHE   1024*4
#define MAX_TRI_CACHE 768 // this is actually # of vertices, not triangles
#define MAX_VTX     256

#define MAX_TMU     2

// Supported flags
#define SUP_TEXMIRROR 0x00000001

// Clipping flags
#define CLIP_XMAX 0x00000001
#define CLIP_XMIN 0x00000002
#define CLIP_YMAX 0x00000004
#define CLIP_YMIN 0x00000008
#define CLIP_WMIN 0x00000010
#define CLIP_ZMAX 0x00000020
#define CLIP_ZMIN 0x00000040

// Flags
#define RDP_ALPHA_COMPARE     0x00000003
#define RDP_Z_SOURCE_SEL      0x00000004
#define RDP_Z_UPDATE_ENABLE   0x00000020
#define RDP_CYCLE_TYPE        0x00300000
#define RDP_TEX_LOD_ENABLE    0x00010000
#define RDP_PERSP_TEX_ENABLE  0x00080000
#define RDP_FORCE_BLEND       0x00004000
#define RDP_COLOR_ON_CVG      0x00000080
#define RDP_ALPHA_CVG_SELECT  0x00002000
#define ZBUF_ENABLED  0x00000001
#define ZBUF_DECAL    0x00000002
#define ZBUF_COMPARE  0x00000004
#define ZBUF_UPDATE   0x00000008
#define ALPHA_COMPARE 0x00000010
#define CULL_FRONT    0x00001000  // * must be here
#define CULL_BACK     0x00002000  // * must be here
#define FOG_ENABLED   0x00010000

#define CULLMASK    0x00003000
#define CULLSHIFT   12

#define CMB_MULT    0x00000001
#define CMB_SET     0x00000002
#define CMB_SUB     0x00000004
#define CMB_ADD     0x00000008
#define CMB_A_MULT  0x00000010
#define CMB_A_SET   0x00000020
#define CMB_A_SUB   0x00000040
#define CMB_A_ADD   0x00000080
#define CMB_SETSHADE_SHADEALPHA 0x00000100
#define CMB_INTER   0x00000200
#define CMB_MULT_OWN_ALPHA  0x00000400
#define CMB_COL_SUB_OWN  0x00000800

#if defined _MSC_VER
#define DECLAREALIGN16VAR(var) __declspec(align(16)) float (var)
#else
#define DECLAREALIGN16VAR(var) float (var) __attribute__ ((aligned(16)))
#endif

//Frame buffer emulation options
#define  fb_emulation            (1<<0)   //frame buffer emulation
#define  fb_hwfbe                (1<<1)   //hardware frame buffer emualtion
#define  fb_motionblur           (1<<2)   //emulate motion blur
#define  fb_ref                  (1<<3)   //read every frame
#define  fb_read_alpha           (1<<4)   //read alpha
#define  fb_hwfbe_buf_clear      (1<<5)   //clear auxiliary texture frame buffers
#define  fb_depth_render         (1<<6)   //enable software depth render
#define  fb_optimize_texrect     (1<<7)   //fast texrect rendering with hwfbe
#define  fb_ignore_aux_copy      (1<<8)   //do not copy auxiliary frame buffers
#define  fb_useless_is_useless   (1<<10)  //
#define  fb_get_info             (1<<11)  //get frame buffer info
#define  fb_read_back_to_screen  (1<<12)  //render N64 frame buffer to screen
#define  fb_read_back_to_screen2 (1<<13)  //render N64 frame buffer to screen
#define  fb_cpu_write_hack       (1<<14)  //show images writed directly by CPU

#define fb_emulation_enabled ((settings.frame_buffer&fb_emulation)>0)
#define fb_hwfbe_enabled ((settings.frame_buffer&(fb_emulation|fb_hwfbe))==(fb_emulation|fb_hwfbe))
#define fb_depth_render_enabled ((settings.frame_buffer&fb_depth_render)>0)

//Special game hacks
#define  hack_ASB         (1<<0)   //All-Star Baseball games
#define  hack_Banjo2      (1<<1)   //Banjo Tooie
#define  hack_BAR         (1<<2)   //Beetle Adventure Racing
#define  hack_Chopper     (1<<3)   //Chopper Attack
#define  hack_Diddy       (1<<4)   //diddy kong racing
#define  hack_Fifa98      (1<<5)   //FIFA - Road to World Cup 98
#define  hack_Fzero       (1<<6)   //F-Zero
#define  hack_GoldenEye   (1<<7)   //Golden Eye
#define  hack_Hyperbike   (1<<8)   //Top Gear Hyper Bike
#define  hack_ISS64       (1<<9)   //International Superstar Soccer 64
#define  hack_KI          (1<<10)  //Killer Instinct
#define  hack_Knockout    (1<<11)  //Knockout Kings 2000
#define  hack_Lego        (1<<12)  //LEGO Racers
#define  hack_MK64        (1<<13)  //Mario Kart
#define  hack_Megaman     (1<<14)  //Megaman64
#define  hack_Makers      (1<<15)  //Mischief-makers
#define  hack_WCWnitro    (1<<16)  //WCW Nitro
#define  hack_Ogre64      (1<<17)  //Ogre Battle 64
#define  hack_Pilotwings  (1<<18)  //Pilotwings
#define  hack_PMario      (1<<19)  //Paper Mario
#define  hack_PPL         (1<<20)  //pokemon puzzle league requires many special fixes
#define  hack_RE2         (1<<21)  //Resident Evil 2
#define  hack_Starcraft   (1<<22)  //StarCraft64
#define  hack_Supercross  (1<<23)  //Supercross 2000
#define  hack_TGR         (1<<24)  //Top Gear Rally
#define  hack_TGR2        (1<<25)  //Top Gear Rally 2
#define  hack_Tonic       (1<<26)  //tonic trouble
#define  hack_Yoshi       (1<<27)  //Yoshi Story
#define  hack_Zelda       (1<<28)  //zeldas hacks
#define  hack_Blastcorps  (1<<29)  /* Blast Corps */
#define  hack_OOT         (1<<30)  /* Zelda OOT hacks */

#define FBCRCMODE_NONE  0
#define FBCRCMODE_FAST  1
#define FBCRCMODE_SAFE  2

// Clipping (scissors)
typedef struct
{
   uint32_t ul_x;
   uint32_t ul_y;
   uint32_t lr_x;
   uint32_t lr_y;
} SCISSOR;

typedef struct
{
   uint32_t depth_bias;

   uint32_t res_x, scr_res_x;
   uint32_t res_y, scr_res_y;

   int vsync;

   int filtering;
   int fog;
   int buff_clear;
   int swapmode;
   bool swapmode_retro;
   int lodmode;
   int aspectmode;

   uint32_t frame_buffer;
   unsigned fb_crc_mode;

   //Debug
   int autodetect_ucode;
   int ucode;
   int unk_as_red;
   int unk_clear;

   // Special fixes
   int offset_x, offset_y;
   int scale_x, scale_y;
   int fast_crc;
   int alt_tex_size;
   int flame_corona; //hack for zeldas flame's corona
   int increase_texrect_edge; // add 1 to lower right corner coordinates of texrect
   int decrease_fillrect_edge; // sub 1 from lower right corner coordinates of fillrect
   int stipple_mode;  //used for dithered alpha emulation
   uint32_t stipple_pattern; //used for dithered alpha emulation
   int force_microcheck; //check microcode each frame, for mixed F3DEX-S2DEX games
   int force_quad3d; //force 0xb5 command to be quad, not line 3d
   int clip_zmin; //enable near z clipping
   int adjust_aspect; //adjust screen aspect for wide screen mode
   int force_calc_sphere; //use spheric mapping only, Ridge Racer 64
   int pal230;    //set special scale for PAL games
   int correct_viewport; //correct viewport values
   int zmode_compare_less; //force GR_CMP_LESS for zmode=0 (opaque)and zmode=1 (interpenetrating)
   int old_style_adither; //apply alpha dither regardless of alpha_dither_mode
   int n64_z_scale; //scale vertex z value before writing to depth buffer, as N64 does.

   uint32_t hacks;
} SETTINGS;

typedef struct
{
   uint8_t hk_ref;
   uint8_t hk_motionblur;
   uint8_t hk_filtering;
} HOTKEY_INFO;

typedef struct
{
   uint32_t tmem_ptr[MAX_TMU];
   uint32_t tex_max_addr;
} VOODOO;

// This structure is what is passed in by rdp:settextureimage
typedef struct
{
   int set_by;          // 0-loadblock 1-loadtile
} TEXTURE_IMAGE;

// This structure is a tile descriptor (as used by rdp:settile and rdp:settilesize)
typedef struct
{
   // rdp:settilesize
   // TODO - eventually remove these
   float f_ul_s;
   float f_ul_t;

   uint32_t width;
   uint32_t height;

   // uc0:texture
   uint8_t on;
   float s_scale;
   float t_scale;

   uint16_t org_s_scale;
   uint16_t org_t_scale;
} TILE;

// This structure forms the lookup table for cached textures
typedef struct {
  uint32_t addr;        // address in RDRAM
  uint32_t crc;         // CRC check
  uint32_t palette;     // Palette #
  uint32_t width;       // width
  uint32_t height;      // height
  uint32_t format;      // format
  uint32_t size;        // size
  uint32_t last_used;   // what frame # was this texture last used (used for replacing)

  uint32_t line;

  uint32_t flags;       // clamp/wrap/mirror flags

  uint32_t realwidth;   // width of actual texture
  uint32_t realheight;  // height of actual texture
  uint32_t lod;
  uint32_t aspect;

  uint8_t set_by;
  uint8_t texrecting;

  float scale_x;        // texture scaling
  float scale_y;
  float scale;          // general scale to 256

  GrTexInfo t_info;     // texture info (glide)
  uint32_t tmem_addr;   // addres in texture memory (glide)

  int uses;             // 1 triangle that uses this texture

  int splitheight;

  float c_off;          // ul center texel offset (both x and y)
  float c_scl_x;        // scale to lower-right center-texel x
  float c_scl_y;        // scale to lower-right center-texel y

  uint32_t mod, mod_color, mod_color1, mod_color2, mod_factor;
} CACHE_LUT;

typedef struct
{
   float col[4];                 // diffuse light value color (RGBA)
   float dir[3];                 // direction towards light source (normalized
   float x, y, z, w;             // light position
   float ca, la, qa;
   uint32_t nonblack;
   uint32_t nonzero;
} LIGHT;

typedef struct {
    int clampdiffs, clampdifft;
    int clampens, clampent;
    int masksclamped, masktclamped;
    int notlutswitch, tlutswitch;
} FAKETILE;

typedef enum
{
   CI_MAIN,      //0, main color image
   CI_ZIMG,      //1, depth image
   CI_UNKNOWN,   //2, status is unknown
   CI_USELESS,   //3, status is unclear
   CI_OLD_COPY,  //4, auxiliary color image, copy of last color image from previous frame
   CI_COPY,      //5, auxiliary color image, copy of previous color image
   CI_COPY_SELF, //6, main color image, it's content will be used to draw into itself
   CI_ZCOPY,     //7, auxiliary color image, copy of depth image
   CI_AUX,       //8, auxiliary color image
   CI_AUX_COPY   //9, auxiliary color image, partial copy of previous color image
} CI_STATUS;

// Frame buffers
typedef struct
{
   uint32_t addr;   //color image address
   uint8_t format;
   uint8_t size;
   uint16_t width;
   uint16_t height;
   CI_STATUS status;
   int   changed;
} COLOR_IMAGE;

typedef struct
{
   int32_t tmu;
   uint32_t addr;          //address of color image
   uint32_t end_addr;
   uint32_t tex_addr;      //address in video memory
   uint32_t width;         //width of color image
   uint32_t height;        //height of color image
   uint8_t  format;        //format of color image
   uint8_t  size;          //format of color image
   uint8_t  clear;         //flag. texture buffer must be cleared
   uint8_t  drawn;         //flag. if equal to 1, this image was already drawn in current frame
   uint32_t crc;           //checksum of the color image
   float scr_width;        //width of rendered image
   float scr_height;       //height of rendered image
   uint32_t tex_width;     //width of texture buffer
   uint32_t tex_height;    //height of texture buffer
   int   tile;     
   uint16_t  tile_uls;     //shift from left bound of the texture
   uint16_t  tile_ult;     //shift from top of the texture
   uint32_t v_shift;       //shift from top of the texture
   uint32_t u_shift;       //shift from left of the texture
   float lr_u;
   float lr_v;
   float u_scale;          //used to map vertex u,v coordinates into hires texture
   float v_scale;          //used to map vertex u,v coordinates into hires texture
   CACHE_LUT * cache;      //pointer to texture cache item
   GrTexInfo info;
   uint16_t t_mem;
} TBUFF_COLOR_IMAGE;

typedef struct
{
   int32_t tmu;
   uint32_t begin;         //start of the block in video memory
   uint32_t end;           //end of the block in video memory
   uint8_t count;          //number of allocated texture buffers
   int clear_allowed;      //stack of buffers can be cleared
   TBUFF_COLOR_IMAGE images[256];
} TEXTURE_BUFFER;

#define NUMTEXBUF 92

#define FOG_MODE_DISABLED       0
#define FOG_MODE_ENABLED        1
#define FOG_MODE_BLEND          2
#define FOG_MODE_BLEND_INVERSE  3

#define NOISE_MODE_NONE         0
#define NOISE_MODE_COMBINE      1
#define NOISE_MODE_TEXTURE      2

struct RDP
{
   uint32_t u_cull_mode;
   float vi_width;
   float vi_height;

   float offset_x, offset_y, offset_x_bak, offset_y_bak;

   float scale_x, scale_1024, scale_x_bak;
   float scale_y, scale_768, scale_y_bak;

   float view_scale[3];
   float view_trans[3];
   float clip_min_x, clip_max_x, clip_min_y, clip_max_y;
   float clip_ratio;

   int updatescreen;

   // Program counter
   uint32_t pc[10]; // Display List PC stack
   uint32_t pc_i;   // current PC index in the stack
   int dl_count; // number of instructions before returning
   int LLE;

   // Segments
   uint32_t segment[16];  // Segment pointer

   // Marks the end of DList execution (done in uc?:enddl)
   int halt;

   // Next command
   uint32_t cmd0;
   uint32_t cmd1;
   uint32_t cmd2;
   uint32_t cmd3;

   // Clipping
   SCISSOR scissor;
   int scissor_set;

   // Colors
   unsigned noise;

   float col[4];   // color multiplier
   float coladd[4];  // color add/subtract
   float shade_factor;

   float col_2[4];

   uint32_t cmb_flags, cmb_flags_2;

   // Matrices
   DECLAREALIGN16VAR(model[4][4]);
   DECLAREALIGN16VAR(proj[4][4]);
   DECLAREALIGN16VAR(combined[4][4]);
   DECLAREALIGN16VAR(dkrproj[3][4][4]);

   DECLAREALIGN16VAR(model_stack[32][4][4]);  // 32 deep, will warn if overflow
   int model_i;          // index in the model matrix stack
   int model_stack_size;

   // Textures
   TEXTURE_IMAGE timg;       // 1 for each tmem address
   TILE tiles[8];          // 8 tile descriptors
   uint32_t addr[512];        // 512 addresses (used to determine address loaded from)

   int     cur_tile;   // current tile
   int     mipmap_level;
   int     last_tile;   // last tile set
   int     last_tile_size;   // last tile size set

   int     t0, t1;
   int     tex;
   int     filter_mode;

   // Texture palette
   uint16_t pal_8[256];
   uint32_t pal_8_crc[16];
   uint32_t pal_256_crc;
   uint8_t tlut_mode;
   int force_wrap;

   // Lighting
   uint32_t num_lights;
   LIGHT light[12];
   float light_vector[12][3];
   float lookat[2][3];
   int  use_lookat;

   // Combine modes
   uint32_t cycle1, cycle2;

   uint8_t fbl_a0, fbl_b0, fbl_c0, fbl_d0;
   uint8_t fbl_a1, fbl_b1, fbl_c1, fbl_d1;

   //  float YUV_C0, YUV_C1, YUV_C2, YUV_C3, YUV_C4; //YUV textures conversion coefficients

   // What needs updating
   uint32_t flags;

   uint32_t tex_ctr;    // incremented every time textures are updated

   int allow_combine; // allow combine updating?

   int s2dex_tex_loaded;
   uint16_t bg_image_height;

   // Debug stuff
   uint32_t rm; // use othermode_l instead, this just as a check for changes
   uint32_t render_mode_changed;
   uint32_t geom_mode;

   uint32_t othermode_h;
   uint32_t othermode_l;

   // used to check if in texrect while loading texture
   uint8_t texrecting;

   //frame buffer related slots. Added by Gonetz
   uint32_t cimg, ocimg, tmpzimg, vi_org_reg;
   COLOR_IMAGE maincimg[2];
   uint32_t last_drawn_ci_addr;
   uint32_t main_ci, main_ci_end, main_ci_bg, main_ci_last_tex_addr, zimg_end, last_bg;
   uint32_t ci_width, ci_height, ci_end;
   uint32_t zi_width;
   int zi_lrx, zi_lry;
   uint8_t  ci_count, num_of_ci, main_ci_index, copy_ci_index, copy_zi_index;
   int swap_ci_index, black_ci_index;
   uint32_t ci_upper_bound, ci_lower_bound;
   int  motionblur, fb_drawn, fb_drawn_front, read_previous_ci, read_whole_frame;
   CI_STATUS ci_status;
   int skip_drawing; //rendering is not required. used for frame buffer emulation

   //fog related slots. Added by Gonetz
   float fog_multiplier, fog_offset;
   unsigned fog_mode;
   // Clipping
   int clip;     // clipping flags
   VERTEX *vtx1; //[256] copy vertex buffer #1 (used for clipping)
   VERTEX *vtx2; //[256] copy vertex buffer #2
   VERTEX *vtxbuf;   // current vertex buffer (reset to vtx, used to determine current vertex buffer)
   VERTEX *vtxbuf2;
   int n_global;   // Used to pass the number of vertices from clip_z to clip_tri
   int vtx_buffer;

   CACHE_LUT *cache[MAX_TMU]; //[MAX_CACHE]
   CACHE_LUT *cur_cache[MAX_TMU];
   int     n_cached[MAX_TMU];

   // Vertices
   VERTEX *vtx; //[MAX_VTX]

   COLOR_IMAGE *frame_buffers; //[NUMTEXBUF+2]

   char RomName[21];
};

void ChangeSize(void);

extern struct RDP rdp;
extern SETTINGS settings;
extern VOODOO voodoo;

extern uint32_t   offset_textures;
extern uint32_t   offset_texbuf1;

extern int	ucode_error_report;

// RDP functions
void rdp_free(void);
void rdp_new(void);
void rdp_reset(void);

extern const char *ACmp[];
extern const char *Mode0[];
extern const char *Mode1[];
extern const char *Mode2[];
extern const char *Mode3[];
extern const char *Alpha0[];
#define Alpha1 Alpha0
extern const char *Alpha2[];
#define Alpha3 Alpha0
extern const char *FBLa[];
extern const char *FBLb[];
extern const char *FBLc[];
extern const char *FBLd[];
extern const char *str_zs[];
extern const char *str_yn[];
extern const char *str_offon[];
extern const char *str_cull[];
// I=intensity probably
extern const char *str_format[];
extern const char *str_size[];
extern const char *str_cm[];
extern const char *str_lod[];
extern const char *str_aspect[];
extern const char *str_filter[];
extern const char *str_tlut[];
extern const char *CIStatus[];

#define FBL_D_1 2
#define FBL_D_0 3

#ifndef HIWORD
#define HIWORD(a) ((uint32_t)(a) >> 16)
#endif

#ifndef LOWORD
#define LOWORD(a) ((a) & 0xFFFF)
#endif

// Convert from u0/v0/u1/v1 to the real coordinates without regard to tmu
static INLINE void ConvertCoordsKeep (VERTEX *v, int n)
{
   int i;
   for (i = 0; i < n; i++)
   {
      v[i].coord[0] = v[i].u[0];
      v[i].coord[1] = v[i].v[0];
      v[i].coord[2] = v[i].u[1];
      v[i].coord[3] = v[i].v[1];
   }
}

// Convert from u0/v0/u1/v1 to the real coordinates based on the tmu they are on
static INLINE void ConvertCoordsConvert (VERTEX *v, int n)
{
   int i;
   for (i = 0; i < n; i++)
   {
      v[i].coord[(rdp.t0 << 1)  ] = v[i].u[0];
      v[i].coord[(rdp.t0 << 1)+1] = v[i].v[0];
      v[i].coord[(rdp.t1 << 1)  ] = v[i].u[1];
      v[i].coord[(rdp.t1 << 1)+1] = v[i].v[1];
   }
}

static INLINE void CalculateFog (VERTEX *v)
{
   if (rdp.flags & FOG_ENABLED)
   {
      if (v->w < 0.0f)
         v->f = 0.0f;
      else
         v->f = min(255.0f, max(0.0f, v->z_w * rdp.fog_multiplier + rdp.fog_offset));
      v->a = (uint8_t)v->f;
   }
   else
      v->f = 1.0f;
}

static INLINE void glideSetVertexFlatShading(VERTEX *v, VERTEX **vtx, uint32_t w1)
{
   int flag = min(2, (w1 >> 24) & 3);
   v->a = vtx[flag]->a;
   v->b = vtx[flag]->b;
   v->g = vtx[flag]->g;
   v->r = vtx[flag]->r;
}

static INLINE void glideSetVertexPrimShading(VERTEX *v, uint32_t prim_color)
{
   v->r = (uint8_t)g_gdp.prim_color.r;
   v->g = (uint8_t)g_gdp.prim_color.g;
   v->b = (uint8_t)g_gdp.prim_color.b;
   v->a = (uint8_t)g_gdp.prim_color.a;
}

static INLINE uint32_t vi_integer_sqrt(uint32_t a)
{
   unsigned long op = a, res = 0, one = 1 << 30;

   while (one > op) 
      one >>= 2;

   while (one != 0) 
   {
      if (op >= res + one) 
      {
         op -= res + one;
         res += one << 1;
      }
      res >>= 1;
      one >>= 2;
   }
   return res;
}

static INLINE float get_float_color_clamped(float col)
{
   if (col > 1.0f)
      col = 1.0f;
   if (col < 0.0f)
      col = 0.0f;
   return col;
}

void newSwapBuffers(void);
extern void rdp_setfuncs(void);
extern int SwapOK;

// ** utility functions
void load_palette (uint32_t addr, uint16_t start, uint16_t count);

#endif  // ifndef RDP_H
