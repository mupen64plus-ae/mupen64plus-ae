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
package paulscode.android.mupen64plusae.util;

import java.util.HashMap;

import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import android.util.Log;

public class RomLookup
{
    private final HashMap<String, ConfigSection> mCrcMap = new HashMap<String, ConfigSection>();
    
    public RomLookup( String mupen64plus_ini )
    {
        ConfigFile cfg = new ConfigFile( mupen64plus_ini );
        for( String key : cfg.keySet() )
        {
            ConfigSection section = cfg.get( key );
            if( section != null )
            {
                String crc = section.get( "CRC" );
                if( crc != null )
                {
                    if( mCrcMap.get( crc ) == null )
                        mCrcMap.put( crc, section );
                }
            }
        }
        Log.i( "RomLookup", "size = " + mCrcMap.size() );
    }
    
    public String getBaseGoodName( String crc )
    {
        // This special CRC belongs to many homebrew roms; just return empty string
        if( "00000000 00000000".equals( crc ) )
            return "";
        
        Log.i( "RomLookup", "Looking up crc = " + crc );
        ConfigSection sec = mCrcMap.get( crc );
        if( sec == null )
            return null;
        
        Log.i( "RomLookup", "Found config section = " + sec.name );
        String name = sec.get( "GoodName" );
        if( name == null )
            return null;
        
        Log.i( "RomLookup", "Found good name = " + name );
        return name.split( " \\(" )[0];
    }
}
