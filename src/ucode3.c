/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - ucode3.c                                        *
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

# include <string.h>
#include <stdint.h>

#include "m64p_plugin.h"
#include "hle.h"
#include "alist_internal.h"

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
    uint16_t table[16 * 8];
} l_alist;

static void SETVOL3(uint32_t inst1, uint32_t inst2)
{
    uint8_t Flags = (uint8_t)(inst1 >> 0x10);
    if (Flags & 0x4) { /* 288 */
        if (Flags & 0x2) { /* 290 */
            l_alist.vol[0]  = (int16_t)inst1; /* 0x50 */
            l_alist.dry   = (int16_t)(inst2 >> 0x10); /* 0x4E */
            l_alist.wet   = (int16_t)inst2; /* 0x4C */
        } else {
            l_alist.target[1]  = (int16_t)inst1; /* 0x46 */
            l_alist.rate[1] = (int32_t)inst2; /* 0x48/0x4A */
        }
    } else {
        l_alist.target[0]  = (int16_t)inst1; /* 0x40 */
        l_alist.rate[0] = (int32_t)inst2; /* 0x42/0x44 */
    }
}

static void ENVMIXER3(uint32_t inst1, uint32_t inst2)
{
    uint8_t flags = (uint8_t)((inst1 >> 16) & 0xff);
    uint32_t addy = (inst2 & 0xFFFFFF);

    short *inp = (short *)(BufferSpace + 0x4F0);
    short *out = (short *)(BufferSpace + 0x9D0);
    short *aux1 = (short *)(BufferSpace + 0xB40);
    short *aux2 = (short *)(BufferSpace + 0xCB0);
    short *aux3 = (short *)(BufferSpace + 0xE20);
    int32_t MainR;
    int32_t MainL;
    int32_t AuxR;
    int32_t AuxL;
    int i1, o1, a1, a2, a3;
    short zero[8];
    int y;

    int32_t LAdder, LAcc, LVol;
    int32_t RAdder, RAcc, RVol;
    /* Most significant part of the Ramp Value */
    int16_t RSig, LSig;
    int16_t Wet, Dry;
    int16_t LTrg, RTrg;
    short save_buffer[40];

    memset(zero, 0, sizeof(zero));

    l_alist.vol[1] = (int16_t)inst1;

    if (flags & A_INIT) {
        LAdder = l_alist.rate[0] / 8;
        LAcc  = 0;
        LVol  = l_alist.vol[0];
        LSig = (int16_t)(l_alist.rate[0] >> 16);

        RAdder = l_alist.rate[1] / 8;
        RAcc  = 0;
        RVol  = l_alist.vol[1];
        RSig = (int16_t)(l_alist.rate[1] >> 16);

        /* Save Wet/Dry values */
        Wet = (int16_t)l_alist.wet;
        Dry = (int16_t)l_alist.dry;
        /* Save Current Left/Right Targets */
        LTrg = l_alist.target[0];
        RTrg = l_alist.target[1];
    } else {
        memcpy((uint8_t *)save_buffer, rsp.RDRAM + addy, 80);
        Wet    = *(int16_t *)(save_buffer +  0); /* 0-1 */
        Dry    = *(int16_t *)(save_buffer +  2); /* 2-3 */
        LTrg   = *(int16_t *)(save_buffer +  4); /* 4-5 */
        RTrg   = *(int16_t *)(save_buffer +  6); /* 6-7 */
        LAdder = *(int32_t *)(save_buffer +  8); /* 8-9 (save_buffer is a 16bit pointer) */
        RAdder = *(int32_t *)(save_buffer + 10); /* 10-11 */
        LAcc   = *(int32_t *)(save_buffer + 12); /* 12-13 */
        RAcc   = *(int32_t *)(save_buffer + 14); /* 14-15 */
        LVol   = *(int32_t *)(save_buffer + 16); /* 16-17 */
        RVol   = *(int32_t *)(save_buffer + 18); /* 18-19 */
        LSig   = *(int16_t *)(save_buffer + 20); /* 20-21 */
        RSig   = *(int16_t *)(save_buffer + 22); /* 22-23 */
    }

    for (y = 0; y < (0x170 / 2); y++) {

        /* Left */
        LAcc += LAdder;
        LVol += (LAcc >> 16);
        LAcc &= 0xFFFF;

        /* Right */
        RAcc += RAdder;
        RVol += (RAcc >> 16);
        RAcc &= 0xFFFF;
/****************************************************************/
        /* Clamp Left */
        if (LSig >= 0) { /* VLT */
            if (LVol > LTrg)
                LVol = LTrg;
        } else { /* VGE */
            if (LVol < LTrg)
                LVol = LTrg;
        }

        /* Clamp Right */
        if (RSig >= 0) { /* VLT */
            if (RVol > RTrg)
                RVol = RTrg;
        } else { /* VGE */
            if (RVol < RTrg)
                RVol = RTrg;
        }
/****************************************************************/
        MainL = ((Dry * LVol) + 0x4000) >> 15;
        MainR = ((Dry * RVol) + 0x4000) >> 15;

        o1 = out [y ^ S];
        a1 = aux1[y ^ S];
        i1 = inp [y ^ S];

        o1 += ((i1 * MainL) + 0x4000) >> 15;
        a1 += ((i1 * MainR) + 0x4000) >> 15;

/****************************************************************/
        o1 = clamp_s16(o1);
        a1 = clamp_s16(a1);

/****************************************************************/

        out[y ^ S] = o1;
        aux1[y ^ S] = a1;

/****************************************************************/
        a2 = aux2[y ^ S];
        a3 = aux3[y ^ S];

        AuxL  = ((Wet * LVol) + 0x4000) >> 15;
        AuxR  = ((Wet * RVol) + 0x4000) >> 15;

        a2 += ((i1 * AuxL) + 0x4000) >> 15;
        a3 += ((i1 * AuxR) + 0x4000) >> 15;

        a2 = clamp_s16(a2);
        a3 = clamp_s16(a3);

        aux2[y ^ S] = a2;
        aux3[y ^ S] = a3;
    }

    *(int16_t *)(save_buffer +  0) = Wet; /* 0-1 */
    *(int16_t *)(save_buffer +  2) = Dry; /* 2-3 */
    *(int16_t *)(save_buffer +  4) = LTrg; /* 4-5 */
    *(int16_t *)(save_buffer +  6) = RTrg; /* 6-7 */
    *(int32_t *)(save_buffer +  8) = LAdder; /* 8-9 (save_buffer is a 16bit pointer) */
    *(int32_t *)(save_buffer + 10) = RAdder; /* 10-11 */
    *(int32_t *)(save_buffer + 12) = LAcc; /* 12-13 */
    *(int32_t *)(save_buffer + 14) = RAcc; /* 14-15 */
    *(int32_t *)(save_buffer + 16) = LVol; /* 16-17 */
    *(int32_t *)(save_buffer + 18) = RVol; /* 18-19 */
    *(int16_t *)(save_buffer + 20) = LSig; /* 20-21 */
    *(int16_t *)(save_buffer + 22) = RSig; /* 22-23 */
    memcpy(rsp.RDRAM + addy, (uint8_t *)save_buffer, 80);
}

static void CLEARBUFF3(uint32_t inst1, uint32_t inst2)
{
    uint16_t addr = (uint16_t)(inst1 & 0xffff);
    uint16_t count = (uint16_t)(inst2 & 0xffff);
    memset(BufferSpace + addr + 0x4f0, 0, count);
}

/* TODO Needs accuracy verification... */
static void MIXER3(uint32_t inst1, uint32_t inst2)
{
    uint16_t dmemin  = (uint16_t)(inst2 >> 0x10)  + 0x4f0;
    uint16_t dmemout = (uint16_t)(inst2 & 0xFFFF) + 0x4f0;
    int32_t gain    = (int16_t)(inst1 & 0xFFFF);
    int32_t temp;
    int x;

    for (x = 0; x < 0x170; x += 2) {
        /* TODO I think I can do this a lot easier */
        temp = (*(int16_t *)(BufferSpace + dmemin + x) * gain) >> 15;
        temp += *(int16_t *)(BufferSpace + dmemout + x);

        temp = clamp_s16((int32_t)temp);

        *(uint16_t *)(BufferSpace + dmemout + x) = (uint16_t)(temp & 0xFFFF);
    }
}

static void LOADBUFF3(uint32_t inst1, uint32_t inst2)
{
    uint32_t v0 = (inst2 & 0xfffffc);
    uint32_t cnt = (((inst1 >> 0xC) + 3) & 0xFFC);
    uint32_t src = (inst1 & 0xffc) + 0x4f0;
    memcpy(BufferSpace + src, rsp.RDRAM + v0, cnt);
}

static void SAVEBUFF3(uint32_t inst1, uint32_t inst2)
{
    uint32_t v0 = (inst2 & 0xfffffc);
    uint32_t cnt = (((inst1 >> 0xC) + 3) & 0xFFC);
    uint32_t src = (inst1 & 0xffc) + 0x4f0;
    memcpy(rsp.RDRAM + v0, BufferSpace + src, cnt);
}

/* Loads an ADPCM table
 * NOTE Works 100% Now 03-13-01
 */
static void LOADADPCM3(uint32_t inst1, uint32_t inst2)
{
    uint32_t v0 = (inst2 & 0xffffff);
    uint32_t x;

    uint16_t *table = (uint16_t *)(rsp.RDRAM + v0);
    for (x = 0; x < ((inst1 & 0xffff) >> 0x4); x++) {
        l_alist.table[(0x0 + (x << 3))^S] = table[0];
        l_alist.table[(0x1 + (x << 3))^S] = table[1];

        l_alist.table[(0x2 + (x << 3))^S] = table[2];
        l_alist.table[(0x3 + (x << 3))^S] = table[3];

        l_alist.table[(0x4 + (x << 3))^S] = table[4];
        l_alist.table[(0x5 + (x << 3))^S] = table[5];

        l_alist.table[(0x6 + (x << 3))^S] = table[6];
        l_alist.table[(0x7 + (x << 3))^S] = table[7];
        table += 8;
    }
}

/* TODO Needs accuracy verification... */
static void DMEMMOVE3(uint32_t inst1, uint32_t inst2)
{
    uint32_t cnt;
    uint32_t v0 = (inst1 & 0xFFFF) + 0x4f0;
    uint32_t v1 = (inst2 >> 0x10) + 0x4f0;
    uint32_t count = ((inst2 + 3) & 0xfffc);

    for (cnt = 0; cnt < count; cnt++)
        *(uint8_t *)(BufferSpace + ((cnt + v1)^S8)) = *(uint8_t *)(BufferSpace + ((cnt + v0)^S8));
}

static void SETLOOP3(uint32_t inst1, uint32_t inst2)
{
    l_alist.loop = (inst2 & 0xffffff);
}

/* TODO Verified to be 100% Accurate... */
static void ADPCM3(uint32_t inst1, uint32_t inst2)
{
    unsigned char Flags = (uint8_t)(inst2 >> 0x1c) & 0xff;
    unsigned int Address = (inst1 & 0xffffff);
    unsigned short inPtr = (inst2 >> 12) & 0xf;
    short *out = (short *)(BufferSpace + (inst2 & 0xfff) + 0x4f0);
    short count = (short)((inst2 >> 16) & 0xfff);
    unsigned char icode;
    unsigned char code;
    int vscale;
    unsigned short index;
    unsigned short j;
    int a[8];
    short *book1, *book2;
    int l1;
    int l2;
    int inp1[8];
    int inp2[8];

    memset(out, 0, 32);

    if (!(Flags & 0x1)) {
        if (Flags & 0x2)
            memcpy(out, &rsp.RDRAM[l_alist.loop], 32);
        else
            memcpy(out, &rsp.RDRAM[Address], 32);
    }

    l1 = out[14 ^ S];
    l2 = out[15 ^ S];
    out += 16;
    while (count > 0) {
        /* the first interation through, these values are
         * either 0 in the case of A_INIT, from a special
         * area of memory in the case of A_LOOP or just
         * the values we calculated the last time
         */

        code = BufferSpace[(0x4f0 + inPtr)^S8];
        index = code & 0xf;
        /* index into the adpcm code table */
        index <<= 4;
        book1 = (short *)&l_alist.table[index];
        book2 = book1 + 8;
        /* upper nibble is scale */
        code >>= 4;
        /* very strange. 0x8000 would be .5 in 16:16 format
         * so this appears to be a fractional scale based
         * on the 12 based inverse of the scale value.  note
         * that this could be negative, in which case we do
         * not use the calculated vscale value... see the
         * if(code>12) check below
         */
        vscale = (0x8000 >> ((12 - code) - 1));

        /* coded adpcm data lies next */
        inPtr++;
        j = 0;
        /* loop of 8, for 8 coded nibbles from 4 bytes
         * which yields 8 short pcm values
         */
        while (j < 8) {
            icode = BufferSpace[(0x4f0 + inPtr)^S8];
            inPtr++;

            /* this will in effect be signed */
            inp1[j] = (int16_t)((icode & 0xf0) << 8);
            if (code < 12)
                inp1[j] = ((int)((int)inp1[j] * (int)vscale) >> 16);
            j++;

            inp1[j] = (int16_t)((icode & 0xf) << 12);
            if (code < 12)
                inp1[j] = ((int)((int)inp1[j] * (int)vscale) >> 16);
            j++;
        }
        j = 0;
        while (j < 8) {
            icode = BufferSpace[(0x4f0 + inPtr)^S8];
            inPtr++;

            /* this will in effect be signed */
            inp2[j] = (short)((icode & 0xf0) << 8);
            if (code < 12)
                inp2[j] = ((int)((int)inp2[j] * (int)vscale) >> 16);
            j++;

            inp2[j] = (short)((icode & 0xf) << 12);
            if (code < 12)
                inp2[j] = ((int)((int)inp2[j] * (int)vscale) >> 16);
            j++;
        }

        a[0] = (int)book1[0] * (int)l1;
        a[0] += (int)book2[0] * (int)l2;
        a[0] += (int)inp1[0] * (int)2048;

        a[1] = (int)book1[1] * (int)l1;
        a[1] += (int)book2[1] * (int)l2;
        a[1] += (int)book2[0] * inp1[0];
        a[1] += (int)inp1[1] * (int)2048;

        a[2] = (int)book1[2] * (int)l1;
        a[2] += (int)book2[2] * (int)l2;
        a[2] += (int)book2[1] * inp1[0];
        a[2] += (int)book2[0] * inp1[1];
        a[2] += (int)inp1[2] * (int)2048;

        a[3] = (int)book1[3] * (int)l1;
        a[3] += (int)book2[3] * (int)l2;
        a[3] += (int)book2[2] * inp1[0];
        a[3] += (int)book2[1] * inp1[1];
        a[3] += (int)book2[0] * inp1[2];
        a[3] += (int)inp1[3] * (int)2048;

        a[4] = (int)book1[4] * (int)l1;
        a[4] += (int)book2[4] * (int)l2;
        a[4] += (int)book2[3] * inp1[0];
        a[4] += (int)book2[2] * inp1[1];
        a[4] += (int)book2[1] * inp1[2];
        a[4] += (int)book2[0] * inp1[3];
        a[4] += (int)inp1[4] * (int)2048;

        a[5] = (int)book1[5] * (int)l1;
        a[5] += (int)book2[5] * (int)l2;
        a[5] += (int)book2[4] * inp1[0];
        a[5] += (int)book2[3] * inp1[1];
        a[5] += (int)book2[2] * inp1[2];
        a[5] += (int)book2[1] * inp1[3];
        a[5] += (int)book2[0] * inp1[4];
        a[5] += (int)inp1[5] * (int)2048;

        a[6] = (int)book1[6] * (int)l1;
        a[6] += (int)book2[6] * (int)l2;
        a[6] += (int)book2[5] * inp1[0];
        a[6] += (int)book2[4] * inp1[1];
        a[6] += (int)book2[3] * inp1[2];
        a[6] += (int)book2[2] * inp1[3];
        a[6] += (int)book2[1] * inp1[4];
        a[6] += (int)book2[0] * inp1[5];
        a[6] += (int)inp1[6] * (int)2048;

        a[7] = (int)book1[7] * (int)l1;
        a[7] += (int)book2[7] * (int)l2;
        a[7] += (int)book2[6] * inp1[0];
        a[7] += (int)book2[5] * inp1[1];
        a[7] += (int)book2[4] * inp1[2];
        a[7] += (int)book2[3] * inp1[3];
        a[7] += (int)book2[2] * inp1[4];
        a[7] += (int)book2[1] * inp1[5];
        a[7] += (int)book2[0] * inp1[6];
        a[7] += (int)inp1[7] * (int)2048;

        for (j = 0; j < 8; j++) {
            a[j ^ S] >>= 11;
            a[j ^ S] = clamp_s16(a[j ^ S]);
            *(out++) = a[j ^ S];
        }
        l1 = a[6];
        l2 = a[7];

        a[0] = (int)book1[0] * (int)l1;
        a[0] += (int)book2[0] * (int)l2;
        a[0] += (int)inp2[0] * (int)2048;

        a[1] = (int)book1[1] * (int)l1;
        a[1] += (int)book2[1] * (int)l2;
        a[1] += (int)book2[0] * inp2[0];
        a[1] += (int)inp2[1] * (int)2048;

        a[2] = (int)book1[2] * (int)l1;
        a[2] += (int)book2[2] * (int)l2;
        a[2] += (int)book2[1] * inp2[0];
        a[2] += (int)book2[0] * inp2[1];
        a[2] += (int)inp2[2] * (int)2048;

        a[3] = (int)book1[3] * (int)l1;
        a[3] += (int)book2[3] * (int)l2;
        a[3] += (int)book2[2] * inp2[0];
        a[3] += (int)book2[1] * inp2[1];
        a[3] += (int)book2[0] * inp2[2];
        a[3] += (int)inp2[3] * (int)2048;

        a[4] = (int)book1[4] * (int)l1;
        a[4] += (int)book2[4] * (int)l2;
        a[4] += (int)book2[3] * inp2[0];
        a[4] += (int)book2[2] * inp2[1];
        a[4] += (int)book2[1] * inp2[2];
        a[4] += (int)book2[0] * inp2[3];
        a[4] += (int)inp2[4] * (int)2048;

        a[5] = (int)book1[5] * (int)l1;
        a[5] += (int)book2[5] * (int)l2;
        a[5] += (int)book2[4] * inp2[0];
        a[5] += (int)book2[3] * inp2[1];
        a[5] += (int)book2[2] * inp2[2];
        a[5] += (int)book2[1] * inp2[3];
        a[5] += (int)book2[0] * inp2[4];
        a[5] += (int)inp2[5] * (int)2048;

        a[6] = (int)book1[6] * (int)l1;
        a[6] += (int)book2[6] * (int)l2;
        a[6] += (int)book2[5] * inp2[0];
        a[6] += (int)book2[4] * inp2[1];
        a[6] += (int)book2[3] * inp2[2];
        a[6] += (int)book2[2] * inp2[3];
        a[6] += (int)book2[1] * inp2[4];
        a[6] += (int)book2[0] * inp2[5];
        a[6] += (int)inp2[6] * (int)2048;

        a[7] = (int)book1[7] * (int)l1;
        a[7] += (int)book2[7] * (int)l2;
        a[7] += (int)book2[6] * inp2[0];
        a[7] += (int)book2[5] * inp2[1];
        a[7] += (int)book2[4] * inp2[2];
        a[7] += (int)book2[3] * inp2[3];
        a[7] += (int)book2[2] * inp2[4];
        a[7] += (int)book2[1] * inp2[5];
        a[7] += (int)book2[0] * inp2[6];
        a[7] += (int)inp2[7] * (int)2048;

        for (j = 0; j < 8; j++) {
            a[j ^ S] >>= 11;
            a[j ^ S] = clamp_s16(a[j ^ S]);
            *(out++) = a[j ^ S];
        }
        l1 = a[6];
        l2 = a[7];

        count -= 32;
    }
    out -= 16;
    memcpy(&rsp.RDRAM[Address], out, 32);
}

static void RESAMPLE3(uint32_t inst1, uint32_t inst2)
{
    unsigned char Flags = (uint8_t)((inst2 >> 0x1e));
    unsigned int Pitch = ((inst2 >> 0xe) & 0xffff) << 1;
    uint32_t addy = (inst1 & 0xffffff);
    unsigned int Accum = 0;
    unsigned int location;
    int16_t *lut;
    short *dst;
    int16_t *src;
    uint32_t srcPtr = ((((inst2 >> 2) & 0xfff) + 0x4f0) / 2);
    uint32_t dstPtr;
    int32_t temp;
    int32_t accum;
    int x, i;

    dst = (short *)(BufferSpace);
    src = (int16_t *)(BufferSpace);

    srcPtr -= 4;

    if (inst2 & 0x3)
        dstPtr = 0x660 / 2;
    else
        dstPtr = 0x4f0 / 2;

    if ((Flags & 0x1) == 0) {
        for (x = 0; x < 4; x++)
            src[(srcPtr + x)^S] = ((uint16_t *)rsp.RDRAM)[((addy / 2) + x)^S];
        Accum = *(uint16_t *)(rsp.RDRAM + addy + 10);
    } else {
        for (x = 0; x < 4; x++)
            src[(srcPtr + x)^S] = 0;
    }

    for (i = 0; i < 0x170 / 2; i++)    {
        location = (((Accum * 0x40) >> 0x10) * 8);
        lut = (int16_t *)(((uint8_t *)ResampleLUT) + location);

        temp = ((int32_t) * (int16_t *)(src + ((srcPtr + 0)^S)) * ((int32_t)((int16_t)lut[0])));
        accum = (int32_t)(temp >> 15);

        temp = ((int32_t) * (int16_t *)(src + ((srcPtr + 1)^S)) * ((int32_t)((int16_t)lut[1])));
        accum += (int32_t)(temp >> 15);

        temp = ((int32_t) * (int16_t *)(src + ((srcPtr + 2)^S)) * ((int32_t)((int16_t)lut[2])));
        accum += (int32_t)(temp >> 15);

        temp = ((int32_t) * (int16_t *)(src + ((srcPtr + 3)^S)) * ((int32_t)((int16_t)lut[3])));
        accum += (int32_t)(temp >> 15);

        accum = clamp_s16(accum);

        dst[dstPtr ^ S] = (accum);
        dstPtr++;
        Accum += Pitch;
        srcPtr += (Accum >> 16);
        Accum &= 0xffff;
    }
    for (x = 0; x < 4; x++)
        ((uint16_t *)rsp.RDRAM)[((addy / 2) + x)^S] = src[(srcPtr + x)^S];
    *(uint16_t *)(rsp.RDRAM + addy + 10) = Accum;
}

/* TODO Needs accuracy verification... */
static void INTERLEAVE3(uint32_t inst1, uint32_t inst2)
{
    uint16_t *outbuff = (uint16_t *)(BufferSpace + 0x4f0);
    uint16_t *inSrcR;
    uint16_t *inSrcL;
    uint16_t Left, Right, Left2, Right2;
    int x;

    inSrcR = (uint16_t *)(BufferSpace + 0xb40);
    inSrcL = (uint16_t *)(BufferSpace + 0x9d0);

    for (x = 0; x < (0x170 / 4); x++) {
        Left = *(inSrcL++);
        Right = *(inSrcR++);
        Left2 = *(inSrcL++);
        Right2 = *(inSrcR++);

#ifdef M64P_BIG_ENDIAN
        *(outbuff++) = Right;
        *(outbuff++) = Left;
        *(outbuff++) = Right2;
        *(outbuff++) = Left2;
#else
        *(outbuff++) = Right2;
        *(outbuff++) = Left2;
        *(outbuff++) = Right;
        *(outbuff++) = Left;
#endif
    }
}

static void WHATISTHIS(uint32_t inst1, uint32_t inst2)
{
}

static uint32_t setaddr;
static void MP3ADDY(uint32_t inst1, uint32_t inst2)
{
    setaddr = (inst2 & 0xffffff);
}

/*
FFT = Fast Fourier Transform
DCT = Discrete Cosine Transform
MPEG-1 Layer 3 retains Layer 2's 1152-sample window, as well as the FFT polyphase filter for
backward compatibility, but adds a modified DCT filter. DCT's advantages over DFTs (discrete
Fourier transforms) include half as many multiply-accumulate operations and half the
generated coefficients because the sinusoidal portion of the calculation is absent, and DCT
generally involves simpler math. The finite lengths of a conventional DCTs' bandpass impulse
responses, however, may result in block-boundary effects. MDCTs overlap the analysis blocks
and lowpass-filter the decoded audio to remove aliases, eliminating these effects. MDCTs also
have a higher transform coding gain than the standard DCT, and their basic functions
correspond to better bandpass response.

MPEG-1 Layer 3's DCT sub-bands are unequally sized, and correspond to the human auditory
system's critical bands. In Layer 3 decoders must support both constant- and variable-bit-rate
bit streams. (However, many Layer 1 and 2 decoders also handle variable bit rates). Finally,
Layer 3 encoders Huffman-code the quantized coefficients before archiving or transmission for
additional lossless compression. Bit streams range from 32 to 320 kbps, and 128-kbps rates
achieve near-CD quality, an important specification to enable dual-channel ISDN
(integrated-services-digital-network) to be the future high-bandwidth pipe to the home.

*/
static void DISABLE(uint32_t inst1, uint32_t inst2)
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
