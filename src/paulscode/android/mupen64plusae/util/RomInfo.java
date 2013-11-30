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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import android.text.TextUtils;

public class RomInfo
{
    public final String md5;
    public final String goodName;
    public final String crc;
    public final String saveType;
    public final int status;
    public final int players;
    public final boolean rumble;
    
    public RomInfo( File file, ConfigFile mupen64plusIni )
    {
        String _goodName = null;
        String _crc = null;
        String _saveType = null;
        int _status = 0;
        int _players = 0;
        boolean _rumble = false;
        
        md5 = file == null ? null : fileToMD5( file.getAbsolutePath() );
        if( md5 != null && mupen64plusIni != null )
        {
            ConfigSection section = mupen64plusIni.get( md5 );
            if( section != null )
            {
                _goodName = section.get( "GoodName" );
                _crc = section.get( "CRC" );
                
                // Some ROMs have multiple entries. Instead of duplicating common data, the ini file
                // just references another entry.
                String refMd5 = section.get( "RefMD5" );
                if( !TextUtils.isEmpty( refMd5 ) )
                    section = mupen64plusIni.get( refMd5 );
                
                if( section != null )
                {
                    _saveType = section.get( "SaveType" );
                    String statusString = section.get( "Status" );
                    String playersString = section.get( "Players" );
                    _status = TextUtils.isEmpty( statusString ) ? 0 : Integer
                            .parseInt( statusString );
                    _players = TextUtils.isEmpty( playersString ) ? 0 : Integer
                            .parseInt( playersString );
                    _rumble = "Yes".equals( section.get( "Rumble" ) );
                }
            }
        }
        
        // Assign the final fields
        goodName = _goodName;
        crc = _crc;
        saveType = _saveType;
        status = _status;
        players = _players;
        rumble = _rumble;
    }
    
    private static String fileToMD5( String filePath )
    {
        // From http://stackoverflow.com/a/16938703
        InputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream( filePath );
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
}
