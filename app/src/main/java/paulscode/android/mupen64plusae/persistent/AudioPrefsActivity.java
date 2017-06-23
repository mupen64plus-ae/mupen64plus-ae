/**
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
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class AudioPrefsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String AUDIO_SLES_TIME_STRETCH = "audioSLESTimeStretch";
    private static final String AUDIO_SLES_BUFFER_NBR = "audioSLESBufferNbr2";
    private static final String AUDIO_SLES_FLOATING_POINT = "audioSLESFloatingPoint";
    private static final String AUDIO_SYNCHRONIZE = "audioSynchronize";
    private static final String AUDIO_SWAP_CHANNELS = "audioSwapChannels";

    private static final String AUDIO_SLES_PLUGIN = "libmupen64plus-audio-sles.so";

    private static final String ROOT = "screenRoot";


    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private SharedPreferences mPrefs = null;

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
        super.onCreate(savedInstanceState);

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource(null, R.xml.preferences_audio);

        // Refresh the preference data wrapper
        mGlobalPrefs = new GlobalPrefs(this, mAppData);
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
        PrefUtil.enablePreference(this, AUDIO_SLES_TIME_STRETCH, mGlobalPrefs.audioPlugin.name.equals(AUDIO_SLES_PLUGIN));
        PrefUtil.enablePreference(this, AUDIO_SLES_BUFFER_NBR, mGlobalPrefs.audioPlugin.name.equals(AUDIO_SLES_PLUGIN) && mGlobalPrefs.enableSLESAudioTimeSretching);
        PrefUtil.enablePreference(this, AUDIO_SLES_FLOATING_POINT, mGlobalPrefs.audioPlugin.name.equals( AUDIO_SLES_PLUGIN ) );
        PrefUtil.enablePreference(this, AUDIO_SYNCHRONIZE, mGlobalPrefs.audioPlugin.enabled);
        PrefUtil.enablePreference(this, AUDIO_SWAP_CHANNELS, mGlobalPrefs.audioPlugin.enabled);

        if(!AppData.IS_LOLLIPOP)
        {
            PrefUtil.removePreference(this, ROOT, AUDIO_SLES_FLOATING_POINT);
        }
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        refreshViews();
    }
}
