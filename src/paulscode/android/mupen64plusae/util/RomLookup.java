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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

public class RomLookup
{
    private static final String URL_TEMPLATE = "https://dl.dropboxusercontent.com/u/3899306/CoverArt/%s.png";
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
    
    public Bitmap getCoverArt( String crc, boolean download )
    {
        // TODO: Get cached data if download == false
        
        String name = getBaseGoodName( crc );
        if( TextUtils.isEmpty( name ) )
            return null;
        
        name = name.trim().replace( ' ', '_' ).replace( "'", "" );
        
        URL url = null;
        URLConnection connection = null;
        InputStream stream = null;
        Bitmap bitmap = null;
        try
        {
            url = new URL( String.format( URL_TEMPLATE, name ) );
            connection = url.openConnection();
            stream = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream( stream );
        }
        catch( MalformedURLException e )
        {
            Log.w( "RomItem", "MalformedURLException: ", e );
        }
        catch( IOException e )
        {
            Log.w( "RomItem", "IOException: ", e );
        }
        finally
        {
            try
            {
                if( stream != null )
                    stream.close();
            }
            catch( IOException e )
            {
                Log.w( "RomItem", "IOException on close: ", e );
            }
        }
        
        return bitmap;
    }
}
