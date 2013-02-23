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
    // Internal flags
    private boolean mIsRgba8888 = false;
    
    // Internal EGL objects
    private EGLSurface mEGLSurface;
    private EGLDisplay mEGLDisplay;
    
    public GameSurface( Context context, AttributeSet attribs )
    {
        super( context, attribs );
        
        getHolder().addCallback( this );
        setFocusable( true );
        setFocusableInTouchMode( true );
        requestFocus();
    }
    
    public void setColorMode( boolean isRgba8888 )
    {
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
    
    public boolean createGLContext( int majorVersion, int minorVersion )
    {
        Log.v( "GameSurface", "Starting up OpenGL ES " + majorVersion + "." + minorVersion );
        try
        {
            // Get EGL instance
            EGL10 egl = (EGL10) EGLContext.getEGL();
            
            // Get an EGL display connection for the native display
            EGLDisplay display = egl.eglGetDisplay( EGL10.EGL_DEFAULT_DISPLAY );
            if( display == EGL10.EGL_NO_DISPLAY )
            {
                Log.e( "GameSurface", "Couldn't find EGL display connection" );
                return false;
            }
            
            // Initialize the EGL display connection and obtain the GLES version supported by the device
            final int[] version = new int[2];
            if( !egl.eglInitialize( display, version ) )
            {
                Log.e( "GameSurface", "Couldn't initialize EGL display connection" );
                return false;
            }
            
            // Generate a bit mask to limit the configuration search to compatible GLES versions
            final int EGL_OPENGL_ES_BIT = 1;
            final int EGL_OPENGL_ES2_BIT = 4;
            int renderableType = 0;
            if( majorVersion == 2 )
            {
                renderableType = EGL_OPENGL_ES2_BIT;
            }
            else if( majorVersion == 1 )
            {
                renderableType = EGL_OPENGL_ES_BIT;
            }
            
            // Specify the desired EGL frame buffer configuration
            // @formatter:off
            final int[] configSpec;
            if( mIsRgba8888 )
            {
                // User has requested 32-bit color
                configSpec = new int[]
                { 
                    EGL10.EGL_RED_SIZE,    8,                   // request 8 bits of red
                    EGL10.EGL_GREEN_SIZE,  8,                   // request 8 bits of green
                    EGL10.EGL_BLUE_SIZE,   8,                   // request 8 bits of blue
                    EGL10.EGL_ALPHA_SIZE,  8,                   // request 8 bits of alpha
                    EGL10.EGL_DEPTH_SIZE, 16,                   // request 16-bit depth (Z) buffer
                    EGL10.EGL_RENDERABLE_TYPE, renderableType,  // limit search to requested GLES version
                    EGL10.EGL_NONE                              // terminate array
                };
            }
            else
            {
                // User will take whatever color depth is available
                configSpec = new int[] 
                { 
                    EGL10.EGL_DEPTH_SIZE, 16,                   // request 16-bit depth (Z) buffer
                    EGL10.EGL_RENDERABLE_TYPE, renderableType,  // limit search to requested GLES version
                    EGL10.EGL_NONE                              // terminate array
                };
            }
            // @formatter:on            
            
            // Get an EGL frame buffer configuration that is compatible with the display and specification
            final EGLConfig[] configs = new EGLConfig[1];
            final int[] num_config = new int[1];
            if( !egl.eglChooseConfig( display, configSpec, configs, 1, num_config ) || num_config[0] == 0 )
            {
                Log.e( "GameSurface", "Couldn't find compatible EGL frame buffer configuration" );
                return false;
            }
            EGLConfig config = configs[0];
            
            // Create an EGL rendering context and ensure that it supports the requested GLES version, display
            // connection, and frame buffer configuration (http://stackoverflow.com/a/5930935/254218)
            final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
            final int[] contextAttrs = new int[] { EGL_CONTEXT_CLIENT_VERSION, majorVersion, EGL10.EGL_NONE };
            EGLContext context = egl.eglCreateContext( display, config, EGL10.EGL_NO_CONTEXT, contextAttrs );
            if( context.equals( EGL10.EGL_NO_CONTEXT ) )
            {
                Log.e( "GameSurface", "Couldn't create EGL rendering context" );
                return false;
            }
            
            // Create an EGL window surface from the generated EGL display and configuration
            EGLSurface surface = egl.eglCreateWindowSurface( display, config, this, null );
            if( surface.equals( EGL10.EGL_NO_SURFACE ) )
            {
                Log.e( "GameSurface", "Couldn't create EGL window surface" );
                return false;
            }
            
            // Bind the EGL rendering context to the window surface and current rendering thread
            if( !egl.eglMakeCurrent( display, surface, surface, context ) )
            {
                Log.e( "GameSurface", "Couldn't bind EGL rendering context to surface" );
                return false;
            }
            
            // Store the EGL objects to permit frame buffer swaps later
            mEGLDisplay = display;
            mEGLSurface = surface;
            return true;
        }
        catch( IllegalArgumentException e )
        {
            Log.v( "GameSurface", e.toString() );
            for( StackTraceElement s : e.getStackTrace() )
            {
                Log.v( "GameSurface", s.toString() );
            }
            return false;
        }
    }
    
    public void flipBuffers()
    {
        try
        {
            // Get EGL instance
            EGL10 egl = (EGL10) EGLContext.getEGL();
            
            // Wait for native-side executions to complete before doing any further GL rendering calls
            egl.eglWaitNative( EGL10.EGL_CORE_NATIVE_ENGINE, null );
            
            // -- Drawing from the core occurs here -- //
            
            // Wait for GL executions to complete before doing any further native-side rendering calls
            egl.eglWaitGL();
            
            // Rendering is complete, swap the buffers
            egl.eglSwapBuffers( mEGLDisplay, mEGLSurface );
        }
        catch( IllegalArgumentException e )
        {
            Log.v( "GameSurface", "flipBuffers(): " + e );
            for( StackTraceElement s : e.getStackTrace() )
            {
                Log.v( "GameSurface", s.toString() );
            }
        }
    }
}
