package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import paulscode.android.mupen64plusae.util.PixelBuffer;

public class ShaderDrawer {
    
    private static final String TAG = "ShaderDrawer";
    private SurfaceTexture mGameTexture;
    private final ShaderLoader mSelectedShader;
    private final Shader mShader;

    public ShaderDrawer(Context context) {
        ShaderLoader.loadShaders(context);
        mSelectedShader = ShaderLoader.CTR_GEOM;
        mShader = new Shader(mSelectedShader.getVertCode(), mSelectedShader.getFragCode());
    }

    public void onSurfaceTextureAvailable(PixelBuffer.SurfaceTextureWithSize surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable");

        if (mGameTexture == null) {
            Log.i(TAG, "Texture available, surface=" + width + "x" + height +
                    " render=" + surface.mWidth + "x" + surface.mHeight);

            mShader.setDimensions(width, height, surface.mWidth, surface.mHeight);

            GLES20.glViewport(0, 0, width, height);
            GLES20.glClearColor(0, 0, 0, 1);

            mShader.initShader();

            mGameTexture = surface.mSurfaceTexture;
            mGameTexture.attachToGLContext(mShader.getTextureId());

            // For some reason this is needed, otherwise frame callbacks stop happening on orientation
            // changes or if the app is put on the background then foreground again
            mGameTexture.updateTexImage();
        }
    }

    public void onSurfaceTextureDestroyed() {
        Log.i(TAG, "onSurfaceTextureDestroyed");

        if (mGameTexture != null) {
            Log.i(TAG, "Dettaching texture");

            mGameTexture.detachFromGLContext();
            mGameTexture = null;
        }
    }

    public void onDrawFrame() {
        if (mGameTexture != null) {
            mGameTexture.updateTexImage();

            mShader.draw();
        }
    }
}