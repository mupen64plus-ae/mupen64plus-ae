/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.Paths;
import paulscode.android.mupen64plusae.persistent.UserPrefs;

public class Globals
{
    // Global preferences/settings
    public static UserPrefs userPrefs;
    public static AppData appData;
    public static Paths paths;
    
    // Frequently-used global objects
    // TODO: Eliminate as many of these as possible
    public static String extraArgs = ".";
    public static boolean finishedReading = false;
    public static boolean resumeLastSession = false;
    
    // Internal development options, might change later or expose to user
    public static final boolean INHIBIT_SUSPEND = true;
    public static final boolean DOWNLOAD_TO_SDCARD = true;
    public static final int SPLASH_DELAY = 1000; // Such a nice picture... shame not to see it :)
    
    /**
     * Populates the core config files with the user preferences.
     */
    public static void syncConfigFiles()
    {
        // TODO: Confirm all booleans (some might need to be negated)
        
        // GLES2N64 config file
        ConfigFile gles2n64_conf = new ConfigFile( paths.gles2n64_conf );
        gles2n64_conf.put( "[<sectionless!>]", "enable fog",
                booleanToString( userPrefs.isGles2N64FogEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "enable alpha test",
                booleanToString( userPrefs.isGles2N64AlphaTestEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "force screen clear",
                booleanToString( userPrefs.isGles2N64ScreenClearEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "hack z",
                booleanToString( !userPrefs.isGles2N64DepthTestEnabled ) );
        gles2n64_conf.save();
        
        // Core and GLES2RICE config file
        ConfigFile mupen64plus_cfg = new ConfigFile( paths.mupen64plus_cfg );
        mupen64plus_cfg.put( "Core", "Version", "1.00" );
        mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        mupen64plus_cfg.put( "Video-Rice", "SkipFrame",
                booleanToString( userPrefs.isGles2RiceAutoFrameskipEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading",
                booleanToString( userPrefs.isGles2RiceFastTextureLoadingEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "FastTextureCRC",
                booleanToString( userPrefs.isGles2RiceFastTextureCrcEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures",
                booleanToString( userPrefs.isGles2RiceHiResTexturesEnabled ) );
        mupen64plus_cfg.put( "Input-SDL-Control1", "Version", "1.00" );
        mupen64plus_cfg.put( "Input-SDL-Control2", "Version", "1.00" );
        mupen64plus_cfg.put( "Input-SDL-Control3", "Version", "1.00" );
        mupen64plus_cfg.put( "Input-SDL-Control4", "Version", "1.00" );
        mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );
        mupen64plus_cfg.put( "UI-Console", "PluginDir", '"' + paths.libsDir + '"' );
        mupen64plus_cfg.put( "UI-Console", "VideoPlugin", '"' + userPrefs.videoPlugin + '"' );
        mupen64plus_cfg.put( "UI-Console", "AudioPlugin", '"' + userPrefs.audioPlugin + '"' );
        mupen64plus_cfg.put( "UI-Console", "InputPlugin", '"' + userPrefs.inputPlugin + '"' );
        mupen64plus_cfg.put( "UI-Console", "RspPlugin", '"' + userPrefs.rspPlugin + '"' );
        mupen64plus_cfg.save();
    }
    
    private static String booleanToString( boolean b )
    {
        return b
                ? "1"
                : "0";
    }
}
