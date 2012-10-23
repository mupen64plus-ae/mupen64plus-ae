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
import paulscode.android.mupen64plusae.persistent.Path;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.DataDownloader;

public class Globals
{
    // Global app/user data
    public static UserPrefs userPrefs;
    public static AppData appData;
    public static Path path;
    public static ConfigFile mupen64plus_cfg;
    public static ConfigFile gles2n64_conf;    

    // Frequently-used global objects
    // TODO: Eliminate some or all of these if possible
    public static TouchscreenView touchscreenInstance = null;
    public static SDLSurface surfaceInstance = null;
    public static GameActivity gameInstance = null;
    public static DataDownloader downloader = null;

    public static String extraArgs = null;
    public static boolean resumeLastSession = false;
    
    // Internal implementation options, might change later or expose to user
    public static final boolean INHIBIT_SUSPEND = true;
    public static final boolean DOWNLOAD_TO_SDCARD = true;
}
