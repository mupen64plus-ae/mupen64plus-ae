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

#include <stdlib.h>
#include <dlfcn.h>
#include "SDL.h"
#include "m64p_types.h"
#include "ae_imports.h"

#ifdef M64P_BIG_ENDIAN
  #define sl(mot) mot
#else
  #define sl(mot) (((mot & 0xFF) << 24) | ((mot & 0xFF00) <<  8) | ((mot & 0xFF0000) >>  8) | ((mot & 0xFF000000) >> 24))
#endif


/*******************************************************************************
 Variables and functions used internally
 *******************************************************************************/

// JNI objects
static JavaVM* mVm;
static void* mReserved;

// Library handles
static void *handleAEI;         // libae-imports.so
static void *handleSDL;         // libSDL2.so
static void *handleCore;        // libmupen64plus-core.so
static void *handleFront;       // libmupen64plus-ui-console.so

// Function types
typedef jint        (*pJNI_OnLoad)      (JavaVM* vm, void* reserved);
typedef void        (*pJNI_OnUnload)    (JavaVM *vm, void *reserved);
typedef int         (*pAeiInit)         (JNIEnv* env, jclass cls);
typedef int         (*pSdlInit)         (JNIEnv* env, jclass cls);
typedef void        (*pSdlSetScreen)    (int width, int height, Uint32 format);
typedef void        (*pVoidFunc)        ();
typedef m64p_error  (*pCoreDoCommand)   (m64p_command, int, void *);
typedef int         (*pFrontMain)       (int argc, char* argv[]);

// Function pointers
static pAeiInit         aeiInit         = NULL;
static pSdlInit         sdlInit         = NULL;
static pSdlSetScreen    sdlSetScreen    = NULL;
static pVoidFunc        sdlMainReady    = NULL;
static pCoreDoCommand   coreDoCommand   = NULL;
static pFrontMain       frontMain       = NULL;

void checkLibraryError(const char* message)
{
    const char* error = dlerror();
    if (error)
        LOGE("%s: %s", message, error);
}

void* loadLibrary(const char* libPath, const char* libName)
{
    char path[256];
    sprintf(path, "%s/lib%s.so", libPath, libName);
    void* handle = dlopen(path, RTLD_NOW);
    if (!handle)
        LOGE("Failed to load lib%s.so", libName);
    checkLibraryError(libName);
    return handle;
}

int unloadLibrary(void* handle, const char* libName)
{
    if (!handle)
        return 0;

    int code = dlclose(handle);
    if (code)
        LOGE("Failed to unload lib%s.so", libName);
    checkLibraryError(libName);
    return code;
}

void* locateFunction(void* handle, const char* libName, const char* funcName)
{
    char message[256];
    sprintf(message, "Locating %s::%s", libName, funcName);
    void* func = dlsym(handle, funcName);
    if (!func)
        LOGE("Failed to locate %s::%s", libName, funcName);
    checkLibraryError(message);
    return func;
}

/*******************************************************************************
 Functions called automatically by JNI framework
 *******************************************************************************/

extern jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    mVm = vm;
    mReserved = reserved;
    return JNI_VERSION_1_4;
}

/*******************************************************************************
 Functions called by Java code
 *******************************************************************************/

extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_jni_NativeExports_loadLibraries(JNIEnv* env, jclass cls, jstring jlibPath)
{
    LOGI("Loading native libraries");

    // Clear stale error messages
    dlerror();

    // Get the library path from the java-managed string
    const char *libPath = env->GetStringUTFChars(jlibPath, 0);
    char path[256];
    strcpy(path, libPath);
    env->ReleaseStringUTFChars(jlibPath, libPath);

    // Open shared libraries
    handleAEI   = loadLibrary(path, "ae-imports");
    handleSDL   = loadLibrary(path, "SDL2");
    handleCore  = loadLibrary(path, "mupen64plus-core");
    handleFront = loadLibrary(path, "mupen64plus-ui-console");

    // Make sure we don't have any typos
    if (!handleAEI || !handleSDL || !handleCore || !handleFront)
    {
        LOGE("Could not load libraries: be sure the paths are correct");
    }

    // Find and call the JNI_OnLoad functions manually since we aren't loading the libraries from Java
    pJNI_OnLoad JNI_OnLoad0 = (pJNI_OnLoad) locateFunction(handleAEI, "ae-imports", "JNI_OnLoad");
    pJNI_OnLoad JNI_OnLoad1 = (pJNI_OnLoad) locateFunction(handleSDL, "SDL2",       "JNI_OnLoad");
    JNI_OnLoad0(mVm, mReserved);
    JNI_OnLoad1(mVm, mReserved);
    JNI_OnLoad0 = NULL;
    JNI_OnLoad1 = NULL;

    // Find library functions
    aeiInit       = (pAeiInit)       locateFunction(handleAEI,   "ae-imports",             "Android_JNI_InitImports");
    sdlInit       = (pSdlInit)       locateFunction(handleSDL,   "SDL2",                   "SDL_Android_Init");
    sdlSetScreen  = (pSdlSetScreen)  locateFunction(handleSDL,   "SDL2",                   "Android_SetScreenResolution");
    sdlMainReady  = (pVoidFunc)      locateFunction(handleSDL,   "SDL2",                   "SDL_SetMainReady");
    coreDoCommand = (pCoreDoCommand) locateFunction(handleCore,  "mupen64plus-core",       "CoreDoCommand");
    frontMain     = (pFrontMain)     locateFunction(handleFront, "mupen64plus-ui-console", "SDL_main");

    // Make sure we don't have any typos
    if (!aeiInit || !sdlInit || !sdlSetScreen || !sdlMainReady || !coreDoCommand || !frontMain)
    {
        LOGE("Could not load library functions: be sure they are named and typedef'd correctly");
    }
}

extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_jni_NativeExports_unloadLibraries(JNIEnv* env, jclass cls)
{
    // Unload the libraries to ensure that static variables are re-initialized next time
    LOGI("Unloading native libraries");

    // Clear stale error messages
    dlerror();

    // Find and call the JNI_OnUnLoad functions from the SDL2 library
    pJNI_OnUnload JNI_OnUnLoad = (pJNI_OnUnload) locateFunction(handleSDL, "SDL2",       "JNI_OnUnload");
    JNI_OnUnLoad(mVm, mReserved);
    JNI_OnUnLoad = NULL;

    // Nullify function pointers so that they can no longer be used
    aeiInit         = NULL;
    sdlInit         = NULL;
    sdlSetScreen    = NULL;
    sdlMainReady    = NULL;
    coreDoCommand   = NULL;
    frontMain       = NULL;

    // Close shared libraries
    unloadLibrary(handleFront, "mupen64plus-ui-console");
    unloadLibrary(handleCore,  "mupen64plus-core");
    unloadLibrary(handleSDL,   "SDL2");
    unloadLibrary(handleAEI,   "ae-imports");

    // Nullify handles so that they can no longer be used
    handleFront     = NULL;
    handleCore      = NULL;
    handleSDL       = NULL;
    handleAEI       = NULL;
}

extern "C" DECLSPEC jint SDLCALL Java_paulscode_android_mupen64plusae_jni_NativeExports_emuStart(JNIEnv* env, jclass cls, jstring juserDataPath, jstring juserCachePath, jobjectArray jargv)
{
    // Define some environment variables needed by rice video plugin
    const char *userDataPath = env->GetStringUTFChars(juserDataPath, 0);
    const char *userCachePath = env->GetStringUTFChars(juserCachePath, 0);
    setenv( "XDG_DATA_HOME", userDataPath, 1 );
    setenv( "XDG_CACHE_HOME", userCachePath, 1 );
    env->ReleaseStringUTFChars(juserDataPath, userDataPath);
    env->ReleaseStringUTFChars(juserCachePath, userCachePath);

    // Initialize dependencies
    jclass nativeImports = env->FindClass("paulscode/android/mupen64plusae/jni/NativeImports");
    jclass nativeSDL = env->FindClass("paulscode/android/mupen64plusae/jni/NativeSDL");
    aeiInit(env, nativeImports);
    sdlInit(env, nativeSDL);
    sdlSetScreen(0, 0, SDL_PIXELFORMAT_RGB565);
    sdlMainReady();

    // Repackage the command-line args
    int argc = env->GetArrayLength(jargv);
    char **argv = (char **) malloc(sizeof(char *) * argc);
    for (int i = 0; i < argc; i++)
    {
        jstring jarg = (jstring) env->GetObjectArrayElement(jargv, i);
        const char *arg = env->GetStringUTFChars(jarg, 0);
        argv[i] = strdup(arg);
        env->ReleaseStringUTFChars(jarg, arg);
    }

    // Launch main emulator loop (continues until emuStop is called)
    return frontMain(argc, argv);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuStop(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_STOP, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuResume(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_RESUME, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuPause(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_PAUSE, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuAdvanceFrame(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_ADVANCE_FRAME, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSetSpeed(JNIEnv* env, jclass cls, jint percent)
{
    int speed_factor = (int) percent;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_SET, M64CORE_SPEED_FACTOR, &speed_factor);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSetFramelimiter(JNIEnv* env, jclass cls, jboolean enabled)
{
    int e = enabled == JNI_TRUE ? 1 : 0;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_SET, M64CORE_SPEED_LIMITER, &e);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSetSlot(JNIEnv* env, jclass cls, jint slotID)
{
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_SET_SLOT, (int) slotID, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuLoadSlot(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_LOAD, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSaveSlot(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_SAVE, 1, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuLoadFile(JNIEnv* env, jclass cls, jstring filename)
{
    const char *nativeString = env->GetStringUTFChars(filename, 0);
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_LOAD, 0, (void *) nativeString);
    env->ReleaseStringUTFChars(filename, nativeString);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSaveFile(JNIEnv* env, jclass cls, jstring filename)
{
    const char *nativeString = env->GetStringUTFChars(filename, 0);
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_SAVE, 1, (void *) nativeString);
    env->ReleaseStringUTFChars(filename, nativeString);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuScreenshot(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_TAKE_NEXT_SCREENSHOT, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGameShark(JNIEnv* env, jclass cls, jboolean pressed)
{
    int p = pressed == JNI_TRUE ? 1 : 0;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_SET, M64CORE_INPUT_GAMESHARK, &p);
}

extern "C" DECLSPEC jint Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGetState(JNIEnv* env, jclass cls)
{
    int state = 0;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_EMU_STATE, &state);
    if (state == M64EMU_STOPPED)
        return (jint) 1;
    else if (state == M64EMU_RUNNING)
        return (jint) 2;
    else if (state == M64EMU_PAUSED)
        return (jint) 3;
    else
        return (jint) 0;
}

extern "C" DECLSPEC jint Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGetSpeed(JNIEnv* env, jclass cls)
{
    int speed = 100;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_SPEED_FACTOR, &speed);
    return (jint) speed;
}

extern "C" DECLSPEC jboolean Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGetFramelimiter(JNIEnv* env, jclass cls)
{
    int e = 1;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_SPEED_LIMITER, &e);
    return (jboolean) (e ? JNI_TRUE : JNI_FALSE);
}

extern "C" DECLSPEC jint Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGetSlot(JNIEnv* env, jclass cls)
{
    int slot = 0;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_SAVESTATE_SLOT, &slot);
    return (jint) slot;
}
