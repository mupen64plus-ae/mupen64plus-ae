/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - gui_gtk2.h                                              *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2008 Tillin9 wahrhaft                                   *
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

#include <stdlib.h>
#include <stdio.h>

#include <gtk/gtk.h>
#include "SDL.h"

#include "gui.h"
#include "gui_gtk2.h"
#include "main.h"

#include "../main/winlnxdefs.h"
#include "../main/version.h"

#define MAXTOOLTIP 512

extern Uint8 Resample;
extern Uint32 PrimaryBufferSize;
extern Uint32 SecondaryBufferSize;
extern Uint32 LowBufferLoadLevel;
extern Uint32 HighBufferLoadLevel;
extern int GameFreq;
extern int VolPercent;
extern int VolDelta;
extern int VolumeControlType;
extern BOOL SwapChannels;

extern void SaveConfig();

/*********************************************************************************************************
 * globals
 */
static SConfigureDialog g_ConfDialog;

/*********************************************************************************************************
 * callbacks
 */

/* Apply / Ok Button. */
static void callback_apply_changes(GtkWidget *widget, void *data)
{
    /* Update config. */
    GameFreq = atoi(gtk_entry_get_text(GTK_ENTRY(g_ConfDialog.defaultfrequencyEntry)));
    PrimaryBufferSize = atoi(gtk_entry_get_text(GTK_ENTRY(g_ConfDialog.primarybufferEntry)));
    SecondaryBufferSize = atoi(gtk_entry_get_text(GTK_ENTRY(g_ConfDialog.secondarybufferEntry)));
    LowBufferLoadLevel = atoi(gtk_entry_get_text(GTK_ENTRY(g_ConfDialog.lowbufferEntry)));
    HighBufferLoadLevel = atoi(gtk_entry_get_text(GTK_ENTRY(g_ConfDialog.highbufferEntry)));
    Resample = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfDialog.resampleCombo)) + 1;
    VolPercent = gtk_range_get_value(GTK_RANGE(g_ConfDialog.volumedefaultSlider));
    VolDelta = gtk_spin_button_get_value_as_int(GTK_SPIN_BUTTON(g_ConfDialog.volumedeltaSpin));
    VolumeControlType = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfDialog.volumetypeCombo)) + 1;
    SwapChannels = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfDialog.swapchannelsCheck));
    SaveConfig();

    /* Hide dialog. */
    if(data)
        { gtk_widget_hide(g_ConfDialog.dialog); }
}

/* Link default volume slider and spinbox. GNOME UI guidelines suggest such an arrangement
 * if the there are more than 20 possibly values for the slider.
 */ 
static void callback_connect_volume( GtkWidget *widget, gpointer data)
{
    if(widget==g_ConfDialog.volumedefaultSlider)
        { gtk_spin_button_set_value(GTK_SPIN_BUTTON(g_ConfDialog.volumedefaultSpin), gtk_range_get_value(GTK_RANGE(g_ConfDialog.volumedefaultSlider))); }
    else if (widget==g_ConfDialog.volumedefaultSpin)
        { gtk_range_set_value(GTK_RANGE(g_ConfDialog.volumedefaultSlider), gtk_spin_button_get_value_as_int(GTK_SPIN_BUTTON(g_ConfDialog.volumedefaultSpin))); }
}

/*********************************************************************************************************
 * dialogs
 */
static char *itoa(int num)
{
    static char txt[16];
    sprintf(txt, "%d", num);
    return txt;
}

EXPORT void CALL DllConfig(HWND hParent)
{
    ReadConfig();

    if(g_ConfDialog.dialog)
        {
        gtk_window_set_focus(GTK_WINDOW(g_ConfDialog.dialog), g_ConfDialog.okButton);
        gtk_widget_show_all(g_ConfDialog.dialog);
        return;
        }

    GtkWidget *label;
    GtkWidget *table;
    char buffer[MAXTOOLTIP];

    g_ConfDialog.dialog = gtk_dialog_new();

    gtk_signal_connect_object(GTK_OBJECT(g_ConfDialog.dialog), "delete-event",                       GTK_SIGNAL_FUNC(gtk_widget_hide_on_delete), GTK_OBJECT(g_ConfDialog.dialog));
    gtk_window_set_title(GTK_WINDOW(g_ConfDialog.dialog), "JttL's SDL Audio Configuration");

    table = gtk_table_new(6, 4, FALSE);
    gtk_container_set_border_width(GTK_CONTAINER(GTK_DIALOG(g_ConfDialog.dialog)->action_area), 12);
    gtk_container_set_border_width( GTK_CONTAINER(table), 10);
    gtk_table_set_col_spacings(GTK_TABLE(table), 10);
    gtk_table_set_row_spacings(GTK_TABLE(table), 2);
    gtk_container_add(GTK_CONTAINER(GTK_DIALOG(g_ConfDialog.dialog)->vbox), table);

    label = gtk_label_new_with_mnemonic("Volume c_ontrol:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    g_ConfDialog.volumetypeCombo = gtk_combo_box_new_text();
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfDialog.volumetypeCombo), "Internal SDL");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfDialog.volumetypeCombo), "OSS mixer" );
    strncpy(buffer, "Internal SDL volume control only affects the volume of Mupen64Plus.  OSS mixer volume control directly controls the OSS mixer, adjusting the master volume for the system.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.volumetypeCombo, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.volumetypeCombo);
    gtk_widget_set_size_request(g_ConfDialog.volumetypeCombo, 120, -1);
    if (VolumeControlType < 1 || VolumeControlType > 2) 
        VolumeControlType = 2;
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfDialog.volumetypeCombo), VolumeControlType - 1);
    gtk_table_attach(GTK_TABLE(table), label, 0, 1, 0, 1, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.volumetypeCombo, 1, 2, 0, 1, GTK_FILL | GTK_EXPAND, GTK_EXPAND, 0, 0);

    label = gtk_label_new_with_mnemonic("Cha_nge volume by:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    g_ConfDialog.volumedeltaSpin = gtk_spin_button_new_with_range(0, 100, 1);
    gtk_spin_button_set_digits(GTK_SPIN_BUTTON(g_ConfDialog.volumedeltaSpin), 0);
    gtk_spin_button_set_value(GTK_SPIN_BUTTON(g_ConfDialog.volumedeltaSpin), VolDelta);
    gtk_entry_set_width_chars(GTK_ENTRY(g_ConfDialog.volumedeltaSpin), 4);
    strncpy(buffer, "Sets the percentage change each time the volume is increased or decreased.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.volumedeltaSpin, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.volumedeltaSpin);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.volumedeltaSpin);
    gtk_table_attach(GTK_TABLE(table), label, 2, 3, 0, 1, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.volumedeltaSpin, 3, 4, 0, 1, GTK_FILL | GTK_EXPAND, GTK_EXPAND, 0, 0);

    label = gtk_label_new_with_mnemonic("_Resampling:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    g_ConfDialog.resampleCombo = gtk_combo_box_new_text();
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfDialog.resampleCombo), "Unfiltered");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfDialog.resampleCombo), "SINC");
    strncpy(buffer, "Resampling type. Unfiltered uses less CPU, but has lower  quality than SINC. For SINC resampling, libsamplerate must be installed.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.resampleCombo, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.resampleCombo, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.resampleCombo);
    gtk_widget_set_size_request(g_ConfDialog.resampleCombo, 120, -1);
    if (Resample < 1 || Resample > 2) 
        Resample = 1;
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfDialog.resampleCombo), Resample - 1);
    gtk_table_attach(GTK_TABLE(table), label, 0, 1, 1, 2, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.resampleCombo, 1, 2, 1, 2, GTK_FILL | GTK_EXPAND, GTK_EXPAND, 0, 0);

    label = gtk_label_new_with_mnemonic("Default _volume:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    gtk_table_attach(GTK_TABLE(table), label, 0, 1, 2, 3, GTK_FILL, GTK_EXPAND, 0, 0);

    g_ConfDialog.volumedefaultSlider = gtk_hscale_new_with_range(0, 100, 1);
    gtk_scale_set_digits(GTK_SCALE(g_ConfDialog.volumedefaultSlider), 0);
    gtk_scale_set_draw_value(GTK_SCALE(g_ConfDialog.volumedefaultSlider), FALSE);
    gtk_range_set_value(GTK_RANGE(g_ConfDialog.volumedefaultSlider), VolPercent);
    strncpy(buffer, "Sets the default volume upon startup when using Internal SDL volume control.  If Internal SDL is not used, this value is unused and the default volume is the volume that the harware mixer is set to when Mupen64Plus loads.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.volumedefaultSlider, buffer);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.volumedefaultSlider, 1, 3, 2, 3, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_signal_connect_object( GTK_OBJECT(g_ConfDialog.volumedefaultSlider), "change-value", GTK_SIGNAL_FUNC(callback_connect_volume), (gpointer)g_ConfDialog.volumedefaultSlider);

    g_ConfDialog.volumedefaultSpin = gtk_spin_button_new_with_range(0, 100, 1);
    gtk_spin_button_set_digits(GTK_SPIN_BUTTON(g_ConfDialog.volumedefaultSpin), 0);
    gtk_spin_button_set_value(GTK_SPIN_BUTTON(g_ConfDialog.volumedefaultSpin), VolPercent);
    strncpy(buffer, "Sets the default volume upon startup when using Internal SDL volume control. If Internal SDL is not used, this value is unused and the default volume is the volume that the harware mixer is set to when Mupen64Plus loads.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.volumedefaultSpin, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.volumedefaultSpin);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.volumedefaultSpin , 3, 4, 2, 3, GTK_FILL, GTK_EXPAND, 0, 10);
    gtk_signal_connect_object( GTK_OBJECT(g_ConfDialog.volumedefaultSpin ), "value-changed", GTK_SIGNAL_FUNC(callback_connect_volume), (gpointer)g_ConfDialog.volumedefaultSpin );

    g_ConfDialog.swapchannelsCheck = gtk_check_button_new_with_mnemonic("S_wap L/R channels");
    gtk_widget_set_tooltip_text(g_ConfDialog.swapchannelsCheck, "Swaps left and right channels.");
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfDialog.swapchannelsCheck), SwapChannels);
    GtkWidget *swapHBox = gtk_hbox_new(0, 0);
    gtk_box_pack_start(GTK_BOX(swapHBox), g_ConfDialog.swapchannelsCheck, FALSE, FALSE, 0);
    gtk_table_attach(GTK_TABLE(table), swapHBox, 1, 2, 3, 4, GTK_FILL, GTK_EXPAND, 0, 0);

    label = gtk_label_new_with_mnemonic("Default _frequency:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    g_ConfDialog.defaultfrequencyEntry = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(g_ConfDialog.defaultfrequencyEntry), itoa(GameFreq));
    gtk_entry_set_width_chars(GTK_ENTRY(g_ConfDialog.defaultfrequencyEntry), 8);
    strncpy(buffer, "Sets the default frequency in Hz, if rom doesn't want to change it. Probably the only games that needs this are the Gamecube Zelda ports.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.defaultfrequencyEntry, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.defaultfrequencyEntry);
    gtk_table_attach(GTK_TABLE(table), label, 2, 3, 3, 4, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.defaultfrequencyEntry, 3, 4, 3, 4, GTK_FILL | GTK_EXPAND, GTK_EXPAND, 0, 0);

    label = gtk_label_new_with_mnemonic("_Primary buffer:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    g_ConfDialog.primarybufferEntry = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(g_ConfDialog.primarybufferEntry), itoa(PrimaryBufferSize));
    gtk_entry_set_width_chars(GTK_ENTRY(g_ConfDialog.primarybufferEntry), 8);
    strncpy(buffer, "Size of primary buffer in bytes. This is the buffer where audio is loaded after it's extracted from the N64's memory.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.primarybufferEntry, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.primarybufferEntry);
    gtk_table_attach(GTK_TABLE(table), label, 0, 1, 4, 5, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.primarybufferEntry, 1, 2, 4, 5, GTK_FILL | GTK_EXPAND, GTK_EXPAND, 0, 0);

    label = gtk_label_new_with_mnemonic("Buffer _low level:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    g_ConfDialog.lowbufferEntry = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(g_ConfDialog.lowbufferEntry), itoa(LowBufferLoadLevel));
    gtk_entry_set_width_chars(GTK_ENTRY(g_ConfDialog.lowbufferEntry), 8);
    strncpy(buffer, "Size of low buffer in bytes. If buffer load goes under this level, then the game will speed up to fill the buffer. You probably want to adjust this value if you get dropouts.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.lowbufferEntry, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.lowbufferEntry);
    gtk_table_attach(GTK_TABLE(table), label, 2, 3, 4, 5, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.lowbufferEntry, 3, 4, 4, 5, GTK_FILL | GTK_EXPAND, GTK_EXPAND, 0, 0);

    label = gtk_label_new_with_mnemonic("_Secondary buffer:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    g_ConfDialog.secondarybufferEntry = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(g_ConfDialog.secondarybufferEntry), itoa(SecondaryBufferSize));
    gtk_entry_set_width_chars(GTK_ENTRY(g_ConfDialog.secondarybufferEntry), 8);
    strncpy(buffer, "This is SDL's hardware buffer size. This is the number of samples, so the actual buffer size in bytes is four times this number.", MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.secondarybufferEntry, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.secondarybufferEntry);
    gtk_table_attach(GTK_TABLE(table), label, 0, 1, 5, 6, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.secondarybufferEntry, 1, 2, 5, 6, GTK_FILL | GTK_EXPAND, GTK_EXPAND, 0, 0);

    label = gtk_label_new_with_mnemonic("Buffer _high level:");
    gtk_misc_set_alignment(GTK_MISC(label), 1, 0.5);
    g_ConfDialog.highbufferEntry = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(g_ConfDialog.highbufferEntry), itoa(HighBufferLoadLevel));
    gtk_entry_set_width_chars(GTK_ENTRY(g_ConfDialog.highbufferEntry), 8);
    strncpy(buffer, "Size of high buffer in bytes. If buffer load exceeds this level, then some extra slowdown is added to prevent buffer overflow.",  MAXTOOLTIP);
    buffer[MAXTOOLTIP-1] = '\0';
    gtk_widget_set_tooltip_text(label, buffer);
    gtk_widget_set_tooltip_text(g_ConfDialog.highbufferEntry, buffer);
    gtk_label_set_mnemonic_widget(GTK_LABEL(label), g_ConfDialog.highbufferEntry);
    gtk_table_attach(GTK_TABLE(table), label, 2, 3, 5, 6, GTK_FILL, GTK_EXPAND, 0, 0);
    gtk_table_attach(GTK_TABLE(table), g_ConfDialog.highbufferEntry, 3, 4, 5, 6, GTK_FILL | GTK_EXPAND, GTK_EXPAND, 0, 0);

    /* Apply / Ok / Cancel buttons. */
    GtkWidget *button;

    button = gtk_button_new_from_stock(GTK_STOCK_APPLY);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(g_ConfDialog.dialog)->action_area), button, TRUE, TRUE, 0);
    gtk_signal_connect(GTK_OBJECT(button), "clicked",
                       GTK_SIGNAL_FUNC(callback_apply_changes), (gpointer)FALSE);

    button = gtk_button_new_from_stock(GTK_STOCK_CANCEL);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(g_ConfDialog.dialog)->action_area), button, TRUE, TRUE, 0);
    gtk_signal_connect_object(GTK_OBJECT(button), "clicked",
                       GTK_SIGNAL_FUNC(gtk_widget_hide_on_delete), GTK_OBJECT(g_ConfDialog.dialog));

    g_ConfDialog.okButton = gtk_button_new_from_stock(GTK_STOCK_OK);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(g_ConfDialog.dialog)->action_area), g_ConfDialog.okButton, TRUE, TRUE, 0);
    gtk_signal_connect(GTK_OBJECT(g_ConfDialog.okButton), "clicked",
                       GTK_SIGNAL_FUNC(callback_apply_changes), (gpointer)TRUE);

    gtk_window_set_focus(GTK_WINDOW(g_ConfDialog.dialog), g_ConfDialog.okButton);
    gtk_widget_show_all(g_ConfDialog.dialog);
}

EXPORT void CALL DllAbout(HWND hParent)
{
    char buffer[256];
    GtkWidget *dialog;
    GtkWidget *label;
    GtkWidget *button;
    GtkWidget *VBox;

    dialog = gtk_dialog_new();
    gtk_window_set_title(GTK_WINDOW(dialog), "About JttL's SDL Audio");

    sprintf(buffer,"Mupen64 SDL Audio Plugin %s.\nOriginal code by JttL.\nGtk GUI by wahrhaft.\nFixes and features by Richard42, DarkJeztr, Tillin9, and others.", PLUGIN_VERSION);
    label = gtk_label_new(buffer);

    button = gtk_button_new_from_stock(GTK_STOCK_OK);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(dialog)->action_area), button, TRUE, TRUE, 0);
    gtk_signal_connect_object(GTK_OBJECT(button), "clicked", GTK_SIGNAL_FUNC(gtk_widget_destroy), GTK_OBJECT(dialog));

    VBox = gtk_hbox_new(0, 0); 
    gtk_container_set_border_width(GTK_CONTAINER(VBox), 10);
    gtk_container_add(GTK_CONTAINER(VBox), label);
    gtk_container_add(GTK_CONTAINER(GTK_DIALOG(dialog)->vbox), VBox);

    gtk_window_set_modal(GTK_WINDOW(dialog), TRUE);
    gtk_widget_show_all(dialog);
}

void display_test(const char *Message)
{
    GtkWidget *dialog;
    GtkWidget *label;
    GtkWidget *button;
    GtkWidget *VBox;

    dialog = gtk_dialog_new();
    gtk_window_set_title(GTK_WINDOW(dialog), "JttL's SDL Audio Test");

    label = gtk_label_new(Message);

    button = gtk_button_new_from_stock(GTK_STOCK_OK);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(dialog)->action_area), button, TRUE, TRUE, 0);
    gtk_signal_connect_object(GTK_OBJECT(button), "clicked", GTK_SIGNAL_FUNC(gtk_widget_destroy), GTK_OBJECT(dialog));

    VBox = gtk_hbox_new(0, 0); 
    gtk_container_set_border_width(GTK_CONTAINER(VBox), 10);
    gtk_container_add(GTK_CONTAINER(VBox), label);
    gtk_container_add(GTK_CONTAINER(GTK_DIALOG(dialog)->vbox), VBox);
    gtk_window_set_modal(GTK_WINDOW(dialog), TRUE);
    gtk_widget_show_all(dialog);
}

