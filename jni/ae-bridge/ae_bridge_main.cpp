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

#include "SDL_main.h"
#include "ae_bridge.h"

/*******************************************************************************
 Functions called automatically by JNI framework
 *******************************************************************************/

// Library init
extern DECLSPEC jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    return JNI_VERSION_1_4;
}

/*******************************************************************************
 Functions called by Java code
 *******************************************************************************/

// Start up the SDL app
extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_CoreInterfaceNative_sdlInit(JNIEnv* env, jclass cls, jobjectArray jargv)
{
    /* This interface could expand with ABI negotiation, calbacks, etc. */
    SDL_Android_Init(env, cls);
    SDL_Android_Init_Extras(env, cls);
    SDL_SetMainReady();

    /* Run the application code! */
    int status;

    /* Simple usage: (i.e. to play Mario 64..)
        char *argv[3];
        argv[0] = strdup("mupen64plus");
        argv[1] = strdup( "roms/mario.n64" );
        argv[2] = NULL;
        status = SDL_main( 2, argv );
    */

    // Allocate enough char pointers to index all the args:
    int argc = env->GetArrayLength(jargv);
    char **argv = (char **) malloc(sizeof(char *) * argc);
    for(int i = 0; i < argc; i++)
    {
        jstring jarg = (jstring) env->GetObjectArrayElement(jargv, i);
        const char *arg = env->GetStringUTFChars(jarg, 0);
        argv[i] = strdup(arg);
        env->ReleaseStringUTFChars(jarg, arg);
    }

    status = SDL_main( argc, argv );  // Launch the emulator

    /* We exit here for consistency with other platforms. */
    exit( status );  // <---- TODO: This is the ASDP bug culprit
}
