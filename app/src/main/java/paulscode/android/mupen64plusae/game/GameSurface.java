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
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * Represents a graphical area of memory that can be drawn to.
 */
public abstract class GameSurface extends SurfaceView
{
    //Listener for a game surface created event
    public interface GameSurfaceCreatedListener {
        //This is called every time the game surface is created
        void onGameSurfaceCreated();
    }

    protected boolean mTryFullGl = false;

    // LogCat strings for debugging, defined here to simplify maintenance/lookup
    protected static final String TAG = "GameSurface";

    protected static final String EGL_GET_DISPLAY_FAIL = "Failed to find EGL display connection";
    protected static final String EGL_GET_DISPLAY = "Found EGL display connection";

    protected static final String EGL_INITIALIZE_FAIL = "Failed to initialize EGL display connection";
    protected static final String EGL_INITIALIZE = "Initialized EGL display connection";
    protected static final String EGL_INITIALIZE_NOCHANGE = "Re-used EGL display connection";

    protected static final String EGL_CHOOSE_CONFIG_NOCHANGE = "Re-used EGL frame buffer configuration";

    protected static final String EGL_CREATE_CONTEXT_FAIL = "Failed to create EGL rendering context";
    protected static final String EGL_CREATE_CONTEXT = "Created EGL rendering context";
    protected static final String EGL_CREATE_CONTEXT_NOCHANGE = "Re-used EGL rendering context";

    protected static final String EGL_CREATE_SURFACE_FAIL = "Failed to create EGL window surface";
    protected static final String EGL_CREATE_SURFACE = "Created EGL window surface";
    protected static final String EGL_CREATE_SURFACE_NOCHANGE = "Re-used EGL window surface";

    protected static final String EGL_BIND_NOCHANGE = "Re-bound EGL rendering context to EGL window surface";
    protected static final String EGL_BIND = "Bound EGL rendering context to EGL window surface";
    protected static final String EGL_BIND_FAIL = "Failed to bind EGL rendering context to EGL window surface";

    protected static final String EGL_UNBIND_FAIL = "Failed to unbind EGL rendering context from EGL window surface";
    protected static final String EGL_UNBIND = "Unbound EGL rendering context from EGL window surface";
    protected static final String EGL_UNBIND_NOCHANGE = "Already unbound EGL rendering context from EGL window surface";

    protected static final String EGL_DESTROY_SURFACE_FAIL = "Failed to destroy EGL window surface";
    protected static final String EGL_DESTROY_SURFACE = "Destroyed EGL window surface";
    protected static final String EGL_DESTROY_SURFACE_NOCHANGE = "Already destroyed EGL window surface";

    protected static final String EGL_DESTROY_CONTEXT_FAIL = "Failed to destroy EGL rendering context";
    protected static final String EGL_DESTROY_CONTEXT = "Destroyed EGL rendering context";
    protected static final String EGL_DESTROY_CONTEXT_NOCHANGE = "Already destroyed EGL rendering context";

    protected static final String EGL_TERMINATE_FAIL = "Failed to terminate EGL display connection";
    protected static final String EGL_TERMINATE = "Terminated EGL display connection";
    protected static final String EGL_TERMINATE_NOCHANGE = "Already terminated EGL display connection";


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
    public GameSurface(Context context, AttributeSet attribs )
    {
        super( context, attribs );
    }

    /**
     * Set the game surface created listener
     * @param gameSurfaceCreatedListener Game surface created listener
     */
    public abstract void SetGameSurfaceCreatedListener(GameSurfaceCreatedListener gameSurfaceCreatedListener);
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
    public abstract boolean createGLContext( int majorVersion, int minorVersion, int[] configSpec, boolean forceCreate );

    /**
     * Unbind and destroy the previously-created OpenGL ES rendering context and window surface.
     *
     * @return True, if successful.
     * @see GameSurface#createGLContext(int, int, int[], boolean)
     */
    public abstract boolean destroyGLContext();
    /**
     * Return the state of the EGL Context
     *
     * @return True, if ready.
     */
    public abstract boolean isEGLContextReady();

    /**
     * Set the state of the EGL Context to not ready
     *
     */
    public abstract void setEGLContextNotReady();

    /**
     * Swap the OpenGL ES framebuffers. Requires valid, bound rendering context and window surface.
     *
     * @see GameSurface#createGLContext(int, int, int[], boolean)
     */
    public abstract void flipBuffers();

    /**
     * Set to true if we want to attempt a full OpenGL context
     * @param tryFullGL Set to true if we want to attempt a full OpenGL context
     */
    public void setFullGLStatus(boolean tryFullGL)
    {
        mTryFullGl = tryFullGL;
    }
}
