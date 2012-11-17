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
 * Authors: littleguy77
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
import android.preference.PreferenceScreen;
import android.util.Log;

public class MenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String MAIN_SETTINGS = "mainSettings";
    private static final String MENU_RESUME = "menuResume";
    private static final String MENU_RESET_USER_PREFS = "menuResetUserPrefs";
    private static final String MENU_DEVICE_INFO = "menuDeviceInfo";
    private static final String MENU_PERIPHERAL_INFO = "menuPeripheralInfo";
    private static final String TOUCHSCREEN = "touchscreen";
    private static final String TOUCHSCREEN_CUSTOM = "touchscreenCustom";
    private static final String TOUCHSCREEN_SIZE = "touchscreenSize";
    private static final String XPERIA = "xperia";
    private static final String XPERIA_ENABLED = "xperiaEnabled";
    private static final String PERIPHERAL = "peripheral";
    private static final String VIDEO = "video";
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Disable the Xperia PLAY plugin as necessary
        if( !Globals.appData.hardwareInfo.isXperiaPlay )
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
            prefs.edit().putBoolean( XPERIA_ENABLED, false ).commit();
        }
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences );
        
        Globals.userPrefs = new UserPrefs( this, Globals.paths );
        
        // Define the click callback for certain menu items that aren't actually preferences
        findPreference( MENU_RESUME ).setOnPreferenceClickListener( this );
        findPreference( MENU_RESET_USER_PREFS ).setOnPreferenceClickListener( this );
        findPreference( MENU_DEVICE_INFO ).setOnPreferenceClickListener( this );
        findPreference( MENU_PERIPHERAL_INFO ).setOnPreferenceClickListener( this );
        
        // Hide the Xperia PLAY menu item as necessary
        if( !Globals.appData.hardwareInfo.isXperiaPlay )
        {
            PreferenceScreen screen = (PreferenceScreen) findPreference( MAIN_SETTINGS );
            Preference xperia = findPreference( XPERIA );
            screen.removePreference( xperia );
        }
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
            if( !Globals.paths.isSdCardAccessible() )
            {
                Log.e( "MenuActivity", "SD Card not accessable in MenuResume.onPreferenceClick" );
                Notifier.showToast( this, R.string.toast_sdInaccessible );
                return true;
            }
            
            // Launch the appropriate game activity
            Intent intent = Globals.userPrefs.isXperiaEnabled
                    ? new Intent( this, GameActivityXperiaPlay.class )
                    : new Intent( this, GameActivity.class );
            startActivity( intent );
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
        else if( key.equals( MENU_DEVICE_INFO ) )
        {
            new Builder( this ).setTitle( this.getString( R.string.menuDeviceInfo_title ) )
                    .setMessage( Utility.getCpuInfo() ).create().show();
            Log.i( "MenuActivity", Utility.getCpuInfo() );
        }
        else if( key.equals( MENU_PERIPHERAL_INFO ) )
        {
            new Builder( this ).setTitle( this.getString( R.string.menuPeripheralInfo_title ) )
                    .setMessage( Utility.getPeripheralInfo( this ) ).create().show();
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
        // Determine which menu items should be enabled
        boolean enableResume = new File( Globals.userPrefs.selectedGame ).exists();
        boolean enableInput = Globals.userPrefs.inputPlugin.enabled;
        boolean enableVideo = Globals.userPrefs.videoPlugin.enabled;
        boolean enableCustom = Globals.userPrefs.isTouchscreenEnabled && Globals.userPrefs.isTouchscreenCustom;
        boolean enableSize = Globals.userPrefs.isTouchscreenEnabled && !Globals.userPrefs.isTouchscreenCustom;
        
        // Enable the play menu only if the selected game actually exists
        findPreference( MENU_RESUME ).setEnabled( enableResume );
        
        // Enable the various input menus only if the input plug-in is not a dummy
        findPreference( TOUCHSCREEN ).setEnabled( enableInput );
        findPreference( PERIPHERAL ).setEnabled( enableInput );
        if( Globals.appData.hardwareInfo.isXperiaPlay )
            findPreference( XPERIA ).setEnabled( enableInput );
        
        // Enable the video menu only if the video plug-in is not a dummy
        findPreference( VIDEO ).setEnabled( enableVideo );
        
        // Enable the custom touchscreen prefs under certain conditions
        findPreference( TOUCHSCREEN_CUSTOM ).setEnabled( enableCustom );
        findPreference( TOUCHSCREEN_SIZE ).setEnabled( enableSize );
        
        // Update the summary text for all ListPreferences
        for( String key : sharedPreferences.getAll().keySet() )
        {
            Preference preference = findPreference( key );
            if( preference instanceof ListPreference )
                preference.setSummary( ( (ListPreference) preference ).getEntry() );
        }
    }
}
