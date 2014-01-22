/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - ucode2.c                                        *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2014 Bobby Smiles                                       *
 *   Copyright (C) 2009 Richard Goedeken                                   *
 *   Copyright (C) 2002 Hacktarux                                          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

#include <string.h>
#include <stdbool.h>
#include <stdint.h>

#include "hle.h"
#include "alist_internal.h"

/* alist state */
static struct {
    /* main buffers */
    uint16_t in;
    uint16_t out;
    uint16_t count;

    /* envmixer ramps */
    uint16_t env_values[3];
    uint16_t env_steps[3];

    /* ADPCM loop point address */
    uint32_t loop;

    /* storage for ADPCM table and polef coefficients */
    int16_t table[16 * 8];

    /* filter audio command state */
    uint16_t filter_count;
    uint32_t filter_lut_address[2];
} l_alist;


static void SPNOOP(uint32_t w1, uint32_t w2)
{
    DebugMessage(M64MSG_ERROR, "Unknown/Unimplemented Audio Command %i in ABI 2", (int)(w1 >> 24));
}


static bool isMKABI = false;
static bool isZeldaABI = false;

void init_ucode2(void)
{
    isMKABI = isZeldaABI = false;
}

static void LOADADPCM2(uint32_t w1, uint32_t w2)
{
    uint16_t count   = (w1 & 0xffff);
    uint32_t address = (w2 & 0xffffff);

    dram_load_u16((uint16_t*)l_alist.table, address, count >> 1);
}

static void SETLOOP2(uint32_t w1, uint32_t w2)
{
    l_alist.loop = w2 & 0xffffff;
}

static void SETBUFF2(uint32_t w1, uint32_t w2)
{
    l_alist.in    = w1;
    l_alist.out   = (w2 >> 16);
    l_alist.count = w2;
}

static void ADPCM2(uint32_t w1, uint32_t w2)
{
    uint8_t  flags   = (w1 >> 16);
    uint32_t address = (w2 & 0xffffff);

    alist_adpcm(
            flags & 0x1,
            flags & 0x2,
            flags & 0x4,
            l_alist.out,
            l_alist.in,
            (l_alist.count + 0x1f) & ~0x1f,
            l_alist.table,
            l_alist.loop,
            address);
}

static void CLEARBUFF2(uint32_t w1, uint32_t w2)
{
    uint16_t dmem  = w1;
    uint16_t count = w2;

    if (count == 0)
        return;

    alist_clear(dmem, count);
}

static void LOADBUFF2(uint32_t w1, uint32_t w2)
{
    uint16_t count   = (w1 >> 12) & 0xfff;
    uint16_t dmem    = (w1 & 0xfff);
    uint32_t address = (w2 & 0xffffff);

    alist_load(dmem & ~3, address & ~3, (count + 3) & ~3);
}

static void SAVEBUFF2(uint32_t w1, uint32_t w2)
{
    uint16_t count   = (w1 >> 12) & 0xfff;
    uint16_t dmem    = (w1 & 0xfff);
    uint32_t address = (w2 & 0xffffff);

    alist_save(dmem & ~3, address & ~3, (count + 3) & ~3);
}

static void MIXER2(uint32_t w1, uint32_t w2)
{
    uint16_t count = (w1 >> 12) & 0xff0;
    int16_t  gain  = w1;
    uint16_t dmemi = (w2 >> 16);
    uint16_t dmemo = w2;

    alist_mix(dmemo, dmemi, count, gain);
}


static void RESAMPLE2(uint32_t w1, uint32_t w2)
{
    uint8_t  flags   = (w1 >> 16);
    uint16_t pitch   = w1;
    uint32_t address = (w2 & 0xffffff);

    alist_resample(
            flags & 0x1,
            l_alist.out,
            l_alist.in,
            (l_alist.count + 0xf) & ~0xf,
            pitch << 1,
            address);
}

static void DMEMMOVE2(uint32_t w1, uint32_t w2)
{
    uint16_t dmemi = w1;
    uint16_t dmemo = (w2 >> 16);
    uint16_t count = w2;

    if (count == 0)
        return;

    alist_move(dmemo, dmemi, (count + 3) & ~3);
}

static void ENVSETUP1(uint32_t w1, uint32_t w2)
{
    l_alist.env_values[2] = (w1 >> 8) & 0xff00;
    l_alist.env_steps[2]  = w1;
    l_alist.env_steps[0]  = (w2 >> 16);
    l_alist.env_steps[1]  = w2;

    /* FIXME: MKABI needs its own ABI */
    if (isMKABI)
        l_alist.env_steps[2] = 0;
}

static void ENVSETUP2(uint32_t w1, uint32_t w2)
{
    l_alist.env_values[0] = (w2 >> 16);
    l_alist.env_values[1] = w2;
}

static void ENVMIXER2(uint32_t w1, uint32_t w2)
{
    int16_t xors[4];

    uint16_t dmemi = (w1 >> 12) & 0xff0;
    uint8_t  count = (w1 >>  8) & 0xff;
    bool     swap_wet_LR = (w1 >> 4) & 0x1;
    xors[2] = 0 - (int16_t)((w1 & 0x8) >> 1);
    xors[3] = 0 - (int16_t)((w1 & 0x4) >> 1);
    xors[0] = 0 - (int16_t)((w1 & 0x2) >> 1);
    xors[1] = 0 - (int16_t)((w1 & 0x1)     );
    uint16_t dmem_dl = (w2 >> 20) & 0xff0;
    uint16_t dmem_dr = (w2 >> 12) & 0xff0;
    uint16_t dmem_wl = (w2 >>  4) & 0xff0;
    uint16_t dmem_wr = (w2 <<  4) & 0xff0;

    /* FIXME: MKABI needs its own ABI */
    if (isMKABI)
    {
        swap_wet_LR = 0;
        xors[2] = 0;
        xors[3] = 0;
    }

    alist_envmix_nead(
            swap_wet_LR,
            dmem_dl, dmem_dr,
            dmem_wl, dmem_wr,
            dmemi, count,
            l_alist.env_values,
            l_alist.env_steps,
            xors);
}

static void DUPLICATE2(uint32_t w1, uint32_t w2)
{
    uint8_t  count = (w1 >> 16);
    uint16_t dmemi = w1;
    uint16_t dmemo = (w2 >> 16);

    alist_repeat64(dmemo, dmemi, count);
}

static void INTERL2(uint32_t w1, uint32_t w2)
{
    uint16_t count = w1;
    uint16_t dmemi = (w2 >> 16);
    uint16_t dmemo = w2;

    alist_copy_every_other_sample(dmemo, dmemi, count);
}

static void INTERLEAVE2(uint32_t w1, uint32_t w2)
{
    uint16_t dmemo;
    uint16_t count = ((w1 >> 12) & 0xff0);
    uint16_t left = (w2 >> 16);
    uint16_t right = w2;

    /* FIXME: needs ABI splitting */
    if (count == 0) {
        count = l_alist.count;
        dmemo = l_alist.out;
    }
    else
        dmemo = w1;

    alist_interleave(dmemo, left, right, count);
}

static void ADDMIXER(uint32_t w1, uint32_t w2)
{
    uint16_t count = (w1 >> 12) & 0xff0;
    uint16_t dmemi = (w2 >> 16);
    uint16_t dmemo = w2;

    alist_add(dmemo, dmemi, count);
}

static void HILOGAIN(uint32_t w1, uint32_t w2)
{
    int8_t   gain  = (w1 >> 16); /* Q4.4 signed */
    uint16_t count = w1;
    uint16_t dmem  = (w2 >> 16);

    alist_multQ44(dmem, count, gain);
}

static void FILTER2(uint32_t w1, uint32_t w2)
{
    uint8_t  flags   = (w1 >> 16);
    uint32_t address = (w2 & 0xffffff);

    if (flags > 1) {
        l_alist.filter_count          = w1;
        l_alist.filter_lut_address[0] = address;    // t6
    }
    else {
        uint16_t dmem = w1;

        l_alist.filter_lut_address[1] = address + 0x10; // t5
        alist_filter(dmem, l_alist.filter_count, address, l_alist.filter_lut_address);
    }
}

static void SEGMENT2(uint32_t w1, uint32_t w2)
{
    if (isZeldaABI) {
        FILTER2(w1, w2);
        return;
    }
    if ((w1 & 0xffffff) == 0) {
        isMKABI = true;
    } else {
        isMKABI = false;
        isZeldaABI = true;
        FILTER2(w1, w2);
    }
}

static void UNKNOWN(uint32_t w1, uint32_t w2)
{
}

static const acmd_callback_t ABI2[0x20] = {
    SPNOOP , ADPCM2, CLEARBUFF2, UNKNOWN, ADDMIXER, RESAMPLE2, UNKNOWN, SEGMENT2,
    SETBUFF2 , DUPLICATE2, DMEMMOVE2, LOADADPCM2, MIXER2, INTERLEAVE2, HILOGAIN, SETLOOP2,
    SPNOOP, INTERL2 , ENVSETUP1, ENVMIXER2, LOADBUFF2, SAVEBUFF2, ENVSETUP2, SPNOOP,
    HILOGAIN , SPNOOP, DUPLICATE2 , UNKNOWN    , SPNOOP  , SPNOOP    , SPNOOP  , SPNOOP
};


void alist_process_mk(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_sfj(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_wrjb(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_sf(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_fz(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_ys(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_1080(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_oot(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_mm(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_mmb(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_ac(void)
{
    alist_process(ABI2, 0x20);
}
