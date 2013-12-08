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
package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.PathPreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.ChangeLog;
import paulscode.android.mupen64plusae.util.OUYAInterface;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
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
import android.preference.PreferenceManager;

public class MenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    
    private static final String PATH_SELECTED_GAME = "pathSelectedGame";
    private static final String ACTION_HELP = "actionHelp";
    private static final String ACTION_ABOUT = "actionAbout";
    private static final String SCREEN_PLAY = "screenPlay";
    private static final String LOCALE_OVERRIDE = "localeOverride";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private SharedPreferences mPrefs = null;
    
    @Override
    protected void onNewIntent( Intent intent )
    {
        // If the activity is already running and is launched again (e.g. from a file manager app),
        // the existing instance will be reused rather than a new one created. This behavior is
        // specified in the manifest (launchMode = singleTask). In that situation, any activities
        // above this on the stack (e.g. GameActivity, PlayMenuActivity) will be destroyed
        // gracefully and onNewIntent() will be called on this instance. onCreate() will NOT be
        // called again on this instance. Currently, the only info that may be passed via the intent
        // is the selected game path, so we only need to refresh that aspect of the UI.  This will
        // happen anyhow in onResume(), so we don't really need to do much here.
        super.onNewIntent( intent );
        
        // Only remember the last intent used
        setIntent( intent );
    }
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
        
        int lastVer = mAppData.getLastAppVersionCode();
        int currVer = mAppData.appVersionCode;
        if( lastVer != currVer )
        {
            // First run after install/update, greet user with changelog, then help dialog
            actionHelp();            
            ChangeLog log = new ChangeLog( getAssets() );
            if( log.show( this, lastVer + 1, currVer ) )
            {
                mAppData.putLastAppVersionCode( currVer );
            }
        }
        
        // Ensure that any missing preferences are populated with defaults (e.g. preference added to new release)
        PreferenceManager.setDefaultValues( this, R.xml.preferences, false );
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences );
        
        // Refresh the preference data wrapper
        mUserPrefs = new UserPrefs( this );
        
        // Populate the language menu
        ListPreference languagePref = (ListPreference) findPreference( LOCALE_OVERRIDE );
        languagePref.setEntryValues( mUserPrefs.localeCodes );
        languagePref.setEntries( mUserPrefs.localeNames );
        
        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_HELP, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_ABOUT, this );
        
        // Initialize the OUYA interface if running on OUYA
        if( OUYAInterface.IS_OUYA_HARDWARE )
            OUYAInterface.init( this );
        
        // Popup a warning if the installation appears to be corrupt
        if( !mAppData.isValidInstallation )
        {
            CharSequence title = getText( R.string.invalidInstall_title );
            CharSequence message = getText( R.string.invalidInstall_message );
            new Builder( this ).setTitle( title ).setMessage( message ).create().show();
        }
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
        refreshViews();
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        if( key.equals( LOCALE_OVERRIDE ) )
        {
            // Sometimes one preference change affects the hierarchy or layout of the views. In this
            // case it's easier just to restart the activity than try to figure out what to fix.
            // Examples:
            // * Restore the preference categories that were removed in refreshViews(...)
            // * Change the input mapping layout when Xperia Play touchpad en/disabled
            finish();
            startActivity( getIntent() );
        }
        else
        {
            // Just refresh the preference screens in place
            refreshViews();
        }
    }
    
    @TargetApi( 9 )
    @SuppressWarnings( "deprecation" )
    private void refreshViews()
    {
        // Refresh the preferences object
        mUserPrefs = new UserPrefs( this );
        
        // Enable the play menu only if the selected game actually exists
        File selectedGame = new File( mUserPrefs.selectedGame );
        boolean isValidGame = selectedGame.exists() && selectedGame.isFile();
        PrefUtil.enablePreference( this, SCREEN_PLAY, isValidGame );
        
        // Update the summary text for the selected game
        PathPreference pp = (PathPreference) findPreference( PATH_SELECTED_GAME );
        if( pp != null )
        {
            pp.setSummary( selectedGame.getName() );
        }
    }
    
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        // Handle the clicks on certain menu items that aren't actually preferences
        String key = preference.getKey();
        
        if( key.equals( ACTION_HELP ) )
            actionHelp();
        
        else if( key.equals( ACTION_ABOUT ) )
            actionAbout();
        
        else
            // Let Android handle all other preference clicks
            return false;
        
        // Tell Android that we handled the click
        return true;
    }
    
    private void actionHelp()
    {
        CharSequence title = getText( R.string.actionHelp_title );
        CharSequence message = getText( R.string.actionHelp_message );
        String faq = getString( R.string.actionHelp_faq );
        String bug = getString( R.string.actionHelp_reportbug );
        OnClickListener listener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_NEUTRAL )
                    Utility.launchUri( MenuActivity.this, R.string.actionHelp_uriFaq );
                else if( which == DialogInterface.BUTTON_POSITIVE )
                    Utility.launchUri( MenuActivity.this, R.string.actionHelp_uriBug );
            }
        };
        new Builder( this ).setTitle( title ).setMessage( message )
                .setNeutralButton( faq, listener ).setNegativeButton( null, null )
                .setPositiveButton( bug, listener ).create().show();
    }
    
    private void actionAbout()
    {
        String title = getString( R.string.actionAbout_title );
        String message = getString( R.string.actionAbout_message, mAppData.appVersion,
                mAppData.appVersionCode );
        String credits = getString( R.string.actionAbout_credits );
        String changelog = getString( R.string.actionAbout_changelog );
        OnClickListener listener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_NEUTRAL )
                    Utility.launchUri( MenuActivity.this, R.string.actionAbout_uriCredits );
                else if( which == DialogInterface.BUTTON_POSITIVE )
                    new ChangeLog( getAssets() ).show( MenuActivity.this, 0, mAppData.appVersionCode );
            }
        };
        new Builder( this ).setTitle( title ).setMessage( message ).setNegativeButton( null, null )
                .setNeutralButton( credits, listener ).setPositiveButton( changelog, listener )
                .create().show();
    }
}
