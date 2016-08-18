#ifndef __Z64_H__
#define __Z64_H__

#include <stdio.h>
#include <stdint.h>

#include <retro_inline.h>

#define SP_INTERRUPT    0x1
#define SI_INTERRUPT    0x2
#define AI_INTERRUPT    0x4
#define VI_INTERRUPT    0x8
#define PI_INTERRUPT    0x10
#define DP_INTERRUPT    0x20

#define SP_STATUS_HALT            0x0001
#define SP_STATUS_BROKE            0x0002
#define SP_STATUS_DMABUSY        0x0004
#define SP_STATUS_DMAFULL        0x0008
#define SP_STATUS_IOFULL        0x0010
#define SP_STATUS_SSTEP            0x0020
#define SP_STATUS_INTR_BREAK    0x0040
#define SP_STATUS_SIGNAL0        0x0080
#define SP_STATUS_SIGNAL1        0x0100
#define SP_STATUS_SIGNAL2        0x0200
#define SP_STATUS_SIGNAL3        0x0400
#define SP_STATUS_SIGNAL4        0x0800
#define SP_STATUS_SIGNAL5        0x1000
#define SP_STATUS_SIGNAL6        0x2000
#define SP_STATUS_SIGNAL7        0x4000

#define DP_STATUS_XBUS_DMA        0x01
#define DP_STATUS_FREEZE        0x02
#define DP_STATUS_FLUSH            0x04
#define DP_STATUS_START_GCLK        0x008
#define DP_STATUS_TMEM_BUSY        0x010
#define DP_STATUS_PIPE_BUSY        0x020
#define DP_STATUS_CMD_BUSY            0x040
#define DP_STATUS_CBUF_READY        0x080
#define DP_STATUS_DMA_BUSY            0x100
#define DP_STATUS_END_VALID        0x200
#define DP_STATUS_START_VALID        0x400

#define R4300i_SP_Intr 1

#ifdef MSB_FIRST
#define BYTE_ADDR_XOR        0
#define WORD_ADDR_XOR        0
#define BYTE4_XOR_BE(a)     (a)
#else
#define BYTE_ADDR_XOR        3
#define WORD_ADDR_XOR        1
#define BYTE4_XOR_BE(a)     ((a) ^ 3)                
#endif

#ifdef MSB_FIRST
#define BYTE_XOR_DWORD_SWAP 4
#define WORD_XOR_DWORD_SWAP 2
#else
#define BYTE_XOR_DWORD_SWAP 7
#define WORD_XOR_DWORD_SWAP 3
#endif
#define DWORD_XOR_DWORD_SWAP 1

#ifdef _MSC_VER
#define NOINLINE        __declspec(noinline)
#define STRICTINLINE    __forceinline
#define ALIGNED         __declspec(align(16))
#else
#define NOINLINE        __attribute__((noinline))
#define STRICTINLINE    INLINE
#define ALIGNED         __attribute__((aligned(16)))
#endif

#ifndef PRESCALE_WIDTH
#define PRESCALE_WIDTH  640
#endif

#ifndef PRESCALE_HEIGHT
#define PRESCALE_HEIGHT 625
#endif

#define RDRAM_MASK 0x00ffffff

#define GET_GFX_INFO(member)    (gfx_info.member)

#define DRAM        GET_GFX_INFO(RDRAM)
#define DRAM16      ((i16 *)DRAM)
#define DRAM32      ((i32 *)DRAM)

#define SP_DMEM         GET_GFX_INFO(DMEM)
#define SP_IMEM         GET_GFX_INFO(IMEM)
#define SP_DMEM16       ((i16 *)SP_DMEM)
#define SP_IMEM16       ((i16 *)SP_IMEM)
#define SP_DMEM32       ((i32 *)SP_DMEM)
#define SP_IMEM32       ((i32 *)SP_IMEM)

#define rdram ((uint32_t*)DRAM)
#define rsp_imem ((uint32_t*)gfx_info.IMEM)
#define rsp_dmem ((uint32_t*)gfx_info.DMEM)

#define rdram16 ((uint16_t*)DRAM)
#define rdram8 (DRAM)

#define vi_origin (*(uint32_t*)gfx_info.VI_ORIGIN_REG)
#define vi_width (*(uint32_t*)gfx_info.VI_WIDTH_REG)
#define vi_control (*(uint32_t*)gfx_info.VI_STATUS_REG)
#define vi_v_sync (*(uint32_t*)gfx_info.VI_V_SYNC_REG)
#define vi_h_sync (*(uint32_t*)gfx_info.VI_H_SYNC_REG)
#define vi_h_start (*(uint32_t*)gfx_info.VI_H_START_REG)
#define vi_v_start (*(uint32_t*)gfx_info.VI_V_START_REG)
#define vi_v_intr (*(uint32_t*)gfx_info.VI_INTR_REG)
#define vi_x_scale (*(uint32_t*)gfx_info.VI_X_SCALE_REG)
#define vi_y_scale (*(uint32_t*)gfx_info.VI_Y_SCALE_REG)
#define vi_timing (*(uint32_t*)gfx_info.VI_TIMING_REG)
#define vi_v_current_line (*(uint32_t*)gfx_info.VI_V_CURRENT_LINE_REG)

#define dp_start (*(uint32_t*)gfx_info.DPC_START_REG)
#define dp_end (*(uint32_t*)gfx_info.DPC_END_REG)
#define dp_current (*(uint32_t*)gfx_info.DPC_CURRENT_REG)
#define dp_status (*(uint32_t*)gfx_info.DPC_STATUS_REG)

#define GET_LOW(x)      (((x) & 0x003E) << 2)
#define GET_MED(x)      (((x) & 0x07C0) >> 3)
#define GET_HI(x)       (((x) >> 8) & 0x00F8)

#define RREADADDR8(rdst, in) {(in) &= RDRAM_MASK; (rdst) = ((in) <= plim) ? (rdram_8[(in) ^ BYTE_ADDR_XOR]) : 0;}
#define RREADIDX16(rdst, in) {(in) &= (RDRAM_MASK >> 1); (rdst) = ((in) <= idxlim16) ? (rdram_16[(in) ^ WORD_ADDR_XOR]) : 0;}
#define RREADIDX32(rdst, in) {(in) &= (RDRAM_MASK >> 2); (rdst) = ((in) <= idxlim32) ? (rdram[(in)]) : 0;}

#define RWRITEADDR8(in, val)	{(in) &= RDRAM_MASK; if ((in) <= plim) rdram_8[(in) ^ BYTE_ADDR_XOR] = (val);}
#define RWRITEIDX16(in, val)	{(in) &= (RDRAM_MASK >> 1); if ((in) <= idxlim16) rdram_16[(in) ^ WORD_ADDR_XOR] = (val);}
#define RWRITEIDX32(in, val)	{(in) &= (RDRAM_MASK >> 2); if ((in) <= idxlim32) rdram[(in)] = (val);}

#define PAIRREAD16(rdst, hdst, in) {             \
   (in) &= (RDRAM_MASK >> 1);			             \
    if ((in) <= idxlim16) {                      \
        (rdst) = rdram_16[(in) ^ WORD_ADDR_XOR]; \
        (hdst) = hidden_bits[(in)];              \
    } else                                       \
        (rdst) = (hdst) = 0;                     \
}

#endif
