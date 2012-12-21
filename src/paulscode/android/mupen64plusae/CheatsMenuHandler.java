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

import java.util.HashMap;
import java.util.LinkedList;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.LongClickCheckBoxPreference;
import paulscode.android.mupen64plusae.persistent.OnPreferenceLongClickListener;
import paulscode.android.mupen64plusae.persistent.OptionCheckBoxPreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.TaskHandler;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.AlertDialog;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

public class CheatsMenuHandler implements OnPreferenceClickListener, OnPreferenceLongClickListener
{
    private static final String MENU_CHEATS = "menuCheats";
    private static final String CHEATS_LAUNCH = "cheatsLaunch";
    private static final String CHEATS_CATEGORY = "cheatsCategory";
    
    public static String ROM = null;
    public static String CRC = null;
    
    private static HashMap<String, String> Cheat_title = null;
    private static HashMap<String, String> Cheat_N = null;
    private static HashMap<String, String> Cheat_O = null;

    // Storing them here, since they get lost on orientation change
    private static LinkedList<Preference> cheatPreferences = new LinkedList<Preference>();

    // Used when concatenating the extra args string
    public static String cheatOptions = null;
    
    private MenuActivity mActivity = null;
    private PreferenceScreen cheatsScreen = null;
    private PreferenceCategory cheatsCategory = null;
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;

    public CheatsMenuHandler( MenuActivity activity, AppData appData, UserPrefs userPrefs )
    {
        mActivity = activity;
        // Get app data and user preferences
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
            TaskHandler.run
            (
                mActivity, mActivity.getString( R.string.cheatsTaskHandler_title ),
                mActivity.getString( R.string.cheatsTaskHandler_message ),
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

        cheatsScreen = (PreferenceScreen) mActivity.findPreference( MENU_CHEATS );
        mActivity.findPreference( CHEATS_LAUNCH ).setOnPreferenceClickListener( this );
        cheatsCategory = (PreferenceCategory) mActivity.findPreference( CHEATS_CATEGORY );
        
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
        cheatsScreen = (PreferenceScreen) mActivity.findPreference( MENU_CHEATS );
        mActivity.findPreference( CHEATS_LAUNCH ).setOnPreferenceClickListener( this );
        cheatsCategory = (PreferenceCategory) mActivity.findPreference( CHEATS_CATEGORY );
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
            if( Utility.isNullOrEmpty( ROM_name ) )
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
            for( int i = 0; !Utility.isNullOrEmpty( val ); i++ )
            {
                val = configSection.get( "Cheat" + i );
                if( !Utility.isNullOrEmpty( val ) )
                {
                    x = val.indexOf( "," );
                    if( x < 3 || x >= val.length() )
                        title = mActivity.getString( R.string.cheats_defaultName, i );
                    else
                        title = val.substring( 1, x - 1 );
                    Cheat_title.put( "Cheat" + i, title );
                    
                    val_N = configSection.get( "Cheat" + i + "_N" );
                    if( !Utility.isNullOrEmpty( val_N ) )
                        Cheat_N.put( "Cheat" + i, val_N );
                    
                    val_O = configSection.get( "Cheat" + i + "_O" );
                    if( !Utility.isNullOrEmpty( val_O ) )
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
                    checkBoxPref.setLongClickListener( this );
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
            if( Utility.isNullOrEmpty( title ) )
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
            if( Utility.isNullOrEmpty( summary ) )
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
        if( key.equals( CHEATS_LAUNCH ) )
        {
            // Launch game with selected cheats
            String cheatArgs = null;
            CheckBoxPreference chkBx;
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
            if( cheatArgs != null )
                cheatOptions = "--cheats " + cheatArgs;
            else
                cheatOptions = null;
            
            // Launch the last game in a new activity
            if( !mAppData.isSdCardAccessible() )
            {
                Log.e( "CheatMenuHandler", "SD Card not accessible in method onPreferenceClick" );
                Notifier.showToast( mActivity, R.string.toast_sdInaccessible );
                return true;
            }
            
            // Notify user that the game activity is starting
            Notifier.showToast( mActivity, R.string.toast_appStarted );
            
            // Launch the appropriate game activity
            Intent intent = new UserPrefs( mActivity ).isXperiaEnabled
                    ? new Intent( mActivity, GameActivityXperiaPlay.class )
                    : new Intent( mActivity, GameActivity.class );
            
            // TODO: Reconstruct the cheats menu after game closes, rather than reloading the entire menu
            mActivity.finish();
            mActivity.startActivity( mActivity.getIntent() );
            
            mActivity.startActivity( intent );
            return true;
        }
        return false;
    }
}
