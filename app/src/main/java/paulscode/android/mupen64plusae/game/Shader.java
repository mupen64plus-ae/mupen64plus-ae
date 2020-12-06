package paulscode.android.mupen64plusae.game;


import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Shader {

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
    private int mTextureTarget = GLES20.GL_TEXTURE_2D;

    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private int mRenderWidth = 0;
    private int mRenderHeight = 0;
    private int mFrameCount = 0;

    private final String mVertexCode;
    private final String mFragmentCode;

    public Shader(String vertexCode, String fragmentCode, boolean firstPass){
        mVertexCode = vertexCode;

        if (firstPass) {
            String builder = "#extension GL_OES_EGL_image_external : require\n" +
                    fragmentCode;
            fragmentCode = builder.replace("sampler2D ", "samplerExternalOES ");
            mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        }

        mFragmentCode = fragmentCode;
    }


    public int getTextureId() {
        return mTextureId;
    }

    public void setDimensions(int surfaceWidth, int surfaceHeight, int renderWidth, int renderHeight) {
        mSurfaceWidth = surfaceWidth;
        mSurfaceHeight = surfaceHeight;
        mRenderWidth = renderWidth;
        mRenderHeight = renderHeight;
    }

    public void initShader(){
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        GLES20.glBindTexture(mTextureTarget, mTextureId);

        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        initializeBuffers();
        initializeProgram(mVertexCode, mFragmentCode);
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

    public void draw(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glUseProgram(program);
        GLES20.glDisable(GLES20.GL_BLEND);

        int texturePositionHandle = GLES20.glGetAttribLocation(program, "TexCoord");
        int textureHandle = GLES20.glGetUniformLocation(program, "Texture");
        int textureSize = GLES20.glGetUniformLocation(program, "TextureSize");

        int positionHandle = GLES20.glGetAttribLocation(program, "VertexCoord");
        int inputSize = GLES20.glGetUniformLocation(program, "InputSize");
        int outputSize = GLES20.glGetUniformLocation(program, "OutputSize");
        int frameCount = GLES20.glGetUniformLocation(program, "FrameCount");

        GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(texturePositionHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, mTextureId);
        GLES20.glUniform1i(textureHandle, 0);
        GLES20.glUniform1i(frameCount, mFrameCount);

        // Normalize to 480 pixel height
        GLES20.glUniform2f(textureSize, (float)mRenderWidth, (float)mRenderHeight);
        GLES20.glUniform2f(inputSize, (float)mRenderWidth, (float)mRenderHeight);
        GLES20.glUniform2f(outputSize, (float)mSurfaceWidth, (float)mSurfaceHeight);

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, verticesBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        ++mFrameCount;
    }
}