#include <stdarg.h>
#include <string.h>


#include "z64.h"
#include "Gfx #1.3.h"
#include "tctables.h"
#include "vi.h"
#include "rdp.h"

#if 0
#define EXTRALOGGING
#endif

#ifdef EXTRALOGGING
static int LOG_ENABLE = 1;
#else
static int LOG_ENABLE = 0;
#endif
#define LOG(...) do { \
   if (LOG_ENABLE) fprintf(stderr, __VA_ARGS__); \
} while(0)

typedef struct
{
  int16_t col[4];
} COLOR;

typedef struct {
    int stalederivs;
    int dolod;
    int partialreject_1cycle; 
    int partialreject_2cycle;
    int special_bsel0; 
    int special_bsel1;
    int rgb_alpha_dither;
    int realblendershiftersneeded;
    int interpixelblendershiftersneeded;
} MODEDERIVS;

typedef struct {
    int sub_a_rgb0;
    int sub_b_rgb0;
    int mul_rgb0;
    int add_rgb0;
    int sub_a_a0;
    int sub_b_a0;
    int mul_a0;
    int add_a0;

    int sub_a_rgb1;
    int sub_b_rgb1;
    int mul_rgb1;
    int add_rgb1;
    int sub_a_a1;
    int sub_b_a1;
    int mul_a1;
    int add_a1;
} COMBINE_MODES;

typedef struct {
    int cycle_type;
    int persp_tex_en;
    int detail_tex_en;
    int sharpen_tex_en;
    int tex_lod_en;
    int en_tlut;
    int tlut_type;
    int sample_type;
    int mid_texel;
    int bi_lerp0;
    int bi_lerp1;
    int convert_one;
    int key_en;
    int rgb_dither_sel;
    int alpha_dither_sel;
    int blend_m1a_0;
    int blend_m1a_1;
    int blend_m1b_0;
    int blend_m1b_1;
    int blend_m2a_0;
    int blend_m2a_1;
    int blend_m2b_0;
    int blend_m2b_1;
    int force_blend;
    int alpha_cvg_select;
    int cvg_times_alpha;
    int z_mode;
    int cvg_dest;
    int color_on_cvg;
    int image_read_en;
    int z_update_en;
    int z_compare_en;
    int antialias_en;
    int z_source_sel;
    int dither_alpha_en;
    int alpha_compare_en;
    MODEDERIVS f;
} OTHER_MODES;

typedef struct {
    int lx, rx;
    int unscrx;
    int validline;
    ALIGNED int32_t rgba[4];
    ALIGNED int32_t stwz[4];
    int32_t majorx[4];
    int32_t minorx[4];
    int32_t invalyscan[4];
} SPAN;

typedef struct {
    int16_t clampdiffs, clampdifft;
    uint8_t clampens, clampent;
    int8_t masksclamped, masktclamped;
    int8_t notlutswitch, tlutswitch;
} FAKETILE;

typedef struct {
    int line; /* 12; */
    int tmem;
    int palette;
    int mask_t, shift_t, mask_s, shift_s;     /* 16 */
    int32_t mask_t_maskbits, mask_s_maskbits; /* 8 */
    uint16_t sl, tl, sh, th;                  /* 8 */
    FAKETILE f;
    char ct, mt, cs, ms;                      /* 6 */
    uint8_t format;
    uint8_t size;
} TILE;

#ifdef _DEBUG
static int render_cycle_mode_counts[4];
#endif

STRICTINLINE int32_t irand(void);

static int8_t get_dither_noise_type;
static int scfield;
static int sckeepodd;

static int ti_format;
static int ti_size;
static int ti_width;
static uint32_t ti_address;

static int fb_format;
static int fb_size;
static int fb_width;
static uint32_t fb_address;
static uint32_t zb_address;

static uint32_t max_level;
static int32_t min_level;
static int16_t primitive_lod_frac;

static uint32_t primitive_z;
static uint16_t primitive_delta_z;

static uint32_t fill_color;

static int16_t *combiner_rgbsub_a_r[2];
static int16_t *combiner_rgbsub_a_g[2];
static int16_t *combiner_rgbsub_a_b[2];
static int16_t *combiner_rgbsub_b_r[2];
static int16_t *combiner_rgbsub_b_g[2];
static int16_t *combiner_rgbsub_b_b[2];
static int16_t *combiner_rgbmul_r[2];
static int16_t *combiner_rgbmul_g[2];
static int16_t *combiner_rgbmul_b[2];
static int16_t *combiner_rgbadd_r[2];
static int16_t *combiner_rgbadd_g[2];
static int16_t *combiner_rgbadd_b[2];

static int16_t *combiner_alphasub_a[2];
static int16_t *combiner_alphasub_b[2];
static int16_t *combiner_alphamul[2];
static int16_t *combiner_alphaadd[2];

static int16_t *blender1a_r[2];
static int16_t *blender1a_g[2];
static int16_t *blender1a_b[2];
static int16_t *blender1b_a[2];
static int16_t *blender2a_r[2];
static int16_t *blender2a_g[2];
static int16_t *blender2a_b[2];
static int16_t *blender2b_a[2];

#define COLOR_RED(val)       (val.col[0])
#define COLOR_GREEN(val)     (val.col[1])
#define COLOR_BLUE(val)      (val.col[2])
#define COLOR_ALPHA(val)     (val.col[3])
#define COLOR_RED_PTR(val)   (val->col[0])
#define COLOR_GREEN_PTR(val) (val->col[1])
#define COLOR_BLUE_PTR(val)  (val->col[2])
#define COLOR_ALPHA_PTR(val) (val->col[3])

#define COLOR_ASSIGN(col0, col1) col0 = col1

#define TRELATIVE(x, y)     ((x) - ((y) << 3));
#define UPPER ((sfrac + tfrac) & 0x20)

static int32_t k0_tf = 0, k1_tf = 0, k2_tf = 0, k3_tf = 0;
static int16_t k4 = 0, k5 = 0;

static TILE tile[8];

static OTHER_MODES other_modes;
static COMBINE_MODES combine;

static COLOR key_width;
static COLOR key_scale;
static COLOR key_center;
static COLOR fog_color;
static COLOR blend_color;
static COLOR prim_color;
static COLOR env_color;

static int rdp_pipeline_crashed;

static RECTANGLE __clip = {
    0, 0, 0x2000, 0x2000
};

static void fbread_4(uint32_t num, uint32_t* curpixel_memcvg);
static void fbread_8(uint32_t num, uint32_t* curpixel_memcvg);
static void fbread_16(uint32_t num, uint32_t* curpixel_memcvg);
static void fbread_32(uint32_t num, uint32_t* curpixel_memcvg);
static void fbread2_4(uint32_t num, uint32_t* curpixel_memcvg);
static void fbread2_8(uint32_t num, uint32_t* curpixel_memcvg);
static void fbread2_16(uint32_t num, uint32_t* curpixel_memcvg);
static void fbread2_32(uint32_t num, uint32_t* curpixel_memcvg);

static void (*fbread_func[4])(uint32_t, uint32_t*) = {
    fbread_4, fbread_8, fbread_16, fbread_32
};
static void (*fbread2_func[4])(uint32_t, uint32_t*) = {
    fbread2_4, fbread2_8, fbread2_16, fbread2_32
};

void (*fbread1_ptr)(uint32_t, uint32_t*);
void (*fbread2_ptr)(uint32_t, uint32_t*);
void (*fbwrite_ptr)(uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t);

#define PAIRWRITE16(in, rval, hval) {            \
   (in) &= (RDRAM_MASK >> 1);	                   \
    if ((in) <= idxlim16) {                      \
        rdram_16[(in) ^ WORD_ADDR_XOR] = (rval); \
        hidden_bits[(in)] = (hval);              \
    }                                            \
}
#define PAIRWRITE32(in, rval, hval0, hval1) {    \
   (in) &= (RDRAM_MASK >> 2);                    \
    if ((in) <= idxlim32) {                      \
        rdram[(in)] = (rval);                    \
        hidden_bits[(in) << 1] = (hval0);        \
        hidden_bits[((in) << 1) + 1] = (hval1);  \
    }                                            \
}
#define PAIRWRITE8(in, rval, hval) {             \
   (in) &= RDRAM_MASK;                           \
    if ((in) <= plim) {                          \
        rdram_8[(in) ^ BYTE_ADDR_XOR] = (rval);  \
        if ((in) & 1)                            \
            hidden_bits[(in) >> 1] = (hval);     \
    }                                            \
}

uint32_t internal_vi_v_current_line = 0;
uint32_t old_vi_origin = 0;
uint32_t oldhstart = 0;
uint32_t oldsomething = 0;
int blshifta = 0, blshiftb = 0, pastblshifta = 0, pastblshiftb = 0;
int32_t pastrawdzmem = 0;
int32_t iseed = 1;

static SPAN span[1024];
uint8_t cvgbuf[1024];

static int32_t spans_d_rgba[4];
static int32_t spans_d_stwz[4];
static uint16_t spans_dzpix;

static int32_t spans_d_rgba_dy[4];
static int32_t spans_cd_rgba[4];
static int spans_cdz;

static int32_t spans_d_stwz_dy[4];

typedef struct
{
    int tilenum;
    uint16_t xl, yl, xh, yh;        
    int16_t s, t;                    
    int16_t dsdx, dtdy;            
    uint32_t flip;    
} TEX_RECTANGLE;


#define CVG_CLAMP                0
#define CVG_WRAP                1
#define CVG_ZAP                    2
#define CVG_SAVE                3


#define ZMODE_OPAQUE            0
#define ZMODE_INTERPENETRATING    1
#define ZMODE_TRANSPARENT        2
#define ZMODE_DECAL                3

COLOR combined_color;
COLOR texel0_color;
COLOR texel1_color;
COLOR nexttexel_color;
COLOR shade_color;
static int16_t noise = 0;
static int16_t one_color = 0x100;
static int16_t zero_color = 0x00;

static int16_t blenderone    = 0xff;

COLOR pixel_color;
COLOR inv_pixel_color;
COLOR blended_pixel_color;
COLOR memory_color;
COLOR pre_memory_color;

int oldscyl = 0;

uint8_t __TMEM[0x1000]; 

#define tlut ((uint16_t*)(&__TMEM[0x800]))

#define PIXELS_TO_BYTES(pix, siz) (((pix) << (siz)) >> 1)

typedef struct{
    int startspan;
    int endspan;
    int preendspan;
    int nextspan;
    int midspan;
    int longspan;
    int onelessthanmid;
}SPANSIGS;

static int16_t lod_frac = 0;
struct {uint32_t shift; uint32_t add;} z_dec_table[8] = {
     6, 0x00000,
     5, 0x20000,
     4, 0x30000,
     3, 0x38000,
     2, 0x3c000,
     1, 0x3e000,
     0, 0x3f000,
     0, 0x3f800,
};

static void render_spans_1cycle_complete(int start, int end, int tilenum, int flip);
static void render_spans_1cycle_notexel1(int start, int end, int tilenum, int flip);
static void render_spans_1cycle_notex(int start, int end, int tilenum, int flip);

static void (*render_spans_1cycle_func[3])(int, int, int, int) =
{
    render_spans_1cycle_notex, render_spans_1cycle_notexel1, render_spans_1cycle_complete
};

static void render_spans_2cycle_complete(int start, int end, int tilenum, int flip);
static void render_spans_2cycle_notexelnext(int start, int end, int tilenum, int flip);
static void render_spans_2cycle_notexel1(int start, int end, int tilenum, int flip);
static void render_spans_2cycle_notex(int start, int end, int tilenum, int flip);

static void (*render_spans_2cycle_func[4])(int, int, int, int) =
{
    render_spans_2cycle_notex, render_spans_2cycle_notexel1, render_spans_2cycle_notexelnext, render_spans_2cycle_complete
};

static void (*render_spans_1cycle_ptr)(int, int, int, int);

static void (*render_spans_2cycle_ptr)(int start, int end, int tilenum, int flip);

uint16_t z_com_table[0x40000];
uint32_t z_complete_dec_table[0x4000];
uint8_t replicated_rgba[32];
uint8_t special_9bit_clamptable[512];
int16_t special_9bit_exttable[512];
int8_t log2table[256];
int32_t tcdiv_table[0x8000];
uint8_t bldiv_hwaccurate_table[0x8000];
uint16_t deltaz_comparator_lut[0x10000];

static STRICTINLINE void tcmask(int32_t* S, int32_t* T, int32_t num)
{
    int32_t wrap;
    
    

    if (tile[num].mask_s)
    {
        if (tile[num].ms)
        {
            wrap = *S >> tile[num].f.masksclamped;
            wrap &= 1;
            *S ^= (-wrap);
        }
        *S &= tile[num].mask_s_maskbits;
    }

    if (tile[num].mask_t)
    {
        if (tile[num].mt)
        {
            wrap = *T >> tile[num].f.masktclamped;
            wrap &= 1;
            *T ^= (-wrap);
        }
        
        *T &= tile[num].mask_t_maskbits;
    }
}

static STRICTINLINE void tcmask_coupled(int32_t* S, int32_t* S1, int32_t* T, int32_t* T1, int32_t num)
{
    int32_t wrap;
    int32_t maskbits; 
    int32_t wrapthreshold; 


    if (tile[num].mask_s)
    {
        if (tile[num].ms)
        {
            wrapthreshold = tile[num].f.masksclamped;

            wrap = (*S >> wrapthreshold) & 1;
            *S ^= (-wrap);

            wrap = (*S1 >> wrapthreshold) & 1;
            *S1 ^= (-wrap);
        }

        maskbits = tile[num].mask_s_maskbits;
        *S &= maskbits;
        *S1 &= maskbits;
    }

    if (tile[num].mask_t)
    {
        if (tile[num].mt)
        {
            wrapthreshold = tile[num].f.masktclamped;

            wrap = (*T >> wrapthreshold) & 1;
            *T ^= (-wrap);

            wrap = (*T1 >> wrapthreshold) & 1;
            *T1 ^= (-wrap);
        }
        maskbits = tile[num].mask_t_maskbits;
        *T &= maskbits;
        *T1 &= maskbits;
    }
}

static STRICTINLINE void tcmask_copy(int32_t* S, int32_t* S1, int32_t* S2, int32_t* S3, int32_t* T, int32_t num)
{
    int32_t wrap;
    int32_t maskbits_s; 
    int32_t swrapthreshold; 

    if (tile[num].mask_s)
    {
        if (tile[num].ms)
        {
            swrapthreshold = tile[num].f.masksclamped;

            wrap = (*S >> swrapthreshold) & 1;
            *S ^= (-wrap);

            wrap = (*S1 >> swrapthreshold) & 1;
            *S1 ^= (-wrap);

            wrap = (*S2 >> swrapthreshold) & 1;
            *S2 ^= (-wrap);

            wrap = (*S3 >> swrapthreshold) & 1;
            *S3 ^= (-wrap);
        }

        maskbits_s = tile[num].mask_s_maskbits;
        *S &= maskbits_s;
        *S1 &= maskbits_s;
        *S2 &= maskbits_s;
        *S3 &= maskbits_s;
    }

    if (tile[num].mask_t)
    {
        if (tile[num].mt)
        {
            wrap = *T >> tile[num].f.masktclamped; 
            wrap &= 1;
            *T ^= (-wrap);
        }

        *T &= tile[num].mask_t_maskbits;
    }
}


STRICTINLINE void tcshift_cycle(int32_t* S, int32_t* T, int32_t* maxs, int32_t* maxt, uint32_t num)
{



    int32_t coord = *S;
    int32_t shifter = tile[num].shift_s;

    if (shifter < 11)
    {
        coord = SIGN16(coord);
        coord >>= shifter;
    }
    else
    {
        coord <<= (16 - shifter);
        coord = SIGN16(coord);
    }
    *S = coord; 

    

    
    *maxs = ((coord >> 3) >= tile[num].sh);
    
    

    coord = *T;
    shifter = tile[num].shift_t;

    if (shifter < 11)
    {
        coord = SIGN16(coord);
        coord >>= shifter;
    }
    else
    {
        coord <<= (16 - shifter);
        coord = SIGN16(coord);
    }
    *T = coord; 
    *maxt = ((coord >> 3) >= tile[num].th);
}    


STRICTINLINE void tcshift_copy(int32_t* S, int32_t* T, uint32_t num)
{
    int32_t coord = *S;
    int32_t shifter = tile[num].shift_s;

    if (shifter < 11)
    {
        coord = SIGN16(coord);
        coord >>= shifter;
    }
    else
    {
        coord <<= (16 - shifter);
        coord = SIGN16(coord);
    }
    *S = coord; 

    coord = *T;
    shifter = tile[num].shift_t;

    if (shifter < 11)
    {
        coord = SIGN16(coord);
        coord >>= shifter;
    }
    else
    {
        coord <<= (16 - shifter);
        coord = SIGN16(coord);
    }
    *T = coord; 
    
}

static STRICTINLINE void tcclamp_cycle(int32_t* S, int32_t* T, int32_t* SFRAC, int32_t* TFRAC, int32_t maxs, int32_t maxt, int32_t num)
{

    int32_t locs = *S, loct = *T;
    if (tile[num].f.clampens)
    {
        if (!(locs & 0x10000))
        {
            if (!maxs)
                *S = (locs >> 5);
            else
            {
                *S = tile[num].f.clampdiffs;
                *SFRAC = 0;
            }
        }
        else
        {
            *S = 0;
            *SFRAC = 0;
        }
    }
    else
        *S = (locs >> 5);

    if (tile[num].f.clampent)
    {
        if (!(loct & 0x10000))
        {
            if (!maxt)
                *T = (loct >> 5);
            else
            {
                *T = tile[num].f.clampdifft;
                *TFRAC = 0;
            }
        }
        else
        {
            *T = 0;
            *TFRAC = 0;
        }
    }
    else
        *T = (loct >> 5);
}

static STRICTINLINE void tcclamp_cycle_light(int32_t* S, int32_t* T, int32_t maxs, int32_t maxt, int32_t num)
{
    int32_t locs = *S, loct = *T;
    if (tile[num].f.clampens)
    {
        if (!(locs & 0x10000))
        {
            if (!maxs)
                *S = (locs >> 5);
            else
                *S = tile[num].f.clampdiffs;
        }
        else
            *S = 0;
    }
    else
        *S = (locs >> 5);

    if (tile[num].f.clampent)
    {
        if (!(loct & 0x10000))
        {
            if (!maxt)
                *T = (loct >> 5);
            else
                *T = tile[num].f.clampdifft;
        }
        else
            *T = 0;
    }
    else
        *T = (loct >> 5);
}

static void fbwrite_4(
    uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en,
    uint32_t curpixel_cvg, uint32_t curpixel_memcvg);
static void fbwrite_8(
    uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en,
    uint32_t curpixel_cvg, uint32_t curpixel_memcvg);
static void fbwrite_16(
    uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en,
    uint32_t curpixel_cvg, uint32_t curpixel_memcvg);
static void fbwrite_32(
    uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en,
    uint32_t curpixel_cvg, uint32_t curpixel_memcvg);
static void (*fbwrite_func[4])(
    uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t) = {
    fbwrite_4, fbwrite_8, fbwrite_16, fbwrite_32
};

static INLINE void SET_BLENDER_INPUT(int cycle, int which, int16_t **input_r, int16_t **input_g, int16_t **input_b, int16_t **input_a, int a, int b)
{

    switch (a & 0x3)
    {
        case 0:
        {
            if (cycle == 0)
            {
                *input_r = &COLOR_RED(pixel_color);
                *input_g = &COLOR_GREEN(pixel_color);
                *input_b = &COLOR_BLUE(pixel_color);
            }
            else
            {
                *input_r = &COLOR_RED(blended_pixel_color);
                *input_g = &COLOR_GREEN(blended_pixel_color);
                *input_b = &COLOR_BLUE(blended_pixel_color);
            }
            break;
        }

        case 1:
        {
            *input_r = &COLOR_RED(memory_color);
            *input_g = &COLOR_GREEN(memory_color);
            *input_b = &COLOR_BLUE(memory_color);
            break;
        }

        case 2:
        {
            *input_r = &COLOR_RED(blend_color);
            *input_g = &COLOR_GREEN(blend_color);
            *input_b = &COLOR_BLUE(blend_color);
            break;
        }

        case 3:
        {
            *input_r = &COLOR_RED(fog_color);
            *input_g = &COLOR_GREEN(fog_color);
            *input_b = &COLOR_BLUE(fog_color);
            break;
        }
    }

    if (which == 0)
    {
        switch (b & 0x3)
        {
           case 0:       
              *input_a = &COLOR_ALPHA(pixel_color);
              break;
           case 1:
              *input_a = &COLOR_ALPHA(fog_color);
              break;
           case 2:
              *input_a = &COLOR_ALPHA(shade_color);
              break;
           case 3:
              *input_a = &zero_color;
              break;
        }
    }
    else
    {
        switch (b & 0x3)
        {
            case 0: 
               *input_a = &COLOR_ALPHA(inv_pixel_color);
               break;
            case 1:
               *input_a = &COLOR_ALPHA(memory_color);
               break;
            case 2:
               *input_a = &blenderone;
               break;
            case 3:
               *input_a = &zero_color;
               break;
        }
    }
}

static INLINE void calculate_clamp_diffs(uint32_t i)
{
    tile[i].f.clampdiffs = ((tile[i].sh >> 2) - (tile[i].sl >> 2)) & 0x3ff;
    tile[i].f.clampdifft = ((tile[i].th >> 2) - (tile[i].tl >> 2)) & 0x3ff;
}


static INLINE void calculate_tile_derivs(uint32_t i)
{
    tile[i].f.clampens = tile[i].cs || !tile[i].mask_s;
    tile[i].f.clampent = tile[i].ct || !tile[i].mask_t;
    tile[i].f.masksclamped = tile[i].mask_s <= 10 ? tile[i].mask_s : 10;
    tile[i].f.masktclamped = tile[i].mask_t <= 10 ? tile[i].mask_t : 10;
    tile[i].f.notlutswitch = (tile[i].format << 2) | tile[i].size;
    tile[i].f.tlutswitch = (tile[i].size << 2) | ((tile[i].format + 2) & 3);
}

static INLINE void z_build_com_table(void)
{
   int z;
   uint16_t altmem = 0;

   for (z = 0; z < 0x40000; z++)
   {
      switch((z >> 11) & 0x7f)
      {
         case 0x00:
         case 0x01:
         case 0x02:
         case 0x03:
         case 0x04:
         case 0x05:
         case 0x06:
         case 0x07:
         case 0x08:
         case 0x09:
         case 0x0a:
         case 0x0b:
         case 0x0c:
         case 0x0d:
         case 0x0e:
         case 0x0f:
         case 0x10:
         case 0x11:
         case 0x12:
         case 0x13:
         case 0x14:
         case 0x15:
         case 0x16:
         case 0x17:
         case 0x18:
         case 0x19:
         case 0x1a:
         case 0x1b:
         case 0x1c:
         case 0x1d:
         case 0x1e:
         case 0x1f:
         case 0x20:
         case 0x21:
         case 0x22:
         case 0x23:
         case 0x24:
         case 0x25:
         case 0x26:
         case 0x27:
         case 0x28:
         case 0x29:
         case 0x2a:
         case 0x2b:
         case 0x2c:
         case 0x2d:
         case 0x2e:
         case 0x2f:
         case 0x30:
         case 0x31:
         case 0x32:
         case 0x33:
         case 0x34:
         case 0x35:
         case 0x36:
         case 0x37:
         case 0x38:
         case 0x39:
         case 0x3a:
         case 0x3b:
         case 0x3c:
         case 0x3d:
         case 0x3e:
         case 0x3f:
            altmem = (z >> 4) & 0x1ffc;
            break;
         case 0x40:
         case 0x41:
         case 0x42:
         case 0x43:
         case 0x44:
         case 0x45:
         case 0x46:
         case 0x47:
         case 0x48:
         case 0x49:
         case 0x4a:
         case 0x4b:
         case 0x4c:
         case 0x4d:
         case 0x4e:
         case 0x4f:
         case 0x50:
         case 0x51:
         case 0x52:
         case 0x53:
         case 0x54:
         case 0x55:
         case 0x56:
         case 0x57:
         case 0x58:
         case 0x59:
         case 0x5a:
         case 0x5b:
         case 0x5c:
         case 0x5d:
         case 0x5e:
         case 0x5f:
            altmem = ((z >> 3) & 0x1ffc) | 0x2000;
            break;
         case 0x60:
         case 0x61:
         case 0x62:
         case 0x63:
         case 0x64:
         case 0x65:
         case 0x66:
         case 0x67:
         case 0x68:
         case 0x69:
         case 0x6a:
         case 0x6b:
         case 0x6c:
         case 0x6d:
         case 0x6e:
         case 0x6f:
            altmem = ((z >> 2) & 0x1ffc) | 0x4000;
            break;
         case 0x70:
         case 0x71:
         case 0x72:
         case 0x73:
         case 0x74:
         case 0x75:
         case 0x76:
         case 0x77:
            altmem = ((z >> 1) & 0x1ffc) | 0x6000;
            break;
         case 0x78:
         case 0x79:
         case 0x7a:
         case 0x7b:
            altmem = (z & 0x1ffc) | 0x8000;
            break;
         case 0x7c:
         case 0x7d:
            altmem = ((z << 1) & 0x1ffc) | 0xa000;
            break;
         case 0x7e:
            altmem = ((z << 2) & 0x1ffc) | 0xc000;
            break;
         case 0x7f:
            altmem = ((z << 2) & 0x1ffc) | 0xe000;
            break;
         default:
            DisplayError("z_build_com_table failed");
            break;
      }

      z_com_table[z] = altmem;
   }
}

static uint32_t vi_integer_sqrt(uint32_t a)
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

static void precalculate_everything(void)
{
    int ps[9];
    uint32_t exponent;
    uint32_t mantissa;
    int temppoint, tempslope; 
    int normout;
    int wnorm;
    int shift, tlu_rcp;
    int d = 0, n = 0, temp = 0, res = 0, invd = 0, nbit = 0;
    int i = 0, k = 0, j = 0;

    for (i = 0; i < 256; i++)
    {
        gamma_table[i] = vi_integer_sqrt(i << 6);
        gamma_table[i] <<= 1;
    }
    for (i = 0; i < 0x4000; i++)
    {
        gamma_dither_table[i] = vi_integer_sqrt(i);
        gamma_dither_table[i] <<= 1;
    }

    z_build_com_table();

    for (i = 0; i < 0x4000; i++)
    {
        exponent = (i >> 11) & 7;
        mantissa = i & 0x7ff;
        z_complete_dec_table[i] = ((mantissa << z_dec_table[exponent].shift) + z_dec_table[exponent].add) & 0x3ffff;
    }

    precalc_cvmask_derivatives();

    i = 0;
    log2table[0] = log2table[1] = 0;
    for (i = 2; i < 256; i++)
    {
        for (k = 7; k > 0; k--)
        {
            if((i >> k) & 1)
            {
                log2table[i] = k;
                break;
            }
        }
    }

    for (i = 0; i < 0x400; i++)
    {
        if (((i >> 5) & 0x1f) < (i & 0x1f))
            vi_restore_table[i] = 1;
        else if (((i >> 5) & 0x1f) > (i & 0x1f))
            vi_restore_table[i] = -1;
        else
            vi_restore_table[i] = 0;
    }

    for (i = 0; i < 32; i++)
        replicated_rgba[i] = (i << 3) | ((i >> 2) & 7); 

    for(i = 0; i < 0x200; i++)
    {
        switch((i >> 7) & 3)
        {
        case 0:
        case 1:
            special_9bit_clamptable[i] = i & 0xff;
            break;
        case 2:
            special_9bit_clamptable[i] = 0xff;
            break;
        case 3:
            special_9bit_clamptable[i] = 0;
            break;
        }
    }

    for(i = 0; i < 0x200; i++)
    {
        special_9bit_exttable[i] = ((i & 0x180) == 0x180) ? (i | ~0x1ff) : (i & 0x1ff);
    }

    for (i = 0; i < 0x8000; i++)
    {
        for (k = 1; k <= 14 && !((i << k) & 0x8000); k++) 
            ;
        shift = k - 1;
        normout = (i << shift) & 0x3fff;
        wnorm = (normout & 0xff) << 2;
        normout >>= 8;

        temppoint = norm_point_table[normout];
        tempslope = norm_slope_table[normout];

        tempslope = (tempslope | ~0x3ff) + 1;
        
        tlu_rcp = (((tempslope * wnorm) >> 10) + temppoint) & 0x7fff;
        
        tcdiv_table[i] = shift | (tlu_rcp << 4);
    }

    for (i = 0; i < 0x8000; i++)
    {
        res = 0;
        d = (i >> 11) & 0xf;
        n = i & 0x7ff;
        invd = (~d) & 0xf;

        temp = invd + (n >> 8) + 1;
        ps[0] = temp & 7;
        for (k = 0; k < 8; k++)
        {
            nbit = (n >> (7 - k)) & 1;
            if (res & (0x100 >> k))
                temp = invd + (ps[k] << 1) + nbit + 1;
            else
                temp = d + (ps[k] << 1) + nbit;
            ps[k + 1] = temp & 7;
            if (temp & 0x10)
                res |= (1 << (7 - k));
        }
        bldiv_hwaccurate_table[i] = res;
    }

    deltaz_comparator_lut[0] = 0;
    for (i = 1; i < 0x10000; i++)
    {
       for (k = 15; k >= 0; k--)
       {
          if (i & (1 << k))
          {
             deltaz_comparator_lut[i] = 1 << k;
             break;
          }
       }
    }
}

void rdp_init(void)
{
    int i;

    fbread1_ptr = fbread_func[0];
    fbread2_ptr = fbread2_func[0];
    fbwrite_ptr = fbwrite_func[0];
    render_spans_1cycle_ptr = render_spans_1cycle_func[2];
    render_spans_2cycle_ptr = render_spans_2cycle_func[1];

    combiner_rgbsub_a_r[0] = combiner_rgbsub_a_r[1] = &one_color;
    combiner_rgbsub_a_g[0] = combiner_rgbsub_a_g[1] = &one_color;
    combiner_rgbsub_a_b[0] = combiner_rgbsub_a_b[1] = &one_color;
    combiner_rgbsub_b_r[0] = combiner_rgbsub_b_r[1] = &one_color;
    combiner_rgbsub_b_g[0] = combiner_rgbsub_b_g[1] = &one_color;
    combiner_rgbsub_b_b[0] = combiner_rgbsub_b_b[1] = &one_color;
    combiner_rgbmul_r[0] = combiner_rgbmul_r[1] = &one_color;
    combiner_rgbmul_g[0] = combiner_rgbmul_g[1] = &one_color;
    combiner_rgbmul_b[0] = combiner_rgbmul_b[1] = &one_color;
    combiner_rgbadd_r[0] = combiner_rgbadd_r[1] = &one_color;
    combiner_rgbadd_g[0] = combiner_rgbadd_g[1] = &one_color;
    combiner_rgbadd_b[0] = combiner_rgbadd_b[1] = &one_color;

    combiner_alphasub_a[0] = combiner_alphasub_a[1] = &one_color;
    combiner_alphasub_b[0] = combiner_alphasub_b[1] = &one_color;
    combiner_alphamul[0] = combiner_alphamul[1] = &one_color;
    combiner_alphaadd[0] = combiner_alphaadd[1] = &one_color;

    SET_BLENDER_INPUT(0, 0, &blender1a_r[0], &blender1a_g[0], &blender1a_b[0],
                      &blender1b_a[0], 0, 0);
    SET_BLENDER_INPUT(0, 1, &blender2a_r[0], &blender2a_g[0], &blender2a_b[0],
                      &blender2b_a[0], 0, 0);
    SET_BLENDER_INPUT(1, 0, &blender1a_r[1], &blender1a_g[1], &blender1a_b[1],
                      &blender1b_a[1], 0, 0);
    SET_BLENDER_INPUT(1, 1, &blender2a_r[1], &blender2a_g[1], &blender2a_b[1],
                      &blender2b_a[1], 0, 0);
    other_modes.f.stalederivs = 1;
    memset(__TMEM, 0, 0x1000);

    for (i = 0; i < sizeof(hidden_bits); i++)
        hidden_bits[i] = 0x03;

    memset(tile, 0, sizeof(tile));
    for (i = 0; i < 8; i++)
    {
        calculate_tile_derivs(i);
        calculate_clamp_diffs(i);
    }

    memset(&combined_color, 0, sizeof(COLOR));
    memset(&prim_color, 0, sizeof(COLOR));
    memset(&env_color, 0, sizeof(COLOR));
    memset(&key_scale, 0, sizeof(COLOR));
    memset(&key_center, 0, sizeof(COLOR));

    rdp_pipeline_crashed = 0;
    memset(&onetimewarnings, 0, sizeof(onetimewarnings));

    precalculate_everything();

/*
 * Any current plugin specifications have never told the graphics plugin how
 * much RDRAM is allocated, so it becomes very difficult to detect this in C.
 *
 * Mupen64Plus seems to carelessly map 8 MiB of RDRAM all the time, so we
 * will simply use the 8-MiB addressing limit.
 */
    plim = 0x007FFFFFul;

    /* 16- and 32-bit pointer indexing limits for aliasing RDRAM reads and writes */
	idxlim16 = 0x3fffff;
	idxlim32 = 0x1fffff;

    rdram_8 = (uint8_t*)gfx_info.RDRAM;
    rdram_16 = (uint16_t*)gfx_info.RDRAM;
}

static INLINE void SET_SUBA_RGB_INPUT(int16_t **input_r, int16_t **input_g, int16_t **input_b, int code)
{
    switch (code & 0xf)
    {
       case 0:
          *input_r = &COLOR_RED(combined_color);
          *input_g = &COLOR_GREEN(combined_color);
          *input_b = &COLOR_BLUE(combined_color);
          break;
       case 1:
          *input_r = &COLOR_RED(texel0_color);
          *input_g = &COLOR_GREEN(texel0_color);
          *input_b = &COLOR_BLUE(texel0_color);
          break;
       case 2:
          *input_r = &COLOR_RED(texel1_color);
          *input_g = &COLOR_GREEN(texel1_color);
          *input_b = &COLOR_BLUE(texel1_color);
          break;
       case 3:
          *input_r = &COLOR_RED(prim_color);
          *input_g = &COLOR_GREEN(prim_color);
          *input_b = &COLOR_BLUE(prim_color);
          break;
       case 4:
          *input_r = &COLOR_RED(shade_color);
          *input_g = &COLOR_GREEN(shade_color);
          *input_b = &COLOR_BLUE(shade_color);
          break;
       case 5:
          *input_r = &COLOR_RED(env_color);
          *input_g = &COLOR_GREEN(env_color);
          *input_b = &COLOR_BLUE(env_color);
          break;
       case 6:
          *input_r = &one_color;
          *input_g = &one_color;
          *input_b = &one_color;
          break;
       case 7:
          *input_r = &noise;
          *input_g = &noise;
          *input_b = &noise;
          break;
       case 8:
       case 9:
       case 10:
       case 11:
       case 12:
       case 13:
       case 14:
       case 15:
          *input_r = &zero_color;
          *input_g = &zero_color;
          *input_b = &zero_color;
          break;
    }
}

static INLINE void SET_SUBB_RGB_INPUT(int16_t **input_r, int16_t **input_g, int16_t **input_b, int code)
{
    switch (code & 0xf)
    {
       case 0:
          *input_r = &COLOR_RED(combined_color);
          *input_g = &COLOR_GREEN(combined_color);
          *input_b = &COLOR_BLUE(combined_color);
          break;
       case 1:
          *input_r = &COLOR_RED(texel0_color);
          *input_g = &COLOR_GREEN(texel0_color);
          *input_b = &COLOR_BLUE(texel0_color);
          break;
       case 2:
          *input_r = &COLOR_RED(texel1_color);
          *input_g = &COLOR_GREEN(texel1_color);
          *input_b = &COLOR_BLUE(texel1_color);
          break;
       case 3:
          *input_r = &COLOR_RED(prim_color);
          *input_g = &COLOR_GREEN(prim_color);
          *input_b = &COLOR_BLUE(prim_color);
          break;
       case 4:
          *input_r = &COLOR_RED(shade_color);
          *input_g = &COLOR_GREEN(shade_color);
          *input_b = &COLOR_BLUE(shade_color);
          break;
       case 5:
          *input_r = &COLOR_RED(env_color);
          *input_g = &COLOR_GREEN(env_color);
          *input_b = &COLOR_BLUE(env_color);
          break;
       case 6:
          *input_r = &COLOR_RED(key_center);
          *input_g = &COLOR_GREEN(key_center);
          *input_b = &COLOR_BLUE(key_center);
          break;
       case 7:
          *input_r = &k4;
          *input_g = &k4;
          *input_b = &k4;
          break;
       case 8:
       case 9:
       case 10:
       case 11:
       case 12:
       case 13:
       case 14:
       case 15:
          *input_r = &zero_color;
          *input_g = &zero_color;
          *input_b = &zero_color;
          break;
    }
}

static INLINE void SET_MUL_RGB_INPUT(int16_t **input_r, int16_t **input_g, int16_t **input_b, int code)
{
    switch (code & 0x1f)
    {
       case 0:
          *input_r = &COLOR_RED(combined_color);
          *input_g = &COLOR_GREEN(combined_color);
          *input_b = &COLOR_BLUE(combined_color);
          break;
       case 1:
          *input_r = &COLOR_RED(texel0_color);
          *input_g = &COLOR_GREEN(texel0_color);
          *input_b = &COLOR_BLUE(texel0_color);
          break;
       case 2:
          *input_r = &COLOR_RED(texel1_color);
          *input_g = &COLOR_GREEN(texel1_color);
          *input_b = &COLOR_BLUE(texel1_color);
          break;
       case 3:
          *input_r = &COLOR_RED(prim_color);
          *input_g = &COLOR_GREEN(prim_color);
          *input_b = &COLOR_BLUE(prim_color);
          break;
       case 4:
          *input_r = &COLOR_RED(shade_color);
          *input_g = &COLOR_GREEN(shade_color);
          *input_b = &COLOR_BLUE(shade_color);
          break;
       case 5:
          *input_r = &COLOR_RED(env_color);
          *input_g = &COLOR_GREEN(env_color);
          *input_b = &COLOR_BLUE(env_color);
          break;
       case 6:
          *input_r = &COLOR_RED(key_scale);
          *input_g = &COLOR_GREEN(key_scale);
          *input_b = &COLOR_BLUE(key_scale);
          break;
       case 7:
          *input_r = &COLOR_ALPHA(combined_color);
          *input_g = &COLOR_ALPHA(combined_color);
          *input_b = &COLOR_ALPHA(combined_color);
          break;
       case 8:
          *input_r = &COLOR_ALPHA(texel0_color);
          *input_g = &COLOR_ALPHA(texel0_color);
          *input_b = &COLOR_ALPHA(texel0_color);
          break;
       case 9:
          *input_r = &COLOR_ALPHA(texel1_color);
          *input_g = &COLOR_ALPHA(texel1_color);
          *input_b = &COLOR_ALPHA(texel1_color);
          break;
       case 10:
          *input_r = &COLOR_ALPHA(prim_color);
          *input_g = &COLOR_ALPHA(prim_color);
          *input_b = &COLOR_ALPHA(prim_color); 
          break;
       case 11:
          *input_r = &COLOR_ALPHA(shade_color);
          *input_g = &COLOR_ALPHA(shade_color);
          *input_b = &COLOR_ALPHA(shade_color);
          break;
       case 12:
          *input_r = &COLOR_ALPHA(env_color);
          *input_g = &COLOR_ALPHA(env_color);
          *input_b = &COLOR_ALPHA(env_color);
          break;
       case 13:
          *input_r = &lod_frac;
          *input_g = &lod_frac;
          *input_b = &lod_frac;
          break;
       case 14:
          *input_r = &primitive_lod_frac;
          *input_g = &primitive_lod_frac;
          *input_b = &primitive_lod_frac;
          break;
       case 15:
          *input_r = &k5;
          *input_g = &k5;
          *input_b = &k5;
          break;
       case 16:
       case 17:
       case 18:
       case 19:
       case 20:
       case 21:
       case 22:
       case 23:
       case 24:
       case 25:
       case 26:
       case 27:
       case 28:
       case 29:
       case 30:
       case 31:
          *input_r = &zero_color;
          *input_g = &zero_color;
          *input_b = &zero_color;
          break;
    }
}

static INLINE void SET_ADD_RGB_INPUT(int16_t **input_r, int16_t **input_g, int16_t **input_b, int code)
{
   switch (code & 0x7)
   {
      case 0:
         *input_r = &COLOR_RED(combined_color);
         *input_g = &COLOR_GREEN(combined_color);
         *input_b = &COLOR_BLUE(combined_color);
         break;
      case 1:
         *input_r = &COLOR_RED(texel0_color);
         *input_g = &COLOR_GREEN(texel0_color);
         *input_b = &COLOR_BLUE(texel0_color);
         break;
      case 2:
         *input_r = &COLOR_RED(texel1_color);
         *input_g = &COLOR_GREEN(texel1_color);
         *input_b = &COLOR_BLUE(texel1_color);
         break;
      case 3:
         *input_r = &COLOR_RED(prim_color);
         *input_g = &COLOR_GREEN(prim_color);
         *input_b = &COLOR_BLUE(prim_color);
         break;
      case 4:
         *input_r = &COLOR_RED(shade_color);
         *input_g = &COLOR_GREEN(shade_color);
         *input_b = &COLOR_BLUE(shade_color);
         break;
      case 5:
         *input_r = &COLOR_RED(env_color);
         *input_g = &COLOR_GREEN(env_color);
         *input_b = &COLOR_BLUE(env_color);
         break;
      case 6:
         *input_r = &one_color;
         *input_g = &one_color;
         *input_b = &one_color;
         break;
      case 7:
         *input_r = &zero_color;
         *input_g = &zero_color;
         *input_b = &zero_color;
         break;
   }
}

static INLINE void SET_SUB_ALPHA_INPUT(int16_t **input, int code)
{
    switch (code & 0x7)
    {
        case 0:        *input = &COLOR_ALPHA(combined_color); break;
        case 1:        *input = &COLOR_ALPHA(texel0_color); break;
        case 2:        *input = &COLOR_ALPHA(texel1_color); break;
        case 3:        *input = &COLOR_ALPHA(prim_color); break;
        case 4:        *input = &COLOR_ALPHA(shade_color); break;
        case 5:        *input = &COLOR_ALPHA(env_color); break;
        case 6:        *input = &one_color; break;
        case 7:        *input = &zero_color; break;
    }
}

static INLINE void SET_MUL_ALPHA_INPUT(int16_t **input, int code)
{
    switch (code & 0x7)
    {
       case 0:
          *input = &lod_frac;
          break;
       case 1:
          *input = &COLOR_ALPHA(texel0_color);
          break;
       case 2:
          *input = &COLOR_ALPHA(texel1_color);
          break;
       case 3:
          *input = &COLOR_ALPHA(prim_color);
          break;
       case 4:
          *input = &COLOR_ALPHA(shade_color);
          break;
       case 5:
          *input = &COLOR_ALPHA(env_color);
          break;
       case 6:
          *input = &primitive_lod_frac;
          break;
       case 7:
          *input = &zero_color;
          break;
    }
}

static STRICTINLINE int alpha_compare(int32_t comb_alpha)
{
    int32_t threshold;

    if (!other_modes.alpha_compare_en)
        return 1;
    else
    {
        if (!other_modes.dither_alpha_en)
            threshold = COLOR_ALPHA(blend_color);
        else
            threshold = irand() & 0xff;
        if (comb_alpha >= threshold)
            return 1;
        else
            return 0;
    }
}

static STRICTINLINE int32_t color_combiner_equation(int32_t a, int32_t b, int32_t c, int32_t d)
{
    a = special_9bit_exttable[a];
    b = special_9bit_exttable[b];
    c = SIGNF(c, 9);
    d = special_9bit_exttable[d];
    a = ((a - b) * c) + (d << 8) + 0x80;
    return (a & 0x1ffff);
}

static STRICTINLINE int32_t alpha_combiner_equation(int32_t a, int32_t b, int32_t c, int32_t d)
{
    a = special_9bit_exttable[a];
    b = special_9bit_exttable[b];
    c = SIGNF(c, 9);
    d = special_9bit_exttable[d];
    a = (((a - b) * c) + (d << 8) + 0x80) >> 8;
    return (a & 0x1ff);
}

static STRICTINLINE int32_t CLIP(int32_t value,int32_t min,int32_t max)
{
    if (value < min)
        return min;
    else if (value > max)
        return max;
    return value;
}

static void combiner_1cycle(int adseed, uint32_t* curpixel_cvg)
{
    int32_t temp_combined_color[3];
    int32_t redkey, greenkey, bluekey, temp;
    COLOR chromabypass;
    int32_t keyalpha;

    if (other_modes.key_en)
    {
       COLOR_RED(chromabypass)   = *combiner_rgbsub_a_r[1];
       COLOR_GREEN(chromabypass) = *combiner_rgbsub_a_g[1];
       COLOR_BLUE(chromabypass)  = *combiner_rgbsub_a_b[1];
    }
    
    temp_combined_color[0] = color_combiner_equation(*combiner_rgbsub_a_r[1],*combiner_rgbsub_b_r[1],*combiner_rgbmul_r[1],*combiner_rgbadd_r[1]);
    temp_combined_color[1] = color_combiner_equation(*combiner_rgbsub_a_g[1],*combiner_rgbsub_b_g[1],*combiner_rgbmul_g[1],*combiner_rgbadd_g[1]);
    temp_combined_color[2] = color_combiner_equation(*combiner_rgbsub_a_b[1],*combiner_rgbsub_b_b[1],*combiner_rgbmul_b[1],*combiner_rgbadd_b[1]);
    COLOR_ALPHA(combined_color) = alpha_combiner_equation(*combiner_alphasub_a[1],*combiner_alphasub_b[1],*combiner_alphamul[1],*combiner_alphaadd[1]);

    COLOR_ALPHA(pixel_color) = special_9bit_clamptable[COLOR_ALPHA(combined_color)];
    if (COLOR_ALPHA(pixel_color) == 0xff)
        COLOR_ALPHA(pixel_color) = 0x100;

    if (!other_modes.key_en)
    {
        COLOR_RED(combined_color)     = temp_combined_color[0] >> 8;
        COLOR_GREEN(combined_color)   = temp_combined_color[1] >> 8;
        COLOR_BLUE(combined_color)    = temp_combined_color[2] >> 8;
        COLOR_RED(pixel_color)        = special_9bit_clamptable[COLOR_RED(combined_color)];
        COLOR_GREEN(pixel_color)      = special_9bit_clamptable[COLOR_GREEN(combined_color)];
        COLOR_BLUE(pixel_color)       = special_9bit_clamptable[COLOR_BLUE(combined_color)];
    }
    else
    {
        redkey = SIGN(temp_combined_color[0], 17);
        if (redkey >= 0)
            redkey = (COLOR_RED(key_width) << 4) - redkey;
        else
            redkey = (COLOR_RED(key_width) << 4) + redkey;
        greenkey = SIGN(temp_combined_color[1], 17);
        if (greenkey >= 0)
            greenkey = (COLOR_GREEN(key_width) << 4) - greenkey;
        else
            greenkey = (COLOR_GREEN(key_width) << 4) + greenkey;
        bluekey = SIGN(temp_combined_color[2], 17);
        if (bluekey >= 0)
            bluekey = (COLOR_BLUE(key_width) << 4) - bluekey;
        else
            bluekey = (COLOR_BLUE(key_width) << 4) + bluekey;
        keyalpha = (redkey < greenkey) ? redkey : greenkey;
        keyalpha = (bluekey < keyalpha) ? bluekey : keyalpha;
        keyalpha = CLIP(keyalpha, 0, 0xff);

        
        COLOR_RED(pixel_color)   = special_9bit_clamptable[COLOR_RED(chromabypass)];
        COLOR_GREEN(pixel_color) = special_9bit_clamptable[COLOR_GREEN(chromabypass)];
        COLOR_BLUE(pixel_color)  = special_9bit_clamptable[COLOR_BLUE(chromabypass)];

        COLOR_RED(combined_color)   = temp_combined_color[0] >> 8;
        COLOR_GREEN(combined_color) = temp_combined_color[1] >> 8;
        COLOR_BLUE(combined_color)  = temp_combined_color[2] >> 8;
    }
    
    
    if (other_modes.cvg_times_alpha)
    {
        temp = (COLOR_ALPHA(pixel_color) * (*curpixel_cvg) + 4) >> 3;
        *curpixel_cvg = (temp >> 5) & 0xf;
    }

    if (!other_modes.alpha_cvg_select)
    {    
        if (!other_modes.key_en)
        {
            COLOR_ALPHA(pixel_color) += adseed;
            if (COLOR_ALPHA(pixel_color) & 0x100)
                COLOR_ALPHA(pixel_color) = 0xff;
        }
        else
            COLOR_ALPHA(pixel_color) = keyalpha;
    }
    else
    {
        if (other_modes.cvg_times_alpha)
            COLOR_ALPHA(pixel_color) = temp;
        else
            COLOR_ALPHA(pixel_color) = (*curpixel_cvg) << 5;
        if (COLOR_ALPHA(pixel_color) > 0xff)
            COLOR_ALPHA(pixel_color) = 0xff;
    }
    
    COLOR_ALPHA(shade_color) += adseed;
    if (COLOR_ALPHA(shade_color) & 0x100)
        COLOR_ALPHA(shade_color) = 0xff;
}

static void combiner_2cycle(int adseed, uint32_t* curpixel_cvg, int32_t* acalpha)
{
    int32_t temp_combined_color[3];
    int32_t redkey, greenkey, bluekey, temp;
    COLOR chromabypass;
    int32_t keyalpha;

    if (other_modes.key_en)
    {
       COLOR_RED(chromabypass)   = *combiner_rgbsub_a_r[1];
       COLOR_GREEN(chromabypass) = *combiner_rgbsub_a_g[1];
       COLOR_BLUE(chromabypass)  = *combiner_rgbsub_a_b[1];
    }

    temp_combined_color[0] = color_combiner_equation(*combiner_rgbsub_a_r[0],*combiner_rgbsub_b_r[0],*combiner_rgbmul_r[0],*combiner_rgbadd_r[0]);
    temp_combined_color[1] = color_combiner_equation(*combiner_rgbsub_a_g[0],*combiner_rgbsub_b_g[0],*combiner_rgbmul_g[0],*combiner_rgbadd_g[0]);
    temp_combined_color[2] = color_combiner_equation(*combiner_rgbsub_a_b[0],*combiner_rgbsub_b_b[0],*combiner_rgbmul_b[0],*combiner_rgbadd_b[0]);
    COLOR_ALPHA(combined_color) = alpha_combiner_equation(*combiner_alphasub_a[0],*combiner_alphasub_b[0],*combiner_alphamul[0],*combiner_alphaadd[0]);

    if (other_modes.alpha_compare_en)
    {
        int32_t preacalpha;
        if (other_modes.key_en)
        {
            redkey = SIGN(temp_combined_color[0], 17);
            if (redkey >= 0)
                redkey = (COLOR_RED(key_width) << 4) - redkey;
            else
                redkey = (COLOR_RED(key_width) << 4) + redkey;
            greenkey = SIGN(temp_combined_color[1], 17);
            if (greenkey >= 0)
                greenkey = (COLOR_GREEN(key_width) << 4) - greenkey;
            else
                greenkey = (COLOR_GREEN(key_width) << 4) + greenkey;
            bluekey = SIGN(temp_combined_color[2], 17);
            if (bluekey >= 0)
                bluekey = (COLOR_BLUE(key_width) << 4) - bluekey;
            else
                bluekey = (COLOR_BLUE(key_width) << 4) + bluekey;
            keyalpha = (redkey < greenkey) ? redkey : greenkey;
            keyalpha = (bluekey < keyalpha) ? bluekey : keyalpha;
            keyalpha = CLIP(keyalpha, 0, 0xff);
        }

        preacalpha = special_9bit_clamptable[COLOR_ALPHA(combined_color)];
        if (preacalpha == 0xff)
            preacalpha = 0x100;

        if (other_modes.cvg_times_alpha)
            temp = (preacalpha * (*curpixel_cvg) + 4) >> 3;

        if (!other_modes.alpha_cvg_select)
        {
            if (!other_modes.key_en)
            {
                preacalpha += adseed;
                if (preacalpha & 0x100)
                    preacalpha = 0xff;
            }
            else
                preacalpha = keyalpha;
        }
        else
        {
            if (other_modes.cvg_times_alpha)
                preacalpha = temp;
            else
                preacalpha = (*curpixel_cvg) << 5;
            if (preacalpha > 0xff)
                preacalpha = 0xff;
        }

        *acalpha = preacalpha;
    }

    COLOR_RED(combined_color)   = temp_combined_color[0]  >> 8;
    COLOR_GREEN(combined_color) = temp_combined_color[1]  >> 8;
    COLOR_BLUE(combined_color)  = temp_combined_color[2]  >> 8;

    COLOR_ASSIGN(texel0_color, texel1_color);
    COLOR_ASSIGN(texel1_color, nexttexel_color);

    temp_combined_color[0]   = color_combiner_equation(*combiner_rgbsub_a_r[1],*combiner_rgbsub_b_r[1],*combiner_rgbmul_r[1],*combiner_rgbadd_r[1]);
    temp_combined_color[1] = color_combiner_equation(*combiner_rgbsub_a_g[1],*combiner_rgbsub_b_g[1],*combiner_rgbmul_g[1],*combiner_rgbadd_g[1]);
    temp_combined_color[2]  = color_combiner_equation(*combiner_rgbsub_a_b[1],*combiner_rgbsub_b_b[1],*combiner_rgbmul_b[1],*combiner_rgbadd_b[1]);
    COLOR_ALPHA(combined_color) = alpha_combiner_equation(*combiner_alphasub_a[1],*combiner_alphasub_b[1],*combiner_alphamul[1],*combiner_alphaadd[1]);

    if (!other_modes.key_en)
    {
        
        COLOR_RED(combined_color)   = temp_combined_color[0]   >> 8;
        COLOR_GREEN(combined_color) = temp_combined_color[1] >> 8;
        COLOR_BLUE(combined_color)  = temp_combined_color[2] >> 8;

        COLOR_RED(pixel_color)   = special_9bit_clamptable[COLOR_RED(combined_color)];
        COLOR_GREEN(pixel_color) = special_9bit_clamptable[COLOR_GREEN(combined_color)];
        COLOR_BLUE(pixel_color)  = special_9bit_clamptable[COLOR_BLUE(combined_color)];
    }
    else
    {
        redkey = SIGN(temp_combined_color[0], 17);
        if (redkey >= 0)
            redkey = (COLOR_RED(key_width) << 4) - redkey;
        else
            redkey = (COLOR_RED(key_width) << 4) + redkey;
        greenkey = SIGN(temp_combined_color[1], 17);
        if (greenkey >= 0)
            greenkey = (COLOR_GREEN(key_width) << 4) - greenkey;
        else
            greenkey = (COLOR_GREEN(key_width) << 4) + greenkey;
        bluekey = SIGN(temp_combined_color[2], 17);
        if (bluekey >= 0)
            bluekey = (COLOR_BLUE(key_width) << 4) - bluekey;
        else
            bluekey = (COLOR_BLUE(key_width) << 4) + bluekey;
        keyalpha = (redkey < greenkey) ? redkey : greenkey;
        keyalpha = (bluekey < keyalpha) ? bluekey : keyalpha;
        keyalpha = CLIP(keyalpha, 0, 0xff);

        COLOR_RED(pixel_color)   = special_9bit_clamptable[COLOR_RED(chromabypass)];
        COLOR_GREEN(pixel_color) = special_9bit_clamptable[COLOR_GREEN(chromabypass)];
        COLOR_BLUE(pixel_color)  = special_9bit_clamptable[COLOR_BLUE(chromabypass)];
        
        COLOR_RED(combined_color)   = temp_combined_color[0] >> 8;
        COLOR_GREEN(combined_color) = temp_combined_color[1] >> 8;
        COLOR_BLUE(combined_color)  = temp_combined_color[2] >> 8;
    }
    
    COLOR_ALPHA(pixel_color) = special_9bit_clamptable[COLOR_ALPHA(combined_color)];
    if (COLOR_ALPHA(pixel_color) == 0xff)
        COLOR_ALPHA(pixel_color) = 0x100;

    
    if (other_modes.cvg_times_alpha)
    {
        temp = (COLOR_ALPHA(pixel_color) * (*curpixel_cvg) + 4) >> 3;
        *curpixel_cvg = (temp >> 5) & 0xf;
    }

    if (!other_modes.alpha_cvg_select)
    {
        if (!other_modes.key_en)
        {
            COLOR_ALPHA(pixel_color) += adseed;
            if (COLOR_ALPHA(pixel_color) & 0x100)
                COLOR_ALPHA(pixel_color) = 0xff;
        }
        else
            COLOR_ALPHA(pixel_color) = keyalpha;
    }
    else
    {
        if (other_modes.cvg_times_alpha)
            COLOR_ALPHA(pixel_color) = temp;
        else
            COLOR_ALPHA(pixel_color) = (*curpixel_cvg) << 5;

        if (COLOR_ALPHA(pixel_color) > 0xff)
            COLOR_ALPHA(pixel_color) = 0xff;
    }
    

    COLOR_ALPHA(shade_color) += adseed;
    if (COLOR_ALPHA(shade_color) & 0x100)
        COLOR_ALPHA(shade_color) = 0xff;
}

static unsigned char bayer_matrix[16] = {
    00, 04, 01, 05,
    04, 00, 05, 01,
    03, 07, 02, 06,
    07, 03, 06, 02
};
static unsigned char magic_matrix[16] = {
    00, 06, 01, 07,
    04, 02, 05, 03,
    03, 05, 02, 04,
    07, 01, 06, 00
};

static void rgb_dither_complete(int* r, int* g, int* b, int dith)
{
   int32_t replacesign, ditherdiff;
   int32_t newr = *r, newg = *g, newb = *b;
	int32_t rcomp = dith, gcomp, bcomp;

	
	if (newr > 247)
		newr = 255;
	else
		newr = (newr & 0xf8) + 8;
	if (newg > 247)
		newg = 255;
	else
		newg = (newg & 0xf8) + 8;
	if (newb > 247)
		newb = 255;
	else
		newb = (newb & 0xf8) + 8;

	if (other_modes.rgb_dither_sel != 2)
		gcomp = bcomp = dith;
	else
	{
		gcomp = (dith + 3) & 7;
		bcomp = (dith + 5) & 7;
	}

	replacesign = (rcomp - (*r & 7)) >> 31;
	ditherdiff = newr - *r;

	*r = *r + (ditherdiff & replacesign);

	replacesign = (gcomp - (*g & 7)) >> 31;
	ditherdiff = newg - *g;
	*g = *g + (ditherdiff & replacesign);

	replacesign = (bcomp - (*b & 7)) >> 31;
	ditherdiff = newb - *b;
	*b = *b + (ditherdiff & replacesign);
}

static void blender_equation_cycle0(int* r, int* g, int* b)
{
    int blend1a, blend2a;
    int blr, blg, blb, sum;
    int mulb;

    blend1a = *blender1b_a[0] >> 3;
    blend2a = *blender2b_a[0] >> 3;

    LOG("blend1a = %d, blend2a = %d\n", blend1a, blend2a);

    if (other_modes.f.special_bsel0)
    {
        blend1a = (blend1a >> blshifta) & 0x3C;
        blend2a = (blend2a >> blshiftb) | 3;
    }
    mulb = blend2a + 1;

    blr = (*blender1a_r[0]) * blend1a + (*blender2a_r[0]) * mulb;
    blg = (*blender1a_g[0]) * blend1a + (*blender2a_g[0]) * mulb;
    blb = (*blender1a_b[0]) * blend1a + (*blender2a_b[0]) * mulb;

    if (!other_modes.force_blend)
    {
        sum = ((blend1a & ~3) + (blend2a & ~3) + 4) << 9;
        *r = bldiv_hwaccurate_table[sum | ((blr >> 2) & 0x7ff)];
        *g = bldiv_hwaccurate_table[sum | ((blg >> 2) & 0x7ff)];
        *b = bldiv_hwaccurate_table[sum | ((blb >> 2) & 0x7ff)];
    }
    else
    {
        *r = (blr >> 5) & 0xff;    
        *g = (blg >> 5) & 0xff; 
        *b = (blb >> 5) & 0xff;
    }    
}

static STRICTINLINE void blender_equation_cycle0_2(int* r, int* g, int* b)
{
    int blend1a, blend2a;
    blend1a = *blender1b_a[0] >> 3;
    blend2a = *blender2b_a[0] >> 3;

    if (other_modes.f.special_bsel0)
    {
        blend1a = (blend1a >> pastblshifta) & 0x3C;
        blend2a = (blend2a >> pastblshiftb) | 3;
    }
    blend2a += 1;
    *r = (((*blender1a_r[0]) * blend1a + (*blender2a_r[0]) * blend2a) >> 5) & 0xff;
    *g = (((*blender1a_g[0]) * blend1a + (*blender2a_g[0]) * blend2a) >> 5) & 0xff;
    *b = (((*blender1a_b[0]) * blend1a + (*blender2a_b[0]) * blend2a) >> 5) & 0xff;
}

static void blender_equation_cycle1(int* r, int* g, int* b)
{
    int blend1a, blend2a;
    int blr, blg, blb, sum;
    int mulb;

    blend1a = *blender1b_a[1] >> 3;
    blend2a = *blender2b_a[1] >> 3;

    if (other_modes.f.special_bsel1)
    {
        blend1a = (blend1a >> blshifta) & 0x3C;
        blend2a = (blend2a >> blshiftb) | 3;
    }
    mulb = blend2a + 1;
    blr = (*blender1a_r[1]) * blend1a + (*blender2a_r[1]) * mulb;
    blg = (*blender1a_g[1]) * blend1a + (*blender2a_g[1]) * mulb;
    blb = (*blender1a_b[1]) * blend1a + (*blender2a_b[1]) * mulb;

    if (!other_modes.force_blend)
    {
        sum = ((blend1a & ~3) + (blend2a & ~3) + 4) << 9;
        *r = bldiv_hwaccurate_table[sum | ((blr >> 2) & 0x7ff)];
        *g = bldiv_hwaccurate_table[sum | ((blg >> 2) & 0x7ff)];
        *b = bldiv_hwaccurate_table[sum | ((blb >> 2) & 0x7ff)];
    }
    else
    {
        *r = (blr >> 5) & 0xff;    
        *g = (blg >> 5) & 0xff; 
        *b = (blb >> 5) & 0xff;
    }
}

static int blender_1cycle(uint32_t* fr, uint32_t* fg, uint32_t* fb, int dith, uint32_t blend_en, uint32_t prewrap, uint32_t curpixel_cvg, uint32_t curpixel_cvbit)
{
    int r, g, b, dontblend;
    
    
    if (alpha_compare(COLOR_ALPHA(pixel_color)))
    {

        

        
        
        
        if (other_modes.antialias_en ? (curpixel_cvg) : (curpixel_cvbit))
        {

            if (!other_modes.color_on_cvg || prewrap)
            {
                dontblend = (other_modes.f.partialreject_1cycle && COLOR_ALPHA(pixel_color) >= 0xff);
                if (!blend_en || dontblend)
                {
                    r = *blender1a_r[0];
                    g = *blender1a_g[0];
                    b = *blender1a_b[0];
                }
                else
                {
                    COLOR_ALPHA(inv_pixel_color) =  (~(*blender1b_a[0])) & 0xff;
                    
                    
                    
                    

                    blender_equation_cycle0(&r, &g, &b);
                }
            }
            else
            {
                r = *blender2a_r[0];
                g = *blender2a_g[0];
                b = *blender2a_b[0];
            }

            if (other_modes.rgb_dither_sel != 3)
               rgb_dither_complete(&r, &g, &b, dith);

            *fr = r;
            *fg = g;
            *fb = b;

            return 1;
        }
        else 
            return 0;
        }
    else 
        return 0;
}

int blender_2cycle(uint32_t* fr, uint32_t* fg, uint32_t* fb, int dith, uint32_t blend_en, uint32_t prewrap, uint32_t curpixel_cvg, uint32_t curpixel_cvbit, int32_t acalpha)
{
    int r, g, b, dontblend;

    
    if (alpha_compare(acalpha))
    {
        if (other_modes.antialias_en ? (curpixel_cvg) : (curpixel_cvbit))
        {
            COLOR_ALPHA(inv_pixel_color) =  (~(*blender1b_a[0])) & 0xff;

            blender_equation_cycle0_2(&r, &g, &b);
            
            COLOR_ASSIGN(memory_color, pre_memory_color);

            COLOR_RED(blended_pixel_color)   = r;
            COLOR_GREEN(blended_pixel_color) = g;
            COLOR_BLUE(blended_pixel_color)  = b;
            COLOR_ALPHA(blended_pixel_color) = COLOR_ALPHA(pixel_color);

            if (!other_modes.color_on_cvg || prewrap)
            {
                dontblend = (other_modes.f.partialreject_2cycle && COLOR_ALPHA(pixel_color) >= 0xff);
                if (!blend_en || dontblend)
                {
                    r = *blender1a_r[1];
                    g = *blender1a_g[1];
                    b = *blender1a_b[1];
                }
                else
                {
                    COLOR_ALPHA(inv_pixel_color) =  (~(*blender1b_a[1])) & 0xff;
                    blender_equation_cycle1(&r, &g, &b);
                }
            }
            else
            {
                r = *blender2a_r[1];
                g = *blender2a_g[1];
                b = *blender2a_b[1];
            }

            
            if (other_modes.rgb_dither_sel != 3)
               rgb_dither_complete(&r, &g, &b, dith);

            *fr = r;
            *fg = g;
            *fb = b;
            return 1;
        }
        else 
        {
           COLOR_ASSIGN(memory_color, pre_memory_color);
           return 0;
        }
    }
    else 
    {
       COLOR_ASSIGN(memory_color, pre_memory_color);
        return 0;
    }
}

static void fetch_texel(COLOR *color, int s, int t, uint32_t tilenum)
{
    uint32_t tbase = tile[tilenum].line * t + tile[tilenum].tmem;
    

    uint32_t tpal    = tile[tilenum].palette;

    
    
    
    
    
    
    
    uint16_t *tc16 = (uint16_t*)__TMEM;
    uint32_t taddr = 0;

    

    

    switch (tile[tilenum].f.notlutswitch)
    {
    case TEXEL_RGBA4:
        {
            uint8_t byteval, c; 
            taddr = ((tbase << 4) + s) >> 1;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);

            byteval = __TMEM[taddr & 0xfff];
            c = ((s & 1)) ? (byteval & 0xf) : (byteval >> 4);
            c |= (c << 4);
            COLOR_RED_PTR(color) = c;
            COLOR_GREEN_PTR(color) = c;
            COLOR_BLUE_PTR(color) = c;
            COLOR_ALPHA_PTR(color) = c;
        }
        break;
    case TEXEL_RGBA8:
        {
            uint8_t p;

            taddr = (tbase << 3) + s;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);

            p = __TMEM[taddr & 0xfff];
            COLOR_RED_PTR(color) = p;
            COLOR_GREEN_PTR(color) = p;
            COLOR_BLUE_PTR(color) = p;
            COLOR_ALPHA_PTR(color) = p;
        }
        break;
    case TEXEL_RGBA16:
        {         
            uint16_t c;

            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);

            c = tc16[taddr & 0x7ff];
            COLOR_RED_PTR(color) = GET_HI_RGBA16_TMEM(c);
            COLOR_GREEN_PTR(color) = GET_MED_RGBA16_TMEM(c);
            COLOR_BLUE_PTR(color) = GET_LOW_RGBA16_TMEM(c);
            COLOR_ALPHA_PTR(color) = (c & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_RGBA32:
        {
            uint16_t c;

            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);

            taddr &= 0x3ff;
            c = tc16[taddr];
            COLOR_RED_PTR(color) = c >> 8;
            COLOR_GREEN_PTR(color) = c & 0xff;
            c = tc16[taddr + 0x400];
            COLOR_BLUE_PTR(color) = c >> 8;
            COLOR_ALPHA_PTR(color) = c & 0xff;
        }
        break;
    case TEXEL_YUV4:
    case TEXEL_YUV8:
        {
            int32_t u, save;

            taddr = (tbase << 3) + s;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
                    
            save = u = __TMEM[taddr & 0x7ff];

            u = (u - 0x80) & 0x1ff;

            COLOR_RED_PTR(color) = u;
            COLOR_GREEN_PTR(color) = u;
            COLOR_BLUE_PTR(color) = save;
            COLOR_ALPHA_PTR(color) = save;
        }
        break;
    case TEXEL_YUV16:
    case TEXEL_YUV32:
        {
            uint16_t c;
            int32_t y, u, v;

            taddr = (tbase << 3) + s;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
            taddr &= 0x7ff;
			c = tc16[taddr >> 1];
            y = __TMEM[taddr + 0x800];
            u = c >> 8;
            v = c & 0xff;

            u = (u - 0x80) & 0x1ff;
            v = (v - 0x80) & 0x1ff;

            COLOR_RED_PTR(color) = u;
            COLOR_GREEN_PTR(color) = v;
            COLOR_BLUE_PTR(color) = y;
            COLOR_ALPHA_PTR(color) = y;
        }
        break;
    case TEXEL_CI4:
        {
            uint8_t p;

            taddr = ((tbase << 4) + s) >> 1;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);

            p = __TMEM[taddr & 0xfff];
            p = (s & 1) ? (p & 0xf) : (p >> 4);
            p = (tpal << 4) | p;
            COLOR_RED_PTR(color) = COLOR_GREEN_PTR(color) = COLOR_BLUE_PTR(color) = COLOR_ALPHA_PTR(color) = p;
        }
        break;
    case TEXEL_CI8:
        {
            uint8_t p;

            taddr = (tbase << 3) + s;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);

            p = __TMEM[taddr & 0xfff];
            COLOR_RED_PTR(color) = p;
            COLOR_GREEN_PTR(color) = p;
            COLOR_BLUE_PTR(color) = p;
            COLOR_ALPHA_PTR(color) = p;
        }
        break;
    case TEXEL_CI16:
        {         
            uint16_t c;

            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);

            c = tc16[taddr & 0x7ff];
            COLOR_RED_PTR(color) = c >> 8;
            COLOR_GREEN_PTR(color) = c & 0xff;
            COLOR_BLUE_PTR(color) = COLOR_RED_PTR(color);
            COLOR_ALPHA_PTR(color) = (c & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_CI32:
        {
            uint16_t c;

            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);
            c = tc16[taddr & 0x7ff];
            COLOR_RED_PTR(color) = c >> 8;
            COLOR_GREEN_PTR(color) = c & 0xff;
            COLOR_BLUE_PTR(color) = COLOR_RED_PTR(color);
            COLOR_ALPHA_PTR(color) = (c & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_IA4:
        {
            uint8_t p, i;

            taddr = ((tbase << 4) + s) >> 1;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
            p = __TMEM[taddr & 0xfff];
            p = (s & 1) ? (p & 0xf) : (p >> 4);
            i = p & 0xe;
            i = (i << 4) | (i << 1) | (i >> 2);
            COLOR_RED_PTR(color) = i;
            COLOR_GREEN_PTR(color) = i;
            COLOR_BLUE_PTR(color) = i;
            COLOR_ALPHA_PTR(color) = (p & 0x1) ? 0xff : 0;
        }
        break;
    case TEXEL_IA8:
        {
            uint8_t p, i;

            taddr = (tbase << 3) + s;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
            p = __TMEM[taddr & 0xfff];
            i = p & 0xf0;
            i |= (i >> 4);
            COLOR_RED_PTR(color) = i;
            COLOR_GREEN_PTR(color) = i;
            COLOR_BLUE_PTR(color) = i;
            COLOR_ALPHA_PTR(color) = ((p & 0xf) << 4) | (p & 0xf);
        }
        break;
    case TEXEL_IA16:
        {
            uint16_t c;

            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);                         
            c = tc16[taddr & 0x7ff];
            COLOR_RED_PTR(color) = COLOR_GREEN_PTR(color) = COLOR_BLUE_PTR(color) = (c >> 8);
            COLOR_ALPHA_PTR(color) = c & 0xff;
        }
        break;
    case TEXEL_IA32:
        {
            uint16_t c;

            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);
            c = tc16[taddr & 0x7ff];
            COLOR_RED_PTR(color) = c >> 8;
            COLOR_GREEN_PTR(color) = c & 0xff;
            COLOR_BLUE_PTR(color) = COLOR_RED_PTR(color);
            COLOR_ALPHA_PTR(color) = (c & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_I4:
        {
            uint8_t byteval, c;

            taddr = ((tbase << 4) + s) >> 1;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
            byteval = __TMEM[taddr & 0xfff];
            c = (s & 1) ? (byteval & 0xf) : (byteval >> 4);
            c |= (c << 4);
            COLOR_RED_PTR(color) = c;
            COLOR_GREEN_PTR(color) = c;
            COLOR_BLUE_PTR(color) = c;
            COLOR_ALPHA_PTR(color) = c;
        }
        break;
    case TEXEL_I8:
        {
            uint8_t c;

            taddr = (tbase << 3) + s;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
            c = __TMEM[taddr & 0xfff];
            COLOR_RED_PTR(color) = c;
            COLOR_GREEN_PTR(color) = c;
            COLOR_BLUE_PTR(color) = c;
            COLOR_ALPHA_PTR(color) = c;
        }
        break;
    case TEXEL_I16:
        {        
            uint16_t c;

            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);    
            c = tc16[taddr & 0x7ff];
            COLOR_RED_PTR(color) = c >> 8;
            COLOR_GREEN_PTR(color) = c & 0xff;
            COLOR_BLUE_PTR(color) = COLOR_RED_PTR(color);
            COLOR_ALPHA_PTR(color) = (c & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_I32:
        {
            uint16_t c;

            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);   
            c = tc16[taddr & 0x7ff];
            COLOR_RED_PTR(color) = c >> 8;
            COLOR_GREEN_PTR(color) = c & 0xff;
            COLOR_BLUE_PTR(color) = COLOR_RED_PTR(color);
            COLOR_ALPHA_PTR(color) = (c & 1) ? 0xff : 0;
        }
        break;
    }
}

static void fetch_texel_entlut(COLOR *color, int s, int t, uint32_t tilenum)
{
    uint32_t tbase = tile[tilenum].line * t + tile[tilenum].tmem;
    uint32_t tpal    = tile[tilenum].palette << 4;
    uint16_t *tc16 = (uint16_t*)__TMEM;
    uint32_t taddr = 0;
    uint32_t c;

    
    
    switch(tile[tilenum].f.tlutswitch)
    {
    case 0:
    case 1:
    case 2:
        {
            taddr = ((tbase << 4) + s) >> 1;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
            c = __TMEM[taddr & 0x7ff];
            c = (s & 1) ? (c & 0xf) : (c >> 4);
#ifdef EXTRALOGGING
            fprintf(stderr, "TPAL: %u\n", tpal);
#endif
            c = tlut[((tpal + c) << 2) + WORD_ADDR_XOR];
        }
        break;
    case 3:
        {
            taddr = (tbase << 3) + s;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
            c = __TMEM[taddr & 0x7ff];
            c = (s & 1) ? (c & 0xf) : (c >> 4);
            c = tlut[((tpal + c) << 2) + WORD_ADDR_XOR];
        }
        break;
    case 4:
    case 5:
    case 6:
    case 7:
    case 11:
    case 15:
        {
            taddr = (tbase << 3) + s;
            taddr ^= ((t & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR);
            c = __TMEM[taddr & 0x7ff];
            c = tlut[(c << 2) + WORD_ADDR_XOR];
        }
        break;
    case 8:
    case 9:
    case 10:
    case 12:
    case 13:
    case 14:
        {
            taddr = (tbase << 2) + s;
            taddr ^= ((t & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR);
            c = tc16[taddr & 0x3ff];
            c = tlut[((c >> 6) & ~3) + WORD_ADDR_XOR];
        }
        break;
    }

    if (!other_modes.tlut_type)
    {
        COLOR_RED_PTR(color) = GET_HI_RGBA16_TMEM(c);
        COLOR_GREEN_PTR(color) = GET_MED_RGBA16_TMEM(c);
        COLOR_BLUE_PTR(color) = GET_LOW_RGBA16_TMEM(c);
        COLOR_ALPHA_PTR(color) = (c & 1) ? 0xff : 0;
    }
    else
    {
        COLOR_RED_PTR(color) = COLOR_GREEN_PTR(color) = COLOR_BLUE_PTR(color) = c >> 8;
        COLOR_ALPHA_PTR(color) = c & 0xff;
    }

}

#define fetch_texel_quadro_rgba16(color0, color1, color2, color3, c0, c1, c2, c3) \
   COLOR_RED_PTR(color0) = GET_HI_RGBA16_TMEM(c0); \
   COLOR_GREEN_PTR(color0) = GET_MED_RGBA16_TMEM(c0); \
   COLOR_BLUE_PTR(color0) = GET_LOW_RGBA16_TMEM(c0); \
   COLOR_ALPHA_PTR(color0) = (c0 & 1) ? 0xff : 0; \
   COLOR_RED_PTR(color1) = GET_HI_RGBA16_TMEM(c1); \
   COLOR_GREEN_PTR(color1) = GET_MED_RGBA16_TMEM(c1); \
   COLOR_BLUE_PTR(color1) = GET_LOW_RGBA16_TMEM(c1); \
   COLOR_ALPHA_PTR(color1) = (c1 & 1) ? 0xff : 0; \
   COLOR_RED_PTR(color2) = GET_HI_RGBA16_TMEM(c2); \
   COLOR_GREEN_PTR(color2) = GET_MED_RGBA16_TMEM(c2); \
   COLOR_BLUE_PTR(color2) = GET_LOW_RGBA16_TMEM(c2); \
   COLOR_ALPHA_PTR(color2) = (c2 & 1) ? 0xff : 0; \
   COLOR_RED_PTR(color3) = GET_HI_RGBA16_TMEM(c3); \
   COLOR_GREEN_PTR(color3) = GET_MED_RGBA16_TMEM(c3); \
   COLOR_BLUE_PTR(color3) = GET_LOW_RGBA16_TMEM(c3); \
   COLOR_ALPHA_PTR(color3) = (c3 & 1) ? 0xff : 0


static void fetch_texel_quadro(COLOR *color0, COLOR *color1, COLOR *color2, COLOR *color3, int s0, int s1, int t0, int t1, uint32_t tilenum)
{

    uint32_t tbase0 = tile[tilenum].line * t0 + tile[tilenum].tmem;
    uint32_t tbase2 = tile[tilenum].line * t1 + tile[tilenum].tmem;
    uint32_t tpal    = tile[tilenum].palette;
    uint32_t xort = 0, ands = 0;

    
    

    uint16_t *tc16 = (uint16_t*)__TMEM;
    uint32_t taddr0 = 0, taddr1 = 0, taddr2 = 0, taddr3 = 0;

    switch (tile[tilenum].f.notlutswitch)
    {
    case TEXEL_RGBA4:
        {
            uint32_t byteval, c;

            taddr0 = ((tbase0 << 4) + s0) >> 1;
            taddr1 = ((tbase0 << 4) + s1) >> 1;
            taddr2 = ((tbase2 << 4) + s0) >> 1;
            taddr3 = ((tbase2 << 4) + s1) >> 1;
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0xfff;
            taddr1 &= 0xfff;
            taddr2 &= 0xfff;
            taddr3 &= 0xfff;
            ands = s0 & 1;
            byteval = __TMEM[taddr0];
            c = (ands) ? (byteval & 0xf) : (byteval >> 4);
            c |= (c << 4);
            COLOR_RED_PTR(color0) = c;
            COLOR_GREEN_PTR(color0) = c;
            COLOR_BLUE_PTR(color0) = c;
            COLOR_ALPHA_PTR(color0) = c;
            byteval = __TMEM[taddr2];
            c = (ands) ? (byteval & 0xf) : (byteval >> 4);
            c |= (c << 4);
            COLOR_RED_PTR(color2) = c;
            COLOR_GREEN_PTR(color2) = c;
            COLOR_BLUE_PTR(color2) = c;
            COLOR_ALPHA_PTR(color2) = c;

            ands = s1 & 1;
            byteval = __TMEM[taddr1];
            c = (ands) ? (byteval & 0xf) : (byteval >> 4);
            c |= (c << 4);
            COLOR_RED_PTR(color1) = c;
            COLOR_GREEN_PTR(color1) = c;
            COLOR_BLUE_PTR(color1) = c;
            COLOR_ALPHA_PTR(color1) = c;
            byteval = __TMEM[taddr3];
            c = (ands) ? (byteval & 0xf) : (byteval >> 4);
            c |= (c << 4);
            COLOR_RED_PTR(color3) = c;
            COLOR_GREEN_PTR(color3) = c;
            COLOR_BLUE_PTR(color3) = c;
            COLOR_ALPHA_PTR(color3) = c;
        }
        break;
    case TEXEL_RGBA8:
        {
            uint32_t p;

            taddr0 = ((tbase0 << 3) + s0);
            taddr1 = ((tbase0 << 3) + s1);
            taddr2 = ((tbase2 << 3) + s0);
            taddr3 = ((tbase2 << 3) + s1);
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0xfff;
            taddr1 &= 0xfff;
            taddr2 &= 0xfff;
            taddr3 &= 0xfff;
            p = __TMEM[taddr0];
            COLOR_RED_PTR(color0) = p;
            COLOR_GREEN_PTR(color0) = p;
            COLOR_BLUE_PTR(color0) = p;
            COLOR_ALPHA_PTR(color0) = p;
            p = __TMEM[taddr2];
            COLOR_RED_PTR(color2) = p;
            COLOR_GREEN_PTR(color2) = p;
            COLOR_BLUE_PTR(color2) = p;
            COLOR_ALPHA_PTR(color2) = p;
            p = __TMEM[taddr1];
            COLOR_RED_PTR(color1) = p;
            COLOR_GREEN_PTR(color1) = p;
            COLOR_BLUE_PTR(color1) = p;
            COLOR_ALPHA_PTR(color1) = p;
            p = __TMEM[taddr3];
            COLOR_RED_PTR(color3) = p;
            COLOR_GREEN_PTR(color3) = p;
            COLOR_BLUE_PTR(color3) = p;
            COLOR_ALPHA_PTR(color3) = p;
        }
        break;
    case TEXEL_RGBA16:
        {
            uint32_t c0, c1, c2, c3;

            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0x7ff;
            taddr1 &= 0x7ff;
            taddr2 &= 0x7ff;
            taddr3 &= 0x7ff;
            c0 = tc16[taddr0];
            c1 = tc16[taddr1];
            c2 = tc16[taddr2];
            c3 = tc16[taddr3];
            fetch_texel_quadro_rgba16(color0, color1, color2, color3, c0, c1, c2, c3);
        }
        break;
    case TEXEL_RGBA32:
        {
            uint16_t c0, c1, c2, c3;

            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0x3ff;
            taddr1 &= 0x3ff;
            taddr2 &= 0x3ff;
            taddr3 &= 0x3ff;
            c0 = tc16[taddr0];
            COLOR_RED_PTR(color0) = c0 >> 8;
            COLOR_GREEN_PTR(color0) = c0 & 0xff;
            c0 = tc16[taddr0 + 0x400];
            COLOR_BLUE_PTR(color0) = c0 >>  8;
            COLOR_ALPHA_PTR(color0) = c0 & 0xff;
            c1 = tc16[taddr1];
            COLOR_RED_PTR(color1) = c1 >> 8;
            COLOR_GREEN_PTR(color1) = c1 & 0xff;
            c1 = tc16[taddr1 + 0x400];
            COLOR_BLUE_PTR(color1) = c1 >>  8;
            COLOR_ALPHA_PTR(color1) = c1 & 0xff;
            c2 = tc16[taddr2];
            COLOR_RED_PTR(color2) = c2 >> 8;
            COLOR_GREEN_PTR(color2) = c2 & 0xff;
            c2 = tc16[taddr2 + 0x400];
            COLOR_BLUE_PTR(color2) = c2 >>  8;
            COLOR_ALPHA_PTR(color2) = c2 & 0xff;
            c3 = tc16[taddr3];
            COLOR_RED_PTR(color3) = c3 >> 8;
            COLOR_GREEN_PTR(color3) = c3 & 0xff;
            c3 = tc16[taddr3 + 0x400];
            COLOR_BLUE_PTR(color3) = c3 >>  8;
            COLOR_ALPHA_PTR(color3) = c3 & 0xff;
        }
        break;
    case TEXEL_YUV4:
    case TEXEL_YUV8:
        {
            int32_t u0, u1, u2, u3, save0, save1, save2, save3;

            taddr0 = (tbase0 << 3) + s0;
            taddr1 = (tbase0 << 3) + s1;
            taddr2 = (tbase2 << 3) + s0;
            taddr3 = (tbase2 << 3) + s1;

            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;

            save0 = u0 = __TMEM[taddr0 & 0x7ff];
            u0 = u0 - 0x80;
            save1 = u1 = __TMEM[taddr1 & 0x7ff];
            u1 = u1 - 0x80;
            save2 = u2 = __TMEM[taddr2 & 0x7ff];
            u2 = u2 - 0x80;
            save3 = u3 = __TMEM[taddr3 & 0x7ff];
            u3 = u3 - 0x80;

            COLOR_RED_PTR(color0) = u0;
            COLOR_GREEN_PTR(color0) = u0;
            COLOR_BLUE_PTR(color0) = save0;
            COLOR_ALPHA_PTR(color0) = save0;
            COLOR_RED_PTR(color1) = u1;
            COLOR_GREEN_PTR(color1) = u1;
            COLOR_BLUE_PTR(color1) = save1;
            COLOR_ALPHA_PTR(color1) = save1;
            COLOR_RED_PTR(color2) = u2;
            COLOR_GREEN_PTR(color2) = u2;
            COLOR_BLUE_PTR(color2) = save2;
            COLOR_ALPHA_PTR(color2) = save2;
            COLOR_RED_PTR(color3) = u3;
            COLOR_GREEN_PTR(color3) = u3;
            COLOR_BLUE_PTR(color3) = save3;
            COLOR_ALPHA_PTR(color3) = save3;
        }
        break;
    case TEXEL_YUV16:
    case TEXEL_YUV32:
        {
            uint16_t c0, c1, c2, c3;
            int32_t y0, y1, y2, y3, u0, u1, u2, u3, v0, v1, v2, v3;

            taddr0 = ((tbase0 << 3) + s0);
            taddr1 = ((tbase0 << 3) + s1);
            taddr2 = ((tbase2 << 3) + s0);
            taddr3 = ((tbase2 << 3) + s1);

            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;

            taddr0 &= 0x7ff;
            taddr1 &= 0x7ff;
            taddr2 &= 0x7ff;
            taddr3 &= 0x7ff;

            c0 = tc16[taddr0 >> 1];
            c1 = tc16[taddr1 >> 1];
            c2 = tc16[taddr2 >> 1];
            c3 = tc16[taddr3 >> 1];                    
            
            y0 = __TMEM[taddr0 + 0x800];
            u0 = c0 >> 8;
            v0 = c0 & 0xff;
            y1 = __TMEM[taddr1 + 0x800];
            u1 = c1 >> 8;
            v1 = c1 & 0xff;
            y2 = __TMEM[taddr2 + 0x800];
            u2 = c2 >> 8;
            v2 = c2 & 0xff;
            y3 = __TMEM[taddr3 + 0x800];
            u3 = c3 >> 8;
            v3 = c3 & 0xff;

            u0 = u0 - 0x80;
            v0 = v0 - 0x80;
            u1 = u1 - 0x80;
            v1 = v1 - 0x80;
            u2 = u2 - 0x80;
            v2 = v2 - 0x80;
            u3 = u3 - 0x80;
            v3 = v3 - 0x80;

            COLOR_RED_PTR(color0) = u0;
            COLOR_GREEN_PTR(color0) = v0;
            COLOR_BLUE_PTR(color0) = y0;
            COLOR_ALPHA_PTR(color0) = y0;
            COLOR_RED_PTR(color1) = u1;
            COLOR_GREEN_PTR(color1) = v1;
            COLOR_BLUE_PTR(color1) = y1;
            COLOR_ALPHA_PTR(color1) = y1;
            COLOR_RED_PTR(color2) = u2;
            COLOR_GREEN_PTR(color2) = v2;
            COLOR_BLUE_PTR(color2) = y2;
            COLOR_ALPHA_PTR(color2) = y2;
            COLOR_RED_PTR(color3) = u3;
            COLOR_GREEN_PTR(color3) = v3;
            COLOR_BLUE_PTR(color3) = y3;
            COLOR_ALPHA_PTR(color3) = y3;
        }
        break;
    case TEXEL_CI4:
        {
            uint32_t p;

            taddr0 = ((tbase0 << 4) + s0) >> 1;
            taddr1 = ((tbase0 << 4) + s1) >> 1;
            taddr2 = ((tbase2 << 4) + s0) >> 1;
            taddr3 = ((tbase2 << 4) + s1) >> 1;
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0xfff;
            taddr1 &= 0xfff;
            taddr2 &= 0xfff;
            taddr3 &= 0xfff;
            ands = s0 & 1;
            p = __TMEM[taddr0];
            p = (ands) ? (p & 0xf) : (p >> 4);
            p = (tpal << 4) | p;
            COLOR_RED_PTR(color0) = COLOR_GREEN_PTR(color0) = COLOR_BLUE_PTR(color0) = COLOR_ALPHA_PTR(color0) = p;
            p = __TMEM[taddr2];
            p = (ands) ? (p & 0xf) : (p >> 4);
            p = (tpal << 4) | p;
            COLOR_RED_PTR(color2) = COLOR_GREEN_PTR(color2) = COLOR_BLUE_PTR(color2) = COLOR_ALPHA_PTR(color2) = p;

            ands = s1 & 1;
            p = __TMEM[taddr1];
            p = (ands) ? (p & 0xf) : (p >> 4);
            p = (tpal << 4) | p;
            COLOR_RED_PTR(color1) = COLOR_GREEN_PTR(color1) = COLOR_BLUE_PTR(color1) = COLOR_ALPHA_PTR(color1) = p;
            p = __TMEM[taddr3];
            p = (ands) ? (p & 0xf) : (p >> 4);
            p = (tpal << 4) | p;
            COLOR_RED_PTR(color3) = COLOR_GREEN_PTR(color3) = COLOR_BLUE_PTR(color3) = COLOR_ALPHA_PTR(color3) = p;
        }
        break;
    case TEXEL_CI8:
        {
            uint32_t p;

            taddr0 = ((tbase0 << 3) + s0);
            taddr1 = ((tbase0 << 3) + s1);
            taddr2 = ((tbase2 << 3) + s0);
            taddr3 = ((tbase2 << 3) + s1);
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;

            taddr0 &= 0xfff;
            taddr1 &= 0xfff;
            taddr2 &= 0xfff;
            taddr3 &= 0xfff;
            p = __TMEM[taddr0];
            COLOR_RED_PTR(color0) = p;
            COLOR_GREEN_PTR(color0) = p;
            COLOR_BLUE_PTR(color0) = p;
            COLOR_ALPHA_PTR(color0) = p;
            p = __TMEM[taddr2];
            COLOR_RED_PTR(color2) = p;
            COLOR_GREEN_PTR(color2) = p;
            COLOR_BLUE_PTR(color2) = p;
            COLOR_ALPHA_PTR(color2) = p;
            p = __TMEM[taddr1];
            COLOR_RED_PTR(color1) = p;
            COLOR_GREEN_PTR(color1) = p;
            COLOR_BLUE_PTR(color1) = p;
            COLOR_ALPHA_PTR(color1) = p;
            p = __TMEM[taddr3];
            COLOR_RED_PTR(color3) = p;
            COLOR_GREEN_PTR(color3) = p;
            COLOR_BLUE_PTR(color3) = p;
            COLOR_ALPHA_PTR(color3) = p;
        }
        break;
    case TEXEL_CI16:
        {
            uint16_t c0, c1, c2, c3;

            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0x7ff;
            taddr1 &= 0x7ff;
            taddr2 &= 0x7ff;
            taddr3 &= 0x7ff;
            c0 = tc16[taddr0];
            COLOR_RED_PTR(color0) = c0 >> 8;
            COLOR_GREEN_PTR(color0) = c0 & 0xff;
            COLOR_BLUE_PTR(color0) = c0 >> 8;
            COLOR_ALPHA_PTR(color0) = (c0 & 1) ? 0xff : 0;
            c1 = tc16[taddr1];
            COLOR_RED_PTR(color1) = c1 >> 8;
            COLOR_GREEN_PTR(color1) = c1 & 0xff;
            COLOR_BLUE_PTR(color1) = c1 >> 8;
            COLOR_ALPHA_PTR(color1) = (c1 & 1) ? 0xff : 0;
            c2 = tc16[taddr2];
            COLOR_RED_PTR(color2) = c2 >> 8;
            COLOR_GREEN_PTR(color2) = c2 & 0xff;
            COLOR_BLUE_PTR(color2) = c2 >> 8;
            COLOR_ALPHA_PTR(color2) = (c2 & 1) ? 0xff : 0;
            c3 = tc16[taddr3];
            COLOR_RED_PTR(color3) = c3 >> 8;
            COLOR_GREEN_PTR(color3) = c3 & 0xff;
            COLOR_BLUE_PTR(color3) = c3 >> 8;
            COLOR_ALPHA_PTR(color3) = (c3 & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_CI32:
        {
            uint16_t c0, c1, c2, c3;

            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0x7ff;
            taddr1 &= 0x7ff;
            taddr2 &= 0x7ff;
            taddr3 &= 0x7ff;
            c0 = tc16[taddr0];
            COLOR_RED_PTR(color0) = c0 >> 8;
            COLOR_GREEN_PTR(color0) = c0 & 0xff;
            COLOR_BLUE_PTR(color0) = c0 >> 8;
            COLOR_ALPHA_PTR(color0) = (c0 & 1) ? 0xff : 0;
            c1 = tc16[taddr1];
            COLOR_RED_PTR(color1) = c1 >> 8;
            COLOR_GREEN_PTR(color1) = c1 & 0xff;
            COLOR_BLUE_PTR(color1) = c1 >> 8;
            COLOR_ALPHA_PTR(color1) = (c1 & 1) ? 0xff : 0;
            c2 = tc16[taddr2];
            COLOR_RED_PTR(color2) = c2 >> 8;
            COLOR_GREEN_PTR(color2) = c2 & 0xff;
            COLOR_BLUE_PTR(color2) = c2 >> 8;
            COLOR_ALPHA_PTR(color2) = (c2 & 1) ? 0xff : 0;
            c3 = tc16[taddr3];
            COLOR_RED_PTR(color3) = c3 >> 8;
            COLOR_GREEN_PTR(color3) = c3 & 0xff;
            COLOR_BLUE_PTR(color3) = c3 >> 8;
            COLOR_ALPHA_PTR(color3) = (c3 & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_IA4:
        {
            uint32_t p, i;

            taddr0 = ((tbase0 << 4) + s0) >> 1;
            taddr1 = ((tbase0 << 4) + s1) >> 1;
            taddr2 = ((tbase2 << 4) + s0) >> 1;
            taddr3 = ((tbase2 << 4) + s1) >> 1;
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0xfff;
            taddr1 &= 0xfff;
            taddr2 &= 0xfff;
            taddr3 &= 0xfff;
            ands = s0 & 1;
            p = __TMEM[taddr0];
            p = ands ? (p & 0xf) : (p >> 4);
            i = p & 0xe;
            i = (i << 4) | (i << 1) | (i >> 2);
            COLOR_RED_PTR(color0) = i;
            COLOR_GREEN_PTR(color0) = i;
            COLOR_BLUE_PTR(color0) = i;
            COLOR_ALPHA_PTR(color0) = (p & 0x1) ? 0xff : 0;
            p = __TMEM[taddr2];
            p = ands ? (p & 0xf) : (p >> 4);
            i = p & 0xe;
            i = (i << 4) | (i << 1) | (i >> 2);
            COLOR_RED_PTR(color2) = i;
            COLOR_GREEN_PTR(color2) = i;
            COLOR_BLUE_PTR(color2) = i;
            COLOR_ALPHA_PTR(color2) = (p & 0x1) ? 0xff : 0;

            ands = s1 & 1;
            p = __TMEM[taddr1];
            p = ands ? (p & 0xf) : (p >> 4);
            i = p & 0xe;
            i = (i << 4) | (i << 1) | (i >> 2);
            COLOR_RED_PTR(color1) = i;
            COLOR_GREEN_PTR(color1) = i;
            COLOR_BLUE_PTR(color1) = i;
            COLOR_ALPHA_PTR(color1) = (p & 0x1) ? 0xff : 0;
            p = __TMEM[taddr3];
            p = ands ? (p & 0xf) : (p >> 4);
            i = p & 0xe;
            i = (i << 4) | (i << 1) | (i >> 2);
            COLOR_RED_PTR(color3) = i;
            COLOR_GREEN_PTR(color3) = i;
            COLOR_BLUE_PTR(color3) = i;
            COLOR_ALPHA_PTR(color3) = (p & 0x1) ? 0xff : 0;
        }
        break;
    case TEXEL_IA8:
        {
            uint32_t p, i;

            taddr0 = ((tbase0 << 3) + s0);
            taddr1 = ((tbase0 << 3) + s1);
            taddr2 = ((tbase2 << 3) + s0);
            taddr3 = ((tbase2 << 3) + s1);
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;

            taddr0 &= 0xfff;
            taddr1 &= 0xfff;
            taddr2 &= 0xfff;
            taddr3 &= 0xfff;
            p = __TMEM[taddr0];
            i = p & 0xf0;
            i |= (i >> 4);
            COLOR_RED_PTR(color0) = i;
            COLOR_GREEN_PTR(color0) = i;
            COLOR_BLUE_PTR(color0) = i;
            COLOR_ALPHA_PTR(color0) = ((p & 0xf) << 4) | (p & 0xf);
            p = __TMEM[taddr1];
            i = p & 0xf0;
            i |= (i >> 4);
            COLOR_RED_PTR(color1) = i;
            COLOR_GREEN_PTR(color1) = i;
            COLOR_BLUE_PTR(color1) = i;
            COLOR_ALPHA_PTR(color1) = ((p & 0xf) << 4) | (p & 0xf);
            p = __TMEM[taddr2];
            i = p & 0xf0;
            i |= (i >> 4);
            COLOR_RED_PTR(color2) = i;
            COLOR_GREEN_PTR(color2) = i;
            COLOR_BLUE_PTR(color2) = i;
            COLOR_ALPHA_PTR(color2) = ((p & 0xf) << 4) | (p & 0xf);
            p = __TMEM[taddr3];
            i = p & 0xf0;
            i |= (i >> 4);
            COLOR_RED_PTR(color3) = i;
            COLOR_GREEN_PTR(color3) = i;
            COLOR_BLUE_PTR(color3) = i;
            COLOR_ALPHA_PTR(color3) = ((p & 0xf) << 4) | (p & 0xf);
        }
        break;
    case TEXEL_IA16:
        {
            uint16_t c0, c1, c2, c3;

            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0x7ff;
            taddr1 &= 0x7ff;
            taddr2 &= 0x7ff;
            taddr3 &= 0x7ff;
            c0 = tc16[taddr0];
            COLOR_RED_PTR(color0) = COLOR_GREEN_PTR(color0) = COLOR_BLUE_PTR(color0) = c0 >> 8;
            COLOR_ALPHA_PTR(color0) = c0 & 0xff;
            c1 = tc16[taddr1];
            COLOR_RED_PTR(color1) = COLOR_GREEN_PTR(color1) = COLOR_BLUE_PTR(color1) = c1 >> 8;
            COLOR_ALPHA_PTR(color1) = c1 & 0xff;
            c2 = tc16[taddr2];
            COLOR_RED_PTR(color2) = COLOR_GREEN_PTR(color2) = COLOR_BLUE_PTR(color2) = c2 >> 8;
            COLOR_ALPHA_PTR(color2) = c2 & 0xff;
            c3 = tc16[taddr3];
            COLOR_RED_PTR(color3) = COLOR_GREEN_PTR(color3) = COLOR_BLUE_PTR(color3) = c3 >> 8;
            COLOR_ALPHA_PTR(color3) = c3 & 0xff;
        }
        break;
    case TEXEL_IA32:
        {
            uint16_t c0, c1, c2, c3;

            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0x7ff;
            taddr1 &= 0x7ff;
            taddr2 &= 0x7ff;
            taddr3 &= 0x7ff;
            c0 = tc16[taddr0];
            COLOR_RED_PTR(color0) = c0 >> 8;
            COLOR_GREEN_PTR(color0) = c0 & 0xff;
            COLOR_BLUE_PTR(color0) = c0 >> 8;
            COLOR_ALPHA_PTR(color0) = (c0 & 1) ? 0xff : 0;
            c1 = tc16[taddr1];
            COLOR_RED_PTR(color1) = c1 >> 8;
            COLOR_GREEN_PTR(color1) = c1 & 0xff;
            COLOR_BLUE_PTR(color1) = c1 >> 8;
            COLOR_ALPHA_PTR(color1) = (c1 & 1) ? 0xff : 0;
            c2 = tc16[taddr2];
            COLOR_RED_PTR(color2) = c2 >> 8;
            COLOR_GREEN_PTR(color2) = c2 & 0xff;
            COLOR_BLUE_PTR(color2) = c2 >> 8;
            COLOR_ALPHA_PTR(color2) = (c2 & 1) ? 0xff : 0;
            c3 = tc16[taddr3];
            COLOR_RED_PTR(color3) = c3 >> 8;
            COLOR_GREEN_PTR(color3) = c3 & 0xff;
            COLOR_BLUE_PTR(color3) = c3 >> 8;
            COLOR_ALPHA_PTR(color3) = (c3 & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_I4:
        {
            uint32_t p, c0, c1, c2, c3;

            taddr0 = ((tbase0 << 4) + s0) >> 1;
            taddr1 = ((tbase0 << 4) + s1) >> 1;
            taddr2 = ((tbase2 << 4) + s0) >> 1;
            taddr3 = ((tbase2 << 4) + s1) >> 1;
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0xfff;
            taddr1 &= 0xfff;
            taddr2 &= 0xfff;
            taddr3 &= 0xfff;
            ands = s0 & 1;
            p = __TMEM[taddr0];
            c0 = ands ? (p & 0xf) : (p >> 4);
            c0 |= (c0 << 4);
            COLOR_RED_PTR(color0) = COLOR_GREEN_PTR(color0) = COLOR_BLUE_PTR(color0) = COLOR_ALPHA_PTR(color0) = c0;
            p = __TMEM[taddr2];
            c2 = ands ? (p & 0xf) : (p >> 4);
            c2 |= (c2 << 4);
            COLOR_RED_PTR(color2) = COLOR_GREEN_PTR(color2) = COLOR_BLUE_PTR(color2) = COLOR_ALPHA_PTR(color2) = c2;

            ands = s1 & 1;
            p = __TMEM[taddr1];
            c1 = ands ? (p & 0xf) : (p >> 4);
            c1 |= (c1 << 4);
            COLOR_RED_PTR(color1) = COLOR_GREEN_PTR(color1) = COLOR_BLUE_PTR(color1) = COLOR_ALPHA_PTR(color1) = c1;
            p = __TMEM[taddr3];
            c3 = ands ? (p & 0xf) : (p >> 4);
            c3 |= (c3 << 4);
            COLOR_RED_PTR(color3) = COLOR_GREEN_PTR(color3) = COLOR_BLUE_PTR(color3) = COLOR_ALPHA_PTR(color3) = c3;
        }
        break;
    case TEXEL_I8:
        {
            uint32_t p;

            taddr0 = ((tbase0 << 3) + s0);
            taddr1 = ((tbase0 << 3) + s1);
            taddr2 = ((tbase2 << 3) + s0);
            taddr3 = ((tbase2 << 3) + s1);
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;

            taddr0 &= 0xfff;
            taddr1 &= 0xfff;
            taddr2 &= 0xfff;
            taddr3 &= 0xfff;
            p = __TMEM[taddr0];
            COLOR_RED_PTR(color0) = p;
            COLOR_GREEN_PTR(color0) = p;
            COLOR_BLUE_PTR(color0) = p;
            COLOR_ALPHA_PTR(color0) = p;
            p = __TMEM[taddr1];
            COLOR_RED_PTR(color1) = p;
            COLOR_GREEN_PTR(color1) = p;
            COLOR_BLUE_PTR(color1) = p;
            COLOR_ALPHA_PTR(color1) = p;
            p = __TMEM[taddr2];
            COLOR_RED_PTR(color2) = p;
            COLOR_GREEN_PTR(color2) = p;
            COLOR_BLUE_PTR(color2) = p;
            COLOR_ALPHA_PTR(color2) = p;
            p = __TMEM[taddr3];
            COLOR_RED_PTR(color3) = p;
            COLOR_GREEN_PTR(color3) = p;
            COLOR_BLUE_PTR(color3) = p;
            COLOR_ALPHA_PTR(color3) = p;
        }
        break;
    case TEXEL_I16:
        {
            uint16_t c0, c1, c2, c3;

            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0x7ff;
            taddr1 &= 0x7ff;
            taddr2 &= 0x7ff;
            taddr3 &= 0x7ff;
            c0 = tc16[taddr0];
            COLOR_RED_PTR(color0) = c0 >> 8;
            COLOR_GREEN_PTR(color0) = c0 & 0xff;
            COLOR_BLUE_PTR(color0) = c0 >> 8;
            COLOR_ALPHA_PTR(color0) = (c0 & 1) ? 0xff : 0;
            c1 = tc16[taddr1];
            COLOR_RED_PTR(color1) = c1 >> 8;
            COLOR_GREEN_PTR(color1) = c1 & 0xff;
            COLOR_BLUE_PTR(color1) = c1 >> 8;
            COLOR_ALPHA_PTR(color1) = (c1 & 1) ? 0xff : 0;
            c2 = tc16[taddr2];
            COLOR_RED_PTR(color2) = c2 >> 8;
            COLOR_GREEN_PTR(color2) = c2 & 0xff;
            COLOR_BLUE_PTR(color2) = c2 >> 8;
            COLOR_ALPHA_PTR(color2) = (c2 & 1) ? 0xff : 0;
            c3 = tc16[taddr3];
            COLOR_RED_PTR(color3) = c3 >> 8;
            COLOR_GREEN_PTR(color3) = c3 & 0xff;
            COLOR_BLUE_PTR(color3) = c3 >> 8;
            COLOR_ALPHA_PTR(color3) = (c3 & 1) ? 0xff : 0;
        }
        break;
    case TEXEL_I32:
        {
            uint16_t c0, c1, c2, c3;

            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
            taddr0 &= 0x7ff;
            taddr1 &= 0x7ff;
            taddr2 &= 0x7ff;
            taddr3 &= 0x7ff;
            c0 = tc16[taddr0];
            COLOR_RED_PTR(color0) = c0 >> 8;
            COLOR_GREEN_PTR(color0) = c0 & 0xff;
            COLOR_BLUE_PTR(color0) = c0 >> 8;
            COLOR_ALPHA_PTR(color0) = (c0 & 1) ? 0xff : 0;
            c1 = tc16[taddr1];
            COLOR_RED_PTR(color1) = c1 >> 8;
            COLOR_GREEN_PTR(color1) = c1 & 0xff;
            COLOR_BLUE_PTR(color1) = c1 >> 8;
            COLOR_ALPHA_PTR(color1) = (c1 & 1) ? 0xff : 0;
            c2 = tc16[taddr2];
            COLOR_RED_PTR(color2) = c2 >> 8;
            COLOR_GREEN_PTR(color2) = c2 & 0xff;
            COLOR_BLUE_PTR(color2) = c2 >> 8;
            COLOR_ALPHA_PTR(color2) = (c2 & 1) ? 0xff : 0;
            c3 = tc16[taddr3];
            COLOR_RED_PTR(color3) = c3 >> 8;
            COLOR_GREEN_PTR(color3) = c3 & 0xff;
            COLOR_BLUE_PTR(color3) = c3 >> 8;
            COLOR_ALPHA_PTR(color3) = (c3 & 1) ? 0xff : 0;
        }
        break;
    }
}

static void fetch_texel_entlut_quadro(COLOR *color0, COLOR *color1, COLOR *color2, COLOR *color3, int s0, int s1, int t0, int t1, uint32_t tilenum)
{
    uint32_t tbase0 = tile[tilenum].line * t0 + tile[tilenum].tmem;
    uint32_t tbase2 = tile[tilenum].line * t1 + tile[tilenum].tmem;
    uint32_t tpal    = tile[tilenum].palette << 4;
    uint32_t xort = 0, ands = 0;

    uint16_t *tc16 = (uint16_t*)__TMEM;
    uint32_t taddr0 = 0, taddr1 = 0, taddr2 = 0, taddr3 = 0;
    uint16_t c0, c1, c2, c3;

    
    
    switch(tile[tilenum].f.tlutswitch)
    {
    case 0:
    case 1:
    case 2:
        {
            taddr0 = ((tbase0 << 4) + s0) >> 1;
            taddr1 = ((tbase0 << 4) + s1) >> 1;
            taddr2 = ((tbase2 << 4) + s0) >> 1;
            taddr3 = ((tbase2 << 4) + s1) >> 1;
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
                                                            
#ifdef EXTRALOGGING
            if (s0 == 0 && t0 == 0)
               fprintf(stderr, "TPAL: %u\n", tpal);
#endif
            ands = s0 & 1;
            c0 = __TMEM[taddr0 & 0x7ff];
            c0 = (ands) ? (c0 & 0xf) : (c0 >> 4);
            c0 = tlut[((tpal + c0) << 2) + WORD_ADDR_XOR];
            c2 = __TMEM[taddr2 & 0x7ff];
            c2 = (ands) ? (c2 & 0xf) : (c2 >> 4);
            c2 = tlut[((tpal + c2) << 2) + WORD_ADDR_XOR];

            ands = s1 & 1;
            c1 = __TMEM[taddr1 & 0x7ff];
            c1 = (ands) ? (c1 & 0xf) : (c1 >> 4);
            c1 = tlut[((tpal + c1) << 2) + WORD_ADDR_XOR];
            c3 = __TMEM[taddr3 & 0x7ff];
            c3 = (ands) ? (c3 & 0xf) : (c3 >> 4);
            c3 = tlut[((tpal + c3) << 2) + WORD_ADDR_XOR];
        }
        break;
    case 3:
        {
            taddr0 = ((tbase0 << 3) + s0);
            taddr1 = ((tbase0 << 3) + s1);
            taddr2 = ((tbase2 << 3) + s0);
            taddr3 = ((tbase2 << 3) + s1);
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;
                                                            
            ands = s0 & 1;
            c0 = __TMEM[taddr0 & 0x7ff];
            c0 = (ands) ? (c0 & 0xf) : (c0 >> 4);
            c0 = tlut[((tpal + c0) << 2) + WORD_ADDR_XOR];
            c2 = __TMEM[taddr2 & 0x7ff];
            c2 = (ands) ? (c2 & 0xf) : (c2 >> 4);
            c2 = tlut[((tpal + c2) << 2) + WORD_ADDR_XOR];

            ands = s1 & 1;
            c1 = __TMEM[taddr1 & 0x7ff];
            c1 = (ands) ? (c1 & 0xf) : (c1 >> 4);
            c1 = tlut[((tpal + c1) << 2) + WORD_ADDR_XOR];
            c3 = __TMEM[taddr3 & 0x7ff];
            c3 = (ands) ? (c3 & 0xf) : (c3 >> 4);
            c3 = tlut[((tpal + c3) << 2) + WORD_ADDR_XOR];
        }
        break;
    case 4:
    case 5:
    case 6:
    case 7:
    case 11:
    case 15:
        {
            taddr0 = ((tbase0 << 3) + s0);
            taddr1 = ((tbase0 << 3) + s1);
            taddr2 = ((tbase2 << 3) + s0);
            taddr3 = ((tbase2 << 3) + s1);
            xort = (t0 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? BYTE_XOR_DWORD_SWAP : BYTE_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;

            c0 = __TMEM[taddr0 & 0x7ff];
            c0 = tlut[(c0 << 2) + WORD_ADDR_XOR];
            c2 = __TMEM[taddr2 & 0x7ff];
            c2 = tlut[(c2 << 2) + WORD_ADDR_XOR];
            c1 = __TMEM[taddr1 & 0x7ff];
            c1 = tlut[(c1 << 2) + WORD_ADDR_XOR];
            c3 = __TMEM[taddr3 & 0x7ff];
            c3 = tlut[(c3 << 2) + WORD_ADDR_XOR];
        }
        break;
    case 8:
    case 9:
    case 10:
    case 12:
    case 13:
    case 14:
        {
            taddr0 = ((tbase0 << 2) + s0);
            taddr1 = ((tbase0 << 2) + s1);
            taddr2 = ((tbase2 << 2) + s0);
            taddr3 = ((tbase2 << 2) + s1);
            xort = (t0 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr0 ^= xort;
            taddr1 ^= xort;
            xort = (t1 & 1) ? WORD_XOR_DWORD_SWAP : WORD_ADDR_XOR;
            taddr2 ^= xort;
            taddr3 ^= xort;

            c0 = tc16[taddr0 & 0x3ff];
            c0 = tlut[((c0 >> 6) & ~3) + WORD_ADDR_XOR];
            c1 = tc16[taddr1 & 0x3ff];
            c1 = tlut[((c1 >> 6) & ~3) + WORD_ADDR_XOR];
            c2 = tc16[taddr2 & 0x3ff];
            c2 = tlut[((c2 >> 6) & ~3) + WORD_ADDR_XOR];
            c3 = tc16[taddr3 & 0x3ff];
            c3 = tlut[((c3 >> 6) & ~3) + WORD_ADDR_XOR];
        }
        break;
    }

    if (!other_modes.tlut_type)
    {
        fetch_texel_quadro_rgba16(color0, color1, color2, color3, c0, c1, c2, c3);
    }
    else
    {
        COLOR_RED_PTR(color0) = COLOR_GREEN_PTR(color0) = COLOR_BLUE_PTR(color0) = c0 >> 8;
        COLOR_ALPHA_PTR(color0) = c0 & 0xff;
        COLOR_RED_PTR(color1) = COLOR_GREEN_PTR(color1) = COLOR_BLUE_PTR(color1) = c1 >> 8;
        COLOR_ALPHA_PTR(color1) = c1 & 0xff;
        COLOR_RED_PTR(color2) = COLOR_GREEN_PTR(color2) = COLOR_BLUE_PTR(color2) = c2 >> 8;
        COLOR_ALPHA_PTR(color2) = c2 & 0xff;
        COLOR_RED_PTR(color3) = COLOR_GREEN_PTR(color3) = COLOR_BLUE_PTR(color3) = c3 >> 8;
        COLOR_ALPHA_PTR(color3) = c3 & 0xff;
    }
}

static void sort_tmem_idx(uint32_t *idx, uint32_t idxa, uint32_t idxb, uint32_t idxc, uint32_t idxd, uint32_t bankno)
{
    if ((idxa & 3) == bankno)
        *idx = idxa & 0x3ff;
    else if ((idxb & 3) == bankno)
        *idx = idxb & 0x3ff;
    else if ((idxc & 3) == bankno)
        *idx = idxc & 0x3ff;
    else if ((idxd & 3) == bankno)
        *idx = idxd & 0x3ff;
    else
        *idx = 0;
}

static void get_tmem_idx(int s, int t, uint32_t tilenum, uint32_t* idx0, uint32_t* idx1, uint32_t* idx2, uint32_t* idx3, uint32_t* bit3flipped, uint32_t* hibit)
{
    uint32_t tbase;
    uint32_t tsize;
    uint32_t tformat;
    uint32_t sshorts;
    int tidx_a, tidx_b, tidx_c, tidx_d;

    tbase  = (tile[tilenum].line * t) & 0x000001FF;
    tbase += tile[tilenum].tmem;
    tsize = tile[tilenum].size;
    tformat = tile[tilenum].format;
    sshorts = 0;

    if (tsize == PIXEL_SIZE_8BIT || tformat == FORMAT_YUV)
        sshorts = s >> 1;
    else if (tsize >= PIXEL_SIZE_16BIT)
        sshorts = s;
    else
        sshorts = s >> 2;
    sshorts &= 0x7ff;

    *bit3flipped = ((sshorts & 2) ? 1 : 0) ^ (t & 1);
    tidx_a = ((tbase << 2) + sshorts) & 0x7fd;
    tidx_b = (tidx_a + 1) & 0x7ff;
    tidx_c = (tidx_a + 2) & 0x7ff;
    tidx_d = (tidx_a + 3) & 0x7ff;

    *hibit = (tidx_a & 0x400) ? 1 : 0;

    if (t & 1)
    {
        tidx_a ^= 2;
        tidx_b ^= 2;
        tidx_c ^= 2;
        tidx_d ^= 2;
    }

    
    sort_tmem_idx(idx0, tidx_a, tidx_b, tidx_c, tidx_d, 0);
    sort_tmem_idx(idx1, tidx_a, tidx_b, tidx_c, tidx_d, 1);
    sort_tmem_idx(idx2, tidx_a, tidx_b, tidx_c, tidx_d, 2);
    sort_tmem_idx(idx3, tidx_a, tidx_b, tidx_c, tidx_d, 3);
}

static void compute_color_index(uint32_t* cidx, uint32_t readshort, uint32_t nybbleoffset, uint32_t tilenum)
{
    uint32_t lownib, hinib;
    if (tile[tilenum].size == PIXEL_SIZE_4BIT)
    {
        lownib = (nybbleoffset ^ 3) << 2;
        hinib = tile[tilenum].palette;
    }
    else
    {
        lownib = ((nybbleoffset & 2) ^ 2) << 2;
        hinib = lownib ? ((readshort >> 12) & 0xf) : ((readshort >> 4) & 0xf);
    }
    lownib = (readshort >> lownib) & 0xf;
    *cidx = (hinib << 4) | lownib;
}

static void sort_tmem_shorts_lowhalf(uint32_t* bindshort, uint32_t short0, uint32_t short1, uint32_t short2, uint32_t short3, uint32_t bankno)
{
    switch(bankno)
    {
    case 0:
        *bindshort = short0;
        break;
    case 1:
        *bindshort = short1;
        break;
    case 2:
        *bindshort = short2;
        break;
    case 3:
        *bindshort = short3;
        break;
    }
}

static void read_tmem_copy(int s, int s1, int s2, int s3, int t, uint32_t tilenum, uint32_t* sortshort, int* hibits, int* lowbits)
{
    uint32_t sortidx[8];
    uint32_t tbase;
    uint32_t tsize;
    uint32_t tformat;
    uint32_t shbytes, shbytes1, shbytes2, shbytes3;
    int32_t delta;
    uint16_t* tmem16;
    uint32_t short0, short1, short2, short3;
    int tidx_a, tidx_blow, tidx_bhi, tidx_c, tidx_dlow, tidx_dhi;

    delta = 0;
    tbase  = (tile[tilenum].line * t) & 0x000001FF;
    tbase += tile[tilenum].tmem;
    tsize = tile[tilenum].size;
    tformat = tile[tilenum].format;

    if (tsize == PIXEL_SIZE_8BIT || tformat == FORMAT_YUV)
    {
        shbytes = s << 1;
        shbytes1 = s1 << 1;
        shbytes2 = s2 << 1;
        shbytes3 = s3 << 1;
    }
    else if (tsize >= PIXEL_SIZE_16BIT)
    {
        shbytes = s << 2;
        shbytes1 = s1 << 2;
        shbytes2 = s2 << 2;
        shbytes3 = s3 << 2;
    }
    else
    {
        shbytes = s;
        shbytes1 = s1;
        shbytes2 = s2;
        shbytes3 = s3;
    }

    shbytes &= 0x1fff;
    shbytes1 &= 0x1fff;
    shbytes2 &= 0x1fff;
    shbytes3 &= 0x1fff;

    tbase <<= 4;
    tidx_a = (tbase + shbytes) & 0x1fff;
    tidx_bhi = (tbase + shbytes1) & 0x1fff;
    tidx_c = (tbase + shbytes2) & 0x1fff;
    tidx_dhi = (tbase + shbytes3) & 0x1fff;

    if (tformat == FORMAT_YUV)
    {
        delta = shbytes1 - shbytes;
        tidx_blow = (tidx_a + (delta << 1)) & 0x1fff;
        tidx_dlow = (tidx_blow + shbytes3 - shbytes) & 0x1fff;
    }
    else
    {
        tidx_blow = tidx_bhi;
        tidx_dlow = tidx_dhi;
    }

    if (t & 1)
    {
        tidx_a ^= 8;
        tidx_blow ^= 8;
        tidx_bhi ^= 8;
        tidx_c ^= 8;
        tidx_dlow ^= 8;
        tidx_dhi ^= 8;
    }

    hibits[0] = (tidx_a & 0x1000) ? 1 : 0;
    hibits[1] = (tidx_blow & 0x1000) ? 1 : 0; 
    hibits[2] =    (tidx_bhi & 0x1000) ? 1 : 0;
    hibits[3] =    (tidx_c & 0x1000) ? 1 : 0;
    hibits[4] =    (tidx_dlow & 0x1000) ? 1 : 0;
    hibits[5] = (tidx_dhi & 0x1000) ? 1 : 0;
    lowbits[0] = tidx_a & 0xf;
    lowbits[1] = tidx_blow & 0xf;
    lowbits[2] = tidx_bhi & 0xf;
    lowbits[3] = tidx_c & 0xf;
    lowbits[4] = tidx_dlow & 0xf;
    lowbits[5] = tidx_dhi & 0xf;

    tmem16 = (uint16_t *)__TMEM;

    tidx_a >>= 2;
    tidx_blow >>= 2;
    tidx_bhi >>= 2;
    tidx_c >>= 2;
    tidx_dlow >>= 2;
    tidx_dhi >>= 2;

    
    sort_tmem_idx(&sortidx[0], tidx_a, tidx_blow, tidx_c, tidx_dlow, 0);
    sort_tmem_idx(&sortidx[1], tidx_a, tidx_blow, tidx_c, tidx_dlow, 1);
    sort_tmem_idx(&sortidx[2], tidx_a, tidx_blow, tidx_c, tidx_dlow, 2);
    sort_tmem_idx(&sortidx[3], tidx_a, tidx_blow, tidx_c, tidx_dlow, 3);

    short0 = tmem16[sortidx[0] ^ WORD_ADDR_XOR];
    short1 = tmem16[sortidx[1] ^ WORD_ADDR_XOR];
    short2 = tmem16[sortidx[2] ^ WORD_ADDR_XOR];
    short3 = tmem16[sortidx[3] ^ WORD_ADDR_XOR];

    
    sort_tmem_shorts_lowhalf(&sortshort[0], short0, short1, short2, short3, lowbits[0] >> 2);
    sort_tmem_shorts_lowhalf(&sortshort[1], short0, short1, short2, short3, lowbits[1] >> 2);
    sort_tmem_shorts_lowhalf(&sortshort[2], short0, short1, short2, short3, lowbits[3] >> 2);
    sort_tmem_shorts_lowhalf(&sortshort[3], short0, short1, short2, short3, lowbits[4] >> 2);

    if (other_modes.en_tlut)
    {
         
        compute_color_index(&short0, sortshort[0], lowbits[0] & 3, tilenum);
        compute_color_index(&short1, sortshort[1], lowbits[1] & 3, tilenum);
        compute_color_index(&short2, sortshort[2], lowbits[3] & 3, tilenum);
        compute_color_index(&short3, sortshort[3], lowbits[4] & 3, tilenum);

        
        sortidx[4] = (short0 << 2);
        sortidx[5] = (short1 << 2) | 1;
        sortidx[6] = (short2 << 2) | 2;
        sortidx[7] = (short3 << 2) | 3;
    }
    else
    {
        sort_tmem_idx(&sortidx[4], tidx_a, tidx_bhi, tidx_c, tidx_dhi, 0);
        sort_tmem_idx(&sortidx[5], tidx_a, tidx_bhi, tidx_c, tidx_dhi, 1);
        sort_tmem_idx(&sortidx[6], tidx_a, tidx_bhi, tidx_c, tidx_dhi, 2);
        sort_tmem_idx(&sortidx[7], tidx_a, tidx_bhi, tidx_c, tidx_dhi, 3);
    }

    short0 = tmem16[(sortidx[4] | 0x400) ^ WORD_ADDR_XOR];
    short1 = tmem16[(sortidx[5] | 0x400) ^ WORD_ADDR_XOR];
    short2 = tmem16[(sortidx[6] | 0x400) ^ WORD_ADDR_XOR];
    short3 = tmem16[(sortidx[7] | 0x400) ^ WORD_ADDR_XOR];

    if (other_modes.en_tlut)
    {
        sort_tmem_shorts_lowhalf(&sortshort[4], short0, short1, short2, short3, 0);
        sort_tmem_shorts_lowhalf(&sortshort[5], short0, short1, short2, short3, 1);
        sort_tmem_shorts_lowhalf(&sortshort[6], short0, short1, short2, short3, 2);
        sort_tmem_shorts_lowhalf(&sortshort[7], short0, short1, short2, short3, 3);
    }
    else
    {
        sort_tmem_shorts_lowhalf(&sortshort[4], short0, short1, short2, short3, lowbits[0] >> 2);
        sort_tmem_shorts_lowhalf(&sortshort[5], short0, short1, short2, short3, lowbits[2] >> 2);
        sort_tmem_shorts_lowhalf(&sortshort[6], short0, short1, short2, short3, lowbits[3] >> 2);
        sort_tmem_shorts_lowhalf(&sortshort[7], short0, short1, short2, short3, lowbits[5] >> 2);
    }
}








static void replicate_for_copy(uint32_t* outbyte, uint32_t inshort, uint32_t nybbleoffset, uint32_t tilenum, uint32_t tformat, uint32_t tsize)
{
    uint32_t lownib, hinib;
    switch(tsize)
    {
    case PIXEL_SIZE_4BIT:
        lownib = (nybbleoffset ^ 3) << 2;
        lownib = hinib = (inshort >> lownib) & 0xf;
        if (tformat == FORMAT_CI)
        {
            *outbyte = (tile[tilenum].palette << 4) | lownib;
        }
        else if (tformat == FORMAT_IA)
        {
            lownib = (lownib << 4) | lownib;
            *outbyte = (lownib & 0xe0) | ((lownib & 0xe0) >> 3) | ((lownib & 0xc0) >> 6);
        }
        else
            *outbyte = (lownib << 4) | lownib;
        break;
    case PIXEL_SIZE_8BIT:
        hinib = ((nybbleoffset ^ 3) | 1) << 2;
        if (tformat == FORMAT_IA)
        {
            lownib = (inshort >> hinib) & 0xf;
            *outbyte = (lownib << 4) | lownib;
        }
        else
        {
            lownib = (inshort >> (hinib & ~4)) & 0xf;
            hinib = (inshort >> hinib) & 0xf;
            *outbyte = (hinib << 4) | lownib;
        }
        break;
    default:
        *outbyte = (inshort >> 8) & 0xff;
        break;
    }
}

static void tc_pipeline_copy(int32_t* sss0, int32_t* sss1, int32_t* sss2, int32_t* sss3, int32_t* sst, int tilenum)                                            
{
    int ss0 = *sss0, ss1 = 0, ss2 = 0, ss3 = 0, st = *sst;

    tcshift_copy(&ss0, &st, tilenum);
    
    

    ss0 = TRELATIVE(ss0, tile[tilenum].sl);
    st = TRELATIVE(st, tile[tilenum].tl);
    ss0 = (ss0 >> 5);
    st = (st >> 5);

    ss1 = ss0 + 1;
    ss2 = ss0 + 2;
    ss3 = ss0 + 3;

    tcmask_copy(&ss0, &ss1, &ss2, &ss3, &st, tilenum);    

    *sss0 = ss0;
    *sss1 = ss1;
    *sss2 = ss2;
    *sss3 = ss3;
    *sst = st;
}

static void fetch_qword_copy(uint32_t* hidword, uint32_t* lowdword, int32_t ssss, int32_t ssst, uint32_t tilenum)
{
    uint32_t shorta, shortb, shortc, shortd;
    uint32_t sortshort[8];
    int hibits[6];
    int lowbits[6];
    int32_t sss = ssss, sst = ssst, sss1 = 0, sss2 = 0, sss3 = 0;
    int largetex = 0;

    uint32_t tformat, tsize;
    if (other_modes.en_tlut)
    {
        tsize = PIXEL_SIZE_16BIT;
        tformat = other_modes.tlut_type ? FORMAT_IA : FORMAT_RGBA;
    }
    else
    {
        tsize = tile[tilenum].size;
        tformat = tile[tilenum].format;
    }

    tc_pipeline_copy(&sss, &sss1, &sss2, &sss3, &sst, tilenum);
    read_tmem_copy(sss, sss1, sss2, sss3, sst, tilenum, sortshort, hibits, lowbits);
    largetex = (tformat == FORMAT_YUV || (tformat == FORMAT_RGBA && tsize == PIXEL_SIZE_32BIT));

    
    if (other_modes.en_tlut)
    {
        shorta = sortshort[4];
        shortb = sortshort[5];
        shortc = sortshort[6];
        shortd = sortshort[7];
    }
    else if (largetex)
    {
        shorta = sortshort[0];
        shortb = sortshort[1];
        shortc = sortshort[2];
        shortd = sortshort[3];
    }
    else
    {
        shorta = hibits[0] ? sortshort[4] : sortshort[0];
        shortb = hibits[1] ? sortshort[5] : sortshort[1];
        shortc = hibits[3] ? sortshort[6] : sortshort[2];
        shortd = hibits[4] ? sortshort[7] : sortshort[3];
    }

    *lowdword = (shortc << 16) | shortd;

    if (tsize == PIXEL_SIZE_16BIT)
        *hidword = (shorta << 16) | shortb;
    else
    {
        replicate_for_copy(&shorta, shorta, lowbits[0] & 3, tilenum, tformat, tsize);
        replicate_for_copy(&shortb, shortb, lowbits[1] & 3, tilenum, tformat, tsize);
        replicate_for_copy(&shortc, shortc, lowbits[3] & 3, tilenum, tformat, tsize);
        replicate_for_copy(&shortd, shortd, lowbits[4] & 3, tilenum, tformat, tsize);
        *hidword = (shorta << 24) | (shortb << 16) | (shortc << 8) | shortd;
    }
}

static unsigned angrylion_filtering = 0;

void angrylion_set_filtering(unsigned filter_type)
{
   angrylion_filtering = filter_type;
}


static void texture_pipeline_cycle(COLOR* TEX, COLOR* prev, int32_t SSS, int32_t SST, uint32_t tilenum, uint32_t cycle)                                            
{
    int32_t maxs, maxt, invt0r, invt0g, invt0b, invt0a;
    int32_t sfrac, tfrac, invsf, invtf;
    int upper = 0;
    int bilerp = cycle ? other_modes.bi_lerp1 : other_modes.bi_lerp0;
    int convert = other_modes.convert_one && cycle;
    COLOR t0, t1, t2, t3;
    int sss1, sst1, sss2, sst2;

    sss1 = SSS;
    sst1 = SST;

    tcshift_cycle(&sss1, &sst1, &maxs, &maxt, tilenum);

    sss1 = TRELATIVE(sss1, tile[tilenum].sl);
    sst1 = TRELATIVE(sst1, tile[tilenum].tl);

    if (other_modes.sample_type 
          && angrylion_filtering != 2)
    {    
        sfrac = sss1 & 0x1f;
        tfrac = sst1 & 0x1f;

        tcclamp_cycle(&sss1, &sst1, &sfrac, &tfrac, maxs, maxt, tilenum);
        
    
        if (tile[tilenum].format != FORMAT_YUV)
            sss2 = sss1 + 1;
        else
            sss2 = sss1 + 2;
        
        
        

        sst2 = sst1 + 1;
        

        
        tcmask_coupled(&sss1, &sss2, &sst1, &sst2, tilenum);
        
        

        
        
        
        

        
        if (bilerp)
        {
            
            if (!other_modes.en_tlut)
                fetch_texel_quadro(&t0, &t1, &t2, &t3, sss1, sss2, sst1, sst2, tilenum);
            else
                fetch_texel_entlut_quadro(&t0, &t1, &t2, &t3, sss1, sss2, sst1, sst2, tilenum);

            if (!other_modes.mid_texel || sfrac != 0x10 || tfrac != 0x10)
            {
                if (!convert)
                {

                    if (UPPER)
                    {
                        
                        invsf = 0x20 - sfrac;
                        invtf = 0x20 - tfrac;
                        COLOR_RED_PTR(TEX) = COLOR_RED(t3) + ((((invsf * (COLOR_RED(t2) - COLOR_RED(t3))) + (invtf * (COLOR_RED(t1) - COLOR_RED(t3)))) + 0x10) >> 5);    
                        COLOR_GREEN_PTR(TEX) = COLOR_GREEN(t3) + ((((invsf * (COLOR_GREEN(t2) - COLOR_GREEN(t3))) + (invtf * (COLOR_GREEN(t1) - COLOR_GREEN(t3)))) + 0x10) >> 5);                                                                        
                        COLOR_BLUE_PTR(TEX) = COLOR_BLUE(t3) + ((((invsf * (COLOR_BLUE(t2) - COLOR_BLUE(t3))) + (invtf * (COLOR_BLUE(t1) - COLOR_BLUE(t3)))) + 0x10) >> 5);                                                                
                        COLOR_ALPHA_PTR(TEX) = COLOR_ALPHA(t3) + ((((invsf * (COLOR_ALPHA(t2) - COLOR_ALPHA(t3))) + (invtf * (COLOR_ALPHA(t1) - COLOR_ALPHA(t3)))) + 0x10) >> 5);
                    }
                    else
                    {
                       COLOR_RED_PTR(TEX) = COLOR_RED(t0) + ((((sfrac * (COLOR_RED(t1) - COLOR_RED(t0))) + (tfrac * (COLOR_RED(t2) - COLOR_RED(t0)))) + 0x10) >> 5);                                            
                       COLOR_GREEN_PTR(TEX) = COLOR_GREEN(t0) + ((((sfrac * (COLOR_GREEN(t1) - COLOR_GREEN(t0))) + (tfrac * (COLOR_GREEN(t2) - COLOR_GREEN(t0)))) + 0x10) >> 5);                                            
                       COLOR_BLUE_PTR(TEX) = COLOR_BLUE(t0) + ((((sfrac * (COLOR_BLUE(t1) - COLOR_BLUE(t0))) + (tfrac * (COLOR_BLUE(t2) - COLOR_BLUE(t0)))) + 0x10) >> 5);                                    
                       COLOR_ALPHA_PTR(TEX) = COLOR_ALPHA(t0) + ((((sfrac * (COLOR_ALPHA(t1) - COLOR_ALPHA(t0))) + (tfrac * (COLOR_ALPHA(t2) - COLOR_ALPHA(t0)))) + 0x10) >> 5);
                    }
                }
                else
                {

                    if (UPPER)
                    {
                       COLOR_RED_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_RED(t2) - COLOR_RED(t3))) + (COLOR_GREEN_PTR(prev) * (COLOR_RED(t1) - COLOR_RED(t3)))) + 0x80) >> 8);    
                       COLOR_GREEN_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_GREEN(t2) - COLOR_GREEN(t3))) + (COLOR_GREEN_PTR(prev) * (COLOR_GREEN(t1) - COLOR_GREEN(t3)))) + 0x80) >> 8);                                                                        
                       COLOR_BLUE_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_BLUE(t2) - COLOR_BLUE(t3))) + (COLOR_GREEN_PTR(prev) * (COLOR_BLUE(t1) - COLOR_BLUE(t3)))) + 0x80) >> 8);                                                                
                       COLOR_ALPHA_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_ALPHA(t2) - COLOR_ALPHA(t3))) + (COLOR_GREEN_PTR(prev) * (COLOR_ALPHA(t1) - COLOR_ALPHA(t3)))) + 0x80) >> 8);
                    }
                    else
                    {
                        COLOR_RED_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_RED(t1) - COLOR_RED(t0))) + (COLOR_GREEN_PTR(prev) * (COLOR_RED(t2) - COLOR_RED(t0)))) + 0x80) >> 8);                                            
                        COLOR_GREEN_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_GREEN(t1) - COLOR_GREEN(t0))) + (COLOR_GREEN_PTR(prev) * (COLOR_GREEN(t2) - COLOR_GREEN(t0)))) + 0x80) >> 8);                                            
                        COLOR_BLUE_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_BLUE(t1) - COLOR_BLUE(t0))) + (COLOR_GREEN_PTR(prev) * (COLOR_BLUE(t2) - COLOR_BLUE(t0)))) + 0x80) >> 8);                                    
                        COLOR_ALPHA_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_ALPHA(t1) - COLOR_ALPHA(t0))) + (COLOR_GREEN_PTR(prev) * (COLOR_ALPHA(t2) - COLOR_ALPHA(t0)))) + 0x80) >> 8);
                    }    
                }
                
            }
            else
            {
               invt0r  = ~COLOR_RED(t0);
               invt0g  = ~COLOR_GREEN(t0);
               invt0b  = ~COLOR_BLUE(t0);
               invt0a  = ~COLOR_ALPHA(t0);

                if (!convert)
                {
                    sfrac <<= 2;
                    tfrac <<= 2;

                    COLOR_RED_PTR(TEX) = COLOR_RED(t0) + ((((sfrac * (COLOR_RED(t1) - COLOR_RED(t0))) + (tfrac * (COLOR_RED(t2) - COLOR_RED(t0)))) + ((invt0r + COLOR_RED(t3)) << 6) + 0xc0) >> 8);                                            
                    COLOR_GREEN_PTR(TEX) = COLOR_GREEN(t0) + ((((sfrac * (COLOR_GREEN(t1) - COLOR_GREEN(t0))) + (tfrac * (COLOR_GREEN(t2) - COLOR_GREEN(t0)))) + ((invt0g + COLOR_GREEN(t3)) << 6) + 0xc0) >> 8);                                            
                    COLOR_BLUE_PTR(TEX) = COLOR_BLUE(t0) + ((((sfrac * (COLOR_BLUE(t1) - COLOR_BLUE(t0))) + (tfrac * (COLOR_BLUE(t2) - COLOR_BLUE(t0)))) + ((invt0b + COLOR_BLUE(t3)) << 6) + 0xc0) >> 8);                                    
                    COLOR_ALPHA_PTR(TEX) = COLOR_ALPHA(t0) + ((((sfrac * (COLOR_ALPHA(t1) - COLOR_ALPHA(t0))) + (tfrac * (COLOR_ALPHA(t2) - COLOR_ALPHA(t0)))) + ((invt0a + COLOR_ALPHA(t3)) << 6) + 0xc0) >> 8);
                }
                else
                {
            COLOR_RED_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_RED(t1) - COLOR_RED(t0))) + (COLOR_GREEN_PTR(prev) * (COLOR_RED(t2) - COLOR_RED(t0)))) + ((invt0r + COLOR_RED(t3)) << 6) + 0xc0) >> 8);                                            
            COLOR_GREEN_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_GREEN(t1) - COLOR_GREEN(t0))) + (COLOR_GREEN_PTR(prev) * (COLOR_GREEN(t2) - COLOR_GREEN(t0)))) + ((invt0g + COLOR_GREEN(t3)) << 6) + 0xc0) >> 8);                                            
            COLOR_BLUE_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_BLUE(t1) - COLOR_BLUE(t0))) + (COLOR_GREEN_PTR(prev) * (COLOR_BLUE(t2) - COLOR_BLUE(t0)))) + ((invt0b + COLOR_BLUE(t3)) << 6) + 0xc0) >> 8);                                    
            COLOR_ALPHA_PTR(TEX) = COLOR_BLUE_PTR(prev) + ((((COLOR_RED_PTR(prev) * (COLOR_ALPHA(t1) - COLOR_ALPHA(t0))) + (COLOR_GREEN_PTR(prev) * (COLOR_ALPHA(t2) - COLOR_ALPHA(t0)))) + ((invt0a + COLOR_ALPHA(t3)) << 6) + 0xc0) >> 8);
                }
            }
            
        }
        else
        {
            if (!other_modes.en_tlut)
                fetch_texel(&t0, sss1, sst1, tilenum);
            else
                fetch_texel_entlut(&t0, sss1, sst1, tilenum);
            if (convert)
            {
                t0 = *prev;
            }

            if (tile[tilenum].format == FORMAT_YUV)
            {
               COLOR_RED(t0)   = SIGN(COLOR_RED(t0), 9);
               COLOR_GREEN(t0) = SIGN(COLOR_GREEN(t0), 9);
            }


            COLOR_RED_PTR(TEX) = COLOR_BLUE(t0) + ((k0_tf * COLOR_GREEN(t0) + 0x80) >> 8);
            COLOR_GREEN_PTR(TEX) = COLOR_BLUE(t0) + ((k1_tf * COLOR_RED(t0) + k2_tf * COLOR_GREEN(t0) + 0x80) >> 8);
            COLOR_BLUE_PTR(TEX) = COLOR_BLUE(t0) + ((k3_tf * COLOR_RED(t0) + 0x80) >> 8);
            COLOR_ALPHA_PTR(TEX) = COLOR_BLUE(t0);
        }
        
        COLOR_RED_PTR(TEX) &= 0x1ff;
        COLOR_GREEN_PTR(TEX) &= 0x1ff;
        COLOR_BLUE_PTR(TEX) &= 0x1ff;
        COLOR_ALPHA_PTR(TEX) &= 0x1ff;
    }
    else                                                                                                
    {                                                                                                        
        
        
        

        tcclamp_cycle_light(&sss1, &sst1, maxs, maxt, tilenum);
        
        tcmask(&sss1, &sst1, tilenum);    
                                                                                                        
            
        if (!other_modes.en_tlut)
            fetch_texel(&t0, sss1, sst1, tilenum);
        else
            fetch_texel_entlut(&t0, sss1, sst1, tilenum);
        
        if (bilerp)
        {
            if (!convert)
            {
                *TEX = t0;
            }
            else
                COLOR_RED_PTR(TEX) = COLOR_GREEN_PTR(TEX) = COLOR_BLUE_PTR(TEX) = COLOR_ALPHA_PTR(TEX) = COLOR_BLUE_PTR(prev);
        }
        else
        {
            if (convert)
            {
                t0 = *prev;
            }
            
            if (tile[tilenum].format == FORMAT_YUV)
            {
               COLOR_RED(t0)   = SIGN(COLOR_RED(t0), 9); 
               COLOR_GREEN(t0) = SIGN(COLOR_GREEN(t0), 9);
            }

            COLOR_RED_PTR(TEX) = COLOR_BLUE(t0) + ((k0_tf * COLOR_GREEN(t0) + 0x80) >> 8);
            COLOR_GREEN_PTR(TEX) = COLOR_BLUE(t0) + ((k1_tf * COLOR_RED(t0) + k2_tf * COLOR_GREEN(t0) + 0x80) >> 8);
            COLOR_BLUE_PTR(TEX) = COLOR_BLUE(t0) + ((k3_tf * COLOR_RED(t0) + 0x80) >> 8);
            COLOR_ALPHA_PTR(TEX) = COLOR_BLUE(t0);
            COLOR_RED_PTR(TEX) &= 0x1ff;
            COLOR_GREEN_PTR(TEX) &= 0x1ff;
            COLOR_BLUE_PTR(TEX) &= 0x1ff;
            COLOR_ALPHA_PTR(TEX) &= 0x1ff;
        }
    }
                                                                                                    
}


static STRICTINLINE void tc_pipeline_load(int32_t* sss, int32_t* sst, int tilenum, int coord_quad)
{
    int sss1 = *sss, sst1 = *sst;
    sss1 = SIGN16(sss1);
    sst1 = SIGN16(sst1);

    
    sss1 = TRELATIVE(sss1, tile[tilenum].sl);
    sst1 = TRELATIVE(sst1, tile[tilenum].tl);
    

    
    if (!coord_quad)
    {
        sss1 = (sss1 >> 5);
        sst1 = (sst1 >> 5);
    }
    else
    {
        sss1 = (sss1 >> 3);
        sst1 = (sst1 >> 3);
    }
    
    *sss = sss1;
    *sst = sst1;
}

static INLINE void tcdiv_nopersp(int32_t ss, int32_t st, int32_t sw, int32_t* sss, int32_t* sst)
{
    *sss = (SIGN16(ss)) & 0x1ffff;
    *sst = (SIGN16(st)) & 0x1ffff;
}

static INLINE void tcdiv_persp(int32_t ss, int32_t st, int32_t sw, int32_t* sss, int32_t* sst)
{
    int w_carry = 0;
    int shift; 
    int tlu_rcp;
    int sprod, tprod;
    int outofbounds_s, outofbounds_t;
    int tempmask;
    int shift_value;
    int32_t temps, tempt;

    
    
    int overunder_s = 0, overunder_t = 0;
    
    
    if (SIGN16(sw) <= 0)
        w_carry = 1;

    sw &= 0x7fff;

    
    
    shift = tcdiv_table[sw];
    tlu_rcp = shift >> 4;
    shift &= 0xf;

    sprod = SIGN16(ss) * tlu_rcp;
    tprod = SIGN16(st) * tlu_rcp;

    
    
    
    tempmask = ((1 << 30) - 1) & -((1 << 29) >> shift);
    
    outofbounds_s = sprod & tempmask;
    outofbounds_t = tprod & tempmask;
    
    if (shift != 0xe)
    {
        shift_value = 13 - shift;
        temps = sprod = (sprod >> shift_value);
        tempt = tprod = (tprod >> shift_value);
    }
    else
    {
        temps = sprod << 1;
        tempt = tprod << 1;
    }
    
    if (outofbounds_s != tempmask && outofbounds_s != 0)
    {
        if (!(sprod & (1 << 29)))
            overunder_s = 2 << 17;
        else
            overunder_s = 1 << 17;
    }

    if (outofbounds_t != tempmask && outofbounds_t != 0)
    {
        if (!(tprod & (1 << 29)))
            overunder_t = 2 << 17;
        else
            overunder_t = 1 << 17;
    }

    if (w_carry)
    {
        overunder_s |= (2 << 17);
        overunder_t |= (2 << 17);
    }

    *sss = (temps & 0x1ffff) | overunder_s;
    *sst = (tempt & 0x1ffff) | overunder_t;
}

static INLINE void tcdiv(int32_t ss, int32_t st, int32_t sw, int32_t* sss, int32_t* sst)
{
   if (other_modes.persp_tex_en)
      tcdiv_persp(ss, st, sw, sss, sst);
   else
      tcdiv_nopersp(ss, st, sw, sss, sst);
}

static STRICTINLINE uint32_t rightcvghex(uint32_t x, uint32_t fmask)
{
    uint32_t covered = ((x & 7) + 1) >> 1;

    covered = 0xf0 >> covered;
    return (covered & fmask);
}

static STRICTINLINE uint32_t leftcvghex(uint32_t x, uint32_t fmask) 
{
    uint32_t covered = ((x & 7) + 1) >> 1;

    covered = 0xf >> covered;
    return (covered & fmask);
}

static STRICTINLINE void compute_cvg_flip(int32_t scanline)
{
   int i, fmask, maskshift, fmaskshifted;
   int32_t minorcur, majorcur, minorcurint, majorcurint, samecvg;
   int32_t purgestart = span[scanline].rx;
   int32_t   purgeend = span[scanline].lx;
   int         length = purgeend - purgestart;

   if (length >= 0)
   {






      memset(&cvgbuf[purgestart], 0xff, length + 1);
      for(i = 0; i < 4; i++)
      {
         int k;
         fmask = 0xa >> (i & 1);




         maskshift = (i - 2) & 4;
         fmaskshifted = fmask << maskshift;

         if (!span[scanline].invalyscan[i])
         {

            minorcur = span[scanline].minorx[i];
            majorcur = span[scanline].majorx[i];
            minorcurint = minorcur >> 3;
            majorcurint = majorcur >> 3;


            for (k = purgestart; k <= majorcurint; k++)
               cvgbuf[k] &= ~fmaskshifted;
            for (k = minorcurint; k <= purgeend; k++)
               cvgbuf[k] &= ~fmaskshifted;









            if (minorcurint > majorcurint)
            {
               cvgbuf[minorcurint] |= (rightcvghex(minorcur, fmask) << maskshift);
               cvgbuf[majorcurint] |= (leftcvghex(majorcur, fmask) << maskshift);
            }
            else if (minorcurint == majorcurint)
            {
               samecvg = rightcvghex(minorcur, fmask) & leftcvghex(majorcur, fmask);
               cvgbuf[majorcurint] |= (samecvg << maskshift);
            }
         }
         else
         {
            for (k = purgestart; k <= purgeend; k++)
               cvgbuf[k] &= ~fmaskshifted;
         }

      }
   }
}

static STRICTINLINE void compute_cvg_noflip(int32_t scanline)
{
	int i, fmask, maskshift, fmaskshifted;
	int32_t minorcur, majorcur, minorcurint, majorcurint, samecvg;
	int32_t purgestart = span[scanline].lx;
	int32_t purgeend   = span[scanline].rx;
	int         length = purgeend - purgestart;

	if (length >= 0)
	{
		memset(&cvgbuf[purgestart], 0xff, length + 1);

		for(i = 0; i < 4; i++)
		{
         int k;

			fmask = 0xa >> (i & 1);
			maskshift = (i - 2) & 4;
			fmaskshifted = fmask << maskshift;

			if (!span[scanline].invalyscan[i])
			{
				minorcur = span[scanline].minorx[i];
				majorcur = span[scanline].majorx[i];
				minorcurint = minorcur >> 3;
				majorcurint = majorcur >> 3;
								
				for (k = purgestart; k <= minorcurint; k++)
					cvgbuf[k] &= ~fmaskshifted;
				for (k = majorcurint; k <= purgeend; k++)
					cvgbuf[k] &= ~fmaskshifted;

				if (majorcurint > minorcurint)
				{
					cvgbuf[minorcurint] |= (leftcvghex(minorcur, fmask) << maskshift);
					cvgbuf[majorcurint] |= (rightcvghex(majorcur, fmask) << maskshift);
				}
				else if (minorcurint == majorcurint)
				{
					samecvg = leftcvghex(minorcur, fmask) & rightcvghex(majorcur, fmask);
					cvgbuf[majorcurint] |= (samecvg << maskshift);
				}
			}
			else
			{
				for (k = purgestart; k <= purgeend; k++)
					cvgbuf[k] &= ~fmaskshifted;
			}
		}
	}
}

static STRICTINLINE uint32_t dz_compress(uint32_t value)
{
    int j = 0;
    if (value & 0xff00)
        j |= 8;
    if (value & 0xf0f0)
        j |= 4;
    if (value & 0xcccc)
        j |= 2;
    if (value & 0xaaaa)
        j |= 1;
    return j;
}

static STRICTINLINE void z_store(uint32_t zcurpixel, uint32_t z, int dzpixenc)
{
    uint16_t zval = z_com_table[z & 0x3ffff]|(dzpixenc >> 2);
    uint8_t hval = dzpixenc & 3;
    PAIRWRITE16(zcurpixel, zval, hval);
}

static STRICTINLINE uint32_t z_decompress(uint32_t zb)
{
    zb &= 0x0000FFFF;
    return (z_complete_dec_table[zb >> 2]);
}

static STRICTINLINE uint32_t dz_decompress(uint32_t dz_compressed)
{
    return (1 << dz_compressed);
}

static uint32_t z_compare(uint32_t zcurpixel, uint32_t sz, uint16_t dzpix, int dzpixenc, uint32_t* blend_en, uint32_t* prewrap, uint32_t* curpixel_cvg, uint32_t curpixel_memcvg)
{
    int32_t diff;
    int32_t rawdzmem;
    uint32_t oz, dzmem, zval, hval;
    uint32_t nearer, max, infront;
    int cvgcoeff       = 0;
    uint32_t dzenc     = 0;
    int force_coplanar = 0;

    sz &= 0x3ffff;
    if (other_modes.z_compare_en)
    {
        uint32_t dznew;
        uint32_t dznotshift;
        uint32_t dzmemmodifier;
        uint32_t farther;
        int overflow;
        int precision_factor;

        PAIRREAD16(zval, hval, zcurpixel);
        oz = z_decompress(zval);
        rawdzmem = ((zval & 3) << 2) | hval;
        dzmem = dz_decompress(rawdzmem);

        if (other_modes.f.realblendershiftersneeded)
        {
           blshifta = CLIP(dzpixenc - rawdzmem, 0, 4);
           blshiftb = CLIP(rawdzmem - dzpixenc, 0, 4);
        }

        if (other_modes.f.interpixelblendershiftersneeded)
        {
           pastblshifta = CLIP(dzpixenc - pastrawdzmem, 0, 4);
           pastblshiftb = CLIP(pastrawdzmem - dzpixenc, 0, 4);
        }
        pastrawdzmem = rawdzmem;

        precision_factor = (zval >> 13) & 0xf;

        if (precision_factor < 3)
        {
            if (dzmem != 0x8000)
            {
                dzmemmodifier = 16 >> precision_factor;
                dzmem <<= 1;
                if (dzmem < dzmemmodifier)
                    dzmem = dzmemmodifier;
            }
            else
            {
                force_coplanar = 1;
                dzmem = 0xffff;
            }
            
        }

        dznew = (uint32_t)deltaz_comparator_lut[dzpix | dzmem];
        dznotshift = dznew;
        dznew <<= 3;
        
        LOG("dznew = %d\n", dznew);

        farther = force_coplanar || ((sz + dznew) >= oz);
        
        overflow = (curpixel_memcvg + *curpixel_cvg) & 8;
        *blend_en = other_modes.force_blend || (!overflow && other_modes.antialias_en && farther);

        LOG("force_blend = %d\n", other_modes.force_blend);
        LOG("oz = %d\n", oz);
        LOG("blend_en = %d\n", *blend_en);
        LOG("overflow = %d\n", overflow);
        
        *prewrap = overflow;

        switch(other_modes.z_mode)
        {
        case ZMODE_OPAQUE: 
            infront = sz < oz;
            diff = (int32_t)sz - (int32_t)dznew;
            nearer = force_coplanar || (diff <= (int32_t)oz);
            max = (oz == 0x3ffff);
            LOG("nearer = %d\n", nearer);
            LOG("opaque\n");
            return (max || (overflow ? infront : nearer));
            break;
        case ZMODE_INTERPENETRATING: 
            infront = sz < oz;
            LOG("inter\n");
            if (!infront || !farther || !overflow)
            {
                diff = (int32_t)sz - (int32_t)dznew;
                nearer = force_coplanar || (diff <= (int32_t)oz);
                max = (oz == 0x3ffff);
                return (max || (overflow ? infront : nearer)); 
            }
            else
            {
                dzenc = dz_compress(dznotshift & 0xffff);
                cvgcoeff = ((oz >> dzenc) - (sz >> dzenc)) & 0xf;
                *curpixel_cvg = ((cvgcoeff * (*curpixel_cvg)) >> 3) & 0xf;
                return 1;
            }
            break;
        case ZMODE_TRANSPARENT: 
            LOG("trans\n");
            infront = sz < oz;
            max = (oz == 0x3ffff);
            return (infront || max); 
            break;
        case ZMODE_DECAL: 
            LOG("decal\n");
            diff = (int32_t)sz - (int32_t)dznew;
            nearer = force_coplanar || (diff <= (int32_t)oz);
            max = (oz == 0x3ffff);
            return (farther && nearer && !max); 
            break;
        }
        return 0;
    }
    else
    {
       int overflow;

       if (other_modes.f.realblendershiftersneeded)
       {
          blshifta = 0;
          if (dzpixenc < 0xb)
             blshiftb = 4;
          else
             blshiftb = 0xf - dzpixenc;
       }

       if (other_modes.f.interpixelblendershiftersneeded)
       {
          pastblshifta = 0;
          if (dzpixenc < 0xb)
             pastblshiftb = 4;
          else
             pastblshiftb = 0xf - dzpixenc;
       }
       pastrawdzmem = 0xf;

       overflow  = (curpixel_memcvg + *curpixel_cvg) & 8;
       *blend_en = other_modes.force_blend || (!overflow && other_modes.antialias_en);
       *prewrap  = overflow;

       return 1;
    }
}

static void get_dither_noise(int x, int y, int* cdith, int* adith)
{
   if (get_dither_noise_type < 2)
   {
      int dithindex = ((y & 3) << 2) | (x & 3);

      if (!get_dither_noise_type)
         noise = ((irand() & 7) << 6) | 0x20;

      switch(other_modes.f.rgb_alpha_dither)
      {
         case 0:
            *adith = *cdith = magic_matrix[dithindex];
            break;
         case 1:
            *cdith = magic_matrix[dithindex];
            *adith = (~(*cdith)) & 7;
            break;
         case 2:
            *cdith = magic_matrix[dithindex];
            *adith = (noise >> 6) & 7;
            break;
         case 3:
            *cdith = magic_matrix[dithindex];
            *adith = 0;
            break;
         case 4:
            *adith = *cdith = bayer_matrix[dithindex];
            break;
         case 5:
            *cdith = bayer_matrix[dithindex];
            *adith = (~(*cdith)) & 7;
            break;
         case 6:
            *cdith = bayer_matrix[dithindex];
            *adith = (noise >> 6) & 7;
            break;
         case 7:
            *cdith = bayer_matrix[dithindex];
            *adith = 0;
            break;
         case 8:
            *cdith = irand();
            *adith = magic_matrix[dithindex];
            break;
         case 9:
            *cdith = irand();
            *adith = (~magic_matrix[dithindex]) & 7;
            break;
         case 10:
            *cdith = irand();
            *adith = (noise >> 6) & 7;
            break;
         case 11:
            *cdith = irand();
            *adith = 0;
            break;
         case 12:
            *cdith = 7;
            *adith = bayer_matrix[dithindex];
            break;
         case 13:
            *cdith = 7;
            *adith = (~bayer_matrix[dithindex]) & 7;
            break;
         case 14:
            *cdith = 7;
            *adith = (noise >> 6) & 7;
            break;
         case 15:
            *cdith = 7;
            *adith = 0;
            break;
      }
   }
}

static STRICTINLINE void get_texel1_1cycle(int32_t* s1, int32_t* t1, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t scanline, SPANSIGS* sigs)
{
    int32_t nexts, nextt, nextsw;
    
    if (!sigs->endspan || !sigs->longspan || !span[scanline + 1].validline)
    {
    
    
        nextsw = (w + dwinc) >> 16;
        nexts = (s + dsinc) >> 16;
        nextt = (t + dtinc) >> 16;
    }
    else
    {
        int32_t nextscan = scanline + 1;
        ALIGNED int32_t *stwz_ptr = (ALIGNED int32_t*)&span[nextscan].stwz[0];

        nexts  = stwz_ptr[0] >> 16;
        nextt  = stwz_ptr[1] >> 16;
        nextsw = stwz_ptr[2] >> 16;
    }
    tcdiv(nexts, nextt, nextsw, s1, t1);
}

STRICTINLINE void lodfrac_lodtile_signals(int lodclamp, int32_t lod, uint32_t* l_tile, uint32_t* magnify, uint32_t* distant)
{
    uint32_t ltil, dis, mag;
    int32_t lf;

    
    if ((lod & 0x4000) || lodclamp)
        lod = 0x7fff;
    else if (lod < min_level)
        lod = min_level;
                        
    mag = (lod < 32) ? 1: 0;
    ltil=  log2table[(lod >> 5) & 0xff];
    dis = ((lod & 0x6000) || (ltil >= max_level)) ? 1 : 0;
                        
    lf = ((lod << 3) >> ltil) & 0xff;

    
    if(!other_modes.sharpen_tex_en && !other_modes.detail_tex_en)
    {
        if (dis)
            lf = 0xff;
        else if (mag)
            lf = 0;
    }

    
    

    if(other_modes.sharpen_tex_en && mag)
        lf |= 0x100;

    *distant = dis;
    *l_tile = ltil;
    *magnify = mag;
    lod_frac = lf;
}

static STRICTINLINE void tclod_4x17_to_15(int32_t scurr, int32_t snext, int32_t tcurr, int32_t tnext, int32_t previous, int32_t* lod)
{
    int dels = SIGN(snext, 17) - SIGN(scurr, 17);
    int delt = SIGN(tnext, 17) - SIGN(tcurr, 17);

    if (dels & 0x20000)
        dels = ~dels & 0x1ffff;
    if (delt & 0x20000)
        delt = ~delt & 0x1ffff;

    dels = (dels > delt) ? dels : delt;
    dels = (previous > dels) ? previous : dels;
    *lod = dels & 0x7fff;
    if (dels & 0x1c000)
        *lod |= 0x4000;
}

static STRICTINLINE void tclod_tcclamp(int32_t* sss, int32_t* sst)
{
    int32_t tempanded, temps = *sss, tempt = *sst;

    
    
    
    
    if (!(temps & 0x40000))
    {
        if (!(temps & 0x20000))
        {
            tempanded = temps & 0x18000;
            if (tempanded != 0x8000)
            {
                if (tempanded != 0x10000)
                    *sss &= 0xffff;
                else
                    *sss = 0x8000;
            }
            else
                *sss = 0x7fff;
        }
        else
            *sss = 0x8000;
    }
    else
        *sss = 0x7fff;

    if (!(tempt & 0x40000))
    {
        if (!(tempt & 0x20000))
        {
            tempanded = tempt & 0x18000;
            if (tempanded != 0x8000)
            {
                if (tempanded != 0x10000)
                    *sst &= 0xffff;
                else
                    *sst = 0x8000;
            }
            else
                *sst = 0x7fff;
        }
        else
            *sst = 0x8000;
    }
    else
        *sst = 0x7fff;

}

static void tclod_1cycle_current(int32_t* sss, int32_t* sst, int32_t nexts, int32_t nextt, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t scanline, int32_t prim_tile, int32_t* t1, SPANSIGS* sigs)
{









    int fars, fart, farsw;
    int lodclamp = 0;
    int32_t lod = 0;
    uint32_t l_tile = 0, magnify = 0, distant = 0;
    
    tclod_tcclamp(sss, sst);

    if (other_modes.f.dolod)
    {
        int nextscan = scanline + 1;

        
        if (span[nextscan].validline)
        {
            if (!sigs->endspan || !sigs->longspan)
            {
                if (!(sigs->preendspan && sigs->longspan) && !(sigs->endspan && sigs->midspan))
                {
                    farsw = (w + (dwinc << 1)) >> 16;
                    fars = (s + (dsinc << 1)) >> 16;
                    fart = (t + (dtinc << 1)) >> 16;
                }
                else
                {
                    farsw = (w - dwinc) >> 16;
                    fars = (s - dsinc) >> 16;
                    fart = (t - dtinc) >> 16;
                }
            }
            else
            {
               ALIGNED int32_t *stwz_ptr = (ALIGNED int32_t*)&span[nextscan].stwz[0];
                fars =  (stwz_ptr[0] + dsinc) >> 16;
                fart =  (stwz_ptr[1] + dtinc) >> 16;
                farsw = (stwz_ptr[2] + dwinc) >> 16;
            }
        }
        else
        {
            farsw = (w + (dwinc << 1)) >> 16;
            fars = (s + (dsinc << 1)) >> 16;
            fart = (t + (dtinc << 1)) >> 16;
        }

        tcdiv(fars, fart, farsw, &fars, &fart);

        lodclamp = (fart & 0x60000) || (nextt & 0x60000) || (fars & 0x60000) || (nexts & 0x60000);
        
        

        
        tclod_4x17_to_15(nexts, fars, nextt, fart, 0, &lod);

        lodfrac_lodtile_signals(lodclamp, lod, &l_tile, &magnify, &distant);
    
        if (other_modes.tex_lod_en)
        {
            if (distant)
                l_tile = max_level;

            
            
            if (!other_modes.detail_tex_en || magnify)
                *t1 = (prim_tile + l_tile) & 7;
            else
                *t1 = (prim_tile + l_tile + 1) & 7;
        }
    }
}



static void tclod_1cycle_current_simple(int32_t* sss, int32_t* sst, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t scanline, int32_t prim_tile, int32_t* t1, SPANSIGS* sigs)
{
    int fars, fart, farsw, nexts, nextt, nextsw;
    int lodclamp = 0;
    int32_t lod = 0;
    uint32_t l_tile = 0, magnify = 0, distant = 0;
    
    tclod_tcclamp(sss, sst);

    if (other_modes.f.dolod)
    {

        int nextscan = scanline + 1;
        if (span[nextscan].validline)
        {
            if (!sigs->endspan || !sigs->longspan)
            {
                nextsw = (w + dwinc) >> 16;
                nexts = (s + dsinc) >> 16;
                nextt = (t + dtinc) >> 16;
                
                if (!(sigs->preendspan && sigs->longspan) && !(sigs->endspan && sigs->midspan))
                {
                    farsw = (w + (dwinc << 1)) >> 16;
                    fars = (s + (dsinc << 1)) >> 16;
                    fart = (t + (dtinc << 1)) >> 16;
                }
                else
                {
                    farsw = (w - dwinc) >> 16;
                    fars = (s - dsinc) >> 16;
                    fart = (t - dtinc) >> 16;
                }
            }
            else
            {
               ALIGNED int32_t *stwz_ptr = (ALIGNED int32_t*)&span[nextscan].stwz[0];

                nexts  = stwz_ptr[0] >> 16;
                nextt  = stwz_ptr[1] >> 16;
                nextsw = stwz_ptr[2] >> 16;
                fars   = (stwz_ptr[0] + dsinc) >> 16;
                fart   = (stwz_ptr[1] + dtinc) >> 16;
                farsw  = (stwz_ptr[2] + dwinc) >> 16;
            }
        }
        else
        {
            nextsw = (w + dwinc) >> 16;
            nexts = (s + dsinc) >> 16;
            nextt = (t + dtinc) >> 16;
            farsw = (w + (dwinc << 1)) >> 16;
            fars = (s + (dsinc << 1)) >> 16;
            fart = (t + (dtinc << 1)) >> 16;
        }

        tcdiv(nexts, nextt, nextsw, &nexts, &nextt);
        tcdiv(fars, fart, farsw, &fars, &fart);

        lodclamp = (fart & 0x60000) || (nextt & 0x60000) || (fars & 0x60000) || (nexts & 0x60000);

        tclod_4x17_to_15(nexts, fars, nextt, fart, 0, &lod);

        lodfrac_lodtile_signals(lodclamp, lod, &l_tile, &magnify, &distant);
    
        if (other_modes.tex_lod_en)
        {
            if (distant)
                l_tile = max_level;
            if (!other_modes.detail_tex_en || magnify)
                *t1 = (prim_tile + l_tile) & 7;
            else
                *t1 = (prim_tile + l_tile + 1) & 7;
        }
    }
}

static void tclod_1cycle_next(int32_t* sss, int32_t* sst, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t scanline, int32_t prim_tile, int32_t* t1, SPANSIGS* sigs, int32_t* prelodfrac)
{
    int nexts, nextt, nextsw, fars, fart, farsw;
    int lodclamp = 0;
    int32_t lod = 0;
    uint32_t l_tile = 0, magnify = 0, distant = 0;
    
    tclod_tcclamp(sss, sst);

    if (other_modes.f.dolod)
    {
        
        int nextscan = scanline + 1;
        
        if (span[nextscan].validline)
        {
            if (!sigs->nextspan)
            {
                if (!sigs->endspan || !sigs->longspan)
                {
                    nextsw = (w + dwinc) >> 16;
                    nexts = (s + dsinc) >> 16;
                    nextt = (t + dtinc) >> 16;
                    
                    if (!(sigs->preendspan && sigs->longspan) && !(sigs->endspan && sigs->midspan))
                    {
                        farsw = (w + (dwinc << 1)) >> 16;
                        fars = (s + (dsinc << 1)) >> 16;
                        fart = (t + (dtinc << 1)) >> 16;
                    }
                    else
                    {
                        farsw = (w - dwinc) >> 16;
                        fars = (s - dsinc) >> 16;
                        fart = (t - dtinc) >> 16;
                    }
                }
                else
                {
                    nexts = span[nextscan].stwz[0];
                    nextt = span[nextscan].stwz[1];
                    nextsw = span[nextscan].stwz[2];
                    fart = (nextt + dtinc) >> 16;
                    fars = (nexts + dsinc) >> 16;
                    farsw = (nextsw + dwinc) >> 16;
                    nextt >>= 16;
                    nexts >>= 16;
                    nextsw >>= 16;
                }
            }
            else
            {
               if (!sigs->onelessthanmid)
                {
                    nexts = span[nextscan].stwz[0] + dsinc;
                    nextt = span[nextscan].stwz[1] + dtinc;
                    nextsw = span[nextscan].stwz[2] + dwinc;
                    fart = (nextt + dtinc) >> 16;
                    fars = (nexts + dsinc) >> 16;
                    farsw = (nextsw + dwinc) >> 16;
                    nextt >>= 16;
                    nexts >>= 16;
                    nextsw >>= 16;
                }
                else
                {
                    nextsw = (w + dwinc) >> 16;
                    nexts = (s + dsinc) >> 16;
                    nextt = (t + dtinc) >> 16;
                    farsw = (w - dwinc) >> 16;
                    fars = (s - dsinc) >> 16;
                    fart = (t - dtinc) >> 16;
                }
            }
        }
        else
        {
            nextsw = (w + dwinc) >> 16;
            nexts = (s + dsinc) >> 16;
            nextt = (t + dtinc) >> 16;
            farsw = (w + (dwinc << 1)) >> 16;
            fars = (s + (dsinc << 1)) >> 16;
            fart = (t + (dtinc << 1)) >> 16;
        }

        tcdiv(nexts, nextt, nextsw, &nexts, &nextt);
        tcdiv(fars, fart, farsw, &fars, &fart);

        lodclamp = (fart & 0x60000) || (nextt & 0x60000) || (fars & 0x60000) || (nexts & 0x60000);
        
        
        tclod_4x17_to_15(nexts, fars, nextt, fart, 0, &lod);

        
        if ((lod & 0x4000) || lodclamp)
            lod = 0x7fff;
        else if (lod < min_level)
            lod = min_level;
                    
        magnify = (lod < 32) ? 1: 0;
        l_tile =  log2table[(lod >> 5) & 0xff];
        distant = ((lod & 0x6000) || (l_tile >= max_level)) ? 1 : 0;

        *prelodfrac = ((lod << 3) >> l_tile) & 0xff;

        
        if(!other_modes.sharpen_tex_en && !other_modes.detail_tex_en)
        {
#ifdef OPTS_ENABLED
           *prelodfrac &= ~magnify;
           *prelodfrac |=  distant;
#else
           if (distant)
              *prelodfrac = 0xff;
           else if (magnify)
              *prelodfrac = 0;
#endif
        }

        if(other_modes.sharpen_tex_en && magnify)
            *prelodfrac |= 0x100;

        if (other_modes.tex_lod_en)
        {
            if (distant)
                l_tile = max_level;
            if (!other_modes.detail_tex_en || magnify)
                *t1 = (prim_tile + l_tile) & 7;
            else
                *t1 = (prim_tile + l_tile + 1) & 7;
        }
    }
}

static void rgbaz_correct_clip(int offx, int offy, int r, int g, int b, int a,
      int* z, uint32_t curpixel_cvg)
{
    int summand_r, summand_b, summand_g, summand_a;
    int summand_z;
    int sz = *z;
    int zanded;




    if (curpixel_cvg == 8)
    {
        r >>= 2;
        g >>= 2;
        b >>= 2;
        a >>= 2;
        sz = sz >> 3;
    }
    else
    {
        summand_r = offx * spans_cd_rgba[0] + offy * spans_d_rgba_dy[0];
        summand_g = offx * spans_cd_rgba[1] + offy * spans_d_rgba_dy[1];
        summand_b = offx * spans_cd_rgba[2] + offy * spans_d_rgba_dy[2];
        summand_a = offx * spans_cd_rgba[3] + offy * spans_d_rgba_dy[3];
        summand_z = offx * spans_cdz + offy * spans_d_stwz_dy[3];

        r = ((r << 2) + summand_r) >> 4;
        g = ((g << 2) + summand_g) >> 4;
        b = ((b << 2) + summand_b) >> 4;
        a = ((a << 2) + summand_a) >> 4;
        sz = ((sz << 2) + summand_z) >> 5;
    }

    
    COLOR_RED(shade_color)   = special_9bit_clamptable[r & 0x1ff];
    COLOR_GREEN(shade_color) = special_9bit_clamptable[g & 0x1ff];
    COLOR_BLUE(shade_color)  = special_9bit_clamptable[b & 0x1ff];
    COLOR_ALPHA(shade_color) = special_9bit_clamptable[a & 0x1ff];
    
    
    
    zanded = (sz & 0x60000) >> 17;

    
    switch(zanded)
    {
        case 0:
        case 1:
           *z = sz;
           break;
        case 2:
        case 3:
           *z = (0x3FFFD + zanded);
           break;
    }

    *z &= 0x3FFFF;
}

static void render_spans_1cycle_complete(int start, int end, int tilenum, int flip)
{
    uint8_t offx, offy;
    SPANSIGS sigs;
    uint32_t blend_en;
    uint32_t prewrap;
    uint32_t curpixel_cvg, curpixel_cvbit, curpixel_memcvg;
    unsigned long zbcur;

    int prim_tile = tilenum;
    int tile1 = tilenum;
    int newtile = tilenum; 
    int news, newt;

    int i, j;
    int drinc, dginc, dbinc, dainc, dzinc, dsinc, dtinc, dwinc;
    int xinc;

    int dzpix;
    int dzpixenc;

    int cdith = 7, adith = 0;
    int r, g, b, a, z, s, t, w;
    int sr, sg, sb, sa, sz, ss, st, sw;
    int xstart, xend, xendsc;
    int sss = 0, sst = 0;
    int32_t prelodfrac;
    int curpixel = 0;
    int x, length, scdiff;
    uint32_t fir, fig, fib;

    if (flip)
    {
        drinc = spans_d_rgba[0];
        dginc = spans_d_rgba[1];
        dbinc = spans_d_rgba[2];
        dainc = spans_d_rgba[3];
        dsinc = spans_d_stwz[0];
        dtinc = spans_d_stwz[1];
        dwinc = spans_d_stwz[2];
        dzinc = spans_d_stwz[3];
        xinc = 1;
    }
    else
    {
        drinc = -spans_d_rgba[0];
        dginc = -spans_d_rgba[1];
        dbinc = -spans_d_rgba[2];
        dainc = -spans_d_rgba[3];
        dsinc = -spans_d_stwz[0];
        dtinc = -spans_d_stwz[1];
        dwinc = -spans_d_stwz[2];
        dzinc = -spans_d_stwz[3];
        xinc = -1;
    }

    if (!other_modes.z_source_sel)
        dzpix = spans_dzpix;
    else
    {
        dzpix = primitive_delta_z;
        dzinc = spans_cdz = spans_d_stwz_dy[3] = 0;
    }
    dzpixenc = dz_compress(dzpix);

    for (i = start; i <= end; i++)
    {
       SPAN *span_ptr = &span[i];
        if (!span_ptr || span_ptr->validline == 0)
            continue;
        xstart = span_ptr->lx;
        xend   = span_ptr->unscrx;
        xendsc = span_ptr->rx;
        r      = span_ptr->rgba[0];
        g      = span_ptr->rgba[1];
        b      = span_ptr->rgba[2];
        a      = span_ptr->rgba[3];
        s      = span_ptr->stwz[0];
        t      = span_ptr->stwz[1];
        w      = span_ptr->stwz[2];
        z      = other_modes.z_source_sel ? primitive_z : span_ptr->stwz[3];

        x = xendsc;
        curpixel = fb_width * i + x;
        zbcur  = zb_address + 2*curpixel;
        zbcur &= 0x00FFFFFF;
        zbcur  = zbcur >> 1;

        if (!flip)
        {
            length = xendsc - xstart;
            scdiff = xend - xendsc;
            compute_cvg_noflip(i);
        }
        else
        {
            length = xstart - xendsc;
            scdiff = xendsc - xend;
            compute_cvg_flip(i);
        }
        sigs.longspan = (length > 7);
        sigs.midspan = (length == 7);
        sigs.onelessthanmid = (length == 6);

        if (scdiff)
        {
            r += (drinc * scdiff);
            g += (dginc * scdiff);
            b += (dbinc * scdiff);
            a += (dainc * scdiff);
            z += (dzinc * scdiff);
            s += (dsinc * scdiff);
            t += (dtinc * scdiff);
            w += (dwinc * scdiff);
        }
        sigs.startspan = 1;

        for (j = 0; j <= length; j++)
        {
            sr = r >> 14;
            sg = g >> 14;
            sb = b >> 14;
            sa = a >> 14;
            ss = s >> 16;
            st = t >> 16;
            sw = w >> 16;
            sz = (z >> 10) & 0x3fffff;

            sigs.endspan = (j == length);
            sigs.preendspan = (j == (length - 1));

            lookup_cvmask_derivatives(cvgbuf[x], &offx, &offy, &curpixel_cvg, &curpixel_cvbit);

            get_texel1_1cycle(&news, &newt, s, t, w, dsinc, dtinc, dwinc, i, &sigs);

            if (!sigs.startspan)
            {
                COLOR_ASSIGN(texel0_color, texel1_color);
                lod_frac = prelodfrac;
            }
            else
            {
                tcdiv(ss, st, sw, &sss, &sst);

                tclod_1cycle_current(&sss, &sst, news, newt, s, t, w, dsinc, dtinc, dwinc, i, prim_tile, &tile1, &sigs);
                texture_pipeline_cycle(&texel0_color, &texel0_color, sss, sst, tile1, 0);

                sigs.startspan = 0;
            }
            sigs.nextspan = sigs.endspan;
            sigs.endspan = sigs.preendspan;
            sigs.preendspan = (j == length - 2);

            s += dsinc;
            t += dtinc;
            w += dwinc;

            tclod_1cycle_next(&news, &newt, s, t, w, dsinc, dtinc, dwinc, i, prim_tile, &newtile, &sigs, &prelodfrac);

            texture_pipeline_cycle(&texel1_color, &texel1_color, news, newt, newtile, 0);

            rgbaz_correct_clip(offx, offy, sr, sg, sb, sa, &sz, curpixel_cvg);
            LOG("SZ = %d\n", sz);
            get_dither_noise(x, i, &cdith, &adith);
            combiner_1cycle(adith, &curpixel_cvg);
            fbread1_ptr(curpixel, &curpixel_memcvg);
            if (z_compare(zbcur, sz, dzpix, dzpixenc, &blend_en, &prewrap, &curpixel_cvg, curpixel_memcvg))
            {
                if (blender_1cycle(&fir, &fig, &fib, cdith, blend_en, prewrap, curpixel_cvg, curpixel_cvbit))
                {
                    fbwrite_ptr(curpixel, fir, fig, fib, blend_en, curpixel_cvg, curpixel_memcvg);
                    if (other_modes.z_update_en)
                        z_store(zbcur, sz, dzpixenc);
                }
            }
            r += drinc;
            g += dginc;
            b += dbinc;
            a += dainc;
            z += dzinc;
            x += xinc;
            curpixel += xinc;
            zbcur += xinc;
            zbcur &= 0x00FFFFFF >> 1;
        }
    }
}

static void render_spans_1cycle_notexel1(int start, int end, int tilenum, int flip)
{
    int zbcur;
    uint8_t offx, offy;
    SPANSIGS sigs;
    uint32_t blend_en;
    uint32_t prewrap;
    uint32_t curpixel_cvg, curpixel_cvbit, curpixel_memcvg;

    int prim_tile = tilenum;
    int tile1 = tilenum;

    int i, j;

    int drinc, dginc, dbinc, dainc, dzinc, dsinc, dtinc, dwinc;
    int xinc;

    int dzpix;
    int dzpixenc;

    int cdith = 7, adith = 0;
    int r, g, b, a, z, s, t, w;
    int sr, sg, sb, sa, sz, ss, st, sw;
    int xstart, xend, xendsc;
    int sss = 0, sst = 0;
    int curpixel = 0;
    int x, length, scdiff;
    uint32_t fir, fig, fib;

    if (flip)
    {
        drinc = spans_d_rgba[0];
        dginc = spans_d_rgba[1];
        dbinc = spans_d_rgba[2];
        dainc = spans_d_rgba[3];
        dsinc = spans_d_stwz[0];
        dtinc = spans_d_stwz[1];
        dwinc = spans_d_stwz[2];
        dzinc = spans_d_stwz[3];
        xinc = 1;
    }
    else
    {
        drinc = -spans_d_rgba[0];
        dginc = -spans_d_rgba[1];
        dbinc = -spans_d_rgba[2];
        dainc = -spans_d_rgba[3];
        dsinc = -spans_d_stwz[0];
        dtinc = -spans_d_stwz[1];
        dwinc = -spans_d_stwz[2];
        dzinc = -spans_d_stwz[3];
        xinc = -1;
    }

    if (!other_modes.z_source_sel)
        dzpix = spans_dzpix;
    else
    {
        dzpix = primitive_delta_z;
        dzinc = spans_cdz = spans_d_stwz_dy[3] = 0;
    }
    dzpixenc = dz_compress(dzpix);
                    
    for (i = start; i <= end; i++)
    {
       SPAN *span_ptr = &span[i];
        if (!span_ptr || span_ptr->validline == 0)
            continue;
        xstart = span_ptr->lx;
        xend   = span_ptr->unscrx;
        xendsc = span_ptr->rx;
        r      = span_ptr->rgba[0];
        g      = span_ptr->rgba[1];
        b      = span_ptr->rgba[2];
        a      = span_ptr->rgba[3];
        s      = span_ptr->stwz[0];
        t      = span_ptr->stwz[1];
        w      = span_ptr->stwz[2];
        z      = other_modes.z_source_sel ? primitive_z : span_ptr->stwz[3];
        
        x = xendsc;
        curpixel = fb_width * i + x;
        zbcur  = zb_address + 2*curpixel;
        zbcur &= 0x00FFFFFF;
        zbcur  = zbcur >> 1;

        if (!flip)
        {
            length = xendsc - xstart;
            scdiff = xend - xendsc;
            compute_cvg_noflip(i);
        }
        else
        {
            length = xstart - xendsc;
            scdiff = xendsc - xend;
            compute_cvg_flip(i);
        }

        sigs.longspan = (length > 7);
        sigs.midspan = (length == 7);

        if (scdiff)
        {
            r += (drinc * scdiff);
            g += (dginc * scdiff);
            b += (dbinc * scdiff);
            a += (dainc * scdiff);
            z += (dzinc * scdiff);
            s += (dsinc * scdiff);
            t += (dtinc * scdiff);
            w += (dwinc * scdiff);
        }

        for (j = 0; j <= length; j++)
        {
            sr = r >> 14;
            sg = g >> 14;
            sb = b >> 14;
            sa = a >> 14;
            ss = s >> 16;
            st = t >> 16;
            sw = w >> 16;
            sz = (z >> 10) & 0x3fffff;

            sigs.endspan = (j == length);
            sigs.preendspan = (j == (length - 1));

            lookup_cvmask_derivatives(cvgbuf[x], &offx, &offy, &curpixel_cvg, &curpixel_cvbit);

            tcdiv(ss, st, sw, &sss, &sst);

            tclod_1cycle_current_simple(&sss, &sst, s, t, w, dsinc, dtinc, dwinc, i, prim_tile, &tile1, &sigs);

            texture_pipeline_cycle(&texel0_color, &texel0_color, sss, sst, tile1, 0);

#ifdef EXTRALOGGING
            LOG_ENABLE = curpixel == 53 * 320 + 77;
            LOG("Preclip SZ = %d\n", sz >> 3);
#endif
            rgbaz_correct_clip(offx, offy, sr, sg, sb, sa, &sz, curpixel_cvg);
#ifdef EXTRALOGGING
            LOG("SZ = %d\n", sz);
#endif

            get_dither_noise(x, i, &cdith, &adith);
            combiner_1cycle(adith, &curpixel_cvg);
                
            fbread1_ptr(curpixel, &curpixel_memcvg);
            LOG("Pre CVG: %d, MEMCVG: %d\n", curpixel_cvg, curpixel_memcvg);
            if (z_compare(zbcur, sz, dzpix, dzpixenc, &blend_en, &prewrap, &curpixel_cvg, curpixel_memcvg))
            {
               LOG("Z pass\n");
                if (blender_1cycle(&fir, &fig, &fib, cdith, blend_en, prewrap, curpixel_cvg, curpixel_cvbit))
                {
                   LOG("Blend pass (CVG: %d)\n", curpixel_cvg);
                    fbwrite_ptr(curpixel, fir, fig, fib, blend_en, curpixel_cvg, curpixel_memcvg);
                    if (other_modes.z_update_en)
                        z_store(zbcur, sz, dzpixenc);
                }
            }
            else
               LOG("Z fail\n");
            LOG("\n");

            s += dsinc;
            t += dtinc;
            w += dwinc;
            r += drinc;
            g += dginc;
            b += dbinc;
            a += dainc;
            z += dzinc;

            x += xinc;
            curpixel += xinc;
            zbcur += xinc;
            zbcur &= 0x00FFFFFF >> 1;
#ifdef EXTRALOGGING
            LOG_ENABLE = 0;
#endif
        }
    }
}

static void render_spans_1cycle_notex(int start, int end, int tilenum, int flip)
{
    int zbcur;
    uint8_t offx, offy;
    uint32_t blend_en;
    uint32_t prewrap;
    uint32_t curpixel_cvg, curpixel_cvbit, curpixel_memcvg;

    int i, j;

    int drinc, dginc, dbinc, dainc, dzinc;
    int xinc;

    int dzpix;
    int dzpixenc;

    int cdith = 7, adith = 0;
    int r, g, b, a, z;
    int sr, sg, sb, sa, sz;
    int xstart, xend, xendsc;
    int curpixel = 0;
    int x, length, scdiff;
    uint32_t fir, fig, fib;

    if (flip)
    {
        drinc = spans_d_rgba[0];
        dginc = spans_d_rgba[1];
        dbinc = spans_d_rgba[2];
        dainc = spans_d_rgba[3];
        dzinc = spans_d_stwz[3];
        xinc = 1;
    }
    else
    {
        drinc = -spans_d_rgba[0];
        dginc = -spans_d_rgba[1];
        dbinc = -spans_d_rgba[2];
        dainc = -spans_d_rgba[3];
        dzinc = -spans_d_stwz[3];
        xinc = -1;
    }
    
    if (!other_modes.z_source_sel)
        dzpix = spans_dzpix;
    else
    {
        dzpix = primitive_delta_z;
        dzinc = spans_cdz = spans_d_stwz_dy[3] = 0;
    }
    dzpixenc = dz_compress(dzpix);
                    
    for (i = start; i <= end; i++)
    {
       SPAN *span_ptr = &span[i];
        if (!span_ptr || span_ptr->validline == 0)
            continue;
        xstart = span_ptr->lx;
        xend   = span_ptr->unscrx;
        xendsc = span_ptr->rx;
        r      = span_ptr->rgba[0];
        g      = span_ptr->rgba[1];
        b      = span_ptr->rgba[2];
        a      = span_ptr->rgba[3];
        z      = other_modes.z_source_sel ? primitive_z : span_ptr->stwz[3];

        x = xendsc;
        curpixel = fb_width * i + x;
        zbcur  = zb_address + 2*curpixel;
        zbcur &= 0x00FFFFFF;
        zbcur  = zbcur >> 1;

        if (!flip)
        {
            length = xendsc - xstart;
            scdiff = xend - xendsc;
            compute_cvg_noflip(i);
        }
        else
        {
            length = xstart - xendsc;
            scdiff = xendsc - xend;
            compute_cvg_flip(i);
        }

        if (scdiff)
        {
            r += (drinc * scdiff);
            g += (dginc * scdiff);
            b += (dbinc * scdiff);
            a += (dainc * scdiff);
            z += (dzinc * scdiff);
        }

        for (j = 0; j <= length; j++)
        {
            sr = r >> 14;
            sg = g >> 14;
            sb = b >> 14;
            sa = a >> 14;
            sz = (z >> 10) & 0x3fffff;

            lookup_cvmask_derivatives(cvgbuf[x], &offx, &offy, &curpixel_cvg, &curpixel_cvbit);

#ifdef EXTRALOGGING
            LOG_ENABLE = curpixel == 53 * 320 + 77;
            LOG("Preclip SZ = %d\n", sz >> 3);
#endif
            rgbaz_correct_clip(offx, offy, sr, sg, sb, sa, &sz, curpixel_cvg);
            LOG("SZ = %d\n", sz);
            get_dither_noise(x, i, &cdith, &adith);
            combiner_1cycle(adith, &curpixel_cvg);
                
            fbread1_ptr(curpixel, &curpixel_memcvg);
            if (z_compare(zbcur, sz, dzpix, dzpixenc, &blend_en, &prewrap, &curpixel_cvg, curpixel_memcvg))
            {
                if (blender_1cycle(&fir, &fig, &fib, cdith, blend_en, prewrap, curpixel_cvg, curpixel_cvbit))
                {
                    fbwrite_ptr(curpixel, fir, fig, fib, blend_en, curpixel_cvg, curpixel_memcvg);
                    if (other_modes.z_update_en)
                        z_store(zbcur, sz, dzpixenc);
                }
            }
            r += drinc;
            g += dginc;
            b += dbinc;
            a += dainc;
            z += dzinc;

            x += xinc;
            curpixel += xinc;
            zbcur += xinc;
            zbcur &= 0x00FFFFFF >> 1;
#ifdef EXTRALOGGING
            LOG_ENABLE = 0;
#endif
        }
    }
}

STRICTINLINE void get_nexttexel0_2cycle(int32_t* s1, int32_t* t1, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc)
{
    int32_t nexts, nextt, nextsw;
    nextsw = (w + dwinc) >> 16;
    nexts = (s + dsinc) >> 16;
    nextt = (t + dtinc) >> 16;

    tcdiv(nexts, nextt, nextsw, s1, t1);
}

static void tclod_2cycle_current(int32_t* sss, int32_t* sst, int32_t nexts, int32_t nextt, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t prim_tile, int32_t* t1, int32_t* t2)
{








    int nextys, nextyt, nextysw;
    int lodclamp = 0;
    int32_t lod = 0;
    uint32_t l_tile;
    uint32_t magnify = 0;
    uint32_t distant = 0;
    int inits = *sss, initt = *sst;

    tclod_tcclamp(sss, sst);

    if (other_modes.f.dolod)
    {
        
        
        
        
        
        
        nextys = (s + spans_d_stwz_dy[0]) >> 16;
        nextyt = (t + spans_d_stwz_dy[1]) >> 16;
        nextysw = (w + spans_d_stwz_dy[2]) >> 16;

        tcdiv(nextys, nextyt, nextysw, &nextys, &nextyt);

        lodclamp = (initt & 0x60000) || (nextt & 0x60000) || (inits & 0x60000) || (nexts & 0x60000) || (nextys & 0x60000) || (nextyt & 0x60000);
        
        

        
        tclod_4x17_to_15(inits, nexts, initt, nextt, 0, &lod);
        tclod_4x17_to_15(inits, nextys, initt, nextyt, lod, &lod);

        lodfrac_lodtile_signals(lodclamp, lod, &l_tile, &magnify, &distant);

        
        if (other_modes.tex_lod_en)
        {
            if (distant)
                l_tile = max_level;
            if (!other_modes.detail_tex_en)
            {
                *t1 = (prim_tile + l_tile) & 7;
                if (!(distant || (!other_modes.sharpen_tex_en && magnify)))
                    *t2 = (*t1 + 1) & 7;
                else
                    *t2 = *t1;
            }
            else 
            {
                if (!magnify)
                    *t1 = (prim_tile + l_tile + 1);
                else
                    *t1 = (prim_tile + l_tile);
                *t1 &= 7;
                if (!distant && !magnify)
                    *t2 = (prim_tile + l_tile + 2) & 7;
                else
                    *t2 = (prim_tile + l_tile + 1) & 7;
            }
        }
    }
}


static void tclod_2cycle_current_simple(int32_t* sss, int32_t* sst, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t prim_tile, int32_t* t1, int32_t* t2)
{
    int nextys, nextyt, nextysw, nexts, nextt, nextsw;
    int lodclamp = 0;
    int32_t lod = 0;
    uint32_t l_tile;
    uint32_t magnify = 0;
    uint32_t distant = 0;
    int inits = *sss, initt = *sst;

    tclod_tcclamp(sss, sst);

    if (other_modes.f.dolod)
    {
        nextsw = (w + dwinc) >> 16;
        nexts = (s + dsinc) >> 16;
        nextt = (t + dtinc) >> 16;
        nextys = (s + spans_d_stwz_dy[0]) >> 16;
        nextyt = (t + spans_d_stwz_dy[1]) >> 16;
        nextysw = (w + spans_d_stwz_dy[2]) >> 16;

        tcdiv(nexts, nextt, nextsw, &nexts, &nextt);
        tcdiv(nextys, nextyt, nextysw, &nextys, &nextyt);

        lodclamp = (initt & 0x60000) || (nextt & 0x60000) || (inits & 0x60000) || (nexts & 0x60000) || (nextys & 0x60000) || (nextyt & 0x60000);

        tclod_4x17_to_15(inits, nexts, initt, nextt, 0, &lod);
        tclod_4x17_to_15(inits, nextys, initt, nextyt, lod, &lod);

        lodfrac_lodtile_signals(lodclamp, lod, &l_tile, &magnify, &distant);
    
        if (other_modes.tex_lod_en)
        {
            if (distant)
                l_tile = max_level;
            if (!other_modes.detail_tex_en)
            {
                *t1 = (prim_tile + l_tile) & 7;
                if (!(distant || (!other_modes.sharpen_tex_en && magnify)))
                    *t2 = (*t1 + 1) & 7;
                else
                    *t2 = *t1;
            }
            else 
            {
                if (!magnify)
                    *t1 = (prim_tile + l_tile + 1);
                else
                    *t1 = (prim_tile + l_tile);
                *t1 &= 7;
                if (!distant && !magnify)
                    *t2 = (prim_tile + l_tile + 2) & 7;
                else
                    *t2 = (prim_tile + l_tile + 1) & 7;
            }
        }
    }
}


static void tclod_2cycle_current_notexel1(int32_t* sss, int32_t* sst, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t prim_tile, int32_t* t1)
{
    int nextys, nextyt, nextysw, nexts, nextt, nextsw;
    int lodclamp = 0;
    int32_t lod = 0;
    uint32_t l_tile;
    uint32_t magnify = 0;
    uint32_t distant = 0;
    int inits = *sss, initt = *sst;

    tclod_tcclamp(sss, sst);

    if (other_modes.f.dolod)
    {
        nextsw = (w + dwinc) >> 16;
        nexts = (s + dsinc) >> 16;
        nextt = (t + dtinc) >> 16;
        nextys = (s + spans_d_stwz_dy[0]) >> 16;
        nextyt = (t + spans_d_stwz_dy[1]) >> 16;
        nextysw = (w + spans_d_stwz_dy[2]) >> 16;

        tcdiv(nexts, nextt, nextsw, &nexts, &nextt);
        tcdiv(nextys, nextyt, nextysw, &nextys, &nextyt);

        lodclamp = (initt & 0x60000) || (nextt & 0x60000) || (inits & 0x60000) || (nexts & 0x60000) || (nextys & 0x60000) || (nextyt & 0x60000);

        tclod_4x17_to_15(inits, nexts, initt, nextt, 0, &lod);
        tclod_4x17_to_15(inits, nextys, initt, nextyt, lod, &lod);

        lodfrac_lodtile_signals(lodclamp, lod, &l_tile, &magnify, &distant);
    
        if (other_modes.tex_lod_en)
        {
            if (distant)
                l_tile = max_level;
            if (!other_modes.detail_tex_en || magnify)
                *t1 = (prim_tile + l_tile) & 7;
            else
                *t1 = (prim_tile + l_tile + 1) & 7;
        }
        
    }
}

static void tclod_2cycle_next(int32_t* sss, int32_t* sst, int32_t s, int32_t t, int32_t w,
      int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t prim_tile, int32_t* t1, int32_t* t2, int32_t* prelodfrac)
{
    int nexts, nextt, nextsw, nextys, nextyt, nextysw;
    int lodclamp = 0;
    int32_t lod = 0;
    uint32_t l_tile;
    uint32_t magnify = 0;
    uint32_t distant = 0;
    int inits = *sss, initt = *sst;

    tclod_tcclamp(sss, sst);

    if (other_modes.f.dolod)
    {
        nextsw = (w + dwinc) >> 16;
        nexts = (s + dsinc) >> 16;
        nextt = (t + dtinc) >> 16;
        nextys = (s + spans_d_stwz_dy[0]) >> 16;
        nextyt = (t + spans_d_stwz_dy[1]) >> 16;
        nextysw = (w + spans_d_stwz_dy[2]) >> 16;

        tcdiv(nexts, nextt, nextsw, &nexts, &nextt);
        tcdiv(nextys, nextyt, nextysw, &nextys, &nextyt);
    
        lodclamp = (initt & 0x60000) || (nextt & 0x60000) || (inits & 0x60000) || (nexts & 0x60000) || (nextys & 0x60000) || (nextyt & 0x60000);

        tclod_4x17_to_15(inits, nexts, initt, nextt, 0, &lod);
        tclod_4x17_to_15(inits, nextys, initt, nextyt, lod, &lod);

        
        if ((lod & 0x4000) || lodclamp)
            lod = 0x7fff;
        else if (lod < min_level)
            lod = min_level;
                        
        magnify = (lod < 32) ? 1: 0;
        l_tile =  log2table[(lod >> 5) & 0xff];
        distant = ((lod & 0x6000) || (l_tile >= max_level)) ? 1 : 0;

        *prelodfrac = ((lod << 3) >> l_tile) & 0xff;

        
        if(!other_modes.sharpen_tex_en && !other_modes.detail_tex_en)
        {
#ifdef OPTS_ENABLED
           *prelodfrac &= ~magnify;
           *prelodfrac |=  distant;
#else
           if (distant)
              *prelodfrac = 0xff;
           else if (magnify)
              *prelodfrac = 0;
#endif
        }

        
        

        if(other_modes.sharpen_tex_en && magnify)
            *prelodfrac |= 0x100;

        if (other_modes.tex_lod_en)
        {
            if (distant)
                l_tile = max_level;
            if (!other_modes.detail_tex_en)
            {
                *t1 = (prim_tile + l_tile) & 7;
                if (!(distant || (!other_modes.sharpen_tex_en && magnify)))
                    *t2 = (*t1 + 1) & 7;
                else
                    *t2 = *t1;
            }
            else 
            {
                if (!magnify)
                    *t1 = (prim_tile + l_tile + 1);
                else
                    *t1 = (prim_tile + l_tile);
                *t1 &= 7;
                if (!distant && !magnify)
                    *t2 = (prim_tile + l_tile + 2) & 7;
                else
                    *t2 = (prim_tile + l_tile + 1) & 7;
            }
        }
    }
}

static void render_spans_2cycle_complete(int start, int end, int tilenum, int flip)
{
    int zbcur;
    uint8_t offx, offy;
    SPANSIGS sigs;
    int32_t prelodfrac;
    COLOR nexttexel1_color;
    uint32_t blend_en;
    uint32_t prewrap;
    uint32_t curpixel_cvg, curpixel_cvbit, curpixel_memcvg;
    int32_t acalpha;

    int tile2 = (tilenum + 1) & 7;
    int tile1 = tilenum;
    int prim_tile = tilenum;

    int newtile1 = tile1;
    int newtile2 = tile2;
    int news, newt;

    int i, j;

    int drinc, dginc, dbinc, dainc, dzinc, dsinc, dtinc, dwinc;
    int xinc;

    int cdith = 7, adith = 0;
    int r, g, b, a, z, s, t, w;
    int sr, sg, sb, sa, sz, ss, st, sw;
    int xstart, xend, xendsc;
    int sss = 0, sst = 0;
    int curpixel = 0;
    
    int x, length, scdiff;
    uint32_t fir, fig, fib;

    int dzpix;
    int dzpixenc;

    if (flip)
    {
        drinc = spans_d_rgba[0];
        dginc = spans_d_rgba[1];
        dbinc = spans_d_rgba[2];
        dainc = spans_d_rgba[3];
        dsinc = spans_d_stwz[0];
        dtinc = spans_d_stwz[1];
        dwinc = spans_d_stwz[2];
        dzinc = spans_d_stwz[3];
        xinc = 1;
    }
    else
    {
        drinc = -spans_d_rgba[0];
        dginc = -spans_d_rgba[1];
        dbinc = -spans_d_rgba[2];
        dainc = -spans_d_rgba[3];
        dsinc = -spans_d_stwz[0];
        dtinc = -spans_d_stwz[1];
        dwinc = -spans_d_stwz[2];
        dzinc = -spans_d_stwz[3];
        xinc = -1;
    }

    if (!other_modes.z_source_sel)
        dzpix = spans_dzpix;
    else
    {
        dzpix = primitive_delta_z;
        dzinc = spans_cdz = spans_d_stwz_dy[3] = 0;
    }
    dzpixenc = dz_compress(dzpix);
                
    for (i = start; i <= end; i++)
    {
        if (span[i].validline == 0)
            continue;
        xstart = span[i].lx;
        xend = span[i].unscrx;
        xendsc = span[i].rx;
        r = span[i].rgba[0];
        g = span[i].rgba[1];
        b = span[i].rgba[2];
        a = span[i].rgba[3];
        s = span[i].stwz[0];
        t = span[i].stwz[1];
        w = span[i].stwz[2];
        z = other_modes.z_source_sel ? primitive_z : span[i].stwz[3];

        x = xendsc;
        curpixel = fb_width * i + x;
        zbcur  = zb_address + 2*curpixel;
        zbcur &= 0x00FFFFFF;
        zbcur  = zbcur >> 1;

        if (!flip)
        {
            length = xendsc - xstart;
            scdiff = xend - xendsc;
            compute_cvg_noflip(i);
        }
        else
        {
            length = xstart - xendsc;
            scdiff = xendsc - xend;
            compute_cvg_flip(i);
        }

        if (scdiff)
        {
            r += (drinc * scdiff);
            g += (dginc * scdiff);
            b += (dbinc * scdiff);
            a += (dainc * scdiff);
            z += (dzinc * scdiff);
            s += (dsinc * scdiff);
            t += (dtinc * scdiff);
            w += (dwinc * scdiff);
        }
        sigs.startspan = 1;

        for (j = 0; j <= length; j++)
        {
            sr = r >> 14;
            sg = g >> 14;
            sb = b >> 14;
            sa = a >> 14;
            ss = s >> 16;
            st = t >> 16;
            sw = w >> 16;
            sz = (z >> 10) & 0x3fffff;

            lookup_cvmask_derivatives(cvgbuf[x], &offx, &offy, &curpixel_cvg, &curpixel_cvbit);

            get_nexttexel0_2cycle(&news, &newt, s, t, w, dsinc, dtinc, dwinc);
            if (!sigs.startspan)
            {
                lod_frac = prelodfrac;
                COLOR_ASSIGN(texel0_color, nexttexel_color);
                COLOR_ASSIGN(texel1_color, nexttexel1_color);
            }
            else
            {
                tcdiv(ss, st, sw, &sss, &sst);

                tclod_2cycle_current(&sss, &sst, news, newt, s, t, w, dsinc, dtinc, dwinc, prim_tile, &tile1, &tile2);

                texture_pipeline_cycle(&texel0_color, &texel0_color, sss, sst, tile1, 0);
                texture_pipeline_cycle(&texel1_color, &texel0_color, sss, sst, tile2, 1);

                sigs.startspan = 0;
            }

            s += dsinc;
            t += dtinc;
            w += dwinc;

            tclod_2cycle_next(&news, &newt, s, t, w, dsinc, dtinc, dwinc, prim_tile, &newtile1, &newtile2, &prelodfrac);

            texture_pipeline_cycle(&nexttexel_color, &nexttexel_color, news, newt, newtile1, 0);
            texture_pipeline_cycle(&nexttexel1_color, &nexttexel_color, news, newt, newtile2, 1);

            rgbaz_correct_clip(offx, offy, sr, sg, sb, sa, &sz, curpixel_cvg);
            get_dither_noise(x, i, &cdith, &adith);
            combiner_2cycle(adith, &curpixel_cvg, &acalpha);
            fbread2_ptr(curpixel, &curpixel_memcvg);
            if (z_compare(zbcur, sz, dzpix, dzpixenc, &blend_en, &prewrap, &curpixel_cvg, curpixel_memcvg))
            {
                if (blender_2cycle(&fir, &fig, &fib, cdith, blend_en, prewrap, curpixel_cvg, curpixel_cvbit, acalpha))
                {
                    fbwrite_ptr(curpixel, fir, fig, fib, blend_en, curpixel_cvg, curpixel_memcvg);
                    if (other_modes.z_update_en)
                        z_store(zbcur, sz, dzpixenc);
                    
                }
            }
            else
            {
               COLOR_ASSIGN(memory_color, pre_memory_color);
            }

            r += drinc;
            g += dginc;
            b += dbinc;
            a += dainc;
            z += dzinc;
            
            x += xinc;
            curpixel += xinc;
            zbcur += xinc;
            zbcur &= 0x00FFFFFF >> 1;
        }
    }
}

static void render_spans_2cycle_notexelnext(int start, int end, int tilenum, int flip)
{
    int zbcur;
    uint8_t offx, offy;
    uint32_t blend_en;
    uint32_t prewrap;
    uint32_t curpixel_cvg, curpixel_cvbit, curpixel_memcvg;
    int32_t acalpha;

    int tile2 = (tilenum + 1) & 7;
    int tile1 = tilenum;
    int prim_tile = tilenum;

    int i, j;

    int drinc, dginc, dbinc, dainc, dzinc, dsinc, dtinc, dwinc;
    int xinc;

    int cdith = 7, adith = 0;
    int r, g, b, a, z, s, t, w;
    int sr, sg, sb, sa, sz, ss, st, sw;
    int xstart, xend, xendsc;
    int sss = 0, sst = 0;
    int curpixel = 0;
    
    int x, length, scdiff;
    uint32_t fir, fig, fib;

    int dzpix;
    int dzpixenc;

    if (flip)
    {
        drinc = spans_d_rgba[0];
        dginc = spans_d_rgba[1];
        dbinc = spans_d_rgba[2];
        dainc = spans_d_rgba[3];
        dsinc = spans_d_stwz[0];
        dtinc = spans_d_stwz[1];
        dwinc = spans_d_stwz[2];
        dzinc = spans_d_stwz[3];
        xinc = 1;
    }
    else
    {
        drinc = -spans_d_rgba[0];
        dginc = -spans_d_rgba[1];
        dbinc = -spans_d_rgba[2];
        dainc = -spans_d_rgba[3];
        dsinc = -spans_d_stwz[0];
        dtinc = -spans_d_stwz[1];
        dwinc = -spans_d_stwz[2];
        dzinc = -spans_d_stwz[3];
        xinc = -1;
    }

    if (!other_modes.z_source_sel)
        dzpix = spans_dzpix;
    else
    {
        dzpix = primitive_delta_z;
        dzinc = spans_cdz = spans_d_stwz_dy[3] = 0;
    }
    dzpixenc = dz_compress(dzpix);
                
    for (i = start; i <= end; i++)
    {
        if (span[i].validline == 0)
            continue;
        xstart = span[i].lx;
        xend = span[i].unscrx;
        xendsc = span[i].rx;
        r = span[i].rgba[0];
        g = span[i].rgba[1];
        b = span[i].rgba[2];
        a = span[i].rgba[3];
        s = span[i].stwz[0];
        t = span[i].stwz[1];
        w = span[i].stwz[2];
        z = other_modes.z_source_sel ? primitive_z : span[i].stwz[3];

        x = xendsc;
        curpixel = fb_width * i + x;
        zbcur  = zb_address + 2*curpixel;
        zbcur &= 0x00FFFFFF;
        zbcur  = zbcur >> 1;

        if (!flip)
        {
            length = xendsc - xstart;
            scdiff = xend - xendsc;
            compute_cvg_noflip(i);
        }
        else
        {
            length = xstart - xendsc;
            scdiff = xendsc - xend;
            compute_cvg_flip(i);
        }

        if (scdiff)
        {
            r += (drinc * scdiff);
            g += (dginc * scdiff);
            b += (dbinc * scdiff);
            a += (dainc * scdiff);
            z += (dzinc * scdiff);
            s += (dsinc * scdiff);
            t += (dtinc * scdiff);
            w += (dwinc * scdiff);
        }

        for (j = 0; j <= length; j++)
        {
            sr = r >> 14;
            sg = g >> 14;
            sb = b >> 14;
            sa = a >> 14;
            ss = s >> 16;
            st = t >> 16;
            sw = w >> 16;
            sz = (z >> 10) & 0x3fffff;

            lookup_cvmask_derivatives(cvgbuf[x], &offx, &offy, &curpixel_cvg, &curpixel_cvbit);
            
            tcdiv(ss, st, sw, &sss, &sst);

            tclod_2cycle_current_simple(&sss, &sst, s, t, w, dsinc, dtinc, dwinc, prim_tile, &tile1, &tile2);
                
            texture_pipeline_cycle(&texel0_color, &texel0_color, sss, sst, tile1, 0);
            texture_pipeline_cycle(&texel1_color, &texel0_color, sss, sst, tile2, 1);

#ifdef EXTRALOGGING
            LOG_ENABLE = curpixel == 53 * 320 + 77;
            LOG("Preclip SZ = %d\n", sz >> 3);
#endif

            rgbaz_correct_clip(offx, offy, sr, sg, sb, sa, &sz, curpixel_cvg);
                    
            get_dither_noise(x, i, &cdith, &adith);
            combiner_2cycle(adith, &curpixel_cvg, &acalpha);
                
            fbread2_ptr(curpixel, &curpixel_memcvg);

            if (z_compare(zbcur, sz, dzpix, dzpixenc, &blend_en, &prewrap, &curpixel_cvg, curpixel_memcvg))
            {
                if (blender_2cycle(&fir, &fig, &fib, cdith, blend_en, prewrap, curpixel_cvg, curpixel_cvbit, acalpha))
                {
                    fbwrite_ptr(curpixel, fir, fig, fib, blend_en, curpixel_cvg, curpixel_memcvg);
                    if (other_modes.z_update_en)
                        z_store(zbcur, sz, dzpixenc);
                }
            }
            else
            {
               COLOR_ASSIGN(memory_color, pre_memory_color);
            }

            s += dsinc;
            t += dtinc;
            w += dwinc;
            r += drinc;
            g += dginc;
            b += dbinc;
            a += dainc;
            z += dzinc;
            
            x += xinc;
            curpixel += xinc;
            zbcur += xinc;
            zbcur &= 0x00FFFFFF >> 1;

#ifdef EXTRALOGGING
            LOG_ENABLE = 0;
#endif
        }
    }
}

void breakme(void)
{}

static void render_spans_2cycle_notexel1(int start, int end, int tilenum, int flip)
{
    int zbcur;
    uint8_t offx, offy;
    uint32_t blend_en;
    uint32_t prewrap;
    uint32_t curpixel_cvg, curpixel_cvbit, curpixel_memcvg;
    int32_t acalpha;

    int tile1 = tilenum;
    int prim_tile = tilenum;

    int i, j;

    int drinc, dginc, dbinc, dainc, dzinc, dsinc, dtinc, dwinc;
    int xinc;

    int cdith = 7, adith = 0;
    int r, g, b, a, z, s, t, w;
    int sr, sg, sb, sa, sz, ss, st, sw;
    int xstart, xend, xendsc;
    int sss = 0, sst = 0;
    int curpixel = 0;
    
    int x, length, scdiff;
    uint32_t fir, fig, fib;

    int dzpix;
    int dzpixenc;

    if (flip)
    {
        drinc = spans_d_rgba[0];
        dginc = spans_d_rgba[1];
        dbinc = spans_d_rgba[2];
        dainc = spans_d_rgba[3];
        dsinc = spans_d_stwz[0];
        dtinc = spans_d_stwz[1];
        dwinc = spans_d_stwz[2];
        dzinc = spans_d_stwz[3];
        xinc = 1;
    }
    else
    {
        drinc = -spans_d_rgba[0];
        dginc = -spans_d_rgba[1];
        dbinc = -spans_d_rgba[2];
        dainc = -spans_d_rgba[3];
        dsinc = -spans_d_stwz[0];
        dtinc = -spans_d_stwz[1];
        dwinc = -spans_d_stwz[2];
        dzinc = -spans_d_stwz[3];
        xinc = -1;
    }

    if (!other_modes.z_source_sel)
        dzpix = spans_dzpix;
    else
    {
        dzpix = primitive_delta_z;
        dzinc = spans_cdz = spans_d_stwz_dy[3] = 0;
    }
    dzpixenc = dz_compress(dzpix);

    for (i = start; i <= end; i++)
    {
        if (span[i].validline == 0)
            continue;
        xstart = span[i].lx;
        xend = span[i].unscrx;
        xendsc = span[i].rx;
        r = span[i].rgba[0];
        g = span[i].rgba[1];
        b = span[i].rgba[2];
        a = span[i].rgba[3];
        s = span[i].stwz[0];
        t = span[i].stwz[1];
        w = span[i].stwz[2];
        z = other_modes.z_source_sel ? primitive_z : span[i].stwz[3];

        x = xendsc;
        curpixel = fb_width * i + x;
        zbcur  = zb_address + 2*curpixel;
        zbcur &= 0x00FFFFFF;
        zbcur  = zbcur >> 1;

        if (!flip)
        {
            length = xendsc - xstart;
            scdiff = xend - xendsc;
            compute_cvg_noflip(i);
        }
        else
        {
            length = xstart - xendsc;
            scdiff = xendsc - xend;
            compute_cvg_flip(i);
        }

        if (scdiff)
        {
            r += (drinc * scdiff);
            g += (dginc * scdiff);
            b += (dbinc * scdiff);
            a += (dainc * scdiff);
            z += (dzinc * scdiff);
            s += (dsinc * scdiff);
            t += (dtinc * scdiff);
            w += (dwinc * scdiff);
        }

        for (j = 0; j <= length; j++)
        {
            sr = r >> 14;
            sg = g >> 14;
            sb = b >> 14;
            sa = a >> 14;
            ss = s >> 16;
            st = t >> 16;
            sw = w >> 16;
            sz = (z >> 10) & 0x3fffff;

            lookup_cvmask_derivatives(cvgbuf[x], &offx, &offy, &curpixel_cvg, &curpixel_cvbit);
            
            tcdiv(ss, st, sw, &sss, &sst);

            tclod_2cycle_current_notexel1(&sss, &sst, s, t, w, dsinc, dtinc, dwinc, prim_tile, &tile1);
            
            
            texture_pipeline_cycle(&texel0_color, &texel0_color, sss, sst, tile1, 0);

#ifdef EXTRALOGGING
            LOG_ENABLE = curpixel == 53 * 320 + 77;
            if (LOG_ENABLE)
               breakme();
            LOG("Preclip SZ = %d\n", sz >> 3);
#endif
            rgbaz_correct_clip(offx, offy, sr, sg, sb, sa, &sz, curpixel_cvg);
            LOG("SZ = %d\n", sz);
                    
            get_dither_noise(x, i, &cdith, &adith);
            combiner_2cycle(adith, &curpixel_cvg, &acalpha);
                
            fbread2_ptr(curpixel, &curpixel_memcvg);

            if (z_compare(zbcur, sz, dzpix, dzpixenc, &blend_en, &prewrap, &curpixel_cvg, curpixel_memcvg))
            {
                if (blender_2cycle(&fir, &fig, &fib, cdith, blend_en, prewrap, curpixel_cvg, curpixel_cvbit, acalpha))
                {
#ifdef EXTRALOGGING
                   if (LOG_ENABLE)
                   {
                      fir = 0xff;
                      fig = 0x00;
                      fib = 0x00;
                   }
#endif
                    fbwrite_ptr(curpixel, fir, fig, fib, blend_en, curpixel_cvg, curpixel_memcvg);
                    if (other_modes.z_update_en)
                        z_store(zbcur, sz, dzpixenc);
                }
            }
            else
            {
               COLOR_ASSIGN(memory_color, pre_memory_color);
            }

            s += dsinc;
            t += dtinc;
            w += dwinc;
            r += drinc;
            g += dginc;
            b += dbinc;
            a += dainc;
            z += dzinc;
            
            x += xinc;
            curpixel += xinc;
            zbcur += xinc;
            zbcur &= 0x00FFFFFF >> 1;
#ifdef EXTRALOGGING
            LOG_ENABLE = 0;
#endif
        }
    }
}

static void render_spans_2cycle_notex(int start, int end, int tilenum, int flip)
{
    int zbcur;
    uint8_t offx, offy;
    int i, j;
    uint32_t blend_en;
    uint32_t prewrap;
    uint32_t curpixel_cvg, curpixel_cvbit, curpixel_memcvg;
    int32_t acalpha;

    int drinc, dginc, dbinc, dainc, dzinc;
    int xinc;

    int cdith = 7, adith = 0;
    int r, g, b, a, z;
    int sr, sg, sb, sa, sz;
    int xstart, xend, xendsc;
    int curpixel = 0;
    
    int x, length, scdiff;
    uint32_t fir, fig, fib;

    int dzpix;
    int dzpixenc;

    if (flip)
    {
        drinc = spans_d_rgba[0];
        dginc = spans_d_rgba[1];
        dbinc = spans_d_rgba[2];
        dainc = spans_d_rgba[3];
        dzinc = spans_d_stwz[3];
        xinc = 1;
    }
    else
    {
        drinc = -spans_d_rgba[0];
        dginc = -spans_d_rgba[1];
        dbinc = -spans_d_rgba[2];
        dainc = -spans_d_rgba[3];
        dzinc = -spans_d_stwz[3];
        xinc = -1;
    }

    if (!other_modes.z_source_sel)
        dzpix = spans_dzpix;
    else
    {
        dzpix = primitive_delta_z;
        dzinc = spans_cdz = spans_d_stwz_dy[3] = 0;
    }
    dzpixenc = dz_compress(dzpix);
                
    for (i = start; i <= end; i++)
    {
        if (span[i].validline == 0)
            continue;
        xstart = span[i].lx;
        xend = span[i].unscrx;
        xendsc = span[i].rx;
        r = span[i].rgba[0];
        g = span[i].rgba[1];
        b = span[i].rgba[2];
        a = span[i].rgba[3];
        z = other_modes.z_source_sel ? primitive_z : span[i].stwz[3];

        x = xendsc;
        curpixel = fb_width * i + x;
        zbcur  = zb_address + 2*curpixel;
        zbcur &= 0x00FFFFFF;
        zbcur  = zbcur >> 1;

        if (!flip)
        {
            length = xendsc - xstart;
            scdiff = xend - xendsc;
            compute_cvg_noflip(i);
        }
        else
        {
            length = xstart - xendsc;
            scdiff = xendsc - xend;
            compute_cvg_flip(i);
        }

        if (scdiff)
        {
            r += (drinc * scdiff);
            g += (dginc * scdiff);
            b += (dbinc * scdiff);
            a += (dainc * scdiff);
            z += (dzinc * scdiff);
        }

        for (j = 0; j <= length; j++)
        {
            sr = r >> 14;
            sg = g >> 14;
            sb = b >> 14;
            sa = a >> 14;
            sz = (z >> 10) & 0x3fffff;

            lookup_cvmask_derivatives(cvgbuf[x], &offx, &offy, &curpixel_cvg, &curpixel_cvbit);

            rgbaz_correct_clip(offx, offy, sr, sg, sb, sa, &sz, curpixel_cvg);
                    
            get_dither_noise(x, i, &cdith, &adith);
            combiner_2cycle(adith, &curpixel_cvg, &acalpha);
                
            fbread2_ptr(curpixel, &curpixel_memcvg);

            if (z_compare(zbcur, sz, dzpix, dzpixenc, &blend_en, &prewrap, &curpixel_cvg, curpixel_memcvg))
            {
                if (blender_2cycle(&fir, &fig, &fib, cdith, blend_en, prewrap, curpixel_cvg, curpixel_cvbit, acalpha))
                {
                    fbwrite_ptr(curpixel, fir, fig, fib, blend_en, curpixel_cvg, curpixel_memcvg);
                    if (other_modes.z_update_en)
                        z_store(zbcur, sz, dzpixenc);
                }
            }
            else
            {
               COLOR_ASSIGN(memory_color, pre_memory_color);
            }

            r += drinc;
            g += dginc;
            b += dbinc;
            a += dainc;
            z += dzinc;
            
            x += xinc;
            curpixel += xinc;
            zbcur += xinc;
            zbcur &= 0x00FFFFFF >> 1;
        }
    }
}


static void render_spans_fill_4(int start, int end, int flip)
{
    rdp_pipeline_crashed = 1;
}

static void render_spans_fill_8(int start, int end, int flip)
{
   int i, j;
   int x, length;
   uint32_t fb;
   int prevxstart;
   const int fastkillbits
      = other_modes.image_read_en | other_modes.z_compare_en;
   const int slowkillbits
      = other_modes.z_update_en & ~other_modes.z_source_sel & ~fastkillbits;
   const int xinc = (flip & 1) ? +1 : -1;
   int xstart = 0, xendsc;
   int curpixel = 0;

   if (fastkillbits | slowkillbits)
   { /* branch very unlikely */
      for (i = start; i <= end; i++)
	  {
         length = span[i].rx - span[i].lx; /* end - start */
         length ^= flip;
         length -= flip;
         if (length < 0)
            continue;
         if (onetimewarnings.fillmbitcrashes == 0)
            DisplayError("render_spans_fill:  RDP crashed");
         onetimewarnings.fillmbitcrashes = 1;
         rdp_pipeline_crashed = 1;
         end = i; /* premature termination of render_spans */
         if (fastkillbits) /* left out for performance */
            DisplayError("Exact fill abort timing not implemented.");
         break;
      }
   }

   for (i = start; i <= end; i++)
   {
      prevxstart = xstart;
      xstart     = span[i].lx;
      xendsc     = span[i].rx;

      x          = xendsc;
      curpixel   = fb_width * i + x;
      length      = flip ? (xstart - xendsc) : (xendsc - xstart);

      if (!span[i].validline)
         continue;

      for (j = 0, fb = fb_address + curpixel; j <= length; j++, fb += xinc)
      {
         uint32_t val = (fill_color >> (((fb & 3) ^ 3) << 3)) & 0xff;
         uint8_t hval = ((val & 1) << 1) | (val & 1);
         PAIRWRITE8(fb, val, hval);
      }
   }
}

static void render_spans_fill_16(int start, int end, int flip)
{
   int i, j;
   int x, length;
   uint32_t fb;
   int prevxstart;
   const int fastkillbits
      = other_modes.image_read_en | other_modes.z_compare_en;
   const int slowkillbits
      = other_modes.z_update_en & ~other_modes.z_source_sel & ~fastkillbits;
   const int xinc = (flip & 1) ? +1 : -1;
   int xstart = 0, xendsc;
   int curpixel = 0;

   if (fastkillbits | slowkillbits)
   { /* branch very unlikely */
      for (i = start; i <= end; i++)
      {
         length = span[i].rx - span[i].lx; /* end - start */
         length ^= flip;
         length -= flip;
         if (length < 0)
            continue;
         if (onetimewarnings.fillmbitcrashes == 0)
            DisplayError("render_spans_fill:  RDP crashed");
         onetimewarnings.fillmbitcrashes = 1;
         rdp_pipeline_crashed = 1;
         end = i; /* premature termination of render_spans */
         if (fastkillbits) /* left out for performance */
            DisplayError("Exact fill abort timing not implemented.");
         break;
	  }
   }

   for (i = start; i <= end; i++)
   {
      uint16_t val;
      uint8_t hval;
      prevxstart = xstart;
      xstart     = span[i].lx;
      xendsc     = span[i].rx;

      x          = xendsc;
      curpixel   = fb_width * i + x;
      length     = flip ? (xstart - xendsc) : (xendsc - xstart);

      if (!span[i].validline)
         continue;

      for (j = 0, fb = (fb_address >> 1) + curpixel; j <= length; j++, fb += xinc)
      {
         val   = (fb & 1 ? fill_color : fill_color >> 16) & 0xffff;
         hval  = (val & 1);
         hval += hval << 1; /* hval = (val & 1) * 3; # lea(%hval, %hval, 2), %hval */
         PAIRWRITE16(fb, val, hval);
      }
   }
}

static void render_spans_fill_32(int start, int end, int flip)
{
   int i, j;
   int x, length;
   uint32_t fb;
   int prevxstart;
   const int fastkillbits
      = other_modes.image_read_en | other_modes.z_compare_en;
   const int slowkillbits
      = other_modes.z_update_en & ~other_modes.z_source_sel & ~fastkillbits;
   const int xinc = (flip & 1) ? +1 : -1;
   int xstart = 0, xendsc;
   int curpixel = 0;

   if (fastkillbits | slowkillbits)
   { /* branch very unlikely */
      for (i = start; i <= end; i++)
      {
         length = span[i].rx - span[i].lx; /* end - start */
         length ^= flip;
         length -= flip;
         if (length < 0)
            continue;
         if (onetimewarnings.fillmbitcrashes == 0)
            DisplayError("render_spans_fill:  RDP crashed");
         onetimewarnings.fillmbitcrashes = 1;
         rdp_pipeline_crashed = 1;
         end = i; /* premature termination of render_spans */
         if (fastkillbits) /* left out for performance */
            DisplayError("Exact fill abort timing not implemented.");
         break;
      }
   }

   for (i = start; i <= end; i++)
   {
      prevxstart = xstart;
      xstart     = span[i].lx;
      xendsc     = span[i].rx;

      x          = xendsc;
      curpixel   = fb_width * i + x;
      length     = flip ? (xstart - xendsc) : (xendsc - xstart);

      if (!span[i].validline)
         continue;

      for (j = 0, fb = (fb_address >> 2) + curpixel; j <= length; j++, fb += xinc)
      {
         PAIRWRITE32(fb, fill_color, (fill_color & 0x10000) ? 3 : 0, (fill_color & 0x1) ? 3 : 0);
      }
   }
}

static void render_spans_fill(int start, int end, int flip)
{
   switch (fb_size)
   {
      case PIXEL_SIZE_4BIT:
         render_spans_fill_4(start, end, flip);
         break;
      case PIXEL_SIZE_8BIT:
         render_spans_fill_8(start, end, flip);
         break;
      case PIXEL_SIZE_16BIT:
         render_spans_fill_16(start, end, flip);
         break;
      case PIXEL_SIZE_32BIT:
         render_spans_fill_32(start, end, flip);
         break;
   }
}

static void tclod_copy(int32_t* sss, int32_t* sst, int32_t s, int32_t t, int32_t w, int32_t dsinc, int32_t dtinc, int32_t dwinc, int32_t prim_tile, int32_t* t1)
{




    int nexts, nextt, nextsw, fars, fart, farsw;
    int lodclamp = 0;
    int32_t lod = 0;
    uint32_t l_tile = 0, magnify = 0, distant = 0;

    tclod_tcclamp(sss, sst);

    if (other_modes.tex_lod_en)
    {
        
        
        
        nextsw = (w + dwinc) >> 16;
        nexts = (s + dsinc) >> 16;
        nextt = (t + dtinc) >> 16;
        farsw = (w + (dwinc << 1)) >> 16;
        fars = (s + (dsinc << 1)) >> 16;
        fart = (t + (dtinc << 1)) >> 16;
    
        tcdiv(nexts, nextt, nextsw, &nexts, &nextt);
        tcdiv(fars, fart, farsw, &fars, &fart);

        lodclamp = (fart & 0x60000) || (nextt & 0x60000) || (fars & 0x60000) || (nexts & 0x60000);

        tclod_4x17_to_15(nexts, fars, nextt, fart, 0, &lod);

        if ((lod & 0x4000) || lodclamp)
            lod = 0x7fff;
        else if (lod < min_level)
            lod = min_level;
                        
        magnify = (lod < 32) ? 1: 0;
        l_tile =  log2table[(lod >> 5) & 0xff];
        distant = ((lod & 0x6000) || (l_tile >= max_level)) ? 1 : 0;

        if (distant)
            l_tile = max_level;
    
        if (!other_modes.detail_tex_en || magnify)
            *t1 = (prim_tile + l_tile) & 7;
        else
            *t1 = (prim_tile + l_tile + 1) & 7;
    }

}

static void render_spans_copy(int start, int end, int tilenum, int flip)
{
    int i, j, k;
    
    int tile1 = tilenum;
    int prim_tile = tilenum;

    int dsinc, dtinc, dwinc;
    int xinc;

    int xstart = 0, xendsc;
    int s = 0, t = 0, w = 0, ss = 0, st = 0, sw = 0, sss = 0, sst = 0, ssw = 0;
    int fb_index, length;
    int diff = 0;

    uint32_t hidword = 0, lowdword = 0;
    uint32_t hidword1 = 0, lowdword1 = 0;
    int fbadvance = (fb_size == PIXEL_SIZE_4BIT) ? 8 : 16 >> fb_size;
    uint32_t fbptr = 0;
    int fbptr_advance = flip ? 8 : -8;
    uint64_t copyqword = 0;
    uint32_t tempdword = 0, tempbyte = 0;
    int copywmask = 0, alphamask = 0;
    int bytesperpixel = (fb_size == PIXEL_SIZE_4BIT) ? 1 : (1 << (fb_size - 1));
    uint32_t fbendptr = 0;
    int32_t threshold, currthreshold;

    if (fb_size == PIXEL_SIZE_32BIT)
    {
        rdp_pipeline_crashed = 1;
        return;
    }

    if (flip)
    {
        dsinc = spans_d_stwz[0];
        dtinc = spans_d_stwz[1];
        dwinc = spans_d_stwz[2];
        xinc = 1;
    }
    else
    {
        dsinc = -spans_d_stwz[0];
        dtinc = -spans_d_stwz[1];
        dwinc = -spans_d_stwz[2];
        xinc = -1;
    }

#define PIXELS_TO_BYTES_SPECIAL4(pix, siz) ((siz) ? PIXELS_TO_BYTES(pix, siz) : (pix))
                
    for (i = start; i <= end; i++)
    {
        if (span[i].validline == 0)
            continue;
        s = span[i].stwz[0];
        t = span[i].stwz[1];
        w = span[i].stwz[2];
        
        xstart = span[i].lx;
        xendsc = span[i].rx;

        fb_index = fb_width * i + xendsc;
        fbptr = fb_address + PIXELS_TO_BYTES_SPECIAL4(fb_index, fb_size);
        fbendptr = fb_address + PIXELS_TO_BYTES_SPECIAL4((fb_width * i + xstart), fb_size);
        fbptr &= 0x00FFFFFF;
        fbendptr &= 0x00FFFFFF;
        length = flip ? (xstart - xendsc) : (xendsc - xstart);

        for (j = 0; j <= length; j += fbadvance)
        {
            ss = s >> 16;
            st = t >> 16;
            sw = w >> 16;

            tcdiv(ss, st, sw, &sss, &sst);
            tclod_copy(&sss, &sst, s, t, w, dsinc, dtinc, dwinc, prim_tile, &tile1);
            fetch_qword_copy(&hidword, &lowdword, sss, sst, tile1);

            if (fb_size == PIXEL_SIZE_16BIT || fb_size == PIXEL_SIZE_8BIT)
                copyqword = ((uint64_t)hidword << 32) | ((uint64_t)lowdword);
            else
                copyqword = 0;
            if (!other_modes.alpha_compare_en)
                alphamask = 0xff;
            else if (fb_size == PIXEL_SIZE_16BIT)
            {
                alphamask = 0;
                alphamask |= (((copyqword >> 48) & 1) ? 0xC0 : 0);
                alphamask |= (((copyqword >> 32) & 1) ? 0x30 : 0);
                alphamask |= (((copyqword >> 16) & 1) ? 0xC : 0);
                alphamask |= ((copyqword & 1) ? 0x3 : 0);
            }
            else if (fb_size == PIXEL_SIZE_8BIT)
            {
                alphamask = 0;
                threshold = (other_modes.dither_alpha_en) ? (irand() & 0xff) : COLOR_ALPHA(blend_color);
                if (other_modes.dither_alpha_en)
                {
                    currthreshold = threshold;
                    alphamask |= (((copyqword >> 24) & 0xff) >= currthreshold ? 0xC0 : 0);
                    currthreshold = ((threshold & 3) << 6) | (threshold >> 2);
                    alphamask |= (((copyqword >> 16) & 0xff) >= currthreshold ? 0x30 : 0);
                    currthreshold = ((threshold & 0xf) << 4) | (threshold >> 4);
                    alphamask |= (((copyqword >> 8) & 0xff) >= currthreshold ? 0xC : 0);
                    currthreshold = ((threshold & 0x3f) << 2) | (threshold >> 6);
                    alphamask |= ((copyqword & 0xff) >= currthreshold ? 0x3 : 0);    
                }
                else
                {
                    alphamask |= (((copyqword >> 24) & 0xff) >= threshold ? 0xC0 : 0);
                    alphamask |= (((copyqword >> 16) & 0xff) >= threshold ? 0x30 : 0);
                    alphamask |= (((copyqword >> 8) & 0xff) >= threshold ? 0xC : 0);
                    alphamask |= ((copyqword & 0xff) >= threshold ? 0x3 : 0);
                }
            }
            else
                alphamask = 0;

            copywmask  = fbptr - fbendptr;
            copywmask ^= -flip;
            copywmask -= -flip;
            copywmask += bytesperpixel;
            if (copywmask > 8) 
                copywmask = 8;
            tempdword = fbptr;
            k = 7;
            while (copywmask > 0)
            {
                tempbyte = (uint32_t)((copyqword >> (k << 3)) & 0xff);
                if (alphamask & (1 << k))
                {
                    PAIRWRITE8(tempdword, tempbyte, (tempbyte & 1) ? 3 : 0);
                }
                k--;
                tempdword += xinc;
                copywmask--;
            }
            s += dsinc;
            t += dtinc;
            w += dwinc;
            fbptr += fbptr_advance;
            fbptr &= 0x00FFFFFF;
        }
    }
}

static void deduce_derivatives(void)
{
    int texel1_used_in_cc1 = 0, texel0_used_in_cc1 = 0, texel0_used_in_cc0 = 0, texel1_used_in_cc0 = 0;
    int texels_in_cc0 = 0, texels_in_cc1 = 0;
    int lod_frac_used_in_cc1 = 0, lod_frac_used_in_cc0 = 0;
    int lodfracused = 0;

    other_modes.f.partialreject_1cycle = (blender2b_a[0] == &COLOR_ALPHA(inv_pixel_color) && blender1b_a[0] == &COLOR_ALPHA(pixel_color));
    other_modes.f.partialreject_2cycle = (blender2b_a[1] == &COLOR_ALPHA(inv_pixel_color) && blender1b_a[1] == &COLOR_ALPHA(pixel_color));

    other_modes.f.special_bsel0 = (blender2b_a[0] == &COLOR_ALPHA(memory_color));
    other_modes.f.special_bsel1 = (blender2b_a[1] == &COLOR_ALPHA(memory_color));

    other_modes.f.realblendershiftersneeded = (other_modes.f.special_bsel0 && other_modes.cycle_type == CYCLE_TYPE_1) || (other_modes.f.special_bsel1 && other_modes.cycle_type == CYCLE_TYPE_2);
    other_modes.f.interpixelblendershiftersneeded = (other_modes.f.special_bsel0 && other_modes.cycle_type == CYCLE_TYPE_2);

    other_modes.f.rgb_alpha_dither = (other_modes.rgb_dither_sel << 2) | other_modes.alpha_dither_sel;

    if ((combiner_rgbmul_r[1] == &lod_frac) || (combiner_alphamul[1] == &lod_frac))
        lod_frac_used_in_cc1 = 1;
    if ((combiner_rgbmul_r[0] == &lod_frac) || (combiner_alphamul[0] == &lod_frac))
        lod_frac_used_in_cc0 = 1;

    if (combiner_rgbmul_r[1] == &COLOR_RED(texel1_color) || combiner_rgbsub_a_r[1] == &COLOR_RED(texel1_color) || combiner_rgbsub_b_r[1] == &COLOR_RED(texel1_color) || combiner_rgbadd_r[1] == &COLOR_RED(texel1_color) ||
        combiner_alphamul[1] == &COLOR_ALPHA(texel1_color) || combiner_alphasub_a[1] == &COLOR_ALPHA(texel1_color) || combiner_alphasub_b[1] == &COLOR_ALPHA(texel1_color) || combiner_alphaadd[1] == &COLOR_ALPHA(texel1_color) || 
        combiner_rgbmul_r[1] == &COLOR_ALPHA(texel1_color))
        texel1_used_in_cc1 = 1;
    if (combiner_rgbmul_r[1] == &COLOR_RED(texel0_color) || combiner_rgbsub_a_r[1] == &COLOR_RED(texel0_color) || combiner_rgbsub_b_r[1] == &COLOR_RED(texel0_color) || combiner_rgbadd_r[1] == &COLOR_RED(texel0_color) || 
        combiner_alphamul[1] == &COLOR_ALPHA(texel0_color) || combiner_alphasub_a[1] == &COLOR_ALPHA(texel0_color) || combiner_alphasub_b[1] == &COLOR_ALPHA(texel0_color) || combiner_alphaadd[1] == &COLOR_ALPHA(texel0_color) || 
        combiner_rgbmul_r[1] == &COLOR_ALPHA(texel0_color))
        texel0_used_in_cc1 = 1;
    if (combiner_rgbmul_r[0] == &COLOR_RED(texel1_color) || combiner_rgbsub_a_r[0] == &COLOR_RED(texel1_color) || combiner_rgbsub_b_r[0] == &COLOR_RED(texel1_color) || combiner_rgbadd_r[0] == &COLOR_RED(texel1_color) || 
        combiner_alphamul[0] == &COLOR_ALPHA(texel1_color) || combiner_alphasub_a[0] == &COLOR_ALPHA(texel1_color) || combiner_alphasub_b[0] == &COLOR_ALPHA(texel1_color) || combiner_alphaadd[0] == &COLOR_ALPHA(texel1_color) ||
        combiner_rgbmul_r[0] == &COLOR_ALPHA(texel1_color))
        texel1_used_in_cc0 = 1;
    if (combiner_rgbmul_r[0] == &COLOR_RED(texel0_color) || combiner_rgbsub_a_r[0] == &COLOR_RED(texel0_color) || combiner_rgbsub_b_r[0] == &COLOR_RED(texel0_color) || combiner_rgbadd_r[0] == &COLOR_RED(texel0_color) || 
        combiner_alphamul[0] == &COLOR_ALPHA(texel0_color) || combiner_alphasub_a[0] == &COLOR_ALPHA(texel0_color) || combiner_alphasub_b[0] == &COLOR_ALPHA(texel0_color) || combiner_alphaadd[0] == &COLOR_ALPHA(texel0_color) || 
        combiner_rgbmul_r[0] == &COLOR_ALPHA(texel0_color))
        texel0_used_in_cc0 = 1;
    texels_in_cc0 = texel0_used_in_cc0 || texel1_used_in_cc0;
    texels_in_cc1 = texel0_used_in_cc1 || texel1_used_in_cc1;    

    
    if (texel1_used_in_cc1)
        render_spans_1cycle_ptr = render_spans_1cycle_func[2];
    else if (texel0_used_in_cc1 || lod_frac_used_in_cc1)
        render_spans_1cycle_ptr = render_spans_1cycle_func[1];
    else
        render_spans_1cycle_ptr = render_spans_1cycle_func[0];

    if (texel1_used_in_cc1)
        render_spans_2cycle_ptr = render_spans_2cycle_func[3];
    else if (texel1_used_in_cc0 || texel0_used_in_cc1)
        render_spans_2cycle_ptr = render_spans_2cycle_func[2];
    else if (texel0_used_in_cc0 || lod_frac_used_in_cc0 || lod_frac_used_in_cc1)
        render_spans_2cycle_ptr = render_spans_2cycle_func[1];
    else
        render_spans_2cycle_ptr = render_spans_2cycle_func[0];

    if ((other_modes.cycle_type == CYCLE_TYPE_2 && (lod_frac_used_in_cc0 || lod_frac_used_in_cc1)) || \
        (other_modes.cycle_type == CYCLE_TYPE_1 && lod_frac_used_in_cc1))
        lodfracused = 1;

    if ((other_modes.cycle_type == CYCLE_TYPE_1 && combiner_rgbsub_a_r[1] == &noise) || \
        (other_modes.cycle_type == CYCLE_TYPE_2 && (combiner_rgbsub_a_r[0] == &noise || combiner_rgbsub_a_r[1] == &noise)) || \
        other_modes.alpha_dither_sel == 2)
        get_dither_noise_type = 0;
    else if (other_modes.f.rgb_alpha_dither != 0xf)
        get_dither_noise_type= 1;
    else
        get_dither_noise_type = 2;

    other_modes.f.dolod = other_modes.tex_lod_en || lodfracused;
}

static void render_spans(
    int yhlimit, int yllimit, int tilenum, int flip)
{
    if (other_modes.f.stalederivs)
    {
        deduce_derivatives();
        other_modes.f.stalederivs = 0;
    }

    fbread1_ptr = fbread_func[fb_size];
    fbread2_ptr = fbread2_func[fb_size];
    fbwrite_ptr = fbwrite_func[fb_size];

#ifdef _DEBUG
    ++render_cycle_mode_counts[other_modes.cycle_type];
#endif

    switch (other_modes.cycle_type)
    {
       case CYCLE_TYPE_1:
          render_spans_1cycle_ptr(yhlimit, yllimit, tilenum, flip);
          break;
       case CYCLE_TYPE_2:
          render_spans_2cycle_ptr(yhlimit, yllimit, tilenum, flip);
          break;
       case CYCLE_TYPE_COPY:
          render_spans_copy(yhlimit, yllimit, tilenum, flip);
          break;
       case CYCLE_TYPE_FILL:
          render_spans_fill(yhlimit, yllimit, flip);
          break;
    }
}

static NOINLINE void loading_pipeline(
    int start, int end, int tilenum, int coord_quad, int ltlut)
{
    int localdebugmode = 0, cnt = 0;
    int i, j;

    int dsinc, dtinc;

    int s, t;
    int ss, st;
    int xstart, xend, xendsc;
    int sss = 0, sst = 0;
    int ti_index, length;

    uint32_t tmemidx0 = 0, tmemidx1 = 0, tmemidx2 = 0, tmemidx3 = 0;
    int dswap = 0;
    uint16_t* tmem16 = (uint16_t*)__TMEM;
    uint32_t readval0, readval1, readval2, readval3;
    uint32_t readidx32;
    uint64_t loadqword;
    uint16_t tempshort;
    int tmem_formatting = 0;
    uint32_t bit3fl = 0, hibit = 0;

    int tiadvance = 0, spanadvance = 0;
    unsigned long tiptr;

    dsinc = spans_d_stwz[0];
    dtinc = spans_d_stwz[1];

    if (end > start && ltlut)
    {
        rdp_pipeline_crashed = 1;
        return;
    }

    if (tile[tilenum].format == FORMAT_YUV)
        tmem_formatting = 0;
    else if (tile[tilenum].format == FORMAT_RGBA && tile[tilenum].size == PIXEL_SIZE_32BIT)
        tmem_formatting = 1;
    else
        tmem_formatting = 2;

    switch (ti_size)
    {
       case PIXEL_SIZE_4BIT:
          rdp_pipeline_crashed = 1;
          return;
       case PIXEL_SIZE_8BIT:
          tiadvance = 8;
          spanadvance = 8;
          break;
       case PIXEL_SIZE_16BIT:
          if (!ltlut)
          {
             tiadvance = 8;
             spanadvance = 4;
          }
          else
          {
             tiadvance = 2;
             spanadvance = 1;
          }
          break;
       case PIXEL_SIZE_32BIT:
          tiadvance = 8;
          spanadvance = 2;
          break;
    }

    for (i = start; i <= end; i++)
    {
        xstart = span[i].lx;
        xend = span[i].unscrx;
        xendsc = span[i].rx;
        s = span[i].stwz[0];
        t = span[i].stwz[1];

        ti_index = ti_width * i + xend;
        tiptr = ti_address + PIXELS_TO_BYTES(ti_index, ti_size);
        tiptr = tiptr & 0x00FFFFFF;

        length = (xstart - xend + 1) & 0xfff;

        for (j = 0; j < length; j+= spanadvance)
        {
            ss = s >> 16;
            st = t >> 16;

            sss = ss & 0xffff;
            sst = st & 0xffff;

            tc_pipeline_load(&sss, &sst, tilenum, coord_quad);

            dswap = sst & 1;

            get_tmem_idx(sss, sst, tilenum, &tmemidx0, &tmemidx1, &tmemidx2, &tmemidx3, &bit3fl, &hibit);

            readidx32 = (tiptr >> 2) & ~1;
            RREADIDX32(readval0, readidx32);
            readidx32++;
            RREADIDX32(readval1, readidx32);
            readidx32++;
            RREADIDX32(readval2, readidx32);
            readidx32++;
            RREADIDX32(readval3, readidx32);

            switch (tiptr & 7)
            {
            case 0:
                if (!ltlut)
                    loadqword = ((uint64_t)readval0 << 32) | readval1;
                else
                {
                    tempshort = readval0 >> 16;
                    loadqword = ((uint64_t)tempshort << 48) | ((uint64_t) tempshort << 32) | ((uint64_t) tempshort << 16) | tempshort;
                }
                break;
            case 1:
                loadqword = ((uint64_t)readval0 << 40) | ((uint64_t)readval1 << 8) | (readval2 >> 24);
                break;
            case 2:
                if (!ltlut)
                    loadqword = ((uint64_t)readval0 << 48) | ((uint64_t)readval1 << 16) | (readval2 >> 16);
                else
                {
                    tempshort = readval0 & 0xffff;
                    loadqword = ((uint64_t)tempshort << 48) | ((uint64_t) tempshort << 32) | ((uint64_t) tempshort << 16) | tempshort;
                }
                break;
            case 3:
                loadqword = ((uint64_t)readval0 << 56) | ((uint64_t)readval1 << 24) | (readval2 >> 8);
                break;
            case 4:
                if (!ltlut)
                    loadqword = ((uint64_t)readval1 << 32) | readval2;
                else
                {
                    tempshort = readval1 >> 16;
                    loadqword = ((uint64_t)tempshort << 48) | ((uint64_t) tempshort << 32) | ((uint64_t) tempshort << 16) | tempshort;
                }
                break;
            case 5:
                loadqword = ((uint64_t)readval1 << 40) | ((uint64_t)readval2 << 8) | (readval3 >> 24);
                break;
            case 6:
                if (!ltlut)
                    loadqword = ((uint64_t)readval1 << 48) | ((uint64_t)readval2 << 16) | (readval3 >> 16);
                else
                {
                    tempshort = readval1 & 0xffff;
                    loadqword = ((uint64_t)tempshort << 48) | ((uint64_t) tempshort << 32) | ((uint64_t) tempshort << 16) | tempshort;
                }
                break;
            case 7:
                loadqword = ((uint64_t)readval1 << 56) | ((uint64_t)readval2 << 24) | (readval3 >> 8);
                break;
            }

            
            switch(tmem_formatting)
            {
            case 0:
                readval0 = (uint32_t)((((loadqword >> 56) & 0xff) << 24) | (((loadqword >> 40) & 0xff) << 16) | (((loadqword >> 24) & 0xff) << 8) | (((loadqword >> 8) & 0xff) << 0));
                readval1 = (uint32_t)((((loadqword >> 48) & 0xff) << 24) | (((loadqword >> 32) & 0xff) << 16) | (((loadqword >> 16) & 0xff) << 8) | (((loadqword >> 0) & 0xff) << 0));
                if (bit3fl)
                {
                    tmem16[tmemidx2 ^ WORD_ADDR_XOR] = (uint16_t)(readval0 >> 16);
                    tmem16[tmemidx3 ^ WORD_ADDR_XOR] = (uint16_t)(readval0 & 0xffff);
                    tmem16[(tmemidx2 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(readval1 >> 16);
                    tmem16[(tmemidx3 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(readval1 & 0xffff);
                }
                else
                {
                    tmem16[tmemidx0 ^ WORD_ADDR_XOR] = (uint16_t)(readval0 >> 16);
                    tmem16[tmemidx1 ^ WORD_ADDR_XOR] = (uint16_t)(readval0 & 0xffff);
                    tmem16[(tmemidx0 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(readval1 >> 16);
                    tmem16[(tmemidx1 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(readval1 & 0xffff);
                }
                break;
            case 1:
                readval0 = (uint32_t)(((loadqword >> 48) << 16) | ((loadqword >> 16) & 0xffff));
                readval1 = (uint32_t)((((loadqword >> 32) & 0xffff) << 16) | (loadqword & 0xffff));

                if (bit3fl)
                {
                    tmem16[tmemidx2 ^ WORD_ADDR_XOR] = (uint16_t)(readval0 >> 16);
                    tmem16[tmemidx3 ^ WORD_ADDR_XOR] = (uint16_t)(readval0 & 0xffff);
                    tmem16[(tmemidx2 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(readval1 >> 16);
                    tmem16[(tmemidx3 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(readval1 & 0xffff);
                }
                else
                {
                    tmem16[tmemidx0 ^ WORD_ADDR_XOR] = (uint16_t)(readval0 >> 16);
                    tmem16[tmemidx1 ^ WORD_ADDR_XOR] = (uint16_t)(readval0 & 0xffff);
                    tmem16[(tmemidx0 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(readval1 >> 16);
                    tmem16[(tmemidx1 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(readval1 & 0xffff);
                }
                break;
            case 2:
                if (!dswap)
                {
                    if (!hibit)
                    {
                        tmem16[tmemidx0 ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 48);
                        tmem16[tmemidx1 ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 32);
                        tmem16[tmemidx2 ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 16);
                        tmem16[tmemidx3 ^ WORD_ADDR_XOR] = (uint16_t)(loadqword & 0xffff);
                    }
                    else
                    {
                        tmem16[(tmemidx0 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 48);
                        tmem16[(tmemidx1 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 32);
                        tmem16[(tmemidx2 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 16);
                        tmem16[(tmemidx3 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(loadqword & 0xffff);
                    }
                }
                else
                {
                    if (!hibit)
                    {
                        tmem16[tmemidx0 ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 16);
                        tmem16[tmemidx1 ^ WORD_ADDR_XOR] = (uint16_t)(loadqword & 0xffff);
                        tmem16[tmemidx2 ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 48);
                        tmem16[tmemidx3 ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 32);
                    }
                    else
                    {
                        tmem16[(tmemidx0 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 16);
                        tmem16[(tmemidx1 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(loadqword & 0xffff);
                        tmem16[(tmemidx2 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 48);
                        tmem16[(tmemidx3 | 0x400) ^ WORD_ADDR_XOR] = (uint16_t)(loadqword >> 32);
                    }
                }
            break;
            }


            s = (s + dsinc) & ~0x1f;
            t = (t + dtinc) & ~0x1f;
            tiptr += tiadvance;
            tiptr &= 0x00FFFFFF;
        }
    }
}

static void edgewalker_for_loads(int32_t* lewdata)
{
    int j = 0;
    int xleft = 0, xright = 0;
    int xstart = 0, xend = 0;
    int s = 0, t = 0, w = 0;
    int dsdx = 0, dtdx = 0;
    int dsdy = 0, dtdy = 0;
    int dsde = 0, dtde = 0;
    int tilenum = 0, flip = 0;

    int spix;
    int ycur;
    int ylfar;

    int32_t maxxmx, minxhx;
    int32_t yl = 0, ym = 0, yh = 0;
    int32_t xl = 0, xm = 0, xh = 0;
    int32_t dxldy = 0, dxhdy = 0, dxmdy = 0;

    int commandcode = (lewdata[0] >> 24) & 0x3f;
    int ltlut = (commandcode == 0x30);
    int coord_quad = ltlut || (commandcode == 0x33);
        
    int k = 0;
    int sign_dxhdy = 0;
    int do_offset = 0;
    int xfrac = 0;

    int valid_y = 1;
    int length = 0;
    int32_t xrsc = 0, xlsc = 0, stickybit = 0;
    int32_t yllimit;
    int32_t yhlimit;

    flip = 1;
    max_level = 0;
    tilenum = (lewdata[0] >> 16) & 7;

    
    yl = SIGN(lewdata[0], 14); 
    ym = lewdata[1] >> 16;
    ym = SIGN(ym, 14);
    yh = SIGN(lewdata[1], 14); 
    
    xl = SIGN(lewdata[2], 30);
    xh = SIGN(lewdata[3], 30);
    xm = SIGN(lewdata[4], 30);
    
    dxldy = 0;
    dxhdy = 0;
    dxmdy = 0;

    s    = lewdata[5] & 0xffff0000;
    t    = (lewdata[5] & 0xffff) << 16;
    w    = 0;
    dsdx = (lewdata[7] & 0xffff0000) | ((lewdata[6] >> 16) & 0xffff);
    dtdx = ((lewdata[7] << 16) & 0xffff0000)    | (lewdata[6] & 0xffff);
    dsde = 0;
    dtde = (lewdata[9] & 0xffff) << 16;
    dsdy = 0;
    dtdy = (lewdata[8] & 0xffff) << 16;

    spans_d_stwz[0] = dsdx & ~0x1f;
    spans_d_stwz[1] = dtdx & ~0x1f;
    spans_d_stwz[2] = 0;

    xright = xh & ~0x1;
    xleft = xm & ~0x1;

#define ADJUST_ATTR_LOAD() {           \
    span[j].stwz[0] = s & ~0x000003FF; \
    span[j].stwz[1] = t & ~0x000003FF; \
}

#define ADDVALUES_LOAD() { \
    t += dtde; \
}

    spix = 0;
    ycur = yh & ~3;
    ylfar = yl | 3;
    yllimit = yl;
    yhlimit = yh;

    xfrac = 0;
    xend = xright >> 16;

    for (k = ycur; k <= ylfar; k++)
    {
        if (k == ym)
            xleft = xl & ~1;
        spix = k & 3;
        if (!(k & ~0xfff))
        {
            j = k >> 2;
            valid_y = !(k < yhlimit || k >= yllimit);

            if (spix == 0)
            {
                maxxmx = 0;
                minxhx = 0xfff;
            }

            xrsc = (xright >> 13) & 0x7ffe;

            xlsc = (xleft >> 13) & 0x7ffe;

            if (valid_y)
            {
                maxxmx = (((xlsc >> 3) & 0xfff) > maxxmx) ? (xlsc >> 3) & 0xfff : maxxmx;
                minxhx = (((xrsc >> 3) & 0xfff) < minxhx) ? (xrsc >> 3) & 0xfff : minxhx;
            }

            if (spix == 0)
            {
                span[j].unscrx = xend;
                ADJUST_ATTR_LOAD();
            }

            if (spix == 3)
            {
                span[j].lx = maxxmx;
                span[j].rx = minxhx;
            }
        }

        if (spix == 3)
        {
            ADDVALUES_LOAD();
        }
    }

    loading_pipeline(yhlimit >> 2, yllimit >> 2, tilenum, coord_quad, ltlut);
}


static const char *const image_format[] = { "RGBA", "YUV", "CI", "IA", "I", "???", "???", "???" };
static const char *const image_size[] = { "4-bit", "8-bit", "16-bit", "32-bit" };


static void tile_tlut_common_cs_decoder(uint32_t w1, uint32_t w2)
{
    int32_t lewdata[10];
    int tilenum = (w2 >> 24) & 0x7;
    int sl, tl, sh, th;

    tile[tilenum].sl = sl = ((w1 >> 12) & 0xfff);
    tile[tilenum].tl = tl = ((w1 >>  0) & 0xfff);
    tile[tilenum].sh = sh = ((w2 >> 12) & 0xfff);
    tile[tilenum].th = th = ((w2 >>  0) & 0xfff);

    calculate_clamp_diffs(tilenum);

    lewdata[0] = (w1 & 0xff000000) | (0x10 << 19) | (tilenum << 16) | (th | 3);
    lewdata[1] = ((th | 3) << 16) | (tl);
    lewdata[2] = ((sh >> 2) << 16) | ((sh & 3) << 14);
    lewdata[3] = ((sl >> 2) << 16) | ((sl & 3) << 14);
    lewdata[4] = ((sh >> 2) << 16) | ((sh & 3) << 14);
    lewdata[5] = ((sl << 3) << 16) | (tl << 3);
    lewdata[6] = 0;
    lewdata[7] = (0x200 >> ti_size) << 16;
    lewdata[8] = 0x20;
    lewdata[9] = 0x20;

    edgewalker_for_loads(lewdata);
}

STRICTINLINE int32_t irand(void)
{
    iseed *= 0x343fd;
    iseed += 0x269ec3;
    return ((iseed >> 16) & 0x7fff);
}





void rdp_close(void)
{
}

static STRICTINLINE int finalize_spanalpha(
    uint32_t blend_en, uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
    int possibilities[4];

    possibilities[CVG_WRAP] = curpixel_memcvg;
    possibilities[CVG_SAVE] = curpixel_memcvg;
    possibilities[CVG_ZAP] = 7;
    possibilities[CVG_CLAMP]  = curpixel_memcvg;
    possibilities[CVG_CLAMP] |= -(signed)(blend_en) ^ ~0;
    possibilities[CVG_CLAMP] += curpixel_cvg;
    possibilities[CVG_WRAP] += curpixel_cvg;
    possibilities[CVG_CLAMP] |= -(possibilities[CVG_CLAMP]>>3 & 1);

    return (possibilities[other_modes.cvg_dest] & 7);
}

static void fbwrite_4(
    uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en,
    uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
    unsigned long addr  = fb_address + curpixel*1;
    addr &= 0x00FFFFFF;

    RWRITEADDR8(addr, 0x00);
}

static void fbwrite_8(
    uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en,
    uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
    unsigned long addr  = fb_address + 1*curpixel;
    addr &= 0x00FFFFFF;
    PAIRWRITE8(addr, r, (r & 1) ? 3 : 0);
}

static void fbwrite_16(
    uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en,
    uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
    uint16_t color;
    unsigned long addr;
    int coverage = finalize_spanalpha(blend_en, curpixel_cvg, curpixel_memcvg);
#undef CVG_DRAW
#ifdef CVG_DRAW
    const int covdraw = (curpixel_cvg - 1) << 5;

    r = covdraw;
    g = covdraw;
    b = covdraw;
#endif
    if (fb_format != FORMAT_RGBA)
    {
        color = (r << 8) | (coverage << 5);
        coverage = 0x00;
    }
    else
    {
        r &= 0xFF & ~7;
        g &= 0xFF & ~7;
        b &= 0xFF & ~7;
        color = (r << 8) | (g << 3) | (b >> 2) | (coverage >> 2);
    }

    addr  = fb_address + 2*curpixel;
    addr &= 0x00FFFFFF;
    addr  = addr >> 1;
    PAIRWRITE16(addr, color, coverage & 3);
}

static void fbwrite_32(
    uint32_t curpixel, uint32_t r, uint32_t g, uint32_t b, uint32_t blend_en,
    uint32_t curpixel_cvg, uint32_t curpixel_memcvg)
{
    uint32_t color;
    int coverage;
    unsigned long addr  = fb_address + 4*curpixel;
    addr &= 0x00FFFFFF;
    addr  = addr >> 2;

    coverage = finalize_spanalpha(blend_en, curpixel_cvg, curpixel_memcvg);
    color  = (r << 24) | (g << 16) | (b <<  8);
    color |= (coverage << 5);

    g = -(signed)(g & 1) & 3;
    PAIRWRITE32(addr, color, g, 0);
}

static void fbread_4(uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    COLOR_RED(memory_color)   = 0x00;
    COLOR_GREEN(memory_color) = 0x00;
    COLOR_BLUE(memory_color)  = 0x00;
    COLOR_ALPHA(memory_color) = 0xE0;
    *curpixel_memcvg          = 7;
}

static void fbread2_4(uint32_t curpixel, uint32_t* curpixel_memcvg)
{
   COLOR_RED(pre_memory_color)   = 0x00;
   COLOR_GREEN(pre_memory_color) = 0x00;
   COLOR_BLUE(pre_memory_color)  = 0x00;
   COLOR_ALPHA(pre_memory_color) = 0xE0;
   *curpixel_memcvg              = 7;
}

static void fbread_8(uint32_t curpixel, uint32_t* curpixel_memcvg)
{
   uint8_t mem;
   uint32_t addr = fb_address + curpixel;
   RREADADDR8(mem, addr);

   COLOR_RED(memory_color)   = mem;
   COLOR_GREEN(memory_color) = mem;
   COLOR_BLUE(memory_color)  = mem;
   COLOR_ALPHA(memory_color) = 0xE0;
   *curpixel_memcvg = 7;
}

static void fbread2_8(uint32_t curpixel, uint32_t* curpixel_memcvg)
{
   uint8_t mem;
   uint32_t addr = fb_address + curpixel;
   RREADADDR8(mem, addr);

   COLOR_RED(pre_memory_color)   = mem;
   COLOR_GREEN(pre_memory_color) = mem;
   COLOR_BLUE(pre_memory_color)  = mem;
   COLOR_ALPHA(pre_memory_color) = 0xE0;
   *curpixel_memcvg = 7;
}

static INLINE void fbread_16(uint32_t curpixel, uint32_t* curpixel_memcvg)
{
	uint16_t fword;
	uint8_t hbyte;
	uint32_t addr = (fb_address >> 1) + curpixel;
	
	uint8_t lowbits;

	
	if (other_modes.image_read_en)
	{
		PAIRREAD16(fword, hbyte, addr);

		if (fb_format == FORMAT_RGBA)
		{
			COLOR_RED(memory_color)   = GET_HI(fword);
			COLOR_GREEN(memory_color) = GET_MED(fword);
			COLOR_BLUE(memory_color)  = GET_LOW(fword);
			lowbits = ((fword & 1) << 2) | hbyte;
		}
		else
		{
			COLOR_RED(memory_color)   = fword >> 8;
         COLOR_GREEN(memory_color) = fword >> 8;
         COLOR_BLUE(memory_color)  = fword >> 8;
			lowbits = (fword >> 5) & 7;
		}

		*curpixel_memcvg = lowbits;
		COLOR_ALPHA(memory_color) = lowbits << 5;
	}
	else
	{
		RREADIDX16(fword, addr);

		if (fb_format == FORMAT_RGBA)
		{
			COLOR_RED(memory_color)   = GET_HI(fword);
			COLOR_GREEN(memory_color) = GET_MED(fword);
			COLOR_BLUE(memory_color)  = GET_LOW(fword);
		}
		else
      {
			COLOR_RED(memory_color)   = fword >> 8;
         COLOR_GREEN(memory_color) = fword >> 8;
         COLOR_BLUE(memory_color)  = fword >> 8;
      }

		*curpixel_memcvg = 7;
		COLOR_ALPHA(memory_color) = 0xe0;
	}
}

static INLINE void fbread2_16(uint32_t curpixel, uint32_t* curpixel_memcvg)
{
	uint16_t fword;
	uint8_t hbyte;
	uint32_t addr = (fb_address >> 1) + curpixel;
	
	uint8_t lowbits;

	if (other_modes.image_read_en)
	{
		PAIRREAD16(fword, hbyte, addr);

		if (fb_format == FORMAT_RGBA)
		{
			COLOR_RED(pre_memory_color)    = GET_HI(fword);
			COLOR_GREEN(pre_memory_color)  = GET_MED(fword);
			COLOR_BLUE(pre_memory_color)   = GET_LOW(fword);
			lowbits = ((fword & 1) << 2) | hbyte;
		}
		else
		{
			COLOR_RED(pre_memory_color)     = fword >> 8;
         COLOR_GREEN(pre_memory_color)   = fword >> 8;
         COLOR_BLUE(pre_memory_color)    = fword >> 8;
			lowbits = (fword >> 5) & 7;
		}

		*curpixel_memcvg              = lowbits;
		COLOR_ALPHA(pre_memory_color) = lowbits << 5;
	}
	else
	{
		RREADIDX16(fword, addr);

		if (fb_format == FORMAT_RGBA)
		{
			COLOR_RED(pre_memory_color)   = GET_HI(fword);
			COLOR_GREEN(pre_memory_color) = GET_MED(fword);
			COLOR_BLUE(pre_memory_color)  = GET_LOW(fword);
		}
		else
      {
			COLOR_RED(pre_memory_color)   = fword >> 8;
         COLOR_GREEN(pre_memory_color) = fword >> 8;
         COLOR_BLUE(pre_memory_color)  = fword >> 8;
      }

		*curpixel_memcvg              = 7;
		COLOR_ALPHA(pre_memory_color) = 0xe0;
	}
	
}

static void fbread_32(uint32_t curpixel, uint32_t* curpixel_memcvg)
{
    uint32_t mem, addr = (fb_address >> 2) + curpixel;
    RREADIDX32(mem, addr);

    COLOR_RED(memory_color)    = (mem >> 24) & 0xFF;
    COLOR_GREEN(memory_color)  = (mem >> 16) & 0xFF;
    COLOR_BLUE(memory_color)   = (mem >>  8) & 0xFF;

    COLOR_ALPHA(memory_color)  = (mem >>  0) & 0xFF;
    COLOR_ALPHA(memory_color) |= ~(-other_modes.image_read_en);
    COLOR_ALPHA(memory_color) &= 0xE0;

    *curpixel_memcvg = (unsigned char)(COLOR_ALPHA(memory_color)) >> 5;
}

static void fbread2_32(uint32_t curpixel, uint32_t* curpixel_memcvg)
{
   uint32_t mem, addr = (fb_address >> 2) + curpixel; 
   RREADIDX32(mem, addr);

   COLOR_RED(pre_memory_color)    = (mem >> 24) & 0xFF;
   COLOR_GREEN(pre_memory_color)  = (mem >> 16) & 0xFF;
   COLOR_BLUE(pre_memory_color)   = (mem >>  8) & 0xFF;

   COLOR_ALPHA(pre_memory_color)  = (mem >>  0) & 0xFF;
   COLOR_ALPHA(pre_memory_color) |= ~(-other_modes.image_read_en);
   COLOR_ALPHA(pre_memory_color)  &= 0xE0;

   *curpixel_memcvg = (unsigned char)(COLOR_ALPHA(pre_memory_color)) >> 5;
}































/*
 * ******************
 * RDP
 * ******************
*/

#ifdef HAVE_RDP_DUMP
#include "../mupen64plus-video-paraLLEl/rdp_dump.h"

static int rdp_dump_active;
#endif

struct stepwalker_info
{
#ifdef USE_SSE_SUPPORT
   __m128i xmm_d_rgba_de, xmm_d_stwz_de;
#endif
   int32_t rgba[4]; /* RGBA color components */
   int32_t d_rgba_dx[4]; /* RGBA delda per x-coordinate delta */
   int32_t d_rgba_de[4]; /* RGBA delta along the edge */
   int32_t d_rgba_dy[4]; /* RGBA delta per y-coordinate delta */
   int16_t rgba_int[4], rgba_frac[4];
   int16_t d_rgba_dx_int[4], d_rgba_dx_frac[4];
   int16_t d_rgba_de_int[4], d_rgba_de_frac[4];
   int16_t d_rgba_dy_int[4], d_rgba_dy_frac[4];

   int32_t stwz[4];
   int32_t d_stwz_dx[4];
   int32_t d_stwz_de[4];
   int32_t d_stwz_dy[4];
   int16_t stwz_int[4], stwz_frac[4];
   int16_t d_stwz_dx_int[4], d_stwz_dx_frac[4];
   int16_t d_stwz_de_int[4], d_stwz_de_frac[4];
   int16_t d_stwz_dy_int[4], d_stwz_dy_frac[4];

   int32_t d_rgba_dxh[4];
   int32_t d_stwz_dxh[4];
   int32_t d_rgba_diff[4], d_stwz_diff[4];
   int32_t xlr[2], xlr_inc[2];
   int base;
   uint8_t xfrac;
};

static int cmd_cur;
static int cmd_ptr; /* for 64-bit elements, always <= +0x7FFF */

/* static DP_FIFO cmd_fifo; */
static DP_FIFO cmd_data[0x0003FFFF/sizeof(int64_t) + 1];

static void invalid(uint32_t w1, uint32_t w2);
static void noop(uint32_t w1, uint32_t w2);
static void tri_noshade(uint32_t w1, uint32_t w2);
static void tri_noshade_z(uint32_t w1, uint32_t w2);

static void tri_tex(uint32_t w1, uint32_t w2);
static void tri_tex_z(uint32_t w1, uint32_t w2);
static void tri_shade(uint32_t w1, uint32_t w2);
static void tri_shade_z(uint32_t w1, uint32_t w2);
static void tri_texshade(uint32_t w1, uint32_t w2);
static void tri_texshade_z(uint32_t w1, uint32_t w2);
static void tex_rect(uint32_t w1, uint32_t w2);
static void tex_rect_flip(uint32_t w1, uint32_t w2);
static void sync_load(uint32_t w1, uint32_t w2);
static void sync_pipe(uint32_t w1, uint32_t w2);
static void sync_tile(uint32_t w1, uint32_t w2);
static void sync_full(uint32_t w1, uint32_t w2);
static void set_key_gb(uint32_t w1, uint32_t w2);
static void set_key_r(uint32_t w1, uint32_t w2);
static void set_convert(uint32_t w1, uint32_t w2);
static void set_scissor(uint32_t w1, uint32_t w2);
static void set_prim_depth(uint32_t w1, uint32_t w2);
static void set_other_modes(uint32_t w1, uint32_t w2);
static void set_tile_size(uint32_t w1, uint32_t w2);
static void load_block(uint32_t w1, uint32_t w2);
static void load_tlut(uint32_t w1, uint32_t w2);
static void load_tile(uint32_t w1, uint32_t w2);
static void set_tile(uint32_t w1, uint32_t w2);
static void fill_rect(uint32_t w1, uint32_t w2);
static void set_fill_color(uint32_t w1, uint32_t w2);
static void set_fog_color(uint32_t w1, uint32_t w2);
static void set_blend_color(uint32_t w1, uint32_t w2);
static void set_prim_color(uint32_t w1, uint32_t w2);
static void set_env_color(uint32_t w1, uint32_t w2);
static void set_combine(uint32_t w1, uint32_t w2);
static void set_texture_image(uint32_t w1, uint32_t w2);
static void set_mask_image(uint32_t w1, uint32_t w2);
static void set_color_image(uint32_t w1, uint32_t w2);

static INLINE uint16_t normalize_dzpix(uint16_t sum)
{
    int count;

    if (sum & 0xC000)
        return 0x8000;
    if (sum == 0x0000)
        return 0x0001;
    if (sum == 0x0001)
        return 0x0003;
    for (count = 0x2000; count > 0; count >>= 1)
        if (sum & count)
            return (count << 1);
    return 0;
}

static void (*const rdp_command_table[64])(uint32_t, uint32_t) = {
    noop              ,invalid           ,invalid           ,invalid           ,
    invalid           ,invalid           ,invalid           ,invalid           ,
    tri_noshade       ,tri_noshade_z     ,tri_tex           ,tri_tex_z         ,
    tri_shade         ,tri_shade_z       ,tri_texshade      ,tri_texshade_z    ,

    invalid           ,invalid           ,invalid           ,invalid           ,
    invalid           ,invalid           ,invalid           ,invalid           ,
    invalid           ,invalid           ,invalid           ,invalid           ,
    invalid           ,invalid           ,invalid           ,invalid           ,

    invalid           ,invalid           ,invalid           ,invalid           ,
    tex_rect          ,tex_rect_flip     ,sync_load         ,sync_pipe         ,
    sync_tile         ,sync_full         ,set_key_gb        ,set_key_r         ,
    set_convert       ,set_scissor       ,set_prim_depth    ,set_other_modes   ,

    load_tlut         ,invalid           ,set_tile_size     ,load_block        ,
    load_tile         ,set_tile          ,fill_rect         ,set_fill_color    ,
    set_fog_color     ,set_blend_color   ,set_prim_color    ,set_env_color     ,
    set_combine       ,set_texture_image ,set_mask_image    ,set_color_image   ,
};

static const int DP_CMD_LEN_W[64] = { /* command length, in DP FIFO words */
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (32) / 8         ,(32+16) / 8      ,(32+64) / 8      ,(32+64+16) / 8   ,
    (32+64) / 8      ,(32+64+16) / 8   ,(32+64+64) / 8   ,(32+64+64+16) / 8,

    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,

    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (16) / 8         ,(16) / 8         ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,

    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
    (8) / 8          ,(8) / 8          ,(8) / 8          ,(8) / 8          ,
};

#ifdef TRACE_DP_COMMANDS
static long cmd_count[64];

static const char DP_command_names[64][18] = {
    "NOOP             ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "TRIFILL          ",
    "TRIFILLZBUFF     ",
    "TRITXTR          ",
    "TRITXTRZBUFF     ",
    "TRISHADE         ",
    "TRISHADEZBUFF    ",
    "TRISHADETXTR     ",
    "TRISHADETXTRZBUFF",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "reserved         ",
    "TEXRECT          ",
    "TEXRECTFLIP      ",
    "LOADSYNC         ",
    "PIPESYNC         ",
    "TILESYNC         ",
    "FULLSYNC         ",
    "SETKEYGB         ",
    "SETKEYR          ",
    "SETCONVERT       ",
    "SETSCISSOR       ",
    "SETPRIMDEPTH     ",
    "SETRDPOTHER      ",
    "LOADTLUT         ",
    "reserved         ",
    "SETTILESIZE      ",
    "LOADBLOCK        ",
    "LOADTILE         ",
    "SETTILE          ",
    "FILLRECT         ",
    "SETFILLCOLOR     ",
    "SETFOGCOLOR      ",
    "SETBLENDCOLOR    ",
    "SETPRIMCOLOR     ",
    "SETENVCOLOR      ",
    "SETCOMBINE       ",
    "SETTIMG          ",
    "SETMIMG          ",
    "SETCIMG          "
};

static void count_DP_commands(void)
{
    int i;
    FILE * stream;
    long total = 0;

    for (i = 0; i < 64; i++)
        total += cmd_count[i];
    stream = fopen("dp_count.txt", "w");
    for (i = 0; i < 64; i++)
        fprintf(
            stream, "%s:  %ld (%f%%)\n", DP_command_names[i], cmd_count[i],
            (float)(cmd_count[i])/(float)(total) * 100.F);
    fclose(stream);
    for (i = 0; i < 64; i++)
        cmd_count[i] = 0; /* reset for fresh data */
}
#endif

void process_RDP_list(void)
{
    int length;
    unsigned int offset;
    const uint32_t DP_CURRENT = *GET_GFX_INFO(DPC_CURRENT_REG) & 0x00FFFFF8;
    const uint32_t DP_END     = *GET_GFX_INFO(DPC_END_REG)     & 0x00FFFFF8;

    *GET_GFX_INFO(DPC_STATUS_REG) &= ~DP_STATUS_FREEZE;

    length = DP_END - DP_CURRENT;
    if (length <= 0)
        return;
    length = (unsigned)(length) / sizeof(int64_t);
    if ((cmd_ptr + length) & ~(0x0003FFFF / sizeof(int64_t)))
    {
        DisplayError("ProcessRDPList\nOut of command cache memory.");
        return;
    }

#ifdef HAVE_RDP_DUMP
    /* Flush out changes in DRAM if anything has changed. */
    if (!rdp_dump_active)
    {
       rdp_dump_flush_dram(DRAM, 8 * 1024 * 1024);
       rdp_dump_begin_command_list();
       rdp_dump_active = 1;
    }
#endif

    --length; /* filling in cmd data in backwards order for performance */
    offset = (DP_END - sizeof(int64_t)) / sizeof(int64_t);
    if (*GET_GFX_INFO(DPC_STATUS_REG) & DP_STATUS_XBUS_DMA)
        do
        {
            offset &= 0xFFF / sizeof(int64_t);
            BUFFERFIFO(cmd_ptr + length, SP_DMEM, offset);
            offset -= 0x001 * sizeof(int8_t);
        } while (--length >= 0);
    else
        if (DP_END > plim || DP_CURRENT > plim)
        {
            DisplayError("DRAM access violation overrides");
            return;
        }
        else
        {
            do
            {
                offset &= 0xFFFFFF / sizeof(int64_t);
                BUFFERFIFO(cmd_ptr + length, DRAM, offset);
                offset -= 0x000001 * sizeof(int8_t);
            } while (--length >= 0);
        }
    cmd_ptr += (DP_END - DP_CURRENT) / sizeof(int64_t); /* += length */
    if (rdp_pipeline_crashed != 0)
        goto exit_a;

    while (cmd_cur - cmd_ptr < 0)
    {
        uint32_t w1    = cmd_data[cmd_cur + 0].UW32[0];
        uint32_t w2    = cmd_data[cmd_cur + 0].UW32[1];
        int command    = (w1 >> 24) % 64;
        int cmd_length = sizeof(int64_t)/sizeof(int64_t) * DP_CMD_LEN_W[command];
#ifdef TRACE_DP_COMMANDS
        ++cmd_count[command];
#endif
        if (cmd_ptr - cmd_cur - cmd_length < 0)
            goto exit_b;

#ifdef HAVE_RDP_DUMP
        rdp_dump_emit_command(command,
              (const uint32_t*)(cmd_data + cmd_cur), cmd_length * 2);
#endif

        rdp_command_table[command](w1, w2);
        cmd_cur += cmd_length;
    };
exit_a:
    cmd_ptr = 0;
    cmd_cur = 0;
exit_b:
    *GET_GFX_INFO(DPC_START_REG)
  = *GET_GFX_INFO(DPC_CURRENT_REG)
  = *GET_GFX_INFO(DPC_END_REG);
}

static char invalid_command[] = "00\nDP reserved command.";

static void invalid(uint32_t w1, uint32_t w2)
{
    const unsigned int command = (w1 & 0x3F000000) >> 24;

    invalid_command[0] = '0' | command >> 3;
    invalid_command[1] = '0' | command & 07;
    DisplayError(invalid_command);
}

static void noop(uint32_t w1, uint32_t w2)
{
}

static INLINE void stepwalker_info_init(struct stepwalker_info *stw_info)
{
   stw_info->base = cmd_cur + 0;

   setzero_si64(stw_info->rgba_int);
   setzero_si64(stw_info->rgba_frac);
   setzero_si64(stw_info->d_rgba_dx_int);
   setzero_si64(stw_info->d_rgba_dx_frac);
   setzero_si64(stw_info->d_rgba_de_int);
   setzero_si64(stw_info->d_rgba_de_frac);
   setzero_si64(stw_info->d_rgba_dy_int);
   setzero_si64(stw_info->d_rgba_dy_frac);
   setzero_si64(stw_info->stwz_int);
   setzero_si64(stw_info->stwz_frac);
   setzero_si64(stw_info->d_stwz_dx_int);
   setzero_si64(stw_info->d_stwz_dx_frac);
   setzero_si64(stw_info->d_stwz_de_int);
   setzero_si64(stw_info->d_stwz_de_frac);
   setzero_si64(stw_info->d_stwz_dy_int);
   setzero_si64(stw_info->d_stwz_dy_frac);

}

static NOINLINE void draw_triangle(uint32_t w1, uint32_t w2,
      int shade, int texture, int zbuffer, struct stepwalker_info *stw_info)
{
    int sign_dxhdy;
    int ycur, ylfar;
    int yllimit, yhlimit;
    int ldflag;
    int invaly;
    int curcross;
    int allover, allunder, curover, curunder;
    int allinval;
    int j, k;
    const int32_t clipxlshift = __clip.xl << 1;
    const int32_t clipxhshift = __clip.xh << 1;

    stepwalker_info_init(stw_info);

    /* Edge Coefficients */
    int lft     = (w1 & 0x00800000) >> (55 - 32);
    /* unused  (w1 & 0x00400000) >> (54 - 32) */
    int level   = (w1 & 0x00380000) >> (51 - 32);
    int tile    = (w1 & 0x00070000) >> (48 - 32);
    int flip    = lft;
    max_level   = level;
    int tilenum = tile;

    /* Triangle edge Y-coordinates */
    int32_t      yl = (w1 & 0x0000FFFF) >> (32 - 32); /* & 0x3FFF */
    int32_t      ym = (w2 & 0xFFFF0000) >> (16 -  0); /* & 0x3FFF */
    int32_t      yh = (w2 & 0x0000FFFF) >> ( 0 -  0); /* & 0x3FFF */
    /* Triangle edge X-coordinates */
    int32_t      xl = cmd_data[stw_info->base + 1].UW32[0];
    int32_t      xh = cmd_data[stw_info->base + 2].UW32[0];
    int32_t      xm = cmd_data[stw_info->base + 3].UW32[0];
    /* Triangle edge inverse-slopes */
    int32_t   DxLDy = cmd_data[stw_info->base + 1].UW32[1];
    int32_t   DxHDy = cmd_data[stw_info->base + 2].UW32[1];
    int32_t   DxMDy = cmd_data[stw_info->base + 3].UW32[1];

    yl = SIGN(yl, 14);
    ym = SIGN(ym, 14);
    yh = SIGN(yh, 14);

    xl = SIGN(xl, 30);
    xh = SIGN(xh, 30);
    xm = SIGN(xm, 30);

    /* Shade Coefficients */
    if (shade == 0) /* branch unlikely */
        goto no_read_shade_coefficients;
    stw_info->rgba_int[0] = (cmd_data[stw_info->base + 4].UW32[0] >> 16) & 0xFFFF;
    stw_info->rgba_int[1] = (cmd_data[stw_info->base + 4].UW32[0] >>  0) & 0xFFFF;
    stw_info->rgba_int[2] = (cmd_data[stw_info->base + 4].UW32[1] >> 16) & 0xFFFF;
    stw_info->rgba_int[3] = (cmd_data[stw_info->base + 4].UW32[1] >>  0) & 0xFFFF;
    stw_info->d_rgba_dx_int[0] = (cmd_data[stw_info->base + 5].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_rgba_dx_int[1] = (cmd_data[stw_info->base + 5].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_rgba_dx_int[2] = (cmd_data[stw_info->base + 5].UW32[1] >> 16) & 0xFFFF;
    stw_info->d_rgba_dx_int[3] = (cmd_data[stw_info->base + 5].UW32[1] >>  0) & 0xFFFF;
    stw_info->rgba_frac[0] = (cmd_data[stw_info->base + 6].UW32[0] >> 16) & 0xFFFF;
    stw_info->rgba_frac[1] = (cmd_data[stw_info->base + 6].UW32[0] >>  0) & 0xFFFF;
    stw_info->rgba_frac[2] = (cmd_data[stw_info->base + 6].UW32[1] >> 16) & 0xFFFF;
    stw_info->rgba_frac[3] = (cmd_data[stw_info->base + 6].UW32[1] >>  0) & 0xFFFF;
    stw_info->d_rgba_dx_frac[0] = (cmd_data[stw_info->base + 7].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_rgba_dx_frac[1] = (cmd_data[stw_info->base + 7].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_rgba_dx_frac[2] = (cmd_data[stw_info->base + 7].UW32[1] >> 16) & 0xFFFF;
    stw_info->d_rgba_dx_frac[3] = (cmd_data[stw_info->base + 7].UW32[1] >>  0) & 0xFFFF;
    stw_info->d_rgba_de_int[0] = (cmd_data[stw_info->base + 8].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_rgba_de_int[1] = (cmd_data[stw_info->base + 8].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_rgba_de_int[2] = (cmd_data[stw_info->base + 8].UW32[1] >> 16) & 0xFFFF;
    stw_info->d_rgba_de_int[3] = (cmd_data[stw_info->base + 8].UW32[1] >>  0) & 0xFFFF;
    stw_info->d_rgba_dy_int[0] = (cmd_data[stw_info->base + 9].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_rgba_dy_int[1] = (cmd_data[stw_info->base + 9].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_rgba_dy_int[2] = (cmd_data[stw_info->base + 9].UW32[1] >> 16) & 0xFFFF;
    stw_info->d_rgba_dy_int[3] = (cmd_data[stw_info->base + 9].UW32[1] >>  0) & 0xFFFF;
    stw_info->d_rgba_de_frac[0] = (cmd_data[stw_info->base + 10].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_rgba_de_frac[1] = (cmd_data[stw_info->base + 10].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_rgba_de_frac[2] = (cmd_data[stw_info->base + 10].UW32[1] >> 16) & 0xFFFF;
    stw_info->d_rgba_de_frac[3] = (cmd_data[stw_info->base + 10].UW32[1] >>  0) & 0xFFFF;
    stw_info->d_rgba_dy_frac[0] = (cmd_data[stw_info->base + 11].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_rgba_dy_frac[1] = (cmd_data[stw_info->base + 11].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_rgba_dy_frac[2] = (cmd_data[stw_info->base + 11].UW32[1] >> 16) & 0xFFFF;
    stw_info->d_rgba_dy_frac[3] = (cmd_data[stw_info->base + 11].UW32[1] >>  0) & 0xFFFF;
    stw_info->base += 8;
no_read_shade_coefficients:
    stw_info->base -= 8;
    stw_info->rgba[0]      = (stw_info->rgba_int[0] << 16) | (uint16_t)(stw_info->rgba_frac[0]);
    stw_info->rgba[1]      = (stw_info->rgba_int[1] << 16) | (uint16_t)(stw_info->rgba_frac[1]);
    stw_info->rgba[2]      = (stw_info->rgba_int[2] << 16) | (uint16_t)(stw_info->rgba_frac[2]);
    stw_info->rgba[3]      = (stw_info->rgba_int[3] << 16) | (uint16_t)(stw_info->rgba_frac[3]);
    stw_info->d_rgba_dx[0] = (stw_info->d_rgba_dx_int[0] << 16) | (uint16_t)(stw_info->d_rgba_dx_frac[0]);
    stw_info->d_rgba_dx[1] = (stw_info->d_rgba_dx_int[1] << 16) | (uint16_t)(stw_info->d_rgba_dx_frac[1]);
    stw_info->d_rgba_dx[2] = (stw_info->d_rgba_dx_int[2] << 16) | (uint16_t)(stw_info->d_rgba_dx_frac[2]);
    stw_info->d_rgba_dx[3] = (stw_info->d_rgba_dx_int[3] << 16) | (uint16_t)(stw_info->d_rgba_dx_frac[3]);
    stw_info->d_rgba_de[0] = (stw_info->d_rgba_de_int[0] << 16) | (uint16_t)(stw_info->d_rgba_de_frac[0]);
    stw_info->d_rgba_de[1] = (stw_info->d_rgba_de_int[1] << 16) | (uint16_t)(stw_info->d_rgba_de_frac[1]);
    stw_info->d_rgba_de[2] = (stw_info->d_rgba_de_int[2] << 16) | (uint16_t)(stw_info->d_rgba_de_frac[2]);
    stw_info->d_rgba_de[3] = (stw_info->d_rgba_de_int[3] << 16) | (uint16_t)(stw_info->d_rgba_de_frac[3]);
    stw_info->d_rgba_dy[0] = (stw_info->d_rgba_dy_int[0] << 16) | (uint16_t)(stw_info->d_rgba_dy_frac[0]);
    stw_info->d_rgba_dy[1] = (stw_info->d_rgba_dy_int[1] << 16) | (uint16_t)(stw_info->d_rgba_dy_frac[1]);
    stw_info->d_rgba_dy[2] = (stw_info->d_rgba_dy_int[2] << 16) | (uint16_t)(stw_info->d_rgba_dy_frac[2]);
    stw_info->d_rgba_dy[3] = (stw_info->d_rgba_dy_int[3] << 16) | (uint16_t)(stw_info->d_rgba_dy_frac[3]);

    /* Texture Coefficients */
    if (texture == 0)
        goto no_read_texture_coefficients;
    stw_info->stwz_int[0]       = (cmd_data[stw_info->base + 12].UW32[0] >> 16) & 0xFFFF;
    stw_info->stwz_int[1]       = (cmd_data[stw_info->base + 12].UW32[0] >>  0) & 0xFFFF;
    stw_info->stwz_int[2]       = (cmd_data[stw_info->base + 12].UW32[1] >> 16) & 0xFFFF;
 /* stw_info->stwz_int[3]       = (cmd_data[stw_info->base + 12].UW32[1] >>  0) & 0xFFFF; */
    stw_info->d_stwz_dx_int[0]  = (cmd_data[stw_info->base + 13].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_stwz_dx_int[1]  = (cmd_data[stw_info->base + 13].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_stwz_dx_int[2]  = (cmd_data[stw_info->base + 13].UW32[1] >> 16) & 0xFFFF;
 /* stw_info->d_stwz_dx_int[3]  = (cmd_data[stw_info->base + 13].UW32[1] >>  0) & 0xFFFF; */
    stw_info->stwz_frac[0]      = (cmd_data[stw_info->base + 14].UW32[0] >> 16) & 0xFFFF;
    stw_info->stwz_frac[1]      = (cmd_data[stw_info->base + 14].UW32[0] >>  0) & 0xFFFF;
    stw_info->stwz_frac[2]      = (cmd_data[stw_info->base + 14].UW32[1] >> 16) & 0xFFFF;
 /* stw_info->stwz_frac[3]      = (cmd_data[stw_info->base + 14].UW32[1] >>  0) & 0xFFFF; */
    stw_info->d_stwz_dx_frac[0] = (cmd_data[stw_info->base + 15].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_stwz_dx_frac[1] = (cmd_data[stw_info->base + 15].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_stwz_dx_frac[2] = (cmd_data[stw_info->base + 15].UW32[1] >> 16) & 0xFFFF;
 /* stw_info->d_stwz_dx_frac[3] = (cmd_data[stw_info->base + 15].UW32[1] >>  0) & 0xFFFF; */
    stw_info->d_stwz_de_int[0]  = (cmd_data[stw_info->base + 16].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_stwz_de_int[1]  = (cmd_data[stw_info->base + 16].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_stwz_de_int[2]  = (cmd_data[stw_info->base + 16].UW32[1] >> 16) & 0xFFFF;
 /* stw_info->d_stwz_de_int[3]  = (cmd_data[stw_info->base + 16].UW32[1] >>  0) & 0xFFFF; */
    stw_info->d_stwz_dy_int[0]  = (cmd_data[stw_info->base + 17].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_stwz_dy_int[1]  = (cmd_data[stw_info->base + 17].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_stwz_dy_int[2]  = (cmd_data[stw_info->base + 17].UW32[1] >> 16) & 0xFFFF;
 /* stw_info->d_stwz_dy_int[3]  = (cmd_data[stw_info->base + 17].UW32[1] >>  0) & 0xFFFF; */
    stw_info->d_stwz_de_frac[0] = (cmd_data[stw_info->base + 18].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_stwz_de_frac[1] = (cmd_data[stw_info->base + 18].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_stwz_de_frac[2] = (cmd_data[stw_info->base + 18].UW32[1] >> 16) & 0xFFFF;
 /* stw_info->d_stwz_de_frac[3] = (cmd_data[stw_info->base + 18].UW32[1] >>  0) & 0xFFFF; */
    stw_info->d_stwz_dy_frac[0] = (cmd_data[stw_info->base + 19].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_stwz_dy_frac[1] = (cmd_data[stw_info->base + 19].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_stwz_dy_frac[2] = (cmd_data[stw_info->base + 19].UW32[1] >> 16) & 0xFFFF;
 /* stw_info->d_stwz_dy_frac[3] = (cmd_data[stw_info->base + 19].UW32[1] >>  0) & 0xFFFF; */
    stw_info->base += 8;
no_read_texture_coefficients:
    stw_info->base -= 8;

    /* Z-Buffer Coefficients */
    if (zbuffer == 0) /* branch unlikely */
        goto no_read_zbuffer_coefficients;
    stw_info->stwz_int[3]       = (cmd_data[stw_info->base + 20].UW32[0] >> 16) & 0xFFFF;
    stw_info->stwz_frac[3]      = (cmd_data[stw_info->base + 20].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_stwz_dx_int[3]  = (cmd_data[stw_info->base + 20].UW32[1] >> 16) & 0xFFFF;
    stw_info->d_stwz_dx_frac[3] = (cmd_data[stw_info->base + 20].UW32[1] >>  0) & 0xFFFF;
    stw_info->d_stwz_de_int[3]  = (cmd_data[stw_info->base + 21].UW32[0] >> 16) & 0xFFFF;
    stw_info->d_stwz_de_frac[3] = (cmd_data[stw_info->base + 21].UW32[0] >>  0) & 0xFFFF;
    stw_info->d_stwz_dy_int[3]  = (cmd_data[stw_info->base + 21].UW32[1] >> 16) & 0xFFFF;
    stw_info->d_stwz_dy_frac[3] = (cmd_data[stw_info->base + 21].UW32[1] >>  0) & 0xFFFF;
    stw_info->base += 8;
no_read_zbuffer_coefficients:
    stw_info->base -= 8;
    stw_info->stwz[0]      = (stw_info->stwz_int[0] << 16)      | (uint16_t)(stw_info->stwz_frac[0]);
    stw_info->stwz[1]      = (stw_info->stwz_int[1] << 16)      | (uint16_t)(stw_info->stwz_frac[1]);
    stw_info->stwz[2]      = (stw_info->stwz_int[2] << 16)      | (uint16_t)(stw_info->stwz_frac[2]);
    stw_info->stwz[3]      = (stw_info->stwz_int[3] << 16)      | (uint16_t)(stw_info->stwz_frac[3]);
    stw_info->d_stwz_dx[0] = (stw_info->d_stwz_dx_int[0] << 16) | (uint16_t)(stw_info->d_stwz_dx_frac[0]);
    stw_info->d_stwz_dx[1] = (stw_info->d_stwz_dx_int[1] << 16) | (uint16_t)(stw_info->d_stwz_dx_frac[1]);
    stw_info->d_stwz_dx[2] = (stw_info->d_stwz_dx_int[2] << 16) | (uint16_t)(stw_info->d_stwz_dx_frac[2]);
    stw_info->d_stwz_dx[3] = (stw_info->d_stwz_dx_int[3] << 16) | (uint16_t)(stw_info->d_stwz_dx_frac[3]);
    stw_info->d_stwz_de[0] = (stw_info->d_stwz_de_int[0] << 16) | (uint16_t)(stw_info->d_stwz_de_frac[0]);
    stw_info->d_stwz_de[1] = (stw_info->d_stwz_de_int[1] << 16) | (uint16_t)(stw_info->d_stwz_de_frac[1]);
    stw_info->d_stwz_de[2] = (stw_info->d_stwz_de_int[2] << 16) | (uint16_t)(stw_info->d_stwz_de_frac[2]);
    stw_info->d_stwz_de[3] = (stw_info->d_stwz_de_int[3] << 16) | (uint16_t)(stw_info->d_stwz_de_frac[3]);
    stw_info->d_stwz_dy[0] = (stw_info->d_stwz_dy_int[0] << 16) | (uint16_t)(stw_info->d_stwz_dy_frac[0]);
    stw_info->d_stwz_dy[1] = (stw_info->d_stwz_dy_int[1] << 16) | (uint16_t)(stw_info->d_stwz_dy_frac[1]);
    stw_info->d_stwz_dy[2] = (stw_info->d_stwz_dy_int[2] << 16) | (uint16_t)(stw_info->d_stwz_dy_frac[2]);
    stw_info->d_stwz_dy[3] = (stw_info->d_stwz_dy_int[3] << 16) | (uint16_t)(stw_info->d_stwz_dy_frac[3]);
#ifdef USE_SSE_SUPPORT
    stw_info->xmm_d_rgba_de = _mm_load_si128((__m128i *)stw_info->d_rgba_de);
    stw_info->xmm_d_stwz_de = _mm_load_si128((__m128i *)stw_info->d_stwz_de);
#endif

    /* rest of edgewalker algorithm */
    spans_d_rgba[0]    = stw_info->d_rgba_dx[0] & ~0x0000001F;
    spans_d_rgba[1]    = stw_info->d_rgba_dx[1] & ~0x0000001F;
    spans_d_rgba[2]    = stw_info->d_rgba_dx[2] & ~0x0000001F;
    spans_d_rgba[3]    = stw_info->d_rgba_dx[3] & ~0x0000001F;
    spans_d_stwz[0]    = stw_info->d_stwz_dx[0] & ~0x0000001F;
    spans_d_stwz[1]    = stw_info->d_stwz_dx[1] & ~0x0000001F;
    spans_d_stwz[2]    = stw_info->d_stwz_dx[2] & ~0x0000001F;
    spans_d_stwz[3]    = stw_info->d_stwz_dx[3];

    spans_d_rgba_dy[0] = stw_info->d_rgba_dy[0] >> 14;
    spans_d_rgba_dy[1] = stw_info->d_rgba_dy[1] >> 14;
    spans_d_rgba_dy[2] = stw_info->d_rgba_dy[2] >> 14;
    spans_d_rgba_dy[3] = stw_info->d_rgba_dy[3] >> 14;
    spans_d_rgba_dy[0] = SIGN(spans_d_rgba_dy[0], 13);
    spans_d_rgba_dy[1] = SIGN(spans_d_rgba_dy[1], 13);
    spans_d_rgba_dy[2] = SIGN(spans_d_rgba_dy[2], 13);
    spans_d_rgba_dy[3] = SIGN(spans_d_rgba_dy[3], 13);

    spans_cd_rgba[0]   = spans_d_rgba[0] >> 14;
    spans_cd_rgba[1]   = spans_d_rgba[1] >> 14;
    spans_cd_rgba[2]   = spans_d_rgba[2] >> 14;
    spans_cd_rgba[3]   = spans_d_rgba[3] >> 14;
    spans_cd_rgba[0]   = SIGN(spans_cd_rgba[0], 13);
    spans_cd_rgba[1]   = SIGN(spans_cd_rgba[1], 13);
    spans_cd_rgba[2]   = SIGN(spans_cd_rgba[2], 13);
    spans_cd_rgba[3]   = SIGN(spans_cd_rgba[3], 13);
    spans_cdz          = spans_d_stwz[3] >> 10;
    spans_cdz          = SIGN(spans_cdz, 22);

    spans_d_stwz_dy[0] = stw_info->d_stwz_dy[0] & ~0x00007FFF;
    spans_d_stwz_dy[1] = stw_info->d_stwz_dy[1] & ~0x00007FFF;
    spans_d_stwz_dy[2] = stw_info->d_stwz_dy[2] & ~0x00007FFF;
    spans_d_stwz_dy[3] = stw_info->d_stwz_dy[3] >> 10;
    spans_d_stwz_dy[3] = SIGN(spans_d_stwz_dy[3], 22);

    stw_info->d_stwz_dx_int[3] ^= (stw_info->d_stwz_dx_int[3] < 0) ? ~0 : 0;
    stw_info->d_stwz_dy_int[3] ^= (stw_info->d_stwz_dy_int[3] < 0) ? ~0 : 0;
    spans_dzpix = normalize_dzpix(stw_info->d_stwz_dx_int[3] + stw_info->d_stwz_dy_int[3]);

    sign_dxhdy = (DxHDy < 0);
    if (sign_dxhdy ^ flip) /* !do_offset */
    {
        setzero_si128(stw_info->d_rgba_diff);
        setzero_si128(stw_info->d_stwz_diff);
    }
    else
    {
        int32_t d_rgba_deh[4], d_stwz_deh[4];
        int32_t d_rgba_dyh[4], d_stwz_dyh[4];

        d_rgba_deh[0]             = stw_info->d_rgba_de[0] & ~0x000001FF;
        d_rgba_deh[1]             = stw_info->d_rgba_de[1] & ~0x000001FF;
        d_rgba_deh[2]             = stw_info->d_rgba_de[2] & ~0x000001FF;
        d_rgba_deh[3]             = stw_info->d_rgba_de[3] & ~0x000001FF;
        d_stwz_deh[0]             = stw_info->d_stwz_de[0] & ~0x000001FF;
        d_stwz_deh[1]             = stw_info->d_stwz_de[1] & ~0x000001FF;
        d_stwz_deh[2]             = stw_info->d_stwz_de[2] & ~0x000001FF;
        d_stwz_deh[3]             = stw_info->d_stwz_de[3] & ~0x000001FF;

        d_rgba_dyh[0]             = stw_info->d_rgba_dy[0] & ~0x000001FF;
        d_rgba_dyh[1]             = stw_info->d_rgba_dy[1] & ~0x000001FF;
        d_rgba_dyh[2]             = stw_info->d_rgba_dy[2] & ~0x000001FF;
        d_rgba_dyh[3]             = stw_info->d_rgba_dy[3] & ~0x000001FF;
        d_stwz_dyh[0]             = stw_info->d_stwz_dy[0] & ~0x000001FF;
        d_stwz_dyh[1]             = stw_info->d_stwz_dy[1] & ~0x000001FF;
        d_stwz_dyh[2]             = stw_info->d_stwz_dy[2] & ~0x000001FF;
        d_stwz_dyh[3]             = stw_info->d_stwz_dy[3] & ~0x000001FF;

        stw_info->d_rgba_diff[0]  = d_rgba_deh[0] - d_rgba_dyh[0];
        stw_info->d_rgba_diff[1]  = d_rgba_deh[1] - d_rgba_dyh[1];
        stw_info->d_rgba_diff[2]  = d_rgba_deh[2] - d_rgba_dyh[2];
        stw_info->d_rgba_diff[3]  = d_rgba_deh[3] - d_rgba_dyh[3];
        stw_info->d_rgba_diff[0] -= (stw_info->d_rgba_diff[0] >> 2);
        stw_info->d_rgba_diff[1] -= (stw_info->d_rgba_diff[1] >> 2);
        stw_info->d_rgba_diff[2] -= (stw_info->d_rgba_diff[2] >> 2);
        stw_info->d_rgba_diff[3] -= (stw_info->d_rgba_diff[3] >> 2);
        stw_info->d_stwz_diff[0]  = d_stwz_deh[0] - d_stwz_dyh[0];
        stw_info->d_stwz_diff[1]  = d_stwz_deh[1] - d_stwz_dyh[1];
        stw_info->d_stwz_diff[2]  = d_stwz_deh[2] - d_stwz_dyh[2];
        stw_info->d_stwz_diff[3]  = d_stwz_deh[3] - d_stwz_dyh[3];
        stw_info->d_stwz_diff[0] -= (stw_info->d_stwz_diff[0] >> 2);
        stw_info->d_stwz_diff[1] -= (stw_info->d_stwz_diff[1] >> 2);
        stw_info->d_stwz_diff[2] -= (stw_info->d_stwz_diff[2] >> 2);
        stw_info->d_stwz_diff[3] -= (stw_info->d_stwz_diff[3] >> 2);
    }

    if (other_modes.cycle_type == CYCLE_TYPE_COPY)
    {
        setzero_si128(stw_info->d_rgba_dxh);
        setzero_si128(stw_info->d_stwz_dxh);
    }
    else
    {
        stw_info->d_rgba_dxh[0] = (stw_info->d_rgba_dx[0] >> 8) & ~0x00000001;
        stw_info->d_rgba_dxh[1] = (stw_info->d_rgba_dx[1] >> 8) & ~0x00000001;
        stw_info->d_rgba_dxh[2] = (stw_info->d_rgba_dx[2] >> 8) & ~0x00000001;
        stw_info->d_rgba_dxh[3] = (stw_info->d_rgba_dx[3] >> 8) & ~0x00000001;
        stw_info->d_stwz_dxh[0] = (stw_info->d_stwz_dx[0] >> 8) & ~0x00000001;
        stw_info->d_stwz_dxh[1] = (stw_info->d_stwz_dx[1] >> 8) & ~0x00000001;
        stw_info->d_stwz_dxh[2] = (stw_info->d_stwz_dx[2] >> 8) & ~0x00000001;
        stw_info->d_stwz_dxh[3] = (stw_info->d_stwz_dx[3] >> 8) & ~0x00000001;
    }

    ldflag = (sign_dxhdy ^ flip) ? 0 : 3;
    invaly = 1;
    yllimit = (yl - __clip.yl < 0) ? yl : __clip.yl; /* clip.yl always &= 0xFFF */

    ycur = yh & ~3;
    ylfar = yllimit | 3;
    if (yl >> 2 > ylfar >> 2)
        ylfar += 4;
    else if (yllimit >> 2 >= 0 && yllimit >> 2 < 1023)
        span[(yllimit >> 2) + 1].validline = 0;

    yhlimit              = (yh - __clip.yh >= 0) ? yh : __clip.yh; /* clip.yh always &= 0xFFF */

    stw_info->xlr_inc[0] = (DxMDy >> 2) & ~0x00000001;
    stw_info->xlr_inc[1] = (DxHDy >> 2) & ~0x00000001;
    stw_info->xlr[0]     = xm & ~0x00000001;
    stw_info->xlr[1]     = xh & ~0x00000001;
    stw_info->xfrac      = (stw_info->xlr[1] >> 8) & 0xFF;

    allover = 1;
    allunder = 1;
    curover = 0;
    curunder = 0;
    allinval = 1;

    for (k = ycur; k <= ylfar; k++)
    {
        static int minmax[2];
        int stickybit;
        int xlrsc[2];
        const int spix = k & 3;
        const int yhclose = yhlimit & ~3;

        if (k == ym)
        {
            stw_info->xlr[0]     = xl & ~0x00000001;
            stw_info->xlr_inc[0] = (DxLDy >> 2) & ~0x00000001;
        }

        if (k < yhclose)
            { /* branch */ }
        else
        {
            invaly = (uint32_t)(k - yhlimit)>>31 | (uint32_t)~(k - yllimit)>>31;
            j = k >> 2;
            if (spix == 0)
            {
                minmax[1] = 0x000;
                minmax[0] = 0xFFF;
                allover = allunder = 1;
                allinval = 1;
            }

            stickybit = (stw_info->xlr[1] & 0x00003FFF) - 1;
            stickybit = (uint32_t)~(stickybit) >> 31; /* (stickybit >= 0) */
            xlrsc[1] = (stw_info->xlr[1] >> 13)&0x1FFE | stickybit;
            curunder = !!(stw_info->xlr[1] & 0x08000000);
            curunder = curunder | (uint32_t)(xlrsc[1] - clipxhshift)>>31;
            xlrsc[1] = curunder ? clipxhshift : (stw_info->xlr[1]>>13)&0x3FFE | stickybit;

            curover  = !!(xlrsc[1] & 0x00002000);
            xlrsc[1] = xlrsc[1] & 0x1FFF;
            curover |= (uint32_t)~(xlrsc[1] - clipxlshift) >> 31;
            xlrsc[1] = curover ? clipxlshift : xlrsc[1];
            span[j].majorx[spix] = xlrsc[1] & 0x1FFF;
            allover &= curover;
            allunder &= curunder;

            stickybit = (stw_info->xlr[0] & 0x00003FFF) - 1; /* xleft/2 & 0x1FFF */
            stickybit = (uint32_t)~(stickybit) >> 31; /* (stickybit >= 0) */
            xlrsc[0] = (stw_info->xlr[0] >> 13)&0x1FFE | stickybit;
            curunder = !!(stw_info->xlr[0] & 0x08000000);
            curunder = curunder | (uint32_t)(xlrsc[0] - clipxhshift)>>31;
            xlrsc[0] = curunder ? clipxhshift : (stw_info->xlr[0]>>13)&0x3FFE | stickybit;
            curover  = !!(xlrsc[0] & 0x00002000);
            xlrsc[0] &= 0x1FFF;
            curover |= (uint32_t)~(xlrsc[0] - clipxlshift) >> 31;
            xlrsc[0] = curover ? clipxlshift : xlrsc[0];
            span[j].minorx[spix] = xlrsc[0] & 0x1FFF;
            allover &= curover;
            allunder &= curunder;

            curcross = ((stw_info->xlr[1 - flip]&0x0FFFC000 ^ 0x08000000)
                     <  (stw_info->xlr[0 + flip]&0x0FFFC000 ^ 0x08000000));
            invaly |= curcross;
            span[j].invalyscan[spix] = invaly;
            allinval &= invaly;
            if (invaly != 0)
                { /* branch */ }
            else
            {
                xlrsc[0] = (xlrsc[0] >> 3) & 0xFFF;
                xlrsc[1] = (xlrsc[1] >> 3) & 0xFFF;
                minmax[0]
                  = (xlrsc[flip - 0] < minmax[0]) ? xlrsc[flip - 0] : minmax[0];
                minmax[1]
                  = (xlrsc[1 - flip] > minmax[1]) ? xlrsc[1 - flip] : minmax[1];
            }

            if (spix == ldflag)
#ifdef USE_SSE_SUPPORT
            {
                __m128i xmm_frac;
                __m128i delta_x_high, delta_diff;
                __m128i prod_hi, prod_lo;
                __m128i result;

                span[j].unscrx  =  (stw_info->xlr[1]) >> 16;
                stw_info->xfrac = (stw_info->xlr[1] >> 8) & 0xFF;
                xmm_frac        = _mm_set1_epi32(stw_info->xfrac);

                delta_x_high = _mm_load_si128((__m128i *)stw_info->d_rgba_dxh);
                prod_lo = _mm_mul_epu32(delta_x_high, xmm_frac);
                delta_x_high = _mm_srli_epi64(delta_x_high, 32);
                prod_hi = _mm_mul_epu32(delta_x_high, xmm_frac);
                prod_lo = _mm_shuffle_epi32(prod_lo, _MM_SHUFFLE(3, 1, 2, 0));
                prod_hi = _mm_shuffle_epi32(prod_hi, _MM_SHUFFLE(3, 1, 2, 0));
                delta_x_high = _mm_unpacklo_epi32(prod_lo, prod_hi);

                delta_diff = _mm_load_si128((__m128i *)stw_info->d_rgba_diff);
                result = _mm_load_si128((__m128i *)stw_info->rgba);
                result = _mm_srli_epi32(result, 9);
                result = _mm_slli_epi32(result, 9);
                result = _mm_add_epi32(result, delta_diff);
                result = _mm_sub_epi32(result, delta_x_high);
                result = _mm_srli_epi32(result, 10);
                result = _mm_slli_epi32(result, 10);
                _mm_store_si128((__m128i *)span[j].rgba, result);

                delta_x_high = _mm_load_si128((__m128i *)stw_info->d_stwz_dxh);
                prod_lo      = _mm_mul_epu32(delta_x_high, xmm_frac);
                delta_x_high = _mm_srli_epi64(delta_x_high, 32);
                prod_hi      = _mm_mul_epu32(delta_x_high, xmm_frac);
                prod_lo      = _mm_shuffle_epi32(prod_lo, _MM_SHUFFLE(3, 1, 2, 0));
                prod_hi      = _mm_shuffle_epi32(prod_hi, _MM_SHUFFLE(3, 1, 2, 0));
                delta_x_high = _mm_unpacklo_epi32(prod_lo, prod_hi);

                delta_diff   = _mm_load_si128((__m128i *)stw_info->d_stwz_diff);
                result = _mm_load_si128((__m128i *)stw_info->stwz);
                result = _mm_srli_epi32(result, 9);
                result = _mm_slli_epi32(result, 9);
                result = _mm_add_epi32(result, delta_diff);
                result = _mm_sub_epi32(result, delta_x_high);
                result = _mm_srli_epi32(result, 10);
                result = _mm_slli_epi32(result, 10);
                _mm_store_si128((__m128i *)span[j].stwz, result);
            }
#else
            {
                span[j].unscrx  = (stw_info->xlr[1] >> 16);
                stw_info->xfrac = (stw_info->xlr[1] >> 8) & 0xFF;
                span[j].rgba[0]
                  = ((stw_info->rgba[0] & ~0x1FF) + stw_info->d_rgba_diff[0] - stw_info->xfrac * stw_info->d_rgba_dxh[0])
                  & ~0x000003FF;
                span[j].rgba[1]
                  = ((stw_info->rgba[1] & ~0x1FF) + stw_info->d_rgba_diff[1] - stw_info->xfrac * stw_info->d_rgba_dxh[1])
                  & ~0x000003FF;
                span[j].rgba[2]
                  = ((stw_info->rgba[2] & ~0x1FF) + stw_info->d_rgba_diff[2] - stw_info->xfrac * stw_info->d_rgba_dxh[2])
                  & ~0x000003FF;
                span[j].rgba[3]
                  = ((stw_info->rgba[3] & ~0x1FF) + stw_info->d_rgba_diff[3] - stw_info->xfrac * stw_info->d_rgba_dxh[3])
                  & ~0x000003FF;
                span[j].stwz[0]
                  = ((stw_info->stwz[0] & ~0x1FF) + stw_info->d_stwz_diff[0] - stw_info->xfrac * stw_info->d_stwz_dxh[0])
                  & ~0x000003FF;
                span[j].stwz[1]
                  = ((stw_info->stwz[1] & ~0x1FF) + stw_info->d_stwz_diff[1] - stw_info->xfrac * stw_info->d_stwz_dxh[1])
                  & ~0x000003FF;
                span[j].stwz[2]
                  = ((stw_info->stwz[2] & ~0x1FF) + stw_info->d_stwz_diff[2] - stw_info->xfrac * stw_info->d_stwz_dxh[2])
                  & ~0x000003FF;
                span[j].stwz[3]
                  = ((stw_info->stwz[3] & ~0x1FF) + stw_info->d_stwz_diff[3] - stw_info->xfrac * stw_info->d_stwz_dxh[3])
                  & ~0x000003FF;
            }
#endif
            if (spix == 3)
            {
                const int invalidline = (sckeepodd ^ j) & scfield
                                      | (allinval | allover | allunder);
                span[j].lx = minmax[flip - 0];
                span[j].rx = minmax[1 - flip];
                span[j].validline = invalidline ^ 1;
            }
        }
        if (spix == 3)
        {
            stw_info->rgba[0] += stw_info->d_rgba_de[0];
            stw_info->rgba[1] += stw_info->d_rgba_de[1];
            stw_info->rgba[2] += stw_info->d_rgba_de[2];
            stw_info->rgba[3] += stw_info->d_rgba_de[3];
            stw_info->stwz[0] += stw_info->d_stwz_de[0];
            stw_info->stwz[1] += stw_info->d_stwz_de[1];
            stw_info->stwz[2] += stw_info->d_stwz_de[2];
            stw_info->stwz[3] += stw_info->d_stwz_de[3];
        }

        stw_info->xlr[0] += stw_info->xlr_inc[0];
        stw_info->xlr[1] += stw_info->xlr_inc[1];
    }
    render_spans(yhlimit >> 2, yllimit >> 2, tilenum, flip);
}

static void tri_noshade(uint32_t w1, uint32_t w2)
{
   struct stepwalker_info stw_info;
   draw_triangle(w1, w2, SHADE_NO, TEXTURE_NO, ZBUFFER_NO, &stw_info);
}

static void tri_noshade_z(uint32_t w1, uint32_t w2)
{
   struct stepwalker_info stw_info;
   draw_triangle(w1, w2, SHADE_NO, TEXTURE_NO, ZBUFFER_YES, &stw_info);
}

static void tri_tex(uint32_t w1, uint32_t w2)
{
   struct stepwalker_info stw_info;
   draw_triangle(w1, w2, SHADE_NO, TEXTURE_YES, ZBUFFER_NO, &stw_info);
}

static void tri_tex_z(uint32_t w1, uint32_t w2)
{
   struct stepwalker_info stw_info;
   draw_triangle(w1, w2, SHADE_NO, TEXTURE_YES, ZBUFFER_YES, &stw_info);
}

static void tri_shade(uint32_t w1, uint32_t w2)
{
   struct stepwalker_info stw_info;
   draw_triangle(w1, w2, SHADE_YES, TEXTURE_NO, ZBUFFER_NO, &stw_info);
}

static void tri_shade_z(uint32_t w1, uint32_t w2)
{
   struct stepwalker_info stw_info;
   draw_triangle(w1, w2, SHADE_YES, TEXTURE_NO, ZBUFFER_YES, &stw_info);
}

static void tri_texshade(uint32_t w1, uint32_t w2)
{
   struct stepwalker_info stw_info;
   draw_triangle(w1, w2, SHADE_YES, TEXTURE_YES, ZBUFFER_NO, &stw_info);
}

static void tri_texshade_z(uint32_t w1, uint32_t w2)
{
   struct stepwalker_info stw_info;
   draw_triangle(w1, w2, SHADE_YES, TEXTURE_YES, ZBUFFER_YES, &stw_info);
}


static NOINLINE void draw_texture_rectangle(
    const int rect_flip, int tilenum,
    int32_t xl, int32_t yl, int32_t xh, int32_t yh,
    int32_t s, int32_t t, int16_t dsdx, int16_t dtdy
)
{
    int32_t xm, ym;
    int32_t yllimit, yhlimit;

    int32_t stwz[4];
    int32_t d_stwz_dx[2], d_stwz_dy[2];

    int32_t d_stwz_dxh[4];
    int32_t xleft, xright;
    uint8_t xfrac;

    int maxxmx, minxhx;
    int curcross;
    int allover, allunder, curover, curunder;
    int allinval;
    int ycur, ylfar;
    int invaly;
    int j, k;
    const int32_t clipxlshift = __clip.xl << 1;
    const int32_t clipxhshift = __clip.xh << 1;

    max_level = 0;
    maxxmx = 0;
    minxhx = 0;

    xl = xl << 14;
    xh = xh << 14;
    xm = xl;
    ym = yl;

/*
 * texture coefficients
 */
    stwz[0] = (s << 16) | 0;
    stwz[1] = (t << 16) | 0;

    setzero_si64(d_stwz_dx);
    setzero_si64(d_stwz_dy);
    if (rect_flip != TEXTURE_FLIP_NO)
    {
        d_stwz_dx[1] = (int32_t)dtdy << 11;
        d_stwz_dy[0] = (int32_t)dsdx << 11;
    }
    else
    {
        d_stwz_dx[0] = (int32_t)dsdx << 11;
        d_stwz_dy[1] = (int32_t)dtdy << 11;
    }

    setzero_si128(spans_d_rgba);
    setzero_si128(spans_d_stwz);
    spans_d_stwz[0] = d_stwz_dx[0] & ~0x0000001F;
    spans_d_stwz[1] = d_stwz_dx[1] & ~0x0000001F;

    setzero_si128(spans_d_rgba_dy);
    setzero_si128(spans_cd_rgba);
    spans_cdz = 0;

    setzero_si128(spans_d_stwz_dy);
    spans_d_stwz_dy[0] = d_stwz_dy[0] & ~0x00007FFF;
    spans_d_stwz_dy[1] = d_stwz_dy[1] & ~0x00007FFF;
    spans_dzpix = normalize_dzpix(0);

    setzero_si128(d_stwz_dxh);
    if (other_modes.cycle_type != CYCLE_TYPE_COPY)
    {
        d_stwz_dxh[0] = (d_stwz_dx[0] >> 8) & ~0x00000001;
        d_stwz_dxh[1] = (d_stwz_dx[1] >> 8) & ~0x00000001;
    }

    invaly = 1;
    yllimit = (yl <  __clip.yl) ? yl : __clip.yl;
    yhlimit = (yh >= __clip.yh) ? yh : __clip.yh;

    ycur = yh & ~3;
    ylfar = yllimit | 3;
    if ((yl >> 2) > (ylfar >> 2))
        ylfar += 4;
    else if ((yllimit >> 2) >= 0 && (yllimit >> 2) < 1023)
        span[(yllimit >> 2) + 1].validline = 0;

    xleft = xm/* & ~0x00000001 // never needed because xm <<= 14 */;
    xright = xh/* & ~0x00000001 // never needed because xh <<= 14 */;
    xfrac = (xright >> 8) & 0xFF;

    allover = 1;
    allunder = 1;
    curover = 0;
    curunder = 0;
    allinval = 1;
    for (k = ycur; k <= ylfar; k++)
    {
        int xrsc, xlsc, stickybit;
        const int spix = k & 3;
        const int yhclose = yhlimit & ~3;

        if (k < yhclose)
            { /* branch */ }
        else
        {
            invaly = (uint32_t)(k - yhlimit)>>31 | (uint32_t)~(k - yllimit)>>31;
            j = k >> 2;
            if (spix == 0)
            {
                maxxmx = 0x000;
                minxhx = 0xFFF;
                allover = allunder = 1;
                allinval = 1;
            }

            stickybit = (xright & 0x00003FFF) - 1; /* xright/2 & 0x1FFF */
            stickybit = (uint32_t)~(stickybit) >> 31; /* (stickybit >= 0) */
            xrsc = (xright >> 13)&0x1FFE | stickybit;
            curunder = !!(xright & 0x08000000);
            curunder = curunder | (uint32_t)(xrsc - clipxhshift)>>31;
            xrsc = curunder ? clipxhshift : (xright>>13)&0x3FFE | stickybit;
            curover  = !!(xrsc & 0x00002000);
            xrsc = xrsc & 0x1FFF;
            curover |= (uint32_t)~(xrsc - clipxlshift) >> 31;
            xrsc = curover ? clipxlshift : xrsc;
            span[j].majorx[spix] = xrsc & 0x1FFF;
            allover &= curover;
            allunder &= curunder;

            stickybit = (xleft & 0x00003FFF) - 1; /* xleft/2 & 0x1FFF */
            stickybit = (uint32_t)~(stickybit) >> 31; /* (stickybit >= 0) */
            xlsc = (xleft >> 13)&0x1FFE | stickybit;
            curunder = !!(xleft & 0x08000000);
            curunder = curunder | (uint32_t)(xlsc - clipxhshift)>>31;
            xlsc = curunder ? clipxhshift : (xleft>>13)&0x3FFE | stickybit;
            curover  = !!(xlsc & 0x00002000);
            xlsc &= 0x1FFF;
            curover |= (uint32_t)~(xlsc - clipxlshift) >> 31;
            xlsc = curover ? clipxlshift : xlsc;
            span[j].minorx[spix] = xlsc & 0x1FFF;
            allover &= curover;
            allunder &= curunder;

            curcross = ((xleft&0x0FFFC000 ^ 0x08000000)
                     < (xright&0x0FFFC000 ^ 0x08000000));
            invaly |= curcross;
            span[j].invalyscan[spix] = invaly;
            allinval &= invaly;
            if (invaly != 0)
                { /* branch */ }
            else
            {
                xlsc = (xlsc >> 3) & 0xFFF;
                xrsc = (xrsc >> 3) & 0xFFF;
                maxxmx = (xlsc > maxxmx) ? xlsc : maxxmx;
                minxhx = (xrsc < minxhx) ? xrsc : minxhx;
            }

            if (spix == 0)
            {
                span[j].unscrx = xright >> 16;
             /* xfrac = (xright >> 8) & 0xFF; // xfrac never changes. */
                setzero_si128(span[j].rgba);
                span[j].stwz[0] = (stwz[0] - xfrac*d_stwz_dxh[0]) & ~0x000003FF;
                span[j].stwz[1] = (stwz[1] - xfrac*d_stwz_dxh[1]) & ~0x000003FF;
                span[j].stwz[2] = 0 & ~0x000003FF;
                span[j].stwz[3] = 0 & ~0x000003FF;
            }
            if (spix == 3)
            {
                const int invalidline = (sckeepodd ^ j) & scfield
                                      | (allinval | allover | allunder);
                span[j].lx = maxxmx;
                span[j].rx = minxhx;
                span[j].validline = invalidline ^ 1;
            }
        }
        if (spix == 3)
        {
            stwz[0] = (stwz[0] + d_stwz_dy[0]) & ~0x000001FF;
            stwz[1] = (stwz[1] + d_stwz_dy[1]) & ~0x000001FF;
        }
    }

    if (other_modes.f.stalederivs)
    {
        deduce_derivatives();
        other_modes.f.stalederivs = 0;
    }
    render_spans(yhlimit >> 2, yllimit >> 2, tilenum, 1);
}

static void tex_rect(uint32_t w1, uint32_t w2)
{
    int tilenum;
    int16_t dsdx, dtdy;
    int32_t xl, yl, xh, yh;
    int32_t s, t;

    xl      = (cmd_data[cmd_cur + 0].UW32[0] & 0x00FFF000) >> 12;
    yl      = (cmd_data[cmd_cur + 0].UW32[0] & 0x00000FFF) >>  0;
    tilenum = (cmd_data[cmd_cur + 0].UW32[1] & 0x07000000) >> 24;
    xh      = (cmd_data[cmd_cur + 0].UW32[1] & 0x00FFF000) >> 12;
    yh      = (cmd_data[cmd_cur + 0].UW32[1] & 0x00000FFF) >>  0;

    yl |= (other_modes.cycle_type & 2) ? 3 : 0; /* FILL OR COPY */

    s    = (cmd_data[cmd_cur + 1].UW32[0] & 0xFFFF0000) >> 16;
    t    = (cmd_data[cmd_cur + 1].UW32[0] & 0x0000FFFF) >>  0;
    dsdx = (cmd_data[cmd_cur + 1].UW32[1] & 0xFFFF0000) >> 16;
    dtdy = (cmd_data[cmd_cur + 1].UW32[1] & 0x0000FFFF) >>  0;
    
    dsdx = SIGN16(dsdx);
    dtdy = SIGN16(dtdy);

    draw_texture_rectangle(
        TEXTURE_FLIP_NO, tilenum,
        xl, yl, xh, yh,
        s, t,
        dsdx, dtdy
    );
}

static void tex_rect_flip(uint32_t w1, uint32_t w2)
{
    int tilenum;
    int16_t dsdx, dtdy;
    int32_t xl, yl, xh, yh;
    int32_t s, t;

    xl      = (cmd_data[cmd_cur + 0].UW32[0] & 0x00FFF000) >> 12;
    yl      = (cmd_data[cmd_cur + 0].UW32[0] & 0x00000FFF) >>  0;
    tilenum = (cmd_data[cmd_cur + 0].UW32[1] & 0x07000000) >> 24;
    xh      = (cmd_data[cmd_cur + 0].UW32[1] & 0x00FFF000) >> 12;
    yh      = (cmd_data[cmd_cur + 0].UW32[1] & 0x00000FFF) >>  0;

    yl |= (other_modes.cycle_type & 2) ? 3 : 0; /* FILL OR COPY */

    s    = (cmd_data[cmd_cur + 1].UW32[0] & 0xFFFF0000) >> 16;
    t    = (cmd_data[cmd_cur + 1].UW32[0] & 0x0000FFFF) >>  0;
    dsdx = (cmd_data[cmd_cur + 1].UW32[1] & 0xFFFF0000) >> 16;
    dtdy = (cmd_data[cmd_cur + 1].UW32[1] & 0x0000FFFF) >>  0;
    
    dsdx = SIGN16(dsdx);
    dtdy = SIGN16(dtdy);

    draw_texture_rectangle(
        TEXTURE_FLIP_YES, tilenum,
        xl, yl, xh, yh,
        s, t,
        dsdx, dtdy
    );
}

static void sync_load(uint32_t w1, uint32_t w2)
{
}

static void sync_pipe(uint32_t w1, uint32_t w2)
{
}

static void sync_tile(uint32_t w1, uint32_t w2)
{
}

static void sync_full(uint32_t w1, uint32_t w2)
{
#ifdef EXTRALOGGING
   fprintf(stderr, "Sync full\n");
   fprintf(stderr, "===================\n");
#endif
    *gfx_info.MI_INTR_REG |= DP_INTERRUPT;
    gfx_info.CheckInterrupts();

#ifdef HAVE_RDP_DUMP
    if (rdp_dump_active)
       rdp_dump_end_command_list();
    rdp_dump_active = 0;
#endif
}

static void set_key_gb(uint32_t w1, uint32_t w2)
{
    COLOR_GREEN(key_width)  = (w1 & 0x00FFF000) >> 12;
    COLOR_BLUE(key_width)   = (w1 & 0x00000FFF) >>  0;
    COLOR_GREEN(key_center) = (w2 & 0xFF000000) >> 24;
    COLOR_GREEN(key_scale)  = (w2 & 0x00FF0000) >> 16;
    COLOR_BLUE(key_center)  = (w2 & 0x0000FF00) >>  8;
    COLOR_BLUE(key_scale)   = (w2 & 0x000000FF) >>  0;
}

static void set_key_r(uint32_t w1, uint32_t w2)
{
    COLOR_RED(key_width)  = (w2 & 0x0FFF0000) >> 16;
    COLOR_RED(key_center) = (w2 & 0x0000FF00) >>  8;
    COLOR_RED(key_scale)  = (w2 & 0x000000FF) >>  0;
}

static void set_convert(uint32_t w1, uint32_t w2)
{
   int32_t k0 = (w1 >> 13) & 0x1ff;
   int32_t k1 = (w1 >> 4) & 0x1ff;
   int32_t k2 = ((w1 & 0xf) << 5) | ((w2 >> 27) & 0x1f);
   int32_t k3 = (w2 >> 18) & 0x1ff;
   k0_tf = (SIGN(k0, 9) << 1) + 1;
   k1_tf = (SIGN(k1, 9) << 1) + 1;
   k2_tf = (SIGN(k2, 9) << 1) + 1;
   k3_tf = (SIGN(k3, 9) << 1) + 1;
   k4 = (w2 >> 9) & 0x1ff;
 	k5 = w2 & 0x1ff;
}

static void set_scissor(uint32_t w1, uint32_t w2)
{
    __clip.xh   = (w1 & 0x00FFF000) >> (44 - 32);
    __clip.yh   = (w1 & 0x00000FFF) >> (32 - 32);
    scfield     = (w2 & 0x02000000) >> (25 -  0);
    sckeepodd   = (w2 & 0x01000000) >> (24 -  0);
    __clip.xl   = (w2 & 0x00FFF000) >> (12 -  0);
    __clip.yl   = (w2 & 0x00000FFF) >> ( 0 -  0);
}

static void set_prim_depth(uint32_t w1, uint32_t w2)
{
    primitive_z       = (w2 & 0xFFFF0000) >> 16;
    primitive_delta_z = (w2 & 0x0000FFFF) >>  0;
    primitive_z = (primitive_z & 0x7FFF) << 16; /* angrylion does this why? */
}

static void set_other_modes(uint32_t w1, uint32_t w2)
{
    const DP_FIFO cmd_fifo = cmd_data[cmd_cur + 0];

 /* K:  atomic_prim              = (cmd_fifo.UW & 0x0080000000000000) >> 55; */
 /* j:  reserved for future use -- (cmd_fifo.UW & 0x0040000000000000) >> 54 */
    other_modes.cycle_type       = (cmd_fifo.UW32[0] & 0x00300000) >> (52 - 32);
    other_modes.persp_tex_en     = !!(cmd_fifo.UW32[0] & 0x00080000); /* 51 */
    other_modes.detail_tex_en    = !!(cmd_fifo.UW32[0] & 0x00040000); /* 50 */
    other_modes.sharpen_tex_en   = !!(cmd_fifo.UW32[0] & 0x00020000); /* 49 */
    other_modes.tex_lod_en       = !!(cmd_fifo.UW32[0] & 0x00010000); /* 48 */
    other_modes.en_tlut          = !!(cmd_fifo.UW32[0] & 0x00008000); /* 47 */
    other_modes.tlut_type        = !!(cmd_fifo.UW32[0] & 0x00004000); /* 46 */
    other_modes.sample_type      = !!(cmd_fifo.UW32[0] & 0x00002000); /* 45 */
    other_modes.mid_texel        = !!(cmd_fifo.UW32[0] & 0x00001000); /* 44 */
    other_modes.bi_lerp0         = !!(cmd_fifo.UW32[0] & 0x00000800); /* 43 */
    other_modes.bi_lerp1         = !!(cmd_fifo.UW32[0] & 0x00000400); /* 42 */
    other_modes.convert_one      = !!(cmd_fifo.UW32[0] & 0x00000200); /* 41 */
    other_modes.key_en           = !!(cmd_fifo.UW32[0] & 0x00000100); /* 40 */
    other_modes.rgb_dither_sel   = (cmd_fifo.UW32[0] & 0x000000C0) >> (38 - 32);
    other_modes.alpha_dither_sel = (cmd_fifo.UW32[0] & 0x00000030) >> (36 - 32);
 /* reserved for future, def:15 -- (cmd_fifo.UW & 0x0000000F00000000) >> 32 */
    other_modes.blend_m1a_0      = (cmd_fifo.UW32[1] & 0xC0000000) >> (30 -  0);
    other_modes.blend_m1a_1      = (cmd_fifo.UW32[1] & 0x30000000) >> (28 -  0);
    other_modes.blend_m1b_0      = (cmd_fifo.UW32[1] & 0x0C000000) >> (26 -  0);
    other_modes.blend_m1b_1      = (cmd_fifo.UW32[1] & 0x03000000) >> (24 -  0);
    other_modes.blend_m2a_0      = (cmd_fifo.UW32[1] & 0x00C00000) >> (22 -  0);
    other_modes.blend_m2a_1      = (cmd_fifo.UW32[1] & 0x00300000) >> (20 -  0);
    other_modes.blend_m2b_0      = (cmd_fifo.UW32[1] & 0x000C0000) >> (18 -  0);
    other_modes.blend_m2b_1      = (cmd_fifo.UW32[1] & 0x00030000) >> (16 -  0);
 /* N:  reserved for future use -- (cmd_fifo.UW & 0x0000000000008000) >> 15 */
    other_modes.force_blend      = !!(cmd_fifo.UW32[1] & 0x00004000); /* 14 */
    other_modes.alpha_cvg_select = !!(cmd_fifo.UW32[1] & 0x00002000); /* 13 */
    other_modes.cvg_times_alpha  = !!(cmd_fifo.UW32[1] & 0x00001000); /* 12 */
    other_modes.z_mode           = (cmd_fifo.UW32[1] & 0x00000C00) >> (10 -  0);
    other_modes.cvg_dest         = (cmd_fifo.UW32[1] & 0x00000300) >> ( 8 -  0);
    other_modes.color_on_cvg     = !!(cmd_fifo.UW32[1] & 0x00000080); /*  7 */
    other_modes.image_read_en    = !!(cmd_fifo.UW32[1] & 0x00000040); /*  6 */
    other_modes.z_update_en      = !!(cmd_fifo.UW32[1] & 0x00000020); /*  5 */
    other_modes.z_compare_en     = !!(cmd_fifo.UW32[1] & 0x00000010); /*  4 */
    other_modes.antialias_en     = !!(cmd_fifo.UW32[1] & 0x00000008); /*  3 */
    other_modes.z_source_sel     = !!(cmd_fifo.UW32[1] & 0x00000004); /*  2 */
    other_modes.dither_alpha_en  = !!(cmd_fifo.UW32[1] & 0x00000002); /*  1 */
    other_modes.alpha_compare_en = !!(cmd_fifo.UW32[1] & 0x00000001); /*  0 */

    SET_BLENDER_INPUT(
        0, 0, &blender1a_r[0], &blender1a_g[0], &blender1a_b[0],
        &blender1b_a[0], other_modes.blend_m1a_0, other_modes.blend_m1b_0);
    SET_BLENDER_INPUT(
        0, 1, &blender2a_r[0], &blender2a_g[0], &blender2a_b[0],
        &blender2b_a[0], other_modes.blend_m2a_0, other_modes.blend_m2b_0);
    SET_BLENDER_INPUT(
        1, 0, &blender1a_r[1], &blender1a_g[1], &blender1a_b[1],
        &blender1b_a[1], other_modes.blend_m1a_1, other_modes.blend_m1b_1);
    SET_BLENDER_INPUT(
        1, 1, &blender2a_r[1], &blender2a_g[1], &blender2a_b[1],
        &blender2b_a[1], other_modes.blend_m2a_1, other_modes.blend_m2b_1);

    other_modes.f.stalederivs = 1;
}

static void set_tile_size(uint32_t w1, uint32_t w2)
{
    int sl      = (w1 & 0x00FFF000) >> (44 - 32);
    int tl      = (w1 & 0x00000FFF) >> (32 - 32);
    int tilenum = (w2 & 0x07000000) >> (24 -  0);
    int sh      = (w2 & 0x00FFF000) >> (12 -  0);
    int th      = (w2 & 0x00000FFF) >> ( 0 -  0);

    tile[tilenum].sl = sl;
    tile[tilenum].tl = tl;
    tile[tilenum].sh = sh;
    tile[tilenum].th = th;
    calculate_clamp_diffs(tilenum);
}

static void load_block(uint32_t w1, uint32_t w2)
{
    int32_t lewdata[10];
    const int command = (w1 & 0xFF000000) >> (56-32);
    const int sl      = (w1 & 0x00FFF000) >> (44-32);
    const int tl      = (w1 & 0x00000FFF) >> (32-32);
    const int tilenum = (w2 & 0x07000000) >> (24- 0);
    const int sh      = (w2 & 0x00FFF000) >> (12- 0);
    const int dxt     = (w2 & 0x00000FFF) >> ( 0- 0);
    const int tlclamped = tl & 0x3FF;

    tile[tilenum].sl = sl;
    tile[tilenum].tl = tl;
    tile[tilenum].sh = sh;
    tile[tilenum].th = dxt;

    calculate_clamp_diffs(tilenum);

    lewdata[0] =
        (command << 24)
      | (0x10 << 19)
      | (tilenum << 16)
      | ((tlclamped << 2) | 3);
    lewdata[1] = (((tlclamped << 2) | 3) << 16) | (tlclamped << 2);
    lewdata[2] = sh << 16;
    lewdata[3] = sl << 16;
    lewdata[4] = sh << 16;
    lewdata[5] = ((sl << 3) << 16) | (tl << 3);
    lewdata[6] = (dxt & 0xff) << 8;
    lewdata[7] = ((0x80 >> ti_size) << 16) | (dxt >> 8);
    lewdata[8] = 0x20;
    lewdata[9] = 0x20;

    edgewalker_for_loads(lewdata);
}

static void load_tlut(uint32_t w1, uint32_t w2)
{
    tile_tlut_common_cs_decoder(w1, w2);
}

static void load_tile(uint32_t w1, uint32_t w2)
{
    tile_tlut_common_cs_decoder(w1, w2);
}

static void set_tile(uint32_t w1, uint32_t w2)
{
    const int tilenum     = (w2 & 0x07000000) >> 24;
    
    tile[tilenum].format  = (w1 & 0x00E00000) >> (53 - 32);
    tile[tilenum].size    = (w1 & 0x00180000) >> (51 - 32);
    tile[tilenum].line    = (w1 & 0x0003FE00) >> (41 - 32);
    tile[tilenum].tmem    = (w1 & 0x000001FF) >> (32 - 32);
 /* tilenum               = (cmd_fifo.UW & 0x0000000007000000) >> 24; */
    tile[tilenum].palette = (w2 & 0x00F00000) >> (20 -  0);
#ifdef EXTRALOGGING
    if (tile[tilenum].palette)
       fprintf(stderr, "Tile %d: Palette: %u\n", tilenum, tile[tilenum].palette);
#endif
    tile[tilenum].ct      = (w2 & 0x00080000) >> (19 -  0);
    tile[tilenum].mt      = (w2 & 0x00040000) >> (18 -  0);
    tile[tilenum].mask_t  = (w2 & 0x0003C000) >> (14 -  0);
    tile[tilenum].shift_t = (w2 & 0x00003C00) >> (10 -  0);
    tile[tilenum].cs      = (w2 & 0x00000200) >> ( 9 -  0);
    tile[tilenum].ms      = (w2 & 0x00000100) >> ( 8 -  0);
    tile[tilenum].mask_s  = (w2 & 0x000000F0) >> ( 4 -  0);
    tile[tilenum].shift_s = (w2 & 0x0000000F) >> ( 0 -  0);

    tile[tilenum].mask_s_maskbits = tile[tilenum].mask_s != 0
       ? ((uint16_t)(0xffff) >> (16 - tile[tilenum].mask_s)) & 0x3ff
       : 0x3ff;

    tile[tilenum].mask_t_maskbits = tile[tilenum].mask_t != 0
       ? ((uint16_t)(0xffff) >> (16 - tile[tilenum].mask_t)) & 0x3ff
       : 0x3ff;

    calculate_tile_derivs(tilenum);
}

static void fill_rect(uint32_t w1, uint32_t w2)
{
    int xlint, xhint;

    int ycur, ylfar;
    int yllimit, yhlimit;
    int invaly;
    int curcross;
    int allover, allunder, curover, curunder;
    int allinval;
    int j, k;
    const int32_t clipxlshift = __clip.xl << 1;
    const int32_t clipxhshift = __clip.xh << 1;

    int xl = (w1 & 0x00FFF000) >> (44 - 32); /* Load XL Integer Portion */
    int yl = (w1 & 0x00000FFF) >> (32 - 32); /* Load YL Integer Portion */
    int xh = (w2 & 0x00FFF000) >> (12 -  0); /* Load XH Integer Portion */
    int yh = (w2 & 0x00000FFF) >> ( 0 -  0); /* Load YH Integer Portion */

    yl |= (other_modes.cycle_type & 2) ? 3 : 0; /* FILL or COPY */

    xlint = (unsigned)(xl) >> 2;
    xhint = (unsigned)(xh) >> 2;

    max_level = 0;
    xl = (xlint << 16) | (xl & 3)<<14;
    xl = SIGN(xl, 30);
    xh = (xhint << 16) | (xh & 3)<<14;
    xh = SIGN(xh, 30);

    setzero_si128(spans_d_rgba);
    setzero_si128(spans_d_stwz);

    setzero_si128(spans_d_rgba_dy);
    setzero_si128(spans_d_stwz_dy);

    setzero_si128(spans_cd_rgba);
    spans_cdz = 0;

    spans_dzpix = normalize_dzpix(0);

    invaly = 1;
    yllimit = (yl < __clip.yl) ? yl : __clip.yl;

    ycur = yh & ~3;
    ylfar = yllimit | 3;
    if (yl >> 2 > ylfar >> 2)
        ylfar += 4;
    else if (yllimit >> 2 >= 0 && yllimit>>2 < 1023)
        span[(yllimit >> 2) + 1].validline = 0;
    yhlimit = (yh >= __clip.yh) ? yh : __clip.yh;

    allover = 1;
    allunder = 1;
    curover = 0;
    curunder = 0;
    allinval = 1;
    for (k = ycur; k <= ylfar; k++)
    {
        static int maxxmx, minxhx;
        int xrsc, xlsc, stickybit;
        const int32_t xleft = xl & ~0x00000001, xright = xh & ~0x00000001;
        const int yhclose = yhlimit & ~3;
        const int spix = k & 3;

        if (k < yhclose)
            continue;
        invaly = (uint32_t)(k - yhlimit)>>31 | (uint32_t)~(k - yllimit)>>31;
        j = k >> 2;
        if (spix == 0)
        {
            maxxmx = 0x000;
            minxhx = 0xFFF;
            allover = allunder = 1;
            allinval = 1;
        }

        stickybit = (xright & 0x00003FFF) - 1; /* xright/2 & 0x1FFF */
        stickybit = (uint32_t)~(stickybit) >> 31; /* (stickybit >= 0) */
        xrsc = (xright >> 13)&0x1FFE | stickybit;
        curunder = !!(xright & 0x08000000);
        curunder = curunder | (uint32_t)(xrsc - clipxhshift)>>31;
        xrsc = curunder ? clipxhshift : (xright>>13)&0x3FFE | stickybit;
        curover  = !!(xrsc & 0x00002000);
        xrsc = xrsc & 0x1FFF;
        curover |= (uint32_t)~(xrsc - clipxlshift) >> 31;
        xrsc = curover ? clipxlshift : xrsc;
        span[j].majorx[spix] = xrsc & 0x1FFF;
        allover &= curover;
        allunder &= curunder;

        stickybit = (xleft & 0x00003FFF) - 1; /* xleft/2 & 0x1FFF */
        stickybit = (uint32_t)~(stickybit) >> 31; /* (stickybit >= 0) */
        xlsc = (xleft >> 13)&0x1FFE | stickybit;
        curunder = !!(xleft & 0x08000000);
        curunder = curunder | (uint32_t)(xlsc - clipxhshift)>>31;
        xlsc = curunder ? clipxhshift : (xleft>>13)&0x3FFE | stickybit;
        curover  = !!(xlsc & 0x00002000);
        xlsc &= 0x1FFF;
        curover |= (uint32_t)~(xlsc - clipxlshift) >> 31;
        xlsc = curover ? clipxlshift : xlsc;
        span[j].minorx[spix] = xlsc & 0x1FFF;
        allover &= curover;
        allunder &= curunder;

        curcross = ((xleft&0x0FFFC000 ^ 0x08000000)
                 < (xright&0x0FFFC000 ^ 0x08000000));
        invaly |= curcross;
        span[j].invalyscan[spix] = invaly;
        allinval &= invaly;
        if (invaly != 0)
            { /* branch */ }
        else
        {
            xlsc = (xlsc >> 3) & 0xFFF;
            xrsc = (xrsc >> 3) & 0xFFF;
            maxxmx = (xlsc > maxxmx) ? xlsc : maxxmx;
            minxhx = (xrsc < minxhx) ? xrsc : minxhx;
        }

        if (spix == 0)
        {
            span[j].unscrx = xright >> 16;
            setzero_si128(span[j].rgba);
            setzero_si128(span[j].stwz);
        }
        else if (spix == 3)
        {
            const int invalidline = (sckeepodd ^ j) & scfield
                                  | (allinval | allover | allunder);
            span[j].lx = maxxmx;
            span[j].rx = minxhx;
            span[j].validline = invalidline ^ 1;
        }
    }
    render_spans(yhlimit >> 2, yllimit >> 2, 0, 1);
}

static void set_fill_color(uint32_t w1, uint32_t w2)
{
    fill_color = w2;
}

static void set_fog_color(uint32_t w1, uint32_t w2)
{
    COLOR_RED(fog_color)   = (w2 & 0xFF000000) >> 24;
    COLOR_GREEN(fog_color) = (w2 & 0x00FF0000) >> 16;
    COLOR_BLUE(fog_color)  = (w2 & 0x0000FF00) >>  8;
    COLOR_ALPHA(fog_color) = (w2 & 0x000000FF) >>  0;
}

static void set_blend_color(uint32_t w1, uint32_t w2)
{
    COLOR_RED(blend_color)   = (w2 & 0xFF000000) >> 24;
    COLOR_GREEN(blend_color) = (w2 & 0x00FF0000) >> 16;
    COLOR_BLUE(blend_color)  = (w2 & 0x0000FF00) >>  8;
    COLOR_ALPHA(blend_color) = (w2 & 0x000000FF) >>  0;
}

static void set_prim_color(uint32_t w1, uint32_t w2)
{
    min_level               = (w1 & 0x00001F00) >>(40-32);
    primitive_lod_frac      = (w1 & 0x000000FF) >>(32-32);
    COLOR_RED(prim_color)   = (w2 & 0xFF000000) >> 24;
    COLOR_GREEN(prim_color) = (w2 & 0x00FF0000) >> 16;
    COLOR_BLUE(prim_color)  = (w2 & 0x0000FF00) >>  8;
    COLOR_ALPHA(prim_color) = (w2 & 0x000000FF) >>  0;
}

static void set_env_color(uint32_t w1, uint32_t w2)
{
    COLOR_RED(env_color)   = (w2 & 0xFF000000) >> 24;
    COLOR_GREEN(env_color) = (w2 & 0x00FF0000) >> 16;
    COLOR_BLUE(env_color)  = (w2 & 0x0000FF00) >>  8;
    COLOR_ALPHA(env_color) = (w2 & 0x000000FF) >>  0;
}

static void set_combine(uint32_t w1, uint32_t w2)
{
    combine.sub_a_rgb0 = (w1 & 0x00F00000) >>(52-32);
    combine.mul_rgb0   = (w1 & 0x000F8000) >>(47-32);
    combine.sub_a_a0   = (w1 & 0x00007000) >>(44-32);
    combine.mul_a0     = (w1 & 0x00000E00) >>(41-32);
    combine.sub_a_rgb1 = (w1 & 0x000001E0) >>(37-32);
    combine.mul_rgb1   = (w1 & 0x0000001F) >>(32-32);
    combine.sub_b_rgb0 = (w2 & 0xF0000000) >> 28;
    combine.sub_b_rgb1 = (w2 & 0x0F000000) >> 24;
    combine.sub_a_a1   = (w2 & 0x00E00000) >> 21;
    combine.mul_a1     = (w2 & 0x001C0000) >> 18;
    combine.add_rgb0   = (w2 & 0x00038000) >> 15;
    combine.sub_b_a0   = (w2 & 0x00007000) >> 12;
    combine.add_a0     = (w2 & 0x00000E00) >>  9;
    combine.add_rgb1   = (w2 & 0x000001C0) >>  6;
    combine.sub_b_a1   = (w2 & 0x00000038) >>  3;
    combine.add_a1     = (w2 & 0x00000007) >>  0;

    SET_SUBA_RGB_INPUT(
        &combiner_rgbsub_a_r[0], &combiner_rgbsub_a_g[0],
        &combiner_rgbsub_a_b[0], combine.sub_a_rgb0);
    SET_SUBB_RGB_INPUT(
        &combiner_rgbsub_b_r[0], &combiner_rgbsub_b_g[0],
        &combiner_rgbsub_b_b[0], combine.sub_b_rgb0);
    SET_MUL_RGB_INPUT(
        &combiner_rgbmul_r[0], &combiner_rgbmul_g[0], &combiner_rgbmul_b[0],
        combine.mul_rgb0);
    SET_ADD_RGB_INPUT(
        &combiner_rgbadd_r[0], &combiner_rgbadd_g[0], &combiner_rgbadd_b[0],
        combine.add_rgb0);
    SET_SUB_ALPHA_INPUT(&combiner_alphasub_a[0], combine.sub_a_a0);
    SET_SUB_ALPHA_INPUT(&combiner_alphasub_b[0], combine.sub_b_a0);
    SET_MUL_ALPHA_INPUT(&combiner_alphamul[0], combine.mul_a0);
    SET_SUB_ALPHA_INPUT(&combiner_alphaadd[0], combine.add_a0);

    SET_SUBA_RGB_INPUT(
        &combiner_rgbsub_a_r[1], &combiner_rgbsub_a_g[1],
        &combiner_rgbsub_a_b[1], combine.sub_a_rgb1);
    SET_SUBB_RGB_INPUT(
        &combiner_rgbsub_b_r[1], &combiner_rgbsub_b_g[1],
        &combiner_rgbsub_b_b[1], combine.sub_b_rgb1);
    SET_MUL_RGB_INPUT(
        &combiner_rgbmul_r[1], &combiner_rgbmul_g[1], &combiner_rgbmul_b[1],
        combine.mul_rgb1);
    SET_ADD_RGB_INPUT(
        &combiner_rgbadd_r[1], &combiner_rgbadd_g[1], &combiner_rgbadd_b[1],
        combine.add_rgb1);
    SET_SUB_ALPHA_INPUT(&combiner_alphasub_a[1], combine.sub_a_a1);
    SET_SUB_ALPHA_INPUT(&combiner_alphasub_b[1], combine.sub_b_a1);
    SET_MUL_ALPHA_INPUT(&combiner_alphamul[1], combine.mul_a1);
    SET_SUB_ALPHA_INPUT(&combiner_alphaadd[1], combine.add_a1);

    other_modes.f.stalederivs = 1;
}

static void set_texture_image(uint32_t w1, uint32_t w2)
{
    ti_format  = (w1 & 0x00E00000) >> (53 - 32);
    ti_size    = (w1 & 0x00180000) >> (51 - 32);
    ti_width   = (w1 & 0x000003FF) >> (32 - 32);
    ti_address = (w2 & 0x03FFFFFF) >> ( 0 -  0);
 /* ti_address &= 0x00FFFFFF; // physical memory limit, enforced later */
    ++ti_width;
}

static void set_mask_image(uint32_t w1, uint32_t w2)
{
    zb_address = w2 & 0x03FFFFFF;
 /* zb_address &= 0x00FFFFFF; */
}

static void set_color_image(uint32_t w1, uint32_t w2)
{
    fb_format  = (w1 & 0x00E00000) >> (53 - 32);
    fb_size    = (w1 & 0x00180000) >> (51 - 32);
    fb_width   = (w1 & 0x000003FF) >> (32 - 32);
    fb_address = (w2 & 0x03FFFFFF) >> ( 0 -  0);
    ++fb_width;
 /* fb_address &= 0x00FFFFFF; */
}


