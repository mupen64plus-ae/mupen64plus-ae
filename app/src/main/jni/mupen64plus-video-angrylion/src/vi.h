#ifndef _VI_H_
#define _VI_H_

#include <stdint.h>
#include "Gfx #1.3.h"
#include "z64.h"

typedef struct {
    int ntscnolerp;
    int copymstrangecrashes;
    int fillmcrashes;
    int fillmbitcrashes;
    int syncfullcrash;
} onetime;


#ifdef _WIN32
#include <windows.h>
#else
typedef struct _RECT {
   int32_t left;
   int32_t top;
   int32_t right;
   int32_t bottom;
} RECT, *PRECT;

typedef struct {   // Declare an unnamed structure and give it the
                   // typedef name POINT.
   int32_t x;
   int32_t y;
} POINT;
#endif

extern RECT __src, __dst;
extern int res;
extern int32_t pitchindwords;

extern uint8_t* rdram_8;
extern uint16_t* rdram_16;
extern uint32_t plim;
extern uint32_t idxlim16;
extern uint32_t idxlim32;
extern uint8_t hidden_bits[0x400000];

extern int overlay;

extern onetime onetimewarnings;
extern uint32_t gamma_table[0x100];
extern uint32_t gamma_dither_table[0x4000];
extern int32_t vi_restore_table[0x400];
extern int32_t oldvstart;

extern NOINLINE void DisplayError(char * error);

void precalc_cvmask_derivatives(void);

void lookup_cvmask_derivatives(uint32_t mask, uint8_t* offx, uint8_t* offy, uint32_t* curpixel_cvg, uint32_t* curpixel_cvbit);

extern void rdp_init(void);
extern void rdp_close(void);
extern void rdp_update(void);

#endif
