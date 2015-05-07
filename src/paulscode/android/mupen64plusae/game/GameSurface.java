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
 * Authors: Paul Lamb, littleguy77, Gillou68310
 */
package paulscode.android.mupen64plusae.game;

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
    // LogCat strings for debugging, defined here to simplify maintenance/lookup
    private static final String TAG = "GameSurface";
    
    private static final String EGL_GET_DISPLAY_FAIL = "Failed to find EGL display connection";
    private static final String EGL_GET_DISPLAY = "Found EGL display connection";
    
    private static final String EGL_INITIALIZE_FAIL = "Failed to initialize EGL display connection";
    private static final String EGL_INITIALIZE = "Initialized EGL display connection";
    
    private static final String EGL_CHOOSE_CONFIG_FAIL = "Failed to find compatible EGL frame buffer configuration";
    private static final String EGL_CHOOSE_CONFIG = "Found compatible EGL frame buffer configuration";
    
    private static final String EGL_CREATE_CONTEXT_FAIL = "Failed to create EGL rendering context";
    private static final String EGL_CREATE_CONTEXT = "Created EGL rendering context";
    private static final String EGL_CREATE_CONTEXT_NOCHANGE = "Re-used EGL rendering context";
    
    private static final String EGL_CREATE_SURFACE_FAIL = "Failed to create EGL window surface";
    private static final String EGL_CREATE_SURFACE = "Created EGL window surface";
    private static final String EGL_CREATE_SURFACE_NOCHANGE = "Re-used EGL window surface";
    
    private static final String EGL_BIND_NOCHANGE = "Re-bound EGL rendering context to EGL window surface";
    private static final String EGL_BIND = "Bound EGL rendering context to EGL window surface";
    private static final String EGL_BIND_FAIL = "Failed to bind EGL rendering context to EGL window surface";
    
    private static final String EGL_UNBIND_FAIL = "Failed to unbind EGL rendering context from EGL window surface";
    private static final String EGL_UNBIND = "Unbound EGL rendering context from EGL window surface";
    private static final String EGL_UNBIND_NOCHANGE = "Already unbound EGL rendering context from EGL window surface";
    
    private static final String EGL_DESTROY_SURFACE_FAIL = "Failed to destroy EGL window surface";
    private static final String EGL_DESTROY_SURFACE = "Destroyed EGL window surface";
    private static final String EGL_DESTROY_SURFACE_NOCHANGE = "Already destroyed EGL window surface";
    
    private static final String EGL_DESTROY_CONTEXT_FAIL = "Failed to destroy EGL rendering context";
    private static final String EGL_DESTROY_CONTEXT = "Destroyed EGL rendering context";
    private static final String EGL_DESTROY_CONTEXT_NOCHANGE = "Already destroyed EGL rendering context";
    
    private static final String EGL_TERMINATE_FAIL = "Failed to terminate EGL display connection";
    private static final String EGL_TERMINATE = "Terminated EGL display connection";
    private static final String EGL_TERMINATE_NOCHANGE = "Already terminated EGL display connection";
    
    // Internal EGL objects, created/destroyed in first-in/last-out order
    // A null value indicates they are destroyed
    // An EGL10.EGL_NO_*** value indicates they were unsuccessfully created
    private EGL10 mEgl = null;
    private EGLDisplay mEglDisplay = null;
    private EGLConfig mEglConfig = null;
    private EGLContext mEglContext = null;
    private EGLSurface mEglSurface = null;
    private int mGlMajorVersion;
    
    /**
     * Constructor that is called when inflating a view from XML. This is called when a view is
     * being constructed from an XML file, supplying attributes that were specified in the XML file.
     * This version uses a default style of 0, so the only attribute values applied are those in the
     * Context's Theme and the given AttributeSet. The method onFinishInflate() will be called after
     * all children have been added.
     * 
     * @param context The Context the view is running in, through which it can access the current
     *            theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public GameSurface( Context context, AttributeSet attribs )
    {
        super( context, attribs );
    }
    
    /**
     * The type of precondition that a method expects.
     */
    private enum Precondition
    {
        /** Method expects valid EGL10 object. */
        EGL,
        /** Method expects valid EGL10, EGLDisplay objects. */
        DISPLAY,
        /** Method expects valid EGL10, EGLDisplay, EGLConfig objects. */
        CONFIG,
        /** Method expects valid EGL10, EGLDisplay, EGLConfig, EGLContext objects. */
        CONTEXT,
        /** Method expects valid EGL10, EGLDisplay, EGLConfig, EGLContext, EGLSurface objects. */
        SURFACE
    }
    
    /**
     * Assert the preconditions for a method.
     * 
     * @param precondition The type of precondition that the method expects.
     * @throws IllegalStateException when the precondition has not been met.
     */
    private void assertPrecondition( Precondition precondition )
    {
        // Check egl precondition
        if( mEgl == null )
            throw new IllegalStateException( "EGL not initialized" );
        
        if( precondition != Precondition.EGL )
        {
            // Check display precondition
            if( mEglDisplay == null || mEglDisplay == EGL10.EGL_NO_DISPLAY )
                throw new IllegalStateException( "EGL display not initialized" );
            
            if( precondition != Precondition.DISPLAY )
            {
                // Check config precondition
                if( mEglConfig == null )
                    throw new IllegalStateException( "EGL config not initialized" );
                
                if( precondition != Precondition.CONFIG )
                {
                    // Check context precondition
                    if( mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT )
                        throw new IllegalStateException( "EGL context not initialized" );
                    
                    if( precondition != Precondition.CONTEXT )
                    {
                        // Check surface precondition
                        if( mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE )
                            throw new IllegalStateException( "EGL surface not initialized" );
                    }
                }
            }
        }
    }
    
    /**
     * Create and bind an OpenGL ES rendering context and window surface.
     * 
     * @param majorVersion The major OpenGL ES version.
     * @param minorVersion The minor OpenGL ES version.
     * @param configSpec The desired context configuration.
     * @param forceCreate False to try to reuse context/surface; true to always recreate.
     * @return True, if successful.
     * @see GameSurface#destroyGLContext()
     */
    public boolean createGLContext( int majorVersion, int minorVersion, int[] configSpec, boolean forceCreate )
    {
        Log.i( TAG, "Creating GL context" );
        if( initializeEGL( majorVersion, minorVersion, configSpec ) )
        {
            if( createEGLContext( forceCreate ) )
            {
                if( createEGLSurface( forceCreate ) )
                {
                    if( bindEGLContext() )
                    {
                        return true;
                    }
                    unbindEGLContext();
                }
                destroyEGLSurface();
            }
            destroyEGLContext();
        }
        terminateEGL();
        Log.e( TAG, "Failed to create GL context" );
        return false;
    }
    
    /**
     * Unbind and destroy the previously-created OpenGL ES rendering context and window surface.
     * 
     * @return True, if successful.
     * @see GameSurface#createGLContext(int, int, int[])
     */
    public boolean destroyGLContext()
    {
        Log.i( TAG, "Destroying GL context" );
        if( unbindEGLContext() )
        {
            if( destroyEGLSurface() )
            {
                if( destroyEGLContext() )
                {
                    if( terminateEGL() )
                    {
                        return true;
                    }
                }
            }
        }
        Log.e( TAG, "Failed to destroy GL context" );
        return false;
    }
    
    /**
     * Swap the OpenGL ES framebuffers. Requires valid, bound rendering context and window surface.
     * 
     * @see GameSurface#createGLContext(int, int, int[])
     */
    public void flipBuffers()
    {
        // Uncomment the next line only for debugging; otherwise don't waste the time
        // assertPrecondition( Precondition.surface );
        mEgl.eglSwapBuffers( mEglDisplay, mEglSurface );
    }
    
    /**
     * Initialize the EGL, display connection, and configuration objects.
     * 
     * @param majorVersion The major OpenGL ES version.
     * @param minorVersion The minor OpenGL ES version.
     * @param configSpec The desired context configuration.
     * @return True if all objects were initialized properly.
     * @see GameSurface#terminateEGL()
     */
    private boolean initializeEGL( int majorVersion, int minorVersion, int[] configSpec )
    {
        // Get the EGL object
        mEgl = (EGL10) EGLContext.getEGL();
        
        // Get an EGL display connection for the native display
        mEglDisplay = mEgl.eglGetDisplay( EGL10.EGL_DEFAULT_DISPLAY );
        if( mEglDisplay == EGL10.EGL_NO_DISPLAY )
        {
            Log.e( TAG, EGL_GET_DISPLAY_FAIL );
            return false;
        }
        Log.v( TAG, EGL_GET_DISPLAY );
        
        // Initialize the EGL display connection and obtain the GLES version supported by the device
        final int[] version = new int[2];
        if( !mEgl.eglInitialize( mEglDisplay, version ) )
        {
            Log.e( TAG, EGL_INITIALIZE_FAIL );
            return false;
        }
        Log.v( TAG, EGL_INITIALIZE );
        
        // Set the EGL frame buffer configuration and ensure that it supports the requested GLES
        // version, display connection, and frame buffer configuration
        // (http://stackoverflow.com/a/5930935/254218)
        
        // Get the number of compatible EGL frame buffer configurations
        final int[] numConfigOut = new int[1];
        mEgl.eglChooseConfig( mEglDisplay, configSpec, null, 0, numConfigOut );
        final int numConfig = numConfigOut[0];
        
        // Get the compatible EGL frame buffer configurations
        final EGLConfig[] configs = new EGLConfig[numConfig];
        boolean success = mEgl.eglChooseConfig( mEglDisplay, configSpec, configs, numConfig, null );
        if( !success || numConfig == 0 )
        {
            Log.e( TAG, EGL_CHOOSE_CONFIG_FAIL );
            return false;
        }
        
        // Select the best configuration
        for( int i = 0; i < numConfig; i++ )
        {
            // "Best" config is the first one that is fast and egl-conformant
            // So we test for the "caveat" flag which would indicate slow/non-conformant
            int[] value = new int[1];
            mEgl.eglGetConfigAttrib( mEglDisplay, configs[i], EGL10.EGL_CONFIG_CAVEAT, value );
            if( value[0] == EGL10.EGL_NONE )
            {
                mEglConfig = configs[i];
                break;
            }
        }
        
        // Record the major version
        mGlMajorVersion = majorVersion;
        
        Log.v( TAG, EGL_CHOOSE_CONFIG );
        return true;
    }
    
    /**
     * Create the rendering context. Precondition: Valid EGL10, EGLDisplay, and EGLConfig objects.
     * 
     * @param forceCreate False to try to reuse context; true to always recreate.
     * @return True if the context was created.
     * @throws IllegalStateException if the precondition was not met.
     * @see GameSurface#destroyEGLContext()
     */
    private boolean createEGLContext( boolean forceCreate )
    {
        assertPrecondition( Precondition.CONFIG );
        
        // Create EGL rendering context
        if( forceCreate || mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT )
        {
            final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
            final int[] contextAttrs = new int[] {
                EGL_CONTEXT_CLIENT_VERSION,
                mGlMajorVersion,
                EGL10.EGL_NONE };
            mEglContext = mEgl.eglCreateContext( mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT,
                    contextAttrs );
            if( mEglContext == EGL10.EGL_NO_CONTEXT )
            {
                Log.e( TAG, EGL_CREATE_CONTEXT_FAIL );
                return false;
            }
            Log.v( TAG, EGL_CREATE_CONTEXT );
            return true;
        }
        Log.v( TAG, EGL_CREATE_CONTEXT_NOCHANGE );
        return true;
    }
    
    /**
     * Create the window surface. Precondition: Valid EGL10, EGLDisplay, EGLConfig, and EGLContext
     * objects.
     * 
     * @param forceCreate False to try to reuse surface; true to always recreate.
     * @return True if the surface was created.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean createEGLSurface( boolean forceCreate )
    {
        assertPrecondition( Precondition.CONTEXT );
        
        // Create window surface
        if( forceCreate || mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE )
        {
            mEglSurface = mEgl.eglCreateWindowSurface( mEglDisplay, mEglConfig, this, null );
            if( mEglSurface == EGL10.EGL_NO_SURFACE )
            {
                Log.e( TAG, EGL_CREATE_SURFACE_FAIL );
                return false;
            }
            Log.v( TAG, EGL_CREATE_SURFACE );
            return true;
        }
        Log.v( TAG, EGL_CREATE_SURFACE_NOCHANGE );
        return true;
    }
    
    /**
     * Bind the rendering context and window surface (i.e. make them "current"). Precondition: Valid
     * EGL10, EGLDisplay, EGLConfig, EGLContext, and EGLSurface objects.
     * 
     * @return True if the context/surface were bound.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean bindEGLContext()
    {
        assertPrecondition( Precondition.SURFACE );
        
        // Bind the EGL rendering context to the window surface and current rendering thread
        if( mEgl.eglGetCurrentContext() != mEglContext )
        {
            if( !mEgl.eglMakeCurrent( mEglDisplay, mEglSurface, mEglSurface, mEglContext ) )
            {
                Log.e( TAG, EGL_BIND_FAIL );
                return false;
            }
            Log.v( TAG, EGL_BIND );
            return true;
        }
        Log.v( TAG, EGL_BIND_NOCHANGE );
        return true;
    }
    
    /**
     * Unbind the rendering context and window surface (i.e. make nothing "current"). Precondition:
     * Valid EGL10 and EGLDisplay objects.
     * 
     * @return True if the context/surface were unbound.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean unbindEGLContext()
    {
        assertPrecondition( Precondition.DISPLAY );
        
        // Unbind rendering context and window surface
        if( mEglDisplay != null )
        {
            if( !mEgl.eglMakeCurrent( mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT ) )
            {
                Log.e( TAG, EGL_UNBIND_FAIL );
                return false;
            }
            Log.v( TAG, EGL_UNBIND );
            return true;
        }
        Log.v( TAG, EGL_UNBIND_NOCHANGE );
        return true;
    }
    
    /**
     * Destroy the window surface. Precondition: Valid EGL10 and EGLDisplay objects.
     * 
     * @return True if the surface was destroyed.
     * @throws IllegalStateException if the precondition was not met.
     * @see GameSurface#createEGLSurface()
     */
    private boolean destroyEGLSurface()
    {
        assertPrecondition( Precondition.DISPLAY );
        
        // Destroy window surface
        if( mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE )
        {
            if( !mEgl.eglDestroySurface( mEglDisplay, mEglSurface ) )
            {
                Log.e( TAG, EGL_DESTROY_SURFACE_FAIL );
                return false;
            }
            mEglSurface = null;
            Log.v( TAG, EGL_DESTROY_SURFACE );
            return true;
        }
        Log.v( TAG, EGL_DESTROY_SURFACE_NOCHANGE );
        return true;
    }
    
    /**
     * Destroy the rendering context. Precondition: Valid EGL10 and EGLDisplay objects.
     * 
     * @return True if the context was destroyed.
     * @throws IllegalStateException if the precondition was not met.
     * @see GameSurface#createEGLContext()
     */
    private boolean destroyEGLContext()
    {
        assertPrecondition( Precondition.DISPLAY );
        
        // Destroy rendering context
        if( mEglContext != null && mEglContext != EGL10.EGL_NO_CONTEXT )
        {
            if( !mEgl.eglDestroyContext( mEglDisplay, mEglContext ) )
            {
                Log.e( TAG, EGL_DESTROY_CONTEXT_FAIL );
                return false;
            }
            mEglContext = null;
            Log.v( TAG, EGL_DESTROY_CONTEXT );
            return true;
        }
        Log.v( TAG, EGL_DESTROY_CONTEXT_NOCHANGE );
        return true;
    }
    
    /**
     * Release the configuration, display connection, and EGL objects. Precondition: Valid EGL10
     * object.
     * 
     * @return True if the objects were properly released.
     * @throws IllegalStateException if the precondition was not met.
     * @see GameSurface#initializeEGL(int, int, int[])
     */
    private boolean terminateEGL()
    {
        assertPrecondition( Precondition.EGL );
        
        // Terminate display connection
        if( mEglDisplay != null && mEglDisplay != EGL10.EGL_NO_DISPLAY )
        {
            if( !mEgl.eglTerminate( mEglDisplay ) )
            {
                Log.e( TAG, EGL_TERMINATE_FAIL );
                return false;
            }
            mEglConfig = null;
            mEglDisplay = null;
            mEgl = null;
            Log.v( TAG, EGL_TERMINATE );
            return true;
        }
        Log.v( TAG, EGL_TERMINATE_NOCHANGE );
        return true;
    }
}
