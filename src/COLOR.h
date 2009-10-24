/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - COLOR.h                                                 *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2002 Rice1964                                           *
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

#ifndef XCOLOR_H
#define XCOLOR_H

typedef struct _COLORVALUE 
{
   
       float r;
       float g;
       float b;
       float a;
} COLORVALUE;


typedef struct XCOLOR {
   FLOAT r, g, b, a;
#ifdef __cplusplus
 public:
   XCOLOR() 
     {
     }
   
   XCOLOR( DWORD argb );
   XCOLOR( CONST FLOAT * );
   XCOLOR( CONST COLORVALUE& );
   XCOLOR( FLOAT r, FLOAT g, FLOAT b, FLOAT a );
   
   // casting
   operator DWORD () const;
   
   operator FLOAT* ();
   operator CONST FLOAT* () const;
   
   operator COLORVALUE* ();
   operator CONST COLORVALUE* () const;
    
   operator COLORVALUE& ();
   operator CONST COLORVALUE& () const;
   
   // assignment operators
   XCOLOR& operator += ( CONST XCOLOR& );
   XCOLOR& operator -= ( CONST XCOLOR& );
   XCOLOR& operator *= ( FLOAT );
   XCOLOR& operator /= ( FLOAT );
   
   // unary operators
   XCOLOR operator + () const;
   XCOLOR operator - () const;
   
   // binary operators
   XCOLOR operator + ( CONST XCOLOR& ) const;
   XCOLOR operator - ( CONST XCOLOR& ) const;
   XCOLOR operator * ( FLOAT ) const;
   XCOLOR operator / ( FLOAT ) const;
   
   friend XCOLOR operator * (FLOAT, CONST XCOLOR& );
    
   BOOL operator == ( CONST XCOLOR& ) const;
   BOOL operator != ( CONST XCOLOR& ) const;
   
#endif //__cplusplus
} XCOLOR;

#endif

