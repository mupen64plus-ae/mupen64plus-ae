/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: paulscode, lioncash, littleguy77
 */
package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class MenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String MENU_RESUME = "menuResume";
    private static final String MENU_RESET_USER_PREFS = "menuResetUserPrefs";
    private static final String MENU_RESET_APP_DATA = "menuResetAppData";
    private static final String MENU_DEVICE_INFO = "menuDeviceInfo";
    private static final String TOUCHSCREEN_CUSTOM = "touchscreenCustom";
    private static final String TOUCHSCREEN_SIZE = "touchscreenSize";
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences );
        
        // Update the global convenience class
        Globals.userPrefs = new UserPrefs( this, Globals.paths );
        
        // Define the click callback for certain menu items that aren't actually preferences
        findPreference( MENU_RESUME ).setOnPreferenceClickListener( this );
        findPreference( MENU_RESET_USER_PREFS ).setOnPreferenceClickListener( this );
        findPreference( MENU_RESET_APP_DATA ).setOnPreferenceClickListener( this );
        findPreference( MENU_DEVICE_INFO ).setOnPreferenceClickListener( this );
    }
    
    @Override
    protected void onResume()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        refreshViews( sharedPreferences );
        sharedPreferences.registerOnSharedPreferenceChangeListener( this );
        super.onResume();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences( this )
                .unregisterOnSharedPreferenceChangeListener( this );
    }
    
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        String key = preference.getKey();
        if( key.equals( MENU_RESUME ) )
        {
            // Launch the last game in a new activity
            
            // TODO: Localize toast string
            if( !Globals.paths.isSdCardAccessible() )
            {
                Log.e( "MenuActivity",
                        "SD Card not accessable in method onPreferenceClick (menuResume)" );
                Notifier.showToast(
                        "App data not accessible (cable plugged in \"USB Mass Storage Device\" mode?)",
                        this );
                return true;
            }
            
            // Launch the appropriate game activity
            Intent intent = Globals.userPrefs.isXperiaEnabled
                    ? new Intent( this, GameActivityXperiaPlay.class )
                    : new Intent( this, GameActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
            finish();
            return true;
        }
        else if( key.equals( MENU_RESET_USER_PREFS ) )
        {
            // Reset the user preferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( this );
            preferences.edit().clear().commit();
            PreferenceManager.setDefaultValues( this, R.xml.preferences, true );
            
            // Restart the activity so that the entire menu system is rebuilt
            // (OnSharedPreferenceChangedListener is not sufficient for this)
            finish();
            startActivity( getIntent() );
            return true;
        }
        else if( key.equals( MENU_RESET_APP_DATA ) )
        {
            // TODO: Add a confirmation dialog
            // Reset the application data
            Globals.appData.resetToDefaults();
            // Force user to restart app so stuff gets refreshed
            finish();
            return true;
        }
        else if( key.equals( MENU_DEVICE_INFO ) )
        {
            Builder builder = new Builder( this );
            builder.setTitle( this.getString( R.string.menuDeviceInfo_title ) );
            builder.setMessage( Utility.getCpuInfo() );
            builder.create().show();            
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        // Update the global convenience class
        Globals.userPrefs = new UserPrefs( this, Globals.paths );
        refreshViews( sharedPreferences );
    }

    @SuppressWarnings( "deprecation" )
    private void refreshViews( SharedPreferences sharedPreferences )
    {
        // Enable the play button only if the selected game actually exists
        findPreference( MENU_RESUME ).setEnabled(
                ( new File( Globals.userPrefs.lastGame ).exists() ) );
        
        // Enable the custom touchscreen prefs under certain conditions
        findPreference( TOUCHSCREEN_CUSTOM ).setEnabled(
                Globals.userPrefs.isTouchscreenEnabled && Globals.userPrefs.isTouchscreenCustom );
        findPreference( TOUCHSCREEN_SIZE ).setEnabled(
                Globals.userPrefs.isTouchscreenEnabled && !Globals.userPrefs.isTouchscreenCustom );
        
        // Update the summary text in the menu for all ListPreferences
        for( String key : sharedPreferences.getAll().keySet() )
        {
            Preference preference = findPreference( key );
            if( preference instanceof ListPreference )
                preference.setSummary( ( (ListPreference) preference ).getEntry() );
        }
    }
}
