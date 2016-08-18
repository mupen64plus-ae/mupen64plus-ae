#include "RefreshScreen.h"
#include <string.h>
#include <android/log.h>
#include <stdbool.h>

static const char* vertex_shader =
"#version 300 es \n"
"layout(location = 0) in vec3 vertexPosition_modelspace;           \n"
"                                                                  \n"
"out vec2 UV;                                                      \n"
"                                                                  \n"
"void main(){                                                      \n"
"	gl_Position = vec4(vertexPosition_modelspace,1);               \n"
"	UV = (vertexPosition_modelspace.xy+vec2(1,1))/2.0;             \n"
"}                                                                 \n"
;

static const char* fragment_shader =
"#version 300 es \n"
"in vec2 UV;                                                      \n"
"                                                                 \n"
"layout(location = 0) out lowp vec4 color;                        \n"
"                                                                 \n"
"uniform sampler2D renderedTexture;                               \n"
"                                                                 \n"
"void main(){                                                     \n"
"    color = texture( renderedTexture, vec2(UV.x, 1.0 - UV.y)).bgra;    \n"
"}                                                                \n"
;

static const GLfloat g_quad_vertex_buffer_data[] = {
    -1.0f, -1.0f, 0.0f,
    1.0f, -1.0f, 0.0f,
    -1.0f,  1.0f, 0.0f,
    -1.0f,  1.0f, 0.0f,
    1.0f, -1.0f, 0.0f,
    1.0f,  1.0f, 0.0f,
};


bool isGLError(const char* test)
{
	GLenum errCode;
	const char* errString;

	if ((errCode = glGetError()) != GL_NO_ERROR) {
			__android_log_print(ANDROID_LOG_ERROR, "RefreshScreen", "GL ERROR data=%s, code=%d", test,errCode);
		return true;
	}
	return false;
}

GLuint gPBO = 0;
GLuint gFBO = 0;
GLenum gDrawBuffers[1] = {GL_COLOR_ATTACHMENT0};
GLuint gProgramId = 0;
GLuint gTexID = 0;
GLuint gQuad_vertexbuffer;

struct CachedTexture
{
	GLuint	glName;
	float	offsetS, offsetT;
	uint16_t		line;
	uint16_t		size;
	uint32_t		tMem;
	uint32_t		palette;
	uint16_t		width, height;			  // N64 width and height
	uint16_t		clampWidth, clampHeight;  // Size to clamp to
	uint16_t		realWidth, realHeight;	  // Actual texture size
	float		scaleS, scaleT;			  // Scale to map to 0.0-1.0
	float		shiftScaleS, shiftScaleT; // Scale to shift
	uint32_t		textureBytes;
	enum {
		fbNone = 0,
		fbOneSample = 1,
		fbMultiSample = 2
	} frameBufferTexture;
} gTexture;

static const GLsizei nShaderLogSize = 1024;
bool checkShaderCompileStatus(GLuint obj)
{
	GLint status;
	glGetShaderiv(obj, GL_COMPILE_STATUS, &status);
	if (status == GL_FALSE) {
		GLchar shader_log[nShaderLogSize];
		GLsizei nLogSize = nShaderLogSize;
		glGetShaderInfoLog(obj, nShaderLogSize, &nLogSize, shader_log);
		shader_log[nLogSize] = 0;
        __android_log_print(ANDROID_LOG_ERROR, "RefreshScreen", "shader_compile error: %s", shader_log);
		return false;
	}
	return true;
}

GLuint SetupShaders(void)
{
	GLuint VertexShaderID = glCreateShader(GL_VERTEX_SHADER);
	GLuint FragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);

	glShaderSource(VertexShaderID, 1, &vertex_shader, NULL);
    glCompileShader(VertexShaderID);
    checkShaderCompileStatus(VertexShaderID);

    glShaderSource(FragmentShaderID, 1, &fragment_shader, NULL);
    glCompileShader(FragmentShaderID);
    checkShaderCompileStatus(FragmentShaderID);

	GLuint ProgramID = glCreateProgram();
	glAttachShader(ProgramID, VertexShaderID);
	glAttachShader(ProgramID, FragmentShaderID);
	glLinkProgram(ProgramID);

	glDetachShader(ProgramID, VertexShaderID);
	glDetachShader(ProgramID, FragmentShaderID);

	glDeleteShader(VertexShaderID);
	glDeleteShader(FragmentShaderID);

	return ProgramID;
}

void RefreshScreenInit(void)
{
	// Enable depth test
	glEnable(GL_DEPTH_TEST);
	// Accept fragment if it closer to the camera than the former one
	glDepthFunc(GL_LESS);

	// Cull triangles which normal is not towards the camera
	glEnable(GL_CULL_FACE);

    //Setup texture
	glGenTextures(1, &gTexture.glName);
	gTexture.frameBufferTexture = fbOneSample;
	gTexture.realWidth = 640;
	gTexture.realHeight = 480;
	gTexture.textureBytes = gTexture.realWidth * gTexture.realHeight * 4;
	glBindTexture( GL_TEXTURE_2D, gTexture.glName );
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, gTexture.realWidth, gTexture.realHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
	glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR );
	glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR );

	// Generate Pixel Buffer Object
	glGenBuffers(1, &gPBO);
	glGenFramebuffers(1, &gFBO);
	glBindFramebuffer(GL_DRAW_FRAMEBUFFER, gFBO);

    //Setup shader
    gProgramId = SetupShaders();

    glUseProgram(gProgramId);
    gTexID = glGetUniformLocation(gProgramId, "renderedTexture");

    //Setup vertexes
    glGenBuffers(1, &gQuad_vertexbuffer);
    glBindBuffer(GL_ARRAY_BUFFER, gQuad_vertexbuffer);
    glBufferData(GL_ARRAY_BUFFER, sizeof(g_quad_vertex_buffer_data), g_quad_vertex_buffer_data, GL_STATIC_DRAW);
}

void RefreshScreenDestroy(void)
{
	glDeleteTextures( 1, &gTexture.glName );

	if (gPBO != 0) {
		glDeleteBuffers(1, &gPBO);
		gPBO = 0;
	}

	if (gFBO != 0) {
		glDeleteFramebuffers(1, &gFBO);
		gFBO = 0;
	}
}

void RefreshScreen(const void *data, unsigned width, unsigned height, size_t pitch)
{
    //Draw a textured quad with graphics
	gTexture.width = width;
	gTexture.height = height;
	const uint32_t dataSize = width*height * 4;
	glBindBuffer(GL_PIXEL_UNPACK_BUFFER, gPBO);
	glBufferData(GL_PIXEL_UNPACK_BUFFER, dataSize, NULL, GL_DYNAMIC_DRAW);
	GLubyte* ptr = (GLubyte*)glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, dataSize, GL_MAP_WRITE_BIT);

	if (ptr == NULL)
	    return;

	uint32_t* dst = (uint32_t*)ptr;

	memcpy(dst, data, dataSize);

	glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER); // release the mapped buffer
	glBindTexture(GL_TEXTURE_2D, gTexture.glName);
	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 0);
	glActiveTexture(GL_TEXTURE0);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
	// Set clamping modes
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	//glBindFramebuffer(GL_DRAW_FRAMEBUFFER, gFBO);
	glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

// Render to the screen
        // Render on the whole framebuffer, complete from the lower left corner to the upper right
    //Draw on screen
	glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
	glViewport(0,0,640,480);

	// Clear the screen
	glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	// Use our shader
	glUseProgram(gProgramId);

	// Bind our texture in Texture Unit 0
	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, gTexture.glName);
	// Set our "renderedTexture" sampler to user Texture Unit 0
	glUniform1i(gTexID, 0);

    //Draw the vertices
    glEnableVertexAttribArray(0);
    glBindBuffer(GL_ARRAY_BUFFER, gQuad_vertexbuffer);
    glVertexAttribPointer(
			0,                  // attribute 0. No particular reason for 0, but must match the layout in the shader.
			3,                  // size
			GL_FLOAT,           // type
			GL_FALSE,           // normalized?
			0,                  // stride
			(void*)0            // array buffer offset
    );

    // Draw the triangles !
    glDrawArrays(GL_TRIANGLES, 0, 6); // 2*3 indices starting at 0 -> 2 triangles

    glDisableVertexAttribArray(0);

}

