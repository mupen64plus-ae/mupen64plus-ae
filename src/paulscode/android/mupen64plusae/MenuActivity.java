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

import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.PathPreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.ChangeLog;
import paulscode.android.mupen64plusae.util.CrashTester;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.OUYAInterface;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class MenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    
    private static final String PATH_SELECTED_GAME = "pathSelectedGame";
    
    private static final String ACTION_PLAY = "actionPlay";
    private static final String ACTION_DEVICE_INFO = "actionDeviceInfo";
    private static final String ACTION_CRASH_TEST = "actionCrashTest";
    private static final String ACTION_RELOAD_ASSETS = "actionReloadAssets";
    private static final String ACTION_RESET_USER_PREFS = "actionResetUserPrefs";
    private static final String ACTION_HELP = "actionHelp";
    private static final String ACTION_ABOUT = "actionAbout";
    
    private static final String SCREEN_TOUCHPAD = "screenTouchpad";
    private static final String SCREEN_TOUCHSCREEN = "screenTouchscreen";
    private static final String SCREEN_VIDEO = "screenVideo";
    
    private static final String CATEGORY_SINGLE_PLAYER = "categorySinglePlayer";
    
    private static final String TOUCHSCREEN_ENABLED = "touchscreenEnabled";
    private static final String TOUCHSCREEN_AUTO_HOLDABLES = "touchscreenAutoHoldables";
    private static final String TOUCHSCREEN_STYLE = "touchscreenStyle";
    private static final String TOUCHSCREEN_HEIGHT = "touchscreenHeight";
    private static final String TOUCHSCREEN_LAYOUT = "touchscreenLayout";
    private static final String PATH_CUSTOM_TOUCHSCREEN = "pathCustomTouchscreen";
    private static final String TOUCHPAD_ENABLED = "touchpadEnabled";
    private static final String TOUCHPAD_LAYOUT = "touchpadLayout";
    private static final String INPUT_VOLUME_MAPPABLE = "inputVolumeMappable";
    private static final String PLUGIN_AUDIO = "pluginAudio";
    private static final String R4300_EMULATOR = "r4300Emulator";
    private static final String VIDEO_POSITION = "videoPosition";
    private static final String VIDEO_RESOLUTION = "videoResolution";
    private static final String VIDEO_SCALING = "videoScaling";
    private static final String VIDEO_IMMERSIVE_MODE = "videoImmersiveMode";
    private static final String VIDEO_ACTION_BAR_TRANSPARENCY = "videoActionBarTransparency";
    private static final String NAVIGATION_MODE = "navigationMode";
    private static final String ACRA_USER_EMAIL = "acra.user.email";
    private static final String LOCALE_OVERRIDE = "localeOverride";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    
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
        
        // Disable the Xperia PLAY plugin as necessary
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        if( !mAppData.hardwareInfo.isXperiaPlay )
            prefs.edit().putBoolean( TOUCHPAD_ENABLED, false ).commit();
        
        // Set some prefs when running in big-screen mode
        if( mUserPrefs.isBigScreenMode )
        {
            prefs.edit().putBoolean( TOUCHSCREEN_ENABLED, false ).commit();
            prefs.edit().putBoolean( INPUT_VOLUME_MAPPABLE, true ).commit();
        }
        
        // Ensure that any missing preferences are populated with defaults (e.g. preference added to new release)
        PreferenceManager.setDefaultValues( this, R.xml.preferences, false );
        
        // Ensure that selected plugin names and other list preferences are valid
        Resources res = getResources();
        PrefUtil.validateListPreference( res, prefs, TOUCHSCREEN_STYLE, R.string.touchscreenStyle_default, R.array.touchscreenStyle_values );
        PrefUtil.validateListPreference( res, prefs, TOUCHSCREEN_HEIGHT, R.string.touchscreenHeight_default, R.array.touchscreenHeight_values );
        PrefUtil.validateListPreference( res, prefs, TOUCHSCREEN_LAYOUT, R.string.touchscreenLayout_default, R.array.touchscreenLayout_values );
        PrefUtil.validateListPreference( res, prefs, TOUCHPAD_LAYOUT, R.string.touchpadLayout_default, R.array.touchpadLayout_values );
        PrefUtil.validateListPreference( res, prefs, VIDEO_POSITION, R.string.videoPosition_default, R.array.videoPosition_values );
        PrefUtil.validateListPreference( res, prefs, VIDEO_RESOLUTION, R.string.videoResolution_default, R.array.videoResolution_values );
        PrefUtil.validateListPreference( res, prefs, VIDEO_SCALING, R.string.videoScaling_default, R.array.videoScaling_values );
        PrefUtil.validateListPreference( res, prefs, PLUGIN_AUDIO, R.string.pluginAudio_default, R.array.pluginAudio_values );
        PrefUtil.validateListPreference( res, prefs, R4300_EMULATOR, R.string.r4300Emulator_default, R.array.r4300Emulator_values );
        PrefUtil.validateListPreference( res, prefs, NAVIGATION_MODE, R.string.navigationMode_default, R.array.navigationMode_values );
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences );
        
        // Refresh the preference data wrapper
        mUserPrefs = new UserPrefs( this );
        
        // Populate the language menu
        ListPreference languagePref = (ListPreference) findPreference( LOCALE_OVERRIDE );
        languagePref.setEntryValues( mUserPrefs.localeCodes );
        languagePref.setEntries( mUserPrefs.localeNames );
        
        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_DEVICE_INFO, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RELOAD_ASSETS, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESET_USER_PREFS, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_HELP, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_ABOUT, this );
        
        // Handle crash tests in a particular way (see CrashTester for more info)
        findPreference( ACTION_CRASH_TEST ).setOnPreferenceClickListener( new CrashTester( this ) );
        
        // Hide certain categories altogether if they're not applicable. Normally we just rely on
        // the built-in dependency disabler, but here the categories are so large that hiding them
        // provides a better user experience.
        if( !AppData.IS_KITKAT )
            PrefUtil.removePreference( this, SCREEN_VIDEO, VIDEO_IMMERSIVE_MODE );
        
        if( !mUserPrefs.isActionBarAvailable )
            PrefUtil.removePreference( this, SCREEN_VIDEO, VIDEO_ACTION_BAR_TRANSPARENCY );
        
        if( !mAppData.hardwareInfo.isXperiaPlay )
            PrefUtil.removePreference( this, CATEGORY_SINGLE_PLAYER, SCREEN_TOUCHPAD );
        
        if( mUserPrefs.isBigScreenMode )
        {
            PrefUtil.removePreference( this, CATEGORY_SINGLE_PLAYER, SCREEN_TOUCHSCREEN );
            PrefUtil.removePreference( this, CATEGORY_SINGLE_PLAYER, INPUT_VOLUME_MAPPABLE );
        }
        
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        sharedPreferences.unregisterOnSharedPreferenceChangeListener( this );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        sharedPreferences.registerOnSharedPreferenceChangeListener( this );
        refreshViews();
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        if( key.equals( TOUCHPAD_ENABLED ) || key.equals( NAVIGATION_MODE )
                || key.equals( LOCALE_OVERRIDE ) )
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
        PrefUtil.enablePreference( this, ACTION_PLAY, isValidGame );
        
        // Update the summary text for the selected game
        PathPreference pp = (PathPreference) findPreference( PATH_SELECTED_GAME );
        if( pp != null )
        {
            pp.setSummary( selectedGame.getName() );
        }
        
        // Enable the auto-holdables pref if auto-hold is not disabled
        PrefUtil.enablePreference( this, TOUCHSCREEN_AUTO_HOLDABLES, mUserPrefs.isTouchscreenEnabled
                && mUserPrefs.touchscreenAutoHold != TouchController.AUTOHOLD_METHOD_DISABLED );
        
        // Enable the custom touchscreen prefs under certain conditions
        PrefUtil.enablePreference( this, PATH_CUSTOM_TOUCHSCREEN, mUserPrefs.isTouchscreenEnabled
                && mUserPrefs.isTouchscreenCustom );
        PrefUtil.enablePreference( this, TOUCHSCREEN_STYLE, mUserPrefs.isTouchscreenEnabled
                && !mUserPrefs.isTouchscreenCustom );
        PrefUtil.enablePreference( this, TOUCHSCREEN_HEIGHT, mUserPrefs.isTouchscreenEnabled
                && !mUserPrefs.isTouchscreenCustom );
        
        // Update the summary text in a particular way for ACRA user info
        EditTextPreference pref = (EditTextPreference) findPreference( ACRA_USER_EMAIL );
        String value = pref.getText();
        if( TextUtils.isEmpty( value ) )
            pref.setSummary( getString( R.string.acraUserEmail_summary ) );
        else
            pref.setSummary( value );
    }
    
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        // Handle the clicks on certain menu items that aren't actually preferences
        String key = preference.getKey();
        
        if( key.equals( ACTION_DEVICE_INFO ) )
            actionDeviceInfo();
        
        else if( key.equals( ACTION_RELOAD_ASSETS ) )
            actionReloadAssets();
        
        else if( key.equals( ACTION_RESET_USER_PREFS ) )
            actionResetUserPrefs();
        
        else if( key.equals( ACTION_HELP ) )
            actionHelp();
        
        else if( key.equals( ACTION_ABOUT ) )
            actionAbout();
        
        else
            // Let Android handle all other preference clicks
            return false;
        
        // Tell Android that we handled the click
        return true;
    }
    
    private void actionDeviceInfo()
    {
        String title = getString( R.string.actionDeviceInfo_title );
        String message = DeviceUtil.getCpuInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void actionReloadAssets()
    {
        mAppData.putAssetVersion( 0 );
        startActivity( new Intent( this, MainActivity.class ) );
        finish();
    }
    
    private void actionResetUserPrefs()
    {
        String title = getString( R.string.confirm_title );
        String message = getString( R.string.actionResetUserPrefs_popupMessage );
        Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                // Reset the user preferences
                SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences( MenuActivity.this );
                preferences.unregisterOnSharedPreferenceChangeListener( MenuActivity.this );
                preferences.edit().clear().commit();
                PreferenceManager.setDefaultValues( MenuActivity.this, R.xml.preferences, true );
                
                // Also reset any manual overrides the user may have made in the config file
                File configFile = new File( mAppData.mupen64plus_cfg );
                if( configFile.exists() )
                    configFile.delete();
                
                // Rebuild the menu system by restarting the activity
                finish();
                startActivity( getIntent() );
            }
        } );
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
