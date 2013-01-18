/*
*   Glide64 - Glide video plugin for Nintendo 64 emulators.
*   Copyright (c) 2002  Dave2001
*   Copyright (c) 2008  Günther <guenther.emu@freenet.de>
*
*   This program is free software; you can redistribute it and/or modify
*   it under the terms of the GNU General Public License as published by
*   the Free Software Foundation; either version 2 of the License, or
*   any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public
*   Licence along with this program; if not, write to the Free
*   Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
*   Boston, MA  02110-1301, USA
*/

//****************************************************************
//
// Glide64 - Glide Plugin for Nintendo 64 emulators (tested mostly with Project64)
// Project started on December 29th, 2001
//
// To modify Glide64:
// * Write your name and (optional)email, commented by your work, so I know who did it, and so that you can find which parts you modified when it comes time to send it to me.
// * Do NOT send me the whole project or file that you modified.  Take out your modified code sections, and tell me where to put them.  If people sent the whole thing, I would have many different versions, but no idea how to combine them all.
//
// Official Glide64 development channel: #Glide64 on EFnet
//
// Original author: Dave2001 (Dave2999@hotmail.com)
// Other authors: Gonetz, Gugaman
//
//****************************************************************

// I am very confident I did this one right -- balrog

//****************************************************************
// 8-bit Horizontal Mirror

void Mirror8bS (unsigned char * tex, wxUint32 mask, wxUint32 max_width, wxUint32 real_width, wxUint32 height)
{
    if (mask == 0) return;

    wxUint32 mask_width = (1 << mask);
    wxUint32 mask_mask = (mask_width-1);
    if (mask_width >= max_width) return;
    int count = max_width - mask_width;
    if (count <= 0) return;
    int line_full = real_width;
    int line = line_full - (count);
    if (line < 0) return;
    unsigned char * start = tex + (mask_width);
    
    //   asmMirror8bS (tex, start, mask_width, height, mask_mask, line, line_full, count);
    
    unsigned char *edi = start;
    
    for(unsigned int ecx = height; ecx; --ecx) {
      for(int edx = 0; edx != count; ++edx) {
        
        unsigned char *esi = tex;
        if((mask_width + edx) & mask_width) {
          esi += (mask_mask - (edx & mask_mask));
        }
        else
        {
          esi += (edx & mask_mask);
        }
        *edi = *esi;
        edi++;
      }
      edi += line;
      tex += line_full;
    }
}

//****************************************************************
// 8-bit Vertical Mirror

void Mirror8bT (unsigned char * tex, wxUint32 mask, wxUint32 max_height, wxUint32 real_width)
{
    if (mask == 0) return;

    wxUint32 mask_height = (1 << mask);
    wxUint32 mask_mask = mask_height-1;
    if (max_height <= mask_height) return;
    int line_full = real_width;

    unsigned char * dst = tex + mask_height * line_full;

    for (wxUint32 y=mask_height; y<max_height; y++)
    {
        if (y & mask_height)
        {
            // mirrored
            memcpy ((void*)dst, (void*)(tex + (mask_mask - (y & mask_mask)) * line_full), line_full);
        }
        else
        {
            // not mirrored
            memcpy ((void*)dst, (void*)(tex + (y & mask_mask) * line_full), line_full);
        }

        dst += line_full;
    }
}

//****************************************************************
// 8-bit Horizontal Wrap (like mirror) ** UNTESTED **


void Wrap8bS (unsigned char * tex, wxUint32 mask, wxUint32 max_width, wxUint32 real_width, wxUint32 height)
{
    if (mask == 0) return;

    wxUint32 mask_width = (1 << mask);
    wxUint32 mask_mask = (mask_width-1) >> 2;
    if (mask_width >= max_width) return;
    int count = (max_width - mask_width) >> 2;
    if (count <= 0) return;
    int line_full = real_width;
    int line = line_full - (count << 2);
    if (line < 0) return;
    unsigned char * start = tex + (mask_width);
    
    wxUint32 *edi = (wxUint32 *)start;
    
    for(wxUint32 ecx = height; ecx; ecx--) {
      for(wxUint32 edx = 0; edx != count; edx++) {
        wxUint32 *esi = (wxUint32 *)tex;
        esi += (edx & mask_mask);
        *edi = *esi;
        edi++;
      }
      edi += line >> 2;
      tex += line_full >> 2;
    }
}

//****************************************************************
// 8-bit Vertical Wrap

void Wrap8bT (unsigned char * tex, wxUint32 mask, wxUint32 max_height, wxUint32 real_width)
{
    if (mask == 0) return;

    wxUint32 mask_height = (1 << mask);
    wxUint32 mask_mask = mask_height-1;
    if (max_height <= mask_height) return;
    int line_full = real_width;

    unsigned char * dst = tex + mask_height * line_full;

    for (wxUint32 y=mask_height; y<max_height; y++)
    {
        // not mirrored
        memcpy ((void*)dst, (void*)(tex + (y & mask_mask) * line_full), line_full);

        dst += line_full;
    }
}

//****************************************************************
// 8-bit Horizontal Clamp


void Clamp8bS (unsigned char * tex, wxUint32 width, wxUint32 clamp_to, wxUint32 real_width, wxUint32 real_height)
{
    if (real_width <= width) return;

    unsigned char * dest = tex + (width);
    unsigned char * constant = dest-1;
    int count = clamp_to - width;

    int line_full = real_width;
    int line = width;
    
//   asmClamp8bS (dest, constant, real_height, line, line_full, count);

    for(wxUint32 ecx = real_height; ecx; ecx--) {
      for(wxUint32 edx = count; edx; edx--) {
        *dest = *constant;
        dest++;
      }
      constant += line_full;
      dest += line;
    }
}

//****************************************************************
// 8-bit Vertical Clamp

void Clamp8bT (unsigned char * tex, wxUint32 height, wxUint32 real_width, wxUint32 clamp_to)
{
    int line_full = real_width;
    unsigned char * dst = tex + height * line_full;
    unsigned char * const_line = dst - line_full;

    for (wxUint32 y=height; y<clamp_to; y++)
    {
        memcpy ((void*)dst, (void*)const_line, line_full);
        dst += line_full;
    }
}

