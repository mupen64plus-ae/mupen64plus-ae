#include "ae_vidext.h"
#include "ae_imports.h"
#include "GL/glcorearb.h"
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <string.h>

EGLDisplay display;
EGLConfig config;
EGLContext context;
EGLSurface surface;
ANativeWindow* native_window;
int isGLES2;
int new_surface;

PFNGLGETINTEGERVPROC g_glGetIntegerv = NULL;
PFNGLGETSTRINGPROC g_glGetString = NULL;

EGLint const defaultAttributeList[] = {
        EGL_BUFFER_SIZE, 0,
        EGL_BLUE_SIZE, 0,
        EGL_GREEN_SIZE, 0,
        EGL_RED_SIZE, 0,
        EGL_ALPHA_SIZE, 0,
        EGL_DEPTH_SIZE, 16,
        EGL_SAMPLE_BUFFERS, 0,
        EGL_SAMPLES, 0,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_NONE
};

EGLint const defaultContextAttribs[] = {
        EGL_CONTEXT_MAJOR_VERSION_KHR, 2,
        EGL_CONTEXT_MINOR_VERSION_KHR, 0,
        EGL_NONE
};

EGLint const defaultWindowAttribs[] = {
        EGL_RENDER_BUFFER, EGL_BACK_BUFFER,
        EGL_NONE
};

EGLint* attribList;
EGLint* windowAttribList;
EGLint* contextAttribs;

size_t FindIndex( const int a[], size_t size, int value )
{
    size_t index = 0;

    while ( index < size && a[index] != value ) ++index;

    return ( index == size ? -1 : index );
}

extern DECLSPEC m64p_error VidExtFuncInit()
{
    new_surface = 0;
    surface = EGL_NO_SURFACE;
    context = EGL_NO_CONTEXT;
    display = EGL_NO_DISPLAY;
    attribList = malloc(sizeof(defaultAttributeList));
    memcpy(attribList, defaultAttributeList, sizeof(defaultAttributeList));
    windowAttribList = malloc(sizeof(defaultWindowAttribs));
    memcpy(windowAttribList, defaultWindowAttribs, sizeof(defaultWindowAttribs));
    contextAttribs = malloc(sizeof(defaultContextAttribs));
    memcpy(contextAttribs, defaultContextAttribs, sizeof(defaultContextAttribs));

    if ((display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay() returned error %d", eglGetError());
        return M64ERR_INVALID_STATE;
    }
    if (!eglInitialize(display, 0, 0)) {
        LOGE("eglInitialize() returned error %d", eglGetError());
        return M64ERR_INVALID_STATE;
    }
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncQuit()
{
    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    if (surface != EGL_NO_SURFACE) {
        eglDestroySurface(display, surface);
        surface = EGL_NO_SURFACE;
    }
    if (context != EGL_NO_CONTEXT) {
        eglDestroyContext(display, context);
        context = EGL_NO_CONTEXT;
    }
    if (display != EGL_NO_DISPLAY) {
        eglTerminate(display);
        display = EGL_NO_DISPLAY;
    }
    free(attribList);
    free(windowAttribList);
    free(contextAttribs);
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncListModes(m64p_2d_size *SizeArray, int *NumSizes)
{
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncSetMode(int Width, int Height, int BitsPerPixel, int ScreenMode, int Flags)
{
    EGLint num_config;
    if (!eglChooseConfig(display, attribList, &config, 1, &num_config)) {
        LOGE("eglChooseConfig() returned error %d", eglGetError());
        return M64ERR_INVALID_STATE;
    }
    if (!(surface = eglCreateWindowSurface(display, config, (EGLNativeWindowType)native_window, windowAttribList))) {
        LOGE("eglCreateWindowSurface() returned error %d", eglGetError());
        return M64ERR_INVALID_STATE;
    }

    if (!(context = eglCreateContext(display, config, EGL_NO_CONTEXT, contextAttribs))) {
        //If creating the context failed, just try to create a GLES2/3 context
        //This is useful because GLideN64 requests an OpenGL 3.3 core context.
        if (!(context = eglCreateContext(display, config, EGL_NO_CONTEXT, defaultContextAttribs))) {
            LOGE("eglCreateContext() returned error %d", eglGetError());
            return M64ERR_INVALID_STATE;
        }
    }
    if (!eglMakeCurrent(display, surface, surface, context)) {
        LOGE("eglMakeCurrent() returned error %d", eglGetError());
        return M64ERR_INVALID_STATE;
    }
    g_glGetIntegerv = (PFNGLGETINTEGERVPROC) eglGetProcAddress("glGetIntegerv");
    g_glGetString = (PFNGLGETSTRINGPROC) eglGetProcAddress("glGetString");
    const char * strVersion = (const char*)g_glGetString(GL_VERSION);
    isGLES2 = strstr(strVersion, "OpenGL ES 2") != NULL;
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncSetCaption(const char *Title)
{
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncToggleFS()
{
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncResizeWindow(int Width, int Height)
{
    return M64ERR_SUCCESS;
}

extern DECLSPEC void * VidExtFuncGLGetProc(const char* Proc)
{
    return eglGetProcAddress(Proc);
}

extern DECLSPEC m64p_error VidExtFuncGLSetAttr(m64p_GLattr Attr, int Value)
{
    int my_index;
    switch (Attr) {
        case M64P_GL_DOUBLEBUFFER:
            my_index = FindIndex(windowAttribList, sizeof(windowAttribList), EGL_RENDER_BUFFER);
            if (my_index != -1) {
                if (Value == 0)
                    windowAttribList[my_index + 1] = EGL_SINGLE_BUFFER;
                else
                    windowAttribList[my_index + 1] = EGL_BACK_BUFFER;
            }
            break;
        case M64P_GL_BUFFER_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_BUFFER_SIZE);
            if (my_index != -1)
                attribList[my_index + 1] = Value;
            break;
        case M64P_GL_DEPTH_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_DEPTH_SIZE);
            if (my_index != -1)
                attribList[my_index + 1] = Value;
            break;
        case M64P_GL_RED_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_RED_SIZE);
            if (my_index != -1)
                attribList[my_index + 1] = Value;
            break;
        case M64P_GL_GREEN_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_GREEN_SIZE);
            if (my_index != -1)
                attribList[my_index + 1] = Value;
            break;
        case M64P_GL_BLUE_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_BLUE_SIZE);
            if (my_index != -1)
                attribList[my_index + 1] = Value;
            break;
        case M64P_GL_ALPHA_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_ALPHA_SIZE);
            if (my_index != -1)
                attribList[my_index + 1] = Value;
            break;
        case M64P_GL_SWAP_CONTROL:
            eglSwapInterval(display, Value);
            break;
        case M64P_GL_MULTISAMPLEBUFFERS:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_SAMPLE_BUFFERS);
            if (my_index != -1)
                attribList[my_index + 1] = Value;
            break;
        case M64P_GL_MULTISAMPLESAMPLES:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_SAMPLES);
            if (my_index != -1)
                attribList[my_index + 1] = Value;
            break;
        case M64P_GL_CONTEXT_MAJOR_VERSION:
            my_index = FindIndex(contextAttribs, sizeof(contextAttribs), EGL_CONTEXT_MAJOR_VERSION_KHR);
            if (my_index != -1)
                contextAttribs[my_index + 1]= Value;
            break;
        case M64P_GL_CONTEXT_MINOR_VERSION:
            my_index = FindIndex(contextAttribs, sizeof(contextAttribs), EGL_CONTEXT_MINOR_VERSION_KHR);
            if (my_index != -1)
                contextAttribs[my_index + 1]= Value;
            break;
        case M64P_GL_CONTEXT_PROFILE_MASK:
            switch (Value) {
                case M64P_GL_CONTEXT_PROFILE_ES:
                    eglBindAPI(EGL_OPENGL_ES_API);
                    my_index = FindIndex(attribList, sizeof(attribList), EGL_RENDERABLE_TYPE);
                    if (my_index != -1)
                        attribList[my_index + 1] = EGL_OPENGL_ES2_BIT;
                    break;
                case M64P_GL_CONTEXT_PROFILE_CORE:
                case M64P_GL_CONTEXT_PROFILE_COMPATIBILITY:
                    eglBindAPI(EGL_OPENGL_API);
                    my_index = FindIndex(attribList, sizeof(attribList), EGL_RENDERABLE_TYPE);
                    if (my_index != -1)
                        attribList[my_index + 1] = EGL_OPENGL_BIT;
                    break;
            }
            break;
    }
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncGLGetAttr(m64p_GLattr Attr, int *pValue)
{
    int value;
    switch (Attr) {
        case M64P_GL_DOUBLEBUFFER:
            eglQueryContext(display, context, EGL_RENDER_BUFFER, &value);
            if (value == EGL_SINGLE_BUFFER)
                *pValue = 0;
            else
                *pValue = 1;
            break;
        case M64P_GL_BUFFER_SIZE:
            eglGetConfigAttrib(display, config, EGL_BUFFER_SIZE, pValue);
            break;
        case M64P_GL_DEPTH_SIZE:
            eglGetConfigAttrib(display, config, EGL_DEPTH_SIZE, pValue);
            break;
        case M64P_GL_RED_SIZE:
            eglGetConfigAttrib(display, config, EGL_RED_SIZE, pValue);
            break;
        case M64P_GL_GREEN_SIZE:
            eglGetConfigAttrib(display, config, EGL_GREEN_SIZE, pValue);
            break;
        case M64P_GL_BLUE_SIZE:
            eglGetConfigAttrib(display, config, EGL_BLUE_SIZE, pValue);
            break;
        case M64P_GL_ALPHA_SIZE:
            eglGetConfigAttrib(display, config, EGL_ALPHA_SIZE, pValue);
            break;
        case M64P_GL_SWAP_CONTROL:
            break;
        case M64P_GL_MULTISAMPLEBUFFERS:
            eglGetConfigAttrib(display, config, EGL_SAMPLE_BUFFERS, pValue);
            break;
        case M64P_GL_MULTISAMPLESAMPLES:
            eglGetConfigAttrib(display, config, EGL_SAMPLES, pValue);
            break;
        case M64P_GL_CONTEXT_MAJOR_VERSION:
            if (!isGLES2)
                g_glGetIntegerv(GL_MAJOR_VERSION, pValue);
            else
                *pValue = 2;
            break;
        case M64P_GL_CONTEXT_MINOR_VERSION:
            if (!isGLES2)
                g_glGetIntegerv(GL_MINOR_VERSION, pValue);
            else
                *pValue = 0;
            break;
        case M64P_GL_CONTEXT_PROFILE_MASK:
            eglQueryContext(display, context, EGL_CONTEXT_CLIENT_TYPE, &value);
            if (value != EGL_OPENGL_ES_API) {
                g_glGetIntegerv(GL_CONTEXT_PROFILE_MASK, &value);
                if (value == GL_CONTEXT_CORE_PROFILE_BIT)
                    *pValue = M64P_GL_CONTEXT_PROFILE_CORE;
                else
                    *pValue = M64P_GL_CONTEXT_PROFILE_COMPATIBILITY;
            } else
                *pValue = M64P_GL_CONTEXT_PROFILE_ES;
            break;
    }
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncGLSwapBuf()
{
    if (new_surface) {
        if (!(surface = eglCreateWindowSurface(display, config, (EGLNativeWindowType)native_window, windowAttribList))) {
            LOGE("eglCreateWindowSurface() returned error %d", eglGetError());
            return M64ERR_INVALID_STATE;
        }
        if (!eglMakeCurrent(display, surface, surface, context)) {
            LOGE("eglMakeCurrent() returned error %d", eglGetError());
            return M64ERR_INVALID_STATE;
        }
        new_surface = 0;
    }
    if (surface != EGL_NO_SURFACE)
        eglSwapBuffers(display, surface);
    return M64ERR_SUCCESS;
}

DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_setNativeWindow(JNIEnv* env, jclass cls, jobject native_surface)
{
    native_window = ANativeWindow_fromSurface(env, native_surface);
    new_surface = 1;
}

DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuDestroySurface(JNIEnv* env, jclass cls)
{
    eglDestroySurface(display, surface);
    surface = EGL_NO_SURFACE;
}
