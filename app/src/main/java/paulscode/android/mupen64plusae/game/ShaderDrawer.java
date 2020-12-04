package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL11;

public class ShaderDrawer {

    public class Square {

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

        private final String defaultVertexShaderCode =
                "attribute vec4 VertexCoord;\n" +
                "attribute vec4 TexCoord;\n" +
                "varying vec2 vTexPosition;\n" +
                "void main() {\n" +
                "  gl_Position = VertexCoord;\n" +
                "  vTexPosition = TexCoord.xy;\n" +
                "}\n";

        private final String defaultFragmentShaderCode =
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES Texture;\n" +
                "varying vec2 vTexPosition;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(Texture, vTexPosition);\n" +
                "}\n";

        private int program;

        public Square(Context context){

            //String shaderName = "test-pattern";
            String shaderName = "scanlines-sine-abs";

            String vertexShader = null;
            String fragmentShader = null;

            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("mupen64plus_data/shaders/" + shaderName + "_vert.glsl"))) {
                vertexShader = IOUtils.toString(reader);

            } catch (IOException|NullPointerException e) {
                e.printStackTrace();
                Log.e(TAG, "Can't find vertex shader");
            }

            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("mupen64plus_data/shaders/" + shaderName + "_frag.glsl"))) {
                fragmentShader = IOUtils.toString(reader);
            } catch (IOException|NullPointerException e) {
                e.printStackTrace();
                Log.e(TAG, "Can't find fragment shader");
            }

            if (vertexShader == null || fragmentShader == null) {
                vertexShader = defaultVertexShaderCode;
                fragmentShader = defaultFragmentShaderCode;
            }

            initializeBuffers();
            initializeProgram(vertexShader, fragmentShader);
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

        public void draw(int texture){
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glUseProgram(program);
            GLES20.glDisable(GLES20.GL_BLEND);

            int positionHandle = GLES20.glGetAttribLocation(program, "VertexCoord");
            int textureHandle = GLES20.glGetUniformLocation(program, "Texture");
            int textureSize = GLES20.glGetUniformLocation(program, "TextureSize");
            int texturePositionHandle = GLES20.glGetAttribLocation(program, "TexCoord");

            GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
            GLES20.glEnableVertexAttribArray(texturePositionHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(mTextureTarget, texture);
            GLES20.glUniform1i(textureHandle, 0);

            // Normalize to 480 pixel height
            float scale = mSurfaceHeight/480.0f;
            GLES20.glUniform2f(textureSize, (float)mSurfaceWidth/scale, (float)mSurfaceHeight/scale);

            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, verticesBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }
    }
    private int mTextureId;
    private Square square;
    private final int mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

    private static final String TAG = "ShaderDrawer";
    private SurfaceTexture mGameTexture;
    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private final Context mContext;

    public ShaderDrawer(Context context){
        super();
        mContext = context;
    }

    private void generateSquare(){
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        GLES20.glBindTexture(mTextureTarget, mTextureId);

        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable");

        if (mGameTexture == null) {
            Log.i(TAG, "Texture available, width=" + width + " height=" + height);

            mSurfaceWidth = width;
            mSurfaceHeight = height;

            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            GLES20.glClearColor(0, 0, 0, 1);

            generateSquare();

            mGameTexture = surface;
            mGameTexture.attachToGLContext(mTextureId);
            // For some reason this is needed, otherwise frame callbacks stop happening on orientation
            // changes or if the app is put on the background then foreground again
            mGameTexture.updateTexImage();

            square = new Square(mContext);
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

            if (square != null) {
                square.draw(mTextureId);
            }
        }
    }
}