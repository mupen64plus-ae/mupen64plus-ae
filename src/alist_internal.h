/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - alist_internal.h                                *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
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

#ifndef ALIST_INTERNAL_H
#define ALIST_INTERNAL_H

#include <stdint.h>

typedef void (*acmd_callback_t)(uint32_t inst1, uint32_t inst2);

/*
 * Audio flags
 */

#define A_INIT          0x01
#define A_CONTINUE      0x00
#define A_LOOP          0x02
#define A_OUT           0x02
#define A_LEFT          0x02
#define A_RIGHT         0x00
#define A_VOL           0x04
#define A_RATE          0x00
#define A_AUX           0x08
#define A_NOAUX         0x00
#define A_MAIN          0x00
#define A_MIX           0x10

/* FIXME: this decomposition into 3 ABI is not accurate,
 * there are a least 9 or 10 different ABI, each with one or a few revisions
 * for a total of almost 16 differents audio ucode.
 *
 * ABI2 in fact is a mix of at least 7 differents ABI which are mostly compatible
 * but not totally, that's why there is a isZeldaABI/isMKABI workaround.
 */
extern const acmd_callback_t ABI1[0x10];
extern const acmd_callback_t ABI2[0x20];
extern const acmd_callback_t ABI3[0x10];

extern uint8_t BufferSpace[0x10000];
#endif
