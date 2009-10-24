/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - main_gtk2.c                                             *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2008 Tillin9                                            *
 *   Copyright (C) 2003 Rice 1964                                          *
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
#include <string.h>

#include <gtk/gtk.h>

#include "../messagebox.h"
#include "../../main/version.h"

#include "../winlnxdefs.h"
#include "../Graphics_1.3.h"
#include "../Config.h" 
#include "../Video.h"

#include "main_gtk2.h"
#include "icontheme.h"

/* Gobals. */
static ConfigDialog g_ConfigDialog;
extern void InitExternalTextures(void); /* (Missing from core headers...) */
extern WindowSettingStruct windowSetting;
extern RomOptions defaultRomOptions;

static char l_IconDir[PATH_MAX] = {0};

SettingInfo colorQualitySettings[] =
{
{"16-bit", 0},
{"32-bit (default)", 1},
};

const int resolutions[][2] =
{
{320, 240},
{400, 300},
{480, 360},
{512, 384},
{640, 480},
{800, 600},
{1024, 768},
{1152, 864},
{1280, 960},
{1400, 1050},
{1600, 1200},
{1920, 1440},
{2048, 1536},
};
const int numberOfResolutions = sizeof(resolutions)/sizeof(int)/2;

const char* resolutionsS[] =
{
"320 x 240",
"400 x 300",
"480 x 360",
"512 x 384",
"640 x 480",
"800 x 600",
"1024 x 768",
"1152 x 864",
"1280 x 960",
"1400 x 1050",
"1600 x 1200",
"1920 x 1440",
"2048 x 1536"
};

EXPORT void CALL SetInstallDir(char* installDir)
{
    snprintf(l_IconDir, sizeof(l_IconDir), "%s/icons", installDir);
    l_IconDir[sizeof(l_IconDir)-1] = '\0';
}

/* If theme changes, update application with images from new theme, or fallbacks. */
static void callback_theme_changed(GtkWidget* widget, gpointer data)
{
    check_icon_theme();
}

void gui_init(void)
{
    GtkIconTheme* icontheme = gtk_icon_theme_get_default();
    g_signal_connect(icontheme, "changed", G_CALLBACK(callback_theme_changed), NULL);
}

char* get_iconpath(char* iconfile)
{
    static char path[PATH_MAX];
    strncpy(path, l_IconDir, sizeof(path));
    strcat(path, iconfile);
    return path;
}

/* Control sensitivity of enhancementControlCombo. */
static void callback_texture_enhancement(GtkWidget* widget, gpointer data)
{
    gint setting = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.textureEnhancementCombo));

    gtk_widget_set_sensitive(g_ConfigDialog.enhancementControlCombo, !(setting==TEXTURE_NO_ENHANCEMENT||setting>= TEXTURE_SHARPEN_ENHANCEMENT));
}

/* Control sensitivity of doubleBufferSmallCheck. */
static void callback_native_resolution(GtkWidget *widget, gpointer data)
{
    defaultRomOptions.bInN64Resolution = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.nativeResolutionCheck));

    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.doubleBufferSmallCheck), !defaultRomOptions.bInN64Resolution);
    gtk_widget_set_sensitive(g_ConfigDialog.doubleBufferSmallCheck, !defaultRomOptions.bInN64Resolution);
}

static void callback_apply_changes(GtkWidget *widget, gpointer data)
{
    int i = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.fullScreenCombo));
    windowSetting.uFullScreenDisplayWidth = resolutions[i][0];
    windowSetting.uFullScreenDisplayHeight = resolutions[i][1];
    windowSetting.uWindowDisplayWidth = windowSetting.uFullScreenDisplayWidth;
    windowSetting.uWindowDisplayHeight = windowSetting.uFullScreenDisplayHeight;
    windowSetting.uDisplayWidth = windowSetting.uWindowDisplayWidth;
    windowSetting.uDisplayHeight = windowSetting.uWindowDisplayHeight;
    options.colorQuality = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.colorBufferDepthCombo));
    options.bEnableSSE = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.enableSSECheck));
    status.isSSEEnabled = status.isSSESupported && options.bEnableSSE;
    options.bSkipFrame = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.skipFrameCheck));
    options.bWinFrameMode = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.wireframeCheck));
    options.bEnableFog = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.enableFogCheck));
    options.bShowFPS = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.showFPSCheck));

    options.OpenglRenderSetting = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.combinerTypeCombo));
    options.OpenglDepthBufferSetting = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.depthBufferSettingCombo));

    options.textureQuality = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.textureQualityCombo));
    options.forceTextureFilter = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.textureFilterCombo));
    options.textureEnhancement = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.textureEnhancementCombo));
    options.textureEnhancementControl = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.enhancementControlCombo));
    options.bFullTMEM = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.fullTMEMCheck));
    options.bTexRectOnly = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.onlyTexRectCheck));
    options.bSmallTextureOnly = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.onlySmallTexturesCheck));
    BOOL bLoadHiResTextures = options.bLoadHiResTextures;
    options.bLoadHiResTextures = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.loadHiResCheck));
    BOOL bDumpTexturesToFiles = options.bDumpTexturesToFiles;
    options.bDumpTexturesToFiles = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.dumpTexturesCheck));
    if(status.bGameIsRunning && ((bLoadHiResTextures != options.bLoadHiResTextures) || (bDumpTexturesToFiles != options.bDumpTexturesToFiles)))
        {
        gdk_threads_leave();
        InitExternalTextures();
        gdk_threads_enter();
        }
    defaultRomOptions.bNormalBlender = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.normalBlenderCheck));
    defaultRomOptions.bNormalCombiner = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.normalCombinerCheck));
    defaultRomOptions.bAccurateTextureMapping = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.accurateMappingCheck));
    defaultRomOptions.bFastTexCRC = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.fastTextureCheck));
    defaultRomOptions.bInN64Resolution = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.nativeResolutionCheck));
    defaultRomOptions.bSaveVRAM = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.saveVRAMCheck));
    defaultRomOptions.bDoubleSizeForSmallTxtrBuf = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.doubleBufferSmallCheck));
    defaultRomOptions.N64RenderToTextureEmuType = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.renderToTextureCombo));

    g_curRomInfo.dwScreenUpdateSetting = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.tmemEmulationCombo));
    g_curRomInfo.dwRenderToTextureOption = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.renderToTextureGameCombo));
    g_curRomInfo.dwNormalBlender = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.normalBlenderCombo));
    g_curRomInfo.dwNormalCombiner = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.normalCombinerCombo));
    g_curRomInfo.dwAccurateTextureMapping = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.accurateMappingCombo));
    g_curRomInfo.dwFastTextureCRC = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.fastTextureCombo));
    g_curRomInfo.bForceScreenClear = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.forceBufferClearCheck));
    g_curRomInfo.bDisableBlender = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.disableBlenderCheck));
    g_curRomInfo.bEmulateClear = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.emulateClearCheck));
    g_curRomInfo.bForceDepthBuffer = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.forceDepthCompareCheck));

    g_curRomInfo.bDisableObjBG = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.disableBigTexturesCheck));
    g_curRomInfo.bUseSmallerTexture = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.useSmallTexturesCheck));
    g_curRomInfo.bDisableCulling = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.disableCullingCheck));
    g_curRomInfo.bTextureScaleHack = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.textureScaleHackCheck));
    g_curRomInfo.bTxtSizeMethod2 = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.alternativeSizeCalcCheck));
    g_curRomInfo.bFastLoadTile = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.fasterLoadingTilesCheck));
    g_curRomInfo.bEnableTxtLOD = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.enableTextureLODCheck));
    g_curRomInfo.bTexture1Hack = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.texture1HackCheck));
    g_curRomInfo.bPrimaryDepthHack = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.primaryDepthHackCheck));
    g_curRomInfo.bIncTexRectEdge = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.increaseTexRectEdgeCheck));
    g_curRomInfo.bZHack = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.nearPlaneZHackCheck));

   char* s = (char*)gtk_entry_get_text(GTK_ENTRY(g_ConfigDialog.n64ScreenWidthHeightEntry1));
   if( atol(s) > 100 )
     g_curRomInfo.VIWidth = atol(s);
   else
     g_curRomInfo.VIWidth = 0;

   s = (char*)gtk_entry_get_text(GTK_ENTRY(g_ConfigDialog.n64ScreenWidthHeightEntry2));
   if( atol(s) > 100 )
     g_curRomInfo.VIHeight = atol(s);
   else
     g_curRomInfo.VIHeight = 0;

    g_curRomInfo.UseCIWidthAndRatio = gtk_combo_box_get_active(GTK_COMBO_BOX(g_ConfigDialog.useCICombo));

    WriteConfiguration();
    /*
    Don't overwrite .ini by default!
    GenerateCurrentRomOptions();
    Ini_StoreRomOptions(&g_curRomInfo);
    */

    if(data)
        gtk_widget_hide(g_ConfigDialog.dialog);
}

static void create_dialog(void)
{
    g_ConfigDialog.dialog = gtk_dialog_new();
    gtk_window_set_title(GTK_WINDOW(g_ConfigDialog.dialog), "Rice Video Configuration");

    GtkWidget *label, *table;
    int i;

    /* General Options Tab. */
    GtkWidget* notebook = gtk_notebook_new();
    gtk_container_set_border_width(GTK_CONTAINER(notebook), 10);
    gtk_notebook_set_tab_pos(GTK_NOTEBOOK(notebook), GTK_POS_TOP);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(g_ConfigDialog.dialog)->vbox), notebook, TRUE, TRUE, 0);

    table = gtk_table_new(2, 5, FALSE);
    gtk_container_set_border_width(GTK_CONTAINER(table), 10);
    gtk_table_set_col_spacings(GTK_TABLE(table), 10);
    gtk_table_set_row_spacings(GTK_TABLE(table), 2);
    gtk_notebook_append_page(GTK_NOTEBOOK(notebook), table, gtk_label_new("General Options"));

    /*
    Why don't we use this?
    label = gtk_label_new("Window Mode Resolution:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);

    GtkWidget *windowModeCombo;
    windowModeCombo = gtk_combo_new();
    */

    label = gtk_label_new("Full Screen Resolution:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 0, 1);

    g_ConfigDialog.fullScreenCombo = gtk_combo_box_new_text();
    for (i = 0; i < numberOfResolutions; ++i)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.fullScreenCombo), resolutionsS[i]);
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.fullScreenCombo, 1, 2, 0, 1);

    label = gtk_label_new("Color Buffer Depth:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 1, 2);

    g_ConfigDialog.colorBufferDepthCombo = gtk_combo_box_new_text();
    for (i = 0; (size_t)i < sizeof(colorQualitySettings)/sizeof(SettingInfo); ++i)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.colorBufferDepthCombo), colorQualitySettings[i].description);
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.colorBufferDepthCombo, 1, 2, 1, 2);

    g_ConfigDialog.enableSSECheck = gtk_check_button_new_with_label("Enable SSE");
    gtk_widget_set_tooltip_text(g_ConfigDialog.enableSSECheck, "On x86 and x86_64 CPUs, SSE (Intel Streaming SMID Extension) can speed up 3D transformation, vertex, and matrix processing. If your processor cannot support SSE, the plugin should autodetect this. You generally want this enabled unless debugging.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.enableSSECheck, 0, 1, 2, 3);

    g_ConfigDialog.skipFrameCheck = gtk_check_button_new_with_label("Skip frame");
    gtk_widget_set_tooltip_text(g_ConfigDialog.skipFrameCheck, "If enabled, only every other frame will be rendered to the screen. This is only useful to speedup GPU limited games and may cause flickering.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.skipFrameCheck, 1, 2, 2, 3);

    g_ConfigDialog.wireframeCheck = gtk_check_button_new_with_label("Wireframe Mode");
    gtk_widget_set_tooltip_text(g_ConfigDialog.wireframeCheck, "If enabled, graphics will be drawn in wireframe mode instead of solid and texture mode.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.wireframeCheck, 0, 1, 3, 4);

    g_ConfigDialog.enableFogCheck = gtk_check_button_new_with_label("Enable Fog");
    gtk_widget_set_tooltip_text(g_ConfigDialog.enableFogCheck, "Enabling fog effects can improve graphics in some games, but may introduce minor artifacts on most platforms. Linux users with ATI cards will likely experience significant artifacts.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.enableFogCheck, 1, 2, 3, 4);

    g_ConfigDialog.showFPSCheck = gtk_check_button_new_with_label("Show FPS");
    gtk_widget_set_tooltip_text(g_ConfigDialog.showFPSCheck, "If enabled, current FPS (frame per second) will be displayed in the titlebar.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.showFPSCheck, 0, 1, 4, 5);

    /* OpenGL Options Tab. */
    table = gtk_table_new(2, 5, FALSE);
    gtk_container_set_border_width(GTK_CONTAINER(table), 10);
    gtk_table_set_col_spacings(GTK_TABLE(table), 10);
    gtk_table_set_row_spacings(GTK_TABLE(table), 2);
    gtk_notebook_append_page(GTK_NOTEBOOK(notebook), table, gtk_label_new("OpenGL Options"));

    label = gtk_label_new("Combiner Type:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 0, 1);

    g_ConfigDialog.combinerTypeCombo = gtk_combo_box_new_text();
    for (i = 0; i < 7; ++i)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.combinerTypeCombo), OpenGLRenderSettings[i].name);
    gtk_widget_set_tooltip_text(g_ConfigDialog.combinerTypeCombo, "OpenGL combiner type. The default [To Fit Your Video Card] should work fine for you, or you can manually select:\n\n- OpenGL 1.1, most video cards support this\n- OpenGL 1.2/1.3, for OGL version without Texture Crossbar support\n- OpenGL 1.4, for OGL version with Texture Crossbar support\n- Nvidia TNT, is good for all Nvidia video cards from TNT\n- Nvidia Register Combiner, is for all Nvidia video cards from Geforce 256. This combiner is better than the Nvidia TNT one\n");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.combinerTypeCombo, 1, 2, 0, 1);

    label = gtk_label_new("Depth Buffer:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 1, 2);

    g_ConfigDialog.depthBufferSettingCombo = gtk_combo_box_new_text();
    for (i = 0; i < 2; ++i)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.depthBufferSettingCombo), openGLDepthBufferSettings[i].description);
     gtk_widget_set_tooltip_text(g_ConfigDialog.depthBufferSettingCombo, "Set OpenGL color depth to 16-bit or 32-bit. 32-bit is higher quality, but some graphics cards may perform better in 16-bit mode.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.depthBufferSettingCombo, 1, 2, 1, 2);

    /* Texture Filters Tab. */
    table = gtk_table_new(2, 7, FALSE);
    gtk_container_set_border_width(GTK_CONTAINER(table), 10);
    gtk_table_set_col_spacings(GTK_TABLE(table), 10);
    gtk_table_set_row_spacings(GTK_TABLE(table), 2);
    gtk_notebook_append_page(GTK_NOTEBOOK(notebook), table, gtk_label_new("Texture Filters"));

    label = gtk_label_new("Texture Quality:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 0, 1);

    g_ConfigDialog.textureQualityCombo = gtk_combo_box_new_text();
    for (i = 0; i < 3; ++i)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.textureQualityCombo), TextureQualitySettings[i].description);
    gtk_widget_set_tooltip_text(g_ConfigDialog.textureQualityCombo, "Default - Use the same quality as color buffer quality\n32-bit - Always use 32 bit textures.\n16-bit - Always use 16 bit textures.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.textureQualityCombo, 1, 2, 0, 1);

    label = gtk_label_new("Texture Filter:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 1, 2);

    g_ConfigDialog.textureFilterCombo = gtk_combo_box_new_text();

    SettingInfo ForceTextureFilterSettings[] =
       {
       {"N64 Default Texture Filter",  FORCE_DEFAULT_FILTER},
       {"Force Nearest Filter (faster, low quality)", FORCE_POINT_FILTER},
       {"Force Linear Filter (slower, better quality)", FORCE_LINEAR_FILTER},
       /* {"Force Bilinear Filter slower, best quality", FORCE_BILINEAR_FILTER}, */
       };

    for (i = 0; i < 3; ++i)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.textureFilterCombo), ForceTextureFilterSettings[i].description);

    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.textureFilterCombo, 1, 2, 1, 2);

    label = gtk_label_new("Texture Enhancement:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 3, 4);

    SettingInfo TextureEnhancementSettings[] =
        {
        {"N64 original texture (No enhancement)", TEXTURE_NO_ENHANCEMENT},
        {"2x (Double the texture size)", TEXTURE_2X_ENHANCEMENT},
        {"2xSaI", TEXTURE_2XSAI_ENHANCEMENT},
        {"hq2x", TEXTURE_HQ2X_ENHANCEMENT},
        {"lq2x", TEXTURE_LQ2X_ENHANCEMENT},
        {"hq4x", TEXTURE_HQ4X_ENHANCEMENT},
        {"Sharpen", TEXTURE_SHARPEN_ENHANCEMENT},
        {"Sharpen More", TEXTURE_SHARPEN_MORE_ENHANCEMENT},
        };

    g_ConfigDialog.textureEnhancementCombo = gtk_combo_box_new_text();
    for (i=0; (size_t)i<sizeof(TextureEnhancementSettings)/sizeof(SettingInfo); i++)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.textureEnhancementCombo), TextureEnhancementSettings[i].description);

    gtk_widget_set_tooltip_text(g_ConfigDialog.textureEnhancementCombo, "Use original N64 textures, or enhance texture via selected filted. Filters may enhanced game visuals at the cost of some speed.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.textureEnhancementCombo, 1, 2, 3, 4);

    label = gtk_label_new("Enhancement Control:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 4, 5);

    g_ConfigDialog.enhancementControlCombo = gtk_combo_box_new_text();
    for (i = 0; i < 5; ++i)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.enhancementControlCombo), TextureEnhancementControlSettings[i].description);
    gtk_widget_set_tooltip_text(g_ConfigDialog.enhancementControlCombo, "Control the texture enhancement filters.\n\n- Normal,                    no enhancement filter.\n- Smooth,                    apply a smoothing filter.\n- Less Smooth,            apply a less intense smoothing filter.\n- 2xSai Smooth,          apply smoothing filter for 2xSai artifacts.\n- 2xSai Less Smooth,  apply a less intense smoothing filter for 2xSai.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.enhancementControlCombo, 1, 2, 4, 5);

    g_ConfigDialog.onlyTexRectCheck = gtk_check_button_new_with_label("For TexRect Only");
    gtk_widget_set_tooltip_text(g_ConfigDialog.onlyTexRectCheck, "If enabled, enhancements will only be applied to textures using TexRect ucode.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.onlyTexRectCheck, 0, 1, 5, 6);

    g_ConfigDialog.loadHiResCheck = gtk_check_button_new_with_label("Load Hi-Res Textures (if available)");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.loadHiResCheck, 1, 2, 5, 6);

    g_ConfigDialog.onlySmallTexturesCheck = gtk_check_button_new_with_label("For Small Textures Only");
    gtk_widget_set_tooltip_text(g_ConfigDialog.onlySmallTexturesCheck, "If enabled, enhancements will only be applied to small textures, i.e. textures with native N64 resolution height and width <= 128 pixels.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.onlySmallTexturesCheck, 0, 1, 6, 7);

    g_ConfigDialog.dumpTexturesCheck = gtk_check_button_new_with_label("Dump textures to files");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.dumpTexturesCheck, 1, 2, 6, 7);

    /* Default Game Options Tab. */
    table = gtk_table_new(2, 6, FALSE);
    gtk_container_set_border_width(GTK_CONTAINER(table), 10);
    gtk_table_set_col_spacings(GTK_TABLE(table), 10);
    gtk_table_set_row_spacings(GTK_TABLE(table), 2);
    gtk_notebook_append_page(GTK_NOTEBOOK(notebook), table, gtk_label_new("Default Game Options"));

    label = gtk_label_new("When game has no .ini entry for a specific property, the plugin uses these defaults.");
    gtk_label_set_line_wrap(GTK_LABEL(label), TRUE);
    gtk_table_attach(GTK_TABLE(table), label, 0, 2, 0, 1, GTK_EXPAND, GTK_FILL, 0, 0);

    label = gtk_label_new("Rendering to Texture Emulation:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 1, 2);

    g_ConfigDialog.renderToTextureCombo = gtk_combo_box_new_text();

    const char *renderToTextureSettings[] =
        {
        "None (default)",
        "Hide Render-to-texture Effects",
        "Basic Render-to-texture",
        "Basic & Write Back",
        "Write Back & Reload",
        };

    for (i = 0; (size_t)i<sizeof(renderToTextureSettings)/sizeof(char*); ++i)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.renderToTextureCombo), renderToTextureSettings[i]);
    gtk_widget_set_tooltip_text(g_ConfigDialog.renderToTextureCombo, "- None (default), don't do any Render-to-texture emulation.\n- Hide Render-to-texture effects,  ignore Render-to-texture drawing, at least such drawing won't draw to the current rendering buffer.\n- Render-to-texture,    support self-render-texture\n- Basic Render-to-texture, will check texture loading address to see if the address is within the frame buffer\n- Basic & Write back, will write the Render-to-render_texture back when rendering is finished\n- Write back & Reload, will load Render-to-texture data from RDRAM before the buffer is rendered.\n");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.renderToTextureCombo, 1, 2, 1, 2);


    g_ConfigDialog.normalBlenderCheck = gtk_check_button_new_with_label("Normal Blender");
    gtk_widget_set_tooltip_text(g_ConfigDialog.normalBlenderCheck, "This option may correct opaque/ transparency problems in certain games.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.normalBlenderCheck, 0, 1, 2, 3);

    g_ConfigDialog.accurateMappingCheck = gtk_check_button_new_with_label("Accurate Texture Mapping");
    gtk_widget_set_tooltip_text(g_ConfigDialog.accurateMappingCheck, "This option may reduce thin black lines in certain games.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.accurateMappingCheck, 1, 2, 2, 3);

    g_ConfigDialog.normalCombinerCheck = gtk_check_button_new_with_label("Normal Combiner");
    gtk_widget_set_tooltip_text(g_ConfigDialog.normalCombinerCheck, "Force use of normal color combiner. This option may correct opaque/ transparency problems in certain games.\n\nNormal color combiner is:\n- Texture * Shade, if both texture and shade are used.\n- Texture only,       if texture is used and shade is not used.\n- Shade only,         if texture is not used.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.normalCombinerCheck, 0, 1, 3, 4);

    g_ConfigDialog.fastTextureCheck = gtk_check_button_new_with_label("Faster Texture Loading");
    gtk_widget_set_tooltip_text(g_ConfigDialog.fastTextureCheck, "Use a faster algorithm to speed up texture loading and CRC computation.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.fastTextureCheck, 1, 2, 3, 4);

    g_ConfigDialog.fullTMEMCheck = gtk_check_button_new_with_label("Full TMEM Emulation");
    gtk_widget_set_tooltip_text(g_ConfigDialog.fullTMEMCheck, "Full TMEM (N64 Texture Memory) Emulation. If disabled, textures are loaded directly from N64 RDRAM causing emulation to be faster. If enabled, texture data will be loaded into a 4KB TMEM buffer, and game textures created from this data. While slower, this is more accurate and required by certain games.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.fullTMEMCheck, 0, 1, 4, 5);

    g_ConfigDialog.nativeResolutionCheck = gtk_check_button_new_with_label("In N64 Native Resolution");
    gtk_widget_set_tooltip_text(g_ConfigDialog.nativeResolutionCheck, "Emulated back buffer resolutions are usually much higher than the native N64 resolution. Back buffer textures can be saved and used in higher resolution to give the best speed and quality, but this needs large amounts of video RAM. Unless your video card has 32MB or less of memory, this should not be needed.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.nativeResolutionCheck, 1, 2, 4, 5);

    g_ConfigDialog.saveVRAMCheck = gtk_check_button_new_with_label("Try to save VRAM");
    gtk_widget_set_tooltip_text(g_ConfigDialog.saveVRAMCheck, "Automatically check if render-to-texture or saved back buffer texture has been overwritten by CPU thread. Unless your video card has very limited video RAM, this should not be needed.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.saveVRAMCheck, 0, 1, 5, 6);

    g_ConfigDialog.doubleBufferSmallCheck = gtk_check_button_new_with_label("Double Small Texture Buffer");
    gtk_widget_set_tooltip_text(g_ConfigDialog.doubleBufferSmallCheck, "Uses double buffer sizes for render to texture effects on small textures, i.e. textures with native N64 resolution height and width <= 128 pixels. This may increase visual quality in certain games at the cost of video RAM and speed.");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.doubleBufferSmallCheck, 1, 2, 5, 6);

    /* Basic Game Options Tab. */
    g_ConfigDialog.basicGameOptions = table = gtk_table_new(4, 7, FALSE);
    gtk_container_set_border_width(GTK_CONTAINER(table), 10);
    gtk_table_set_col_spacings(GTK_TABLE(table), 10);
    gtk_table_set_row_spacings(GTK_TABLE(table), 2);
    gtk_notebook_append_page(GTK_NOTEBOOK(notebook), table, gtk_label_new("Game Options"));

    label = gtk_label_new("Frame Update at:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 0, 1);

    const char *screenUpdateSettings[] =
    {
    "At VI origin update",
    "At VI origin change",
    "At CI change",
    "At the 1st CI change",
    "At the 1st drawing",
    "Before clear the screen",
    "At VI origin update after screen is drawn (default)",
    };

    g_ConfigDialog.frameUpdateAtCombo = gtk_combo_box_new_text();
    for (i = 0; i < 7; i++)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.frameUpdateAtCombo), screenUpdateSettings[i]);
    gtk_table_attach(GTK_TABLE(table), g_ConfigDialog.frameUpdateAtCombo, 1, 4, 0, 1, GTK_FILL, GTK_EXPAND, 0, 0);

    label = gtk_label_new("Rendering to Texture:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 1, 2);

    g_ConfigDialog.renderToTextureGameCombo = gtk_combo_box_new_text();
    for (i=0; (size_t)i<sizeof(renderToTextureSettings)/sizeof(char*); i++)
        gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.renderToTextureGameCombo), renderToTextureSettings[i]);
    gtk_table_attach(GTK_TABLE(table), g_ConfigDialog.renderToTextureGameCombo, 1, 4, 1, 2, GTK_FILL, GTK_EXPAND, 0, 0);

    label = gtk_label_new("Normal Blender:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 2, 3);

    g_ConfigDialog.normalBlenderCombo = gtk_combo_box_new_text();
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.normalBlenderCombo), "Use Default");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.normalBlenderCombo), "Disabled");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.normalBlenderCombo), "Enabled");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.normalBlenderCombo, 1, 2, 2, 3);

    label = gtk_label_new("Accurate Texture Mapping:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 2, 3, 2, 3);

    g_ConfigDialog.accurateMappingCombo = gtk_combo_box_new_text();
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.accurateMappingCombo), "Use Default");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.accurateMappingCombo), "Disabled");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.accurateMappingCombo), "Enabled");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.accurateMappingCombo, 3, 4, 2, 3);

    label = gtk_label_new("Normal Combiner:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 3, 4);

    g_ConfigDialog.normalCombinerCombo = gtk_combo_box_new_text();
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.normalCombinerCombo), "Use Default");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.normalCombinerCombo), "Disabled");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.normalCombinerCombo), "Enabled");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.normalCombinerCombo, 1, 2, 3, 4);

    label = gtk_label_new("Faster Texture Loading:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 2, 3, 3, 4);

    g_ConfigDialog.fastTextureCombo = gtk_combo_box_new_text();
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.fastTextureCombo), "Use Default");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.fastTextureCombo), "Disabled");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.fastTextureCombo), "Enabled");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.fastTextureCombo, 3, 4, 3, 4);

    g_ConfigDialog.forceBufferClearCheck = gtk_check_button_new_with_label("Force Buffer Clear");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.forceBufferClearCheck, 0, 2, 5, 6);

    g_ConfigDialog.emulateClearCheck = gtk_check_button_new_with_label("Emulate Clear");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.emulateClearCheck, 2, 4, 5, 6);

    g_ConfigDialog.disableBlenderCheck = gtk_check_button_new_with_label("Disable Blender");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.disableBlenderCheck, 0, 2, 6, 7);

    g_ConfigDialog.forceDepthCompareCheck = gtk_check_button_new_with_label("Force Depth Compare");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.forceDepthCompareCheck, 2, 4, 6, 7);

    /* Advanced Game Options Tab. */
    g_ConfigDialog.advancedGameOptions = table = gtk_table_new(3, 9, FALSE);
    gtk_container_set_border_width(GTK_CONTAINER(table), 10);
    gtk_table_set_col_spacings(GTK_TABLE(table), 10);
    gtk_table_set_row_spacings(GTK_TABLE(table), 2);
    gtk_notebook_append_page(GTK_NOTEBOOK(notebook), table, gtk_label_new("Advanced Game Options"));

    g_ConfigDialog.disableBigTexturesCheck = gtk_check_button_new_with_label("Disable Big Textures");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.disableBigTexturesCheck, 0, 1, 0, 1);

    g_ConfigDialog.useSmallTexturesCheck = gtk_check_button_new_with_label("Try to Use Small Textures");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.useSmallTexturesCheck, 1, 3, 0, 1);

    g_ConfigDialog.alternativeSizeCalcCheck = gtk_check_button_new_with_label("Alternative Texture Size Calculation");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.alternativeSizeCalcCheck, 0, 1, 1, 2);

    g_ConfigDialog.increaseTexRectEdgeCheck = gtk_check_button_new_with_label("Increase TexRect Edge");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.increaseTexRectEdgeCheck, 1, 3, 1, 2);

    g_ConfigDialog.fasterLoadingTilesCheck = gtk_check_button_new_with_label("Faster Loading Tiles");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.fasterLoadingTilesCheck, 0, 1, 2, 3);

    g_ConfigDialog.textureScaleHackCheck = gtk_check_button_new_with_label("Texture Scale Hack");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.textureScaleHackCheck, 1, 3, 2, 3);

    g_ConfigDialog.texture1HackCheck = gtk_check_button_new_with_label("Texture 1 Hack");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.texture1HackCheck, 0, 1, 3, 4);

    g_ConfigDialog.enableTextureLODCheck = gtk_check_button_new_with_label("Enable Texture LOD");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.enableTextureLODCheck, 1, 3, 3, 4);

    g_ConfigDialog.primaryDepthHackCheck = gtk_check_button_new_with_label("Primary Depth Hack");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.primaryDepthHackCheck, 0, 1, 4, 5);

    g_ConfigDialog.nearPlaneZHackCheck = gtk_check_button_new_with_label("Near Plane Z Hack");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.nearPlaneZHackCheck, 1, 3, 4, 5);

    g_ConfigDialog.disableCullingCheck = gtk_check_button_new_with_label("Disable Culling");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.disableCullingCheck, 0, 1, 5, 6);

    label = gtk_label_new("TMEM Emulation:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 6, 7);

    g_ConfigDialog.tmemEmulationCombo = gtk_combo_box_new_text();
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.tmemEmulationCombo), "Use Default");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.tmemEmulationCombo), "Disabled");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.tmemEmulationCombo), "Enabled");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.tmemEmulationCombo, 1, 3, 6, 7);

    label = gtk_label_new("Use CI Width and Ratio:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 7, 8);

    g_ConfigDialog.useCICombo = gtk_combo_box_new_text();
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.useCICombo), "No");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.useCICombo), "NTSC");
    gtk_combo_box_append_text(GTK_COMBO_BOX(g_ConfigDialog.useCICombo), "PAL");
    gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.useCICombo, 1, 3, 7, 8);

    label = gtk_label_new("N64 Screen Width/ Height:");
    gtk_misc_set_alignment(GTK_MISC(label), 0, 0.5);
    gtk_table_attach_defaults(GTK_TABLE(table), label, 0, 1, 8, 9);

   g_ConfigDialog.n64ScreenWidthHeightEntry1 = gtk_entry_new_with_max_length(5);
   gtk_entry_set_width_chars(GTK_ENTRY(g_ConfigDialog.n64ScreenWidthHeightEntry1), 5);
   gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.n64ScreenWidthHeightEntry1, 1, 2, 8, 9);

   g_ConfigDialog.n64ScreenWidthHeightEntry2 = gtk_entry_new_with_max_length(5);
   gtk_entry_set_width_chars(GTK_ENTRY(g_ConfigDialog.n64ScreenWidthHeightEntry2), 5);
   gtk_table_attach_defaults(GTK_TABLE(table), g_ConfigDialog.n64ScreenWidthHeightEntry2, 2, 3, 8, 9);

    /* Signal callbacks. */
    g_signal_connect(g_ConfigDialog.dialog, "delete-event", G_CALLBACK(gtk_widget_hide_on_delete), NULL);
    g_signal_connect(g_ConfigDialog.textureEnhancementCombo, "changed", G_CALLBACK(callback_texture_enhancement), NULL);
    g_signal_connect(g_ConfigDialog.nativeResolutionCheck, "clicked", G_CALLBACK(callback_native_resolution), NULL);

    /* Apply / Ok / Cancel buttons. */
    GtkWidget* button;

    button = gtk_button_new_from_stock(GTK_STOCK_APPLY);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(g_ConfigDialog.dialog)->action_area), button, TRUE, TRUE, 0);
    g_signal_connect(button, "clicked", G_CALLBACK(callback_apply_changes), (gpointer)FALSE);

    button = gtk_button_new_from_stock(GTK_STOCK_CANCEL);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(g_ConfigDialog.dialog)->action_area), button, TRUE, TRUE, 0);
    g_signal_connect_object(button, "clicked", G_CALLBACK(gtk_widget_hide_on_delete), g_ConfigDialog.dialog, G_CONNECT_SWAPPED);

    g_ConfigDialog.okButton = gtk_button_new_from_stock(GTK_STOCK_OK);
    gtk_box_pack_start(GTK_BOX(GTK_DIALOG(g_ConfigDialog.dialog)->action_area), g_ConfigDialog.okButton, TRUE, TRUE, 0);
    g_signal_connect(g_ConfigDialog.okButton, "clicked", G_CALLBACK(callback_apply_changes), (gpointer)TRUE);
}

static void show_config()
{
    int i;

    for (i = 0; i < numberOfResolutions; ++i)
        {
        if (windowSetting.uWindowDisplayWidth == resolutions[i][0])
            gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.fullScreenCombo), i); 
        }

    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.colorBufferDepthCombo), options.colorQuality);

    gtk_widget_set_sensitive(g_ConfigDialog.fullScreenCombo, !status.bGameIsRunning);
    gtk_widget_set_sensitive(g_ConfigDialog.colorBufferDepthCombo, !status.bGameIsRunning);

    if(status.isSSESupported)
        gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.enableSSECheck), options.bEnableSSE);
    else
        {
        gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.enableSSECheck), FALSE);
        gtk_widget_set_sensitive(g_ConfigDialog.enableSSECheck, FALSE);
        }

    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.skipFrameCheck), options.bSkipFrame);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.wireframeCheck), options.bWinFrameMode);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.enableFogCheck), options.bEnableFog);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.showFPSCheck), options.bShowFPS);

    //OpenGL
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.combinerTypeCombo), options.OpenglRenderSetting);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.depthBufferSettingCombo), options.OpenglDepthBufferSetting);
    gtk_widget_set_sensitive(g_ConfigDialog.combinerTypeCombo, !status.bGameIsRunning);
    gtk_widget_set_sensitive(g_ConfigDialog.depthBufferSettingCombo, !status.bGameIsRunning);

    options.bOGLVertexClipper = FALSE;

   // Texture Filters
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.textureQualityCombo), options.textureQuality);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.textureFilterCombo), options.forceTextureFilter);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.fullTMEMCheck), options.bFullTMEM);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.textureEnhancementCombo), options.textureEnhancement);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.enhancementControlCombo), options.textureEnhancementControl);
    gtk_widget_set_sensitive(g_ConfigDialog.enhancementControlCombo, !(options.textureEnhancement==TEXTURE_NO_ENHANCEMENT||options.textureEnhancement>=TEXTURE_SHARPEN_ENHANCEMENT));
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.onlyTexRectCheck), options.bTexRectOnly);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.onlySmallTexturesCheck), options.bSmallTextureOnly);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.loadHiResCheck), options.bLoadHiResTextures);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.dumpTexturesCheck), options.bDumpTexturesToFiles);

    // Game Default Options
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.renderToTextureCombo), defaultRomOptions.N64RenderToTextureEmuType);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.normalBlenderCheck), defaultRomOptions.bNormalBlender);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.accurateMappingCheck), defaultRomOptions.bAccurateTextureMapping);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.normalCombinerCheck), defaultRomOptions.bNormalCombiner);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.fastTextureCheck), defaultRomOptions.bFastTexCRC);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.nativeResolutionCheck), defaultRomOptions.bInN64Resolution);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.saveVRAMCheck), defaultRomOptions.bSaveVRAM);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.doubleBufferSmallCheck), defaultRomOptions.bDoubleSizeForSmallTxtrBuf);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.doubleBufferSmallCheck), !defaultRomOptions.bInN64Resolution);
    gtk_widget_set_sensitive(g_ConfigDialog.doubleBufferSmallCheck, !defaultRomOptions.bInN64Resolution);

    //Current Game Options
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.frameUpdateAtCombo), g_curRomInfo.dwFrameBufferOption);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.renderToTextureGameCombo), g_curRomInfo.dwRenderToTextureOption);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.normalBlenderCombo), g_curRomInfo.dwNormalBlender);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.accurateMappingCombo), g_curRomInfo.dwAccurateTextureMapping);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.normalCombinerCombo), g_curRomInfo.dwNormalCombiner);
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.fastTextureCombo), g_curRomInfo.dwFastTextureCRC);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.forceBufferClearCheck), g_curRomInfo.bForceScreenClear);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.disableBlenderCheck), g_curRomInfo.bDisableBlender);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.emulateClearCheck), g_curRomInfo.bEmulateClear);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.forceDepthCompareCheck), g_curRomInfo.bForceDepthBuffer);

    //Advanced and less useful options.
    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.tmemEmulationCombo), g_curRomInfo.dwScreenUpdateSetting);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.disableBigTexturesCheck), g_curRomInfo.bDisableObjBG);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.useSmallTexturesCheck), g_curRomInfo.bUseSmallerTexture);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.disableCullingCheck), g_curRomInfo.bDisableCulling);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.textureScaleHackCheck), g_curRomInfo.bTextureScaleHack);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.alternativeSizeCalcCheck), g_curRomInfo.bTxtSizeMethod2);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.fasterLoadingTilesCheck), g_curRomInfo.bFastLoadTile);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.enableTextureLODCheck), g_curRomInfo.bEnableTxtLOD);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.texture1HackCheck), g_curRomInfo.bTexture1Hack);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.primaryDepthHackCheck), g_curRomInfo.bPrimaryDepthHack);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.increaseTexRectEdgeCheck), g_curRomInfo.bIncTexRectEdge);
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(g_ConfigDialog.nearPlaneZHackCheck), g_curRomInfo.bZHack);

    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.tmemEmulationCombo), g_curRomInfo.dwFullTMEM);

char generalText[100];
if(g_curRomInfo.VIWidth > 0)
    {
    sprintf(generalText, "%d", g_curRomInfo.VIWidth);
    gtk_entry_set_text(GTK_ENTRY(g_ConfigDialog.n64ScreenWidthHeightEntry1), generalText);
    }

if(g_curRomInfo.VIHeight > 0)
   {
   sprintf(generalText, "%d", g_curRomInfo.VIHeight);
   gtk_entry_set_text(GTK_ENTRY(g_ConfigDialog.n64ScreenWidthHeightEntry2), generalText);
   }

    gtk_combo_box_set_active(GTK_COMBO_BOX(g_ConfigDialog.useCICombo), g_curRomInfo.UseCIWidthAndRatio); 

    gtk_widget_show_all(g_ConfigDialog.dialog);

    if(!status.bGameIsRunning)
        {
        gtk_widget_hide(g_ConfigDialog.basicGameOptions);
        gtk_widget_hide(g_ConfigDialog.advancedGameOptions);
        }
}

EXPORT void CALL DllConfig(HWND hParent)
{
    InitConfiguration();

    if (g_ConfigDialog.dialog == NULL) 
        create_dialog();

    show_config();
}

void gui_update(void)
{
    if(GTK_IS_WIDGET(g_ConfigDialog.dialog))
        {
        gdk_threads_enter();

        if(GTK_WIDGET_VISIBLE(g_ConfigDialog.dialog))
            show_config();

        gdk_threads_leave();
        }
}
