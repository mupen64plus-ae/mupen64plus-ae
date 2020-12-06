/*
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
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import paulscode.android.mupen64plusae.util.PixelBuffer;

import static android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT;

/**
 * Represents a graphical area of memory that can be drawn to.
 */
@SuppressWarnings({"unused", "RedundantSuppression", "SameParameterValue"})
public class GameSurface extends SurfaceView implements SurfaceHolder.Callback
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

    // Internal EGL objects, created/destroyed in first-in/last-out order
    // A null value indicates they are destroyed
    // An EGL14.EGL_NO_*** value indicates they were unsuccessfully created
    private EGLDisplay mEglDisplay = null;
    private EGLConfig mEglConfig = null;
    private EGLContext mEglContext = null;
    private EGLSurface mEglSurface = null;
    private boolean mFullOpenGL = false;
    private ShaderDrawer mShaderDrawer;
    private RenderThread mRenderThread = null;
    private PixelBuffer.SurfaceTextureWithSize mSurfaceTexture = null;
    boolean mSurfaceAvailable = false;
    boolean mGlContextStarted = false;
    Context mContext;

    private boolean mIsEGLContextReady = false;     // true if the context is ready

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

        mContext = context;
        getHolder().addCallback(this);
    }

    /**
     * Set the surface texture used for the GL context
     * @param surfaceTexture Game surface texture
     */
    public void setSurfaceTexture(PixelBuffer.SurfaceTextureWithSize surfaceTexture)
    {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.getLayoutParams();

        mSurfaceTexture = surfaceTexture;

        if (mRenderThread != null && mRenderThread.getHandler() != null && mSurfaceTexture != null) {
            mRenderThread.getHandler().sendSurfaceTextureAvailable(params.width, params.height, mSurfaceTexture);
        }
    }

    public void setSurfaceTextureDestroyed() {
        if (mRenderThread != null && mRenderThread.getHandler() != null) {
            mRenderThread.getHandler().sendSurfaceTextureDestroyed();
        }

        mSurfaceTexture = null;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        mSurfaceAvailable = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            holder.getSurface().setFrameRate(59.94f, FRAME_RATE_COMPATIBILITY_DEFAULT);
        }
        startGlContext();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");

        stopGlContext();
        mSurfaceAvailable = false;
    }

    protected void startGlContext() {
        Log.i(TAG, "StartGlContext");

        if (mSurfaceAvailable && !mGlContextStarted) {
            mShaderDrawer = new ShaderDrawer(mContext);
            mRenderThread = new RenderThread();
            mRenderThread.setName("Render thread");
            mRenderThread.start();
            mRenderThread.waitUntilReady();
            setSurfaceTexture(mSurfaceTexture);
            mGlContextStarted = true;
        }
    }

    protected void stopGlContext() {

        Log.i(TAG, "stopGlContext");

        if (mRenderThread == null) {
            return;
        }

        if (mSurfaceAvailable && mGlContextStarted) {
            RenderHandler rh = mRenderThread.getHandler();
            rh.sendShutdown();

            try {
                mRenderThread.join();
            } catch (InterruptedException ie) {
                // not expected
                ie.printStackTrace();
            }
            mRenderThread = null;

            mGlContextStarted = false;
        }
    }


    /**
     * Create and bind an OpenGL ES rendering context and window surface.
     *
     * @param majorVersion The major OpenGL ES version.
]     * @param forceCreate False to try to reuse context/surface; true to always recreate.
     * @return True, if successful.
     */
    private boolean createGLContext( int majorVersion, boolean forceCreate )
    {
        if( mEglSurface != null && mEglSurface != EGL14.EGL_NO_SURFACE )
        {
            if( !unbindEGLContext() || !destroyEGLSurface() )
            {
                Log.e( TAG, "Failed to create GL context" );
                return false;
            }
        }

        Log.i( TAG, "Creating GL context" );
        if( initializeEGL() )
        {
            if( createEGLContext( forceCreate, majorVersion ) )
            {
                if( createEGLSurface( forceCreate ) )
                {
                    if( bindEGLContext() )
                    {
                        final String version = GLES10.glGetString( GLES10.GL_VERSION );
                        Log.i( TAG, "Created GL context " + version );

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
     */
    private boolean destroyGLContext()
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
    public boolean isEGLContextReady()
    {
        return mIsEGLContextReady;
    }

    /**
     * Set the state of the EGL Context to not ready
     *
     */
    public void setEGLContextNotReady()
    {
        mIsEGLContextReady = false;
    }

    /**
     * Swap the OpenGL ES framebuffers. Requires valid, bound rendering context and window surface.
     */

    private void flipBuffers()
    {
        try
        {
            EGL14.eglSwapBuffers( mEglDisplay, mEglSurface );
        }
        catch(final IllegalArgumentException exception)
        {
            exception.printStackTrace();
        }
    }

    /**
     * Initialize the EGL, display connection, and configuration objects.
     *
     * @return True if all objects were initialized properly.
     */
    private boolean initializeEGL()
    {
        // Get an EGL display connection for the native display
        if ( mEglDisplay == null || mEglDisplay == EGL14.EGL_NO_DISPLAY )
        {
            mFullOpenGL = mTryFullGl && EGL14.eglBindAPI(EGL14.EGL_OPENGL_API);

            mEglDisplay = EGL14.eglGetDisplay( EGL14.EGL_DEFAULT_DISPLAY );
            if( mEglDisplay == EGL14.EGL_NO_DISPLAY )
            {
                Log.e( TAG, EGL_GET_DISPLAY_FAIL );
                return false;
            }
            Log.v( TAG, EGL_GET_DISPLAY );

            // Initialize the EGL display connection and obtain the GLES version supported by the device
            final int[] version = new int[2];
            if( !EGL14.eglInitialize( mEglDisplay, version, 0, version, 1 ) )
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

        if (mEglContext == null || mEglContext == EGL14.EGL_NO_CONTEXT)
        {
            int[] numConfigs = new int[1];

            EGLConfig[] configs = new EGLConfig[128];

            int[] num_config = new int[1];

            int[] configSpec = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
            };

            //Full OpenGL, we need to take the old config spec and add a couple of parameters
            if(mFullOpenGL)
            {
                int[] oldConfigSpec = configSpec;
                configSpec = new int[oldConfigSpec.length + 2];

                System.arraycopy( oldConfigSpec, 0, configSpec, 0, oldConfigSpec.length );

                int index = oldConfigSpec.length - 1;
                configSpec[index++] = EGL14.EGL_RENDERABLE_TYPE;
                configSpec[index++] = EGL14.EGL_OPENGL_BIT;
                configSpec[index] = EGL14.EGL_NONE;
            }


            if (!EGL14.eglChooseConfig(mEglDisplay, configSpec, 0, configs, 0, 1, num_config, 0) || num_config[0] == 0) {
                Log.e("GameSurface", "No EGL config available");
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
                    if (configSpec[j] == EGL14.EGL_NONE)
                        break;

                    if (configSpec[j] == EGL14.EGL_RED_SIZE ||
                            configSpec[j] == EGL14.EGL_GREEN_SIZE ||
                            configSpec[j] == EGL14.EGL_BLUE_SIZE ||
                            configSpec[j] == EGL14.EGL_ALPHA_SIZE ||
                            configSpec[j] == EGL14.EGL_DEPTH_SIZE) {
                        EGL14.eglGetConfigAttrib(mEglDisplay, configs[i], configSpec[j], value, 0);
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

        return true;
    }

    /**
     * Create the rendering context. Precondition: Valid EGL14, EGLDisplay, and EGLConfig objects.
     *
     * @param forceCreate False to try to reuse context; true to always recreate.
     * @return True if the context was created.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean createEGLContext( boolean forceCreate, int glMajorVersion )
    {
        // Create EGL rendering context
        if( forceCreate || mEglContext == null || mEglContext == EGL14.EGL_NO_CONTEXT )
        {
            final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
            int[] contextAttrs;

            if(mFullOpenGL)
            {
                contextAttrs = new int[] { EGL14.EGL_NONE };
            }
            else
            {
                contextAttrs = new int[] {
                        EGL_CONTEXT_CLIENT_VERSION,
                        glMajorVersion,
                        EGL14.EGL_NONE };
            }

            mEglContext = EGL14.eglCreateContext( mEglDisplay, mEglConfig, EGL14.EGL_NO_CONTEXT,
                    contextAttrs, 0);
            if( mEglContext == EGL14.EGL_NO_CONTEXT )
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
     * Create the window surface. Precondition: Valid EGL14, EGLDisplay, EGLConfig, and EGLContext
     * objects.
     *
     * @param forceCreate False to try to reuse surface; true to always recreate.
     * @return True if the surface was created.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean createEGLSurface( boolean forceCreate )
    {
        // Create window surface
        if( forceCreate || mEglSurface == null || mEglSurface == EGL14.EGL_NO_SURFACE )
        {
            int[] surfaceAttribs = {EGL14.EGL_NONE};
            mEglSurface = EGL14.eglCreateWindowSurface( mEglDisplay, mEglConfig, getHolder().getSurface(), surfaceAttribs, 0);
            if( mEglSurface == EGL14.EGL_NO_SURFACE )
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
     * EGL14, EGLDisplay, EGLConfig, EGLContext, and EGLSurface objects.
     *
     * @return True if the context/surface were bound.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean bindEGLContext()
    {
        // Bind the EGL rendering context to the window surface and current rendering thread
        if( EGL14.eglGetCurrentContext() != mEglContext )
        {
            if( !EGL14.eglMakeCurrent( mEglDisplay, mEglSurface, mEglSurface, mEglContext ) )
            {
                Log.e( TAG, EGL_BIND_FAIL );
                return false;
            }

            // Disable vsync for minimum latency
            EGL14.eglSwapInterval(mEglDisplay, 0);

            Log.v( TAG, EGL_BIND );
            return true;
        }
        Log.v( TAG, EGL_BIND_NOCHANGE );
        return true;
    }

    /**
     * Unbind the rendering context and window surface (i.e. make nothing "current"). Precondition:
     * Valid EGL14 and EGLDisplay objects.
     *
     * @return True if the context/surface were unbound.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean unbindEGLContext()
    {
        // Unbind rendering context and window surface
        if( mEglDisplay != null && mEglDisplay != EGL14.EGL_NO_DISPLAY )
        {
            if( !EGL14.eglMakeCurrent( mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT ) )
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
     * Destroy the window surface. Precondition: Valid EGL14 and EGLDisplay objects.
     *
     * @return True if the surface was destroyed.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean destroyEGLSurface()
    {
        // Destroy window surface
        if( mEglSurface != null && mEglSurface != EGL14.EGL_NO_SURFACE )
        {
            if( !EGL14.eglDestroySurface( mEglDisplay, mEglSurface ) )
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
     * Destroy the rendering context. Precondition: Valid EGL14 and EGLDisplay objects.
     *
     * @return True if the context was destroyed.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean destroyEGLContext()
    {
        // Destroy rendering context
        if( mEglContext != null && mEglContext != EGL14.EGL_NO_CONTEXT )
        {
            if( !EGL14.eglDestroyContext( mEglDisplay, mEglContext ) )
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
     * Release the configuration, display connection, and EGL objects. Precondition: Valid EGL14
     * object.
     *
     * @return True if the objects were properly released.
     * @throws IllegalStateException if the precondition was not met.
     */
    private boolean terminateEGL()
    {
        // Terminate display connection
        if( mEglDisplay != null && mEglDisplay != EGL14.EGL_NO_DISPLAY )
        {
            if( !EGL14.eglTerminate( mEglDisplay ) )
            {
                Log.e( TAG, EGL_TERMINATE_FAIL );
                return false;
            }
            mEglConfig = null;
            mEglDisplay = null;
            Log.v( TAG, EGL_TERMINATE );
            return true;
        }
        Log.v( TAG, EGL_TERMINATE_NOCHANGE );
        return true;
    }

    /**
     * Thread that handles all rendering and camera operations.
     */
    private class RenderThread extends Thread implements
            SurfaceTexture.OnFrameAvailableListener, Choreographer.FrameCallback {
        // Object must be created on render thread to get correct Looper, but is used from
        // UI thread, so we need to declare it volatile to ensure the UI thread sees a fully
        // constructed object.
        private volatile RenderHandler mHandler;

        // Used to wait for the thread to start.
        private final Object mStartLock = new Object();
        private final Object mStopLock = new Object();
        private boolean mReady = false;
        private SurfaceTexture mFrameAvailableTexture = null;
        private boolean mShuttingDown = false;

        /**
         * Constructor.
         */
        public RenderThread() {
        }

        /**
         * Thread entry point.
         */
        @Override
        public void run() {
            Looper.prepare();

            // We need to create the Handler before reporting ready.
            mHandler = new RenderHandler(this);
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();
            }

            if (createGLContext(2, false)) {
                Looper.loop();
            }

            if (!destroyGLContext()) {
                Log.w( TAG, "Unable to destroy GL context" );
            }

            synchronized (mStartLock) {
                mReady = false;
            }
        }

        /**
         * Waits until the render thread is ready to receive messages.
         * <p>
         * Call from the UI thread.
         */
        public void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        /**
         * Shuts everything down.
         */
        private void shutdown() {
            Log.i(TAG, "shutdown");

            synchronized (mStopLock)
            {
                mShuttingDown = true;

                if (mFrameAvailableTexture != null) {
                    mFrameAvailableTexture.setOnFrameAvailableListener(null);
                    Choreographer.getInstance().removeFrameCallback(this);
                }
                mShaderDrawer.onSurfaceTextureDestroyed();
                Looper.myLooper().quit();
            }
        }

        /**
         * Returns the render thread's Handler. This may be called from any thread.
         */
        public RenderHandler getHandler() {
            return mHandler;
        }

        @Override   // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            synchronized (mStopLock)
            {
                if (!mShuttingDown)
                    mHandler.sendFrameAvailable();
            }
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            /*
            synchronized (mStopLock)
            {
                if (!mShuttingDown) {
                    Choreographer.getInstance().postFrameCallback(this);
                    mHandler.sendFrameAvailable();
                }
            }
             */
        }

        /**
         * Handles incoming fram
         */
        private void frameAvailable() {
            mShaderDrawer.onDrawFrame();
            flipBuffers();
        }

        /**
         * Handles incoming fram
         */
        private void surfaceTextureAvailable(int width, int height, PixelBuffer.SurfaceTextureWithSize surfaceTexture) {
            mFrameAvailableTexture = surfaceTexture.mSurfaceTexture;
            mFrameAvailableTexture.setOnFrameAvailableListener(this);
            Choreographer.getInstance().postFrameCallback(this);
            mShaderDrawer.onSurfaceTextureAvailable(surfaceTexture, width, height);

            // Draw a single frame to prevent a black screen on rotation while game is paused
            frameAvailable();
        }

        /**
         * Handles incoming fram
         */
        private void surfaceTextureDestroyed() {
            if (mFrameAvailableTexture != null) {
                mFrameAvailableTexture.setOnFrameAvailableListener(null);
                Choreographer.getInstance().removeFrameCallback(this);
            }
            mShaderDrawer.onSurfaceTextureDestroyed();
        }
    }

    /**
     * Handler for RenderThread.  Used for messages sent from the UI thread to the render thread.
     * <p>
     * The object is created on the render thread, and the various "send" methods are called
     * from the UI thread.
     */
    private static class RenderHandler extends Handler {
        private static final int MSG_SURFACETEXTURE_AVAILABLE = 1;
        private static final int MSG_SURFACETEXTURE_DESTROYED = 2;
        private static final int MSG_SHUTDOWN = 3;
        private static final int MSG_FRAME_AVAILABLE = 4;

        // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
        // but no real harm in it.
        private final WeakReference<RenderThread> mWeakRenderThread;

        /**
         * Call from render thread.
         */
        public RenderHandler(RenderThread rt) {
            super(Looper.myLooper());
            mWeakRenderThread = new WeakReference<>(rt);
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         * <p>
         * Call from UI thread.
         */
        public void sendShutdown() {
            sendMessage(obtainMessage(MSG_SHUTDOWN));
        }

        /**
         * Sends the "frame available" message.
         * <p>
         * Call from UI thread.
         */
        public void sendFrameAvailable() {
            sendMessage(obtainMessage(MSG_FRAME_AVAILABLE));
        }

        /**
         * Sends the "surface texture available" message
         * <p>
         * Call from UI thread.
         */
        public void sendSurfaceTextureAvailable(int width, int height, PixelBuffer.SurfaceTextureWithSize surfaceTexture) {
            sendMessage(obtainMessage(MSG_SURFACETEXTURE_AVAILABLE, width, height, surfaceTexture));
        }

        /**
         * Sends the "surface texture available" message
         * <p>
         * Call from UI thread.
         */
        public void sendSurfaceTextureDestroyed() {
            sendMessage(obtainMessage(MSG_SURFACETEXTURE_DESTROYED));
        }


        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;

            RenderThread renderThread = mWeakRenderThread.get();
            if (renderThread == null) {
                Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
                return;
            }

            switch (what) {
                case MSG_SHUTDOWN:
                    renderThread.shutdown();
                    break;
                case MSG_FRAME_AVAILABLE:
                    renderThread.frameAvailable();
                    break;
                case MSG_SURFACETEXTURE_AVAILABLE:
                    renderThread.surfaceTextureAvailable(msg.arg1, msg.arg2, (PixelBuffer.SurfaceTextureWithSize) msg.obj);
                    break;
                case MSG_SURFACETEXTURE_DESTROYED:
                    renderThread.surfaceTextureDestroyed();
                    break;
                default:
                    throw new RuntimeException("unknown message " + what);
            }
        }
    }
}
