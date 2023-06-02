package paulscode.android.mupen64plusae_mpn.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import paulscode.android.mupen64plusae_mpn.util.PixelBuffer;

public class ShaderDrawer {

    private static final String TAG = "ShaderDrawer";
    private SurfaceTexture mGameTexture;
    private final ArrayList<ArrayList<Shader>> mShaderPasses = new ArrayList<>();
    private int mWidth = 0;
    private int mHeight = 0;

    public ShaderDrawer(Context context, ArrayList<ShaderLoader> selectedShaders) {
        ShaderLoader.loadShaders(context);

        for (int index = 0; index < selectedShaders.size(); ++ index) {
            boolean first = index == 0;
            boolean last = index == selectedShaders.size() - 1;

            mShaderPasses.add(new ArrayList<>());
            for (int shaderCodeIndex = 0; shaderCodeIndex < selectedShaders.get(index).getShaderCode().size(); ++shaderCodeIndex) {

                boolean actualLast = last && shaderCodeIndex == selectedShaders.get(index).getShaderCode().size() - 1;
                mShaderPasses.get(index).add(new Shader(selectedShaders.get(index).getShaderCode().get(shaderCodeIndex), first, actualLast,
                        index == 0, shaderCodeIndex));
                first = false;
            }
        }

        if (mShaderPasses.size() == 0) {
            mShaderPasses.add(new ArrayList<>());
            mShaderPasses.get(0).add(new Shader(ShaderLoader.DEFAULT.getShaderCode().get(0), true, true, true, 0));
        }
    }

    public void onSurfaceTextureAvailable(PixelBuffer.SurfaceTextureWithSize surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable");

        if (mGameTexture == null) {
            Log.i(TAG, "Texture available, surface_final=" + width + "x" + height +
                    " orig_render=" + surface.mWidth + "x" + surface.mHeight);
            mWidth = width;
            mHeight = height;

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

            Shader.TexturePassResult prevResult = new Shader.TexturePassResult(texture, surface.mWidth, surface.mHeight,
                    surface.mWidth, surface.mHeight);

            for (int subPassIndex = 0; subPassIndex < mShaderPasses.size(); ++subPassIndex ) {

                ArrayList<Shader> shaderSubPasses = mShaderPasses.get(subPassIndex);
                ArrayList<Shader.TexturePassResult> texturePassResults = new ArrayList<>();

                for (int shaderIndex = 0; shaderIndex < shaderSubPasses.size(); ++shaderIndex) {
                    Shader shader = shaderSubPasses.get(shaderIndex);
                    shader.setSourceTexture(texture);

                    // Always scale at the last shader of the first subpass
                    if (subPassIndex == 0) {
                        if (shaderIndex == shaderSubPasses.size() - 1) {
                            Log.d("Shader", "subpass=" + subPassIndex + " shader=" + shaderIndex + " scale=yes");
                            shader.setDimensions(surface.mWidth, surface.mHeight, surface.mWidth, surface.mHeight, width, height);
                        } else {
                            Log.d("Shader", "subpass=" + subPassIndex + " shader=" + shaderIndex + " scale=no");
                            shader.setDimensions(surface.mWidth, surface.mHeight, surface.mWidth, surface.mHeight, surface.mWidth, surface.mHeight);
                        }
                    } else {
                        Log.d("Shader", "subpass=" + subPassIndex + " shader=" + shaderIndex + " scale=already");
                        shader.setDimensions(surface.mWidth, surface.mHeight, width, height, width, height);
                    }

                    texturePassResults.add(0, prevResult);

                    shader.initShader();
                    shader.setShaderSubPasses(new ArrayList<>(texturePassResults));
                    texture = shader.getFboTextureId();
                    prevResult = shader.getTexturePassResult();
                }
            }
        }
    }

    public void onSurfaceTextureDestroyed() {
        Log.i(TAG, "onSurfaceTextureDestroyed");

        if (mGameTexture != null) {
            Log.i(TAG, "Dettaching texture");

            try {
                mGameTexture.detachFromGLContext();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            mGameTexture = null;
        }
    }

    public Bitmap getScreenShot() {

        if (mWidth <= 0 || mHeight <= 0) {
            return null;
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(mWidth * mHeight * 4);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

        // Fix alpha to 255 for all pixels, some plugins don't ever set alpha values
        for (int bufferIndex = 3; bufferIndex < buffer.array().length; bufferIndex+=4) {
            buffer.array()[bufferIndex] = -1;
        }

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

            for (ArrayList<Shader> shaderSubPasses : mShaderPasses) {
                for (Shader shader : shaderSubPasses) {
                    shader.draw();
                }
            }
        }
    }
}