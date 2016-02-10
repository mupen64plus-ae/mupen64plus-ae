/******************************************************************************
 * Glide64 - Glide video plugin for Nintendo 64 emulators.
 * http://bitbucket.org/richard42/mupen64plus-video-glide64mk2/
 *
 * Copyright (C) 2010 Jon Ring
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *****************************************************************************/

#ifndef M64P_H
#define M64P_H

#include "m64p_types.h"
#include "m64p_plugin.h"
#include "m64p_config.h"
#include "m64p_vidext.h"
#include <stdio.h>

#define VIDEO_PLUGIN_API_VERSION	0x020100

void WriteLog(m64p_msg_level level, const char *msg, ...);

#endif
