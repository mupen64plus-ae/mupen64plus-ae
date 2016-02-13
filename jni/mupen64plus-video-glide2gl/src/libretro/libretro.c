#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <SDL_opengles2.h>
#include <android/log.h>
#include <Config.h>

#include "libretro/libretro.h"
#include "libretro/libretro_memory.h"

struct retro_perf_callback perf_cb;
retro_get_cpu_features_t perf_get_cpu_features_cb = NULL;

retro_log_printf_t log_cb = NULL;
retro_video_refresh_t video_cb = NULL;
retro_input_poll_t poll_cb = NULL;
retro_input_state_t input_cb = NULL;
retro_audio_sample_batch_t audio_batch_cb = NULL;
retro_environment_t environ_cb = NULL;

struct retro_rumble_interface rumble;

save_memory_data saved_memory;

float polygonOffsetFactor;
float polygonOffsetUnits;

int astick_deadzone;
bool flip_only;

static uint8_t* game_data = NULL;
static uint32_t game_size = 0;

static bool     emu_initialized     = false;
static unsigned initial_boot        = true;
static unsigned audio_buffer_size   = 2048;

static unsigned retro_filtering     = 0;
static bool     reinit_screen       = false;
static bool     first_context_reset = false;
static bool     pushed_frame        = false;

unsigned frame_dupe = false;

extern uint32_t *blitter_buf;


enum gfx_plugin_type gfx_plugin = GFX_GLIDE64;
uint32_t gfx_plugin_accuracy = 2;
static enum rsp_plugin_type rsp_plugin;
uint32_t screen_width;
uint32_t screen_height;
uint32_t screen_pitch;
uint32_t screen_aspectmodehint;

unsigned int VI_REFRESH;
unsigned int BUFFERSWAP;
unsigned int FAKE_SDL_TICKS;

static void setup_variables(void)
{
   struct retro_variable variables[] = {
      { "mupen64-gfxplugin-accuracy",
         "GFX Accuracy (restart); medium|high|veryhigh|low" },
      { "mupen64-screensize",
         "Resolution (restart); 640x480|960x720|1280x960|1600x1200|1920x1440|2240x1680|320x240" },
      { "mupen64-aspectratiohint",
         "Aspect ratio hint (reinit); normal|widescreen" },
      { "mupen64-filtering",
                 "Texture Filtering; automatic|N64 3-point|bilinear|nearest" },
      { "mupen64-polyoffset-factor",
       "(Glide64) Polygon Offset Factor; -3.0|-2.5|-2.0|-1.5|-1.0|-0.5|0.0|0.5|1.0|1.5|2.0|2.5|3.0|3.5|4.0|4.5|5.0|-3.5|-4.0|-4.5|-5.0"
      },
      { "mupen64-polyoffset-units",
       "(Glide64) Polygon Offset Units; -3.0|-2.5|-2.0|-1.5|-1.0|-0.5|0.0|0.5|1.0|1.5|2.0|2.5|3.0|3.5|4.0|4.5|5.0|-3.5|-4.0|-4.5|-5.0"
      },
      { "mupen64-bufferswap",
         "Buffer Swap; on|off" },
      { "mupen64-vcache-vbo",
         "(Glide64) Vertex cache VBO (restart); off|on" },
      { NULL, NULL },
   };

   environ_cb(RETRO_ENVIRONMENT_SET_VARIABLES, variables);
}

void update_variables(bool startup)
{
   static float last_aspect = 4.0 / 3.0;
   struct retro_variable var;

   var.key = "mupen64-screensize";
   var.value = NULL;

   if (environ_cb(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
   {
      /* TODO/FIXME - hack - force screen width and height back to 640x480 in case
       * we change it with Angrylion. If we ever want to support variable resolution sizes in Angrylion
       * then we need to drop this. */
      if (gfx_plugin == GFX_ANGRYLION || sscanf(var.value ? var.value : "640x480", "%dx%d", &screen_width, &screen_height) != 2)
      {
         screen_width = 640;
         screen_height = 480;
      }

      if(log_cb)
      {
         log_cb (RETRO_LOG_INFO, "Resolution=%dx%d", screen_width, screen_height);
      }
   }

   if (startup)
   {

   var.key = "mupen64-filtering";
   var.value = NULL;

      if (environ_cb (RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
      {
         if (!strcmp (var.value, "automatic"))
            retro_filtering = 0;
         else if (!strcmp (var.value, "N64 3-point"))
#ifdef DISABLE_3POINT
            retro_filtering = 3;
#else
            retro_filtering = 1;
#endif
         else if (!strcmp (var.value, "nearest"))
            retro_filtering = 2;
         else if (!strcmp (var.value, "bilinear"))
            retro_filtering = 3;
         if (gfx_plugin == GFX_GLIDE64)
         {
            log_cb (RETRO_LOG_DEBUG, "set glide filtering mode\n");
            glide_set_filtering (retro_filtering);
         }
      }
   }

   if (!startup)
   {
      var.key = "mupen64-aspectratiohint";
      var.value = NULL;

      if (environ_cb(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
      {
         float aspect_val = 4.0 / 3.0;
         float aspectmode = 0;

         if (!strcmp(var.value, "widescreen"))
         {
            aspect_val = 16.0 / 9.0;
            aspectmode = 1;
         }
         else if (!strcmp(var.value, "normal"))
         {
            aspect_val = 4.0 / 3.0;
            aspectmode = 0;
         }

         if (aspect_val != last_aspect)
         {
            screen_aspectmodehint = aspectmode;

            switch (gfx_plugin)
            {
               case GFX_GLIDE64:
                  ChangeSize();
                  break;
               default:
                  break;
            }

            last_aspect = aspect_val;
            reinit_screen = true;
         }
      }
   }

   var.key = "mupen64-polyoffset-factor";
   var.value = NULL;

   if (environ_cb(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
   {
      float new_val = (float)atoi(var.value);
      polygonOffsetFactor = new_val;
   }

   var.key = "mupen64-polyoffset-units";
   var.value = NULL;

   if (environ_cb(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
   {
      float new_val = (float)atoi(var.value);
      polygonOffsetUnits = new_val;
   }

   var.key = "mupen64-gfxplugin-accuracy";
   var.value = NULL;

   if (environ_cb(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
   {
       if (var.value && !strcmp(var.value, "veryhigh"))
          gfx_plugin_accuracy = 3;
       else if (var.value && !strcmp(var.value, "high"))
          gfx_plugin_accuracy = 2;
       else if (var.value && !strcmp(var.value, "medium"))
          gfx_plugin_accuracy = 1;
       else if (var.value && !strcmp(var.value, "low"))
          gfx_plugin_accuracy = 0;
   }

   var.key = "mupen64-bufferswap";
   var.value = NULL;

   if (environ_cb(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value)
   {
      if (!strcmp(var.value, "on"))
         BUFFERSWAP = true;
      else if (!strcmp(var.value, "off"))
         BUFFERSWAP = false;
   }

   var.key = "mupen64-framerate";
   var.value = NULL;

   if (environ_cb(RETRO_ENVIRONMENT_GET_VARIABLE, &var) && var.value && initial_boot)
   {
      if (!strcmp(var.value, "original"))
         frame_dupe = false;
      else if (!strcmp(var.value, "fullspeed"))
         frame_dupe = true;
   }
}


bool emu_step_render()
{
   if (flip_only)
   {
      video_cb(RETRO_HW_FRAME_BUFFER_VALID, screen_width, screen_height, 0);

      pushed_frame = true;
      return true;
   }

   if (!pushed_frame && frame_dupe) // Dupe. Not duping violates libretro API, consider it a speedhack.
      video_cb(NULL, screen_width, screen_height, screen_pitch);

   return false;
}

void androidLog(enum retro_log_level level, const char *fmt, ...)
{
   android_LogPriority logPriority = 0;

   va_list arguments;
   va_start ( arguments, fmt );

   switch(level){
      case RETRO_LOG_DEBUG:
         logPriority = ANDROID_LOG_DEBUG;
         break;
      case RETRO_LOG_INFO:
         logPriority = ANDROID_LOG_INFO;
         break;
      case RETRO_LOG_WARN:
         logPriority = ANDROID_LOG_WARN;
         break;
      case RETRO_LOG_ERROR:
         logPriority = ANDROID_LOG_ERROR;
         break;
      default:
         break;
   }
   __android_log_vprint(logPriority, "glide2gl",fmt, arguments);

   va_end ( arguments );
}

bool environment(unsigned cmd, void *data)
{
   if(cmd == RETRO_ENVIRONMENT_GET_PERF_INTERFACE)
   {
      return false;
   }
   if(cmd == RETRO_ENVIRONMENT_SET_PIXEL_FORMAT)
   {
      return false;
   }

   if(cmd == RETRO_ENVIRONMENT_GET_VARIABLE)
   {
      struct retro_variable* var = (struct retro_variable*)data;
      static char returnData[256];
      var->value = returnData;

      if(!strcmp (var->key, "mupen64-screensize"))
      {
         int width = Config_ReadScreenInt("ScreenWidth");
         int height = Config_ReadScreenInt("ScreenHeight");
         sprintf(returnData, "%dx%d", width, height);

         if(log_cb)
         {
            log_cb (RETRO_LOG_INFO, "Setting screen resolution to %s", returnData);
         }

      }
      else if (!strcmp (var->key, "mupen64-gfxplugin-accuracy"))
      {
         const char* value = Config_ReadString("accuracy",
            "FX Accuracy (restart); medium|high|veryhigh|low", "medium");
         strcpy(returnData, value);
      }
      else if(!strcmp (var->key, "mupen64-aspectratiohint"))
      {
         const char* value = Config_ReadString("aspect",
            "Aspect ratio hint (reinit); normal|widescreen", "normal");
         strcpy(returnData, value);
      }
      else if(!strcmp (var->key, "mupen64-filtering"))
      {
         const char* value = Config_ReadString("filtering",
            "Texture Filtering; automatic|N64 3-point|bilinear|nearest", "automatic");
         strcpy(returnData, value);
      }
      else if(!strcmp (var->key, "mupen64-polyoffset-factor"))
      {
         const char* value = Config_ReadString("polyoffset-factor",
            "Polygon Offset Factor; -3.0|-2.5|-2.0|-1.5|-1.0|-0.5|0.0|0.5|1.0|1.5|2.0|2.5|3.0|3.5|4.0|4.5|5.0|-3.5|-4.0|-4.5|-5.0", "-3.0");
         strcpy(returnData, value);
      }
      else if(!strcmp (var->key, "mupen64-polyoffset-units"))
      {
         const char* value = Config_ReadString("polyoffset-units",
            "Polygon Offset Factor; -3.0|-2.5|-2.0|-1.5|-1.0|-0.5|0.0|0.5|1.0|1.5|2.0|2.5|3.0|3.5|4.0|4.5|5.0|-3.5|-4.0|-4.5|-5.0", "-3.0");
         strcpy(returnData, value);
      }
      else if(!strcmp (var->key, "mupen64-bufferswap"))
      {
         const char* value = Config_ReadString("bufferswap",
            "Buffer Swap; on|off", "on");
         strcpy(returnData, value);
      }
      else if(!strcmp (var->key, "mupen64-framerate"))
      {
         const char* value = Config_ReadString("framerate",
            "Framerate (restart); original|fullspeed", "original");
         strcpy(returnData, value);
      }
      else if(!strcmp (var->key, "mupen64-vcache-vbo"))
      {
         const char* value = Config_ReadString("vcache-vbo",
            "Vertex cache VBO (restart); off|on", "off");
         strcpy(returnData, value);
      }

      return true;
   }
   return false;
}


void retro_init(void)
{
   log_cb = androidLog;
   environ_cb = environment;

   unsigned colorMode = RETRO_PIXEL_FORMAT_XRGB8888;
   screen_pitch = 0;

   if (environ_cb(RETRO_ENVIRONMENT_GET_PERF_INTERFACE, &perf_cb))
      perf_get_cpu_features_cb = perf_cb.get_cpu_features;
   else
      perf_get_cpu_features_cb = NULL;

   environ_cb(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &colorMode);

   //hacky stuff for Glide64
   polygonOffsetUnits = -3.0f;
   polygonOffsetFactor =  -3.0f;
}

