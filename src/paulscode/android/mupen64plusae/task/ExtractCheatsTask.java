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

import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class ExtractCheatsTask extends AsyncTask<String, String, String>
{
    private final Context mContext;
    private final ExtractCheatListener mExtractCheatListener;
    private final String mCheatPath;
    private final String mCrc;
    private final byte mCountryCode;
    private final ArrayList<Cheat> mCheats;
    
    public interface ExtractCheatListener
    {
        //This is called once we are done retrieving the cheats
        public void onExtractFinished(ArrayList<Cheat> cheats);
    }

    
    public ExtractCheatsTask( Context context, ExtractCheatListener extractCheatListener,
        String cheatPath, String crc, byte romCountryCode)
    {
        mContext = context;
        mExtractCheatListener = extractCheatListener;
        mCheatPath = cheatPath;
        mCrc = crc;
        mCountryCode = romCountryCode;
        mCheats = new ArrayList<Cheat>();
    }

    @Override
    protected String doInBackground(String... params)
    {
        buildCheatsCategory();

        
        return null;
    }
    
    private void buildCheatsCategory()
    {
        Log.v( "GamePrefsActivity", "building from CRC = " + mCrc );
        if( mCrc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        
        String countryString = String.format("%02x", mCountryCode).substring(0, 2);
        String regularExpression = "^" + mCrc.replace( ' ', '-') + "-C:" + countryString + ".*";
        
        BufferedReader cheatLocation = CheatUtils.getCheatsLocation(regularExpression, mCheatPath);
        if( cheatLocation == null  )
        {
            Log.w( "GamePrefsActivity", "No cheat section found for '" + mCrc + "'" );
            return;
        }

        mCheats.addAll( CheatUtils.populateWithPosition( cheatLocation, mCrc, mCountryCode, mContext ) );
    }
    
    @Override
    protected void onPostExecute( String result )
    {        
        mExtractCheatListener.onExtractFinished(mCheats);
    }

}