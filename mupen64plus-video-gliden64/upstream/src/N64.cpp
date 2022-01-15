#include "N64.h"

u8 *HEADER;
u8 *DMEM;
u8 *IMEM;
u64 TMEM[1024];
u8 *RDRAM;

u32 RDRAMSize = 0;

N64Regs REG;

bool ConfigOpen = false;
