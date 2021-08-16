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
 * Authors: littleguy77, Paul Lamb, fzurita
 */

#include <cstring>
#include <cstdio>
#include <cmath>
#include <jni.h>
#include <android/log.h>
#include <algorithm>

#include "m64p_plugin.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma ide diagnostic ignored "UnusedLocalVariable"
#pragma ide diagnostic ignored "OCUnusedMacroInspection"
#ifndef DECLSPEC
# if defined(__BEOS__) || defined(__HAIKU__)
#  if defined(__GNUC__)
#   define DECLSPEC __declspec(dllexport)
#  else
#   define DECLSPEC __declspec(export)
#  endif
# elif defined(__WIN32__)
#  ifdef __BORLANDC__
#   ifdef BUILD_SDL
#    define DECLSPEC
#   else
#    define DECLSPEC    __declspec(dllimport)
#   endif
#  else
#   define DECLSPEC __declspec(dllexport)
#  endif
# else
#  if defined(__GNUC__) && __GNUC__ >= 4
#   define DECLSPEC __attribute__ ((visibility("default")))
#  else
#   define DECLSPEC
#  endif
# endif
#endif

// Internal macros
#define PLUGIN_NAME                 "Mupen64Plus Android Input Plugin"
#define PLUGIN_VERSION              0x010000
#define INPUT_PLUGIN_API_VERSION    0x020100
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
JavaVM* mJavaVM;
static jclass mActivityClass;
static jmethodID jniRumble = nullptr;

static int androidPluggedState[4];
static int androidPakType[4];
static unsigned char androidButtonState[4][16];
static double androidAnalogX[4];
static double androidAnalogY[4];
static bool isAnalogDigital[4];
static int pluginInitialized = 0;
static CONTROL* controllerInfos = nullptr;

// Function declarations
static void DebugMessage(int level, const char *message, ...);

/*******************************************************************************
 Functions called internally
 *******************************************************************************/

static unsigned char DataCRC(const unsigned char* data, int length)
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
    mJavaVM = vm;
    return JNI_VERSION_1_6;
}

//*****************************************************************************
// JNI exported function definitions
//*****************************************************************************

extern "C" JNIEXPORT void Java_paulscode_android_mupen64plusae_jni_NativeInput_init(JNIEnv* env, jclass cls)
{
    DebugMessage(M64MSG_INFO, "init()");

    // Discard stale pointer
    controllerInfos = nullptr;

    mActivityClass = (jclass) env->NewGlobalRef(cls);
    jniRumble = env->GetStaticMethodID(cls, "rumble", "(IZ)V");

    if (!jniRumble)
    {
        DebugMessage(M64MSG_WARNING, "Couldn't locate Java callbacks, check that they're named and typed correctly");
    }
}

extern "C" JNIEXPORT void Java_paulscode_android_mupen64plusae_jni_NativeInput_setConfig(JNIEnv* env, jclass jcls, jint controllerNum, jboolean plugged,
        jint pakType)
{
    if (controllerNum < 4 && controllerNum > -1)
    {
        // Cache the values if called before InitiateControllers
        androidPluggedState[controllerNum] = (plugged == JNI_TRUE ? 1 : 0);
        androidPakType[controllerNum] = (int) pakType;

        // Update the values if called after InitiateControllers
        if (controllerInfos != nullptr)
        {
            controllerInfos[controllerNum].Present = androidPluggedState[controllerNum];
            controllerInfos[controllerNum].Plugin = androidPakType[controllerNum];
        }
    }
}

extern "C" JNIEXPORT void Java_paulscode_android_mupen64plusae_jni_NativeInput_setState(JNIEnv* env, jclass jcls, jint controllerNum, jbooleanArray mp64pButtons,
        jdouble mp64pXAxis, jdouble mp64pYAxis, jboolean isDigital)
{
    jboolean* elements = env->GetBooleanArrayElements(mp64pButtons, nullptr);
    int b;
    for (b = 0; b < 16; b++)
    {
        androidButtonState[controllerNum][b] = elements[b];
    }
    env->ReleaseBooleanArrayElements(mp64pButtons, elements, 0);

    androidAnalogX[controllerNum] = mp64pXAxis;
    androidAnalogY[controllerNum] = mp64pYAxis;
    isAnalogDigital[controllerNum] = isDigital;
}

//*****************************************************************************
// JNI imported function definitions
//*****************************************************************************

extern "C" JNIEXPORT void JNI_Rumble(int controllerNum, int active)
{
    JNIEnv *env;
    if (mJavaVM->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK)
        return;

    jboolean a = active == 0 ? JNI_FALSE : JNI_TRUE;
    env->CallStaticVoidMethod(mActivityClass, jniRumble, controllerNum, a);
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

extern "C" EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type* pluginType, int* pluginVersion, int* apiVersion, const char** pluginNamePtr, int* capabilities)
{
    if (pluginType != nullptr)
        *pluginType = M64PLUGIN_INPUT;

    if (pluginVersion != nullptr)
        *pluginVersion = PLUGIN_VERSION;

    if (apiVersion != nullptr)
        *apiVersion = INPUT_PLUGIN_API_VERSION;

    if (pluginNamePtr != nullptr)
        *pluginNamePtr = PLUGIN_NAME;

    if (capabilities != nullptr)
        *capabilities = 0;

    return M64ERR_SUCCESS;
}

extern "C" EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle coreLibHandle, void* context, void (*DebugCallback)(void*, int, const char*))
{
    if (pluginInitialized)
        return M64ERR_ALREADY_INIT;

    pluginInitialized = 1;
    return M64ERR_SUCCESS;
}

extern "C" EXPORT m64p_error CALL PluginShutdown()
{
    if (!pluginInitialized)
        return M64ERR_NOT_INIT;

    pluginInitialized = 0;
    return M64ERR_SUCCESS;
}

//*****************************************************************************
// Mupen64Plus input plugin function definitions
//*****************************************************************************

extern "C" EXPORT void CALL InitiateControllers(CONTROL_INFO controlInfo)
{
    controllerInfos = controlInfo.Controls;

    int i;
    for (i = 0; i < 4; i++)
    {
        // Configure each controller
        controllerInfos[i].Present = androidPluggedState[i];
        controllerInfos[i].Plugin = androidPakType[i];
        controllerInfos[i].RawData = 0;
    }
}

// Credit: MerryMage
void simulateOctagon(double inputX, double inputY, int& outputX, int& outputY)
{
    //scale to {-84 ... +84}
    double ax = inputX * 85.0;
    double ay = inputY * 85.0;

    double len = std::sqrt(ax*ax+ay*ay);
    if (len < 16.0) {
        len = 0;
    } else if (len > 85.0) {
        len = 85.0 / len;
    } else {
        len = (len - 16.0) * 85.0 / (85.0 - 16.0) / len;
    }
    ax *= len;
    ay *= len;

    //bound diagonals to an octagonal range {-68 ... +68}
    if(ax != 0.0 && ay != 0.0) {
        double slope = ay / ax;
        double edgex = copysign(85.0 / (std::abs(slope) + 16.0 / 69.0), ax);
        double edgey = copysign(std::min(std::abs(edgex * slope), 85.0 / (1.0 / std::abs(slope) + 16.0 / 69.0)), ay);
        edgex = edgey / slope;

        double scale = std::sqrt(edgex*edgex+edgey*edgey) / 85.0;
        ax *= scale;
        ay *= scale;
    }

    outputX = static_cast<int>(ax);
    outputY = static_cast<int>(ay);
}

extern "C" EXPORT void CALL GetKeys(int controllerNum, BUTTONS* keys)
{
    // Reset the controller state
    keys->Value = 0;

    // Set the button bits
    int b;
    for (b = 0; b < 16; b++)
    {
        if (androidButtonState[controllerNum][b])
            keys->Value |= BUTTON_BITS[b];
    }

    double inputX;
    double inputY;
    int outputX;
    int outputY;

    // Limit the speed of the analog stick under certain circumstances
    if (isAnalogDigital[controllerNum]) {
        static double actualXAxis[4] = {0};
        static double actualYAxis[4] = {0};
        static const double maxChange = 0.375;
        static const double distanceForInstantChange = 1.4375;

        double distance = sqrt(pow(actualXAxis[controllerNum] - androidAnalogX[controllerNum],2) +
                               pow(actualYAxis[controllerNum] - androidAnalogY[controllerNum],2));
        bool instantChange = distance > distanceForInstantChange || distance < maxChange ||
                             (androidAnalogX[controllerNum] == 0 && androidAnalogY[controllerNum] == 0);

        double xDiff = androidAnalogX[controllerNum] - actualXAxis[controllerNum];
        double yDiff = androidAnalogY[controllerNum] - actualYAxis[controllerNum];

        actualXAxis[controllerNum] = instantChange ? androidAnalogX[controllerNum] :
                                     actualXAxis[controllerNum] + copysign(1.0, xDiff)*std::min(static_cast<double>(maxChange), abs(xDiff));
        actualYAxis[controllerNum] = instantChange ? androidAnalogY[controllerNum] :
                                     actualYAxis[controllerNum] + copysign(1.0, yDiff)*std::min(static_cast<double>(maxChange), abs(yDiff));

        inputX = actualXAxis[controllerNum];
        inputY = actualYAxis[controllerNum];
    } else {
        inputX = androidAnalogX[controllerNum];
        inputY = androidAnalogY[controllerNum];
    }

    simulateOctagon(inputX, inputY, outputX, outputY);
    keys->X_AXIS = outputX;
    keys->Y_AXIS = outputY;
}

extern "C" EXPORT void CALL ControllerCommand(int controllerNum, unsigned char* command)
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

extern "C" EXPORT void CALL ReadController(int control, unsigned char* command)
{
}

extern "C" EXPORT void CALL RomClosed()
{
}

extern "C" EXPORT int CALL RomOpen()
{
    return 1;
}

extern "C" EXPORT void CALL SDL_KeyDown(int keymod, int keysym)
{
}

extern "C" EXPORT void CALL SDL_KeyUp(int keymod, int keysym)
{
}

extern "C" EXPORT void CALL RenderCallback(void)
{

}

#pragma clang diagnostic pop