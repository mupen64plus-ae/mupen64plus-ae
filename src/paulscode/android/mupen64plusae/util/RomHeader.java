package paulscode.android.mupen64plusae.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;

public class RomHeader
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
    public final int unknown1;                   // 0x18
    public final int unknown2;                   // 0x19
    public final String name;                    // 0x20
    public final int unknown3;                   // 0x34
    public final int manufacturerId;             // 0x38
    public final short cartridgeId;              // 0x3C - Game serial number
    public final short countryCode;              // 0x3E
    // @formatter:on
    public final String crc;
    
    public RomHeader( File file )
    {
        DataInputStream in = null;
        try
        {
            in = new DataInputStream( new FileInputStream( file ) );
        }
        catch( FileNotFoundException e )
        {
            Log.e( "RomHeader", "ROM file not found", e );
        }
        
        if( in == null )
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
            name = null;
            unknown3 = 0;
            manufacturerId = 0;
            cartridgeId = 0;
            countryCode = 0;
        }
        else
        {
            init_PI_BSB_DOM1_LAT_REG = readByte( in );
            init_PI_BSB_DOM1_PGS_REG = readByte( in );
            init_PI_BSB_DOM1_PWD_REG = readByte( in );
            init_PI_BSB_DOM1_PGS_REG2 = readByte( in );
            clockRate = readInt( in );
            pc = readInt( in );
            release = readInt( in );
            crc1 = readInt( in );
            crc2 = readInt( in );
            unknown1 = readInt( in );
            unknown2 = readInt( in );
            byte[] nameBytes = new byte[20];
            for( int i = 0; i < nameBytes.length; i++ )
                nameBytes[i] = readByte( in );
            name = new String( nameBytes );
            unknown3 = readInt( in );
            manufacturerId = readInt( in );
            cartridgeId = readShort( in );
            countryCode = readShort( in );
            try
            {
                in.close();
            }
            catch( IOException e )
            {
                Log.e( "RomHeader", "ROM file could not be closed", e );
            }
        }
        crc = String.format( "%08X %08X", crc1, crc2 );
    }
    
    private byte readByte( DataInputStream in )
    {
        try
        {
            return in.readByte();
        }
        catch( IOException e )
        {
            Log.e( "RomHeader", "ROM file could not read char", e );
            return 0;
        }
    }
    
    private int readInt( DataInputStream in )
    {
        try
        {
            return in.readInt();
        }
        catch( IOException e )
        {
            Log.e( "RomHeader", "ROM file could not read int", e );
            return 0;
        }
    }
    
    private short readShort( DataInputStream in )
    {
        try
        {
            return in.readShort();
        }
        catch( IOException e )
        {
            Log.e( "RomHeader", "ROM file could not read short", e );
            return 0;
        }
    }
}
