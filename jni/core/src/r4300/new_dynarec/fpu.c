/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - fpu.c                                                   *
 *   Copyright (C) 2010 Ari64                                              *
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

#include <math.h>

extern int FCR0, FCR31;

void cvt_s_w(int *source,float *dest)
{
  *dest = *source;
}
void cvt_d_w(int *source,double *dest)
{
  *dest = *source;
}
void cvt_s_l(long long *source,float *dest)
{
  *dest = *source;
}
void cvt_d_l(long long *source,double *dest)
{
  *dest = *source;
}
void cvt_d_s(float *source,double *dest)
{
  *dest = *source;
}
void cvt_s_d(double *source,float *dest)
{
  *dest = *source;
}

void round_l_s(float *source,long long *dest)
{
  *dest = roundf(*source);
}
void round_w_s(float *source,int *dest)
{
  *dest = roundf(*source);
}
void trunc_l_s(float *source,long long *dest)
{
  *dest = truncf(*source);
}
void trunc_w_s(float *source,int *dest)
{
  *dest = truncf(*source);
}
void ceil_l_s(float *source,long long *dest)
{
  *dest = ceilf(*source);
}
void ceil_w_s(float *source,int *dest)
{
  *dest = ceilf(*source);
}
void floor_l_s(float *source,long long *dest)
{
  *dest = floorf(*source);
}
void floor_w_s(float *source,int *dest)
{
  *dest = floorf(*source);
}

void round_l_d(double *source,long long *dest)
{
  *dest = round(*source);
}
void round_w_d(double *source,int *dest)
{
  *dest = round(*source);
}
void trunc_l_d(double *source,long long *dest)
{
  *dest = trunc(*source);
}
void trunc_w_d(double *source,int *dest)
{
  *dest = trunc(*source);
}
void ceil_l_d(double *source,long long *dest)
{
  *dest = ceil(*source);
}
void ceil_w_d(double *source,int *dest)
{
  *dest = ceil(*source);
}
void floor_l_d(double *source,long long *dest)
{
  *dest = floor(*source);
}
void floor_w_d(double *source,int *dest)
{
  *dest = floor(*source);
}

void cvt_w_s(float *source,int *dest)
{
  switch(FCR31&3)
  {
    case 0: round_w_s(source,dest);return;
    case 1: trunc_w_s(source,dest);return;
    case 2: ceil_w_s(source,dest);return;
    case 3: floor_w_s(source,dest);return;
  }
}
void cvt_w_d(double *source,int *dest)
{
  switch(FCR31&3)
  {
    case 0: round_w_d(source,dest);return;
    case 1: trunc_w_d(source,dest);return;
    case 2: ceil_w_d(source,dest);return;
    case 3: floor_w_d(source,dest);return;
  }
}
void cvt_l_s(float *source,long long *dest)
{
  switch(FCR31&3)
  {
    case 0: round_l_s(source,dest);return;
    case 1: trunc_l_s(source,dest);return;
    case 2: ceil_l_s(source,dest);return;
    case 3: floor_l_s(source,dest);return;
  }
}
void cvt_l_d(double *source,long long *dest)
{
  switch(FCR31&3)
  {
    case 0: round_l_d(source,dest);return;
    case 1: trunc_l_d(source,dest);return;
    case 2: ceil_l_d(source,dest);return;
    case 3: floor_l_d(source,dest);return;
  }
}

void c_f_s()
{
  FCR31 &= ~0x800000;
}
void c_un_s(float *source,float *target)
{
  FCR31=(isnan(*source) || isnan(*target)) ? FCR31|0x800000 : FCR31&~0x800000;
}
                          
void c_eq_s(float *source,float *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31&=~0x800000;return;}
  FCR31 = *source==*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ueq_s(float *source,float *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31|=0x800000;return;}
  FCR31 = *source==*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_olt_s(float *source,float *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31&=~0x800000;return;}
  FCR31 = *source<*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ult_s(float *source,float *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31|=0x800000;return;}
  FCR31 = *source<*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_ole_s(float *source,float *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31&=~0x800000;return;}
  FCR31 = *source<=*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ule_s(float *source,float *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31|=0x800000;return;}
  FCR31 = *source<=*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_sf_s(float *source,float *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31&=~0x800000;
}
void c_ngle_s(float *source,float *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31&=~0x800000;
}

void c_seq_s(float *source,float *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source==*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ngl_s(float *source,float *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source==*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_lt_s(float *source,float *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source<*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_nge_s(float *source,float *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source<*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_le_s(float *source,float *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source<=*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ngt_s(float *source,float *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source<=*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_f_d()
{
  FCR31 &= ~0x800000;
}
void c_un_d(double *source,double *target)
{
  FCR31=(isnan(*source) || isnan(*target)) ? FCR31|0x800000 : FCR31&~0x800000;
}
                          
void c_eq_d(double *source,double *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31&=~0x800000;return;}
  FCR31 = *source==*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ueq_d(double *source,double *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31|=0x800000;return;}
  FCR31 = *source==*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_olt_d(double *source,double *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31&=~0x800000;return;}
  FCR31 = *source<*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ult_d(double *source,double *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31|=0x800000;return;}
  FCR31 = *source<*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_ole_d(double *source,double *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31&=~0x800000;return;}
  FCR31 = *source<=*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ule_d(double *source,double *target)
{
  if (isnan(*source) || isnan(*target)) {FCR31|=0x800000;return;}
  FCR31 = *source<=*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_sf_d(double *source,double *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31&=~0x800000;
}
void c_ngle_d(double *source,double *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31&=~0x800000;
}

void c_seq_d(double *source,double *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source==*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ngl_d(double *source,double *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source==*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_lt_d(double *source,double *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source<*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_nge_d(double *source,double *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source<*target ? FCR31|0x800000 : FCR31&~0x800000;
}

void c_le_d(double *source,double *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source<=*target ? FCR31|0x800000 : FCR31&~0x800000;
}
void c_ngt_d(double *source,double *target)
{
  //if (isnan(*source) || isnan(*target)) // FIXME - exception
  FCR31 = *source<=*target ? FCR31|0x800000 : FCR31&~0x800000;
}


void add_s(float *source1,float *source2,float *target)
{
  *target=(*source1)+(*source2);
}
void sub_s(float *source1,float *source2,float *target)
{
  *target=(*source1)-(*source2);
}
void mul_s(float *source1,float *source2,float *target)
{
  *target=(*source1)*(*source2);
}
void div_s(float *source1,float *source2,float *target)
{
  *target=(*source1)/(*source2);
}
void sqrt_s(float *source,float *target)
{
  *target=sqrtf(*source);
}
void abs_s(float *source,float *target)
{
  *target=fabsf(*source);
}
void mov_s(float *source,float *target)
{
  *target=*source;
}
void neg_s(float *source,float *target)
{
  *target=-(*source);
}
void add_d(double *source1,double *source2,double *target)
{
  *target=(*source1)+(*source2);
}
void sub_d(double *source1,double *source2,double *target)
{
  *target=(*source1)-(*source2);
}
void mul_d(double *source1,double *source2,double *target)
{
  *target=(*source1)*(*source2);
}
void div_d(double *source1,double *source2,double *target)
{
  *target=(*source1)/(*source2);
}
void sqrt_d(double *source,double *target)
{
  *target=sqrt(*source);
}
void abs_d(double *source,double *target)
{
  *target=fabs(*source);
}
void mov_d(double *source,double *target)
{
  *target=*source;
}
void neg_d(double *source,double *target)
{
  *target=-(*source);
}

