/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - alist.c                                         *
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

#include <stdbool.h>
#include <stdint.h>
#include <string.h>

#include "hle.h"
#include "alist_internal.h"
#include "audio.h"

/* FIXME: make it local */
uint8_t BufferSpace[0x10000];

/* local functions */
static int16_t* sample(unsigned pos)
{
    return (int16_t*)BufferSpace + (pos ^ S);
}


/* global functions */
void alist_process(const acmd_callback_t abi[], unsigned int abi_size)
{
    uint32_t w1, w2;
    unsigned int acmd;

    const uint32_t *alist = dram_u32(*dmem_u32(TASK_DATA_PTR));
    const uint32_t *const alist_end = alist + (*dmem_u32(TASK_DATA_SIZE) >> 2);

    while (alist != alist_end) {
        w1 = *(alist++);
        w2 = *(alist++);

        acmd = w1 >> 24;

        if (acmd < abi_size)
            (*abi[acmd])(w1, w2);
        else
            DebugMessage(M64MSG_WARNING, "Invalid ABI command %u", acmd);
    }
}

void alist_clear(uint16_t dmem, uint16_t count)
{
    memset(BufferSpace + dmem, 0, count);
}

void alist_load(uint16_t dmem, uint32_t address, uint16_t count)
{
    memcpy(BufferSpace + dmem, rsp.RDRAM + address, count);
}

void alist_save(uint16_t dmem, uint32_t address, uint16_t count)
{
    memcpy(rsp.RDRAM + address, BufferSpace + dmem, count);
}

void alist_move(uint16_t dmemo, uint16_t dmemi, uint16_t count)
{
    while (count != 0) {
        BufferSpace[(dmemo++)^S8] = BufferSpace[(dmemi++)^S8];
        --count;
    }
}

void alist_interleave(uint16_t dmemo, uint16_t left, uint16_t right, uint16_t count)
{
    uint16_t       *dst  = (uint16_t*)(BufferSpace + dmemo);
    const uint16_t *srcL = (uint16_t*)(BufferSpace + left);
    const uint16_t *srcR = (uint16_t*)(BufferSpace + right);

    count >>= 2;

    while(count != 0) {
        uint16_t l1 = *(srcL++);
        uint16_t l2 = *(srcL++);
        uint16_t r1 = *(srcR++);
        uint16_t r2 = *(srcR++);

#if M64P_BIG_ENDIAN
        *(dst++) = l1;
        *(dst++) = r1;
        *(dst++) = l2;
        *(dst++) = r2;
#else
        *(dst++) = r2;
        *(dst++) = l2;
        *(dst++) = r1;
        *(dst++) = l1;
#endif
        --count;
    }
}

void alist_mix(uint16_t dmemo, uint16_t dmemi, uint16_t count, int16_t gain)
{
    int16_t       *dst = (int16_t*)(BufferSpace + dmemo);
    const int16_t *src = (int16_t*)(BufferSpace + dmemi);

    count >>= 1;

    while(count != 0) {
        *dst = clamp_s16(*dst + ((*src * gain) >> 15));

        ++dst;
        ++src;
        --count;
    }
}



static void alist_resample_reset(uint16_t pos, uint32_t* pitch_accu)
{
    unsigned k;

    for(k = 0; k < 4; ++k)
        *sample(pos + k) = 0;

    *pitch_accu = 0;
}

static void alist_resample_load(uint32_t address, uint16_t pos, uint32_t* pitch_accu)
{
    *sample(pos + 0) = *dram_u16(address + 0);
    *sample(pos + 1) = *dram_u16(address + 2);
    *sample(pos + 2) = *dram_u16(address + 4);
    *sample(pos + 3) = *dram_u16(address + 6);

    *pitch_accu = *dram_u16(address + 8);
}

static void alist_resample_save(uint32_t address, uint16_t pos, uint32_t pitch_accu)
{
    *dram_u16(address + 0) = *sample(pos + 0);
    *dram_u16(address + 2) = *sample(pos + 1);
    *dram_u16(address + 4) = *sample(pos + 2);
    *dram_u16(address + 6) = *sample(pos + 3);

    *dram_u16(address + 8) = pitch_accu;
}

void alist_resample(
        bool init,
        uint16_t dmemo,
        uint16_t dmemi,
        uint16_t count,
        uint32_t pitch,     /* Q16.16 */
        uint32_t address)
{
    uint32_t pitch_accu;

    uint16_t ipos = dmemi >> 1;
    uint16_t opos = dmemo >> 1;
    count >>= 1;
    ipos -= 4;

    if (init)
        alist_resample_reset(ipos, &pitch_accu);
    else
        alist_resample_load(address, ipos, &pitch_accu);

    while (count != 0) {
        const int16_t* lut = RESAMPLE_LUT + ((pitch_accu & 0xfc00) >> 8);

        *sample(opos++) = clamp_s16(
                ((*sample(ipos    ) * lut[0]) >> 15) +
                ((*sample(ipos + 1) * lut[1]) >> 15) +
                ((*sample(ipos + 2) * lut[2]) >> 15) +
                ((*sample(ipos + 3) * lut[3]) >> 15));

        pitch_accu += pitch;
        ipos += (pitch_accu >> 16);
        pitch_accu &= 0xffff;
        --count;
    }

    alist_resample_save(address, ipos, pitch_accu);
}


typedef unsigned int (*adpcm_predict_frame_t)(int16_t* dst, uint16_t dmemi, unsigned char scale);

static unsigned int adpcm_predict_frame_4bits(int16_t* dst, uint16_t dmemi, unsigned char scale)
{
    unsigned int i;
    unsigned int rshift = (scale < 12) ? 12 - scale : 0;

    for(i = 0; i < 8; ++i) {
        uint8_t byte = BufferSpace[(dmemi++)^S8];

        *(dst++) = adpcm_predict_sample(byte, 0xf0,  8, rshift);
        *(dst++) = adpcm_predict_sample(byte, 0x0f, 12, rshift);
    }

    return 8;
}

static unsigned int adpcm_predict_frame_2bits(int16_t* dst, uint16_t dmemi, unsigned char scale)
{
    unsigned int i;
    unsigned int rshift = (scale < 14) ? 14 - scale : 0;

    for(i = 0; i < 4; ++i) {
        uint8_t byte = BufferSpace[(dmemi++)^S8];

        *(dst++) = adpcm_predict_sample(byte, 0xc0,  8, rshift);
        *(dst++) = adpcm_predict_sample(byte, 0x30, 10, rshift);
        *(dst++) = adpcm_predict_sample(byte, 0x0c, 12, rshift);
        *(dst++) = adpcm_predict_sample(byte, 0x03, 14, rshift);
    }

    return 4;
}

void alist_adpcm(
        bool init,
        bool loop,
        bool two_bit_per_sample,
        uint16_t dmemo,
        uint16_t dmemi,
        uint16_t count,
        const int16_t* codebook,
        uint32_t loop_address,
        uint32_t last_frame_address)
{
    assert((count & 0x1f) == 0);

    int16_t last_frame[16];
    size_t i;

    if (init)
        memset(last_frame, 0, 16*sizeof(last_frame[0]));
    else
        dram_load_u16((uint16_t*)last_frame, (loop) ? loop_address : last_frame_address, 16);

    for(i = 0; i < 16; ++i, dmemo += 2)
        *(int16_t*)(BufferSpace + (dmemo ^ S16)) = last_frame[i];

    adpcm_predict_frame_t predict_frame = (two_bit_per_sample)
        ? adpcm_predict_frame_2bits
        : adpcm_predict_frame_4bits;

    while (count != 0) {
        int16_t frame[16];
        uint8_t code = BufferSpace[(dmemi++)^S8];
        unsigned char scale = (code & 0xf0) >> 4;
        const int16_t* const cb_entry = codebook + ((code & 0xf) << 4);

        dmemi += predict_frame(frame, dmemi, scale);

        adpcm_compute_residuals(last_frame    , frame    , cb_entry, last_frame + 14, 8);
        adpcm_compute_residuals(last_frame + 8, frame + 8, cb_entry, last_frame + 6 , 8);

        for(i = 0; i < 16; ++i, dmemo += 2)
            *(int16_t*)(BufferSpace + (dmemo ^ S16)) = last_frame[i];

        count -= 32;
    }

    dram_store_u16((uint16_t*)last_frame, last_frame_address, 16);
}
