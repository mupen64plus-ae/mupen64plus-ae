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
// Store custom polygon offset natively to reduce JNI calls
static float customPolygonOffset = -0.2f;

// Imported java class reference
static jclass mActivityClass;

// Imported java method references
static jmethodID midStateCallback;
static jmethodID midGetHardwareType;
static jmethodID midGetCustomPolygonOffset;
static jmethodID midUseRGBA8888;

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

static int GetBooleanAsInt(jmethodID methodID)
{
    JNIEnv *env = Android_JNI_GetEnv();
    jboolean b = env->CallStaticBooleanMethod(mActivityClass, methodID);
    return (b == JNI_TRUE) ? 1 : 0;
}

static int GetInt(jmethodID methodID)
{
    JNIEnv *env = Android_JNI_GetEnv();
    jint i = env->CallStaticIntMethod(mActivityClass, methodID);
    return (int) i;
}

static float GetFloat(jmethodID methodID)
{
    JNIEnv *env = Android_JNI_GetEnv();
    jfloat f = env->CallStaticFloatMethod(mActivityClass, methodID);
    return (float) f;
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
        LOGE("Error initializing pthread key");
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
extern DECLSPEC void SDL_Android_Init_Extras(JNIEnv* env, jclass cls)
{
    LOGI("SDL_Android_Init_Extras()");

    Android_JNI_SetupThread();

    mActivityClass = (jclass) env->NewGlobalRef(cls);

    midStateCallback          = env->GetStaticMethodID(mActivityClass, "stateCallback",          "(II)V");
    midGetHardwareType        = env->GetStaticMethodID(mActivityClass, "getHardwareType",        "()I");
    midGetCustomPolygonOffset = env->GetStaticMethodID(mActivityClass, "getCustomPolygonOffset", "()F");
    midUseRGBA8888            = env->GetStaticMethodID(mActivityClass, "useRGBA8888",            "()Z");

    if (!midStateCallback || !midGetHardwareType || !midGetCustomPolygonOffset || !midUseRGBA8888)
    {
        LOGE("Couldn't locate Java callbacks, check that they're named and typed correctly");
    }
}

/*******************************************************************************
 Functions called by native code
 *******************************************************************************/

extern DECLSPEC void Android_JNI_State_Callback(int paramChanged, int newValue)
{
    JNIEnv *env = Android_JNI_GetEnv();
    env->CallStaticVoidMethod(mActivityClass, midStateCallback, paramChanged, newValue);
}

extern DECLSPEC int Android_JNI_GetHardwareType()
{
    int hardwareType = GetInt( midGetHardwareType );
    if( hardwareType == HARDWARE_TYPE_CUSTOM )
    {
        customPolygonOffset = GetFloat( midGetCustomPolygonOffset );
    }
    return hardwareType;
}

extern DECLSPEC int Android_JNI_UseRGBA8888()
{
    return GetBooleanAsInt(midUseRGBA8888);
}

extern DECLSPEC void Android_JNI_GetPolygonOffset(const int hardwareType, const int bias, float* f1, float* f2)
{
    // Part of the missing shadows and stars bug fix
    if( hardwareType == HARDWARE_TYPE_OMAP )
    {
        *f1 = bias > 0 ? 0.2f : 0.0f;
        *f2 = bias > 0 ? 0.2f : 0.0f;
    }
    else if( hardwareType == HARDWARE_TYPE_OMAP_2 )
    {
        *f1 = bias > 0 ? -1.5f : 0.0f;
        *f2 = bias > 0 ? -1.5f : 0.0f;
    }
    else if( hardwareType == HARDWARE_TYPE_QUALCOMM )
    {
        *f1 = bias > 0 ? -0.2f : 0.0f;
        *f2 = bias > 0 ? -0.2f : 0.0f;
    }
    else if( hardwareType == HARDWARE_TYPE_IMAP )
    {
        *f1 = bias > 0 ? -0.001f : 0.0f;
        *f2 = bias > 0 ? -0.001f : 0.0f;
    }
    else if( hardwareType == HARDWARE_TYPE_TEGRA )
    {
        *f1 = bias > 0 ? -2.0f : 0.0f;
        *f2 = bias > 0 ? -2.0f : 0.0f;
    }
    else if( hardwareType == HARDWARE_TYPE_CUSTOM )
    {
        *f1 = bias > 0 ? customPolygonOffset : 0.0f;
        *f2 = bias > 0 ? customPolygonOffset : 0.0f;
    }
    else // HARDWARE_TYPE_UNKNOWN
    {
        *f1 = bias > 0 ? -0.2f : 0.0f;
        *f2 = bias > 0 ? -0.2f : 0.0f;
    }
}
