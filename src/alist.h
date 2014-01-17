/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - alist.h                                         *
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

#ifndef ALIST_H
#define ALIST_H

void alist_process_audio(void);
void alist_process_audio_ge(void);
void alist_process_audio_bc(void);
void alist_process_mk(void);
void alist_process_sfj(void);
void alist_process_wrjb(void);
void alist_process_sf(void);
void alist_process_fz(void);
void alist_process_ys(void);
void alist_process_1080(void);
void alist_process_oot(void);
void alist_process_mm(void);
void alist_process_mmb(void);
void alist_process_ac(void);
void alist_process_naudio(void);
void alist_process_naudio_bk(void);
void alist_process_naudio_dk(void);
void alist_process_naudio_mp3(void);
void alist_process_naudio_cbfd(void);

/* FIXME: to remove when isZeldaABI/isMKABI workaround is gone */
void init_ucode2(void);

#endif

