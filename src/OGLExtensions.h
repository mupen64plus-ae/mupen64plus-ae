/* OGLExtensions.h
Copyright (C) 2009 Richard Goedeken

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

/* This header file contains function pointers to some OpenGL functions */
/* This is only necessary because Windows does not contain development support for OpenGL versions beyond 1.1 */

#if !defined(OGL_EXTENSIONS_H)
#define OGL_EXTENSIONS_H

#include <SDL_opengl.h>

void OGLExtensions_Init(void);

extern PFNGLCOMBINERPARAMETERFVNVPROC     pglCombinerParameterfvNV;
extern PFNGLFINALCOMBINERINPUTNVPROC      pglFinalCombinerInputNV;
extern PFNGLCOMBINEROUTPUTNVPROC          pglCombinerOutputNV;
extern PFNGLCOMBINERINPUTNVPROC           pglCombinerInputNV;
extern PFNGLCOMBINERPARAMETERINVPROC      pglCombinerParameteriNV;

extern PFNGLACTIVETEXTUREPROC             pglActiveTexture;
extern PFNGLACTIVETEXTUREARBPROC          pglActiveTextureARB;
extern PFNGLMULTITEXCOORD2FPROC           pglMultiTexCoord2f;
extern PFNGLMULTITEXCOORD2FVPROC          pglMultiTexCoord2fv;
extern PFNGLDELETEPROGRAMSARBPROC         pglDeleteProgramsARB;
extern PFNGLPROGRAMSTRINGARBPROC          pglProgramStringARB;
extern PFNGLBINDPROGRAMARBPROC            pglBindProgramARB;
extern PFNGLGENPROGRAMSARBPROC            pglGenProgramsARB;
extern PFNGLPROGRAMENVPARAMETER4FVARBPROC pglProgramEnvParameter4fvARB;
extern PFNGLFOGCOORDPOINTEREXTPROC        pglFogCoordPointerEXT;
extern PFNGLCLIENTACTIVETEXTUREARBPROC    pglClientActiveTextureARB;

#endif  // OGL_EXTENSIONS_H

