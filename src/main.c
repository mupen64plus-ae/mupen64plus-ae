/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - main.c                                                  *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
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

#ifdef __WIN32__
#include <windows.h>
#include "./winproject/resource.h"
#include "./win/win.h"
#else
#ifdef USE_GTK
#include <gtk/gtk.h>
#endif
#include "wintypes.h"
#include <string.h>
#endif
#include <stdio.h>

#include "Audio_1.1.h"
#include "Rsp_1.1.h"
#include "hle.h"

RSP_INFO rsp;

BOOL AudioHle = FALSE, GraphicsHle = TRUE, SpecificHle = FALSE;

#ifdef __WIN32__
extern void (*processAList)();
static BOOL firstTime = TRUE;
void loadPlugin();
#endif

__declspec(dllexport) void CloseDLL (void)
{
}

__declspec(dllexport) void DllAbout ( HWND hParent )
{
#ifdef __WIN32__
   MessageBox(NULL, "Mupen64 HLE RSP plugin v0.2 with Azimers code by Hacktarux", "RSP HLE", MB_OK);
#else
#ifdef USE_GTK
   char tMsg[256];
   GtkWidget *dialog, *label, *okay_button;

   dialog = gtk_dialog_new();
   sprintf(tMsg,"Mupen64 HLE RSP plugin v0.2 with Azimers code by Hacktarux");
   label = gtk_label_new(tMsg);
   okay_button = gtk_button_new_with_label("OK");

   gtk_signal_connect_object(GTK_OBJECT(okay_button), "clicked",
                 GTK_SIGNAL_FUNC(gtk_widget_destroy),
                 GTK_OBJECT(dialog));
   gtk_container_add(GTK_CONTAINER(GTK_DIALOG(dialog)->action_area),
             okay_button);

   gtk_container_add(GTK_CONTAINER(GTK_DIALOG(dialog)->vbox),
             label);
   gtk_window_set_modal(GTK_WINDOW(dialog), TRUE);
   gtk_widget_show_all(dialog);
#else
   char tMsg[256];
   sprintf(tMsg,"Mupen64 HLE RSP plugin v0.2 with Azimers code by Hacktarux");
   fprintf(stderr, "About\n%s\n", tMsg);
#endif
#endif
}

__declspec(dllexport) void DllConfig ( HWND hParent )
{
#ifdef __WIN32__
    if (firstTime)
    DialogBox(dll_hInstance,
                     MAKEINTRESOURCE(IDD_RSPCONFIG), hParent, ConfigDlgProc);
   //MessageBox(NULL, "no config", "noconfig", MB_OK);
#endif
}

__declspec(dllexport) void DllTest ( HWND hParent )
{
#ifdef __WIN32__
   MessageBox(NULL, "no test", "no test", MB_OK);
#endif
}

static int audio_ucode_detect(OSTask_t *task)
{
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

extern void (*ABI1[0x20])();
extern void (*ABI2[0x20])();
extern void (*ABI3[0x20])();

void (*ABI[0x20])();

u32 inst1, inst2;

static int audio_ucode(OSTask_t *task)
{
    unsigned int *p_alist = (unsigned int*)(rsp.RDRAM + task->data_ptr);
    unsigned int i;

    switch(audio_ucode_detect(task))
    {
    case 1: // mario ucode
        memcpy( ABI, ABI1, sizeof(ABI[0])*0x20 );
        break;
    case 2: // banjo kazooie ucode
        memcpy( ABI, ABI2, sizeof(ABI[0])*0x20 );
        break;
    case 3: // zelda ucode
        memcpy( ABI, ABI3, sizeof(ABI[0])*0x20 );
        break;
    default:
        {
/*      char s[1024];
        sprintf(s, "unknown audio\n\tsum:%x", sum);
#ifdef __WIN32__
        MessageBox(NULL, s, "unknown task", MB_OK);
#else
        printf("%s\n", s);
#endif*/
        return -1;
        }
    }

//  data = (short*)(rsp.RDRAM + task->ucode_data);

    for (i = 0; i < (task->data_size/4); i += 2)
    {
        inst1 = p_alist[i];
        inst2 = p_alist[i+1];
        ABI[inst1 >> 24]();
    }

    return 0;
}

__declspec(dllexport) DWORD DoRspCycles ( DWORD Cycles )
{
   OSTask_t *task = (OSTask_t*)(rsp.DMEM + 0xFC0);
   unsigned int i, sum=0;
#ifdef __WIN32__
   if(firstTime)
   {
      firstTime=FALSE;
      if (SpecificHle)
            loadPlugin();
   }
#endif

   if( task->type == 1 && task->data_ptr != 0 && GraphicsHle) {
      if (rsp.ProcessDlistList != NULL) {
     rsp.ProcessDlistList();
      }
      *rsp.SP_STATUS_REG |= 0x0203;
      if ((*rsp.SP_STATUS_REG & 0x40) != 0 ) {
     *rsp.MI_INTR_REG |= 0x1;
     rsp.CheckInterrupts();
      }

      *rsp.DPC_STATUS_REG &= ~0x0002;
      return Cycles;
   } else if (task->type == 2 && AudioHle) {
#ifdef __WIN32__
      if (SpecificHle)
            processAList();
      else
#endif
      if (rsp.ProcessAlistList != NULL) {
     rsp.ProcessAlistList();
      }
      *rsp.SP_STATUS_REG |= 0x0203;
      if ((*rsp.SP_STATUS_REG & 0x40) != 0 ) {
     *rsp.MI_INTR_REG |= 0x1;
     rsp.CheckInterrupts();
      }
      return Cycles;
   } else if (task->type == 7) {
      rsp.ShowCFB();
   }

   *rsp.SP_STATUS_REG |= 0x203;
   if ((*rsp.SP_STATUS_REG & 0x40) != 0 )
     {
    *rsp.MI_INTR_REG |= 0x1;
    rsp.CheckInterrupts();
     }

   if (task->ucode_size <= 0x1000)
     for (i=0; i<(task->ucode_size/2); i++)
       sum += *(rsp.RDRAM + task->ucode + i);
   else
     for (i=0; i<(0x1000/2); i++)
       sum += *(rsp.IMEM + i);


   if (task->ucode_size > 0x1000)
     {
    switch(sum)
      {
       case 0x9E2: // banjo tooie (U) boot code
           {
          int i,j;
          memcpy(rsp.IMEM + 0x120, rsp.RDRAM + 0x1e8, 0x1e8);
          for (j=0; j<0xfc; j++)
            for (i=0; i<8; i++)
              *(rsp.RDRAM+((0x2fb1f0+j*0xff0+i)^S8))=*(rsp.IMEM+((0x120+j*8+i)^S8));
           }
         return Cycles;
         break;
       case 0x9F2: // banjo tooie (E) + zelda oot (E) boot code
           {
          int i,j;
          memcpy(rsp.IMEM + 0x120, rsp.RDRAM + 0x1e8, 0x1e8);
          for (j=0; j<0xfc; j++)
            for (i=0; i<8; i++)
              *(rsp.RDRAM+((0x2fb1f0+j*0xff0+i)^S8))=*(rsp.IMEM+((0x120+j*8+i)^S8));
           }
         return Cycles;
         break;
      }
     }
   else
     {
    switch(task->type)
      {
       case 2: // audio
         if (audio_ucode(task) == 0)
           return Cycles;
         break;
       case 4: // jpeg
         switch(sum)
           {
        case 0x278: // used by zelda during boot
          *rsp.SP_STATUS_REG |= 0x200;
          return Cycles;
          break;
        case 0x2e4fc: // uncompress
          jpg_uncompress(task);
          return Cycles;
          break;
        default:
            {
               char s[1024];
               sprintf(s, "unknown jpeg:\n\tsum:%x", sum);
#ifdef __WIN32__
               MessageBox(NULL, s, "unknown task", MB_OK);
#else
               printf("%s\n", s);
#endif
            }
           }
         break;
      }
     }

     {
    char s[1024];
    FILE *f;
    sprintf(s, "unknown task:\n\ttype:%d\n\tsum:%x\n\tPC:%lx", (int)task->type, sum, (long) rsp.SP_PC_REG);
#ifdef __WIN32__
    MessageBox(NULL, s, "unknown task", MB_OK);
#else
    printf("%s\n", s);
#endif

    if (task->ucode_size <= 0x1000)
      {
         f = fopen("imem.dat", "wb");
         fwrite(rsp.RDRAM + task->ucode, task->ucode_size, 1, f);
         fclose(f);

         f = fopen("dmem.dat", "wb");
         fwrite(rsp.RDRAM + task->ucode_data, task->ucode_data_size, 1, f);
         fclose(f);
      }
    else
      {
         f = fopen("imem.dat", "wb");
         fwrite(rsp.IMEM, 0x1000, 1, f);
         fclose(f);

         f = fopen("dmem.dat", "wb");
         fwrite(rsp.DMEM, 0x1000, 1, f);
         fclose(f);
      }
     }

   return Cycles;
}

__declspec(dllexport) void GetDllInfo ( PLUGIN_INFO * PluginInfo )
{
   PluginInfo->Version = 0x0101;
   PluginInfo->Type = PLUGIN_TYPE_RSP;
   strcpy(PluginInfo->Name, "Hacktarux/Azimer hle rsp plugin");
   PluginInfo->NormalMemory = TRUE;
   PluginInfo->MemoryBswaped = TRUE;
}

__declspec(dllexport) void InitiateRSP ( RSP_INFO Rsp_Info, DWORD * CycleCount)
{
   rsp = Rsp_Info;
}

__declspec(dllexport) void RomClosed (void)
{
   int i;
   for (i=0; i<0x1000; i++)
     {
    rsp.DMEM[i] = rsp.IMEM[i] = 0;
     }
/*   init_ucode1();
   init_ucode2();*/
#ifdef __WIN32__
   firstTime = TRUE;
#endif
}

