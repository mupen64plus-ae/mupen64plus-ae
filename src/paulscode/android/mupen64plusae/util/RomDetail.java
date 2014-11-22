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
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.apache.commons.lang.NullArgumentException;

import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import android.text.TextUtils;

public class RomDetail
{
    public final String md5;
    public final String crc;
    public final String goodName;
    public final String baseName;
    public final String artName;
    public final String artUrl;
    public final String wikiUrl;
    public final String saveType;
    public final int status;
    public final int players;
    public final boolean rumble;
    
    private static final String ART_URL_TEMPLATE = "http://paulscode.com/downloads/Mupen64Plus-AE/CoverArt/%s";
    private static final String WIKI_URL_TEMPLATE = "http://littleguy77.wikia.com/wiki/%s";
    
    private static ConfigFile sConfigFile = null;
    private static final HashMap<String, ArrayList<ConfigSection>> sCrcMap = new HashMap<String, ArrayList<ConfigSection>>();
    
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
                        sCrcMap.put( crc, new ArrayList<ConfigSection>() );
                    sCrcMap.get( crc ).add( section );
                }
            }
        }
    }
    
    public static RomDetail[] lookupByCrc( String crc )
    {
        ArrayList<ConfigSection> sections = sCrcMap.get( crc );
        if( sections == null )
            return new RomDetail[0];
        
        RomDetail[] results = new RomDetail[sections.size()];
        for( int i = 0; i < results.length; i++ )
            results[i] = new RomDetail( sections.get( i ) );
        return results;
    }
    
    public static RomDetail lookupByMd5( String md5 )
    {
        ConfigSection section = sConfigFile.get( md5 );
        return section == null ? null : new RomDetail( section );
    }
    
    public static String computeMd5( File file )
    {
        // From http://stackoverflow.com/a/16938703
        InputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream( file );
            MessageDigest digester = MessageDigest.getInstance( "MD5" );
            byte[] bytes = new byte[8192];
            int byteCount;
            while( ( byteCount = inputStream.read( bytes ) ) > 0 )
            {
                digester.update( bytes, 0, byteCount );
            }
            return convertHashToString( digester.digest() );
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
    
    private RomDetail( ConfigSection section )
    {
        // Never pass a null section
        if( section == null )
            throw new NullArgumentException( "section" );
        
        // Never query the database before initializing it
        if( sConfigFile == null )
            throw new IllegalStateException(
                    "RomDetail#initializeDatabase must be called before any other RomDetail method" );
        
        String _md5 = null;
        String _crc = null;
        String _goodName = null;
        String _baseName = null;
        String _artName = null;
        String _artUrl = null;
        String _wikiUrl = null;
        String _saveType = null;
        int _status = 0;
        int _players = 0;
        boolean _rumble = false;
        
        {
            // Record the MD5 if it was accurately provided; otherwise null
            _md5 = section.name;
            _crc = section.get( "CRC" );
            
            // Use an empty goodname (not null) for certain homebrew ROMs
            if( "00000000 00000000".equals( _crc ) )
                _goodName = "";
            else
                _goodName = section.get( "GoodName" );
            
            if( _goodName != null )
            {
                // Extract basename (goodname without the extra parenthetical tags)
                _baseName = _goodName.split( " \\(" )[0].trim();
                
                // Generate the cover art URL string
                _artName = _baseName.replaceAll( "['\\.]", "" ).replaceAll( "\\W+", "_" ) + ".png";
                _artUrl = String.format( ART_URL_TEMPLATE, _artName );
                
                // Generate wiki page URL string
                _wikiUrl = String.format( WIKI_URL_TEMPLATE, _baseName.replaceAll( " ", "_" ) );
                if( _goodName.contains( "(Kiosk" ) )
                    _wikiUrl += "_(Kiosk_Demo)";
            }
            
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
        goodName = _goodName;
        baseName = _baseName;
        artName = _artName;
        artUrl = _artUrl;
        wikiUrl = _wikiUrl;
        crc = _crc;
        saveType = _saveType;
        status = _status;
        players = _players;
        rumble = _rumble;
    }
}
