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

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class InputPrefsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener,
        PromptInputCodeDialog.PromptInputCodeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String NAVIGATION_MODE = "navigationMode";

    // App data and user preferences
    private SharedPreferences mPrefs = null;
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

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

        mPrefs = ActivityHelper.getDefaultSharedPreferencesMultiProcess(this);

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource(null, R.xml.preferences_input);
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
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        // Just refresh the preference screens in place
        refreshViews();

        if (key.equals(NAVIGATION_MODE))
        {
            // Sometimes one preference change affects the hierarchy or layout
            // of the views. In this
            // case it's easier just to restart the activity than try to figure
            // out what to fix.
            ActivityHelper.restartActivity(this);
        }
    }

    private void refreshViews()
    {
        // Refresh the preferences object
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        // Enable/disable player map item as necessary
        PrefUtil.enablePreference(this, GlobalPrefs.PLAYER_MAP,
                !mGlobalPrefs.autoPlayerMapping && !mGlobalPrefs.isControllerShared);

        // Define which buttons to show in player map dialog
        final PlayerMapPreference playerPref = (PlayerMapPreference) findPreference(GlobalPrefs.PLAYER_MAP);
        if (playerPref != null)
        {
            // Check null in case preference has been removed
            final boolean enable1 = mGlobalPrefs.controllerProfile1 != null;
            final boolean enable2 = mGlobalPrefs.controllerProfile2 != null;
            final boolean enable3 = mGlobalPrefs.controllerProfile3 != null;
            final boolean enable4 = mGlobalPrefs.controllerProfile4 != null;
            playerPref.setControllersEnabled(enable1, enable2, enable3, enable4);
        }
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        refreshViews();
    }

    @Override
    public void onDialogClosed(int inputCode, int hardwareId, int which)
    {
        final PlayerMapPreference playerPref = (PlayerMapPreference) findPreference(GlobalPrefs.PLAYER_MAP);
        playerPref.onDialogClosed(hardwareId, which);
    }
}
