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
    public final short countryCode;              // 0x3E
    // @formatter:on
    public final String crc;
    
    /**
     * Constructor.
     * 
     * @param file The ROM file to get the header information about.
     */
    public RomHeader( File file )
    {
        byte[] buffer = readFile( file );
        
        if( buffer == null )
        {
            init_PI_BSB_DOM1_LAT_REG = 0;
            init_PI_BSB_DOM1_PGS_REG = 0;
            init_PI_BSB_DOM1_PWD_REG = 0;
            init_PI_BSB_DOM1_PGS_REG2 = 0;
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
            countryCode = readShort( buffer, 0x3E );
        }
        crc = String.format( "%08X %08X", crc1, crc2 );
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
            Log.e( "RomHeader", "ROM file could not be read", e );
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
                Log.e( "RomHeader", "ROM file could not be closed", e );
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
