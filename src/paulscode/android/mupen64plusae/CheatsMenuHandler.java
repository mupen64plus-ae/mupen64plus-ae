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
import java.util.HashMap;
import java.util.LinkedList;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.LongClickCheckBoxPreference;
import paulscode.android.mupen64plusae.persistent.LongClickCheckBoxPreference.OnPreferenceLongClickListener;
import paulscode.android.mupen64plusae.persistent.OptionCheckBoxPreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.TaskHandler;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

public class CheatsMenuHandler implements OnPreferenceClickListener, OnPreferenceLongClickListener
{
    private static final String SCREEN_PLAY = "screenPlay";
    private static final String ACTION_RESUME = "actionResume";
    private static final String ACTION_RESTART = "actionRestart";
    private static final String CATEGORY_CHEATS = "categoryCheats";
    
    public static String ROM = null;
    public static String CRC = null;
    
    private static HashMap<String, String> Cheat_title = null;
    private static HashMap<String, String> Cheat_N = null;
    private static HashMap<String, String> Cheat_O = null;

    // Storing them here, since they get lost on orientation change
    private static LinkedList<Preference> cheatPreferences = new LinkedList<Preference>();

    // Used when concatenating the extra args string
    public static String cheatOptions = null;
    
    // False if game should resume (default)
    public static boolean toRestart = false;
    
    private MenuActivity mActivity = null;
    private PreferenceScreen cheatsScreen = null;
    private PreferenceCategory cheatsCategory = null;
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    
    // Handle to the thread populating the cheat options
    @SuppressWarnings( "unused" )
    private Thread cheatsThread = null;
    
    public CheatsMenuHandler( MenuActivity activity, AppData appData, UserPrefs userPrefs )
    {
        mActivity = activity;
        mAppData = appData;
        mUserPrefs = userPrefs;
    }
    
    public void rebuild()
    {
        // Place to unzip the ROM if necessary
        final String tmpFolderName = mAppData.dataDir + "/tmp";

        // Path to the game selected in the user preferences
        final String selectedGame = mUserPrefs.selectedGame;
        
        // No need to reload the ROM header if we already have it
        if( CRC == null || ROM == null || !ROM.equals( mUserPrefs.selectedGame ) )
        {
            ROM = mUserPrefs.selectedGame;
            cheatsThread = TaskHandler.run
            (
                mActivity, mActivity.getString( R.string.cheatsTaskHandler_title ),
                new TaskHandler.Task()
                {
                    @Override
                    public void run()
                    {
                        CheatsMenuHandler.CRC = Utility.getHeaderCRC( selectedGame, tmpFolderName );
                    }

                    @Override
                    public void onComplete()
                    {
                        build();
                    }
                }
            );
        }
        else
        {
            refresh();
        }
    }

    @SuppressWarnings( "deprecation" )
    public void refresh()
    {
        if( ROM == null )
            return;
        cheatsScreen = (PreferenceScreen) mActivity.findPreference( SCREEN_PLAY );
        mActivity.findPreference( ACTION_RESUME ).setOnPreferenceClickListener( this );
        mActivity.findPreference( ACTION_RESTART ).setOnPreferenceClickListener( this );
        cheatsCategory = (PreferenceCategory) mActivity.findPreference( CATEGORY_CHEATS );
        
        if( cheatPreferences.size() > 0 && cheatsCategory.getPreferenceCount() == 0 )
        {
            for (Preference pref : cheatPreferences)
                cheatsCategory.addPreference( pref );
            return;
        }
        
        build();
    }

    @SuppressWarnings( "deprecation" )
    private void build()
    {
        cheatsScreen = (PreferenceScreen) mActivity.findPreference( SCREEN_PLAY );
        mActivity.findPreference( ACTION_RESUME ).setOnPreferenceClickListener( this );
        mActivity.findPreference( ACTION_RESTART ).setOnPreferenceClickListener( this );
        cheatsCategory = (PreferenceCategory) mActivity.findPreference( CATEGORY_CHEATS );
        cheatsCategory.removeAll();
        cheatPreferences.clear();
        
        if( CRC == null )
        {
            Log.e( "CheatMenuHandler", "CRC null in method refresh" );
            return;
        }

        ConfigFile mupen64plus_cht = new ConfigFile( mAppData.dataDir + "/data/mupen64plus.cht" );
        ConfigFile.ConfigSection configSection = mupen64plus_cht.match( "^" + CRC.replace( ' ', '.' ) + ".*" );

        if( configSection == null )
        {
            Log.e( "CheatMenuHandler", "No cheat section found for '" + CRC + "'" );
        }
        else
        {
            String ROM_name = configSection.get( "Name" );
            if( TextUtils.isEmpty( ROM_name ) )
            {
                cheatsScreen.setTitle( mActivity.getString( R.string.cheats_title ) );
            }
            else
            {
                cheatsScreen.setTitle( mActivity.getString( R.string.cheats_titleFor, ROM_name ) );
            }
            
            Cheat_title = new HashMap<String, String>();
            Cheat_N = new HashMap<String, String>();
            Cheat_O = new HashMap<String, String>();
            
            int x;
            String val_N, val_O, title;
            String val = " ";
            LongClickCheckBoxPreference checkBoxPref;
            for( int i = 0; !TextUtils.isEmpty( val ); i++ )
            {
                val = configSection.get( "Cheat" + i );
                if( !TextUtils.isEmpty( val ) )
                {
                    x = val.indexOf( "," );
                    if( x < 3 || x >= val.length() )
                        title = mActivity.getString( R.string.cheats_defaultName, i );
                    else
                        title = val.substring( 1, x - 1 );
                    Cheat_title.put( "Cheat" + i, title );
                    
                    val_N = configSection.get( "Cheat" + i + "_N" );
                    if( !TextUtils.isEmpty( val_N ) )
                        Cheat_N.put( "Cheat" + i, val_N );
                    
                    val_O = configSection.get( "Cheat" + i + "_O" );
                    if( !TextUtils.isEmpty( val_O ) )
                    {
                        Cheat_O.put( "Cheat" + i, val_O );

                        String[] uOpts = val_O.split( "," );
                        String[] opts = new String[ uOpts.length ];
                        int c;
                        for( int z = 0; z < uOpts.length; z++ )
                        {
                            opts[z] = uOpts[z].trim();
                            c = opts[z].indexOf( " " );
                            if( c > -1 && c < opts[z].length() - 1 )
                                opts[z] = opts[z].substring( c + 1 );
                            else
                                opts[z] = mActivity.getString( R.string.cheats_longPress );
                        }

                        checkBoxPref = new OptionCheckBoxPreference( mActivity, title, opts,
                            mActivity.getString( R.string.cheat_disabled ) );
                    }
                    else
                    {
                        checkBoxPref = new LongClickCheckBoxPreference( mActivity );
                    }
                    
                    checkBoxPref.setTitle( title );
                    checkBoxPref.setChecked( false );
                    checkBoxPref.setKey( "Cheat" + i );
                    checkBoxPref.setOnPreferenceLongClickListener( this );
                    cheatsCategory.addPreference( checkBoxPref );
                    cheatPreferences.add( checkBoxPref );
                }
            }
        }
    }
    
    @Override
    public void onPreferenceLongClick( Preference preference )
    {
        String whichCheat = preference.getKey();
        String title;   // Title of the cheat
        String summary; // Summary of the cheat
        
        // Determine the title
        if( Cheat_title == null )
        {
            title = mActivity.getString( R.string.cheatNotes_title );
        }
        else
        {
            title = Cheat_title.get( whichCheat );
            if( TextUtils.isEmpty( title ) )
            {
                title = mActivity.getString( R.string.cheatNotes_title );
            }
        }
        
        // Determine the summary
        if( Cheat_N == null )
        {
             summary = mActivity.getString( R.string.cheatNotes_none );
        }
        else
        {
            summary = Cheat_N.get( whichCheat );
            if( TextUtils.isEmpty( summary ) )
            {
                summary = mActivity.getString( R.string.cheatNotes_none );
            }
        }
        new AlertDialog.Builder( mActivity ).setTitle( title ).setMessage( summary ).create().show();
    }
    
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        String key = preference.getKey();
        if( key.equals( ACTION_RESUME ) )
        {
            toRestart = false;
            launchGame();
            return true;
        }
        else if( key.equals( ACTION_RESTART ) )
        {
            toRestart = true;
            CharSequence title = mActivity.getText( R.string.confirm_title );
            CharSequence message = mActivity.getText( R.string.confirmResetGame_message );
            Prompt.promptConfirm( mActivity, title, message, new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        launchGame();
                    }
                }
            } );
            return true;
        }
        return false;
    }

    private void launchGame()
    {
        String cheatArgs = null;
        CheckBoxPreference chkBx;
        
        // Although this is apparently not necessary, there is no difference in the delay by commenting it out:
        // SafeMethods.join( cheatsThread, 0 );
        
        for( int i = 0; i < cheatsCategory.getPreferenceCount(); i++ )
        {
            chkBx = (CheckBoxPreference) cheatsCategory.getPreference( i );
            if( chkBx.isChecked() )
            {
                if( cheatArgs == null )
                    cheatArgs = "";
                else
                    cheatArgs += ",";

                cheatArgs += String.valueOf( i );

                if( chkBx instanceof OptionCheckBoxPreference )
                    cheatArgs += "-" + ((OptionCheckBoxPreference) chkBx).mChoice;
            }
        }
        
        if( cheatArgs != null && toRestart )
            cheatOptions = "--cheats " + cheatArgs;  // Restart game with selected cheats
        else
            cheatOptions = null;
        
        // Launch the last game in a new activity
        if( !mAppData.isSdCardAccessible() )
        {
            Log.e( "CheatMenuHandler", "SD Card not accessible in method onPreferenceClick" );
            Notifier.showToast( mActivity, R.string.toast_sdInaccessible );
            return;
        }
        
        // Make sure that the game save subdirectories exist so that we can write to them
        new File( mUserPrefs.manualSaveDir ).mkdirs();
        new File( mUserPrefs.slotSaveDir ).mkdirs();
        new File( mUserPrefs.autoSaveDir ).mkdirs();
        
        // Notify user that the game activity is starting
        Notifier.showToast( mActivity, R.string.toast_launchingEmulator );
        
        // Launch the appropriate game activity
        Intent intent = new UserPrefs( mActivity ).isTouchpadEnabled
                ? new Intent( mActivity, GameActivityXperiaPlay.class )
                : new Intent( mActivity, GameActivity.class );            
        
        // TODO: Reconstruct the cheats menu after game closes, rather than reloading the entire menu
        mActivity.finish();
        mActivity.startActivity( mActivity.getIntent() );
        
        mActivity.startActivity( intent );
    }
}
