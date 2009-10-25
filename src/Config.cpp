/*
Copyright (C) 2003 Rice1964

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/

#include <limits.h> // PATH_MAX

#include "m64p_plugin.h"
#include "stdafx.h"

#define INI_FILE        "RiceVideoLinux.ini"
#define CONFIG_FILE     "RiceVideo.cfg"
const char *project_name =  "Rice's Video Plugin";

void GetPluginDir(char *Directory);

// Disable the config dialog box to allow Vtune call graph feature to work
#define ENABLE_CONFIG_DIALOG

const char *frameBufferSettings[] =
{
"None (default)",
"Hide Framebuffer Effects",
"Basic Framebuffer",
"Basic & Write Back",
"Write Back & Reload",
"Write Back Every Frame",
"With Emulator",
"Basic Framebuffer & With Emulator",
"With Emulator Read Only",
"With Emulator Write Only",
};

const int resolutions[][2] =
{
{320, 240},
{400, 300},
{480, 360},
{512, 384},
{640, 480},
{800, 600},
{1024, 768},
{1152, 864},
{1280, 960},
{1400, 1050},
{1600, 1200},
{1920, 1440},
{2048, 1536},
};
const int numberOfResolutions = sizeof(resolutions)/sizeof(int)/2;

const char* resolutionsS[] =
{
"320 x 240",
"400 x 300",
"480 x 360",
"512 x 384",
"640 x 480",
"800 x 600",
"1024 x 768",
"1152 x 864",
"1280 x 960",
"1400 x 1050",
"1600 x 1200",
"1920 x 1440",
"2048 x 1536"
};

const char *frameBufferWriteBackControlSettings[] =
{
"Every Frame (default)",
"Every 2 Frames",
"Every 3 Frames",
"Every 4 Frames",
"Every 5 Frames",
"Every 6 Frames",
"Every 7 Frames",
"Every 8 Frames",
};

const char *renderToTextureSettings[] =
{
"None (default)",
"Hide Render-to-texture Effects",
"Basic Render-to-texture",
"Basic & Write Back",
"Write Back & Reload",
};

const char *screenUpdateSettings[] =
{
"At VI origin update",
"At VI origin change",
"At CI change",
"At the 1st CI change",
"At the 1st drawing",
"Before clear the screen",
"At VI origin update after screen is drawn (default)",
};

WindowSettingStruct windowSetting;
GlobalOptions options;
RomOptions defaultRomOptions;
RomOptions currentRomOptions;
FrameBufferOptions frameBufferOptions;
std::vector<IniSection> IniSections;
bool    bIniIsChanged = false;
char    szIniFileName[300];

RenderEngineSetting RenderEngineSettings[] =
{
{"DirectX", DIRECTX_DEVICE},
{"OpenGL", OGL_DEVICE},
};

SettingInfo TextureQualitySettings[] =
{
{"Default", FORCE_DEFAULT_FILTER},
{"32-bit Texture", FORCE_POINT_FILTER},
{"16-bit Texture", FORCE_LINEAR_FILTER},
};

SettingInfo ForceTextureFilterSettings[] =
{
{"N64 Default Texture Filter",  FORCE_DEFAULT_FILTER},
{"Force Nearest Filter (faster, low quality)", FORCE_POINT_FILTER},
{"Force Linear Filter (slower, better quality)", FORCE_LINEAR_FILTER},
//{"Force Bilinear Filter slower, best quality", FORCE_BILINEAR_FILTER}, 
};

SettingInfo TextureEnhancementSettings[] =
{
{"N64 original texture (No enhancement)", TEXTURE_NO_ENHANCEMENT},
{"2x (Double the texture size)", TEXTURE_2X_ENHANCEMENT},
{"2xSaI", TEXTURE_2XSAI_ENHANCEMENT},
{"hq2x", TEXTURE_HQ2X_ENHANCEMENT},
{"lq2x", TEXTURE_LQ2X_ENHANCEMENT},
{"hq4x", TEXTURE_HQ4X_ENHANCEMENT},
{"Sharpen", TEXTURE_SHARPEN_ENHANCEMENT},
{"Sharpen More", TEXTURE_SHARPEN_MORE_ENHANCEMENT},
};

SettingInfo TextureEnhancementControlSettings[] =
{
{"Normal", TEXTURE_ENHANCEMENT_NORMAL},
{"Smooth", TEXTURE_ENHANCEMENT_WITH_SMOOTH_FILTER_1},
{"Less smooth", TEXTURE_ENHANCEMENT_WITH_SMOOTH_FILTER_2},
{"2xSaI smooth", TEXTURE_ENHANCEMENT_WITH_SMOOTH_FILTER_3},
{"Less 2xSaI smooth", TEXTURE_ENHANCEMENT_WITH_SMOOTH_FILTER_4},
};

SettingInfo colorQualitySettings[] =
{
{"16-bit", TEXTURE_FMT_A4R4G4B4},
{"32-bit (def)", TEXTURE_FMT_A8R8G8B8},
};

const char* strDXDeviceDescs[] = { "HAL", "REF" };

SettingInfo openGLDepthBufferSettings[] =
{
{"16-bit (def)", 16},
{"32-bit", 32},
};

RenderEngineSetting OpenGLRenderSettings[] =
{
{"To Fit Your Video Card", OGL_DEVICE},
{"OpenGL 1.1 (Lowest)",  OGL_1_1_DEVICE},
{"OpenGL 1.2/1.3", OGL_1_2_DEVICE},
{"OpenGL 1.4", OGL_1_4_DEVICE},
//{"OpenGL 1.4, the 2nd combiner",  OGL_1_4_V2_DEVICE},
{"OpenGL for Nvidia TNT or better", OGL_TNT2_DEVICE},
{"OpenGL for Nvidia GeForce or better ", NVIDIA_OGL_DEVICE},
{"OpenGL Fragment Program Extension", OGL_FRAGMENT_PROGRAM},
};

BufferSettingInfo DirectXCombinerSettings[] =
{
{"To Fit Your Video Card", DX_BEST_FIT, DX_BEST_FIT},
{"For Low End Video Cards", DX_LOW_END, DX_LOW_END},
{"For High End Video Cards", DX_HIGH_END, DX_HIGH_END},
{"For NVidia TNT/TNT2/Geforce/GF2", DX_NVIDIA_TNT, DX_NVIDIA_TNT},
{"Limited 2 stage combiner", DX_2_STAGES, DX_2_STAGES},
{"Limited 3 stage combiner", DX_3_STAGES, DX_3_STAGES},
{"Limited 4 stage combiner", DX_4_STAGES, DX_4_STAGES},
{"Pixel Shader", DX_PIXEL_SHADER, DX_PIXEL_SHADER},
{"Semi-Pixel Shader", DX_SEMI_PIXEL_SHADER, DX_SEMI_PIXEL_SHADER},
};

SettingInfo OnScreenDisplaySettings[] =
{
{"Display Nothing", ONSCREEN_DISPLAY_NOTHING},
{"Display DList Per Second", ONSCREEN_DISPLAY_DLIST_PER_SECOND},
{"Display Frame Per Second", ONSCREEN_DISPLAY_FRAME_PER_SECOND},
{"Display Debug Information Only", ONSCREEN_DISPLAY_DEBUG_INFORMATION_ONLY},
{"Display Messages From CPU Core Only", ONSCREEN_DISPLAY_TEXT_FROM_CORE_ONLY},
{"Display DList Per Second With Core Msgs", ONSCREEN_DISPLAY_DLIST_PER_SECOND_WITH_CORE_MSG},
{"Display Frame Per Second With Core Msgs", ONSCREEN_DISPLAY_FRAME_PER_SECOND_WITH_CORE_MSG},
{"Display Debug Information With Core Msgs", ONSCREEN_DISPLAY_DEBUG_INFORMATION_WITH_CORE_MSG},
};

const int numberOfRenderEngineSettings = sizeof(RenderEngineSettings)/sizeof(RenderEngineSetting);
const int numberOfOpenGLRenderEngineSettings = sizeof(OpenGLRenderSettings)/sizeof(RenderEngineSetting);

void WriteConfiguration(void);
void GenerateCurrentRomOptions();

void GenerateFrameBufferOptions(void)
{
    if( CDeviceBuilder::GetGeneralDeviceType() == OGL_DEVICE )
    {
        // OpenGL does not support much yet
        if( currentRomOptions.N64FrameBufferEmuType != FRM_BUF_NONE )
            currentRomOptions.N64FrameBufferEmuType = FRM_BUF_IGNORE;
        if( currentRomOptions.N64RenderToTextureEmuType != TXT_BUF_NONE )
            currentRomOptions.N64RenderToTextureEmuType = TXT_BUF_IGNORE;
    }

    frameBufferOptions.bUpdateCIInfo            = false;

    frameBufferOptions.bCheckBackBufs           = false;
    frameBufferOptions.bWriteBackBufToRDRAM     = false;
    frameBufferOptions.bLoadBackBufFromRDRAM    = false;

    frameBufferOptions.bIgnore                  = true;

    frameBufferOptions.bSupportRenderTextures           = false;
    frameBufferOptions.bCheckRenderTextures         = false;
    frameBufferOptions.bRenderTextureWriteBack          = false;
    frameBufferOptions.bLoadRDRAMIntoRenderTexture      = false;

    frameBufferOptions.bProcessCPUWrite         = false;
    frameBufferOptions.bProcessCPURead          = false;
    frameBufferOptions.bAtEachFrameUpdate       = false;
    frameBufferOptions.bIgnoreRenderTextureIfHeightUnknown      = false;

    switch( currentRomOptions.N64FrameBufferEmuType )
    {
    case FRM_BUF_NONE:
        break;
    case FRM_BUF_COMPLETE:
        frameBufferOptions.bAtEachFrameUpdate       = true;
        frameBufferOptions.bProcessCPUWrite         = true;
        frameBufferOptions.bProcessCPURead          = true;
        frameBufferOptions.bUpdateCIInfo            = true;
        break;
    case FRM_BUF_WRITEBACK_AND_RELOAD:
        frameBufferOptions.bLoadBackBufFromRDRAM    = true;
    case FRM_BUF_BASIC_AND_WRITEBACK:
        frameBufferOptions.bWriteBackBufToRDRAM     = true;
    case FRM_BUF_BASIC:
        frameBufferOptions.bCheckBackBufs           = true;
    case FRM_BUF_IGNORE:
        frameBufferOptions.bUpdateCIInfo            = true;
        break;
    case FRM_BUF_BASIC_AND_WITH_EMULATOR:
        // Banjo Kazooie
        frameBufferOptions.bCheckBackBufs           = true;
    case FRM_BUF_WITH_EMULATOR:
        frameBufferOptions.bUpdateCIInfo            = true;
        frameBufferOptions.bProcessCPUWrite         = true;
        frameBufferOptions.bProcessCPURead          = true;
        break;
    case FRM_BUF_WITH_EMULATOR_READ_ONLY:
        frameBufferOptions.bUpdateCIInfo            = true;
        frameBufferOptions.bProcessCPURead          = true;
        break;
    case FRM_BUF_WITH_EMULATOR_WRITE_ONLY:
        frameBufferOptions.bUpdateCIInfo            = true;
        frameBufferOptions.bProcessCPUWrite         = true;
        break;
    }

    switch( currentRomOptions.N64RenderToTextureEmuType )
    {
    case TXT_BUF_NONE:
        frameBufferOptions.bSupportRenderTextures           = false;
        break;
    case TXT_BUF_WRITE_BACK_AND_RELOAD:
        frameBufferOptions.bLoadRDRAMIntoRenderTexture      = true;
    case TXT_BUF_WRITE_BACK:
        frameBufferOptions.bRenderTextureWriteBack          = true;
    case TXT_BUF_NORMAL:
        frameBufferOptions.bCheckRenderTextures         = true;
        frameBufferOptions.bIgnore                  = false;
    case TXT_BUF_IGNORE:
        frameBufferOptions.bUpdateCIInfo            = true;
        frameBufferOptions.bSupportRenderTextures           = true;
        break;
    }

    if( currentRomOptions.screenUpdateSetting >= SCREEN_UPDATE_AT_CI_CHANGE )
    {
        frameBufferOptions.bUpdateCIInfo = true;
    }
}

BOOL TestRegistry(void)
{
   FILE *f;
   char name[PATH_MAX];
   GetPluginDir(name);
   strcat(name, CONFIG_FILE);
   f = fopen(name, "rb");
   if (!f) return FALSE;
   fclose(f);
   return TRUE;
}

void WriteConfiguration(void)
{
   char name[PATH_MAX];
   GetPluginDir(name);
   strcat(name, CONFIG_FILE);
   FILE *f = fopen(name, "rb");
   if (!f)
     {
    windowSetting.uWindowDisplayWidth=640;
    windowSetting.uWindowDisplayHeight=480;
    windowSetting.uFullScreenDisplayWidth=640;
    windowSetting.uFullScreenDisplayHeight=480;
     }
   else
     fclose(f);
   
   f = fopen(name, "wb");
    
   fprintf(f, "WinModeWidth ");
   fprintf(f, "%d\n", windowSetting.uWindowDisplayWidth);

   fprintf(f, "WinModeHeight ");
   fprintf(f, "%d\n", windowSetting.uWindowDisplayHeight);

   fprintf(f, "FulScreenWidth ");
   fprintf(f, "%d\n", windowSetting.uFullScreenDisplayWidth);
   
   fprintf(f, "FulScreenHeight ");
   fprintf(f, "%d\n", windowSetting.uFullScreenDisplayHeight);
   
   fprintf(f, "EnableHacks ");
   fprintf(f, "%d\n", options.bEnableHacks);
   
   fprintf(f, "FrameBufferSetting ");
   fprintf(f, "%d\n", defaultRomOptions.N64FrameBufferEmuType);
   
   fprintf(f, "FrameBufferWriteBackControl ");
   fprintf(f, "%d\n", defaultRomOptions.N64FrameBufferWriteBackControl);
       
   fprintf(f, "RenderToTexture ");
   fprintf(f, "%d\n", defaultRomOptions.N64RenderToTextureEmuType);
   
   fprintf(f, "ScreenUpdateSetting ");
   fprintf(f, "%d\n", defaultRomOptions.screenUpdateSetting);
   
   fprintf(f, "FPSColor ");
   fprintf(f, "%d\n", options.FPSColor);
   
   fprintf(f, "OpenGLDepthBufferSetting ");
   fprintf(f, "%d\n", options.OpenglDepthBufferSetting);
   
   fprintf(f, "ColorQuality ");
   fprintf(f, "%d\n", options.colorQuality);
   
   fprintf(f, "OpenGLRenderSetting ");
   fprintf(f, "%d\n", options.OpenglRenderSetting);
   
   fprintf(f, "NormalAlphaBlender ");
   fprintf(f, "%d\n", defaultRomOptions.bNormalBlender);
   
   fprintf(f, "EnableFog ");
   fprintf(f, "%d\n", options.bEnableFog);
   
   fprintf(f, "WinFrameMode ");
   fprintf(f, "%d\n", options.bWinFrameMode);
   
   fprintf(f, "FullTMEMEmulation ");
   fprintf(f, "%d\n", options.bFullTMEM);

   fprintf(f, "ForceSoftwareTnL ");
   fprintf(f, "%d\n", options.bForceSoftwareTnL);
   
   fprintf(f, "ForceSoftwareClipper ");
   fprintf(f, "%d\n", options.bForceSoftwareClipper);
   
   fprintf(f, "OpenGLVertexClipper ");
   fprintf(f, "%d\n", options.bOGLVertexClipper);
   
   fprintf(f, "EnableSSE ");
   fprintf(f, "%d\n", options.bEnableSSE);
   
   fprintf(f, "EnableVertexShader ");
   fprintf(f, "%d\n", options.bEnableVertexShader);
   
   fprintf(f, "SkipFrame ");
   fprintf(f, "%d\n", options.bSkipFrame);
   
   fprintf(f, "DisplayTooltip ");
   fprintf(f, "%d\n", options.bDisplayTooltip);
   
   fprintf(f, "HideAdvancedOptions ");
   fprintf(f, "%d\n", options.bHideAdvancedOptions);
   
   fprintf(f, "DisplayOnscreenFPS ");
   fprintf(f, "%d\n", options.bDisplayOnscreenFPS);
   
   fprintf(f, "FrameBufferType ");
   fprintf(f, "%d\n", options.RenderBufferSetting);
   
   fprintf(f, "FulScreenHeight ");
   fprintf(f, "%d\n", windowSetting.uFullScreenDisplayHeight);
   
   fprintf(f, "FastTextureLoading ");
   fprintf(f, "%d\n", defaultRomOptions.bFastTexCRC);
   
   fprintf(f, "RenderEngine ");
   //fprintf(f, "%d\n", (uint32)CDeviceBuilder::GetDeviceType());
   fprintf(f, "%d\n", 0);
   
   fprintf(f, "ForceTextureFilter ");
   fprintf(f, "%d\n", (uint32)options.forceTextureFilter);
   
   fprintf(f, "TextureQuality ");
   fprintf(f, "%d\n", (uint32)options.textureQuality);
   
   fprintf(f, "TexRectOnly ");
   fprintf(f, "%d\n", (uint32)options.bTexRectOnly);
   
   fprintf(f, "SmallTextureOnly ");
   fprintf(f, "%d\n", (uint32)options.bSmallTextureOnly);
   
   fprintf(f, "LoadHiResTextures ");
   fprintf(f, "%d\n", (uint32)options.bLoadHiResTextures);
   
   fprintf(f, "DumpTexturesToFiles ");
   fprintf(f, "%d\n", (uint32)options.bDumpTexturesToFiles);
   
   fprintf(f, "TextureEnhancement ");
   fprintf(f, "%d\n", (uint32)options.textureEnhancement);
   
   fprintf(f, "TextureEnhancementControl ");
   fprintf(f, "%d\n", (uint32)options.textureEnhancementControl);
   
   fprintf(f, "FullScreenFrequency ");
   fprintf(f, "%d\n", (uint32)windowSetting.uFullScreenRefreshRate);
   
   fprintf(f, "AccurateTextureMapping ");
   fprintf(f, "%d\n", (uint32)defaultRomOptions.bAccurateTextureMapping);
   
   fprintf(f, "InN64Resolution ");
   fprintf(f, "%d\n", (uint32)defaultRomOptions.bInN64Resolution);
   
   fprintf(f, "SaveVRAM ");
   fprintf(f, "%d\n", (uint32)defaultRomOptions.bSaveVRAM);
   
   fprintf(f, "OverlapAutoWriteBack ");
   fprintf(f, "%d\n", (uint32)defaultRomOptions.bOverlapAutoWriteBack);
   
   fprintf(f, "DoubleSizeForSmallTxtrBuf ");
   fprintf(f, "%d\n", (uint32)defaultRomOptions.bDoubleSizeForSmallTxtrBuf);
   
   fprintf(f, "ShowFPS ");
   fprintf(f, "%d\n", (uint32)options.bShowFPS);
   
   fclose(f);
}

uint32 ReadRegistryDwordVal(const char *Field)
{
   char name[PATH_MAX];
   GetPluginDir(name);
   strcat(name, CONFIG_FILE);
   FILE *f = fopen(name, "rb");
   if(!f) return 0;
   char buf[0x1000];
   while(fscanf(f, "%s", buf) == 1)
     {
    int dword;
    int n = fscanf(f, "%d", &dword);
    if (n==1)
      {
         if (!strcmp(buf, Field))
           {
          fclose(f);
          return dword;
           }
      }
     }
   fclose(f);
   return 0;
}


bool isMMXSupported() 
{ 
    int IsMMXSupported = 0; 
   
#if defined(__INTEL_COMPILER) && !defined(NO_ASM)
    __asm 
    { 
        mov eax,1   // CPUID level 1 
        cpuid       // EDX = feature flag 
        and edx,0x800000        // test bit 23 of feature flag 
        mov IsMMXSupported,edx  // != 0 if MMX is supported 
    } 
#elif defined(__GNUC__) && defined(__x86_64__) && !defined(NO_ASM)
  return true;
#elif !defined(NO_ASM) // GCC assumed
   asm volatile (
         "push %%ebx           \n"
         "mov $1, %%eax        \n"  // CPUID level 1 
         "cpuid                \n"      // EDX = feature flag 
         "and $0x800000, %%edx \n"      // test bit 23 of feature flag 
         "pop %%ebx            \n"
         : "=d"(IsMMXSupported)
         :
         : "memory", "cc", "eax", "ecx"
         );
#endif
    if (IsMMXSupported != 0) 
        return true; 
    else 
        return false; 
} 

bool isSSESupported() 
{
    int SSESupport = 0;

    // And finally, check the CPUID for Streaming SIMD Extensions support.
#if defined(__INTEL_COMPILER) && !defined(NO_ASM)
    _asm{
       mov      eax, 1          // Put a "1" in eax to tell CPUID to get the feature bits
         cpuid                  // Perform CPUID (puts processor feature info into EDX)
         and        edx, 02000000h  // Test bit 25, for Streaming SIMD Extensions existence.
         mov        SSESupport, edx // SIMD Extensions).  Set return value to 1 to indicate,
    }
#elif defined(__GNUC__) && defined(__x86_64__) && !defined(NO_ASM)
  return true;
#elif !defined(NO_ASM) // GCC assumed
   asm volatile (
         "push %%ebx                       \n"
         "mov $1, %%eax                    \n"          // Put a "1" in eax to tell CPUID to get the feature bits
         "cpuid                            \n"                  // Perform CPUID (puts processor feature info into EDX)
         "and       $0x02000000, %%edx \n"  // Test bit 25, for Streaming SIMD Extensions existence.
         "pop %%ebx                        \n"
         : "=d"(SSESupport)
         :
         : "memory", "cc", "eax", "ecx"
         );
# endif
    
    if (SSESupport != 0) 
        return true; 
    else 
        return false; 
} 

void ReadConfiguration(void)
{
    options.bEnableHacks = TRUE;
    options.bEnableSSE = TRUE;
    options.bEnableVertexShader = FALSE;

    defaultRomOptions.screenUpdateSetting = SCREEN_UPDATE_AT_VI_CHANGE;
    //defaultRomOptions.screenUpdateSetting = SCREEN_UPDATE_AT_VI_UPDATE_AND_DRAWN;

    status.isMMXSupported = isMMXSupported();
    status.isSSESupported = isSSESupported();
    status.isVertexShaderSupported = false;

    defaultRomOptions.N64FrameBufferEmuType = FRM_BUF_NONE;
    defaultRomOptions.N64FrameBufferWriteBackControl = FRM_BUF_WRITEBACK_NORMAL;
    defaultRomOptions.N64RenderToTextureEmuType = TXT_BUF_NONE;

    if(TestRegistry() == FALSE)
    {
        options.bEnableFog = TRUE;
        options.bWinFrameMode = FALSE;
        options.bFullTMEM = FALSE;
        options.bUseFullTMEM = FALSE;

        options.bForceSoftwareTnL = TRUE;
        options.bForceSoftwareClipper = TRUE;
        options.bEnableSSE = TRUE;

        options.bEnableVertexShader = FALSE;
        options.bOGLVertexClipper = FALSE;
        options.RenderBufferSetting=1;
        options.forceTextureFilter = 0;
        options.textureQuality = TXT_QUALITY_DEFAULT;
        options.bTexRectOnly = FALSE;
        options.bSmallTextureOnly = FALSE;
        options.bLoadHiResTextures = FALSE;
        options.bDumpTexturesToFiles = FALSE;
        options.DirectXDepthBufferSetting = 0;
        options.OpenglDepthBufferSetting = 16;
        options.colorQuality = TEXTURE_FMT_A8R8G8B8;
        options.textureEnhancement = 0;
        options.textureEnhancementControl = 0;
        options.OpenglRenderSetting = OGL_DEVICE;
        options.bSkipFrame = FALSE;
        options.bDisplayTooltip = FALSE;
        options.bHideAdvancedOptions = TRUE;
        options.bDisplayOnscreenFPS = FALSE;
        options.DirectXAntiAliasingValue = 0;
        options.DirectXCombiner = DX_BEST_FIT;
        options.DirectXDevice = 0;  // HAL device
        options.DirectXAnisotropyValue = 0;
        options.DirectXMaxFSAA = 16;
        options.FPSColor = 0xFFFFFFFF;
        options.DirectXMaxAnisotropy = 16;

        defaultRomOptions.N64FrameBufferEmuType = FRM_BUF_NONE;
        defaultRomOptions.N64FrameBufferWriteBackControl = FRM_BUF_WRITEBACK_NORMAL;
        defaultRomOptions.N64RenderToTextureEmuType = TXT_BUF_NONE;

        defaultRomOptions.bNormalBlender = FALSE;
        defaultRomOptions.bFastTexCRC=FALSE;
        defaultRomOptions.bNormalCombiner = FALSE;
        defaultRomOptions.bAccurateTextureMapping = TRUE;
        defaultRomOptions.bInN64Resolution = FALSE;
        defaultRomOptions.bSaveVRAM = FALSE;
        defaultRomOptions.bOverlapAutoWriteBack = FALSE;
        defaultRomOptions.bDoubleSizeForSmallTxtrBuf = FALSE;
        windowSetting.uFullScreenRefreshRate = 0;   // 0 is the default value, means to use Window default frequency
       
        WriteConfiguration();
        return;
    }
    else
    {
        windowSetting.uWindowDisplayWidth = (uint16)ReadRegistryDwordVal("WinModeWidth");
        if( windowSetting.uWindowDisplayWidth == 0 )
        {
            windowSetting.uWindowDisplayWidth = 640;
        }

        windowSetting.uWindowDisplayHeight = (uint16)ReadRegistryDwordVal("WinModeHeight");
        if( windowSetting.uWindowDisplayHeight == 0 )
        {
            windowSetting.uWindowDisplayHeight = 480;
        }
        
        windowSetting.uDisplayWidth = windowSetting.uWindowDisplayWidth;
        windowSetting.uDisplayHeight = windowSetting.uWindowDisplayHeight;


        windowSetting.uFullScreenDisplayWidth = (uint16)ReadRegistryDwordVal("FulScreenWidth");
        if( windowSetting.uFullScreenDisplayWidth == 0 )
        {
            windowSetting.uFullScreenDisplayWidth = 640;
        }
        windowSetting.uFullScreenDisplayHeight = (uint16)ReadRegistryDwordVal("FulScreenHeight");
        if( windowSetting.uFullScreenDisplayHeight == 0 )
        {
            windowSetting.uFullScreenDisplayHeight = 480;
        }
       
            windowSetting.uWindowDisplayWidth = windowSetting.uFullScreenDisplayWidth;
            windowSetting.uWindowDisplayHeight = windowSetting.uFullScreenDisplayHeight;
            windowSetting.uDisplayWidth = windowSetting.uWindowDisplayWidth;
        windowSetting.uDisplayHeight = windowSetting.uWindowDisplayHeight;

        defaultRomOptions.N64FrameBufferEmuType = ReadRegistryDwordVal("FrameBufferSetting");
        defaultRomOptions.N64FrameBufferWriteBackControl = ReadRegistryDwordVal("FrameBufferWriteBackControl");
        defaultRomOptions.N64RenderToTextureEmuType = ReadRegistryDwordVal("RenderToTexture");
        defaultRomOptions.bNormalBlender = ReadRegistryDwordVal("NormalAlphaBlender");

        options.bEnableFog = ReadRegistryDwordVal("EnableFog");
        options.bWinFrameMode = ReadRegistryDwordVal("WinFrameMode");
        options.bFullTMEM = ReadRegistryDwordVal("FullTMEMEmulation");
        options.bForceSoftwareTnL = ReadRegistryDwordVal("ForceSoftwareTnL");
        options.bForceSoftwareClipper = ReadRegistryDwordVal("ForceSoftwareClipper");
        options.bOGLVertexClipper = ReadRegistryDwordVal("OpenGLVertexClipper");
        options.bEnableSSE = ReadRegistryDwordVal("EnableSSE");
        options.bEnableVertexShader = ReadRegistryDwordVal("EnableVertexShader");
        options.bEnableVertexShader = FALSE;
        options.bSkipFrame = ReadRegistryDwordVal("SkipFrame");
        options.bDisplayTooltip = ReadRegistryDwordVal("DisplayTooltip");
        options.bHideAdvancedOptions = ReadRegistryDwordVal("HideAdvancedOptions");
        options.bDisplayOnscreenFPS = ReadRegistryDwordVal("DisplayOnscreenFPS");
        options.RenderBufferSetting = ReadRegistryDwordVal("FrameBufferType");
        options.textureEnhancement = ReadRegistryDwordVal("TextureEnhancement");
        options.textureEnhancementControl = ReadRegistryDwordVal("TextureEnhancementControl");
        options.forceTextureFilter = ReadRegistryDwordVal("ForceTextureFilter");
        options.textureQuality = ReadRegistryDwordVal("TextureQuality");
        options.bTexRectOnly = ReadRegistryDwordVal("TexRectOnly");
        options.bSmallTextureOnly = ReadRegistryDwordVal("SmallTextureOnly");
        options.bLoadHiResTextures = ReadRegistryDwordVal("LoadHiResTextures");
        options.bDumpTexturesToFiles = ReadRegistryDwordVal("DumpTexturesToFiles");
        defaultRomOptions.bFastTexCRC = ReadRegistryDwordVal("FastTextureLoading");
        options.bShowFPS = ReadRegistryDwordVal("ShowFPS");
        options.FPSColor = ReadRegistryDwordVal("FPSColor");;
        options.DirectXMaxAnisotropy = ReadRegistryDwordVal("DirectXMaxAnisotropy");;
        options.OpenglDepthBufferSetting = ReadRegistryDwordVal("OpenGLDepthBufferSetting");
        options.colorQuality = ReadRegistryDwordVal("ColorQuality");
        options.OpenglRenderSetting = ReadRegistryDwordVal("OpenGLRenderSetting");
        defaultRomOptions.bFastTexCRC = ReadRegistryDwordVal("FastTextureLoading");
        defaultRomOptions.bAccurateTextureMapping = ReadRegistryDwordVal("AccurateTextureMapping");
        defaultRomOptions.bInN64Resolution = ReadRegistryDwordVal("InN64Resolution");
        defaultRomOptions.bSaveVRAM = ReadRegistryDwordVal("SaveVRAM");
        defaultRomOptions.bOverlapAutoWriteBack = ReadRegistryDwordVal("OverlapAutoWriteBack");
        defaultRomOptions.bDoubleSizeForSmallTxtrBuf = ReadRegistryDwordVal("DoubleSizeForSmallTxtrBuf");
        windowSetting.uFullScreenRefreshRate = ReadRegistryDwordVal("FullScreenFrequency");

        CDeviceBuilder::SelectDeviceType((SupportedDeviceType)options.OpenglRenderSetting);
    }

    status.isSSEEnabled = status.isSSESupported && options.bEnableSSE;
#if !defined(NO_ASM)
    if( status.isSSEEnabled )
    {
        ProcessVertexData = ProcessVertexDataSSE;
        printf("[RiceVideo] SSE processing enabled.\n");
    }
    else
#endif
    {
        ProcessVertexData = ProcessVertexDataNoSSE;
        printf("[RiceVideo] Disabled SSE processing.\n");
    }

    status.isVertexShaderEnabled = status.isVertexShaderSupported && options.bEnableVertexShader;
    status.bUseHW_T_L = false;
}
    
BOOL InitConfiguration(void)
{
    //Initialize this DLL

    IniSections.clear();
    bIniIsChanged = false;
    strcpy(szIniFileName, INI_FILE);

    if (!ReadIniFile())
        {
        ErrorMsg("Unable to read ini file from disk");
        WriteIniFile();
            return FALSE;
        }
    ReadConfiguration();
    return TRUE;
}

void GenerateCurrentRomOptions()
{
    currentRomOptions.N64FrameBufferEmuType     =g_curRomInfo.dwFrameBufferOption;  
    currentRomOptions.N64FrameBufferWriteBackControl        =defaultRomOptions.N64FrameBufferWriteBackControl;  
    currentRomOptions.N64RenderToTextureEmuType =g_curRomInfo.dwRenderToTextureOption;  
    currentRomOptions.screenUpdateSetting       =g_curRomInfo.dwScreenUpdateSetting;
    currentRomOptions.bNormalCombiner           =g_curRomInfo.dwNormalCombiner;
    currentRomOptions.bNormalBlender            =g_curRomInfo.dwNormalBlender;
    currentRomOptions.bFastTexCRC               =g_curRomInfo.dwFastTextureCRC;
    currentRomOptions.bAccurateTextureMapping   =g_curRomInfo.dwAccurateTextureMapping;

    options.enableHackForGames = NO_HACK_FOR_GAME;
    if ((strncmp((char*)g_curRomInfo.szGameName, "BANJO TOOIE", 11) == 0))
    {
        options.enableHackForGames = HACK_FOR_BANJO_TOOIE;
    }
    else if ((strncmp((char*)g_curRomInfo.szGameName, "DR.MARIO", 8) == 0))
    {
        options.enableHackForGames = HACK_FOR_DR_MARIO;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "Pilot", 5) == 0))
    {
        options.enableHackForGames = HACK_FOR_PILOT_WINGS;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "YOSHI", 5) == 0))
    {
        options.enableHackForGames = HACK_FOR_YOSHI;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "NITRO", 5) == 0))
    {
        options.enableHackForGames = HACK_FOR_NITRO;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "TONY HAWK", 9) == 0))
    {
        options.enableHackForGames = HACK_FOR_TONYHAWK;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "THPS", 4) == 0))
    {
        options.enableHackForGames = HACK_FOR_TONYHAWK;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "SPIDERMAN", 9) == 0))
    {
        options.enableHackForGames = HACK_FOR_TONYHAWK;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "NASCAR", 6) == 0))
    {
        options.enableHackForGames = HACK_FOR_NASCAR;
    }
    else if ((strstr((char*)g_curRomInfo.szGameName, "ZELDA") != 0) && (strstr((char*)g_curRomInfo.szGameName, "MASK") != 0))
    {
        options.enableHackForGames = HACK_FOR_ZELDA_MM;
    }
    else if ((strstr((char*)g_curRomInfo.szGameName, "ZELDA") != 0))
    {
        options.enableHackForGames = HACK_FOR_ZELDA;
    }
    else if ((strstr((char*)g_curRomInfo.szGameName, "Ogre") != 0))
    {
        options.enableHackForGames = HACK_FOR_OGRE_BATTLE;
    }
    else if ((strstr((char*)g_curRomInfo.szGameName, "TWINE") != 0))
    {
        options.enableHackForGames = HACK_FOR_TWINE;
    }
    else if ((strstr((char*)g_curRomInfo.szGameName, "Squadron") != 0))
    {
        options.enableHackForGames = HACK_FOR_ROGUE_SQUADRON;
    }
    else if ((strstr((char*)g_curRomInfo.szGameName, "Baseball") != 0) && (strstr((char*)g_curRomInfo.szGameName, "Star") != 0))
    {
        options.enableHackForGames = HACK_FOR_ALL_STAR_BASEBALL;
    }
    else if ((strstr((char*)g_curRomInfo.szGameName, "Tigger") != 0) && (strstr((char*)g_curRomInfo.szGameName, "Honey") != 0))
    {
        options.enableHackForGames = HACK_FOR_TIGER_HONEY_HUNT;
    }
    else if ((strstr((char*)g_curRomInfo.szGameName, "Bust") != 0) && (strstr((char*)g_curRomInfo.szGameName, "Move") != 0))
    {
        options.enableHackForGames = HACK_FOR_BUST_A_MOVE;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "MarioTennis",11) == 0))
    {
        options.enableHackForGames = HACK_FOR_MARIO_TENNIS;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "SUPER BOWLING",13) == 0))
    {
        options.enableHackForGames = HACK_FOR_SUPER_BOWLING;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "CONKER",6) == 0))
    {
        options.enableHackForGames = HACK_FOR_CONKER;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "MK_MYTHOLOGIES",14) == 0))
    {
        options.enableHackForGames = HACK_REVERSE_Y_COOR;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "Fighting Force",14) == 0))
    {
        options.enableHackForGames = HACK_REVERSE_XY_COOR;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "GOLDENEYE",9) == 0))
    {
        options.enableHackForGames = HACK_FOR_GOLDEN_EYE;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "F-ZERO",6) == 0))
    {
        options.enableHackForGames = HACK_FOR_FZERO;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "Command&Conquer",15) == 0))
    {
        options.enableHackForGames = HACK_FOR_COMMANDCONQUER;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "READY 2 RUMBLE",14) == 0))
    {
        options.enableHackForGames = HACK_FOR_RUMBLE;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "READY to RUMBLE",15) == 0))
    {
        options.enableHackForGames = HACK_FOR_RUMBLE;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "South Park Rally",16) == 0))
    {
        options.enableHackForGames = HACK_FOR_SOUTH_PARK_RALLY;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "Extreme G 2",11) == 0))
    {
        options.enableHackForGames = HACK_FOR_EXTREME_G2;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "MarioGolf64",11) == 0))
    {
        options.enableHackForGames = HACK_FOR_MARIO_GOLF;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "MLB FEATURING",13) == 0))
    {
        options.enableHackForGames = HACK_FOR_MLB;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "POLARISSNOCROSS",15) == 0))
    {
        options.enableHackForGames = HACK_FOR_POLARISSNOCROSS;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "TOP GEAR RALLY",14) == 0))
    {
        options.enableHackForGames = HACK_FOR_TOPGEARRALLY;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "DUKE NUKEM",10) == 0))
    {
        options.enableHackForGames = HACK_FOR_DUKE_NUKEM;
    }
    else if ((strncasecmp((char*)g_curRomInfo.szGameName, "MARIOKART64",11) == 0))
    {
        options.enableHackForGames = HACK_FOR_MARIO_KART;
    }

    if (options.enableHackForGames != NO_HACK_FOR_GAME)
        printf("[RiceVideo] Enabled hacks for game: '%s'\n", g_curRomInfo.szGameName);

    if( currentRomOptions.N64FrameBufferEmuType == 0 )      currentRomOptions.N64FrameBufferEmuType = defaultRomOptions.N64FrameBufferEmuType;
    else currentRomOptions.N64FrameBufferEmuType--;
    if( currentRomOptions.N64RenderToTextureEmuType == 0 )      currentRomOptions.N64RenderToTextureEmuType = defaultRomOptions.N64RenderToTextureEmuType;
    else currentRomOptions.N64RenderToTextureEmuType--;
    if( currentRomOptions.screenUpdateSetting == 0 )        currentRomOptions.screenUpdateSetting = defaultRomOptions.screenUpdateSetting;
    if( currentRomOptions.bNormalCombiner == 0 )            currentRomOptions.bNormalCombiner = defaultRomOptions.bNormalCombiner;
    else currentRomOptions.bNormalCombiner--;
    if( currentRomOptions.bNormalBlender == 0 )         currentRomOptions.bNormalBlender = defaultRomOptions.bNormalBlender;
    else currentRomOptions.bNormalBlender--;
    if( currentRomOptions.bFastTexCRC == 0 )                currentRomOptions.bFastTexCRC = defaultRomOptions.bFastTexCRC;
    else currentRomOptions.bFastTexCRC--;
    if( currentRomOptions.bAccurateTextureMapping == 0 )        currentRomOptions.bAccurateTextureMapping = defaultRomOptions.bAccurateTextureMapping;
    else currentRomOptions.bAccurateTextureMapping--;

    options.bUseFullTMEM = ((options.bFullTMEM && (g_curRomInfo.dwFullTMEM == 0)) || g_curRomInfo.dwFullTMEM == 2);

    GenerateFrameBufferOptions();

    if( options.enableHackForGames == HACK_FOR_MARIO_GOLF || options.enableHackForGames == HACK_FOR_MARIO_TENNIS )
    {
        frameBufferOptions.bIgnoreRenderTextureIfHeightUnknown = true;
    }
}

void Ini_GetRomOptions(LPGAMESETTING pGameSetting)
{
    LONG i;

    i = FindIniEntry(pGameSetting->romheader.dwCRC1,
                              pGameSetting->romheader.dwCRC2,
                              pGameSetting->romheader.nCountryID,
                              (char*)pGameSetting->szGameName);

    pGameSetting->bDisableTextureCRC    = IniSections[i].bDisableTextureCRC;
    pGameSetting->bDisableCulling       = IniSections[i].bDisableCulling;
    pGameSetting->bIncTexRectEdge       = IniSections[i].bIncTexRectEdge;
    pGameSetting->bZHack                = IniSections[i].bZHack;
    pGameSetting->bTextureScaleHack     = IniSections[i].bTextureScaleHack;
    pGameSetting->bPrimaryDepthHack     = IniSections[i].bPrimaryDepthHack;
    pGameSetting->bTexture1Hack         = IniSections[i].bTexture1Hack;
    pGameSetting->bFastLoadTile         = IniSections[i].bFastLoadTile;
    pGameSetting->bUseSmallerTexture    = IniSections[i].bUseSmallerTexture;

    pGameSetting->VIWidth               = IniSections[i].VIWidth;
    pGameSetting->VIHeight              = IniSections[i].VIHeight;
    pGameSetting->UseCIWidthAndRatio    = IniSections[i].UseCIWidthAndRatio;
    pGameSetting->dwFullTMEM            = IniSections[i].dwFullTMEM;
    pGameSetting->bTxtSizeMethod2       = IniSections[i].bTxtSizeMethod2;
    pGameSetting->bEnableTxtLOD         = IniSections[i].bEnableTxtLOD;

    pGameSetting->dwFastTextureCRC      = IniSections[i].dwFastTextureCRC;
    pGameSetting->bEmulateClear         = IniSections[i].bEmulateClear;
    pGameSetting->bForceScreenClear     = IniSections[i].bForceScreenClear;
    pGameSetting->dwAccurateTextureMapping  = IniSections[i].dwAccurateTextureMapping;
    pGameSetting->dwNormalBlender       = IniSections[i].dwNormalBlender;
    pGameSetting->bDisableBlender       = IniSections[i].bDisableBlender;
    pGameSetting->dwNormalCombiner      = IniSections[i].dwNormalCombiner;
    pGameSetting->bForceDepthBuffer     = IniSections[i].bForceDepthBuffer;
    pGameSetting->bDisableObjBG         = IniSections[i].bDisableObjBG;
    pGameSetting->dwFrameBufferOption   = IniSections[i].dwFrameBufferOption;
    pGameSetting->dwRenderToTextureOption   = IniSections[i].dwRenderToTextureOption;
    pGameSetting->dwScreenUpdateSetting = IniSections[i].dwScreenUpdateSetting;
}

void Ini_StoreRomOptions(LPGAMESETTING pGameSetting)
{
    LONG i;

    i = FindIniEntry(pGameSetting->romheader.dwCRC1,
        pGameSetting->romheader.dwCRC2,
        pGameSetting->romheader.nCountryID,
        (char*)pGameSetting->szGameName);

    if( IniSections[i].bDisableTextureCRC   !=pGameSetting->bDisableTextureCRC )
    {
        IniSections[i].bDisableTextureCRC   =pGameSetting->bDisableTextureCRC    ;
        bIniIsChanged=true;
    }

    if( IniSections[i].bDisableCulling  !=pGameSetting->bDisableCulling )
    {
        IniSections[i].bDisableCulling  =pGameSetting->bDisableCulling   ;
        bIniIsChanged=true;
    }

    if( IniSections[i].dwFastTextureCRC !=pGameSetting->dwFastTextureCRC )
    {
        IniSections[i].dwFastTextureCRC =pGameSetting->dwFastTextureCRC      ;
        bIniIsChanged=true;
    }

    if( IniSections[i].bEmulateClear !=pGameSetting->bEmulateClear )
    {
        IniSections[i].bEmulateClear    =pGameSetting->bEmulateClear         ;
        bIniIsChanged=true;
    }

    if( IniSections[i].dwNormalBlender      !=pGameSetting->dwNormalBlender )
    {
        IniSections[i].dwNormalBlender      =pGameSetting->dwNormalBlender       ;
        bIniIsChanged=true;
    }

    if( IniSections[i].bDisableBlender  !=pGameSetting->bDisableBlender )
    {
        IniSections[i].bDisableBlender  =pGameSetting->bDisableBlender       ;
        bIniIsChanged=true;
    }

    if( IniSections[i].bForceScreenClear    !=pGameSetting->bForceScreenClear )
    {
        IniSections[i].bForceScreenClear    =pGameSetting->bForceScreenClear         ;
        bIniIsChanged=true;
    }
    if( IniSections[i].dwAccurateTextureMapping !=pGameSetting->dwAccurateTextureMapping )
    {
        IniSections[i].dwAccurateTextureMapping =pGameSetting->dwAccurateTextureMapping      ;
        bIniIsChanged=true;
    }
    if( IniSections[i].dwNormalCombiner !=pGameSetting->dwNormalCombiner )
    {
        IniSections[i].dwNormalCombiner =pGameSetting->dwNormalCombiner      ;
        bIniIsChanged=true;
    }
    if( IniSections[i].bForceDepthBuffer    !=pGameSetting->bForceDepthBuffer )
    {
        IniSections[i].bForceDepthBuffer    =pGameSetting->bForceDepthBuffer         ;
        bIniIsChanged=true;
    }
    if( IniSections[i].bDisableObjBG    !=pGameSetting->bDisableObjBG )
    {
        IniSections[i].bDisableObjBG    =pGameSetting->bDisableObjBG         ;
        bIniIsChanged=true;
    }
    if( IniSections[i].dwFrameBufferOption  !=pGameSetting->dwFrameBufferOption )
    {
        IniSections[i].dwFrameBufferOption  =pGameSetting->dwFrameBufferOption       ;
        bIniIsChanged=true;
    }
    if( IniSections[i].dwRenderToTextureOption  !=pGameSetting->dwRenderToTextureOption )
    {
        IniSections[i].dwRenderToTextureOption  =pGameSetting->dwRenderToTextureOption       ;
        bIniIsChanged=true;
    }
    if( IniSections[i].dwScreenUpdateSetting    !=pGameSetting->dwScreenUpdateSetting )
    {
        IniSections[i].dwScreenUpdateSetting    =pGameSetting->dwScreenUpdateSetting         ;
        bIniIsChanged=true;
    }
    if( IniSections[i].bIncTexRectEdge  != pGameSetting->bIncTexRectEdge )
    {
        IniSections[i].bIncTexRectEdge      =pGameSetting->bIncTexRectEdge;
        bIniIsChanged=true;
    }
    if( IniSections[i].bZHack   != pGameSetting->bZHack )
    {
        IniSections[i].bZHack       =pGameSetting->bZHack;
        bIniIsChanged=true;
    }
    if( IniSections[i].bTextureScaleHack    != pGameSetting->bTextureScaleHack )
    {
        IniSections[i].bTextureScaleHack        =pGameSetting->bTextureScaleHack;
        bIniIsChanged=true;
    }
    if( IniSections[i].bPrimaryDepthHack    != pGameSetting->bPrimaryDepthHack )
    {
        IniSections[i].bPrimaryDepthHack        =pGameSetting->bPrimaryDepthHack;
        bIniIsChanged=true;
    }
    if( IniSections[i].bTexture1Hack    != pGameSetting->bTexture1Hack )
    {
        IniSections[i].bTexture1Hack        =pGameSetting->bTexture1Hack;
        bIniIsChanged=true;
    }
    if( IniSections[i].bFastLoadTile    != pGameSetting->bFastLoadTile )
    {
        IniSections[i].bFastLoadTile    =pGameSetting->bFastLoadTile;
        bIniIsChanged=true;
    }
    if( IniSections[i].bUseSmallerTexture   != pGameSetting->bUseSmallerTexture )
    {
        IniSections[i].bUseSmallerTexture   =pGameSetting->bUseSmallerTexture;
        bIniIsChanged=true;
    }
    if( IniSections[i].VIWidth  != pGameSetting->VIWidth )
    {
        IniSections[i].VIWidth  =pGameSetting->VIWidth;
        bIniIsChanged=true;
    }
    if( IniSections[i].VIHeight != pGameSetting->VIHeight )
    {
        IniSections[i].VIHeight =pGameSetting->VIHeight;
        bIniIsChanged=true;
    }
    if( IniSections[i].UseCIWidthAndRatio   != pGameSetting->UseCIWidthAndRatio )
    {
        IniSections[i].UseCIWidthAndRatio   =pGameSetting->UseCIWidthAndRatio;
        bIniIsChanged=true;
    }
    if( IniSections[i].dwFullTMEM   != pGameSetting->dwFullTMEM )
    {
        IniSections[i].dwFullTMEM   =pGameSetting->dwFullTMEM;
        bIniIsChanged=true;
    }
    if( IniSections[i].bTxtSizeMethod2  != pGameSetting->bTxtSizeMethod2 )
    {
        IniSections[i].bTxtSizeMethod2  =pGameSetting->bTxtSizeMethod2;
        bIniIsChanged=true;
    }
    if( IniSections[i].bEnableTxtLOD    != pGameSetting->bEnableTxtLOD )
    {
        IniSections[i].bEnableTxtLOD    =pGameSetting->bEnableTxtLOD;
        bIniIsChanged=true;
    }

    if( bIniIsChanged )
    {
        WriteIniFile();
        TRACE0("Rom option is changed and saved");
    }
}

typedef struct {
    const char *title;
    const char *text;
} ToolTipMsg;

ToolTipMsg ttmsg[] = {
    { 
            "Render Engine",
            "Select which render engine to use, DirectX or OpenGL.\n"
    },
    { 
            "Choose a color combiner to use with the render engine.\n",
            "The default [To Fit Your Video Card] should work just fine for you, or you can change:\n\n"
            "For DirectX, you can use low end, mid end, high end or Nvidia TNT combiner.\n"
            "- Low-end combiner is for video cards which can only do 1 combiner cycle or has only 1 texture unit."
            " It is for old or low-end video cards, and most onboard ones\n"
            "- Mid-end combiner is for video cards which can do more than 1 combiner cycles and/or has more than"
            "1 texture units, but with limited combiner modes (only supporting LERP, MULTIPLYADD). For video"
            " cards such as Rage 128, Voodoos, etc\n"
            "- High-end combiner is for video cards over mid-end ones, which can do LERP, MULTIPLYADD etc. "
            "It is for Radeon, Geforce 2/3/4 ti (not GF2 MX, or GF4 MX)\n"
            "- Nvidia TNT combiner is for TNT, TNT2, Geforce2 MX (not TI), Geforce 4 MX (not ti)\n"
            "- Limited stage combiners: can be used in case that the maximum combiner stage number reported by the video card driver is wrong (from Nvidia drivers)\n"
            "- Pixel shader: this is the best combiner if your video card supports it. In order to use it, your video card have "
            "to support DirectX version 8.1 or up features."
            "- Semi-pixel shader: this combiner only uses pixel shader if needed, and uses regular DirectX combiner "
            "settings for simpler N64 combiner modes. For video cards with slower pixel shader implementation, this combiner "
            "will be faster than the pure pixel shader combiner."

    },
    { 
            "Choose a color combiner to use with the render engine.\n",
            "The default [To Fit Your Video Card] should work just fine for you, or you can change:\n\n"
            "For OpenGL, you can use Ogl 1.1, Ogl 1.2/1.3/1.4, Nvidia TNT, Nvidia Geforce Register combiner\n"
            "- Ogl 1.1, most video cards support this\n"
            "- Ogl 1.2/1.3, for OGL version without Texture Crossbar support\n"
            "- Ogl 1.4, for OGL version with Texture Crossbar support\n"
            "- Nvidia TNT, is good for all Nvidia video cards from TNT\n"
            "- Nvidia Register Combiner, is for all Nvidia video cards from Geforce 256. This combiner is "
            "better than the Nvidia TNT one\n"
    },
    { 
            "DirectX Frame Buffer Swap Effect",
            "Double buffer flip is faster for full screen\n\n"
    },
    { 
            "DirectX Full Screen Mode Anti-Aliasing Setting",
            "Please refer to your video card driver setting to determine the maximum supported FSAA value. The plugin will try to determine "
            "the highest supported FSAA mode, but it may not work well enough since highest FSAA setting is also dependent on the full scrren "
            "resolution. Using incorrect FSAA value will cause DirectX fail to initialize.\n\n"
            "FSAA usage is not compatible with frame buffer effects. Frame buffer may fail to work if FSAA is used."
    },
    { 
            "DirectX Anisotropy Filtering Setting",
            "DirectX Anisotropy Filtering Setting"
    },
    { 
            "Full Screen Mode Color Quality",
            "16 bits:  should be faster.\n"
            "32 bits:  gives better color qualify.\n"
    },
    { 
            "Depth buffer setting",
            "You don't need to modify this setting.\n"
    },
    { 
            "Window mode display resolution",
            "Window mode display resolution"
    },
    { 
            "Full screen mode display resolution",
            "Full screen mode display resolution"
    },
    { 
            "Texture enhancement",
            "Enhance texture when loading the texture.\n\n"
            "- 2x        double the texture size\n"
            "- 2x texture rectangle only,    double the texture size, only for textRect, not for triangle primitives\n"
            "- 2xSai,    double the texture size and apply 2xSai algorithm\n"
            "- 2xSai for texture rectangle only\n"
            "- Sharpen,      apply sharpen filter (cool effects)\n"
            "- Sharpen more, do more sharpening"
    },
    { 
            "Teture enhancement control",
            "Control the texture enhancement filters.\n\n"
            "- Normal                without control\n"
            "- small texture only,   to enhance the texture for small textures only\n"
            "- Smooth                to apply a smooth filter after enhancement\n"
            "- Less smooth           to apply a (less) smooth filter\n"
            "- 2xSai smooth          to apply smooth filter for 2xSai artifacts effects\n"
            "- sxSai less smooth     again, this is for 2xSai, but with less smooth"
    },
    { 
            "Force texture filter",
            "Force Nearest filter and force bilinear filter\n"
    },
    { 
            "For small textures only",
            "If enabled, texture enhancement will be done only for textures width+height<=128\n"
    },
    { 
            "For TxtRect ucode only",
            "If enabled, texture enhancement will be done only for TxtRect ucode\n"
    },
    { 
            "Enable/Disable Fog",
            "Enable or disable fog by this option\n"
    },
    { 
            "Enable/Disable SSE for Intel P3/P4 CPU",
            "SSE (Intel Streaming SMID Extension) can speed up 3D transformation, vertex and matrix processing. "
            "It is only available with Intel P3 and P4 CPU, not with AMD CPUs. P3 is actually much faster than P4 "
            "with SSE instructions\n"
    },
    { 
            "Frame skipping",
            "If this option is on, the plugin will skip every other frames. This could help to improve "
            "speed for some games, and could cause flickering for other games.\n"
    },
    { 
            "Vertex Shader",
            "If this option is on, the plugin will try to use vertex shader if supported by GPU. Using "
            "a vertex shader will transfer most CPU duty on vertex transforming and lighting to GPU, "
            "will great decrease the CPU duty and increase the game speed.\n"
            "The plugin uses vertex shader 1.0 which is defined by DirectX 8.0. The plugin supports vertex "
            "in DirectX mode only at this moment."
    },
    { 
            "Force to use normal alpha blender",
            "Use this option if you have opaque/transparency problems with certain games.\n"
            "\nWhen a game is not running, it is the default value (for all games), available values are on/off.\n"
            "When a game is running, it is the game setting. Three available setting are on/off/as_default."
    },
    { 
            "Normal color combiner",
            "Force to use normal color combiner\n"
            "Normal color combiner is:\n"
            "- Texture * Shade,  if both texture and shade are used\n"
            "- Texture only,     if texture is used and shade is not used\n"
            "- shade only,       if texture is not used\n\n"
            "Try to use this option if you have ingame texture color problems, transparency problems, "
            "or black/white texture problems\n"
            "\nWhen a game is not running, it is the default value (for all games), available values are on/off.\n"
            "When a game is running, it is the game setting. Three available setting are on/off/as_default."
    },
    { 
            "Fast texture loading",
            "Using a faster algorithm to speed up texture loading and CRC computation.\n"
            "\nWhen a game is not running, it is the default value (for all games), available values are on/off.\n"
            "When a game is running, it is the game setting. Three available setting are on/off/as_default."
    },
    { 
            "Force Buffer Clear",
            "This option helps to reduce thin black lines in some games\n"
            "\nWhen a game is not running, it is the default value (for all games), available values are on/off.\n"
            "When a game is running, it is the game setting. Three available setting are on/off/as_default."
    },
    { 
            "Force Buffer Clear",
            "Force to clear screen before drawing any primitives.\n"
            "This is in fact a hack, only for a few games, including KI Gold\n"
            "\nWhen a game is not running, it is the default value (for all games), available values are on/off.\n"
            "When a game is running, it is the game setting. Three available setting are on/off/as_default."
    },
    { 
            "Enable BG primitive",
            "Disable this option for Zelda MM, otherwise its intro will be covered by a black layer (by drawing of a black BG texture).\n"
            "\nWhen a game is not running, it is the default value (for all games), available values are on/off.\n"
            "When a game is running, it is the game setting. Three available setting are on/off/as_default."
    },
    { 
            "Control when the screen will be updated",
            "\n"
            "At VI origin update (default)\n"
            "At VI origin change\n"
            "At CI change\n"
            "At the 1st CI change\n"
            "At the 1st drawing\n"
            "\nWhen a game is not running, it is the default value (for all games).\n"
            "When a game is running, it is the game setting."
    },
    { 
            "Control when the screen will be updated",
            "This option is to prevent or reduce flicking in certain games by controlling when the screen will be updated\n\n"
            "At VI origin update (default)\n"
            "At VI origin change\n"
            "At CI change\n"
            "At the 1st CI change\n"
            "At the 1st drawing\n"
            "\nWhen a game is not running, it is the default value (for all games).\n"
            "When a game is running, it is the game setting."
    },
    { 
            "N64 CPU frame buffer emulation",
            "CPU frame buffer is referred to N64 drawing buffers in RDRAM."
            "Games could draw into a frame buffer other than a displayed render buffer and use the result as textures for further drawing into rendering buffer. "
            "It is very difficult to emulate N64 frame buffer through either DirectX or OpenGL\n\n"
            "- None (default), don't do any frame buffer emulating\n"
            "- Hide framebuffer effects,  ignore frame buffer drawing, at least such drawing won't draw to the current rendering buffer\n"
            "- Basic framebuffer, will check texture loading address to see if the address is within the frame buffer\n"
            "- Basic & Write back, will write the frame buffer back to RDRAM if a texture is loaded from it\n"
            "- Write back & Reload, will load frame buffer from RDRAM at each frame\n"
            "- Write Back Every Frame,       a complete emulation, very slow\n"
            "- With Emulator,  new 1964 will inform the plugin about CPU frame buffer memory direct read/write, for Dr. Mario\n"
    },
    { 
            "Render-to-texture emulation",
            "- None (default), don't do any Render-to-texture emulation\n"
            "- Hide Render-to-texture effects,  ignore Render-to-texture drawing, at least such drawing won't draw to the current rendering buffer\n"
            "- Render-to-texture,    support self-render-texture\n"
            "- Basic Render-to-texture, will check texture loading address to see if the address is within the frame buffer\n"
            "- Basic & Write back, will write the Render-to-render_texture back when rendering is finished\n"
            "- Write back & Reload, will load Render-to-render_texture from RDRAM before the buffer is rendered.\n"
    },
    { 
            "Frame Buffer Write Back Control",
            "Control the frequence of frame buffer writing back to RDRAM\n"
    },
    { 
            "Default options or Rom specific settings",
            "\nWhen a game is not running, it is the default value (for all games).\n"
            "When a game is running, it is the game setting."
    },
    { 
            "Emulate Memory Clear",
            "\nA few games need this option to work better, including DK64."
    },
    { 
            "Force Software Tranlation & Lighting",
            "\nThis option will force to use software T&L instead of available hardware T&L."
            "It is needed for most newer ATI Radeons."
            "\n\nThe plugin will run slower with this option on. If you don't need it, don't leave it on."
    },
    { 
            "Monitor Refresh Frequency in FullScreen Mode",
            "Select the frequency for your full screen mode.\n\n"
            "You should know what's the highest frequency your monitor can display for each screen resolution. If you select a higher frequency "
            "then your monitor can display, you will get black screen or full screen just does not work. At the time, you can press [ALT-Enter] key again to go back to windowed mode."
    },
    { 
            "Display tooltips",
            "Enable/Disable tooltip display in the configuration dialog box\n\n"
    },
    { 
            "Software Vertex Clipper",
            "Enable/Disable Software Vertex Clipper.\n\n"
            "Games graphics are rendered as triangles. Each triangle is determined by 3 vertexes. A triangle could "
            "be completely or partially out of screen as its vertexes go out of screen in X, Y and Z direction. The "
            "process of clipping is to chop the triangle along the boundary by converting the triangle to 1 or "
            "more in-bound triangles.\n\n"
            "The software clipper is needed for most new video cards, but not for most older video cards\n"
            "If your video card works without it, then don't turn it on since it is CPU intensive, games"
            " will become slower if you have slower CPU.\n"
    },
    { 
            "Software Vertex Clipper",
            "Enable/Disable Software Vertex Clipper.\n\n"
            "Games graphics are rendered as triangles. Each triangle is determined by 3 vertexes. A triangle could "
            "be completely or partially out of screen as its vertexes go out of screen in X, Y and Z direction. The "
            "process of clipping is to chop the triangle along the boundary by converting the triangle to 1 or "
            "more in-bound triangles.\n\n"
            "The software clipper for OpenGL helps to resolve near plane clipping problem. For most games, you don't "
            "have to use it since OpenGL has its own vertex clipper.\n"
    },
    { 
            "Force Using Depth Bufer",
            "Force to enable depth buffer compare and update.\n\n"
    },
    { 
            "Disable Alpha Blender",
            "Enable / Disable Alpha Blender\n\n"
            "This option is different from the Normal Blender option. If this option is on, alpha blender "
            "will be disabled completely. All transparency effects are disabled. "
    },
    { 
            "Manually Set the N64 Screen Resolution",
            "Manually set the N64 screen width, the value will overwrite the screen resolution auto detection"
    },
    { 
            "Manually Set the N64 Screen Resolution",
            "Manually set the N64 screen height, the value will overwrite the screen resolution auto detection"
    },
    { 
            "Increase TextRect Edge by 1",
            "This is an advanced option. Try it if you see there are horizonal or vertical thin "
            "lines accross big texture blocks in menu or logo."
    },
    { 
            "Hack the z value",
            "This is an advanced option. If enabled, range of vertex Z values will be adjust "
            " so that vertex before the near plane can be rendered without clipped."
    },
    { 
            "Hack Texture Scale",
            "This is an advanced option. Don't bother if you have no idea what it is. It is only "
            "a hack for a few games."
    },
    { 
            "Faster Texture Tile Loading Algorithm",
            "This is an advanced option. It may increase texture loading if textures are loaded "
            "by LoadTile ucodes."
    },
    { 
            "Primary Depth Hack",
            "This is an advanced option. This is a hack for a few games, don't bother with it."
    },
    { 
            "Texture 1 Hack",
            "This is an advanced option. This is a hack for a few games, don't bother with it."
    },
    { 
            "Disable DL Culling",
            "This is an advanced option. If enabled, it will disable the CullDL ucode."
    },
    { 
            "Disable Texture Caching",
            "This is an advanced option. If enabled, it will disable texture caching. Textures "
            "will be always reloaded, game will be running slower."
    },
    { 
            "Display OnScreen FPS",
            "If enabled, current FPS (frame per second) will be displayed at the right-bottom corner of the screen "
            "in selected color"
    },
    { 
            "Onscreen FPS Display Text Color",
            "Color must be in 32bit HEX format, as AARRGGBB, AA=alpha, RR=red, GG=green, BB=Blue\n"
            "Data must be entered exactly in 8 hex numbers, or the entered value won't be accepted."
    },
    { 
            "TMEM (N64 Texture Memory) Full Emulation",
            "If this option is on, texture data will be loaded into the 4KB TMEM, textures are then created from data in the TMEM.\n"
            "If this option is off, textures are then loaded directly from N64 RDRAM.\n\n"
            "This feature is required by certain games. If it is on, Faster_Loading_Tile option will not work, and sprite ucodes may give errors.\n\n"
            "Sorry for non-perfect implementation."
    },
    { 
            "Frame buffer emulation in N64 native resolution",
            "Back buffer resolution on PC is usually much higher than the N64 native resolution. Back buffer texture "
            "can be saved and used in PC resolution to give the best speed and quality, but this needs large amount "
            "of video card memory. \n\n"
            "If your video card has 32MB or less memory, you'd better to enable this option."
    },
    { 
            "Try to save video RAM for lower end video cards",
            "If enabled, will automatically check if render-to-texture or saved back buffer texture has "
            "been overwritten by CPU thread. If yes, will delete the render_texture to save VRAM.\n"
            "It may be slower because extra checking need to be done at each frame."
    },
    { 
            "Automatically write overlapped texture back to RDRAM",
            "If enabled, such render-to-textures or saved back buffer textures will be written back "
            "to RDRAM if they are to be covered partially by new textures.\n"
    },
    { 
            "Texture Quality",
            "Default - Use the same quality as color buffer quality\n"
            "32-bit Texture - Always use 32 bit textures\n"
            "16-bit Texture - Always use 16 bit textures\n"
    },
    { 
            "Double Texture Buffer Size for Small Render-to-Textures",
            "Enable this option to have better render-to-texture quality, of course this requires "
            "more video RAM."
    },
    { 
            "Hide Advanced Options",
            "If enabled, all advanced options will be hidden. Per game settings, default games settings "
            "and texture filter settings will be all hidden."
    },
    { 
            "WinFrame Mode",
            "If enabled, graphics will be drawn in WinFrame mode instead of solid and texture mode."
    },
};

int numOfTTMsgs = sizeof(ttmsg)/sizeof(ToolTipMsg);

std::ifstream& getline( std::ifstream &is, char *str );

char * left(char * src, int nchars)
{
    static char dst[300];           // BUGFIX (STRMNNRM)
    strncpy(dst,src,nchars);
    dst[nchars]=0;
    return dst;
}

char * right(char *src, int nchars)
{
    static char dst[300];           // BUGFIX (STRMNNRM)
    strncpy(dst, src + strlen(src) - nchars, nchars);
    dst[nchars]=0;
    return dst;
}

char * tidy(char * s)
{
    char * p = s + strlen(s);

    p--;
    while (p >= s && (*p == ' ' || *p == 0xa || *p == '\n') )
    {
        *p = 0;
        p--;
    }
    return s;

}

extern void GetPluginDir( char * Directory );

BOOL ReadIniFile()
{
    std::ifstream inifile;
    char readinfo[100];

    char filename[PATH_MAX];
    GetPluginDir(filename);
    strcat(filename,szIniFileName);
    inifile.open(filename);

    if (inifile.fail())
    {
        return FALSE;
    }

    while (getline(inifile,readinfo)/*&&sectionno<999*/)
    {
        tidy(readinfo);

        if (readinfo[0] == '/')
            continue;

        if (!strcasecmp(readinfo,"")==0)
        {
            if (readinfo[0] == '{') //if a section heading
            {
                section newsection;

                readinfo[strlen(readinfo)-1]='\0';
                strcpy(newsection.crccheck, readinfo+1);

                newsection.bDisableTextureCRC = FALSE;
                newsection.bDisableCulling = FALSE;
                newsection.bIncTexRectEdge = FALSE;
                newsection.bZHack = FALSE;
                newsection.bTextureScaleHack = FALSE;
                newsection.bFastLoadTile = FALSE;
                newsection.bUseSmallerTexture = FALSE;
                newsection.bPrimaryDepthHack = FALSE;
                newsection.bTexture1Hack = FALSE;
                newsection.bDisableObjBG = FALSE;
                newsection.VIWidth = -1;
                newsection.VIHeight = -1;
                newsection.UseCIWidthAndRatio = NOT_USE_CI_WIDTH_AND_RATIO;
                newsection.dwFullTMEM = 0;
                newsection.bTxtSizeMethod2 = FALSE;
                newsection.bEnableTxtLOD = FALSE;

                newsection.bEmulateClear = FALSE;
                newsection.bForceScreenClear = FALSE;
                newsection.bDisableBlender = FALSE;
                newsection.bForceDepthBuffer = FALSE;
                newsection.dwFastTextureCRC = 0;
                newsection.dwAccurateTextureMapping = 0;
                newsection.dwNormalBlender = 0;
                newsection.dwNormalCombiner = 0;
                newsection.dwFrameBufferOption = 0;
                newsection.dwRenderToTextureOption = 0;
                newsection.dwScreenUpdateSetting = 0;

                IniSections.push_back(newsection);

            }
            else
            {       
                int sectionno = IniSections.size() - 1;

                if (strcasecmp(left(readinfo,4), "Name")==0)
                    strcpy(IniSections[sectionno].name,right(readinfo,strlen(readinfo)-5));

                if (strcasecmp(left(readinfo,17), "DisableTextureCRC")==0)
                    IniSections[sectionno].bDisableTextureCRC=true;

                if (strcasecmp(left(readinfo,14), "DisableCulling")==0)
                    IniSections[sectionno].bDisableCulling=true;

                if (strcasecmp(left(readinfo,16), "PrimaryDepthHack")==0)
                    IniSections[sectionno].bPrimaryDepthHack=true;

                if (strcasecmp(left(readinfo,12), "Texture1Hack")==0)
                    IniSections[sectionno].bTexture1Hack=true;

                if (strcasecmp(left(readinfo,12), "FastLoadTile")==0)
                    IniSections[sectionno].bFastLoadTile=true;

                if (strcasecmp(left(readinfo,17), "UseSmallerTexture")==0)
                    IniSections[sectionno].bUseSmallerTexture=true;

                if (strcasecmp(left(readinfo,14), "IncTexRectEdge")==0)
                    IniSections[sectionno].bIncTexRectEdge=true;

                if (strcasecmp(left(readinfo,5), "ZHack")==0)
                    IniSections[sectionno].bZHack=true;

                if (strcasecmp(left(readinfo,16), "TexRectScaleHack")==0)
                    IniSections[sectionno].bTextureScaleHack=true;

                if (strcasecmp(left(readinfo,7), "VIWidth")==0)
                    IniSections[sectionno].VIWidth = strtol(right(readinfo,3),NULL,10);

                if (strcasecmp(left(readinfo,8), "VIHeight")==0)
                    IniSections[sectionno].VIHeight = strtol(right(readinfo,3),NULL,10);

                if (strcasecmp(left(readinfo,18), "UseCIWidthAndRatio")==0)
                    IniSections[sectionno].UseCIWidthAndRatio = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,8), "FullTMEM")==0)
                    IniSections[sectionno].dwFullTMEM = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,24), "AlternativeTxtSizeMethod")==0)
                    IniSections[sectionno].bTxtSizeMethod2 = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,12), "EnableTxtLOD")==0)
                    IniSections[sectionno].bEnableTxtLOD = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,12), "DisableObjBG")==0)
                    IniSections[sectionno].bDisableObjBG = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,16), "ForceScreenClear")==0)
                    IniSections[sectionno].bForceScreenClear = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,22), "AccurateTextureMapping")==0)
                    IniSections[sectionno].dwAccurateTextureMapping = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,14), "FastTextureCRC")==0)
                    IniSections[sectionno].dwFastTextureCRC = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,12), "EmulateClear")==0)
                    IniSections[sectionno].bEmulateClear = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,18), "NormalAlphaBlender")==0)
                    IniSections[sectionno].dwNormalBlender = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,19), "DisableAlphaBlender")==0)
                    IniSections[sectionno].bDisableBlender = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,19), "NormalColorCombiner")==0)
                    IniSections[sectionno].dwNormalCombiner = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,16), "ForceDepthBuffer")==0)
                    IniSections[sectionno].bForceDepthBuffer = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,20), "FrameBufferEmulation")==0)
                    IniSections[sectionno].dwFrameBufferOption = strtol(readinfo+21,NULL,10);

                if (strcasecmp(left(readinfo,15), "RenderToTexture")==0)
                    IniSections[sectionno].dwRenderToTextureOption = strtol(right(readinfo,1),NULL,10);

                if (strcasecmp(left(readinfo,19), "ScreenUpdateSetting")==0)
                    IniSections[sectionno].dwScreenUpdateSetting = strtol(right(readinfo,1),NULL,10);
            }
        }
    }
    inifile.close();

    return TRUE;
}

//read a line from the ini file
std::ifstream & getline(std::ifstream & is, char *str)
{
    char buf[100];

    is.getline(buf,100);
    strcpy( str,buf);
    return is;
}

void WriteIniFile()
{
    TCHAR szFileNameOut[PATH_MAX+1];
    TCHAR szFileNameDelete[PATH_MAX+1];
    TCHAR filename[PATH_MAX+1];
    uint32 i;
    FILE * fhIn;
    FILE * fhOut;
    TCHAR szBuf[1024+1];

    GetPluginDir((char*)szFileNameOut);
    GetPluginDir((char*)szFileNameDelete);
    sprintf((char*)filename, "%s.tmp", szIniFileName);
    strcat((char*)szFileNameOut, (char*)filename);
    sprintf((char*)filename, "%s.del", szIniFileName);
    strcat((char*)szFileNameDelete, (char*)filename);

    GetPluginDir((char*)filename);
    strcat((char*)filename,szIniFileName);
    fhIn = fopen((char*)filename, "r");
    if (fhIn == NULL)
    {
        // Create a new file
        fhOut = fopen((char*)filename,"w");
        fclose(fhOut);
        return;
    }

    fhOut = fopen((char*)szFileNameOut, "w");
    if (fhOut == NULL)
    {
        fclose(fhIn);
        return;
    }

    // Mark all sections and needing to be written
    for (i = 0; i < IniSections.size(); i++)
    {
        IniSections[i].bOutput = false;
    }


    while (fgets((char*)szBuf, 1024, fhIn))
    {
        if (szBuf[0] == '{')
        {
            BOOL bFound = FALSE;
            // Start of section
            tidy((char*)szBuf);
            szBuf[strlen((char*)szBuf)-1]='\0';

            for (i = 0; i < IniSections.size(); i++)
            {
                if (IniSections[i].bOutput)
                    continue;

                if (strcasecmp((char*)szBuf+1, IniSections[i].crccheck) == 0)
                {
                    // Output this CRC
                    OutputSectionDetails(i, fhOut);
                    IniSections[i].bOutput = true;
                    bFound = TRUE;
                    break;
                }
            }
            if (!bFound)
            {
                // Do what? This should never happen, unless the user
                // replaces the inifile while game is running!
            }
        }
        else if (szBuf[0] == '/')
        {
            // Comment
            fputs((char*)szBuf, fhOut);
            continue;
        }

    }

    // Input buffer done-  process any new entries!
    for (i = 0; i < IniSections.size(); i++)
    {
        // Skip any that have not been done.
        if (IniSections[i].bOutput)
            continue;
        // Output this CRC
        // Removed at request of Genueix :)
        //fprintf(fhOut, "// Automatically generated entry - may need editing\n");
        OutputSectionDetails(i, fhOut);
        IniSections[i].bOutput = true;
    }

    fclose(fhOut);
    fclose(fhIn);

    // Create the new file
    remove((char*)filename);
    rename((char*)szFileNameOut, (char*)filename);

    bIniIsChanged = false;
}

void OutputSectionDetails(uint32 i, FILE * fh)
{
    fprintf(fh, "{%s}\n", IniSections[i].crccheck);

    fprintf(fh, "Name=%s\n", IniSections[i].name);
    //fprintf(fh, "UCode=%d\n", IniSections[i].ucode);

    // Tri-state variables
    if (IniSections[i].dwAccurateTextureMapping != 0)
        fprintf(fh, "AccurateTextureMapping=%d\n", IniSections[i].dwAccurateTextureMapping);

    if (IniSections[i].dwFastTextureCRC != 0)
        fprintf(fh, "FastTextureCRC=%d\n", IniSections[i].dwFastTextureCRC);

    if (IniSections[i].dwNormalBlender != 0)
        fprintf(fh, "NormalAlphaBlender=%d\n", IniSections[i].dwNormalBlender);

    if (IniSections[i].dwNormalCombiner != 0)
        fprintf(fh, "NormalColorCombiner=%d\n", IniSections[i].dwNormalCombiner);


    // Normal bi-state variables
    if (IniSections[i].bDisableTextureCRC)
        fprintf(fh, "DisableTextureCRC\n");

    if (IniSections[i].bDisableCulling)
        fprintf(fh, "DisableCulling\n");

    if (IniSections[i].bPrimaryDepthHack)
        fprintf(fh, "PrimaryDepthHack\n");

    if (IniSections[i].bTexture1Hack)
        fprintf(fh, "Texture1Hack\n");

    if (IniSections[i].bFastLoadTile)
        fprintf(fh, "FastLoadTile\n");

    if (IniSections[i].bUseSmallerTexture)
        fprintf(fh, "UseSmallerTexture\n");

    if (IniSections[i].bIncTexRectEdge)
        fprintf(fh, "IncTexRectEdge\n");

    if (IniSections[i].bZHack)
        fprintf(fh, "ZHack\n");

    if (IniSections[i].bTextureScaleHack)
        fprintf(fh, "TexRectScaleHack\n");

    if (IniSections[i].VIWidth > 0)
        fprintf(fh, "VIWidth=%d\n", IniSections[i].VIWidth);

    if (IniSections[i].VIHeight > 0)
        fprintf(fh, "VIHeight=%d\n", IniSections[i].VIHeight);

    if (IniSections[i].UseCIWidthAndRatio > 0)
        fprintf(fh, "UseCIWidthAndRatio=%d\n", IniSections[i].UseCIWidthAndRatio);

    if (IniSections[i].dwFullTMEM > 0)
        fprintf(fh, "FullTMEM=%d\n", IniSections[i].dwFullTMEM);

    if (IniSections[i].bTxtSizeMethod2 != FALSE )
        fprintf(fh, "AlternativeTxtSizeMethod=%d\n", IniSections[i].bTxtSizeMethod2);

    if (IniSections[i].bEnableTxtLOD != FALSE )
        fprintf(fh, "EnableTxtLOD=%d\n", IniSections[i].bEnableTxtLOD);

    if (IniSections[i].bDisableObjBG != 0 )
        fprintf(fh, "DisableObjBG=%d\n", IniSections[i].bDisableObjBG);

    if (IniSections[i].bForceScreenClear != 0)
        fprintf(fh, "ForceScreenClear=%d\n", IniSections[i].bForceScreenClear);

    if (IniSections[i].bEmulateClear != 0)
        fprintf(fh, "EmulateClear=%d\n", IniSections[i].bEmulateClear);

    if (IniSections[i].bDisableBlender != 0)
        fprintf(fh, "DisableAlphaBlender=%d\n", IniSections[i].bDisableBlender);

    if (IniSections[i].bForceDepthBuffer != 0)
        fprintf(fh, "ForceDepthBuffer=%d\n", IniSections[i].bForceDepthBuffer);

    if (IniSections[i].dwFrameBufferOption != 0)
        fprintf(fh, "FrameBufferEmulation=%d\n", IniSections[i].dwFrameBufferOption);

    if (IniSections[i].dwRenderToTextureOption != 0)
        fprintf(fh, "RenderToTexture=%d\n", IniSections[i].dwRenderToTextureOption);

    if (IniSections[i].dwScreenUpdateSetting != 0)
        fprintf(fh, "ScreenUpdateSetting=%d\n", IniSections[i].dwScreenUpdateSetting);

    fprintf(fh, "\n");          // Spacer
}

// Find the entry corresponding to the specified rom. 
// If the rom is not found, a new entry is created
// The resulting value is returned
void __cdecl DebuggerAppendMsg (const char * Message, ...);
int FindIniEntry(uint32 dwCRC1, uint32 dwCRC2, uint8 nCountryID, char* szName)
{
    uint32 i;
    CHAR szCRC[50+1];

    // Generate the CRC-ID for this rom:
    sprintf((char*)szCRC, "%08x%08x-%02x", (unsigned int)dwCRC1, (unsigned int)dwCRC2, nCountryID);

    for (i = 0; i < IniSections.size(); i++)
    {
        if (strcasecmp((char*)szCRC, IniSections[i].crccheck) == 0)
        {
            printf("[RiceVideo] Found ROM '%s', CRC %s\n", IniSections[i].name, szCRC);
            return i;
        }
    }

    // Add new entry!!!
    section newsection;

    strcpy(newsection.crccheck, (char*)szCRC);

    strncpy(newsection.name, szName, 50);
    newsection.bDisableTextureCRC = FALSE;
    newsection.bDisableCulling = FALSE;
    newsection.bIncTexRectEdge = FALSE;
    newsection.bZHack = FALSE;
    newsection.bTextureScaleHack = FALSE;
    newsection.bFastLoadTile = FALSE;
    newsection.bUseSmallerTexture = FALSE;
    newsection.bPrimaryDepthHack = FALSE;
    newsection.bTexture1Hack = FALSE;
    newsection.bDisableObjBG = FALSE;
    newsection.VIWidth = -1;
    newsection.VIHeight = -1;
    newsection.UseCIWidthAndRatio = NOT_USE_CI_WIDTH_AND_RATIO;
    newsection.dwFullTMEM = 0;
    newsection.bTxtSizeMethod2 = FALSE;
    newsection.bEnableTxtLOD = FALSE;

    newsection.bEmulateClear = FALSE;
    newsection.bForceScreenClear = FALSE;
    newsection.bDisableBlender = FALSE;
    newsection.bForceDepthBuffer = FALSE;
    newsection.dwFastTextureCRC = 0;
    newsection.dwAccurateTextureMapping = 0;
    newsection.dwNormalBlender = 0;
    newsection.dwNormalCombiner = 0;
    newsection.dwFrameBufferOption = 0;
    newsection.dwRenderToTextureOption = 0;
    newsection.dwScreenUpdateSetting = 0;

    IniSections.push_back(newsection);

    bIniIsChanged = true;               // Flag to indicate we should be updated
    return IniSections.size()-1;            // -1 takes into account increment
}

GameSetting g_curRomInfo;

void ROM_GetRomNameFromHeader(TCHAR * szName, ROMHeader * pHdr)
{
    TCHAR * p;

    memcpy(szName, pHdr->szName, 20);
    szName[20] = '\0';

    p = szName + (strlen((char*)szName) -1);        // -1 to skip null
    while (p >= szName && *p == ' ')
    {
        *p = 0;
        p--;
    }
}

uint32 CountryCodeToTVSystem(uint32 countryCode)
{
    uint32 system;
    switch(countryCode)
    {
        /* Demo */
    case 0:
        system = TV_SYSTEM_NTSC;
        break;

    case '7':
        system = TV_SYSTEM_NTSC;
        break;

    case 0x41:
        system = TV_SYSTEM_NTSC;
        break;

        /* Germany */
    case 0x44:
        system = TV_SYSTEM_PAL;
        break;

        /* USA */
    case 0x45:
        system = TV_SYSTEM_NTSC;
        break;

        /* France */
    case 0x46:
        system = TV_SYSTEM_PAL;
        break;

        /* Italy */
    case 'I':
        system = TV_SYSTEM_PAL;
        break;

        /* Japan */
    case 0x4A:
        system = TV_SYSTEM_NTSC;
        break;

        /* Europe - PAL */
    case 0x50:
        system = TV_SYSTEM_PAL;
        break;

    case 'S':   /* Spain */
        system = TV_SYSTEM_PAL;
        break;

        /* Australia */
    case 0x55:
        system = TV_SYSTEM_PAL;
        break;

    case 0x58:
        system = TV_SYSTEM_PAL;
        break;

        /* Australia */
    case 0x59:
        system = TV_SYSTEM_PAL;
        break;

    case 0x20:
    case 0x21:
    case 0x38:
    case 0x70:
        system = TV_SYSTEM_PAL;
        break;

        /* ??? */
    default:
        system = TV_SYSTEM_PAL;
        break;
    }

    return system;
}


