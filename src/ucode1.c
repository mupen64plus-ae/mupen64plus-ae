/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - ucode1.c                                        *
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

/* alist audio state */
static struct {
    /* main buffers */
    uint16_t in;
    uint16_t out;
    uint16_t count;

    /* auxiliary buffers */
    uint16_t dry_right;
    uint16_t wet_left;
    uint16_t wet_right;

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


/* audio commands definition */
static void SPNOOP(uint32_t w1, uint32_t w2)
{
}

static void CLEARBUFF(uint32_t w1, uint32_t w2)
{
    uint16_t dmem  = w1;
    uint16_t count = w2;

    alist_clear(dmem & ~3, (count + 3) & ~3);
}

static void ENVMIXER(uint32_t w1, uint32_t w2)
{
    uint8_t  flags   = (w1 >> 16);
    uint32_t address = (w2 & 0xffffff);

    alist_envmix_exp(
            flags & A_INIT,
            flags & A_AUX,
            l_alist.out, l_alist.dry_right,
            l_alist.wet_left, l_alist.wet_right,
            l_alist.in, l_alist.count,
            l_alist.dry, l_alist.wet,
            l_alist.vol,
            l_alist.target,
            l_alist.rate,
            address);
}

static void RESAMPLE(uint32_t w1, uint32_t w2)
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

static void SETVOL(uint32_t w1, uint32_t w2)
{
    uint8_t flags = (w1 >> 16);

    if (flags & A_AUX) {
        l_alist.dry = w1;
        l_alist.wet = w2;
    }
    else {
        unsigned lr = (flags & A_LEFT) ? 0 : 1;

        if (flags & A_VOL)
            l_alist.vol[lr] = w1;
        else {
            l_alist.target[lr] = w1;
            l_alist.rate[lr]   = w2;
        }
    }
}

static void SETLOOP(uint32_t w1, uint32_t w2)
{
    l_alist.loop = (w2 & 0xffffff);
}

static void ADPCM(uint32_t w1, uint32_t w2)
{
    uint8_t  flags   = (w1 >> 16);
    uint32_t address = (w2 & 0xffffff);

    alist_adpcm(
            flags & 0x1,
            flags & 0x2,
            false,          /* unsupported in this ucode */
            l_alist.out,
            l_alist.in,
            (l_alist.count + 0x1f) & ~0x1f,
            l_alist.table,
            l_alist.loop,
            address);
}

static void LOADBUFF(uint32_t w1, uint32_t w2)
{
    uint32_t address = (w2 & 0xffffff);

    if (l_alist.count == 0)
        return;

    alist_load(l_alist.in & ~3, address & ~3, (l_alist.count + 3) & ~3);
}

static void SAVEBUFF(uint32_t w1, uint32_t w2)
{
    uint32_t address = (w2 & 0xffffff);

    if (l_alist.count == 0)
        return;

    alist_save(l_alist.out & ~3, address & ~3, (l_alist.count + 3) & ~3);
}

static void SETBUFF(uint32_t w1, uint32_t w2)
{
    uint8_t flags = (w1 >> 16);

    if (flags & A_AUX) {
        l_alist.dry_right = w1;
        l_alist.wet_left  = (w2 >> 16);
        l_alist.wet_right = w2;
    } else {
        l_alist.in    = w1;
        l_alist.out   = (w2 >> 16);
        l_alist.count = w2;
    }
}

static void DMEMMOVE(uint32_t w1, uint32_t w2)
{
    uint16_t dmemi = w1;
    uint16_t dmemo = (w2 >> 16);
    uint16_t count = w2;

    if (count == 0)
        return;

    alist_move(dmemo, dmemi, (count + 3) & ~3);
}

static void LOADADPCM(uint32_t w1, uint32_t w2)
{
    uint16_t count   = (w1 & 0xffff);
    uint32_t address = (w2 & 0xffffff);

    dram_load_u16((uint16_t*)l_alist.table, address, count >> 1);
}

static void INTERLEAVE(uint32_t w1, uint32_t w2)
{
    uint16_t left  = (w2 >> 16);
    uint16_t right = w2;

    if (l_alist.count == 0)
        return;

    alist_interleave(l_alist.out, left, right, l_alist.count);
}

static void MIXER(uint32_t w1, uint32_t w2)
{
    int16_t  gain  = w1;
    uint16_t dmemi = (w2 >> 16);
    uint16_t dmemo = w2;

    if (l_alist.count == 0)
        return;

    alist_mix(dmemo, dmemi, l_alist.count, gain);
}

static void SEGMENT(uint32_t w1, uint32_t w2)
{
    /* TODO */
}

static void POLEF(uint32_t w1, uint32_t w2)
{
    uint8_t  flags   = (w1 >> 16);
    uint16_t gain    = w1;
    uint32_t address = (w2 & 0xffffff);

    if (l_alist.count == 0)
        return;

    alist_polef(
            flags & A_INIT,
            l_alist.out,
            l_alist.in,
            l_alist.count,
            gain,
            l_alist.table,
            address);
}

/* global functions */
void alist_process_audio(void)
{
    static const acmd_callback_t ABI[0x10] = {
        SPNOOP,         ADPCM ,         CLEARBUFF,      ENVMIXER,
        LOADBUFF,       RESAMPLE,       SAVEBUFF,       SEGMENT,
        SETBUFF,        SETVOL,         DMEMMOVE,       LOADADPCM,
        MIXER,          INTERLEAVE,     POLEF,          SETLOOP
    };

    alist_process(ABI, 0x10);
}

void alist_process_audio_ge(void)
{
    /* TODO: see what differs from alist_process_audio */
    static const acmd_callback_t ABI[0x10] = {
        SPNOOP,         ADPCM ,         CLEARBUFF,      ENVMIXER,
        LOADBUFF,       RESAMPLE,       SAVEBUFF,       SEGMENT,
        SETBUFF,        SETVOL,         DMEMMOVE,       LOADADPCM,
        MIXER,          INTERLEAVE,     POLEF,          SETLOOP
    };

    alist_process(ABI, 0x10);
}

void alist_process_audio_bc(void)
{
    /* TODO: see what differs from alist_process_audio */
    static const acmd_callback_t ABI[0x10] = {
        SPNOOP,         ADPCM ,         CLEARBUFF,      ENVMIXER,
        LOADBUFF,       RESAMPLE,       SAVEBUFF,       SEGMENT,
        SETBUFF,        SETVOL,         DMEMMOVE,       LOADADPCM,
        MIXER,          INTERLEAVE,     POLEF,          SETLOOP
    };

    alist_process(ABI, 0x10);
}
