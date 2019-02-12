/*
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

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GLideN64Prefs;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;

class NativeConfigFiles
{
    private final static String EMPTY = "\"\"";

    private static final int GZ_HIRESTEXCACHE = 0x00800000;
    private static final int FORCE16BPP_HIRESTEX = 0x10000000;
    private static final int LET_TEXARTISTS_FLY = 0x40000000;

    private static boolean hiresTexHTCPresent = false;
    private static boolean zipTextureCache = false;
    private static boolean force16bpp = false;
    private static boolean fullAlphaChannel = false;

    //True if this device supports full GL mode
    private static boolean supportsFullGl = false;

    /**
     * Populates the core configuration files with the user preferences.
     */
    static boolean syncConfigFiles(boolean useRaphnet, GamePrefs game, GlobalPrefs global, AppData appData)
    {
        //@formatter:off

        supportsFullGl = AppData.doesSupportFullGL();

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
        glide64_conf.put( "DEFAULT", "aspect", "2" );

        // Core and rice config file
        final ConfigFile mupen64plus_cfg = new ConfigFile( game.getMupen64plusCfg() );

        mupen64plus_cfg.put( "Audio-OpenSLES", "Version", "1.000000" );
        mupen64plus_cfg.put( "Audio-OpenSLES", "SWAP_CHANNELS", boolToTF( global.audioSwapChannels ) );
        mupen64plus_cfg.put( "Audio-OpenSLES", "SECONDARY_BUFFER_SIZE", String.valueOf( global.audioSLESSecondaryBufferSize ) );
        mupen64plus_cfg.put( "Audio-OpenSLES", "SECONDARY_BUFFER_NBR", String.valueOf( global.audioSLESSecondaryBufferNbr ) );
        mupen64plus_cfg.put( "Audio-OpenSLES", "SAMPLING_RATE", String.valueOf( global.audioSLESSamplingRate ) );
        mupen64plus_cfg.put( "Audio-OpenSLES", "SAMPLING_TYPE", String.valueOf( global.audioSLESSamplingType ) );
        mupen64plus_cfg.put( "Audio-OpenSLES", "TIME_STRETCH_ENABLED", boolToTF( global.enableSLESAudioTimeSretching ) );

        mupen64plus_cfg.put( "Core", "Version", "1.010000" );
        mupen64plus_cfg.put( "Core", "OnScreenDisplay", "False" );
        mupen64plus_cfg.put( "Core", "R4300Emulator", game.r4300Emulator );
        mupen64plus_cfg.put( "Core", "DisableExtraMem", boolToTF(game.disableExpansionPak) );
        mupen64plus_cfg.put( "Core", "AutoStateSlotIncrement", "False" );
        mupen64plus_cfg.put( "Core", "ScreenshotPath", '"' + game.getScreenshotDir() + '"' );
        mupen64plus_cfg.put( "Core", "SaveStatePath", '"' + game.getSlotSaveDir() + '"' );
        mupen64plus_cfg.put( "Core", "SaveSRAMPath", '"' + game.getSramDataDir() + '"' );
        mupen64plus_cfg.put( "Core", "SharedDataPath", '"' + appData.coreSharedDataDir + '"' );
        mupen64plus_cfg.put( "Core", "CountPerOp", String.valueOf( game.countPerOp ) );

        mupen64plus_cfg.put( "CoreEvents", "Version", "1.000000" );
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

        mupen64plus_cfg.put( "UI-Console", "Version", "1.000000" );
        mupen64plus_cfg.put( "UI-Console", "PluginDir", '"' + appData.libsDir + '"' );

        mupen64plus_cfg.put( "Transferpak", "GB-rom-1", game.getTransferPakRom(1) );
        mupen64plus_cfg.put( "Transferpak", "GB-ram-1", game.getTransferPakRam(1) );
        mupen64plus_cfg.put( "Transferpak", "GB-rom-2", game.getTransferPakRom(2) );
        mupen64plus_cfg.put( "Transferpak", "GB-ram-2", game.getTransferPakRam(2) );
        mupen64plus_cfg.put( "Transferpak", "GB-rom-3", game.getTransferPakRom(3) );
        mupen64plus_cfg.put( "Transferpak", "GB-ram-3", game.getTransferPakRam(3) );
        mupen64plus_cfg.put( "Transferpak", "GB-rom-4", game.getTransferPakRom(4) );
        mupen64plus_cfg.put( "Transferpak", "GB-ram-4", game.getTransferPakRam(4) );

        //Add safety checks to prevent users from manually inputting unsupported plugins for their device
        String videoPluginString = game.videoPlugin.path;

        if(game.isGliden64Enabled)
        {
            // Fix old format GLideN64 library using regular expression, for example, this will replace
            // libmupen64plus-video-gliden64-gles3.so with libmupen64plus-video-gliden64.so
            videoPluginString = videoPluginString.replaceAll("libmupen64plus-video-gliden64.*so", "libmupen64plus-video-gliden64.so");
        }

        if(game.isGlide64Enabled && supportsFullGl)
        {
            //Use the full GL version of Glide64mk2 if it's supported by the device
            videoPluginString = videoPluginString.replace("libmupen64plus-video-glide64mk2.so", "libmupen64plus-video-glide64mk2-egl.so");
        }

        // Angrylion was replaced with Angrylion RDP Plus
        videoPluginString = videoPluginString.replaceAll("libmupen64plus-video-angrylion.so", "libmupen64plus-video-angrylion-rdp-plus.so");

        mupen64plus_cfg.put( "UI-Console", "VideoPlugin", '"' + videoPluginString + '"' );

        //Use the FP version of the SLES audio plugin if the API level is high enough
        String audioPluginString = global.audioPlugin.path;

        if(audioPluginString.endsWith("libmupen64plus-audio-sles.so"))
        {
            //If running lollipop, use the floating point version
            if(AppData.IS_LOLLIPOP && global.audioSLESFloatingPoint)
            {
                audioPluginString = audioPluginString.replace("libmupen64plus-audio-sles.so", "libmupen64plus-audio-sles-fp.so");
            }
        }

        mupen64plus_cfg.put( "UI-Console", "AudioPlugin", '"' + audioPluginString + '"' );

        if (useRaphnet) {
            mupen64plus_cfg.put( "UI-Console", "InputPlugin", '"' + appData.inputLibRaphnet + '"' );
        } else {
            mupen64plus_cfg.put( "UI-Console", "InputPlugin", '"' + appData.inputLib + '"' );
        }

        mupen64plus_cfg.put( "UI-Console", "RspPlugin", '"' + game.rspPluginPath + '"' );

        mupen64plus_cfg.put( "rsp-cxd4", "Version", "1" );
        mupen64plus_cfg.put( "rsp-cxd4", "DisplayListToGraphicsPlugin", boolToNum(game.rspHleVideo) );
        mupen64plus_cfg.put( "rsp-cxd4", "AudioListToAudioPlugin", "0" );
        mupen64plus_cfg.put( "rsp-cxd4", "WaitForCPUHost", "0" );
        mupen64plus_cfg.put( "rsp-cxd4", "SupportCPUSemaphoreLock", "0" );
            
        mupen64plus_cfg.put( "Rsp-HLE", "Version", "1" );
        mupen64plus_cfg.put( "Rsp-HLE", "RspFallback", appData.libsDir + "/libmupen64plus-rsp-cxd4.so" );
        mupen64plus_cfg.put( "Rsp-HLE", "DisplayListToGraphicsPlugin", "1" );
        mupen64plus_cfg.put( "Rsp-HLE", "AudioListToAudioPlugin", "0" );

        mupen64plus_cfg.put( "Video-General", "Fullscreen", "False" );
        mupen64plus_cfg.put( "Video-General", "ScreenWidth", String.valueOf( game.videoRenderWidth ) );
        mupen64plus_cfg.put( "Video-General", "ScreenHeight", String.valueOf( game.videoRenderHeight ) );
        mupen64plus_cfg.put( "Video-General", "VerticalSync", "False" );

        mupen64plus_cfg.put( "Video-Glide64mk2", "vsync", "False" );
        mupen64plus_cfg.put( "Video-Glide64mk2", "force_polygon_offset", boolToNum( global.isPolygonOffsetHackEnabled ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "polygon_offset_factor", String.valueOf( global.videoPolygonOffset ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "polygon_offset_units", String.valueOf( global.videoPolygonOffset ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "autoframeskip", boolToNum( game.glide64mk2Prefs.autoFrameskipEnabled ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "maxframeskip", String.valueOf( game.glide64mk2Prefs.maxFrameskip ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "filtering", String.valueOf( game.glide64mk2Prefs.filtering ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fog", String.valueOf( game.glide64mk2Prefs.fog ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "buff_clear", String.valueOf( game.glide64mk2Prefs.buff_clear ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "swapmode", String.valueOf( game.glide64mk2Prefs.swapmode ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "lodmode", String.valueOf( game.glide64mk2Prefs.lodmode ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fb_smart", String.valueOf( game.glide64mk2Prefs.fb_smart ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fb_hires", String.valueOf( game.glide64mk2Prefs.fb_hires ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fb_get_info", String.valueOf( game.glide64mk2Prefs.fb_get_info ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fb_render", String.valueOf( game.glide64mk2Prefs.fb_render ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fb_crc_mode", String.valueOf( game.glide64mk2Prefs.fb_crc_mode ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "read_back_to_screen", String.valueOf( game.glide64mk2Prefs.read_back_to_screen ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "detect_cpu_write", String.valueOf( game.glide64mk2Prefs.detect_cpu_write ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "alt_tex_size", String.valueOf( game.glide64mk2Prefs.alt_tex_size ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "use_sts1_only", String.valueOf( game.glide64mk2Prefs.use_sts1_only ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fast_crc", String.valueOf( game.glide64mk2Prefs.fast_crc ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "force_microcheck", String.valueOf( game.glide64mk2Prefs.force_microcheck ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "force_quad3d", String.valueOf( game.glide64mk2Prefs.force_quad3d ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "optimize_texrect", String.valueOf( game.glide64mk2Prefs.optimize_texrect ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "hires_buf_clear", String.valueOf( game.glide64mk2Prefs.hires_buf_clear ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fb_read_alpha", String.valueOf( game.glide64mk2Prefs.fb_read_alpha ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "force_calc_sphere", String.valueOf( game.glide64mk2Prefs.force_calc_sphere ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "texture_correction", String.valueOf( game.glide64mk2Prefs.texture_correction ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "increase_texrect_edge", String.valueOf( game.glide64mk2Prefs.increase_texrect_edge ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "decrease_fillrect_edge", String.valueOf( game.glide64mk2Prefs.decrease_fillrect_edge ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "stipple_mode", String.valueOf( game.glide64mk2Prefs.stipple_mode ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "stipple_pattern", String.valueOf( game.glide64mk2Prefs.stipple_pattern ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "clip_zmax", String.valueOf( game.glide64mk2Prefs.clip_zmax ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "clip_zmin", String.valueOf( game.glide64mk2Prefs.clip_zmin ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "adjust_aspect", String.valueOf( game.glide64mk2Prefs.adjust_aspect ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "correct_viewport", String.valueOf( game.glide64mk2Prefs.correct_viewport ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "aspect", String.valueOf( game.glide64mk2Prefs.aspect ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "zmode_compare_less", String.valueOf( game.glide64mk2Prefs.zmode_compare_less ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "old_style_adither", String.valueOf( game.glide64mk2Prefs.old_style_adither ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "n64_z_scale", String.valueOf( game.glide64mk2Prefs.n64_z_scale ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "pal230", String.valueOf( game.glide64mk2Prefs.pal230 ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "ignore_aux_copy", String.valueOf( game.glide64mk2Prefs.ignore_aux_copy ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "useless_is_useless", String.valueOf( game.glide64mk2Prefs.useless_is_useless ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "fb_read_always", String.valueOf( game.glide64mk2Prefs.fb_read_always ) );
        mupen64plus_cfg.put( "Video-Glide64mk2", "wrpAnisotropic", String.valueOf( game.glide64mk2Prefs.wrpAnisotropic ) );

        String aspectRatio = "0";
        if( game.emulationProfile.get( "WidescreenHack", "False" ).equals("True") )
            aspectRatio = "3";

        readHiResSettings(game, global);

        // gln64 config file
        final ConfigFile glideN64_conf = new ConfigFile( appData.glideN64_conf );

        mupen64plus_cfg.put( "Video-GLideN64", "configVersion", String.valueOf(GLideN64Prefs.VERSION) );

        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "AspectRatio", aspectRatio);
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "ForcePolygonOffset", boolToTF( global.isPolygonOffsetHackEnabled ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "PolygonOffsetFactor", String.valueOf( global.videoPolygonOffset ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "PolygonOffsetUnits", String.valueOf( global.videoPolygonOffset ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "FXAA", boolToTF( game.glideN64Prefs.fxaa ));
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "bilinearMode", boolToTF( game.glideN64Prefs.bilinearMode ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "enableHalosRemoval", boolToTF( game.glideN64Prefs.enableHalosRemoval ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "MaxAnisotropy", String.valueOf( game.glideN64Prefs.maxAnisotropy ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableNoise", boolToTF( game.glideN64Prefs.enableNoise ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableLOD", boolToTF( game.glideN64Prefs.enableLOD ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableHWLighting", boolToTF( game.glideN64Prefs.enableHWLighting ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableLegacyBlending", boolToTF( game.glideN64Prefs.enableLegacyBlending) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableFragmentDepthWrite", boolToTF( game.glideN64Prefs.enableFragmentDepthWrite) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "CorrectTexrectCoords", String.valueOf( game.glideN64Prefs.correctTexrectCoords ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableNativeResTexrects", boolToTF( game.glideN64Prefs.enableNativeResTexrects) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "BackgroundsMode", String.valueOf( game.glideN64Prefs.backgroundMode ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableBlitScreenWorkaround", boolToTF( global.enableBlitScreenWorkaround) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableFBEmulation", boolToTF( game.glideN64Prefs.enableFBEmulation ));
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "BufferSwapMode", String.valueOf( game.glideN64Prefs.bufferSwapMode ));
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableCopyColorToRDRAM", String.valueOf( game.glideN64Prefs.enableCopyColorToRDRAM ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableCopyAuxiliaryToRDRAM", boolToTF( game.glideN64Prefs.enableCopyAuxiliaryToRDRAM ));
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableCopyDepthToRDRAM", String.valueOf( game.glideN64Prefs.enableCopyDepthToRDRAM ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableCopyColorFromRDRAM", boolToTF( game.glideN64Prefs.enableCopyColorFromRDRAM ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "EnableN64DepthCompare", boolToTF( game.glideN64Prefs.enableN64DepthCompare ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "ForceDepthBufferClear", boolToTF( game.glideN64Prefs.forceDepthBufferClear ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "UseNativeResolutionFactor", String.valueOf( game.glideN64Prefs.useNativeResolutionFactor ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txFilterMode", String.valueOf( game.glideN64Prefs.txFilterMode ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txEnhancementMode", String.valueOf( game.glideN64Prefs.txEnhancementMode ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txDeposterize", boolToTF( game.glideN64Prefs.txDeposterize ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txFilterIgnoreBG", boolToTF( game.glideN64Prefs.txFilterIgnoreBG ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txCacheSize", String.valueOf( game.glideN64Prefs.txCacheSize ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txHiresEnable", boolToTF( game.glideN64Prefs.txHiresEnable ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txPath", global.hiResTextureDir);
        
        if(hiresTexHTCPresent && game.glideN64Prefs.txHiresEnable)
        {
            putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txHiresFullAlphaChannel", boolToTF( fullAlphaChannel ) );
            putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txCacheCompression", boolToTF( zipTextureCache ) );
            putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txForce16bpp", boolToTF( force16bpp ) );
            putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txSaveCache", boolToTF( true ) );
        }
        else
        {
            putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txHiresFullAlphaChannel", boolToTF( game.glideN64Prefs.txHiresFullAlphaChannel ) );
            putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txCacheCompression", boolToTF( game.glideN64Prefs.txCacheCompression ) );
            putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txForce16bpp", boolToTF( game.glideN64Prefs.txForce16bpp ) );
            putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txSaveCache", boolToTF( game.glideN64Prefs.txSaveCache ) );
        }
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "txHresAltCRC", boolToTF( game.glideN64Prefs.txHresAltCRC ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "fontName", "DroidSans.ttf" );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "fontSize", "18" );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "fontColor", "B5E61D" );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "ForceGammaCorrection", boolToTF( game.glideN64Prefs.forceGammaCorrection ) );
        putGLideN64Setting(mupen64plus_cfg, glideN64_conf, game, "GammaCorrectionLevel", String.valueOf( game.glideN64Prefs.gammaCorrectionLevel ) );

        mupen64plus_cfg.put( "Video-Rice", "ForcePolygonOffset", boolToTF( global.isPolygonOffsetHackEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "PolygonOffsetFactor", String.valueOf( global.videoPolygonOffset ) );
        mupen64plus_cfg.put( "Video-Rice", "PolygonOffsetUnits", String.valueOf( global.videoPolygonOffset ) );
        mupen64plus_cfg.put( "Video-Rice", "ScreenUpdateSetting", game.riceScreenUpdateType );
        mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", boolToTF( game.isRiceFastTextureLoadingEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "SkipFrame", boolToTF( game.isRiceAutoFrameskipEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", boolToTF( game.isRiceHiResTexturesEnabled ) );
        if( game.isRiceForceTextureFilterEnabled )
            mupen64plus_cfg.put( "Video-Rice", "ForceTextureFilter", "2");
        else
            mupen64plus_cfg.put( "Video-Rice", "ForceTextureFilter", "0");
        mupen64plus_cfg.put( "Video-Rice", "TextureEnhancement", game.riceTextureEnhancement );
        mupen64plus_cfg.put( "Video-Rice", "TextureEnhancementControl", "1" );
        mupen64plus_cfg.put( "Video-Rice", "Mipmapping", "0" );
        mupen64plus_cfg.put( "Video-Rice", "FogMethod", boolToNum( game.isRiceFogEnabled ) );

        mupen64plus_cfg.put( "Video-Angrylion-Plus", "Parallel", boolToTF( game.angrylionPlusPrefs.parallel ) );
        mupen64plus_cfg.put( "Video-Angrylion-Plus", "NumWorkers", String.valueOf(game.angrylionPlusPrefs.numWorkers));
        mupen64plus_cfg.put( "Video-Angrylion-Plus", "ViMode", String.valueOf(game.angrylionPlusPrefs.viMode) );
        mupen64plus_cfg.put( "Video-Angrylion-Plus", "ViInterpolation", String.valueOf(game.angrylionPlusPrefs.viInterpolation) );
        mupen64plus_cfg.put( "Video-Angrylion-Plus", "ViWidescreen", boolToTF( game.angrylionPlusPrefs.viWidescreen ) );
        mupen64plus_cfg.put( "Video-Angrylion-Plus", "ViHideOverscan", boolToTF( game.angrylionPlusPrefs.viHideOverscan ) );

        gln64_conf.save();
        glide64_conf.save();
        return mupen64plus_cfg.save();

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

    private static void readHiResSettings( GamePrefs game, GlobalPrefs global)
    {
        final String hiResHtc = global.textureCacheDir + "/" + game.gameHeaderName + "_HIRESTEXTURES.htc";
        final File htcFile = new File(hiResHtc);

        hiresTexHTCPresent = htcFile.exists();

        if(hiresTexHTCPresent)
        {
            InputStream stream;
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
            catch (final IOException e)
            {
                hiresTexHTCPresent = false;
            }
            finally
            {
                try
                {
                    if(gzipStream != null)
                        gzipStream.close();
                }
                catch (final IOException e)
                {
                    Log.e("NativeConfigFiles", "Unable to close gzip file");
                }
            }
        }
    }

    /**
     * The Glide N64 config file contains override values for games to work correctly. If a value
     * is present in that file for a specifc game, use that instead
     * @param mupenConfigFile Mupen64Plus config file
     * @param glideN64ConfigFile GLideN64 config file
     * @param game Game preferences
     * @param setting Setting value to look up
     * @param value Value to use if setting is not present in the file
     */
    private static void putGLideN64Setting(ConfigFile mupenConfigFile, ConfigFile glideN64ConfigFile,
        GamePrefs game, String setting, String value)
    {
        String headerNameURL = game.gameHeaderName;

        try {
            headerNameURL = java.net.URLEncoder.encode(headerNameURL, "UTF-8");
            headerNameURL = headerNameURL.replace("+", "%20").toUpperCase();
        } catch (UnsupportedEncodingException|java.lang.NullPointerException e) {
            Log.e("NativeConfigFile", "Error on loading gameHeaderNameUrl: e=" + e.toString());
        }

        String glideN64settingValue = glideN64ConfigFile.get(headerNameURL, setting);
        // TODO: A lot of devices have issues with EnableCopyColorToRDRAM=2 for GLES 2.0
        // due to incompatibility with Android Native buffers, due to this, don't load
        // default values from GLideN64.custom.ini when running with GLES 2.0 for EnableCopyColorToRDRAM
        // because it could be set that way there.
        // For GLES-3.0/3.1, some devices don't support fast async reads
        if (glideN64settingValue != null &&
                !(!supportsFullGl && setting.equals("EnableCopyColorToRDRAM")))
        {
            mupenConfigFile.put( "Video-GLideN64", setting, glideN64settingValue);

            Log.i("NativeConfigFile", "param(override)=" + setting + " value=" + glideN64settingValue);
        }
        else
        {
            mupenConfigFile.put( "Video-GLideN64", setting, value);
            Log.i("NativeConfigFile", "param=" + setting + " value=" + value);
        }
    }

}
