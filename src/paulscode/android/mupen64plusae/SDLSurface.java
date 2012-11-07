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

import java.io.File;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import paulscode.android.mupen64plusae.input.transform.VisibleTouchMap;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.SafeMethods;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Represents a graphical area of memory that can be drawn to. </p> This is what we draw on, so we
 * need to know when it's created in order to do anything useful. </p> Because of this, that's where
 * we set up the SDL thread.
 */
public class SDLSurface extends SurfaceView implements SurfaceHolder.Callback
{
    // This is what SDL runs in. It invokes SDL_main(), eventually
    private static Thread mSDLThread;
    
    // Frame rate calculations
    private long mLastFPSCheck = 0;
    private int mFrameCount = -1;
    private VisibleTouchMap mTouchMap;
    
    // Internal flags
    private boolean mBuffFlipped = false;
    
    // EGL private objects
    private EGLSurface mEGLSurface;
    private EGLDisplay mEGLDisplay;
    
    // Startup
    @TargetApi( 12 )
    public SDLSurface( Context context, AttributeSet attribs )
    {
        super( context, attribs );
        
        getHolder().addCallback( this );
        setFocusable( true );
        setFocusableInTouchMode( true );
        requestFocus();
    }
    
    public void initialize( VisibleTouchMap touchMap )
    {
        mTouchMap = touchMap;
    }
    
    public void waitForResume()
    {
        mBuffFlipped = false;
        
        // Wait for the game to resume by monitoring emulator state and the EGL buffer flip
        do
        {
            SafeMethods.sleep( 500 );
        }
        while( !mBuffFlipped && NativeMethods.stateEmulator() == CoreInterface.EMULATOR_STATE_PAUSED );
    }
    
    private void resumeLastSession()
    {
        while( !CoreInterface.finishedReading )
            SafeMethods.sleep( 40 );
        
        while( NativeMethods.stateEmulator() != CoreInterface.EMULATOR_STATE_RUNNING )
            SafeMethods.sleep( 40 );
        
        mBuffFlipped = false;
        while( !mBuffFlipped )
            SafeMethods.sleep( 40 );
        
        Log.v( "SDLSurface", "Resuming last session" );
        Notifier.showToast( (Activity) getContext(), R.string.toast_resumingSession );
        
        // TODO: *Uncomment NativeMethods.fileLoadEmulator
        // NativeMethods.fileLoadEmulator( Globals.userPrefs.selectedGameAutoSavefile );
    }
    
    // Called when we have a valid drawing surface
    @Override
    public void surfaceCreated( SurfaceHolder holder )
    {
    }
    
    // Called when the surface is resized
    @SuppressWarnings( "deprecation" )
    @Override
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
        
        mSDLThread = new Thread( new Runnable()
        {
            public void run()
            {
                NativeMethods.init();
            }
        }, "SDLThread" );
        mSDLThread.start();
        
        // Resume last game if user is auto-saving and the auto-savefile exists
        if( Globals.userPrefs.isAutoSaveEnabled
                && new File( Globals.userPrefs.selectedGameAutoSavefile ).exists() )
        {
            new Thread( "ResumeSessionThread" )
            {
                @Override
                public void run()
                {
                    resumeLastSession();
                }
            }.start();
        }
    }
    
    // Called when we lose the surface
    @Override
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
    }
    
    @Override
    public void onDraw( Canvas canvas )
    {
        // Unused, suppress the super method
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
            
            // @formatter:off
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
            // @formatter:on
            
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
            int[] contextAttrs = new int[] {
                EGL_CONTEXT_CLIENT_VERSION,
                majorVersion,
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
        mBuffFlipped = true;
        
        // Update frame rate info
        mFrameCount++;
        if( ( mTouchMap != null && mFrameCount >= mTouchMap.getFpsRecalcPeriod() ) )
        {
            long currentTime = System.currentTimeMillis();
            float fFPS = ( (float) mFrameCount / (float) ( currentTime - mLastFPSCheck ) ) * 1000.0f;
            mTouchMap.updateFps( Math.round( fFPS ) );
            mFrameCount = 0;
            mLastFPSCheck = currentTime;
        }
    }
}
