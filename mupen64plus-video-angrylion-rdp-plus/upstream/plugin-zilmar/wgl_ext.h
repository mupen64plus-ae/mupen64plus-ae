#ifndef WINDOWSGL_NOLOAD_STYLE_H
#define WINDOWSGL_NOLOAD_STYLE_H

#ifdef __wglext_h_
#error Attempt to include auto-generated WGL header after wglext.h
#endif

#define __wglext_h_

#ifndef WIN32_LEAN_AND_MEAN
	#define WIN32_LEAN_AND_MEAN 1
#endif
#ifndef NOMINMAX
	#define NOMINMAX
#endif
#include <windows.h>

#ifdef CODEGEN_FUNCPTR
#undef CODEGEN_FUNCPTR
#endif /*CODEGEN_FUNCPTR*/
#define CODEGEN_FUNCPTR WINAPI

#ifndef GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS
#define GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS

typedef unsigned int GLenum;
typedef unsigned char GLboolean;
typedef unsigned int GLbitfield;
typedef signed char GLbyte;
typedef short GLshort;
typedef int GLint;
typedef int GLsizei;
typedef unsigned char GLubyte;
typedef unsigned short GLushort;
typedef unsigned int GLuint;
typedef float GLfloat;
typedef float GLclampf;
typedef double GLdouble;
typedef double GLclampd;
#define GLvoid void

#endif /*GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS*/


#ifndef GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS
#define GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS


#endif /*GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS*/

struct _GPU_DEVICE {
    DWORD  cb;
    CHAR   DeviceName[32];
    CHAR   DeviceString[128];
    DWORD  Flags;
    RECT   rcVirtualScreen;
};
DECLARE_HANDLE(HPBUFFERARB);
DECLARE_HANDLE(HPBUFFEREXT);
DECLARE_HANDLE(HVIDEOOUTPUTDEVICENV);
DECLARE_HANDLE(HPVIDEODEV);
DECLARE_HANDLE(HGPUNV);
DECLARE_HANDLE(HVIDEOINPUTDEVICENV);
typedef struct _GPU_DEVICE *PGPU_DEVICE;

#ifdef __cplusplus
extern "C" {
#endif /*__cplusplus*/

/***********************/
/* Extension Variables*/

extern int wgl_ext_EXT_swap_control;
extern int wgl_ext_ARB_create_context;
extern int wgl_ext_ARB_create_context_profile;

/* Extension: ARB_create_context*/
#define WGL_CONTEXT_DEBUG_BIT_ARB        0x00000001
#define WGL_CONTEXT_FLAGS_ARB            0x2094
#define WGL_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB 0x00000002
#define WGL_CONTEXT_LAYER_PLANE_ARB      0x2093
#define WGL_CONTEXT_MAJOR_VERSION_ARB    0x2091
#define WGL_CONTEXT_MINOR_VERSION_ARB    0x2092
#define WGL_ERROR_INVALID_VERSION_ARB    0x2095

/* Extension: ARB_create_context_profile*/
#define WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB 0x00000002
#define WGL_CONTEXT_CORE_PROFILE_BIT_ARB 0x00000001
#define WGL_CONTEXT_PROFILE_MASK_ARB     0x9126
#define WGL_ERROR_INVALID_PROFILE_ARB    0x2096


/* Extension: EXT_swap_control*/
extern int (CODEGEN_FUNCPTR *_ptrc_wglGetSwapIntervalEXT)(void);
#define wglGetSwapIntervalEXT _ptrc_wglGetSwapIntervalEXT
extern BOOL (CODEGEN_FUNCPTR *_ptrc_wglSwapIntervalEXT)(int interval);
#define wglSwapIntervalEXT _ptrc_wglSwapIntervalEXT

/* Extension: ARB_create_context*/
extern HGLRC (CODEGEN_FUNCPTR *_ptrc_wglCreateContextAttribsARB)(HDC hDC, HGLRC hShareContext, const int * attribList);
#define wglCreateContextAttribsARB _ptrc_wglCreateContextAttribsARB

void wgl_CheckExtensions(HDC hdc);
#ifdef __cplusplus
}
#endif /*__cplusplus*/

#endif /*WINDOWSGL_NOLOAD_STYLE_H*/
