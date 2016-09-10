#ifndef RSP_H
#define RSP_H

#include "N64.h"
#include "GBI.h"
//#include "gSP.h"
#include "Types.h"

#ifndef min
#define min(a,b) ((a) < (b) ? (a) : (b))
#endif

#define RSPMSG_CLOSE            0
#define RSPMSG_UPDATESCREEN     1
#define RSPMSG_PROCESSDLIST     2
#define RSPMSG_CAPTURESCREEN    3
#define RSPMSG_DESTROYTEXTURES  4
#define RSPMSG_INITTEXTURES     5

typedef struct
{
    u32 PC[18], PCi, busy, halt, close, DList, uc_start, uc_dstart, cmd, nextCmd, count;
} RSPInfo;

extern RSPInfo RSP;

#define RSP_SegmentToPhysical( segaddr ) ((gSP.segment[(segaddr >> 24) & 0x0F] + (segaddr & 0x00FFFFFF)) & 0x00FFFFFF)

void RSP_Init();
void RSP_ProcessDList();
void RSP_LoadMatrix( f32 mtx[4][4], u32 address );

#endif

