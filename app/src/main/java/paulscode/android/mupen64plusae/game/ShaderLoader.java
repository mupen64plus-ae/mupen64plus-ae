package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.mupen64plusae.v3.alpha.R;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public enum ShaderLoader {
    DEFAULT("default", R.string.shadersNone_title, R.string.shadersNone_summary, false),
    SCANLINES_SINE_ABS("scanlines-sine-abs", R.string.shadersScanlines_title, R.string.shadersScanlines_summary, false),
    TEST_PATTERN("test-pattern", R.string.shadersTestPattern_title, R.string.shadersTestPattern_summary, false),
    BLUR9X9("blur9x9", R.string.shadersBlur9x9_title, R.string.shadersBlur9x9_summary, false),
    N64_DITHER("n64-dither", R.string.shadersN64Dither_title, R.string.shadersN64Dither_summary, false),
    FXAA("fxaa", R.string.shadersFxaa_title, R.string.shadersFxaa_summary, false),
    CTR_GEOM("crt-geom", R.string.shadersCrtGeom_title, R.string.shadersCrtGeom_summary, true);

    private final String mShaderName;
    private final int mFriendlyName;
    private final int mDescription;
    private final boolean mNeedsVsync;
    private String mVertCode = null;
    private String mFragCode = null;

    static final String TAG = "ShaderLoader";

    ShaderLoader(String shaderName, int friendlyName, int description, boolean needsVsync) {
        mShaderName = shaderName;
        mFriendlyName = friendlyName;
        mDescription = description;
        mNeedsVsync = needsVsync;
    }

    public String getName() {
        return mShaderName;
    }

    public int getFriendlyName() {
        return mFriendlyName;
    }

    public int getDescription() {
        return mDescription;
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

    public static boolean needsVsync(ArrayList<ShaderLoader> shaderList) {
        for(ShaderLoader shader : shaderList) {
            if (shader.mNeedsVsync) {
                return true;
            }
        }

        return false;
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