package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;

public enum ShaderLoader {
    DEFAULT("default"),
    SCANLINES_SINE_ABS("scanlines-sine-abs"),
    TEST_PATTERN("test-pattern"),
    BLUR9X9("blur9x9"),
    N64_DITHER("n64-dither"),
    FXAA("fxaa"),
    CTR_GEOM("crt-geom");

    private final String mShaderName;
    private String mVertCode = null;
    private String mFragCode = null;

    static final String TAG = "ShaderLoader";

    ShaderLoader(String shaderName) {
        mShaderName = shaderName;
    }

    private void setVertCode(String vertCode) {
        mVertCode = vertCode;
    }

    private void setFragCode(String fragCode) {
        mFragCode = fragCode;
    }

    public String getVertCode() {
        return mVertCode;
    }

    public String getFragCode() {
        return mFragCode;
    }

    public static void loadShaders(Context context){

        for (ShaderLoader shader : ShaderLoader.values()) {
            String vertexShader = null;
            String fragmentShader = null;

            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("mupen64plus_data/shaders/" + shader.mShaderName + "_vert.glsl"))) {
                vertexShader = IOUtils.toString(reader);

            } catch (IOException|NullPointerException e) {
                e.printStackTrace();
                Log.e(TAG, "Can't find vertex shader");
            }

            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("mupen64plus_data/shaders/" + shader.mShaderName + "_frag.glsl"))) {
                fragmentShader = IOUtils.toString(reader);
            } catch (IOException|NullPointerException e) {
                e.printStackTrace();
                Log.e(TAG, "Can't find fragment shader");
            }

            if (vertexShader != null || fragmentShader != null) {
                shader.setVertCode(vertexShader);
                shader.setFragCode(fragmentShader);
            }
        }
    }
}