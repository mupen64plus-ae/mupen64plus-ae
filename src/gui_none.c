/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - nogui.h                                                 *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2008 Tillin9 wahrhaft                                   *
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

#include <stdlib.h>
#include <stdio.h>

#include "gui.h"

#include "../main/winlnxdefs.h"
#include "../main/version.h"

EXPORT void CALL DllConfig(HWND hParent)
{
printf("JttL's SDL Audio compiled with GUI=NONE. Please edit the config file\nin SHAREDIR/jttl_audio.conf.conf manually or recompile plugin with a GUI.\n");
}

EXPORT void CALL DllAbout(HWND hParent)
{
printf("Mupen64 SDL Audio Plugin %s.\nOriginal code by JttL.\nGtk GUI by wahrhaft.\nFixes and features by Richard42, DarkJeztr, Tillin9, and others.\n", PLUGIN_VERSION);
}

void display_test(const char *Message)
{
printf("%s\n", Message);
}

