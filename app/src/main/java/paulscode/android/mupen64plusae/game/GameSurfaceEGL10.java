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

import android.content.Context;
import android.opengl.GLES10;
import android.util.AttributeSet;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import paulscode.android.mupen64plusae.jni.CoreInterface;

/**
 * Represents a graphical area of memory that can be drawn to.
 */
public class GameSurfaceEGL10 extends GameSurface
{
    // Internal EGL objects, created/destroyed in first-in/last-out order
    // A null value indicates they are destroyed
    // An EGL10.EGL_NO_*** value indicates they were unsuccessfully created
    private EGL10 mEgl = null;
    private EGLDisplay mEglDisplay = null;
    private EGLConfig mEglConfig = null;
    private EGLContext mEglContext = null;
    private EGLSurface mEglSurface = null;
    private int mGlMajorVersion;

    private boolean mIsEGLContextReady = false;     // true if the context is ready

    //Game surface created listener
    GameSurfaceCreatedListener mGameSurfaceCreatedListener = null;

    /**
     * Set the game surface created listener
     * @param gameSurfaceCreatedListener Game surface created listener
     */
    @Override
    public void SetGameSurfaceCreatedListener(GameSurfaceCreatedListener gameSurfaceCreatedListener)
    {
        mGameSurfaceCreatedListener = gameSurfaceCreatedListener;
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called when a view is
     * being constructed from an XML file, supplying attributes that were specified in the XML file.
     * This version uses a default style of 0, so the only attribute values applied are those in the
     * Context's Theme and the given AttributeSet. The method onFinishInflate() will be called after
     * all children have been added.
     *
     * @param context The Context the view is running in, through which it can access the current
     *            theme, resources, etc.
     * @param attribs The attributes of the XML tag that is inflating the view.
     */
    public GameSurfaceEGL10(Context context, AttributeSet attribs )
    {
        super( context, attribs );
    }

    /**
     * Create and bind an OpenGL ES rendering context and window surface.
     *
     * @param majorVersion The major OpenGL ES version.
     * @param minorVersion The minor OpenGL ES version.
     * @param configSpec The desired context configuration.
     * @param forceCreate False to try to reuse context/surface; true to always recreate.
     * @return True, if successful.
     * @see GameSurfaceEGL10#destroyGLContext()
     */
    @Override
    public boolean createGLContext( int majorVersion, int minorVersion, int[] configSpec, boolean forceCreate )
    {
        if( mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE )
        {
            if( !unbindEGLContext() || !destroyEGLSurface() )
            {
                Log.e( TAG, "Failed to create GL context" );
                return false;
            }
        }

        Log.i( TAG, "Creating GL context" );
        if( initializeEGL( majorVersion, minorVersion, configSpec ) )
        {
            if( createEGLContext( forceCreate ) )
            {
                if( createEGLSurface( forceCreate ) )
                {
                    if( bindEGLContext() )
                    {
                        final String version = GLES10.glGetString( GLES10.GL_VERSION );
                        Log.i( TAG, "Created GL context " + version );

                        if(mGameSurfaceCreatedListener != null)
                        {
                            mGameSurfaceCreatedListener.onGameSurfaceCreated();
                        }

                        mIsEGLContextReady = true;
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
     * @see GameSurfaceEGL10#createGLContext(int, int, int[], boolean)
     */
    @Override
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
                        mIsEGLContextReady = false;
                        return true;
                    }
                }
            }
        }
        Log.e( TAG, "Failed to destroy GL context" );
        return false;
    }

    /**
     * Return the state of the EGL Context
     *
     * @return True, if ready.
     */
    @Override
    public boolean isEGLContextReady()
    {
        return mIsEGLContextReady;
    }

    /**
     * Set the state of the EGL Context to not ready
     *
     */
    @Override
    public void setEGLContextNotReady()
    {
        mIsEGLContextReady = false;
    }

    /**
     * Swap the OpenGL ES framebuffers. Requires valid, bound rendering context and window surface.
     *
     * @see GameSurfaceEGL10#createGLContext(int, int, int[], boolean)
     */
    @Override
    public void flipBuffers()
    {
        try
        {
            //Don't swap if paused, fixes core dump in some devices.
            if(mEgl != null && mEglDisplay != null && mEglSurface != null &&
                    !CoreInterface.isPaused())
            {
                mEgl.eglSwapBuffers( mEglDisplay, mEglSurface );
            }
        }
        catch(final IllegalArgumentException exception)
        {
            final StringWriter writer = new StringWriter();
            final PrintWriter printWriter = new PrintWriter( writer );
            exception.printStackTrace( printWriter );
            printWriter.flush();

            final String stackTrace = writer.toString();

            Log.e("GameSurface", "Exception thrown in flipBuffers, stack trace: " + stackTrace);
        }
    }

    /**
     * Initialize the EGL, display connection, and configuration objects.
     *
     * @param majorVersion The major OpenGL ES version.
     * @param minorVersion The minor OpenGL ES version.
     * @param configSpec The desired context configuration.
     * @return True if all objects were initialized properly.
     * @see GameSurfaceEGL10#terminateEGL()
     */
    private boolean initializeEGL( int majorVersion, int minorVersion, int[] configSpec )
    {
        // Get the EGL object
        mEgl = (EGL10) EGLContext.getEGL();

        // Get an EGL display connection for the native display
        if ( mEglDisplay == null || mEglDisplay == EGL10.EGL_NO_DISPLAY )
        {
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
        }
        else
            Log.v( TAG, EGL_INITIALIZE_NOCHANGE );

        // Set the EGL frame buffer configuration and ensure that it supports the requested GLES
        // version, display connection, and frame buffer configuration
        // (http://stackoverflow.com/a/5930935/254218)

        if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT)
        {
            EGLConfig[] configs = new EGLConfig[128];
            int[] num_config = new int[1];
            if (!mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, num_config) || num_config[0] == 0) {
                Log.e("SDL", "No EGL config available");
                return false;
            }
            EGLConfig config = null;
            int bestdiff = -1, bitdiff;
            int[] value = new int[1];

            // eglChooseConfig returns a number of configurations that match or exceed the requested attribs.
            // From those, we select the one that matches our requirements more closely
            Log.v("SDL", "Got " + num_config[0] + " valid modes from egl");
            for(int i = 0; i < num_config[0]; i++) {
                bitdiff = 0;
                // Go through some of the attributes and compute the bit difference between what we want and what we get.
                for (int j = 0; ; j += 2) {
                    if (configSpec[j] == EGL10.EGL_NONE)
                        break;

                    if (configSpec[j+1] != EGL10.EGL_DONT_CARE && (configSpec[j] == EGL10.EGL_RED_SIZE ||
                            configSpec[j] == EGL10.EGL_GREEN_SIZE ||
                            configSpec[j] == EGL10.EGL_BLUE_SIZE ||
                            configSpec[j] == EGL10.EGL_ALPHA_SIZE ||
                            configSpec[j] == EGL10.EGL_DEPTH_SIZE)) {
                        mEgl.eglGetConfigAttrib(mEglDisplay, configs[i], configSpec[j], value);
                        bitdiff += value[0] - configSpec[j + 1]; // value is always >= attrib
                    }
                }

                if (bitdiff < bestdiff || bestdiff == -1) {
                    config = configs[i];
                    bestdiff = bitdiff;
                }

                if (bitdiff == 0) break; // we found an exact match!
            }

            mEglConfig = config;
        }
        else
            Log.v( TAG, EGL_CHOOSE_CONFIG_NOCHANGE );

        // Record the major version
        mGlMajorVersion = majorVersion;

        return true;
    }

    /**
     * Create the rendering context. Precondition: Valid EGL10, EGLDisplay, and EGLConfig objects.
     *
     * @param forceCreate False to try to reuse context; true to always recreate.
     * @return True if the context was created.
     * @throws IllegalStateException if the precondition was not met.
     * @see GameSurfaceEGL10#destroyEGLContext()
     */
    private boolean createEGLContext( boolean forceCreate )
    {
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
        // Create window surface
        if( forceCreate || mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE )
        {
            int[] surfaceAttribs = {EGL10.EGL_NONE};
            mEglSurface = mEgl.eglCreateWindowSurface( mEglDisplay, mEglConfig, this, surfaceAttribs );
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
        // Unbind rendering context and window surface
        if( mEglDisplay != null && mEglDisplay != EGL10.EGL_NO_DISPLAY )
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
     * @see GameSurfaceEGL10#createEGLSurface(boolean)
     */
    private boolean destroyEGLSurface()
    {
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
     * @see GameSurfaceEGL10#createEGLContext(boolean)
     */
    private boolean destroyEGLContext()
    {
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
     * @see GameSurfaceEGL10#initializeEGL(int, int, int[])
     */
    private boolean terminateEGL()
    {
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
