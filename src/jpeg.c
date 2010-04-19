/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - jpeg.c                                          *
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

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "m64p_types.h"
#include "m64p_plugin.h"
#include "hle.h"

static struct 
{
   unsigned int pic;
   int w;
   int h;
   unsigned int m1;
   unsigned int m2;
   unsigned int m3;
} jpg_data;

static short* q[3];

static short *pic;

static unsigned int len1, len2;

void jpg_uncompress(OSTask_t *task)
{
   int i, w;
   short *temp1,*temp2;
   short* data = (short*)(rsp.RDRAM + task->ucode_data);
   short m[8*32];
   
   if (!(task->flags & 1))
   {
    int copysize = task->data_size;
    if (copysize > sizeof(jpg_data))
        copysize = sizeof(jpg_data);
    memcpy(&jpg_data, rsp.RDRAM+task->data_ptr, copysize);
    q[0] = (short*)(rsp.RDRAM + jpg_data.m1);
    q[1] = (short*)(rsp.RDRAM + jpg_data.m2);
    q[2] = (short*)(rsp.RDRAM + jpg_data.m3);
    
    if (jpg_data.h == 0)
      {
         len1 = 512;
         len2 = 255;
      }
    else
      {
         len1 = 768;
         len2 = 511;
      }
     }
   else
   {
     DebugMessage(M64MSG_WARNING, "jpg_uncompress(): unexpected flags\n");
   }
   pic = (short*)(rsp.RDRAM + jpg_data.pic);

   temp1 = (short*)malloc((jpg_data.h+4)*64*2);
   temp2 = (short*)malloc((jpg_data.h+4)*64*2);
   w = jpg_data.w;
   
   do
     {
    // quantification
    for (i=0; i<(jpg_data.h+2)*64; i++)
      temp1[i] = (short)((unsigned short)(pic[i^S]*q[0][(i&0x3F)^S])*(int)data[0^S]);
    for (;i<(jpg_data.h+3)*64; i++)
      temp1[i] = (short)((unsigned short)(pic[i^S]*q[1][(i&0x3F)^S])*(int)data[0^S]);
    for (;i<(jpg_data.h+4)*64; i++)
      temp1[i] = (short)((unsigned short)(pic[i^S]*q[2][(i&0x3F)^S])*(int)data[0^S]);
    
    // zigzag
    for (i=0; i<(jpg_data.h+4); i++)
      {
         temp2[i*64+0 ] = temp1[i*64+0 ];
         temp2[i*64+8 ] = temp1[i*64+1 ];
         temp2[i*64+1 ] = temp1[i*64+2 ];
         temp2[i*64+2 ] = temp1[i*64+3 ];
         temp2[i*64+9 ] = temp1[i*64+4 ];
         temp2[i*64+16] = temp1[i*64+5 ];
         temp2[i*64+24] = temp1[i*64+6 ];
         temp2[i*64+17] = temp1[i*64+7 ];
         temp2[i*64+10] = temp1[i*64+8 ];
         temp2[i*64+3 ] = temp1[i*64+9 ];
         temp2[i*64+4 ] = temp1[i*64+10];
         temp2[i*64+11] = temp1[i*64+11];
         temp2[i*64+18] = temp1[i*64+12];
         temp2[i*64+25] = temp1[i*64+13];
         temp2[i*64+32] = temp1[i*64+14];
         temp2[i*64+40] = temp1[i*64+15];
         temp2[i*64+33] = temp1[i*64+16];
         temp2[i*64+26] = temp1[i*64+17];
         temp2[i*64+19] = temp1[i*64+18];
         temp2[i*64+12] = temp1[i*64+19];
         temp2[i*64+5 ] = temp1[i*64+20];
         temp2[i*64+6 ] = temp1[i*64+21];
         temp2[i*64+13] = temp1[i*64+22];
         temp2[i*64+20] = temp1[i*64+23];
         temp2[i*64+27] = temp1[i*64+24];
         temp2[i*64+34] = temp1[i*64+25];
         temp2[i*64+41] = temp1[i*64+26];
         temp2[i*64+48] = temp1[i*64+27];
         temp2[i*64+56] = temp1[i*64+28];
         temp2[i*64+49] = temp1[i*64+29];
         temp2[i*64+42] = temp1[i*64+30];
         temp2[i*64+35] = temp1[i*64+31];
         temp2[i*64+28] = temp1[i*64+32];
         temp2[i*64+21] = temp1[i*64+33];
         temp2[i*64+14] = temp1[i*64+34];
         temp2[i*64+7 ] = temp1[i*64+35];
         temp2[i*64+15] = temp1[i*64+36];
         temp2[i*64+22] = temp1[i*64+37];
         temp2[i*64+29] = temp1[i*64+38];
         temp2[i*64+36] = temp1[i*64+39];
         temp2[i*64+43] = temp1[i*64+40];
         temp2[i*64+50] = temp1[i*64+41];
         temp2[i*64+57] = temp1[i*64+42];
         temp2[i*64+58] = temp1[i*64+43];
         temp2[i*64+51] = temp1[i*64+44];
         temp2[i*64+44] = temp1[i*64+45];
         temp2[i*64+37] = temp1[i*64+46];
         temp2[i*64+30] = temp1[i*64+47];
         temp2[i*64+23] = temp1[i*64+48];
         temp2[i*64+31] = temp1[i*64+49];
         temp2[i*64+38] = temp1[i*64+50];
         temp2[i*64+45] = temp1[i*64+51];
         temp2[i*64+52] = temp1[i*64+52];
         temp2[i*64+59] = temp1[i*64+53];
         temp2[i*64+60] = temp1[i*64+54];
         temp2[i*64+53] = temp1[i*64+55];
         temp2[i*64+46] = temp1[i*64+56];
         temp2[i*64+39] = temp1[i*64+57];
         temp2[i*64+47] = temp1[i*64+58];
         temp2[i*64+54] = temp1[i*64+59];
         temp2[i*64+61] = temp1[i*64+60];
         temp2[i*64+62] = temp1[i*64+61];
         temp2[i*64+55] = temp1[i*64+62];
         temp2[i*64+63] = temp1[i*64+63];
      }
    
    // idct
    for (i=0; i<(jpg_data.h+4); i++)
      {
         int j,k;
         int accum;
         
         for (j=0; j<8; j++)
           {
          m[8 *8+j] = (((int)temp2[i*64+1*8+j] * (int)data[(2*8+0)^S]*2)+0x8000
                   +((int)temp2[i*64+7*8+j] * (int)data[(2*8+1)^S]*2))>>16;
          m[9 *8+j] = (((int)temp2[i*64+5*8+j] * (int)data[(2*8+2)^S]*2)+0x8000
                   +((int)temp2[i*64+3*8+j] * (int)data[(2*8+3)^S]*2))>>16;
          m[10*8+j] = (((int)temp2[i*64+3*8+j] * (int)data[(2*8+2)^S]*2)+0x8000
                   +((int)temp2[i*64+5*8+j] * (int)data[(2*8+4)^S]*2))>>16;
          m[11*8+j] = (((int)temp2[i*64+7*8+j] * (int)data[(2*8+0)^S]*2)+0x8000
                   +((int)temp2[i*64+1*8+j] * (int)data[(2*8+5)^S]*2))>>16;
          
          m[6 *8+j] = (((int)temp2[i*64+0*8+j] * (int)data[(3*8+0)^S]*2)+0x8000
                   +  ((int)temp2[i*64+4*8+j] * (int)data[(3*8+1)^S]*2))>>16;
          
          m[5 *8+j] = m[11*8+j]-m[10*8+j];
          m[4 *8+j] = m[8 *8+j]-m[9 *8+j];
          m[12*8+j] = m[8 *8+j]+m[9 *8+j];
          m[15*8+j] = m[11*8+j]+m[10*8+j];
          
          m[13*8+j] = (((int)m[5*8+j] * (int)data[(3*8+0)^S]*2)+0x8000
                   +((int)m[4*8+j] * (int)data[(3*8+1)^S]*2))>>16;
          m[14*8+j] = (((int)m[5*8+j] * (int)data[(3*8+0)^S]*2)+0x8000
                   +((int)m[4*8+j] * (int)data[(3*8+0)^S]*2))>>16;
          
          m[4 *8+j] = (((int)temp2[i*64+0*8+j] * (int)data[(3*8+0)^S]*2)+0x8000
                   +((int)temp2[i*64+4*8+j] * (int)data[(3*8+0)^S]*2))>>16;
          m[5 *8+j] = (((int)temp2[i*64+6*8+j] * (int)data[(3*8+2)^S]*2)+0x8000
                   +((int)temp2[i*64+2*8+j] * (int)data[(3*8+4)^S]*2))>>16;
          m[7 *8+j] = (((int)temp2[i*64+2*8+j] * (int)data[(3*8+2)^S]*2)+0x8000
                   +((int)temp2[i*64+6*8+j] * (int)data[(3*8+3)^S]*2))>>16;
          
          m[8 *8+j] = m[4 *8+j]+m[5 *8+j];
          m[9 *8+j] = m[6 *8+j]+m[7 *8+j];
          m[10*8+j] = m[6 *8+j]-m[7 *8+j];
          m[11*8+j] = m[4 *8+j]-m[5 *8+j];
          
          m[16*8+j] = m[8 *8+j]+m[15*8+j];
          m[17*8+j] = m[9 *8+j]+m[14*8+j];
          m[18*8+j] = m[10*8+j]+m[13*8+j];
          m[19*8+j] = m[11*8+j]+m[12*8+j];
          m[20*8+j] = m[11*8+j]-m[12*8+j];
          m[21*8+j] = m[10*8+j]-m[13*8+j];
          m[22*8+j] = m[9 *8+j]-m[14*8+j];
          m[23*8+j] = m[8 *8+j]-m[15*8+j];
           }
         // transpose
         for (j=0; j<8; j++)
           for (k=j; k<8; k++)
         {
            m[24*8+j*8+k] = m[16*8+k*8+j];
            m[24*8+k*8+j] = m[16*8+j*8+k];
         }
         
         for (j=0; j<8; j++)
           {
          m[8 *8+j] = (((int)m[25*8+j] * (int)data[(2*8+0)^S]*2)+0x8000
                   +((int)m[31*8+j] * (int)data[(2*8+1)^S]*2))>>16;
          m[9 *8+j] = (((int)m[29*8+j] * (int)data[(2*8+2)^S]*2)+0x8000
                   +((int)m[27*8+j] * (int)data[(2*8+3)^S]*2))>>16;
          m[10*8+j] = (((int)m[27*8+j] * (int)data[(2*8+2)^S]*2)+0x8000
                   +((int)m[29*8+j] * (int)data[(2*8+4)^S]*2))>>16;
          m[11*8+j] = (((int)m[31*8+j] * (int)data[(2*8+0)^S]*2)+0x8000
                   +((int)m[25*8+j] * (int)data[(2*8+5)^S]*2))>>16;
          
          m[6 *8+j] = (((int)m[24*8+j] * (int)data[(3*8+0)^S]*2)+0x8000
                   +((int)m[28*8+j] * (int)data[(3*8+1)^S]*2))>>16;
          
          m[5 *8+j] = m[11*8+j]-m[10*8+j];
          m[4 *8+j] = m[8 *8+j]-m[9 *8+j];
          m[12*8+j] = m[8 *8+j]+m[9 *8+j];
          m[15*8+j] = m[11*8+j]+m[10*8+j];
          
          m[13*8+j] = (((int)m[5*8+j] * (int)data[(3*8+0)^S]*2)+0x8000
                   +((int)m[4*8+j] * (int)data[(3*8+1)^S]*2))>>16;
          m[14*8+j] = (((int)m[5*8+j] * (int)data[(3*8+0)^S]*2)+0x8000
                   +((int)m[4*8+j] * (int)data[(3*8+0)^S]*2))>>16;
          
          m[4 *8+j] = (((int)m[24*8+j] * (int)data[(3*8+0)^S]*2)+0x8000
                   +((int)m[28*8+j] * (int)data[(3*8+0)^S]*2))>>16;
          m[5 *8+j] = (((int)m[30*8+j] * (int)data[(3*8+2)^S]*2)+0x8000
                   +((int)m[26*8+j] * (int)data[(3*8+4)^S]*2))>>16;
          m[7 *8+j] = (((int)m[26*8+j] * (int)data[(3*8+2)^S]*2)+0x8000
                   +((int)m[30*8+j] * (int)data[(3*8+3)^S]*2))>>16;
          
          m[8 *8+j] = m[4 *8+j]+m[5 *8+j];
          m[9 *8+j] = m[6 *8+j]+m[7 *8+j];
          m[10*8+j] = m[6 *8+j]-m[7 *8+j];
          m[11*8+j] = m[4 *8+j]-m[5 *8+j];
          
          accum = ((int)m[8 *8+j] * (int)data[1^S]*2)+0x8000
            + ((int)m[15*8+j] * (int)data[1^S]*2);
          temp1[i*64+0*8+j] = (short)(accum>>16);
          temp1[i*64+7*8+j] = (accum+((int)m[15*8+j]*(int)data[2^S]*2))>>16;
          accum = ((int)m[9 *8+j] * (int)data[1^S]*2)+0x8000
            + ((int)m[14*8+j] * (int)data[1^S]*2);
          temp1[i*64+1*8+j] = (short)(accum>>16);
          temp1[i*64+6*8+j] = (accum+((int)m[14*8+j]*(int)data[2^S]*2))>>16;
          accum = ((int)m[10*8+j] * (int)data[1^S]*2)+0x8000
            + ((int)m[13*8+j] * (int)data[1^S]*2);
          temp1[i*64+2*8+j] = (short)(accum>>16);
          temp1[i*64+5*8+j] = (accum+((int)m[13*8+j]*(int)data[2^S]*2))>>16;
          accum = ((int)m[11*8+j] * (int)data[1^S]*2)+0x8000
            + ((int)m[12*8+j] * (int)data[1^S]*2);
          temp1[i*64+3*8+j] = (short)(accum>>16);
          temp1[i*64+4*8+j] = (accum+((int)m[12*8+j]*(int)data[2^S]*2))>>16;
           }
      }
    
    if (jpg_data.h == 0)
      {
         DebugMessage(M64MSG_WARNING, "jpg_uncompress: jpg_data.h==0\n");
      }
    else
      {
         for (i=0; i<8; i++)
           m[9 *8+i] = m[10*8+i] = m[11*8+i] = m[12*8+i] = 0;
         m[9 *8+0] = m[10*8+2] = m[11*8+4] = m[12*8+6] = data[6^S];
         m[9 *8+1] = m[10*8+3] = m[11*8+5] = m[12*8+7] = data[7^S];
         for (i=0; i<8; i++)
           {
          m[1 *8+i] = data[(0*8+i)^S];
          m[4 *8+i] = data[(1*8+i)^S];
           }
         for (i=0; i<2; i++)
           {
          int j;
          for (j=0; j<4; j++)
            {
               int k;
               for (k=0; k<8; k++)
             {
                m[16*8+k]=(short)((int)m[9 *8+k]*(int)temp1[256+i*32+j*8+64+0]
                          +(int)m[10*8+k]*(int)temp1[256+i*32+j*8+64+1]
                          +(int)m[11*8+k]*(int)temp1[256+i*32+j*8+64+2]
                          +(int)m[12*8+k]*(int)temp1[256+i*32+j*8+64+3]);
                
                m[15*8+k] =(short)((int)m[9 *8+k]*(int)temp1[256+i*32+j*8+64+4]
                           +(int)m[10*8+k]*(int)temp1[256+i*32+j*8+64+5]
                           +(int)m[11*8+k]*(int)temp1[256+i*32+j*8+64+6]
                           +(int)m[12*8+k]*(int)temp1[256+i*32+j*8+64+7]);
                
                m[18*8+k] = temp1[i*128+j*16+k]+m[4*8+7];
                m[17*8+k] = temp1[i*128+j*16+64+k]+m[4*8+7];
                
                m[14*8+k] =(short)((int)m[9 *8+k]*(int)temp1[256+i*32+j*8+0]
                           +(int)m[10*8+k]*(int)temp1[256+i*32+j*8+1]
                           +(int)m[11*8+k]*(int)temp1[256+i*32+j*8+2]
                           +(int)m[12*8+k]*(int)temp1[256+i*32+j*8+3]);
                
                m[13*8+k] =(short)((int)m[9 *8+k]*(int)temp1[256+i*32+j*8+4]
                           +(int)m[10*8+k]*(int)temp1[256+i*32+j*8+5]
                           +(int)m[11*8+k]*(int)temp1[256+i*32+j*8+6]
                           +(int)m[12*8+k]*(int)temp1[256+i*32+j*8+7]);
                
                m[24*8+k] = (short)(((int)m[16*8+k]*(unsigned short)m[4*8+0])>>16);
                m[23*8+k] = (short)(((int)m[15*8+k]*(unsigned short)m[4*8+0])>>16);
                m[26*8+k] = (short)(((int)m[14*8+k]*(unsigned short)m[4*8+1])>>16);
                m[25*8+k] = (short)(((int)m[13*8+k]*(unsigned short)m[4*8+1])>>16);
                m[21*8+k] = (short)(((int)m[16*8+k]*(unsigned short)m[4*8+2])>>16);
                m[22*8+k] = (short)(((int)m[15*8+k]*(unsigned short)m[4*8+2])>>16);
                m[28*8+k] = (short)(((int)m[14*8+k]*(unsigned short)m[4*8+3])>>16);
                m[27*8+k] = (short)(((int)m[13*8+k]*(unsigned short)m[4*8+3])>>16);
                
                m[24*8+k] += m[16*8+k];
                m[23*8+k] += m[15*8+k];
                m[26*8+k] += m[21*8+k];
                m[25*8+k] += m[22*8+k];
                m[28*8+k] += m[14*8+k];
                m[27*8+k] += m[13*8+k];
                m[24*8+k] += m[18*8+k];
                m[23*8+k] += m[17*8+k];
                m[26*8+k] = m[18*8+k] - m[26*8+k];
                m[25*8+k] = m[17*8+k] - m[25*8+k];
                m[28*8+k] += m[18*8+k];
                m[27*8+k] += m[17*8+k];
                
                m[23*8+k] = m[23*8+k] >= 0 ? m[23*8+k] : 0;
                m[24*8+k] = m[24*8+k] >= 0 ? m[24*8+k] : 0;
                m[25*8+k] = m[25*8+k] >= 0 ? m[25*8+k] : 0;
                m[26*8+k] = m[26*8+k] >= 0 ? m[26*8+k] : 0;
                m[27*8+k] = m[27*8+k] >= 0 ? m[27*8+k] : 0;
                m[28*8+k] = m[28*8+k] >= 0 ? m[28*8+k] : 0;
                
                m[23*8+k] = m[23*8+k] < m[4*8+4] ? m[23*8+k] : m[4*8+4];
                m[24*8+k] = m[24*8+k] < m[4*8+4] ? m[24*8+k] : m[4*8+4];
                m[25*8+k] = m[25*8+k] < m[4*8+4] ? m[25*8+k] : m[4*8+4];
                m[26*8+k] = m[26*8+k] < m[4*8+4] ? m[26*8+k] : m[4*8+4];
                m[27*8+k] = m[27*8+k] < m[4*8+4] ? m[27*8+k] : m[4*8+4];
                m[28*8+k] = m[28*8+k] < m[4*8+4] ? m[28*8+k] : m[4*8+4];
                
                m[23*8+k] = (short)(((int)m[23*8+k] * (unsigned short)m[4*8+6])>>16);
                m[24*8+k] = (short)(((int)m[24*8+k] * (unsigned short)m[4*8+6])>>16);
                m[25*8+k] = (short)(((int)m[25*8+k] * (unsigned short)m[4*8+6])>>16);
                m[26*8+k] = (short)(((int)m[26*8+k] * (unsigned short)m[4*8+6])>>16);
                m[27*8+k] = (short)(((int)m[27*8+k] * (unsigned short)m[4*8+6])>>16);
                m[28*8+k] = (short)(((int)m[28*8+k] * (unsigned short)m[4*8+6])>>16);
                
                m[23*8+k] = (short)((unsigned short)m[23*8+k] * (int)m[1*8+3]);
                m[24*8+k] = (short)((unsigned short)m[24*8+k] * (int)m[1*8+3]);
                m[25*8+k] = (short)((int)m[25*8+k] * (int)m[1*8+4]);
                m[26*8+k] = (short)((int)m[26*8+k] * (int)m[1*8+4]);
                m[27*8+k] = (short)((int)m[27*8+k] * (int)m[1*8+5]);
                m[28*8+k] = (short)((int)m[28*8+k] * (int)m[1*8+5]);
                
                m[18*8+k] = temp1[i*128+j*16+8+k] + m[4*8+7];
                m[17*8+k] = temp1[i*128+j*16+8+64+k] + m[4*8+7];
                
                m[24*8+k] |= m[26*8+k];
                m[23*8+k] |= m[25*8+k];
                
                m[20*8+k] = (short)(((int)m[16*8+k] * (unsigned short)m[4*8+0])>>16);
                m[19*8+k] = (short)(((int)m[15*8+k] * (unsigned short)m[4*8+0])>>16);
                
                m[30*8+k] = m[24*8+k] | m[28*8+k];
                m[29*8+k] = m[23*8+k] | m[27*8+k];
                
                m[26*8+k] = (short)(((int)m[14*8+k] * (unsigned short)m[4*8+1])>>16);
                m[25*8+k] = (short)(((int)m[13*8+k] * (unsigned short)m[4*8+1])>>16);
                m[21*8+k] = (short)(((int)m[16*8+k] * (unsigned short)m[4*8+2])>>16);
                m[22*8+k] = (short)(((int)m[15*8+k] * (unsigned short)m[4*8+2])>>16);
                m[28*8+k] = (short)(((int)m[14*8+k] * (unsigned short)m[4*8+3])>>16);
                m[27*8+k] = (short)(((int)m[13*8+k] * (unsigned short)m[4*8+3])>>16);
                
                m[30*8+k] |= m[1*8+6];
                m[29*8+k] |= m[1*8+6];
                
                pic[(i*128+j*32+0+k)^S] = m[30*8+k];
                pic[(i*128+j*32+8+k)^S] = m[29*8+k];
                
                m[24*8+k] = m[20*8+k] + m[16*8+k];
                m[23*8+k] = m[19*8+k] + m[15*8+k];
                
                m[26*8+k] += m[21*8+k];
                m[25*8+k] += m[22*8+k];
                m[28*8+k] += m[14*8+k];
                m[27*8+k] += m[13*8+k];
                m[24*8+k] += m[18*8+k];
                m[23*8+k] += m[17*8+k];
                
                m[26*8+k] = m[18*8+k] - m[26*8+k];
                m[25*8+k] = m[17*8+k] - m[25*8+k];
                
                m[28*8+k] += m[18*8+k];
                m[27*8+k] += m[17*8+k];
                
                m[23*8+k] = m[23*8+k] >= 0 ? m[23*8+k] : 0;
                m[24*8+k] = m[24*8+k] >= 0 ? m[24*8+k] : 0;
                m[25*8+k] = m[25*8+k] >= 0 ? m[25*8+k] : 0;
                m[26*8+k] = m[26*8+k] >= 0 ? m[26*8+k] : 0;
                m[27*8+k] = m[27*8+k] >= 0 ? m[27*8+k] : 0;
                m[28*8+k] = m[28*8+k] >= 0 ? m[28*8+k] : 0;
                
                m[23*8+k] = m[23*8+k] < m[4*8+4] ? m[23*8+k] : m[4*8+4];
                m[24*8+k] = m[24*8+k] < m[4*8+4] ? m[24*8+k] : m[4*8+4];
                m[25*8+k] = m[25*8+k] < m[4*8+4] ? m[25*8+k] : m[4*8+4];
                m[26*8+k] = m[26*8+k] < m[4*8+4] ? m[26*8+k] : m[4*8+4];
                m[27*8+k] = m[27*8+k] < m[4*8+4] ? m[27*8+k] : m[4*8+4];
                m[28*8+k] = m[28*8+k] < m[4*8+4] ? m[28*8+k] : m[4*8+4];
                
                m[23*8+k] = (short)(((int)m[23*8+k] * (unsigned short)m[4*8+6])>>16);
                m[24*8+k] = (short)(((int)m[24*8+k] * (unsigned short)m[4*8+6])>>16);
                m[25*8+k] = (short)(((int)m[25*8+k] * (unsigned short)m[4*8+6])>>16);
                m[26*8+k] = (short)(((int)m[26*8+k] * (unsigned short)m[4*8+6])>>16);
                m[27*8+k] = (short)(((int)m[27*8+k] * (unsigned short)m[4*8+6])>>16);
                m[28*8+k] = (short)(((int)m[28*8+k] * (unsigned short)m[4*8+6])>>16);
                
                m[23*8+k] = (short)((unsigned short)m[23*8+k] * (int)m[1*8+3]);
                m[24*8+k] = (short)((unsigned short)m[24*8+k] * (int)m[1*8+3]);
                m[25*8+k] = (short)((int)m[25*8+k] * (int)m[1*8+4]);
                m[26*8+k] = (short)((int)m[26*8+k] * (int)m[1*8+4]);
                m[27*8+k] = (short)((int)m[27*8+k] * (int)m[1*8+5]);
                m[28*8+k] = (short)((int)m[28*8+k] * (int)m[1*8+5]);
                
                pic[(i*128+j*32+16+k)^S] = m[24*8+k] | m[26*8+k] | m[28*8+k] | m[1*8+6];
                pic[(i*128+j*32+24+k)^S] = m[23*8+k] | m[25*8+k] | m[27*8+k] | m[1*8+6];
             }
            }
           }
      }
    pic += len1/2;
     } while (w-- != 1 && !(*rsp.SP_STATUS_REG & 0x80));
   
   pic -= len1 * jpg_data.w / 2;
   free(temp2);
   free(temp1);
}

