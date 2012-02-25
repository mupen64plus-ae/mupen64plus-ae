/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - jpeg.c                                          *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2012 Bobby Smiles                                       *
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

#define M64P_PLUGIN_PROTOTYPES 1
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


// transposed JPEG QTable
static unsigned QTable_T[64] = {
	16, 12, 14, 14,  18,  24,  49,  72,
	11, 12, 13, 17,  22,  35,  64,  92,
	10, 14, 16, 22,  37,  55,  78,  95,
	16, 19, 24, 29,  56,  64,  87,  98,
	24, 26, 40, 51,  68,  81, 103, 112,
	40, 58, 57, 87, 109, 104, 121, 100,
	51, 60, 69, 80, 103, 113, 120, 103,
	61, 55, 56, 62,  77,  92, 101,  99
};

// ZigZag indices
static unsigned ZigZag[64] = {
	 0,  1,  5,  6, 14, 15, 27, 28,
	 2,  4,  7, 13, 16, 26, 29, 42,
	 3,  8, 12, 17, 25, 30, 41, 43,
	 9, 11, 18, 24, 31, 40, 44, 53,
	10, 19, 23, 32, 39, 45, 52, 54,
	20, 22, 33, 38, 46, 51, 55, 60,
	21, 34, 37, 47, 50, 56, 59, 61,
	35, 36, 48, 49, 57, 58, 62, 63
};

// Lazy way of transposing a block
static unsigned Transpose[64] = {
	0,  8, 16, 24, 32, 40, 48, 56,
	1,  9, 17, 25, 33, 41, 49, 57,
	2, 10, 18, 26, 34, 42, 50, 58,
	3, 11, 19, 27, 35, 43, 51, 59,
	4, 12, 20, 28, 36, 44, 52, 60,
	5, 13, 21, 29, 37, 45, 53, 61,
	6, 14, 22, 30, 38, 46, 54, 62,
	7, 15, 23, 31, 39, 47, 55, 63
};

static inline const unsigned char clamp(short x) {
		return (x & (0xff00)) ? ((-x) >> 15) & 0xff : x;
}

void ob_jpg_uncompress(OSTask_t *task)
{
	// Fetch arguments
	unsigned pBuffer = task->data_ptr;
	unsigned nMacroBlocks = task->data_size;
	signed QScale = task->yield_data_size;

	DebugMessage(M64MSG_INFO,
		"OB Task: *buffer=%x, #MB=%d, Qscale=%d\n",
		pBuffer, nMacroBlocks, QScale);

	// Rescale QTable if needed
	unsigned i;
	unsigned qtable[64];

	if (QScale != 0) {
		if (QScale > 0) {
			for(i = 0; i < 64; i++) {
				unsigned q  = QTable_T[i] * QScale;
				if (q > 32767) q = 32767;
				qtable[i] = q;
			}
		}
		else {
			unsigned Shift = -QScale;
			for(i = 0; i < 64; i++) {
				qtable[i] = QTable_T[i] >> Shift;
			}
		}
	}

	unsigned mb;

	int y_dc = 0;
	int u_dc = 0;
	int v_dc = 0;


	// foreach MB
	for(mb=0; mb < nMacroBlocks; mb++) {
		unsigned sb;
		short macroblock[2][0x300/2];

		// load MB into short_buffer
		unsigned offset = pBuffer + 0x300*mb;
		for(i = 0; i < 0x300/2; i++) {
			unsigned short s = rsp.RDRAM[(offset+0)^S8];
			s <<= 8;
			s += rsp.RDRAM[(offset+1)^S8];
			macroblock[0][i] = s;
			offset += 2;
		}

		// foreach SB
		for(sb = 0; sb < 6; sb++) {

			// apply delta to DC
			int dc = (signed)macroblock[0][sb*0x40];
			switch(sb) {
			case 0: case 1: case 2: case 3: y_dc += dc; macroblock[1][sb*0x40] = y_dc & 0xffff; break;
			case 4: u_dc += dc; macroblock[1][sb*0x40] = u_dc & 0xffff; break;
			case 5: v_dc += dc; macroblock[1][sb*0x40] = v_dc & 0xffff; break;
			}

			// zigzag reordering
			for(i = 1; i < 64; i++) {
				macroblock[1][sb*0x40+i] = macroblock[0][sb*0x40+ZigZag[i]];
			}

			// Apply Dequantization
			if (QScale != 0) {
				for(i = 0; i < 64; i++) {
					int v = macroblock[1][sb*0x40+i] * qtable[i];
					if (v > 32767) { v = 32767; }
					if (v < -32768) { v = -32768; }
					macroblock[1][sb*0x40+i] = (short)v;
				}
			}

			// Transpose
			for(i = 0; i < 64; i++) {
					macroblock[0][sb*0x40+i] = macroblock[1][sb*0x40+Transpose[i]];
			}

			// Apply Invert Discrete Cosinus Transform
			idct(&macroblock[0][sb*0x40], &macroblock[1][sb*0x40]);

			// Clamp values between [0..255]
			for(i = 0; i < 64; i++) {
				macroblock[0][sb*0x40+i] = clamp(macroblock[1][sb*0x40+i]);
			}
		}

		// Texel Formatting
		unsigned y_offset = 0;
		offset = pBuffer + 0x300*mb;
		for(i = 0; i < 8; i++) {
			// U
			rsp.RDRAM[(offset+0x00)^S8] = (unsigned char)macroblock[0][(0x200 + i*0x10)/2];
			rsp.RDRAM[(offset+0x04)^S8] = (unsigned char)macroblock[0][(0x202 + i*0x10)/2];
			rsp.RDRAM[(offset+0x08)^S8] = (unsigned char)macroblock[0][(0x204 + i*0x10)/2];
			rsp.RDRAM[(offset+0x0c)^S8] = (unsigned char)macroblock[0][(0x206 + i*0x10)/2];
			rsp.RDRAM[(offset+0x10)^S8] = (unsigned char)macroblock[0][(0x208 + i*0x10)/2];
			rsp.RDRAM[(offset+0x14)^S8] = (unsigned char)macroblock[0][(0x20a + i*0x10)/2];
			rsp.RDRAM[(offset+0x18)^S8] = (unsigned char)macroblock[0][(0x20c + i*0x10)/2];
			rsp.RDRAM[(offset+0x1c)^S8] = (unsigned char)macroblock[0][(0x20e + i*0x10)/2];
			rsp.RDRAM[(offset+0x20)^S8] = (unsigned char)macroblock[0][(0x200 + i*0x10)/2];
			rsp.RDRAM[(offset+0x24)^S8] = (unsigned char)macroblock[0][(0x202 + i*0x10)/2];
			rsp.RDRAM[(offset+0x28)^S8] = (unsigned char)macroblock[0][(0x204 + i*0x10)/2];
			rsp.RDRAM[(offset+0x2c)^S8] = (unsigned char)macroblock[0][(0x206 + i*0x10)/2];
			rsp.RDRAM[(offset+0x30)^S8] = (unsigned char)macroblock[0][(0x208 + i*0x10)/2];
			rsp.RDRAM[(offset+0x34)^S8] = (unsigned char)macroblock[0][(0x20a + i*0x10)/2];
			rsp.RDRAM[(offset+0x38)^S8] = (unsigned char)macroblock[0][(0x20c + i*0x10)/2];
			rsp.RDRAM[(offset+0x3c)^S8] = (unsigned char)macroblock[0][(0x20e + i*0x10)/2];

			// V
			rsp.RDRAM[(offset+0x02)^S8] = (unsigned char)macroblock[0][(0x280 + i*0x10)/2];
			rsp.RDRAM[(offset+0x06)^S8] = (unsigned char)macroblock[0][(0x282 + i*0x10)/2];
			rsp.RDRAM[(offset+0x0a)^S8] = (unsigned char)macroblock[0][(0x284 + i*0x10)/2];
			rsp.RDRAM[(offset+0x0e)^S8] = (unsigned char)macroblock[0][(0x286 + i*0x10)/2];
			rsp.RDRAM[(offset+0x12)^S8] = (unsigned char)macroblock[0][(0x288 + i*0x10)/2];
			rsp.RDRAM[(offset+0x16)^S8] = (unsigned char)macroblock[0][(0x28a + i*0x10)/2];
			rsp.RDRAM[(offset+0x1a)^S8] = (unsigned char)macroblock[0][(0x28c + i*0x10)/2];
			rsp.RDRAM[(offset+0x1e)^S8] = (unsigned char)macroblock[0][(0x28e + i*0x10)/2];
			rsp.RDRAM[(offset+0x22)^S8] = (unsigned char)macroblock[0][(0x280 + i*0x10)/2];
			rsp.RDRAM[(offset+0x26)^S8] = (unsigned char)macroblock[0][(0x282 + i*0x10)/2];
			rsp.RDRAM[(offset+0x2a)^S8] = (unsigned char)macroblock[0][(0x284 + i*0x10)/2];
			rsp.RDRAM[(offset+0x2e)^S8] = (unsigned char)macroblock[0][(0x286 + i*0x10)/2];
			rsp.RDRAM[(offset+0x32)^S8] = (unsigned char)macroblock[0][(0x288 + i*0x10)/2];
			rsp.RDRAM[(offset+0x36)^S8] = (unsigned char)macroblock[0][(0x28a + i*0x10)/2];
			rsp.RDRAM[(offset+0x3a)^S8] = (unsigned char)macroblock[0][(0x28c + i*0x10)/2];
			rsp.RDRAM[(offset+0x3e)^S8] = (unsigned char)macroblock[0][(0x28e + i*0x10)/2];

			// Ya/Yb
			rsp.RDRAM[(offset+0x01)^S8] = (unsigned char)macroblock[0][(y_offset + 0x00)/2];
			rsp.RDRAM[(offset+0x03)^S8] = (unsigned char)macroblock[0][(y_offset + 0x02)/2];
			rsp.RDRAM[(offset+0x05)^S8] = (unsigned char)macroblock[0][(y_offset + 0x04)/2];
			rsp.RDRAM[(offset+0x07)^S8] = (unsigned char)macroblock[0][(y_offset + 0x06)/2];
			rsp.RDRAM[(offset+0x09)^S8] = (unsigned char)macroblock[0][(y_offset + 0x08)/2];
			rsp.RDRAM[(offset+0x0b)^S8] = (unsigned char)macroblock[0][(y_offset + 0x0a)/2];
			rsp.RDRAM[(offset+0x0d)^S8] = (unsigned char)macroblock[0][(y_offset + 0x0c)/2];
			rsp.RDRAM[(offset+0x0f)^S8] = (unsigned char)macroblock[0][(y_offset + 0x0e)/2];
			rsp.RDRAM[(offset+0x21)^S8] = (unsigned char)macroblock[0][(y_offset + 0x10)/2];
			rsp.RDRAM[(offset+0x23)^S8] = (unsigned char)macroblock[0][(y_offset + 0x12)/2];
			rsp.RDRAM[(offset+0x25)^S8] = (unsigned char)macroblock[0][(y_offset + 0x14)/2];
			rsp.RDRAM[(offset+0x27)^S8] = (unsigned char)macroblock[0][(y_offset + 0x16)/2];
			rsp.RDRAM[(offset+0x29)^S8] = (unsigned char)macroblock[0][(y_offset + 0x18)/2];
			rsp.RDRAM[(offset+0x2b)^S8] = (unsigned char)macroblock[0][(y_offset + 0x1a)/2];
			rsp.RDRAM[(offset+0x2d)^S8] = (unsigned char)macroblock[0][(y_offset + 0x1c)/2];
			rsp.RDRAM[(offset+0x2f)^S8] = (unsigned char)macroblock[0][(y_offset + 0x1e)/2];

			// Ya+1/Yb+1
			rsp.RDRAM[(offset+0x11)^S8] = (unsigned char)macroblock[0][(y_offset + 0x80)/2];
			rsp.RDRAM[(offset+0x13)^S8] = (unsigned char)macroblock[0][(y_offset + 0x82)/2];
			rsp.RDRAM[(offset+0x15)^S8] = (unsigned char)macroblock[0][(y_offset + 0x84)/2];
			rsp.RDRAM[(offset+0x17)^S8] = (unsigned char)macroblock[0][(y_offset + 0x86)/2];
			rsp.RDRAM[(offset+0x19)^S8] = (unsigned char)macroblock[0][(y_offset + 0x88)/2];
			rsp.RDRAM[(offset+0x1b)^S8] = (unsigned char)macroblock[0][(y_offset + 0x8a)/2];
			rsp.RDRAM[(offset+0x1d)^S8] = (unsigned char)macroblock[0][(y_offset + 0x8c)/2];
			rsp.RDRAM[(offset+0x1f)^S8] = (unsigned char)macroblock[0][(y_offset + 0x8e)/2];
			rsp.RDRAM[(offset+0x31)^S8] = (unsigned char)macroblock[0][(y_offset + 0x90)/2];
			rsp.RDRAM[(offset+0x33)^S8] = (unsigned char)macroblock[0][(y_offset + 0x92)/2];
			rsp.RDRAM[(offset+0x35)^S8] = (unsigned char)macroblock[0][(y_offset + 0x94)/2];
			rsp.RDRAM[(offset+0x37)^S8] = (unsigned char)macroblock[0][(y_offset + 0x96)/2];
			rsp.RDRAM[(offset+0x39)^S8] = (unsigned char)macroblock[0][(y_offset + 0x98)/2];
			rsp.RDRAM[(offset+0x3b)^S8] = (unsigned char)macroblock[0][(y_offset + 0x9a)/2];
			rsp.RDRAM[(offset+0x3d)^S8] = (unsigned char)macroblock[0][(y_offset + 0x9c)/2];
			rsp.RDRAM[(offset+0x3f)^S8] = (unsigned char)macroblock[0][(y_offset + 0x9e)/2];

			offset += 0x40;
			y_offset += (i == 3) ? 0xa0 : 0x20;
		}
	}
}

