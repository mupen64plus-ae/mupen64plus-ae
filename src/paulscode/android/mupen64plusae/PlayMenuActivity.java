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

import java.io.File;
import java.util.LinkedList;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.CheatFile;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatBlock;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatCode;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatOption;
import paulscode.android.mupen64plusae.persistent.CheatFile.CheatSection;
import paulscode.android.mupen64plusae.persistent.CheatPreference;
import paulscode.android.mupen64plusae.persistent.PlayerMapPreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.bda.controller.Controller;

public class PlayMenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    public static final String EXTRA_MD5 = "EXTRA_MD5";
    
    // These constants must match the keys used in res/xml/preferences_play.xml
    private static final String SCREEN_PLAY_MENU_ACTIVITY = "screenPlayMenuActivity";
    private static final String ACTION_RESUME = "actionResume";
    private static final String ACTION_RESTART = "actionRestart";
    private static final String PLAYER_MAP = "playerMap";
    private static final String PLAY_SHOW_CHEATS = "playShowCheats";
    private static final String CATEGORY_CHEATS = "categoryCheats";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    
    // ROM info
    private RomDetail mRomDetail = null;
    
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
        
        // Get the ROM's MD5 that was passed to the activity
        Bundle extras = getIntent().getExtras();
        String md5 = null;
        if( extras == null || TextUtils.isEmpty( md5 = extras.getString( EXTRA_MD5 ) ) )
            throw new Error( "MD5 must be passed via the extras bundle when starting PlayMenuActivity" );
        
        // Get the detailed info about the ROM
        mRomDetail = RomDetail.lookupByMd5( md5 );
        setTitle( mRomDetail.goodName );
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences_play );
        
        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESUME, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESTART, this );
        
        // Hide the multi-player menu if not needed
        if( !mUserPrefs.playerMap.isEnabled() )
        {
            PrefUtil.removePreference( this, SCREEN_PLAY_MENU_ACTIVITY, PLAYER_MAP );
        }
        else
        {
            PlayerMapPreference preference = (PlayerMapPreference) findPreference( PLAYER_MAP );
            preference.setMogaController( mMogaController );
        }
        
        // Hide or populate the cheats category depending on user preference
        if( mUserPrefs.isCheatOptionsShown )
        {
            // Populate cheats category with menu items
            build( mRomDetail.crc );
        }
        else
        {
            // Hide the cheats category
            PrefUtil.removePreference( this, SCREEN_PLAY_MENU_ACTIVITY, CATEGORY_CHEATS );
        }
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        sharedPreferences.registerOnSharedPreferenceChangeListener( this );
        mMogaController.onResume();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        sharedPreferences.unregisterOnSharedPreferenceChangeListener( this );
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
        if( key.equals( PLAY_SHOW_CHEATS ) )
        {
            // Rebuild the menu; the easiest way is to simply restart the activity
            startActivity( getIntent() );
            finish();
        }
        mUserPrefs = new UserPrefs( this );
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
        return false;
    }
    
    @SuppressWarnings( "deprecation" )
    private void build( final String crc )
    {
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
        PreferenceCategory cheatsCategory = (PreferenceCategory) findPreference( CATEGORY_CHEATS );
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
                cheatsCategory.addPreference( pref );
            }
        }
    }
    
    private void launchGame( boolean isRestarting )
    {
        // Popup the multi-player dialog and abort if any players don't have a controller assigned
        if( mUserPrefs.playerMap.isEnabled() && mUserPrefs.getPlayerMapReminder() )
        {
            mUserPrefs.playerMap.removeUnavailableMappings();
            boolean needs1 = mUserPrefs.isControllerEnabled1 && !mUserPrefs.playerMap.isMapped( 1 );
            boolean needs2 = mUserPrefs.isControllerEnabled2 && !mUserPrefs.playerMap.isMapped( 2 );
            boolean needs3 = mUserPrefs.isControllerEnabled3 && !mUserPrefs.playerMap.isMapped( 3 );
            boolean needs4 = mUserPrefs.isControllerEnabled4 && !mUserPrefs.playerMap.isMapped( 4 );
            
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
        
        // Make sure that the game save subdirectories exist so that we can write to them
        UserPrefs userPrefs = new UserPrefs( this );
        new File( userPrefs.manualSaveDir ).mkdirs();
        new File( userPrefs.slotSaveDir ).mkdirs();
        new File( userPrefs.sramSaveDir ).mkdirs();
        new File( userPrefs.autoSaveDir ).mkdirs();
        new File( userPrefs.coreUserConfigDir ).mkdirs();
        new File( userPrefs.coreUserDataDir ).mkdirs();
        new File( userPrefs.coreUserCacheDir ).mkdirs();
        
        // Notify user that the game activity is starting
        Notifier.showToast( this, R.string.toast_launchingEmulator );
        
        // Launch the appropriate game activity
        Intent intent = userPrefs.isTouchpadEnabled ? new Intent( this,
                GameActivityXperiaPlay.class ) : new Intent( this, GameActivity.class );
        
        // Pass the startup info via the intent
        intent.putExtra( GameActivity.EXTRA_CHEAT_ARGS, getCheatArgs() );
        intent.putExtra( GameActivity.EXTRA_DO_RESTART, isRestarting );
        
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
