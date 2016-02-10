/*
* Glide64 - Glide video plugin for Nintendo 64 emulators.
* Copyright (c) 2010  Jon Ring
* Copyright (c) 2002  Dave2001
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
* You should have received a copy of the GNU General Public
* Licence along with this program; if not, write to the Free
* Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
* Boston, MA  02110-1301, USA
*/
#include "Gfx_1.3.h"
#include "Config.h"
#include "m64p.h"
#include "rdp.h"
#include "libretro/libretro.h"

extern ptr_ConfigOpenSection      CoreConfig_ConfigOpenSection;
extern ptr_ConfigSetParameter     CoreConfig_ConfigSetParameter;
extern ptr_ConfigGetParameter     CoreConfig_ConfigGetParameter;
extern ptr_ConfigGetParameterHelp CoreConfig_ConfigGetParameterHelp;
extern ptr_ConfigSetDefaultInt    CoreConfig_ConfigSetDefaultInt;
extern ptr_ConfigSetDefaultFloat  CoreConfig_ConfigSetDefaultFloat;
extern ptr_ConfigSetDefaultBool   CoreConfig_ConfigSetDefaultBool;
extern ptr_ConfigSetDefaultString CoreConfig_ConfigSetDefaultString;
extern ptr_ConfigGetParamInt      CoreConfig_ConfigGetParamInt;
extern ptr_ConfigGetParamFloat    CoreConfig_ConfigGetParamFloat;
extern ptr_ConfigGetParamBool     CoreConfig_ConfigGetParamBool;
extern ptr_ConfigGetParamString   CoreConfig_ConfigGetParamString;

extern ptr_ConfigGetSharedDataFilepath CoreConfig_ConfigGetSharedDataFilepath;
extern ptr_ConfigGetUserConfigPath     CoreConfig_ConfigGetUserConfigPath;
extern ptr_ConfigGetUserDataPath       CoreConfig_ConfigGetUserDataPath;
extern ptr_ConfigGetUserCachePath      CoreConfig_ConfigGetUserCachePath;

extern retro_log_printf_t log_cb;

static m64p_handle video_general_section;
static m64p_handle video_glide2gl_section;

BOOL Config_Open()
{
    if (CoreConfig_ConfigOpenSection("Video-General", &video_general_section) != M64ERR_SUCCESS ||
          CoreConfig_ConfigOpenSection("Video-Glide2gl", &video_glide2gl_section) != M64ERR_SUCCESS)
    {
        return FALSE;
    }

    CoreConfig_ConfigSetDefaultString(video_glide2gl_section, "accuracy", "medium",
       "FX Accuracy (restart); medium|high|veryhigh|low");
    CoreConfig_ConfigSetDefaultString(video_glide2gl_section, "aspect", "normal",
       "Aspect ratio hint (reinit); normal|widescreen");
    CoreConfig_ConfigSetDefaultString(video_glide2gl_section, "filtering", "automatic",
       "Texture Filtering; automatic|N64 3-point|bilinear|nearest");
    CoreConfig_ConfigSetDefaultString(video_glide2gl_section, "polyoffset-factor", "-3.0",
       "Polygon Offset Factor; -3.0|-2.5|-2.0|-1.5|-1.0|-0.5|0.0|0.5|1.0|1.5|2.0|2.5|3.0|3.5|4.0|4.5|5.0|-3.5|-4.0|-4.5|-5.0");
    CoreConfig_ConfigSetDefaultString(video_glide2gl_section, "polyoffset-units", "-3.0",
       "Polygon Offset Factor; -3.0|-2.5|-2.0|-1.5|-1.0|-0.5|0.0|0.5|1.0|1.5|2.0|2.5|3.0|3.5|4.0|4.5|5.0|-3.5|-4.0|-4.5|-5.0");
    CoreConfig_ConfigSetDefaultString(video_glide2gl_section, "bufferswap", "on",
       "Buffer Swap; on|off");
    CoreConfig_ConfigSetDefaultString(video_glide2gl_section, "framerate", "original",
       "Framerate (restart); original|fullspeed");
    CoreConfig_ConfigSetDefaultString(video_glide2gl_section, "vcache-vbo", "ofg",
       "Vertex cache VBO (restart); off|on");

    return TRUE;
}

int Config_ReadScreenInt(const char *itemname)
{
    return CoreConfig_ConfigGetParamInt(video_general_section, itemname);
}

BOOL Config_ReadInt(const char *itemname, const char *desc, int def_value, int create, int isBoolean)
{
    if (isBoolean)
    {
       CoreConfig_ConfigSetDefaultBool(video_glide2gl_section, itemname, def_value, desc);
       return CoreConfig_ConfigGetParamBool(video_glide2gl_section, itemname);
    }
    else
    {
       CoreConfig_ConfigSetDefaultInt(video_glide2gl_section, itemname, def_value, desc);
       return CoreConfig_ConfigGetParamInt(video_glide2gl_section, itemname);
    }

}

float Config_ReadFloat(const char *itemname, const char *desc, float def_value)
{
    CoreConfig_ConfigSetDefaultFloat(video_glide2gl_section, itemname, def_value, desc);
    return CoreConfig_ConfigGetParamFloat(video_glide2gl_section, itemname);
}

const char* Config_ReadString(const char *itemname, const char *desc, const char *def_value)
{
   CoreConfig_ConfigSetDefaultString(video_glide2gl_section, itemname, def_value, desc);

   const char* readValue = CoreConfig_ConfigGetParamString(video_glide2gl_section, itemname);

   if(log_cb)
   {
      log_cb (RETRO_LOG_INFO, "Read config string %s with value %s", itemname, readValue);
   }

   return readValue;
}
