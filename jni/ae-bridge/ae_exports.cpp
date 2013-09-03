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
#include "ae_bridge.h"

#define M64P_CORE_PROTOTYPES
#include "m64p_frontend.h"

#ifdef M64P_BIG_ENDIAN
  #define sl(mot) mot
#else
  #define sl(mot) (((mot & 0xFF) << 24) | ((mot & 0xFF00) <<  8) | ((mot & 0xFF0000) >>  8) | ((mot & 0xFF000000) >> 24))
#endif

/*******************************************************************************
 Functions called internally
 *******************************************************************************/

static char strBuff[1024];

static void swap_rom(unsigned char* localrom, int loadlength)
{
    unsigned char temp;
    int i;

    /* Btyeswap if .v64 image. */
    if (localrom[0] == 0x37)
    {
        for (i = 0; i < loadlength; i += 2)
        {
            temp = localrom[i];
            localrom[i] = localrom[i + 1];
            localrom[i + 1] = temp;
        }
    }
    /* Wordswap if .n64 image. */
    else if (localrom[0] == 0x40)
    {
        for (i = 0; i < loadlength; i += 4)
        {
            temp = localrom[i];
            localrom[i] = localrom[i + 3];
            localrom[i + 3] = temp;
            temp = localrom[i + 1];
            localrom[i + 1] = localrom[i + 2];
            localrom[i + 2] = temp;
        }
    }
}

static char * trim(char *str)
{
    unsigned int i;
    char *p = str;

    while (isspace(*p))
        p++;

    if (str != p)
    {
        for (i = 0; i <= strlen(p); ++i)
            str[i] = p[i];
    }

    p = str + strlen(str) - 1;
    if (p > str)
    {
        while (isspace(*p))
            p--;
        p[1] = '\0';
    }

    return str;
}

/*******************************************************************************
 Functions called automatically by JNI framework
 *******************************************************************************/

// Library init
extern jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    return JNI_VERSION_1_4;
}

/*******************************************************************************
 Functions called by Java code
 *******************************************************************************/

extern "C" DECLSPEC void SDLCALL Java_org_libsdl_app_SDLActivity_onNativeResize(JNIEnv* env, jclass jcls, jint width, jint height, jint format);
extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_CoreInterfaceNative_sdlOnResize(JNIEnv* env, jclass jcls, jint width, jint height, jint format)
{
    // Simple wrapper so that we don't have to touch the original SDL code
    Java_org_libsdl_app_SDLActivity_onNativeResize(env, jcls, width, height, format);
}

extern "C" DECLSPEC void SDLCALL Java_org_libsdl_app_SDLActivity_nativeQuit(JNIEnv* env, jclass cls);
extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_CoreInterfaceNative_sdlQuit(JNIEnv* env, jclass cls)
{
    // Simple wrapper so that we don't have to touch the original SDL code
    Java_org_libsdl_app_SDLActivity_nativeQuit(env, cls);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuGameShark(JNIEnv* env, jclass cls, jboolean pressed)
{
    int p = pressed == JNI_TRUE ? 1 : 0;
    (*CoreDoCommand)(M64CMD_CORE_STATE_SET, M64CORE_INPUT_GAMESHARK, &p);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuPause(JNIEnv* env, jclass cls)
{
    (*CoreDoCommand)(M64CMD_PAUSE, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuResume(JNIEnv* env, jclass cls)
{
    (*CoreDoCommand)(M64CMD_RESUME, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuStop(JNIEnv* env, jclass cls)
{
    (*CoreDoCommand)(M64CMD_STOP, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuAdvanceFrame(JNIEnv* env, jclass cls)
{
    (*CoreDoCommand)(M64CMD_ADVANCE_FRAME, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuSetSpeed(JNIEnv* env, jclass cls, jint percent)
{
    int speed_factor = (int) percent;
    (*CoreDoCommand)(M64CMD_CORE_STATE_SET, M64CORE_SPEED_FACTOR, &speed_factor);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuSetSlot(JNIEnv* env, jclass cls, jint slotID)
{
    (*CoreDoCommand)(M64CMD_STATE_SET_SLOT, (int) slotID, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuLoadSlot(JNIEnv* env, jclass cls)
{
    (*CoreDoCommand)(M64CMD_STATE_LOAD, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuSaveSlot(JNIEnv* env, jclass cls)
{
    (*CoreDoCommand)(M64CMD_STATE_SAVE, 1, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuLoadFile(JNIEnv* env, jclass cls, jstring filename)
{
    const char *nativeString = env->GetStringUTFChars(filename, 0);
    (*CoreDoCommand)(M64CMD_STATE_LOAD, 0, (void *) nativeString);
    env->ReleaseStringUTFChars(filename, nativeString);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuSaveFile(JNIEnv* env, jclass cls, jstring filename)
{
    const char *nativeString = env->GetStringUTFChars(filename, 0);
    (*CoreDoCommand)(M64CMD_STATE_SAVE, 1, (void *) nativeString);
    env->ReleaseStringUTFChars(filename, nativeString);
}

extern "C" DECLSPEC jint Java_paulscode_android_mupen64plusae_CoreInterfaceNative_emuGetState(JNIEnv* env, jclass cls)
{
    int state = 0;
    (*CoreDoCommand)(M64CMD_CORE_STATE_QUERY, M64CORE_EMU_STATE, &state);
    if (state == M64EMU_STOPPED)
        return (jint) 1;
    else if (state == M64EMU_RUNNING)
        return (jint) 2;
    else if (state == M64EMU_PAUSED)
        return (jint) 3;
    else
        return (jint) 0;
}
