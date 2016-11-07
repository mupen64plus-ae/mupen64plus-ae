/**
 * GL off screen rendered taken from http://www.anddev.org/android-2d-3d-graphics-opengl-tutorials-f2/possible-to-do-opengl-off-screen-rendering-in-android-t13232.html#wrap
 */

package paulscode.android.mupen64plusae.util;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;
import static javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_HEIGHT;
import static javax.microedition.khronos.egl.EGL10.EGL_NONE;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE;
import static javax.microedition.khronos.egl.EGL10.EGL_WIDTH;
import static javax.microedition.khronos.opengles.GL10.GL_RGBA;
import static javax.microedition.khronos.opengles.GL10.GL_UNSIGNED_BYTE;

public class PixelBuffer {
    final static String TAG = "PixelBuffer";
    final static boolean LIST_CONFIGS = false;

    GLSurfaceView.Renderer mRenderer; // borrow this interface
    int mWidth, mHeight;
    Bitmap mBitmap;

    EGL10 mEGL;
    EGLDisplay mEGLDisplay;
    EGLConfig[] mEGLConfigs;
    EGLConfig mEGLConfig;
    EGLContext mEGLContext;
    EGLSurface mEGLSurface;
    GL10 mGL;

    String mThreadOwner;

    public PixelBuffer(int width, int height) {
        mWidth = width;
        mHeight = height;

        int[] version = new int[2];
        int[] attribList = new int[] {
                EGL_WIDTH, mWidth,
                EGL_HEIGHT, mHeight,
                EGL_NONE
        };

        // No error checking performed, minimum required code to elucidate logic
        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetDisplay(EGL_DEFAULT_DISPLAY);
        mEGL.eglInitialize(mEGLDisplay, version);
        mEGLConfig = chooseConfig(); // Choosing a config is a little more complicated

        final int[] contextAttrs = new int[] {
                EGL_CONTEXT_CLIENT_VERSION,
                2,
                EGL10.EGL_NONE };

        mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig, EGL_NO_CONTEXT, contextAttrs);
        mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig,  attribList);
        mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
        mGL = (GL10) mEGLContext.getGL();

        // Record thread owner of OpenGL context
        mThreadOwner = Thread.currentThread().getName();
    }

    public void destroyGlContext()
    {
        mEGL.eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        mEGL.eglDestroySurface( mEGLDisplay, mEGLSurface );
        mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
    }

    public void setRenderer(GLSurfaceView.Renderer renderer) {
        mRenderer = renderer;

        // Does this thread own the OpenGL context?
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "setRenderer: This thread does not own the OpenGL context.");
            return;
        }

        // Call the renderer initialization routines
        mRenderer.onSurfaceCreated(mGL, mEGLConfig);
        mRenderer.onSurfaceChanged(mGL, mWidth, mHeight);
    }

    public String getGLVersion()
    {
        // Create a minimum supported OpenGL ES context, then check:
        return mGL.glGetString( GL10.GL_VERSION);
    }

    public Bitmap getBitmap() {
        // Do we have a renderer?
        if (mRenderer == null) {
            Log.e(TAG, "getBitmap: Renderer was not set.");
            return null;
        }

        // Does this thread own the OpenGL context?
        if (!Thread.currentThread().getName().equals(mThreadOwner)) {
            Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.");
            return null;
        }

        // Call the renderer draw routine
        mRenderer.onDrawFrame(mGL);
        convertToBitmap();
        return mBitmap;
    }

    private EGLConfig chooseConfig() {
        int[] attribList = new int[] {
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL_NONE
        };

        // No error checking performed, minimum required code to elucidate logic
        // Expand on this logic to be more selective in choosing a configuration
        int[] numConfig = new int[1];
        mEGL.eglChooseConfig(mEGLDisplay, attribList, null, 0, numConfig);
        int configSize = numConfig[0];
        mEGLConfigs = new EGLConfig[configSize];
        mEGL.eglChooseConfig(mEGLDisplay, attribList, mEGLConfigs, configSize, numConfig);

        if (LIST_CONFIGS) {
            listConfig();
        }

        return mEGLConfigs[0];  // Best match is probably the first configuration
    }

    private void listConfig() {
        Log.i(TAG, "Config List {");

        for (EGLConfig config : mEGLConfigs) {
            int mineEGL_ALPHA_SIZE = getConfigAttrib(config, EGL10.EGL_ALPHA_SIZE);
            int mineEGL_ALPHA_MASK_SIZE = getConfigAttrib(config, EGL10.EGL_ALPHA_MASK_SIZE);
            int mineEGL_BLUE_SIZE = getConfigAttrib(config, EGL10.EGL_BLUE_SIZE);
            int mineEGL_BUFFER_SIZE = getConfigAttrib(config, EGL10.EGL_BUFFER_SIZE);
            int mineEGL_COLOR_BUFFER_TYPE = getConfigAttrib(config, EGL10.EGL_COLOR_BUFFER_TYPE);
            int mineEGL_CONFIG_CAVEAT = getConfigAttrib(config, EGL10.EGL_CONFIG_CAVEAT);
            int mineEGL_CONFIG_ID = getConfigAttrib(config, EGL10.EGL_CONFIG_ID);
            int mineEGL_DEPTH_SIZE = getConfigAttrib(config, EGL10.EGL_DEPTH_SIZE);
            int mineEGL_GREEN_SIZE = getConfigAttrib(config, EGL10.EGL_GREEN_SIZE);
            int mineEGL_LEVEL = getConfigAttrib(config, EGL10.EGL_LEVEL);
            int mineEGL_LUMINANCE_SIZE = getConfigAttrib(config, EGL10.EGL_LUMINANCE_SIZE);
            int mineEGL_MAX_PBUFFER_WIDTH = getConfigAttrib(config, EGL10.EGL_MAX_PBUFFER_WIDTH);
            int mineEGL_MAX_PBUFFER_HEIGHT = getConfigAttrib(config, EGL10.EGL_MAX_PBUFFER_HEIGHT);
            int mineEGL_MAX_PBUFFER_PIXELS = getConfigAttrib(config, EGL10.EGL_MAX_PBUFFER_PIXELS);
            int mineEGL_NATIVE_RENDERABLE = getConfigAttrib(config, EGL10.EGL_NATIVE_RENDERABLE);
            int mineEGL_NATIVE_VISUAL_ID = getConfigAttrib(config, EGL10.EGL_NATIVE_VISUAL_ID);
            int mineEGL_NATIVE_VISUAL_TYPE = getConfigAttrib(config, EGL10.EGL_NATIVE_VISUAL_TYPE);
            int mineEGL_RED_SIZE = getConfigAttrib(config, EGL10.EGL_RED_SIZE);
            int mineEGL_RENDERABLE_TYPE = getConfigAttrib(config, EGL10.EGL_RENDERABLE_TYPE);
            int mineEGL_SAMPLE_BUFFERS = getConfigAttrib(config, EGL10.EGL_SAMPLE_BUFFERS);
            int mineEGL_SAMPLES = getConfigAttrib(config, EGL10.EGL_SAMPLES);
            int mineEGL_STENCIL_SIZE = getConfigAttrib(config, EGL10.EGL_STENCIL_SIZE);
            int mineEGL_SURFACE_TYPE = getConfigAttrib(config, EGL10.EGL_SURFACE_TYPE);
            int mineEGL_TRANSPARENT_TYPE = getConfigAttrib(config, EGL10.EGL_TRANSPARENT_TYPE);
            int mineEGL_TRANSPARENT_RED_VALUE = getConfigAttrib(config, EGL10.EGL_TRANSPARENT_RED_VALUE);
            int mineEGL_TRANSPARENT_GREEN_VALUE = getConfigAttrib(config, EGL10.EGL_TRANSPARENT_GREEN_VALUE);
            int mineEGL_TRANSPARENT_BLUE_VALUE = getConfigAttrib(config, EGL10.EGL_TRANSPARENT_BLUE_VALUE);

            Log.i(TAG, ""
                    + " EGL_RED_SIZE=" + mineEGL_RED_SIZE
                    + " EGL_GREEN_SIZE=" + mineEGL_GREEN_SIZE
                    + " EGL_BLUE_SIZE=" + mineEGL_BLUE_SIZE
                    + " EGL_ALPHA_SIZE=" + mineEGL_ALPHA_SIZE
                    + " EGL_DEPTH_SIZE=" + mineEGL_DEPTH_SIZE
                    + " EGL_ALPHA_MASK_SIZE=" + mineEGL_ALPHA_MASK_SIZE
                    + " EGL_BUFFER_SIZE=" + mineEGL_BUFFER_SIZE
                    + " EGL_COLOR_BUFFER_TYPE=" + mineEGL_COLOR_BUFFER_TYPE
                    + " EGL_CONFIG_CAVEAT=" + mineEGL_CONFIG_CAVEAT
                    + " EGL_CONFIG_ID=" + mineEGL_CONFIG_ID
                    + " EGL_LEVEL=" + mineEGL_LEVEL
                    + " EGL_LUMINANCE_SIZE=" + mineEGL_LUMINANCE_SIZE
                    + " EGL_MAX_PBUFFER_WIDTH=" + mineEGL_MAX_PBUFFER_WIDTH
                    + " EGL_MAX_PBUFFER_HEIGHT=" + mineEGL_MAX_PBUFFER_HEIGHT
                    + " EGL_MAX_PBUFFER_PIXELS=" + mineEGL_MAX_PBUFFER_PIXELS
                    + " EGL_NATIVE_RENDERABLE=" + mineEGL_NATIVE_RENDERABLE
                    + " EGL_NATIVE_VISUAL_ID=" + mineEGL_NATIVE_VISUAL_ID
                    + " EGL_NATIVE_VISUAL_TYPE=" + mineEGL_NATIVE_VISUAL_TYPE
                    + " EGL_RENDERABLE_TYPE=" + mineEGL_RENDERABLE_TYPE
                    + " EGL_SAMPLE_BUFFERS=" + mineEGL_SAMPLE_BUFFERS
                    + " EGL_SAMPLES=" + mineEGL_SAMPLES
                    + " EGL_STENCIL_SIZE=" + mineEGL_STENCIL_SIZE
                    + " EGL_SURFACE_TYPE=" + mineEGL_SURFACE_TYPE
                    + " EGL_TRANSPARENT_TYPE=" + mineEGL_TRANSPARENT_TYPE
                    + " EGL_TRANSPARENT_RED_VALUE=" + mineEGL_TRANSPARENT_RED_VALUE
                    + " EGL_TRANSPARENT_GREEN_VALUE=" + mineEGL_TRANSPARENT_GREEN_VALUE
                    + " EGL_TRANSPARENT_BLUE_VALUE=" + mineEGL_TRANSPARENT_BLUE_VALUE);
        }

        Log.i(TAG, "}");
    }

    private int getConfigAttrib(EGLConfig config, int attribute) {
        int[] value = new int[1];
        return mEGL.eglGetConfigAttrib(mEGLDisplay, config,
                attribute, value)? value[0] : 0;
    }

    private void convertToBitmap() {
        IntBuffer ib = IntBuffer.allocate(mWidth*mHeight);
        IntBuffer ibt = IntBuffer.allocate(mWidth*mHeight);
        mGL.glReadPixels(0, 0, mWidth, mHeight, GL_RGBA, GL_UNSIGNED_BYTE, ib);

        // Convert upside down mirror-reversed image to right-side up normal image.
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                ibt.put((mHeight-i-1)*mWidth + j, ib.get(i*mWidth + j));
            }
        }

        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(ibt);
    }
}