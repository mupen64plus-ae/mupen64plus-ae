package paulscode.android.mupen64plusae_mpn.game;


import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

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

    private final float[] textureVerticesInverted = {
            0f,0f,
            1f,0f,
            0f,1f,
            1f,1f
    };

    private int program;
    private int mTextureId;
    private int mTextureTarget = GLES20.GL_TEXTURE_2D;
    private int mFboId = 0;
    private int mFboTextureId = 0;
    private final boolean mFirstPass;
    private final boolean mLastPass;
    private final int mShaderSubPass;
    private final boolean mFirstMultiPass;

    private int mOutputWidth = 0;
    private int mOutputHeight = 0;
    private int mInputWidth = 0;
    private int mInputHeight = 0;
    private int mTextureWidth = 0;
    private int mTextureHeight = 0;
    private int mFrameCount = 0;

    private final String mVertexCode;
    private final String mFragmentCode;

    private ArrayList<TexturePassResult> mTexturePassResults = null;

    public static class TexturePassResult {
        private final int fboTextureId;
        private final int fboWidth;
        private final int fboHeight;
        private final int inputWidth;
        private final int inputHeight;

        public TexturePassResult (int fboTextureId, int fboWidth, int fboHeight, int inputWidth, int inputHeight)
        {
            this.fboTextureId = fboTextureId;
            this.fboWidth = fboWidth;
            this.fboHeight = fboHeight;
            this.inputWidth = inputWidth;
            this.inputHeight = inputHeight;
        }
    }

    public Shader(String shaderCode, boolean firstPass, boolean lastPass, boolean firstMultiPass, int shaderSubPass)
    {
        mVertexCode = shaderCode.replaceAll("#version 100",
                "#version 100\n" +
                        "#define VERTEX 1\n");

        String fragmentCode = shaderCode.replaceAll("#version 100",
                "#version 100\n" +
                        "#define FRAGMENT 1\n");

        fragmentCode = fragmentCode.replaceAll("#define FRAGMENT 1",
                "#define FRAGMENT 1\n" +
                        "#extension GL_OES_EGL_image_external : require\n");

        if (firstPass) {
            fragmentCode = fragmentCode.replace("sampler2D Texture", "samplerExternalOES Texture");
            mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        }

        if (firstMultiPass) {
            fragmentCode = fragmentCode.replace("sampler2D PassPrev" + (shaderSubPass+1) + "Texture",
                    "samplerExternalOES PassPrev" + (shaderSubPass+1) + "Texture");
        }

        mFragmentCode = fragmentCode;
        mFirstPass = firstPass;
        mLastPass = lastPass;
        mShaderSubPass = shaderSubPass;
        mFirstMultiPass = firstMultiPass;
    }

    public int getFboTextureId()
    {
        return mFboTextureId;
    }

    public void setSourceTexture(int sourceTexture)
    {
        mTextureId = sourceTexture;
    }

    public TexturePassResult getTexturePassResult()
    {
        return new TexturePassResult(mFboTextureId, mOutputWidth, mOutputHeight, mInputWidth, mInputHeight);
    }

    public void setShaderSubPasses(ArrayList<TexturePassResult> texturePassResults)
    {
        mTexturePassResults = texturePassResults;
    }

    public void setDimensions(int inputWidth, int inputHeight, int textureWidth, int textureHeight,
                              int outputeWidth, int outputHeight)
    {
        mInputWidth = inputWidth;
        mInputHeight = inputHeight;
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
        mOutputWidth = outputeWidth;
        mOutputHeight = outputHeight;
    }

    public void initShader()
    {
        initializeBuffers();
        initializeProgram(mVertexCode, mFragmentCode);

        if (!mLastPass) {
            initializeFbo();
        }
    }

    private void initializeFbo()
    {
        int[] framebuffers = new int[1];
        GLES20.glGenFramebuffers(1, framebuffers, 0);
        mFboId = framebuffers[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);

        //Create a texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mFboTextureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTextureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mOutputWidth, mOutputHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        //Attach the texture to the framebuffer
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mFboTextureId, 0);

    }

    private void initializeBuffers()
    {
        ByteBuffer buff = ByteBuffer.allocateDirect(vertices.length * 4);
        buff.order(ByteOrder.nativeOrder());
        verticesBuffer = buff.asFloatBuffer();
        verticesBuffer.put(vertices);
        verticesBuffer.position(0);

        float[] textureVerticesFinal;

        if (mFirstPass) {
            textureVerticesFinal = textureVertices;
        } else {
            textureVerticesFinal = textureVerticesInverted;
        }

        buff = ByteBuffer.allocateDirect(textureVerticesFinal.length * 4);
        buff.order(ByteOrder.nativeOrder());
        textureBuffer = buff.asFloatBuffer();
        textureBuffer.put(textureVerticesFinal);
        textureBuffer.position(0);
    }

    private void initializeProgram(String vertexShaderText, String fragmentShaderText)
    {
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderText);
        GLES20.glCompileShader(vertexShader);

        int[] params = new int[1];
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, params, 0);
        if (params[0] == GLES20.GL_FALSE) {
            Log.e("Shader", "Vertex Compilation error:\n" + GLES20.glGetShaderInfoLog(vertexShader)
                    + "\n Shader code:\n" + vertexShaderText);
        }

        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShaderText);
        GLES20.glCompileShader(fragmentShader);

        GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, params, 0);
        if (params[0] == GLES20.GL_FALSE) {
            Log.e("Shader", "Fragment Compilation error:\n" + GLES20.glGetShaderInfoLog(fragmentShader)
                    + "\n Shader code:\n" + fragmentShaderText);
        }

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        GLES20.glLinkProgram(program);
    }

    public void draw()
    {
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
        GLES20.glClearColor(0, 0, 0, 1);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);

        if (!mLastPass) {
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mFboTextureId);
        }

        GLES20.glUseProgram(program);
        GLES20.glDisable(GLES20.GL_BLEND);

        int texturePositionHandle = GLES20.glGetAttribLocation(program, "TexCoord");
        int textureHandle = GLES20.glGetUniformLocation(program, "Texture");
        int textureSize = GLES20.glGetUniformLocation(program, "TextureSize");

        int positionHandle = GLES20.glGetAttribLocation(program, "VertexCoord");
        int inputSize = GLES20.glGetUniformLocation(program, "InputSize");
        int outputSize = GLES20.glGetUniformLocation(program, "OutputSize");
        int frameCount = GLES20.glGetUniformLocation(program, "FrameCount");

        ArrayList<Integer> prevInputSize = new ArrayList<>();
        ArrayList<Integer> prevTextureSize = new ArrayList<>();
        ArrayList<Integer> prevTexture = new ArrayList<>();

        for (int prevIndex = 0; prevIndex < mTexturePassResults.size(); ++prevIndex) {
            prevInputSize.add(GLES20.glGetUniformLocation(program, "PassPrev" + (prevIndex+1) + "InputSize"));
            prevTextureSize.add(GLES20.glGetUniformLocation(program, "PassPrev" + (prevIndex+1) + "TextureSize"));
            prevTexture.add(GLES20.glGetUniformLocation(program, "PassPrev" + (prevIndex+1) + "Texture"));
        }

        GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(texturePositionHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, mTextureId);
        GLES20.glUniform1i(textureHandle, 0);
        GLES20.glUniform1i(frameCount, mFrameCount);

        GLES20.glUniform2f(inputSize, (float) mInputWidth, (float) mInputHeight);
        GLES20.glUniform2f(textureSize, (float) mInputWidth, (float) mInputHeight);
        GLES20.glUniform2f(outputSize, (float) mOutputWidth, (float) mOutputHeight);

        for (int prevIndex = 0; prevIndex < mTexturePassResults.size(); ++prevIndex) {
            if (prevInputSize.get(prevIndex) != -1) {
                GLES20.glUniform2f(prevInputSize.get(prevIndex),
                        (float) mTexturePassResults.get(prevIndex).inputWidth,
                        (float) mTexturePassResults.get(prevIndex).inputHeight);
            }

            if (prevTextureSize.get(prevIndex) != -1) {
                GLES20.glUniform2f(prevTextureSize.get(prevIndex),
                        (float) mTexturePassResults.get(prevIndex).fboWidth,
                        (float) mTexturePassResults.get(prevIndex).fboHeight);
            }

            if (prevTexture.get(prevIndex) != -1) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + prevIndex + 1);
                int textureTarget = prevIndex == mShaderSubPass && mFirstMultiPass ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D;
                GLES20.glBindTexture(textureTarget, mTexturePassResults.get(prevIndex).fboTextureId);
                GLES20.glUniform1i(prevTexture.get(prevIndex), prevIndex + 1);
            }
        }

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, verticesBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        ++mFrameCount;
    }
}