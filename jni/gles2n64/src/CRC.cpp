#include "Types.h"

#define CRC32_POLYNOMIAL     0x04C11DB7

#ifdef __CRC_OPT
unsigned int CRCTable[ 256 * 4];
#else
unsigned int CRCTable[ 256 ];
#endif

u32 Reflect( u32 ref, char ch )
{
     u32 value = 0;

     // Swap bit 0 for bit 7
     // bit 1 for bit 6, etc.
     for (int i = 1; i < (ch + 1); i++)
     {
          if(ref & 1)
               value |= 1 << (ch - i);
          ref >>= 1;
     }
     return value;
}

void CRC_BuildTable()
{
    u32 crc;

    for (int i = 0; i < 256; i++)
    {
        crc = Reflect( i, 8 ) << 24;
        for (int j = 0; j < 8; j++)
            crc = (crc << 1) ^ (crc & (1 << 31) ? CRC32_POLYNOMIAL : 0);

        CRCTable[i] = Reflect( crc, 32 );
    }

#ifdef __CRC_OPT
    for (int i = 0; i < 256; i++)
    {
        for(int j = 0; j < 3; j++)
        {
            CRCTable[256*(j+1) + i] = (CRCTable[256*j + i]>>8) ^ CRCTable[CRCTable[256*j + i]&0xFF];
        }
    }
#endif

}

u32 CRC_Calculate( u32 crc, void *buffer, u32 count )
{
    u8 *p;
    u32 orig = crc;

    p = (u8*) buffer;

#ifdef __CRC_OPT
    while(count > 3)
    {
        crc ^= *(unsigned int*) p; p += 4;
        crc = CRCTable[3*256 + (crc&0xFF)]
          ^ CRCTable[2*256 + ((crc>>8)&0xFF)]
          ^ CRCTable[1*256 + ((crc>>16)&0xFF)]
          ^ CRCTable[0*256 + ((crc>>24))];

        count -= 4;
    }
#endif

    while (count--)
        crc = (crc >> 8) ^ CRCTable[(crc & 0xFF) ^ *p++];

    return crc ^ orig;
}

u32 CRC_CalculatePalette( u32 crc, void *buffer, u32 count )
{
    u8 *p;
    u32 orig = crc;

    p = (u8*) buffer;
    while (count--)
    {
        crc = (crc >> 8) ^ CRCTable[(crc & 0xFF) ^ *p++];
        crc = (crc >> 8) ^ CRCTable[(crc & 0xFF) ^ *p++];

        p += 6;
    }

    return crc ^ orig;
}

