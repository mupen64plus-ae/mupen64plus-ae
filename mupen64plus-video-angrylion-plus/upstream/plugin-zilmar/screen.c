#include "gfx_1.3.h"

#include "plugin-common/gl_screen.h"
#include "plugin-common/gl_core_3_3.h"
#include "wgl_ext.h"

#include "core/screen.h"
#include "core/msg.h"

#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>

extern GFX_INFO gfx;

// default size of the window
#define WINDOW_DEFAULT_WIDTH 640
#define WINDOW_DEFAULT_HEIGHT 480

// context states
static HDC dc;
static HGLRC glrc;
static HGLRC glrc_core;
static bool fullscreen;

// Win32 helpers
void win32_client_resize(HWND hWnd, HWND hStatus, int32_t nWidth, int32_t nHeight)
{
    RECT rclient;
    GetClientRect(hWnd, &rclient);

    RECT rwin;
    GetWindowRect(hWnd, &rwin);

    if (hStatus) {
        RECT rstatus;
        GetClientRect(hStatus, &rstatus);

        rclient.bottom -= rstatus.bottom;
    }

    POINT pdiff;
    pdiff.x = (rwin.right - rwin.left) - rclient.right;
    pdiff.y = (rwin.bottom - rwin.top) - rclient.bottom;

    MoveWindow(hWnd, rwin.left, rwin.top, nWidth + pdiff.x, nHeight + pdiff.y, TRUE);
}

void screen_init(struct rdp_config* config)
{
    // make window resizable for the user
    if (!fullscreen) {
        LONG style = GetWindowLong(gfx.hWnd, GWL_STYLE);
        style |= WS_SIZEBOX | WS_MAXIMIZEBOX;
        SetWindowLong(gfx.hWnd, GWL_STYLE, style);

        BOOL zoomed = IsZoomed(gfx.hWnd);

        if (zoomed) {
            ShowWindow(gfx.hWnd, SW_RESTORE);
        }

        // Fix client size after changing the window style, otherwise the PJ64
        // menu will be displayed incorrectly.
        // For some reason, this needs to be called twice, probably because the
        // style set above isn't applied immediately.
        for (int i = 0; i < 2; i++) {
            win32_client_resize(gfx.hWnd, gfx.hStatusBar, WINDOW_DEFAULT_WIDTH, WINDOW_DEFAULT_HEIGHT);
        }

        if (zoomed) {
            ShowWindow(gfx.hWnd, SW_MAXIMIZE);
        }
    }

    PIXELFORMATDESCRIPTOR win_pfd = {
        sizeof(PIXELFORMATDESCRIPTOR), 1,
        PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER, // Flags
        PFD_TYPE_RGBA, // The kind of framebuffer. RGBA or palette.
        32,            // Colordepth of the framebuffer.
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        24, // Number of bits for the depthbuffer
        8,  // Number of bits for the stencilbuffer
        0,  // Number of Aux buffers in the framebuffer.
        PFD_MAIN_PLANE, 0, 0, 0, 0
    };

    dc = GetDC(gfx.hWnd);
    if (!dc) {
        msg_error("Can't get device context.");
    }

    int32_t win_pf = ChoosePixelFormat(dc, &win_pfd);
    if (!win_pf) {
        msg_error("Can't choose pixel format.");
    }
    SetPixelFormat(dc, win_pf, &win_pfd);

    // create legacy context, required for wglGetProcAddress to work properly
    glrc = wglCreateContext(dc);
    if (!glrc || !wglMakeCurrent(dc, glrc)) {
        msg_error("Can't create OpenGL context.");
    }

    // attributes for a 3.3 core profile without all the legacy stuff
    GLint attribs[] = {
        WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
        WGL_CONTEXT_MINOR_VERSION_ARB, 3,
        WGL_CONTEXT_PROFILE_MASK_ARB, WGL_CONTEXT_CORE_PROFILE_BIT_ARB,
        0
    };

    // create the actual context
    glrc_core = wglCreateContextAttribsARB(dc, glrc, attribs);
    if (!glrc_core || !wglMakeCurrent(dc, glrc_core)) {
        // rendering probably still works with the legacy context, so just send
        // a warning
        msg_warning("Can't create OpenGL 3.3 core context.");
    }

    gl_screen_init(config);
}

void screen_write(struct rdp_frame_buffer* buffer, int32_t output_height)
{
    gl_screen_write(buffer, output_height);
}

void screen_read(struct rdp_frame_buffer* buffer, bool rgb)
{
    gl_screen_read(buffer, rgb);
}

void screen_swap(bool blank)
{
    // don't render when the window is minimized
    if (IsIconic(gfx.hWnd)) {
        return;
    }

    // clear current buffer, indicating the start of a new frame
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    RECT rect;
    GetClientRect(gfx.hWnd, &rect);

    // status bar covers the client area, so exclude it from calculation
    RECT statusrect;
    SetRectEmpty(&statusrect);

    if (gfx.hStatusBar) {
        GetClientRect(gfx.hStatusBar, &statusrect);
        rect.bottom -= statusrect.bottom;
    }

    int32_t win_width = rect.right - rect.left;
    int32_t win_height = rect.bottom - rect.top;

    // default to bottom left corner of the window above the status bar
    int32_t win_x = 0;
    int32_t win_y = statusrect.bottom;

    if (!blank) {
        gl_screen_render(win_width, win_height, win_x, win_y);
    }

    // swap front and back buffers
    SwapBuffers(dc);
}

void screen_set_fullscreen(bool _fullscreen)
{
    static HMENU old_menu;
    static LONG old_style;
    static WINDOWPLACEMENT old_pos;

    if (_fullscreen) {
        // hide curser
        ShowCursor(FALSE);

        // hide status bar
        if (gfx.hStatusBar) {
            ShowWindow(gfx.hStatusBar, SW_HIDE);
        }

        // disable menu and save it to restore it later
        old_menu = GetMenu(gfx.hWnd);
        if (old_menu) {
            SetMenu(gfx.hWnd, NULL);
        }

        // save old window position and size
        GetWindowPlacement(gfx.hWnd, &old_pos);

        // use virtual screen dimensions for fullscreen mode
        int32_t vs_width = GetSystemMetrics(SM_CXSCREEN);
        int32_t vs_height = GetSystemMetrics(SM_CYSCREEN);

        // disable all styles to get a borderless window and save it to restore
        // it later
        old_style = GetWindowLong(gfx.hWnd, GWL_STYLE);
        SetWindowLong(gfx.hWnd, GWL_STYLE, WS_VISIBLE);

        // resize window so it covers the entire virtual screen
        SetWindowPos(gfx.hWnd, HWND_TOP, 0, 0, vs_width, vs_height, SWP_SHOWWINDOW);
    } else {
        // restore cursor
        ShowCursor(TRUE);

        // restore status bar
        if (gfx.hStatusBar) {
            ShowWindow(gfx.hStatusBar, SW_SHOW);
        }

        // restore menu
        if (old_menu) {
            SetMenu(gfx.hWnd, old_menu);
            old_menu = NULL;
        }

        // restore style
        SetWindowLong(gfx.hWnd, GWL_STYLE, old_style);

        // restore window size and position
        SetWindowPlacement(gfx.hWnd, &old_pos);
    }

    fullscreen = _fullscreen;
}

bool screen_get_fullscreen(void)
{
    return fullscreen;
}

void screen_close(void)
{
    gl_screen_close();

    if (glrc_core) {
        wglDeleteContext(glrc_core);
    }

    wglDeleteContext(glrc);
}
