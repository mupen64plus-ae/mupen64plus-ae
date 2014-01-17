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
#include "m64p_plugin.h"
#include "m64p_types.h"

#include "hle.h"
#include "alist_internal.h"
#include "alist.h"

/* FIXME: use DMEM instead */
uint8_t BufferSpace[0x10000];

/* local functions */
static void alist_process(const acmd_callback_t abi[], unsigned int abi_size)
{
    uint32_t inst1, inst2;
    unsigned int acmd;

    const uint32_t *alist = dram_u32(*dmem_u32(TASK_DATA_PTR));
    const uint32_t *const alist_end = alist + (*dmem_u32(TASK_DATA_SIZE) >> 2);

    while (alist != alist_end) {
        inst1 = *(alist++);
        inst2 = *(alist++);

        acmd = inst1 >> 24;

        if (acmd < abi_size)
            (*abi[acmd])(inst1, inst2);
        else
            DebugMessage(M64MSG_WARNING, "Invalid ABI command %u", acmd);
    }
}

/* global functions */
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

