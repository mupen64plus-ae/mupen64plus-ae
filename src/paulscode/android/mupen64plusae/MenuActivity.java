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

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;

public class MenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    
    private static final String LAUNCH_RESUME = "menuResume";
    private static final String LAUNCH_RESET_USER_PREFS = "menuResetUserPrefs";
    private static final String LAUNCH_DEVICE_INFO = "menuDeviceInfo";
    private static final String LAUNCH_PERIPHERAL_INFO = "menuPeripheralInfo";
    private static final String LAUNCH_CRASH = "launchCrash";

    private static final String TOUCHSCREEN = "touchscreen";
    private static final String PERIPHERAL = "peripheral";
    private static final String AUDIO = "audio";
    private static final String VIDEO = "video";
    
    private static final String XPERIA_ENABLED = "xperiaEnabled";
    private static final String XPERIA_LAYOUT = "xperiaLayout";
    private static final String TOUCHSCREEN_CUSTOM = "touchscreenCustom";
    private static final String TOUCHSCREEN_SIZE = "touchscreenSize";
    private static final String TOUCHSCREEN_OCTAGON_JOYSTICK = "touchscreenOctagonJoystick";
    private static final String VIDEO_PLUGIN = "videoPlugin";
    private static final String CATEGORY_GLES2_RICE = "categoryGles2Rice";
    private static final String CATEGORY_GLES2_N64 = "categoryGles2N64";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Get app data and user preferences
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        
        // Disable the Xperia PLAY plugin as necessary
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        if( !mAppData.hardwareInfo.isXperiaPlay )
            prefs.edit().putBoolean( XPERIA_ENABLED, false ).commit();
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences );
        
        // Refresh the preference data wrapper
        mUserPrefs = new UserPrefs( this );
        
        // Define the click callback for certain menu items that aren't actually preferences
        listenTo( LAUNCH_RESUME );
        listenTo( LAUNCH_RESET_USER_PREFS );
        listenTo( LAUNCH_DEVICE_INFO );
        listenTo( LAUNCH_PERIPHERAL_INFO );
        listenTo( LAUNCH_CRASH );
        
        // Provide the opportunity to override other preference clicks
        for( String key : prefs.getAll().keySet() )
            listenTo( key );
        
        // Hide the Xperia PLAY menu items as necessary
        if( !mAppData.hardwareInfo.isXperiaPlay )
        {
            removePreference( TOUCHSCREEN, XPERIA_ENABLED );
            removePreference( TOUCHSCREEN, XPERIA_LAYOUT );
        }
    }
    
    @SuppressWarnings( "deprecation" )
    private void listenTo( String key )
    {
        Preference preference = findPreference( key );
        if( preference != null )
            preference.setOnPreferenceClickListener( this );
    }

    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        // Handle the clicks on certain menu items that aren't actually preferences
        String key = preference.getKey();
        
        if( key.equals( LAUNCH_RESUME ) )
            launchResume();
        
        else if( key.equals( LAUNCH_RESET_USER_PREFS ) )
            launchResetUserPrefs();
        
        else if( key.equals( LAUNCH_DEVICE_INFO ) )
            launchDeviceInfo();
        
        else if( key.equals( LAUNCH_PERIPHERAL_INFO ) )
            launchPeripheralInfo();
        
        else if( key.equals( LAUNCH_CRASH ) )
            launchCrash();
        
        else // Let Android handle all other preference clicks
            return false;
        
        // Tell Android that we handled the click
        return true;
    }
    
    private void launchResume()
    {
        // Launch the last game in a new activity
        if( !mAppData.isSdCardAccessible() )
        {
            Log.e( "MenuActivity", "SD Card not accessable in MenuResume.onPreferenceClick" );
            Notifier.showToast( this, R.string.toast_sdInaccessible );
            return;
        }
        
        // Notify user that the game activity is starting
        Notifier.showToast( this, R.string.toast_appStarted );
        
        // Launch the appropriate game activity
        Intent intent = mUserPrefs.isXperiaEnabled
                ? new Intent( this, GameActivityXperiaPlay.class )
                : new Intent( this, GameActivity.class );
        startActivity( intent );
    }

    private void launchResetUserPrefs()
    {
        String title = getString( R.string._confirmation );
        String message = getString( R.string.resetPrefs_popupMessage );
        Prompt.promptConfirm( this, title, message, new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    // Reset the user preferences
                    SharedPreferences preferences = PreferenceManager
                            .getDefaultSharedPreferences( MenuActivity.this );
                    preferences.edit().clear().commit();
                    PreferenceManager.setDefaultValues( MenuActivity.this, R.xml.preferences, true );
                    
                    // Restart the activity so that the entire menu system is rebuilt
                    // (OnSharedPreferenceChangedListener is not sufficient for this)
                    finish();
                    startActivity( getIntent() );
                }
            }
        } );
    }
    
    private void launchDeviceInfo()
    {
        String title = getString( R.string.menuDeviceInfo_title );
        String message = Utility.getCpuInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void launchPeripheralInfo()
    {
        String title = getString( R.string.menuPeripheralInfo_title );
        String message = Utility.getPeripheralInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    @SuppressWarnings( "null" )
    private void launchCrash()
    {
        // Intentionally crash the app to test auto crash reporting
        String x = null;
        x.replace( 'a', 'b' );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        sharedPreferences.unregisterOnSharedPreferenceChangeListener( this );
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        refreshViews( sharedPreferences, mUserPrefs );
        sharedPreferences.registerOnSharedPreferenceChangeListener( this );
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        boolean restoreMissingPreferences = key.equals( VIDEO_PLUGIN );
        
        if( restoreMissingPreferences )
        {
            // Restore the preference categories that were removed in refreshViews(...)
            finish();
            startActivity( getIntent() );
            return;
        }
        else
        {
            // Just refresh the preference screens in place
            mUserPrefs = new UserPrefs( this );
            refreshViews( sharedPreferences, mUserPrefs );
        }
    }

    private void refreshViews( SharedPreferences sharedPreferences, UserPrefs user )
    {
        // Enable the play menu only if the selected game actually exists
        enablePreference( LAUNCH_RESUME, new File( mUserPrefs.selectedGame ).exists() );
        
        // Enable the various input menus only if the input plug-in is not a dummy
        enablePreference( TOUCHSCREEN, user.inputPlugin.enabled );
        enablePreference( PERIPHERAL, user.inputPlugin.enabled );
        enablePreference( TOUCHSCREEN_OCTAGON_JOYSTICK, user.isTouchscreenEnabled || user.isXperiaEnabled );
        
        // Enable the audio menu only if the audio plug-in is not a dummy
        enablePreference( AUDIO, user.audioPlugin.enabled );
        
        // Enable the video menu only if the video plug-in is not a dummy
        enablePreference( VIDEO, user.videoPlugin.enabled );
        
        // Hide certain categories altogether if they're not applicable. Normally we just rely on
        // the built-in dependency disabler, but here the categories are so large that hiding them
        // provides a better user experience.
        if( !user.isGles2N64Enabled )
            removePreference( VIDEO, CATEGORY_GLES2_N64 );
        
        if( !user.isGles2RiceEnabled )
            removePreference( VIDEO, CATEGORY_GLES2_RICE );
        
        // Enable the custom touchscreen prefs under certain conditions
        enablePreference( TOUCHSCREEN_CUSTOM, user.isTouchscreenEnabled && user.isTouchscreenCustom );
        enablePreference( TOUCHSCREEN_SIZE, user.isTouchscreenEnabled && !user.isTouchscreenCustom );
        
        // Update the summary text for all relevant preferences
        for( String key : sharedPreferences.getAll().keySet() )
            refreshText( key );
    }
    
    @SuppressWarnings( "deprecation" )
    private void refreshText( String key )
    {
        Preference preference = findPreference( key );
        if( preference instanceof ListPreference )
            preference.setSummary( ( (ListPreference) preference ).getEntry() );
    }    

    @SuppressWarnings( "deprecation" )
    private void enablePreference( String key, boolean enabled )
    {
        Preference preference = findPreference( key );
        if( preference != null )
            preference.setEnabled( enabled );
    }

    @SuppressWarnings( "deprecation" )
    private void removePreference( String keyParent, String keyChild )
    {
        Preference parent = findPreference( keyParent );
        Preference child = findPreference( keyChild );
        if( parent instanceof PreferenceGroup && child != null )
            ( (PreferenceGroup) parent ).removePreference( child );
    }
}
