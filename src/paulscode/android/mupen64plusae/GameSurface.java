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
package paulscode.android.mupen64plusae;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Represents a graphical area of memory that can be drawn to.
 */
public class GameSurface extends SurfaceView implements SurfaceHolder.Callback
{
    public interface OnFpsChangedListener
    {
        /**
         * Called when the frame rate value has changed.
         * 
         * @param fps The new FPS value.
         */
        public void onFpsChanged( int fps );
    }
    
    // Frame rate listener
    private OnFpsChangedListener mFpsListener;
    private int mFpsRecalcPeriod = 0;
    private boolean mIsFpsEnabled = false;
    private long mLastFpsTime = 0;
    private int mFrameCount = -1;
    
    // Internal flags
    private boolean mIsRgba8888 = false;
    
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
    
    public void init( OnFpsChangedListener fpsListener, int fpsRecalcPeriod, boolean isRgba8888 )
    {
        mFpsListener = fpsListener;
        mFpsRecalcPeriod = fpsRecalcPeriod;
        mIsFpsEnabled = mFpsRecalcPeriod > 0;
        mIsRgba8888 = isRgba8888;
    }
    
    @Override
    public void surfaceCreated( SurfaceHolder holder )
    {
        Log.i( "GameSurface", "surfaceCreated: " );
    }
    
    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
    {
        Log.i( "GameSurface", "surfaceChanged: " );
        CoreInterface.onResize( format, width, height );
        CoreInterface.startupEmulator();
    }
    
    @Override
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        Log.i( "GameSurface", "surfaceDestroyed: " );
        CoreInterface.shutdownEmulator();
    }
    
    @Override
    public void onDraw( Canvas canvas )
    {
        // Unused, suppress the super method
    }
    
    // EGL functions
    public boolean createGLContext( int majorVersion, int minorVersion )
    {
        Log.v( "GameSurface", "Starting up OpenGL ES " + majorVersion + "." + minorVersion );
        try
        {
            final int EGL_OPENGL_ES_BIT = 1;
            final int EGL_OPENGL_ES2_BIT = 4;
            final int[] version = new int[2];
            final int[] configSpec;
            
            // Get EGL instance.
            EGL10 egl = (EGL10) EGLContext.getEGL();
            
            // Now get an EGL display connection for the native display
            EGLDisplay dpy = egl.eglGetDisplay( EGL10.EGL_DEFAULT_DISPLAY );
            
            // Now initialize the EGL display.
            egl.eglInitialize( dpy, version );
            
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
            if( mIsRgba8888 )
            {
                configSpec = new int[]
                        { 
                            EGL10.EGL_RED_SIZE,    8, // get a config with 8 bits of red
                            EGL10.EGL_GREEN_SIZE,  8, // get a config with 8 bits of green
                            EGL10.EGL_BLUE_SIZE,   8, // get a config with 8 bits of blue
                            EGL10.EGL_ALPHA_SIZE,  8, // get a config with 8 bits of alpha
                            EGL10.EGL_DEPTH_SIZE, 16, // get a config with 16 bits of Z in the depth buffer
                            EGL10.EGL_RENDERABLE_TYPE, renderableType, EGL10.EGL_NONE
                        };
            }
            else
            {
                configSpec = new int[] 
                        { 
                            EGL10.EGL_DEPTH_SIZE, 16, // get a config with 16-bits of Z in the depth buffer.
                            EGL10.EGL_RENDERABLE_TYPE, renderableType, EGL10.EGL_NONE
                        };
            }
            
            final EGLConfig[] configs = new EGLConfig[1];
            final int[] num_config = new int[1];

            // If none of the EGL framebuffer configs correspond to our attributes, then we stop initializing.
            if( !egl.eglChooseConfig( dpy, configSpec, configs, 1, num_config ) || num_config[0] == 0 )
            {
                Log.e( "GameSurface", "No EGL config available" );
                return false;
            }
            // @formatter:on
            
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
        catch( IllegalArgumentException e )
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
    public void flipBuffers()
    {
        try
        {
            // Get an EGL instance.
            EGL10 egl = (EGL10) EGLContext.getEGL();
            
            // Make sure native-side executions complete before
            // doing any further GL rendering calls.
            egl.eglWaitNative( EGL10.EGL_CORE_NATIVE_ENGINE, null );
            
            // -- Drawing here --//
            
            // Make sure all GL executions are complete before
            // doing any further native-side rendering calls.
            egl.eglWaitGL();
            
            // Now finally 'flip' the buffer.
            egl.eglSwapBuffers( mEGLDisplay, mEGLSurface );
            
        }
        catch( IllegalArgumentException e )
        {
            Log.v( "GameSurface", "flipEGL(): " + e );
            for( StackTraceElement s : e.getStackTrace() )
            {
                Log.v( "GameSurface", s.toString() );
            }
        }
        
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
