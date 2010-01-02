/* OGLExtensions.cpp
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

/* This source file contains code for assigning function pointers to some OpenGL functions */
/* This is only necessary because Windows does not contain development support for OpenGL versions beyond 1.1 */

#include <SDL_opengl.h>
#include "Video.h"

static void APIENTRY EmptyFunc(void) { return; }

PFNGLCOMBINERPARAMETERFVNVPROC     pglCombinerParameterfvNV = (PFNGLCOMBINERPARAMETERFVNVPROC) EmptyFunc;
PFNGLFINALCOMBINERINPUTNVPROC      pglFinalCombinerInputNV = (PFNGLFINALCOMBINERINPUTNVPROC) EmptyFunc;
PFNGLCOMBINEROUTPUTNVPROC          pglCombinerOutputNV = (PFNGLCOMBINEROUTPUTNVPROC) EmptyFunc;
PFNGLCOMBINERINPUTNVPROC           pglCombinerInputNV = (PFNGLCOMBINERINPUTNVPROC) EmptyFunc;
PFNGLCOMBINERPARAMETERINVPROC      pglCombinerParameteriNV = (PFNGLCOMBINERPARAMETERINVPROC) EmptyFunc;

PFNGLACTIVETEXTUREPROC             pglActiveTexture = (PFNGLACTIVETEXTUREPROC) EmptyFunc;
PFNGLACTIVETEXTUREARBPROC          pglActiveTextureARB = (PFNGLACTIVETEXTUREARBPROC) EmptyFunc;
PFNGLMULTITEXCOORD2FPROC           pglMultiTexCoord2f = (PFNGLMULTITEXCOORD2FPROC) EmptyFunc;
PFNGLMULTITEXCOORD2FVPROC          pglMultiTexCoord2fv = (PFNGLMULTITEXCOORD2FVPROC) EmptyFunc;
PFNGLDELETEPROGRAMSARBPROC         pglDeleteProgramsARB = (PFNGLDELETEPROGRAMSARBPROC) EmptyFunc;
PFNGLPROGRAMSTRINGARBPROC          pglProgramStringARB = (PFNGLPROGRAMSTRINGARBPROC) EmptyFunc;
PFNGLBINDPROGRAMARBPROC            pglBindProgramARB = (PFNGLBINDPROGRAMARBPROC) EmptyFunc;
PFNGLGENPROGRAMSARBPROC            pglGenProgramsARB = (PFNGLGENPROGRAMSARBPROC) EmptyFunc;
PFNGLPROGRAMENVPARAMETER4FVARBPROC pglProgramEnvParameter4fvARB = (PFNGLPROGRAMENVPARAMETER4FVARBPROC) EmptyFunc;
PFNGLFOGCOORDPOINTEREXTPROC        pglFogCoordPointerEXT = (PFNGLFOGCOORDPOINTEREXTPROC) EmptyFunc;
PFNGLCLIENTACTIVETEXTUREARBPROC    pglClientActiveTextureARB = (PFNGLCLIENTACTIVETEXTUREARBPROC) EmptyFunc;

#define INIT_ENTRY_POINT(type, funcname) \
  p##funcname = (type) CoreVideo_GL_GetProcAddress(#funcname); \
  if (p##funcname == NULL) { DebugMessage(M64MSG_WARNING, \
  "Couldn't get address of OpenGL function: '%s'", #funcname); p##funcname = (type) EmptyFunc; }

void OGLExtensions_Init(void)
{
    INIT_ENTRY_POINT(PFNGLCOMBINERPARAMETERFVNVPROC,     glCombinerParameterfvNV);
    INIT_ENTRY_POINT(PFNGLFINALCOMBINERINPUTNVPROC,      glFinalCombinerInputNV);
    INIT_ENTRY_POINT(PFNGLCOMBINEROUTPUTNVPROC,          glCombinerOutputNV);
    INIT_ENTRY_POINT(PFNGLCOMBINERINPUTNVPROC,           glCombinerInputNV);
    INIT_ENTRY_POINT(PFNGLCOMBINERPARAMETERINVPROC,      glCombinerParameteriNV);

    INIT_ENTRY_POINT(PFNGLACTIVETEXTUREPROC,             glActiveTexture);
    INIT_ENTRY_POINT(PFNGLACTIVETEXTUREARBPROC,          glActiveTextureARB);
    INIT_ENTRY_POINT(PFNGLMULTITEXCOORD2FPROC,           glMultiTexCoord2f);
    INIT_ENTRY_POINT(PFNGLMULTITEXCOORD2FVPROC,          glMultiTexCoord2fv);
    INIT_ENTRY_POINT(PFNGLDELETEPROGRAMSARBPROC,         glDeleteProgramsARB);
    INIT_ENTRY_POINT(PFNGLPROGRAMSTRINGARBPROC,          glProgramStringARB);
    INIT_ENTRY_POINT(PFNGLBINDPROGRAMARBPROC,            glBindProgramARB);
    INIT_ENTRY_POINT(PFNGLGENPROGRAMSARBPROC,            glGenProgramsARB);
    INIT_ENTRY_POINT(PFNGLPROGRAMENVPARAMETER4FVARBPROC, glProgramEnvParameter4fvARB);
    INIT_ENTRY_POINT(PFNGLFOGCOORDPOINTEREXTPROC,        glFogCoordPointerEXT);
    INIT_ENTRY_POINT(PFNGLCLIENTACTIVETEXTUREARBPROC,    glClientActiveTextureARB);
}


