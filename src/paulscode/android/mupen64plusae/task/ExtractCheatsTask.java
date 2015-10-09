/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2015 Paul Lamb
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
 * Authors: fzurita
 */
package paulscode.android.mupen64plusae.task;

import java.io.BufferedReader;
import java.util.ArrayList;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.persistent.AppData;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;

public class ExtractCheatsTask extends AsyncTask<String, String, String>
{
    private final Context mContext;
    private final AppData mAppData;
    private final String mCrc;
    private final PreferenceGroup mPreferenceGroup;
    private final PreferenceGroup mCategoryCheats;
    private final ArrayList<Cheat> mCheats;
    private final boolean mIsCheatOptionsShown;

    
    public ExtractCheatsTask( Context context, AppData appData,
        String crc, PreferenceGroup preferenceGroup, PreferenceGroup categoryCheats, boolean isCheatOptionsShown)
    {
        mContext = context;
        mAppData = appData;
        mCrc = crc;
        mPreferenceGroup = preferenceGroup;
        mCategoryCheats = categoryCheats;
        mCheats = new ArrayList<Cheat>();
        mIsCheatOptionsShown = isCheatOptionsShown;
    }

    @Override
    protected String doInBackground(String... params)
    {
        if(mIsCheatOptionsShown)
        {
            buildCheatsCategory();
            mPreferenceGroup.addPreference( mCategoryCheats );
        }
        else
        {
            mPreferenceGroup.removePreference( mCategoryCheats );
        }

        
        return null;
    }
    
    private void buildCheatsCategory()
    {
        mCategoryCheats.removeAll();
        
        Log.v( "GamePrefsActivity", "building from CRC = " + mCrc );
        if( mCrc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        String regularExpression = "^" + mCrc.replace( ' ', '-' ) + ".*";
        
        BufferedReader cheatLocation = CheatUtils.getCheatsLocation(regularExpression, mAppData.mupencheat_txt);
        if( cheatLocation == null  )
        {
            Log.w( "GamePrefsActivity", "No cheat section found for '" + mCrc + "'" );
            return;
        }

        mCheats.addAll( CheatUtils.populateWithPosition( cheatLocation, mCrc, true, mContext ) );
        CheatUtils.reset();
        
        // Layout the menu, populating it with appropriate cheat options
        for( int i = 0; i < mCheats.size(); i++ )
        {
            // Get the short title of the cheat (shown in the menu)
            String title;
            if( mCheats.get( i ).name == null )
            {
                // Title not available, just use a default string for the menu
                title = mContext.getString( R.string.cheats_defaultName, i );
            }
            else
            {
                // Title available, remove the leading/trailing quotation marks
                title = mCheats.get( i ).name;
            }
            String notes = mCheats.get( i ).desc;
            String options = mCheats.get( i ).option;
            String[] optionStrings = null;
            if( !TextUtils.isEmpty( options ) )
            {
                optionStrings = options.split( "\n" );
            }
            
            // Create the menu item associated with this cheat
            CheatPreference pref = new CheatPreference( mContext, title, notes, optionStrings );
            pref.setKey( mCrc + " Cheat" + i );
            
            // Add the preference menu item to the cheats category
            mCategoryCheats.addPreference( pref );
        }
    }
    
    @Override
    protected void onPostExecute( String result )
    {        
        
    }

}