
#include "TexLoad.h"
#include "Gfx_1.3.h"
#include "Combine.h"
#include "Util.h"

#define ALOWORD(x)   (*((uint16_t*)&x))   // low word

static INLINE void load4bCI(uint8_t *src, uint8_t *dst, int wid_64, int height, uint16_t line, int ext, uint16_t *pal)
{
    uint32_t *src32 =(uint32_t*)src;
    uint32_t *dst32 =(uint32_t*)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;

        while (width--)
        {
            uint32_t v12 = m64p_swap32(src32[odd]);
            uint32_t v16 = m64p_swap32(src32[!odd]);
            uint32_t pix = width + 1;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v12 >> 23) & 0x1E)), 1);
            pix = pix << 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v12 >> 27) & 0x1E)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v12 >> 15) & 0x1E)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v12 >> 19) & 0x1E)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v12 >> 7) & 0x1E)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v12 >> 11) & 0x1E)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + (2 *(uint8_t)v12 & 0x1E)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v12 >> 3) & 0x1E)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v16 >> 23) & 0x1E)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v16 >> 27) & 0x1E)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v16 >> 15) & 0x1E)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v16 >> 19) & 0x1E)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v16 >> 7) & 0x1E)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v16 >> 11) & 0x1E)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + (2 *(uint8_t)v16 & 0x1E)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((v16 >> 3) & 0x1E)), 1);
            *dst32++ = pix;

            src32 += 2;
        }

        src32 =(uint32_t*)&src[(line +(uintptr_t)src32 -(uintptr_t)src) & 0x7FF];
        dst32 =(uint32_t*)((uint8_t*)dst32 + ext);

        odd ^= 1;
    }
}

static INLINE void load4bIAPal(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext, uint16_t *pal)
{
    uint32_t *src32 =(uint32_t*)src;
    uint32_t *dst32 =(uint32_t*)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;
        while (width--)
        {
            uint32_t ab = m64p_swap32(src32[odd]);
            uint32_t cd = m64p_swap32(src32[!odd]);
            uint32_t pix = width + 1;

            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((ab >> 23) & 0x1E)), 8);
            pix = pix << 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((ab >> 27) & 0x1E)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((ab >> 15) & 0x1E)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((ab >> 19) & 0x1E)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((ab >> 7) & 0x1E)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((ab >> 11) & 0x1E)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + (2 *(uint8_t)ab & 0x1E)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((ab >> 3) & 0x1E)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((cd >> 23) & 0x1E)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((cd >> 27) & 0x1E)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((cd >> 15) & 0x1E)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((cd >> 19) & 0x1E)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((cd >> 7) & 0x1E)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((cd >> 11) & 0x1E)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + (2 *(uint8_t)cd & 0x1E)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((char *)pal + ((cd >> 3) & 0x1E)), 8);
            *dst32++ = pix;

            src32 += 2;
        }

        src32 =(uint32_t*)&src[(line +(uintptr_t)src32 -(uintptr_t)src) & 0x7FF];
        dst32 =(uint32_t*)((uint8_t*)dst32 + ext);

        odd ^= 1;
    }
}

static INLINE void load4bIA(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext)
{
   unsigned odd = 0;
   uint32_t *src32 = (uint32_t*)src;
   uint32_t *dst32 = (uint32_t*)dst;

   while (height--)
   {
      int count = wid_64;
      while (count--)
      {
         uint32_t v1, v2, v3, v4, v5, v6, v7, v8, v9, v10;

#define do_load4biA \
         v2 = v1;\
         v3 = (8 * (v1 & 0x100000)) | (4 * (v1 & 0x100000)) | (2 * (v1 & 0x100000)) | (v1 & 0x100000) | ((((v1 >> 16) & 0xE00) >> 3) & 0x100) | ((v1 >> 16) & 0xE00) | (8 * ((v1 >> 12) & 0x1000)) | (4 * ((v1 >> 12) & 0x1000)) | (2 * ((v1 >> 12) & 0x1000)) | ((v1 >> 12) & 0x1000) | ((((v1 >> 28) & 0xE) >> 3)) | ((v1 >> 28) & 0xE) | (8 * ((v1 >> 24) & 0x10)) | (4 * ((v1 >> 24) & 0x10)) | (2 * ((v1 >> 24) & 0x10)) | ((v1 >> 24) & 0x10);\
         v1 = (v1 >> 4) & 0xE0000u;\
         v4 = v1 | v3;\
         v1 >>= 3;\
         *dst32++ = ((((v2 << 8) & 0xE000000) >> 3) & 0x1000000) | ((v2 << 8) & 0xE000000) | (8 * ((v2 << 12) & 0x10000000)) | (4 * ((v2 << 12) & 0x10000000)) | (2 * ((v2 << 12) & 0x10000000)) | ((v2 << 12) & 0x10000000) | (v1 & 0x10000) | v4;\
         \
         v5 = 16 * (uint16_t)v2 & 0x1000;\
         v6 = (((v2 & 0xE00) >> 3) & 0x100) | (v2 & 0xE00) | (8 * v5) | (4 * v5) | (2 * v5) | (v5) | ((((v2 >> 12) & 0xE) >> 3)) | ((v2 >> 12) & 0xE) | (8 * ((v2 >> 8) & 0x10)) | (4 * ((v2 >> 8) & 0x10)) | (2 * ((v2 >> 8) & 0x10)) | ((v2 >> 8) & 0x10);\
         v7 = v2 << 16;\
         v8 = (8 * (v7 & 0x100000)) | (4 * (v7 & 0x100000)) | (2 * (v7 & 0x100000)) | (v7 & 0x100000) | v6;\
         v9 = (v2 << 12) & 0xE0000u;\
         v10 = v9 | v8;\
         v9 >>= 3;\
         *dst32++ = ((((v2 << 24) & 0xE000000) >> 3) & 0x1000000) | ((v2 << 24) & 0xE000000) | (8 * ((v2 << 28) & 0x10000000)) | (4 * ((v2 << 28) & 0x10000000)) | (2 * ((v2 << 28) & 0x10000000)) | ((v2 << 28) & 0x10000000) | (v9 & 0x10000) | v10

         v1 = m64p_swap32(src32[odd]);
         do_load4biA;

         v1 = m64p_swap32(src32[!odd]);

         do_load4biA;
#undef do_load4biA

         src32 += 2;
      }

      src32 = (uint32_t*)((char*)src32 + line);
      dst32 = (uint32_t*)((char*)dst32 + ext);
      odd ^= 1;
   }
}

static INLINE void load4bI(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext)
{
    uint32_t *src32 =(uint32_t*)src;
    uint32_t *dst32 =(uint32_t*)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;

        while (width--)
        {
            uint32_t ab = m64p_swap32(src32[odd]);
            uint32_t cd = m64p_swap32(src32[!odd]);
            uint32_t pix;

            pix = ab >> 4;
            *dst32++ = (16 * ((ab << 8) & 0xF000000)) | ((ab << 8) & 0xF000000) | (16 * (pix & 0xF0000)) | (pix & 0xF0000) | (16 * ((ab >> 16) & 0xF00)) | ((ab >> 16) & 0xF00) | (16 * (ab >> 28)) | (ab >> 28);

            pix = ab << 12;
            *dst32++ = (16 * ((ab << 24) & 0xF000000)) | ((ab << 24) & 0xF000000) | (16 * (pix & 0xF0000)) | (pix & 0xF0000) | (16 * (ab & 0xF00)) | (ab & 0xF00) | (16 * ((uint16_t)ab >> 12)) | ((uint16_t)ab >> 12);

            pix = cd >> 4;
            *dst32++ = (16 * ((cd << 8) & 0xF000000)) | ((cd << 8) & 0xF000000) | (16 * (pix & 0xF0000)) | (pix & 0xF0000) | (16 * ((cd >> 16) & 0xF00)) | ((cd >> 16) & 0xF00) | (16 * (cd >> 28)) | (cd >> 28);

            pix = cd << 12;
            *dst32++ = (16 * ((cd << 24) & 0xF000000)) | ((cd << 24) & 0xF000000) | (16 * (pix & 0xF0000)) | (pix & 0xF0000) | (16 * (cd & 0xF00)) | (cd & 0xF00) | (16 * ((uint16_t)cd >> 12)) | ((uint16_t)cd >> 12);

            src32 += 2;
        }

        src32 =(uint32_t*)((uint8_t*)src32 + line);
        dst32 =(uint32_t*)((uint8_t*)dst32 + ext);
        odd ^= 1;
    }
}


static INLINE void load8bCI(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext, uint16_t *pal)
{
    uint32_t *src32 =(uint32_t*)src;
    uint32_t *dst32 =(uint32_t *)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;

        while (width--)
        {
            uint32_t abcd = m64p_swap32(src32[odd]);
            uint32_t efgh = m64p_swap32(src32[!odd]);
            uint32_t pix = width + 1;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((abcd >> 15) & 0x1FE)), 1);
            pix = pix << 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((abcd >> 23) & 0x1FE)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + (2 *(uint16_t)abcd & 0x1FE)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((abcd >> 7) & 0x1FE)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((efgh >> 15) & 0x1FE)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((efgh >> 23) & 0x1FE)), 1);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + (2 *(uint16_t)efgh & 0x1FE)), 1);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t*)((uint8_t*)pal + ((efgh >> 7) & 0x1FE)), 1);
            *dst32++ = pix;

            src32 += 2;
        }

        src32 =(uint32_t*)&src[(line +(uintptr_t)src32 -(uintptr_t)src) & 0x7FF];
        dst32 =(uint32_t*)((char *)dst32 + ext);

        odd ^= 1;
    }
}

static INLINE void load8bIA8(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext, uint16_t *pal)
{
    uint32_t *src32 =(uint32_t *)src;
    uint32_t *dst32 =(uint32_t *)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;

        while (width--)
        {
            uint32_t abcd = m64p_swap32(src32[odd]);
            uint32_t efgh = m64p_swap32(src32[!odd]);
            uint32_t pix  = width + 1;

            ALOWORD(pix) = ror16(*(uint16_t *)((uint8_t*)pal + ((abcd >> 15) & 0x1FE)), 8);
            pix = pix << 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((uint8_t*)pal + ((abcd >> 23) & 0x1FE)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((uint8_t*)pal + (2 *(uint16_t)abcd & 0x1FE)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((uint8_t*)pal + ((abcd >> 7) & 0x1FE)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((uint8_t*)pal + ((efgh >> 15) & 0x1FE)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((uint8_t*)pal + ((efgh >> 23) & 0x1FE)), 8);
            *dst32++ = pix;

            ALOWORD(pix) = ror16(*(uint16_t *)((uint8_t*)pal + (2 *(uint16_t)efgh & 0x1FE)), 8);
            pix <<= 16;
            ALOWORD(pix) = ror16(*(uint16_t *)((uint8_t*)pal + ((efgh >> 7) & 0x1FE)), 8);
            *dst32++ = pix;

            src32 += 2;
        }

        src32 =(uint32_t *)((uint8_t*)src32 + line);
        dst32 =(uint32_t *)((uint8_t*)dst32 + ext);
        odd ^= 1;
    }
}

static INLINE void load8bIA4(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext)
{
    uint32_t *src32 =(uint32_t *)src;
    uint32_t *dst32 =(uint32_t *)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;

        while(width--)
        {
            uint32_t ab = src32[odd];
            uint32_t cd = src32[!odd];

            *dst32++ = (16 * ab & 0xF0F0F0F0) | ((ab >> 4) & 0xF0F0F0F);
            *dst32++ = (16 * cd & 0xF0F0F0F0) | ((cd >> 4) & 0xF0F0F0F);

            src32 += 2;
        }

        src32 =(uint32_t *)((uint8_t*)src32 + line);
        dst32 =(uint32_t *)((uint8_t*)dst32 + ext);
        odd ^= 1;
    }
}

static INLINE void load8bI(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext)
{
    uint32_t *src32 =(uint32_t *)src;
    uint32_t *dst32 =(uint32_t *)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;

        while (width--)
        {
            *dst32++ = src32[odd];
            *dst32++ = src32[!odd];
            src32 += 2;
        }

        src32 =(uint32_t *)((uint8_t*)src32 + line);
        dst32 =(uint32_t *)((uint8_t*)dst32 + ext);
        odd ^= 1;
    }
}


static INLINE void load16bRGBA(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext)
{
    uint32_t *src32 =(uint32_t*)src;
    uint32_t *dst32 =(uint32_t*)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;

        while (width--)
        {
            uint32_t ab = m64p_swap32(src32[odd]);
            uint32_t cd = m64p_swap32(src32[!odd]);

            ALOWORD(ab) = ror16((uint16_t)ab, 1);
            ALOWORD(cd) = ror16((uint16_t)cd, 1);
            ab = ror32(ab, 16);
            cd = ror32(cd, 16);
            ALOWORD(ab) = ror16((uint16_t)ab, 1);
            ALOWORD(cd) = ror16((uint16_t)cd, 1);

            *dst32++ = ab;
            *dst32++ = cd;

            src32 += 2;
        }

        src32 =(uint32_t*)&src[(line +(uintptr_t)src32 -(uintptr_t)src) & 0xFFF];
        dst32 =(uint32_t*)((uint8_t*)dst32 + ext);

        odd ^= 1;
    }
}

static INLINE void load16bIA(uint8_t *src, uint8_t *dst, int wid_64, int height, int line, int ext)
{
    uint32_t *src32 =(uint32_t *)src;
    uint32_t *dst32 =(uint32_t *)dst;
    unsigned odd = 0;

    while (height--)
    {
        int width = wid_64;

        while (width--)
        {
            *dst32++ = src32[odd];
            *dst32++ = src32[!odd];

            src32 += 2;
        }

        src32 =(uint32_t*)((uint8_t*)src32 + line);
        dst32 =(uint32_t*)((uint8_t*)dst32 + ext);

        odd ^= 1;
    }
}

static uint32_t LoadNone(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    (void)dst;
    (void)src;
    (void)wid_64;
    (void)height;
    (void)line;
    (void)real_width;
    (void)tile;

    return GR_TEXFMT_ARGB_1555;
}

//****************************************************************
// Size: 0, Format: 2

static uint32_t Load4bCI(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    int ext;
    uintptr_t pal;

    if (wid_64 < 1)
        wid_64 = 1;
    if (height < 1)
        height = 1;
    ext = (real_width - (wid_64 << 4));

    if (rdp.tlut_mode == 0)
    {
        //in tlut DISABLE mode load CI texture as plain intensity texture instead of palette dereference.
        //Thanks to angrylion for the advice
        load4bI ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext);
        return /*(0 << 16) | */GR_TEXFMT_ALPHA_INTENSITY_44;
    }

    pal =(uintptr_t)(rdp.pal_8 + (g_gdp.tile[tile].palette << 4));
    if (rdp.tlut_mode == 2)
    {
        ext <<= 1;
        load4bCI ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext,(uint16_t *)pal);

        return (1 << 16) | GR_TEXFMT_ARGB_1555;
    }

    ext <<= 1;
    load4bIAPal ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext,(uint16_t *)pal);
    return (1 << 16) | GR_TEXFMT_ALPHA_INTENSITY_88;
}

//****************************************************************
// Size: 0, Format: 3
//
// ** BY GUGAMAN **

static uint32_t Load4bIA(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    int ext;
    if (rdp.tlut_mode != 0)
        return Load4bCI (dst, src, wid_64, height, line, real_width, tile);

    if (wid_64 < 1) wid_64 = 1;
    if (height < 1) height = 1;
    ext = (real_width - (wid_64 << 4));
    load4bIA ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext);
    return /*(0 << 16) | */GR_TEXFMT_ALPHA_INTENSITY_44;
}

//****************************************************************
// Size: 0, Format: 4

static uint32_t Load4bI(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    int ext;
    if (rdp.tlut_mode != 0)
        return Load4bCI (dst, src, wid_64, height, line, real_width, tile);

    if (wid_64 < 1)
        wid_64 = 1;
    if (height < 1)
        height = 1;
    ext = (real_width - (wid_64 << 4));
    load4bI ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext);

    return /*(0 << 16) | */GR_TEXFMT_ALPHA_INTENSITY_44;
}

//****************************************************************
// Size: 0, Format: 0

static uint32_t Load4bSelect(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    if (rdp.tlut_mode == 0)
        return Load4bI (dst, src, wid_64, height, line, real_width, tile);

    return Load4bCI (dst, src, wid_64, height, line, real_width, tile);
}



//****************************************************************
// Size: 1, Format: 2
//

static uint32_t Load8bCI(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    int ext;
    unsigned short *pal;

    if (wid_64 < 1)
        wid_64 = 1;
    if (height < 1)
        height = 1;

    ext = (real_width - (wid_64 << 3));
    pal = (unsigned short*)rdp.pal_8;

    switch (rdp.tlut_mode)
    {
    case 0:
        //palette is not used
        //in tlut DISABLE mode load CI texture as plain intensity texture instead of palette dereference.
        //Thanks to angrylion for the advice
        load8bI ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext);
        return /*(0 << 16) | */GR_TEXFMT_ALPHA_8;
    case 2: //color palette
        ext <<= 1;
        load8bCI ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext, pal);
        return (1 << 16) | GR_TEXFMT_ARGB_1555;
    default: //IA palette
        ext <<= 1;
        load8bIA8 ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext, pal);
        return (1 << 16) | GR_TEXFMT_ALPHA_INTENSITY_88;
    }
}

//****************************************************************
// Size: 1, Format: 3
//
// ** by Gugaman **

static uint32_t Load8bIA(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    int ext;
    if (rdp.tlut_mode != 0)
        return Load8bCI (dst, src, wid_64, height, line, real_width, tile);

    if (wid_64 < 1) wid_64 = 1;
    if (height < 1) height = 1;
    ext = (real_width - (wid_64 << 3));
    load8bIA4 ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext);
    return /*(0 << 16) | */GR_TEXFMT_ALPHA_INTENSITY_44;
}

//****************************************************************
// Size: 1, Format: 4
//
// ** by Gugaman **

static uint32_t Load8bI(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    int ext;
    if (rdp.tlut_mode != 0)
        return Load8bCI (dst, src, wid_64, height, line, real_width, tile);

    if (wid_64 < 1) wid_64 = 1;
    if (height < 1) height = 1;
    ext = (real_width - (wid_64 << 3));
    load8bI ((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext);
    return /*(0 << 16) | */GR_TEXFMT_ALPHA_8;
}


//****************************************************************
// Size: 2, Format: 0
//

static uint32_t Load16bRGBA(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    int ext;
    if (wid_64 < 1)
        wid_64 = 1;
    if (height < 1)
        height = 1;
    ext = (real_width - (wid_64 << 2)) << 1;

    load16bRGBA((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext);

    return (1 << 16) | GR_TEXFMT_ARGB_1555;
}

//****************************************************************
// Size: 2, Format: 3
//
// ** by Gugaman/Dave2001 **

static uint32_t Load16bIA(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    int ext;
    if (wid_64 < 1)
        wid_64 = 1;
    if (height < 1)
        height = 1;
    ext = (real_width - (wid_64 << 2)) << 1;

    load16bIA((uint8_t *)src,(uint8_t *)dst, wid_64, height, line, ext);

    return (1 << 16) | GR_TEXFMT_ALPHA_INTENSITY_88;
}

//****************************************************************
// Size: 2, Format: 1
//

static uint16_t yuv_to_rgb565(uint8_t y, uint8_t u, uint8_t v)
{
    float r = y + (1.370705f * (v-128));
    float g = y - (0.698001f * (v-128)) - (0.337633f * (u-128));
    float b = y + (1.732446f * (u-128));

    r *= 0.125f;
    g *= 0.25f;
    b *= 0.125f;
    //clipping the result
    if (r > 31) r = 31;
    if (g > 63) g = 63;
    if (b > 31) b = 31;
    if (r < 0) r = 0;
    if (g < 0) g = 0;
    if (b < 0) b = 0;
    return(uint16_t)(((uint16_t)(r) << 11) |
                     ((uint16_t)(g) << 5) |
                     (uint16_t)(b) );
    //*/
    /*
      const uint32_t c = y - 16;
      const uint32_t d = u - 128;
      const uint32_t e = v - 128;

      uint32_t r =  (298 * c           + 409 * e + 128) & 0xf800;
      uint32_t g = ((298 * c - 100 * d - 208 * e + 128) >> 5) & 0x7e0;
      uint32_t b = ((298 * c + 516 * d           + 128) >> 11) & 0x1f;

      WORD texel = (WORD)(r | g | b);

      return texel;
      */
}

//****************************************************************
// Size: 2, Format: 1
//

static uint32_t Load16bYUV(uintptr_t dst, uintptr_t src,
                           int wid_64, int height, int line, int real_width, int tile)
{
    uint16_t i;
    uint32_t *mb =(uint32_t*)(gfx_info.RDRAM+rdp.addr[g_gdp.tile[tile].tmem]); //pointer to the macro block
    uint16_t *tex =(uint16_t*)dst;

    for (i = 0; i < 128; i++)
    {
        uint32_t  t = mb[i]; //each uint32_t contains 2 pixels
        uint8_t  y1 =(uint8_t)t&0xFF;
        uint8_t  v  =(uint8_t)(t>>8)&0xFF;
        uint8_t  y0 =(uint8_t)(t>>16)&0xFF;
        uint8_t  u  =(uint8_t)(t>>24)&0xFF;
        uint16_t c = yuv_to_rgb565(y0, u, v);

        *(tex++) = c;
        c = yuv_to_rgb565(y1, u, v);
        *(tex++) = c;
    }
    return (1 << 16) | GR_TEXFMT_RGB_565;
}


//****************************************************************
// Size: 2, Format: 0
//
// Load 32bit RGBA texture
// Based on sources of angrylion's software plugin.
//
static uint32_t Load32bRGBA(uintptr_t dst, uintptr_t src, int wid_64, int height, int line, int real_width, int tile)
{
    uint32_t s, t, c, *tex;
    uint16_t rg, ba;
    int id;
    uint32_t mod;
    const uint16_t *tmem16 =(uint16_t*)g_gdp.tmem;
    const uint32_t tbase = (src -(uintptr_t)g_gdp.tmem) >> 1;
    const uint32_t width = max(1, wid_64 << 1);
    const int ext = real_width - width;
    line = width + (line>>2);
    tex  =(uint32_t*)dst;
    if (height < 1)
        height = 1;

    for (t = 0; t <(uint32_t)height; t++)
    {
        uint32_t tline = tbase + line * t;
        uint32_t xorval = (t & 1) ? 3 : 1;
        for (s = 0; s < width; s++)
        {
            uint32_t taddr = ((tline + s) ^ xorval) & 0x3ff;
            rg = tmem16[taddr];
            ba = tmem16[taddr|0x400];
            c = ((ba&0xFF)<<24) | (rg << 8) | (ba>>8);
            *tex++ = c;
        }
        tex += ext;
    }
    id = tile - rdp.cur_tile;
    mod = (id == 0) ? cmb.mod_0 : cmb.mod_1;
    if (mod /*|| !voodoo.sup_32bit_tex*/)
    {
        uint32_t i;
        //convert to ARGB_4444
        const uint32_t tex_size = real_width * height;
        uint16_t *tex16 = (uint16_t*)dst;
        tex   = (uint32_t *)dst;

        for (i = 0; i < tex_size; i++)
        {
           uint16_t a, r, g, b;
           c = tex[i];
           a = (c >> 28) & 0xF;
           r = (c >> 20) & 0xF;
           g = (c >> 12) & 0xF;
           b = (c >> 4)  & 0xF;
           tex16[i] = (a <<12) | (r << 8) | (g << 4) | b;
        }
        return (1 << 16) | GR_TEXFMT_ARGB_4444;
    }
    return (2 << 16) | GR_TEXFMT_ARGB_8888;
}

//****************************************************************
// LoadTile for 32bit RGBA texture
// Based on sources of angrylion's software plugin.
//
void LoadTile32b(uint32_t tile, uint32_t ul_s, uint32_t ul_t, uint32_t width, uint32_t height)
{
    uint32_t j;
    const uint32_t line  = g_gdp.tile[tile].line << 2;
    const uint32_t tbase = g_gdp.tile[tile].tmem << 2;
    const uint32_t addr  = g_gdp.ti_address >> 2;
    const uint32_t* src  = (const uint32_t*)gfx_info.RDRAM;
    uint16_t *tmem16     =(uint16_t*)g_gdp.tmem;

    for (j = 0; j < height; j++)
    {
       uint32_t i;
       uint32_t  tline = tbase + line * j;
       uint32_t      s = ((j + ul_t) * g_gdp.ti_width) + ul_s;
       uint32_t xorval = (j & 1) ? 3 : 1;

       for (i = 0; i < width; i++)
       {
          uint32_t c = src[addr + s + i];
          uint32_t ptr = ((tline + i) ^ xorval) & 0x3ff;
          tmem16[ptr] = c >> 16;
          tmem16[ptr|0x400] = c & 0xffff;
       }
    }
}

//****************************************************************
// LoadBlock for 32bit RGBA texture
// Based on sources of angrylion's software plugin.
//
void LoadBlock32b(uint32_t tile, uint32_t ul_s,
                  uint32_t ul_t, uint32_t lr_s, uint32_t dxt)
{
    const uint32_t * src       = (const uint32_t*)gfx_info.RDRAM;
    const uint32_t tb          = g_gdp.tile[tile].tmem << 2;
    const uint32_t tiwindwords = g_gdp.ti_width;
    const uint32_t slindwords  = ul_s;
    const uint32_t line        = g_gdp.tile[tile].line << 2;
    uint16_t *tmem16           =(uint16_t*)g_gdp.tmem;
    uint32_t addr              = g_gdp.ti_address >> 2;
    uint32_t width             = (lr_s - ul_s + 1) << 2;

    if (width & 7)
        width = (width & (~7)) + 8;

    if (dxt != 0)
    {
        uint32_t i;
        uint32_t t    = 0;
        uint32_t j    = 0;
        uint32_t oldt = 0;

        addr += (ul_t * tiwindwords) + slindwords;

        for (i = 0; i < width; i += 2)
        {
           uint32_t c, ptr;

           oldt = t;
           t = ((j >> 11) & 1) ? 3 : 1;
           if (t != oldt)
              i += line;
           ptr = ((tb + i) ^ t) & 0x3ff;
           c = src[addr + i];
           tmem16[ptr] = c >> 16;
           tmem16[ptr|0x400] = c & 0xffff;
           ptr = ((tb+ i + 1) ^ t) & 0x3ff;
           c = src[addr + i + 1];
           tmem16[ptr] = c >> 16;
           tmem16[ptr|0x400] = c & 0xffff;
           j += dxt;
        }
    }
    else
    {
        uint32_t i;
        addr += (ul_t * tiwindwords) + slindwords;

        for (i = 0; i < width; i ++)
        {
            uint32_t ptr = ((tb + i) ^ 1) & 0x3ff;
            uint32_t c = src[addr + i];
            tmem16[ptr] = c >> 16;
            tmem16[ptr|0x400] = c & 0xffff;
        }
    }
}

texfunc load_table[4][5] = { // [size][format]
                             { Load4bSelect, LoadNone,   Load4bCI,    Load4bIA,  Load4bI  },
                             { Load8bCI,     LoadNone,   Load8bCI,    Load8bIA,  Load8bI  },
                             { Load16bRGBA,  Load16bYUV, Load16bRGBA, Load16bIA, LoadNone },
                             { Load32bRGBA,  LoadNone,   LoadNone,    LoadNone,  LoadNone }
                           };


static INLINE void load_block_line(uint32_t *src, uint32_t *dst,
      unsigned offset, unsigned width, bool is_load_block)
{
   unsigned length = width;
   uint32_t *src32 = (uint32_t*)((uint8_t*)src + (offset & 0xFFFFFFFC));
   uint32_t *dst32 = dst;

   if (!length)
      return;

   if (offset & 3)
   {
      uint32_t numrot = offset & 3;
      uint32_t nwords = 4 - numrot;
      uint32_t word   = *src32++;

      while (numrot--)
         word = rol32(word, 8);

      if (is_load_block)
      {
         while (nwords--)
         {
            word = rol32(word, 8);
            *dst32++ = word;
         }
      }
      else
      {
         while (nwords--)
         {
            word = rol32(word, 8);
            *(uint8_t*)dst32 = word;
            dst32 = (uint32_t*)((uint8_t*)dst32 + 1);
         }
      }

      *dst32++ = m64p_swap32(*src32++);

      --length;
   }

   while (length--)
   {
      *dst32++ = m64p_swap32(*src32++);
      *dst32++ = m64p_swap32(*src32++);
   }

   if (offset & 3)
   {
      uint32_t nwords = offset & 3;
      uint32_t word   = *(uint32_t*)((uint8_t*)src + ((8 * width + offset) & 0xFFFFFFFC));

      if (is_load_block)
      {
         while (nwords--)
         {
            word = rol32(word, 8);
            *dst32++ = word;
         }
      }
      else
      {
         while (nwords--)
         {
            word = rol32(word, 8);
            *(uint8_t*)dst32 = word;
            dst32 = (uint32_t*)((uint8_t*)dst32 + 1);
         }
      }
   }
}

static INLINE void dxt_swap(uint32_t *line, int width)
{
   while (width--)
   {
      line[0] ^= line[1];
      line[1] ^= line[0];
      line[0] ^= line[1];
      line += 2;
   }
}

void loadTile(uint32_t *src, uint32_t *dst,
      int width, int height, int line, int off, uint32_t *end)
{
   unsigned odd = 0;

   while (height-- && end >= dst)
   {
      load_block_line(src, dst, off, width, false);

      if (odd)
         dxt_swap(dst, width);

      dst += width * 2;
      off += line;
      odd ^= 1;
   }
}

void loadBlock(uint32_t *src, uint32_t *dst, uint32_t off, int dxt, int cnt)
{
   int32_t v16 = 0;
   int32_t length = cnt;

   load_block_line(src, dst, off, cnt, true);

   while (length-- > 0)
   {
      int32_t v18 = 0;

      dst += 2;
      v16 += dxt;

      while (v16 < 0 && length--)
      {
         ++v18;
         v16 += dxt;
      }

      dxt_swap(dst, v18);
      dst += v18 * 2;
   }
}
