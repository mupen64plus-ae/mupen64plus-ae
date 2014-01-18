/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - alist.c                                         *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2012 Bobby Smiles                                       *
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

#include <stdint.h>

#include "hle.h"
#include "alist_internal.h"

/* FIXME: use DMEM instead */
uint8_t BufferSpace[0x10000];

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
