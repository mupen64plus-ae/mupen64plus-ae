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

import android.text.TextUtils;
import org.acra.ACRA;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.TaskHandler;
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
    
    private static final String PLAY_MENU = "menuPlay";
    private static final String LAUNCH_RESET_USER_PREFS = "menuResetUserPrefs";
    private static final String LAUNCH_RELOAD_APP_DATA = "menuReloadAppData";
    private static final String LAUNCH_DEVICE_INFO = "menuDeviceInfo";
    private static final String LAUNCH_PERIPHERAL_INFO = "menuPeripheralInfo";
    private static final String LAUNCH_CRASH = "launchCrash";
    private static final String PROCESS_TEXTURE_PACK = "gles2RiceImportHiResTextures";

    // private static final String SELECTED_GAME = "selectedGame";

    private static final String INPUT = "input";
    private static final String AUDIO = "audio";
    private static final String VIDEO = "video";
    
    private static final String XPERIA = "xperia";
    private static final String XPERIA_ENABLED = "xperiaEnabled";
    private static final String TOUCHSCREEN_CUSTOM = "touchscreenCustom";
    private static final String TOUCHSCREEN_SIZE = "touchscreenSize";
    private static final String VIDEO_PLUGIN = "videoPlugin";
    private static final String CATEGORY_SINGLE_PLAYER = "categorySinglePlayer";
    private static final String CATEGORY_GLES2_RICE = "categoryGles2Rice";
    private static final String CATEGORY_GLES2_N64 = "categoryGles2N64";
    
    private CheatsMenuHandler mCheatsMenuHandler = null;
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;

    // Don't need to call these every time the orientation changes
    static
    {
        // Required for reading CRC header
        FileUtil.loadNativeLibName( "SDL" );
        FileUtil.loadNativeLibName( "core" );
        FileUtil.loadNativeLibName( "front-end" );
    }
    
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
        
        // Instantiate the cheats menu handler
        if( mCheatsMenuHandler == null )
            mCheatsMenuHandler = new CheatsMenuHandler( this, mAppData, mUserPrefs );
        // TODO: Only refresh when the cheats menu is open
        mCheatsMenuHandler.refresh();
        
        // Define the click callback for certain menu items that aren't actually preferences
        listenTo( LAUNCH_RESET_USER_PREFS );
        listenTo( LAUNCH_RELOAD_APP_DATA );
        listenTo( LAUNCH_DEVICE_INFO );
        listenTo( LAUNCH_PERIPHERAL_INFO );
        listenTo( LAUNCH_CRASH );
        listenTo( PLAY_MENU );
        
        // Provide the opportunity to override other preference clicks
        for( String key : prefs.getAll().keySet() )
            listenTo( key );
        
        // Hide the Xperia PLAY menu items as necessary
        if( !mAppData.hardwareInfo.isXperiaPlay )
        {
            removePreference( CATEGORY_SINGLE_PLAYER, XPERIA );
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
        
        if( key.equals( PLAY_MENU ) )
        {
            mCheatsMenuHandler.rebuild();
            // Let Android open the play menu, once built
            return false;
        }
        
        else if( key.equals( LAUNCH_RESET_USER_PREFS ) )
            launchResetUserPrefs();
        
        else if( key.equals( LAUNCH_RELOAD_APP_DATA ) )
            launchReloadAppData();
        
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
                    // Don't handle all the changes that are about to be made
                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences( MenuActivity.this );
                    sharedPreferences
                            .unregisterOnSharedPreferenceChangeListener( MenuActivity.this );
                    
                    // Reset the user preferences
                    SharedPreferences preferences = PreferenceManager
                            .getDefaultSharedPreferences( MenuActivity.this );
                    preferences.edit().clear().commit();
                    PreferenceManager.setDefaultValues( MenuActivity.this, R.xml.preferences, true );
                    
                    // Rebuild the menu system by restarting the activity
                    finish();
                    startActivity( getIntent() );
                }
            }
        } );
    }
    
    private void launchReloadAppData()
    {
        mAppData.setAssetVersion( 0 );        
        startActivity( new Intent( this, MainActivity.class ) );
        finish();
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
    
    private void launchCrash()
    {
        // Test auto crash reporting system by sending a report
        ACRA.getErrorReporter().handleSilentException( new Exception( "BENIGN CRASH TEST" ) );
        Notifier.showToast( this, "Report sent." );  // TODO localize
    }
    
    private void processTexturePak( String filename )
    {
        if( TextUtils.isEmpty( filename ) )
        {
            ErrorLogger.put( "Video", "gles2RiceImportHiResTextures", "Filename not specified in MenuActivity.processTexturePak" );
            Notifier.showToast( this, getString( R.string.gles2RiceImportHiResTexturesTask_errorMessage ) );
            return;
        }
    	
    	final String textureFile = filename;
        TaskHandler.run
        (
            this, getString( R.string.gles2RiceImportHiResTexturesTask_title ),
            getString( R.string.gles2RiceImportHiResTexturesTask_message ),
            new TaskHandler.Task()
            {
                @Override
                public void run()
                {
                    String headerName = Utility.getTexturePackName( textureFile );
                    if( !ErrorLogger.hasError() )
                    {
                        if( TextUtils.isEmpty( headerName ) )
                        {
                            ErrorLogger.setLastError( "getTexturePackName returned null in MenuActivity.processTexturePak" );
                            ErrorLogger.putLastError( "Video", "gles2RiceImportHiResTextures" );
                        }
                        else
                        {
                            String outputFolder = mAppData.dataDir + "/data/hires_texture/" + headerName;
                            FileUtil.deleteFolder( new File( outputFolder ) );
                            Utility.unzipAll( new File( textureFile ), outputFolder );
                        }
                    }
                }
                @Override
                public void onComplete()
                {
                    if( ErrorLogger.hasError() )
                        Notifier.showToast( MenuActivity.this, getString( R.string.gles2RiceImportHiResTexturesTask_errorMessage ) );
                    ErrorLogger.clearLastError();
                }
            }
        );
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

    @SuppressWarnings( "deprecation" )
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        boolean rebuildHierarchy =
                   key.equals( VIDEO_PLUGIN )
                // || key.equals( SELECTED_GAME )
                || key.equals( XPERIA_ENABLED );
        
        if( rebuildHierarchy )
        {
            // Sometimes one preference change affects the hierarchy or layout of the views.
            // In this case it's easier just to restart the activity than trying to figure out what to fix.
            // Examples:
            //   Restore the preference categories that were removed in refreshViews(...)
            //   Change the input mapping layout file when Xperia Play touchpad en/disabled
            finish();
            startActivity( getIntent() );
        }
        else if( key.equals( PROCESS_TEXTURE_PACK ) )
        {
            // TODO: Make this summary persist, rather than the last selected filename
            findPreference( key ).setSummary( getString( R.string.gles2RiceImportHiResTextures_summary ) );
            processTexturePak( sharedPreferences.getString( PROCESS_TEXTURE_PACK, "" ) );
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
        File selectedGame = new File( mUserPrefs.selectedGame );
        boolean isValidGame = selectedGame.exists() && selectedGame.isFile();
        enablePreference( PLAY_MENU, isValidGame );
        
        // Enable the input menu only if the input plug-in is not a dummy
        enablePreference( INPUT, user.inputPlugin.enabled );
        
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
