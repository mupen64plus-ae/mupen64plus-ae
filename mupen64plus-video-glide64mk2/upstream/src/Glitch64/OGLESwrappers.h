void vbo_draw();

static GLenum cached_ActiveTexture_texture;
void inline cache_glActiveTexture (GLenum texture)
{
  if(texture != cached_ActiveTexture_texture)
  {
    vbo_draw();
    glActiveTexture(texture);
    cached_ActiveTexture_texture = texture;
  }
}
#define glActiveTexture(texture) cache_glActiveTexture(texture)

void inline cache_glBindTexture (GLenum target, GLuint texture)
{
    vbo_draw();
    glBindTexture(target, texture);
}
#define glBindTexture(target, texture) cache_glBindTexture(target, texture)

static GLenum cached_BlendEquation_mode;
void inline cache_glBlendEquation ( GLenum mode )
{
  if(mode != cached_BlendEquation_mode)
  {
    vbo_draw();
    glBlendEquation(mode);
    cached_BlendEquation_mode = mode;
  }
}
#define glBlendEquation(mode) cache_glBlendEquation(mode)

static GLenum cached_BlendEquationSeparate_modeRGB;
static GLenum cached_BlendEquationSeparate_modeAlpha;
void inline cache_glBlendEquationSeparate (GLenum modeRGB, GLenum modeAlpha)
{
  if(modeRGB != cached_BlendEquationSeparate_modeRGB || modeAlpha != cached_BlendEquationSeparate_modeAlpha)
  {
    vbo_draw();
    glBlendEquationSeparate(modeRGB, modeAlpha);
    cached_BlendEquationSeparate_modeRGB = modeRGB;
    cached_BlendEquationSeparate_modeAlpha = modeAlpha;
  }
}
#define glBlendEquationSeparate(modeRGB, modeAlpha) cache_glBlendEquationSeparate(modeRGB, modeAlpha)

static GLenum cached_BlendFunc_sfactor;
static GLenum cached_BlendFunc_dfactor;
void inline cache_glBlendFunc (GLenum sfactor, GLenum dfactor)
{
  if(sfactor != cached_BlendFunc_sfactor || dfactor != cached_BlendFunc_dfactor)
  {
    vbo_draw();
    glBlendFunc(sfactor, dfactor);
    cached_BlendFunc_sfactor = sfactor;
    cached_BlendFunc_dfactor = dfactor;
  }
}
#define glBlendFunc(sfactor, dfactor) cache_glBlendFunc(sfactor, dfactor)

static GLenum cached_BlendFuncSeparate_srcRGB;
static GLenum cached_BlendFuncSeparate_dstRGB;
static GLenum cached_BlendFuncSeparate_srcAlpha;
static GLenum cached_BlendFuncSeparate_dstAlpha;
void inline cache_glBlendFuncSeparate (GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha)
{
  if(srcRGB != cached_BlendFuncSeparate_srcRGB || dstRGB != cached_BlendFuncSeparate_dstRGB || srcAlpha != cached_BlendFuncSeparate_srcAlpha || dstAlpha != cached_BlendFuncSeparate_dstAlpha)
  {
    vbo_draw();
    glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    cached_BlendFuncSeparate_srcRGB = srcRGB;
    cached_BlendFuncSeparate_dstRGB = dstRGB;
    cached_BlendFuncSeparate_srcAlpha = srcAlpha;
    cached_BlendFuncSeparate_dstAlpha = dstAlpha;
  }
}
#define glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha) cache_glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)

static GLclampf cached_ClearColor_red;
static GLclampf cached_ClearColor_green;
static GLclampf cached_ClearColor_blue;
static GLclampf cached_ClearColor_alpha;
void inline cache_glClearColor (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
  if(red != cached_ClearColor_red || green != cached_ClearColor_green || blue != cached_ClearColor_blue || alpha != cached_ClearColor_alpha)
  {
    vbo_draw();
    glClearColor(red, green, blue, alpha);
    cached_ClearColor_red = red;
    cached_ClearColor_green = green;
    cached_ClearColor_blue = blue;
    cached_ClearColor_alpha = alpha;
  }
}
#define glClearColor(red, green, blue, alpha) cache_glClearColor(red, green, blue, alpha)

static GLclampf cached_ClearDepthf_depth;
void inline cache_glClearDepthf (GLclampf depth)
{
  if(depth != cached_ClearDepthf_depth)
  {
    vbo_draw();
    glClearDepthf(depth);
    cached_ClearDepthf_depth = depth;
  }
}
#define glClearDepthf(depth) cache_glClearDepthf(depth)

static GLenum cached_CullFace_mode;
void inline cache_glCullFace (GLenum mode)
{
  if(mode != cached_CullFace_mode)
  {
    vbo_draw();
    glCullFace(mode);
    cached_CullFace_mode = mode;
  }
}
#define glCullFace(mode) cache_glCullFace(mode)

static GLenum cached_DepthFunc_func;
void inline cache_glDepthFunc (GLenum func)
{
  if(func != cached_DepthFunc_func)
  {
    vbo_draw();
    glDepthFunc(func);
    cached_DepthFunc_func = func;
  }
}
#define glDepthFunc(func) cache_glDepthFunc(func)

static GLboolean cached_DepthMask_flag;
void inline cache_glDepthMask (GLboolean flag)
{
  if(flag != cached_DepthMask_flag)
  {
    vbo_draw();
    glDepthMask(flag);
    cached_DepthMask_flag = flag;
  }
}
#define glDepthMask(flag) cache_glDepthMask(flag)

static GLclampf cached_DepthRangef_zNear;
static GLclampf cached_DepthRangef_zFar;
void inline cache_glDepthRangef (GLclampf zNear, GLclampf zFar)
{
  if(zNear != cached_DepthRangef_zNear || zFar != cached_DepthRangef_zFar)
  {
    vbo_draw();
    glDepthRangef(zNear, zFar);
    cached_DepthRangef_zNear = zNear;
    cached_DepthRangef_zFar = zFar;
  }
}
#define glDepthRangef(zNear, zFar) cache_glDepthRangef(zNear, zFar)

static bool cached_BLEND = false;
static bool cached_CULL_FACE = false;
static bool cached_DEPTH_TEST = false;
static bool cached_DITHER = false;
static bool cached_POLYGON_OFFSET_FILL = false;
static bool cached_SAMPLE_ALPHA_TO_COVERAGE = false;
static bool cached_SAMPLE_COVERAGE = false;
static bool cached_SCISSOR_TEST = false;
static bool cached_STENCIL_TEST = false;
void inline cache_glDisable (GLenum cap)
{
  if(cap == GL_BLEND && cached_BLEND)
  {
    vbo_draw();
    glDisable(GL_BLEND);
    cached_BLEND = false;
  }
  else if(cap == GL_CULL_FACE && cached_CULL_FACE)
  {
    vbo_draw();
    glDisable(GL_CULL_FACE);
    cached_CULL_FACE = false;
  }
  else if(cap == GL_DEPTH_TEST && cached_DEPTH_TEST)
  {
    vbo_draw();
    glDisable(GL_DEPTH_TEST);
    cached_DEPTH_TEST = false;
  }
  else if(cap == GL_DITHER && cached_DITHER)
  {
    vbo_draw();
    glDisable(GL_DITHER);
    cached_DITHER = false;
  }
  else if(cap == GL_POLYGON_OFFSET_FILL && cached_POLYGON_OFFSET_FILL)
  {
    vbo_draw();
    glDisable(GL_POLYGON_OFFSET_FILL);
    cached_POLYGON_OFFSET_FILL = false;
  }
  else if(cap == GL_SAMPLE_ALPHA_TO_COVERAGE && cached_SAMPLE_ALPHA_TO_COVERAGE)
  {
    vbo_draw();
    glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE);
    cached_SAMPLE_ALPHA_TO_COVERAGE = false;
  }
  else if(cap == GL_SAMPLE_COVERAGE && cached_SAMPLE_COVERAGE)
  {
    vbo_draw();
    glDisable(GL_SAMPLE_COVERAGE);
    cached_SAMPLE_COVERAGE = false;
  }
  else if(cap == GL_SCISSOR_TEST && cached_SCISSOR_TEST)
  {
    vbo_draw();
    glDisable(GL_SCISSOR_TEST);
    cached_SCISSOR_TEST = false;
  }
  else if(cap == GL_STENCIL_TEST && cached_STENCIL_TEST)
  {
    vbo_draw();
    glDisable(GL_STENCIL_TEST);
    cached_STENCIL_TEST = false;
  }
}
#define glDisable(cap) cache_glDisable(cap)

void inline cache_glEnable (GLenum cap)
{
  if(cap == GL_BLEND && !cached_BLEND)
  {
    vbo_draw();
    glEnable(GL_BLEND);
    cached_BLEND = true;
  }
  else if(cap == GL_CULL_FACE && !cached_CULL_FACE)
  {
    vbo_draw();
    glEnable(GL_CULL_FACE);
    cached_CULL_FACE = true;
  }
  else if(cap == GL_DEPTH_TEST && !cached_DEPTH_TEST)
  {
    vbo_draw();
    glEnable(GL_DEPTH_TEST);
    cached_DEPTH_TEST = true;
  }
  else if(cap == GL_DITHER && !cached_DITHER)
  {
    vbo_draw();
    glEnable(GL_DITHER);
    cached_DITHER = true;
  }
  else if(cap == GL_POLYGON_OFFSET_FILL && !cached_POLYGON_OFFSET_FILL)
  {
    vbo_draw();
    glEnable(GL_POLYGON_OFFSET_FILL);
    cached_POLYGON_OFFSET_FILL = true;
  }
  else if(cap == GL_SAMPLE_ALPHA_TO_COVERAGE && !cached_SAMPLE_ALPHA_TO_COVERAGE)
  {
    vbo_draw();
    glEnable(GL_SAMPLE_ALPHA_TO_COVERAGE);
    cached_SAMPLE_ALPHA_TO_COVERAGE = true;
  }
  else if(cap == GL_SAMPLE_COVERAGE && !cached_SAMPLE_COVERAGE)
  {
    vbo_draw();
    glEnable(GL_SAMPLE_COVERAGE);
    cached_SAMPLE_COVERAGE = true;
  }
  else if(cap == GL_SCISSOR_TEST && !cached_SCISSOR_TEST)
  {
    vbo_draw();
    glEnable(GL_SCISSOR_TEST);
    cached_SCISSOR_TEST = true;
  }
  else if(cap == GL_STENCIL_TEST && !cached_STENCIL_TEST)
  {
    vbo_draw();
    glEnable(GL_STENCIL_TEST);
    cached_STENCIL_TEST = true;
  }
}
#define glEnable(cap) cache_glEnable(cap)

static GLenum cached_FrontFace_mode;
void inline cache_glFrontFace (GLenum mode)
{
  if(mode != cached_FrontFace_mode)
  {
    vbo_draw();
    glFrontFace(mode);
    cached_FrontFace_mode = mode;
  }
}
#define glFrontFace(mode) cache_glFrontFace(mode)

static GLfloat cached_PolygonOffset_factor;
static GLfloat cached_PolygonOffset_units;
void inline cache_glPolygonOffset (GLfloat factor, GLfloat units)
{
  if(factor != cached_PolygonOffset_factor || units != cached_PolygonOffset_units)
  {
    vbo_draw();
    glPolygonOffset(factor, units);
    cached_PolygonOffset_factor = factor;
    cached_PolygonOffset_units = units;
  }
}
#define glPolygonOffset(factor, units) cache_glPolygonOffset(factor, units)

static GLint cached_Scissor_x;
static GLint cached_Scissor_y;
static GLsizei cached_Scissor_width;
static GLsizei cached_Scissor_height;
void inline cache_glScissor (GLint x, GLint y, GLsizei width, GLsizei height)
{
  if(x != cached_Scissor_x || y != cached_Scissor_y || width != cached_Scissor_width || height != cached_Scissor_height)
  {
    vbo_draw();
    glScissor(x, y, width, height);
    cached_Scissor_x = x;
    cached_Scissor_y = y;
    cached_Scissor_width = width;
    cached_Scissor_height = height;
  }
}
#define glScissor(x, y, width, height) cache_glScissor(x, y, width, height)

static GLuint cached_UseProgram_program;
void inline cache_glUseProgram (GLuint program)
{
  if(program != cached_UseProgram_program)
  {
    vbo_draw();
    glUseProgram(program);
    cached_UseProgram_program = program;
  }
}
#define glUseProgram(program) cache_glUseProgram(program)

static GLint cached_Viewport_x;
static GLint cached_Viewport_y;
static GLsizei cached_Viewport_width;
static GLsizei cached_Viewport_height;
void inline cache_glViewport (GLint x, GLint y, GLsizei width, GLsizei height)
{
  if(x != cached_Viewport_x || y != cached_Viewport_y || width != cached_Viewport_width || height != cached_Viewport_height)
  {
    vbo_draw();
    glViewport(x, y, width, height);
    cached_Viewport_x = x;
    cached_Viewport_y = y;
    cached_Viewport_width = width;
    cached_Viewport_height = height;
  }
}
#define glViewport(x, y, width, height) cache_glViewport(x, y, width, height)

