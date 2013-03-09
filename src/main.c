/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-rsp-hle - main.c                                          *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2012 Bobby Smiles                                       *
 *   Copyright (C) 2009 Richard Goedeken                                   *
 *   Copyright (C) 2002 Hacktarux                                          *
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

#include <stdarg.h>
#include <string.h>
#include <stdio.h>

#define M64P_PLUGIN_PROTOTYPES 1
#include "m64p_types.h"
#include "m64p_common.h"
#include "m64p_plugin.h"
#include "hle.h"
#include "alist.h"
#include "jpeg.h"

/* helper functions prototypes */
static unsigned int sum_bytes(const unsigned char *bytes, unsigned int size);
static void dump_binary(char *filename, unsigned char *bytes, unsigned size);
static void dump_task(char *filename, const OSTask_t * const task);


/* global variables */
RSP_INFO rsp;

/* local variables */
static const int AudioHle = 0, GraphicsHle = 1;
static void (*l_DebugCallback)(void *, int, const char *) = NULL;
static void *l_DebugCallContext = NULL;
static int l_PluginInit = 0;

/* local functions */


/**
 * Try to figure if the RSP was launched using osSpTask* functions
 * and not run directly (in which case DMEM[0xfc0-0xfff] is meaningless).
 *
 * Previously, the ucode_size field was used to determine this,
 * but it is not robust enough (hi Pokemon Stadium !) because games could write anything
 * in this field : most ucode_boot discard the value and just use 0xf7f anyway.
 *
 * Using ucode_boot_size should be more robust in this regard.
 **/
static int is_task()
{
    return (get_task()->ucode_boot_size <= 0x1000);
}


/**
 * Simulate the effect of setting the TASKDONE bit (aliased to SIG2)
 * and executing a break instruction (setting HALT and BROKE bits).
 **/
static void taskdone()
{
    // On hardware writing to SP_STATUS_REG is an indirect way of changing its content.
    // For instance, in order to set the TASKDONE bit (bit 9), one should write 0x4000
    // to the SP_STATUS_REG : Read Access & Write Access don't have the same semantic.
    //
    // Here, this indirect way of changing the status register is bypassed :
    // we modify the bits directly.
    //
    // 0x203 = TASKDONE | BROKE | HALT
    *rsp.SP_STATUS_REG |= 0x203;

    // if INTERRUPT_ON_BREAK we generate the interrupt
    if ((*rsp.SP_STATUS_REG & 0x40) != 0 )
    {
        *rsp.MI_INTR_REG |= 0x1;
        rsp.CheckInterrupts();
    }
}


static int audio_ucode_detect()
{
    const OSTask_t * const task = get_task();

    if (*(unsigned int*)(rsp.RDRAM + task->ucode_data + 0) != 0x1)
    {
        if (*(rsp.RDRAM + task->ucode_data + (0 ^ (3-S8))) == 0xF)
            return 4;
        else
            return 3;
    }
    else
    {
        if (*(unsigned int*)(rsp.RDRAM + task->ucode_data + 0x30) == 0xF0000F00)
            return 1;
        else
            return 2;
    }
}

static int audio_ucode()
{
    switch(audio_ucode_detect())
    {
    case 1: // mario ucode
        alist_process_ABI1();
        break;
    case 2: // zelda ucode
        alist_process_ABI2();
        break;
    case 3: // banjo kazooie ucode
        alist_process_ABI3();
        break;
    default:
        {
        DebugMessage(M64MSG_WARNING, "unknown audio ucode");
        return -1;
        }
    }

    return 0;
}

static int DoGFXTask(int sum)
{
    if (GraphicsHle && rsp.ProcessDlistList != NULL)
    {
        rsp.ProcessDlistList();
        taskdone();
        *rsp.DPC_STATUS_REG &= ~0x0002;
        return 1;
    }
    else
    {
        DebugMessage(M64MSG_WARNING, "GFX ucode through rsp plugin is not implemented");
        return 0;
    }
}

static int DoAudioTask(int sum)
{
    if (AudioHle && rsp.ProcessAlistList != NULL)
    {
        rsp.ProcessAlistList();
        taskdone();
        return 1;
    }
    else
    {
        if (audio_ucode() == 0)
        {
            taskdone();
            return 1;
        }
    }

    return 0;
}

static int DoJPEGTask(int sum)
{
    switch(sum)
    {
    case 0x278: // Zelda OOT during boot
      taskdone();
      return 1;
    case 0x2c85a: // Pokemon stadium J jpg decompression
        jpeg_decode_PS0();
        taskdone();
        return 1;
    case 0x2caa6: // Zelda OOT, Pokemon Stadium {1,2} jpg decompression
        jpeg_decode_PS();
        taskdone();
        return 1;
    case 0x130de: // Ogre Battle background decompression
        jpeg_decode_OB();
        taskdone();
        return 1;
    }

    return 0;
}

static int DoCFBTask(int sum)
{
    rsp.ShowCFB();
    taskdone();
    return 1;
}


/* Global functions */
void DebugMessage(int level, const char *message, ...)
{
    char msgbuf[1024];
    va_list args;

    if (l_DebugCallback == NULL)
        return;

    va_start(args, message);
    vsprintf(msgbuf, message, args);

    (*l_DebugCallback)(l_DebugCallContext, level, msgbuf);

    va_end(args);
}

/* DLL-exported functions */
EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle CoreLibHandle, void *Context,
                                   void (*DebugCallback)(void *, int, const char *))
{
    if (l_PluginInit)
        return M64ERR_ALREADY_INIT;

    /* first thing is to set the callback function for debug info */
    l_DebugCallback = DebugCallback;
    l_DebugCallContext = Context;

    /* this plugin doesn't use any Core library functions (ex for Configuration), so no need to keep the CoreLibHandle */

    l_PluginInit = 1;
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginShutdown(void)
{
    if (!l_PluginInit)
        return M64ERR_NOT_INIT;

    /* reset some local variable */
    l_DebugCallback = NULL;
    l_DebugCallContext = NULL;

    l_PluginInit = 0;
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type *PluginType, int *PluginVersion, int *APIVersion, const char **PluginNamePtr, int *Capabilities)
{
    /* set version info */
    if (PluginType != NULL)
        *PluginType = M64PLUGIN_RSP;

    if (PluginVersion != NULL)
        *PluginVersion = RSP_HLE_VERSION;

    if (APIVersion != NULL)
        *APIVersion = RSP_PLUGIN_API_VERSION;
    
    if (PluginNamePtr != NULL)
        *PluginNamePtr = "Hacktarux/Azimer High-Level Emulation RSP Plugin";

    if (Capabilities != NULL)
    {
        *Capabilities = 0;
    }
                    
    return M64ERR_SUCCESS;
}

EXPORT unsigned int CALL DoRspCycles(unsigned int Cycles)
{
    const OSTask_t * const task = get_task();
    unsigned int i, sum;
    char filename[256];

    if (is_task())
    {
        // most ucode_boot procedure copy 0xf80 bytes of ucode whatever the ucode_size is.
        // For practical purpose we use a ucode_size = min(0xf80, task->ucode_size)
        unsigned int ucode_size = (task->ucode_size > 0xf80) ? 0xf80 : task->ucode_size;
        unsigned int sum = sum_bytes(rsp.RDRAM + task->ucode, ucode_size >> 1);

        switch(task->type)
        {
        case 0: // Not specified
            {
                switch(sum)
                {
                case 0x212ee: // Twintris (task type is in fact GFX)
                    {
                        if (DoGFXTask(sum)) return Cycles;
                        break;
                    }
                }
                break;
            }
        case 1: // GFX
            {
                if (DoGFXTask(sum)) return Cycles;
                break;
            }

        case 2: // AUDIO
            {
                if (DoAudioTask(sum)) return Cycles;
                break;
            }

        case 4: // JPEG
            {
                if (DoJPEGTask(sum)) return Cycles;
                break;
            }

        case 7: // CFB
            {
                if (DoCFBTask(sum)) return Cycles;
                break;
            }
        }

        DebugMessage(M64MSG_WARNING, "unknown OSTask: sum %x PC:%x", sum, *rsp.SP_PC_REG);

        sprintf(&filename[0], "task_%x.log", sum);
        dump_task(filename, task);
        
        // dump ucode_boot
        sprintf(&filename[0], "ucode_boot_%x.bin", sum);
        dump_binary(filename, rsp.RDRAM + (task->ucode_boot & 0x7fffff), task->ucode_boot_size);

        // dump ucode
        if (task->ucode != 0)
        {
            sprintf(&filename[0], "ucode_%x.bin", sum);
            dump_binary(filename, rsp.RDRAM + (task->ucode & 0x7fffff), ucode_size);
        }

        // dump ucode_data
        if (task->ucode_data != 0)
        {
            sprintf(&filename[0], "ucode_data_%x.bin", sum);
            dump_binary(filename, rsp.RDRAM + (task->ucode_data & 0x7fffff), task->ucode_data_size);
        }

        // dump data
        if (task->data_ptr != 0)
        {
            sprintf(&filename[0], "data_%x.bin", sum);
            dump_binary(filename, rsp.RDRAM + (task->data_ptr & 0x7fffff), task->data_size);
        }
    }
    else
    {
        // For ucodes that are not run using the osSpTask* functions

        // Try to identify the RSP code we should run
        sum = sum_bytes(rsp.IMEM, 0x1000 >> 1);

        switch(sum)
        {
        // CIC 6105 IPL3 run some code on the RSP
        // We only emulate the part that modify RDRAM
        //
        // It is used for instance in Banjo Tooie, Zelda, Perfect Dark...
        case 0x9e2: // banjo tooie (U)
        case 0x9f2: // banjo tooie (E)
            {
            int i,j;
            memcpy(rsp.IMEM + 0x120, rsp.RDRAM + 0x1e8, 0x1f0);
            for (j=0; j<0xfc; j++)
                for (i=0; i<8; i++)
                    *(rsp.RDRAM+((0x2fb1f0+j*0xff0+i)^S8))=*(rsp.IMEM+((0x120+j*8+i)^S8));
            return Cycles;
            }
        }

        DebugMessage(M64MSG_WARNING, "unknown RSP code: sum: %x PC:%x", sum, *rsp.SP_PC_REG);

        // dump IMEM & DMEM for further analysis
        sprintf(&filename[0], "imem_%x.bin", sum);
        dump_binary(filename, rsp.IMEM, 0x1000);

        sprintf(&filename[0], "dmem_%x.bin", sum);
        dump_binary(filename, rsp.DMEM, 0x1000);
    }

    return Cycles;
}

EXPORT void CALL InitiateRSP(RSP_INFO Rsp_Info, unsigned int *CycleCount)
{
    rsp = Rsp_Info;
}

EXPORT void CALL RomClosed(void)
{
    int i;
    for (i=0; i<0x1000; i++)
        rsp.DMEM[i] = rsp.IMEM[i] = 0;

    //init_ucode1();
    init_ucode2();
}


/* local helper functions */
static unsigned int sum_bytes(const unsigned char *bytes, unsigned int size)                     
{                                                                                                
    unsigned int sum = 0;                                                                        
    const unsigned char * const bytes_end = bytes + size;                                        
                                                                                                 
    while (bytes != bytes_end)                                                                   
        sum += *bytes++;                                                                         
                                                                                                 
    return sum;                                                                                  
}   


static void dump_binary(char *filename, unsigned char *bytes, unsigned size)
{
    FILE *f;

    // if file already exists, do nothing
    f = fopen(filename, "r");
    if (f == NULL)
    {
        // else we write bytes to the file
        f= fopen(filename, "wb");
        if (f != NULL) {
            if (fwrite(bytes, 1, size, f) != size)
            {
                DebugMessage(M64MSG_ERROR, "Writing error on %s", filename);
            }
            fclose(f);
        }
        else
        {
            DebugMessage(M64MSG_ERROR, "Couldn't open %s for writing !", filename);
        }
    }
    else
    {
        fclose(f);
    }
}

static void dump_task(char *filename, const OSTask_t * const task)                               
{                                                                                                
    FILE *f;                                                                                     
                                                                                                 
    f = fopen(filename, "r");                                                                    
    if (f == NULL)                                                                               
    {                                                                                            
        f = fopen(filename, "w");                                                                
        fprintf(f,                                                                               
            "type = %d\n"                                                                        
            "flags = %d\n"                                                                       
            "ucode_boot  = %#08x size  = %#x\n"                                                  
            "ucode       = %#08x size  = %#x\n"                                                  
            "ucode_data  = %#08x size  = %#x\n"                                                  
            "dram_stack  = %#08x size  = %#x\n"                                                  
            "output_buff = %#08x *size = %#x\n"                                                  
            "data        = %#08x size  = %#x\n"                                                  
            "yield_data  = %#08x size  = %#x\n",                                                 
            task->type, task->flags,                                                             
            task->ucode_boot, task->ucode_boot_size,                                             
            task->ucode, task->ucode_size,                                                       
            task->ucode_data, task->ucode_data_size,                                             
            task->dram_stack, task->dram_stack_size,                                             
            task->output_buff, task->output_buff_size,                                           
            task->data_ptr, task->data_size,                                                     
            task->yield_data_ptr, task->yield_data_size);                                        
        fclose(f);                                                                               
    }                                                                                            
    else                                                                                         
    {                                                                                            
        fclose(f);                                                                               
    }                                                                                            
}        

