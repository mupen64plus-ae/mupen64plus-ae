#ifndef WINLNXDEFS_H
#define WINLNXDEFS_H

#define wxPtrToUInt (uintptr_t)
#define TRUE 1
#define FALSE 0

#define _T(x) x

#include <stdint.h>

typedef int BOOL;
typedef uint32_t wxUint32;
typedef uint16_t wxUint16;
typedef uint8_t wxUint8;
typedef uint8_t BYTE;
typedef long long LONGLONG;


typedef int32_t wxInt32;
typedef int16_t wxInt16;
typedef int8_t wxInt8;

typedef uint64_t wxUint64;
typedef int64_t wxInt64;

typedef unsigned char wxChar;
typedef uintptr_t wxUIntPtr;

#ifndef WIN32

typedef union _LARGE_INTEGER
{
   struct
     {
    uint32_t LowPart;
    uint32_t HighPart;
     } s;
   struct
     {
    uint32_t LowPart;
    uint32_t HighPart;
     } u;
   long long QuadPart;
} LARGE_INTEGER, *PLARGE_INTEGER;

#define WINAPI

#endif

#endif
