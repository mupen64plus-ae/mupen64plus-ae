/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - ucode1.c                                        *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
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
    uint8_t flags = (uint8_t)((w1 >> 16) & 0xff);
    uint32_t addy = (w2 & 0xFFFFFF);
    short *inp = (short *)(BufferSpace + l_alist.in);
    short *out = (short *)(BufferSpace + l_alist.out);
    short *aux1 = (short *)(BufferSpace + l_alist.dry_right);
    short *aux2 = (short *)(BufferSpace + l_alist.wet_left);
    short *aux3 = (short *)(BufferSpace + l_alist.wet_right);
    int32_t MainR;
    int32_t MainL;
    int32_t AuxR;
    int32_t AuxL;
    int i1, o1, a1, a2 = 0, a3 = 0;
    unsigned short AuxIncRate = 1;
    short zero[8];
    int32_t LVol, RVol;
    int32_t LAcc, RAcc;
    int32_t LTrg, RTrg;
    int16_t Wet, Dry;
    uint32_t ptr = 0;
    int32_t RRamp, LRamp;
    int32_t LAdderStart, RAdderStart, LAdderEnd, RAdderEnd;
    int32_t oMainR, oMainL, oAuxR, oAuxL;
    int x, y;
    short save_buffer[40];

    memset(zero, 0, sizeof(zero));

    if (flags & A_INIT) {
        LVol = ((l_alist.vol[0] * (int32_t)l_alist.rate[0]));
        RVol = ((l_alist.vol[1] * (int32_t)l_alist.rate[1]));
        Wet = (int16_t)l_alist.wet;
        /* Save Wet/Dry values */
        Dry = (int16_t)l_alist.dry;
        /* Save Current Left/Right Targets */
        LTrg = (l_alist.target[0] << 16);
        RTrg = (l_alist.target[1] << 16);
        LAdderStart = l_alist.vol[0] << 16;
        RAdderStart = l_alist.vol[1] << 16;
        LAdderEnd = LVol;
        RAdderEnd = RVol;
        RRamp = l_alist.rate[1];
        LRamp = l_alist.rate[0];
    } else {
        /* Load LVol, RVol, LAcc, and RAcc (all 32bit)
         * Load Wet, Dry, LTrg, RTrg
         */
        memcpy((uint8_t *)save_buffer, (rsp.RDRAM + addy), 80);
        Wet  = *(int16_t *)(save_buffer +  0); /* 0-1 */
        Dry  = *(int16_t *)(save_buffer +  2); /* 2-3 */
        LTrg = *(int32_t *)(save_buffer +  4); /* 4-5 */
        RTrg = *(int32_t *)(save_buffer +  6); /* 6-7 */
        LRamp = *(int32_t *)(save_buffer +  8); /* 8-9 (save_buffer is a 16bit pointer) */
        RRamp = *(int32_t *)(save_buffer + 10); /* 10-11 */
        LAdderEnd = *(int32_t *)(save_buffer + 12); /* 12-13 */
        RAdderEnd = *(int32_t *)(save_buffer + 14); /* 14-15 */
        LAdderStart = *(int32_t *)(save_buffer + 16); /* 12-13 */
        RAdderStart = *(int32_t *)(save_buffer + 18); /* 14-15 */
    }

    if (!(flags & A_AUX)) {
        AuxIncRate = 0;
        aux2 = aux3 = zero;
    }

    oMainL = (Dry * (LTrg >> 16) + 0x4000) >> 15;
    oAuxL  = (Wet * (LTrg >> 16) + 0x4000)  >> 15;
    oMainR = (Dry * (RTrg >> 16) + 0x4000) >> 15;
    oAuxR  = (Wet * (RTrg >> 16) + 0x4000)  >> 15;

    for (y = 0; y < l_alist.count; y += 0x10) {

        if (LAdderStart != LTrg) {
            LAcc = LAdderStart;
            LVol = (LAdderEnd - LAdderStart) >> 3;
            LAdderEnd   = (int32_t)(((int64_t)LAdderEnd * (int64_t)LRamp) >> 16);
            LAdderStart = (int32_t)(((int64_t)LAcc * (int64_t)LRamp) >> 16);
        } else {
            LAcc = LTrg;
            LVol = 0;
        }

        if (RAdderStart != RTrg) {
            RAcc = RAdderStart;
            RVol = (RAdderEnd - RAdderStart) >> 3;
            RAdderEnd   = (int32_t)(((int64_t)RAdderEnd * (int64_t)RRamp) >> 16);
            RAdderStart = (int32_t)(((int64_t)RAcc * (int64_t)RRamp) >> 16);
        } else {
            RAcc = RTrg;
            RVol = 0;
        }

        for (x = 0; x < 8; x++) {
            i1 = (int)inp[ptr ^ S];
            o1 = (int)out[ptr ^ S];
            a1 = (int)aux1[ptr ^ S];
            if (AuxIncRate) {
                a2 = (int)aux2[ptr ^ S];
                a3 = (int)aux3[ptr ^ S];
            }
            /* TODO: here...
             * LAcc = LTrg;
             * RAcc = RTrg;
             */

            LAcc += LVol;
            RAcc += RVol;

            if (LVol <= 0) {
                /* Decrementing */
                if (LAcc < LTrg) {
                    LAcc = LTrg;
                    LAdderStart = LTrg;
                    MainL = oMainL;
                    AuxL  = oAuxL;
                } else {
                    MainL = (Dry * ((int32_t)LAcc >> 16) + 0x4000) >> 15;
                    AuxL  = (Wet * ((int32_t)LAcc >> 16) + 0x4000)  >> 15;
                }
            } else {
                if (LAcc > LTrg) {
                    LAcc = LTrg;
                    LAdderStart = LTrg;
                    MainL = oMainL;
                    AuxL  = oAuxL;
                } else {
                    MainL = (Dry * ((int32_t)LAcc >> 16) + 0x4000) >> 15;
                    AuxL  = (Wet * ((int32_t)LAcc >> 16) + 0x4000)  >> 15;
                }
            }

            if (RVol <= 0) {
                /* Decrementing */
                if (RAcc < RTrg) {
                    RAcc = RTrg;
                    RAdderStart = RTrg;
                    MainR = oMainR;
                    AuxR  = oAuxR;
                } else {
                    MainR = (Dry * ((int32_t)RAcc >> 16) + 0x4000) >> 15;
                    AuxR  = (Wet * ((int32_t)RAcc >> 16) + 0x4000)  >> 15;
                }
            } else {
                if (RAcc > RTrg) {
                    RAcc = RTrg;
                    RAdderStart = RTrg;
                    MainR = oMainR;
                    AuxR  = oAuxR;
                } else {
                    MainR = (Dry * ((int32_t)RAcc >> 16) + 0x4000) >> 15;
                    AuxR  = (Wet * ((int32_t)RAcc >> 16) + 0x4000)  >> 15;
                }
            }

            o1 += ((i1 * MainR) + 0x4000) >> 15;
            a1 += ((i1 * MainL) + 0x4000) >> 15;

            o1 = clamp_s16(o1);
            a1 = clamp_s16(a1);

            out[ptr ^ S] = o1;
            aux1[ptr ^ S] = a1;
            if (AuxIncRate) {
                a2 += ((i1 * AuxR) + 0x4000) >> 15;
                a3 += ((i1 * AuxL) + 0x4000) >> 15;

                a2 = clamp_s16(a2);
                a3 = clamp_s16(a3);

                aux2[ptr ^ S] = a2;
                aux3[ptr ^ S] = a3;
            }
            ptr++;
        }
    }

    *(int16_t *)(save_buffer +  0) = Wet; /* 0-1 */
    *(int16_t *)(save_buffer +  2) = Dry; /* 2-3 */
    *(int32_t *)(save_buffer +  4) = LTrg; /* 4-5 */
    *(int32_t *)(save_buffer +  6) = RTrg; /* 6-7 */
    *(int32_t *)(save_buffer +  8) = LRamp; /* 8-9 (save_buffer is a 16bit pointer) */
    *(int32_t *)(save_buffer + 10) = RRamp; /* 10-11 */
    *(int32_t *)(save_buffer + 12) = LAdderEnd; /* 12-13 */
    *(int32_t *)(save_buffer + 14) = RAdderEnd; /* 14-15 */
    *(int32_t *)(save_buffer + 16) = LAdderStart; /* 12-13 */
    *(int32_t *)(save_buffer + 18) = RAdderStart; /* 14-15 */
    memcpy(rsp.RDRAM + addy, (uint8_t *)save_buffer, 80);
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

        if (flags & A_VOL) {
            l_alist.vol[lr] = w1;
            l_alist.vol[lr] = w2;
        }
        else {
            l_alist.target[lr] = w1;
            l_alist.rate[lr]   = w2;
        }
    }
}

static void UNKNOWN(uint32_t w1, uint32_t w2) {}

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

static const acmd_callback_t ABI1[0x10] = {
    SPNOOP , ADPCM , CLEARBUFF, ENVMIXER  , LOADBUFF, RESAMPLE  , SAVEBUFF, UNKNOWN,
    SETBUFF, SETVOL, DMEMMOVE , LOADADPCM , MIXER   , INTERLEAVE, UNKNOWN , SETLOOP
};

void alist_process_audio(void)
{
    alist_process(ABI1, 0x10);
}

void alist_process_audio_ge(void)
{
    alist_process(ABI1, 0x10);
}

void alist_process_audio_bc(void)
{
    alist_process(ABI1, 0x10);
}
