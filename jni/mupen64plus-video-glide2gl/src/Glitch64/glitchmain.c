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

#include <stdint.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "glide.h"
#include "glitchmain.h"
#include "../Glide64/rdp.h"
#include "../../libretro/SDL.h"

extern retro_environment_t environ_cb;

int width, height;
int bgra8888_support;
static int npot_support;
// ZIGGY
static GLuint default_texture;
int glsl_support = 1;
//Gonetz

uint16_t *frameBuffer;
static uint8_t  *buf;

#ifdef EMSCRIPTEN
GLuint glitch_vbo;
#endif

static int isExtensionSupported(const char *extension)
{
   const char *str = (const char*)glGetString(GL_EXTENSIONS);
   if (str && strstr(str, extension))
      return 1;
   return 0;
}

void FindBestDepthBias();

uint32_t grSstWinOpen(void)
{
   bool ret;
   struct retro_variable var = { "mupen64-screensize", 0 };

   if (frameBuffer)
      grSstWinClose(0);

   ret = environ_cb(RETRO_ENVIRONMENT_GET_VARIABLE, &var);

   if (ret && var.value)
   {
      if (sscanf(var.value ? var.value : "640x480", "%dx%d", &width, &height) != 2)
      {
         width = 640;
         height = 480;
      }
   }
   else
   {
      width = 640;
      height =480;
   }

   // ZIGGY
   // allocate static texture names
   // the initial value should be big enough to support the maximal resolution
   glGenTextures(1, &default_texture);
   frameBuffer = (uint16_t*)malloc(width * height * sizeof(uint16_t));
   buf = (uint8_t*)malloc(width * height * 4 * sizeof(uint8_t));
#ifdef EMSCRIPTEN
   glGenBuffers(1, &glitch_vbo);
#endif
   glViewport(0, 0, width, height);

#if 0
   if (isExtensionSupported("GL_ARB_texture_env_combine") == 0 &&
         isExtensionSupported("GL_EXT_texture_env_combine") == 0)
      DISPLAY_WARNING("Your video card doesn't support GL_ARB_texture_env_combine extension");
   if (isExtensionSupported("GL_ARB_multitexture") == 0)
      DISPLAY_WARNING("Your video card doesn't support GL_ARB_multitexture extension");
   if (isExtensionSupported("GL_ARB_texture_mirrored_repeat") == 0)
      DISPLAY_WARNING("Your video card doesn't support GL_ARB_texture_mirrored_repeat extension");
#endif

   packed_pixels_support = 0;
   
   // we can assume that non-GLES has GL_EXT_packed_pixels
   // support -it's included since OpenGL 1.2
   if (isExtensionSupported("GL_EXT_packed_pixels") != 0)
      packed_pixels_support = 1;

   if (isExtensionSupported("GL_ARB_texture_non_power_of_two") == 0)
   {
      //DISPLAY_WARNING("GL_ARB_texture_non_power_of_two supported.\n");
      npot_support = 0;
   }
   else
   {
      printf("GL_ARB_texture_non_power_of_two supported.\n");
      npot_support = 1;
   }

   if (isExtensionSupported("GL_ARB_shading_language_100") &&
         isExtensionSupported("GL_ARB_shader_objects") &&
         isExtensionSupported("GL_ARB_fragment_shader") &&
         isExtensionSupported("GL_ARB_vertex_shader"))
   {}

   if (isExtensionSupported("GL_EXT_texture_format_BGRA8888"))
   {
      printf("GL_EXT_texture_format_BGRA8888 supported.\n");
      bgra8888_support = 1;
   }
   else
   {
      //DISPLAY_WARNING("GL_EXT_texture_format_BGRA8888 not supported.\n");
      bgra8888_support = 0;
   }

   FindBestDepthBias();

   init_geometry();
   init_combiner();
   init_textures();

   return 1;
}

int32_t grSstWinClose(uint32_t context)
{
   if (frameBuffer)
      free(frameBuffer);

   if (buf)
      free(buf);

   glDeleteTextures(1, &default_texture);
#ifdef EMSCRIPTEN
   glDeleteBuffers(1, &glitch_vbo);
#endif

   frameBuffer = NULL;
   buf         = NULL;

   free_geometry();
   free_combiners();
   free_textures();

   return FXTRUE;
}

// frame buffer

int32_t grLfbLock( int32_t type, int32_t buffer, int32_t writeMode,
          int32_t origin, int32_t pixelPipeline,
          GrLfbInfo_t *info )
{
   info->origin        = origin;
   info->strideInBytes = width * ((writeMode == GR_LFBWRITEMODE_888) ? 4 : 2);
   info->lfbPtr        = frameBuffer;
   info->writeMode     = writeMode;

   if (writeMode == GR_LFBWRITEMODE_565)
   {
      signed i, j;

      glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buf);
      for (j=0; j < height; j++)
      {
         for (i=0; i < width; i++)
         {
            frameBuffer[(height-j-1)*width+i] =
               ((buf[j*width*4+i*4+0] >> 3) << 11) |
               ((buf[j*width*4+i*4+1] >> 2) <<  5) |
               (buf[j*width*4+i*4+2] >> 3);
         }
      }
   }

   return FXTRUE;
}

int32_t grLfbReadRegion( int32_t src_buffer,
      uint32_t src_x, uint32_t src_y,
      uint32_t src_width, uint32_t src_height,
      uint32_t dst_stride, void *dst_data )
{
   unsigned int i,j;

   glReadPixels(src_x, height-src_y-src_height, src_width, src_height, GL_RGBA, GL_UNSIGNED_BYTE, buf);

   for (j=0; j<src_height; j++)
   {
      for (i=0; i<src_width; i++)
      {
         frameBuffer[j*(dst_stride/2)+i] =
            ((buf[(src_height-j-1)*src_width*4+i*4+0] >> 3) << 11) |
            ((buf[(src_height-j-1)*src_width*4+i*4+1] >> 2) <<  5) |
            (buf[(src_height-j-1)*src_width*4+i*4+2] >> 3);
      }
   }

   return FXTRUE;
}

int32_t 
grLfbWriteRegion( int32_t dst_buffer,
      uint32_t dst_x, uint32_t dst_y,
      uint32_t src_format,
      uint32_t src_width, uint32_t src_height,
      int32_t pixelPipeline,
      int32_t src_stride, void *src_data )
{
   unsigned int i,j;
   uint16_t *frameBuffer = (uint16_t*)src_data;

   if(dst_buffer == GR_BUFFER_AUXBUFFER)
   {
      for (j=0; j<src_height; j++)
         for (i=0; i<src_width; i++)
            buf[j*src_width + i] = (uint8_t)
               ((frameBuffer[(src_height-j-1)*(src_stride/2)+i]/(65536.0f*(2.0f/zscale)))+1-zscale/2.0f)
            ;

      glEnable(GL_DEPTH_TEST);
      glDepthFunc(GL_ALWAYS);

      //glDrawBuffer(GL_BACK);
      glClear( GL_DEPTH_BUFFER_BIT );
      glDepthMask(1);
      //glDrawPixels(src_width, src_height, GL_DEPTH_COMPONENT, GL_FLOAT, buf);
   }
   else
   {
      int invert;
      int textureSizes_location;
      static float data[16];
      const unsigned int half_stride = src_stride / 2;

      glActiveTexture(GL_TEXTURE0);

      /* src_format is GR_LFBWRITEMODE_555 */
      for (j=0; j<src_height; j++)
      {
         for (i=0; i<src_width; i++)
         {
            const unsigned int col = frameBuffer[j*half_stride+i];
            buf[j*src_width*4+i*4+0]=((col>>10)&0x1F)<<3;
            buf[j*src_width*4+i*4+1]=((col>>5)&0x1F)<<3;
            buf[j*src_width*4+i*4+2]=((col>>0)&0x1F)<<3;
            buf[j*src_width*4+i*4+3]=0xFF;
         }
      }

      glBindTexture(GL_TEXTURE_2D, default_texture);
      glTexSubImage2D(GL_TEXTURE_2D, 0, 4, src_width, src_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);

      set_copy_shader();

      glDisable(GL_DEPTH_TEST);
      glDisable(GL_BLEND);
      invert = 1;

      data[ 0] = (float)((int)dst_x);                             /* X 0 */
      data[ 1] = (float)(invert*-((int)dst_y));                   /* Y 0 */
      data[ 2] = 0.0f;                                            /* U 0 */
      data[ 3] = 0.0f;                                            /* V 0 */
      data[ 4] = (float)((int)dst_x);                             /* X 1 */
      data[ 5] = (float)(invert*-((int)dst_y + (int)src_height)); /* Y 1 */
      data[ 6] = 0.0f;                                            /* U 1 */
      data[ 7] = (float)src_height;                               /* V 1 */
      data[ 8] = (float)((int)dst_x + (int)src_width);
      data[ 9] = (float)(invert*-((int)dst_y + (int)src_height));
      data[10] = (float)src_width;
      data[11] = (float)src_height;
      data[12] = (float)((int)dst_x);
      data[13] = (float)(invert*-((int)dst_y));
      data[14] = 0.0f;
      data[15] = 0.0f;

#ifdef EMSCRIPTEN
      glBindBuffer(GL_ARRAY_BUFFER, glitch_vbo);
      glBufferData(GL_ARRAY_BUFFER, sizeof(data), data, GL_DYNAMIC_DRAW);
#endif

      glDisableVertexAttribArray(COLOUR_ATTR);
      glDisableVertexAttribArray(TEXCOORD_1_ATTR);
      glDisableVertexAttribArray(FOG_ATTR);

#ifdef EMSCRIPTEN
      glVertexAttribPointer(POSITION_ATTR,2,GL_FLOAT,false,4 * sizeof(float), 0); //Position
      glVertexAttribPointer(TEXCOORD_0_ATTR,2,GL_FLOAT,false,4 * sizeof(float), 2); //Tex
#else
      glVertexAttribPointer(POSITION_ATTR,2,GL_FLOAT,false,4 * sizeof(float), &data[0]); //Position
      glVertexAttribPointer(TEXCOORD_0_ATTR,2,GL_FLOAT,false,4 * sizeof(float), &data[2]); //Tex
#endif

      glEnableVertexAttribArray(COLOUR_ATTR);
      glEnableVertexAttribArray(TEXCOORD_1_ATTR);
      glEnableVertexAttribArray(FOG_ATTR);

      textureSizes_location = glGetUniformLocation(program_object_default,"textureSizes");
      glUniform4f(textureSizes_location,1,1,1,1);

      glDrawArrays(GL_TRIANGLE_STRIP,0,4);
#ifdef EMSCRIPTEN
      glBindBuffer(GL_ARRAY_BUFFER, 0);
#endif

      compile_shader();

      glEnable(GL_DEPTH_TEST);
      glEnable(GL_BLEND);
   }
   return FXTRUE;
}

void grBufferSwap(uint32_t swap_interval)
{
   bool swapmode = settings.swapmode_retro && BUFFERSWAP;
   if (!swapmode)
      retro_return(true);
}

void grClipWindow(uint32_t minx, uint32_t miny, uint32_t maxx, uint32_t maxy)
{
   glScissor(minx, height - maxy, maxx - minx, maxy - miny);
   glEnable(GL_SCISSOR_TEST);
}

void grBufferClear(uint32_t color, uint32_t alpha, uint32_t depth)
{
   glClearColor(((color >> 24) & 0xFF) / 255.0f,
         ((color >> 16) & 0xFF) / 255.0f,
         (color         & 0xFF) / 255.0f,
         alpha / 255.0f);
   glClearDepth(depth / 65535.0f);
   glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
}

void grColorMask(bool rgb, bool a)
{
   glColorMask(rgb, rgb, rgb, a);
}
