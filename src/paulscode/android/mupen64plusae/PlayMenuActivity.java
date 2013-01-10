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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.LongClickCheckBoxPreference;
import paulscode.android.mupen64plusae.persistent.LongClickCheckBoxPreference.OnPreferenceLongClickListener;
import paulscode.android.mupen64plusae.persistent.OptionCheckBoxPreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.TaskHandler;
import paulscode.android.mupen64plusae.util.TaskHandler.Task;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;

public class PlayMenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnPreferenceLongClickListener
{
    // These constants must match the keys used in res/xml/preferences_play.xml
    private static final String SCREEN_PLAY = "screenPlay";
    private static final String ACTION_RESUME = "actionResume";
    private static final String ACTION_RESTART = "actionRestart";
    private static final String PLAYER_MAP = "playerMap";
    private static final String CATEGORY_CHEATS = "categoryCheats";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    
    // Handle to the thread populating the cheat options
    @SuppressWarnings( "unused" )
    private Thread crcThread = null;
    
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
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences_play );
        
        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESUME, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESTART, this );
        
        // Hide the multi-player menu if not needed
        if( !mUserPrefs.playerMap.isEnabled() )
            PrefUtil.removePreference( this, SCREEN_PLAY, PLAYER_MAP );
        
        // Populate cheats category with menu items
        if( mUserPrefs.selectedGame.equals( mAppData.getLastRom() ) )
        {
            // Use the cached CRC and add the cheats menu items
            build( mAppData.getLastCrc() );
        }
        else
        {
            // Recompute the CRC in a separate thread, then add the cheats menu items
            rebuild( mUserPrefs.selectedGame );
        }
    }
    
    @Override
    public void onPreferenceLongClick( Preference preference )
    {
        LongClickCheckBoxPreference checkBoxPref = (LongClickCheckBoxPreference) preference;
        
        // Determine the title
        String title = checkBoxPref.getDialogTitle();
        if( TextUtils.isEmpty( title ) )
        {
            title = getString( R.string.cheatNotes_title );
        }
        
        // Determine the summary
        String summary = checkBoxPref.getDialogMessage();
        if( TextUtils.isEmpty( summary ) )
        {
            summary = getString( R.string.cheatNotes_none );
        }
        
        // Popup a dialog to display the cheat notes
        new Builder( this ).setTitle( title ).setMessage( summary ).create().show();
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
            Prompt.promptConfirm( this, title, message, new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        launchGame( true );
                    }
                }
            } );
            return true;
        }
        return false;
    }
    
    private void rebuild( final String rom )
    {
        Log.v( "PlayMenuActivity", "rebuilding for ROM = " + rom );
        Notifier.showToast( PlayMenuActivity.this, R.string.toast_rebuildingCheats );
        
        // Place to unzip the ROM if necessary
        final String tmpFolderName = mAppData.dataDir + "/tmp";
        
        // Define the task to be done on a separate thread
        Task task = new Task()
        {
            private String crc;
            
            @Override
            public void run()
            {
                crc = Utility.getHeaderCRC( rom, tmpFolderName );
            }
            
            @Override
            public void onComplete()
            {
                mAppData.putLastRom( rom );
                mAppData.putLastCrc( crc );
                build( crc );
            }
        };
        
        // Run the task on a separate thread
        crcThread = TaskHandler.run( this, getString( R.string.cheatsTaskHandler_title ), task );
    }
    
    @SuppressWarnings( "deprecation" )
    private void build( final String crc )
    {
        Log.v( "PlayMenuActivity", "building from CRC = " + crc );
        
        if( crc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        ConfigFile mupen64plus_cht = new ConfigFile( mAppData.dataDir + "/data/mupen64plus.cht" );
        ConfigSection configSection = mupen64plus_cht.match( "^" + crc.replace( ' ', '.' ) + ".*" );
        
        if( configSection == null )
        {
            Log.w( "PlayMenuActivity", "No cheat section found for '" + crc + "'" );
            return;
        }
        
        // Set the title of the menu to the game name, if available
        String ROM_name = configSection.get( "Name" );
        if( !TextUtils.isEmpty( ROM_name ) )
            setTitle( ROM_name );
        
        // Layout the menu, populating it with appropriate cheat options
        PreferenceCategory cheatsCategory = (PreferenceCategory) findPreference( CATEGORY_CHEATS );
        String cheat = " ";
        for( int i = 0; !TextUtils.isEmpty( cheat ); i++ )
        {
            cheat = configSection.get( "Cheat" + i );
            if( !TextUtils.isEmpty( cheat ) )
            {
                // Get the short title of the cheat (shown in the menu)
                int x = cheat.indexOf( "," );
                String title;
                if( x < 3 || x >= cheat.length() )
                {
                    // Title not available, just use a default string for the menu
                    title = getString( R.string.cheats_defaultName, i );
                }
                else
                {
                    // Title available, remove the leading/trailing quotation marks
                    title = cheat.substring( 1, x - 1 );
                }
                
                // Get the descriptive note for this cheat (shown on long-click)
                final String note = configSection.get( "Cheat" + i + "_N" );
                
                // Get the options for this cheat
                LongClickCheckBoxPreference checkBoxPref;
                final String val_O = configSection.get( "Cheat" + i + "_O" );
                if( TextUtils.isEmpty( val_O ) )
                {
                    // This cheat is a binary option, use a type of checkbox preference
                    checkBoxPref = new LongClickCheckBoxPreference( this );
                }
                else
                {
                    // Parse the comma-delimited string to get the map elements
                    String[] uOpts = val_O.split( "," );
                    
                    // Each element is a key-value pair
                    String[] optionStrings = new String[uOpts.length];
                    for( int z = 0; z < uOpts.length; z++ )
                    {
                        // The first non-leading space character is the pair delimiter
                        optionStrings[z] = uOpts[z].trim();
                        int c = optionStrings[z].indexOf( " " );
                        if( c > -1 && c < optionStrings[z].length() - 1 )
                            optionStrings[z] = optionStrings[z].substring( c + 1 );
                        else
                            optionStrings[z] = getString( R.string.cheats_longPress );
                    }
                    
                    // Create the menu item associated with this cheat
                    checkBoxPref = new OptionCheckBoxPreference( this, title, optionStrings,
                            getString( R.string.cheat_disabled ) );
                }
                
                // Set the preference menu item properties
                checkBoxPref.setTitle( title );
                checkBoxPref.setDialogTitle( title );
                checkBoxPref.setDialogMessage( note );
                checkBoxPref.setChecked( false );
                checkBoxPref.setKey( crc + " Cheat" + i );
                checkBoxPref.setOnPreferenceLongClickListener( this );
                
                // Add the preference menu item to the cheats category
                cheatsCategory.addPreference( checkBoxPref );
            }
        }
    }
    
    private void launchGame( boolean isRestarting )
    {
        // Although this is apparently not necessary, there is no difference in the delay by
        // commenting it out:
        // SafeMethods.join( crcThread, 0 );
        
        // Make sure that the storage is accessible
        if( !mAppData.isSdCardAccessible() )
        {
            Log.e( "CheatMenuHandler", "SD Card not accessible in method onPreferenceClick" );
            Notifier.showToast( this, R.string.toast_sdInaccessible );
            return;
        }
        
        // Make sure that the game save subdirectories exist so that we can write to them
        new File( mUserPrefs.manualSaveDir ).mkdirs();
        new File( mUserPrefs.slotSaveDir ).mkdirs();
        new File( mUserPrefs.autoSaveDir ).mkdirs();
        
        // Notify user that the game activity is starting
        Notifier.showToast( this, R.string.toast_launchingEmulator );
        
        // Set the startup mode for the core
        CoreInterface.setStartupMode( getCheatArgs(), isRestarting );
        
        // Launch the appropriate game activity
        Intent intent = mUserPrefs.isTouchpadEnabled ? new Intent( this,
                GameActivityXperiaPlay.class ) : new Intent( this, GameActivity.class );
        
        startActivity( intent );
    }
    
    @SuppressWarnings( "deprecation" )
    private String getCheatArgs()
    {
        PreferenceCategory cheatsCategory = (PreferenceCategory) findPreference( CATEGORY_CHEATS );
        
        String cheatArgs = null;
        for( int i = 0; i < cheatsCategory.getPreferenceCount(); i++ )
        {
            CheckBoxPreference chkBx = (CheckBoxPreference) cheatsCategory.getPreference( i );
            if( chkBx.isChecked() )
            {
                if( cheatArgs == null )
                    cheatArgs = "";
                else
                    cheatArgs += ",";
                
                cheatArgs += String.valueOf( i );
                
                if( chkBx instanceof OptionCheckBoxPreference )
                    cheatArgs += "-" + ( (OptionCheckBoxPreference) chkBx ).mChoice;
            }
        }
        return cheatArgs;
    }
}
