/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - osal_win32types.h                                       *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
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

#if defined(WIN32)
  #include <windows.h>
#elif !defined(WIN32TYPES_H)
#define WIN32TYPES_H

typedef unsigned int BOOL;
typedef unsigned char BYTE, CHAR, TCHAR, *LPBYTE;
typedef unsigned int UINT, uint, ULONG;
typedef void VOID, *LPVOID;
typedef float FLOAT;

typedef unsigned int DWORD, *LPDWORD;
typedef unsigned short WORD;
typedef int LONG;

#define __int16 short
#define __int32 int
#define __int64 long long

typedef int HWND;
typedef void* HBITMAP;

typedef struct
{
   int top;
   int bottom;
   int right;
   int left;
} RECT;

typedef struct _COORDRECT
{
   int x1,y1;
   int x2,y2;
} COORDRECT;

#define __declspec(dllexport)
#define _cdecl
#define __cdecl

#ifndef FALSE
#define FALSE 0
#endif

#ifndef TRUE
#define TRUE 1
#endif

#define MAX_PATH PATH_MAX
#define _MAX_PATH PATH_MAX

typedef unsigned int COLOR;
typedef int SURFFORMAT;

#define SURFFMT_A8R8G8B8 21

#define COLOR_RGBA(r,g,b,a) (((r&0xFF)<<16) | ((g&0xFF)<<8) | ((b&0xFF)<<0) | ((a&0xFF)<<24))
#define CONST const


typedef struct tagBITMAPINFOHEADER
{
   unsigned int biSize;
   int biWidth;
   int biHeight;
   unsigned short biPlanes;
   unsigned short biBitCount;
   unsigned int biCompression;
   unsigned int biSizeImage;
   int biXPelsPerMeter;
   int biYPelsPerMeter;
   unsigned int biClrUsed;
   unsigned int biClrImportant;
}  __attribute__ ((packed)) BITMAPINFOHEADER;

typedef struct tagRGBQUAD
{
   BYTE rgbBlue;
   BYTE rgbGreen;
   BYTE rgbRed;
   BYTE rgbReserved;
} RGBQUAD;

typedef struct tagBITMAPINFO
{
   BITMAPINFOHEADER bmiHeader;
   RGBQUAD bmiColors[1];
} BITMAPINFO;

typedef enum _IMAGE_FILEFORMAT 
{
   XIFF_BMP = 0,
     XIFF_JPG = 1,
     XIFF_TGA = 2,
     XIFF_PNG = 3,
     XIFF_DDS = 4,
     XIFF_PPM = 5,
     XIFF_DIB = 6,
     XIFF_HDR = 7,
     XIFF_PFM = 8,
     XIFF_FORCE_DWORD = 0x7fffffff
} IMAGE_FILEFORMAT;

typedef struct _IMAGE_INFO
{
   UINT Width;
   UINT Height;
   UINT Depth;
   UINT MipLevels;
   SURFFORMAT Format;
   IMAGE_FILEFORMAT ImageFileFormat;
} IMAGE_INFO;


typedef struct tagBITMAPFILEHEADER
{
   unsigned short    bfType; 
   unsigned int   bfSize; 
   unsigned short    bfReserved1; 
   unsigned short    bfReserved2; 
   unsigned int   bfOffBits; 
} __attribute__ ((packed)) BITMAPFILEHEADER, *PBITMAPFILEHEADER;

#define BI_RGB 0

typedef enum _BLEND 
{
   BLEND_ZERO = 1,
     BLEND_ONE = 2,
     BLEND_SRCCOLOR = 3,
     BLEND_INVSRCCOLOR = 4,
     BLEND_SRCALPHA = 5,
     BLEND_INVSRCALPHA = 6,
     BLEND_DESTALPHA = 7,
     BLEND_INVDESTALPHA = 8,
     BLEND_DESTCOLOR = 9,
     BLEND_INVDESTCOLOR = 10,
     BLEND_SRCALPHASAT = 11,
     BLEND_BOTHSRCALPHA = 12,
     BLEND_BOTHINVSRCALPHA = 13,
     BLEND_BLENDFACTOR = 14,
     BLEND_INVBLENDFACTOR = 15,
     BLEND_FORCE_DWORD = 0x7fffffff
} BLEND;

#endif // WIN32TYPES_H

