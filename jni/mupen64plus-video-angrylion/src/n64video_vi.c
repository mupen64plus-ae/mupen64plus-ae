#include <string.h>

#include "z64.h"
#include "rdp.h"
#include "vi.h"
#include "api/libretro.h"

typedef struct {
    uint8_t r, g, b, cvg;
} CCVG;

typedef struct {
    uint8_t cvg;
    uint8_t cvbit;
    uint8_t xoff;
    uint8_t yoff;
} CVtcmaskDERIVATIVE;

static CVtcmaskDERIVATIVE cvarray[0x100];

#define VI_ANDER(x) {                                 \
    PAIRREAD16(pix, hidval, x);                       \
    if (hidval == 3 && (pix & 1)) {                   \
        backr[numoffull] = GET_HI(pix);               \
        backg[numoffull] = GET_MED(pix);              \
        backb[numoffull] = GET_LOW(pix);              \
       numoffull++;                                      \
    }                                                 \
}
#define VI_ANDER32(x) {                               \
   RREADIDX32(pix, (x));                              \
    pixcvg = (pix >> 5) & 7;                          \
    if (pixcvg == 7) {                                \
        backr[numoffull] = (pix >> 24) & 0xFF;        \
        backg[numoffull] = (pix >> 16) & 0xFF;        \
        backb[numoffull] = (pix >>  8) & 0xFF;        \
       numoffull++;                                      \
    }                                           \
}
#define VI_COMPARE(x) {                      \
   addr = (x);                               \
   RREADIDX16(pix, addr);                    \
   tempr = (pix >> 11) & 0x1f;										\
	tempg = (pix >> 6) & 0x1f;										\
	tempb = (pix >> 1) & 0x1f;										\
	rend += redptr[tempr];											\
	gend += greenptr[tempg];										\
	bend += blueptr[tempb];                               \
}

#define VI_COMPARE_OPT(x)											\
{																	\
	addr = (x);														\
	pix = rdram_16[addr ^ WORD_ADDR_XOR];							\
	tempr = (pix >> 11) & 0x1f;										\
	tempg = (pix >> 6) & 0x1f;										\
	tempb = (pix >> 1) & 0x1f;										\
	rend += redptr[tempr];											\
	gend += greenptr[tempg];										\
	bend += blueptr[tempb];											\
}

#define VI_COMPARE32(x) {                    \
   addr = (x);                               \
   RREADIDX32(pix, addr);                    \
   tempr = (pix >> 27) & 0x1f;											\
	tempg = (pix >> 19) & 0x1f;											\
	tempb = (pix >> 11) & 0x1f;											\
	rend += redptr[tempr];												\
	gend += greenptr[tempg];											\
	bend += blueptr[tempb];                                  \
}

#define VI_COMPARE32_OPT(x)													\
{																		\
	addr = (x);															\
	pix = rdram[addr];												\
	tempr = (pix >> 27) & 0x1f;											\
	tempg = (pix >> 19) & 0x1f;											\
	tempb = (pix >> 11) & 0x1f;											\
	rend += redptr[tempr];												\
	gend += greenptr[tempg];											\
	bend += blueptr[tempb];												\
}

extern retro_log_printf_t log_cb;
extern retro_environment_t environ_cb;

onetime onetimewarnings;

uint8_t* rdram_8;
uint16_t* rdram_16;
uint32_t plim;
uint32_t idxlim16;
uint32_t idxlim32;
uint8_t hidden_bits[0x400000];

uint32_t gamma_table[0x100];
uint32_t gamma_dither_table[0x4000];
int32_t vi_restore_table[0x400];
int32_t oldvstart = 1337;

int overlay = 0;

extern uint32_t *blitter_buf_lock;

static uint32_t tvfadeoutstate[625];
static uint32_t brightness = 0;
static uint32_t prevwasblank = 0;

STRICTINLINE static void video_filter16(
    int* r, int* g, int* b, uint32_t fboffset, uint32_t num, uint32_t hres,
    uint32_t centercvg);
STRICTINLINE static void video_filter32(
    int* endr, int* endg, int* endb, uint32_t fboffset, uint32_t num, uint32_t hres,
    uint32_t centercvg);
STRICTINLINE static void divot_filter(
    CCVG* final, CCVG centercolor, CCVG leftcolor, CCVG rightcolor);
STRICTINLINE static void restore_filter16(
    int* r, int* g, int* b, uint32_t fboffset, uint32_t num, uint32_t hres);
STRICTINLINE static void restore_filter32(
    int* r, int* g, int* b, uint32_t fboffset, uint32_t num, uint32_t hres);
static void gamma_filters(unsigned char* argb, int gamma_and_dither);
static void adjust_brightness(unsigned char* argb, int brightcoeff);
STRICTINLINE static void vi_vl_lerp(CCVG* up, CCVG down, uint32_t frac);
STRICTINLINE static void video_max_optimized(uint32_t* Pixels, uint32_t* penumin, uint32_t* penumax, int numofels);

STRICTINLINE static void vi_fetch_filter16(
    CCVG* res, uint32_t fboffset, uint32_t cur_x, uint32_t fsaa, uint32_t dither_filter);
STRICTINLINE static void vi_fetch_filter32(
    CCVG* res, uint32_t fboffset, uint32_t cur_x, uint32_t fsaa, uint32_t dither_filter);

static void do_frame_buffer_proper(
    uint32_t prescale_ptr, int hres, int vres, int x_start, int vitype,
    int linecount);
static void do_frame_buffer_raw(
    uint32_t prescale_ptr, int hres, int vres, int x_start, int vitype,
    int linecount);
static void (*do_frame_buffer[2])(uint32_t, int, int, int, int, int) = {
    do_frame_buffer_raw, do_frame_buffer_proper
};

static void (*vi_fetch_filter_ptr)(
    CCVG*, uint32_t, uint32_t, uint32_t, uint32_t);
static void (*vi_fetch_filter_func[2])(
    CCVG*, uint32_t, uint32_t, uint32_t, uint32_t) = {
    vi_fetch_filter16, vi_fetch_filter32
};

static STRICTINLINE uint16_t decompress_cvmask_frombyte(uint8_t x)
{
    uint16_t y = (x & 1) | ((x & 2) << 4) | (x & 4) | ((x & 8) << 4) |
        ((x & 0x10) << 4) | ((x & 0x20) << 8) | ((x & 0x40) << 4) | ((x & 0x80) << 8);
    return y;
}

void precalc_cvmask_derivatives(void)
{
    int i = 0, k = 0;
    uint16_t mask = 0, maskx = 0, masky = 0;
    uint8_t offx = 0, offy = 0;
    static const uint8_t yarray[16] = {0, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0};
    static const uint8_t xarray[16] = {0, 3, 2, 2, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0};

    
    for (; i < 0x100; i++)
    {
        mask = decompress_cvmask_frombyte(i);
        cvarray[i].cvg = cvarray[i].cvbit = 0;
        cvarray[i].cvbit = (i >> 7) & 1;
        for (k = 0; k < 8; k++)
            cvarray[i].cvg += ((i >> k) & 1);

        
        masky = maskx = offx = offy = 0;
        for (k = 0; k < 4; k++)
            masky |= ((mask & (0xf000 >> (k << 2))) > 0) << k;

        offy = yarray[masky];
        
        maskx = (mask & (0xf000 >> (offy << 2))) >> ((offy ^ 3) << 2);
        
        
        offx = xarray[maskx];
        
        cvarray[i].xoff = offx;
        cvarray[i].yoff = offy;
    }
}

void lookup_cvmask_derivatives(uint32_t mask, uint8_t* offx, uint8_t* offy, uint32_t* curpixel_cvg, uint32_t* curpixel_cvbit)
{
    CVtcmaskDERIVATIVE temp = cvarray[mask];
    *curpixel_cvg = temp.cvg;
    *curpixel_cvbit = temp.cvbit;
    *offx = temp.xoff;
    *offy = temp.yoff;
}

void rdp_update(void)
{
    uint32_t prescale_ptr;
    uint32_t pix;
    uint8_t cur_cvg;
    int hres, vres;
    int h_start, v_start;
    int x_start;
    int h_end;
    int two_lines, line_shifter, line_count;
    int hrightblank;
    int vactivelines;
    int validh;
    int serration_pulses;
    int validinterlace;
    int lowerfield;
    register int i, j;
    extern uint32_t *blitter_buf_lock;
    struct retro_framebuffer fb = {0};
    const int x_add = *GET_GFX_INFO(VI_X_SCALE_REG) & 0x00000FFF;
    const int v_sync = *GET_GFX_INFO(VI_V_SYNC_REG) & 0x000003FF;
    const int ispal  = (v_sync > 550);
    const int x1 = (*GET_GFX_INFO(VI_H_START_REG) >> 16) & 0x03FF;
    const int y1 = (*GET_GFX_INFO(VI_V_START_REG) >> 16) & 0x03FF;
    const int x2 = (*GET_GFX_INFO(VI_H_START_REG) >>  0) & 0x03FF;
    const int y2 = (*GET_GFX_INFO(VI_V_START_REG) >>  0) & 0x03FF;
    const int delta_x = x2 - x1;
    const int delta_y = y2 - y1;
    const int vitype = *GET_GFX_INFO(VI_STATUS_REG) & 0x00000003;
    const int pixel_size = sizeof(int32_t);

#if 0
    fb.width        = PRESCALE_WIDTH;
    fb.height       = PRESCALE_HEIGHT;
    fb.access_flags = RETRO_MEMORY_ACCESS_WRITE;

    if (environ_cb(RETRO_ENVIRONMENT_GET_CURRENT_SOFTWARE_FRAMEBUFFER, &fb)
          && fb.format == RETRO_PIXEL_FORMAT_XRGB8888)
       blitter_buf_lock = (uint32_t*)fb.data;
#endif

/*
 * initial value (angrylion)
 */
    serration_pulses  = !!(*GET_GFX_INFO(VI_STATUS_REG) & 0x00000040);
    serration_pulses &= (y1 != oldvstart);
    two_lines = serration_pulses ^ 0;

    validinterlace = (vitype & 2) && serration_pulses;
    if (!validinterlace)
       internal_vi_v_current_line = 0;
    lowerfield = validinterlace && !(internal_vi_v_current_line & 1);
    if (validinterlace)
       internal_vi_v_current_line ^= 1;

    line_count = pitchindwords << serration_pulses;
    line_shifter = serration_pulses ^ 1;

    hres = delta_x;
    vres = delta_y;
    h_start = x1 - (ispal ? 128 : 108);
    v_start = y1 - (ispal ?  47 :  37);
    x_start = (*gfx_info.VI_X_SCALE_REG >> 16) & 0x00000FFF;

    if (h_start < 0)
    {
        x_start -= x_add * h_start;
        h_start  = 0;
    }
    oldvstart = y1;
    v_start >>= 1;
    v_start  &= -(v_start >= 0);
    vres >>= 1;

    if (hres > PRESCALE_WIDTH - h_start)
        hres = PRESCALE_WIDTH - h_start;
    if (vres > PRESCALE_HEIGHT - v_start)
        vres = PRESCALE_HEIGHT - v_start;
    h_end = hres + h_start;

    hrightblank = PRESCALE_WIDTH - h_end;
    vactivelines = v_sync - (ispal ? 47 : 37);
    if (vactivelines > PRESCALE_HEIGHT)
    {
       if (log_cb)
          log_cb(RETRO_LOG_WARN, "VI_V_SYNC_REG too big\n");
        return;
    }
    if (vactivelines < 0)
    {
       if (log_cb)
          log_cb(RETRO_LOG_WARN, "vactivelines lesser than 0\n");
        return;
    }
    vactivelines >>= line_shifter;
    validh = (hres >= 0 && h_start >= 0 && h_start < PRESCALE_WIDTH);
    pix = 0;
    cur_cvg = 0;
    if (hres <= 0 || vres <= 0 || (!(vitype & 2) && prevwasblank)) /* early return. */
        return;

    if (vitype >> 1 == 0)
    {
        memset(tvfadeoutstate, 0, pixel_size*PRESCALE_HEIGHT);
        for (i = 0; i < PRESCALE_HEIGHT; i++)
            memset(&blitter_buf_lock[i * pitchindwords], 0, pixel_size*PRESCALE_WIDTH);
        prevwasblank = 1;
        goto no_frame_buffer;
    }
#undef RENDER_CVG_BITS16
#undef RENDER_CVG_BITS32
#undef RENDER_MIN_CVG_ONLY
#undef RENDER_MAX_CVG_ONLY

#undef MONITOR_Z
#undef BW_ZBUFFER
#undef ZBUFF_AS_16B_IATEXTURE

#ifdef MONITOR_Z
    frame_buffer = zb_address;
#endif

    prevwasblank = 0;
    if (h_start > 0 && h_start < PRESCALE_WIDTH)
        for (i = 0; i < vactivelines; i++)
            memset(&blitter_buf_lock[i*pitchindwords], 0, pixel_size*h_start);

    if (h_end >= 0 && h_end < PRESCALE_WIDTH)
        for (i = 0; i < vactivelines; i++)
            memset(&blitter_buf_lock[i*pitchindwords + h_end], 0, pixel_size*hrightblank);

    for (i = 0; i < (v_start << two_lines) + lowerfield; i++)
    {
        tvfadeoutstate[i] >>= 1;
        if (~tvfadeoutstate[i] & validh)
            memset(&blitter_buf_lock[i*pitchindwords + h_start], 0, pixel_size*hres);
    }

    if (serration_pulses == 0)
        for (j = 0; j < vres; j++)
            tvfadeoutstate[i++] = 2;
    else
        for (j = 0; j < vres; j++)
        {
            tvfadeoutstate[i] = 2;
            ++i;
            tvfadeoutstate[i] >>= 1;
            if (~tvfadeoutstate[i] & validh)
                memset(&blitter_buf_lock[i*pitchindwords + h_start], 0, pixel_size*hres);
            ++i;
        }

    while (i < vactivelines)
    {
        tvfadeoutstate[i] >>= 1;
        if (~tvfadeoutstate[i] & validh)
            memset(&blitter_buf_lock[i*pitchindwords + h_start], 0, pixel_size*hres);
        ++i;
    }

    prescale_ptr =
        (v_start * line_count) + h_start + (lowerfield ? pitchindwords : 0);
    do_frame_buffer[overlay](
        prescale_ptr, hres, vres, x_start, vitype, line_count);
no_frame_buffer:

    __src.bottom = (ispal ? 576 : 480) >> line_shifter; /* visible lines */

    if (line_shifter != 0) /* 240p non-interlaced VI DAC mode */
    {
        register signed int cur_line;

        cur_line = 240 - 1;
        while (cur_line >= 0)
        {
            memcpy(
                &blitter_buf_lock[2*PRESCALE_WIDTH*cur_line + PRESCALE_WIDTH],
                &blitter_buf_lock[1*PRESCALE_WIDTH*cur_line],
                4 * PRESCALE_WIDTH
            );
            memcpy(
                &blitter_buf_lock[2*PRESCALE_WIDTH*cur_line + 0],
                &blitter_buf_lock[1*PRESCALE_WIDTH*cur_line],
                4 * PRESCALE_WIDTH
            );
            --cur_line;
        }
    }
}

static void do_frame_buffer_proper(
    uint32_t prescale_ptr, int hres, int vres, int x_start, int vitype,
    int linecount)
{
    CCVG viaa_array[2048];
    CCVG divot_array[2048];
    CCVG *viaa_cache, *viaa_cache_next, *divot_cache, *divot_cache_next;
    CCVG *tempccvgptr;
    CCVG color, nextcolor, scancolor, scannextcolor;
    uint32_t * scanline;
    uint32_t pixels = 0, nextpixels = 0;
    uint32_t prevy = 0;
    uint32_t y_start = (vi_y_scale >> 16) & 0x0FFF;
	uint32_t frame_buffer = vi_origin & 0x00FFFFFF;
    signed int cache_marker_init;
    int line_x = 0, next_line_x = 0, prev_line_x = 0, far_line_x = 0;
    int prev_scan_x = 0, scan_x = 0, next_scan_x = 0, far_scan_x = 0;
    int prev_x = 0, cur_x = 0, next_x = 0, far_x = 0;
    int cache_marker = 0, cache_next_marker = 0, divot_cache_marker = 0, divot_cache_next_marker = 0;
    int xfrac = 0, yfrac = 0;
    int slowbright;
    int lerping = 0;
    int vi_width_low = vi_width & 0xFFF;
    const int x_add = *GET_GFX_INFO(VI_X_SCALE_REG) & 0x00000FFF;
    uint32_t y_add = vi_y_scale & 0xfff;
    register int i, j;
    const int gamma_dither     = !!(*GET_GFX_INFO(VI_STATUS_REG) & 0x00000004);
    const int gamma            = !!(*GET_GFX_INFO(VI_STATUS_REG) & 0x00000008);
    const int divot            = !!(*GET_GFX_INFO(VI_STATUS_REG) & 0x00000010);
    const int clock_enable     = !!(*GET_GFX_INFO(VI_STATUS_REG) & 0x00000020);
    const int extralines       =  !(*GET_GFX_INFO(VI_STATUS_REG) & 0x00000100);
    const int fsaa             =  !(*GET_GFX_INFO(VI_STATUS_REG) & 0x00000200);
    const int dither_filter    = !!(*GET_GFX_INFO(VI_STATUS_REG) & 0x00010000);
    const int gamma_and_dither = (gamma << 1) | gamma_dither;
    const int lerp_en          = fsaa | extralines;

    if (frame_buffer == 0)
        return;

    if (clock_enable)
        DisplayError(
            "rdp_update: vbus_clock_enable bit set in VI_CONTROL_REG "\
            "register. Never run this code on your N64! It's rumored that "\
            "turning this bit on will result in permanent damage to the "\
            "hardware! Emulation will now continue.");

    viaa_cache = &viaa_array[0];
    viaa_cache_next = &viaa_array[1024];
    divot_cache = &divot_array[0];
    divot_cache_next = &divot_array[1024];

    cache_marker_init  = (x_start >> 10) - 2;
    cache_marker_init |= -(cache_marker_init < 0);

    slowbright = 0;
#if 0
    if (GetAsyncKeyState(0x91))
        brightness = ++brightness & 0xF;
    slowbright = brightness >> 1;
#endif
    pixels = 0;

    for (j = 0; j < vres; j++)
    {
        x_start = (vi_x_scale >> 16) & 0x0FFF;

        if ((y_start >> 10) == (prevy + 1) && j)
        {
            cache_marker = cache_next_marker;
            cache_next_marker = cache_marker_init;

            tempccvgptr = viaa_cache;
            viaa_cache = viaa_cache_next;
            viaa_cache_next = tempccvgptr;
            if (divot == 0)
                {/* do nothing and branch */}
            else
            {
                divot_cache_marker = divot_cache_next_marker;
                divot_cache_next_marker = cache_marker_init;
                tempccvgptr = divot_cache;
                divot_cache = divot_cache_next;
                divot_cache_next = tempccvgptr;
            }
        }
        else if ((y_start >> 10) != prevy || !j)
        {
            cache_marker = cache_next_marker = cache_marker_init;
            if (divot == 0)
                {/* do nothing and branch */}
            else
                divot_cache_marker
              = divot_cache_next_marker
              = cache_marker_init;
        }

        scanline = &blitter_buf_lock[prescale_ptr];
        prescale_ptr += linecount;

        prevy = y_start >> 10;
        yfrac = (y_start >> 5) & 0x1f;
        pixels = vi_width_low * prevy;
        nextpixels = pixels + vi_width_low;

        for (i = 0; i < hres; i++)
        {
            unsigned char argb[4];

            line_x = x_start >> 10;
            prev_line_x = line_x - 1;
            next_line_x = line_x + 1;
            far_line_x = line_x + 2;

            cur_x = pixels + line_x;
            prev_x = pixels + prev_line_x;
            next_x = pixels + next_line_x;
            far_x = pixels + far_line_x;

            scan_x = nextpixels + line_x;
            prev_scan_x = nextpixels + prev_line_x;
            next_scan_x = nextpixels + next_line_x;
            far_scan_x = nextpixels + far_line_x;

            xfrac = (x_start >> 5) & 0x1f;
            lerping = lerp_en & (xfrac || yfrac);

            if (prev_line_x > cache_marker)
            {
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache[prev_line_x], frame_buffer, prev_x, fsaa,
                    dither_filter);
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache[line_x], frame_buffer, cur_x, fsaa,
                    dither_filter);
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache[next_line_x], frame_buffer, next_x, fsaa,
                    dither_filter);
                cache_marker = next_line_x;
            }
            else if (line_x > cache_marker)
            {
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache[line_x], frame_buffer, cur_x, fsaa,
                    dither_filter);
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache[next_line_x], frame_buffer, next_x, fsaa,
                    dither_filter);
                cache_marker = next_line_x;
            }
            else if (next_line_x > cache_marker)
            {
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache[next_line_x], frame_buffer, next_x, fsaa,
                    dither_filter);
                cache_marker = next_line_x;
            }

            if (prev_line_x > cache_next_marker)
            {
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache_next[prev_line_x], frame_buffer, prev_scan_x,
                    fsaa, dither_filter);
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache_next[line_x], frame_buffer, scan_x, fsaa,
                    dither_filter);
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache_next[next_line_x], frame_buffer, next_scan_x,
                    fsaa, dither_filter);
                cache_next_marker = next_line_x;
            }
            else if (line_x > cache_next_marker)
            {
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache_next[line_x], frame_buffer, scan_x, fsaa,
                    dither_filter);
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache_next[next_line_x], frame_buffer, next_scan_x,
                    fsaa, dither_filter);
                cache_next_marker = next_line_x;
            }
            else if (next_line_x > cache_next_marker)
            {
                vi_fetch_filter_func[vitype & 1](
                    &viaa_cache_next[next_line_x], frame_buffer, next_scan_x,
                    fsaa, dither_filter);
                cache_next_marker = next_line_x;
            }

            if (divot == 0)
                color = viaa_cache[line_x];
            else
            {
                if (far_line_x > cache_marker)
                {
                    vi_fetch_filter_func[vitype & 1](
                        &viaa_cache[far_line_x], frame_buffer, far_x, fsaa,
                        dither_filter);
                    cache_marker = far_line_x;
                }

                if (far_line_x > cache_next_marker)
                {
                    vi_fetch_filter_func[vitype & 1](
                        &viaa_cache_next[far_line_x], frame_buffer, far_scan_x,
                        fsaa, dither_filter);
                    cache_next_marker = far_line_x;
                }

                if (line_x > divot_cache_marker)
                {
                    divot_filter(
                        &divot_cache[line_x], viaa_cache[line_x],
                        viaa_cache[prev_line_x], viaa_cache[next_line_x]);
                    divot_filter(
                        &divot_cache[next_line_x], viaa_cache[next_line_x],
                        viaa_cache[line_x], viaa_cache[far_line_x]);
                    divot_cache_marker = next_line_x;
                }
                else if (next_line_x > divot_cache_marker)
                {
                    divot_filter(
                        &divot_cache[next_line_x], viaa_cache[next_line_x],
                        viaa_cache[line_x], viaa_cache[far_line_x]);
                    divot_cache_marker = next_line_x;
                }

                if (line_x > divot_cache_next_marker)
                {
                    divot_filter(
                        &divot_cache_next[line_x], viaa_cache_next[line_x],
                        viaa_cache_next[prev_line_x],
                        viaa_cache_next[next_line_x]);
                    divot_filter(
                        &divot_cache_next[next_line_x],
                        viaa_cache_next[next_line_x], viaa_cache_next[line_x],
                        viaa_cache_next[far_line_x]);
                    divot_cache_next_marker = next_line_x;
                }
                else if (next_line_x > divot_cache_next_marker)
                {
                    divot_filter(
                        &divot_cache_next[next_line_x],
                        viaa_cache_next[next_line_x], viaa_cache_next[line_x],
                        viaa_cache_next[far_line_x]);
                    divot_cache_next_marker = next_line_x;
                }
                color = divot_cache[line_x];
            }

            if (lerping)
            {
                if (divot == 0)
                { /* branch unlikely */
                    nextcolor = viaa_cache[next_line_x];
                    scancolor = viaa_cache_next[line_x];
                    scannextcolor = viaa_cache_next[next_line_x];
                }
                else
                {
                    nextcolor = divot_cache[next_line_x];
                    scancolor = divot_cache_next[line_x];
                    scannextcolor = divot_cache_next[next_line_x];
                }
                if (yfrac == 0)
                    {}
                else
                {
                    vi_vl_lerp(&color, scancolor, yfrac);
                    vi_vl_lerp(&nextcolor, scannextcolor, yfrac);
                }
                if (xfrac == 0)
                    {}
                else
                    vi_vl_lerp(&color, nextcolor, xfrac);
            }
            argb[1 ^ BYTE_ADDR_XOR] = color.r;
            argb[2 ^ BYTE_ADDR_XOR] = color.g;
            argb[3 ^ BYTE_ADDR_XOR] = color.b;

            gamma_filters(argb, gamma_and_dither);
#ifdef BW_ZBUFFER
            uint32_t tempz = RREADIDX16((frame_buffer >> 1) + cur_x);

            pix = tempz;
            argb[1 ^ 3] = argb[2 ^ 3] = argb[3 ^ 3] = pix >> 8;
#endif
#ifdef ZBUFF_AS_16B_IATEXTURE
            argb[1 ^ 3] = argb[2 ^ 3] = argb[3 ^ 3] =
                (unsigned char)(pix >> 8)*(unsigned char)(pix >> 0) >> 8;
#endif
#ifdef RENDER_CVG_BITS16
            argb[1 ^ 3] = argb[2 ^ 3] = argb[3 ^ 3] = cur_cvg << 5;
#endif
#ifdef RENDER_CVG_BITS32
            argb[1 ^ 3] = argb[2 ^ 3] = argb[3 ^ 3] = cur_cvg << 5;
#endif
#ifdef RENDER_MIN_CVG_ONLY
            if (!cur_cvg)
                argb[1 ^ 3] = argb[2 ^ 3] = argb[3 ^ 3] = 0x00;
            else
                argb[1 ^ 3] = argb[2 ^ 3] = argb[3 ^ 3] = 0xFF;
#endif
#ifdef RENDER_MAX_CVG_ONLY
            if (cur_cvg != 7)
                argb[1 ^ 3] = argb[2 ^ 3] = argb[3 ^ 3] = 0x00;
            else
                argb[1 ^ 3] = argb[2 ^ 3] = argb[3 ^ 3] = 0xFF;
#endif
            x_start += x_add;
            scanline[i] = *(int32_t *)(argb);
            if (slowbright == 0)
                continue;
            adjust_brightness(argb, slowbright);
            scanline[i] = *(int32_t *)(argb);
        }
        y_start += y_add;
    }
}
static void do_frame_buffer_raw(
    uint32_t prescale_ptr, int hres, int vres, int x_start, int vitype,
    int linecount)
{
    uint32_t * scanline;
    int pixels;
    int prevy, y_start;
    int cur_x, line_x;
    register int i;
    const int frame_buffer = *GET_GFX_INFO(VI_ORIGIN_REG) & 0x00FFFFFF;
    const int VI_width = *GET_GFX_INFO(VI_WIDTH_REG) & 0x00000FFF;
    const int x_add = *GET_GFX_INFO(VI_X_SCALE_REG) & 0x00000FFF;
    const int y_add = *GET_GFX_INFO(VI_Y_SCALE_REG) & 0x00000FFF;

    if (frame_buffer == 0)
        return;
    y_start = *GET_GFX_INFO(VI_Y_SCALE_REG)>>16 & 0x0FFF;

    if (vitype & 1) /* 32-bit RGBA (branch unlikely) */
    {
        while (--vres >= 0)
        {
            x_start = *GET_GFX_INFO(VI_X_SCALE_REG)>>16 & 0x0FFF;
            scanline = &blitter_buf_lock[prescale_ptr];
            prescale_ptr += linecount;

            prevy = y_start >> 10;
            pixels = VI_width * prevy;

            for (i = 0; i < hres; i++)
            {
                unsigned long pix;
                unsigned long addr;
#ifdef MSB_FIRST
                unsigned char argb[4];
#endif

                line_x = x_start >> 10;
                cur_x = pixels + line_x;

                x_start += x_add;
                addr = frame_buffer + 4*cur_x;
                if (plim - addr < 0)
                    continue;
                pix = *(int32_t *)(DRAM + addr);
#ifdef MSB_FIRST
                argb[1 ^ BYTE_ADDR_XOR] = (unsigned char)(pix >> 24);
                argb[2 ^ BYTE_ADDR_XOR] = (unsigned char)(pix >> 16);
                argb[3 ^ BYTE_ADDR_XOR] = (unsigned char)(pix >>  8);
                argb[0 ^ BYTE_ADDR_XOR] = (unsigned char)(pix >>  0);
                scanline[i] = *(int32_t *)(argb);
#else
				scanline[i] = (pix >> 8) | (pix << 24);
#endif
            }
            y_start += y_add;
        }
    }
    else /* 16-bit RRRRR GGGGG BBBBB A */
    {
        while (--vres >= 0)
        {
            x_start = *GET_GFX_INFO(VI_X_SCALE_REG)>>16 & 0x0FFF;
            scanline = &blitter_buf_lock[prescale_ptr];
            prescale_ptr += linecount;

            prevy = y_start >> 10;
            pixels = VI_width * prevy;

            for (i = 0; i < hres; i++)
            {
                unsigned short pix;
                unsigned long addr;
#ifdef MSB_FIRST
                unsigned char argb[4];
#else
                uint32_t argb;
#endif

                line_x = x_start >> 10;
                cur_x = pixels + line_x;

                x_start += x_add;
                addr = frame_buffer + 2*cur_x;
                if (plim - addr < 0)
                    continue;
                addr = addr ^ (WORD_ADDR_XOR << 1);
                pix = *(int16_t *)(DRAM + addr);

#ifdef MSB_FIRST
                argb[1 ^ BYTE_ADDR_XOR] = (unsigned char)(pix >> 8);
                argb[2 ^ BYTE_ADDR_XOR] = (unsigned char)(pix >> 3) & ~7;
                argb[3 ^ BYTE_ADDR_XOR] = (unsigned char)(pix & ~1) << 2;
                scanline[i] = *(int32_t *)(argb);
#else
                argb = (pix << 8) & 0x00F80000;
                argb |= (pix << 5) & 0x0000F800;
                argb |= (pix & 0x3E) << 2;
                argb += 0xFF000000;
                scanline[i] = argb;
#endif
            }
            y_start += y_add;
        }
    }
}

STRICTINLINE static void vi_fetch_filter16(
    CCVG* res, uint32_t fboffset, uint32_t cur_x, uint32_t fsaa, uint32_t dither_filter)
{
    int r, g, b;
    uint32_t pix, hval;
    uint32_t cur_cvg;
    uint32_t idx = (fboffset >> 1) + cur_x;
    uint32_t fbw = vi_width & 0xfff;

    if (fsaa)
    {
       PAIRREAD16(pix, hval, idx); 
       cur_cvg = ((pix & 1) << 2) | hval;
    }
    else
    {
       RREADIDX16(pix, idx);
       cur_cvg = 7;
    }
    r = GET_HI(pix);
    g = GET_MED(pix);
    b = GET_LOW(pix);

    if (cur_cvg == 7)
    {
        if (dither_filter)
            restore_filter16(&r, &g, &b, fboffset, cur_x, fbw);
    }
    else
    {
        video_filter16(&r, &g, &b, fboffset, cur_x, fbw, cur_cvg);
    }

    res -> r = r;
    res -> g = g;
    res -> b = b;
    res -> cvg = cur_cvg;
}

STRICTINLINE static void vi_fetch_filter32(
    CCVG* res, uint32_t fboffset, uint32_t cur_x, uint32_t fsaa, uint32_t dither_filter)
{
    int r, g, b;
    uint32_t cur_cvg;
    uint32_t fbw = vi_width & 0xfff;
    uint32_t pix, addr = (fboffset >> 2) + cur_x;
    RREADIDX32(pix, addr);

    if (fsaa)
        cur_cvg = (pix >> 5) & 7;
    else
        cur_cvg = 7;

    r = (pix >> 24) & 0xff;
    g = (pix >> 16) & 0xff;
    b = (pix >> 8) & 0xff;

    if (cur_cvg == 7)
    {
        if (dither_filter)
            restore_filter32(&r, &g, &b, fboffset, cur_x, fbw);
    }
    else
    {
        video_filter32(&r, &g, &b, fboffset, cur_x, fbw, cur_cvg);
    }

    res -> r = r;
    res -> g = g;
    res -> b = b;
    res -> cvg = cur_cvg;
}

STRICTINLINE static void video_filter16(
    int* endr, int* endg, int* endb, uint32_t fboffset, uint32_t num, uint32_t hres,
    uint32_t centercvg)
{
    uint32_t penumaxr, penumaxg, penumaxb, penuminr, penuming, penuminb;
    uint16_t pix;
    uint32_t numoffull = 1;
    uint32_t hidval;
    uint32_t r, g, b; 
    uint32_t backr[7], backg[7], backb[7];
    uint32_t colr, colg, colb;

    uint32_t idx = (fboffset >> 1) + num;
    uint32_t leftup = idx - hres - 1;
    uint32_t rightup = idx - hres + 1;
    uint32_t toleft = idx - 2;
    uint32_t toright = idx + 2;
    uint32_t leftdown = idx + hres - 1;
    uint32_t rightdown = idx + hres + 1;
    uint32_t coeff = 7 - centercvg;

    r = *endr;
    g = *endg;
    b = *endb;

    backr[0] = r;
    backg[0] = g;
    backb[0] = b;

    VI_ANDER(leftup);
    VI_ANDER(rightup);
    VI_ANDER(toleft);
    VI_ANDER(toright);
    VI_ANDER(leftdown);
    VI_ANDER(rightdown);

    video_max_optimized(backr, &penuminr, &penumaxr, numoffull);
	video_max_optimized(backg, &penuming, &penumaxg, numoffull);
	video_max_optimized(backb, &penuminb, &penumaxb, numoffull);

    colr = penuminr + penumaxr - (r << 1);
    colg = penuming + penumaxg - (g << 1);
    colb = penuminb + penumaxb - (b << 1);

    colr = (((colr * coeff) + 4) >> 3) + r;
    colg = (((colg * coeff) + 4) >> 3) + g;
    colb = (((colb * coeff) + 4) >> 3) + b;

    *endr = colr & 0xFF;
    *endg = colg & 0xFF;
    *endb = colb & 0xFF;
}

STRICTINLINE static void video_filter32(
    int* endr, int* endg, int* endb, uint32_t fboffset, uint32_t num, uint32_t hres,
    uint32_t centercvg)
{
    uint32_t penumaxr, penumaxg, penumaxb, penuminr, penuming, penuminb;
    uint32_t numoffull = 1;
    uint32_t pix = 0, pixcvg = 0;
    uint32_t r, g, b; 
    uint32_t backr[7], backg[7], backb[7];
    uint32_t colr, colg, colb;

    uint32_t idx = (fboffset >> 2) + num;
    uint32_t leftup = idx - hres - 1;
    uint32_t rightup = idx - hres + 1;
    uint32_t toleft = idx - 2;
    uint32_t toright = idx + 2;
    uint32_t leftdown = idx + hres - 1;
    uint32_t rightdown = idx + hres + 1;
    uint32_t coeff = 7 - centercvg;

    r = *endr;
    g = *endg;
    b = *endb;

    backr[0] = r;
    backg[0] = g;
    backb[0] = b;

    VI_ANDER32(leftup);
    VI_ANDER32(rightup);
    VI_ANDER32(toleft);
    VI_ANDER32(toright);
    VI_ANDER32(leftdown);
    VI_ANDER32(rightdown);

    video_max_optimized(backr, &penuminr, &penumaxr, numoffull);
    video_max_optimized(backg, &penuming, &penumaxg, numoffull);
    video_max_optimized(backb, &penuminb, &penumaxb, numoffull);

    colr = penuminr + penumaxr - (r << 1);
    colg = penuming + penumaxg - (g << 1);
    colb = penuminb + penumaxb - (b << 1);

    colr = (((colr * coeff) + 4) >> 3) + r;
    colg = (((colg * coeff) + 4) >> 3) + g;
    colb = (((colb * coeff) + 4) >> 3) + b;

    *endr = colr & 0xFF;
    *endg = colg & 0xFF;
    *endb = colb & 0xFF;
}

STRICTINLINE static void divot_filter(
    CCVG* final, CCVG centercolor, CCVG leftcolor, CCVG rightcolor)
{
    uint32_t leftr, leftg, leftb;
    uint32_t rightr, rightg, rightb;
    uint32_t centerr, centerg, centerb;

    *final = centercolor;
    if ((centercolor.cvg & leftcolor.cvg & rightcolor.cvg) == 7)
        return;

    leftr = leftcolor.r;    
    leftg = leftcolor.g;    
    leftb = leftcolor.b;
    rightr = rightcolor.r;    
    rightg = rightcolor.g;    
    rightb = rightcolor.b;
    centerr = centercolor.r;
    centerg = centercolor.g;
    centerb = centercolor.b;

    if ((leftr >= centerr && rightr >= leftr) || (leftr >= rightr && centerr >= leftr))
        final -> r = leftr;
    else if ((rightr >= centerr && leftr >= rightr) || (rightr >= leftr && centerr >= rightr))
        final -> r = rightr;

    if ((leftg >= centerg && rightg >= leftg) || (leftg >= rightg && centerg >= leftg))
        final -> g = leftg;
    else if ((rightg >= centerg && leftg >= rightg) || (rightg >= leftg && centerg >= rightg))
        final -> g = rightg;

    if ((leftb >= centerb && rightb >= leftb) || (leftb >= rightb && centerb >= leftb))
        final -> b = leftb;
    else if ((rightb >= centerb && leftb >= rightb) || (rightb >= leftb && centerb >= rightb))
        final -> b = rightb;
}

STRICTINLINE static void restore_filter16(
    int* r, int* g, int* b, uint32_t fboffset, uint32_t num, uint32_t hres)
{
    uint32_t tempr, tempg, tempb;
    uint16_t pix;
    uint32_t addr;

    uint32_t idx = (fboffset >> 1) + num;
    uint32_t leftuppix = idx - hres - 1;
    uint32_t leftdownpix = idx + hres - 1;
    uint32_t toleftpix = idx - 1;
    uint32_t maxpix = idx + hres + 1;

    int32_t rend = *r;
	int32_t gend = *g;
	int32_t bend = *b;
	const int32_t* redptr = &vi_restore_table[(rend << 2) & 0x3e0];
	const int32_t* greenptr = &vi_restore_table[(gend << 2) & 0x3e0];
	const int32_t* blueptr = &vi_restore_table[(bend << 2) & 0x3e0];

    if (maxpix <= idxlim16 && leftuppix <= idxlim16)
	{
		VI_COMPARE_OPT(leftuppix);
		VI_COMPARE_OPT(leftuppix + 1);
		VI_COMPARE_OPT(leftuppix + 2);
		VI_COMPARE_OPT(leftdownpix);
		VI_COMPARE_OPT(leftdownpix + 1);
		VI_COMPARE_OPT(maxpix);
		VI_COMPARE_OPT(toleftpix);
		VI_COMPARE_OPT(toleftpix + 2);
	}
	else
	{
		VI_COMPARE(leftuppix);
		VI_COMPARE(leftuppix + 1);
		VI_COMPARE(leftuppix + 2);
		VI_COMPARE(leftdownpix);
		VI_COMPARE(leftdownpix + 1);
		VI_COMPARE(maxpix);
		VI_COMPARE(toleftpix);
		VI_COMPARE(toleftpix + 2);
	}

    *r = rend;
    *g = gend;
    *b = bend;
}

STRICTINLINE static void restore_filter32(
    int* r, int* g, int* b, uint32_t fboffset, uint32_t num, uint32_t hres)
{
    uint32_t tempr, tempg, tempb;
    uint32_t pix, addr;

    uint32_t idx = (fboffset >> 2) + num;
    uint32_t leftuppix = idx - hres - 1;
    uint32_t leftdownpix = idx + hres - 1;
    uint32_t toleftpix = idx - 1;
    uint32_t maxpix = idx + hres + 1;

    int32_t rend = *r;
    int32_t gend = *g;
    int32_t bend = *b;
    const int32_t* redptr = &vi_restore_table[(rend << 2) & 0x3e0];
    const int32_t* greenptr = &vi_restore_table[(gend << 2) & 0x3e0];
    const int32_t* blueptr = &vi_restore_table[(bend << 2) & 0x3e0];


    if (maxpix <= idxlim32 && leftuppix <= idxlim32)
	{
		VI_COMPARE32_OPT(leftuppix);
		VI_COMPARE32_OPT(leftuppix + 1);
		VI_COMPARE32_OPT(leftuppix + 2);
		VI_COMPARE32_OPT(leftdownpix);
		VI_COMPARE32_OPT(leftdownpix + 1);
		VI_COMPARE32_OPT(maxpix);
		VI_COMPARE32_OPT(toleftpix);
		VI_COMPARE32_OPT(toleftpix + 2);
	}
	else
	{
		VI_COMPARE32(leftuppix);
		VI_COMPARE32(leftuppix + 1);
		VI_COMPARE32(leftuppix + 2);
		VI_COMPARE32(leftdownpix);
		VI_COMPARE32(leftdownpix + 1);
		VI_COMPARE32(maxpix);
		VI_COMPARE32(toleftpix);
		VI_COMPARE32(toleftpix + 2);
	}


    *r = rend;
    *g = gend;
    *b = bend;
}

static void gamma_filters(unsigned char* argb, int gamma_and_dither)
{
    int cdith, dith;
    int r, g, b;

    if (gamma_and_dither == 0)
       return;

    r = argb[1 ^ BYTE_ADDR_XOR];
    g = argb[2 ^ BYTE_ADDR_XOR];
    b = argb[3 ^ BYTE_ADDR_XOR];

    switch(gamma_and_dither)
    {
        case 1:
            cdith = irand();
            dith = cdith & 1;
            if (r < 255)
                r += dith;
            dith = (cdith >> 1) & 1;
            if (g < 255)
                g += dith;
            dith = (cdith >> 2) & 1;
            if (b < 255)
                b += dith;
            break;
        case 2:
            r = gamma_table[r];
            g = gamma_table[g];
            b = gamma_table[b];
            break;
        case 3:
            cdith = irand();
            dith = cdith & 0x3f;
            r = gamma_dither_table[(r << 6) | dith];
            dith = (cdith >> 6) & 0x3f;
            g = gamma_dither_table[(g << 6) | dith];
            dith = ((cdith >> 9) & 0x38) | (cdith & 7);
            b = gamma_dither_table[(b << 6) | dith];
            break;
    }
    argb[1 ^ BYTE_ADDR_XOR] = (unsigned char)(r);
    argb[2 ^ BYTE_ADDR_XOR] = (unsigned char)(g);
    argb[3 ^ BYTE_ADDR_XOR] = (unsigned char)(b);
}

static void adjust_brightness(unsigned char* argb, int brightcoeff)
{
    int r, g, b;

    r = argb[1 ^ BYTE_ADDR_XOR];
    g = argb[2 ^ BYTE_ADDR_XOR];
    b = argb[3 ^ BYTE_ADDR_XOR];
    brightcoeff &= 7;
    switch (brightcoeff)
    {
        case 0:    
            break;
        case 1: 
        case 2:
        case 3:
            r += (r >> (4 - brightcoeff));
            g += (g >> (4 - brightcoeff));
            b += (b >> (4 - brightcoeff));
            if (r > 0xFF)
                r = 0xFF;
            if (g > 0xFF)
                g = 0xFF;
            if (b > 0xFF)
                b = 0xFF;
            break;
        case 4:
        case 5:
        case 6:
        case 7:
            r = (r + 1) << (brightcoeff - 3);
            g = (g + 1) << (brightcoeff - 3);
            b = (b + 1) << (brightcoeff - 3);
            if (r > 0xFF)
                r = 0xFF;
            if (g > 0xFF)
                g = 0xFF;
            if (b > 0xFF)
                b = 0xFF;
            break;
    }
    argb[1 ^ BYTE_ADDR_XOR] = (unsigned char)(r);
    argb[2 ^ BYTE_ADDR_XOR] = (unsigned char)(g);
    argb[3 ^ BYTE_ADDR_XOR] = (unsigned char)(b);
}

STRICTINLINE static void vi_vl_lerp(CCVG* up, CCVG down, uint32_t frac)
{
    uint32_t r0, g0, b0;

    if (frac == 0)
        return;

    r0 = up -> r;
    g0 = up -> g;
    b0 = up -> b;

    up -> r = (((frac*(down.r - r0) + 16) >> 5) + r0) & 0xFF;
    up -> g = (((frac*(down.g - g0) + 16) >> 5) + g0) & 0xFF;
    up -> b = (((frac*(down.b - b0) + 16) >> 5) + b0) & 0xFF;
}

STRICTINLINE void video_max_optimized(uint32_t* pixels, uint32_t* penumin, uint32_t* penumax, int numofels)
{
	int i;
	uint32_t max, min;
	int posmax = 0, posmin = 0;
	uint32_t curpenmax = pixels[0], curpenmin = pixels[0];

	for (i = 1; i < numofels; i++)
	{
	    if (pixels[i] > pixels[posmax])
		{
			curpenmax = pixels[posmax];
			posmax = i;			
		}
		else if (pixels[i] < pixels[posmin])
		{
			curpenmin = pixels[posmin];
			posmin = i;
		}
	}
	max = pixels[posmax];
	min = pixels[posmin];
	if (curpenmax != max)
	{
		for (i = posmax + 1; i < numofels; i++)
		{
			if (pixels[i] > curpenmax)
				curpenmax = pixels[i];
		}
	}
	if (curpenmin != min)
	{
		for (i = posmin + 1; i < numofels; i++)
		{
			if (pixels[i] < curpenmin)
				curpenmin = pixels[i];
		}
	}
	*penumax = curpenmax;
	*penumin = curpenmin;
}

NOINLINE void DisplayError(char * error)
{
    //MessageBox(NULL, error, NULL, MB_ICONERROR);
}
