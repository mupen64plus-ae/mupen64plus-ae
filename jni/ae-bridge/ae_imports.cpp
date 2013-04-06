/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 *
 * Copyright (C) 2013 Paul Lamb
 *
 * This file is part of Mupen64PlusAE.
 *
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: Paul Lamb, littleguy77
 */

#include <SDL.h>
#include <pthread.h>
#include "ae_bridge.h"

/*******************************************************************************
 Globals used externally
 *******************************************************************************/

DECLSPEC int do_Start = 1;

/*******************************************************************************
 Globals used internally
 *******************************************************************************/

static pthread_key_t mThreadKey;
static JavaVM* mJavaVM;

// Imported java class reference
static jclass mActivityClass;

// Imported java method references
static jmethodID midVibrate;
static jmethodID midStateCallback;
static jmethodID midShowToast;
static jmethodID midGetHardwareType;
static jmethodID midGetDataDir;
static jmethodID midGetROMPath;
static jmethodID midGetExtraArgs;
static jmethodID midGetAutoFrameSkip;
static jmethodID midGetMaxFrameSkip;
static jmethodID midGetScreenPosition;
static jmethodID midGetScreenStretch;
static jmethodID midUseRGBA8888;

// Buffers
static jstring jmessage = NULL;
static jstring dataDirString = NULL;
static jstring buffString = NULL;
static char appDataDir[60];
static char buffArray[1024];

/*******************************************************************************
 Functions called internally
 *******************************************************************************/

static void Android_JNI_ThreadDestroyed(void* value)
{
    /* The thread is being destroyed, detach it from the Java VM and set the mThreadKey value to NULL as required */
    JNIEnv *env = (JNIEnv*) value;
    if (env != NULL)
    {
        mJavaVM->DetachCurrentThread();
        pthread_setspecific(mThreadKey, NULL);
    }
}

static JNIEnv* Android_JNI_GetEnv(void)
{
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
    if (status < 0)
    {
        return 0;
    }

    return env;
}

static int Android_JNI_SetupThread(void)
{
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

/*******************************************************************************
 Functions called automatically by JNI framework
 *******************************************************************************/

// Library init
extern jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv *env;
    mJavaVM = vm;
    if (mJavaVM->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
    {
        return -1;
    }
    /*
     * Create mThreadKey so we can keep track of the JNIEnv assigned to each thread
     * Refer to http://developer.android.com/guide/practices/design/jni.html for the rationale behind this
     */
    if (pthread_key_create(&mThreadKey, Android_JNI_ThreadDestroyed))
    {
        __android_log_print(ANDROID_LOG_ERROR, "ae-bridge", "Error initializing pthread key");
    }
    else
    {
        Android_JNI_SetupThread();
    }

    return JNI_VERSION_1_4;
}

/*******************************************************************************
 Functions called during main initialization
 *******************************************************************************/

// Called before SDL_main() to initialize JNI bindings
extern DECLSPEC void SDL_Android_Init_Extras(JNIEnv* mEnv, jclass cls)
{
    __android_log_print(ANDROID_LOG_INFO, "SDL", "SDL_Android_Init()");

    Android_JNI_SetupThread();

    mActivityClass = (jclass) mEnv->NewGlobalRef(cls);

    midVibrate              = mEnv->GetStaticMethodID(mActivityClass, "vibrate",            "(Z)V");
    midStateCallback        = mEnv->GetStaticMethodID(mActivityClass, "stateCallback",      "(II)V");
    midShowToast            = mEnv->GetStaticMethodID(mActivityClass, "showToast",          "(Ljava/lang/String;)V");
    midGetHardwareType      = mEnv->GetStaticMethodID(mActivityClass, "getHardwareType",    "()I");
    midGetDataDir           = mEnv->GetStaticMethodID(mActivityClass, "getDataDir",         "()Ljava/lang/Object;");
    midGetROMPath           = mEnv->GetStaticMethodID(mActivityClass, "getROMPath",         "()Ljava/lang/Object;");
    midGetExtraArgs         = mEnv->GetStaticMethodID(mActivityClass, "getExtraArgs",       "()Ljava/lang/Object;");
    midGetAutoFrameSkip     = mEnv->GetStaticMethodID(mActivityClass, "getAutoFrameSkip",   "()Z");
    midGetMaxFrameSkip      = mEnv->GetStaticMethodID(mActivityClass, "getMaxFrameSkip",    "()I");
    midGetScreenPosition    = mEnv->GetStaticMethodID(mActivityClass, "getScreenPosition",  "()I");
    midGetScreenStretch     = mEnv->GetStaticMethodID(mActivityClass, "getScreenStretch",   "()Z");
    midUseRGBA8888          = mEnv->GetStaticMethodID(mActivityClass, "useRGBA8888",        "()Z");

    if (!midVibrate || !midStateCallback || !midShowToast || !midGetHardwareType ||
        !midGetDataDir || !midGetROMPath || !midGetExtraArgs || !midGetAutoFrameSkip ||
        !midGetMaxFrameSkip || !midGetScreenPosition || !midGetScreenStretch || !midUseRGBA8888)
    {
        __android_log_print(ANDROID_LOG_ERROR, "ae-bridge", "Couldn't locate Java callbacks, check that they're named and typed correctly");
    }
}

/*******************************************************************************
 Functions called by native code
 *******************************************************************************/

extern DECLSPEC void Android_JNI_Vibrate(int active)
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    jboolean a = JNI_FALSE;
    if (active)
        a = JNI_TRUE;
    mEnv->CallStaticVoidMethod(mActivityClass, midVibrate, a);
}

extern DECLSPEC void Android_JNI_ShowToast(const char *message)
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    jmessage = mEnv->NewStringUTF(message);
    mEnv->CallStaticVoidMethod(mActivityClass, midShowToast, jmessage);
    mEnv->DeleteLocalRef(jmessage);
}

extern DECLSPEC void Android_JNI_State_Callback(int paramChanged, int newValue)
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    __android_log_print(ANDROID_LOG_VERBOSE, "SDL-android", "Emulator param %i changed to %i", paramChanged, newValue);
    mEnv->CallStaticVoidMethod(mActivityClass, midStateCallback, paramChanged, newValue);
}

extern DECLSPEC int Android_JNI_GetHardwareType()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    jint hardwareType = mEnv->CallStaticIntMethod(mActivityClass, midGetHardwareType);
    return (int) hardwareType;
}

extern DECLSPEC char * Android_JNI_GetDataDir()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    dataDirString = (jstring) mEnv->CallStaticObjectMethod(mActivityClass, midGetDataDir);
    const char *nativeString = mEnv->GetStringUTFChars(dataDirString, 0);
    strcpy(appDataDir, nativeString);
    mEnv->ReleaseStringUTFChars(dataDirString, nativeString);
    return appDataDir;
}

extern DECLSPEC char * Android_JNI_GetROMPath()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    buffString = (jstring) mEnv->CallStaticObjectMethod(mActivityClass, midGetROMPath);
    const char *nativeString = mEnv->GetStringUTFChars(buffString, 0);
    strcpy(buffArray, nativeString);
    mEnv->ReleaseStringUTFChars(buffString, nativeString);
    return buffArray;
}

extern DECLSPEC char * Android_JNI_GetExtraArgs()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    buffString = (jstring) mEnv->CallStaticObjectMethod(mActivityClass, midGetExtraArgs);
    const char *nativeString = mEnv->GetStringUTFChars(buffString, 0);
    strcpy(buffArray, nativeString);
    mEnv->ReleaseStringUTFChars(buffString, nativeString);
    return buffArray;
}

extern DECLSPEC int Android_JNI_GetAutoFrameSkip()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    jboolean b;
    b = mEnv->CallStaticBooleanMethod(mActivityClass, midGetAutoFrameSkip);
    if (b == JNI_TRUE)
        return 1;
    else
        return 0;
}

extern DECLSPEC int Android_JNI_GetMaxFrameSkip()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    __android_log_print(ANDROID_LOG_VERBOSE, "SDL-android", "About to call midGetMaxFrameSkip");
    jint i = mEnv->CallStaticIntMethod(mActivityClass, midGetMaxFrameSkip);
    __android_log_print(ANDROID_LOG_VERBOSE, "SDL-android", "Android_JNI_GetMaxFrameSkip returning %i", (int) i);
    return (int) i;
}

extern DECLSPEC int Android_JNI_GetScreenStretch()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    jboolean b;
    b = mEnv->CallStaticBooleanMethod(mActivityClass, midGetScreenStretch);
    if (b == JNI_TRUE)
        return 1;
    else
        return 0;
}

extern DECLSPEC int Android_JNI_GetScreenPosition()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    __android_log_print(ANDROID_LOG_VERBOSE, "SDL-android", "About to call midGetScreenPosition");
    jint i = mEnv->CallStaticIntMethod(mActivityClass, midGetScreenPosition);
    __android_log_print(ANDROID_LOG_VERBOSE, "SDL-android", "Android_JNI_GetScreenPosition returning %i", (int) i);
    return (int) i;
}

extern DECLSPEC int Android_JNI_UseRGBA8888()
{
    JNIEnv *mEnv = Android_JNI_GetEnv();
    if (mEnv->CallStaticBooleanMethod(mActivityClass, midUseRGBA8888))
    {
        return 1;
    }
    else
    {
        return 0;
    }
}
