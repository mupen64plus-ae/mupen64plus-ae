/*
* Glide64 - Glide video plugin for Nintendo 64 emulators.
* Copyright (c) 2002  Dave2001
* Copyright (c) 2003-2009  Sergey 'Gonetz' Lipski
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

//****************************************************************
//
// Glide64 - Glide Plugin for Nintendo 64 emulators
// Project started on December 29th, 2001
//
// Authors:
// Dave2001, original author, founded the project in 2001, left it in 2002
// Gugaman, joined the project in 2002, left it in 2002
// Sergey 'Gonetz' Lipski, joined the project in 2002, main author since fall of 2002
// Hiroshi 'KoolSmoky' Morii, joined the project in 2007
//
//****************************************************************
//
// To modify Glide64:
// * Write your name and (optional)email, commented by your work, so I know who did it, and so that you can find which parts you modified when it comes time to send it to me.
// * Do NOT send me the whole project or file that you modified.  Take out your modified code sections, and tell me where to put them.  If people sent the whole thing, I would have many different versions, but no idea how to combine them all.
//
//****************************************************************
//
// Created by Gonetz, 2007
//
//****************************************************************

//****************************************************************
// 32-bit Horizontal Mirror

void Mirror32bS (unsigned char * tex, wxUint32 mask, wxUint32 max_width, wxUint32 real_width, wxUint32 height)
{
	if (mask == 0) return;

	wxUint32 mask_width = (1 << mask);
  wxUint32 mask_mask = (mask_width-1) << 2;
	if (mask_width >= max_width) return;
	int count = max_width - mask_width;
	if (count <= 0) return;
  int line_full = real_width; // << 2;
  int line = line_full - (count); // << 2);
	if (line < 0) return;
	
  unsigned int * start = (unsigned int *)(tex) + (mask_width);
	
  unsigned int *edi = start;
//     asmMirror32bS (tex, start, mask_width, height, mask_mask, line, line_full, count);
  
  for(unsigned int ecx = height; ecx; --ecx) {
    for(int edx = 0; edx != count; ++edx) {
      
      unsigned int *esi = (unsigned int *)(tex);
      
      if ((mask_width + edx) & mask_width) {
        esi += (mask_mask - ((edx << 2) & mask_mask)) >> 2;
      }
      else
      {
        esi += ((edx << 2) & mask_mask) >> 2;
      } 
      *edi = *esi;
      edi++;
    }
    edi += line;
    tex += line_full << 2;
  }
}

//****************************************************************
// 32-bit Horizontal Wrap 

void Wrap32bS (unsigned char * tex, wxUint32 mask, wxUint32 max_width, wxUint32 real_width, wxUint32 height)
{
  if (mask == 0) return;

  wxUint32 mask_width = (1 << mask);
  wxUint32 mask_mask = (mask_width-1);
  if (mask_width >= max_width) return;
  int count = (max_width - mask_width);
  if (count <= 0) return;
  int line_full = real_width << 2;
  int line = line_full - (count << 2);
  if (line < 0) return;

  wxUint32 *start = (wxUint32 *)tex + (mask_width);

//     asmWrap32bS (tex, start, height, mask_mask, line, line_full, count);

  
  wxUint32 *edi = start;

  // esi is a pointer

  for(wxUint32 ecx = height; ecx; ecx--) {

    for(wxUint32 edx = 0; edx != count; edx++) {
      wxUint32 *esi = (wxUint32 *)tex;

      esi += edx & mask_mask;
      *edi = *esi;
      edi++;
    }
    edi += line >> 2;
    tex += line_full;
  }
}

//****************************************************************
// 32-bit Horizontal Clamp

void Clamp32bS (unsigned char * tex, wxUint32 width, wxUint32 clamp_to, wxUint32 real_width, wxUint32 real_height)
{
	if (real_width <= width) return;

  wxUint32 *dest = (wxUint32 *)tex + (width);
	wxUint32 *constant = dest-1;
	
	int count = clamp_to - width;
	
//    asmClamp32bS (dest, constant, real_height, line, line_full, count);	
	
	// converted asm portion begins here
  wxUint32 *esi = constant;
  wxUint32 *edi = dest;
  
  for(wxUint32 ecx = real_height; ecx; ecx--) {
    wxUint32 eax = *esi;
    
    for(wxUint32 edx = count; edx; edx--) {
      *edi = eax;
      edi++;
    }
    esi += real_width;
    edi += width;
    
  }
}

//****************************************************************
// 32-bit Vertical Mirror

void Mirror32bT (unsigned char * tex, wxUint32 mask, wxUint32 max_height, wxUint32 real_width)
{
	if (mask == 0) return;

	wxUint32 mask_height = (1 << mask);
	wxUint32 mask_mask = mask_height-1;
	if (max_height <= mask_height) return;
  int line_full = real_width << 2;

	wxUint32 *dst = (wxUint32 *)tex + ((mask_height * line_full) >> 2);

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

		dst += line_full>>2;
	}
}

//****************************************************************
// 32-bit Vertical Wrap

void Wrap32bT (unsigned char * tex, wxUint32 mask, wxUint32 max_height, wxUint32 real_width)
{
	if (mask == 0) return;

	wxUint32 mask_height = (1 << mask);
	wxUint32 mask_mask = mask_height-1;
	if (max_height <= mask_height) return;
  int line_full = real_width; // << 2;

	wxUint32 *dst = (wxUint32 *)tex + mask_height * line_full;

	for (wxUint32 y=mask_height; y<max_height; y++)
	{
		// not mirrored
		memcpy ((void*)dst, (void*)(tex + (y & mask_mask) * (line_full>>2)), (line_full>>2));

		dst += line_full>>2;
	}
}

//****************************************************************
// 32-bit Vertical Clamp

void Clamp32bT (unsigned char * tex, wxUint32 height, wxUint32 real_width, wxUint32 clamp_to)
{
  int line_full = real_width; // << 2;
	wxUint32  *dst = (wxUint32 *)tex + height * line_full;
	wxUint32 *const_line = (wxUint32 *)dst - line_full;

	for (wxUint32 y=height; y<clamp_to; y++)
	{
		memcpy ((void*)dst, (void*)const_line, line_full>>2);
		dst += line_full>>2;
	}
}
