/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - musyx.c                                         *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2013 Bobby Smiles                                       *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include <stddef.h>

#include "m64p_plugin.h"
#include "m64p_types.h"
#include "hle.h"
#include "musyx.h"

/* various constants */
enum { SUBFRAME_SIZE = 192 };
enum { MAX_VOICES = 32 };

enum { SAMPLE_BUFFER_SIZE = 0x200 };


enum {
    SFD_VOICE_COUNT     = 0x0,
    SFD_SFX_INDEX       = 0x2,
    SFD_VOICE_BITMASK   = 0x4,
    SFD_STATE_PTR       = 0x8,
    SFD_SFX_PTR         = 0xc,

    SFD_VOICES          = 0x10
};

enum {
    VOICE_ENV_BEGIN         = 0x00,
    VOICE_ENV_STEP          = 0x10,
    VOICE_PITCH_Q16         = 0x20,
    VOICE_PITCH_SHIFT       = 0x22,
    VOICE_CATSRC_0          = 0x24,
    VOICE_CATSRC_1          = 0x30,
    VOICE_ADPCM_FRAMES      = 0x3c,
    VOICE_SKIP_SAMPLES      = 0x3e,

    /* for PCM16 */
    VOICE_U16_40            = 0x40,
    VOICE_U16_42            = 0x42,

    /* for ADPCM */
    VOICE_ADPCM_TABLE_PTR   = 0x40,

    VOICE_INTERLEAVED_PTR   = 0x44,
    VOICE_END_POINT         = 0x48,
    VOICE_RESTART_POINT     = 0x4a,
    VOICE_U16_4C            = 0x4c,
    VOICE_U16_4E            = 0x4e,

    VOICE_SIZE              = 0x50
};

enum {
    CATSRC_PTR1     = 0x00,
    CATSRC_PTR2     = 0x04,
    CATSRC_SIZE1    = 0x08,
    CATSRC_SIZE2    = 0x0a
};

enum {
    STATE_LAST_SAMPLE   = 0x0,
    STATE_BASE_VOL      = 0x100,
    STATE_CC0           = 0x110,
    STATE_740_LAST4     = 0x290
};

enum {
    SFX_CBUFFER_PTR     = 0x00,
    SFX_CBUFFER_LENGTH  = 0x04,
    SFX_TAP_COUNT       = 0x08,
    SFX_FIR4_HGAIN      = 0x0a,
    SFX_TAP_DELAYS      = 0x0c,
    SFX_TAP_GAINS       = 0x2c,
    /* padding          = 0x3c */
    SFX_FIR4_HCOEFFS    = 0x40
};


/* struct definition */
typedef struct {
    /* internal subframes */
    int16_t left[SUBFRAME_SIZE];
    int16_t right[SUBFRAME_SIZE];
    int16_t cc0[SUBFRAME_SIZE];
    int16_t e50[SUBFRAME_SIZE];

    /* internal subframes base volumes */
    int32_t base_vol[4];

    /* */
    int16_t subframe_740_last4[4];
} musyx_t;

/* helper functions prototypes */
static void load_base_vol(int32_t *base_vol, uint32_t address);
static void save_base_vol(const int32_t *base_vol, uint32_t address);
static void update_base_vol(int32_t *base_vol, uint32_t voice_mask,
                            uint32_t last_sample_ptr);

static void init_subframes(musyx_t *musyx);

static uint32_t voice_stage(musyx_t *musyx, uint32_t voice_ptr,
                            uint32_t last_sample_ptr);

static void dma_cat8(uint8_t *dst, uint32_t catsrc_ptr);
static void dma_cat16(uint16_t *dst, uint32_t catsrc_ptr);

static void load_samples_PCM16(uint32_t voice_ptr, int16_t *samples,
                               unsigned *segbase, unsigned *offset);
static void load_samples_ADPCM(uint32_t voice_ptr, int16_t *samples,
                               unsigned *segbase, unsigned *offset);

static void adpcm_decode_frames(int16_t *dst, const uint8_t *src,
                                const int16_t *table, uint8_t count,
                                uint8_t skip_samples);

static int16_t adpcm_get_predicted_sample(uint8_t byte, uint8_t mask,
                                          unsigned lshift, unsigned rshift);
static void adpcm_get_predicted_frame(int16_t *dst, const uint8_t *src,
                                      const uint8_t *nibbles,
                                      unsigned int rshift);
static void adpcm_decode_upto_8_samples(int16_t *dst, const int16_t *src,
                                        const int16_t *cb_entry,
                                        const int16_t *last_samples,
                                        size_t size);

static void mix_voice_samples(musyx_t *musyx, uint32_t voice_ptr,
                              const int16_t *samples, unsigned segbase,
                              unsigned offset, uint32_t last_sample_ptr);

static void sfx_stage(musyx_t *musyx, uint32_t sfx_ptr, uint16_t idx);

static void interleave_stage(musyx_t *musyx, uint32_t output_ptr);


static uint8_t  *dram_u8(uint32_t address);
static uint16_t *dram_u16(uint32_t address);
static uint32_t *dram_u32(uint32_t address);

static void load_u8(uint8_t *dst, uint32_t address, size_t count);
static void load_u16(uint16_t *dst, uint32_t address, size_t count);
static void load_u32(uint32_t *dst, uint32_t address, size_t count);

static void store_u16(const uint16_t *src, uint32_t address, size_t count);

static inline int16_t clamp_s16(int32_t x)
{
    if (x > 32767)
        x = 32767;
    else if (x < -32768)
        x = -32768;

    return x;
}

static inline unsigned int align(unsigned int x, unsigned amount)
{
    --amount;
    return (x + amount) & ~amount;
}

static int32_t rdot(size_t n, const int16_t *x, const int16_t *y)
{
    int32_t accu = 0;

    y += n;

    while (n != 0) {
        accu += ((int32_t)*(x++) * (int32_t)*(--y));
        --n;
    }

    return accu;
}


static int32_t dot4(const int16_t *x, const int16_t *y)
{
    size_t i;
    int32_t accu = 0;

    for (i = 0; i < 4; ++i)
        accu = clamp_s16(accu + (((int32_t)x[i] * (int32_t)y[i]) >> 15));

    return accu;
}

/* Fast and dirty way of reading dram memory
 * Assume properly aligned access
 */
static uint8_t *dram_u8(uint32_t address)
{
    return (uint8_t *)&rsp.RDRAM[(address & 0xffffff) ^ S8];
}

static uint16_t *dram_u16(uint32_t address)
{
    return (uint16_t *)&rsp.RDRAM[(address & 0xffffff) ^ S16];
}

static uint32_t *dram_u32(uint32_t address)
{
    return (uint32_t *)&rsp.RDRAM[address & 0xffffff];
}

static void load_u8(uint8_t *dst, uint32_t address, size_t count)
{
    while (count != 0) {
        *(dst++) = *dram_u8(address);
        address += 1;
        --count;
    }
}

static void load_u16(uint16_t *dst, uint32_t address, size_t count)
{
    while (count != 0) {
        *(dst++) = *dram_u16(address);
        address += 2;
        --count;
    }
}

static void load_u32(uint32_t *dst, uint32_t address, size_t count)
{
    /* Optimization for uint32_t */
    const uint32_t *src = dram_u32(address);

    memcpy(dst, src, count * sizeof(uint32_t));
}

static void store_u16(const uint16_t *src, uint32_t address, size_t count)
{
    while (count != 0) {
        *dram_u16(address) = *(src++);
        address += 2;
        --count;
    }
}

/**************************************************************************
 * MusyX audio ucode
 **************************************************************************/
void musyx_task(void)
{
    const OSTask_t *const task = get_task();

    uint32_t sfd_ptr   = task->data_ptr;
    uint32_t sfd_count = task->data_size;
    uint32_t state_ptr;
    musyx_t musyx;

    DebugMessage(M64MSG_VERBOSE, "musyx_task: *data=%x, #SF=%d",
                 sfd_ptr,
                 sfd_count);

    state_ptr = *dram_u32(sfd_ptr + SFD_STATE_PTR);

    /* load initial state */
    load_base_vol(musyx.base_vol, state_ptr + STATE_BASE_VOL);
    load_u16((uint16_t *)musyx.cc0, state_ptr + STATE_CC0, SUBFRAME_SIZE);
    load_u16((uint16_t *)musyx.subframe_740_last4, state_ptr + STATE_740_LAST4,
             4);

    for (;;) {
        /* parse SFD structre */
        uint16_t sfx_index   = *dram_u16(sfd_ptr + SFD_SFX_INDEX);
        uint32_t voice_mask  = *dram_u32(sfd_ptr + SFD_VOICE_BITMASK);
        uint32_t sfx_ptr     = *dram_u32(sfd_ptr + SFD_SFX_PTR);
        uint32_t voice_ptr       = sfd_ptr + SFD_VOICES;
        uint32_t last_sample_ptr = state_ptr + STATE_LAST_SAMPLE;
        uint32_t output_ptr;

        /* initialize internal subframes using updated base volumes */
        update_base_vol(musyx.base_vol, voice_mask, last_sample_ptr);
        init_subframes(&musyx);

        /* active voices get mixed into L,R,cc0,e50 subframes (optional) */
        output_ptr = voice_stage(&musyx, voice_ptr, last_sample_ptr);

        /* apply delay-based effects (optional) */
        sfx_stage(&musyx, sfx_ptr, sfx_index);

        /* emit interleaved L,R subframes */
        interleave_stage(&musyx, output_ptr);

        --sfd_count;
        if (sfd_count == 0)
            break;

        sfd_ptr += SFD_VOICES + MAX_VOICES * VOICE_SIZE;
        state_ptr = *dram_u32(sfd_ptr + SFD_STATE_PTR);
    }

    /* writeback updated state */
    save_base_vol(musyx.base_vol, state_ptr + STATE_BASE_VOL);
    store_u16((uint16_t *)musyx.cc0, state_ptr + STATE_CC0, SUBFRAME_SIZE);
    store_u16((uint16_t *)musyx.subframe_740_last4, state_ptr + STATE_740_LAST4,
              4);
}

static void load_base_vol(int32_t *base_vol, uint32_t address)
{
    base_vol[0] = ((uint32_t)(*dram_u16(address))     << 16) | (*dram_u16(address +  8));
    base_vol[1] = ((uint32_t)(*dram_u16(address + 2)) << 16) | (*dram_u16(address + 10));
    base_vol[2] = ((uint32_t)(*dram_u16(address + 4)) << 16) | (*dram_u16(address + 12));
    base_vol[3] = ((uint32_t)(*dram_u16(address + 6)) << 16) | (*dram_u16(address + 14));
}

static void save_base_vol(const int32_t *base_vol, uint32_t address)
{
    unsigned k;

    for (k = 0; k < 4; ++k) {
        *dram_u16(address) = (uint16_t)(base_vol[k] >> 16);
        address += 2;
    }

    for (k = 0; k < 4; ++k) {
        *dram_u16(address) = (uint16_t)(base_vol[k]);
        address += 2;
    }
}

static void update_base_vol(int32_t *base_vol, uint32_t voice_mask,
                            uint32_t last_sample_ptr)
{
    unsigned i, k;
    uint32_t mask;

    DebugMessage(M64MSG_VERBOSE, "base_vol voice_mask = %08x", voice_mask);
    DebugMessage(M64MSG_VERBOSE, "BEFORE: base_vol = %08x %08x %08x %08x",
                 base_vol[0], base_vol[1], base_vol[2], base_vol[3]);

    /* optim: skip voices contributions entirely if voice_mask is empty */
    if (voice_mask != 0) {
        for (i = 0, mask = 1; i < MAX_VOICES;
             ++i, mask <<= 1, last_sample_ptr += 8) {
            if ((voice_mask & mask) == 0)
                continue;

            for (k = 0; k < 4; ++k)
                base_vol[k] += (int16_t)*dram_u16(last_sample_ptr + k * 2);
        }
    }

    /* apply 3% decay */
    for (k = 0; k < 4; ++k)
        base_vol[k] = (base_vol[k] * 0x0000f850) >> 16;

    DebugMessage(M64MSG_VERBOSE, "AFTER: base_vol = %08x %08x %08x %08x",
                 base_vol[0], base_vol[1], base_vol[2], base_vol[3]);
}

static void init_subframes(musyx_t *musyx)
{
    unsigned i;

    int16_t base_cc0 = clamp_s16(musyx->base_vol[2]);
    int16_t base_e50 = clamp_s16(musyx->base_vol[3]);

    int16_t *left  = musyx->left;
    int16_t *right = musyx->right;
    int16_t *cc0   = musyx->cc0;
    int16_t *e50   = musyx->e50;

    for (i = 0; i < SUBFRAME_SIZE; ++i) {
        *(e50++)    = base_e50;
        *(left++)   = clamp_s16(*cc0 + base_cc0);
        *(right++)  = clamp_s16(-*cc0 - base_cc0);
        *(cc0++)    = 0;
    }
}

/* Process voices, and returns interleaved subframe destination address */
static uint32_t voice_stage(musyx_t *musyx, uint32_t voice_ptr,
                            uint32_t last_sample_ptr)
{
    uint32_t output_ptr;
    int i = 0;

    /* voice stage can be skipped if first voice has no samples */
    if (*dram_u16(voice_ptr + VOICE_CATSRC_0 + CATSRC_SIZE1) == 0) {
        DebugMessage(M64MSG_VERBOSE, "Skipping Voice stage");
        output_ptr = *dram_u32(voice_ptr + VOICE_INTERLEAVED_PTR);
    }
    /* otherwise process voices until a non null output_ptr is encountered */
    else {
        for (;;) {
            /* load voice samples (PCM16 or APDCM) */
            int16_t samples[SAMPLE_BUFFER_SIZE];
            unsigned segbase;
            unsigned offset;

            DebugMessage(M64MSG_VERBOSE, "Processing Voice #%d", i);

            if (*dram_u8(voice_ptr + VOICE_ADPCM_FRAMES) == 0)
                load_samples_PCM16(voice_ptr, samples, &segbase, &offset);
            else
                load_samples_ADPCM(voice_ptr, samples, &segbase, &offset);

            /* mix them with each internal subframes */
            mix_voice_samples(musyx, voice_ptr, samples, segbase, offset,
                              last_sample_ptr + i * 8);

            /* check break condition */
            output_ptr = *dram_u32(voice_ptr + VOICE_INTERLEAVED_PTR);
            if (output_ptr != 0)
                break;

            /* next voice */
            ++i;
            voice_ptr += VOICE_SIZE;
        }
    }

    return output_ptr;
}

static void dma_cat8(uint8_t *dst, uint32_t catsrc_ptr)
{
    uint32_t ptr1  = *dram_u32(catsrc_ptr + CATSRC_PTR1);
    uint32_t ptr2  = *dram_u32(catsrc_ptr + CATSRC_PTR2);
    uint16_t size1 = *dram_u16(catsrc_ptr + CATSRC_SIZE1);
    uint16_t size2 = *dram_u16(catsrc_ptr + CATSRC_SIZE2);

    size_t count1 = size1;
    size_t count2 = size2;

    DebugMessage(M64MSG_VERBOSE, "dma_cat: %08x %08x %04x %04x",
                 ptr1,
                 ptr2,
                 size1,
                 size2);

    load_u8(dst, ptr1, count1);

    if (size2 == 0)
        return;

    load_u8(dst + count1, ptr2, count2);
}

static void dma_cat16(uint16_t *dst, uint32_t catsrc_ptr)
{
    uint32_t ptr1  = *dram_u32(catsrc_ptr + CATSRC_PTR1);
    uint32_t ptr2  = *dram_u32(catsrc_ptr + CATSRC_PTR2);
    uint16_t size1 = *dram_u16(catsrc_ptr + CATSRC_SIZE1);
    uint16_t size2 = *dram_u16(catsrc_ptr + CATSRC_SIZE2);

    size_t count1 = size1 >> 1;
    size_t count2 = size2 >> 1;

    DebugMessage(M64MSG_VERBOSE, "dma_cat: %08x %08x %04x %04x",
                 ptr1,
                 ptr2,
                 size1,
                 size2);

    load_u16(dst, ptr1, count1);

    if (size2 == 0)
        return;

    load_u16(dst + count1, ptr2, count2);
}

static void load_samples_PCM16(uint32_t voice_ptr, int16_t *samples,
                               unsigned *segbase, unsigned *offset)
{

    uint8_t  u8_3e  = *dram_u8(voice_ptr + VOICE_SKIP_SAMPLES);
    uint16_t u16_40 = *dram_u16(voice_ptr + VOICE_U16_40);
    uint16_t u16_42 = *dram_u16(voice_ptr + VOICE_U16_42);

    unsigned count = align(u16_40 + u8_3e, 4);

    DebugMessage(M64MSG_VERBOSE, "Format: PCM16");

    *segbase = SAMPLE_BUFFER_SIZE - count;
    *offset  = u8_3e;

    dma_cat16((uint16_t *)samples + *segbase, voice_ptr + VOICE_CATSRC_0);

    if (u16_42 != 0)
        dma_cat16((uint16_t *)samples, voice_ptr + VOICE_CATSRC_1);
}

static void load_samples_ADPCM(uint32_t voice_ptr, int16_t *samples,
                               unsigned *segbase, unsigned *offset)
{
    /* decompressed samples cannot exceed 0x400 bytes;
     * ADPCM has a compression ratio of 5/16 */
    uint8_t buffer[SAMPLE_BUFFER_SIZE * 2 * 5 / 16];
    int16_t adpcm_table[128];

    uint8_t u8_3c = *dram_u8(voice_ptr + VOICE_ADPCM_FRAMES    );
    uint8_t u8_3d = *dram_u8(voice_ptr + VOICE_ADPCM_FRAMES + 1);
    uint8_t u8_3e = *dram_u8(voice_ptr + VOICE_SKIP_SAMPLES    );
    uint8_t u8_3f = *dram_u8(voice_ptr + VOICE_SKIP_SAMPLES + 1);
    uint32_t adpcm_table_ptr = *dram_u32(voice_ptr + VOICE_ADPCM_TABLE_PTR);
    unsigned count;

    DebugMessage(M64MSG_VERBOSE, "Format: ADPCM");

    DebugMessage(M64MSG_VERBOSE, "Loading ADPCM table: %08x", adpcm_table_ptr);
    load_u16((uint16_t *)adpcm_table, adpcm_table_ptr, 128);

    count = u8_3c << 5;

    *segbase = SAMPLE_BUFFER_SIZE - count;
    *offset  = u8_3e & 0x1f;

    dma_cat8(buffer, voice_ptr + VOICE_CATSRC_0);
    adpcm_decode_frames(samples + *segbase, buffer, adpcm_table, u8_3c, u8_3e);

    if (u8_3d != 0) {
        dma_cat8(buffer, voice_ptr + VOICE_CATSRC_1);
        adpcm_decode_frames(samples, buffer, adpcm_table, u8_3d, u8_3f);
    }
}

static void adpcm_decode_frames(int16_t *dst, const uint8_t *src,
                                const int16_t *table, uint8_t count,
                                uint8_t skip_samples)
{
    int16_t frame[32];
    const uint8_t *nibbles = src + 8;
    unsigned i;
    bool jump_gap = false;

    DebugMessage(M64MSG_VERBOSE, "ADPCM decode: count=%d, skip=%d", count,
                 skip_samples);

    if (skip_samples >= 32) {
        jump_gap = true;
        nibbles += 16;
        src += 4;
    }

    for (i = 0; i < count; ++i) {
        uint8_t c2 = nibbles[0];

        const int16_t *book = (c2 & 0xf0) + table;
        unsigned int rshift = (c2 & 0x0f);

        adpcm_get_predicted_frame(frame, src, nibbles, rshift);

        memcpy(dst, frame, 2 * sizeof(frame[0]));
        adpcm_decode_upto_8_samples(dst +  2, frame +  2, book, dst     , 6);
        adpcm_decode_upto_8_samples(dst +  8, frame +  8, book, dst +  6, 8);
        adpcm_decode_upto_8_samples(dst + 16, frame + 16, book, dst + 14, 8);
        adpcm_decode_upto_8_samples(dst + 24, frame + 24, book, dst + 22, 8);

        if (jump_gap) {
            nibbles += 8;
            src += 32;
        }

        jump_gap = !jump_gap;
        nibbles += 16;
        src += 4;
        dst += 32;
    }
}

static int16_t adpcm_get_predicted_sample(uint8_t byte, uint8_t mask,
                                          unsigned lshift, unsigned rshift)
{
    int16_t sample = ((uint16_t)byte & (uint16_t)mask) << lshift;
    sample >>= rshift; /* signed */
    return sample;
}

static void adpcm_get_predicted_frame(int16_t *dst, const uint8_t *src,
                                      const uint8_t *nibbles,
                                      unsigned int rshift)
{
    unsigned int i;

    *(dst++) = (src[0] << 8) | src[1];
    *(dst++) = (src[2] << 8) | src[3];

    for (i = 1; i < 16; ++i) {
        uint8_t byte = nibbles[i];

        *(dst++) = adpcm_get_predicted_sample(byte, 0xf0,  8, rshift);
        *(dst++) = adpcm_get_predicted_sample(byte, 0x0f, 12, rshift);
    }
}

static void adpcm_decode_upto_8_samples(int16_t *dst, const int16_t *src,
                                        const int16_t *cb_entry,
                                        const int16_t *last_samples,
                                        size_t size)
{
    const int16_t *const book1 = cb_entry;
    const int16_t *const book2 = cb_entry + 8;

    const int16_t l1 = last_samples[0];
    const int16_t l2 = last_samples[1];

    size_t i;
    int32_t accu;

    for (i = 0; i < size; ++i) {
        accu = (int32_t)src[i] << 11;
        accu += book1[i] * l1 + book2[i] * l2 + rdot(i, book2, src);
        dst[i] = clamp_s16(accu >> 11);
    }
}

static void mix_voice_samples(musyx_t *musyx, uint32_t voice_ptr,
                              const int16_t *samples, unsigned segbase,
                              unsigned offset, uint32_t last_sample_ptr)
{
    int i, k;

    /* parse VOICE structure */
    const uint16_t pitch_q16   = *dram_u16(voice_ptr + VOICE_PITCH_Q16);
    const uint16_t pitch_shift = *dram_u16(voice_ptr + VOICE_PITCH_SHIFT); /* Q4.12 */

    const uint16_t end_point     = *dram_u16(voice_ptr + VOICE_END_POINT);
    const uint16_t restart_point = *dram_u16(voice_ptr + VOICE_RESTART_POINT);

    const uint16_t u16_4e = *dram_u16(voice_ptr + VOICE_U16_4E);

    /* init values and pointers */
    const int16_t       *sample         = samples + segbase + offset + u16_4e;
    const int16_t *const sample_end     = samples + segbase + end_point;
    const int16_t *const sample_restart = samples + (restart_point & 0x7fff) +
                                          (((restart_point & 0x8000) != 0) ? 0x000 : segbase);


    uint32_t pitch_accu = pitch_q16;
    uint32_t pitch_step = pitch_shift << 4;

    int32_t  v4_env[4];
    int32_t  v4_env_step[4];
    int16_t *v4_dst[4];
    int16_t  v4[4];

    load_u32((uint32_t *)v4_env,      voice_ptr + VOICE_ENV_BEGIN, 4);
    load_u32((uint32_t *)v4_env_step, voice_ptr + VOICE_ENV_STEP,  4);

    v4_dst[0] = musyx->left;
    v4_dst[1] = musyx->right;
    v4_dst[2] = musyx->cc0;
    v4_dst[3] = musyx->e50;

    DebugMessage(M64MSG_VERBOSE,
                 "Voice debug: segbase=%d"
                 "\tu16_4e=%04x\n"
                 "\tpitch: frac0=%04x shift=%04x\n"
                 "\tend_point=%04x restart_point=%04x\n"
                 "\tenv      = %08x %08x %08x %08x\n"
                 "\tenv_step = %08x %08x %08x %08x\n",
                 segbase,
                 u16_4e,
                 pitch_q16, pitch_shift,
                 end_point, restart_point,
                 v4_env[0],      v4_env[1],      v4_env[2],      v4_env[3],
                 v4_env_step[0], v4_env_step[1], v4_env_step[2], v4_env_step[3]);

    for (i = 0; i < SUBFRAME_SIZE; ++i) {
        /* update sample and resample_lut pointers and then pitch_accu */
        const int16_t *lut = (int16_t *)(ResampleLUT + ((pitch_accu & 0xfc00) >> 8));
        int dist;
        int16_t v;

        sample += (pitch_accu >> 16);
        pitch_accu &= 0xffff;
        pitch_accu += pitch_step;

        /* handle end/restart points */
        dist = sample - sample_end;
        if (dist >= 0)
            sample = sample_restart + dist;

        /* apply resample filter */
        v = clamp_s16(dot4(sample, lut));

        for (k = 0; k < 4; ++k) {
            /* envmix */
            int32_t accu = (v * (v4_env[k] >> 16)) >> 15;
            v4[k] = clamp_s16(accu);
            *(v4_dst[k]) = clamp_s16(accu + *(v4_dst[k]));

            /* update envelopes and dst pointers */
            ++(v4_dst[k]);
            v4_env[k] += v4_env_step[k];
        }
    }

    /* save last resampled sample */
    store_u16((uint16_t *)v4, last_sample_ptr, 4);

    DebugMessage(M64MSG_VERBOSE, "last_sample = %04x %04x %04x %04x",
                 v4[0], v4[1], v4[2], v4[3]);
}


static void sfx_stage(musyx_t *musyx, uint32_t sfx_ptr, uint16_t idx)
{
    uint32_t tap_delays[8];
    uint16_t tap_gains[8];
    uint16_t fir4_hcoeffs[4];

    uint32_t cbuffer_ptr;
    uint32_t cbuffer_length;
    uint16_t tap_count;
    uint16_t fir4_hgain;

    DebugMessage(M64MSG_VERBOSE, "SFX: %08x, idx=%d", sfx_ptr, idx);

    if (sfx_ptr == 0)
        return;

    /* load sfx  parameters */
    cbuffer_ptr    = *dram_u32(sfx_ptr + SFX_CBUFFER_PTR);
    cbuffer_length = *dram_u32(sfx_ptr + SFX_CBUFFER_LENGTH);

    tap_count      = *dram_u16(sfx_ptr + SFX_TAP_COUNT);
    load_u32(tap_delays, sfx_ptr + SFX_TAP_DELAYS, 8);
    load_u16(tap_gains,  sfx_ptr + SFX_TAP_GAINS,  8);

    fir4_hgain     = *dram_u16(sfx_ptr + SFX_FIR4_HGAIN);
    load_u16(fir4_hcoeffs, sfx_ptr + SFX_FIR4_HCOEFFS, 4);

    DebugMessage(M64MSG_VERBOSE, "cbuffer: ptr=%08x length=%d", cbuffer_ptr,
                 cbuffer_length);

    DebugMessage(M64MSG_VERBOSE, "fir4: hgain=%04x hcoeff=%04x %04x %04x %04x",
                 fir4_hgain, fir4_hcoeffs[0], fir4_hcoeffs[1], fir4_hcoeffs[2],
                 fir4_hcoeffs[3]);

    DebugMessage(M64MSG_VERBOSE, "tap count=%d", tap_count);
    /* TODO: */
}

static void interleave_stage(musyx_t *musyx, uint32_t output_ptr)
{
    size_t i;

    int16_t base_left;
    int16_t base_right;

    int16_t *left;
    int16_t *right;
    uint32_t *dst;

    DebugMessage(M64MSG_VERBOSE, "interleave: %08x", output_ptr);

    base_left  = clamp_s16(musyx->base_vol[0]);
    base_right = clamp_s16(musyx->base_vol[1]);

    left  = musyx->left;
    right = musyx->right;
    dst  = dram_u32(output_ptr);

    for (i = 0; i < SUBFRAME_SIZE; ++i) {
        uint16_t l = clamp_s16(*(left++)  + base_left);
        uint16_t r = clamp_s16(*(right++) + base_right);

        *(dst++) = (l << 16) | r;
    }
}
