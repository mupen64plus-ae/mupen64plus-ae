/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: paulscode, lioncash
 */
package paulscode.android.mupen64plusae;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import paulscode.android.mupen64plusae.util.SafeMethods;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Represents a graphical area of memory that can be drawn to.
 */
public class GameSurface extends SurfaceView implements SurfaceHolder.Callback
{
    public interface CoreLifecycleListener
    {
        public void onCoreStartup();
        public void onCoreShutdown();
    }
    
    public interface OnFpsChangedListener
    {
        /**
         * Called when the frame rate value has changed.
         * 
         * @param fps The new FPS value.
         */
        public void onFpsChanged( int fps );
    }
    
    // Thread that the emulator core runs on
    private static Thread mCoreThread;
    
    // Core lifecycle listener
    private CoreLifecycleListener mClListener;
    
    // Frame rate listener
    private OnFpsChangedListener mFpsListener;
    private int mFpsRecalcPeriod = 0;
    private boolean mIsFpsEnabled = false;
    private long mLastFpsTime = 0;
    private int mFrameCount = -1;
    
    // Internal flags
    private boolean mBuffFlipped = false;
    
    // EGL private objects
    private EGLSurface mEGLSurface;
    private EGLDisplay mEGLDisplay;
    
    // Startup
    public GameSurface( Context context, AttributeSet attribs )
    {
        super( context, attribs );
        
        getHolder().addCallback( this );
        setFocusable( true );
        setFocusableInTouchMode( true );
        requestFocus();
    }
    
    public void setListeners( CoreLifecycleListener clListener, OnFpsChangedListener fpsListener, int fpsRecalcPeriod )
    {
        mClListener = clListener;
        mFpsListener = fpsListener;
        mFpsRecalcPeriod = fpsRecalcPeriod;
        mIsFpsEnabled = mFpsRecalcPeriod > 0;
    }

    @Override
    public void surfaceCreated( SurfaceHolder holder )
    {
        // Called when we have a valid drawing surface
        Log.i( "GameSurface", "surfaceCreated: " );
    }
    
    @SuppressWarnings( "deprecation" )
    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
    {
        // Called when the surface is resized
        Log.i( "GameSurface", "surfaceChanged: " );
        
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
                 * TODO: Not sure this is right, Android API says,
                 * "System chooses an opaque format", but how do we know which one??
                 */
                break;
            default:
                Log.w( "GameLifecycleHandler", "Pixel format unknown: " + format );
                break;
        }
        NativeMethods.onResize( width, height, sdlFormat );
        
        mCoreThread = new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                NativeMethods.init();
            }
        }, "CoreThread" );
        mCoreThread.start();
        
        // Wait for initialization to complete
        while( !CoreInterface.finishedReading )
            SafeMethods.sleep( 40 );
        
        while( NativeMethods.stateEmulator() != CoreInterface.EMULATOR_STATE_RUNNING )
            SafeMethods.sleep( 40 );
        
        mBuffFlipped = false;
        while( !mBuffFlipped )
            SafeMethods.sleep( 40 );
        
        // The core is fully started up, notify the listener
        if( mClListener != null )
            mClListener.onCoreStartup();
    }
    
    @Override
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        // Called when we lose the surface
        Log.i( "GameSurface", "surfaceDestroyed: " );
        
        // The core is about to shut down, notify the listener
        if( mClListener != null )
            mClListener.onCoreShutdown();
        
        // Tell the core to quit
        NativeMethods.quit();
        
        // Now wait for the core thread to quit
        if( mCoreThread != null )
        {
            try
            {
                mCoreThread.join();
            }
            catch( Exception e )
            {
                Log.i( "GameSurface", "Problem stopping core thread: " + e );
            }
            mCoreThread = null;
        }

    }
    
    @Override
    public void onDraw( Canvas canvas )
    {
        // Unused, suppress the super method
    }
    
    // EGL functions
    public boolean initEGL( int majorVersion, int minorVersion )
    {
        Log.v( "GameSurface", "Starting up OpenGL ES " + majorVersion + "." + minorVersion );
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
            
            // @formatter:off
            int[] configSpec;
            if( Globals.userPrefs.isRgba8888 )
            {
                configSpec = new int[]
                        { 
                            EGL10.EGL_RED_SIZE,    8, // get a config with red 8
                            EGL10.EGL_GREEN_SIZE,  8, // get a config with green 8
                            EGL10.EGL_BLUE_SIZE,   8, // get a config with blue 8
                            EGL10.EGL_ALPHA_SIZE,  8, // get a config with alpha 8
                            EGL10.EGL_DEPTH_SIZE, 16, // get a config with depth 16
                            EGL10.EGL_RENDERABLE_TYPE, renderableType, EGL10.EGL_NONE
                        };
            }
            else
            {
                configSpec = new int[] 
                        { 
                            EGL10.EGL_DEPTH_SIZE, 16, // get a config with depth 16
                            EGL10.EGL_RENDERABLE_TYPE, renderableType, EGL10.EGL_NONE
                        };
            }
            // @formatter:on
            
            EGLConfig[] configs = new EGLConfig[1];
            int[] num_config = new int[1];
            if( !egl.eglChooseConfig( dpy, configSpec, configs, 1, num_config )
                    || num_config[0] == 0 )
            {
                Log.e( "GameSurface", "No EGL config available" );
                return false;
            }
            
            EGLConfig config = configs[0];
            // paulscode, GLES2 fix:
            int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
            int[] contextAttrs = new int[] {
                EGL_CONTEXT_CLIENT_VERSION,
                majorVersion,
                EGL10.EGL_NONE };
            
            EGLContext ctx = egl.eglCreateContext( dpy, config, EGL10.EGL_NO_CONTEXT, contextAttrs );
            // end GLES2 fix
            // EGLContext ctx = egl.eglCreateContext( dpy, config, EGL10.EGL_NO_CONTEXT, null );
            
            if( ctx.equals( EGL10.EGL_NO_CONTEXT ) )
            {
                Log.e( "GameSurface", "Couldn't create context" );
                return false;
            }
            
            EGLSurface surface = egl.eglCreateWindowSurface( dpy, config, this, null );
            if( surface.equals( EGL10.EGL_NO_SURFACE ) )
            {
                Log.e( "GameSurface", "Couldn't create surface" );
                return false;
            }
            
            if( !egl.eglMakeCurrent( dpy, surface, surface, ctx ) )
            {
                Log.e( "GameSurface", "Couldn't make context current" );
                return false;
            }
            
            mEGLDisplay = dpy;
            mEGLSurface = surface;
        }
        catch( Exception e )
        {
            Log.v( "GameSurface", e.toString() );
            for( StackTraceElement s : e.getStackTrace() )
            {
                Log.v( "GameSurface", s.toString() );
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
            Log.v( "GameSurface", "flipEGL(): " + e );
            for( StackTraceElement s : e.getStackTrace() )
            {
                Log.v( "GameSurface", s.toString() );
            }
        }
        mBuffFlipped = true;
        
        // Update frame rate info
        if( mIsFpsEnabled )
        {
            mFrameCount++;
            if( mFrameCount >= mFpsRecalcPeriod && mFpsListener != null )
            {
                long currentTime = System.currentTimeMillis();
                float fFPS = ( (float) mFrameCount / (float) ( currentTime - mLastFpsTime ) ) * 1000.0f;
                mFpsListener.onFpsChanged( Math.round( fFPS ) );
                mFrameCount = 0;
                mLastFpsTime = currentTime;
            }
        }
    }
}
