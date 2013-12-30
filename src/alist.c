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

#include "hle.h"
#include "alist_internal.h"
#include "alist.h"

/* local functions */
static void alist_process(const acmd_callback_t abi[], unsigned int abi_size)
{
    uint32_t inst1, inst2;
    unsigned int acmd;
    const OSTask_t *const task = get_task();

    const unsigned int *alist = (unsigned int *)(rsp.RDRAM + task->data_ptr);
    const unsigned int *const alist_end = alist + (task->data_size >> 2);

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
void alist_process_ABI1(void)
{
    alist_process(ABI1, 0x10);
}

void alist_process_ABI2(void)
{
    alist_process(ABI2, 0x20);
}

void alist_process_ABI3(void)
{
    alist_process(ABI3, 0x10);
}


