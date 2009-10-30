/***************************************************************************
                          pad.h  -  description
                             -------------------
    begin                : Mon Nov 11 2002
    copyright            : (C) 2002 by blight
    email                : blight@fuckmicrosoft.com
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

#ifdef GUI_SDL

#ifndef __PAD_IMAGE_H__
#define __PAD_IMAGE_H__

extern const struct {
  unsigned int   width;
  unsigned int   height;
  unsigned int   bytes_per_pixel; /* 3:RGB, 4:RGBA */
  unsigned char  pixel_data[304 * 310 * 4 + 1];
} pad_image;

#endif // __PAD_IMAGE_H__

#endif // GUI_SDL

