#include "gfx_1.3.h"
#include "config.h"
#include "resource.h"

#include "core/rdp.h"
#include "core/screen.h"
#include "core/version.h"
#include "core/msg.h"
#include "core/plugin.h"

#include <stdio.h>

static bool warn_hle;
static HINSTANCE hinst;
static char screenshot_path[MAX_PATH];

GFX_INFO gfx;

static void write_screenshot(char* path)
{
    struct rdp_frame_buffer fb = { 0 };
    screen_read(&fb, false);

    // prepare bitmap headers
    BITMAPINFOHEADER ihdr = {0};
    ihdr.biSize = sizeof(ihdr);
    ihdr.biWidth = fb.width;
    ihdr.biHeight = fb.height;
    ihdr.biPlanes = 1;
    ihdr.biBitCount = 32;
    ihdr.biSizeImage = fb.width * fb.height * sizeof(int32_t);

    BITMAPFILEHEADER fhdr = {0};
    fhdr.bfType = 'B' | ('M' << 8);
    fhdr.bfOffBits = sizeof(fhdr) + sizeof(ihdr) + 10;
    fhdr.bfSize = ihdr.biSizeImage + fhdr.bfOffBits;

    FILE* fp = fopen(path, "wb");

    if (!fp) {
        msg_warning("Can't open screenshot file %s!", path);
        return;
    }

    // write bitmap headers
    fwrite(&fhdr, sizeof(fhdr), 1, fp);
    fwrite(&ihdr, sizeof(ihdr), 1, fp);

    // write bitmap contents
    fseek(fp, fhdr.bfOffBits, SEEK_SET);

    fb.pixels = malloc(ihdr.biSizeImage);
    screen_read(&fb, false);
    fwrite(fb.pixels, ihdr.biSizeImage, 1, fp);
    free(fb.pixels);

    fclose(fp);
}

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved)
{
    switch (fdwReason) {
        case DLL_PROCESS_ATTACH:
            config_init(hinstDLL);
            break;
    }
    return TRUE;
}

EXPORT void CALL CaptureScreen(char* directory)
{
    char rom_name[128];
    plugin_get_rom_name(rom_name, sizeof(rom_name));

    for (int32_t i = 0; i < 10000; i++) {
        sprintf(screenshot_path, "%s\\%s_%04d.bmp", directory, rom_name, i);
        FILE* fp = fopen(screenshot_path, "rb");
        if (!fp) {
            break;
        }
        fclose(fp);
    }
}

EXPORT void CALL ChangeWindow(void)
{
    screen_toggle_fullscreen();
}

EXPORT void CALL CloseDLL(void)
{
}

EXPORT void CALL DllAbout(HWND hParent)
{
    msg_warning(
        CORE_NAME "\n\n"
        "Build commit:\n"
        GIT_BRANCH "\n"
        GIT_COMMIT_HASH "\n"
        GIT_COMMIT_DATE "\n\n"
        "Build date: " __DATE__ " " __TIME__ "\n\n"
        "https://github.com/ata4/angrylion-rdp-plus"
    );
}

EXPORT void CALL DllConfig(HWND hParent)
{
    config_dialog(hParent);
}

EXPORT void CALL ReadScreen(void **dest, long *width, long *height)
{
}

EXPORT void CALL DrawScreen(void)
{
}

EXPORT void CALL GetDllInfo(PLUGIN_INFO* PluginInfo)
{
    PluginInfo->Version = 0x0103;
    PluginInfo->Type  = PLUGIN_TYPE_GFX;
    sprintf(PluginInfo->Name, CORE_NAME);

    PluginInfo->NormalMemory = TRUE;
    PluginInfo->MemoryBswaped = TRUE;
}

EXPORT BOOL CALL InitiateGFX(GFX_INFO Gfx_Info)
{
    gfx = Gfx_Info;

    return TRUE;
}

EXPORT void CALL MoveScreen(int xpos, int ypos)
{
}

EXPORT void CALL ProcessDList(void)
{
    if (!warn_hle) {
        msg_warning("Please disable 'Graphic HLE' in the plugin settings.");
        warn_hle = true;
    }
}

EXPORT void CALL ProcessRDPList(void)
{
    rdp_update();
}

EXPORT void CALL RomClosed(void)
{
    rdp_close();
}

EXPORT void CALL RomOpen(void)
{
    config_load();
    rdp_init(config_get());
}

EXPORT void CALL ShowCFB(void)
{
}

EXPORT void CALL UpdateScreen(void)
{
    rdp_update_vi();

    // write screenshot file if requested
    if (screenshot_path[0]) {
        write_screenshot(screenshot_path);
        screenshot_path[0] = 0;
    }
}

EXPORT void CALL ViStatusChanged(void)
{
}

EXPORT void CALL ViWidthChanged(void)
{
}

EXPORT void CALL FBWrite(DWORD addr, DWORD val)
{
}

EXPORT void CALL FBWList(FrameBufferModifyEntry *plist, DWORD size)
{
}

EXPORT void CALL FBRead(DWORD addr)
{
}

EXPORT void CALL FBGetFrameBufferInfo(void *pinfo)
{
}
