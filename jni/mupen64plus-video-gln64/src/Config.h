#ifndef CONFIG_H
#define CONFIG_H

struct Config
{
    int     version;

    struct
    {
        int flipVertical;
    } screen;

    struct
    {
        int xpos, ypos, width, height, refwidth, refheight;
    } window;

    struct
    {
        int enable, bilinear;
        int xpos, ypos, width, height;
    } framebuffer;

    struct
    {
        int force, width, height;
    } video;

    struct
    {
        int maxAnisotropy;
        int enableMipmap;
        int forceBilinear;
        int sai2x;
        int useIA;
        int fastCRC;
        int pow2;
    } texture;

    int     logFrameRate;
    int     updateMode;
    int     forceBufferClear;
    int     ignoreOffscreenRendering;
    int     zHack;

    int     autoFrameSkip;
    int     maxFrameSkip;
    int     targetFPS;
    int     frameRenderRate;
    int     verticalSync;

    int     enableFog;
    int     enablePrimZ;
    int     enableLighting;
    int     enableAlphaTest;
    int     enableClipping;
    int     enableFaceCulling;
    int     enableNoise;

// paulscode: removed from pre-compile to a config option
//// (part of the Galaxy S Zelda crash-fix
    int     tribufferOpt;
//

    int     hackBanjoTooie;
    int     hackZelda;
    int     hackAlpha;

    bool    stretchVideo;
    bool    romPAL;    //is the rom PAL
    char    romName[21];
};

extern Config config;

void Config_LoadConfig();
void Config_LoadRomConfig(unsigned char* header);
#endif

