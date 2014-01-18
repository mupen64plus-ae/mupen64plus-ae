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

    /* ADPCM loop point address */
    uint32_t loop;

    /* storage for ADPCM table and polef coefficients */
    int16_t table[16 * 8];
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

static uint32_t t3, s5, s6;
static uint16_t env[8];

static void ENVSETUP1(uint32_t w1, uint32_t w2)
{
    uint32_t tmp;

    t3 = w1 & 0xFFFF;
    tmp = (w1 >> 0x8) & 0xFF00;
    env[4] = (uint16_t)tmp;
    tmp += t3;
    env[5] = (uint16_t)tmp;
    s5 = w2 >> 0x10;
    s6 = w2 & 0xFFFF;
}

static void ENVSETUP2(uint32_t w1, uint32_t w2)
{
    uint32_t tmp;

    tmp = (w2 >> 0x10);
    env[0] = (uint16_t)tmp;
    tmp += s5;
    env[1] = (uint16_t)tmp;
    tmp = w2 & 0xffff;
    env[2] = (uint16_t)tmp;
    tmp += s6;
    env[3] = (uint16_t)tmp;
}

static void ENVMIXER2(uint32_t w1, uint32_t w2)
{
    int16_t *bufft6, *bufft7, *buffs0, *buffs1;
    int16_t *buffs3;
    int32_t count;
    uint32_t adder;

    int16_t vec9, vec10;

    int16_t v2[8];

    buffs3 = (int16_t *)(BufferSpace + ((w1 >> 0x0c) & 0x0ff0));
    bufft6 = (int16_t *)(BufferSpace + ((w2 >> 0x14) & 0x0ff0));
    bufft7 = (int16_t *)(BufferSpace + ((w2 >> 0x0c) & 0x0ff0));
    buffs0 = (int16_t *)(BufferSpace + ((w2 >> 0x04) & 0x0ff0));
    buffs1 = (int16_t *)(BufferSpace + ((w2 << 0x04) & 0x0ff0));


    v2[0] = 0 - (int16_t)((w1 & 0x2) >> 1);
    v2[1] = 0 - (int16_t)((w1 & 0x1));
    v2[2] = 0 - (int16_t)((w1 & 0x8) >> 1);
    v2[3] = 0 - (int16_t)((w1 & 0x4) >> 1);

    count = (w1 >> 8) & 0xff;

    if (!isMKABI) {
        s5 *= 2;
        s6 *= 2;
        t3 *= 2;
        adder = 0x10;
    } else {
        w1 = 0;
        adder = 0x8;
        t3 = 0;
    }


    while (count > 0) {
        int temp, x;
        for (x = 0; x < 0x8; x++) {
            vec9  = (int16_t)(((int32_t)buffs3[x ^ S] * (uint32_t)env[0]) >> 0x10) ^ v2[0];
            vec10 = (int16_t)(((int32_t)buffs3[x ^ S] * (uint32_t)env[2]) >> 0x10) ^ v2[1];
            temp = bufft6[x ^ S] + vec9;
            temp = clamp_s16(temp);
            bufft6[x ^ S] = temp;
            temp = bufft7[x ^ S] + vec10;
            temp = clamp_s16(temp);
            bufft7[x ^ S] = temp;
            vec9  = (int16_t)(((int32_t)vec9  * (uint32_t)env[4]) >> 0x10) ^ v2[2];
            vec10 = (int16_t)(((int32_t)vec10 * (uint32_t)env[4]) >> 0x10) ^ v2[3];
            if (w1 & 0x10) {
                temp = buffs0[x ^ S] + vec10;
                temp = clamp_s16(temp);
                buffs0[x ^ S] = temp;
                temp = buffs1[x ^ S] + vec9;
                temp = clamp_s16(temp);
                buffs1[x ^ S] = temp;
            } else {
                temp = buffs0[x ^ S] + vec9;
                temp = clamp_s16(temp);
                buffs0[x ^ S] = temp;
                temp = buffs1[x ^ S] + vec10;
                temp = clamp_s16(temp);
                buffs1[x ^ S] = temp;
            }
        }

        if (!isMKABI)
            for (x = 0x8; x < 0x10; x++) {
                vec9  = (int16_t)(((int32_t)buffs3[x ^ S] * (uint32_t)env[1]) >> 0x10) ^ v2[0];
                vec10 = (int16_t)(((int32_t)buffs3[x ^ S] * (uint32_t)env[3]) >> 0x10) ^ v2[1];
                temp = bufft6[x ^ S] + vec9;
                temp = clamp_s16(temp);
                bufft6[x ^ S] = temp;
                temp = bufft7[x ^ S] + vec10;
                temp = clamp_s16(temp);
                bufft7[x ^ S] = temp;
                vec9  = (int16_t)(((int32_t)vec9  * (uint32_t)env[5]) >> 0x10) ^ v2[2];
                vec10 = (int16_t)(((int32_t)vec10 * (uint32_t)env[5]) >> 0x10) ^ v2[3];
                if (w1 & 0x10) {
                    temp = buffs0[x ^ S] + vec10;
                    temp = clamp_s16(temp);
                    buffs0[x ^ S] = temp;
                    temp = buffs1[x ^ S] + vec9;
                    temp = clamp_s16(temp);
                    buffs1[x ^ S] = temp;
                } else {
                    temp = buffs0[x ^ S] + vec9;
                    temp = clamp_s16(temp);
                    buffs0[x ^ S] = temp;
                    temp = buffs1[x ^ S] + vec10;
                    temp = clamp_s16(temp);
                    buffs1[x ^ S] = temp;
                }
            }
        bufft6 += adder;
        bufft7 += adder;
        buffs0 += adder;
        buffs1 += adder;
        buffs3 += adder;
        count  -= adder;
        env[0] += (uint16_t)s5;
        env[1] += (uint16_t)s5;
        env[2] += (uint16_t)s6;
        env[3] += (uint16_t)s6;
        env[4] += (uint16_t)t3;
        env[5] += (uint16_t)t3;
    }
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
    static int cnt = 0;
    static int16_t *lutt6;
    static int16_t *lutt5;
    uint8_t *save = (rsp.RDRAM + (w2 & 0xFFFFFF));
    uint8_t t4 = (uint8_t)((w1 >> 0x10) & 0xFF);
    int x;
    short *inp1, *inp2;
    int32_t out1[8];
    int16_t outbuff[0x3c0], *outp;
    uint32_t inPtr;

    if (t4 > 1) {
        /* Then set the cnt variable */
        cnt = (w1 & 0xFFFF);
        lutt6 = (int16_t *)save;
        return;
    }

    if (t4 == 0)
        lutt5 = (short *)(save + 0x10);

    lutt5 = (short *)(save + 0x10);

    for (x = 0; x < 8; x++) {
        int32_t a;
        a = (lutt5[x] + lutt6[x]) >> 1;
        lutt5[x] = lutt6[x] = (short)a;
    }
    inPtr = (uint32_t)(w1 & 0xffff);
    inp1 = (short *)(save);
    outp = outbuff;
    inp2 = (short *)(BufferSpace + inPtr);
    for (x = 0; x < cnt; x += 0x10) {
        out1[1] =  inp1[0] * lutt6[6];
        out1[1] += inp1[3] * lutt6[7];
        out1[1] += inp1[2] * lutt6[4];
        out1[1] += inp1[5] * lutt6[5];
        out1[1] += inp1[4] * lutt6[2];
        out1[1] += inp1[7] * lutt6[3];
        out1[1] += inp1[6] * lutt6[0];
        out1[1] += inp2[1] * lutt6[1]; /* 1 */

        out1[0] =  inp1[3] * lutt6[6];
        out1[0] += inp1[2] * lutt6[7];
        out1[0] += inp1[5] * lutt6[4];
        out1[0] += inp1[4] * lutt6[5];
        out1[0] += inp1[7] * lutt6[2];
        out1[0] += inp1[6] * lutt6[3];
        out1[0] += inp2[1] * lutt6[0];
        out1[0] += inp2[0] * lutt6[1];

        out1[3] =  inp1[2] * lutt6[6];
        out1[3] += inp1[5] * lutt6[7];
        out1[3] += inp1[4] * lutt6[4];
        out1[3] += inp1[7] * lutt6[5];
        out1[3] += inp1[6] * lutt6[2];
        out1[3] += inp2[1] * lutt6[3];
        out1[3] += inp2[0] * lutt6[0];
        out1[3] += inp2[3] * lutt6[1];

        out1[2] =  inp1[5] * lutt6[6];
        out1[2] += inp1[4] * lutt6[7];
        out1[2] += inp1[7] * lutt6[4];
        out1[2] += inp1[6] * lutt6[5];
        out1[2] += inp2[1] * lutt6[2];
        out1[2] += inp2[0] * lutt6[3];
        out1[2] += inp2[3] * lutt6[0];
        out1[2] += inp2[2] * lutt6[1];

        out1[5] =  inp1[4] * lutt6[6];
        out1[5] += inp1[7] * lutt6[7];
        out1[5] += inp1[6] * lutt6[4];
        out1[5] += inp2[1] * lutt6[5];
        out1[5] += inp2[0] * lutt6[2];
        out1[5] += inp2[3] * lutt6[3];
        out1[5] += inp2[2] * lutt6[0];
        out1[5] += inp2[5] * lutt6[1];

        out1[4] =  inp1[7] * lutt6[6];
        out1[4] += inp1[6] * lutt6[7];
        out1[4] += inp2[1] * lutt6[4];
        out1[4] += inp2[0] * lutt6[5];
        out1[4] += inp2[3] * lutt6[2];
        out1[4] += inp2[2] * lutt6[3];
        out1[4] += inp2[5] * lutt6[0];
        out1[4] += inp2[4] * lutt6[1];

        out1[7] =  inp1[6] * lutt6[6];
        out1[7] += inp2[1] * lutt6[7];
        out1[7] += inp2[0] * lutt6[4];
        out1[7] += inp2[3] * lutt6[5];
        out1[7] += inp2[2] * lutt6[2];
        out1[7] += inp2[5] * lutt6[3];
        out1[7] += inp2[4] * lutt6[0];
        out1[7] += inp2[7] * lutt6[1];

        out1[6] =  inp2[1] * lutt6[6];
        out1[6] += inp2[0] * lutt6[7];
        out1[6] += inp2[3] * lutt6[4];
        out1[6] += inp2[2] * lutt6[5];
        out1[6] += inp2[5] * lutt6[2];
        out1[6] += inp2[4] * lutt6[3];
        out1[6] += inp2[7] * lutt6[0];
        out1[6] += inp2[6] * lutt6[1];
        outp[1] = /*CLAMP*/((out1[1] + 0x4000) >> 0xF);
        outp[0] = /*CLAMP*/((out1[0] + 0x4000) >> 0xF);
        outp[3] = /*CLAMP*/((out1[3] + 0x4000) >> 0xF);
        outp[2] = /*CLAMP*/((out1[2] + 0x4000) >> 0xF);
        outp[5] = /*CLAMP*/((out1[5] + 0x4000) >> 0xF);
        outp[4] = /*CLAMP*/((out1[4] + 0x4000) >> 0xF);
        outp[7] = /*CLAMP*/((out1[7] + 0x4000) >> 0xF);
        outp[6] = /*CLAMP*/((out1[6] + 0x4000) >> 0xF);
        inp1 = inp2;
        inp2 += 8;
        outp += 8;
    }
    memcpy(save, inp2 - 8, 0x10);
    memcpy(BufferSpace + (w1 & 0xffff), outbuff, cnt);
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
