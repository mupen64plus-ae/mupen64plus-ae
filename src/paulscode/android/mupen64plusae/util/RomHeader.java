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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.util.Log;

/**
 * Utility class for retrieving information
 * about the header of a given ROM file.
 */
public final class RomHeader
{
    // @formatter:off
    public final byte init_PI_BSB_DOM1_LAT_REG;  // 0x00
    public final byte init_PI_BSB_DOM1_PGS_REG;  // 0x01
    public final byte init_PI_BSB_DOM1_PWD_REG;  // 0x02
    public final byte init_PI_BSB_DOM1_PGS_REG2; // 0x03
    public final int clockRate;                  // 0x04
    public final int pc;                         // 0x08
    public final int release;                    // 0x0C
    public final int crc1;                       // 0x10
    public final int crc2;                       // 0x14
    public final byte unknown1;                  // 0x18
    public final byte unknown2;                  // 0x19
    public final String name;                    // 0x20
    public final int unknown3;                   // 0x34
    public final int manufacturerId;             // 0x38
    public final short cartridgeId;              // 0x3C - Game serial number
    public final byte countryCode;               // 0x3E
    // @formatter:on
    public final String crc;
    public final String countrySymbol;
    public final boolean isValid;
    public final boolean isZip;
    
    /**
     * Constructor.
     * 
     * @param path The path of the ROM to get the header information about.
     */
    public RomHeader( String path )
    {
        this( new File( path ) );
    }
    
    /**
     * Constructor.
     * 
     * @param file The ROM file to get the header information about.
     */
    public RomHeader( File file )
    {
        this(readFile( file ));
    }
    
    /**
     * Constructor.
     * 
     * @param buffer The array of bytes to get the header information about.
     */
    public RomHeader( byte[] buffer )
    {
        if( buffer == null ||  buffer.length < 0x40 )
        {
            if (buffer == null || buffer.length < 4 )
            {
                init_PI_BSB_DOM1_LAT_REG = 0;
                init_PI_BSB_DOM1_PGS_REG = 0;
                init_PI_BSB_DOM1_PWD_REG = 0;
                init_PI_BSB_DOM1_PGS_REG2 = 0;
            }
            else
            {
                init_PI_BSB_DOM1_LAT_REG = buffer[0x00];
                init_PI_BSB_DOM1_PGS_REG = buffer[0x01];
                init_PI_BSB_DOM1_PWD_REG = buffer[0x02];
                init_PI_BSB_DOM1_PGS_REG2 = buffer[0x03];
            }
            clockRate = 0;
            pc = 0;
            release = 0;
            crc1 = 0;
            crc2 = 0;
            unknown1 = 0;
            unknown2 = 0;
            name = "";
            unknown3 = 0;
            manufacturerId = 0;
            cartridgeId = 0;
            countryCode = 0;
            crc = "";
        }
        else
        {
            swapBytes( buffer );
            init_PI_BSB_DOM1_LAT_REG = buffer[0x00];
            init_PI_BSB_DOM1_PGS_REG = buffer[0x01];
            init_PI_BSB_DOM1_PWD_REG = buffer[0x02];
            init_PI_BSB_DOM1_PGS_REG2 = buffer[0x03];
            clockRate = readInt( buffer, 0x04 );
            pc = readInt( buffer, 0x08 );
            release = readInt( buffer, 0x0C );
            crc1 = readInt( buffer, 0x10 );
            crc2 = readInt( buffer, 0x14 );
            unknown1 = buffer[0x18];
            unknown2 = buffer[0x19];
            name = readString( buffer, 0x20, 0x34 ).trim();
            unknown3 = readInt( buffer, 0x34 );
            manufacturerId = readInt( buffer, 0x38 );
            cartridgeId = readShort( buffer, 0x3C );
            countryCode = buffer[0x3E];
            crc = String.format( "%08X %08X", crc1, crc2 );
        }
        
        // Symbols match mappings from mupen64plus-core/util.c
        // See also https://code.google.com/p/mupen64plus/wiki/RomBrowserColumns
        switch( countryCode )
        {
            case 0x00: // Demo
                countrySymbol = "(Demo)";
                break;
            case 0x07: // Beta
                countrySymbol = "(Beta)";
                break;
            case 0x41: // 'A' - Japan / USA
                countrySymbol = "(JU)";
                break;
            case 0x44: // 'D' - Germany
                countrySymbol = "(G)";
                break;
            case 0x45: // 'E' - USA
                countrySymbol = "(U)";
                break;
            case 0x46: // 'F' - France
                countrySymbol = "(F)";
                break;
            case 0x49: // 'I' - Italy
                countrySymbol = "(I)";
                break;
            case 0x4a: // 'J' - Japan
                countrySymbol = "(J)";
                break;
            case 0x53: // 'S' - Spain
                countrySymbol = "(S)";
                break;
            case 0x55: // 'U' - Australia
            case 0x59: // 'Y' - Australia (alt)
                countrySymbol = "(A)";
                break;
            case 0x50: // 'P' - Europe
            case 0x58: // 'X' - Europe (alt)
            case 0x20: // ' ' - Europe (alt)
            case 0x21: // '!' - Europe (alt)
            case 0x38: // '8' - Europe (alt)
            case 0x70: // 'p' - Europe (alt)
                countrySymbol = "(E)";
                break;
            default:
                countrySymbol = String.format("(%02X)", countryCode );
                break;
        }
        
        isValid = init_PI_BSB_DOM1_LAT_REG == (byte) 0x80
                && init_PI_BSB_DOM1_PGS_REG == (byte) 0x37
                && init_PI_BSB_DOM1_PWD_REG == (byte) 0x12
                && init_PI_BSB_DOM1_PGS_REG2 == (byte) 0x40;
        
        isZip = init_PI_BSB_DOM1_LAT_REG == (byte) 0x50
                && init_PI_BSB_DOM1_PGS_REG == (byte) 0x4b
                && init_PI_BSB_DOM1_PWD_REG == (byte) 0x03
                && init_PI_BSB_DOM1_PGS_REG2 == (byte) 0x04;
    }
    
    private static byte[] readFile( File file )
    {
        byte[] buffer = new byte[0x40];
        DataInputStream in = null;
        try
        {
            in = new DataInputStream( new FileInputStream( file ) );
            in.read( buffer );
        }
        catch( IOException e )
        {
            Log.w( "RomHeader", "ROM file could not be read" );
            buffer = null;
        }
        finally
        {
            try
            {
                if( in != null )
                    in.close();
            }
            catch( IOException e )
            {
                Log.w( "RomHeader", "ROM file could not be closed" );
            }
        }
        return buffer;
    }
    
    private static void swapBytes( byte[] buffer )
    {
        if( buffer[0] == 0x37 )
        {
            // Byteswap if .v64 image
            for( int i = 0; i < buffer.length; i += 2 )
            {
                byte temp = buffer[i];
                buffer[i] = buffer[i + 1];
                buffer[i + 1] = temp;
            }
        }
        else if( buffer[0] == 0x40 )
        {
            // Wordswap if .n64 image
            for( int i = 0; i < buffer.length; i += 4 )
            {
                byte temp = buffer[i];
                buffer[i] = buffer[i + 3];
                buffer[i + 3] = temp;
                temp = buffer[i + 1];
                buffer[i + 1] = buffer[i + 2];
                buffer[i + 2] = temp;
            }
        }
    }
    
    private static int readInt( byte[] buffer, int start )
    {
        // @formatter:off
        return  (buffer[start + 3] & 0xFF)       |
                (buffer[start + 2] & 0xFF) << 8  |
                (buffer[start + 1] & 0xFF) << 16 |
                (buffer[start + 0] & 0xFF) << 24;
        // @formatter:on
    }
    
    private static short readShort( byte[] buffer, int start )
    {
        // @formatter:off
        int value = (buffer[start + 1] & 0xFF) |
                    (buffer[start + 0] & 0xFF) << 8;
        // @formatter:on
        return (short) value;
    }
    
    private String readString( byte[] buffer, int start, int end )
    {
        // Arrays.copyOfRange( buffer, start, end ) requires API 9, so do it manually
        byte[] newBuffer = new byte[end - start];
        for( int i = 0; i < newBuffer.length; i++ )
            newBuffer[i] = buffer[start + i];
        return new String( newBuffer );
    }
}
