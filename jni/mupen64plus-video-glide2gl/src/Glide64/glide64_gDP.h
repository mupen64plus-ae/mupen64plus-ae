#include "Util.h"
#include <stdint.h>
#include "TexLoad.h"

//forward decls
extern void RestoreScale(void);

extern uint32_t ucode5_texshiftaddr;
extern uint32_t ucode5_texshiftcount;
extern uint16_t ucode5_texshift;
extern int CI_SET;
extern uint32_t swapped_addr;

static void gDPSetScissor_G64( uint32_t mode, float ulx, float uly, float lrx, float lry )
{
   g_gdp.__clip.xh = (uint32_t)ulx;
   g_gdp.__clip.yh = (uint32_t)ulx;
   g_gdp.__clip.xl = (uint32_t)lrx;
   g_gdp.__clip.yl = (uint32_t)lry;
   if (rdp.ci_count)
   {
      COLOR_IMAGE *cur_fb = (COLOR_IMAGE*)&rdp.frame_buffers[rdp.ci_count-1];
      if (g_gdp.__clip.xl - g_gdp.__clip.xh > (uint32_t)(cur_fb->width >> 1))
      {
         if (cur_fb->height == 0 || (cur_fb->width >= g_gdp.__clip.xl - 1 && cur_fb->width <= g_gdp.__clip.xl + 1))
            cur_fb->height = g_gdp.__clip.yl;
      }
   }
}

/*
* Loads a texture from DRAM into texture memory (TMEM).
* The texture image is loaded into memory in a single transfer.
*
* tile - Tile descriptor index 93-bit precision, 0~7).
* ul_s - texture tile's upper-left s coordinate (10.2, 0.0~1023.75)
* ul_t - texture tile's upper-left t coordinate (10.2, 0.0~1023.75)
* lr_s - texture tile's lower-right s coordinate (10.2, 0.0~1023.75)
* dxt - amount of change in value of t per scan line (12-bit precision, 0~4095)
*/
static void gDPLoadBlock( uint32_t tile, uint32_t ul_s, uint32_t ul_t, uint32_t lr_s, uint32_t dxt )
{
   uint32_t _dxt, addr, off, cnt;
   uint8_t *dst;

   if (rdp.skip_drawing)
      return;

   if (ucode5_texshiftaddr)
   {
      if (ucode5_texshift % ((lr_s+1)<<3))
      {
         g_gdp.ti_address -= ucode5_texshift;
         ucode5_texshiftaddr = 0;
         ucode5_texshift = 0;
         ucode5_texshiftcount = 0;
      }
      else
         ucode5_texshiftcount++;
   }

   rdp.addr[g_gdp.tile[tile].tmem] = g_gdp.ti_address;

   // ** DXT is used for swapping every other line
   /* double fdxt = (double)0x8000000F/(double)((uint32_t)(2047/(dxt-1))); // F for error
      uint32_t _dxt = (uint32_t)fdxt;*/

   // 0x00000800 -> 0x80000000 (so we can check the sign bit instead of the 11th bit)
   _dxt = dxt << 20;
   addr = RSP_SegmentToPhysical(g_gdp.ti_address);

   g_gdp.tile[tile].sh = ul_s;
   g_gdp.tile[tile].th = ul_t;
   g_gdp.tile[tile].sl = lr_s;

   rdp.timg.set_by = 0; // load block

   // do a quick boundary check before copying to eliminate the possibility for exception
   if (ul_s >= 512)
   {
      lr_s = 1; // 1 so that it doesn't die on memcpy
      ul_s = 511;
   }
   if (ul_s+lr_s > 512)
      lr_s = 512-ul_s;

   if (addr+(lr_s<<3) > BMASK+1)
      lr_s = (uint16_t)((BMASK-addr)>>3);

   //angrylion's advice to use ul_s in texture image offset and cnt calculations.
   //Helps to fix Vigilante 8 jpeg backgrounds and logos
   off = g_gdp.ti_address + (ul_s << g_gdp.tile[tile].size >> 1);
   dst = ((uint8_t*)g_gdp.tmem) + (g_gdp.tile[tile].tmem<<3);
   cnt = lr_s-ul_s+1;
   if (g_gdp.tile[tile].size == 3)
      cnt <<= 1;

   if (g_gdp.ti_size == G_IM_SIZ_32b)
      LoadBlock32b(tile, ul_s, ul_t, lr_s, dxt);
   else
      loadBlock((uint32_t *)gfx_info.RDRAM, (uint32_t *)dst, off, _dxt, cnt);

   g_gdp.ti_address += cnt << 3;
   g_gdp.tile[tile].tl = ul_t + ((dxt*cnt)>>11);

   g_gdp.flags |= UPDATE_TEXTURE;

#ifdef HAVE_HWFBE
   if (fb_hwfbe_enabled)
      setTBufTex(rdp.tiles[tile].t_mem, cnt);
#endif
}
