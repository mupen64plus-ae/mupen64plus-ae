/*
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

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility class for retrieving information
 * about the header of a given ROM file.
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue", "PointlessArithmeticExpression"})
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
    public final CountryCode countryCode;               // 0x3E
    // @formatter:on
    public final String crc;
    public final String countrySymbol;
    public final boolean isValid;
    public final boolean isZip;
    public final boolean is7Zip;
    public final boolean isRar;
    public final boolean isNdd;
    
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
        this(readHeader( file ));
    }

    /**
     * Constructor.
     *
     * @param context Used for retrieving file descriptor of URI
     * @param file The ROM file to get the header information about.
     */
    public RomHeader( Context context, Uri file )
    {
        this(readHeader( context, file ));
    }
    
    /**
     * Constructor.
     * 
     * @param buffer The array of bytes to get the header information about.
     */
    public RomHeader( byte[] buffer )
    {
        CountryCode tempCountryCode;
        String tempName;
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
            tempName = "";
            unknown3 = 0;
            manufacturerId = 0;
            cartridgeId = 0;
            tempCountryCode = CountryCode.UNKNOWN;
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
            tempName = readString( buffer, 0x20, 0x34 ).trim();
            unknown3 = readInt( buffer, 0x34 );
            manufacturerId = readInt( buffer, 0x38 );
            cartridgeId = readShort( buffer, 0x3C );
            tempCountryCode = CountryCode.getCountryCode(buffer[0x3E]);
            crc = String.format( "%08X %08X", crc1, crc2 );
        }

        isValid = init_PI_BSB_DOM1_LAT_REG == (byte) 0x80
                && init_PI_BSB_DOM1_PGS_REG == (byte) 0x37
                && init_PI_BSB_DOM1_PWD_REG == (byte) 0x12
                && init_PI_BSB_DOM1_PGS_REG2 == (byte) 0x40;

        if (buffer != null && buffer.length >= 4) {
            isZip = buffer[0x00] == (byte) 0x50
                    && buffer[0x01] == (byte) 0x4b
                    && buffer[0x02] == (byte) 0x03
                    && buffer[0x03] == (byte) 0x04;

            is7Zip = buffer[0x00] == (byte) 0x7a
                    && buffer[0x01] == (byte) 0x37
                    && buffer[0x02] == (byte) 0xaf
                    && buffer[0x03] == (byte) 0xbc;

            isRar = buffer[0x00] == (byte) 0x52
                    && buffer[0x01] == (byte) 0x61
                    && buffer[0x02] == (byte) 0x72
                    && buffer[0x03] == (byte) 0x21;

            if (buffer.length >= 0xe8) {
                isNdd = buffer[0x04] == (byte) 0x10
                        && buffer[0x18] == (byte) 0xff
                        && buffer[0x19] == (byte) 0xff
                        && buffer[0x1a] == (byte) 0xff
                        && buffer[0x1b] == (byte) 0xff
                        && buffer[0xe6] == (byte) 0xff
                        && buffer[0xe7] == (byte) 0xff;
                if (isNdd) {
                    boolean isJapan = buffer[0x00] == (byte) 0xe8
                            && buffer[0x01] == (byte) 0x48
                            && buffer[0x02] == (byte) 0xd3
                            && buffer[0x03] == (byte) 0x16;
                    boolean isUsa = buffer[0x00] == (byte) 0x22
                            && buffer[0x01] == (byte) 0x63
                            && buffer[0x02] == (byte) 0xee
                            && buffer[0x03] == (byte) 0x56;
                    boolean isDev = buffer[0x00] == (byte) 0x00
                            && buffer[0x01] == (byte) 0x00
                            && buffer[0x02] == (byte) 0x00
                            && buffer[0x03] == (byte) 0x00;

                    if (isJapan) {
                        tempCountryCode = CountryCode.JAPAN;
                    } else if (isUsa) {
                        tempCountryCode = CountryCode.USA;
                    } else if (isDev) {
                        tempCountryCode = CountryCode.BETA;
                    } else {
                        tempCountryCode = CountryCode.UNKNOWN;
                    }

                    tempName = "";
                }
            } else {
                isNdd = false;
            }
        } else {
            isZip = false;
            is7Zip = false;
            isRar = false;
            isNdd = false;
        }

        countryCode = tempCountryCode;
        name = tempName;
        countrySymbol = countryCode.toString();
    }

    private static byte[] readHeader(File file )
    {
        byte[] buffer = new byte[0xe8];
        DataInputStream in = null;
        try
        {
            in = new DataInputStream( new FileInputStream( file ) );
            if (in.read( buffer ) != 0xe8) {
                buffer = null;
                Log.w( "RomHeader", "Not enough data for header" );
            }
        }
        catch( IOException e )
        {
            Log.w( "RomHeader", "ROM file could not be read: " + file );
            buffer = null;
        }
        catch( NullPointerException e )
        {
            Log.w( "RomHeader", "File does not exist: " + file );
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
                Log.w( "RomHeader", "ROM file could not be closed: " + file );
            }
        }
        return buffer;
    }

    private static byte[] readHeader(Context context, Uri file )
    {
        byte[] buffer = new byte[0xe8];

        if (file != null && !TextUtils.isEmpty(file.toString())) {
            try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(file, "r")) {

                if (parcelFileDescriptor != null) {
                    try (DataInputStream in = new DataInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()))){
                        if (in.read(buffer) != 0xe8) {
                            buffer = null;
                            Log.w("RomHeader", "Not enough data for header");
                        }
                    } catch (IOException e) {
                        Log.w("RomHeader", "ROM file could not be read: " + file);
                        buffer = null;
                    } catch (NullPointerException e) {
                        Log.w("RomHeader", "How did this happen?: " + file);
                        buffer = null;
                    }

                }
            } catch (IOException|java.lang.IllegalArgumentException|java.lang.SecurityException e) {
                e.printStackTrace();
                buffer = null;
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
        System.arraycopy(buffer, start, newBuffer, 0, newBuffer.length);
        return new String( newBuffer );
    }
}
