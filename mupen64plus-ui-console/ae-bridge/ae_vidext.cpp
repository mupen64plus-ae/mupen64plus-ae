#include <GL/EGLLoader.h>
#include "ae_vidext.h"
#include "ae_imports.h"
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <mutex>
#include <thread>
#include <unistd.h>

EGLDisplay display = EGL_NO_DISPLAY;
EGLConfig config;
EGLContext context = EGL_NO_CONTEXT;
EGLSurface surface = EGL_NO_SURFACE;
ANativeWindow* native_window = nullptr;
std::mutex nativeWindowAccess;
int isGLES2 = 1;
bool new_surface = false;
int FPSRecalcPeriod = 0;
uint32_t frameCount = 0;
int64_t oldTime;
int vsync = 0;
int oldVsync = 1;
bool isPaused = false;

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

EGLint const defaultGlEsContextAttribs[] = {
		EGL_CONTEXT_CLIENT_VERSION, 2,
		EGL_NONE
};

EGLint const defaultWindowAttribs[] = {
        EGL_RENDER_BUFFER, EGL_BACK_BUFFER,
        EGL_NONE
};

EGLint attribList[sizeof(defaultAttributeList) / sizeof(EGLint)];
EGLint windowAttribList[sizeof(defaultWindowAttribs) / sizeof(EGLint)];
EGLint contextAttribs[sizeof(defaultContextAttribs) / sizeof(EGLint)];

size_t FindIndex( const EGLint a[], size_t size, int value )
{
    size_t index = 0;

    while ( index < (size/sizeof(EGLint)) && a[index] != value ) ++index;

    return ( index == (size/sizeof(EGLint)) ? -1 : index );
}


JavaVM* mJavaVM;

// Library init
extern jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	mJavaVM = vm;
	return JNI_VERSION_1_6;
}

extern DECLSPEC m64p_error VidExtFuncInit()
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

    frameCount = 0;
    surface = EGL_NO_SURFACE;
    context = EGL_NO_CONTEXT;
    display = EGL_NO_DISPLAY;
    memcpy(attribList, defaultAttributeList, sizeof(defaultAttributeList));
    memcpy(windowAttribList, defaultWindowAttribs, sizeof(defaultWindowAttribs));
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

extern DECLSPEC m64p_error VidExtFuncListModes(m64p_2d_size *SizeArray, int *NumSizes)
{
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncSetMode(int Width, int Height, int BitsPerPixel, int ScreenMode, int Flags)
{
	{
		std::unique_lock<std::mutex> guard(nativeWindowAccess);

		EGLint num_config;
		if (!eglChooseConfig(display, attribList, &config, 1, &num_config)) {
			LOGE("eglChooseConfig() returned error %d", eglGetError());
			return M64ERR_INVALID_STATE;
		}
		if (num_config == 0) {
			//Try to fallback to GLES context
			eglBindAPI(EGL_OPENGL_ES_API);
			attribList[FindIndex(attribList, sizeof(attribList), EGL_RENDERABLE_TYPE) + 1] = EGL_OPENGL_ES2_BIT;
			if (!eglChooseConfig(display, attribList, &config, 1, &num_config)) {
				LOGE("eglChooseConfig() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}
		}

		if (!(context = eglCreateContext(display, config, EGL_NO_CONTEXT, contextAttribs))) {
			//If creating the context failed, just try to create a GLES2/3 context
			//This is useful because GLideN64 requests an OpenGL 3.3 core context.
			if (!(context = eglCreateContext(display, config, EGL_NO_CONTEXT, defaultGlEsContextAttribs))) {
				LOGE("eglCreateContext() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}
		}
	}

	// Wait for the native window to be set before continuing
	while (native_window == nullptr) {
		usleep(1000);
	}

	{
		std::unique_lock<std::mutex> guard(nativeWindowAccess);
		if(new_surface && native_window != nullptr)
		{
			LOGI("VidExtFuncSetMode: Initializing surface");

			if (!(surface = eglCreateWindowSurface(display, config, (EGLNativeWindowType)native_window, windowAttribList)))
			{
				LOGE("eglCreateWindowSurface() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			if (!eglMakeCurrent(display, surface, surface, context))
			{
				LOGE("eglMakeCurrent() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			new_surface = false;
		} else {
			LOGE("VidExtFuncSetMode called before surface has been set");
			return M64ERR_INVALID_STATE;
		}

		EGLLoader::loadEGLFunctions();

		const char * strVersion = reinterpret_cast<const char*>(g_glGetString(GL_VERSION));
		isGLES2 = strstr(strVersion, "OpenGL ES 2") != nullptr;
	}

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

extern DECLSPEC m64p_function VidExtFuncGLGetProc(const char* Proc)
{
    return reinterpret_cast<m64p_function>(eglGetProcAddress(Proc));
}

extern DECLSPEC m64p_error VidExtFuncGLSetAttr(m64p_GLattr Attr, int Value)
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

    int my_index;
    switch (Attr) {
        case M64P_GL_DOUBLEBUFFER:
            my_index = FindIndex(windowAttribList, sizeof(windowAttribList), EGL_RENDER_BUFFER);
            if (Value == 0)
                windowAttribList[my_index + 1] = EGL_SINGLE_BUFFER;
            else
                windowAttribList[my_index + 1] = EGL_BACK_BUFFER;
            break;
        case M64P_GL_BUFFER_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_BUFFER_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_DEPTH_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_DEPTH_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_RED_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_RED_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_GREEN_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_GREEN_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_BLUE_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_BLUE_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_ALPHA_SIZE:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_ALPHA_SIZE);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_SWAP_CONTROL:
            break;
        case M64P_GL_MULTISAMPLEBUFFERS:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_SAMPLE_BUFFERS);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_MULTISAMPLESAMPLES:
            my_index = FindIndex(attribList, sizeof(attribList), EGL_SAMPLES);
            attribList[my_index + 1] = Value;
            break;
        case M64P_GL_CONTEXT_MAJOR_VERSION:
            my_index = FindIndex(contextAttribs, sizeof(contextAttribs), EGL_CONTEXT_MAJOR_VERSION_KHR);
            contextAttribs[my_index + 1]= Value;
            break;
        case M64P_GL_CONTEXT_MINOR_VERSION:
            my_index = FindIndex(contextAttribs, sizeof(contextAttribs), EGL_CONTEXT_MINOR_VERSION_KHR);
            contextAttribs[my_index + 1]= Value;
            break;
        case M64P_GL_CONTEXT_PROFILE_MASK:
            switch (Value) {
                case M64P_GL_CONTEXT_PROFILE_ES:
                    eglBindAPI(EGL_OPENGL_ES_API);
                    my_index = FindIndex(attribList, sizeof(attribList), EGL_RENDERABLE_TYPE);
                    attribList[my_index + 1] = EGL_OPENGL_ES2_BIT;
                    break;
                case M64P_GL_CONTEXT_PROFILE_CORE:
                case M64P_GL_CONTEXT_PROFILE_COMPATIBILITY:
                    if (eglBindAPI(EGL_OPENGL_API)) {
                        my_index = FindIndex(attribList, sizeof(attribList), EGL_RENDERABLE_TYPE);
                        attribList[my_index + 1] = EGL_OPENGL_BIT;
                    }
                    break;
            }
            break;
    }
    return M64ERR_SUCCESS;
}

extern DECLSPEC m64p_error VidExtFuncGLGetAttr(m64p_GLattr Attr, int *pValue)
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

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
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	if(native_window != nullptr)
	{
		if (new_surface) {

			LOGI("VidExtFuncGLSwapBuf: New surface has been detected");

			if (!(surface = eglCreateWindowSurface(display, config, (EGLNativeWindowType)native_window, windowAttribList))) {
				LOGE("eglCreateWindowSurface() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			// This first eglMakeCurrent is needed for badly behaving GPU drivers
			if (!eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)) {
				LOGE("eglMakeCurrent() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			if (!eglMakeCurrent(display, surface, surface, context)) {
				LOGE("eglMakeCurrent() returned error %d", eglGetError());
				return M64ERR_INVALID_STATE;
			}

			eglSwapInterval(display, vsync);

			new_surface = false;
		}

		if(surface != EGL_NO_SURFACE)
		{
			if (vsync != oldVsync) {
				eglSwapInterval(display, vsync);
				oldVsync = vsync;
			}

			if (!isPaused) {
				eglSwapBuffers(display, surface);
			}
		}
	}

	if (FPSRecalcPeriod > 0) {
		frameCount++;
		if (frameCount >= FPSRecalcPeriod) {
			struct timespec spec;
			clock_gettime(CLOCK_MONOTONIC, &spec);
			int64_t currentTime = (int64_t) spec.tv_sec * 1000000000LL + spec.tv_nsec;
			float fFPS = ((float) frameCount / (float) (currentTime - oldTime)) * 1000000000.0f;
			Android_JNI_FPSCounter(lround(fFPS));
			frameCount = 0;
			oldTime = currentTime;
		}
	}

    return M64ERR_SUCCESS;
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_setNativeWindow(JNIEnv* env, jclass cls, jobject native_surface)
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	LOGI("setNativeWindow: New surface has been set");

	native_window = ANativeWindow_fromSurface(env, native_surface);
	new_surface = true;
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_unsetNativeWindow(JNIEnv* env, jclass cls, jobject native_surface)
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	LOGI("unsetNativeWindow: Native window has been unset");

	if(native_window != nullptr)
	{
		ANativeWindow_release(native_window);
		native_window = nullptr;

		//sleep for 50 ms to allow all queued swap buffer calls to finish
		usleep(50000);
	}
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuDestroySurface(JNIEnv* env, jclass cls)
{
	LOGI("emuDestroySurface: Deleting surface");

	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	if(native_window != nullptr)
	{
		ANativeWindow_release(native_window);
		native_window = nullptr;

		//sleep for 50 ms to allow all queued swap buffer calls to finish
		usleep(50000);
	}

    if (display != EGL_NO_DISPLAY && surface != EGL_NO_SURFACE)
        eglDestroySurface(display, surface);
    surface = EGL_NO_SURFACE;

	native_window = nullptr;
}

extern DECLSPEC m64p_error VidExtFuncQuit()
{
	std::unique_lock<std::mutex> guard(nativeWindowAccess);

	LOGI("VidExtFuncQuit");

	eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

	if (surface != EGL_NO_SURFACE) {
		eglDestroySurface(display, surface);
		surface = EGL_NO_SURFACE;
	}

	if(native_window != nullptr) {
		ANativeWindow_release(native_window);
		native_window = nullptr;
	}

	if (context != EGL_NO_CONTEXT) {
		eglDestroyContext(display, context);
		context = EGL_NO_CONTEXT;
	}

	if (display != EGL_NO_DISPLAY) {
		eglTerminate(display);
		display = EGL_NO_DISPLAY;
	}

	if (detachOnQuit()) {
        mJavaVM->DetachCurrentThread();
	}

	return M64ERR_SUCCESS;
}

extern DECLSPEC uint32_t VidExtFuncGLGetDefaultFramebuffer(void)
{
    return 0;
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_FPSEnabled(JNIEnv* env, jclass cls, int recalc)
{
    FPSRecalcPeriod = recalc;
}

extern DECLSPEC void vsyncEnabled(int enabled)
{
    vsync = enabled;
}

extern DECLSPEC void pauseEmulator()
{
    isPaused = true;
}

extern DECLSPEC void resumeEmulator()
{
    isPaused = false;
}
