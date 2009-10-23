/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - wintypes.h                                              *
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

#ifndef __WINTYPES_H__
#define __WINTYPES_H__

#include <sys/types.h>

typedef int HWND;
typedef int HINSTANCE;
typedef void* LPVOID;

#define __declspec(dllexport)
#define __cdecl
#define _cdecl
#define WINAPI

typedef u_int32_t       DWORD;
typedef u_int16_t       WORD;
typedef u_int8_t            BYTE, byte;
typedef int         BOOL, BOOLEAN;
#define __int8                  char
#define __int16                 short
#define __int32                 int
#define __int64                 long long

/** HRESULT stuff **/
typedef int             HRESULT;
#define S_OK                ((HRESULT)0L)
#define E_NOTIMPL       0x80004001L

#ifndef FALSE
# define FALSE (0)
#endif
#ifndef TRUE
# define TRUE (!FALSE)
#endif

typedef int HMENU;
typedef int RECT;
typedef int PAINTSTRUCT;

#endif /* __WINTYPES_H__ */

