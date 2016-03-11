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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;

public class NativeConfigFiles
{
    private final static String EMPTY = "\"\"";

    private static final int GZ_HIRESTEXCACHE = 0x00800000;
    private static final int FORCE16BPP_HIRESTEX = 0x10000000;
    private static final int LET_TEXARTISTS_FLY = 0x40000000;

    private static boolean hiresTexHTCPresent = false;
    private static boolean zipTextureCache = false;
    private static boolean force16bpp = false;
    private static boolean fullAlphaChannel = false;

    /**
     * Populates the core configuration files with the user preferences.
     */
    public static void syncConfigFiles( GamePrefs game, GlobalPrefs global, AppData appData )
    {
        //@formatter:off

        // gln64 config file
        final ConfigFile gln64_conf = new ConfigFile( appData.gln64_conf );
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
        final ConfigFile glide64_conf = new ConfigFile( appData.glide64mk2_ini );
        glide64_conf.put( "DEFAULT", "aspect", "2" );                                                                       // Stretch to GameSurface, Java will manage aspect ratio

        // Core and rice config file
        final ConfigFile mupen64plus_cfg = new ConfigFile( game.mupen64plus_cfg );

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

        readHiResSettings(game, global, appData);

        mupen64plus_cfg.put( "Video-GLideN64", "configVersion", "7" );
        mupen64plus_cfg.put( "Video-GLideN64", "AspectRatio", aspectRatio);
        mupen64plus_cfg.put( "Video-GLideN64", "ForcePolygonOffset", boolToTF( global.isPolygonOffsetHackEnabled ) );
        mupen64plus_cfg.put( "Video-GLideN64", "PolygonOffsetFactor", String.valueOf( global.videoPolygonOffset ) );
        mupen64plus_cfg.put( "Video-GLideN64", "PolygonOffsetUnits", String.valueOf( global.videoPolygonOffset ) );
        mupen64plus_cfg.put( "Video-GLideN64", "MultiSampling", String.valueOf( game.gliden64MultiSampling ));
        mupen64plus_cfg.put( "Video-GLideN64", "bilinearMode", String.valueOf( game.gliden64BilinearMode ) );
        mupen64plus_cfg.put( "Video-GLideN64", "MaxAnisotropy", String.valueOf( game.gliden64MaxAnisotropy ) );
        mupen64plus_cfg.put( "Video-GLideN64", "CacheSize", String.valueOf( game.gliden64CacheSize ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableFog", boolToNum( game.gliden64EnableFog ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableNoise", boolToNum( game.gliden64EnableNoise ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableLOD", boolToNum( game.gliden64EnableLOD ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableHWLighting", boolToNum( game.gliden64EnableHWLighting ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableShaderStorage", boolToNum( game.gliden64EnableShaderStorage ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableFBEmulation", boolToNum( game.gliden64EnableFBEmulation ));
        mupen64plus_cfg.put( "Video-GLideN64", "BufferSwapMode", String.valueOf( game.gliden64BufferSwapMode ));
        mupen64plus_cfg.put( "Video-GLideN64", "EnableCopyColorToRDRAM", String.valueOf( game.gliden64EnableCopyColorToRDRAM ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableCopyAuxiliaryToRDRAM", boolToNum( game.gliden64EnableCopyAuxiliaryToRDRAM ));
        mupen64plus_cfg.put( "Video-GLideN64", "EnableCopyDepthToRDRAM", boolToNum( game.gliden64EnableCopyDepthToRDRAM ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableCopyColorFromRDRAM", boolToNum( game.gliden64EnableCopyColorFromRDRAM ) );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableN64DepthCompare", boolToNum( game.gliden64EnableN64DepthCompare ) );
        mupen64plus_cfg.put( "Video-GLideN64", "UseNativeResolutionFactor", String.valueOf( game.gliden64UseNativeResolutionFactor ) );
        mupen64plus_cfg.put( "Video-GLideN64", "DisableFBInfo", boolToNum( game.gliden64DisableFBInfo ) );
        mupen64plus_cfg.put( "Video-GLideN64", "FBInfoReadColorChunk", boolToNum( game.gliden64FBInfoReadColorChunk ) );
        mupen64plus_cfg.put( "Video-GLideN64", "FBInfoReadDepthChunk", boolToNum( game.gliden64FBInfoReadDepthChunk ) );
        mupen64plus_cfg.put( "Video-GLideN64", "txFilterMode", String.valueOf( game.gliden64TxFilterMode ) );
        mupen64plus_cfg.put( "Video-GLideN64", "txEnhancementMode", String.valueOf( game.gliden64TxEnhancementMode ) );
        mupen64plus_cfg.put( "Video-GLideN64", "txFilterIgnoreBG", boolToNum( game.gliden64TxFilterIgnoreBG ) );
        mupen64plus_cfg.put( "Video-GLideN64", "txCacheSize", String.valueOf( game.gliden64TxCacheSize ) );
        mupen64plus_cfg.put( "Video-GLideN64", "txHiresEnable", boolToNum( game.gliden64TxHiresEnable ) );

        if(hiresTexHTCPresent && game.gliden64TxHiresEnable)
        {
            mupen64plus_cfg.put( "Video-GLideN64", "txHiresFullAlphaChannel", boolToNum( fullAlphaChannel ) );
            mupen64plus_cfg.put( "Video-GLideN64", "txCacheCompression", boolToNum( zipTextureCache ) );
            mupen64plus_cfg.put( "Video-GLideN64", "txForce16bpp", boolToNum( force16bpp ) );
            mupen64plus_cfg.put( "Video-GLideN64", "txSaveCache", boolToNum( true ) );
        }
        else
        {
            mupen64plus_cfg.put( "Video-GLideN64", "txHiresFullAlphaChannel", boolToNum( game.gliden64TxHiresFullAlphaChannel ) );
            mupen64plus_cfg.put( "Video-GLideN64", "txCacheCompression", boolToNum( game.gliden64TxCacheCompression ) );
            mupen64plus_cfg.put( "Video-GLideN64", "txForce16bpp", boolToNum( game.gliden64TxForce16bpp ) );
            mupen64plus_cfg.put( "Video-GLideN64", "txSaveCache", boolToNum( game.gliden64TxSaveCache ) );
        }
        mupen64plus_cfg.put( "Video-GLideN64", "txDump", "0" );
        mupen64plus_cfg.put( "Video-GLideN64", "txHresAltCRC", boolToNum( game.gliden64TxHresAltCRC ) );
        mupen64plus_cfg.put( "Video-GLideN64", "fontName", "DroidSans.ttf" );
        mupen64plus_cfg.put( "Video-GLideN64", "fontSize", "18" );
        mupen64plus_cfg.put( "Video-GLideN64", "fontColor", "B5E61D" );
        mupen64plus_cfg.put( "Video-GLideN64", "EnableBloom", boolToNum( game.gliden64EnableBloom ) );
        mupen64plus_cfg.put( "Video-GLideN64", "bloomThresholdLevel", String.valueOf( game.gliden64BloomThresholdLevel ) );
        mupen64plus_cfg.put( "Video-GLideN64", "bloomBlendMode", String.valueOf( game.gliden64BloomBlendMode ) );                                                        // Bloom blend mode (0=Strong, 1=Mild, 2=Light)
        mupen64plus_cfg.put( "Video-GLideN64", "blurAmount", String.valueOf( game.gliden64BlurAmount ) );
        mupen64plus_cfg.put( "Video-GLideN64", "blurStrength", String.valueOf( game.gliden64BlurStrength ) );
        mupen64plus_cfg.put( "Video-GLideN64", "ForceGammaCorrection", boolToNum( game.gliden64ForceGammaCorrection ) );
        mupen64plus_cfg.put( "Video-GLideN64", "GammaCorrectionLevel", String.valueOf( game.gliden64GammaCorrectionLevel ) );

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

    private static String boolToTF( boolean b )
    {
        return b ? "True" : "False";
    }

    private static String boolToNum( boolean b )
    {
        return b ? "1" : "0";
    }

    private static void readHiResSettings( GamePrefs game, GlobalPrefs global, AppData appData)
    {
        final String hiResHtc = global.textureCacheDir + "/" + game.gameHeaderName + "_HIRESTEXTURES.htc";
        final File htcFile = new File(hiResHtc);

        hiresTexHTCPresent = htcFile.exists();

        if(hiresTexHTCPresent)
        {
            InputStream stream = null;
            InputStream gzipStream = null;

            try
            {
                stream = new FileInputStream(htcFile);
                gzipStream = new GZIPInputStream(stream);

                final byte[] buffer = new byte[4];

                gzipStream.read(buffer);
                final ByteBuffer wrapped = ByteBuffer.wrap(buffer);
                wrapped.order(ByteOrder.LITTLE_ENDIAN);
                final int config = wrapped.getInt();

                zipTextureCache = (config & GZ_HIRESTEXCACHE) == GZ_HIRESTEXCACHE;
                force16bpp = (config & FORCE16BPP_HIRESTEX) == FORCE16BPP_HIRESTEX;
                fullAlphaChannel = (config & LET_TEXARTISTS_FLY) == LET_TEXARTISTS_FLY;
            }
            catch (final FileNotFoundException e)
            {
                hiresTexHTCPresent = false;
            }
            catch (final IOException e)
            {
                hiresTexHTCPresent = false;
            }
            finally
            {
                try
                {
                    gzipStream.close();
                }
                catch (final IOException e)
                {
                }
            }
        }
    }
}
