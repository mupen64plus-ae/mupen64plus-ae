package paulscode.android.mupen64plusae;

import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.egl.EGLConfig;

import android.content.*;
import android.graphics.*;
import android.hardware.*;
import android.os.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;

/**
 * SDLSurface. This is what we draw on, so we need to know when it's created
 * in order to do anything useful. 
 *
 * Because of this, that's where we set up the SDL thread
 */
class SDLSurface extends SurfaceView implements SurfaceHolder.Callback, 
    View.OnKeyListener, View.OnTouchListener, SensorEventListener
{
    // Controlled by IME special keys used for analog input:
    private boolean[] mp64pButtons = new boolean[14];
    private int axisX = 0;
    private int axisY = 0;

    // This is what SDL runs in. It invokes SDL_main(), eventually
    private static Thread mSDLThread;
    private static int glMajorVersion;
    private static int glMinorVersion;
    
    // EGL private objects
    private EGLContext  mEGLContext;
    private EGLSurface  mEGLSurface;
    private EGLDisplay  mEGLDisplay;

    // Sensors
    private static SensorManager mSensorManager;

    public boolean[] pointers = new boolean[256];
    public int[] pointerX = new int[256];
    public int[] pointerY = new int[256];
    public boolean buffFlipped = false;

    // Startup    
    //public SDLSurface( Context context )
    public SDLSurface( Context context, AttributeSet attribs )
    {
        //super( context );
        super( context, attribs );

        int x;
        for( x = 0; x < 256; x++ )
        {
            pointers[x] = false;
            pointerX[x] = -1;
            pointerY[x] = -1;
        }
        for( x = 0; x < 14; x++ )
            mp64pButtons[x] = false;

        getHolder().addCallback( this ); 
    
        setFocusable( true );
        setFocusableInTouchMode( true );
        requestFocus();
        setOnKeyListener( this ); 
        setOnTouchListener( this );   

        mSensorManager = (SensorManager) context.getSystemService( "sensor" );
        requestFocus();
        setFocusableInTouchMode( true );
    }

    // Called when we have a valid drawing surface
    public void surfaceCreated( SurfaceHolder holder )
    {
        enableSensor( Sensor.TYPE_ACCELEROMETER, true );
    }

    // Called when we lose the surface
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        // Send a quit message to the application
        GameActivityCommon.nativeQuit();
        // Now wait for the SDL thread to quit
        if( mSDLThread != null )
        {
            try
            {
                mSDLThread.join();
            }
            catch( Exception e )
            {
                Log.v( "SDLSurface", "Problem stopping SDL thread: " + e );
            }
            mSDLThread = null;
        }
        enableSensor( Sensor.TYPE_ACCELEROMETER, false );
    }

    // Called when the surface is resized
    public void surfaceChanged( SurfaceHolder holder,
                                int format, int width, int height )
    {
        Log.v( "SDLSurface", "SDLSurface changed" );

        int sdlFormat = 0x85151002; // SDL_PIXELFORMAT_RGB565 by default
        switch( format )
        {
            case PixelFormat.A_8:
                break;
            case PixelFormat.LA_88:
                break;
            case PixelFormat.L_8:
                break;
            case PixelFormat.RGBA_4444:
                sdlFormat = 0x85421002; // SDL_PIXELFORMAT_RGBA4444
                break;
            case PixelFormat.RGBA_5551:
                sdlFormat = 0x85441002; // SDL_PIXELFORMAT_RGBA5551
                break;
            case PixelFormat.RGBA_8888:
                sdlFormat = 0x86462004; // SDL_PIXELFORMAT_RGBA8888
                break;
            case PixelFormat.RGBX_8888:
                sdlFormat = 0x86262004; // SDL_PIXELFORMAT_RGBX8888
                break;
            case PixelFormat.RGB_332:
                sdlFormat = 0x84110801; // SDL_PIXELFORMAT_RGB332
                break;
            case PixelFormat.RGB_565:
                sdlFormat = 0x85151002; // SDL_PIXELFORMAT_RGB565
                break;
            case PixelFormat.RGB_888:
                // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
                sdlFormat = 0x86161804; // SDL_PIXELFORMAT_RGB888
                break;
            case PixelFormat.OPAQUE:
                /* Not sure this is right, Android API says, "System chooses an
                   opaque format", but how do we know which one?? */
                break;
            default:
                Log.v( "SDLSurface", "pixel format unknown " + format );
                break;
        }
        GameActivityCommon.onNativeResize( width, height, sdlFormat );

        mSDLThread = new Thread( new SDLMain(), "SDLThread" ); 
        mSDLThread.start();

        if( GameActivityCommon.resumeLastSession )
        {
            new Thread( "ResumeSessionThread" )
            {
                @Override
                public void run()
                {
                    int c = 0;

                    for( c = 0; !GameActivityCommon.finishedReading; c++ )
                    {
                        try{Thread.sleep( 40 );}catch(InterruptedException e){}
                    }
                    
                    try{Thread.sleep( 40 ); }catch(InterruptedException e){}
                    int state = GameActivityCommon.stateEmulator();
                    
                    for( c = 0; state != GameActivityCommon.EMULATOR_STATE_RUNNING; c++ )
                    {
                        try{Thread.sleep( 40 );}catch(InterruptedException e){}
                        state = GameActivityCommon.stateEmulator();
                    }
                    buffFlipped = false;

                    for( c = 0; !buffFlipped; c++ )
                    { // Wait for the game to have started, as indicated
                      // by a call to flip the EGL buffers.
                        try{Thread.sleep( 20 );}catch(InterruptedException e){}
                    }
                    
                    try{Thread.sleep( 40 );}catch(InterruptedException e){}  // Just to be sure..
                    Log.v( "SDLSurface", "Resuming last session" );
                    GameActivityCommon.showToast( "Resuming game" );
                    GameActivityCommon.fileLoadEmulator( "Mupen64PlusAE_LastSession.sav" );
                }
            }.start();
        }
    }

    // Unused
    public void onDraw( Canvas canvas )
    {}


    // EGL functions
    public boolean initEGL( int majorVersion, int minorVersion )
    {
        Log.v( "SDLSurface", "Starting up OpenGL ES " + majorVersion + "." + minorVersion );
        glMajorVersion = majorVersion;
        glMinorVersion = minorVersion;

        try
        {
            EGL10 egl = (EGL10)EGLContext.getEGL();

            EGLDisplay dpy = egl.eglGetDisplay( EGL10.EGL_DEFAULT_DISPLAY );


            int[] version = new int[2];
            egl.eglInitialize( dpy, version );


            int EGL_OPENGL_ES_BIT = 1;
            int EGL_OPENGL_ES2_BIT = 4;
            int renderableType = 0;
            if( majorVersion == 2 )
            {
                renderableType = EGL_OPENGL_ES2_BIT;
            }
            else if( majorVersion == 1 )
            {
                renderableType = EGL_OPENGL_ES_BIT;
            }

            int[] configSpec;
            if( GameActivityCommon.rgba8888 )
                configSpec = new int[]
                {
                    EGL10.EGL_RED_SIZE,     8,  // paulscode: get a config with red 8
                    EGL10.EGL_GREEN_SIZE,   8,  // paulscode: get a config with green 8
                    EGL10.EGL_BLUE_SIZE,    8,  // paulscode: get a config with blue 8
                    EGL10.EGL_ALPHA_SIZE,   8,  // paulscode: get a config with alpha 8
                    EGL10.EGL_DEPTH_SIZE,   16, // paulscode: get a config with depth 16
                    EGL10.EGL_RENDERABLE_TYPE, renderableType,
                    EGL10.EGL_NONE
                };
            else
                configSpec = new int[]
                {
                    EGL10.EGL_DEPTH_SIZE,   16,  // paulscode: get a config with depth 16
                    EGL10.EGL_RENDERABLE_TYPE, renderableType,
                    EGL10.EGL_NONE
                };

            EGLConfig[] configs = new EGLConfig[1];
            int[] num_config = new int[1];
            if( !egl.eglChooseConfig( dpy, configSpec, configs, 1, num_config ) || num_config[0] == 0 )
            {
                Log.e( "SDLSurface", "No EGL config available" );
                return false;
            }

            EGLConfig config = configs[0];
            // paulscode, GLES2 fix:
                int EGL_CONTEXT_CLIENT_VERSION=0x3098;
                int[] contextAttrs = new int[]
                {
                    EGL_CONTEXT_CLIENT_VERSION, majorVersion,
                    EGL10.EGL_NONE
                };

                EGLContext ctx = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, contextAttrs);
            // end GLES2 fix
                //EGLContext ctx = egl.eglCreateContext( dpy, config, EGL10.EGL_NO_CONTEXT, null );

            if( ctx == EGL10.EGL_NO_CONTEXT )
            {
                Log.e( "SDLSurface", "Couldn't create context" );
                return false;
            }

            EGLSurface surface = egl.eglCreateWindowSurface( dpy, config, this, null );
            if( surface == EGL10.EGL_NO_SURFACE )
            {
                Log.e( "SDLSurface", "Couldn't create surface" );
                return false;
            }

            if( !egl.eglMakeCurrent( dpy, surface, surface, ctx ) )
            {
                Log.e( "SDLSurface", "Couldn't make context current" );
                return false;
            }

            mEGLContext = ctx;
            mEGLDisplay = dpy;
            mEGLSurface = surface;
        }
        catch( Exception e )
        {
            Log.v( "SDLSurface", e.toString() );
            for( StackTraceElement s : e.getStackTrace() )
            {
                Log.v( "SDLSurface", s.toString() );
            }
        }

        return true;
    }

    // EGL buffer flip
    public void flipEGL()
    {
        try
        {
            EGL10 egl = (EGL10) EGLContext.getEGL();

            egl.eglWaitNative( EGL10.EGL_NATIVE_RENDERABLE, null );

            // Drawing here

            egl.eglWaitGL();

            egl.eglSwapBuffers(mEGLDisplay, mEGLSurface);

            
        }
        catch( Exception e )
        {
            Log.v( "SDLSurface", "flipEGL(): " + e );
            for( StackTraceElement s : e.getStackTrace() )
            {
                Log.v( "SDLSurface", s.toString() );
            }
        }
        buffFlipped = true;
    }

    public boolean onSDLKey( int keyCode, int action )
    {
        if( GameActivityCommon.noInputPlugin )
            return false;

        if( action == KeyEvent.ACTION_DOWN )
        {
            GameActivityCommon.onNativeSDLKeyDown( keyCode );
            return true;
        }
        else if( action == KeyEvent.ACTION_UP )
        {
            GameActivityCommon.onNativeSDLKeyUp( keyCode );
            return true;
        }
        
        return false;
    }

    // Key events
    public boolean onKey( View  v, int keyCode, KeyEvent event )
    { // Call the other method, so we don't have the same code in two places:
        return onKey( keyCode, event.getAction() );
    }
    public boolean onKey( int keyCode, int action )
    { // This method is used by the Xperia-Play native activity:
        if( GameActivityCommon.noInputPlugin )
            return false;

        int key = keyCode;
        float str = 0;
        if( keyCode > 255 && Globals.analog_100_64 )
        {
            key = ( keyCode / 100 );
            if( action == KeyEvent.ACTION_DOWN )
                str = ( (float) keyCode - ( (float) key * 100.0f ) );
        }
        else if( action == KeyEvent.ACTION_DOWN )
            str = 64.0f;
        int scancode;
        if( key < 0 || key > 255 )
            scancode = 0;
        else
            scancode = key;
        
        for( int p = 0; p < 4; p++ )
        {
            if( Globals.analog_100_64 && ( scancode == Globals.ctrlr[p][0] || scancode == Globals.ctrlr[p][1] ||
                                           scancode == Globals.ctrlr[p][2] || scancode == Globals.ctrlr[p][3] ) )
            {
                if( scancode == Globals.ctrlr[p][0] )
                    axisX = (int) (80.0f * (str / 64.0f));
                else if( scancode == Globals.ctrlr[p][1] )
                    axisX = (int) (-80.0f * (str / 64.0f));
                else if( scancode == Globals.ctrlr[p][2] )
                    axisY = (int) (-80.0f * (str / 64.0f));
                else if( scancode == Globals.ctrlr[p][3] )
                    axisY = (int) (80.0f * (str / 64.0f));
                GameActivityCommon.updateVirtualGamePadStates( p, mp64pButtons, axisX, axisY );
                return true;
            }
        }
        
        // TODO: implement controllers 2 - 4
        if( action == KeyEvent.ACTION_DOWN )
        {
            if( key == KeyEvent.KEYCODE_MENU )
                return false;
            else if( key == KeyEvent.KEYCODE_BACK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            {  // There is no Menu button in HC/ ICS, so show/ hide ActionBar when "Back" is pressed
                if( GameActivityCommon.mSingleton != null )
                {  // Show/ hide the Action Bar
                    if( GameActivityCommon.mSingleton.getActionBar().isShowing() )
                    {
                        View mView = getRootView();
                        if( mView == null )
                            Log.e( "SDLSurface", "getRootView() returned null in method onKey" );
                        else
                            mView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
                        GameActivityCommon.mSingleton.getActionBar().hide();
                    }
                    else
                        GameActivityCommon.mSingleton.getActionBar().show();
                }
                return true;
            }

            if( key == KeyEvent.KEYCODE_VOLUME_UP ||
                key == KeyEvent.KEYCODE_VOLUME_DOWN )
            {
                if( Globals.volumeKeysDisabled )
                {
                    GameActivityCommon.onNativeKeyDown( key );
                    return true;
                }
                return false;
            }
            GameActivityCommon.onNativeKeyDown( key );
            return true;
        }
        else if( action == KeyEvent.ACTION_UP )
        {
            if( key == KeyEvent.KEYCODE_MENU )
                return false;
            else if( key == KeyEvent.KEYCODE_BACK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
                return true;  // We handled ACTION_DOWN in HC/ ICS, so we must also handle ACTION_UP

            if( key == KeyEvent.KEYCODE_VOLUME_UP ||
                key == KeyEvent.KEYCODE_VOLUME_DOWN )
            {
                if( Globals.volumeKeysDisabled )
                {
                    GameActivityCommon.onNativeKeyUp( key );
                    return true;
                }
                return false;
            }
            GameActivityCommon.onNativeKeyUp( key );
            return true;
        }
        return false;
    }



    public boolean onTouch( View v, MotionEvent event )
    {
        if( GameActivityCommon.noInputPlugin )
            return false;
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        float x = event.getX();
        float y = event.getY();
        float p = event.getPressure();

        GameActivityCommon.onNativeTouch( action, x, y, p );

        int maxPid = 0;
        int pid, i;
        
        if( actionCode == MotionEvent.ACTION_POINTER_DOWN )
        {
            pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
            if( pid > maxPid )
                maxPid = pid;
            pointers[pid] = true;
        }
        else if( actionCode == MotionEvent.ACTION_POINTER_UP )
        {
            pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
            if( pid > maxPid )
                maxPid = pid;
            pointers[pid] = false;
        }
        else if( actionCode == MotionEvent.ACTION_DOWN )
        {
            for( i = 0; i < event.getPointerCount(); i++ )
            {
                pid = event.getPointerId(i);
                if( pid > maxPid )
                    maxPid = pid;
                pointers[pid] = true;
            }
        }
        else if( actionCode == MotionEvent.ACTION_UP ||
                 actionCode == MotionEvent.ACTION_CANCEL )
        {
            for( i = 0; i < 256; i++ )
            {
                pointers[i] = false;
                pointerX[i] = -1;
                pointerY[i] = -1;
            }
        }

        for( i = 0; i < event.getPointerCount(); i++ )
        {
            pid = event.getPointerId(i);
            if( pointers[pid] )
            {
                if( pid > maxPid )
                    maxPid = pid;
                pointerX[pid] = (int) event.getX(i);
                pointerY[pid] = (int) event.getY(i);
            }
        }
        GameActivityCommon.mGamePad.updatePointers( pointers, pointerX, pointerY, maxPid );
        return true;
    }

    // paulscode: Xperia Play native touch input linkage:
    public void onTouchScreen( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
        if( !Globals.isXperiaPlay || GameActivityCommon.noInputPlugin )
            return;

        GameActivityCommon.mGamePad.updatePointers( pointers, pointerX, pointerY, maxPid );
    }
    public void onTouchPad( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
        if( !Globals.isXperiaPlay || GameActivityCommon.noInputPlugin )
            return;

        GameActivityXperiaPlay.mTouchPad.updatePointers( pointers, pointerX, pointerY, maxPid );
    }

    // Sensor events
    public void enableSensor(int sensortype, boolean enabled)
    {
        // TODO: This uses getDefaultSensor - what if we have >1 accels?
        if( enabled )
        {
            mSensorManager.registerListener( this, 
                            mSensorManager.getDefaultSensor( sensortype ), 
                            SensorManager.SENSOR_DELAY_GAME, null );
        }
        else
        {
            mSensorManager.unregisterListener( this, 
                            mSensorManager.getDefaultSensor( sensortype ) );
        }
    }
    
    public void onAccuracyChanged( Sensor sensor, int accuracy )
    {
        // TODO
    }

    public void onSensorChanged(SensorEvent event)
    {
        if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER )
        {
            GameActivityCommon.onNativeAccel( event.values[0],
                                       event.values[1],
                                       event.values[2] );
        }
    }
}

