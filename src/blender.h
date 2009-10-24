/*
Copyright (C) 2003 Rice1964

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

#ifndef _BLENDER_H_
#define _BLENDER_H_

class CRender;

class CBlender
{
public:
    virtual ~CBlender() {}
    
    virtual void InitBlenderMode(void);
    virtual void NormalAlphaBlender(void)=0;
    virtual void DisableAlphaBlender(void)=0;
    
    virtual void BlendFunc(uint32 srcFunc, uint32 desFunc)=0;

    virtual void Enable()=0;
    virtual void Disable()=0;
protected:
    CBlender(CRender *pRender) : m_pRender(pRender) {}
    CRender *m_pRender;
};

#endif



