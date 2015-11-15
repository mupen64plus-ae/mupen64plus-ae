/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: Paul Lamb, littleguy77
 */
package paulscode.android.mupen64plusae.jni;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;

public class NativeConfigFiles
{
    private final static String EMPTY = "\"\"";
    
    /**
     * Populates the core configuration files with the user preferences.
     */
    public static void syncConfigFiles( GamePrefs game, GlobalPrefs global, AppData appData )
    {
        //@formatter:off
        
        // gln64 config file
        ConfigFile gln64_conf = new ConfigFile( appData.gln64_conf );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "window width", String.valueOf( game.videoRenderWidth ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "window height", String.valueOf( game.videoRenderHeight ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "auto frameskip", boolToNum( game.isGln64AutoFrameskipEnabled ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "max frameskip", String.valueOf( game.gln64MaxFrameskip ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "polygon offset hack", boolToNum( global.isPolygonOffsetHackEnabled ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "polygon offset factor", String.valueOf( global.videoPolygonOffset ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "polygon offset units", String.valueOf( global.videoPolygonOffset ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "enable fog", boolToNum( game.isGln64FogEnabled ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "texture 2xSAI", boolToNum( game.isGln64SaiEnabled ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "enable alpha test", boolToNum( game.isGln64AlphaTestEnabled ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "force screen clear", boolToNum( game.isGln64ScreenClearEnabled ) );
        gln64_conf.put( ConfigFile.SECTIONLESS_NAME, "hack z", boolToNum( game.isGln64HackDepthEnabled ) );
        
        // glide64 config file
        ConfigFile glide64_conf = new ConfigFile( appData.glide64mk2_ini );
        glide64_conf.put( "DEFAULT", "aspect", "2" );                                                                       // Stretch to GameSurface, Java will manage aspect ratio
        
        // Core and rice config file
        ConfigFile mupen64plus_cfg = new ConfigFile( game.mupen64plus_cfg );
        
        mupen64plus_cfg.put( "Audio-SDL", "Version", "1.000000" );                                                          // Mupen64Plus SDL Audio Plugin config parameter version number
        mupen64plus_cfg.put( "Audio-SDL", "SWAP_CHANNELS", boolToTF( global.audioSwapChannels ) );                          // Swaps left and right channels
        mupen64plus_cfg.put( "Audio-SDL", "SECONDARY_BUFFER_SIZE", String.valueOf( global.audioSDLSecondaryBufferSize ) );  // Size of secondary buffer in output samples. This is SDL's hardware buffer.
        
        mupen64plus_cfg.put( "Audio-OpenSLES", "Version", "1.000000" );                                                          // Mupen64Plus OpenSLES Audio Plugin config parameter version number
        mupen64plus_cfg.put( "Audio-OpenSLES", "SWAP_CHANNELS", boolToTF( global.audioSwapChannels ) );                          // Swaps left and right channels
        mupen64plus_cfg.put( "Audio-OpenSLES", "SECONDARY_BUFFER_SIZE", String.valueOf( global.audioSLESSecondaryBufferSize ) ); // Size of secondary buffer in output samples. This is OpenSLES's hardware buffer.
        mupen64plus_cfg.put( "Audio-OpenSLES", "SECONDARY_BUFFER_NBR", String.valueOf( global.audioSLESSecondaryBufferNbr ) );   // Number of secondary buffer.
        
        mupen64plus_cfg.put( "Core", "Version", "1.010000" );                                                               // Mupen64Plus Core config parameter set version number.  Please don't change this version number.
        mupen64plus_cfg.put( "Core", "OnScreenDisplay", "False" );                                                          // Draw on-screen display if True, otherwise don't draw OSD
        mupen64plus_cfg.put( "Core", "R4300Emulator", game.r4300Emulator );                                                 // Use Pure Interpreter if 0, Cached Interpreter if 1, or Dynamic Recompiler if 2 or more
        mupen64plus_cfg.put( "Core", "AutoStateSlotIncrement", "False" );                                                   // Increment the save state slot after each save operation
        mupen64plus_cfg.put( "Core", "ScreenshotPath", '"' + game.screenshotDir + '"' );                                    // Path to directory where screenshots are saved. If this is blank, the default value of ${UserConfigPath}/screenshot will be used
        mupen64plus_cfg.put( "Core", "SaveStatePath", '"' + game.slotSaveDir + '"' );                                       // Path to directory where emulator save states (snapshots) are saved. If this is blank, the default value of ${UserConfigPath}/save will be used
        mupen64plus_cfg.put( "Core", "SaveSRAMPath", '"' + game.sramDataDir + '"' );                                        // Path to directory where SRAM/EEPROM data (in-game saves) are stored. If this is blank, the default value of ${UserConfigPath}/save will be used
        mupen64plus_cfg.put( "Core", "SharedDataPath", '"' + appData.coreSharedDataDir + '"' );                             // Path to a directory to search when looking for shared data files
        
        mupen64plus_cfg.put( "CoreEvents", "Version", "1.000000" );                                                         // Mupen64Plus CoreEvents config parameter set version number.  Please don't change this version number.
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Stop", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fullscreen", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Save State", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Load State", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increment Slot", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Reset", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Down", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Up", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Screenshot", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Pause", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Mute", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increase Volume", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Decrease Volume", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fast Forward", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Frame Advance", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Gameshark", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Stop", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Fullscreen", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Save State", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Load State", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Increment Slot", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Screenshot", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Pause", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Mute", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Increase Volume", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Decrease Volume", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Fast Forward", EMPTY );
        mupen64plus_cfg.put( "CoreEvents", "Joy Mapping Gameshark", EMPTY );
        
        mupen64plus_cfg.put( "UI-Console", "Version", "1.000000" );                                                         // Mupen64Plus UI-Console config parameter set version number.  Please don't change this version number.
        mupen64plus_cfg.put( "UI-Console", "PluginDir", '"' + appData.libsDir + '"' );                                      // Directory in which to search for plugins
        mupen64plus_cfg.put( "UI-Console", "VideoPlugin", '"' + game.videoPlugin.path + '"' );                              // Filename of video plugin
        mupen64plus_cfg.put( "UI-Console", "AudioPlugin", '"' + global.audioPlugin.path + '"' );                            // Filename of audio plugin
        mupen64plus_cfg.put( "UI-Console", "InputPlugin", '"' + appData.inputLib + '"' );                                   // Filename of input plugin
        mupen64plus_cfg.put( "UI-Console", "RspPlugin", '"' + appData.rspLib + '"' );                                       // Filename of RSP plugin
        
        mupen64plus_cfg.put( "Video-General", "Fullscreen", "False" );                                                      // Use fullscreen mode if True, or windowed mode if False
        mupen64plus_cfg.put( "Video-General", "ScreenWidth", String.valueOf( game.videoRenderWidth ) );                     // Width of output window or fullscreen width
        mupen64plus_cfg.put( "Video-General", "ScreenHeight", String.valueOf( game.videoRenderHeight ) );                   // Height of output window or fullscreen height
        mupen64plus_cfg.put( "Video-General", "VerticalSync", "False" );                                                    // If true, activate the SDL_GL_SWAP_CONTROL attribute
        
        mupen64plus_cfg.put( "Video-Glide64mk2", "vsync", "False" );                                                        // Vertical sync
        mupen64plus_cfg.put( "Video-Glide64mk2", "wrpAnisotropic", "False" );                                               // Wrapper Anisotropic Filtering
        mupen64plus_cfg.put( "Video-Glide64mk2", "fb_read_always", "0" );                                                   // Read framebuffer every frame (may be slow use only for effects that need it e.g. Banjo Kazooie, DK64 transitions)
        mupen64plus_cfg.put( "Video-Glide64mk2", "force_polygon_offset", boolToNum( global.isPolygonOffsetHackEnabled ) );  // If true, use polygon offset values specified below
        mupen64plus_cfg.put( "Video-Glide64mk2", "polygon_offset_factor", String.valueOf( global.videoPolygonOffset ) );    // Specifies a scale factor that is used to create a variable depth offset for each polygon
        mupen64plus_cfg.put( "Video-Glide64mk2", "polygon_offset_units", String.valueOf( global.videoPolygonOffset ) );     // Is multiplied by an implementation-specific value to create a constant depth offset
        mupen64plus_cfg.put( "Video-Glide64mk2", "autoframeskip", boolToNum( game.isGlide64AutoFrameskipEnabled ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "maxframeskip", String.valueOf( game.glide64MaxFrameskip ) );
        
        String aspectRatio = "0";
        if( game.emulationProfile.get( "WidescreenHack", "False" ).equals("True") )
            aspectRatio = "3";
        
        mupen64plus_cfg.put( "Video-GLideN64", "configVersion", "5" );                                                      // Settings version. Don't touch it.
        putGliden64( mupen64plus_cfg, game, "MultiSampling", "0" );                                                         // Enable/Disable MultiSampling (0=off, 2,4,8,16=quality)
        putGliden64( mupen64plus_cfg, game, "AspectRatio", aspectRatio );                                                   // Screen aspect ratio (0=stretch, 1=force 4:3, 2=force 16:9, 3=adjust)
        putGliden64( mupen64plus_cfg, game, "bilinearMode", "1" );                                                          // Bilinear filtering mode (0=N64 3point, 1=standard)
        putGliden64( mupen64plus_cfg, game, "MaxAnisotropy", "0" );                                                         // Max level of Anisotropic Filtering, 0 for off
        putGliden64( mupen64plus_cfg, game, "CacheSize", "500" );                                                           // Size of texture cache in megabytes. Good value is VRAM*3/4
        putGliden64( mupen64plus_cfg, game, "EnableFog", "True" );                                                          // Enable fog emulation.
        putGliden64( mupen64plus_cfg, game, "EnableNoise", "True" );                                                        // Enable color noise emulation.
        putGliden64( mupen64plus_cfg, game, "EnableLOD", "True" );                                                          // Enable LOD emulation.
        putGliden64( mupen64plus_cfg, game, "EnableHWLighting", "False" );                                                  // Enable hardware per-pixel lighting.
        putGliden64( mupen64plus_cfg, game, "EnableShaderStorage", "True" );                                                // Use persistent storage for compiled shaders.
        putGliden64( mupen64plus_cfg, game, "ForceGammaCorrection", "False" );                                              // Force gamma correction.
        putGliden64( mupen64plus_cfg, game, "GammaCorrectionLevel", "1.0" );                                                // Gamma correction value.
        putGliden64( mupen64plus_cfg, game, "EnableFBEmulation", "True" );                                                  // Enable frame and|or depth buffer emulation.
        putGliden64( mupen64plus_cfg, game, "EnableCopyColorToRDRAM", "2" );                                                // Enable color buffer copy to RDRAM (0=do not copy, 1=copy in sync mode, 2=copy in async mode)
        putGliden64( mupen64plus_cfg, game, "EnableCopyAuxiliaryToRDRAM", "False" );                                         // Enable auxiliary buffer copy to RDRAM.
        putGliden64( mupen64plus_cfg, game, "EnableCopyDepthToRDRAM", "False" );                                            // Enable depth buffer copy to RDRAM.
        putGliden64( mupen64plus_cfg, game, "EnableCopyColorFromRDRAM", "False" );                                          // Enable color buffer copy from RDRAM.
        putGliden64( mupen64plus_cfg, game, "EnableDetectCFB", "False" );                                                   // Detect CPU writes to frame buffer.
        putGliden64( mupen64plus_cfg, game, "EnableN64DepthCompare", "False" );                                             // Enable N64 depth compare instead of OpenGL standard one. Experimental.
        putGliden64( mupen64plus_cfg, game, "txFilterMode", "0" );                                                          // Texture filter (0=none, 1=Smooth filtering 1, 2=Smooth filtering 2, 3=Smooth filtering 3, 4=Smooth filtering 4, 5=Sharp filtering 1, 6=Sharp filtering 2)
        putGliden64( mupen64plus_cfg, game, "txEnhancementMode", "0" );                                                     // Texture Enhancement (0=none, 1=store as is, 2=X2, 3=X2SAI, 4=HQ2X, 5=HQ2XS, 6=LQ2X, 7=LQ2XS, 8=HQ4X, 9=2xBRZ, 10=3xBRZ, 11=4xBRZ, 12=5xBRZ)
        putGliden64( mupen64plus_cfg, game, "txFilterIgnoreBG", "False" );                                                  // Don't filter background textures.
        putGliden64( mupen64plus_cfg, game, "txCacheSize", "100" );                                                         // Size of filtered textures cache in megabytes.
        putGliden64( mupen64plus_cfg, game, "txHiresEnable", "False" );                                                     // Use high-resolution texture packs if available.
        putGliden64( mupen64plus_cfg, game, "txHiresFullAlphaChannel", "False" );                                           // Allow to use alpha channel of high-res texture fully.
        putGliden64( mupen64plus_cfg, game, "txHresAltCRC", "False" );                                                      // Use alternative method of paletted textures CRC calculation.
        mupen64plus_cfg.put( "Video-GLideN64", "txDump", "False" );                                                         // Enable dump of loaded N64 textures.
        putGliden64( mupen64plus_cfg, game, "txCacheCompression", "True" );                                                 // Zip textures cache.
        putGliden64( mupen64plus_cfg, game, "txForce16bpp", "False" );                                                      // Force use 16bit texture formats for HD textures.
        putGliden64( mupen64plus_cfg, game, "txSaveCache", "True" );                                                        // Save texture cache to hard disk.
        putGliden64( mupen64plus_cfg, game, "fontName", "DroidSans.ttf" );                                                  // File name of True Type Font for text messages.
        putGliden64( mupen64plus_cfg, game, "fontSize", "18" );                                                             // Font size.
        putGliden64( mupen64plus_cfg, game, "fontColor", "B5E61D" );                                                        // Font color in RGB format.
        putGliden64( mupen64plus_cfg, game, "EnableBloom", "0" );                                                           // Enable bloom filter
        putGliden64( mupen64plus_cfg, game, "bloomThresholdLevel", "4" );                                                   // Brightness threshold level for bloom. Values [2, 6]
        putGliden64( mupen64plus_cfg, game, "bloomBlendMode", "0" );                                                        // Bloom blend mode (0=Strong, 1=Mild, 2=Light)
        putGliden64( mupen64plus_cfg, game, "blurAmount", "10" );                                                           // Blur radius. Values [2, 10]
        putGliden64( mupen64plus_cfg, game, "blurStrength", "20" );                                                         // Blur strength. Values [10, 100]
        mupen64plus_cfg.put( "Video-GLideN64", "ForcePolygonOffset", boolToTF( global.isPolygonOffsetHackEnabled ) );       // If true, use polygon offset values specified below
        mupen64plus_cfg.put( "Video-GLideN64", "PolygonOffsetFactor", String.valueOf( global.videoPolygonOffset ) );        // Specifies a scale factor that is used to create a variable depth offset for each polygon
        mupen64plus_cfg.put( "Video-GLideN64", "PolygonOffsetUnits", String.valueOf( global.videoPolygonOffset ) );         // Is multiplied by an implementation-specific value to create a constant depth offset
        
        mupen64plus_cfg.put( "Video-Rice", "ForcePolygonOffset", boolToTF( global.isPolygonOffsetHackEnabled ) );           // If true, use polygon offset values specified below
        mupen64plus_cfg.put( "Video-Rice", "PolygonOffsetFactor", String.valueOf( global.videoPolygonOffset ) );            // Specifies a scale factor that is used to create a variable depth offset for each polygon
        mupen64plus_cfg.put( "Video-Rice", "PolygonOffsetUnits", String.valueOf( global.videoPolygonOffset ) );             // Is multiplied by an implementation-specific value to create a constant depth offset
        mupen64plus_cfg.put( "Video-Rice", "ScreenUpdateSetting", game.riceScreenUpdateType );                              // Control when the screen will be updated (0=ROM default, 1=VI origin update, 2=VI origin change, 3=CI change, 4=first CI change, 5=first primitive draw, 6=before screen clear, 7=after screen drawn)
        mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", boolToTF( game.isRiceFastTextureLoadingEnabled ) );        // Use a faster algorithm to speed up texture loading and CRC computation
        mupen64plus_cfg.put( "Video-Rice", "SkipFrame", boolToTF( game.isRiceAutoFrameskipEnabled ) );                      // If this option is enabled, the plugin will skip every other frame
        mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", boolToTF( game.isRiceHiResTexturesEnabled ) );              // Enable hi-resolution texture file loading
        if( game.isRiceForceTextureFilterEnabled )                                                                          // Force to use texture filtering or not (0=auto: n64 choose, 1=force no filtering, 2=force filtering)
            mupen64plus_cfg.put( "Video-Rice", "ForceTextureFilter", "2");
        else
            mupen64plus_cfg.put( "Video-Rice", "ForceTextureFilter", "0");
        mupen64plus_cfg.put( "Video-Rice", "TextureEnhancement", game.riceTextureEnhancement );                             // Primary texture enhancement filter (0=None, 1=2X, 2=2XSAI, 3=HQ2X, 4=LQ2X, 5=HQ4X, 6=Sharpen, 7=Sharpen More, 8=External, 9=Mirrored)
        mupen64plus_cfg.put( "Video-Rice", "TextureEnhancementControl", "1" );                                              // Secondary texture enhancement filter (0 = none, 1-4 = filtered)
        mupen64plus_cfg.put( "Video-Rice", "Mipmapping", "0" );                                                             // Use Mipmapping? 0=no, 1=nearest, 2=bilinear, 3=trilinear
        mupen64plus_cfg.put( "Video-Rice", "FogMethod", boolToNum( game.isRiceFogEnabled ) );                               // Enable, Disable or Force fog generation (0=Disable, 1=Enable n64 choose, 2=Force Fog)
        
        gln64_conf.save();
        glide64_conf.save();
        mupen64plus_cfg.save();
        
        //@formatter:on
    }
    
    private static void putGliden64( ConfigFile cfg, GamePrefs game, String key, String defaultValue )
    {
        // Just a temporary implementation until integration is complete
        cfg.put( "Video-GLideN64", key, game.emulationProfile.get( key, defaultValue ) );
    }
    
    private static String boolToTF( boolean b )
    {
        return b ? "True" : "False";
    }
    
    private static String boolToNum( boolean b )
    {
        return b ? "1" : "0";
    }
}
