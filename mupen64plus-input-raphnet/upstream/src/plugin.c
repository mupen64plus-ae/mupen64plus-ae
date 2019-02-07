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

#include <string.h>
#include <stdio.h>
#include <jni.h>
#include <android/log.h>

#include "m64p_plugin.h"

// Internal macros
#define PLUGIN_NAME                 "Mupen64Plus Raphnet Input Plugin"
#define PLUGIN_VERSION              0x010000
#define INPUT_PLUGIN_API_VERSION    0x020000


static int _pluginInitialized = 0;

// Internal variables
static JavaVM* _javaVM;
static jclass _jniClass = NULL;

// Function declarations
static void DebugMessage(int level, const char *message, ...);

/*******************************************************************************
 Functions called automatically by JNI framework
 *******************************************************************************/

extern jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    _javaVM = vm;
    return JNI_VERSION_1_6;
}

//*****************************************************************************
// JNI exported function definitions
//*****************************************************************************

JNIEXPORT void JNICALL Java_paulscode_android_mupen64plusae_jni_RaphnetControllerHandler_init(JNIEnv* env, jclass cls, jint usbFileDescriptor)
{
    DebugMessage(M64MSG_INFO, "init()");

    _jniClass = (jclass)(*env)->NewGlobalRef(env, cls);
}

//*****************************************************************************
// Mupen64Plus debug function definitions
//*****************************************************************************

static void DebugMessage(int level, const char* message, ...)
{
    char msgbuf[1024];
    va_list args;
    va_start(args, message);
    vsprintf(msgbuf, message, args);
    va_end(args);

    switch (level)
    {
    case M64MSG_ERROR:
        __android_log_print(ANDROID_LOG_ERROR, "input-raphnet", "%s", msgbuf);
        break;
    case M64MSG_WARNING:
        __android_log_print(ANDROID_LOG_WARN, "input-raphnet", "%s", msgbuf);
        break;
    case M64MSG_INFO:
        __android_log_print(ANDROID_LOG_INFO, "input-raphnet", "%s", msgbuf);
        break;
    case M64MSG_STATUS:
        __android_log_print(ANDROID_LOG_DEBUG, "input-raphnet", "%s", msgbuf);
        break;
    case M64MSG_VERBOSE:
    default:
        //__android_log_print( ANDROID_LOG_VERBOSE, "input-android", "%s", msgbuf );
        break;
    }
}

//*****************************************************************************
// Mupen64Plus common plugin function definitions
//*****************************************************************************

EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type* pluginType, int* pluginVersion, int* apiVersion, const char** pluginNamePtr, int* capabilities)
{
    if (pluginType != NULL)
        *pluginType = M64PLUGIN_INPUT;

    if (pluginVersion != NULL)
        *pluginVersion = PLUGIN_VERSION;

    if (apiVersion != NULL)
        *apiVersion = INPUT_PLUGIN_API_VERSION;

    if (pluginNamePtr != NULL)
        *pluginNamePtr = PLUGIN_NAME;

    if (capabilities != NULL)
        *capabilities = 0;

    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle coreLibHandle, void* context, void (*DebugCallback)(void*, int, const char*))
{
    if (_pluginInitialized)
        return M64ERR_ALREADY_INIT;

    _pluginInitialized = 1;
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginShutdown()
{
    if (!_pluginInitialized)
        return M64ERR_NOT_INIT;

    _pluginInitialized = 0;
    return M64ERR_SUCCESS;
}

//*****************************************************************************
// Mupen64Plus input plugin function definitions
//*****************************************************************************

EXPORT void CALL InitiateControllers(CONTROL_INFO controlInfo)
{

}

EXPORT void CALL GetKeys(int controllerNum, BUTTONS* keys)
{

}

EXPORT void CALL ControllerCommand(int controllerNum, unsigned char* command)
{

}

EXPORT void CALL ReadController(int control, unsigned char* command)
{
}

EXPORT void CALL RomClosed()
{
}

EXPORT int CALL RomOpen()
{
    return 1;
}

EXPORT void CALL SDL_KeyDown(int keymod, int keysym)
{
}

EXPORT void CALL SDL_KeyUp(int keymod, int keysym)
{
}
