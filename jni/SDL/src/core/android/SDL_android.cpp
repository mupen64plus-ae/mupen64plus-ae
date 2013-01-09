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
static JNIEnv* mEnv = NULL;
static JNIEnv* mAudioEnv = NULL;

// Main activity
static jclass mActivityClass;

// method signatures
static jmethodID midCreateGLContext;
static jmethodID midVibrate;
static jmethodID midUseRGBA8888;
static jmethodID midFlipBuffers;
static jmethodID midGetDataDir;
static jmethodID midGetHardwareType;
static jmethodID midGetExtraArgs;
static jmethodID midGetROMPath;
static jmethodID midGetScreenStretch;
static jmethodID midGetAutoFrameSkip;
static jmethodID midGetMaxFrameSkip;
static jmethodID midEmuStateCallback;
static jmethodID midShowToast;
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
    return JNI_VERSION_1_4;
}

// Called before SDL_main() to initialize JNI bindings
extern "C" DECLSPEC void SDLCALL SDL_Android_Init(JNIEnv* env, jclass cls)
{
    __android_log_print(ANDROID_LOG_INFO, "SDL", "SDL_Android_Init()");

    mEnv = env;
    mActivityClass = cls;

    midCreateGLContext = mEnv->GetStaticMethodID(mActivityClass,
                                "createGLContext","(II)Z");
    midVibrate = mEnv->GetStaticMethodID(mActivityClass,
                                "vibrate","(Z)V");
    midUseRGBA8888 = mEnv->GetStaticMethodID(mActivityClass,
                                "useRGBA8888","()Z");
    midFlipBuffers = mEnv->GetStaticMethodID(mActivityClass,
                                "flipBuffers","()V");
    midGetDataDir = mEnv->GetStaticMethodID(mActivityClass,
                                "getDataDir", "()Ljava/lang/Object;");
    midGetHardwareType = mEnv->GetStaticMethodID(mActivityClass,
                                "getHardwareType", "()I");
    midGetExtraArgs = mEnv->GetStaticMethodID(mActivityClass,
                                "getExtraArgs", "()Ljava/lang/Object;");
    midGetROMPath = mEnv->GetStaticMethodID(mActivityClass,
                                "getROMPath", "()Ljava/lang/Object;");
    midGetScreenStretch = mEnv->GetStaticMethodID(mActivityClass,
                                "getScreenStretch", "()Z");
    midGetAutoFrameSkip = mEnv->GetStaticMethodID(mActivityClass,
                                "getAutoFrameSkip", "()Z");
    midGetMaxFrameSkip = mEnv->GetStaticMethodID(mActivityClass,
                                "getMaxFrameSkip", "()I");
    midEmuStateCallback = mEnv->GetStaticMethodID(mActivityClass,
                                "emuStateCallback", "(I)V");
    midShowToast = mEnv->GetStaticMethodID(mActivityClass,
                                "showToast", "(Ljava/lang/String;)V");
    midAudioInit = mEnv->GetStaticMethodID(mActivityClass, 
                                "audioInit", "(IZZI)Ljava/lang/Object;");
    midAudioWriteShortBuffer = mEnv->GetStaticMethodID(mActivityClass,
                                "audioWriteShortBuffer", "([S)V");
    midAudioWriteByteBuffer = mEnv->GetStaticMethodID(mActivityClass,
                                "audioWriteByteBuffer", "([B)V");
    midAudioQuit = mEnv->GetStaticMethodID(mActivityClass,
                                "audioQuit", "()V");

    if(!midCreateGLContext || !midVibrate || !midUseRGBA8888 || !midFlipBuffers || !midGetDataDir ||
       !midGetHardwareType || !midGetExtraArgs || !midGetROMPath || !midGetScreenStretch ||
       !midGetAutoFrameSkip || !midGetMaxFrameSkip || ! midEmuStateCallback || !midShowToast ||
       !midAudioInit || !midAudioWriteShortBuffer || !midAudioWriteByteBuffer || !midAudioQuit)
    {
        __android_log_print(ANDROID_LOG_WARN, "SDL", "SDL: Couldn't locate Java callbacks, check that they're named and typed correctly");
    }
}

// Resize
extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_NativeMethods_onResize(
                                    JNIEnv* env, jclass jcls,
                                    jint width, jint height, jint format)
{
    Android_SetScreenResolution(width, height, format);
}

// Accelerometer
extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_NativeMethods_onAccel(
                                    JNIEnv* env, jclass jcls,
                                    jfloat x, jfloat y, jfloat z)
{
    fLastAccelerometer[0] = x;
    fLastAccelerometer[1] = y;
    fLastAccelerometer[2] = z;   
}

// Quit
extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_NativeMethods_quit(
                                    JNIEnv* env, jclass cls)
{    
    // Inject a SDL_QUIT event
    SDL_SendQuit();
}

extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_NativeMethods_runAudioThread(
                                    JNIEnv* env, jclass cls)
{
    /* This is the audio thread, with a different environment */
    mAudioEnv = env;

    Android_RunAudioThread();
}

//// paulscode, added for different configurations based on hardware
// (part of the missing shadows and stars bug fix)
extern "C" DECLSPEC int SDLCALL Android_JNI_GetHardwareType()
{
    jint hardwareType = mEnv->CallStaticIntMethod(mActivityClass, midGetHardwareType);
    return (int) hardwareType;
}
////

/*******************************************************************************
             Functions called by SDL into Java
*******************************************************************************/

extern "C" DECLSPEC SDL_bool SDLCALL Android_JNI_CreateContext(int majorVersion, int minorVersion)
{
    if (mEnv->CallStaticBooleanMethod(mActivityClass, midCreateGLContext, majorVersion, minorVersion)) {
        return SDL_TRUE;
    } else {
        return SDL_FALSE;
    }
}

extern "C" DECLSPEC void SDLCALL Android_JNI_Vibrate( int active )
{
    jboolean a = JNI_FALSE;
    if( active )
        a = JNI_TRUE;
    mEnv->CallStaticVoidMethod( mActivityClass, midVibrate, a ); 
}

extern "C" DECLSPEC int SDLCALL Android_JNI_UseRGBA8888()
{
    if (mEnv->CallStaticBooleanMethod(mActivityClass, midUseRGBA8888)) {
        return 1;
    } else {
        return 0;
    }
}

extern "C" DECLSPEC void SDLCALL Android_JNI_SwapWindow()
{
    mEnv->CallStaticVoidMethod(mActivityClass, midFlipBuffers); 
}

static jstring dataDirString = NULL;
static char appDataDir[60];
extern "C" DECLSPEC char * SDLCALL Android_JNI_GetDataDir()
{
    dataDirString = (jstring) mEnv->CallStaticObjectMethod( mActivityClass, midGetDataDir );
    const char *nativeString = mEnv->GetStringUTFChars( dataDirString, 0 );
    strcpy( appDataDir, nativeString );
    mEnv->ReleaseStringUTFChars( dataDirString, nativeString );
    return appDataDir;
}

static jstring buffString = NULL;
static char buffArray[1024];
extern "C" DECLSPEC char * SDLCALL Android_JNI_GetExtraArgs()
{
    buffString = (jstring) mEnv->CallStaticObjectMethod( mActivityClass, midGetExtraArgs );
    const char *nativeString = mEnv->GetStringUTFChars( buffString, 0 );
    strcpy( buffArray, nativeString );
    mEnv->ReleaseStringUTFChars( buffString, nativeString );
    return buffArray;
}
extern "C" DECLSPEC char * SDLCALL Android_JNI_GetROMPath()
{
    buffString = (jstring) mEnv->CallStaticObjectMethod( mActivityClass, midGetROMPath );
    const char *nativeString = mEnv->GetStringUTFChars( buffString, 0 );
    strcpy( buffArray, nativeString );
    mEnv->ReleaseStringUTFChars( buffString, nativeString );
    return buffArray;
}
extern "C" DECLSPEC int SDLCALL Android_JNI_GetScreenStretch()
{
    jboolean b;
    b = mEnv->CallStaticBooleanMethod( mActivityClass, midGetScreenStretch );
    if( b == JNI_TRUE )
        return 1;
    else
        return 0;
}
extern "C" DECLSPEC int SDLCALL Android_JNI_GetAutoFrameSkip()
{
    jboolean b;
    b = mEnv->CallStaticBooleanMethod( mActivityClass, midGetAutoFrameSkip );
    if( b == JNI_TRUE )
        return 1;
    else
        return 0;
}
extern "C" DECLSPEC int SDLCALL Android_JNI_GetMaxFrameSkip()
{
    __android_log_print( ANDROID_LOG_VERBOSE, "SDL-android", "About to call midGetMaxFrameSkip" );
    jint i = mEnv->CallStaticIntMethod( mActivityClass, midGetMaxFrameSkip );
    __android_log_print( ANDROID_LOG_VERBOSE, "SDL-android", "Android_JNI_GetMaxFrameSkip returning %i", (int) i );
    return (int) i;
}
extern "C" DECLSPEC void SDLCALL Android_JNI_EMU_STATE_Callback( int newState )
{
    __android_log_print( ANDROID_LOG_VERBOSE, "SDL-android", "Emulator state changed to %i", newState );
    mEnv->CallStaticVoidMethod( mActivityClass, midEmuStateCallback, newState );
}

// paulscode, added for showing the user a short message
static jstring jmessage = NULL;
extern "C" DECLSPEC void SDLCALL Android_JNI_ShowToast( const char *message )
{
    jmessage = mEnv->NewStringUTF( message );
    mEnv->CallStaticVoidMethod( mActivityClass, midShowToast, jmessage );
    mEnv->DeleteLocalRef( jmessage );
}
//

extern "C" DECLSPEC void SDLCALL Android_JNI_SetActivityTitle(const char *title)
{
    jmethodID mid;

    mid = mEnv->GetStaticMethodID(mActivityClass,"setActivityTitle","(Ljava/lang/String;)V");
    if (mid) {
        mEnv->CallStaticVoidMethod(mActivityClass, mid, mEnv->NewStringUTF(title));
    }
}

extern "C" DECLSPEC void SDLCALL Android_JNI_GetAccelerometerValues(float values[3])
{
    int i;
    for (i = 0; i < 3; ++i) {
        values[i] = fLastAccelerometer[i];
    }
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

    __android_log_print(ANDROID_LOG_VERBOSE, "SDL", "SDL audio: opening device");
    audioBuffer16Bit = is16Bit;
    audioBufferStereo = channelCount > 1;

    audioBuffer = mEnv->CallStaticObjectMethod(mActivityClass, midAudioInit, sampleRate, audioBuffer16Bit, audioBufferStereo, desiredBufferFrames);

    if (audioBuffer == NULL) {
        __android_log_print(ANDROID_LOG_WARN, "SDL", "SDL audio: didn't get back a good audio buffer!");
        return 0;
    }
    audioBuffer = mEnv->NewGlobalRef(audioBuffer);

    jboolean isCopy = JNI_FALSE;
    if (audioBuffer16Bit) {
        audioBufferPinned = mEnv->GetShortArrayElements((jshortArray)audioBuffer, &isCopy);
        audioBufferFrames = mEnv->GetArrayLength((jshortArray)audioBuffer);
    } else {
        audioBufferPinned = mEnv->GetByteArrayElements((jbyteArray)audioBuffer, &isCopy);
        audioBufferFrames = mEnv->GetArrayLength((jbyteArray)audioBuffer);
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
    mEnv->CallStaticVoidMethod(mActivityClass, midAudioQuit); 

    if (audioBuffer) {
        mEnv->DeleteGlobalRef(audioBuffer);
        audioBuffer = NULL;
        audioBufferPinned = NULL;
    }
}

/* vi: set ts=4 sw=4 expandtab: */
