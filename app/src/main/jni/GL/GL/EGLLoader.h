#pragma once

#ifdef OS_WINDOWS
#include <windows.h>
#elif defined(OS_LINUX)
//#define GL_GLEXT_PROTOTYPES
#include <winlnxdefs.h>
#endif

#ifdef EGL
#include <GL/gl.h>
#undef GL_VERSION_1_1
#include <GL/glcorearb.h>
#elif defined(OS_MAC_OS_X)
#include <OpenGL/OpenGL.h>
#include <stddef.h>
#include <OpenGL/gl3.h>
#else
#include <GL/gl.h>
#endif

#include <GL/glext.h>
#include <stdexcept>
#include <android/log.h>
#include <cstdlib>

#ifdef GL_ERROR_DEBUG
#define CHECKED_GL_FUNCTION(proc_name, ...) checked([&]() { proc_name(__VA_ARGS__);}, #proc_name)
#define CHECKED_GL_FUNCTION_WITH_RETURN(proc_name, ReturnType, ...) checkedWithReturn<ReturnType>([&]() { return proc_name(__VA_ARGS__);}, #proc_name)
#else
#define CHECKED_GL_FUNCTION(proc_name, ...) proc_name(__VA_ARGS__)
#define CHECKED_GL_FUNCTION_WITH_RETURN(proc_name, ReturnType, ...) proc_name(__VA_ARGS__)
#endif

#define IS_GL_FUNCTION_VALID(proc_name) g_##proc_name != NULL
#define GET_GL_FUNCTION(proc_name) g_##proc_name

#ifdef EGL

#include <EGL/egl.h>
#include <EGL/eglext.h>
#define glGetProcAddress eglGetProcAddress
#define GL_GET_PROC_ADR(proc_type, proc_name) g_##proc_name = (proc_type) glGetProcAddress(#proc_name)

#define glGetError g_glGetError
#define glBlendFunc(...) CHECKED_GL_FUNCTION(g_glBlendFunc, __VA_ARGS__)
#define glPixelStorei(...) CHECKED_GL_FUNCTION(g_glPixelStorei, __VA_ARGS__)
#define glClearColor(...) CHECKED_GL_FUNCTION(g_glClearColor, __VA_ARGS__)
#define glCullFace(...) CHECKED_GL_FUNCTION(g_glCullFace, __VA_ARGS__)
#define glDepthFunc(...) CHECKED_GL_FUNCTION(g_glDepthFunc, __VA_ARGS__)
#define glDepthMask(...) CHECKED_GL_FUNCTION(g_glDepthMask, __VA_ARGS__)
#define glDisable(...) CHECKED_GL_FUNCTION(g_glDisable, __VA_ARGS__)
#define glEnable(...) CHECKED_GL_FUNCTION(g_glEnable, __VA_ARGS__)
#define glPolygonOffset(...) CHECKED_GL_FUNCTION(g_glPolygonOffset, __VA_ARGS__)
#define glScissor(...) CHECKED_GL_FUNCTION(g_glScissor, __VA_ARGS__)
#define glViewport(...) CHECKED_GL_FUNCTION(g_glViewport, __VA_ARGS__)
#define glBindTexture(...) CHECKED_GL_FUNCTION(g_glBindTexture, __VA_ARGS__)
#define glTexImage2D(...) CHECKED_GL_FUNCTION(g_glTexImage2D, __VA_ARGS__)
#define glTexParameteri(...) CHECKED_GL_FUNCTION(g_glTexParameteri, __VA_ARGS__)
#define glGetIntegerv(...) CHECKED_GL_FUNCTION(g_glGetIntegerv, __VA_ARGS__)
#define glGetString(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glGetString, const GLubyte*, __VA_ARGS__)
#define glReadPixels(...) CHECKED_GL_FUNCTION(g_glReadPixels, __VA_ARGS__)
#define glTexSubImage2D(...) CHECKED_GL_FUNCTION(g_glTexSubImage2D, __VA_ARGS__)
#define glDrawArrays(...) CHECKED_GL_FUNCTION(g_glDrawArrays, __VA_ARGS__)
#define glDrawElements(...) CHECKED_GL_FUNCTION(g_glDrawElements, __VA_ARGS__)
#define glLineWidth(...) CHECKED_GL_FUNCTION(g_glLineWidth, __VA_ARGS__)
#define glClear(...) CHECKED_GL_FUNCTION(g_glClear, __VA_ARGS__)
#define glGetFloatv(...) CHECKED_GL_FUNCTION(g_glGetFloatv, __VA_ARGS__)
#define glDeleteTextures(...) CHECKED_GL_FUNCTION(g_glDeleteTextures, __VA_ARGS__)
#define glGenTextures(...) CHECKED_GL_FUNCTION(g_glGenTextures, __VA_ARGS__)
#define glTexParameterf(...) CHECKED_GL_FUNCTION(g_glTexParameterf, __VA_ARGS__)
#define glActiveTexture(...) CHECKED_GL_FUNCTION(g_glActiveTexture, __VA_ARGS__)
#define glBlendColor(...) CHECKED_GL_FUNCTION(g_glBlendColor, __VA_ARGS__)
#define glReadBuffer(...) CHECKED_GL_FUNCTION(g_glReadBuffer, __VA_ARGS__)
#define glFinish(...) CHECKED_GL_FUNCTION(g_glFinish, __VA_ARGS__)

#define glTexEnvi(...) CHECKED_GL_FUNCTION(g_glTexEnvi, __VA_ARGS__)
#define glActiveTextureARB(...) CHECKED_GL_FUNCTION(g_glActiveTextureARB, __VA_ARGS__)
#define glCreateShaderObjectARB(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glCreateShaderObjectARB, GLhandleARB, __VA_ARGS__)
#define glShaderSourceARB(...) CHECKED_GL_FUNCTION(g_glShaderSourceARB, __VA_ARGS__)
#define glCompileShaderARB(...) CHECKED_GL_FUNCTION(g_glCompileShaderARB, __VA_ARGS__)
#define glCreateProgramObjectARB(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glCreateProgramObjectARB, GLhandleARB, __VA_ARGS__)
#define glAttachObjectARB(...) CHECKED_GL_FUNCTION(g_glAttachObjectARB, __VA_ARGS__)
#define glLinkProgramARB(...) CHECKED_GL_FUNCTION(g_glLinkProgramARB, __VA_ARGS__)
#define glUseProgramObjectARB(...) CHECKED_GL_FUNCTION(g_glUseProgramObjectARB, __VA_ARGS__)
#define glGetObjectParameterivARB(...) CHECKED_GL_FUNCTION(g_glGetObjectParameterivARB, __VA_ARGS__)
#define glGetInfoLogARB(...) CHECKED_GL_FUNCTION(g_glGetInfoLogARB, __VA_ARGS__)
#define glGetUniformLocationARB(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glGetUniformLocationARB, GLint, __VA_ARGS__)
#define glUniform1iARB(...) CHECKED_GL_FUNCTION(g_glUniform1iARB, __VA_ARGS__)
#define glUniform4fARB(...) CHECKED_GL_FUNCTION(g_glUniform4fARB, __VA_ARGS__)
#define glUniform1fARB(...) CHECKED_GL_FUNCTION(g_glUniform1fARB, __VA_ARGS__)
#define glBlendFuncSeparateEXT(...) CHECKED_GL_FUNCTION(g_glBlendFuncSeparateEXT, __VA_ARGS__)
#define glAlphaFunc(...) CHECKED_GL_FUNCTION(g_glAlphaFunc, __VA_ARGS__)
#define glFogi(...) CHECKED_GL_FUNCTION(g_glFogi, __VA_ARGS__)
#define glFogf(...) CHECKED_GL_FUNCTION(g_glFogf, __VA_ARGS__)
#define glFogfv(...) CHECKED_GL_FUNCTION(g_glFogfv, __VA_ARGS__)
#define glCompressedTexImage2DARB(...) CHECKED_GL_FUNCTION(g_glCompressedTexImage2DARB, __VA_ARGS__)
#define glPushAttrib(...) CHECKED_GL_FUNCTION(g_glPushAttrib, __VA_ARGS__)
#define glDrawBuffer(...) CHECKED_GL_FUNCTION(g_glDrawBuffer, __VA_ARGS__)
#define glColor4ub(...) CHECKED_GL_FUNCTION(g_glColor4ub, __VA_ARGS__)
#define glBegin(...) CHECKED_GL_FUNCTION(g_glBegin, __VA_ARGS__)
#define glVertex3f(...) CHECKED_GL_FUNCTION(g_glVertex3f, __VA_ARGS__)
#define glEnd(...) CHECKED_GL_FUNCTION(g_glEnd, __VA_ARGS__)
#define glPopAttrib(...) CHECKED_GL_FUNCTION(g_glPopAttrib, __VA_ARGS__)
#define glMultiTexCoord2fARB(...) CHECKED_GL_FUNCTION(g_glMultiTexCoord2fARB, __VA_ARGS__)
#define glTexCoord2f(...) CHECKED_GL_FUNCTION(g_glTexCoord2f, __VA_ARGS__)
#define glColor4f(...) CHECKED_GL_FUNCTION(g_glColor4f, __VA_ARGS__)
#define glSecondaryColor3f(...) CHECKED_GL_FUNCTION(g_glSecondaryColor3f, __VA_ARGS__)
#define glVertex4f(...) CHECKED_GL_FUNCTION(g_glVertex4f, __VA_ARGS__)
#define glColorMask(...) CHECKED_GL_FUNCTION(g_glColorMask, __VA_ARGS__)
#define glMatrixMode(...) CHECKED_GL_FUNCTION(g_glMatrixMode, __VA_ARGS__)
#define glLoadIdentity(...) CHECKED_GL_FUNCTION(g_glLoadIdentity, __VA_ARGS__)
#define glTranslatef(...) CHECKED_GL_FUNCTION(g_glTranslatef, __VA_ARGS__)
#define glScalef(...) CHECKED_GL_FUNCTION(g_glScalef, __VA_ARGS__)
#define glGetTexLevelParameteriv(...) CHECKED_GL_FUNCTION(g_glGetTexLevelParameteriv, __VA_ARGS__)
#define glBindFramebufferEXT(...) CHECKED_GL_FUNCTION(g_glBindFramebufferEXT, __VA_ARGS__)
#define glDeleteFramebuffersEXT(...) CHECKED_GL_FUNCTION(g_glDeleteFramebuffersEXT, __VA_ARGS__)
#define glDeleteRenderbuffersEXT(...) CHECKED_GL_FUNCTION(g_glDeleteRenderbuffersEXT, __VA_ARGS__)
#define glFramebufferTexture2DEXT(...) CHECKED_GL_FUNCTION(g_glFramebufferTexture2DEXT, __VA_ARGS__)
#define glBindRenderbufferEXT(...) CHECKED_GL_FUNCTION(g_glBindRenderbufferEXT, __VA_ARGS__)
#define glFramebufferRenderbufferEXT(...) CHECKED_GL_FUNCTION(g_glFramebufferRenderbufferEXT, __VA_ARGS__)
#define glCheckFramebufferStatusEXT(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glCheckFramebufferStatusEXT, GLenum, __VA_ARGS__)
#define glGenFramebuffersEXT(...) CHECKED_GL_FUNCTION(g_glGenFramebuffersEXT, __VA_ARGS__)
#define glGenRenderbuffersEXT(...) CHECKED_GL_FUNCTION(g_glGenRenderbuffersEXT, __VA_ARGS__)
#define glRenderbufferStorageEXT(...) CHECKED_GL_FUNCTION(g_glRenderbufferStorageEXT, __VA_ARGS__)
#define glCopyTexSubImage2D(...) CHECKED_GL_FUNCTION(g_glCopyTexSubImage2D, __VA_ARGS__)
#define glVertex2f(...) CHECKED_GL_FUNCTION(g_glVertex2f, __VA_ARGS__)
#define glCopyTexImage2D(...) CHECKED_GL_FUNCTION(g_glCopyTexImage2D, __VA_ARGS__)
#define glLoadMatrixf(...) CHECKED_GL_FUNCTION(g_glLoadMatrixf, __VA_ARGS__)
#define glClearDepth(...) CHECKED_GL_FUNCTION(g_glClearDepth, __VA_ARGS__)
#define glGetHandleARB(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glGetHandleARB, GLhandleARB, __VA_ARGS__)
#define glDrawPixels(...) CHECKED_GL_FUNCTION(g_glDrawPixels, __VA_ARGS__)

typedef void (APIENTRYP PFNGLTEXENVIPROC) ( GLenum target, GLenum pname, GLint param );
typedef void (APIENTRYP PFNGLACTIVATETEXTUREARBPROC) ( GLenum texture );
typedef void (APIENTRYP PFNGLALPHAFUNCPROC) ( GLenum func, GLclampf ref );
typedef void (APIENTRYP PFNGLFOGIPROC) ( GLenum pname, GLint param );
typedef void (APIENTRYP PFNGLFOGFPROC) ( GLenum pname, GLfloat param );
typedef void (APIENTRYP PFNGLFOGFVPROC) ( GLenum pname, const GLfloat *params );
typedef void (APIENTRYP PFNGLPUSHATTRIBPROC) ( GLbitfield mask );
typedef void (APIENTRYP PFNGLCOLOR4UBPROC) ( GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha );
typedef void (APIENTRYP PFNGLBEGINPROC) ( GLenum mode );
typedef void (APIENTRYP PFNGLVERTEX3FPROC) ( GLfloat x, GLfloat y, GLfloat z );
typedef void (APIENTRYP PFNGLENDPROC) ( void );
typedef void (APIENTRYP PFNGLPOPATTRIBPROC) ( void );
typedef void (APIENTRYP PFNGLMULTITEXCOORD2FARBPROC) ( GLenum target, GLfloat s, GLfloat t );
typedef void (APIENTRYP PFNGLTEXCOORD2FPROC) ( GLfloat s, GLfloat t );
typedef void (APIENTRYP PFNGLCOLOR4FPROC) ( GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha );
typedef void (APIENTRYP PFNGLSECONDARYCOLOR3FPROC) ( GLfloat red, GLfloat green, GLfloat blue );
typedef void (APIENTRYP PFNGLVERTEX4FPROC) ( GLfloat x, GLfloat y, GLfloat z, GLfloat w );
typedef void (APIENTRYP PFNGLMATRIXMODEPROC) ( GLenum mode );
typedef void (APIENTRYP PFNGLLOADIDENTITYPROC) ( void );
typedef void (APIENTRYP PFNGLTRANSLATEFPROC) ( GLfloat x, GLfloat y, GLfloat z );
typedef void (APIENTRYP PFNGLSCALEFPROC) ( GLfloat x, GLfloat y, GLfloat z );
typedef void (APIENTRYP PFNGLVERTEX2FPROC) ( GLfloat x, GLfloat y );
typedef void (APIENTRYP PFNGLLOADMATRIXFPROC) ( const GLfloat *m );
typedef void (APIENTRYP PFNGLDRAWPIXELSPROC) ( GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels );

extern PFNGLBLENDFUNCPROC g_glBlendFunc;
extern PFNGLPIXELSTOREIPROC g_glPixelStorei;
extern PFNGLCLEARCOLORPROC g_glClearColor;
extern PFNGLCULLFACEPROC g_glCullFace;
extern PFNGLDEPTHFUNCPROC g_glDepthFunc;
extern PFNGLDEPTHMASKPROC g_glDepthMask;
extern PFNGLDISABLEPROC g_glDisable;
extern PFNGLENABLEPROC g_glEnable;
extern PFNGLPOLYGONOFFSETPROC g_glPolygonOffset;
extern PFNGLSCISSORPROC g_glScissor;
extern PFNGLVIEWPORTPROC g_glViewport;
extern PFNGLBINDTEXTUREPROC g_glBindTexture;
extern PFNGLTEXIMAGE2DPROC g_glTexImage2D;
extern PFNGLTEXPARAMETERIPROC g_glTexParameteri;
extern PFNGLGETINTEGERVPROC g_glGetIntegerv;
extern PFNGLGETSTRINGPROC g_glGetString;
extern PFNGLREADPIXELSPROC g_glReadPixels;
extern PFNGLTEXSUBIMAGE2DPROC g_glTexSubImage2D;
extern PFNGLDRAWARRAYSPROC g_glDrawArrays;
extern PFNGLGETERRORPROC g_glGetError;
extern PFNGLDRAWELEMENTSPROC g_glDrawElements;
extern PFNGLLINEWIDTHPROC g_glLineWidth;
extern PFNGLCLEARPROC g_glClear;
extern PFNGLGETFLOATVPROC g_glGetFloatv;
extern PFNGLDELETETEXTURESPROC g_glDeleteTextures;
extern PFNGLGENTEXTURESPROC g_glGenTextures;
extern PFNGLTEXPARAMETERFPROC g_glTexParameterf;
extern PFNGLACTIVETEXTUREPROC g_glActiveTexture;
extern PFNGLBLENDCOLORPROC g_glBlendColor;
extern PFNGLREADBUFFERPROC g_glReadBuffer;
extern PFNGLFINISHPROC g_glFinish;

extern PFNGLTEXENVIPROC g_glTexEnvi;
extern PFNGLACTIVATETEXTUREARBPROC g_glActiveTextureARB;
extern PFNGLCREATESHADEROBJECTARBPROC g_glCreateShaderObjectARB;
extern PFNGLSHADERSOURCEARBPROC g_glShaderSourceARB;
extern PFNGLCOMPILESHADERARBPROC g_glCompileShaderARB;
extern PFNGLCREATEPROGRAMOBJECTARBPROC g_glCreateProgramObjectARB;
extern PFNGLATTACHOBJECTARBPROC g_glAttachObjectARB;
extern PFNGLLINKPROGRAMARBPROC g_glLinkProgramARB;
extern PFNGLUSEPROGRAMOBJECTARBPROC g_glUseProgramObjectARB;
extern PFNGLGETOBJECTPARAMETERIVARBPROC g_glGetObjectParameterivARB;
extern PFNGLGETINFOLOGARBPROC g_glGetInfoLogARB;
extern PFNGLGETUNIFORMLOCATIONARBPROC g_glGetUniformLocationARB;
extern PFNGLUNIFORM1IARBPROC g_glUniform1iARB;
extern PFNGLUNIFORM4FARBPROC g_glUniform4fARB;
extern PFNGLUNIFORM1FARBPROC g_glUniform1fARB;
extern PFNGLBLENDFUNCSEPARATEEXTPROC g_glBlendFuncSeparateEXT;
extern PFNGLALPHAFUNCPROC g_glAlphaFunc;
extern PFNGLFOGIPROC g_glFogi;
extern PFNGLFOGFPROC g_glFogf;
extern PFNGLFOGFVPROC g_glFogfv;
extern PFNGLCOMPRESSEDTEXIMAGE2DARBPROC g_glCompressedTexImage2DARB;
extern PFNGLPUSHATTRIBPROC g_glPushAttrib;
extern PFNGLDRAWBUFFERPROC g_glDrawBuffer;
extern PFNGLCOLOR4UBPROC g_glColor4ub;
extern PFNGLBEGINPROC g_glBegin;
extern PFNGLVERTEX3FPROC g_glVertex3f;
extern PFNGLENDPROC g_glEnd;
extern PFNGLPOPATTRIBPROC g_glPopAttrib;
extern PFNGLMULTITEXCOORD2FARBPROC g_glMultiTexCoord2fARB;
extern PFNGLTEXCOORD2FPROC g_glTexCoord2f;
extern PFNGLCOLOR4FPROC g_glColor4f;
extern PFNGLSECONDARYCOLOR3FPROC g_glSecondaryColor3f;
extern PFNGLVERTEX4FPROC g_glVertex4f;
extern PFNGLCOLORMASKPROC g_glColorMask;
extern PFNGLMATRIXMODEPROC g_glMatrixMode;
extern PFNGLLOADIDENTITYPROC g_glLoadIdentity;
extern PFNGLTRANSLATEFPROC g_glTranslatef;
extern PFNGLSCALEFPROC g_glScalef;
extern PFNGLGETTEXLEVELPARAMETERIVPROC g_glGetTexLevelParameteriv;
extern PFNGLBINDFRAMEBUFFEREXTPROC g_glBindFramebufferEXT;
extern PFNGLDELETEFRAMEBUFFERSEXTPROC g_glDeleteFramebuffersEXT;
extern PFNGLDELETERENDERBUFFERSEXTPROC g_glDeleteRenderbuffersEXT;
extern PFNGLFRAMEBUFFERTEXTURE2DEXTPROC g_glFramebufferTexture2DEXT;
extern PFNGLBINDRENDERBUFFEREXTPROC g_glBindRenderbufferEXT;
extern PFNGLFRAMEBUFFERRENDERBUFFEREXTPROC g_glFramebufferRenderbufferEXT;
extern PFNGLCHECKFRAMEBUFFERSTATUSEXTPROC g_glCheckFramebufferStatusEXT;
extern PFNGLGENFRAMEBUFFERSEXTPROC g_glGenFramebuffersEXT;
extern PFNGLGENRENDERBUFFERSEXTPROC g_glGenRenderbuffersEXT;
extern PFNGLRENDERBUFFERSTORAGEEXTPROC g_glRenderbufferStorageEXT;
extern PFNGLCOPYTEXSUBIMAGE2DPROC g_glCopyTexSubImage2D;
extern PFNGLVERTEX2FPROC g_glVertex2f;
extern PFNGLCOPYTEXIMAGE2DPROC g_glCopyTexImage2D;
extern PFNGLLOADMATRIXFPROC g_glLoadMatrixf;
extern PFNGLCLEARDEPTHPROC g_glClearDepth;
extern PFNGLGETHANDLEARBPROC g_glGetHandleARB;
extern PFNGLDRAWPIXELSPROC g_glDrawPixels;

#endif

#ifdef OS_WINDOWS
#define glActiveTexture g_glActiveTexture
#define glBlendColor g_glBlendColor

extern PFNGLACTIVETEXTUREPROC g_glActiveTexture;
extern PFNGLBLENDCOLORPROC g_glBlendColor;
#endif

#define glCreateShader(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glCreateShader, GLuint, __VA_ARGS__)
#define glCompileShader(...) CHECKED_GL_FUNCTION(g_glCompileShader, __VA_ARGS__)
#define glShaderSource(...) CHECKED_GL_FUNCTION(g_glShaderSource, __VA_ARGS__)
#define glCreateProgram(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glCreateProgram, GLuint, __VA_ARGS__)
#define glAttachShader(...) CHECKED_GL_FUNCTION(g_glAttachShader, __VA_ARGS__)
#define glLinkProgram(...) CHECKED_GL_FUNCTION(g_glLinkProgram, __VA_ARGS__)
#define glUseProgram(...) CHECKED_GL_FUNCTION(g_glUseProgram, __VA_ARGS__)
#define glGetUniformLocation(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glGetUniformLocation, GLint, __VA_ARGS__)
#define glUniform1i(...) CHECKED_GL_FUNCTION(g_glUniform1i, __VA_ARGS__)
#define glUniform1f(...) CHECKED_GL_FUNCTION(g_glUniform1f, __VA_ARGS__)
#define glUniform2f(...) CHECKED_GL_FUNCTION(g_glUniform2f, __VA_ARGS__)
#define glUniform2i(...) CHECKED_GL_FUNCTION(g_glUniform2i, __VA_ARGS__)
#define glUniform4i(...) CHECKED_GL_FUNCTION(g_glUniform4i, __VA_ARGS__)

#define glUniform4f(...) CHECKED_GL_FUNCTION(g_glUniform4f, __VA_ARGS__)
#define glUniform3fv(...) CHECKED_GL_FUNCTION(g_glUniform3fv, __VA_ARGS__)
#define glUniform4fv(...) CHECKED_GL_FUNCTION(g_glUniform4fv, __VA_ARGS__)
#define glDetachShader(...) CHECKED_GL_FUNCTION(g_glDetachShader, __VA_ARGS__)
#define glDeleteShader(...) CHECKED_GL_FUNCTION(g_glDeleteShader, __VA_ARGS__)
#define glDeleteProgram(...) CHECKED_GL_FUNCTION(g_glDeleteProgram, __VA_ARGS__)
#define glGetProgramInfoLog(...) CHECKED_GL_FUNCTION(g_glGetProgramInfoLog, __VA_ARGS__)
#define glGetShaderInfoLog(...) CHECKED_GL_FUNCTION(g_glGetShaderInfoLog, __VA_ARGS__)
#define glGetShaderiv(...) CHECKED_GL_FUNCTION(g_glGetShaderiv, __VA_ARGS__)
#define glGetProgramiv(...) CHECKED_GL_FUNCTION(g_glGetProgramiv, __VA_ARGS__)

#define glEnableVertexAttribArray(...) CHECKED_GL_FUNCTION(g_glEnableVertexAttribArray, __VA_ARGS__)
#define glDisableVertexAttribArray(...) CHECKED_GL_FUNCTION(g_glDisableVertexAttribArray, __VA_ARGS__)
#define glVertexAttribPointer(...) CHECKED_GL_FUNCTION(g_glVertexAttribPointer, __VA_ARGS__)
#define glBindAttribLocation(...) CHECKED_GL_FUNCTION(g_glBindAttribLocation, __VA_ARGS__)
#define glVertexAttrib1f(...) CHECKED_GL_FUNCTION(g_glVertexAttrib1f, __VA_ARGS__)
#define glVertexAttrib4f(...) CHECKED_GL_FUNCTION(g_glVertexAttrib4f, __VA_ARGS__)
#define glVertexAttrib4fv(...) CHECKED_GL_FUNCTION(g_glVertexAttrib4fv, __VA_ARGS__)

#define glDepthRangef(...) CHECKED_GL_FUNCTION(g_glDepthRangef, __VA_ARGS__)
#define glClearDepthf(...) CHECKED_GL_FUNCTION(g_glClearDepthf, __VA_ARGS__)

#define glBindBuffer(...) CHECKED_GL_FUNCTION(g_glBindBuffer, __VA_ARGS__)
#define glBindFramebuffer(...) CHECKED_GL_FUNCTION(g_glBindFramebuffer, __VA_ARGS__)
#define glBindRenderbuffer(...) CHECKED_GL_FUNCTION(g_glBindRenderbuffer, __VA_ARGS__)
#define glDrawBuffers(...) CHECKED_GL_FUNCTION(g_glDrawBuffers, __VA_ARGS__)
#define glGenFramebuffers(...) CHECKED_GL_FUNCTION(g_glGenFramebuffers, __VA_ARGS__)
#define glDeleteFramebuffers(...) CHECKED_GL_FUNCTION(g_glDeleteFramebuffers, __VA_ARGS__)
#define glFramebufferTexture2D(...) CHECKED_GL_FUNCTION(g_glFramebufferTexture2D, __VA_ARGS__)
#define glTexImage2DMultisample(...) CHECKED_GL_FUNCTION(g_glTexImage2DMultisample, __VA_ARGS__)
#define glTexStorage2DMultisample(...) CHECKED_GL_FUNCTION(g_glTexStorage2DMultisample, __VA_ARGS__)
#define glGenRenderbuffers(...) CHECKED_GL_FUNCTION(g_glGenRenderbuffers, __VA_ARGS__)
#define glRenderbufferStorage(...) CHECKED_GL_FUNCTION(g_glRenderbufferStorage, __VA_ARGS__)
#define glDeleteRenderbuffers(...) CHECKED_GL_FUNCTION(g_glDeleteRenderbuffers, __VA_ARGS__)
#define glFramebufferRenderbuffer(...) CHECKED_GL_FUNCTION(g_glFramebufferRenderbuffer, __VA_ARGS__)
#define glCheckFramebufferStatus(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glCheckFramebufferStatus, GLenum, __VA_ARGS__)
#define glBlitFramebuffer(...) CHECKED_GL_FUNCTION(g_glBlitFramebuffer, __VA_ARGS__)
#define glGenVertexArrays(...) CHECKED_GL_FUNCTION(g_glGenVertexArrays, __VA_ARGS__)
#define glBindVertexArray(...) CHECKED_GL_FUNCTION(g_glBindVertexArray, __VA_ARGS__)
#define glDeleteVertexArrays(...) CHECKED_GL_FUNCTION(g_glDeleteVertexArrays, __VA_ARGS__);
#define glGenBuffers(...) CHECKED_GL_FUNCTION(g_glGenBuffers, __VA_ARGS__)
#define glBufferData(...) CHECKED_GL_FUNCTION(g_glBufferData, __VA_ARGS__)
#define glMapBuffer(...) CHECKED_GL_FUNCTION(g_glMapBuffer, __VA_ARGS__)
#define glMapBufferRange(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glMapBufferRange, void*, __VA_ARGS__)
#define glUnmapBuffer(...) CHECKED_GL_FUNCTION(g_glUnmapBuffer, __VA_ARGS__)
#define glDeleteBuffers(...) CHECKED_GL_FUNCTION(g_glDeleteBuffers, __VA_ARGS__)
#define glBindImageTexture(...) CHECKED_GL_FUNCTION(g_glBindImageTexture, __VA_ARGS__)
#define glMemoryBarrier(...) CHECKED_GL_FUNCTION(g_glMemoryBarrier, __VA_ARGS__)
#define glGetStringi(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glGetStringi, const GLubyte*, __VA_ARGS__)
#define glInvalidateFramebuffer(...) CHECKED_GL_FUNCTION(g_glInvalidateFramebuffer, __VA_ARGS__)
#define glBufferStorage(...) CHECKED_GL_FUNCTION(g_glBufferStorage, __VA_ARGS__)
#define glFenceSync(...) CHECKED_GL_FUNCTION_WITH_RETURN(g_glFenceSync, GLsync, __VA_ARGS__)
#define glClientWaitSync(...) CHECKED_GL_FUNCTION(g_glClientWaitSync, __VA_ARGS__)
#define glDeleteSync(...) CHECKED_GL_FUNCTION(g_glDeleteSync, __VA_ARGS__)

#define glGetUniformBlockIndex(...) CHECKED_GL_FUNCTION(g_glGetUniformBlockIndex, __VA_ARGS__)
#define glUniformBlockBinding(...) CHECKED_GL_FUNCTION(g_glUniformBlockBinding, __VA_ARGS__)
#define glGetActiveUniformBlockiv(...) CHECKED_GL_FUNCTION(g_glGetActiveUniformBlockiv, __VA_ARGS__)
#define glGetUniformIndices(...) CHECKED_GL_FUNCTION(g_glGetUniformIndices, __VA_ARGS__)
#define glGetActiveUniformsiv(...) CHECKED_GL_FUNCTION(g_glGetActiveUniformsiv, __VA_ARGS__)
#define glBindBufferBase(...) CHECKED_GL_FUNCTION(g_glBindBufferBase, __VA_ARGS__)
#define glBufferSubData(...) CHECKED_GL_FUNCTION(g_glBufferSubData, __VA_ARGS__)

#define glGetProgramBinary(...) CHECKED_GL_FUNCTION(g_glGetProgramBinary, __VA_ARGS__)
#define glProgramBinary(...) CHECKED_GL_FUNCTION(g_glProgramBinary, __VA_ARGS__)
#define glProgramParameteri(...) CHECKED_GL_FUNCTION(g_glProgramParameteri, __VA_ARGS__)

#define glTexStorage2D(...) CHECKED_GL_FUNCTION(g_glTexStorage2D, __VA_ARGS__)
#define glTextureStorage2D(...) CHECKED_GL_FUNCTION(g_glTextureStorage2D, __VA_ARGS__)
#define glTextureSubImage2D(...) CHECKED_GL_FUNCTION(g_glTextureSubImage2D, __VA_ARGS__)
#define glTextureStorage2DMultisample(...) CHECKED_GL_FUNCTION(g_glTextureStorage2DMultisample, __VA_ARGS__)
#define glTextureParameteri(...) CHECKED_GL_FUNCTION(g_glTextureParameteri, __VA_ARGS__)
#define glTextureParameterf(...) CHECKED_GL_FUNCTION(g_glTextureParameterf, __VA_ARGS__)
#define glCreateTextures(...) CHECKED_GL_FUNCTION(g_glCreateTextures, __VA_ARGS__)
#define glCreateBuffers(...) CHECKED_GL_FUNCTION(g_glCreateBuffers, __VA_ARGS__)
#define glCreateFramebuffers(...) CHECKED_GL_FUNCTION(g_glCreateFramebuffers, __VA_ARGS__)
#define glNamedFramebufferTexture(...) CHECKED_GL_FUNCTION(g_glNamedFramebufferTexture, __VA_ARGS__)
#define glDrawElementsBaseVertex(...) CHECKED_GL_FUNCTION(g_glDrawElementsBaseVertex, __VA_ARGS__)
#define glFlushMappedBufferRange(...) CHECKED_GL_FUNCTION(g_glFlushMappedBufferRange, __VA_ARGS__)

extern PFNGLCREATESHADERPROC g_glCreateShader;
extern PFNGLCOMPILESHADERPROC g_glCompileShader;
extern PFNGLSHADERSOURCEPROC g_glShaderSource;
extern PFNGLCREATEPROGRAMPROC g_glCreateProgram;
extern PFNGLATTACHSHADERPROC g_glAttachShader;
extern PFNGLLINKPROGRAMPROC g_glLinkProgram;
extern PFNGLUSEPROGRAMPROC g_glUseProgram;
extern PFNGLGETUNIFORMLOCATIONPROC g_glGetUniformLocation;
extern PFNGLUNIFORM1IPROC g_glUniform1i;
extern PFNGLUNIFORM1FPROC g_glUniform1f;
extern PFNGLUNIFORM2FPROC g_glUniform2f;
extern PFNGLUNIFORM2IPROC g_glUniform2i;
extern PFNGLUNIFORM4IPROC g_glUniform4i;

extern PFNGLUNIFORM4FPROC g_glUniform4f;
extern PFNGLUNIFORM3FVPROC g_glUniform3fv;
extern PFNGLUNIFORM4FVPROC g_glUniform4fv;
extern PFNGLDETACHSHADERPROC g_glDetachShader;
extern PFNGLDELETESHADERPROC g_glDeleteShader;
extern PFNGLDELETEPROGRAMPROC g_glDeleteProgram;
extern PFNGLGETPROGRAMINFOLOGPROC g_glGetProgramInfoLog;
extern PFNGLGETSHADERINFOLOGPROC g_glGetShaderInfoLog;
extern PFNGLGETSHADERIVPROC g_glGetShaderiv;
extern PFNGLGETPROGRAMIVPROC g_glGetProgramiv;

extern PFNGLENABLEVERTEXATTRIBARRAYPROC g_glEnableVertexAttribArray;
extern PFNGLDISABLEVERTEXATTRIBARRAYPROC g_glDisableVertexAttribArray;
extern PFNGLVERTEXATTRIBPOINTERPROC g_glVertexAttribPointer;
extern PFNGLBINDATTRIBLOCATIONPROC g_glBindAttribLocation;
extern PFNGLVERTEXATTRIB1FPROC g_glVertexAttrib1f;
extern PFNGLVERTEXATTRIB4FPROC g_glVertexAttrib4f;
extern PFNGLVERTEXATTRIB4FVPROC g_glVertexAttrib4fv;

extern PFNGLDEPTHRANGEFPROC g_glDepthRangef;
extern PFNGLCLEARDEPTHFPROC g_glClearDepthf;

extern PFNGLDRAWBUFFERSPROC g_glDrawBuffers;
extern PFNGLGENFRAMEBUFFERSPROC g_glGenFramebuffers;
extern PFNGLBINDFRAMEBUFFERPROC g_glBindFramebuffer;
extern PFNGLDELETEFRAMEBUFFERSPROC g_glDeleteFramebuffers;
extern PFNGLFRAMEBUFFERTEXTURE2DPROC g_glFramebufferTexture2D;
extern PFNGLTEXIMAGE2DMULTISAMPLEPROC g_glTexImage2DMultisample;
extern PFNGLTEXSTORAGE2DMULTISAMPLEPROC g_glTexStorage2DMultisample;
extern PFNGLGENRENDERBUFFERSPROC g_glGenRenderbuffers;
extern PFNGLBINDRENDERBUFFERPROC g_glBindRenderbuffer;
extern PFNGLRENDERBUFFERSTORAGEPROC g_glRenderbufferStorage;
extern PFNGLDELETERENDERBUFFERSPROC g_glDeleteRenderbuffers;
extern PFNGLFRAMEBUFFERRENDERBUFFERPROC g_glFramebufferRenderbuffer;
extern PFNGLCHECKFRAMEBUFFERSTATUSPROC g_glCheckFramebufferStatus;
extern PFNGLBLITFRAMEBUFFERPROC g_glBlitFramebuffer;
extern PFNGLGENVERTEXARRAYSPROC g_glGenVertexArrays;
extern PFNGLBINDVERTEXARRAYPROC g_glBindVertexArray;
extern PFNGLDELETEVERTEXARRAYSPROC g_glDeleteVertexArrays;
extern PFNGLGENBUFFERSPROC g_glGenBuffers;
extern PFNGLBINDBUFFERPROC g_glBindBuffer;
extern PFNGLBUFFERDATAPROC g_glBufferData;
extern PFNGLMAPBUFFERPROC g_glMapBuffer;
extern PFNGLMAPBUFFERRANGEPROC g_glMapBufferRange;
extern PFNGLUNMAPBUFFERPROC g_glUnmapBuffer;
extern PFNGLDELETEBUFFERSPROC g_glDeleteBuffers;
extern PFNGLBINDIMAGETEXTUREPROC g_glBindImageTexture;
extern PFNGLMEMORYBARRIERPROC g_glMemoryBarrier;
extern PFNGLGETSTRINGIPROC g_glGetStringi;
extern PFNGLINVALIDATEFRAMEBUFFERPROC g_glInvalidateFramebuffer;
extern PFNGLBUFFERSTORAGEPROC g_glBufferStorage;
extern PFNGLFENCESYNCPROC g_glFenceSync;
extern PFNGLCLIENTWAITSYNCPROC g_glClientWaitSync;
extern PFNGLDELETESYNCPROC g_glDeleteSync;

extern PFNGLGETUNIFORMBLOCKINDEXPROC g_glGetUniformBlockIndex;
extern PFNGLUNIFORMBLOCKBINDINGPROC g_glUniformBlockBinding;
extern PFNGLGETACTIVEUNIFORMBLOCKIVPROC g_glGetActiveUniformBlockiv;
extern PFNGLGETUNIFORMINDICESPROC g_glGetUniformIndices;
extern PFNGLGETACTIVEUNIFORMSIVPROC g_glGetActiveUniformsiv;
extern PFNGLBINDBUFFERBASEPROC g_glBindBufferBase;
extern PFNGLBUFFERSUBDATAPROC g_glBufferSubData;

extern PFNGLGETPROGRAMBINARYPROC g_glGetProgramBinary;
extern PFNGLPROGRAMBINARYPROC g_glProgramBinary;
extern PFNGLPROGRAMPARAMETERIPROC g_glProgramParameteri;

extern PFNGLTEXSTORAGE2DPROC g_glTexStorage2D;
extern PFNGLTEXTURESTORAGE2DPROC g_glTextureStorage2D;
extern PFNGLTEXTURESUBIMAGE2DPROC g_glTextureSubImage2D;
extern PFNGLTEXTURESTORAGE2DMULTISAMPLEEXTPROC g_glTextureStorage2DMultisample;
extern PFNGLTEXTUREPARAMETERIPROC g_glTextureParameteri;
extern PFNGLTEXTUREPARAMETERFPROC g_glTextureParameterf;
extern PFNGLCREATETEXTURESPROC g_glCreateTextures;
extern PFNGLCREATEBUFFERSPROC g_glCreateBuffers;
extern PFNGLCREATEFRAMEBUFFERSPROC g_glCreateFramebuffers;
extern PFNGLNAMEDFRAMEBUFFERTEXTUREPROC g_glNamedFramebufferTexture;
extern PFNGLDRAWELEMENTSBASEVERTEXPROC g_glDrawElementsBaseVertex;
extern PFNGLFLUSHMAPPEDBUFFERRANGEPROC g_glFlushMappedBufferRange;

namespace  EGLLoader {

	void loadEGLFunctions();
}

template<typename F> void checked(F fn, const char* _functionName)
{
	fn();
	int error = glGetError();
	if (error != GL_NO_ERROR) {
		__android_log_print(ANDROID_LOG_ERROR, "EGLLoader", "GL error, function = %s, error = 0x%02x", _functionName, error);
		abort();
	}
}

template<typename R, typename F> R checkedWithReturn(F fn, const char* _functionName)
{
	R returnValue = fn();
	int error = glGetError();
	if (error != GL_NO_ERROR) {
		__android_log_print(ANDROID_LOG_ERROR, "EGLLoader", "GL error, function = %s, error = 0x%02x", _functionName, error);
		abort();
	}

	return returnValue;
}
