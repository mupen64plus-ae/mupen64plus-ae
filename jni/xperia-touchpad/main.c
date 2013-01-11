// paulscode, code heavily modified to fit into Mupen64Plus AE project
/*
 * Copyright (c) 2011, Sony Ericsson Mobile Communications AB.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sony Ericsson Mobile Communications AB nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#include <poll.h>
#include <pthread.h>
#include <sched.h>

#include <android/configuration.h>
#include <android/looper.h>
#include <android/native_activity.h>

#include <jni.h>
#include <errno.h>

#include <EGL/egl.h>
#include <GLES/gl.h>

#include <android/log.h>

#define TAG "xperia-touchpad"
#define LOGV(...) ((void)__android_log_print( ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__ ))
#define LOGI(...) ((void)__android_log_print( ANDROID_LOG_INFO, TAG, __VA_ARGS__ ))
#define LOGW(...) ((void)__android_log_print( ANDROID_LOG_WARN, TAG, __VA_ARGS__ ))
#define LOGE(...) ((void)__android_log_print( ANDROID_LOG_ERROR, TAG, __VA_ARGS__ ))

#undef NUM_METHODS
#define NUM_METHODS(x) (sizeof(x)/sizeof(*(x)))

#define APP_STATE_NONE		0
#define APP_STATE_START		1
#define APP_STATE_RESUME	2
#define APP_STATE_PAUSE		3
#define APP_STATE_STOP		4

#define LOOPER_ID_INPUT		1

#define MSG_APP_START				1
#define MSG_APP_RESUME				2
#define MSG_APP_PAUSE				3
#define MSG_APP_SAVEINSTANCESTATE	4
#define MSG_APP_STOP				5
#define MSG_APP_DESTROYED			6
#define MSG_APP_CONFIGCHANGED		7
#define MSG_APP_LOWMEMORY			8
#define MSG_WINDOW_FOCUSCHANGED		9
#define MSG_WINDOW_CREATED			10
#define MSG_WINDOW_DESTROYED		11
#define MSG_INPUTQUEUE_CREATED		12
#define MSG_INPUTQUEUE_DESTROYED	13

#define MAX_POINTERS                64
#define PAD_HEIGHT                  360
#define PAD_WIDTH                   966
#define SOURCE_TOUCHSCREEN          4098
#define SOURCE_TOUCHPAD             1048584

static jobject   g_pActivity        = 0;
static jmethodID javaOnNativeKey    = 0;
static jmethodID javaOnNativeTouch  = 0;
static JNIEnv    *g_pEnv            = 0;
static JavaVM    *g_pVM             = 0;

struct APP_MSG
{
    char msg;
    char arg1;
};

struct APP_INSTANCE
{
    // The application can place a pointer to its own state object here if it likes.
    void* userData;

    // The ANativeActivity object instance that this app is running in.
    ANativeActivity* activity;

    // The current configuration the app is running in.
    AConfiguration* config;

    // The ALooper associated with the app's thread.
    ALooper* looper;

    // When non-NULL, this is the input queue from which the app will receive user input events.
    AInputQueue* inputQueue;
    AInputQueue* pendingInputQueue;

    // When non-NULL, this is the window surface that the app can draw in.
    ANativeWindow* window;
    ANativeWindow* pendingWindow;

    // Activity's current state: APP_STATE_*
    int activityState;
    int pendingActivityState;

    // Message queue.
    struct APP_MSG msgQueue[512];
    int msgQueueLength;

    // Used to synchronize between callbacks and game thread.
    pthread_mutex_t mutex;
    pthread_cond_t cond;

    // Game thread.
    pthread_t thread;

    // State flags.
    int running;
    int destroyed;
};

/**
 * Our saved state data.
 */
struct TOUCHSTATE
{
    int down;
    int x;
    int y;
};

/**
 * Shared state for our app.
 */
struct ENGINE
{
    struct APP_INSTANCE* app;
    struct TOUCHSTATE touchstate_screen[MAX_POINTERS];
    struct TOUCHSTATE touchstate_pad[MAX_POINTERS];
};

static int32_t engine_handle_input( struct APP_INSTANCE* app, AInputEvent* event )
{
    struct ENGINE* engine = ( struct ENGINE* ) app->userData;

    if( AInputEvent_getType( event ) == AINPUT_EVENT_TYPE_KEY )
    {
        int nKeyCode = AKeyEvent_getKeyCode( event );
        int nAction = AKeyEvent_getAction( event );
        int handled = 0;
        if( g_pEnv && g_pActivity )
            handled = ( *g_pEnv )->CallIntMethod( g_pEnv, g_pActivity, javaOnNativeKey, nAction, nKeyCode );
        return handled;
    }

    else if( AInputEvent_getType( event ) == AINPUT_EVENT_TYPE_MOTION
            && g_pEnv )
    {
        // TODO: This is probably inefficient; move some work to startup/shutdown methods

        int action = AMotionEvent_getAction( event );
        int source = AInputEvent_getSource( event );
        int pointerCount = AMotionEvent_getPointerCount( event );

        jintArray pointerIds = ( *g_pEnv )->NewIntArray( g_pEnv, MAX_POINTERS );
        jfloatArray pointerX = ( *g_pEnv )->NewFloatArray( g_pEnv, MAX_POINTERS );
        jfloatArray pointerY = ( *g_pEnv )->NewFloatArray( g_pEnv, MAX_POINTERS );

        jint * tempIds = ( *g_pEnv )->GetIntArrayElements( g_pEnv, pointerIds, 0 );
        jfloat * tempX = ( *g_pEnv )->GetFloatArrayElements( g_pEnv, pointerX, 0 );
        jfloat * tempY = ( *g_pEnv )->GetFloatArrayElements( g_pEnv, pointerY, 0 );

        int i;
        for( i = 0; i < pointerCount; i++ )
        {
            tempIds[i] = AMotionEvent_getPointerId( event, i );
            tempX[i] = AMotionEvent_getX( event, i );
            if( source == SOURCE_TOUCHPAD )
                tempY[i] = PAD_HEIGHT - AMotionEvent_getY( event, i );
            else
                tempY[i] = AMotionEvent_getY( event, i );
        }

        ( *g_pEnv )->ReleaseIntArrayElements( g_pEnv, pointerIds, tempIds, 0 );
        ( *g_pEnv )->ReleaseFloatArrayElements( g_pEnv, pointerX, tempX, 0 );
        ( *g_pEnv )->ReleaseFloatArrayElements( g_pEnv, pointerY, tempY, 0 );

        int handled = 0;
        if( g_pEnv && g_pActivity )
            handled = ( *g_pEnv )->CallIntMethod( g_pEnv, g_pActivity, javaOnNativeTouch,
                    source, action, pointerCount, pointerIds, pointerX, pointerY );

        ( *g_pEnv )->DeleteLocalRef( g_pEnv, pointerIds );
        ( *g_pEnv )->DeleteLocalRef( g_pEnv, pointerX );
        ( *g_pEnv )->DeleteLocalRef( g_pEnv, pointerY );

        return handled;
    }
    return 0;
}

static void app_lock_queue( struct APP_INSTANCE* state )
{
    pthread_mutex_lock( &state->mutex );
}

static void app_unlock_queue( struct APP_INSTANCE* state )
{
    pthread_cond_broadcast( &state->cond );
    pthread_mutex_unlock( &state->mutex );
}

static void instance_app_main( struct APP_INSTANCE* app_instance )
{
    LOGI( "main entering." );

    struct ENGINE engine;

    memset( &engine, 0, sizeof( engine ) );
    app_instance->userData = &engine;
    engine.app = app_instance;

    int run = 1;

    // Our 'main loop'
    while( run == 1 )
    {
        // Read all pending events.
        int msg_index;
        int ident;
        int events;
        struct android_poll_source* source;

        app_lock_queue( app_instance );

        for( msg_index = 0; msg_index < app_instance->msgQueueLength; ++msg_index )
        {
            switch( app_instance->msgQueue[msg_index].msg )
            {
            case MSG_APP_START:
                app_instance->activityState = app_instance->pendingActivityState;
                break;
            case MSG_APP_RESUME:
                app_instance->activityState = app_instance->pendingActivityState;
                break;
            case MSG_APP_PAUSE:
                app_instance->activityState = app_instance->pendingActivityState;
                break;
            case MSG_APP_STOP:
                app_instance->activityState = app_instance->pendingActivityState;
                break;
            case MSG_APP_SAVEINSTANCESTATE:
                break;
            case MSG_APP_LOWMEMORY:
                break;
            case MSG_APP_CONFIGCHANGED:
                break;
            case MSG_APP_DESTROYED:
                run = 0;
                break;
            case MSG_WINDOW_FOCUSCHANGED:
                break;
            case MSG_WINDOW_CREATED:
                app_instance->window = app_instance->pendingWindow;

                int nWidth = ANativeWindow_getWidth( app_instance->window );
                int nHeight = ANativeWindow_getHeight( app_instance->window );
                int nFormat = ANativeWindow_getFormat( app_instance->window );

                unsigned int nHexFormat = 0x00000000;
                if( nFormat == WINDOW_FORMAT_RGBA_8888 )
                    nHexFormat = 0x8888;
                else if( nFormat == WINDOW_FORMAT_RGBX_8888 )
                    nHexFormat = 0x8880;
                else
                    nHexFormat = 0x0565;

                LOGI( "Window Created : Width(%d) Height(%d) Format(%04x)", nWidth, nHeight, nHexFormat );
                break;
            case MSG_WINDOW_DESTROYED:
                app_instance->window = NULL;
                break;
            case MSG_INPUTQUEUE_CREATED:
            case MSG_INPUTQUEUE_DESTROYED:
                if( app_instance->inputQueue != NULL )
                    AInputQueue_detachLooper( app_instance->inputQueue );

                app_instance->inputQueue = app_instance->pendingInputQueue;
                if( app_instance->inputQueue != NULL )
                {
                    AInputQueue_attachLooper( app_instance->inputQueue,
                            app_instance->looper, LOOPER_ID_INPUT, NULL, NULL );
                }
                break;
            };
        }

        app_instance->msgQueueLength = 0;

        app_unlock_queue( app_instance );

        if( !run )
            break;

        // If not rendering, we will block forever waiting for events.
        // If rendering, we loop until all events are read, then continue
        // to draw the next frame.
        while( ( ident = ALooper_pollAll( 250, NULL, &events,
                ( void** ) &source ) ) >= 0 )
        {
            if( ident == LOOPER_ID_INPUT )
            {
                AInputEvent* event = NULL;
                if( AInputQueue_getEvent( app_instance->inputQueue, &event ) >= 0 )
                {
                    if( AInputQueue_preDispatchEvent( app_instance->inputQueue, event ) )
                        continue;

                    int handled = engine_handle_input( app_instance, event );

                    AInputQueue_finishEvent( app_instance->inputQueue, event, handled );
                }
            }
        }
    }

    LOGI( "main exiting." );
}

///////////////////////
///////////////////////
///////////////////////
///////////////////////
///////////////////////

///////////////
static
void OnDestroy( ANativeActivity* activity )
{
    LOGI( "NativeActivity destroy: %p\n", activity );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_APP_DESTROYED;

    while( !app_instance->destroyed )
    {
        LOGI( "NativeActivity destroy waiting on app thread" );
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnStart( ANativeActivity* activity )
{
    LOGI( "NativeActivity start: %p\n", activity );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->pendingActivityState = APP_STATE_START;
    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_APP_START;

    while( app_instance->activityState != app_instance->pendingActivityState )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    app_instance->pendingActivityState = APP_STATE_NONE;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnResume( ANativeActivity* activity )
{
    LOGI( "NativeActivity resume: %p\n", activity );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->pendingActivityState = APP_STATE_RESUME;
    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_APP_RESUME;

    while( app_instance->activityState != app_instance->pendingActivityState )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    app_instance->pendingActivityState = APP_STATE_NONE;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void*
OnSaveInstanceState( ANativeActivity* activity, size_t* out_lentch )
{
    LOGI( "NativeActivity save instance state: %p\n", activity );

    return 0;
}

static
void OnPause( ANativeActivity* activity )
{
    LOGI( "NativeActivity pause: %p\n", activity );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->pendingActivityState = APP_STATE_PAUSE;
    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_APP_PAUSE;

    while( app_instance->activityState != app_instance->pendingActivityState )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    app_instance->pendingActivityState = APP_STATE_NONE;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnStop( ANativeActivity* activity )
{
    LOGI( "NativeActivity stop: %p\n", activity );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->pendingActivityState = APP_STATE_STOP;
    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_APP_STOP;

    while( app_instance->activityState != app_instance->pendingActivityState )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    app_instance->pendingActivityState = APP_STATE_NONE;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnConfigurationChanged( ANativeActivity* activity )
{
    LOGI( "NativeActivity configuration changed: %p\n", activity );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_APP_CONFIGCHANGED;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnLowMemory( ANativeActivity* activity )
{
    LOGI( "NativeActivity low memory: %p\n", activity );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_APP_CONFIGCHANGED;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnWindowFocusChanged( ANativeActivity* activity, int focused )
{
    LOGI( "NativeActivity window focus changed: %p -- %d\n", activity, focused );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->msgQueue[app_instance->msgQueueLength].msg = MSG_WINDOW_FOCUSCHANGED;
    app_instance->msgQueue[app_instance->msgQueueLength++].arg1 = focused;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnNativeWindowCreated( ANativeActivity* activity, ANativeWindow* window )
{
    LOGI( "NativeActivity native window created: %p -- %p\n", activity, window );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->pendingWindow = window;
    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_WINDOW_CREATED;

    while( app_instance->window != app_instance->pendingWindow )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    app_instance->pendingWindow = NULL;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnNativeWindowDestroyed( ANativeActivity* activity, ANativeWindow* window )
{
    LOGI( "NativeActivity native window destroyed: %p -- %p\n", activity, window );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->pendingWindow = NULL;
    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_WINDOW_DESTROYED;

    while( app_instance->window != app_instance->pendingWindow )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnInputQueueCreated( ANativeActivity* activity, AInputQueue* queue )
{
    LOGI( "NativeActivity input queue created: %p -- %p\n", activity, queue );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->pendingInputQueue = queue;
    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_INPUTQUEUE_CREATED;

    while( app_instance->inputQueue != app_instance->pendingInputQueue )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    app_instance->pendingInputQueue = NULL;

    pthread_mutex_unlock( &app_instance->mutex );
}

static
void OnInputQueueDestroyed( ANativeActivity* activity, AInputQueue* queue )
{
    LOGI( "NativeActivity input queue destroyed: %p -- %p\n", activity, queue );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) activity->instance;

    pthread_mutex_lock( &app_instance->mutex );

    app_instance->pendingInputQueue = NULL;
    app_instance->msgQueue[app_instance->msgQueueLength++].msg = MSG_INPUTQUEUE_DESTROYED;

    while( app_instance->inputQueue != app_instance->pendingInputQueue )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }

    pthread_mutex_unlock( &app_instance->mutex );
}

///////////////

static
void*
app_thread_entry( void* param )
{
    LOGI( "NativeActivity entered application thread" );

    ( *g_pVM )->AttachCurrentThread( g_pVM, &g_pEnv, 0 );

    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) param;

    app_instance->config = AConfiguration_new();
    AConfiguration_fromAssetManager( app_instance->config, app_instance->activity->assetManager );

    // Create/get a looper
    ALooper* looper = ALooper_prepare( ALOOPER_PREPARE_ALLOW_NON_CALLBACKS );
    app_instance->looper = looper;

    // Tell the thread which created this one that we are now up and running
    pthread_mutex_lock( &app_instance->mutex );
    app_instance->running = 1;
    pthread_cond_broadcast( &app_instance->cond );
    pthread_mutex_unlock( &app_instance->mutex );

    // Run the game
    instance_app_main( app_instance );

    pthread_mutex_lock( &app_instance->mutex );

    AConfiguration_delete( app_instance->config );

    if( app_instance->inputQueue != NULL )
    {
        AInputQueue_detachLooper( app_instance->inputQueue );
    }

    app_instance->destroyed = 1;

    pthread_cond_broadcast( &app_instance->cond );
    pthread_mutex_unlock( &app_instance->mutex );
    pthread_mutex_destroy( &app_instance->mutex );

    free( app_instance );

    ( *g_pVM )->DetachCurrentThread( g_pVM );

    LOGI( "NativeActivity exiting application thread" );

    return NULL;
}

//
static struct APP_INSTANCE*
app_instance_create( ANativeActivity* activity, void* saved_state, size_t saved_state_size )
{
    struct APP_INSTANCE* app_instance = ( struct APP_INSTANCE* ) malloc( sizeof(struct APP_INSTANCE) );
    memset( app_instance, 0, sizeof(struct APP_INSTANCE) );
    app_instance->activity = activity;

    pthread_mutex_init( &app_instance->mutex, NULL );
    pthread_cond_init( &app_instance->cond, NULL );

    pthread_attr_t attr;
    pthread_attr_init( &attr );
    pthread_attr_setdetachstate( &attr, PTHREAD_CREATE_DETACHED );
    pthread_create( &app_instance->thread, &attr, app_thread_entry, app_instance );

    // Wait for thread to start.
    pthread_mutex_lock( &app_instance->mutex );
    while( !app_instance->running )
    {
        pthread_cond_wait( &app_instance->cond, &app_instance->mutex );
    }
    pthread_mutex_unlock( &app_instance->mutex );

    return app_instance;
}

// Entry point from android/nativeactivity
JNIEXPORT void JNICALL
ANativeActivity_onCreate( ANativeActivity* activity, void* saved_state, size_t saved_state_size )
{
    LOGI( "NativeActivity creating: %p\n", activity );

    activity->callbacks->onDestroy = OnDestroy;
    activity->callbacks->onStart = OnStart;
    activity->callbacks->onResume = OnResume;
    activity->callbacks->onSaveInstanceState = OnSaveInstanceState;
    activity->callbacks->onPause = OnPause;
    activity->callbacks->onStop = OnStop;
    activity->callbacks->onConfigurationChanged = OnConfigurationChanged;
    activity->callbacks->onLowMemory = OnLowMemory;
    activity->callbacks->onWindowFocusChanged = OnWindowFocusChanged;
    activity->callbacks->onNativeWindowCreated = OnNativeWindowCreated;
    activity->callbacks->onNativeWindowDestroyed = OnNativeWindowDestroyed;
    activity->callbacks->onInputQueueCreated = OnInputQueueCreated;
    activity->callbacks->onInputQueueDestroyed = OnInputQueueDestroyed;

    activity->instance = app_instance_create( activity, saved_state, saved_state_size );
}

static
int RegisterThis( JNIEnv* env, jobject clazz )
{
    LOGI( "RegisterThis() was called" );
    g_pActivity = ( jobject )( *env )->NewGlobalRef( env, clazz );

    return 0;
}

static const JNINativeMethod activity_methods[] =
    { { "RegisterThis", "()I", ( void* ) RegisterThis }, };

JNIEXPORT jint JNICALL
JNI_OnLoad( JavaVM * vm, void * reserved )
{
    JNIEnv* env = 0;
    g_pVM = vm;

    if( ( *vm )->GetEnv( vm, ( void** ) &env, JNI_VERSION_1_4 ) != JNI_OK )
    {
        LOGE( "%s - Failed to get the environment using GetEnv()", __FUNCTION__ );
        return -1;
    }

    const char* interface_path = "paulscode/android/mupen64plusae/input/provider/NativeInputSource";
    jclass java_activity_class = ( *env )->FindClass( env, interface_path );

    if( !java_activity_class )
    {
        LOGE( "%s - Failed to get %s class reference", __FUNCTION__, interface_path );
        return -1;
    }

    if( ( *env )->RegisterNatives( env, java_activity_class, activity_methods, NUM_METHODS(activity_methods) ) != JNI_OK )
    {
        LOGE( "%s - Failed to register native activity methods", __FUNCTION__ );
        return -1;
    }

    javaOnNativeKey = ( *env )->GetMethodID( env, java_activity_class, "onNativeKey", "(II)Z" );
    if( !javaOnNativeKey )
    {
        if( ( *env )->ExceptionCheck( env ) )
        {
            LOGE( "%s - GetMethodID( 'onNativeKey' ) threw exception!", __FUNCTION__ );
            ( *env )->ExceptionClear( env );
        }
        return JNI_FALSE;
    }
    javaOnNativeTouch = ( *env )->GetMethodID( env, java_activity_class, "onNativeTouch", "(III[I[F[F)Z" );
    if( !javaOnNativeTouch )
    {
        if( ( *env )->ExceptionCheck( env ) )
        {
            LOGE( "%s - GetMethodID( 'onNativeTouch' ) threw exception!", __FUNCTION__ );
            ( *env )->ExceptionClear( env );
        }
        return JNI_FALSE;
    }

    LOGI( "%s - Complete", __FUNCTION__ );

    return JNI_VERSION_1_4;
}
