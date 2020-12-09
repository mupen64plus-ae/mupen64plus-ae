package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.util.ArrayList;

import paulscode.android.mupen64plusae.util.PixelBuffer;

public class ShaderDrawer {

    private static final String TAG = "ShaderDrawer";
    private SurfaceTexture mGameTexture;
    private final ArrayList<Shader> mShaderPasses = new ArrayList<>();

    public ShaderDrawer(Context context) {
        ShaderLoader.loadShaders(context);
        //mShaderPasses.add(new Shader(ShaderLoader.N64_DITHER.getVertCode(), ShaderLoader.N64_DITHER.getFragCode(), true, true));
        mShaderPasses.add(new Shader(ShaderLoader.DEFAULT.getVertCode(), ShaderLoader.DEFAULT.getFragCode(), true, true));
/*
        mShaderPasses.add(new Shader(ShaderLoader.BLUR9X9.getVertCode(), ShaderLoader.BLUR9X9.getFragCode(), true, false));
        mShaderPasses.add(new Shader(ShaderLoader.CTR_GEOM.getVertCode(), ShaderLoader.CTR_GEOM.getFragCode(), false, true));

 */

    }

    public void onSurfaceTextureAvailable(PixelBuffer.SurfaceTextureWithSize surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable");

        if (mGameTexture == null) {
            Log.i(TAG, "Texture available, surface=" + width + "x" + height +
                    " render=" + surface.mWidth + "x" + surface.mHeight);

            GLES20.glViewport(0, 0, width, height);
            GLES20.glClearColor(0, 0, 0, 1);

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            int texture = textures[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);

            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            mGameTexture = surface.mSurfaceTexture;
            mGameTexture.attachToGLContext(texture);

            // For some reason this is needed, otherwise frame callbacks stop happening on orientation
            // changes or if the app is put on the background then foreground again
            mGameTexture.updateTexImage();

            for (Shader shader : mShaderPasses) {
                shader.setSourceTexture(texture);
                if (shader.isFirstPass()) {
                    shader.setDimensions(surface.mWidth, surface.mHeight, surface.mWidth, surface.mHeight, width, height);
                } else {
                    shader.setDimensions(surface.mWidth, surface.mHeight, width, height, width, height);

                }
                shader.initShader();
                texture = shader.getFboTextureId();
            }
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

            for (Shader shader : mShaderPasses) {
                shader.draw();
            }
        }
    }
}