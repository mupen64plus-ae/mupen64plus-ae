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
 * Authors: paulscode, littleguy77
 */
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
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
    Preference mMenuResume;
    Preference mMenuResetPrefs;
    Preference mMenuResetAppData;
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences );
        
        // Update the global convenience class
        Globals.userPrefs = new UserPrefs( this );
        
        // Define the callback when the user presses certain menu items
        mMenuResume = findPreference( "menuResume" );
        mMenuResetPrefs = findPreference( "menuReset" );
        mMenuResetAppData = findPreference( "menuResetAppData" );
        mMenuResume.setOnPreferenceClickListener( this );
        mMenuResetPrefs.setOnPreferenceClickListener( this );
        mMenuResetAppData.setOnPreferenceClickListener( this );
    }
    
    @Override
    protected void onResume()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        refreshSummaries( sharedPreferences );
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
    
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        // Update the global convenience class
        Globals.userPrefs = new UserPrefs( this );
        refreshSummaries( sharedPreferences );
    }
    
    @SuppressWarnings( "deprecation" )
    private void refreshSummaries( SharedPreferences sharedPreferences )
    {
        // Update the summary text in the menu for all ListPreferences
        for( String key : sharedPreferences.getAll().keySet() )
        {
            Preference preference = findPreference( key );
            if( preference instanceof ListPreference )
            {
                preference.setSummary( ( (ListPreference) preference ).getEntry() );
            }
        }
    }
    
    public boolean onPreferenceClick( Preference preference )
    {
        if( preference == mMenuResume )
        {
            // Launch the last game in a new activity
            if( !Globals.path.isSdCardAccessible() )
            {
                Log.e( "MenuActivity", "SD Card not accessable in method onPreferenceClick (menuResume)" );
                Notifier.showToast(
                        "App data not accessible (cable plugged in \"USB Mass Storage Device\" mode?)",
                        this );
                return true;
            }
            Globals.mupen64plus_cfg.save();
            Globals.resumeLastSession = false; //TODO: something screwy when this is true
            
            Intent intent = new Intent( this, GameActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
            //finish(); // TODO: Don't finish MenuActivity... user may come back later
            return true;
        }
        else if( preference == mMenuResetPrefs )
        {
            // Reset the user preferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( this );
            preferences.edit().clear().commit();
            PreferenceManager.setDefaultValues( this, R.xml.preferences, true );
            
            // Refresh the entire menu by restarting the activity
            finish();
            startActivity( getIntent() );
            return true;
        }
        else if( preference == mMenuResetAppData )
        {
            // Reset the application data
            Globals.appData.resetToDefaults();
            return true;
        }
        // (To add handlers for other preferences, repeat this pattern and return true)
        return false;
    }
}
