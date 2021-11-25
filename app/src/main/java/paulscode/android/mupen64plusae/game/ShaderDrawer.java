package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.util.PixelBuffer;

public class ShaderDrawer {

    private static final String TAG = "ShaderDrawer";
    private SurfaceTexture mGameTexture;
    private final ArrayList<Shader> mShaderPasses = new ArrayList<>();
    private int mWidth = 0;
    private int mHeight = 0;

    public ShaderDrawer(Context context, ArrayList<ShaderLoader> selectedShaders) {
        ShaderLoader.loadShaders(context);

        for (int index = 0; index < selectedShaders.size(); ++ index) {
            boolean first = index == 0;
            boolean last = index == selectedShaders.size() - 1;
            mShaderPasses.add(new Shader(selectedShaders.get(index).getVertCode(), selectedShaders.get(index).getFragCode(), first, last));
        }

        if (mShaderPasses.size() == 0) {
            mShaderPasses.add(new Shader(ShaderLoader.DEFAULT.getVertCode(), ShaderLoader.DEFAULT.getFragCode(), true, true));
        }

    }

    public void onSurfaceTextureAvailable(PixelBuffer.SurfaceTextureWithSize surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable");

        if (mGameTexture == null) {
            Log.i(TAG, "Texture available, surface=" + width + "x" + height +
                    " render=" + surface.mWidth + "x" + surface.mHeight);
            mWidth = width;
            mHeight = height;

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

            try {
                mGameTexture.attachToGLContext(texture);
            } catch (RuntimeException e) {
                mGameTexture = null;
                return;
            }

            // For some reason this is needed, otherwise frame callbacks stop happening on orientation
            // changes or if the app is put on the background then foreground again
            try {
                mGameTexture.updateTexImage();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

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

    public Bitmap getScreenShot() {

        if (mWidth <= 0 && mHeight <= 0) {
            return null;
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(mWidth * mHeight * 4);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    public void onDrawFrame() {
        if (mGameTexture != null) {
            try {
                mGameTexture.updateTexImage();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            for (Shader shader : mShaderPasses) {
                shader.draw();
            }
        }
    }
}