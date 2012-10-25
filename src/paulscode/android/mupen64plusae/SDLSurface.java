package paulscode.android.mupen64plusae;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import paulscode.android.mupen64plusae.util.Notifier;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * SDLSurface. This is what we draw on, so we need to know when it's created in order to do anything
 * useful.
 * 
 * Because of this, that's where we set up the SDL thread
 */
public class SDLSurface extends SurfaceView implements SurfaceHolder.Callback, View.OnKeyListener,
        View.OnTouchListener, SensorEventListener
{
    public boolean buffFlipped = false;
    
    // This is what SDL runs in. It invokes SDL_main(), eventually
    private static Thread mSDLThread;
    
    // Framerate calculations
    private static long lastFPSCheck = 0;
    private static int fpsRate = 15;
    private static int frameCount = -1;
    
    // Controlled by IME special keys used for analog input:
    private boolean[] mp64pButtons = new boolean[14];
    private int axisX = 0;
    private int axisY = 0;
    private boolean[] pointers = new boolean[256];
    private int[] pointerX = new int[256];
    private int[] pointerY = new int[256];
    
    // EGL private objects
    private EGLSurface mEGLSurface;
    private EGLDisplay mEGLDisplay;
    
    // Sensors
    private static SensorManager mSensorManager;
    
    // Startup
    @TargetApi( 12 )
    public SDLSurface( Context context, AttributeSet attribs )
    {
        super( context, attribs );
        
        for( int x = 0; x < 256; x++ )
        {
            pointers[x] = false;
            pointerX[x] = -1;
            pointerY[x] = -1;
        }
        
        for( int x = 0; x < 14; x++ )
            mp64pButtons[x] = false;
        
        getHolder().addCallback( this );

        setOnKeyListener( this );
        setOnTouchListener( this );
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 )
        {
            setOnGenericMotionListener( new JoystickListener( this ) );
        }
        if( !isInEditMode() )
        {
            // Do not run this code when drawing this in Eclipse's graphical editor
            mSensorManager = (SensorManager) context.getSystemService( "sensor" );
        }

        setFocusable( true );
        setFocusableInTouchMode( true );
        requestFocus();
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
        NativeMethods.quit();
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
    @SuppressWarnings( "deprecation" )
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
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
                /*
                 * Not sure this is right, Android API says, "System chooses an opaque format", but
                 * how do we know which one??
                 */
                break;
            default:
                Log.v( "SDLSurface", "Pixel format unknown " + format );
                break;
        }
        NativeMethods.onResize( width, height, sdlFormat );
        
        mSDLThread = new Thread( new Runnable() {
            public void run()
            {
                NativeMethods.init();
            }
        }, "SDLThread" );
        mSDLThread.start();
        
        if( Globals.resumeLastSession )
        {
            // TODO: This block seems to cause a force-close
            new Thread( "ResumeSessionThread" )
            {
                @Override
                public void run()
                {
                    while( !GameActivity.finishedReading )
                    {
                        try
                        {
                            Thread.sleep( 40 );
                        }
                        catch( InterruptedException e )
                        {
                        }
                    }
                    
                    try
                    {
                        Thread.sleep( 40 );
                    }
                    catch( InterruptedException e )
                    {
                    }
                    int state = NativeMethods.stateEmulator();
                    
                    while( state != GameActivity.EMULATOR_STATE_RUNNING )
                    {
                        try
                        {
                            Thread.sleep( 40 );
                        }
                        catch( InterruptedException e )
                        {
                        }
                        state = NativeMethods.stateEmulator();
                    }
                    buffFlipped = false;
                    
                    while( !buffFlipped )
                    { // Wait for the game to have started, as indicated
                      // by a call to flip the EGL buffers.
                        try
                        {
                            Thread.sleep( 20 );
                        }
                        catch( InterruptedException e )
                        {
                        }
                    }
                    
                    try
                    {
                        Thread.sleep( 40 );
                    }
                    catch( InterruptedException e )
                    {
                    } // Just to be sure..
                    Log.v( "SDLSurface", "Resuming last session" );
                    Notifier.showToast( "Resuming game", Globals.gameInstance );
                    NativeMethods.fileLoadEmulator( "Mupen64PlusAE_LastSession.sav" );
                }
            }.start();
        }
    }
    
    public void onDraw( Canvas canvas )
    {
        // TODO: Confirm that we are intentionally disabling the call to super.onDraw( canvas )
    }
    
    // EGL functions
    public boolean initEGL( int majorVersion, int minorVersion )
    {
        Log.v( "SDLSurface", "Starting up OpenGL ES " + majorVersion + "." + minorVersion );
        try
        {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            
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
            if( Globals.userPrefs.isRgba8888 )
            {
                configSpec = new int[]
                        { 
                            EGL10.EGL_RED_SIZE,    8, // paulscode: get a config with red 8
                            EGL10.EGL_GREEN_SIZE,  8, // paulscode: get a config with green 8
                            EGL10.EGL_BLUE_SIZE,   8, // paulscode: get a config with blue 8
                            EGL10.EGL_ALPHA_SIZE,  8, // paulscode: get a config with alpha 8
                            EGL10.EGL_DEPTH_SIZE, 16, // paulscode: get a config with depth 16
                            EGL10.EGL_RENDERABLE_TYPE, renderableType, EGL10.EGL_NONE
                        };
            }
            else
            {
                configSpec = new int[] 
                        { 
                            EGL10.EGL_DEPTH_SIZE, 16, // paulscode: get a config with depth 16
                            EGL10.EGL_RENDERABLE_TYPE, renderableType, EGL10.EGL_NONE
                        };
            }
            
            EGLConfig[] configs = new EGLConfig[1];
            int[] num_config = new int[1];
            if( !egl.eglChooseConfig( dpy, configSpec, configs, 1, num_config )
                    || num_config[0] == 0 )
            {
                Log.e( "SDLSurface", "No EGL config available" );
                return false;
            }
            
            EGLConfig config = configs[0];
            // paulscode, GLES2 fix:
            int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
            int[] contextAttrs = new int[] { EGL_CONTEXT_CLIENT_VERSION, majorVersion,
                    EGL10.EGL_NONE };
            
            EGLContext ctx = egl.eglCreateContext( dpy, config, EGL10.EGL_NO_CONTEXT, contextAttrs );
            // end GLES2 fix
            // EGLContext ctx = egl.eglCreateContext( dpy, config, EGL10.EGL_NO_CONTEXT, null );
            
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
            
            egl.eglWaitNative( EGL10.EGL_CORE_NATIVE_ENGINE, null );
            
            // Drawing here
            
            egl.eglWaitGL();
            
            egl.eglSwapBuffers( mEGLDisplay, mEGLSurface );
            
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
        if( !Globals.userPrefs.isInputEnabled )
            return false;
        
        if( action == KeyEvent.ACTION_DOWN )
        {
            NativeMethods.onSDLKeyDown( keyCode );
            return true;
        }
        else if( action == KeyEvent.ACTION_UP )
        {
            NativeMethods.onSDLKeyUp( keyCode );
            return true;
        }
        
        return false;
    }
    


    public boolean onKey( View v, int keyCode, KeyEvent event )
    {
        // Call the other method, so we don't have the same code in two places:
        return onKey( keyCode, event.getAction() );
    }
    
    @TargetApi( 12 )
    public boolean onKey( int key, int action )
    {
        // This method is used by the Xperia-Play native activity:
        if( !Globals.userPrefs.isInputEnabled )
            return false;
        
        if( action == KeyEvent.ACTION_DOWN )
        {
            if( key == KeyEvent.KEYCODE_MENU )
                return false;
            else if( key == KeyEvent.KEYCODE_BACK
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            {
                // There is no Menu button in HC/ ICS, so show/ hide ActionBar when "Back" is pressed
                if( Globals.gameInstance != null )
                {
                    // Show/ hide the Action Bar
                    Globals.gameInstance.runOnUiThread( new Runnable()
                    {
                        @TargetApi( 11 )
                        public void run()
                        {
                            if( Globals.gameInstance.getActionBar().isShowing() )
                            {
                                View view = getRootView();
                                if( view == null )
                                    Log.e( "SDLSurface",
                                            "getRootView() returned null in method onKey" );
                                else
                                    view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
                                Globals.gameInstance.getActionBar().hide();
                            }
                            else
                            {
                                Globals.gameInstance.getActionBar().show();
                            }
                        }
                    } );
                }
                return true;
            }
            
            if( key == KeyEvent.KEYCODE_VOLUME_UP || key == KeyEvent.KEYCODE_VOLUME_DOWN )
            {
                if( Globals.userPrefs.isVolKeysEnabled )
                {
                    NativeMethods.onKeyDown( key );
                    return true;
                }
                return false;
            }
            NativeMethods.onKeyDown( key );
            return true;
        }
        else if( action == KeyEvent.ACTION_UP )
        {
            if( key == KeyEvent.KEYCODE_MENU )
                return false;
            else if( key == KeyEvent.KEYCODE_BACK
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
                return true; // We handled ACTION_DOWN in HC/ ICS, so we must also handle ACTION_UP
                
            if( key == KeyEvent.KEYCODE_VOLUME_UP || key == KeyEvent.KEYCODE_VOLUME_DOWN )
            {
                if( !Globals.userPrefs.isVolKeysEnabled )
                {
                    NativeMethods.onKeyUp( key );
                    return true;
                }
                return false;
            }
            NativeMethods.onKeyUp( key );
            return true;
        }
        return false;
    }
    
    @TargetApi( 5 )
    public boolean onTouch( View v, MotionEvent event )
    {
        if( !Globals.userPrefs.isInputEnabled )
            return false;
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        float x = event.getX();
        float y = event.getY();
        float p = event.getPressure();
        
        NativeMethods.onTouch( action, x, y, p );
        
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
                pid = event.getPointerId( i );
                if( pid > maxPid )
                    maxPid = pid;
                pointers[pid] = true;
            }
        }
        else if( actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_CANCEL )
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
            pid = event.getPointerId( i );
            if( pointers[pid] )
            {
                if( pid > maxPid )
                    maxPid = pid;
                pointerX[pid] = (int) event.getX( i );
                pointerY[pid] = (int) event.getY( i );
            }
        }
        Globals.touchscreenInstance.updatePointers( pointers, pointerX, pointerY, maxPid );
        return true;
    }
    
    // paulscode: Xperia Play native touch input linkage:
    public void onTouchScreen( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
        if( !Globals.userPrefs.isXperiaEnabled || !Globals.userPrefs.isInputEnabled )
            return;
        
        Globals.touchscreenInstance.updatePointers( pointers, pointerX, pointerY, maxPid );
    }
    
    public void onTouchPad( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
        if( !Globals.userPrefs.isXperiaEnabled || !Globals.userPrefs.isInputEnabled )
            return;
        
        GameActivity.mTouchPad.updatePointers( pointers, pointerX, pointerY, maxPid );
    }
    
    // Sensor events
    public void enableSensor( int sensortype, boolean enabled )
    {
        // TODO: This uses getDefaultSensor - what if we have >1 accels?
        if( enabled )
        {
            mSensorManager.registerListener( this, mSensorManager.getDefaultSensor( sensortype ),
                    SensorManager.SENSOR_DELAY_GAME, null );
        }
        else
        {
            mSensorManager.unregisterListener( this, mSensorManager.getDefaultSensor( sensortype ) );
        }
    }
    
    public void onAccuracyChanged( Sensor sensor, int accuracy )
    {
        // TODO: Do we need to implement anything here? This is simply to implement interface
    }
    
    public void onSensorChanged( SensorEvent event )
    {
        if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER )
        {
            NativeMethods.onAccel( event.values[0], event.values[1], event.values[2] );
        }
    }
    
    public static void setActivityTitle( String title )
    {
        // Called from SDLMain() thread and can't directly affect the view
        Globals.gameInstance.sendCommand( GameActivity.COMMAND_CHANGE_TITLE, title );
    }

    public static boolean createGLContext( int majorVersion, int minorVersion )
    {
        return Globals.surfaceInstance.initEGL( majorVersion, minorVersion );
    }

    public static void flipBuffers()
    {
        Globals.surfaceInstance.flipEGL();
        if( SDLSurface.frameCount < 0 )
        {
            SDLSurface.frameCount = 0;
            SDLSurface.lastFPSCheck = System.currentTimeMillis();
        }
        SDLSurface.frameCount++;
        if( ( Globals.touchscreenInstance != null && SDLSurface.frameCount >= Globals.touchscreenInstance.fpsRate )
                || ( Globals.touchscreenInstance == null && SDLSurface.frameCount >= SDLSurface.fpsRate ) )
        {
            long currentTime = System.currentTimeMillis();
            float fFPS = ( (float) SDLSurface.frameCount / (float) ( currentTime - SDLSurface.lastFPSCheck ) ) * 1000.0f;
            if( Globals.touchscreenInstance != null )
                Globals.touchscreenInstance.updateFPS( (int) fFPS );
            SDLSurface.frameCount = 0;
            SDLSurface.lastFPSCheck = currentTime;
        }
    }

    private class JoystickListener implements View.OnGenericMotionListener
    {
        private SDLSurface parent;
        
        public JoystickListener( SDLSurface parent )
        {
            this.parent = parent;
        }
        
        @TargetApi( 12 )
        public boolean onGenericMotion( View v, MotionEvent event )
        {
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 )
            {
                // Must be the same order as EButton listing in plugin.h! (input-sdl plug-in)
                final int Z = 5;
                final int CRight = 8;
                final int CLeft = 9;
                final int CDown = 10;
                final int CUp = 11;
                
                // Z-button and C-pad, interpret left analog trigger and right analog stick
                parent.mp64pButtons[Z] = ( event.getAxisValue( MotionEvent.AXIS_Z ) > 0 );
                parent.mp64pButtons[CLeft] = ( event.getAxisValue( MotionEvent.AXIS_RX ) < -0.5 );
                parent.mp64pButtons[CRight] = ( event.getAxisValue( MotionEvent.AXIS_RX ) > 0.5 );
                parent.mp64pButtons[CUp] = ( event.getAxisValue( MotionEvent.AXIS_RY ) < -0.5 );
                parent.mp64pButtons[CDown] = ( event.getAxisValue( MotionEvent.AXIS_RY ) > 0.5 );
                
                // Analog X-Y, interpret the left analog stick
                parent.axisX = (int) ( 80.0f * event.getAxisValue( MotionEvent.AXIS_X ) );
                parent.axisY = (int) ( -80.0f * event.getAxisValue( MotionEvent.AXIS_Y ) );
                
                NativeMethods.updateVirtualGamePadStates( 0, parent.mp64pButtons, parent.axisX,
                        parent.axisY );
                return true;
            }
            return false;
        }
    }
}
