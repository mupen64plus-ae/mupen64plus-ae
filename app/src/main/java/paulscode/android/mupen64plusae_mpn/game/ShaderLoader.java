package paulscode.android.mupen64plusae_mpn.game;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.mupen64plusae_mpn.v3.alpha.R;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

public enum ShaderLoader {
    DEFAULT(R.string.shadersNone_title, R.string.shadersNone_summary, false, "default"),
    SCANLINES_SINE_ABS(R.string.shadersScanlines_title, R.string.shadersScanlines_summary, false, "scanlines-sine-abs"),
    TEST_PATTERN(R.string.shadersTestPattern_title, R.string.shadersTestPattern_summary, false, "test-pattern"),
    BLUR9X9(R.string.shadersBlur9x9_title, R.string.shadersBlur9x9_summary, false, "blur9x9"),
    N64_DITHER(R.string.shadersN64Dither_title, R.string.shadersN64Dither_summary, false, "n64-dither"),
    FXAA(R.string.shadersFxaa_title, R.string.shadersFxaa_summary, false, "fxaa"),
    CTR_GEOM(R.string.shadersCrtGeom_title, R.string.shadersCrtGeom_summary, true, "crt-geom"),
    SCALEFX(R.string.shadersScaleFx_title, R.string.shadersScaleFx_summary, false,
            "default",
            "scalefx-pass0",
            "scalefx-pass1",
            "scalefx-pass2",
            "scalefx-pass3",
            "scalefx-pass4"
    );

    private final ArrayList<String> mShaderNames = new ArrayList<>();
    private final int mFriendlyName;
    private final int mDescription;
    private final boolean mNeedsVsync;
    private final ArrayList<String> mShaderCode = new ArrayList<>();

    static final String TAG = "ShaderLoader";

    ShaderLoader(int friendlyName, int description, boolean needsVsync, String ... shaderNameList ) {

        Collections.addAll(mShaderNames, shaderNameList);
        mFriendlyName = friendlyName;
        mDescription = description;
        mNeedsVsync = needsVsync;
    }

    public ArrayList<String> getNames() {
        return mShaderNames;
    }

    public int getFriendlyName() {
        return mFriendlyName;
    }

    public int getDescription() {
        return mDescription;
    }

    private void addShaderCode(String shaderCode) {
        mShaderCode.add(shaderCode);
    }

    public ArrayList<String> getShaderCode() {
        return mShaderCode;
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

            if (shader.getShaderCode().isEmpty()) {
                for (String shaderName : shader.mShaderNames) {
                    String shaderText = null;

                    try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("mupen64plus_data/shaders/" + shaderName + ".glsl"))) {
                        shaderText = IOUtils.toString(reader);
                    } catch (IOException|NullPointerException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Can't find shader");
                    }

                    if (shaderText != null) {
                        shader.addShaderCode(shaderText);
                    }
                }
            }
        }
    }
}