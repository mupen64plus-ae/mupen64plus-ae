#include "gl_screen.h"
#include <GLES3/gl3.h>
#include <memory.h>
#include <malloc.h>

#include "core/msg.h"

bool isGLError(const char* test)
{
    GLenum errCode;

    if ((errCode = glGetError()) != GL_NO_ERROR) {
        msg_error("GL ERROR data=%s, code=%#08x", test,errCode);
        return true;
    } else {
        msg_warning("NO ERROR data=%s", test);
    }
    return false;
}

static const char* vertex_shader =
        "#version 300 es \n"
        "layout(location = 0) in vec3 vertexPosition_modelspace;           \n"
        "                                                                  \n"
        "out lowp vec2 UV;                                                 \n"
        "                                                                  \n"
        "void main(){                                                      \n"
        "    gl_Position = vec4(vertexPosition_modelspace,1);               \n"
        "    UV = (vertexPosition_modelspace.xy+vec2(1,1))/2.0;             \n"
        "}                                                                 \n"
;

static const char* fragment_shader =
        "#version 300 es \n"
        "in lowp vec2 UV;                                                 \n"
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

GLuint gPBO = 0;
GLuint gFBO = 0;
GLuint gProgramId = 0;
GLint gTexID = 0;
GLuint gQuad_vertexbuffer;

struct CachedTexture
{
    GLuint    glName;
    int32_t width;
    int32_t height;
    int32_t render_width;
    int32_t render_height;
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
        msg_error("shader_compile error: %s", shader_log);
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

void gl_screen_init(struct rdp_config* config)
{
    // Enable depth test
    glEnable(GL_DEPTH_TEST);
    // Accept fragment if it closer to the camera than the former one
    glDepthFunc(GL_LESS);

    // Cull triangles which normal is not towards the camera
    glEnable(GL_CULL_FACE);

    //Setup texture
    glGenTextures(1, &gTexture.glName);
    gTexture.width = 640;
    gTexture.height = 480;
    gTexture.render_width = 640;
    gTexture.render_height = 480;
    glBindTexture( GL_TEXTURE_2D, gTexture.glName );
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, gTexture.width, gTexture.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);

    // select interpolation method
    GLint filter;
    switch (config->vi.interp) {
        case VI_INTERP_LINEAR:
            filter = GL_LINEAR;
            break;
        case VI_INTERP_NEAREST:
        default:
            filter = GL_NEAREST;
    }
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);

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

bool gl_screen_write(struct rdp_frame_buffer* fb, int32_t output_height)
{
    bool buffer_size_changed = gTexture.width != fb->width || gTexture.height != fb->height;

    // check if the framebuffer size has changed
    if (buffer_size_changed) {
        //Draw a textured quad with graphics
        gTexture.width = fb->width;
        gTexture.height = fb->height;

        glBindTexture(GL_TEXTURE_2D, gTexture.glName);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, gTexture.width, gTexture.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);

        msg_warning("screen: resized framebuffer texture: %d, %d", fb->width, fb->height);
    }

    // copy local buffer to GPU texture buffer
    const int32_t dataSize = fb->width*fb->height * 4;
    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, gPBO);
    glBufferData(GL_PIXEL_UNPACK_BUFFER, dataSize, NULL, GL_DYNAMIC_DRAW);

    GLubyte* ptr = (GLubyte*)glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, dataSize, GL_MAP_WRITE_BIT);

    if (ptr == NULL) {
        msg_warning("Failed call to glMapBufferRange");
        return false;
    }

    uint32_t* dst = (uint32_t*)ptr;
    memcpy(dst, fb->pixels, dataSize);

    glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER); // release the mapped buffer

    glBindTexture(GL_TEXTURE_2D, gTexture.glName);

    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, gTexture.width, gTexture.height, GL_RGBA, GL_UNSIGNED_BYTE, 0);
    glActiveTexture(GL_TEXTURE0);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);

    // Set clamping modes
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

    return buffer_size_changed;

}

void gl_screen_read(struct rdp_frame_buffer* fb, bool rgb)
{
    fb->width = (uint32_t)gTexture.render_width;
    fb->height = (uint32_t)gTexture.render_height;
    fb->pitch = (uint32_t)gTexture.render_width;

    if (!fb->pixels) {
        return;
    }

    char *pBufferData = (char*)malloc(fb->width*fb->height*4);
    char *pDest = (char*)fb->pixels;

    glReadPixels(0, 0, gTexture.render_width, gTexture.render_height, GL_RGBA, GL_UNSIGNED_BYTE, pBufferData);

    //Convert RGBA to RGB
    for (int32_t y = 0; y < fb->height; ++y) {
        char *ptr = pBufferData + (fb->width * 4 * y);
        for (int32_t x = 0; x < fb->width; ++x) {
            pDest[x * 3] = ptr[0]; // red
            pDest[x * 3 + 1] = ptr[1]; // green
            pDest[x * 3 + 2] = ptr[2]; // blue
            ptr += 4;
        }
        pDest += fb->width * 3;
    }

    free(pBufferData);
}

void gl_screen_render(int32_t win_width, int32_t win_height, int32_t win_x, int32_t win_y)
{
    gTexture.render_width = win_width;
    gTexture.render_height = win_height;

    // Render to the screen
    // Render on the whole framebuffer, complete from the lower left corner to the upper right
    // Draw on screen
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    glViewport(0,0,win_width, win_height);

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

void gl_screen_clear(void)
{
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
}

void gl_screen_close(void)
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
