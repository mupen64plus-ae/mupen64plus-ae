/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;

import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

public class RomDetail
{
    public final String md5;
    public final String crc;
    public final String goodName;
    public final String baseName;
    public final String artUrl;
    public final String saveType;
    public final int status;
    public final int players;
    public final boolean rumble;
    
    private static final String URL_TEMPLATE = "https://dl.dropboxusercontent.com/u/3899306/CoverArt/%s.png";
    
    private static ConfigFile sConfigFile = null;
    private static final HashMap<String, ConfigSection> sCrcMap = new HashMap<String, ConfigSection>();
    
    public static void initializeDatabase( String mupen64plusIni )
    {
        sConfigFile = new ConfigFile( mupen64plusIni );
        for( String key : sConfigFile.keySet() )
        {
            ConfigSection section = sConfigFile.get( key );
            if( section != null )
            {
                String crc = section.get( "CRC" );
                if( crc != null )
                {
                    if( sCrcMap.get( crc ) == null )
                        sCrcMap.put( crc, section );
                }
            }
        }
    }
    
    public static RomDetail lookupByCrc( String crc )
    {
        return new RomDetail( sCrcMap.get( crc ), null );
    }
    
    public static RomDetail lookupByMd5( String md5 )
    {
        return new RomDetail( sConfigFile.get( md5 ), md5 );
    }
    
    public static String computeMd5( File file )
    {
        // From http://stackoverflow.com/a/16938703
        InputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream( file );
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            int numRead = 0;
            while( numRead != -1 )
            {
                numRead = inputStream.read( buffer );
                if( numRead > 0 )
                    digest.update( buffer, 0, numRead );
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString( md5Bytes );
        }
        catch( Exception e )
        {
            return null;
        }
        finally
        {
            if( inputStream != null )
            {
                try
                {
                    inputStream.close();
                }
                catch( Exception e )
                {
                }
            }
        }
    }
    
    private static String convertHashToString( byte[] md5Bytes )
    {
        // From http://stackoverflow.com/a/16938703
        String returnVal = "";
        for( int i = 0; i < md5Bytes.length; i++ )
        {
            returnVal += Integer.toString( ( md5Bytes[i] & 0xff ) + 0x100, 16 ).substring( 1 );
        }
        return returnVal.toUpperCase( Locale.US );
    }
    
    private RomDetail( ConfigSection section, String computedMd5 )
    {
        // Never query the database before initializing it
        if( sConfigFile == null )
            throw new IllegalStateException(
                    "RomDetail#initializeDatabase must be called before any other RomDetail method" );
        
        String _md5 = null;
        String _crc = null;
        String _goodName = null;
        String _baseName = null;
        String _artUrl = null;
        String _saveType = null;
        int _status = 0;
        int _players = 0;
        boolean _rumble = false;
        
        if( section != null )
        {
            // Record the MD5 if it was accurately provided; otherwise null
            _md5 = section.name.equals( computedMd5 ) ? computedMd5 : null;
            _crc = section.get( "CRC" );
            
            // Use an empty goodname (not null) for certain homebrew ROMs
            if( "00000000 00000000".equals( _crc ) )
                _goodName = "";
            else
                _goodName = section.get( "GoodName" );
            
            // Extract basename (goodname without the extra parenthetical tags)
            _baseName = _goodName == null ? null : _goodName.split( " \\(" )[0].trim();
            
            // Generate the cover art URL string
            _artUrl = _baseName == null ? null : String.format( URL_TEMPLATE,
                    _baseName.replace( ' ', '_' ).replace( "'", "" ).replace( '-', '_' ) );
            
            // Some ROMs have multiple entries. Instead of duplicating common data, the ini file
            // just references another entry.
            String refMd5 = section.get( "RefMD5" );
            if( !TextUtils.isEmpty( refMd5 ) )
                section = sConfigFile.get( refMd5 );
            
            if( section != null )
            {
                _saveType = section.get( "SaveType" );
                String statusString = section.get( "Status" );
                String playersString = section.get( "Players" );
                _status = TextUtils.isEmpty( statusString ) ? 0 : Integer.parseInt( statusString );
                _players = TextUtils.isEmpty( playersString ) ? 0 : Integer.parseInt( playersString );
                _rumble = "Yes".equals( section.get( "Rumble" ) );
            }
        }
        
        // Assign the final fields; assign goodname only if MD5 is valid
        md5 = _md5;
        goodName = _md5 == null ? null : _goodName;
        baseName = _baseName;
        artUrl = _artUrl;
        crc = _crc;
        saveType = _saveType;
        status = _status;
        players = _players;
        rumble = _rumble;
    }
    
    public Bitmap getCoverArt( boolean redownload )
    {
        // TODO Use cached value when redownload == false
        if( TextUtils.isEmpty( artUrl ) )
            return null;
        
        URL url = null;
        URLConnection connection = null;
        InputStream stream = null;
        Bitmap bitmap = null;
        try
        {
            url = new URL( artUrl );
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
