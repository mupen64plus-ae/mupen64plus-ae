/*
  Simple DirectMedia Layer
  Copyright (C) 1997-2011 Sam Lantinga <slouken@libsdl.org>

  This software is provided 'as-is', without any express or implied
  warranty.  In no event will the authors be held liable for any damages
  arising from the use of this software.

  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:

  1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software
     in a product, an acknowledgment in the product documentation would be
     appreciated but is not required.
  2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.
  3. This notice may not be removed or altered from any source distribution.
*/
#include "SDL_config.h"
#include "SDL_stdinc.h"

#include "SDL_android.h"

extern "C" {
#include "../../events/SDL_events_c.h"
#include "../../video/android/SDL_androidkeyboard.h"
#include "../../video/android/SDL_androidtouch.h"
#include "../../video/android/SDL_androidvideo.h"

#include <android/log.h>
#include <pthread.h>

/* Uncomment this to log messages entering and exiting methods in this file */
//#define DEBUG_JNI

/* Impelemented in audio/android/SDL_androidaudio.c */
extern void Android_RunAudioThread();
} // C

/*******************************************************************************
 This file links the Java side of Android with libsdl
*******************************************************************************/
#include <jni.h>
#include <android/log.h>

/*******************************************************************************
                               Globals
*******************************************************************************/
static pthread_key_t mThreadKey;
static JavaVM* mJavaVM;

// Main activity
static jclass mActivityClass;

// method signatures
static jmethodID midCreateGLContext;
static jmethodID midFlipBuffers;
static jmethodID midAudioInit;
static jmethodID midAudioWriteShortBuffer;
static jmethodID midAudioWriteByteBuffer;
static jmethodID midAudioQuit;

// Accelerometer data storage
static float fLastAccelerometer[3];

/*******************************************************************************
                 Functions called by JNI
*******************************************************************************/

// Library init
extern "C" DECLSPEC jint SDLCALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv *env;
    mJavaVM = vm;
    if (mJavaVM->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    /*
     * Create mThreadKey so we can keep track of the JNIEnv assigned to each thread
     * Refer to http://developer.android.com/guide/practices/design/jni.html for the rationale behind this
     */
    if (pthread_key_create(&mThreadKey, Android_JNI_ThreadDestroyed)) {
        __android_log_print(ANDROID_LOG_ERROR, "SDL", "Error initializing pthread key");
    }
    else {
        Android_JNI_SetupThread();
    }

    return JNI_VERSION_1_4;
}

// Called before SDL_main() to initialize JNI bindings
extern "C" DECLSPEC void SDLCALL SDL_Android_Init(JNIEnv* mEnv, jclass cls)
{
    __android_log_print(ANDROID_LOG_INFO, "SDL", "SDL_Android_Init()");

    Android_JNI_SetupThread();

    mActivityClass = (jclass)mEnv->NewGlobalRef(cls);

    midCreateGLContext = mEnv->GetStaticMethodID(mActivityClass,
                                "createGLContext","(II)Z");
    midFlipBuffers = mEnv->GetStaticMethodID(mActivityClass,
                                "flipBuffers","()V");
    midAudioInit = mEnv->GetStaticMethodID(mActivityClass, 
                                "audioInit", "(IZZI)V");
    midAudioWriteShortBuffer = mEnv->GetStaticMethodID(mActivityClass,
                                "audioWriteShortBuffer", "([S)V");
    midAudioWriteByteBuffer = mEnv->GetStaticMethodID(mActivityClass,
                                "audioWriteByteBuffer", "([B)V");
    midAudioQuit = mEnv->GetStaticMethodID(mActivityClass,
                                "audioQuit", "()V");

    if(!midCreateGLContext || !midFlipBuffers || !midAudioInit ||
       !midAudioWriteShortBuffer || !midAudioWriteByteBuffer || !midAudioQuit) {
        __android_log_print(ANDROID_LOG_WARN, "SDL", "SDL: Couldn't locate Java callbacks, check that they're named and typed correctly");
    }
}

// Resize
extern "C" DECLSPEC void SDLCALL Java_org_libsdl_app_SDLActivity_onNativeResize(
                                    JNIEnv* env, jclass jcls,
                                    jint width, jint height, jint format)
{
    Android_SetScreenResolution(width, height, format);
}

// Accelerometer
extern "C" DECLSPEC void SDLCALL Java_org_libsdl_app_SDLActivity_onNativeAccel(
                                    JNIEnv* env, jclass jcls,
                                    jfloat x, jfloat y, jfloat z)
{
    fLastAccelerometer[0] = x;
    fLastAccelerometer[1] = y;
    fLastAccelerometer[2] = z;   
}

// Quit
extern "C" DECLSPEC void SDLCALL Java_org_libsdl_app_SDLActivity_nativeQuit(
                                    JNIEnv* env, jclass cls)
{    
    // Inject a SDL_QUIT event
    SDL_SendQuit();
}

extern "C" DECLSPEC void SDLCALL Java_org_libsdl_app_SDLActivity_nativeRunAudioThread(
                                    JNIEnv* env, jclass cls)
{
    /* This is the audio thread, with a different environment */
    Android_JNI_SetupThread();

    Android_RunAudioThread();
}

/*******************************************************************************
             Functions called by SDL into Java
*******************************************************************************/

extern "C" DECLSPEC SDL_bool SDLCALL Android_JNI_CreateContext(int majorVersion, int minorVersion)
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    if (mEnv->CallStaticBooleanMethod(mActivityClass, midCreateGLContext, majorVersion, minorVersion)) {
        return SDL_TRUE;
    } else {
        return SDL_FALSE;
    }
}

extern "C" DECLSPEC void SDLCALL Android_JNI_SwapWindow()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    mEnv->CallStaticVoidMethod(mActivityClass, midFlipBuffers); 
}

extern "C" DECLSPEC void SDLCALL Android_JNI_SetActivityTitle(const char *title)
{
    jmethodID mid;
    JNIEnv *mEnv = Android_JNI_GetEnv();
    mid = mEnv->GetStaticMethodID(mActivityClass,"setActivityTitle","(Ljava/lang/String;)V");
    if (mid) {
        jstring jtitle = reinterpret_cast<jstring>(mEnv->NewStringUTF(title));
        mEnv->CallStaticVoidMethod(mActivityClass, mid, jtitle);
        mEnv->DeleteLocalRef(jtitle);
    }
}

extern "C" DECLSPEC void SDLCALL Android_JNI_GetAccelerometerValues(float values[3])
{
    int i;
    for (i = 0; i < 3; ++i) {
        values[i] = fLastAccelerometer[i];
    }
}


static void Android_JNI_ThreadDestroyed(void* value) {
    /* The thread is being destroyed, detach it from the Java VM and set the mThreadKey value to NULL as required */
    JNIEnv *env = (JNIEnv*) value;
    if (env != NULL) {
        mJavaVM->DetachCurrentThread();
        pthread_setspecific(mThreadKey, NULL);
    }
}

JNIEnv* Android_JNI_GetEnv(void) {
    /* From http://developer.android.com/guide/practices/jni.html
     * All threads are Linux threads, scheduled by the kernel.
     * They're usually started from managed code (using Thread.start), but they can also be created elsewhere and then
     * attached to the JavaVM. For example, a thread started with pthread_create can be attached with the
     * JNI AttachCurrentThread or AttachCurrentThreadAsDaemon functions. Until a thread is attached, it has no JNIEnv,
     * and cannot make JNI calls.
     * Attaching a natively-created thread causes a java.lang.Thread object to be constructed and added to the "main"
     * ThreadGroup, making it visible to the debugger. Calling AttachCurrentThread on an already-attached thread
     * is a no-op.
     * Note: You can call this function any number of times for the same thread, there's no harm in it
     */

    JNIEnv *env;
    int status = mJavaVM->AttachCurrentThread(&env, NULL);
    if(status < 0) {
        return 0;
    }

    return env;
}

int Android_JNI_SetupThread(void) {
    /* From http://developer.android.com/guide/practices/jni.html
     * Threads attached through JNI must call DetachCurrentThread before they exit. If coding this directly is awkward,
     * in Android 2.0 (Eclair) and higher you can use pthread_key_create to define a destructor function that will be
     * called before the thread exits, and call DetachCurrentThread from there. (Use that key with pthread_setspecific
     * to store the JNIEnv in thread-local-storage; that way it'll be passed into your destructor as the argument.)
     * Note: The destructor is not called unless the stored value is != NULL
     * Note: You can call this function any number of times for the same thread, there's no harm in it
     *       (except for some lost CPU cycles)
     */
    JNIEnv *env = Android_JNI_GetEnv();
    pthread_setspecific(mThreadKey, (void*) env);
    return 1;
}

//
// Audio support
//
static jboolean audioBuffer16Bit = JNI_FALSE;
static jboolean audioBufferStereo = JNI_FALSE;
static jobject audioBuffer = NULL;
static void* audioBufferPinned = NULL;

extern "C" DECLSPEC int SDLCALL Android_JNI_OpenAudioDevice(int sampleRate, int is16Bit, int channelCount, int desiredBufferFrames)
{
    int audioBufferFrames;

    int status;
    JNIEnv *env = Android_JNI_GetEnv();

    Android_JNI_SetupThread();

    __android_log_print(ANDROID_LOG_VERBOSE, "SDL", "SDL audio: opening device");
    audioBuffer16Bit = is16Bit;
    audioBufferStereo = channelCount > 1;

    env->CallStaticVoidMethod(mActivityClass, midAudioInit, sampleRate, audioBuffer16Bit, audioBufferStereo, desiredBufferFrames);

    /* Allocating the audio buffer from the Java side and passing it as the return value for audioInit no longer works on
     * Android >= 4.2 due to a "stale global reference" error. So now we allocate this buffer directly from this side. */
    
    if (is16Bit) {
        jshortArray audioBufferLocal = env->NewShortArray(desiredBufferFrames * (audioBufferStereo ? 2 : 1));
        if (audioBufferLocal) {
            audioBuffer = env->NewGlobalRef(audioBufferLocal);
            env->DeleteLocalRef(audioBufferLocal);
        }
    }
    else {
        jbyteArray audioBufferLocal = env->NewByteArray(desiredBufferFrames * (audioBufferStereo ? 2 : 1));
        if (audioBufferLocal) {
            audioBuffer = env->NewGlobalRef(audioBufferLocal);
            env->DeleteLocalRef(audioBufferLocal);
        }
    }

    if (audioBuffer == NULL) {
        __android_log_print(ANDROID_LOG_WARN, "SDL", "SDL audio: could not allocate an audio buffer!");
        return 0;
    }

    jboolean isCopy = JNI_FALSE;
    if (audioBuffer16Bit) {
        audioBufferPinned = env->GetShortArrayElements((jshortArray)audioBuffer, &isCopy);
        audioBufferFrames = env->GetArrayLength((jshortArray)audioBuffer);
    } else {
        audioBufferPinned = env->GetByteArrayElements((jbyteArray)audioBuffer, &isCopy);
        audioBufferFrames = env->GetArrayLength((jbyteArray)audioBuffer);
    }
    if (audioBufferStereo) {
        audioBufferFrames /= 2;
    }

    return audioBufferFrames;
}

extern "C" DECLSPEC void * SDLCALL Android_JNI_GetAudioBuffer()
{
    return audioBufferPinned;
}

extern "C" DECLSPEC void SDLCALL Android_JNI_WriteAudioBuffer()
{
    JNIEnv *mAudioEnv = Android_JNI_GetEnv();

    if (audioBuffer16Bit) {
        mAudioEnv->ReleaseShortArrayElements((jshortArray)audioBuffer, (jshort *)audioBufferPinned, JNI_COMMIT);
        mAudioEnv->CallStaticVoidMethod(mActivityClass, midAudioWriteShortBuffer, (jshortArray)audioBuffer);
    } else {
        mAudioEnv->ReleaseByteArrayElements((jbyteArray)audioBuffer, (jbyte *)audioBufferPinned, JNI_COMMIT);
        mAudioEnv->CallStaticVoidMethod(mActivityClass, midAudioWriteByteBuffer, (jbyteArray)audioBuffer);
    }

    /* JNI_COMMIT means the changes are committed to the VM but the buffer remains pinned */
}

extern "C" DECLSPEC void SDLCALL Android_JNI_CloseAudioDevice()
{
    int status;
    JNIEnv *env = Android_JNI_GetEnv();

    env->CallStaticVoidMethod(mActivityClass, midAudioQuit); 

    if (audioBuffer) {
        env->DeleteGlobalRef(audioBuffer);
        audioBuffer = NULL;
        audioBufferPinned = NULL;
    }
}

/* vi: set ts=4 sw=4 expandtab: */
