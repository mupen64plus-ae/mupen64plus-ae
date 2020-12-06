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

    private FloatBuffer verticesBuffer;
    private FloatBuffer textureBuffer;

    private final float[] vertices = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
    };

    private final float[] textureVertices = {
            0f,1f,
            1f,1f,
            0f,0f,
            1f,0f
    };

    private int program;
    private int mTextureId;
    private final int mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

    private static final String TAG = "ShaderDrawer";
    private SurfaceTexture mGameTexture;
    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private int mRenderWidth = 0;
    private int mRenderHeight = 0;
    private int mFrameCount = 0;
    private final ShaderLoader mSelectedShader;

    public ShaderDrawer(Context context){
        super();
        ShaderLoader.loadShaders(context);
        mSelectedShader = ShaderLoader.CTR_GEOM;
    }

    private void generateShaderTexture(){
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        GLES20.glBindTexture(mTextureTarget, mTextureId);

        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    public void onSurfaceTextureAvailable(PixelBuffer.SurfaceTextureWithSize surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable");

        if (mGameTexture == null) {
            Log.i(TAG, "Texture available, surface=" + width + "x" + height +
                    " render=" + surface.mWidth + "x" + surface.mHeight);

            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mRenderWidth = surface.mWidth;
            mRenderHeight = surface.mHeight;

            GLES20.glViewport(0, 0, mRenderWidth*3, mRenderHeight*3);
            GLES20.glClearColor(0, 0, 0, 1);

            generateShaderTexture();

            mGameTexture = surface.mSurfaceTexture;
            mGameTexture.attachToGLContext(mTextureId);
            // For some reason this is needed, otherwise frame callbacks stop happening on orientation
            // changes or if the app is put on the background then foreground again
            mGameTexture.updateTexImage();

            loadShaders();
        }
    }

    public void onSurfaceTextureDestroyed() {
        Log.i(TAG, "onSurfaceTextureDestroyed");

        if (mGameTexture != null) {
            Log.i(TAG, "Dettaching texture");

            mGameTexture.detachFromGLContext();
            mGameTexture = null;
        }

        int[] textures = new int[1];
        textures[0] = mTextureId;
        GLES20.glDeleteTextures(1, textures, 0);
    }

    public void onDrawFrame() {
        if (mGameTexture != null) {
            mGameTexture.updateTexImage();

            draw(mTextureId, mFrameCount);
            ++mFrameCount;
        }
    }

    private void loadShaders(){

        initializeBuffers();
        initializeProgram(mSelectedShader.getVertCode(), mSelectedShader.getFragCode());
    }

    private void initializeBuffers() {
        ByteBuffer buff = ByteBuffer.allocateDirect(vertices.length * 4);
        buff.order(ByteOrder.nativeOrder());
        verticesBuffer = buff.asFloatBuffer();
        verticesBuffer.put(vertices);
        verticesBuffer.position(0);

        buff = ByteBuffer.allocateDirect(textureVertices.length * 4);
        buff.order(ByteOrder.nativeOrder());
        textureBuffer = buff.asFloatBuffer();
        textureBuffer.put(textureVertices);
        textureBuffer.position(0);
    }

    private void initializeProgram(String vertexShaderText, String fragmentShaderText){
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderText);
        GLES20.glCompileShader(vertexShader);

        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShaderText);
        GLES20.glCompileShader(fragmentShader);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        GLES20.glLinkProgram(program);
    }

    public void draw(int texture, int frameCountValue){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glUseProgram(program);
        GLES20.glDisable(GLES20.GL_BLEND);

        int positionHandle = GLES20.glGetAttribLocation(program, "VertexCoord");
        int texturePositionHandle = GLES20.glGetAttribLocation(program, "TexCoord");
        int textureHandle = GLES20.glGetUniformLocation(program, "Texture");
        int textureSize = GLES20.glGetUniformLocation(program, "TextureSize");
        int inputSize = GLES20.glGetUniformLocation(program, "InputSize");
        int outputSize = GLES20.glGetUniformLocation(program, "OutputSize");
        int frameCount = GLES20.glGetUniformLocation(program, "FrameCount");

        GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(texturePositionHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, texture);
        GLES20.glUniform1i(textureHandle, 0);
        GLES20.glUniform1i(frameCount, frameCountValue);

        // Normalize to 480 pixel height
        GLES20.glUniform2f(textureSize, (float)mRenderWidth, (float)mRenderHeight);
        GLES20.glUniform2f(inputSize, (float)mRenderWidth, (float)mRenderHeight);
        GLES20.glUniform2f(outputSize, (float)mSurfaceWidth, (float)mSurfaceHeight);

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, verticesBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}