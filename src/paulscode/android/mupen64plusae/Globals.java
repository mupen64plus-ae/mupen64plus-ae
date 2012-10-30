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
    public static ConfigFile mupen64plus_cfg;
    public static ConfigFile gles2n64_conf;    

    // Frequently-used global objects
    // TODO: Eliminate as many of these as possible
    public static TouchscreenView touchscreenView = null;
    public static SDLSurface sdlSurface = null;
    public static String extraArgs = ".";
    public static boolean resumeLastSession = false;
    
    // Internal development options, might change later or expose to user
    public static final boolean INHIBIT_SUSPEND = true;
    public static final boolean DOWNLOAD_TO_SDCARD = true;
    public static final int SPLASH_DELAY = 1000; // Such a nice picture... shame not to see it :)
}
