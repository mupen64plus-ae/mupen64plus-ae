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
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.CrashTester;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptConfirmListener;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class SettingsGlobalActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    
    private static final String VIDEO_POLYGON_OFFSET = "videoPolygonOffset";
    private static final String VIDEO_HARDWARE_TYPE = "videoHardwareType";
    private static final String ACTION_CRASH_TEST = "actionCrashTest";
    private static final String ACTION_RELOAD_ASSETS = "actionReloadAssets";
    private static final String ACTION_RESET_USER_PREFS = "actionResetUserPrefs";
    
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String SCREEN_DISPLAY = "screenDisplay";
    private static final String SCREEN_TOUCHSCREEN = "screenTouchscreen";
    private static final String SCREEN_TOUCHPAD = "screenTouchpad";
    private static final String SCREEN_INPUT = "screenInput";
    
    private static final String DISPLAY_ORIENTATION = "displayOrientation";
    private static final String DISPLAY_RESOLUTION = "displayResolution";
    private static final String DISPLAY_IMMERSIVE_MODE = "displayImmersiveMode";
    private static final String DISPLAY_ACTION_BAR_TRANSPARENCY = "displayActionBarTransparency";
    private static final String DISPLAY_FPS_REFRESH = "displayFpsRefresh";
    private static final String AUDIO_BUFFER_SIZE = "audioBufferSize";
    private static final String AUDIO_SYNCHRONIZE = "audioSynchronize";
    private static final String AUDIO_SWAP_CHANNELS = "audioSwapChannels";
    private static final String TOUCHSCREEN_FEEDBACK = "touchscreenFeedback";
    private static final String TOUCHSCREEN_AUTO_HOLD = "touchscreenAutoHold";
    private static final String NAVIGATION_MODE = "navigationMode";
    private static final String ACRA_USER_EMAIL = "acra.user.email";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private SharedPreferences mPrefs = null;
    
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
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences_global );
        
        // Refresh the preference data wrapper
        mUserPrefs = new UserPrefs( this );
        
        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RELOAD_ASSETS, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESET_USER_PREFS, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_CRASH_TEST, new CrashTester( this ) );
        
        // Hide certain categories altogether if they're not applicable. Normally we just rely on
        // the built-in dependency disabler, but here the categories are so large that hiding them
        // provides a better user experience.
        if( !AppData.IS_KITKAT )
            PrefUtil.removePreference( this, SCREEN_DISPLAY, DISPLAY_IMMERSIVE_MODE );
        
        if( !mUserPrefs.isActionBarAvailable )
            PrefUtil.removePreference( this, SCREEN_DISPLAY, DISPLAY_ACTION_BAR_TRANSPARENCY );
        
        if( !mAppData.hardwareInfo.isXperiaPlay )
            PrefUtil.removePreference( this, SCREEN_ROOT, SCREEN_TOUCHPAD );
        
        // Remove some menu items in some cases
        Bundle extras = getIntent().getExtras();
        if( extras != null )
        {
            int mode = extras.getInt( Keys.Extras.MENU_DISPLAY_MODE, 0 );
            if( mode == 1 )
            {
                // Remove distractions if this was launched from TouchscreenProfileActivity
                PrefUtil.removePreference( this, SCREEN_ROOT, "categoryAudio" );
                PrefUtil.removePreference( this, SCREEN_ROOT, SCREEN_TOUCHPAD );
                PrefUtil.removePreference( this, SCREEN_ROOT, SCREEN_INPUT );
                PrefUtil.removePreference( this, SCREEN_ROOT, "categoryData" );
                PrefUtil.removePreference( this, SCREEN_ROOT, "categoryCrashReports" );
                PrefUtil.removePreference( this, SCREEN_ROOT, ACTION_RESET_USER_PREFS );
                PrefUtil.removePreference( this, SCREEN_DISPLAY, DISPLAY_ORIENTATION );
                PrefUtil.removePreference( this, SCREEN_DISPLAY, DISPLAY_RESOLUTION );
                PrefUtil.removePreference( this, SCREEN_DISPLAY, DISPLAY_FPS_REFRESH );
                PrefUtil.removePreference( this, SCREEN_DISPLAY, VIDEO_HARDWARE_TYPE );
                PrefUtil.removePreference( this, SCREEN_DISPLAY, VIDEO_POLYGON_OFFSET );
                PrefUtil.removePreference( this, SCREEN_TOUCHSCREEN, TOUCHSCREEN_FEEDBACK );
                PrefUtil.removePreference( this, SCREEN_TOUCHSCREEN, TOUCHSCREEN_AUTO_HOLD );
            }
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
        if( key.equals( NAVIGATION_MODE ) )
        {
            // Sometimes one preference change affects the hierarchy or layout of the views. In this
            // case it's easier just to restart the activity than try to figure out what to fix.
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
        
        // Enable audio prefs if audio is enabled
        PrefUtil.enablePreference( this, AUDIO_BUFFER_SIZE, mUserPrefs.audioPlugin.enabled );
        PrefUtil.enablePreference( this, AUDIO_SYNCHRONIZE, mUserPrefs.audioPlugin.enabled );
        PrefUtil.enablePreference( this, AUDIO_SWAP_CHANNELS, mUserPrefs.audioPlugin.enabled );
        
        // Update the summary text in a particular way for ACRA user info
        EditTextPreference pref = (EditTextPreference) findPreference( ACRA_USER_EMAIL );
        if( pref != null )
        {
            String value = pref.getText();
            if( TextUtils.isEmpty( value ) )
                pref.setSummary( getString( R.string.acraUserEmail_summary ) );
            else
                pref.setSummary( value );
        }
    }
    
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        // Handle the clicks on certain menu items that aren't actually preferences
        String key = preference.getKey();
        
        if( key.equals( ACTION_RELOAD_ASSETS ) )
            actionReloadAssets();
        
        else if( key.equals( ACTION_RESET_USER_PREFS ) )
            actionResetUserPrefs();
        
        else
            // Let Android handle all other preference clicks
            return false;
        
        // Tell Android that we handled the click
        return true;
    }
    
    private void actionReloadAssets()
    {
        mAppData.putAssetVersion( 0 );
        startActivity( new Intent( this, SplashActivity.class ) );
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
                mPrefs.unregisterOnSharedPreferenceChangeListener( SettingsGlobalActivity.this );
                mPrefs.edit().clear().commit();
                PreferenceManager.setDefaultValues( SettingsGlobalActivity.this, R.xml.preferences_global, true );
                
                // Also reset any manual overrides the user may have made in the config file
                File configFile = new File( mUserPrefs.mupen64plus_cfg );
                if( configFile.exists() )
                    configFile.delete();
                
                // Rebuild the menu system by restarting the activity
                finish();
                startActivity( getIntent() );
            }
        } );
    }
}
