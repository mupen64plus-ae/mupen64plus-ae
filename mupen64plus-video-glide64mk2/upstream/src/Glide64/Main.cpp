/*
* Glide64 - Glide video plugin for Nintendo 64 emulators.
* Copyright (c) 2002  Dave2001
* Copyright (c) 2003-2009  Sergey 'Gonetz' Lipski
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

//****************************************************************
//
// Glide64 - Glide Plugin for Nintendo 64 emulators
// Project started on December 29th, 2001
//
// Authors:
// Dave2001, original author, founded the project in 2001, left it in 2002
// Gugaman, joined the project in 2002, left it in 2002
// Sergey 'Gonetz' Lipski, joined the project in 2002, main author since fall of 2002
// Hiroshi 'KoolSmoky' Morii, joined the project in 2007
//
//****************************************************************
//
// To modify Glide64:
// * Write your name and (optional)email, commented by your work, so I know who did it, and so that you can find which parts you modified when it comes time to send it to me.
// * Do NOT send me the whole project or file that you modified.  Take out your modified code sections, and tell me where to put them.  If people sent the whole thing, I would have many different versions, but no idea how to combine them all.
//
//****************************************************************

#include "Gfx_1.3.h"
#include "Ini.h"
#include "Config.h"
#include "Util.h"
#include "3dmath.h"
#include "Debugger.h"
#include "Combine.h"
#include "TexCache.h"
#include "CRC.h"
#include "FBtoScreen.h"
#include "DepthBufferRender.h"

#if defined(__GNUC__)
#include <sys/time.h>
#elif defined(__MSC__)
#include <time.h>
#define PATH_MAX MAX_PATH
#endif
#ifndef PATH_MAX
  #define PATH_MAX 4096
#endif
#include "osal_dynamiclib.h"
#ifdef TEXTURE_FILTER // Hiroshi Morii <koolsmoky@users.sourceforge.net>
#include <stdarg.h>
int  ghq_dmptex_toggle_key = 0;
#endif
#if defined(__MINGW32__)
#define swprintf _snwprintf
#define vswprintf _vsnwprintf
#endif

#define G64_VERSION "G64 Mk2"

#ifdef EXT_LOGGING
std::ofstream extlog;
#endif

#ifdef LOGGING
std::ofstream loga;
#endif

#ifdef RDP_LOGGING
int log_open = FALSE;
std::ofstream rdp_log;
#endif

#ifdef RDP_ERROR_LOG
int elog_open = FALSE;
std::ofstream rdp_err;
#endif

GFX_INFO gfx;

/* definitions of pointers to Core config functions */
ptr_ConfigOpenSection      ConfigOpenSection = NULL;
ptr_ConfigSetParameter     ConfigSetParameter = NULL;
ptr_ConfigGetParameter     ConfigGetParameter = NULL;
ptr_ConfigGetParameterHelp ConfigGetParameterHelp = NULL;
ptr_ConfigSetDefaultInt    ConfigSetDefaultInt = NULL;
ptr_ConfigSetDefaultFloat  ConfigSetDefaultFloat = NULL;
ptr_ConfigSetDefaultBool   ConfigSetDefaultBool = NULL;
ptr_ConfigSetDefaultString ConfigSetDefaultString = NULL;
ptr_ConfigGetParamInt      ConfigGetParamInt = NULL;
ptr_ConfigGetParamFloat    ConfigGetParamFloat = NULL;
ptr_ConfigGetParamBool     ConfigGetParamBool = NULL;
ptr_ConfigGetParamString   ConfigGetParamString = NULL;

ptr_ConfigGetSharedDataFilepath ConfigGetSharedDataFilepath = NULL;
ptr_ConfigGetUserConfigPath     ConfigGetUserConfigPath = NULL;
ptr_ConfigGetUserDataPath       ConfigGetUserDataPath = NULL;
ptr_ConfigGetUserCachePath      ConfigGetUserCachePath = NULL;

/* definitions of pointers to Core video extension functions */
ptr_VidExt_Init                  CoreVideo_Init = NULL;
ptr_VidExt_Quit                  CoreVideo_Quit = NULL;
ptr_VidExt_ListFullscreenModes   CoreVideo_ListFullscreenModes = NULL;
ptr_VidExt_SetVideoMode          CoreVideo_SetVideoMode = NULL;
ptr_VidExt_SetCaption            CoreVideo_SetCaption = NULL;
ptr_VidExt_ToggleFullScreen      CoreVideo_ToggleFullScreen = NULL;
ptr_VidExt_ResizeWindow          CoreVideo_ResizeWindow = NULL;
ptr_VidExt_GL_GetProcAddress     CoreVideo_GL_GetProcAddress = NULL;
ptr_VidExt_GL_SetAttribute       CoreVideo_GL_SetAttribute = NULL;
ptr_VidExt_GL_SwapBuffers        CoreVideo_GL_SwapBuffers = NULL;
int to_fullscreen = FALSE;
int fullscreen = FALSE;
int romopen = FALSE;
GrContext_t gfx_context = 0;
int debugging = FALSE;
int exception = FALSE;

int evoodoo = 0;
int ev_fullscreen = 0;

#ifdef ALTTAB_FIX
HHOOK hhkLowLevelKybd = NULL;
LRESULT CALLBACK LowLevelKeyboardProc(int nCode,
                                      WPARAM wParam, LPARAM lParam);
#endif

#ifdef PERFORMANCE
int64 perf_cur;
int64 perf_next;
#endif

#ifdef FPS
LARGE_INTEGER perf_freq;
LARGE_INTEGER fps_last;
LARGE_INTEGER fps_next;
float      fps = 0.0f;
wxUint32   fps_count = 0;

wxUint32   vi_count = 0;
float      vi = 0.0f;
#endif

/* custom macros made up by cxd4 for tracking the system type better */
#define OS_TV_TYPE_PAL 0
#define OS_TV_TYPE_NTSC 1
#define OS_TV_TYPE_MPAL 2
unsigned int region;

// ref rate
// 60=0x0, 70=0x1, 72=0x2, 75=0x3, 80=0x4, 90=0x5, 100=0x6, 85=0x7, 120=0x8, none=0xff

#ifdef USE_FRAMESKIPPER
#include "FrameSkipper.h"
FrameSkipper frameSkipper;
#endif

unsigned long BMASK = 0x7FFFFF;
// Reality display processor structure
RDP rdp;

SETTINGS settings = { FALSE, 640, 480, GR_RESOLUTION_640x480, 0 };

HOTKEY_INFO hotkey_info;

VOODOO voodoo = {0, 0, 0, 0,
                 0, 0, 0, 0,
                 0, 0, 0, 0
                };

GrTexInfo fontTex;
GrTexInfo cursorTex;
wxUint32   offset_font = 0;
wxUint32   offset_cursor = 0;
wxUint32   offset_textures = 0;
wxUint32   offset_texbuf1 = 0;

int    capture_screen = 0;
char    capture_path[256];

SDL_sem *mutexProcessDList = SDL_CreateSemaphore(1);

// SOME FUNCTION DEFINITIONS 

static void DrawFrameBuffer ();


void (*renderCallback)(int) = NULL;
static void (*l_DebugCallback)(void *, int, const char *) = NULL;
static void *l_DebugCallContext = NULL;

void _ChangeSize ()
{
  rdp.scale_1024 = settings.scr_res_x / 1024.0f;
  rdp.scale_768 = settings.scr_res_y / 768.0f;

//  float res_scl_x = (float)settings.res_x / 320.0f;
  float res_scl_y = (float)settings.res_y / 240.0f;

  wxUint32 scale_x = *gfx.VI_X_SCALE_REG & 0xFFF;
  if (!scale_x) return;
  wxUint32 scale_y = *gfx.VI_Y_SCALE_REG & 0xFFF;
  if (!scale_y) return;

  float fscale_x = (float)scale_x / 1024.0f;
  float fscale_y = (float)scale_y / 2048.0f;

  wxUint32 dwHStartReg = *gfx.VI_H_START_REG;
  wxUint32 dwVStartReg = *gfx.VI_V_START_REG;

  wxUint32 hstart = dwHStartReg >> 16;
  wxUint32 hend = dwHStartReg & 0xFFFF;

  // dunno... but sometimes this happens
  if (hend == hstart) hend = (int)(*gfx.VI_WIDTH_REG / fscale_x);

  wxUint32 vstart = dwVStartReg >> 16;
  wxUint32 vend = dwVStartReg & 0xFFFF;

  rdp.vi_width = (hend - hstart) * fscale_x;
  rdp.vi_height = (vend - vstart) * fscale_y * 1.0126582f;
  float aspect = (settings.adjust_aspect && (fscale_y > fscale_x) && (rdp.vi_width > rdp.vi_height)) ? fscale_x/fscale_y : 1.0f;

#ifdef LOGGING
  sprintf (out_buf, "hstart: %d, hend: %d, vstart: %d, vend: %d\n", hstart, hend, vstart, vend);
  LOG (out_buf);
  sprintf (out_buf, "size: %d x %d\n", (int)rdp.vi_width, (int)rdp.vi_height);
  LOG (out_buf);
#endif

  rdp.scale_x = (float)settings.res_x / rdp.vi_width;
  if (region != OS_TV_TYPE_NTSC && settings.pal230)
  {
    // odd... but pal games seem to want 230 as height...
    rdp.scale_y = res_scl_y * (230.0f / rdp.vi_height)  * aspect;
  }
  else
  {
    rdp.scale_y = (float)settings.res_y / rdp.vi_height * aspect;
  }
  //  rdp.offset_x = settings.offset_x * res_scl_x;
  //  rdp.offset_y = settings.offset_y * res_scl_y;
  //rdp.offset_x = 0;
  //  rdp.offset_y = 0;
  rdp.offset_y = ((float)settings.res_y - rdp.vi_height * rdp.scale_y) * 0.5f;
  if (((wxUint32)rdp.vi_width <= (*gfx.VI_WIDTH_REG)/2) && (rdp.vi_width > rdp.vi_height))
    rdp.scale_y *= 0.5f;

  rdp.scissor_o.ul_x = 0;
  rdp.scissor_o.ul_y = 0;
  rdp.scissor_o.lr_x = (wxUint32)rdp.vi_width;
  rdp.scissor_o.lr_y = (wxUint32)rdp.vi_height;

  rdp.update |= UPDATE_VIEWPORT | UPDATE_SCISSOR;
}

void ChangeSize ()
{
  if (debugging)
  {
    _ChangeSize ();
    return;
  }
  switch (settings.aspectmode)
  {
  case 0: //4:3
    if (settings.scr_res_x >= settings.scr_res_y * 4.0f / 3.0f) {
      settings.res_y = settings.scr_res_y;
      settings.res_x = (wxUint32)(settings.res_y * 4.0f / 3.0f);
    } else {
      settings.res_x = settings.scr_res_x;
      settings.res_y = (wxUint32)(settings.res_x / 4.0f * 3.0f);
    }
    break;
  case 1: //16:9
    if (settings.scr_res_x >= settings.scr_res_y * 16.0f / 9.0f) {
      settings.res_y = settings.scr_res_y;
      settings.res_x = (wxUint32)(settings.res_y * 16.0f / 9.0f);
    } else {
      settings.res_x = settings.scr_res_x;
      settings.res_y = (wxUint32)(settings.res_x / 16.0f * 9.0f);
    }
    break;
  default: //stretch or original
    settings.res_x = settings.scr_res_x;
    settings.res_y = settings.scr_res_y;
  }
  _ChangeSize ();
  rdp.offset_x = (settings.scr_res_x - settings.res_x) / 2.0f;
  float offset_y = (settings.scr_res_y - settings.res_y) / 2.0f;
  settings.res_x += (wxUint32)rdp.offset_x;
  settings.res_y += (wxUint32)offset_y;
  rdp.offset_y += offset_y;
  if (settings.aspectmode == 3) // original
  {
	  rdp.scale_x = rdp.scale_y = 1.0f;
	  rdp.offset_x = (settings.scr_res_x - rdp.vi_width) / 2.0f;
	  rdp.offset_y = (settings.scr_res_y - rdp.vi_height) / 2.0f;
  }
  //	settings.res_x = settings.scr_res_x;
  //	settings.res_y = settings.scr_res_y;
}

void ConfigWrapper()
{
  char strConfigWrapperExt[] = "grConfigWrapperExt";
  GRCONFIGWRAPPEREXT grConfigWrapperExt = (GRCONFIGWRAPPEREXT)grGetProcAddress(strConfigWrapperExt);
  if (grConfigWrapperExt)
    grConfigWrapperExt(settings.wrpResolution, settings.wrpVRAM * 1024 * 1024, settings.wrpFBO, settings.wrpAnisotropic);
}
/*
static wxConfigBase * OpenIni()
{
  wxConfigBase * ini = wxConfigBase::Get(false);
  if (!ini)
  {
    if (iniName.IsEmpty())
      iniName = pluginPath + wxT("/Glide64mk2.ini");
    if (wxFileExists(iniName))
    {
      wxFileInputStream is(iniName);
      wxFileConfig * fcfg = new wxFileConfig(is, wxConvISO8859_1);
      wxConfigBase::Set(fcfg);
      ini = fcfg;
    }
  }
  if (!ini)
    wxMessageBox(_T("Can not find ini file! Plugin will not run properly."), _T("File not found"), wxOK|wxICON_EXCLAMATION);
  return ini;
}
*/

void WriteLog(m64p_msg_level level, const char *msg, ...)
{
  char buf[1024];
  va_list args;
  va_start(args, msg);
  vsnprintf(buf, 1023, msg, args);
  buf[1023]='\0';
  va_end(args);
  if (l_DebugCallback)
  {
    l_DebugCallback(l_DebugCallContext, level, buf);
  }
}

void ReadSettings ()
{
  //  LOG("ReadSettings\n");
  if (!Config_Open())
  {
    ERRLOG("Could not open configuration!");
    return;
  }

  settings.card_id = (BYTE)Config_ReadInt ("card_id", "Card ID", 0, TRUE, FALSE);
  //settings.lang_id not needed
  // depth_bias = -Config_ReadInt ("depth_bias", "Depth bias level", 0, TRUE, FALSE);
  settings.res_data = 0;
  settings.scr_res_x = settings.res_x = Config_ReadScreenInt("ScreenWidth");
  settings.scr_res_y = settings.res_y = Config_ReadScreenInt("ScreenHeight");

  settings.rotate = Config_ReadScreenInt("Rotate");

  settings.force_polygon_offset = (BOOL)Config_ReadInt("force_polygon_offset", "If true, use polygon offset values specified below", 0, TRUE, TRUE);
  settings.polygon_offset_factor = Config_ReadFloat("polygon_offset_factor", "Specifies a scale factor that is used to create a variable depth offset for each polygon", 0.0f);
  settings.polygon_offset_units = Config_ReadFloat("polygon_offset_units", "Is multiplied by an implementation-specific value to create a constant depth offset", 0.0f);

#ifdef USE_FRAMESKIPPER
  settings.autoframeskip = (BOOL)Config_ReadInt("autoframeskip", "If true, skip up to maxframeskip frames to maintain clock schedule; if false, skip exactly maxframeskip frames", 0, TRUE, TRUE);
  settings.maxframeskip = Config_ReadInt("maxframeskip", "If autoframeskip is true, skip up to this many frames to maintain clock schedule; if autoframeskip is false, skip exactly this many frames", 0, TRUE, FALSE);
  if( settings.autoframeskip )
    frameSkipper.setSkips( FrameSkipper::AUTO, settings.maxframeskip );
  else
    frameSkipper.setSkips( FrameSkipper::MANUAL, settings.maxframeskip );
#endif

  settings.vsync = (BOOL)Config_ReadInt ("vsync", "Vertical sync", 1);
  settings.ssformat = (BOOL)Config_ReadInt("ssformat", "TODO:ssformat", 0);
  //settings.fast_crc = (BOOL)Config_ReadInt ("fast_crc", "Fast CRC", 0);

  settings.show_fps = (BYTE)Config_ReadInt ("show_fps", "Display performance stats (add together desired flags): 1=FPS counter, 2=VI/s counter, 4=% speed, 8=FPS transparent", 0, TRUE, FALSE);
  settings.clock = (BOOL)Config_ReadInt ("clock", "Clock enabled", 0);
  settings.clock_24_hr = (BOOL)Config_ReadInt ("clock_24_hr", "Clock is 24-hour", 1);
  // settings.advanced_options only good for GUI config
  // settings.texenh_options = only good for GUI config
  //settings.use_hotkeys = ini->Read(_T("hotkeys"), 1l);

  settings.wrpResolution = (BYTE)Config_ReadInt ("wrpResolution", "Wrapper resolution", 0, TRUE, FALSE);
  settings.wrpVRAM = (BYTE)Config_ReadInt ("wrpVRAM", "Wrapper VRAM", 0, TRUE, FALSE);
  settings.wrpFBO = (BOOL)Config_ReadInt ("wrpFBO", "Wrapper FBO", 1, TRUE, TRUE);
  settings.wrpAnisotropic = (BOOL)Config_ReadInt ("wrpAnisotropic", "Wrapper Anisotropic Filtering", 1, TRUE, TRUE);

#ifndef _ENDUSER_RELEASE_
  settings.autodetect_ucode = (BOOL)Config_ReadInt ("autodetect_ucode", "Auto-detect microcode", 1);
  settings.ucode = (wxUint32)Config_ReadInt ("ucode", "Force microcode", 2, TRUE, FALSE);
  settings.wireframe = (BOOL)Config_ReadInt ("wireframe", "Wireframe display", 0);
  settings.wfmode = (int)Config_ReadInt ("wfmode", "Wireframe mode: 0=Normal colors, 1=Vertex colors, 2=Red only", 1, TRUE, FALSE);

  settings.logging = (BOOL)Config_ReadInt ("logging", "Logging", 0);
  settings.log_clear = (BOOL)Config_ReadInt ("log_clear", "", 0);

  settings.run_in_window = (BOOL)Config_ReadInt ("run_in_window", "", 0);

  settings.elogging = (BOOL)Config_ReadInt ("elogging", "", 0);
  settings.filter_cache = (BOOL)Config_ReadInt ("filter_cache", "Filter cache", 0);
  settings.unk_as_red = (BOOL)Config_ReadInt ("unk_as_red", "Display unknown combines as red", 0);
  settings.log_unk = (BOOL)Config_ReadInt ("log_unk", "Log unknown combines", 0);
  settings.unk_clear = (BOOL)Config_ReadInt ("unk_clear", "", 0);
#else
  settings.autodetect_ucode = TRUE;
  settings.ucode = 2;
  settings.wireframe = FALSE;
  settings.wfmode = 0;
  settings.logging = FALSE;
  settings.log_clear = FALSE;
  settings.run_in_window = FALSE;
  settings.elogging = FALSE;
  settings.filter_cache = FALSE;
  settings.unk_as_red = FALSE;
  settings.log_unk = FALSE;
  settings.unk_clear = FALSE;
#endif

#ifdef TEXTURE_FILTER
  
  // settings.ghq_fltr range is 0 through 6
  // Filters:\nApply a filter to either smooth or sharpen textures.\nThere are 4 different smoothing filters and 2 different sharpening filters.\nThe higher the number, the stronger the effect,\ni.e. \"Smoothing filter 4\" will have a much more noticeable effect than \"Smoothing filter 1\".\nBe aware that performance may have an impact depending on the game and/or the PC.\n[Recommended: your preference]
  // _("None"),
  // _("Smooth filtering 1"),
  // _("Smooth filtering 2"),
  // _("Smooth filtering 3"),
  // _("Smooth filtering 4"),
  // _("Sharp filtering 1"),
  // _("Sharp filtering 2")

// settings.ghq_cmpr 0=S3TC and 1=FXT1

//settings.ghq_ent is ___
// "Texture enhancement:\n7 different filters are selectable here, each one with a distinctive look.\nBe aware of possible performance impacts.\n\nIMPORTANT: 'Store' mode - saves textures in cache 'as is'. It can improve performance in games, which load many textures.\nDisable 'Ignore backgrounds' option for better result.\n\n[Recommended: your preference]"



  settings.ghq_fltr = Config_ReadInt ("ghq_fltr", "Texture Enhancement: Smooth/Sharpen Filters", 0, TRUE, FALSE);
  settings.ghq_cmpr = Config_ReadInt ("ghq_cmpr", "Texture Compression: 0 for S3TC, 1 for FXT1", 0, TRUE, FALSE);
  settings.ghq_enht = Config_ReadInt ("ghq_enht", "Texture Enhancement: More filters", 0, TRUE, FALSE);
  settings.ghq_hirs = Config_ReadInt ("ghq_hirs", "Hi-res texture pack format (0 for none, 1 for Rice)", 0, TRUE, FALSE);
  settings.ghq_enht_cmpr  = Config_ReadInt ("ghq_enht_cmpr", "Compress texture cache with S3TC or FXT1", 0, TRUE, TRUE);
  settings.ghq_enht_tile = Config_ReadInt ("ghq_enht_tile", "Tile textures (saves memory but could cause issues)", 0, TRUE, FALSE);
  settings.ghq_enht_f16bpp = Config_ReadInt ("ghq_enht_f16bpp", "Force 16bpp textures (saves ram but lower quality)", 0, TRUE, TRUE);
  settings.ghq_enht_gz  = Config_ReadInt ("ghq_enht_gz", "Compress texture cache", 1, TRUE, TRUE);
  settings.ghq_enht_nobg  = Config_ReadInt ("ghq_enht_nobg", "Don't enhance textures for backgrounds", 0, TRUE, TRUE);
  settings.ghq_hirs_cmpr  = Config_ReadInt ("ghq_hirs_cmpr", "Enable S3TC and FXT1 compression", 0, TRUE, TRUE);
  settings.ghq_hirs_tile = Config_ReadInt ("ghq_hirs_tile", "Tile hi-res textures (saves memory but could cause issues)", 0, TRUE, TRUE);
  settings.ghq_hirs_f16bpp = Config_ReadInt ("ghq_hirs_f16bpp", "Force 16bpp hi-res textures (saves ram but lower quality)", 0, TRUE, TRUE);
  settings.ghq_hirs_gz  = Config_ReadInt ("ghq_hirs_gz", "Compress hi-res texture cache", 1, TRUE, TRUE);
  settings.ghq_hirs_altcrc = Config_ReadInt ("ghq_hirs_altcrc", "Alternative CRC calculation -- emulates Rice bug", 1, TRUE, TRUE);
  settings.ghq_cache_save = Config_ReadInt ("ghq_cache_save", "Save tex cache to disk", 1, TRUE, TRUE);
  settings.ghq_cache_size = Config_ReadInt ("ghq_cache_size", "Texture Cache Size (MB)", 128, TRUE, FALSE);
  settings.ghq_hirs_let_texartists_fly = Config_ReadInt ("ghq_hirs_let_texartists_fly", "Use full alpha channel -- could cause issues for some tex packs", 0, TRUE, TRUE);
  settings.ghq_hirs_dump = Config_ReadInt ("ghq_hirs_dump", "Dump textures", 0, FALSE, TRUE);
#endif

  settings.special_alt_tex_size = Config_ReadInt("alt_tex_size", "Alternate texture size method: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_use_sts1_only = Config_ReadInt("use_sts1_only", "Use first SETTILESIZE only: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_force_calc_sphere = Config_ReadInt("force_calc_sphere", "Use spheric mapping only: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_correct_viewport = Config_ReadInt("correct_viewport", "Force positive viewport: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_increase_texrect_edge = Config_ReadInt("increase_texrect_edge", "Force texrect size to integral value: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_decrease_fillrect_edge = Config_ReadInt("decrease_fillrect_edge", "Reduce fillrect size by 1: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_texture_correction = Config_ReadInt("texture_correction", "Enable perspective texture correction emulation: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_pal230 = Config_ReadInt("pal230", "Set special scale for PAL games: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_stipple_mode = Config_ReadInt("stipple_mode", "3DFX Dithered alpha emulation mode: -1=Game default, >=0=dithered alpha emulation mode", -1, TRUE, FALSE);
  settings.special_stipple_pattern = Config_ReadInt("stipple_pattern", "3DFX Dithered alpha pattern: -1=Game default, >=0=pattern used for dithered alpha emulation", -1, TRUE, FALSE);
  settings.special_force_microcheck = Config_ReadInt("force_microcheck", "Check microcode each frame: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_force_quad3d = Config_ReadInt("force_quad3d", "Force 0xb5 command to be quad, not line 3D: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_clip_zmin = Config_ReadInt("clip_zmin", "Enable near z clipping: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_clip_zmax = Config_ReadInt("clip_zmax", "Enable far plane clipping: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_fast_crc = Config_ReadInt("fast_crc", "Use fast CRC algorithm: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_adjust_aspect = Config_ReadInt("adjust_aspect", "Adjust screen aspect for wide screen mode: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_zmode_compare_less = Config_ReadInt("zmode_compare_less", "Force strict check in Depth buffer test: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_old_style_adither = Config_ReadInt("old_style_adither", "Apply alpha dither regardless of alpha_dither_mode: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_n64_z_scale = Config_ReadInt("n64_z_scale", "Scale vertex z value before writing to depth buffer: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_optimize_texrect = Config_ReadInt("optimize_texrect", "Fast texrect rendering with hwfbe: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_ignore_aux_copy = Config_ReadInt("ignore_aux_copy", "Do not copy auxiliary frame buffers: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_hires_buf_clear = Config_ReadInt("hires_buf_clear", "Clear auxiliary texture frame buffers: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_fb_read_alpha = Config_ReadInt("fb_read_alpha", "Read alpha from framebuffer: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_useless_is_useless = Config_ReadInt("useless_is_useless", "Handle unchanged fb: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_fb_crc_mode = Config_ReadInt("fb_crc_mode", "Set frambuffer CRC mode: -1=Game default, 0=disable CRC, 1=fast CRC, 2=safe CRC", -1, TRUE, FALSE);
  settings.special_filtering = Config_ReadInt("filtering", "Filtering mode: -1=Game default, 0=automatic, 1=force bilinear, 2=force point sampled", -1, TRUE, FALSE);
  settings.special_fog = Config_ReadInt("fog", "Fog: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_buff_clear = Config_ReadInt("buff_clear", "Buffer clear on every frame: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_swapmode = Config_ReadInt("swapmode", "Buffer swapping method: -1=Game default, 0=swap buffers when vertical interrupt has occurred, 1=swap buffers when set of conditions is satisfied. Prevents flicker on some games, 2=mix of first two methods", -1, TRUE, FALSE);
  settings.special_aspect = Config_ReadInt("aspect", "Aspect ratio: -1=Game default, 0=Force 4:3, 1=Force 16:9, 2=Stretch, 3=Original", -1, TRUE, FALSE);
  settings.special_lodmode = Config_ReadInt("lodmode", "LOD calculation: -1=Game default, 0=disable. 1=fast, 2=precise", -1, TRUE, FALSE);
  settings.special_fb_smart = Config_ReadInt("fb_smart", "Smart framebuffer: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_fb_hires = Config_ReadInt("fb_hires", "Hardware frame buffer emulation: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_fb_read_always = Config_ReadInt("fb_read_always", "Read framebuffer every frame (may be slow use only for effects that need it e.g. Banjo Kazooie, DK64 transitions): -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_read_back_to_screen = Config_ReadInt("read_back_to_screen", "Render N64 frame buffer as texture: -1=Game default, 0=disable, 1=mode1, 2=mode2", -1, TRUE, FALSE);
  settings.special_detect_cpu_write = Config_ReadInt("detect_cpu_write", "Show images written directly by CPU: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_fb_get_info = Config_ReadInt("fb_get_info", "Get frame buffer info: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);
  settings.special_fb_render = Config_ReadInt("fb_render", "Enable software depth render: -1=Game default, 0=disable. 1=enable", -1, TRUE, FALSE);

  //TODO-PORT: remove?
  ConfigWrapper();
}

void ReadSpecialSettings (const char * name)
{
  //  char buf [256];
  //  sprintf(buf, "ReadSpecialSettings. Name: %s\n", name);
  //  LOG(buf);
  settings.hacks = 0;

  //detect games which require special hacks
  if (strstr(name, (const char *)"ZELDA"))
    settings.hacks |= (hack_Zelda | hack_OoT);
  else if(strstr(name, (const char *)"MASK"))
    settings.hacks |= hack_Zelda;
  else if (strstr(name, (const char *)"ROADSTERS TROPHY"))
    settings.hacks |= hack_Zelda;
  else if (strstr(name, (const char *)"Diddy Kong Racing"))
    settings.hacks |= hack_Diddy;
  else if (strstr(name, (const char *)"Tonic Trouble"))
    settings.hacks |= hack_Tonic;
  else if (strstr(name, (const char *)"All") && strstr(name, (const char *)"Star") && strstr(name, (const char *)"Baseball"))
    settings.hacks |= hack_ASB;
  else if (strstr(name, (const char *)"Beetle") || strstr(name, (const char *)"BEETLE") || strstr(name, (const char *)"HSV"))
    settings.hacks |= hack_BAR;
  else if (strstr(name, (const char *)"I S S 64") || strstr(name, (const char *)"J WORLD SOCCER3") || strstr(name, (const char *)"PERFECT STRIKER") || strstr(name, (const char *)"RONALDINHO SOCCER"))
    settings.hacks |= hack_ISS64;
  else if (strstr(name, (const char *)"MARIOKART64"))
    settings.hacks |= hack_MK64;
  else if (strstr(name, (const char *)"NITRO64"))
    settings.hacks |= hack_WCWnitro;
  else if (strstr(name, (const char *)"CHOPPER_ATTACK") || strstr(name, (const char *)"WILD CHOPPERS"))
    settings.hacks |= hack_Chopper;
  else if (strstr(name, (const char *)"Resident Evil II") || strstr(name, (const char *)"BioHazard II"))
    settings.hacks |= hack_RE2;
  else if (strstr(name, (const char *)"YOSHI STORY"))
    settings.hacks |= hack_Yoshi;
  else if (strstr(name, (const char *)"F-Zero X") || strstr(name, (const char *)"F-ZERO X"))
    settings.hacks |= hack_Fzero;
  else if (strstr(name, (const char *)"PAPER MARIO") || strstr(name, (const char *)"MARIO STORY"))
    settings.hacks |= hack_PMario;
  else if (strstr(name, (const char *)"TOP GEAR RALLY 2"))
    settings.hacks |= hack_TGR2;
  else if (strstr(name, (const char *)"TOP GEAR RALLY"))
    settings.hacks |= hack_TGR;
  else if (strstr(name, (const char *)"Top Gear Hyper Bike"))
    settings.hacks |= hack_Hyperbike;
  else if (strstr(name, (const char *)"Killer Instinct Gold") || strstr(name, (const char *)"KILLER INSTINCT GOLD"))
    settings.hacks |= hack_KI;
  else if (strstr(name, (const char *)"Knockout Kings 2000"))
    settings.hacks |= hack_Knockout;
  else if (strstr(name, (const char *)"LEGORacers"))
    settings.hacks |= hack_Lego;
  else if (strstr(name, (const char *)"OgreBattle64"))
    settings.hacks |= hack_Ogre64;
  else if (strstr(name, (const char *)"Pilot Wings64"))
    settings.hacks |= hack_Pilotwings;
  else if (strstr(name, (const char *)"Supercross"))
    settings.hacks |= hack_Supercross;
  else if (strstr(name, (const char *)"STARCRAFT 64"))
    settings.hacks |= hack_Starcraft;
  else if (strstr(name, (const char *)"BANJO KAZOOIE 2") || strstr(name, (const char *)"BANJO TOOIE"))
    settings.hacks |= hack_Banjo2;
  else if (strstr(name, (const char *)"FIFA: RTWC 98") || strstr(name, (const char *)"RoadToWorldCup98"))
    settings.hacks |= hack_Fifa98;
  else if (strstr(name, (const char *)"Mega Man 64") || strstr(name, (const char *)"RockMan Dash"))
    settings.hacks |= hack_Megaman;
  else if (strstr(name, (const char *)"MISCHIEF MAKERS") || strstr(name, (const char *)"TROUBLE MAKERS"))
    settings.hacks |= hack_Makers;
  else if (strstr(name, (const char *)"GOLDENEYE"))
    settings.hacks |= hack_GoldenEye;
  else if (strstr(name, (const char *)"PUZZLE LEAGUE"))
    settings.hacks |= hack_PPL;

  Ini * ini = Ini::OpenIni();
  if (!ini)
    return;
  ini->SetPath(name);

  ini->Read(_T("alt_tex_size"), &(settings.alt_tex_size));
  if (settings.special_alt_tex_size >= 0)
    settings.alt_tex_size = settings.special_alt_tex_size;

  ini->Read(_T("use_sts1_only"), &(settings.use_sts1_only));
  if (settings.special_use_sts1_only >= 0)
    settings.use_sts1_only = settings.special_use_sts1_only;

  ini->Read(_T("force_calc_sphere"), &(settings.force_calc_sphere));
  if (settings.special_force_calc_sphere >= 0)
    settings.force_calc_sphere = settings.special_force_calc_sphere;

  ini->Read(_T("correct_viewport"), &(settings.correct_viewport));
  if (settings.special_correct_viewport >= 0)
    settings.correct_viewport = settings.special_correct_viewport;

  ini->Read(_T("increase_texrect_edge"), &(settings.increase_texrect_edge));
  if (settings.special_increase_texrect_edge >= 0)
    settings.increase_texrect_edge = settings.special_increase_texrect_edge;

  ini->Read(_T("decrease_fillrect_edge"), &(settings.decrease_fillrect_edge));
  if (settings.special_decrease_fillrect_edge >= 0)
    settings.decrease_fillrect_edge = settings.special_decrease_fillrect_edge;

  if (ini->Read(_T("texture_correction"), -1) == 0) settings.texture_correction = 0;
  else settings.texture_correction = 1;
  if (settings.special_texture_correction >= 0)
    settings.texture_correction = settings.special_texture_correction;

  if (ini->Read(_T("pal230"), -1) == 1) settings.pal230 = 1;
  else settings.pal230 = 0;
  if (settings.special_pal230 >= 0)
    settings.pal230 = settings.special_pal230;

  ini->Read(_T("stipple_mode"), &(settings.stipple_mode));
  if (settings.special_stipple_mode >= 0)
    settings.stipple_mode = settings.special_stipple_mode;

  int stipple_pattern = ini->Read(_T("stipple_pattern"), -1);
  if (stipple_pattern > 0) settings.stipple_pattern = (wxUint32)stipple_pattern;
  if (settings.special_stipple_pattern >= 0)
    stipple_pattern = settings.special_stipple_pattern;

  ini->Read(_T("force_microcheck"), &(settings.force_microcheck));
  if (settings.special_force_microcheck >= 0)
    settings.force_microcheck = settings.special_force_microcheck;

  ini->Read(_T("force_quad3d"), &(settings.force_quad3d));
  if (settings.special_force_quad3d >= 0)
    settings.force_quad3d = settings.special_force_quad3d;

  ini->Read(_T("clip_zmin"), &(settings.clip_zmin));
  if (settings.special_clip_zmin >= 0)
    settings.clip_zmin = settings.special_clip_zmin;

  ini->Read(_T("clip_zmax"), &(settings.clip_zmax));
  if (settings.special_clip_zmax >= 0)
    settings.clip_zmax = settings.special_clip_zmax;

  ini->Read(_T("fast_crc"), &(settings.fast_crc));
  if (settings.special_fast_crc >= 0)
    settings.fast_crc = settings.special_fast_crc;

  ini->Read(_T("adjust_aspect"), &(settings.adjust_aspect), 1);
  if (settings.special_adjust_aspect >= 0)
    settings.adjust_aspect = settings.special_adjust_aspect;

  ini->Read(_T("zmode_compare_less"), &(settings.zmode_compare_less));
  if (settings.special_zmode_compare_less >= 0)
    settings.zmode_compare_less = settings.special_zmode_compare_less;

  ini->Read(_T("old_style_adither"), &(settings.old_style_adither));
  if (settings.special_old_style_adither >= 0)
    settings.old_style_adither = settings.special_old_style_adither;

  ini->Read(_T("n64_z_scale"), &(settings.n64_z_scale));
  if (settings.special_n64_z_scale >= 0)
    settings.n64_z_scale = settings.special_n64_z_scale;

  if (settings.n64_z_scale)
    ZLUT_init();

  //frame buffer
  int optimize_texrect = ini->Read(_T("optimize_texrect"), -1);
  if (settings.special_optimize_texrect >= 0)
    optimize_texrect = settings.special_optimize_texrect;

  int ignore_aux_copy = ini->Read(_T("ignore_aux_copy"), -1);
  if (settings.special_ignore_aux_copy >= 0)
    ignore_aux_copy = settings.special_ignore_aux_copy;

  int hires_buf_clear = ini->Read(_T("hires_buf_clear"), -1);
  if (settings.special_hires_buf_clear >= 0)
    hires_buf_clear = settings.special_hires_buf_clear;

  int read_alpha = ini->Read(_T("fb_read_alpha"), -1);
  if (settings.special_fb_read_alpha >= 0)
    read_alpha = settings.special_fb_read_alpha;

  int useless_is_useless = ini->Read(_T("useless_is_useless"), -1);
  if (settings.special_useless_is_useless >= 0)
    useless_is_useless = settings.special_useless_is_useless;

  int fb_crc_mode = ini->Read(_T("fb_crc_mode"), -1);
  if (settings.special_fb_crc_mode >= 0)
    fb_crc_mode = settings.special_fb_crc_mode;


  if (optimize_texrect > 0) settings.frame_buffer |= fb_optimize_texrect;
  else if (optimize_texrect == 0) settings.frame_buffer &= ~fb_optimize_texrect;
  if (ignore_aux_copy > 0) settings.frame_buffer |= fb_ignore_aux_copy;
  else if (ignore_aux_copy == 0) settings.frame_buffer &= ~fb_ignore_aux_copy;
  if (hires_buf_clear > 0) settings.frame_buffer |= fb_hwfbe_buf_clear;
  else if (hires_buf_clear == 0) settings.frame_buffer &= ~fb_hwfbe_buf_clear;
  if (read_alpha > 0) settings.frame_buffer |= fb_read_alpha;
  else if (read_alpha == 0) settings.frame_buffer &= ~fb_read_alpha;
  if (useless_is_useless > 0) settings.frame_buffer |= fb_useless_is_useless;
  else settings.frame_buffer &= ~fb_useless_is_useless;
  if (fb_crc_mode >= 0) settings.fb_crc_mode = (SETTINGS::FBCRCMODE)fb_crc_mode;

  //  if (settings.custom_ini)
  {
    ini->Read(_T("filtering"), &(settings.filtering));
    if (settings.special_filtering >= 0)
      settings.filtering = settings.special_filtering;

    ini->Read(_T("fog"), &(settings.fog));
    if (settings.special_fog >= 0)
      settings.fog = settings.special_fog;

    ini->Read(_T("buff_clear"), &(settings.buff_clear));
    if (settings.special_buff_clear >= 0)
      settings.buff_clear = settings.special_buff_clear;

    ini->Read(_T("swapmode"), &(settings.swapmode));
    if (settings.special_swapmode >= 0)
      settings.swapmode = settings.special_swapmode;

    ini->Read(_T("aspect"), &(settings.aspectmode));
    if (settings.special_aspect >= 0)
      settings.aspectmode = settings.special_aspect;

    ini->Read(_T("lodmode"), &(settings.lodmode));
    if (settings.special_lodmode >= 0)
      settings.lodmode = settings.special_lodmode;

    /*
    TODO-port: fix resolutions
    int resolution;
    if (ini->Read(_T("resolution"), &resolution))
    {
      settings.res_data = (wxUint32)resolution;
      if (settings.res_data >= 0x18) settings.res_data = 12;
      settings.scr_res_x = settings.res_x = resolutions[settings.res_data][0];
      settings.scr_res_y = settings.res_y = resolutions[settings.res_data][1];
    }
    */
	
	PackedScreenResolution tmpRes = Config_ReadScreenSettings();
	settings.res_data = tmpRes.resolution;
	settings.scr_res_x = settings.res_x = tmpRes.width;
	settings.scr_res_y = settings.res_y = tmpRes.height;

    //frame buffer
    int smart_read = ini->Read(_T("fb_smart"), -1);
    if (settings.special_fb_smart >= 0)
      smart_read = settings.special_fb_smart;

    int hires = ini->Read(_T("fb_hires"), -1);
    if (settings.special_fb_hires >= 0)
      hires = settings.special_fb_hires;

    int read_always = ini->Read(_T("fb_read_always"), -1);
    if (settings.special_fb_read_always >= 0)
      read_always = settings.special_fb_read_always;

    int read_back_to_screen = ini->Read(_T("read_back_to_screen"), -1);
    if (settings.special_read_back_to_screen >= 0)
      read_back_to_screen = settings.special_read_back_to_screen;

    int cpu_write_hack = ini->Read(_T("detect_cpu_write"), -1);
    if (settings.special_detect_cpu_write >= 0)
      cpu_write_hack = settings.special_detect_cpu_write;

    int get_fbinfo = ini->Read(_T("fb_get_info"), -1);
    if (settings.special_fb_get_info >= 0)
      get_fbinfo = settings.special_fb_get_info;

    int depth_render = ini->Read(_T("fb_render"), -1);
    if (settings.special_fb_render >= 0)
      depth_render = settings.special_fb_render;

    if (smart_read > 0) settings.frame_buffer |= fb_emulation;
    else if (smart_read == 0) settings.frame_buffer &= ~fb_emulation;
    if (hires > 0) settings.frame_buffer |= fb_hwfbe;
    else if (hires == 0) settings.frame_buffer &= ~fb_hwfbe;
    if (read_always > 0) settings.frame_buffer |= fb_ref;
    else if (read_always == 0) settings.frame_buffer &= ~fb_ref;
    if (read_back_to_screen == 1) settings.frame_buffer |= fb_read_back_to_screen;
    else if (read_back_to_screen == 2) settings.frame_buffer |= fb_read_back_to_screen2;
    else if (read_back_to_screen == 0) settings.frame_buffer &= ~(fb_read_back_to_screen|fb_read_back_to_screen2);
    if (cpu_write_hack > 0) settings.frame_buffer |= fb_cpu_write_hack;
    else if (cpu_write_hack == 0) settings.frame_buffer &= ~fb_cpu_write_hack;
    if (get_fbinfo > 0) settings.frame_buffer |= fb_get_info;
    else if (get_fbinfo == 0) settings.frame_buffer &= ~fb_get_info;
    if (depth_render > 0) settings.frame_buffer |= fb_depth_render;
    else if (depth_render == 0) settings.frame_buffer &= ~fb_depth_render;
    settings.frame_buffer |= fb_motionblur;
  }
  settings.flame_corona = (settings.hacks & hack_Zelda) && !fb_depth_render_enabled;
}

//TODO-PORT: more ini stuff
void WriteSettings (bool saveEmulationSettings)
{
/*
  wxConfigBase * ini = OpenIni();
  if (!ini || !ini->HasGroup(_T("/SETTINGS")))
    return;
  ini->SetPath(_T("/SETTINGS"));

  ini->Write(_T("card_id"), settings.card_id);
  ini->Write(_T("lang_id"), settings.lang_id);
  ini->Write(_T("resolution"), (int)settings.res_data);
  ini->Write(_T("ssformat"), settings.ssformat);
  ini->Write(_T("vsync"), settings.vsync);
  ini->Write(_T("show_fps"), settings.show_fps);
  ini->Write(_T("clock"), settings.clock);
  ini->Write(_T("clock_24_hr"), settings.clock_24_hr);
  ini->Write(_T("advanced_options"), settings.advanced_options);
  ini->Write(_T("texenh_options"), settings.texenh_options);

  ini->Write(_T("wrpResolution"), settings.wrpResolution);
  ini->Write(_T("wrpVRAM"), settings.wrpVRAM);
  ini->Write(_T("wrpFBO"), settings.wrpFBO);
  ini->Write(_T("wrpAnisotropic"), settings.wrpAnisotropic);

#ifndef _ENDUSER_RELEASE_
  ini->Write(_T("autodetect_ucode"), settings.autodetect_ucode);
  ini->Write(_T("ucode"), (int)settings.ucode);
  ini->Write(_T("wireframe"), settings.wireframe);
  ini->Write(_T("wfmode"), settings.wfmode);
  ini->Write(_T("logging"), settings.logging);
  ini->Write(_T("log_clear"), settings.log_clear);
  ini->Write(_T("run_in_window"), settings.run_in_window);
  ini->Write(_T("elogging"), settings.elogging);
  ini->Write(_T("filter_cache"), settings.filter_cache);
  ini->Write(_T("unk_as_red"), settings.unk_as_red);
  ini->Write(_T("log_unk"), settings.log_unk);
  ini->Write(_T("unk_clear"), settings.unk_clear);
#endif //_ENDUSER_RELEASE_

#ifdef TEXTURE_FILTER
  ini->Write(_T("ghq_fltr"), settings.ghq_fltr);
  ini->Write(_T("ghq_cmpr"), settings.ghq_cmpr);
  ini->Write(_T("ghq_enht"), settings.ghq_enht);
  ini->Write(_T("ghq_hirs"), settings.ghq_hirs);
  ini->Write(_T("ghq_enht_cmpr"), settings.ghq_enht_cmpr);
  ini->Write(_T("ghq_enht_tile"), settings.ghq_enht_tile);
  ini->Write(_T("ghq_enht_f16bpp"), settings.ghq_enht_f16bpp);
  ini->Write(_T("ghq_enht_gz"), settings.ghq_enht_gz);
  ini->Write(_T("ghq_enht_nobg"), settings.ghq_enht_nobg);
  ini->Write(_T("ghq_hirs_cmpr"), settings.ghq_hirs_cmpr);
  ini->Write(_T("ghq_hirs_tile"), settings.ghq_hirs_tile);
  ini->Write(_T("ghq_hirs_f16bpp"), settings.ghq_hirs_f16bpp);
  ini->Write(_T("ghq_hirs_gz"), settings.ghq_hirs_gz);
  ini->Write(_T("ghq_hirs_altcrc"), settings.ghq_hirs_altcrc);
  ini->Write(_T("ghq_cache_save"), settings.ghq_cache_save);
  ini->Write(_T("ghq_cache_size"), settings.ghq_cache_size);
  ini->Write(_T("ghq_hirs_let_texartists_fly"), settings.ghq_hirs_let_texartists_fly);
  ini->Write(_T("ghq_hirs_dump"), settings.ghq_hirs_dump);
#endif

  if (saveEmulationSettings)
  {
    if (romopen)
    {
      wxString S = _T("/");
      ini->SetPath(S+rdp.RomName);
    }
    else
      ini->SetPath(_T("/DEFAULT"));
    ini->Write(_T("filtering"), settings.filtering);
    ini->Write(_T("fog"), settings.fog);
    ini->Write(_T("buff_clear"), settings.buff_clear);
    ini->Write(_T("swapmode"), settings.swapmode);
    ini->Write(_T("lodmode"), settings.lodmode);
    ini->Write(_T("aspect"), settings.aspectmode);

    ini->Write(_T("fb_read_always"), settings.frame_buffer&fb_ref ? 1 : 0l);
    ini->Write(_T("fb_smart"), settings.frame_buffer & fb_emulation ? 1 : 0l);
    //    ini->Write("motionblur", settings.frame_buffer & fb_motionblur ? 1 : 0);
    ini->Write(_T("fb_hires"), settings.frame_buffer & fb_hwfbe ? 1 : 0l);
    ini->Write(_T("fb_get_info"), settings.frame_buffer & fb_get_info ? 1 : 0l);
    ini->Write(_T("fb_render"), settings.frame_buffer & fb_depth_render ? 1 : 0l);
    ini->Write(_T("detect_cpu_write"), settings.frame_buffer & fb_cpu_write_hack ? 1 : 0l);
    if (settings.frame_buffer & fb_read_back_to_screen)
      ini->Write(_T("read_back_to_screen"), 1);
    else if (settings.frame_buffer & fb_read_back_to_screen2)
      ini->Write(_T("read_back_to_screen"), 2);
    else
      ini->Write(_T("read_back_to_screen"), 0l);
  }

  wxFileOutputStream os(iniName);
  ((wxFileConfig*)ini)->Save(os);
*/
}

GRTEXBUFFEREXT   grTextureBufferExt = NULL;
GRTEXBUFFEREXT   grTextureAuxBufferExt = NULL;
GRAUXBUFFEREXT   grAuxBufferExt = NULL;
GRSTIPPLE grStippleModeExt = NULL;
GRSTIPPLE grStipplePatternExt = NULL;
FxBool (FX_CALL *grKeyPressed)(FxU32) = NULL;

int GetTexAddrUMA(int tmu, int texsize)
{
  int addr = voodoo.tex_min_addr[0] + voodoo.tmem_ptr[0];
  voodoo.tmem_ptr[0] += texsize;
  voodoo.tmem_ptr[1] = voodoo.tmem_ptr[0];
  return addr;
}
int GetTexAddrNonUMA(int tmu, int texsize)
{
  int addr = voodoo.tex_min_addr[tmu] + voodoo.tmem_ptr[tmu];
  voodoo.tmem_ptr[tmu] += texsize;
  return addr;
}
GETTEXADDR GetTexAddr = GetTexAddrNonUMA;

// guLoadTextures - used to load the cursor and font textures
void guLoadTextures ()
{
  if (grTextureBufferExt)
  {
    int tbuf_size = 0;
    if (voodoo.max_tex_size <= 256)
    {
      grTextureBufferExt(  GR_TMU1, voodoo.tex_min_addr[GR_TMU1], GR_LOD_LOG2_256, GR_LOD_LOG2_256,
        GR_ASPECT_LOG2_1x1, GR_TEXFMT_RGB_565, GR_MIPMAPLEVELMASK_BOTH );
      tbuf_size = 8 * grTexCalcMemRequired(GR_LOD_LOG2_256, GR_LOD_LOG2_256,
        GR_ASPECT_LOG2_1x1, GR_TEXFMT_RGB_565);
    }
    else if (settings.scr_res_x <= 1024)
    {
      grTextureBufferExt(  GR_TMU0, voodoo.tex_min_addr[GR_TMU0], GR_LOD_LOG2_1024, GR_LOD_LOG2_1024,
        GR_ASPECT_LOG2_1x1, GR_TEXFMT_RGB_565, GR_MIPMAPLEVELMASK_BOTH );
      tbuf_size = grTexCalcMemRequired(GR_LOD_LOG2_1024, GR_LOD_LOG2_1024,
        GR_ASPECT_LOG2_1x1, GR_TEXFMT_RGB_565);
      grRenderBuffer( GR_BUFFER_TEXTUREBUFFER_EXT );
      grBufferClear (0, 0, 0xFFFF);
      grRenderBuffer( GR_BUFFER_BACKBUFFER );
    }
    else
    {
      grTextureBufferExt(  GR_TMU0, voodoo.tex_min_addr[GR_TMU0], GR_LOD_LOG2_2048, GR_LOD_LOG2_2048,
        GR_ASPECT_LOG2_1x1, GR_TEXFMT_RGB_565, GR_MIPMAPLEVELMASK_BOTH );
      tbuf_size = grTexCalcMemRequired(GR_LOD_LOG2_2048, GR_LOD_LOG2_2048,
        GR_ASPECT_LOG2_1x1, GR_TEXFMT_RGB_565);
      grRenderBuffer( GR_BUFFER_TEXTUREBUFFER_EXT );
      grBufferClear (0, 0, 0xFFFF);
      grRenderBuffer( GR_BUFFER_BACKBUFFER );
    }

    rdp.texbufs[0].tmu = GR_TMU0;
    rdp.texbufs[0].begin = voodoo.tex_min_addr[GR_TMU0];
    rdp.texbufs[0].end = rdp.texbufs[0].begin+tbuf_size;
    rdp.texbufs[0].count = 0;
    rdp.texbufs[0].clear_allowed = TRUE;
    offset_font = tbuf_size;
    if (voodoo.num_tmu > 1)
    {
      rdp.texbufs[1].tmu = GR_TMU1;
      rdp.texbufs[1].begin = voodoo.tex_UMA ? rdp.texbufs[0].end : voodoo.tex_min_addr[GR_TMU1];
      rdp.texbufs[1].end = rdp.texbufs[1].begin+tbuf_size;
      rdp.texbufs[1].count = 0;
      rdp.texbufs[1].clear_allowed = TRUE;
      if (voodoo.tex_UMA)
        offset_font += tbuf_size;
      else
        offset_texbuf1 = tbuf_size;
    }
  }
  else
    offset_font = 0;

#include "font.h"
  wxUint32 *data = (wxUint32*)font;
  wxUint32 cur;

  // ** Font texture **
  wxUint8 *tex8 = (wxUint8*)malloc(256*64);

  fontTex.smallLodLog2 = fontTex.largeLodLog2 = GR_LOD_LOG2_256;
  fontTex.aspectRatioLog2 = GR_ASPECT_LOG2_4x1;
  fontTex.format = GR_TEXFMT_ALPHA_8;
  fontTex.data = tex8;

  // Decompression: [1-bit inverse alpha --> 8-bit alpha]
  wxUint32 i,b;
  for (i=0; i<0x200; i++)
  {
    // cur = ~*(data++), byteswapped
#ifdef __VISUALC__
    cur = _byteswap_ulong(~*(data++));
#else
    cur = ~*(data++);
    cur = ((cur&0xFF)<<24)|(((cur>>8)&0xFF)<<16)|(((cur>>16)&0xFF)<<8)|((cur>>24)&0xFF);
#endif

    for (b=0x80000000; b!=0; b>>=1)
    {
      if (cur&b) *tex8 = 0xFF;
      else *tex8 = 0x00;
      tex8 ++;
    }
  }

  grTexDownloadMipMap (GR_TMU0,
    voodoo.tex_min_addr[GR_TMU0] + offset_font,
    GR_MIPMAPLEVELMASK_BOTH,
    &fontTex);

  offset_cursor = offset_font + grTexTextureMemRequired (GR_MIPMAPLEVELMASK_BOTH, &fontTex);

  free (fontTex.data);

  // ** Cursor texture **
#include "cursor.h"
  data = (wxUint32*)cursor;

  wxUint16 *tex16 = (wxUint16*)malloc(32*32*2);

  cursorTex.smallLodLog2 = cursorTex.largeLodLog2 = GR_LOD_LOG2_32;
  cursorTex.aspectRatioLog2 = GR_ASPECT_LOG2_1x1;
  cursorTex.format = GR_TEXFMT_ARGB_1555;
  cursorTex.data = tex16;

  // Conversion: [16-bit 1555 (swapped) --> 16-bit 1555]
  for (i=0; i<0x200; i++)
  {
    cur = *(data++);
    *(tex16++) = (wxUint16)(((cur&0x000000FF)<<8)|((cur&0x0000FF00)>>8));
    *(tex16++) = (wxUint16)(((cur&0x00FF0000)>>8)|((cur&0xFF000000)>>24));
  }

  grTexDownloadMipMap (GR_TMU0,
    voodoo.tex_min_addr[GR_TMU0] + offset_cursor,
    GR_MIPMAPLEVELMASK_BOTH,
    &cursorTex);

  // Round to higher 16
  offset_textures = ((offset_cursor + grTexTextureMemRequired (GR_MIPMAPLEVELMASK_BOTH, &cursorTex))
    & 0xFFFFFFF0) + 16;
  free (cursorTex.data);
}

#ifdef TEXTURE_FILTER
void DisplayLoadProgress(const wchar_t *format, ...)
{
  va_list args;
  wchar_t wbuf[INFO_BUF];
  char buf[INFO_BUF];

  // process input
  va_start(args, format);
  vswprintf(wbuf, INFO_BUF, format, args);
  va_end(args);

  // XXX: convert to multibyte
  wcstombs(buf, wbuf, INFO_BUF);

  if (fullscreen)
  {
    float x;
    set_message_combiner ();
    output (382, 380, 1, "LOADING TEXTURES. PLEASE WAIT...");
    int len = min (strlen(buf)*8, 1024);
    x = (1024-len)/2.0f;
    output (x, 360, 1, "%s", buf);
    grBufferSwap (0);
    grColorMask (FXTRUE, FXTRUE);
    grBufferClear (0, 0, 0xFFFF);
  }
}
#endif

int InitGfx ()
{
#ifdef TEXTURE_FILTER
  wchar_t romname[256];
  wchar_t foldername[PATH_MAX + 64];
  wchar_t cachename[PATH_MAX + 64];
#endif
  if (fullscreen)
    ReleaseGfx ();

  OPEN_RDP_LOG ();  // doesn't matter if opens again; it will check for it
  OPEN_RDP_E_LOG ();
  VLOG ("InitGfx ()\n");

  debugging = FALSE;
  rdp_reset ();

  // Initialize Glide
  grGlideInit ();

  // Select the Glide device
  grSstSelect (settings.card_id);

  // Is mirroring allowed?
  const char *extensions = grGetString (GR_EXTENSION);

  // Check which SST we are using and initialize stuff
  // Hiroshi Morii <koolsmoky@users.sourceforge.net>
  enum {
    GR_SSTTYPE_VOODOO  = 0,
    GR_SSTTYPE_SST96   = 1,
    GR_SSTTYPE_AT3D    = 2,
    GR_SSTTYPE_Voodoo2 = 3,
    GR_SSTTYPE_Banshee = 4,
    GR_SSTTYPE_Voodoo3 = 5,
    GR_SSTTYPE_Voodoo4 = 6,
    GR_SSTTYPE_Voodoo5 = 7
  };
  const char *hardware = grGetString(GR_HARDWARE);
  unsigned int SST_type = GR_SSTTYPE_VOODOO;
  if (strstr(hardware, "Rush")) {
    SST_type = GR_SSTTYPE_SST96;
  } else if (strstr(hardware, "Voodoo2")) {
    SST_type = GR_SSTTYPE_Voodoo2;
  } else if (strstr(hardware, "Voodoo Banshee")) {
    SST_type = GR_SSTTYPE_Banshee;
  } else if (strstr(hardware, "Voodoo3")) {
    SST_type = GR_SSTTYPE_Voodoo3;
  } else if (strstr(hardware, "Voodoo4")) {
    SST_type = GR_SSTTYPE_Voodoo4;
  } else if (strstr(hardware, "Voodoo5")) {
    SST_type = GR_SSTTYPE_Voodoo5;
  }
  // 2Mb Texture boundary
  voodoo.has_2mb_tex_boundary = (SST_type < GR_SSTTYPE_Banshee) && !evoodoo;
  // use UMA if available
  voodoo.tex_UMA = FALSE;
  //*
  if (strstr(extensions, " TEXUMA ")) {
    // we get better texture cache hits with UMA on
    grEnable(GR_TEXTURE_UMA_EXT);
    voodoo.tex_UMA = TRUE;
    LOG ("Using TEXUMA extension.\n");
  }
  //*/
//TODO-PORT: fullscreen stuff
  wxUint32 res_data = settings.res_data;
  char strWrapperFullScreenResolutionExt[] = "grWrapperFullScreenResolutionExt";
  if (ev_fullscreen)
  {
      GRWRAPPERFULLSCREENRESOLUTIONEXT grWrapperFullScreenResolutionExt =
        (GRWRAPPERFULLSCREENRESOLUTIONEXT)grGetProcAddress(strWrapperFullScreenResolutionExt);
      if (grWrapperFullScreenResolutionExt) {
        wxUint32 _width, _height = 0;
        settings.res_data = grWrapperFullScreenResolutionExt(&_width, &_height);
        settings.scr_res_x = settings.res_x = _width;
        settings.scr_res_y = settings.res_y = _height;
      }
      res_data = settings.res_data;
  }
  else if (evoodoo)
  {
      GRWRAPPERFULLSCREENRESOLUTIONEXT grWrapperFullScreenResolutionExt =
        (GRWRAPPERFULLSCREENRESOLUTIONEXT)grGetProcAddress(strWrapperFullScreenResolutionExt);
      if (grWrapperFullScreenResolutionExt != NULL)
      {
/*
        TODO-port: fix resolutions
        settings.res_data = settings.res_data_org;
        settings.scr_res_x = settings.res_x = resolutions[settings.res_data][0];
        settings.scr_res_y = settings.res_y = resolutions[settings.res_data][1];
*/
      }
      res_data = settings.res_data | 0x80000000;
  }

  gfx_context = 0;

  // Select the window

  if (fb_hwfbe_enabled)
  {
    char strSstWinOpenExt[] ="grSstWinOpenExt";
    GRWINOPENEXT grSstWinOpenExt = (GRWINOPENEXT)grGetProcAddress(strSstWinOpenExt);
    if (grSstWinOpenExt)
      gfx_context = grSstWinOpenExt ((uintptr_t)NULL,
      res_data,
      GR_REFRESH_60Hz,
      GR_COLORFORMAT_RGBA,
      GR_ORIGIN_UPPER_LEFT,
      fb_emulation_enabled?GR_PIXFMT_RGB_565:GR_PIXFMT_ARGB_8888, //32b color is not compatible with fb emulation
      2,    // Double-buffering
      1);   // 1 auxillary buffer
  }
  if (!gfx_context)
    gfx_context = grSstWinOpen ((uintptr_t)NULL,
    res_data,
    GR_REFRESH_60Hz,
    GR_COLORFORMAT_RGBA,
    GR_ORIGIN_UPPER_LEFT,
    2,    // Double-buffering
    1);   // 1 auxillary buffer

  if (!gfx_context)
  {
    ERRLOG("Error setting display mode");
    //    grSstWinClose (gfx_context);
    grGlideShutdown ();
    return FALSE;
  }

  fullscreen = TRUE;
  to_fullscreen = FALSE;


  // get the # of TMUs available
  grGet (GR_NUM_TMU, 4, (FxI32*)&voodoo.num_tmu);
  // get maximal texture size
  grGet (GR_MAX_TEXTURE_SIZE, 4, (FxI32*)&voodoo.max_tex_size);
  voodoo.sup_large_tex = (voodoo.max_tex_size > 256 && !(settings.hacks & hack_PPL));

  //num_tmu = 1;
  if (voodoo.tex_UMA)
  {
    GetTexAddr = GetTexAddrUMA;
    voodoo.tex_min_addr[0] = voodoo.tex_min_addr[1] = grTexMinAddress(GR_TMU0);
    voodoo.tex_max_addr[0] = voodoo.tex_max_addr[1] = grTexMaxAddress(GR_TMU0);
  }
  else
  {
    GetTexAddr = GetTexAddrNonUMA;
    voodoo.tex_min_addr[0] = grTexMinAddress(GR_TMU0);
    voodoo.tex_min_addr[1] = grTexMinAddress(GR_TMU1);
    voodoo.tex_max_addr[0] = grTexMaxAddress(GR_TMU0);
    voodoo.tex_max_addr[1] = grTexMaxAddress(GR_TMU1);
  }

  if (strstr (extensions, "TEXMIRROR") && !(settings.hacks&hack_Zelda)) //zelda's trees suffer from hardware mirroring
    voodoo.sup_mirroring = 1;
  else
    voodoo.sup_mirroring = 0;

  if (strstr (extensions, "TEXFMT"))  //VSA100 texture format extension
    voodoo.sup_32bit_tex = TRUE;
  else
    voodoo.sup_32bit_tex = FALSE;

  voodoo.gamma_correction = 0;
  if (strstr(extensions, "GETGAMMA"))
    grGet(GR_GAMMA_TABLE_ENTRIES, sizeof(voodoo.gamma_table_size), &voodoo.gamma_table_size);

  if (fb_hwfbe_enabled)
  {
    if (char * extstr = (char*)strstr(extensions, "TEXTUREBUFFER"))
    {
      if (!strncmp(extstr, "TEXTUREBUFFER", 13))
      {
        char strTextureBufferExt[] = "grTextureBufferExt";
        grTextureBufferExt = (GRTEXBUFFEREXT) grGetProcAddress(strTextureBufferExt);
        char strTextureAuxBufferExt[] = "grTextureAuxBufferExt";
        grTextureAuxBufferExt = (GRTEXBUFFEREXT) grGetProcAddress(strTextureAuxBufferExt);
        char strAuxBufferExt[] = "grAuxBufferExt";
        grAuxBufferExt = (GRAUXBUFFEREXT) grGetProcAddress(strAuxBufferExt);
      }
    }
    else
      settings.frame_buffer &= ~fb_hwfbe;
  }
  else
    grTextureBufferExt = 0;

  grStippleModeExt = (GRSTIPPLE)grStippleMode;
  grStipplePatternExt = (GRSTIPPLE)grStipplePattern;

  if (grStipplePatternExt)
    grStipplePatternExt(settings.stipple_pattern);

//  char strKeyPressedExt[] = "grKeyPressedExt";
//  grKeyPressed = (FxBool (FX_CALL *)(FxU32))grGetProcAddress (strKeyPressedExt);

  InitCombine();

#ifdef SIMULATE_VOODOO1
  voodoo.num_tmu = 1;
  voodoo.sup_mirroring = 0;
#endif

#ifdef SIMULATE_BANSHEE
  voodoo.num_tmu = 1;
  voodoo.sup_mirroring = 1;
#endif

  grCoordinateSpace (GR_WINDOW_COORDS);
  grVertexLayout (GR_PARAM_XY, offsetof(VERTEX,x), GR_PARAM_ENABLE);
  grVertexLayout (GR_PARAM_Q, offsetof(VERTEX,q), GR_PARAM_ENABLE);
  grVertexLayout (GR_PARAM_Z, offsetof(VERTEX,z), GR_PARAM_ENABLE);
  grVertexLayout (GR_PARAM_ST0, offsetof(VERTEX,coord[0]), GR_PARAM_ENABLE);
  grVertexLayout (GR_PARAM_ST1, offsetof(VERTEX,coord[2]), GR_PARAM_ENABLE);
  grVertexLayout (GR_PARAM_PARGB, offsetof(VERTEX,b), GR_PARAM_ENABLE);

  grCullMode(GR_CULL_NEGATIVE);

  if (settings.fog) //"FOGCOORD" extension
  {
    if (strstr (extensions, "FOGCOORD"))
    {
      GrFog_t fog_t[64];
      guFogGenerateLinear (fog_t, 0.0f, 255.0f);//(float)rdp.fog_multiplier + (float)rdp.fog_offset);//256.0f);

      for (int i = 63; i > 0; i--)
      {
        if (fog_t[i] - fog_t[i-1] > 63)
        {
          fog_t[i-1] = fog_t[i] - 63;
        }
      }
      fog_t[0] = 0;
      //      for (int f = 0; f < 64; f++)
      //      {
      //        FRDP("fog[%d]=%d->%f\n", f, fog_t[f], guFogTableIndexToW(f));
      //      }
      grFogTable (fog_t);
      grVertexLayout (GR_PARAM_FOG_EXT, offsetof(VERTEX,f), GR_PARAM_ENABLE);
    }
    else //not supported
      settings.fog = FALSE;
  }

  grDepthBufferMode (GR_DEPTHBUFFER_ZBUFFER);
  grDepthBufferFunction(GR_CMP_LESS);
  grDepthMask(FXTRUE);

  settings.res_x = settings.scr_res_x;
  settings.res_y = settings.scr_res_y;
  ChangeSize ();

  guLoadTextures ();
  ClearCache ();

  grCullMode (GR_CULL_DISABLE);
  grDepthBufferMode (GR_DEPTHBUFFER_ZBUFFER);
  grDepthBufferFunction (GR_CMP_ALWAYS);
  grRenderBuffer(GR_BUFFER_BACKBUFFER);
  grColorMask (FXTRUE, FXTRUE);
  grDepthMask (FXTRUE);
  grBufferClear (0, 0, 0xFFFF);
  grBufferSwap (0);
  grBufferClear (0, 0, 0xFFFF);
  grDepthMask (FXFALSE);
  grTexFilterMode (0, GR_TEXTUREFILTER_BILINEAR, GR_TEXTUREFILTER_BILINEAR);
  grTexFilterMode (1, GR_TEXTUREFILTER_BILINEAR, GR_TEXTUREFILTER_BILINEAR);
  grTexClampMode (0, GR_TEXTURECLAMP_CLAMP, GR_TEXTURECLAMP_CLAMP);
  grTexClampMode (1, GR_TEXTURECLAMP_CLAMP, GR_TEXTURECLAMP_CLAMP);
  grClipWindow (0, 0, settings.scr_res_x, settings.scr_res_y);
  rdp.update |= UPDATE_SCISSOR | UPDATE_COMBINE | UPDATE_ZBUF_ENABLED | UPDATE_CULL_MODE;

#ifdef TEXTURE_FILTER // Hiroshi Morii <koolsmoky@users.sourceforge.net>
  if (!settings.ghq_use)
  {
    settings.ghq_use = settings.ghq_fltr || settings.ghq_enht /*|| settings.ghq_cmpr*/ || settings.ghq_hirs;
    if (settings.ghq_use)
    {
      /* Plugin path */
      int options = texfltr[settings.ghq_fltr]|texenht[settings.ghq_enht]|texcmpr[settings.ghq_cmpr]|texhirs[settings.ghq_hirs];
      if (settings.ghq_enht_cmpr)
        options |= COMPRESS_TEX;
      if (settings.ghq_hirs_cmpr)
        options |= COMPRESS_HIRESTEX;
      //      if (settings.ghq_enht_tile)
      //        options |= TILE_TEX;
      if (settings.ghq_hirs_tile)
        options |= TILE_HIRESTEX;
      if (settings.ghq_enht_f16bpp)
        options |= FORCE16BPP_TEX;
      if (settings.ghq_hirs_f16bpp)
        options |= FORCE16BPP_HIRESTEX;
      if (settings.ghq_enht_gz)
        options |= GZ_TEXCACHE;
      if (settings.ghq_hirs_gz)
        options |= GZ_HIRESTEXCACHE;
      if (settings.ghq_cache_save)
        options |= (DUMP_TEXCACHE|DUMP_HIRESTEXCACHE);
      if (settings.ghq_hirs_let_texartists_fly)
        options |= LET_TEXARTISTS_FLY;
      if (settings.ghq_hirs_dump)
        options |= DUMP_TEX;

      ghq_dmptex_toggle_key = 0;

      swprintf(romname, sizeof(romname) / sizeof(*romname), L"%hs", rdp.RomName);
      swprintf(foldername, sizeof(foldername) / sizeof(*foldername), L"%hs", ConfigGetUserDataPath());
      swprintf(cachename, sizeof(cachename) / sizeof(*cachename), L"%hs", ConfigGetUserCachePath());

      settings.ghq_use = (int)ext_ghq_init(voodoo.max_tex_size, // max texture width supported by hardware
        voodoo.max_tex_size, // max texture height supported by hardware
        voodoo.sup_32bit_tex?32:16, // max texture bpp supported by hardware
        options,
        settings.ghq_cache_size * 1024*1024, // cache texture to system memory
        foldername,
        cachename,
        romname, // name of ROM. must be no longer than 256 characters
        DisplayLoadProgress);
    }
  }
  if (settings.ghq_use && strstr (extensions, "TEXMIRROR"))
    voodoo.sup_mirroring = 1;
#endif

  return TRUE;
}

void ReleaseGfx ()
{
  VLOG("ReleaseGfx ()\n");

  // Restore gamma settings
  if (voodoo.gamma_correction)
  {
    if (voodoo.gamma_table_r)
      grLoadGammaTable(voodoo.gamma_table_size, voodoo.gamma_table_r, voodoo.gamma_table_g, voodoo.gamma_table_b);
    else
      guGammaCorrectionRGB(1.3f, 1.3f, 1.3f); //1.3f is default 3dfx gamma for everything but desktop
    voodoo.gamma_correction = 0;
  }

  // Release graphics
  grSstWinClose (gfx_context);

  // Shutdown glide
  grGlideShutdown();

  fullscreen = FALSE;
  rdp.window_changed = TRUE;
}

// new API code begins here!

#ifdef __cplusplus
extern "C" {
#endif

EXPORT void CALL ReadScreen2(void *dest, int *width, int *height, int front)
{
  VLOG("CALL ReadScreen2 ()\n");
  *width = settings.res_x;
  *height = settings.res_y;
  if (dest)
  {
    BYTE * line = (BYTE*)dest;
    if (!fullscreen)
    {
      for (wxUint32 y=0; y<settings.res_y; y++)
      {
        for (wxUint32 x=0; x<settings.res_x; x++)
        {
          line[x*3] = 0x20;
          line[x*3+1] = 0x7f;
          line[x*3+2] = 0x40;
        }
      }
      // LOG ("ReadScreen. not in the fullscreen!\n");
      WARNLOG("[Glide64] Cannot save screenshot in windowed mode?\n");

      return;
    }

  GrLfbInfo_t info;
  info.size = sizeof(GrLfbInfo_t);
  if (grLfbLock (GR_LFB_READ_ONLY,
    GR_BUFFER_FRONTBUFFER,
    GR_LFBWRITEMODE_888,
    GR_ORIGIN_UPPER_LEFT,
    FXFALSE,
    &info))
  {
    // Copy the screen, let's hope this works.
      for (wxUint32 y=0; y<settings.res_y; y++)
      {
        BYTE *ptr = (BYTE*) info.lfbPtr + (info.strideInBytes * y);
        for (wxUint32 x=0; x<settings.res_x; x++)
        {
#ifdef USE_GLES
          // GLESv2 only guarantees support for GL_RGBA pixel format
          line[x*3]   = ptr[0];  // red
          line[x*3+1] = ptr[1];  // green
          line[x*3+2] = ptr[2];  // blue
#else
          // OpenGL guarantees support for GL_BGRA pixel format
          line[x*3]   = ptr[2];  // red
          line[x*3+1] = ptr[1];  // green
          line[x*3+2] = ptr[0];  // blue
#endif
          ptr += 4;
        }
        line += settings.res_x * 3;
      }

      // Unlock the frontbuffer
      grLfbUnlock (GR_LFB_READ_ONLY, GR_BUFFER_FRONTBUFFER);
    }
    VLOG ("ReadScreen. Success.\n");
  }
}

EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle CoreLibHandle, void *Context,
                                   void (*DebugCallback)(void *, int, const char *))
{
  VLOG("CALL PluginStartup ()\n");
    l_DebugCallback = DebugCallback;
    l_DebugCallContext = Context;

    /* attach and call the CoreGetAPIVersions function, check Config and Video Extension API versions for compatibility */
    ptr_CoreGetAPIVersions CoreAPIVersionFunc;
    CoreAPIVersionFunc = (ptr_CoreGetAPIVersions) osal_dynlib_getproc(CoreLibHandle, "CoreGetAPIVersions");
    if (CoreAPIVersionFunc == NULL)
    {
        ERRLOG("Core emulator broken; no CoreAPIVersionFunc() function found.");
        return M64ERR_INCOMPATIBLE;
    }
    int ConfigAPIVersion, DebugAPIVersion, VidextAPIVersion;
    (*CoreAPIVersionFunc)(&ConfigAPIVersion, &DebugAPIVersion, &VidextAPIVersion, NULL);
    if ((ConfigAPIVersion & 0xffff0000) != (CONFIG_API_VERSION & 0xffff0000))
    {
        ERRLOG("Emulator core Config API incompatible with this plugin");
        return M64ERR_INCOMPATIBLE;
    }
    if ((VidextAPIVersion & 0xffff0000) != (VIDEXT_API_VERSION & 0xffff0000))
    {
        ERRLOG("Emulator core Video Extension API incompatible with this plugin");
        return M64ERR_INCOMPATIBLE;
    }

    ConfigOpenSection = (ptr_ConfigOpenSection) osal_dynlib_getproc(CoreLibHandle, "ConfigOpenSection");
    ConfigSetParameter = (ptr_ConfigSetParameter) osal_dynlib_getproc(CoreLibHandle, "ConfigSetParameter");
    ConfigGetParameter = (ptr_ConfigGetParameter) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParameter");
    ConfigSetDefaultInt = (ptr_ConfigSetDefaultInt) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultInt");
    ConfigSetDefaultFloat = (ptr_ConfigSetDefaultFloat) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultFloat");
    ConfigSetDefaultBool = (ptr_ConfigSetDefaultBool) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultBool");
    ConfigSetDefaultString = (ptr_ConfigSetDefaultString) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultString");
    ConfigGetParamInt = (ptr_ConfigGetParamInt) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamInt");
    ConfigGetParamFloat = (ptr_ConfigGetParamFloat) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamFloat");
    ConfigGetParamBool = (ptr_ConfigGetParamBool) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamBool");
    ConfigGetParamString = (ptr_ConfigGetParamString) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamString");

    ConfigGetSharedDataFilepath = (ptr_ConfigGetSharedDataFilepath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetSharedDataFilepath");
    ConfigGetUserConfigPath = (ptr_ConfigGetUserConfigPath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserConfigPath");
    ConfigGetUserDataPath = (ptr_ConfigGetUserDataPath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserDataPath");
    ConfigGetUserCachePath = (ptr_ConfigGetUserCachePath) osal_dynlib_getproc(CoreLibHandle, "ConfigGetUserCachePath");

    if (!ConfigOpenSection   || !ConfigSetParameter    || !ConfigGetParameter ||
        !ConfigSetDefaultInt || !ConfigSetDefaultFloat || !ConfigSetDefaultBool || !ConfigSetDefaultString ||
        !ConfigGetParamInt   || !ConfigGetParamFloat   || !ConfigGetParamBool   || !ConfigGetParamString ||
        !ConfigGetSharedDataFilepath || !ConfigGetUserConfigPath || !ConfigGetUserDataPath || !ConfigGetUserCachePath)
    {
        ERRLOG("Couldn't connect to Core configuration functions");
        return M64ERR_INCOMPATIBLE;
    }

    /* Get the core Video Extension function pointers from the library handle */
    CoreVideo_Init = (ptr_VidExt_Init) osal_dynlib_getproc(CoreLibHandle, "VidExt_Init");
    CoreVideo_Quit = (ptr_VidExt_Quit) osal_dynlib_getproc(CoreLibHandle, "VidExt_Quit");
    CoreVideo_ListFullscreenModes = (ptr_VidExt_ListFullscreenModes) osal_dynlib_getproc(CoreLibHandle, "VidExt_ListFullscreenModes");
    CoreVideo_SetVideoMode = (ptr_VidExt_SetVideoMode) osal_dynlib_getproc(CoreLibHandle, "VidExt_SetVideoMode");
    CoreVideo_SetCaption = (ptr_VidExt_SetCaption) osal_dynlib_getproc(CoreLibHandle, "VidExt_SetCaption");
    CoreVideo_ToggleFullScreen = (ptr_VidExt_ToggleFullScreen) osal_dynlib_getproc(CoreLibHandle, "VidExt_ToggleFullScreen");
    CoreVideo_ResizeWindow = (ptr_VidExt_ResizeWindow) osal_dynlib_getproc(CoreLibHandle, "VidExt_ResizeWindow");
    CoreVideo_GL_GetProcAddress = (ptr_VidExt_GL_GetProcAddress) osal_dynlib_getproc(CoreLibHandle, "VidExt_GL_GetProcAddress");
    CoreVideo_GL_SetAttribute = (ptr_VidExt_GL_SetAttribute) osal_dynlib_getproc(CoreLibHandle, "VidExt_GL_SetAttribute");
    CoreVideo_GL_SwapBuffers = (ptr_VidExt_GL_SwapBuffers) osal_dynlib_getproc(CoreLibHandle, "VidExt_GL_SwapBuffers");

    if (!CoreVideo_Init || !CoreVideo_Quit || !CoreVideo_ListFullscreenModes || !CoreVideo_SetVideoMode ||
        !CoreVideo_SetCaption || !CoreVideo_ToggleFullScreen || !CoreVideo_ResizeWindow || !CoreVideo_GL_GetProcAddress ||
        !CoreVideo_GL_SetAttribute || !CoreVideo_GL_SwapBuffers)
    {
        ERRLOG("Couldn't connect to Core video functions");
        return M64ERR_INCOMPATIBLE;
    }

    const char *configDir = ConfigGetSharedDataFilepath("Glide64mk2.ini");
    if (configDir)
    {
        SetConfigDir(configDir);
        ReadSettings();
		return M64ERR_SUCCESS;
    }
    else
    {
        ERRLOG("Couldn't find Glide64mk2.ini");
        return M64ERR_FILES;
    }
}

EXPORT m64p_error CALL PluginShutdown(void)
{
  VLOG("CALL PluginShutdown ()\n");
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type *PluginType, int *PluginVersion, int *APIVersion, const char **PluginNamePtr, int *Capabilities)
{
  VLOG("CALL PluginGetVersion ()\n");
    /* set version info */
    if (PluginType != NULL)
        *PluginType = M64PLUGIN_GFX;

    if (PluginVersion != NULL)
        *PluginVersion = PLUGIN_VERSION;

    if (APIVersion != NULL)
        *APIVersion = VIDEO_PLUGIN_API_VERSION;

    if (PluginNamePtr != NULL)
        *PluginNamePtr = PLUGIN_NAME;

    if (Capabilities != NULL)
    {
        *Capabilities = 0;
    }

    return M64ERR_SUCCESS;
}

/******************************************************************
Function: CaptureScreen
Purpose:  This function dumps the current frame to a file
input:    pointer to the directory to save the file to
output:   none
*******************************************************************/
EXPORT void CALL CaptureScreen ( char * Directory )
{
  capture_screen = 1;
  strcpy (capture_path, Directory);
}

/******************************************************************
Function: ChangeWindow
Purpose:  to change the window between fullscreen and window
mode. If the window was in fullscreen this should
change the screen to window mode and vice vesa.
input:    none
output:   none
*******************************************************************/
//#warning ChangeWindow unimplemented
EXPORT void CALL ChangeWindow (void)
{
  VLOG ("ChangeWindow()\n");
}

/******************************************************************
Function: CloseDLL
Purpose:  This function is called when the emulator is closing
down allowing the dll to de-initialise.
input:    none
output:   none
*******************************************************************/
void CALL CloseDLL (void)
{
  VLOG ("CloseDLL ()\n");

#ifdef ALTTAB_FIX
  if (hhkLowLevelKybd)
  {
    UnhookWindowsHookEx(hhkLowLevelKybd);
    hhkLowLevelKybd = 0;
  }
#endif

  //CLOSELOG ();

#ifdef TEXTURE_FILTER // Hiroshi Morii <koolsmoky@users.sourceforge.net>
  if (settings.ghq_use)
  {
    ext_ghq_shutdown();
    settings.ghq_use = 0;
  }
#endif
  if (fullscreen)
    ReleaseGfx ();
  ZLUT_release();
  ClearCache ();
  delete[] voodoo.gamma_table_r;
  voodoo.gamma_table_r = 0;
  delete[] voodoo.gamma_table_g;
  voodoo.gamma_table_g = 0;
  delete[] voodoo.gamma_table_b;
  voodoo.gamma_table_b = 0;
}

/******************************************************************
Function: DllTest
Purpose:  This function is optional function that is provided
to allow the user to test the dll
input:    a handle to the window that calls this function
output:   none
*******************************************************************/
void CALL DllTest ( HWND hParent )
{
}

/******************************************************************
Function: DrawScreen
Purpose:  This function is called when the emulator receives a
WM_PAINT message. This allows the gfx to fit in when
it is being used in the desktop.
input:    none
output:   none
*******************************************************************/
void CALL DrawScreen (void)
{
  VLOG ("DrawScreen ()\n");
}

/******************************************************************
Function: GetDllInfo
Purpose:  This function allows the emulator to gather information
about the dll by filling in the PluginInfo structure.
input:    a pointer to a PLUGIN_INFO stucture that needs to be
filled by the function. (see def above)
output:   none
*******************************************************************/
void CALL GetDllInfo ( PLUGIN_INFO * PluginInfo )
{
  VLOG ("GetDllInfo ()\n");
  PluginInfo->Version = 0x0103;     // Set to 0x0103
  PluginInfo->Type  = PLUGIN_TYPE_GFX;  // Set to PLUGIN_TYPE_GFX
  sprintf (PluginInfo->Name, "Glide64mk2 " G64_VERSION);  // Name of the DLL

  // If DLL supports memory these memory options then set them to TRUE or FALSE
  //  if it does not support it
  PluginInfo->NormalMemory = TRUE;  // a normal wxUint8 array
  PluginInfo->MemoryBswaped = TRUE; // a normal wxUint8 array where the memory has been pre
  // bswap on a dword (32 bits) boundry
}

#ifndef _WIN32
BOOL WINAPI QueryPerformanceCounter(PLARGE_INTEGER counter)
{
   struct timeval tv;

   /* generic routine */
   gettimeofday( &tv, NULL );
   counter->QuadPart = (LONGLONG)tv.tv_usec + (LONGLONG)tv.tv_sec * 1000000;
   return TRUE;
}

BOOL WINAPI QueryPerformanceFrequency(PLARGE_INTEGER frequency)
{
   frequency->s.LowPart= 1000000;
   frequency->s.HighPart= 0;
   return TRUE;
}
#endif

/******************************************************************
Function: InitiateGFX
Purpose:  This function is called when the DLL is started to give
information from the emulator that the n64 graphics
uses. This is not called from the emulation thread.
Input:    Gfx_Info is passed to this function which is defined
above.
Output:   TRUE on success
FALSE on failure to initialise

** note on interrupts **:
To generate an interrupt set the appropriate bit in MI_INTR_REG
and then call the function CheckInterrupts to tell the emulator
that there is a waiting interrupt.
*******************************************************************/

EXPORT int CALL InitiateGFX (GFX_INFO Gfx_Info)
{
  VLOG ("InitiateGFX (*)\n");
  voodoo.num_tmu = 2;

  // Assume scale of 1 for debug purposes
  rdp.scale_x = 1.0f;
  rdp.scale_y = 1.0f;

  memset (&settings, 0, sizeof(SETTINGS));
  ReadSettings ();
  char name[21] = "DEFAULT";
  ReadSpecialSettings (name);
  settings.res_data_org = settings.res_data;
#ifdef FPS
  QueryPerformanceFrequency (&perf_freq);
  QueryPerformanceCounter (&fps_last);
#endif

  debug_init ();    // Initialize debugger

  gfx = Gfx_Info;

  util_init ();
  math_init ();
  TexCacheInit ();
  CRC_BuildTable();
  CountCombine();
  if (fb_depth_render_enabled)
    ZLUT_init();

  char strConfigWrapperExt[] = "grConfigWrapperExt";
  GRCONFIGWRAPPEREXT grConfigWrapperExt = (GRCONFIGWRAPPEREXT)grGetProcAddress(strConfigWrapperExt);
  if (grConfigWrapperExt)
    grConfigWrapperExt(settings.wrpResolution, settings.wrpVRAM * 1024 * 1024, settings.wrpFBO, settings.wrpAnisotropic);

  grGlideInit ();
  grSstSelect (0);
  const char *extensions = grGetString (GR_EXTENSION);
  grGlideShutdown ();
  if (strstr (extensions, "EVOODOO"))
  {
    evoodoo = 1;
    voodoo.has_2mb_tex_boundary = 0;
  }
  else {
    evoodoo = 0;
    voodoo.has_2mb_tex_boundary = 1;
  }

  return TRUE;
}

/******************************************************************
Function: MoveScreen
Purpose:  This function is called in response to the emulator
receiving a WM_MOVE passing the xpos and ypos passed
from that message.
input:    xpos - the x-coordinate of the upper-left corner of the
client area of the window.
ypos - y-coordinate of the upper-left corner of the
client area of the window.
output:   none
*******************************************************************/
EXPORT void CALL MoveScreen (int xpos, int ypos)
{
  rdp.window_changed = TRUE;
}

/******************************************************************
Function: ResizeVideoOutput
Purpose:  This function is called to force us to resize our output OpenGL window.
          This is currently unsupported, and should never be called because we do
          not pass the RESIZABLE flag to VidExt_SetVideoMode when initializing.
input:    new width and height
output:   none
*******************************************************************/
EXPORT void CALL ResizeVideoOutput(int Width, int Height)
{
}

/******************************************************************
Function: RomClosed
Purpose:  This function is called when a rom is closed.
input:    none
output:   none
*******************************************************************/
EXPORT void CALL RomClosed (void)
{
  VLOG ("RomClosed ()\n");

  CLOSE_RDP_LOG ();
  CLOSE_RDP_E_LOG ();
  rdp.window_changed = TRUE;
  romopen = FALSE;
  if (fullscreen && evoodoo)
    ReleaseGfx ();
}

static void CheckDRAMSize()
{
  wxUint32 test;
  GLIDE64_TRY
  {
    test = gfx.RDRAM[0x007FFFFF] + 1;
  }
  GLIDE64_CATCH
  {
    test = 0;
  }
  if (test)
    BMASK = 0x7FFFFF;
  else
    BMASK = WMASK;
#ifdef LOGGING
  sprintf (out_buf, "Detected RDRAM size: %08lx\n", BMASK);
  LOG (out_buf);
#endif
}

/******************************************************************
Function: RomOpen
Purpose:  This function is called when a rom is open. (from the
emulation thread)
input:    none
output:   none
*******************************************************************/
EXPORT int CALL RomOpen (void)
{
  VLOG ("RomOpen ()\n");
  no_dlist = true;
  romopen = TRUE;
  ucode_error_report = TRUE;	// allowed to report ucode errors
  rdp_reset ();

  /* cxd4 -- Glide64 tries to predict PAL scaling based on the ROM header. */
  region = OS_TV_TYPE_NTSC; /* Invalid region codes are probably NTSC betas. */
  switch (gfx.HEADER[BYTEADDR(0x3E)])
  {
     case 'A': /* generic NTSC, not documented, used by 1080 Snowboarding */
        region = OS_TV_TYPE_NTSC; break;
     case 'B': /* Brazilian */
        region = OS_TV_TYPE_MPAL; break;
     case 'C': /* Chinese */
        region = OS_TV_TYPE_NTSC; break;
     case 'D': /* German */
        region = OS_TV_TYPE_PAL ; break;
     case 'E': /* North America */
        region = OS_TV_TYPE_NTSC; break;
     case 'F': /* French */
        region = OS_TV_TYPE_PAL ; break;
     case 'G': /* Gateway 64 (NTSC) */
        region = OS_TV_TYPE_NTSC; break;
     case 'H': /* Dutch */
        region = OS_TV_TYPE_PAL ; break;
     case 'I': /* Italian */
        region = OS_TV_TYPE_PAL ; break;
     case 'J': /* Japanese */
        region = OS_TV_TYPE_NTSC; break;
     case 'K': /* Korean */
        region = OS_TV_TYPE_NTSC; break;
     case 'L': /* Gateway 64 (PAL) */
        region = OS_TV_TYPE_PAL ; break;
     case 'N': /* Canadian */
        region = OS_TV_TYPE_NTSC; break;
     case 'P': /* European (basic spec.) */
        region = OS_TV_TYPE_PAL ; break;
     case 'S': /* Spanish */
        region = OS_TV_TYPE_PAL ; break;
     case 'U': /* Australian */
        region = OS_TV_TYPE_PAL ; break;
     case 'W': /* Scandinavian */
        region = OS_TV_TYPE_PAL ; break;
     case 'X': case 'Y': case 'Z': /* documented "others", always PAL I think? */
        region = OS_TV_TYPE_PAL ; break;
  }

#ifdef USE_FRAMESKIPPER
  frameSkipper.setTargetFPS(region == OS_TV_TYPE_PAL ? 50 : 60);
#endif

  char name[21] = "DEFAULT";
  ReadSpecialSettings (name);

  // get the name of the ROM
  for (int i=0; i<20; i++)
    name[i] = gfx.HEADER[BYTEADDR(32+i)];
  name[20] = 0;

  // remove all trailing spaces
  while (name[strlen(name)-1] == ' ')
    name[strlen(name)-1] = 0;

  strncpy(rdp.RomName, name, sizeof(rdp.RomName));
  ReadSpecialSettings (name);
  ClearCache ();

  CheckDRAMSize();

  OPEN_RDP_LOG ();
  OPEN_RDP_E_LOG ();


  // ** EVOODOO EXTENSIONS **
  if (!fullscreen)
  {
    grGlideInit ();
    grSstSelect (0);
  }
  const char *extensions = grGetString (GR_EXTENSION);
  if (!fullscreen)
  {
    grGlideShutdown ();

    if (strstr (extensions, "EVOODOO"))
      evoodoo = 1;
    else
      evoodoo = 0;

    if (evoodoo)
      InitGfx ();
  }

  if (strstr (extensions, "ROMNAME"))
  {
    char strSetRomName[] = "grSetRomName";
    void (FX_CALL *grSetRomName)(char*);
    grSetRomName = (void (FX_CALL *)(char*))grGetProcAddress (strSetRomName);
    grSetRomName (name);
  }
  // **
  return true;
}

/******************************************************************
Function: ShowCFB
Purpose:  Useally once Dlists are started being displayed, cfb is
ignored. This function tells the dll to start displaying
them again.
input:    none
output:   none
*******************************************************************/
bool no_dlist = true;
EXPORT void CALL ShowCFB (void)
{
  no_dlist = true;
  VLOG ("ShowCFB ()\n");
}

EXPORT void CALL SetRenderingCallback(void (*callback)(int))
{
  VLOG("CALL SetRenderingCallback (*)\n");
    renderCallback = callback;
}

void drawViRegBG()
{
  LRDP("drawViRegBG\n");
  const wxUint32 VIwidth = *gfx.VI_WIDTH_REG;
  FB_TO_SCREEN_INFO fb_info;
  fb_info.width  = VIwidth;
  fb_info.height = (wxUint32)rdp.vi_height;
  if (fb_info.height == 0)
  {
    LRDP("Image height = 0 - skipping\n");
    return;
  }
  fb_info.ul_x = 0;

  fb_info.lr_x = VIwidth - 1;
  //  fb_info.lr_x = (wxUint32)rdp.vi_width - 1;
  fb_info.ul_y = 0;
  fb_info.lr_y = fb_info.height - 1;
  fb_info.opaque = 1;
  fb_info.addr = *gfx.VI_ORIGIN_REG;
  fb_info.size = *gfx.VI_STATUS_REG & 3;
  rdp.last_bg = fb_info.addr;

  bool drawn = DrawFrameBufferToScreen(fb_info);
  if (settings.hacks&hack_Lego && drawn)
  {
    rdp.updatescreen = 1;
    newSwapBuffers ();
    DrawFrameBufferToScreen(fb_info);
  }
}

}

void drawNoFullscreenMessage();

void DrawFrameBuffer ()
{
  if (!fullscreen)
  {
    drawNoFullscreenMessage();
  }
  if (to_fullscreen)
    GoToFullScreen();

  if (fullscreen)
  {
    grDepthMask (FXTRUE);
    grColorMask (FXTRUE, FXTRUE);
    grBufferClear (0, 0, 0xFFFF);
    drawViRegBG();
  }
}

extern "C" {
/******************************************************************
Function: UpdateScreen
Purpose:  This function is called in response to a vsync of the
screen were the VI bit in MI_INTR_REG has already been
set
input:    none
output:   none
*******************************************************************/
wxUint32 update_screen_count = 0;
EXPORT void CALL UpdateScreen (void)
{
#ifdef USE_FRAMESKIPPER
  frameSkipper.update();
#endif
#ifdef LOG_KEY
  if (CheckKeyPressed(G64_VK_SPACE, 0x0001))
  {
    LOG ("KEY!!!\n");
  }
#endif
  char out_buf[128];
  sprintf (out_buf, "UpdateScreen (). Origin: %08x, Old origin: %08x, width: %d\n", *gfx.VI_ORIGIN_REG, rdp.vi_org_reg, *gfx.VI_WIDTH_REG);
  VLOG ("%s", out_buf);
  LRDP(out_buf);

  wxUint32 width = (*gfx.VI_WIDTH_REG) << 1;
  if (fullscreen && (*gfx.VI_ORIGIN_REG  > width))
    update_screen_count++;
//TODO-PORT: wx times
#ifdef FPS
  // vertical interrupt has occurred, increment counter
  vi_count ++;

  // Check frames per second
  LARGE_INTEGER difference;
  QueryPerformanceCounter (&fps_next);
  difference.QuadPart = fps_next.QuadPart - fps_last.QuadPart;
  float diff_secs = (float)((double)difference.QuadPart / (double)perf_freq.QuadPart);
  if (diff_secs > 0.5f)
  {
    fps = (float)(fps_count / diff_secs);
    vi = (float)(vi_count / diff_secs);
    fps_last = fps_next;
    fps_count = 0;
    vi_count = 0;
  }
#endif
  //*
  wxUint32 limit = (settings.hacks&hack_Lego) ? 15 : 30;
  if ((settings.frame_buffer&fb_cpu_write_hack) && (update_screen_count > limit) && (rdp.last_bg == 0))
  {
    LRDP("DirectCPUWrite hack!\n");
    update_screen_count = 0;
    no_dlist = true;
    ClearCache ();
    UpdateScreen();
    return;
  }
  //*/
  //*
  if( no_dlist )
  {
    if( *gfx.VI_ORIGIN_REG  > width )
    {
      ChangeSize ();
      LRDP("ChangeSize done\n");
      DrawFrameBuffer();
      LRDP("DrawFrameBuffer done\n");
      rdp.updatescreen = 1;
      newSwapBuffers ();
    }
    return;
  }
  //*/
  if (settings.swapmode == 0)
    newSwapBuffers ();
}

static void DrawWholeFrameBufferToScreen()
{
  static wxUint32 toScreenCI = 0;
  if (rdp.ci_width < 200)
    return;
  if (rdp.cimg == toScreenCI)
    return;
  toScreenCI = rdp.cimg;
  FB_TO_SCREEN_INFO fb_info;
  fb_info.addr   = rdp.cimg;
  fb_info.size   = rdp.ci_size;
  fb_info.width  = rdp.ci_width;
  fb_info.height = rdp.ci_height;
  if (fb_info.height == 0)
    return;
  fb_info.ul_x = 0;
  fb_info.lr_x = rdp.ci_width-1;
  fb_info.ul_y = 0;
  fb_info.lr_y = rdp.ci_height-1;
  fb_info.opaque = 0;
  DrawFrameBufferToScreen(fb_info);
  if (!(settings.frame_buffer & fb_ref))
    memset(gfx.RDRAM+rdp.cimg, 0, (rdp.ci_width*rdp.ci_height)<<rdp.ci_size>>1);
}

static void GetGammaTable()
{
  char strGetGammaTableExt[] = "grGetGammaTableExt";
  void (FX_CALL *grGetGammaTableExt)(FxU32, FxU32*, FxU32*, FxU32*) =
    (void (FX_CALL *)(FxU32, FxU32*, FxU32*, FxU32*))grGetProcAddress(strGetGammaTableExt);
  if (grGetGammaTableExt)
  {
    voodoo.gamma_table_r = new FxU32[voodoo.gamma_table_size];
    voodoo.gamma_table_g = new FxU32[voodoo.gamma_table_size];
    voodoo.gamma_table_b = new FxU32[voodoo.gamma_table_size];
    grGetGammaTableExt(voodoo.gamma_table_size, voodoo.gamma_table_r, voodoo.gamma_table_g, voodoo.gamma_table_b);
  }
}

}
wxUint32 curframe = 0;
void newSwapBuffers()
{
  if (!rdp.updatescreen)
    return;

  rdp.updatescreen = 0;

  LRDP("swapped\n");

  // Allow access to the whole screen
  if (fullscreen)
  {
    rdp.update |= UPDATE_SCISSOR | UPDATE_COMBINE | UPDATE_ZBUF_ENABLED | UPDATE_CULL_MODE;
    grClipWindow (0, 0, settings.scr_res_x, settings.scr_res_y);
    grDepthBufferFunction (GR_CMP_ALWAYS);
    grDepthMask (FXFALSE);
    grCullMode (GR_CULL_DISABLE);

    if ((settings.show_fps & 0xF) || settings.clock)
      set_message_combiner ();
#ifdef FPS
    float y = (float)settings.res_y;
    if (settings.show_fps & 0x0F)
    {
      if (settings.show_fps & 4)
      {
        const float percentage = vi / (region == OS_TV_TYPE_PAL ? .5f : .6f); /* PAL is 50Hz; NTSC & MPAL are 60Hz */

        output(0, y, 0, "%d%% ", (int)percentage);
        y -= 16;
      }
      if (settings.show_fps & 2)
      {
        output (0, y, 0, "VI/s: %.02f ", vi);
        y -= 16;
      }
      if (settings.show_fps & 1)
        output (0, y, 0, "FPS: %.02f ", fps);
    }
#endif

    if (settings.clock)
    {
      if (settings.clock_24_hr)
      {
          time_t ltime;
          time (&ltime);
          tm *cur_time = localtime (&ltime);

          sprintf (out_buf, "%.2d:%.2d:%.2d", cur_time->tm_hour, cur_time->tm_min, cur_time->tm_sec);
      }
      else
      {
          char ampm[] = "AM";
          time_t ltime;

          time (&ltime);
          tm *cur_time = localtime (&ltime);

          if (cur_time->tm_hour >= 12)
          {
            strcpy (ampm, "PM");
            if (cur_time->tm_hour != 12)
              cur_time->tm_hour -= 12;
          }
          if (cur_time->tm_hour == 0)
            cur_time->tm_hour = 12;

          if (cur_time->tm_hour >= 10)
            sprintf (out_buf, "%.5s %s", asctime(cur_time) + 11, ampm);
          else
            sprintf (out_buf, " %.4s %s", asctime(cur_time) + 12, ampm);
        }
        output ((float)(settings.res_x - 68), y, 0, "%s", out_buf);
      }
    //hotkeys
    if (CheckKeyPressed(G64_VK_BACK, 0x0001))
    {
      hotkey_info.hk_filtering = 100;
      if (settings.filtering < 2)
        settings.filtering++;
      else
        settings.filtering = 0;
    }
    if ((abs((int)(frame_count - curframe)) > 3 ) && CheckKeyPressed(G64_VK_ALT, 0x8000))  //alt +
    {
      if (CheckKeyPressed(G64_VK_B, 0x8000))  //b
      {
        hotkey_info.hk_motionblur = 100;
        hotkey_info.hk_ref = 0;
        curframe = frame_count;
        settings.frame_buffer ^= fb_motionblur;
      }
      else if (CheckKeyPressed(G64_VK_V, 0x8000))  //v
      {
        hotkey_info.hk_ref = 100;
        hotkey_info.hk_motionblur = 0;
        curframe = frame_count;
        settings.frame_buffer ^= fb_ref;
      }
    }
    if (settings.buff_clear && (hotkey_info.hk_ref || hotkey_info.hk_motionblur || hotkey_info.hk_filtering))
    {
      set_message_combiner ();
      char buf[256];
      buf[0] = 0;
      char * message = 0;
      if (hotkey_info.hk_ref)
      {
        if (settings.frame_buffer & fb_ref)
          message = strcat(buf, "FB READ ALWAYS: ON");
        else
          message = strcat(buf, "FB READ ALWAYS: OFF");
        hotkey_info.hk_ref--;
      }
      if (hotkey_info.hk_motionblur)
      {
        if (settings.frame_buffer & fb_motionblur)
          message = strcat(buf, "  MOTION BLUR: ON");
        else
          message = strcat(buf, "  MOTION BLUR: OFF");
        hotkey_info.hk_motionblur--;
      }
      if (hotkey_info.hk_filtering)
      {
        switch (settings.filtering)
        {
        case 0:
          message = strcat(buf, "  FILTERING MODE: AUTOMATIC");
          break;
        case 1:
          message = strcat(buf, "  FILTERING MODE: FORCE BILINEAR");
          break;
        case 2:
          message = strcat(buf, "  FILTERING MODE: FORCE POINT-SAMPLED");
          break;
        }
        hotkey_info.hk_filtering--;
      }
      output (120.0f, (float)settings.res_y, 0, "%s", message);
    }
  }

  // Capture the screen if debug capture is set
  if (_debugger.capture)
  {
    // Allocate the screen
    _debugger.screen = new wxUint8 [(settings.res_x*settings.res_y) << 1];

    // Lock the backbuffer (already rendered)
    GrLfbInfo_t info;
    info.size = sizeof(GrLfbInfo_t);
    while (!grLfbLock (GR_LFB_READ_ONLY,
      GR_BUFFER_BACKBUFFER,
      GR_LFBWRITEMODE_565,
      GR_ORIGIN_UPPER_LEFT,
      FXFALSE,
      &info));

    wxUint32 offset_src=0, offset_dst=0;

    // Copy the screen
    for (wxUint32 y=0; y<settings.res_y; y++)
    {
      if (info.writeMode == GR_LFBWRITEMODE_8888)
      {
        wxUint32 *src = (wxUint32*)((wxUint8*)info.lfbPtr + offset_src);
        wxUint16 *dst = (wxUint16*)(_debugger.screen + offset_dst);
        wxUint8 r, g, b;
        wxUint32 col;
        for (unsigned int x = 0; x < settings.res_x; x++)
        {
          col = src[x];
          r = (wxUint8)((col >> 19) & 0x1F);
          g = (wxUint8)((col >> 10) & 0x3F);
          b = (wxUint8)((col >> 3)  & 0x1F);
          dst[x] = (r<<11)|(g<<5)|b;
        }
      }
      else
      {
        memcpy (_debugger.screen + offset_dst, (wxUint8*)info.lfbPtr + offset_src, settings.res_x << 1);
      }
      offset_dst += settings.res_x << 1;
      offset_src += info.strideInBytes;
    }

    // Unlock the backbuffer
    grLfbUnlock (GR_LFB_READ_ONLY, GR_BUFFER_BACKBUFFER);
  }

  if (fullscreen && debugging)
  {
    debug_keys ();
    debug_cacheviewer ();
    debug_mouse ();
  }

  if (settings.frame_buffer & fb_read_back_to_screen)
    DrawWholeFrameBufferToScreen();

  if (fullscreen)
  {
    if (fb_hwfbe_enabled && !(settings.hacks&hack_RE2) && !evoodoo)
      grAuxBufferExt( GR_BUFFER_AUXBUFFER );
    grBufferSwap (settings.vsync);
    fps_count ++;
    if (*gfx.VI_STATUS_REG&0x08) //gamma correction is used
    {
      if (!voodoo.gamma_correction)
      {
        if (voodoo.gamma_table_size && !voodoo.gamma_table_r)
          GetGammaTable(); //save initial gamma tables
        guGammaCorrectionRGB(2.0f, 2.0f, 2.0f); //with gamma=2.0 gamma table is the same, as in N64
        voodoo.gamma_correction = 1;
      }
    }
    else
    {
      if (voodoo.gamma_correction)
      {
        if (voodoo.gamma_table_r)
          grLoadGammaTable(voodoo.gamma_table_size, voodoo.gamma_table_r, voodoo.gamma_table_g, voodoo.gamma_table_b);
        else
          guGammaCorrectionRGB(1.3f, 1.3f, 1.3f); //1.3f is default 3dfx gamma for everything but desktop
        voodoo.gamma_correction = 0;
      }
    }
  }

  if (_debugger.capture)
    debug_capture ();

  if (fullscreen)
  {
    if  (debugging || settings.wireframe || settings.buff_clear || (settings.hacks&hack_PPL && settings.ucode == 6))
    {
      if (settings.hacks&hack_RE2 && fb_depth_render_enabled)
        grDepthMask (FXFALSE);
      else
        grDepthMask (FXTRUE);
      grBufferClear (0, 0, 0xFFFF);
    }
    /* //let the game to clear the buffers
    else
    {
    grDepthMask (FXTRUE);
    grColorMask (FXFALSE, FXFALSE);
    grBufferClear (0, 0, 0xFFFF);
    grColorMask (FXTRUE, FXTRUE);
    }
    */
  }

  if (settings.frame_buffer & fb_read_back_to_screen2)
    DrawWholeFrameBufferToScreen();

  frame_count ++;

  // Open/close debugger?
  if (CheckKeyPressed(G64_VK_SCROLL, 0x0001))
  {
    if (!debugging)
    {
      //if (settings.scr_res_x == 1024 && settings.scr_res_y == 768)
      {
        debugging = 1;

        // Recalculate screen size, don't resize screen
        settings.res_x = (wxUint32)(settings.scr_res_x * 0.625f);
        settings.res_y = (wxUint32)(settings.scr_res_y * 0.625f);

        ChangeSize ();
      }
    } 
    else
    {
      debugging = 0;

      settings.res_x = settings.scr_res_x;
      settings.res_y = settings.scr_res_y;

      ChangeSize ();
    }
  }

  // Debug capture?
  if (/*fullscreen && */debugging && CheckKeyPressed(G64_VK_INSERT, 0x0001))
  {
    _debugger.capture = 1;
  }
}

extern "C"
{

/******************************************************************
Function: ViStatusChanged
Purpose:  This function is called to notify the dll that the
ViStatus registers value has been changed.
input:    none
output:   none
*******************************************************************/
EXPORT void CALL ViStatusChanged (void)
{
}

/******************************************************************
Function: ViWidthChanged
Purpose:  This function is called to notify the dll that the
ViWidth registers value has been changed.
input:    none
output:   none
*******************************************************************/
EXPORT void CALL ViWidthChanged (void)
{
}

}

int CheckKeyPressed(int key, int mask)
{
static Glide64Keys g64Keys;
  if (settings.use_hotkeys == 0)
    return 0;
  if (grKeyPressed)
    return grKeyPressed(g64Keys[key]);
  return 0;
}


#ifdef ALTTAB_FIX
int k_ctl=0, k_alt=0, k_del=0;

LRESULT CALLBACK LowLevelKeyboardProc(int nCode,
                                      WPARAM wParam, LPARAM lParam)
{
  if (!fullscreen) return CallNextHookEx(NULL, nCode, wParam, lParam);

  int TabKey = FALSE;

  PKBDLLHOOKSTRUCT p;

  if (nCode == HC_ACTION)
  {
    switch (wParam) {
case WM_KEYUP:    case WM_SYSKEYUP:
  p = (PKBDLLHOOKSTRUCT) lParam;
  if (p->vkCode == 162) k_ctl = 0;
  if (p->vkCode == 164) k_alt = 0;
  if (p->vkCode == 46) k_del = 0;
  goto do_it;

case WM_KEYDOWN:  case WM_SYSKEYDOWN:
  p = (PKBDLLHOOKSTRUCT) lParam;
  if (p->vkCode == 162) k_ctl = 1;
  if (p->vkCode == 164) k_alt = 1;
  if (p->vkCode == 46) k_del = 1;
  goto do_it;

do_it:
  TabKey =
    ((p->vkCode == VK_TAB) && ((p->flags & LLKHF_ALTDOWN) != 0)) ||
    ((p->vkCode == VK_ESCAPE) && ((p->flags & LLKHF_ALTDOWN) != 0)) ||
    ((p->vkCode == VK_ESCAPE) && ((GetKeyState(VK_CONTROL) & 0x8000) != 0)) ||
    (k_ctl && k_alt && k_del);

  break;
    }
  }

  if (TabKey)
  {
    k_ctl = 0;
    k_alt = 0;
    k_del = 0;
    ReleaseGfx ();
  }

  return CallNextHookEx(NULL, nCode, wParam, lParam);
}
#endif

