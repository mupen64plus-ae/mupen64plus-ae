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

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceManager;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.preference.PrefUtil;

public class DataPrefsActivity extends AppCompatPreferenceActivity implements OnPreferenceClickListener,
    PromptConfirmListener, SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final int RESET_GLOBAL_PREFS_CONFIRM_DIALOG_ID = 0;
    private static final String RESET_GLOBAL_PREFS_CONFIRM_DIALOG_STATE = "RESET_GLOBAL_PREFS_CONFIRM_DIALOG_STATE";

    // These constants must match the keys used in res/xml/preferences.xml
    private static final String PATH_GAME_SAVES = "pathGameSaves";
    private static final String PATH_APP_DATA = "pathAppData";
    private static final String ACTION_RELOAD_ASSETS = "actionReloadAssets";
    private static final String ACTION_RESET_USER_PREFS = "actionResetUserPrefs";

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private SharedPreferences mPrefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        mGlobalPrefs.enforceLocale(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource(null, R.xml.preferences_data);

        // Refresh the preference data wrapper
        mGlobalPrefs = new GlobalPrefs(this, mAppData);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        // Handle the clicks on certain menu items that aren't actually
        // preferences
        final String key = preference.getKey();

        if (key.equals(ACTION_RELOAD_ASSETS))
        {
            actionReloadAssets();
        }
        else if (key.equals(ACTION_RESET_USER_PREFS))
        {
            actionResetUserPrefs();
        }
        else if (key.equals(PATH_GAME_SAVES))
        {
            return false;
        }
        else if (key.equals(PATH_APP_DATA))
        {
            return false;
        }
        else
            // Let Android handle all other preference clicks
            return false;

        // Tell Android that we handled the click
        return true;
    }

    private void actionReloadAssets()
    {
        mAppData.putAssetVersion(0);
        ActivityHelper.startSplashActivity(this);
        finish();
    }

    private void actionResetUserPrefs()
    {
        final String title = getString(R.string.confirm_title);
        final String message = getString(R.string.actionResetUserPrefs_popupMessage);

        final ConfirmationDialog confirmationDialog = ConfirmationDialog.newInstance(
            RESET_GLOBAL_PREFS_CONFIRM_DIALOG_ID, title, message);

        final FragmentManager fm = getSupportFragmentManager();
        confirmationDialog.show(fm, RESET_GLOBAL_PREFS_CONFIRM_DIALOG_STATE);
    }

    @Override
    public void onPromptDialogClosed(int id, int which)
    {
        if (id == RESET_GLOBAL_PREFS_CONFIRM_DIALOG_ID && which == DialogInterface.BUTTON_POSITIVE)
        {
            // Reset the user preferences
            mPrefs.edit().clear().commit();
            PreferenceManager.setDefaultValues(DataPrefsActivity.this, R.xml.preferences_data, true);

            // Rebuild the menu system by restarting the activity
            ActivityHelper.restartActivity(DataPrefsActivity.this);
        }
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        // Handle certain menu items that require extra processing or aren't
        // actually preferences
        PrefUtil.setOnPreferenceClickListener(this, ACTION_RELOAD_ASSETS, this);
        PrefUtil.setOnPreferenceClickListener(this, ACTION_RESET_USER_PREFS, this);
        PrefUtil.setOnPreferenceClickListener(this, PATH_GAME_SAVES, this);
        PrefUtil.setOnPreferenceClickListener(this, PATH_APP_DATA, this);
    }

    @TargetApi( 16 )
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        if(key.equals(PATH_APP_DATA))
        {
            //Force reload of assets
            mAppData.putAssetVersion( 0 );
            ActivityHelper.startSplashActivity(this);

            if( AppData.IS_JELLY_BEAN)
            {
                finishAffinity();
            }
        }
    }
}
