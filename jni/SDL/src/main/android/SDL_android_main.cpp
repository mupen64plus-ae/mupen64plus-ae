
/* Include the SDL main definition header */
#include "SDL_main.h"

/*******************************************************************************
                 Functions called by JNI
*******************************************************************************/
#include <jni.h>

// Called before SDL_main() to initialize JNI bindings in SDL library
extern "C" void SDL_Android_Init(JNIEnv* env, jclass cls);
// Used to look up any extra commandline args
extern "C" char * Android_JNI_GetExtraArgs();  // TODO: allow this to be used for other args too, not just cheats
// Used to look up which ROM to run
extern "C" char * Android_JNI_GetROMPath();

// Library init
extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    return JNI_VERSION_1_4;
}

// Start up the SDL app
extern "C" void Java_paulscode_android_mupen64plusae_NativeMethods_init(JNIEnv* env, jclass cls, jobject obj)
{
    /* This interface could expand with ABI negotiation, calbacks, etc. */
    SDL_Android_Init(env, cls);

    /* Run the application code! */
    int status;

    /* Let's play Mario 64 */
    char *argv[6];

    argv[0] = strdup("mupen64plus");
    argv[1] = strdup("--cheats");
    argv[2] = strdup( Android_JNI_GetExtraArgs() );  // TODO: allow this to hold other things besides cheats
    argv[3] = strdup("--nospeedlimit");
    argv[4] = strdup( Android_JNI_GetROMPath() );
    argv[5] = NULL;
    status = SDL_main(5, argv);

//    argv[0] = strdup("mupen64plus");
//    argv[1] = strdup( /*"roms/mario.n64"*/ Android_JNI_GetROMPath() );
//    argv[2] = NULL;
//    status = SDL_main(2, argv);

    /* We exit here for consistency with other platforms. */
    exit(status);
}

/* vi: set ts=4 sw=4 expandtab: */
