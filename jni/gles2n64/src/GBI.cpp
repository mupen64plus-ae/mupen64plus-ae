#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include "gles2N64.h"
#include "GBI.h"
#include "RDP.h"
#include "RSP.h"
#include "F3D.h"
#include "F3DEX.h"
#include "F3DEX2.h"
#include "L3D.h"
#include "L3DEX.h"
#include "L3DEX2.h"
#include "S2DEX.h"
#include "S2DEX2.h"
#include "F3DDKR.h"
#include "F3DWRUS.h"
#include "F3DPD.h"
#include "F3DCBFD.h"
#include "Types.h"
# include <string.h>
# include <unistd.h>
# include <stdlib.h>
# include "convert.h"
#include "Common.h"
#include "ticks.h"

#include "CRC.h"
#include "Debug.h"

u32 uc_crc, uc_dcrc;
char uc_str[256];

SpecialMicrocodeInfo specialMicrocodes[] =
{
    {F3DWRUS, FALSE, 0xd17906e2, "RSP SW Version: 2.0D, 04-01-96"},
    {F3DWRUS, FALSE,  0x94c4c833, "RSP SW Version: 2.0D, 04-01-96"},
    {S2DEX, FALSE, 0x9df31081, "RSP Gfx ucode S2DEX  1.06 Yoshitaka Yasumoto Nintendo."},
    {F3DDKR, FALSE, 0x8d91244f, "Diddy Kong Racing"},
    {F3DDKR, FALSE, 0x6e6fc893, "Diddy Kong Racing"},
    {F3DDKR, FALSE, 0xbde9d1fb, "Jet Force Gemini"},
    {F3DPD, FALSE, 0x1c4f7869, "Perfect Dark"},
    {F3DEX, FALSE, 0x0ace4c3f, "Mario Kart"},
    //{F3DEX, FALSE, 0xda51ccdb, "Rogue Squadron"},
    //{F3DCBFD, FALSE, 0x1b4ace88, "RSP Gfx ucode F3DEXBG.NoN fifo 2.08  Yoshitaka Yasumoto 1999 Nintendo."},
};

u32 G_RDPHALF_1, G_RDPHALF_2, G_RDPHALF_CONT;
u32 G_SPNOOP;
u32 G_SETOTHERMODE_H, G_SETOTHERMODE_L;
u32 G_DL, G_ENDDL, G_CULLDL, G_BRANCH_Z;
u32 G_LOAD_UCODE;
u32 G_MOVEMEM, G_MOVEWORD;
u32 G_MTX, G_POPMTX;
u32 G_GEOMETRYMODE, G_SETGEOMETRYMODE, G_CLEARGEOMETRYMODE;
u32 G_TEXTURE;
u32 G_DMA_IO, G_DMA_DL, G_DMA_TRI, G_DMA_MTX, G_DMA_VTX, G_DMA_OFFSETS;
u32 G_SPECIAL_1, G_SPECIAL_2, G_SPECIAL_3;
u32 G_VTX, G_MODIFYVTX, G_VTXCOLORBASE;
u32 G_TRI1, G_TRI2, G_TRI4;
u32 G_QUAD, G_LINE3D;
u32 G_RESERVED0, G_RESERVED1, G_RESERVED2, G_RESERVED3;
u32 G_SPRITE2D_BASE;
u32 G_BG_1CYC, G_BG_COPY;
u32 G_OBJ_RECTANGLE, G_OBJ_SPRITE, G_OBJ_MOVEMEM;
u32 G_SELECT_DL, G_OBJ_RENDERMODE, G_OBJ_RECTANGLE_R;
u32 G_OBJ_LOADTXTR, G_OBJ_LDTX_SPRITE, G_OBJ_LDTX_RECT, G_OBJ_LDTX_RECT_R;
u32 G_RDPHALF_0, G_TRI_UNKNOWN;

u32 G_MTX_STACKSIZE;
u32 G_MTX_MODELVIEW;
u32 G_MTX_PROJECTION;
u32 G_MTX_MUL;
u32 G_MTX_LOAD;
u32 G_MTX_NOPUSH;
u32 G_MTX_PUSH;

u32 G_TEXTURE_ENABLE;
u32 G_SHADING_SMOOTH;
u32 G_CULL_FRONT;
u32 G_CULL_BACK;
u32 G_CULL_BOTH;
u32 G_CLIPPING;

u32 G_MV_VIEWPORT;

u32 G_MWO_aLIGHT_1, G_MWO_bLIGHT_1;
u32 G_MWO_aLIGHT_2, G_MWO_bLIGHT_2;
u32 G_MWO_aLIGHT_3, G_MWO_bLIGHT_3;
u32 G_MWO_aLIGHT_4, G_MWO_bLIGHT_4;
u32 G_MWO_aLIGHT_5, G_MWO_bLIGHT_5;
u32 G_MWO_aLIGHT_6, G_MWO_bLIGHT_6;
u32 G_MWO_aLIGHT_7, G_MWO_bLIGHT_7;
u32 G_MWO_aLIGHT_8, G_MWO_bLIGHT_8;

//GBIFunc GBICmd[256];
GBIInfo GBI;

void GBI_Unknown( u32 w0, u32 w1 )
{
}

#if 0
INT_PTR CALLBACK MicrocodeDlgProc( HWND hWndDlg, UINT uMsg, WPARAM wParam, LPARAM lParam )
{
    switch (uMsg)
    {
        case WM_INITDIALOG:
            for (int i = 0; i < numMicrocodeTypes; i++)
            {
                SendDlgItemMessage( hWndDlg, IDC_MICROCODE, CB_ADDSTRING, 0, (LPARAM)MicrocodeTypes[i] );
            }
            SendDlgItemMessage( hWndDlg, IDC_MICROCODE, CB_SETCURSEL, 0, 0 );

            char text[1024];
            sprintf( text, "Microcode CRC:\t\t0x%08x\r\nMicrocode Data CRC:\t0x%08x\r\nMicrocode Text:\t\t%s", uc_crc, uc_dcrc, uc_str );
            SendDlgItemMessage( hWndDlg, IDC_TEXTBOX, WM_SETTEXT, NULL, (LPARAM)text );
            return TRUE;

        case WM_COMMAND:
            switch (LOWORD(wParam))
            {
                case IDOK:
                    EndDialog( hWndDlg, SendDlgItemMessage( hWndDlg, IDC_MICROCODE, CB_GETCURSEL, 0, 0 ) );
                    return TRUE;

                case IDCANCEL:
                    EndDialog( hWndDlg, NONE );
                    return TRUE;
            }
            break;
    }

    return FALSE;
}
#elif defined(USE_GTK)
static int selectedMicrocode = -1;
static GtkWidget *microcodeWindow = 0;
static GtkWidget *microcodeList = 0;

static void okButton_clicked( GtkWidget *widget, void *data )
{
    gtk_widget_hide( microcodeWindow );
    if (GTK_LIST(microcodeList)->selection != 0)
    {
        char *text = 0;
        GtkListItem *item = GTK_LIST_ITEM(GTK_LIST(microcodeList)->selection->data);
        GtkLabel *label = GTK_LABEL(GTK_BIN(item)->child);
        gtk_label_get( label, &text );
        if (text != 0)
            for (int i = 0; i < numMicrocodeTypes; i++)
                if (!strcmp( text, MicrocodeTypes[i] ))
                {
                    selectedMicrocode = i;
                    return;
                }
    }

    selectedMicrocode = NONE;
}

static void stopButton_clicked( GtkWidget *widget, void *data )
{
    gtk_widget_hide( microcodeWindow );
    selectedMicrocode = NONE;
}

static gint
delete_question_event(GtkWidget *widget, GdkEvent *event, gpointer data)
{
    return TRUE; // undeleteable
}

static int MicrocodeDialog()
{
    GtkWidget *infoLabel;
    GtkWidget *infoFrame, *infoTable;
    GtkWidget *crcInfoLabel, *crcDataInfoLabel, *textInfoLabel;
    GtkWidget *crcLabel = NULL, *crcDataLabel = NULL, *textLabel = NULL;
    GtkWidget *selectUcodeLabel;
    //GtkWidget *microcodeLabel;
    GtkWidget *okButton, *stopButton;
    GList *ucodeList = 0;
    char buf[1024];

    if (!g_thread_supported())
        g_thread_init( NULL );
    gdk_threads_enter();

    // create dialog
    if (microcodeWindow == 0)
    {
        microcodeWindow = gtk_dialog_new();
        gtk_signal_connect( GTK_OBJECT(microcodeWindow), "delete_event",
                            GTK_SIGNAL_FUNC(delete_question_event), (gpointer)NULL );
        sprintf( buf, "%s - unknown microcode", pluginName );
        gtk_window_set_title( GTK_WINDOW(microcodeWindow), buf );
        gtk_container_set_border_width( GTK_CONTAINER(GTK_DIALOG(microcodeWindow)->vbox), 11 );

        // ok button
        okButton = gtk_button_new_with_label( "Ok" );
        gtk_signal_connect_object( GTK_OBJECT(okButton), "clicked",
                               GTK_SIGNAL_FUNC(okButton_clicked), NULL );
        gtk_container_add( GTK_CONTAINER(GTK_DIALOG(microcodeWindow)->action_area), okButton );

        // stop button
        stopButton = gtk_button_new_with_label( "Stop" );
        gtk_signal_connect_object( GTK_OBJECT(stopButton), "clicked",
                               GTK_SIGNAL_FUNC(stopButton_clicked), NULL );
        gtk_container_add( GTK_CONTAINER(GTK_DIALOG(microcodeWindow)->action_area), stopButton );

        // info label
        infoLabel = gtk_label_new( "Unknown microcode. Please notify Orkin, including the following information:" );
        gtk_box_pack_start_defaults( GTK_BOX(GTK_DIALOG(microcodeWindow)->vbox), infoLabel );

        // info frame
        infoFrame = gtk_frame_new( "Microcode info" );
        gtk_container_set_border_width( GTK_CONTAINER(infoFrame), 7 );
        gtk_box_pack_start_defaults( GTK_BOX(GTK_DIALOG(microcodeWindow)->vbox), infoFrame );

        infoTable = gtk_table_new( 3, 2, FALSE );
        gtk_container_set_border_width( GTK_CONTAINER(infoTable), 7 );
        gtk_table_set_col_spacings( GTK_TABLE(infoTable), 3 );
        gtk_table_set_row_spacings( GTK_TABLE(infoTable), 3 );
        gtk_container_add( GTK_CONTAINER(infoFrame), infoTable );

        crcInfoLabel = gtk_label_new( "Microcode CRC:" );
        crcDataInfoLabel = gtk_label_new( "Microcode Data CRC:" );
        textInfoLabel = gtk_label_new( "Microcode Text:" );

        crcLabel = gtk_label_new( "" );
        crcDataLabel = gtk_label_new( "" );
        textLabel = gtk_label_new( "" );

        gtk_table_attach_defaults( GTK_TABLE(infoTable), crcInfoLabel, 0, 1, 0, 1 );
        gtk_table_attach_defaults( GTK_TABLE(infoTable), crcLabel, 1, 2, 0, 1 );
        gtk_table_attach_defaults( GTK_TABLE(infoTable), crcDataInfoLabel, 0, 1, 1, 2 );
        gtk_table_attach_defaults( GTK_TABLE(infoTable), crcDataLabel, 1, 2, 1, 2 );
        gtk_table_attach_defaults( GTK_TABLE(infoTable), textInfoLabel, 0, 1, 2, 3 );
        gtk_table_attach_defaults( GTK_TABLE(infoTable), textLabel, 1, 2, 2, 3 );

        selectUcodeLabel = gtk_label_new( "You can manually select the closest matching microcode." );
        for (int i = 0; i < numMicrocodeTypes; i++)
            ucodeList = g_list_append( ucodeList, gtk_list_item_new_with_label( MicrocodeTypes[i] ) );
        microcodeList = gtk_list_new();
        gtk_list_set_selection_mode( GTK_LIST(microcodeList), GTK_SELECTION_SINGLE );
        gtk_list_append_items( GTK_LIST(microcodeList), ucodeList );

        gtk_box_pack_start_defaults( GTK_BOX(GTK_DIALOG(microcodeWindow)->vbox), selectUcodeLabel );
        gtk_box_pack_start_defaults( GTK_BOX(GTK_DIALOG(microcodeWindow)->vbox), microcodeList );
    }

    snprintf( buf, 1024, "0x%8.8X", (unsigned int)uc_crc );
        if(crcLabel) gtk_label_set_text( GTK_LABEL(crcLabel), buf );
    snprintf( buf, 1024, "0x%8.8X", (unsigned int)uc_dcrc );
    if(crcDataLabel) gtk_label_set_text( GTK_LABEL(crcDataLabel), buf );
    if(textLabel) gtk_label_set_text( GTK_LABEL(textLabel), uc_str );

    selectedMicrocode = -1;
    gtk_widget_show_all( microcodeWindow );

    while (selectedMicrocode == -1)
    {
        if( gtk_main_iteration() )
            break;
        usleep( 10000 );
    }
    gdk_threads_leave();

    return selectedMicrocode;
}
#else
static int MicrocodeDialog()
{
    // FIXME
    return 0;
}
#endif

MicrocodeInfo *GBI_AddMicrocode()
{
    MicrocodeInfo *newtop = (MicrocodeInfo*)malloc( sizeof( MicrocodeInfo ) );

    newtop->lower = GBI.top;
    newtop->higher = NULL;

    if (GBI.top)
        GBI.top->higher = newtop;

    if (!GBI.bottom)
        GBI.bottom = newtop;

    GBI.top = newtop;

    GBI.numMicrocodes++;


    return newtop;
}

void GBI_Init()
{
    GBI.top = NULL;
    GBI.bottom = NULL;
    GBI.current = NULL;
    GBI.numMicrocodes = 0;

    for (u32 i = 0; i <= 0xFF; i++)
        GBI.cmd[i] = GBI_Unknown;

#ifdef PROFILE_GBI
    GBI_ProfileInit();
#endif
}

void GBI_Destroy()
{
    while (GBI.bottom)
    {
        MicrocodeInfo *newBottom = GBI.bottom->higher;

        if (GBI.bottom == GBI.top)
            GBI.top = NULL;

        free( GBI.bottom );

        GBI.bottom = newBottom;

        if (GBI.bottom)
            GBI.bottom->lower = NULL;

        GBI.numMicrocodes--;
    }
}

#ifdef PROFILE_GBI
void GBI_ProfileInit()
{
    GBI_ProfileReset();
}

void GBI_ProfileBegin(u32 cmd)
{
    GBI.profileTmp = ticksGetTicks();
}

void GBI_ProfileEnd(u32 cmd)
{
    unsigned int i = 256*GBI.current->type + cmd;
    GBI.profileNum[i]++;
    GBI.profileTimer[i] += ticksGetTicks() - GBI.profileTmp;
}

void
GBI_ProfileReset()
{
    memset(GBI.profileTimer, 0, 12 * 256 * sizeof(int));
    memset(GBI.profileNum, 0, 12 * 256 * sizeof(int));
}

u32
GBI_GetFuncTime(u32 ucode, u32 cmd)
{
    return GBI.profileTimer[ucode*256+cmd];
}

u32
GBI_GetFuncNum(u32 ucode, u32 cmd)
{
    return GBI.profileNum[ucode*256+cmd];
}

u32
GBI_ProfilePrint(FILE *file)
{
    int uc, cmd, total=0;

    for(uc=0;uc<12;uc++)
    {
        for(cmd=0;cmd<256;cmd++)
        {
            total += GBI_GetFuncTime(uc, cmd);
        }
    }


    for(uc=0;uc<12;uc++)
    {
        for(cmd=0;cmd<256;cmd++)
        {
            unsigned int t = GBI_GetFuncTime(uc, cmd);
            if (t != 0)
            {
                fprintf(file, "%s x %i = %u ms (%.2f%%)\n", GBI_GetFuncName(uc,cmd), GBI_GetFuncNum(uc, cmd), t, 100.0f * (float)t / total);
            }
        }
    }
    return total;
}

const char*
GBI_GetUcodeName(u32 ucode)
{
    switch(ucode)
    {
        case F3D:       return "F3D";
        case F3DEX:     return "F3DEX";
        case F3DEX2:    return "F3DEX2";
        case L3D:       return "L3D";
        case L3DEX:     return "L3DEX";
        case L3DEX2:    return "L3DEX2";
        case S2DEX:     return "S2DEX";
        case S2DEX2:    return "S2DEX2";
        case F3DPD:     return "F3DPD";
        case F3DDKR:    return "F3DDKR";
        case F3DWRUS:   return "F3DWRUS";
        case NONE:      return "NONE";
        default:        return "UNKNOWN UCODE";
    }
}

const char*
GBI_GetFuncName(unsigned int ucode, unsigned int cmd)
{
    switch(cmd)
    {
        //common
        case G_SETCIMG:             return "G_SETCIMG";
        case G_SETZIMG:             return "G_SETZIMG";
        case G_SETTIMG:             return "G_SETTIMG";
        case G_SETCOMBINE:          return "G_SETCOMBINE";
        case G_SETENVCOLOR:         return "G_SETENVCOLOR";
        case G_SETPRIMCOLOR:        return "G_SETPRIMCOLOR";
        case G_SETBLENDCOLOR:       return "G_SETBLENDCOLOR";
        case G_SETFOGCOLOR:         return "G_SETFOGCOLOR";
        case G_SETFILLCOLOR:        return "G_SETFILLCOLOR";
        case G_FILLRECT:            return "G_FILLRECT";
        case G_SETTILE:             return "G_SETTILE";
        case G_LOADTILE:            return "G_LOADTILE";
        case G_LOADBLOCK:           return "G_LOADBLOCK";
        case G_SETTILESIZE:         return "G_SETTILESIZE";
        case G_LOADTLUT:            return "G_LOADTLUT";
        case G_RDPSETOTHERMODE:     return "G_RDPSETOTHERMODE";
        case G_SETPRIMDEPTH:        return "G_SETPRIMDEPTH";
        case G_SETSCISSOR:          return "G_SETSCISSOR";
        case G_SETCONVERT:          return "G_SETCONVERT";
        case G_SETKEYR:             return "G_SETKEYR";
        case G_SETKEYGB:            return "G_SETKEYGB";
        case G_RDPFULLSYNC:         return "G_RDPFULLSYNC";
        case G_RDPTILESYNC:         return "G_RDPTILESYNC";
        case G_RDPPIPESYNC:         return "G_RDPPIPESYNC";
        case G_RDPLOADSYNC:         return "G_RDPLOADSYNC";
        case G_TEXRECTFLIP:         return "G_TEXRECTFLIP";

        //ucode
        case 0x00:                  return "SPNOOP";

/*
        F3D_MTX:                0x01
        F3DEX2_VTX              0x01
        F3DDKR_DMA_MTX          0x01
        S2DEX_BG_1CYC           0x01
        S2DEX2_OBJ_RECTANGLE    0x01
*/
        case 0x01:
        {
            switch(ucode)
            {
                case F3DEX2:        return "F3DEX2_VTX";
                case F3DDKR:        return "F3DDKR_DMA_MTX";
                case S2DEX:         return "S2DEX_BG_1CYC";
                case S2DEX2:        return "S2DEX2_OBJ_RECTANGLE";
                default:            return "F3D_MTX";
            }
        }
/*
        F3D_RESERVED0:          0x02
        F3DEX2_MODIFYVTX        0x02
        S2DEX_BG_COPY           0x02
        S2DEX2_OBJ_SPRITE       0x02
*/
        case 0x02:
        {
            switch(ucode)
            {
                case F3DEX2:        return "F3DEX2_MODIFYVTX";
                case S2DEX:         return "S2DEX_BG_COPY";
                case S2DEX2:        return "S2DEX2_OBJ_SPRITE";
                default:            return "F3D_RESERVED0";
            }
        }
/*
        F3D_MOVEMEM:            0x03
        F3DEX2_CULLDL           0x03
        S2DEX_OBJ_RECTANGLE     0x03
*/
        case 0x03:
        {
            switch(ucode)
            {
                case F3DEX2:        return "F3DEX2_CULLDL";
                case S2DEX:         return "S2DEX_OBJ_RECTANGLE";
                default:            return "F3D_MOVEMEM";
            }
        }
/*
        F3D_VTX:                0x04
        F3DEX2_BRANCH_Z         0x04
        F3DDKR_DMA_VTX          0x04
        S2DEX_OBJ_SPRITE        0x04
        S2DEX2_SELECT_DL        0x04
*/
        case 0x04:
        {
            switch(ucode)
            {
                case F3DEX2:        return "F3DEX2_BRANCH_Z";
                case F3DDKR:        return "F3DDKR_DMA_VTX";
                case S2DEX:         return "S2DEX_OBJ_SPRITE";
                case S2DEX2:        return "S2DEX2_SELECT_DL";
                default:            return "F3D_VTX";
            }
        }

/*
        F3D_RESERVED1:          0x05
        F3DEX2_TRI1             0x05
        F3DDKR_DMA_TRI          0x05
        S2DEX_OBJ_MOVEMEM       0x05
        S2DEX2_OBJ_LOADTXTR     0x05
*/
        case 0x05:
        {
            switch(ucode)
            {
                case F3DEX2:        return "F3DEX2_TR1";
                case F3DDKR:        return "F3DDKR_DMA_TRI";
                case S2DEX:         return "S2DEX_OBJ_MOVEMEM";
                case S2DEX2:        return "S2DEX2_OBJ_LOADTXTR";
                default:            return "F3D_RESERVED1";
            }
        }
/*
        F3D_DL:                 0x06
        F3DEX2_TRI2             0x06
        S2DEX2_OBJ_LDTX_SPRITE  0x06
*/
        case 0x06:
        {
            switch(ucode)
            {
                case F3DEX2:        return "F3DEX2_TR2";
                case S2DEX2:        return "S2DEX2_OBJ_LDTX_SPRITE";
                default:            return "F3D_DL";
            }
        }

/*
        F3D_RESERVED2:          0x07
        F3DEX2_QUAD             0x07
        F3DPD_VTXCOLORBASE      0x07
        F3DDKR_DMA_DL           0x07
        S2DEX2_OBJ_LDTX_RECT    0x07
*/
        case 0x07:
        {
            switch(ucode)
            {
                case F3DEX2:        return "F3DEX2_QUAD";
                case F3DPD:         return "F3DPD_VTXCOLORBASE";
                case F3DDKR:        return "F3DDKR_DMA_DL";
                case S2DEX2:        return "S2DEX2_OBJ_LDTX_RECT";
                default:            return "F3D_RESERVED2";
            }
        }
/*
        F3D_RESERVED3:          0x08
        L3DEX2_LINE3D           0x08
        S2DEX2_OBJ_LDTX_RECT_R  0x08
*/
        case 0x08:
        {
            switch(ucode)
            {
                case L3DEX2:        return "L3DEX2_LINE3D";
                case S2DEX2:        return "S2DEX2_OBJ_LDTX_RECT_R";
                default:            return "F3D_RESERVED3";
            }
        }

/*
        F3D_SPRITE2D_BASE:      0x09
        S2DEX2_BG_1CYC          0x09
*/
        case 0x09:
        {
            switch(ucode)
            {
                case S2DEX2:        return "S2DEX2_BG_1CYC";
                default:            return "F3D_SPRITE2D_BASE";
            }
        }

//        S2DEX2_BG_COPY          0x0A
        case 0x0A:                  return "S2DEX2_BG_COPY";
//        S2DEX2_OBJ_RENDERMODE   0x0B
        case 0x0B:                  return "S2DEX2_OBJ_RENDERMODE";
//        F3DEX2_RDPHALF_2        0xF1
        case 0xF1:                  return "F3DEX2_RDPHALF_2";
/*
        S2DEX_RDPHALF_0         0xE4
        S2DEX2_RDPHALF_0        0xE4
*/
        case 0xE4:
        {
            switch(ucode)
            {
                case S2DEX:         return "S2DEX_RDPHALF_0";
                case S2DEX2:        return "S2DEX2_RDPHALF_0";
                default:            return "G_TEXRECT";
            }
        }
//        F3DEX2_SETOTHERMODE_H   0xE3
        case 0xE3:                  return "F3DEX2_SETOTHERMODE_H";
//        F3DEX2_SETOTHERMODE_L   0xE2
        case 0xE2:                  return "F3DEX2_SETOTHERMODE_L";
//        F3DEX2_RDPHALF_1        0xE1
        case 0xE1:                  return "F3DEX2_RDPHALF_1";
//        F3DEX2_SPNOOP           0xE0
        case 0xE0:                  return "F3DEX2_SPNOOP";
//        F3DEX2_ENDDL            0xDF
        case 0xDF:                  return "F3DEX2_ENDDL";
//        F3DEX2_DL               0xDE
        case 0xDE:                  return "F3DEX2_DL";
//        F3DEX2_LOAD_UCODE       0xDD
        case 0xDD:                  return "F3DEX2_LOAD_UCODE";
/*
        F3DEX2_MOVEMEM          0xDC
        S2DEX2_OBJ_MOVEMEM      0xDC
*/
        case 0xDC:
        {
            switch(ucode)
            {
                case S2DEX2:        return "S2DEX2_OBJ_MOVEMEM";
                default:            return "F3DEX2_MOVEMEM";
            }
        }
//        F3DEX2_MOVEWORD         0xDB
        case 0xDB:                  return "F3DEX2_MOVEWORD";
/*
        F3DEX2_MTX              0xDA
        S2DEX2_OBJ_RECTANGLE_R  0xDA
*/
        case 0xDA:
        {
            switch(ucode)
            {
                case S2DEX2:        return "S2DEX2_OBJ_RECTANGLE_R";
                default:            return "F3DEX2_MTX";
            }
        }
//        F3DEX2_GEOMETRYMODE     0xD9
        case 0xD9:                  return "F3DEX2_GEOMETRYMODE";
//        F3DEX2_POPMTX           0xD8
        case 0xD8:                  return "F3DEX2_POPMTX";
//        F3DEX2_TEXTURE          0xD7
        case 0xD7:                  return "F3DEX2_TEXTURE";
//        F3DEX2_DMA_IO           0xD6
        case 0xD6:                  return "F3DEX2_DMA_IO";
//        F3DEX2_SPECIAL_1        0xD5
        case 0xD5:                  return "F3DEX2_SPECIAL_1";
//        F3DEX2_SPECIAL_2        0xD4
        case 0xD4:                  return "F3DEX2_SPECIAL_2";
//        F3DEX2_SPECIAL_3        0xD3
        case 0xD3:                  return "F3DEX2_SPECIAL_3";

//        S2DEX_OBJ_LOADTXTR      0xC1
        case 0xC1:                  return "S2DEX_OBJ_LOADTXTR";
//        S2DEX_OBJ_LDTX_SPRITE   0xC2
        case 0xC2:                  return "S2DEX_OBJ_LDTX_SPRITE";
//        S2DEX_OBJ_LDTX_RECT     0xC3
        case 0xC3:                  return "S2DEX_OBJ_LDTX_RECT";
//        S2DEX_OBJ_LDTX_RECT_R   0xC4
        case 0xC4:                  return "S2DEX_OBJ_LDTX_RECT_R";
/*
        F3D_TRI1:               0xBF
        F3DDKR_DMA_OFFSETS      0xBF
*/
        case 0xBF:
        {
            switch(ucode)
            {
                case F3DDKR:        return "F3DDKR_DMA_OFFSETS";
                default:            return "F3D_TRI1";
            }
        }

//        F3D_CULLDL:             0xBE
        case 0xBE:                  return "F3D_CULLDL";
//        F3D_POPMTX:             0xBD
        case 0xBD:                  return "F3D_POPMTX";
//        F3D_MOVEWORD:           0xBC
        case 0xBC:                  return "F3D_MOVEWORD";
//        F3D_TEXTURE:            0xBB
        case 0xBB:                  return "F3D_TEXTURE";
//        F3D_SETOTHERMODE_H:     0xBA
        case 0xBA:                  return "F3D_SETOTHERMODE_H";
//        F3D_SETOTHERMODE_L:     0xB9
        case 0xB9:                  return "F3D_SETOTHERMODE_L";
//        F3D_ENDDL:              0xB8
        case 0xB8:                  return "F3D_ENDDL";
//        F3D_SETGEOMETRYMODE:    0xB7
        case 0xB7:                  return "F3D_SETGEOMETRYMODE";
//        F3D_CLEARGEOMETRYMODE:  0xB6
        case 0xB6:                  return "F3D_CLEARGEOMETRYMODE";
/*
        F3D_QUAD:               0xB5
        L3D_LINE3D              0xB5
*/
        case 0xB5:
        {
            switch(ucode)
            {
                case L3D:           return "L3D_LINE3D";
                default:            return "F3D_QUAD";
            }
        }

//        F3D_RDPHALF_1:          0xB4
        case 0xB4:                  return "F3D_RDPHALF_1";
//        F3D_RDPHALF_2:          0xB3
        case 0xB3:                  return "F3D_RDPHALF_2";
/*
        F3D_RDPHALF_CONT:       0xB2
        F3DEX_MODIFYVTX         0xB2
        S2DEX_OBJ_RECTANGLE_R   0xB2
*/
        case 0xB2:
        {
            switch(ucode)
            {
                case F3DEX:         return "F3DEX_MODIFYVTX";
                case S2DEX:         return "S2DEX_OBJ_RECTANGLE_R";
                default:            return "F3D_RDPHALF_CONT";
            }
        }
/*
        F3D_TRI4:               0xB1
        F3DEX_TRI2              0xB1
        F3DWRUS_TRI2            0xB1
        S2DEX_OBJ_RENDERMODE    0xB1
*/
        case 0xB1:
        {
            switch(ucode)
            {
                case F3DEX:         return "F3DEX_TRI2";
                case F3DWRUS:       return "F3DWRUS_TRI2";
                case S2DEX:         return "S2DEX_OBJ_RENDERMODE";
                default:            return "F3D_TRI4";
            }
        }
/*
        F3DEX_BRANCH_Z          0xB0
        S2DEX_SELECT_DL         0xB0
*/
        case 0xB0:
        {
            switch(ucode)
            {
                case S2DEX:         return "S2DEX_SELECT_DL";
                default:            return "F3DEX_BRANCH_Z";
            }
        }
/*
        F3DEX_LOAD_UCODE        0xAF
        S2DEX_LOAD_UCODE        0xAF
*/
        case 0xAF:
        {
            switch(ucode)
            {
                case S2DEX:         return "S2DEX_LOAD_UCODE";
                default:            return "F3DEX_LOAD_UCODE";
            }
        }

        default:
        {
            if (ucode == F3DCBFD)
            {
                if (cmd >= 0x10 && cmd <= 0x1f)
                    return "F3DCBFD_TRI4";

            }
            return "UNKNOWN CMD";
        }
    }
}
#endif

MicrocodeInfo *GBI_DetectMicrocode( u32 uc_start, u32 uc_dstart, u16 uc_dsize )
{
    MicrocodeInfo *current;

    for (unsigned int i = 0; i < GBI.numMicrocodes; i++)
    {
        current = GBI.top;

        while (current)
        {
            if ((current->address == uc_start) && (current->dataAddress == uc_dstart) && (current->dataSize == uc_dsize))
                return current;

            current = current->lower;
        }
    }

    current = GBI_AddMicrocode();

    current->address = uc_start;
    current->dataAddress = uc_dstart;
    current->dataSize = uc_dsize;
    current->NoN = FALSE;
    current->type = NONE;

    // See if we can identify it by CRC
    uc_crc = CRC_Calculate( 0xFFFFFFFF, &RDRAM[uc_start & 0x1FFFFFFF], 4096);
    LOG(LOG_MINIMAL, "UCODE CRC=0x%x\n", uc_crc);

    for (u32 i = 0; i < sizeof( specialMicrocodes ) / sizeof( SpecialMicrocodeInfo ); i++)
    {
        if (uc_crc == specialMicrocodes[i].crc)
        {
            current->type = specialMicrocodes[i].type;
            return current;
        }
    }

    // See if we can identify it by text
    char uc_data[2048];
    UnswapCopy( &RDRAM[uc_dstart & 0x1FFFFFFF], uc_data, 2048 );
    strcpy( uc_str, "Not Found" );

    for (u32 i = 0; i < 2048; i++)
    {
        if ((uc_data[i] == 'R') && (uc_data[i+1] == 'S') && (uc_data[i+2] == 'P'))
        {
            u32 j = 0;
            while (uc_data[i+j] > 0x0A)
            {
                uc_str[j] = uc_data[i+j];
                j++;
            }

            uc_str[j] = 0x00;

            int type = NONE;

            if (strncmp( &uc_str[4], "SW", 2 ) == 0)
            {
                type = F3D;
            }
            else if (strncmp( &uc_str[4], "Gfx", 3 ) == 0)
            {
                current->NoN = (strncmp( &uc_str[20], ".NoN", 4 ) == 0);

                if (strncmp( &uc_str[14], "F3D", 3 ) == 0)
                {
                    if (uc_str[28] == '1')
                        type = F3DEX;
                    else if (uc_str[31] == '2')
                        type = F3DEX2;
                }
                else if (strncmp( &uc_str[14], "L3D", 3 ) == 0)
                {
                    if (uc_str[28] == '1')
                        type = L3DEX;
                    else if (uc_str[31] == '2')
                        type = L3DEX2;
                }
                else if (strncmp( &uc_str[14], "S2D", 3 ) == 0)
                {
                    if (uc_str[28] == '1')
                        type = S2DEX;
                    else if (uc_str[31] == '2')
                        type = S2DEX2;
                }
            }

            LOG(LOG_VERBOSE, "UCODE STRING=%s\n", uc_str);

            if (type != NONE)
            {
                current->type = type;
                return current;
            }

            break;
        }
    }


    for (u32 i = 0; i < sizeof( specialMicrocodes ) / sizeof( SpecialMicrocodeInfo ); i++)
    {
        if (strcmp( uc_str, specialMicrocodes[i].text ) == 0)
        {
            current->type = specialMicrocodes[i].type;
            return current;
        }
    }

    // Let the user choose the microcode
    LOG(LOG_ERROR, "[gles2n64]: Warning - unknown ucode!!!\n");
    if(last_good_ucode != (u32)-1)
    {
        current->type=last_good_ucode;
    }
    else
    {
        current->type = MicrocodeDialog();
    }
    return current;
}

void GBI_MakeCurrent( MicrocodeInfo *current )
{
    if (current != GBI.top)
    {
        if (current == GBI.bottom)
        {
            GBI.bottom = current->higher;
            GBI.bottom->lower = NULL;
        }
        else
        {
            current->higher->lower = current->lower;
            current->lower->higher = current->higher;
        }

        current->higher = NULL;
        current->lower = GBI.top;
        GBI.top->higher = current;
        GBI.top = current;
    }

    if (!GBI.current || (GBI.current->type != current->type))
    {

        for (int i = 0; i <= 0xFF; i++)
            GBI.cmd[i] = GBI_Unknown;

        RDP_Init();
        switch (current->type)
        {
            case F3D:       F3D_Init();     break;
            case F3DEX:     F3DEX_Init();   break;
            case F3DEX2:    F3DEX2_Init();  break;
            case L3D:       L3D_Init();     break;
            case L3DEX:     L3DEX_Init();   break;
            case L3DEX2:    L3DEX2_Init();  break;
            case S2DEX:     S2DEX_Init();   break;
            case S2DEX2:    S2DEX2_Init();  break;
            case F3DDKR:    F3DDKR_Init();  break;
            case F3DWRUS:   F3DWRUS_Init(); break;
            case F3DPD:     F3DPD_Init();   break;
            case F3DCBFD:   F3DCBFD_Init(); break;
        }
    }


    GBI.current = current;
}

