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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae;

import java.util.ArrayList;

import paulscode.android.mupen64plusae.cheat.CheatEditorActivity;
import paulscode.android.mupen64plusae.cheat.CheatFile;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatSection;
import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.PlayerMapPreference;
import paulscode.android.mupen64plusae.persistent.ProfilePreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.util.RomDetail;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;

import com.bda.controller.Controller;

public class PlayMenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences_play.xml
    private static final String SCREEN_CHEATS = "screenCheats";
    
    private static final String CATEGORY_GAME_SETTINGS = "categoryGameSettings";
    private static final String CATEGORY_CHEATS = "categoryCheats";
    
    private static final String ACTION_RESUME = "actionResume";
    private static final String ACTION_RESTART = "actionRestart";
    private static final String ACTION_CHEAT_EDITOR = "actionCheatEditor";
    private static final String ACTION_WIKI = "actionWiki";
    
    private static final String EMULATION_PROFILE = "emulationProfile";
    private static final String TOUCHSCREEN_PROFILE = "touchscreenProfile";
    private static final String CONTROLLER_PROFILE1 = "controllerProfile1";
    private static final String CONTROLLER_PROFILE2 = "controllerProfile2";
    private static final String CONTROLLER_PROFILE3 = "controllerProfile3";
    private static final String CONTROLLER_PROFILE4 = "controllerProfile4";
    private static final String PLAYER_MAP = "playerMap";
    private static final String PLAY_SHOW_CHEATS = "playShowCheats";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private GamePrefs mGamePrefs = null;
    private SharedPreferences mPrefs = null;
    
    // ROM info
    private String mRomPath = null;
    private RomDetail mRomDetail = null;
    
    // Preference menu items
    ProfilePreference mEmulationProfile = null;
    ProfilePreference mTouchscreenProfile = null;
    ProfilePreference mControllerProfile1 = null;
    ProfilePreference mControllerProfile2 = null;
    ProfilePreference mControllerProfile3 = null;
    ProfilePreference mControllerProfile4 = null;
    PreferenceGroup mScreenCheats = null;
    PreferenceGroup mCategoryCheats = null;
    
    // MOGA controller interface
    private Controller mMogaController = Controller.getInstance( this );
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get the ROM path and MD5 that was passed to the activity
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle" );
        mRomPath = extras.getString( Keys.Extras.ROM_PATH );
        String romMd5 = extras.getString( Keys.Extras.ROM_MD5 );
        String romCrc = extras.getString( Keys.Extras.ROM_CRC ); 
        if( TextUtils.isEmpty( mRomPath ) || TextUtils.isEmpty( romMd5 ) || TextUtils.isEmpty( romCrc ) )
            throw new Error( "ROM path, MD5, and CRC must be passed via the extras bundle" );
        
        // Initialize MOGA controller API
        mMogaController.init();
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        mGamePrefs = new GamePrefs( this, romMd5 );
        mUserPrefs.enforceLocale( this );
        mPrefs = getSharedPreferences( mGamePrefs.sharedPrefsName, MODE_PRIVATE );
        
        // Get the detailed info about the ROM
        mRomDetail = RomDetail.lookupByMd5( romMd5 );
        
        // Load user preference menu structure from XML and update view
        getPreferenceManager().setSharedPreferencesName( mGamePrefs.sharedPrefsName );
        addPreferencesFromResource( R.xml.preferences_game );
        mEmulationProfile = (ProfilePreference) findPreference( EMULATION_PROFILE );
        mTouchscreenProfile = (ProfilePreference) findPreference( TOUCHSCREEN_PROFILE );
        mControllerProfile1 = (ProfilePreference) findPreference( CONTROLLER_PROFILE1 );
        mControllerProfile2 = (ProfilePreference) findPreference( CONTROLLER_PROFILE2 );
        mControllerProfile3 = (ProfilePreference) findPreference( CONTROLLER_PROFILE3 );
        mControllerProfile4 = (ProfilePreference) findPreference( CONTROLLER_PROFILE4 );
        mScreenCheats = (PreferenceGroup) findPreference( SCREEN_CHEATS );
        mCategoryCheats = (PreferenceGroup) findPreference( CATEGORY_CHEATS );
        
        // Set some game-specific strings
        setTitle( mRomDetail.goodName );
        if( !TextUtils.isEmpty( mRomDetail.baseName ) )
        {
            String title = getString( R.string.categoryGameSettings_titleNamed, mRomDetail.baseName );
            findPreference( CATEGORY_GAME_SETTINGS ).setTitle( title );
        }
        
        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESUME, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESTART, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_CHEAT_EDITOR, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_WIKI, this );
        
        // Remove wiki menu item if not applicable
        if( TextUtils.isEmpty( mRomDetail.wikiUrl ) )
        {
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, ACTION_WIKI );
        }
        
        // Setup controller profiles settings based on ROM's number of players
        if( mRomDetail.players == 1 )
        {
            // Simplify name of "controller 1" to just "controller" to eliminate confusion
            findPreference( CONTROLLER_PROFILE1 ).setTitle( R.string.controllerProfile_title );
            
            // Remove unneeded preference items
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE2 );
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE3 );
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE4 );
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, PLAYER_MAP );
        }
        else
        {
            // Remove unneeded preference items
            if( mRomDetail.players < 4 )
                PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE4 );
            if( mRomDetail.players < 3 )
                PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE3 );
            
            // Configure the player map preference
            PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( PLAYER_MAP );
            playerPref.setMogaController( mMogaController );
        }
        
        // Build the cheats category as needed
        refreshCheatsCategory();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
        mMogaController.onResume();
        refreshViews();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        mMogaController.onPause();
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mMogaController.exit();
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        refreshViews();
        if( key.equals( PLAY_SHOW_CHEATS ) )
        {
            refreshCheatsCategory();
        }
    }
    
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == 111 )
            refreshCheatsCategory();
    }
    
    private void refreshViews()
    {
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        
        // Refresh the preferences objects
        mUserPrefs = new UserPrefs( this );
        mGamePrefs = new GamePrefs( this, mRomDetail.md5 );
        
        // Populate the profile preferences
        mEmulationProfile.populateProfiles( mAppData.emulationProfiles_cfg,
                mUserPrefs.emulationProfiles_cfg, mUserPrefs.getEmulationProfileDefault() );
        mTouchscreenProfile.populateProfiles( mAppData.touchscreenProfiles_cfg,
                mUserPrefs.touchscreenProfiles_cfg, mUserPrefs.getTouchscreenProfileDefault() );
        mControllerProfile1.populateProfiles( mAppData.controllerProfiles_cfg,
                mUserPrefs.controllerProfiles_cfg, mUserPrefs.getControllerProfileDefault() );
        mControllerProfile2.populateProfiles( mAppData.controllerProfiles_cfg,
                mUserPrefs.controllerProfiles_cfg, "" );
        mControllerProfile3.populateProfiles( mAppData.controllerProfiles_cfg,
                mUserPrefs.controllerProfiles_cfg, "" );
        mControllerProfile4.populateProfiles( mAppData.controllerProfiles_cfg,
                mUserPrefs.controllerProfiles_cfg, "" );
        
        // Refresh the preferences objects in case populate* changed a value
        mUserPrefs = new UserPrefs( this );
        mGamePrefs = new GamePrefs( this, mRomDetail.md5 );
        
        // Set cheats screen summary text
        mScreenCheats.setSummary( mGamePrefs.isCheatOptionsShown
                ? R.string.screenCheats_summaryEnabled
                : R.string.screenCheats_summaryDisabled );
        
        // Enable/disable player map item as necessary
        PrefUtil.enablePreference( this, PLAYER_MAP, mGamePrefs.playerMap.isEnabled() );
        
        // Define which buttons to show in player map dialog
        @SuppressWarnings( "deprecation" )
        PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( PLAYER_MAP );
        if( playerPref != null )
        {
            // Check null in case preference has been removed
            boolean enable1 = mGamePrefs.isControllerEnabled1;
            boolean enable2 = mGamePrefs.isControllerEnabled2 && mRomDetail.players > 1;
            boolean enable3 = mGamePrefs.isControllerEnabled3 && mRomDetail.players > 2;
            boolean enable4 = mGamePrefs.isControllerEnabled4 && mRomDetail.players > 3;
            playerPref.setControllersEnabled( enable1, enable2, enable3, enable4 );
        }
        
        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }
    
    private void refreshCheatsCategory()
    {
        if( mGamePrefs.isCheatOptionsShown )
        {
            // Populate menu items
            buildCheatsCategory( mRomDetail.crc );
            
            // Show the cheats category
            mScreenCheats.addPreference( mCategoryCheats );
        }
        else
        {
            // Hide the cheats category
            mScreenCheats.removePreference( mCategoryCheats );
        }
    }
    
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        String key = preference.getKey();
        if( key.equals( ACTION_RESUME ) )
        {
            launchGame( false );
            return true;
        }
        else if( key.equals( ACTION_RESTART ) )
        {
            CharSequence title = getText( R.string.confirm_title );
            CharSequence message = getText( R.string.confirmResetGame_message );
            Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
            {
                @Override
                public void onConfirm()
                {
                    launchGame( true );
                }
            } );
            return true;
        }
        else if( key.equals( ACTION_CHEAT_EDITOR ) )
        {
            Intent intent = new Intent( this, CheatEditorActivity.class );
            intent.putExtra( Keys.Extras.ROM_PATH, mRomPath );
            startActivityForResult( intent, 111 );
        }
        else if( key.equals( ACTION_WIKI ) )
        {
            Utility.launchUri( this, mRomDetail.wikiUrl );
        }
        return false;
    }
    
    private void buildCheatsCategory( final String crc )
    {
        mCategoryCheats.removeAll();
        
        Log.v( "PlayMenuActivity", "building from CRC = " + crc );
        if( crc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
        CheatSection cheatSection = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( cheatSection == null )
        {
            Log.w( "PlayMenuActivity", "No cheat section found for '" + crc + "'" );
            return;
        }
        ArrayList<Cheat> cheats = new ArrayList<Cheat>();
        cheats.addAll( CheatUtils.populate( crc, mupencheat_txt, true, this ) );
        CheatUtils.reset();
        
        // Layout the menu, populating it with appropriate cheat options
        for( int i = 0; i < cheats.size(); i++ )
        {
            // Get the short title of the cheat (shown in the menu)
            String title;
            if( cheats.get( i ).name == null )
            {
                // Title not available, just use a default string for the menu
                title = getString( R.string.cheats_defaultName, i );
            }
            else
            {
                // Title available, remove the leading/trailing quotation marks
                title = cheats.get( i ).name;
            }
            String notes = cheats.get( i ).desc;
            String options = cheats.get( i ).option;
            String[] optionStrings = null;
            if( !TextUtils.isEmpty( options ) )
            {
                optionStrings = options.split( "\n" );
            }
            
            // Create the menu item associated with this cheat
            CheatPreference pref = new CheatPreference( this, title, notes, optionStrings );
            pref.setKey( crc + " Cheat" + i );
            
            // Add the preference menu item to the cheats category
            mCategoryCheats.addPreference( pref );
        }
    }
    
    private void launchGame( boolean isRestarting )
    {
        // Popup the multi-player dialog if necessary and abort if any players are unassigned
        if( mRomDetail.players > 1 && mGamePrefs.playerMap.isEnabled()
                && mUserPrefs.getPlayerMapReminder() )
        {
            mGamePrefs.playerMap.removeUnavailableMappings();
            boolean needs1 = mGamePrefs.isControllerEnabled1 && !mGamePrefs.playerMap.isMapped( 1 );
            boolean needs2 = mGamePrefs.isControllerEnabled2 && !mGamePrefs.playerMap.isMapped( 2 );
            boolean needs3 = mGamePrefs.isControllerEnabled3 && !mGamePrefs.playerMap.isMapped( 3 )
                    && mRomDetail.players > 2;
            boolean needs4 = mGamePrefs.isControllerEnabled4 && !mGamePrefs.playerMap.isMapped( 4 )
                    && mRomDetail.players > 3;
            
            if( needs1 || needs2 || needs3 || needs4 )
            {
                @SuppressWarnings( "deprecation" )
                PlayerMapPreference pref = (PlayerMapPreference) findPreference( "playerMap" );
                pref.show();
                return;
            }
        }
        
        // Make sure that the storage is accessible
        if( !mAppData.isSdCardAccessible() )
        {
            Log.e( "CheatMenuHandler", "SD Card not accessible in method onPreferenceClick" );
            Notifier.showToast( this, R.string.toast_sdInaccessible );
            return;
        }
        
        // Notify user that the game activity is starting
        Notifier.showToast( this, R.string.toast_launchingEmulator );
        
        // Launch the appropriate game activity
        Intent intent = mUserPrefs.isTouchpadEnabled ? new Intent( this,
                GameActivityXperiaPlay.class ) : new Intent( this, GameActivity.class );
        
        // Pass the startup info via the intent
        intent.putExtra( Keys.Extras.ROM_PATH, mRomPath );
        intent.putExtra( Keys.Extras.ROM_MD5, mRomDetail.md5 );
        intent.putExtra( Keys.Extras.CHEAT_ARGS, getCheatArgs() );
        intent.putExtra( Keys.Extras.DO_RESTART, isRestarting );
        
        startActivity( intent );
    }
    
    @SuppressWarnings( "deprecation" )
    private String getCheatArgs()
    {
        String cheatArgs = null;
        
        PreferenceCategory cheatsCategory = (PreferenceCategory) findPreference( CATEGORY_CHEATS );
        if( cheatsCategory != null )
        {
            for( int i = 0; i < cheatsCategory.getPreferenceCount(); i++ )
            {
                CheatPreference pref = (CheatPreference) cheatsCategory.getPreference( i );
                if( pref.isCheatEnabled() )
                {
                    if( cheatArgs == null )
                        cheatArgs = ""; // First time through
                    else
                        cheatArgs += ",";
                    
                    cheatArgs += pref.getCheatCodeString( i );
                }
            }
        }
        
        return cheatArgs;
    }
}
