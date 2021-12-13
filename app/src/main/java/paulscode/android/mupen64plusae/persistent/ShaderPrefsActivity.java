/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 *
 * Copyright (C) 2013 Paul Lamb
 *
 * This file is part of Mupen64PlusAE.
 *
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import org.mupen64plusae.v3.alpha.R;

import java.util.ArrayList;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.game.ShaderLoader;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.preference.ShaderPreference;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class ShaderPrefsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, ShaderPreference.OnRemove {
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String CATEGORY_PASSES = "categoryShaderPasses";
    private static final String ADD_PREFERENCE = "addShader";
    private static final String SHADER_PASS_KEY = "shaderpass,";

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private SharedPreferences mPrefs = null;
    private PreferenceGroup mCategoryPasses = null;

    static final int MAX_SHADER_PASSES = 5;

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i("Shader", "onCreate");

        super.onCreate(savedInstanceState);

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource(null, R.xml.preferences_shader);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume()
    {
        Log.i("Shader", "onResume");

        super.onResume();
        // Just refresh the preference screens in place
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        Log.i("Shader", "onSharedPreferenceChanged");

        ArrayList<ShaderLoader> shaderPasses = mGlobalPrefs.getShaderPasses();

        if (key.startsWith(SHADER_PASS_KEY)) {
            String value = mPrefs.getString(key, ShaderLoader.DEFAULT.toString());

            String[] currentPassSplitString = key.split(",");

            if (currentPassSplitString.length == 2) {
                ShaderLoader valueEnum;
                try {
                    valueEnum = ShaderLoader.valueOf(value);
                } catch (java.lang.IllegalArgumentException e) {
                    valueEnum = null;
                }

                int changedPass = Integer.parseInt(currentPassSplitString[1]) - 1;

                if (changedPass >= 0 && changedPass < shaderPasses.size() && valueEnum != null) {
                    shaderPasses.set(changedPass, valueEnum);
                    mGlobalPrefs.putShaderPasses(shaderPasses);

                    refreshViews();
                }
            }
        }
    }

    private void refreshViews()
    {
        Log.i("Shader", "refreshViews");

        // Refresh the preferences object
        mGlobalPrefs = new GlobalPrefs(this, mAppData);
        PreferenceGroup screenRoot = (PreferenceGroup) findPreference(SCREEN_ROOT);
        PreferenceGroup categoryPasses = (PreferenceGroup) findPreference(CATEGORY_PASSES);

        if (mCategoryPasses != null) {
            mCategoryPasses.removeAll();
        }

        ArrayList<ShaderLoader> shaderPasses = mGlobalPrefs.getShaderPasses();

        for (int index = 0; index < shaderPasses.size(); ++index) {
            addShaderPass(shaderPasses.get(index), index + 1);
        }

        // If there are no shaders, then remove the category
        if (mCategoryPasses != null) {
            if (shaderPasses.isEmpty()) {
                screenRoot.removePreference(mCategoryPasses);
            } else if (categoryPasses == null) {
                screenRoot.addPreference(mCategoryPasses);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Only when in game
        if(this.getIntent().getBooleanExtra("gameRunning",false)) {
            Intent intent = new Intent();

            ArrayList<ShaderLoader> shaderPasses = mGlobalPrefs.getShaderPasses();
            StringBuilder sb = new StringBuilder();

            for (ShaderLoader shaderPass : shaderPasses) {
                sb.append(shaderPass.toString()).append(",");
            }

            intent.putExtra(GlobalPrefs.KEY_SHADER_PASS, sb.toString());
            intent.putExtra("shaderScaleFactor",mGlobalPrefs.shaderScaleFactor);


            setResult(RESULT_OK, intent);
        }
        super.onBackPressed();
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        Log.i("Shader", "OnPreferenceScreenChange");
        mCategoryPasses = (PreferenceGroup) findPreference( CATEGORY_PASSES );

        if (mCategoryPasses == null) {
            resetPreferences();
        } else {
            PrefUtil.setOnPreferenceClickListener(this, ADD_PREFERENCE, this);
            refreshViews();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        // Handle the clicks on certain menu items that aren't actually
        // preferences
        final String key = preference.getKey();

        if (ADD_PREFERENCE.equals(key)) {
            ArrayList<ShaderLoader> shaderPasses = mGlobalPrefs.getShaderPasses();

            if (shaderPasses.size() < MAX_SHADER_PASSES) {
                shaderPasses.add(ShaderLoader.DEFAULT);
                mGlobalPrefs.putShaderPasses(shaderPasses);
                refreshViews();
            }
        }

        // Tell Android that we handled the click
        return true;
    }

    public void addShaderPass(ShaderLoader shader, int shaderPass) {
        if (mCategoryPasses != null) {
            ShaderPreference preference = new ShaderPreference(getPreferenceManagerContext());
            String key = SHADER_PASS_KEY + shaderPass;
            preference.setKey(key);
            preference.populateShaderOptions(this);
            String title = getString(R.string.shadersPass_title) + " " + shaderPass;
            preference.setTitle(title);
            preference.setSummary(shader.getFriendlyName());
            preference.setValue(shader.toString());
            preference.setOnRemoveCallback(this);

            mCategoryPasses.addPreference(preference);
        }
    }

    @Override
    public void onRemove(String key) {
        String[] currentPassSplitString = key.split(",");

        if (currentPassSplitString.length == 2) {
            ArrayList<ShaderLoader> shaderPasses = mGlobalPrefs.getShaderPasses();
            int changedPass = Integer.parseInt(currentPassSplitString[1]) - 1;

            if (changedPass >= 0 && changedPass < shaderPasses.size()) {
                shaderPasses.remove(changedPass);
                mPrefs.edit().remove(key).apply();
                mGlobalPrefs.putShaderPasses(shaderPasses);

                refreshViews();
            }
        }
    }
}
