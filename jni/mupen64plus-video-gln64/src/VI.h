#ifndef VI_H
#define VI_H
#include "Types.h"

struct VIInfo
{
    u32 width, height;
    f32 rwidth, rheight;
    u32 lastOrigin;

    u32 realWidth, realHeight;

    struct{
        u32 start, end;
    } display[16];

    u32 displayNum;

};

extern VIInfo VI;

void VI_UpdateSize();
void VI_UpdateScreen();

#endif

