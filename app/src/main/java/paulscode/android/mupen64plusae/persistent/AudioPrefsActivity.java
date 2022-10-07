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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

import static paulscode.android.mupen64plusae.persistent.GlobalPrefs.AUDIO_SAMPLING_TYPE;

public class AudioPrefsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener {
    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    // User preferences
    private SharedPreferences mPrefs = null;

    @Override
    protected void attachBaseContext(Context newBase) {
        if (TextUtils.isEmpty(LocaleContextWrapper.getLocalCode())) {
            super.attachBaseContext(newBase);
        } else {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase, LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);
    }

    @Override
    protected String getSharedPrefsName() {
        return null;
    }

    @Override
    protected int getSharedPrefsId()
    {
        return R.xml.preferences_audio;
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
        super.onResume();

        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        // Just refresh the preference screens in place
        refreshViews();
    }

    private void refreshViews()
    {
        // Refresh the preferences object
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        // Enable audio prefs if audio is enabled
        PrefUtil.enablePreference(this, AUDIO_SAMPLING_TYPE, !mGlobalPrefs.enableAudioTimeSretching);
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        refreshViews();
    }
}
