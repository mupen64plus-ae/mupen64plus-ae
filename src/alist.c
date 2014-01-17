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

/* global functions */
void alist_process(const acmd_callback_t abi[], unsigned int abi_size)
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

