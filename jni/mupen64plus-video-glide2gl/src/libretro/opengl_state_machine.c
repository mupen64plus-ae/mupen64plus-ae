#include <stdlib.h>
#include <string.h>

#define NO_TRANSLATE 1

#include <glsym/glsym.h>
#include "opengl_state_machine.h"
#include "m64p_plugin.h"
#include "libretro/libretro.h"
#include <libco.h>

// mupen64 defines
#ifndef GFX_ANGRYLION
#define GFX_ANGRYLION 3
#endif

#ifndef GLES
#define HAVE_LEGACY_GL
#endif

extern cothread_t main_thread;
extern bool flip_only;
extern int stop;
extern enum gfx_plugin_type gfx_plugin;
struct retro_hw_render_callback hw_render;

//forward declarations
//
static int CapState[SGL_CAP_MAX];

static const int CapTranslate[SGL_CAP_MAX] = 
{
    GL_DEPTH_TEST, GL_BLEND, GL_POLYGON_OFFSET_FILL, GL_FOG, GL_CULL_FACE, GL_ALPHA_TEST, GL_SCISSOR_TEST
};

#ifndef HAVE_SHARED_CONTEXT
#define MAX_ATTRIB 8
#define MAX_TEXTURE 32
#define ATTRIB_INITER(X) { X, X, X, X, X, X, X, X }

static GLint VertexAttribPointer_enabled[MAX_ATTRIB] = ATTRIB_INITER(0);

static GLuint Framebuffer_framebuffer = 0;
static GLenum BlendFunc_srcRGB = GL_ONE,  BlendFunc_srcAlpha = GL_ONE;
static GLenum BlendFunc_dstRGB = GL_ZERO, BlendFunc_dstAlpha = GL_ZERO;
static GLclampf ClearColor_red = 0.0f, ClearColor_green = 0.0f, ClearColor_blue = 0.0f, ClearColor_alpha = 0.0f;
static GLdouble ClearDepth_value = 1.0;
static GLboolean ColorMask_red = GL_TRUE;
static GLboolean ColorMask_green = GL_TRUE;
static GLboolean ColorMask_blue = GL_TRUE;
static GLboolean ColorMask_alpha = GL_TRUE;
static GLenum CullFace_mode = GL_BACK;
static GLenum DepthFunc_func = GL_LESS;
static GLboolean DepthMask_flag = GL_TRUE;
static GLclampd DepthRange_zNear = 0.0, DepthRange_zFar = 1.0;
static GLenum FrontFace_mode = GL_CCW;
static GLfloat PolygonOffset_factor = 0.0f, PolygonOffset_units = 0.0f;
static GLint Scissor_x = 0, Scissor_y = 0;
static GLsizei Scissor_width = 640, Scissor_height = 480;

static GLuint UseProgram_program = 0;
static GLint Viewport_x = 0, Viewport_y = 0;
static GLsizei Viewport_width = 640, Viewport_height = 480;
static GLenum ActiveTexture_texture = 0;
static GLuint BindTexture_ids[MAX_TEXTURE];
#endif

extern void vbo_draw(void);
extern void vbo_disable();

#ifndef GLIDE64_MK2
static void gl_vbo_draw(void)
{
   vbo_draw();
}
#else
static void gl_vbo_draw(void)
{
   if (gfx_plugin != GFX_GLIDE64)
      return;

   vbo_draw();
}
#endif

void sglGenerateMipmap(GLenum target)
{
   glGenerateMipmap(target);
}

void sglUniform1f(GLint location, GLfloat v0)
{
   glUniform1f(location, v0);
}

void sglUniform1i(GLint location, GLint v0)
{
   glUniform1i(location, v0);
}

void sglUniform2f(GLint location, GLfloat v0, GLfloat v1)
{
   glUniform2f(location, v0, v1);
}

void sglUniform3f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2)
{
   glUniform3f(location, v0, v1, v2);
}

void sglUniform4f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3)
{
   glUniform4f(location, v0, v1, v2, v3);
}

void sglUniform4fv(GLint location, GLsizei count, const GLfloat *value)
{
   glUniform4fv(location, count, value);
}

void sglDeleteShader(GLuint shader)
{
   glDeleteShader(shader);
}

void sglDeleteProgram(GLuint program)
{
   glDeleteProgram(program);
}

GLuint sglCreateShader(GLenum shaderType)
{
   return glCreateShader(shaderType);
}

GLuint sglCreateProgram(void)
{
   return glCreateProgram();
}

void sglCompileShader(GLuint shader)
{
   glCompileShader(shader);
}

void sglLinkProgram(GLuint program)
{
   glLinkProgram(program);
}

void sglAttachShader(GLuint program, GLuint shader)
{
   glAttachShader(program, shader);
}

void sglGetShaderiv(GLuint shader, GLenum pname, GLint *params)
{
   glGetShaderiv(shader, pname, params);
}

void sglGetProgramiv(GLuint shader, GLenum pname, GLint *params)
{
   glGetProgramiv(shader, pname, params);
}

void sglShaderSource(GLuint shader, GLsizei count, const GLchar **string, const GLint *length)
{
   glShaderSource(shader, count, string, length);
}

void sglBindAttribLocation(GLuint program, GLuint index, const GLchar *name)
{
   glBindAttribLocation(program, index, name);
}

void sglGetShaderInfoLog(GLuint shader, GLsizei maxLength, GLsizei *length, GLchar *infoLog)
{
   glGetShaderInfoLog(shader, maxLength, length, infoLog);
}

void sglGetProgramInfoLog(GLuint shader, GLsizei maxLength, GLsizei *length, GLchar *infoLog)
{
   glGetProgramInfoLog(shader, maxLength, length, infoLog);
}

GLint sglGetUniformLocation(GLuint program, const GLchar *name)
{
   return glGetUniformLocation(program, name);
}

void sglEnable(GLenum cap)
{
   gl_vbo_draw();
    glEnable(CapTranslate[cap]);
    CapState[cap] = 1;
}

void sglDisable(GLenum cap)
{
   gl_vbo_draw();
    glDisable(CapTranslate[cap]);
    CapState[cap] = 0;
}

GLboolean sglIsEnabled(GLenum cap)
{
    return CapState[cap] ? GL_TRUE : GL_FALSE;
}



void sglEnableVertexAttribArray(GLuint index)
{
   gl_vbo_draw();
#ifndef HAVE_SHARED_CONTEXT
   VertexAttribPointer_enabled[index] = 1;
#endif
    glEnableVertexAttribArray(index);
}

void sglDisableVertexAttribArray(GLuint index)
{
#ifndef HAVE_SHARED_CONTEXT
   VertexAttribPointer_enabled[index] = 0;
#endif
    glDisableVertexAttribArray(index);
}

void sglVertexAttribPointer(GLuint name, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid* pointer)
{
    glVertexAttribPointer(name, size, type, normalized, stride, pointer);
}

void sglVertexAttrib4f(GLuint name, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    glVertexAttrib4f(name, x, y, z, w);
}

void sglVertexAttrib4fv(GLuint name, GLfloat* v)
{
    glVertexAttrib4fv(name, v);
}

void sglBindFramebuffer(GLenum target, GLuint framebuffer)
{
   gl_vbo_draw();
   //if (!stop)
      glBindFramebuffer(GL_FRAMEBUFFER, framebuffer ? framebuffer : hw_render.get_current_framebuffer());
}

void sglBlendFunc(GLenum sfactor, GLenum dfactor)
{
   gl_vbo_draw();
#ifndef HAVE_SHARED_CONTEXT
    BlendFunc_srcRGB = BlendFunc_srcAlpha = sfactor;
    BlendFunc_dstRGB = BlendFunc_dstAlpha = dfactor;
#endif
    glBlendFunc(sfactor, dfactor);
}

void sglBlendFuncSeparate(GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha)
{
   gl_vbo_draw();
#ifndef HAVE_SHARED_CONTEXT
    BlendFunc_srcRGB = srcRGB;
    BlendFunc_dstRGB = dstRGB;
    BlendFunc_srcAlpha = srcAlpha;
    BlendFunc_dstAlpha = dstAlpha;
#endif
    glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
}

void sglClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
   gl_vbo_draw();
   glClearColor(red, green, blue, alpha);
#ifndef HAVE_SHARED_CONTEXT
   ClearColor_red = red;
   ClearColor_green = green;
   ClearColor_blue = blue;
   ClearColor_alpha = alpha;
#endif
}

void sglClearDepthf(GLdouble depth)
{
   gl_vbo_draw();
#ifdef GLES
   glClearDepthf(depth);
#else
   glClearDepth(depth);
#endif
}

void sglClearDepth(GLdouble depth)
{
   gl_vbo_draw();
   sglClearDepthf(depth);
#ifndef HAVE_SHARED_CONTEXT
   ClearDepth_value = depth;
#endif
}

void sglColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
   gl_vbo_draw();
   glColorMask(red, green, blue, alpha);
#ifndef HAVE_SHARED_CONTEXT
   ColorMask_red = red;
   ColorMask_green = green;
   ColorMask_blue = blue;
   ColorMask_alpha = alpha;
#endif
}


void sglCullFace(GLenum mode)
{
   gl_vbo_draw();
   glCullFace(mode);
#ifndef HAVE_SHARED_CONTEXT
   CullFace_mode = mode;
#endif
}

void sglDepthFunc(GLenum func)
{
   gl_vbo_draw();
  glDepthFunc(func);
#ifndef HAVE_SHARED_CONTEXT
  DepthFunc_func = func;
#endif
}

void sglDepthMask(GLboolean flag)
{
   gl_vbo_draw();
  glDepthMask(flag);
#ifndef HAVE_SHARED_CONTEXT
  DepthMask_flag = flag;
#endif
}

void sglDepthRangef(GLclampd zNear, GLclampd zFar)
{
#ifdef GLES
   glDepthRangef(zNear, zFar);
#else
   glDepthRange(zNear, zFar);
#endif
}

void sglDepthRange(GLclampd zNear, GLclampd zFar)
{
   sglDepthRangef(zNear, zFar);
#ifndef HAVE_SHARED_CONTEXT
   DepthRange_zNear = zNear;
   DepthRange_zFar = zFar;
#endif
}

void sglFrontFace(GLenum mode)
{
   gl_vbo_draw();
   glFrontFace(mode);
#ifndef HAVE_SHARED_CONTEXT
   FrontFace_mode = mode;
#endif
}

void sglPolygonOffset(GLfloat factor, GLfloat units)
{
   gl_vbo_draw();
  glPolygonOffset(factor, units);
#ifndef HAVE_SHARED_CONTEXT
  PolygonOffset_factor = factor;
  PolygonOffset_units = units;
#endif
}

void sglScissor(GLint x, GLint y, GLsizei width, GLsizei height)
{
   gl_vbo_draw();
  glScissor(x, y, width, height);
#ifndef HAVE_SHARED_CONTEXT
  Scissor_x = x;
  Scissor_y = y;
  Scissor_width = width;
  Scissor_height = height;
#endif
}

void sglUseProgram(GLuint program)
{
   gl_vbo_draw();
   glUseProgram(program);
#ifndef HAVE_SHARED_CONTEXT
   UseProgram_program = program;
#endif
}

void sglViewport(GLint x, GLint y, GLsizei width, GLsizei height)
{
   gl_vbo_draw();
   glViewport(x, y, width, height);
#ifndef HAVE_SHARED_CONTEXT
   Viewport_x = x;
   Viewport_y = y;
   Viewport_width = width;
   Viewport_height = height;
#endif
}


void sglActiveTexture(GLenum texture)
{
   gl_vbo_draw();
   glActiveTexture(texture);
#ifndef HAVE_SHARED_CONTEXT
   ActiveTexture_texture = texture - GL_TEXTURE0;
#endif
}

void sglBindTexture(GLenum target, GLuint texture)
{
   gl_vbo_draw();
   glBindTexture(target, texture);
#ifndef HAVE_SHARED_CONTEXT
   BindTexture_ids[ActiveTexture_texture] = texture;
#endif
}

void sglDeleteRenderbuffers(GLsizei n, GLuint *renderbuffers)
{
   glDeleteRenderbuffers(n, renderbuffers);
}

void sglGenFramebuffers(GLsizei n, GLuint *ids)
{
   glGenFramebuffers(n, ids);
}

void sglGenRenderbuffers(GLsizei n, GLuint *renderbuffers)
{
   glGenRenderbuffers(n, renderbuffers);
}

void sglGenTextures(GLsizei n, GLuint *textures)
{
   glGenTextures(n, textures);
}

void sglBindRenderbuffer(GLenum target, GLuint renderbuffer)
{
   glBindRenderbuffer(target, renderbuffer);
}

void sglRenderbufferStorage(GLenum target, GLenum internalFormat, GLsizei width, GLsizei height)
{
   glRenderbufferStorage(target, internalFormat, width, height);
}

void sglFramebufferRenderbuffer(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer)
{
   glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
}

GLenum sglCheckFramebufferStatus(GLenum target)
{
   return glCheckFramebufferStatus(target);
}

void sglDeleteFramebuffers(GLsizei n, GLuint *framebuffers)
{
   glDeleteFramebuffers(n, framebuffers);
}

void sglDeleteTextures(GLsizei n, const GLuint *textures)
{
   glDeleteTextures(n, textures);
}

void sglFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)
{
   glFramebufferTexture2D(target, attachment, textarget, texture, level);
}

void sglBindBuffer(GLenum target, GLuint buffer)
{
   gl_vbo_draw();
   glBindBuffer(target, buffer);
}

void sglCompressedTexImage2D(GLenum target, GLint level, GLenum internalformat,
      GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data)
{
   glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data);
}

void sglTexCoord2f(GLfloat s, GLfloat t)
{
#ifdef HAVE_LEGACY_GL
   glTexCoord2f(s, t);
#endif
}

void sglDrawArrays(GLenum mode, GLint first, GLsizei count)
{
   glDrawArrays(mode, first, count);
}

void sglBufferData(GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage)
{
   glBufferData(target, size, data, usage);
}

void sglBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data)
{
   glBufferSubData(target, offset, size, data);
}

void sglGenBuffers(GLsizei n, GLuint *buffers)
{
   glGenBuffers(n, buffers);
}

void sglDeleteBuffers(GLsizei n, const GLuint *buffers)
{
   glDeleteBuffers(n, buffers);
}

#if 0
struct tex_map
{
   unsigned address;
   GLuint tex;
};

static struct tex_map *texture_map;
static size_t texture_map_size;
static size_t texture_map_cap;

static GLuint find_tex_from_address(unsigned address)
{
   size_t i;
   for (i = 0; i < texture_map_size; i++)
   {
      if (texture_map[i].address == address)
         return texture_map[i].tex;
   }
   return 0;
}

static void delete_tex_from_address(unsigned address)
{
   size_t i;
   for (i = 0; i < texture_map_size; i++)
   {
      if (texture_map[i].address == address)
      {
         glDeleteTextures(1, &texture_map[i].tex);
         memmove(texture_map + i, texture_map + i + 1, (texture_map_size - (i + 1)) * sizeof(*texture_map));
         texture_map_size--;
         return;
      }
   }

   glDeleteTextures(1, &address);
}
#endif

extern int InitGfx(void);
extern void gles2n64_reset(void);
extern void reinit_gfx_plugin(void);

static int gotsym;
static void context_reset(void)
{
   //if (!gotsym && gfx_plugin != GFX_ANGRYLION)
      gotsym = (rglgen_resolve_symbols(hw_render.get_proc_address), 1);


#ifdef HAVE_SHARED_CONTEXT
   sglBindFramebuffer(GL_FRAMEBUFFER, 0);
#endif
}

void *retro_gl_init(void)
{
   //if (gfx_plugin == GFX_ANGRYLION)
   //   return NULL;

#ifdef GLES
#if defined(GLES31)
   hw_render.context_type = RETRO_HW_CONTEXT_OPENGLES_VERSION;
   hw_render.version_major = 3;
   hw_render.version_minor = 1;
#elif defined(GLES3)
   hw_render.context_type = RETRO_HW_CONTEXT_OPENGLES3;
#else
   hw_render.context_type = RETRO_HW_CONTEXT_OPENGLES2;
#endif
#else
#ifdef CORE
   hw_render.context_type = RETRO_HW_CONTEXT_OPENGL_CORE;
   hw_render.version_major = 3;
   hw_render.version_minor = 1;
#else
   hw_render.context_type = RETRO_HW_CONTEXT_OPENGL;
#endif
#endif
   hw_render.context_reset = context_reset;
   hw_render.depth = true;
   hw_render.bottom_left_origin = true;
   hw_render.cache_context = true;

   return &hw_render;
}

#ifndef HAVE_SHARED_CONTEXT
void sglEnter(void)
{
   int i;

   //if (stop)
   //   return;

   if (!gotsym)
      gotsym = (rglgen_resolve_symbols(hw_render.get_proc_address), 1);

   for (i = 0; i < MAX_ATTRIB; i ++)
   {
      if (VertexAttribPointer_enabled[i])
         glEnableVertexAttribArray(i);
      else
         glDisableVertexAttribArray(i);
   }

    sglBindFramebuffer(GL_FRAMEBUFFER, Framebuffer_framebuffer);

    glBlendFuncSeparate(BlendFunc_srcRGB, BlendFunc_dstRGB, BlendFunc_srcAlpha, BlendFunc_dstAlpha);
    glClearColor(ClearColor_red, ClearColor_green, ClearColor_blue, ClearColor_alpha);
    sglClearDepthf(ClearDepth_value);
    glColorMask(ColorMask_red, ColorMask_green, ColorMask_blue, ColorMask_alpha);
    glCullFace(CullFace_mode);
    glDepthFunc(DepthFunc_func);
    glDepthMask(DepthMask_flag);
    sglDepthRangef(DepthRange_zNear, DepthRange_zFar);
    glFrontFace(FrontFace_mode);
    glPolygonOffset(PolygonOffset_factor, PolygonOffset_units);
    glScissor(Scissor_x, Scissor_y, Scissor_width, Scissor_height);
    glUseProgram(UseProgram_program);
    glViewport(Viewport_x, Viewport_y, Viewport_width, Viewport_height);

    for(i = 0; i != SGL_CAP_MAX; i ++)
    {
        if (CapState[i])
           glEnable(CapTranslate[i]);
        else
           glDisable(CapTranslate[i]);
    }

    for (i = 0; i < MAX_TEXTURE; i ++)
    {
        glActiveTexture(GL_TEXTURE0 + i);
        glBindTexture(GL_TEXTURE_2D, BindTexture_ids[i]);
    }

    glActiveTexture(GL_TEXTURE0 + ActiveTexture_texture);


    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

void sglExit(void)
{
   int i;

   //if (/*gfx_plugin == GFX_ANGRYLION || */stop)
   //   return;

    for (i = 0; i < SGL_CAP_MAX; i ++)
        glDisable(CapTranslate[i]);

    glBlendFunc(GL_ONE, GL_ZERO);
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
    glCullFace(GL_BACK);
    glDepthMask(GL_TRUE);
    sglDepthRangef(0, 1);
    glFrontFace(GL_CCW);
    glPolygonOffset(0, 0);
    glUseProgram(0);

    // Clear textures
    for (i = 0; i < MAX_TEXTURE; i ++)
    {
        glActiveTexture(GL_TEXTURE0 + i);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    glActiveTexture(GL_TEXTURE0);

    for (i = 0; i < MAX_ATTRIB; i ++)
       glDisableVertexAttribArray(i);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
}
#endif

extern bool frame_dupe;


extern retro_video_refresh_t video_cb;
extern uint32_t screen_width;
extern uint32_t screen_height;
bool emu_step_render(void);

int retro_return(int just_flipping)
{
   vbo_disable();

   flip_only = just_flipping;

   if (just_flipping)
   {
#ifndef HAVE_SHARED_CONTEXT
      sglExit();
#endif

      emu_step_render();

#ifndef HAVE_SHARED_CONTEXT
      sglEnter();
#endif
   }

  // stop = 1;
   return 0;
}
