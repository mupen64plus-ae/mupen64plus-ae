#ifndef WINLNXDEFS_H
#define WINLNXDEFS_H

#define wxPtrToUInt (uintptr_t)
#define TRUE 1
#define FALSE 0

#define _T(x) x

#ifndef WIN32

#include <stdint.h>


typedef int BOOL;
typedef unsigned int wxUint32;
typedef unsigned short wxUint16;
typedef unsigned char wxUint8;
typedef unsigned char BYTE;
typedef long long LONGLONG;


typedef int wxInt32;
typedef short wxInt16;
typedef char wxInt8;

typedef uint64_t wxUint64;
typedef int64_t wxInt64;

typedef unsigned char wxChar;
typedef uintptr_t wxUIntPtr;


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

#else

typedef DWORD wxUint32;
typedef WORD wxUint16;
typedef BYTE wxUint8;

typedef int wxInt32;
typedef short wxInt16;
typedef char wxInt8;


typedef BYTE char wxChar;
typedef uintptr_t wxUIntPtr;

#endif

#endif
