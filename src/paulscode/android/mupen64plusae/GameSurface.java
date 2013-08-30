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
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

/**
 * Represents a graphical area of memory that can be drawn to.
 */
public class GameSurface extends SurfaceView
{
    // Internal EGL objects
    private EGLSurface mEGLSurface;
    private EGLDisplay mEGLDisplay;
    
    public GameSurface( Context context, AttributeSet attribs )
    {
        super( context, attribs );
    }
    
    public boolean createGLContext( int majorVersion, int minorVersion, int[] configSpec )
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
            
            // Get the number of frame buffer configurations that are compatible with the display and specification
            final int[] num_config_out = new int[1];
            egl.eglChooseConfig( display, configSpec, null, 0, num_config_out );
            final int num_config = num_config_out[0];
            
            // Get the compatible EGL frame buffer configurations
            final EGLConfig[] configs = new EGLConfig[num_config];
            boolean success = egl.eglChooseConfig( display, configSpec, configs, num_config, null );
            int bestConfig = -1;
            for( int i = 0; i < num_config; i++ )
            {
                if( bestConfig < 0 )
                {
                    // "Best" config is the first one that is fast and egl-conformant (i.e. not slow, not non-conformant)
                    int[] value = new int[1];
                    egl.eglGetConfigAttrib( display, configs[i], EGL10.EGL_CONFIG_CAVEAT, value );
                    if( value[0] == EGL10.EGL_NONE )
                    {
                        bestConfig = i;
                    }
                }
            }
            if( !success || num_config == 0 )
            {
                Log.e( "GameSurface", "Couldn't find compatible EGL frame buffer configuration" );
                return false;
            }
            
            // Select the best configuration
            Log.i( "GameSurface", "Using Config[" + bestConfig + "]" );
            EGLConfig config = configs[bestConfig];
            
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
            
            // Swap the buffers
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
