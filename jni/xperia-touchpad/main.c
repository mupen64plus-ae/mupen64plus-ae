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
 * Authors: littleguy77, Paul Lamb
 */

#include <jni.h>
#include <android/log.h>
#include <android_native_app_glue.h>

#define TAG "xperia-touchpad"
#define LOGI(...) ((void)__android_log_print( ANDROID_LOG_INFO, TAG, __VA_ARGS__ ))
#define LOGE(...) ((void)__android_log_print( ANDROID_LOG_ERROR, TAG, __VA_ARGS__ ))

#undef NUM_METHODS
#define NUM_METHODS(x) (sizeof(x)/sizeof(*(x)))

#define MAX_POINTERS                64
#define PAD_HEIGHT                  360
#define PAD_WIDTH                   966
#define SOURCE_TOUCHSCREEN          4098
#define SOURCE_TOUCHPAD             1048584

static JNIEnv *g_pEnv = 0;
static JavaVM *g_pVM = 0;
static jobject g_pActivity = 0;
static jmethodID javaOnNativeKey = 0;
static jmethodID javaOnNativeTouch = 0;

static int RegisterThis(JNIEnv* env, jobject clazz)
{
    LOGI("RegisterThis() was called");
    g_pActivity = (jobject)(*env)->NewGlobalRef(env, clazz);

    return 0;
}

static const JNINativeMethod activity_methods[] =
{
{ "RegisterThis", "()I", (void*) RegisterThis } };

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM * vm, void * reserved)
{
    JNIEnv* env = 0;
    g_pVM = vm;

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK)
    {
        LOGE("%s - Failed to get the environment using GetEnv()", __FUNCTION__);
        return -1;
    }

    const char* interface_path = "paulscode/android/mupen64plusae/jni/NativeXperiaTouchpad";
    jclass java_activity_class = (*env)->FindClass(env, interface_path);

    if (!java_activity_class)
    {
        LOGE("%s - Failed to get %s class reference", __FUNCTION__, interface_path);
        return -1;
    }

    if ((*env)->RegisterNatives(env, java_activity_class, activity_methods, NUM_METHODS(activity_methods)) != JNI_OK)
    {
        LOGE("%s - Failed to register native activity methods", __FUNCTION__);
        return -1;
    }

    javaOnNativeKey = (*env)->GetMethodID(env, java_activity_class, "onNativeKey", "(II)Z");
    if (!javaOnNativeKey)
    {
        if ((*env)->ExceptionCheck(env))
        {
            LOGE("%s - GetMethodID( 'onNativeKey' ) threw exception!", __FUNCTION__);
            (*env)->ExceptionClear(env);
        }
        return JNI_FALSE;
    }
    javaOnNativeTouch = (*env)->GetMethodID(env, java_activity_class, "onNativeTouch", "(III[I[F[F)Z");
    if (!javaOnNativeTouch)
    {
        if ((*env)->ExceptionCheck(env))
        {
            LOGE("%s - GetMethodID( 'onNativeTouch' ) threw exception!", __FUNCTION__);
            (*env)->ExceptionClear(env);
        }
        return JNI_FALSE;
    }

    LOGI("%s - Complete", __FUNCTION__);

    return JNI_VERSION_1_4;
}

/**
 * Process the next input event.
 */
static int32_t onInputEvent(struct android_app* app, AInputEvent* event)
{
    if (AInputEvent_getType(event) == AINPUT_EVENT_TYPE_KEY)
    {
        int nKeyCode = AKeyEvent_getKeyCode(event);
        int nAction = AKeyEvent_getAction(event);
        int handled = 0;
        if (g_pEnv && g_pActivity)
            handled = (*g_pEnv)->CallIntMethod(g_pEnv, g_pActivity, javaOnNativeKey, nAction, nKeyCode);
        return handled;
    }

    else if (AInputEvent_getType(event) == AINPUT_EVENT_TYPE_MOTION && g_pEnv)
    {
        int action = AMotionEvent_getAction(event);
        int source = AInputEvent_getSource(event);
        int pointerCount = AMotionEvent_getPointerCount(event);

        jintArray pointerIds = (*g_pEnv)->NewIntArray(g_pEnv, MAX_POINTERS);
        jfloatArray pointerX = (*g_pEnv)->NewFloatArray(g_pEnv, MAX_POINTERS);
        jfloatArray pointerY = (*g_pEnv)->NewFloatArray(g_pEnv, MAX_POINTERS);

        jint * tempIds = (*g_pEnv)->GetIntArrayElements(g_pEnv, pointerIds, 0);
        jfloat * tempX = (*g_pEnv)->GetFloatArrayElements(g_pEnv, pointerX, 0);
        jfloat * tempY = (*g_pEnv)->GetFloatArrayElements(g_pEnv, pointerY, 0);

        int i;
        for (i = 0; i < pointerCount; i++)
        {
            tempIds[i] = AMotionEvent_getPointerId(event, i);
            tempX[i] = AMotionEvent_getX(event, i);
            if (source == SOURCE_TOUCHPAD)
                tempY[i] = PAD_HEIGHT - AMotionEvent_getY(event, i);
            else
                tempY[i] = AMotionEvent_getY(event, i);
        }

        (*g_pEnv)->ReleaseIntArrayElements(g_pEnv, pointerIds, tempIds, 0);
        (*g_pEnv)->ReleaseFloatArrayElements(g_pEnv, pointerX, tempX, 0);
        (*g_pEnv)->ReleaseFloatArrayElements(g_pEnv, pointerY, tempY, 0);

        int handled = 0;
        if (g_pEnv && g_pActivity)
            handled = (*g_pEnv)->CallIntMethod(g_pEnv, g_pActivity, javaOnNativeTouch, source, action, pointerCount, pointerIds, pointerX, pointerY);

        (*g_pEnv)->DeleteLocalRef(g_pEnv, pointerIds);
        (*g_pEnv)->DeleteLocalRef(g_pEnv, pointerX);
        (*g_pEnv)->DeleteLocalRef(g_pEnv, pointerY);

        return handled;
    }
    return 0;
}

/**
 * Process the next application command.
 */
static void onAppCmd(struct android_app* app, int32_t cmd)
{
    switch (cmd)
    {
    case APP_CMD_RESUME:
        if (g_pVM)
            (*g_pVM)->AttachCurrentThread(g_pVM, &g_pEnv, 0);
        break;
    case APP_CMD_PAUSE:
        if (g_pVM)
            (*g_pVM)->DetachCurrentThread(g_pVM);
        break;
    }
}

/**
 * This is the main entry point of a native application that is using
 * android_native_app_glue.  It runs in its own thread, with its own
 * event loop for receiving input events and application messages.
 */
void android_main(struct android_app* state)
{
    // Make sure glue isn't stripped
    app_dummy();

    // Register callback functions
    state->onAppCmd = onAppCmd;
    state->onInputEvent = onInputEvent;

    // Main loop
    while (1)
    {
        // Read all pending events
        int ident;
        int events;
        struct android_poll_source* source;

        while ((ident = ALooper_pollAll(250, NULL, &events, (void**) &source)) >= 0)
        {
            // Notify glue code to call onInputEvent
            if (source != NULL)
            {
                source->process(state, source);
            }

            // Exit main loop if necessary
            if (state->destroyRequested != 0)
            {
                return;
            }
        }
    }
}
