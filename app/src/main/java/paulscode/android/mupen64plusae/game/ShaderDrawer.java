package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;

import org.mupen64plusae.v3.alpha.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public class ShaderDrawer implements GLSurfaceView.Renderer {

    public class Square {

        private FloatBuffer verticesBuffer;
        private FloatBuffer textureBuffer;

        private float vertices[] = {
                -1f, -1f,
                1f, -1f,
                -1f, 1f,
                1f, 1f,
        };

        private float textureVertices[] = {
                0f,1f,
                1f,1f,
                0f,0f,
                1f,0f
        };

        private final String vertexShaderCode =
                "attribute vec4 aPosition;" +
                        "attribute vec2 aTexPosition;" +
                        "varying vec2 vTexPosition;" +
                        "void main() {" +
                        "  gl_Position = aPosition;" +
                        "  vTexPosition = aTexPosition;" +
                        "}";
/*
        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform sampler2D uTexture;" +
                        "varying vec2 vTexPosition;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(uTexture, vTexPosition);" +
                        "}";
 */
       /*
          vec4 Color = texture2D( gm_BaseTexture, v_vTexcoord );
    vec3 lum = vec3(0.299, 0.587, 0.114);
    gl_FragColor = vec4( vec3(dot( Color.rgb, lum)), Color.a);
        */
        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform sampler2D uTexture;" +
                        "varying vec2 vTexPosition;" +
                        "void main() {" +
                        " vec4 color = texture2D(uTexture, vTexPosition);" +
                        " vec3 lum = vec3(0.299, 0.587, 0.114);" +
                        "  gl_FragColor = vec4( vec3(dot( color.rgb, lum)), color.a);" +
                        "}";


        private int vertexShader;
        private int fragmentShader;
        private int program;

        public Square(){
            initializeBuffers();
            initializeProgram();
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

        private void initializeProgram(){
            vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(vertexShader, vertexShaderCode);
            GLES20.glCompileShader(vertexShader);

            fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
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

            int positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
            int textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
            int texturePositionHandle = GLES20.glGetAttribLocation(program, "aTexPosition");

            GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
            GLES20.glEnableVertexAttribArray(texturePositionHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
            GLES20.glUniform1i(textureHandle, 0);

            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, verticesBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }
    }

    private int textures[] = new int[2];
    private Square square;
    private TextureView mGameView;
    private Context mContext;
    private Bitmap mBitmap;
    private final Object syncObject = new Object();

    // Create a handler thread to offload the processing of the image.
    final HandlerThread handlerThread = new HandlerThread("PixelCopier");

    public ShaderDrawer(TextureView gameView, Context context){
        super();
        mGameView = gameView;
        mContext = context;
        handlerThread.start();
    }

    private void generateSquare(){
        GLES20.glGenTextures(2, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width, height);
        GLES20.glClearColor(0,0,0,1);

        generateSquare();

        mBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.dummy_screen);

        if (mBitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
        }
        square = new Square();
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        synchronized (syncObject) {
            if (mBitmap != null) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
                square.draw(textures[0]);
            }
        }
    }
/*
    void getBitMapFromSurfaceView() {
        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(mGameView.getWidth(), mGameView.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Make the request to copy.
        PixelCopy.request(mGameView, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {

                synchronized (syncObject) {
                    mBitmap = bitmap;
                }
            }
        }, new Handler(handlerThread.getLooper()));
    }
*/
    /*
    void test()
    {
        EGL10 test = (EGL10)EGLContext.getEGL();
        test.eglCreateWindowSurface()
    }

    */
}