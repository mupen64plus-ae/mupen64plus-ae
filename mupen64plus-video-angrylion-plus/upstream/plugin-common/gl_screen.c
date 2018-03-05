#include "gl_screen.h"
#include "gl_core_3_3.h"

#include "core/msg.h"

#include <stdlib.h>
#include <string.h>

// supposedly, these settings are most hardware-friendly on all platforms
#define TEX_INTERNAL_FORMAT GL_RGBA8
#define TEX_FORMAT GL_BGRA
#define TEX_TYPE GL_UNSIGNED_INT_8_8_8_8_REV

static GLuint program;
static GLuint vao;
static GLuint texture;

static int32_t tex_width;
static int32_t tex_height;

static int32_t tex_display_height;

static void gl_check_errors(void)
{
#ifdef _DEBUG
    GLenum err;
    static int32_t invalid_op_count = 0;
    while ((err = glGetError()) != GL_NO_ERROR) {
        // if gl_check_errors is called from a thread with no valid
        // GL context, it would be stuck in an infinite loop here, since
        // glGetError itself causes GL_INVALID_OPERATION, so check for a few
        // cycles and abort if there are too many errors of that kind
        if (err == GL_INVALID_OPERATION) {
            if (++invalid_op_count >= 100) {
                msg_error("gl_check_errors: invalid OpenGL context!");
            }
        } else {
            invalid_op_count = 0;
        }

        char* err_str;
        switch (err) {
            case GL_INVALID_OPERATION:
                err_str = "INVALID_OPERATION";
                break;
            case GL_INVALID_ENUM:
                err_str = "INVALID_ENUM";
                break;
            case GL_INVALID_VALUE:
                err_str = "INVALID_VALUE";
                break;
            case GL_OUT_OF_MEMORY:
                err_str = "OUT_OF_MEMORY";
                break;
            case GL_INVALID_FRAMEBUFFER_OPERATION:
                err_str = "INVALID_FRAMEBUFFER_OPERATION";
                break;
            default:
                err_str = "unknown";
        }
        msg_debug("gl_check_errors: %d (%s)", err, err_str);
    }
#endif
}

static GLuint gl_shader_compile(GLenum type, const GLchar* source)
{
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, NULL);
    glCompileShader(shader);

    GLint param;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &param);

    if (!param) {
        GLchar log[4096];
        glGetShaderInfoLog(shader, sizeof(log), NULL, log);
        msg_error("%s shader error: %s\n", type == GL_FRAGMENT_SHADER ? "Frag" : "Vert", log);
    }

    return shader;
}

static GLuint gl_shader_link(GLuint vert, GLuint frag)
{
    GLuint program = glCreateProgram();
    glAttachShader(program, vert);
    glAttachShader(program, frag);
    glLinkProgram(program);

    GLint param;
    glGetProgramiv(program, GL_LINK_STATUS, &param);

    if (!param) {
        GLchar log[4096];
        glGetProgramInfoLog(program, sizeof(log), NULL, log);
        msg_error("Shader link error: %s\n", log);
    }

    glDeleteShader(frag);
    glDeleteShader(vert);

    return program;
}

void gl_screen_init(struct rdp_config* config)
{
    // shader sources for drawing a clipped full-screen triangle. the geometry
    // is defined by the vertex ID, so a VBO is not required.
    const GLchar* vert_shader =
        "#version 330 core\n"
        "out vec2 uv;\n"
        "void main(void) {\n"
        "    uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);\n"
        "    gl_Position = vec4(uv * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 0.0, 1.0);\n"
        "}\n";

    const GLchar* frag_shader =
        "#version 330 core\n"
        "in vec2 uv;\n"
        "layout(location = 0) out vec4 color;\n"
        "uniform sampler2D tex0;\n"
        "void main(void) {\n"
        "    color = texture(tex0, uv);\n"
        "}\n";

    // compile and link OpenGL program
    GLuint vert = gl_shader_compile(GL_VERTEX_SHADER, vert_shader);
    GLuint frag = gl_shader_compile(GL_FRAGMENT_SHADER, frag_shader);
    program = gl_shader_link(vert, frag);
    glUseProgram(program);

    // prepare dummy VAO
    glGenVertexArrays(1, &vao);
    glBindVertexArray(vao);

    // prepare texture
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);

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

    // check if there was an error when using any of the commands above
    gl_check_errors();
}

bool gl_screen_write(struct rdp_frame_buffer* fb, int32_t output_height)
{
    bool buffer_size_changed = tex_width != fb->width || tex_height != fb->height;

    // check if the framebuffer size has changed
    if (buffer_size_changed) {
        tex_width = fb->width;
        tex_height = fb->height;

        // set pitch for all unpacking operations
        glPixelStorei(GL_UNPACK_ROW_LENGTH, fb->pitch);

        // reallocate texture buffer on GPU
        glTexImage2D(GL_TEXTURE_2D, 0, TEX_INTERNAL_FORMAT, tex_width,
            tex_height, 0, TEX_FORMAT, TEX_TYPE, fb->pixels);

        msg_debug("screen: resized framebuffer texture: %dx%d", tex_width, tex_height);
    } else {
        // copy local buffer to GPU texture buffer
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, tex_width, tex_height,
            TEX_FORMAT, TEX_TYPE, fb->pixels);
    }

    // update output size
    tex_display_height = output_height;

    return buffer_size_changed;
}

void gl_screen_read(struct rdp_frame_buffer* fb)
{
    fb->width = tex_width;
    fb->height = tex_display_height;
    fb->pitch = tex_width;

    if (!fb->pixels) {
        return;
    }

    // check if resampling is required
    if (tex_display_height == tex_height) {
        // size matches, direct copy
        glGetTexImage(GL_TEXTURE_2D, 0, TEX_FORMAT, TEX_TYPE, (void*)fb->pixels);
    } else {
        // do nearest-neighbor resampling
        int32_t* tex_buffer = malloc(tex_width * tex_display_height * sizeof(int32_t));
        glGetTexImage(GL_TEXTURE_2D, 0, TEX_FORMAT, TEX_TYPE, tex_buffer);

        for (int32_t y = 0; y < tex_display_height; y++) {
            int32_t iy = y * tex_height / tex_display_height;
            uint32_t os = tex_width * iy;
            uint32_t od = tex_width * y;
            memcpy(fb->pixels + od, tex_buffer + os, tex_width * sizeof(int32_t));
        }

        free(tex_buffer);
    }
}

void gl_screen_render(int32_t win_width, int32_t win_height, int32_t win_x, int32_t win_y)
{
    int32_t hw = tex_display_height * win_width;
    int32_t wh = tex_width * win_height;

    // add letterboxes or pillarboxes if the window has a different aspect ratio
    // than the current display mode
    if (hw > wh) {
        int32_t w_max = wh / tex_display_height;
        win_x += (win_width - w_max) / 2;
        win_width = w_max;
    } else if (hw < wh) {
        int32_t h_max = hw / tex_width;
        win_y += (win_height - h_max) / 2;
        win_height = h_max;
    }

    // configure viewport
    glViewport(win_x, win_y, win_width, win_height);

    // draw fullscreen triangle
    glDrawArrays(GL_TRIANGLES, 0, 3);

    // check if there was an error when using any of the commands above
    gl_check_errors();
}

void gl_screen_close(void)
{
    tex_width = 0;
    tex_height = 0;

    tex_display_height = 0;

    glDeleteTextures(1, &texture);
    glDeleteVertexArrays(1, &vao);
    glDeleteProgram(program);
}
