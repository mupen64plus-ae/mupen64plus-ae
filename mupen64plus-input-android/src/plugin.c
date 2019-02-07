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
#define PLUGIN_NAME                 "Mupen64Plus Android Input Plugin"
#define PLUGIN_VERSION              0x010000
#define INPUT_PLUGIN_API_VERSION    0x020000
#define PAK_IO_RUMBLE       		0xC000	// the address where rumble commands are sent
#define PAK_TYPE_NONE               1;
#define PAK_TYPE_MEM                2;
#define PAK_TYPE_RUMBLE             5;

// ControllerCommand commands
#define RD_GETSTATUS        		0x00   	// get status
#define RD_READKEYS         		0x01  	// read button values
#define RD_READPAK         			0x02   	// read from controllerpack
#define RD_WRITEPAK         		0x03   	// write to controllerpack
#define RD_READEEPROM       		0x04  	// read eeprom
#define RD_WRITEEPROM       		0x05   	// write eeprom
#define RD_RESETCONTROLLER  		0xff   	// reset controller

// Internal constants
static const unsigned short BUTTON_BITS[] =
{
        0x0001,  // R_DPAD
        0x0002,  // L_DPAD
        0x0004,  // D_DPAD
        0x0008,  // U_DPAD
        0x0010,  // START_BUTTON
        0x0020,  // Z_TRIG
        0x0040,  // B_BUTTON
        0x0080,  // A_BUTTON
        0x0100,  // R_CBUTTON
        0x0200,  // L_CBUTTON
        0x0400,  // D_CBUTTON
        0x0800,  // U_CBUTTON
        0x1000,  // R_TRIG
        0x2000,  // L_TRIG
        0x4000,  // Reserved1
        0x8000   // Reserved2
};

// Internal variables
static JavaVM* _javaVM;
static jclass _jniClass = NULL;
static jmethodID _jniRumble = NULL;
static int _androidPluggedState[4];
static int _androidPakType[4];
static unsigned char _androidButtonState[4][16];
static signed char _androidAnalogX[4];
static signed char _androidAnalogY[4];
static int _pluginInitialized = 0;
static CONTROL* _controllerInfos = NULL;

// Function declarations
static void DebugMessage(int level, const char *message, ...);

/*******************************************************************************
 Functions called internally
 *******************************************************************************/

static unsigned char DataCRC(unsigned char* data, int length)
{
    unsigned char remainder = data[0];

    int iByte = 1;
    unsigned char bBit = 0;

    while (iByte <= length)
    {
        int highBit = ((remainder & 0x80) != 0);
        remainder = remainder << 1;

        remainder += (iByte < length && (data[iByte] & (0x80 >> bBit))) ? 1 : 0;

        remainder ^= (highBit) ? 0x85 : 0;

        bBit++;
        iByte += bBit / 8;
        bBit %= 8;
    }

    return remainder;
}

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

JNIEXPORT void JNICALL Java_paulscode_android_mupen64plusae_jni_NativeInput_init(JNIEnv* env, jclass cls)
{
    DebugMessage(M64MSG_INFO, "init()");

    // Discard stale pointer
    _controllerInfos = NULL;

    _jniClass = (jclass)(*env)->NewGlobalRef(env, cls);

    _jniRumble = (*env)->GetStaticMethodID(env, cls, "rumble", "(IZ)V");
    if (!_jniRumble)
    {
        DebugMessage(M64MSG_WARNING, "Couldn't locate Java callbacks, check that they're named and typed correctly");
    }
}

JNIEXPORT void JNICALL Java_paulscode_android_mupen64plusae_jni_NativeInput_setConfig(JNIEnv* env, jclass jcls, jint controllerNum, jboolean plugged,
        jint pakType)
{
    if (controllerNum < 4 && controllerNum > -1)
    {
        // Cache the values if called before InitiateControllers
        _androidPluggedState[controllerNum] = (plugged == JNI_TRUE ? 1 : 0);
        _androidPakType[controllerNum] = (int) pakType;

        // Update the values if called after InitiateControllers
        if (_controllerInfos != NULL)
        {
            _controllerInfos[controllerNum].Present = _androidPluggedState[controllerNum];
            _controllerInfos[controllerNum].Plugin = _androidPakType[controllerNum];
        }
    }
}

JNIEXPORT void JNICALL Java_paulscode_android_mupen64plusae_jni_NativeInput_setState(JNIEnv* env, jclass jcls, jint controllerNum, jbooleanArray mp64pButtons,
        jint mp64pXAxis, jint mp64pYAxis)
{
    jboolean* elements = (*env)->GetBooleanArrayElements(env, mp64pButtons, NULL);
    int b;
    for (b = 0; b < 16; b++)
    {
        _androidButtonState[controllerNum][b] = elements[b];
    }
    (*env)->ReleaseBooleanArrayElements(env, mp64pButtons, elements, 0);

    _androidAnalogX[controllerNum] = (signed char) ((int) mp64pXAxis);
    _androidAnalogY[controllerNum] = (signed char) ((int) mp64pYAxis);
}

//*****************************************************************************
// JNI imported function definitions
//*****************************************************************************

JNIEXPORT void JNICALL JNI_Rumble(int controllerNum, int active)
{
    JNIEnv *env;
    if ((*_javaVM)->GetEnv(_javaVM, (void**) &env, JNI_VERSION_1_6) != JNI_OK)
        return;
    jboolean a = active == 0 ? JNI_FALSE : JNI_TRUE;
    (*env)->CallStaticVoidMethod(env, _jniClass, _jniRumble, controllerNum, a);
}

//*****************************************************************************
// Mupen64Plus debug function definitions
//*****************************************************************************

static void DebugMessage(int level, const char* message, ...)
{
    // TODO: Obtain this implementation from an extern
    char msgbuf[1024];
    va_list args;
    va_start(args, message);
    vsprintf(msgbuf, message, args);
    va_end(args);

    switch (level)
    {
    case M64MSG_ERROR:
        __android_log_print(ANDROID_LOG_ERROR, "input-android", "%s", msgbuf);
        break;
    case M64MSG_WARNING:
        __android_log_print(ANDROID_LOG_WARN, "input-android", "%s", msgbuf);
        break;
    case M64MSG_INFO:
        __android_log_print(ANDROID_LOG_INFO, "input-android", "%s", msgbuf);
        break;
    case M64MSG_STATUS:
        __android_log_print(ANDROID_LOG_DEBUG, "input-android", "%s", msgbuf);
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
    _controllerInfos = controlInfo.Controls;

    int i;
    for (i = 0; i < 4; i++)
    {
        // Configure each controller
        _controllerInfos[i].Present = _androidPluggedState[i];
        _controllerInfos[i].Plugin = _androidPakType[i];
        _controllerInfos[i].RawData = 0;
    }
}

EXPORT void CALL GetKeys(int controllerNum, BUTTONS* keys)
{
    // Reset the controller state
    keys->Value = 0;

    // Set the button bits
    int b;
    for (b = 0; b < 16; b++)
    {
        if (_androidButtonState[controllerNum][b])
            keys->Value |= BUTTON_BITS[b];
    }

    // Set the analog bytes
    keys->X_AXIS = _androidAnalogX[controllerNum];
    keys->Y_AXIS = _androidAnalogY[controllerNum];
}

EXPORT void CALL ControllerCommand(int controllerNum, unsigned char* command)
{
    if (controllerNum < 0)
        return;

    unsigned char* data = command + 5;
    unsigned int dwAddress = (command[3] << 8) + (command[4] & 0xE0);
    switch (command[2])
    {
    case RD_READPAK:
        if ((dwAddress >= 0x8000) && (dwAddress < 0x9000))
        {
            memset(data, 0x80, 32);
        }
        else
        {
            memset(data, 0x00, 32);
        }
        data[32] = DataCRC(data, 32);
        break;

    case RD_WRITEPAK:
        if (dwAddress == PAK_IO_RUMBLE)
        {
            JNI_Rumble(controllerNum, data[0]);
        }
        data[32] = DataCRC(data, 32);
        break;
    }
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
