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
    uint8_t Flags = (uint8_t)(w1 >> 0x10);
    if (Flags & 0x4) { /* 288 */
        if (Flags & 0x2) { /* 290 */
            l_alist.vol[0]  = (int16_t)w1; /* 0x50 */
            l_alist.dry   = (int16_t)(w2 >> 0x10); /* 0x4E */
            l_alist.wet   = (int16_t)w2; /* 0x4C */
        } else {
            l_alist.target[1]  = (int16_t)w1; /* 0x46 */
            l_alist.rate[1] = (int32_t)w2; /* 0x48/0x4A */
        }
    } else {
        l_alist.target[0]  = (int16_t)w1; /* 0x40 */
        l_alist.rate[0] = (int32_t)w2; /* 0x42/0x44 */
    }
}

static void ENVMIXER3(uint32_t w1, uint32_t w2)
{
    uint8_t flags = (uint8_t)((w1 >> 16) & 0xff);
    uint32_t addy = (w2 & 0xFFFFFF);

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

    l_alist.vol[1] = (int16_t)w1;

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

static void CLEARBUFF3(uint32_t w1, uint32_t w2)
{
    uint16_t dmem  = w1 + 0x4f0;
    uint16_t count = w2;

    alist_clear(dmem, count);
}

/* TODO Needs accuracy verification... */
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

/* TODO Needs accuracy verification... */
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

/* TODO Needs accuracy verification... */
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
