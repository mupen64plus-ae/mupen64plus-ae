
/* Include the SDL main definition header */
#include "SDL_main.h"

/*******************************************************************************
                 Functions called by JNI
*******************************************************************************/
#include <jni.h>
#include <android/log.h>
#define printf(...) __android_log_print(ANDROID_LOG_VERBOSE, "SDL_android_main", __VA_ARGS__)

// Called before SDL_main() to initialize JNI bindings in SDL library
extern "C" DECLSPEC void SDLCALL SDL_Android_Init(JNIEnv* env, jclass cls);
// Used to look up any extra commandline args
extern "C" DECLSPEC char * SDLCALL Android_JNI_GetExtraArgs();
// Used to look up which ROM to run
extern "C" DECLSPEC char * SDLCALL Android_JNI_GetROMPath();

// Library init
extern "C" DECLSPEC jint SDLCALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    return JNI_VERSION_1_4;
}

// Start up the SDL app
extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_CoreInterface_nativeInit(JNIEnv* env, jclass cls, jobject obj)
{
    /* This interface could expand with ABI negotiation, calbacks, etc. */
    SDL_Android_Init(env, cls);

    /* Run the application code! */
    int status;

    /* Simple usage: (i.e. to play Mario 64..)
        char *argv[3];
        argv[0] = strdup("mupen64plus");
        argv[1] = strdup( "roms/mario.n64" );
        argv[2] = NULL;
        status = SDL_main( 2, argv );
    */
    // Retrieve the (space-separated) extra args string from Java:
    char *extraArgs = Android_JNI_GetExtraArgs();
    
    char **argv;              // Map to hold the indices
    int argc = 1;             // First arg is reserved for program name
    char *index = extraArgs;  // Start at the beginning of the string
    
    // Loop through the args string to count them:
    while( index != NULL )
    {
        argc++;                        // Count the arg
        index = strchr( index + 1, ' ' );  // Advance to next space
    }
    argc += 2;  // Last two args are the ROM path and NULL
    
    // Allocate enough char pointers to index all the args:
    argv = (char **) malloc( sizeof( char *) * argc );
    
    argv[0] = strdup( "mupen64plus" );  // Store the first arg
    
    char *argSpace;     // Index for pointing to the next space
    argc = 1;           // Reuse argc rather than making a new counter
    index = extraArgs;  // Rewind back to the beginning of the string
    
    // Loop through the args string again, this time to index them:
    while( index != NULL )
    {
        argSpace = strchr( index + 1, ' ' );  // Find the end of the arg
        // Make sure this isn't the last arg:
        if( argSpace != NULL )
           argSpace[0] = '\0'; // Change space to end-of-string
        argv[argc] = strdup( index );  // Have to strdup, or args not recognized
        if( argSpace == NULL )
            index = NULL;  // Last arg, end the loop
        else
            index = argSpace + 1; // Advance to the next arg
        argc++;  // Count the arg
    }
    argv[argc] = strdup( Android_JNI_GetROMPath() );
    argc++;  // Count the ROM path arg
    argv[argc] = NULL;  // End of args
    
    status = SDL_main( argc, argv );  // Launch the emulator

    /* We exit here for consistency with other platforms. */
    exit( status );  // <---- TODO: This is the ASDP bug culprit
}

/* vi: set ts=4 sw=4 expandtab: */
