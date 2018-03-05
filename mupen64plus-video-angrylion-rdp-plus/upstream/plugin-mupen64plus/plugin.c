#include "gfx_m64p.h"

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define DP_INTERRUPT    0x20

static uint32_t rdram_size;
static ptr_PluginGetVersion CoreGetVersion = NULL;

void plugin_init(void)
{
    CoreGetVersion = (ptr_PluginGetVersion) DLSYM(CoreLibHandle, "PluginGetVersion");

    int core_version;
    CoreGetVersion(NULL, &core_version, NULL, NULL, NULL);
    if (core_version >= 0x020501) {
        rdram_size = *gfx.RDRAM_SIZE;
    } else {
        rdram_size = RDRAM_MAX_SIZE;
    }
}

void plugin_sync_dp(void)
{
    *gfx.MI_INTR_REG |= DP_INTERRUPT;
    gfx.CheckInterrupts();
}

uint32_t** plugin_get_dp_registers(void)
{
    // HACK: this only works because the ordering of registers in GFX_INFO is
    // the same as in dp_register
    return (uint32_t**)&gfx.DPC_START_REG;
}

uint32_t** plugin_get_vi_registers(void)
{
    // HACK: this only works because the ordering of registers in GFX_INFO is
    // the same as in vi_register
    return (uint32_t**)&gfx.VI_STATUS_REG;
}

uint8_t* plugin_get_rdram(void)
{
    return gfx.RDRAM;
}

uint32_t plugin_get_rdram_size(void)
{
    return rdram_size;
}

uint8_t* plugin_get_dmem(void)
{
    return gfx.DMEM;
}

uint8_t* plugin_get_rom_header(void)
{
    return gfx.HEADER;
}

void plugin_close(void)
{
}
