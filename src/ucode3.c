/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - ucode3.c                                        *
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
#include <stdint.h>

#include "hle.h"
#include "alist_internal.h"

void MP3(uint32_t w1, uint32_t w2);

/* alist naudio state */
static struct {
    /* gains */
    int16_t dry;
    int16_t wet;

    /* envelopes (0:left, 1:right) */
    int16_t vol[2];
    int16_t target[2];
    int32_t rate[2];

    /* ADPCM loop point address */
    uint32_t loop;

    /* storage for ADPCM table and polef coefficients */
    int16_t table[16 * 8];
} l_alist;

static void SETVOL3(uint32_t w1, uint32_t w2)
{
    uint8_t flags = (w1 >> 16);

    if (flags & 0x4) {
        if (flags & 0x2) {
            l_alist.vol[0] = w1;
            l_alist.dry    = (w2 >> 16);
            l_alist.wet    = w2;
        }
        else {
            l_alist.target[1] = w1;
            l_alist.rate[1]   = w2;
        }
    }
    else {
        l_alist.target[0] = w1;
        l_alist.rate[0]   = w2;
    }
}

static void ENVMIXER3(uint32_t w1, uint32_t w2)
{
    uint8_t  flags   = (w1 >> 16);
    uint32_t address = (w2 & 0xffffff);

    l_alist.vol[1] = w1;

    alist_envmix_lin(
            flags & 0x1,
            0x9d0,
            0xb40,
            0xcb0,
            0xe20,
            0x4f0,
            0x170,
            l_alist.dry,
            l_alist.wet,
            l_alist.vol,
            l_alist.target,
            l_alist.rate,
            address);
}

static void CLEARBUFF3(uint32_t w1, uint32_t w2)
{
    uint16_t dmem  = w1 + 0x4f0;
    uint16_t count = w2;

    alist_clear(dmem, count);
}

static void MIXER3(uint32_t w1, uint32_t w2)
{
    int16_t  gain  = w1;
    uint16_t dmemi = (w2 >> 16) + 0x4f0;
    uint16_t dmemo = w2 + 0x4f0;

    alist_mix(dmemo, dmemi, 0x170, gain);
}

static void LOADBUFF3(uint32_t w1, uint32_t w2)
{
    uint16_t count   = (w1 >> 12) & 0xfff;
    uint16_t dmem    = (w1 & 0xfff) + 0x4f0;
    uint32_t address = (w2 & 0xffffff);

    alist_load(dmem & ~3, address & ~3, (count + 3) & ~3);
}

static void SAVEBUFF3(uint32_t w1, uint32_t w2)
{
    uint16_t count   = (w1 >> 12) & 0xfff;
    uint16_t dmem    = (w1 & 0xfff) + 0x4f0;
    uint32_t address = (w2 & 0xffffff);

    alist_save(dmem & ~3, address & ~3, (count + 3) & ~3);
}

static void LOADADPCM3(uint32_t w1, uint32_t w2)
{
    uint16_t count   = (w1 & 0xffff);
    uint32_t address = (w2 & 0xffffff);

    dram_load_u16((uint16_t*)l_alist.table, address, count >> 1);
}

static void DMEMMOVE3(uint32_t w1, uint32_t w2)
{
    uint16_t dmemi = w1 + 0x4f0;
    uint16_t dmemo = (w2 >> 16) + 0x4f0;
    uint16_t count = w2;

    alist_move(dmemo, dmemi, (count + 3) & ~3);
}

static void SETLOOP3(uint32_t w1, uint32_t w2)
{
    l_alist.loop = (w2 & 0xffffff);
}

static void ADPCM3(uint32_t w1, uint32_t w2)
{
    uint32_t address = (w1 & 0xffffff);
    uint8_t  flags   = (w2 >> 28);
    uint16_t count   = (w2 >> 16) & 0xfff;
    uint16_t dmemi   = ((w2 >> 12) & 0xf) + 0x4f0;
    uint16_t dmemo   = (w2 & 0xfff) + 0x4f0;

    alist_adpcm(
            flags & 0x1,
            flags & 0x2,
            false,          /* unsuported by this ucode */
            dmemo,
            dmemi,
            (count + 0x1f) & ~0x1f,
            l_alist.table,
            l_alist.loop,
            address);
}

static void RESAMPLE3(uint32_t w1, uint32_t w2)
{
    uint32_t address = (w1 & 0xffffff);
    uint8_t  flags   = (w2 >> 30);
    uint16_t pitch   = (w2 >> 14);
    uint16_t dmemi   = ((w2 >> 2) & 0xfff) + 0x4f0;
    uint16_t dmemo   = (w2 & 0x3) ? 0x660 : 0x4f0;

    alist_resample(
            flags & 0x1,
            dmemo,
            dmemi,
            0x170,
            pitch << 1,
            address);
}

static void INTERLEAVE3(uint32_t w1, uint32_t w2)
{
    alist_interleave(0x4f0, 0x9d0, 0xb40, 0x170);
}

static void WHATISTHIS(uint32_t w1, uint32_t w2)
{
}

static uint32_t setaddr;
static void MP3ADDY(uint32_t w1, uint32_t w2)
{
    setaddr = (w2 & 0xffffff);
}

static void DISABLE(uint32_t w1, uint32_t w2)
{
}


static const acmd_callback_t ABI3[0x10] = {
    DISABLE , ADPCM3 , CLEARBUFF3,  ENVMIXER3  , LOADBUFF3, RESAMPLE3  , SAVEBUFF3, MP3,
    MP3ADDY, SETVOL3, DMEMMOVE3 , LOADADPCM3 , MIXER3   , INTERLEAVE3, WHATISTHIS   , SETLOOP3
};


void alist_process_naudio(void)
{
    alist_process(ABI3, 0x10);
}

void alist_process_naudio_bk(void)
{
    alist_process(ABI3, 0x10);
}

void alist_process_naudio_dk(void)
{
    alist_process(ABI3, 0x10);
}

void alist_process_naudio_mp3(void)
{
    alist_process(ABI3, 0x10);
}

void alist_process_naudio_cbfd(void)
{
    alist_process(ABI3, 0x10);
}
