#ifndef OPENGL_STATE_MACHINE_H__
#define OPENGL_STATE_MACHINE_H__

#include <glsym/rglgen.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef HAVE_OPENGLES2
typedef GLfloat GLdouble;
typedef GLclampf GLclampd;
#endif

#ifndef GL_FOG
#define GL_FOG 0x0B60
#endif

#ifndef GL_ALPHA_TEST
#define GL_ALPHA_TEST 0x0BC0
#endif

enum
{
   SGL_DEPTH_TEST,
   SGL_BLEND,
   SGL_POLYGON_OFFSET_FILL,
   SGL_FOG,
   SGL_CULL_FACE,
   SGL_ALPHA_TEST,
   SGL_SCISSOR_TEST,
   SGL_CAP_MAX
};

void sglEnable(GLenum cap);
void sglDisable(GLenum cap);
GLboolean sglIsEnabled(GLenum cap);

void sglDrawArrays(GLenum mode, GLint first, GLsizei count);
void sglEnableVertexAttribArray(GLuint index);
void sglDisableVertexAttribArray(GLuint index);
void sglVertexAttribPointer(GLuint index, GLint size, GLenum type, GLboolean normalize,
      GLsizei stride, const GLvoid* pointer);
void sglVertexAttrib4f(GLuint index, GLfloat x, GLfloat y, GLfloat z, GLfloat w);
void sglVertexAttrib4fv(GLuint index, GLfloat* v);
void sglGenerateMipmap(GLenum target);

void sglCompressedTexImage2D(GLenum target, GLint level, GLenum internalformat,
      GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data);

void sglGenTextures(GLsizei n, GLuint *textures);

void sglUniform1f(GLint location, GLfloat v0);
void sglUniform1i(GLint location, GLint v0);
void sglUniform2f(GLint location, GLfloat v0, GLfloat v1);
void sglUniform3f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2);
void sglUniform4f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3);
void sglUniform4fv(GLint location, GLsizei count, const GLfloat *value);
int sglGetUniformLocation(GLuint program, const GLchar *name);
void sglGetShaderInfoLog(GLuint shader, GLsizei maxLength, GLsizei *length, GLchar *infoLog);
void sglGetProgramInfoLog(GLuint shader, GLsizei maxLength, GLsizei *length, GLchar *infoLog);

void sglBindFramebuffer(GLenum target, GLuint framebuffer);
void sglBlendFunc(GLenum sfactor, GLenum dfactor);
void sglBlendFuncSeparate(GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha);
void sglClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
void sglClearDepth(GLdouble value);
void sglColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
void sglCullFace(GLenum mode);
void sglDepthFunc(GLenum func);
void sglDepthMask(GLboolean flag);
void sglDepthRange(GLclampd nearVal, GLclampd farVal);
void sglFrontFace(GLenum mode);
void sglPolygonOffset(GLfloat factor, GLfloat units);
void sglScissor(GLint x, GLint y, GLsizei width, GLsizei height);
void sglUseProgram(GLuint program);
void sglViewport(GLint x, GLint y, GLsizei width, GLsizei height);

void sglBindBuffer(GLenum target, GLuint buffer);
void sglActiveTexture(GLenum texture);
void sglBindTexture(GLenum target, GLuint texture);

void sglDeleteTextures(GLsizei n, const GLuint *textures);
void sglDeleteShader(GLuint shader);
void sglDeleteProgram(GLuint program);
GLuint sglCreateShader(GLenum shaderType);
GLuint sglCreateProgram(void);
void sglShaderSource(GLuint shader, GLsizei count, const GLchar **string, const GLint *length);

void sglCompileShader(GLuint shader);
void sglGetShaderiv(GLuint shader, GLenum pname, GLint *params);
void sglGetProgramiv(GLuint program, GLenum pname, GLint *params);
void sglAttachShader(GLuint program, GLuint shader);

void sglLinkProgram(GLuint program);
void sglBindAttribLocation(GLuint program, GLuint index, const GLchar *name);

void sglGenRenderbuffers(GLsizei n, GLuint *renderbuffers);
void sglRenderbufferStorage(GLenum target, GLenum internalFormat, GLsizei width, GLsizei height);
void sglBindRenderbuffer(GLenum target, GLuint renderbuffer);
void sglFramebufferRenderbuffer(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer);
GLenum sglCheckFramebufferStatus(GLenum target);
void sglDeleteFramebuffers(GLsizei n, GLuint *framebuffers);
void sglBindFramebuffer(GLenum target, GLuint framebuffer);
void sglGenFramebuffers(GLsizei n, GLuint *ids);
void sglFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
void sglDeleteRenderbuffers(GLsizei n, GLuint *renderbuffers);

void sglBufferData(GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage);
void sglBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data);

void sglGenBuffers(GLsizei n, GLuint * buffers);

void sglDeleteBuffers(GLsizei n, const GLuint *buffers);

void sglTexCoord2f(float S, float t);

#ifndef NO_TRANSLATE
#define glEnable(T) sglEnable(S##T)
#define glDisable(T) sglDisable(S##T)
#define glIsEnabled(T) sglIsEnabled(S##T)
#define glEnableVertexAttribArray sglEnableVertexAttribArray
#define glDisableVertexAttribArray sglDisableVertexAttribArray
#define glVertexAttribPointer sglVertexAttribPointer
#define glVertexAttrib4f sglVertexAttrib4f
#define glVertexAttrib4fv sglVertexAttrib4fv
#define glBindFramebuffer sglBindFramebuffer
#define glBlendFunc sglBlendFunc
#define glBlendFuncSeparate sglBlendFuncSeparate
#define glClearColor sglClearColor
#define glClearDepth sglClearDepth
#define glClearDepthf sglClearDepth
#define glColorMask sglColorMask
#define glCullFace sglCullFace
#define glDepthFunc sglDepthFunc
#define glDepthMask sglDepthMask
#define glDepthRange sglDepthRange
#define glFrontFace sglFrontFace
#define glPolygonOffset sglPolygonOffset
#define glScissor sglScissor
#define glUseProgramARB sglUseProgram
#define glUseProgram sglUseProgram
#define glViewport sglViewport
#define glActiveTextureARB sglActiveTexture
#define glActiveTexture sglActiveTexture
#define glBindTexture sglBindTexture
#define glGenerateMipmap sglGenerateMipmap
#define glUniform1i sglUniform1i
#define glGetUniformLocation sglGetUniformLocation
#define glUniform1f sglUniform1f
#define glUniform2f sglUniform2f
#define glUniform3f sglUniform3f
#define glUniform4f sglUniform4f
#define glUniform4fv sglUniform4fv
#define glGetShaderInfoLog sglGetShaderInfoLog
#define glGetProgramInfoLog sglGetProgramInfoLog
#define glDeleteProgram sglDeleteProgram
#define glDeleteShader sglDeleteShader
#define glCreateShader sglCreateShader
#define glCreateProgram sglCreateProgram
#define glShaderSource sglShaderSource
#define glCompileShader sglCompileShader
#define glGetShaderiv sglGetShaderiv
#define glGetProgramiv sglGetProgramiv
#define glAttachShader sglAttachShader
#define glLinkProgram sglLinkProgram
#define glBindAttribLocation sglBindAttribLocation
#define glDeleteRenderbuffers sglDeleteRenderbuffers
#define glDeleteFramebuffers sglDeleteFramebuffers
#define glGenFramebuffers sglGenFramebuffers
#define glGenRenderbuffers sglGenRenderbuffers
#define glGenTextures sglGenTextures
#define glBindBuffer sglBindBuffer
#define glBindRenderbuffer sglBindRenderbuffer
#define glRenderbufferStorage sglRenderbufferStorage
#define glFramebufferRenderbuffer sglFramebufferRenderbuffer
#define glCheckFramebufferStatus sglCheckFramebufferStatus
#define glCheckFramebufferStatusEXT sglCheckFramebufferStatus
#define glDeleteFramebuffers sglDeleteFramebuffers
#define glDeleteTextures sglDeleteTextures
#define glFramebufferTexture2D sglFramebufferTexture2D
#define glCompressedTexImage2DARB sglCompressedTexImage2D
#define glCompressedTexImage2D sglCompressedTexImage2D
#define glTexCoord2f sglTexCoord2f
#define glDrawArrays sglDrawArrays
#define glBufferData sglBufferData
#define glBufferSubData sglBufferSubData
#define glGenBuffers sglGenBuffers
#define glDeleteBuffers sglDeleteBuffers
#endif

void sglEnter(void);
void sglExit(void);

void *retro_gl_init(void);

int retro_return(int just_flipping);

#ifdef __cplusplus
}
#endif

#endif
