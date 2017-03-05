/*
 * Texture Filtering
 * Version:  1.0
 *
 * Copyright (C) 2007  Hiroshi Morii   All Rights Reserved.
 * Email koolsmoky(at)users.sourceforge.net
 * Web   http://www.3dfxzone.it/koolsmoky
 *
 * this is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * this is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GNU Make; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

#ifndef __TXQUANTIZE_H__
#define __TXQUANTIZE_H__

#include "TxInternal.h"
#include "TxUtil.h"

class TxQuantize
{
private:
  int _numcore;

  const volatile unsigned char One2Eight[2] =
  {
      0, // 0 = 00000000
     255, // 1 = 11111111
  };

  const volatile unsigned char Five2Eight[32] =
  {
    0, // 00000 = 00000000
    8, // 00001 = 00001000
    16, // 00010 = 00010000
    25, // 00011 = 00011001
    33, // 00100 = 00100001
    41, // 00101 = 00101001
    49, // 00110 = 00110001
    58, // 00111 = 00111010
    66, // 01000 = 01000010
    74, // 01001 = 01001010
    82, // 01010 = 01010010
    90, // 01011 = 01011010
    99, // 01100 = 01100011
    107, // 01101 = 01101011
    115, // 01110 = 01110011
    123, // 01111 = 01111011
    132, // 10000 = 10000100
    140, // 10001 = 10001100
    148, // 10010 = 10010100
    156, // 10011 = 10011100
    165, // 10100 = 10100101
    173, // 10101 = 10101101
    181, // 10110 = 10110101
    189, // 10111 = 10111101
    197, // 11000 = 11000101
    206, // 11001 = 11001110
    214, // 11010 = 11010110
    222, // 11011 = 11011110
    230, // 11100 = 11100110
    239, // 11101 = 11101111
    247, // 11110 = 11110111
    255  // 11111 = 11111111
  };

  /* fast optimized... well, sort of. */
  void ARGB1555_ARGB8888(uint32* src, uint32* dst, int width, int height);
  void ARGB4444_ARGB8888(uint32* src, uint32* dst, int width, int height);
  void RGB565_ARGB8888(uint32* src, uint32* dst, int width, int height);
  void A8_ARGB8888(uint32* src, uint32* dst, int width, int height);
  void AI44_ARGB8888(uint32* src, uint32* dst, int width, int height);
  void AI88_ARGB8888(uint32* src, uint32* dst, int width, int height);

  void ARGB8888_ARGB1555(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_ARGB4444(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_RGB565(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_A8(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_AI44(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_AI88(uint32* src, uint32* dst, int width, int height);

  /* quality */
  void ARGB8888_RGB565_ErrD(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_ARGB1555_ErrD(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_ARGB4444_ErrD(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_AI44_ErrD(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_AI88_Slow(uint32* src, uint32* dst, int width, int height);
  void ARGB8888_I8_Slow(uint32* src, uint32* dst, int width, int height);

public:
  TxQuantize();
  ~TxQuantize();

  /* others */
  void P8_16BPP(uint32* src, uint32* dst, int width, int height, uint32* palette);

  boolean quantize(uint8* src, uint8* dest, int width, int height, uint16 srcformat, uint16 destformat, boolean fastQuantizer = 1);
};

#endif /* __TXQUANTIZE_H__ */
