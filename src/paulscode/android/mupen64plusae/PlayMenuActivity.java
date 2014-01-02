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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import paulscode.android.mupen64plusae.cheat.CheatEditorActivity;
import paulscode.android.mupen64plusae.cheat.CheatFile;
import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatBlock;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatCode;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatOption;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatSection;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.CompatibleListPreference;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.PlayerMapPreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.util.RomDetail;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
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
    
    private static final String CONTROLLER_PROFILE1 = "controllerProfile1";
    private static final String CONTROLLER_PROFILE2 = "controllerProfile2";
    private static final String CONTROLLER_PROFILE3 = "controllerProfile3";
    private static final String CONTROLLER_PROFILE4 = "controllerProfile4";
    private static final String PLAYER_MAP = "playerMap";
    private static final String PLAY_SHOW_CHEATS = "playShowCheats";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private SharedPreferences mPrefs = null;
    
    // ROM info
    private String mRomPath = null;
    private RomDetail mRomDetail = null;
    
    // Preference menu items
    PreferenceGroup mScreenCheats = null;
    PreferenceGroup mCategoryCheats = null;
    
    // MOGA controller interface
    private Controller mMogaController = Controller.getInstance( this );
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Initialize MOGA controller API
        mMogaController.init();
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
        
        // Get the ROM path and MD5 that was passed to the activity
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle when starting PlayMenuActivity" );
        mRomPath = extras.getString( Keys.Extras.ROM_PATH );
        final String md5 = extras.getString( Keys.Extras.ROM_MD5 );
        if( TextUtils.isEmpty( mRomPath ) || TextUtils.isEmpty( md5 ) )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle when starting PlayMenuActivity" );
        
        // Get the detailed info about the ROM
        mRomDetail = RomDetail.lookupByMd5( md5 );
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences_play );
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
        
        // Setup controller profiles settings based on ROM's number of players
        if( mRomDetail.players == 1 )
        {
            // Simplify name of "controller 1" to just "controller" to eliminate confusion
            findPreference(CONTROLLER_PROFILE1).setTitle( R.string.controllerProfile_title );
            
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
    public void finish()
    {
        // Disable transition animation to behave like any other screen in the menu hierarchy
        super.finish();
        overridePendingTransition( 0, 0 );
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
    
    private void refreshViews()
    {
        // Refresh the preferences object
        mUserPrefs = new UserPrefs( this );
        
        // Enable/disable player map item as necessary
        PrefUtil.enablePreference( this, PLAYER_MAP, mUserPrefs.playerMap.isEnabled() );
        
        // Set cheats screen summary text
        mScreenCheats.setSummary( mUserPrefs.isCheatOptionsShown
                ? R.string.screenCheats_summaryEnabled
                : R.string.screenCheats_summaryDisabled );
        
        // Construct the controller profiles list
        ConfigFile configBuiltin = new ConfigFile( mAppData.controllerProfiles_cfg );
        ConfigFile configCustom = new ConfigFile( mUserPrefs.controllerProfiles_cfg );
        List<ControllerProfile> profiles = new ArrayList<ControllerProfile>();
        profiles.addAll( ControllerProfile.getProfiles( configBuiltin, true ) );
        profiles.addAll( ControllerProfile.getProfiles( configCustom, false ) );
        Collections.sort( profiles );
        CharSequence[] entries = new CharSequence[profiles.size() + 1];
        String[] values = new String[profiles.size() + 1];
        entries[0] = getText( R.string.listItem_disabled );
        values[0] = "";
        for( int i = 0; i < profiles.size(); i++ )
        {
            ControllerProfile profile = profiles.get( i );
            int resId = profile.isBuiltin
                    ? R.string.listItem_profileBuiltin
                    : R.string.listItem_profileCustom;
            entries[i + 1] = getString( resId, profile.name );
            values[i + 1] = profile.name;
        }
        
        // Populate and validate the controller profile preferences
        populateListPreference( entries, values, CONTROLLER_PROFILE1,
                R.string.controllerProfile_default );
        populateListPreference( entries, values, CONTROLLER_PROFILE2,
                R.string.controllerProfile_default );
        populateListPreference( entries, values, CONTROLLER_PROFILE3,
                R.string.controllerProfile_default );
        populateListPreference( entries, values, CONTROLLER_PROFILE4,
                R.string.controllerProfile_default );
        
        // Refresh the preferences object in case populate* changed a value
        mUserPrefs = new UserPrefs( this );
    }
    
    private void populateListPreference( CharSequence[] entries, String[] values, String key,
            int resIdDefault )
    {
        @SuppressWarnings( "deprecation" )
        CompatibleListPreference listPref = (CompatibleListPreference) findPreference( key );
        if( listPref != null )
        {
            listPref.setEntries( entries );
            listPref.setEntryValues( values );
            String selectedValue = mPrefs.getString( key, null );
            String defaultValue = getString( resIdDefault );
            if( !ArrayUtils.contains( values, selectedValue ) )
                mPrefs.edit().putString( key, defaultValue ).commit();
            selectedValue = mPrefs.getString( key, null );
            listPref.setValue( selectedValue );
        }
    }
    
    private void refreshCheatsCategory()
    {
        if( mUserPrefs.isCheatOptionsShown )
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
            startActivity( intent );
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
        
        // Layout the menu, populating it with appropriate cheat options
        CheatBlock cheat;
        for( int i = 0; i < cheatSection.size(); i++ )
        {
            cheat = cheatSection.get( i );
            if( cheat != null )
            {
                // Get the short title of the cheat (shown in the menu)
                String title;
                if( cheat.name == null )
                {
                    // Title not available, just use a default string for the menu
                    title = getString( R.string.cheats_defaultName, i );
                }
                else
                {
                    // Title available, remove the leading/trailing quotation marks
                    title = cheat.name;
                }
                
                // Get the descriptive note for this cheat (shown on long-click)
                final String notes = cheat.description;
                
                // Get the options for this cheat
                LinkedList<CheatCode> codes = new LinkedList<CheatCode>();
                LinkedList<CheatOption> options = new LinkedList<CheatOption>();
                for( int o = 0; o < cheat.size(); o++ )
                {
                    codes.add( cheat.get( o ) );
                }
                /*
                 * There shouldn't be more than one set of options per cheat so why do we need to
                 * recurse individual codes?
                 */
                for( int o = 0; o < codes.size(); o++ )
                {
                    if( codes.get( o ).options != null )
                    {
                        options = codes.get( o ).options;
                    }
                }
                String[] optionStrings = null;
                if( options != null && !options.isEmpty() )
                {
                    // This is a multi-choice cheat
                    optionStrings = new String[options.size()];
                    
                    // Each element is a key-value pair
                    for( int z = 0; z < options.size(); z++ )
                    {
                        // The first non-leading space character is the pair delimiter
                        optionStrings[z] = options.get( z ).name;
                        if( TextUtils.isEmpty( optionStrings[z] ) )
                            optionStrings[z] = getString( R.string.cheats_longPress );
                    }
                }
                
                // Create the menu item associated with this cheat
                CheatPreference pref = new CheatPreference( this, title, notes, optionStrings );
                pref.setKey( crc + " Cheat" + i );
                
                // Add the preference menu item to the cheats category
                mCategoryCheats.addPreference( pref );
            }
        }
    }
    
    private void launchGame( boolean isRestarting )
    {
        // Popup the multi-player dialog if necessary and abort if any players don't have a controller assigned
        if( mRomDetail.players > 1 && mUserPrefs.playerMap.isEnabled()
                && mUserPrefs.getPlayerMapReminder() )
        {
            mUserPrefs.playerMap.removeUnavailableMappings();
            boolean needs1 = mUserPrefs.isControllerEnabled1 && !mUserPrefs.playerMap.isMapped( 1 );
            boolean needs2 = mUserPrefs.isControllerEnabled2 && !mUserPrefs.playerMap.isMapped( 2 );
            boolean needs3 = mUserPrefs.isControllerEnabled3 && !mUserPrefs.playerMap.isMapped( 3 ) && mRomDetail.players > 2;
            boolean needs4 = mUserPrefs.isControllerEnabled4 && !mUserPrefs.playerMap.isMapped( 4 ) && mRomDetail.players > 3;
            
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
