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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.bda.controller.Controller;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog.PromptInputCodeListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.preference.ProfilePreference;

public class InputPrefsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener,
    PromptInputCodeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String NAVIGATION_MODE = "navigationMode";

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private SharedPreferences mPrefs = null;

    // MOGA controller interface
    private final Controller mMogaController = Controller.getInstance(this);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        mGlobalPrefs.enforceLocale(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init(mMogaController, this);

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource(null, R.xml.preferences_input);

        // Refresh the preference data wrapper
        mGlobalPrefs = new GlobalPrefs(this, mAppData);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        mMogaController.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mMogaController.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mMogaController.exit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals(NAVIGATION_MODE))
        {
            // Sometimes one preference change affects the hierarchy or layout
            // of the views. In this
            // case it's easier just to restart the activity than try to figure
            // out what to fix.
            ActivityHelper.restartActivity(this);
        }
        else
        {
            // Just refresh the preference screens in place
            refreshViews();
        }
    }

    private void refreshViews()
    {
        // Refresh the preferences object
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        // Get and update the controller profile information
        final ProfilePreference controllerProfile1 = (ProfilePreference) findPreference(GamePrefs.CONTROLLER_PROFILE1);
        final ProfilePreference controllerProfile2 = (ProfilePreference) findPreference(GamePrefs.CONTROLLER_PROFILE2);
        final ProfilePreference controllerProfile3 = (ProfilePreference) findPreference(GamePrefs.CONTROLLER_PROFILE3);
        final ProfilePreference controllerProfile4 = (ProfilePreference) findPreference(GamePrefs.CONTROLLER_PROFILE4);

        if (controllerProfile1 != null)
        {
            controllerProfile1.populateProfiles(mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), false, GlobalPrefs.DEFAULT_CONTROLLER_PROFILE_DEFAULT, null,
                    mGlobalPrefs.showBuiltInControllerProfiles);
            controllerProfile1.setSummary(controllerProfile1.getCurrentValue(null));
        }

        if (controllerProfile2 != null)
        {
            controllerProfile2.populateProfiles(mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), false, null, null,
                    mGlobalPrefs.showBuiltInControllerProfiles);
            controllerProfile2.setSummary(controllerProfile2.getCurrentValue(null));
        }

        if (controllerProfile3 != null)
        {
            controllerProfile3.populateProfiles(mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), false, null, null,
                    mGlobalPrefs.showBuiltInControllerProfiles);
            controllerProfile3.setSummary(controllerProfile3.getCurrentValue(null));
        }

        if (controllerProfile4 != null)
        {
            controllerProfile4.populateProfiles(mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), false, null, null,
                    mGlobalPrefs.showBuiltInControllerProfiles);
            controllerProfile4.setSummary(controllerProfile4.getCurrentValue(null));
        }

        // Enable/disable player map item as necessary
        PrefUtil.enablePreference(this, GamePrefs.PLAYER_MAP, mGlobalPrefs.playerMap.isEnabled());

        // Define which buttons to show in player map dialog
        final PlayerMapPreference playerPref = (PlayerMapPreference) findPreference(GamePrefs.PLAYER_MAP);
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
        final PlayerMapPreference playerPref = (PlayerMapPreference) findPreference(GamePrefs.PLAYER_MAP);
        playerPref.onDialogClosed(inputCode, hardwareId, which);
    }

    @Override
    public Controller getMogaController()
    {
        return mMogaController;
    }
}
