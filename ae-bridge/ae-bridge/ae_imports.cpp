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

#include "SDL.h"
#include "ae_imports.h"

/*******************************************************************************
 Globals used internally
 *******************************************************************************/

JavaVM* mJavaVM;

// Imported java class reference
static jclass mActivityClass;

// Imported java method references
static jmethodID midStateCallback;
static jmethodID midFPSCounter;


static bool detachOnQuitCore = false;

/*******************************************************************************
 Functions called automatically by JNI framework
 *******************************************************************************/

// Library init
extern jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    mJavaVM = vm;
    return JNI_VERSION_1_6;
}

/*******************************************************************************
 Functions called by native code
 *******************************************************************************/

extern DECLSPEC void Android_JNI_InitImports(JNIEnv* env, jclass cls)
{
    LOGI("Android_JNI_InitImports()");

    mActivityClass = (jclass) env->NewGlobalRef(cls);
    midStateCallback = env->GetStaticMethodID(mActivityClass, "stateCallback", "(II)V");
    midFPSCounter = env->GetStaticMethodID(mActivityClass, "FPSCounter", "(I)V");
    if (!midStateCallback || !midFPSCounter)
    {
        LOGE("Couldn't locate Java callbacks, check that they're named and typed correctly");
    }
}

// Called by ae-exports
extern DECLSPEC void Android_JNI_DestroyImports(JNIEnv* env)
{
    LOGI("Android_JNI_DestroyImports()");

    env->DeleteGlobalRef(mActivityClass);
}

extern DECLSPEC void Android_JNI_StateCallback(void* context, m64p_core_param paramChanged, int newValue)
{
    /*----ParamChanged-----------------
     *    --------NewValue--------
     *    M64CORE_EMU_STATE           1
     *            M64EMU_STOPPED 1
     *            M64EMU_RUNNING 2
     *            M64EMU_PAUSED  3
     *    M64CORE_VIDEO_MODE          2
     *    M64CORE_SAVESTATE_SLOT      3
     *    M64CORE_SPEED_FACTOR        4
     *    M64CORE_SPEED_LIMITER       5
	 *    M64CORE_VIDEO_SIZE          6
	 *    M64CORE_AUDIO_VOLUME        7
	 *    M64CORE_AUDIO_MUTE          8
	 *    M64CORE_INPUT_GAMESHARK     9
	 *    M64CORE_STATE_LOADCOMPLETE 10
	 *            (successful)   1
	 *            (unsuccessful) 0
	 *    M64CORE_STATE_SAVECOMPLETE 11
	 *            (successful)   1
	 *            (unsuccessful) 0
     */
    JNIEnv *env;
    if (mJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK)
        return;
    env->CallStaticVoidMethod(mActivityClass, midStateCallback, (int) paramChanged, newValue);
}

extern DECLSPEC void Android_JNI_FPSCounter(int fps)
{
    JNIEnv *env;
    if (mJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        mJavaVM->AttachCurrentThread(&env, nullptr);
        detachOnQuitCore = true;
        return;
    }
    env->CallStaticVoidMethod(mActivityClass, midFPSCounter, fps);
}

extern DECLSPEC int detachOnQuit()
{
    return detachOnQuitCore;
}